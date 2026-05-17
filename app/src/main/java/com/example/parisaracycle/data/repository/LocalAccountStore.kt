package com.example.parisaracycle.data.repository

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Locale

internal object LocalAccountStore {
    data class Account(
        val key: String,
        val uid: String,
        val email: String,
        val salt: String,
        val passwordHash: String
    )

    data class Registration(
        val account: Account,
        val accounts: Map<String, Account>
    )

    fun register(
        accounts: Map<String, Account>,
        email: String,
        password: String
    ): Result<Registration> = runCatching {
        val normalizedEmail = normalizeEmail(email)
        val key = accountKey(normalizedEmail)
        require(key !in accounts) { "An account already exists for this email." }

        val salt = randomHex(byteCount = 16)
        val account = Account(
            key = key,
            uid = "local-${randomHex(byteCount = 16)}",
            email = normalizedEmail,
            salt = salt,
            passwordHash = hashPassword(password, salt)
        )
        Registration(account = account, accounts = accounts + (key to account))
    }

    fun signIn(
        accounts: Map<String, Account>,
        email: String,
        password: String
    ): Result<Account> = runCatching {
        val normalizedEmail = normalizeEmail(email)
        val account = accounts[accountKey(normalizedEmail)]
            ?: throw IllegalArgumentException("No local account found for this email. Create an account first.")
        require(account.passwordHash == hashPassword(password, account.salt)) {
            "Incorrect email or password."
        }
        account
    }

    fun normalizeEmail(email: String): String =
        email.trim().lowercase(Locale.US)

    fun accountKey(email: String): String =
        sha256(normalizeEmail(email))

    private fun hashPassword(password: String, salt: String): String =
        sha256("$salt:$password")

    private fun randomHex(byteCount: Int): String {
        val bytes = ByteArray(byteCount)
        SecureRandom().nextBytes(bytes)
        return bytes.toHex()
    }

    private fun sha256(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
            .toHex()

    private fun ByteArray.toHex(): String =
        joinToString(separator = "") { byte -> "%02x".format(byte) }
}
