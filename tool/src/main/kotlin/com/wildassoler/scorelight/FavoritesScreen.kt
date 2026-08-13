package com.wildassoler.scorelight

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import com.thelightphone.sdk.LightScreen
import com.thelightphone.sdk.LightViewModel
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.ui.LightBarButton
import com.thelightphone.sdk.ui.LightIcons
import com.thelightphone.sdk.ui.LightText
import com.thelightphone.sdk.ui.LightTextVariant
import com.thelightphone.sdk.ui.LightTheme
import com.thelightphone.sdk.ui.LightThemeController
import com.thelightphone.sdk.ui.LightTopBar
import com.thelightphone.sdk.ui.LightTopBarCenter
import com.thelightphone.sdk.ui.lightClickable
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

class FavoritesScreenViewModel(favoritesStore: FavoritesStore) : LightViewModel<Unit>() {
    val favorites = favoritesStore.favorites
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}

class FavoritesScreen(sealedActivity: SealedLightActivity) :
    LightScreen<Unit, FavoritesScreenViewModel>(sealedActivity) {

    override val viewModelClass: Class<FavoritesScreenViewModel>
        get() = FavoritesScreenViewModel::class.java

    override fun createViewModel(): FavoritesScreenViewModel =
        FavoritesScreenViewModel(FavoritesStore(lightContext.dataStore))

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()
        val favorites by viewModel.favorites.collectAsState()

        LightTheme(colors = themeColors) {
            Column(modifier = Modifier.fillMaxSize()) {
                LightTopBar(
                    leftButton = LightBarButton.LightIcon(
                        icon = LightIcons.BACK,
                        onClick = { goBack() },
                    ),
                    center = LightTopBarCenter.Text("Favourites"),
                    modifier = Modifier.padding(bottom = 16.dp),
                )

                if (favorites.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(32.dp),
                        contentAlignment = Alignment.TopStart,
                    ) {
                        LightText(
                            text = "No favourites yet. Tap the star on a result to save it here.",
                            variant = LightTextVariant.Copy,
                            lighten = true,
                        )
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp)) {
                        items(favorites, key = { it.barcode }) { product ->
                            LightText(
                                text = product.name,
                                variant = LightTextVariant.Copy,
                                modifier = Modifier
                                    .lightClickable(
                                        onClick = {
                                            navigateTo(
                                                screenFactory = { activity ->
                                                    FavoriteDetailScreen(activity, product)
                                                },
                                            )
                                        },
                                    )
                                    .padding(vertical = 12.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}