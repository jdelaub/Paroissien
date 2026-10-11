package app.paroissien.core.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

val LocalPaperColors = staticCompositionLocalOf { paperColors(PaperTheme.CLAIR) }

/** Material 3 sert d'ossature, habillé « papier et encre » : pas de couleur dynamique (rapport 08, D2). */
@Composable
fun ParoissienTheme(theme: PaperTheme = PaperTheme.CLAIR, content: @Composable () -> Unit) {
    val paper = paperColors(theme)
    val scheme = if (paper.isDark) {
        darkColorScheme(
            primary = paper.uiPrimary, background = paper.background, surface = paper.surface,
            onBackground = paper.inkPrimary, onSurface = paper.inkPrimary,
            onSurfaceVariant = paper.inkSecondary, outline = paper.hairline, error = paper.error,
        )
    } else {
        lightColorScheme(
            primary = paper.uiPrimary, background = paper.background, surface = paper.surface,
            onBackground = paper.inkPrimary, onSurface = paper.inkPrimary,
            onSurfaceVariant = paper.inkSecondary, outline = paper.hairline, error = paper.error,
        )
    }
    CompositionLocalProvider(LocalPaperColors provides paper) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
