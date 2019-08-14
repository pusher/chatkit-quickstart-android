package com.pusher.chatkit.gettingstarted

import com.pusher.chatkit.messages.multipart.Message
import com.pusher.chatkit.messages.multipart.NewPart
import com.pusher.chatkit.messages.multipart.Payload
import java.util.*

// This helper maps messages to and from their text, and the internal identifiers which we assign
// them. Chatkit backend assigns every message a unique ID, but we want to add messages to the
// store and UI before we get a response from the SDK (as pending messages) and be able to match
// these messages by an ID we choose so that we can update their status when they succeed or fail.
//
// Chatkit messages can be made of multiple "parts", so we use one part to hold our message text,
// and another to hold the ID which *we* assign. We don't render the second part in the UI, we
// simply use it to hold a unique ID so that we can match copies of the same message before and
// after it is sent.
object MessageMapper {
    private const val MIME_TYPE_INTERNAL_ID = "com-pusher-gettingstarted/internal-id"
    private const val MIME_TYPE_TEXT = "text/plain"

    fun messageToInternalId(message: Message): String? =
        findContentOfType(MIME_TYPE_INTERNAL_ID, message)

    fun messageToInternalId(message: List<NewPart>): String? =
        findContentOfType(MIME_TYPE_INTERNAL_ID, message)

    fun messageToText(message: Message): String? =
        findContentOfType(MIME_TYPE_TEXT, message)

    fun messageToText(message: List<NewPart>): String? =
        findContentOfType(MIME_TYPE_TEXT, message)

    private fun findContentOfType(type: String, message: Message): String? {
        val part = message.parts.find {
            (it.payload as? Payload.Inline)?.type == type
        }

        return (part?.payload as? Payload.Inline)?.content
    }

    private fun findContentOfType(type: String, message: List<NewPart>): String? {
        val part = message.find {
            (it as? NewPart.Inline)?.type == type
        }

        return (part as? NewPart.Inline)?.content
    }

    fun textToNewMessage(text: String): List<NewPart> {
        return listOf(
            NewPart.Inline(text, MIME_TYPE_TEXT),
            NewPart.Inline(generateInternalId(), MIME_TYPE_INTERNAL_ID)
        )
    }

    private fun generateInternalId(): String {
        return UUID.randomUUID().toString()
    }
}
