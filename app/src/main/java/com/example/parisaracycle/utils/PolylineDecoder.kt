package com.example.parisaracycle.utils

import com.google.android.gms.maps.model.LatLng

object PolylineDecoder {
    fun decode(encoded: String): List<LatLng> {
        val points = mutableListOf<LatLng>()
        var index = 0
        var latitude = 0
        var longitude = 0

        while (index < encoded.length) {
            var shift = 0
            var result = 0
            var byte: Int
            do {
                byte = encoded[index++].code - 63
                result = result or ((byte and 0x1f) shl shift)
                shift += 5
            } while (byte >= 0x20)
            latitude += if ((result and 1) != 0) (result shr 1).inv() else result shr 1

            shift = 0
            result = 0
            do {
                byte = encoded[index++].code - 63
                result = result or ((byte and 0x1f) shl shift)
                shift += 5
            } while (byte >= 0x20)
            longitude += if ((result and 1) != 0) (result shr 1).inv() else result shr 1

            points += LatLng(latitude / 1E5, longitude / 1E5)
        }

        return points
    }
}
