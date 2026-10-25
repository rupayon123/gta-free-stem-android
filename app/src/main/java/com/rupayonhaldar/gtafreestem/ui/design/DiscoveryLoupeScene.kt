package com.rupayonhaldar.gtafreestem.ui.design

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp

/**
 * Original Toronto drawing informed by lucasmarkes/hairline's isometric loupe.
 * Reference: 37d2e78 (MIT, Lucas Marques). Native Canvas; no web dependency.
 * Search changes the lens position. No timer, idle loop, or moving touch target.
 * Compose animation observes the system MotionDurationScale, including zero.
 */
@Composable
fun DiscoveryLoupeScene(query: String, modifier: Modifier = Modifier) {
    val progress by animateFloatAsState(
        targetValue = (query.length / 16f).coerceIn(0f, 1f),
        animationSpec = spring(dampingRatio = 0.84f, stiffness = 220f),
        label = "discovery-loupe-search",
    )
    val ink = MaterialTheme.colorScheme.onSurface
    val paper = MaterialTheme.colorScheme.surface
    val sheet = MaterialTheme.colorScheme.primaryContainer
    val teal = MaterialTheme.colorScheme.primary
    val yellow = MaterialTheme.colorScheme.tertiary
    Canvas(
        modifier = modifier.fillMaxWidth().height(104.dp).clearAndSetSemantics {},
    ) {
        val scale = minOf(size.width / 260, size.height / 160)
        fun line(points: List<Offset>, closed: Boolean = false) = Path().apply {
            points.firstOrNull()?.let { moveTo(it.x, it.y) }
            points.drop(1).forEach { lineTo(it.x, it.y) }
            if (closed) close()
        }
        val outline = Stroke(2f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        withTransform({
            translate((size.width - 260 * scale) / 2, (size.height - 160 * scale) / 2)
            scale(scale, scale, pivot = Offset.Zero)
        }) {
            val page = line(listOf(Offset(20f,91f), Offset(150f,46f), Offset(241f,91f), Offset(110f,143f)), closed = true)
            drawPath(page, sheet)
            drawPath(page, ink, style = outline)
            repeat(5) { row ->
                val y = row * 9f
                drawPath(line(listOf(Offset(48+y,87+y*.48f), Offset(151+y,52+y*.48f))), teal, style = Stroke(1f))
            }
            drawPath(line(listOf(Offset(156f,87f), Offset(156f,67f), Offset(168f,67f), Offset(168f,83f), Offset(179f,79f), Offset(179f,57f), Offset(192f,57f), Offset(192f,74f), Offset(205f,69f))), ink, style = outline)
            drawLine(ink, Offset(139f,87f), Offset(139f,25f), 2f, StrokeCap.Round)
            drawOval(yellow, Offset(130f,43f), Size(18f,7f))
            drawOval(ink, Offset(130f,43f), Size(18f,7f), style = outline)
            val x = 85 + progress.coerceIn(0f,1f) * 50
            val y = 81f
            listOf(Offset(-31f,22f), Offset(29f,23f), Offset(20f,-9f)).forEach { foot ->
                drawLine(ink, Offset(x+foot.x,y+foot.y), Offset(x+foot.x*.7f,y-20+foot.y*.35f), 2f, StrokeCap.Round)
            }
            val lens = Path().apply { addOval(androidx.compose.ui.geometry.Rect(x-32,y-43,x+32,y-4)) }
            drawPath(lens, paper)
            clipPath(lens) {
                repeat(5) { row ->
                    drawLine(ink, Offset(x-33,y-22+row*9), Offset(x+35,y-43+row*9), 1.5f)
                }
            }
            drawPath(lens, teal, style = Stroke(4f))
            drawCircle(yellow, 7f, Offset(220f,40f))
        }
    }
}
