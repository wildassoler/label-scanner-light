package com.wildassoler.scorelight

import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.thelightphone.sdk.InitialScreen
import com.thelightphone.sdk.LightScreen
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.rememberKeyboardOptions
import com.thelightphone.sdk.ui.LightTextInputEditor
import com.thelightphone.sdk.ui.LightThemeController
import com.thelightphone.sdk.ui.LightTheme

@InitialScreen
class ManualEntryScreen(sealedActivity: SealedLightActivity) :
    LightScreen<Unit, ManualEntryScreenViewModel>(sealedActivity) {

    override val viewModelClass: Class<ManualEntryScreenViewModel>
        get() = ManualEntryScreenViewModel::class.java

    override fun createViewModel(): ManualEntryScreenViewModel = ManualEntryScreenViewModel()

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()
        val textFieldState = rememberTextFieldState("")
        val keyboardOptionsFlow = rememberKeyboardOptions()

        LightTheme(colors = themeColors) {
            LightTextInputEditor(
                title = "Scan Health",
                state = textFieldState,
                keyboardOptionsFlow = keyboardOptionsFlow,
                singleLine = true,
                showBackButton = false,
                submitLabel = "LOOK UP",
                onSubmit = { barcode ->
                    val trimmed = barcode.toString().trim()
                    if (trimmed.isNotEmpty()) {
                        navigateTo(
                            screenFactory = { activity -> ResultScreen(activity, trimmed) },
                        )
                    }
                },
                onBack = { /* no-op: this is the initial screen, nothing to go back to */ },
                modifier = Modifier,
            )
        }
    }
}

class ManualEntryScreenViewModel : com.thelightphone.sdk.LightViewModel<Unit>()