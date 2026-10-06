package com.notivault.parsing

/** What a parser extracts from one notification. Every field is optional. */
data class ParsedTransaction(
    val amount: Double? = null,
    val currency: String? = null,
    val direction: Direction? = null,
    /** Who paid you / who you paid. */
    val counterparty: String? = null,
    /** Balance after the transaction, if the bank shows it. */
    val balance: Double? = null,
    /** Transfer note / reference / transaction id. */
    val reference: String? = null,
)

enum class Direction { IN, OUT }
