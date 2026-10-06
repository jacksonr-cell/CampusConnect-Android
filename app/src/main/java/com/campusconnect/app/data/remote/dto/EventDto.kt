package com.campusconnect.app.data.remote.dto

data class EventDto(
    val id             : String,
    val organizerId    : String? = null,
    val organizer      : UserDto? = null,
    val clubId         : String? = null,
    val title          : String,
    val description    : String,
    val coverImage     : String? = null,
    val location       : String? = null,
    val startTime      : String,
    val endTime        : String? = null,
    val category       : String = "other",
    val isTicketed     : Boolean = false,
    // Decimal column — serialised as a string by TypeORM/pg. See DecimalParsing.kt.
    val ticketPrice    : String? = null,
    val maxAttendees   : Int? = null,
    val attendeesCount : Int = 0,
    val isCancelled    : Boolean = false,
    val isFeatured     : Boolean = false,
    /** The requesting user's own RSVP status, if any. Backend gap fix — see EventMapper.kt. */
    val myStatus       : String? = null,
)

data class RsvpRequest(val status: String)
