package com.example.parisaracycle.data.repository

import com.example.parisaracycle.data.model.PitStop
import com.example.parisaracycle.data.model.PitStopType
import com.google.android.gms.maps.model.LatLng

class PitStopRepository {
    private val defaultCenter = LatLng(12.9141, 74.8560)

    fun getNearbyPitStops(center: LatLng?): List<PitStop> {
        val base = center ?: defaultCenter
        return listOf(
            PitStop(
                id = "repair-north",
                name = "Green Wheel Repair",
                latitude = base.latitude + 0.0060,
                longitude = base.longitude + 0.0045,
                type = PitStopType.Repair
            ),
            PitStop(
                id = "water-east",
                name = "Public Water Point",
                latitude = base.latitude + 0.0025,
                longitude = base.longitude + 0.0080,
                type = PitStopType.Water
            ),
            PitStop(
                id = "repair-south",
                name = "Cycle Care Stand",
                latitude = base.latitude - 0.0065,
                longitude = base.longitude - 0.0035,
                type = PitStopType.Repair
            ),
            PitStop(
                id = "water-west",
                name = "Park Water Station",
                latitude = base.latitude - 0.0020,
                longitude = base.longitude - 0.0080,
                type = PitStopType.Water
            )
        )
    }
}
