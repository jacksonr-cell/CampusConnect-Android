package com.campusconnect.app.data.remote.mapper

import com.campusconnect.app.data.model.ChatMessage
import com.campusconnect.app.data.model.Conversation
import com.campusconnect.app.data.model.ConversationType
import com.campusconnect.app.data.model.MessageType
import com.campusconnect.app.data.model.User
import com.campusconnect.app.data.remote.dto.ConversationDto
import com.campusconnect.app.data.remote.dto.MessageDto
import com.campusconnect.app.data.remote.dto.ParticipantDto

private fun String.toConversationType(): ConversationType =
    if (this == "group") ConversationType.GROUP else ConversationType.DIRECT

private fun String.toMessageType(): MessageType = when (this) {
    "image"  -> MessageType.IMAGE
    "file"   -> MessageType.FILE
    "audio"  -> MessageType.AUDIO
    "system" -> MessageType.SYSTEM
    else     -> MessageType.TEXT
}

/**
 * DIRECT conversations have no stored name/avatar (only GROUP does) — the
 * display identity is resolved client-side from whichever participant
 * isn't the current user.
 */
fun ConversationDto.toDomain(currentUserId: String): Conversation {
    val type = type.toConversationType()
    val otherParticipant: ParticipantDto? = participants.firstOrNull { it.userId != currentUserId }
    val myParticipant: ParticipantDto? = participants.firstOrNull { it.userId == currentUserId }

    val resolvedName = if (type == ConversationType.GROUP) {
        name ?: "Group Chat"
    } else {
        otherParticipant?.user?.let { "${it.firstName} ${it.lastName}" } ?: "Unknown User"
    }
    val resolvedAvatar = if (type == ConversationType.GROUP) avatarUrl else otherParticipant?.user?.avatarUrl

    val lastMessageAtMillis = lastMessageAt.toEpochMillisOrNull()
    val myLastReadMillis = myParticipant?.lastReadAt.toEpochMillisOrNull()
    val hasUnread = lastMessageAtMillis != null &&
        (myLastReadMillis == null || lastMessageAtMillis > myLastReadMillis) &&
        lastMessage?.senderId != currentUserId // don't show unread for your own last message

    return Conversation(
        id                 = id,
        type               = type,
        displayName        = resolvedName,
        displayAvatarUrl   = resolvedAvatar,
        otherParticipant   = otherParticipant?.user?.toDomain(),
        lastMessagePreview = lastMessage?.let { previewTextFor(it) },
        lastMessageAt      = lastMessageAtMillis,
        hasUnread          = hasUnread,
        messagesCount      = messagesCount,
    )
}

private fun previewTextFor(dto: MessageDto): String = when (dto.messageType) {
    "image" -> "📷 Photo"
    "file"  -> "📎 File"
    "audio" -> "🎤 Voice note"
    else    -> dto.content ?: ""
}

fun MessageDto.toDomain(currentUserId: String): ChatMessage = ChatMessage(
    id             = id,
    conversationId = conversationId,
    sender         = sender?.toDomain() ?: User(
        id = senderId ?: "unknown", username = "unknown", firstName = "Unknown", lastName = "",
    ),
    content        = content,
    messageType    = messageType.toMessageType(),
    mediaUrl       = mediaUrl,
    isRead         = isRead,
    isMine         = senderId == currentUserId,
    replyToId      = replyToId,
    createdAt      = createdAt.toEpochMillisOrNow(),
)
