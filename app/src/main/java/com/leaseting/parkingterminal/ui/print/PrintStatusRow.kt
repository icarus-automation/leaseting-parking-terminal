package com.leaseting.parkingterminal.ui.print

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.leaseting.parkingterminal.R
import com.leaseting.parkingterminal.ui.components.SecondaryAction
import com.leaseting.parkingterminal.ui.theme.Sizes
import com.leaseting.parkingterminal.ui.theme.Spacing
import com.leaseting.parkingterminal.ui.theme.extendedColors
import com.leaseting.parkingterminal.ui.theme.motion

/**
 * What the printer is doing, and the way back from it going wrong.
 *
 * A failure here is a warning, never an error: the money and the stay are
 * already recorded, so the only thing missing is paper. The reprint button
 * stays available after a successful print too, because a stub can tear.
 *
 * The status line crossfades between states rather than jumping, which keeps
 * the card from twitching under a thumb while a job is being sent.
 */
@Composable
fun PrintStatusRow(
    state: PrintState,
    onReprint: () -> Unit,
    @StringRes reprintLabelRes: Int,
    modifier: Modifier = Modifier,
) {
    val quick = motion.quick

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.Small),
    ) {
        AnimatedContent(
            targetState = state,
            transitionSpec = { fadeIn(tween(quick)) togetherWith fadeOut(tween(quick)) },
            label = "print-status",
        ) { current ->
            when (current) {
                PrintState.Idle -> Row {}

                PrintState.Printing -> StatusLine(
                    text = stringResource(R.string.print_printing),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    showSpinner = true,
                )

                PrintState.Sent -> StatusLine(
                    text = stringResource(R.string.print_sent),
                    color = MaterialTheme.extendedColors.success,
                    showSpinner = false,
                )

                is PrintState.Failed -> Column(
                    verticalArrangement = Arrangement.spacedBy(Spacing.Hair),
                ) {
                    StatusLine(
                        text = stringResource(current.messageRes),
                        color = MaterialTheme.extendedColors.warning,
                        showSpinner = false,
                    )
                    current.detail?.takeIf { it.isNotBlank() }?.let { detail ->
                        Text(
                            text = detail,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        if (state != PrintState.Printing) {
            SecondaryAction(text = stringResource(reprintLabelRes), onClick = onReprint)
        }
    }
}

@Composable
private fun StatusLine(
    text: String,
    color: androidx.compose.ui.graphics.Color,
    showSpinner: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showSpinner) {
            CircularProgressIndicator(
                modifier = Modifier.size(Sizes.SmallSpinner),
                strokeWidth = 2.dp,
            )
        }
        Text(text = text, style = MaterialTheme.typography.bodyMedium, color = color)
    }
}
