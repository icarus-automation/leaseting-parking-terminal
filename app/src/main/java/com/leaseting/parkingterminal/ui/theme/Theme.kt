package com.leaseting.parkingterminal.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.unit.dp

/**
 * Light only, and never the device's dynamic palette: a guard reads this screen
 * outdoors on a fixed handheld, so the colours have to be the ones the receipts
 * and the web client were designed against, on every unit.
 */
private val LightColors = lightColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = OnPrimaryContainer,
    inversePrimary = InversePrimary,
    secondary = Secondary,
    onSecondary = OnSecondary,
    secondaryContainer = SecondaryContainer,
    onSecondaryContainer = OnSecondaryContainer,
    tertiary = Secondary,
    onTertiary = OnSecondary,
    tertiaryContainer = SecondaryContainer,
    onTertiaryContainer = OnSecondaryContainer,
    background = Background,
    onBackground = TextPrimary,
    surface = Surface,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = TextSecondary,
    surfaceContainer = SurfaceContainer,
    surfaceContainerLow = Surface,
    surfaceContainerHigh = SurfaceVariant,
    inverseSurface = InverseSurface,
    inverseOnSurface = InverseOnSurface,
    outline = Outline,
    outlineVariant = OutlineVariant,
    error = Error,
    onError = OnError,
    errorContainer = ErrorContainer,
    onErrorContainer = OnErrorContainer,
    scrim = Scrim,
)

/**
 * Softer than Material's defaults at the small end and squarer at the large one.
 * Cards and fields end up on the same family of corners, which is most of what
 * makes a stack of them read as one panel rather than as separate widgets.
 */
private val TerminalShapeScheme = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

@Composable
fun LeasetingParkingTerminalTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalExtendedColors provides LightExtendedColors,
        LocalMotion provides rememberDeviceMotion(),
    ) {
        MaterialTheme(
            colorScheme = LightColors,
            typography = Typography,
            shapes = TerminalShapeScheme,
            content = content,
        )
    }
}

/** `MaterialTheme.extendedColors` reads like the built-in `colorScheme`. */
val MaterialTheme.extendedColors: ExtendedColors
    @Composable
    @ReadOnlyComposable
    get() = LocalExtendedColors.current
