package com.campusconnect.app.data.remote.api

import com.campusconnect.app.data.remote.dto.*
import retrofit2.http.*

interface MessagingApi {

    @GET("messaging/conversations")
    suspend fun getConversations(
        @Query("page")  page: Int = 1,
        @Query("limit") limit: Int = 20,
    ): ApiEnvelope<PaginatedDto<ConversationDto>>

    @POST("messaging/conversations/direct/{userId}")
    suspend fun getOrCreateDirect(@Path("userId") userId: String): ApiEnvelope<ConversationDto>

    @POST("messaging/conversations/group")
    suspend fun createGroup(@Body body: CreateGroupRequest): ApiEnvelope<ConversationDto>

    @GET("messaging/conversations/{id}/messages")
    suspend fun getMessages(
        @Path("id")     conversationId: String,
        @Query("page")  page: Int = 1,
        @Query("limit") limit: Int = 50,
    ): ApiEnvelope<PaginatedDto<MessageDto>>

    @POST("messaging/conversations/{id}/messages")
    suspend fun sendMessage(
        @Path("id") conversationId: String,
        @Body body: SendMessageRequest,
    ): ApiEnvelope<MessageDto>

    @POST("messaging/conversations/{id}/read")
    suspend fun markAsRead(@Path("id") conversationId: String): ApiEnvelope<MessageResponseDto>
}
