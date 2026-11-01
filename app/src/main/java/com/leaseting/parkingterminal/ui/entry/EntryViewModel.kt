package com.leaseting.parkingterminal.ui.entry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.leaseting.parkingterminal.domain.auth.AuthRepository
import com.leaseting.parkingterminal.domain.auth.SessionState
import com.leaseting.parkingterminal.domain.parking.ParkingRepository
import com.leaseting.parkingterminal.domain.parking.ParkingResult
import com.leaseting.parkingterminal.domain.parking.ParkingSession
import com.leaseting.parkingterminal.domain.parking.ShiftState
import com.leaseting.parkingterminal.domain.parking.VehicleType
import com.leaseting.parkingterminal.domain.printing.EntryStub
import com.leaseting.parkingterminal.domain.printing.StubPrinter
import com.leaseting.parkingterminal.ui.appContainer
import com.leaseting.parkingterminal.ui.format.formatTime
import com.leaseting.parkingterminal.ui.print.PrintState
import com.leaseting.parkingterminal.ui.print.toPrintState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EntryUiState(
    val isLoadingVehicleTypes: Boolean = true,
    val vehicleTypes: List<VehicleType> = emptyList(),
    val selectedVehicleTypeId: String? = null,
    val plateNumber: String = "",
    val isSubmitting: Boolean = false,
    /** Set once the stay exists in leaseting-api. The screen switches to the stub. */
    val recorded: ParkingSession? = null,
    val printState: PrintState = PrintState.Idle,
    val failure: ParkingResult.Failure? = null,
) {
    val canSubmit: Boolean
        get() = !isSubmitting &&
            recorded == null &&
            selectedVehicleTypeId != null &&
            plateNumber.isNotBlank()
}

/**
 * Recording an arrival, then printing the stub for it.
 *
 * Those are two steps on purpose. The stay is the row in leaseting-api; the
 * stub is paper. A jammed printer must not turn a vehicle away or, worse, let
 * one in with no record, so the entry is recorded first and the print is
 * offered — and re-offered — afterwards.
 */
class EntryViewModel(
    private val parkingRepository: ParkingRepository,
    private val authRepository: AuthRepository,
    private val stubPrinter: StubPrinter,
) : ViewModel() {

    private val _uiState = MutableStateFlow(EntryUiState())
    val uiState: StateFlow<EntryUiState> = _uiState.asStateFlow()

    init {
        loadVehicleTypes()
    }

    fun loadVehicleTypes() {
        _uiState.update { it.copy(isLoadingVehicleTypes = true) }
        viewModelScope.launch {
            when (val result = parkingRepository.loadCatalog()) {
                is ParkingResult.Success -> {
                    val types = result.value.selectableVehicleTypes
                    _uiState.update {
                        it.copy(
                            isLoadingVehicleTypes = false,
                            vehicleTypes = types,
                            selectedVehicleTypeId = it.selectedVehicleTypeId
                                ?: types.defaultSelection()?.id,
                        )
                    }
                }

                is ParkingResult.Failure ->
                    _uiState.update { it.copy(isLoadingVehicleTypes = false, failure = result) }
            }
        }
    }

    fun onVehicleTypeSelected(vehicleTypeId: String) {
        _uiState.update { it.copy(selectedVehicleTypeId = vehicleTypeId, failure = null) }
    }

    fun onPlateChange(plateNumber: String) {
        // Upper-cased as it is typed: it is what the API stores and what the
        // guard will read back off a stub.
        _uiState.update { it.copy(plateNumber = plateNumber.uppercase(), failure = null) }
    }

    fun onSubmit() {
        val state = _uiState.value
        val vehicleTypeId = state.selectedVehicleTypeId ?: return
        if (!state.canSubmit) return

        _uiState.update { it.copy(isSubmitting = true, failure = null) }
        viewModelScope.launch {
            when (val result = parkingRepository.recordEntry(vehicleTypeId, state.plateNumber)) {
                is ParkingResult.Success -> {
                    _uiState.update { it.copy(isSubmitting = false, recorded = result.value) }
                    printStub()
                }

                is ParkingResult.Failure ->
                    _uiState.update { it.copy(isSubmitting = false, failure = result) }
            }
        }
    }

    /** Also the reprint: the stub is regenerated from the recorded stay each time. */
    fun printStub() {
        val session = _uiState.value.recorded ?: return
        val shift = (parkingRepository.shiftState.value as? ShiftState.Open)?.shift ?: return
        val signedIn = (authRepository.sessionState.value as? SessionState.SignedIn)?.attendant

        _uiState.update { it.copy(printState = PrintState.Printing) }
        viewModelScope.launch {
            val result = stubPrinter.printEntryStub(
                EntryStub(
                    ticketCode = session.ticketCode,
                    plateNumber = session.plateNumber,
                    vehicleTypeName = session.vehicleTypeName,
                    propertyName = shift.propertyName,
                    terminalName = shift.terminalName,
                    entryAt = formatTime(session.entryAt),
                    // The API's own record of who booked it in. The signed-in
                    // attendant is the same person in every ordinary case, and is
                    // only reached for if a build of the API omits the field.
                    attendantName = session.entryAttendantName
                        ?: signedIn?.name.orEmpty(),
                ),
            )
            _uiState.update { it.copy(printState = result.toPrintState()) }
        }
    }

    /** Clears the form for the vehicle behind this one, keeping the type selected. */
    fun onNextVehicle() {
        _uiState.update {
            it.copy(
                plateNumber = "",
                recorded = null,
                printState = PrintState.Idle,
                failure = null,
            )
        }
    }

    fun onFailureShown() {
        _uiState.update { it.copy(failure = null) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val container = appContainer()
                EntryViewModel(
                    parkingRepository = container.parkingRepository,
                    authRepository = container.authRepository,
                    stubPrinter = container.stubPrinter,
                )
            }
        }
    }
}

/**
 * The type a guard would otherwise tap first, every time.
 *
 * Nearly everything through the barrier is a car, so the form arrives ready for
 * one and the guard changes it for the exception. The chip stays visibly
 * selected, which is what keeps the default honest: a motorcycle priced as a car
 * is a wrong fee, and the only thing standing between that and the till is the
 * guard seeing which chip is lit.
 *
 * Matched on the name rather than a flag, because the API has no notion of a
 * default and these rows are staff-editable in the web client. An organization
 * that calls it something else gets no preselection rather than the wrong one.
 */
internal fun List<VehicleType>.defaultSelection(): VehicleType? =
    firstOrNull { it.name.trim().equals(DEFAULT_VEHICLE_TYPE_NAME, ignoreCase = true) }
        // One type is not a choice, it is the answer.
        ?: singleOrNull()

private const val DEFAULT_VEHICLE_TYPE_NAME = "Car"
