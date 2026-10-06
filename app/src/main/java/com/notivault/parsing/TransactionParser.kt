package com.notivault.parsing

import com.notivault.data.NotificationEntity

/**
 * Implement this once per bank (see TemplateBankParser.kt), then add it to ParserRegistry.
 *
 * Tip: write your regex against [NotificationEntity.fullText]. It's the cleaned-up text of the
 * notification and is shown on the detail screen, so you can copy real examples from there.
 */
interface TransactionParser {
    /** Stored in the `parserId` column so you know which parser produced a row. Bump the version when you change it. */
    val id: String

    /** Return true for the package names (banks) this parser understands. */
    fun supports(packageName: String): Boolean

    /** Return null when the notification isn't a transaction this parser understands. */
    fun parse(n: NotificationEntity): ParsedTransaction?
}
