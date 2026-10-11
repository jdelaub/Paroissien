package app.paroissien.spike

/**
 * Livre d'essai de la phase 1a (porte A) : quelques pages découpées à la main, en attendant le paginateur (1b)
 * et les paquets (phase 2). Textes du domaine public : Imitation (latin ; trad. Lamennais, 1824), Ordinaire et
 * Gloria du Missel romain, traduction française des anciens paroissiens. Les rubriques sont de notre main.
 * Dans le texte, _…_ marque l'italique ; U+202F est l'espace fine des signes doubles.
 */
enum class Lang(val tag: String) { FR("fr"), LA("la") }

sealed interface Block {
    data class Title(val text: String, val subtitle: String? = null) : Block
    data class Heading(val text: String) : Block
    data class Para(val text: String, val lang: Lang, val dropCap: Boolean = false) : Block
    data class Rubric(val text: String) : Block
    data class Versicle(val mark: Char, val text: String, val lang: Lang = Lang.LA) : Block
    data object Ornament : Block
    data object GlyphCheck : Block
    data object HyphenationCheck : Block
    data class Credits(val lines: List<String>) : Block
}

data class SpikePage(val runningHead: String, val blocks: List<Block>)

private const val NNBSP = ' '

val spikeBook: List<SpikePage> = listOf(
    SpikePage(
        runningHead = "",
        blocks = listOf(
            Block.Ornament,
            Block.Title("Paroissien", "Livre d’essai — feuille et encre"),
            Block.Ornament,
            Block.Rubric("Tournez la page en la tirant par son bord, ou touchez le bord droit. Touchez le milieu pour les réglages."),
        ),
    ),
    SpikePage(
        runningHead = "Imitation de Jésus-Christ",
        blocks = listOf(
            Block.Heading("Livre premier — Chapitre premier"),
            Block.Rubric("Qu’il faut imiter Jésus-Christ, et mépriser toutes les vanités du monde."),
            Block.Para(
                "_Celui qui me suit, ne marche point dans les ténèbres_, dit le Seigneur. Ce sont les paroles de " +
                    "Jésus-Christ, par lesquelles il nous exhorte à imiter sa conduite et sa vie, si nous voulons être " +
                    "vraiment éclairés et délivrés de tout aveuglement du cœur.",
                Lang.FR, dropCap = true,
            ),
            Block.Para("Que notre principale étude soit donc de méditer la vie de Jésus-Christ.", Lang.FR),
            Block.Para(
                "La doctrine de Jésus-Christ surpasse toute doctrine des Saints$NNBSP; et qui posséderait son esprit, " +
                    "y trouverait la manne cachée.",
                Lang.FR,
            ),
        ),
    ),
    SpikePage(
        runningHead = "Imitation de Jésus-Christ",
        blocks = listOf(
            Block.Para(
                "Mais il arrive que plusieurs, à force d’entendre l’Évangile n’en sont que peu touchés, parce qu’ils " +
                    "n’ont point l’esprit de Jésus-Christ.",
                Lang.FR,
            ),
            Block.Para(
                "Voulez-vous comprendre parfaitement et goûter les paroles de Jésus-Christ$NNBSP: appliquez-vous à " +
                    "conformer toute votre vie à la sienne.",
                Lang.FR,
            ),
            Block.Para(
                "Que vous sert de raisonner profondément sur la Trinité, si vous n’êtes pas humbles, et que par là " +
                    "vous déplaisiez à la Trinité$NNBSP?",
                Lang.FR,
            ),
            Block.Para(
                "Certes les discours sublimes ne font pas l’homme juste et saint$NNBSP; mais une vie pure rend cher à Dieu.",
                Lang.FR,
            ),
            Block.Para("J’aime mieux sentir la componction, que d’en savoir la définition.", Lang.FR),
        ),
    ),
    SpikePage(
        runningHead = "Imitation de Jésus-Christ",
        blocks = listOf(
            Block.Para(
                "Quand vous sauriez toute la Bible et toutes les sentences des philosophes, que vous servirait tout " +
                    "cela, sans la grâce et la charité$NNBSP?",
                Lang.FR,
            ),
            Block.Para(
                "_Vanité des vanités, et tout n’est que vanité_, hors aimer Dieu, et le servir lui seul.",
                Lang.FR,
            ),
            Block.Para("La souveraine sagesse est de tendre au royaume du Ciel par le mépris du monde.", Lang.FR),
            Block.Para("Vanité donc, d’amasser des richesses périssables, et d’espérer en elles.", Lang.FR),
            Block.Para("Vanité, d’aspirer aux honneurs, et de s’élever à ce qu’il y a de plus haut.", Lang.FR),
        ),
    ),
    SpikePage(
        runningHead = "De imitatione Christi",
        blocks = listOf(
            Block.Heading("Liber primus — Caput I"),
            Block.Rubric("De imitatione Christi et contemptu mundi omniumque ejus vanitatum."),
            Block.Para(
                "Qui sequitur me non ambulat in tenebris, dicit Dominus. Hæc sunt verba Christi, quibus admonemur " +
                    "quatenus vitam ejus et mores imitemur, si volumus veraciter illuminari, et ab omni cæcitate cordis " +
                    "liberari. Summum igitur studium nostrum sit in vita Jesu meditari.",
                Lang.LA, dropCap = true,
            ),
            Block.Para(
                "Doctrina ejus omnes doctrinas Sanctorum præcellit, et qui spiritum haberet absconditum ibi manna " +
                    "inveniret. Sed contingit quod multi ex frequenti auditu Evangelii parvum desiderium sentiunt, quia " +
                    "spiritum Christi non habent.",
                Lang.LA,
            ),
        ),
    ),
    SpikePage(
        runningHead = "De imitatione Christi",
        blocks = listOf(
            Block.Para(
                "Qui autem vult plene et sapide verba Christi intelligere, oportet ut totam vitam suam illi studeat " +
                    "conformare.",
                Lang.LA,
            ),
            Block.Para(
                "Quid prodest tibi alta de Trinitate disputare, si careas humilitate unde displiceas Trinitati? Vere " +
                    "alta verba non faciunt sanctum et justum, sed virtuosa vita efficit Deo carum. Opto magis sentire " +
                    "compunctionem quam scire definitionem.",
                Lang.LA,
            ),
            Block.Para(
                "Vanitas vanitatum et omnia vanitas, præter amare Deum et illi soli servire. Ista est summa sapientia, " +
                    "per contemptum mundi tendere ad regna cælestia.",
                Lang.LA,
            ),
        ),
    ),
    SpikePage(
        runningHead = "Ordinaire de la messe",
        blocks = listOf(
            Block.Heading("Prières au bas de l’autel"),
            Block.Rubric("Le prêtre, au bas de l’autel, fait le signe de la croix et dit$NNBSP:"),
            Block.Para("In nómine Patris, ✠ et Fílii, et Spíritus Sancti. Amen.", Lang.LA),
            Block.Versicle('℣', "Introíbo ad altáre Dei."),
            Block.Versicle('℟', "Ad Deum, qui lætíficat juventútem meam."),
            Block.Rubric("Puis il récite, alternativement avec le servant, le psaume 42$NNBSP:"),
            Block.Versicle(
                '℣',
                "Júdica me, Deus, et discérne causam meam de gente non sancta: ab hómine iníquo et dolóso érue me.",
            ),
            Block.Versicle(
                '℟',
                "Quia tu es, Deus, fortitúdo mea: quare me repulísti, et quare tristis incédo, dum afflígit me inimícus?",
            ),
        ),
    ),
    SpikePage(
        runningHead = "Ordinaire de la messe",
        blocks = listOf(
            Block.Heading("Gloria in excélsis"),
            Block.Para(
                "Glória in excélsis Deo. Et in terra pax homínibus bonæ voluntátis. Laudámus te. Benedícimus te. " +
                    "Adorámus te. Glorificámus te. Grátias ágimus tibi propter magnam glóriam tuam. Dómine Deus, Rex " +
                    "cæléstis, Deus Pater omnípotens. Dómine Fili unigénite, Jesu Christe. Dómine Deus, Agnus Dei, " +
                    "Fílius Patris. Qui tollis peccáta mundi, miserére nobis. Qui tollis peccáta mundi, súscipe " +
                    "deprecatiónem nostram. Qui sedes ad déxteram Patris, miserére nobis. Quóniam tu solus Sanctus. " +
                    "Tu solus Dóminus. Tu solus Altíssimus, Jesu Christe. Cum Sancto Spíritu ✠ in glória Dei Patris. Amen.",
                Lang.LA, dropCap = true,
            ),
        ),
    ),
    SpikePage(
        runningHead = "Ordinaire de la messe",
        blocks = listOf(
            Block.Heading("Gloire à Dieu"),
            Block.Para(
                "Gloire à Dieu au plus haut des cieux, et paix sur la terre aux hommes de bonne volonté. Nous vous " +
                    "louons. Nous vous bénissons. Nous vous adorons. Nous vous glorifions. Nous vous rendons grâces " +
                    "pour votre grande gloire. Seigneur Dieu, Roi du ciel, Dieu le Père tout-puissant. Seigneur, Fils " +
                    "unique, Jésus-Christ. Seigneur Dieu, Agneau de Dieu, Fils du Père. Vous qui effacez les péchés du " +
                    "monde, ayez pitié de nous. Vous qui effacez les péchés du monde, recevez notre prière. Vous qui " +
                    "êtes assis à la droite du Père, ayez pitié de nous. Car vous êtes le seul Saint. Vous êtes le seul " +
                    "Seigneur. Vous êtes le seul Très-Haut, Jésus-Christ, avec le Saint-Esprit ✠ dans la gloire de " +
                    "Dieu le Père. Ainsi soit-il.",
                Lang.FR, dropCap = true,
            ),
        ),
    ),
    SpikePage(
        runningHead = "Contrôle typographique",
        blocks = listOf(
            Block.Heading("Glyphes de contrôle"),
            Block.GlyphCheck,
            Block.Heading("Césure du latin liturgique"),
            Block.HyphenationCheck,
        ),
    ),
    SpikePage(
        runningHead = "Colophon",
        blocks = listOf(
            Block.Ornament,
            Block.Credits(
                listOf(
                    "Imitation de Jésus-Christ, traduction de F. de Lamennais (1824), texte du Projet Gutenberg n° 57824.",
                    "Texte latin : The Latin Library, corrigé.",
                    "Ordinaire et Gloria : Missel romain (1962), domaine public.",
                    "Polices : EB Garamond, Literata et Junicode, sous licence OFL 1.1.",
                    "Tourne-page adapté de Mekuri (AniCanon), licence MIT.",
                    "Césure latine : motifs hyph-utf8 de C. Beccari, Solesmes et É. Roux, licence MIT.",
                ),
            ),
            Block.Ornament,
        ),
    ),
)
