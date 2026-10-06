package com.campusconnect.app.data.remote.api

import com.campusconnect.app.data.remote.dto.*
import retrofit2.http.*

interface AccommodationApi {

    @GET("accommodation/properties")
    suspend fun getProperties(
        @Query("page")     page: Int = 1,
        @Query("limit")    limit: Int = 20,
        @Query("city")     city: String? = null,
        @Query("type")     type: String? = null,
        @Query("minPrice") minPrice: Int? = null,
        @Query("maxPrice") maxPrice: Int? = null,
    ): ApiEnvelope<PaginatedDto<PropertyDto>>

    @GET("accommodation/properties/{id}")
    suspend fun getProperty(@Path("id") id: String): ApiEnvelope<PropertyDto>

    @GET("accommodation/saved")
    suspend fun getSavedProperties(
        @Query("page")  page: Int = 1,
        @Query("limit") limit: Int = 20,
    ): ApiEnvelope<PaginatedDto<PropertyDto>>

    @POST("accommodation/properties/{id}/save")
    suspend fun saveProperty(@Path("id") id: String): ApiEnvelope<MessageResponseDto>

    @DELETE("accommodation/properties/{id}/save")
    suspend fun unsaveProperty(@Path("id") id: String): ApiEnvelope<MessageResponseDto>

    @POST("accommodation/properties/{id}/inquire")
    suspend fun inquire(
        @Path("id") id: String,
        @Body body: CreateInquiryRequest,
    ): ApiEnvelope<MessageResponseDto>
}
