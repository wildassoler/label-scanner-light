package com.wildassoler.scorelight

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import com.thelightphone.sdk.LightScreen
import com.thelightphone.sdk.LightViewModel
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.ui.LightBarButton
import com.thelightphone.sdk.ui.LightIcons
import com.thelightphone.sdk.ui.LightTheme
import com.thelightphone.sdk.ui.LightThemeController
import com.thelightphone.sdk.ui.LightTopBar
import com.thelightphone.sdk.ui.LightTopBarCenter
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FavoriteDetailScreenViewModel(
    private val product: ScannedProduct,
    private val favoritesStore: FavoritesStore,
) : LightViewModel<Unit>() {
    val score = calculateHealthScore(product)

    val isFavorite = favoritesStore.isFavorite(product.barcode)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun toggleFavorite() {
        viewModelScope.launch { favoritesStore.toggleFavorite(product) }
    }
}

class FavoriteDetailScreen(
    sealedActivity: SealedLightActivity,
    private val product: ScannedProduct,
) : LightScreen<Unit, FavoriteDetailScreenViewModel>(sealedActivity) {

    override val viewModelClass: Class<FavoriteDetailScreenViewModel>
        get() = FavoriteDetailScreenViewModel::class.java

    override fun createViewModel(): FavoriteDetailScreenViewModel =
        FavoriteDetailScreenViewModel(product, FavoritesStore(lightContext.dataStore))

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()
        val isFavorite by viewModel.isFavorite.collectAsState()

        LightTheme(colors = themeColors) {
            Column(modifier = Modifier.fillMaxSize()) {
                LightTopBar(
                    leftButton = LightBarButton.LightIcon(
                        icon = LightIcons.BACK,
                        onClick = { goBack() },
                    ),
                    center = LightTopBarCenter.Text("Favourite"),
                    modifier = Modifier.padding(bottom = 16.dp),
                )

                ProductDetailContent(
                    product = product,
                    score = viewModel.score,
                    isFavorite = isFavorite,
                    onToggleFavorite = { viewModel.toggleFavorite() },
                )
            }
        }
    }
}