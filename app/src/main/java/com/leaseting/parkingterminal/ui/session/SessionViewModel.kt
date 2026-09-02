package com.leaseting.parkingterminal.ui.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.leaseting.parkingterminal.domain.auth.AuthRepository
import com.leaseting.parkingterminal.domain.auth.SessionState
import com.leaseting.parkingterminal.ui.appContainer
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Who is signed in, for the whole application. Held above the navigation graph
 * so a session that ends — by sign-out, or by the API rejecting the token —
 * moves the terminal back to the login screen wherever the guard happens to be.
 */
class SessionViewModel(private val authRepository: AuthRepository) : ViewModel() {

    val sessionState: StateFlow<SessionState> = authRepository.sessionState

    fun signOut() {
        viewModelScope.launch { authRepository.signOut() }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { SessionViewModel(appContainer().authRepository) }
        }
    }
}
