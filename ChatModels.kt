package com.didi.chatarchive.model

/** Fixed participant mapping, from DIDI's point of view (she is the device owner). */
object Participants {
    /** Sender name exactly as written in the export for the device owner (shown on the RIGHT). */
    const val OWNER_EXPORT_NAME = "Didi 🤔"
    /** Sender name exactly as written in the export for the contact (shown on the LEFT). */
    const val CONTACT_EXPORT_NAME = "𝔻ᴇʙsʜᴀɴᴋᴀʀ 𝕄ᴏɴᴅᴀʟ"
    /** Name shown in the chat header. */
    const val CONTACT_DISPLAY_NAME = "Debshankar Mondal"
    /** Used when rewriting "You ..." system notices that the export wrote from Debshankar's side. */
    const val CONTACT_SHORT_NAME = "Debshankar"
}

enum class Kind { TEXT, MEDIA_OMITTED, CONTACT, ATTACHMENT, LOCATION, POLL, EMPTY, SYSTEM }

data class ChatMessage(
    val kind: Kind,
    /** true = sent by Didi (right, pink); false = sent by Debshankar (left, white). Ignored for SYSTEM. */
    val outgoing: Boolean,
    val year: Int,
    val month: Int,
    val day: Int,
    val hour: Int,
    val minute: Int,
    val text: String,
    val edited: Boolean = false,
    /** CONTACT: "name\nphone". POLL: option lines "label\tvotes". */
    val extra: String = ""
) {
    val dateKey: Int get() = year * 10000 + month * 100 + day
    val sortKey: Long get() = dateKey * 10000L + hour * 100 + minute
}

data class ContactCard(val name: String, val phone: String)

data class ImportStats(
    val totalEntries: Int,
    val kept: Int,
    val deletedDropped: Int,
    val system: Int,
    val fromDidi: Int,
    val fromDebshankar: Int,
    val mediaOmitted: Int,
    val edited: Int,
    val empty: Int,
    val contactCards: Int,
    val locations: Int,
    val polls: Int,
    val attachments: Int,
    val outOfOrderSteps: Int,
    val extraFilesInZip: Int,
    val firstDate: String,
    val lastDate: String
)

data class ParseResult(val messages: List<ChatMessage>, val stats: ImportStats)

class ImportException(message: String) : Exception(message)
