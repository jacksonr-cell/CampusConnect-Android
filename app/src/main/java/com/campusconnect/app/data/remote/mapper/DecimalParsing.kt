package com.campusconnect.app.data.remote.mapper

/**
 * TypeORM's `@Column({ type: 'decimal' })` fields (Property.pricePerMonth,
 * Property.deposit, Property.ratingAverage, Event.ticketPrice, etc.) are
 * serialised by `pg` as JSON STRINGS by default — e.g. `"8500.00"`, not the
 * number `8500` — because Postgres's `numeric` type can exceed JS's safe
 * integer/float range and the driver doesn't silently risk precision loss.
 *
 * Backend DTO fields for these columns are typed as `String?` and converted
 * with this helper, rather than as Double, so a stray non-numeric value
 * (or a future backend change to add a value transformer) never crashes
 * Gson parsing — it just falls back to 0.0.
 */
fun String?.toSafeDouble(default: Double = 0.0): Double =
    this?.toDoubleOrNull() ?: default

fun String?.toSafeDoubleOrNull(): Double? = this?.toDoubleOrNull()
