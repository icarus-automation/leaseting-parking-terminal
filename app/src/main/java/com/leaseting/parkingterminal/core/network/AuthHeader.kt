package com.leaseting.parkingterminal.core.network

/** The header leaseting-api's Better Auth bearer plugin reads. */
const val HEADER_AUTHORIZATION = "Authorization"

/** `Authorization` value carrying a Better Auth session token. */
fun bearerHeader(token: String): String = "Bearer $token"
