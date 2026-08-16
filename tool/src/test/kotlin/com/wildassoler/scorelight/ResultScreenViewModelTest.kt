package com.wildassoler.scorelight

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

// Returns a fixed result for every lookup — enough to drive the
// ViewModel's state transitions without touching the real network.
private class FakeProductLookupService(
    private val result: ScanLookupResult,
) : ProductLookupService {
    override suspend fun fetchProduct(barcode: String): ScanLookupResult = result
}

// Plain in-memory favourites — no DataStore, no disk I/O. Real persistence
// is FavoritesStore's job; that's a separate concern from what
// ResultScreenViewModel needs to be correct.
private class InMemoryFavoritesRepository : FavoritesRepository {
    private val state = MutableStateFlow<List<ScannedProduct>>(emptyList())
    override val favorites: Flow<List<ScannedProduct>> = state.asStateFlow()

    override fun isFavorite(barcode: String): Flow<Boolean> =
        favorites.map { list -> list.any { it.barcode == barcode } }

    override suspend fun toggleFavorite(product: ScannedProduct) {
        state.value = if (state.value.any { it.barcode == product.barcode }) {
            state.value.filterNot { it.barcode == product.barcode }
        } else {
            state.value + product
        }
    }
}

private val sampleProduct = ScannedProduct(
    barcode = "1234567890123",
    name = "Sample Product",
    brand = "Sample Brand",
    nutriscoreGrade = "b",
    novaGroup = 2,
    additivesCount = 0,
    ingredientsText = null,
    energyKcalPer100g = null,
    proteinsPer100g = null,
    carbohydratesPer100g = null,
    fiberPer100g = null,
    sugarsPer100g = null,
    saturatedFatPer100g = null,
    saltPer100g = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
class ResultScreenViewModelTest {

    // viewModelScope launches on Dispatchers.Main.immediate — swap in a
    // test dispatcher so coroutines run synchronously within runTest.
    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun foundResultUpdatesStateWithProductAndCalculatedScore() = runTest {
        val viewModel = ResultScreenViewModel(
            barcode = sampleProduct.barcode,
            lookupService = FakeProductLookupService(ScanLookupResult.Found(sampleProduct)),
            favoritesStore = InMemoryFavoritesRepository(),
        )

        val state = assertIs<ResultUiState.Found>(viewModel.uiState.value)
        assertEquals(sampleProduct, state.product)
        assertEquals("Good", state.score.label) // nutriscoreGrade "b" -> Good
    }

    @Test
    fun notFoundResultUpdatesStateAccordingly() = runTest {
        val viewModel = ResultScreenViewModel(
            barcode = "0000000000000",
            lookupService = FakeProductLookupService(ScanLookupResult.NotFound),
            favoritesStore = InMemoryFavoritesRepository(),
        )

        assertIs<ResultUiState.NotFound>(viewModel.uiState.value)
    }

    @Test
    fun errorResultCarriesTheMessageThrough() = runTest {
        val viewModel = ResultScreenViewModel(
            barcode = "0000000000000",
            lookupService = FakeProductLookupService(ScanLookupResult.Error("timeout")),
            favoritesStore = InMemoryFavoritesRepository(),
        )

        val state = assertIs<ResultUiState.Error>(viewModel.uiState.value)
        assertEquals("timeout", state.message)
    }

    @Test
    fun isFavoriteReflectsTheStoreAndTogglingUpdatesIt() = runTest {
        val favoritesStore = InMemoryFavoritesRepository()
        val viewModel = ResultScreenViewModel(
            barcode = sampleProduct.barcode,
            lookupService = FakeProductLookupService(ScanLookupResult.Found(sampleProduct)),
            favoritesStore = favoritesStore,
        )

        // isFavorite uses WhileSubscribed(5000) — it only actually collects
        // from the underlying store once something is actively subscribed
        // to it, the same way collectAsState() keeps it "warm" on the real
        // screen. Start a collector, then advanceUntilIdle() to let the
        // stateIn sharing coroutine actually run and settle, rather than
        // assuming it happened synchronously.
        backgroundScope.launch { viewModel.isFavorite.collect {} }
        advanceUntilIdle()

        assertEquals(false, viewModel.isFavorite.value)

        viewModel.toggleFavorite(sampleProduct)
        advanceUntilIdle()
        assertEquals(true, viewModel.isFavorite.value)

        viewModel.toggleFavorite(sampleProduct)
        advanceUntilIdle()
        assertEquals(false, viewModel.isFavorite.value)
    }
}