package com.campusconnect.app.data.remote.api

import com.campusconnect.app.data.remote.dto.*
import retrofit2.http.*

interface PostsApi {

    @GET("posts/feed")
    suspend fun getFeed(
        @Query("page")  page: Int = 1,
        @Query("limit") limit: Int = 20,
    ): ApiEnvelope<PaginatedDto<PostDto>>

    @GET("posts/user/{userId}")
    suspend fun getPostsByUser(
        @Path("userId") userId: String,
        @Query("page")  page: Int = 1,
        @Query("limit") limit: Int = 20,
    ): ApiEnvelope<PaginatedDto<PostDto>>

    @GET("posts/{id}")
    suspend fun getPost(@Path("id") id: String): ApiEnvelope<PostDto>

    @GET("posts/{id}/comments")
    suspend fun getComments(
        @Path("id") id: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 50,
    ): ApiEnvelope<PaginatedDto<PostCommentDto>>

    @POST("posts")
    suspend fun createPost(@Body body: CreatePostRequest): ApiEnvelope<PostDto>

    @POST("posts/{id}/comments")
    suspend fun addComment(
        @Path("id") id: String,
        @Body body: CreateCommentRequest,
    ): ApiEnvelope<PostCommentDto>

    @POST("posts/{id}/react")
    suspend fun reactToPost(
        @Path("id") id: String,
        @Body body: ReactPostRequest,
    ): ApiEnvelope<MessageResponseDto>

    @DELETE("posts/{id}")
    suspend fun deletePost(@Path("id") id: String): ApiEnvelope<MessageResponseDto>
}
