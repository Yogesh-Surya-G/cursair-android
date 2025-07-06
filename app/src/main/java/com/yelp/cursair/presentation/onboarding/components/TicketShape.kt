package com.yelp.cursair.presentation.onboarding.components

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/**
 * A custom Shape class that creates the "ticket" path with side cutouts.
 * This can be used for clipping, backgrounds, borders, etc.
 */
class TicketShape(
    private val cornerRadius: Dp = 16.dp,
    private val cutoutRadius: Dp = 25.dp
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val cornerRadiusPx = with(density) { cornerRadius.toPx() }
        val cutoutRadiusPx = with(density) { cutoutRadius.toPx() }

        // The path logic is copied directly from your drawWithCache block
        val path = Path().apply {
            reset()
            moveTo(cornerRadiusPx, 0f)
            lineTo(size.width - cornerRadiusPx, 0f)
            arcTo(Rect(size.width - 2 * cornerRadiusPx, 0f, size.width, 2 * cornerRadiusPx), -90f, 90f, false)
            lineTo(size.width, size.height / 2 - cutoutRadiusPx)
            arcTo(
                Rect(
                    size.width - cutoutRadiusPx,
                    size.height / 2 - cutoutRadiusPx,
                    size.width + cutoutRadiusPx,
                    size.height / 2 + cutoutRadiusPx
                ), -90f, -180f, false)
            lineTo(size.width, size.height - cornerRadiusPx)
            arcTo(
                Rect(
                    size.width - 2 * cornerRadiusPx,
                    size.height - 2 * cornerRadiusPx,
                    size.width,
                    size.height
                ), 0f, 90f, false)
            lineTo(cornerRadiusPx, size.height)
            arcTo(Rect(0f, size.height - 2 * cornerRadiusPx, 2 * cornerRadiusPx, size.height), 90f, 90f, false)
            lineTo(0f, size.height / 2 + cutoutRadiusPx)
            arcTo(
                Rect(
                    -cutoutRadiusPx,
                    size.height / 2 - cutoutRadiusPx,
                    cutoutRadiusPx,
                    size.height / 2 + cutoutRadiusPx
                ), 90f, -180f, false)
            lineTo(0f, cornerRadiusPx)
            arcTo(Rect(0f, 0f, 2 * cornerRadiusPx, 2 * cornerRadiusPx), 180f, 90f, false)
            close()
        }
        return Outline.Generic(path)
    }
}