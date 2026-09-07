package com.leaseting.parkingterminal.data.parking.dto

import kotlinx.serialization.Serializable

/**
 * `GET /parking/catalog`.
 *
 * The catalog also carries rate plans and rounding rules. The terminal does not
 * decode them: fees are priced by the API on every exit, so a rate board on the
 * device would only be a second, staler answer to a question it never asks.
 */
@Serializable
data class ParkingCatalogResponse(
    val terminals: List<ParkingTerminalDto> = emptyList(),
    val vehicleTypes: List<VehicleTypeDto> = emptyList(),
)

@Serializable
data class ParkingTerminalDto(
    val id: String,
    val name: String,
    val isActive: Boolean = true,
    val propertyId: String,
    val propertyName: String,
    val propertyIsArchived: Boolean = false,
)

@Serializable
data class VehicleTypeDto(
    val id: String,
    val name: String,
    val isArchived: Boolean = false,
)

/** `POST /parking/shifts`. */
@Serializable
data class OpenShiftRequest(val terminalId: String)

/** `POST /parking/shifts`, `GET /parking/shifts/current`, `POST .../declare`. */
@Serializable
data class ParkingShiftDto(
    val id: String,
    val status: String,
    val openedAt: String,
    val terminalId: String,
    val terminalName: String,
    val propertyName: String,
    val expectedCash: String? = null,
    val declaredCash: String? = null,
)

/** `GET /parking/shifts/{id}/summary`. */
@Serializable
data class ShiftSummaryDto(
    val shiftId: String,
    val entryCount: Int,
    val paidExitCount: Int,
    val expectedCash: String,
)

/** `POST /parking/shifts/{id}/declare`. */
@Serializable
data class DeclareShiftRequest(
    val declaredCash: String,
    val note: String? = null,
)

/** `POST /parking/sessions`. */
@Serializable
data class CreateSessionRequest(
    val shiftId: String,
    val vehicleTypeId: String,
    val plateNumber: String,
)

/** Every parking session route answers with this. */
@Serializable
data class ParkingSessionDto(
    val id: String,
    val ticketCode: String,
    val plateNumber: String,
    val status: String,
    val entryAt: String,
    val exitAt: String? = null,
    val terminalName: String,
    val vehicleTypeName: String,
    val ratePlanName: String? = null,
    val feeAmount: String? = null,
    val amountTendered: String? = null,
    val changeGiven: String? = null,
)

/** `GET /parking/sessions/{id}/fee-preview`. */
@Serializable
data class FeeQuoteDto(
    val sessionId: String,
    val ticketCode: String,
    val plateNumber: String,
    val vehicleTypeName: String,
    val entryAt: String,
    val quotedAt: String,
    val stayMinutes: Int,
    val billedMinutes: Int,
    val graceMinutes: Int,
    val ratePlanName: String,
    val feeAmount: String,
)

/**
 * `POST /parking/sessions/{id}/exit`.
 *
 * No fee and no method: v1 is cash, and the amount owed is the API's to decide.
 * There is no waive field by design — a fee the guard disputes is an admin void
 * in the web client, never a number typed at the gate.
 */
@Serializable
data class ExitSessionRequest(
    val shiftId: String,
    val amountTendered: String,
)
