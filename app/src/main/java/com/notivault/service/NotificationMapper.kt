package com.notivault.service

import android.app.Notification
import android.content.Context
import android.os.Bundle
import android.service.notification.StatusBarNotification
import com.notivault.data.NotificationEntity
import java.security.MessageDigest

/** Converts a system notification into a database row. Returns null for things not worth storing. */
object NotificationMapper {

    /** Group summaries usually just repeat their children ("3 new messages"). */
    private const val SKIP_GROUP_SUMMARIES = true
    /** Ongoing = sticky notifications like "Syncing…" or a running foreground service. */
    private const val SKIP_ONGOING = true

    fun map(context: Context, sbn: StatusBarNotification, now: Long = System.currentTimeMillis()): NotificationEntity? {
        val n = sbn.notification ?: return null
        if (SKIP_ONGOING && sbn.isOngoing) return null
        if (SKIP_GROUP_SUMMARIES && (n.flags and Notification.FLAG_GROUP_SUMMARY) != 0) return null

        val e = n.extras ?: Bundle()
        fun cs(key: String): String? = try {
            e.getCharSequence(key)?.toString()?.trim()?.takeIf { it.isNotEmpty() }
        } catch (_: Throwable) { null }

        val title = cs(Notification.EXTRA_TITLE) ?: cs(Notification.EXTRA_TITLE_BIG)
        val text = cs(Notification.EXTRA_TEXT)
        val bigText = cs(Notification.EXTRA_BIG_TEXT)
        val subText = cs(Notification.EXTRA_SUB_TEXT)
        val summary = cs(Notification.EXTRA_SUMMARY_TEXT)
        val info = cs(Notification.EXTRA_INFO_TEXT)
        val lines = try {
            e.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
                ?.mapNotNull { it?.toString()?.trim()?.takeIf(String::isNotEmpty) }
                ?.joinToString("\n")?.takeIf { it.isNotEmpty() }
        } catch (_: Throwable) { null }
        val messages = extractMessages(e)
        val ticker = n.tickerText?.toString()?.trim()?.takeIf { it.isNotEmpty() }

        val fullText = combine(listOf(title, text, bigText, lines, messages, subText, summary, info))
            .ifEmpty { ticker.orEmpty() }
        if (fullText.isBlank()) return null

        val isTest = sbn.packageName == context.packageName && n.channelId == TestNotification.CHANNEL_ID
        val label = if (isTest) "NotiVault test" else appLabel(context, sbn.packageName)
        val raw = try { ExtrasJson.toJson(e).toString() } catch (t: Throwable) { "{\"_error\":\"${t.javaClass.simpleName}\"}" }

        return NotificationEntity(
            packageName = sbn.packageName,
            appLabel = label,
            notificationKey = sbn.key,
            notificationId = sbn.id,
            tag = sbn.tag,
            postedAt = sbn.postTime,
            receivedAt = now,
            channelId = n.channelId,
            category = n.category,
            title = title,
            text = text,
            bigText = bigText,
            subText = subText,
            summaryText = summary,
            infoText = info,
            textLines = lines,
            messages = messages,
            tickerText = ticker,
            fullText = fullText,
            rawExtras = raw,
            dedupHash = sha256("${sbn.packageName}|${sbn.key}|${n.`when`}|$fullText"),
        )
    }

    /** Joins text parts, dropping blanks, duplicates, and parts already contained in a longer part. */
    private fun combine(parts: List<String?>): String {
        val clean = parts.mapNotNull { it?.trim()?.takeIf(String::isNotEmpty) }.distinct()
        return clean.filter { p -> clean.none { other -> other != p && other.contains(p) } }
            .joinToString("\n")
    }

    @Suppress("DEPRECATION")
    private fun extractMessages(e: Bundle): String? = try {
        e.getParcelableArray(Notification.EXTRA_MESSAGES)
            ?.mapNotNull { p ->
                val b = p as? Bundle ?: return@mapNotNull null
                val t = b.getCharSequence("text")?.toString()?.trim() ?: return@mapNotNull null
                val sender = b.getCharSequence("sender")?.toString()
                if (sender.isNullOrBlank()) t else "$sender: $t"
            }
            ?.joinToString("\n")?.takeIf { it.isNotEmpty() }
    } catch (_: Throwable) { null }

    @Suppress("DEPRECATION")
    private fun appLabel(context: Context, pkg: String): String = try {
        val pm = context.packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
    } catch (_: Throwable) { pkg }

    private fun sha256(s: String): String =
        MessageDigest.getInstance("SHA-256").digest(s.toByteArray()).joinToString("") { "%02x".format(it) }
}
