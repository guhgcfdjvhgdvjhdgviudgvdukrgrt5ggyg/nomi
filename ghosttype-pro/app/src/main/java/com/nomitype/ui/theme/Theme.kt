package com.nomitype.ui.theme

import android.content.SharedPreferences
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.nomitype.utils.SettingsStore

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFF8C00),
    onPrimary = Color.Black,
    background = Color(0xFF0D0D0D),
    onBackground = Color.White,
    surface = Color(0xFF1F1F1F),
    onSurface = Color.White,
    secondary = Color(0xFFFFB347),
    surfaceContainerLow = Color(0xFF161616),
    surfaceContainer = Color(0xFF1A1A1A),
    surfaceContainerHigh = Color(0xFF222222)
)

private val LightColors = lightColorScheme(
    primary = Color(0xFFFF8C00),
    onPrimary = Color.White,
    background = Color(0xFFF5F5F5),
    onBackground = Color(0xFF1A1A1A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1A1A),
    secondary = Color(0xFFCC6600),
    surfaceContainerLow = Color(0xFFEFEFEF),
    surfaceContainer = Color(0xFFE8E8E8),
    surfaceContainerHigh = Color(0xFFDDDDDD)
)

@Composable
fun GhostTypeTheme(content: @Composable () -> Unit) {
    val ctx = LocalContext.current
    val prefs = SettingsStore.prefs(ctx)
    var isLight by remember { mutableStateOf(prefs.getString(SettingsStore.KEY_UI_THEME, "dark") == "light") }

    DisposableEffect(prefs) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == SettingsStore.KEY_UI_THEME) {
                isLight = prefs.getString(SettingsStore.KEY_UI_THEME, "dark") == "light"
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    val colors = if (isLight) LightColors else DarkColors
    MaterialTheme(colorScheme = colors, content = content)
}
