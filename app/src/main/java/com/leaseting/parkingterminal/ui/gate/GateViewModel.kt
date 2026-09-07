package com.leaseting.parkingterminal.ui.gate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.leaseting.parkingterminal.domain.parking.ParkingRepository
import com.leaseting.parkingterminal.domain.parking.ParkingResult
import com.leaseting.parkingterminal.domain.parking.ShiftSummary
import com.leaseting.parkingterminal.ui.appContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GateUiState(
    val isRefreshing: Boolean = false,
    val summary: ShiftSummary? = null,
    val failure: ParkingResult.Failure? = null,
)

/**
 * The gate's running totals: stubs printed, exits collected, cash owed.
 *
 * Refreshed whenever the guard returns to this screen rather than polled. A
 * terminal changes only when its own guard does something, so the moment they
 * come back from an entry or an exit is exactly when the numbers are stale.
 */
class GateViewModel(private val parkingRepository: ParkingRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(GateUiState())
    val uiState: StateFlow<GateUiState> = _uiState.asStateFlow()

    fun refresh() {
        _uiState.update { it.copy(isRefreshing = true) }
        viewModelScope.launch {
            when (val result = parkingRepository.shiftSummary()) {
                is ParkingResult.Success ->
                    _uiState.update {
                        it.copy(isRefreshing = false, summary = result.value, failure = null)
                    }

                is ParkingResult.Failure ->
                    // The last known totals stay on screen. A dropped connection
                    // should not blank the numbers a guard was just reading.
                    _uiState.update { it.copy(isRefreshing = false, failure = result) }
            }
        }
    }

    fun onFailureShown() {
        _uiState.update { it.copy(failure = null) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { GateViewModel(appContainer().parkingRepository) }
        }
    }
}
