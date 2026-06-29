package com.killer.automation.core.auth

import okhttp3.Interceptor
import okhttp3.Response
import timber.log.Timber
import javax.inject.Inject

/**
 * Interceptor de OkHttp que agrega el JWT token a las peticiones
 * Automáticamente agrega el header Authorization: Bearer <token>
 * a todas las peticiones HTTP
 */
class AuthInterceptor @Inject constructor(
    private val authManager: AuthManager
) : Interceptor {

    companion object {
        private const val TAG = "AuthInterceptor"
        private const val AUTHORIZATION_HEADER = "Authorization"
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        try {
            val originalRequest = chain.request()

            // Obtener token JWT del AuthManager
            val authHeader = authManager.getTokenForHeader()

            // Si no hay token, continuar sin agregar header
            if (authHeader == null) {
                Timber.d("$TAG: No hay token JWT disponible")
                return chain.proceed(originalRequest)
            }

            // Crear nueva solicitud con header Authorization
            val authenticatedRequest = originalRequest.newBuilder()
                .addHeader(AUTHORIZATION_HEADER, authHeader)
                .build()

            Timber.d("$TAG: Header Authorization agregado a petición")
            return chain.proceed(authenticatedRequest)
        } catch (e: Exception) {
            Timber.e(e, "$TAG: Error en interceptor de autenticación")
            return chain.proceed(chain.request())
        }
    }
}
