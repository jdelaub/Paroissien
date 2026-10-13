package app.paroissien.core.designsystem.frame

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.paroissien.core.designsystem.BookFace
import app.paroissien.core.designsystem.LocalPaperColors
import app.paroissien.core.designsystem.rememberBookFonts
import app.paroissien.core.liturgy.LiturgicalColor

/**
 * Échantillon : la p.103 « Pour la confession », en EB Garamond, dans un cadre reconstruit (celui de la confession
 * par défaut). Le texte défile à l'intérieur du cadre, qui reste fixe. Sert de témoin visuel aux cadres en attendant
 * leur branchement au lecteur.
 */
@Composable
fun FramedPageSample(
    modifier: Modifier = Modifier,
    spec: FrameSpec = ConfessionFrame,
    style: FrameStyle = ConfessionStyle,
    liturgicalColor: LiturgicalColor? = null,
    fontSize: TextUnit = 18.sp,
    outerOnRight: Boolean = true,
) {
    val paper = LocalPaperColors.current
    val fonts = rememberBookFonts(BookFace.GARAMOND)
    val body = TextStyle(
        fontFamily = fonts.regular,
        fontSize = fontSize,
        lineHeight = fontSize * 1.32f,
        color = paper.inkPrimary,
        textAlign = TextAlign.Justify,
        hyphens = Hyphens.Auto,
        lineBreak = LineBreak.Paragraph,
        localeList = LocaleList("fr"),
    )

    OrnamentalFrame(
        spec = spec,
        style = style,
        modifier = modifier,
        liturgicalColor = liturgicalColor,
        headerHeight = 22.dp,
        outerOnRight = outerOnRight,
        header = { RunningHead(spec.title + ".", if (outerOnRight) "103" else "104", fonts.regular) },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .fadingEdges()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(fontSize.value.dp * 0.55f),
        ) {
            Spacer(Modifier.height(6.dp))
            Text(
                "Toute ma vie je glorifierai un Dieu si bon, le meilleur de tous les maîtres, le plus doux et le plus " +
                    "aimable de tous les pères.",
                style = body,
            )
            SectionTitle("Réitérer la résolution de ne plus pécher.", fonts.regular, fontSize)
            Lead(
                "Mon",
                " Dieu, ce que vous venez de faire en ma faveur m’inspire une haine toute nouvelle pour le péché, et me " +
                    "fait prendre une nouvelle résolution de n’en plus commettre. Je vous conjure donc, ô mon Dieu ! " +
                    "d’augmenter en moi le désir que j’ai de changer de vie ; fortifiez par votre grâce la résolution " +
                    "où je suis de ne plus pécher, et rendez efficace le propos que je fais d’éviter toutes les occasions " +
                    "du péché, et surtout du péché qui vous déplaît en moi depuis si longtemps.",
                body,
            )
            Text(
                "Je vais commencer, ô mon Dieu ! et faire voir dès ce moment que j’ai eu le bonheur de me " +
                    "réconcilier avec vous. On s’apercevra dès aujourd’hui, par la…",
                style = body.copy(textIndent = TextIndent(firstLine = fontSize * 1.1f)),
            )
            Text("❧", Modifier.fillMaxWidth(), style = body.copy(textAlign = TextAlign.Center))
            SectionTitle("Acte de contrition.", fonts.regular, fontSize)
            Lead(
                "Mon",
                " Dieu, j’ai un très grand regret de vous avoir offensé, parce que vous êtes infiniment bon, infiniment " +
                    "aimable, et que le péché vous déplaît. Je prends la ferme résolution, avec le secours de votre " +
                    "sainte grâce, de ne plus vous offenser et de faire pénitence.",
                body,
            )
            Spacer(Modifier.height(18.dp))
        }
    }
}

/** Titre courant en capitales espacées et folio à droite, comme au-dessus du cadre dans le livre. */
@Composable
private fun RunningHead(title: String, folio: String, font: FontFamily) {
    val paper = LocalPaperColors.current
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            title.uppercase(),
            style = TextStyle(fontFamily = font, fontSize = 10.5.sp, letterSpacing = 0.3.em, color = paper.inkSecondary),
        )
        Text(
            folio,
            Modifier.align(Alignment.CenterEnd).padding(end = 4.dp),
            style = TextStyle(fontFamily = font, fontSize = 12.sp, color = paper.inkSecondary),
        )
    }
}

/** Titre de section en petites capitales, à l'encre comme dans le livre (le rouge reste aux rubriques). */
@Composable
private fun SectionTitle(text: String, font: FontFamily, fontSize: TextUnit) {
    Text(
        text,
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        style = TextStyle(
            fontFamily = font,
            fontSize = fontSize * 0.92f,
            lineHeight = fontSize * 1.2f,
            letterSpacing = 0.06.em,
            fontFeatureSettings = "c2sc, smcp",
            color = LocalPaperColors.current.inkPrimary,
            textAlign = TextAlign.Center,
        ),
    )
}

/**
 * Début de section : initiale en rubrique, puis la fin du premier mot en petites capitales (« Mᴏɴ Dieu »).
 * La lettrine sur plusieurs lignes relève du moteur de lecture ; ici l'initiale reste dans la ligne.
 */
@Composable
private fun Lead(firstWord: String, rest: String, body: TextStyle) {
    val rubric = LocalPaperColors.current.rubric
    Text(
        buildAnnotatedString {
            withStyle(SpanStyle(color = rubric, fontSize = 1.8.em)) { append(firstWord.take(1)) }
            withStyle(SpanStyle(fontFeatureSettings = "smcp", letterSpacing = 0.03.em)) { append(firstWord.drop(1)) }
            append(rest)
        },
        style = body,
    )
}
