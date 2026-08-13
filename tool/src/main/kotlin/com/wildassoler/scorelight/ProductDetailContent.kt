package com.wildassoler.scorelight

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.thelightphone.sdk.ui.LightIcons
import com.thelightphone.sdk.ui.LightIcon
import com.thelightphone.sdk.ui.LightText
import com.thelightphone.sdk.ui.LightTextVariant
import com.thelightphone.sdk.ui.LightThemeTokens
import com.thelightphone.sdk.ui.lightClickable

@Composable
fun ProductDetailContent(
    product: ScannedProduct,
    score: HealthScore,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
) {
    val scoreLabelStyle = LightThemeTokens.typography.heading.copy(
        color = LightThemeTokens.colors.content,
        fontSize = LightThemeTokens.typography.heading.fontSize * 1.4f,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
    )

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
        ) {
            LightText(
                text = product.name,
                variant = LightTextVariant.Heading,
                modifier = Modifier.padding(end = 8.dp),
            )
            LightIcon(
                icon = if (isFavorite) LightIcons.STAR else LightIcons.STAR_OUTLINE,
                modifier = Modifier.lightClickable(onClick = onToggleFavorite),
            )
        }

        product.brand?.let { brand ->
            LightText(
                text = brand,
                variant = LightTextVariant.Copy,
                lighten = true,
                modifier = Modifier.padding(bottom = 16.dp),
            )
        }

        androidx.compose.foundation.text.BasicText(
            text = score.label,
            style = scoreLabelStyle,
            modifier = Modifier.padding(bottom = 24.dp),
        )

        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            item {
                LightText(
                    text = "Why",
                    variant = LightTextVariant.Copy,
                    lighten = true,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            items(score.reasons) { reason ->
                LightText(
                    text = "• $reason",
                    variant = LightTextVariant.Copy,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }

            item {
                LightText(
                    text = "Nutrition (per 100g)",
                    variant = LightTextVariant.Copy,
                    lighten = true,
                    modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
                )
            }
            items(nutritionLines(product)) { line ->
                LightText(
                    text = line,
                    variant = LightTextVariant.Copy,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }

            product.ingredientsText?.let { ingredients ->
                item {
                    LightText(
                        text = "Ingredients",
                        variant = LightTextVariant.Copy,
                        lighten = true,
                        modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
                    )
                }
                item {
                    LightText(
                        text = ingredients,
                        variant = LightTextVariant.Copy,
                        modifier = Modifier.padding(bottom = 32.dp),
                    )
                }
            }
        }
    }
}

private fun nutritionLines(product: ScannedProduct): List<String> {
    val lines = mutableListOf<String>()
    product.energyKcalPer100g?.let { lines.add("Calories: ${it.oneDecimal()} kcal") }
    product.proteinsPer100g?.let { lines.add("Protein: ${it.oneDecimal()}g") }
    product.carbohydratesPer100g?.let { lines.add("Carbohydrates: ${it.oneDecimal()}g") }
    product.fiberPer100g?.let { lines.add("Fibre: ${it.oneDecimal()}g") }
    product.sugarsPer100g?.let { lines.add("Sugar: ${it.oneDecimal()}g") }
    product.saturatedFatPer100g?.let { lines.add("Saturated fat: ${it.oneDecimal()}g") }
    product.saltPer100g?.let { lines.add("Salt: ${it.oneDecimal()}g") }
    if (lines.isEmpty()) lines.add("No nutrition data available for this product.")
    return lines
}

fun Double.oneDecimal(): String {
    val rounded = kotlin.math.round(this * 10) / 10.0
    return rounded.toString()
}