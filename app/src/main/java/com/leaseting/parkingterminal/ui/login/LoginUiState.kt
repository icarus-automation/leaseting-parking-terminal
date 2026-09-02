package com.leaseting.parkingterminal.ui.login

import com.leaseting.parkingterminal.domain.auth.SignInFailure

/**
 * Everything the login screen draws.
 *
 * [failure] is the domain reason, not a sentence: the screen owns the wording so
 * the copy stays in `strings.xml` and translatable.
 */
data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val isSubmitting: Boolean = false,
    val failure: SignInFailure? = null,
) {
    val canSubmit: Boolean
        get() = !isSubmitting && email.isNotBlank() && password.isNotBlank()
}
