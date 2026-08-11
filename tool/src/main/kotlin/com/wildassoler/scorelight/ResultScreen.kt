package com.wildassoler.scorelight

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ResultUiState {
    data object Loading : ResultUiState()
    data class Found(val product: ScannedProduct, val score: HealthScore) : ResultUiState()
    data object NotFound : ResultUiState()
    data class Error(val message: String) : ResultUiState()
}

data class HealthScore(
    val label: String,
    val reasons: List<String>,
)

class ResultScreenViewModel(private val barcode: String) : LightViewModel<Unit>() {
    private val api = OpenFoodFactsApi()

    private val _uiState = MutableStateFlow<ResultUiState>(ResultUiState.Loading)
    val uiState = _uiState.asStateFlow()

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

    override fun createViewModel(): ResultScreenViewModel = ResultScreenViewModel(barcode)

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()
        val state by viewModel.uiState.collectAsState()

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
                    is ResultUiState.Loading -> LoadingContent()
                    is ResultUiState.Found -> FoundContent(current.product, current.score)
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
private fun LoadingContent() {
    Column(modifier = Modifier.fillMaxSize().padding(32.dp)) {
        LightText(text = "Looking up product…", variant = LightTextVariant.Copy, lighten = true)
    }
}

@Composable
private fun MessageContent(message: String) {
    Column(modifier = Modifier.fillMaxSize().padding(32.dp)) {
        LightText(
            text = message,
            variant = LightTextVariant.Copy,
            align = TextAlign.Start,
        )
    }
}

@Composable
private fun FoundContent(product: ScannedProduct, score: HealthScore) {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp)) {
        LightText(
            text = product.name,
            variant = LightTextVariant.Heading,
            modifier = Modifier.padding(bottom = 8.dp),
        )

        LightText(
            text = score.label,
            variant = LightTextVariant.Heading,
            modifier = Modifier.padding(bottom = 24.dp),
        )

        LazyColumn {
            items(score.reasons) { reason ->
                LightText(
                    text = "• $reason",
                    variant = LightTextVariant.Copy,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
            }
        }
    }
}

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

private fun Double.oneDecimal(): String {
    val rounded = kotlin.math.round(this * 10) / 10.0
    return rounded.toString()
}