package com.rupayonhaldar.gtafreestem.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalAccountDataDeletionCoordinatorTest {
    @Test
    fun `delete all local account data clears profile history and saves`() {
        val calls = mutableListOf<String>()
        val coordinator = LocalAccountDataDeletionCoordinator(
            deleteProfile = {
                calls += "profile"
                true
            },
            deleteSearchHistory = {
                calls += "history"
                true
            },
            deleteSavedOpportunities = {
                calls += "saves"
                true
            },
        )

        val result = coordinator.deleteAllLocalAccountData()

        assertTrue(result.allLocalAccountDataDeleted)
        assertEquals(
            LocalAccountDataDeletionResult(true, true, true),
            result,
        )
        assertEquals(listOf("profile", "history", "saves"), calls)
    }

    @Test
    fun `every deletion is attempted and failures are reported without throwing`() {
        val calls = mutableListOf<String>()
        val coordinator = LocalAccountDataDeletionCoordinator(
            deleteProfile = {
                calls += "profile"
                error("profile deletion failed")
            },
            deleteSearchHistory = {
                calls += "history"
                false
            },
            deleteSavedOpportunities = {
                calls += "saves"
                error("save deletion failed")
            },
        )

        val result = coordinator.deleteAllLocalAccountData()

        assertFalse(result.allLocalAccountDataDeleted)
        assertEquals(
            LocalAccountDataDeletionResult(false, false, false),
            result,
        )
        assertEquals(listOf("profile", "history", "saves"), calls)
    }
}
