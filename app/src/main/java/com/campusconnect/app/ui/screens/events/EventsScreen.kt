package com.campusconnect.app.ui.screens.events

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.campusconnect.app.data.model.EventCategory
import com.campusconnect.app.data.model.EventsUiState
import com.campusconnect.app.ui.components.EventCard
import com.campusconnect.app.ui.components.Featured3DCarousel
import com.campusconnect.app.ui.components.GlassmorphicCard
import com.campusconnect.app.ui.components.ShimmerBox
import com.campusconnect.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventsScreen(
    onNavigateToDetail: (String) -> Unit = {},
    viewModel: EventsViewModel = viewModel(factory = com.campusconnect.app.di.AppViewModelProvider.Factory),
) {
    val state            by viewModel.state.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val pullState = rememberPullToRefreshState()

    Scaffold(
        containerColor = RichBlack,
        topBar = { EventsTopBar() },
    ) { padding ->
        PullToRefreshBox(
            state        = pullState,
            isRefreshing = (state as? EventsUiState.Success)?.isRefreshing == true,
            onRefresh    = { viewModel.loadEvents(refresh = true) },
            modifier     = Modifier.fillMaxSize().padding(padding),
            indicator    = {
                PullToRefreshDefaults.Indicator(state = pullState, color = ElectricPurple, containerColor = ElevatedSurface)
            },
        ) {
            when (val s = state) {
                is EventsUiState.Loading -> EventsLoadingSkeleton()

                is EventsUiState.Success -> {
                    LazyColumn(
                        modifier       = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 32.dp),
                    ) {
                        if (s.featured.isNotEmpty()) {
                            item {
                                Spacer(Modifier.height(8.dp))
                                SectionHeader("Featured Events", "⭐")
                                Spacer(Modifier.height(12.dp))
                                Featured3DCarousel(
                                    events       = s.featured,
                                    onEventClick = { onNavigateToDetail(it.id) },
                                    onRsvp       = { viewModel.toggleRsvp(it) },
                                )
                                Spacer(Modifier.height(24.dp))
                            }
                        }

                        item {
                            SectionHeader("Browse by Category", "🏷️")
                            Spacer(Modifier.height(12.dp))
                            CategoryFilterRow(
                                selected = selectedCategory,
                                onSelect = viewModel::selectCategory,
                            )
                            Spacer(Modifier.height(20.dp))
                        }

                        item {
                            SectionHeader("Upcoming Events", "📅")
                            Spacer(Modifier.height(12.dp))
                        }

                        if (s.upcoming.isEmpty()) {
                            item { EmptyEventsState() }
                        } else {
                            items(s.upcoming, key = { it.id }) { event ->
                                EventCard(
                                    event    = event,
                                    onClick  = { onNavigateToDetail(event.id) },
                                    onRsvp   = { viewModel.toggleRsvp(it) },
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                )
                            }
                        }
                    }
                }

                is EventsUiState.Error -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(s.message, color = TextSecondary)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EventsTopBar() {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Event, null, tint = NeonViolet, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Events", fontWeight = FontWeight.Bold, color = TextPrimary)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = RichBlack, titleContentColor = TextPrimary),
    )
}

@Composable
private fun SectionHeader(title: String, emoji: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(emoji, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.width(8.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun CategoryFilterRow(selected: EventCategory?, onSelect: (EventCategory) -> Unit) {
    LazyRow(
        contentPadding        = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(EventCategory.entries) { category ->
            val isSelected = selected == category
            FilterChip(
                selected = isSelected,
                onClick  = { onSelect(category) },
                label    = { Text(category.label, style = MaterialTheme.typography.labelMedium) },
                colors   = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = category.tint.copy(alpha = 0.25f),
                    selectedLabelColor     = category.tint,
                    containerColor         = ElevatedSurface,
                    labelColor             = TextSecondary,
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true, selected = isSelected,
                    borderColor = GlassBorder, selectedBorderColor = category.tint,
                ),
            )
        }
    }
}

@Composable
private fun EmptyEventsState() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Outlined.EventBusy, null, tint = TextTertiary, modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(12.dp))
        Text("No events in this category yet", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
    }
}

@Composable
private fun EventsLoadingSkeleton() {
    LazyColumn(contentPadding = PaddingValues(16.dp)) {
        item {
            ShimmerBox(Modifier.fillMaxWidth().height(280.dp), RoundedCornerShape(20.dp))
            Spacer(Modifier.height(20.dp))
        }
        items(4) {
            GlassmorphicCard(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                Row(Modifier.padding(12.dp)) {
                    ShimmerBox(Modifier.size(64.dp), RoundedCornerShape(10.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        ShimmerBox(Modifier.fillMaxWidth(0.7f).height(16.dp))
                        Spacer(Modifier.height(8.dp))
                        ShimmerBox(Modifier.fillMaxWidth(0.4f).height(12.dp))
                    }
                }
            }
        }
    }
}
