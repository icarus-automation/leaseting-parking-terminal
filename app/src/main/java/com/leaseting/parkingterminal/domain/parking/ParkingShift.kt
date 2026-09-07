package com.leaseting.parkingterminal.domain.parking

import java.math.BigDecimal
import java.time.Instant

/**
 * The guard's turn at one terminal, and the till that goes with it.
 *
 * Nothing can be recorded without one: an entry belongs to the shift that
 * printed the stub, and a collected exit to the shift that took the money.
 */
data class ParkingShift(
    val id: String,
    val status: ParkingShiftStatus,
    val openedAt: Instant,
    val terminalId: String,
    val terminalName: String,
    val propertyName: String,
    val expectedCash: BigDecimal?,
    val declaredCash: BigDecimal?,
)

/**
 * How far along the hand-over a shift is. The terminal only ever moves a shift
 * from [OPEN] to [DECLARED]; [CONFIRMED] is an admin reconciling the cash in
 * the web client, which the handheld shows but never causes.
 */
enum class ParkingShiftStatus {
    OPEN,
    DECLARED,
    CONFIRMED,

    /** A status this build does not know. Treated as "not open". */
    UNKNOWN,
}

/** What the guard is shown before counting the drawer. */
data class ShiftSummary(
    val shiftId: String,
    val entryCount: Int,
    val paidExitCount: Int,
    val expectedCash: BigDecimal,
)

/** Whether this handheld currently has somewhere to record transactions. */
sealed interface ShiftState {
    /** The API has not been asked yet. */
    data object Unknown : ShiftState

    /** Signed in, no shift open: the terminal picker is the next screen. */
    data object None : ShiftState

    data class Open(val shift: ParkingShift) : ShiftState
}
