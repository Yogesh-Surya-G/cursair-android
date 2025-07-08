package com.yelp.cursair.presentation.main

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.yelp.cursair.domain.ConnectionManager
import com.yelp.cursair.presentation.common.CursairLogo
import com.yelp.cursair.presentation.common.HorizontalDottedLoader
import com.yelp.cursair.presentation.common.QRCodeScannerView
import com.yelp.cursair.presentation.main.components.MainCardHost
import com.yelp.cursair.presentation.main.components.MainPageContent
import com.yelp.cursair.ui.theme.CursairTheme
import kotlinx.coroutines.launch

@Composable
fun MainScreen(
    onConnectionEstablished: () -> Unit = {}
) {

    val pagerState = rememberPagerState(pageCount = { 2 })
    val scope = rememberCoroutineScope()

    // States for the UI flow within Page 0
    var showScanner by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    // This state will hold the result of the connection attempt
    var connectionSuccessful by remember { mutableStateOf<Boolean?>(null) }

    // --- Camera Permission Handling ---
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
    // --- End Permission Handling ---

    CursairTheme {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding( horizontal = 24.dp)
            ) {
                CursairLogo(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .graphicsLayer(
                            scaleX = 0.6f,
                            scaleY = 0.6f,
                            transformOrigin = TransformOrigin(0f, 0.5f)
                        )
                )

                MainCardHost(
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    HorizontalPager(
                        state = pagerState,
                        userScrollEnabled = false
                    ) { page ->
                        when (page) {
                            0 -> { // --- Page 0: Initial Flow (Scan, Scanner, Loading) ---
                                AnimatedContent(
                                    targetState = when {
                                        isLoading -> "LOADING"
                                        showScanner -> "SCANNER"
                                        else -> "CONTENT"
                                    },
                                    label = "MainPageAnimation"
                                ) { targetState ->
                                    when (targetState) {
                                        "LOADING" -> { /* ... same as before ... */
                                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
                                                    isLoading = true
                                                    showScanner = false
                                                    scope.launch {
                                                        // Call the actual connect function
                                                        val result = ConnectionManager.connect(scannedValue)
                                                        connectionSuccessful = result
                                                        isLoading = false
                                                        // Navigate to the result page
                                                        pagerState.animateScrollToPage(1)
                                                    }
                                                }
                                            )
                                        }
                                        else -> { // "CONTENT"
                                            MainPageContent(cardData = initialPageCard)
                                        }
                                    }
                                }
                            }
                            1 -> { // --- Page 1: Result Page (Success or Failure) ---
                                val cardToDisplay = if (connectionSuccessful == true) successCard else failureCard
                                MainPageContent(cardData = cardToDisplay)
                            }
                        }
                    }
                }

                // The button's visibility is now controlled by the flow state
                val isButtonVisible = !showScanner && !isLoading
                AnimatedVisibility(
                    visible = isButtonVisible,
                    modifier = Modifier.align(Alignment.BottomCenter),
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    val cardDataForButton = when (pagerState.currentPage) {
                        0 -> initialPageCard
                        1 -> if (connectionSuccessful == true) successCard else failureCard
                        else -> initialPageCard // Fallback
                    }

                    Button(
                        onClick = {
                            when (pagerState.currentPage) {
                                0 -> { // On "Scan" page, start the scanning process
                                    showScanner = true
                                }
                                1 -> { // On a result page
                                    if (connectionSuccessful == true) {
                                        // On "Start" button click
                                        onConnectionEstablished()
                                    } else {
                                        // On "Try Again" button click, go back to scan page
                                        scope.launch {
                                            pagerState.animateScrollToPage(0)
                                        }
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.onSurface,
                            contentColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Text(
                            text = cardDataForButton.buttonText,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenFlowPreview() {
    MainScreen()
}