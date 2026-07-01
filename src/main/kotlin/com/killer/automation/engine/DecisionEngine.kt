package com.killer.automation.engine

import android.util.Log
import com.killer.automation.data.OfferData
import com.killer.automation.data.UserPreferences
import com.killer.automation.data.ZonePreference
import com.killer.automation.utils.EncryptedDataManager
import kotlin.math.*

/**
 * DecisionEngine - Motor de lógica local (Singleton)
 * Evalúa si una oferta debe ser aceptada basándose en los filtros locales del usuario
 */
class DecisionEngine private constructor(private val encryptedDataManager: EncryptedDataManager) {
    
    private var userPreferences: UserPreferences? = null
    private var evaluationHistory = mutableListOf<EvaluationResult>()
    
    init {
        Log.d(TAG, "DecisionEngine initialized")
        loadUserPreferences()
    }
    
    /**
     * Carga las preferencias del usuario desde almacenamiento encriptado
     */
    fun loadUserPreferences() {
        try {
            userPreferences = encryptedDataManager.getUserPreferences()
            Log.d(TAG, "User preferences loaded: \$userPreferences")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading user preferences: \${e.message}")
            userPreferences = UserPreferences.getDefaults()
        }
    }
    
    /**
     * Evaluación principal: determina si una oferta debe ser aceptada
     * @param offerData Datos de la oferta (precio, distancia, coordenadas, etc)
     * @return true si la oferta cumple todos los criterios, false si debe ser ignorada
     */
    fun isOfferAcceptable(offerData: OfferData): Boolean {
        return try {
            val prefs = userPreferences ?: run {
                Log.w(TAG, "User preferences not loaded, loading now...")
                loadUserPreferences()
                userPreferences ?: UserPreferences.getDefaults()
            }
            
            // Validación de precio mínimo
            if (!validateMinimumPrice(offerData.price, prefs)) {
                Log.d(TAG, "Offer rejected: Price \${offerData.price} below minimum \${prefs.minPrice}")
                recordEvaluation(offerData, false, "PRECIO_BAJO")
                return false
            }
            
            // Validación de precio por km
            if (!validatePricePerKm(offerData.price, offerData.distance, prefs)) {
                Log.d(TAG, "Offer rejected: Price/km \${offerData.price / offerData.distance} below threshold \${prefs.pricePerKm}")
                recordEvaluation(offerData, false, "PRECIO_POR_KM_BAJO")
                return false
            }
            
            // Validación de geofencing
            if (!validateGeofencing(offerData, prefs)) {
                Log.d(TAG, "Offer rejected: Location in blocked zone or not in preferred zone")
                recordEvaluation(offerData, false, "ZONA_BLOQUEADA")
                return false
            }
            
            // Validaciones adicionales opcionales
            if (!validateDistance(offerData.distance, prefs)) {
                Log.d(TAG, "Offer rejected: Distance \${offerData.distance} exceeds maximum \${prefs.maxDistance}")
                recordEvaluation(offerData, false, "DISTANCIA_EXCEDIDA")
                return false
            }
            
            Log.d(TAG, "Offer ACCEPTED: All criteria met")
            recordEvaluation(offerData, true, "ACEPTADA")
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "Error evaluating offer: \${e.message}", e)
            recordEvaluation(offerData, false, "ERROR_EVALUACION")
            false
        }
    }
    
    /**
     * Valida si el precio es mayor o igual al mínimo establecido
     */
    private fun validateMinimumPrice(price: Double, prefs: UserPreferences): Boolean {
        return price >= prefs.minPrice
    }
    
    /**
     * Valida el precio por km
     * Calcula (precio / distancia) y compara con el umbral
     */
    private fun validatePricePerKm(price: Double, distance: Double, prefs: UserPreferences): Boolean {
        if (distance <= 0) return false
        
        val pricePerKm = price / distance
        val threshold = prefs.pricePerKm
        
        Log.d(TAG, "Price per km check: \$pricePerKm >= \$threshold = \${pricePerKm >= threshold}")
        return pricePerKm >= threshold
    }
    
    /**
     * Valida la distancia máxima
     */
    private fun validateDistance(distance: Double, prefs: UserPreferences): Boolean {
        if (prefs.maxDistance <= 0) return true // Sin límite de distancia
        return distance <= prefs.maxDistance
    }
    
    /**
     * Valida geofencing:
     * - Rechaza si está en zona bloqueada
     * - Acepta si no hay zonas preferidas O está en una zona preferida
     * - Rechaza si hay zonas preferidas pero no está en ninguna
     */
    private fun validateGeofencing(offerData: OfferData, prefs: UserPreferences): Boolean {
        val currentLocation = offerData.currentLocation
        
        // Validación 1: Verificar zonas bloqueadas
        for (blockedZone in prefs.blockedZones) {
            if (isLocationInZone(currentLocation, blockedZone)) {
                Log.d(TAG, "Location is in blocked zone: \${blockedZone.name}")
                return false
            }
        }
        
        // Validación 2: Verificar zonas preferidas
        if (prefs.preferredZones.isNotEmpty()) {
            val isInPreferredZone = prefs.preferredZones.any { zone ->
                isLocationInZone(currentLocation, zone)
            }
            
            if (!isInPreferredZone) {
                Log.d(TAG, "Location is not in any preferred zone")
                return false
            }
        }
        
        return true
    }
    
    /**
     * Verifica si una ubicación está dentro de una zona usando distancia Haversine
     * @param location Ubicación actual (lat, lon)
     * @param zone Zona de referencia con centro y radio en km
     * @return true si la distancia al centro es menor que el radio de la zona
     */
    private fun isLocationInZone(
        location: Pair<Double, Double>,
        zone: ZonePreference
    ): Boolean {
        val distance = calculateHaversineDistance(
            location.first, location.second,
            zone.latitude, zone.longitude
        )
        
        val inZone = distance <= zone.radiusKm
        Log.d(TAG, "Zone check '\${zone.name}': distance=\$distance km, radius=\${zone.radiusKm} km, inZone=\$inZone")
        
        return inZone
    }
    
    /**
     * Calcula la distancia en km entre dos puntos usando la fórmula de Haversine
     * @return Distancia en kilómetros
     */
    private fun calculateHaversineDistance(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        val R = 6371 // Radio de la Tierra en km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        
        return R * c
    }
    
    /**
     * Registra el resultado de una evaluación para auditoría/análisis
     */
    private fun recordEvaluation(offerData: OfferData, accepted: Boolean, reason: String) {
        val result = EvaluationResult(
            timestamp = System.currentTimeMillis(),
            offerPrice = offerData.price,
            offerDistance = offerData.distance,
            accepted = accepted,
            reason = reason
        )
        
        evaluationHistory.add(result)
        
        // Mantener solo los últimos 1000 registros
        if (evaluationHistory.size > 1000) {
            evaluationHistory.removeAt(0)
        }
    }
    
    /**
     * Obtiene el historial de evaluaciones para análisis
     */
    fun getEvaluationHistory(): List<EvaluationResult> {
        return evaluationHistory.toList()
    }
    
    /**
     * Limpia el historial de evaluaciones
     */
    fun clearEvaluationHistory() {
        evaluationHistory.clear()
    }
    
    /**
     * Actualiza las preferencias del usuario
     */
    fun updateUserPreferences(preferences: UserPreferences) {
        userPreferences = preferences
        encryptedDataManager.saveUserPreferences(preferences)
        Log.d(TAG, "User preferences updated and saved")
    }
    
    /**
     * Obtiene las preferencias actuales del usuario
     */
    fun getUserPreferences(): UserPreferences? {
        return userPreferences
    }
    
    companion object {
        private const val TAG = "DecisionEngine"
        
        @Volatile
        private var instance: DecisionEngine? = null
        
        fun getInstance(encryptedDataManager: EncryptedDataManager): DecisionEngine {
            return instance ?: synchronized(this) {
                instance ?: DecisionEngine(encryptedDataManager).also { instance = it }
            }
        }
    }
    
    /**
     * Resultado de una evaluación de oferta
     */
    data class EvaluationResult(
        val timestamp: Long,
        val offerPrice: Double,
        val offerDistance: Double,
        val accepted: Boolean,
        val reason: String
    )
}
