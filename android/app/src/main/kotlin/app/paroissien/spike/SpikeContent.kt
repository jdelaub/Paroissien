package app.paroissien.spike

import android.content.Context
import app.paroissien.core.model.text.LiangHyphenator
import app.paroissien.core.reading.book.BlockLang
import app.paroissien.core.reading.book.ReadingBlock
import app.paroissien.core.reading.book.ReadingBlock.Heading
import app.paroissien.core.reading.book.ReadingBlock.Ornament
import app.paroissien.core.reading.book.ReadingBlock.PageBreak
import app.paroissien.core.reading.book.ReadingBlock.Paragraph
import app.paroissien.core.reading.book.ReadingBlock.Rubric
import app.paroissien.core.reading.book.ReadingBlock.Title
import app.paroissien.core.reading.book.ReadingBlock.Versicle
import app.paroissien.core.reading.book.TextRole
import org.json.JSONObject

/**
 * Livre d'essai de la phase 1b : le Livre I complet de l'Imitation (extraction provisoire, content/spike), puis des
 * pages latines et liturgiques pour la typographie. Remplacé par les paquets de la phase 2.
 */
fun loadSpikeBook(context: Context): List<ReadingBlock> {
    val json = context.assets.open("spike/imitation-livre1.json").bufferedReader(Charsets.UTF_8).use { JSONObject(it.readText()) }
    return buildList {
        add(Ornament("titre.o1"))
        add(Title("titre", "Paroissien", "Livre d’essai — le livre"))
        add(Ornament("titre.o2"))
        add(Rubric("titre.aide", "Tournez la page en la tirant par son bord, ou touchez le bord. Touchez le milieu pour les réglages."))
        add(PageBreak("titre.fin"))
        addAll(imitation(json))
        add(PageBreak("latin.debut"))
        addAll(LATIN_AND_LITURGY)
    }
}

private fun imitation(json: JSONObject): List<ReadingBlock> = buildList {
    add(Title("imit.titre", "Imitation de Jésus-Christ", "Livre premier — ${json.getString("subtitle").lowercase().replaceFirstChar { it.uppercase() }}"))
    add(Rubric("imit.trad", "Traduction de F. de Lamennais"))
    add(PageBreak("imit.titre.fin"))
    val chapters = json.getJSONArray("chapters")
    for (c in 0 until chapters.length()) {
        val chapter = chapters.getJSONObject(c)
        val n = chapter.getInt("n")
        val roman = roman(n)
        add(Heading("imit.1.$n", "Chapitre $roman", runningHead = "Imitation · Livre I · Chapitre $roman"))
        add(Rubric("imit.1.$n.titre", chapter.getString("title")))
        val paragraphs = chapter.getJSONArray("paragraphs")
        for (p in 0 until paragraphs.length()) {
            val para = paragraphs.getJSONObject(p)
            add(
                Paragraph(
                    id = "imit.1.$n.p$p",
                    text = para.getString("text"),
                    lang = BlockLang.FR,
                    dropCap = p == 0,
                    verse = if (p > 0 && para.has("verse")) para.getInt("verse") else null,
                ),
            )
        }
        val reflection = chapter.getJSONArray("reflection")
        if (reflection.length() > 0) {
            add(Heading("imit.1.$n.reflexion", "Réflexion", minor = true))
            for (r in 0 until reflection.length()) add(Paragraph("imit.1.$n.r$r", reflection.getString(r), BlockLang.FR, role = TextRole.SMALL))
        }
    }
}

private fun roman(n: Int): String {
    val values = listOf(10 to "X", 9 to "IX", 5 to "V", 4 to "IV", 1 to "I")
    var rest = n
    return buildString { values.forEach { (v, s) -> while (rest >= v) { append(s); rest -= v } } }
}

private const val NNBSP = ' '

