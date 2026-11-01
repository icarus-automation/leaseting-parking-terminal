package com.leaseting.parkingterminal.ui.find

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.leaseting.parkingterminal.R
import com.leaseting.parkingterminal.domain.parking.ParkingSession
import com.leaseting.parkingterminal.domain.parking.ParkingSessionStatus
import com.leaseting.parkingterminal.ui.components.EmptyState
import com.leaseting.parkingterminal.ui.components.EyebrowLabel
import com.leaseting.parkingterminal.ui.components.LoadingState
import com.leaseting.parkingterminal.ui.components.SectionCard
import com.leaseting.parkingterminal.ui.components.TerminalScaffold
import com.leaseting.parkingterminal.ui.format.formatElapsed
import com.leaseting.parkingterminal.ui.format.formatTicketCode
import com.leaseting.parkingterminal.ui.format.formatTime
import com.leaseting.parkingterminal.ui.parking.userMessage
import com.leaseting.parkingterminal.ui.theme.LeasetingParkingTerminalTheme
import com.leaseting.parkingterminal.ui.theme.Sizes
import com.leaseting.parkingterminal.ui.theme.Spacing
import com.leaseting.parkingterminal.ui.theme.motion
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
    val standard = motion.standard
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

    TerminalScaffold(
        title = stringResource(R.string.find_title),
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            // The search panel keeps the surface colour so it reads as part of
            // the bar above it, and the list below sits on the tinted page.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(Spacing.Gutter),
                verticalArrangement = Arrangement.spacedBy(Spacing.Medium),
            ) {
                OutlinedTextField(
                    value = uiState.ticketCode,
                    onValueChange = onTicketCodeChange,
                    label = { Text(stringResource(R.string.find_code_label)) },
                    supportingText = { Text(stringResource(R.string.find_code_hint)) },
                    singleLine = true,
                    enabled = !uiState.isResolvingCode,
                    shape = MaterialTheme.shapes.small,
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
                            CircularProgressIndicator(
                                modifier = Modifier.size(Sizes.SmallSpinner),
                                strokeWidth = 2.dp,
                            )
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
                    shape = MaterialTheme.shapes.small,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        imeAction = ImeAction.Search,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Crossfade(
                targetState = uiState.stage,
                animationSpec = tween(standard),
                label = "in-park",
            ) { stage ->
                when (stage) {
                    Stage.Loading -> LoadingState(stringResource(R.string.find_loading))

                    Stage.Empty -> EmptyState(
                        title = stringResource(R.string.find_empty_list),
                        body = stringResource(R.string.find_empty_list_body),
                    )

                    Stage.NoMatch -> EmptyState(
                        title = stringResource(R.string.find_empty_search),
                        body = stringResource(R.string.find_empty_search_body),
                    )

                    Stage.Sessions -> LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(Spacing.Gutter),
                        verticalArrangement = Arrangement.spacedBy(Spacing.Small),
                    ) {
                        item(key = "section") {
                            EyebrowLabel(
                                text = stringResource(R.string.find_section_in_park),
                                modifier = Modifier.padding(bottom = Spacing.Small),
                            )
                        }
                        items(uiState.sessions, key = { it.id }) { session ->
                            SessionRow(
                                session = session,
                                onClick = { onSessionSelected(session.id) },
                                modifier = Modifier.animateItem(),
                            )
                        }
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
    SectionCard(
        modifier = modifier.clickable(onClick = onClick),
        contentPadding = Spacing.Medium,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.Medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Spacing.Hair),
            ) {
                Text(text = session.plateNumber, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "${formatTicketCode(session.ticketCode)} · ${session.vehicleTypeName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = FontFamily.Monospace,
                )
                Text(
                    text = "${session.terminalName} · ${formatTime(session.entryAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            ElapsedBadge(formatElapsed(session.entryAt))
        }
    }
}

/** How long this one has been in. The number a guard scans the list for. */
@Composable
private fun ElapsedBadge(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.small,
            )
            .padding(horizontal = Spacing.Small, vertical = Spacing.Tight),
    )
}

/**
 * What the list area is showing. An empty park and a search that found nothing
 * are different problems with different answers, so they are different states.
 */
private enum class Stage { Loading, Empty, NoMatch, Sessions }

private val FindVehicleUiState.stage: Stage
    get() = when {
        isLoading && sessions.isEmpty() -> Stage.Loading
        sessions.isNotEmpty() -> Stage.Sessions
        plateQuery.isNotBlank() -> Stage.NoMatch
        else -> Stage.Empty
    }

@Preview(showBackground = true, heightDp = 780)
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
                        entryAttendantName = "Ana Reyes",
                        exitAttendantName = null,
                    ),
                    ParkingSession(
                        id = "s2",
                        ticketCode = "7XQP2LMN",
                        plateNumber = "NBC 8891",
                        status = ParkingSessionStatus.OPEN,
                        entryAt = Instant.now().minusSeconds(900),
                        exitAt = null,
                        terminalName = "Podium",
                        vehicleTypeName = "Motorcycle",
                        ratePlanName = null,
                        feeAmount = null,
                        amountTendered = null,
                        changeGiven = null,
                        entryAttendantName = "Ana Reyes",
                        exitAttendantName = null,
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

@Preview(showBackground = true, heightDp = 780)
@Composable
private fun FindVehicleEmptyPreview() {
    LeasetingParkingTerminalTheme {
        FindVehicleScreen(
            uiState = FindVehicleUiState(isLoading = false),
            onTicketCodeChange = {},
            onSubmitCode = {},
            onPlateQueryChange = {},
            onSessionSelected = {},
            onSettledCodeShown = {},
            onFailureShown = {},
        )
    }
}
