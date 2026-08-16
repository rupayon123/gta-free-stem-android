package com.rupayonhaldar.gtafreestem.data.repository

import com.rupayonhaldar.gtafreestem.data.feed.OpportunityFeedCodec
import com.rupayonhaldar.gtafreestem.data.local.BundledOpportunityFeedSource
import com.rupayonhaldar.gtafreestem.data.local.OpportunityFeedCache
import com.rupayonhaldar.gtafreestem.data.network.OpportunityFeedNetwork
import com.rupayonhaldar.gtafreestem.data.network.RemoteFeedEndpoint
import com.rupayonhaldar.gtafreestem.domain.model.OpportunityFeedSource
import java.net.URL
import java.time.Instant
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class DefaultOpportunityRepositoryTest {
    private val now = Instant.parse("2026-08-16T00:00:00Z")

    @Test
    fun `bootstrap selects the newest validated local snapshot`() = runBlocking {
        val cache = FakeCache(feed("cached", "2026-08-01"))
        val repository = repository(
            network = FakeNetwork(emptyList(), emptyMap()),
            cache = cache,
            bundledJson = feed("bundled", "2026-08-06"),
        )

        val snapshot = repository.bootstrap()

        assertEquals(OpportunityFeedSource.BUNDLED, snapshot.source)
        assertEquals("bundled", snapshot.opportunities.single().id)
    }

    @Test
    fun `refresh validates primary then uses and persists fallback`() = runBlocking {
        val primary = RemoteFeedEndpoint(URL("https://primary.example/feed.json"), OpportunityFeedSource.PRIMARY_NETWORK)
        val fallback = RemoteFeedEndpoint(URL("https://fallback.example/feed.json"), OpportunityFeedSource.FALLBACK_NETWORK)
        val validFallback = feed("fallback", "2026-08-15")
        val cache = FakeCache(null)
        val network = FakeNetwork(
            endpoints = listOf(primary, fallback),
            responses = mapOf(
                primary.url to """{"count":2,"lastDataChange":"2026-08-15","opportunities":[]} """,
                fallback.url to validFallback,
            ),
        )
        val repository = repository(network, cache, feed("bundled", "2026-08-06"))

        val snapshot = repository.refresh()

        assertEquals(OpportunityFeedSource.FALLBACK_NETWORK, snapshot.source)
        assertEquals("fallback", snapshot.opportunities.single().id)
        assertEquals(listOf(primary.url, fallback.url), network.requests)
        assertEquals(validFallback, cache.value)
        assertFalse(snapshot.isStale)
    }

    @Test
    fun `refresh returns validated cache when every remote endpoint fails`() = runBlocking {
        val primary = RemoteFeedEndpoint(URL("https://primary.example/feed.json"), OpportunityFeedSource.PRIMARY_NETWORK)
        val repository = repository(
            network = FakeNetwork(listOf(primary), emptyMap()),
            cache = FakeCache(feed("cached", "2026-08-05")),
            bundledJson = feed("bundled", "2026-08-01"),
        )

        val snapshot = repository.refresh()

        assertEquals(OpportunityFeedSource.LAST_GOOD_CACHE, snapshot.source)
        assertEquals("cached", snapshot.opportunities.single().id)
    }

    @Test
    fun `refresh never replaces a newer retained snapshot with an older remote feed`() = runBlocking {
        val primary = RemoteFeedEndpoint(
            URL("https://primary.example/feed.json"),
            OpportunityFeedSource.PRIMARY_NETWORK,
        )
        val newerCache = feed("newer-cache", "2026-08-15")
        val cache = FakeCache(newerCache)
        val repository = repository(
            network = FakeNetwork(
                endpoints = listOf(primary),
                responses = mapOf(primary.url to feed("older-remote", "2026-08-14")),
            ),
            cache = cache,
            bundledJson = feed("bundled", "2026-08-06"),
        )
        repository.bootstrap()

        val snapshot = repository.refresh()

        assertEquals(OpportunityFeedSource.LAST_GOOD_CACHE, snapshot.source)
        assertEquals("newer-cache", snapshot.opportunities.single().id)
        assertEquals(newerCache, cache.value)
    }

    private fun repository(
        network: OpportunityFeedNetwork,
        cache: OpportunityFeedCache,
        bundledJson: String,
    ) = DefaultOpportunityRepository(
        codec = OpportunityFeedCodec(),
        network = network,
        cache = cache,
        bundled = object : BundledOpportunityFeedSource {
            override fun read() = bundledJson
        },
        now = { now },
    )

    private fun feed(id: String, date: String): String = """
        {
          "count": 1,
          "lastDataChange": "$date",
          "sourceHealth": {
            "library": {
              "status": "healthy",
              "attemptedPages": 1,
              "successfulPages": 1,
              "pageSuccessRatio": 1.0,
              "minimumPageSuccessRatio": 0.75,
              "acceptedListings": 1,
              "minimumAcceptedListings": 1
            },
            "discovery": {
              "status": "healthy",
              "sourcesChecked": 1,
              "successfulSources": 1,
              "sourceSuccessRatio": 1.0,
              "minimumSourceSuccessRatio": 0.75
            }
          },
          "opportunities": [{
            "id": "$id",
            "title": "Title",
            "organization": "Provider",
            "description": "Description",
            "category": "STEM",
            "city": "Toronto",
            "region": "Toronto",
            "ageMin": 10,
            "cost": "Free to join",
            "status": "active",
            "sourceUrl": "https://example.org/$id"
          }]
        }
    """.trimIndent()

    private class FakeCache(var value: String?) : OpportunityFeedCache {
        override fun read() = value
        override fun write(json: String) {
            value = json
        }
    }

    private class FakeNetwork(
        override val endpoints: List<RemoteFeedEndpoint>,
        private val responses: Map<URL, String>,
    ) : OpportunityFeedNetwork {
        val requests = mutableListOf<URL>()

        override suspend fun fetch(endpoint: RemoteFeedEndpoint): String {
            requests += endpoint.url
            return responses[endpoint.url] ?: error("offline")
        }
    }
}
