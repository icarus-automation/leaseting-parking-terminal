package com.leaseting.parkingterminal.domain.auth

import kotlinx.coroutines.flow.StateFlow

/**
 * The terminal's session. One attendant is signed in at a time, and the session
 * survives restarts and outages — a guard at a gate cannot be asked to
 * re-authenticate because the network dropped.
 */
interface AuthRepository {
    val sessionState: StateFlow<SessionState>

    suspend fun signIn(email: String, password: String): SignInResult

    /** Clears the local session; tells the API too when it is reachable. */
    suspend fun signOut()
}
