package com.yelp.cursair.domain

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
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
 * An advanced sensor manager that streams cursor movement data using the phone's gyroscope.
 *
 * This class implements a full processing pipeline:
 * 1.  **Dynamic Drift Correction:** Automatically detects when the phone is stationary
 *     to correct for inherent sensor drift, ensuring the cursor remains stable.
 * 2.  **Low-Pass Filter:** Smooths out high-frequency sensor "jitter" for fluid, non-jumpy
 *     cursor movement.
 * 3.  **Non-Linear Response Curve:** Provides fine-grained control for small, precise
 *     movements while allowing for rapid, sweeping motions, creating a natural feel.
 *
 * @param context The application context, used to get the SensorManager system service.
 * @param coroutineScope A CoroutineScope from the caller (e.g., a ViewModel or Composable)
 *                       to manage the lifecycle of the streaming job.
 */
class AdvancedGyroscopeMouseStreamer(
    context: Context,
    private val coroutineScope: CoroutineScope
) : SensorEventListener {

    // --- Companion Object for Tunable Parameters ---
    companion object {
        // --- PRIMARY TUNING ---
        // Higher value = faster cursor. Tune this after other parameters are set.
        private const val SENSITIVITY_X = 900.0f
        private const val SENSITIVITY_Y = 1200.0f // Vertical motion (roll) is often more subtle, so it might need a higher sensitivity.


        // Closer to 0.0 = more smoothing, but more "lag". A good range is 0.1 to 0.3.
        private const val LOW_PASS_ALPHA = 0.2f
        // An exponent > 1.0 creates a deadzone and accelerates cursor with larger movements.
        // A good starting value is 1.5 to 2.0.
        private const val RESPONSE_CURVE_POWER = 2.0f

        // --- DRIFT CORRECTION TUNING (Advanced) ---
        // Number of samples to analyze to detect if the device is stationary.
        private const val DRIFT_CALIBRATION_SAMPLES = 100
        // If the variance of the last N samples is below this threshold, we consider
        // the device stationary and recalibrate the drift.
        private const val DRIFT_CALIBRATION_THRESHOLD = 0.00001f
    }

    private var sensorManager: SensorManager
    private var gyroscopeSensor: Sensor? = null
    private var isSensorRegistered = false
    private var streamingJob: Job? = null

    // State variables for the filters and corrections
    private var smoothedYaw: Float = 0f
    private var smoothedRoll: Float = 0f
    private var driftOffsetYaw: Float = 0f
    private var driftOffsetRoll: Float = 0f
    private val driftSamplesYaw = mutableListOf<Float>()
    private val driftSamplesRoll = mutableListOf<Float>()





    private val movementChannel = Channel<Movement>(Channel.CONFLATED)

    init {
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    }

    /**
     * Starts listening to the gyroscope and streaming movement data.
     * This method is idempotent; calling it while already streaming has no effect.
     */
    fun startStreaming() {
        if (streamingJob?.isActive == true) {
            Log.d("AdvancedGyroscope", "Streaming is already active.")
            return
        }
        if (gyroscopeSensor == null) {
            Log.e("AdvancedGyroscope", "Gyroscope sensor not available on this device.")
            return
        }
        resetState() // Ensure a clean state every time streaming starts
        if (!isSensorRegistered) {
            sensorManager.registerListener(this, gyroscopeSensor, SensorManager.SENSOR_DELAY_GAME)
            isSensorRegistered = true
        }
        streamingJob = coroutineScope.launch {
            Log.i("AdvancedGyroscope", "Starting advanced cursor movement stream.")
            movementChannel.receiveAsFlow().collect { movement ->
                if (!isActive) return@collect
                try {
                    ConnectionManager.sendMessage(formatMovementToJsonString(movement.dx, movement.dy))
                } catch (e: Exception) {
                    Log.e("AdvancedGyroscope", "Error sending movement data", e)
                }
            }
        }
    }

    /**
     * Stops the gyroscope listener and cancels the streaming job to conserve battery.
     */
    fun stopStreaming() {
        Log.i("AdvancedGyroscope", "Stopping cursor movement stream.")
        streamingJob?.cancel()
        streamingJob = null
        if (isSensorRegistered) {
            sensorManager.unregisterListener(this)
            isSensorRegistered = false
        }
    }

    private fun resetState() {
        smoothedYaw = 0f
        driftOffsetYaw = 0f
        driftSamplesYaw.clear()
        driftOffsetRoll = 0f // Add this
        driftSamplesRoll.clear() // Add this
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (streamingJob?.isActive != true || event?.sensor?.type != Sensor.TYPE_GYROSCOPE) return




        val rawRoll = event.values[0]  // Rotation around X-axis (tilting left/right edge up/down)
//        val rawPitch = event.values[1] // Rotation around Y-axis (tilting top/bottom edge up/down)
        val rawYaw = event.values[2]   // Rotation around Z-axis (twisting flat)

//
//        // --- Step 1: Dynamic Drift Correction ---
//        updateDriftCorrection(rawYaw, rawPitch)
//        val correctedYaw = rawYaw - driftOffsetYaw
//        val correctedPitch = rawPitch - driftOffsetPitch


        // --- DX CALCULATION (DO NOT CHANGE) ---
        // This part is working perfectly.
        updateDriftCorrection(rawYaw, rawRoll) // Updated to pass all 3 axes
        val correctedYaw = rawYaw - driftOffsetYaw
        val smoothedYaw = LOW_PASS_ALPHA * correctedYaw + (1 - LOW_PASS_ALPHA) * smoothedYaw
        val finalYaw = smoothedYaw.applyResponseCurve()
        val dx = (-finalYaw * SENSITIVITY_X).toInt()


//        // --- Step 2: Apply Low-Pass Filter for Smoothing ---
//        smoothedYaw = LOW_PASS_ALPHA * correctedYaw + (1 - LOW_PASS_ALPHA) * smoothedYaw
//        smoothedPitch = LOW_PASS_ALPHA * correctedPitch + (1 - LOW_PASS_ALPHA) * smoothedPitch
//
//        // --- Step 3: Apply Non-Linear Response Curve for Natural Feel ---
//        val finalYaw = smoothedYaw.applyResponseCurve()
//        val finalPitch = smoothedPitch.applyResponseCurve()
//
//        // --- Step 4: Final Mapping to Cursor Movement ---
//        // Invert yaw for natural right/left movement (clockwise twist -> move right).
//        val dx = (-finalYaw * SENSITIVITY).toInt()
//        // Tilting phone forward (negative pitch) should move cursor up (negative dy), so no inversion needed.
//        val dy = (finalPitch * SENSITIVITY).toInt()
        // --- Step 4: Process DY (Vertical Movement) ---
        // This is the new, correct logic for flat surface movement.
        val correctedRoll = rawRoll - driftOffsetRoll
        smoothedRoll = LOW_PASS_ALPHA * correctedRoll + (1 - LOW_PASS_ALPHA) * smoothedRoll
        val finalRoll = smoothedRoll.applyResponseCurve()
        // Pushing forward causes a positive roll, which should move cursor up (negative dy).
        val dy = (-finalRoll * SENSITIVITY_Y).toInt()

        movementChannel.trySend(Movement(dx, dy))
    }


    /**
     * Analyzes recent sensor data to detect when the device is stationary and
     * calculates the drift offset to be subtracted from future readings.
     */
    private fun updateDriftCorrection(rawYaw: Float,rawRoll: Float) {
        driftSamplesYaw.add(rawYaw)
//        driftSamplesPitch.add(rawPitch)

        driftSamplesRoll.add(rawRoll) // Add this


//        if (driftSamplesYaw.size >= DRIFT_CALIBRATION_SAMPLES) {
//            if (driftSamplesYaw.variance() < DRIFT_CALIBRATION_THRESHOLD) {
//                driftOffsetYaw = driftSamplesYaw.average().toFloat()
//                Log.d("AdvancedGyroscope", "Recalibrated Yaw drift offset to $driftOffsetYaw")
//            }
//            if (driftSamplesPitch.variance() < DRIFT_CALIBRATION_THRESHOLD) {
//                driftOffsetPitch = driftSamplesPitch.average().toFloat()
//                Log.d("AdvancedGyroscope", "Recalibrated Pitch drift offset to $driftOffsetPitch")
//            }
//            // Clear lists to start collecting a new batch of samples
//            driftSamplesYaw.clear()
//            driftSamplesPitch.clear()
//        }

        if (driftSamplesYaw.size >= DRIFT_CALIBRATION_SAMPLES) {
            // Yaw (for dx)
            if (driftSamplesYaw.variance() < DRIFT_CALIBRATION_THRESHOLD) {
                driftOffsetYaw = driftSamplesYaw.average().toFloat()
                Log.d("AdvancedGyroscope", "Recalibrated Yaw drift to $driftOffsetYaw")
            }
            // Roll (for dy)
            if (driftSamplesRoll.variance() < DRIFT_CALIBRATION_THRESHOLD) {
                driftOffsetRoll = driftSamplesRoll.average().toFloat()
                Log.d("AdvancedGyroscope", "Recalibrated Roll drift to $driftOffsetRoll")
            }
            // Clear lists to start collecting a new batch of samples
            driftSamplesYaw.clear()
            driftSamplesRoll.clear()
        }
    }




    // Extension function to apply the power curve, creating a more responsive feel.
    private fun Float.applyResponseCurve(): Float {
        return sign(this) * abs(this).pow(RESPONSE_CURVE_POWER)
    }

    // Extension function to calculate variance, needed for drift detection.
    private fun List<Float>.variance(): Float {
        if (this.size < 2) return 0f
        val mean = this.average().toFloat()
        return this.map { (it - mean).pow(2) }.average().toFloat()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not critical for this use case, but can be used for logging if needed.
    }

    private fun formatMovementToJsonString(dx: Int, dy: Int): String {
        return JSONObject().apply {
            put("dx", dx)
            put("dy", dy)
        }.toString()
    }
}