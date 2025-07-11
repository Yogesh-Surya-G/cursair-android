package com.yelp.cursair.domain

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sign

/**
 * A data class to hold the calculated 2D movement.
 */
data class Movement(val dx: Int, val dy: Int)

/**
 * The definitive hybrid sensor manager, engineered to provide the most intuitive and stable
 * "flat mouse" experience by implementing a "Snap-to-Zero" mechanism on the vertical axis.
 *
 * This implementation features:
 *
 * 1.  **DX (Horizontal):** A **Kalman Filter** on the GYROSCOPE for exceptionally smooth rotational control.
 *
 * 2.  **DY (Vertical):** A **Stillness Detection and Velocity Clamp** system built on top of the
 *     Kalman-filtered LINEAR ACCELERATOR. This model:
 *     - Uses responsive physics during motion.
 *     - Intelligently detects when the user has stopped moving the phone.
 *     - Forcefully clamps the vertical velocity to zero upon stopping, providing a crisp,
 *       definitive end to movement and completely eliminating any "spring-back" effect.
 */
class AdvancedHybridMouseStreamer(
    context: Context,
    private val coroutineScope: CoroutineScope
) : SensorEventListener {

    // A simple, self-contained 1D Kalman Filter implementation.
    private class KalmanFilter1D(private val R: Float, private val Q: Float) {
        private var x: Float = 0f; private var p: Float = 1f
        fun update(measurement: Float): Float {
            p += Q; val k = p / (p + R); x += k * (measurement - x); p *= (1 - k); return x
        }
        fun reset() { x = 0f; p = 1f }
    }

    companion object {
        private const val PACKET_SEND_INTERVAL_MS = 16L // ~60 FPS updates

        // --- GYROSCOPE (DX) TUNING ---
        private const val SENSITIVITY_X = 1000.0f
        private const val KALMAN_R_YAW = 0.5f; private const val KALMAN_Q_YAW = 0.01f
        private const val GYRO_RESPONSE_CURVE_POWER = 1.5f

        // --- LINEAR ACCELERATION (DY) TUNING ---
        private const val SENSITIVITY_Y = 350.0f
        private const val KALMAN_R_ACCEL_Y = 2.0f; private const val KALMAN_Q_ACCEL_Y = 0.05f
        private const val IDLE_DRAG_FACTOR = 0.90f
        private const val BRAKING_DAMPING_FACTOR = 0.60f
        private const val ACCEL_DEADZONE_THRESHOLD = 0.05f

        // --- STILLNESS DETECTION (The "Snap-to-Zero" Logic) ---
        // How many consecutive frames of stillness are needed to trigger a velocity clamp.
        private const val STILLNESS_FRAME_THRESHOLD = 8
        // A very tight threshold. If filtered acceleration is below this, we consider it "still".
        private const val STILLNESS_ACCEL_THRESHOLD = 0.01f
    }

    private var sensorManager: SensorManager
    private var gyroscopeSensor: Sensor? = null
    private var linearAccelSensor: Sensor? = null
    private var isSensorRegistered = false
    private var packetSenderJob: Job? = null

    private val yawKalmanFilter = KalmanFilter1D(R = KALMAN_R_YAW, Q = KALMAN_Q_YAW)
    private val accelYKalmanFilter = KalmanFilter1D(R = KALMAN_R_ACCEL_Y, Q = KALMAN_Q_ACCEL_Y)

    private var velocityY: Float = 0f
    private var stillnessCounter: Int = 0
    @Volatile private var latestFilteredAccelY: Float = 0f
    @Volatile private var currentDx: Int = 0
    @Volatile private var currentDy: Int = 0

    init {
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        linearAccelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
    }

    fun startStreaming() {
        if (packetSenderJob?.isActive == true) return
        if (gyroscopeSensor == null || linearAccelSensor == null) {
            Log.e("SnapToZeroHybrid", "Required sensors not available.")
            return
        }
        resetState()
        if (!isSensorRegistered) {
            sensorManager.registerListener(this, gyroscopeSensor, SensorManager.SENSOR_DELAY_GAME)
            sensorManager.registerListener(this, linearAccelSensor, SensorManager.SENSOR_DELAY_GAME)
            isSensorRegistered = true
        }

        packetSenderJob = coroutineScope.launch {
            Log.i("SnapToZeroHybrid", "Starting snap-to-zero hybrid mouse stream.")
            while (isActive) {
                val accel = latestFilteredAccelY

                // --- Physics for DY happens here ---
                if (sign(accel) != sign(velocityY) && velocityY != 0f) {
                    velocityY *= BRAKING_DAMPING_FACTOR
                } else {
                    velocityY += accel
                    velocityY *= IDLE_DRAG_FACTOR
                }

                // --- The "Snap-to-Zero" Logic ---
                // Check if the phone is being held still.
                if (abs(accel) < STILLNESS_ACCEL_THRESHOLD) {
                    stillnessCounter++
                } else {
                    // Any significant movement resets the counter.
                    stillnessCounter = 0
                }

                // If we've been still for long enough, forcefully clamp the velocity to zero.
                if (stillnessCounter >= STILLNESS_FRAME_THRESHOLD) {
                    velocityY = 0f
                }

                currentDy = (-velocityY * SENSITIVITY_Y).toInt()

                // Send the final packet
                try {
                    ConnectionManager.sendMessage(formatMovementToJsonString(currentDx, currentDy))
                } catch (e: Exception) {
                    Log.e("SnapToZeroHybrid", "Error sending movement data", e)
                }
                delay(PACKET_SEND_INTERVAL_MS)
            }
        }
    }

    fun stopStreaming() {
        packetSenderJob?.cancel()
        packetSenderJob = null
        if (isSensorRegistered) {
            sensorManager.unregisterListener(this)
            isSensorRegistered = false
        }
    }

    private fun resetState() {
        yawKalmanFilter.reset()
        accelYKalmanFilter.reset()
        velocityY = 0f
        stillnessCounter = 0
        latestFilteredAccelY = 0f
        currentDx = 0
        currentDy = 0
    }

    override fun onSensorChanged(event: SensorEvent?) {
        when (event?.sensor?.type) {
            Sensor.TYPE_GYROSCOPE -> {
                val rawYaw = event.values[2]
                val filteredYaw = yawKalmanFilter.update(rawYaw)
                val finalYaw = filteredYaw.applyResponseCurve(GYRO_RESPONSE_CURVE_POWER)
                currentDx = (-finalYaw * SENSITIVITY_X).toInt()
            }
            Sensor.TYPE_LINEAR_ACCELERATION -> {
                val rawY = event.values[1]
                val deadzonedY = if (abs(rawY) > ACCEL_DEADZONE_THRESHOLD) rawY else 0f
                latestFilteredAccelY = accelYKalmanFilter.update(deadzonedY)
            }
        }
    }

    private fun Float.applyResponseCurve(power: Float): Float {
        return sign(this) * abs(this).pow(power)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun formatMovementToJsonString(dx: Int, dy: Int): String {
        return JSONObject().apply {
            put("dx", dx)
            put("dy", dy)
        }.toString()
    }
}