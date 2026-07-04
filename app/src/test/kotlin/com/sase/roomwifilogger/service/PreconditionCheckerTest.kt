package com.sase.roomwifilogger.service

import com.sase.roomwifilogger.wifi.WifiStatus
import com.sase.roomwifilogger.wifi.WifiStatusMonitor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Assert.assertEquals
import org.junit.Test

class PreconditionCheckerTest {
    @Test
    fun checkReturnsPermissionMissingBeforeOtherProblems() {
        val checker = DefaultPreconditionChecker(
            permissionStatus = FakePermissionStatus(granted = false),
            locationServiceStatus = FakeLocationServiceStatus(enabled = false),
            wifiStatusMonitor = LocalFakeWifiStatusMonitor(MutableStateFlow(WifiStatus.Disconnected)),
        )

        assertEquals(PreconditionResult.PermissionMissing, checker.check())
    }

    @Test
    fun checkReturnsLocationServiceOffBeforeWifiDisconnected() {
        val checker = DefaultPreconditionChecker(
            permissionStatus = FakePermissionStatus(granted = true),
            locationServiceStatus = FakeLocationServiceStatus(enabled = false),
            wifiStatusMonitor = LocalFakeWifiStatusMonitor(MutableStateFlow(WifiStatus.Disconnected)),
        )

        assertEquals(PreconditionResult.LocationServiceOff, checker.check())
    }

    @Test
    fun checkReturnsWifiDisconnectedWhenPermissionAndLocationAreReady() {
        val checker = DefaultPreconditionChecker(
            permissionStatus = FakePermissionStatus(granted = true),
            locationServiceStatus = FakeLocationServiceStatus(enabled = true),
            wifiStatusMonitor = LocalFakeWifiStatusMonitor(MutableStateFlow(WifiStatus.Disconnected)),
        )

        assertEquals(PreconditionResult.WifiDisconnected, checker.check())
    }

    @Test
    fun checkReturnsReadyWhenAllPreconditionsAreReady() {
        val checker = DefaultPreconditionChecker(
            permissionStatus = FakePermissionStatus(granted = true),
            locationServiceStatus = FakeLocationServiceStatus(enabled = true),
            wifiStatusMonitor = LocalFakeWifiStatusMonitor(
                MutableStateFlow(
                    WifiStatus.Connected(
                        com.sase.roomwifilogger.wifi.WifiSnapshot(
                            ssid = "Home",
                            bssid = "00:11:22:33:44:55",
                            rssiDbm = -50,
                            linkSpeedMbps = 144,
                            band = "5GHz",
                            capturedAtMillis = 1L,
                        ),
                    ),
                ),
            ),
        )

        assertEquals(PreconditionResult.Ready, checker.check())
    }
}

private class FakePermissionStatus(private val granted: Boolean) : PermissionStatus {
    override fun hasFineLocationPermission(): Boolean = granted
}

private class FakeLocationServiceStatus(private val enabled: Boolean) : LocationServiceStatus {
    override fun isLocationServiceEnabled(): Boolean = enabled
}

private class LocalFakeWifiStatusMonitor(
    override val status: StateFlow<WifiStatus>,
) : WifiStatusMonitor {
    override fun start() = Unit
    override fun stop() = Unit
}
