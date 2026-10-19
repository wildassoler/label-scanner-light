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
import com.thelightphone.sdk.ui.LightBarcodeFormats
import com.thelightphone.sdk.ui.LightTheme
import com.thelightphone.sdk.ui.LightThemeController
import com.thelightphone.sdk.ui.LightThemeTokens

class ScanScreenViewModel : LightViewModel<String>()

/**
 * Camera scanner for product barcodes. Despite the "Qr" in the wrapper's
 * name, it decodes whatever formats we pass in; here, the retail symbologies
 * only. Returns the normalised barcode to whoever opened it.
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
                formats = listOf(
                    LightBarcodeFormats.EAN_13,
                    LightBarcodeFormats.EAN_8,
                    LightBarcodeFormats.UPC_A,
                    LightBarcodeFormats.UPC_E,
                ),
                // Required by the wrapper, but unused: we read the richer
                // onScannedBarcode callback below instead.
                onScanned = {},
                onScannedBarcode = { pendingScan = it.value },
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