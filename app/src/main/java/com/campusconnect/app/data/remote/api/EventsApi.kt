package com.campusconnect.app.data.remote.api

import com.campusconnect.app.data.remote.dto.*
import retrofit2.http.*

interface EventsApi {

    @GET("events")
    suspend fun getEvents(
        @Query("page")  page: Int = 1,
        @Query("limit") limit: Int = 20,
    ): ApiEnvelope<PaginatedDto<EventDto>>

    @GET("events/{id}")
    suspend fun getEvent(@Path("id") id: String): ApiEnvelope<EventDto>

    @POST("events/{id}/rsvp")
    suspend fun rsvp(
        @Path("id") id: String,
        @Body body: RsvpRequest,
    ): ApiEnvelope<MessageResponseDto>
}
