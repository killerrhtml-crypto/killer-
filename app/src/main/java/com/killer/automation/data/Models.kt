package com.killer.automation.data

import com.google.gson.annotations.SerializedName
import java.util.*

/**
 * Estructura de datos para una oferta de viaje (ride offer)
 * Contiene toda la información de un viaje disponible
 *
 * @property offerId Identificador único de la oferta
 * @property price Precio de la oferta en moneda local
 * @property distance Distancia estimada en kilómetros
 * @property estimatedTime Tiempo estimado en minutos
 * @property pickupLatitude Latitud del punto de recogida
 * @property pickupLongitude Longitud del punto de recogida
 * @property dropoffLatitude Latitud del punto de destino
 * @property dropoffLongitude Longitud del punto de destino
 * @property pickupAddress Dirección legible del punto de recogida
 * @property dropoffAddress Dirección legible del punto de destino
 * @property offerTimestamp Timestamp de cuando se generó la oferta
 * @property expirationTime Tiempo en milisegundos hasta que expira la oferta
 */
data class OfferData(
    @SerializedName("offer_id")
    val offerId: String,
    
    @SerializedName("price")
    val price: Double,
    
    @SerializedName("distance")
    val distance: Double,
    
    @SerializedName("estimated_time")
    val estimatedTime: Int,
    
    @SerializedName("pickup_lat")
    val pickupLatitude: Double,
    
    @SerializedName("pickup_lng")
    val pickupLongitude: Double,
    
    @SerializedName("dropoff_lat")
    val dropoffLatitude: Double,
    
    @SerializedName("dropoff_lng")
    val dropoffLongitude: Double,
    
    @SerializedName("pickup_address")
    val pickupAddress: String,
    
    @SerializedName("dropoff_address")
    val dropoffAddress: String,
    
    @SerializedName("offer_timestamp")
    val offerTimestamp: Long,
    
    @SerializedName("expiration_time")
    val expirationTime: Long
) {
    companion object {
        /**
         * Factory para crear una OfferData con valores por defecto (para testing)
         */
        fun empty() = OfferData(
            offerId = "",
            price = 0.0,
            distance = 0.0,
            estimatedTime = 0,
            pickupLatitude = 0.0,
            pickupLongitude = 0.0,
            dropoffLatitude = 0.0,
            dropoffLongitude = 0.0,
            pickupAddress = "",
            dropoffAddress = "",
            offerTimestamp = System.currentTimeMillis(),
            expirationTime = 0L
        )
    }
}

/**
 * Estructura de datos para las preferencias del usuario (chofer)
 * Define los filtros y criterios para aceptar/rechazar ofertas automáticamente
 *
 * @property userId Identificador único del usuario
 * @property minPrice Precio mínimo aceptable para una oferta
 * @property maxDistance Distancia máxima aceptable en kilómetros
 * @property maxEstimatedTime Tiempo máximo aceptable en minutos
 * @property searchRadius Radio de búsqueda en kilómetros desde la ubicación actual
 * @property preferredZones Lista de IDs de zonas preferidas
 * @property blockedZones Lista de IDs de zonas bloqueadas
 * @property autoBidEnabled Si está habilitada la aceptación automática de ofertas
 * @property bidDelayMs Retraso en milisegundos antes de aceptar automáticamente
 * @property maxActiveBids Número máximo de ofertas activas simultáneamente
 * @property isEnabled Si el usuario tiene habilitado el servicio de automatización
 * @property lastUpdated Timestamp de la última actualización
 */
data class UserPreferences(
    @SerializedName("user_id")
    val userId: String,
    
    @SerializedName("min_price")
    val minPrice: Double,
    
    @SerializedName("max_distance")
    val maxDistance: Double,
    
    @SerializedName("max_estimated_time")
    val maxEstimatedTime: Int,
    
    @SerializedName("search_radius")
    val searchRadius: Double,
    
    @SerializedName("preferred_zones")
    val preferredZones: List<String>,
    
    @SerializedName("blocked_zones")
    val blockedZones: List<String>,
    
    @SerializedName("auto_bid_enabled")
    val autoBidEnabled: Boolean,
    
    @SerializedName("bid_delay_ms")
    val bidDelayMs: Long,
    
    @SerializedName("max_active_bids")
    val maxActiveBids: Int,
    
    @SerializedName("is_enabled")
    val isEnabled: Boolean,
    
    @SerializedName("last_updated")
    val lastUpdated: Long
) {
    companion object {
        /**
         * Factory para crear UserPreferences con valores por defecto
         */
        fun default(userId: String) = UserPreferences(
            userId = userId,
            minPrice = 5.0,
            maxDistance = 50.0,
            maxEstimatedTime = 45,
            searchRadius = 10.0,
            preferredZones = emptyList(),
            blockedZones = emptyList(),
            autoBidEnabled = true,
            bidDelayMs = 500L,
            maxActiveBids = 3,
            isEnabled = true,
            lastUpdated = System.currentTimeMillis()
        )
    }
}

