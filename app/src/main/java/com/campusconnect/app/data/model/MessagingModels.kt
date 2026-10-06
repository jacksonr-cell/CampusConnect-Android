package com.campusconnect.app.data.model

enum class ConversationType { DIRECT, GROUP }

enum class MessageType { TEXT, IMAGE, FILE, AUDIO, SYSTEM }

data class Conversation(
    val id                : String,
    val type              : ConversationType,
    /** Resolved display name — the group's name for GROUP, or the other participant's name for DIRECT. */
    val displayName       : String,
    val displayAvatarUrl  : String?,
    /** For DIRECT conversations, the other person — used to open their profile, show online status, etc. */
    val otherParticipant  : User?,
    val lastMessagePreview: String?,
    val lastMessageAt     : Long?,
    val hasUnread         : Boolean,
    val messagesCount     : Int,
)

data class ChatMessage(
    val id             : String,
    val conversationId : String,
    val sender         : User,
    val content        : String?,
    val messageType    : MessageType,
    val mediaUrl       : String?,
    val isRead         : Boolean,
    /** True if the current user sent this — drives bubble alignment/colour. */
    val isMine          : Boolean,
    val replyToId      : String?,
    val createdAt      : Long,
    /** Client-only — true while an optimistically-sent message hasn't been confirmed by the server yet. */
    val isSending      : Boolean = false,
    val failedToSend   : Boolean = false,
)

sealed interface ConversationsUiState {
    data object Loading : ConversationsUiState
    data class  Success(val conversations: List<Conversation>, val isRefreshing: Boolean = false) : ConversationsUiState
    data class  Error(val message: String) : ConversationsUiState
}

sealed interface ChatUiState {
    data object Loading : ChatUiState
    data class  Success(val messages: List<ChatMessage>) : ChatUiState
    data class  Error(val message: String) : ChatUiState
}
