package com.sase.roomwifilogger.service

import com.sase.roomwifilogger.data.MeasurementRepository
import com.sase.roomwifilogger.data.db.ExportRow
import com.sase.roomwifilogger.data.db.HistoryItem
import com.sase.roomwifilogger.data.db.MeasurementEntity
import com.sase.roomwifilogger.wifi.WifiSnapshot
import com.sase.roomwifilogger.wifi.WifiStatus
import com.sase.roomwifilogger.wifi.WifiStatusMonitor
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MeasurementEngineTest {
    @Test
    fun runAggregatesSamplesAndSavesOneRecord() = runTest {
        val status = MutableStateFlow<WifiStatus>(WifiStatus.Connected(snapshot(rssi = -60, capturedAt = 1_000L)))
        val repository = FakeMeasurementRepository()
        val engine = DefaultMeasurementEngine(FakeWifiStatusMonitor(status), repository)

        val states = mutableListOf<MeasurementState>()
        val job = launch {
            engine.run(
                roomId = 42L,
                memo = "desk",
                config = MeasurementConfig(durationMillis = 9_000L, sampleIntervalMillis = 3_000L),
            ).toList(states)
        }

        runCurrent()
        status.value = WifiStatus.Connected(snapshot(rssi = -51, capturedAt = 4_000L, ssid = "Office"))
        advanceTimeBy(3_000L)
        runCurrent()
        status.value = WifiStatus.Connected(snapshot(rssi = -50, capturedAt = 7_000L, linkSpeed = 300))
        advanceTimeBy(3_000L)
        runCurrent()
        advanceTimeBy(3_000L)
        job.join()

        assertEquals(1, repository.inserted.size)
        val record = repository.inserted.single()
        assertEquals(42L, record.roomId)
        assertEquals(7_000L, record.measuredAt)
        assertEquals("Home", record.ssid)
        assertEquals(-53.7, record.avgRssi, 0.0)
        assertEquals(-60, record.minRssi)
        assertEquals(-50, record.maxRssi)
        assertEquals(3, record.sampleCount)
        assertEquals(300, record.linkSpeedMbps)
        assertEquals("desk", record.memo)
        assertTrue(states.last() is MeasurementState.Completed)
        assertEquals(listOf(1, 2, 3), states.filterIsInstance<MeasurementState.Running>().map { it.sampleCount })
    }

    @Test
    fun runFailsWithoutSavingWhenWifiDisconnectsDuringSampling() = runTest {
        val status = MutableStateFlow<WifiStatus>(WifiStatus.Connected(snapshot(rssi = -60)))
        val repository = FakeMeasurementRepository()
        val engine = DefaultMeasurementEngine(FakeWifiStatusMonitor(status), repository)

        val states = mutableListOf<MeasurementState>()
        val job = launch {
            engine.run(
                roomId = 1L,
                memo = "",
                config = MeasurementConfig(durationMillis = 9_000L, sampleIntervalMillis = 3_000L),
            ).toList(states)
        }

        runCurrent()
        status.value = WifiStatus.Disconnected
        advanceTimeBy(3_000L)
        job.join()

        assertTrue(repository.inserted.isEmpty())
        assertEquals(MeasurementState.Failed(MeasurementError.WifiLost), states.last())
    }

    @Test
    fun runDoesNotSaveWhenCollectionIsCancelled() = runTest {
        val status = MutableStateFlow<WifiStatus>(WifiStatus.Connected(snapshot(rssi = -60)))
        val repository = FakeMeasurementRepository()
        val engine = DefaultMeasurementEngine(FakeWifiStatusMonitor(status), repository)

        val job = launch {
            engine.run(
                roomId = 1L,
                memo = "",
                config = MeasurementConfig(durationMillis = 30_000L, sampleIntervalMillis = 3_000L),
            ).collect {}
        }

        runCurrent()
        job.cancelAndJoin()

        assertTrue(repository.inserted.isEmpty())
    }

    @Test
    fun runFailsWithoutSavingWhenDurationAllowsNoSamples() = runTest {
        val status = MutableStateFlow<WifiStatus>(WifiStatus.Connected(snapshot(rssi = -60)))
        val repository = FakeMeasurementRepository()
        val engine = DefaultMeasurementEngine(FakeWifiStatusMonitor(status), repository)

        val states = engine.run(
            roomId = 1L,
            memo = "",
            config = MeasurementConfig(durationMillis = 0L, sampleIntervalMillis = 3_000L),
        ).toList()

        assertTrue(repository.inserted.isEmpty())
        assertEquals(MeasurementState.Failed(MeasurementError.NoSamples), states.last())
    }

    private fun snapshot(
        rssi: Int,
        capturedAt: Long = 1_000L,
        ssid: String? = "Home",
        linkSpeed: Int = 144,
    ): WifiSnapshot =
        WifiSnapshot(
            ssid = ssid,
            bssid = "00:11:22:33:44:55",
            rssiDbm = rssi,
            linkSpeedMbps = linkSpeed,
            band = "5GHz",
            capturedAtMillis = capturedAt,
        )
}

private class FakeWifiStatusMonitor(
    override val status: StateFlow<WifiStatus>,
) : WifiStatusMonitor {
    override fun start() = Unit
    override fun stop() = Unit
}

private class FakeMeasurementRepository : MeasurementRepository {
    val inserted = mutableListOf<MeasurementEntity>()

    override suspend fun insert(record: MeasurementEntity): Long {
        inserted += record
        return inserted.size.toLong()
    }

    override fun observeHistory(roomId: Long?): Flow<List<HistoryItem>> =
        throw NotImplementedError()

    override suspend fun getAllForExport(): List<ExportRow> =
        throw NotImplementedError()
}
