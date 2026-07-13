package com.killer.automation.di

import com.killer.automation.data.security.EncryptedDataManager
import com.killer.automation.engine.DecisionEngine
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module that provides the DecisionEngine singleton.
 */
@Module
@InstallIn(SingletonComponent::class)
object EngineModule {

    @Provides
    @Singleton
    fun provideDecisionEngine(encryptedDataManager: EncryptedDataManager): DecisionEngine {
        // If DecisionEngine has an @Inject constructor accepting EncryptedDataManager,
        // Hilt could construct it automatically. We provide it explicitly here to ensure
        // the dependency is satisfied and to keep a single optimized instance.
        return DecisionEngine(encryptedDataManager)
    }
}
