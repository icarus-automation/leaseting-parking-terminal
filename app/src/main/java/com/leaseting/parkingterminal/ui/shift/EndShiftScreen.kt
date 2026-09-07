package com.leaseting.parkingterminal.ui.shift

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.leaseting.parkingterminal.R
import com.leaseting.parkingterminal.domain.parking.ShiftSummary
import com.leaseting.parkingterminal.ui.format.formatAmount
import com.leaseting.parkingterminal.ui.parking.userMessage
import com.leaseting.parkingterminal.ui.theme.LeasetingParkingTerminalTheme
import com.leaseting.parkingterminal.ui.theme.extendedColors
import java.math.BigDecimal

/**
 * Ending the shift: what the API expected, what the guard counted, and the
 * difference between them going to an admin as it stands.
 */
@Composable
fun EndShiftScreen(
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EndShiftViewModel = viewModel(factory = EndShiftViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    EndShiftScreen(
        uiState = uiState,
        onDeclaredChange = viewModel::onDeclaredChange,
        onNoteChange = viewModel::onNoteChange,
        onSubmit = viewModel::onSubmit,
        onRetry = viewModel::load,
        onFailureShown = viewModel::onFailureShown,
        onCancel = onCancel,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EndShiftScreen(
    uiState: EndShiftUiState,
    onDeclaredChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onRetry: () -> Unit,
    onFailureShown: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val failureMessage = uiState.failure?.userMessage()

    LaunchedEffect(failureMessage) {
        if (failureMessage != null) {
            snackbarHostState.showSnackbar(failureMessage)
            onFailureShown()
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.end_shift_title)) },
                actions = {
                    if (uiState.declared == null) {
                        TextButton(onClick = onCancel) {
                            Text(stringResource(R.string.action_cancel))
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when {
                uiState.declared != null -> DeclaredCard(uiState)

                uiState.isLoading -> Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator()
                }

                uiState.summary == null -> Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = stringResource(R.string.end_shift_no_summary),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TextButton(onClick = onRetry) { Text(stringResource(R.string.action_retry)) }
                }

                else -> DeclareForm(
                    summary = uiState.summary,
                    uiState = uiState,
                    onDeclaredChange = onDeclaredChange,
                    onNoteChange = onNoteChange,
                    onSubmit = onSubmit,
                )
            }
        }
    }
}

@Composable
private fun DeclareForm(
    summary: ShiftSummary,
    uiState: EndShiftUiState,
    onDeclaredChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(R.string.end_shift_expected_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = formatAmount(summary.expectedCash),
                    style = MaterialTheme.typography.headlineMedium,
                )
                HorizontalDivider()
                SummaryRow(
                    stringResource(R.string.gate_stat_entries),
                    summary.entryCount.toString(),
                )
                SummaryRow(
                    stringResource(R.string.gate_stat_exits),
                    summary.paidExitCount.toString(),
                )
            }
        }

        OutlinedTextField(
            value = uiState.declaredInput,
            onValueChange = onDeclaredChange,
            label = { Text(stringResource(R.string.end_shift_counted_label)) },
            singleLine = true,
            enabled = !uiState.isSubmitting,
            textStyle = MaterialTheme.typography.headlineSmall,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Next,
            ),
            modifier = Modifier.fillMaxWidth(),
        )

        uiState.variance?.let { variance -> VarianceRow(variance) }

        OutlinedTextField(
            value = uiState.note,
            onValueChange = onNoteChange,
            label = { Text(stringResource(R.string.end_shift_note_label)) },
            enabled = !uiState.isSubmitting,
            minLines = 2,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth(),
        )

        Button(
            onClick = onSubmit,
            enabled = uiState.canSubmit,
            modifier = Modifier
                .fillMaxWidth()
                .height(BIG_BUTTON_HEIGHT),
        ) {
            if (uiState.isSubmitting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                Text(
                    text = stringResource(R.string.end_shift_submit),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }

        Text(
            text = stringResource(R.string.end_shift_footnote),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Over, short or exact — stated plainly, in the guard's favour nowhere. It is
 * the number an admin reconciles against, so the screen does not soften it.
 */
@Composable
private fun VarianceRow(variance: BigDecimal, modifier: Modifier = Modifier) {
    val isExact = variance.signum() == 0
    val labelRes = when {
        isExact -> R.string.end_shift_variance_exact
        variance.signum() > 0 -> R.string.end_shift_variance_over
        else -> R.string.end_shift_variance_short
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = stringResource(labelRes), style = MaterialTheme.typography.titleMedium)
        Text(
            text = formatAmount(variance.abs()),
            style = MaterialTheme.typography.titleMedium,
            color = if (isExact) {
                MaterialTheme.extendedColors.success
            } else {
                MaterialTheme.extendedColors.warning
            },
        )
    }
}

@Composable
private fun DeclaredCard(uiState: EndShiftUiState, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.end_shift_declared_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.extendedColors.success,
            )
            SummaryRow(
                stringResource(R.string.end_shift_expected_label),
                formatAmount(uiState.declared?.expectedCash ?: BigDecimal.ZERO),
            )
            SummaryRow(
                stringResource(R.string.end_shift_counted_label),
                formatAmount(uiState.declared?.declaredCash ?: BigDecimal.ZERO),
            )
            Text(
                text = stringResource(R.string.end_shift_declared_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}

private val BIG_BUTTON_HEIGHT = 64.dp

@Preview(showBackground = true)
@Composable
private fun EndShiftPreview() {
    LeasetingParkingTerminalTheme {
        EndShiftScreen(
            uiState = EndShiftUiState(
                isLoading = false,
                summary = ShiftSummary("s1", 24, 19, BigDecimal("1850.00")),
                declaredInput = "1830",
            ),
            onDeclaredChange = {},
            onNoteChange = {},
            onSubmit = {},
            onRetry = {},
            onFailureShown = {},
            onCancel = {},
        )
    }
}
