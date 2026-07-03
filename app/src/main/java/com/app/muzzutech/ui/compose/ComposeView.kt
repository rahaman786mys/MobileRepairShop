package com.app.muzzutech.ui.compose

import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.lifecycle.compose.LocalLifecycleOwner

fun composeView(fragment: Fragment, content: @Composable () -> Unit): View {
    return ComposeView(fragment.requireContext()).apply {
        setContent {
            CompositionLocalProvider(
                LocalLifecycleOwner provides fragment.viewLifecycleOwner
            ) {
                content()
            }
        }
    }
}
