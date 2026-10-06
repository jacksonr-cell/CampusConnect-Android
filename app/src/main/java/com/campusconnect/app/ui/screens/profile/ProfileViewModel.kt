package com.campusconnect.app.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campusconnect.app.data.model.*
import com.campusconnect.app.data.repository.ApiResult
import com.campusconnect.app.data.repository.AuthRepository
import com.campusconnect.app.data.repository.ProfileRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val profileRepository: ProfileRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    private val _selectedTab = MutableStateFlow(ProfileTab.POSTS)
    val selectedTab: StateFlow<ProfileTab> = _selectedTab.asStateFlow()

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            _state.value = ProfileUiState.Loading

            when (val profileResult = profileRepository.getMyProfile()) {
                is ApiResult.Error -> _state.value = ProfileUiState.Error(profileResult.message)
                is ApiResult.Success -> {
                    val profile = profileResult.data

                    // Posts and saved properties are secondary to the core
                    // profile — degrade to an empty list rather than fail
                    // the whole screen if either call has trouble.
                    val postsResult = profileRepository.getMyPosts(profile.user.id)
                    val savedResult = profileRepository.getSavedProperties()

                    _state.value = ProfileUiState.Success(
                        profile         = profile,
                        posts           = (postsResult as? ApiResult.Success)?.data ?: emptyList(),
                        savedProperties = (savedResult as? ApiResult.Success)?.data ?: emptyList(),
                    )
                }
            }
        }
    }

    fun selectTab(tab: ProfileTab) {
        _selectedTab.value = tab
    }

    /**
     * Clears the local session (and best-effort revokes it server-side).
     * The nav graph observes TokenManager.isLoggedInFlow globally and
     * redirects to Login automatically once this completes — no explicit
     * navigation call needed here.
     */
    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
        }
    }
}
