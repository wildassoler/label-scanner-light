package com.wildassoler.scorelight

import androidx.compose.foundation.layout.Arrangement
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
import com.thelightphone.sdk.InitialScreen
import com.thelightphone.sdk.LightScreen
import com.thelightphone.sdk.LightViewModel
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.ui.LightIcon
import com.thelightphone.sdk.ui.LightIcons
import com.thelightphone.sdk.ui.LightText
import com.thelightphone.sdk.ui.LightTextVariant
import com.thelightphone.sdk.ui.LightTheme
import com.thelightphone.sdk.ui.LightThemeController
import com.thelightphone.sdk.ui.lightClickable

/**
 * Home screen: just the title, a big search icon that opens the barcode
 * entry pad, and a link into favourites. Deliberately minimal — the number
 * pad needs its own screen with room to breathe (see BarcodeEntryScreen),
 * rather than sharing this one with the title/subtitle/favourites link.
 */
@InitialScreen
class ManualEntryScreen(sealedActivity: SealedLightActivity) :
    LightScreen<Unit, ManualEntryScreenViewModel>(sealedActivity) {

    override val viewModelClass: Class<ManualEntryScreenViewModel>
        get() = ManualEntryScreenViewModel::class.java

    override fun createViewModel(): ManualEntryScreenViewModel = ManualEntryScreenViewModel()

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()

        LightTheme(colors = themeColors) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize().padding(32.dp),
            ) {
                LightText(
                    text = "Score Light",
                    variant = LightTextVariant.Heading,
                    align = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                LightText(
                    text = "Look up a product's health score.",
                    variant = LightTextVariant.Copy,
                    align = TextAlign.Center,
                    lighten = true,
                    modifier = Modifier.padding(bottom = 48.dp),
                )

                LightIcon(
                    icon = LightIcons.SEARCH,
                    modifier = Modifier
                        .lightClickable(
                            onClick = {
                                navigateTo(screenFactory = { activity -> BarcodeEntryScreen(activity) })
                            },
                        )
                        .padding(bottom = 48.dp),
                )

                LightText(
                    text = "Favourites",
                    variant = LightTextVariant.Heading,
                    modifier = Modifier.lightClickable(
                        onClick = {
                            navigateTo(screenFactory = { activity -> FavoritesScreen(activity) })
                        },
                    ),
                )
            }
        }
    }
}

class ManualEntryScreenViewModel : LightViewModel<Unit>()