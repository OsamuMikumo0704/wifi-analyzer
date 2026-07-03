package com.sase.roomwifilogger.wifi

data class WifiSnapshot(
    val ssid: String?,
    val bssid: String?,
    val rssiDbm: Int,
    val linkSpeedMbps: Int,
    val band: String,
    val capturedAtMillis: Long,
)

sealed interface WifiStatus {
    data object Disconnected : WifiStatus
    data class Connected(val snapshot: WifiSnapshot) : WifiStatus
}
