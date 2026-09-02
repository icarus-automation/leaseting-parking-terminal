package com.leaseting.parkingterminal.data.auth.dto

import kotlinx.serialization.Serializable

/** `POST /auth/sign-in/email` request body. */
@Serializable
data class SignInRequest(
    val email: String,
    val password: String,
)

/**
 * `POST /auth/sign-in/email` response.
 *
 * The session token also arrives in the `set-auth-token` response header, which
 * is what the bearer plugin documents for native clients; this body copy is the
 * fallback.
 */
@Serializable
data class SignInResponse(
    val token: String? = null,
)

/** `GET /users/me` payload, inside the standard envelope. */
@Serializable
data class CurrentUserResponse(
    val id: String,
    val name: String,
    val email: String,
    val organizationRole: String? = null,
    val audience: String? = null,
)
