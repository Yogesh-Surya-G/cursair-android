package com.yelp.cursair.presentation.main.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yelp.cursair.presentation.common.CursairLogoIcon
import com.yelp.cursair.presentation.main.initialPageCard
import com.yelp.cursair.ui.theme.CursairTheme


@Composable
fun MainCardHost(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val surfaceColor = MaterialTheme.colorScheme.surface
    val outlineColor = if(isSystemInDarkTheme()){
        MaterialTheme.colorScheme.surfaceBright
    }else{
        MaterialTheme.colorScheme.surfaceDim
    }

    val ticketShape = remember { TicketShape() }

    val alphaValue = if(isSystemInDarkTheme()){
        0.05f
    }else{
        0.1f
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .width(380.dp)
                .height(380.dp)
                .clip(ticketShape)
                .drawWithCache {
                    val cornerRadius = 16.dp.toPx()
                    val cutoutRadius = 25.dp.toPx()
                    val path = Path().apply {
                        reset()
                        moveTo(cornerRadius, 0f)
                        lineTo(size.width - cornerRadius, 0f)
                        arcTo(Rect(size.width - 2 * cornerRadius, 0f, size.width, 2 * cornerRadius), -90f, 90f, false)
                        lineTo(size.width, size.height / 2 - cutoutRadius)
                        arcTo(Rect(size.width - cutoutRadius, size.height / 2 - cutoutRadius, size.width + cutoutRadius, size.height / 2 + cutoutRadius), -90f, -180f, false)
                        lineTo(size.width, size.height - cornerRadius)
                        arcTo(Rect(size.width - 2 * cornerRadius, size.height - 2 * cornerRadius, size.width, size.height), 0f, 90f, false)
                        lineTo(cornerRadius, size.height)
                        arcTo(Rect(0f, size.height - 2 * cornerRadius, 2 * cornerRadius, size.height), 90f, 90f, false)
                        lineTo(0f, size.height / 2 + cutoutRadius)
                        arcTo(Rect(-cutoutRadius, size.height / 2 - cutoutRadius, cutoutRadius, size.height / 2 + cutoutRadius), 90f, -180f, false)
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
            // This Box layers the faint background logo and the main content.
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                // Faint logo in the background of the card
                CursairLogoIcon(
                    modifier = Modifier.size(220.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = alphaValue)
                )
                // The main text content on top of the logo
                content()
            }
        }
    }
}

@Composable
fun MainPageContent(cardData: com.yelp.cursair.presentation.main.CardData) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceAround // Vertically center the text block
    ) {
        Text(
            text = cardData.connectionState,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            letterSpacing = 2.sp
        )
        Spacer(modifier = Modifier.height(36.dp))
        Text(
            text = cardData.title,
            style = MaterialTheme.typography.headlineLarge, // Use a larger style for emphasis
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
            lineHeight = 44.sp // Adjust line height for the two-line title
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = cardData.description,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.secondary,
            lineHeight = 24.sp
        )
    }
}

@PreviewLightDark
@Composable
fun MainPageCardPreview() {
    CursairTheme {
        Box(modifier = Modifier.padding(20.dp), contentAlignment = Alignment.Center) {
            MainCardHost {
                MainPageContent(initialPageCard)
            }
        }
    }
}