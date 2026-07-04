package com.sase.roomwifilogger.ui.measure

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sase.roomwifilogger.service.MeasurementEngine
import com.sase.roomwifilogger.service.MeasurementError
import com.sase.roomwifilogger.service.MeasurementState
import com.sase.roomwifilogger.service.MeasurementSummary
import com.sase.roomwifilogger.service.PreconditionChecker
import com.sase.roomwifilogger.service.PreconditionResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MeasurementUiState(
    val roomId: Long,
    val roomName: String,
    val memo: String = "",
    val status: MeasurementUiStatus = MeasurementUiStatus.Idle,
)

sealed interface MeasurementUiStatus {
    data object Idle : MeasurementUiStatus
    data object PermissionRequired : MeasurementUiStatus
    data object PermissionDenied : MeasurementUiStatus
    data object LocationServiceOff : MeasurementUiStatus
    data object WifiDisconnected : MeasurementUiStatus
    data class Running(
        val elapsedMillis: Long,
        val sampleCount: Int,
        val durationMillis: Long,
    ) : MeasurementUiStatus
    data class Completed(val summary: MeasurementSummary?) : MeasurementUiStatus
    data class Failed(val error: MeasurementError) : MeasurementUiStatus
    data object Cancelled : MeasurementUiStatus
}

class MeasurementViewModel(
    roomId: Long,
    roomName: String,
    private val preconditionChecker: PreconditionChecker,
    private val measurementEngine: MeasurementEngine,
    private val externalScope: CoroutineScope? = null,
) : ViewModel() {
    private val scope: CoroutineScope
        get() = externalScope ?: viewModelScope

    private val _uiState = MutableStateFlow(
        MeasurementUiState(roomId = roomId, roomName = roomName),
    )
    val uiState: StateFlow<MeasurementUiState> = _uiState.asStateFlow()

    private var measurementJob: Job? = null

    fun updateMemo(memo: String) {
        _uiState.update { it.copy(memo = memo) }
    }

    fun startMeasurement() {
        when (preconditionChecker.check()) {
            PreconditionResult.PermissionMissing -> {
                _uiState.update { it.copy(status = MeasurementUiStatus.PermissionRequired) }
            }
            PreconditionResult.LocationServiceOff -> {
                _uiState.update { it.copy(status = MeasurementUiStatus.LocationServiceOff) }
            }
            PreconditionResult.WifiDisconnected -> {
                _uiState.update { it.copy(status = MeasurementUiStatus.WifiDisconnected) }
            }
            PreconditionResult.Ready -> runMeasurement()
        }
    }

    fun onPermissionDenied() {
        _uiState.update { it.copy(status = MeasurementUiStatus.PermissionDenied) }
    }

    fun cancelMeasurement() {
        measurementJob?.cancel()
        measurementJob = null
        _uiState.update { it.copy(status = MeasurementUiStatus.Cancelled) }
    }

    fun acknowledgeStatus() {
        _uiState.update { it.copy(status = MeasurementUiStatus.Idle) }
    }

    private fun runMeasurement() {
        if (measurementJob?.isActive == true) return
        val state = uiState.value
        measurementJob = scope.launch {
            measurementEngine.run(roomId = state.roomId, memo = state.memo).collect { measurementState ->
                _uiState.update { current ->
                    current.copy(status = measurementState.toUiStatus())
                }
            }
        }
    }

    private fun MeasurementState.toUiStatus(): MeasurementUiStatus =
        when (this) {
            MeasurementState.Idle -> MeasurementUiStatus.Idle
            is MeasurementState.Running -> MeasurementUiStatus.Running(
                elapsedMillis = elapsedMillis,
                sampleCount = sampleCount,
                durationMillis = 30_000L,
            )
            is MeasurementState.Completed -> MeasurementUiStatus.Completed(summary)
            is MeasurementState.Failed -> MeasurementUiStatus.Failed(error)
            MeasurementState.Cancelled -> MeasurementUiStatus.Cancelled
        }
}
