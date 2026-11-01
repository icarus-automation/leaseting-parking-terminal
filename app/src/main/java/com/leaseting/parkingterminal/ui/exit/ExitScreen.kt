package com.leaseting.parkingterminal.ui.exit

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.leaseting.parkingterminal.R
import com.leaseting.parkingterminal.domain.parking.FeeQuote
import com.leaseting.parkingterminal.domain.parking.ParkingSession
import com.leaseting.parkingterminal.domain.parking.ParkingSessionStatus
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
import com.leaseting.parkingterminal.ui.format.formatDuration
import com.leaseting.parkingterminal.ui.format.formatTicketCode
import com.leaseting.parkingterminal.ui.format.formatTime
import com.leaseting.parkingterminal.ui.parking.userMessage
import com.leaseting.parkingterminal.ui.print.PrintState
import com.leaseting.parkingterminal.ui.print.PrintStatusRow
import com.leaseting.parkingterminal.ui.theme.LeasetingParkingTerminalTheme
import com.leaseting.parkingterminal.ui.theme.Spacing
import com.leaseting.parkingterminal.ui.theme.extendedColors
import com.leaseting.parkingterminal.ui.theme.motion
import com.leaseting.parkingterminal.ui.theme.swap
import java.math.BigDecimal
import java.time.Instant

/**
 * The money screen: what is owed, what was handed over, what goes back.
 *
 * There is no field for the fee. It is the API's number, shown as it stands,
 * and the guard's only input is the cash in their hand.
 */
@Composable
fun ExitScreen(
    sessionId: String,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ExitViewModel = viewModel(factory = ExitViewModel.factory(sessionId)),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ExitScreen(
        uiState = uiState,
        onCashChange = viewModel::onCashChange,
        onCashPreset = viewModel::onCashPreset,
        onCollect = viewModel::onCollect,
        onRetryQuote = viewModel::loadQuote,
        onReprint = viewModel::printReceipt,
        onFailureShown = viewModel::onFailureShown,
        onDone = onDone,
        modifier = modifier,
    )
}

