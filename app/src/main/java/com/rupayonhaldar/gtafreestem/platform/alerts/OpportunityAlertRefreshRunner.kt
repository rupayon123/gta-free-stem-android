package com.rupayonhaldar.gtafreestem.platform.alerts

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

data class OpportunityNotificationCopy(
    val title: String,
    val body: String,
)

enum class OpportunityAlertRefreshOutcome {
    DISABLED,
    NOTIFICATION_UNAVAILABLE,
    BASELINED,
    NO_NEW_MATCHES,
    THROTTLED,
    NOTIFIED,
    RETRY,
}

/** Pure orchestration behind the Worker so history, throttling, and denial paths are testable. */
internal class OpportunityAlertRefreshRunner(
    private val isPreferred: () -> Boolean,
    private val canNotify: () -> Boolean,
    private val loadMatchingIds: suspend () -> List<String>,
    private val history: OpportunityAlertHistoryStore,
    private val lease: OpportunityAlertRunLease,
    private val notificationCopy: (Int) -> OpportunityNotificationCopy,
    private val postNotification: (OpportunityNotificationCopy, Int, Long) -> Boolean,
    private val nowEpochMillis: () -> Long,
    private val isWorkerStopped: () -> Boolean = { false },
) {
    suspend fun run(): OpportunityAlertRefreshOutcome {
        if (!canContinue()) {
            return OpportunityAlertRefreshOutcome.DISABLED
        }
        if (runCatching(history::currentLease).getOrNull() != lease) {
            return OpportunityAlertRefreshOutcome.DISABLED
        }
        if (!runCatching(canNotify).getOrDefault(false)) {
            return OpportunityAlertRefreshOutcome.NOTIFICATION_UNAVAILABLE
        }

        val currentIds = try {
            loadMatchingIds()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Throwable) {
            return OpportunityAlertRefreshOutcome.RETRY
        }
        // Cancellation and preference writes can race a network refresh. Re-read both before
        // touching history so opt-out/deletion cannot be undone by an already-running worker.
        if (!canContinue()) return OpportunityAlertRefreshOutcome.DISABLED
        val previous = runCatching(history::read).getOrElse {
            return OpportunityAlertRefreshOutcome.RETRY
        }
        val merged = KnownOpportunityHistory.merge(currentIds, previous.knownIds)
        if (!canContinue()) return OpportunityAlertRefreshOutcome.DISABLED
        when (
            runCatching {
                history.saveKnownIds(
                    lease = lease,
                    ids = merged.retainedIds,
                    hasBaseline = true,
                )
            }.getOrDefault(OpportunityAlertLeaseAccess.Failed)
        ) {
            is OpportunityAlertLeaseAccess.Granted -> Unit
            OpportunityAlertLeaseAccess.Stale -> return OpportunityAlertRefreshOutcome.DISABLED
            OpportunityAlertLeaseAccess.Failed -> return OpportunityAlertRefreshOutcome.RETRY
        }
        if (!previous.hasBaseline) return OpportunityAlertRefreshOutcome.BASELINED
        if (merged.newCount == 0) return OpportunityAlertRefreshOutcome.NO_NEW_MATCHES

        val now = runCatching(nowEpochMillis).getOrDefault(0L)
        if (now <= 0L) return OpportunityAlertRefreshOutcome.RETRY
        val lastNotification = previous.lastNotificationEpochMillis
        if (lastNotification != null && now - lastNotification < MINIMUM_NOTIFICATION_INTERVAL_MS) {
            return OpportunityAlertRefreshOutcome.THROTTLED
        }

        // Record the throttle before posting so a notification-system failure cannot create spam.
        if (!canContinue()) return OpportunityAlertRefreshOutcome.DISABLED
        when (
            runCatching {
                history.recordNotificationAt(lease = lease, epochMillis = now)
            }.getOrDefault(OpportunityAlertLeaseAccess.Failed)
        ) {
            is OpportunityAlertLeaseAccess.Granted -> Unit
            OpportunityAlertLeaseAccess.Stale -> return OpportunityAlertRefreshOutcome.DISABLED
            OpportunityAlertLeaseAccess.Failed -> return OpportunityAlertRefreshOutcome.RETRY
        }
        val copy = runCatching { notificationCopy(merged.newCount) }.getOrElse {
            return OpportunityAlertRefreshOutcome.NOTIFICATION_UNAVAILABLE
        }
        if (!canContinue()) return OpportunityAlertRefreshOutcome.DISABLED
        // Preference disable/deletion revokes the same persisted lease while holding the same
        // process lock. Whichever side enters first linearizes: a completed opt-out cannot be
        // followed by this stale worker's notification side effect.
        val finalAccess = runCatching {
            history.withActiveLease(lease) {
                when {
                    runCatching(isWorkerStopped).getOrDefault(true) ->
                        FinalNotificationResult.DISABLED
                    !runCatching(isPreferred).getOrDefault(false) ->
                        FinalNotificationResult.DISABLED
                    !runCatching(canNotify).getOrDefault(false) ->
                        FinalNotificationResult.UNAVAILABLE
                    runCatching {
                        postNotification(copy, merged.newCount, now)
                    }.getOrDefault(false) -> FinalNotificationResult.POSTED
                    else -> FinalNotificationResult.UNAVAILABLE
                }
            }
        }.getOrDefault(OpportunityAlertLeaseAccess.Failed)
        return when (finalAccess) {
            is OpportunityAlertLeaseAccess.Granted -> when (finalAccess.value) {
                FinalNotificationResult.DISABLED -> OpportunityAlertRefreshOutcome.DISABLED
                FinalNotificationResult.UNAVAILABLE ->
                    OpportunityAlertRefreshOutcome.NOTIFICATION_UNAVAILABLE
                FinalNotificationResult.POSTED -> OpportunityAlertRefreshOutcome.NOTIFIED
            }
            OpportunityAlertLeaseAccess.Stale -> OpportunityAlertRefreshOutcome.DISABLED
            OpportunityAlertLeaseAccess.Failed ->
                OpportunityAlertRefreshOutcome.NOTIFICATION_UNAVAILABLE
        }
    }

    private suspend fun canContinue(): Boolean {
        currentCoroutineContext().ensureActive()
        if (runCatching(isWorkerStopped).getOrDefault(true)) return false
        return runCatching(isPreferred).getOrDefault(false)
    }

    companion object {
        const val MINIMUM_NOTIFICATION_INTERVAL_MS = 60L * 60L * 1_000L
    }

    private enum class FinalNotificationResult {
        DISABLED,
        UNAVAILABLE,
        POSTED,
    }
}
