package com.campusconnect.app.ui.screens.accommodation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campusconnect.app.data.model.Property
import com.campusconnect.app.data.repository.AccommodationRepository
import com.campusconnect.app.data.repository.ApiResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface PropertyDetailState {
    data object Loading : PropertyDetailState
    data class  Success(val property: Property) : PropertyDetailState
    data class  Error(val message: String) : PropertyDetailState
}

/**
 * Separate from [AccommodationViewModel] deliberately: fetching a single
 * property by id via GET /accommodation/properties/:id is a different
 * concern from the filtered/paginated list, and searching for a property
 * inside whatever page the list ViewModel happens to have loaded is
 * fragile (fails if it's filtered out or beyond the current page).
 */
class PropertyDetailViewModel(private val accommodationRepository: AccommodationRepository) : ViewModel() {

    private val _state = MutableStateFlow<PropertyDetailState>(PropertyDetailState.Loading)
    val state: StateFlow<PropertyDetailState> = _state.asStateFlow()

    private var loadedForId: String? = null

    fun loadProperty(id: String) {
        if (loadedForId == id && _state.value is PropertyDetailState.Success) return
        loadedForId = id
        viewModelScope.launch {
            _state.value = PropertyDetailState.Loading
            when (val result = accommodationRepository.getProperty(id)) {
                is ApiResult.Success -> _state.value = PropertyDetailState.Success(result.data)
                is ApiResult.Error   -> _state.value = PropertyDetailState.Error(result.message)
            }
        }
    }

    fun toggleSave(property: Property) {
        val wasSaved = property.isSaved
        _state.value = PropertyDetailState.Success(property.copy(isSaved = !wasSaved))

        viewModelScope.launch {
            val result = accommodationRepository.toggleSave(property.id, currentlySaved = wasSaved)
            if (result is ApiResult.Error) {
                _state.value = PropertyDetailState.Success(property.copy(isSaved = wasSaved))
            }
        }
    }

    fun sendInquiry(property: Property, message: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            when (val result = accommodationRepository.inquire(property.id, message, null, null)) {
                is ApiResult.Success -> onResult(true, null)
                is ApiResult.Error   -> onResult(false, result.message)
            }
        }
    }
}
