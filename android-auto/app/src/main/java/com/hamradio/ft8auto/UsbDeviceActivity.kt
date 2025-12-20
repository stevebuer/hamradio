package com.hamradio.ft8auto

import android.app.Activity
import android.content.Intent
import android.hardware.usb.UsbManager
import android.os.Bundle
import com.hamradio.ft8auto.service.FT8DataService

/**
 * Activity to handle USB device attachment
 */
class UsbDeviceActivity : Activity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (intent?.action == UsbManager.ACTION_USB_DEVICE_ATTACHED) {
            // Handle USB device attachment
            val device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, android.hardware.usb.UsbDevice::class.java)
            
            device?.let {
                // Start the data service with serial mode
                val serviceIntent = Intent(this, FT8DataService::class.java).apply {
                    action = FT8DataService.ACTION_START_SERIAL
                    putExtra(FT8DataService.EXTRA_SERIAL_DEVICE, it.deviceName)
                }
                startForegroundService(serviceIntent)
            }
        }
        
        // Launch main activity
        val mainIntent = Intent(this, MainActivity::class.java)
        startActivity(mainIntent)
        finish()
    }
}
