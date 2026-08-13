package com.wildassoler.scorelight

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

private val FAVORITES_KEY = stringPreferencesKey("favorite_products")
private val json = Json { ignoreUnknownKeys = true }

class FavoritesStore(private val dataStore: DataStore<Preferences>) {

    val favorites: Flow<List<ScannedProduct>> = dataStore.data.map { prefs ->
        decodeList(prefs[FAVORITES_KEY])
    }

    fun isFavorite(barcode: String): Flow<Boolean> =
        favorites.map { list -> list.any { it.barcode == barcode } }

    suspend fun toggleFavorite(product: ScannedProduct) {
        dataStore.edit { prefs ->
            val current = decodeList(prefs[FAVORITES_KEY])
            val alreadyFavorite = current.any { it.barcode == product.barcode }

            val updated = if (alreadyFavorite) {
                current.filterNot { it.barcode == product.barcode }
            } else {
                current + product
            }

            prefs[FAVORITES_KEY] = json.encodeToString(updated)
        }
    }

    private fun decodeList(raw: String?): List<ScannedProduct> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching { json.decodeFromString<List<ScannedProduct>>(raw) }
            .getOrDefault(emptyList())
    }
}