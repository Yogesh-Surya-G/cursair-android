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
// */
//data class Movement(val dx: Int, val dy: Int)

/**
 * An advanced, final implementation for a "flat mouse" experience using a hybrid sensor approach.
 *
 * This class solves two key problems with advanced techniques:
 *
 * 1.  **DX (Horizontal Control):** Uses a **Kalman Filter** on the GYROSCOPE's Yaw data.
 *     This provides superior noise reduction and drift compensation compared to simpler filters,
 *     resulting in extremely smooth and responsive rotational tracking.
 *
 * 2.  **DY (Vertical Control):** Implements an **Integration with Drag** model on the
 *     LINEAR_ACCELERATION data. This correctly interprets forward/backward sliding and
 *     eliminates the "spring-back" effect common with raw acceleration mapping.
 */
class KalmanMouseStreamer(
    context: Context,
    private val coroutineScope: CoroutineScope
) : SensorEventListener {

    // --- A simple, self-contained 1D Kalman Filter ---
    private class KalmanFilter1D(
        private val R: Float, // Measurement Noise: How much noise we think the sensor has.
        private val Q: Float  // Process Noise: How much we think the state varies between updates.
    ) {
        private var x: Float = 0f // The state estimate (our filtered value)
        private var p: Float = 1f // The uncertainty of our state estimate

        fun update(measurement: Float): Float {
            // Prediction Step
            p += Q

            // Update Step
            val k = p / (p + R) // Kalman Gain
            x += k * (measurement - x)
            p *= (1 - k)

            return x
        }

        fun reset() {
            x = 0f
            p = 1f
        }
    }

    // --- Companion Object for Tunable Parameters ---
    companion object {
        private const val PACKET_SEND_INTERVAL_MS = 16L // ~60 FPS updates

        // --- GYROSCOPE (DX) TUNING ---
        private const val SENSITIVITY_X = 1000.0f
        // Kalman Filter Tuning:
        // R (Measurement Noise): Higher means we trust the sensor less.
        // Q (Process Noise): Higher means we expect the true value to change more rapidly.
        private const val KALMAN_R_YAW = 0.5f  // Gyro is noisy, so R is relatively high.
        private const val KALMAN_Q_YAW = 0.01f // We expect yaw to be fairly stable, so Q is low.
        private const val GYRO_RESPONSE_CURVE_POWER = 1.5f

        // --- LINEAR ACCELERATION (DY) TUNING ---
        private const val SENSITIVITY_Y = 300.0f
        // Drag factor for the integration model. Closer to 1.0 = less friction.
        private const val ACCEL_DRAG_FACTOR = 0.85f
        private const val ACCEL_LOW_PASS_ALPHA = 0.15f // Still useful to pre-smooth acceleration
        private const val ACCEL_RESPONSE_CURVE_POWER = 1.3f
        private const val ACCEL_DEADZONE_THRESHOLD = 0.05f
    }

    private var sensorManager: SensorManager
    private var gyroscopeSensor: Sensor? = null
    private var linearAccelSensor: Sensor? = null
    private var isSensorRegistered = false
    private var packetSenderJob: Job? = null

    // --- State variables for Gyroscope (DX) ---
    private val yawKalmanFilter = KalmanFilter1D(R = KALMAN_R_YAW, Q = KALMAN_Q_YAW)

    // --- State variables for Linear Acceleration (DY) ---
    private var smoothedAccelY: Float = 0f
    private var velocityY: Float = 0f // The integrated velocity for dy

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
            Log.e("AdvancedHybrid", "Required sensors not available.")
            return
        }
        resetState()
        if (!isSensorRegistered) {
            sensorManager.registerListener(this, gyroscopeSensor, SensorManager.SENSOR_DELAY_GAME)
            sensorManager.registerListener(this, linearAccelSensor, SensorManager.SENSOR_DELAY_GAME)
            isSensorRegistered = true
        }

        packetSenderJob = coroutineScope.launch {
            Log.i("AdvancedHybrid", "Starting advanced hybrid mouse stream.")
            while (isActive) {
                // Apply drag to the dy velocity in the sender loop.
                // This ensures a constant decay over time.
                velocityY *= ACCEL_DRAG_FACTOR
                // If velocity becomes very small, snap it to zero to prevent tiny drifts.
                if (abs(velocityY) < 0.001f) {
                    velocityY = 0f
                }
                currentDy = (velocityY * SENSITIVITY_Y).toInt()

                // Send the latest calculated dx and the updated dy
                val movementToSend = Movement(currentDx, currentDy)
                try {
                    ConnectionManager.sendMessage(formatMovementToJsonString(movementToSend.dx, movementToSend.dy))
                } catch (e: Exception) {
                    Log.e("AdvancedHybrid", "Error sending movement data", e)
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
        smoothedAccelY = 0f
        velocityY = 0f
        currentDx = 0
        currentDy = 0
    }

    override fun onSensorChanged(event: SensorEvent?) {
        when (event?.sensor?.type) {
            Sensor.TYPE_GYROSCOPE -> {
                val rawYaw = event.values[2]
                // The Kalman filter replaces both the LPF and the drift correction logic for yaw.
                val filteredYaw = yawKalmanFilter.update(rawYaw)
                val finalYaw = filteredYaw.applyResponseCurve(GYRO_RESPONSE_CURVE_POWER)
                currentDx = (-finalYaw * SENSITIVITY_X).toInt()
            }
            Sensor.TYPE_LINEAR_ACCELERATION -> {
                val rawY = event.values[1]
                val deadzonedY = if (abs(rawY) > ACCEL_DEADZONE_THRESHOLD) rawY else 0f
                // We still smooth the raw acceleration before integrating it.
                smoothedAccelY = ACCEL_LOW_PASS_ALPHA * deadzonedY + (1 - ACCEL_LOW_PASS_ALPHA) * smoothedAccelY
                // Integrate the smoothed acceleration to update velocity.
                velocityY += smoothedAccelY
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