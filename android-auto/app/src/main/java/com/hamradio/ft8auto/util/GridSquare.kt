package com.hamradio.ft8auto.util

import kotlin.math.*

/**
 * Utility for converting Maidenhead grid squares to/from lat/lon coordinates
 * and calculating distances between locations
 */
object GridSquare {
    
    /**
     * Convert a Maidenhead grid square to latitude/longitude
     * Supports 4-character (e.g., FN42) and 6-character (e.g., FN42ab) grid squares
     * Returns the center point of the grid square
     */
    fun toLatLon(grid: String): Pair<Double, Double>? {
        if (grid.length !in 4..6) return null
        
        val normalized = grid.uppercase()
        
        try {
            // First pair: Field (20° lon x 10° lat)
            val lonField = (normalized[0] - 'A') * 20.0
            val latField = (normalized[1] - 'A') * 10.0
            
            // Second pair: Square (2° lon x 1° lat)
            val lonSquare = (normalized[2] - '0') * 2.0
            val latSquare = (normalized[3] - '0') * 1.0
            
            var lon = lonField + lonSquare - 180.0
            var lat = latField + latSquare - 90.0
            
            // Third pair (if present): Subsquare (5' lon x 2.5' lat)
            if (normalized.length >= 6) {
                val lonSubsquare = (normalized[4] - 'A') * (2.0 / 24.0)
                val latSubsquare = (normalized[5] - 'A') * (1.0 / 24.0)
                lon += lonSubsquare
                lat += latSubsquare
                // Center of subsquare
                lon += 1.0 / 24.0
                lat += 1.0 / 48.0
            } else {
                // Center of 4-character grid square
                lon += 1.0
                lat += 0.5
            }
            
            return Pair(lat, lon)
        } catch (e: Exception) {
            return null
        }
    }
    
    /**
     * Calculate the great circle distance between two lat/lon points in kilometers
     * Uses the Haversine formula
     */
    fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadiusKm = 6371.0
        
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        
        val lat1Rad = Math.toRadians(lat1)
        val lat2Rad = Math.toRadians(lat2)
        
        val a = sin(dLat / 2).pow(2) +
                sin(dLon / 2).pow(2) * cos(lat1Rad) * cos(lat2Rad)
        val c = 2 * asin(sqrt(a))
        
        return earthRadiusKm * c
    }
    
    /**
     * Calculate distance from current location to a grid square in kilometers
     */
    fun calculateDistanceToGrid(myLat: Double, myLon: Double, targetGrid: String): Double? {
        val targetLatLon = toLatLon(targetGrid) ?: return null
        return calculateDistance(myLat, myLon, targetLatLon.first, targetLatLon.second)
    }
    
    /**
     * Format distance in appropriate units (km or miles)
     * @param distanceKm distance in kilometers
     * @param useMiles if true, convert to miles
     */
    fun formatDistance(distanceKm: Double, useMiles: Boolean = false): String {
        return if (useMiles) {
            val miles = distanceKm * 0.621371
            String.format("%.1f mi", miles)
        } else {
            String.format("%.1f km", distanceKm)
        }
    }
    
    /**
     * Calculate bearing from one point to another in degrees (0-360)
     */
    fun calculateBearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val lat1Rad = Math.toRadians(lat1)
        val lat2Rad = Math.toRadians(lat2)
        val dLon = Math.toRadians(lon2 - lon1)
        
        val y = sin(dLon) * cos(lat2Rad)
        val x = cos(lat1Rad) * sin(lat2Rad) - sin(lat1Rad) * cos(lat2Rad) * cos(dLon)
        
        val bearingRad = atan2(y, x)
        val bearingDeg = Math.toDegrees(bearingRad)
        
        return (bearingDeg + 360) % 360
    }
}
