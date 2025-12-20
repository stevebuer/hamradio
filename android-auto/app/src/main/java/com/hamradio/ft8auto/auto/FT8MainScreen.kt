package com.hamradio.ft8auto.auto

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.*
import com.hamradio.ft8auto.model.FT8Decode
import com.hamradio.ft8auto.model.FT8DecodeManager

/**
 * Main screen for Android Auto showing FT8 decodes
 */
class FT8MainScreen(carContext: CarContext) : Screen(carContext) {
    
    private val decodeManager = FT8DecodeManager.getInstance()
    
    private val decodeListener = object : FT8DecodeManager.DecodeListener {
        override fun onNewDecode(decode: FT8Decode) {
            invalidate()
        }
        
        override fun onDecodesCleared() {
            invalidate()
        }
    }
    
    init {
        decodeManager.addListener(decodeListener)
    }
    
    override fun onGetTemplate(): Template {
        val decodes = decodeManager.getDecodes()
        
        val listBuilder = ItemList.Builder()
            .setNoItemsMessage("Waiting for FT8 decodes...")
        
        // Add decodes to the list
        decodes.take(6).forEach { decode ->
            val row = Row.Builder()
                .setTitle(decode.callsign.ifEmpty { "UNKNOWN" })
                .addText("${decode.formattedTime} | SNR: ${decode.snr}dB")
                .addText(decode.message)
                .build()
            
            listBuilder.addItem(row)
        }
        
        return MessageTemplate.Builder(buildMessage(decodes))
            .setTitle("FT8 Decodes")
            .setHeaderAction(Action.APP_ICON)
            .addAction(
                Action.Builder()
                    .setTitle("Refresh")
                    .setOnClickListener { invalidate() }
                    .build()
            )
            .build()
    }
    
    private fun buildMessage(decodes: List<FT8Decode>): String {
        if (decodes.isEmpty()) {
            return "Waiting for FT8 decodes...\n\nMake sure the data service is running."
        }
        
        val sb = StringBuilder()
        sb.append("Recent FT8 Decodes (${decodes.size}):\n\n")
        
        decodes.take(10).forEach { decode ->
            sb.append("${decode.displayText}\n")
        }
        
        return sb.toString()
    }
}
