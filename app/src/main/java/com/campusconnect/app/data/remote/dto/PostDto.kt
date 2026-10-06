package com.campusconnect.app.data.remote.dto

data class PostDto(
    val id           : String,
    val userId       : String? = null,
    val user         : UserDto? = null,
    val content      : String? = null,
    val postType     : String  = "text",
    val mediaUrls    : List<String> = emptyList(),
    val communityId  : String? = null,
    val visibility   : String  = "public",
    val likesCount   : Int     = 0,
    val commentsCount: Int     = 0,
    val sharesCount  : Int     = 0,
    val viewsCount   : Int     = 0,
    val createdAt    : String? = null,
    /** Per-emoji reaction counts, e.g. {"like": 12, "love": 3}. Backend gap fix — see PostMapper.kt. */
    val reactionsBreakdown: Map<String, Int> = emptyMap(),
    /** The requesting user's own reaction on this post, if any. */
    val myReaction   : String? = null,
)

data class CreatePostRequest(
    val content     : String?       = null,
    val postType    : String?       = null,
    val mediaUrls   : List<String>? = null,
    val communityId : String?       = null,
)

data class ReactPostRequest(val reactionType: String)

data class CreateCommentRequest(val content: String)

data class PostCommentDto(
    val id              : String,
    val postId          : String,
    val userId          : String,
    val content         : String,
    val parentCommentId : String? = null,
    val user            : UserDto? = null,
    val createdAt       : String? = null,
)
