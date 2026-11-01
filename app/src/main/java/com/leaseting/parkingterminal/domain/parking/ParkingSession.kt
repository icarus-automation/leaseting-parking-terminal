package com.leaseting.parkingterminal.domain.parking

import java.math.BigDecimal
import java.time.Instant

/**
 * One vehicle's stay, from the printed stub to the collected exit.
 *
 * The money fields are filled in by the API at exit and are read-only here. A
 * paid stay is never edited from the handheld: a correction is an admin void in
 * the web client.
 */
data class ParkingSession(
    val id: String,
    val ticketCode: String,
    val plateNumber: String,
    val status: ParkingSessionStatus,
    val entryAt: Instant,
    val exitAt: Instant?,
    val terminalName: String,
    val vehicleTypeName: String,
    val ratePlanName: String?,
    val feeAmount: BigDecimal?,
    val amountTendered: BigDecimal?,
    val changeGiven: BigDecimal?,
    /** Who booked the vehicle in, as a person. Printed on the stub. */
    val entryAttendantName: String?,
    /** Who took the money. Printed on the receipt. Null until paid. */
    val exitAttendantName: String?,
)

enum class ParkingSessionStatus {
    OPEN,
    PAID,
    VOID,

    /** A status this build does not know. Never treated as still in the park. */
    UNKNOWN,
}

/**
 * What a stay costs as of [quotedAt], priced by the API against the
 * organization's current rate board.
 *
 * The terminal does not compute this and does not cache it. An admin repricing
 * a plan in the web client changes the next quote, which is the behaviour a
 * guard at a barrier needs: what the screen says is what the API will collect.
 */
data class FeeQuote(
    val sessionId: String,
    val ticketCode: String,
    val plateNumber: String,
    val vehicleTypeName: String,
    val entryAt: Instant,
    val quotedAt: Instant,
    val stayMinutes: Int,
    val billedMinutes: Int,
    val graceMinutes: Int,
    val ratePlanName: String,
    val feeAmount: BigDecimal,
)
