package com.leaseting.parkingterminal.data.parking.remote

import com.leaseting.parkingterminal.core.network.ApiEnvelope
import com.leaseting.parkingterminal.core.network.NullableApiEnvelope
import com.leaseting.parkingterminal.data.parking.dto.CreateSessionRequest
import com.leaseting.parkingterminal.data.parking.dto.DeclareShiftRequest
import com.leaseting.parkingterminal.data.parking.dto.ExitSessionRequest
import com.leaseting.parkingterminal.data.parking.dto.FeeQuoteDto
import com.leaseting.parkingterminal.data.parking.dto.OpenShiftRequest
import com.leaseting.parkingterminal.data.parking.dto.ParkingCatalogResponse
import com.leaseting.parkingterminal.data.parking.dto.ParkingSessionDto
import com.leaseting.parkingterminal.data.parking.dto.ParkingShiftDto
import com.leaseting.parkingterminal.data.parking.dto.ShiftSummaryDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * The `parking` audience routes on leaseting-api.
 *
 * Every call returns a raw [Response] so the repository can read the API's own
 * rejection message off a 4xx body rather than losing it to an exception. The
 * bearer token is attached by the shared interceptor; the attendant is taken
 * from the session server-side and is never sent.
 */
interface ParkingApi {

    @GET("parking/catalog")
    suspend fun catalog(): Response<ApiEnvelope<ParkingCatalogResponse>>

    @POST("parking/shifts")
    suspend fun openShift(@Body body: OpenShiftRequest): Response<ApiEnvelope<ParkingShiftDto>>

    /** `data` is null when this attendant holds no open shift. */
    @GET("parking/shifts/current")
    suspend fun currentShift(): Response<NullableApiEnvelope<ParkingShiftDto>>

    @GET("parking/shifts/{id}/summary")
    suspend fun shiftSummary(@Path("id") id: String): Response<ApiEnvelope<ShiftSummaryDto>>

    @POST("parking/shifts/{id}/declare")
    suspend fun declareShift(
        @Path("id") id: String,
        @Body body: DeclareShiftRequest,
    ): Response<ApiEnvelope<ParkingShiftDto>>

    @POST("parking/sessions")
    suspend fun createSession(
        @Body body: CreateSessionRequest,
    ): Response<ApiEnvelope<ParkingSessionDto>>

    @GET("parking/sessions")
    suspend fun sessions(
        @Query("terminalId") terminalId: String? = null,
        @Query("plate") plate: String? = null,
    ): Response<ApiEnvelope<List<ParkingSessionDto>>>

    /** The scan path. Takes the short stub code, not the session id. */
    @GET("parking/sessions/by-code/{code}")
    suspend fun sessionByCode(
        @Path("code") code: String,
    ): Response<ApiEnvelope<ParkingSessionDto>>

    @GET("parking/sessions/{id}/fee-preview")
    suspend fun feePreview(@Path("id") id: String): Response<ApiEnvelope<FeeQuoteDto>>

    @POST("parking/sessions/{id}/exit")
    suspend fun payExit(
        @Path("id") id: String,
        @Body body: ExitSessionRequest,
    ): Response<ApiEnvelope<ParkingSessionDto>>
}
