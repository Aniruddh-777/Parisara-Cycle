package com.example.parisaracycle.data.firebase

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore

data class FirebaseClients(
    val auth: FirebaseAuth,
    val firestore: FirebaseFirestore,
    val realtimeDatabase: FirebaseDatabase
) {
    companion object {
        fun from(context: Context): FirebaseClients? {
            if (FirebaseApp.getApps(context).isEmpty()) return null

            return runCatching {
                FirebaseClients(
                    auth = FirebaseAuth.getInstance(),
                    firestore = FirebaseFirestore.getInstance(),
                    realtimeDatabase = FirebaseDatabase.getInstance()
                )
            }.getOrNull()
        }
    }
}
