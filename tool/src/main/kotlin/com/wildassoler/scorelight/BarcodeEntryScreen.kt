package com.wildassoler.scorelight

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.thelightphone.sdk.LightScreen
import com.thelightphone.sdk.LightViewModel
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.ui.LightBarButton
import com.thelightphone.sdk.ui.LightIcon
import com.thelightphone.sdk.ui.LightIcons
import com.thelightphone.sdk.ui.LightText
import com.thelightphone.sdk.ui.LightTextVariant
import com.thelightphone.sdk.ui.LightTheme
import com.thelightphone.sdk.ui.LightThemeController
import com.thelightphone.sdk.ui.LightThemeTokens
import com.thelightphone.sdk.ui.LightTopBar
import com.thelightphone.sdk.ui.LightTopBarCenter
import com.thelightphone.sdk.ui.lightClickable

private const val MAX_BARCODE_LENGTH = 13

private const val DISABLED_ICON_ALPHA = 0.35f

class BarcodeEntryScreen(sealedActivity: SealedLightActivity) :
    LightScreen<Unit, BarcodeEntryScreenViewModel>(sealedActivity) {

    override val viewModelClass: Class<BarcodeEntryScreenViewModel>
        get() = BarcodeEntryScreenViewModel::class.java

    override fun createViewModel(): BarcodeEntryScreenViewModel = BarcodeEntryScreenViewModel()

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()
        var barcodeText by remember { mutableStateOf("") }

        LightTheme(colors = themeColors) {
            Column(modifier = Modifier.fillMaxSize()) {
                LightTopBar(
                    leftButton = LightBarButton.LightIcon(
                        icon = LightIcons.BACK,
                        onClick = { goBack() },
                    ),
                    center = LightTopBarCenter.Text("Enter Barcode"),
                )

                BarcodeDisplay(barcodeText, modifier = Modifier.padding(top = 16.dp, bottom = 16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(LightThemeTokens.colors.contentSecondary),
                )

                NumberPad(
                    onDigit = { digit ->
                        if (barcodeText.length < MAX_BARCODE_LENGTH) barcodeText += digit
                    },
                    onBackspace = { barcodeText = barcodeText.dropLast(1) },
                    onSubmit = { submitIfNotBlank(barcodeText) { code -> navigateToResult(code) } },
                    isSubmitEnabled = barcodeText.isNotEmpty(),
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                )
            }
        }
    }

    private fun navigateToResult(barcode: String) {
        navigateTo(screenFactory = { activity -> ResultScreen(activity, normalizeBarcode(barcode)) })
    }

    private inline fun submitIfNotBlank(value: String, onValid: (String) -> Unit) {
        val trimmed = value.trim()
        if (trimmed.isNotEmpty()) onValid(trimmed)
    }
}

class BarcodeEntryScreenViewModel : LightViewModel<Unit>()

@Composable
private fun BarcodeDisplay(barcodeText: String, modifier: Modifier = Modifier) {
    val display = if (barcodeText.isEmpty()) "—" else formatBarcodeGrouping(barcodeText)

    LightText(
        text = display,
        variant = LightTextVariant.Heading,
        align = TextAlign.Center,
        modifier = modifier.fillMaxWidth(),
    )
}

private fun formatBarcodeGrouping(digits: String): String = buildString {
    digits.forEachIndexed { index, char ->
        if (index == 1 || index == 7) append(' ')
        append(char)
    }
}

@Composable
private fun NumberPad(
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit,
    onSubmit: () -> Unit,
    isSubmitEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        val digitRows = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
        )

        digitRows.forEach { row ->
            Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
                row.forEach { digit ->
                    PadCell(modifier = Modifier.weight(1f), onClick = { onDigit(digit) }) {
                        LightText(text = digit, variant = LightTextVariant.Heading)
                    }
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
            PadCell(modifier = Modifier.weight(1f), onClick = onBackspace) {
                LightIcon(icon = LightIcons.DELETE)
            }
            PadCell(modifier = Modifier.weight(1f), onClick = { onDigit("0") }) {
                LightText(text = "0", variant = LightTextVariant.Heading)
            }
            PadCell(
                modifier = Modifier.weight(1f),
                onClick = onSubmit,
            ) {
                LightIcon(
                    icon = LightIcons.SEARCH,
                    modifier = Modifier.alpha(if (isSubmitEnabled) 1f else DISABLED_ICON_ALPHA),
                )
            }
        }
    }
}

@Composable
private fun PadCell(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .lightClickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}