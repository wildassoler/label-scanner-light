package com.wildassoler.scorelight

// Abstraction over "look up a product by barcode" — lets ResultScreenViewModel
// depend on this interface instead of OpenFoodFactsApi directly, so tests
// can swap in a fake implementation instead of hitting the real network.
interface ProductLookupService {
    suspend fun fetchProduct(barcode: String): ScanLookupResult
}

// Real implementation — wraps OpenFoodFactsApi, used in production.
class OpenFoodFactsService : ProductLookupService {
    private val api = OpenFoodFactsApi()

    override suspend fun fetchProduct(barcode: String): ScanLookupResult =
        api.fetchProduct(barcode)

    fun close() = api.close()
}