package com.leaseting.parkingterminal.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.leaseting.parkingterminal.ui.theme.Sizes
import com.leaseting.parkingterminal.ui.theme.Spacing
import com.leaseting.parkingterminal.ui.theme.extendedColors
import com.leaseting.parkingterminal.ui.theme.motion

/**
 * The mark that says the money is in, or the vehicle is on the record.
 *
 * It draws itself once, in about the time it takes to look up from the till,
 * and then holds still. That single stroke is the only decorative motion in the
 * app, and it earns its place: it is the difference between a guard trusting
 * that a tap landed and tapping again. With device animations off it is simply
 * there, already drawn.
 */
@Composable
fun ConfirmationMark(modifier: Modifier = Modifier, diameter: Dp = Sizes.Confirmation) {
    val stroke = MaterialTheme.extendedColors.success
    val ring = MaterialTheme.extendedColors.successContainer
    val duration = motion.emphasised

    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        if (duration == 0) {
            progress.snapTo(1f)
        } else {
            progress.animateTo(1f, tween(duration))
        }
    }

    Canvas(modifier = modifier.size(diameter)) {
        val width = size.minDimension
        val strokeWidth = width * STROKE_FRACTION

        drawCircle(color = ring, radius = width / 2f)

        // A tick in two strokes: down into the corner, then up and out. Both are
        // drawn from the same clock, so the corner is reached at the same point
        // of the animation every time.
        val start = Offset(width * 0.27f, width * 0.52f)
        val corner = Offset(width * 0.44f, width * 0.69f)
        val end = Offset(width * 0.75f, width * 0.34f)

        val drawn = progress.value
        val firstLeg = (drawn / CORNER_AT).coerceIn(0f, 1f)
        val secondLeg = ((drawn - CORNER_AT) / (1f - CORNER_AT)).coerceIn(0f, 1f)

        drawLine(
            color = stroke,
            start = start,
            end = start.lerpTo(corner, firstLeg),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
        if (secondLeg > 0f) {
            drawLine(
                color = stroke,
                start = corner,
                end = corner.lerpTo(end, secondLeg),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
            )
        }
    }
}

/**
 * The mark with the outcome beside it, which is how every success card in the
 * app opens.
 */
@Composable
fun ConfirmationHeader(text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Spacing.Medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ConfirmationMark(diameter = MARK_SIZE)
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.extendedColors.success,
        )
    }
}

private fun Offset.lerpTo(other: Offset, fraction: Float): Offset =
    Offset(x + (other.x - x) * fraction, y + (other.y - y) * fraction)

/** The tick spends the first half of its time on the short leg. */
private const val CORNER_AT = 0.42f
private const val STROKE_FRACTION = 0.1f

private val MARK_SIZE = 36.dp