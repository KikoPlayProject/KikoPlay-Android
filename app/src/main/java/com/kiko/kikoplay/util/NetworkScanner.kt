package com.kiko.kikoplay.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket

data class DiscoveredDevice(
    val host: String,
    val port: Int,
    val deviceName: String? = null
)

object NetworkScanner {

    private val commonPorts = listOf(8000, 8080, 8888, 9000, 9090)

    fun getLocalIpAddress(): String? {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return null
            for (intf in interfaces) {
                if (intf.isLoopback || !intf.isUp) continue
                for (addr in intf.inetAddresses) {
                    if (addr is Inet4Address && !addr.isLoopbackAddress) {
                        return addr.hostAddress
                    }
                }
            }
        } catch (_: Exception) {}
        return null
    }

    fun scan(port: Int? = null): Flow<DiscoveredDevice> = flow {
        val localIp = getLocalIpAddress() ?: return@flow
        val subnet = localIp.substringBeforeLast(".")
        val portsToScan = if (port != null) listOf(port) else commonPorts
        val semaphore = Semaphore(30)

        val results = coroutineScope {
            (1..254).flatMap { i ->
                val ip = "$subnet.$i"
                if (ip == localIp) emptyList()
                else portsToScan.map { p ->
                    async {
                        semaphore.withPermit {
                            if (isPortOpen(ip, p, 300)) {
                                DiscoveredDevice(host = ip, port = p)
                            } else null
                        }
                    }
                }
            }.awaitAll().filterNotNull()
        }

        for (device in results) {
            emit(device)
        }
    }.flowOn(Dispatchers.IO)

    private fun isPortOpen(host: String, port: Int, timeoutMs: Int): Boolean {
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, port), timeoutMs)
                true
            }
        } catch (_: Exception) {
            false
        }
    }
}
