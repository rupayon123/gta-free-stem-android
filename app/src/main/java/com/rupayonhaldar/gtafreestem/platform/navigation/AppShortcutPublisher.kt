package com.rupayonhaldar.gtafreestem.platform.navigation

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import com.rupayonhaldar.gtafreestem.MainActivity
import com.rupayonhaldar.gtafreestem.R
import com.rupayonhaldar.gtafreestem.localization.AppLanguage
import com.rupayonhaldar.gtafreestem.localization.AppStringCatalog
import com.rupayonhaldar.gtafreestem.ui.shell.PrimaryDestination

object AppShortcutPublisher {
    const val OPPORTUNITIES_ID = "open-opportunities"
    const val HIGH_SCHOOL_ID = "open-high-school"

    fun publish(
        context: Context,
        catalog: AppStringCatalog,
        language: AppLanguage,
    ): Boolean = runCatching {
        val appContext = context.applicationContext
        val manager = appContext.getSystemService(ShortcutManager::class.java)
            ?: return false
        manager.dynamicShortcuts = listOf(
            shortcut(
                context = appContext,
                id = OPPORTUNITIES_ID,
                destination = PrimaryDestination.OPPORTUNITIES,
                shortLabel = catalog.localizedOrFallback(
                    key = "navOpportunities",
                    language = language,
                    fallback = appContext.getString(R.string.shortcut_opportunities_short_label),
                ),
                longLabel = appContext.getString(R.string.shortcut_opportunities_long_label),
                iconResource = R.drawable.ic_search,
                rank = 0,
            ),
            shortcut(
                context = appContext,
                id = HIGH_SCHOOL_ID,
                destination = PrimaryDestination.HIGH_SCHOOL,
                shortLabel = catalog.localizedOrFallback(
                    key = "highSchool",
                    language = language,
                    fallback = appContext.getString(R.string.shortcut_high_school_short_label),
                ),
                longLabel = appContext.getString(R.string.shortcut_high_school_long_label),
                iconResource = R.drawable.ic_school,
                rank = 1,
            ),
        )
        true
    }.getOrDefault(false)

    /** Reports only launches that carry an ID inserted by this publisher. */
    fun reportUsageIfShortcut(context: Context, intent: Intent?) {
        val shortcutId = intent?.getStringExtra(SHORTCUT_ID_EXTRA)
            ?.takeIf { it == OPPORTUNITIES_ID || it == HIGH_SCHOOL_ID }
            ?: return
        runCatching {
            context.getSystemService(ShortcutManager::class.java)
                ?.reportShortcutUsed(shortcutId)
        }
    }

    private fun shortcut(
        context: Context,
        id: String,
        destination: PrimaryDestination,
        shortLabel: String,
        longLabel: String,
        iconResource: Int,
        rank: Int,
    ): ShortcutInfo = ShortcutInfo.Builder(context, id)
        .setShortLabel(shortLabel)
        .setLongLabel(longLabel)
        .setIcon(Icon.createWithResource(context, iconResource))
        .setRank(rank)
        .setIntent(
            Intent(Intent.ACTION_VIEW, AppDeepLink.uri(destination), context, MainActivity::class.java)
                .putExtra(SHORTCUT_ID_EXTRA, id)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        )
        .build()

    private const val SHORTCUT_ID_EXTRA = "gta_free_stem_shortcut_id"
}

private fun AppStringCatalog.localizedOrFallback(
    key: String,
    language: AppLanguage,
    fallback: String,
): String = text(key, language)
    .trim()
    .takeIf { it.isNotEmpty() && it != key }
    ?: fallback
