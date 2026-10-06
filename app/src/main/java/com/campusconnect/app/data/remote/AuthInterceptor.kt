package com.campusconnect.app.data.remote

import com.campusconnect.app.data.local.TokenManager
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Attaches `Authorization: Bearer <token>` to outgoing requests.
 *
 * Three cases, based on the request path:
 *  - Public auth endpoints (login, register, password reset, email
 *    verification) → no header at all; there's no session yet.
 *  - POST /auth/refresh → the REFRESH token, not the access token. The
 *    backend's jwt-refresh Passport strategy validates this endpoint's
 *    Bearer header specifically as a refresh token (see AuthApi.refresh()).
 *  - Everything else → the current access token, if any.
 */
class AuthInterceptor(private val tokenManager: TokenManager) : Interceptor {

    private val noAuthPaths = setOf(
        "auth/login",
        "auth/register",
        "auth/forgot-password",
        "auth/reset-password",
        "auth/verify-email",
        "auth/resend-verification",
    )

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val path     = original.url.encodedPath.trimStart('/')

        val token = when {
            noAuthPaths.any { path.endsWith(it) }   -> null
            path.endsWith("auth/refresh")            -> tokenManager.getRefreshTokenBlocking()
            else                                     -> tokenManager.getAccessTokenBlocking()
        }

        val request = if (token != null) {
            original.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            original
        }

        return chain.proceed(request)
    }
}
