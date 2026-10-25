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
import com.rupayonhaldar.gtafreestem.domain.validation.FeedFreshnessPolicy
import com.rupayonhaldar.gtafreestem.localization.AndroidAppStringCatalogLoader
import com.rupayonhaldar.gtafreestem.localization.AppLanguage
import com.rupayonhaldar.gtafreestem.localization.AppStringCatalog
import java.time.Instant
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
            catalog = AndroidAppStringCatalogLoader.load(appContext),
        )
    }
}

internal class DefaultOpportunityRepository(
    private val codec: OpportunityFeedCodec,
    private val network: OpportunityFeedNetwork,
    private val cache: OpportunityFeedCache,
    private val bundled: BundledOpportunityFeedSource,
    private val catalog: AppStringCatalog? = null,
    private val now: () -> Instant = Instant::now,
) : OpportunityRepository {
    private val retained = AtomicReference<OpportunityFeedSnapshot?>(null)
    private val publicationMutex = Mutex()

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

        publicationMutex.withLock {
            // Disk decoding may overlap a successful network refresh. Compare
            // again at publication so a late local result cannot roll it back.
            listOfNotNull(
                retained.get(),
                candidates.maxByOrNull(OpportunityFeedSnapshot::lastUpdated),
            ).maxByOrNull(OpportunityFeedSnapshot::lastUpdated)
                ?.let { selected ->
                    selected.copy(isStale = !FeedFreshnessPolicy.isFresh(selected.lastUpdated, now()))
                }
                ?.also(retained::set)
                ?: throw OpportunityRepositoryException("No validated local opportunity snapshot is available")
        }
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
                val accepted = publicationMutex.withLock {
                    val current = retained.get()
                    if (current != null && feed.lastUpdated.isBefore(current.lastUpdated)) {
                        null
                    } else {
                        // Keep the freshness decision, disk write and memory
                        // publication together across overlapping refreshes.
                        // A storage failure still permits a valid live result.
                        val selected = runCatching { cache.writeAndSelectLatest(raw, now()) }
                            .getOrDefault(raw)
                        val latest = if (selected == raw) {
                            feed.toSnapshot(endpoint.source)
                        } else {
                            // Another cache instance, such as the alert worker,
                            // may have already committed a newer valid response.
                            codec.decodeAndValidate(selected, now(), requireFreshness = false)
                                .toSnapshot(OpportunityFeedSource.LAST_GOOD_CACHE)
                        }
                        latest.also(retained::set)
                    }
                }
                if (accepted == null) {
                    lastError = OpportunityRepositoryException(
                        "Remote opportunity snapshot is older than the retained snapshot",
                    )
                    continue
                }
                return@withContext accepted
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                lastError = error
            }
        }

        return@withContext try {
            // bootstrap performs its own newest-snapshot check under the same
            // publication lock; never assign an older fallback after it returns.
            bootstrap()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            retained.get() ?: throw OpportunityRepositoryException(
                    "No validated network or local opportunity snapshot is available",
                    lastError ?: error,
                )
        }
    }

    override fun current(): OpportunityFeedSnapshot? = retained.get()

    override fun search(
        query: String,
        filters: OpportunitySearchFilters,
        language: AppLanguage,
    ): List<Opportunity> = OpportunitySearch.search(
        opportunities = retained.get()?.opportunities.orEmpty(),
        query = query,
        filters = filters,
        language = language,
        catalog = catalog,
    )

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