/**
 * Estructura de datos para una zona preferida o bloqueada
 * Define un área geográfica usando radio circular o polígono
 *
 * @property zoneId Identificador único de la zona
 * @property zoneName Nombre descriptivo de la zona
 * @property zoneType Tipo de zona: "CIRCLE" (radio circular) o "POLYGON" (polígono)
 * @property centerLatitude Latitud del centro (para zonas circulares)
 * @property centerLongitude Longitud del centro (para zonas circulares)
 * @property radiusKm Radio en kilómetros (para zonas circulares)
 * @property polygonPoints Lista de puntos del polígono (formato: "lat,lng;lat,lng;...")
 * @property isPreferred Si es true, la zona es PREFERIDA; si es false, es BLOQUEADA
 * @property isActive Si la zona está activa/habilitada
 * @property createdAt Timestamp de creación
 */
data class ZonePreference(
    @SerializedName("zone_id")
    val zoneId: String,
    
    @SerializedName("zone_name")
    val zoneName: String,
    
    @SerializedName("zone_type")
    val zoneType: ZoneType,
    
    @SerializedName("center_lat")
    val centerLatitude: Double,
    
    @SerializedName("center_lng")
    val centerLongitude: Double,
    
    @SerializedName("radius_km")
    val radiusKm: Double,
    
    @SerializedName("polygon_points")
    val polygonPoints: List<String>,
    
    @SerializedName("is_preferred")
    val isPreferred: Boolean,
    
    @SerializedName("is_active")
    val isActive: Boolean,
    
    @SerializedName("created_at")
    val createdAt: Long
) {
    companion object {
        /**
         * Factory para crear una zona circular
         */
        fun createCircle(
            zoneId: String,
            zoneName: String,
            centerLat: Double,
            centerLng: Double,
            radiusKm: Double,
            isPreferred: Boolean
        ) = ZonePreference(
            zoneId = zoneId,
            zoneName = zoneName,
            zoneType = ZoneType.CIRCLE,
            centerLatitude = centerLat,
            centerLongitude = centerLng,
            radiusKm = radiusKm,
            polygonPoints = emptyList(),
            isPreferred = isPreferred,
            isActive = true,
            createdAt = System.currentTimeMillis()
        )

        /**
         * Factory para crear una zona poligonal
         */
        fun createPolygon(
            zoneId: String,
            zoneName: String,
            polygonPoints: List<String>,
            isPreferred: Boolean
        ) = ZonePreference(
            zoneId = zoneId,
            zoneName = zoneName,
            zoneType = ZoneType.POLYGON,
            centerLatitude = 0.0,
            centerLongitude = 0.0,
            radiusKm = 0.0,
            polygonPoints = polygonPoints,
            isPreferred = isPreferred,
            isActive = true,
            createdAt = System.currentTimeMillis()
        )
    }
}

/**
 * Enum para los tipos de zonas geográficas
 */
enum class ZoneType {
    @SerializedName("CIRCLE")
    CIRCLE,
    
    @SerializedName("POLYGON")
    POLYGON
}

/**
 * Enum para los estados de suscripción del usuario
 *
 * @property ACTIVE La suscripción está activa y el usuario puede recibir ofertas
 * @property BLOCKED La suscripción está bloqueada (violación de términos, etc.)
 * @property EXPIRED La suscripción ha expirado
 * @property GRACE_PERIOD La suscripción está en período de gracia
 * @property SUSPENDED Suspensión temporal (por mantenimiento o problemas)
 */
enum class SubscriptionStatus {
    @SerializedName("ACTIVE")
    ACTIVE,
    
    @SerializedName("BLOCKED")
    BLOCKED,
    
    @SerializedName("EXPIRED")
    EXPIRED,
    
    @SerializedName("GRACE_PERIOD")
    GRACE_PERIOD,
    
    @SerializedName("SUSPENDED")
    SUSPENDED;

    /**
     * Determina si la suscripción es válida para recibir ofertas
     */
    fun isValid(): Boolean = this == ACTIVE || this == GRACE_PERIOD

