package com.campusconnect.app.data.remote

import com.campusconnect.app.data.local.TokenManager
import com.campusconnect.app.data.remote.api.*
import com.google.gson.GsonBuilder
import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Builds the single shared OkHttpClient + Retrofit instance and every typed
 * API service the app uses. Constructed once by [AppContainer] and reused
 * app-wide — Retrofit/OkHttp clients are expensive to create and are
 * designed to be long-lived singletons.
 */
class NetworkModule(tokenManager: TokenManager) {

    private val gson = GsonBuilder().setLenient().create()

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        // BODY logging is convenient in development but will log tokens and
        // personal data to logcat — force it down to headers-only (or off)
        // before a release build.
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .addInterceptor(AuthInterceptor(tokenManager))
        .addInterceptor(loggingInterceptor)
        .authenticator(TokenAuthenticator(tokenManager, NetworkConfig.BASE_URL))
        .apply {
            if (NetworkConfig.ENABLE_CERTIFICATE_PINNING && NetworkConfig.CERTIFICATE_PINS.isNotEmpty()) {
                val pinnerBuilder = CertificatePinner.Builder()
                NetworkConfig.CERTIFICATE_PINS.forEach { (host, pins) ->
                    pins.forEach { pin -> pinnerBuilder.add(host, pin) }
                }
                certificatePinner(pinnerBuilder.build())
            }
        }
        .build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(NetworkConfig.BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()

    val authApi          : AuthApi          = retrofit.create(AuthApi::class.java)
    val postsApi          : PostsApi         = retrofit.create(PostsApi::class.java)
    val accommodationApi  : AccommodationApi = retrofit.create(AccommodationApi::class.java)
    val eventsApi          : EventsApi        = retrofit.create(EventsApi::class.java)
    val usersApi           : UsersApi         = retrofit.create(UsersApi::class.java)
    val gamificationApi    : GamificationApi  = retrofit.create(GamificationApi::class.java)
    val messagingApi       : MessagingApi     = retrofit.create(MessagingApi::class.java)
}
