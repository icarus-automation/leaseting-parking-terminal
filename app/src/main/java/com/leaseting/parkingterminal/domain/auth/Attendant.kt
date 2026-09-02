package com.leaseting.parkingterminal.domain.auth

/** The signed-in parking attendant. Identity only: no session token reaches the UI. */
data class Attendant(
    val id: String,
    val name: String,
    val email: String,
)
