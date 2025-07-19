package com.yelp.cursair.domain.Sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import android.view.WindowManager
import com.yelp.cursair.domain.ConnectionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.max
import kotlin.math.min



class AirMouseStreamer(
    context: Context,
    private val coroutineScope: CoroutineScope
) : SensorEventListener {

    companion object {
        private const val PACKET_INTERVAL_MS = 16L          // ≈60 Hz
        private const val DT = PACKET_INTERVAL_MS / 1000f

        // ΔX : GYROSCOPE
        private const val GYRO_SENS = 900f
        private const val GYRO_ALPHA = 0.20f
        private const val GYRO_POW = 1.7f
        private const val GYRO_CAL_N = 100
        private const val GYRO_CAL_VAR_MAX = 1e-5f

        // ΔY : ABSOLUTE PITCH POSITIONING (custom mapped)
        private const val PITCH_RANGE_RAD = 0.6f           // ±0.6 radians
        private const val PITCH_SENSITIVITY = 0.12f        // Overall speed multiplier
        private const val PITCH_ALPHA = 0.25f              // Smoothing, natural feel
        private const val PITCH_POW = 1.8f                 // Response curve: 1=linear, >1=gentle
        private const val PITCH_DEADZONE_RAD = 0.05f       // No motion for small tilts (~3°)
        private const val PITCH_DOWNWARD_SKEW = 1.25f      // Easier to push down
        private const val CHANGE_SENSITIVITY = 0.08f
        private const val STANDARD_SCREEN_WIDTH = 720f
    }

    // Android sensors
    private val sensorMgr =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val gyroSensor =
        sensorMgr.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private val accelSensor =
        sensorMgr.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magnetSensor =
        sensorMgr.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    // Streaming
    private var streamJob: Job? = null
    private var sensorsRegistered = false

    // DX (gyro)
    private var smoothYaw = 0f
    private var yawDrift = 0f
    private val yawBuf = mutableListOf<Float>()
    @Volatile private var currentDx = 0

    // DY (absolute, filtered, nonlinear pitch mapping)
    private val rotationMatrix = FloatArray(9)
    private val orientationValues = FloatArray(3)
    private var gravity = FloatArray(3)
    private var geomagnetic = FloatArray(3)
    private var pitch = 0f
    private var pitchOffset = 0f
    private var smoothedPitch = 0f
    private var smoothedPitchOutput = 0f
    private var screenHeight = 0
    private var screenWidth = 0
    @Volatile private var currentDy = 0

    init {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val displayMetrics = context.resources.displayMetrics
        screenHeight = displayMetrics.heightPixels
        screenWidth = displayMetrics.widthPixels
        Log.d("CursAir", "Initialized - Screen: ${screenWidth}x${screenHeight}")
    }

    fun startStreaming() {
        if (streamJob?.isActive == true) return
        if (gyroSensor == null || accelSensor == null || magnetSensor == null) {
            Log.e("CursAir", "Required sensors unavailable.")
            return
        }
        resetState()
        if (!sensorsRegistered) {
            val rate = SensorManager.SENSOR_DELAY_GAME
            sensorMgr.registerListener(this, gyroSensor, rate)
            sensorMgr.registerListener(this, accelSensor, rate)
            sensorMgr.registerListener(this, magnetSensor, rate)
            sensorsRegistered = true
        }
        streamJob = coroutineScope.launch {
            while (isActive) {
                try {
                    ConnectionManager.sendMessage(
                        movementToJson(currentDx, currentDy)
                    )
                } catch (e: Exception) {
                    Log.e("CursAir", "Socket error", e)
                }
                delay(PACKET_INTERVAL_MS)
            }
        }
    }

    fun stopStreaming() {
        streamJob?.cancel(); streamJob = null
        if (sensorsRegistered) {
            sensorMgr.unregisterListener(this)
            sensorsRegistered = false
        }
    }

    fun calibrate() {
        pitchOffset = pitch
        smoothedPitch = 0f
        smoothedPitchOutput = 0f
        Log.d("CursAir", "Calibrated pitch offset: $pitchOffset")
    }

    override fun onSensorChanged(event: SensorEvent) = when (event.sensor.type) {
        Sensor.TYPE_GYROSCOPE         -> handleGyro(event.values[2])          // yaw
        Sensor.TYPE_ACCELEROMETER     -> handleAccelerometer(event.values)
        Sensor.TYPE_MAGNETIC_FIELD    -> handleMagnetometer(event.values)
        else -> Unit
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    // ΔX: gyroscope yaw
    private fun handleGyro(rawYaw: Float) {
        yawBuf += rawYaw
        if (yawBuf.size >= GYRO_CAL_N) {
            if (yawBuf.variance() < GYRO_CAL_VAR_MAX)
                yawDrift = yawBuf.average().toFloat()
            yawBuf.clear()
        }
        val corrected = rawYaw - yawDrift
        smoothYaw = GYRO_ALPHA * corrected + (1 - GYRO_ALPHA) * smoothYaw
        val curved = smoothYaw.signPow(GYRO_POW)
        currentDx = (-curved * GYRO_SENS).toInt()
    }

    // ΔY: accelerometer data
    private fun handleAccelerometer(values: FloatArray) {
        gravity = lowPass(values.clone(), gravity)
        updateOrientation()
    }

    // ΔY: magnetometer data
    private fun handleMagnetometer(values: FloatArray) {
        geomagnetic = lowPass(values.clone(), geomagnetic)
        updateOrientation()
    }

    // DY: Absolute pitch, filtered, non-linear, ergonomic
    private fun updateOrientation() {
        if (gravity.isNotEmpty() && geomagnetic.isNotEmpty()
            && !gravity.all { it == 0f } && !geomagnetic.all { it == 0f }) {
            val success = SensorManager.getRotationMatrix(rotationMatrix, null, gravity, geomagnetic)
            if (success) {
                SensorManager.getOrientation(rotationMatrix, orientationValues)
                pitch = orientationValues[1] // pitch in radians
                // Calibration offset (neutral position)
                var adjustedPitch = pitch - pitchOffset
                // Deadzone
                if (abs(adjustedPitch) < PITCH_DEADZONE_RAD) {
                    adjustedPitch = 0f
                }
                // First filter
                smoothedPitch = PITCH_ALPHA * adjustedPitch + (1 - PITCH_ALPHA) * smoothedPitch
                // Clamp to usable tilt range
                val clampedPitch = max(-PITCH_RANGE_RAD, min(PITCH_RANGE_RAD, smoothedPitch))
                // Normalize to [-1, 1]
                var normalizedPitch = clampedPitch / PITCH_RANGE_RAD
                // Asymmetric mapping: easier to push cursor down (device nose-forward)
                if (normalizedPitch > 0)
                    normalizedPitch *= PITCH_DOWNWARD_SKEW
                // Natural (power) curve, like horizontal movement
                val curved = normalizedPitch.sign * abs(normalizedPitch).pow(PITCH_POW)
                // Filter again for smooth movement and edge stopping
                smoothedPitchOutput = PITCH_ALPHA * curved + (1 - PITCH_ALPHA) * smoothedPitchOutput
                // Scale to screen height
                currentDy = (smoothedPitchOutput * screenHeight / 2 * PITCH_SENSITIVITY).toInt()
            }
        }
    }

    private fun lowPass(input: FloatArray, previousOutput: FloatArray): FloatArray {
        if (previousOutput.isEmpty() || previousOutput.all { it == 0f }) {
            return input
        }
        for (i in input.indices) {
            previousOutput[i] = previousOutput[i] + CHANGE_SENSITIVITY * (input[i] - previousOutput[i])
        }
        return previousOutput
    }

    private fun resetState() {
        smoothYaw = 0f; yawDrift = 0f; yawBuf.clear(); currentDx = 0
        pitch = 0f; pitchOffset = 0f; smoothedPitch = 0f; smoothedPitchOutput = 0f; currentDy = 0
        gravity = FloatArray(3); geomagnetic = FloatArray(3)
        Log.d("CursAir", "State reset")
    }

    private fun movementToJson(dx: Int, dy: Int): String =
        JSONObject().apply {
            put("event", "stream")
            put("dx", dx)
            put("dy", dy)
        }.toString()

    private fun Float.signPow(p: Float) = sign(this) * abs(this).pow(p)

    private fun List<Float>.variance(): Float {
        if (size < 2) return 0f
        val m = average().toFloat()
        return map { (it - m).pow(2) }.average().toFloat()
    }
}
