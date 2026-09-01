package com.rupayonhaldar.gtafreestem.ui.design

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rupayonhaldar.gtafreestem.R
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AppLaunchExperience(
    progress: Float,
    status: String,
    progressLabel: String,
    brandLabel: String,
    modifier: Modifier = Modifier,
) {
    val boundedProgress = progress.coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = boundedProgress,
        animationSpec = tween(durationMillis = 320),
        label = "launch-progress",
    )
    val infiniteTransition = rememberInfiniteTransition(label = "launch-science-field")
    val orbit by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(6_500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "launch-orbit",
    )

    StorybookBackground(modifier = modifier.testTag("app-launch-experience")) {
        ScienceField(
            orbit = orbit,
            modifier = Modifier.fillMaxSize(),
        )
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .widthIn(max = 520.dp)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Image(
                painter = painterResource(R.drawable.app_logo),
                contentDescription = brandLabel,
                modifier = Modifier
                    .size(150.dp)
                    .clip(RoundedCornerShape(32.dp)),
            )
            Text(
                text = brandLabel,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
            Text(
                text = status,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = progressLabel
                        progressBarRangeInfo = ProgressBarRangeInfo(
                            current = animatedProgress,
                            range = 0f..1f,
                        )
                    },
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                strokeCap = StrokeCap.Round,
            )
        }
    }
}

@Composable
private fun ScienceField(
    orbit: Float,
    modifier: Modifier = Modifier,
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val primary = MaterialTheme.colorScheme.primary.copy(alpha = if (isDark) 0.18f else 0.15f)
    val secondary = MaterialTheme.colorScheme.secondary.copy(alpha = if (isDark) 0.16f else 0.13f)
    val tertiary = MaterialTheme.colorScheme.tertiary.copy(alpha = if (isDark) 0.13f else 0.11f)
    Canvas(modifier = modifier.semantics { }) {
        val rotation = orbit * 2f * PI.toFloat()
        val atomCenter = Offset(size.width * 0.18f, size.height * 0.25f)
        repeat(3) { index ->
            rotate(degrees = index * 60f, pivot = atomCenter) {
                drawOval(
                    color = primary,
                    topLeft = Offset(atomCenter.x - 48.dp.toPx(), atomCenter.y - 18.dp.toPx()),
                    size = androidx.compose.ui.geometry.Size(96.dp.toPx(), 36.dp.toPx()),
                    style = Stroke(width = 1.5.dp.toPx()),
                )
            }
        }
        drawCircle(color = tertiary, radius = 5.dp.toPx(), center = atomCenter)

        val moleculeCenter = Offset(size.width * 0.82f, size.height * 0.72f)
        val moleculeRadius = 44.dp.toPx()
        val nodes = List(4) { index ->
            val angle = rotation + index * PI.toFloat() / 2f
            Offset(
                moleculeCenter.x + cos(angle) * moleculeRadius,
                moleculeCenter.y + sin(angle) * moleculeRadius,
            )
        }
        nodes.forEach { node ->
            drawLine(
                color = secondary,
                start = moleculeCenter,
                end = node,
                strokeWidth = 1.5.dp.toPx(),
            )
            drawCircle(color = primary, radius = 7.dp.toPx(), center = node)
        }
        drawCircle(color = tertiary, radius = 10.dp.toPx(), center = moleculeCenter)
    }
}
