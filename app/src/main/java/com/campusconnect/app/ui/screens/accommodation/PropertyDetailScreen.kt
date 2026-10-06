package com.campusconnect.app.ui.screens.accommodation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.campusconnect.app.data.model.*
import com.campusconnect.app.ui.components.GlassmorphicCard
import com.campusconnect.app.ui.components.PrimaryButton
import com.campusconnect.app.ui.theme.*

@Composable
fun PropertyDetailScreen(
    propertyId       : String,
    widthSizeClass    : WindowWidthSizeClass,
    onBack           : () -> Unit,
    onInquire        : (Property) -> Unit = {},
    viewModel: PropertyDetailViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = com.campusconnect.app.di.AppViewModelProvider.Factory,
    ),
) {
    LaunchedEffect(propertyId) {
        viewModel.loadProperty(propertyId)
    }

    val state by viewModel.state.collectAsStateWithLifecycle()

    val property = (state as? PropertyDetailState.Success)?.property

    if (state is PropertyDetailState.Error) {
        val message = (state as PropertyDetailState.Error).message
        Box(Modifier.fillMaxSize().background(RichBlack), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(message, color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(12.dp))
                TextButton(onClick = { viewModel.loadProperty(propertyId) }) {
                    Text("Retry", color = ElectricPurple)
                }
            }
        }
        return
    }

    if (property == null) {
        Box(Modifier.fillMaxSize().background(RichBlack), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = ElectricPurple)
        }
        return
    }

    val isWide = widthSizeClass != WindowWidthSizeClass.Compact

    Scaffold(
        containerColor = RichBlack,
        bottomBar = { InquiryBottomBar(property = property, onInquire = { onInquire(property) }) },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (isWide) {
                // ── Two-pane layout: gallery left, info right ──────────────
                Row(Modifier.fillMaxSize()) {
                    Box(Modifier.weight(1f).fillMaxHeight()) {
                        ImageGallery(images = property.images, onBack = onBack, heightFraction = 1f)
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                    ) {
                        PropertyInfoContent(property = property)
                    }
                }
            } else {
                // ── Single column — phone layout ────────────────────────────
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    ImageGallery(images = property.images, onBack = onBack, heightFraction = 0f, fixedHeight = 320)
                    PropertyInfoContent(property = property)
                }
            }
        }
    }
}

// ── Image gallery with back button overlay ─────────────────────────────────

