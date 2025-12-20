package com.hamradio.ft8auto

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.hamradio.ft8auto.model.FT8Decode
import com.hamradio.ft8auto.model.FT8DecodeManager
import com.hamradio.ft8auto.service.FT8DataService

class MainActivity : AppCompatActivity() {
    
    private lateinit var decodeTextView: TextView
    private lateinit var hostEditText: EditText
    private lateinit var portEditText: EditText
    private lateinit var connectButton: Button
    private lateinit var disconnectButton: Button
    private lateinit var clearButton: Button
    
    private val decodeManager = FT8DecodeManager.getInstance()
    
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
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        initializeViews()
        setupListeners()
        
        decodeManager.addListener(decodeListener)
        updateDecodeDisplay()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        decodeManager.removeListener(decodeListener)
    }
    
    private fun initializeViews() {
        decodeTextView = findViewById(R.id.decodeTextView)
        hostEditText = findViewById(R.id.hostEditText)
        portEditText = findViewById(R.id.portEditText)
        connectButton = findViewById(R.id.connectButton)
        disconnectButton = findViewById(R.id.disconnectButton)
        clearButton = findViewById(R.id.clearButton)
        
        // Set default values
        hostEditText.setText("192.168.1.100")
        portEditText.setText("8080")
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
    }
    
    private fun updateDecodeDisplay() {
        val decodes = decodeManager.getDecodes()
        
        if (decodes.isEmpty()) {
            decodeTextView.text = "No FT8 decodes yet.\n\nConnect to a data source to start receiving decodes."
        } else {
            val sb = StringBuilder()
            sb.append("Received ${decodes.size} decodes:\n\n")
            decodes.forEach { decode ->
                sb.append(decode.displayText)
                sb.append("\n")
            }
            decodeTextView.text = sb.toString()
        }
    }
}
