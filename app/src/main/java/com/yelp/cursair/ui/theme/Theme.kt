package com.yelp.cursair.ui.theme



import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors: ColorScheme = lightColorScheme(
    primary = AccentColor,
    onPrimary = LightSurface,
    background = LightBackground,
    onBackground = LightOnBg,
    surface = LightSurface,
    onSurface = LightOnBg,
    secondary = LightSecondary,
    onSecondary = LightSurface
)

private val DarkColors: ColorScheme = darkColorScheme(
    primary = AccentColor,
    onPrimary = DarkSurface,
    background = DarkBackground,
    onBackground = DarkOnBg,
    surface = DarkSurface,
    onSurface = DarkOnBg,
    secondary = DarkSecondary,
    onSecondary = DarkSurface
)


@Composable
fun CursairTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        DarkColors
    } else {
        LightColors
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = androidx.compose.material3.Typography(
            headlineLarge = AppType.Heading1,
            headlineMedium = AppType.Heading2,
            bodyLarge = AppType.Body,
            labelLarge = AppType.Button
        ),
        content = content
    )
}