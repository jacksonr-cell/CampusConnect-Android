package com.campusconnect.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.campusconnect.app.data.model.Story
import com.campusconnect.app.ui.theme.*

@Composable
fun StoryRow(
    stories: List<Story>,
    onAddStory: () -> Unit,
    onStoryClick: (Story) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier            = modifier,
        contentPadding      = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // "Add story" tile — always first
        item {
            AddStoryItem(onClick = onAddStory)
        }
        items(stories, key = { it.id }) { story ->
            StoryItem(story = story, onClick = { onStoryClick(story) })
        }
    }
}

@Composable
private fun AddStoryItem(onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(68.dp)
            .clickable(onClick = onClick),
    ) {
        Box(contentAlignment = Alignment.BottomEnd) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(50))
                    .background(SurfaceDark)
                    .border(2.dp, GlassBorder, RoundedCornerShape(50)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector        = Icons.Default.Add,
                    contentDescription = "Add story",
                    tint               = TextSecondary,
                    modifier           = Modifier.size(28.dp),
                )
            }
            // Purple "+" badge
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(RoundedCornerShape(50))
                    .background(DeepRoyalPurple)
                    .border(1.5.dp, DarkCharcoal, RoundedCornerShape(50)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector        = Icons.Default.Add,
                    contentDescription = null,
                    tint               = TextPrimary,
                    modifier           = Modifier.size(12.dp),
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text      = "Your Story",
            style     = MaterialTheme.typography.labelSmall,
            color     = TextSecondary,
            maxLines  = 1,
            overflow  = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun StoryItem(story: Story, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(68.dp)
            .clickable(onClick = onClick),
    ) {
        // Gradient ring — purple when unseen, grey when seen
        val ringBrush = if (!story.isSeen) {
            Brush.sweepGradient(StoryGradient)
        } else {
            Brush.linearGradient(listOf(Color(0xFF444444), Color(0xFF444444)))
        }

        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(50))
                .border(2.5.dp, ringBrush, RoundedCornerShape(50)),
            contentAlignment = Alignment.Center,
        ) {
            // 2dp white gap between ring and avatar
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(RoundedCornerShape(50))
                    .border(2.dp, DarkCharcoal, RoundedCornerShape(50)),
                contentAlignment = Alignment.Center,
            ) {
                if (story.user.avatarUrl != null) {
                    AsyncImage(
                        model              = story.user.avatarUrl,
                        contentDescription = story.user.displayName,
                        contentScale       = ContentScale.Crop,
                        modifier           = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(50)),
                    )
                } else {
                    // Fallback initials avatar
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Brush.linearGradient(PurpleGradient)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text  = story.user.initials,
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary,
                            fontSize = 18.sp,
                        )
                    }
                }
            }

            // Online dot
            if (story.user.isOnline) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(14.dp)
                        .clip(RoundedCornerShape(50))
                        .background(OnlineGreen)
                        .border(2.dp, DarkCharcoal, RoundedCornerShape(50))
                )
            }
        }

        Spacer(Modifier.height(6.dp))
        Text(
            text      = story.user.firstName,
            style     = MaterialTheme.typography.labelSmall,
            color     = if (story.isSeen) TextTertiary else TextPrimary,
            maxLines  = 1,
            overflow  = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}
