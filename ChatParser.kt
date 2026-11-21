package com.didi.chatarchive.data

import com.didi.chatarchive.model.*

/**
 * Parses a WhatsApp "Export chat" TXT.
 * Verified format of the real export: `DD/MM/YY, h:mm<U+202F>am - Sender: text`, multi-line messages
 * continue on following lines. File order is preserved (never re-sorted).
 */
object ChatParser {
    private val HEADER = Regex(
        """^[‎‏]?(\d{1,2})/(\d{1,2})/(\d{2,4}), (\d{1,2}):(\d{2})[\s ]*([apAP][mM]) - (.*)$""",
        RegexOption.DOT_MATCHES_ALL
    )
    private val EDITED = Regex("""\s*<This message was edited>\s*$""")
    private val OPTION = Regex("""^OPTION: (.*) \((\d+) votes?\)$""")
    private const val MEDIA_OMITTED = "<Media omitted>"
    private const val FILE_ATTACHED_SUFFIX = " (file attached)"
    private val ATTACHED_TAG = Regex("""^<attached: (.+)>$""")
    private val DELETED = setOf("You deleted this message", "This message was deleted")
    private val MONTHS = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )

    private class RawEntry(
        val day: Int, val month: Int, val year: Int, val hour: Int, val minute: Int,
        val body: StringBuilder
    )

    fun parse(chatText: String, contactCards: Map<String, ContactCard>, extraFilesInZip: Int = 0): ParseResult {
        val owner = Participants.OWNER_EXPORT_NAME + ": "
        val contact = Participants.CONTACT_EXPORT_NAME + ": "

        val entries = ArrayList<RawEntry>(90_000)
        for (line in chatText.removePrefix("﻿").replace("\r\n", "\n").split('\n')) {
            val m = HEADER.matchEntire(line)
            if (m != null) {
                val v = m.groupValues
                var year = v[3].toInt()
                if (year < 100) year += 2000
                val h12 = v[4].toInt() % 12
                val hour = if (v[6].equals("pm", ignoreCase = true)) h12 + 12 else h12
                entries.add(RawEntry(v[1].toInt(), v[2].toInt(), year, hour, v[5].toInt(), StringBuilder(v[7])))
            } else if (entries.isNotEmpty()) {
                entries.last().body.append('\n').append(line)
            }
        }
        if (entries.isEmpty()) throw ImportException("No WhatsApp messages were found in this file.")
        if (entries.any { it.month > 12 }) {
            throw ImportException("Dates are not in the expected DD/MM/YY format, so the import was stopped to avoid mixing up days and months.")
        }
        val ownerCount = entries.count { it.body.startsWith(owner) }
        val contactCount = entries.count { it.body.startsWith(contact) }
        if (ownerCount == 0 || contactCount == 0) {
            throw ImportException(
                "Expected both participants (\"${Participants.OWNER_EXPORT_NAME}\" and " +
                    "\"${Participants.CONTACT_EXPORT_NAME}\") in this chat, but found $ownerCount and $contactCount messages."
            )
        }

        val out = ArrayList<ChatMessage>(entries.size)
        var deleted = 0; var system = 0; var fromDidi = 0; var fromDeb = 0
        var media = 0; var edited = 0; var empty = 0; var cards = 0; var locs = 0; var polls = 0; var atts = 0

        for (e in entries) {
            val body = e.body.toString()
            val isOwner = body.startsWith(owner)
            val isContact = !isOwner && body.startsWith(contact)

            if (!isOwner && !isContact) {
                system++
                out.add(
                    ChatMessage(Kind.SYSTEM, false, e.year, e.month, e.day, e.hour, e.minute, adaptSystem(body))
                )
                continue
            }
            var text = body.substring(if (isOwner) owner.length else contact.length)

            // Deleted messages are removed completely.
            if (text.trim() in DELETED) { deleted++; continue }

            val wasEdited = EDITED.containsMatchIn(text)
            text = EDITED.replace(text, "").trimEnd()

            var kind = Kind.TEXT
            var extra = ""
            val attachedName = when {
                text.endsWith(FILE_ATTACHED_SUFFIX) && '\n' !in text -> text.removeSuffix(FILE_ATTACHED_SUFFIX)
                else -> ATTACHED_TAG.matchEntire(text)?.groupValues?.get(1)
            }
            when {
                text.isEmpty() -> { kind = Kind.EMPTY; empty++ }
                text == MEDIA_OMITTED -> { kind = Kind.MEDIA_OMITTED; media++ }
                attachedName != null -> {
                    val card = contactCards[attachedName]
                    if (card != null) {
                        kind = Kind.CONTACT; extra = card.name + "\n" + card.phone; text = attachedName; cards++
                    } else {
                        kind = Kind.ATTACHMENT; text = attachedName; atts++
                    }
                }
                text.startsWith("location: ") -> { kind = Kind.LOCATION; text = text.removePrefix("location: ").trim(); locs++ }
                text.startsWith("POLL:\n") -> {
                    kind = Kind.POLL; polls++
                    val lines = text.removePrefix("POLL:\n").split('\n')
                    val options = lines.mapNotNull { OPTION.matchEntire(it.trim())?.let { m -> m.groupValues[1] + "\t" + m.groupValues[2] } }
                    text = lines.takeWhile { !it.startsWith("OPTION: ") }.joinToString("\n").trim()
                    extra = options.joinToString("\n")
                }
            }
            if (wasEdited) edited++
            if (isOwner) fromDidi++ else fromDeb++
            out.add(ChatMessage(kind, isOwner, e.year, e.month, e.day, e.hour, e.minute, text, wasEdited, extra))
        }

        var backwards = 0
        for (i in 1 until out.size) if (out[i].sortKey < out[i - 1].sortKey) backwards++

        val stats = ImportStats(
            totalEntries = entries.size, kept = out.size, deletedDropped = deleted, system = system,
            fromDidi = fromDidi, fromDebshankar = fromDeb, mediaOmitted = media, edited = edited,
            empty = empty, contactCards = cards, locations = locs, polls = polls, attachments = atts,
            outOfOrderSteps = backwards, extraFilesInZip = extraFilesInZip,
            firstDate = dateLabel(out.first()), lastDate = dateLabel(out.last())
        )
        return ParseResult(out, stats)
    }

    fun dateLabel(m: ChatMessage): String = "${m.day} ${MONTHS[m.month - 1]} ${m.year}"

    /**
     * The export was made on Debshankar's phone, so its notices say "You ..." for him.
     * From Didi's perspective those actions belong to Debshankar.
     */
    private fun adaptSystem(raw: String): String {
        val s = raw.trim().replace("*", "")
        val who = Participants.CONTACT_SHORT_NAME
        return when {
            s.startsWith("You turned AI responses on") ->
                s.substringBefore(" Restart AI responses")
                    .replace("You turned AI responses on, but your AI agent avoids interrupting you.",
                        "$who turned AI responses on, but his AI agent avoids interrupting him.")
                    .trim()
            s.startsWith("Your AI will no longer") ->
                s.substringBefore(" You can change this").replaceFirst("Your AI", "$who's AI")
            s.startsWith("You ") -> who + " " + s.removePrefix("You ")
            s.startsWith("Your ") -> "$who's " + s.removePrefix("Your ")
            else -> s
        }
    }

    fun parseVcf(text: String): ContactCard? {
        var name: String? = null
        var phone: String? = null
        for (line in text.replace("\r\n", "\n").split('\n')) {
            when {
                name == null && line.startsWith("FN") && ':' in line -> name = line.substringAfter(':').trim()
                phone == null && line.startsWith("TEL") && ':' in line -> phone = line.substringAfter(':').trim()
            }
        }
        return if (name.isNullOrEmpty()) null else ContactCard(name, phone ?: "")
    }
}
