package com.rupayonhaldar.gtafreestem.platform.alerts

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OpportunityAlertRefreshRunnerTest {
    @Test
    fun firstRunBaselinesWithoutAlertingForExistingListings() = runTest {
        val fixture = Fixture(
            initialHistory = OpportunityAlertHistoryState(hasBaseline = false),
            matchingIds = listOf("a", "b"),
        )

        assertEquals(OpportunityAlertRefreshOutcome.BASELINED, fixture.runner.run())
        assertTrue(fixture.history.state.hasBaseline)
        assertEquals(setOf("a", "b"), fixture.history.state.knownIds.toSet())
        assertFalse(fixture.posted)
    }

    @Test
    fun newMatchingIdPostsLocalizedCountAndRecordsThrottle() = runTest {
        val fixture = Fixture(
            initialHistory = OpportunityAlertHistoryState(
                knownIds = listOf("a"),
                hasBaseline = true,
            ),
            matchingIds = listOf("a", "b", "b"),
        )

        assertEquals(OpportunityAlertRefreshOutcome.NOTIFIED, fixture.runner.run())
        assertTrue(fixture.posted)
        assertEquals(1, fixture.postedCount)
        assertEquals("1 localized matches", fixture.postedCopy?.body)
        assertEquals(NOW, fixture.history.state.lastNotificationEpochMillis)
    }

    @Test
    fun hourlyThrottleStillRetainsNewIdsToPreventLaterDuplicates() = runTest {
        val fixture = Fixture(
            initialHistory = OpportunityAlertHistoryState(
                knownIds = listOf("a"),
                hasBaseline = true,
                lastNotificationEpochMillis = NOW - 30_000L,
            ),
            matchingIds = listOf("a", "b"),
        )

        assertEquals(OpportunityAlertRefreshOutcome.THROTTLED, fixture.runner.run())
        assertEquals(setOf("a", "b"), fixture.history.state.knownIds.toSet())
        assertFalse(fixture.posted)
    }

    @Test
    fun disabledAndUnavailableStatesDoNoNetworkWork() = runTest {
        val disabled = Fixture(preferred = false)
        assertEquals(OpportunityAlertRefreshOutcome.DISABLED, disabled.runner.run())
        assertEquals(0, disabled.loadCount)

        val revoked = Fixture()
        revoked.history.invalidateLease()
        assertEquals(OpportunityAlertRefreshOutcome.DISABLED, revoked.runner.run())
        assertEquals(0, revoked.loadCount)

        val unavailable = Fixture(canNotify = false)
        assertEquals(
            OpportunityAlertRefreshOutcome.NOTIFICATION_UNAVAILABLE,
            unavailable.runner.run(),
        )
        assertEquals(0, unavailable.loadCount)
    }

    @Test
    fun optOutDuringRefreshPreventsHistoryWritesAndNotification() = runTest {
        var preferred = true
        var posted = false
        val history = RecordingHistory(
            OpportunityAlertHistoryState(
                knownIds = listOf("known"),
                hasBaseline = true,
            ),
        )
        val runner = OpportunityAlertRefreshRunner(
            isPreferred = { preferred },
            canNotify = { true },
            loadMatchingIds = {
                preferred = false
                listOf("known", "new")
            },
            history = history,
            lease = requireNotNull(history.currentLease()),
            notificationCopy = { OpportunityNotificationCopy("title", "body") },
            postNotification = { _, _, _ -> posted = true; true },
            nowEpochMillis = { NOW },
        )

        assertEquals(OpportunityAlertRefreshOutcome.DISABLED, runner.run())
        assertEquals(listOf("known"), history.state.knownIds)
        assertEquals(null, history.state.lastNotificationEpochMillis)
        assertFalse(posted)
    }

    @Test
    fun deletionImmediatelyBeforeKnownIdsWriteCannotRecreateClearedHistory() = runTest {
        var preferred = true
        var posted = false
        val history = RecordingHistory(
            OpportunityAlertHistoryState(
                knownIds = listOf("known"),
                hasBaseline = true,
            ),
        ).apply {
            beforeSaveKnownIds = {
                preferred = false
                invalidateLeaseAndClear()
            }
        }
        val runner = runner(
            isPreferred = { preferred },
            history = history,
            postNotification = { _, _, _ -> posted = true; true },
        )

        assertEquals(OpportunityAlertRefreshOutcome.DISABLED, runner.run())
        assertEquals(OpportunityAlertHistoryState(), history.state)
        assertFalse(posted)
    }

    @Test
    fun deletionImmediatelyBeforeThrottleWriteCannotRecreateClearedHistory() = runTest {
        var preferred = true
        var posted = false
        val history = RecordingHistory(
            OpportunityAlertHistoryState(
                knownIds = listOf("known"),
                hasBaseline = true,
            ),
        ).apply {
            beforeRecordNotification = {
                preferred = false
                invalidateLeaseAndClear()
            }
        }
        val runner = runner(
            isPreferred = { preferred },
            history = history,
            postNotification = { _, _, _ -> posted = true; true },
        )

        assertEquals(OpportunityAlertRefreshOutcome.DISABLED, runner.run())
        assertEquals(OpportunityAlertHistoryState(), history.state)
        assertFalse(posted)
    }

    @Test
    fun deletionAtFinalNotificationBoundaryCannotPostFromStaleRefresh() = runTest {
        var preferred = true
        var posted = false
        val history = RecordingHistory(
            OpportunityAlertHistoryState(
                knownIds = listOf("known"),
                hasBaseline = true,
            ),
        ).apply {
            beforeGuardedAction = {
                preferred = false
                invalidateLeaseAndClear()
            }
        }
        val runner = runner(
            isPreferred = { preferred },
            history = history,
            postNotification = { _, _, _ -> posted = true; true },
        )

        assertEquals(OpportunityAlertRefreshOutcome.DISABLED, runner.run())
        assertEquals(OpportunityAlertHistoryState(), history.state)
        assertFalse(posted)
    }

    @Test
    fun optOutAtFinalNotificationBoundaryCannotPostFromStaleRefresh() = runTest {
        var preferred = true
        var posted = false
        val history = RecordingHistory(
            OpportunityAlertHistoryState(
                knownIds = listOf("known"),
                hasBaseline = true,
            ),
        ).apply {
            beforeGuardedAction = {
                preferred = false
                invalidateLease()
            }
        }
        val runner = runner(
            isPreferred = { preferred },
            history = history,
            postNotification = { _, _, _ -> posted = true; true },
        )

        assertEquals(OpportunityAlertRefreshOutcome.DISABLED, runner.run())
        assertEquals(setOf("known", "new"), history.state.knownIds.toSet())
        assertEquals(NOW, history.state.lastNotificationEpochMillis)
        assertFalse(posted)
    }

    private fun runner(
        isPreferred: () -> Boolean,
        history: RecordingHistory,
        postNotification: (OpportunityNotificationCopy, Int, Long) -> Boolean,
    ) = OpportunityAlertRefreshRunner(
        isPreferred = isPreferred,
        canNotify = { true },
        loadMatchingIds = { listOf("known", "new") },
        history = history,
        lease = requireNotNull(history.currentLease()),
        notificationCopy = { OpportunityNotificationCopy("title", "body") },
        postNotification = postNotification,
        nowEpochMillis = { NOW },
    )

    private class Fixture(
        preferred: Boolean = true,
        canNotify: Boolean = true,
        initialHistory: OpportunityAlertHistoryState = OpportunityAlertHistoryState(),
        private val matchingIds: List<String> = emptyList(),
    ) {
        val history = RecordingHistory(initialHistory)
        var loadCount = 0
        var posted = false
        var postedCount = 0
        var postedCopy: OpportunityNotificationCopy? = null
        val runner = OpportunityAlertRefreshRunner(
            isPreferred = { preferred },
            canNotify = { canNotify },
            loadMatchingIds = { loadCount += 1; matchingIds },
            history = history,
            lease = requireNotNull(history.currentLease()),
            notificationCopy = { count ->
                OpportunityNotificationCopy("localized title", "$count localized matches")
            },
            postNotification = { copy, count, _ ->
                posted = true
                postedCopy = copy
                postedCount = count
                true
            },
            nowEpochMillis = { NOW },
        )
    }

    private class RecordingHistory(
        var state: OpportunityAlertHistoryState,
    ) : OpportunityAlertHistoryStore {
        private var nextLeaseNumber = 1
        private var activeLease: OpportunityAlertRunLease? = OpportunityAlertRunLease("lease-0")
        var beforeSaveKnownIds: () -> Unit = {}
        var beforeRecordNotification: () -> Unit = {}
        var beforeGuardedAction: () -> Unit = {}

        override fun currentLease(): OpportunityAlertRunLease? = activeLease

        override fun replaceActiveLease(): OpportunityAlertRunLease {
            activeLease = OpportunityAlertRunLease("lease-${nextLeaseNumber++}")
            return requireNotNull(activeLease)
        }

        override fun invalidateLease(): Boolean {
            activeLease = null
            return true
        }

        override fun <T> withActiveLease(
            lease: OpportunityAlertRunLease,
            action: () -> T,
        ): OpportunityAlertLeaseAccess<T> {
            beforeGuardedAction()
            if (lease != activeLease) return OpportunityAlertLeaseAccess.Stale
            return runCatching(action).fold(
                onSuccess = { OpportunityAlertLeaseAccess.Granted(it) },
                onFailure = { OpportunityAlertLeaseAccess.Failed },
            )
        }

        override fun read(): OpportunityAlertHistoryState = state

        override fun saveKnownIds(
            lease: OpportunityAlertRunLease,
            ids: List<String>,
            hasBaseline: Boolean,
        ): OpportunityAlertLeaseAccess<Unit> {
            beforeSaveKnownIds()
            if (lease != activeLease) return OpportunityAlertLeaseAccess.Stale
            state = state.copy(knownIds = ids, hasBaseline = hasBaseline)
            return OpportunityAlertLeaseAccess.Granted(Unit)
        }

        override fun recordNotificationAt(
            lease: OpportunityAlertRunLease,
            epochMillis: Long,
        ): OpportunityAlertLeaseAccess<Unit> {
            beforeRecordNotification()
            if (lease != activeLease) return OpportunityAlertLeaseAccess.Stale
            state = state.copy(lastNotificationEpochMillis = epochMillis)
            return OpportunityAlertLeaseAccess.Granted(Unit)
        }

        override fun clear(): Boolean {
            invalidateLeaseAndClear()
            return true
        }

        fun invalidateLeaseAndClear() {
            activeLease = null
            state = OpportunityAlertHistoryState()
        }
    }

    private companion object {
        const val NOW = 2_000_000L
    }
}
