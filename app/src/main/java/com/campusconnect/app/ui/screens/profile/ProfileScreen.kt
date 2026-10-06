package com.campusconnect.app.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.campusconnect.app.data.model.*
import com.campusconnect.app.ui.components.*
import com.campusconnect.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ProfileScreen(
    widthSizeClass    : WindowWidthSizeClass,
    onNavigateToPost  : (String) -> Unit = {},
    onNavigateToProperty: (String) -> Unit = {},
    onEditProfile     : () -> Unit = {},
    viewModel: ProfileViewModel = viewModel(factory = com.campusconnect.app.di.AppViewModelProvider.Factory),
) {
    val state       by viewModel.state.collectAsStateWithLifecycle()
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val isWide = widthSizeClass != WindowWidthSizeClass.Compact

    Scaffold(
        containerColor = RichBlack,
        topBar = { ProfileTopBar(onLogout = viewModel::logout) },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (val s = state) {
                is ProfileUiState.Loading -> ProfileLoadingSkeleton()

                is ProfileUiState.Success -> {
                    if (isWide) {
                        Row(Modifier.fillMaxSize()) {
                            // ── Fixed sidebar ────────────────────────────────
                            Column(
                                modifier = Modifier
                                    .width(320.dp)
                                    .fillMaxHeight()
                                    .verticalScroll(rememberScrollState())
                                    .padding(20.dp),
                            ) {
                                ProfileHeader(profile = s.profile, onEditProfile = onEditProfile, compact = true)
                                Spacer(Modifier.height(20.dp))
                                LevelCard(stats = s.profile.stats)
                                Spacer(Modifier.height(20.dp))
                                BadgesPreview(badges = s.profile.badges)
                            }

                            VerticalDivider(color = GlassBorder, thickness = 1.dp)

                            // ── Scrollable content pane ──────────────────────
                            Box(Modifier.weight(1f).fillMaxHeight()) {
                                ProfileTabbedContent(
                                    state              = s,
                                    selectedTab        = selectedTab,
                                    onSelectTab        = viewModel::selectTab,
                                    widthSizeClass     = widthSizeClass,
                                    onNavigateToPost   = onNavigateToPost,
                                    onNavigateToProperty = onNavigateToProperty,
                                    showHeaderInline   = false,
                                )
                            }
                        }
                    } else {
                        // ── Compact — single scrolling column ─────────────────
                        ProfileTabbedContent(
                            state              = s,
                            selectedTab        = selectedTab,
                            onSelectTab        = viewModel::selectTab,
                            widthSizeClass     = widthSizeClass,
                            onNavigateToPost   = onNavigateToPost,
                            onNavigateToProperty = onNavigateToProperty,
                            showHeaderInline   = true,
                            onEditProfile      = onEditProfile,
                        )
                    }
                }

                is ProfileUiState.Error -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(s.message, color = TextSecondary)
                    }
                }
            }
        }
    }
}

// ── Top bar ────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileTopBar(onLogout: () -> Unit) {
    var showMenu by remember { mutableStateOf(false) }
    var showLogoutConfirm by remember { mutableStateOf(false) }

    TopAppBar(
        title = { Text("Profile", fontWeight = FontWeight.Bold, color = TextPrimary) },
        actions = {
            IconButton(onClick = { showMenu = true }) {
                Icon(Icons.Outlined.MoreVert, "More options", tint = TextSecondary)
            }
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                DropdownMenuItem(
                    text = { Text("Settings") },
                    leadingIcon = { Icon(Icons.Outlined.Settings, null) },
                    onClick = { showMenu = false /* settings screen not built yet */ },
                )
                DropdownMenuItem(
                    text = { Text("Log out", color = ErrorRed) },
                    leadingIcon = { Icon(Icons.AutoMirrored.Outlined.Logout, null, tint = ErrorRed) },
                    onClick = {
                        showMenu = false
                        showLogoutConfirm = true
                    },
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = RichBlack, titleContentColor = TextPrimary),
    )

    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = { Text("Log out?") },
            text  = { Text("You'll need to sign in again to access your account.") },
            confirmButton = {
                TextButton(onClick = { showLogoutConfirm = false; onLogout() }) {
                    Text("Log out", color = ErrorRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirm = false }) { Text("Cancel") }
            },
            containerColor = ElevatedSurface,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
        )
    }
}

// ── Tabbed content — shared between compact (with header) and wide (without) ──

