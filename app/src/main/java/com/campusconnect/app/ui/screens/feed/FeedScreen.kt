package com.campusconnect.app.ui.screens.feed

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalContext
import com.campusconnect.app.CampusConnectApp
import com.campusconnect.app.data.model.*
import com.campusconnect.app.ui.components.*
import com.campusconnect.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    onNavigateToPost:    (String) -> Unit = {},
    onNavigateToProfile: (String) -> Unit = {},
    onNavigateToCreate:  () -> Unit       = {},
    refreshFeed: Boolean = false,
    onRefreshHandled: () -> Unit = {},
    viewModel: FeedViewModel = viewModel(factory = com.campusconnect.app.di.AppViewModelProvider.Factory),
) {
    val feedState   by viewModel.feedState.collectAsStateWithLifecycle()
    val isLoadingMore by viewModel.isLoadingMore.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val realtimeService = (context.applicationContext as CampusConnectApp).container.realtimeService
    val unreadNotificationCount by realtimeService.unreadNotificationCount.collectAsStateWithLifecycle()

    val pullState = rememberPullToRefreshState()
    val listState = rememberLazyListState()
    val topAppBarScrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    LaunchedEffect(refreshFeed) {
        if (refreshFeed) {
            viewModel.loadFeed(refresh = true)
            onRefreshHandled()
        }
    }

    // Load more when near end of list
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val total       = listState.layoutInfo.totalItemsCount
            lastVisible >= total - 3 && total > 0
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) viewModel.loadMorePosts()
    }

    // Pull-to-refresh trigger
    if (pullState.isAnimating) {
        LaunchedEffect(true) { viewModel.loadFeed(refresh = true) }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(topAppBarScrollBehavior.nestedScrollConnection),
        containerColor = RichBlack,
        topBar = {
            FeedTopAppBar(
                scrollBehavior          = topAppBarScrollBehavior,
                unreadNotificationCount = unreadNotificationCount,
                onNotificationsClick    = { realtimeService.markNotificationsSeen() },
            )
        },
        floatingActionButton = {
            CreatePostFab(onClick = onNavigateToCreate)
        },
    ) { paddingValues ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            // Animated purple glow at top — premium atmosphere
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                DeepRoyalPurple.copy(alpha = 0.12f),
                                RichBlack,
                            )
                        )
                    )
            )

            PullToRefreshBox(
                state      = pullState,
                isRefreshing = (feedState as? FeedState.Success)?.isRefreshing == true,
                onRefresh  = { viewModel.loadFeed(refresh = true) },
                modifier   = Modifier.fillMaxSize(),
                indicator  = {
                    PurplePullIndicator(
                        state = pullState,
                        isRefreshing = (feedState as? FeedState.Success)?.isRefreshing == true,
                    )
                },
            ) {
                LazyColumn(
                    state             = listState,
                    modifier          = Modifier.fillMaxSize(),
                    contentPadding    = PaddingValues(bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                ) {
                    // Feed content
                    when (val state = feedState) {
                        is FeedState.Loading -> {
                            items(4) {
                                PostCardSkeleton(
                                    modifier = Modifier
                                        .padding(horizontal = 16.dp, vertical = 6.dp)
                                )
                            }
                        }

                        is FeedState.Success -> {
                            items(state.posts, key = { it.id }) { post ->
                                PostCard(
                                    post         = post,
                                    onReact      = { p, r -> viewModel.reactToPost(p, r) },
                                    onComment    = { onNavigateToPost(it.id) },
                                    onShare      = { /* TODO */ },
                                    onBookmark   = { viewModel.bookmarkPost(it) },
                                    onAuthorClick = { onNavigateToProfile(it.id) },
                                    onPostClick  = { onNavigateToPost(it.id) },
                                    modifier     = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                )
                            }

                            // Load-more spinner
                            if (isLoadingMore) {
                                item(key = "loadMore") {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        CircularProgressIndicator(
                                            color  = ElectricPurple,
                                            modifier = Modifier.size(28.dp),
                                            strokeWidth = 2.dp,
                                        )
                                    }
                                }
                            }
                        }

                        is FeedState.Error -> {
                            item {
                                ErrorState(
                                    message = state.message,
                                    onRetry = { viewModel.loadFeed() },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Top App Bar ───────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeedTopAppBar(
    scrollBehavior: TopAppBarScrollBehavior,
    unreadNotificationCount: Int,
    onNotificationsClick: () -> Unit,
) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            brush = Brush.linearGradient(PurpleGradient),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("CC", color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 12.sp)
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    text       = "CampusConnect",
                    fontWeight = FontWeight.Bold,
                    fontSize   = 20.sp,
                    color      = TextPrimary,
                )
            }
        },
        actions = {
            IconButton(onClick = { /* search */ }) {
                Icon(Icons.Outlined.Search, "Search", tint = TextSecondary)
            }
            IconButton(onClick = onNotificationsClick) {
                if (unreadNotificationCount > 0) {
                    BadgedBox(badge = { Badge { Text(unreadNotificationCount.coerceAtMost(9).let { if (it == 9) "9+" else it.toString() }) } }) {
                        Icon(Icons.Outlined.Notifications, "Notifications", tint = TextSecondary)
                    }
                } else {
                    Icon(Icons.Outlined.Notifications, "Notifications", tint = TextSecondary)
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor       = RichBlack.copy(alpha = 0.95f),
            scrolledContainerColor = DarkCharcoal.copy(alpha = 0.98f),
            titleContentColor    = TextPrimary,
        ),
        scrollBehavior = scrollBehavior,
    )
}

// ── Stories Section ───────────────────────────────────────────────────────
// ── Section Divider ───────────────────────────────────────────────────────

@Composable
private fun SectionDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(
                Brush.horizontalGradient(
                    listOf(
                        RichBlack,
                        GlassBorder,
                        DeepRoyalPurple.copy(alpha = 0.3f),
                        GlassBorder,
                        RichBlack,
                    )
                )
            )
    )
}

// ── Pull-to-refresh Indicator ─────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PurplePullIndicator(state: PullToRefreshState, isRefreshing: Boolean) {
    PullToRefreshDefaults.Indicator(
        isRefreshing = isRefreshing,
        state      = state,
        color      = ElectricPurple,
        containerColor = ElevatedSurface,
    )
}

// ── Create Post FAB ───────────────────────────────────────────────────────

@Composable
private fun CreatePostFab(onClick: () -> Unit) {
    ExtendedFloatingActionButton(
        onClick           = onClick,
        containerColor    = DeepRoyalPurple,
        contentColor      = TextPrimary,
        icon = {
            Icon(Icons.Default.Edit, contentDescription = null)
        },
        text = {
            Text("Post", fontWeight = FontWeight.SemiBold)
        },
    )
}

// ── Error State ───────────────────────────────────────────────────────────

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(
        modifier            = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(
            imageVector        = Icons.Outlined.WifiOff,
            contentDescription = null,
            tint               = TextSecondary,
            modifier           = Modifier.size(48.dp),
        )
        Text(
            text  = message,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
        )
        Button(
            onClick         = onRetry,
            colors          = ButtonDefaults.buttonColors(containerColor = DeepRoyalPurple),
        ) {
            Text("Try again")
        }
    }
}
