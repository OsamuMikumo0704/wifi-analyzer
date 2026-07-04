package com.sase.roomwifilogger

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sase.roomwifilogger.data.AppContainer
import com.sase.roomwifilogger.ui.history.HistoryRoute
import com.sase.roomwifilogger.ui.history.HistoryViewModel
import com.sase.roomwifilogger.ui.measure.MeasurementRoute
import com.sase.roomwifilogger.ui.measure.MeasurementViewModel
import com.sase.roomwifilogger.ui.rooms.RoomListNavigation
import com.sase.roomwifilogger.ui.rooms.RoomListRoute
import com.sase.roomwifilogger.ui.rooms.RoomListViewModel
import com.sase.roomwifilogger.ui.theme.RoomWifiLoggerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val appContainer = (application as RoomWifiLoggerApp).appContainer
        setContent {
            RoomWifiLoggerTheme {
                AppRoot(appContainer)
            }
        }
    }
}

@Composable
private fun AppRoot(appContainer: AppContainer) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = AppRoutes.Rooms,
    ) {
        composable(AppRoutes.Rooms) {
            val viewModel: RoomListViewModel = viewModel(
                factory = AppViewModelFactory(appContainer),
            )
            RoomListRoute(
                viewModel = viewModel,
                onNavigate = { navigation ->
                    when (navigation) {
                        RoomListNavigation.History -> navController.navigate(AppRoutes.History)
                        is RoomListNavigation.Measurement -> navController.navigate(
                            AppRoutes.measurementRoute(navigation.roomId, navigation.roomName),
                        )
                    }
                },
            )
        }
        composable(
            route = AppRoutes.MeasurementPattern,
            arguments = listOf(
                navArgument("roomId") { type = NavType.LongType },
                navArgument("roomName") { type = NavType.StringType },
            ),
        ) { backStackEntry ->
            val roomId = requireNotNull(backStackEntry.arguments?.getLong("roomId"))
            val roomName = AppRoutes.decodeRouteArgument(
                requireNotNull(backStackEntry.arguments?.getString("roomName")),
            )
            val viewModel: MeasurementViewModel = viewModel(
                key = "measurement-$roomId-$roomName",
                factory = AppViewModelFactory(
                    appContainer = appContainer,
                    roomId = roomId,
                    roomName = roomName,
                ),
            )
            MeasurementRoute(viewModel)
        }
        composable(AppRoutes.History) {
            val viewModel: HistoryViewModel = viewModel(
                factory = AppViewModelFactory(appContainer),
            )
            HistoryRoute(viewModel)
        }
    }
}

@Composable
fun RoomWifiLoggerAppRoot(appContainer: AppContainer) {
    RoomWifiLoggerTheme {
        AppRoot(appContainer)
    }
}
