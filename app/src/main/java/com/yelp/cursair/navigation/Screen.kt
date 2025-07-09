package com.yelp.cursair.navigation

sealed class Screen(val route : String) {
    object MainScreen : Screen("main_screen")
    object OnBoardingScreen : Screen("onboarding_screen")
    object MouseScreen : Screen("mouse_screen")
}