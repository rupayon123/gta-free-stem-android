package com.rupayonhaldar.gtafreestem.data.repository

import com.rupayonhaldar.gtafreestem.data.feed.OpportunityFeedCodec
import com.rupayonhaldar.gtafreestem.data.local.BundledOpportunityFeedSource
import com.rupayonhaldar.gtafreestem.data.local.OpportunityFeedCache
import com.rupayonhaldar.gtafreestem.data.network.OpportunityFeedNetwork
import com.rupayonhaldar.gtafreestem.data.network.RemoteFeedEndpoint
import com.rupayonhaldar.gtafreestem.domain.model.OpportunityFeedSource
import com.rupayonhaldar.gtafreestem.localization.AppLanguage
import com.rupayonhaldar.gtafreestem.localization.AppStringCatalog
import java.net.URL
import java.time.Instant
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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

    @Test
    fun `late bootstrap cannot roll back a newer accepted live snapshot`() = runBlocking {
        val primary = RemoteFeedEndpoint(
            URL("https://primary.example/feed.json"),
            OpportunityFeedSource.PRIMARY_NETWORK,
        )
        val cachedReadStarted = CountDownLatch(1)
        val releaseCachedRead = CountDownLatch(1)
        val blockFirstRead = AtomicBoolean(true)
        val cachedBytes = AtomicReference(feed("older-cache", "2026-08-14"))
        val cache = object : OpportunityFeedCache {
            override fun read(): String {
                val captured = cachedBytes.get()
                if (blockFirstRead.compareAndSet(true, false)) {
                    cachedReadStarted.countDown()
                    check(releaseCachedRead.await(5, TimeUnit.SECONDS))
                }
                return captured
            }

            override fun write(json: String) {
                cachedBytes.set(json)
            }
        }
        val newerFeed = feed("newer-live", "2026-08-15")
        val repository = repository(
            FakeNetwork(listOf(primary), mapOf(primary.url to newerFeed)),
            cache,
            feed("bundled", "2026-08-06"),
        )
        val initialLocalRead = async(Dispatchers.IO) { repository.bootstrap() }
        try {
            assertTrue(cachedReadStarted.await(5, TimeUnit.SECONDS))
            assertEquals("newer-live", repository.refresh().opportunities.single().id)
        } finally {
            releaseCachedRead.countDown()
        }

        val restored = withTimeout(5_000) { initialLocalRead.await() }
        assertEquals("newer-live", restored.opportunities.single().id)
        assertEquals("newer-live", repository.current()?.opportunities?.single()?.id)
        assertEquals(newerFeed, cachedBytes.get())
    }

    @Test
    fun `bootstrap keeps newer memory when a valid live response could not be cached`() = runBlocking {
        val primary = RemoteFeedEndpoint(
            URL("https://primary.example/feed.json"),
            OpportunityFeedSource.PRIMARY_NETWORK,
        )
        val olderFeed = feed("older-cache", "2026-08-14")
        val cache = object : OpportunityFeedCache {
            override fun read() = olderFeed
            override fun write(json: String) = error("storage unavailable")
        }
        val repository = repository(
            FakeNetwork(listOf(primary), mapOf(primary.url to feed("newer-live", "2026-08-15"))),
            cache,
            feed("bundled", "2026-08-06"),
        )
        assertEquals("newer-live", repository.refresh().opportunities.single().id)

        val restored = repository.bootstrap()

        assertEquals("newer-live", restored.opportunities.single().id)
        assertEquals("newer-live", repository.current()?.opportunities?.single()?.id)
        assertEquals(olderFeed, cache.read())
    }

    @Test
    fun `invalid refresh candidates preserve accepted memory and persisted data`() = runBlocking {
        val primary = RemoteFeedEndpoint(
            URL("https://primary.example/feed.json"),
            OpportunityFeedSource.PRIMARY_NETWORK,
        )
        val valid = feed("unaccepted", "2026-08-15")
        val invalidCandidates = mapOf(
            "malformed JSON" to valid.dropLast(4),
            "partial count" to valid.replace("\"count\": 1", "\"count\": 2"),
            "empty payload" to """{"count":0,"lastDataChange":"2026-08-15","opportunities":[]}""",
            "stale" to valid.replace("2026-08-15", "2025-08-15"),
            "future" to valid.replace("2026-08-15", "2030-08-15"),
            "missing freshness" to valid.replace("\"lastDataChange\": \"2026-08-15\",", ""),
            "failed pages" to valid.replace("\"successfulPages\": 1", "\"successfulPages\": 0"),
            "zero minimum listings" to valid.replace("\"minimumAcceptedListings\": 1", "\"minimumAcceptedListings\": 0"),
            "zero library threshold" to valid.replace("\"minimumPageSuccessRatio\": 0.75", "\"minimumPageSuccessRatio\": 0"),
            "zero discovery threshold" to valid.replace("\"minimumSourceSuccessRatio\": 0.75", "\"minimumSourceSuccessRatio\": 0"),
            "inconsistent health" to valid.replace("\"pageSuccessRatio\": 1.0", "\"pageSuccessRatio\": 0.1"),
            "malformed schedule" to valid.replace("\"status\": \"active\",", "\"status\": \"active\", \"endDate\": \"next someday\","),
            "paid only" to valid.replace("Free to join", "25 dollars per workshop"),
        )

        invalidCandidates.forEach { (case, candidate) ->
            val accepted = feed("accepted", "2026-08-14")
            val cache = FakeCache(accepted)
            val network = FakeNetwork(listOf(primary), mapOf(primary.url to candidate))
            val repository = repository(network, cache, feed("bundled", "2026-08-06"))
            repository.bootstrap()

            val result = repository.refresh()

            assertEquals(case, "accepted", result.opportunities.single().id)
            assertEquals(case, "accepted", repository.current()?.opportunities?.single()?.id)
            assertEquals(case, accepted, cache.value)
            assertEquals(case, listOf(primary.url), network.requests)
        }
    }

    @Test
    fun `refresh selects newer bytes committed by another cache instance`() = runBlocking {
        val primary = RemoteFeedEndpoint(URL("https://primary.example/feed.json"), OpportunityFeedSource.PRIMARY_NETWORK)
        val newer = feed("other-worker-newer", "2026-08-15")
        val cache = object : OpportunityFeedCache {
            override fun read() = newer
            override fun write(json: String) = error("atomic selection API is required")
            override fun writeAndSelectLatest(json: String, now: Instant) = newer
        }
        val repository = repository(
            FakeNetwork(listOf(primary), mapOf(primary.url to feed("older-live", "2026-08-14"))),
            cache,
            feed("bundled", "2026-08-06"),
        )

        val selected = repository.refresh()

        assertEquals(OpportunityFeedSource.LAST_GOOD_CACHE, selected.source)
        assertEquals("other-worker-newer", selected.opportunities.single().id)
        assertEquals("other-worker-newer", repository.current()?.opportunities?.single()?.id)
        assertEquals(newer, cache.read())
    }

    @Test
    fun `retained data freshness is recalculated after time advances offline`() = runBlocking {
        val primary = RemoteFeedEndpoint(URL("https://primary.example/feed.json"), OpportunityFeedSource.PRIMARY_NETWORK)
        val clock = AtomicReference(now)
        val offline = AtomicBoolean(false)
        val network = object : OpportunityFeedNetwork {
            override val endpoints = listOf(primary)
            override suspend fun fetch(endpoint: RemoteFeedEndpoint): String {
                check(!offline.get()) { "offline" }
                return feed("retained-live", "2026-08-15")
            }
        }
        val repository = repository(
            network,
            FakeCache(null),
            feed("bundled", "2026-08-06"),
            nowProvider = clock::get,
        )
        assertFalse(repository.refresh().isStale)
        clock.set(Instant.parse("2026-09-01T00:00:00Z"))
        offline.set(true)

        val selected = repository.refresh()

        assertEquals("retained-live", selected.opportunities.single().id)
        assertTrue(selected.isStale)
        assertTrue(repository.current()?.isStale == true)
    }

    @Test
    fun `cancelling a suspended primary fetch does not try mirror or mutate accepted data`() = runBlocking {
        val primary = RemoteFeedEndpoint(URL("https://primary.example/feed.json"), OpportunityFeedSource.PRIMARY_NETWORK)
        val mirror = RemoteFeedEndpoint(URL("https://mirror.example/feed.json"), OpportunityFeedSource.FALLBACK_NETWORK)
        val started = CompletableDeferred<Unit>()
        val requests = Collections.synchronizedList(mutableListOf<URL>())
        val network = object : OpportunityFeedNetwork {
            override val endpoints = listOf(primary, mirror)
            override suspend fun fetch(endpoint: RemoteFeedEndpoint): String {
                requests += endpoint.url
                started.complete(Unit)
                awaitCancellation()
            }
        }
        val accepted = feed("accepted", "2026-08-14")
        val cache = FakeCache(accepted)
        val repository = repository(network, cache, feed("bundled", "2026-08-06"))
        repository.bootstrap()
        val refresh = launch { repository.refresh() }
        try {
            withTimeout(5_000) { started.await() }
        } finally {
            refresh.cancelAndJoin()
        }

        assertTrue(refresh.isCancelled)
        assertEquals(listOf(primary.url), requests.toList())
        assertEquals("accepted", repository.current()?.opportunities?.single()?.id)
        assertEquals(accepted, cache.value)
    }

    @Test
    fun `overlapping refresh stress retains newest memory and serializes cache writes`() = runBlocking {
        val primary = RemoteFeedEndpoint(URL("https://primary.example/feed.json"), OpportunityFeedSource.PRIMARY_NETWORK)
        val batchSize = 64
        val fetches = AtomicInteger()
        val allStarted = CompletableDeferred<Unit>()
        val newest = feed("live-15", "2026-08-15")
        val network = object : OpportunityFeedNetwork {
            override val endpoints = listOf(primary)
            override suspend fun fetch(endpoint: RemoteFeedEndpoint): String {
                val index = fetches.getAndIncrement()
                if (index == batchSize - 1) allStarted.complete(Unit)
                withTimeout(10_000) { allStarted.await() }
                return when (index % 4) {
                    0 -> newest
                    1 -> feed("live-14", "2026-08-14")
                    2 -> feed("live-13", "2026-08-13")
                    else -> error("offline during overlapping refresh")
                }
            }
        }
        val cached = AtomicReference(feed("accepted", "2026-08-12"))
        val activeWrites = AtomicInteger()
        val maxWrites = AtomicInteger()
        val cache = object : OpportunityFeedCache {
            override fun read() = cached.get()
            override fun write(json: String) {
                val writers = activeWrites.incrementAndGet()
                maxWrites.updateAndGet { maxOf(it, writers) }
                try {
                    repeat(100) { Thread.yield() }
                    cached.set(json)
                } finally {
                    activeWrites.decrementAndGet()
                }
            }
        }
        val repository = repository(network, cache, feed("bundled", "2026-08-06"))
        repository.bootstrap()

        withTimeout(15_000) {
            List(batchSize) { async(Dispatchers.Default) { repository.refresh() } }.awaitAll()
        }

        assertEquals(batchSize, fetches.get())
        assertEquals("live-15", repository.current()?.opportunities?.single()?.id)
        assertEquals(newest, cached.get())
        assertEquals(1, maxWrites.get())
        assertEquals(0, activeWrites.get())
    }

    @Test
    fun `repository search switches selected translation and uses its injected catalog`() = runBlocking {
        val catalog = AppStringCatalog.decode(
            """
            {
              "en": {"categoryCodingAndRobotics": "Coding and robotics"},
              "es": {"categoryCodingAndRobotics": "Programación y robótica"}
            }
            """.trimIndent(),
        )
        val repository = repository(
            network = FakeNetwork(emptyList(), emptyMap()),
            cache = FakeCache(null),
            bundledJson = translatedFeed(),
            catalog = catalog,
        )
        repository.bootstrap()

        assertEquals(
            listOf("translated"),
            repository.search(
                query = "robotica programacion",
                language = AppLanguage.SPANISH,
            ).map { it.id },
        )
        assertEquals(
            emptyList<String>(),
            repository.search(
                query = "robotica programacion",
                language = AppLanguage.ENGLISH,
            ).map { it.id },
        )
        assertEquals(
            emptyList<String>(),
            repository.search(
                query = "robotica programacion",
                language = AppLanguage.FRENCH,
            ).map { it.id },
        )
    }

    private fun repository(
        network: OpportunityFeedNetwork,
        cache: OpportunityFeedCache,
        bundledJson: String,
        catalog: AppStringCatalog? = null,
        nowProvider: () -> Instant = { now },
    ) = DefaultOpportunityRepository(
        codec = OpportunityFeedCodec(),
        network = network,
        cache = cache,
        bundled = object : BundledOpportunityFeedSource {
            override fun read() = bundledJson
        },
        catalog = catalog,
        now = nowProvider,
    )

    private fun translatedFeed(): String = """
        {
          "count": 1,
          "lastDataChange": "2026-08-15",
          "opportunities": [{
            "id": "translated",
            "title": "Robotics Club",
            "organization": "Provider",
            "description": "Build robots.",
            "category": "Coding & Robotics",
            "city": "Toronto",
            "region": "Toronto",
            "ageMin": 10,
            "cost": "Free to join",
            "status": "active",
            "sourceUrl": "https://example.org/translated",
            "translations": {
              "es": {"title": "Club de robótica"}
            }
          }]
        }
    """.trimIndent()

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
