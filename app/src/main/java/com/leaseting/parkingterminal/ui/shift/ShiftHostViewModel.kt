package com.leaseting.parkingterminal.ui.shift

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.leaseting.parkingterminal.domain.parking.ParkingRepository
import com.leaseting.parkingterminal.domain.parking.ShiftState
import com.leaseting.parkingterminal.ui.appContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Whether this handheld has a shift open, for the whole application.
 *
 * Held above the navigation graph next to the session: a terminal with no shift
 * shows the picker and a terminal with one shows the gate, wherever the guard
 * happens to be. Declaring a shift moves the graph back on its own.
 */
class ShiftHostViewModel(private val parkingRepository: ParkingRepository) : ViewModel() {

    val shiftState: StateFlow<ShiftState> = parkingRepository.shiftState

    private val _hasCheckedShift = MutableStateFlow(false)

    /**
     * Whether the API has been asked yet, however it answered.
     *
     * Distinct from [shiftState] because a failed check leaves the shift
     * genuinely unknown, and the terminal must not sit on a spinner at a gate
     * waiting for a network that is down. Once the question has been put, the
     * guard is sent to the picker, where the error is visible and picking the
     * same gate again hands their shift back.
     */
    val hasCheckedShift: StateFlow<Boolean> = _hasCheckedShift.asStateFlow()

    /**
     * Asks the API whether this attendant already holds a shift. Called when a
     * session appears — at sign-in, and on a cold start with a stored one — so a
     * handheld that restarted mid-shift comes back to the same till instead of
     * asking the guard to open a second one.
     */
    fun refresh() {
        _hasCheckedShift.value = false
        viewModelScope.launch {
            parkingRepository.refreshShift()
            _hasCheckedShift.value = true
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { ShiftHostViewModel(appContainer().parkingRepository) }
        }
    }
}
