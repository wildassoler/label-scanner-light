package com.wildassoler.scorelight

fun normalizeBarcode(raw: String): String {
    val digits = raw.filter { it.isDigit() }
    return if (digits.length == 12) "0$digits" else digits
}