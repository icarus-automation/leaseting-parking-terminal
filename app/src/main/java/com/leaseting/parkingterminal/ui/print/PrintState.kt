package com.leaseting.parkingterminal.ui.print

import androidx.annotation.StringRes
import com.leaseting.parkingterminal.R
import com.leaseting.parkingterminal.domain.printing.PrintResult

/**
 * How far along a print job is, as a screen sees it.
 *
 * [Sent] is as far as the vendor SDK lets anyone go: it reports no completion
 * and no paper status, so the screen says the job went to the printer and
 * leaves a reprint within reach. Nothing here ever blocks the transaction — by
 * the time a print starts, the stay is already recorded in leaseting-api.
 */
sealed interface PrintState {
    data object Idle : PrintState
    data object Printing : PrintState
    data object Sent : PrintState
    data class Failed(@param:StringRes val messageRes: Int, val detail: String?) : PrintState
}

fun PrintResult.toPrintState(): PrintState = when (this) {
    PrintResult.Sent -> PrintState.Sent
    PrintResult.PrinterUnavailable ->
        PrintState.Failed(R.string.print_error_unavailable, null)

    is PrintResult.Failed -> PrintState.Failed(R.string.print_error_failed, message)
}
