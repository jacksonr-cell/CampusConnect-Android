package com.campusconnect.app.realtime

/**
 * Event payloads mirror what CampusConnectGateway (backend Round 1) emits
 * over the `/ws` namespace. See src/modules/websocket/websocket.gateway.ts.
 */

data class IncomingMessage(
    val id             : String,
    val conversationId : String,
    val senderId       : String,
    val content        : String?,
    val messageType    : String?,
    val mediaUrl       : String?,
    val createdAt      : String?,
)

data class TypingEvent(
    val userId         : String,
    val username       : String?,
    val conversationId : String,
    val isTyping       : Boolean,
)

data class ReadReceiptEvent(
    val userId         : String,
    val conversationId : String,
    val readAt         : String?,
)

data class UserStatusEvent(
    val userId   : String,
    val isOnline : Boolean,
)

data class MessageReactionEvent(
    val messageId : String,
    val userId    : String,
    val emoji     : String,
)

/**
 * NOTE: as of Round 1, the backend gateway exposes `emitToUser(...)` as a
 * public method but nothing calls it yet when a Notification is created —
 * see the Round-4 backend fix that wires NotificationsService to call it.
 * This event class is ready on the client the moment that fix ships.
 */
data class NotificationEvent(
    val id     : String?,
    val type   : String?,
    val title  : String?,
    val body   : String?,
)

sealed interface ConnectionState {
    data object Disconnected : ConnectionState
    data object Connecting   : ConnectionState
    data object Connected    : ConnectionState
    data class  Error(val message: String) : ConnectionState
}
