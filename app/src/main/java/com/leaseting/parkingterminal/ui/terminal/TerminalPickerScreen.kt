package com.leaseting.parkingterminal.ui.terminal

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import com.leaseting.parkingterminal.ui.components.CardTone
import com.leaseting.parkingterminal.ui.components.EmptyState
import com.leaseting.parkingterminal.ui.components.EyebrowLabel
import com.leaseting.parkingterminal.ui.components.LoadingState
import com.leaseting.parkingterminal.ui.components.SectionCard
import com.leaseting.parkingterminal.ui.components.TerminalBarAction
import com.leaseting.parkingterminal.ui.components.TerminalScaffold
import com.leaseting.parkingterminal.ui.parking.userMessage
import com.leaseting.parkingterminal.ui.theme.LeasetingParkingTerminalTheme
import com.leaseting.parkingterminal.ui.theme.Sizes
import com.leaseting.parkingterminal.ui.theme.Spacing
import com.leaseting.parkingterminal.ui.theme.motion

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
    val standard = motion.standard

    LaunchedEffect(failureMessage) {
        if (failureMessage != null) {
            snackbarHostState.showSnackbar(failureMessage)
            onFailureShown()
        }
    }

    TerminalScaffold(
        title = stringResource(R.string.terminal_picker_title),
        snackbarHostState = snackbarHostState,
        modifier = modifier,
        actions = {
            TerminalBarAction(
                text = stringResource(R.string.action_sign_out),
                onClick = onSignOut,
            )
        },
    ) { innerPadding ->
        // One state at a time, faded rather than swapped, so a slow network does
        // not flash a spinner and a list at whoever is watching.
        Crossfade(
            targetState = uiState.stage,
            animationSpec = tween(standard),
            label = "terminal-picker",
            modifier = Modifier.padding(innerPadding),
        ) { stage ->
            when (stage) {
                Stage.Loading -> LoadingState(stringResource(R.string.terminal_picker_loading))

                Stage.Empty -> EmptyState(
                    title = stringResource(R.string.terminal_picker_empty_title),
                    body = stringResource(R.string.terminal_picker_empty_body),
                    actionText = stringResource(R.string.action_retry),
                    onAction = onRetry,
                )

                Stage.Terminals -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(Spacing.Gutter),
                    verticalArrangement = Arrangement.spacedBy(Spacing.Medium),
                ) {
                    item(key = "intro") {
                        Column(
                            modifier = Modifier.padding(bottom = Spacing.Tight),
                            verticalArrangement = Arrangement.spacedBy(Spacing.Tight),
                        ) {
                            EyebrowLabel(stringResource(R.string.terminal_picker_section))
                            Text(
                                text = stringResource(R.string.terminal_picker_subtitle),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    items(uiState.terminals, key = { it.id }) { terminal ->
                        TerminalCard(
                            terminal = terminal,
                            isOpening = uiState.openingTerminalId == terminal.id,
                            isEnabled = uiState.openingTerminalId == null,
                            onClick = { onTerminalSelected(terminal.id) },
                            modifier = Modifier.animateItem(),
                        )
                    }
                }
            }
        }
    }
}

/**
 * A gate, and what tapping it will do. The action is spelled out rather than
 * implied by an arrow: opening a shift is a commitment, and it should read like
 * one before it is tapped.
 */
@Composable
private fun TerminalCard(
    terminal: ParkingTerminal,
    isOpening: Boolean,
    isEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SectionCard(
        modifier = modifier.clickable(enabled = isEnabled, onClick = onClick),
        tone = if (isOpening) CardTone.Brand else CardTone.Plain,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.Gutter),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Spacing.Hair),
            ) {
                Text(text = terminal.name, style = MaterialTheme.typography.titleLarge)
                Text(
                    text = terminal.propertyName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (isOpening) {
                CircularProgressIndicator(
                    modifier = Modifier.size(Sizes.Spinner),
                    strokeWidth = 2.dp,
                )
            } else {
                Text(
                    text = stringResource(R.string.terminal_picker_open),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

/**
 * The three things this screen can be showing. Naming them keeps the crossfade
 * from restarting every time the list is re-emitted unchanged.
 */
private enum class Stage { Loading, Empty, Terminals }

private val TerminalPickerUiState.stage: Stage
    get() = when {
        isLoading -> Stage.Loading
        terminals.isEmpty() -> Stage.Empty
        else -> Stage.Terminals
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

@Preview(showBackground = true)
@Composable
private fun TerminalPickerEmptyPreview() {
    LeasetingParkingTerminalTheme {
        TerminalPickerScreen(
            uiState = TerminalPickerUiState(isLoading = false),
            onTerminalSelected = {},
            onRetry = {},
            onFailureShown = {},
            onSignOut = {},
        )
    }
}