    /**
     * Obtiene una descripción legible del estado
     */
    fun getDescription(): String = when (this) {
        ACTIVE -> "Suscripción activa"
        BLOCKED -> "Suscripción bloqueada"
        EXPIRED -> "Suscripción expirada"
        GRACE_PERIOD -> "Período de gracia"
        SUSPENDED -> "Suscripción suspendida"
    }
}

/**
 * Estructura de datos para el estado de suscripción del usuario
 * Incluye información sobre validez, tokens y timestamps
 *
 * @property userId Identificador único del usuario
 * @property status Estado actual de la suscripción
 * @property activatedAt Timestamp de activación de la suscripción
 * @property expiresAt Timestamp de expiración
 * @property lastValidatedAt Timestamp de la última validación remota
 * @property jwtToken Token JWT actual del usuario
 * @property refreshToken Token de refresco para obtener nuevos JWT
 * @property isLocallyValid Si es válida localmente (basado en timestamp)
 */
data class SubscriptionStatusData(
    @SerializedName("user_id")
    val userId: String,
    
    @SerializedName("status")
    val status: SubscriptionStatus,
    
    @SerializedName("activated_at")
    val activatedAt: Long,
    
    @SerializedName("expires_at")
    val expiresAt: Long,
    
    @SerializedName("last_validated_at")
    val lastValidatedAt: Long,
    
    @SerializedName("jwt_token")
    val jwtToken: String,
    
    @SerializedName("refresh_token")
    val refreshToken: String,
    
    @SerializedName("is_locally_valid")
    val isLocallyValid: Boolean
) {
    companion object {
        /**
         * Factory para crear un estado de suscripción vacío
         */
        fun empty() = SubscriptionStatusData(
            userId = "",
            status = SubscriptionStatus.EXPIRED,
            activatedAt = 0L,
            expiresAt = 0L,
            lastValidatedAt = 0L,
            jwtToken = "",
            refreshToken = "",
            isLocallyValid = false
        )
    }
}

/**
 * Estructura de datos para el resultado de la evaluación de una oferta
 * Usada internamente por DecisionEngine (NO se serializa directamente)
 *
 * @property offerId ID de la oferta evaluada
 * @property shouldAccept Si se debe aceptar la oferta
 * @property matchScore Puntuación de coincidencia (0-100)
 * @property rejectionReason Razón del rechazo (si aplica)
 * @property evaluatedAt Timestamp de la evaluación
 */
data class OfferEvaluationResult(
    val offerId: String,
    val shouldAccept: Boolean,
    val matchScore: Int,
    val rejectionReason: String? = null,
    val evaluatedAt: Long = System.currentTimeMillis()
)

/**
 * Estructura de datos para auditoría de decisiones
 * Registra cada decisión tomada por el sistema
 *
 * @property auditId ID único del registro
 * @property userId ID del usuario
 * @property offerId ID de la oferta
 * @property decision ACCEPTED o REJECTED
 * @property reason Razón detallada de la decisión
 * @property matchScore Puntuación de coincidencia
 * @property timestamp Timestamp de la decisión
 */
data class AuditLog(
    @SerializedName("audit_id")
    val auditId: String,
    
    @SerializedName("user_id")
    val userId: String,
    
    @SerializedName("offer_id")
    val offerId: String,
    
    @SerializedName("decision")
    val decision: String,
    
    @SerializedName("reason")
    val reason: String,
    
    @SerializedName("match_score")
    val matchScore: Int,
    
    @SerializedName("timestamp")
    val timestamp: Long
) {
    companion object {
        /**
         * Factory para crear un registro de auditoría
         */
        fun createAccepted(
            userId: String,
            offerId: String,
            reason: String,
            matchScore: Int
        ) = AuditLog(
            auditId = UUID.randomUUID().toString(),
            userId = userId,
            offerId = offerId,
            decision = "ACCEPTED",
            reason = reason,
            matchScore = matchScore,
            timestamp = System.currentTimeMillis()
        )

        /**
         * Factory para crear un registro de rechazo
         */
        fun createRejected(
            userId: String,
            offerId: String,
            reason: String,
            matchScore: Int
        ) = AuditLog(
            auditId = UUID.randomUUID().toString(),
            userId = userId,
            offerId = offerId,
            decision = "REJECTED",
            reason = reason,
            matchScore = matchScore,
            timestamp = System.currentTimeMillis()
        )
    }
}
