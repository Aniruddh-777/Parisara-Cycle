package com.example.parisaracycle.data.repository

import android.net.Uri
import com.example.parisaracycle.BuildConfig
import com.example.parisaracycle.data.model.RouteResult
import com.example.parisaracycle.utils.distanceToKm
import com.example.parisaracycle.utils.PolylineDecoder
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class DirectionsRepository {
    suspend fun getBicycleRoute(origin: LatLng, destination: LatLng): Result<RouteResult> =
        withContext(Dispatchers.IO) {
            runCatching {
                val apiKey = configuredApiKey()
                if (apiKey == null) return@runCatching buildLocalRoute(origin, destination)

                val uri = Uri.Builder()
                    .scheme("https")
                    .authority("maps.googleapis.com")
                    .appendPath("maps")
                    .appendPath("api")
                    .appendPath("directions")
                    .appendPath("json")
                    .appendQueryParameter("origin", "${origin.latitude},${origin.longitude}")
                    .appendQueryParameter("destination", "${destination.latitude},${destination.longitude}")
                    .appendQueryParameter("mode", "bicycling")
                    .appendQueryParameter("avoid", "highways")
                    .appendQueryParameter("key", apiKey)
                    .build()

                val response = executeGet(uri.toString())
                val json = JSONObject(response)
                val status = json.optString("status")
                require(status == "OK") {
                    json.optString("error_message").ifBlank { "Directions API returned $status." }
                }

                val routes = json.getJSONArray("routes")
                require(routes.length() > 0) { "No bicycle route found." }

                val route = routes.getJSONObject(0)
                val encodedPolyline = route.getJSONObject("overview_polyline").getString("points")
                val points = PolylineDecoder.decode(encodedPolyline)
                val legs = route.getJSONArray("legs")
                var distanceMeters = 0
                for (index in 0 until legs.length()) {
                    distanceMeters += legs.getJSONObject(index)
                        .getJSONObject("distance")
                        .getInt("value")
                }

                RouteResult(points = points, distanceMeters = distanceMeters)
            }
        }

    private fun buildLocalRoute(origin: LatLng, destination: LatLng): RouteResult {
        val midpointA = LatLng(
            origin.latitude + (destination.latitude - origin.latitude) * 0.35,
            origin.longitude
        )
        val midpointB = LatLng(
            origin.latitude + (destination.latitude - origin.latitude) * 0.65,
            destination.longitude
        )
        val points = listOf(origin, midpointA, midpointB, destination)
        val distanceKm = points.zipWithNext().sumOf { (from, to) -> from.distanceToKm(to) }
        return RouteResult(
            points = points,
            distanceMeters = (distanceKm * 1000).toInt()
        )
    }

    private fun configuredApiKey(): String? {
        val directionsKey = BuildConfig.DIRECTIONS_API_KEY.takeIf { it.isUsableApiKey() }
        val mapsKey = BuildConfig.MAPS_API_KEY.takeIf { it.isUsableApiKey() }
        return directionsKey ?: mapsKey
    }

    private fun String.isUsableApiKey(): Boolean =
        isNotBlank() && this != "DEFAULT_API_KEY"

    private fun executeGet(url: String): String {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 15_000
            requestMethod = "GET"
        }

        return try {
            val stream = if (connection.responseCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }
            val body = stream.bufferedReader().use { it.readText() }
            require(connection.responseCode in 200..299) {
                "Directions request failed with HTTP ${connection.responseCode}."
            }
            body
        } finally {
            connection.disconnect()
        }
    }
}
