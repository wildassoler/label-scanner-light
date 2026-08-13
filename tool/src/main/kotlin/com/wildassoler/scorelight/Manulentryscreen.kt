package com.wildassoler.scorelight

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
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
import com.thelightphone.sdk.ui.LightThemeTokens
import com.thelightphone.sdk.ui.lightClickable

@InitialScreen
class ManualEntryScreen(sealedActivity: SealedLightActivity) :
    LightScreen<Unit, ManualEntryScreenViewModel>(sealedActivity) {

    override val viewModelClass: Class<ManualEntryScreenViewModel>
        get() = ManualEntryScreenViewModel::class.java

    override fun createViewModel(): ManualEntryScreenViewModel = ManualEntryScreenViewModel()

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()
        var barcodeText by remember { mutableStateOf("") }

        LightTheme(colors = themeColors) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxSize().padding(32.dp),
            ) {
                LightText(
                    text = "Score Light",
                    variant = LightTextVariant.Heading,
                    align = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                LightText(
                    text = "Type a product's barcode to see its health score.",
                    variant = LightTextVariant.Copy,
                    align = TextAlign.Center,
                    lighten = true,
                    modifier = Modifier.padding(bottom = 32.dp),
                )

                val inputTextStyle = LightThemeTokens.typography.heading
                    .copy(color = LightThemeTokens.colors.content)

                val interactionSource = remember { MutableInteractionSource() }
                val isFocused by interactionSource.collectIsFocusedAsState()

                Box(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)) {
                    if (barcodeText.isEmpty() && !isFocused) {
                        LightText(
                            text = "Enter barcode",
                            variant = LightTextVariant.Copy,
                            lighten = true,
                            align = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    BasicTextField(
                        value = barcodeText,
                        onValueChange = { newValue -> barcodeText = newValue.filter { it.isDigit() } },
                        textStyle = inputTextStyle.copy(textAlign = TextAlign.Center),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { submitIfNotBlank(barcodeText) { code -> navigateToResult(code) } },
                        ),
                        singleLine = true,
                        visualTransformation = Ean13GroupingTransformation,
                        interactionSource = interactionSource,
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(LightThemeTokens.colors.content),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(LightThemeTokens.colors.contentSecondary)
                        .padding(bottom = 32.dp),
                )

                LightIcon(
                    icon = LightIcons.SEARCH,
                    modifier = Modifier
                        .lightClickable(
                            onClick = { submitIfNotBlank(barcodeText) { code -> navigateToResult(code) } },
                        )
                        .padding(top = 32.dp, bottom = 48.dp),
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

    private fun navigateToResult(barcode: String) {
        navigateTo(screenFactory = { activity -> ResultScreen(activity, barcode) })
    }

    private inline fun submitIfNotBlank(value: String, onValid: (String) -> Unit) {
        val trimmed = value.trim()
        if (trimmed.isNotEmpty()) onValid(trimmed)
    }
}

class ManualEntryScreenViewModel : LightViewModel<Unit>()

private object Ean13GroupingTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val digits = text.text
        val formatted = buildString {
            digits.forEachIndexed { index, char ->
                if (index == 1 || index == 7) append(' ')
                append(char)
            }
        }

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int = offset + when {
                offset > 7 -> 2
                offset > 1 -> 1
                else -> 0
            }

            override fun transformedToOriginal(offset: Int): Int = offset - when {
                offset > 8 -> 2
                offset > 1 -> 1
                else -> 0
            }
        }

        return TransformedText(AnnotatedString(formatted), offsetMapping)
    }
}