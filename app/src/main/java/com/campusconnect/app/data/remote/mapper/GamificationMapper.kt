package com.campusconnect.app.data.remote.mapper

import com.campusconnect.app.data.model.Badge
import com.campusconnect.app.data.model.UserLevel
import com.campusconnect.app.data.remote.dto.BadgeCatalogItemDto
import com.campusconnect.app.data.remote.dto.UserPointsDto

/**
 * Backend gap fix: `isEarned`/`earnedAt` now come from the real catalog
 * response (server merges the user's UserBadge rows against every active
 * Badge) instead of being hardcoded `true` — locked badges are now
 * genuinely representable.
 */
fun BadgeCatalogItemDto.toDomain(): Badge = Badge(
    id          = id,
    name        = name,
    description = description,
    category    = category.toBadgeCategory(),
    isEarned    = isEarned,
    earnedAt    = earnedAt.toEpochMillisOrNull(),
)

fun UserPointsDto?.toLevel(): UserLevel = this?.level.toUserLevel()
fun UserPointsDto?.toTotalPoints(): Int = this?.totalPoints ?: 0
