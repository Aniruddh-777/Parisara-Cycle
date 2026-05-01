package com.example.parisaracycle.data.repository

import com.example.parisaracycle.data.model.BuddyLocation
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class BuddyLocationRepository(
    private val database: FirebaseDatabase?
) {
    private val path = "live_locations"
    private val localBuddyLocations = MutableStateFlow<List<BuddyLocation>>(emptyList())

    fun observeBuddyLocations(): Flow<List<BuddyLocation>> =
        if (database == null) {
            localBuddyLocations
        } else {
            callbackFlow {
                val reference = database.reference.child(path)

                val listener = object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val locations = snapshot.children.mapNotNull { child ->
                            val userId = child.key ?: return@mapNotNull null
                            val latitude = child.child("latitude").getValue(Double::class.java) ?: return@mapNotNull null
                            val longitude = child.child("longitude").getValue(Double::class.java) ?: return@mapNotNull null
                            val timestamp = child.child("timestamp").getValue(Long::class.java) ?: return@mapNotNull null
                            BuddyLocation(userId, latitude, longitude, timestamp)
                        }
                        trySend(locations)
                    }

                    override fun onCancelled(error: DatabaseError) {
                        trySend(emptyList())
                    }
                }

                reference.addValueEventListener(listener)
                awaitClose { reference.removeEventListener(listener) }
            }
        }

    suspend fun publishLocation(userId: String, position: LatLng): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                if (database == null) {
                    localBuddyLocations.value = buildLocalBuddies(position)
                } else {
                    val payload = mapOf(
                        "latitude" to position.latitude,
                        "longitude" to position.longitude,
                        "timestamp" to System.currentTimeMillis()
                    )
                    database.reference.child(path).child(userId).setValue(payload).await()
                }
                Unit
            }
        }

    suspend fun removeLocation(userId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                if (database == null) {
                    localBuddyLocations.value = emptyList()
                } else {
                    database.reference.child(path).child(userId).removeValue().await()
                }
                Unit
            }
        }

    private fun buildLocalBuddies(position: LatLng): List<BuddyLocation> {
        val now = System.currentTimeMillis()
        return listOf(
            BuddyLocation("demo-rider-1", position.latitude + 0.006, position.longitude + 0.004, now),
            BuddyLocation("demo-rider-2", position.latitude - 0.005, position.longitude + 0.006, now),
            BuddyLocation("demo-rider-3", position.latitude + 0.003, position.longitude - 0.007, now)
        )
    }
}
