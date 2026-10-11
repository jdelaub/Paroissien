package app.paroissien.core.designsystem

import android.content.Context
import android.graphics.Typeface
import android.graphics.fonts.Font
import android.graphics.fonts.FontStyle
import androidx.annotation.FontRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import android.graphics.fonts.FontFamily as PlatformFontFamily
import androidx.compose.ui.text.font.Typeface as ComposeTypeface

/** Police du texte courant (ADR 007) : EB Garamond pour les livres liturgiques, Literata en option de lecture. */
enum class BookFace { GARAMOND, LITERATA }

/**
 * Familles du livre. Chacune chaîne la police choisie, puis Junicode (✠ ✝ ⁂ ℣ ℟ U+202F), puis le serif système,
 * grâce à Typeface.CustomFallbackBuilder (API 29). Une famille Compose bâtie sur un seul Typeface ne sait pas
 * choisir l'italique : le rendu prend explicitement [italic] pour les rubriques et les citations.
 */
@Immutable
class BookFonts(
    val regular: FontFamily,
    val italic: FontFamily,
    val medium: FontFamily,
    /** Le Typeface Android du texte courant, pour vérifier la couverture des glyphes (Paint.hasGlyph). */
    val regularTypeface: Typeface,
)

@Composable
fun rememberBookFonts(face: BookFace): BookFonts {
    val context = LocalContext.current
    return remember(face, context) { loadBookFonts(context, face) }
}

fun loadBookFonts(context: Context, face: BookFace): BookFonts {
    val (upright, slanted, uprightAxes, mediumAxes, slantedAxes) = when (face) {
        BookFace.GARAMOND -> Files(R.font.eb_garamond, R.font.eb_garamond_italic, "'wght' 400", "'wght' 560", "'wght' 400")
        // Literata : axe de taille optique réglé pour le texte courant (environ 12 pt).
        BookFace.LITERATA -> Files(R.font.literata, R.font.literata_italic, "'opsz' 12, 'wght' 400", "'opsz' 18, 'wght' 560", "'opsz' 12, 'wght' 400")
    }
    val regular = typeface(context, upright, weight = 400, italic = false, axes = uprightAxes)
    return BookFonts(
        regular = FontFamily(ComposeTypeface(regular)),
        italic = FontFamily(ComposeTypeface(typeface(context, slanted, weight = 400, italic = true, axes = slantedAxes))),
        medium = FontFamily(ComposeTypeface(typeface(context, upright, weight = 560, italic = false, axes = mediumAxes))),
        regularTypeface = regular,
    )
}

private data class Files(@FontRes val upright: Int, @FontRes val slanted: Int, val uprightAxes: String, val mediumAxes: String, val slantedAxes: String)

private fun typeface(context: Context, @FontRes file: Int, weight: Int, italic: Boolean, axes: String): Typeface {
    val slant = if (italic) FontStyle.FONT_SLANT_ITALIC else FontStyle.FONT_SLANT_UPRIGHT
    val main = Font.Builder(context.resources, file)
        .setFontVariationSettings(axes)
        .setWeight(weight)
        .setSlant(slant)
        .build()
    val symbols = Font.Builder(context.resources, R.font.junicode_symbols).build()
    return Typeface.CustomFallbackBuilder(PlatformFontFamily.Builder(main).build())
        .addCustomFallback(PlatformFontFamily.Builder(symbols).build())
        .setSystemFallback("serif")
        .setStyle(FontStyle(weight, slant))
        .build()
}
