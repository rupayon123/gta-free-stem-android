package com.rupayonhaldar.gtafreestem.ui.browse

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rupayonhaldar.gtafreestem.MainActivity
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BrowseResponsivenessRuntimeTest {
    @get:Rule val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun typingAndFilterEditsKeepMainLooperResponsiveWithLiveFeed() {
        composeRule.onNodeWithTag("primary-navigation-opportunities").performClick()
        var feedCount = 0
        composeRule.waitUntil(timeoutMillis = 60_000) {
            feedCount = composeRule.onAllNodesWithText("available", substring = true)
                .fetchSemanticsNodes()
                .flatMap { node -> node.config[SemanticsProperties.Text] }
                .mapNotNull { text ->
                    Regex("""(\d{4}) available""").find(text.text)?.groupValues?.get(1)?.toIntOrNull()
                }
                .firstOrNull() ?: 0
            feedCount >= 1_000
        }

        val handler = Handler(Looper.getMainLooper())
        val delays = mutableListOf<Long>()
        val heartbeat = thread(name = "browse-main-looper-heartbeat") {
            repeat(40) {
                val postedAt = SystemClock.uptimeMillis()
                val completed = CountDownLatch(1)
                handler.post {
                    synchronized(delays) { delays += SystemClock.uptimeMillis() - postedAt }
                    completed.countDown()
                }
                assertTrue("Main Looper heartbeat timed out", completed.await(2, TimeUnit.SECONDS))
                Thread.sleep(25)
            }
        }

        listOf("r", "ro", "rob", "robot", "robotics", "science", "math").forEach { query ->
            composeRule.onNode(hasSetTextAction()).performTextReplacement(query)
        }
        composeRule.onNodeWithText("AI & Digital Media").performClick()
        composeRule.onNodeWithText("AI & Digital Media").performClick()
        heartbeat.join(15_000)
        assertTrue("Heartbeat thread did not finish", !heartbeat.isAlive)
        val maxDelay = synchronized(delays) { delays.maxOrNull() ?: Long.MAX_VALUE }
        Log.i("BrowseResponsiveness", "feed=$feedCount samples=${delays.size} maxMainLooperDelayMs=$maxDelay")
        assertTrue("Main Looper delayed $maxDelay ms", maxDelay < 1_000)
    }
}
