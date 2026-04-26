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

data class LocalIpv4Network(
    val address: String,
    val prefixLength: Int
)

object NetworkScanner {

    private val commonPorts = listOf(8000, 8080, 8888, 9000, 9090)

    fun getLocalIpv4Networks(): List<LocalIpv4Network> {
        return try {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return emptyList()
            buildList {
                for (networkInterface in interfaces) {
                    if (!networkInterface.isUp || networkInterface.isLoopback) continue
                    for (interfaceAddress in networkInterface.interfaceAddresses) {
                        val address = interfaceAddress.address as? Inet4Address ?: continue
                        if (address.isLoopbackAddress || address.isLinkLocalAddress) continue
                        add(
                            LocalIpv4Network(
                                address = address.hostAddress ?: continue,
                                prefixLength = interfaceAddress.networkPrefixLength.toInt().coerceIn(0, 32)
                            )
                        )
                    }
                }
            }.distinct()
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun getLocalIpAddress(): String? {
        return getLocalIpv4Networks().firstOrNull()?.address
    }

    fun isInSameSubnet(host: String, localNetwork: LocalIpv4Network): Boolean {
        val hostValue = ipv4ToLong(host) ?: return false
        val localValue = ipv4ToLong(localNetwork.address) ?: return false
        val prefixLength = localNetwork.prefixLength.coerceIn(0, 32)
        val mask = if (prefixLength == 0) 0L else (-1L shl (32 - prefixLength)) and 0xFFFF_FFFFL
        return (hostValue and mask) == (localValue and mask)
    }

    fun scan(port: Int? = null): Flow<DiscoveredDevice> = flow {
        val localIp = getLocalIpAddress() ?: return@flow
        val subnet = localIp.substringBeforeLast(".")
        val portsToScan = if (port != null) listOf(port) else commonPorts
        val semaphore = Semaphore(30)

        val results = coroutineScope {
            (1..254).flatMap { index ->
                val ip = "$subnet.$index"
                if (ip == localIp) {
                    emptyList()
                } else {
                    portsToScan.map { targetPort ->
                        async {
                            semaphore.withPermit {
                                if (isPortOpen(ip, targetPort, 300)) {
                                    DiscoveredDevice(host = ip, port = targetPort)
                                } else {
                                    null
                                }
                            }
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

    private fun ipv4ToLong(ip: String): Long? {
        val parts = ip.split('.')
        if (parts.size != 4) return null

        var value = 0L
        for (part in parts) {
            val octet = part.toIntOrNull() ?: return null
            if (octet !in 0..255) return null
            value = (value shl 8) or octet.toLong()
        }
        return value
    }
}
