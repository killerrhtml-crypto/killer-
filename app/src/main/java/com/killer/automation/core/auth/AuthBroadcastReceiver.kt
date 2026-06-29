package com.killer.automation.core.auth

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import timber.log.Timber

/**
 * Receptor de Broadcast para eventos de autenticación
 * Escucha señales de token expirado y autenticación fallida
 */
class AuthBroadcastReceiver(
    private val onTokenExpired: () -> Unit,
    private val onAuthFailed: (reason: String) -> Unit
) : BroadcastReceiver() {

    companion object {
        private const val TAG = "AuthBroadcastReceiver"
        const val ACTION_TOKEN_EXPIRED = "com.killer.automation.auth.TOKEN_EXPIRED"
        const val ACTION_AUTH_FAILED = "com.killer.automation.auth.AUTH_FAILED"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        when (intent.action) {
            ACTION_TOKEN_EXPIRED -> {
                Timber.w("$TAG: Token expirado - Deteniendo servicio")
                onTokenExpired()
            }
            ACTION_AUTH_FAILED -> {
                val reason = intent.getStringExtra("reason") ?: "Desconocido"
                Timber.e("$TAG: Autenticación fallida - Razón: $reason")
                onAuthFailed(reason)
            }
        }
    }

    /**
     * Registra el receptor de broadcast
     */
    fun register(context: Context) {
        try {
            val intentFilter = IntentFilter().apply {
                addAction(ACTION_TOKEN_EXPIRED)
                addAction(ACTION_AUTH_FAILED)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(
                    this,
                    intentFilter,
                    Context.RECEIVER_NOT_EXPORTED
                )
            } else {
                @Suppress("UnspecifiedRegisterReceiverFlag")
                context.registerReceiver(this, intentFilter)
            }

            Timber.d("$TAG: Receptor registrado")
        } catch (e: Exception) {
            Timber.e(e, "$TAG: Error registrando receptor")
        }
    }

    /**
     * Desregistra el receptor de broadcast
     */
    fun unregister(context: Context) {
        try {
            context.unregisterReceiver(this)
            Timber.d("$TAG: Receptor desregistrado")
        } catch (e: Exception) {
            Timber.e(e, "$TAG: Error desregistrando receptor")
        }
    }
}
