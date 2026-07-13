package com.killer.automation.di

import android.content.Context
import android.view.WindowManager
import androidx.security.crypto.MasterKey
import com.killer.automation.data.security.EncryptedDataManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
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

}
