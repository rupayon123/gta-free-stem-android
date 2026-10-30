package com.rupayonhaldar.gtafreestem.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.coroutines.test.runTest

class LocalAccountDataDeletionCoordinatorTest {
    @Test
    fun `delete all local account data clears profile history and saves`() = runTest {
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
            deleteLocalAlerts = {
                calls += "alerts"
                true
            },
            deleteNamedSearches = {
                calls += "named searches"
                true
            },
        )

        val result = coordinator.deleteAllLocalAccountData()

        assertTrue(result.allLocalAccountDataDeleted)
        assertEquals(
            LocalAccountDataDeletionResult(true, true, true, true),
            result,
        )
        assertEquals(listOf("alerts", "profile", "history", "saves", "named searches"), calls)
    }

    @Test
    fun `every deletion is attempted and failures are reported without throwing`() = runTest {
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
            deleteLocalAlerts = {
                calls += "alerts"
                false
            },
            deleteNamedSearches = {
                calls += "named searches"
                false
            },
        )

        val result = coordinator.deleteAllLocalAccountData()

        assertFalse(result.allLocalAccountDataDeleted)
        assertEquals(
            LocalAccountDataDeletionResult(false, false, false, false, false),
            result,
        )
        assertEquals(listOf("alerts", "profile", "history", "saves", "named searches"), calls)
    }
}
