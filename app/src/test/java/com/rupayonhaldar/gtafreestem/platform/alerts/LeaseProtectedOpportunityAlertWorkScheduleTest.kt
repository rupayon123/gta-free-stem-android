package com.rupayonhaldar.gtafreestem.platform.alerts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LeaseProtectedOpportunityAlertWorkScheduleTest {
    @Test
    fun scheduleRotatesLeaseBeforeEnqueueingWork() {
        val events = mutableListOf<String>()
        val leases = RecordingLeaseStore(events)
        val schedule = LeaseProtectedOpportunityAlertWorkSchedule(
            leaseStore = leases,
            scheduleWork = { lease -> events += "schedule-work:${lease.token}"; true },
            cancelWork = { events += "cancel-work"; true },
        )

        assertTrue(schedule.schedule())
        assertEquals(listOf("replace-lease", "schedule-work:lease-1"), events)
        assertEquals(OpportunityAlertRunLease("lease-1"), leases.currentLease())
    }

    @Test
    fun failedEnqueueRevokesTheLeaseItJustCreated() {
        val events = mutableListOf<String>()
        val leases = RecordingLeaseStore(events)
        val schedule = LeaseProtectedOpportunityAlertWorkSchedule(
            leaseStore = leases,
            scheduleWork = { lease -> events += "schedule-work:${lease.token}"; false },
            cancelWork = { events += "cancel-work"; true },
        )

        assertFalse(schedule.schedule())
        assertEquals(
            listOf("replace-lease", "schedule-work:lease-1", "invalidate-lease"),
            events,
        )
        assertEquals(null, leases.currentLease())
    }

    @Test
    fun cancelRevokesBeforeRequestingAsynchronousWorkCancellation() {
        val events = mutableListOf<String>()
        val leases = RecordingLeaseStore(events).apply { requireNotNull(replaceActiveLease()) }
        events.clear()
        val schedule = LeaseProtectedOpportunityAlertWorkSchedule(
            leaseStore = leases,
            scheduleWork = { lease -> events += "schedule-work:${lease.token}"; true },
            cancelWork = { events += "cancel-work"; true },
        )

        assertTrue(schedule.cancel())
        assertEquals(listOf("invalidate-lease", "cancel-work"), events)
        assertEquals(null, leases.currentLease())
    }

    @Test
    fun preEnableRevocationDoesNotRaceAnUnneededAsyncCancellationWithScheduling() {
        val events = mutableListOf<String>()
        val leases = RecordingLeaseStore(events).apply { requireNotNull(replaceActiveLease()) }
        events.clear()
        val schedule = LeaseProtectedOpportunityAlertWorkSchedule(
            leaseStore = leases,
            scheduleWork = { lease -> events += "schedule-work:${lease.token}"; true },
            cancelWork = { events += "cancel-work"; true },
        )

        assertTrue(schedule.revokeRunningRefresh())

        assertEquals(listOf("invalidate-lease"), events)
        assertEquals(null, leases.currentLease())
    }

    @Test
    fun reEnableGetsANewTokenAndNeverRevalidatesAStaleWorker() {
        val leases = RecordingLeaseStore(mutableListOf())
        requireNotNull(leases.replaceActiveLease())
        val staleLease = requireNotNull(leases.currentLease())
        assertTrue(leases.invalidateLease())
        requireNotNull(leases.replaceActiveLease())

        assertNotEquals(staleLease, leases.currentLease())
        assertEquals(
            OpportunityAlertLeaseAccess.Stale,
            leases.withActiveLease(staleLease) { "must not run" },
        )
    }

    private class RecordingLeaseStore(
        private val events: MutableList<String>,
    ) : OpportunityAlertLeaseStore {
        private var sequence = 0
        private var lease: OpportunityAlertRunLease? = null

        override fun currentLease(): OpportunityAlertRunLease? = lease

        override fun replaceActiveLease(): OpportunityAlertRunLease {
            events += "replace-lease"
            lease = OpportunityAlertRunLease("lease-${++sequence}")
            return requireNotNull(lease)
        }

        override fun invalidateLease(): Boolean {
            events += "invalidate-lease"
            lease = null
            return true
        }

        override fun <T> withActiveLease(
            lease: OpportunityAlertRunLease,
            action: () -> T,
        ): OpportunityAlertLeaseAccess<T> = if (lease == this.lease) {
            OpportunityAlertLeaseAccess.Granted(action())
        } else {
            OpportunityAlertLeaseAccess.Stale
        }
    }
}
