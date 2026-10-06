package com.campusconnect.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.campusconnect.app.data.model.CampusEvent
import com.campusconnect.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.absoluteValue
import kotlin.math.min

/**
 * 3D coverflow-style carousel for featured campus events.
 *
 * Cards rotate around the Y-axis and scale/fade based on distance from the
 * centered page, creating the "layered depth" and "3D-inspired interface"
 * effect called for in the CampusConnect animation spec. Built entirely
 * from graphicsLayer transforms driven by HorizontalPager's scroll offset —
 * no extra dependency needed, and it runs on the compositor thread for
 * smooth 60–120fps scrolling.
 */
@Composable
fun FeaturedEventsCarousel(
    events    : List<CampusEvent>,
    onCardClick: (CampusEvent) -> Unit,
    modifier  : Modifier = Modifier,
) {
    if (events.isEmpty()) return

    val pagerState = rememberPagerState(initialPage = 0) { events.size }

    Column(modifier = modifier) {
        HorizontalPager(
            state           = pagerState,
            contentPadding  = PaddingValues(horizontal = 48.dp),
            pageSpacing     = 12.dp,
            modifier        = Modifier
                .fillMaxWidth()
                .height(260.dp),
        ) { page ->
            Featured3DCard(
                event      = events[page],
                pagerState = pagerState,
                page       = page,
                onClick    = { onCardClick(events[page]) },
            )
        }

        Spacer(Modifier.height(14.dp))

        // Page indicator dots — glow on active
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
        ) {
            events.indices.forEach { index ->
                val distance = (pagerState.currentPage - index).absoluteValue
                val isActive = distance == 0
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .size(if (isActive) 8.dp else 6.dp)
                        .clip(CircleShape)
                        .background(
                            if (isActive) Brush.linearGradient(PurpleGradient)
                            else Brush.linearGradient(listOf(Color(0xFF3D3D3D), Color(0xFF3D3D3D)))
                        )
                )
            }
        }
    }
}

@Composable
private fun Featured3DCard(
    event     : CampusEvent,
    pagerState: PagerState,
    page      : Int,
    onClick   : () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .graphicsLayer {
                // Signed offset: -1 = fully left neighbour, 0 = centered, +1 = right neighbour
                val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction)
                val absOffset  = pageOffset.absoluteValue.coerceIn(0f, 1f)

                // Y-axis rotation — cards "turn away" from the viewer as they leave centre
                rotationY = pageOffset * -35f

                // Depth scale — centre card is largest, side cards shrink and recede
                val scale = lerp(1f, 0.82f, absOffset)
                scaleX = scale
                scaleY = scale

                // Fade + darken off-centre cards for depth perception
                alpha = lerp(1f, 0.55f, absOffset)

                // Slight vertical lift for the centre card
                translationY = lerp(0f, 18f, absOffset)

                // Perspective pivot — rotate around the near edge, coverflow-style
                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(
                    pivotFractionX = if (pageOffset > 0) 0f else 1f,
                    pivotFractionY = 0.5f,
                )
                cameraDistance = 16f * density

                compositingStrategy = CompositingStrategy.Offscreen
            }
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick),
    ) {
        // ── Background image ────────────────────────────────────────────
        if (event.coverImage != null) {
            AsyncImage(
                model              = event.coverImage,
                contentDescription = null,
                contentScale       = ContentScale.Crop,
                modifier           = Modifier.fillMaxSize(),
            )
        } else {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Brush.linearGradient(listOf(event.category.tint.copy(alpha = 0.5f), RichBlack)))
            )
        }

        // ── Gradient scrim for text legibility ───────────────────────────
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0xCC0A0A0A),
                            Color(0xF20A0A0A),
                        ),
                        startY = 0f,
                    )
                )
        )

        // ── Purple glow border for the "premium" feel ────────────────────
        Box(
            Modifier
                .fillMaxSize()
                .border(1.dp, Brush.linearGradient(listOf(Color.White.copy(alpha = 0.25f), event.category.tint.copy(alpha = 0.4f))), RoundedCornerShape(24.dp))
        )

        // ── Featured ribbon ───────────────────────────────────────────────
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(14.dp)
                .clip(ChipShape)
                .background(Brush.horizontalGradient(PurpleGradient))
                .padding(horizontal = 10.dp, vertical = 5.dp),
        ) {
            Text("⭐ FEATURED", style = MaterialTheme.typography.labelSmall, color = TextPrimary, fontWeight = FontWeight.Bold)
        }

        // ── Category chip ─────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(14.dp)
                .clip(ChipShape)
                .background(event.category.tint.copy(alpha = 0.85f))
                .padding(horizontal = 10.dp, vertical = 5.dp),
        ) {
            Text(event.category.label, style = MaterialTheme.typography.labelSmall, color = TextPrimary, fontWeight = FontWeight.SemiBold)
        }

        // ── Event info ─────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp),
        ) {
            Text(
                text      = event.title,
                style     = MaterialTheme.typography.titleLarge,
                fontWeight= FontWeight.Bold,
                color     = TextPrimary,
                maxLines  = 2,
                overflow  = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.CalendarToday, null, tint = NeonViolet, modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(5.dp))
                Text(formatEventDate(event.startTime), style = MaterialTheme.typography.labelMedium, color = TextSecondary)
            }
            if (event.location != null) {
                Spacer(Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.LocationOn, null, tint = NeonViolet, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(5.dp))
                    Text(
                        event.location, style = MaterialTheme.typography.labelMedium, color = TextSecondary,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "${event.attendeesCount} going" + if (event.isTicketed) " · Ticketed" else " · Free",
                style = MaterialTheme.typography.labelSmall, color = NeonViolet, fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

private fun lerp(start: Float, stop: Float, fraction: Float): Float = start + (stop - start) * fraction

private fun formatEventDate(timestampMillis: Long): String {
    val fmt = SimpleDateFormat("EEE, MMM d · h:mm a", Locale.getDefault())
    return fmt.format(Date(timestampMillis))
}
