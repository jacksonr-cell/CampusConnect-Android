package com.campusconnect.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.navigation.compose.rememberNavController
import com.campusconnect.app.ui.navigation.AdaptiveNavigation
import com.campusconnect.app.ui.navigation.CampusConnectNavGraph
import com.campusconnect.app.ui.theme.CampusConnectTheme

class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            CampusConnectTheme {
                val navController   = rememberNavController()
                val windowSizeClass = calculateWindowSizeClass(this)

                // AdaptiveNavigation wraps main routes only.
                // Auth routes (splash, onboarding, login, register) render
                // without any nav chrome — clean fullscreen experience.
                AdaptiveNavigation(
                    navController  = navController,
                    widthSizeClass = windowSizeClass.widthSizeClass,
                ) {
                    CampusConnectNavGraph(
                        navController  = navController,
                        widthSizeClass = windowSizeClass.widthSizeClass,
                    )
                }
            }
        }
    }
}
