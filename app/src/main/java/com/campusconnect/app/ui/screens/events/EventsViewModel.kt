package com.campusconnect.app.ui.screens.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campusconnect.app.data.model.*
import com.campusconnect.app.data.repository.ApiResult
import com.campusconnect.app.data.repository.EventsRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class EventsViewModel(private val eventsRepository: EventsRepository) : ViewModel() {

    private val _state = MutableStateFlow<EventsUiState>(EventsUiState.Loading)
    val state: StateFlow<EventsUiState> = _state.asStateFlow()

    private val _selectedCategory = MutableStateFlow<EventCategory?>(null)
    val selectedCategory: StateFlow<EventCategory?> = _selectedCategory.asStateFlow()

    private var allEvents: List<CampusEvent> = emptyList()

    init {
        loadEvents()
    }

    fun loadEvents(refresh: Boolean = false) {
        viewModelScope.launch {
            if (refresh) {
                (_state.value as? EventsUiState.Success)?.let {
                    _state.value = it.copy(isRefreshing = true)
                }
            } else {
                _state.value = EventsUiState.Loading
            }

            when (val result = eventsRepository.getEvents(page = 1, limit = 50)) {
                is ApiResult.Success -> {
                    allEvents = result.data
                    applyFilter()
                }
                is ApiResult.Error -> _state.value = EventsUiState.Error(result.message)
            }
        }
    }

    fun selectCategory(category: EventCategory?) {
        _selectedCategory.value = if (_selectedCategory.value == category) null else category
        applyFilter()
    }

    /**
     * Optimistic RSVP toggle — flips local state immediately, then confirms
     * against the real endpoint. The backend now returns the requester's
     * actual RSVP status on every load (previously a documented gap where
     * `myStatus` was always null on a fresh fetch), so this correctly
     * reflects prior RSVPs across app restarts too, not just within a session.
     */
    fun toggleRsvp(event: CampusEvent) {
        val wasGoing = event.myStatus == AttendeeStatus.GOING
        val newStatus = if (wasGoing) null else AttendeeStatus.GOING
        val delta = if (newStatus == AttendeeStatus.GOING) 1 else -1

        allEvents = allEvents.map {
            if (it.id != event.id) it
            else it.copy(myStatus = newStatus, attendeesCount = (it.attendeesCount + delta).coerceAtLeast(0))
        }
        applyFilter()

        viewModelScope.launch {
            val targetStatus = newStatus ?: AttendeeStatus.NOT_GOING
            val result = eventsRepository.rsvp(event.id, targetStatus)
            if (result is ApiResult.Error) {
                // Roll back on failure
                allEvents = allEvents.map {
                    if (it.id != event.id) it
                    else it.copy(myStatus = event.myStatus, attendeesCount = event.attendeesCount)
                }
                applyFilter()
            }
        }
    }

    private fun applyFilter() {
        val featured = allEvents.filter { it.isFeatured && !it.isCancelled }
        val category = _selectedCategory.value
        val upcoming = allEvents
            .filter { !it.isCancelled }
            .let { list -> if (category != null) list.filter { it.category == category } else list }
            .sortedBy { it.startTime }

        _state.value = EventsUiState.Success(featured = featured, upcoming = upcoming)
    }
}
