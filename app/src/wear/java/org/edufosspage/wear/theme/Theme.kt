package org.edufosspage.wear.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material.Colors
import androidx.wear.compose.material.MaterialTheme

/**
 * Wear is dark-first, so the watch only ships a dark palette. The hues are lifted
 * from the phone app's BlueDarkScheme so both clients read as the same product.
 */
private val EduWearColors = Colors(
    primary          = Color(0xFFA4C8FF),
    primaryVariant   = Color(0xFF00479A),
    secondary        = Color(0xFFBBC7DB),
    secondaryVariant = Color(0xFF253140),
    background       = Color(0xFF000000),
    surface          = Color(0xFF1A1C20),
    error            = Color(0xFFFFB4AB),
    onPrimary        = Color(0xFF003063),
    onSecondary      = Color(0xFF253140),
    onBackground     = Color(0xFFE2E2E9),
    onSurface        = Color(0xFFE2E2E9),
    onSurfaceVariant = Color(0xFFC3C7CF),
    onError          = Color(0xFF690005),
)

@Composable
fun EduWearTheme(content: @Composable () -> Unit) {
    MaterialTheme(colors = EduWearColors, content = content)
}
