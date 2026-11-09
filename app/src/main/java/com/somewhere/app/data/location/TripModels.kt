package com.somewhere.app.data.location

import kotlinx.serialization.Serializable

@Serializable
enum class TravelMode(val label: String, val apiValue: String, val routesValue: String) {
    WALKING("Walk", "walking", "WALK"),
    CYCLING("Cycle", "bicycling", "BICYCLE"),
    DRIVING("Drive", "driving", "DRIVE")
}

@Serializable
enum class TripMedia { ANY, PHOTOS, AUDIO, TEXT }

@Serializable
data class TripOptions(
    val travelMode: TravelMode = TravelMode.WALKING,
    val radiusMeters: Double = 2000.0,
    val timeBudgetMinutes: Int? = null,
    val maxDetourMinutes: Int? = 10,
    val stopCount: Int = 3,
    val dwellMinutes: Int = 2,
    val returnToStart: Boolean = false,
    val unvisitedOnly: Boolean = false,
    val preferRecent: Boolean = false,
    val category: String? = null,
    val media: TripMedia = TripMedia.ANY,
    val cameraFree: Boolean = false,
    val haptics: Boolean = true,
    val stepFreeOnly: Boolean = false
)

@Serializable
data class TripPoint(val latitude: Double, val longitude: Double) {
    init {
        require(latitude.isFinite() && latitude in -90.0..90.0)
        require(longitude.isFinite() && longitude in -180.0..180.0)
    }
    val coordinate: String get() = "$latitude,$longitude"
    companion object {
        fun parse(value: String): TripPoint? {
            val parts = value.split(',')
            if (parts.size != 2) return null
            val lat = parts[0].trim().toDoubleOrNull() ?: return null
            val lng = parts[1].trim().toDoubleOrNull() ?: return null
            return runCatching { TripPoint(lat, lng) }.getOrNull()
        }
    }
}

/** Only user input and user-authored stop data are persisted, not map-provider responses. */
@Serializable
data class TripRequest(
    val origin: TripPoint,
    val destination: TripPoint? = null,
    val waypoints: List<TripPoint> = emptyList(),
    val query: String = "",
    val exclusions: List<String> = emptyList(),
    val options: TripOptions = TripOptions(),
    val action: TripAction = TripAction.RADIUS_SEARCH,
    val destinationLabel: String = "",
    val trailId: String? = null,
    val fixedStopIds: List<String> = emptyList(),
    val searchCenter: TripPoint = origin,
    val radiusIsConstraint: Boolean = false,
    val returnPoint: TripPoint? = null,
    val buildItinerary: Boolean = false,
    val useFixedStops: Boolean = false
)

@Serializable
data class SavedTrip(
    val id: String,
    val title: String,
    val request: TripRequest,
    val stopIds: List<String>,
    val createdAtMillis: Long,
    val completedAtMillis: Long? = null,
    val visitedStopIds: Set<String> = emptySet(),
    val note: String = "",
    val isBookmark: Boolean = true,
    val stopNotes: Map<String, String> = emptyMap(),
    val photoPaths: List<String> = emptyList()
)

@Serializable
data class CuratedTrail(
    val id: String,
    val title: String,
    val description: String = "",
    val user_id: String,
    val stop_ids: List<String>,
    val travel_mode: String = "walking",
    val return_to_start: Boolean = false,
    val start_lat: Double,
    val start_lng: Double,
    val created_at: String? = null
)

data class RankedTripDrop(
    val drop: com.somewhere.app.data.remote.NearbyDrop,
    val score: Double,
    val reasons: List<String>,
    val detourSeconds: Long? = null,
    val routeProgressMeters: Double? = null
)
