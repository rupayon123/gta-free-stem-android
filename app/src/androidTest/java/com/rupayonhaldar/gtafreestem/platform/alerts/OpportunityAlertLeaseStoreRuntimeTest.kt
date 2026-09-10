package com.rupayonhaldar.gtafreestem.platform.alerts

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OpportunityAlertLeaseStoreRuntimeTest {
    @Test
    fun clearAtomicallyRevokesTheLeaseAndHistoryAndReEnableUsesANewToken() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = SharedPreferencesOpportunityAlertHistoryStore(context)
        assertTrue(OpportunityAlertPlatform.clearLocalHistory(context))
        requireNotNull(store.replaceActiveLease())
        val staleLease = requireNotNull(store.currentLease())
        assertEquals(
            OpportunityAlertLeaseAccess.Granted(Unit),
            store.saveKnownIds(staleLease, listOf("known"), hasBaseline = true),
        )

        assertTrue(OpportunityAlertPlatform.clearLocalHistory(context))

        assertNull(store.currentLease())
        assertEquals(OpportunityAlertHistoryState(), store.read())
        assertEquals(
            OpportunityAlertLeaseAccess.Stale,
            store.withActiveLease(staleLease) { "must not run" },
        )

        requireNotNull(store.replaceActiveLease())
        assertNotEquals(staleLease, store.currentLease())
        assertTrue(OpportunityAlertPlatform.clearLocalHistory(context))
    }
}
