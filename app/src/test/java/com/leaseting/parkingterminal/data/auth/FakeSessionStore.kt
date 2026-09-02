package com.leaseting.parkingterminal.data.auth

import com.leaseting.parkingterminal.data.auth.local.SessionStore
import com.leaseting.parkingterminal.data.auth.local.StoredSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** In-memory stand-in for the Keystore-backed store, which needs a device. */
class FakeSessionStore(initial: StoredSession? = null) : SessionStore {
    private val state = MutableStateFlow(initial)

    override val session: Flow<StoredSession?> = state

    val stored: StoredSession? get() = state.value

    override suspend fun save(session: StoredSession) {
        state.value = session
    }

    override suspend fun clear() {
        state.value = null
    }
}
