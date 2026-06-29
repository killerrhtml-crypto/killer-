package com.killer.automation.core.auth

import android.content.Context
import android.content.Intent
import com.auth0.jwt.JWT
import com.auth0.jwt.exceptions.JWTDecodeException
import com.killer.automation.data.security.EncryptedDataManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import timber.log.Timber
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gestor de autenticación central
 * Responsabilidades:
 * - Realizar autenticación inicial
 * - Almacenar JWT de forma segura
 * - Validar tokens (local y remoto)
 * - Manejar expiración y refresco
 * - Emitir señales de bloqueo
 */
@Singleton
class AuthManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val encryptedDataManager: EncryptedDataManager,
    private val authService: AuthService
) {

    private val authScope = CoroutineScope(Dispatchers.IO + Job())
    private var validationJob: Job? = null

    companion object {
        private const val TAG = "AuthManager"
        private const val TOKEN_KEY = "auth_jwt_token"
        private const val REFRESH_TOKEN_KEY = "refresh_jwt_token"
        private const val USER_ID_KEY = "user_id"
        private const val TOKEN_EXPIRY_KEY = "token_expiry_timestamp"
        private const val LAST_VALIDATION_KEY = "last_validation_timestamp"
        private const val VALIDATION_INTERVAL_MS = 30 * 60 * 1000  // 30 minutos
        private const val TOKEN_REFRESH_THRESHOLD_MS = 5 * 60 * 1000  // Refrescar 5 min antes
        private const val ACTION_TOKEN_EXPIRED = "com.killer.automation.auth.TOKEN_EXPIRED"
        private const val ACTION_AUTH_FAILED = "com.killer.automation.auth.AUTH_FAILED"
    }

    /**
     * Autentica el usuario con credenciales
     * Almacena el JWT de forma segura
     *
     * @param email Email del usuario
     * @param password Contraseña del usuario
     * @param deviceId ID único del dispositivo (opcional)
     * @return true si la autenticación fue exitosa
     */
    suspend fun authenticate(
        email: String,
        password: String,
        deviceId: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            Timber.i("$TAG: Iniciando autenticación para: $email")

            val credentials = LoginCredentials(
                email = email,
                password = password,
                deviceId = deviceId
            )

            val response = authService.login(credentials)

            if (response.isSuccessful && response.body() != null) {
                val authData = response.body()!!
                saveAuthData(authData)

                Timber.i("$TAG: Autenticación exitosa para: $email")
                startBackgroundValidation()
                return@withContext true
            } else {
                val errorMsg = "Error de respuesta: ${response.code()} - ${response.message()}"
                Timber.e("$TAG: $errorMsg")
                emitAuthFailedSignal(errorMsg)
                return@withContext false
            }
        } catch (e: Exception) {
            Timber.e(e, "$TAG: Error durante autenticación")
            emitAuthFailedSignal(e.message ?: "Error desconocido")
            return@withContext false
        }
    }

    /**
     * Verifica si el token JWT es válido
     * Realiza dos niveles de validación:
     * 1. Validación local: decodifica JWT y verifica expiración
     * 2. Validación remota: valida contra el servidor
     *
     * @return true si el token es válido en ambos niveles
     */
    suspend fun isTokenValid(): Boolean = withContext(Dispatchers.IO) {
        try {
            val token = getToken()
            if (token.isEmpty()) {
                Timber.w("$TAG: Token no encontrado")
                return@withContext false
            }

            // Validación local
            if (!isLocalTokenValid(token)) {
                Timber.w("$TAG: Token inválido localmente")
                handleTokenExpiration()
                return@withContext false
            }

            // Validación remota
            if (!isRemoteTokenValid(token)) {
                Timber.w("$TAG: Token inválido remotamente")
                handleTokenExpiration()
                return@withContext false
            }

            updateLastValidationTime()
            Timber.d("$TAG: Token válido")
            return@withContext true
        } catch (e: Exception) {
            Timber.e(e, "$TAG: Error validando token")
            handleTokenExpiration()
            return@withContext false
        }
    }

    /**
     * Valida el token localmente
     * Decodifica el JWT y verifica la expiración
     *
     * @param token JWT token
     * @return true si el token es válido localmente
     */
    private fun isLocalTokenValid(token: String): Boolean {
        return try {
            val decodedJWT = JWT.decode(token)
            val expiresAt = decodedJWT.expiresAt

            // Token válido si expira en el futuro
            if (expiresAt == null) {
                Timber.w("$TAG: Token sin fecha de expiración")
                return false
            }

            val isValid = expiresAt.after(Date())
            val timeUntilExpiry = expiresAt.time - System.currentTimeMillis()

            Timber.d("$TAG: Validación local - Válido: $isValid, Expira en: ${timeUntilExpiry / 1000}s")

            // Si expira pronto, refrescar
            if (isValid && timeUntilExpiry < TOKEN_REFRESH_THRESHOLD_MS) {
                authScope.launch {
                    refreshToken()
                }
            }

            isValid
        } catch (e: JWTDecodeException) {
            Timber.e(e, "$TAG: Error decodificando JWT")
            false
        }
    }

    /**
     * Valida el token contra el servidor remoto
     * Verifica que el token no haya sido revocado
     *
     * @param token JWT token
     * @return true si el token es válido remotamente
     */
    private suspend fun isRemoteTokenValid(token: String): Boolean {
        return try {
            val request = TokenValidationRequest(token = token)
            val response = authService.validateToken(request)

            if (response.isSuccessful && response.body() != null) {
                val validationResponse = response.body()!!
                Timber.d("$TAG: Validación remota - Válido: ${validationResponse.isValid}")
                return validationResponse.isValid
            } else {
                Timber.e("$TAG: Error en validación remota: ${response.code()}")
                return false
            }
        } catch (e: Exception) {
            Timber.e(e, "$TAG: Excepción en validación remota")
            // En caso de error de red, confiar en validación local
            return isLocalTokenValid(getToken())
        }
    }

    /**
     * Refresca el JWT usando el refresh token
     * Se ejecuta automáticamente si el token expira pronto
     *
     * @return true si el refresco fue exitoso
     */
    suspend fun refreshToken(): Boolean = withContext(Dispatchers.IO) {
        try {
            val refreshToken = getRefreshToken()
            if (refreshToken.isEmpty()) {
                Timber.w("$TAG: Refresh token no encontrado")
                return@withContext false
            }

            Timber.i("$TAG: Refrescando token")
            val request = RefreshTokenRequest(refreshToken = refreshToken)
            val response = authService.refreshToken(request)

            if (response.isSuccessful && response.body() != null) {
                val authData = response.body()!!
                saveAuthData(authData)
                Timber.i("$TAG: Token refrescado exitosamente")
                return@withContext true
            } else {
                Timber.e("$TAG: Error refrescando token: ${response.code()}")
                handleTokenExpiration()
                return@withContext false
            }
        } catch (e: Exception) {
            Timber.e(e, "$TAG: Excepción refrescando token")
            handleTokenExpiration()
            return@withContext false
        }
    }

    /**
     * Guarda los datos de autenticación de forma segura
     *
     * @param authData Respuesta de autenticación con tokens
     */
    private fun saveAuthData(authData: AuthResponse) {
        encryptedDataManager.saveAuthToken(authData.token)
        encryptedDataManager.saveRefreshToken(authData.refreshToken)

        // Calcular timestamp de expiración
        val expiryTimestamp = System.currentTimeMillis() + (authData.expiresIn * 1000)
        encryptedDataManager.saveString(TOKEN_EXPIRY_KEY, expiryTimestamp.toString())

        if (!authData.userId.isNullOrEmpty()) {
            encryptedDataManager.saveUserId(authData.userId)
        }

        Timber.i("$TAG: Datos de autenticación guardados de forma segura")
    }

    /**
     * Obtiene el token JWT almacenado
     *
     * @return Token JWT o cadena vacía
     */
    private fun getToken(): String {
        return encryptedDataManager.getAuthToken()
    }

    /**
     * Obtiene el refresh token almacenado
     *
     * @return Refresh token o cadena vacía
     */
    private fun getRefreshToken(): String {
        return encryptedDataManager.getRefreshToken()
    }

    /**
     * Obtiene el ID del usuario autenticado
     *
     * @return ID del usuario o cadena vacía
     */
    fun getUserId(): String {
        return encryptedDataManager.getUserId()
    }

    /**
     * Obtiene el token JWT actual
     * Devuelve el token sin el prefijo "Bearer "
     *
     * @return Token JWT o null si no existe
     */
    fun getTokenForHeader(): String? {
        val token = getToken()
        return if (token.isNotEmpty()) "Bearer $token" else null
    }

    /**
     * Actualiza el timestamp de la última validación
     */
    private fun updateLastValidationTime() {
        encryptedDataManager.saveLastSyncTime(System.currentTimeMillis())
    }

    /**
     * Obtiene el timestamp de la última validación
     *
     * @return Timestamp en milisegundos, 0 si nunca se validó
     */
    fun getLastValidationTime(): Long {
        return encryptedDataManager.getLastSyncTime()
    }

    /**
     * Verifica si es necesario realizar una validación remota
     * Se basa en el intervalo de validación (30 minutos)
     *
     * @return true si ha pasado el intervalo desde la última validación
     */
    private fun shouldValidateRemote(): Boolean {
        val lastValidation = getLastValidationTime()
        val timeSinceLastValidation = System.currentTimeMillis() - lastValidation
        return timeSinceLastValidation >= VALIDATION_INTERVAL_MS
    }

    /**
     * Inicia la validación en background
     * Ejecuta una comprobación de validación cada 30 minutos
     */
    private fun startBackgroundValidation() {
        // Cancelar trabajo anterior si existe
        validationJob?.cancel()

        validationJob = authScope.launch {
            while (isActive) {
                try {
                    delay(VALIDATION_INTERVAL_MS.toLong())

                    if (shouldValidateRemote()) {
                        Timber.d("$TAG: Ejecutando validación en background")
                        val isValid = isTokenValid()

                        if (!isValid) {
                            Timber.w("$TAG: Validación en background falló")
                            handleTokenExpiration()
                            break
                        }
                    }
                } catch (e: Exception) {
                    if (e !is CancellationException) {
                        Timber.e(e, "$TAG: Error en validación en background")
                    }
                }
            }
        }

        Timber.i("$TAG: Validación en background iniciada")
    }

    /**
     * Maneja la expiración del token
     * Emite señal de bloqueo para que AccessibilityService se detenga
     */
    private fun handleTokenExpiration() {
        Timber.w("$TAG: Manejando expiración de token")
        encryptedDataManager.clear()
        emitTokenExpiredSignal()
    }

    /**
     * Emite señal de token expirado
     * Broadcast para que AccessibilityService se detenga
     */
    private fun emitTokenExpiredSignal() {
        try {
            val intent = Intent(ACTION_TOKEN_EXPIRED).apply {
                setPackage(context.packageName)
            }
            context.sendBroadcast(intent)
            Timber.i("$TAG: Broadcast TOKEN_EXPIRED enviado")
        } catch (e: Exception) {
            Timber.e(e, "$TAG: Error enviando broadcast TOKEN_EXPIRED")
        }
    }

    /**
     * Emite señal de autenticación fallida
     */
    private fun emitAuthFailedSignal(reason: String) {
        try {
            val intent = Intent(ACTION_AUTH_FAILED).apply {
                setPackage(context.packageName)
                putExtra("reason", reason)
            }
            context.sendBroadcast(intent)
            Timber.i("$TAG: Broadcast AUTH_FAILED enviado")
        } catch (e: Exception) {
            Timber.e(e, "$TAG: Error enviando broadcast AUTH_FAILED")
        }
    }

    /**
     * Cierra sesión del usuario
     * Revoca el token en el servidor
     *
     * @return true si el logout fue exitoso
     */
    suspend fun logout(): Boolean = withContext(Dispatchers.IO) {
        try {
            val token = getTokenForHeader()
            if (token == null) {
                Timber.w("$TAG: No hay token para logout")
                return@withContext false
            }

            Timber.i("$TAG: Realizando logout")
            val response = authService.logout(token)

            if (response.isSuccessful) {
                encryptedDataManager.clear()
                validationJob?.cancel()
                Timber.i("$TAG: Logout exitoso")
                return@withContext true
            } else {
                Timber.e("$TAG: Error en logout: ${response.code()}")
                // Limpiar datos locales de todas formas
                encryptedDataManager.clear()
                return@withContext false
            }
        } catch (e: Exception) {
            Timber.e(e, "$TAG: Excepción en logout")
            // Limpiar datos locales de todas formas
            encryptedDataManager.clear()
            return@withContext false
        }
    }

    /**
     * Verifica si el usuario está autenticado
     *
     * @return true si hay un token almacenado
     */
    fun isAuthenticated(): Boolean {
        return getToken().isNotEmpty()
    }

    /**
     * Limpia los recursos
     * Cancela trabajos en segundo plano
     */
    fun cancel() {
        validationJob?.cancel()
        authScope.cancel()
        Timber.i("$TAG: AuthManager cancelado")
    }

    override fun finalize() {
        cancel()
    }
}
