package com.rupayonhaldar.gtafreestem.data.local

import com.rupayonhaldar.gtafreestem.domain.search.OpportunitySearchFilters
import com.rupayonhaldar.gtafreestem.domain.search.OpportunitySearchLimits
import com.rupayonhaldar.gtafreestem.domain.search.OpportunitySearchSort
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BrowseSearchStateStoreTest {
    @Test
    fun `query every filter and sort survive a JSON persistence round trip`() {
        val persistence = InMemoryPersistence()
        val first = PersistentBrowseSearchStateStore(persistence)
        val expected = BrowseSearchState(
            query = "robotics café",
            filters = OpportunitySearchFilters(
                region = "Toronto",
                city = "Scarborough",
                category = "Coding & Robotics",
                adultsOnly = true,
                language = "fr-CA",
                volunteerHoursOnly = true,
                coopOnly = true,
                mentorshipOnly = true,
                scholarshipsOnly = true,
                blackFocusedOnly = true,
                girlsFocusedOnly = true,
                indigenousFocusedOnly = true,
                leadershipOnly = true,
                activeOnly = false,
                sort = OpportunitySearchSort.RELEVANCE,
                distanceKm = 25,
                includeNewFinds = false,
            ),
        )

        assertTrue(first.write(expected))
        val restored = PersistentBrowseSearchStateStore(persistence).read()

        assertEquals(expected, restored)
        assertTrue(persistence.stateJson.orEmpty().contains("\"schemaVersion\":1"))
    }

    @Test
    fun `blank and all selections normalize before persistence`() {
        val persistence = InMemoryPersistence()
        val store = PersistentBrowseSearchStateStore(persistence)

        assertTrue(
            store.write(
                BrowseSearchState(
                    filters = OpportunitySearchFilters(
                        region = " All ",
                        city = " ",
                        category = "ALL",
                        language = "all",
                    ),
                ),
            ),
        )

        val filters = store.read().filters
        assertNull(filters.region)
        assertNull(filters.city)
        assertNull(filters.category)
        assertNull(filters.language)
    }

    @Test
    fun `malformed oversized deeply nested and future schema JSON restore defaults`() {
        val defaultState = BrowseSearchState()
        val corruptPayloads = listOf(
            "{not-json",
            "x".repeat(2_000),
            """{"schemaVersion":99,"query":"stale"}""",
            """{"schemaVersion":1,"query":"stale","filters":{"age":121}}""",
            """{"schemaVersion":1,"query":"stale","filters":{"distanceKm":4}}""",
            """{"schemaVersion":1,"unknown":${"[".repeat(70)}0${"]".repeat(70)}}""",
        )

        corruptPayloads.forEach { payload ->
            val store = PersistentBrowseSearchStateStore(
                persistence = InMemoryPersistence(payload),
                maximumPayloadBytes = 1_024,
            )
            assertEquals(defaultState, store.read())
        }
    }

    @Test
    fun `overlong query is rejected without replacing last good state`() {
        val persistence = InMemoryPersistence()
        val store = PersistentBrowseSearchStateStore(persistence)
        val retained = BrowseSearchState(query = "robotics")
        assertTrue(store.write(retained))
        val retainedJson = persistence.stateJson

        assertFalse(
            store.write(
                BrowseSearchState(
                    query = "q".repeat(OpportunitySearchLimits.MAXIMUM_QUERY_LENGTH + 1),
                ),
            ),
        )

        assertEquals(retainedJson, persistence.stateJson)
        assertEquals(retained, store.read())
    }

    @Test
    fun `invalid age distance and overlong selections are rejected`() {
        val store = PersistentBrowseSearchStateStore(InMemoryPersistence())

        assertFalse(
            store.write(
                BrowseSearchState(filters = OpportunitySearchFilters(age = 121)),
            ),
        )
        assertFalse(
            store.write(
                BrowseSearchState(filters = OpportunitySearchFilters(distanceKm = 4)),
            ),
        )
        assertFalse(
            store.write(
                BrowseSearchState(
                    filters = OpportunitySearchFilters(
                        region = "r".repeat(OpportunitySearchLimits.MAXIMUM_SELECTION_LENGTH + 1),
                    ),
                ),
            ),
        )
    }

    @Test
    fun `encoded payload bound rejects an otherwise valid large state`() {
        val persistence = InMemoryPersistence()
        val store = PersistentBrowseSearchStateStore(
            persistence = persistence,
            maximumPayloadBytes = 128,
        )

        assertFalse(store.write(BrowseSearchState(query = "bounded")))
        assertNull(persistence.stateJson)
    }

    @Test
    fun `persistence read and write exceptions safely fall back or report failure`() {
        val failing = object : BrowseSearchStatePersistence {
            override fun readStateJson(): String? = error("read failure")
            override fun writeStateJson(json: String): Boolean = error("write failure")
            override fun clearState(): Boolean = error("clear failure")
        }
        val store = PersistentBrowseSearchStateStore(failing)

        assertEquals(BrowseSearchState(), store.read())
        assertFalse(store.write(BrowseSearchState(query = "safe")))
        assertFalse(store.clear())
    }

    @Test
    fun `clear deletes persisted browse state only when storage confirms success`() {
        val persistence = InMemoryPersistence()
        val store = PersistentBrowseSearchStateStore(persistence)
        val retained = BrowseSearchState(query = "robotics")
        assertTrue(store.write(retained))

        persistence.acceptsClears = false
        assertFalse(store.clear())
        assertEquals(retained, store.read())

        persistence.acceptsClears = true
        assertTrue(store.clear())
        assertNull(persistence.stateJson)
        assertEquals(BrowseSearchState(), store.read())
    }

    private class InMemoryPersistence(
        var stateJson: String? = null,
        var acceptsClears: Boolean = true,
    ) : BrowseSearchStatePersistence {
        override fun readStateJson(): String? = stateJson

        override fun writeStateJson(json: String): Boolean {
            stateJson = json
            return true
        }

        override fun clearState(): Boolean {
            if (!acceptsClears) return false
            stateJson = null
            return true
        }
    }
}
