package com.leaseting.parkingterminal.ui.shift

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.leaseting.parkingterminal.R
import com.leaseting.parkingterminal.domain.parking.ShiftSummary
import com.leaseting.parkingterminal.ui.components.CardDivider
import com.leaseting.parkingterminal.ui.components.CardTone
import com.leaseting.parkingterminal.ui.components.ConfirmationHeader
import com.leaseting.parkingterminal.ui.components.DetailRow
import com.leaseting.parkingterminal.ui.components.EmptyState
import com.leaseting.parkingterminal.ui.components.EyebrowLabel
import com.leaseting.parkingterminal.ui.components.LoadingState
import com.leaseting.parkingterminal.ui.components.PrimaryAction
import com.leaseting.parkingterminal.ui.components.ScreenColumn
import com.leaseting.parkingterminal.ui.components.SectionCard
import com.leaseting.parkingterminal.ui.components.TerminalBarAction
import com.leaseting.parkingterminal.ui.components.TerminalScaffold
import com.leaseting.parkingterminal.ui.format.formatAmount
import com.leaseting.parkingterminal.ui.parking.userMessage
import com.leaseting.parkingterminal.ui.theme.LeasetingParkingTerminalTheme
import com.leaseting.parkingterminal.ui.theme.Spacing
import com.leaseting.parkingterminal.ui.theme.extendedColors
import com.leaseting.parkingterminal.ui.theme.motion
import com.leaseting.parkingterminal.ui.theme.swap
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
    val screenSwap = motion.swap()

    LaunchedEffect(failureMessage) {
        if (failureMessage != null) {
            snackbarHostState.showSnackbar(failureMessage)
            onFailureShown()
        }
    }

    TerminalScaffold(
        title = stringResource(R.string.end_shift_title),
        snackbarHostState = snackbarHostState,
        modifier = modifier,
        actions = {
            if (uiState.declared == null) {
                TerminalBarAction(
                    text = stringResource(R.string.action_cancel),
                    onClick = onCancel,
                )
            }
        },
    ) { innerPadding ->
        ScreenColumn(innerPadding) {
            AnimatedContent(
                targetState = uiState.stage,
                transitionSpec = { screenSwap },
                label = "end-shift",
            ) { stage ->
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.Gutter)) {
                    when (stage) {
                        Stage.Loading -> LoadingState(
                            message = stringResource(R.string.end_shift_loading),
                            fillHeight = false,
                        )

                        Stage.NoSummary -> EmptyState(
                            title = stringResource(R.string.end_shift_no_summary),
                            body = stringResource(R.string.end_shift_no_summary_body),
                            actionText = stringResource(R.string.action_retry),
                            onAction = onRetry,
                        )

                        Stage.Declaring -> uiState.summary?.let { summary ->
                            DeclareForm(
                                summary = summary,
                                uiState = uiState,
                                onDeclaredChange = onDeclaredChange,
                                onNoteChange = onNoteChange,
                                onSubmit = onSubmit,
                            )
                        }

                        Stage.Declared -> DeclaredCard(uiState)
                    }
                }
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
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Spacing.Gutter)) {
        SectionCard(tone = CardTone.Brand, verticalSpacing = Spacing.Small) {
            EyebrowLabel(
                text = stringResource(R.string.end_shift_expected_label),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                text = formatAmount(summary.expectedCash),
                style = MaterialTheme.typography.displaySmall,
            )
            CardDivider()
            DetailRow(
                label = stringResource(R.string.gate_stat_entries),
                value = summary.entryCount.toString(),
            )
            DetailRow(
                label = stringResource(R.string.gate_stat_exits),
                value = summary.paidExitCount.toString(),
            )
        }

        SectionCard {
            OutlinedTextField(
                value = uiState.declaredInput,
                onValueChange = onDeclaredChange,
                label = { Text(stringResource(R.string.end_shift_counted_label)) },
                singleLine = true,
                enabled = !uiState.isSubmitting,
                shape = MaterialTheme.shapes.small,
                textStyle = MaterialTheme.typography.headlineSmall,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Next,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            uiState.variance?.let { variance ->
                VarianceRow(variance)
            }

            OutlinedTextField(
                value = uiState.note,
                onValueChange = onNoteChange,
                label = { Text(stringResource(R.string.end_shift_note_label)) },
                enabled = !uiState.isSubmitting,
                minLines = 2,
                shape = MaterialTheme.shapes.small,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        PrimaryAction(
            text = stringResource(R.string.end_shift_submit),
            onClick = onSubmit,
            enabled = uiState.canSubmit,
            isBusy = uiState.isSubmitting,
        )

        Text(
            text = stringResource(R.string.end_shift_footnote),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Over, short or exact, stated plainly and in the guard's favour nowhere. It is
 * the number an admin reconciles against, so the screen does not soften it, and
 * the tinted band is there to make sure it is not missed on the way past.
 */
@Composable
private fun VarianceRow(variance: BigDecimal, modifier: Modifier = Modifier) {
    val extended = MaterialTheme.extendedColors
    val isExact = variance.signum() == 0
    val labelRes = when {
        isExact -> R.string.end_shift_variance_exact
        variance.signum() > 0 -> R.string.end_shift_variance_over
        else -> R.string.end_shift_variance_short
    }
    val container: Color = if (isExact) extended.successContainer else extended.warningContainer
    val onContainer: Color =
        if (isExact) extended.onSuccessContainer else extended.onWarningContainer

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(color = container, shape = MaterialTheme.shapes.small)
            .padding(horizontal = Spacing.Medium, vertical = Spacing.Small),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(labelRes),
            style = MaterialTheme.typography.titleSmall,
            color = onContainer,
        )
        Text(
            text = formatAmount(variance.abs()),
            style = MaterialTheme.typography.titleMedium,
            color = onContainer,
        )
    }
}

@Composable
private fun DeclaredCard(uiState: EndShiftUiState, modifier: Modifier = Modifier) {
    SectionCard(modifier = modifier, tone = CardTone.Confirmed) {
        ConfirmationHeader(stringResource(R.string.end_shift_declared_title))
        CardDivider()
        DetailRow(
            label = stringResource(R.string.end_shift_expected_label),
            value = formatAmount(uiState.declared?.expectedCash ?: BigDecimal.ZERO),
        )
        DetailRow(
            label = stringResource(R.string.end_shift_counted_label),
            value = formatAmount(uiState.declared?.declaredCash ?: BigDecimal.ZERO),
        )
        CardDivider()
        Text(
            text = stringResource(R.string.end_shift_declared_body),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

/** Where the hand-over has got to. */
private enum class Stage { Loading, NoSummary, Declaring, Declared }

private val EndShiftUiState.stage: Stage
    get() = when {
        declared != null -> Stage.Declared
        isLoading -> Stage.Loading
        summary == null -> Stage.NoSummary
        else -> Stage.Declaring
    }

@Preview(showBackground = true, heightDp = 900)
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
