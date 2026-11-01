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

/**
 * `GET /users/me` payload, inside the standard envelope.
 *
 * [displayName] is the person, and it is what an Attendant line prints.
 * [name] is kept only as the fallback for a build of the API that predates
 * the field. See `docs/integrations/attendant-display-name.md`.
 */
@Serializable
data class CurrentUserResponse(
    val id: String,
    val name: String,
    val displayName: String? = null,
    val email: String,
    val organizationRole: String? = null,
    val audience: String? = null,
)
