package com.rupayonhaldar.gtafreestem.data.feed

import java.time.Instant
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.*
import org.junit.Test

class OpportunityContentRightsPolicyTest {
    private val codec = OpportunityFeedCodec()
    private val now = Instant.parse("2026-10-26T13:10:37.523Z")

    @Test fun `live owner confirmation preserves full original description`() {
        val result = live(base())
        assertEquals("owned", result.opportunities.single().id)
        assertEquals("Build a device, test its circuit, and take it home.", result.opportunities.single().description)
    }

    @Test fun `strict legacy terms remain supported with exact reviewed evidence`() {
        val legacy = sourceChange { source ->
            source.remove("ownerPermission")
            source["status"] = JsonPrimitive("terms-permitted")
            source["basisUrl"] = JsonPrimitive("https://markham.bibliocommons.com/info/terms/")
            source["termsVerifiedAt"] = JsonPrimitive("2026-10-26")
            source["termsVersion"] = JsonPrimitive("2018-09-19")
            source["termsDigestVersion"] = JsonPrimitive("bibliocommons-relevant-sections-v1")
            source["termsSectionDigest"] = JsonPrimitive("sha256:451e63466c801166653f47336e236d05f9a50a3df45a917fa603e92bcd11c8a0")
            source["conditions"] = JsonArray(listOf(
                "Automated access uses the provider's RSS/XML event feed.",
                "Every displayed extract links to its original BiblioCommons event page.",
                "GTA FREE STEM is free and does not monetize provider content.",
            ).map(::JsonPrimitive))
        }
        assertEquals(1, live(legacy).opportunities.size)
        rejects(legacy.replace("sha256:451e63466c801166653f47336e236d05f9a50a3df45a917fa603e92bcd11c8a0", "sha256:" + "0".repeat(64)))
        rejects(legacy.replace("2018-09-19", "unreviewed"))
    }

    @Test fun `absent rights migrate only from unversioned trusted local snapshots`() {
        val unversioned = JsonObject(Json.parseToJsonElement(base()).jsonObject.toMutableMap().apply {
            remove("contentRights"); remove("publicationRevision")
        }).toString()
        assertEquals(1, codec.decodeAndValidate(unversioned, now, false).opportunities.size)
        rejects(unversioned)
        val versioned = JsonObject(Json.parseToJsonElement(base()).jsonObject.toMutableMap().apply { remove("contentRights") }).toString()
        assertThrows(InvalidOpportunityFeedException::class.java) { codec.decodeAndValidate(versioned, now, false) }
    }

    @Test fun `owner record date scope and identity are pinned`() {
        for ((key, value) in listOf(
            "confirmationID" to JsonPrimitive("self-approved"),
            "confirmedAt" to JsonPrimitive("2026-10-26"),
            "scope" to JsonArray(listOf(JsonPrimitive("display-event-listings"))),
        )) rejects(sourceChange { source ->
            source["ownerPermission"] = JsonObject(source.getValue("ownerPermission").jsonObject.toMutableMap().apply { put(key, value) })
        })
    }

    @Test fun `neither unknown providers nor first party host claims authorize extracts`() {
        rejects(sourceChange { it["organization"] = JsonPrimitive("Unreviewed Library") })
        rejects(sourceChange { it["sourceHost"] = JsonPrimitive("gta-free-stem.pages.dev") })
        rejects(sourceChange { it["status"] = JsonPrimitive("approved-by-publisher") })
    }

    @Test fun `all original URLs retain exact source attribution`() {
        val urls = listOf("http://markham.bibliocommons.com/events/owned", "https://user@markham.bibliocommons.com/events/owned",
            "https://markham.bibliocommons.com:444/events/owned", "https://attacker.example/events/owned", "https://markham.bibliocommons.com/info/about")
        for (url in urls) rejects(rowChange { row ->
            row["sourceUrl"] = JsonPrimitive(url); row["registrationUrl"] = JsonPrimitive(url)
            row["sources"] = JsonArray(listOf(JsonObject(mapOf("url" to JsonPrimitive(url)))))
        })
        rejects(rowChange { it["registrationUrl"] = JsonPrimitive("https://attacker.example/events/owned") })
        rejects(rowChange { it["sources"] = JsonArray(emptyList()) })
    }

    @Test fun `provider declarations must exactly match record counts`() {
        rejects(sourceChange { it["records"] = JsonPrimitive(2) })
        rejects(rightsChange { rights ->
            val source = rights.getValue("sources").jsonArray.single()
            rights["sources"] = JsonArray(listOf(source, source))
        })
    }

