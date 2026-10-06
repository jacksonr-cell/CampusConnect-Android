package com.campusconnect.app.data.remote.mapper

import java.time.Instant
import java.time.format.DateTimeParseException

/**
 * Backend timestamps are ISO-8601 strings (TypeORM's default `timestamp`
 * serialisation, e.g. "2026-07-08T14:30:00.000Z"). minSdk 26 means
 * java.time is available natively — no desugaring needed.
 */
fun String?.toEpochMillisOrNow(): Long {
    if (this.isNullOrBlank()) return System.currentTimeMillis()
    return try {
        Instant.parse(this).toEpochMilli()
    } catch (e: DateTimeParseException) {
        System.currentTimeMillis()
    }
}

fun String?.toEpochMillisOrNull(): Long? {
    if (this.isNullOrBlank()) return null
    return try {
        Instant.parse(this).toEpochMilli()
    } catch (e: DateTimeParseException) {
        null
    }
}
