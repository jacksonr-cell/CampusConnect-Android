package com.campusconnect.app.data.model

enum class PropertyType(val label: String) {
    HOSTEL       ("Hostel"),
    APARTMENT    ("Apartment"),
    STUDIO       ("Studio"),
    SHARED_ROOM  ("Shared Room"),
    PRIVATE_ROOM ("Private Room"),
    BEDSITTER    ("Bedsitter"),
}

enum class AmenityCategory { UTILITIES, SECURITY, CONNECTIVITY, APPLIANCES, TRANSPORT, OTHER }

data class Amenity(
    val name    : String,
    val category: AmenityCategory,
    val icon    : String, // maps to an icon in AmenityIcons
)

data class PropertyReview(
    val id        : String,
    val reviewer  : User,
    val rating    : Int,
    val content   : String? = null,
    val ratingCleanliness: Int? = null,
    val ratingValue      : Int? = null,
    val ratingLocation   : Int? = null,
    val ratingLandlord   : Int? = null,
    val createdAt : Long,
)

data class Property(
    val id             : String,
    val landlord       : User,
    val title          : String,
    val description    : String,
    val propertyType   : PropertyType,
    val address        : String,
    val city           : String,
    val neighborhood   : String? = null,
    val pricePerMonth  : Double,
    val currency       : String = "KES",
    val deposit        : Double? = null,
    val bedrooms       : Int = 1,
    val bathrooms      : Int = 1,
    val maxOccupants   : Int = 1,
    val images         : List<String> = emptyList(),
    val amenities      : List<Amenity> = emptyList(),
    val ratingAverage  : Double = 0.0,
    val reviewsCount   : Int = 0,
    val isAvailable    : Boolean = true,
    val isVerified     : Boolean = false,
    val isFeatured     : Boolean = false,
    val isSaved        : Boolean = false,
    val viewsCount     : Int = 0,
    val inquiriesCount : Int = 0,
    val distanceFromCampusKm: Double? = null,
)

enum class BookingStatus { INQUIRY, PENDING, ACCEPTED, REJECTED, CANCELLED, COMPLETED }

data class AccommodationFilters(
    val city        : String? = null,
    val propertyType: PropertyType? = null,
    val minPrice    : Int? = null,
    val maxPrice    : Int? = null,
    val minBedrooms : Int? = null,
    val sortBy      : SortOption = SortOption.FEATURED,
)

enum class SortOption(val label: String) {
    FEATURED   ("Featured"),
    PRICE_LOW  ("Price: Low to High"),
    PRICE_HIGH ("Price: High to Low"),
    RATING     ("Highest Rated"),
    NEWEST     ("Newest"),
}

sealed interface AccommodationState {
    data object Loading : AccommodationState
    data class  Success(val properties: List<Property>, val isRefreshing: Boolean = false) : AccommodationState
    data class  Error(val message: String) : AccommodationState
}
