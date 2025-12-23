package com.hamradio.ft8auto.util

import android.content.Context
import android.content.SharedPreferences

/**
 * Centralized preferences management for the FT8 Auto app
 */
object PreferencesManager {
    
    private const val PREFS_NAME = "ft8auto_prefs"
    
    // Key constants
    private const val KEY_TRACKER_HOST = "tracker_host"
    private const val KEY_TRACKER_PORT = "tracker_port"
    private const val KEY_USE_MILES = "use_miles"
    private const val KEY_GPS_UPLOAD_ENABLED = "gps_upload_enabled"
    private const val KEY_CURRENT_BAND = "current_band"
    private const val KEY_GPS_UPLOAD_INTERVAL = "gps_upload_interval"
    
    // Defaults
    private const val DEFAULT_HOST = "192.168.1.100"
    private const val DEFAULT_PORT = 8080
    private const val DEFAULT_USE_MILES = true
    private const val DEFAULT_GPS_UPLOAD_ENABLED = false
    private const val DEFAULT_CURRENT_BAND = "Select Band"
    private const val DEFAULT_GPS_UPLOAD_INTERVAL = 30
    
    private lateinit var prefs: SharedPreferences
    
    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    // Tracker Host
    fun getTrackerHost(): String {
        return prefs.getString(KEY_TRACKER_HOST, DEFAULT_HOST) ?: DEFAULT_HOST
    }
    
    fun setTrackerHost(host: String) {
        prefs.edit().putString(KEY_TRACKER_HOST, host).apply()
    }
    
    // Tracker Port
    fun getTrackerPort(): Int {
        return prefs.getInt(KEY_TRACKER_PORT, DEFAULT_PORT)
    }
    
    fun setTrackerPort(port: Int) {
        prefs.edit().putInt(KEY_TRACKER_PORT, port).apply()
    }
    
    // Units (miles vs km)
    fun getUseMiles(): Boolean {
        return prefs.getBoolean(KEY_USE_MILES, DEFAULT_USE_MILES)
    }
    
    fun setUseMiles(useMiles: Boolean) {
        prefs.edit().putBoolean(KEY_USE_MILES, useMiles).apply()
    }
    
    // GPS Upload Enabled
    fun isGpsUploadEnabled(): Boolean {
        return prefs.getBoolean(KEY_GPS_UPLOAD_ENABLED, DEFAULT_GPS_UPLOAD_ENABLED)
    }
    
    fun setGpsUploadEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_GPS_UPLOAD_ENABLED, enabled).apply()
    }
    
    // Current Band
    fun getCurrentBand(): String {
        return prefs.getString(KEY_CURRENT_BAND, DEFAULT_CURRENT_BAND) ?: DEFAULT_CURRENT_BAND
    }
    
    fun setCurrentBand(band: String) {
        prefs.edit().putString(KEY_CURRENT_BAND, band).apply()
    }
    
    // GPS Upload Interval (seconds)
    fun getGpsUploadInterval(): Int {
        return prefs.getInt(KEY_GPS_UPLOAD_INTERVAL, DEFAULT_GPS_UPLOAD_INTERVAL)
    }
    
    fun setGpsUploadInterval(seconds: Int) {
        prefs.edit().putInt(KEY_GPS_UPLOAD_INTERVAL, seconds).apply()
    }
    
    // Clear all preferences (for reset/logout)
    fun clearAll() {
        prefs.edit().clear().apply()
    }
}
