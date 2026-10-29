package com.rupayonhaldar.gtafreestem.data.local

import com.rupayonhaldar.gtafreestem.domain.search.OpportunitySearchFilters
import com.rupayonhaldar.gtafreestem.domain.search.OpportunitySearchSort
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SavedSearchStoreTest {
    @Test
    fun `three named ages persist independently with neutralized legacy language`() {
        val persistence = MemoryPersistence()
        val store = PersistentSavedSearchStore(persistence)
        val searches = listOf(4, 10, 16).map { age ->
            SavedSearchEntry(
                id = "search-$age", name = "Age $age", query = "robotics",
                filters = OpportunitySearchFilters(
                    city = "Toronto", category = "Coding & Robotics", age = age,
                    sort = OpportunitySearchSort.RELEVANCE, language = "ja",
                ),
                knownMatchingIds = listOf("existing-$age"), hasBaseline = true,
                lastProcessedRevisionEpochMillis = 1000L,
                lastKnownMatchCount = 1,
            )
        }
        assertTrue(store.write(searches))

        val restored = PersistentSavedSearchStore(persistence).read()
        assertEquals(listOf(4, 10, 16), restored.map { it.filters.age })
        assertEquals(listOf("Age 4", "Age 10", "Age 16"), restored.map { it.name })
        assertTrue(restored.all { it.filters.language == null })
        assertTrue(restored.all { it.lastKnownMatchCount == 1 })
        assertTrue(restored.all { it.filters.city == "Toronto" && it.filters.sort == OpportunitySearchSort.RELEVANCE })
        assertTrue(store.write(restored.filterNot { it.id == "search-10" }))
        assertEquals(listOf("search-4", "search-16"), store.read().map { it.id })
    }

    @Test
    fun `corrupt duplicate or oversized collection cannot replace persisted searches`() {
        val persistence = MemoryPersistence()
        val store = PersistentSavedSearchStore(persistence)
        val search = SavedSearchEntry("one", "Science", "", OpportunitySearchFilters(), emptyList())
        assertTrue(store.write(listOf(search)))
        val original = persistence.value
        assertFalse(store.write(listOf(search, search)))
        assertEquals(original, persistence.value)
        assertFalse(store.write(listOf(search.copy(name = " "))))
        assertFalse(store.write(listOf(search.copy(
            filters = OpportunitySearchFilters(latitude = 43.65, longitude = -79.38),
        ))))
        assertFalse(store.write(listOf(search.copy(lastKnownMatchCount = -1))))
        assertEquals(listOf(search), store.read())
        persistence.value = "broken json"
        assertTrue(store.read().isEmpty())
    }

    private class MemoryPersistence(var value: String? = null) : SavedSearchPersistence {
        override fun readJson() = value
        override fun writeJson(value: String): Boolean { this.value = value; return true }
        override fun clear(): Boolean { value = null; return true }
    }
}
