package com.yelp.cursair.presentation.mouse.modes

import com.yelp.cursair.domain.Sensor.AirMouseStreamer
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yelp.cursair.domain.ConnectionManager
import com.yelp.cursair.ui.theme.CursairTheme
import kotlinx.coroutines.launch

@Composable
fun AirMouseScreen(
    onDisconnect: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val isConnected by ConnectionManager.isConnected.collectAsState()

    val isInPreview = LocalInspectionMode.current
    val rotationStreamer = remember {
        if (isInPreview) {
            null
        } else {
            AirMouseStreamer(context, scope)
        }
    }

    LaunchedEffect(isConnected) {
        if (isConnected) {
            rotationStreamer?.startStreaming()
        } else {
            rotationStreamer?.stopStreaming()
        }
    }

    CursairTheme {
        Scaffold { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(modifier = Modifier.height(32.dp))

                // Mouse Click Buttons with Scroll in center
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Button
                    MouseButton(
                        text = "Left",
                        modifier = Modifier.width(120.dp)
                    ) {
                        scope.launch {
                            ConnectionManager.sendMessage("{\"event\":\"lmb\"}")
                        }
                    }

                    // Scroll Button in center
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(40.dp),
                            shape = CircleShape,
                            color = Color.Transparent,
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "^",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "v",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Scroll",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    // Right Button
                    MouseButton(
                        text = "Right",
                        modifier = Modifier.width(120.dp)
                    ) {
                        scope.launch {
                            ConnectionManager.sendMessage("{\"event\":\"rmb\"}")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Trackpad area with grid pattern
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    color = Color.Transparent
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Grid pattern
                        Canvas(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp)) {
                            val gridSpacing = 30.dp.toPx()
                            val dotRadius = 2.dp.toPx()
                            val dotColor = Color.Gray.copy(alpha = 0.4f)

                            val width = size.width
                            val height = size.height

                            // Draw grid dots
                            var x = gridSpacing
                            while (x < width) {
                                var y = gridSpacing
                                while (y < height) {
                                    drawCircle(
                                        color = dotColor,
                                        radius = dotRadius,
                                        center = Offset(x, y)
                                    )
                                    y += gridSpacing
                                }
                                x += gridSpacing
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun MouseButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(200.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@PreviewLightDark
@Composable
fun AirMousePreview(){
    CursairTheme {
       AirMouseScreen(onDisconnect = {})
    }
}