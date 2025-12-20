package com.hamradio.ft8auto.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.hamradio.ft8auto.R
import com.hamradio.ft8auto.util.LocationTracker
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Background service that periodically uploads GPS location to tracker server
 */
class GPSUploadService : Service() {
    
    private lateinit var locationTracker: LocationTracker
    private var serviceJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    private var trackerHost: String = ""
    private var trackerPort: Int = 8081
    private var uploadInterval: Long = 30000 // 30 seconds default
    
    companion object {
        private const val TAG = "GPSUploadService"
        private const val NOTIFICATION_ID = 2001
        private const val CHANNEL_ID = "gps_upload_channel"
        
        const val ACTION_START = "com.hamradio.ft8auto.action.START_GPS_UPLOAD"
        const val ACTION_STOP = "com.hamradio.ft8auto.action.STOP_GPS_UPLOAD"
        const val EXTRA_HOST = "host"
        const val EXTRA_PORT = "port"
        const val EXTRA_INTERVAL = "interval"
        
        fun start(context: Context, host: String, port: Int = 8081, intervalSeconds: Int = 30) {
            val intent = Intent(context, GPSUploadService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_HOST, host)
                putExtra(EXTRA_PORT, port)
                putExtra(EXTRA_INTERVAL, intervalSeconds)
            }
            context.startForegroundService(intent)
        }
        
        fun stop(context: Context) {
            val intent = Intent(context, GPSUploadService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")
        
        // Initialize location tracker
        locationTracker = LocationTracker(this)
        
        // Create notification channel
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                trackerHost = intent.getStringExtra(EXTRA_HOST) ?: ""
                trackerPort = intent.getIntExtra(EXTRA_PORT, 8081)
                val intervalSeconds = intent.getIntExtra(EXTRA_INTERVAL, 30)
                uploadInterval = intervalSeconds * 1000L
                
                if (trackerHost.isNotEmpty()) {
                    startForeground(NOTIFICATION_ID, createNotification())
                    startLocationTracking()
                    startUploadLoop()
                    Log.i(TAG, "GPS upload started to $trackerHost:$trackerPort every ${intervalSeconds}s")
                } else {
                    Log.e(TAG, "No host specified, stopping service")
                    stopSelf()
                }
            }
            ACTION_STOP -> {
                stopUploadLoop()
                stopSelf()
            }
        }
        
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        stopUploadLoop()
        locationTracker.stopLocationUpdates()
        Log.d(TAG, "Service destroyed")
    }
    
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "GPS Upload Service",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Uploads GPS location to tracker"
            setShowBadge(false)
        }
        
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }
    
    private fun createNotification(): Notification {
        val notificationIntent = Intent(this, Class.forName("com.hamradio.ft8auto.MainActivity"))
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )
        
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("GPS Upload Active")
            .setContentText("Sharing location with tracker")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }
    
    private fun startLocationTracking() {
        if (locationTracker.hasLocationPermission()) {
            locationTracker.startLocationUpdates()
        } else {
            Log.e(TAG, "Location permission not granted")
            stopSelf()
        }
    }
    
    private fun startUploadLoop() {
        serviceJob = serviceScope.launch {
            while (isActive) {
                try {
                    uploadCurrentLocation()
                } catch (e: Exception) {
                    Log.e(TAG, "Error in upload loop: ${e.message}")
                }
                delay(uploadInterval)
            }
        }
    }
    
    private fun stopUploadLoop() {
        serviceJob?.cancel()
        serviceJob = null
        serviceScope.cancel()
    }
    
    private suspend fun uploadCurrentLocation() {
        val lat = locationTracker.currentLatitude
        val lon = locationTracker.currentLongitude
        
        if (lat == null || lon == null) {
            Log.d(TAG, "No GPS fix yet, skipping upload")
            return
        }
        
        withContext(Dispatchers.IO) {
            try {
                val url = URL("http://$trackerHost:$trackerPort/gps")
                val connection = url.openConnection() as HttpURLConnection
                
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                
                // Create JSON payload
                val jsonData = JSONObject().apply {
                    put("latitude", lat)
                    put("longitude", lon)
                    put("timestamp", System.currentTimeMillis() / 1000)
                    put("source", "android_auto")
                }
                
                // Send request
                val writer = OutputStreamWriter(connection.outputStream)
                writer.write(jsonData.toString())
                writer.flush()
                writer.close()
                
                // Check response
                val responseCode = connection.responseCode
                if (responseCode == 200) {
                    Log.d(TAG, "GPS uploaded successfully: $lat, $lon")
                } else {
                    Log.w(TAG, "Upload failed with code: $responseCode")
                }
                
                connection.disconnect()
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to upload GPS: ${e.message}")
            }
        }
    }
}
