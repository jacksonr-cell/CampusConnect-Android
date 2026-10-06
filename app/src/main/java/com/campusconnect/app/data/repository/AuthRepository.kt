package com.campusconnect.app.data.repository

import com.campusconnect.app.data.local.TokenManager
import com.campusconnect.app.data.model.User
import com.campusconnect.app.data.remote.api.AuthApi
import com.campusconnect.app.data.remote.dto.LoginRequest
import com.campusconnect.app.data.remote.dto.LoginResponseDto
import com.campusconnect.app.data.remote.dto.RegisterRequest
import com.campusconnect.app.data.remote.dto.VerifyMfaLoginRequest
import com.campusconnect.app.data.remote.mapper.toDomain
import kotlinx.coroutines.flow.StateFlow

/** POST /auth/login (and /register) return either a full session or an MFA challenge. */
sealed interface LoginOutcome {
    data class Success(val user: User) : LoginOutcome
    data class MfaRequired(val mfaToken: String) : LoginOutcome
}

class AuthRepository(
    private val authApi: AuthApi,
    private val tokenManager: TokenManager,
) {
    val isLoggedIn: StateFlow<Boolean> = tokenManager.isLoggedInFlow

    suspend fun register(
        email: String, username: String, password: String,
        firstName: String, lastName: String, university: String?,
    ): ApiResult<LoginOutcome> {
        val result = safeApiCall {
            authApi.register(RegisterRequest(email, username, password, firstName, lastName, university))
        }
        return handleLoginResponse(result)
    }

    suspend fun login(email: String, password: String): ApiResult<LoginOutcome> {
        val result = safeApiCall { authApi.login(LoginRequest(email, password)) }
        return handleLoginResponse(result)
    }

    suspend fun verifyMfaLogin(mfaToken: String, code: String): ApiResult<User> {
        val result = safeApiCall { authApi.verifyMfaLogin(VerifyMfaLoginRequest(mfaToken, code)) }
        return when (result) {
            is ApiResult.Error -> result
            is ApiResult.Success -> {
                applyTokensIfPresent(result.data)
                val user = result.data.user
                if (user != null) ApiResult.Success(user.toDomain())
                else ApiResult.Error("MFA verification succeeded but no user was returned")
            }
        }
    }

    suspend fun logout() {
        // Always clear local session, even if the network call fails —
        // the user should never be "stuck" logged in on-device because the
        // backend was unreachable.
        try {
            authApi.logout()
        } catch (_: Exception) {
            // ignored — best-effort server-side revocation
        } finally {
            tokenManager.clearTokens()
        }
    }

    suspend fun getCurrentUser(): ApiResult<User> =
        when (val result = safeApiCall { authApi.getMe() }) {
            is ApiResult.Success -> ApiResult.Success(result.data.toDomain())
            is ApiResult.Error   -> result
        }

    /** Shared branching for /login and /register — both can return a full session or an MFA challenge. */
    private fun handleLoginResponse(result: ApiResult<LoginResponseDto>): ApiResult<LoginOutcome> {
        return when (result) {
            is ApiResult.Error -> result
            is ApiResult.Success -> {
                val dto = result.data
                when {
                    dto.mfaRequired == true && dto.mfaToken != null ->
                        ApiResult.Success(LoginOutcome.MfaRequired(dto.mfaToken))
                    dto.user != null && dto.tokens != null -> {
                        applyTokensIfPresent(dto)
                        ApiResult.Success(LoginOutcome.Success(dto.user.toDomain()))
                    }
                    else -> ApiResult.Error("Unexpected response shape from the server")
                }
            }
        }
    }

    private fun applyTokensIfPresent(dto: LoginResponseDto) {
        dto.tokens?.let { tokens ->
            tokenManager.saveTokens(
                accessToken  = tokens.accessToken,
                refreshToken = tokens.refreshToken,
                userId       = dto.user?.id,
            )
        }
    }
}
