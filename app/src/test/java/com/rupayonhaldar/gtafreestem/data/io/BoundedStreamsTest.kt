package com.rupayonhaldar.gtafreestem.data.io

import java.io.ByteArrayInputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class BoundedStreamsTest {
    @Test
    fun `accepts exact byte limit and rejects the next byte`() {
        assertEquals(
            "12345",
            ByteArrayInputStream("12345".toByteArray()).readUtf8Bounded(maxBytes = 5),
        )
        assertThrows(FeedSizeLimitException::class.java) {
            ByteArrayInputStream("123456".toByteArray()).readUtf8Bounded(maxBytes = 5)
        }
    }
}
