package com.app.muzzutech.ui.compose

import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment

/**
 * Host a Jetpack Compose screen inside a Fragment's onCreateView.
 *
 * Usage in a Fragment:
 * ```kotlin
 * override fun onCreateView(...) = composeView(this) {
 *     MuzzuTheme { MyScreen() }
 * }
 * ```
 */
fun composeView(fragment: Fragment, content: @Composable () -> Unit): View {
    return ComposeView(fragment.requireContext()).apply {
        setContent { content() }
    }
}
