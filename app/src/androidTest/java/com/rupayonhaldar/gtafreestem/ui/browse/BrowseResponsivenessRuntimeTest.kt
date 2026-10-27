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
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread
import org.junit.Assert.assertEquals
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
        composeRule.onNodeWithText("Filters", substring = true).performClick()
        val reset = composeRule.onNodeWithTag(OpportunityFilterPanelTestTags.RESET)
        if (!reset.fetchSemanticsNode().config.contains(SemanticsProperties.Disabled)) {
            reset.performClick()
        }
        composeRule.onNodeWithText("Back").performClick()
        composeRule.onNode(hasSetTextAction()).performTextReplacement("")
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
        val heartbeatFailure = AtomicReference<Throwable?>(null)
        val heartbeat = thread(name = "browse-main-looper-heartbeat") {
            try {
                repeat(40) {
                    val postedAt = SystemClock.uptimeMillis()
                    val completed = CountDownLatch(1)
                    handler.post {
                        synchronized(delays) { delays += SystemClock.uptimeMillis() - postedAt }
                        completed.countDown()
                    }
                    check(completed.await(2, TimeUnit.SECONDS)) { "Main Looper heartbeat timed out" }
                    Thread.sleep(25)
                }
            } catch (error: Throwable) {
                heartbeatFailure.set(error)
            }
        }

        listOf("r", "ro", "rob", "robot", "robotics", "science", "math").forEach { query ->
            composeRule.onNode(hasSetTextAction()).performTextReplacement(query)
        }
        composeRule.onNodeWithTag("browse-category-AI & Digital Media").performClick()
        composeRule.onNodeWithTag("browse-category-AI & Digital Media").performClick()
        heartbeat.join(15_000)
        assertTrue("Heartbeat thread did not finish", !heartbeat.isAlive)
        heartbeatFailure.get()?.let { throw AssertionError("Main Looper heartbeat failed", it) }
        val maxDelay = synchronized(delays) {
            assertEquals(40, delays.size)
            delays.maxOrNull() ?: Long.MAX_VALUE
        }
        Log.i("BrowseResponsiveness", "feed=$feedCount samples=${delays.size} maxMainLooperDelayMs=$maxDelay")
        assertTrue("Main Looper delayed $maxDelay ms", maxDelay < 1_000)
    }
}
