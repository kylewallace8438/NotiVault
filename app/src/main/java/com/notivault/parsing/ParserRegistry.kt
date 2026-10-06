package com.notivault.parsing

import com.notivault.data.NotificationEntity

object ParserRegistry {
    /**
     * Tried in order; the first parser that supports the app AND returns a result wins.
     * Put bank-specific parsers first and keep GenericAmountParser last as the fallback.
     */
    val parsers: List<TransactionParser> = listOf(
        // TemplateBankParser,
        GenericAmountParser,
    )

    /**
     * Returns a copy of [e] with the parsed columns filled in (or cleared if nothing matched).
     * Called for every new notification, and for every stored row by Settings > Re-run parsers.
     */
    fun apply(e: NotificationEntity, now: Long = System.currentTimeMillis()): NotificationEntity {
        val cleared = e.copy(
            amount = null, currency = null, direction = null, counterparty = null,
            balance = null, reference = null, parserId = null, parsedAt = null,
        )
        for (p in parsers) {
            if (!p.supports(e.packageName)) continue
            val r = runCatching { p.parse(e) }.getOrNull() ?: continue
            return cleared.copy(
                amount = r.amount,
                currency = r.currency,
                direction = r.direction?.name,
                counterparty = r.counterparty,
                balance = r.balance,
                reference = r.reference,
                parserId = p.id,
                parsedAt = now,
            )
        }
        return cleared
    }
}
