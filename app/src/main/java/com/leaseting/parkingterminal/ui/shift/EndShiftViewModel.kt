package com.leaseting.parkingterminal.ui.shift

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.leaseting.parkingterminal.domain.parking.ParkingRepository
import com.leaseting.parkingterminal.domain.parking.ParkingResult
import com.leaseting.parkingterminal.domain.parking.ParkingShift
import com.leaseting.parkingterminal.domain.parking.ShiftSummary
import com.leaseting.parkingterminal.ui.appContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal

data class EndShiftUiState(
    val isLoading: Boolean = true,
    val summary: ShiftSummary? = null,
    val declaredInput: String = "",
    val note: String = "",
    val isSubmitting: Boolean = false,
    /** Set once the API has the declaration. The screen shows the hand-over. */
    val declared: ParkingShift? = null,
    val failure: ParkingResult.Failure? = null,
) {
    val declaredCash: BigDecimal?
        get() = declaredInput.trim().takeIf { it.isNotEmpty() }?.toBigDecimalOrNull()

    /**
     * Counted minus expected. Shown, never corrected: a short or over drawer is
     * a real thing that an admin needs to see, and hiding it would only push the
     * discrepancy somewhere it cannot be reconciled.
     */
    val variance: BigDecimal?
        get() {
            val expected = summary?.expectedCash ?: return null
            return declaredCash?.minus(expected)
        }

    val canSubmit: Boolean
        get() = !isSubmitting && declared == null && declaredCash != null
}

/**
 * Counting the drawer and handing the shift in.
 *
 * The guard types what they counted; the API records it next to what it
 * expected and marks the shift declared. Confirming the difference is an
 * admin's job in the web client (Phase 2B) — nothing here decides whether the
 * count is acceptable.
 */
class EndShiftViewModel(private val parkingRepository: ParkingRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(EndShiftUiState())
    val uiState: StateFlow<EndShiftUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        _uiState.update { it.copy(isLoading = true, failure = null) }
        viewModelScope.launch {
            when (val result = parkingRepository.shiftSummary()) {
                is ParkingResult.Success ->
                    _uiState.update { it.copy(isLoading = false, summary = result.value) }

                is ParkingResult.Failure ->
                    _uiState.update { it.copy(isLoading = false, failure = result) }
            }
        }
    }

    fun onDeclaredChange(cash: String) {
        _uiState.update {
            it.copy(declaredInput = cash.filter { char -> char.isDigit() || char == '.' })
        }
    }

    fun onNoteChange(note: String) {
        _uiState.update { it.copy(note = note) }
    }

    fun onSubmit() {
        val state = _uiState.value
        val declaredCash = state.declaredCash ?: return
        if (!state.canSubmit) return

        _uiState.update { it.copy(isSubmitting = true, failure = null) }
        viewModelScope.launch {
            when (val result = parkingRepository.declareShift(declaredCash, state.note)) {
                is ParkingResult.Success ->
                    _uiState.update { it.copy(isSubmitting = false, declared = result.value) }

                is ParkingResult.Failure ->
                    _uiState.update { it.copy(isSubmitting = false, failure = result) }
            }
        }
    }

    fun onFailureShown() {
        _uiState.update { it.copy(failure = null) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { EndShiftViewModel(appContainer().parkingRepository) }
        }
    }
}
