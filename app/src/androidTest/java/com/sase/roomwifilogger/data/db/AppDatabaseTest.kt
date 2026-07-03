package com.sase.roomwifilogger.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import java.io.IOException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class AppDatabaseTest {
    private lateinit var database: AppDatabase

    @Before
    fun createDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    @Throws(IOException::class)
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun deletingRoomCascadesMeasurements() = runTest {
        val roomId = database.roomDao().insert(RoomEntity(name = "Living", createdAt = 1L))
        database.measurementDao().insert(sampleMeasurement(roomId, measuredAt = 2L))

        database.roomDao().delete(RoomEntity(id = roomId, name = "Living", createdAt = 1L))

        assertEquals(0, database.measurementDao().countByRoom(roomId))
    }

    @Test
    fun historyIsNewestFirstAndCanFilterByRoom() = runTest {
        val livingId = database.roomDao().insert(RoomEntity(name = "Living", createdAt = 1L))
        val kitchenId = database.roomDao().insert(RoomEntity(name = "Kitchen", createdAt = 2L))
        database.measurementDao().insert(sampleMeasurement(livingId, measuredAt = 10L, avgRssi = -52.0))
        database.measurementDao().insert(sampleMeasurement(kitchenId, measuredAt = 30L, avgRssi = -62.0))
        database.measurementDao().insert(sampleMeasurement(livingId, measuredAt = 20L, avgRssi = -48.0))

        val allHistory = database.measurementDao().observeHistory().first()
        val livingHistory = database.measurementDao().observeHistory(livingId).first()

        assertEquals(listOf(30L, 20L, 10L), allHistory.map { it.measuredAt })
        assertEquals(listOf(20L, 10L), livingHistory.map { it.measuredAt })
        assertEquals(listOf("Living", "Living"), livingHistory.map { it.roomName })
    }

    @Test
    fun exportRowsIncludeRoomNameAndNewestFirst() = runTest {
        val livingId = database.roomDao().insert(RoomEntity(name = "Living", createdAt = 1L))
        database.measurementDao().insert(sampleMeasurement(livingId, measuredAt = 10L, ssid = "wifi-a"))
        database.measurementDao().insert(sampleMeasurement(livingId, measuredAt = 20L, ssid = "wifi-b"))

        val exportRows = database.measurementDao().getAllForExport()

        assertEquals(listOf(20L, 10L), exportRows.map { it.measuredAt })
        assertEquals(listOf("Living", "Living"), exportRows.map { it.roomName })
        assertEquals(listOf("wifi-b", "wifi-a"), exportRows.map { it.ssid })
    }

    private fun sampleMeasurement(
        roomId: Long,
        measuredAt: Long,
        avgRssi: Double = -50.0,
        ssid: String = "wifi",
    ): MeasurementEntity =
        MeasurementEntity(
            roomId = roomId,
            measuredAt = measuredAt,
            ssid = ssid,
            bssid = "00:11:22:33:44:55",
            band = "5GHz",
            avgRssi = avgRssi,
            minRssi = -60,
            maxRssi = -40,
            sampleCount = 10,
            linkSpeedMbps = 866,
            memo = "",
        )
}
