package com.campusconnect.app.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.campusconnect.app.CampusConnectApp
import com.campusconnect.app.ui.screens.accommodation.AccommodationScreen
import com.campusconnect.app.ui.screens.accommodation.PropertyDetailScreen
import com.campusconnect.app.ui.screens.auth.LoginScreen
import com.campusconnect.app.ui.screens.auth.RegisterScreen
import com.campusconnect.app.ui.screens.events.EventsScreen
import com.campusconnect.app.ui.screens.feed.FeedScreen
import com.campusconnect.app.ui.screens.feed.CreatePostScreen
import com.campusconnect.app.ui.screens.feed.PostDetailScreen
import com.campusconnect.app.ui.screens.onboarding.OnboardingScreen
import com.campusconnect.app.ui.screens.profile.ProfileScreen
import com.campusconnect.app.ui.screens.splash.SplashScreen
import com.campusconnect.app.ui.theme.RichBlack
import com.campusconnect.app.ui.theme.TextSecondary

// ── Route constants ────────────────────────────────────────────────────────
object Routes {
    const val SPLASH      = "splash"
    const val ONBOARDING  = "onboarding"
    const val LOGIN       = "login"
    const val REGISTER    = "register"

    // Main app (authenticated)
    const val FEED             = "feed"
    const val EXPLORE          = "explore"
    const val ACCOMMODATION    = "accommodation"
    const val PROPERTY_DETAIL  = "accommodation/{propertyId}"
    const val EVENTS           = "events"
    const val EVENT_DETAIL     = "events/{eventId}"
    const val PROFILE          = "profile"
    const val POST_DETAIL      = "post/{postId}"
    const val USER_PROFILE     = "user/{userId}"
    const val CREATE_POST      = "create_post"

    fun postDetail(postId: String)         = "post/$postId"
    fun userProfile(userId: String)        = "user/$userId"
    fun propertyDetail(propertyId: String) = "accommodation/$propertyId"
    fun eventDetail(eventId: String)       = "events/$eventId"
}

/**
 * Root nav graph — starts at Splash, branches into auth or main app.
 * Auth flow:  Splash → Onboarding → Login | Register
 * Main flow:  Splash (already logged in) → Feed
 *
 * @param widthSizeClass forwarded to screens that need adaptive layout
 *        decisions (e.g. Kabarak Nest grid columns, two-pane detail view).
 */
