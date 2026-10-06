package com.campusconnect.app.data.repository

import com.campusconnect.app.data.remote.dto.ApiEnvelope
import com.campusconnect.app.data.remote.dto.ApiErrorBody
import com.google.gson.Gson
import retrofit2.HttpException
import java.io.IOException

/** Uniform success/failure wrapper every repository method returns. */
sealed interface ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>
    data class Error(val message: String, val httpCode: Int? = null) : ApiResult<Nothing>
}

private val errorGson = Gson()

/**
 * Wraps a suspend Retrofit call, unwraps the backend's `ApiEnvelope`, and
 * converts any failure — network, HTTP error, or unexpected exception —
 * into a single [ApiResult.Error] with the most useful message available:
 * the backend's own error text when present, otherwise a generic fallback.
 */
suspend fun <T> safeApiCall(call: suspend () -> ApiEnvelope<T>): ApiResult<T> {
    return try {
        val envelope = call()
        if (envelope.success && envelope.data != null) {
            ApiResult.Success(envelope.data)
        } else {
            ApiResult.Error(envelope.message ?: "Request failed")
        }
    } catch (e: HttpException) {
        val rawBody = try { e.response()?.errorBody()?.string() } catch (_: Exception) { null }
        val parsedMessage = try {
            rawBody?.let { errorGson.fromJson(it, ApiErrorBody::class.java) }?.error?.firstOrNull()
        } catch (_: Exception) { null }
        ApiResult.Error(parsedMessage ?: "Server error (${e.code()})", e.code())
    } catch (e: IOException) {
        ApiResult.Error("Network error — check your connection and that the backend is reachable")
    } catch (e: Exception) {
        ApiResult.Error(e.message ?: "Unexpected error")
    }
}

inline fun <T, R> ApiResult<T>.map(transform: (T) -> R): ApiResult<R> = when (this) {
    is ApiResult.Success -> ApiResult.Success(transform(data))
    is ApiResult.Error    -> this
}

inline fun <T> ApiResult<T>.onSuccess(action: (T) -> Unit): ApiResult<T> {
    if (this is ApiResult.Success) action(data)
    return this
}

inline fun <T> ApiResult<T>.onError(action: (String) -> Unit): ApiResult<T> {
    if (this is ApiResult.Error) action(message)
    return this
}
