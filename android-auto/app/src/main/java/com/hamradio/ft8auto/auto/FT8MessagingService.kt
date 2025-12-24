package com.hamradio.ft8auto.auto

import androidx.car.app.messaging.CarMessagingService
import androidx.car.app.messaging.ConversationItem
import androidx.car.app.messaging.Message
import androidx.car.app.messaging.Person

/**
 * Android Auto Messaging Service for FT8 decodes.
 * This exposes FT8 message conversations to Android Auto.
 *
 * IMPORTANT:
 * - Messaging apps do NOT use CarAppService, Sessions, or Screens.
 * - Android Auto pulls conversations from this service and from notifications.
 */
class FT8MessagingService : CarMessagingService() {

    /**
     * Return the list of active conversations.
     * You will populate this from your FT8 decode data.
     */
    override fun onGetConversations(): List<ConversationItem> {
        // TODO: Replace this with real FT8 conversation data
        return emptyList()
    }

    /**
     * Called when the host (Android Auto) wants to mark a conversation as read.
     */
    override fun onMarkAsRead(conversationId: String) {
        // TODO: Update your internal state to mark messages as read
    }

    /**
     * Called when the user replies from Android Auto.
     */
    override fun onSendMessage(conversationId: String, messageText: String) {
        // TODO: Send the FT8 message through your backend or radio interface
    }
}
