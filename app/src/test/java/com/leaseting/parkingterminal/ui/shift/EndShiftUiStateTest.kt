package com.leaseting.parkingterminal.ui.shift

import com.leaseting.parkingterminal.domain.parking.ShiftSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

/**
 * The hand-over figure.
 *
 * The variance is shown exactly as it falls. A terminal that quietly rounded a
 * short drawer towards the expected figure would be hiding the one number an
 * admin reconciles against.
 */
class EndShiftUiStateTest {

    private val summary = ShiftSummary(
        shiftId = "shift-1",
        entryCount = 24,
        paidExitCount = 19,
        expectedCash = BigDecimal("1850.00"),
    )

    @Test
    fun `reports a short drawer as short`() {
        val state = EndShiftUiState(isLoading = false, summary = summary, declaredInput = "1830")

        assertEquals(BigDecimal("-20.00"), state.variance)
        assertTrue(state.canSubmit)
    }

    @Test
    fun `reports an over drawer as over`() {
        val state = EndShiftUiState(isLoading = false, summary = summary, declaredInput = "1900")

        assertEquals(BigDecimal("50.00"), state.variance)
    }

    @Test
    fun `reports a balanced drawer as balanced`() {
        val state = EndShiftUiState(isLoading = false, summary = summary, declaredInput = "1850.00")

        assertEquals(0, state.variance?.signum())
    }

    @Test
    fun `allows a genuinely empty till to be declared as zero`() {
        val empty = summary.copy(paidExitCount = 0, expectedCash = BigDecimal.ZERO)
        val state = EndShiftUiState(isLoading = false, summary = empty, declaredInput = "0")

        assertTrue(state.canSubmit)
        assertEquals(0, state.variance?.signum())
    }

    @Test
    fun `will not submit an empty count`() {
        val state = EndShiftUiState(isLoading = false, summary = summary, declaredInput = "")

        assertNull(state.declaredCash)
        assertNull(state.variance)
        assertFalse(state.canSubmit)
    }

    @Test
    fun `will not submit twice`() {
        val state = EndShiftUiState(
            isLoading = false,
            summary = summary,
            declaredInput = "1850",
            declared = com.leaseting.parkingterminal.domain.parking.ParkingShift(
                id = "shift-1",
                status = com.leaseting.parkingterminal.domain.parking.ParkingShiftStatus.DECLARED,
                openedAt = java.time.Instant.parse("2026-09-07T06:00:00Z"),
                terminalId = "term-1",
                terminalName = "Basement",
                propertyName = "Brickstone",
                expectedCash = BigDecimal("1850.00"),
                declaredCash = BigDecimal("1850.00"),
            ),
        )

        assertFalse(state.canSubmit)
    }
}
