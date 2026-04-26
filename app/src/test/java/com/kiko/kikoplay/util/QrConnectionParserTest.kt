package com.kiko.kikoplay.util

import org.junit.Assert.assertEquals
import org.junit.Test

class QrConnectionParserTest {

    @Test
    fun parseServiceAddresses_supportsStandardAndNonStandardFormats() {
        val rawContent = """
            http://192.168.31.8:8000/
            http://10.0.0.5/:9000
            172.16.4.9:8080
            http://10.0.0.5/:9000
            invalid
        """.trimIndent()

        val addresses = QrConnectionParser.parseServiceAddresses(rawContent)

        assertEquals(
            listOf(
                ServiceAddress("192.168.31.8", 8000),
                ServiceAddress("10.0.0.5", 9000),
                ServiceAddress("172.16.4.9", 8080)
            ),
            addresses
        )
    }

    @Test
    fun prioritizeForCurrentLan_movesSameSubnetAddressesToFront() {
        val addresses = listOf(
            ServiceAddress("10.0.0.8", 8000),
            ServiceAddress("192.168.31.20", 8000),
            ServiceAddress("192.168.31.30", 9000)
        )

        val ordered = QrConnectionParser.prioritizeForCurrentLan(
            addresses = addresses,
            localNetworks = listOf(LocalIpv4Network("192.168.31.12", 24))
        )

        assertEquals(
            listOf(
                ServiceAddress("192.168.31.20", 8000),
                ServiceAddress("192.168.31.30", 9000),
                ServiceAddress("10.0.0.8", 8000)
            ),
            ordered
        )
    }

    @Test
    fun prioritizeForCurrentLan_keepsOriginalOrderWhenNoSubnetMatches() {
        val addresses = listOf(
            ServiceAddress("10.0.0.8", 8000),
            ServiceAddress("172.16.4.9", 8080)
        )

        val ordered = QrConnectionParser.prioritizeForCurrentLan(
            addresses = addresses,
            localNetworks = listOf(LocalIpv4Network("192.168.31.12", 24))
        )

        assertEquals(addresses, ordered)
    }
}
