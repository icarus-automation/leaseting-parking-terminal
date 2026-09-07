package com.leaseting.parkingterminal.domain.parking

import kotlinx.coroutines.flow.StateFlow
import java.math.BigDecimal

/**
 * The gate loop, as the screens see it.
 *
 * Phase 2A is online-first on purpose. Every call here goes to leaseting-api
 * and fails visibly when the gate is offline, because the alternative — a
 * handheld that quotes a fee from a cached rate board — is a handheld that
 * collects the wrong money after an admin reprices in the web client. The
 * offline queue in the PRD builds on top of this once the online loop is
 * proven, and it will queue entries rather than fees.
 *
 * [shiftState] is the one piece of state the screens share: it decides whether
 * the terminal shows a picker or a working gate, and it survives the guard
 * moving between screens.
 */
interface ParkingRepository {
    val shiftState: StateFlow<ShiftState>

    /** Terminals and vehicle types. Re-read whenever a picker is opened. */
    suspend fun loadCatalog(): ParkingResult<ParkingCatalog>

    /**
     * Asks the API whether this attendant already holds a shift, and publishes
     * the answer to [shiftState]. Called on launch: a handheld that restarts
     * mid-shift comes back to the same till.
     */
    suspend fun refreshShift(): ParkingResult<ParkingShift?>

    suspend fun openShift(terminalId: String): ParkingResult<ParkingShift>

    /** Records an arrival and mints the stub code that gets printed. */
    suspend fun recordEntry(vehicleTypeId: String, plateNumber: String): ParkingResult<ParkingSession>

    /** Vehicles still in the park. [plate] narrows it for a lost-stub lookup. */
    suspend fun openSessions(plate: String? = null): ParkingResult<List<ParkingSession>>

    /** The scan path: a code off the QR, or typed off the stub. */
    suspend fun findByTicketCode(code: String): ParkingResult<ParkingSession>

    /** What this stay costs right now. Shown to the guard before the drawer opens. */
    suspend fun previewFee(sessionId: String): ParkingResult<FeeQuote>

    /**
     * Takes the cash and closes the stay. The fee is not passed: the API prices
     * the stay again at the moment of collection and rejects a short tender, so
     * a stale quote on screen cannot undercharge.
     */
    suspend fun payExit(sessionId: String, amountTendered: BigDecimal): ParkingResult<ParkingSession>

    suspend fun shiftSummary(): ParkingResult<ShiftSummary>

    /** Ends the shift with the guard's own count. One way: a shift never reopens. */
    suspend fun declareShift(declaredCash: BigDecimal, note: String?): ParkingResult<ParkingShift>
}
