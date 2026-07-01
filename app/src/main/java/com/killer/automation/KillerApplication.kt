package com.killer.automation

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

/**
 * Application class anotada con @HiltAndroidApp para inicializar Hilt
 * y configurar los componentes globales de la aplicación.
 */
@HiltAndroidApp
class KillerApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        initializeLogging()
    }

    /**
     * Inicializa el sistema de logging con Timber
     */
    private fun initializeLogging() {
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
            Timber.d("Killer Automation Application initialized in DEBUG mode")
        } else {
            // En producción, usar un árbol personalizado sin logs sensitivos
            Timber.plant(ReleaseTree())
            Timber.i("Killer Automation Application initialized in RELEASE mode")
        }
    }

    /**
     * Árbol de logging personalizado para modo Release
     * Solo registra errores críticos, no información sensitiva
     */
    private class ReleaseTree : Timber.Tree() {
        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            // En producción, solo loguear errores
            if (priority >= android.util.Log.ERROR) {
                // Aquí se podría enviar a un servicio de crash reporting
                // como Firebase Crashlytics
            }
        }
    }
}
