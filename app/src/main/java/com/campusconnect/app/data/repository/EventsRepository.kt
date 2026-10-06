package com.campusconnect.app.data.repository

import com.campusconnect.app.data.model.AttendeeStatus
import com.campusconnect.app.data.model.CampusEvent
import com.campusconnect.app.data.remote.api.EventsApi
import com.campusconnect.app.data.remote.dto.RsvpRequest
import com.campusconnect.app.data.remote.mapper.toApiValue
import com.campusconnect.app.data.remote.mapper.toDomain

class EventsRepository(private val api: EventsApi) {

    suspend fun getEvents(page: Int = 1, limit: Int = 20): ApiResult<List<CampusEvent>> =
        when (val result = safeApiCall { api.getEvents(page, limit) }) {
            is ApiResult.Success -> ApiResult.Success(result.data.data.map { it.toDomain() })
            is ApiResult.Error   -> result
        }

    suspend fun rsvp(eventId: String, status: AttendeeStatus): ApiResult<Unit> =
        when (val result = safeApiCall { api.rsvp(eventId, RsvpRequest(status.toApiValue())) }) {
            is ApiResult.Success -> ApiResult.Success(Unit)
            is ApiResult.Error   -> result
        }
}
