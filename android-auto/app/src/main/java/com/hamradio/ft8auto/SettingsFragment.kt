package com.hamradio.ft8auto

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.google.android.material.switchmaterial.SwitchMaterial
import com.hamradio.ft8auto.service.FT8DataService
import com.hamradio.ft8auto.service.GPSUploadService
import com.hamradio.ft8auto.util.PreferencesManager

/**
 * Fragment for app settings and configuration
 */
class SettingsFragment : Fragment() {
    
    private lateinit var hostEditText: EditText
    private lateinit var portEditText: EditText
    private lateinit var connectButton: Button
    private lateinit var disconnectButton: Button
    private lateinit var testConnectionButton: Button
    private lateinit var resetButton: Button
    
    private lateinit var unitsSwitch: SwitchMaterial
    private lateinit var gpsUploadSwitch: SwitchMaterial
    private lateinit var bandSpinner: Spinner
    private lateinit var gpsIntervalSeekBar: SeekBar
    private lateinit var gpsIntervalLabel: TextView
    
    private val bands = arrayOf("Select Band", "80m", "60m", "40m", "30m", "20m", "17m", "15m", "12m", "10m", "6m")
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize views
        hostEditText = view.findViewById(R.id.settingsHostEditText)
        portEditText = view.findViewById(R.id.settingsPortEditText)
        connectButton = view.findViewById(R.id.settingsConnectButton)
        disconnectButton = view.findViewById(R.id.settingsDisconnectButton)
        testConnectionButton = view.findViewById(R.id.testConnectionButton)
        resetButton = view.findViewById(R.id.resetButton)
        
        unitsSwitch = view.findViewById(R.id.settingsUnitsSwitch)
        gpsUploadSwitch = view.findViewById(R.id.settingsGpsUploadSwitch)
        bandSpinner = view.findViewById(R.id.settingsBandSpinner)
        gpsIntervalSeekBar = view.findViewById(R.id.gpsIntervalSeekBar)
        gpsIntervalLabel = view.findViewById(R.id.gpsIntervalLabel)
        
        // Load and display saved preferences
        loadPreferences()
        setupListeners()
        setupBandSpinner()
    }
    
    private fun loadPreferences() {
        // Load network settings
        hostEditText.setText(PreferencesManager.getTrackerHost())
        portEditText.setText(PreferencesManager.getTrackerPort().toString())
        
        // Load units preference
        unitsSwitch.isChecked = PreferencesManager.getUseMiles()
        
        // Load GPS upload preference
        gpsUploadSwitch.isChecked = PreferencesManager.isGpsUploadEnabled()
        
        // Load GPS upload interval
        val interval = PreferencesManager.getGpsUploadInterval()
        gpsIntervalSeekBar.progress = interval
        updateGpsIntervalLabel(interval)
        
        // Load current band
        val savedBand = PreferencesManager.getCurrentBand()
        val bandPosition = bands.indexOf(savedBand)
        if (bandPosition >= 0) {
            bandSpinner.setSelection(bandPosition)
        }
    }
    
    private fun setupBandSpinner() {
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, bands)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        bandSpinner.adapter = adapter
    }
    
    private fun setupListeners() {
        connectButton.setOnClickListener {
            saveNetworkSettings()
            connectToTracker()
        }
        
        disconnectButton.setOnClickListener {
            disconnectFromTracker()
        }
        
        testConnectionButton.setOnClickListener {
            testConnection()
        }
        
        resetButton.setOnClickListener {
            resetToDefaults()
        }
        
        unitsSwitch.setOnCheckedChangeListener { _, isChecked ->
            PreferencesManager.setUseMiles(isChecked)
            // Notify MainActivity to update fragments
            (activity as? MainActivity)?.let {
                it.useMiles = isChecked
            }
        }
        
        gpsUploadSwitch.setOnCheckedChangeListener { _, isChecked ->
            PreferencesManager.setGpsUploadEnabled(isChecked)
            
            if (isChecked) {
                val host = hostEditText.text.toString()
                val port = portEditText.text.toString().toIntOrNull() ?: 8080
                val interval = gpsIntervalSeekBar.progress
                if (host.isNotEmpty()) {
                    GPSUploadService.start(requireContext(), host, port, interval)
                }
            } else {
                GPSUploadService.stop(requireContext())
            }
        }
        
        gpsIntervalSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                updateGpsIntervalLabel(progress)
                PreferencesManager.setGpsUploadInterval(progress)
                
                // Restart GPS upload service with new interval if enabled
                if (gpsUploadSwitch.isChecked) {
                    val host = hostEditText.text.toString()
                    val port = portEditText.text.toString().toIntOrNull() ?: 8080
                    if (host.isNotEmpty()) {
                        GPSUploadService.start(requireContext(), host, port, progress)
                    }
                }
            }
            
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }
    
    private fun saveNetworkSettings() {
        val host = hostEditText.text.toString().trim()
        val port = portEditText.text.toString().toIntOrNull() ?: 8080
        
        if (host.isEmpty()) {
            hostEditText.error = "Host cannot be empty"
            return
        }
        
        PreferencesManager.setTrackerHost(host)
        PreferencesManager.setTrackerPort(port)
    }
    
    private fun connectToTracker() {
        val host = hostEditText.text.toString()
        val port = portEditText.text.toString().toIntOrNull() ?: 8080
        
        val intent = Intent(requireContext(), FT8DataService::class.java).apply {
            action = FT8DataService.ACTION_START_NETWORK
            putExtra(FT8DataService.EXTRA_HOST, host)
            putExtra(FT8DataService.EXTRA_PORT, port)
        }
        requireContext().startForegroundService(intent)
    }
    
    private fun disconnectFromTracker() {
        val intent = Intent(requireContext(), FT8DataService::class.java).apply {
            action = FT8DataService.ACTION_STOP
        }
        requireContext().startService(intent)
    }
    
    private fun testConnection() {
        val host = hostEditText.text.toString()
        val port = portEditText.text.toString().toIntOrNull() ?: 8080
        
        if (host.isEmpty()) {
            hostEditText.error = "Host cannot be empty"
            return
        }
        
        // Start a background task to test connection
        Thread {
            try {
                val socket = java.net.Socket(host, port)
                socket.close()
                
                activity?.runOnUiThread {
                    android.widget.Toast.makeText(
                        requireContext(),
                        "Connection successful!",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                activity?.runOnUiThread {
                    android.widget.Toast.makeText(
                        requireContext(),
                        "Connection failed: ${e.message}",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }.start()
    }
    
    private fun resetToDefaults() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Reset to Defaults?")
        builder.setMessage("This will reset all settings to default values. Continue?")
        builder.setPositiveButton("Yes") { _, _ ->
            PreferencesManager.clearAll()
            loadPreferences()
            
            android.widget.Toast.makeText(
                requireContext(),
                "Settings reset to defaults",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
        builder.setNegativeButton("Cancel", null)
        builder.show()
    }
    
    private fun updateGpsIntervalLabel(seconds: Int) {
        gpsIntervalLabel.text = "GPS Upload Interval: $seconds seconds"
    }
}
