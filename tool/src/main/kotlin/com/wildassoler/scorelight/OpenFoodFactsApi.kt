package com.wildassoler.scorelight

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive
import io.ktor.serialization.kotlinx.json.json

// Base URL for the OpenFoodFacts read API (v2). No API key required.
private const val OFF_BASE = "https://world.openfoodfacts.org/api/v2"

@Serializable
internal data class OffProductResponse(
    val status: Int = 0,
    val product: OffProduct? = null,
)

@Serializable
internal data class OffProduct(
    @SerialName("product_name") val productName: String? = null,
    @SerialName("nutriscore_grade") val nutriscoreGrade: String? = null,
    @SerialName("nova_group") val novaGroup: Int? = null,
    @SerialName("additives_tags") val additivesTags: List<String> = emptyList(),
    val nutriments: JsonObject? = null,
)

data class ScannedProduct(
    val barcode: String,
    val name: String,
    val nutriscoreGrade: String?,
    val novaGroup: Int?,
    val additivesCount: Int,
    val sugarsPer100g: Double?,
    val saturatedFatPer100g: Double?,
    val saltPer100g: Double?,
)

sealed class ScanLookupResult {
    data class Found(val product: ScannedProduct) : ScanLookupResult()
    data object NotFound : ScanLookupResult()
    data class Error(val message: String) : ScanLookupResult()
}

class OpenFoodFactsApi {
    private val json = Json { ignoreUnknownKeys = true }

    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(json)
        }
    }

    suspend fun fetchProduct(barcode: String): ScanLookupResult {
        return try {
            val fields = "product_name,nutriscore_grade,nova_group,additives_tags,nutriments"
            val response = client.get("$OFF_BASE/product/$barcode.json?fields=$fields")

            if (!response.status.isSuccess()) {
                val body = response.bodyAsText().take(300)
                return ScanLookupResult.Error("HTTP ${response.status.value}: $body")
            }

            val parsed: OffProductResponse = response.body()
            val product = parsed.product

            if (parsed.status != 1 || product == null) {
                ScanLookupResult.NotFound
            } else {
                ScanLookupResult.Found(product.toScannedProduct(barcode))
            }
        } catch (e: Exception) {
            ScanLookupResult.Error(e.message ?: "Unknown network error")
        }
    }

    fun close() {
        client.close()
    }
}

private fun JsonObject?.nutrient(key: String): Double? {
    val element: JsonElement = this?.get("${key}_100g") ?: return null
    return element.jsonPrimitive.doubleOrNull
}

private fun OffProduct.toScannedProduct(barcode: String): ScannedProduct {
    return ScannedProduct(
        barcode = barcode,
        name = productName?.takeIf { it.isNotBlank() } ?: "Unknown product",
        nutriscoreGrade = nutriscoreGrade?.lowercase(),
        novaGroup = novaGroup,
        additivesCount = additivesTags.size,
        sugarsPer100g = nutriments.nutrient("sugars"),
        saturatedFatPer100g = nutriments.nutrient("saturated-fat"),
        saltPer100g = nutriments.nutrient("salt"),
    )
}