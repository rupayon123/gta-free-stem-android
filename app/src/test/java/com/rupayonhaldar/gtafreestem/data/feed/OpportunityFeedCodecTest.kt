package com.rupayonhaldar.gtafreestem.data.feed

import java.io.File
import java.time.Instant
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
    fun `bundled production feed satisfies the strict boundary`() {
        val bundledFeed = sequenceOf(
            File("app/src/main/res/raw/opportunities.json"),
            File("src/main/res/raw/opportunities.json"),
        ).firstOrNull(File::isFile)
        assertTrue("Bundled feed fixture is missing", bundledFeed != null)

        val feed = codec.decodeAndValidate(
            requireNotNull(bundledFeed).readText(),
            now,
            requireFreshness = false,
            requireSourceHealth = true,
        )

        assertEquals(125, feed.declaredRecordCount)
        assertEquals(125, feed.opportunities.size)
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
          "opportunities": [${opportunityJson("healthy", "Free")}]
        }
    """.trimIndent()

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
}
