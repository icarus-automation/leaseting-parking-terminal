package com.leaseting.parkingterminal.data.parking

import com.leaseting.parkingterminal.data.parking.remote.ParkingApi
import com.leaseting.parkingterminal.domain.auth.SessionState
import com.leaseting.parkingterminal.domain.parking.ParkingFailure
import com.leaseting.parkingterminal.domain.parking.ParkingRepository
import com.leaseting.parkingterminal.domain.parking.ParkingResult
import com.leaseting.parkingterminal.domain.parking.ParkingSessionStatus
import com.leaseting.parkingterminal.domain.parking.ShiftState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.math.BigDecimal

/**
 * The gate loop, driven through real HTTP.
 *
 * The cases that matter here are the ones that decide money and paper: that the
 * terminal sends the shift it actually holds, that it never invents a fee, and
 * that a rejection from leaseting-api reaches the guard in the API's own words
 * rather than as a shrug.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DefaultParkingRepositoryTest {

    private lateinit var server: MockWebServer
    private lateinit var parkingApi: ParkingApi

    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
        parkingApi = Retrofit.Builder()
            .baseUrl(server.url("/api/v1/"))
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(ParkingApi::class.java)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `reports no shift when the attendant holds none`() = runTest {
        server.enqueue(envelope("null"))
        val repository = repository()

        val result = repository.refreshShift()

        assertTrue(result is ParkingResult.Success)
        assertNull((result as ParkingResult.Success).value)
        assertEquals(ShiftState.None, repository.shiftState.value)
    }

    @Test
    fun `restores the shift a restarted handheld already holds`() = runTest {
        server.enqueue(envelope(OPEN_SHIFT_JSON))
        val repository = repository()

        repository.refreshShift()

        val state = repository.shiftState.value
        assertTrue(state is ShiftState.Open)
        assertEquals("shift-1", (state as ShiftState.Open).shift.id)
        assertEquals("Basement", state.shift.terminalName)
    }

    @Test
    fun `a declared shift is not somewhere transactions can be recorded`() = runTest {
        server.enqueue(envelope(OPEN_SHIFT_JSON.replace("\"OPEN\"", "\"DECLARED\"")))
        val repository = repository()

        repository.refreshShift()

        assertEquals(ShiftState.None, repository.shiftState.value)
    }

    @Test
    fun `opening a shift publishes it to every screen`() = runTest {
        server.enqueue(envelope(OPEN_SHIFT_JSON))
        val repository = repository()

        repository.openShift("term-1")

        assertTrue(repository.shiftState.value is ShiftState.Open)
        assertEquals("""{"terminalId":"term-1"}""", server.takeRequest().body.readUtf8())
    }

    @Test
    fun `records an entry against the shift the terminal holds`() = runTest {
        val repository = repositoryWithOpenShift()
        server.enqueue(envelope(OPEN_SESSION_JSON))

        val result = repository.recordEntry(vehicleTypeId = "veh-1", plateNumber = " abc 1234 ")

        assertTrue(result is ParkingResult.Success)
        assertEquals("A3KD9MPQ", (result as ParkingResult.Success).value.ticketCode)

        val body = server.takeRequest().body.readUtf8()
        assertTrue(body.contains("\"shiftId\":\"shift-1\""))
        assertTrue(body.contains("\"vehicleTypeId\":\"veh-1\""))
        // Trimmed, and sent as typed: the API owns the rest of the normalising.
        assertTrue(body.contains("\"plateNumber\":\"abc 1234\""))
    }

    @Test
    fun `refuses to record anything without an open shift`() = runTest {
        val repository = repository()

        val result = repository.recordEntry(vehicleTypeId = "veh-1", plateNumber = "ABC 1234")

        assertEquals(ParkingFailure.Conflict, (result as ParkingResult.Failure).reason)
        // Nothing was sent: a stub with no shift to hang off is not a request
        // worth making.
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `scopes the in-park list to the terminal being worked`() = runTest {
        val repository = repositoryWithOpenShift()
        server.enqueue(envelope("[]"))

        repository.openSessions()

        val path = server.takeRequest().path.orEmpty()
        assertTrue(path.contains("terminalId=term-1"))
    }

    @Test
    fun `searches for a plate across every gate`() = runTest {
        val repository = repositoryWithOpenShift()
        server.enqueue(envelope("[$OPEN_SESSION_JSON]"))

        val result = repository.openSessions(plate = " 1234 ")

        assertTrue(result is ParkingResult.Success)
        val path = server.takeRequest().path.orEmpty()
        assertTrue(path.contains("plate=1234"))
        // A stub lost on a vehicle that entered elsewhere still has to be found.
        assertTrue(!path.contains("terminalId"))
    }

    @Test
    fun `resolves a scanned code through the dash and case a stub is printed with`() = runTest {
        val repository = repository()
        server.enqueue(envelope(OPEN_SESSION_JSON))

        repository.findByTicketCode("a3kd-9mpq\n")

        assertTrue(server.takeRequest().path.orEmpty().endsWith("/parking/sessions/by-code/A3KD9MPQ"))
    }

    @Test
    fun `answers a malformed scan without asking the API`() = runTest {
        val repository = repository()

        val result = repository.findByTicketCode("!!")

        assertEquals(ParkingFailure.NotFound, (result as ParkingResult.Failure).reason)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `takes the fee from the API rather than computing one`() = runTest {
        val repository = repository()
        server.enqueue(envelope(FEE_QUOTE_JSON))

        val result = repository.previewFee("sess-1")

        val quote = (result as ParkingResult.Success).value
        assertEquals(BigDecimal("150.00"), quote.feeAmount)
        assertEquals(150, quote.stayMinutes)
        assertEquals(15, quote.graceMinutes)
        assertEquals("Hourly", quote.ratePlanName)
    }

    @Test
    fun `posts cash against the API fee and never sends a fee of its own`() = runTest {
        val repository = repositoryWithOpenShift()
        server.enqueue(envelope(PAID_SESSION_JSON))

        val result = repository.payExit("sess-1", BigDecimal("200"))

        val paid = (result as ParkingResult.Success).value
        assertEquals(ParkingSessionStatus.PAID, paid.status)
        assertEquals(BigDecimal("50.00"), paid.changeGiven)

        val body = server.takeRequest().body.readUtf8()
        assertTrue(body.contains("\"shiftId\":\"shift-1\""))
        assertTrue(body.contains("\"amountTendered\":\"200\""))
        assertTrue(!body.contains("feeAmount"))
    }

    @Test
    fun `passes the API's own rejection through to the guard`() = runTest {
        val repository = repositoryWithOpenShift()
        server.enqueue(
            MockResponse().setResponseCode(400).setBody(
                """{"statusCode":400,"message":"Cash tendered is less than the 150.00 fee","error":"Bad Request"}""",
            ),
        )

        val result = repository.payExit("sess-1", BigDecimal("100"))

        val failure = result as ParkingResult.Failure
        assertEquals(ParkingFailure.Rejected, failure.reason)
        assertEquals("Cash tendered is less than the 150.00 fee", failure.message)
    }

    @Test
    fun `flattens a validation error list into one line`() = runTest {
        val repository = repositoryWithOpenShift()
        server.enqueue(
            MockResponse().setResponseCode(400).setBody(
                """{"statusCode":400,"message":["plateNumber must be longer","shiftId must be a UUID"],"error":"Bad Request"}""",
            ),
        )

        val result = repository.recordEntry("veh-1", "A")

        assertEquals(
            "plateNumber must be longer shiftId must be a UUID",
            (result as ParkingResult.Failure).message,
        )
    }

    @Test
    fun `reads a double collection as a conflict, not a failure to retry`() = runTest {
        val repository = repositoryWithOpenShift()
        server.enqueue(
            MockResponse().setResponseCode(409).setBody(
                """{"statusCode":409,"message":"This stub has already been paid","error":"Conflict"}""",
            ),
        )

        val result = repository.payExit("sess-1", BigDecimal("200"))

        assertEquals(ParkingFailure.Conflict, (result as ParkingResult.Failure).reason)
    }

    @Test
    fun `reports an unreachable gate as unreachable`() = runTest {
        val repository = repository()
        server.enqueue(MockResponse().apply { socketPolicy = SocketPolicy.DISCONNECT_AT_START })

        val result = repository.previewFee("sess-1")

        assertEquals(ParkingFailure.Unreachable, (result as ParkingResult.Failure).reason)
    }

    @Test
    fun `declaring closes the shift on the handheld`() = runTest {
        val repository = repositoryWithOpenShift()
        server.enqueue(envelope(DECLARED_SHIFT_JSON))

        val result = repository.declareShift(BigDecimal("1830"), "  short by 20  ")

        val shift = (result as ParkingResult.Success).value
        assertEquals(BigDecimal("1850.00"), shift.expectedCash)
        assertEquals(BigDecimal("1830.00"), shift.declaredCash)
        assertEquals(ShiftState.None, repository.shiftState.value)

        val body = server.takeRequest().body.readUtf8()
        assertTrue(body.contains("\"declaredCash\":\"1830\""))
        assertTrue(body.contains("\"note\":\"short by 20\""))
    }

    @Test
    fun `drops the shift when the session ends`() = runTest {
        val sessionState = MutableStateFlow<SessionState>(
            SessionState.SignedIn(
                com.leaseting.parkingterminal.domain.auth.Attendant("u1", "Guard", "g@x.com"),
            ),
        )
        server.enqueue(envelope(OPEN_SHIFT_JSON))
        val repository = repository(sessionState)
        repository.refreshShift()
        assertTrue(repository.shiftState.value is ShiftState.Open)

        // Covers the involuntary kind too: UnauthorizedInterceptor clears the
        // session on a revoked token, and the till must not survive it.
        sessionState.value = SessionState.SignedOut

        assertEquals(ShiftState.Unknown, repository.shiftState.value)
    }

    private fun TestScope.repository(
        sessionState: MutableStateFlow<SessionState> = MutableStateFlow(SessionState.Restoring),
    ): ParkingRepository = DefaultParkingRepository(
        parkingApi = parkingApi,
        json = json,
        ioDispatcher = UnconfinedTestDispatcher(testScheduler),
        sessionState = sessionState,
        externalScope = TestScope(UnconfinedTestDispatcher(testScheduler)),
    )

    /** A repository that already holds shift-1 at term-1, as a working gate does. */
    private suspend fun TestScope.repositoryWithOpenShift(): ParkingRepository {
        server.enqueue(envelope(OPEN_SHIFT_JSON))
        return repository().also {
            it.refreshShift()
            server.takeRequest()
        }
    }

    private fun envelope(data: String) = MockResponse()
        .setResponseCode(200)
        .setBody("""{"statusCode":200,"message":"Success","data":$data}""")

    private companion object {
        const val OPEN_SHIFT_JSON = """
            {
              "id": "shift-1",
              "status": "OPEN",
              "openedAt": "2026-09-07T06:00:00.000Z",
              "terminalId": "term-1",
              "terminalName": "Basement",
              "propertyName": "Brickstone"
            }
        """

        const val DECLARED_SHIFT_JSON = """
            {
              "id": "shift-1",
              "status": "DECLARED",
              "openedAt": "2026-09-07T06:00:00.000Z",
              "terminalId": "term-1",
              "terminalName": "Basement",
              "propertyName": "Brickstone",
              "expectedCash": "1850.00",
              "declaredCash": "1830.00"
            }
        """

        const val OPEN_SESSION_JSON = """
            {
              "id": "sess-1",
              "ticketCode": "A3KD9MPQ",
              "plateNumber": "ABC 1234",
              "status": "OPEN",
              "entryAt": "2026-09-07T02:00:00.000Z",
              "terminalName": "Basement",
              "vehicleTypeName": "Car"
            }
        """

        const val PAID_SESSION_JSON = """
            {
              "id": "sess-1",
              "ticketCode": "A3KD9MPQ",
              "plateNumber": "ABC 1234",
              "status": "PAID",
              "entryAt": "2026-09-07T02:00:00.000Z",
              "exitAt": "2026-09-07T04:30:00.000Z",
              "terminalName": "Basement",
              "vehicleTypeName": "Car",
              "ratePlanName": "Hourly",
              "feeAmount": "150.00",
              "amountTendered": "200.00",
              "changeGiven": "50.00"
            }
        """

        const val FEE_QUOTE_JSON = """
            {
              "sessionId": "sess-1",
              "ticketCode": "A3KD9MPQ",
              "plateNumber": "ABC 1234",
              "vehicleTypeName": "Car",
              "entryAt": "2026-09-07T02:00:00.000Z",
              "quotedAt": "2026-09-07T04:30:00.000Z",
              "stayMinutes": 150,
              "billedMinutes": 135,
              "graceMinutes": 15,
              "ratePlanName": "Hourly",
              "feeAmount": "150.00"
            }
        """
    }
}
