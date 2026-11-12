package com.myiptv.clean.data.repository

import org.junit.Assert.assertEquals
import org.junit.Test

class GuestProfileErrorMessageTest {
    @Test fun usernameConflictBecomesShortFriendlySpanishMessage() {
        val backendError = """{"code":"23505","details":"Key (lower(username))=(makao) already exists","message":"duplicate key violates unique constraint guest_profiles_username_ci_idx"}"""
        assertEquals(
            "Ese nombre de usuario ya está en uso. Prueba con otro.",
            guestProfileHttpErrorMessage(500, "{\"error\":\"Database request failed (409): $backendError\"}", savingProfile = true)
        )
    }

    @Test fun databaseInternalsAreNeverShownForUnexpectedServerErrors() {
        assertEquals(
            "El servicio de perfiles no está disponible. Inténtalo de nuevo más tarde.",
            guestProfileHttpErrorMessage(500, "Database request failed (500): SQLSTATE 42P01", savingProfile = false)
        )
    }

}
