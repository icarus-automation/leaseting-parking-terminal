package com.leaseting.parkingterminal.ui.terminal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.leaseting.parkingterminal.domain.parking.ParkingRepository
import com.leaseting.parkingterminal.domain.parking.ParkingResult
import com.leaseting.parkingterminal.domain.parking.ParkingTerminal
import com.leaseting.parkingterminal.ui.appContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Everything the terminal picker draws. Disabled terminals and archived
 * properties never reach it — the repository's catalog filters them, so the
 * list only ever offers gates a shift can actually be opened on.
 */
data class TerminalPickerUiState(
    val isLoading: Boolean = true,
    val terminals: List<ParkingTerminal> = emptyList(),
    val openingTerminalId: String? = null,
    val failure: ParkingResult.Failure? = null,
)

/**
 * Picking a gate and opening the shift on it.
 *
 * Success is not reported back to the screen: the shift flow changes and the
 * navigation graph moves off this screen on its own, the same way sign-in does.
 */
class TerminalPickerViewModel(private val parkingRepository: ParkingRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(TerminalPickerUiState())
    val uiState: StateFlow<TerminalPickerUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        _uiState.update { it.copy(isLoading = true, failure = null) }
        viewModelScope.launch {
            when (val result = parkingRepository.loadCatalog()) {
                is ParkingResult.Success ->
                    _uiState.update {
                        it.copy(isLoading = false, terminals = result.value.selectableTerminals)
                    }

                is ParkingResult.Failure ->
                    _uiState.update { it.copy(isLoading = false, failure = result) }
            }
        }
    }

    fun onTerminalSelected(terminalId: String) {
        if (_uiState.value.openingTerminalId != null) return

        _uiState.update { it.copy(openingTerminalId = terminalId, failure = null) }
        viewModelScope.launch {
            val result = parkingRepository.openShift(terminalId)
            _uiState.update {
                it.copy(
                    openingTerminalId = null,
                    failure = result as? ParkingResult.Failure,
                )
            }
        }
    }

    fun onFailureShown() {
        _uiState.update { it.copy(failure = null) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { TerminalPickerViewModel(appContainer().parkingRepository) }
        }
    }
}
