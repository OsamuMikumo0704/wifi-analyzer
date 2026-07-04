package com.sase.roomwifilogger.ui.measure

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sase.roomwifilogger.service.MeasurementError
import com.sase.roomwifilogger.service.MeasurementSummary
import com.sase.roomwifilogger.ui.theme.RoomWifiLoggerTheme

@Composable
fun MeasurementRoute(viewModel: MeasurementViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    MeasurementScreen(
        uiState = uiState,
        onMemoChange = viewModel::updateMemo,
        onStartClick = viewModel::startMeasurement,
        onCancelClick = viewModel::cancelMeasurement,
        onPermissionDenied = viewModel::onPermissionDenied,
    )
}

@Composable
fun MeasurementScreen(
    uiState: MeasurementUiState,
    onMemoChange: (String) -> Unit,
    onStartClick: () -> Unit,
    onCancelClick: () -> Unit,
    onPermissionDenied: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (!granted) onPermissionDenied()
    }
    val isRunning = uiState.status is MeasurementUiStatus.Running
    KeepScreenOn(active = isRunning)

    LaunchedEffect(uiState.status) {
        if (uiState.status == MeasurementUiStatus.PermissionRequired) {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(uiState.roomName, style = MaterialTheme.typography.headlineSmall)
        OutlinedTextField(
            value = uiState.memo,
            onValueChange = onMemoChange,
            label = { Text("メモ") },
            minLines = 2,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isRunning,
        )

        when (val status = uiState.status) {
            MeasurementUiStatus.Idle,
            MeasurementUiStatus.Cancelled,
            MeasurementUiStatus.PermissionRequired,
            -> Button(onClick = onStartClick, enabled = !isRunning) {
                Text("測定開始")
            }
            MeasurementUiStatus.PermissionDenied -> ErrorText(
                "接続情報の取得には位置情報権限が必要です。"
            )
            MeasurementUiStatus.LocationServiceOff -> {
                ErrorText("位置情報サービスを有効にしてください。")
                Button(
                    onClick = {
                        context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                    },
                ) {
                    Text("設定を開く")
                }
            }
            MeasurementUiStatus.WifiDisconnected -> ErrorText("Wi-Fi に接続されていません。")
            is MeasurementUiStatus.Running -> RunningMeasurement(status, onCancelClick)
            is MeasurementUiStatus.Completed -> CompletedMeasurement(status.summary)
            is MeasurementUiStatus.Failed -> ErrorText(
                when (status.error) {
                    MeasurementError.WifiLost -> "測定中に Wi-Fi 接続が失われたため中断しました。保存されていません。"
                    MeasurementError.NoSamples -> "有効なサンプルが取得できませんでした。保存されていません。"
                },
            )
        }
    }
}

@Composable
private fun RunningMeasurement(
    status: MeasurementUiStatus.Running,
    onCancelClick: () -> Unit,
) {
    val progress = (status.elapsedMillis.toFloat() / status.durationMillis.toFloat()).coerceIn(0f, 1f)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth(),
        )
        Text("残り ${(status.durationMillis - status.elapsedMillis).coerceAtLeast(0L) / 1000} 秒")
        Text("採取数 ${status.sampleCount}")
        TextButton(onClick = onCancelClick) {
            Text("キャンセル")
        }
    }
}

@Composable
private fun CompletedMeasurement(summary: MeasurementSummary?) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("保存しました", style = MaterialTheme.typography.titleMedium)
        if (summary != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("平均 ${summary.avgRssi} dBm")
                Text("最小 ${summary.minRssi} dBm")
                Text("最大 ${summary.maxRssi} dBm")
            }
        }
    }
}

@Composable
private fun ErrorText(text: String) {
    Text(text = text, color = MaterialTheme.colorScheme.error)
}

@Composable
private fun KeepScreenOn(active: Boolean) {
    val view = LocalView.current
    DisposableEffect(active) {
        val window = (view.context as? Activity)?.window
        if (active) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MeasurementScreenPreview() {
    RoomWifiLoggerTheme {
        MeasurementScreen(
            uiState = MeasurementUiState(
                roomId = 1,
                roomName = "Living",
                status = MeasurementUiStatus.Running(
                    elapsedMillis = 9_000L,
                    sampleCount = 3,
                    durationMillis = 30_000L,
                ),
            ),
            onMemoChange = {},
            onStartClick = {},
            onCancelClick = {},
            onPermissionDenied = {},
        )
    }
}
