package com.yelp.cursair.navigation

import UserPreferencesRepository
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.yelp.cursair.R
import com.yelp.cursair.presentation.main.MainScreen
import com.yelp.cursair.presentation.mouse.MouseScreen
import com.yelp.cursair.presentation.onboarding.OnBoardingScreen
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun Navigation(){

    val context = LocalContext.current
    val repository = remember { UserPreferencesRepository(context) }
    val scope = rememberCoroutineScope()

    val startDestination by produceState<String?>(initialValue = null) {
        value = if (repository.isOnboardingFinished.first()) {
            Screen.MainScreen.route
        } else {
            Screen.OnBoardingScreen.route
        }
    }

    if(startDestination!=null){
        val navController = rememberNavController()
        NavHost(navController = navController, startDestination = startDestination.toString()) {
            composable(Screen.OnBoardingScreen.route) {
                OnBoardingScreen {
                    scope.launch {
                        repository.setOnboardingFinished()
                        navController.navigate(Screen.MouseScreen.route) {
                            popUpTo(Screen.OnBoardingScreen.route) {
                                inclusive = true
                            }
                        }
                    }
                }
            }

            composable(Screen.MainScreen.route) {
                MainScreen(
                    onConnectionEstablished = {
                        navController.navigate(Screen.MouseScreen.route)
                    }
                )
            }

            composable(Screen.MouseScreen.route) {
                MouseScreen(
                    onDisconnect = {
                        navController.navigate(Screen.MainScreen.route)
                    }
                )
            }
        }
    }else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(color = MaterialTheme.colorScheme.background), // Your splash background color
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.mipmap.ic_launcher_foreground),
                contentDescription = "App Logo",
                modifier = Modifier.size(240.dp)
            )
        }
    }
}
