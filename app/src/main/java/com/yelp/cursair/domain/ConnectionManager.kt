package com.yelp.cursair.domain

import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * This is a mock business logic layer that simulates a network connection.
 * In a real app, this would handle the actual UDP socket connection.
 */
object ConnectionManager {

    /**
     * Attempts to connect using the data from the QR code.
     * @param qrData The raw string from the scanned QR code.
     * @return `true` if the connection was successful, `false` otherwise.
     */
    suspend fun connect(qrData: String): Boolean {
        // Simulate network latency
        delay(2500) // 2.5 seconds

        // In a real app, you would parse qrData and attempt the connection.
        // Here, we'll just randomly return true or false to test both UI paths.
        // For stable testing, you can just return true or false.
        // e.g., return qrData.contains("valid-cursair-token")
        return Random.nextBoolean()
    }
}