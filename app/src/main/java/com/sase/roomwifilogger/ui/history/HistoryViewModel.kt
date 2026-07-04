package com.sase.roomwifilogger.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sase.roomwifilogger.data.MeasurementRepository
import com.sase.roomwifilogger.data.RoomRepository
import com.sase.roomwifilogger.data.db.HistoryItem
import com.sase.roomwifilogger.data.db.RoomEntity
import com.sase.roomwifilogger.service.CsvExporter
import com.sase.roomwifilogger.service.ExportError
import com.sase.roomwifilogger.service.ExportException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HistoryUiState(
    val rooms: List<RoomEntity> = emptyList(),
    val selectedRoomId: Long? = null,
    val records: List<HistoryItem> = emptyList(),
    val exportStatus: ExportStatus = ExportStatus.Idle,
)

sealed interface ExportStatus {
    data object Idle : ExportStatus
    data object InProgress : ExportStatus
    data class Success(val fileName: String) : ExportStatus
    data object NoRecords : ExportStatus
    data object Failed : ExportStatus
}

class HistoryViewModel(
    private val roomRepository: RoomRepository,
    private val measurementRepository: MeasurementRepository,
    private val csvExporter: CsvExporter,
    private val externalScope: CoroutineScope? = null,
) : ViewModel() {
    private val scope: CoroutineScope
        get() = externalScope ?: viewModelScope

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    private var historyJob: Job? = null

    init {
        scope.launch {
            roomRepository.observeRooms().collect { rooms ->
                _uiState.update { it.copy(rooms = rooms) }
            }
        }
        observeHistory(null)
    }

    fun selectRoom(roomId: Long?) {
        _uiState.update { it.copy(selectedRoomId = roomId) }
        observeHistory(roomId)
    }

    fun exportAll() {
        scope.launch {
            _uiState.update { it.copy(exportStatus = ExportStatus.InProgress) }
            csvExporter.exportAll().fold(
                onSuccess = { result ->
                    _uiState.update { it.copy(exportStatus = ExportStatus.Success(result.fileName)) }
                },
                onFailure = { error ->
                    val status = if ((error as? ExportException)?.error == ExportError.NoRecords) {
                        ExportStatus.NoRecords
                    } else {
                        ExportStatus.Failed
                    }
                    _uiState.update { it.copy(exportStatus = status) }
                },
            )
        }
    }

    fun clearExportStatus() {
        _uiState.update { it.copy(exportStatus = ExportStatus.Idle) }
    }

    private fun observeHistory(roomId: Long?) {
        historyJob?.cancel()
        historyJob = scope.launch {
            measurementRepository.observeHistory(roomId).collect { records ->
                _uiState.update { it.copy(records = records) }
            }
        }
    }
}
