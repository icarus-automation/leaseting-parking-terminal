package com.leaseting.parkingterminal.data.parking

import com.leaseting.parkingterminal.core.network.ApiEnvelope
import com.leaseting.parkingterminal.core.network.ApiErrorResponse
import com.leaseting.parkingterminal.core.network.NullableApiEnvelope
import com.leaseting.parkingterminal.data.parking.dto.CreateSessionRequest
import com.leaseting.parkingterminal.data.parking.dto.DeclareShiftRequest
import com.leaseting.parkingterminal.data.parking.dto.ExitSessionRequest
import com.leaseting.parkingterminal.data.parking.dto.FeeQuoteDto
import com.leaseting.parkingterminal.data.parking.dto.OpenShiftRequest
import com.leaseting.parkingterminal.data.parking.dto.ParkingSessionDto
import com.leaseting.parkingterminal.data.parking.dto.ParkingShiftDto
import com.leaseting.parkingterminal.data.parking.remote.ParkingApi
import com.leaseting.parkingterminal.domain.auth.SessionState
import com.leaseting.parkingterminal.domain.parking.FeeQuote
import com.leaseting.parkingterminal.domain.parking.ParkingCatalog
import com.leaseting.parkingterminal.domain.parking.ParkingFailure
import com.leaseting.parkingterminal.domain.parking.ParkingRepository
import com.leaseting.parkingterminal.domain.parking.ParkingResult
import com.leaseting.parkingterminal.domain.parking.ParkingSession
import com.leaseting.parkingterminal.domain.parking.ParkingSessionStatus
import com.leaseting.parkingterminal.domain.parking.ParkingShift
import com.leaseting.parkingterminal.domain.parking.ParkingShiftStatus
import com.leaseting.parkingterminal.domain.parking.ParkingTerminal
import com.leaseting.parkingterminal.domain.parking.ShiftState
import com.leaseting.parkingterminal.domain.parking.ShiftSummary
import com.leaseting.parkingterminal.domain.parking.VehicleType
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import retrofit2.Response
import java.io.IOException
import java.math.BigDecimal
import java.net.HttpURLConnection.HTTP_CONFLICT
import java.net.HttpURLConnection.HTTP_NOT_FOUND
import java.time.Instant

/**
 * The gate loop against leaseting-api.
 *
 * Two rules shape this class:
 *
 * 1. No money is computed here. The API prices a stay, the terminal shows that
 *    number and posts cash against it. Staff reprice plans in the web client
 *    whenever they like, and the only way a handheld can be right about a fee
 *    is to ask at the moment it collects.
 * 2. The shift is the one piece of shared state, and it is republished on every
 *    call that could change it, so no screen has to be told twice.
 */
