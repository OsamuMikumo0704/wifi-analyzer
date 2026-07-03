package com.sase.roomwifilogger.wifi

fun frequencyMhzToBand(frequencyMhz: Int): String =
    when (frequencyMhz) {
        in 2400..2500 -> "2.4GHz"
        in 4900..5895 -> "5GHz"
        in 5925..7125 -> "6GHz"
        else -> "Unknown"
    }
