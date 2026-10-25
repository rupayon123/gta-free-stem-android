package com.rupayonhaldar.gtafreestem.data.io

import java.io.ByteArrayInputStream
import java.nio.charset.CharacterCodingException
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

    @Test
    fun `rejects malformed UTF8 instead of replacing identifier bytes`() {
        val malformed = """{"id":"event-""".toByteArray(Charsets.UTF_8) +
            byteArrayOf(0xC3.toByte(), 0x28) +
            "\"}".toByteArray(Charsets.UTF_8)

        assertThrows(CharacterCodingException::class.java) {
            ByteArrayInputStream(malformed).readUtf8Bounded()
        }
    }

    @Test
    fun `rejects a multibyte UTF8 character truncated at EOF`() {
        val truncated = byteArrayOf(0xF0.toByte(), 0x9F.toByte(), 0xA7.toByte())

        assertThrows(CharacterCodingException::class.java) {
            fragmentedStream(truncated, chunkSize = 1).readUtf8Bounded()
        }
    }

    @Test
    fun `preserves valid multibyte UTF8 split across reads`() {
        val expected = "STEM café · বিজ্ঞান 🧪"
        val encoded = expected.toByteArray(Charsets.UTF_8)

        for (chunkSize in 1..3) {
            assertEquals(
                expected,
                fragmentedStream(encoded, chunkSize).readUtf8Bounded(maxBytes = encoded.size),
            )
        }
    }

    @Test
    fun `UTF8 byte cap permits an exact multibyte payload but not its next byte`() {
        val expected = "é🧪"
        val encoded = expected.toByteArray(Charsets.UTF_8)
        assertEquals(6, encoded.size)
        assertEquals(
            expected,
            fragmentedStream(encoded, chunkSize = 1).readUtf8Bounded(maxBytes = 6),
        )
        assertThrows(FeedSizeLimitException::class.java) {
            fragmentedStream(encoded + byteArrayOf(0x21), chunkSize = 1).readUtf8Bounded(maxBytes = 6)
        }
    }

    @Test
    fun `accepts a deliberately encoded Unicode replacement character`() {
        val expected = "STEM \uFFFD title"

        assertEquals(
            expected,
            fragmentedStream(expected.toByteArray(Charsets.UTF_8), chunkSize = 1).readUtf8Bounded(),
        )
    }

    private fun fragmentedStream(bytes: ByteArray, chunkSize: Int): ByteArrayInputStream =
        object : ByteArrayInputStream(bytes) {
            override fun read(buffer: ByteArray, offset: Int, length: Int): Int =
                super.read(buffer, offset, minOf(length, chunkSize))
        }
}
