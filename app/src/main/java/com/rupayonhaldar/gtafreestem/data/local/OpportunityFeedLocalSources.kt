package com.rupayonhaldar.gtafreestem.data.local

import android.content.Context
import android.util.AtomicFile
import com.rupayonhaldar.gtafreestem.data.feed.OpportunityFeedCodec
import com.rupayonhaldar.gtafreestem.domain.validation.PublicationRevisionPolicy
import com.rupayonhaldar.gtafreestem.data.io.MAX_FEED_BYTES
import com.rupayonhaldar.gtafreestem.data.io.readUtf8Bounded
import java.io.File
import java.io.IOException
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

internal interface OpportunityFeedCache {
    fun read(): String?
    fun write(json: String)

    /** Returns the exact snapshot selected by persistence, which may be newer. */
    fun writeAndSelectLatest(json: String, now: Instant): String {
        write(json)
        return json
    }
}

internal interface BundledOpportunityFeedSource {
    fun read(): String
}

internal class AndroidOpportunityFeedCache(
    context: Context,
    atomicFileFactory: (File) -> AtomicFile = ::AtomicFile,
) : OpportunityFeedCache {
    private val cachePath = File(context.filesDir, FILE_NAME).canonicalFile
    private val file = atomicFileFactory(cachePath)
    private val fileLock = pathLocks.computeIfAbsent(cachePath.path) { Any() }
    private val codec = OpportunityFeedCodec()

    override fun read(): String? = synchronized(fileLock) { readLocked() }

    private fun readLocked(): String? {
        // AtomicFile.openRead restores a last-good .bak after an interrupted
        // write. Inspecting the partial/missing base first bypasses recovery.
        // The stream reader still enforces the byte limit and strict UTF-8.
        return runCatching { file.openRead().use { it.readUtf8Bounded() } }.getOrNull()
    }

    override fun write(json: String) {
        writeAndSelectLatest(json, Instant.now())
    }

    override fun writeAndSelectLatest(json: String, now: Instant): String {
        val bytes = json.toByteArray(Charsets.UTF_8)
        require(bytes.size <= MAX_FEED_BYTES) { "Opportunity feed exceeds the cache limit" }
        val incoming = codec.decodeAndValidate(json, now, requireFreshness = false)
        return synchronized(fileLock) {
            val previousBytes = readLocked()
            val previous = previousBytes?.let { stored ->
                runCatching { codec.decodeAndValidate(stored, now, requireFreshness = false) }.getOrNull()
            }
            if (previousBytes != null && previous != null && !PublicationRevisionPolicy.shouldReplace(
                    incoming.lastUpdated, incoming.publicationRevision,
                    previous.lastUpdated, previous.publicationRevision,
                )
            ) {
                previousBytes
            } else {
                writeLocked(bytes)
                // AtomicFile reports some rename failures through logging only.
                // Do not claim a committed selection if the read-back disagrees.
                if (readLocked() != json) throw IOException("The opportunity cache commit could not be verified")
                json
            }
        }
    }

    private fun writeLocked(bytes: ByteArray) {
        val output = file.startWrite()
        try {
            output.write(bytes)
            file.finishWrite(output)
        } catch (error: Throwable) {
            file.failWrite(output)
            throw error
        }
    }

    private companion object {
        const val FILE_NAME = "opportunities-last-good.json"
        // UI and background-worker repositories own separate cache objects in
        // this process; AtomicFile itself does not provide mutual exclusion.
        val pathLocks = ConcurrentHashMap<String, Any>()
    }
}

internal class AndroidBundledOpportunityFeedSource(
    private val context: Context,
    private val rawResourceId: Int,
) : BundledOpportunityFeedSource {
    override fun read(): String = context.resources
        .openRawResource(rawResourceId)
        .use { it.readUtf8Bounded() }
}
