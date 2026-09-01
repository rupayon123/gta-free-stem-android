package com.rupayonhaldar.gtafreestem.platform.alerts

enum class OpportunityAlertPreferenceUpdate {
    APPLIED,
    REQUEST_PERMISSION,
    FAILED,
}

internal interface OpportunityAlertWorkSchedule {
    /** Revokes any prior run without issuing a second asynchronous WorkManager cancellation. */
    fun revokeRunningRefresh(): Boolean

    /** Activates a fresh persisted run lease before enqueueing periodic work. */
    fun schedule(): Boolean

    /** Revokes the run lease synchronously before requesting asynchronous work cancellation. */
    fun cancel(): Boolean
}

/**
 * Keeps the stored switch truthful: enabled means permission exists and periodic work is queued.
 * A permission request is initiated by the Activity only after REQUEST_PERMISSION is returned.
 */
internal class OpportunityAlertPreferenceController(
    private val readPreferred: () -> Boolean,
    private val writePreferred: (Boolean) -> Boolean,
    private val hasNotificationPermission: () -> Boolean,
    private val canPostNotifications: () -> Boolean,
    private val workSchedule: OpportunityAlertWorkSchedule,
) {
    fun update(preferred: Boolean): OpportunityAlertPreferenceUpdate {
        if (!preferred) {
            return if (disable()) {
                OpportunityAlertPreferenceUpdate.APPLIED
            } else {
                OpportunityAlertPreferenceUpdate.FAILED
            }
        }

        if (!runCatching(hasNotificationPermission).getOrDefault(false)) {
            return if (disable()) {
                OpportunityAlertPreferenceUpdate.REQUEST_PERMISSION
            } else {
                OpportunityAlertPreferenceUpdate.FAILED
            }
        }

        if (!runCatching(canPostNotifications).getOrDefault(false)) {
            disable()
            return OpportunityAlertPreferenceUpdate.FAILED
        }

        return if (enable()) {
            OpportunityAlertPreferenceUpdate.APPLIED
        } else {
            OpportunityAlertPreferenceUpdate.FAILED
        }
    }

    /** Completes the user-initiated Android permission flow without ever storing a denied state. */
    fun completePermissionRequest(granted: Boolean): Boolean =
        if (granted && runCatching(canPostNotifications).getOrDefault(false)) {
            enable()
        } else {
            disable()
        }

    /** Repairs permission changes made in Android settings and restores/cancels unique work. */
    fun reconcile(): Boolean {
        val preferred = runCatching(readPreferred).getOrDefault(false)
        return when {
            !preferred -> runCatching(workSchedule::cancel).getOrDefault(false)
            !runCatching(hasNotificationPermission).getOrDefault(false) -> disable()
            !runCatching(canPostNotifications).getOrDefault(false) -> disable()
            else -> runCatching(workSchedule::schedule).getOrDefault(false)
        }
    }

    private fun enable(): Boolean {
        // Revoke any prior run before making the stored preference true. This closes the window
        // where a stale worker could become authorized again during an off -> on transition.
        val isolated = runCatching(workSchedule::revokeRunningRefresh).getOrDefault(false)
        if (!isolated) return false

        val saved = runCatching { writePreferred(true) }.getOrDefault(false)
        if (!saved) return false
        val scheduled = runCatching(workSchedule::schedule).getOrDefault(false)
        if (scheduled) return true

        // Revoke before rolling the preference back so in-flight work fails closed even when the
        // preference write or asynchronous WorkManager cancellation reports a failure.
        runCatching(workSchedule::cancel)
        runCatching { writePreferred(false) }
        return false
    }

    private fun disable(): Boolean {
        // The schedule owns the persisted run lease and invalidates it synchronously before asking
        // WorkManager to cancel. Always persist false after that linearization point: another
        // Activity/store instance may have enabled alerts while this controller's read cache was
        // stale, so skipping an apparently redundant write could resurrect alerts after deletion.
        val cancelled = runCatching(workSchedule::cancel).getOrDefault(false)
        val saved = runCatching { writePreferred(false) }.getOrDefault(false)
        return saved && cancelled
    }
}
