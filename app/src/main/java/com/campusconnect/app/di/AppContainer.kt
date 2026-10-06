package com.campusconnect.app.di

import android.content.Context
import com.campusconnect.app.data.local.TokenManager
import com.campusconnect.app.data.remote.NetworkModule
import com.campusconnect.app.data.repository.AccommodationRepository
import com.campusconnect.app.data.repository.AuthRepository
import com.campusconnect.app.data.repository.EventsRepository
import com.campusconnect.app.data.repository.MessagingRepository
import com.campusconnect.app.data.repository.PostsRepository
import com.campusconnect.app.data.repository.ProfileRepository
import com.campusconnect.app.realtime.RealtimeService

/**
 * Lightweight manual dependency container — deliberately NOT Hilt.
 *
 * Hilt would need a KSP/kapt annotation-processing setup added to the
 * Gradle build that can't be verified end-to-end without a real compiler
 * in this environment; a plain container of `by lazy` singletons is fully
 * transparent Kotlin, carries zero build-tooling risk, and is a completely
 * standard pattern (see Android's own "Now in Android" sample app). Swap
 * to Hilt later if the team wants it — every constructor here already
 * takes its dependencies as plain parameters, so the migration is
 * mechanical (add annotations, delete this file).
 */
interface AppContainer {
    val tokenManager: TokenManager
    val authRepository: AuthRepository
    val postsRepository: PostsRepository
    val accommodationRepository: AccommodationRepository
    val eventsRepository: EventsRepository
    val profileRepository: ProfileRepository
    val messagingRepository: MessagingRepository
    val realtimeService: RealtimeService
}

class DefaultAppContainer(context: Context) : AppContainer {

    override val tokenManager: TokenManager by lazy { TokenManager(context) }

    private val networkModule: NetworkModule by lazy { NetworkModule(tokenManager) }

    override val authRepository: AuthRepository by lazy {
        AuthRepository(networkModule.authApi, tokenManager)
    }

    override val postsRepository: PostsRepository by lazy {
        PostsRepository(networkModule.postsApi)
    }

    override val accommodationRepository: AccommodationRepository by lazy {
        AccommodationRepository(networkModule.accommodationApi)
    }

    override val eventsRepository: EventsRepository by lazy {
        EventsRepository(networkModule.eventsApi)
    }

    override val profileRepository: ProfileRepository by lazy {
        ProfileRepository(
            usersApi         = networkModule.usersApi,
            gamificationApi  = networkModule.gamificationApi,
            postsApi         = networkModule.postsApi,
            accommodationApi = networkModule.accommodationApi,
        )
    }

    override val messagingRepository: MessagingRepository by lazy {
        MessagingRepository(networkModule.messagingApi, tokenManager)
    }

    override val realtimeService: RealtimeService by lazy {
        RealtimeService(tokenManager)
    }
}
