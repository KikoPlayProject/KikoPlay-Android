package com.kiko.kikoplay.util

import java.net.URI

data class ServiceAddress(
    val host: String,
    val port: Int
) {
    val baseUrl: String
        get() = "http://$host:$port/"
}

object QrConnectionParser {

    fun parseServiceAddresses(rawContent: String): List<ServiceAddress> {
        val seen = LinkedHashSet<ServiceAddress>()
        rawContent.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .forEach { line ->
                parseLine(line)?.let(seen::add)
            }
        return seen.toList()
    }

    fun prioritizeForCurrentLan(
        addresses: List<ServiceAddress>,
        localNetworks: List<LocalIpv4Network> = NetworkScanner.getLocalIpv4Networks()
    ): List<ServiceAddress> {
        val uniqueAddresses = addresses.distinct()
        if (uniqueAddresses.isEmpty() || localNetworks.isEmpty()) return uniqueAddresses

        val sameLan = uniqueAddresses.filter { address ->
            localNetworks.any { network -> NetworkScanner.isInSameSubnet(address.host, network) }
        }
        if (sameLan.isEmpty()) return uniqueAddresses

        val remaining = uniqueAddresses.filterNot(sameLan::contains)
        return sameLan + remaining
    }

    private fun parseLine(line: String): ServiceAddress? {
        parseUriAddress(line)?.let { return it }
        return parseHostPortAddress(line)
    }

    private fun parseUriAddress(candidate: String): ServiceAddress? {
        val uri = runCatching { URI(candidate) }.getOrNull() ?: return null
        val host = uri.host ?: return null
        if (!isValidIpv4(host)) return null

        val resolvedPort = when {
            uri.port in 1..65535 -> uri.port
            else -> extractPortFromPath(uri.path)
        } ?: return null

        return ServiceAddress(host = host, port = resolvedPort)
    }

    private fun parseHostPortAddress(candidate: String): ServiceAddress? {
        val normalized = candidate
            .removePrefix("http://")
            .removePrefix("https://")
            .trimEnd('/')

        if ('/' in normalized) return null

        val separatorIndex = normalized.lastIndexOf(':')
        if (separatorIndex <= 0 || separatorIndex == normalized.lastIndex) return null

        val host = normalized.substring(0, separatorIndex)
        val port = normalized.substring(separatorIndex + 1).toIntOrNull()
            ?.takeIf { it in 1..65535 }
            ?: return null

        if (!isValidIpv4(host)) return null
        return ServiceAddress(host = host, port = port)
    }

    private fun extractPortFromPath(path: String?): Int? {
        if (path.isNullOrBlank()) return null
        val match = NON_STANDARD_PORT_PATH.matchEntire(path) ?: STANDARD_PORT_PATH.matchEntire(path)
        return match?.groupValues?.get(1)?.toIntOrNull()?.takeIf { it in 1..65535 }
    }

    private fun isValidIpv4(host: String): Boolean {
        val segments = host.split('.')
        if (segments.size != 4) return false
        return segments.all { segment ->
            val value = segment.toIntOrNull()
            value != null && value in 0..255
        }
    }

    private val NON_STANDARD_PORT_PATH = Regex("^/:(\\d+)/?$")
    private val STANDARD_PORT_PATH = Regex("^/(\\d+)/?$")
}
