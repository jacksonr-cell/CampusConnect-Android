package com.campusconnect.app.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campusconnect.app.data.repository.ApiResult
import com.campusconnect.app.data.repository.AuthRepository
import com.campusconnect.app.data.repository.LoginOutcome
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class AuthViewModel(private val authRepository: AuthRepository) : ViewModel() {

    // ── Login ──────────────────────────────────────────────────────────────
    private val _loginState = MutableStateFlow<AuthState>(AuthState.Idle)
    val loginState: StateFlow<AuthState> = _loginState.asStateFlow()

    var loginForm = MutableStateFlow(LoginForm())
        private set

    fun onLoginEmailChange(value: String)    { loginForm.value = loginForm.value.copy(email = value) }
    fun onLoginPasswordChange(value: String) { loginForm.value = loginForm.value.copy(password = value) }

    fun login(onSuccess: () -> Unit) {
        val form = loginForm.value
        if (form.email.isBlank() || form.password.isBlank()) {
            _loginState.value = AuthState.Error("Please fill in all fields")
            return
        }
        viewModelScope.launch {
            _loginState.value = AuthState.Loading
            when (val result = authRepository.login(form.email, form.password)) {
                is ApiResult.Success -> when (val outcome = result.data) {
                    is LoginOutcome.Success -> {
                        _loginState.value = AuthState.Success()
                        onSuccess()
                    }
                    is LoginOutcome.MfaRequired -> {
                        // NOTE: a dedicated MFA code-entry screen isn't built yet.
                        // Surfacing this clearly rather than silently failing or
                        // (worse) silently proceeding as if login succeeded.
                        _loginState.value = AuthState.Error(
                            "This account has two-factor authentication enabled. " +
                            "MFA entry isn't wired into the app yet — disable MFA " +
                            "on the account to test login for now.",
                        )
                    }
                }
                is ApiResult.Error -> _loginState.value = AuthState.Error(result.message)
            }
        }
    }

    fun resetLoginState() { _loginState.value = AuthState.Idle }

    // ── Register ───────────────────────────────────────────────────────────
    private val _registerState = MutableStateFlow<AuthState>(AuthState.Idle)
    val registerState: StateFlow<AuthState> = _registerState.asStateFlow()

    private val _registerStep = MutableStateFlow(RegisterStep.CREDENTIALS)
    val registerStep: StateFlow<RegisterStep> = _registerStep.asStateFlow()

    var registerForm = MutableStateFlow(RegisterForm())
        private set

    fun onRegisterFieldChange(update: RegisterForm.() -> RegisterForm) {
        registerForm.value = registerForm.value.update()
    }

    fun validateAndAdvance(): Boolean {
        val form = registerForm.value
        return when (_registerStep.value) {
            RegisterStep.CREDENTIALS -> {
                when {
                    form.email.isBlank()    -> { _registerState.value = AuthState.Error("Email is required"); false }
                    !form.email.contains("@") -> { _registerState.value = AuthState.Error("Enter a valid email"); false }
                    form.username.length < 3  -> { _registerState.value = AuthState.Error("Username must be at least 3 characters"); false }
                    form.password.length < 8  -> { _registerState.value = AuthState.Error("Password must be at least 8 characters"); false }
                    form.password != form.confirmPassword -> { _registerState.value = AuthState.Error("Passwords do not match"); false }
                    else -> {
                        _registerState.value = AuthState.Idle
                        _registerStep.value  = RegisterStep.PROFILE
                        true
                    }
                }
            }
            RegisterStep.PROFILE -> true
        }
    }

    fun goBackToCredentials() { _registerStep.value = RegisterStep.CREDENTIALS }

    fun register(onSuccess: () -> Unit) {
        val form = registerForm.value
        if (form.firstName.isBlank() || form.lastName.isBlank()) {
            _registerState.value = AuthState.Error("First and last name are required")
            return
        }
        viewModelScope.launch {
            _registerState.value = AuthState.Loading
            when (val result = authRepository.register(
                email      = form.email,
                username   = form.username,
                password   = form.password,
                firstName  = form.firstName,
                lastName   = form.lastName,
                university = form.university.ifBlank { null },
            )) {
                is ApiResult.Success -> when (result.data) {
                    is LoginOutcome.Success -> {
                        _registerState.value = AuthState.Success("Account created!")
                        onSuccess()
                    }
                    is LoginOutcome.MfaRequired -> {
                        // Shouldn't happen for a brand-new account, but handle
                        // defensively instead of assuming success.
                        _registerState.value = AuthState.Error("Unexpected MFA challenge during registration")
                    }
                }
                is ApiResult.Error -> _registerState.value = AuthState.Error(result.message)
            }
        }
    }

    fun resetRegisterState() { _registerState.value = AuthState.Idle }
}
