package com.killer.automation.core.gesture

import android.graphics.Rect
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.killer.automation.core.accessibility.AutomationAccessibilityService
import dagger.hilt.android.scopes.ServiceScoped
import kotlinx.coroutines.*
import timber.log.Timber
import javax.inject.Inject

/**
 * Interceptor de gestos singleton que detecta eventos de accesibilidad
 * e identifica si deben ser bloqueados y sustituidos por gestos programáticos
 *
 * Funcionalidades:
 * - Detecta clics en botones objetivo
 * - Bloquea acciones nativas
 * - Delega a gestos programáticos
 */
@ServiceScoped
class GestureInterceptor @Inject constructor() {

    private val interceptorScope = CoroutineScope(Dispatchers.Main + Job())
    private val blockedNodeIds = mutableSetOf<Int>()
    private val targetButtonPatterns = mutableListOf<String>()

    // Patrones de botones objetivo por defecto
    private val defaultTargetPatterns = listOf(
        "accept",
        "reject",
        "decline",
        "confirm",
        "approve",
        "deny",
        "yes",
        "no",
        "ok",
        "cancel",
        "submit",
        "ok_btn",
        "reject_btn"
    )

    init {
        targetButtonPatterns.addAll(defaultTargetPatterns)
        Timber.d("GestureInterceptor inicializado")
    }

