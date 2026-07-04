package com.sase.roomwifilogger

import org.junit.Assert.assertEquals
import org.junit.Test

class AppRoutesTest {
    @Test
    fun measurementRouteEncodesRoomNameAsSinglePathSegment() {
        val route = AppRoutes.measurementRoute(roomId = 42L, roomName = "Living / North")

        assertEquals("measure/42/Living%20%2F%20North", route)
    }

    @Test
    fun decodeRouteArgumentRestoresEncodedRoomName() {
        val decoded = AppRoutes.decodeRouteArgument("Living%20%2F%20North")

        assertEquals("Living / North", decoded)
    }
}
