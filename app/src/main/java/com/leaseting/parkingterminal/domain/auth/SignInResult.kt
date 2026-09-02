package com.leaseting.parkingterminal.domain.auth

/** Outcome of a sign-in attempt. Everything the UI needs to decide what to say. */
sealed interface SignInResult {
    data object Success : SignInResult

    data class Failure(val reason: SignInFailure) : SignInResult
}

/**
 * Why sign-in did not produce a session. Each case maps to one message on the
 * login screen, so the screen never has to inspect HTTP status codes.
 */
enum class SignInFailure {
    /** The API rejected the email and password pair. */
    InvalidCredentials,

    /** Authentic account, wrong application: staff or a resident, not an attendant. */
    NotAParkingAccount,

    /** The API could not be reached. Distinct from a rejection: retrying may work. */
    Unreachable,

    /** The API answered, but not with anything usable. */
    ServerError,
}
