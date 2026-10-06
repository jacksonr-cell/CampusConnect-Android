package com.campusconnect.app.data.local

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Persists the JWT access/refresh token pair using [EncryptedSharedPreferences]
 * — AES-256-GCM value encryption with a hardware-backed Keystore master key
 * (via [MasterKey.KeyScheme.AES256_GCM]), rather than plaintext SharedPreferences
 * or plain DataStore. This is the production-grade storage tier for exactly
 * this use case: a handful of small, highly sensitive string values.
 *
 * A `MutableStateFlow` cache sits in front of the encrypted store so callers
 * get reactive updates without needing to poll — writes go to both the flow
 * and disk; reads for reactive UI use the flow, and the OkHttp
 * interceptor/authenticator (which run synchronously, off the main thread,
 * and cannot suspend) read the encrypted prefs directly.
 */
class TokenManager(context: Context) {

    private companion object {
        const val PREFS_FILE      = "campusconnect_secure_prefs"
        const val KEY_ACCESS      = "access_token"
        const val KEY_REFRESH     = "refresh_token"
        const val KEY_USER_ID     = "user_id"
    }

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val encryptedPrefs = EncryptedSharedPreferences.create(
        context,
        PREFS_FILE,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    private val _accessToken  = MutableStateFlow(encryptedPrefs.getString(KEY_ACCESS, null))
    private val _refreshToken = MutableStateFlow(encryptedPrefs.getString(KEY_REFRESH, null))
    private val _isLoggedIn   = MutableStateFlow(_accessToken.value != null)

    val accessTokenFlow : StateFlow<String?> = _accessToken.asStateFlow()
    val refreshTokenFlow: StateFlow<String?> = _refreshToken.asStateFlow()
    val isLoggedInFlow   : StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    fun saveTokens(accessToken: String, refreshToken: String, userId: String? = null) {
        encryptedPrefs.edit()
            .putString(KEY_ACCESS, accessToken)
            .putString(KEY_REFRESH, refreshToken)
            .apply { userId?.let { putString(KEY_USER_ID, it) } }
            .apply()

        _accessToken.value  = accessToken
        _refreshToken.value = refreshToken
        _isLoggedIn.value   = true
    }

    fun clearTokens() {
        encryptedPrefs.edit().clear().apply()
        _accessToken.value  = null
        _refreshToken.value = null
        _isLoggedIn.value   = false
    }

    fun getUserId(): String? = encryptedPrefs.getString(KEY_USER_ID, null)

    /** Synchronous reads for OkHttp Interceptor/Authenticator use — see AuthInterceptor.kt. */
    fun getAccessTokenBlocking(): String?  = encryptedPrefs.getString(KEY_ACCESS, null)
    fun getRefreshTokenBlocking(): String? = encryptedPrefs.getString(KEY_REFRESH, null)
}
