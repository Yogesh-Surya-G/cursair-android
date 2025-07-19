package com.yelp.cursair.presentation.mouse.modes

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yelp.cursair.domain.ConnectionManager
import com.yelp.cursair.domain.Sensor.AirMouseStreamerV
import com.yelp.cursair.presentation.mouse.components.MouseButton
import com.yelp.cursair.ui.theme.CursairTheme
import kotlinx.coroutines.launch
import kotlin.math.abs


@Composable
fun AirMouseVScreen() {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val isConnected by ConnectionManager.isConnected.collectAsState()
    var isLeftPressed by remember { mutableStateOf(false) }
    var isRightPressed by remember { mutableStateOf(false) }



    val isInPreview = LocalInspectionMode.current
    val rotationStreamer = remember {
        if (isInPreview) {
            null
        } else {
            AirMouseStreamerV(context, scope)
        }
    }

    LaunchedEffect(isConnected) {
        if (isConnected) {
            rotationStreamer?.startStreaming()
        } else {
            rotationStreamer?.stopStreaming()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        // Top grid pattern area
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(24.dp),
            color = Color.Transparent
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
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

        Spacer(modifier = Modifier.height(24.dp))

        // Mouse Click Buttons with Scroll in center
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Button
            MouseButton(
                text = "Left",
                modifier = Modifier.width(120.dp),
                isPressed = isLeftPressed,
                onSingleClick = {
                    scope.launch {
                        ConnectionManager.sendMessage("{\"event\":\"lmb\"}")
                    }
                },
                onPressDown = {
                    isLeftPressed = true
                    scope.launch {
                        ConnectionManager.sendMessage("{\"event\":\"lmb_down\"}")
                    }
                },
                onPressUp = {
                    isLeftPressed = false
                    scope.launch {
                        ConnectionManager.sendMessage("{\"event\":\"lmb_up\"}")
                    }
                }
            )


            Surface(
                modifier = Modifier
                    .size(80.dp)
                    .pointerInput("tap") {
                        detectTapGestures(
                            onTap = {
                                scope.launch {
                                    ConnectionManager.sendMessage("{\"event\":\"mmb\"}")
                                    println("MMB clicked") // Debug log
                                }
                            }
                        )
                    }
                    .pointerInput("drag") {
                        detectDragGestures { change, dragAmount ->
                            val scrollDistance = dragAmount.y.toInt()

                            if (abs(scrollDistance) > 3) {
                                val dist = -scrollDistance
                                scope.launch {
                                    ConnectionManager.sendMessage("{\"event\":\"scroll\", \"dist\":$dist}")
                                    println("Scroll: $dist") // Debug log
                                }
                            }
                        }
                    },
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
                            text = "↑",
                            fontSize = 24.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "↓",
                            fontSize = 24.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                    }
                }
            }


            //Right Mouse Button
            MouseButton(
                text = "Right",
                modifier = Modifier.width(120.dp),
                isPressed = isRightPressed,
                onSingleClick = {
                    scope.launch {
                        ConnectionManager.sendMessage("{\"event\":\"rmb\"}")
                    }
                },
                onPressDown = {
                    isRightPressed = true
                    scope.launch {
                        ConnectionManager.sendMessage("{\"event\":\"rmb_down\"}")
                    }
                },
                onPressUp = {
                    isRightPressed = false
                    scope.launch {
                        ConnectionManager.sendMessage("{\"event\":\"rmb_up\"}")
                    }
                }
            )

        }

        Spacer(modifier = Modifier.height(24.dp))

        // Bottom grid pattern area
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(24.dp),
            color = Color.Transparent
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
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

        Spacer(modifier = Modifier.height(200.dp)) // Space for bottom navigation
    }
}

@PreviewLightDark
@Composable
fun AirMouseVPreview(){
    CursairTheme {
        AirMouseVScreen()
    }
}
