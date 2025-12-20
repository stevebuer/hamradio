package com.hamradio.ft8auto.model

import com.hamradio.ft8auto.util.GridSquare
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
    
    /**
     * Calculate distance to this station from given location
     * @param myLat current latitude
     * @param myLon current longitude
     * @return distance in kilometers, or null if grid is invalid or empty
     */
    fun calculateDistance(myLat: Double, myLon: Double): Double? {
        if (grid.isEmpty()) return null
        return GridSquare.calculateDistanceToGrid(myLat, myLon, grid)
    }
    
    /**
     * Get formatted display text with optional distance
     * @param myLat current latitude (optional)
     * @param myLon current longitude (optional)
     * @param useMiles if true, show distance in miles instead of km
     */
    fun getDisplayText(myLat: Double? = null, myLon: Double? = null, useMiles: Boolean = false): String {
        val baseText = String.format(
            Locale.US,
            "%s | %s | SNR:%+d",
            formattedTime,
            callsign,
            snr
        )
        
        val distanceText = if (myLat != null && myLon != null && grid.isNotEmpty()) {
            calculateDistance(myLat, myLon)?.let { distKm ->
                " | ${GridSquare.formatDistance(distKm, useMiles)}"
            } ?: ""
        } else {
            ""
        }
        
        return "$baseText$distanceText | $message"
    }
    
    val displayText: String
        get() = getDisplayText()
    
    override fun toString(): String = displayText
}
