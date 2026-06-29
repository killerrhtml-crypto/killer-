package com.killer.automation.di

import android.content.Context
import android.view.WindowManager
import com.killer.automation.core.gesture.GestureInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Módulo Hilt que proporciona dependencias para servicios de accesibilidad
 */
@Module
@InstallIn(SingletonComponent::class)
object AccessibilityModule {

    /**
     * Proporciona GestureInterceptor como Singleton
     * Responsable de interceptar y manejar eventos de accesibilidad
     */
    @Singleton
    @Provides
    fun provideGestureInterceptor(): GestureInterceptor {
        return GestureInterceptor()
    }
}
