package com.notivault.parsing

import com.notivault.data.NotificationEntity

/**
 * COPY THIS FILE for each bank, then register it in ParserRegistry (above GenericAmountParser).
 *
 * How to write one:
 *  1. Let the app collect a few real notifications from the bank.
 *  2. Open one in the app, copy the "Full text" and the package name.
 *  3. Write regexes against that text (regex101.com, flavor "Java 8", is handy for testing).
 *
 * This example targets text like:
 *   TK 1234xxx789|GD: +2,500,000VND 06/10/26 09:15|SD: 12,345,678VND|ND: NGUYEN VAN A chuyen tien
 */
object TemplateBankParser : TransactionParser {
    override val id = "template-bank-v1"

    private val PACKAGES = setOf("com.example.bank") // <- the bank app's package name

    override fun supports(packageName: String) = packageName in PACKAGES

    private val AMOUNT = Regex("""GD:\s*([+-])([\d.,]+)\s*VND""")
    private val BALANCE = Regex("""SD:\s*([\d.,]+)\s*VND""")
    private val NOTE = Regex("""ND:\s*(.+)""")
    // Many banks put the sender's name at the start of the transfer note, in capitals.
    private val NAME_IN_NOTE = Regex("""^([A-Z][A-Z ]{2,40}?)\s+(?:chuyen|ck|transfer|tt)""", RegexOption.IGNORE_CASE)

    override fun parse(n: NotificationEntity): ParsedTransaction? {
        val text = n.fullText
        val amount = AMOUNT.find(text) ?: return null
        val note = NOTE.find(text)?.groupValues?.get(1)?.trim()
        return ParsedTransaction(
            amount = Amounts.parse(amount.groupValues[2]),
            currency = "VND",
            direction = if (amount.groupValues[1] == "+") Direction.IN else Direction.OUT,
            balance = BALANCE.find(text)?.groupValues?.get(1)?.let(Amounts::parse),
            reference = note,
            counterparty = note?.let { NAME_IN_NOTE.find(it)?.groupValues?.get(1)?.trim() },
        )
    }
}
