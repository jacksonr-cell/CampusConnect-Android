package com.campusconnect.app.ui.screens.messaging

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campusconnect.app.data.model.*
import com.campusconnect.app.data.remote.mapper.toEpochMillisOrNow
import com.campusconnect.app.data.repository.ApiResult
import com.campusconnect.app.data.repository.MessagingRepository
import com.campusconnect.app.realtime.RealtimeService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChatViewModel(
    private val messagingRepository: MessagingRepository,
    private val realtimeService: RealtimeService,
) : ViewModel() {

    private val _state = MutableStateFlow<ChatUiState>(ChatUiState.Loading)
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    private val _otherUserTyping = MutableStateFlow(false)
    val otherUserTyping: StateFlow<Boolean> = _otherUserTyping.asStateFlow()

    private var conversationId: String? = null
    private var typingResetJob: Job? = null

    /** Called once when the screen first composes with its conversationId. */
    fun initialize(conversationId: String) {
        if (this.conversationId == conversationId) return // already initialized for this conversation
        this.conversationId = conversationId

        realtimeService.joinConversation(conversationId)
        loadMessages(conversationId)
        markAsRead(conversationId)
        observeRealtimeEvents(conversationId)
    }

    override fun onCleared() {
        super.onCleared()
        conversationId?.let { realtimeService.leaveConversation(it) }
    }

    fun loadMessages(conversationId: String) {
        viewModelScope.launch {
            _state.value = ChatUiState.Loading
            when (val result = messagingRepository.getMessages(conversationId)) {
                is ApiResult.Success -> _state.value = ChatUiState.Success(result.data)
                is ApiResult.Error   -> _state.value = ChatUiState.Error(result.message)
            }
        }
    }

    /**
     * Optimistic send: a temporary bubble appears instantly with `isSending`,
     * then gets swapped for the server-confirmed message on success, or
     * flagged `failedToSend` (kept visible, not silently dropped) on failure.
     */
    fun sendMessage(content: String) {
        val id = conversationId ?: return
        if (content.isBlank()) return

        val tempId = "temp-${System.currentTimeMillis()}"
        val optimisticMessage = ChatMessage(
            id             = tempId,
            conversationId = id,
            sender         = User(id = "me", username = "me", firstName = "", lastName = ""),
            content        = content,
            messageType    = MessageType.TEXT,
            mediaUrl       = null,
            isRead         = false,
            isMine         = true,
            replyToId      = null,
            createdAt      = System.currentTimeMillis(),
            isSending      = true,
        )

        val current = _state.value as? ChatUiState.Success
        _state.value = ChatUiState.Success((current?.messages ?: emptyList()) + optimisticMessage)

        viewModelScope.launch {
            when (val result = messagingRepository.sendMessage(id, content)) {
                is ApiResult.Success -> replaceMessage(tempId, result.data)
                is ApiResult.Error   -> markFailed(tempId)
            }
        }
    }

    fun retrySend(failedMessage: ChatMessage) {
        removeMessage(failedMessage.id)
        sendMessage(failedMessage.content.orEmpty())
    }

    fun onTypingChanged(isTyping: Boolean) {
        val id = conversationId ?: return
        if (isTyping) realtimeService.sendTypingStart(id) else realtimeService.sendTypingStop(id)
    }

    private fun markAsRead(conversationId: String) {
        viewModelScope.launch {
            messagingRepository.markAsRead(conversationId)
            realtimeService.markMessageRead(conversationId)
        }
    }

    private fun observeRealtimeEvents(conversationId: String) {
        viewModelScope.launch {
            realtimeService.incomingMessages.collect { incoming ->
                if (incoming.conversationId != conversationId) return@collect
                appendIfNew(incoming)
                // A message arrived while this screen is open — treat it as read immediately.
                markAsRead(conversationId)
            }
        }
        viewModelScope.launch {
            realtimeService.typingEvents.collect { event ->
                if (event.conversationId != conversationId) return@collect
                _otherUserTyping.value = event.isTyping
                typingResetJob?.cancel()
                if (event.isTyping) {
                    // Safety net: clear the indicator if a "stopped" event is ever missed.
                    typingResetJob = viewModelScope.launch {
                        delay(6000)
                        _otherUserTyping.value = false
                    }
                }
            }
        }
    }

    /**
     * The backend now broadcasts `message:new` to the WHOLE conversation
     * room, including the sender (see the backend fix making REST-based
     * sends real-time) — so a message THIS device just sent via REST will
     * also arrive back over the socket. Dedupe by id so it never appears
     * twice.
     */
    private fun appendIfNew(incoming: com.campusconnect.app.realtime.IncomingMessage) {
        val current = _state.value as? ChatUiState.Success ?: return
        if (current.messages.any { it.id == incoming.id }) return

        val message = ChatMessage(
            id             = incoming.id,
            conversationId = incoming.conversationId,
            sender         = User(id = incoming.senderId, username = "", firstName = "", lastName = ""),
            content        = incoming.content,
            messageType    = MessageType.TEXT,
            mediaUrl       = incoming.mediaUrl,
            isRead         = false,
            isMine         = false, // this device's own sends are added optimistically, not via this path
            replyToId      = null,
            createdAt      = incoming.createdAt.toEpochMillisOrNow(),
        )
        _state.value = current.copy(messages = current.messages + message)
    }

    private fun replaceMessage(tempId: String, confirmed: ChatMessage) {
        val current = _state.value as? ChatUiState.Success ?: return
        _state.value = current.copy(
            messages = current.messages.map { if (it.id == tempId) confirmed else it },
        )
    }

    private fun markFailed(tempId: String) {
        val current = _state.value as? ChatUiState.Success ?: return
        _state.value = current.copy(
            messages = current.messages.map {
                if (it.id == tempId) it.copy(isSending = false, failedToSend = true) else it
            },
        )
    }

    private fun removeMessage(id: String) {
        val current = _state.value as? ChatUiState.Success ?: return
        _state.value = current.copy(messages = current.messages.filterNot { it.id == id })
    }
}
