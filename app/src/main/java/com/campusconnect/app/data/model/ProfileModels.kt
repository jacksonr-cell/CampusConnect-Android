package com.campusconnect.app.data.model

import androidx.compose.ui.graphics.Color
import com.campusconnect.app.ui.theme.*

enum class UserLevel(val label: String, val minPoints: Int, val tint: Color) {
    NEWCOMER       ("Newcomer",        0,     TextTertiary),
    STUDENT        ("Student",         100,   InfoBlue),
    ACTIVE_STUDENT ("Active Student",  500,   SuccessGreen),
    CAMPUS_REGULAR ("Campus Regular",  1500,  ElectricPurple),
    CAMPUS_STAR    ("Campus Star",     5000,  NeonViolet),
    CAMPUS_LEGEND  ("Campus Legend",   15000, WarningAmber),
}

/** Returns the level immediately above this one, or null if already max. */
fun UserLevel.next(): UserLevel? {
    val all = UserLevel.entries
    val idx = all.indexOf(this)
    return all.getOrNull(idx + 1)
}

enum class BadgeCategory(val emoji: String, val tint: Color) {
    SOCIAL       ("💬", InfoBlue),
    ACADEMIC     ("📚", ElectricPurple),
    ACCOMMODATION("🏠", NeonViolet),
    EVENTS       ("🎉", WarningAmber),
    MARKETPLACE  ("🛍️", SuccessGreen),
    SPECIAL      ("🏆", LovePink),
}

data class Badge(
    val id         : String,
    val name       : String,
    val description: String,
    val category   : BadgeCategory,
    val isEarned   : Boolean,
    val earnedAt   : Long? = null,
)

data class ProfileStats(
    val postsCount      : Int,
    val followersCount  : Int,
    val followingCount  : Int,
    val reputationPoints: Int,
    val level           : UserLevel,
)

/** Progress fraction (0f–1f) toward the next level, for a progress bar. */
val ProfileStats.progressToNextLevel: Float
    get() {
        val next = level.next() ?: return 1f
        val range = (next.minPoints - level.minPoints).toFloat()
        if (range <= 0f) return 1f
        return ((reputationPoints - level.minPoints) / range).coerceIn(0f, 1f)
    }

data class UserProfile(
    val user          : User,
    val bio           : String? = null,
    val coverImageUrl : String? = null,
    val courseOfStudy : String? = null,
    val yearOfStudy    : Int? = null,
    val joinedAtMillis: Long,
    val stats         : ProfileStats,
    val badges        : List<Badge> = emptyList(),
    val isOwnProfile   : Boolean = true,
    val isFollowing    : Boolean = false,
)

enum class ProfileTab(val label: String) {
    POSTS ("Posts"),
    SAVED ("Saved"),
    BADGES("Badges"),
}

sealed interface ProfileUiState {
    data object Loading : ProfileUiState
    data class  Success(
        val profile    : UserProfile,
        val posts      : List<Post> = emptyList(),
        val savedProperties: List<Property> = emptyList(),
    ) : ProfileUiState
    data class  Error(val message: String) : ProfileUiState
}
