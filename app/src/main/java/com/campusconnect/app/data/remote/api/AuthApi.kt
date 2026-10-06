package com.campusconnect.app.data.remote.api

import com.campusconnect.app.data.remote.dto.*
import retrofit2.http.*

interface AuthApi {

    @POST("auth/register")
    suspend fun register(@Body body: RegisterRequest): ApiEnvelope<LoginResponseDto>

    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): ApiEnvelope<LoginResponseDto>

    @POST("auth/mfa/verify-login")
    suspend fun verifyMfaLogin(@Body body: VerifyMfaLoginRequest): ApiEnvelope<LoginResponseDto>

    /**
     * IMPORTANT: this endpoint reads the REFRESH token from the
     * Authorization header (validated by the backend's jwt-refresh
     * Passport strategy) — not the access token, and not a request body.
     * AuthInterceptor/TokenAuthenticator must swap in the refresh token
     * for this specific call. See TokenAuthenticator.kt.
     */
    @POST("auth/refresh")
    suspend fun refresh(): ApiEnvelope<RefreshTokenResponseDto>

    @POST("auth/logout")
    suspend fun logout(@Body body: Map<String, String> = emptyMap()): ApiEnvelope<MessageResponseDto>

    @POST("auth/logout-all")
    suspend fun logoutAll(): ApiEnvelope<MessageResponseDto>

    @GET("auth/me")
    suspend fun getMe(): ApiEnvelope<UserDto>

    @POST("auth/verify-email")
    suspend fun verifyEmail(@Body body: Map<String, String>): ApiEnvelope<MessageResponseDto>

    @POST("auth/resend-verification")
    suspend fun resendVerification(@Body body: Map<String, String>): ApiEnvelope<MessageResponseDto>

    @POST("auth/forgot-password")
    suspend fun forgotPassword(@Body body: Map<String, String>): ApiEnvelope<MessageResponseDto>

    @POST("auth/reset-password")
    suspend fun resetPassword(@Body body: Map<String, String>): ApiEnvelope<MessageResponseDto>
}
