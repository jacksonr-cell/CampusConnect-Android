package com.campusconnect.app.data.remote.mapper

import com.campusconnect.app.data.model.CampusEvent
import com.campusconnect.app.data.model.User
import com.campusconnect.app.data.remote.dto.EventDto

/**
 * Backend now joins the requesting user's EventAttendee row and returns
 * `myStatus` directly (see EventsService.enrichWithMyStatus) — this used
 * to always be null as a documented stopgap; that's closed now.
 */
fun EventDto.toDomain(): CampusEvent = CampusEvent(
    id             = id,
    organizer      = organizer?.toDomain() ?: User(
        id = organizerId ?: "unknown", username = "unknown", firstName = "Unknown", lastName = "",
    ),
    clubName       = null, // club relation not currently included in the response
    title          = title,
    description    = description,
    coverImage     = coverImage,
    location       = location,
    startTime      = startTime.toEpochMillisOrNow(),
    endTime        = endTime.toEpochMillisOrNull(),
    category       = category.toEventCategory(),
    isTicketed     = isTicketed,
    ticketPrice    = ticketPrice.toSafeDoubleOrNull(),
    maxAttendees   = maxAttendees,
    attendeesCount = attendeesCount,
    isFeatured     = isFeatured,
    isCancelled    = isCancelled,
    myStatus       = myStatus.toAttendeeStatus(),
    attendeeAvatars= emptyList(),
)