private val LATIN_AND_LITURGY: List<ReadingBlock> = listOf(
    Heading("lat.1", "Liber primus — Caput I", runningHead = "De imitatione Christi"),
    Rubric("lat.1.titre", "De imitatione Christi et contemptu mundi omniumque ejus vanitatum."),
    Paragraph(
        "lat.1.p1",
        "Qui sequitur me non ambulat in tenebris, dicit Dominus. Hæc sunt verba Christi, quibus admonemur quatenus " +
            "vitam ejus et mores imitemur, si volumus veraciter illuminari, et ab omni cæcitate cordis liberari. Summum " +
            "igitur studium nostrum sit in vita Jesu meditari.",
        BlockLang.LA, dropCap = true,
    ),
    Paragraph(
        "lat.1.p2",
        "Doctrina ejus omnes doctrinas Sanctorum præcellit, et qui spiritum haberet absconditum ibi manna inveniret. Sed " +
            "contingit quod multi ex frequenti auditu Evangelii parvum desiderium sentiunt, quia spiritum Christi non habent.",
        BlockLang.LA,
    ),
    PageBreak("ord.debut"),
    Heading("ord.1", "Prières au bas de l’autel", runningHead = "Ordinaire de la messe"),
    Rubric("ord.1.r1", "Le prêtre, au bas de l’autel, fait le signe de la croix et dit$NNBSP:"),
    Paragraph("ord.1.p1", "In nómine Patris, ✠ et Fílii, et Spíritus Sancti. Amen.", BlockLang.LA),
    Versicle("ord.1.v1", '℣', "Introíbo ad altáre Dei."),
    Versicle("ord.1.v2", '℟', "Ad Deum, qui lætíficat juventútem meam."),
    Rubric("ord.1.r2", "Puis il récite, alternativement avec le servant, le psaume 42$NNBSP:"),
    Versicle("ord.1.v3", '℣', "Júdica me, Deus, et discérne causam meam de gente non sancta: ab hómine iníquo et dolóso érue me."),
    Versicle("ord.1.v4", '℟', "Quia tu es, Deus, fortitúdo mea: quare me repulísti, et quare tristis incédo, dum afflígit me inimícus?"),
    Heading("ord.2", "Gloria in excélsis"),
    Paragraph(
        "ord.2.la",
        "Glória in excélsis Deo. Et in terra pax homínibus bonæ voluntátis. Laudámus te. Benedícimus te. Adorámus te. " +
            "Glorificámus te. Grátias ágimus tibi propter magnam glóriam tuam. Dómine Deus, Rex cæléstis, Deus Pater " +
            "omnípotens. Dómine Fili unigénite, Jesu Christe. Dómine Deus, Agnus Dei, Fílius Patris. Qui tollis peccáta " +
            "mundi, miserére nobis. Qui tollis peccáta mundi, súscipe deprecatiónem nostram. Qui sedes ad déxteram Patris, " +
            "miserére nobis. Quóniam tu solus Sanctus. Tu solus Dóminus. Tu solus Altíssimus, Jesu Christe. Cum Sancto " +
            "Spíritu ✠ in glória Dei Patris. Amen.",
        BlockLang.LA, dropCap = true,
    ),
    Heading("ord.3", "Gloire à Dieu", minor = true),
    Paragraph(
        "ord.3.fr",
        "Gloire à Dieu au plus haut des cieux, et paix sur la terre aux hommes de bonne volonté. Nous vous louons. Nous " +
            "vous bénissons. Nous vous adorons. Nous vous glorifions. Nous vous rendons grâces pour votre grande gloire. " +
            "Seigneur Dieu, Roi du ciel, Dieu le Père tout-puissant. Seigneur, Fils unique, Jésus-Christ. Seigneur Dieu, " +
            "Agneau de Dieu, Fils du Père. Vous qui effacez les péchés du monde, ayez pitié de nous. Vous qui effacez les " +
            "péchés du monde, recevez notre prière. Vous qui êtes assis à la droite du Père, ayez pitié de nous. Car vous " +
            "êtes le seul Saint. Vous êtes le seul Seigneur. Vous êtes le seul Très-Haut, Jésus-Christ, avec le " +
            "Saint-Esprit ✠ dans la gloire de Dieu le Père. Ainsi soit-il.",
        BlockLang.FR, role = TextRole.SMALL,
    ),
    PageBreak("ctl.debut"),
    Heading("ctl.1", "Glyphes de contrôle", runningHead = "Contrôle typographique"),
    Paragraph("ctl.1.p", "✠ ✝ ⁂ ℣ ℟ ❦ ❧ ☙ æ œ ǽ ǣ ſ á é í ó ú ý ë ï « » ’ – — … « Fiat »$NNBSP; ainsi soit-il$NNBSP!", BlockLang.FR, role = TextRole.CENTERED),
    Heading("ctl.2", "Césure du latin liturgique"),
    Paragraph(
        "ctl.2.p",
        listOf(
            "Dóminus", "glória", "excélsis", "Spíritus", "Kýrie", "eléison", "sǽcula", "Benedícimus", "Glorificámus",
            "deprecatiónem", "misericórdiam", "Introíbo", "lætíficat", "juventútem", "discérne", "iníquo", "fortitúdo",
            "súscipe", "unigénite", "Altíssimus", "voluntátis", "homínibus", "consubstantiálem", "resurrectiónem",
        ).joinToString("   ") { LiangHyphenator.latinLiturgical.insertSoftHyphens(it).replace(LiangHyphenator.SOFT_HYPHEN, '‐') },
        BlockLang.FR, role = TextRole.SMALL,
    ),
    PageBreak("colophon.debut"),
    Ornament("colophon.o1"),
    Heading("colophon", "Colophon", runningHead = "Colophon"),
    Paragraph(
        "colophon.p",
        "Imitation de Jésus-Christ, traduction de F. de Lamennais (1824), texte du Projet Gutenberg n° 57824. Texte " +
            "latin : The Latin Library, corrigé. Ordinaire et Gloria : Missel romain, domaine public. Polices EB Garamond, " +
            "Literata et Junicode, licence OFL 1.1. Tourne-page adapté de Mekuri (AniCanon), licence MIT. Césure latine : " +
            "motifs hyph-utf8 de C. Beccari, Solesmes et É. Roux, licence MIT.",
        BlockLang.FR, role = TextRole.CENTERED,
    ),
    Ornament("colophon.o2"),
)
