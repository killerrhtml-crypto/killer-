# Phase 3: Autenticación JWT y Validación Remota

## Overview

Phase 3 implementa el **Motor de Seguridad y Autenticación** que controla el acceso a toda la aplicación. Incluye:

1. **AuthService** - Interfaz Retrofit para endpoints de autenticación remota
2. **AuthManager** - Gestor centralizado de autenticación y validación de tokens
3. **AuthBroadcastReceiver** - Receptor de señales de expiración de token
4. **AuthInterceptor** - Interceptor OkHttp que agrega JWT a las peticiones
5. **TokenValidationWorker** - Worker de WorkManager para validación periódica
6. **AuthModule** - Módulo Hilt con proveedores de dependencias

---

## 1. AuthService (Retrofit)

### Interfaz de Endpoints

```kotlin
interface AuthService {
    @POST("/auth/login")
    suspend fun login(@Body credentials: LoginCredentials): Response<AuthResponse>
    
    @POST("/auth/validate")
    suspend fun validateToken(@Body request: TokenValidationRequest): Response<TokenValidationResponse>
    
    @POST("/auth/refresh")
    suspend fun refreshToken(@Body request: RefreshTokenRequest): Response<AuthResponse>
    
    @POST("/auth/logout")
    suspend fun logout(@Header("Authorization") token: String): Response<LogoutResponse>
}
```

### Modelos de Datos

#### LoginCredentials
```kotlin
data class LoginCredentials(
    val email: String,           // Email del usuario
    val password: String,        // Contraseña
    val deviceId: String? = null // ID único del dispositivo
)
```

#### AuthResponse
```kotlin
data class AuthResponse(
    val token: String,                  // JWT access token
    val refreshToken: String,           // Refresh token
    val expiresIn: Long,                // Expiración en segundos
    val tokenType: String = "Bearer",  // Tipo de token
    val userId: String? = null,         // ID del usuario
    val email: String? = null           // Email del usuario
)
```

#### TokenValidationRequest / TokenValidationResponse
```kotlin
data class TokenValidationRequest(val token: String)

data class TokenValidationResponse(
    val isValid: Boolean,          // Si el token es válido
    val expiresAt: Long? = null,   // Timestamp de expiración
    val userId: String? = null,    // ID del usuario
    val message: String? = null    // Mensaje adicional
)
```

### Excepciones Personalizadas

```kotlin
class AuthException(message: String, cause: Throwable? = null)
class TokenExpiredException(message: String = "Token ha expirado")
class InvalidTokenException(message: String = "Token inválido")
class AuthenticationFailedException(message: String = "Autenticación fallida")
class NetworkException(message: String = "Error de red", cause: Throwable? = null)
```

---

## 2. AuthManager (Singleton)

### Responsabilidades

✅ **Autenticación Inicial**
- Realiza login con email/password
- Almacena JWT de forma segura
- Configura validación en background

✅ **Almacenamiento Seguro**
- Usa `EncryptedDataManager` para guardar tokens
- Encriptación AES-256-GCM
- Protegido por Android Keystore

✅ **Validación de Tokens**
- Validación local: decodifica JWT y verifica expiración
- Validación remota: consulta servidor
- Refresco automático si expira pronto

✅ **Bloqueo y Señales**
- Emite broadcasts si token expira
- Bloquea AccessibilityService automáticamente
- Redirige a pantalla de login

✅ **Validación en Background**
- Cada 30 minutos verifica validez
- WorkManager para tareas persistentes
- Manejo de errores de red

### Métodos Principales

#### `authenticate(email, password, deviceId)`
```kotlin
/**
 * Autentica el usuario con credenciales
 * Almacena JWT de forma segura
 * Inicia validación en background
 * 
 * @return true si autenticación exitosa
 */
suspend fun authenticate(
    email: String,
    password: String,
    deviceId: String? = null
): Boolean
```

**Flujo:**
```
1. Enviar POST /auth/login con credenciales
2. Si exitoso:
   - Guardar JWT y RefreshToken cifrados
   - Guardar timestamp de expiración
   - Iniciar validación en background
3. Si falla:
   - Emitir broadcast AUTH_FAILED
   - Retornar false
```

