package com.sase.roomwifilogger.data

import android.content.Context
import android.net.ConnectivityManager
import com.sase.roomwifilogger.data.db.AppDatabase
import com.sase.roomwifilogger.service.AndroidLocationServiceStatus
import com.sase.roomwifilogger.service.AndroidPermissionStatus
import com.sase.roomwifilogger.service.CsvExporter
import com.sase.roomwifilogger.service.DefaultCsvExporter
import com.sase.roomwifilogger.service.DefaultMeasurementEngine
import com.sase.roomwifilogger.service.DefaultPreconditionChecker
import com.sase.roomwifilogger.service.MeasurementEngine
import com.sase.roomwifilogger.service.MediaStoreCsvFileWriter
import com.sase.roomwifilogger.service.PreconditionChecker
import com.sase.roomwifilogger.wifi.AndroidWifiStatusMonitor
import com.sase.roomwifilogger.wifi.WifiStatusMonitor

class AppContainer(context: Context) {
    val database: AppDatabase = AppDatabase.create(context)
    val roomRepository: RoomRepository = DefaultRoomRepository(database.roomDao())
    val measurementRepository: MeasurementRepository = DefaultMeasurementRepository(database.measurementDao())
    val wifiStatusMonitor: WifiStatusMonitor = AndroidWifiStatusMonitor(
        context.getSystemService(ConnectivityManager::class.java),
    )
    val preconditionChecker: PreconditionChecker = DefaultPreconditionChecker(
        permissionStatus = AndroidPermissionStatus(context),
        locationServiceStatus = AndroidLocationServiceStatus(context),
        wifiStatusMonitor = wifiStatusMonitor,
    )
    val measurementEngine: MeasurementEngine = DefaultMeasurementEngine(
        wifiStatusMonitor = wifiStatusMonitor,
        measurementRepository = measurementRepository,
    )
    val csvExporter: CsvExporter = DefaultCsvExporter(
        repository = measurementRepository,
        writer = MediaStoreCsvFileWriter(context.contentResolver),
    )
}
