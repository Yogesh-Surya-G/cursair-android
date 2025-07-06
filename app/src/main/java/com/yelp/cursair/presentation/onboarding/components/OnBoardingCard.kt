package com.yelp.cursair.presentation.onboarding.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yelp.cursair.presentation.onboarding.CardData
import com.yelp.cursair.presentation.onboarding.cards


@Composable
fun OnBoardingCardHost(pagerState: PagerState, content: @Composable () -> Unit) {

        val targetProgress = cards[pagerState.currentPage].progress
        val animatedProgress by animateFloatAsState(
            targetValue = targetProgress,
            animationSpec = tween(durationMillis = 400),
            label = "ProgressAnimation"
        )


        val surfaceColor = MaterialTheme.colorScheme.surface
        val outlineColor = if(isSystemInDarkTheme()){
            MaterialTheme.colorScheme.surfaceBright
        }else{
            MaterialTheme.colorScheme.surfaceDim
        }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .width(340.dp)
                .height(340.dp)
                .drawWithCache {
                    val cornerRadius = 16.dp.toPx()
                    val cutoutRadius = 25.dp.toPx()
                    val path = Path().apply {
                        reset()
                        moveTo(cornerRadius, 0f)
                        lineTo(size.width - cornerRadius, 0f)
                        arcTo(
                            Rect(size.width - 2 * cornerRadius, 0f, size.width, 2 * cornerRadius),
                            -90f,
                            90f,
                            false
                        )
                        lineTo(size.width, size.height / 2 - cutoutRadius)
                        arcTo(
                            Rect(
                                size.width - cutoutRadius,
                                size.height / 2 - cutoutRadius,
                                size.width + cutoutRadius,
                                size.height / 2 + cutoutRadius
                            ), -90f, -180f, false
                        )
                        lineTo(size.width, size.height - cornerRadius)
                        arcTo(
                            Rect(
                                size.width - 2 * cornerRadius,
                                size.height - 2 * cornerRadius,
                                size.width,
                                size.height
                            ), 0f, 90f, false
                        )
                        lineTo(cornerRadius, size.height)
                        arcTo(
                            Rect(
                                0f,
                                size.height - 2 * cornerRadius,
                                2 * cornerRadius,
                                size.height
                            ), 90f, 90f, false
                        )
                        lineTo(0f, size.height / 2 + cutoutRadius)
                        arcTo(
                            Rect(
                                -cutoutRadius,
                                size.height / 2 - cutoutRadius,
                                cutoutRadius,
                                size.height / 2 + cutoutRadius
                            ), 90f, -180f, false
                        )
                        lineTo(0f, cornerRadius)
                        arcTo(Rect(0f, 0f, 2 * cornerRadius, 2 * cornerRadius), 180f, 90f, false)
                        close()
                    }

                    onDrawBehind {

                        drawPath(path = path, color = surfaceColor)
                        drawPath(
                            path = path,
                            color = outlineColor,
                            style = Stroke(
                                width = 2.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 10f), 0f)
                            )
                        )
                    }
                }
        ) {
            content()
        }
        Spacer(modifier = Modifier.height(20.dp))
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .width(339.dp)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = MaterialTheme.colorScheme.onSurface,
            trackColor = MaterialTheme.colorScheme.inverseOnSurface,
        )
    }

}





@Composable
fun OnBoardingPageContent(cardData: CardData, onButtonClick: () -> Unit) {

    Column(
            modifier = Modifier.padding(horizontal = 32.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "ADMIT ONE",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                letterSpacing = 2.sp

            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = cardData.title,
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = cardData.description,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.secondary,
                lineHeight = 24.sp
            )
//            Spacer(modifier = Modifier.height(28.dp))
            Spacer(modifier = Modifier.weight(1f))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(
                        onClick = onButtonClick,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(color = MaterialTheme.colorScheme.primary)
                    ),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.onSurface
            ) {
                Box(
                    modifier = Modifier.padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = cardData.buttonText,
                        color = MaterialTheme.colorScheme.surface,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
}



