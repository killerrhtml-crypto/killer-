package com.killer.automation.core.overlay

import android.content.Context
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import timber.log.Timber

/**
 * Vista personalizada para el widget flotante
 * Maneja eventos de toque y arrastre (drag-and-drop)
 * Esta clase es una alternativa a usar ComposeView
 */
class FloatingWidgetView(
    context: Context,
    private val windowManager: WindowManager,
    private val onTapped: (Float, Float) -> Unit
) : View(context) {

    private var lastX = 0f
    private var lastY = 0f
    private var offsetX = 0f
    private var offsetY = 0f
    private var isDragging = false
    private val dragThreshold = 5 // píxeles mínimos para considerar arrastre

    companion object {
        private const val TAG = "FloatingWidgetView"
    }

    init {
        setBackgroundResource(android.R.color.transparent)
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return event?.let { motionEvent ->
            when (motionEvent.action) {
                MotionEvent.ACTION_DOWN -> {
                    handleTouchDown(motionEvent)
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    handleTouchMove(motionEvent)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    handleTouchUp(motionEvent)
                    true
                }
                else -> false
            }
        } ?: false
    }

    /**
     * Maneja el inicio del evento de toque
     */
    private fun handleTouchDown(event: MotionEvent) {
        lastX = event.rawX
        lastY = event.rawY
        offsetX = 0f
        offsetY = 0f
        isDragging = false
        Timber.d("$TAG: Toque iniciado en (${event.rawX}, ${event.rawY})")
    }

    /**
     * Maneja el movimiento del toque (arrastre)
     */
    private fun handleTouchMove(event: MotionEvent) {
        offsetX = event.rawX - lastX
        offsetY = event.rawY - lastY

        // Verificar si el movimiento es suficiente para considerarlo arrastre
        if (!isDragging && (Math.abs(offsetX) > dragThreshold || Math.abs(offsetY) > dragThreshold)) {
            isDragging = true
            Timber.d("$TAG: Arrastre iniciado")
        }

        if (isDragging) {
            // Actualizar posición del widget
            val layoutParams = layoutParams as WindowManager.LayoutParams
            layoutParams.x = (layoutParams.x + offsetX.toInt()).coerceAtLeast(0)
            layoutParams.y = (layoutParams.y + offsetY.toInt()).coerceAtLeast(0)

            try {
                windowManager.updateViewLayout(this, layoutParams)
                lastX = event.rawX
                lastY = event.rawY
            } catch (e: Exception) {
                Timber.e(e, "$TAG: Error actualizando posición")
            }
        }
    }

    /**
     * Maneja el fin del evento de toque
     */
    private fun handleTouchUp(event: MotionEvent) {
        if (!isDragging) {
            // Si no hay arrastre, se considera un toque (clic)
            Timber.i("$TAG: Widget tocado en (${event.rawX}, ${event.rawY})")
            onTapped(event.rawX, event.rawY)
        } else {
            Timber.d("$TAG: Arrastre finalizado")
        }
        isDragging = false
    }
}
