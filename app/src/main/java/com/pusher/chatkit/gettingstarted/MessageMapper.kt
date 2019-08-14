package com.pusher.chatkit.gettingstarted

import com.pusher.chatkit.messages.multipart.Message
import com.pusher.chatkit.messages.multipart.NewPart
import com.pusher.chatkit.messages.multipart.Payload
import java.util.*


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
