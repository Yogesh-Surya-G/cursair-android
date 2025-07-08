package com.yelp.cursair.presentation

import androidx.compose.animation.Crossfade
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.yelp.cursair.presentation.main.MainScreen
import com.yelp.cursair.presentation.mouse.MouseScreen
import com.yelp.cursair.presentation.onboarding.OnBoardingScreen

// Define simple screen states for navigation
enum class Screen {
    Onboarding,
    Mouse,
    Main,

}

/**
 * The main entry point for the app's UI, which handles navigation
 * between the onboarding flow and the main application screen.
 */
@Composable
fun CursairApp() {
    // rememberSaveable ensures the state survives configuration changes
    var currentScreen by rememberSaveable { mutableStateOf(Screen.Onboarding) }

    // Crossfade provides a smooth transition between screens
    Crossfade(targetState = currentScreen, label = "main-navigation") { screen ->
        when (screen) {
            Screen.Onboarding -> {
                OnBoardingScreen(
                    onOnboardingFinished = {
                        // When onboarding is done, switch to the main screen
                        currentScreen = Screen.Mouse
                    }
                )
            }
            Screen.Mouse -> {
                MouseScreen(
                    onDisconnect = {
                        // When user disconnects, go back to the onboarding screen
                        currentScreen = Screen.Onboarding
                    }
                )
            }
            Screen.Main ->{
                MainScreen(
                    onConnectionEstablished = {
                        // When user connects, switch to the main screen
                        currentScreen = Screen.Mouse
                    }
                )
            }
        }
    }
}