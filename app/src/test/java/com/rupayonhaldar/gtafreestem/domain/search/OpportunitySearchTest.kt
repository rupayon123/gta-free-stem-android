package com.rupayonhaldar.gtafreestem.domain.search

import com.rupayonhaldar.gtafreestem.domain.model.Opportunity
import com.rupayonhaldar.gtafreestem.domain.model.OpportunityTranslation
import com.rupayonhaldar.gtafreestem.localization.AppLanguage
import com.rupayonhaldar.gtafreestem.localization.AppStringCatalog
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OpportunitySearchTest {
    private val now = Instant.parse("2026-08-16T16:00:00Z")

    @Test
    fun `query follows selected translation plus English source when language switches`() {
        val translated = opportunity(
            id = "robotics",
            title = "Robotics Café",
            translations = mapOf(
                "es" to OpportunityTranslation(
                    title = "Club de Robótica",
                    tags = listOf("programación"),
                ),
                "fr" to OpportunityTranslation(
                    title = "Club de robotique",
                    tags = listOf("codage"),
                ),
            ),
        )

        assertEquals(
            listOf("robotics"),
            OpportunitySearch.search(
                listOf(translated, opportunity("science")),
                query = "ROBOTICA programacion",
                now = now,
                language = AppLanguage.SPANISH,
            ).map(Opportunity::id),
        )
        assertTrue(
            OpportunitySearch.search(
                listOf(translated),
                query = "cafe",
                now = now,
                language = AppLanguage.SPANISH,
            ).isNotEmpty(),
        )
        assertTrue(
            OpportunitySearch.search(
                listOf(translated),
                query = "programacion",
                now = now,
                language = AppLanguage.FRENCH,
            ).isEmpty(),
        )
        assertEquals(
            listOf("robotics"),
            OpportunitySearch.search(
                listOf(translated),
                query = "codage",
                now = now,
                language = AppLanguage.FRENCH,
            ).map(Opportunity::id),
        )
        assertTrue(OpportunitySearch.search(listOf(translated), query = "missing", now = now).isEmpty())
    }

    @Test
    fun `localized catalog category is searchable when feed translation is missing`() {
        val catalog = AppStringCatalog.decode(
            """
            {
              "en": {"categoryCodingAndRobotics": "Coding and robotics"},
              "es": {"categoryCodingAndRobotics": "Programación y robótica"}
            }
            """.trimIndent(),
        )
        val opportunity = opportunity(
            id = "catalog-category",
            category = "Coding & Robotics",
        )

        assertEquals(
            listOf("catalog-category"),
            OpportunitySearch.search(
                opportunities = listOf(opportunity),
                query = "programacion",
                now = now,
                language = AppLanguage.SPANISH,
                catalog = catalog,
            ).map(Opportunity::id),
        )
        assertTrue(
            OpportunitySearch.search(
                opportunities = listOf(opportunity),
                query = "programacion",
                now = now,
                language = AppLanguage.ENGLISH,
                catalog = catalog,
            ).isEmpty(),
        )
    }

    @Test
    fun `region city category exact age and language filters combine with AND`() {
        val match = opportunity(
            id = "match",
            region = "Toronto",
            city = "Toronto",
            category = "Coding & Robotics",
            categories = listOf("STEM", "Coding & Robotics"),
            ageMin = 10,
            ageMax = 14,
            languages = listOf("en", "fr-CA"),
        )
        val wrongCity = match.copy(id = "wrong-city", city = "Brampton")
        val filters = OpportunitySearchFilters(
            region = "toronto",
            city = "TORONTO",
            category = "coding & robotics",
            age = 12,
            language = "fr",
        )

        assertEquals(
            listOf("match"),
            OpportunitySearch.search(listOf(wrongCity, match), filters = filters, now = now)
                .map(Opportunity::id),
        )
    }

    @Test
    fun `18 plus is an adult bucket while exact 18 still includes youth capped at 18`() {
        val youthOnly = opportunity("youth", ageMin = 13, ageMax = 18)
        val adultCompatible = opportunity("adult", ageMin = 18, ageMax = 24)
        val openEnded = opportunity("open", ageMin = 6, ageMax = null)

        assertEquals(
            setOf("adult", "open"),
            OpportunitySearch.search(
                listOf(youthOnly, adultCompatible, openEnded),
                filters = OpportunitySearchFilters(adultsOnly = true),
                now = now,
            ).map(Opportunity::id).toSet(),
        )
        assertEquals(
            setOf("youth", "adult", "open"),
            OpportunitySearch.search(
                listOf(youthOnly, adultCompatible, openEnded),
                filters = OpportunitySearchFilters(age = 18),
                now = now,
            ).map(Opportunity::id).toSet(),
        )
    }

    @Test
    fun `language matching accepts locale aliases but not a different language`() {
        val opportunity = opportunity("mandarin", languages = listOf("zh-Hans", "en-CA"))

        assertEquals(
            listOf("mandarin"),
            OpportunitySearch.search(
                listOf(opportunity),
                filters = OpportunitySearchFilters(language = "zh"),
                now = now,
            ).map(Opportunity::id),
        )
        assertTrue(
            OpportunitySearch.search(
                listOf(opportunity),
                filters = OpportunitySearchFilters(language = "fr"),
                now = now,
            ).isEmpty(),
        )
    }

    @Test
    fun `volunteer and coop predicates accept explicit flags and parity keywords`() {
        val volunteerFlag = opportunity("volunteer-flag", volunteerHoursEligible = true)
        val volunteerText = opportunity("volunteer-text", description = "Earn community service credit.")
        val coopFlag = opportunity("coop-flag", coopEligible = true)
        val coopText = opportunity("coop-text", tags = listOf("SHSM placement"))
        val all = listOf(volunteerFlag, volunteerText, coopFlag, coopText)

        assertEquals(
            setOf("volunteer-flag", "volunteer-text"),
            OpportunitySearch.search(
                all,
                filters = OpportunitySearchFilters(volunteerHoursOnly = true),
                now = now,
            ).map(Opportunity::id).toSet(),
        )
        assertEquals(
            setOf("coop-flag", "coop-text"),
            OpportunitySearch.search(
                all,
                filters = OpportunitySearchFilters(coopOnly = true),
                now = now,
            ).map(Opportunity::id).toSet(),
        )
    }

    @Test
    fun `mentorship and scholarship predicates follow iOS keyword coverage`() {
        val mentor = opportunity("mentor", tags = listOf("Career role model"))
        val scholarship = opportunity("scholarship", description = "A bursary and financial aid program.")
        val unrelated = opportunity("other")

        assertEquals(
            listOf("mentor"),
            OpportunitySearch.search(
                listOf(unrelated, mentor, scholarship),
                filters = OpportunitySearchFilters(mentorshipOnly = true),
                now = now,
            ).map(Opportunity::id),
        )
        assertEquals(
            listOf("scholarship"),
            OpportunitySearch.search(
                listOf(unrelated, mentor, scholarship),
                filters = OpportunitySearchFilters(scholarshipsOnly = true),
                now = now,
            ).map(Opportunity::id),
        )
    }

    @Test
    fun `equity and leadership predicates search tags community fields and translations`() {
        val black = opportunity("black", communityFocus = listOf("Black and Caribbean youth"))
        val girls = opportunity("girls", tags = listOf("Women in technology"))
        val indigenous = opportunity(
            "indigenous",
            translations = mapOf("fr" to OpportunityTranslation(tags = listOf("Jeunes Métis"))),
        )
        val leadership = opportunity("leadership", description = "Join the youth council.")
        val all = listOf(black, girls, indigenous, leadership)

        assertEquals(
            listOf("black"),
            ids(all, OpportunitySearchFilters(blackFocusedOnly = true)),
        )
        assertEquals(
            listOf("girls"),
            ids(all, OpportunitySearchFilters(girlsFocusedOnly = true)),
        )
        assertEquals(
            listOf("indigenous"),
            OpportunitySearch.search(
                opportunities = all,
                filters = OpportunitySearchFilters(indigenousFocusedOnly = true),
                now = now,
                language = AppLanguage.FRENCH,
            ).map(Opportunity::id),
        )
        assertEquals(
            listOf("leadership"),
            ids(all, OpportunitySearchFilters(leadershipOnly = true)),
        )
    }

    @Test
    fun `multiple pathway and equity toggles require every predicate`() {
        val both = opportunity(
            "both",
            tags = listOf("Volunteer hours", "Girls in STEM"),
        )
        val volunteerOnly = opportunity("volunteer", tags = listOf("Volunteer hours"))
        val girlsOnly = opportunity("girls", tags = listOf("Girls in STEM"))

        assertEquals(
            listOf("both"),
            ids(
                listOf(volunteerOnly, girlsOnly, both),
                OpportunitySearchFilters(
                    volunteerHoursOnly = true,
                    girlsFocusedOnly = true,
                ),
            ),
        )
    }

    @Test
    fun `free active and effective deadline rules are enforced by default`() {
        val valid = opportunity("valid")
        val paid = opportunity("paid", cost = "${'$'}40")
        val inactive = opportunity("inactive", status = "cancelled")
        val closedRegistration = opportunity(
            "closed",
            startDate = "2026-09-01T12:00:00Z",
            endDate = "2026-09-30T12:00:00Z",
            deadline = "2026-08-15T12:00:00Z",
        )
        val mirroredStartDeadline = opportunity(
            "ongoing",
            startDate = "2026-06-01T12:00:00Z",
            endDate = "2026-09-30T12:00:00Z",
            deadline = "2026-06-01T12:00:00Z",
        )
        val all = listOf(valid, paid, inactive, closedRegistration, mirroredStartDeadline)

        assertEquals(setOf("valid", "ongoing"), ids(all).toSet())
        assertEquals(
            setOf("valid", "inactive", "closed", "ongoing"),
            ids(all, OpportunitySearchFilters(activeOnly = false)).toSet(),
        )
        assertFalse(ids(all, OpportunitySearchFilters(activeOnly = false)).contains("paid"))
    }

    @Test
    fun `nearby filtering validates coordinates applies radius and returns Haversine distance`() {
        val originLatitude = 43.6532
        val originLongitude = -79.3832
        val atOrigin = opportunity("at-origin").copy(
            latitude = originLatitude,
            longitude = originLongitude,
        )
        val nearby = opportunity("nearby").copy(
            latitude = originLatitude + 0.02,
            longitude = originLongitude,
        )
        val outsideRadius = opportunity("outside").copy(
            latitude = originLatitude + 0.10,
            longitude = originLongitude,
        )
        val missingCoordinates = opportunity("missing")

        val results = OpportunitySearch.search(
            opportunities = listOf(outsideRadius, missingCoordinates, nearby, atOrigin),
            filters = OpportunitySearchFilters(
                latitude = originLatitude,
                longitude = originLongitude,
                distanceKm = 5,
                sort = OpportunitySearchSort.NEAREST,
            ),
            now = now,
        )

        assertEquals(listOf("at-origin", "nearby"), results.map(Opportunity::id))
        assertEquals(0.0, results.first().distanceKm ?: error("missing distance"), 0.0001)
        assertEquals(2.22, results.last().distanceKm ?: error("missing distance"), 0.05)

        val fiftyFiveKmNorth = opportunity("north").copy(
            latitude = originLatitude + 0.5,
            longitude = originLongitude,
        )
        val distance = OpportunitySearch.search(
            opportunities = listOf(fiftyFiveKmNorth),
            filters = OpportunitySearchFilters(
                latitude = originLatitude,
                longitude = originLongitude,
                sort = OpportunitySearchSort.NEAREST,
            ),
            now = now,
        ).single().distanceKm
        assertEquals(55.6, distance ?: error("missing distance"), 0.2)
    }

    @Test
    fun `nearest sort uses stable soonest fallback without a valid origin`() {
        val later = opportunity("later", startDate = "2026-10-01T12:00:00Z")
        val sooner = opportunity("sooner", startDate = "2026-09-01T12:00:00Z")

        assertEquals(
            listOf("sooner", "later"),
            OpportunitySearch.search(
                opportunities = listOf(later, sooner),
                filters = OpportunitySearchFilters(
                    latitude = 91.0,
                    longitude = -79.0,
                    distanceKm = 25,
                    sort = OpportunitySearchSort.NEAREST,
                ),
                now = now,
            ).map(Opportunity::id),
        )
    }

    @Test
    fun `coordinate and radius normalization rejects partial nonfinite and out of range values`() {
        listOf(
            OpportunitySearchFilters(latitude = 43.0, longitude = null, distanceKm = 25),
            OpportunitySearchFilters(latitude = Double.NaN, longitude = -79.0, distanceKm = 25),
            OpportunitySearchFilters(latitude = 43.0, longitude = -181.0, distanceKm = 25),
        ).forEach { invalid ->
            val normalized = invalid.normalized()
            assertEquals(null, normalized.latitude)
            assertEquals(null, normalized.longitude)
            assertEquals(25, normalized.distanceKm)
            assertFalse(normalized.hasValidLocation)
        }

        val validLocation = OpportunitySearchFilters(
            latitude = 43.0,
            longitude = -79.0,
            distanceKm = 4,
        ).normalized()
        assertEquals(43.0, validLocation.latitude ?: error("missing latitude"), 0.0)
        assertEquals(-79.0, validLocation.longitude ?: error("missing longitude"), 0.0)
        assertEquals(null, validLocation.distanceKm)
        assertTrue(validLocation.hasValidLocation)
    }

    @Test
    fun `new find filter excludes only explicitly new records`() {
        val newFind = opportunity("new").copy(isNewFind = true)
        val known = opportunity("known").copy(isNewFind = false)
        val unspecified = opportunity("unspecified")

        assertEquals(
            setOf("known", "unspecified"),
            OpportunitySearch.search(
                opportunities = listOf(newFind, known, unspecified),
                filters = OpportunitySearchFilters(includeNewFinds = false),
                now = now,
            ).map(Opportunity::id).toSet(),
        )
    }

    @Test
    fun `soonest sorting uses earliest upcoming date and deterministic title and ID ties`() {
        val beta = opportunity(
            "beta",
            title = "Beta",
            startDate = "2026-09-01T12:00:00Z",
            endDate = "2026-10-01T12:00:00Z",
        )
        val alphaB = beta.copy(id = "alpha-b", title = "Alpha")
        val alphaA = beta.copy(id = "alpha-a", title = "Alpha")
        val deadlineFirst = opportunity(
            "deadline",
            title = "Deadline",
            startDate = "2026-09-10T12:00:00Z",
            endDate = "2026-10-01T12:00:00Z",
            deadline = "2026-09-05T12:00:00Z",
        )
        val undated = opportunity(
            "undated",
            title = "Undated",
            startDate = null,
            endDate = null,
        )

        assertEquals(
            listOf("alpha-a", "alpha-b", "beta", "deadline", "undated"),
            OpportunitySearch.search(
                listOf(undated, deadlineFirst, beta, alphaB, alphaA),
                now = now,
            ).map(Opportunity::id),
        )
    }

    @Test
    fun `relevance weights title above description then uses soonest and stable ties`() {
        val descriptionMatch = opportunity(
            "description",
            title = "Alpha Program",
            description = "Hands-on robotics activities.",
            startDate = "2026-09-01T12:00:00Z",
        )
        val laterTitleMatch = opportunity(
            "title",
            title = "Robotics Club",
            description = "Hands-on activities.",
            startDate = "2026-09-20T12:00:00Z",
        )
        val titleTieSooner = opportunity(
            "tie-a",
            title = "Robotics Workshop",
            startDate = "2026-09-10T12:00:00Z",
        )

        assertEquals(
            listOf("tie-a", "title", "description"),
            OpportunitySearch.search(
                listOf(descriptionMatch, laterTitleMatch, titleTieSooner),
                query = "robotics",
                filters = OpportunitySearchFilters(sort = OpportunitySearchSort.RELEVANCE),
                now = now,
            ).map(Opportunity::id),
        )
    }

    @Test
    fun `relevance without a query falls back to soonest sorting`() {
        val later = opportunity("later", startDate = "2026-10-01T12:00:00Z")
        val sooner = opportunity("sooner", startDate = "2026-09-01T12:00:00Z")

        assertEquals(
            listOf("sooner", "later"),
            OpportunitySearch.search(
                listOf(later, sooner),
                filters = OpportunitySearchFilters(sort = OpportunitySearchSort.RELEVANCE),
                now = now,
            ).map(Opportunity::id),
        )
    }

    @Test
    fun `option lists are distinct sorted and include exact youth ages plus adult bucket`() {
        val options = OpportunitySearch.options(
            listOf(
                opportunity(
                    "one",
                    region = "York",
                    city = "Vaughan",
                    category = "STEM",
                    categories = listOf("STEM", "Coding & Robotics"),
                    languages = listOf("fr", "en"),
                ),
                opportunity(
                    "two",
                    region = "toronto",
                    city = "Toronto",
                    category = "coding & robotics",
                    languages = listOf("EN", "zh-Hans"),
                ),
            ),
        )

        assertEquals(listOf("toronto", "York"), options.regions)
        assertEquals(listOf("Toronto", "Vaughan"), options.cities)
        assertEquals(listOf("Coding & Robotics", "STEM"), options.categories)
        assertEquals(listOf("en", "fr", "zh-Hans"), options.languages)
        assertEquals(20, options.ages.size)
        assertEquals("any", options.ages.first().id)
        assertEquals("18+", options.ages.last().id)
        assertTrue(options.ages.last().adultsOnly)
    }

    @Test
    fun `normalization treats all and blank selections as defaults`() {
        val filters = OpportunitySearchFilters(
            region = " All ",
            city = " ",
            category = "ALL",
            age = 12,
            adultsOnly = true,
            language = "all",
        ).normalized()

        assertEquals(null, filters.region)
        assertEquals(null, filters.city)
        assertEquals(null, filters.category)
        assertEquals(null, filters.age)
        assertEquals(null, filters.language)
        assertTrue(filters.adultsOnly)
    }

    private fun ids(
        opportunities: List<Opportunity>,
        filters: OpportunitySearchFilters = OpportunitySearchFilters(),
    ): List<String> = OpportunitySearch.search(
        opportunities = opportunities,
        filters = filters,
        now = now,
    ).map(Opportunity::id)

    private fun opportunity(
        id: String,
        title: String = "Program $id",
        description: String = "A hands-on STEM program.",
        category: String = "STEM",
        categories: List<String> = listOf(category),
        city: String = "Toronto",
        region: String = "Toronto",
        ageMin: Int = 8,
        ageMax: Int? = 18,
        languages: List<String> = listOf("en"),
        cost: String = "Free to join",
        status: String = "active",
        startDate: String? = null,
        endDate: String? = "2027-12-31",
        deadline: String? = null,
        tags: List<String> = emptyList(),
        communityFocus: List<String> = emptyList(),
        volunteerHoursEligible: Boolean = false,
        coopEligible: Boolean = false,
        translations: Map<String, OpportunityTranslation> = emptyMap(),
    ) = Opportunity(
        id = id,
        title = title,
        organization = "Community Library",
        description = description,
        category = category,
        categories = categories,
        city = city,
        region = region,
        startDate = startDate,
        endDate = endDate,
        deadline = deadline,
        ageMin = ageMin,
        ageMax = ageMax,
        languages = languages,
        cost = cost,
        sourceUrl = "https://example.org/$id",
        status = status,
        communityFocus = communityFocus,
        volunteerHoursEligible = volunteerHoursEligible,
        coopEligible = coopEligible,
        tags = tags,
        translations = translations,
    )
}
