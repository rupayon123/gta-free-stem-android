package com.rupayonhaldar.gtafreestem.data.network

import com.rupayonhaldar.gtafreestem.data.io.MAX_FEED_BYTES
import com.rupayonhaldar.gtafreestem.data.io.readUtf8Bounded
import com.rupayonhaldar.gtafreestem.domain.model.OpportunityFeedSource
import java.io.InputStream
import java.net.URL
import java.util.zip.GZIPInputStream
import javax.net.ssl.HttpsURLConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal data class RemoteFeedEndpoint(
    val url: URL,
    val source: OpportunityFeedSource,
)

internal interface OpportunityFeedNetwork {
    val endpoints: List<RemoteFeedEndpoint>
    suspend fun fetch(endpoint: RemoteFeedEndpoint): String
}

internal class HttpsOpportunityFeedNetwork(
    override val endpoints: List<RemoteFeedEndpoint> = DEFAULT_ENDPOINTS,
) : OpportunityFeedNetwork {
    override suspend fun fetch(endpoint: RemoteFeedEndpoint): String = withContext(Dispatchers.IO) {
        require(endpoint.url.protocol.equals("https", ignoreCase = true)) { "HTTPS is required" }
        require(endpoint.url.host.isNotBlank()) { "A feed host is required" }

        val connection = endpoint.url.openConnection() as? HttpsURLConnection
            ?: throw IllegalArgumentException("HTTPS is required")
        try {
            connection.instanceFollowRedirects = false
            connection.useCaches = false
            connection.doInput = true
            connection.doOutput = false
            connection.connectTimeout = CONNECT_TIMEOUT_MILLIS
            connection.readTimeout = READ_TIMEOUT_MILLIS
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("Accept-Encoding", "identity")
            // Do not send, accept, or persist cookie state for a public static feed.
            connection.setRequestProperty("Cookie", "")
            connection.connect()

            if (connection.responseCode !in 200..299) {
                throw IllegalStateException("Feed returned HTTP ${connection.responseCode}")
            }
            val contentLength = connection.contentLengthLong
            if (contentLength > MAX_FEED_BYTES) {
                throw IllegalArgumentException("Opportunity feed exceeds the size limit")
            }

            val body: InputStream = if (connection.contentEncoding.equals("gzip", ignoreCase = true)) {
                GZIPInputStream(connection.inputStream)
            } else {
                connection.inputStream
            }
            body.use { it.readUtf8Bounded() }
        } finally {
            connection.disconnect()
        }
    }

    internal companion object {
        private const val CONNECT_TIMEOUT_MILLIS = 10_000
        private const val READ_TIMEOUT_MILLIS = 20_000

        val DEFAULT_ENDPOINTS = listOf(
            RemoteFeedEndpoint(
                URL("https://raw.githubusercontent.com/rupayon123/gta-free-stem-opportunities/main/public/opportunities.json"),
                OpportunityFeedSource.PRIMARY_NETWORK,
            ),
            RemoteFeedEndpoint(
                URL("https://cdn.jsdelivr.net/gh/rupayon123/gta-free-stem-opportunities@main/public/opportunities.json"),
                OpportunityFeedSource.FALLBACK_NETWORK,
            ),
        )
    }
}
