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
                    val account = LocalAccountStore.signIn(
                        accounts = readLocalAccounts(),
                        email = email,
                        password = password
                    ).getOrThrow()
                    saveCurrentLocalUser(account)
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
                    val registration = LocalAccountStore.register(
                        accounts = readLocalAccounts(),
                        email = email,
                        password = password
                    ).getOrThrow()
                    saveLocalAccount(registration.account)
                    saveCurrentLocalUser(registration.account)
                } else {
                    auth.createUserWithEmailAndPassword(email.trim(), password).await()
                }
                Unit
            }
        }

    fun signOut() {
        if (auth == null) {
            preferences.edit()
                .remove(KEY_CURRENT_UID)
                .remove(KEY_CURRENT_EMAIL)
                .apply()
            localAuthState.value = null
        } else {
            auth.signOut()
        }
    }

    private fun FirebaseUser.toAppUser(): AppUser =
        AppUser(uid = uid, email = email.orEmpty())

    private fun readLocalUser(): AppUser? {
        val uid = preferences.getString(KEY_CURRENT_UID, null) ?: return null
        val email = preferences.getString(KEY_CURRENT_EMAIL, null).orEmpty()
        val account = readLocalAccounts()[LocalAccountStore.accountKey(email)] ?: return null
        if (account.uid != uid) return null
        return AppUser(uid = uid, email = email)
    }

    private fun readLocalAccounts(): Map<String, LocalAccountStore.Account> =
        preferences.all.keys
            .filter { key -> key.startsWith(ACCOUNT_PREFIX) && key.endsWith(".email") }
            .mapNotNull { emailKey ->
                val accountKey = emailKey
                    .removePrefix(ACCOUNT_PREFIX)
                    .removeSuffix(".email")
                val email = preferences.getString(accountField(accountKey, "email"), null)
                    ?: return@mapNotNull null
                val uid = preferences.getString(accountField(accountKey, "uid"), null)
                    ?: return@mapNotNull null
                val salt = preferences.getString(accountField(accountKey, "salt"), null)
                    ?: return@mapNotNull null
                val passwordHash = preferences.getString(accountField(accountKey, "passwordHash"), null)
                    ?: return@mapNotNull null

                accountKey to LocalAccountStore.Account(
                    key = accountKey,
                    uid = uid,
                    email = email,
                    salt = salt,
                    passwordHash = passwordHash
                )
            }
            .toMap()

    private fun saveLocalAccount(account: LocalAccountStore.Account) {
        preferences.edit()
            .putString(accountField(account.key, "email"), account.email)
            .putString(accountField(account.key, "uid"), account.uid)
            .putString(accountField(account.key, "salt"), account.salt)
            .putString(accountField(account.key, "passwordHash"), account.passwordHash)
            .apply()
    }

    private fun saveCurrentLocalUser(account: LocalAccountStore.Account) {
        preferences.edit()
            .putString(KEY_CURRENT_UID, account.uid)
            .putString(KEY_CURRENT_EMAIL, account.email)
            .apply()
        localAuthState.value = AppUser(uid = account.uid, email = account.email)
    }

    private fun accountField(accountKey: String, field: String): String =
        "$ACCOUNT_PREFIX$accountKey.$field"

    companion object {
        private const val KEY_CURRENT_UID = "uid"
        private const val KEY_CURRENT_EMAIL = "email"
        private const val ACCOUNT_PREFIX = "account."
    }
}
