package com.leaseting.parkingterminal.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/**
 * The spacing rhythm, on a 4dp grid.
 *
 * Screens name a step rather than a number, which is what stops a layout from
 * drifting into 13dp here and 18dp there once several people have touched it.
 */
object Spacing {
    /** Between a label and the value directly under it. */
    val Hair = 2.dp
    val Tight = 4.dp
    val Small = 8.dp
    val Medium = 12.dp

    /** The screen margin, and the gap between stacked cards. */
    val Gutter = 16.dp
    val Large = 24.dp
    val Loose = 32.dp
}

/**
 * Sizes that come from the hardware rather than from taste.
 *
 * The gate is worked one-handed, often in gloves, so the actions that move a
 * vehicle are far larger than the 48dp Material minimum. Nothing tappable is
 * ever smaller than [MinimumTouchTarget].
 */
object Sizes {
    /** Vehicle in, vehicle out, collect. The buttons a shift is made of. */
    val PrimaryAction = 72.dp

    /** End shift, retry, reprint. Secondary, but still a gloved thumb. */
    val SecondaryAction = 56.dp

    val MinimumTouchTarget = 48.dp

    val Spinner = 24.dp
    val SmallSpinner = 18.dp

    /** The success mark that confirms an entry or a collection. */
    val Confirmation = 56.dp

    val Hairline = 1.dp
}

/** Corners are a single radius everywhere, so cards and fields agree. */
object TerminalShapes {
    val Card = RoundedCornerShape(16.dp)
    val Field = RoundedCornerShape(12.dp)
    val Action = RoundedCornerShape(14.dp)
}
