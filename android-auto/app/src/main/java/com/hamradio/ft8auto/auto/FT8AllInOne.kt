package com.hamradio.ft8auto.auto

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import androidx.car.app.messaging.CarMessagingService
import androidx.car.app.messaging.ConversationItem
import androidx.car.app.messaging.Message
import androidx.car.app.messaging.Person
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput
import androidx.core.app.Person as CorePerson
import androidx.core.app.NotificationCompat.CarExtender.UnreadConversation

import com.hamradio.ft8auto.R
import com.hamradio.ft8auto.model.FT8Decode
import com.hamradio.ft8auto.model.FT8DecodeManager

/**
 * ============================================================
 *  FT8MessagingService
 *  Android Auto Messaging Entry Point
 * ============================================================
 */

 /*
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
*/

/**
 * ============================================================
 *  FT8Conversation Model
 * ============================================================
 */
data class FT8Conversation(
    val id: String,                 // Unique per callsign or QSO
    val remoteCallsign: String,     // e.g. "K7ABC"
    val messages: List<FT8Decode>,  // Ordered oldest → newest
    val isGroup: Boolean = false
)

/**
 * ============================================================
 *  FT8ConversationMapper
 *  Converts FT8Conversation → Android Auto ConversationItem
 * ============================================================
 */
object FT8ConversationMapper {

    fun toCarConversations(conversations: List<FT8Conversation>): List<ConversationItem> {
        return conversations.map { toCarConversation(it) }
    }

    private fun toCarConversation(conv: FT8Conversation): ConversationItem {
        val remotePerson = Person.Builder()
            .setName(conv.remoteCallsign)
            .build()

        val carMessages = conv.messages.map { decode ->
            toCarMessage(decode, remotePerson)
        }

        return ConversationItem.Builder(conv.id)
            .setTitle(conv.remoteCallsign)
            .setGroup(conv.isGroup)
            .setParticipants(listOf(remotePerson))
            .setMessages(carMessages)
            .build()
    }

    private fun toCarMessage(decode: FT8Decode, sender: Person): Message {
        return Message.Builder(decode.message)
            .setTimestamp(decode.timestampMillis) // Add this field to FT8Decode if needed
            .setSender(sender)
            .build()
    }
}

/**
 * ============================================================
 *  FT8NotificationFactory
 *  Builds Android Auto–compatible messaging notifications
 * ============================================================
 */
object FT8NotificationFactory {

    const val KEY_TEXT_REPLY = "key_text_reply"
    const val CHANNEL_ID = "ft8_messages"

    fun buildConversationNotification(
        context: Context,
        conversation: FT8Conversation,
        contentIntent: PendingIntent?,
        replyIntent: PendingIntent
    ): Notification {

        val userPerson = CorePerson.Builder()
            .setName("You")
            .build()

        val remotePerson = CorePerson.Builder()
            .setName(conversation.remoteCallsign)
            .build()

        // MessagingStyle for Android + Android Auto
        val style = NotificationCompat.MessagingStyle(userPerson)
            .setConversationTitle(conversation.remoteCallsign)

        conversation.messages.forEach { msg ->
            style.addMessage(
                NotificationCompat.MessagingStyle.Message(
                    msg.message,
                    msg.timestampMillis,
                    remotePerson
                )
            )
        }

        // RemoteInput for inline reply
        val remoteInput = RemoteInput.Builder(KEY_TEXT_REPLY)
            .setLabel("Reply")
            .build()

        // Car-specific unread conversation
        val unreadBuilder = UnreadConversation.Builder(conversation.remoteCallsign)
        conversation.messages.forEach { msg ->
            unreadBuilder.addMessage(msg.message)
        }
        unreadBuilder.setLatestTimestamp(
            conversation.messages.lastOrNull()?.timestampMillis ?: System.currentTimeMillis()
        )
        unreadBuilder.setReplyAction(replyIntent, remoteInput)

        val carExtender = NotificationCompat.CarExtender()
            .setUnreadConversation(unreadBuilder.build())

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle(conversation.remoteCallsign)
            .setContentText(conversation.messages.lastOrNull()?.message ?: "New FT8 message")
            .setStyle(style)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .extend(carExtender)
            .build()
    }
}
