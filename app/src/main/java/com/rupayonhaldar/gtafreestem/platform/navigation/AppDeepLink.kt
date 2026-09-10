package com.rupayonhaldar.gtafreestem.platform.navigation

import androidx.core.net.toUri
import com.rupayonhaldar.gtafreestem.ui.shell.PrimaryDestination
import java.net.URI
import java.util.Locale

/** App-owned links shared with the Apple app and Android shortcuts/notifications. */
object AppDeepLink {
    const val SCHEME = "gtafreestem"

    /**
     * Parses the first non-empty host or path component, matching the Apple app's aliases.
     * Other schemes and unknown targets are ignored instead of silently opening Home.
     */
    fun parse(rawLink: String?): PrimaryDestination? {
        val link = rawLink?.trim()?.takeIf(String::isNotEmpty) ?: return null
        val uri = runCatching { URI(link) }.getOrNull() ?: return null
        if (!uri.scheme.equals(SCHEME, ignoreCase = true)) return null
        if (uri.rawUserInfo != null || uri.port != -1) return null

        val target = sequenceOf(
            uri.host,
            uri.path?.trim('/'),
        ).firstOrNull { !it.isNullOrBlank() }
            ?.lowercase(Locale.ROOT)
            ?: return null

        return when (target) {
            "home" -> PrimaryDestination.HOME
            "opportunities", "hunt", "search" -> PrimaryDestination.OPPORTUNITIES
            "high-school", "highschool", "school" -> PrimaryDestination.HIGH_SCHOOL
            "support", "feedback", "submit" -> PrimaryDestination.SUPPORT
            "account", "settings" -> PrimaryDestination.ACCOUNT
            else -> null
        }
    }

    fun uri(destination: PrimaryDestination): android.net.Uri =
        "$SCHEME://${destination.canonicalDeepLinkTarget}".toUri()
}

private val PrimaryDestination.canonicalDeepLinkTarget: String
    get() = when (this) {
        PrimaryDestination.HOME -> "home"
        PrimaryDestination.OPPORTUNITIES -> "opportunities"
        PrimaryDestination.HIGH_SCHOOL -> "high-school"
        PrimaryDestination.SUPPORT -> "support"
        PrimaryDestination.ACCOUNT -> "account"
    }
