package com.leaseting.parkingterminal.data.auth.local

import kotlinx.serialization.Serializable

/** The persisted session: the bearer token plus who it belongs to. */
@Serializable
data class StoredSession(
    val token: String,
    val attendantId: String,
    val attendantName: String,
    val attendantEmail: String,
)
