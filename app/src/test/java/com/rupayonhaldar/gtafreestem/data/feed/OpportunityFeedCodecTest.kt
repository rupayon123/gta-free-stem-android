package com.rupayonhaldar.gtafreestem.data.feed

import java.io.File
import java.security.MessageDigest
import java.time.Instant
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class OpportunityFeedCodecTest {
    private val codec = OpportunityFeedCodec()
    private val now = Instant.parse("2026-08-16T00:00:00Z")

    @Test
    fun `decodes opportunities envelope and ignores unknown keys`() {
        val feed = codec.decodeAndValidate(
            """
            {
              "count": 1,
              "lastDataChange": "2026-08-06",
              "futureEnvelopeField": {"safe": true},
              "opportunities": [{
                "id": "robotics",
                "title": "Robotics Club",
                "provider": "Public Library",
                "summary": "Build robots.",
                "categories": ["Coding & Robotics"],
                "city": "Toronto",
                "ages": {"min": 10, "max": 14},
                "languages": ["en", "fr"],
                "cost": "Free to join",
                "registrationUrl": "https://example.org/register",
                "status": "active",
                "distanceKm": 3.25,
                "isNewFind": true,
                "sourceConfidence": "high",
                "unknownListingField": 42
              }]
            }
            """.trimIndent(),
            now,
            requireFreshness = true,
        )

        val opportunity = feed.opportunities.single()
        assertEquals("Public Library", opportunity.organization)
        assertEquals("Build robots.", opportunity.description)
        assertEquals("Coding & Robotics", opportunity.category)
        assertEquals(10, opportunity.ageMin)
        assertEquals(14, opportunity.ageMax)
        assertEquals(listOf("en", "fr"), opportunity.languages)
        assertEquals("https://example.org/register", opportunity.sourceUrl)
        assertEquals(3.25, opportunity.distanceKm ?: error("missing distance"), 0.0)
        assertEquals(true, opportunity.isNewFind)
        assertEquals("high", opportunity.sourceConfidence)
    }

    @Test
    fun `decodes data envelope with metadata freshness`() {
        val feed = codec.decodeAndValidate(
            """
            {
              "data": [${opportunityJson("one", "No cost")}],
              "meta": {"activeCount": 1, "lastUpdated": "2026-08-15T12:00:00Z"}
            }
            """.trimIndent(),
            now,
            requireFreshness = true,
        )

        assertEquals("one", feed.opportunities.single().id)
        assertEquals(Instant.parse("2026-08-15T12:00:00Z"), feed.lastUpdated)
    }

    @Test
    fun `rss excerpts capped by the publisher end cleanly without changing other copy`() {
        val cutMidWord = "A".repeat(248) + " fraud detec"
        val completedSentence = "B".repeat(259) + "."
        val quotedCompletedSentence = "D".repeat(258) + ".”"
        val noWordBoundary = "E".repeat(260)
        val outerWhitespace = " " + "F".repeat(258) + " "
        val trailingWordBoundary = "I".repeat(250) + " complete "
        val punctuationOnlyPrefix = "- " + "G".repeat(258)
        val astralFinalCodePoint = "H".repeat(258) + "😀"
        val nonRssCopy = "C".repeat(248) + " fraud detec"
        assertEquals(260, cutMidWord.length)
        assertEquals(260, completedSentence.length)
        assertEquals(260, quotedCompletedSentence.length)
        assertEquals(260, noWordBoundary.length)
        assertEquals(260, outerWhitespace.length)
        assertEquals(260, trailingWordBoundary.length)
        assertEquals(260, punctuationOnlyPrefix.length)
        assertEquals(260, astralFinalCodePoint.length)
        assertEquals(260, nonRssCopy.length)

        val feed = codec.decodeAndValidate(
            """
            {
              "count": 9,
              "lastDataChange": "2026-08-15",
              "opportunities": [
                ${opportunityJson("tpl-rss-cut", "Free").replace("Description", cutMidWord)},
                ${opportunityJson("tpl-rss-complete", "Free").replace("Description", completedSentence)},
                ${opportunityJson("tpl-rss-quoted", "Free").replace("Description", quotedCompletedSentence)},
                ${opportunityJson("tpl-rss-token", "Free").replace("Description", noWordBoundary)},
                ${opportunityJson("tpl-rss-whitespace", "Free").replace("Description", outerWhitespace)},
                ${opportunityJson("tpl-rss-boundary", "Free").replace("Description", trailingWordBoundary)},
                ${opportunityJson("tpl-rss-punctuation", "Free").replace("Description", punctuationOnlyPrefix)},
                ${opportunityJson("tpl-rss-astral", "Free").replace("Description", astralFinalCodePoint)},
                ${opportunityJson("curated-copy", "Free").replace("Description", nonRssCopy)}
              ]
            }
            """.trimIndent(),
            now,
            requireFreshness = true,
        )

        assertEquals("A".repeat(248) + " fraud…", feed.opportunities[0].description)
        assertEquals(completedSentence, feed.opportunities[1].description)
        assertEquals(quotedCompletedSentence, feed.opportunities[2].description)
        assertEquals("E".repeat(259) + "…", feed.opportunities[3].description)
        assertEquals("F".repeat(258), feed.opportunities[4].description)
        assertEquals("I".repeat(250) + " complete…", feed.opportunities[5].description)
        assertEquals("- " + "G".repeat(257) + "…", feed.opportunities[6].description)
        assertEquals("H".repeat(258) + "…", feed.opportunities[7].description)
        assertEquals(nonRssCopy, feed.opportunities[8].description)
    }

    @Test
    fun `bundled production feed satisfies the strict boundary`() {
        val bundledFeed = sequenceOf(
            File("app/src/main/res/raw/opportunities.json"),
            File("src/main/res/raw/opportunities.json"),
        ).firstOrNull(File::isFile)
        assertTrue("Bundled feed fixture is missing", bundledFeed != null)

        val raw = requireNotNull(bundledFeed).readText()
        val feed = codec.decodeAndValidate(
            raw,
            Instant.parse("2026-10-27T12:18:29.362Z"),
            requireFreshness = true,
            requireSourceHealth = true,
        )
        assertEquals(1878, feed.declaredRecordCount)
        assertEquals(1878, feed.opportunities.size)
        assertEquals(Instant.parse("2026-10-27T00:00:00Z"), feed.lastUpdated)
        assertEquals(Instant.parse("2026-10-27T12:17:29.362Z"), feed.publicationRevision)
        assertTrue(feed.opportunities.none { it.isNewFind == true })
        assertTrue(feed.opportunities.all { it.translations.isEmpty() })
        val exact = feed.opportunities.single { it.id == "tpl-rss-6ac92852295b3b0033cb1160" }
        val original = Json.parseToJsonElement(raw).jsonObject.getValue("opportunities").jsonArray
            .single { it.jsonObject.getValue("id").jsonPrimitive.content == exact.id }
            .jsonObject.getValue("description").jsonPrimitive.content
        assertEquals(original, exact.description)
        assertTrue(exact.description.contains("volunteer hours"))
        assertEquals(
            setOf("tpl.bibliocommons.com", "markham.bibliocommons.com"),
            feed.opportunities.map { java.net.URI(it.sourceUrl).host }.toSet(),
        )
        assertEquals(
            "cc1363b02347700bb6be7de4407a0bc8e8baf9d8fbcfa578750658d2f5e3a9ba",
            requireNotNull(bundledFeed).sha256(),
        )
    }

    @Test
    fun `checks declared count before removing paid listings`() {
        val valid = """
            {
              "count": 2,
              "lastDataChange": "2026-08-15",
              "opportunities": [
                ${opportunityJson("free", "Free")},
                ${opportunityJson("paid", "${'$'}25 per workshop")}
              ]
            }
        """.trimIndent()

        assertEquals(
            listOf("free"),
            codec.decodeAndValidate(valid, now, true).opportunities.map { it.id },
        )

        val mismatch = valid.replace("\"count\": 2", "\"count\": 1")
        assertReason(InvalidOpportunityFeedReason.COUNT_MISMATCH, mismatch)
    }

    @Test
    fun `rejects empty payload`() {
        assertReason(
            InvalidOpportunityFeedReason.EMPTY_PAYLOAD,
            """{"count":0,"lastDataChange":"2026-08-15","opportunities":[]}""",
        )
    }

    @Test
    fun `drops invalid identifiers and rejects duplicate valid identifiers`() {
        assertReason(
            InvalidOpportunityFeedReason.NO_VALID_OPPORTUNITIES,
            """{"count":1,"lastDataChange":"2026-08-15","opportunities":[${opportunityJson("   ", "Free")}] }""",
        )
        assertReason(
            InvalidOpportunityFeedReason.DUPLICATE_ID,
            """
            {
              "count": 2,
              "lastDataChange": "2026-08-15",
              "opportunities": [
                ${opportunityJson("same", "Free")},
                ${opportunityJson("same", "${'$'}30")}
              ]
            }
            """.trimIndent(),
        )
    }

    @Test
    fun `rejects payloads beyond the bounded record count`() {
        val listings = List(5_001) { opportunityJson("duplicate", "Free") }.joinToString(",")
        assertReason(
            InvalidOpportunityFeedReason.TOO_MANY_RECORDS,
            """{"count":5001,"lastDataChange":"2026-08-15","opportunities":[$listings]}""",
        )
    }

    @Test
    fun `drops unsafe listings before domain mapping`() {
        val longId = "i".repeat(257)
        val longTitle = "t".repeat(301)
        val longProvider = "p".repeat(301)
        val longDescription = "d".repeat(20_001)
        val longUrl = "https://example.org/" + "u".repeat(4_100)
        val unsafeListings = listOf(
            opportunityJson("blank-title", "Free")
                .replace("Title blank-title", "   "),
            opportunityJson("blank-provider", "Free")
                .replace("\"organization\":\"Provider\"", "\"organization\":\"   \""),
            opportunityJson("blank-description", "Free")
                .replace("\"description\":\"Description\"", "\"description\":\"   \""),
            opportunityJson("blank-category", "Free")
                .replace("\"category\":\"STEM\"", "\"category\":\"   \""),
            opportunityJson("missing-status", "Free")
                .replace(",\"status\":\"active\"", ""),
            opportunityJson("inactive", "Free")
                .replace("\"status\":\"active\"", "\"status\":\"inactive\""),
            opportunityJson("negative-age", "Free")
                .replace("\"ageMin\":10", "\"ageMin\":-1"),
            opportunityJson("reversed-age", "Free")
                .replace("\"ageMin\":10", "\"ageMin\":14,\"ageMax\":10"),
            opportunityJson(longId, "Free"),
            opportunityJson("long-title", "Free")
                .replace("Title long-title", longTitle),
            opportunityJson("long-provider", "Free")
                .replace("\"organization\":\"Provider\"", "\"organization\":\"$longProvider\""),
            opportunityJson("long-description", "Free")
                .replace("\"description\":\"Description\"", "\"description\":\"$longDescription\""),
            opportunityJson("http-source", "Free")
                .replace("https://example.org/http-source", "http://example.org/http-source"),
            opportunityJson("unsafe-registration", "Free")
                .replace(
                    ",\"status\":\"active\"",
                    ",\"registrationUrl\":\"javascript:alert(1)\",\"status\":\"active\"",
                ),
            opportunityJson("long-registration", "Free")
                .replace(
                    ",\"status\":\"active\"",
                    ",\"registrationUrl\":\"$longUrl\",\"status\":\"active\"",
                ),
        )
        val feed = codec.decodeAndValidate(
            """
            {
              "count": ${unsafeListings.size + 1},
              "lastDataChange": "2026-08-15",
              "opportunities": [
                ${opportunityJson("safe", "Free")},
                ${unsafeListings.joinToString(",")}
              ]
            }
            """.trimIndent(),
            now,
            requireFreshness = true,
        )

        assertEquals(listOf("safe"), feed.opportunities.map { it.id })
    }

    @Test
    fun `drops malformed schedule fields and end dates before their starts`() {
        val malformedSchedules = listOf(
            opportunityJson("bad-start", "Free").replace(
                ",\"status\":\"active\"",
                ",\"startDate\":\"not-a-date\",\"status\":\"active\"",
            ),
            opportunityJson("bad-end", "Free").replace(
                ",\"status\":\"active\"",
                ",\"endDate\":\"2026-02-30\",\"status\":\"active\"",
            ),
            opportunityJson("bad-deadline", "Free").replace(
                ",\"status\":\"active\"",
                ",\"deadline\":\"tomorrow\",\"status\":\"active\"",
            ),
            opportunityJson("backwards", "Free").replace(
                ",\"status\":\"active\"",
                ",\"startDate\":\"2026-10-01\",\"endDate\":\"2026-09-30\",\"status\":\"active\"",
            ),
        )
        val validSchedule = opportunityJson("valid-schedule", "Free").replace(
            ",\"status\":\"active\"",
            ",\"startDate\":\"2026-09-01\",\"endDate\":\"2026-10-01T20:00:00Z\",\"deadline\":\"2026-08-31T23:59:00-04:00\",\"status\":\"active\"",
        )

        val feed = codec.decodeAndValidate(
            """
            {
              "count": ${malformedSchedules.size + 1},
              "lastDataChange": "2026-08-15",
              "opportunities": [
                $validSchedule,
                ${malformedSchedules.joinToString(",")}
              ]
            }
            """.trimIndent(),
            now,
            requireFreshness = true,
        )

        assertEquals(listOf("valid-schedule"), feed.opportunities.map { it.id })
    }

    @Test
    fun `accepts an end timestamp later on a date-only start day`() {
        val dateOnlyStartWithSameDayEnd = opportunityJson("same-day-schedule", "Free").replace(
            ",\"status\":\"active\"",
            ",\"startDate\":\"2026-09-01\",\"endDate\":\"2026-09-01T20:00:00Z\",\"status\":\"active\"",
        )

        val feed = codec.decodeAndValidate(
            """
            {
              "count": 2,
              "lastDataChange": "2026-08-15",
              "opportunities": [
                ${opportunityJson("control", "Free")},
                $dateOnlyStartWithSameDayEnd
              ]
            }
            """.trimIndent(),
            now,
            requireFreshness = true,
        )

        assertEquals(
            listOf("control", "same-day-schedule"),
            feed.opportunities.map { it.id },
        )
    }

    @Test
    fun `drops oversized translation list and text fields before domain mapping`() {
        val excessiveTags = List(101) { index -> "\"tag-$index\"" }.joinToString(",")
        val excessiveTranslations = (0..32).joinToString(",") { index ->
            "\"x$index\":{\"title\":\"Translation $index\"}"
        }
        val oversizedTranslationTitle = "t".repeat(301)
        val oversizedCity = "c".repeat(301)
        val oversizedConfidence = "c".repeat(101)
        val unsafe = listOf(
            opportunityJson("too-many-tags", "Free").replace(
                ",\"status\":\"active\"",
                ",\"tags\":[$excessiveTags],\"status\":\"active\"",
            ),
            opportunityJson("too-many-translations", "Free").replace(
                ",\"status\":\"active\"",
                ",\"translations\":{$excessiveTranslations},\"status\":\"active\"",
            ),
            opportunityJson("long-translation", "Free").replace(
                ",\"status\":\"active\"",
                ",\"translations\":{\"es\":{\"title\":\"$oversizedTranslationTitle\"}},\"status\":\"active\"",
            ),
            opportunityJson("long-city", "Free").replace(
                "\"city\":\"Toronto\"",
                "\"city\":\"$oversizedCity\"",
            ),
            opportunityJson("long-confidence", "Free").replace(
                ",\"status\":\"active\"",
                ",\"sourceConfidence\":\"$oversizedConfidence\",\"status\":\"active\"",
            ),
            opportunityJson("negative-distance", "Free").replace(
                ",\"status\":\"active\"",
                ",\"distanceKm\":-1,\"status\":\"active\"",
            ),
        )

        val feed = codec.decodeAndValidate(
            """
            {
              "count": ${unsafe.size + 1},
              "lastDataChange": "2026-08-15",
              "opportunities": [
                ${opportunityJson("safe-bounds", "Free")},
                ${unsafe.joinToString(",")}
              ]
            }
            """.trimIndent(),
            now,
            requireFreshness = true,
        )

        assertEquals(listOf("safe-bounds"), feed.opportunities.map { it.id })
    }

    @Test
    fun `rejects malformed translation and list JSON types`() {
        val translationArray = opportunityJson("bad-translation", "Free").replace(
            ",\"status\":\"active\"",
            ",\"translations\":[],\"status\":\"active\"",
        )
        assertReason(
            InvalidOpportunityFeedReason.MALFORMED_JSON,
            """{"count":1,"lastDataChange":"2026-08-15","opportunities":[$translationArray]}""",
        )

        val stringTags = opportunityJson("bad-tags", "Free").replace(
            ",\"status\":\"active\"",
            ",\"tags\":\"robotics\",\"status\":\"active\"",
        )
        assertReason(
            InvalidOpportunityFeedReason.MALFORMED_JSON,
            """{"count":1,"lastDataChange":"2026-08-15","opportunities":[$stringTags]}""",
        )
    }

    @Test
    fun `filters malformed ambiguous and paid costs without failing good records`() {
        val feed = codec.decodeAndValidate(
            """
            {
              "count": 4,
              "lastDataChange": "2026-08-15",
              "opportunities": [
                ${opportunityJson("verified", "Free to join")},
                ${opportunityJson("paid", "${'$'}25 per workshop")},
                ${opportunityJson("ambiguous", "Free to join; ${'$'}15 materials fee")},
                {"id":"malformed","title":"Malformed","cost":{"amount":0}}
              ]
            }
            """.trimIndent(),
            now,
            requireFreshness = true,
        )

        assertEquals(listOf("verified"), feed.opportunities.map { it.id })
    }

    @Test
    fun `accepts internally consistent healthy source metrics`() {
        val feed = codec.decodeAndValidate(
            feedWithSourceHealth(status = "healthy", ratio = 1.0),
            now,
            requireFreshness = true,
            requireSourceHealth = true,
        )
        assertEquals(1, feed.opportunities.size)
    }

    @Test
    fun `required source health rejects missing and incomplete evidence`() {
        val withoutHealth = """
            {"count":1,"lastDataChange":"2026-08-15","opportunities":[${opportunityJson("one", "Free")}]}
        """.trimIndent()
        assertReason(
            InvalidOpportunityFeedReason.MISSING_SOURCE_HEALTH,
            withoutHealth,
            requireSourceHealth = true,
        )
        assertReason(
            InvalidOpportunityFeedReason.UNHEALTHY_SOURCE_METRICS,
            feedWithRawHealth("{}"),
            requireSourceHealth = true,
        )
        assertReason(
            InvalidOpportunityFeedReason.UNHEALTHY_SOURCE_METRICS,
            feedWithRawHealth(healthyLibraryHealth()),
            requireSourceHealth = true,
        )
    }

    @Test
    fun `rejects unhealthy or inconsistent source metrics when declared`() {
        assertReason(
            InvalidOpportunityFeedReason.UNHEALTHY_SOURCE_METRICS,
            feedWithSourceHealth(status = "unhealthy", ratio = 1.0),
        )
        assertReason(
            InvalidOpportunityFeedReason.UNHEALTHY_SOURCE_METRICS,
            feedWithSourceHealth(status = "healthy", ratio = 0.25),
        )
    }

    @Test
    fun `local fallback can remain usable after remote freshness window`() {
        val stale = """
            {"count":1,"lastDataChange":"2026-07-01","opportunities":[${opportunityJson("offline", "Free")}]}
        """.trimIndent()

        assertReason(InvalidOpportunityFeedReason.STALE_OR_FUTURE_FEED, stale)
        assertTrue(codec.decodeAndValidate(stale, now, requireFreshness = false).isStale)
    }

    @Test
    fun `publication revision is retained while absent legacy metadata remains readable`() {
        val legacy = feedWithSourceHealth("healthy", 1.0)
        val revised = legacy.replaceFirst("{", "{\n \"publicationRevision\": \"2026-08-15T20:55:11.491Z\",")
        assertEquals(null, codec.decodeAndValidate(legacy, now, true).publicationRevision)
        assertEquals(Instant.parse("2026-08-15T20:55:11.491Z"), codec.decodeAndValidate(revised, now, true).publicationRevision)
    }

    @Test
    fun `malformed null nonstring and future revisions cannot enter the cache`() {
        val legacy = feedWithSourceHealth("healthy", 1.0)
        for (revision in listOf("null", "17", "true", "{}", "[]", "\"\"", "\"2026-08-15\"", "\"2026-08-16T00:05:00.001Z\"")) {
            val raw = legacy.replaceFirst("{", "{\n \"publicationRevision\": $revision,")
            assertThrows("Revision $revision must be rejected", InvalidOpportunityFeedException::class.java) {
                codec.decodeAndValidate(raw, now, true)
            }
        }
    }

    @Test
    fun `recent publication never renews stale event data freshness`() {
        val raw = feedWithSourceHealth("healthy", 1.0)
            .replace("2026-08-15", "2026-07-01")
            .replaceFirst("{", "{\n \"publicationRevision\": \"2026-08-16T00:00:00.000Z\",")
        assertReason(InvalidOpportunityFeedReason.STALE_OR_FUTURE_FEED, raw)
    }

    private fun assertReason(
        reason: InvalidOpportunityFeedReason,
        json: String,
        requireSourceHealth: Boolean = false,
    ) {
        val error = assertThrows(InvalidOpportunityFeedException::class.java) {
            codec.decodeAndValidate(
                json,
                now,
                requireFreshness = true,
                requireSourceHealth = requireSourceHealth,
            )
        }
        assertEquals(reason, error.reason)
    }

    private fun opportunityJson(id: String, cost: String): String =
        """{"id":"$id","title":"Title $id","organization":"Provider","description":"Description","category":"STEM","city":"Toronto","region":"Toronto","ageMin":10,"language":["en"],"cost":"$cost","sourceUrl":"https://example.org/$id","status":"active"}"""

    private fun feedWithSourceHealth(status: String, ratio: Double): String = feedWithRawHealth(
        """
        {
          "library": ${healthyLibraryHealth(status, ratio)},
          "discovery": {
            "status": "healthy",
            "sourcesChecked": 4,
            "successfulSources": 4,
            "sourceSuccessRatio": 1.0,
            "minimumSourceSuccessRatio": 0.75
          }
        }
        """.trimIndent(),
    )

    private fun feedWithRawHealth(healthJson: String): String = """
        {
          "count": 1,
          "lastDataChange": "2026-08-15",
          "sourceHealth": $healthJson,
          "contentRights": {"termsVerifiedAt": "2026-08-15", "restrictedOpportunityIds": [], "sources": [{"organization": "Markham Public Library", "sourceHost": "markham.bibliocommons.com", "records": 1, "status": "terms-permitted", "basisUrl": "https://markham.bibliocommons.com/info/terms/", "conditions": ["Automated access uses the provider's RSS/XML event feed.", "Every displayed extract links to its original BiblioCommons event page.", "GTA FREE STEM is free and does not monetize provider content."], "termsVerifiedAt": "2026-08-15", "termsVersion": "2018-09-19", "termsDigestVersion": "bibliocommons-relevant-sections-v1", "termsSectionDigest": "sha256:451e63466c801166653f47336e236d05f9a50a3df45a917fa603e92bcd11c8a0"}]},
          "opportunities": [${approvedOpportunityJson("healthy")}]
        }
    """.trimIndent()

    private fun approvedOpportunityJson(id: String): String =
        opportunityJson(id, "Free")
            .replace("\"organization\":\"Provider\"", "\"organization\":\"Markham Public Library\"")
            .replace("https://example.org/$id", "https://markham.bibliocommons.com/events/$id")
            .replace("\"status\":\"active\"", "\"registrationUrl\":\"https://markham.bibliocommons.com/events/$id\",\"sources\":[{\"label\":\"Official event page\",\"url\":\"https://markham.bibliocommons.com/events/$id\",\"capturedAt\":\"2026-08-15T12:00:00Z\",\"confidence\":\"high\"}],\"status\":\"active\"")

    private fun healthyLibraryHealth(
        status: String = "healthy",
        ratio: Double = 1.0,
    ): String = """
        {
          "status": "$status",
          "attemptedPages": 4,
          "successfulPages": 4,
          "pageSuccessRatio": $ratio,
          "minimumPageSuccessRatio": 0.75,
          "acceptedListings": 1,
          "minimumAcceptedListings": 1
        }
    """.trimIndent()

    private fun File.sha256(): String = MessageDigest.getInstance("SHA-256")
        .digest(readBytes())
        .joinToString("") { byte -> "%02x".format(byte) }
}
