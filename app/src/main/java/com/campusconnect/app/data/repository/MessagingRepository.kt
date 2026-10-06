package com.campusconnect.app.data.repository

import com.campusconnect.app.data.local.TokenManager
import com.campusconnect.app.data.model.ChatMessage
import com.campusconnect.app.data.model.Conversation
import com.campusconnect.app.data.remote.api.MessagingApi
import com.campusconnect.app.data.remote.dto.CreateGroupRequest
import com.campusconnect.app.data.remote.dto.SendMessageRequest
import com.campusconnect.app.data.remote.mapper.toDomain

class MessagingRepository(
    private val api: MessagingApi,
    private val tokenManager: TokenManager,
) {
    private val currentUserId: String
        get() = tokenManager.getUserId() ?: ""

    suspend fun getConversations(page: Int = 1, limit: Int = 20): ApiResult<List<Conversation>> =
        when (val result = safeApiCall { api.getConversations(page, limit) }) {
            is ApiResult.Success -> ApiResult.Success(result.data.data.map { it.toDomain(currentUserId) })
            is ApiResult.Error   -> result
        }

    suspend fun getOrCreateDirect(otherUserId: String): ApiResult<Conversation> =
        when (val result = safeApiCall { api.getOrCreateDirect(otherUserId) }) {
            is ApiResult.Success -> ApiResult.Success(result.data.toDomain(currentUserId))
            is ApiResult.Error   -> result
        }

    suspend fun createGroup(name: String, participantIds: List<String>): ApiResult<Conversation> =
        when (val result = safeApiCall { api.createGroup(CreateGroupRequest(name, participantIds)) }) {
            is ApiResult.Success -> ApiResult.Success(result.data.toDomain(currentUserId))
            is ApiResult.Error   -> result
        }

    suspend fun getMessages(conversationId: String, page: Int = 1, limit: Int = 50): ApiResult<List<ChatMessage>> =
        when (val result = safeApiCall { api.getMessages(conversationId, page, limit) }) {
            // Backend returns newest-first; reverse to oldest-first for a natural top-to-bottom chat reading order.
            is ApiResult.Success -> ApiResult.Success(result.data.data.reversed().map { it.toDomain(currentUserId) })
            is ApiResult.Error   -> result
        }

    suspend fun sendMessage(conversationId: String, content: String): ApiResult<ChatMessage> =
        when (val result = safeApiCall { api.sendMessage(conversationId, SendMessageRequest(content = content)) }) {
            is ApiResult.Success -> ApiResult.Success(result.data.toDomain(currentUserId))
            is ApiResult.Error   -> result
        }

    suspend fun markAsRead(conversationId: String): ApiResult<Unit> =
        when (val result = safeApiCall { api.markAsRead(conversationId) }) {
            is ApiResult.Success -> ApiResult.Success(Unit)
            is ApiResult.Error   -> result
        }
}
