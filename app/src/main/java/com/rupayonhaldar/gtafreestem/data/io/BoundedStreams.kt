package com.rupayonhaldar.gtafreestem.data.io

import java.io.ByteArrayOutputStream
import java.io.InputStream

internal const val MAX_FEED_BYTES: Int = 10_000_000

internal fun InputStream.readUtf8Bounded(maxBytes: Int = MAX_FEED_BYTES): String {
    val output = ByteArrayOutputStream(minOf(DEFAULT_BUFFER_SIZE, maxBytes))
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    var total = 0
    while (true) {
        val count = read(buffer)
        if (count < 0) break
        total += count
        if (total > maxBytes) throw FeedSizeLimitException(maxBytes)
        output.write(buffer, 0, count)
    }
    return output.toString(Charsets.UTF_8.name())
}

internal class FeedSizeLimitException(maxBytes: Int) :
    IllegalArgumentException("Opportunity feed exceeds the $maxBytes-byte limit")
