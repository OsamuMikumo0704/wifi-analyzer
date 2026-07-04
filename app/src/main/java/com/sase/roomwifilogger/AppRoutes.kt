package com.sase.roomwifilogger

import java.nio.charset.StandardCharsets

object AppRoutes {
    const val Rooms = "rooms"
    const val History = "history"
    const val MeasurementPattern = "measure/{roomId}/{roomName}"

    fun measurementRoute(roomId: Long, roomName: String): String =
        "measure/$roomId/${encodeRouteArgument(roomName)}"

    fun decodeRouteArgument(value: String): String =
        value.decodePercentEncoded()

    private fun encodeRouteArgument(value: String): String {
        val bytes = value.toByteArray(StandardCharsets.UTF_8)
        return buildString {
            bytes.forEach { byte ->
                val unsigned = byte.toInt() and 0xFF
                val char = unsigned.toChar()
                if (char.isUnreservedRouteChar()) {
                    append(char)
                } else {
                    append('%')
                    append(unsigned.toString(16).uppercase().padStart(2, '0'))
                }
            }
        }
    }

    private fun String.decodePercentEncoded(): String {
        val decoded = StringBuilder()
        val encodedBytes = mutableListOf<Byte>()
        var index = 0

        fun flushEncodedBytes() {
            if (encodedBytes.isNotEmpty()) {
                decoded.append(encodedBytes.toByteArray().toString(StandardCharsets.UTF_8))
                encodedBytes.clear()
            }
        }

        while (index < length) {
            if (this[index] == '%' && index + 2 < length) {
                encodedBytes += substring(index + 1, index + 3).toInt(16).toByte()
                index += 3
            } else {
                flushEncodedBytes()
                decoded.append(this[index])
                index += 1
            }
        }
        flushEncodedBytes()
        return decoded.toString()
    }

    private fun Char.isUnreservedRouteChar(): Boolean =
        this in 'A'..'Z' ||
            this in 'a'..'z' ||
            this in '0'..'9' ||
            this == '-' ||
            this == '.' ||
            this == '_' ||
            this == '~'
}
