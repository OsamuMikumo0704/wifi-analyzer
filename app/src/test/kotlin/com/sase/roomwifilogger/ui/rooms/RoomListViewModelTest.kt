package com.sase.roomwifilogger.ui.rooms

import com.sase.roomwifilogger.data.RoomNameError
import com.sase.roomwifilogger.data.RoomRepository
import com.sase.roomwifilogger.data.db.RoomEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RoomListViewModelTest {
    @Test
    fun addRoomShowsInlineErrorForEmptyName() = runTest {
        val repository = FakeRoomRepository(addResult = Result.failure(RoomNameError.Empty))
        val viewModel = RoomListViewModel(repository, backgroundScope)

        viewModel.showAddDialog()
        viewModel.updateDialogInput(" ")
        viewModel.confirmAdd()
        runCurrent()

        val dialog = viewModel.uiState.value.dialog as RoomDialogState.Add
        assertEquals(RoomNameError.Empty, dialog.error)
    }

    @Test
    fun renameRoomShowsInlineErrorForDuplicateName() = runTest {
        val room = RoomEntity(id = 7, name = "Living", createdAt = 0)
        val repository = FakeRoomRepository(renameResult = Result.failure(RoomNameError.Duplicate))
        val viewModel = RoomListViewModel(repository, backgroundScope)

        viewModel.showRenameDialog(room)
        viewModel.updateDialogInput("Bedroom")
        viewModel.confirmRename()
        runCurrent()

        val dialog = viewModel.uiState.value.dialog as RoomDialogState.Rename
        assertEquals(RoomNameError.Duplicate, dialog.error)
    }

    @Test
    fun deleteConfirmationDeletesOnlyWhenConfirmed() = runTest {
        val room = RoomEntity(id = 3, name = "Kitchen", createdAt = 0)
        val repository = FakeRoomRepository()
        val viewModel = RoomListViewModel(repository, backgroundScope)

        viewModel.showDeleteDialog(room)
        viewModel.dismissDialog()
        runCurrent()
        assertTrue(repository.deletedIds.isEmpty())

        viewModel.showDeleteDialog(room)
        viewModel.confirmDelete()
        runCurrent()

        assertEquals(listOf(3L), repository.deletedIds)
        assertEquals(RoomDialogState.None, viewModel.uiState.value.dialog)
    }
}

private class FakeRoomRepository(
    private val addResult: Result<Long> = Result.success(1L),
    private val renameResult: Result<Unit> = Result.success(Unit),
) : RoomRepository {
    val deletedIds = mutableListOf<Long>()
    private val rooms = MutableStateFlow<List<RoomEntity>>(emptyList())

    override fun observeRooms(): Flow<List<RoomEntity>> = rooms

    override suspend fun addRoom(name: String): Result<Long> = addResult

    override suspend fun renameRoom(id: Long, name: String): Result<Unit> = renameResult

    override suspend fun deleteRoom(id: Long) {
        deletedIds += id
    }
}
