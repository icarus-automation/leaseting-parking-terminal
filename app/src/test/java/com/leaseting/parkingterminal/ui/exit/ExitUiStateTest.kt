package com.leaseting.parkingterminal.ui.exit

import com.leaseting.parkingterminal.domain.parking.FeeQuote
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant

/**
 * The arithmetic a guard watches while counting change.
 *
 * None of this decides what is collected — the API prices the stay and rejects
 * a short tender — but it is what the person at the barrier acts on, so it is
 * pinned.
 */
class ExitUiStateTest {

    private val quote = FeeQuote(
        sessionId = "sess-1",
        ticketCode = "A3KD9MPQ",
        plateNumber = "ABC 1234",
        vehicleTypeName = "Car",
        entryAt = Instant.parse("2026-09-07T02:00:00Z"),
        quotedAt = Instant.parse("2026-09-07T04:30:00Z"),
        stayMinutes = 150,
        billedMinutes = 135,
        graceMinutes = 15,
        ratePlanName = "Hourly",
        feeAmount = BigDecimal("150.00"),
    )

    @Test
    fun `works out the change on the note handed over`() {
        val state = ExitUiState(isLoadingQuote = false, quote = quote, cashInput = "200")

        assertEquals(BigDecimal("50.00"), state.change)
        assertFalse(state.isShortTender)
        assertTrue(state.canCollect)
    }

    @Test
    fun `blocks a collection that would come up short`() {
        val state = ExitUiState(isLoadingQuote = false, quote = quote, cashInput = "100")

        assertTrue(state.isShortTender)
        assertNull(state.change)
        assertFalse(state.canCollect)
    }

    @Test
    fun `treats exact money as collectable with no change`() {
        val state = ExitUiState(isLoadingQuote = false, quote = quote, cashInput = "150.00")

        assertEquals(BigDecimal("0.00"), state.change)
        assertTrue(state.canCollect)
    }

    @Test
    fun `lets a stay inside grace go out for nothing`() {
        val free = quote.copy(feeAmount = BigDecimal.ZERO, stayMinutes = 10, billedMinutes = 0)
        val state = ExitUiState(isLoadingQuote = false, quote = free, cashInput = "0")

        assertFalse(state.isShortTender)
        assertTrue(state.canCollect)
    }

    @Test
    fun `waits while the cash box holds something that is not a number`() {
        val state = ExitUiState(isLoadingQuote = false, quote = quote, cashInput = ".")

        assertNull(state.tendered)
        assertFalse(state.canCollect)
        // Not short either: nothing has been entered to compare against.
        assertFalse(state.isShortTender)
    }

    @Test
    fun `cannot collect twice on one stay`() {
        val state = ExitUiState(
            isLoadingQuote = false,
            quote = quote,
            cashInput = "200",
            paid = null,
        )
        assertTrue(state.canCollect)

        val collected = state.copy(
            paid = com.leaseting.parkingterminal.domain.parking.ParkingSession(
                id = "sess-1",
                ticketCode = "A3KD9MPQ",
                plateNumber = "ABC 1234",
                status = com.leaseting.parkingterminal.domain.parking.ParkingSessionStatus.PAID,
                entryAt = quote.entryAt,
                exitAt = quote.quotedAt,
                terminalName = "Basement",
                vehicleTypeName = "Car",
                ratePlanName = "Hourly",
                feeAmount = BigDecimal("150.00"),
                amountTendered = BigDecimal("200.00"),
                changeGiven = BigDecimal("50.00"),
            ),
        )
        assertFalse(collected.canCollect)
    }
}
