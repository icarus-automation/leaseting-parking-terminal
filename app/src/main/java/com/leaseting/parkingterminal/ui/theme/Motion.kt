package com.leaseting.parkingterminal.ui.theme

import android.provider.Settings
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

/**
 * How long anything on screen is allowed to take.
 *
 * Motion here has one job: to say that the screen changed because of something
 * the guard just did. A vehicle is waiting at the barrier, so nothing loops,
 * nothing bounces, and nothing delays a tap. The longest thing in the app is the
 * mark that confirms money has been taken, and it is under half a second.
 *
 * When the device has animations turned off, every duration here collapses to
 * zero and the same code paths run without motion. That covers the platform's
 * own reduced-motion signal, which on Android is the animator duration scale in
 * developer options and accessibility settings.
 */
@Immutable
data class Motion(val enabled: Boolean) {

    /** A control reacting under a thumb. */
    val quick: Int = if (enabled) 110 else 0

    /** One piece of a screen replacing another. */
    val standard: Int = if (enabled) 220 else 0

    /** The confirmation of an entry or a collection, drawn once. */
    val emphasised: Int = if (enabled) 420 else 0

    val easing: Easing get() = TerminalEasing
}

/** Material's standard-decelerate curve: quick to start, settled at the end. */
private val TerminalEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

val LocalMotion = staticCompositionLocalOf { Motion(enabled = true) }

val motion: Motion
    @Composable
    @ReadOnlyComposable
    get() = LocalMotion.current

/**
 * Reads the device's animator duration scale once. A scale of zero is the
 * setting a person turns on when motion bothers them, and it is honoured rather
 * than guessed at.
 */
@Composable
internal fun rememberDeviceMotion(): Motion {
    val context = LocalContext.current
    return remember(context) {
        val scale = runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f,
            )
        }.getOrDefault(1f)
        Motion(enabled = scale > 0f)
    }
}

/**
 * One state of a screen giving way to the next: the outgoing part fades, the
 * incoming one fades up a few pixels behind it. Used where a form becomes a
 * receipt, or a spinner becomes a list.
 */
fun Motion.swap(): ContentTransform =
    (
        fadeIn(tween(standard, delayMillis = quick, easing = easing)) +
            slideInVertically(
                animationSpec = tween(standard, delayMillis = quick, easing = easing),
                initialOffsetY = { height -> height / SLIDE_FRACTION },
            )
        ).togetherWith(fadeOut(tween(quick, easing = easing)))

/** A short rise, roughly a finger's width on this screen. */
private const val SLIDE_FRACTION = 8
