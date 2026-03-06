package com.hamradio.ft8auto.auto

import androidx.car.app.messaging.CarMessagingService
import androidx.car.app.messaging.ConversationItem
import androidx.car.app.messaging.Message
import androidx.car.app.messaging.Person

/**
 * ============================================================
 *  FT8MessagingService
 *  Android Auto Messaging Entry Point
 * ============================================================
 */

class FT8MessagingService : CarMessagingService() {

    private val decodeManager = FT8DecodeManager.getInstance()

    override fun onGetConversations(): List<ConversationItem> {
        // Convert FT8 decodes into grouped conversations
        val conversations = decodeManager.getConversations()  // You will implement this
        return FT8ConversationMapper.toCarConversations(conversations)
    }

    override fun onMarkAsRead(conversationId: String) {
        decodeManager.markConversationRead(conversationId)
    }

    override fun onSendMessage(conversationId: String, messageText: String) {
        decodeManager.sendFt8Message(conversationId, messageText)
    }
}