#### `isTokenValid()`
```kotlin
/**
 * Valida token en dos niveles:
 * 1. Local: decodifica JWT y verifica expiración
 * 2. Remoto: consulta servidor si token sigue siendo válido
 * 
 * @return true si válido en ambos niveles
 */
suspend fun isTokenValid(): Boolean
```

**Validación Local:**
```kotlin
private fun isLocalTokenValid(token: String): Boolean {
    val decodedJWT = JWT.decode(token)
    val expiresAt = decodedJWT.expiresAt
    
    // Si expira en < 5 minutos, refrescar automáticamente
    val timeUntilExpiry = expiresAt.time - System.currentTimeMillis()
    if (isValid && timeUntilExpiry < 5_MINUTES) {
        refreshToken()  // En background
    }
    
    return expiresAt.after(Date())
}
```

**Validación Remota:**
```kotlin
private suspend fun isRemoteTokenValid(token: String): Boolean {
    val request = TokenValidationRequest(token = token)
    val response = authService.validateToken(request)
    
    // Retorna result.isValid desde servidor
    // Si error de red, confiar en validación local
    return validationResponse.isValid
}
```

#### `refreshToken()`
```kotlin
/**
 * Refresca JWT usando el refresh token
 * Se ejecuta automáticamente si:
 * - El token expira en < 5 minutos
 * - El usuario solicita refresco manual
 * 
 * @return true si refresco exitoso
 */
suspend fun refreshToken(): Boolean
```

#### `startBackgroundValidation()`
```kotlin
/**
 * Inicia validación en background
 * Ejecuta cada 30 minutos:
 * - Valida token localmente
 * - Si falla, emite señal de bloqueo
 * - Si expira, cancela validación
 */
private fun startBackgroundValidation()
```

#### `logout()`
```kotlin
/**
 * Cierra sesión del usuario
 * - Revoca token en servidor
 * - Limpia datos locales cifrados
 * - Cancela validación en background
 * 
 * @return true si logout exitoso
 */
suspend fun logout(): Boolean
```

### Señales de Control

#### TOKEN_EXPIRED Broadcast
```kotlin
private fun emitTokenExpiredSignal() {
    val intent = Intent("com.killer.automation.auth.TOKEN_EXPIRED")
    context.sendBroadcast(intent)
    // AccessibilityService ejecuta: disableSelf()
}
```

#### AUTH_FAILED Broadcast
```kotlin
private fun emitAuthFailedSignal(reason: String) {
    val intent = Intent("com.killer.automation.auth.AUTH_FAILED")
    intent.putExtra("reason", reason)
    context.sendBroadcast(intent)
    // MainActivity redirige a login
}
```

---

## 3. AuthBroadcastReceiver

### Función

Escucha señales de autenticación y ejecuta callbacks:

```kotlin
class AuthBroadcastReceiver(
    private val onTokenExpired: () -> Unit,
    private val onAuthFailed: (reason: String) -> Unit
) : BroadcastReceiver()
```

### Acciones Escuchadas

```kotlin
ACTION_TOKEN_EXPIRED  // "com.killer.automation.auth.TOKEN_EXPIRED"
ACTION_AUTH_FAILED    // "com.killer.automation.auth.AUTH_FAILED"
```

### Uso en AccessibilityService

```kotlin
@AndroidEntryPoint
class AutomationAccessibilityService : AccessibilityService() {
    
    private fun registerAuthBroadcastReceiver() {
        authBroadcastReceiver = AuthBroadcastReceiver(
            onTokenExpired = {
                Timber.w("Token expirado - Deteniendo servicio")
                disableSelf()  // Detiene el AccessibilityService
            },
            onAuthFailed = { reason ->
                Timber.e("Autenticación fallida - Deteniendo servicio")
                disableSelf()
            }
        )
        authBroadcastReceiver?.register(this)
    }
}
```

---

## 4. AuthInterceptor

### Función

Interceptor de OkHttp que agrega JWT a todas las peticiones HTTP:

```kotlin
class AuthInterceptor(private val authManager: AuthManager) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = authManager.getTokenForHeader()  // "Bearer <token>"
        
        val authenticatedRequest = chain.request().newBuilder()
            .addHeader("Authorization", token)
            .build()
        
        return chain.proceed(authenticatedRequest)
    }
}
```

