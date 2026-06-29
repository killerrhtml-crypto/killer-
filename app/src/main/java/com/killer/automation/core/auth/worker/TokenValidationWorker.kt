package com.killer.automation.core.auth.worker

import android.content.Context
import androidx.work.*
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import com.killer.automation.core.auth.AuthManager
import java.util.concurrent.TimeUnit

/**
 * Worker para validación periódica de token en background
 * Ejecuta una validación remota cada 30 minutos
 */
class TokenValidationWorker(
    context: Context,
    params: WorkerParameters,
    private val authManager: AuthManager
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "TokenValidationWorker"
        private const val VALIDATION_INTERVAL_MINUTES = 30L
        const val TAG_TOKEN_VALIDATION = "token_validation"

        /**
         * Programa una tarea de validación periódica
         */
        fun scheduleTokenValidation(context: Context) {
            try {
                val tokenValidationRequest = PeriodicWorkRequestBuilder<TokenValidationWorker>(
                    VALIDATION_INTERVAL_MINUTES,
                    TimeUnit.MINUTES
                )
                    .setBackoffCriteria(
                        BackoffPolicy.EXPONENTIAL,
                        5,
                        TimeUnit.MINUTES
                    )
                    .addTag(TAG_TOKEN_VALIDATION)
                    .build()

                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    TAG_TOKEN_VALIDATION,
                    ExistingPeriodicWorkPolicy.KEEP,
                    tokenValidationRequest
                )

                Timber.i("$TAG: Validación periódica programada cada $VALIDATION_INTERVAL_MINUTES minutos")
            } catch (e: Exception) {
                Timber.e(e, "$TAG: Error programando validación periódica")
            }
        }

        /**
         * Cancela la tarea de validación periódica
         */
        fun cancelTokenValidation(context: Context) {
            try {
                WorkManager.getInstance(context).cancelUniqueWork(TAG_TOKEN_VALIDATION)
                Timber.i("$TAG: Validación periódica cancelada")
            } catch (e: Exception) {
                Timber.e(e, "$TAG: Error cancelando validación periódica")
            }
        }
    }

    override suspend fun doWork(): Result {
        return try {
            Timber.i("$TAG: Ejecutando validación de token")

            val isValid = authManager.isTokenValid()

            if (isValid) {
                Timber.i("$TAG: Token válido - Continuando")
                Result.success()
            } else {
                Timber.w("$TAG: Token inválido - Deteniendo servicio")
                Result.retry()
            }
        } catch (e: Exception) {
            Timber.e(e, "$TAG: Error en validación de token")
            Result.retry()
        }
    }
}
