package com.leaseting.parkingterminal.ui.gate

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.leaseting.parkingterminal.R
import com.leaseting.parkingterminal.domain.auth.Attendant
import com.leaseting.parkingterminal.domain.parking.ParkingShift
import com.leaseting.parkingterminal.domain.parking.ParkingShiftStatus
import com.leaseting.parkingterminal.domain.parking.ShiftSummary
import com.leaseting.parkingterminal.ui.components.CardDivider
import com.leaseting.parkingterminal.ui.components.CardTone
import com.leaseting.parkingterminal.ui.components.EyebrowLabel
import com.leaseting.parkingterminal.ui.components.PrimaryAction
import com.leaseting.parkingterminal.ui.components.ScreenColumn
import com.leaseting.parkingterminal.ui.components.SecondaryAction
import com.leaseting.parkingterminal.ui.components.SectionCard
import com.leaseting.parkingterminal.ui.components.TerminalBarAction
import com.leaseting.parkingterminal.ui.components.TerminalScaffold
import com.leaseting.parkingterminal.ui.format.formatAmount
import com.leaseting.parkingterminal.ui.format.formatTime
import com.leaseting.parkingterminal.ui.parking.userMessage
import com.leaseting.parkingterminal.ui.theme.LeasetingParkingTerminalTheme
import com.leaseting.parkingterminal.ui.theme.Spacing
import com.leaseting.parkingterminal.ui.theme.motion
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

    TerminalScaffold(
        title = shift.terminalName,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
        actions = {
            TerminalBarAction(
                text = stringResource(R.string.action_sign_out),
                onClick = { isConfirmingSignOut = true },
            )
        },
    ) { innerPadding ->
        ScreenColumn(innerPadding) {
            ShiftHeader(shift = shift, attendant = attendant)
            TillCard(summary = uiState.summary)

            Spacer(Modifier.height(Spacing.Tight))

            PrimaryAction(
                text = stringResource(R.string.gate_action_entry),
                onClick = onRecordEntry,
            )
            PrimaryAction(
                text = stringResource(R.string.gate_action_exit),
                onClick = onVehicleOut,
            )
            SecondaryAction(
                text = stringResource(R.string.gate_action_end_shift),
                onClick = onEndShift,
            )
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

/**
 * Whose shift this is and where, as a page heading rather than a card. It is
 * context for everything below it, so it should not compete with the till for
 * a card of its own.
 */
@Composable
private fun ShiftHeader(
    shift: ParkingShift,
    attendant: Attendant,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.Tight),
    ) {
        Text(
            text = shift.propertyName,
            style = MaterialTheme.typography.titleLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = stringResource(R.string.gate_shift_opened, formatTime(shift.openedAt)),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(R.string.gate_attendant, attendant.name),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Cash owed, next to what produced it. Shown as "expected" everywhere so a
 * guard never reads it as a figure the count has to be forced to match.
 *
 * The figure crossfades when it moves. Coming back from a collection, that is
 * the confirmation that the money landed on the shift, without a banner having
 * to say so.
 */
@Composable
private fun TillCard(summary: ShiftSummary?, modifier: Modifier = Modifier) {
    val expected = formatAmount(summary?.expectedCash ?: BigDecimal.ZERO)
    val standard = motion.standard

    SectionCard(modifier = modifier, tone = CardTone.Brand, verticalSpacing = Spacing.Small) {
        EyebrowLabel(
            text = stringResource(R.string.gate_till_title),
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        AnimatedContent(
            targetState = expected,
            transitionSpec = { fadeIn(tween(standard)) togetherWith fadeOut(tween(standard)) },
            label = "expected-cash",
        ) { amount ->
            Text(text = amount, style = MaterialTheme.typography.displaySmall)
        }
        CardDivider()
        Row(
            modifier = Modifier.padding(top = Spacing.Tight),
            horizontalArrangement = Arrangement.spacedBy(Spacing.Loose),
        ) {
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

@Composable
private fun Stat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.width(STAT_WIDTH),
        verticalArrangement = Arrangement.spacedBy(Spacing.Hair),
    ) {
        Text(text = value, style = MaterialTheme.typography.headlineSmall)
        EyebrowLabel(
            text = label,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

@Composable
private fun SignOutDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.large,
        title = {
            Text(
                text = stringResource(R.string.sign_out_title),
                style = MaterialTheme.typography.titleLarge,
            )
        },
        // Signing out leaves the shift open on purpose: the till is still that
        // guard's, and they come back to it when they sign in again.
        text = {
            Text(
                text = stringResource(R.string.gate_sign_out_body),
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(R.string.action_sign_out)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

/** Wide enough for four digits, so the two stats stay on the same grid. */
private val STAT_WIDTH = 84.dp

@Preview(showBackground = true, heightDp = 780)
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
                summary = ShiftSummary(
                    shiftId = "s1",
                    entryCount = 24,
                    paidExitCount = 19,
                    expectedCash = BigDecimal("1850.00"),
                ),
            ),
            onRecordEntry = {},
            onVehicleOut = {},
            onEndShift = {},
            onSignOut = {},
            onFailureShown = {},
        )
    }
}