@Composable
private fun ProfileTabbedContent(
    state               : ProfileUiState.Success,
    selectedTab         : ProfileTab,
    onSelectTab         : (ProfileTab) -> Unit,
    widthSizeClass      : WindowWidthSizeClass,
    onNavigateToPost    : (String) -> Unit,
    onNavigateToProperty: (String) -> Unit,
    showHeaderInline    : Boolean,
    onEditProfile       : () -> Unit = {},
) {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 32.dp)) {

        if (showHeaderInline) {
            item {
                Column(Modifier.padding(20.dp)) {
                    ProfileHeader(profile = state.profile, onEditProfile = onEditProfile, compact = false)
                    Spacer(Modifier.height(20.dp))
                    LevelCard(stats = state.profile.stats)
                }
            }
        }

        item {
            ProfileTabRow(
                selected = selectedTab,
                onSelect = onSelectTab,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            )
        }

        when (selectedTab) {
            ProfileTab.POSTS -> {
                if (state.posts.isEmpty()) {
                    item { EmptyTabState("No posts yet") }
                } else {
                    items(state.posts, key = { it.id }) { post ->
                        PostCard(
                            post          = post,
                            onReact       = { _, _ -> },
                            onComment     = { onNavigateToPost(it.id) },
                            onShare       = {},
                            onBookmark    = {},
                            onAuthorClick = {},
                            onPostClick   = { onNavigateToPost(it.id) },
                            modifier      = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        )
                    }
                }
            }

            ProfileTab.SAVED -> {
                if (state.savedProperties.isEmpty()) {
                    item { EmptyTabState("No saved properties yet") }
                } else {
                    val columns = columnsForWidth(widthSizeClass, dense = false)
                    item {
                        Column(Modifier.padding(horizontal = 16.dp)) {
                            state.savedProperties.chunked(columns).forEach { rowItems ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    rowItems.forEach { property ->
                                        PropertyCard(
                                            property     = property,
                                            onClick      = { onNavigateToProperty(property.id) },
                                            onSaveToggle = {},
                                            modifier     = Modifier.weight(1f),
                                            compact      = true,
                                        )
                                    }
                                    // Fill remaining slots so the last row doesn't stretch
                                    repeat(columns - rowItems.size) { Spacer(Modifier.weight(1f)) }
                                }
                            }
                        }
                    }
                }
            }

            ProfileTab.BADGES -> {
                item {
                    BadgesGrid(
                        badges  = state.profile.badges,
                        columns = columnsForWidth(widthSizeClass, dense = true),
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }
        }
    }
}

// ── Header (cover + avatar + name + bio) ────────────────────────────────────

@Composable
private fun ProfileHeader(profile: UserProfile, onEditProfile: () -> Unit, compact: Boolean) {
    Column {
        // Cover image + overlapping avatar
        Box(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (compact) 90.dp else 130.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (profile.coverImageUrl != null) Brush.linearGradient(listOf(Color(0xFF1E1B4B), Color(0xFF0F0A1E)))
                        else Brush.linearGradient(PurpleGradient)
                    ),
            ) {
                if (profile.coverImageUrl != null) {
                    AsyncImage(
                        model = profile.coverImageUrl, contentDescription = null,
                        contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp)
                    .offset(y = if (compact) 28.dp else 36.dp)
                    .size(if (compact) 64.dp else 76.dp)
                    .clip(CircleShape)
                    .background(RichBlack)
                    .padding(3.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(PurpleGradient)),
                contentAlignment = Alignment.Center,
            ) {
                if (profile.user.avatarUrl != null) {
                    AsyncImage(
                        model = profile.user.avatarUrl, contentDescription = null,
                        contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(CircleShape),
                    )
                } else {
                    Text(profile.user.initials, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = if (compact) 20.sp else 24.sp)
                }
            }
        }

        Spacer(Modifier.height(if (compact) 36.dp else 44.dp))

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(profile.user.displayName, style = MaterialTheme.typography.titleLarge, color = TextPrimary, fontWeight = FontWeight.Bold)
                    if (profile.user.isVerified) {
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.Filled.Verified, null, tint = ElectricPurple, modifier = Modifier.size(18.dp))
                    }
                }
                Text("@${profile.user.username}", style = MaterialTheme.typography.bodyMedium, color = TextTertiary)
            }
            if (profile.isOwnProfile) {
                OutlinedButton(
                    onClick = onEditProfile,
                    shape   = ChipShape,
                    border  = androidx.compose.foundation.BorderStroke(1.dp, ElectricPurple),
                    colors  = ButtonDefaults.outlinedButtonColors(contentColor = ElectricPurple),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                ) {
                    Icon(Icons.Outlined.Edit, null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Edit", style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        if (profile.courseOfStudy != null) {
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.School, null, tint = TextTertiary, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    buildString {
                        append(profile.courseOfStudy)
                        profile.yearOfStudy?.let { append(" · Year $it") }
                    },
                    style = MaterialTheme.typography.bodySmall, color = TextSecondary,
                )
            }
        }

        if (profile.bio != null) {
            Spacer(Modifier.height(10.dp))
            Text(profile.bio, style = MaterialTheme.typography.bodyMedium, color = TextSecondary, lineHeight = 20.sp)
        }

        Spacer(Modifier.height(14.dp))
        StatsRow(stats = profile.stats)

        Spacer(Modifier.height(10.dp))
        Text(
            "Joined ${SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date(profile.joinedAtMillis))}",
            style = MaterialTheme.typography.labelSmall, color = TextTertiary,
        )
    }
}

@Composable
private fun StatsRow(stats: ProfileStats) {
    Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        StatItem(value = stats.postsCount, label = "Posts")
        StatItem(value = stats.followersCount, label = "Followers")
        StatItem(value = stats.followingCount, label = "Following")
    }
}

