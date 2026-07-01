package com.alowork.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = AloworkPrimary,
    background = AloworkBackground,
    onBackground = AloworkOnBackground,
    onSurface = AloworkOnBackground,
    onSurfaceVariant = AloworkSurfaceVariant,
)

@Composable
fun AloworkTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = AloworkTypography,
        content = content,
    )
}
