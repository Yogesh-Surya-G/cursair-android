package com.yelp.cursair.presentation.onboarding

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.yelp.cursair.domain.ConnectionManager
import com.yelp.cursair.presentation.common.CursairLogo
import com.yelp.cursair.presentation.common.QRCodeScannerView
import com.yelp.cursair.presentation.common.HorizontalDottedLoader
import com.yelp.cursair.presentation.onboarding.components.OnBoardingCardHost
import com.yelp.cursair.presentation.onboarding.components.OnBoardingPageContent
import com.yelp.cursair.ui.theme.CursairTheme
import kotlinx.coroutines.launch

@Composable
fun OnBoardingScreen(
    onOnboardingFinished: () -> Unit = {}
) {
    val pagerState = rememberPagerState(pageCount = { cards.size })

    val scope = rememberCoroutineScope()

    var showScanner by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    var connectionSuccessful by remember { mutableStateOf<Boolean?>(null) }

    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasCameraPermission = granted
        }
    )

    LaunchedEffect(showScanner, hasCameraPermission) {
        if (showScanner && !hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    CursairTheme {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CursairLogo(modifier = Modifier)
                Spacer(modifier = Modifier.height(48.dp))
                OnBoardingCardHost(pagerState = pagerState) {
                    HorizontalPager(
                        state = pagerState,
                        userScrollEnabled = false
                    ) { page ->
                        when (page) {
                            0 -> { // Welcome Page
                                OnBoardingPageContent(
                                    cardData = cards[page],
                                    onButtonClick = { scope.launch { pagerState.animateScrollToPage(1) } }
                                )
                            }
                            1 -> { // Scan Page with Loader
                                AnimatedContent(
                                    targetState = when {
                                        isLoading -> "LOADING"
                                        showScanner -> "SCANNER"
                                        else -> "CONTENT"
                                    },
                                    label = "ScanPageAnimation"
                                ) { targetState ->
                                    when (targetState) {
                                        "LOADING" -> {
                                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    modifier = Modifier.padding(vertical = 32.dp)
                                                ) {
                                                    HorizontalDottedLoader()
                                                    Spacer(modifier = Modifier.height(16.dp))
                                                    Text("Connecting...", style = MaterialTheme.typography.bodyLarge)
                                                }
                                            }
                                        }
                                        "SCANNER" -> {
                                            QRCodeScannerView(
                                                hasPermission = hasCameraPermission,
                                                onRequestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                                                onQRCodeScanned = { scannedValue ->
                                                    // Start the connection process
                                                    isLoading = true
                                                    showScanner = false
                                                    scope.launch {
                                                        val result = ConnectionManager.connect(scannedValue)
                                                        connectionSuccessful = result
                                                        isLoading = false
                                                        // Navigate only after the process is complete
                                                        pagerState.animateScrollToPage(2)
                                                    }
                                                }
                                            )
                                        }
                                        else -> { // "CONTENT"
                                            OnBoardingPageContent(
                                                cardData = cards[page],
                                                onButtonClick = { showScanner = true }
                                            )
                                        }
                                    }
                                }
                            }
                            2 -> { // Result Page
                                val cardToDisplay = if (connectionSuccessful == true) successCard else failureCard
                                OnBoardingPageContent(
                                    cardData = cardToDisplay,
                                    onButtonClick = {
                                        if (connectionSuccessful == true) {
                                            onOnboardingFinished()
                                        } else {
                                            // Go back to the scan page to try again
                                            scope.launch {
                                                pagerState.animateScrollToPage(1)
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


@PreviewLightDark
@Composable
fun OnBoardingScreenPreview() {
    OnBoardingScreen()
}



