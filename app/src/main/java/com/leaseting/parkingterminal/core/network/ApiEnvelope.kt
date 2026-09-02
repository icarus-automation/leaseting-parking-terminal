package com.leaseting.parkingterminal.core.network

import kotlinx.serialization.Serializable

/**
 * The `{ statusCode, message, data }` wrapper leaseting-api puts around every
 * domain response. Better Auth routes under `/auth` are not wrapped.
 */
@Serializable
data class ApiEnvelope<T>(
    val statusCode: Int,
    val message: String,
    val data: T,
)
