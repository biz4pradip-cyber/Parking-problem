package com.parkspot.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
 * A thin ring and one pointer. No tick marks: the ring does not rotate with the phone, so ticks
 * only ever implied a compass rose that was not there.
 *
 * @param rotationDegrees where the car is relative to the top of the screen; `null` while the
 *   heading is unknown, which greys the pointer out.
 */
@Composable
fun CompassDial(
    rotationDegrees: Float?,
    headline: String,
    subline: String,
    modifier: Modifier = Modifier,
    active: Boolean = true,
) {
    // Accumulated so the pointer always turns the short way round rather than unwinding through
    // 359 degrees when it crosses north.
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
    val ringColor = MaterialTheme.colorScheme.outlineVariant

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
                radius = radius - 1.dp.toPx(),
                center = center,
                style = Stroke(width = 1.dp.toPx()),
            )

            rotate(degrees = animatedRotation, pivot = center) {
                val tip = center.y - radius + 14.dp.toPx()
                val baseY = center.y - radius * 0.66f
                val halfWidth = radius * 0.085f
                val pointer = Path().apply {
                    moveTo(center.x, tip)
                    lineTo(center.x + halfWidth, baseY)
                    lineTo(center.x - halfWidth, baseY)
                    close()
                }
                drawPath(path = pointer, color = pointerColor)
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = headline,
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = subline,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
