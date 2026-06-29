package com.killer.automation.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * Base de datos Room cifrada con SQLCipher
 * Define todas las entidades y DAOs de la aplicación
 */
@Database(
    entities = [
        // Entidades serán añadidas en fases posteriores
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(DatabaseConverters::class)
abstract class KillerDatabase : RoomDatabase() {
    // DAOs serán definidos aquí en fases posteriores
}
