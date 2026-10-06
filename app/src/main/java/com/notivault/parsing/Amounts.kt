package com.notivault.parsing

object Amounts {
    /**
     * Turns "1,250,000" / "1.250.000" / "1,234.56" / "1.234,56" / "12.50" into a number.
     *
     * Rule: if both ',' and '.' appear, the last one is the decimal separator.
     * If only one kind appears, it's a thousands separator when it repeats or has exactly 3 digits after it.
     * (So "1.234" becomes 1234, which is what you want for VND. Write a bank-specific parser if that's wrong for you.)
     */
    fun parse(raw: String): Double? {
        val s = raw.trim().replace(" ", "")
        if (s.isEmpty()) return null
        val lastComma = s.lastIndexOf(',')
        val lastDot = s.lastIndexOf('.')
        val normalized = when {
            lastComma >= 0 && lastDot >= 0 ->
                if (lastComma > lastDot) s.replace(".", "").replace(',', '.') else s.replace(",", "")
            lastComma >= 0 -> if (isThousands(s, ',')) s.replace(",", "") else s.replace(',', '.')
            lastDot >= 0 -> if (isThousands(s, '.')) s.replace(".", "") else s
            else -> s
        }
        return normalized.toDoubleOrNull()
    }

    private fun isThousands(s: String, sep: Char): Boolean {
        val parts = s.split(sep)
        return parts.size > 2 || parts.last().length == 3
    }

    /** "đ", "₫", "VNĐ" -> "VND", "$" -> "USD", etc. */
    fun normalizeCurrency(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        return when (raw.trim().uppercase()) {
            "Đ", "₫", "VNĐ", "VND" -> "VND"
            "$", "US$", "USD" -> "USD"
            "€", "EUR" -> "EUR"
            "£", "GBP" -> "GBP"
            "¥", "JPY" -> "JPY"
            "₹", "INR" -> "INR"
            "฿", "THB" -> "THB"
            "₩", "KRW" -> "KRW"
            else -> raw.trim().uppercase()
        }
    }
}
