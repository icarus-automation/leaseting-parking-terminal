package com.leaseting.parkingterminal.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * The type scale, sized for the way this app is actually read: a handheld held
 * at arm's length, outdoors, by someone with a queue behind them.
 *
 * Two departures from the Material defaults carry most of the weight. Body text
 * starts at 15sp rather than 14sp, and titles are semibold rather than medium,
 * so a glance picks out the structure of a screen before any of its words. The
 * styles that carry money, plate numbers and stub codes ask for tabular figures,
 * which keeps a column of amounts lined up on their decimal point.
 */

/** `tnum`: every digit the same width, so amounts in a column line up. */
private const val TABULAR_FIGURES = "tnum"

private val Sans = FontFamily.Default

private fun numeric(
    size: Int,
    lineHeight: Int,
    weight: FontWeight,
    tracking: Double = 0.0,
) = TextStyle(
    fontFamily = Sans,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = tracking.sp,
    fontFeatureSettings = TABULAR_FIGURES,
)

private fun text(
    size: Int,
    lineHeight: Int,
    weight: FontWeight,
    tracking: Double = 0.0,
) = TextStyle(
    fontFamily = Sans,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = tracking.sp,
)

val Typography = Typography(
    // Reserved for the one number a screen exists to show: the fee, the change,
    // the cash a shift owes.
    displaySmall = numeric(40, 46, FontWeight.Bold, -1.0),
    displayMedium = numeric(48, 54, FontWeight.Bold, -1.2),
    displayLarge = numeric(56, 62, FontWeight.Bold, -1.4),

    headlineLarge = numeric(32, 38, FontWeight.Bold, -0.6),
    headlineMedium = numeric(28, 34, FontWeight.Bold, -0.4),
    headlineSmall = numeric(23, 29, FontWeight.SemiBold, -0.2),

    titleLarge = text(20, 26, FontWeight.SemiBold, -0.1),
    titleMedium = text(17, 23, FontWeight.SemiBold),
    titleSmall = text(15, 20, FontWeight.SemiBold),

    bodyLarge = text(17, 25, FontWeight.Normal),
    bodyMedium = text(15, 22, FontWeight.Normal, 0.1),
    bodySmall = text(13, 18, FontWeight.Normal, 0.1),

    // Buttons on this device are pressed with a thumb through the sun, so their
    // label is a size up from the Material default.
    labelLarge = text(16, 20, FontWeight.SemiBold, 0.1),
    // The quiet caption above a value. Tracked out and used in caps, it reads as
    // a heading without needing to be big.
    labelMedium = text(12, 16, FontWeight.SemiBold, 0.9),
    labelSmall = text(11, 15, FontWeight.Medium, 0.6),
)
