package com.campusconnect.app.data.repository

import com.campusconnect.app.data.model.Post
import com.campusconnect.app.data.model.PostComment
import com.campusconnect.app.data.model.ReactionType
import com.campusconnect.app.data.remote.api.PostsApi
import com.campusconnect.app.data.remote.dto.CreateCommentRequest
import com.campusconnect.app.data.remote.dto.CreatePostRequest
import com.campusconnect.app.data.remote.dto.ReactPostRequest
import com.campusconnect.app.data.remote.mapper.toEpochMillisOrNow
import com.campusconnect.app.data.remote.mapper.toApiValue
import com.campusconnect.app.data.remote.mapper.toDomain

class PostsRepository(private val postsApi: PostsApi) {

    suspend fun getFeed(page: Int = 1, limit: Int = 20): ApiResult<List<Post>> =
        when (val result = safeApiCall { postsApi.getFeed(page, limit) }) {
            is ApiResult.Success -> ApiResult.Success(result.data.data.map { it.toDomain() })
            is ApiResult.Error   -> result
        }

    /** Real endpoint — replaces the old feed-page-scanning stopgap in ProfileRepository. */
    suspend fun getPostsByUser(userId: String, page: Int = 1, limit: Int = 20): ApiResult<List<Post>> =
        when (val result = safeApiCall { postsApi.getPostsByUser(userId, page, limit) }) {
            is ApiResult.Success -> ApiResult.Success(result.data.data.map { it.toDomain() })
            is ApiResult.Error   -> result
        }

    suspend fun getPost(postId: String): ApiResult<Post> =
        when (val result = safeApiCall { postsApi.getPost(postId) }) {
            is ApiResult.Success -> ApiResult.Success(result.data.toDomain())
            is ApiResult.Error -> result
        }

    suspend fun createPost(content: String): ApiResult<Post> =
        when (val result = safeApiCall { postsApi.createPost(CreatePostRequest(content = content)) }) {
            is ApiResult.Success -> ApiResult.Success(result.data.toDomain())
            is ApiResult.Error -> result
        }

    suspend fun getComments(postId: String, page: Int = 1, limit: Int = 50): ApiResult<List<PostComment>> =
        when (val result = safeApiCall { postsApi.getComments(postId, page, limit) }) {
            is ApiResult.Success -> ApiResult.Success(result.data.data.map { dto ->
                PostComment(
                    id = dto.id,
                    author = dto.user?.toDomain() ?: com.campusconnect.app.data.model.User(
                        id = dto.userId,
                        username = "unknown",
                        firstName = "Unknown",
                        lastName = "",
                    ),
                    content = dto.content,
                    createdAt = dto.createdAt.toEpochMillisOrNow(),
                )
            })
            is ApiResult.Error -> result
        }

    suspend fun addComment(postId: String, content: String): ApiResult<PostComment> =
        when (val result = safeApiCall { postsApi.addComment(postId, CreateCommentRequest(content)) }) {
            is ApiResult.Success -> {
                val dto = result.data
                ApiResult.Success(
                    PostComment(
                        id = dto.id,
                        author = dto.user?.toDomain() ?: com.campusconnect.app.data.model.User(
                            id = dto.userId,
                            username = "unknown",
                            firstName = "Unknown",
                            lastName = "",
                        ),
                        content = dto.content,
                        createdAt = dto.createdAt.toEpochMillisOrNow(),
                    ),
                )
            }
            is ApiResult.Error -> result
        }

    suspend fun reactToPost(postId: String, reactionType: ReactionType): ApiResult<Unit> =
        when (val result = safeApiCall { postsApi.reactToPost(postId, ReactPostRequest(reactionType.toApiValue())) }) {
            is ApiResult.Success -> ApiResult.Success(Unit)
            is ApiResult.Error   -> result
        }
}
