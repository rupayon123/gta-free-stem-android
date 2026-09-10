package com.rupayonhaldar.gtafreestem.platform.alerts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OpportunityAlertPreferenceControllerTest {
    @Test
    fun permittedEnablePersistsAndSchedules() {
        val fixture = Fixture(hasPermission = true)

        assertEquals(
            OpportunityAlertPreferenceUpdate.APPLIED,
            fixture.controller.update(preferred = true),
        )
        assertTrue(fixture.preferred)
        assertEquals(1, fixture.schedule.scheduleCount)
        assertEquals(0, fixture.schedule.cancelCount)
        assertEquals(listOf("revoke", "write:true", "schedule"), fixture.events)
    }

    @Test
    fun permissionRequestDoesNotPersistMisleadingEnabledState() {
        val fixture = Fixture(hasPermission = false)

        assertEquals(
            OpportunityAlertPreferenceUpdate.REQUEST_PERMISSION,
            fixture.controller.update(preferred = true),
        )
        assertFalse(fixture.preferred)
        assertEquals(1, fixture.schedule.cancelCount)
    }

    @Test
    fun permissionGrantEnablesAndDenialDisables() {
        val granted = Fixture(hasPermission = false)
        assertTrue(granted.controller.completePermissionRequest(granted = true))
        assertTrue(granted.preferred)
        assertEquals(1, granted.schedule.scheduleCount)
        assertEquals(0, granted.schedule.cancelCount)

        val denied = Fixture(hasPermission = false, initiallyPreferred = true)
        assertTrue(denied.controller.completePermissionRequest(granted = false))
        assertFalse(denied.preferred)
        assertEquals(1, denied.schedule.cancelCount)
    }

    @Test
    fun schedulingFailureRollsBackStoredPreference() {
        val fixture = Fixture(hasPermission = true)
        fixture.schedule.scheduleSucceeds = false

        assertEquals(
            OpportunityAlertPreferenceUpdate.FAILED,
            fixture.controller.update(preferred = true),
        )
        assertFalse(fixture.preferred)
        assertEquals(1, fixture.schedule.cancelCount)
        assertEquals(
            listOf("revoke", "write:true", "schedule", "cancel", "write:false"),
            fixture.events,
        )
    }

    @Test
    fun blockedAppOrChannelKeepsPreferenceOffAndCancelsWork() {
        val fixture = Fixture(
            hasPermission = true,
            canPost = false,
            initiallyPreferred = true,
        )

        assertEquals(
            OpportunityAlertPreferenceUpdate.FAILED,
            fixture.controller.update(preferred = true),
        )
        assertFalse(fixture.preferred)
        assertEquals(1, fixture.schedule.cancelCount)

        fixture.preferred = true
        assertTrue(fixture.controller.reconcile())
        assertFalse(fixture.preferred)
        assertEquals(2, fixture.schedule.cancelCount)
    }

    @Test
    fun disableRevokesAndCancelsBeforePersistingTheOptOut() {
        val fixture = Fixture(hasPermission = true, initiallyPreferred = true)

        assertEquals(
            OpportunityAlertPreferenceUpdate.APPLIED,
            fixture.controller.update(preferred = false),
        )

        assertFalse(fixture.preferred)
        assertEquals(listOf("cancel", "write:false"), fixture.events)
    }

    @Test
    fun disablePersistsOptOutEvenWhenTheActivityReadCacheIsStale() {
        var persistedPreference = true
        val events = mutableListOf<String>()
        val controller = OpportunityAlertPreferenceController(
            readPreferred = { false },
            writePreferred = { value ->
                events += "write:$value"
                persistedPreference = value
                true
            },
            hasNotificationPermission = { true },
            canPostNotifications = { true },
            workSchedule = RecordingSchedule(events),
        )

        assertEquals(
            OpportunityAlertPreferenceUpdate.APPLIED,
            controller.update(preferred = false),
        )
        assertFalse(persistedPreference)
        assertEquals(listOf("cancel", "write:false"), events)
    }

    private class Fixture(
        private var hasPermission: Boolean,
        private var canPost: Boolean = true,
        initiallyPreferred: Boolean = false,
    ) {
        var preferred = initiallyPreferred
        val events = mutableListOf<String>()
        val schedule = RecordingSchedule(events)
        val controller = OpportunityAlertPreferenceController(
            readPreferred = { preferred },
            writePreferred = { value ->
                events += "write:$value"
                preferred = value
                true
            },
            hasNotificationPermission = { hasPermission },
            canPostNotifications = { canPost },
            workSchedule = schedule,
        )
    }

    private class RecordingSchedule(
        private val events: MutableList<String>,
    ) : OpportunityAlertWorkSchedule {
        var scheduleSucceeds = true
        var scheduleCount = 0
        var cancelCount = 0

        override fun revokeRunningRefresh(): Boolean {
            events += "revoke"
            return true
        }

        override fun schedule(): Boolean {
            events += "schedule"
            scheduleCount += 1
            return scheduleSucceeds
        }

        override fun cancel(): Boolean {
            events += "cancel"
            cancelCount += 1
            return true
        }
    }
}
