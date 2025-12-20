package com.hamradio.ft8auto.auto

import android.content.Intent
import androidx.car.app.CarAppService
import androidx.car.app.Screen
import androidx.car.app.Session
import androidx.car.app.validation.HostValidator

/**
 * Android Auto Car App Service for FT8 decodes
 * This makes the app appear as a messaging app in Android Auto
 */
class FT8MessagingService : CarAppService() {
    
    override fun createHostValidator(): HostValidator {
        return HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
    }
    
    override fun onCreateSession(): Session {
        return FT8Session()
    }
    
    inner class FT8Session : Session() {
        override fun onCreateScreen(intent: Intent): Screen {
            return FT8MainScreen(carContext)
        }
    }
}
