package app.paroissien.spike

import android.graphics.Paint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.paroissien.core.designsystem.BookFonts
import app.paroissien.core.designsystem.LocalPaperColors
import app.paroissien.core.designsystem.PaperColors
import app.paroissien.core.designsystem.paperBackground
import app.paroissien.core.model.text.LiangHyphenator

private val LITURGICAL_MARKS = setOf('℣', '℟', '✠', '✝')
private const val CONTROL_GLYPHS = "✠ ✝ ⁂ ℣ ℟ ❦ ❧ ☙ æ œ ǽ ǣ ſ á é í ó ú ý ë ï « » ’ – — …"

/** Une page du livre d'essai, dessinée sur le papier. Le contenu qui dépasse est coupé (paginateur : phase 1b). */
@Composable
fun BookPage(page: SpikePage, number: Int, fonts: BookFonts, modifier: Modifier = Modifier) {
    val paper = LocalPaperColors.current
    val styles = remember(fonts, paper) { BookStyles(fonts, paper) }
    Box(modifier.fillMaxSize().paperBackground()) {
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.displayCutout)
                .padding(horizontal = 28.dp, vertical = 22.dp),
        ) {
            Text(page.runningHead, style = styles.runningHead, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(14.dp))
            Column(
                Modifier.weight(1f).fillMaxWidth().clipToBounds(),
                verticalArrangement = Arrangement.spacedBy(10.dp, if (page.runningHead.isEmpty()) Alignment.CenterVertically else Alignment.Top),
            ) {
                page.blocks.forEach { BlockView(it, styles, fonts) }
            }
            Text(if (number > 0) number.toString() else "", style = styles.folio, modifier = Modifier.fillMaxWidth())
        }
    }
}

private class BookStyles(fonts: BookFonts, val paper: PaperColors) {
    val body = TextStyle(
        fontFamily = fonts.regular, fontSize = 18.sp, lineHeight = 25.sp, color = paper.inkPrimary,
        textAlign = TextAlign.Justify, hyphens = Hyphens.Auto, lineBreak = LineBreak.Paragraph,
        fontFeatureSettings = "onum, liga, kern",
    )
    val italic = SpanStyle(fontFamily = fonts.italic)
    val rubric = body.copy(fontFamily = fonts.italic, fontSize = 15.sp, lineHeight = 20.sp, color = paper.rubric, localeList = LocaleList("fr"))
    val heading = TextStyle(
        fontFamily = fonts.medium, fontSize = 16.sp, color = paper.inkPrimary, textAlign = TextAlign.Center,
        letterSpacing = 0.06.em, fontFeatureSettings = "smcp, c2sc, onum",
    )
    val runningHead = heading.copy(fontFamily = fonts.regular, fontSize = 12.sp, color = paper.inkTertiary)
    val folio = TextStyle(fontFamily = fonts.regular, fontSize = 13.sp, color = paper.inkTertiary, textAlign = TextAlign.Center, fontFeatureSettings = "onum")
    val title = TextStyle(fontFamily = fonts.medium, fontSize = 40.sp, color = paper.inkPrimary, textAlign = TextAlign.Center, fontFeatureSettings = "liga, dlig")
    val subtitle = TextStyle(fontFamily = fonts.italic, fontSize = 17.sp, color = paper.inkSecondary, textAlign = TextAlign.Center)
    val ornament = TextStyle(fontSize = 24.sp, color = paper.rubric, textAlign = TextAlign.Center)

    // La locale « la » active le « locl » latin d'EB Garamond, qui dessine u en v (inscriptions antiques) :
    // on le coupe, la césure latine venant de nos propres motifs.
    fun bodyFor(lang: Lang) = when (lang) {
        Lang.LA -> body.copy(localeList = LocaleList(lang.tag), fontFeatureSettings = "onum, liga, kern, 'locl' 0")
        Lang.FR -> body.copy(localeList = LocaleList(lang.tag))
    }
}

