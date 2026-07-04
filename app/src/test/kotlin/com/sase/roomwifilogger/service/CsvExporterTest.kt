package com.sase.roomwifilogger.service

import com.sase.roomwifilogger.data.MeasurementRepository
import com.sase.roomwifilogger.data.db.ExportRow
import com.sase.roomwifilogger.data.db.HistoryItem
import com.sase.roomwifilogger.data.db.MeasurementEntity
import java.nio.charset.StandardCharsets
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvExporterTest {
    private val clock = Clock.fixed(Instant.parse("2026-07-03T12:00:00Z"), ZoneId.of("Asia/Tokyo"))

    @Test
    fun exportAllWritesBomHeaderAndEscapedRows() = runTest {
        val repository = FakeExportRepository(
            rows = listOf(
                ExportRow(
                    measuredAt = Instant.parse("2026-07-03T01:02:03Z").toEpochMilli(),
                    roomName = "Living, \"East\"",
                    ssid = "Home\nWiFi",
                    bssid = "00:11:22:33:44:55",
                    band = "5GHz",
                    avgRssi = -53.7,
                    minRssi = -60,
                    maxRssi = -50,
                    sampleCount = 3,
                    linkSpeedMbps = 300,
                    memo = "line1\r\nline2",
                ),
            ),
        )
        val writer = CapturingCsvFileWriter()
        val exporter = DefaultCsvExporter(repository, writer, clock, ZoneId.of("Asia/Tokyo"))

        val result = exporter.exportAll()

        assertTrue(result.isSuccess)
        assertEquals(ExportResult(fileName = "wifi_log_2026-07-03.csv", rowCount = 1), result.getOrThrow())
        assertArrayEquals(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()), writer.bytes.take(3).toByteArray())
        val csv = writer.bytes.drop(3).toByteArray().toString(StandardCharsets.UTF_8)
        assertTrue(
            csv.startsWith(
                "\u6e2c\u5b9a\u65e5,\u6e2c\u5b9a\u6642\u523b,\u90e8\u5c4b\u540d,SSID,BSSID," +
                    "\u5468\u6ce2\u6570\u5e2f,\u5e73\u5747 RSSI,\u6700\u5c0f RSSI," +
                    "\u6700\u5927 RSSI,\u30b5\u30f3\u30d7\u30eb\u6570,\u30ea\u30f3\u30af\u901f\u5ea6,\u30e1\u30e2\r\n",
            ),
        )
        assertTrue(csv.contains("2026-07-03,10:02:03,\"Living, \"\"East\"\"\",\"Home\nWiFi\",00:11:22:33:44:55,5GHz,-53.7,-60,-50,3,300,\"line1\r\nline2\""))
    }

    @Test
    fun exportAllReturnsNoRecordsAndDoesNotWriteWhenRepositoryIsEmpty() = runTest {
        val writer = CapturingCsvFileWriter()
        val exporter = DefaultCsvExporter(
            repository = FakeExportRepository(emptyList()),
            writer = writer,
            clock = clock,
            zoneId = ZoneId.of("Asia/Tokyo"),
        )

        val result = exporter.exportAll()

        assertTrue(result.isFailure)
        assertEquals(ExportError.NoRecords, (result.exceptionOrNull() as ExportException).error)
        assertTrue(writer.bytes.isEmpty())
    }

    @Test
    fun exportAllWritesRowsInMeasuredAtAscendingOrder() = runTest {
        val newer = exportRow(
            measuredAt = Instant.parse("2026-07-03T02:00:00Z").toEpochMilli(),
            ssid = "newer",
        )
        val older = exportRow(
            measuredAt = Instant.parse("2026-07-03T01:00:00Z").toEpochMilli(),
            ssid = "older",
        )
        val writer = CapturingCsvFileWriter()
        val exporter = DefaultCsvExporter(
            repository = FakeExportRepository(listOf(newer, older)),
            writer = writer,
            clock = clock,
            zoneId = ZoneId.of("Asia/Tokyo"),
        )

        exporter.exportAll().getOrThrow()

        val csv = writer.bytes.drop(3).toByteArray().toString(StandardCharsets.UTF_8)
        assertTrue(csv.indexOf("older") < csv.indexOf("newer"))
    }
}

private fun exportRow(
    measuredAt: Long,
    ssid: String,
): ExportRow =
    ExportRow(
        measuredAt = measuredAt,
        roomName = "Room",
        ssid = ssid,
        bssid = "00:11:22:33:44:55",
        band = "5GHz",
        avgRssi = -50.0,
        minRssi = -60,
        maxRssi = -40,
        sampleCount = 3,
        linkSpeedMbps = 300,
        memo = "",
    )

private class FakeExportRepository(
    private val rows: List<ExportRow>,
) : MeasurementRepository {
    override suspend fun insert(record: MeasurementEntity): Long =
        throw NotImplementedError()

    override fun observeHistory(roomId: Long?): Flow<List<HistoryItem>> =
        throw NotImplementedError()

    override suspend fun getAllForExport(): List<ExportRow> = rows
}

private class CapturingCsvFileWriter : CsvFileWriter {
    var bytes: ByteArray = byteArrayOf()
        private set

    override fun write(fileName: String, bytes: ByteArray) {
        this.bytes = bytes
    }
}
