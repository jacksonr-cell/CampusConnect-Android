package com.campusconnect.app.ui.screens.messaging

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.campusconnect.app.data.model.Conversation
import com.campusconnect.app.data.model.ConversationType
import com.campusconnect.app.data.model.ConversationsUiState
import com.campusconnect.app.di.AppViewModelProvider
import com.campusconnect.app.ui.components.GlassmorphicCard
import com.campusconnect.app.ui.components.PostCardSkeleton
import com.campusconnect.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationsScreen(
    onOpenConversation: (String) -> Unit,
    viewModel: ConversationsViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val onlineUserIds by viewModel.onlineUserIds.collectAsStateWithLifecycle()
    val pullState = rememberPullToRefreshState()

    Scaffold(
        containerColor = RichBlack,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Chat, null, tint = NeonViolet, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Messages", fontWeight = FontWeight.Bold, color = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = RichBlack, titleContentColor = TextPrimary),
            )
        },
    ) { padding ->
        PullToRefreshBox(
            state        = pullState,
            isRefreshing = (state as? ConversationsUiState.Success)?.isRefreshing == true,
            onRefresh    = { viewModel.loadConversations(refresh = true) },
            modifier     = Modifier.fillMaxSize().padding(padding),
            indicator    = {
                PullToRefreshDefaults.Indicator(
                    isRefreshing = (state as? ConversationsUiState.Success)?.isRefreshing == true,
                    state = pullState,
                    color = ElectricPurple,
                    containerColor = ElevatedSurface,
                )
            },
        ) {
            when (val s = state) {
                is ConversationsUiState.Loading -> {
                    LazyColumn(contentPadding = PaddingValues(16.dp)) {
                        items(5) { PostCardSkeleton(modifier = Modifier.padding(vertical = 6.dp)) }
                    }
                }

                is ConversationsUiState.Success -> {
                    if (s.conversations.isEmpty()) {
                        EmptyConversationsState()
                    } else {
                        LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
                            items(s.conversations, key = { it.id }) { conversation ->
                                ConversationRow(
                                    conversation = conversation,
                                    isOtherOnline = conversation.otherParticipant?.id in onlineUserIds,
                                    onClick      = { onOpenConversation(conversation.id) },
                                )
                            }
                        }
                    }
                }

                is ConversationsUiState.Error -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(s.message, color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.height(12.dp))
                            TextButton(onClick = { viewModel.loadConversations() }) {
                                Text("Retry", color = ElectricPurple)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConversationRow(conversation: Conversation, isOtherOnline: Boolean, onClick: () -> Unit) {
    GlassmorphicCard(
        modifier  = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        elevation = if (conversation.hasUnread) 6.dp else 3.dp,
    ) {
        Row(
            modifier = Modifier
                .clickable(onClick = onClick)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ConversationAvatar(conversation, isOtherOnline)

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text       = conversation.displayName,
                        style      = MaterialTheme.typography.titleSmall,
                        color      = TextPrimary,
                        fontWeight = if (conversation.hasUnread) FontWeight.Bold else FontWeight.SemiBold,
                        maxLines   = 1,
                        overflow   = TextOverflow.Ellipsis,
                        modifier   = Modifier.weight(1f),
                    )
                    conversation.lastMessageAt?.let {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text  = formatRelativeTime(it),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (conversation.hasUnread) ElectricPurple else TextTertiary,
                        )
                    }
                }
                Spacer(Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text     = conversation.lastMessagePreview ?: "No messages yet",
                        style    = MaterialTheme.typography.bodySmall,
                        color    = if (conversation.hasUnread) TextPrimary else TextTertiary,
                        fontWeight = if (conversation.hasUnread) FontWeight.Medium else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (conversation.hasUnread) {
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(PurpleGradient)),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ConversationAvatar(conversation: Conversation, isOtherOnline: Boolean) {
    Box(
        modifier = Modifier.size(52.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(modifier = Modifier.fillMaxSize().clip(CircleShape)) {
            if (conversation.displayAvatarUrl != null) {
                AsyncImage(
                    model              = conversation.displayAvatarUrl,
                    contentDescription = null,
                    contentScale       = ContentScale.Crop,
                    modifier           = Modifier.fillMaxSize(),
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize().background(Brush.linearGradient(PurpleGradient)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (conversation.type == ConversationType.GROUP) {
                        Icon(Icons.Filled.Group, null, tint = TextPrimary, modifier = Modifier.size(22.dp))
                    } else {
                        Text(
                            text     = conversation.displayName.take(1).uppercase(),
                            color    = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }

        if (conversation.type == ConversationType.DIRECT && isOtherOnline) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(OnlineGreen)
                    .border(2.dp, DarkCharcoal, CircleShape),
            )
        }
    }
}

@Composable
private fun EmptyConversationsState() {
    Column(
        modifier = Modifier.fillMaxSize().padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Outlined.ChatBubbleOutline, null, tint = TextTertiary, modifier = Modifier.size(56.dp))
        Spacer(Modifier.height(16.dp))
        Text("No conversations yet", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
        Spacer(Modifier.height(8.dp))
        Text(
            "Messages from other students will show up here",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
        )
    }
}

private fun formatRelativeTime(millis: Long): String {
    val diff = System.currentTimeMillis() - millis
    return when {
        diff < 60_000L      -> "now"
        diff < 3_600_000L    -> "${diff / 60_000}m"
        diff < 86_400_000L   -> "${diff / 3_600_000}h"
        diff < 604_800_000L  -> "${diff / 86_400_000}d"
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(millis))
    }
}
