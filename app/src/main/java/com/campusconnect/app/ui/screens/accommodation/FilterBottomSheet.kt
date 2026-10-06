package com.campusconnect.app.ui.screens.accommodation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.campusconnect.app.data.model.AccommodationFilters
import com.campusconnect.app.data.model.PropertyType
import com.campusconnect.app.data.model.SortOption
import com.campusconnect.app.ui.components.PrimaryButton
import com.campusconnect.app.ui.components.SecondaryButton
import com.campusconnect.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterBottomSheet(
    currentFilters: AccommodationFilters,
    onApply       : (AccommodationFilters) -> Unit,
    onDismiss     : () -> Unit,
) {
    var draft by remember { mutableStateOf(currentFilters) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        containerColor   = DarkCharcoal,
        dragHandle       = { BottomSheetDefaults.DragHandle(color = GlassBorder) },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
        ) {
            Text("Filter Properties", style = MaterialTheme.typography.titleLarge, color = TextPrimary, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(24.dp))

            // ── Property type ──────────────────────────────────────────────
            FilterSectionLabel("Property Type")
            Spacer(Modifier.height(10.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(PropertyType.entries) { type ->
                    val selected = draft.propertyType == type
                    FilterChip(
                        selected = selected,
                        onClick  = { draft = draft.copy(propertyType = if (selected) null else type) },
                        label    = { Text(type.label, style = MaterialTheme.typography.labelMedium) },
                        colors   = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = DeepRoyalPurple.copy(alpha = 0.3f),
                            selectedLabelColor     = ElectricPurple,
                            containerColor         = ElevatedSurface,
                            labelColor             = TextSecondary,
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true, selected = selected,
                            borderColor = GlassBorder, selectedBorderColor = ElectricPurple,
                        ),
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── Price range ────────────────────────────────────────────────
            FilterSectionLabel("Price Range (KES/month)")
            Spacer(Modifier.height(10.dp))
            val priceRanges = listOf(
                "Under 6K"   to (null to 6000),
                "6K - 10K"   to (6000 to 10000),
                "10K - 15K"  to (10000 to 15000),
                "15K+"       to (15000 to null),
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(priceRanges) { (label, range) ->
                    val selected = draft.minPrice == range.first && draft.maxPrice == range.second
                    FilterChip(
                        selected = selected,
                        onClick  = {
                            draft = if (selected) draft.copy(minPrice = null, maxPrice = null)
                                    else draft.copy(minPrice = range.first, maxPrice = range.second)
                        },
                        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = DeepRoyalPurple.copy(alpha = 0.3f),
                            selectedLabelColor     = ElectricPurple,
                            containerColor         = ElevatedSurface,
                            labelColor             = TextSecondary,
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true, selected = selected,
                            borderColor = GlassBorder, selectedBorderColor = ElectricPurple,
                        ),
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── Sort by ────────────────────────────────────────────────────
            FilterSectionLabel("Sort By")
            Spacer(Modifier.height(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SortOption.entries.forEach { option ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = draft.sortBy == option,
                            onClick  = { draft = draft.copy(sortBy = option) },
                            colors   = RadioButtonDefaults.colors(selectedColor = ElectricPurple, unselectedColor = TextTertiary),
                        )
                        Text(option.label, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                    }
                }
            }

            Spacer(Modifier.height(28.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SecondaryButton(
                    text     = "Reset",
                    onClick  = { draft = AccommodationFilters() },
                    modifier = Modifier.weight(1f),
                )
                Box(modifier = Modifier.weight(1f)) {
                    PrimaryButton(text = "Apply Filters", onClick = { onApply(draft) })
                }
            }
        }
    }
}

@Composable
private fun FilterSectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, color = TextPrimary, fontWeight = FontWeight.SemiBold)
}
