package com.rupayonhaldar.gtafreestem.data.local

import android.content.Context
import android.content.ContextWrapper
import android.util.AtomicFile
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import java.util.UUID
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import java.util.concurrent.Callable
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AndroidOpportunityFeedCacheRuntimeTest {
    private val now = Instant.parse("2026-08-16T12:00:00Z")

    @Test
    fun separateInstancesDoNotLetAReadInvalidateAnInProgressAtomicWrite() = withTemporaryFiles { context ->
        val older = feed("older", "2026-08-14")
        val newer = feed("newer", "2026-08-15")
        val reader = AndroidOpportunityFeedCache(context)
        reader.writeAndSelectLatest(older, now)
        assertEquals(older, reader.read())
        val writeStarted = CountDownLatch(1)
        val releaseWrite = CountDownLatch(1)
        val writer = AndroidOpportunityFeedCache(context) { file ->
            object : AtomicFile(file) {
                override fun startWrite(): FileOutputStream {
                    val output = super.startWrite()
                    writeStarted.countDown()
                    try {
                        check(releaseWrite.await(5, TimeUnit.SECONDS)) { "Timed out waiting to finish the gated cache write" }
                    } catch (error: Throwable) {
                        super.failWrite(output)
                        throw error
                    }
                    return output
                }
            }
        }
        val executor = Executors.newSingleThreadExecutor()
        val readStarted = CountDownLatch(1)
        val readResult = CompletableFuture<String?>()
        val reading = Thread({
            try {
                readStarted.countDown()
                readResult.complete(reader.read())
            } catch (error: Throwable) {
                readResult.completeExceptionally(error)
            }
        }, "gta-cache-reader").apply { isDaemon = true }
        val writing = executor.submit(Callable { writer.writeAndSelectLatest(newer, now) })
        try {
            assertTrue(writeStarted.await(5, TimeUnit.SECONDS))
            reading.start()
            assertTrue(readStarted.await(5, TimeUnit.SECONDS))
            // Wait for the attempted read to either finish (the broken path) or
            // wait for the writer. This establishes ordering without a stress loop.
            val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
            while (!readResult.isDone && reading.state != Thread.State.BLOCKED && System.nanoTime() < deadline) {
                Thread.sleep(1)
            }
            assertTrue(readResult.isDone || reading.state == Thread.State.BLOCKED)
            releaseWrite.countDown()

            assertEquals(newer, writing.get(5, TimeUnit.SECONDS))
            assertEquals(newer, readResult.get(5, TimeUnit.SECONDS))
            assertEquals(newer, AndroidOpportunityFeedCache(context).read())
        } finally {
            releaseWrite.countDown()
            reading.join(5_000)
            executor.shutdownNow()
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS))
            assertFalse(reading.isAlive)
        }
    }

    @Test
    fun olderSeparateInstanceReceivesTheNewerCommittedBytes() = withTemporaryFiles { context ->
        val olderWorker = AndroidOpportunityFeedCache(context)
        val newerScreen = AndroidOpportunityFeedCache(context)
        val newer = feed("screen-newer", "2026-08-15")
        val older = feed("worker-older", "2026-08-14")

        assertEquals(newer, newerScreen.writeAndSelectLatest(newer, now))
        assertEquals(newer, olderWorker.writeAndSelectLatest(older, now))
        assertEquals(newer, AndroidOpportunityFeedCache(context).read())
    }

    @Test
    fun sameDayOlderPublicationFromSeparateInstanceCannotReplaceCommittedCorrection() = withTemporaryFiles { context ->
        val newer = feed("new-current-program", "2026-08-15").replaceFirst(
            "{", "{\n  \"publicationRevision\": \"2026-08-15T20:55:11.491Z\",",
        )
        val older = feed("removed-old-program", "2026-08-15").replaceFirst(
            "{", "{\n  \"publicationRevision\": \"2026-08-15T16:26:56.721Z\",",
        )
        val screen = AndroidOpportunityFeedCache(context)
        val worker = AndroidOpportunityFeedCache(context)

        assertEquals(newer, screen.writeAndSelectLatest(newer, now))
        assertEquals(newer, worker.writeAndSelectLatest(older, now))
        assertEquals(newer, AndroidOpportunityFeedCache(context).read())
        // The opposite arrival order must still accept the same-day correction.
        assertEquals(newer, screen.writeAndSelectLatest(newer, now))
    }

    @Test
    fun publicationMigrationAndInvalidRevisionWritesRetainExactBytes() = withTemporaryFiles { context ->
        fun revised(id: String, revision: String) = feed(id, "2026-08-15").replaceFirst(
            "{", "{\n  \"publicationRevision\": \"$revision\",",
        )
        val cache = AndroidOpportunityFeedCache(context)
        val legacy = feed("legacy", "2026-08-15")
        val first = revised("first", "2026-08-15T16:26:56.721Z")
        val corrected = revised("corrected", "2026-08-15T20:55:11.491Z")
        assertEquals(legacy, cache.writeAndSelectLatest(legacy, now))
        assertEquals(first, cache.writeAndSelectLatest(first, now))
        assertEquals(corrected, AndroidOpportunityFeedCache(context).writeAndSelectLatest(corrected, now))
        assertEquals(corrected, cache.writeAndSelectLatest(legacy, now))
        assertEquals(corrected, cache.writeAndSelectLatest(revised("conflict", "2026-08-15T20:55:11.491Z"), now))
        for (revision in listOf("bad", "2026-08-17T00:00:00.000Z")) {
            var rejected = false
            try { cache.writeAndSelectLatest(revised("invalid", revision), now) }
            catch (_: IllegalArgumentException) { rejected = true }
            assertTrue(rejected)
            assertEquals(corrected, cache.read())
        }
    }

    @Test
    fun invalidFuturePersistedRevisionCannotPinCache() = withTemporaryFiles { context ->
        val poisoned = feed("future", "2026-08-15").replaceFirst(
            "{", "{\n  \"publicationRevision\": \"2026-08-17T00:00:00.000Z\",",
        )
        File(context.filesDir, "opportunities-last-good.json").writeText(poisoned)
        val valid = feed("recovered", "2026-08-15")
        assertEquals(valid, AndroidOpportunityFeedCache(context).writeAndSelectLatest(valid, now))
        assertEquals(valid, AndroidOpportunityFeedCache(context).read())
    }

    @Test
    fun invalidPersistedBytesDoNotBlockAValidCommit() = withTemporaryFiles { context ->
        File(context.filesDir, "opportunities-last-good.json").writeText("{broken cache")
        val cache = AndroidOpportunityFeedCache(context)
        val valid = feed("recovered", "2026-08-15")

        assertEquals(valid, cache.writeAndSelectLatest(valid, now))
        assertEquals(valid, cache.read())
    }

    @Test
    fun equalTimestampCorrectionIsKeptAndOversizeInputDoesNotReplaceIt() = withTemporaryFiles { context ->
        val cache = AndroidOpportunityFeedCache(context)
        val original = feed("original", "2026-08-15")
        val corrected = feed("corrected", "2026-08-15")
        cache.writeAndSelectLatest(original, now)
        assertEquals(corrected, cache.writeAndSelectLatest(corrected, now))
        val tooLarge = corrected.replace("Description", "x".repeat(10_000_001))

        val failure = runCatching { cache.writeAndSelectLatest(tooLarge, now) }.exceptionOrNull()

        assertTrue(failure is IllegalArgumentException)
        assertEquals(corrected, cache.read())
    }

    @Test
    fun trustedUnversionedCacheMigratesButVersionedMissingRightsCannotReplaceIt() = withTemporaryFiles { context ->
        val legacy = JsonObject(Json.parseToJsonElement(feed("legacy", "2026-08-14")).jsonObject
            .toMutableMap().apply { remove("contentRights") }).toString()
        File(context.filesDir, "opportunities-last-good.json").writeText(legacy)
        val cache = AndroidOpportunityFeedCache(context)
        assertEquals(legacy, cache.read())
        val accepted = feed("accepted", "2026-08-15").replaceFirst(
            "{", "{\n  \"publicationRevision\": \"2026-08-15T20:55:11.491Z\",",
        )
        assertEquals(accepted, cache.writeAndSelectLatest(accepted, now))
        val missing = JsonObject(Json.parseToJsonElement(accepted).jsonObject
            .toMutableMap().apply { remove("contentRights") }).toString()

        assertTrue(runCatching { cache.writeAndSelectLatest(missing, now) }.exceptionOrNull() is IllegalArgumentException)
        assertEquals(accepted, AndroidOpportunityFeedCache(context).read())
    }

    @Test
    fun invalidAuthorityCannotOverwriteOrPinThePersistentCache() = withTemporaryFiles { context ->
        val accepted = feed("accepted", "2026-08-15")
        val poisoned = feed("poisoned", "2026-08-15").replace("terms-permitted", "publisher-approved")
        val cache = AndroidOpportunityFeedCache(context)
        assertEquals(accepted, cache.writeAndSelectLatest(accepted, now))
        assertTrue(runCatching { cache.writeAndSelectLatest(poisoned, now) }.exceptionOrNull() is IllegalArgumentException)
        assertEquals(accepted, cache.read())
        File(context.filesDir, "opportunities-last-good.json").writeText(poisoned)
        assertEquals(accepted, AndroidOpportunityFeedCache(context).writeAndSelectLatest(accepted, now))
        assertEquals(accepted, AndroidOpportunityFeedCache(context).read())
    }

    private fun withTemporaryFiles(block: (Context) -> Unit) {
        val application = ApplicationProvider.getApplicationContext<Context>()
        val directory = File(application.cacheDir, "feed-cache-test-${UUID.randomUUID()}")
        check(directory.mkdirs())
        val context = object : ContextWrapper(application) {
            override fun getFilesDir(): File = directory
        }
        try {
            block(context)
        } finally {
            check(directory.deleteRecursively())
        }
    }

    private fun feed(id: String, updated: String) = """
        {
          "count": 1,
          "lastDataChange": "$updated",
          "contentRights": {"termsVerifiedAt": "$updated", "restrictedOpportunityIds": [], "sources": [{"organization": "Markham Public Library", "sourceHost": "markham.bibliocommons.com", "records": 1, "status": "terms-permitted", "basisUrl": "https://markham.bibliocommons.com/info/terms/", "conditions": ["Automated access uses the provider's RSS/XML event feed.", "Every displayed extract links to its original BiblioCommons event page.", "GTA FREE STEM is free and does not monetize provider content."], "termsVerifiedAt": "$updated", "termsVersion": "2018-09-19", "termsDigestVersion": "bibliocommons-relevant-sections-v1", "termsSectionDigest": "sha256:451e63466c801166653f47336e236d05f9a50a3df45a917fa603e92bcd11c8a0"}]},
          "opportunities": [{
            "id": "$id", "title": "Title", "organization": "Markham Public Library",
            "description": "Description", "category": "STEM", "city": "Toronto",
            "region": "Toronto", "ageMin": 10, "cost": "Free", "status": "active",
            "sourceUrl": "https://markham.bibliocommons.com/events/$id",
            "registrationUrl": "https://markham.bibliocommons.com/events/$id",
            "sources": [{"label":"Official event page","url":"https://markham.bibliocommons.com/events/$id","capturedAt":"2026-08-15T12:00:00Z","confidence":"high"}]
          }]
        }
    """.trimIndent()
}
