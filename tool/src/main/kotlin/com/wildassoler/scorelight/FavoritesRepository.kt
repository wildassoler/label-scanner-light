package com.wildassoler.scorelight

import kotlinx.coroutines.flow.Flow

// Extracted from FavoritesStore so ResultScreenViewModel (and friends) can
// depend on this interface instead of the concrete DataStore-backed class —
// same reasoning as ProductLookupService: lets tests use an in-memory fake
// instead of touching real device storage.
interface FavoritesRepository {
    val favorites: Flow<List<ScannedProduct>>
    fun isFavorite(barcode: String): Flow<Boolean>
    suspend fun toggleFavorite(product: ScannedProduct)
}