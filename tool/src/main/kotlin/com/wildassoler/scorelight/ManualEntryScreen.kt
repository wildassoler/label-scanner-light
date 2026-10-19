package com.wildassoler.scorelight

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.thelightphone.sdk.ui.LightBarButton
import com.thelightphone.sdk.ui.LightBottomBar
import com.thelightphone.sdk.ui.LightIcons
import com.thelightphone.sdk.ui.LightText
import com.thelightphone.sdk.ui.LightTextVariant
import com.thelightphone.sdk.ui.LightTheme
import com.thelightphone.sdk.ui.LightThemeController
import com.thelightphone.sdk.ui.lightClickable

/**
 * Home screen: Title, subtitle, central "Type barcode..." link for manual entry,
 * and standard Light OS bottom bar with camera search (left) and favourites (right).
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
                modifier = Modifier.fillMaxSize(),
            ) {
                // Central content
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp),
                ) {
                    LightText(
                        text = "Score Light",
                        variant = LightTextVariant.Heading,
                        align = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                    LightText(
                        text = "Scan a product's barcode for its health score.",
                        variant = LightTextVariant.Copy,
                        align = TextAlign.Center,
                        lighten = true,
                        modifier = Modifier.padding(bottom = 48.dp),
                    )

                    // Manual fallback: open custom number pad
                    LightText(
                        text = "TYPE BARCODE",
                        variant = LightTextVariant.Copy,
                        lighten = true,
                        underline = true,
                        modifier = Modifier.lightClickable(
                            onClick = {
                                navigateTo(screenFactory = { activity -> BarcodeEntryScreen(activity) })
                            },
                        ),
                    )
                }

                // Light OS standard bottom action bar
                LightBottomBar(
                    items = listOf(
                        LightBarButton.LightIcon(
                            icon = LightIcons.SEARCH,
                            onClick = {
                                navigateTo(
                                    screenFactory = { activity -> ScanScreen(activity) },
                                    resultCallback = { barcode ->
                                        navigateTo(
                                            screenFactory = { activity ->
                                                ResultScreen(activity, barcode)
                                            },
                                        )
                                    },
                                )
                            },
                        ),
                        LightBarButton.LightIcon(
                            icon = LightIcons.STAR,
                            onClick = {
                                navigateTo(screenFactory = { activity -> FavoritesScreen(activity) })
                            },
                        ),
                    ),
                )
            }
        }
    }
}

class ManualEntryScreenViewModel : LightViewModel<Unit>()