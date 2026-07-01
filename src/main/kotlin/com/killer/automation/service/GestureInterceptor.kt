package com.killer.automation.service

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.killer.automation.data.OfferData
import com.killer.automation.engine.DecisionEngine
import com.killer.automation.manager.SubscriptionManager
import com.killer.automation.utils.EncryptedDataManager

/**
 * GestureInterceptor - Servicio de Accesibilidad mejorado
 * Intercepta eventos y aplica la lógica de DecisionEngine y SubscriptionManager
 */
class GestureInterceptor : AccessibilityService() {
    
    private lateinit var subscriptionManager: SubscriptionManager
    private lateinit var decisionEngine: DecisionEngine
    private lateinit var encryptedDataManager: EncryptedDataManager
    
    private var isRunning = false
    
    override fun onCreate() {
        super.onCreate()
        
        encryptedDataManager = EncryptedDataManager(this)
        subscriptionManager = SubscriptionManager.getInstance(this)
        decisionEngine = DecisionEngine.getInstance(encryptedDataManager)
        
        Log.d(TAG, "GestureInterceptor created")
    }
    
    override fun onServiceConnected() {
        super.onServiceConnected()
        isRunning = true
        
        // Sincronizar suscripción al conectar
        subscriptionManager.synchronizeSubscription()
        
        Log.i(TAG, "GestureInterceptor service connected")
    }
    
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!isRunning) {
            Log.w(TAG, "Service not running")
            return
        }
        
        // VALIDACIÓN 1: Verificar si la suscripción está activa
        if (!subscriptionManager.isAutomationEnabled()) {
            Log.w(TAG, "Automation disabled - Subscription: \${subscriptionManager.getSubscriptionStatus()}")
            return
        }
        
        event?.let {
            when (event.eventType) {
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                    handleWindowContentChanged(event)
                }
                
                AccessibilityEvent.TYPE_VIEW_CLICKED -> {
                    handleViewClicked(event)
                }
                
                else -> {
                    // Ignorar otros eventos
                }
            }
        }
    }
    
    override fun onInterrupt() {
        isRunning = false
        Log.i(TAG, "GestureInterceptor interrupted")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        subscriptionManager.cleanup()
        Log.i(TAG, "GestureInterceptor destroyed")
    }
    
    /**
     * Maneja cambios en el contenido de la ventana (detección de ofertas)
     */
    private fun handleWindowContentChanged(event: AccessibilityEvent) {
        try {
            // Extraer datos de la oferta de la interfaz (implementar según tu app de viajes)
            val offerData = extractOfferData(event)
            
            if (offerData != null) {
                processOffer(offerData, event)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling window content: \${e.message}", e)
        }
    }
    
    /**
     * Maneja clicks en la interfaz
     */
    private fun handleViewClicked(event: AccessibilityEvent) {
        try {
            Log.d(TAG, "View clicked: \${event.source?.viewIdResourceName}")
            // Puede usarse para auditoría o análisis
        } catch (e: Exception) {
            Log.e(TAG, "Error handling view click: \${e.message}", e)
        }
    }
    
    /**
     * Procesa una oferta usando el DecisionEngine
     */
    private fun processOffer(offerData: OfferData, event: AccessibilityEvent) {
        Log.d(TAG, "Processing offer: \${offerData.offerId}")
        
        // VALIDACIÓN 2: Aplicar lógica local del DecisionEngine
        val isAcceptable = decisionEngine.isOfferAcceptable(offerData)
        
        if (isAcceptable) {
            Log.i(TAG, "Offer accepted: \${offerData.offerId} - Executing action")
            // Ejecutar la acción de aceptar la oferta
            executeOfferAcceptance(offerData, event)
        } else {
            Log.d(TAG, "Offer rejected: \${offerData.offerId} - Ignoring event")
            // No hacer nada - ignorar el evento
        }
    }
    
    /**
     * Ejecuta la acción de aceptar una oferta
     */
    private fun executeOfferAcceptance(offerData: OfferData, event: AccessibilityEvent) {
        try {
            // Buscar el botón de aceptar en la jerarquía de accesibilidad
            val rootNode = rootInActiveWindow ?: return
            
            val acceptButton = findAcceptButton(rootNode)
            if (acceptButton != null) {
                Log.i(TAG, "Found accept button, performing action")
                performGlobalAction(GLOBAL_ACTION_BACK) // Ejemplo de acción
                acceptButton.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            } else {
                Log.w(TAG, "Accept button not found in current view")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error executing offer acceptance: \${e.message}", e)
        }
    }
    
    /**
     * Busca el botón de aceptar en la jerarquía de nodos
     */
    private fun findAcceptButton(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        // Búsqueda por texto común en apps de viajes (Adapt según tu app)
        val acceptTexts = listOf(
            "aceptar", "accept", "confirmar", "confirm",
            "tomar viaje", "take ride", "next"
        )
        
        if (acceptTexts.any { node.text?.contains(it, ignoreCase = true) == true }) {
            return node
        }
        
        // Búsqueda recursiva en hijos
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                val found = findAcceptButton(child)
                if (found != null) return found
            }
        }
        
        return null
    }
    
    /**
     * Extrae datos de la oferta de la interfaz
     * NOTA: Implementar según la estructura de tu app de viajes específica
     */
    private fun extractOfferData(event: AccessibilityEvent): OfferData? {
        return try {
            // Esta es una implementación de ejemplo
            // Adaptar según tu app real
            
            val source = event.source ?: return null
            
            // Buscar elementos específicos en la UI (precio, distancia, etc)
            val priceText = findNodeByResourceId(source, "price_text")?.text?.toString()
            val distanceText = findNodeByResourceId(source, "distance_text")?.text?.toString()
            
            if (priceText == null || distanceText == null) {
                return null
            }
            
            val price = priceText.replace("[^0-9.]".toRegex(), "").toDoubleOrNull() ?: return null
            val distance = distanceText.replace("[^0-9.]".toRegex(), "").toDoubleOrNull() ?: return null
            
            // Obtener ubicación actual (implementar con LocationManager)
            val currentLocation = Pair(0.0, 0.0) // TODO: Obtener ubicación real
            
            OfferData(
                offerId = "OFFER-\${System.currentTimeMillis()}",
                price = price,
                distance = distance,
                currentLocation = currentLocation
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting offer data: \${e.message}", e)
            null
        }
    }
    
    /**
     * Busca un nodo por su ID de recurso
     */
    private fun findNodeByResourceId(
        node: AccessibilityNodeInfo?,
        resourceId: String
    ): AccessibilityNodeInfo? {
        if (node == null) return null
        
        if (node.viewIdResourceName?.endsWith(resourceId) == true) {
            return node
        }
        
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            val found = findNodeByResourceId(child, resourceId)
            if (found != null) return found
        }
        
        return null
    }
    
    companion object {
        private const val TAG = "GestureInterceptor"
    }
}
