package com.wildassoler.scorelight

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive

private const val OFF_BASE = "https://world.openfoodfacts.org/api/v2"

// Fields requested from OpenFoodFacts. Kept as one list so the response
// parsing below and the query string can't drift out of sync.
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
    // comma-separated, e.g. "Milka, Mondelez"
    val brands: String? = null,
    @SerialName("nutriscore_grade") val nutriscoreGrade: String? = null,
    @SerialName("nova_group") val novaGroup: Int? = null,
    @SerialName("additives_tags") val additivesTags: List<String> = emptyList(),
    @SerialName("ingredients_text") val ingredientsText: String? = null,
    val nutriments: JsonObject? = null,
)

// A trimmed-down, UI-ready view of a product. Serializable so it can be
// cached whole in FavoritesStore — favourited products don't need a
// second network call to display later.
@Serializable
data class ScannedProduct(
    val barcode: String,
    val name: String,
    val brand: String?,
    val nutriscoreGrade: String?, // "a".."e", or null if unknown
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

class OpenFoodFactsApi {
    private val json = Json { ignoreUnknownKeys = true }

    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(json)
        }
    }

    suspend fun fetchProduct(barcode: String): ScanLookupResult {
        return try {
            val response = client.get("$OFF_BASE/product/$barcode.json?fields=$OFF_FIELDS")

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

// Pulls a numeric nutrient out of the loosely-typed `nutriments` object.
// OpenFoodFacts always suffixes per-100g fields with "_100g", e.g.
// "sugars_100g", "saturated-fat_100g", "proteins_100g".
private fun JsonObject?.nutrient(key: String): Double? {
    val element: JsonElement = this?.get("${key}_100g") ?: return null
    return element.jsonPrimitive.doubleOrNull
}

private fun OffProduct.toScannedProduct(barcode: String): ScannedProduct {
    return ScannedProduct(
        barcode = barcode,
        name = productName?.takeIf { it.isNotBlank() } ?: "Unknown product",
        // Brands field can list several ("Milka, Mondelez International") —
        // just take the first one, which is normally the actual brand name.
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