package com.wildassoler.scorelight

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.thelightphone.sdk.LightQrCodeScanner
import com.thelightphone.sdk.LightScreen
import com.thelightphone.sdk.LightViewModel
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.ui.LightTheme
import com.thelightphone.sdk.ui.LightThemeController
import com.thelightphone.sdk.ui.LightThemeTokens

class ScanScreenViewModel : LightViewModel<String>()

/**
 * Camera scanner for product barcodes. Returns the normalized barcode to whoever opened it.
 */
class ScanScreen(sealedActivity: SealedLightActivity) :
    LightScreen<String, ScanScreenViewModel>(sealedActivity) {

    override val viewModelClass: Class<ScanScreenViewModel>
        get() = ScanScreenViewModel::class.java

    override fun createViewModel() = ScanScreenViewModel()

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()
        var pendingScan by remember { mutableStateOf<String?>(null) }

        LightTheme(colors = themeColors) {
            LightQrCodeScanner(
                title = "Scan Barcode",
                onScanned = { pendingScan = it },
                onBack = { goBack() },
                modifier = Modifier.background(LightThemeTokens.colors.background),
            )
        }

        // Defer navigation out of the scan callback, as the SDK recommends:
        // pop the scanner first, then let the caller push the next screen.
        LaunchedEffect(pendingScan) {
            val value = pendingScan ?: return@LaunchedEffect
            pendingScan = null
            goBack(normalizeBarcode(value))
        }
    }
}