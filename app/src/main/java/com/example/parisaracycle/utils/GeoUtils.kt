package com.example.parisaracycle.utils

import android.location.Location
import com.google.android.gms.maps.model.LatLng
import kotlin.math.roundToInt

fun LatLng.distanceToKm(other: LatLng): Double {
    val result = FloatArray(1)
    Location.distanceBetween(latitude, longitude, other.latitude, other.longitude, result)
    return result[0] / 1000.0
}

fun formatDistance(km: Double): String =
    if (km < 1.0) "${(km * 1000).roundToInt()} m" else "%.1f km".format(km)

fun formatCo2(grams: Double): String =
    if (grams < 1000.0) "${grams.roundToInt()} g" else "%.2f kg".format(grams / 1000.0)
