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
import javax.inject.Inject
import javax.inject.Singleton

data class ClientInfo(val address: InetAddress, val port: Int)

@Singleton
class MicServiceAdvertiser @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private var registrationListener: NsdManager.RegistrationListener? = null
    private var serviceName: String? = null

    // --- 新增属性 ---
    private var udpSocket: DatagramSocket? = null
    private var listeningJob: Job? = null
    private val coroutineScope = CoroutineScope(Dispatchers.IO) // 用于网络操作的协程作用域

    // 使用 StateFlow 向外部暴露客户端连接信息
    private val _clientAddressFlow = MutableStateFlow<ClientInfo?>(null)
    val clientAddressFlow = _clientAddressFlow.asStateFlow()
    // --- 新增属性结束 ---

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

                // *** 关键逻辑：服务注册成功后，开始在同一端口监听 ***
                startListeningForClient(port)
            }

            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.e(TAG, "Service registration failed. Error code: $errorCode")
                cleanup() // 失败后清理
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
        listeningJob?.cancel() // 取消旧任务

        listeningJob = coroutineScope.launch {
            // 在这里声明 socket，确保它的作用域只在协程内
            var socket: DatagramSocket? = null
            try {
                socket = DatagramSocket(port)
                // udpSocket = socket // 不再需要将 socket 赋值给类成员变量

                Log.d(TAG, "🎧 Now listening for client on UDP port $port...")
                val buffer = ByteArray(10240)
                val packet = DatagramPacket(buffer, buffer.size)
                while (isActive) {
                    socket.receive(packet) // 使用局部变量 socket
                    val clientIp = packet.address
                    val clientPort = packet.port
                    Log.d(
                        TAG,
                        "🤝 Client handshake received from: ${clientIp.hostAddress}:$clientPort"
                    )
                    _clientAddressFlow.value = ClientInfo(clientIp, clientPort)
                    break
                }
            } catch (e: Exception) {
                if (e is java.net.SocketException && (e.message?.contains("Socket closed") == true || e is java.net.BindException)) {
                    Log.d(
                        TAG,
                        "Socket exception during listen (normal on cleanup or port busy): ${e.message}"
                    )
                } else {
                    Log.e(TAG, "Error while listening for client", e)
                }
            } finally {
                // 这个 finally 块是关闭 socket 的唯一责任方
                socket?.close()
                Log.d(TAG, "UDP Listener has been shut down.")
            }
        }
    }
}