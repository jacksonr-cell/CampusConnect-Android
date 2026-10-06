package com.campusconnect.app.realtime

import android.util.Log
import com.campusconnect.app.data.local.TokenManager
import com.campusconnect.app.data.remote.NetworkConfig
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

/**
 * Wraps the Socket.IO connection to the backend's `/ws` namespace
 * (CampusConnectGateway). One instance lives for the app's lifetime,
 * created by [com.campusconnect.app.di.AppContainer] — connect()/disconnect()
 * are driven by auth state (see CampusConnectApp.kt), not by individual
 * screens, so the connection persists across navigation.
 *
 * Authentication matches what the backend's `afterInit` middleware expects:
 * a `token` field in the Socket.IO handshake `auth` payload, verified there
 * as a JWT access token before the connection is accepted.
 */
class RealtimeService(private val tokenManager: TokenManager) {

    private var socket: Socket? = null

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _incomingMessages = MutableSharedFlow<IncomingMessage>(extraBufferCapacity = 32)
    val incomingMessages: SharedFlow<IncomingMessage> = _incomingMessages.asSharedFlow()

    private val _typingEvents = MutableSharedFlow<TypingEvent>(extraBufferCapacity = 32)
    val typingEvents: SharedFlow<TypingEvent> = _typingEvents.asSharedFlow()

    private val _readReceipts = MutableSharedFlow<ReadReceiptEvent>(extraBufferCapacity = 32)
    val readReceipts: SharedFlow<ReadReceiptEvent> = _readReceipts.asSharedFlow()

    private val _userStatusEvents = MutableSharedFlow<UserStatusEvent>(extraBufferCapacity = 32)
    val userStatusEvents: SharedFlow<UserStatusEvent> = _userStatusEvents.asSharedFlow()

    private val _messageReactions = MutableSharedFlow<MessageReactionEvent>(extraBufferCapacity = 32)
    val messageReactions: SharedFlow<MessageReactionEvent> = _messageReactions.asSharedFlow()

    /** Fires once the backend actually emits these — see class doc note. */
    private val _notifications = MutableSharedFlow<NotificationEvent>(extraBufferCapacity = 32)
    val notifications: SharedFlow<NotificationEvent> = _notifications.asSharedFlow()

    /**
     * Simple in-memory "unread since last seen" count, incremented as real
     * `notification` events arrive over the socket and reset by
     * [markNotificationsSeen] (call when the user opens a notification bell
     * / notifications list). This is session-only — it does not persist
     * across app restarts and doesn't reconcile with GET /notifications'
     * actual isRead state; wire that up when a real notifications screen
     * is built.
     */
    private val _unreadNotificationCount = MutableStateFlow(0)
    val unreadNotificationCount: StateFlow<Int> = _unreadNotificationCount.asStateFlow()

    fun markNotificationsSeen() {
        _unreadNotificationCount.value = 0
    }

    fun connect() {
        if (socket?.connected() == true) return
        val token = tokenManager.getAccessTokenBlocking()
        if (token == null) {
            Log.w(TAG, "connect() called with no access token — skipping")
            return
        }

        _connectionState.value = ConnectionState.Connecting

        val options = IO.Options().apply {
            forceNew              = true
            reconnection           = true
            reconnectionAttempts   = Int.MAX_VALUE
            reconnectionDelay      = 1000
            reconnectionDelayMax   = 10_000
            transports             = arrayOf("websocket")
            auth                   = mapOf("token" to token)
        }

        val newSocket = IO.socket(java.net.URI.create(NetworkConfig.WEBSOCKET_URL), options)
        registerListeners(newSocket)
        newSocket.connect()
        socket = newSocket
    }

    fun disconnect() {
        socket?.disconnect()
        socket?.off()
        socket = null
        _connectionState.value = ConnectionState.Disconnected
    }

    fun joinConversation(conversationId: String) {
        socket?.emit("join:conversation", JSONObject().put("conversationId", conversationId))
    }

    fun leaveConversation(conversationId: String) {
        socket?.emit("leave:conversation", JSONObject().put("conversationId", conversationId))
    }

