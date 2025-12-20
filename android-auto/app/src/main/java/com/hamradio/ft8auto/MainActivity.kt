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
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity() {
    
    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2
    private lateinit var hostEditText: EditText
    private lateinit var portEditText: EditText
    private lateinit var connectButton: Button
    private lateinit var disconnectButton: Button
    private lateinit var clearButton: Button
    private lateinit var locationTextView: TextView
    private lateinit var unitsSwitch: SwitchMaterial
    private lateinit var gpsUploadSwitch: SwitchMaterial
    private lateinit var bandSpinner: Spinner
    
    private val decodeManager = FT8DecodeManager.getInstance()
    private lateinit var locationTracker: LocationTracker
    var useMiles = true // Default to miles for USA (made public for fragments)
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
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
        
        initializeViews()
        setupListeners()
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
        hostEditText = findViewById(R.id.hostEditText)
        portEditText = findViewById(R.id.portEditText)
        connectButton = findViewById(R.id.connectButton)
        disconnectButton = findViewById(R.id.disconnectButton)
        clearButton = findViewById(R.id.clearButton)
        locationTextView = findViewById(R.id.locationTextView)
        unitsSwitch = findViewById(R.id.unitsSwitch)
        gpsUploadSwitch = findViewById(R.id.gpsUploadSwitch)
        bandSpinner = findViewById(R.id.bandSpinner)
        
        // Set default values
        hostEditText.setText("192.168.1.100")
        portEditText.setText("8080")
        
        // Setup band spinner
        val bands = arrayOf("Select Band", "80m", "60m", "40m", "30m", "20m", "17m", "15m", "12m", "10m", "6m")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, bands)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        bandSpinner.adapter = adapter
        
        // Load saved preference for units (default to miles)
        val prefs = getSharedPreferences("ft8auto_prefs", MODE_PRIVATE)
        useMiles = prefs.getBoolean("use_miles", true)
        unitsSwitch.isChecked = useMiles
        
        // Load GPS upload preference
        val gpsUploadEnabled = prefs.getBoolean("gps_upload_enabled", false)
        gpsUploadSwitch.isChecked = gpsUploadEnabled
        
        // Load saved band
        val savedBand = prefs.getString("current_band", "Select Band")
        val bandPosition = bands.indexOf(savedBand)
        if (bandPosition >= 0) {
            bandSpinner.setSelection(bandPosition)
        }
    }
    
    private fun setupListeners() {
        connectButton.setOnClickListener {
            val host = hostEditText.text.toString()
            val port = portEditText.text.toString().toIntOrNull() ?: 8080
            
            val intent = Intent(this, FT8DataService::class.java).apply {
                action = FT8DataService.ACTION_START_NETWORK
                putExtra(FT8DataService.EXTRA_HOST, host)
                putExtra(FT8DataService.EXTRA_PORT, port)
            }
            startForegroundService(intent)
        }
        
        disconnectButton.setOnClickListener {
            val intent = Intent(this, FT8DataService::class.java).apply {
                action = FT8DataService.ACTION_STOP
            }
            startService(intent)
        }
        
        clearButton.setOnClickListener {
            decodeManager.clearDecodes()
        }
        
        unitsSwitch.setOnCheckedChangeListener { _, isChecked ->
            useMiles = isChecked
            // Save preference
            getSharedPreferences("ft8auto_prefs", MODE_PRIVATE)
                .edit()
                .putBoolean("use_miles", useMiles)
                .apply()
            // Fragments will pick up the change automatically
        }
        }
        
        gpsUploadSwitch.setOnCheckedChangeListener { _, isChecked ->
            // Save preference
            getSharedPreferences("ft8auto_prefs", MODE_PRIVATE)
                .edit()
                .putBoolean("gps_upload_enabled", isChecked)
                .apply()
            
            if (isChecked) {
                // Start GPS upload service
                val host = hostEditText.text.toString()
                GPSUploadService.start(this, host, port = 8081, intervalSeconds = 30)
            } else {
                // Stop GPS upload service
                GPSUploadService.stop(this)
            }
        }
        
        bandSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val band = bands[position]
                if (band != "Select Band") {
                    // Save preference
                    getSharedPreferences("ft8auto_prefs", MODE_PRIVATE)
                        .edit()
                        .putString("current_band", band)
                        .apply()
                    
                    // Send band update to tracker
                    sendBandUpdate(band)
                }
            }
            
            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing
            }
        }
    }
    
    private fun setupTabs() {
        // Setup ViewPager2 adapter
        viewPager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int = 2
            
            override fun createFragment(position: Int): Fragment {
                return when (position) {
                    0 -> DecodeListFragment()
                    1 -> MapFragment()
                    else -> DecodeListFragment()
                }
            }
        }
        
        // Link TabLayout with ViewPager2
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Decodes"
                1 -> "Map"
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
        val host = hostEditText.text.toString()
        if (host.isEmpty()) return
        
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val url = URL("http://$host:8081/band")
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
