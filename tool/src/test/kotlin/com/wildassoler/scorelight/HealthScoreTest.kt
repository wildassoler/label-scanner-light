package com.wildassoler.scorelight

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HealthScoreTest {

    @Test
    fun nutriscoreGradeMappingWorks() {
        val grades = mapOf(
            "a" to "Excellent",
            "b" to "Good",
            "c" to "Average",
            "d" to "Poor",
            "e" to "Bad",
            null to "Unrated",
        )

        grades.forEach { (grade, expectedLabel) ->
            val product = ScannedProduct(
                barcode = "123",
                name = "Test",
                brand = null,
                nutriscoreGrade = grade,
                novaGroup = null,
                additivesCount = 0,
                ingredientsText = null,
                energyKcalPer100g = null,
                proteinsPer100g = null,
                carbohydratesPer100g = null,
                fiberPer100g = null,
                sugarsPer100g = null,
                saturatedFatPer100g = null,
                saltPer100g = null,
            )
            val score = calculateHealthScore(product)
            assertEquals(expectedLabel, score.label)
        }
    }

    @Test
    fun highNutrientThresholdsGenerateWarnings() {
        val product = ScannedProduct(
            barcode = "123",
            name = "Salty Sweet Snack",
            brand = null,
            nutriscoreGrade = "d",
            novaGroup = 4,
            additivesCount = 3,
            ingredientsText = null,
            energyKcalPer100g = 500.0,
            proteinsPer100g = null,
            carbohydratesPer100g = null,
            fiberPer100g = null,
            sugarsPer100g = 20.0,
            saturatedFatPer100g = 8.0,
            saltPer100g = 2.0,
        )

        val score = calculateHealthScore(product)

        assertTrue(score.reasons.any { it.contains("NOVA group 4") })
        assertTrue(score.reasons.any { it.contains("High in sugar") })
        assertTrue(score.reasons.any { it.contains("High in saturated fat") })
        assertTrue(score.reasons.any { it.contains("High in salt") })
        assertTrue(score.reasons.any { it.contains("Contains 3 food additive(s)") })
    }
}