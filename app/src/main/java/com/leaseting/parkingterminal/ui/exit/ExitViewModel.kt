package com.leaseting.parkingterminal.ui.exit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.leaseting.parkingterminal.domain.auth.AuthRepository
import com.leaseting.parkingterminal.domain.auth.SessionState
import com.leaseting.parkingterminal.domain.parking.FeeQuote
import com.leaseting.parkingterminal.domain.parking.ParkingRepository
import com.leaseting.parkingterminal.domain.parking.ParkingResult
import com.leaseting.parkingterminal.domain.parking.ParkingSession
import com.leaseting.parkingterminal.domain.parking.ShiftState
import com.leaseting.parkingterminal.domain.printing.ExitReceipt
import com.leaseting.parkingterminal.domain.printing.StubPrinter
import com.leaseting.parkingterminal.ui.appContainer
import com.leaseting.parkingterminal.ui.format.formatAmount
import com.leaseting.parkingterminal.ui.format.formatDuration
import com.leaseting.parkingterminal.ui.format.formatTime
import com.leaseting.parkingterminal.ui.print.PrintState
import com.leaseting.parkingterminal.ui.print.toPrintState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.Instant

data class ExitUiState(
    val isLoadingQuote: Boolean = true,
    val quote: FeeQuote? = null,
    val cashInput: String = "",
    val isCollecting: Boolean = false,
    /** Set once the API has taken the money. The screen switches to the receipt. */
    val paid: ParkingSession? = null,
    val printState: PrintState = PrintState.Idle,
    val failure: ParkingResult.Failure? = null,
) {
    /** Null while the box holds something that is not a number. */
    val tendered: BigDecimal?
        get() = cashInput.trim().takeIf { it.isNotEmpty() }?.toBigDecimalOrNull()

    /**
     * Shown before the money is taken, and only as a preview: the API works out
     * the real change from the fee it prices at the moment of collection.
     */
    val change: BigDecimal?
        get() {
            val fee = quote?.feeAmount ?: return null
            val cash = tendered ?: return null
            return if (cash >= fee) cash - fee else null
        }

    val isShortTender: Boolean
        get() = quote != null && tendered != null && tendered!! < quote.feeAmount

    val canCollect: Boolean
        get() = !isCollecting && paid == null && quote != null && !isShortTender && tendered != null
}

/**
 * Pricing a stay, taking the cash, and printing the receipt.
 *
 * The fee on screen is the API's, and the collection posts the cash against it
 * rather than sending a figure of its own. That is what makes the exit
 * immutable: there is no field a guard can put a different number in, no waive
 * and no override. A fee that is genuinely wrong is an admin void in the web
 * client, which is Phase 2B.
 */
class ExitViewModel(
    private val sessionId: String,
    private val parkingRepository: ParkingRepository,
    private val authRepository: AuthRepository,
    private val stubPrinter: StubPrinter,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExitUiState())
    val uiState: StateFlow<ExitUiState> = _uiState.asStateFlow()

    init {
        loadQuote()
    }

    fun loadQuote() {
        _uiState.update { it.copy(isLoadingQuote = true, failure = null) }
        viewModelScope.launch {
            when (val result = parkingRepository.previewFee(sessionId)) {
                is ParkingResult.Success ->
                    _uiState.update {
                        it.copy(
                            isLoadingQuote = false,
                            quote = result.value,
                            // Exact money is the common case, and a free exit
                            // inside grace becomes one tap.
                            cashInput = result.value.feeAmount.toPlainString(),
                        )
                    }

                is ParkingResult.Failure ->
                    _uiState.update { it.copy(isLoadingQuote = false, failure = result) }
            }
        }
    }

    fun onCashChange(cash: String) {
        _uiState.update { it.copy(cashInput = cash.filter { char -> char.isDigit() || char == '.' }) }
    }

    /** The note the driver handed over, straight onto the field. */
    fun onCashPreset(amount: BigDecimal) {
        _uiState.update { it.copy(cashInput = amount.toPlainString()) }
    }

    fun onCollect() {
        val state = _uiState.value
        val tendered = state.tendered ?: return
        if (!state.canCollect) return

        _uiState.update { it.copy(isCollecting = true, failure = null) }
        viewModelScope.launch {
            when (val result = parkingRepository.payExit(sessionId, tendered)) {
                is ParkingResult.Success -> {
                    _uiState.update { it.copy(isCollecting = false, paid = result.value) }
                    printReceipt()
                }

                is ParkingResult.Failure ->
                    // The quote is reloaded on a rejection: the commonest cause is
                    // the stay having crossed into another billing increment while
                    // the guard was counting, and the new figure is what they need.
                    _uiState.update { it.copy(isCollecting = false, failure = result) }
            }
        }
    }

    /** Also the reprint. Built from the paid stay, so it always matches the record. */
    fun printReceipt() {
        val paid = _uiState.value.paid ?: return
        val quote = _uiState.value.quote
        val shift = (parkingRepository.shiftState.value as? ShiftState.Open)?.shift ?: return
        val signedIn = (authRepository.sessionState.value as? SessionState.SignedIn)?.attendant

        _uiState.update { it.copy(printState = PrintState.Printing) }
        viewModelScope.launch {
            val result = stubPrinter.printExitReceipt(
                ExitReceipt(
                    ticketCode = paid.ticketCode,
                    plateNumber = paid.plateNumber,
                    vehicleTypeName = paid.vehicleTypeName,
                    propertyName = shift.propertyName,
                    terminalName = shift.terminalName,
                    entryAt = formatTime(paid.entryAt),
                    exitAt = formatTime(paid.exitAt ?: Instant.now()),
                    stayLabel = formatDuration(quote?.stayMinutes ?: 0),
                    ratePlanName = paid.ratePlanName.orEmpty(),
                    feeAmount = formatAmount(paid.feeAmount ?: BigDecimal.ZERO),
                    amountTendered = formatAmount(paid.amountTendered ?: BigDecimal.ZERO),
                    changeGiven = formatAmount(paid.changeGiven ?: BigDecimal.ZERO),
                    // Whoever the API recorded as collecting, which is the line
                    // that has to match the books. It is the signed-in guard in
                    // every ordinary case, and that is the only fallback.
                    attendantName = paid.exitAttendantName
                        ?: signedIn?.name.orEmpty(),
                ),
            )
            _uiState.update { it.copy(printState = result.toPrintState()) }
        }
    }

    fun onFailureShown() {
        _uiState.update { it.copy(failure = null) }
    }

    companion object {
        fun factory(sessionId: String) = viewModelFactory {
            initializer {
                val container = appContainer()
                ExitViewModel(
                    sessionId = sessionId,
                    parkingRepository = container.parkingRepository,
                    authRepository = container.authRepository,
                    stubPrinter = container.stubPrinter,
                )
            }
        }
    }
}
