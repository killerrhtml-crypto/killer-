package com.killer.automation.core.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.graphics.Path
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.killer.automation.core.auth.AuthBroadcastReceiver
import com.killer.automation.core.auth.AuthManager
import com.killer.automation.core.gesture.GestureInterceptor
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import timber.log.Timber
import javax.inject.Inject
import kotlin.random.Random

/**
 * Servicio de Accesibilidad para automatización de Android
 * Anotado con @AndroidEntryPoint para inyección de dependencias con Hilt
 * Escucha eventos de accesibilidad y ejecuta gestos programáticos
 * 
 * AHORA CON VALIDACIÓN DE AUTENTICACIÓN:
 * - Inyecta AuthManager
 * - Verifica isTokenValid() antes de procesar gestos
 * - Se detiene automáticamente si el token expira
 * - Escucha broadcasts de TOKEN_EXPIRED y AUTH_FAILED
 */
@AndroidEntryPoint
class AutomationAccessibilityService : AccessibilityService() {

    @Inject
    lateinit var gestureInterceptor: GestureInterceptor

    @Inject
    lateinit var authManager: AuthManager

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var isServiceActive = false
    private var authBroadcastReceiver: AuthBroadcastReceiver? = null

    companion object {
        private const val TAG = "AutomationAccessibilityService"
        private const val JITTER_RADIUS = 3f // ±3px para simular entrada humana
        private const val GESTURE_DURATION_CLICK = 50L
        private const val GESTURE_DURATION_SWIPE = 300L
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        isServiceActive = true
        configureAccessibilityServiceInfo()
        registerAuthBroadcastReceiver()
        Timber.i("$TAG: Servicio de accesibilidad conectado")
    }

    /**
     * Registra el receptor de broadcast para eventos de autenticación
     */
    private fun registerAuthBroadcastReceiver() {
        try {
            authBroadcastReceiver = AuthBroadcastReceiver(
                onTokenExpired = {
                    Timber.w("$TAG: Token expirado - Deteniendo servicio")
                    disableSelf()
                },
                onAuthFailed = { reason ->
                    Timber.e("$TAG: Autenticación fallida ($reason) - Deteniendo servicio")
                    disableSelf()
                }
            )
            authBroadcastReceiver?.register(this)
        } catch (e: Exception) {
            Timber.e(e, "$TAG: Error registrando receptor de broadcast")
        }
    }

    /**
     * Configura los parámetros del servicio de accesibilidad
     * Define qué eventos de accesibilidad desea escuchar
     */
    private fun configureAccessibilityServiceInfo() {
        val info = AccessibilityServiceInfo().apply {
            // Tipos de eventos a escuchar
            eventTypes = AccessibilityEvent.TYPE_VIEW_CLICKED or
                    AccessibilityEvent.TYPE_TOUCH_INTERACTION_START or
                    AccessibilityEvent.TYPE_TOUCH_INTERACTION_END or
                    AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED

            // Tipo de feedback (si aplica)
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC

            // Notificaciones para eventos importantes
            notificationTimeout = 100

            // Permisos requeridos
            flags = AccessibilityServiceInfo.FLAG_REQUEST_ENHANCED_WEB_ACCESSIBILITY or
                    AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_REQUEST_TOUCH_EXPLORATION_MODE
        }

        serviceInfo = info
        Timber.d("$TAG: Configuración de accesibilidad aplicada")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!isServiceActive || event == null) return

