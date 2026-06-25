package com.nomitype.ui

import androidx.compose.runtime.Composable

@Composable
fun GatedApp(content: @Composable () -> Unit) {
    content()
}
