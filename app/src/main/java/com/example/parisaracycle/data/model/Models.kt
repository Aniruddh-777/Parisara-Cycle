package com.example.parisaracycle.data.model

import com.google.android.gms.maps.model.LatLng

data class AppUser(
    val uid: String,
    val email: String
)

enum class DangerZoneType(val label: String) {
    Pothole("Pothole"),
    DangerousIntersection("Dangerous Intersection"),
    BlockedPath("Blocked Path");

    companion object {
        fun fromStorage(value: String?): DangerZoneType =
            entries.firstOrNull { it.name == value || it.label == value } ?: Pothole
    }
}

data class DangerZone(
    val id: String,
    val latitude: Double,
    val longitude: Double,
    val type: DangerZoneType,
    val timestamp: Long,
    val userId: String
) {
    val position: LatLng
        get() = LatLng(latitude, longitude)
}

enum class PitStopType(val label: String) {
    Repair("Cycle repair"),
    Water("Water point")
}

data class PitStop(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val type: PitStopType
) {
    val position: LatLng
        get() = LatLng(latitude, longitude)
}

data class BuddyLocation(
    val userId: String,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long
) {
    val position: LatLng
        get() = LatLng(latitude, longitude)
}

data class RouteResult(
    val points: List<LatLng>,
    val distanceMeters: Int
) {
    val distanceKm: Double
        get() = distanceMeters / 1000.0
}

data class EcoStats(
    val todayDistanceKm: Double = 0.0,
    val todayCo2Grams: Double = 0.0,
    val monthDistanceKm: Double = 0.0,
    val monthCo2Grams: Double = 0.0
)

enum class MapLayer(val label: String) {
    Danger("Danger"),
    PitStops("Pit-stops"),
    Buddies("Buddies"),
    Route("Route")
}
