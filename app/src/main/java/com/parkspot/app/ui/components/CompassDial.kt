package com.parkspot.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.parkspot.app.util.GeoUtils

/**
 * The "walk this way" dial: a ring with a pointer that rotates to the car, and the remaining
 * distance in the middle.
 *
 * @param rotationDegrees where to point, relative to the top of the screen; `null` while the
 *   heading is unknown (no compass or no fix yet), which greys the pointer out.
 */
@Composable
fun CompassDial(
    rotationDegrees: Float?,
    headline: String,
    subline: String,
    modifier: Modifier = Modifier,
    active: Boolean = true,
) {
    // Accumulate rotation so the pointer always turns the short way round instead of unwinding
    // through 359 degrees when crossing north.
    var accumulated by remember { mutableFloatStateOf(rotationDegrees ?: 0f) }
    LaunchedEffect(rotationDegrees) {
        val target = rotationDegrees ?: return@LaunchedEffect
        accumulated += GeoUtils.shortestRotation(GeoUtils.normalizeDegrees(accumulated), target)
    }
    val animatedRotation by animateFloatAsState(
        targetValue = accumulated,
        animationSpec = tween(durationMillis = 250, easing = LinearEasing),
        label = "pointer",
    )

    val pointerColor = if (active && rotationDegrees != null) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }
    val ringColor = MaterialTheme.colorScheme.surfaceVariant
    val tickColor = MaterialTheme.colorScheme.outlineVariant

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = size.minDimension / 2f
            val center = Offset(size.width / 2f, size.height / 2f)

            drawCircle(
                color = ringColor,
                radius = radius - 6.dp.toPx(),
                center = center,
                style = Stroke(width = 12.dp.toPx()),
            )

            // Twelve ticks, so the ring reads as a dial rather than a plain circle.
            repeat(12) { index ->
                rotate(degrees = index * 30f, pivot = center) {
                    val long = index % 3 == 0
                    drawLine(
                        color = tickColor,
                        start = Offset(center.x, center.y - radius + 16.dp.toPx()),
                        end = Offset(
                            center.x,
                            center.y - radius + (if (long) 32.dp else 24.dp).toPx(),
                        ),
                        strokeWidth = (if (long) 3.dp else 1.5.dp).toPx(),
                    )
                }
            }

            rotate(degrees = animatedRotation, pivot = center) {
                val tip = center.y - radius + 22.dp.toPx()
                val baseY = center.y - radius * 0.45f
                val halfWidth = radius * 0.17f
                val pointer = Path().apply {
                    moveTo(center.x, tip)
                    lineTo(center.x + halfWidth, baseY)
                    lineTo(center.x, baseY - radius * 0.08f)
                    lineTo(center.x - halfWidth, baseY)
                    close()
                }
                drawPath(path = pointer, color = pointerColor)
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = headline,
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Text(
                text = subline,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
