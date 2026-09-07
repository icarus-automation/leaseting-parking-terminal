package com.leaseting.parkingterminal.domain.printing

/**
 * The handheld's 58 mm thermal printer, as the rest of the app sees it.
 *
 * Printing is deliberately not part of recording a transaction. The stub is
 * paper; the stay is the row in leaseting-api. A jam, an empty roll or a
 * printer service that never bound must not stop a vehicle entering or a fee
 * being collected, so every screen records first and prints after, and offers a
 * reprint when the print fails.
 */
interface StubPrinter {
    /** Whether the vendor print service is bound and ready to take a job. */
    val isReady: Boolean

    suspend fun printEntryStub(stub: EntryStub): PrintResult

    suspend fun printExitReceipt(receipt: ExitReceipt): PrintResult
}

/**
 * The entry stub a driver keeps. [ticketCode] is both printed in full and
 * carried in the QR, so a guard can read it back when the scan fails.
 */
data class EntryStub(
    val ticketCode: String,
    val plateNumber: String,
    val vehicleTypeName: String,
    val propertyName: String,
    val terminalName: String,
    val entryAt: String,
)

/** The receipt for a collected exit. Amounts are already formatted for paper. */
data class ExitReceipt(
    val ticketCode: String,
    val plateNumber: String,
    val vehicleTypeName: String,
    val propertyName: String,
    val terminalName: String,
    val entryAt: String,
    val exitAt: String,
    val stayLabel: String,
    val ratePlanName: String,
    val feeAmount: String,
    val amountTendered: String,
    val changeGiven: String,
    val attendantName: String,
)

/**
 * The vendor interface reports nothing back — no status query, no print-success
 * callback — so success here means only that the job was handed to the print
 * service without the binder throwing. Paper is still the guard's problem to
 * look at.
 */
sealed interface PrintResult {
    data object Sent : PrintResult

    /** No print service bound. On the H10S this normally means it is not installed. */
    data object PrinterUnavailable : PrintResult

    /** The binder call threw or the service died mid-job. A reprint may work. */
    data class Failed(val message: String?) : PrintResult
}
