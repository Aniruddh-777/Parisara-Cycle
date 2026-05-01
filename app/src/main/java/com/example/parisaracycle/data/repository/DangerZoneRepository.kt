package com.example.parisaracycle.data.repository

import android.content.Context
import com.example.parisaracycle.data.model.DangerZone
import com.example.parisaracycle.data.model.DangerZoneType
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class DangerZoneRepository(
    private val firestore: FirebaseFirestore?,
    context: Context
) {
    private val collectionName = "danger_zones"
    private val preferences = context.getSharedPreferences("local_danger_zones", Context.MODE_PRIVATE)
    private val localDangerZones = MutableStateFlow(readLocalDangerZones())

    fun observeDangerZones(): Flow<List<DangerZone>> =
        if (firestore == null) {
            localDangerZones
        } else {
            callbackFlow {
                val registration = firestore.collection(collectionName)
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(300)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null || snapshot == null) {
                            trySend(emptyList())
                            return@addSnapshotListener
                        }

                        val zones = snapshot.documents.mapNotNull { document ->
                            val latitude = document.getDouble("latitude") ?: return@mapNotNull null
                            val longitude = document.getDouble("longitude") ?: return@mapNotNull null
                            DangerZone(
                                id = document.id,
                                latitude = latitude,
                                longitude = longitude,
                                type = DangerZoneType.fromStorage(document.getString("type")),
                                timestamp = document.getLong("timestamp") ?: 0L,
                                userId = document.getString("userId").orEmpty()
                            )
                        }
                        trySend(zones)
                    }

                awaitClose { registration.remove() }
            }
        }

    suspend fun reportDangerZone(
        position: LatLng,
        type: DangerZoneType,
        userId: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            if (firestore == null) {
                val updated = listOf(
                    DangerZone(
                        id = UUID.randomUUID().toString(),
                        latitude = position.latitude,
                        longitude = position.longitude,
                        type = type,
                        timestamp = System.currentTimeMillis(),
                        userId = userId
                    )
                ) + localDangerZones.value
                saveLocalDangerZones(updated)
            } else {
                val data = mapOf(
                    "latitude" to position.latitude,
                    "longitude" to position.longitude,
                    "type" to type.name,
                    "timestamp" to System.currentTimeMillis(),
                    "userId" to userId
                )
                firestore.collection(collectionName).add(data).await()
            }
            Unit
        }
    }

    private fun readLocalDangerZones(): List<DangerZone> {
        val rawJson = preferences.getString("zones", null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(rawJson)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    add(
                        DangerZone(
                            id = item.optString("id"),
                            latitude = item.optDouble("latitude"),
                            longitude = item.optDouble("longitude"),
                            type = DangerZoneType.fromStorage(item.optString("type")),
                            timestamp = item.optLong("timestamp"),
                            userId = item.optString("userId")
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun saveLocalDangerZones(zones: List<DangerZone>) {
        val array = JSONArray()
        zones.take(300).forEach { zone ->
            array.put(
                JSONObject()
                    .put("id", zone.id)
                    .put("latitude", zone.latitude)
                    .put("longitude", zone.longitude)
                    .put("type", zone.type.name)
                    .put("timestamp", zone.timestamp)
                    .put("userId", zone.userId)
            )
        }
        preferences.edit().putString("zones", array.toString()).apply()
        localDangerZones.value = zones.take(300)
    }
}
