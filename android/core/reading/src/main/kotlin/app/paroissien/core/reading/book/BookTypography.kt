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
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.paroissien.core.designsystem.BookFonts
import app.paroissien.core.designsystem.PaperColors
import app.paroissien.core.model.text.Inline
import app.paroissien.core.model.text.LiangHyphenator
import app.paroissien.core.model.text.Mark
import app.paroissien.core.model.text.RichText

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
            BlockLang.LA -> base.copy(localeList = LocaleList("la"), fontFeatureSettings = LATIN_FEATURES)
            BlockLang.FR -> base.copy(localeList = LocaleList("fr"))
        }
    }

    fun versicle(lang: BlockLang) = paragraph(lang, TextRole.BODY).copy(textIndent = TextIndent(restLine = 1.1.em))

    /**
     * Inlines → texte affiché : marques (italique, gras, petites capitales, rouge, exposant, espacé), symboles
     * liturgiques en rouge, numéros de versets en petit rouge. Le latin reçoit la césure liturgique à l'affichage
     * (jamais stockée, ADR 004). Les numéros sont annotés [NON_CANONICAL] : ils ne comptent pas dans les ancres.
     */
    fun render(text: RichText, lang: BlockLang): AnnotatedString = buildAnnotatedString { append(text, lang) }

    private fun AnnotatedString.Builder.append(text: RichText, lang: BlockLang) {
        for (node in text) when (node) {
            is Inline.Text -> append(if (lang == BlockLang.LA) LiangHyphenator.latinLiturgical.insertSoftHyphens(node.value) else node.value)
            is Inline.Marked -> withStyle(node.marks.fold(SpanStyle()) { style, mark -> style.merge(spanFor(mark)) }) { append(node.children, lang) }
            is Inline.Lang -> {
                val inner = BlockLang.of(node.lang)
                withStyle(SpanStyle(localeList = LocaleList(inner.tag), fontFeatureSettings = if (inner == BlockLang.LA) LATIN_FEATURES else null)) {
                    append(node.children, inner)
                }
            }
            is Inline.Link -> append(node.children, lang)
            is Inline.Sym -> withStyle(SpanStyle(color = paper.rubric)) { append(node.symbol.char) }
            is Inline.Num -> {
                pushStringAnnotation(NON_CANONICAL, node.value)
                withStyle(SpanStyle(color = paper.rubric, fontSize = 0.72.em, fontFeatureSettings = "lnum")) { append(node.value) }
                append('\u2002')
                pop()
            }
            Inline.Break -> append('\n')
            is Inline.NoteRef -> Unit // les notes s'afficheront avec leur panneau (plus tard)
        }
    }

    private fun spanFor(mark: Mark): SpanStyle = when (mark) {
        Mark.ITALIC -> SpanStyle(fontFamily = fonts.italic)
        Mark.BOLD -> SpanStyle(fontFamily = fonts.medium)
        Mark.SMALL_CAPS -> SpanStyle(fontFeatureSettings = "smcp, c2sc, onum")
        Mark.RED -> SpanStyle(color = paper.rubric)
        Mark.SUP -> SpanStyle(baselineShift = BaselineShift.Superscript, fontSize = 0.7.em)
        Mark.SUB -> SpanStyle(baselineShift = BaselineShift.Subscript, fontSize = 0.7.em)
        Mark.SPACED -> SpanStyle(letterSpacing = 0.12.em)
    }

    fun withScale(newScale: Float) = BookTypography(fonts, paper, newScale)

    companion object {
        /** Annotation des caractères affichés hors du texte canonique (numéros de versets). */
        const val NON_CANONICAL = "nc"
        private const val LATIN_FEATURES = "onum, liga, kern, 'locl' 0"
    }
}
