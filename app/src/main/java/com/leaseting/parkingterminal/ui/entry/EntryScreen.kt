package com.leaseting.parkingterminal.ui.entry

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.leaseting.parkingterminal.R
import com.leaseting.parkingterminal.domain.parking.ParkingSession
import com.leaseting.parkingterminal.domain.parking.ParkingSessionStatus
import com.leaseting.parkingterminal.domain.parking.VehicleType
import com.leaseting.parkingterminal.ui.components.CardDivider
import com.leaseting.parkingterminal.ui.components.CardTone
import com.leaseting.parkingterminal.ui.components.ConfirmationHeader
import com.leaseting.parkingterminal.ui.components.EyebrowLabel
import com.leaseting.parkingterminal.ui.components.PrimaryAction
import com.leaseting.parkingterminal.ui.components.ScreenColumn
import com.leaseting.parkingterminal.ui.components.SectionCard
import com.leaseting.parkingterminal.ui.components.TerminalBarAction
import com.leaseting.parkingterminal.ui.components.TerminalScaffold
import com.leaseting.parkingterminal.ui.format.formatTicketCode
import com.leaseting.parkingterminal.ui.format.formatTime
import com.leaseting.parkingterminal.ui.parking.userMessage
import com.leaseting.parkingterminal.ui.print.PrintState
import com.leaseting.parkingterminal.ui.print.PrintStatusRow
import com.leaseting.parkingterminal.ui.theme.LeasetingParkingTerminalTheme
import com.leaseting.parkingterminal.ui.theme.Sizes
import com.leaseting.parkingterminal.ui.theme.Spacing
import com.leaseting.parkingterminal.ui.theme.motion
import com.leaseting.parkingterminal.ui.theme.swap
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
    val screenSwap = motion.swap()

    LaunchedEffect(failureMessage) {
        if (failureMessage != null) {
            snackbarHostState.showSnackbar(failureMessage)
            onFailureShown()
        }
    }

    // The plate is the only thing that has to be typed, so the caret starts
    // there, both on arrival and after the form clears for the next vehicle.
    LaunchedEffect(uiState.recorded) {
        if (uiState.recorded == null) runCatching { plateFocus.requestFocus() }
    }

    TerminalScaffold(
        title = stringResource(R.string.entry_title),
        snackbarHostState = snackbarHostState,
        modifier = modifier,
        actions = {
            TerminalBarAction(text = stringResource(R.string.action_done), onClick = onDone)
        },
    ) { innerPadding ->
        ScreenColumn(innerPadding) {
            // The form giving way to the stub is the only thing that happens on
            // this screen, so it is the one place worth animating.
            AnimatedContent(
                targetState = uiState.recorded,
                transitionSpec = { screenSwap },
                contentKey = { it != null },
                label = "entry",
            ) { recorded ->
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.Gutter)) {
                    if (recorded != null) {
                        StubCard(
                            session = recorded,
                            printState = uiState.printState,
                            onReprint = onReprint,
                        )
                        PrimaryAction(
                            text = stringResource(R.string.entry_next_vehicle),
                            onClick = onNextVehicle,
                        )
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
    }
}

@Composable
private fun EntryForm(
    uiState: EntryUiState,
    plateFocus: FocusRequester,
    onVehicleTypeSelected: (String) -> Unit,
    onPlateChange: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Spacing.Gutter)) {
        SectionCard(verticalSpacing = Spacing.Medium) {
            EyebrowLabel(stringResource(R.string.entry_vehicle_type_label))
            if (uiState.isLoadingVehicleTypes) {
                CircularProgressIndicator(
                    modifier = Modifier.size(Sizes.Spinner),
                    strokeWidth = 2.dp,
                )
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
                    verticalArrangement = Arrangement.spacedBy(Spacing.Small),
                ) {
                    uiState.vehicleTypes.forEach { type ->
                        FilterChip(
                            selected = uiState.selectedVehicleTypeId == type.id,
                            onClick = { onVehicleTypeSelected(type.id) },
                            label = {
                                Text(
                                    text = type.name,
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            },
                            enabled = !uiState.isSubmitting,
                            shape = MaterialTheme.shapes.small,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor =
                                    MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor =
                                    MaterialTheme.colorScheme.onPrimaryContainer,
                            ),
                            modifier = Modifier.height(Sizes.MinimumTouchTarget),
                        )
                    }
                }
            }

            CardDivider()

            OutlinedTextField(
                value = uiState.plateNumber,
                onValueChange = onPlateChange,
                label = { Text(stringResource(R.string.entry_plate_label)) },
                singleLine = true,
                enabled = !uiState.isSubmitting,
                shape = MaterialTheme.shapes.small,
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
        }

        PrimaryAction(
            text = stringResource(R.string.entry_submit),
            onClick = onSubmit,
            enabled = uiState.canSubmit,
            isBusy = uiState.isSubmitting,
        )
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
    SectionCard(modifier = modifier, tone = CardTone.Confirmed) {
        ConfirmationHeader(stringResource(R.string.entry_recorded))
        CardDivider()

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Spacing.Small),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.Tight),
        ) {
            EyebrowLabel(
                text = stringResource(R.string.entry_stub_code),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = formatTicketCode(session.ticketCode),
                style = MaterialTheme.typography.headlineLarge,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center,
            )
            Text(
                text = session.plateNumber,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "${session.vehicleTypeName} · ${formatTime(session.entryAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }

        CardDivider()
        PrintStatusRow(
            state = printState,
            onReprint = onReprint,
            reprintLabelRes = R.string.entry_reprint,
        )
    }
}

@Preview(showBackground = true, heightDp = 780)
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

@Preview(showBackground = true, heightDp = 780)
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
                    entryAttendantName = "Ana Reyes",
                    exitAttendantName = null,
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
