package com.campusconnect.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.campusconnect.app.data.model.AttendeeStatus
import com.campusconnect.app.data.model.CampusEvent
import com.campusconnect.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Compact horizontal event card for the upcoming-events list — a date
 * "chip" on the left, thumbnail + details on the right.
 */
@Composable
fun EventCard(
    event      : CampusEvent,
    onClick    : () -> Unit,
    onRsvp     : (CampusEvent) -> Unit,
    modifier   : Modifier = Modifier,
) {
    GlassmorphicCard(
        modifier  = modifier.fillMaxWidth(),
        glowColor = event.category.tint.copy(alpha = 0.25f),
        elevation = 4.dp,
    ) {
        Row(
            modifier = Modifier
                .clickable(onClick = onClick)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DateBlock(event.startTime, event.category.tint)

            Spacer(Modifier.width(12.dp))

            // Thumbnail
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(10.dp)),
            ) {
                if (event.coverImage != null) {
                    AsyncImage(
                        model = event.coverImage, contentDescription = null,
                        contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Box(
                        Modifier.fillMaxSize().background(event.category.tint.copy(alpha = 0.25f)),
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    event.title, style = MaterialTheme.typography.titleSmall, color = TextPrimary,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(3.dp))
                if (event.location != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.LocationOn, null, tint = TextTertiary, modifier = Modifier.size(11.dp))
                        Spacer(Modifier.width(3.dp))
                        Text(
                            event.location, style = MaterialTheme.typography.labelSmall, color = TextTertiary,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GlassPill {
                        Text(event.category.label, style = MaterialTheme.typography.labelSmall, color = event.category.tint)
                    }
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "${event.attendeesCount} going",
                        style = MaterialTheme.typography.labelSmall, color = TextTertiary,
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            RsvpButton(status = event.myStatus, onClick = { onRsvp(event) })
        }
    }
}

@Composable
private fun DateBlock(startTime: Long, tint: androidx.compose.ui.graphics.Color) {
    val day   = SimpleDateFormat("d", Locale.getDefault()).format(Date(startTime))
    val month = SimpleDateFormat("MMM", Locale.getDefault()).format(Date(startTime)).uppercase()

    Column(
        modifier = Modifier
            .width(52.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(tint.copy(alpha = 0.15f))
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(month, style = MaterialTheme.typography.labelSmall, color = tint, fontWeight = FontWeight.Bold)
        Text(day, style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun RsvpButton(status: AttendeeStatus?, onClick: () -> Unit) {
    val isGoing = status == AttendeeStatus.GOING
    val backgroundBrush = if (isGoing) {
        Brush.linearGradient(listOf(SuccessGreen.copy(alpha = 0.2f), SuccessGreen.copy(alpha = 0.2f)))
    } else {
        Brush.linearGradient(PurpleGradient)
    }
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(backgroundBrush)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text  = if (isGoing) "✓" else "+",
            style = MaterialTheme.typography.titleMedium,
            color = if (isGoing) SuccessGreen else TextPrimary,
            fontWeight = FontWeight.Bold,
        )
    }
}
