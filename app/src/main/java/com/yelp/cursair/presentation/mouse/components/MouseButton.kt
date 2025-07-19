package com.yelp.cursair.presentation.mouse.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.yelp.cursair.ui.theme.CursairTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun MouseButton(
    text: String,
    modifier: Modifier = Modifier,
    isPressed: Boolean = false,
    onSingleClick: () -> Unit = {},
    onPressDown: () -> Unit = {},
    onPressUp: () -> Unit = {}
) {
    val interactionSource = remember { MutableInteractionSource() }
    val scope = rememberCoroutineScope()

    Surface(
        modifier = modifier
            .height(200.dp)
            .indication(
                interactionSource = interactionSource,
                indication = ripple(
                    bounded = true,
                    radius = 150.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            )
            .pointerInput("mouseButton") {
                detectTapGestures(
                    onPress = { offset ->
                        var isHold = false

                        // Create press interaction for ripple
                        val press = PressInteraction.Press(offset)
                        interactionSource.emit(press)

                        val holdJob = scope.launch {
                            delay(200)
                            isHold = true
                            onPressDown()
                        }

                        val released = tryAwaitRelease()
                        holdJob.cancel()

                        // Release the press interaction
                        interactionSource.emit(
                            if (released) {
                                PressInteraction.Release(press)
                            } else {
                                PressInteraction.Cancel(press)
                            }
                        )

                        if (isHold) {
                            onPressUp()
                        } else {
                            onSingleClick()
                        }
                    }
                )
            },
        shape = RoundedCornerShape(16.dp),
        color = if (isPressed)
            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        else
            Color.Transparent,
        border = BorderStroke(
            1.dp,
            if (isPressed)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.outline
        )
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = if (isPressed)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@PreviewLightDark
@Composable
fun MouseButtonPreview(){
    CursairTheme {
        MouseButton(text = "Test")
    }
}
