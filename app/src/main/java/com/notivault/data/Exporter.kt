package com.notivault.data

import android.net.Uri
import com.notivault.NotiVaultApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant

enum class ExportFormat(val mime: String, val extension: String) {
    JSON("application/json", "json"),
    CSV("text/csv", "csv"),
    DATABASE("application/octet-stream", "db"),
}

object Exporter {

    /** Writes everything to [uri] (a file the user picked). Returns the number of rows. */
    suspend fun export(app: NotiVaultApp, uri: Uri, format: ExportFormat): Int = withContext(Dispatchers.IO) {
        val dao = app.database.notificationDao()
        val stream = app.contentResolver.openOutputStream(uri) ?: error("Can't open the file")
        stream.use { out ->
            when (format) {
                ExportFormat.JSON -> {
                    val all = dao.getAll()
                    val array = JSONArray().apply { all.forEach { put(it.toJson()) } }
                    out.write(array.toString(2).toByteArray(Charsets.UTF_8))
                    all.size
                }
                ExportFormat.CSV -> {
                    val all = dao.getAll()
                    val w = out.bufferedWriter(Charsets.UTF_8)
                    w.write("\uFEFF") // so Excel opens Vietnamese/accented text correctly
                    w.write(CSV_COLUMNS.joinToString(","))
                    w.write("\r\n")
                    all.forEach { n ->
                        w.write(
                            listOf(
                                n.id, Instant.ofEpochMilli(n.postedAt), n.packageName, n.appLabel,
                                n.title, n.text, n.bigText, n.fullText,
                                n.amount?.plain(), n.currency, n.direction, n.counterparty,
                                n.balance?.plain(), n.reference, n.parserId,
                            ).joinToString(",") { csv(it) }
                        )
                        w.write("\r\n")
                    }
                    w.flush()
                    all.size
                }
                ExportFormat.DATABASE -> {
                    app.database.checkpoint()
                    app.getDatabasePath(AppDatabase.NAME).inputStream().use { it.copyTo(out) }
                    dao.count()
                }
            }
        }
    }

    private val CSV_COLUMNS = listOf(
        "id", "postedAt", "packageName", "appLabel", "title", "text", "bigText", "fullText",
        "amount", "currency", "direction", "counterparty", "balance", "reference", "parserId",
    )

    private fun csv(value: Any?): String {
        val s = value?.toString() ?: return ""
        return if (s.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"" + s.replace("\"", "\"\"") + "\""
        } else s
    }

    private fun Double.plain(): String = toBigDecimal().stripTrailingZeros().toPlainString()
}

/** Full record as JSON (used by export and by "Copy JSON" on the detail screen). */
fun NotificationEntity.toJson(): JSONObject = JSONObject().apply {
    fun putN(key: String, value: Any?) = put(key, value ?: JSONObject.NULL)
    putN("id", id)
    putN("packageName", packageName)
    putN("appLabel", appLabel)
    putN("postedAt", postedAt)
    putN("postedAtIso", Instant.ofEpochMilli(postedAt).toString())
    putN("receivedAt", receivedAt)
    putN("notificationKey", notificationKey)
    putN("notificationId", notificationId)
    putN("tag", tag)
    putN("channelId", channelId)
    putN("category", category)
    putN("title", title)
    putN("text", text)
    putN("bigText", bigText)
    putN("subText", subText)
    putN("summaryText", summaryText)
    putN("infoText", infoText)
    putN("textLines", textLines)
    putN("messages", messages)
    putN("tickerText", tickerText)
    putN("fullText", fullText)
    putN("amount", amount)
    putN("currency", currency)
    putN("direction", direction)
    putN("counterparty", counterparty)
    putN("balance", balance)
    putN("reference", reference)
    putN("parserId", parserId)
    putN("parsedAt", parsedAt)
    putN("rawExtras", runCatching { JSONObject(rawExtras) }.getOrElse { rawExtras })
}
