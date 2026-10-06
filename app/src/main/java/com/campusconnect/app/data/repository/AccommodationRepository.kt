package com.campusconnect.app.data.repository

import com.campusconnect.app.data.model.Property
import com.campusconnect.app.data.remote.api.AccommodationApi
import com.campusconnect.app.data.remote.dto.CreateInquiryRequest
import com.campusconnect.app.data.remote.mapper.toDomain

/**
 * The property list/detail endpoints don't return per-item "is this saved
 * by me" state, so this repository keeps a lightweight client-side cache of
 * saved property ids — populated whenever [getSavedProperties] or
 * [toggleSave] runs — and cross-references it into every mapped [Property].
 * Until the user visits the Saved tab or saves something, the cache starts
 * empty and every card correctly shows as unsaved (never wrong, just
 * possibly stale until one of those two calls happens).
 */
class AccommodationRepository(private val api: AccommodationApi) {

    private var cachedSavedIds: Set<String> = emptySet()

    suspend fun getProperties(
        page: Int = 1, limit: Int = 20,
        city: String? = null, propertyType: String? = null,
        minPrice: Int? = null, maxPrice: Int? = null,
    ): ApiResult<List<Property>> =
        when (val result = safeApiCall { api.getProperties(page, limit, city, propertyType, minPrice, maxPrice) }) {
            is ApiResult.Success -> ApiResult.Success(result.data.data.map { it.toDomain(cachedSavedIds) })
            is ApiResult.Error   -> result
        }

    suspend fun getProperty(id: String): ApiResult<Property> =
        when (val result = safeApiCall { api.getProperty(id) }) {
            is ApiResult.Success -> ApiResult.Success(result.data.toDomain(cachedSavedIds))
            is ApiResult.Error   -> result
        }

    suspend fun getSavedProperties(page: Int = 1, limit: Int = 50): ApiResult<List<Property>> =
        when (val result = safeApiCall { api.getSavedProperties(page, limit) }) {
            is ApiResult.Success -> {
                cachedSavedIds = result.data.data.map { it.id }.toSet()
                ApiResult.Success(result.data.data.map { it.toDomain(cachedSavedIds) })
            }
            is ApiResult.Error -> result
        }

    suspend fun toggleSave(propertyId: String, currentlySaved: Boolean): ApiResult<Unit> {
        val result = if (currentlySaved) {
            safeApiCall { api.unsaveProperty(propertyId) }
        } else {
            safeApiCall { api.saveProperty(propertyId) }
        }
        return when (result) {
            is ApiResult.Success -> {
                cachedSavedIds = if (currentlySaved) cachedSavedIds - propertyId else cachedSavedIds + propertyId
                ApiResult.Success(Unit)
            }
            is ApiResult.Error -> result
        }
    }

    suspend fun inquire(propertyId: String, message: String, moveInDate: String?, moveOutDate: String?): ApiResult<Unit> =
        when (val result = safeApiCall {
            api.inquire(propertyId, CreateInquiryRequest(message, moveInDate, moveOutDate))
        }) {
            is ApiResult.Success -> ApiResult.Success(Unit)
            is ApiResult.Error   -> result
        }
}
