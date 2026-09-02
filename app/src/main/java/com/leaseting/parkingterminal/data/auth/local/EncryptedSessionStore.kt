package com.leaseting.parkingterminal.data.auth.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

/**
 * The session, serialised and encrypted under an Android Keystore key, held as
 * one opaque DataStore entry.
 *
 * One value means one write: a crash mid-save cannot leave a token without the
 * attendant it belongs to.
 */
class EncryptedSessionStore(
    private val dataStore: DataStore<Preferences>,
    private val cipher: SessionCipher,
    private val json: Json,
) : SessionStore {

    override val session: Flow<StoredSession?> = dataStore.data.map { preferences ->
        preferences[SESSION_KEY]?.let(::decode)
    }

    override suspend fun save(session: StoredSession) {
        val payload = cipher.encrypt(json.encodeToString(session))
        dataStore.edit { it[SESSION_KEY] = payload }
    }

    override suspend fun clear() {
        dataStore.edit { it.remove(SESSION_KEY) }
    }

    /**
     * An entry that will not decode is no session at all — a rotated key or a
     * restored backup signs the guard out, it never crashes the launch.
     */
    private fun decode(payload: String): StoredSession? = runCatching {
        cipher.decrypt(payload)?.let { json.decodeFromString<StoredSession>(it) }
    }.getOrNull()

    private companion object {
        val SESSION_KEY = stringPreferencesKey("encrypted_session")
    }
}
