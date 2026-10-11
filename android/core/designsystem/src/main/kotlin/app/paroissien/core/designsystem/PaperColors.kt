package app.paroissien.core.designsystem

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import app.paroissien.core.liturgy.LiturgicalColor

/** Les quatre thèmes de lecture (rapport 08, §2.4). Tous les textes dépassent 4,5:1 de contraste. */
enum class PaperTheme { CLAIR, SEPIA, SOMBRE, NUIT }

@Immutable
data class PaperColors(
    val background: Color,
    val surface: Color,
    val inkPrimary: Color,
    val inkSecondary: Color,
    val inkTertiary: Color,
    /** Rubriques, ℣ ℟, numéros de versets : toujours doublé d'un second indice (italique) pour les daltoniens. */
    val rubric: Color,
    val hairline: Color,
    val uiPrimary: Color,
    val error: Color,
    val highlight: Color,
    val isDark: Boolean,
)

fun paperColors(theme: PaperTheme): PaperColors = when (theme) {
    PaperTheme.CLAIR -> PaperColors(
        background = Color(0xFFFBF8F1), surface = Color(0xFFFFFDF8),
        inkPrimary = Color(0xFF1F1B16), inkSecondary = Color(0xFF5E554A), inkTertiary = Color(0xFF7A7064),
        rubric = Color(0xFFA31F1A), hairline = Color(0xFFDDD4C4), uiPrimary = Color(0xFF5A4630),
        error = Color(0xFF8C1D18), highlight = Color(0xFFF3E3A6), isDark = false,
    )
    PaperTheme.SEPIA -> PaperColors(
        background = Color(0xFFF2E8D3), surface = Color(0xFFF8F0DE),
        inkPrimary = Color(0xFF2E2418), inkSecondary = Color(0xFF5A4B38), inkTertiary = Color(0xFF6E5E48),
        rubric = Color(0xFF962216), hairline = Color(0xFFD4C4A3), uiPrimary = Color(0xFF57412A),
        error = Color(0xFF8C1D18), highlight = Color(0xFFF3E3A6), isDark = false,
    )
    PaperTheme.SOMBRE -> PaperColors(
        background = Color(0xFF17140F), surface = Color(0xFF211D17),
        inkPrimary = Color(0xFFE6DED0), inkSecondary = Color(0xFFB3A897), inkTertiary = Color(0xFF9A8F7F),
        rubric = Color(0xFFE88A78), hairline = Color(0xFF3A332A), uiPrimary = Color(0xFFD9C3A0),
        error = Color(0xFFFFB4A6), highlight = Color(0xFF4A3F1C), isDark = true,
    )
    PaperTheme.NUIT -> PaperColors(
        background = Color(0xFF000000), surface = Color(0xFF0D0A07),
        inkPrimary = Color(0xFF9A7F5C), inkSecondary = Color(0xFF8A7252), inkTertiary = Color(0xFF8A7252),
        rubric = Color(0xFFC2604F), hairline = Color(0xFF2A2118), uiPrimary = Color(0xFF9A7F5C),
        error = Color(0xFFC2604F), highlight = Color(0xFF2A2010), isDark = true,
    )
}

/** Couleur d'un ruban de tissu ou d'une pastille liturgique, et son accent de texte (rapport 08, §2.5). */
@Immutable
data class LiturgicalTokens(val fabric: Color, val textAccent: Color, val trim: Color? = null)

fun liturgicalTokens(color: LiturgicalColor, dark: Boolean): LiturgicalTokens = when (color) {
    LiturgicalColor.GREEN -> if (dark) LiturgicalTokens(Color(0xFF4E9A5E), Color(0xFF7FC08B)) else LiturgicalTokens(Color(0xFF2E6B3A), Color(0xFF2E6B3A))
    LiturgicalColor.RED -> if (dark) LiturgicalTokens(Color(0xFFC8423F), Color(0xFFF07D74)) else LiturgicalTokens(Color(0xFFA3171C), Color(0xFFA3171C))
    LiturgicalColor.VIOLET -> if (dark) LiturgicalTokens(Color(0xFF8E5BA6), Color(0xFFC49AD6)) else LiturgicalTokens(Color(0xFF5B2A6E), Color(0xFF5B2A6E))
    LiturgicalColor.ROSE -> if (dark) LiturgicalTokens(Color(0xFFD886A3), Color(0xFFF0A6BF)) else LiturgicalTokens(Color(0xFFC9708F), Color(0xFFA2405F))
    // Blanc et noir se perdent sur leur fond : un liseré les détache.
    LiturgicalColor.WHITE -> if (dark) LiturgicalTokens(Color(0xFFEDE6D3), Color(0xFFDDBB66)) else LiturgicalTokens(Color(0xFFF4EEDC), Color(0xFF7A5E14), trim = Color(0xFFB8912F))
    LiturgicalColor.BLACK -> if (dark) LiturgicalTokens(Color(0xFF0A0A0A), Color(0xFFBDB6AA), trim = Color(0xFF5A5650)) else LiturgicalTokens(Color(0xFF1C1C1C), Color(0xFF2B2B2B))
    LiturgicalColor.GOLD -> if (dark) LiturgicalTokens(Color(0xFFDDBB66), Color(0xFFDDBB66)) else LiturgicalTokens(Color(0xFFB8912F), Color(0xFF7A5E14))
}
