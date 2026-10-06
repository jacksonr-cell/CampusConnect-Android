package com.campusconnect.app.data.remote.mapper

import com.campusconnect.app.data.model.Amenity
import com.campusconnect.app.data.model.Property
import com.campusconnect.app.data.model.User
import com.campusconnect.app.data.remote.dto.PropertyAmenityDto
import com.campusconnect.app.data.remote.dto.PropertyDto

fun PropertyAmenityDto.toDomain(): Amenity = Amenity(
    name     = amenityName,
    category = category.toAmenityCategory(),
    icon     = category, // raw category string doubles as an icon-lookup key for now
)

/**
 * @param savedPropertyIds current user's saved-property id set, used to
 *        populate `isSaved` — the property list/detail endpoints don't
 *        return this per-item, so it's cross-referenced client-side against
 *        a separate `GET /accommodation/saved` call.
 */
fun PropertyDto.toDomain(savedPropertyIds: Set<String> = emptySet()): Property = Property(
    id                = id,
    landlord          = landlord?.toDomain() ?: User(
        id = landlordId ?: "unknown", username = "unknown", firstName = "Unknown", lastName = "",
    ),
    title             = title,
    description       = description,
    propertyType      = propertyType.toPropertyType(),
    address           = address,
    city              = city,
    neighborhood      = neighborhood,
    pricePerMonth     = pricePerMonth.toSafeDouble(),
    currency          = currency,
    deposit           = deposit.toSafeDoubleOrNull(),
    bedrooms          = bedrooms,
    bathrooms         = bathrooms,
    maxOccupants      = maxOccupants,
    images            = images.sortedByDescending { it.isPrimary }.map { it.imageUrl },
    amenities         = amenities.map { it.toDomain() },
    ratingAverage     = ratingAverage.toSafeDouble(),
    reviewsCount      = reviewsCount,
    isAvailable       = isAvailable,
    isVerified        = isVerified,
    isFeatured        = isFeatured,
    isSaved           = id in savedPropertyIds,
    viewsCount        = viewsCount,
    inquiriesCount    = inquiriesCount,
    distanceFromCampusKm = null, // no campus-distance field on the backend yet
)
