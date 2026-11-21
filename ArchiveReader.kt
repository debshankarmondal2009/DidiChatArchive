package com.didi.chatarchive.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.didi.chatarchive.model.ContactCard
import com.didi.chatarchive.model.ImportException
import com.didi.chatarchive.model.ParseResult
import java.io.BufferedInputStream
import java.util.zip.ZipInputStream

/** Reads a WhatsApp export ZIP (or a bare TXT) from a user-picked document. Nothing is uploaded anywhere. */
object ArchiveReader {
    data class Source(val displayName: String, val result: ParseResult)

    fun read(context: Context, uri: Uri): Source {
        val resolver = context.contentResolver
        val displayName = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
            if (it.moveToFirst()) it.getString(0) else null
        } ?: "chat export"

        val stream = resolver.openInputStream(uri) ?: throw ImportException("Could not open the selected file.")
        BufferedInputStream(stream).use { input ->
            input.mark(4)
            val b0 = input.read(); val b1 = input.read()
            input.reset()
            val isZip = b0 == 'P'.code && b1 == 'K'.code

            if (!isZip) {
                val text = String(input.readBytes(), Charsets.UTF_8)
                return Source(displayName, ChatParser.parse(text, emptyMap()))
            }

            var chatBytes: ByteArray? = null
            val cards = HashMap<String, ContactCard>()
            var extra = 0
            ZipInputStream(input).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    if (entry.isDirectory) continue
                    val name = entry.name.substringAfterLast('/')
                    when {
                        name.endsWith(".txt", ignoreCase = true) -> {
                            val bytes = zip.readBytes()
                            if (chatBytes == null || bytes.size > chatBytes!!.size) chatBytes = bytes
                        }
                        name.endsWith(".vcf", ignoreCase = true) -> {
                            ChatParser.parseVcf(String(zip.readBytes(), Charsets.UTF_8))?.let { cards[name] = it }
                        }
                        else -> extra++
                    }
                }
            }
            val bytes = chatBytes ?: throw ImportException("This ZIP does not contain a WhatsApp chat TXT file.")
            return Source(displayName, ChatParser.parse(String(bytes, Charsets.UTF_8), cards, extra))
        }
    }
}
