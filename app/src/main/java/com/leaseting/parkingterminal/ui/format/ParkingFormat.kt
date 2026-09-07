package com.leaseting.parkingterminal.ui.format

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * How the terminal writes money, times and durations.
 *
 * Fixed to [Locale.ROOT] and Philippine pesos rather than the device locale: a
 * receipt printed at a gate has to read the same on every handheld, and the
 * figures on the screen have to be the ones on the paper. Times use the
 * device's zone, which at a Manila gate is the zone the API's own overnight
 * windows are evaluated in.
 */
private val amountFormat = DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale.ROOT))

private val timeFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd MMM yyyy, h:mm a", Locale.ROOT)

const val PESO_SIGN = "₱"

/** `₱1,234.50`. Always two decimals: a fee is a price, not a measurement. */
fun formatAmount(amount: BigDecimal): String =
    PESO_SIGN + amountFormat.format(amount.setScale(2, RoundingMode.HALF_UP))

/** The same figure without the sign, for a column that already has one. */
fun formatAmountPlain(amount: BigDecimal): String =
    amountFormat.format(amount.setScale(2, RoundingMode.HALF_UP))

fun formatTime(instant: Instant): String =
    timeFormatter.format(instant.atZone(ZoneId.systemDefault()))

/**
 * `2h 30m`, or `45m` under the hour. Minutes only — a guard reading a duration
 * off a stub does not care about seconds, and the API bills in whole minutes.
 */
fun formatDuration(minutes: Int): String {
    val safe = minutes.coerceAtLeast(0)
    val hours = safe / MINUTES_PER_HOUR
    val remainder = safe % MINUTES_PER_HOUR
    return when {
        hours == 0 -> "${remainder}m"
        remainder == 0 -> "${hours}h"
        else -> "${hours}h ${remainder}m"
    }
}

/** How long a vehicle has been in, for a row in the in-park list. */
fun formatElapsed(since: Instant, now: Instant = Instant.now()): String =
    formatDuration(Duration.between(since, now).toMinutes().coerceAtLeast(0).toInt())

/** `A3KD-9MPQ` — the grouping a stub is printed with, so it reads back the same. */
fun formatTicketCode(code: String): String =
    if (code.length == TICKET_CODE_LENGTH) {
        "${code.take(TICKET_CODE_GROUP)}-${code.drop(TICKET_CODE_GROUP)}"
    } else {
        code
    }

private const val MINUTES_PER_HOUR = 60
private const val TICKET_CODE_LENGTH = 8
private const val TICKET_CODE_GROUP = 4
