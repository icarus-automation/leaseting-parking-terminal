package com.leaseting.parkingterminal.ui.exit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.SuggestionChip
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.leaseting.parkingterminal.R
import com.leaseting.parkingterminal.domain.parking.FeeQuote
import com.leaseting.parkingterminal.domain.parking.ParkingSession
import com.leaseting.parkingterminal.domain.parking.ParkingSessionStatus
import com.leaseting.parkingterminal.ui.format.formatAmount
import com.leaseting.parkingterminal.ui.format.formatDuration
import com.leaseting.parkingterminal.ui.format.formatTicketCode
import com.leaseting.parkingterminal.ui.format.formatTime
import com.leaseting.parkingterminal.ui.parking.userMessage
import com.leaseting.parkingterminal.ui.print.PrintState
import com.leaseting.parkingterminal.ui.print.PrintStatusRow
import com.leaseting.parkingterminal.ui.theme.LeasetingParkingTerminalTheme
import com.leaseting.parkingterminal.ui.theme.extendedColors
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

@OptIn(ExperimentalMaterial3Api::class)
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
                title = { Text(stringResource(R.string.exit_title)) },
                actions = {
                    TextButton(onClick = onDone) { Text(stringResource(R.string.action_done)) }
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
            val paid = uiState.paid
            when {
                paid != null -> {
                    ReceiptCard(
                        session = paid,
                        printState = uiState.printState,
                        onReprint = onReprint,
                    )
                    Button(
                        onClick = onDone,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(BIG_BUTTON_HEIGHT),
                    ) {
                        Text(
                            text = stringResource(R.string.exit_next_vehicle),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }

                uiState.isLoadingQuote -> LoadingQuote()

                uiState.quote == null -> QuoteUnavailable(onRetry = onRetryQuote)

                else -> CollectForm(
                    quote = uiState.quote,
                    uiState = uiState,
                    onCashChange = onCashChange,
                    onCashPreset = onCashPreset,
                    onCollect = onCollect,
                )
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
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        FeeCard(quote)

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
            textStyle = MaterialTheme.typography.headlineSmall,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Done,
            ),
            modifier = Modifier.fillMaxWidth(),
        )

        // The notes a driver actually hands over at a Manila gate, plus the
        // exact fee for the common case where they have counted it out.
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SuggestionChip(
                onClick = { onCashPreset(quote.feeAmount) },
                label = { Text(stringResource(R.string.exit_cash_exact)) },
            )
            CASH_PRESETS
                .filter { it > quote.feeAmount }
                .forEach { amount ->
                    SuggestionChip(
                        onClick = { onCashPreset(amount) },
                        label = { Text(formatAmount(amount)) },
                    )
                }
        }

        AmountRow(
            label = stringResource(R.string.exit_change_label),
            value = formatAmount(uiState.change ?: BigDecimal.ZERO),
            emphasise = true,
        )

        Button(
            onClick = onCollect,
            enabled = uiState.canCollect,
            modifier = Modifier
                .fillMaxWidth()
                .height(BIG_BUTTON_HEIGHT),
        ) {
            if (uiState.isCollecting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                Text(
                    text = stringResource(R.string.exit_collect),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}

@Composable
private fun FeeCard(quote: FeeQuote, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(text = quote.plateNumber, style = MaterialTheme.typography.headlineSmall)
            Text(
                text = "${formatTicketCode(quote.ticketCode)} · ${quote.vehicleTypeName}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = FontFamily.Monospace,
            )
            HorizontalDivider()
            DetailRow(stringResource(R.string.exit_detail_in), formatTime(quote.entryAt))
            DetailRow(stringResource(R.string.exit_detail_stay), formatDuration(quote.stayMinutes))
            if (quote.graceMinutes > 0) {
                DetailRow(
                    stringResource(R.string.exit_detail_grace),
                    formatDuration(quote.graceMinutes),
                )
            }
            DetailRow(stringResource(R.string.exit_detail_rate), quote.ratePlanName)
            HorizontalDivider()
            AmountRow(
                label = stringResource(R.string.exit_fee_label),
                value = formatAmount(quote.feeAmount),
                emphasise = true,
            )
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String, modifier: Modifier = Modifier) {
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

@Composable
private fun AmountRow(
    label: String,
    value: String,
    emphasise: Boolean,
    modifier: Modifier = Modifier,
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
        )
    }
}

@Composable
private fun ReceiptCard(
    session: ParkingSession,
    printState: PrintState,
    onReprint: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.exit_collected),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.extendedColors.success,
            )
            Text(text = session.plateNumber, style = MaterialTheme.typography.headlineSmall)
            HorizontalDivider()
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
            AmountRow(
                label = stringResource(R.string.exit_change_label),
                value = formatAmount(session.changeGiven ?: BigDecimal.ZERO),
                emphasise = true,
            )
            PrintStatusRow(
                state = printState,
                onReprint = onReprint,
                reprintLabelRes = R.string.exit_reprint,
            )
        }
    }
}

@Composable
private fun LoadingQuote(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CircularProgressIndicator()
        Text(
            text = stringResource(R.string.exit_pricing),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun QuoteUnavailable(onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.exit_no_quote),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(onClick = onRetry) { Text(stringResource(R.string.action_retry)) }
    }
}

private val BIG_BUTTON_HEIGHT = 64.dp

private val CASH_PRESETS = listOf(
    BigDecimal("20"),
    BigDecimal("50"),
    BigDecimal("100"),
    BigDecimal("200"),
    BigDecimal("500"),
    BigDecimal("1000"),
)

@Preview(showBackground = true)
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

@Preview(showBackground = true)
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
