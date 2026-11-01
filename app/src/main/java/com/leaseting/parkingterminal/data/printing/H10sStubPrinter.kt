package com.leaseting.parkingterminal.data.printing

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.os.IBinder
import android.os.RemoteException
import android.util.Log
import com.leaseting.parkingterminal.domain.printing.EntryStub
import com.leaseting.parkingterminal.domain.printing.ExitReceipt
import com.leaseting.parkingterminal.domain.printing.PrintResult
import com.leaseting.parkingterminal.domain.printing.StubPrinter
import com.sr.SrPrinter
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import recieptservice.com.recieptservice.PrinterInterface

/**
 * The H10S integrated 58 mm printer, driven through the vendor
 * `printer.jar`.
 *
 * The binding is process-wide and one-way: `SrPrinter.bindPrinter` starts the
 * vendor service and binds to it, and neither `SrPrinter` nor
 * `PrinterInterface` exposes a shutdown. The connection is held for the life of
 * the application — a terminal that lives at a gate has no moment where letting
 * the printer go would help — so nothing here calls `unbindService`.
 *
 * Only methods that exist in `app/libs/printer.jar` are used, and only after
 * `onServiceConnected`. The vendor interface has no status query and no
 * print-success callback, so a [PrintResult.Sent] means the job reached the
 * service, nothing more. See `docs/integrations/h10s-printer.md`.
 */
