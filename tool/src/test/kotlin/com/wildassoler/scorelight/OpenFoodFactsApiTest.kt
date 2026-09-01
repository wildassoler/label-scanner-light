package com.wildassoler.scorelight

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

private fun fakeClient(status: HttpStatusCode, body: String): HttpClient {
    val mockEngine = MockEngine { _ ->
        respond(
            content = body,
            status = status,
            headers = headersOf(HttpHeaders.ContentType, "application/json"),
        )
    }
    return HttpClient(mockEngine) {
        install(ContentNegotiation) { json() }
    }
}

class OpenFoodFactsApiTest {

    @Test
    fun foundProductParsesAllFieldsCorrectly() = runTest {
        val body = """
            {
              "status": 1,
              "product": {
                "product_name": "Test Chocolate",
                "brands": "TestBrand, TestParentCo",
                "nutriscore_grade": "D",
                "nova_group": 4,
                "additives_tags": ["en:e322", "en:e500"],
                "ingredients_text": "Sugar, cocoa, milk",
                "nutriments": {
                  "energy-kcal_100g": 540.0,
                  "sugars_100g": 52.0,
                  "saturated-fat_100g": 30.0,
                  "salt_100g": 0.1
                }
              }
            }
        """.trimIndent()

        val api = OpenFoodFactsApi(httpClient = fakeClient(HttpStatusCode.OK, body))
        val result = api.fetchProduct("1234567890123")

        val found = assertIs<ScanLookupResult.Found>(result)
        assertEquals("Test Chocolate", found.product.name)
        assertEquals("TestBrand", found.product.brand)
        assertEquals("d", found.product.nutriscoreGrade)
        assertEquals(4, found.product.novaGroup)
        assertEquals(2, found.product.additivesCount)
        assertEquals("Sugar, cocoa, milk", found.product.ingredientsText)
        assertEquals(540.0, found.product.energyKcalPer100g)
        assertEquals(52.0, found.product.sugarsPer100g)
    }

    @Test
    fun statusZeroMeansNotFoundEvenWithHttp200() = runTest {
        val body = """{ "status": 0 }"""
        val api = OpenFoodFactsApi(httpClient = fakeClient(HttpStatusCode.OK, body))

        val result = api.fetchProduct("0000000000000")

        assertIs<ScanLookupResult.NotFound>(result)
    }

    @Test
    fun missingProductNameFallsBackToPlaceholder() = runTest {
        val body = """{ "status": 1, "product": {} }"""
        val api = OpenFoodFactsApi(httpClient = fakeClient(HttpStatusCode.OK, body))

        val result = api.fetchProduct("1234567890123")

        val found = assertIs<ScanLookupResult.Found>(result)
        assertEquals("Unknown product", found.product.name)
    }

    @Test
    fun serverErrorBecomesAnErrorResult() = runTest {
        val api = OpenFoodFactsApi(
            httpClient = fakeClient(HttpStatusCode.InternalServerError, "Internal Server Error"),
        )

        val result = api.fetchProduct("1234567890123")

        val error = assertIs<ScanLookupResult.Error>(result)
        assertEquals(true, error.message.contains("500"))
    }

    @Test
    fun blankIngredientsTextIsTreatedAsAbsent() = runTest {
        val body = """
            { "status": 1, "product": { "ingredients_text": "   " } }
        """.trimIndent()
        val api = OpenFoodFactsApi(httpClient = fakeClient(HttpStatusCode.OK, body))

        val result = api.fetchProduct("1234567890123")

        val found = assertIs<ScanLookupResult.Found>(result)
        assertEquals(null, found.product.ingredientsText)
    }
}