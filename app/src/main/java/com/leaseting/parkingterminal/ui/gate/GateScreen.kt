package com.leaseting.parkingterminal.ui.gate

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.leaseting.parkingterminal.R
import com.leaseting.parkingterminal.domain.auth.Attendant
import com.leaseting.parkingterminal.domain.parking.ParkingShift
import com.leaseting.parkingterminal.domain.parking.ParkingShiftStatus
import com.leaseting.parkingterminal.domain.parking.ShiftSummary
import com.leaseting.parkingterminal.ui.format.formatAmount
import com.leaseting.parkingterminal.ui.format.formatTime
import com.leaseting.parkingterminal.ui.parking.userMessage
import com.leaseting.parkingterminal.ui.theme.LeasetingParkingTerminalTheme
import com.leaseting.parkingterminal.ui.theme.extendedColors
import java.math.BigDecimal
import java.time.Instant

/**
 * The working gate. Everything a guard does during a shift starts here, and the
 * running totals for the till sit above the buttons so the number they will be
 * asked to count is never a surprise at the end.
 */
@Composable
fun GateScreen(
    attendant: Attendant,
    shift: ParkingShift,
    onRecordEntry: () -> Unit,
    onVehicleOut: () -> Unit,
    onEndShift: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GateViewModel = viewModel(factory = GateViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Re-read on every return to this screen: coming back from an entry or an
    // exit is precisely when the totals have moved.
    LaunchedEffect(shift.id) { viewModel.refresh() }

    GateScreen(
        attendant = attendant,
        shift = shift,
        uiState = uiState,
        onRecordEntry = onRecordEntry,
        onVehicleOut = onVehicleOut,
        onEndShift = onEndShift,
        onSignOut = onSignOut,
        onFailureShown = viewModel::onFailureShown,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GateScreen(
    attendant: Attendant,
    shift: ParkingShift,
    uiState: GateUiState,
    onRecordEntry: () -> Unit,
    onVehicleOut: () -> Unit,
    onEndShift: () -> Unit,
    onSignOut: () -> Unit,
    onFailureShown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isConfirmingSignOut by remember { mutableStateOf(false) }
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
                title = { Text(shift.terminalName) },
                actions = {
                    TextButton(onClick = { isConfirmingSignOut = true }) {
                        Text(stringResource(R.string.action_sign_out))
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
            ShiftCard(shift = shift, attendant = attendant)
            TillCard(summary = uiState.summary)

            Button(
                onClick = onRecordEntry,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(BIG_BUTTON_HEIGHT),
            ) {
                Text(
                    text = stringResource(R.string.gate_action_entry),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Button(
                onClick = onVehicleOut,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(BIG_BUTTON_HEIGHT),
            ) {
                Text(
                    text = stringResource(R.string.gate_action_exit),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            OutlinedButton(onClick = onEndShift, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.gate_action_end_shift))
            }
        }
    }

    if (isConfirmingSignOut) {
        SignOutDialog(
            onConfirm = {
                isConfirmingSignOut = false
                onSignOut()
            },
            onDismiss = { isConfirmingSignOut = false },
        )
    }
}

@Composable
private fun ShiftCard(shift: ParkingShift, attendant: Attendant, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = shift.propertyName,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.gate_shift_opened, formatTime(shift.openedAt)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.gate_attendant, attendant.name),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Cash owed, next to what produced it. Shown as "expected" everywhere so a
 * guard never reads it as a figure the count has to be forced to match.
 */
@Composable
private fun TillCard(summary: ShiftSummary?, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.gate_till_title),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = formatAmount(summary?.expectedCash ?: BigDecimal.ZERO),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.extendedColors.success,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Stat(
                    label = stringResource(R.string.gate_stat_entries),
                    value = (summary?.entryCount ?: 0).toString(),
                )
                Stat(
                    label = stringResource(R.string.gate_stat_exits),
                    value = (summary?.paidExitCount ?: 0).toString(),
                )
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(text = value, style = MaterialTheme.typography.titleLarge)
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Start,
        )
    }
}

@Composable
private fun SignOutDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.sign_out_title)) },
        // Signing out leaves the shift open on purpose: the till is still that
        // guard's, and they come back to it when they sign in again.
        text = { Text(stringResource(R.string.gate_sign_out_body)) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(R.string.action_sign_out)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

private val BIG_BUTTON_HEIGHT = 72.dp

@Preview(showBackground = true)
@Composable
private fun GateScreenPreview() {
    LeasetingParkingTerminalTheme {
        GateScreen(
            attendant = Attendant("1", "Guard One", "guard@leaseting.com"),
            shift = ParkingShift(
                id = "s1",
                status = ParkingShiftStatus.OPEN,
                openedAt = Instant.parse("2026-09-07T06:00:00Z"),
                terminalId = "t1",
                terminalName = "Basement",
                propertyName = "Brickstone",
                expectedCash = null,
                declaredCash = null,
            ),
            uiState = GateUiState(
                summary = ShiftSummary("s1", entryCount = 24, paidExitCount = 19, expectedCash = BigDecimal("1850.00")),
            ),
            onRecordEntry = {},
            onVehicleOut = {},
            onEndShift = {},
            onSignOut = {},
            onFailureShown = {},
        )
    }
}
