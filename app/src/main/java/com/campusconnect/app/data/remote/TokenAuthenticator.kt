package com.campusconnect.app.data.remote

import android.util.Log
import com.campusconnect.app.data.local.TokenManager
import com.google.gson.Gson
import com.campusconnect.app.data.remote.dto.ApiEnvelope
import com.campusconnect.app.data.remote.dto.RefreshTokenResponseDto
import okhttp3.Authenticator
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route
import java.io.IOException

/**
 * Handles automatic token refresh on a 401 response.
 *
 * OkHttp Authenticators run synchronously on the calling (background)
 * thread, so this makes a plain blocking call to POST /auth/refresh using
 * a bare-bones OkHttpClient that deliberately has NEITHER this
 * Authenticator NOR AuthInterceptor attached — attaching either would
 * recurse (a 401 on the refresh call would try to refresh again, forever).
 *
 * On success: persists the new token pair and retries the original request
 * with the new access token. On failure: clears stored tokens (so the app's
 * auth-state observers can route the user back to login) and gives up.
 */
class TokenAuthenticator(
    private val tokenManager: TokenManager,
    private val baseUrl: String,
) : Authenticator {

    private val gson = Gson()

    private val bareClient: OkHttpClient by lazy {
        OkHttpClient.Builder().build()
    }

    override fun authenticate(route: Route?, response: Response): Request? {
        // Avoid infinite retry loops: give up if we've already retried this
        // request once (OkHttp calls authenticate() again if the retried
        // request also 401s).
        if (responseCount(response) >= 2) return null

        // Don't try to "refresh" a 401 from the refresh call itself.
        if (response.request.url.encodedPath.endsWith("auth/refresh")) {
            tokenManager.clearTokens()
            return null
        }

        val refreshToken = tokenManager.getRefreshTokenBlocking() ?: return null

        val newAccessToken = try {
            performRefresh(refreshToken)
        } catch (e: IOException) {
            Log.w("TokenAuthenticator", "Token refresh failed: ${e.message}")
            null
        }

        if (newAccessToken == null) {
            tokenManager.clearTokens()
            return null
        }

        return response.request.newBuilder()
            .header("Authorization", "Bearer $newAccessToken")
            .build()
    }

    private fun performRefresh(refreshToken: String): String? {
        val request = Request.Builder()
            .url(baseUrl.trimEnd('/') + "/auth/refresh")
            .header("Authorization", "Bearer $refreshToken")
            .post(ByteArray(0).toRequestBody(null))
            .build()

        bareClient.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) return null
            val body = resp.body?.string() ?: return null

            val type = com.google.gson.reflect.TypeToken
                .getParameterized(ApiEnvelope::class.java, RefreshTokenResponseDto::class.java)
                .type
            val envelope = gson.fromJson<ApiEnvelope<RefreshTokenResponseDto>>(body, type)
            val tokens = envelope.data ?: return null

            tokenManager.saveTokens(tokens.accessToken, tokens.refreshToken)
            return tokens.accessToken
        }
    }

    private fun responseCount(response: Response): Int {
        var result = 1
        var prior = response.priorResponse
        while (prior != null) {
            result++
            prior = prior.priorResponse
        }
        return result
    }
}
