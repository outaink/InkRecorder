package com.outaink.inkrecorder.data.discovery

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketException
import javax.inject.Inject
import javax.inject.Singleton

data class ClientInfo(
    val address: InetAddress,
    val listeningPort: Int // Port where client is listening for audio stream
)

@Singleton
class MicServiceAdvertiser @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private var registrationListener: NsdManager.RegistrationListener? = null
    private var serviceName: String? = null
    private var listeningJob: Job? = null
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    private val _clientAddressFlow = MutableStateFlow<ClientInfo?>(null)
    val clientAddressFlow = _clientAddressFlow.asStateFlow()

    companion object {
        const val SERVICE_TYPE = "_androidmic._udp."
        const val TAG = "MicServiceAdvertiser"
    }

    fun registerService(port: Int, deviceName: String) {
        cleanup()

        registrationListener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(serviceInfo: NsdServiceInfo) {
                serviceName = serviceInfo.serviceName
                Log.d(TAG, "✅ Service registered: $serviceName on port ${port}")
                startListeningForClient(port)
            }

            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.e(TAG, "Service registration failed. Error code: $errorCode")
                cleanup()
            }

            override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) {
                Log.d(TAG, "Service unregistered: ${serviceInfo.serviceName}")
            }

            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.e(TAG, "Service unregistration failed. Error code: $errorCode")
            }
        }

        val serviceInfo = NsdServiceInfo().apply {
            this.serviceName = deviceName
            this.serviceType = SERVICE_TYPE
            this.port = port
        }

        nsdManager.registerService(
            serviceInfo,
            NsdManager.PROTOCOL_DNS_SD,
            registrationListener
        )
    }

    fun cleanup() {
        registrationListener?.let {
            nsdManager.unregisterService(it)
            registrationListener = null
        }

        listeningJob?.cancel()
        listeningJob = null

        _clientAddressFlow.value = null
    }

    private fun startListeningForClient(port: Int) {
        listeningJob?.cancel()

        listeningJob = coroutineScope.launch {
            // 在这里声明 socket，确保它的作用域只在协程内
            var socket: DatagramSocket? = null
            try {
                socket = DatagramSocket(port)
                Log.d(TAG, "🎧 Now listening for client on UDP port $port...")
                val buffer = ByteArray(10240)
                val packet = DatagramPacket(buffer, buffer.size)
                while (isActive) {
                    socket.receive(packet) // 使用局部变量 socket
                    val clientIp = packet.address
                    val clientPort = packet.port
                    
                    // Parse the received message
                    val messageBytes = ByteArray(packet.length)
                    System.arraycopy(packet.data, packet.offset, messageBytes, 0, packet.length)
                    val message = String(messageBytes, Charsets.UTF_8)
                    
                    Log.d(TAG, "🤝 Client handshake received from: ${clientIp.hostAddress}:$clientPort")
                    Log.d(TAG, "📨 Message content: $message")
                    
                    // Parse CONNECT:<port> message
                    if (message.startsWith("CONNECT:")) {
                        try {
                            val listeningPort = message.substring(8).toInt()
                            Log.d(TAG, "✅ Parsed client listening port: $listeningPort")
                            
                            _clientAddressFlow.emit(ClientInfo(clientIp, listeningPort))
                            break
                        } catch (e: NumberFormatException) {
                            Log.e(TAG, "❌ Invalid CONNECT message format: $message", e)
                        }
                    } else {
                        Log.w(TAG, "⚠️ Unknown handshake message format: $message")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error while listening for client", e)
            } finally {
                // 这个 finally 块是关闭 socket 的唯一责任方
                socket?.close()
                Log.d(TAG, "UDP Listener has been shut down.")
            }
        }
    }
}