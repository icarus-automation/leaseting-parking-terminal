package com.leaseting.parkingterminal.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.leaseting.parkingterminal.ui.theme.Spacing

/**
 * Waiting on the API, with the reason stated.
 *
 * A spinner on its own tells a guard the terminal is busy but not what it is
 * busy with, and at a barrier that is the difference between waiting and
 * restarting the app.
 */
@Composable
fun LoadingState(
    message: String,
    modifier: Modifier = Modifier,
    fillHeight: Boolean = true,
) {
    Column(
        modifier = modifier
            .then(if (fillHeight) Modifier.fillMaxSize() else Modifier.fillMaxWidth())
            .padding(Spacing.Large),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.Gutter, Alignment.CenterVertically),
    ) {
        CircularProgressIndicator(strokeWidth = 3.dp)
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * Nothing to show, and what to do about it.
 *
 * Every empty state in the app carries the same three things: what is not there,
 * why that might be, and the one action that changes it. A bare line of grey
 * text leaves a guard deciding whether the terminal is broken.
 */
@Composable
fun EmptyState(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(Spacing.Large),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.Small, Alignment.CenterVertically),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = READABLE_WIDTH),
        )
        if (actionText != null && onAction != null) {
            SecondaryAction(
                text = actionText,
                onClick = onAction,
                modifier = Modifier
                    .padding(top = Spacing.Small)
                    .widthIn(max = READABLE_WIDTH),
            )
        }
    }
}

/** Wide enough for a sentence, narrow enough that the eye does not have to hunt
 * for the start of the next line. */
private val READABLE_WIDTH = 320.dp
