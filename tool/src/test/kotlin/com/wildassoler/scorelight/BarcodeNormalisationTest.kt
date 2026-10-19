package com.wildassoler.scorelight

import kotlin.test.Test
import kotlin.test.assertEquals

class BarcodeNormalisationTest {

    @Test
    fun twelveDigitUpcPrependsZero() {
        val result = normalizeBarcode("012345678905")
        assertEquals("0012345678905", result)
    }

    @Test
    fun thirteenDigitEanStaysThirteenDigits() {
        val result = normalizeBarcode("7891234567890")
        assertEquals("7891234567890", result)
    }

    @Test
    fun nonDigitsAreStripped() {
        val result = normalizeBarcode("789-123 456 7890")
        assertEquals("7891234567890", result)
    }
}