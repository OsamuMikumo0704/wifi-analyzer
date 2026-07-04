package com.sase.roomwifilogger.service

import android.content.ContentResolver
import android.content.ContentValues
import android.os.Environment
import android.provider.MediaStore
import com.sase.roomwifilogger.data.MeasurementRepository
import com.sase.roomwifilogger.data.db.ExportRow
import java.nio.charset.StandardCharsets
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class ExportResult(val fileName: String, val rowCount: Int)

sealed interface ExportError {
    data object NoRecords : ExportError
    data class WriteFailed(val cause: Throwable) : ExportError
}

class ExportException(val error: ExportError) : Exception(error.toString())

interface CsvExporter {
    suspend fun exportAll(): Result<ExportResult>
}

interface CsvFileWriter {
    fun write(fileName: String, bytes: ByteArray)
}

class DefaultCsvExporter(
    private val repository: MeasurementRepository,
    private val writer: CsvFileWriter,
    private val clock: Clock = Clock.systemDefaultZone(),
    private val zoneId: ZoneId = ZoneId.systemDefault(),
) : CsvExporter {
    override suspend fun exportAll(): Result<ExportResult> {
        val rows = repository.getAllForExport()
        if (rows.isEmpty()) {
            return Result.failure(ExportException(ExportError.NoRecords))
        }

        val fileName = "wifi_log_${DateTimeFormatter.ISO_LOCAL_DATE.format(clock.instant().atZone(zoneId))}.csv"
        val bytes = CsvGenerator.toUtf8BomCsv(rows.sortedBy { it.measuredAt }, zoneId)

        return try {
            writer.write(fileName, bytes)
            Result.success(ExportResult(fileName = fileName, rowCount = rows.size))
        } catch (error: Throwable) {
            Result.failure(ExportException(ExportError.WriteFailed(error)))
        }
    }
}

class MediaStoreCsvFileWriter(
    private val contentResolver: ContentResolver,
) : CsvFileWriter {
    override fun write(fileName: String, bytes: ByteArray) {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }

        val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: throw IllegalStateException("Failed to create Downloads entry")

        try {
            contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(bytes)
            } ?: throw IllegalStateException("Failed to open output stream")

            val completedValues = ContentValues().apply {
                put(MediaStore.MediaColumns.IS_PENDING, 0)
            }
            contentResolver.update(uri, completedValues, null, null)
        } catch (error: Throwable) {
            contentResolver.delete(uri, null, null)
            throw error
        }
    }
}

object CsvGenerator {
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
    private val header = listOf(
        "\u6e2c\u5b9a\u65e5",
        "\u6e2c\u5b9a\u6642\u523b",
        "\u90e8\u5c4b\u540d",
        "SSID",
        "BSSID",
        "\u5468\u6ce2\u6570\u5e2f",
        "\u5e73\u5747 RSSI",
        "\u6700\u5c0f RSSI",
        "\u6700\u5927 RSSI",
        "\u30b5\u30f3\u30d7\u30eb\u6570",
        "\u30ea\u30f3\u30af\u901f\u5ea6",
        "\u30e1\u30e2",
    )

    fun toUtf8BomCsv(rows: List<ExportRow>, zoneId: ZoneId): ByteArray {
        val csv = buildString {
            append(header.toCsvLine())
            append("\r\n")
            rows.forEach { row ->
                append(row.toColumns(zoneId).toCsvLine())
                append("\r\n")
            }
        }

        return byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) +
            csv.toByteArray(StandardCharsets.UTF_8)
    }

    private fun ExportRow.toColumns(zoneId: ZoneId): List<String> {
        val measuredAt = Instant.ofEpochMilli(measuredAt).atZone(zoneId)
        return listOf(
            dateFormatter.format(measuredAt),
            timeFormatter.format(measuredAt),
            roomName,
            ssid,
            bssid,
            band,
            avgRssi.toString(),
            minRssi.toString(),
            maxRssi.toString(),
            sampleCount.toString(),
            linkSpeedMbps.toString(),
            memo,
        )
    }

    private fun List<String>.toCsvLine(): String =
        joinToString(separator = ",") { it.escapeCsvValue() }

    private fun String.escapeCsvValue(): String {
        val requiresQuotes = any { it == ',' || it == '"' || it == '\r' || it == '\n' }
        if (!requiresQuotes) return this
        return "\"" + replace("\"", "\"\"") + "\""
    }
}