    fun sendMessage(conversationId: String, content: String) {
        val payload = JSONObject()
            .put("conversationId", conversationId)
            .put("content", content)
        socket?.emit("message:send", payload)
    }

    fun sendTypingStart(conversationId: String) {
        socket?.emit("typing:start", JSONObject().put("conversationId", conversationId))
    }

    fun sendTypingStop(conversationId: String) {
        socket?.emit("typing:stop", JSONObject().put("conversationId", conversationId))
    }

    fun markMessageRead(conversationId: String) {
        socket?.emit("message:read", JSONObject().put("conversationId", conversationId))
    }

    private fun registerListeners(socket: Socket) {
        socket.on(Socket.EVENT_CONNECT) {
            _connectionState.value = ConnectionState.Connected
            Log.d(TAG, "Connected to $TAG namespace")
        }
        socket.on(Socket.EVENT_DISCONNECT) {
            _connectionState.value = ConnectionState.Disconnected
        }
        socket.on(Socket.EVENT_CONNECT_ERROR) { args ->
            val message = args.firstOrNull()?.toString() ?: "Unknown connection error"
            _connectionState.value = ConnectionState.Error(message)
            Log.w(TAG, "Connect error: $message")
        }

        socket.on("message:new") { args ->
            (args.firstOrNull() as? JSONObject)?.let { json ->
                _incomingMessages.tryEmit(
                    IncomingMessage(
                        id             = json.optString("id"),
                        conversationId = json.optString("conversationId"),
                        senderId       = json.optJSONObject("sender")?.optString("id") ?: json.optString("senderId"),
                        content        = json.optStringOrNull("content"),
                        messageType    = json.optStringOrNull("messageType"),
                        mediaUrl       = json.optStringOrNull("mediaUrl"),
                        createdAt      = json.optStringOrNull("createdAt"),
                    ),
                )
            }
        }

        socket.on("typing:started") { args -> emitTyping(args, isTyping = true) }
        socket.on("typing:stopped") { args -> emitTyping(args, isTyping = false) }

        socket.on("message:read") { args ->
            (args.firstOrNull() as? JSONObject)?.let { json ->
                _readReceipts.tryEmit(
                    ReadReceiptEvent(
                        userId         = json.optString("userId"),
                        conversationId = json.optString("conversationId"),
                        readAt         = json.optStringOrNull("readAt"),
                    ),
                )
            }
        }

        socket.on("user:status") { args ->
            (args.firstOrNull() as? JSONObject)?.let { json ->
                _userStatusEvents.tryEmit(
                    UserStatusEvent(
                        userId   = json.optString("userId"),
                        isOnline = json.optBoolean("isOnline"),
                    ),
                )
            }
        }

        socket.on("message:reaction") { args ->
            (args.firstOrNull() as? JSONObject)?.let { json ->
                _messageReactions.tryEmit(
                    MessageReactionEvent(
                        messageId = json.optString("messageId"),
                        userId    = json.optString("userId"),
                        emoji     = json.optString("emoji"),
                    ),
                )
            }
        }

        socket.on("notification") { args ->
            (args.firstOrNull() as? JSONObject)?.let { json ->
                _notifications.tryEmit(
                    NotificationEvent(
                        id    = json.optStringOrNull("id"),
                        type  = json.optStringOrNull("type"),
                        title = json.optStringOrNull("title"),
                        body  = json.optStringOrNull("body"),
                    ),
                )
                _unreadNotificationCount.value += 1
            }
        }
    }

    private fun emitTyping(args: Array<Any>, isTyping: Boolean) {
        (args.firstOrNull() as? JSONObject)?.let { json ->
            _typingEvents.tryEmit(
                TypingEvent(
                    userId         = json.optString("userId"),
                    username       = json.optStringOrNull("username"),
                    conversationId = json.optString("conversationId"),
                    isTyping       = isTyping,
                ),
            )
        }
    }

    private fun JSONObject.optStringOrNull(key: String): String? =
        if (has(key) && !isNull(key)) optString(key) else null

    private companion object {
        const val TAG = "RealtimeService"
    }
}
