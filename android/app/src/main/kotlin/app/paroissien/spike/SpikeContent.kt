package app.paroissien.spike

import app.paroissien.core.model.text.Inlines
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

/**
 * Livre de démonstration de la typographie (phases 1a et 1b) : latin, Ordinaire, Gloria, contrôle des glyphes et de la
 * césure. Les vrais livres viennent maintenant des paquets (phase 2) ; celui-ci reste écrit à la main.
 */
fun demoBook(): List<ReadingBlock> = buildList {
    add(Ornament("titre.o1"))
    add(Title("titre", rt("Paroissien"), rt("Livre de démonstration")))
    add(Ornament("titre.o2"))
    add(Rubric("titre.aide", rt("Tournez la page en la tirant par son bord, ou touchez le bord. Touchez le milieu pour les réglages et la bibliothèque.")))
    add(PageBreak("titre.fin"))
    addAll(LATIN_AND_LITURGY)
}

private fun rt(markup: String) = Inlines.fromMarkup(markup)

private fun para(id: String, text: String, lang: BlockLang, dropCap: Boolean = false, role: TextRole = TextRole.BODY) =
    Paragraph(id, rt(text), lang, dropCap = dropCap, role = role)

private const val NNBSP = '\u202F'

private val LATIN_AND_LITURGY: List<ReadingBlock> = listOf(
    Heading("lat.1", rt("Liber primus — Caput I"), runningHead = "De imitatione Christi"),
    Rubric("lat.1.titre", rt("De imitatione Christi et contemptu mundi omniumque ejus vanitatum.")),
    para(
        "lat.1.p1",
        "Qui sequitur me non ambulat in tenebris, dicit Dominus. Hæc sunt verba Christi, quibus admonemur quatenus " +
            "vitam ejus et mores imitemur, si volumus veraciter illuminari, et ab omni cæcitate cordis liberari. Summum " +
            "igitur studium nostrum sit in vita Jesu meditari.",
        BlockLang.LA, dropCap = true,
    ),
    para(
        "lat.1.p2",
        "Doctrina ejus omnes doctrinas Sanctorum præcellit, et qui spiritum haberet absconditum ibi manna inveniret. Sed " +
            "contingit quod multi ex frequenti auditu Evangelii parvum desiderium sentiunt, quia spiritum Christi non habent.",
        BlockLang.LA,
    ),
    PageBreak("ord.debut"),
    Heading("ord.1", rt("Prières au bas de l’autel"), runningHead = "Ordinaire de la messe"),
    Rubric("ord.1.r1", rt("Le prêtre, au bas de l’autel, fait le signe de la croix et dit$NNBSP:")),
    para("ord.1.p1", "In nómine Patris, ✠ et Fílii, et Spíritus Sancti. Amen.", BlockLang.LA),
    Versicle("ord.1.v1", rt("℣ Introíbo ad altáre Dei.")),
    Versicle("ord.1.v2", rt("℟ Ad Deum, qui lætíficat juventútem meam.")),
    Rubric("ord.1.r2", rt("Puis il récite, alternativement avec le servant, le psaume 42$NNBSP:")),
    Versicle("ord.1.v3", rt("℣ Júdica me, Deus, et discérne causam meam de gente non sancta: ab hómine iníquo et dolóso érue me.")),
    Versicle("ord.1.v4", rt("℟ Quia tu es, Deus, fortitúdo mea: quare me repulísti, et quare tristis incédo, dum afflígit me inimícus?")),
    Heading("ord.2", rt("Gloria in excélsis")),
    para(
        "ord.2.la",
        "Glória in excélsis Deo. Et in terra pax homínibus bonæ voluntátis. Laudámus te. Benedícimus te. Adorámus te. " +
            "Glorificámus te. Grátias ágimus tibi propter magnam glóriam tuam. Dómine Deus, Rex cæléstis, Deus Pater " +
            "omnípotens. Dómine Fili unigénite, Jesu Christe. Dómine Deus, Agnus Dei, Fílius Patris. Qui tollis peccáta " +
            "mundi, miserére nobis. Qui tollis peccáta mundi, súscipe deprecatiónem nostram. Qui sedes ad déxteram Patris, " +
            "miserére nobis. Quóniam tu solus Sanctus. Tu solus Dóminus. Tu solus Altíssimus, Jesu Christe. Cum Sancto " +
            "Spíritu ✠ in glória Dei Patris. Amen.",
        BlockLang.LA, dropCap = true,
    ),
    Heading("ord.3", rt("Gloire à Dieu"), minor = true),
    para(
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
    Heading("ctl.1", rt("Glyphes de contrôle"), runningHead = "Contrôle typographique"),
    para("ctl.1.p", "✠ ✝ ⁂ ℣ ℟ ❦ ❧ ☙ æ œ ǽ ǣ ſ á é í ó ú ý ë ï « » ’ – — … « Fiat »$NNBSP; ainsi soit-il$NNBSP!", BlockLang.FR, role = TextRole.CENTERED),
    Heading("ctl.2", rt("Césure du latin liturgique")),
    para(
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
    Heading("colophon", rt("Colophon"), runningHead = "Colophon"),
    para(
        "colophon.p",
        "Ordinaire et Gloria : Missel romain, domaine public. Polices EB Garamond, " +
            "Literata et Junicode, licence OFL 1.1. Tourne-page adapté de Mekuri (AniCanon), licence MIT. Césure latine : " +
            "motifs hyph-utf8 de C. Beccari, Solesmes et É. Roux, licence MIT.",
        BlockLang.FR, role = TextRole.CENTERED,
    ),
    Ornament("colophon.o2"),
)
