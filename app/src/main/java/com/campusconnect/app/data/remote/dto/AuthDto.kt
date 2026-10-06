package com.campusconnect.app.data.remote.dto

data class RegisterRequest(
    val email     : String,
    val username  : String,
    val password  : String,
    val firstName : String,
    val lastName  : String,
    val university: String? = null,
)

data class LoginRequest(
    val email   : String,
    val password: String,
)

data class VerifyMfaLoginRequest(
    val mfaToken: String,
    val code    : String,
)

data class TokensDto(
    val accessToken : String,
    val refreshToken: String,
    val expiresIn   : Int,
)

/**
 * POST /auth/login can return EITHER a full login success shape
 * (`user` + `tokens`) OR an MFA challenge shape (`mfaRequired` + `mfaToken`)
 * — the backend models this as a TS union type. Gson doesn't do discriminated
 * unions, so all fields are nullable here and the repository branches on
 * which ones are present.
 */
data class LoginResponseDto(
    val user       : UserDto?  = null,
    val tokens     : TokensDto? = null,
    val mfaRequired: Boolean?  = null,
    val mfaToken   : String?   = null,
)

data class RefreshTokenResponseDto(
    val accessToken : String,
    val refreshToken: String,
    val expiresIn    : Int,
)
