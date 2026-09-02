package com.leaseting.parkingterminal.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * The leaseting-client palette, converted from its OKLCH source values to sRGB
 * (out-of-gamut chroma reduced the way CSS maps it, so the terminal and the web
 * client read as the same product).
 *
 * Nothing outside this file names a colour literal: screens take theirs from
 * `MaterialTheme.colorScheme` or `LocalExtendedColors`.
 */

// Brand — oklch(0.50 0.16 262) and the tones derived around it.
internal val Primary = Color(0xFF2C5DBD)
internal val OnPrimary = Color(0xFFFFFFFF)
internal val PrimaryContainer = Color(0xFFDCE9FF)
internal val OnPrimaryContainer = Color(0xFF072869)
internal val InversePrimary = Color(0xFF91B7FE)

// Secondary is unspecified by the palette, so it stays in the primary's hue
// family rather than introducing a colour the web client does not use.
internal val Secondary = Color(0xFF5E7295)
internal val OnSecondary = Color(0xFFFFFFFF)
internal val SecondaryContainer = Color(0xFFE4ECF9)
internal val OnSecondaryContainer = Color(0xFF202E47)

// Surfaces — oklch(1.000 0.000 0) background, with neutrals in the same hue.
internal val Background = Color(0xFFFFFFFF)
internal val Surface = Color(0xFFFFFFFF)
internal val SurfaceVariant = Color(0xFFF3F5F9)
internal val SurfaceContainer = Color(0xFFF7F8FB)
internal val InverseSurface = Color(0xFF1C1F25)
internal val InverseOnSurface = Color(0xFFF4F5F8)
internal val Scrim = Color(0xFF000000)

// Text — oklch(0.14 0.008 262) primary, oklch(0.52 0.015 262) secondary.
internal val TextPrimary = Color(0xFF07090C)
internal val TextSecondary = Color(0xFF646972)

// Outline — oklch(0.880 0.006 262).
internal val Outline = Color(0xFFD5D7DB)
internal val OutlineVariant = Color(0xFFE6E8EB)

// Status — oklch(0.54 0.22 15) error, oklch(0.52 0.18 145) success,
// oklch(0.72 0.15 68) warning. Material 3 has a slot for error only.
internal val Error = Color(0xFFCD0044)
internal val OnError = Color(0xFFFFFFFF)
internal val ErrorContainer = Color(0xFFFFE3E5)
internal val OnErrorContainer = Color(0xFF6D0020)

internal val Success = Color(0xFF008020)
internal val OnSuccess = Color(0xFFFFFFFF)
internal val SuccessContainer = Color(0xFFD7F5D7)
internal val OnSuccessContainer = Color(0xFF003F0B)

internal val Warning = Color(0xFFE1901F)
internal val OnWarning = Color(0xFFFFFFFF)
internal val WarningContainer = Color(0xFFFFEBD6)
internal val OnWarningContainer = Color(0xFF5F3800)
