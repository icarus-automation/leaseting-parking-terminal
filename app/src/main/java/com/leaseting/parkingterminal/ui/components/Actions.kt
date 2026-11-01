package com.leaseting.parkingterminal.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.leaseting.parkingterminal.ui.theme.Sizes
import com.leaseting.parkingterminal.ui.theme.TerminalShapes
import com.leaseting.parkingterminal.ui.theme.motion

/**
 * The action a screen exists for: record the entry, collect the money, take the
 * next vehicle.
 *
 * Full width and 72dp tall, which is large enough to hit without looking while
 * a driver is waiting. The label crossfades to a spinner while the request is in
 * flight, so the button never changes size under a thumb that is still on it.
 */
@Composable
fun PrimaryAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isBusy: Boolean = false,
) {
    val quick = motion.quick

    Button(
        onClick = onClick,
        enabled = enabled && !isBusy,
        shape = TerminalShapes.Action,
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(Sizes.PrimaryAction),
    ) {
        AnimatedContent(
            targetState = isBusy,
            transitionSpec = {
                fadeIn(tween(quick)) togetherWith fadeOut(tween(quick))
            },
            label = "primary-action",
        ) { busy ->
            if (busy) {
                CircularProgressIndicator(
                    modifier = Modifier.size(Sizes.Spinner),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 3.dp,
                )
            } else {
                Text(text = text, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

/**
 * Everything a guard reaches for less often: end the shift, try again, reprint.
 * Still a full-width target, but outlined so it never competes with the action
 * above it.
 */
@Composable
fun SecondaryAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = TerminalShapes.Action,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.primary,
        ),
        border = BorderStroke(Sizes.Hairline, MaterialTheme.colorScheme.outline),
        modifier = modifier
            .fillMaxWidth()
            .height(Sizes.SecondaryAction),
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}
