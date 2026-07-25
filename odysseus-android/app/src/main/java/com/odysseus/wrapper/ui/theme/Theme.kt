package com.odysseus.wrapper.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Dark color scheme — matches Odysseus webapp default dark theme
private val OdDarkColors = darkColorScheme(
    primary          = OdRed,           // --red: #e06c75 — main accent
    onPrimary        = OdBg,
    primaryContainer = Color(0x33E06C75),
    onPrimaryContainer = OdRed,
    secondary        = OdFg,            // --fg: #9cdef2 — teal text
    onSecondary      = OdBg,
    secondaryContainer = Color(0x229CDEF2),
    onSecondaryContainer = OdFg,
    tertiary         = OdGreen,
    onTertiary       = OdBg,
    error            = OdRed,
    onError          = OdBg,
    errorContainer   = Color(0x44E06C75),
    onErrorContainer = OdFg,
    background       = OdBg,            // --bg: #282c34
    onBackground     = OdFg,            // --fg: #9cdef2
    surface          = OdPanel,         // --panel: #111
    onSurface        = OdFg,
    surfaceVariant   = OdSurface,
    onSurfaceVariant = OdFg,
    outline          = OdBorder,        // --border: #355a66
    outlineVariant   = Color(0xFF1E3A42)
)

// Light color scheme
private val OdLightColors = lightColorScheme(
    primary          = OdRed,
    onPrimary        = OdLightBg,
    secondary        = Color(0xFF2B7A8E),
    onSecondary      = OdLightBg,
    error            = OdRed,
    background       = OdLightBg,
    onBackground     = OdLightFg,
    surface          = OdLightPanel,
    onSurface        = OdLightFg,
    surfaceVariant   = OdLightBg,
    onSurfaceVariant = OdLightFg,
    outline          = OdLightBorder
)

@Composable
fun OdysseusTheme(darkTheme: Boolean = true, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) OdDarkColors else OdLightColors,
        typography  = Typography(),
        content     = content
    )
}