class DefaultParkingRepository(
    private val parkingApi: ParkingApi,
    private val json: Json,
    private val ioDispatcher: CoroutineDispatcher,
    sessionState: StateFlow<SessionState>,
    externalScope: CoroutineScope,
) : ParkingRepository {

    private val _shiftState = MutableStateFlow<ShiftState>(ShiftState.Unknown)
    override val shiftState: StateFlow<ShiftState> = _shiftState.asStateFlow()

    init {
        // A shift belongs to whoever is signed in. Signing out — including the
        // involuntary kind, when the API rejects a revoked token — must not
        // leave the next guard looking at the last one's till.
        sessionState
            .onEach { state ->
                if (state is SessionState.SignedOut) _shiftState.value = ShiftState.Unknown
            }
            .launchIn(externalScope)
    }

    override suspend fun loadCatalog(): ParkingResult<ParkingCatalog> = call {
        val body = parkingApi.catalog().unwrap()
        ParkingCatalog(
            terminals = body.terminals.map {
                ParkingTerminal(
                    id = it.id,
                    name = it.name,
                    isActive = it.isActive,
                    propertyId = it.propertyId,
                    propertyName = it.propertyName,
                    propertyIsArchived = it.propertyIsArchived,
                )
            },
            vehicleTypes = body.vehicleTypes.map {
                VehicleType(id = it.id, name = it.name, isArchived = it.isArchived)
            },
        )
    }

    override suspend fun refreshShift(): ParkingResult<ParkingShift?> = call {
        val shift = parkingApi.currentShift().unwrapNullable()?.toShift()
        _shiftState.value = shift.toShiftState()
        shift
    }

    override suspend fun openShift(terminalId: String): ParkingResult<ParkingShift> = call {
        val shift = parkingApi.openShift(OpenShiftRequest(terminalId)).unwrap().toShift()
        _shiftState.value = shift.toShiftState()
        shift
    }

    override suspend fun recordEntry(
        vehicleTypeId: String,
        plateNumber: String,
    ): ParkingResult<ParkingSession> {
        val shiftId = requireOpenShiftId() ?: return noShift()
        return call {
            parkingApi.createSession(
                CreateSessionRequest(
                    shiftId = shiftId,
                    vehicleTypeId = vehicleTypeId,
                    plateNumber = plateNumber.trim(),
                ),
            ).unwrap().toSession()
        }
    }

    /**
     * The in-park list is scoped to the terminal the guard is standing at,
     * because that is the queue in front of them. A plate search is not: a stub
     * lost on a vehicle that entered by another gate still has to be findable,
     * and each row names the terminal it entered at.
     */
    override suspend fun openSessions(plate: String?): ParkingResult<List<ParkingSession>> {
        val search = plate?.trim()?.takeIf { it.isNotEmpty() }
        val terminalId = if (search == null) currentShift()?.terminalId else null

        return call {
            parkingApi.sessions(terminalId = terminalId, plate = search)
                .unwrap()
                .map { it.toSession() }
        }
    }

    override suspend fun findByTicketCode(code: String): ParkingResult<ParkingSession> {
        val normalized = normalizeTicketCode(code)
            ?: return ParkingResult.Failure(ParkingFailure.NotFound, NO_SUCH_STUB)

        return call { parkingApi.sessionByCode(normalized).unwrap().toSession() }
    }

    override suspend fun previewFee(sessionId: String): ParkingResult<FeeQuote> = call {
        parkingApi.feePreview(sessionId).unwrap().toQuote()
    }

    override suspend fun payExit(
        sessionId: String,
        amountTendered: BigDecimal,
    ): ParkingResult<ParkingSession> {
        val shiftId = requireOpenShiftId() ?: return noShift()
        return call {
            parkingApi.payExit(
                sessionId,
                ExitSessionRequest(
                    shiftId = shiftId,
                    amountTendered = amountTendered.toPlainString(),
                ),
            ).unwrap().toSession()
        }
    }

    override suspend fun shiftSummary(): ParkingResult<ShiftSummary> {
        val shiftId = requireOpenShiftId() ?: return noShift()
        return call {
            val dto = parkingApi.shiftSummary(shiftId).unwrap()
            ShiftSummary(
                shiftId = dto.shiftId,
                entryCount = dto.entryCount,
                paidExitCount = dto.paidExitCount,
                expectedCash = dto.expectedCash.toAmount() ?: BigDecimal.ZERO,
            )
        }
    }

    override suspend fun declareShift(
        declaredCash: BigDecimal,
        note: String?,
    ): ParkingResult<ParkingShift> {
        val shiftId = requireOpenShiftId() ?: return noShift()
        return call {
            val shift = parkingApi.declareShift(
                shiftId,
                DeclareShiftRequest(
                    declaredCash = declaredCash.toPlainString(),
                    note = note?.trim()?.takeIf { it.isNotEmpty() },
                ),
            ).unwrap().toShift()
            // Declared, so no longer somewhere transactions can be recorded.
            _shiftState.value = ShiftState.None
            shift
        }
    }

    private fun currentShift(): ParkingShift? = (_shiftState.value as? ShiftState.Open)?.shift

    private fun requireOpenShiftId(): String? = currentShift()?.id

    private fun <T> noShift(): ParkingResult<T> =
        ParkingResult.Failure(ParkingFailure.Conflict, NO_OPEN_SHIFT)

    /**
     * Turns transport and decoding failures into a [ParkingFailure].
     * Cancellation is not an error, so the catches stay narrow and let it
     * propagate.
     */
    private suspend fun <T> call(block: suspend () -> T): ParkingResult<T> =
        withContext(ioDispatcher) {
            try {
                ParkingResult.Success(block())
            } catch (e: ApiRejection) {
                ParkingResult.Failure(e.reason, e.apiMessage)
            } catch (_: IOException) {
                ParkingResult.Failure(ParkingFailure.Unreachable)
            } catch (_: SerializationException) {
                ParkingResult.Failure(ParkingFailure.ServerError)
            }
        }

    /**
     * The envelope's payload, or the API's own rejection turned into something
     * the screens can show. A 4xx body carries a sentence written for the person
     * holding the handheld, so it is read rather than discarded.
     */
    private fun <T> Response<ApiEnvelope<T>>.unwrap(): T {
        if (isSuccessful) {
            val envelope = body() ?: throw ApiRejection(ParkingFailure.ServerError, null)
            return envelope.data
        }
        throw rejection()
    }

    /** The same, for the one route whose payload is legitimately absent. */
    private fun <T> Response<NullableApiEnvelope<T>>.unwrapNullable(): T? {
        if (isSuccessful) {
            val envelope = body() ?: throw ApiRejection(ParkingFailure.ServerError, null)
            return envelope.data
        }
        throw rejection()
    }

    private fun Response<*>.rejection(): ApiRejection {
        val message = errorBody()
            ?.let { runCatching { json.decodeFromString<ApiErrorResponse>(it.string()) }.getOrNull() }
            ?.message
            ?.text
            ?.takeIf { it.isNotBlank() }

        return ApiRejection(reasonFor(code()), message)
    }

    private fun reasonFor(code: Int): ParkingFailure = when (code) {
        HTTP_NOT_FOUND -> ParkingFailure.NotFound
        HTTP_CONFLICT -> ParkingFailure.Conflict
        in CLIENT_ERROR_RANGE -> ParkingFailure.Rejected
        else -> ParkingFailure.ServerError
    }

    private class ApiRejection(
        val reason: ParkingFailure,
        val apiMessage: String?,
    ) : Exception(apiMessage)

    private fun ParkingShift?.toShiftState(): ShiftState = when {
        this == null -> ShiftState.None
        status == ParkingShiftStatus.OPEN -> ShiftState.Open(this)
        else -> ShiftState.None
    }

    private fun ParkingShiftDto.toShift() = ParkingShift(
        id = id,
        status = parseShiftStatus(status),
        openedAt = openedAt.toInstant(),
        terminalId = terminalId,
        terminalName = terminalName,
        propertyName = propertyName,
        expectedCash = expectedCash?.toAmount(),
        declaredCash = declaredCash?.toAmount(),
    )

    private fun ParkingSessionDto.toSession() = ParkingSession(
        id = id,
        ticketCode = ticketCode,
        plateNumber = plateNumber,
        status = parseSessionStatus(status),
        entryAt = entryAt.toInstant(),
        exitAt = exitAt?.toInstant(),
        terminalName = terminalName,
        vehicleTypeName = vehicleTypeName,
        ratePlanName = ratePlanName,
        feeAmount = feeAmount?.toAmount(),
        amountTendered = amountTendered?.toAmount(),
        changeGiven = changeGiven?.toAmount(),
        entryAttendantName = entryAttendantDisplayName?.takeIf { it.isNotBlank() },
        exitAttendantName = exitAttendantDisplayName?.takeIf { it.isNotBlank() },
    )

    private fun FeeQuoteDto.toQuote() = FeeQuote(
        sessionId = sessionId,
        ticketCode = ticketCode,
        plateNumber = plateNumber,
        vehicleTypeName = vehicleTypeName,
        entryAt = entryAt.toInstant(),
        quotedAt = quotedAt.toInstant(),
        stayMinutes = stayMinutes,
        billedMinutes = billedMinutes,
        graceMinutes = graceMinutes,
        ratePlanName = ratePlanName,
        feeAmount = feeAmount.toAmount() ?: BigDecimal.ZERO,
    )

    private companion object {
        val CLIENT_ERROR_RANGE = 400..499

        const val NO_OPEN_SHIFT = "Open a shift before recording anything."
        const val NO_SUCH_STUB = "No stub matches that code."
    }
}

