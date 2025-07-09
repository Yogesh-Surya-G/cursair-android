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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject

class RotationSensorStreamer(
    context: Context,
    private val coroutineScope: CoroutineScope // Scope from the calling Composable/Activity
) : SensorEventListener {

    private var sensorManager: SensorManager
    private var rotationVectorSensor: Sensor? = null
    private var isSensorRegistered = false

    private var sendJob: Job? = null
    private val SEND_INTERVAL_MS = 5000L // Adjust as needed

    private val _rotationMatrixChannel = Channel<FloatArray>(Channel.CONFLATED)
    private val rotationMatrixFlow = _rotationMatrixChannel.receiveAsFlow()

    init {
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    }

    fun startStreaming() {
        if (sendJob?.isActive == true) {
            Log.d("RotationSensorStreamer", "Streaming already active.")
            return
        }
        if (rotationVectorSensor == null) {
            Log.e("RotationSensorStreamer", "Rotation Vector sensor not available.")
            return
        }

        if (!isSensorRegistered) {
            sensorManager.registerListener(this, rotationVectorSensor, SensorManager.SENSOR_DELAY_UI)
            isSensorRegistered = true
        }

        sendJob = coroutineScope.launch {
            Log.i("RotationSensorStreamer", "Starting rotation matrix stream.")
            rotationMatrixFlow.collect { matrix ->
                if (!isActive) return@collect // Coroutine cancelled

                try {
                    // Option 1: String
                    val matrixString = formatMatrixToJsonString(matrix)
                    ConnectionManager.sendMessage(matrixString)

                } catch (e: Exception) {
                    Log.e("RotationSensorStreamer", "Error sending rotation matrix", e)
                }
                delay(SEND_INTERVAL_MS)
            }
        }
    }

    fun stopStreaming() {
        Log.i("RotationSensorStreamer", "Stopping rotation matrix stream.")
        sendJob?.cancel()
        sendJob = null
        if (isSensorRegistered) {
            sensorManager.unregisterListener(this)
            isSensorRegistered = false
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (sendJob?.isActive != true) return // Only process if streaming job is active

        if (event?.sensor?.type == Sensor.TYPE_ROTATION_VECTOR) {
            val rotationMatrix = FloatArray(16)
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
            val matrix3x3 = floatArrayOf(
                rotationMatrix[0], rotationMatrix[1], rotationMatrix[2],
                rotationMatrix[4], rotationMatrix[5], rotationMatrix[6],
                rotationMatrix[8], rotationMatrix[9], rotationMatrix[10]
            )
            _rotationMatrixChannel.trySend(matrix3x3)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not critical for this use case
    }

    private fun formatMatrixToJsonString(matrix : FloatArray) : String {
        val json = JSONObject()
        for (i in matrix.indices) {
            json.put("value$i", matrix[i])
        }
        return json.toString()
    }

}

