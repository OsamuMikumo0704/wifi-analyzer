package com.sase.roomwifilogger.service

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import com.sase.roomwifilogger.wifi.WifiStatus
import com.sase.roomwifilogger.wifi.WifiStatusMonitor

sealed interface PreconditionResult {
    data object Ready : PreconditionResult
    data object PermissionMissing : PreconditionResult
    data object LocationServiceOff : PreconditionResult
    data object WifiDisconnected : PreconditionResult
}

interface PreconditionChecker {
    fun check(): PreconditionResult
}

interface PermissionStatus {
    fun hasFineLocationPermission(): Boolean
}

interface LocationServiceStatus {
    fun isLocationServiceEnabled(): Boolean
}

class DefaultPreconditionChecker(
    private val permissionStatus: PermissionStatus,
    private val locationServiceStatus: LocationServiceStatus,
    private val wifiStatusMonitor: WifiStatusMonitor,
) : PreconditionChecker {
    override fun check(): PreconditionResult =
        when {
            !permissionStatus.hasFineLocationPermission() -> PreconditionResult.PermissionMissing
            !locationServiceStatus.isLocationServiceEnabled() -> PreconditionResult.LocationServiceOff
            !hasWifiConnection() -> PreconditionResult.WifiDisconnected
            else -> PreconditionResult.Ready
        }

    private fun hasWifiConnection(): Boolean {
        wifiStatusMonitor.start()
        return wifiStatusMonitor.status.value is WifiStatus.Connected
    }
}

class AndroidPermissionStatus(
    private val context: Context,
) : PermissionStatus {
    override fun hasFineLocationPermission(): Boolean =
        context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
}

class AndroidLocationServiceStatus(
    context: Context,
) : LocationServiceStatus {
    private val locationManager = context.getSystemService(LocationManager::class.java)

    override fun isLocationServiceEnabled(): Boolean =
        locationManager.isLocationEnabled
}
