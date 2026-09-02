package com.leaseting.parkingterminal.data.auth.local

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/** Where the terminal keeps the session between launches. */
interface SessionStore {
    val session: Flow<StoredSession?>

    suspend fun save(session: StoredSession)

    suspend fun clear()

    /** Snapshot for callers that cannot collect, such as an OkHttp interceptor. */
    suspend fun currentSession(): StoredSession? = session.first()
}
