package com.yelp.cursair.presentation.onboarding

data class CardData(
    val title : String,
    val description : String,
    val buttonText : String,
    val progress: Float,
)


val cards = listOf<CardData>(
    CardData(
        title = "Welcome to Cursair",
        description = "Your new mouse.\nLet's get you set up.",
        buttonText = "Get Started",
        progress = 0.3f,
    ),
    CardData(
        title = "Scan \nQR Code",
        description = "Open Cursair on your laptop.\nLet's get you connected.",
        buttonText = "Scan",
        progress = 0.6f,
    ),
    CardData(
        title = "Devices connected.",
        description = "Place the phone on a flat surface.\nEnjoy your new mouse.",
        buttonText = "Start",
        progress = 1.0f,
    )
)
