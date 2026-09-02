package com.leaseting.parkingterminal.domain.auth

/**
 * Which Leaseting application an account is allowed to sign in to, as reported
 * by `GET /users/me`.
 *
 * The terminal accepts [PARKING] and nothing else. Staff belong in
 * leaseting-client and residents in Residence Care, and letting either in here
 * would only strand them on screens the API refuses to serve.
 */
enum class Audience(private val wireValue: String) {
    STAFF("staff"),
    TENANT("tenant"),
    PARKING("parking"),

    /** A value this build does not know — treated as "not ours". */
    UNKNOWN("");

    companion object {
        fun fromWireValue(value: String?): Audience =
            entries.firstOrNull { it != UNKNOWN && it.wireValue == value } ?: UNKNOWN
    }
}
