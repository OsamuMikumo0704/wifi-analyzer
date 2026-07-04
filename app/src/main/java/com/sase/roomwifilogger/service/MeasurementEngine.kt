package com.sase.roomwifilogger.service

import com.sase.roomwifilogger.data.MeasurementRepository
import com.sase.roomwifilogger.data.db.MeasurementEntity
import com.sase.roomwifilogger.wifi.WifiSnapshot
import com.sase.roomwifilogger.wifi.WifiStatus
import com.sase.roomwifilogger.wifi.WifiStatusMonitor
import kotlin.math.round
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

data class MeasurementConfig(
    val durationMillis: Long = 30_000L,
    val sampleIntervalMillis: Long = 3_000L,
)

data class MeasurementSummary(
    val avgRssi: Double,
    val minRssi: Int,
    val maxRssi: Int,
    val sampleCount: Int,
)

sealed interface MeasurementState {
    data object Idle : MeasurementState
    data class Running(val elapsedMillis: Long, val sampleCount: Int) : MeasurementState
    data class Completed(
        val recordId: Long,
        val summary: MeasurementSummary? = null,
    ) : MeasurementState
    data class Failed(val error: MeasurementError) : MeasurementState
    data object Cancelled : MeasurementState
}

enum class MeasurementError { WifiLost, NoSamples }

interface MeasurementEngine {
    fun run(
        roomId: Long,
        memo: String,
        config: MeasurementConfig = MeasurementConfig(),
    ): Flow<MeasurementState>
}

class DefaultMeasurementEngine(
    private val wifiStatusMonitor: WifiStatusMonitor,
    private val measurementRepository: MeasurementRepository,
) : MeasurementEngine {
    override fun run(
        roomId: Long,
        memo: String,
        config: MeasurementConfig,
    ): Flow<MeasurementState> = flow {
        val samples = mutableListOf<WifiSnapshot>()
        var elapsedMillis = 0L

        while (elapsedMillis < config.durationMillis) {
            currentCoroutineContext().ensureActive()
            when (val status = wifiStatusMonitor.status.value) {
                WifiStatus.Disconnected -> {
                    emit(MeasurementState.Failed(MeasurementError.WifiLost))
                    return@flow
                }
                is WifiStatus.Connected -> {
                    samples += status.snapshot
                    emit(
                        MeasurementState.Running(
                            elapsedMillis = elapsedMillis,
                            sampleCount = samples.size,
                        ),
                    )
                }
            }

            delay(config.sampleIntervalMillis)
            elapsedMillis += config.sampleIntervalMillis
        }

        val result = samples.toMeasurementResult(roomId = roomId, memo = memo)
        if (result == null) {
            emit(MeasurementState.Failed(MeasurementError.NoSamples))
            return@flow
        }

        val recordId = measurementRepository.insert(result.record)
        emit(MeasurementState.Completed(recordId, result.summary))
    }

    private fun List<WifiSnapshot>.toMeasurementResult(
        roomId: Long,
        memo: String,
    ): MeasurementResult? {
        if (isEmpty()) return null
        val lastSample = last()
        val rssiValues = map { it.rssiDbm }
        val avgRssi = round(rssiValues.average() * 10.0) / 10.0

        return MeasurementResult(
            record = MeasurementEntity(
                roomId = roomId,
                measuredAt = lastSample.capturedAtMillis,
                ssid = lastSample.ssid.orEmpty(),
                bssid = lastSample.bssid.orEmpty(),
                band = lastSample.band,
                avgRssi = avgRssi,
                minRssi = rssiValues.min(),
                maxRssi = rssiValues.max(),
                sampleCount = size,
                linkSpeedMbps = lastSample.linkSpeedMbps,
                memo = memo,
            ),
            summary = MeasurementSummary(
                avgRssi = avgRssi,
                minRssi = rssiValues.min(),
                maxRssi = rssiValues.max(),
                sampleCount = size,
            ),
        )
    }

    private data class MeasurementResult(
        val record: MeasurementEntity,
        val summary: MeasurementSummary,
    )
}
