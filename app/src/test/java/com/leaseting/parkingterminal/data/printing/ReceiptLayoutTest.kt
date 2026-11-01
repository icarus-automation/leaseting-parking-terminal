package com.leaseting.parkingterminal.data.printing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The 58 mm roll, pinned.
 *
 * Every case here came back from a gate on real paper. First a timestamp split
 * as `09 Sep 2026, 7:` then `46 PM`, and an attendant as `Parking Attenda` then
 * `nt`, both from the vendor's `printTableText` halving the width. Then, once
 * the rows were laid out here, the three money lines came back with their last
 * digit orphaned underneath:
 *
 * ```text
 * AMOUNT DUE              ₱150.0
 * 0
 * ```
 *
 * Those lines were 32 characters like every other row, and 33 cells, because
 * the peso sign is not ASCII and the printer's font gives it two.
 *
 * Nothing below may break a value inside a word, and nothing below may be wider
 * than [ReceiptLayout.COLUMNS] cells.
 */
class ReceiptLayoutTest {

    @Test
    fun `counts a peso sign as the two cells it prints as`() {
        assertEquals(7, "₱150.00".length)
        assertEquals(8, ReceiptLayout.cells("₱150.00"))
    }

    @Test
    fun `counts plain text one cell to the character`() {
        assertEquals(20, ReceiptLayout.cells("09 Sep 2026, 7:46 PM"))
        assertEquals(0, ReceiptLayout.cells(""))
    }

    @Test
    fun `keeps the amount due on one line`() {
        assertEquals(
            listOf("AMOUNT DUE              ₱150.00"),
            ReceiptLayout.row("AMOUNT DUE", "₱150.00"),
        )
    }

    @Test
    fun `keeps the cash taken on one line`() {
        assertEquals(
            listOf("Cash                    ₱200.00"),
            ReceiptLayout.row("Cash", "₱200.00"),
        )
    }

    @Test
    fun `keeps the change on one line`() {
        assertEquals(
            listOf("Change                   ₱50.00"),
            ReceiptLayout.row("Change", "₱50.00"),
        )
    }

    /**
     * The orphaned digit, guarded directly. A money row that measures 33 cells
     * is one the printer will break, however many characters it has.
     */
    @Test
    fun `never lets a digit orphan off the end of a money row`() {
        val amounts = listOf(
            "₱0.00",
            "₱50.00",
            "₱150.00",
            "₱200.00",
            "₱1,850.00",
            "₱12,345.00",
        )

        listOf("AMOUNT DUE", "Cash", "Change").forEach { label ->
            amounts.forEach { amount ->
                val lines = ReceiptLayout.row(label, amount)

                assertEquals("$label $amount", 1, lines.size)
                assertEquals("$label $amount", 32, ReceiptLayout.cells(lines.single()))
                assertTrue("$label $amount", lines.single().endsWith(amount))
            }
        }
    }

    @Test
    fun `keeps a printed timestamp on one line`() {
        assertEquals(
            listOf("Time in     09 Sep 2026, 7:46 PM"),
            ReceiptLayout.row("Time in", "09 Sep 2026, 7:46 PM"),
        )
    }

    @Test
    fun `keeps the longest timestamp of the day on one line`() {
        assertEquals(
            listOf("Time out   09 Sep 2026, 11:46 PM"),
            ReceiptLayout.row("Time out", "09 Sep 2026, 11:46 PM"),
        )
    }

    @Test
    fun `keeps the attendant's title on one line`() {
        assertEquals(
            listOf("Attendant      Parking Attendant"),
            ReceiptLayout.row("Attendant", "Parking Attendant"),
        )
    }

    @Test
    fun `breaks a long name between its words rather than through one`() {
        assertEquals(
            listOf(
                "Attendant",
                " Maria Cristina Dela Cruz Santos",
            ),
            ReceiptLayout.row("Attendant", "Maria Cristina Dela Cruz Santos"),
        )
    }

    @Test
    fun `carries a name too long for one line onto a second`() {
        assertEquals(
            listOf(
                "Attendant",
                " Maria Cristina Dela Cruz Santos",
                "                      Villanueva",
            ),
            ReceiptLayout.row("Attendant", "Maria Cristina Dela Cruz Santos Villanueva"),
        )
    }

    @Test
    fun `prints a label alone when there is no value to put beside it`() {
        assertEquals(listOf("Rate"), ReceiptLayout.row("Rate", ""))
    }

    @Test
    fun `collapses the whitespace a typed name arrives with`() {
        assertEquals(
            listOf("Attendant         Juan Dela Cruz"),
            ReceiptLayout.row("Attendant", " Juan   Dela  Cruz "),
        )
    }

