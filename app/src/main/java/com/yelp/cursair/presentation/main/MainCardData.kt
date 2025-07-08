package com.yelp.cursair.presentation.main

data class CardData(
    val title: String,
    val description: String,
    val connectionState: String,
    val buttonText: String
)

// The card for the initial "Scan" state (Page 0)
val initialPageCard = CardData(
    title = "Scan\nQR Code",
    description = "Open Cursair on your laptop.\nLet's get you connected.",
    connectionState = "NO CONNECTION",
    buttonText = "Scan"
)

// The card for the "Connected" success state (Result of Page 0)
val successCard = CardData(
    title = "Devices\nConnected",
    description = "Place your phone on a flat surface.",
    connectionState = "CONNECTED",
    buttonText = "Start"
)

// The card for the "Connection Failed" state (Result of Page 0)
val failureCard = CardData(
    title = "Connection\nFailed",
    description = "Please ensure your laptop is on the same network and try again.",
    connectionState = "NOT CONNECTED",
    buttonText = "Try Again"
)