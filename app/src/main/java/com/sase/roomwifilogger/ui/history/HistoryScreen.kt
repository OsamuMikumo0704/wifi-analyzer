package com.sase.roomwifilogger.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sase.roomwifilogger.data.db.HistoryItem
import com.sase.roomwifilogger.data.db.RoomEntity
import com.sase.roomwifilogger.ui.theme.RoomWifiLoggerTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun HistoryRoute(viewModel: HistoryViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    HistoryScreen(
        uiState = uiState,
        onRoomSelected = viewModel::selectRoom,
        onExportClick = viewModel::exportAll,
    )
}

@Composable
fun HistoryScreen(
    uiState: HistoryUiState,
    onRoomSelected: (Long?) -> Unit,
    onExportClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("履歴", style = MaterialTheme.typography.headlineSmall)
        RoomFilter(
            rooms = uiState.rooms,
            selectedRoomId = uiState.selectedRoomId,
            onRoomSelected = onRoomSelected,
        )
        Button(onClick = onExportClick) {
            Text("CSV エクスポート")
        }
        ExportStatusText(uiState.exportStatus)
        if (uiState.records.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth().weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("データがありません")
            }
        } else {
            LazyColumn {
                items(uiState.records, key = { it.id }) { item ->
                    ListItem(
                        headlineContent = { Text(item.roomName) },
                        supportingContent = { Text(formatMeasuredAt(item.measuredAt)) },
                        trailingContent = { Text("${item.avgRssi} dBm") },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RoomFilter(
    rooms: List<RoomEntity>,
    selectedRoomId: Long?,
    onRoomSelected: (Long?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = rooms.firstOrNull { it.id == selectedRoomId }?.name ?: "すべて"

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            value = selectedName,
            onValueChange = {},
            readOnly = true,
            label = { Text("部屋") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            DropdownMenuItem(
                text = { Text("すべて") },
                onClick = {
                    onRoomSelected(null)
                    expanded = false
                },
            )
            rooms.forEach { room ->
                DropdownMenuItem(
                    text = { Text(room.name) },
                    onClick = {
                        onRoomSelected(room.id)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun ExportStatusText(status: ExportStatus) {
    when (status) {
        ExportStatus.Idle -> Unit
        ExportStatus.InProgress -> Text("エクスポート中")
        ExportStatus.NoRecords -> Text("エクスポートするデータがありません", color = MaterialTheme.colorScheme.error)
        ExportStatus.Failed -> Text("エクスポートに失敗しました", color = MaterialTheme.colorScheme.error)
        is ExportStatus.Success -> Text("${status.fileName} に保存しました")
    }
}

private fun formatMeasuredAt(measuredAt: Long): String {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    return formatter.format(Instant.ofEpochMilli(measuredAt).atZone(ZoneId.systemDefault()))
}

@Preview(showBackground = true)
@Composable
private fun HistoryScreenPreview() {
    RoomWifiLoggerTheme {
        HistoryScreen(
            uiState = HistoryUiState(
                rooms = listOf(RoomEntity(id = 1, name = "Living", createdAt = 0)),
                records = listOf(
                    HistoryItem(
                        id = 1,
                        roomId = 1,
                        roomName = "Living",
                        measuredAt = 1_783_000_000_000L,
                        avgRssi = -52.4,
                    ),
                ),
                exportStatus = ExportStatus.Success("wifi_log_2026-07-04.csv"),
            ),
            onRoomSelected = {},
            onExportClick = {},
        )
    }
}
