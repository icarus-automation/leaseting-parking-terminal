package com.leaseting.parkingterminal.ui.find

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.leaseting.parkingterminal.domain.parking.ParkingRepository
import com.leaseting.parkingterminal.domain.parking.ParkingResult
import com.leaseting.parkingterminal.domain.parking.ParkingSession
import com.leaseting.parkingterminal.domain.parking.ParkingSessionStatus
import com.leaseting.parkingterminal.ui.appContainer
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FindVehicleUiState(
    val ticketCode: String = "",
    val plateQuery: String = "",
    val isLoading: Boolean = true,
    val isResolvingCode: Boolean = false,
    val sessions: List<ParkingSession> = emptyList(),
    /** Set when a code resolves to an open stay: the screen moves to the exit. */
    val resolvedSessionId: String? = null,
    /**
     * A code that resolved to a stay that is no longer in the park. Scanning the
     * same stub twice is the commonest mis-scan at a gate, and it deserves a
     * plain answer rather than a screen that appears to do nothing.
     */
    val settledCode: String? = null,
    val failure: ParkingResult.Failure? = null,
) {
    val canSubmitCode: Boolean
        get() = !isResolvingCode && ticketCode.isNotBlank()
}

/**
 * The three ways a guard finds a vehicle on the way out: the stub code, the
 * in-park list, and the plate when the stub is gone.
 *
 * They are one screen because they are one moment. A driver at the barrier is
 * either holding a stub or not, and making the guard pick a mode first is a
 * step that buys nothing.
 */
@OptIn(FlowPreview::class)
class FindVehicleViewModel(private val parkingRepository: ParkingRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(FindVehicleUiState())
    val uiState: StateFlow<FindVehicleUiState> = _uiState.asStateFlow()

    init {
        // The list follows the plate box. Debounced because a guard types a
        // plate at a barrier, not into a search engine, and every keystroke
        // would otherwise be a round trip on a gate network.
        _uiState
            .map { it.plateQuery.trim() }
            .distinctUntilChanged()
            .debounce(SEARCH_DEBOUNCE_MS)
            .onEach { load(it) }
            .launchIn(viewModelScope)
    }

    fun refresh() = load(_uiState.value.plateQuery.trim())

    private fun load(plate: String) {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            when (val result = parkingRepository.openSessions(plate.takeIf { it.isNotEmpty() })) {
                is ParkingResult.Success ->
                    _uiState.update { state ->
                        // Only stays still in the park can be exited. A settled
                        // one on this list would be a row that does nothing.
                        state.copy(
                            isLoading = false,
                            sessions = result.value.filter {
                                it.status == ParkingSessionStatus.OPEN
                            },
                        )
                    }

                is ParkingResult.Failure ->
                    _uiState.update { it.copy(isLoading = false, failure = result) }
            }
        }
    }

    /**
     * Takes what the scanner or the guard put in the code box. A handheld
     * scanner that behaves as a keyboard fills this field and submits it; a
     * guard reading a creased stub types the same characters.
     */
    fun onTicketCodeChange(code: String) {
        _uiState.update { it.copy(ticketCode = code.uppercase(), failure = null, settledCode = null) }
    }

    fun onPlateQueryChange(plate: String) {
        _uiState.update { it.copy(plateQuery = plate.uppercase(), failure = null) }
    }

    fun onSubmitCode() {
        val state = _uiState.value
        if (!state.canSubmitCode) return

        _uiState.update { it.copy(isResolvingCode = true, failure = null, settledCode = null) }
        viewModelScope.launch {
            when (val result = parkingRepository.findByTicketCode(state.ticketCode)) {
                is ParkingResult.Success -> {
                    val session = result.value
                    val isOpen = session.status == ParkingSessionStatus.OPEN
                    _uiState.update {
                        it.copy(
                            isResolvingCode = false,
                            ticketCode = "",
                            resolvedSessionId = session.id.takeIf { _ -> isOpen },
                            settledCode = session.ticketCode.takeIf { _ -> !isOpen },
                        )
                    }
                }

                is ParkingResult.Failure ->
                    _uiState.update { it.copy(isResolvingCode = false, failure = result) }
            }
        }
    }

    fun onSessionNavigated() {
        _uiState.update { it.copy(resolvedSessionId = null) }
    }

    fun onSettledCodeShown() {
        _uiState.update { it.copy(settledCode = null) }
    }

    fun onFailureShown() {
        _uiState.update { it.copy(failure = null) }
    }

    companion object {
        private const val SEARCH_DEBOUNCE_MS = 300L

        val Factory = viewModelFactory {
            initializer { FindVehicleViewModel(appContainer().parkingRepository) }
        }
    }
}
