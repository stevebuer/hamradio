package com.hamradio.ft8auto.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.hamradio.ft8auto.MainActivity
import com.hamradio.ft8auto.model.FT8DecodeManager
import com.hamradio.ft8auto.parser.FT8Parser
import com.hamradio.ft8auto.util.PreferencesManager
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.Socket
import java.net.URL

/**
 * Background service that receives FT8 decode data from network or serial port
 */
class FT8DataService : Service() {
    
    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private val decodeManager = FT8DecodeManager.getInstance()
    
    private var networkJob: Job? = null
    
    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "FT8DataChannel"
        const val ACTION_START_NETWORK = "com.hamradio.ft8auto.START_NETWORK"
        const val ACTION_STOP = "com.hamradio.ft8auto.STOP"
        
        const val EXTRA_HOST = "host"
        const val EXTRA_PORT = "port"
    }
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_NETWORK -> {
                val host = intent.getStringExtra(EXTRA_HOST) ?: "localhost"
                val port = intent.getIntExtra(EXTRA_PORT, 8080)
                startNetworkReceiver(host, port)
            }
            ACTION_STOP -> {
                stopSelf()
            }
        }
        
        val notification = createNotification("FT8 Data Service Running")
        startForeground(NOTIFICATION_ID, notification)
        
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        networkJob?.cancel()
        serviceScope.cancel()
    }
    
    private fun startNetworkReceiver(host: String, port: Int) {
        networkJob?.cancel()
        networkJob = serviceScope.launch {
            try {
                connectToNetworkSource(host, port)
            } catch (e: Exception) {
                e.printStackTrace()
                // Retry after delay
                delay(5000)
                if (isActive) {
                    startNetworkReceiver(host, port)
                }
            }
        }
    }
    
    private suspend fun connectToNetworkSource(host: String, port: Int) {
        withContext(Dispatchers.IO) {
            var socket: Socket? = null
            try {
                socket = Socket(host, port)
                
                // Send band information as part of connect sequence
                sendBandInformation(host, port)
                
                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                
                updateNotification("Connected to $host:$port")
                
                while (isActive) {
                    val line = reader.readLine() ?: break
                    processLine(line)
                }
            } catch (e: Exception) {
                updateNotification("Connection failed: ${e.message}")
                throw e
            } finally {
                socket?.close()
            }
        }
    }
    
    private suspend fun sendBandInformation(host: String, port: Int) {
        try {
            val band = PreferencesManager.getCurrentBand()
            if (band.isEmpty() || band == "Select Band") {
                return  // Skip if no band selected
            }
            
            val url = URL("http://$host:$port/band")
            val connection = url.openConnection() as HttpURLConnection
            
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            
            val jsonData = JSONObject().apply {
                put("band", band)
            }
            
            val writer = OutputStreamWriter(connection.outputStream)
            writer.write(jsonData.toString())
            writer.flush()
            writer.close()
            
            val responseCode = connection.responseCode
            android.util.Log.d("FT8DataService", "Band information sent: $band (HTTP $responseCode)")
            connection.disconnect()
            
        } catch (e: Exception) {
            // Log but don't fail connection if band update fails
            android.util.Log.w("FT8DataService", "Failed to send band information: ${e.message}")
        }
    }
    
    
    private fun processLine(line: String) {
        val decode = FT8Parser.parse(line)
        if (decode != null) {
            decodeManager.addDecode(decode)
        }
    }
    
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "FT8 Data Service",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Receives FT8 decode data"
        }
        
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
    
    private fun createNotification(message: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("FT8 Auto Display")
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }
    
    private fun updateNotification(message: String) {
        val notification = createNotification(message)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}
