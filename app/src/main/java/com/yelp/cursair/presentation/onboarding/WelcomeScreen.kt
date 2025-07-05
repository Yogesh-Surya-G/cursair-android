package com.yelp.cursair.presentation.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yelp.cursair.R
import com.yelp.cursair.ui.theme.CursairTheme


@Composable
fun WelcomeCard() {
        val surfaceColor = MaterialTheme.colorScheme.surface
        val outlineColor = if(isSystemInDarkTheme()){
            MaterialTheme.colorScheme.surfaceBright
        }else{
            MaterialTheme.colorScheme.surfaceDim
        }
        Box(
            modifier = Modifier
                .width(340.dp)
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
            CardContent()
        }

}



@Composable
fun WelcomeScreen() {
    CursairTheme {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CursairLogo()
                Spacer(modifier = Modifier.height(48.dp))
                WelcomeCard()
            }
        }
    }
}

@Composable
fun CursairLogo() {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Image(
                painter = painterResource(R.drawable.logo),
                contentDescription = null,
                modifier = Modifier.size(36.dp)
            )
            Text(
                text = "Cursair",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
}

@Composable
fun CardContent() {
        Column(
            modifier = Modifier.padding(horizontal = 32.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "ADMIT ONE",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Welcome to Cursair",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Your new mouse.\nLet's get you set up.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.secondary,
                lineHeight = 24.sp
            )
            Spacer(modifier = Modifier.height(28.dp))
            Button(
                onClick = { /* Handle Get Started click */ },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onSurface)
            ) {
                Text(
                    text = "Get Started",
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.padding(vertical = 8.dp),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            LinearProgressIndicator(
                progress = { 0.3f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = MaterialTheme.colorScheme.onSurface,
                trackColor = MaterialTheme.colorScheme.inverseOnSurface,
            )
        }
}


@PreviewLightDark
@Composable
fun WelcomeScreenPreview() {
    CursairTheme {
        WelcomeScreen()
    }
}
