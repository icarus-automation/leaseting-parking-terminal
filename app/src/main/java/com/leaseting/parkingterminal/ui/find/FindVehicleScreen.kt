package com.leaseting.parkingterminal.ui.find

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import com.leaseting.parkingterminal.ui.format.formatElapsed
import com.leaseting.parkingterminal.ui.format.formatTicketCode
import com.leaseting.parkingterminal.ui.format.formatTime
import com.leaseting.parkingterminal.ui.parking.userMessage
import com.leaseting.parkingterminal.ui.theme.LeasetingParkingTerminalTheme
import java.time.Instant

/**
 * Finding the vehicle at the barrier: scan or type its stub code, or pick it
 * out of what is still in the park.
 */
@Composable
fun FindVehicleScreen(
    onSessionSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FindVehicleViewModel = viewModel(factory = FindVehicleViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.resolvedSessionId) {
        uiState.resolvedSessionId?.let { sessionId ->
            viewModel.onSessionNavigated()
            onSessionSelected(sessionId)
        }
    }

    FindVehicleScreen(
        uiState = uiState,
        onTicketCodeChange = viewModel::onTicketCodeChange,
        onSubmitCode = viewModel::onSubmitCode,
        onPlateQueryChange = viewModel::onPlateQueryChange,
        onSessionSelected = onSessionSelected,
        onSettledCodeShown = viewModel::onSettledCodeShown,
        onFailureShown = viewModel::onFailureShown,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FindVehicleScreen(
    uiState: FindVehicleUiState,
    onTicketCodeChange: (String) -> Unit,
    onSubmitCode: () -> Unit,
    onPlateQueryChange: (String) -> Unit,
    onSessionSelected: (String) -> Unit,
    onSettledCodeShown: () -> Unit,
    onFailureShown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val codeFocus = remember { FocusRequester() }
    val failureMessage = uiState.failure?.userMessage()
    val settledMessage = uiState.settledCode
        ?.let { stringResource(R.string.find_code_already_settled, formatTicketCode(it)) }

    LaunchedEffect(failureMessage) {
        if (failureMessage != null) {
            snackbarHostState.showSnackbar(failureMessage)
            onFailureShown()
        }
    }

    LaunchedEffect(settledMessage) {
        if (settledMessage != null) {
            snackbarHostState.showSnackbar(settledMessage)
            onSettledCodeShown()
        }
    }

    // The code box holds focus by default. A handheld scanner that behaves as a
    // keyboard needs somewhere for its characters to land, and this is it.
    LaunchedEffect(Unit) { runCatching { codeFocus.requestFocus() } }

    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(stringResource(R.string.find_title)) }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = uiState.ticketCode,
                    onValueChange = onTicketCodeChange,
                    label = { Text(stringResource(R.string.find_code_label)) },
                    supportingText = { Text(stringResource(R.string.find_code_hint)) },
                    singleLine = true,
                    enabled = !uiState.isResolvingCode,
                    textStyle = MaterialTheme.typography.headlineSmall.copy(
                        fontFamily = FontFamily.Monospace,
                    ),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        imeAction = ImeAction.Search,
                    ),
                    // A wedge scanner ends its scan with Enter, which lands here.
                    keyboardActions = KeyboardActions(onSearch = { onSubmitCode() }),
                    trailingIcon = {
                        if (uiState.isResolvingCode) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(codeFocus),
                )

                OutlinedTextField(
                    value = uiState.plateQuery,
                    onValueChange = onPlateQueryChange,
                    label = { Text(stringResource(R.string.find_plate_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        imeAction = ImeAction.Search,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            HorizontalDivider()

            when {
                uiState.isLoading && uiState.sessions.isEmpty() -> Loading()

                uiState.sessions.isEmpty() -> EmptyList(
                    isSearching = uiState.plateQuery.isNotBlank(),
                )

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(uiState.sessions, key = { it.id }) { session ->
                        SessionRow(
                            session = session,
                            onClick = { onSessionSelected(session.id) },
                        )
                    }
                }
            }
        }
    }
}

/**
 * One vehicle still in the park. The terminal it entered at is on the row
 * because a plate search reaches across gates, and a guard needs to know they
 * are looking at a vehicle that came in somewhere else.
 */
@Composable
private fun SessionRow(
    session: ParkingSession,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = session.plateNumber, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "${formatTicketCode(session.ticketCode)} · ${session.vehicleTypeName}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = FontFamily.Monospace,
                )
                Text(
                    text = "${session.terminalName} · ${formatTime(session.entryAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = formatElapsed(session.entryAt),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Loading(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun EmptyList(isSearching: Boolean, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
    ) {
        Text(
            text = stringResource(
                if (isSearching) R.string.find_empty_search else R.string.find_empty_list,
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun FindVehiclePreview() {
    LeasetingParkingTerminalTheme {
        FindVehicleScreen(
            uiState = FindVehicleUiState(
                isLoading = false,
                sessions = listOf(
                    ParkingSession(
                        id = "s1",
                        ticketCode = "A3KD9MPQ",
                        plateNumber = "ABC 1234",
                        status = ParkingSessionStatus.OPEN,
                        entryAt = Instant.now().minusSeconds(4500),
                        exitAt = null,
                        terminalName = "Basement",
                        vehicleTypeName = "Car",
                        ratePlanName = null,
                        feeAmount = null,
                        amountTendered = null,
                        changeGiven = null,
                    ),
                ),
            ),
            onTicketCodeChange = {},
            onSubmitCode = {},
            onPlateQueryChange = {},
            onSessionSelected = {},
            onSettledCodeShown = {},
            onFailureShown = {},
        )
    }
}
