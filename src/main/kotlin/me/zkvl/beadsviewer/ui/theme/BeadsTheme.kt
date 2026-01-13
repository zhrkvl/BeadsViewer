package me.zkvl.beadsviewer.ui.theme

import androidx.compose.runtime.*
import com.intellij.ide.ui.LafManager
import com.intellij.ide.ui.LafManagerListener
import com.intellij.openapi.application.ApplicationManager

/**
 * CompositionLocal providing access to the current theme color scheme.
 */
val LocalBeadsColorScheme = compositionLocalOf { DarkColorScheme }

/**
 * Root theme composable for the Beads Viewer.
 * Provides theme colors to all child composables via CompositionLocal.
 *
 * @param themeMode The theme mode to use (LIGHT, DARK, or SYSTEM)
 * @param content The content to theme
 */
@Composable
fun BeadsTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeMode) {
        ThemeMode.LIGHT -> LightColorScheme
        ThemeMode.DARK -> DarkColorScheme
        ThemeMode.SYSTEM -> {
            // Detect IntelliJ IDE theme
            val isDark = rememberIdeTheme()
            if (isDark) DarkColorScheme else LightColorScheme
        }
    }

    CompositionLocalProvider(
        LocalBeadsColorScheme provides colorScheme,
        content = content
    )
}

/**
 * Remembers and observes the IntelliJ IDE theme state.
 * Returns true if the IDE is using a dark theme, false otherwise.
 */
@Composable
private fun rememberIdeTheme(): Boolean {
    var isDark by remember {
        mutableStateOf(isIdeDarkTheme())
    }

    DisposableEffect(Unit) {
        val connection = ApplicationManager.getApplication().messageBus.connect()
        connection.subscribe(LafManagerListener.TOPIC, LafManagerListener {
            isDark = isIdeDarkTheme()
        })

        onDispose {
            connection.disconnect()
        }
    }

    return isDark
}

/**
 * Checks if the IntelliJ IDE is currently using a dark theme.
 */
private fun isIdeDarkTheme(): Boolean {
    val laf = LafManager.getInstance().currentUIThemeLookAndFeel
    return laf?.isDark ?: true
}

/**
 * Extension property to access the current theme colors from any composable.
 * Usage: val colors = BeadsTheme.colors
 */
object BeadsTheme {
    val colors: BeadsColorScheme
        @Composable
        @ReadOnlyComposable
        get() = LocalBeadsColorScheme.current
}
