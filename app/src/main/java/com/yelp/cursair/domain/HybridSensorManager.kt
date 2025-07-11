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
//data class Movement(val dx: Int, val dy: Int)

/**
 * The definitive hybrid sensor manager with an intelligent "Active Braking" physics model.
 *
 * This version provides the most stable and intuitive "flat mouse" experience by solving
 * the "rebound" and "drift" problems inherent in using accelerometer data.
 *
 * - DX (Horizontal): Uses GYROSCOPE's Yaw for proven, smooth, rotational control.
 * - DY (Vertical): Uses a sophisticated physics model for LINEAR ACCELERATION:
 *   1.  **Integration:** Calculates true velocity from acceleration.
 *   2.  **Drift Correction:** Actively learns and cancels out sensor bias when the phone is still.
 *   3.  **Dual-Friction Model:** Applies gentle friction for gliding and strong friction for
 *       active braking, eliminating the "spring-back" effect.
 */
class HybridMouseStreamer(
    context: Context,
    private val coroutineScope: CoroutineScope
) : SensorEventListener {

    companion object {
        private const val PACKET_SEND_INTERVAL_MS = 16L // ~60Hz

        // --- GYROSCOPE (DX) TUNING ---
        private const val SENSITIVITY_X = 900.0f
        private const val GYRO_LOW_PASS_ALPHA = 0.2f
        private const val GYRO_RESPONSE_CURVE_POWER = 1.7f
        private const val GYRO_DRIFT_CALIBRATION_SAMPLES = 100
        private const val GYRO_DRIFT_CALIBRATION_THRESHOLD = 0.00001f

        // --- LINEAR ACCELERATION (DY) TUNING ---
        private const val SENSITIVITY_Y = 45.0f
        // Friction when gliding to a stop (user is not touching the phone).
        private const val PASSIVE_GLIDING_FRICTION = 0.96f
        // Strong friction when the user actively pushes against the direction of motion.
        private const val ACTIVE_BRAKING_FRICTION = 0.75f
        private const val ACCEL_DEADZONE_THRESHOLD = 0.05f
        private const val VELOCITY_SNAP_THRESHOLD = 0.001f
        private const val ACCEL_DRIFT_CALIBRATION_SAMPLES = 150
        private const val ACCEL_DRIFT_CALIBRATION_THRESHOLD = 0.001f
    }

    private var sensorManager: SensorManager
    private var gyroscopeSensor: Sensor? = null
    private var linearAccelSensor: Sensor? = null

    private var isSensorRegistered = false
    private var packetSenderJob: Job? = null

    // --- State for Gyroscope (DX) ---
    private var smoothedYaw: Float = 0f
    private var driftOffsetYaw: Float = 0f
    private val driftSamplesYaw = mutableListOf<Float>()

    // --- State for Physics-Based Acceleration (DY) ---
    @Volatile private var velocityDy: Float = 0f
    @Volatile private var latestCorrectedAccelY: Float = 0f
    private var driftOffsetAccelY: Float = 0f
    private val driftSamplesAccelY = mutableListOf<Float>()

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
            Log.e("HybridMouseStreamer", "Required sensors not available.")
            return
        }
        resetState()
        if (!isSensorRegistered) {
            sensorManager.registerListener(this, gyroscopeSensor, SensorManager.SENSOR_DELAY_GAME)
            sensorManager.registerListener(this, linearAccelSensor, SensorManager.SENSOR_DELAY_GAME)
            isSensorRegistered = true
        }

        packetSenderJob = coroutineScope.launch {
            Log.i("HybridMouseStreamer", "Starting intelligent physics-based mouse stream.")
            while (isActive) {
                // --- PHYSICS LOOP FOR DY ---
                val isBraking = sign(latestCorrectedAccelY) != sign(velocityDy) && velocityDy != 0f
                val friction = if (isBraking) ACTIVE_BRAKING_FRICTION else PASSIVE_GLIDING_FRICTION

                velocityDy *= friction
                if (abs(velocityDy) < VELOCITY_SNAP_THRESHOLD) {
                    velocityDy = 0f
                }
                currentDy = (-velocityDy * SENSITIVITY_Y).toInt()

                // --- PACKET SENDING ---
                val movementToSend = Movement(currentDx, currentDy)
                try {
                    ConnectionManager.sendMessage(formatMovementToJsonString(movementToSend.dx, movementToSend.dy))
                } catch (e: Exception) {
                    Log.e("HybridMouseStreamer", "Error sending movement data", e)
                }

                delay(PACKET_SEND_INTERVAL_MS)
            }
        }
    }

    fun stopStreaming() {
        Log.i("HybridMouseStreamer", "Stopping hybrid mouse stream.")
        packetSenderJob?.cancel()
        packetSenderJob = null
        if (isSensorRegistered) {
            sensorManager.unregisterListener(this)
            isSensorRegistered = false
        }
    }

    private fun resetState() {
        smoothedYaw = 0f; driftOffsetYaw = 0f; driftSamplesYaw.clear(); currentDx = 0
        velocityDy = 0f; driftOffsetAccelY = 0f; driftSamplesAccelY.clear(); currentDy = 0
        latestCorrectedAccelY = 0f
    }

    override fun onSensorChanged(event: SensorEvent?) {
        when (event?.sensor?.type) {
            Sensor.TYPE_GYROSCOPE -> {
                val rawYaw = event.values[2]
                updateGyroDriftCorrection(rawYaw)
                val correctedYaw = rawYaw - driftOffsetYaw
                smoothedYaw = GYRO_LOW_PASS_ALPHA * correctedYaw + (1 - GYRO_LOW_PASS_ALPHA) * smoothedYaw
                val finalYaw = smoothedYaw.applyResponseCurve(GYRO_RESPONSE_CURVE_POWER)
                currentDx = (-finalYaw * SENSITIVITY_X).toInt()
            }
            Sensor.TYPE_LINEAR_ACCELERATION -> {
                val rawAccelY = event.values[1]
                updateAccelDriftCorrection(rawAccelY)
                val correctedAccelY = rawAccelY - driftOffsetAccelY
                latestCorrectedAccelY = if (abs(correctedAccelY) > ACCEL_DEADZONE_THRESHOLD) correctedAccelY else 0f

                // Integrate: velocity = velocity + acceleration
                // No need for deltaTime here, as the fixed-rate physics loop handles timing.
                velocityDy += latestCorrectedAccelY
            }
        }
    }

    private fun updateGyroDriftCorrection(rawYaw: Float) {
        driftSamplesYaw.add(rawYaw)
        if (driftSamplesYaw.size >= GYRO_DRIFT_CALIBRATION_SAMPLES) {
            if (driftSamplesYaw.variance() < GYRO_DRIFT_CALIBRATION_THRESHOLD) {
                driftOffsetYaw = driftSamplesYaw.average().toFloat()
            }
            driftSamplesYaw.clear()
        }
    }

    private fun updateAccelDriftCorrection(rawAccelY: Float) {
        driftSamplesAccelY.add(rawAccelY)
        if (driftSamplesAccelY.size >= ACCEL_DRIFT_CALIBRATION_SAMPLES) {
            if (driftSamplesAccelY.variance() < ACCEL_DRIFT_CALIBRATION_THRESHOLD) {
                driftOffsetAccelY = driftSamplesAccelY.average().toFloat()
                Log.d("HybridMouseStreamer", "Recalibrated Accel-Y drift to $driftOffsetAccelY")
            }
            driftSamplesAccelY.clear()
        }
    }

    private fun Float.applyResponseCurve(power: Float): Float = sign(this) * abs(this).pow(power)

    private fun List<Float>.variance(): Float {
        if (this.size < 2) return 0f
        val mean = this.average().toFloat()
        return this.map { (it - mean).pow(2) }.average().toFloat()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun formatMovementToJsonString(dx: Int, dy: Int): String {
        return JSONObject().apply {
            put("dx", dx)
            put("dy", dy)
        }.toString()
    }
}