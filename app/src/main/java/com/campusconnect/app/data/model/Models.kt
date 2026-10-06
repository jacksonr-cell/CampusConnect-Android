package com.campusconnect.app.data.model

import androidx.compose.ui.graphics.Color
import com.campusconnect.app.ui.theme.*

// ── User ──────────────────────────────────────────────────────────────────
data class User(
    val id: String,
    val username: String,
    val firstName: String,
    val lastName: String,
    val avatarUrl: String? = null,
    val university: String? = null,
    val isVerified: Boolean = false,
    val isOnline: Boolean = false,
    val role: UserRole = UserRole.STUDENT,
) {
    val displayName get() = "$firstName $lastName"
    val initials    get() = "${firstName.firstOrNull() ?: ""}${lastName.firstOrNull() ?: ""}"
}

enum class UserRole { STUDENT, CLUB_LEADER, LANDLORD, ADMIN }

// ── Post ──────────────────────────────────────────────────────────────────
data class Post(
    val id: String,
    val author: User,
    val content: String? = null,
    val mediaUrls: List<String> = emptyList(),
    val postType: PostType = PostType.TEXT,
    val reactions: Map<ReactionType, Int> = emptyMap(),
    val commentsCount: Int = 0,
    val sharesCount: Int = 0,
    val viewsCount: Int = 0,
    val communityName: String? = null,
    val userReaction: ReactionType? = null,
    val isBookmarked: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)

data class PostComment(
    val id: String,
    val author: User,
    val content: String,
    val createdAt: Long = System.currentTimeMillis(),
)

enum class PostType { TEXT, IMAGE, VIDEO, POLL }

// ── Reactions ─────────────────────────────────────────────────────────────
enum class ReactionType(val emoji: String, val label: String, val tint: Color) {
    LIKE   ("👍", "Like",  LikeRed),
    LOVE   ("❤️", "Love",  LovePink),
    HAHA   ("😂", "Haha",  HahaYellow),
    WOW    ("😮", "Wow",   WowOrange),
    SAD    ("😢", "Sad",   SadBlue),
    ANGRY  ("😡", "Angry", ErrorRed),
}

val Post.totalReactions get() = reactions.values.sum()
val Post.dominantReaction get() = reactions.maxByOrNull { it.value }?.key

// ── Story ─────────────────────────────────────────────────────────────────
data class Story(
    val id: String,
    val user: User,
    val mediaUrl: String? = null,
    val isSeen: Boolean = false,
    val isOwn: Boolean = false,
)

// ── Feed UI State ─────────────────────────────────────────────────────────
sealed interface FeedState {
    data object Loading  : FeedState
    data class  Success(val posts: List<Post>, val isRefreshing: Boolean = false) : FeedState
    data class  Error(val message: String) : FeedState
}