    @Test
    fun `never lets a line run past the paper`() {
        val values = listOf(
            "09 Sep 2026, 11:46 PM",
            "₱1,850.00",
            "Maria Cristina Dela Cruz Santos Villanueva",
            "Overnight Flat Rate, Weekends And Holidays",
            "Peña Dela Cruz-Villanueva",
            "A".repeat(47),
            "₱".repeat(30),
        )

        values.forEach { value ->
            ReceiptLayout.row("Attendant", value).forEach { line ->
                assertTrue(line, ReceiptLayout.cells(line) <= ReceiptLayout.COLUMNS)
            }
        }
    }

    @Test
    fun `wraps double-width text by cells rather than by characters`() {
        // Sixteen peso signs are thirty-two cells: a full line and not one more.
        assertEquals(
            listOf("₱".repeat(16), "₱".repeat(4)),
            ReceiptLayout.wrap("₱".repeat(20)),
        )
    }

    @Test
    fun `chops a token wider than the paper rather than dropping it`() {
        assertEquals(
            listOf("A".repeat(32), "A".repeat(8)),
            ReceiptLayout.wrap("A".repeat(40)),
        )
    }

    @Test
    fun `has nothing to wrap when there is nothing to print`() {
        assertEquals(emptyList<String>(), ReceiptLayout.wrap("   "))
    }

    @Test
    fun `rules across the full width`() {
        assertEquals(ReceiptLayout.COLUMNS, ReceiptLayout.cells(ReceiptLayout.rule()))
    }

    /**
     * A person's name, which is what an Attendant line carries now that the API
     * sends `displayName`. See `docs/integrations/attendant-display-name.md`.
     */
    @Test
    fun `keeps an attendant's person name on one line`() {
        listOf("Ana Reyes", "Rico Delgado", "Ma. Teresa Bonifacio").forEach { name ->
            val lines = ReceiptLayout.row("Attendant", name)

            assertEquals(name, 1, lines.size)
            assertEquals(name, 32, ReceiptLayout.cells(lines.single()))
            assertTrue(name, lines.single().endsWith(name))
        }
    }

    /**
     * The entry stub, which now names the guard who booked the vehicle in. It is
     * a shorter block than the receipt and its Attendant line follows the same
     * rules.
     */
    @Test
    fun `prints the stub block a driver keeps`() {
        val rows = listOf(
            "Plate" to "ABC 1234",
            "Vehicle" to "Car",
            "Time in" to "09 Sep 2026, 5:16 PM",
            "Attendant" to "Ana Reyes",
        )

        val paper = rows.flatMap { (label, value) -> ReceiptLayout.row(label, value) }

        assertEquals(
            """
            Plate                   ABC 1234
            Vehicle                      Car
            Time in     09 Sep 2026, 5:16 PM
            Attendant              Ana Reyes
            """.trimIndent(),
            paper.joinToString("\n"),
        )
    }

    /**
     * The exit receipt as a block, so a change to the layout has to be looked at
     * against the paper it produces rather than one row at a time. The labels
     * and the order are the ones `H10sStubPrinter.printExitReceipt` sends.
     *
     * The money rows are a character shorter than the rest and the same width on
     * paper. That is the fix, visible in the golden.
     */
    @Test
    fun `prints the receipt block a guard hands over`() {
        val rows = listOf(
            "Stub" to "A3KD-9MPQ",
            "Plate" to "ABC 1234",
            "Vehicle" to "Car",
            "Time in" to "09 Sep 2026, 5:16 PM",
            "Time out" to "09 Sep 2026, 7:46 PM",
            "Duration" to "2h 30m",
            "Rate" to "Hourly",
            "AMOUNT DUE" to "₱150.00",
            "Cash" to "₱200.00",
            "Change" to "₱50.00",
            "Attendant" to "Parking Attendant",
        )

        val paper = rows.flatMap { (label, value) -> ReceiptLayout.row(label, value) }

        assertEquals(
            """
            Stub                   A3KD-9MPQ
            Plate                   ABC 1234
            Vehicle                      Car
            Time in     09 Sep 2026, 5:16 PM
            Time out    09 Sep 2026, 7:46 PM
            Duration                  2h 30m
            Rate                      Hourly
            AMOUNT DUE              ₱150.00
            Cash                    ₱200.00
            Change                   ₱50.00
            Attendant      Parking Attendant
            """.trimIndent(),
            paper.joinToString("\n"),
        )

        paper.forEach { line ->
            assertEquals(line, ReceiptLayout.COLUMNS, ReceiptLayout.cells(line))
        }
    }
}
