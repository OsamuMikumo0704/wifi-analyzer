package com.sase.roomwifilogger.data.db

import androidx.room.ColumnInfo

data class ExportRow(
    @ColumnInfo(name = "measured_at")
    val measuredAt: Long,
    @ColumnInfo(name = "room_name")
    val roomName: String,
    val ssid: String,
    val bssid: String,
    val band: String,
    @ColumnInfo(name = "avg_rssi")
    val avgRssi: Double,
    @ColumnInfo(name = "min_rssi")
    val minRssi: Int,
    @ColumnInfo(name = "max_rssi")
    val maxRssi: Int,
    @ColumnInfo(name = "sample_count")
    val sampleCount: Int,
    @ColumnInfo(name = "link_speed_mbps")
    val linkSpeedMbps: Int,
    val memo: String,
)
