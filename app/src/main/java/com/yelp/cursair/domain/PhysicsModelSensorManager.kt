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
 * The definitive hybrid sensor manager with an intelligent "Active Braking" physics model.
 * This version uses a stable physics simulation for vertical movement on a flat surface.
 *
 * - DX (Horizontal): Uses GYROSCOPE's Yaw for proven, smooth, rotational control.
 * - DY (Vertical): Uses a sophisticated and STABLE physics model for LINEAR ACCELERATION:
 *   1.  **Correct Integration:** Calculates true velocity from acceleration within a fixed-rate physics loop.
 *   2.  **Advanced Drift Correction:** Actively learns and cancels out sensor bias when the phone is stationary.
 *   3.  **Dual-Friction Model:** Applies gentle friction for gliding and strong friction for active braking.
 */
class PhysicsModelSensorManager(
    context: Context,
    private val coroutineScope: CoroutineScope
) : SensorEventListener {

    companion object {
        // --- CORE TIMING ---
        private const val PACKET_SEND_INTERVAL_MS = 16L // ~60Hz update rate
        private const val PHYSICS_TIME_STEP_S = PACKET_SEND_INTERVAL_MS / 1000.0f

        // --- GYROSCOPE (DX) TUNING ---
        private const val SENSITIVITY_X = 900.0f
        private const val GYRO_LOW_PASS_ALPHA = 0.2f
        private const val GYRO_RESPONSE_CURVE_POWER = 1.7f
        private const val GYRO_DRIFT_CALIBRATION_SAMPLES = 100
        private const val GYRO_DRIFT_CALIBRATION_THRESHOLD = 0.00001f

        // --- LINEAR ACCELERATION (DY) PHYSICS & TUNING ---
        private const val SENSITIVITY_Y = 1200.0f // Scales final velocity to cursor movement
        private const val PASSIVE_GLIDING_FRICTION = 0.99f // Closer to 1.0 = less friction.
        private const val ACTIVE_BRAKING_FRICTION = 0.9f  // Lower = stronger braking.
        private const val ACCEL_DEADZONE_THRESHOLD = 0.05f // Ignore tiny, noisy acceleration.
        private const val VELOCITY_SNAP_THRESHOLD = 0.001f // Snap to 0 to prevent micro-drifting.
        private const val ACCEL_DRIFT_CALIBRATION_SAMPLES = 150
        private const val ACCEL_DRIFT_CALIBRATION_THRESHOLD = 0.001f
    }

    private var sensorManager: SensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private var gyroscopeSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private var linearAccelSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)

    private var isSensorRegistered = false
    private var packetSenderJob: Job? = null

    // --- State for Gyroscope (DX) ---
    private var smoothedYaw: Float = 0f
    private var driftOffsetYaw: Float = 0f
    private val driftSamplesYaw = mutableListOf<Float>()
    @Volatile private var currentDx: Int = 0

    // --- State for Physics-Based Acceleration (DY) ---
    private var velocityDy: Float = 0f
    @Volatile private var latestCorrectedAccelY: Float = 0f
    private var driftOffsetAccelY: Float = 0f
    private val driftSamplesAccelY = mutableListOf<Float>()
    @Volatile private var currentDy: Int = 0

    fun startStreaming() {
        if (packetSenderJob?.isActive == true) return
        if (gyroscopeSensor == null || linearAccelSensor == null) {
            Log.e("HybridMouseStreamer", "Required sensors not available.")
            return
        }
        resetState()
        if (!isSensorRegistered) {
            val rate = SensorManager.SENSOR_DELAY_GAME
            sensorManager.registerListener(this, gyroscopeSensor, rate)
            sensorManager.registerListener(this, linearAccelSensor, rate)
            isSensorRegistered = true
        }

        packetSenderJob = coroutineScope.launch {
            Log.i("HybridMouseStreamer", "Starting intelligent physics-based mouse stream.")
            while (isActive) {
                // Perform all DY physics calculations here, at a stable rate.
                processPhysicsTick()

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

    /**
     * This function runs at a fixed interval and is the heart of the DY physics simulation.
     */
    private fun processPhysicsTick() {
        // 1. Integrate acceleration to get velocity (v = u + at).
        velocityDy += latestCorrectedAccelY * PHYSICS_TIME_STEP_S

        // 2. Determine user intent and apply the correct friction model.
        val isActivelyAccelerating = abs(latestCorrectedAccelY) > 0f
        val isBraking = sign(latestCorrectedAccelY) != sign(velocityDy) && !isActivelyAccelerating
        val isGliding = !isActivelyAccelerating && !isBraking

        val frictionFactor = when {
            isBraking -> ACTIVE_BRAKING_FRICTION // Strong friction to stop fast
            isGliding -> PASSIVE_GLIDING_FRICTION // Gentle friction for smooth coasting
            else -> 1.0f // No friction if actively accelerating
        }
        velocityDy *= frictionFactor

        // 3. Snap-to-zero to prevent infinitesimal drift.
        if (abs(velocityDy) < VELOCITY_SNAP_THRESHOLD && !isActivelyAccelerating) {
            velocityDy = 0f
        }

        // 4. Convert the final velocity into a delta movement for the cursor.
        // Moving phone away from you (positive accel) moves cursor up (traditionally negative dy).
        currentDy = (-velocityDy * SENSITIVITY_Y).toInt()
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
                val rawAccelY = event.values[1] // Y-axis for forward/backward movement
                updateAccelDriftCorrection(rawAccelY)
                val correctedAccelY = rawAccelY - driftOffsetAccelY

                // CRITICAL FIX: Only CAPTURE the latest acceleration here.
                // Do NOT integrate here. All physics happen in the fixed-rate loop.
                latestCorrectedAccelY = if (abs(correctedAccelY) > ACCEL_DEADZONE_THRESHOLD) correctedAccelY else 0f
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
            val isSensorStable = driftSamplesAccelY.variance() < ACCEL_DRIFT_CALIBRATION_THRESHOLD
            val isDeviceAtRest = abs(velocityDy) < VELOCITY_SNAP_THRESHOLD

            if (isSensorStable && isDeviceAtRest) {
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
            put("event","stream")
            put("dx", dx)
            put("dy", dy)
        }.toString()
    }
}