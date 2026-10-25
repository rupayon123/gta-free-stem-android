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
          "opportunities": [{
            "id": "$id", "title": "Title", "organization": "Provider",
            "description": "Description", "category": "STEM", "city": "Toronto",
            "region": "Toronto", "ageMin": 10, "cost": "Free", "status": "active",
            "sourceUrl": "https://example.org/$id"
          }]
        }
    """.trimIndent()
}
