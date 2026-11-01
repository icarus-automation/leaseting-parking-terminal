package com.leaseting.parkingterminal.data.printing

/**
 * The 58 mm roll, measured in character cells.
 *
 * The vendor's own two-column primitive, `printTableText`, gives each column a
 * share of the width from a weight and then breaks anything that outgrows its
 * share wherever the overflow falls. A receipt back from the gate read
 * `09 Sep 2026, 7:` then `46 PM` on the next line, and its attendant as
 * `Parking Attenda` then `nt`. Weights are relative and the service rounds them
 * itself, so no pair of weights makes that safe.
 *
 * Rows are laid out here instead and handed to `printText` already padded. The
 * printer's font is a fixed-cell bitmap, the same assumption the separator rule
 * has always rested on, and that is what lets a padded string land where it is
 * put.
 *
 * Everything here is pure Kotlin, so the layout is pinned by a unit test rather
 * than by feeding paper through a handheld.
 */
internal object ReceiptLayout {

    /** What one line holds at the body text size on 58 mm paper. */
    const val COLUMNS = 32

    /** At least one space, so a label never runs into its value. */
    private const val MIN_GAP = 1

    private val WHITESPACE = Regex("\\s+")

    /**
     * How wide a string prints, in cells.
     *
     * The font is single width for ASCII and double width for everything else,
     * which is the usual arrangement on these printers and is not something the
     * JAR lets anyone ask about. It cost a second field receipt to find: money
     * rows came back with their last digit alone on the next line, as
     * `AMOUNT DUE ... P150.0` then `0`, while every row without a peso sign sat
     * on one line. Those money lines were 32 characters like the rest, and 33
     * cells because of the one glyph outside ASCII.
     *
     * So nothing here counts characters. `₱150.00` is seven characters and eight
     * cells, and eight is the number that has to fit.
     */
    fun cells(text: String): Int {
        var width = 0
        for (character in text) width += if (character.code < ASCII_LIMIT) 1 else 2
        return width
    }

    fun rule(columns: Int = COLUMNS): String = "-".repeat(columns)

    /**
     * `Label                      value`, with the label at the left margin and
     * the value against the right one.
     *
     * One line whenever the pair fits. When it does not, the label keeps its own
     * line and the value wraps beneath it at word boundaries, still right
     * aligned, so a long name breaks between its words instead of through one.
     *
     * Runs of whitespace inside a value are collapsed first, so a name typed
     * with a stray double space prints the same whether it fits or wraps.
     */
    fun row(label: String, value: String, columns: Int = COLUMNS): List<String> {
        val cleanLabel = label.collapseWhitespace().takeCells(columns)
        val cleanValue = value.collapseWhitespace()
        if (cleanValue.isEmpty()) return listOf(cleanLabel)

        val gap = columns - cells(cleanLabel) - cells(cleanValue)
        if (gap >= MIN_GAP) return listOf(cleanLabel + " ".repeat(gap) + cleanValue)

        return buildList {
            add(cleanLabel)
            wrap(cleanValue, columns).forEach { add(it.alignRight(columns)) }
        }
    }

    /**
     * Greedy word wrap. A single token wider than the paper is chopped rather
     * than dropped: losing a character of a rate plan's name is bad, losing the
     * whole name is worse.
     */
    fun wrap(text: String, columns: Int = COLUMNS): List<String> {
        require(columns > 0) { "A line holds at least one cell" }

        val words = text.trim().split(WHITESPACE).filter { it.isNotEmpty() }
        if (words.isEmpty()) return emptyList()

        val lines = mutableListOf<String>()
        var line = StringBuilder()

        for (word in words) {
            var remaining = word
            while (cells(remaining) > columns) {
                if (line.isNotEmpty()) {
                    lines += line.toString()
                    line = StringBuilder()
                }
                val head = remaining.takeCells(columns)
                lines += head
                remaining = remaining.drop(head.length)
            }
            if (remaining.isEmpty()) continue

            when {
                line.isEmpty() -> line.append(remaining)
                cells(line.toString()) + MIN_GAP + cells(remaining) <= columns ->
                    line.append(' ').append(remaining)

                else -> {
                    lines += line.toString()
                    line = StringBuilder(remaining)
                }
            }
        }

        if (line.isNotEmpty()) lines += line.toString()
        return lines
    }

    /** As much of the string as fits the budget, never splitting a cell in half. */
    private fun String.takeCells(columns: Int): String {
        var width = 0
        for ((index, character) in withIndex()) {
            val next = width + if (character.code < ASCII_LIMIT) 1 else 2
            if (next > columns) return take(index)
            width = next
        }
        return this
    }

    private fun String.alignRight(columns: Int): String =
        " ".repeat((columns - cells(this)).coerceAtLeast(0)) + this

    private fun String.collapseWhitespace(): String = trim().replace(WHITESPACE, " ")

    private const val ASCII_LIMIT = 0x80
}
