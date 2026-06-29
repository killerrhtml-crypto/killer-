package com.killer.automation.di

import android.content.Context
import android.view.WindowManager
import androidx.room.Room
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.killer.automation.data.database.KillerDatabase
import com.killer.automation.data.security.EncryptedDataManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.zetetic.database.sqlcipher.SupportFactory
import javax.inject.Singleton

/**
 * Módulo de Hilt que proporciona dependencias globales
 * Anotado con @InstallIn(SingletonComponent::class) para que las
 * dependencias sean Singletons en toda la aplicación
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /**
     * Proporciona WindowManager como Singleton
     * Usado para operaciones de overlay y ventanas flotantes
     */
    @Singleton
    @Provides
    fun provideWindowManager(
        @ApplicationContext context: Context
    ): WindowManager {
        return context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    /**
     * Proporciona MasterKey para cifrado de EncryptedSharedPreferences
     * El MasterKey es generado y almacenado de forma segura en el Keystore del sistema
     */
    @Singleton
    @Provides
    fun provideMasterKey(
        @ApplicationContext context: Context
    ): MasterKey {
        return MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    /**
     * Proporciona EncryptedDataManager como Singleton
     * Gestiona el almacenamiento cifrado de datos sensibles
     */
    @Singleton
    @Provides
    fun provideEncryptedDataManager(
        @ApplicationContext context: Context,
        masterKey: MasterKey
    ): EncryptedDataManager {
        return EncryptedDataManager(context, masterKey)
    }

    /**
     * Proporciona la base de datos Room cifrada con SQLCipher
     * Requiere EncryptedDataManager para obtener la contraseña de cifrado
     */
    @Singleton
    @Provides
    fun provideDatabase(
        @ApplicationContext context: Context,
        encryptedDataManager: EncryptedDataManager
    ): KillerDatabase {
        // Obtener o generar la contraseña de cifrado
        val passphrase = encryptedDataManager.getOrGenerateDatabasePassphrase()
        
        // Crear factory de SQLCipher
        val factory = SupportFactory(passphrase.toByteArray())
        
        return Room.databaseBuilder(
            context.applicationContext,
            KillerDatabase::class.java,
            "killer_automation.db"
        )
            .openHelperFactory(factory)
            .fallbackToDestructiveMigration()
            .build()
    }
}
