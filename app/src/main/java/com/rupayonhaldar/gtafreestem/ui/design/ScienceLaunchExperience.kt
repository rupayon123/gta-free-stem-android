package com.rupayonhaldar.gtafreestem.ui.design

import android.animation.ValueAnimator
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rupayonhaldar.gtafreestem.R
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Presentation for a real pending load. The caller owns whether it is visible.
 * [status] must be localized by the caller. No progress estimate, minimum duration,
 * network work or navigation is performed here. Removing this composable stops motion.
 */
@Composable
fun ScienceLaunchExperience(status: String, modifier: Modifier = Modifier) {
    val durationScale = rememberCoroutineScope().coroutineContext[MotionDurationScale]
    val motionAllowed = (durationScale?.scaleFactor ?: 1f) > 0f && ValueAnimator.areAnimatorsEnabled()
    val orbit = scienceLoadingOrbit(motionAllowed)
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.background)
            .testTag("science-launch-experience")
            .padding(horizontal = 24.dp, vertical = 20.dp)
            .semantics(mergeDescendants = true) {
                liveRegion = LiveRegionMode.Polite
                progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier.widthIn(max = 320.dp).fillMaxWidth().aspectRatio(1f),
            contentAlignment = Alignment.Center,
        ) {
            // Animation state is read in draw/layer phases, not the composition/layout.
            // Counterrotation preserves upright science glyphs as their positions orbit.
            Canvas(Modifier.fillMaxSize().clearAndSetSemantics {}.testTag("science-launch-orbit")) {
                val scale = size.minDimension / 320f
                withTransform({ scale(scale, scale, pivot = Offset.Zero) }) {
                    val center = Offset(160f, 160f)
                    drawCircle(colors.primaryContainer, 126f, center, style = Stroke(1.7f))
                    drawCircle(colors.primaryContainer, 108f, center, style = Stroke(1f))
                    val phase = orbit.value
                    ScienceLoadingGlyph.entries.forEachIndexed { index, glyph ->
                        val angle = phase + 25f + index * 90f
                        val node = Offset(center.x + 126f, center.y)
                        val fill = if (index % 2 == 0) colors.tertiary else colors.primaryContainer
                        val ink = if (index % 2 == 0) colors.onTertiary else colors.onPrimaryContainer
                        rotate(angle, pivot = center) {
                            rotate(-angle, pivot = node) {
                                drawCircle(fill, 22f, node)
                                drawScienceLoadingGlyph(glyph, node, ink, fill)
                            }
                        }
                    }
                    drawCircle(colors.primary, 3f, Offset(61f, 66f))
                    drawCircle(colors.tertiary, 4f, Offset(261f, 242f))
                }
            }
            Image(
                painter = painterResource(R.drawable.app_logo),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize(0.51f)
                    .graphicsLayer {
                        val pulse = if (motionAllowed) {
                            (sin(orbit.value * PI / 90.0) * 0.015).toFloat()
                        } else {
                            0f
                        }
                        scaleX = 1f + pulse
                        scaleY = 1f + pulse
                    },
            )
        }
        Text(
            text = status,
            modifier = Modifier.widthIn(max = 440.dp).testTag("science-launch-status"),
            style = MaterialTheme.typography.bodyLarge,
            color = colors.onBackground,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun scienceLoadingOrbit(motionAllowed: Boolean): State<Float> {
    if (!motionAllowed) return remember { mutableFloatStateOf(0f) }
    return rememberInfiniteTransition(label = "science-loading-orbit").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 14_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "upright-science-glyph-positions",
    )
}

private enum class ScienceLoadingGlyph { Gear, Atom, Beaker, Bulb }

private fun DrawScope.drawScienceLoadingGlyph(
    glyph: ScienceLoadingGlyph,
    center: Offset,
    ink: Color,
    fill: Color,
) {
    val outline = Stroke(width = 1.8f, cap = StrokeCap.Round)
    when (glyph) {
        ScienceLoadingGlyph.Gear -> {
            val gear = Path().apply {
                repeat(24) { index ->
                    val radius = if (index % 3 == 2) 9f else 12f
                    val angle = index * PI / 12.0
                    val x = center.x + (cos(angle) * radius).toFloat()
                    val y = center.y + (sin(angle) * radius).toFloat()
                    if (index == 0) moveTo(x, y) else lineTo(x, y)
                }
                close()
            }
            drawPath(gear, ink)
            drawCircle(fill, 4f, center)
        }
        ScienceLoadingGlyph.Atom -> {
            repeat(3) { index ->
                rotate(index * 60f, pivot = center) {
                    drawOval(ink, center - Offset(14f, 5f), Size(28f, 10f), style = outline)
                }
            }
            drawCircle(ink, 2.7f, center)
        }
        ScienceLoadingGlyph.Beaker -> {
            val beaker = Path().apply {
                moveTo(center.x - 4f, center.y - 12f)
                lineTo(center.x - 4f, center.y - 3f)
                lineTo(center.x - 11f, center.y + 10f)
                quadraticTo(center.x - 12f, center.y + 13f, center.x - 8f, center.y + 13f)
                lineTo(center.x + 8f, center.y + 13f)
                quadraticTo(center.x + 12f, center.y + 13f, center.x + 11f, center.y + 10f)
                lineTo(center.x + 4f, center.y - 3f)
                lineTo(center.x + 4f, center.y - 12f)
            }
            drawPath(beaker, ink, style = outline)
            drawLine(ink, center + Offset(-6f, -12f), center + Offset(6f, -12f), 1.8f, StrokeCap.Round)
            drawLine(ink, center + Offset(-6f, 7f), center + Offset(6f, 7f), 1.8f, StrokeCap.Round)
            drawCircle(ink, 1.3f, center + Offset(2f, 3f))
        }
        ScienceLoadingGlyph.Bulb -> {
            drawCircle(ink, 7.5f, center + Offset(0f, -4f), style = outline)
            drawLine(ink, center + Offset(-4f, 3f), center + Offset(-3f, 10f), 1.8f, StrokeCap.Round)
            drawLine(ink, center + Offset(4f, 3f), center + Offset(3f, 10f), 1.8f, StrokeCap.Round)
            drawLine(ink, center + Offset(-3f, 10f), center + Offset(3f, 10f), 1.8f, StrokeCap.Round)
            drawLine(ink, center + Offset(-2f, 13f), center + Offset(2f, 13f), 1.8f, StrokeCap.Round)
            drawLine(ink, center + Offset(0f, -17f), center + Offset(0f, -14f), 1.5f, StrokeCap.Round)
            drawLine(ink, center + Offset(-14f, -5f), center + Offset(-11f, -5f), 1.5f, StrokeCap.Round)
            drawLine(ink, center + Offset(11f, -5f), center + Offset(14f, -5f), 1.5f, StrokeCap.Round)
        }
    }
}
