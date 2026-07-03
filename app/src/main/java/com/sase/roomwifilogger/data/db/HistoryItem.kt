package com.sase.roomwifilogger.data.db

import androidx.room.ColumnInfo

data class HistoryItem(
    val id: Long,
    @ColumnInfo(name = "room_id")
    val roomId: Long,
    @ColumnInfo(name = "room_name")
    val roomName: String,
    @ColumnInfo(name = "measured_at")
    val measuredAt: Long,
    @ColumnInfo(name = "avg_rssi")
    val avgRssi: Double,
)
