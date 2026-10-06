package com.campusconnect.app.ui.screens.auth

/** Sealed state for every auth operation. */
sealed interface AuthState {
    data object Idle    : AuthState
    data object Loading : AuthState
    data class  Success(val message: String = "")  : AuthState
    data class  Error(val message: String)         : AuthState
}

/** Registration is split into two steps. */
enum class RegisterStep { CREDENTIALS, PROFILE }

data class RegisterForm(
    // Step 1
    val email:     String = "",
    val username:  String = "",
    val password:  String = "",
    val confirmPassword: String = "",
    // Step 2
    val firstName: String = "",
    val lastName:  String = "",
    val university: String = "",
    val role:      String = "student",
)

data class LoginForm(
    val email:    String = "",
    val password: String = "",
)
