package com.rupayonhaldar.gtafreestem.data.repository

import android.content.Context
import com.rupayonhaldar.gtafreestem.R
import com.rupayonhaldar.gtafreestem.data.feed.OpportunityFeedCodec
import com.rupayonhaldar.gtafreestem.data.local.AndroidBundledOpportunityFeedSource
import com.rupayonhaldar.gtafreestem.data.local.AndroidOpportunityFeedCache
import com.rupayonhaldar.gtafreestem.data.local.BundledOpportunityFeedSource
import com.rupayonhaldar.gtafreestem.data.local.OpportunityFeedCache
import com.rupayonhaldar.gtafreestem.data.network.HttpsOpportunityFeedNetwork
import com.rupayonhaldar.gtafreestem.data.network.OpportunityFeedNetwork
import com.rupayonhaldar.gtafreestem.domain.model.Opportunity
import com.rupayonhaldar.gtafreestem.domain.model.OpportunityFeedSnapshot
import com.rupayonhaldar.gtafreestem.domain.model.OpportunityFeedSource
import com.rupayonhaldar.gtafreestem.domain.repository.OpportunityRepository
import com.rupayonhaldar.gtafreestem.domain.repository.OpportunityRepositoryException
import com.rupayonhaldar.gtafreestem.domain.search.OpportunitySearch
import com.rupayonhaldar.gtafreestem.domain.search.OpportunitySearchFilters
import java.time.Instant
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object OpportunityRepositories {
    const val PRIMARY_FEED_URL =
        "https://raw.githubusercontent.com/rupayon123/gta-free-stem-opportunities/main/public/opportunities.json"
    const val FALLBACK_FEED_URL =
        "https://cdn.jsdelivr.net/gh/rupayon123/gta-free-stem-opportunities@main/public/opportunities.json"

    fun create(context: Context): OpportunityRepository {
        val appContext = context.applicationContext
        return create(appContext, R.raw.opportunities)
    }

    fun create(context: Context, bundledRawResourceId: Int): OpportunityRepository {
        val appContext = context.applicationContext
        return DefaultOpportunityRepository(
            codec = OpportunityFeedCodec(),
            network = HttpsOpportunityFeedNetwork(),
            cache = AndroidOpportunityFeedCache(appContext),
            bundled = AndroidBundledOpportunityFeedSource(appContext, bundledRawResourceId),
        )
    }
}

internal class DefaultOpportunityRepository(
    private val codec: OpportunityFeedCodec,
    private val network: OpportunityFeedNetwork,
    private val cache: OpportunityFeedCache,
    private val bundled: BundledOpportunityFeedSource,
    private val now: () -> Instant = Instant::now,
) : OpportunityRepository {
    private val retained = AtomicReference<OpportunityFeedSnapshot?>(null)

    override suspend fun bootstrap(): OpportunityFeedSnapshot = withContext(Dispatchers.IO) {
        val candidates = buildList {
            cache.read()?.let { cachedJson ->
                runCatching {
                    codec.decodeAndValidate(cachedJson, now(), requireFreshness = false)
                }.getOrNull()?.let { feed ->
                    add(feed.toSnapshot(OpportunityFeedSource.LAST_GOOD_CACHE))
                }
            }
            runCatching { bundled.read() }.getOrNull()?.let { bundledJson ->
                runCatching {
                    codec.decodeAndValidate(bundledJson, now(), requireFreshness = false)
                }.getOrNull()?.let { feed ->
                    add(feed.toSnapshot(OpportunityFeedSource.BUNDLED))
                }
            }
        }

        val selected = candidates.maxByOrNull(OpportunityFeedSnapshot::lastUpdated)
            ?: throw OpportunityRepositoryException("No validated local opportunity snapshot is available")
        retained.set(selected)
        selected
    }

    override suspend fun refresh(): OpportunityFeedSnapshot = withContext(Dispatchers.IO) {
        var lastError: Throwable? = null
        for (endpoint in network.endpoints) {
            try {
                val raw = network.fetch(endpoint)
                val feed = codec.decodeAndValidate(
                    raw,
                    now(),
                    requireFreshness = true,
                    requireSourceHealth = true,
                )
                val current = retained.get()
                if (current != null && feed.lastUpdated.isBefore(current.lastUpdated)) {
                    lastError = OpportunityRepositoryException(
                        "Remote opportunity snapshot is older than the retained snapshot",
                    )
                    continue
                }
                // A storage failure must not hide an otherwise valid live response.
                runCatching { cache.write(raw) }
                return@withContext feed.toSnapshot(endpoint.source).also(retained::set)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                lastError = error
            }
        }

        val previouslyRetained = retained.get()
        return@withContext runCatching { bootstrap() }.fold(
            onSuccess = { local ->
                listOfNotNull(previouslyRetained, local)
                    .maxBy(OpportunityFeedSnapshot::lastUpdated)
                    .also(retained::set)
            },
            onFailure = {
                previouslyRetained ?: throw OpportunityRepositoryException(
                    "No validated network or local opportunity snapshot is available",
                    lastError ?: it,
                )
            },
        )
    }

    override fun current(): OpportunityFeedSnapshot? = retained.get()

    override fun search(query: String, filters: OpportunitySearchFilters): List<Opportunity> =
        OpportunitySearch.search(retained.get()?.opportunities.orEmpty(), query, filters)

    override fun findById(id: String): Opportunity? {
        val normalized = id.trim()
        if (normalized.isEmpty()) return null
        return retained.get()?.opportunities?.firstOrNull { it.id == normalized }
    }
}

private fun com.rupayonhaldar.gtafreestem.data.feed.ValidatedOpportunityFeed.toSnapshot(
    source: OpportunityFeedSource,
) = OpportunityFeedSnapshot(
    opportunities = opportunities,
    lastUpdated = lastUpdated,
    source = source,
    declaredRecordCount = declaredRecordCount,
    isStale = isStale,
)
