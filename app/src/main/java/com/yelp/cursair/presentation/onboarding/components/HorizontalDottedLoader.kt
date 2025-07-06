package com.yelp.cursair.presentation.onboarding.components


import androidx.compose.animation.core.InfiniteTransition
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.StartOffsetType
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateValue
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A cool, modern loader with three horizontally arranged dots that animate up and down in a wave.
 *
 * @param modifier Modifier to be applied to the layout.
 * @param color The color of the dots.
 * @param dotSize The size of each dot.
 * @param dotSpacing The spacing between each dot.
 * @param travelDistance The vertical distance each dot will travel.
 */
@Composable
fun HorizontalDottedLoader(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    dotSize: Dp = 12.dp,
    dotSpacing: Dp = 8.dp,
    travelDistance: Dp = 16.dp
) {
    // This is the infinite transition that drives the animation.
    val infiniteTransition = rememberInfiniteTransition(label = "DottedLoaderTransition")

    // A list to hold the animated Y-offset for each dot.
    val yOffsets = listOf(
        animateDot(infiniteTransition, delay = 0),
        animateDot(infiniteTransition, delay = 160),
        animateDot(infiniteTransition, delay = 320)
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(dotSpacing)
    ) {
        yOffsets.forEach { yOffset ->
            Dot(
                yOffset = yOffset.value,
                dotSize = dotSize,
                color = color,
                travelDistance = travelDistance
            )
        }
    }
}

/**
 * Creates a single dot with its animation properties.
 */
@Composable
private fun animateDot(
    infiniteTransition: InfiniteTransition,
    delay: Int
): State<Dp> {
    return infiniteTransition.animateValue(
        initialValue = 0.dp,
        targetValue = 0.dp, // This value is not used, the animation is driven by the spec
        typeConverter = Dp.VectorConverter,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1000 // Total duration for one cycle
                0.dp at 0 // Start at baseline
                (-16).dp at 250 // Move up
                0.dp at 500 // Return to baseline
            },
            repeatMode = RepeatMode.Restart,
            initialStartOffset = StartOffset(delay, StartOffsetType.Delay)
        ), label = "DotAnimation"
    )
}

/**
 * The actual Composable for a single dot.
 */
@Composable
private fun Dot(
    yOffset: Dp,
    dotSize: Dp,
    color: Color,
    travelDistance: Dp
) {
    Box(
        modifier = Modifier
            // The offset modifier moves the dot vertically based on the animation value
            .offset(y = yOffset)
            .size(dotSize)
            .clip(CircleShape)
            .background(color)
    )
}