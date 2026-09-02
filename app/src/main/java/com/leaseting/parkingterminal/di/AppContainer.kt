package com.leaseting.parkingterminal.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.leaseting.parkingterminal.BuildConfig
import com.leaseting.parkingterminal.core.network.BearerAuthInterceptor
import com.leaseting.parkingterminal.core.network.UnauthorizedInterceptor
import com.leaseting.parkingterminal.data.auth.DefaultAuthRepository
import com.leaseting.parkingterminal.data.auth.local.EncryptedSessionStore
import com.leaseting.parkingterminal.data.auth.local.SessionCipher
import com.leaseting.parkingterminal.data.auth.remote.AuthApi
import com.leaseting.parkingterminal.domain.auth.AuthRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "session")

/**
 * The application's object graph, wired by hand.
 *
 * The terminal has one dependency chain — HTTP client, session store,
 * repository — and reading it top to bottom is worth more here than the
 * indirection an annotation processor would add. `AppContainer` is created once
 * by [com.leaseting.parkingterminal.ParkingTerminalApplication] and
 * lives as long as the process.
 */
interface AppContainer {
    val authRepository: AuthRepository
}

class DefaultAppContainer(context: Context) : AppContainer {

    /**
     * Outlives any one screen: the session flow has to stay warm while the
     * guard moves between them, and sign-out has to finish even if the screen
     * that asked for it is gone.
     */
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val json = Json {
        // leaseting-api grows fields the terminal does not read yet, and a new
        // one must never turn into a parse failure at a gate.
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    private val sessionStore = EncryptedSessionStore(
        dataStore = context.applicationContext.dataStore,
        cipher = SessionCipher(keyAlias = SESSION_KEY_ALIAS),
        json = json,
    )

    private val okHttpClient = OkHttpClient.Builder()
        // OkHttp calls interceptors off the main thread, and DataStore serves
        // the cached value after its first read, so this does not touch disk
        // per request.
        .addInterceptor(BearerAuthInterceptor { runBlocking { sessionStore.currentSession()?.token } })
        .addInterceptor(UnauthorizedInterceptor { applicationScope.launch { sessionStore.clear() } })
        .apply {
            if (BuildConfig.DEBUG) {
                addInterceptor(
                    HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY },
                )
            }
        }
        .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    private val authApi: AuthApi = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory(JSON_MEDIA_TYPE.toMediaType()))
        .build()
        .create(AuthApi::class.java)

    override val authRepository: AuthRepository = DefaultAuthRepository(
        authApi = authApi,
        sessionStore = sessionStore,
        ioDispatcher = Dispatchers.IO,
        externalScope = applicationScope,
    )

    private companion object {
        const val SESSION_KEY_ALIAS = "leaseting_parking_terminal_session"
        const val JSON_MEDIA_TYPE = "application/json"

        /**
         * Gate networks are slow and occasionally absent. Long enough to ride out
         * a bad minute, short enough that a guard is not left holding a spinner.
         */
        const val TIMEOUT_SECONDS = 20L
    }
}
