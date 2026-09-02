package com.leaseting.parkingterminal.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Semantic colours Material 3 has no slot for.
 *
 * A parking terminal reports outcomes constantly — synced, pending, needs
 * review — and those states need colours as stable as the ones in
 * `ColorScheme`. Reach them through `LocalExtendedColors.current` (or
 * `MaterialTheme.extendedColors`); never with a literal in a screen.
 */
@Immutable
data class ExtendedColors(
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val warning: Color,
    val onWarning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
)

internal val LightExtendedColors = ExtendedColors(
    success = Success,
    onSuccess = OnSuccess,
    successContainer = SuccessContainer,
    onSuccessContainer = OnSuccessContainer,
    warning = Warning,
    onWarning = OnWarning,
    warningContainer = WarningContainer,
    onWarningContainer = OnWarningContainer,
)

/**
 * Fails loudly rather than silently painting the wrong colour: a composable
 * that reads this outside [LeasetingParkingTerminalTheme] is a bug.
 */
val LocalExtendedColors = staticCompositionLocalOf<ExtendedColors> {
    error("No ExtendedColors provided. Wrap the content in LeasetingParkingTerminalTheme.")
}
