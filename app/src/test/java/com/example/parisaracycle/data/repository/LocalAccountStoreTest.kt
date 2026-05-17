package com.example.parisaracycle.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalAccountStoreTest {
    @Test
    fun signInFailsBeforeRegistration() {
        val result = LocalAccountStore.signIn(
            accounts = emptyMap(),
            email = "rider@example.com",
            password = "cycle123"
        )

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Create an account first") == true)
    }

    @Test
    fun registerCreatesAccountAndSignInAcceptsRegisteredCredentials() {
        val registration = LocalAccountStore.register(
            accounts = emptyMap(),
            email = " Rider@Example.com ",
            password = "cycle123"
        ).getOrThrow()

        val signIn = LocalAccountStore.signIn(
            accounts = registration.accounts,
            email = "rider@example.com",
            password = "cycle123"
        ).getOrThrow()

        assertEquals("rider@example.com", registration.account.email)
        assertEquals(registration.account.uid, signIn.uid)
    }

    @Test
    fun duplicateRegistrationFailsForSameEmail() {
        val registration = LocalAccountStore.register(
            accounts = emptyMap(),
            email = "rider@example.com",
            password = "cycle123"
        ).getOrThrow()

        val duplicate = LocalAccountStore.register(
            accounts = registration.accounts,
            email = "RIDER@example.com",
            password = "cycle456"
        )

        assertTrue(duplicate.isFailure)
        assertTrue(duplicate.exceptionOrNull()?.message?.contains("already exists") == true)
    }

    @Test
    fun signInRejectsWrongPassword() {
        val registration = LocalAccountStore.register(
            accounts = emptyMap(),
            email = "rider@example.com",
            password = "cycle123"
        ).getOrThrow()

        val result = LocalAccountStore.signIn(
            accounts = registration.accounts,
            email = "rider@example.com",
            password = "wrong123"
        )

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Incorrect email or password") == true)
    }
}
