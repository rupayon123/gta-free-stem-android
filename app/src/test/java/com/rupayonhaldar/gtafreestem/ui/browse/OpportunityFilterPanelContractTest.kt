package com.rupayonhaldar.gtafreestem.ui.browse

import com.rupayonhaldar.gtafreestem.domain.search.OpportunityAgeOption
import com.rupayonhaldar.gtafreestem.domain.search.OpportunitySearchFilters
import com.rupayonhaldar.gtafreestem.domain.search.OpportunitySearchSort
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OpportunityFilterPanelContractTest {
    @Test
    fun `visible active count includes every rendered filter exactly once`() {
        val filters = OpportunitySearchFilters(
            region = "Toronto",
            city = "Scarborough",
            category = "Coding & Robotics",
            age = 12,
            language = "fr",
            volunteerHoursOnly = true,
            coopOnly = true,
            mentorshipOnly = true,
            scholarshipsOnly = true,
            blackFocusedOnly = true,
            girlsFocusedOnly = true,
            indigenousFocusedOnly = true,
            leadershipOnly = true,
            sort = OpportunitySearchSort.RELEVANCE,
        )

        assertEquals(14, opportunityFilterPanelActiveCount(filters))
        assertEquals(0, opportunityFilterPanelActiveCount(OpportunitySearchFilters()))
    }

    @Test
    fun `adult bucket counts as one age selection and suppresses exact age`() {
        val filters = OpportunitySearchFilters(age = 17, adultsOnly = true)

        assertEquals(1, opportunityFilterPanelActiveCount(filters))
        assertNull(filters.normalized().age)
        assertTrue(filters.normalized().adultsOnly)
    }

    @Test
    fun `deferred and enforcement fields are not presented as visible filters`() {
        val filters = OpportunitySearchFilters(
            activeOnly = false,
            distanceKm = 25,
            includeNewFinds = false,
        )

        assertEquals(0, opportunityFilterPanelActiveCount(filters))
        assertTrue(filters.hasActiveFilters)
    }

    @Test
    fun `string choices keep any first and retain a missing current value without duplicates`() {
        val choices = stringPanelChoices(
            current = "Toronto",
            available = listOf("Peel", "toronto", "York", "Peel"),
            anyLabel = "Any",
        )

        assertEquals(listOf("Any", "Toronto", "Peel", "York"), choices.map(PanelChoice<*>::label))
        assertNull(choices.first().value)
        assertEquals("Toronto", choices[1].value)
    }

    @Test
    fun `age choices supply localized any plus 18 and retain an uncommon current age`() {
        val current = AgeSelection(age = 18, adultsOnly = false)
        val choices = agePanelChoices(
            current = current,
            available = listOf(
                OpportunityAgeOption(id = "any", label = "Any"),
                OpportunityAgeOption(id = "17", label = "17", age = 17),
                OpportunityAgeOption(id = "18+", label = "18+", adultsOnly = true),
            ),
            anyLabel = "Tous",
        )

        assertEquals("Tous", choices.first().label)
        assertTrue(choices.any { it.value == AgeSelection(age = null, adultsOnly = true) })
        assertTrue(choices.any { it.value == current })
        assertFalse(choices.map(PanelChoice<*>::id).let { it.size != it.distinct().size })
    }

    @Test
    fun `English fallback handles zero singular and plural counts`() {
        val labels = OpportunityFilterPanelLabels.English

        assertEquals("No filters active", labels.activeFilterCount(0))
        assertEquals("1 filter active", labels.activeFilterCount(1))
        assertEquals("3 filters active", labels.activeFilterCount(3))
        assertTrue(labels.scopeDescription.contains("Distance and New Finds"))
    }
}
