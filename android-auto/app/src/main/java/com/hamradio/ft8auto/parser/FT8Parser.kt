package com.hamradio.ft8auto.parser

import com.hamradio.ft8auto.model.FT8Decode

/**
 * Parses FT8 decode messages from various formats
 */
object FT8Parser {
    // Common FT8 decode format: "134500 -12  0.3 1234 ~ CQ K1ABC FN42"
    // Time, SNR, DT, Freq, Message
    private val WSJTX_PATTERN = Regex(
        """(\d{6})\s+([+-]?\d+)\s+([+-]?\d+\.\d+)\s+(\d+)\s+~?\s+(.+)"""
    )
    
    // Simple format: "CALLSIGN GRID SNR"
    private val SIMPLE_PATTERN = Regex(
        """([A-Z0-9/]+)\s+([A-Z]{2}\d{2})\s+([+-]?\d+)"""
    )
    
    private val CALLSIGN_PATTERN = Regex(
        """\b([A-Z0-9]{1,3}[0-9][A-Z0-9]{0,3}(?:/[A-Z0-9]+)?)\b"""
    )
    
    private val GRID_PATTERN = Regex(
        """\b([A-R]{2}[0-9]{2}(?:[A-X]{2})?)\b"""
    )
    
    fun parse(line: String?): FT8Decode? {
        if (line.isNullOrBlank()) return null
        
        val trimmedLine = line.trim()
        
        // Try WSJT-X format first
        WSJTX_PATTERN.matchEntire(trimmedLine)?.let { match ->
            try {
                val snr = match.groupValues[2].toInt()
                val freq = match.groupValues[4].toInt()
                val message = match.groupValues[5].trim()
                
                val callsign = extractCallsign(message)
                val grid = extractGrid(message)
                
                return FT8Decode(callsign, grid, snr, freq, message)
            } catch (e: NumberFormatException) {
                // Fall through to try other formats
            }
        }
        
        // Try simple format
        SIMPLE_PATTERN.matchEntire(trimmedLine)?.let { match ->
            try {
                val callsign = match.groupValues[1]
                val grid = match.groupValues[2]
                val snr = match.groupValues[3].toInt()
                
                return FT8Decode(callsign, grid, snr, 0, trimmedLine)
            } catch (e: NumberFormatException) {
                // Fall through
            }
        }
        
        // If no pattern matches, create a basic decode with the raw message
        return FT8Decode("UNKNOWN", "", 0, 0, trimmedLine)
    }
    
    private fun extractCallsign(message: String): String {
        val words = message.split("""\s+""".toRegex())
        
        // Skip first word if it's CQ, DE, etc.
        for (word in words) {
            if (word !in listOf("CQ", "DE", "TNX", "73")) {
                if (word.matches(Regex("[A-Z0-9]{1,3}[0-9][A-Z0-9]{0,3}(?:/[A-Z0-9]+)?"))) {
                    return word
                }
            }
        }
        
        return ""
    }
    
    private fun extractGrid(message: String): String {
        return GRID_PATTERN.find(message)?.groupValues?.get(1) ?: ""
    }
}
