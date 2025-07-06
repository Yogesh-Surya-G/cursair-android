package com.yelp.cursair.presentation.onboarding

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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.yelp.cursair.presentation.common.CursairLogo
import com.yelp.cursair.presentation.onboarding.components.OnBoardingCardHost
import com.yelp.cursair.presentation.onboarding.components.OnBoardingPageContent
import com.yelp.cursair.ui.theme.CursairTheme
import kotlinx.coroutines.launch

@Composable
fun OnBoardingScreen(
    onBoardingComplete: () -> Unit = {}
) {
    val pagerState = rememberPagerState(pageCount = { cards.size })

    val scope = rememberCoroutineScope()

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
                    HorizontalPager(state = pagerState) { page ->
                        OnBoardingPageContent(cardData = cards[page]) {
                            scope.launch {
                                if (pagerState.currentPage < cards.size - 1) {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                } else {
                                    onBoardingComplete()
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