@Composable
fun CampusConnectNavGraph(
    navController : NavHostController,
    widthSizeClass: WindowWidthSizeClass,
) {
    val context = LocalContext.current
    val app = context.applicationContext as CampusConnectApp
    val isLoggedIn by app.container.tokenManager.isLoggedInFlow.collectAsStateWithLifecycle()

    // Tracks the PREVIOUS login state so we only react to a session that WAS
    // active suddenly dying (refresh token expired/revoked, or an explicit
    // logout) — not the app's normal "never logged in yet" starting state,
    // which would otherwise wrongly fire this on every fresh install.
    var wasLoggedIn by remember { mutableStateOf(isLoggedIn) }

    LaunchedEffect(isLoggedIn) {
        if (wasLoggedIn && !isLoggedIn) {
            // Session died while the user was somewhere in the authenticated
            // app — clear the ENTIRE back stack so Back can't return into a
            // now-broken screen full of failed API calls.
            navController.navigate(Routes.LOGIN) {
                popUpTo(0) { inclusive = true }
            }
        }
        wasLoggedIn = isLoggedIn
    }

    NavHost(
        navController    = navController,
        startDestination = Routes.SPLASH,
    ) {

        // ── Splash ─────────────────────────────────────────────────────────
        composable(Routes.SPLASH) {
            SplashScreen(
                onSplashComplete = { isLoggedIn ->
                    val destination = if (isLoggedIn) Routes.FEED else Routes.ONBOARDING
                    navController.navigate(destination) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        // ── Onboarding ─────────────────────────────────────────────────────
        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                onGetStarted = {
                    navController.navigate(Routes.REGISTER) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                },
                onLogin = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                },
            )
        }

        // ── Login ──────────────────────────────────────────────────────────
        composable(Routes.LOGIN) {
            LoginScreen(
                onNavigateToRegister = {
                    navController.navigate(Routes.REGISTER) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onLoginSuccess = {
                    navController.navigate(Routes.FEED) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }

        // ── Register ───────────────────────────────────────────────────────
        composable(Routes.REGISTER) {
            RegisterScreen(
                onNavigateToLogin = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.REGISTER) { inclusive = true }
                    }
                },
                onRegisterSuccess = {
                    navController.navigate(Routes.FEED) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }

        // ── Main app ───────────────────────────────────────────────────────
        composable(Routes.FEED) { backStackEntry ->
            val refreshFeed by backStackEntry.savedStateHandle
                .getStateFlow("refreshFeed", false)
                .collectAsStateWithLifecycle()
            FeedScreen(
                onNavigateToPost    = { postId  -> navController.navigate(Routes.postDetail(postId)) },
                onNavigateToProfile = { userId  -> navController.navigate(Routes.userProfile(userId)) },
                onNavigateToCreate  = { navController.navigate(Routes.CREATE_POST) },
                refreshFeed = refreshFeed,
                onRefreshHandled = { backStackEntry.savedStateHandle["refreshFeed"] = false },
            )
        }

        composable(Routes.EXPLORE) { PlaceholderScreen("Explore") }

        // ── Kabarak Nest — accommodation listing ────────────────────────────
        composable(Routes.ACCOMMODATION) {
            AccommodationScreen(
                widthSizeClass     = widthSizeClass,
                onNavigateToDetail = { propertyId -> navController.navigate(Routes.propertyDetail(propertyId)) },
            )
        }

        // ── Kabarak Nest — property detail ───────────────────────────────────
        composable(
            route     = Routes.PROPERTY_DETAIL,
            arguments = listOf(navArgument("propertyId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val propertyId = backStackEntry.arguments?.getString("propertyId") ?: return@composable
            PropertyDetailScreen(
                propertyId     = propertyId,
                widthSizeClass = widthSizeClass,
                onBack         = { navController.popBackStack() },
                onInquire      = { /* TODO: open inquiry form */ },
            )
        }

        // ── Events — 3D featured carousel + upcoming list ────────────────────
        composable(Routes.EVENTS) {
            EventsScreen(
                onNavigateToDetail = { eventId -> navController.navigate(Routes.eventDetail(eventId)) },
            )
        }

        composable(
            route     = Routes.EVENT_DETAIL,
            arguments = listOf(navArgument("eventId") { type = NavType.StringType }),
        ) {
            PlaceholderScreen("Event Detail")
        }
        composable(Routes.PROFILE) {
            ProfileScreen(
                widthSizeClass       = widthSizeClass,
                onNavigateToPost     = { postId -> navController.navigate(Routes.postDetail(postId)) },
                onNavigateToProperty = { propertyId -> navController.navigate(Routes.propertyDetail(propertyId)) },
                onEditProfile        = { /* TODO: edit profile screen */ },
            )
        }
        composable(
            route = Routes.POST_DETAIL,
            arguments = listOf(navArgument("postId") { type = NavType.StringType }),
        ) { backStackEntry ->
            PostDetailScreen(
                postId = backStackEntry.arguments?.getString("postId") ?: return@composable,
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.USER_PROFILE) { PlaceholderScreen("User Profile") }
        composable(Routes.CREATE_POST) {
            CreatePostScreen(
                onBack = { navController.popBackStack() },
                onCreated = {
                    navController.previousBackStackEntry?.savedStateHandle?.set("refreshFeed", true)
                    navController.popBackStack()
                },
            )
        }
    }
}

@Composable
private fun PlaceholderScreen(title: String) {
    Box(
        modifier         = Modifier.fillMaxSize().background(RichBlack),
        contentAlignment = Alignment.Center,
    ) {
        Text(title, style = MaterialTheme.typography.headlineMedium, color = TextSecondary)
    }
}
