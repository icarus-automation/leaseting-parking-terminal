package com.leaseting.parkingterminal.ui.print

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.leaseting.parkingterminal.R
import com.leaseting.parkingterminal.ui.theme.extendedColors

/**
 * What the printer is doing, and the way back from it going wrong.
 *
 * A failure here is a warning, never an error: the money and the stay are
 * already recorded, so the only thing missing is paper. The reprint button
 * stays available after a successful print too — a stub can tear.
 */
@Composable
fun PrintStatusRow(
    state: PrintState,
    onReprint: () -> Unit,
    @androidx.annotation.StringRes reprintLabelRes: Int,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        when (state) {
            PrintState.Idle -> Unit

            PrintState.Printing -> Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp))
                Text(
                    text = stringResource(R.string.print_printing),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            PrintState.Sent -> Text(
                text = stringResource(R.string.print_sent),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.extendedColors.success,
            )

            is PrintState.Failed -> Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(state.messageRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.extendedColors.warning,
                )
                state.detail?.takeIf { it.isNotBlank() }?.let { detail ->
                    Text(
                        text = detail,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        if (state != PrintState.Printing) {
            OutlinedButton(onClick = onReprint, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(reprintLabelRes))
            }
        }
    }
}
