package com.sase.roomwifilogger.ui.rooms

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sase.roomwifilogger.data.RoomNameError
import com.sase.roomwifilogger.data.db.RoomEntity
import com.sase.roomwifilogger.ui.theme.RoomWifiLoggerTheme

@Composable
fun RoomListRoute(
    viewModel: RoomListViewModel,
    onNavigate: (RoomListNavigation) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    LaunchedEffect(viewModel) {
        viewModel.navigation.collect(onNavigate)
    }

    RoomListScreen(
        uiState = uiState,
        onAddClick = viewModel::showAddDialog,
        onHistoryClick = viewModel::openHistory,
        onRoomClick = viewModel::openRoom,
        onRenameClick = viewModel::showRenameDialog,
        onDeleteClick = viewModel::showDeleteDialog,
        onDialogInputChange = viewModel::updateDialogInput,
        onDialogDismiss = viewModel::dismissDialog,
        onAddConfirm = viewModel::confirmAdd,
        onRenameConfirm = viewModel::confirmRename,
        onDeleteConfirm = viewModel::confirmDelete,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomListScreen(
    uiState: RoomListUiState,
    onAddClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onRoomClick: (RoomEntity) -> Unit,
    onRenameClick: (RoomEntity) -> Unit,
    onDeleteClick: (RoomEntity) -> Unit,
    onDialogInputChange: (String) -> Unit,
    onDialogDismiss: () -> Unit,
    onAddConfirm: () -> Unit,
    onRenameConfirm: () -> Unit,
    onDeleteConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Room WiFi Logger") },
                actions = {
                    TextButton(onClick = onHistoryClick) {
                        Text("履歴")
                    }
                },
            )
        },
        floatingActionButton = {
            Button(onClick = onAddClick) {
                Text("追加")
            }
        },
    ) { padding ->
        if (uiState.rooms.isEmpty()) {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(24.dp)
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("部屋がありません", style = MaterialTheme.typography.titleMedium)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
            ) {
                items(uiState.rooms, key = { it.id }) { room ->
                    ListItem(
                        headlineContent = { Text(room.name) },
                        modifier = Modifier.clickable { onRoomClick(room) },
                        trailingContent = {
                            Row {
                                IconButton(onClick = { onRenameClick(room) }) {
                                    Text("編集")
                                }
                                IconButton(onClick = { onDeleteClick(room) }) {
                                    Text("削除")
                                }
                            }
                        },
                    )
                }
            }
        }
    }

    when (val dialog = uiState.dialog) {
        is RoomDialogState.Add -> RoomNameDialog(
            title = "部屋を追加",
            input = dialog.input,
            error = dialog.error,
            onInputChange = onDialogInputChange,
            onDismiss = onDialogDismiss,
            onConfirm = onAddConfirm,
        )
        is RoomDialogState.Rename -> RoomNameDialog(
            title = "部屋名を変更",
            input = dialog.input,
            error = dialog.error,
            onInputChange = onDialogInputChange,
            onDismiss = onDialogDismiss,
            onConfirm = onRenameConfirm,
        )
        is RoomDialogState.ConfirmDelete -> DeleteRoomDialog(
            roomName = dialog.room.name,
            onDismiss = onDialogDismiss,
            onConfirm = onDeleteConfirm,
        )
        RoomDialogState.None -> Unit
    }
}

@Composable
private fun RoomNameDialog(
    title: String,
    input: String,
    error: RoomNameError?,
    onInputChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = input,
                    onValueChange = onInputChange,
                    label = { Text("部屋名") },
                    isError = error != null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (error != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = when (error) {
                            RoomNameError.Empty -> "部屋名を入力してください"
                            RoomNameError.Duplicate -> "同じ名前の部屋があります"
                        },
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("キャンセル")
            }
        },
    )
}

@Composable
private fun DeleteRoomDialog(
    roomName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("部屋を削除") },
        text = { Text("「$roomName」を削除します。測定データもすべて削除されます。") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("削除")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("キャンセル")
            }
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun RoomListScreenPreview() {
    RoomWifiLoggerTheme {
        RoomListScreen(
            uiState = RoomListUiState(
                rooms = listOf(
                    RoomEntity(id = 1, name = "Living", createdAt = 0),
                    RoomEntity(id = 2, name = "Bedroom", createdAt = 0),
                ),
            ),
            onAddClick = {},
            onHistoryClick = {},
            onRoomClick = {},
            onRenameClick = {},
            onDeleteClick = {},
            onDialogInputChange = {},
            onDialogDismiss = {},
            onAddConfirm = {},
            onRenameConfirm = {},
            onDeleteConfirm = {},
        )
    }
}
