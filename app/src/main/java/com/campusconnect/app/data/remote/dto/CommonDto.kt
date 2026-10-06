package com.campusconnect.app.data.remote.dto

/**
 * Matches the backend's global ResponseInterceptor envelope
 * (src/common/interceptors/response.interceptor.ts):
 *
 *   { success, statusCode, message, data, timestamp }
 *
 * `data` is generic — for list endpoints it's a [PaginatedDto], for single
 * resources it's the resource DTO directly.
 */
data class ApiEnvelope<T>(
    val success   : Boolean,
    val statusCode: Int,
    val message   : String?,
    val data      : T?,
    val timestamp : String?,
)

/**
 * Matches src/common/dto/pagination.dto.ts's `paginate()` helper output:
 *
 *   { data: T[], meta: { total, page, limit, totalPages, hasNextPage, hasPreviousPage } }
 */
data class PaginatedDto<T>(
    val data: List<T> = emptyList(),
    val meta: PaginationMetaDto? = null,
)

data class PaginationMetaDto(
    val total          : Int = 0,
    val page           : Int = 1,
    val limit          : Int = 20,
    val totalPages      : Int = 0,
    val hasNextPage     : Boolean = false,
    val hasPreviousPage : Boolean = false,
)

/** Generic backend error body shape from AllExceptionsFilter. */
data class ApiErrorBody(
    val success   : Boolean = false,
    val statusCode: Int = 0,
    val error     : List<String> = emptyList(),
)

/**
 * Many backend endpoints (logout, react, rsvp, save/unsave, etc.) return a
 * plain `{ message: "..." }` acknowledgement rather than a real resource —
 * used as the `data` type for those instead of `Unit`, since deserialising
 * an arbitrary JSON object into Kotlin's `Unit` via Gson is undefined.
 */
data class MessageResponseDto(val message: String? = null)
