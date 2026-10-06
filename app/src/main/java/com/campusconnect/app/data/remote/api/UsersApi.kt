package com.campusconnect.app.data.remote.api

import com.campusconnect.app.data.remote.dto.UserDto
import com.campusconnect.app.data.remote.dto.ApiEnvelope
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Path

interface UsersApi {

    @GET("users/profile")
    suspend fun getMyProfile(): ApiEnvelope<UserDto>

    @GET("users/{id}")
    suspend fun getUserById(@Path("id") id: String): ApiEnvelope<UserDto>
}
