package com.leaseting.parkingterminal.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.leaseting.parkingterminal.domain.auth.AuthRepository
import com.leaseting.parkingterminal.domain.auth.SignInResult
import com.leaseting.parkingterminal.ui.appContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Drives the login form. Success is not reported back to the screen: the
 * session flow changes, and the navigation graph moves off this screen on its
 * own.
 */
class LoginViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onEmailChange(email: String) {
        _uiState.update { it.copy(email = email, failure = null) }
    }

    fun onPasswordChange(password: String) {
        _uiState.update { it.copy(password = password, failure = null) }
    }

    fun onTogglePasswordVisibility() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun onSubmit() {
        if (!_uiState.value.canSubmit) return

        _uiState.update { it.copy(isSubmitting = true, failure = null) }
        viewModelScope.launch {
            val state = _uiState.value
            when (val result = authRepository.signIn(state.email, state.password)) {
                // The password is dropped either way: a rejected one must not sit
                // in memory, and a correct one has no further use.
                is SignInResult.Failure ->
                    _uiState.update {
                        it.copy(isSubmitting = false, password = "", failure = result.reason)
                    }

                SignInResult.Success ->
                    _uiState.update { it.copy(isSubmitting = false, password = "") }
            }
        }
    }

    /** Called once the failure has been shown, so it is not raised again on recomposition. */
    fun onFailureShown() {
        _uiState.update { it.copy(failure = null) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { LoginViewModel(appContainer().authRepository) }
        }
    }
}
