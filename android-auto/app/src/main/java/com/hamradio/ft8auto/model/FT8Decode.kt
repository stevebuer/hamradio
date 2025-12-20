package com.hamradio.ft8auto.model

import java.text.SimpleDateFormat
import java.util.*

/**
 * Represents a single FT8 decode message
 */
data class FT8Decode(
    val callsign: String,
    val grid: String,
    val snr: Int,
    val frequency: Int,
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
) {
    val formattedTime: String
        get() {
            val sdf = SimpleDateFormat("HH:mm:ss", Locale.US)
            return sdf.format(Date(timestamp))
        }
    
    val displayText: String
        get() = String.format(
            Locale.US,
            "%s | %s | SNR:%+d | %s",
            formattedTime,
            callsign,
            snr,
            message
        )
    
    override fun toString(): String = displayText
}