### Integración en OkHttpClient

```kotlin
val okHttpClient = OkHttpClient.Builder()
    .addInterceptor(authInterceptor)  // Agrega header Authorization
    .connectTimeout(30, TimeUnit.SECONDS)
    .readTimeout(30, TimeUnit.SECONDS)
    .writeTimeout(30, TimeUnit.SECONDS)
    .build()
```

---

## 5. TokenValidationWorker

### Función

Worker de WorkManager que ejecuta validación cada 30 minutos:

```kotlin
class TokenValidationWorker(...) : CoroutineWorker(...) {
    override suspend fun doWork(): Result {
        val isValid = authManager.isTokenValid()
        return if (isValid) Result.success() else Result.retry()
    }
}
```

### Programar Validación

```kotlin
TokenValidationWorker.scheduleTokenValidation(context)
// Programa tarea periódica cada 30 minutos
```

### Cancelar Validación

```kotlin
TokenValidationWorker.cancelTokenValidation(context)
```

---

## 6. AuthModule (Hilt)

### Proveedores

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object AuthModule {
    
    @Singleton
    @Provides
    fun provideAuthInterceptor(authManager: AuthManager): AuthInterceptor
    
    @Singleton
    @Provides
    fun provideOkHttpClient(authInterceptor: AuthInterceptor): OkHttpClient
    
    @Singleton
    @Provides
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit
    
    @Singleton
    @Provides
    fun provideAuthService(retrofit: Retrofit): AuthService
    
    @Singleton
    @Provides
    fun provideAuthManager(
        @ApplicationContext context: Context,
        encryptedDataManager: EncryptedDataManager,
        authService: AuthService
    ): AuthManager
}
```

---

## 7. Integración en AutomationAccessibilityService

### Inyección de AuthManager

```kotlin
@AndroidEntryPoint
class AutomationAccessibilityService : AccessibilityService() {
    
    @Inject
    lateinit var authManager: AuthManager
    
    // ...
}
```

### Validación Antes de Procesar Gestos

```kotlin
override fun onAccessibilityEvent(event: AccessibilityEvent?) {
    if (!isServiceActive || event == null) return
    
    serviceScope.launch {
        // IMPORTANTE: Validar autenticación PRIMERO
        if (!authManager.isTokenValid()) {
            Timber.w("Token inválido - Deteniendo servicio")
            disableSelf()  // Detener servicio de accesibilidad
            return@launch
        }
        
        // Solo procesar si token es válido
        gestureInterceptor.handleAccessibilityEvent(event)
    }
}
```

### Registro de Receptor de Broadcasts

```kotlin
override fun onServiceConnected() {
    super.onServiceConnected()
    registerAuthBroadcastReceiver()
    // ...
}

private fun registerAuthBroadcastReceiver() {
    authBroadcastReceiver = AuthBroadcastReceiver(
        onTokenExpired = { disableSelf() },
        onAuthFailed = { reason -> disableSelf() }
    )
    authBroadcastReceiver?.register(this)
}
```

---

## Flujo Completo de Autenticación

```
1. Usuario inicia aplicación
   ↓
2. MainActivity solicita login (email, password)
   ↓
3. AuthManager.authenticate(email, password)
   ↓
4. POST /auth/login con credenciales
   ↓
5. Servidor retorna JWT + RefreshToken + ExpiresIn
   ↓
6. Guardar de forma segura (EncryptedDataManager):
   - JWT en EncryptedSharedPreferences
   - RefreshToken en EncryptedSharedPreferences
   - Timestamp de expiración
   ↓
7. Iniciar validación en background (cada 30 min)
   ↓
8. AccessibilityService inicia
   ↓
9. Cada evento -> verificar isTokenValid()
   ↓
10. Validación exitosa -> procesar gesto
    Validación fallida -> disableSelf()
```

---

## Flujo de Validación (30 minutos)

```
WorkManager Timer (30 min)
    ↓
TokenValidationWorker.doWork()
    ↓
authManager.isTokenValid()
    ↓
