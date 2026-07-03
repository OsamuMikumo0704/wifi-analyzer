package com.sase.roomwifilogger.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "rooms",
    indices = [
        Index(value = ["name"], unique = true),
    ],
)
data class RoomEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
)

@Entity(
    tableName = "measurements",
    foreignKeys = [
        ForeignKey(
            entity = RoomEntity::class,
            parentColumns = ["id"],
            childColumns = ["room_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["room_id"]),
        Index(value = ["measured_at"]),
    ],
)
data class MeasurementEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "room_id")
    val roomId: Long,
    @ColumnInfo(name = "measured_at")
    val measuredAt: Long,
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
