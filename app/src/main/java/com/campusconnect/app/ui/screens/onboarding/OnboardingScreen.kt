package com.campusconnect.app.ui.screens.onboarding

import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.campusconnect.app.ui.components.PrimaryButton
import com.campusconnect.app.ui.components.SecondaryButton
import com.campusconnect.app.ui.components.StepIndicator
import com.campusconnect.app.ui.theme.*
import kotlinx.coroutines.launch

data class OnboardingPage(
    val icon:        ImageVector,
    val iconTint:    Color,
    val title:       String,
    val description: String,
    val gradient:    List<Color>,
)

private val pages = listOf(
    OnboardingPage(
        icon        = Icons.Filled.People,
        iconTint    = ElectricPurple,
        title       = "Your Campus Social Network",
        description = "Connect with fellow students, share moments, join communities, and stay updated with everything happening on campus.",
        gradient    = listOf(Color(0xFF1E1B4B), Color(0xFF0F0A1E)),
    ),
    OnboardingPage(
        icon        = Icons.Filled.House,
        iconTint    = NeonViolet,
        title       = "Find Your Perfect Nest",
        description = "Kabarak Nest makes finding accommodation easy. Browse verified listings, compare prices, and contact landlords — all in one place.",
        gradient    = listOf(Color(0xFF1A1040), Color(0xFF0A0A1A)),
    ),
    OnboardingPage(
        icon        = Icons.Filled.Event,
        iconTint    = Color(0xFFF59E0B),
        title       = "Never Miss an Event",
        description = "Discover campus events, RSVP in one tap, and get reminders. From academic conferences to social nights — it's all here.",
        gradient    = listOf(Color(0xFF1C1A10), Color(0xFF0F0A00)),
    ),
    OnboardingPage(
        icon        = Icons.Filled.ShoppingBag,
        iconTint    = Color(0xFF22C55E),
        title       = "Campus Marketplace",
        description = "Buy, sell, and trade with fellow students. Textbooks, electronics, furniture — find everything you need at student prices.",
        gradient    = listOf(Color(0xFF0A1F0A), Color(0xFF050F05)),
    ),
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onGetStarted: () -> Unit,
    onLogin:      () -> Unit,
) {
    val pagerState = rememberPagerState { pages.size }
    val scope      = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize().background(RichBlack)) {

        // Animated page background
        HorizontalPager(
            state    = pagerState,
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(pages[page].gradient))
            )
        }

        // Overlay fade to black at bottom
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.5f)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(listOf(Color.Transparent, RichBlack))
                )
        )

        Column(modifier = Modifier.fillMaxSize()) {

            // Pager content
            HorizontalPager(
                state    = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) { pageIndex ->
                val page = pages[pageIndex]
                val pageOffset by remember(pagerState) {
                    derivedStateOf {
                        val raw = pagerState.currentPage - pageIndex + pagerState.currentPageOffsetFraction
                        raw.coerceIn(-1f, 1f)
                    }
                }
                val alpha = 1f - (pageOffset * pageOffset).coerceIn(0f, 1f)

                Column(
                    modifier            = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 32.dp)
                        .alpha(alpha),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    // Feature icon
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .background(
                                brush  = Brush.radialGradient(
                                    listOf(page.iconTint.copy(alpha = 0.2f), Color.Transparent)
                                ),
                                shape  = androidx.compose.foundation.shape.RoundedCornerShape(50),
                            )
                            .background(
                                GlassWhite05,
                                androidx.compose.foundation.shape.RoundedCornerShape(50),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector        = page.icon,
                            contentDescription = null,
                            tint               = page.iconTint,
                            modifier           = Modifier.size(56.dp),
                        )
                    }

                    Spacer(Modifier.height(36.dp))

                    Text(
                        text       = page.title,
                        style      = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color      = TextPrimary,
                        textAlign  = TextAlign.Center,
                        lineHeight = 32.sp,
                    )

                    Spacer(Modifier.height(16.dp))

                    Text(
                        text      = page.description,
                        style     = MaterialTheme.typography.bodyMedium,
                        color     = TextSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp,
                    )
                }
            }

            // Bottom controls
            Column(
                modifier            = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
                    .padding(bottom = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Step dots
                StepIndicator(
                    totalSteps  = pages.size,
                    currentStep = pagerState.currentPage,
                    modifier    = Modifier.padding(bottom = 8.dp),
                )

                val isLastPage = pagerState.currentPage == pages.size - 1

                if (isLastPage) {
                    PrimaryButton(text = "Get Started", onClick = onGetStarted)
                    SecondaryButton(text = "I already have an account", onClick = onLogin)
                } else {
                    PrimaryButton(
                        text    = "Next",
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        },
                    )
                    TextButton(onClick = onGetStarted) {
                        Text("Skip", color = TextSecondary, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}
