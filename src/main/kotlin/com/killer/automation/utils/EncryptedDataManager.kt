package com.killer.automation.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.Gson
import com.killer.automation.data.UserPreferences
import com.killer.automation.manager.SubscriptionManager

/**
 * EncryptedDataManager - Singleton para gestionar datos encriptados
 * Maneja almacenamiento seguro de preferencias, API keys, conexiones BD, etc.
 */
class EncryptedDataManager(private val context: Context) {
    
    private val gson = Gson()
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    
    private val encryptedPrefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        ENCRYPTED_PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    
    /**
     * Guarda las preferencias del usuario
     */
    fun saveUserPreferences(preferences: UserPreferences) {
        try {
            val json = gson.toJson(preferences)
            encryptedPrefs.edit().putString(KEY_USER_PREFERENCES, json).apply()
            Log.d(TAG, "User preferences saved")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving user preferences: ${e.message}")
        }
    }
    
    /**
     * Obtiene las preferencias del usuario
     */
    fun getUserPreferences(): UserPreferences? {
        return try {
            val json = encryptedPrefs.getString(KEY_USER_PREFERENCES, null)
            if (json != null) {
                gson.fromJson(json, UserPreferences::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user preferences: ${e.message}")
            null
        }
    }
    
    /**
     * Guarda el ID del dispositivo
     */
    fun saveDeviceId(deviceId: String) {
        encryptedPrefs.edit().putString(KEY_DEVICE_ID, deviceId).apply()
    }
    
    /**
     * Obtiene el ID del dispositivo
     */
    fun getDeviceId(): String? {
        return encryptedPrefs.getString(KEY_DEVICE_ID, null)
    }
    
    /**
     * Guarda la API Key
     */
    fun saveApiKey(apiKey: String) {
        encryptedPrefs.edit().putString(KEY_API_KEY, apiKey).apply()
    }
    
    /**
     * Obtiene la API Key
     */
    fun getApiKey(): String? {
        return encryptedPrefs.getString(KEY_API_KEY, null)
    }
    
    /**
     * Guarda la URL del servidor
     */
    fun saveServerUrl(url: String) {
        encryptedPrefs.edit().putString(KEY_SERVER_URL, url).apply()
    }
    
    /**
     * Obtiene la URL del servidor
     */
    fun getServerUrl(): String? {
        return encryptedPrefs.getString(KEY_SERVER_URL, null)
    }
    
    /**
     * Guarda el timestamp de última sincronización
     */
    fun saveLastSyncTimestamp(timestamp: Long) {
        encryptedPrefs.edit().putLong(KEY_LAST_SYNC_TIMESTAMP, timestamp).apply()
    }
    
    /**
     * Obtiene el timestamp de última sincronización
     */
    fun getLastSyncTimestamp(): Long {
        return encryptedPrefs.getLong(KEY_LAST_SYNC_TIMESTAMP, 0L)
    }
    
    /**
     * Guarda si el AccessibilityService está habilitado
     */
    fun saveAccessibilityEnabled(enabled: Boolean) {
        encryptedPrefs.edit().putBoolean(KEY_ACCESSIBILITY_ENABLED, enabled).apply()
    }
    
    /**
     * Obtiene si el AccessibilityService está habilitado
     */
    fun isAccessibilityEnabled(): Boolean {
        return encryptedPrefs.getBoolean(KEY_ACCESSIBILITY_ENABLED, false)
    }
    
    /**
     * Guarda el último estado conocido de suscripción
     */
    fun saveLastKnownSubscriptionStatus(status: SubscriptionManager.SubscriptionStatus) {
        encryptedPrefs.edit().putString(KEY_LAST_SUBSCRIPTION_STATUS, status.name).apply()
    }
    
    /**
     * Obtiene el último estado conocido de suscripción
     */
    fun getLastKnownSubscriptionStatus(): SubscriptionManager.SubscriptionStatus? {
        val status = encryptedPrefs.getString(KEY_LAST_SUBSCRIPTION_STATUS, null) ?: return null
        return try {
            SubscriptionManager.SubscriptionStatus.valueOf(status)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Limpia todos los datos encriptados
     */
    fun clearAllData() {
        try {
            encryptedPrefs.edit().clear().apply()
            Log.i(TAG, "All encrypted data cleared")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing encrypted data: ${e.message}")
        }
    }
    
    companion object {
        private const val TAG = "EncryptedDataManager"
        
        private const val ENCRYPTED_PREFS_NAME = "killer_automation_encrypted_prefs"
        
        // Keys
        private const val KEY_USER_PREFERENCES = "user_preferences"
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_API_KEY = "api_key"
        private const val KEY_SERVER_URL = "server_url"
        private const val KEY_LAST_SYNC_TIMESTAMP = "last_sync_timestamp"
        private const val KEY_ACCESSIBILITY_ENABLED = "accessibility_enabled"
        private const val KEY_LAST_SUBSCRIPTION_STATUS = "last_subscription_status"
    }
}
