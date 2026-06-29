package com.killer.automation.data.database

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Convertidores de tipos para Room Database
 * Utilizados para serializar/deserializar tipos complejos
 */
object DatabaseConverters {

    private val gson = Gson()

    /**
     * Convierte un List<String> a JSON string
     */
    @TypeConverter
    fun fromStringList(list: List<String>?): String? {
        return list?.let { gson.toJson(it) }
    }

    /**
     * Convierte un JSON string a List<String>
     */
    @TypeConverter
    fun toStringList(json: String?): List<String>? {
        return json?.let {
            val type = object : TypeToken<List<String>>() {}.type
            gson.fromJson(it, type)
        }
    }

    /**
     * Convierte un Map<String, String> a JSON string
     */
    @TypeConverter
    fun fromStringMap(map: Map<String, String>?): String? {
        return map?.let { gson.toJson(it) }
    }

    /**
     * Convierte un JSON string a Map<String, String>
     */
    @TypeConverter
    fun toStringMap(json: String?): Map<String, String>? {
        return json?.let {
            val type = object : TypeToken<Map<String, String>>() {}.type
            gson.fromJson(it, type)
        }
    }

    /**
     * Convierte Long a milisegundos desde epoch
     */
    @TypeConverter
    fun fromTimestamp(value: Long?): Long? = value

    /**
     * Convierte timestamp a Long
     */
    @TypeConverter
    fun dateToTimestamp(value: Long?): Long? = value
}
