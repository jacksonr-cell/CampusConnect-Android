package com.campusconnect.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class NavigationItem(
    val route:          String,
    val label:          String,
    val selectedIcon:   ImageVector,
    val unselectedIcon: ImageVector,
) {
    data object Feed : NavigationItem(
        route          = Routes.FEED,
        label          = "Feed",
        selectedIcon   = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home,
    )
    data object Explore : NavigationItem(
        route          = Routes.EXPLORE,
        label          = "Explore",
        selectedIcon   = Icons.Filled.Explore,
        unselectedIcon = Icons.Outlined.Explore,
    )
    data object Accommodation : NavigationItem(
        route          = Routes.ACCOMMODATION,
        label          = "Nest",
        selectedIcon   = Icons.Filled.House,
        unselectedIcon = Icons.Outlined.House,
    )
    data object Events : NavigationItem(
        route          = Routes.EVENTS,
        label          = "Events",
        selectedIcon   = Icons.Filled.Event,
        unselectedIcon = Icons.Outlined.Event,
    )
    data object Profile : NavigationItem(
        route          = Routes.PROFILE,
        label          = "Profile",
        selectedIcon   = Icons.Filled.Person,
        unselectedIcon = Icons.Outlined.Person,
    )
}