class H10sStubPrinter(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher,
) : StubPrinter {

    @Volatile
    private var printerInterface: PrinterInterface? = null

    /** One job at a time: two receipts interleaved on one roll is unreadable. */
    private val printLock = Mutex()

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            printerInterface = PrinterInterface.Stub.asInterface(binder)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            printerInterface = null
        }
    }

    override val isReady: Boolean
        get() = printerInterface != null

    /**
     * Called once from [com.leaseting.parkingterminal.ParkingTerminalApplication].
     * `bindPrinter` returns void and reports neither success nor failure, so the
     * only signal that the printer is usable is [isReady] flipping true.
     */
    fun bind() {
        runCatching { SrPrinter.bindPrinter(context.applicationContext, connection) }
            .onFailure { Log.w(TAG, "Printer service bind failed", it) }
    }

    override suspend fun printEntryStub(stub: EntryStub): PrintResult = print { printer ->
        printer.header(stub.propertyName, stub.terminalName)

        printer.setAlignment(ALIGN_CENTER)
        printer.setTextSize(TEXT_SIZE_TITLE)
        printer.setTextBold(true)
        printer.printText("PARKING STUB\n")
        printer.setTextBold(false)
        printer.setTextSize(TEXT_SIZE_BODY)
        printer.nextLine(1)

        // The QR carries the bare code; the same code is printed underneath it
        // so a creased stub is still redeemable by hand.
        printer.printQRCode(stub.ticketCode, QR_MODULE_SIZE, QR_ERROR_LEVEL)
        printer.nextLine(1)
        printer.setTextSize(TEXT_SIZE_TITLE)
        printer.setTextBold(true)
        printer.printText("${formatTicketCode(stub.ticketCode)}\n")
        printer.setTextBold(false)
        printer.setTextSize(TEXT_SIZE_BODY)
        printer.nextLine(1)

        printer.setAlignment(ALIGN_LEFT)
        printer.row("Plate", stub.plateNumber)
        printer.row("Vehicle", stub.vehicleTypeName)
        printer.row("Time in", stub.entryAt)
        printer.row("Attendant", stub.attendantName)
        printer.rule()

        printer.setAlignment(ALIGN_CENTER)
        printer.printText("Keep this stub.\n")
        printer.printText("Pay on exit. Cash only.\n")
        printer.feedAndReset()
    }

    override suspend fun printExitReceipt(receipt: ExitReceipt): PrintResult = print { printer ->
        printer.header(receipt.propertyName, receipt.terminalName)

        printer.setAlignment(ALIGN_CENTER)
        printer.setTextSize(TEXT_SIZE_TITLE)
        printer.setTextBold(true)
        printer.printText("OFFICIAL RECEIPT\n")
        printer.setTextBold(false)
        printer.setTextSize(TEXT_SIZE_BODY)
        printer.nextLine(1)

        printer.setAlignment(ALIGN_LEFT)
        printer.row("Stub", formatTicketCode(receipt.ticketCode))
        printer.row("Plate", receipt.plateNumber)
        printer.row("Vehicle", receipt.vehicleTypeName)
        printer.row("Time in", receipt.entryAt)
        printer.row("Time out", receipt.exitAt)
        printer.row("Duration", receipt.stayLabel)
        printer.row("Rate", receipt.ratePlanName)
        printer.rule()

        printer.setTextBold(true)
        printer.row("AMOUNT DUE", receipt.feeAmount)
        printer.setTextBold(false)
        printer.row("Cash", receipt.amountTendered)
        printer.row("Change", receipt.changeGiven)
        printer.rule()

        printer.row("Attendant", receipt.attendantName)
        printer.setAlignment(ALIGN_CENTER)
        printer.printText("Thank you.\n")
        printer.feedAndReset()
    }

    /**
     * Runs one job under the lock, off the main thread. A dead binder shows up
     * as [RemoteException] and is reported rather than thrown: the transaction
     * is already recorded in leaseting-api, and losing the paper must not lose
     * the money.
     */
    private suspend fun print(job: (PrinterInterface) -> Unit): PrintResult =
        withContext(ioDispatcher) {
            val printer = printerInterface ?: return@withContext PrintResult.PrinterUnavailable

            printLock.withLock {
                try {
                    printer.beginWork()
                    job(printer)
                    printer.endWork()
                    PrintResult.Sent
                } catch (e: RemoteException) {
                    Log.w(TAG, "Print job failed", e)
                    PrintResult.Failed(e.message)
                } catch (e: IllegalStateException) {
                    Log.w(TAG, "Print job rejected", e)
                    PrintResult.Failed(e.message)
                }
            }
        }

    private fun PrinterInterface.header(propertyName: String, terminalName: String) {
        setLineHeight(LINE_HEIGHT)
        setAlignment(ALIGN_CENTER)
        setTextSize(TEXT_SIZE_BODY)
        setTextBold(true)
        printText("$propertyName\n")
        setTextBold(false)
        printText("$terminalName\n")
        rule()
    }

    /**
     * A label and its value on one line, laid out by [ReceiptLayout] rather
     * than by the vendor's `printTableText`. That primitive breaks a value
     * mid-token the moment it outgrows its column, which is what put a time on
     * the paper as `7:` / `46 PM`. See the layout's own notes.
     */
    private fun PrinterInterface.row(label: String, value: String) {
        setAlignment(ALIGN_LEFT)
        ReceiptLayout.row(label, value).forEach { line -> printText("$line\n") }
    }

    private fun PrinterInterface.rule() {
        setAlignment(ALIGN_LEFT)
        printText("${ReceiptLayout.rule()}\n")
    }

    /** Enough blank lines that the tear-off edge clears the print head. */
    private fun PrinterInterface.feedAndReset() {
        nextLine(FEED_LINES)
        setAlignment(ALIGN_LEFT)
        setTextBold(false)
        setTextSize(TEXT_SIZE_BODY)
    }

    private fun formatTicketCode(code: String): String =
        if (code.length == TICKET_CODE_LENGTH) {
            "${code.take(TICKET_CODE_GROUP)}-${code.drop(TICKET_CODE_GROUP)}"
        } else {
            code
        }

    private companion object {
        const val TAG = "H10sStubPrinter"

        // Documented in the vendor reference: 0 left, 1 centre, 2 right.
        const val ALIGN_LEFT = 0
        const val ALIGN_CENTER = 1

        /**
         * Documented ranges are modulesize 1-16 and errorlevel 0-3. An 8-character
         * code is tiny, so the largest practical module that still fits 58 mm is
         * used with the highest correction — a stub spends its life folded in a
         * car door. Confirm on a physical H10S before changing these.
         */
        const val QR_MODULE_SIZE = 8
        const val QR_ERROR_LEVEL = 3

        const val TEXT_SIZE_BODY = 24f
        const val TEXT_SIZE_TITLE = 32f
        const val LINE_HEIGHT = 30f

        const val FEED_LINES = 4

        const val TICKET_CODE_LENGTH = 8
        const val TICKET_CODE_GROUP = 4
    }
}
