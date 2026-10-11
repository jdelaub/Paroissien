package app.paroissien.core.reading.book

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.paroissien.core.designsystem.BookFonts
import app.paroissien.core.designsystem.PaperColors
import app.paroissien.core.model.text.LiangHyphenator

/** Styles du livre pour une police, un papier et un corps donnés ([scale] = 1 pour 18 sp). */
@Immutable
class BookTypography(val fonts: BookFonts, val paper: PaperColors, val scale: Float = 1f) {

    val body = TextStyle(
        fontFamily = fonts.regular, fontSize = (18 * scale).sp, lineHeight = (25 * scale).sp, color = paper.inkPrimary,
        textAlign = TextAlign.Justify, hyphens = Hyphens.Auto, lineBreak = LineBreak.Paragraph,
        fontFeatureSettings = "onum, liga, kern",
    )
    val small = body.copy(fontSize = (16 * scale).sp, lineHeight = (22 * scale).sp)
    val rubric = body.copy(
        fontFamily = fonts.italic, fontSize = (15 * scale).sp, lineHeight = (20 * scale).sp,
        color = paper.rubric, localeList = LocaleList("fr"),
    )
    val heading = TextStyle(
        fontFamily = fonts.medium, fontSize = (16 * scale).sp, lineHeight = (24 * scale).sp, color = paper.inkPrimary,
        textAlign = TextAlign.Center, letterSpacing = 0.06.em, fontFeatureSettings = "smcp, c2sc, onum",
        hyphens = Hyphens.None,
    )
    val minorHeading = heading.copy(fontSize = (14 * scale).sp, color = paper.inkSecondary)
    val title = TextStyle(
        fontFamily = fonts.medium, fontSize = (38 * scale).sp, lineHeight = (46 * scale).sp, color = paper.inkPrimary,
        textAlign = TextAlign.Center, fontFeatureSettings = "liga, dlig",
    )
    val subtitle = TextStyle(fontFamily = fonts.italic, fontSize = (17 * scale).sp, lineHeight = (23 * scale).sp, color = paper.inkSecondary, textAlign = TextAlign.Center)
    val ornament = TextStyle(fontFamily = fonts.regular, fontSize = (24 * scale).sp, color = paper.rubric, textAlign = TextAlign.Center)
    val runningHead = heading.copy(fontFamily = fonts.regular, fontSize = 12.sp, lineHeight = 16.sp, color = paper.inkTertiary)
    val folio = TextStyle(fontFamily = fonts.regular, fontSize = 13.sp, color = paper.inkTertiary, textAlign = TextAlign.Center, fontFeatureSettings = "onum")
    val dropCap = TextStyle(fontFamily = fonts.regular, color = paper.rubric, fontFeatureSettings = "lnum")

    fun paragraph(lang: BlockLang, role: TextRole): TextStyle {
        val base = when (role) {
            TextRole.BODY -> body
            TextRole.SMALL -> small
            TextRole.CENTERED -> body.copy(textAlign = TextAlign.Center, hyphens = Hyphens.None)
        }
        // La locale « la » active le « locl » latin d'EB Garamond (u dessiné en v) : coupé ; la césure latine
        // vient de nos motifs (ADR 007).
        return when (lang) {
            BlockLang.LA -> base.copy(localeList = LocaleList("la"), fontFeatureSettings = "onum, liga, kern, 'locl' 0")
            BlockLang.FR -> base.copy(localeList = LocaleList("fr"))
        }
    }

    fun versicle(lang: BlockLang) = paragraph(lang, TextRole.BODY).copy(textIndent = TextIndent(restLine = 1.1.em))

    /** _…_ en italique, ℣ ℟ ✠ ✝ en rouge ; césure liturgique insérée pour le latin (jamais stockée, ADR 004). */
    fun inline(text: String, lang: BlockLang): AnnotatedString {
        val source = if (lang == BlockLang.LA) LiangHyphenator.latinLiturgical.insertSoftHyphens(text) else text
        val italic = SpanStyle(fontFamily = fonts.italic)
        val red = SpanStyle(color = paper.rubric)
        return buildAnnotatedString {
            var inItalic = false
            for (c in source) {
                when {
                    c == '_' -> inItalic = !inItalic
                    c in LITURGICAL_MARKS -> withStyle(red) { append(c) }
                    inItalic -> withStyle(italic) { append(c) }
                    else -> append(c)
                }
            }
        }
    }

    fun withScale(newScale: Float) = BookTypography(fonts, paper, newScale)

    private companion object {
        val LITURGICAL_MARKS = setOf('℣', '℟', '✠', '✝')
    }
}
