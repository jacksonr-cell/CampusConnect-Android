package com.campusconnect.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.campusconnect.app.data.model.Property
import com.campusconnect.app.ui.theme.*
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

/**
 * Kabarak Nest property card — glassmorphic surface, swipeable image
 * carousel, price/rating overlay, amenity chips, and a spring-animated
 * save (heart) button matching the CampusConnect interaction spec.
 */
@Composable
fun PropertyCard(
    property   : Property,
    onClick    : () -> Unit,
    onSaveToggle: (Property) -> Unit,
    modifier   : Modifier = Modifier,
    compact    : Boolean = false,
) {
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue   = if (pressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "cardPressScale",
    )

    GlassmorphicCard(
        modifier = modifier
            .scale(scale)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
    ) {
        Column {
            // ── Image carousel ────────────────────────────────────────────
            PropertyImageCarousel(
                images       = property.images,
                heightDp     = if (compact) 140 else 190,
                isFeatured   = property.isFeatured,
                isVerified   = property.isVerified,
                isSaved      = property.isSaved,
                onSaveToggle = { onSaveToggle(property) },
            )

            Column(modifier = Modifier.padding(14.dp)) {
                // ── Price + rating row ────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text       = "${property.currency} ${formatPrice(property.pricePerMonth)}",
                                style      = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color      = NeonViolet,
                            )
                            Text(
                                text  = "/mo",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextTertiary,
                                modifier = Modifier.padding(start = 2.dp, bottom = 2.dp),
                            )
                        }
                    }
                    if (property.ratingAverage > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.Star,
                                contentDescription = null,
                                tint = WarningAmber,
                                modifier = Modifier.size(14.dp),
                            )
                            Spacer(Modifier.width(2.dp))
                            Text(
                                text  = String.format("%.1f", property.ratingAverage),
                                style = MaterialTheme.typography.labelMedium,
                                color = TextPrimary,
                            )
                            Text(
                                text  = " (${property.reviewsCount})",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextTertiary,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(6.dp))

                // ── Title ──────────────────────────────────────────────────
                Text(
                    text      = property.title,
                    style     = MaterialTheme.typography.titleSmall,
                    color     = TextPrimary,
                    maxLines  = 1,
                    overflow  = TextOverflow.Ellipsis,
                )

                Spacer(Modifier.height(4.dp))

                // ── Location ───────────────────────────────────────────────
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.LocationOn,
                        contentDescription = null,
                        tint = TextTertiary,
                        modifier = Modifier.size(13.dp),
                    )
                    Spacer(Modifier.width(3.dp))
                    Text(
                        text      = buildString {
                            append(property.city)
                            property.neighborhood?.let { append(", $it") }
                            property.distanceFromCampusKm?.let { append(" · ${it}km from campus") }
                        },
                        style     = MaterialTheme.typography.bodySmall,
                        color     = TextTertiary,
                        maxLines  = 1,
                        overflow  = TextOverflow.Ellipsis,
                    )
                }

                if (!compact) {
                    Spacer(Modifier.height(10.dp))

                    // ── Bed/bath/type chips ────────────────────────────────
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        InfoChip(icon = Icons.Outlined.Bed, label = "${property.bedrooms} bed")
                        InfoChip(icon = Icons.Outlined.Bathtub, label = "${property.bathrooms} bath")
                        InfoChip(icon = Icons.Outlined.Home, label = property.propertyType.label)
                    }
                }
            }
        }
    }
}

@Composable
private fun PropertyImageCarousel(
    images      : List<String>,
    heightDp    : Int,
    isFeatured  : Boolean,
    isVerified  : Boolean,
    isSaved     : Boolean,
    onSaveToggle: () -> Unit,
) {
    val pagerState = rememberPagerState { images.size.coerceAtLeast(1) }

    Box(modifier = Modifier.fillMaxWidth().height(heightDp.dp)) {
        if (images.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().background(SurfaceDark),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.Home, null, tint = TextTertiary, modifier = Modifier.size(40.dp))
            }
        } else {
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                AsyncImage(
                    model              = images[page],
                    contentDescription = null,
                    contentScale       = ContentScale.Crop,
                    modifier           = Modifier.fillMaxSize(),
                )
            }
        }

        // Gradient scrim for badge/button legibility
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .align(Alignment.TopCenter)
                .background(Brush.verticalGradient(listOf(Color(0x99000000), Color.Transparent)))
        )

        // Featured / Verified badges
        Row(
            modifier = Modifier.align(Alignment.TopStart).padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (isFeatured) {
                Badge(text = "Featured", containerColor = Brush.horizontalGradient(PurpleGradient))
            }
            if (isVerified) {
                Badge(text = "✓ Verified", containerColor = Brush.horizontalGradient(listOf(SuccessGreen, SuccessGreen)))
            }
        }

        // Save (heart) button
        SaveButton(
            isSaved  = isSaved,
            onToggle = onSaveToggle,
            modifier = Modifier.align(Alignment.TopEnd).padding(10.dp),
        )

        // Page indicator dots
        if (images.size > 1) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                images.indices.forEach { index ->
                    val isActive = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .size(if (isActive) 6.dp else 5.dp)
                            .clip(CircleShape)
                            .background(if (isActive) Color.White else Color.White.copy(alpha = 0.4f))
                    )
                }
            }
        }
    }
}

@Composable
private fun Badge(text: String, containerColor: Brush) {
    Box(
        modifier = Modifier
            .clip(ChipShape)
            .background(containerColor)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelSmall, color = TextPrimary, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SaveButton(isSaved: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    val scale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(Color(0x66000000))
            .clickable {
                scope.launch {
                    scale.animateTo(1.4f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh))
                    scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
                }
                onToggle()
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector        = if (isSaved) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
            contentDescription = if (isSaved) "Unsave" else "Save",
            tint               = if (isSaved) LikeRed else Color.White,
            modifier           = Modifier.size(18.dp).scale(scale.value),
        )
    }
}

@Composable
private fun InfoChip(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    Row(
        modifier = Modifier
            .clip(ChipShape)
            .background(GlassWhite05)
            .border(0.5.dp, GlassBorder, ChipShape)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(12.dp))
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
    }
}

// ── Small helpers ─────────────────────────────────────────────────────────

private fun formatPrice(price: Double): String {
    return if (price >= 1000) {
        String.format("%,.0f", price)
    } else {
        price.toInt().toString()
    }
}
