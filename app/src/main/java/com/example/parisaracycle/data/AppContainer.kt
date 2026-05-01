package com.example.parisaracycle.data

import android.content.Context
import com.example.parisaracycle.data.firebase.FirebaseClients
import com.example.parisaracycle.data.repository.AuthRepository
import com.example.parisaracycle.data.repository.BuddyLocationRepository
import com.example.parisaracycle.data.repository.DangerZoneRepository
import com.example.parisaracycle.data.repository.DirectionsRepository
import com.example.parisaracycle.data.repository.EcoStatsRepository
import com.example.parisaracycle.data.repository.LocationRepository
import com.example.parisaracycle.data.repository.PitStopRepository

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val firebaseClients = FirebaseClients.from(appContext)

    val authRepository = AuthRepository(firebaseClients?.auth, appContext)
    val dangerZoneRepository = DangerZoneRepository(firebaseClients?.firestore, appContext)
    val buddyLocationRepository = BuddyLocationRepository(firebaseClients?.realtimeDatabase)
    val directionsRepository = DirectionsRepository()
    val ecoStatsRepository = EcoStatsRepository(appContext)
    val locationRepository = LocationRepository(appContext)
    val pitStopRepository = PitStopRepository()
}
