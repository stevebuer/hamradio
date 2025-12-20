package com.hamradio.ft8auto.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*

/**
 * Manages GPS location tracking for the application
 */
class LocationTracker(private val context: Context) {
    
    interface LocationListener {
        fun onLocationChanged(latitude: Double, longitude: Double)
        fun onLocationError(error: String)
    }
    
    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var locationCallback: LocationCallback? = null
    private var listener: LocationListener? = null
    
    var currentLatitude: Double? = null
        private set
    var currentLongitude: Double? = null
        private set
    
    val hasLocation: Boolean
        get() = currentLatitude != null && currentLongitude != null
    
    fun setListener(listener: LocationListener) {
        this.listener = listener
    }
    
    /**
     * Check if location permissions are granted
     */
    fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Start location updates
     */
    fun startLocationUpdates() {
        if (!hasLocationPermission()) {
            listener?.onLocationError("Location permission not granted")
            return
        }
        
        if (fusedLocationClient == null) {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        }
        
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            30000L // 30 seconds
        ).apply {
            setMinUpdateIntervalMillis(10000L) // 10 seconds
            setMaxUpdateDelayMillis(60000L) // 1 minute
        }.build()
        
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    currentLatitude = location.latitude
                    currentLongitude = location.longitude
                    listener?.onLocationChanged(location.latitude, location.longitude)
                }
            }
            
            override fun onLocationAvailability(availability: LocationAvailability) {
                if (!availability.isLocationAvailable) {
                    listener?.onLocationError("Location not available")
                }
            }
        }
        
        try {
            fusedLocationClient?.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                Looper.getMainLooper()
            )
            
            // Also get last known location immediately
            fusedLocationClient?.lastLocation?.addOnSuccessListener { location: Location? ->
                location?.let {
                    currentLatitude = it.latitude
                    currentLongitude = it.longitude
                    listener?.onLocationChanged(it.latitude, it.longitude)
                }
            }
        } catch (e: SecurityException) {
            listener?.onLocationError("Security exception: ${e.message}")
        }
    }
    
    /**
     * Stop location updates
     */
    fun stopLocationUpdates() {
        locationCallback?.let {
            fusedLocationClient?.removeLocationUpdates(it)
        }
        locationCallback = null
    }
    
    companion object {
        const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }
}
