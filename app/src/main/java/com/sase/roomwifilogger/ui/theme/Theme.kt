package com.sase.roomwifilogger.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF1D7A8C),
    secondary = Color(0xFF6A6F28),
    tertiary = Color(0xFF8A4B2A),
)

@Composable
fun RoomWifiLoggerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        content = content,
    )
}
