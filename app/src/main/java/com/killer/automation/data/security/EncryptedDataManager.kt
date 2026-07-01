package com.killer.automation.data.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import com.killer.automation.data.*
import timber.log.Timber
import java.util.*

/**
 * Gestor de datos cifrados que utiliza androidx.security:security-crypto
 * Proporciona métodos para guardar y recuperar datos de forma segura
 * utilizando AES-256-GCM para encriptación y MasterKey del Keystore del sistema
 *
 * Características:
 * - Almacenamiento cifrado de preferencias de usuario
 * - Gestión de zonas geográficas (preferidas y bloqueadas)
 * - Almacenamiento seguro de tokens JWT y estado de suscripción
 * - Serialización/deserialización con Gson
 * - Manejo seguro de errores sin crashes
 */
class EncryptedDataManager(
    private val context: Context,
    private val masterKey: MasterKey
) {

    private companion object {
        const val PREFS_FILE_NAME = "killer_secure_prefs"
        
        // Keys para almacenamiento
        const val DB_PASSPHRASE_KEY = "db_passphrase_key"
        const val TOKEN_KEY = "auth_token"
        const val REFRESH_TOKEN_KEY = "refresh_token"
        const val USER_ID_KEY = "user_id"
        const val LAST_SYNC_KEY = "last_sync"
        
        // Keys para nuevas estructuras
        const val USER_PREFERENCES_KEY = "user_preferences"
        const val ZONES_KEY = "zones"
        const val SUBSCRIPTION_STATUS_KEY = "subscription_status"
        const val AUDIT_LOG_KEY = "audit_logs"
    }

    private val encryptedSharedPreferences: EncryptedSharedPreferences by lazy {
        EncryptedSharedPreferences.create(
            context,
            PREFS_FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        ).also {
            Timber.d("EncryptedSharedPreferences initialized successfully")
        }
    }

    /**
     * Instancia única de Gson para toda la clase (reutilizable y thread-safe)
     */
    private val gson = Gson()

    // ============================================================================
    // Métodos originales (preservados para compatibilidad)
    // ============================================================================

    /**
     * Guarda un String de forma cifrada
     * @param key Clave del dato
     * @param value Valor a guardar
     */
    fun saveString(key: String, value: String) {
        try {
            encryptedSharedPreferences.edit().apply {
                putString(key, value)
                apply()
            }
            Timber.d("String saved securely: $key")
        } catch (e: Exception) {
            Timber.e(e, "Error saving string for key: $key")
        }
    }

    /**
     * Recupera un String cifrado
     * @param key Clave del dato
     * @param defaultValue Valor por defecto si no existe
     * @return El valor descifrado o el valor por defecto
     */
    fun getString(key: String, defaultValue: String = ""): String {
        return try {
            encryptedSharedPreferences.getString(key, defaultValue) ?: defaultValue
        } catch (e: Exception) {
            Timber.e(e, "Error retrieving string for key: $key")
            defaultValue
        }
    }

    /**
     * Guarda un entero de forma cifrada
     * @param key Clave del dato
     * @param value Valor a guardar
     */
    fun saveInt(key: String, value: Int) {
        try {
            encryptedSharedPreferences.edit().apply {
                putInt(key, value)
                apply()
            }
            Timber.d("Int saved securely: $key")
        } catch (e: Exception) {
            Timber.e(e, "Error saving int for key: $key")
        }
    }

    /**
     * Recupera un entero cifrado
     * @param key Clave del dato
     * @param defaultValue Valor por defecto si no existe
     * @return El valor descifrado o el valor por defecto
     */
    fun getInt(key: String, defaultValue: Int = 0): Int {
        return try {
            encryptedSharedPreferences.getInt(key, defaultValue)
        } catch (e: Exception) {
            Timber.e(e, "Error retrieving int for key: $key")
            defaultValue
        }
    }

    /**
     * Guarda un Float de forma cifrada
     * @param key Clave del dato
     * @param value Valor a guardar
     */
    fun saveFloat(key: String, value: Float) {
        try {
            encryptedSharedPreferences.edit().apply {
                putFloat(key, value)
                apply()
            }
            Timber.d("Float saved securely: $key")
        } catch (e: Exception) {
            Timber.e(e, "Error saving float for key: $key")
        }
    }

    /**
     * Recupera un Float cifrado
     * @param key Clave del dato
     * @param defaultValue Valor por defecto si no existe
     * @return El valor descifrado o el valor por defecto
     */
    fun getFloat(key: String, defaultValue: Float = 0f): Float {
        return try {
            encryptedSharedPreferences.getFloat(key, defaultValue)
        } catch (e: Exception) {
            Timber.e(e, "Error retrieving float for key: $key")
            defaultValue
        }
    }

    /**
     * Guarda un Boolean de forma cifrada
     * @param key Clave del dato
     * @param value Valor a guardar
     */
    fun saveBoolean(key: String, value: Boolean) {
        try {
            encryptedSharedPreferences.edit().apply {
                putBoolean(key, value)
                apply()
            }
            Timber.d("Boolean saved securely: $key")
        } catch (e: Exception) {
            Timber.e(e, "Error saving boolean for key: $key")
        }
    }

    /**
     * Recupera un Boolean cifrado
     * @param key Clave del dato
     * @param defaultValue Valor por defecto si no existe
     * @return El valor descifrado o el valor por defecto
     */
    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean {
        return try {
            encryptedSharedPreferences.getBoolean(key, defaultValue)
        } catch (e: Exception) {
            Timber.e(e, "Error retrieving boolean for key: $key")
            defaultValue
        }
    }

    /**
     * Elimina una clave específica
     * @param key Clave a eliminar
     */
    fun remove(key: String) {
        try {
            encryptedSharedPreferences.edit().apply {
                remove(key)
                apply()
            }
            Timber.d("Key removed: $key")
        } catch (e: Exception) {
            Timber.e(e, "Error removing key: $key")
        }
    }

    /**
     * Elimina todos los datos cifrados
     * ADVERTENCIA: Esta operación no se puede deshacer
     */
    fun clear() {
        try {
            encryptedSharedPreferences.edit().apply {
                clear()
                apply()
            }
            Timber.w("All encrypted data cleared")
        } catch (e: Exception) {
            Timber.e(e, "Error clearing encrypted data")
        }
    }

    /**
     * Verifica si existe una clave
     * @param key Clave a verificar
     * @return true si existe, false si no
     */
    fun contains(key: String): Boolean {
        return try {
            encryptedSharedPreferences.contains(key)
        } catch (e: Exception) {
            Timber.e(e, "Error checking if key exists: $key")
            false
        }
    }

    /**
     * Obtiene o genera la contraseña para cifrar la base de datos SQLCipher
     * La contraseña es generada una sola vez y almacenada de forma cifrada
     * @return La contraseña en formato String
     */
    fun getOrGenerateDatabasePassphrase(): String {
        return try {
            val existingPassphrase = getString(DB_PASSPHRASE_KEY)
            if (existingPassphrase.isNotEmpty()) {
                Timber.d("Using existing database passphrase")
                existingPassphrase
            } else {
                // Generar nueva contraseña usando UUID
                val newPassphrase = UUID.randomUUID().toString()
                saveString(DB_PASSPHRASE_KEY, newPassphrase)
                Timber.i("New database passphrase generated and stored securely")
                newPassphrase
            }
        } catch (e: Exception) {
            Timber.e(e, "Error managing database passphrase")
            throw SecurityException("Failed to manage database passphrase", e)
        }
    }

    /**
     * Guarda un token JWT de forma cifrada
     * @param token Token JWT a guardar
     */
    fun saveAuthToken(token: String) {
        saveString(TOKEN_KEY, token)
        Timber.d("Auth token saved securely")
    }

    /**
     * Recupera el token JWT cifrado
     * @return El token JWT o cadena vacía si no existe
     */
    fun getAuthToken(): String {
        return getString(TOKEN_KEY)
    }

    /**
     * Guarda un refresh token de forma cifrada
     * @param token Refresh token a guardar
     */
    fun saveRefreshToken(token: String) {
        saveString(REFRESH_TOKEN_KEY, token)
        Timber.d("Refresh token saved securely")
    }

    /**
     * Recupera el refresh token cifrado
     * @return El refresh token o cadena vacía si no existe
     */
    fun getRefreshToken(): String {
        return getString(REFRESH_TOKEN_KEY)
    }

    /**
     * Guarda el ID del usuario
     * @param userId ID del usuario
     */
    fun saveUserId(userId: String) {
        saveString(USER_ID_KEY, userId)
    }

    /**
     * Recupera el ID del usuario
     * @return El ID del usuario o cadena vacía si no existe
     */
    fun getUserId(): String {
        return getString(USER_ID_KEY)
    }

    /**
     * Guarda el timestamp de la última sincronización
     * @param timestamp Timestamp en milisegundos
     */
    fun saveLastSyncTime(timestamp: Long) {
        encryptedSharedPreferences.edit().apply {
            putLong(LAST_SYNC_KEY, timestamp)
            apply()
        }
    }

    /**
     * Recupera el timestamp de la última sincronización
     * @return Timestamp en milisegundos, 0 si no existe
     */
    fun getLastSyncTime(): Long {
        return try {
            encryptedSharedPreferences.getLong(LAST_SYNC_KEY, 0L)
        } catch (e: Exception) {
            Timber.e(e, "Error retrieving last sync time")
            0L
        }
    }

    // ============================================================================
    // NUEVOS MÉTODOS: Gestión de UserPreferences
    // ============================================================================

    /**
     * Guarda las preferencias del usuario de forma cifrada
     * @param preferences Objeto UserPreferences a guardar
     * @return true si se guardó correctamente, false si hubo error
     */
    fun saveUserPreferences(preferences: UserPreferences): Boolean {
        return try {
            val json = gson.toJson(preferences)
            saveString(USER_PREFERENCES_KEY, json)
            Timber.i("UserPreferences saved successfully for user: ${preferences.userId}")
            true
        } catch (e: Exception) {
            Timber.e(e, "Error serializing UserPreferences")
            false
        }
    }

    /**
     * Recupera las preferencias del usuario
     * @return UserPreferences descifradas o null si no existen
     */
    fun getUserPreferences(): UserPreferences? {
        return try {
            val json = getString(USER_PREFERENCES_KEY)
            if (json.isEmpty()) {
                Timber.d("No UserPreferences found")
                return null
            }
            val preferences = gson.fromJson(json, UserPreferences::class.java)
            Timber.d("UserPreferences retrieved successfully for user: ${preferences.userId}")
            preferences
        } catch (e: JsonSyntaxException) {
            Timber.e(e, "Error deserializing UserPreferences - data may be corrupted")
            null
        } catch (e: Exception) {
            Timber.e(e, "Unexpected error retrieving UserPreferences")
            null
        }
    }

    /**
     * Obtiene preferencias con fallback a valores por defecto
     * @param userId ID del usuario para crear preferencias por defecto si no existen
     * @return UserPreferences existentes o por defecto
     */
    fun getUserPreferencesOrDefault(userId: String): UserPreferences {
        return getUserPreferences() ?: UserPreferences.default(userId)
    }

    // ============================================================================
    // NUEVOS MÉTODOS: Gestión de Zonas (ZonePreference)
    // ============================================================================

    /**
     * Guarda una lista de zonas de forma cifrada
     * @param zones Lista de ZonePreference a guardar
     * @return true si se guardó correctamente, false si hubo error
     */
    fun saveZones(zones: List<ZonePreference>): Boolean {
        return try {
            val json = gson.toJson(zones)
            saveString(ZONES_KEY, json)
            Timber.i("Saved ${zones.size} zones successfully")
            true
        } catch (e: Exception) {
            Timber.e(e, "Error serializing zones")
            false
        }
    }

    /**
     * Recupera la lista de zonas
     * @return Lista de ZonePreference descifradas, o lista vacía si no existen
     */
    fun getZones(): List<ZonePreference> {
        return try {
            val json = getString(ZONES_KEY)
            if (json.isEmpty()) {
                Timber.d("No zones found, returning empty list")
                return emptyList()
            }
            val type = object : TypeToken<List<ZonePreference>>() {}.type
            val zones = gson.fromJson<List<ZonePreference>>(json, type)
            Timber.d("Retrieved ${zones.size} zones successfully")
            zones
        } catch (e: JsonSyntaxException) {
            Timber.e(e, "Error deserializing zones - data may be corrupted")
            emptyList()
        } catch (e: Exception) {
            Timber.e(e, "Unexpected error retrieving zones")
            emptyList()
        }
    }

    /**
     * Agrega una zona a la lista existente
     * @param zone ZonePreference a agregar
     * @return true si se agregó correctamente
     */
    fun addZone(zone: ZonePreference): Boolean {
        return try {
            val zones = getZones().toMutableList()
            // Evitar duplicados
            zones.removeAll { it.zoneId == zone.zoneId }
            zones.add(zone)
            saveZones(zones)
        } catch (e: Exception) {
            Timber.e(e, "Error adding zone: ${zone.zoneId}")
            false
        }
    }

    /**
     * Elimina una zona de la lista
     * @param zoneId ID de la zona a eliminar
     * @return true si se eliminó correctamente
     */
    fun removeZone(zoneId: String): Boolean {
        return try {
            val zones = getZones().toMutableList()
            val removed = zones.removeAll { it.zoneId == zoneId }
            if (removed) {
                saveZones(zones)
                Timber.d("Zone removed: $zoneId")
            }
            removed
        } catch (e: Exception) {
            Timber.e(e, "Error removing zone: $zoneId")
            false
        }
    }

    /**
     * Obtiene solo las zonas preferidas
     * @return Lista de zonas donde isPreferred == true
     */
    fun getPreferredZones(): List<ZonePreference> {
        return getZones().filter { it.isPreferred }
    }

    /**
     * Obtiene solo las zonas bloqueadas
     * @return Lista de zonas donde isPreferred == false
     */
    fun getBlockedZones(): List<ZonePreference> {
        return getZones().filter { !it.isPreferred }
    }

    // ============================================================================
    // NUEVOS MÉTODOS: Gestión de SubscriptionStatus
    // ============================================================================

    /**
     * Guarda el estado de suscripción del usuario
     * @param status SubscriptionStatusData a guardar
     * @return true si se guardó correctamente, false si hubo error
     */
    fun saveSubscriptionStatus(status: SubscriptionStatusData): Boolean {
        return try {
            val json = gson.toJson(status)
            saveString(SUBSCRIPTION_STATUS_KEY, json)
            Timber.i("SubscriptionStatus saved successfully for user: ${status.userId} - Status: ${status.status}")
            true
        } catch (e: Exception) {
            Timber.e(e, "Error serializing SubscriptionStatus")
            false
        }
    }

    /**
     * Recupera el estado de suscripción
     * @return SubscriptionStatusData descifrado o null si no existe
     */
    fun getSubscriptionStatus(): SubscriptionStatusData? {
        return try {
            val json = getString(SUBSCRIPTION_STATUS_KEY)
            if (json.isEmpty()) {
                Timber.d("No SubscriptionStatus found")
                return null
            }
            val status = gson.fromJson(json, SubscriptionStatusData::class.java)
            Timber.d("SubscriptionStatus retrieved successfully for user: ${status.userId} - Status: ${status.status}")
            status
        } catch (e: JsonSyntaxException) {
            Timber.e(e, "Error deserializing SubscriptionStatus - data may be corrupted")
            null
        } catch (e: Exception) {
            Timber.e(e, "Unexpected error retrieving SubscriptionStatus")
            null
        }
    }

    /**
     * Obtiene estado de suscripción con fallback a estado vacío
     * @return SubscriptionStatusData existente o vacío
     */
    fun getSubscriptionStatusOrEmpty(): SubscriptionStatusData {
        return getSubscriptionStatus() ?: SubscriptionStatusData.empty()
    }

    /**
     * Verifica si la suscripción es válida localmente
     * @return true si el status es ACTIVE o GRACE_PERIOD
     */
    fun isSubscriptionValid(): Boolean {
        return try {
            val status = getSubscriptionStatus()
            status?.status?.isValid() ?: false
        } catch (e: Exception) {
            Timber.e(e, "Error checking subscription validity")
            false
        }
    }

    /**
     * Verifica si la suscripción ha expirado
     * @return true si expiresAt < System.currentTimeMillis()
     */
    fun isSubscriptionExpired(): Boolean {
        return try {
            val status = getSubscriptionStatus()
            if (status != null) {
                status.expiresAt < System.currentTimeMillis()
            } else {
                true // Si no hay status, consideramos como expirada
            }
        } catch (e: Exception) {
            Timber.e(e, "Error checking subscription expiration")
            true
        }
    }

    /**
     * Actualiza solo el token JWT (sin alterar otros campos)
     * @param newToken Nuevo token JWT
     * @return true si se actualizó correctamente
     */
    fun updateAuthToken(newToken: String): Boolean {
        return try {
            val currentStatus = getSubscriptionStatus() ?: return false
            val updatedStatus = currentStatus.copy(jwtToken = newToken)
            saveSubscriptionStatus(updatedStatus)
        } catch (e: Exception) {
            Timber.e(e, "Error updating auth token")
            false
        }
    }

    /**
     * Actualiza solo el refresh token
     * @param newRefreshToken Nuevo refresh token
     * @return true si se actualizó correctamente
     */
    fun updateRefreshToken(newRefreshToken: String): Boolean {
        return try {
            val currentStatus = getSubscriptionStatus() ?: return false
            val updatedStatus = currentStatus.copy(refreshToken = newRefreshToken)
            saveSubscriptionStatus(updatedStatus)
        } catch (e: Exception) {
            Timber.e(e, "Error updating refresh token")
            false
        }
    }

    // ============================================================================
    // MÉTODOS DE UTILIDAD: Gestión de Auditoría
    // ============================================================================

    /**
     * Agrega un registro de auditoría a la lista
     * @param auditLog AuditLog a agregar
     * @return true si se agregó correctamente
     */
    fun addAuditLog(auditLog: AuditLog): Boolean {
        return try {
            val logs = getAuditLogs().toMutableList()
            logs.add(auditLog)
            
            // Mantener solo los últimos 1000 registros para no saturar el almacenamiento
            if (logs.size > 1000) {
                logs.removeRange(0, logs.size - 1000)
            }
            
            val json = gson.toJson(logs)
            saveString(AUDIT_LOG_KEY, json)
            Timber.d("AuditLog added: ${auditLog.auditId}")
            true
        } catch (e: Exception) {
            Timber.e(e, "Error adding audit log")
            false
        }
    }

    /**
     * Recupera todos los registros de auditoría
     * @return Lista de AuditLog, o lista vacía si no existen
     */
    fun getAuditLogs(): List<AuditLog> {
        return try {
            val json = getString(AUDIT_LOG_KEY)
            if (json.isEmpty()) {
                return emptyList()
            }
            val type = object : TypeToken<List<AuditLog>>() {}.type
            val logs = gson.fromJson<List<AuditLog>>(json, type)
            Timber.d("Retrieved ${logs.size} audit logs")
            logs
        } catch (e: JsonSyntaxException) {
            Timber.e(e, "Error deserializing audit logs - data may be corrupted")
            emptyList()
        } catch (e: Exception) {
            Timber.e(e, "Unexpected error retrieving audit logs")
            emptyList()
        }
    }

    /**
     * Limpia todos los registros de auditoría
     * @return true si se limpió correctamente
     */
    fun clearAuditLogs(): Boolean {
        return try {
            remove(AUDIT_LOG_KEY)
            Timber.d("Audit logs cleared")
            true
        } catch (e: Exception) {
            Timber.e(e, "Error clearing audit logs")
            false
        }
    }
}
