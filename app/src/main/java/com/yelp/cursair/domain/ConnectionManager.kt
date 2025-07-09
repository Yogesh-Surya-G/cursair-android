package com.yelp.cursair.domain

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketException
import java.net.SocketTimeoutException


object ConnectionManager {

    private const val TAG = "ConnectionManager"
    private const val CONNECTION_TIMEOUT_MS = 5000
    private const val BUFFER_SIZE = 1024

    private val _isConnected = MutableStateFlow(false)
    val isConnected = _isConnected.asStateFlow()

    private var socket: DatagramSocket? = null
    private var hostAddress: InetAddress? = null
    private var hostPort: Int = -1

    private var listeningJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _receivedMessages = MutableSharedFlow<String>()
    val receivedMessages = _receivedMessages.asSharedFlow()

    suspend fun connect(qrData: String): Boolean =
        withContext(Dispatchers.IO) {
            if (socket?.isConnected == true) {
                disconnect()
            }

            try {
                val jsonObject = JSONObject(qrData)
                val hostname = jsonObject.getString("targetIp")
                val port = jsonObject.getInt("targetPort")
                val password = jsonObject.getString("password")

                hostAddress = InetAddress.getByName(hostname)
                hostPort = port

                socket = DatagramSocket()
                socket?.soTimeout = CONNECTION_TIMEOUT_MS

                val jsonRequest = JSONObject()
                jsonRequest.put("password",password)
                val jsonRequestString = jsonRequest.toString()
                val connectRequest = jsonRequestString.toByteArray()
                val sendPacket =
                    DatagramPacket(connectRequest, connectRequest.size, hostAddress, hostPort)
                socket?.send(sendPacket)

                Log.d(TAG, "Authentication request (password) sent to $hostAddress:$hostPort")

                val receiveBuffer = ByteArray(BUFFER_SIZE)
                val receivePacket = DatagramPacket(receiveBuffer, receiveBuffer.size)
                socket?.receive(receivePacket)
                val response = String(receivePacket.data, 0, receivePacket.length).trim()
                Log.d(TAG, "Authentication response received: $response")
                if (response == "CONNECTED") {
                    Log.i(TAG, "Connection successful")

                    startListening()
                    _isConnected.value = true
                    return@withContext true
                } else {
                    Log.w(TAG, "Connection failed")
                    disconnect()
                    return@withContext false
                }
            } catch (e: JSONException) {
                _isConnected.value = false
                Log.e(TAG, "Connection failed : Invalid QR Code", e)
                return@withContext false
            } catch (e: SocketTimeoutException) {
                Log.e(TAG, "Connection failed : Timeout", e)
                disconnect()
                return@withContext false
            } catch (e: Exception) {
                Log.e(TAG, "Connection Failed: Unknown Error",e)
                disconnect()
                return@withContext false
            }
        }


    suspend fun sendMessage(message: String) = withContext(Dispatchers.IO) {
        if (socket == null || socket!!.isClosed || hostAddress == null) {
            Log.w(TAG, "Cannot send message, not connected.")
            return@withContext
        }
        try {
            val messageBytes = message.toByteArray()
            val packet = DatagramPacket(messageBytes, messageBytes.size, hostAddress, hostPort)
            socket?.send(packet)
            Log.d(TAG, "Message sent: $message")
        } catch (e: Exception) {
            Log.e(TAG, "Error sending message", e)
        }
    }

    private fun startListening() {
        listeningJob?.cancel()

        listeningJob = scope.launch {
            Log.i(TAG, "Started listening for messages")
            while (isActive && socket?.isClosed == false) {
                try {
                    val buffer = ByteArray(BUFFER_SIZE)
                    val packet = DatagramPacket(buffer, buffer.size)
                    socket?.receive(packet)
                    val message = String(packet.data, 0, packet.length)
                    Log.d(TAG, "Received message: $message")
                    _receivedMessages.emit(message)
                } catch (e: SocketException) {
                    if (isActive) {
                        Log.e(TAG, "Socket closed unexpectedly while listening", e)
                    } else {
                        Log.i(TAG, "Listening cancelled")
                    }
                    break
                } catch (e: Exception) {
                    Log.e(TAG, "Error receiving message", e)
                }
                break
            }
            Log.i(TAG, "Stopped listening for messages")
        }
    }


    suspend fun disconnect() = withContext(Dispatchers.IO) {
        Log.i(TAG,"Disconnecting...")
        listeningJob?.cancel()
        listeningJob = null

        socket?.close()
        socket = null

        _isConnected.value = false

        hostAddress = null
        hostPort = -1
        Log.i(TAG, "Successfully Disconnected")
    }
}