package com.campusconnect.app.data.remote.dto

data class UserPointsDto(
    val userId       : String,
    val totalPoints  : Int = 0,
    val monthlyPoints: Int = 0,
    val level        : String = "newcomer",
)

/**
 * GET /gamification/me/badges now returns the FULL badge catalog (every
 * active badge) with earned/locked status merged in server-side — this
 * used to be a flat list of only-earned UserBadge rows with a nested
 * `badge` object; the backend gap (no catalog endpoint) is closed, so the
 * shape is flatter and includes locked badges too.
 */
data class BadgeCatalogItemDto(
    val id          : String,
    val name        : String,
    val description : String,
    val iconUrl     : String? = null,
    val category    : String = "special",
    val pointsReward: Int = 0,
    val isEarned    : Boolean = false,
    val earnedAt    : String? = null,
)
