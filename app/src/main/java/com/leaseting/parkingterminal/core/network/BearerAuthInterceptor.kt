package com.leaseting.parkingterminal.core.network

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Attaches the stored session token as `Authorization: Bearer`, which is how
 * leaseting-api authenticates a native client — a handheld has no browser
 * cookie jar.
 *
 * A request that already carries the header is left alone: the sign-in flow
 * passes a token it has deliberately not stored yet.
 */
class BearerAuthInterceptor(private val tokenProvider: () -> String?) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (request.header(HEADER_AUTHORIZATION) != null) return chain.proceed(request)

        val token = tokenProvider() ?: return chain.proceed(request)
        return chain.proceed(
            request.newBuilder().header(HEADER_AUTHORIZATION, bearerHeader(token)).build(),
        )
    }
}
