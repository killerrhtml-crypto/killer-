package com.killer.automation.core.auth

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Header

/**
 * Interfaz de servicio de autenticación usando Retrofit
 * Define los endpoints remotos para autenticación y validación de tokens
 */
interface AuthService {

    /**
     * Autentica un usuario con credenciales
     * Retorna un JWT token y refresh token
     *
     * @param credentials Credenciales de login (username/email + password)
     * @return Response con AuthResponse (token, refreshToken, expiresIn)
     */
    @POST("/auth/login")
    suspend fun login(
        @Body credentials: LoginCredentials
    ): Response<AuthResponse>

    /**
     * Valida un JWT token contra el servidor
     * Verifica que el token sea válido y no haya sido revocado
     *
     * @param token JWT token a validar
     * @return Response con ValidationResponse (isValid, expiresAt)
     */
    @POST("/auth/validate")
    suspend fun validateToken(
        @Body request: TokenValidationRequest
    ): Response<TokenValidationResponse>

    /**
     * Refresca un JWT token expirado o próximo a expirar
     * Utiliza un refresh token para obtener un nuevo JWT
     *
     * @param refreshToken Token para refrescar
     * @return Response con AuthResponse (nuevo token)
     */
    @POST("/auth/refresh")
    suspend fun refreshToken(
        @Body request: RefreshTokenRequest
    ): Response<AuthResponse>

    /**
     * Cierra sesión del usuario
     * Revoca el token en el servidor
     *
     * @param token JWT token a revocar
     * @return Response con LogoutResponse
     */
    @POST("/auth/logout")
    suspend fun logout(
        @Header("Authorization") token: String
    ): Response<LogoutResponse>
}

/**
 * Modelo de credenciales de login
 * Contiene las credenciales del usuario para autenticación
 */
data class LoginCredentials(
    val email: String,
    val password: String,
    val deviceId: String? = null  // ID único del dispositivo (opcional)
)

/**
 * Modelo de respuesta de autenticación
 * Contiene tokens JWT y información de expiración
 */
data class AuthResponse(
    val token: String,                    // JWT access token
    val refreshToken: String,             // Token para refrescar JWT
    val expiresIn: Long,                  // Tiempo de expiración en segundos
    val tokenType: String = "Bearer",    // Tipo de token (por defecto Bearer)
    val userId: String? = null,           // ID del usuario (opcional)
    val email: String? = null             // Email del usuario (opcional)
)

/**
 * Modelo de solicitud de validación de token
 */
data class TokenValidationRequest(
    val token: String
)

/**
 * Modelo de respuesta de validación de token
 */
data class TokenValidationResponse(
    val isValid: Boolean,                 // Si el token es válido
    val expiresAt: Long? = null,          // Timestamp de expiración
    val userId: String? = null,           // ID del usuario
    val message: String? = null           // Mensaje adicional
)

/**
 * Modelo de solicitud de refresco de token
 */
data class RefreshTokenRequest(
    val refreshToken: String
)

/**
 * Modelo de respuesta de logout
 */
data class LogoutResponse(
    val success: Boolean,
    val message: String? = null
)

/**
 * Excepciones personalizadas para autenticación
 */
class AuthException(message: String, cause: Throwable? = null) : Exception(message, cause)

class TokenExpiredException(message: String = "Token ha expirado") : AuthException(message)

class InvalidTokenException(message: String = "Token inválido") : AuthException(message)

class AuthenticationFailedException(message: String = "Autenticación fallida") : AuthException(message)

class NetworkException(message: String = "Error de red", cause: Throwable? = null) : AuthException(message, cause)
