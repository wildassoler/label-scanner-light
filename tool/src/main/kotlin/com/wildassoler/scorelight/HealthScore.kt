package com.wildassoler.scorelight

// Pure domain logic — no Compose, no Android, no SDK imports. Testable in
// plain JUnit without touching a device or emulator.

/** The health score shown for a product: a short label plus the reasons behind it. */
data class HealthScore(
    val label: String,
    val reasons: List<String>,
)

// "High in ___" thresholds, per 100g. These match the ones Nutri-Score /
// French public health guidance (Repères nutritionnels) uses for its own
// "high in" nutrient warnings — not something we invented ourselves.
private const val HIGH_SUGAR_THRESHOLD_G = 15.0
private const val HIGH_SATURATED_FAT_THRESHOLD_G = 5.0
private const val HIGH_SALT_THRESHOLD_G = 1.5

// NOVA group 4 = "ultra-processed food" in the NOVA food classification.
private const val NOVA_ULTRA_PROCESSED_GROUP = 4

// Turns raw OpenFoodFacts fields into a short label + a few plain-English
// reasons. Deliberately simple (no weighted formula) — Nutri-Score is
// already a well-reviewed, published scoring system, so we lean on it for
// the label and just surface the specific numbers behind it as the reasons.
fun calculateHealthScore(product: ScannedProduct): HealthScore {
    val reasons = mutableListOf<String>()

    val label = when (product.nutriscoreGrade) {
        "a" -> "Excellent"
        "b" -> "Good"
        "c" -> "Average"
        "d" -> "Poor"
        "e" -> "Bad"
        else -> "Unrated"
    }

    if (product.nutriscoreGrade != null) {
        reasons.add("Nutri-Score: ${product.nutriscoreGrade.uppercase()}")
    } else {
        reasons.add("No Nutri-Score available for this product.")
    }

    if (product.novaGroup == NOVA_ULTRA_PROCESSED_GROUP) {
        reasons.add("Ultra-processed food (NOVA group 4).")
    }

    product.sugarsPer100g?.let { sugars ->
        if (sugars > HIGH_SUGAR_THRESHOLD_G) {
            reasons.add("High in sugar (${sugars.oneDecimal()}g per 100g).")
        }
    }
    product.saturatedFatPer100g?.let { fat ->
        if (fat > HIGH_SATURATED_FAT_THRESHOLD_G) {
            reasons.add("High in saturated fat (${fat.oneDecimal()}g per 100g).")
        }
    }
    product.saltPer100g?.let { salt ->
        if (salt > HIGH_SALT_THRESHOLD_G) {
            reasons.add("High in salt (${salt.oneDecimal()}g per 100g).")
        }
    }

    if (product.additivesCount > 0) {
        reasons.add("Contains ${product.additivesCount} food additive(s).")
    }

    return HealthScore(label = label, reasons = reasons)
}

fun Double.oneDecimal(): String {
    val rounded = kotlin.math.round(this * 10) / 10.0
    return rounded.toString()
}