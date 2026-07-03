package com.sase.roomwifilogger.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MeasurementDao {
    @Insert
    suspend fun insert(measurement: MeasurementEntity): Long

    @Query(
        """
        SELECT
            measurements.id AS id,
            measurements.room_id AS room_id,
            rooms.name AS room_name,
            measurements.measured_at AS measured_at,
            measurements.avg_rssi AS avg_rssi
        FROM measurements
        INNER JOIN rooms ON rooms.id = measurements.room_id
        WHERE (:roomId IS NULL OR measurements.room_id = :roomId)
        ORDER BY measurements.measured_at DESC, measurements.id DESC
        """,
    )
    fun observeHistory(roomId: Long? = null): Flow<List<HistoryItem>>

    @Query(
        """
        SELECT
            measurements.measured_at AS measured_at,
            rooms.name AS room_name,
            measurements.ssid AS ssid,
            measurements.bssid AS bssid,
            measurements.band AS band,
            measurements.avg_rssi AS avg_rssi,
            measurements.min_rssi AS min_rssi,
            measurements.max_rssi AS max_rssi,
            measurements.sample_count AS sample_count,
            measurements.link_speed_mbps AS link_speed_mbps,
            measurements.memo AS memo
        FROM measurements
        INNER JOIN rooms ON rooms.id = measurements.room_id
        ORDER BY measurements.measured_at DESC, measurements.id DESC
        """,
    )
    suspend fun getAllForExport(): List<ExportRow>

    @Query("SELECT COUNT(*) FROM measurements WHERE room_id = :roomId")
    suspend fun countByRoom(roomId: Long): Int
}
