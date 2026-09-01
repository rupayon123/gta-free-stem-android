package com.rupayonhaldar.gtafreestem.platform.alerts

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit

internal class WorkManagerOpportunityAlertSchedule(
    context: Context,
) : OpportunityAlertWorkSchedule {
    private val workManager = WorkManager.getInstance(context.applicationContext)
    private val leaseProtectedSchedule = LeaseProtectedOpportunityAlertWorkSchedule(
        leaseStore = SharedPreferencesOpportunityAlertHistoryStore(context.applicationContext),
        scheduleWork = ::enqueuePeriodicWork,
        cancelWork = ::cancelPeriodicWork,
    )

    override fun revokeRunningRefresh(): Boolean =
        leaseProtectedSchedule.revokeRunningRefresh()

    override fun schedule(): Boolean = leaseProtectedSchedule.schedule()

    override fun cancel(): Boolean = leaseProtectedSchedule.cancel()

    private fun enqueuePeriodicWork(lease: OpportunityAlertRunLease): Boolean = runCatching {
        val request = PeriodicWorkRequestBuilder<OpportunityAlertWorker>(
            REPEAT_INTERVAL_HOURS,
            TimeUnit.HOURS,
        )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .setInputData(workDataOf(LEASE_INPUT_KEY to lease.token))
            .addTag(WORK_TAG)
            .build()
        workManager.enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
        true
    }.getOrDefault(false)

    private fun cancelPeriodicWork(): Boolean = runCatching {
        workManager.cancelUniqueWork(UNIQUE_WORK_NAME)
        true
    }.getOrDefault(false)

    companion object {
        const val UNIQUE_WORK_NAME = "gta-free-stem-new-opportunity-refresh"
        const val WORK_TAG = "gta-free-stem-opportunity-alert"
        const val REPEAT_INTERVAL_HOURS = 3L
        const val LEASE_INPUT_KEY = "opportunity-alert-refresh-lease"
    }
}

/**
 * Couples WorkManager scheduling to a persisted refresh lease. Cancellation revokes first;
 * scheduling rotates first, binds that exact token into WorkRequest input, and rolls the lease
 * back when enqueueing fails.
 */
internal class LeaseProtectedOpportunityAlertWorkSchedule(
    private val leaseStore: OpportunityAlertLeaseStore,
    private val scheduleWork: (OpportunityAlertRunLease) -> Boolean,
    private val cancelWork: () -> Boolean,
) : OpportunityAlertWorkSchedule {
    override fun revokeRunningRefresh(): Boolean = synchronized(PROCESS_SCHEDULE_LOCK) {
        runCatching(leaseStore::invalidateLease).getOrDefault(false)
    }

    override fun schedule(): Boolean = synchronized(PROCESS_SCHEDULE_LOCK) {
        val lease = runCatching(leaseStore::replaceActiveLease).getOrNull() ?: return false

        val scheduled = runCatching { scheduleWork(lease) }.getOrDefault(false)
        if (scheduled) return@synchronized true

        runCatching(leaseStore::invalidateLease)
        false
    }

    override fun cancel(): Boolean = synchronized(PROCESS_SCHEDULE_LOCK) {
        // Revocation is deliberately first. WorkManager cancellation is asynchronous, while the
        // lease prevents a refresh that is already executing from crossing a guarded side effect.
        val invalidated = runCatching(leaseStore::invalidateLease).getOrDefault(false)
        val cancelled = runCatching(cancelWork).getOrDefault(false)
        invalidated && cancelled
    }

    private companion object {
        /** Serializes multiple Activity/test adapters so WorkRequest input matches the live lease. */
        val PROCESS_SCHEDULE_LOCK = Any()
    }
}

object OpportunityAlertPlatform {
    /** Used by the local-data deletion flow; no server or push registration exists. */
    fun clearLocalHistory(context: Context): Boolean {
        // Clearing history revokes the worker lease under the same process lock used by the final
        // notification post. Cancel after that barrier so even a post that linearized first is gone.
        val historyCleared = SharedPreferencesOpportunityAlertHistoryStore(context).clear()
        val notificationsCleared = OpportunityNotificationPublisher.clearPostedNotifications(context)
        return historyCleared && notificationsCleared
    }
}