@Composable
private fun StatItem(value: Int, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(formatCompact(value), style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.bodySmall, color = TextTertiary)
    }
}

// ── Level / reputation card ──────────────────────────────────────────────

@Composable
private fun LevelCard(stats: ProfileStats) {
    GlassmorphicCard(modifier = Modifier.fillMaxWidth(), glowColor = stats.level.tint.copy(alpha = 0.3f)) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(38.dp).clip(CircleShape).background(stats.level.tint.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.MilitaryTech, null, tint = stats.level.tint, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(stats.level.label, style = MaterialTheme.typography.titleSmall, color = TextPrimary, fontWeight = FontWeight.Bold)
                    Text("${stats.reputationPoints} points", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                }
            }

            val next = stats.level.next()
            if (next != null) {
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { stats.progressToNextLevel },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = stats.level.tint,
                    trackColor = Color(0xFF2D2D2D),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "${next.minPoints - stats.reputationPoints} points to ${next.label}",
                    style = MaterialTheme.typography.labelSmall, color = TextTertiary,
                )
            } else {
                Spacer(Modifier.height(8.dp))
                Text("Max level reached! 🎉", style = MaterialTheme.typography.labelSmall, color = stats.level.tint)
            }
        }
    }
}

// ── Tab row ───────────────────────────────────────────────────────────────

@Composable
private fun ProfileTabRow(selected: ProfileTab, onSelect: (ProfileTab) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(ChipShape)
            .background(ElevatedSurface)
            .padding(4.dp),
    ) {
        ProfileTab.entries.forEach { tab ->
            val isSelected = tab == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(ChipShape)
                    .background(if (isSelected) Brush.linearGradient(PurpleGradient) else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent)))
                    .clickableNoRipple { onSelect(tab) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    tab.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isSelected) TextPrimary else TextSecondary,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                )
            }
        }
    }
}

private fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier = this.then(
    Modifier.then(
        androidx.compose.foundation.clickable(
            interactionSource = androidx.compose.runtime.remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
            indication = null,
            onClick = onClick,
        )
    )
)

// ── Badges — preview (sidebar) and full grid (tab) ──────────────────────────

@Composable
private fun BadgesPreview(badges: List<Badge>) {
    val earned = badges.filter { it.isEarned }
    Column {
        Text("Badges (${earned.size}/${badges.size})", style = MaterialTheme.typography.titleSmall, color = TextPrimary, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            earned.take(4).forEach { badge -> BadgeIcon(badge, size = 42.dp) }
            if (earned.size > 4) {
                Box(
                    modifier = Modifier.size(42.dp).clip(CircleShape).background(GlassWhite05),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("+${earned.size - 4}", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                }
            }
        }
    }
}

@Composable
private fun BadgesGrid(badges: List<Badge>, columns: Int, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        badges.chunked(columns).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                row.forEach { badge ->
                    GlassmorphicCard(
                        modifier  = Modifier.weight(1f),
                        glowColor = if (badge.isEarned) badge.category.tint.copy(alpha = 0.3f) else Color.Transparent,
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            BadgeIcon(badge, size = 44.dp)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                badge.name, style = MaterialTheme.typography.labelMedium,
                                color = if (badge.isEarned) TextPrimary else TextTertiary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                badge.description, style = MaterialTheme.typography.labelSmall,
                                color = TextTertiary, textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                maxLines = 2, overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun BadgeIcon(badge: Badge, size: androidx.compose.ui.unit.Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(if (badge.isEarned) badge.category.tint.copy(alpha = 0.2f) else Color(0xFF2A2A2A))
            .border(1.dp, if (badge.isEarned) badge.category.tint.copy(alpha = 0.5f) else Color(0xFF3D3D3D), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            badge.category.emoji,
            style    = MaterialTheme.typography.titleMedium,
            modifier = Modifier.alpha(if (badge.isEarned) 1f else 0.35f),
        )
    }
}

// ── Empty state + loading skeleton ──────────────────────────────────────────

@Composable
private fun EmptyTabState(message: String) {
    Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
        Text(message, style = MaterialTheme.typography.bodyMedium, color = TextTertiary)
    }
}

@Composable
private fun ProfileLoadingSkeleton() {
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        ShimmerBox(Modifier.fillMaxWidth().height(90.dp), RoundedCornerShape(16.dp))
        Spacer(Modifier.height(44.dp))
        ShimmerBox(Modifier.width(160.dp).height(20.dp))
        Spacer(Modifier.height(8.dp))
        ShimmerBox(Modifier.width(100.dp).height(14.dp))
        Spacer(Modifier.height(20.dp))
        ShimmerBox(Modifier.fillMaxWidth().height(80.dp), RoundedCornerShape(12.dp))
    }
}

private fun formatCompact(count: Int): String = when {
    count >= 1_000_000 -> "${count / 1_000_000}M"
    count >= 1_000     -> String.format("%.1fK", count / 1000f)
    else               -> count.toString()
}
