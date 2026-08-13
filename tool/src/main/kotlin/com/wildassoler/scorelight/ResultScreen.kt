package com.wildassoler.scorelight

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import com.thelightphone.sdk.LightScreen
import com.thelightphone.sdk.LightViewModel
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.SimpleLightScreen
import com.thelightphone.sdk.ui.LightBarButton
import com.thelightphone.sdk.ui.LightIcons
import com.thelightphone.sdk.ui.LightText
import com.thelightphone.sdk.ui.LightTextVariant
import com.thelightphone.sdk.ui.LightTheme
import com.thelightphone.sdk.ui.LightThemeController
import com.thelightphone.sdk.ui.LightTopBar
import com.thelightphone.sdk.ui.LightTopBarCenter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class ResultUiState {
    data object Loading : ResultUiState()
    data class Found(val product: ScannedProduct, val score: HealthScore) : ResultUiState()
    data object NotFound : ResultUiState()
    data class Error(val message: String) : ResultUiState()
}

// The health score we show for a product: a short label ("Good", "Poor"...)
// plus a handful of one-line reasons explaining where it came from.
data class HealthScore(
    val label: String,
    val reasons: List<String>,
)

class ResultScreenViewModel(
    private val barcode: String,
    private val favoritesStore: FavoritesStore,
) : LightViewModel<Unit>() {
    private val api = OpenFoodFactsApi()

    private val _uiState = MutableStateFlow<ResultUiState>(ResultUiState.Loading)
    val uiState = _uiState.asStateFlow()

    val isFavorite = favoritesStore.isFavorite(barcode)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    override fun onScreenShow(screen: SimpleLightScreen<Unit>) {
        super.onScreenShow(screen)
        lookUp()
    }

    private fun lookUp() {
        viewModelScope.launch {
            _uiState.value = ResultUiState.Loading
            when (val result = api.fetchProduct(barcode)) {
                is ScanLookupResult.Found -> {
                    _uiState.value = ResultUiState.Found(
                        product = result.product,
                        score = calculateHealthScore(result.product),
                    )
                }
                is ScanLookupResult.NotFound -> _uiState.value = ResultUiState.NotFound
                is ScanLookupResult.Error -> _uiState.value = ResultUiState.Error(result.message)
            }
        }
    }

    fun toggleFavorite(product: ScannedProduct) {
        viewModelScope.launch { favoritesStore.toggleFavorite(product) }
    }

    override fun onCleared() {
        super.onCleared()
        api.close()
    }
}

class ResultScreen(
    sealedActivity: SealedLightActivity,
    private val barcode: String,
) : LightScreen<Unit, ResultScreenViewModel>(sealedActivity) {

    override val viewModelClass: Class<ResultScreenViewModel>
        get() = ResultScreenViewModel::class.java

    override fun createViewModel(): ResultScreenViewModel =
        ResultScreenViewModel(barcode, FavoritesStore(lightContext.dataStore))

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()
        val state by viewModel.uiState.collectAsState()
        val isFavorite by viewModel.isFavorite.collectAsState()

        LightTheme(colors = themeColors) {
            Column(modifier = Modifier.fillMaxSize()) {
                LightTopBar(
                    leftButton = LightBarButton.LightIcon(
                        icon = LightIcons.BACK,
                        onClick = { goBack() },
                    ),
                    center = LightTopBarCenter.Text("Result"),
                    modifier = Modifier.padding(bottom = 16.dp),
                )

                when (val current = state) {
                    is ResultUiState.Loading -> MessageContent("Looking up product…")
                    is ResultUiState.Found -> ProductDetailContent(
                        product = current.product,
                        score = current.score,
                        isFavorite = isFavorite,
                        onToggleFavorite = { viewModel.toggleFavorite(current.product) },
                    )
                    is ResultUiState.NotFound -> MessageContent(
                        "Product not found in the OpenFoodFacts database.",
                    )
                    is ResultUiState.Error -> MessageContent(
                        "Something went wrong: ${current.message}",
                    )
                }
            }
        }
    }
}

@Composable
private fun MessageContent(message: String) {
    Box(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        contentAlignment = Alignment.TopStart,
    ) {
        LightText(text = message, variant = LightTextVariant.Copy, align = TextAlign.Start)
    }
}

// Turns raw OpenFoodFacts fields into a short label + a few plain-English
// reasons. Deliberately simple (no weighted formula) — Nutri-Score is
// already a well-reviewed, published scoring system, so we lean on it for
// the label and just surface the specific numbers behind it as the reasons.
fun calculateHealthScore(product: ScannedProduct): HealthScore {
    val reasons = mutableListOf<String>()

    val label = when (product.nutriscoreGrade) {
        "a" -> "Excellent"
        "b" -> "Good"
        "c" -> "Average"
        "d" -> "Poor"
        "e" -> "Bad"
        else -> "Unrated"
    }

    if (product.nutriscoreGrade != null) {
        reasons.add("Nutri-Score: ${product.nutriscoreGrade.uppercase()}")
    } else {
        reasons.add("No Nutri-Score available for this product.")
    }

    if (product.novaGroup == 4) {
        reasons.add("Ultra-processed food (NOVA group 4).")
    }

    // Thresholds below are the same ones used by Nutri-Score / French
    // public health guidance for "high in" nutrient warnings.
    product.sugarsPer100g?.let { sugars ->
        if (sugars > 15.0) reasons.add("High in sugar (${sugars.oneDecimal()}g per 100g).")
    }
    product.saturatedFatPer100g?.let { fat ->
        if (fat > 5.0) reasons.add("High in saturated fat (${fat.oneDecimal()}g per 100g).")
    }
    product.saltPer100g?.let { salt ->
        if (salt > 1.5) reasons.add("High in salt (${salt.oneDecimal()}g per 100g).")
    }

    if (product.additivesCount > 0) {
        reasons.add("Contains ${product.additivesCount} food additive(s).")
    }

    return HealthScore(label = label, reasons = reasons)
}