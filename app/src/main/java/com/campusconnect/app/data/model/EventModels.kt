package com.campusconnect.app.data.model

import androidx.compose.ui.graphics.Color
import com.campusconnect.app.ui.theme.*

enum class EventCategory(val label: String, val tint: Color) {
    ACADEMIC     ("Academic",      InfoBlue),
    SOCIAL       ("Social",        LovePink),
    SPORTS       ("Sports",        SuccessGreen),
    CULTURE      ("Culture",       NeonViolet),
    CAREER       ("Career",        WarningAmber),
    TECH         ("Tech",          ElectricPurple),
    ENTERTAINMENT("Entertainment", HahaYellow),
    VOLUNTEER    ("Volunteer",     OnlineGreen),
    OTHER        ("Other",         TextSecondary),
}

enum class AttendeeStatus { GOING, MAYBE, NOT_GOING }

data class CampusEvent(
    val id             : String,
    val organizer      : User,
    val clubName       : String? = null,
    val title          : String,
    val description    : String,
    val coverImage     : String? = null,
    val location       : String? = null,
    val startTime      : Long,
    val endTime        : Long? = null,
    val category       : EventCategory = EventCategory.OTHER,
    val isTicketed     : Boolean = false,
    val ticketPrice    : Double? = null,
    val maxAttendees   : Int? = null,
    val attendeesCount : Int = 0,
    val isFeatured     : Boolean = false,
    val isCancelled    : Boolean = false,
    val myStatus       : AttendeeStatus? = null,
    val attendeeAvatars: List<String> = emptyList(), // preview avatars for "N going"
)

sealed interface EventsUiState {
    data object Loading : EventsUiState
    data class  Success(
        val featured : List<CampusEvent>,
        val upcoming : List<CampusEvent>,
        val isRefreshing: Boolean = false,
    ) : EventsUiState
    data class  Error(val message: String) : EventsUiState
}
