package com.leaseting.parkingterminal.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Which set of status bar icons this screen needs.
 *
 * The app draws behind the status bar, so the clock and the battery sit on
 * whatever the screen puts up there. Every working screen puts the brand blue
 * bar there and needs the light icons; sign-in puts the page under it and needs
 * the dark ones. Left to the platform, the choice comes from the device's dark
 * mode setting instead, which on a light-mode handheld paints navy on blue.
 */
@Composable
fun StatusBarIcons(onDarkBackground: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return

    val window = view.context.findActivity()?.window ?: return
    SideEffect {
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars =
            !onDarkBackground
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
