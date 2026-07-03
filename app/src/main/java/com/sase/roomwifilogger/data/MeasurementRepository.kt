package com.sase.roomwifilogger.data

import com.sase.roomwifilogger.data.db.ExportRow
import com.sase.roomwifilogger.data.db.HistoryItem
import com.sase.roomwifilogger.data.db.MeasurementDao
import com.sase.roomwifilogger.data.db.MeasurementEntity
import kotlinx.coroutines.flow.Flow

interface MeasurementRepository {
    suspend fun insert(record: MeasurementEntity): Long
    fun observeHistory(roomId: Long? = null): Flow<List<HistoryItem>>
    suspend fun getAllForExport(): List<ExportRow>
}

class DefaultMeasurementRepository(
    private val measurementDao: MeasurementDao,
) : MeasurementRepository {
    override suspend fun insert(record: MeasurementEntity): Long =
        measurementDao.insert(record)

    override fun observeHistory(roomId: Long?): Flow<List<HistoryItem>> =
        measurementDao.observeHistory(roomId)

    override suspend fun getAllForExport(): List<ExportRow> =
        measurementDao.getAllForExport()
}
