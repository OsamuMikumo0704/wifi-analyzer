package com.sase.roomwifilogger.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.sase.roomwifilogger.data.db.AppDatabase
import com.sase.roomwifilogger.data.db.MeasurementEntity
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RoomRepositoryIntegrationTest {
    private lateinit var database: AppDatabase
    private lateinit var repository: RoomRepository

    @Before
    fun createDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = DefaultRoomRepository(database.roomDao(), clockMillis = { 1L })
    }

    @After
    @Throws(IOException::class)
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun renameRoomKeepsExistingMeasurementsRelatedToSameRoom() = runTest {
        val roomId = repository.addRoom("Living").getOrThrow()
        database.measurementDao().insert(sampleMeasurement(roomId, measuredAt = 2L))

        repository.renameRoom(roomId, "Kitchen").getOrThrow()

        assertEquals(1, database.measurementDao().countByRoom(roomId))
        assertEquals(listOf("Kitchen"), database.measurementDao().getAllForExport().map { it.roomName })
    }

    @Test
    fun addRoomTrimsNameAndRejectsBlankOrDuplicateNames() = runTest {
        val roomId = repository.addRoom("  Living  ").getOrThrow()

        val blankResult = repository.addRoom("   ")
        val duplicateResult = repository.addRoom("living")

        assertEquals(listOf("Living"), database.roomDao().getAll().map { it.name })
        assertEquals(roomId, database.roomDao().getAll().single().id)
        assertTrue(blankResult.exceptionOrNull() is RoomNameError.Empty)
        assertTrue(duplicateResult.exceptionOrNull() is RoomNameError.Duplicate)
    }

    @Test
    fun renameRoomRejectsBlankOrDuplicateNames() = runTest {
        val livingId = repository.addRoom("Living").getOrThrow()
        repository.addRoom("Kitchen").getOrThrow()

        val blankResult = repository.renameRoom(livingId, " ")
        val duplicateResult = repository.renameRoom(livingId, "kitchen")

        assertTrue(blankResult.exceptionOrNull() is RoomNameError.Empty)
        assertTrue(duplicateResult.exceptionOrNull() is RoomNameError.Duplicate)
        assertEquals("Living", database.roomDao().getById(livingId)?.name)
    }

    @Test
    fun deleteRoomDelegatesCascadeToDatabase() = runTest {
        val roomId = repository.addRoom("Living").getOrThrow()
        database.measurementDao().insert(sampleMeasurement(roomId, measuredAt = 2L))

        repository.deleteRoom(roomId)

        assertEquals(0, database.measurementDao().countByRoom(roomId))
    }

    private fun sampleMeasurement(
        roomId: Long,
        measuredAt: Long,
    ): MeasurementEntity =
        MeasurementEntity(
            roomId = roomId,
            measuredAt = measuredAt,
            ssid = "wifi",
            bssid = "00:11:22:33:44:55",
            band = "5GHz",
            avgRssi = -50.0,
            minRssi = -60,
            maxRssi = -40,
            sampleCount = 10,
            linkSpeedMbps = 866,
            memo = "",
        )
}
