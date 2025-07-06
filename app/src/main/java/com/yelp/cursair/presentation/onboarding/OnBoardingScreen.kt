package com.yelp.cursair.presentation.onboarding

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import com.yelp.cursair.presentation.common.CursairLogo
import com.yelp.cursair.presentation.common.QRCodeScannerView
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
                CursairLogo()
                Spacer(modifier = Modifier.height(48.dp))
                OnBoardingCardHost(pagerState = pagerState) {
                    HorizontalPager(state = pagerState, userScrollEnabled = false) { page ->
                        if (page == 1) {
                            AnimatedContent(
                                targetState = showScanner,
                                label = "CardContentAnimation",
                                transitionSpec = {
                                    (scaleIn() + fadeIn()) togetherWith (scaleOut() + fadeOut())
                                }
                            ) { isScanning ->
                                if (isScanning) {
                                    QRCodeScannerView(
                                        hasPermission = hasCameraPermission,
                                        onRequestPermission = {
                                            permissionLauncher.launch(Manifest.permission.CAMERA)
                                        },
                                        onQRCodeScanned = {
                                            scope.launch {
                                                pagerState.animateScrollToPage(page + 1)
                                            }
                                            showScanner = false
                                        }
                                    )
                                } else {
                                    OnBoardingPageContent(cardData = cards[page]){
                                        showScanner = true
                                    }
                                }
                            }
                        } else {
                            OnBoardingPageContent(cardData = cards[page]){
                                scope.launch {
                                    if (pagerState.currentPage < cards.size - 1) {
                                        pagerState.animateScrollToPage(page + 1)
                                    } else {
                                        onOnboardingFinished()
                                    }
                                }
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



