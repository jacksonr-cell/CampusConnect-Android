package com.campusconnect.app.ui.screens.feed

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.campusconnect.app.CampusConnectApp
import com.campusconnect.app.data.repository.ApiResult
import com.campusconnect.app.data.repository.PostsRepository
import com.campusconnect.app.ui.theme.ElectricPurple
import com.campusconnect.app.ui.theme.RichBlack
import com.campusconnect.app.ui.theme.TextPrimary
import com.campusconnect.app.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePostScreen(
    onBack: () -> Unit,
    onCreated: () -> Unit,
) {
    val app = LocalContext.current.applicationContext as CampusConnectApp
    val postsRepository: PostsRepository = app.container.postsRepository
    val scope = rememberCoroutineScope()
    var content by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = RichBlack,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Create post", color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(
                        enabled = content.isNotBlank() && !isSubmitting,
                        onClick = {
                            scope.launch {
                                isSubmitting = true
                                errorMessage = null
                                when (val result = postsRepository.createPost(content.trim())) {
                                    is ApiResult.Success -> onCreated()
                                    is ApiResult.Error -> {
                                        errorMessage = result.message
                                        isSubmitting = false
                                    }
                                }
                            }
                        },
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.padding(12.dp),
                                color = ElectricPurple,
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Icon(Icons.Default.Send, "Publish", tint = ElectricPurple)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = RichBlack),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = content,
                onValueChange = {
                    if (it.length <= 5000) content = it
                    errorMessage = null
                },
                modifier = Modifier.fillMaxWidth().weight(1f),
                placeholder = { Text("What's happening on campus?", color = TextSecondary) },
                supportingText = {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(errorMessage.orEmpty(), color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.weight(1f))
                        Text("${content.length}/5000", color = TextSecondary)
                    }
                },
                minLines = 8,
                maxLines = 16,
                enabled = !isSubmitting,
            )
        }
    }
}