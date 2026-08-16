package com.rupayonhaldar.gtafreestem.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalAccountPreferencesStoreTest {
    @Test
    fun `missing persisted values use privacy-safe system defaults`() {
        val store = PersistentLocalAccountPreferencesStore(FakePersistence())

        assertEquals(
            LocalAccountPreferences(
                displayName = null,
                theme = AppThemePreference.SYSTEM,
                opportunityAlertsPreferred = false,
            ),
            store.currentPreferences(),
        )
        assertFalse(store.currentPreferences().hasProfile)
    }

    @Test
    fun `corrupt values and read failures recover without crashing`() {
        val corrupt = PersistentLocalAccountPreferencesStore(
            FakePersistence(
                displayName = "x".repeat(LocalDisplayNamePolicy.MAXIMUM_CODE_POINTS + 1),
                theme = "ultraviolet",
                alertsPreferred = null,
            ),
        )
        val throwing = PersistentLocalAccountPreferencesStore(
            FakePersistence(throwOnRead = true),
        )

        assertEquals(LocalAccountPreferences(), corrupt.currentPreferences())
        assertEquals(LocalAccountPreferences(), throwing.currentPreferences())
    }

    @Test
    fun `legacy capitalization is accepted and a saved name is normalized`() {
        val persistence = FakePersistence(
            displayName = "  Ada\n  Lovelace  ",
            theme = " Dark ",
            alertsPreferred = true,
        )
        val store = PersistentLocalAccountPreferencesStore(persistence)

        assertEquals(
            LocalAccountPreferences("Ada Lovelace", AppThemePreference.DARK, true),
            store.currentPreferences(),
        )
        assertEquals(
            DisplayNameSaveResult.SAVED,
            store.saveDisplayName("  Grace   Hopper  "),
        )
        assertEquals("Grace Hopper", persistence.displayName)
        assertTrue(store.currentPreferences().hasProfile)
    }

    @Test
    fun `blank control and oversized display names are rejected without a write`() {
        val persistence = FakePersistence(displayName = "Existing")
        val store = PersistentLocalAccountPreferencesStore(persistence)

        assertEquals(DisplayNameSaveResult.INVALID, store.saveDisplayName(" \n\t "))
        assertEquals(DisplayNameSaveResult.INVALID, store.saveDisplayName("Bad\u0000Name"))
        assertEquals(
            DisplayNameSaveResult.INVALID,
            store.saveDisplayName("x".repeat(LocalDisplayNamePolicy.MAXIMUM_CODE_POINTS + 1)),
        )
        assertEquals("Existing", store.currentPreferences().displayName)
        assertEquals(0, persistence.displayNameWriteCount)
    }

    @Test
    fun `display name bound counts Unicode code points rather than UTF 16 units`() {
        val persistence = FakePersistence()
        val store = PersistentLocalAccountPreferencesStore(persistence)
        val maximumEmojiName = "🤖".repeat(LocalDisplayNamePolicy.MAXIMUM_CODE_POINTS)

        assertEquals(DisplayNameSaveResult.SAVED, store.saveDisplayName(maximumEmojiName))
        assertEquals(
            DisplayNameSaveResult.INVALID,
            store.saveDisplayName(maximumEmojiName + "🤖"),
        )
    }

    @Test
    fun `theme alerts and profile clearing persist independently`() {
        val persistence = FakePersistence(displayName = "Ada")
        val store = PersistentLocalAccountPreferencesStore(persistence)

        assertTrue(store.setTheme(AppThemePreference.LIGHT))
        assertTrue(store.setOpportunityAlertsPreferred(true))
        assertTrue(store.clearProfile())

        assertEquals(
            LocalAccountPreferences(null, AppThemePreference.LIGHT, true),
            store.currentPreferences(),
        )
        assertNull(persistence.displayName)
        assertEquals("light", persistence.theme)
        assertEquals(true, persistence.alertsPreferred)
    }

    @Test
    fun `failed and throwing writes leave the last confirmed state unchanged`() {
        val rejectedPersistence = FakePersistence(displayName = "Ada", acceptsWrites = false)
        val rejectedStore = PersistentLocalAccountPreferencesStore(rejectedPersistence)
        val throwingPersistence = FakePersistence(displayName = "Grace", throwOnWrite = true)
        val throwingStore = PersistentLocalAccountPreferencesStore(throwingPersistence)

        assertEquals(
            DisplayNameSaveResult.STORAGE_ERROR,
            rejectedStore.saveDisplayName("New name"),
        )
        assertFalse(rejectedStore.setTheme(AppThemePreference.DARK))
        assertFalse(rejectedStore.setOpportunityAlertsPreferred(true))
        assertFalse(rejectedStore.clearProfile())
        assertEquals(LocalAccountPreferences("Ada"), rejectedStore.currentPreferences())

        assertEquals(
            DisplayNameSaveResult.STORAGE_ERROR,
            throwingStore.saveDisplayName("New name"),
        )
        assertFalse(throwingStore.setTheme(AppThemePreference.DARK))
        assertFalse(throwingStore.setOpportunityAlertsPreferred(true))
        assertFalse(throwingStore.clearProfile())
        assertEquals(LocalAccountPreferences("Grace"), throwingStore.currentPreferences())
    }

    private class FakePersistence(
        var displayName: String? = null,
        var theme: String? = null,
        var alertsPreferred: Boolean? = null,
        private val acceptsWrites: Boolean = true,
        private val throwOnRead: Boolean = false,
        private val throwOnWrite: Boolean = false,
    ) : LocalAccountPreferencesPersistence {
        var displayNameWriteCount = 0
            private set

        override fun readDisplayName(): String? = read { displayName }

        override fun readTheme(): String? = read { theme }

        override fun readOpportunityAlertsPreferred(): Boolean? = read { alertsPreferred }

        override fun writeDisplayName(displayName: String?): Boolean = write {
            displayNameWriteCount += 1
            this.displayName = displayName
        }

        override fun writeTheme(theme: String): Boolean = write { this.theme = theme }

        override fun writeOpportunityAlertsPreferred(preferred: Boolean): Boolean = write {
            alertsPreferred = preferred
        }

        private fun <T> read(block: () -> T): T {
            if (throwOnRead) error("read failure")
            return block()
        }

        private fun write(update: () -> Unit): Boolean {
            if (throwOnWrite) error("write failure")
            if (!acceptsWrites) return false
            update()
            return true
        }
    }
}
