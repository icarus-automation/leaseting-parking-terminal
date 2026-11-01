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

/**
 * The same envelope for a route that answers `data: null` as a normal outcome,
 * such as asking for a shift the attendant does not have.
 *
 * It exists as its own type because Kotlin's nullability is erased in the
 * `java.lang.reflect.Type` Retrofit hands to the serializer: an
 * `ApiEnvelope<Thing?>` resolves to a non-null `Thing` serializer at runtime
 * and a `null` payload fails to decode. Declaring the nullability on the field
 * is the only place it survives.
 */
@Serializable
data class NullableApiEnvelope<T>(
    val statusCode: Int,
    val message: String,
    val data: T? = null,
)