@Composable
private fun ExitScreen(
    uiState: ExitUiState,
    onCashChange: (String) -> Unit,
    onCashPreset: (BigDecimal) -> Unit,
    onCollect: () -> Unit,
    onRetryQuote: () -> Unit,
    onReprint: () -> Unit,
    onFailureShown: () -> Unit,
    onDone: () -> Unit,
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
        title = stringResource(R.string.exit_title),
        snackbarHostState = snackbarHostState,
        modifier = modifier,
        actions = {
            TerminalBarAction(text = stringResource(R.string.action_done), onClick = onDone)
        },
    ) { innerPadding ->
        ScreenColumn(innerPadding) {
            // Pricing, then collecting, then the receipt. Each replaces the last
            // in place, so the screen never appears to navigate somewhere new
            // while a driver is waiting at the barrier.
            AnimatedContent(
                targetState = uiState.stage,
                transitionSpec = { screenSwap },
                label = "exit",
            ) { stage ->
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.Gutter)) {
                    when (stage) {
                        Stage.Pricing -> LoadingState(
                            message = stringResource(R.string.exit_pricing),
                            fillHeight = false,
                        )

                        Stage.NoQuote -> EmptyState(
                            title = stringResource(R.string.exit_no_quote),
                            body = stringResource(R.string.exit_no_quote_body),
                            actionText = stringResource(R.string.action_retry),
                            onAction = onRetryQuote,
                        )

                        Stage.Collecting -> uiState.quote?.let { quote ->
                            CollectForm(
                                quote = quote,
                                uiState = uiState,
                                onCashChange = onCashChange,
                                onCashPreset = onCashPreset,
                                onCollect = onCollect,
                            )
                        }

                        Stage.Paid -> uiState.paid?.let { paid ->
                            ReceiptCard(
                                session = paid,
                                printState = uiState.printState,
                                onReprint = onReprint,
                            )
                            PrimaryAction(
                                text = stringResource(R.string.exit_next_vehicle),
                                onClick = onDone,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CollectForm(
    quote: FeeQuote,
    uiState: ExitUiState,
    onCashChange: (String) -> Unit,
    onCashPreset: (BigDecimal) -> Unit,
    onCollect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Spacing.Gutter)) {
        FeeCard(quote)

        SectionCard {
            EyebrowLabel(stringResource(R.string.exit_section_payment))

            OutlinedTextField(
                value = uiState.cashInput,
                onValueChange = onCashChange,
                label = { Text(stringResource(R.string.exit_cash_label)) },
                singleLine = true,
                isError = uiState.isShortTender,
                supportingText = if (uiState.isShortTender) {
                    { Text(stringResource(R.string.exit_cash_short)) }
                } else {
                    null
                },
                enabled = !uiState.isCollecting,
                shape = MaterialTheme.shapes.small,
                textStyle = MaterialTheme.typography.headlineSmall,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Done,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            // The notes a driver actually hands over at a Manila gate, plus the
            // exact fee for the common case where they have counted it out.
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
                verticalArrangement = Arrangement.spacedBy(Spacing.Small),
            ) {
                CashChip(
                    label = stringResource(R.string.exit_cash_exact),
                    onClick = { onCashPreset(quote.feeAmount) },
                )
                CASH_PRESETS
                    .filter { it > quote.feeAmount }
                    .forEach { amount ->
                        CashChip(
                            label = formatAmount(amount),
                            onClick = { onCashPreset(amount) },
                        )
                    }
            }

            CardDivider()

            AmountRow(
                label = stringResource(R.string.exit_change_label),
                value = formatAmount(uiState.change ?: BigDecimal.ZERO),
                emphasise = true,
            )
        }

        PrimaryAction(
            text = stringResource(R.string.exit_collect),
            onClick = onCollect,
            enabled = uiState.canCollect,
            isBusy = uiState.isCollecting,
        )
    }
}

/**
 * The stay and its price. The fee is the largest thing on the screen because it
 * is the one number that has to be read correctly the first time, out loud, to
 * someone in a car.
 */
@Composable
private fun FeeCard(quote: FeeQuote, modifier: Modifier = Modifier) {
    SectionCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.Hair)) {
            Text(
                text = quote.plateNumber,
                style = MaterialTheme.typography.headlineSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${formatTicketCode(quote.ticketCode)} · ${quote.vehicleTypeName}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = FontFamily.Monospace,
            )
        }

        CardDivider()

        Column(verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
            DetailRow(stringResource(R.string.exit_detail_in), formatTime(quote.entryAt))
            DetailRow(stringResource(R.string.exit_detail_stay), formatDuration(quote.stayMinutes))
            if (quote.graceMinutes > 0) {
                DetailRow(
                    label = stringResource(R.string.exit_detail_grace),
                    value = formatDuration(quote.graceMinutes),
                    valueColor = MaterialTheme.extendedColors.success,
                )
            }
            DetailRow(stringResource(R.string.exit_detail_rate), quote.ratePlanName)
        }

        CardDivider()

        Column(verticalArrangement = Arrangement.spacedBy(Spacing.Hair)) {
            EyebrowLabel(stringResource(R.string.exit_fee_label))
            Text(
                text = formatAmount(quote.feeAmount),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun CashChip(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    SuggestionChip(
        onClick = onClick,
        label = { Text(text = label, style = MaterialTheme.typography.labelLarge) },
        shape = MaterialTheme.shapes.small,
        colors = SuggestionChipDefaults.suggestionChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            labelColor = MaterialTheme.colorScheme.onSurface,
        ),
        border = null,
        modifier = modifier.height(CHIP_HEIGHT),
    )
}

@Composable
private fun AmountRow(
    label: String,
    value: String,
    emphasise: Boolean,
    modifier: Modifier = Modifier,
    valueColor: Color = Color.Unspecified,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, style = MaterialTheme.typography.titleMedium)
        Text(
            text = value,
            style = if (emphasise) {
                MaterialTheme.typography.headlineMedium
            } else {
                MaterialTheme.typography.titleMedium
            },
            color = valueColor,
        )
    }
}

/**
 * What was collected, once the API has taken it. The mark draws itself once so
 * a guard can look up from the cash and know the transaction landed.
 */
@Composable
private fun ReceiptCard(
    session: ParkingSession,
    printState: PrintState,
    onReprint: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SectionCard(modifier = modifier, tone = CardTone.Confirmed) {
        ConfirmationHeader(stringResource(R.string.exit_collected))
        Text(
            text = session.plateNumber,
            style = MaterialTheme.typography.headlineSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        CardDivider()

        Column(verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
            AmountRow(
                label = stringResource(R.string.exit_fee_label),
                value = formatAmount(session.feeAmount ?: BigDecimal.ZERO),
                emphasise = false,
            )
            AmountRow(
                label = stringResource(R.string.exit_cash_label),
                value = formatAmount(session.amountTendered ?: BigDecimal.ZERO),
                emphasise = false,
            )
        }

        CardDivider()

        AmountRow(
            label = stringResource(R.string.exit_change_label),
            value = formatAmount(session.changeGiven ?: BigDecimal.ZERO),
            emphasise = true,
        )

        CardDivider()

        PrintStatusRow(
            state = printState,
            onReprint = onReprint,
            reprintLabelRes = R.string.exit_reprint,
        )
    }
}

/** The four things this screen can be doing, in the order it does them. */
private enum class Stage { Pricing, NoQuote, Collecting, Paid }

private val ExitUiState.stage: Stage
    get() = when {
        paid != null -> Stage.Paid
        isLoadingQuote -> Stage.Pricing
        quote == null -> Stage.NoQuote
        else -> Stage.Collecting
    }

private val CHIP_HEIGHT = 44.dp

private val CASH_PRESETS = listOf(
    BigDecimal("20"),
    BigDecimal("50"),
    BigDecimal("100"),
    BigDecimal("200"),
    BigDecimal("500"),
    BigDecimal("1000"),
)

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun ExitCollectPreview() {
    LeasetingParkingTerminalTheme {
        ExitScreen(
            uiState = ExitUiState(
                isLoadingQuote = false,
                quote = FeeQuote(
                    sessionId = "s1",
                    ticketCode = "A3KD9MPQ",
                    plateNumber = "ABC 1234",
                    vehicleTypeName = "Car",
                    entryAt = Instant.parse("2026-09-07T02:00:00Z"),
                    quotedAt = Instant.parse("2026-09-07T04:30:00Z"),
                    stayMinutes = 150,
                    billedMinutes = 135,
                    graceMinutes = 15,
                    ratePlanName = "Hourly",
                    feeAmount = BigDecimal("150.00"),
                ),
                cashInput = "200",
            ),
            onCashChange = {},
            onCashPreset = {},
            onCollect = {},
            onRetryQuote = {},
            onReprint = {},
            onFailureShown = {},
            onDone = {},
        )
    }
}

@Preview(showBackground = true, heightDp = 780)
@Composable
private fun ExitReceiptPreview() {
    LeasetingParkingTerminalTheme {
        ExitScreen(
            uiState = ExitUiState(
                isLoadingQuote = false,
                paid = ParkingSession(
                    id = "s1",
                    ticketCode = "A3KD9MPQ",
                    plateNumber = "ABC 1234",
                    status = ParkingSessionStatus.PAID,
                    entryAt = Instant.parse("2026-09-07T02:00:00Z"),
                    exitAt = Instant.parse("2026-09-07T04:30:00Z"),
                    terminalName = "Basement",
                    vehicleTypeName = "Car",
                    ratePlanName = "Hourly",
                    feeAmount = BigDecimal("150.00"),
                    amountTendered = BigDecimal("200.00"),
                    changeGiven = BigDecimal("50.00"),
                    entryAttendantName = "Ana Reyes",
                    exitAttendantName = "Rico Delgado",
                ),
                printState = PrintState.Sent,
            ),
            onCashChange = {},
            onCashPreset = {},
            onCollect = {},
            onRetryQuote = {},
            onReprint = {},
            onFailureShown = {},
            onDone = {},
        )
    }
}
