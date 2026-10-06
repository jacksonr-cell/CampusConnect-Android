package com.campusconnect.app.ui.screens.messaging

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campusconnect.app.data.model.ConversationsUiState
import com.campusconnect.app.data.repository.ApiResult
import com.campusconnect.app.data.repository.MessagingRepository
import com.campusconnect.app.realtime.RealtimeService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ConversationsViewModel(
    private val messagingRepository: MessagingRepository,
    private val realtimeService: RealtimeService,
) : ViewModel() {

    private val _state = MutableStateFlow<ConversationsUiState>(ConversationsUiState.Loading)
    val state: StateFlow<ConversationsUiState> = _state.asStateFlow()

    /**
     * `User.isOnline` from the REST-mapped User is always false (presence
     * isn't a REST concept — see UserMapper.kt) — this tracks who's
     * actually online from the gateway's real-time `user:status`
     * broadcasts instead, so the conversation list's online dots are
     * genuinely live rather than permanently showing "offline".
     */
    private val _onlineUserIds = MutableStateFlow<Set<String>>(emptySet())
    val onlineUserIds: StateFlow<Set<String>> = _onlineUserIds.asStateFlow()

    init {
        loadConversations()
        observeIncomingMessages()
        observeUserStatus()
    }

    fun loadConversations(refresh: Boolean = false) {
        viewModelScope.launch {
            if (refresh) {
                (_state.value as? ConversationsUiState.Success)?.let {
                    _state.value = it.copy(isRefreshing = true)
                }
            } else {
                _state.value = ConversationsUiState.Loading
            }

            when (val result = messagingRepository.getConversations(page = 1, limit = 50)) {
                is ApiResult.Success -> _state.value = ConversationsUiState.Success(result.data)
                is ApiResult.Error   -> _state.value = ConversationsUiState.Error(result.message)
            }
        }
    }

    /**
     * A new message arriving over the socket for ANY conversation should
     * bump that conversation to the top of the list and mark it unread —
     * reloading from the network on every incoming message would be
     * wasteful and laggy, so this just re-sorts/flags what's already
     * in memory. A full [loadConversations] refresh still happens on
     * pull-to-refresh or re-entering the screen, so this never drifts far
     * from the server's truth.
     */
    private fun observeIncomingMessages() {
        viewModelScope.launch {
            realtimeService.incomingMessages.collect { message ->
                val current = _state.value as? ConversationsUiState.Success ?: return@collect
                val updated = current.conversations.map { convo ->
                    if (convo.id != message.conversationId) return@map convo
                    convo.copy(
                        lastMessagePreview = message.content ?: convo.lastMessagePreview,
                        lastMessageAt      = System.currentTimeMillis(),
                        hasUnread          = true,
                    )
                }.sortedByDescending { it.lastMessageAt ?: 0L }
                _state.value = current.copy(conversations = updated)
            }
        }
    }

    private fun observeUserStatus() {
        viewModelScope.launch {
            realtimeService.userStatusEvents.collect { event ->
                _onlineUserIds.value = if (event.isOnline) {
                    _onlineUserIds.value + event.userId
                } else {
                    _onlineUserIds.value - event.userId
                }
            }
        }
    }
}
