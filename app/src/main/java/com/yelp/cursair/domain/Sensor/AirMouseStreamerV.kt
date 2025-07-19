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

class AirMouseStreamerV(
    context: Context,
    private val coroutineScope: CoroutineScope
) : SensorEventListener {

    companion object {
        private const val PACKET_INTERVAL_MS = 16L          // ≈60 Hz

        // ΔX : GYROSCOPE YAW (horizontal movement)
        private const val GYRO_YAW_SENS = 900f
        private const val GYRO_YAW_ALPHA = 0.20f
        private const val GYRO_YAW_POW = 1.7f
        private const val GYRO_CAL_N = 100
        private const val GYRO_CAL_VAR_MAX = 1e-5f

        // ΔY : GYROSCOPE PITCH (vertical movement)
        private const val GYRO_PITCH_SENS = 900f
        private const val GYRO_PITCH_ALPHA = 0.20f
        private const val GYRO_PITCH_POW = 1.7f
    }

    // Android sensors
    private val sensorMgr =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val gyroSensor =
        sensorMgr.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    // Streaming
    private var streamJob: Job? = null
    private var sensorsRegistered = false

    // DX (gyro yaw - horizontal)
    private var smoothYaw = 0f
    private var yawDrift = 0f
    private val yawBuf = mutableListOf<Float>()
    @Volatile private var currentDx = 0

    // DY (gyro pitch - vertical)
    private var smoothPitch = 0f
    private var pitchDrift = 0f
    private val pitchBuf = mutableListOf<Float>()
    @Volatile private var currentDy = 0

    private var screenHeight = 0
    private var screenWidth = 0

    init {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val displayMetrics = context.resources.displayMetrics
        screenHeight = displayMetrics.heightPixels
        screenWidth = displayMetrics.widthPixels
        Log.d("CursAir", "Initialized - Screen: ${screenWidth}x${screenHeight}")
    }

    fun startStreaming() {
        if (streamJob?.isActive == true) return
        if (gyroSensor == null) {
            Log.e("CursAir", "Gyroscope sensor unavailable.")
            return
        }
        resetState()
        if (!sensorsRegistered) {
            val rate = SensorManager.SENSOR_DELAY_GAME
            sensorMgr.registerListener(this, gyroSensor, rate)
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
        // Reset drift buffers to recalibrate both axes
        yawBuf.clear()
        pitchBuf.clear()
        yawDrift = 0f
        pitchDrift = 0f
        Log.d("CursAir", "Calibrated gyroscope drift")
    }

    override fun onSensorChanged(event: SensorEvent) = when (event.sensor.type) {
        Sensor.TYPE_GYROSCOPE -> {
            handleGyroYaw(event.values[2])    // yaw (horizontal)
            handleGyroPitch(event.values[0])  // pitch (vertical)
        }
        else -> Unit
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    // ΔX: gyroscope yaw (horizontal movement)
    private fun handleGyroYaw(rawYaw: Float) {
        yawBuf += rawYaw
        if (yawBuf.size >= GYRO_CAL_N) {
            if (yawBuf.variance() < GYRO_CAL_VAR_MAX)
                yawDrift = yawBuf.average().toFloat()
            yawBuf.clear()
        }
        val corrected = rawYaw - yawDrift
        smoothYaw = GYRO_YAW_ALPHA * corrected + (1 - GYRO_YAW_ALPHA) * smoothYaw
        val curved = smoothYaw.signPow(GYRO_YAW_POW)
        currentDx = (-curved * GYRO_YAW_SENS).toInt()
    }

    // ΔY: gyroscope pitch (vertical movement)
    private fun handleGyroPitch(rawPitch: Float) {
        pitchBuf += rawPitch
        if (pitchBuf.size >= GYRO_CAL_N) {
            if (pitchBuf.variance() < GYRO_CAL_VAR_MAX)
                pitchDrift = pitchBuf.average().toFloat()
            pitchBuf.clear()
        }
        val corrected = rawPitch - pitchDrift
        smoothPitch = GYRO_PITCH_ALPHA * corrected + (1 - GYRO_PITCH_ALPHA) * smoothPitch
        val curved = smoothPitch.signPow(GYRO_PITCH_POW)
        currentDy = (-curved * GYRO_PITCH_SENS).toInt() // Fixed: Added negative sign to reverse direction
    }

    private fun resetState() {
        // Reset yaw (horizontal)
        smoothYaw = 0f
        yawDrift = 0f
        yawBuf.clear()
        currentDx = 0

        // Reset pitch (vertical)
        smoothPitch = 0f
        pitchDrift = 0f
        pitchBuf.clear()
        currentDy = 0

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
