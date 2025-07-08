package com.yelp.cursair.presentation.common

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import com.yelp.cursair.R

@Composable
fun CursairLogoIcon(modifier: Modifier = Modifier, tint: Color) {
    Icon(
        painter = painterResource(id = R.drawable.logo),
        contentDescription = null,
        modifier = modifier,
        tint = tint
    )
}

