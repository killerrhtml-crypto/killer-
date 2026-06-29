package com.killer.automation.di

import android.content.Context
import com.killer.automation.core.auth.AuthInterceptor
import com.killer.automation.core.auth.AuthManager
import com.killer.automation.core.auth.AuthService
import com.killer.automation.data.security.EncryptedDataManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Módulo Hilt para inyección de dependencias de autenticación
 */
@Module
@InstallIn(SingletonComponent::class)
object AuthModule {

    private const val API_BASE_URL = "https://api.killer-automation.com/"
    private const val CONNECT_TIMEOUT_SECONDS = 30L
    private const val READ_TIMEOUT_SECONDS = 30L
    private const val WRITE_TIMEOUT_SECONDS = 30L

    /**
     * Proporciona AuthInterceptor como Singleton
     * Interceptor que agrega el JWT token a las peticiones
     */
    @Singleton
    @Provides
    fun provideAuthInterceptor(authManager: AuthManager): AuthInterceptor {
        return AuthInterceptor(authManager)
    }

    /**
     * Proporciona OkHttpClient configurado con interceptor de autenticación
     */
    @Singleton
    @Provides
    fun provideOkHttpClient(authInterceptor: AuthInterceptor): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Proporciona Retrofit configurado con OkHttpClient
     */
    @Singleton
    @Provides
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    /**
     * Proporciona AuthService (interfaz Retrofit)
     */
    @Singleton
    @Provides
    fun provideAuthService(retrofit: Retrofit): AuthService {
        return retrofit.create(AuthService::class.java)
    }

    /**
     * Proporciona AuthManager como Singleton
     * Responsable de gestión de autenticación y validación de tokens
     */
    @Singleton
    @Provides
    fun provideAuthManager(
        @ApplicationContext context: Context,
        encryptedDataManager: EncryptedDataManager,
        authService: AuthService
    ): AuthManager {
        return AuthManager(context, encryptedDataManager, authService)
    }
}
