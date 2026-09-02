package com.leaseting.parkingterminal.data.auth.remote

import com.leaseting.parkingterminal.core.network.ApiEnvelope
import com.leaseting.parkingterminal.data.auth.dto.CurrentUserResponse
import com.leaseting.parkingterminal.data.auth.dto.SignInRequest
import com.leaseting.parkingterminal.data.auth.dto.SignInResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

/** The leaseting-api endpoints the terminal needs to establish a session. */
interface AuthApi {
    /** Raw [Response] because the session token comes back in a header. */
    @POST("auth/sign-in/email")
    suspend fun signIn(@Body body: SignInRequest): Response<SignInResponse>

    /** Ends the stored session. The bearer interceptor supplies its token. */
    @POST("auth/sign-out")
    suspend fun signOut(): Response<Unit>

    /**
     * Ends a session whose token was never stored — the one handed to an account
     * that turned out to belong to another Leaseting app.
     */
    @POST("auth/sign-out")
    suspend fun revokeSession(
        @Header("Authorization") authorization: String,
    ): Response<Unit>

    /**
     * Takes the token explicitly so sign-in can check the audience *before*
     * anything is persisted. An account that belongs to another Leaseting app
     * never leaves a token on the handheld.
     */
    @GET("users/me")
    suspend fun currentUser(
        @Header("Authorization") authorization: String,
    ): ApiEnvelope<CurrentUserResponse>
}
