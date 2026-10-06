package com.campusconnect.app.ui.screens.accommodation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.campusconnect.app.data.model.*
import com.campusconnect.app.ui.components.*
import com.campusconnect.app.ui.theme.*

@Composable
fun AccommodationScreen(
    widthSizeClass    : WindowWidthSizeClass,
    onNavigateToDetail: (String) -> Unit = {},
    viewModel: AccommodationViewModel = viewModel(factory = com.campusconnect.app.di.AppViewModelProvider.Factory),
) {
    val state       by viewModel.state.collectAsStateWithLifecycle()
    val filters     by viewModel.filters.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    var showFilterSheet by remember { mutableStateOf(false) }
    val columns = columnsForWidth(widthSizeClass, dense = false)

    Scaffold(
        containerColor = RichBlack,
        topBar = { NestTopBar() },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {

            // ── Search + filter row ───────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                SearchField(
                    value        = searchQuery,
                    onValueChange= viewModel::onSearchQueryChange,
                    modifier     = Modifier.weight(1f),
                )
                FilterButton(
                    hasActiveFilters = filters != AccommodationFilters(),
                    onClick          = { showFilterSheet = true },
                )
            }

            // ── Active filter chips ───────────────────────────────────────
            if (filters != AccommodationFilters()) {
                ActiveFilterChips(
                    filters      = filters,
                    onClearAll   = viewModel::clearFilters,
                    modifier     = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }

            // ── Content ────────────────────────────────────────────────────
            when (val s = state) {
                is AccommodationState.Loading -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(columns),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement   = Arrangement.spacedBy(12.dp),
                    ) {
                        items(6) { PropertyCardSkeleton() }
                    }
                }

                is AccommodationState.Success -> {
                    if (s.properties.isEmpty()) {
                        EmptyState(onClearFilters = viewModel::clearFilters)
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(columns),
                            contentPadding = PaddingValues(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement   = Arrangement.spacedBy(12.dp),
                        ) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Text(
                                    text  = "${s.properties.size} properties found",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextTertiary,
                                    modifier = Modifier.padding(bottom = 4.dp),
                                )
                            }
                            items(s.properties, key = { it.id }) { property ->
                                PropertyCard(
                                    property     = property,
                                    onClick      = { onNavigateToDetail(property.id) },
                                    onSaveToggle = viewModel::toggleSave,
                                )
                            }
                        }
                    }
                }

                is AccommodationState.Error -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(s.message, color = TextSecondary)
                    }
                }
            }
        }
    }

    if (showFilterSheet) {
        FilterBottomSheet(
            currentFilters = filters,
            onApply        = { newFilters ->
                viewModel.updateFilters { newFilters }
                showFilterSheet = false
            },
            onDismiss = { showFilterSheet = false },
        )
    }
}

// ── Top bar ──────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NestTopBar() {
    TopAppBar(
        title = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.House, null, tint = NeonViolet, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Kabarak Nest", fontWeight = FontWeight.Bold, color = TextPrimary)
                }
                Text("Find your perfect student home", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
            }
        },
        actions = {
            IconButton(onClick = { /* saved properties */ }) {
                Icon(Icons.Outlined.FavoriteBorder, "Saved", tint = TextSecondary)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = RichBlack, titleContentColor = TextPrimary),
    )
}

// ── Search field ───────────────────────────────────────────────────────────

@Composable
private fun SearchField(value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
        Row(
            modifier = modifier
                .clip(ChipShape)
                .background(ElevatedSurface)
                .border(0.5.dp, GlassBorder, ChipShape)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
        Icon(Icons.Outlined.Search, null, tint = TextTertiary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        androidx.compose.foundation.text.BasicTextField(
            value           = value,
            onValueChange   = onValueChange,
            modifier        = Modifier.weight(1f),
            singleLine      = true,
            textStyle       = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary),
            cursorBrush     = Brush.linearGradient(listOf(ElectricPurple, ElectricPurple)),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            decorationBox   = { inner ->
                if (value.isEmpty()) {
                    Text("Search by area, campus...", style = MaterialTheme.typography.bodyMedium, color = TextTertiary)
                }
                inner()
            },
        )
    }
}

// ── Filter button ──────────────────────────────────────────────────────────

@Composable
private fun FilterButton(hasActiveFilters: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(ChipShape)
            .background(if (hasActiveFilters) Brush.linearGradient(PurpleGradient) else Brush.linearGradient(listOf(ElevatedSurface, ElevatedSurface)))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Filled.Tune, "Filters", tint = TextPrimary, modifier = Modifier.size(20.dp))
    }
}

// ── Active filter chips ────────────────────────────────────────────────────

@Composable
private fun ActiveFilterChips(filters: AccommodationFilters, onClearAll: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        filters.propertyType?.let {
            AssistChip(onClick = {}, label = { Text(it.label, style = MaterialTheme.typography.labelSmall) },
                colors = AssistChipDefaults.assistChipColors(containerColor = DeepRoyalPurple.copy(alpha = 0.2f), labelColor = ElectricPurple))
        }
        if (filters.minPrice != null || filters.maxPrice != null) {
            AssistChip(onClick = {}, label = { Text("Price range", style = MaterialTheme.typography.labelSmall) },
                colors = AssistChipDefaults.assistChipColors(containerColor = DeepRoyalPurple.copy(alpha = 0.2f), labelColor = ElectricPurple))
        }
        TextButton(onClick = onClearAll) {
            Text("Clear all", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
        }
    }
}

// ── Empty state ────────────────────────────────────────────────────────────

@Composable
private fun EmptyState(onClearFilters: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Outlined.SearchOff, null, tint = TextTertiary, modifier = Modifier.size(56.dp))
        Spacer(Modifier.height(16.dp))
        Text("No properties found", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
        Spacer(Modifier.height(8.dp))
        Text("Try adjusting your filters or search", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Spacer(Modifier.height(20.dp))
        TextButton(onClick = onClearFilters) {
            Text("Clear filters", color = ElectricPurple)
        }
    }
}

@Composable
private fun PropertyCardSkeleton() {
    GlassmorphicCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            ShimmerBox(Modifier.fillMaxWidth().height(140.dp), RoundedCornerShape(0.dp))
            Column(Modifier.padding(12.dp)) {
                ShimmerBox(Modifier.width(80.dp).height(16.dp))
                Spacer(Modifier.height(8.dp))
                ShimmerBox(Modifier.fillMaxWidth().height(14.dp))
                Spacer(Modifier.height(6.dp))
                ShimmerBox(Modifier.width(120.dp).height(12.dp))
            }
        }
    }
}
