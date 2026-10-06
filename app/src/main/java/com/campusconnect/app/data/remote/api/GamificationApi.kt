package com.campusconnect.app.data.remote.api

import com.campusconnect.app.data.remote.dto.ApiEnvelope
import com.campusconnect.app.data.remote.dto.BadgeCatalogItemDto
import com.campusconnect.app.data.remote.dto.UserPointsDto
import retrofit2.http.GET

interface GamificationApi {

    @GET("gamification/me/points")
    suspend fun getMyPoints(): ApiEnvelope<UserPointsDto>

    @GET("gamification/me/badges")
    suspend fun getMyBadges(): ApiEnvelope<List<BadgeCatalogItemDto>>
}
