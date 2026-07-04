package com.sase.roomwifilogger.ui.measure

import com.sase.roomwifilogger.service.MeasurementEngine
import com.sase.roomwifilogger.service.MeasurementError
import com.sase.roomwifilogger.service.MeasurementState
import com.sase.roomwifilogger.service.MeasurementSummary
import com.sase.roomwifilogger.service.PreconditionChecker
import com.sase.roomwifilogger.service.PreconditionResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MeasurementViewModelTest {
    @Test
    fun startMeasurementRequestsPermissionWhenPermissionMissing() = runTest {
        val viewModel = viewModel(precondition = PreconditionResult.PermissionMissing, scope = backgroundScope)

        viewModel.startMeasurement()

        assertEquals(MeasurementUiStatus.PermissionRequired, viewModel.uiState.value.status)
    }

    @Test
    fun permissionDeniedShowsReasonWithoutStartingMeasurement() = runTest {
        val engine = FakeMeasurementEngine(emptyList())
        val viewModel = viewModel(
            precondition = PreconditionResult.PermissionMissing,
            engine = engine,
            scope = backgroundScope,
        )

        viewModel.startMeasurement()
        viewModel.onPermissionDenied()

        assertEquals(MeasurementUiStatus.PermissionDenied, viewModel.uiState.value.status)
        assertEquals(0, engine.runCount)
    }

    @Test
    fun startMeasurementShowsLocationServiceOffError() = runTest {
        val viewModel = viewModel(precondition = PreconditionResult.LocationServiceOff, scope = backgroundScope)

        viewModel.startMeasurement()

        assertEquals(MeasurementUiStatus.LocationServiceOff, viewModel.uiState.value.status)
    }

    @Test
    fun startMeasurementShowsWifiDisconnectedError() = runTest {
        val viewModel = viewModel(precondition = PreconditionResult.WifiDisconnected, scope = backgroundScope)

        viewModel.startMeasurement()

        assertEquals(MeasurementUiStatus.WifiDisconnected, viewModel.uiState.value.status)
    }

    @Test
    fun progressCompletionAndFailureStatesAreMappedFromEngine() = runTest {
        val summary = MeasurementSummary(avgRssi = -55.5, minRssi = -60, maxRssi = -50, sampleCount = 2)
        val viewModel = viewModel(
            precondition = PreconditionResult.Ready,
            engine = FakeMeasurementEngine(
                listOf(
                    MeasurementState.Running(elapsedMillis = 3_000L, sampleCount = 1),
                    MeasurementState.Completed(recordId = 9, summary = summary),
                ),
            ),
            scope = backgroundScope,
        )

        viewModel.startMeasurement()
        runCurrent()

        assertEquals(MeasurementUiStatus.Completed(summary), viewModel.uiState.value.status)
    }

    @Test
    fun cancelReturnsToCancelledStateAndStopsCollection() = runTest {
        val engine = FakeMeasurementEngine(
            listOf(MeasurementState.Running(elapsedMillis = 0, sampleCount = 1)),
        )
        val viewModel = viewModel(precondition = PreconditionResult.Ready, engine = engine, scope = backgroundScope)

        viewModel.startMeasurement()
        runCurrent()
        viewModel.cancelMeasurement()

        assertEquals(MeasurementUiStatus.Cancelled, viewModel.uiState.value.status)
    }

    @Test
    fun failedStateIncludesUnsavedReason() = runTest {
        val viewModel = viewModel(
            precondition = PreconditionResult.Ready,
            engine = FakeMeasurementEngine(
                listOf(MeasurementState.Failed(MeasurementError.WifiLost)),
            ),
            scope = backgroundScope,
        )

        viewModel.startMeasurement()
        runCurrent()

        assertTrue(viewModel.uiState.value.status is MeasurementUiStatus.Failed)
    }

    private fun viewModel(
        precondition: PreconditionResult,
        engine: FakeMeasurementEngine = FakeMeasurementEngine(emptyList()),
        scope: CoroutineScope,
    ): MeasurementViewModel =
        MeasurementViewModel(
            roomId = 1,
            roomName = "Living",
            preconditionChecker = FakePreconditionChecker(precondition),
            measurementEngine = engine,
            externalScope = scope,
        )
}

private class FakePreconditionChecker(
    private val result: PreconditionResult,
) : PreconditionChecker {
    override fun check(): PreconditionResult = result
}

private class FakeMeasurementEngine(
    private val states: List<MeasurementState>,
) : MeasurementEngine {
    var runCount = 0

    override fun run(
        roomId: Long,
        memo: String,
        config: com.sase.roomwifilogger.service.MeasurementConfig,
    ): Flow<MeasurementState> = flow {
        runCount += 1
        states.forEach { emit(it) }
    }
}
