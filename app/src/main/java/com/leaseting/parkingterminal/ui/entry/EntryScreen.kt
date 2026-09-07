package com.leaseting.parkingterminal.ui.entry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.leaseting.parkingterminal.R
import com.leaseting.parkingterminal.domain.parking.ParkingSession
import com.leaseting.parkingterminal.domain.parking.ParkingSessionStatus
import com.leaseting.parkingterminal.domain.parking.VehicleType
import com.leaseting.parkingterminal.ui.format.formatTicketCode
import com.leaseting.parkingterminal.ui.format.formatTime
import com.leaseting.parkingterminal.ui.parking.userMessage
import com.leaseting.parkingterminal.ui.print.PrintState
import com.leaseting.parkingterminal.ui.print.PrintStatusRow
import com.leaseting.parkingterminal.ui.theme.LeasetingParkingTerminalTheme
import com.leaseting.parkingterminal.ui.theme.extendedColors
import java.time.Instant

/**
 * Recording a vehicle in. Two fields and a button, because there is a queue
 * behind the one at the barrier.
 */
@Composable
fun EntryScreen(
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EntryViewModel = viewModel(factory = EntryViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    EntryScreen(
        uiState = uiState,
        onVehicleTypeSelected = viewModel::onVehicleTypeSelected,
        onPlateChange = viewModel::onPlateChange,
        onSubmit = viewModel::onSubmit,
        onReprint = viewModel::printStub,
        onNextVehicle = viewModel::onNextVehicle,
        onFailureShown = viewModel::onFailureShown,
        onDone = onDone,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EntryScreen(
    uiState: EntryUiState,
    onVehicleTypeSelected: (String) -> Unit,
    onPlateChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onReprint: () -> Unit,
    onNextVehicle: () -> Unit,
    onFailureShown: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val plateFocus = remember { FocusRequester() }
    val failureMessage = uiState.failure?.userMessage()

    LaunchedEffect(failureMessage) {
        if (failureMessage != null) {
            snackbarHostState.showSnackbar(failureMessage)
            onFailureShown()
        }
    }

    // The plate is the only thing that has to be typed, so the caret starts
    // there — both on arrival and after the form clears for the next vehicle.
    LaunchedEffect(uiState.recorded) {
        if (uiState.recorded == null) runCatching { plateFocus.requestFocus() }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.entry_title)) },
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
            val recorded = uiState.recorded
            if (recorded != null) {
                StubCard(
                    session = recorded,
                    printState = uiState.printState,
                    onReprint = onReprint,
                )
                Button(
                    onClick = onNextVehicle,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(BIG_BUTTON_HEIGHT),
                ) {
                    Text(
                        text = stringResource(R.string.entry_next_vehicle),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            } else {
                EntryForm(
                    uiState = uiState,
                    plateFocus = plateFocus,
                    onVehicleTypeSelected = onVehicleTypeSelected,
                    onPlateChange = onPlateChange,
                    onSubmit = onSubmit,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EntryForm(
    uiState: EntryUiState,
    plateFocus: FocusRequester,
    onVehicleTypeSelected: (String) -> Unit,
    onPlateChange: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = stringResource(R.string.entry_vehicle_type_label),
            style = MaterialTheme.typography.labelLarge,
        )
        if (uiState.isLoadingVehicleTypes) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp))
        } else {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                uiState.vehicleTypes.forEach { type ->
                    FilterChip(
                        selected = uiState.selectedVehicleTypeId == type.id,
                        onClick = { onVehicleTypeSelected(type.id) },
                        label = { Text(type.name) },
                        enabled = !uiState.isSubmitting,
                    )
                }
            }
        }

        OutlinedTextField(
            value = uiState.plateNumber,
            onValueChange = onPlateChange,
            label = { Text(stringResource(R.string.entry_plate_label)) },
            singleLine = true,
            enabled = !uiState.isSubmitting,
            textStyle = MaterialTheme.typography.headlineSmall,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Characters,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { onSubmit() }),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(plateFocus),
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
                    text = stringResource(R.string.entry_submit),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}

/**
 * The stub, on screen. The code is shown large and in the same grouping it is
 * printed in, so a guard can read it onto a hand-written slip when the printer
 * has nothing left to print on.
 */
@Composable
private fun StubCard(
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
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.entry_recorded),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.extendedColors.success,
            )
            Text(
                text = formatTicketCode(session.ticketCode),
                style = MaterialTheme.typography.headlineLarge,
                fontFamily = FontFamily.Monospace,
            )
            Text(text = session.plateNumber, style = MaterialTheme.typography.titleLarge)
            Text(
                text = "${session.vehicleTypeName} · ${formatTime(session.entryAt)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            PrintStatusRow(
                state = printState,
                onReprint = onReprint,
                reprintLabelRes = R.string.entry_reprint,
            )
        }
    }
}

private val BIG_BUTTON_HEIGHT = 64.dp

@Preview(showBackground = true)
@Composable
private fun EntryFormPreview() {
    LeasetingParkingTerminalTheme {
        EntryScreen(
            uiState = EntryUiState(
                isLoadingVehicleTypes = false,
                vehicleTypes = listOf(
                    VehicleType("1", "Car", false),
                    VehicleType("2", "Motorcycle", false),
                    VehicleType("3", "Truck", false),
                ),
                selectedVehicleTypeId = "1",
                plateNumber = "ABC 1234",
            ),
            onVehicleTypeSelected = {},
            onPlateChange = {},
            onSubmit = {},
            onReprint = {},
            onNextVehicle = {},
            onFailureShown = {},
            onDone = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EntryStubPreview() {
    LeasetingParkingTerminalTheme {
        EntryScreen(
            uiState = EntryUiState(
                isLoadingVehicleTypes = false,
                recorded = ParkingSession(
                    id = "s1",
                    ticketCode = "A3KD9MPQ",
                    plateNumber = "ABC 1234",
                    status = ParkingSessionStatus.OPEN,
                    entryAt = Instant.parse("2026-09-07T06:12:00Z"),
                    exitAt = null,
                    terminalName = "Basement",
                    vehicleTypeName = "Car",
                    ratePlanName = null,
                    feeAmount = null,
                    amountTendered = null,
                    changeGiven = null,
                ),
                printState = PrintState.Sent,
            ),
            onVehicleTypeSelected = {},
            onPlateChange = {},
            onSubmit = {},
            onReprint = {},
            onNextVehicle = {},
            onFailureShown = {},
            onDone = {},
        )
    }
}
