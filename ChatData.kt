package com.didi.chatarchive.data

import com.didi.chatarchive.model.ChatMessage
import com.didi.chatarchive.model.Kind

sealed interface ChatItem { val id: Long }
data class DateItem(val label: String, override val id: Long) : ChatItem
data class MessageItem(val message: ChatMessage, val firstInGroup: Boolean, override val id: Long) : ChatItem
data class DateEntry(val label: String, val itemIndex: Int, val count: Int)

/** Messages flattened into list items (date separators + bubbles) in original file order. */
class ChatData(val messages: List<ChatMessage>) {
    val items: List<ChatItem>
    val dates: List<DateEntry>
    val rangeLabel: String

    init {
        val list = ArrayList<ChatItem>(messages.size + 400)
        val dateList = ArrayList<DateEntry>()
        var lastKey = -1
        var prev: ChatMessage? = null
        var id = 0L
        var dateStart = 0
        var dateLabel = ""
        var count = 0
        for (m in messages) {
            if (m.dateKey != lastKey) {
                if (lastKey != -1) dateList.add(DateEntry(dateLabel, dateStart, count))
                dateLabel = ChatParser.dateLabel(m)
                dateStart = list.size
                count = 0
                list.add(DateItem(dateLabel, id++))
                lastKey = m.dateKey
                prev = null
            }
            val first = prev == null || prev.kind == Kind.SYSTEM || m.kind == Kind.SYSTEM || prev.outgoing != m.outgoing
            list.add(MessageItem(m, first, id++))
            if (m.kind != Kind.SYSTEM) count++
            prev = m
        }
        if (lastKey != -1) dateList.add(DateEntry(dateLabel, dateStart, count))
        items = list
        dates = dateList
        rangeLabel = if (messages.isEmpty()) "" else
            "${messages.first().day} ${monthShort(messages.first().month)} – ${messages.last().day} ${monthShort(messages.last().month)} ${messages.last().year}"
    }

    private fun monthShort(m: Int) = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")[m - 1]
}
