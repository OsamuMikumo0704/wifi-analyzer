package com.sase.roomwifilogger.wifi

import org.junit.Assert.assertEquals
import org.junit.Test

class BandConverterTest {
    @Test
    fun convertsKnownWifiFrequencyRangesToBandNames() {
        assertEquals("2.4GHz", frequencyMhzToBand(2412))
        assertEquals("2.4GHz", frequencyMhzToBand(2484))
        assertEquals("5GHz", frequencyMhzToBand(5180))
        assertEquals("5GHz", frequencyMhzToBand(5885))
        assertEquals("6GHz", frequencyMhzToBand(5955))
        assertEquals("6GHz", frequencyMhzToBand(7115))
    }

    @Test
    fun unknownFrequencyFallsBackToUnknownBand() {
        assertEquals("Unknown", frequencyMhzToBand(0))
        assertEquals("Unknown", frequencyMhzToBand(5900))
    }
}
