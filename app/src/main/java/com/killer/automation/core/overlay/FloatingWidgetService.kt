package com.killer.automation.core.overlay

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import com.killer.automation.core.gesture.GestureInterceptor
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Servicio que gestiona una ventana flotante (overlay) persistente
 * Usa WindowManager para mostrar un widget sobre otras aplicaciones
 * La ventana es draggable y puede iniciar automatización al ser tocada
 */
@AndroidEntryPoint
class FloatingWidgetService : Service() {

    @Inject
    lateinit var windowManager: WindowManager

    @Inject
    lateinit var gestureInterceptor: GestureInterceptor

    private var composeView: ComposeView? = null
    private var params: WindowManager.LayoutParams? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())

    companion object {
        private const val TAG = "FloatingWidgetService"
        private const val INITIAL_X = 0
        private const val INITIAL_Y = 100
        private const val WIDGET_SIZE = 100
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.i("$TAG: Servicio iniciado")
        createFloatingWidget()
        return START_STICKY
    }

    /**
     * Crea el widget flotante y lo agrega al WindowManager
     */
    private fun createFloatingWidget() {
        try {
            // Crear ComposeView para renderizar la UI
            composeView = ComposeView(this).apply {
                setContent {
                    FloatingWidgetContent(
                        onWidgetTapped = { x, y ->
                            serviceScope.launch {
                                gestureInterceptor.onFloatingWidgetTapped(x, y)
                            }
                        }
                    )
                }
            }

            // Configurar parámetros de la ventana
            params = WindowManager.LayoutParams().apply {
                type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_PHONE
                }

                format = PixelFormat.TRANSLUCENT
                flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS

                gravity = Gravity.TOP or Gravity.START
                width = WIDGET_SIZE
                height = WIDGET_SIZE
                x = INITIAL_X
                y = INITIAL_Y

                // Transparencia y opciones de visualización
                alpha = 0.95f
            }

            // Agregar vista al WindowManager
            windowManager.addView(composeView, params)
            Timber.i("$TAG: Widget flotante creado exitosamente")
        } catch (e: Exception) {
            Timber.e(e, "$TAG: Error creando widget flotante")
        }
    }

    /**
     * Actualiza la posición del widget en la pantalla
     *
     * @param x Nueva posición X
     * @param y Nueva posición Y
     */
    private fun updateWidgetPosition(x: Int, y: Int) {
        params?.apply {
            this.x = x
            this.y = y
            try {
                windowManager.updateViewLayout(composeView, this)
                Timber.d("$TAG: Posición actualizada a ($x, $y)")
            } catch (e: Exception) {
                Timber.e(e, "$TAG: Error actualizando posición del widget")
            }
        }
    }

    /**
     * Elimina el widget de la pantalla
     */
    private fun removeWidget() {
        try {
            composeView?.let { windowManager.removeView(it) }
            Timber.i("$TAG: Widget flotante eliminado")
        } catch (e: Exception) {
            Timber.e(e, "$TAG: Error eliminando widget")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        removeWidget()
        serviceScope.cancel()
        Timber.i("$TAG: Servicio destruido")
    }
}

/**
 * Composable que renderiza el contenido del widget flotante
 * Incluye lógica de drag-and-drop y visualización
 */
@Composable
private fun FloatingWidgetContent(
    onWidgetTapped: (Float, Float) -> Unit
) {
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    Box(
        modifier = Modifier
            .size(100.dp)
            .background(
                color = Color(0xFF1976D2),
                shape = CircleShape
            )
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        offsetX += dragAmount.x
                        offsetY += dragAmount.y
                        Timber.d("Widget arrastrado a ($offsetX, $offsetY)")
                    },
                    onDragEnd = {
                        Timber.d("Arrastre finalizado en ($offsetX, $offsetY)")
                    }
                )
            }
            .pointerInput(Unit) {
                // Detectar toque sin arrastre (clic)
                // Este será mejorado en versiones posteriores
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.PlayArrow,
            contentDescription = "Play Automation",
            tint = Color.White,
            modifier = Modifier.size(50.dp)
        )
    }
}
