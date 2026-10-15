package com.ashrin.ashtube.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val AshTubeColors = darkColorScheme()

@Composable
fun AshTubeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AshTubeColors,
        content = content,
    )
}