1. Validación Local:
   - Decodificar JWT
   - Verificar expiresAt > now()
   - Si expira en < 5 min: refreshToken()
   ↓
2. Validación Remota:
   - POST /auth/validate con token
   - Servidor retorna isValid
   ↓
Resultado:
- Válido: Result.success()
- Inválido: 
  - emitTokenExpiredSignal()
  - AccessibilityService: disableSelf()
  - Result.retry()
```

---

## Flujo de Refresco de Token

```
Token expira en < 5 minutos
    ↓
authManager.refreshToken()
    ↓
POST /auth/refresh con RefreshToken
    ↓
Servidor retorna nuevo JWT + RefreshToken + ExpiresIn
    ↓
Guardar de forma segura
    ↓
Continuar con automatización sin interrupciones
```

---

## Flujo de Expiración de Token

```
Token expirado detectado
    ↓
emitTokenExpiredSignal()
    ↓
Broadcast: "com.killer.automation.auth.TOKEN_EXPIRED"
    ↓
AuthBroadcastReceiver.onReceive()
    ↓
AccessibilityService.onTokenExpired()
    ↓
disableSelf()  // Detener servicio
    ↓
MainActivity: redirigir a LoginActivity
    ↓
Limpiar datos cifrados:
- encryptedDataManager.clear()
- EncryptedSharedPreferences borrado
```

---

## Uso en Aplicación

### Login

```kotlin
@AndroidEntryPoint
class LoginActivity : ComponentActivity() {
    
    @Inject
    lateinit var authManager: AuthManager
    
    private fun performLogin(email: String, password: String) {
        lifecycleScope.launch {
            val success = authManager.authenticate(email, password)
            if (success) {
                // Navegar a MainActivity
                startActivity(Intent(this@LoginActivity, MainActivity::class.java))
            } else {
                // Mostrar error
                showError("Autenticación fallida")
            }
        }
    }
}
```

### Logout

```kotlin
private fun performLogout() {
    lifecycleScope.launch {
        authManager.logout()  // Revoca token + limpia datos
        // Navegar a LoginActivity
        startActivity(Intent(this@MainActivity, LoginActivity::class.java))
    }
}
```

### Obtener Token para Peticiones Manuales

```kotlin
val token = authManager.getTokenForHeader()  // "Bearer <jwt>"
// O solo el token
val jwtToken = authManager.getToken()
```

---

## Constantes de Configuración

```kotlin
companion object {
    private const val VALIDATION_INTERVAL_MS = 30 * 60 * 1000  // 30 minutos
    private const val TOKEN_REFRESH_THRESHOLD_MS = 5 * 60 * 1000  // 5 minutos
    private const val CONNECT_TIMEOUT_SECONDS = 30L
    private const val READ_TIMEOUT_SECONDS = 30L
    private const val WRITE_TIMEOUT_SECONDS = 30L
}
```

---

## Manejo de Errores

### Errores de Autenticación

```kotlin
- TokenExpiredException: Token ha expirado
- InvalidTokenException: Token no válido
- AuthenticationFailedException: Credenciales incorrectas
- NetworkException: Error de conectividad
```

### Recuperación de Errores

```kotlin
// Si error de red -> confiar en validación local
// Si token inválido -> redirigir a login
// Si refresco falla -> emitir señal de bloqueo
```

---

## Seguridad

✅ **JWT Almacenado Cifrado**
- EncryptedSharedPreferences con AES-256
- Protegido por Android Keystore

✅ **Validación Dual**
- Local: verificar expiración en JWT
- Remota: consultar servidor

✅ **Refresco Automático**
- Si expira en < 5 minutos
- Sin interrupciones en automatización

✅ **Bloqueo Automático**
- Token expirado -> disableSelf()
- Auth fallida -> broadcasts

✅ **Logging Seguro**
- No loguear tokens
- No loguear contraseñas
- Solo eventos en Timber

---

## Próximos Pasos (Phase 4)

- [ ] Motor de filtrado de datos (precio, distancia, zonas)
- [ ] Geofencing y zonificación
- [ ] Persistencia de configuración
- [ ] Tests unitarios y de integración

---

## License

Proprietary - Killer Automation
