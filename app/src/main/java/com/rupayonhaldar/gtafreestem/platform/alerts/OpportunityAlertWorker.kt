package com.rupayonhaldar.gtafreestem.platform.alerts

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.rupayonhaldar.gtafreestem.R
import com.rupayonhaldar.gtafreestem.data.local.SharedPreferencesBrowseSearchStateStore
import com.rupayonhaldar.gtafreestem.data.local.SharedPreferencesLocalAccountStore
import com.rupayonhaldar.gtafreestem.data.repository.OpportunityRepositories
import com.rupayonhaldar.gtafreestem.localization.AndroidAppStringCatalogLoader
import com.rupayonhaldar.gtafreestem.localization.AppLanguage
import com.rupayonhaldar.gtafreestem.localization.AppStringCatalog
import com.rupayonhaldar.gtafreestem.localization.SharedPreferencesLanguagePreferenceStore
import java.time.Clock

class OpportunityAlertWorker(
    appContext: Context,
    workerParameters: WorkerParameters,
) : CoroutineWorker(appContext, workerParameters) {
    override suspend fun doWork(): Result {
        val context = applicationContext
        val historyStore = SharedPreferencesOpportunityAlertHistoryStore(context)
        val lease = inputData
            .getString(WorkManagerOpportunityAlertSchedule.LEASE_INPUT_KEY)
            ?.trim()
            ?.takeIf(String::isNotEmpty)
            ?.let(::OpportunityAlertRunLease)
            ?: return Result.success()
        val language = runCatching {
            SharedPreferencesLanguagePreferenceStore(context).resolvedLanguageForDevice()
        }.getOrDefault(AppLanguage.ENGLISH)
        val catalog = runCatching {
            AndroidAppStringCatalogLoader.load(context)
        }.getOrNull()
        OpportunityNotificationPublisher.ensureChannel(
            context = context,
            localizedName = catalog?.localizedOrNull("alerts", language),
        )

        val runner = OpportunityAlertRefreshRunner(
            isPreferred = {
                // Construct a fresh policy store for every gate so an in-flight worker observes
                // an opt-out/deletion written after its network refresh started.
                SharedPreferencesLocalAccountStore(context)
                    .currentPreferences()
                    .opportunityAlertsPreferred
            },
            canNotify = {
                OpportunityNotificationPublisher.canPost(context)
            },
            loadMatchingIds = {
                val repository = OpportunityRepositories.create(context)
                runCatching { repository.bootstrap() }
                repository.refresh()
                val savedHunt = SharedPreferencesBrowseSearchStateStore(context).read()
                repository.search(
                    query = savedHunt.query,
                    filters = savedHunt.filters,
                    language = language,
                ).map { it.id }
            },
            history = historyStore,
            lease = lease,
            notificationCopy = { count ->
                notificationCopy(context, catalog, language, count)
            },
            postNotification = { copy, count, now ->
                OpportunityNotificationPublisher.post(context, copy, count, now)
            },
            nowEpochMillis = { Clock.systemUTC().millis() },
            isWorkerStopped = { isStopped },
        )
        return when (runner.run()) {
            OpportunityAlertRefreshOutcome.RETRY -> Result.retry()
            else -> Result.success()
        }
    }
}

private fun notificationCopy(
    context: Context,
    catalog: AppStringCatalog?,
    language: AppLanguage,
    count: Int,
): OpportunityNotificationCopy {
    val title = catalog?.localizedOrNull("newOpportunitiesNotificationTitle", language)
        ?: context.getString(R.string.new_opportunities_notification_title)
    val body = catalog?.localizedOrNull(
        key = "newOpportunitiesNotificationBody",
        language = language,
        placeholders = mapOf("count" to count.toString()),
    ) ?: context.resources.getQuantityString(
        R.plurals.new_opportunities_notification_body,
        count,
        count,
    )
    return OpportunityNotificationCopy(title = title, body = body)
}

private fun AppStringCatalog.localizedOrNull(
    key: String,
    language: AppLanguage,
    placeholders: Map<String, String> = emptyMap(),
): String? = text(key, language, placeholders)
    .trim()
    .takeIf { it.isNotEmpty() && it != key }
