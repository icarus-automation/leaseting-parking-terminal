package com.leaseting.parkingterminal.domain.auth

/** Whether anyone is signed in on this handheld. */
sealed interface SessionState {
    /** Stored credentials have not been read back yet. */
    data object Restoring : SessionState

    data object SignedOut : SessionState

    data class SignedIn(val attendant: Attendant) : SessionState
}
