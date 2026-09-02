package com.leaseting.parkingterminal.data.auth

import com.leaseting.parkingterminal.core.network.bearerHeader
import com.leaseting.parkingterminal.data.auth.dto.CurrentUserResponse
import com.leaseting.parkingterminal.data.auth.dto.SignInRequest
import com.leaseting.parkingterminal.data.auth.dto.SignInResponse
import com.leaseting.parkingterminal.data.auth.local.SessionStore
import com.leaseting.parkingterminal.data.auth.local.StoredSession
import com.leaseting.parkingterminal.data.auth.remote.AuthApi
import com.leaseting.parkingterminal.domain.auth.Attendant
import com.leaseting.parkingterminal.domain.auth.Audience
import com.leaseting.parkingterminal.domain.auth.AuthRepository
import com.leaseting.parkingterminal.domain.auth.SessionState
import com.leaseting.parkingterminal.domain.auth.SignInFailure
import com.leaseting.parkingterminal.domain.auth.SignInResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.net.HttpURLConnection.HTTP_BAD_REQUEST
import java.net.HttpURLConnection.HTTP_FORBIDDEN
import java.net.HttpURLConnection.HTTP_UNAUTHORIZED

/**
 * Signs an attendant in against leaseting-api and keeps the session on the
 * handheld.
 *
 * Two rules shape this class:
 *
 * 1. Only a `parking` audience gets a stored session. Authenticating proves who
 *    someone is, not that this is their application, so the audience is checked
 *    with a token that has not been written to disk yet, and that token is
 *    revoked when the answer is wrong.
 * 2. [SessionStore] is the only session state. Nothing is mirrored in memory
 *    that could disagree with what survives a restart.
 */
class DefaultAuthRepository(
    private val authApi: AuthApi,
    private val sessionStore: SessionStore,
    private val ioDispatcher: CoroutineDispatcher,
    externalScope: CoroutineScope,
) : AuthRepository {

    override val sessionState: StateFlow<SessionState> = sessionStore.session
        .map { stored ->
            stored?.let { SessionState.SignedIn(it.toAttendant()) } ?: SessionState.SignedOut
        }
        .stateIn(externalScope, SharingStarted.Eagerly, SessionState.Restoring)

    override suspend fun signIn(email: String, password: String): SignInResult =
        withContext(ioDispatcher) {
            val token = when (val outcome = requestToken(email, password)) {
                is Outcome.Failed -> return@withContext SignInResult.Failure(outcome.reason)
                is Outcome.Succeeded -> outcome.value
            }

            val user = when (val outcome = fetchCurrentUser(token)) {
                is Outcome.Failed -> return@withContext SignInResult.Failure(outcome.reason)
                is Outcome.Succeeded -> outcome.value
            }

            if (Audience.fromWireValue(user.audience) != Audience.PARKING) {
                revoke(token)
                return@withContext SignInResult.Failure(SignInFailure.NotAParkingAccount)
            }

            sessionStore.save(
                StoredSession(
                    token = token,
                    attendantId = user.id,
                    attendantName = user.name,
                    attendantEmail = user.email,
                ),
            )
            SignInResult.Success
        }

    /**
     * Clears the handheld first: a guard handing the device over must not stay
     * signed in just because the gate is offline.
     */
    override suspend fun signOut() {
        sessionStore.clear()
        withContext(ioDispatcher) {
            runCatching { authApi.signOut() }
        }
    }

    private suspend fun requestToken(email: String, password: String): Outcome<String> =
        callApi {
            val response = authApi.signIn(SignInRequest(email = email.trim(), password = password))
            when {
                response.code() in CREDENTIAL_REJECTION_CODES ->
                    Outcome.Failed(SignInFailure.InvalidCredentials)

                !response.isSuccessful -> Outcome.Failed(SignInFailure.ServerError)

                else -> response.sessionToken()
                    ?.let { Outcome.Succeeded(it) }
                    ?: Outcome.Failed(SignInFailure.ServerError)
            }
        }

    private suspend fun fetchCurrentUser(token: String): Outcome<CurrentUserResponse> =
        callApi {
            Outcome.Succeeded(authApi.currentUser(bearerHeader(token)).data)
        }

    /** Best effort: the token is discarded either way, so a failure here changes nothing. */
    private suspend fun revoke(token: String) {
        runCatching { authApi.revokeSession(bearerHeader(token)) }
    }

    /**
     * Turns transport and decoding failures into a [SignInFailure]. Cancellation
     * is not an error, so the catches stay narrow and let it propagate.
     */
    private inline fun <T> callApi(block: () -> Outcome<T>): Outcome<T> =
        try {
            block()
        } catch (_: IOException) {
            Outcome.Failed(SignInFailure.Unreachable)
        } catch (_: HttpException) {
            Outcome.Failed(SignInFailure.ServerError)
        } catch (_: SerializationException) {
            Outcome.Failed(SignInFailure.ServerError)
        }

    /**
     * The session token: the `set-auth-token` header Better Auth's bearer plugin
     * sets for native clients, or the body copy if a proxy strips the header.
     */
    private fun Response<SignInResponse>.sessionToken(): String? =
        headers()[SET_AUTH_TOKEN]?.takeIf { it.isNotBlank() }
            ?: body()?.token?.takeIf { it.isNotBlank() }

    private fun StoredSession.toAttendant() = Attendant(
        id = attendantId,
        name = attendantName,
        email = attendantEmail,
    )

    /** Internal two-case result so each step can report why it stopped. */
    private sealed interface Outcome<out T> {
        data class Succeeded<T>(val value: T) : Outcome<T>
        data class Failed(val reason: SignInFailure) : Outcome<Nothing>
    }

    private companion object {
        const val SET_AUTH_TOKEN = "set-auth-token"

        /**
         * Better Auth answers a bad email or password with 401; the validation
         * pipe and the audience guard use 400 and 403. On a login screen all
         * three mean the same thing: these credentials did not get you in.
         */
        val CREDENTIAL_REJECTION_CODES = setOf(HTTP_BAD_REQUEST, HTTP_UNAUTHORIZED, HTTP_FORBIDDEN)
    }
}
