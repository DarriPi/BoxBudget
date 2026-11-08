package com.example.boxbudget.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val BoxBudgetColors = lightColorScheme(
    primary = Teal,
    onPrimary = Card,
    secondary = Teal,
    onSecondary = Card,
    background = Page,
    onBackground = Ink,
    surface = Card,
    onSurface = Ink,
    surfaceVariant = Track,
    onSurfaceVariant = Muted,
    outline = Hairline,
    error = Red,
)

/** The design is a single light theme, so system dark mode and dynamic colour are not used. */
@Composable
fun BoxBudgetTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = BoxBudgetColors, typography = Typography, content = content)
}
