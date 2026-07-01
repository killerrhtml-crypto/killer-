package com.killer.automation.manager

import android.content.Context
import android.util.Log
import com.killer.automation.utils.EncryptedDataManager
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicBoolean

/**
 * SubscriptionManager - Singleton que maneja sincronización de suscripción
 * Se encarga de validar el estado de la suscripción del usuario llamando a la API backend
 */
class SubscriptionManager private constructor(private val context: Context) {
    
    private val encryptedDataManager = EncryptedDataManager(context)
    private val coroutineScope = CoroutineScope(Dispatchers.IO + Job())
    
    private val isSubscriptionActive = AtomicBoolean(false)
    private val isAccessibilityEnabled = AtomicBoolean(false)
    
    var subscriptionStatus: SubscriptionStatus = SubscriptionStatus.UNKNOWN
        private set
    
    init {
        Log.d(TAG, "SubscriptionManager initialized")
    }
    
    /**
     * Realiza una sincronización de suscripción al iniciar la app
     */
    fun synchronizeSubscription() {
        coroutineScope.launch {
            try {
                Log.d(TAG, "Starting subscription synchronization...")
                
                val deviceId = encryptedDataManager.getDeviceId()
                val apiKey = encryptedDataManager.getApiKey()
                
                if (deviceId.isNullOrEmpty() || apiKey.isNullOrEmpty()) {
                    Log.w(TAG, "Device ID or API Key not found")
                    subscriptionStatus = SubscriptionStatus.UNKNOWN
                    return@launch
                }
                
                // Llamar a la API del backend
                val status = checkSubscriptionStatus(deviceId, apiKey)
                subscriptionStatus = status
                
                when (status) {
                    SubscriptionStatus.ACTIVE -> {
                        isSubscriptionActive.set(true)
                        Log.i(TAG, "Subscription is ACTIVE - Automation enabled")
                        // Guardar timestamp de última sincronización exitosa
                        encryptedDataManager.saveLastSyncTimestamp(System.currentTimeMillis())
                    }
                    
                    SubscriptionStatus.BLOCKED -> {
                        isSubscriptionActive.set(false)
                        Log.w(TAG, "Subscription is BLOCKED - Disabling automation")
                        disableAccessibilityService()
                    }
                    
                    SubscriptionStatus.EXPIRED -> {
                        isSubscriptionActive.set(false)
                        Log.w(TAG, "Subscription is EXPIRED - Disabling automation")
                        disableAccessibilityService()
                    }
                    
                    SubscriptionStatus.UNKNOWN -> {
                        Log.e(TAG, "Unknown subscription status")
                        // Usar último estado conocido o desactivar por seguridad
                        val lastKnownStatus = encryptedDataManager.getLastKnownSubscriptionStatus()
                        isSubscriptionActive.set(lastKnownStatus == SubscriptionStatus.ACTIVE)
                    }
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error synchronizing subscription: \${e.message}")
                subscriptionStatus = SubscriptionStatus.UNKNOWN
            }
        }
    }
    
    /**
     * Llama a la API backend para verificar el estado de la suscripción
     */
    private suspend fun checkSubscriptionStatus(
        deviceId: String,
        apiKey: String
    ): SubscriptionStatus = withContext(Dispatchers.IO) {
        return@withContext try {
            // TODO: Implementar llamada real a la API
            // Por ahora retornamos un valor simulado
            val baseUrl = encryptedDataManager.getServerUrl()
            
            // Simulación de respuesta de API
            simulateApiCall(deviceId, apiKey, baseUrl)
            
        } catch (e: Exception) {
            Log.e(TAG, "API call failed: \${e.message}")
            SubscriptionStatus.UNKNOWN
        }
    }
    
    /**
     * Simulación de llamada a API (reemplazar con Retrofit/OkHttp en producción)
     */
    private suspend fun simulateApiCall(
        deviceId: String,
        apiKey: String,
        baseUrl: String?
    ): SubscriptionStatus = withContext(Dispatchers.IO) {
        // En producción, implementar con Retrofit
        try {
            // val response = apiService.checkSubscriptionStatus(deviceId, apiKey)
            // return@withContext response.status
            
            // Simulación temporal
            Log.d(TAG, "Simulating API call to \$baseUrl/api/check-status")
            delay(1000) // Simular latencia de red
            
            SubscriptionStatus.ACTIVE // Respuesta simulada
        } catch (e: Exception) {
            SubscriptionStatus.UNKNOWN
        }
    }
    
    /**
     * Desactiva el AccessibilityService de forma permanente
     */
    private fun disableAccessibilityService() {
        isAccessibilityEnabled.set(false)
        encryptedDataManager.saveAccessibilityEnabled(false)
        Log.i(TAG, "AccessibilityService disabled due to subscription status")
        
        // TODO: Notificar al usuario sobre el cambio de estado
        // showDisabledNotification("Tu suscripción ha sido bloqueada/expirada")
    }
    
    /**
     * Verifica si la suscripción está activa y el servicio está habilitado
     */
    fun isAutomationEnabled(): Boolean {
        return isSubscriptionActive.get() && isAccessibilityEnabled.get()
    }
    
    /**
     * Verifica si la suscripción específicamente está activa
     */
    fun isSubscriptionValid(): Boolean {
        return isSubscriptionActive.get()
    }
    
    /**
     * Habilita el AccessibilityService (solo si suscripción es válida)
     */
    fun enableAccessibilityService(): Boolean {
        if (!isSubscriptionActive.get()) {
            Log.w(TAG, "Cannot enable accessibility - subscription not active")
            return false
        }
        isAccessibilityEnabled.set(true)
        encryptedDataManager.saveAccessibilityEnabled(true)
        Log.i(TAG, "AccessibilityService enabled")
        return true
    }
    
    /**
     * Deshabilita el AccessibilityService manualmente
     */
    fun disableAccessibilityServiceManually() {
        disableAccessibilityService()
    }
    
    /**
     * Obtiene el estado actual de la suscripción
     */
    fun getSubscriptionStatus(): SubscriptionStatus {
        return subscriptionStatus
    }
    
    /**
     * Limpia los recursos
     */
    fun cleanup() {
        coroutineScope.cancel()
    }
    
    companion object {
        private const val TAG = "SubscriptionManager"
        
        @Volatile
        private var instance: SubscriptionManager? = null
        
        fun getInstance(context: Context): SubscriptionManager {
            return instance ?: synchronized(this) {
                instance ?: SubscriptionManager(context.applicationContext).also { instance = it }
            }
        }
    }
    
    /**
     * Estados de suscripción posibles
     */
    enum class SubscriptionStatus {
        ACTIVE,      // Suscripción válida y activa
        BLOCKED,     // Suscripción bloqueada por el administrador
        EXPIRED,     // Suscripción expirada
        UNKNOWN      // Estado desconocido
    }
}