/**
 * Prisma serialises timestamps as ISO-8601 in UTC. A value this build cannot
 * read falls back to the epoch rather than crashing a gate: the screen shows an
 * obviously wrong time, which a guard can report, instead of nothing at all.
 */
private fun String.toInstant(): Instant =
    runCatching { Instant.parse(this) }.getOrDefault(Instant.EPOCH)

/** Prisma Decimal arrives as a string. Null on anything that is not a number. */
private fun String.toAmount(): BigDecimal? = runCatching { BigDecimal(this) }.getOrNull()

private fun parseShiftStatus(value: String): ParkingShiftStatus =
    runCatching { ParkingShiftStatus.valueOf(value) }.getOrDefault(ParkingShiftStatus.UNKNOWN)

private fun parseSessionStatus(value: String): ParkingSessionStatus =
    runCatching { ParkingSessionStatus.valueOf(value) }.getOrDefault(ParkingSessionStatus.UNKNOWN)

/**
 * What a scan or a typed code becomes before it is sent.
 *
 * A keyboard-wedge scanner appends whitespace and a guard copies the dash off
 * the stub, so both go; the API applies the same rule. Anything that is not a
 * whole code returns null, which the caller answers as "no such stub" rather
 * than sending to the API.
 */
internal fun normalizeTicketCode(raw: String): String? {
    val normalized = raw.uppercase().replace(Regex("[^0-9A-Z]"), "")
    return normalized.takeIf { it.length == TICKET_CODE_LENGTH }
}

internal const val TICKET_CODE_LENGTH = 8
