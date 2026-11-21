package com.didi.chatarchive.data

import android.content.Context
import com.didi.chatarchive.model.ChatMessage
import com.didi.chatarchive.model.Kind
import org.json.JSONObject
import java.io.File

/** Local, file-based storage in the app's private internal storage: meta.json + messages.jsonl. */
class ChatStore(context: Context) {
    private val dir = File(context.filesDir, "chat")
    private val messagesFile get() = File(dir, "messages.jsonl")
    private val metaFile get() = File(dir, "meta.json")

    fun hasChat(): Boolean = messagesFile.exists() && metaFile.exists()

    fun save(messages: List<ChatMessage>, sourceName: String) {
        dir.mkdirs()
        val tmp = File(dir, "messages.jsonl.tmp")
        tmp.bufferedWriter(Charsets.UTF_8).use { w ->
            for (m in messages) {
                val o = JSONObject()
                o.put("k", m.kind.name); o.put("o", m.outgoing)
                o.put("y", m.year); o.put("mo", m.month); o.put("d", m.day)
                o.put("h", m.hour); o.put("mi", m.minute)
                o.put("t", m.text)
                if (m.edited) o.put("e", true)
                if (m.extra.isNotEmpty()) o.put("x", m.extra)
                w.write(o.toString()); w.write("\n")
            }
        }
        if (messagesFile.exists()) messagesFile.delete()
        tmp.renameTo(messagesFile)
        val meta = JSONObject()
        meta.put("source", sourceName); meta.put("count", messages.size); meta.put("importedAt", System.currentTimeMillis())
        metaFile.writeText(meta.toString(), Charsets.UTF_8)
    }

    fun load(): List<ChatMessage> {
        val list = ArrayList<ChatMessage>(90_000)
        messagesFile.bufferedReader(Charsets.UTF_8).useLines { lines ->
            for (line in lines) {
                if (line.isBlank()) continue
                val o = JSONObject(line)
                list.add(
                    ChatMessage(
                        kind = Kind.valueOf(o.getString("k")), outgoing = o.getBoolean("o"),
                        year = o.getInt("y"), month = o.getInt("mo"), day = o.getInt("d"),
                        hour = o.getInt("h"), minute = o.getInt("mi"),
                        text = o.getString("t"), edited = o.optBoolean("e", false), extra = o.optString("x", "")
                    )
                )
            }
        }
        return list
    }
}
