package com.campusconnect.app.data.remote.dto

data class PropertyImageDto(
    val id       : String,
    val imageUrl : String,
    val isPrimary: Boolean = false,
    val sortOrder: Int = 0,
)

data class PropertyAmenityDto(
    val id          : String,
    val amenityName : String,
    val category    : String = "other",
)

data class PropertyDto(
    val id             : String,
    val landlordId     : String? = null,
    val landlord       : UserDto? = null,
    val title          : String,
    val description    : String,
    val propertyType   : String,
    val address        : String,
    val city           : String,
    val neighborhood   : String? = null,
    // Decimal columns — TypeORM/pg serialise these as STRINGS. See DecimalParsing.kt.
    val pricePerMonth  : String,
    val currency       : String = "KES",
    val deposit        : String? = null,
    val ratingAverage  : String  = "0",
    val bedrooms       : Int = 1,
    val bathrooms      : Int = 1,
    val maxOccupants   : Int = 1,
    val reviewsCount   : Int = 0,
    val viewsCount     : Int = 0,
    val inquiriesCount : Int = 0,
    val isAvailable    : Boolean = true,
    val isVerified     : Boolean = false,
    val isFeatured     : Boolean = false,
    val images         : List<PropertyImageDto> = emptyList(),
    val amenities      : List<PropertyAmenityDto> = emptyList(),
)

data class CreateInquiryRequest(
    val message    : String,
    val moveInDate : String? = null,
    val moveOutDate: String? = null,
)
