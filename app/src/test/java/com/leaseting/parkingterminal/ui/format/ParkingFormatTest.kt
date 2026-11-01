package com.leaseting.parkingterminal.ui.format

import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant
import java.util.Locale
import java.util.TimeZone

/**
 * Money and durations, as a guard and a receipt see them.
 *
 * The formatting is pinned rather than assumed: what is on the screen when the
 * cash changes hands has to be what is on the paper, on every handheld,
 * regardless of the device's locale.
 */
class ParkingFormatTest {

    @Test
    fun `writes pesos with a sign, grouping and two decimals`() {
        assertEquals("₱150.00", formatAmount(BigDecimal("150")))
        assertEquals("₱1,850.00", formatAmount(BigDecimal("1850.00")))
        assertEquals("₱0.00", formatAmount(BigDecimal.ZERO))
    }

    @Test
    fun `rounds half up, the way a posted price does`() {
        assertEquals("₱150.13", formatAmount(BigDecimal("150.125")))
    }

    @Test
    fun `writes durations in the units a stub is read in`() {
        assertEquals("45m", formatDuration(45))
        assertEquals("2h", formatDuration(120))
        assertEquals("2h 30m", formatDuration(150))
        assertEquals("0m", formatDuration(0))
    }

    @Test
    fun `treats a negative duration as none rather than showing a minus`() {
        assertEquals("0m", formatDuration(-5))
    }

    @Test
    fun `elapsed time never runs backwards on a clock that has drifted`() {
        val now = Instant.parse("2026-09-07T04:30:00Z")
        assertEquals("2h 30m", formatElapsed(Instant.parse("2026-09-07T02:00:00Z"), now))
        assertEquals("0m", formatElapsed(Instant.parse("2026-09-07T05:00:00Z"), now))
    }

    @Test
    fun `groups a stub code the way it is printed`() {
        assertEquals("A3KD-9MPQ", formatTicketCode("A3KD9MPQ"))
    }

    @Test
    fun `leaves a code of an unexpected length alone`() {
        assertEquals("SHORT", formatTicketCode("SHORT"))
    }

    /**
     * The month arrives as letters, and the same letters on every handheld.
     *
     * A receipt came off a roll reading `09 M09 2026`: the formatter had asked
     * root for a month abbreviation, and on Android root is resolved through
     * ICU, which answers with CLDR's placeholder. Naming the locale is what
     * fixes that, and pinning it here is what keeps a device's own locale from
     * getting a say in what the paper says.
     */
    @Test
    fun `writes the month in letters, whatever the device is set to`() {
        val previousZone = TimeZone.getDefault()
        val previousLocale = Locale.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("Asia/Manila"))
            Locale.setDefault(Locale.forLanguageTag("ar-EG"))

            assertEquals(
                "09 Sep 2026, 7:46 PM",
                formatTime(Instant.parse("2026-09-09T11:46:00Z")),
            )
        } finally {
            TimeZone.setDefault(previousZone)
            Locale.setDefault(previousLocale)
        }
    }
}
