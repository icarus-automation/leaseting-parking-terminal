package com.leaseting.parkingterminal.core.network

import okhttp3.Interceptor
import okhttp3.Response
import java.net.HttpURLConnection

/**
 * Ends the local session when the API rejects a token it was given.
 *
 * The terminal deliberately trusts its stored session offline, so a revoked or
 * expired token is only ever discovered by using it. Requests that carried no
 * token are ignored — a failed sign-in is a wrong password, not a dead session.
 */
class UnauthorizedInterceptor(private val onUnauthorized: () -> Unit) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)

        val tokenWasSent = request.header(HEADER_AUTHORIZATION) != null
        if (tokenWasSent && response.code == HttpURLConnection.HTTP_UNAUTHORIZED) {
            onUnauthorized()
        }
        return response
    }
}
