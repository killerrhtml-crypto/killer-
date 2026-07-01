package com.killer.automation.data

import com.google.gson.annotations.SerializedName

/**
 * Datos de una oferta de viaje
 */
data class OfferData(
    @SerializedName("offer_id")
    val offerId: String,
    
    @SerializedName("price")
    val price: Double,
    
    @SerializedName("distance")
    val distance: Double,
    
    @SerializedName("current_location")
    val currentLocation: Pair<Double, Double>, // Latitude, Longitude
    
    @SerializedName("destination_location")
    val destinationLocation: Pair<Double, Double>? = null,
    
    @SerializedName("pickup_location")
    val pickupLocation: Pair<Double, Double>? = null,
    
    @SerializedName("zone")
    val zone: String? = null,
    
    @SerializedName("destination")
    val destination: String? = null,
    
    @SerializedName("timestamp")
    val timestamp: Long = System.currentTimeMillis(),
    
    @SerializedName("extra_info")
    val extraInfo: Map<String, String> = emptyMap()
) {
    companion object {
        /**
         * Crea un OfferData de ejemplo para testing
         */
        fun createExample(): OfferData {
            return OfferData(
                offerId = "OFFER-001",
                price = 350.0,
                distance = 8.5,
                currentLocation = Pair(18.4861, -69.9312), // Santo Domingo
                zone = "Centro Histórico",
                destination = "Aeropuerto"
            )
        }
    }
}

/**
 * Preferencias del usuario para filtrado de ofertas
 */
data class UserPreferences(
    @SerializedName("min_price")
    val minPrice: Double = 50.0,
    
    @SerializedName("price_per_km")
    val pricePerKm: Double = 30.0,
    
    @SerializedName("max_distance")
    val maxDistance: Double = 50.0, // 0 = sin límite
    
    @SerializedName("blocked_zones")
    val blockedZones: List<ZonePreference> = emptyList(),
    
    @SerializedName("preferred_zones")
    val preferredZones: List<ZonePreference> = emptyList(),
    
    @SerializedName("auto_accept")
    val autoAccept: Boolean = false,
    
    @SerializedName("quiet_hours_enabled")
    val quietHoursEnabled: Boolean = false,
    
    @SerializedName("quiet_hours_start")
    val quietHoursStart: String = "22:00", // HH:mm
    
    @SerializedName("quiet_hours_end")
    val quietHoursEnd: String = "06:00",
    
    @SerializedName("updated_at")
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        /**
         * Retorna las preferencias por defecto
         */
        fun getDefaults(): UserPreferences {
            return UserPreferences(
                minPrice = 50.0,
                pricePerKm = 30.0,
                maxDistance = 50.0,
                autoAccept = true,
                quietHoursEnabled = false
            )
        }
    }
}

/**
 * Define una zona (bloqueada o preferida)
 */
data class ZonePreference(
    @SerializedName("name")
    val name: String,
    
    @SerializedName("latitude")
    val latitude: Double,
    
    @SerializedName("longitude")
    val longitude: Double,
    
    @SerializedName("radius_km")
    val radiusKm: Double,
    
    @SerializedName("zone_type")
    val zoneType: ZoneType = ZoneType.BLOCKED,
    
    @SerializedName("created_at")
    val createdAt: Long = System.currentTimeMillis()
) {
    enum class ZoneType {
        BLOCKED,   // Zona donde no trabajar
        PREFERRED  // Zona preferida para trabajar
    }
    
    companion object {
        /**
         * Crea una zona de ejemplo
         */
        fun createExample(
            name: String = "Zona Centro",
            latitude: Double = 18.4861,
            longitude: Double = -69.9312,
            radiusKm: Double = 2.0,
            type: ZoneType = ZoneType.PREFERRED
        ): ZonePreference {
            return ZonePreference(
                name = name,
                latitude = latitude,
                longitude = longitude,
                radiusKm = radiusKm,
                zoneType = type
            )
        }
    }
}
