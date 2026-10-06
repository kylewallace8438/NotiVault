package com.notivault.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One captured notification = one row.
 *
 * Columns are grouped in three parts:
 *  1. Source:  which app, when, which channel.
 *  2. Content: every text field Android exposes, plus `fullText` (all of them combined; point your regex here)
 *              and `rawExtras` (the ENTIRE notification extras bundle as JSON, so nothing is ever lost).
 *  3. Parsed:  empty until a parser in /parsing fills them. Re-run parsers any time from Settings.
 */
@Entity(
    tableName = "notifications",
    indices = [
        Index("packageName"),
        Index("postedAt"),
        Index(value = ["dedupHash"], unique = true),
    ],
)
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,

    // ---- 1. Source ----
    val packageName: String,
    val appLabel: String,
    val notificationKey: String,
    val notificationId: Int,
    val tag: String?,
    /** When the bank app posted it (epoch millis). */
    val postedAt: Long,
    /** When NotiVault stored it (epoch millis). */
    val receivedAt: Long,
    val channelId: String?,
    val category: String?,

    // ---- 2. Content ----
    val title: String?,
    val text: String?,
    val bigText: String?,
    val subText: String?,
    val summaryText: String?,
    val infoText: String?,
    /** InboxStyle lines, one per line. */
    val textLines: String?,
    /** MessagingStyle messages ("sender: text"), one per line. */
    val messages: String?,
    val tickerText: String?,
    /** All text fields above combined (duplicates removed), newline separated. Target this with regex. */
    val fullText: String,
    /** The whole Notification.extras bundle as JSON. */
    val rawExtras: String,
    /** Prevents the same notification being stored twice (e.g. when the listener reconnects). */
    val dedupHash: String,

    // ---- 3. Parsed (filled by parsers) ----
    val amount: Double? = null,
    val currency: String? = null,
    /** "IN" or "OUT" */
    val direction: String? = null,
    val counterparty: String? = null,
    val balance: Double? = null,
    val reference: String? = null,
    /** Which parser produced the values above, e.g. "generic-v1". Null = nothing matched. */
    val parserId: String? = null,
    val parsedAt: Long? = null,
)

/** Row type for the app filter chips on the home screen. */
data class SourceCount(
    val packageName: String,
    val appLabel: String,
    val count: Int,
)
