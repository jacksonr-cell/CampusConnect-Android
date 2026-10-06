package com.campusconnect.app.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.campusconnect.app.ui.theme.*

/** Routes that should show the bottom/rail/drawer navigation. */
private val mainRoutes = setOf(
    Routes.FEED, Routes.EXPLORE, Routes.ACCOMMODATION, Routes.EVENTS, Routes.PROFILE,
)

@Composable
fun AdaptiveNavigation(
    navController:  NavHostController,
    widthSizeClass: WindowWidthSizeClass,
    content:        @Composable () -> Unit,
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute   = backStackEntry?.destination?.route
    val showNav        = currentRoute in mainRoutes

    when {
        !showNav                              -> content()   // auth / splash screens — no nav
        widthSizeClass == WindowWidthSizeClass.Compact  -> CompactLayout(navController, currentRoute, content)
        widthSizeClass == WindowWidthSizeClass.Medium   -> MediumLayout(navController, currentRoute, content)
        else                                             -> ExpandedLayout(navController, currentRoute, content)
    }
}

// ── Compact — Bottom Navigation Bar ───────────────────────────────────────
@Composable
private fun CompactLayout(navController: NavHostController, currentRoute: String?, content: @Composable () -> Unit) {
    val items = listOf(
        NavigationItem.Feed, NavigationItem.Explore,
        NavigationItem.Accommodation, NavigationItem.Events, NavigationItem.Profile,
    )
    Scaffold(
        containerColor = RichBlack,
        bottomBar = {
            NavigationBar(
                containerColor = DarkCharcoal,
                tonalElevation = 0.dp,
            ) {
                items.forEach { item ->
                    val selected = currentRoute == item.route
                    NavigationBarItem(
                        selected = selected,
                        onClick  = { navController.navigateSingleTop(item.route) },
                        icon     = { Icon(if (selected) item.selectedIcon else item.unselectedIcon, item.label) },
                        label    = { Text(item.label, style = MaterialTheme.typography.labelSmall) },
                        colors   = NavigationBarItemDefaults.colors(
                            selectedIconColor   = ElectricPurple,
                            selectedTextColor   = ElectricPurple,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary,
                            indicatorColor      = DeepRoyalPurple.copy(alpha = 0.2f),
                        ),
                    )
                }
            }
        }
    ) { padding -> Box(Modifier.padding(padding)) { content() } }
}

// ── Medium — Navigation Rail ───────────────────────────────────────────────
@Composable
private fun MediumLayout(navController: NavHostController, currentRoute: String?, content: @Composable () -> Unit) {
    val items = listOf(NavigationItem.Feed, NavigationItem.Explore, NavigationItem.Accommodation, NavigationItem.Events, NavigationItem.Profile)
    Row(Modifier.fillMaxSize()) {
        NavigationRail(containerColor = DarkCharcoal) {
            Spacer(Modifier.height(12.dp))
            items.forEach { item ->
                val selected = currentRoute == item.route
                NavigationRailItem(
                    selected = selected,
                    onClick  = { navController.navigateSingleTop(item.route) },
                    icon     = { Icon(if (selected) item.selectedIcon else item.unselectedIcon, item.label) },
                    label    = { Text(item.label, style = MaterialTheme.typography.labelSmall) },
                    colors   = NavigationRailItemDefaults.colors(
                        selectedIconColor   = ElectricPurple,
                        selectedTextColor   = ElectricPurple,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary,
                        indicatorColor      = DeepRoyalPurple.copy(alpha = 0.2f),
                    ),
                )
            }
        }
        Box(Modifier.weight(1f)) { content() }
    }
}

// ── Expanded — Permanent Drawer ────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpandedLayout(navController: NavHostController, currentRoute: String?, content: @Composable () -> Unit) {
    val items = listOf(NavigationItem.Feed, NavigationItem.Explore, NavigationItem.Accommodation, NavigationItem.Events, NavigationItem.Profile)
    PermanentNavigationDrawer(
        drawerContent = {
            PermanentDrawerSheet(
                modifier             = Modifier.width(260.dp),
                drawerContainerColor = DarkCharcoal,
            ) {
                Spacer(Modifier.height(24.dp))
                Row(modifier = Modifier.padding(horizontal = 20.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Brush.linearGradient(PurpleGradient), androidx.compose.foundation.shape.RoundedCornerShape(12.dp)),
                        contentAlignment = androidx.compose.ui.Alignment.Center,
                    ) { Text("CC", color = TextPrimary, style = MaterialTheme.typography.titleMedium) }
                    Spacer(Modifier.width(12.dp))
                    Text("CampusConnect", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                }
                Spacer(Modifier.height(24.dp))
                items.forEach { item ->
                    val selected = currentRoute == item.route
                    NavigationDrawerItem(
                        icon     = { Icon(if (selected) item.selectedIcon else item.unselectedIcon, item.label) },
                        label    = { Text(item.label) },
                        selected = selected,
                        onClick  = { navController.navigateSingleTop(item.route) },
                        modifier = Modifier.padding(horizontal = 12.dp),
                        colors   = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = DeepRoyalPurple.copy(alpha = 0.2f),
                            selectedIconColor      = ElectricPurple,
                            selectedTextColor      = ElectricPurple,
                            unselectedIconColor    = TextSecondary,
                            unselectedTextColor    = TextSecondary,
                        ),
                    )
                }
            }
        }
    ) { content() }
}

fun NavHostController.navigateSingleTop(route: String) {
    navigate(route) {
        popUpTo(graph.startDestinationId) { saveState = true }
        launchSingleTop = true
        restoreState    = true
    }
}
