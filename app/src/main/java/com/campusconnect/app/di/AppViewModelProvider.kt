package com.campusconnect.app.di

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.campusconnect.app.CampusConnectApp
import com.campusconnect.app.ui.screens.accommodation.AccommodationViewModel
import com.campusconnect.app.ui.screens.accommodation.PropertyDetailViewModel
import com.campusconnect.app.ui.screens.auth.AuthViewModel
import com.campusconnect.app.ui.screens.events.EventsViewModel
import com.campusconnect.app.ui.screens.feed.FeedViewModel
import com.campusconnect.app.ui.screens.messaging.ChatViewModel
import com.campusconnect.app.ui.screens.messaging.ConversationsViewModel
import com.campusconnect.app.ui.screens.profile.ProfileViewModel

/** Retrieves the app-wide [AppContainer] from a ViewModel's [CreationExtras]. */
fun CreationExtras.campusConnectApplication(): CampusConnectApp =
    (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as CampusConnectApp)

/**
 * Single factory used by every screen's `viewModel(factory = AppViewModelProvider.Factory)`
 * call. Each initializer pulls its ViewModel's real dependencies out of the
 * app-wide [AppContainer] — no Hilt annotations needed anywhere.
 */
object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer {
            AuthViewModel(campusConnectApplication().container.authRepository)
        }
        initializer {
            FeedViewModel(campusConnectApplication().container.postsRepository)
        }
        initializer {
            AccommodationViewModel(campusConnectApplication().container.accommodationRepository)
        }
        initializer {
            PropertyDetailViewModel(campusConnectApplication().container.accommodationRepository)
        }
        initializer {
            EventsViewModel(campusConnectApplication().container.eventsRepository)
        }
        initializer {
            ProfileViewModel(
                profileRepository = campusConnectApplication().container.profileRepository,
                authRepository    = campusConnectApplication().container.authRepository,
            )
        }
        initializer {
            ConversationsViewModel(
                messagingRepository = campusConnectApplication().container.messagingRepository,
                realtimeService     = campusConnectApplication().container.realtimeService,
            )
        }
        initializer {
            ChatViewModel(
                messagingRepository = campusConnectApplication().container.messagingRepository,
                realtimeService     = campusConnectApplication().container.realtimeService,
            )
        }
    }
}
