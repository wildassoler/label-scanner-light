package com.wildassoler.scorelight

// Abstraction over "look up a product by barcode" — lets ResultScreenViewModel
// depend on this interface instead of OpenFoodFactsApi directly, so tests
// can swap in a fake implementation instead of hitting the real network.
interface ProductLookupService {
    suspend fun fetchProduct(barcode: String): ScanLookupResult
}

// Real implementation — wraps OpenFoodFactsApi, used in production.
// OpenFoodFactsApi's HttpClient is a shared singleton now (see its
// companion object), so there's no per-instance resource to close here.
class OpenFoodFactsService : ProductLookupService {
    private val api = OpenFoodFactsApi()

    override suspend fun fetchProduct(barcode: String): ScanLookupResult =
        api.fetchProduct(barcode)
}