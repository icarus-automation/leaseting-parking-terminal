package com.leaseting.parkingterminal.data.auth

import com.leaseting.parkingterminal.data.auth.local.StoredSession
import com.leaseting.parkingterminal.data.auth.remote.AuthApi
import com.leaseting.parkingterminal.domain.auth.SessionState
import com.leaseting.parkingterminal.domain.auth.SignInFailure
import com.leaseting.parkingterminal.domain.auth.SignInResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
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

/**
 * The audience gate is what makes this app a *parking* terminal rather than a
 * second front door to Leaseting, so these tests drive it through real HTTP.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DefaultAuthRepositoryTest {

    private lateinit var server: MockWebServer
    private lateinit var authApi: AuthApi
    private lateinit var sessionStore: FakeSessionStore

    private val json = Json { ignoreUnknownKeys = true }

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
        authApi = Retrofit.Builder()
            .baseUrl(server.url("/api/v1/"))
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(AuthApi::class.java)
        sessionStore = FakeSessionStore()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `stores the session when the account is a parking attendant`() = runTest {
        server.enqueue(signInResponse(token = "session-token"))
        server.enqueue(currentUserResponse(audience = "parking"))

        val result = repository().signIn("parkingattendant@leaseting.com", "parking123")

        assertEquals(SignInResult.Success, result)
        assertEquals(
            StoredSession(
                token = "session-token",
                attendantId = "user-1",
                attendantName = "Parking Attendant",
                attendantEmail = "parkingattendant@leaseting.com",
            ),
            sessionStore.stored,
        )
    }

    @Test
    fun `refuses a staff account and keeps nothing on the device`() = runTest {
        server.enqueue(signInResponse(token = "session-token"))
        server.enqueue(currentUserResponse(audience = "staff"))
        server.enqueue(MockResponse().setResponseCode(200).setBody("{}"))

        val result = repository().signIn("admin@leaseting.com", "admin123")

        assertEquals(SignInResult.Failure(SignInFailure.NotAParkingAccount), result)
        assertNull(sessionStore.stored)
    }

    @Test
    fun `revokes the token of an account it turns away`() = runTest {
        server.enqueue(signInResponse(token = "session-token"))
        server.enqueue(currentUserResponse(audience = "tenant"))
        server.enqueue(MockResponse().setResponseCode(200).setBody("{}"))

        repository().signIn("juan.delacruz@gmail.com", "juan101rent")

        server.takeRequest() // sign-in
        server.takeRequest() // users/me
        val revoke = server.takeRequest()
        assertEquals("/api/v1/auth/sign-out", revoke.path)
        assertEquals("Bearer session-token", revoke.getHeader("Authorization"))
    }

    @Test
    fun `reports invalid credentials when the api rejects the password`() = runTest {
        server.enqueue(MockResponse().setResponseCode(401).setBody("""{"message":"nope"}"""))

        val result = repository().signIn("parkingattendant@leaseting.com", "wrong")

        assertEquals(SignInResult.Failure(SignInFailure.InvalidCredentials), result)
        assertNull(sessionStore.stored)
    }

    @Test
    fun `reports the api unreachable rather than blaming the password`() = runTest {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))

        val result = repository().signIn("parkingattendant@leaseting.com", "parking123")

        assertEquals(SignInResult.Failure(SignInFailure.Unreachable), result)
    }

    @Test
    fun `treats a sign-in without a token as a server error`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"redirect":false}"""))

        val result = repository().signIn("parkingattendant@leaseting.com", "parking123")

        assertEquals(SignInResult.Failure(SignInFailure.ServerError), result)
        assertNull(sessionStore.stored)
    }

    @Test
    fun `falls back to the token in the sign-in body when the header is stripped`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody("""{"token":"body-token"}"""),
        )
        server.enqueue(currentUserResponse(audience = "parking"))

        repository().signIn("parkingattendant@leaseting.com", "parking123")

        assertEquals("body-token", sessionStore.stored?.token)
    }

    @Test
    fun `signing out clears the device even when the api cannot be reached`() = runTest {
        sessionStore.save(storedSession())
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))

        repository().signOut()

        assertNull(sessionStore.stored)
    }

    @Test
    fun `reports a stored session as signed in`() = runTest {
        sessionStore.save(storedSession())

        val state = repository().sessionState.first { it !is SessionState.Restoring }

        assertTrue(state is SessionState.SignedIn)
        assertEquals(
            "parkingattendant@leaseting.com",
            (state as SessionState.SignedIn).attendant.email,
        )
    }

    private fun TestScope.repository() = DefaultAuthRepository(
        authApi = authApi,
        sessionStore = sessionStore,
        ioDispatcher = UnconfinedTestDispatcher(testScheduler),
        externalScope = backgroundScope,
    )

    private fun signInResponse(token: String) = MockResponse()
        .setResponseCode(200)
        .setHeader("set-auth-token", token)
        .setBody("""{"redirect":false,"token":"$token"}""")

    private fun currentUserResponse(audience: String) = MockResponse()
        .setResponseCode(200)
        .setBody(
            """
            {
              "statusCode": 200,
              "message": "Success",
              "data": {
                "id": "user-1",
                "name": "Parking Attendant",
                "email": "parkingattendant@leaseting.com",
                "organizationRole": "parking_attendant",
                "audience": "$audience"
              }
            }
            """.trimIndent(),
        )

    private fun storedSession() = StoredSession(
        token = "session-token",
        attendantId = "user-1",
        attendantName = "Parking Attendant",
        attendantEmail = "parkingattendant@leaseting.com",
    )
}
