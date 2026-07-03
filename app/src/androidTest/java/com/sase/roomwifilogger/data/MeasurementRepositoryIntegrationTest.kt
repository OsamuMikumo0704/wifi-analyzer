package com.sase.roomwifilogger.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.sase.roomwifilogger.data.db.AppDatabase
import com.sase.roomwifilogger.data.db.MeasurementEntity
import com.sase.roomwifilogger.data.db.RoomEntity
import java.io.IOException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class MeasurementRepositoryIntegrationTest {
    private lateinit var database: AppDatabase
    private lateinit var repository: MeasurementRepository

    @Before
    fun createDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = DefaultMeasurementRepository(database.measurementDao())
    }

    @After
    @Throws(IOException::class)
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun insertAndObserveHistoryNewestFirstWithRoomFilter() = runTest {
        val livingId = database.roomDao().insert(RoomEntity(name = "Living", createdAt = 1L))
        val kitchenId = database.roomDao().insert(RoomEntity(name = "Kitchen", createdAt = 2L))

        repository.insert(sampleMeasurement(livingId, measuredAt = 10L, avgRssi = -52.0))
        repository.insert(sampleMeasurement(kitchenId, measuredAt = 30L, avgRssi = -62.0))
        repository.insert(sampleMeasurement(livingId, measuredAt = 20L, avgRssi = -48.0))

        val allHistory = repository.observeHistory().first()
        val livingHistory = repository.observeHistory(livingId).first()

        assertEquals(listOf(30L, 20L, 10L), allHistory.map { it.measuredAt })
        assertEquals(listOf(20L, 10L), livingHistory.map { it.measuredAt })
        assertEquals(listOf("Living", "Living"), livingHistory.map { it.roomName })
    }

    @Test
    fun getAllForExportReturnsJoinedRowsInStableMeasurementOrder() = runTest {
        val roomId = database.roomDao().insert(RoomEntity(name = "Living", createdAt = 1L))
        val firstId = repository.insert(sampleMeasurement(roomId, measuredAt = 10L, ssid = "wifi-a"))
        val secondId = repository.insert(sampleMeasurement(roomId, measuredAt = 10L, ssid = "wifi-b"))

        val exportRows = repository.getAllForExport()

        assertEquals(listOf(secondId, firstId).size, exportRows.size)
        assertEquals(listOf("wifi-b", "wifi-a"), exportRows.map { it.ssid })
        assertEquals(listOf("Living", "Living"), exportRows.map { it.roomName })
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
