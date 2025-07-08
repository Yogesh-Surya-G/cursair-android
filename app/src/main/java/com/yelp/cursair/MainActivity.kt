package com.yelp.cursair

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.yelp.cursair.presentation.CursairApp
import com.yelp.cursair.presentation.main.MainScreen
import com.yelp.cursair.presentation.onboarding.OnBoardingScreen
import com.yelp.cursair.ui.theme.CursairTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CursairTheme {
                CursairApp()
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun Preview() {
    CursairTheme {
         OnBoardingScreen()
    }
}
