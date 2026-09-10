package com.rupayonhaldar.gtafreestem.platform.navigation

import com.rupayonhaldar.gtafreestem.ui.shell.PrimaryDestination
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppDeepLinkTest {
    @Test
    fun canonicalTargetsMatchAppleApp() {
        val expected = mapOf(
            "gtafreestem://home" to PrimaryDestination.HOME,
            "gtafreestem://opportunities" to PrimaryDestination.OPPORTUNITIES,
            "gtafreestem://high-school" to PrimaryDestination.HIGH_SCHOOL,
            "gtafreestem://support" to PrimaryDestination.SUPPORT,
            "gtafreestem://account" to PrimaryDestination.ACCOUNT,
        )

        expected.forEach { (link, destination) ->
            assertEquals(destination, AppDeepLink.parse(link))
        }
    }

    @Test
    fun aliasesAndPathLinksMatchAppleApp() {
        mapOf(
            "hunt" to PrimaryDestination.OPPORTUNITIES,
            "search" to PrimaryDestination.OPPORTUNITIES,
            "highschool" to PrimaryDestination.HIGH_SCHOOL,
            "school" to PrimaryDestination.HIGH_SCHOOL,
            "feedback" to PrimaryDestination.SUPPORT,
            "submit" to PrimaryDestination.SUPPORT,
            "settings" to PrimaryDestination.ACCOUNT,
        ).forEach { (alias, destination) ->
            assertEquals(destination, AppDeepLink.parse("gtafreestem://$alias"))
            assertEquals(destination, AppDeepLink.parse("gtafreestem:///$alias"))
        }
    }

    @Test
    fun hostTakesPrecedenceAndMatchingIsCaseInsensitive() {
        assertEquals(
            PrimaryDestination.OPPORTUNITIES,
            AppDeepLink.parse("GTAFREESTEM://SEARCH/ignored"),
        )
    }

    @Test
    fun malformedForeignAndUnknownLinksAreIgnored() {
        listOf(
            null,
            "",
            "https://example.com/opportunities",
            "gtafreestem://unknown",
            "gtafreestem://user@home",
            "gtafreestem://home:80",
            "not a uri",
        ).forEach { link -> assertNull(AppDeepLink.parse(link)) }
    }
}
