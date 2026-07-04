package com.sase.roomwifilogger.ui.rooms

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sase.roomwifilogger.data.RoomNameError
import com.sase.roomwifilogger.data.RoomRepository
import com.sase.roomwifilogger.data.db.RoomEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RoomListUiState(
    val rooms: List<RoomEntity> = emptyList(),
    val dialog: RoomDialogState = RoomDialogState.None,
)

sealed interface RoomDialogState {
    data object None : RoomDialogState
    data class Add(val input: String = "", val error: RoomNameError? = null) : RoomDialogState
    data class Rename(
        val room: RoomEntity,
        val input: String = room.name,
        val error: RoomNameError? = null,
    ) : RoomDialogState
    data class ConfirmDelete(val room: RoomEntity) : RoomDialogState
}

sealed interface RoomListNavigation {
    data class Measurement(val roomId: Long, val roomName: String) : RoomListNavigation
    data object History : RoomListNavigation
}

class RoomListViewModel(
    private val repository: RoomRepository,
    private val externalScope: CoroutineScope? = null,
) : ViewModel() {
    private val scope: CoroutineScope
        get() = externalScope ?: viewModelScope

    private val _uiState = MutableStateFlow(RoomListUiState())
    val uiState: StateFlow<RoomListUiState> = _uiState.asStateFlow()

    private val _navigation = MutableSharedFlow<RoomListNavigation>()
    val navigation: SharedFlow<RoomListNavigation> = _navigation.asSharedFlow()

    init {
        scope.launch {
            repository.observeRooms().collect { rooms ->
                _uiState.update { it.copy(rooms = rooms) }
            }
        }
    }

    fun showAddDialog() {
        _uiState.update { it.copy(dialog = RoomDialogState.Add()) }
    }

    fun showRenameDialog(room: RoomEntity) {
        _uiState.update { it.copy(dialog = RoomDialogState.Rename(room = room)) }
    }

    fun showDeleteDialog(room: RoomEntity) {
        _uiState.update { it.copy(dialog = RoomDialogState.ConfirmDelete(room)) }
    }

    fun dismissDialog() {
        _uiState.update { it.copy(dialog = RoomDialogState.None) }
    }

    fun updateDialogInput(input: String) {
        _uiState.update { state ->
            when (val dialog = state.dialog) {
                is RoomDialogState.Add -> state.copy(dialog = dialog.copy(input = input, error = null))
                is RoomDialogState.Rename -> state.copy(dialog = dialog.copy(input = input, error = null))
                else -> state
            }
        }
    }

    fun confirmAdd() {
        val dialog = uiState.value.dialog as? RoomDialogState.Add ?: return
        scope.launch {
            repository.addRoom(dialog.input).fold(
                onSuccess = { dismissDialog() },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(dialog = dialog.copy(error = error as? RoomNameError))
                    }
                },
            )
        }
    }

    fun confirmRename() {
        val dialog = uiState.value.dialog as? RoomDialogState.Rename ?: return
        scope.launch {
            repository.renameRoom(dialog.room.id, dialog.input).fold(
                onSuccess = { dismissDialog() },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(dialog = dialog.copy(error = error as? RoomNameError))
                    }
                },
            )
        }
    }

    fun confirmDelete() {
        val dialog = uiState.value.dialog as? RoomDialogState.ConfirmDelete ?: return
        scope.launch {
            repository.deleteRoom(dialog.room.id)
            dismissDialog()
        }
    }

    fun openRoom(room: RoomEntity) {
        scope.launch {
            _navigation.emit(RoomListNavigation.Measurement(room.id, room.name))
        }
    }

    fun openHistory() {
        scope.launch {
            _navigation.emit(RoomListNavigation.History)
        }
    }
}
