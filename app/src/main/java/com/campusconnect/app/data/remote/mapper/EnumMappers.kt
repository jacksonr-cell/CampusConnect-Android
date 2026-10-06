package com.campusconnect.app.data.remote.mapper

import com.campusconnect.app.data.model.*

/**
 * Backend enums are lowercase snake_case strings (TypeScript enum values).
 * These converters are deliberately defensive — an unrecognised value falls
 * back to a sensible default rather than crashing, so a backend enum
 * addition doesn't take down the whole screen on the next release.
 */

fun String?.toUserRole(): UserRole = when (this) {
    "club_leader" -> UserRole.CLUB_LEADER
    "landlord"    -> UserRole.LANDLORD
    "admin"       -> UserRole.ADMIN
    else          -> UserRole.STUDENT
}

fun String?.toPropertyType(): PropertyType = when (this) {
    "apartment"    -> PropertyType.APARTMENT
    "studio"       -> PropertyType.STUDIO
    "shared_room"  -> PropertyType.SHARED_ROOM
    "private_room" -> PropertyType.PRIVATE_ROOM
    "bedsitter"    -> PropertyType.BEDSITTER
    else           -> PropertyType.HOSTEL
}

fun String?.toAmenityCategory(): AmenityCategory = when (this) {
    "security"     -> AmenityCategory.SECURITY
    "connectivity" -> AmenityCategory.CONNECTIVITY
    "appliances"   -> AmenityCategory.APPLIANCES
    "transport"    -> AmenityCategory.TRANSPORT
    "utilities"    -> AmenityCategory.UTILITIES
    else           -> AmenityCategory.OTHER
}

fun String?.toEventCategory(): EventCategory = when (this) {
    "social"        -> EventCategory.SOCIAL
    "sports"        -> EventCategory.SPORTS
    "culture"       -> EventCategory.CULTURE
    "career"        -> EventCategory.CAREER
    "tech"          -> EventCategory.TECH
    "entertainment" -> EventCategory.ENTERTAINMENT
    "volunteer"     -> EventCategory.VOLUNTEER
    "academic"      -> EventCategory.ACADEMIC
    else            -> EventCategory.OTHER
}

fun String?.toAttendeeStatus(): AttendeeStatus? = when (this) {
    "going"     -> AttendeeStatus.GOING
    "maybe"     -> AttendeeStatus.MAYBE
    "not_going" -> AttendeeStatus.NOT_GOING
    else        -> null
}

fun String?.toReactionType(): ReactionType? = when (this) {
    "like"  -> ReactionType.LIKE
    "love"  -> ReactionType.LOVE
    "haha"  -> ReactionType.HAHA
    "wow"   -> ReactionType.WOW
    "sad"   -> ReactionType.SAD
    "angry" -> ReactionType.ANGRY
    else    -> null
}

fun String?.toBadgeCategory(): BadgeCategory = when (this) {
    "academic"      -> BadgeCategory.ACADEMIC
    "accommodation" -> BadgeCategory.ACCOMMODATION
    "events"        -> BadgeCategory.EVENTS
    "marketplace"   -> BadgeCategory.MARKETPLACE
    "special"       -> BadgeCategory.SPECIAL
    else            -> BadgeCategory.SOCIAL
}

fun String?.toUserLevel(): UserLevel = when (this) {
    "student"        -> UserLevel.STUDENT
    "active_student" -> UserLevel.ACTIVE_STUDENT
    "campus_regular" -> UserLevel.CAMPUS_REGULAR
    "campus_star"    -> UserLevel.CAMPUS_STAR
    "campus_legend"  -> UserLevel.CAMPUS_LEGEND
    else             -> UserLevel.NEWCOMER
}

fun ReactionType.toApiValue(): String = name.lowercase()
fun AttendeeStatus.toApiValue(): String = name.lowercase()
