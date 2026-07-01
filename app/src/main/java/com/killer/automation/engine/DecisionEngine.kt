package com.killer.automation.engine

import com.killer.automation.data.*
import com.killer.automation.data.security.EncryptedDataManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import kotlin.math.*

/**
 * DecisionEngine es el corazón del sistema de automatización.
 * Responsable de evaluar ofertas contra preferencias del usuario y tomar decisiones.
 *
 * Características:
 * - Validación de precio, distancia y tiempo estimado
 * - Geofencing inteligente (soporta zonas circulares y poligonales)
 * - Optimización con Bounding Box + Haversine/RayCasting
 * - Cálculo de Match Score para analíticas
 * - Auditoría completa de decisiones
 * - Validación de suscripción antes de procesar
 * - Coroutines para cálculos intensivos
 *
 * Nota: Esta clase es un POJO puro (sin referencias a Android Context)
 * para permitir testing unitario completo.
 */
class DecisionEngine(
    private val encryptedDataManager: EncryptedDataManager
) {

    companion object {
        // Constantes para Haversine (radio de la Tierra en km)
        private const val EARTH_RADIUS_KM = 6371.0

        // Constantes para optimización
        private const val BOUNDING_BOX_MARGIN_KM = 0.015  // ~1.5 km en grados
    }

    /**
     * Método principal: Evalúa una oferta completa
     * @param offer Oferta a evaluar
     * @return OfferEvaluationResult con decisión y detalles
     */
    suspend fun evaluateOffer(offer: OfferData): OfferEvaluationResult {
        return withContext(Dispatchers.Default) {
            try {
                Timber.d("Evaluating offer: ${offer.offerId}")

                // 1. Validar suscripción
                if (!encryptedDataManager.isSubscriptionValid()) {
                    Timber.w("Offer evaluation blocked: invalid subscription")
                    return@withContext OfferEvaluationResult(
                        offerId = offer.offerId,
                        shouldAccept = false,
                        matchScore = 0,
                        rejectionReason = "SUBSCRIPTION_INVALID"
                    )
                }

                // 2. Obtener preferencias del usuario
                val userPreferences = encryptedDataManager.getUserPreferences()
                if (userPreferences == null) {
                    Timber.w("Offer evaluation blocked: no user preferences found")
                    return@withContext OfferEvaluationResult(
                        offerId = offer.offerId,
                        shouldAccept = false,
                        matchScore = 0,
                        rejectionReason = "NO_USER_PREFERENCES"
                    )
                }

                // 3. Validar si debe aceptar
                val shouldAccept = shouldAcceptOffer(offer, userPreferences)

                // 4. Calcular match score
                val matchScore = calculateMatchScore(offer, userPreferences)

                // 5. Obtener razón de rechazo (si aplica)
                val rejectionReason = if (!shouldAccept) {
                    getRejectionReason(offer, userPreferences)
                } else {
                    null
                }

                // 6. Registrar en auditoría
                val auditLog = if (shouldAccept) {
                    AuditLog.createAccepted(
                        userId = userPreferences.userId,
                        offerId = offer.offerId,
                        reason = "Offer matches all criteria",
                        matchScore = matchScore
                    )
                } else {
                    AuditLog.createRejected(
                        userId = userPreferences.userId,
                        offerId = offer.offerId,
                        reason = rejectionReason ?: "Unknown reason",
                        matchScore = matchScore
                    )
                }
                encryptedDataManager.addAuditLog(auditLog)

                // 7. Retornar resultado
                OfferEvaluationResult(
                    offerId = offer.offerId,
                    shouldAccept = shouldAccept,
                    matchScore = matchScore,
                    rejectionReason = rejectionReason
                )
            } catch (e: Exception) {
                Timber.e(e, "Error evaluating offer: ${offer.offerId}")
                OfferEvaluationResult(
                    offerId = offer.offerId,
                    shouldAccept = false,
                    matchScore = 0,
                    rejectionReason = "EVALUATION_ERROR: ${e.message}"
                )
            }
        }
    }

    /**
     * Valida si una oferta debe ser aceptada según las preferencias del usuario
     * @param offer Oferta a validar
     * @param prefs Preferencias del usuario
     * @return true si cumple todos los criterios, false si falla alguno
     */
    suspend fun shouldAcceptOffer(offer: OfferData, prefs: UserPreferences): Boolean {
        return withContext(Dispatchers.Default) {
            try {
                // 1. Validar precio
                if (offer.price < prefs.minPrice) {
                    Timber.d("Offer ${offer.offerId} rejected: price too low (${offer.price} < ${prefs.minPrice})")
                    return@withContext false
                }

                // 2. Validar distancia
                if (offer.distance > prefs.maxDistance) {
                    Timber.d("Offer ${offer.offerId} rejected: distance too high (${offer.distance} > ${prefs.maxDistance})")
                    return@withContext false
                }

                // 3. Validar tiempo estimado
                if (offer.estimatedTime > prefs.maxEstimatedTime) {
                    Timber.d("Offer ${offer.offerId} rejected: time too high (${offer.estimatedTime} > ${prefs.maxEstimatedTime})")
                    return@withContext false
                }

                // 4. Obtener zonas del usuario
                val zones = encryptedDataManager.getZones()

                // 5. Validar geofencing: rechazar si está en zona bloqueada
                if (isOfferInBlockedZone(offer, zones)) {
                    Timber.d("Offer ${offer.offerId} rejected: offer in blocked zone")
                    return@withContext false
                }

                // 6. Si hay zonas preferidas, la oferta debe estar en una de ellas
                val preferredZones = zones.filter { it.isPreferred && it.isActive }
                if (preferredZones.isNotEmpty()) {
                    if (!isOfferInPreferredZone(offer, preferredZones)) {
                        Timber.d("Offer ${offer.offerId} rejected: offer not in any preferred zone")
                        return@withContext false
                    }
                }

                // 7. Validar que la oferta no haya expirado
                if (isOfferExpired(offer)) {
                    Timber.d("Offer ${offer.offerId} rejected: offer expired")
                    return@withContext false
                }

                Timber.d("Offer ${offer.offerId} accepted: all criteria met")
                true
            } catch (e: Exception) {
                Timber.e(e, "Error validating offer: ${offer.offerId}")
                false
            }
        }
    }

    /**
     * Calcula un score de coincidencia (0-100) basado en qué tan cerca está la oferta del ideal
     * Útil para analíticas y tomar decisiones secundarias
     *
     * Factores considerados:
     * - Precio: cuánto menos del máximo (mejor)
     * - Distancia: cuánto menos del máximo (mejor)
     * - Tiempo: cuánto menos del máximo (mejor)
     * - Bonus: zona preferida
     *
     * @param offer Oferta a evaluar
     * @param prefs Preferencias del usuario
     * @return Score entre 0-100
     */
    suspend fun calculateMatchScore(offer: OfferData, prefs: UserPreferences): Int {
        return withContext(Dispatchers.Default) {
            try {
                var score = 100

                // Factor de precio: penalizar si está cerca del mínimo
                val priceScore = ((offer.price - prefs.minPrice) / prefs.minPrice * 20).coerceIn(0.0, 20.0)
                score -= (20 - priceScore).toInt()

                // Factor de distancia: penalizar si está cerca del máximo
                val distanceRatio = offer.distance / prefs.maxDistance
                val distanceScore = (distanceRatio * 30).coerceIn(0.0, 30.0)
                score -= (30 - distanceScore).toInt()

                // Factor de tiempo: penalizar si está cerca del máximo
                val timeRatio = offer.estimatedTime.toDouble() / prefs.maxEstimatedTime
                val timeScore = (timeRatio * 20).coerceIn(0.0, 20.0)
                score -= (20 - timeScore).toInt()

                // Bonus: si está en una zona preferida
                val zones = encryptedDataManager.getZones()
                if (isOfferInPreferredZone(offer, zones.filter { it.isPreferred })) {
                    score += 10
                }

                // Bonus: si la distancia es muy pequeña (oferta cercana)
                if (offer.distance < 2.0) {
                    score += 10
                }

                // Asegurar que el score esté entre 0 y 100
                score.coerceIn(0, 100)
            } catch (e: Exception) {
                Timber.e(e, "Error calculating match score for offer: ${offer.offerId}")
                0
            }
        }
    }

    /**
     * Verifica si una oferta está en una zona preferida
     * Soporta tanto zonas circulares como poligonales
     *
     * @param offer Oferta a verificar
     * @param preferredZones Zonas preferidas del usuario
     * @return true si la oferta está en al menos una zona preferida
     */
    suspend fun isOfferInPreferredZone(offer: OfferData, preferredZones: List<ZonePreference>): Boolean {
        return withContext(Dispatchers.Default) {
            if (preferredZones.isEmpty()) return@withContext true

            try {
                for (zone in preferredZones) {
                    val isInZone = when (zone.zoneType) {
                        ZoneType.CIRCLE -> isPointInCircle(
                            offer.pickupLatitude,
                            offer.pickupLongitude,
                            zone.centerLatitude,
                            zone.centerLongitude,
                            zone.radiusKm
                        )
                        ZoneType.POLYGON -> isPointInPolygon(
                            offer.pickupLatitude,
                            offer.pickupLongitude,
                            zone.polygonPoints
                        )
                    }
                    if (isInZone) {
                        Timber.d("Offer ${offer.offerId} is in preferred zone: ${zone.zoneName}")
                        return@withContext true
                    }
                }
                Timber.d("Offer ${offer.offerId} is NOT in any preferred zone")
                false
            } catch (e: Exception) {
                Timber.e(e, "Error checking preferred zones for offer: ${offer.offerId}")
                false
            }
        }
    }

    /**
     * Verifica si una oferta está en una zona bloqueada
     * Soporta tanto zonas circulares como poligonales
     *
     * @param offer Oferta a verificar
     * @param allZones Todas las zonas del usuario
     * @return true si la oferta está en al menos una zona bloqueada
     */
    suspend fun isOfferInBlockedZone(offer: OfferData, allZones: List<ZonePreference>): Boolean {
        return withContext(Dispatchers.Default) {
            val blockedZones = allZones.filter { !it.isPreferred && it.isActive }
            if (blockedZones.isEmpty()) return@withContext false

            try {
                for (zone in blockedZones) {
                    val isInZone = when (zone.zoneType) {
                        ZoneType.CIRCLE -> isPointInCircle(
                            offer.pickupLatitude,
                            offer.pickupLongitude,
                            zone.centerLatitude,
                            zone.centerLongitude,
                            zone.radiusKm
                        )
                        ZoneType.POLYGON -> isPointInPolygon(
                            offer.pickupLatitude,
                            offer.pickupLongitude,
                            zone.polygonPoints
                        )
                    }
                    if (isInZone) {
                        Timber.d("Offer ${offer.offerId} is in blocked zone: ${zone.zoneName}")
                        return@withContext true
                    }
                }
                Timber.d("Offer ${offer.offerId} is NOT in any blocked zone")
                false
            } catch (e: Exception) {
                Timber.e(e, "Error checking blocked zones for offer: ${offer.offerId}")
                false
            }
        }
    }

    /**
     * Obtiene la razón del rechazo de una oferta
     * @param offer Oferta rechazada
     * @param prefs Preferencias del usuario
     * @return String con la razón específica del rechazo
     */
    private suspend fun getRejectionReason(offer: OfferData, prefs: UserPreferences): String {
        return withContext(Dispatchers.Default) {
            when {
                offer.price < prefs.minPrice -> 
                    "PRICE_TOO_LOW: $${offer.price} < $${prefs.minPrice}"
                
                offer.distance > prefs.maxDistance -> 
                    "DISTANCE_TOO_HIGH: ${offer.distance}km > ${prefs.maxDistance}km"
                
                offer.estimatedTime > prefs.maxEstimatedTime -> 
                    "TIME_TOO_HIGH: ${offer.estimatedTime}min > ${prefs.maxEstimatedTime}min"
                
                isOfferInBlockedZone(offer, encryptedDataManager.getZones()) -> 
                    "IN_BLOCKED_ZONE"
                
                isOfferExpired(offer) -> 
                    "OFFER_EXPIRED"
                
                else -> 
                    "UNKNOWN_REASON"
            }
        }
    }

    /**
     * Verifica si una oferta ha expirado
     * @param offer Oferta a verificar
     * @return true si el timestamp actual > offerTimestamp + expirationTime
     */
    private fun isOfferExpired(offer: OfferData): Boolean {
        val currentTime = System.currentTimeMillis()
        val expirationTime = offer.offerTimestamp + offer.expirationTime
        return currentTime > expirationTime
    }

    // ============================================================================
    // MÉTODOS PRIVADOS: Cálculos Geoespaciales Optimizados
    // ============================================================================

    /**
     * Verifica si un punto está dentro de una zona circular
     * Optimización: Primero Bounding Box, luego Haversine
     *
     * @param pointLat Latitud del punto (pickup de la oferta)
     * @param pointLng Longitud del punto
     * @param centerLat Latitud del centro de la zona
     * @param centerLng Longitud del centro de la zona
     * @param radiusKm Radio de la zona en km
     * @return true si el punto está dentro del círculo
     */
    private fun isPointInCircle(
        pointLat: Double,
        pointLng: Double,
        centerLat: Double,
        centerLng: Double,
        radiusKm: Double
    ): Boolean {
        // 1. Filtro rápido: Bounding Box (evita cálculos Haversine innecesarios)
        val marginDegrees = BOUNDING_BOX_MARGIN_KM / EARTH_RADIUS_KM * (180.0 / PI)
        
        val latInBounds = abs(pointLat - centerLat) <= marginDegrees * (radiusKm / 1.5)
        val lngInBounds = abs(pointLng - centerLng) <= marginDegrees * (radiusKm / 1.5)
        
        if (!latInBounds || !lngInBounds) {
            return false
        }

        // 2. Cálculo exacto: Fórmula de Haversine
        val distance = haversineDistance(pointLat, pointLng, centerLat, centerLng)
        return distance <= radiusKm
    }

    /**
     * Verifica si un punto está dentro de un polígono
     * Usa el algoritmo Ray Casting (eficiente y preciso)
     *
     * @param pointLat Latitud del punto
     * @param pointLng Longitud del punto
     * @param polygonPoints Lista de puntos del polígono en formato "lat,lng;lat,lng;..."
     * @return true si el punto está dentro del polígono
     */
    private fun isPointInPolygon(
        pointLat: Double,
        pointLng: Double,
        polygonPoints: List<String>
    ): Boolean {
        if (polygonPoints.size < 3) {
            Timber.w("Polygon has less than 3 points, cannot calculate")
            return false
        }

        return try {
            // Parsear los puntos del polígono
            val vertices = polygonPoints.map { point ->
                val parts = point.split(",")
                Pair(parts[0].toDouble(), parts[1].toDouble()) // Pair<lat, lng>
            }

            // Aplicar algoritmo Ray Casting
            rayCasting(pointLat, pointLng, vertices)
        } catch (e: Exception) {
            Timber.e(e, "Error parsing polygon points")
            false
        }
    }

    /**
     * Algoritmo Ray Casting para detectar si un punto está dentro de un polígono
     * Lanza un rayo infinito desde el punto y cuenta intersecciones con los lados
     *
     * @param pointLat Latitud del punto
     * @param pointLng Longitud del punto
     * @param vertices Lista de vértices del polígono (Pair<lat, lng>)
     * @return true si el punto está dentro del polígono
     */
    private fun rayCasting(
        pointLat: Double,
        pointLng: Double,
        vertices: List<Pair<Double, Double>>
    ): Boolean {
        var inside = false
        var p1Lat = vertices.last().first
        var p1Lng = vertices.last().second

        for (i in vertices.indices) {
            val p2Lat = vertices[i].first
            val p2Lng = vertices[i].second

            if (pointLng > minOf(p1Lng, p2Lng)) {
                if (pointLng <= maxOf(p1Lng, p2Lng)) {
                    if (pointLat <= maxOf(p1Lat, p2Lat)) {
                        if (p1Lng != p2Lng) {
                            val xinters = (pointLng - p1Lng) * (p2Lat - p1Lat) / (p2Lng - p1Lng) + p1Lat
                            if (p1Lat == p2Lat || pointLat <= xinters) {
                                inside = !inside
                            }
                        }
                    }
                }
            }
            p1Lat = p2Lat
            p1Lng = p2Lng
        }

        return inside
    }

    /**
     * Calcula la distancia entre dos puntos usando la fórmula de Haversine
     * Retorna la distancia en kilómetros
     *
     * Fórmula de Haversine:
     * a = sin²(Δlat/2) + cos(lat1) * cos(lat2) * sin²(Δlon/2)
     * c = 2 * atan2(√a, √(1−a))
     * d = R * c
     *
     * @param lat1 Latitud del primer punto
     * @param lng1 Longitud del primer punto
     * @param lat2 Latitud del segundo punto
     * @param lng2 Longitud del segundo punto
     * @return Distancia en kilómetros
     */
    private fun haversineDistance(
        lat1: Double,
        lng1: Double,
        lat2: Double,
        lng2: Double
    ): Double {
        val deltaLat = Math.toRadians(lat2 - lat1)
        val deltaLng = Math.toRadians(lng2 - lng1)

        val a = sin(deltaLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(deltaLng / 2).pow(2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return EARTH_RADIUS_KM * c
    }
}
