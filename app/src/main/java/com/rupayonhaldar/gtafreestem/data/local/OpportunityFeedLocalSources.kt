package com.rupayonhaldar.gtafreestem.data.local

import android.content.Context
import android.util.AtomicFile
import com.rupayonhaldar.gtafreestem.data.io.MAX_FEED_BYTES
import com.rupayonhaldar.gtafreestem.data.io.readUtf8Bounded
import java.io.File

internal interface OpportunityFeedCache {
    fun read(): String?
    fun write(json: String)
}

internal interface BundledOpportunityFeedSource {
    fun read(): String
}

internal class AndroidOpportunityFeedCache(context: Context) : OpportunityFeedCache {
    private val file = AtomicFile(File(context.filesDir, FILE_NAME))

    override fun read(): String? {
        if (!file.baseFile.isFile || file.baseFile.length() > MAX_FEED_BYTES) return null
        return runCatching { file.openRead().use { it.readUtf8Bounded() } }.getOrNull()
    }

    override fun write(json: String) {
        val bytes = json.toByteArray(Charsets.UTF_8)
        require(bytes.size <= MAX_FEED_BYTES) { "Opportunity feed exceeds the cache limit" }
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