@Composable
private fun BlockView(block: Block, s: BookStyles, fonts: BookFonts) {
    when (block) {
        is Block.Title -> Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(block.text, style = s.title, modifier = Modifier.fillMaxWidth())
            block.subtitle?.let { Text(it, style = s.subtitle, modifier = Modifier.fillMaxWidth()) }
        }
        is Block.Heading -> Text(block.text, style = s.heading, modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
        is Block.Para -> Text(inline(block.text, block.lang, s, block.dropCap), style = s.bodyFor(block.lang))
        is Block.Rubric -> Text(block.text, style = s.rubric)
        is Block.Versicle -> Text(
            buildAnnotatedString {
                withStyle(SpanStyle(color = s.paper.rubric)) { append(block.mark) }
                append(' ')
                append(inline(block.text, block.lang, s))
            },
            style = s.bodyFor(block.lang).copy(textIndent = TextIndent(restLine = 1.1.em)),
        )
        Block.Ornament -> Text("❦", style = s.ornament.copy(fontFamily = fonts.regular), modifier = Modifier.fillMaxWidth())
        Block.GlyphCheck -> GlyphCheck(s, fonts)
        Block.HyphenationCheck -> Text(
            HYPHENATION_SAMPLE.joinToString("   ") { word ->
                LiangHyphenator.latinLiturgical.insertSoftHyphens(word).replace(LiangHyphenator.SOFT_HYPHEN, '‐')
            },
            style = s.body.copy(fontSize = 15.sp, lineHeight = 21.sp, textAlign = TextAlign.Start, hyphens = Hyphens.None),
        )
        is Block.Credits -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            block.lines.forEach { Text(it, style = s.body.copy(fontSize = 14.sp, lineHeight = 19.sp, textAlign = TextAlign.Center, hyphens = Hyphens.None)) }
        }
    }
}

/** Chaque glyphe de contrôle, et une croix rouge s'il n'est rendu par aucune police de la chaîne. */
@Composable
private fun GlyphCheck(s: BookStyles, fonts: BookFonts) {
    val paint = remember(fonts) { Paint().apply { typeface = fonts.regularTypeface } }
    val text = buildAnnotatedString {
        CONTROL_GLYPHS.split(' ').forEach { glyph ->
            append(glyph)
            if (!paint.hasGlyph(glyph)) withStyle(SpanStyle(color = s.paper.error)) { append("✗") }
            append("  ")
        }
        append("\nEspace fine : « Fiat » ; ainsi soit-il !")
    }
    Text(text, style = s.body.copy(fontSize = 22.sp, lineHeight = 32.sp, textAlign = TextAlign.Center, hyphens = Hyphens.None))
}

/** _…_ en italique, ℣ ℟ ✠ en rouge, lettrine rouge facultative ; césure liturgique pour le latin. */
private fun inline(text: String, lang: Lang, s: BookStyles, dropCap: Boolean = false): AnnotatedString {
    val source = if (lang == Lang.LA) LiangHyphenator.latinLiturgical.insertSoftHyphens(text) else text
    return buildAnnotatedString {
        var italic = false
        var first = dropCap
        for (c in source) {
            when {
                c == '_' -> italic = !italic
                first && c.isLetter() -> {
                    withStyle(SpanStyle(color = s.paper.rubric, fontSize = 1.25.em)) { append(c) }
                    first = false
                }
                c in LITURGICAL_MARKS -> withStyle(SpanStyle(color = s.paper.rubric)) { append(c) }
                italic -> withStyle(s.italic) { append(c) }
                else -> append(c)
            }
        }
    }
}

private val HYPHENATION_SAMPLE = listOf(
    "Dóminus", "glória", "excélsis", "Spíritus", "Kýrie", "eléison", "sǽcula", "Benedícimus", "Glorificámus",
    "deprecatiónem", "misericórdiam", "Introíbo", "lætíficat", "juventútem", "discérne", "iníquo", "fortitúdo",
    "súscipe", "unigénite", "Altíssimus", "voluntátis", "homínibus", "consubstantiálem", "resurrectiónem",
)

