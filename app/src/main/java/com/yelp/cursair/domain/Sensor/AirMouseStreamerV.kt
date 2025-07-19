package com.yelp.cursair.domain.Sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
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
        private const val PACKET_INTERVAL_MS = 16L // ≈60 Hz

        // DX (gyro yaw)
        private const val GYRO_SENS = 900f
        private const val GYRO_ALPHA = 0.20f
        private const val GYRO_POW = 1.7f
        private const val GYRO_CAL_N = 100
        private const val GYRO_CAL_VAR_MAX = 1e-5f

        // DY (pitch velocity)
        private const val DY_SENS = 900f         // Vertical sensitivity
        private const val DY_ALPHA = 0.20f       // Like dx for similar feel
        private const val DY_POW = 1.7f
        private const val DY_DEADZONE = 0.016f   // ≈0.91°
    }

    private val sensorMgr = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val gyroSensor = sensorMgr.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private val rotSensor = sensorMgr.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    private var streamJob: Job? = null
    private var sensorsRegistered = false

    // DX (gyro yaw, velocity)
    private var smoothYaw = 0f
    private var yawDrift = 0f
    private val yawBuf = mutableListOf<Float>()
    @Volatile private var currentDx = 0

    // DY (rotation-vector pitch, velocity)
    private val rotationMatrix = FloatArray(9)
    private val orientation = FloatArray(3)
    private var prevPitch = Float.NaN
    private var smoothPitchDelta = 0f
    @Volatile private var currentDy = 0

    fun startStreaming() {
        if (streamJob?.isActive == true) return
        if (gyroSensor == null || rotSensor == null) {
            Log.e("CursAir", "Required sensors unavailable.")
            return
        }
        resetState()
        if (!sensorsRegistered) {
            val rate = SensorManager.SENSOR_DELAY_GAME
            sensorMgr.registerListener(this, gyroSensor, rate)
            sensorMgr.registerListener(this, rotSensor, rate)
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

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_GYROSCOPE -> handleGyro(event.values[2])
            Sensor.TYPE_ROTATION_VECTOR -> handleRotation(event.values)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    // DX: Gyro (yaw velocity with drift cancellation)
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

    // DY: Rotation-vector (pitch velocity, filtered, with deadzone and curve)
    private fun handleRotation(vec: FloatArray) {
        SensorManager.getRotationMatrixFromVector(rotationMatrix, vec)
        SensorManager.getOrientation(rotationMatrix, orientation)
        val pitch = orientation[1] // radians (nose-up positive)
        if (prevPitch.isNaN()) {
            prevPitch = pitch
            return
        }
        var delta = pitch - prevPitch
        prevPitch = pitch

        // Deadzone for dy (independent of dx)
        if (abs(delta) < DY_DEADZONE) delta = 0f

        smoothPitchDelta = DY_ALPHA * delta + (1 - DY_ALPHA) * smoothPitchDelta
        val curved = smoothPitchDelta.signPow(DY_POW)
        currentDy = (curved * DY_SENS).toInt()
    }

    private fun resetState() {
        smoothYaw = 0f; yawDrift = 0f; yawBuf.clear(); currentDx = 0
        prevPitch = Float.NaN; smoothPitchDelta = 0f; currentDy = 0
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
