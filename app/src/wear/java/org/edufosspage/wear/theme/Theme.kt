package org.edufosspage.wear.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.wear.compose.material.Colors
import androidx.wear.compose.material.MaterialTheme
import org.edufosspage.data.AppTheme
import org.edufosspage.data.ThemeStore

/**
 * The watch is dark-first, but it follows the same theme preference as the phone so the two clients
 * stay in step. The hues for the dark palette are lifted from the phone app's BlueDarkScheme.
 */
private val WearDarkColors = Colors(
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

/** Dark surfaces flattened to true black, for OLED panels. */
private val WearAmoledColors = Colors(
    primary          = Color(0xFFA4C8FF),
    primaryVariant   = Color(0xFF00347A),
    secondary        = Color(0xFFBBC7DB),
    secondaryVariant = Color(0xFF1A1A1A),
    background       = Color(0xFF000000),
    surface          = Color(0xFF000000),
    error            = Color(0xFFFFB4AB),
    onPrimary        = Color(0xFF003063),
    onSecondary      = Color(0xFF253140),
    onBackground     = Color(0xFFE2E2E9),
    onSurface        = Color(0xFFE2E2E9),
    onSurfaceVariant = Color(0xFFC3C7CF),
    onError          = Color(0xFF690005),
)

/** Light palette, matching the phone app's BlueLightScheme so the watch does not look alien. */
private val WearLightColors = Colors(
    primary          = Color(0xFF1565C0),
    primaryVariant   = Color(0xFF00347A),
    secondary        = Color(0xFF535F70),
    secondaryVariant = Color(0xFFD7E3F7),
    background       = Color(0xFFF8F9FF),
    surface          = Color(0xFFECEFF4),
    error            = Color(0xFFB3261E),
    onPrimary        = Color.White,
    onSecondary      = Color.White,
    onBackground     = Color(0xFF191C20),
    onSurface        = Color(0xFF191C20),
    onSurfaceVariant = Color(0xFF43474E),
    onError          = Color.White,
)

@Composable
fun EduWearTheme(content: @Composable () -> Unit) {
    val ctx = LocalContext.current
    val stored by ThemeStore.themeFlow(ctx).collectAsState(initial = AppTheme.SYSTEM)
    val systemDark = isSystemInDarkTheme()

    val colors = when (stored) {
        AppTheme.LIGHT  -> WearLightColors
        AppTheme.DARK   -> WearDarkColors
        AppTheme.AMOLED -> WearAmoledColors
        AppTheme.SYSTEM -> if (systemDark) WearDarkColors else WearLightColors
    }

    MaterialTheme(colors = colors) {
        // Wear's Scaffold does not paint a backdrop of its own, so anything it does not cover shows
        // the activity's window background. Filling the screen with the palette background keeps
        // every screen independent of the window theme.
        Box(Modifier.fillMaxSize().background(MaterialTheme.colors.background)) {
            content()
        }
    }
}
