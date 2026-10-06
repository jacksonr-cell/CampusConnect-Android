package com.campusconnect.app.data.remote.dto

data class ParticipantDto(
    val id             : String,
    val userId         : String,
    val user           : UserDto? = null,
    val lastReadAt     : String? = null,
    val isMuted        : Boolean = false,
    val isAdmin        : Boolean = false,
)

data class ConversationDto(
    val id            : String,
    val type          : String = "direct",
    val name          : String? = null,
    val avatarUrl     : String? = null,
    val lastMessageAt : String? = null,
    val messagesCount : Int = 0,
    val participants  : List<ParticipantDto> = emptyList(),
    val createdAt      : String? = null,
    /** Attached server-side by MessagingService.attachLastMessages() — see EnrichedConversation. */
    val lastMessage   : MessageDto? = null,
)

data class MessageDto(
    val id             : String,
    val conversationId : String,
    val senderId       : String? = null,
    val sender         : UserDto? = null,
    val content        : String? = null,
    val messageType    : String = "text",
    val mediaUrl       : String? = null,
    val isRead         : Boolean = false,
    val replyToId      : String? = null,
    val createdAt      : String? = null,
)

data class SendMessageRequest(
    val content     : String? = null,
    val messageType : String? = null,
    val mediaUrl    : String? = null,
    val replyToId   : String? = null,
)

data class CreateGroupRequest(
    val name          : String,
    val participantIds: List<String>,
    val avatarUrl     : String? = null,
)
