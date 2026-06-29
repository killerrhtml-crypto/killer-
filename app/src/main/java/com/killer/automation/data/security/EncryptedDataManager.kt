package com.killer.automation.data.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import timber.log.Timber
import java.util.UUID

/**
 * Gestor de datos cifrados que utiliza androidx.security:security-crypto
 * Proporciona métodos para guardar y recuperar datos de forma segura
 * utilizando AES-256-GCM para encriptación y MasterKey del Keystore del sistema
 */
class EncryptedDataManager(
    private val context: Context,
    private val masterKey: MasterKey
) {

    private companion object {
        const val PREFS_FILE_NAME = "killer_secure_prefs"
        const val DB_PASSPHRASE_KEY = "db_passphrase_key"
        const val TOKEN_KEY = "auth_token"
        const val REFRESH_TOKEN_KEY = "refresh_token"
        const val USER_ID_KEY = "user_id"
        const val LAST_SYNC_KEY = "last_sync"
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
}
