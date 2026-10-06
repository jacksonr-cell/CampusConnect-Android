package com.campusconnect.app.ui.screens.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.campusconnect.app.CampusConnectApp
import com.campusconnect.app.data.model.Post
import com.campusconnect.app.data.model.PostComment
import com.campusconnect.app.data.repository.ApiResult
import com.campusconnect.app.data.repository.PostsRepository
import com.campusconnect.app.ui.theme.ElectricPurple
import com.campusconnect.app.ui.theme.RichBlack
import com.campusconnect.app.ui.theme.SurfaceDark
import com.campusconnect.app.ui.theme.TextPrimary
import com.campusconnect.app.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(
    postId: String,
    onBack: () -> Unit,
) {
    val app = LocalContext.current.applicationContext as CampusConnectApp
    val postsRepository: PostsRepository = app.container.postsRepository
    val scope = rememberCoroutineScope()
    var post by remember(postId) { mutableStateOf<Post?>(null) }
    var comments by remember(postId) { mutableStateOf<List<PostComment>>(emptyList()) }
    var commentText by remember(postId) { mutableStateOf("") }
    var errorMessage by remember(postId) { mutableStateOf<String?>(null) }
    var isLoading by remember(postId) { mutableStateOf(true) }
    var isSubmitting by remember { mutableStateOf(false) }
    var reloadKey by remember(postId) { mutableStateOf(0) }

    LaunchedEffect(postId, reloadKey) {
        isLoading = true
        errorMessage = null
        when (val result = postsRepository.getPost(postId)) {
            is ApiResult.Success -> post = result.data
            is ApiResult.Error -> errorMessage = result.message
        }
        when (val result = postsRepository.getComments(postId)) {
            is ApiResult.Success -> comments = result.data
            is ApiResult.Error -> if (errorMessage == null) errorMessage = result.message
        }
        isLoading = false
    }

    Scaffold(
        containerColor = RichBlack,
        topBar = {
            TopAppBar(
                title = { Text("Post", color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = RichBlack),
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(RichBlack)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = commentText,
                    onValueChange = { if (it.length <= 2000) commentText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Write a comment", color = TextSecondary) },
                    enabled = !isSubmitting,
                    maxLines = 4,
                )
                Spacer(Modifier.width(8.dp))
                IconButton(
                    enabled = commentText.isNotBlank() && !isSubmitting,
                    onClick = {
                        val content = commentText.trim()
                        scope.launch {
                            isSubmitting = true
                            when (val result = postsRepository.addComment(postId, content)) {
                                is ApiResult.Success -> {
                                    comments = comments + result.data
                                    post = post?.copy(commentsCount = (post?.commentsCount ?: 0) + 1)
                                    commentText = ""
                                    errorMessage = null
                                }
                                is ApiResult.Error -> errorMessage = result.message
                            }
                            isSubmitting = false
                        }
                    },
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = ElectricPurple,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Icon(Icons.Default.Send, "Send comment", tint = ElectricPurple)
                    }
                }
            }
        },
    ) { padding ->
        when {
            isLoading && post == null -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = ElectricPurple)
            }
            post == null -> Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(errorMessage ?: "Post unavailable", color = TextSecondary)
                TextButton(onClick = { reloadKey++ }) {
                    Text("Retry", color = ElectricPurple)
                }
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
            ) {
                item {
                    PostDetailContent(post = post!!)
                }
                item {
                    Text(
                        "Comments (${post!!.commentsCount})",
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                        color = TextPrimary,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                if (errorMessage != null) {
                    item {
                        Text(
                            errorMessage.orEmpty(),
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
                if (comments.isEmpty() && !isLoading) {
                    item {
                        Text(
                            "No comments yet",
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                            color = TextSecondary,
                        )
                    }
                }
                items(comments, key = { it.id }) { comment ->
                    CommentRow(comment)
                }
            }
        }
    }
}

@Composable
private fun PostDetailContent(post: Post) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
        Text(
            post.author.displayName.ifBlank { post.author.username },
            color = TextPrimary,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text("@${post.author.username}", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
        if (!post.content.isNullOrBlank()) {
            Text(
                post.content,
                modifier = Modifier.padding(top = 14.dp),
                color = TextPrimary,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@Composable
private fun CommentRow(comment: PostComment) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Surface(
            modifier = Modifier.size(36.dp),
            shape = CircleShape,
            color = SurfaceDark,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    comment.author.initials,
                    color = ElectricPurple,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        Column(modifier = Modifier.padding(start = 10.dp).weight(1f)) {
            Text(
                comment.author.displayName.ifBlank { comment.author.username },
                color = TextPrimary,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(comment.content, color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
        }
    }
}