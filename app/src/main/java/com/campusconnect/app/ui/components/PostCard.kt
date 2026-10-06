package com.campusconnect.app.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.campusconnect.app.data.model.*
import com.campusconnect.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PostCard(
    post: Post,
    onReact: (Post, ReactionType) -> Unit,
    onComment: (Post) -> Unit,
    onShare: (Post) -> Unit,
    onBookmark: (Post) -> Unit,
    onAuthorClick: (User) -> Unit,
    onPostClick: (Post) -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassmorphicCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onPostClick(post) },
    ) {
        Column {
            // ── Header ────────────────────────────────────────────────────
            PostHeader(
                post          = post,
                onAuthorClick = onAuthorClick,
                modifier      = Modifier.padding(start = 16.dp, end = 8.dp, top = 16.dp),
            )

            // ── Community tag ─────────────────────────────────────────────
            if (post.communityName != null) {
                Spacer(Modifier.height(8.dp))
                GlassPill(modifier = Modifier.padding(start = 16.dp)) {
                    Text(
                        text  = "# ${post.communityName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = ElectricPurple,
                    )
                }
            }

            // ── Text content ──────────────────────────────────────────────
            if (!post.content.isNullOrBlank()) {
                Spacer(Modifier.height(12.dp))
                var expanded by remember { mutableStateOf(false) }
                val isLong = post.content.length > 280

                Text(
                    text      = post.content,
                    style     = MaterialTheme.typography.bodyMedium,
                    color     = TextPrimary,
                    maxLines  = if (expanded || !isLong) Int.MAX_VALUE else 4,
                    overflow  = TextOverflow.Ellipsis,
                    modifier  = Modifier.padding(horizontal = 16.dp),
                )
                if (isLong) {
                    TextButton(
                        onClick  = { expanded = !expanded },
                        modifier = Modifier.padding(start = 8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    ) {
                        Text(
                            if (expanded) "Show less" else "Read more",
                            color = ElectricPurple,
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
            }

            // ── Media grid ────────────────────────────────────────────────
            if (post.mediaUrls.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                MediaGrid(
                    urls     = post.mediaUrls,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }

            Spacer(Modifier.height(12.dp))

            // ── Reaction summary ──────────────────────────────────────────
            if (post.totalReactions > 0 || post.commentsCount > 0) {
                ReactionSummaryBar(post = post, modifier = Modifier.padding(horizontal = 16.dp))
                Divider(
                    color     = GlassBorder,
                    thickness = 0.5.dp,
                    modifier  = Modifier.padding(vertical = 8.dp),
                )
            } else {
                Spacer(Modifier.height(4.dp))
            }

            // ── Action bar ────────────────────────────────────────────────
            ActionBar(
                post       = post,
                onReact    = { onReact(post, it) },
                onComment  = { onComment(post) },
                onShare    = { onShare(post) },
                onBookmark = { onBookmark(post) },
                modifier   = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
}

// ── Post Header ───────────────────────────────────────────────────────────

@Composable
private fun PostHeader(
    post: Post,
    onAuthorClick: (User) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier             = modifier.fillMaxWidth(),
        verticalAlignment    = Alignment.CenterVertically,
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(50))
                .clickable { onAuthorClick(post.author) },
            contentAlignment = Alignment.Center,
        ) {
            if (post.author.avatarUrl != null) {
                AsyncImage(
                    model              = post.author.avatarUrl,
                    contentDescription = post.author.displayName,
                    contentScale       = ContentScale.Crop,
                    modifier           = Modifier.fillMaxSize(),
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.linearGradient(PurpleGradient)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text     = post.author.initials,
                        color    = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        Spacer(Modifier.width(12.dp))

        // Name + meta
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text      = post.author.displayName,
                    style     = MaterialTheme.typography.titleSmall,
                    color     = TextPrimary,
                    maxLines  = 1,
                    overflow  = TextOverflow.Ellipsis,
                )
                if (post.author.isVerified) {
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        imageVector        = Icons.Filled.Verified,
                        contentDescription = "Verified",
                        tint               = ElectricPurple,
                        modifier           = Modifier.size(14.dp),
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (post.author.university != null) {
                    Text(
                        text  = post.author.university,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextTertiary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Text("·", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                }
                Text(
                    text  = post.createdAt.toRelativeTime(),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary,
                )
            }
        }

        // Options menu
        IconButton(onClick = { /* TODO: show options */ }) {
            Icon(
                imageVector        = Icons.Default.MoreHoriz,
                contentDescription = "More options",
                tint               = TextSecondary,
            )
        }
    }
}

// ── Media Grid ────────────────────────────────────────────────────────────

@Composable
private fun MediaGrid(urls: List<String>, modifier: Modifier = Modifier) {
    when (urls.size) {
        1 -> AsyncImage(
            model              = urls[0],
            contentDescription = null,
            contentScale       = ContentScale.Crop,
            modifier           = modifier
                .fillMaxWidth()
                .height(280.dp)
                .clip(MediaGridShape),
        )
        2 -> Row(modifier = modifier.height(200.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            urls.forEach { url ->
                AsyncImage(
                    model              = url,
                    contentDescription = null,
                    contentScale       = ContentScale.Crop,
                    modifier           = Modifier.weight(1f).fillMaxHeight().clip(MediaGridShape),
                )
            }
        }
        else -> Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
            // First row — single large image
            AsyncImage(
                model              = urls[0],
                contentDescription = null,
                contentScale       = ContentScale.Crop,
                modifier           = Modifier.fillMaxWidth().height(200.dp).clip(MediaGridShape),
            )
            // Second row — remaining images (max 3)
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                urls.drop(1).take(3).forEachIndexed { index, url ->
                    Box(modifier = Modifier.weight(1f).height(100.dp)) {
                        AsyncImage(
                            model              = url,
                            contentDescription = null,
                            contentScale       = ContentScale.Crop,
                            modifier           = Modifier.fillMaxSize().clip(MediaGridShape),
                        )
                        // "+N more" overlay on last cell
                        if (index == 2 && urls.size > 4) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(MediaGridShape)
                                    .background(Color(0xAA000000)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text  = "+${urls.size - 4}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Reaction Summary Bar ──────────────────────────────────────────────────

@Composable
private fun ReactionSummaryBar(post: Post, modifier: Modifier = Modifier) {
    Row(
        modifier             = modifier.fillMaxWidth(),
        verticalAlignment    = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            // Top reaction emojis
            post.reactions.entries
                .sortedByDescending { it.value }
                .take(3)
                .forEach { (type, _) ->
                    Text(type.emoji, fontSize = 14.sp)
                }
            if (post.totalReactions > 0) {
                Text(
                    text  = formatCount(post.totalReactions),
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                )
            }
        }
        if (post.commentsCount > 0) {
            Text(
                text  = "${formatCount(post.commentsCount)} comments",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
            )
        }
    }
}

// ── Action Bar ────────────────────────────────────────────────────────────

@Composable
private fun ActionBar(
    post: Post,
    onReact: (ReactionType) -> Unit,
    onComment: () -> Unit,
    onShare: () -> Unit,
    onBookmark: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showReactions by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        // Reaction picker popup
        AnimatedVisibility(
            visible = showReactions,
            enter   = fadeIn() + expandVertically(),
            exit    = fadeOut() + shrinkVertically(),
        ) {
            ReactionPicker(
                onReactionSelected = { reaction ->
                    onReact(reaction)
                    showReactions = false
                },
                modifier = Modifier.padding(bottom = 8.dp, start = 8.dp),
            )
        }

        Row(
            modifier             = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            // Like / React button
            val isReacted = post.userReaction != null
            ActionButton(
                icon      = if (isReacted) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                label     = if (isReacted) (post.userReaction?.label ?: "Like") else "Like",
                tint      = if (isReacted) ElectricPurple else TextSecondary,
                onClick   = { showReactions = !showReactions },
                onLongClick = { showReactions = true },
            )

            // Comment
            ActionButton(
                icon    = Icons.Outlined.ChatBubbleOutline,
                label   = "Comment",
                tint    = TextSecondary,
                onClick = onComment,
            )

            // Share
            ActionButton(
                icon    = Icons.Outlined.Share,
                label   = "Share",
                tint    = TextSecondary,
                onClick = onShare,
            )

            // Bookmark
            ActionButton(
                icon    = if (post.isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                label   = "Save",
                tint    = if (post.isBookmarked) NeonViolet else TextSecondary,
                onClick = onBookmark,
            )
        }
    }
}

@Composable
private fun ActionButton(
    icon: ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue  = if (pressed) 0.88f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label        = "btnScale",
    )

    TextButton(
        onClick  = onClick,
        modifier = modifier.scale(scale),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
    ) {
        Icon(
            imageVector        = icon,
            contentDescription = label,
            tint               = tint,
            modifier           = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text  = label,
            style = MaterialTheme.typography.labelMedium,
            color = tint,
        )
    }
}

// ── Reaction Picker ───────────────────────────────────────────────────────

@Composable
private fun ReactionPicker(
    onReactionSelected: (ReactionType) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(ElevatedSurface)
            .border(1.dp, GlassBorder, RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        ReactionType.entries.forEach { reaction ->
            var hovered by remember { mutableStateOf(false) }
            val scale by animateFloatAsState(
                targetValue  = if (hovered) 1.4f else 1f,
                animationSpec = spring(stiffness = Spring.StiffnessMedium),
                label        = "reactionScale",
            )
            Text(
                text     = reaction.emoji,
                fontSize = 24.sp,
                modifier = Modifier
                    .scale(scale)
                    .clickable { onReactionSelected(reaction) },
            )
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────

private fun Long.toRelativeTime(): String {
    val diff = System.currentTimeMillis() - this
    return when {
        diff < 60_000L              -> "Just now"
        diff < 3_600_000L           -> "${diff / 60_000}m"
        diff < 86_400_000L          -> "${diff / 3_600_000}h"
        diff < 604_800_000L         -> "${diff / 86_400_000}d"
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(this))
    }
}

private fun formatCount(count: Int): String = when {
    count >= 1_000_000 -> "${count / 1_000_000}M"
    count >= 1_000     -> "${count / 1_000}K"
    else               -> count.toString()
}
