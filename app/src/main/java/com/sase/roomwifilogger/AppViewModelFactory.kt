package com.sase.roomwifilogger

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.sase.roomwifilogger.data.AppContainer
import com.sase.roomwifilogger.ui.history.HistoryViewModel
import com.sase.roomwifilogger.ui.measure.MeasurementViewModel
import com.sase.roomwifilogger.ui.rooms.RoomListViewModel

class AppViewModelFactory(
    private val appContainer: AppContainer,
    private val roomId: Long? = null,
    private val roomName: String? = null,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        when {
            modelClass.isAssignableFrom(RoomListViewModel::class.java) ->
                RoomListViewModel(appContainer.roomRepository) as T
            modelClass.isAssignableFrom(MeasurementViewModel::class.java) ->
                MeasurementViewModel(
                    roomId = requireNotNull(roomId) { "roomId is required for MeasurementViewModel." },
                    roomName = requireNotNull(roomName) { "roomName is required for MeasurementViewModel." },
                    preconditionChecker = appContainer.preconditionChecker,
                    measurementEngine = appContainer.measurementEngine,
                ) as T
            modelClass.isAssignableFrom(HistoryViewModel::class.java) ->
                HistoryViewModel(
                    roomRepository = appContainer.roomRepository,
                    measurementRepository = appContainer.measurementRepository,
                    csvExporter = appContainer.csvExporter,
                ) as T
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
}