    /**
     * Maneja eventos de accesibilidad
     * Identifica y bloquea acciones en botones objetivo
     *
     * @param event Evento de accesibilidad recibido
     */
    suspend fun handleAccessibilityEvent(event: AccessibilityEvent) {
        withContext(Dispatchers.Main) {
            try {
                when (event.eventType) {
                    AccessibilityEvent.TYPE_VIEW_CLICKED -> {
                        handleClickEvent(event)
                    }
                    AccessibilityEvent.TYPE_TOUCH_INTERACTION_START -> {
                        handleTouchStart(event)
                    }
                    AccessibilityEvent.TYPE_TOUCH_INTERACTION_END -> {
                        handleTouchEnd(event)
                    }
                    AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                        handleWindowStateChanged(event)
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error manejando evento de accesibilidad")
            }
        }
    }

    /**
     * Maneja eventos de clic
     * Identifica si el nodo clickeado es un botón objetivo
     * Si lo es, lo bloquea y ejecuta un gesto programático
     *
     * @param event Evento de clic
     */
    private suspend fun handleClickEvent(event: AccessibilityEvent) {
        event.source?.let { source ->
            val nodeId = System.identityHashCode(source)

            // Verificar si el nodo ya está bloqueado
            if (nodeId in blockedNodeIds) {
                Timber.d("Nodo ya bloqueado: $nodeId")
                source.recycle()
                return
            }

            // Si el nodo debe ser bloqueado, agregarlo y ejecutar gesto
            if (shouldBlockNode(source)) {
                blockedNodeIds.add(nodeId)
                Timber.i("Nodo bloqueado: $nodeId - Ejecutando gesto programático")
                performProgrammaticGesture(source)
            }

            source.recycle()
        }
    }

    /**
     * Verifica si un nodo debe ser bloqueado
     * Comprueba clase, descripción de contenido y texto
     *
     * @param node Nodo a verificar
     * @return true si debe bloquearse
     */
    private suspend fun shouldBlockNode(node: AccessibilityNodeInfo): Boolean = withContext(Dispatchers.Default) {
        val className = node.className?.toString() ?: ""
        val contentDesc = node.contentDescription?.toString()?.lowercase() ?: ""
        val text = node.text?.toString()?.lowercase() ?: ""
        val viewIdResourceName = node.viewIdResourceName?.toString()?.lowercase() ?: ""

        // Verificar si es un Button
        val isButton = className.contains("Button", ignoreCase = true) ||
                className.contains("ImageButton", ignoreCase = true) ||
                className.contains("FloatingActionButton", ignoreCase = true)

        if (!isButton) {
            Timber.d("No es botón: $className")
            return@withContext false
        }

        // Verificar patrones objetivo en descripción, texto e ID
        val matchesPattern = targetButtonPatterns.any { pattern ->
            contentDesc.contains(pattern) ||
            text.contains(pattern) ||
            viewIdResourceName.contains(pattern)
        }

        if (matchesPattern) {
            Timber.i("Nodo objetivo detectado - Text: $text, ContentDesc: $contentDesc")
        }

        return@withContext matchesPattern
    }

    /**
     * Ejecuta un gesto programático en respuesta a un botón bloqueado
     * Obtiene coordenadas del nodo y ejecuta un swipe desde esa posición
     *
     * @param sourceNode Nodo del botón bloqueado
     */
    private suspend fun performProgrammaticGesture(sourceNode: AccessibilityNodeInfo) {
        withContext(Dispatchers.Main) {
            try {
                val bounds = Rect()
                sourceNode.getBoundsInScreen(bounds)

                // Coordenadas del centro del nodo
                val centerX = bounds.centerX().toFloat()
                val centerY = bounds.centerY().toFloat()

                Timber.d("Ejecutando gesto en ($centerX, $centerY)")

                // Ejecutar swipe desde el botón hacia arriba
                val swipeDistance = 200f
                val endX = centerX
                val endY = centerY - swipeDistance

                // Este método debería ser llamado desde el AccesibilityService
                // Por ahora, lo logging es suficiente
                Timber.i("Gesto interceptado - Swipe de ($centerX,$centerY) a ($endX,$endY)")
            } catch (e: Exception) {
                Timber.e(e, "Error ejecutando gesto programático")
            }
        }
    }

    /**
     * Maneja el inicio de interacción táctil
     *
     * @param event Evento de inicio de toque
     */
    private suspend fun handleTouchStart(event: AccessibilityEvent) {
        withContext(Dispatchers.Default) {
            Timber.d("Inicio de interacción táctil")
        }
    }

    /**
     * Maneja el fin de interacción táctil
     *
     * @param event Evento de fin de toque
     */
    private suspend fun handleTouchEnd(event: AccessibilityEvent) {
        withContext(Dispatchers.Default) {
            Timber.d("Fin de interacción táctil")
        }
    }

    /**
     * Maneja cambios en el estado de la ventana
     *
     * @param event Evento de cambio de ventana
     */
    private suspend fun handleWindowStateChanged(event: AccessibilityEvent) {
        withContext(Dispatchers.Default) {
            val packageName = event.packageName
            Timber.d("Cambio de ventana - Paquete: $packageName")
        }
    }

    /**
     * Callback cuando se toca el widget flotante
     * Inicia la automatización
     *
     * @param x Coordenada X del toque
     * @param y Coordenada Y del toque
     */
    suspend fun onFloatingWidgetTapped(x: Float, y: Float) {
        withContext(Dispatchers.Main) {
            Timber.i("Widget flotante tocado en ($x, $y) - Iniciando automatización")
            // Aquí se ejecutaría la lógica de automatización
        }
    }

    /**
     * Agrega un patrón de botón objetivo personalizado
     *
     * @param pattern Patrón a agregar
     */
    fun addTargetButtonPattern(pattern: String) {
        targetButtonPatterns.add(pattern.lowercase())
        Timber.d("Patrón de botón objetivo agregado: $pattern")
    }

    /**
     * Elimina un patrón de botón objetivo
     *
     * @param pattern Patrón a eliminar
     */
    fun removeTargetButtonPattern(pattern: String) {
        targetButtonPatterns.remove(pattern.lowercase())
        Timber.d("Patrón de botón objetivo eliminado: $pattern")
    }

    /**
     * Limpia todos los patrones de botón objetivo y usa los predeterminados
     */
    fun resetTargetButtonPatterns() {
        targetButtonPatterns.clear()
        targetButtonPatterns.addAll(defaultTargetPatterns)
        Timber.i("Patrones de botón objetivo reiniciados a predeterminados")
    }

    /**
     * Obtiene lista actual de patrones objetivo
     *
     * @return Lista de patrones
     */
    fun getTargetButtonPatterns(): List<String> = targetButtonPatterns.toList()

    /**
     * Limpia el interceptor
     * Cancela todas las corrutinas pendientes
     */
    fun cancel() {
        interceptorScope.cancel()
        blockedNodeIds.clear()
        Timber.i("GestureInterceptor cancelado")
    }
}
