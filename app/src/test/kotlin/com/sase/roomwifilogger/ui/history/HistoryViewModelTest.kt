package com.sase.roomwifilogger.ui.history

import com.sase.roomwifilogger.data.MeasurementRepository
import com.sase.roomwifilogger.data.RoomRepository
import com.sase.roomwifilogger.data.db.ExportRow
import com.sase.roomwifilogger.data.db.HistoryItem
import com.sase.roomwifilogger.data.db.MeasurementEntity
import com.sase.roomwifilogger.data.db.RoomEntity
import com.sase.roomwifilogger.service.CsvExporter
import com.sase.roomwifilogger.service.ExportError
import com.sase.roomwifilogger.service.ExportException
import com.sase.roomwifilogger.service.ExportResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class HistoryViewModelTest {
    @Test
    fun observesHistoryAndFiltersByRoom() = runTest {
        val repository = FakeMeasurementRepository()
        val viewModel = HistoryViewModel(
            roomRepository = FakeRoomRepository(),
            measurementRepository = repository,
            csvExporter = FakeCsvExporter(Result.success(ExportResult("wifi_log.csv", 1))),
            externalScope = backgroundScope,
        )
        runCurrent()

        repository.emit(roomId = null, records = listOf(historyItem(id = 1, roomId = 1)))
        runCurrent()
        assertEquals(listOf(1L), viewModel.uiState.value.records.map { it.id })

        viewModel.selectRoom(2L)
        runCurrent()
        repository.emit(roomId = 2L, records = listOf(historyItem(id = 2, roomId = 2)))
        runCurrent()

        assertEquals(2L, viewModel.uiState.value.selectedRoomId)
        assertEquals(listOf(2L), viewModel.uiState.value.records.map { it.id })
    }

    @Test
    fun exportSuccessShowsFileName() = runTest {
        val viewModel = HistoryViewModel(
            roomRepository = FakeRoomRepository(),
            measurementRepository = FakeMeasurementRepository(),
            csvExporter = FakeCsvExporter(Result.success(ExportResult("wifi_log_2026-07-04.csv", 3))),
            externalScope = backgroundScope,
        )

        viewModel.exportAll()
        runCurrent()

        assertEquals(ExportStatus.Success("wifi_log_2026-07-04.csv"), viewModel.uiState.value.exportStatus)
    }

    @Test
    fun exportNoRecordsShowsNoRecordsMessage() = runTest {
        val viewModel = HistoryViewModel(
            roomRepository = FakeRoomRepository(),
            measurementRepository = FakeMeasurementRepository(),
            csvExporter = FakeCsvExporter(Result.failure(ExportException(ExportError.NoRecords))),
            externalScope = backgroundScope,
        )

        viewModel.exportAll()
        runCurrent()

        assertEquals(ExportStatus.NoRecords, viewModel.uiState.value.exportStatus)
    }

    private fun historyItem(id: Long, roomId: Long): HistoryItem =
        HistoryItem(
            id = id,
            roomId = roomId,
            roomName = "Room $roomId",
            measuredAt = id,
            avgRssi = -50.0,
        )
}

private class FakeRoomRepository : RoomRepository {
    private val rooms = MutableStateFlow(
        listOf(
            RoomEntity(id = 1, name = "Living", createdAt = 0),
            RoomEntity(id = 2, name = "Bedroom", createdAt = 0),
        ),
    )

    override fun observeRooms(): Flow<List<RoomEntity>> = rooms

    override suspend fun addRoom(name: String): Result<Long> = Result.success(1)

    override suspend fun renameRoom(id: Long, name: String): Result<Unit> = Result.success(Unit)

    override suspend fun deleteRoom(id: Long) = Unit
}

private class FakeMeasurementRepository : MeasurementRepository {
    private val allHistory = MutableStateFlow<List<HistoryItem>>(emptyList())
    private val filteredHistory = mutableMapOf<Long, MutableStateFlow<List<HistoryItem>>>()

    override suspend fun insert(record: MeasurementEntity): Long = 1

    override fun observeHistory(roomId: Long?): Flow<List<HistoryItem>> =
        roomId?.let { filteredHistory.getOrPut(it) { MutableStateFlow(emptyList()) } } ?: allHistory

    override suspend fun getAllForExport(): List<ExportRow> = emptyList()

    fun emit(roomId: Long?, records: List<HistoryItem>) {
        if (roomId == null) {
            allHistory.value = records
        } else {
            filteredHistory.getOrPut(roomId) { MutableStateFlow(emptyList()) }.value = records
        }
    }
}

private class FakeCsvExporter(
    private val result: Result<ExportResult>,
) : CsvExporter {
    override suspend fun exportAll(): Result<ExportResult> = result
}
