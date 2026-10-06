package com.campusconnect.app.ui.screens.accommodation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campusconnect.app.data.model.*
import com.campusconnect.app.data.repository.AccommodationRepository
import com.campusconnect.app.data.repository.ApiResult
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class AccommodationViewModel(private val accommodationRepository: AccommodationRepository) : ViewModel() {

    private val _state = MutableStateFlow<AccommodationState>(AccommodationState.Loading)
    val state: StateFlow<AccommodationState> = _state.asStateFlow()

    private val _filters = MutableStateFlow(AccommodationFilters())
    val filters: StateFlow<AccommodationFilters> = _filters.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    /** Server-filtered result set — search text, min-bedrooms, and sort are applied client-side on top of this. */
    private var serverFilteredProperties: List<Property> = emptyList()

    init {
        loadProperties()
    }

    /**
     * Fetches from the server using whatever filters the backend actually
     * supports (city, propertyType, minPrice, maxPrice — see
     * AccommodationApi.getProperties). Search text, minimum bedrooms, and
     * sort order aren't backend query params, so those are applied
     * client-side in [applyClientSideRefinements] on top of this result.
     */
    fun loadProperties(refresh: Boolean = false) {
        viewModelScope.launch {
            if (refresh) {
                (_state.value as? AccommodationState.Success)?.let {
                    _state.value = it.copy(isRefreshing = true)
                }
            } else {
                _state.value = AccommodationState.Loading
            }

            val f = _filters.value
            val result = accommodationRepository.getProperties(
                page         = 1,
                limit        = 50, // fetch a generous page so client-side search/sort has enough to work with
                city         = f.city,
                propertyType = f.propertyType?.name?.lowercase(),
                minPrice     = f.minPrice,
                maxPrice     = f.maxPrice,
            )

            when (result) {
                is ApiResult.Success -> {
                    serverFilteredProperties = result.data
                    applyClientSideRefinements()
                }
                is ApiResult.Error -> _state.value = AccommodationState.Error(result.message)
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        applyClientSideRefinements()
    }

    /** City/type/price changed → these need a fresh server fetch. */
    fun updateFilters(update: AccommodationFilters.() -> AccommodationFilters) {
        _filters.value = _filters.value.update()
        loadProperties()
    }

    fun clearFilters() {
        _filters.value = AccommodationFilters()
        loadProperties()
    }

    /** Optimistic save/unsave — flips immediately, confirms against the API, rolls back on failure. */
    fun toggleSave(property: Property) {
        val wasSaved = property.isSaved
        serverFilteredProperties = serverFilteredProperties.map {
            if (it.id == property.id) it.copy(isSaved = !wasSaved) else it
        }
        applyClientSideRefinements()

        viewModelScope.launch {
            val result = accommodationRepository.toggleSave(property.id, currentlySaved = wasSaved)
            if (result is ApiResult.Error) {
                serverFilteredProperties = serverFilteredProperties.map {
                    if (it.id == property.id) it.copy(isSaved = wasSaved) else it
                }
                applyClientSideRefinements()
            }
        }
    }

    private fun applyClientSideRefinements() {
        val f = _filters.value
        val q = _searchQuery.value.trim().lowercase()

        var result = serverFilteredProperties

        if (q.isNotBlank()) {
            result = result.filter {
                it.title.lowercase().contains(q) ||
                it.city.lowercase().contains(q) ||
                (it.neighborhood?.lowercase()?.contains(q) == true)
            }
        }
        f.minBedrooms?.let { min -> result = result.filter { it.bedrooms >= min } }

        result = when (f.sortBy) {
            SortOption.FEATURED   -> result.sortedByDescending { it.isFeatured }
            SortOption.PRICE_LOW  -> result.sortedBy { it.pricePerMonth }
            SortOption.PRICE_HIGH -> result.sortedByDescending { it.pricePerMonth }
            SortOption.RATING     -> result.sortedByDescending { it.ratingAverage }
            SortOption.NEWEST     -> result
        }

        _state.value = AccommodationState.Success(result)
    }
}