        serviceScope.launch {
            try {
                // IMPORTANTE: Validar autenticación antes de procesar cualquier evento
                if (!authManager.isTokenValid()) {
                    Timber.w("$TAG: Token inválido - Deteniendo servicio")
                    disableSelf()
                    return@launch
                }

                Timber.d("$TAG: Evento recibido - Tipo: ${event.eventType}")
                gestureInterceptor.handleAccessibilityEvent(event)
            } catch (e: Exception) {
                Timber.e(e, "$TAG: Error procesando evento de accesibilidad")
            }
        }
    }

    override fun onInterrupt() {
        isServiceActive = false
        authBroadcastReceiver?.unregister(this)
        serviceScope.cancel()
        Timber.i("$TAG: Servicio de accesibilidad interrumpido")
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceActive = false
        authBroadcastReceiver?.unregister(this)
        serviceScope.cancel()
        Timber.i("$TAG: Servicio de accesibilidad destruido")
    }

    /**
     * Ejecuta un gesto con jitter aleatorio para simular entrada humana
     * El jitter añade variación aleatoria de ±3px a las coordenadas
     *
     * @param path Ruta del gesto (Path con coordenadas)
     * @param duration Duración del gesto en milisegundos
     * @param jitterRadius Radio del jitter en píxeles (por defecto ±3px)
     */
    fun dispatchGestureWithJitter(
        path: Path,
        duration: Long = GESTURE_DURATION_SWIPE,
        jitterRadius: Float = JITTER_RADIUS
    ) {
        if (!isServiceActive) {
            Timber.w("$TAG: Intento de ejecutar gesto sin servicio activo")
            return
        }

        try {
            val jitteredGesture = createJitteredGesture(path, duration, jitterRadius)
            dispatchGesture(jitteredGesture) { success ->
                if (success) {
                    Timber.d("$TAG: Gesto ejecutado exitosamente")
                } else {
                    Timber.w("$TAG: Fallo al ejecutar gesto")
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "$TAG: Error despachando gesto con jitter")
        }
    }

    /**
     * Crea un gesto con jitter aplicado a las coordenadas
     * El jitter es una variación aleatoria de ±3px que simula entrada humana imprecisa
     *
     * @param originalPath Ruta original sin jitter
     * @param duration Duración del gesto
     * @param jitterRadius Radio del jitter en píxeles
     * @return GestureDescription con jitter aplicado
     */
    private fun createJitteredGesture(
        originalPath: Path,
        duration: Long,
        jitterRadius: Float
    ): GestureDescription {
        // Crear copia de la ruta para aplicar transformación
        val jitteredPath = Path(originalPath)

        // Aplicar jitter aleatorio en rango [-jitterRadius, +jitterRadius]
        val random = Random.Default
        val jitterX = random.nextFloat() * jitterRadius * 2 - jitterRadius
        val jitterY = random.nextFloat() * jitterRadius * 2 - jitterRadius

        Timber.d("$TAG: Jitter aplicado - X: $jitterX, Y: $jitterY")
        jitteredPath.offset(jitterX, jitterY)

        // Crear GestureDescription con la ruta jitterizada
        return GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(jitteredPath, 0, duration))
            .build()
    }

    /**
     * Ejecuta un clic con jitter en coordenadas específicas
     * Simula un toque humano impreciso
     *
     * @param x Coordenada X del clic
     * @param y Coordenada Y del clic
     * @param jitterRadius Radio del jitter (por defecto ±3px)
     */
    fun performClickWithJitter(
        x: Float,
        y: Float,
        jitterRadius: Float = JITTER_RADIUS
    ) {
        if (!isServiceActive) {
            Timber.w("$TAG: Intento de hacer clic sin servicio activo")
            return
        }

        try {
            val random = Random.Default
            val jitterX = random.nextFloat() * jitterRadius * 2 - jitterRadius
            val jitterY = random.nextFloat() * jitterRadius * 2 - jitterRadius

            val clickPath = Path().apply {
                moveTo(x + jitterX, y + jitterY)
            }

            Timber.d("$TAG: Ejecutando clic en ($x, $y) con jitter ($jitterX, $jitterY)")
            dispatchGestureWithJitter(clickPath, duration = GESTURE_DURATION_CLICK, jitterRadius)
        } catch (e: Exception) {
            Timber.e(e, "$TAG: Error ejecutando clic con jitter")
        }
    }

    /**
     * Ejecuta un swipe (deslizamiento) con jitter
     * El swipe es un gesto continuo desde un punto de inicio a uno final
     *
     * @param startX Coordenada X de inicio
     * @param startY Coordenada Y de inicio
     * @param endX Coordenada X de fin
     * @param endY Coordenada Y de fin
     * @param duration Duración del swipe en milisegundos
     * @param jitterRadius Radio del jitter
     */
    fun performSwipeWithJitter(
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        duration: Long = GESTURE_DURATION_SWIPE,
        jitterRadius: Float = JITTER_RADIUS
    ) {
        if (!isServiceActive) {
            Timber.w("$TAG: Intento de hacer swipe sin servicio activo")
            return
        }

        try {
            val swipePath = Path().apply {
                moveTo(startX, startY)
                lineTo(endX, endY)
            }

            Timber.d("$TAG: Ejecutando swipe de ($startX,$startY) a ($endX,$endY)")
            dispatchGestureWithJitter(swipePath, duration, jitterRadius)
        } catch (e: Exception) {
            Timber.e(e, "$TAG: Error ejecutando swipe con jitter")
        }
    }

    /**
     * Ejecuta un scroll (desplazamiento) con múltiples puntos interpolados
     * Crea un movimiento suave simulando scroll natural
     *
     * @param startX Coordenada X de inicio
     * @param startY Coordenada Y de inicio
     * @param endX Coordenada X de fin
     * @param endY Coordenada Y de fin
     * @param steps Número de pasos interpolados (default 10)
     * @param duration Duración total del scroll
     */
    fun performScrollWithJitter(
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        steps: Int = 10,
        duration: Long = GESTURE_DURATION_SWIPE
    ) {
        if (!isServiceActive) {
            Timber.w("$TAG: Intento de hacer scroll sin servicio activo")
            return
        }

        try {
            val scrollPath = Path().apply {
                moveTo(startX, startY)

                // Interpolar puntos intermedios para suavidad
                val stepX = (endX - startX) / steps
                val stepY = (endY - startY) / steps

                repeat(steps) { i ->
                    val nextX = startX + (stepX * (i + 1))
                    val nextY = startY + (stepY * (i + 1))
                    lineTo(nextX, nextY)
                }
            }

            Timber.d("$TAG: Ejecutando scroll de ($startX,$startY) a ($endX,$endY) en $steps pasos")
            dispatchGestureWithJitter(scrollPath, duration)
        } catch (e: Exception) {
            Timber.e(e, "$TAG: Error ejecutando scroll con jitter")
        }
    }

    /**
     * Ejecuta un gesto multitoque (pinch, doble tap, etc.)
     * Permite gestos más complejos con múltiples toques simultáneos
     *
     * @param paths Lista de rutas para cada dedo
     * @param duration Duración del gesto
     */
    fun performMultiTouchGesture(
        paths: List<Path>,
        duration: Long = GESTURE_DURATION_SWIPE
    ) {
        if (!isServiceActive || paths.isEmpty()) {
            Timber.w("$TAG: Intento de gesto multitoque inválido")
            return
        }

        try {
            val gestureBuilder = GestureDescription.Builder()

            paths.forEach { path ->
                gestureBuilder.addStroke(GestureDescription.StrokeDescription(path, 0, duration))
            }

            val gesture = gestureBuilder.build()
            dispatchGesture(gesture) { success ->
                if (success) {
                    Timber.d("$TAG: Gesto multitoque ejecutado exitosamente")
                } else {
                    Timber.w("$TAG: Fallo al ejecutar gesto multitoque")
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "$TAG: Error ejecutando gesto multitoque")
        }
    }

    /**
     * Obtiene el AccessibilityNodeInfo raíz de la ventana actual
     * Útil para navegar la jerarquía de vistas
     *
     * @return AccessibilityNodeInfo raíz o null
     */
    fun getRootNode(): AccessibilityNodeInfo? {
        return rootInActiveWindow?.let { root ->
            Timber.d("$TAG: Nodo raíz obtenido")
            root
        }
    }

    /**
     * Busca un nodo específico por su contenido de texto
     * Util para ubicar vistas específicas en la jerarquía
     *
     * @param text Texto a buscar
     * @return AccessibilityNodeInfo o null si no encuentra
     */
    fun findNodeByText(text: String): AccessibilityNodeInfo? {
        val root = getRootNode() ?: return null

        val queue = mutableListOf(root)
        while (queue.isNotEmpty()) {
            val node = queue.removeAt(0)
            val nodeText = node.text?.toString() ?: ""

            if (nodeText.contains(text, ignoreCase = true)) {
                Timber.d("$TAG: Nodo encontrado con texto: '$text'")
                return node
            }

            repeat(node.childCount) { i ->
                node.getChild(i)?.let { child ->
                    queue.add(child)
                }
            }
        }

        Timber.w("$TAG: Nodo no encontrado con texto: '$text'")
        return null
    }
}
