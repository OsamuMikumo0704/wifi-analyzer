package com.sase.roomwifilogger.wifi

import android.annotation.SuppressLint
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface WifiStatusMonitor {
    val status: StateFlow<WifiStatus>
    fun start()
    fun stop()
}

class AndroidWifiStatusMonitor(
    private val connectivityManager: ConnectivityManager,
    private val clockMillis: () -> Long = System::currentTimeMillis,
) : WifiStatusMonitor {
    private val _status = MutableStateFlow<WifiStatus>(WifiStatus.Disconnected)
    override val status: StateFlow<WifiStatus> = _status.asStateFlow()

    private var callback: ConnectivityManager.NetworkCallback? = null

    @SuppressLint("MissingPermission")
    override fun start() {
        if (callback != null) {
            return
        }

        val networkCallback = object : ConnectivityManager.NetworkCallback(
            ConnectivityManager.NetworkCallback.FLAG_INCLUDE_LOCATION_INFO,
        ) {
            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities,
            ) {
                _status.value = networkCapabilities.toWifiStatus()
            }

            override fun onLost(network: Network) {
                _status.value = WifiStatus.Disconnected
            }
        }

        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()
        connectivityManager.registerNetworkCallback(request, networkCallback)
        callback = networkCallback

        connectivityManager.activeNetwork
            ?.let(connectivityManager::getNetworkCapabilities)
            ?.takeIf { it.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) }
            ?.let { _status.value = it.toWifiStatus() }
    }

    override fun stop() {
        callback?.let { networkCallback ->
            runCatching { connectivityManager.unregisterNetworkCallback(networkCallback) }
        }
        callback = null
    }

    private fun NetworkCapabilities.toWifiStatus(): WifiStatus {
        val wifiInfo = transportInfo as? WifiInfo ?: return WifiStatus.Disconnected
        return WifiStatus.Connected(
            WifiSnapshot(
                ssid = wifiInfo.ssid.normalizedSsid(),
                bssid = wifiInfo.bssid,
                rssiDbm = wifiInfo.rssi,
                linkSpeedMbps = wifiInfo.linkSpeed,
                band = frequencyMhzToBand(wifiInfo.frequency),
                capturedAtMillis = clockMillis(),
            ),
        )
    }

    private fun String?.normalizedSsid(): String? {
        val value = this?.trim() ?: return null
        if (value.isEmpty() || value == WifiManagerUnknownSsid) {
            return null
        }
        return value.removeSurrounding("\"")
    }

    private companion object {
        const val WifiManagerUnknownSsid = "<unknown ssid>"
    }
}
