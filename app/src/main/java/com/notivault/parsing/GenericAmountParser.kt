package com.notivault.parsing

import com.notivault.data.NotificationEntity

/**
 * A best-effort parser that works on many banks without configuration.
 * It finds: a signed amount ("+1,250,000 VND", "-89.000đ") or an amount with a currency,
 * the balance ("Balance", "Số dư", "SD", "Avl Bal" ...), and guesses the direction from keywords.
 *
 * It does NOT try to find the counterparty; that's too bank-specific. Write a bank parser for that.
 */
object GenericAmountParser : TransactionParser {
    override val id = "generic-v1"

    override fun supports(packageName: String) = true

    override fun parse(n: NotificationEntity): ParsedTransaction? = parseText(n.fullText)

    // Longer tokens first so "VNĐ" wins over "đ".
    private const val CUR = """VNĐ|VND|USD|US\$|EUR|GBP|JPY|SGD|THB|AUD|CAD|INR|IDR|MYR|PHP|KRW|CNY|đ|₫|\$|€|£|¥|₹|฿|₩"""
    private const val NUM = """\d[\d.,]*\d|\d"""

    /** "+1,250,000 VND", "-USD 12.50", "GD: +2,500,000VND" (sign must not follow a letter/digit, so dates are skipped). */
    private val SIGNED = Regex("""(?<![\p{L}\p{N}])([+\-−])\s?(?:($CUR)\s?)?($NUM)(?:\s?($CUR))?""", RegexOption.IGNORE_CASE)

    /** "USD 12.50" or "1,250,000 VND" (no sign). */
    private val UNSIGNED = Regex("""(?<![\p{L}\p{N}.,])(?:($CUR)\s?($NUM)|($NUM)\s?($CUR))""", RegexOption.IGNORE_CASE)

    /** "Balance: 8,430,000 VND", "Số dư 3,200,000VND", "SD: 12,345,678", "Avl Bal INR 20,000.00". */
    private val BALANCE = Regex(
        """(?:số\s*dư|so\s*du|(?<![\p{L}])(?:avail(?:able)?\.?\s*)?bal(?:ance)?\.?(?![\p{L}])|(?<![\p{L}])SD(?![\p{L}]))[^\d\n]{0,25}?($NUM)(?:\s?($CUR))?""",
        RegexOption.IGNORE_CASE,
    )

    private val IN_WORDS = Regex("""(?<![\p{L}])(received|credited|deposit(?:ed)?|incoming|refund(?:ed)?|nhận|nhan|ghi có|ghi co|cộng|cong)(?![\p{L}])""", RegexOption.IGNORE_CASE)
    private val OUT_WORDS = Regex("""(?<![\p{L}])(paid|debited|withdrawn?|withdrawal|spent|sent|purchase|payment|ghi nợ|ghi no|trừ|thanh toán|thanh toan|chuyển đi|chuyen di)(?![\p{L}])""", RegexOption.IGNORE_CASE)

    fun parseText(text: String): ParsedTransaction? {
        // 1) Balance: first match that looks like money (has a currency or a thousands/decimal separator),
        //    so account numbers like "Số dư TK 0123456789" are skipped.
        var balance: Double? = null
        var balanceRange: IntRange? = null
        for (m in BALANCE.findAll(text)) {
            val num = m.groups[1] ?: continue
            if (m.groupValues[2].isEmpty() && !hasSeparator(num.value)) continue
            balance = Amounts.parse(num.value)
            balanceRange = num.range
            break
        }
        fun outsideBalance(r: IntRange) = balanceRange == null || r.last < balanceRange.first || r.first > balanceRange.last

        // 2) Signed amount: the sign tells us the direction.
        for (m in SIGNED.findAll(text)) {
            val num = m.groups[3] ?: continue
            if (!outsideBalance(num.range)) continue
            val cur = m.groupValues[2].ifEmpty { m.groupValues[4] }
            if (cur.isEmpty() && !hasSeparator(num.value)) continue
            val amount = Amounts.parse(num.value) ?: continue
            return ParsedTransaction(
                amount = amount,
                currency = Amounts.normalizeCurrency(cur),
                direction = if (m.groupValues[1] == "+") Direction.IN else Direction.OUT,
                balance = balance,
            )
        }

        // 3) Unsigned amount with a currency: guess the direction from keywords.
        for (m in UNSIGNED.findAll(text)) {
            val num = m.groups[2] ?: m.groups[3] ?: continue
            if (!outsideBalance(num.range)) continue
            val amount = Amounts.parse(num.value) ?: continue
            val cur = m.groupValues[1].ifEmpty { m.groupValues[4] }
            return ParsedTransaction(
                amount = amount,
                currency = Amounts.normalizeCurrency(cur),
                direction = guessDirection(text),
                balance = balance,
            )
        }

        return if (balance != null) ParsedTransaction(balance = balance) else null
    }

    private fun hasSeparator(s: String) = s.contains(',') || s.contains('.')

    private fun guessDirection(text: String): Direction? {
        val isIn = IN_WORDS.containsMatchIn(text)
        val isOut = OUT_WORDS.containsMatchIn(text)
        return when {
            isIn && !isOut -> Direction.IN
            isOut && !isIn -> Direction.OUT
            else -> null
        }
    }
}
