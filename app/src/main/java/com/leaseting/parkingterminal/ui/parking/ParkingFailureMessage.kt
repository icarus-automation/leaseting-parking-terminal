package com.leaseting.parkingterminal.ui.parking

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.leaseting.parkingterminal.R
import com.leaseting.parkingterminal.domain.parking.ParkingFailure
import com.leaseting.parkingterminal.domain.parking.ParkingResult

/**
 * What the guard is told when a call fails.
 *
 * The API's own sentence wins whenever it sent one. It knows things the
 * terminal does not — which vehicle type is unpriced, which attendant holds the
 * gate, how short the tender was — and a guard at a barrier needs the specific
 * answer, not a category. The per-reason strings are the fallback for the cases
 * where there is no body to read: a dropped connection, a malformed response.
 */
@Composable
fun ParkingResult.Failure.userMessage(): String =
    message?.takeIf { it.isNotBlank() } ?: stringResource(reason.fallbackMessageRes)

@get:StringRes
private val ParkingFailure.fallbackMessageRes: Int
    get() = when (this) {
        ParkingFailure.Unreachable -> R.string.parking_error_unreachable
        ParkingFailure.NotFound -> R.string.parking_error_not_found
        ParkingFailure.Conflict -> R.string.parking_error_conflict
        ParkingFailure.Rejected -> R.string.parking_error_rejected
        ParkingFailure.ServerError -> R.string.parking_error_server
    }
