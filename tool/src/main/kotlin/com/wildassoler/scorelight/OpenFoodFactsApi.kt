package com.wildassoler.scorelight

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive

private const val OFF_BASE = "https://world.openfoodfacts.org/api/v2"
private const val REQUEST_TIMEOUT_MILLIS = 10_000L

private const val OFF_FIELDS = "product_name,brands,nutriscore_grade,nova_group," +
        "additives_tags,ingredients_text,nutriments"

@Serializable
internal data class OffProductResponse(
    val status: Int = 0,
    val product: OffProduct? = null,
)

@Serializable
internal data class OffProduct(
    @SerialName("product_name") val productName: String? = null,
    val brands: String? = null,
    @SerialName("nutriscore_grade") val nutriscoreGrade: String? = null,
    @SerialName("nova_group") val novaGroup: Int? = null,
    @SerialName("additives_tags") val additivesTags: List<String> = emptyList(),
    @SerialName("ingredients_text") val ingredientsText: String? = null,
    val nutriments: JsonObject? = null,
)

@Serializable
data class ScannedProduct(
    val barcode: String,
    val name: String,
    val brand: String?,
    val nutriscoreGrade: String?,
    val novaGroup: Int?,
    val additivesCount: Int,
    val ingredientsText: String?,
    val energyKcalPer100g: Double?,
    val proteinsPer100g: Double?,
    val carbohydratesPer100g: Double?,
    val fiberPer100g: Double?,
    val sugarsPer100g: Double?,
    val saturatedFatPer100g: Double?,
    val saltPer100g: Double?,
)

sealed class ScanLookupResult {
    data class Found(val product: ScannedProduct) : ScanLookupResult()
    data object NotFound : ScanLookupResult()
    data class Error(val message: String) : ScanLookupResult()
}

class OpenFoodFactsApi(private val httpClient: HttpClient = client) {

    suspend fun fetchProduct(barcode: String): ScanLookupResult {
        return try {
            val response = httpClient.get("$OFF_BASE/product/$barcode.json?fields=$OFF_FIELDS")

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
        } catch (e: HttpRequestTimeoutException) {
            ScanLookupResult.Error("Request timed out. Check your connection and try again.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            ScanLookupResult.Error(e.message ?: "Unknown network error")
        }
    }

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        private val client = HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json(json)
            }
            install(HttpTimeout) {
                requestTimeoutMillis = REQUEST_TIMEOUT_MILLIS
                connectTimeoutMillis = REQUEST_TIMEOUT_MILLIS
                socketTimeoutMillis = REQUEST_TIMEOUT_MILLIS
            }
        }
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
        brand = brands?.split(",")?.firstOrNull()?.trim()?.takeIf { it.isNotBlank() },
        nutriscoreGrade = nutriscoreGrade?.lowercase(),
        novaGroup = novaGroup,
        additivesCount = additivesTags.size,
        ingredientsText = ingredientsText?.takeIf { it.isNotBlank() },
        energyKcalPer100g = nutriments.nutrient("energy-kcal"),
        proteinsPer100g = nutriments.nutrient("proteins"),
        carbohydratesPer100g = nutriments.nutrient("carbohydrates"),
        fiberPer100g = nutriments.nutrient("fiber"),
        sugarsPer100g = nutriments.nutrient("sugars"),
        saturatedFatPer100g = nutriments.nutrient("saturated-fat"),
        saltPer100g = nutriments.nutrient("salt"),
    )
}