package com.example.parisaracycle.data.repository

import android.content.Context
import com.example.parisaracycle.data.model.AppUser
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID

class AuthRepository(
    private val auth: FirebaseAuth?,
    context: Context
) {
    private val preferences = context.getSharedPreferences("local_auth", Context.MODE_PRIVATE)
    private val localAuthState = MutableStateFlow(readLocalUser())

    val isConfigured: Boolean = true

    val authState: Flow<AppUser?> =
        if (auth == null) {
            localAuthState
        } else {
            callbackFlow {
                val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
                    trySend(firebaseAuth.currentUser?.toAppUser())
                }
                auth.addAuthStateListener(listener)
                trySend(auth.currentUser?.toAppUser())
                awaitClose { auth.removeAuthStateListener(listener) }
            }.distinctUntilChanged()
        }

    suspend fun signIn(email: String, password: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                if (auth == null) {
                    saveLocalUser(email.trim())
                } else {
                    auth.signInWithEmailAndPassword(email.trim(), password).await()
                }
                Unit
            }
        }

    suspend fun register(email: String, password: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                if (auth == null) {
                    saveLocalUser(email.trim())
                } else {
                    auth.createUserWithEmailAndPassword(email.trim(), password).await()
                }
                Unit
            }
        }

    fun signOut() {
        if (auth == null) {
            preferences.edit().clear().apply()
            localAuthState.value = null
        } else {
            auth.signOut()
        }
    }

    private fun FirebaseUser.toAppUser(): AppUser =
        AppUser(uid = uid, email = email.orEmpty())

    private fun readLocalUser(): AppUser? {
        val uid = preferences.getString("uid", null) ?: return null
        val email = preferences.getString("email", null).orEmpty()
        return AppUser(uid = uid, email = email)
    }

    private fun saveLocalUser(email: String) {
        val uid = preferences.getString("uid", null) ?: "local-${UUID.randomUUID()}"
        preferences.edit()
            .putString("uid", uid)
            .putString("email", email)
            .apply()
        localAuthState.value = AppUser(uid = uid, email = email)
    }
}
