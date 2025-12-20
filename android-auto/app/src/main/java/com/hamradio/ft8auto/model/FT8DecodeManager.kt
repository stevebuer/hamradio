package com.hamradio.ft8auto.model

import java.util.concurrent.CopyOnWriteArrayList

/**
 * Manages the list of FT8 decodes and notifies listeners of changes
 */
class FT8DecodeManager private constructor() {
    
    interface DecodeListener {
        fun onNewDecode(decode: FT8Decode)
        fun onDecodesCleared()
    }
    
    private val decodes = CopyOnWriteArrayList<FT8Decode>()
    private val listeners = CopyOnWriteArrayList<DecodeListener>()
    
    companion object {
        private const val MAX_DECODES = 100
        
        @Volatile
        private var instance: FT8DecodeManager? = null
        
        fun getInstance(): FT8DecodeManager {
            return instance ?: synchronized(this) {
                instance ?: FT8DecodeManager().also { instance = it }
            }
        }
    }
    
    fun addDecode(decode: FT8Decode) {
        decodes.add(0, decode) // Add to beginning
        
        // Keep only the most recent MAX_DECODES
        while (decodes.size > MAX_DECODES) {
            decodes.removeAt(decodes.size - 1)
        }
        
        // Notify listeners
        listeners.forEach { it.onNewDecode(decode) }
    }
    
    fun getDecodes(): List<FT8Decode> = ArrayList(decodes)
    
    fun clearDecodes() {
        decodes.clear()
        listeners.forEach { it.onDecodesCleared() }
    }
    
    fun addListener(listener: DecodeListener) {
        listeners.add(listener)
    }
    
    fun removeListener(listener: DecodeListener) {
        listeners.remove(listener)
    }
    
    val decodeCount: Int
        get() = decodes.size
}
