package com.leaseting.parkingterminal.ui.terminal

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
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.leaseting.parkingterminal.R
import com.leaseting.parkingterminal.domain.parking.ParkingTerminal
import com.leaseting.parkingterminal.ui.parking.userMessage
import com.leaseting.parkingterminal.ui.theme.LeasetingParkingTerminalTheme

/**
 * Where a signed-in guard lands with no shift open. Picking a gate opens the
 * shift; the navigation graph does the rest.
 */
@Composable
fun TerminalPickerScreen(
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TerminalPickerViewModel = viewModel(factory = TerminalPickerViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    TerminalPickerScreen(
        uiState = uiState,
        onTerminalSelected = viewModel::onTerminalSelected,
        onRetry = viewModel::load,
        onFailureShown = viewModel::onFailureShown,
        onSignOut = onSignOut,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TerminalPickerScreen(
    uiState: TerminalPickerUiState,
    onTerminalSelected: (String) -> Unit,
    onRetry: () -> Unit,
    onFailureShown: () -> Unit,
    onSignOut: () -> Unit,
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
                title = { Text(stringResource(R.string.terminal_picker_title)) },
                actions = {
                    TextButton(onClick = onSignOut) {
                        Text(stringResource(R.string.action_sign_out))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        when {
            uiState.isLoading -> LoadingTerminals(Modifier.padding(innerPadding))

            uiState.terminals.isEmpty() -> NoTerminals(
                onRetry = onRetry,
                modifier = Modifier.padding(innerPadding),
            )

            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Text(
                        text = stringResource(R.string.terminal_picker_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                items(uiState.terminals, key = { it.id }) { terminal ->
                    TerminalCard(
                        terminal = terminal,
                        isOpening = uiState.openingTerminalId == terminal.id,
                        isEnabled = uiState.openingTerminalId == null,
                        onClick = { onTerminalSelected(terminal.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun TerminalCard(
    terminal: ParkingTerminal,
    isOpening: Boolean,
    isEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = isEnabled, onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = terminal.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = terminal.propertyName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (isOpening) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            }
        }
    }
}

@Composable
private fun LoadingTerminals(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        CircularProgressIndicator()
        Text(
            text = stringResource(R.string.terminal_picker_loading),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Covers both "the org has none" and "the call failed", because from the gate
 * they look identical and the answer to both is the same: ask someone to check
 * the web client, then try again.
 */
@Composable
private fun NoTerminals(onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
    ) {
        Text(
            text = stringResource(R.string.terminal_picker_empty_title),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = stringResource(R.string.terminal_picker_empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(onClick = onRetry) { Text(stringResource(R.string.action_retry)) }
    }
}

@Preview(showBackground = true)
@Composable
private fun TerminalPickerPreview() {
    LeasetingParkingTerminalTheme {
        TerminalPickerScreen(
            uiState = TerminalPickerUiState(
                isLoading = false,
                terminals = listOf(
                    ParkingTerminal("1", "Basement", true, "p1", "Brickstone", false),
                    ParkingTerminal("2", "Podium", true, "p1", "Brickstone", false),
                ),
            ),
            onTerminalSelected = {},
            onRetry = {},
            onFailureShown = {},
            onSignOut = {},
        )
    }
}
