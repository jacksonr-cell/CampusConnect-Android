package com.campusconnect.app.ui.screens.messaging

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.campusconnect.app.data.model.ChatMessage
import com.campusconnect.app.data.model.ChatUiState
import com.campusconnect.app.data.model.ConversationType
import com.campusconnect.app.di.AppViewModelProvider
import com.campusconnect.app.ui.theme.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatScreen(
    conversationId  : String,
    displayName     : String,
    displayAvatarUrl: String?,
    conversationType: ConversationType,
    onBack          : () -> Unit,
    viewModel: ChatViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    LaunchedEffect(conversationId) { viewModel.initialize(conversationId) }

    val state       by viewModel.state.collectAsStateWithLifecycle()
    val otherTyping by viewModel.otherUserTyping.collectAsStateWithLifecycle()
    val listState    = rememberLazyListState()
    var inputText by remember { mutableStateOf("") }
    var isTyping by remember { mutableStateOf(false) }

    // Auto-scroll to the newest message whenever the list grows.
    LaunchedEffect(state) {
        val count = (state as? ChatUiState.Success)?.messages?.size ?: 0
        if (count > 0) listState.animateScrollToItem(count - 1)
    }

    // Debounce typing-stop: send "stopped" 2s after the last keystroke.
    LaunchedEffect(inputText) {
        if (inputText.isNotEmpty() && !isTyping) {
            isTyping = true
            viewModel.onTypingChanged(true)
        }
        delay(2000)
        if (isTyping) {
            isTyping = false
            viewModel.onTypingChanged(false)
        }
    }

    Scaffold(
        containerColor = RichBlack,
        topBar = {
            ChatTopBar(
                displayName      = displayName,
                displayAvatarUrl = displayAvatarUrl,
                conversationType = conversationType,
                isOtherTyping    = otherTyping,
                onBack           = onBack,
            )
        },
        bottomBar = {
            ChatInputBar(
                text        = inputText,
                onTextChange = { inputText = it },
                onSend      = {
                    if (inputText.isNotBlank()) {
                        viewModel.sendMessage(inputText.trim())
                        inputText = ""
                        isTyping = false
                        viewModel.onTypingChanged(false)
                    }
                },
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val s = state) {
                is ChatUiState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = ElectricPurple)
                    }
                }

                is ChatUiState.Success -> {
                    LazyColumn(
                        state          = listState,
                        modifier       = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(s.messages, key = { it.id }) { message ->
                            MessageBubbleRow(
                                message = message,
                                onRetry = { viewModel.retrySend(message) },
                            )
                        }
                        if (otherTyping) {
                            item(key = "typing-indicator") {
                                TypingIndicatorBubble()
                            }
                        }
                    }
                }

                is ChatUiState.Error -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(s.message, color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.height(12.dp))
                            TextButton(onClick = { viewModel.loadMessages(conversationId) }) {
                                Text("Retry", color = ElectricPurple)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Top bar ──────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatTopBar(
    displayName     : String,
    displayAvatarUrl: String?,
    conversationType: ConversationType,
    isOtherTyping   : Boolean,
    onBack          : () -> Unit,
) {
    TopAppBar(
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TextPrimary)
            }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(38.dp).clip(CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    if (displayAvatarUrl != null) {
                        AsyncImage(
                            model = displayAvatarUrl, contentDescription = null,
                            contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize().background(Brush.linearGradient(PurpleGradient)),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (conversationType == ConversationType.GROUP) {
                                Icon(Icons.Filled.Group, null, tint = TextPrimary, modifier = Modifier.size(16.dp))
                            } else {
                                Text(displayName.take(1).uppercase(), color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(displayName, style = MaterialTheme.typography.titleSmall, color = TextPrimary, fontWeight = FontWeight.Bold)
                    Text(
                        text  = if (isOtherTyping) "typing..." else " ",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isOtherTyping) ElectricPurple else TextTertiary,
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkCharcoal, titleContentColor = TextPrimary),
    )
}

// ── Message bubbles ────────────────────────────────────────────────────────

@Composable
private fun MessageBubbleRow(message: ChatMessage, onRetry: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isMine) Arrangement.End else Arrangement.Start,
    ) {
        Column(
            horizontalAlignment = if (message.isMine) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 280.dp),
        ) {
            Box(
                modifier = Modifier
                    .clip(bubbleShapeFor(message.isMine))
                    .background(
                        if (message.isMine) Brush.linearGradient(PurpleGradient)
                        else Brush.linearGradient(listOf(ElevatedSurface, ElevatedSurface)),
                    )
                    .let { if (!message.isMine) it.border(1.dp, GlassBorder, bubbleShapeFor(false)) else it }
                    .alpha(if (message.isSending) 0.6f else 1f)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                Text(
                    text  = message.content.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                )
            }
            Spacer(Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (message.failedToSend) {
                    Icon(
                        Icons.Filled.ErrorOutline, null,
                        tint = ErrorRed, modifier = Modifier.size(12.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "Failed to send · Retry",
                        style = MaterialTheme.typography.labelSmall,
                        color = ErrorRed,
                        modifier = Modifier.clickable(onClick = onRetry),
                    )
                } else {
                    Text(
                        text  = if (message.isSending) "Sending…" else formatTime(message.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextTertiary,
                    )
                }
            }
        }
    }
}

private fun bubbleShapeFor(isMine: Boolean): RoundedCornerShape = if (isMine) {
    RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 5.dp)
} else {
    RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 5.dp, bottomEnd = 18.dp)
}

@Composable
private fun TypingIndicatorBubble() {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        Row(
            modifier = Modifier
                .clip(bubbleShapeFor(false))
                .background(ElevatedSurface)
                .border(1.dp, GlassBorder, bubbleShapeFor(false))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            val transition = rememberInfiniteTransition(label = "typing")
            repeat(3) { index ->
                val delayMs = index * 150
                val alpha by transition.animateFloat(
                    initialValue = 0.3f, targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(600, delayMillis = delayMs),
                        repeatMode = RepeatMode.Reverse,
                    ),
                    label = "dot$index",
                )
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(TextSecondary.copy(alpha = alpha)),
                )
            }
        }
    }
}

// ── Input bar ──────────────────────────────────────────────────────────────

@Composable
private fun ChatInputBar(text: String, onTextChange: (String) -> Unit, onSend: () -> Unit) {
    Surface(color = DarkCharcoal) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(ElevatedSurface)
                    .border(1.dp, GlassBorder, RoundedCornerShape(24.dp))
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            ) {
                androidx.compose.foundation.text.BasicTextField(
                    value           = text,
                    onValueChange   = onTextChange,
                    modifier        = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    textStyle       = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary),
                    cursorBrush     = Brush.linearGradient(listOf(ElectricPurple, ElectricPurple)),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    decorationBox   = { inner ->
                        if (text.isEmpty()) {
                            Text("Message...", style = MaterialTheme.typography.bodyMedium, color = TextTertiary)
                        }
                        inner()
                    },
                )
            }
            Spacer(Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(PurpleGradient))
                    .clickable(onClick = onSend),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, "Send", tint = TextPrimary, modifier = Modifier.size(18.dp))
            }
        }
    }
}

private fun formatTime(millis: Long): String =
    SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(millis))
