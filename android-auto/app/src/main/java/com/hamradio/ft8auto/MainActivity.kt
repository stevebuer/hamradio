package com.hamradio.ft8auto

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.hamradio.ft8auto.model.FT8Decode
import com.hamradio.ft8auto.model.FT8DecodeManager
import com.hamradio.ft8auto.service.FT8DataService
import com.hamradio.ft8auto.service.GPSUploadService
import com.hamradio.ft8auto.util.LocationTracker
import com.hamradio.ft8auto.util.PreferencesManager
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity() {
    
    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2
    private lateinit var locationTextView: TextView
    
    private val decodeManager = FT8DecodeManager.getInstance()
    private lateinit var locationTracker: LocationTracker
    var useMiles = true // Default to miles for USA (made public for fragments)
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val bands = arrayOf("Select Band", "80m", "60m", "40m", "30m", "20m", "17m", "15m", "12m", "10m", "6m")
    
    // Method to expose current location to MapFragment
    fun getCurrentLatitude(): Double? = locationTracker.currentLatitude
    fun getCurrentLongitude(): Double? = locationTracker.currentLongitude
    
    private val decodeListener = object : FT8DecodeManager.DecodeListener {
        override fun onNewDecode(decode: FT8Decode) {
            runOnUiThread {
                updateDecodeDisplay()
            }
        }
        
        override fun onDecodesCleared() {
            runOnUiThread {
                updateDecodeDisplay()
            }
        }
    }
    
    private val locationListener = object : LocationTracker.LocationListener {
        override fun onLocationChanged(latitude: Double, longitude: Double) {
            runOnUiThread {
                updateLocationDisplay()
                // Map will update automatically via its own listener
            }
        }
        
        override fun onLocationError(error: String) {
            runOnUiThread {
                locationTextView.text = "Location: Error - $error"
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // Initialize preferences manager
        PreferencesManager.init(this)
        
        initializeViews()
        setupTabs()
        
        // Initialize location tracker
        locationTracker = LocationTracker(this)
        locationTracker.setListener(locationListener)
        
        // Request location permissions
        requestLocationPermissions()
        
        decodeManager.addListener(decodeListener)
        updateLocationDisplay()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        decodeManager.removeListener(decodeListener)
        locationTracker.stopLocationUpdates()
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (requestCode == LocationTracker.LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                locationTracker.startLocationUpdates()
            } else {
                locationTextView.text = "Location: Permission denied"
            }
        }
    }
    
    private fun initializeViews() {
        tabLayout = findViewById(R.id.tabLayout)
        viewPager = findViewById(R.id.viewPager)
        locationTextView = findViewById(R.id.locationTextView)
        
        // Load saved preferences
        useMiles = PreferencesManager.getUseMiles()
    }
    
    private fun setupTabs() {
        // Setup ViewPager2 adapter
        viewPager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int = 3
            
            override fun createFragment(position: Int): Fragment {
                return when (position) {
                    0 -> DecodeListFragment()
                    1 -> MapFragment()
                    2 -> SettingsFragment()
                    else -> DecodeListFragment()
                }
            }
        }
        
        // Keep all fragments in memory to prevent destruction/recreation
        // This prevents the MapFragment from being destroyed when switching tabs
        viewPager.offscreenPageLimit = 2
        
        // Link TabLayout with ViewPager2
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Decodes"
                1 -> "Map"
                2 -> "Settings"
                else -> ""
            }
        }.attach()
    }
    
    private fun updateDecodeDisplay() {
        // Fragments handle their own display updates
    }
    
    private fun updateLocationDisplay() {
        val lat = locationTracker.currentLatitude
        val lon = locationTracker.currentLongitude
        
        if (lat != null && lon != null) {
            locationTextView.text = String.format(
                "Location: %.4f°, %.4f°",
                lat, lon
            )
        } else {
            locationTextView.text = "Location: Acquiring..."
        }
    }
    
    private fun requestLocationPermissions() {
        if (!locationTracker.hasLocationPermission()) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                LocationTracker.LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            locationTracker.startLocationUpdates()
        }
    }
    
    private fun sendBandUpdate(band: String) {
        val host = PreferencesManager.getTrackerHost()
        val port = PreferencesManager.getTrackerPort()
        if (host.isEmpty()) return
        
        coroutineScope.launch(Dispatchers.IO) {
            try {
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
                withContext(Dispatchers.Main) {
                    if (responseCode == 200) {
                        // Optionally show success message
                    }
                }
                
                connection.disconnect()
                
            } catch (e: Exception) {
                // Silently fail - band update is not critical
            }
        }
    }
}
