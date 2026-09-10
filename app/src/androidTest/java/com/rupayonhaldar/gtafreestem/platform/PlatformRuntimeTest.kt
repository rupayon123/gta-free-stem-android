package com.rupayonhaldar.gtafreestem.platform

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutManager
import android.net.Uri
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.rupayonhaldar.gtafreestem.MainActivity
import com.rupayonhaldar.gtafreestem.localization.AndroidAppStringCatalogLoader
import com.rupayonhaldar.gtafreestem.localization.AppLanguage
import com.rupayonhaldar.gtafreestem.platform.alerts.OpportunityNotificationPublisher
import com.rupayonhaldar.gtafreestem.platform.alerts.WorkManagerOpportunityAlertSchedule
import com.rupayonhaldar.gtafreestem.platform.location.NearbyLocationViewModel
import com.rupayonhaldar.gtafreestem.platform.navigation.AppDeepLink
import com.rupayonhaldar.gtafreestem.platform.navigation.AppShortcutPublisher
import com.rupayonhaldar.gtafreestem.ui.shell.PrimaryDestination
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlatformRuntimeTest {
    @Test
    fun mainActivityHandlesColdAndWarmDeepLinks() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val coldIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("gtafreestem://opportunities"),
            context,
            MainActivity::class.java,
        )

        val scenario = ActivityScenario.launch<MainActivity>(coldIntent)
        scenario.onActivity { activity ->
            val cold = activity.platformNavigationCoordinator.request.value
            assertEquals(PrimaryDestination.OPPORTUNITIES, cold?.destination)

            activity.onNewIntent(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("gtafreestem://high-school"),
                    activity,
                    MainActivity::class.java,
                ),
            )
            val warm = activity.platformNavigationCoordinator.request.value
            assertEquals(PrimaryDestination.HIGH_SCHOOL, warm?.destination)
            assertTrue(requireNotNull(warm).sequence > requireNotNull(cold).sequence)
            // This emulator intermittently fails ActivityScenario's blocking DESTROYED wait even
            // after finish is accepted. Request teardown without making system-UI timing the test.
            activity.finish()
        }
    }

    @Test
    fun recreatingMainActivityDoesNotReplayItsColdDeepLink() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val coldIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("gtafreestem://opportunities"),
            context,
            MainActivity::class.java,
        )

        ActivityScenario.launch<MainActivity>(coldIntent).use { scenario ->
            scenario.onActivity { activity ->
                assertEquals(
                    PrimaryDestination.OPPORTUNITIES,
                    activity.platformNavigationCoordinator.request.value?.destination,
                )
            }

            scenario.recreate()

            scenario.onActivity { recreatedActivity ->
                assertNull(
                    "Recreation must restore Compose state without delivering the cold intent again",
                    recreatedActivity.platformNavigationCoordinator.request.value,
                )
            }
        }
    }

    @Test
    fun recreatingMainActivityRetainsItsNearbyLocationViewModel() {
        lateinit var originalViewModel: NearbyLocationViewModel

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                originalViewModel = activity.nearbyLocationViewModel
            }

            scenario.recreate()

            scenario.onActivity { recreatedActivity ->
                assertSame(originalViewModel, recreatedActivity.nearbyLocationViewModel)
            }
        }
    }

    @Test
    fun dynamicShortcutsOpenCanonicalDestinations() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val catalog = AndroidAppStringCatalogLoader.load(context)
        assertTrue(AppShortcutPublisher.publish(context, catalog, AppLanguage.ENGLISH))

        val shortcuts = context.getSystemService(ShortcutManager::class.java)
            .dynamicShortcuts
            .associateBy { it.id }
        assertEquals(
            PrimaryDestination.OPPORTUNITIES,
            AppDeepLink.parse(
                requireNotNull(shortcuts.getValue(AppShortcutPublisher.OPPORTUNITIES_ID).intent)
                    .dataString,
            ),
        )
        assertEquals(
            PrimaryDestination.HIGH_SCHOOL,
            AppDeepLink.parse(
                requireNotNull(shortcuts.getValue(AppShortcutPublisher.HIGH_SCHOOL_ID).intent)
                    .dataString,
            ),
        )
    }

    @Test
    fun notificationChannelAndUniquePeriodicWorkAreCreated() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        OpportunityNotificationPublisher.ensureChannel(context, "Test opportunity alerts")
        val channel = context
            .getSystemService(android.app.NotificationManager::class.java)
            .getNotificationChannel(OpportunityNotificationPublisher.CHANNEL_ID)
        assertNotNull(channel)

        val schedule = WorkManagerOpportunityAlertSchedule(context)
        assertTrue(schedule.schedule())
        val workManager = WorkManager.getInstance(context)
        var work = emptyList<WorkInfo>()
        repeat(30) {
            work = workManager
                .getWorkInfosForUniqueWork(WorkManagerOpportunityAlertSchedule.UNIQUE_WORK_NAME)
                .get(5, TimeUnit.SECONDS)
            if (work.any { it.state == WorkInfo.State.ENQUEUED }) return@repeat
            Thread.sleep(100)
        }
        assertTrue(work.any { it.state == WorkInfo.State.ENQUEUED })
        assertTrue(schedule.cancel())
    }
}