@Composable
private fun ImageGallery(
    images        : List<String>,
    onBack        : () -> Unit,
    heightFraction: Float,
    fixedHeight   : Int = 0,
) {
    val pagerState = rememberPagerState { images.size.coerceAtLeast(1) }
    val modifier = if (heightFraction > 0f) Modifier.fillMaxWidth().fillMaxHeight() else Modifier.fillMaxWidth().height(fixedHeight.dp)

    Box(modifier = modifier) {
        if (images.isNotEmpty()) {
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                AsyncImage(
                    model = images[page], contentDescription = null,
                    contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize(),
                )
            }
        } else {
            Box(Modifier.fillMaxSize().background(SurfaceDark), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Home, null, tint = TextTertiary, modifier = Modifier.size(60.dp))
            }
        }

        // Scrim + back button
        Box(
            Modifier.fillMaxWidth().height(90.dp).align(Alignment.TopCenter)
                .background(Brush.verticalGradient(listOf(Color(0xAA000000), Color.Transparent)))
        )
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
                .clip(CircleShape)
                .background(Color(0x66000000)),
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
        }

        // Image counter
        if (images.size > 1) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp)
                    .clip(ChipShape)
                    .background(Color(0x99000000))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Text("${pagerState.currentPage + 1}/${images.size}", color = Color.White, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

// ── Info content — everything below/beside the gallery ─────────────────────

@Composable
private fun PropertyInfoContent(property: Property) {
    Column(Modifier.padding(20.dp)) {

        // ── Price + badges ─────────────────────────────────────────────────
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${property.currency} ${String.format("%,.0f", property.pricePerMonth)}",
                style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = NeonViolet,
            )
            Text(" /month", style = MaterialTheme.typography.bodyMedium, color = TextTertiary)
            Spacer(Modifier.weight(1f))
            if (property.isVerified) {
                Icon(Icons.Filled.Verified, null, tint = SuccessGreen, modifier = Modifier.size(22.dp))
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(property.title, style = MaterialTheme.typography.titleLarge, color = TextPrimary, fontWeight = FontWeight.Bold)

        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.LocationOn, null, tint = TextTertiary, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(4.dp))
            Text(
                "${property.address}, ${property.city}",
                style = MaterialTheme.typography.bodyMedium, color = TextSecondary,
            )
        }

        if (property.ratingAverage > 0) {
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Star, null, tint = WarningAmber, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    "${property.ratingAverage} · ${property.reviewsCount} reviews",
                    style = MaterialTheme.typography.bodyMedium, color = TextPrimary,
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        // ── Quick stats row ────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            StatItem(Icons.Outlined.Bed, "${property.bedrooms}", "Bedrooms")
            StatItem(Icons.Outlined.Bathtub, "${property.bathrooms}", "Bathrooms")
            StatItem(Icons.Outlined.People, "${property.maxOccupants}", "Max Occupants")
            StatItem(Icons.Outlined.Home, property.propertyType.label, "Type")
        }

        Spacer(Modifier.height(24.dp))
        HorizontalDivider(color = GlassBorder)
        Spacer(Modifier.height(20.dp))

        // ── Description ────────────────────────────────────────────────────
        Text("Description", style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Text(property.description, style = MaterialTheme.typography.bodyMedium, color = TextSecondary, lineHeight = 22.sp)

        Spacer(Modifier.height(24.dp))

        // ── Amenities ──────────────────────────────────────────────────────
        if (property.amenities.isNotEmpty()) {
            Text("Amenities", style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.height((60 * ((property.amenities.size + 1) / 2)).dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                userScrollEnabled = false,
            ) {
                items(property.amenities) { amenity -> AmenityItem(amenity) }
            }
            Spacer(Modifier.height(24.dp))
        }

        HorizontalDivider(color = GlassBorder)
        Spacer(Modifier.height(20.dp))

        // ── Landlord card ──────────────────────────────────────────────────
        Text("Listed by", style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(12.dp))
        LandlordCard(landlord = property.landlord)

        Spacer(Modifier.height(100.dp)) // room for sticky bottom bar
    }
}

@Composable
private fun StatItem(icon: androidx.compose.ui.graphics.vector.ImageVector, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.size(44.dp).clip(CircleShape).background(GlassWhite05),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = ElectricPurple, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(value, style = MaterialTheme.typography.titleSmall, color = TextPrimary, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextTertiary)
    }
}

@Composable
private fun AmenityItem(amenity: Amenity) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(GlassWhite05)
            .border(0.5.dp, GlassBorder, RoundedCornerShape(10.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.CheckCircle, null, tint = SuccessGreen, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(amenity.name, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
    }
}

@Composable
private fun LandlordCard(landlord: User) {
    GlassmorphicCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(14.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(48.dp).clip(CircleShape).background(Brush.linearGradient(PurpleGradient)),
                contentAlignment = Alignment.Center,
            ) {
                Text(landlord.initials, color = TextPrimary, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(landlord.displayName, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                    if (landlord.isVerified) {
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.Filled.Verified, null, tint = ElectricPurple, modifier = Modifier.size(14.dp))
                    }
                }
                Text("Property Manager", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
            }
            IconButton(onClick = { /* message landlord */ }) {
                Icon(Icons.Outlined.ChatBubbleOutline, "Message", tint = ElectricPurple)
            }
        }
    }
}

// ── Sticky bottom inquiry bar ────────────────────────────────────────────

@Composable
private fun InquiryBottomBar(property: Property, onInquire: () -> Unit) {
    Surface(color = DarkCharcoal, tonalElevation = 8.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "${property.currency} ${String.format("%,.0f", property.pricePerMonth)}/mo",
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary,
                )
                if (property.deposit != null) {
                    Text(
                        "Deposit: ${property.currency} ${String.format("%,.0f", property.deposit)}",
                        style = MaterialTheme.typography.labelSmall, color = TextTertiary,
                    )
                }
            }
            Box(modifier = Modifier.width(180.dp)) {
                PrimaryButton(text = "Send Inquiry", onClick = onInquire)
            }
        }
    }
}