    @Test fun `restricted identities are explicit unique and excluded`() {
        rejects(rightsChange { it.remove("restrictedOpportunityIds") })
        for (ids in listOf(listOf("owned"), listOf("excluded", "excluded"), listOf(" "))) {
            rejects(rightsChange { it["restrictedOpportunityIds"] = JsonArray(ids.map(::JsonPrimitive)) })
        }
    }

    @Test fun `null malformed and future evidence cannot enter live results`() {
        rejects(JsonObject(Json.parseToJsonElement(base()).jsonObject.toMutableMap().apply { put("contentRights", JsonNull) }).toString())
        rejects(rightsChange { it["termsVerifiedAt"] = JsonPrimitive("not-a-date") })
        val future = rightsChange { it["termsVerifiedAt"] = JsonPrimitive("2026-10-27") }
        rejects(future)
        assertThrows(InvalidOpportunityFeedException::class.java) { codec.decodeAndValidate(future, now, false) }
        rejects(rightsChange { it["termsVerifiedAt"] = JsonPrimitive("2026-10-25") })
    }

    @Test fun `offline evidence age does not expire the owner permission grant`() {
        val later = Instant.parse("2026-11-20T00:00:00Z")
        val result = codec.decodeAndValidate(base(), later, requireFreshness = false)
        assertEquals(1, result.opportunities.size)
        assertTrue(result.isStale)
        assertThrows(InvalidOpportunityFeedException::class.java) { codec.decodeAndValidate(base(), later, true, true) }
    }

    private fun live(raw: String) = codec.decodeAndValidate(raw, now, true, true)
    private fun rejects(raw: String) { assertThrows(InvalidOpportunityFeedException::class.java) { live(raw) } }
    private fun rightsChange(change: (MutableMap<String, JsonElement>) -> Unit): String {
        val root = Json.parseToJsonElement(base()).jsonObject.toMutableMap()
        val rights = root.getValue("contentRights").jsonObject.toMutableMap(); change(rights)
        root["contentRights"] = JsonObject(rights); return JsonObject(root).toString()
    }
    private fun sourceChange(change: (MutableMap<String, JsonElement>) -> Unit): String = rightsChange { rights ->
        val source = rights.getValue("sources").jsonArray.single().jsonObject.toMutableMap(); change(source)
        rights["sources"] = JsonArray(listOf(JsonObject(source)))
    }
    private fun rowChange(change: (MutableMap<String, JsonElement>) -> Unit): String {
        val root = Json.parseToJsonElement(base()).jsonObject.toMutableMap()
        val row = root.getValue("opportunities").jsonArray.single().jsonObject.toMutableMap(); change(row)
        root["opportunities"] = JsonArray(listOf(JsonObject(row))); return JsonObject(root).toString()
    }
    private fun base() = """
        {"count":1,"lastDataChange":"2026-10-26","publicationRevision":"2026-10-26T13:09:37.523Z",
         "sourceHealth":{"library":{"status":"healthy","attemptedPages":1,"successfulPages":1,"pageSuccessRatio":1.0,"minimumPageSuccessRatio":0.75,"acceptedListings":1,"minimumAcceptedListings":1},"discovery":{"status":"healthy","sourcesChecked":1,"successfulSources":1,"sourceSuccessRatio":1.0,"minimumSourceSuccessRatio":0.75}},
         "contentRights":{"termsVerifiedAt":"2026-10-26","restrictedOpportunityIds":["excluded"],"sources":[{"organization":"Markham Public Library","sourceHost":"markham.bibliocommons.com","records":1,"status":"owner-confirmed-permission","ownerPermission":{"confirmationID":"gta-library-content-2026-10-25","confirmedAt":"2026-10-25","scope":["display-event-listings","translate-event-listings"]}}]},
         "opportunities":[{"id":"owned","title":"Build a circuit","organization":"Markham Public Library","description":"Build a device, test its circuit, and take it home.","category":"STEM","city":"Markham","region":"York","ageMin":10,"cost":"Free","status":"active","sourceUrl":"https://markham.bibliocommons.com/events/owned","registrationUrl":"https://markham.bibliocommons.com/events/owned","sources":[{"label":"Official event page","url":"https://markham.bibliocommons.com/events/owned","capturedAt":"2026-10-26T13:00:00Z","confidence":"high"}]}]}
    """.trimIndent()
}
