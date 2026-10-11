# Imitation de Jésus-Christ : provenance

Deux publications du domaine public, produites par les scripts de ce dossier :

| Publication | Paquet | Contenu | Blocs | Paquet SQLite (gzip -9) |
|---|---|---|---|---|
| `imitation.fr` | `imitation-fr` | Traduction de Lamennais (1824) avec ses 115 réflexions et 721 notes | 3 465 | 2 424 Ko (860 Ko) |
| `imitation.la` | `imitation-la` | Texte latin, 4 livres, après 485 corrections d'OCR | 667 | 1 048 Ko (395 Ko) |

Chapitres : 25, 12, 59 et 18 dans les deux langues, plus l'exhortation qui ouvre le livre IV.

## Sources

| Fichier (`content/.cache/raw/imitation/`) | Rôle | Origine |
|---|---|---|
| `pg57824.txt` | texte français | Projet Gutenberg n° 57824, <https://www.gutenberg.org/cache/epub/57824/pg57824.txt> |
| `kempis1.shtml` à `kempis4.shtml` | texte latin | The Latin Library, <https://www.thelatinlibrary.com/kempis/kempis1.shtml> à `kempis4.shtml` |
| `wikisource-l1.wiki` à `-l4.wiki` | témoin (relecture seulement) | la.wikisource, « De imitatione Christi/I » à « /IV », wikitexte brut |
| `archive-thomaekempisde00thom.txt` | témoin | archive.org, Leipzig, Tauchnitz, 1840 (texte de Desbillons), OCR djvu |
| `archive-deimitationechr00thomgoog.txt` | témoin | archive.org, Londres, Williams et Norgate, 1867, OCR djvu médiocre |
| `archive-deimitationechri00unse.txt` | témoin | archive.org, New York et Londres, White and Allen, 1888, OCR Tesseract |

`fetch-log.json` consigne pour chaque fichier l'URL, la date (2026-10-11), la taille et le sha256. Les témoins ne sont pas redistribués : ils servent seulement à décider des corrections. La Vulgate clémentine du cache biblique (`content/.cache/raw/bible/`) a servi de lexique latin d'appoint, par l'option `--vocab` de `collate_la.py`.

### Français : Lamennais, édition Bray 1859

L'édition source est la « traduction nouvelle avec des réflexions à la fin de chaque chapitre », 43ᵉ édition, Paris, Ambroise Bray, 1859. Elle a été numérisée par Google Books, puis transcrite et relue par Laurent Vogel et l'équipe Distributed Proofreaders (pgdp.net), en 2018. La transcription est de très bonne qualité ; seules deux anomalies de structure ont été relevées (voir plus bas).

### Latin : The Latin Library

The Latin Library n'indique pas son édition. Les erreurs typiques d'OCR (s long lu « f », æ lu « ex ») désignent une impression ancienne, sans doute du XVIIIᵉ siècle, avec j, æ et œ. La.wikisource reprend la même transcription et en a corrigé une partie (environ 350 mots). Les éditions de 1840 et 1888 suivent une autre recension : elles départagent les lectures fautives, mais leurs variantes ne sont jamais importées.

## Chaîne

```bash
python3 -I content/sources/imitation/fetch.py --compare     # sources + témoins, une seule fois (cache)
python3 -I content/sources/imitation/normalize.py           # → content/data/imitation.fr et imitation.la
python3 -I content/sources/imitation/check.py               # contrôles propres à l'Imitation
python3 -I content/sources/imitation/collate_la.py --vocab <vulgate.osis.xml> > rapport.tsv   # aide à la relecture du latin
python3 -I content/pqt.py validate imitation.fr imitation.la
```

`fetch.py` sans option ne télécharge que les sources ; `--compare` ajoute les témoins. User-Agent `Paroissien-pipeline/0.1`, une pause de 2 s entre deux requêtes, et aucun fichier déjà en cache n'est retéléchargé. `normalize.py` s'arrête à la moindre anomalie de structure : chapitre manquant, note non appelée, italique non fermé, mot en « oe » imprévu, correction introuvable. `check.py` vérifie le nombre et l'ordre des chapitres, les lettrines, l'absence de « [n] », de « _ », de « oe » et d'apostrophe droite, ainsi que les espaces fines et la ponctuation. Il vérifie aussi que chaque ligne de `corrections-la.tsv` figure dans ce README.

## Décisions communes

- **Unités** : `l1` à `l4`, un livre chacune. Le premier bloc, `lN.titre`, est un paragraphe centré : nom du livre en petites capitales, puis titre du livre (« Avis utiles pour entrer dans la vie intérieure. »). Le livre IV latin n'a pas de titre propre dans la source : on y met seulement « Liber Quartus », tiré de l'en-tête de page.
- **Chapitres** : `lN.cK`, `heading` de niveau 1 avec `label` « Chapitre III » ou « Caput III ». Le texte est le titre imprimé.
- **Exhortation du livre IV** : clé `l4.exhortation`, `heading` de niveau 1 sans `label`, avec ses paragraphes, sa réflexion et ses notes (`l4.exhortation.p1`, `l4.exhortation.r1`, `l4.exhortation.n557`).
- **Lettrine** : `dropCap: true` sur le premier paragraphe de chaque chapitre et de l'exhortation.
- **Numéros de verset** : un paragraphe qui ouvre un verset numéroté porte l'attribut `verse` (« 3 ») et commence par l'inline `{"num": "3"}`. Le numéro reste visible à la lecture et ne compte pas dans le texte canonique.
- **`aligned: false`** : les deux publications sont indépendantes, mais leurs clés de chapitre sont communes (`l3.c23`). Les découpages en paragraphes diffèrent d'une langue à l'autre.

## Français

- **Périmètre** : les quatre livres. Ne sont pas repris : l'avertissement des éditeurs, la préface de Lamennais (avec ses notes [1] à [13]), les prières pendant la messe tirées de Fénelon, les vêpres et complies du dimanche et la table. Ces textes restent dans la source s'ils servent un jour (missel, prières).
- **Gutenberg** : l'en-tête et la licence du Projet Gutenberg sont retirés. La source est créditée dans `publication.json` (`source`, `license.attribution`), comme le prévoit le schéma.
- **Paragraphes** : `lN.cK.pM` suit l'ordre des paragraphes dans le chapitre (2 371 paragraphes, dont 534 ouvrent un verset).
- **Réflexion** : `heading` `lN.cK.r` de niveau 2 (« Réflexion. »), puis les paragraphes `lN.cK.r1`, `r2`… en style `small`. Il y en a 115 : une par chapitre, plus celle de l'exhortation.
- **Intertitres internes** (`lN.cK.hJ`, niveau 2) :
  - les cinq « Prière … » du livre III (III, 3 ; 15 ; 23 deux fois ; 27). Contrairement à ce que suggérait la commande, ce ne sont pas des ajouts de Lamennais : elles appartiennent au texte de l'Imitation (les *Oratio* du latin). Elles restent donc en style normal et suivent la numérotation des versets ;
  - les « Voix de Jésus-Christ », « Voix du disciple » et « Voix du Bien-Aimé » du livre IV.
- **Notes** : les 721 notes [14] à [734] deviennent des blocs `note` (`placement: foot`) placés à la fin de l'unité, dans l'ordre des numéros. Leur clé reprend le chapitre de l'appel et le numéro de la source (`l1.c1.n14`, `label` « 14 »). L'appel est `{"note": "l1.c1.n14", "l": "14"}`, à l'intérieur de l'italique s'il y était.
- **Italique** : `_…_` donne `{"m": ["i"], …}`. Les espaces au bord d'une marque sont reportées hors de la marque.
- **Typographie** :
  - apostrophe ’ (5 421) ;
  - espace fine insécable U+202F avant ; : ! ? et à l'intérieur de « » (2 613) ;
  - « ... » remplacé par « … » (2 fois) ;
  - « oe » remplacé par « œ » selon une liste fermée : cœur 274, œuvres 34, cœurs 14, œil 10, œuvre 9, mœurs 6, vœux 4, chœurs 3, sœurs 1, bœufs 1.

  Les intertitres imprimés en capitales passent en casse de lecture par une table fermée (`FR_CAPS`). Aucune autre retouche d'orthographe : « très-grave », « piége » et « rassassiés » restent tels quels.
- **Anomalies de la source** :
  - le chapitre III, 38 est imprimé « CHAPITRE XXXIII. » ; seul le libellé est recalculé (`FR_CHAPTER_ERRATA`) ;
  - en III, 15, la numérotation repart à 1 après la « Prière pour demander à Dieu la grâce d'accomplir sa volonté » : `l3.c15` a donc deux paragraphes `verse: "1"`. Les identifiants, fondés sur la position, restent uniques.
- Les interlocuteurs « J.-C. » et « LE F. » restent tels que transcrits. Ils étaient probablement en petites capitales dans l'imprimé : on pourra les passer en `sc`.

## Latin

- **Paragraphes** : `lN.cK.vX` reprend le numéro latin (540 paragraphes numérotés, numérotation régulière partout). Deux paragraphes ne sont pas numérotés :
  - l'alinéa « Nulla actio te impediet… », qui prolonge II, 4, 1 : `l2.c4.v1.2`, sur le modèle des sous-paragraphes `ex.230.2` ;
  - le préambule du livre IV : `l4.exhortation.p1`.
- **Intertitres** : les cinq *Oratio* du livre III et « Vox Christi dicit. » de l'exhortation, en `heading` de niveau 2 (`lN.cK.hJ`).
- **Orthographe de l'édition conservée** : j, æ, œ, *charitas*, *prætium*, *negocium*, *contemtus*, *exultare*, *quicquam*, *solicitudo*, *Eya*, etc. Seules les fautes de lecture sont corrigées. Les espaces devant la ponctuation sont retirées. `<i>` devient la marque `i`.
- **Méthode de correction** :
  1. `collate_la.py` signale trois cas : un mot absent de tous les témoins et du lexique de la Vulgate ; un mot dont un témoin donne, entre les deux mêmes voisins, une forme proche différente ; une différence avec la.wikisource.
  2. Chaque signalement est jugé à la main. Une correction n'est retenue que si la forme de la source n'est pas du latin, ou si elle est absurde dans la phrase et s'explique par une erreur de lecture : s long lu f, f lu s long, lettre omise ou doublée, æ lu « ex », mots soudés.
  3. Les variantes de recension, c'est-à-dire un mot valide et différent dans une autre édition, ne sont jamais importées : elles sont signalées plus bas.
  4. Chaque correction est une ligne de `corrections-la.tsv` (`id`, `avant`, `après`, `n` occurrences attendues, `motif ; témoins`). `normalize.py` l'applique en vérifiant le nombre d'occurrences. Le tableau ci-dessous en est la copie.
- **Bilan** : 485 corrections (487 occurrences), dont 353 déjà faites par la.wikisource. Les témoins imprimés (1840, 1867, 1888) en confirment 292 ; 81 reposent sur la seule évidence (mot inexistant, correction d'une lettre). Par type :
  - 167 lettres omises ;
  - 95 autres lettres mal lues ;
  - 73 lettres en trop ;
  - 44 s longs lus « f » et 13 f lus « s » ;
  - 30 mots mal lus ;
  - 26 interversions ;
  - 10 æ lus « ex » ;
  - 10 mots soudés et 6 espaces parasites ;
  - 4 dittographies ;
  - 3 lettrines « O » perdues ;
  - 3 mots ou phrases omis ;
  - 1 abréviation développée.
- **Corrections de la.wikisource écartées** :
  - I, 3, 5 *movendi* (Wikisource : *movendas*), car *movendi quæstiones* se construit ;
  - III, 15, 3 *charius* (Wikisource : *carius*), graphie de l'édition comme *charitas* ;
  - l'« Amen » final que Wikisource ajoute en IV, 18, absent de la source et de l'édition de 1888.
- En III, 3, intertitre *imporandum* est corrigé en *implorandum*, comme Wikisource. L'édition de 1840 lit *implorandam*.

## Points douteux à relire

Latin, laissés tels quels (mots valides, mais suspects ou différents des autres éditions) :

| Lieu | Source | Remarque |
|---|---|---|
| I, 1, 5 | — | La phrase *Memento illius frequenter proverbii: Quia non satiatur oculus visu, nec auris impletur auditu* manque (aussi dans Wikisource) ; Lamennais la traduit. Non rétablie, faute de témoin de la même édition. |
| I, 5, 1 | *defendat* | 1840 et 1888 : *offendat* (« Non te offendat auctoritas scribentis »). |
| I, 17, 2 ; I, 24, 5 | *paciendum*, *pacientia* | graphie médiévale ou lecture de « ti » : ailleurs l'édition écrit *patientia*. |
| I, 18, 4 | *delecti* | autres éditions : *dilecti*. |
| I, 20, 3 | *desperationem* | 1840 et 1888 : *deceptionem*. |
| I, 21, 2 | *vane reddimus verba* | autres éditions : *vane ridemus*. |
| I, 24, 4 | *propetenus* | mot inconnu ; 1888 : *securus cum beatis*. |
| II, 9, 1 | *Quid magis es* | autres éditions : *Quid magnum*. |
| II, 9, 5 | *informis* | *infirmis* attendu par le sens ; aucun témoin lisible. |
| III, 3, intertitre | *implorandum* | 1840 : *implorandam*. |
| III, 10, 2 | *Nonquid* | forme attendue : *Numquid*. |
| III, 58, 1 | *divum judicium* | *divinum* ? |
| III, 58, 6 | *cogitant* | corrigé de *cognitant* sans témoin ; *cognoscunt* possible. |
| III, 20, 4 | *tædium* | corrigé de *tædiunt* d'après le sens, sans témoin. |
| III, 36, 3 | *succumbere* | corrigé de *succumbi* d'après 1867 seulement. |
| III, 50, 4 | *mihi* | corrigé de *michi* ; peut-être une graphie de l'édition. |
| I, 24, 5 | *mærebit* | corrigé de *merebit* ; l'édition imprimait peut-être *mœrebit*. |
| I, 13, 7 | *exaltabit* | leçon de Wikisource, sans témoin imprimé ; la source avait *exultabit*, mot valide. |

Plus généralement, une confusion qui donne un mot valide (*fit* pour *sit*, *fine* pour *sine*) n'est repérée que si un témoin a le même contexte. Il peut en rester quelques-unes. Une collation complète avec une édition à s long de la même famille (Barbou 1773 ou 1789, Pezzana 1735 sur archive.org) reste à faire.

Français :

- « JÉSUS-CHRIST » en capitales dans quelques titres de chapitre, et « LE F. » : petites capitales de l'imprimé, rendues en capitales par la transcription.
- Le crédit au « Projet Gutenberg » dans `publication.json` suit le schéma, alors que le texte est diffusé sans la licence Gutenberg. À faire confirmer par l'agent 12 (marque « Project Gutenberg »).

## Corrections du latin

Copie de `corrections-la.tsv`, dans l'ordre du texte. Témoins : ws = la.wikisource ; 1840, 1867, 1888 = éditions d'archive.org.

<!-- corrections:début -->
| Bloc | Lieu | Avant | Après | n | Motif ; témoins |
|---|---|---|---|---|---|
| `l1.c1.v2` | I, 1, 2 | `Chrisi` | `Christi` | 1 | lettre omise ; ws, 1840, 1867, 1888 |
| `l1.c1.v3` | I, 1, 3 | `fervire` | `servire` | 1 | s long lu f ; ws, 1840, 1888 |
| `l1.c1.v5` | I, 1, 5 | `invisiblia` | `invisibilia` | 1 | lettre omise ; 1840, 1888 |
| `l1.c2.v1` | I, 2, 1 | `confiderat` | `considerat` | 1 | s long lu f ; ws, 1888 |
| `l1.c2.v1` | I, 2, 1 | `juavert` | `juvaret` | 1 | mot mal lu ; ws, 1840, 1888 |
| `l1.c2.v1` | I, 2, 1 | `fervit` | `servit` | 1 | s long lu f ; 1888 |
| `l1.c2.v3` | I, 2, 3 | `nesci` | `nescis` | 1 | lettre omise ; ws, 1840 |
| `l1.c2.v4` | I, 2, 4 | `exstimare` | `æstimare` | 1 | æ lu « ex » ; 1840, 1888 |
| `l1.c2.v4` | I, 2, 4 | `ipius` | `ipsius` | 1 | lettre omise ; 1840, 1888 |
| `l1.c3.v1` | I, 3, 1 | `opinion` | `opinio` | 1 | lettre en trop ; ws, 1867, 1888 |
| `l1.c3.v1` | I, 3, 1 | `argueur` | `arguemur` | 1 | lettre omise ; ws, 1840, 1867 |
| `l1.c3.v2` | I, 3, 2 | `Cuit` | `Cui` | 1 | lettre en trop ; ws, 1840, 1888 |
| `l1.c3.v2` | I, 3, 2 | `t` | `et` | 1 | lettre omise ; ws, 1840 |
| `l1.c3.v2` | I, 3, 2 | `toum` | `totum` | 1 | lettre omise ; ws |
| `l1.c3.v2` | I, 3, 2 | `omnia unam sunt` | `omnia unum sunt` | 1 | « u » lu « a » ; 1840, 1888 |
| `l1.c3.v4` | I, 3, 4 | `confiderata` | `considerata` | 1 | s long lu f ; ws, 1888 |
| `l1.c3.v4` | I, 3, 4 | `eet` | `et` | 1 | lettre en trop ; ws, 1888 |
| `l1.c3.v5` | I, 3, 5 | `e` | `die` | 1 | mot mal lu ; ws, 1840, 1888 |
| `l1.c3.v5` | I, 3, 5 | `visimus` | `viximus` | 1 | « x » lu « s » ; ws, 1840 |
| `l1.c3.v5` | I, 3, 5 | `inferendas` | `inserendas` | 1 | s long lu f ; 1840, 1888 |
| `l1.c3.v6` | I, 3, 6 | `fervitio` | `servitio` | 1 | s long lu f ; ws |
| `l1.c3.v6` | I, 3, 6 | `cognitationibus` | `cogitationibus` | 1 | lettre en trop ; 1840, 1888 |
| `l1.c4.v1` | I, 4, 1 | `fecundum` | `secundum` | 1 | s long lu f ; ws, 1867 |
| `l1.c4.v1` | I, 4, 1 | `enarrant` | `enarranti` | 1 | lettre omise ; ws, 1840, 1888 |
| `l1.c4.v1` | I, 4, 1 | `fatis` | `satis` | 1 | s long lu f |
| `l1.c5.v2` | I, 5, 2 | `cause` | `causa` | 1 | « a » lu « e » ; ws, 1888 |
| `l1.c6.v2` | I, 6, 2 | `profecutus` | `prosecutus` | 1 | s long lu f ; ws, 1840 |
| `l1.c9.v3` | I, 9, 3 | `bonum fit` | `bonum sit` | 1 | s long lu f ; 1840, 1888 |
| `l1.c10.v1` | I, 10, 1 | `fæcularium` | `sæcularium` | 1 | s long lu f ; ws, 1840 |
| `l1.c11.v1` | I, 11, 1 | `die` | `diu` | 1 | « u » lu « e » ; ws |
| `l1.c11.v1` | I, 11, 1 | `intrinsecur` | `intrinsecus` | 1 | « s » lu « r » |
| `l1.c11.v3` | I, 11, 3 | `dijicimur` | `dejicimur` | 1 | « e » lu « i » ; ws |
| `l1.c11.v3` | I, 11, 3 | `occurit` | `occurrit` | 1 | lettre omise |
| `l1.c11.v4` | I, 11, 4 | `Dominii` | `Domini` | 1 | lettre en trop |
| `l1.c11.v5` | I, 11, 5 | `prosessionis` | `professionis` | 1 | f lu s long ; ws, 1840, 1888 |
| `l1.c11.v6` | I, 11, 6 | `inclinatini` | `inclinationi` | 1 | lettre omise ; ws, 1840, 1888 |
| `l1.c11.v6` | I, 11, 6 | `defere` | `desere` | 1 | s long lu f ; ws |
| `l1.c13.v3` | I, 13, 3 | `recedene` | `recedente` | 1 | lettre omise ; ws, 1840, 1888 |
| `l1.c13.v3` | I, 13, 3 | `hositbus` | `hostibus` | 1 | lettres interverties ; ws, 1840, 1888 |
| `l1.c13.v5` | I, 13, 5 | `finitur` | `sinitur` | 1 | s long lu f ; 1840, 1888 |
| `l1.c13.v5` | I, 13, 5 | `pricipio` | `principio` | 1 | lettre omise |
| `l1.c13.v6` | I, 13, 6 | `fatis` | `satis` | 1 | s long lu f |
| `l1.c13.v7` | I, 13, 7 | `exultabit` | `exaltabit` | 1 | « a » lu « u » ; ws |
| `l1.c13.v8` | I, 13, 8 | `devotus fit` | `devotus sit` | 1 | s long lu f ; 1840, 1888 |
| `l1.c14.v2` | I, 14, 2 | `etaim` | `etiam` | 1 | lettres interverties ; ws, 1888 |
| `l1.c14.v2` | I, 14, 2 | `diffensiones` | `dissensiones` | 1 | s long lu f ; ws, 1840, 1888 |
| `l1.c14.v3` | I, 14, 3 | `trade` | `tarde` | 1 | lettres interverties ; ws |
| `l1.c14.v3` | I, 14, 3 | `subjicii` | `subjici` | 1 | lettre en trop |
| `l1.c15.v2` | I, 15, 2 | `fervit` | `servit` | 1 | s long lu f |
| `l1.c17.v1` | I, 17, 1 | `visit` | `vixit` | 1 | « x » lu « s » ; ws |
| `l1.c17.v2` | I, 17, 2 | `passionem` | `passionum` | 1 | « u » lu « e » ; ws, 1888 |
| `l1.c18.v1` | I, 18, 1 | `fatigatine` | `fatigatione` | 1 | lettre omise ; ws |
| `l1.c18.v2` | I, 18, 2 | `Virginies` | `Virgines` | 1 | lettre en trop ; ws, 1840, 1888 |
| `l1.c18.v4` | I, 18, 4 | `dispecti` | `despecti` | 1 | « e » lu « i » ; ws, 1888 |
| `l1.c19.v1` | I, 19, 1 | `fervitio` | `servitio` | 1 | s long lu f ; ws, 1840, 1888 |
| `l1.c19.v2` | I, 19, 2 | `defertio` | `desertio` | 1 | s long lu f ; ws, 1840, 1888 |
| `l1.c19.v4` | I, 19, 4 | `inclinatinem` | `inclinationem` | 1 | lettre omise ; ws, 1888 |
| `l1.c19.v4` | I, 19, 4 | `offendisti ex proximum` | `offendisti et proximum` | 1 | « t » lu « x » |
| `l1.c20.v1` | I, 20, 1 | `remoribus` | `rumoribus` | 1 | « u » lu « e » |
| `l1.c20.v2` | I, 20, 2 | `conscienitæ` | `conscientiæ` | 1 | lettres interverties ; ws |
| `l1.c20.v8` | I, 20, 8 | `quod die potest` | `quod diu potest` | 1 | « u » lu « e » |
| `l1.c21.v2` | I, 21, 2 | `nosrorum` | `nostrorum` | 1 | lettre omise ; ws, 1840, 1888 |
| `l1.c21.v2` | I, 21, 2 | `senstimus` | `sentimus` | 1 | lettre en trop ; ws, 1888 |
| `l1.c21.v2` | I, 21, 2 | `quandomerito` | `quando merito` | 1 | mots soudés ; ws |
| `l1.c21.v2` | I, 21, 2 | `homies` | `homines` | 1 | lettre omise ; ws, 1840, 1888 |
| `l1.c21.v3` | I, 21, 3 | `secudum` | `secundum` | 1 | lettre omise ; ws, 1840, 1888 |
| `l1.c21.v4` | I, 21, 4 | `cælistia` | `cælestia` | 1 | « e » lu « i » ; ws, 1888 |
| `l1.c21.v5` | I, 21, 5 | `vitæ tua cogitares` | `vitæ tuæ cogitares` | 1 | accord (lettre omise) ; ws |
| `l1.c21.v5` | I, 21, 5 | `Pergatorii` | `Purgatorii` | 1 | « u » lu « e » ; ws |
| `l1.c21.v5` | I, 21, 5 | `sivi` | `sive` | 1 | « e » lu « i » |
| `l1.c22.v2` | I, 22, 2 | `præfens` | `præsens` | 1 | s long lu f ; ws, 1840, 1888 |
| `l1.c22.v3` | I, 22, 3 | `ocrporalibus` | `corporalibus` | 1 | lettres interverties ; ws, 1840, 1888 |
| `l1.c22.v4` | I, 22, 4 | `ætena` | `æterna` | 1 | lettre omise ; ws, 1840, 1888 |
| `l1.c22.v5` | I, 22, 5 | `venia` | `venias` | 1 | lettre omise ; ws |
| `l1.c22.v5` | I, 22, 5 | `abforbeatur` | `absorbeatur` | 1 | s long lu f ; ws, 1840, 1888 |
| `l1.c22.v7` | I, 22, 7 | `futurea` | `futura` | 1 | lettre en trop ; ws |
| `l1.c23.v1` | I, 23, 1 | `fugure` | `fugere` | 1 | « e » lu « u » |
| `l1.c23.v3` | I, 23, 3 | `inveniant` | `inveniat` | 1 | lettre en trop ; ws |
| `l1.c23.v6` | I, 23, 6 | `moris` | `mortis` | 1 | lettre omise ; ws, 1840, 1867, 1888 |
| `l1.c24.v1` | I, 24, 1 | `escusationes` | `excusationes` | 1 | « x » lu « s » ; ws, 1840, 1888 |
| `l1.c24.v1` | I, 24, 1 | `omniamala` | `omnia mala` | 1 | mots soudés ; ws, 1888 |
| `l1.c24.v1` | I, 24, 1 | `sisi` | `sibi` | 1 | « b » lu « s » ; ws |
| `l1.c24.v1` | I, 24, 1 | `acceptabilies` | `acceptabilis` | 1 | lettre en trop ; ws, 1840, 1888 |
| `l1.c24.v3` | I, 24, 3 | `purgentur` | `pungentur` | 1 | « n » lu « r » ; 1867, 1888 |
| `l1.c24.v4` | I, 24, 4 | `humlis` | `humilis` | 1 | lettre omise ; ws |
| `l1.c24.v5` | I, 24, 5 | `merebit` | `mærebit` | 1 | ligature perdue ; ws |
| `l1.c24.v5` | I, 24, 5 | `religiosus` | `irreligiosus` | 1 | mot mal lu ; ws, 1840, 1888 |
| `l1.c24.v6` | I, 24, 6 | `fervato` | `servato` | 1 | s long lu f ; ws, 1840, 1888 |
| `l1.c24.v6` | I, 24, 6 | `sancta sancta opera` | `sancta opera` | 1 | dittographie ; ws |
| `l1.c24.v6` | I, 24, 6 | `ardus poenitntia` | `ardua poenitentia` | 1 | mot mal lu ; ws |
| `l1.c24.v6` | I, 24, 6 | `es` | `et` | 1 | « t » lu « s » ; ws, 1840, 1888 |
| `l1.c24.v6` | I, 24, 6 | `facietur` | `faciet` | 1 | lettres en trop ; 1840, 1888 |
| `l1.c24.v7` | I, 24, 7 | `foli` | `soli` | 1 | s long lu f ; ws, 1840, 1888 |
| `l1.c24.v7` | I, 24, 7 | `judcium` | `judicium` | 1 | lettre omise ; ws |
| `l1.c24.v7` | I, 24, 7 | `gehenæ` | `gehennæ` | 1 | lettre omise ; ws |
| `l1.c25.v1` | I, 25, 1 | `fervitio` | `servitio` | 1 | s long lu f ; ws |
| `l1.c25.v2` | I, 25, 2 | `perserveratus` | `perseveraturus` | 1 | lettres interverties ; 1840, 1888 ; ws |
| `l1.c25.v5` | I, 25, 5 | `confideraveris` | `consideraveris` | 1 | s long lu f ; ws, 1840, 1888 |
| `l1.c25.v5` | I, 25, 5 | `confiderat` | `considerat` | 1 | s long lu f ; ws, 1840, 1888 |
| `l1.c25.v5` | I, 25, 5 | `disciiplinatos` | `disciplinatos` | 1 | lettre en trop ; ws |
| `l1.c25.v5` | I, 25, 5 | `comissia` | `commissa` | 1 | lettres mal placées ; 1840, 1888 ; ws |
| `l1.c25.v8` | I, 25, 8 | `execunt` | `exeunt` | 1 | lettre en trop ; ws |
| `l1.c25.v9` | I, 25, 9 | `tot` | `toto` | 1 | lettre omise ; ws, 1840, 1888 |
| `l1.c25.v10` | I, 25, 10 | `parit` | `perit` | 1 | « e » lu « a » ; ws, 1840, 1888 |
| `l2.c1` | II, 1, titre | `conversationes` | `conversatione` | 1 | lettre en trop |
| `l2.c1.v1` | II, 1, 1 | `consolatinem` | `consolationem` | 1 | lettre omise ; ws, 1840, 1888 |
| `l2.c1.v2` | II, 1, 2 | `sparare` | `sperare` | 1 | « e » lu « a » ; ws, 1840, 1888 |
| `l2.c1.v2` | II, 1, 2 | `ut non fit opus` | `ut non sit opus` | 1 | s long lu f ; 1840, 1888 |
| `l2.c1.v3` | II, 1, 3 | `Nonhabes` | `Non habes` | 1 | mots soudés ; ws, 1840, 1888 |
| `l2.c1.v3` | II, 1, 3 | `utilis fit` | `utilis sit` | 1 | s long lu f ; 1840, 1888 |
| `l2.c1.v4` | II, 1, 4 | `requietonis` | `requietionis` | 1 | lettre omise ; ws |
| `l2.c1.v5` | II, 1, 5 | `patentia` | `patientia` | 1 | lettre omise ; ws, 1840, 1888 |
| `l2.c1.v5` | II, 1, 5 | `regare` | `regnare` | 1 | lettre omise |
| `l2.c1.v7` | II, 1, 7 | `reqirit` | `requirit` | 1 | lettre omise ; ws, 1840, 1888 |
| `l2.c1.v8` | II, 1, 8 | `recte tibi esses` | `recte tibi esset` | 1 | « t » lu « s » ; 1840, 1888 |
| `l2.c2.v1` | II, 2, 1 | `sci` | `scis` | 1 | lettre omise ; ws, 1840 |
| `l2.c2.v1` | II, 2, 1 | `idea` | `ideo` | 1 | « o » lu « a » ; ws, 1867, 1888 |
| `l2.c2.v1` | II, 2, 1 | `contra te fit` | `contra te sit` | 1 | s long lu f ; 1840, 1888 |
| `l2.c3.v2` | II, 3, 2 | `nuli` | `nulli` | 1 | lettre omise ; ws, 1840, 1888 |
| `l2.c3.v2` | II, 3, 2 | `indignarei` | `indignari` | 1 | lettre en trop ; ws, 1888 |
| `l2.c3.v3` | II, 3, 3 | `fui` | `sui` | 1 | s long lu f ; ws, 1840, 1888 |
| `l2.c5.v3` | II, 5, 3 | `seriatum` | `feriatum` | 1 | f lu s long ; ws, 1840, 1888 |
| `l2.c5.v3` | II, 5, 3 | `exstima` | `existima` | 1 | lettre omise ; 1840, 1888 |
| `l2.c6.v3` | II, 6, 3 | `confiderat` | `considerat` | 1 | s long lu f |
| `l2.c7.v1` | II, 7, 1 | `propeter` | `propter` | 1 | lettre en trop ; ws, 1840, 1888 |
| `l2.c7.v2` | II, 7, 2 | `fedelitati` | `fidelitati` | 1 | « i » lu « e » ; ws, 1840, 1888 |
| `l2.c7.v2` | II, 7, 2 | `potestte` | `potest te` | 1 | mots soudés ; ws |
| `l2.c8.v1` | II, 8, 1 | `totum durum est.` | `totum durum est. Quando Jesus intus non loquitur, consolatio vilis est.` | 1 | phrase omise (saut du même au même) ; ws |
| `l2.c8.v1` | II, 8, 1 | `autuem` | `autem` | 1 | lettre en trop ; ws, 1840, 1888 |
| `l2.c8.v1` | II, 8, 1 | `surreit` | `surrexit` | 1 | lettre omise ; ws, 1840, 1888 |
| `l2.c8.v1` | II, 8, 1 | `adet` | `adest` | 1 | lettre omise ; ws, 1840 |
| `l2.c8.v5` | II, 8, 5 | `nectem` | `noctem` | 1 | « o » lu « e » |
| `l2.c9.v1` | II, 9, 1 | `hilaris si,` | `hilaris sis,` | 1 | lettre omise ; ws |
| `l2.c9.v2` | II, 9, 2 | `Sixtem` | `Sixtum` | 1 | « u » lu « e » ; ws, 1840, 1888 |
| `l2.c9.v4` | II, 9, 4 | `suit` | `fuit` | 1 | f lu s long ; ws, 1840 |
| `l2.c9.v5` | II, 9, 5 | `iluculo` | `diluculo` | 1 | lettre omise ; ws, 1840, 1888 |
| `l2.c9.v5` | II, 9, 5 | `deprocabor` | `deprecabor` | 1 | « e » lu « o » |
| `l2.c9.v6` | II, 9, 6 | `defertus` | `desertus` | 1 | s long lu f |
| `l2.c10` | II, 10, titre | `progratia` | `pro gratia` | 1 | mots soudés |
| `l2.c10.v1` | II, 10, 1 | `consoltionem` | `consolationem` | 1 | lettre omise ; ws |
| `l2.c10.v1` | II, 10, 1 | `spiritualiem` | `spiritualem` | 1 | lettre en trop ; ws, 1840, 1888 |
| `l2.c10.v1` | II, 10, 1 | `divnis` | `divinis` | 1 | lettre omise ; ws, 1840, 1888 |
| `l2.c10.v2` | II, 10, 2 | `consolatinis` | `consolationis` | 1 | lettre omise ; ws |
| `l2.c10.v2` | II, 10, 2 | `salsa` | `falsa` | 1 | f lu s long ; 1840, 1888 |
| `l2.c11.v1` | II, 11, 1 | `as` | `ad` | 1 | « d » lu « s » ; ws, 1888 |
| `l2.c11.v3` | II, 11, 3 | `propio` | `proprio` | 1 | lettre omise |
| `l2.c11.v5` | II, 11, 5 | `extimari` | `æstimari` | 1 | æ lu « ex » ; d'après l'usage de l'édition |
| `l2.c11.v5` | II, 11, 5 | `fervum` | `servum` | 1 | s long lu f ; ws, 1840 |
| `l2.c12.v1` | II, 12, 1 | `Abnego` | `Abnega` | 1 | « a » lu « o » |
| `l2.c12.v2` | II, 12, 2 | `pœ` | `pœnæ` | 1 | mot mal lu ; ws, 1888 |
| `l2.c12.v4` | II, 12, 4 | `necese` | `necesse` | 1 | lettre omise ; ws |
| `l2.c12.v6` | II, 12, 6 | `Opertebat` | `Oportebat` | 1 | « o » lu « e » |
| `l2.c12.v7` | II, 12, 7 | `reqiuem` | `requiem` | 1 | lettres interverties ; ws, 1840, 1867, 1888 |
| `l2.c12.v7` | II, 12, 7 | `gravioes` | `graviores` | 1 | lettre omise ; ws |
| `l2.c12.v8` | II, 12, 8 | `lavamine` | `levamine` | 1 | « e » lu « a » ; ws, 1840, 1867, 1888 |
| `l2.c12.v9` | II, 12, 9 | `quæquæ` | `quæque` | 1 | « e » lu « æ » |
| `l2.c12.v10` | II, 12, 10 | `tribulatones` | `tribulationes` | 1 | lettre omise |
| `l2.c12.v11` | II, 12, 11 | `exstima` | `æstima` | 1 | æ lu « ex » ; ws, 1840 |
| `l2.c12.v13` | II, 12, 13 | `aliqud` | `aliquid` | 1 | lettre omise |
| `l3.titre` | III, titre du livre | `consolatine` | `consolatione` | 1 | lettre omise |
| `l3.c1` | III, 1, titre | `consolatine` | `consolatione` | 1 | lettre omise |
| `l3.c2.v1` | III, 2, 1 | `popheta` | `propheta` | 1 | lettre omise ; ws, 1840, 1888 |
| `l3.c2.v1` | III, 2, 1 | `otius` | `potius` | 1 | lettre omise ; ws |
| `l3.c2.v1` | III, 2, 1 | `humiliter ad disideranter` | `humiliter ac desideranter` | 1 | « c » lu « d », « e » lu « i » ; 1840, 1888 |
| `l3.c2.v1` | III, 2, 1 | `servus suus` | `servus tuus` | 1 | « t » lu « s » ; 1840, 1888 |
| `l3.c2.v2` | III, 2, 2 | `accedunt` | `accendunt` | 1 | lettre omise ; 1840, 1888 |
| `l3.c3.v1` | III, 3, 1 | `sunsu` | `sensu` | 1 | « e » lu « u » ; ws, 1888 |
| `l3.c3.v1` | III, 3, 1 | `scientam` | `scientiam` | 1 | lettre omise |
| `l3.c3.v3` | III, 3, 3 | `dix` | `dixi` | 1 | lettre omise ; ws, 1840, 1867, 1888 |
| `l3.c3.v3` | III, 3, 3 | `confitentem` | `confidentem` | 1 | « d » lu « t » ; 1840, 1888 |
| `l3.c3.v4` | III, 3, 4 | `vittia` | `vitia` | 1 | lettre en trop ; ws, 1840, 1867, 1888 |
| `l3.c3.h1` | III, 3, intertitre | `imporandum` | `implorandum` | 1 | lettre omise ; ws |
| `l3.c3.v5` | III, 3, 5 | `ceum` | `meum` | 1 | « m » lu « c » ; ws |
| `l3.c3.v6` | III, 3, 6 | `noli consolationem tuam prolongare, noli consolationem tuam abstrahere` | `noli consolationem tuam abstrahere` | 1 | dittographie ; ws |
| `l3.c4.v1` | III, 4, 1 | `at` | `et` | 1 | « e » lu « a » ; ws |
| `l3.c4.v2` | III, 4, 2 | `nuquam` | `nunquam` | 1 | lettre omise ; ws, 1840 |
| `l3.c4.v3` | III, 4, 3 | `curiositte` | `curiositate` | 1 | lettre omise ; ws, 1840, 1867, 1888 |
| `l3.c4.v3` | III, 4, 3 | `arrogania` | `arrogantia` | 1 | lettre omise ; ws, 1840, 1867, 1888 |
| `l3.c4.v3` | III, 4, 3 | `temeas` | `timeas` | 1 | « i » lu « e » |
| `l3.c4.v4` | III, 4, 4 | `sempe` | `semper` | 1 | lettre omise ; ws, 1840, 1867, 1888 |
| `l3.c5.v2` | III, 5, 2 | `impefectus` | `imperfectus` | 1 | lettre omise |
| `l3.c6.v4` | III, 6, 4 | `vade` | `valde` | 1 | lettre omise ; ws, 1840, 1888 |
| `l3.c6.v4` | III, 6, 4 | `stabilis` | `stabis` | 1 | mot mal lu ; ws, 1840, 1888 |
| `l3.c6.v5` | III, 6, 5 | `cecitatem` | `cæcitatem` | 1 | « æ » lu « e » ; ws, 1840, 1888 |
| `l3.c7.v3` | III, 7, 3 | `osses` | `posses` | 1 | lettre omise ; ws, 1840, 1888 |
| `l3.c7.v3` | III, 7, 3 | `magi` | `magni` | 1 | lettre omise ; 1840, 1888 |
| `l3.c7.v5` | III, 7, 5 | `recognita` | `recogita` | 1 | lettre en trop ; ws |
| `l3.c7.v5` | III, 7, 5 | `posses` | `posse` | 1 | lettre en trop ; ws, 1840, 1888 |
| `l3.c7.v5` | III, 7, 5 | `extimanda` | `æstimanda` | 1 | æ lu « ex » ; 1888 |
| `l3.c8` | III, 8, titre | `exstimatione` | `æstimatione` | 1 | æ lu « ex » ; 1840, 1888 |
| `l3.c8.v1` | III, 8, 1 | `at` | `ad` | 1 | « d » lu « t » ; ws, 1840 |
| `l3.c8.v1` | III, 8, 1 | `exstimatio` | `æstimatio` | 1 | æ lu « ex » ; 1888 |
| `l3.c8.v1` | III, 8, 1 | `spulverizavero` | `pulverizavero` | 1 | lettre en trop |
| `l3.c8.v1` | III, 8, 1 | `Se mihi` | `Si mihi` | 1 | « i » lu « e » ; 1840, 1888 |
| `l3.c8.v2` | III, 8, 2 | `ver` | `vere` | 1 | lettre omise ; ws, 1840, 1888 |
| `l3.c9.v3` | III, 9, 3 | `neqe` | `neque` | 1 | lettre omise ; ws, 1888 |
| `l3.c10.v1` | III, 10, 1 | `cum no essem` | `cum non essem` | 1 | lettre omise |
| `l3.c10.v3` | III, 10, 3 | `ispum` | `ipsum` | 1 | lettres interverties ; ws, 1840, 1888 |
| `l3.c11.v2` | III, 11, 2 | `incurra` | `incurras` | 1 | lettre omise |
| `l3.c11.v3` | III, 11, 3 | `fatagere` | `satagere` | 1 | s long lu f ; ws |
| `l3.c11.v3` | III, 11, 3 | `inonveniens` | `inconveniens` | 1 | lettre omise ; ws, 1888 |
| `l3.c12.v1` | III, 12, 1 | `extimare` | `æstimare` | 1 | æ lu « ex » ; d'après l'usage de l'édition |
| `l3.c12.v3` | III, 12, 3 | `sumus` | `fumus` | 1 | f lu s long ; ws, 1840, 1888 |
| `l3.c13.v2` | III, 13, 2 | `dice` | `disce` | 1 | lettre omise ; ws, 1840, 1888 |
| `l3.c13.v2` | III, 13, 2 | `quando eo Omnipotens` | `quando ego Omnipotens` | 1 | lettre omise ; 1840, 1888 |
| `l3.c14.v1` | III, 14, 1 | `epercisti` | `pepercisti` | 1 | lettre omise ; ws, 1840, 1888 |
| `l3.c14.v1` | III, 14, 1 | `opra` | `opera` | 1 | lettre omise ; ws, 1888 |
| `l3.c14.v3` | III, 14, 3 | `instransnatabile` | `intransnatabile` | 1 | lettre en trop ; ws, 1840, 1867, 1888 |
| `l3.c14.v4` | III, 14, 4 | `omni` | `omnis` | 1 | lettre omise ; ws, 1840, 1867, 1888 |
| `l3.c16.v1` | III, 16, 1 | `durare no possent` | `durare non possent` | 1 | lettre omise |
| `l3.c18.v2` | III, 18, 2 | `consolatioria` | `consolatoria` | 1 | lettre en trop ; ws |
| `l3.c18.v3` | III, 18, 3 | `cinctis` | `cunctis` | 1 | « u » lu « i » ; ws, 1840, 1888 |
| `l3.c18.v3` | III, 18, 3 | `patietiam` | `patientiam` | 1 | lettre omise ; ws, 1840, 1888 |
| `l3.c18.v3` | III, 18, 3 | `retrocenderent` | `retrocederent` | 1 | lettre en trop ; ws |
| `l3.c19.v2` | III, 19, 2 | `dica` | `dicas` | 1 | lettre omise ; ws |
| `l3.c19.v5` | III, 19, 5 | `impossibiel` | `impossibile` | 1 | lettres interverties ; ws, 1840, 1888 |
| `l3.c20.v1` | III, 20, 1 | `Præpono` | `Propono` | 1 | mot mal lu ; 1888 |
| `l3.c20.v2` | III, 20, 2 | `infectatio` | `insectatio` | 1 | s long lu f ; ws, 1840, 1888 |
| `l3.c20.v2` | III, 20, 2 | `irrunt` | `irruunt` | 1 | lettre omise ; ws, 1840, 1888 |
| `l3.c20.v4` | III, 20, 4 | `tædiunt` | `tædium` | 1 | mot mal lu (sens) |
| `l3.c20.v5` | III, 20, 5 | `qutem` | `autem` | 1 | « a » lu « q » ; ws, 1840, 1867, 1888 |
| `l3.c21.v5` | III, 21, 5 | `nihi` | `mihi` | 1 | « m » lu « n » ; ws |
| `l3.c21.v7` | III, 21, 7 | `ma` | `mea` | 1 | lettre omise ; ws, 1840, 1867, 1888 |
| `l3.c22.v2` | III, 22, 2 | `cunta` | `cuncta` | 1 | lettre omise ; ws, 1840, 1888 |
| `l3.c22.v2` | III, 22, 2 | `esistimat` | `existimat` | 1 | « x » lu « s » ; ws |
| `l3.c22.v3` | III, 22, 3 | `aud` | `apud` | 1 | lettre omise ; ws, 1840, 1888 |
| `l3.c22.v4` | III, 22, 4 | `contemelias` | `contumelias` | 1 | « u » lu « e » |
| `l3.c23` | III, 23, titre | `4.or.` | `quatuor` | 1 | abréviation « 4.or. » développée ; 1840, 1888 |
| `l3.c23.v1` | III, 23, 1 | `decebo` | `docebo` | 1 | « o » lu « e » ; ws, 1840, 1888 |
| `l3.c23.v4` | III, 23, 4 | `origi` | `oriri` | 1 | « r » lu « g » ; ws |
| `l3.c23.v4` | III, 23, 4 | `amjorem` | `majorem` | 1 | lettres interverties ; ws, 1840, 1867, 1888 |
| `l3.c23.v4` | III, 23, 4 | `posim` | `possim` | 1 | lettre omise ; ws, 1840, 1867 |
| `l3.c23.v8` | III, 23, 8 | `lumnis` | `luminis` | 1 | lettre omise ; ws |
| `l3.c25.v2` | III, 25, 2 | `perfictio` | `perfectio` | 1 | « e » lu « i » ; ws, 1840, 1888 |
| `l3.c25.v2` | III, 25, 2 | `exstimes` | `æstimes` | 1 | æ lu « ex » ; 1840, 1888 |
| `l3.c25.v2` | III, 25, 2 | `congnoscitur` | `cognoscitur` | 1 | lettre en trop |
| `l3.c25.v3` | III, 25, 3 | `peveneris` | `perveneris` | 1 | lettre omise ; ws, 1888 |
| `l3.c26.v1` | III, 26, 1 | `adhærando` | `adhærendo` | 1 | « e » lu « a » |
| `l3.c26.v3` | III, 26, 3 | `snaguis` | `sanguis` | 1 | lettres interverties ; ws |
| `l3.c26.v3` | III, 26, 3 | `suplantet` | `supplantet` | 1 | lettre omise ; ws, 1840, 1888 |
| `l3.c26.v3` | III, 26, 3 | `amoe` | `amore` | 1 | lettre omise ; ws, 1840, 1888 |
| `l3.c26.v4` | III, 26, 4 | `pertenentia` | `pertinentia` | 1 | « i » lu « e » ; ws, 1840, 1888 |
| `l3.c26.v4` | III, 26, 4 | `serventi` | `ferventi` | 1 | f lu s long ; ws, 1840, 1888 |
| `l3.c26.v4` | III, 26, 4 | `auem` | `autem` | 1 | lettre omise ; ws, 1840, 1888 |
| `l3.c29.v2` | III, 29, 2 | `omniotens` | `omnipotens` | 1 | lettre omise ; ws, 1840, 1888 |
| `l3.c29.v2` | III, 29, 2 | `difficuliu` | `difficilius` | 1 | lettres mal lues ; 1840, 1888 ; ws |
| `l3.c30.v1` | III, 30, 1 | `tribulatoinis` | `tribulationis` | 1 | lettres interverties ; ws |
| `l3.c30.v1` | III, 30, 1 | `fueris` | `fuerit` | 1 | « t » lu « s » ; ws |
| `l3.c30.v1` | III, 30, 1 | `restaruem` | `restaurem` | 1 | lettres interverties ; ws |
| `l3.c30.v3` | III, 30, 3 | `exstimas` | `æstimas` | 2 | æ lu « ex » ; d'après l'usage de l'édition |
| `l3.c30.v6` | III, 30, 6 | `dilexet` | `dilexit` | 1 | « i » lu « e » ; ws, 1840, 1867 |
| `l3.c31.v1` | III, 31, 1 | `indegeo` | `indigeo` | 1 | « i » lu « e » ; ws |
| `l3.c31.v4` | III, 31, 4 | `satim` | `statim` | 1 | lettre omise ; ws, 1888 |
| `l3.c31.v4` | III, 31, 4 | `corruperate` | `corruperat` | 1 | lettre en trop ; ws, 1840 |
| `l3.c32.v1` | III, 32, 1 | `intelligis` | `intelliges` | 1 | « e » lu « i » ; ws |
| `l3.c32.v1` | III, 32, 1 | `quod non es ex Deo` | `quod non est ex Deo` | 1 | lettre omise ; 1840, 1888 |
| `l3.c32.v2` | III, 32, 2 | `conclluditur` | `concluditur` | 1 | lettre en trop ; ws |
| `l3.c32.v2` | III, 32, 2 | `Religioforum` | `Religiosorum` | 1 | s long lu f ; ws |
| `l3.c32.v3` | III, 32, 3 | `conculacantem` | `conculcantem` | 1 | lettre en trop ; ws |
| `l3.c33.v2` | III, 33, 2 | `Sed multis caligat` | `Sed in multis caligat` | 1 | mot omis ; ws |
| `l3.c33.v2` | III, 33, 2 | `oculos` | `oculus` | 1 | « u » lu « o » ; ws, 1840, 1888 |
| `l3.c34.v1` | III, 34, 1 | `faci` | `facis` | 1 | lettre omise ; ws, 1840, 1867, 1888 |
| `l3.c34.v2` | III, 34, 2 | `dissimlis` | `dissimilis` | 1 | lettre omise ; ws, 1840, 1888 |
| `l3.c35.v2` | III, 35, 2 | `a` | `ad` | 1 | lettre omise ; ws, 1840, 1888 |
| `l3.c36.v1` | III, 36, 1 | `omibus` | `omnibus` | 1 | lettre omise ; ws |
| `l3.c36.v2` | III, 36, 2 | `cogitatantiam` | `cogitantium` | 1 | mot mal lu ; ws, 1867 |
| `l3.c36.v3` | III, 36, 3 | `querolosis` | `querulosis` | 1 | « u » lu « o » ; ws, 1840, 1867 |
| `l3.c36.v3` | III, 36, 3 | `succumbi` | `succumbere` | 1 | lettres omises ; 1867 |
| `l3.c37.v1` | III, 37, 1 | `File,` | `Fili,` | 1 | « i » lu « e » ; ws |
| `l3.c37.v3` | III, 37, 3 | `agi` | `agis` | 1 | lettre omise ; ws, 1840, 1888 |
| `l3.c37.v5` | III, 37, 5 | `niihil` | `nihil` | 1 | lettre en trop ; ws, 1888 |
| `l3.c38.v1` | III, 38, 1 | `in fortem ac` | `in sortem ac` | 1 | s long lu f ; 1840, 1888 |
| `l3.c38.v2` | III, 38, 2 | `oratiois` | `orationis` | 1 | lettre omise ; ws, 1840, 1888 |
| `l3.c39.v4` | III, 39, 4 | `hosti` | `hostis` | 1 | lettre omise ; ws, 1840, 1888 |
| `l3.c40.v1` | III, 40, 1 | `aud quid` | `aut quid` | 1 | « t » lu « d » |
| `l3.c40.v5` | III, 40, 5 | `infirmatibus` | `infirmitatibus` | 1 | lettres omises |
| `l3.c40.v6` | III, 40, 6 | `vertus` | `virtus` | 1 | « i » lu « e » ; ws, 1888 |
| `l3.c42.v1` | III, 42, 1 | `verba` | `vera` | 1 | lettre en trop ; ws |
| `l3.c42.v2` | III, 42, 2 | `evacurare` | `evacuare` | 1 | lettre en trop ; ws, 1840 |
| `l3.c42.v2` | III, 42, 2 | `Creatoriem` | `Creatorem` | 1 | lettre en trop ; ws, 1888 |
| `l3.c43.v1` | III, 43, 1 | `cordo` | `corda` | 1 | « a » lu « o » ; 1840, 1888 |
| `l3.c43.v4` | III, 43, 4 | `cognitationum` | `cogitationum` | 1 | lettre en trop ; ws, 1888 |
| `l3.c45.v1` | III, 45, 1 | `libi` | `ibi` | 1 | lettre en trop ; ws, 1840, 1888 |
| `l3.c45.v3` | III, 45, 3 | `mesero` | `misero` | 1 | « i » lu « e » ; ws, 1840, 1888 |
| `l3.c45.v3` | III, 45, 3 | `multus` | `multis` | 1 | « i » lu « u » ; ws, 1840, 1888 |
| `l3.c45.v3` | III, 45, 3 | `quænon` | `quæ non` | 1 | mots soudés ; ws |
| `l3.c45.v4` | III, 45, 4 | `omnimodo` | `omnimode` | 1 | « e » lu « o » ; ws, 1840 |
| `l3.c45.v4` | III, 45, 4 | `calidam` | `callidam` | 1 | lettre omise ; 1840, 1888 |
| `l3.c45.v4` | III, 45, 4 | `incideam` | `incidam` | 1 | lettre en trop |
| `l3.c45.v5` | III, 45, 5 | `emendtionem` | `emendationem` | 1 | lettre omise ; ws, 1840 |
| `l3.c45.v5` | III, 45, 5 | `servorem` | `fervorem` | 1 | f lu s long ; ws |
| `l3.c46.v2` | III, 46, 2 | `malitionsissime` | `malitiosissime` | 1 | lettre en trop ; ws |
| `l3.c46.v2` | III, 46, 2 | `etrahere` | `extrahere` | 1 | lettre omise ; ws |
| `l3.c46.v3` | III, 46, 3 | `ocuis` | `oculis` | 1 | lettre omise ; ws |
| `l3.c46.v4` | III, 46, 4 | `salit` | `fallit` | 1 | mot mal lu ; ws, 1840, 1888 |
| `l3.c47.v4` | III, 47, 4 | `et fine fine` | `et sine fine` | 1 | f pour s long ; ws, 1867 |
| `l3.c48.v1` | III, 48, 1 | `Supernæ civitatis` | `O, supernæ civitatis` | 1 | lettrine « O » perdue ; ws |
| `l3.c48.v1` | III, 48, 1 | `peregrinanatibus` | `peregrinantibus` | 1 | lettre en trop ; ws, 1840, 1888 |
| `l3.c48.v3` | III, 48, 3 | `qundo` | `quando` | 1 | lettre omise ; ws |
| `l3.c48.v3` | III, 48, 3 | `præparasit` | `præparasti` | 1 | lettres interverties ; ws, 1840, 1888 |
| `l3.c48.v5` | III, 48, 5 | `safittas` | `sagittas` | 1 | « g » lu « f » ; ws, 1840 |
| `l3.c48.v5` | III, 48, 5 | `meo` | `meos` | 1 | lettre omise ; ws, 1888 |
| `l3.c48.v5` | III, 48, 5 | `occurit` | `occurrit` | 1 | lettre omise |
| `l3.c49.v1` | III, 49, 1 | `serventi` | `ferventi` | 1 | f lu s long ; ws |
| `l3.c49.v3` | III, 49, 3 | `temus` | `tempus` | 1 | lettre omise ; ws |
| `l3.c49.v3` | III, 49, 3 | `robatinis` | `probationis` | 1 | mot mal lu ; ws, 1840, 1888 |
| `l3.c49.v4` | III, 49, 4 | `Patent` | `Petent` | 1 | « e » lu « a » ; ws, 1840, 1888 |
| `l3.c49.v6` | III, 49, 6 | `fuctus` | `fructus` | 1 | lettre omise |
| `l3.c49.v6` | III, 49, 6 | `pœitentiæ` | `pœnitentiæ` | 1 | lettre omise |
| `l3.c49.v7` | III, 49, 7 | `humliter` | `humiliter` | 1 | lettre omise ; ws |
| `l3.c49.v7` | III, 49, 7 | `quia` | `quis` | 1 | « s » lu « a » ; ws, 1840, 1888 |
| `l3.c49.v7` | III, 49, 7 | `ve` | `vel` | 1 | lettre omise ; ws, 1840, 1888 |
| `l3.c49.v7` | III, 49, 7 | `au` | `aut` | 1 | lettre omise ; ws |
| `l3.c50` | III, 50, titre | `defolatus` | `desolatus` | 1 | s long lu f |
| `l3.c50.v2` | III, 50, 2 | `agudium` | `gaudium` | 1 | lettres interverties |
| `l3.c50.v4` | III, 50, 4 | `to` | `tuo` | 1 | lettre omise ; ws |
| `l3.c50.v4` | III, 50, 4 | `elatines` | `elationes` | 1 | lettre omise ; ws, 1840, 1888 |
| `l3.c50.v4` | III, 50, 4 | `michi` | `mihi` | 1 | mot mal lu ; 1840, 1888 |
| `l3.c50.v5` | III, 50, 5 | `quæsub` | `quæ sub` | 1 | mots soudés ; ws, 1888 |
| `l3.c50.v7` | III, 50, 7 | `secundam visionem` | `secundum visionem` | 1 | « u » lu « a » ; 1840, 1888 |
| `l3.c51.v1` | III, 51, 1 | `serventiori` | `ferventiori` | 1 | f lu s long ; ws, 1840, 1888 |
| `l3.c51.v1` | III, 51, 1 | `guod` | `quod` | 1 | « q » lu « g » ; ws, 1840 |
| `l3.c51.v1` | III, 51, 1 | `indefinenter` | `indesinenter` | 1 | s long lu f ; ws, 1888 |
| `l3.c51.v2` | III, 51, 2 | `incipas` | `incipias` | 1 | lettre omise ; ws, 1840 |
| `l3.c51.v2` | III, 51, 2 | `emorum` | `meorum` | 1 | lettres interverties ; ws, 1840, 1888 |
| `l3.c51.v2` | III, 51, 2 | `condignaæ` | `condignæ` | 1 | lettre en trop |
| `l3.c51.v2` | III, 51, 2 | `temproris` | `temporis` | 1 | lettre en trop |
| `l3.c52.v2` | III, 52, 2 | `commarari` | `commorari` | 1 | « o » lu « a » ; ws, 1888 |
| `l3.c52.v4` | III, 52, 4 | `incesum` | `incensum` | 1 | lettre omise ; ws, 1888 |
| `l3.c52.v4` | III, 52, 4 | `volusti` | `voluisti` | 1 | lettre omise ; ws, 1840, 1888 |
| `l3.c53.v1` | III, 53, 1 | `confabulatinem` | `confabulationem` | 1 | lettre omise ; ws, 1840, 1888 |
| `l3.c53.v1` | III, 53, 1 | `exstima` | `æstima` | 1 | æ lu « ex » ; 1840 |
| `l3.c53.v3` | III, 53, 3 | `occultam in inordinatam` | `occultam et inordinatam` | 1 | mot mal lu ; ws, 1888 |
| `l3.c53.v3` | III, 53, 3 | `inordinatam inclinationem ad te ipsum et ad omne privatum inclinationem ad te ipsum et ad omne privatum` | `inordinatam inclinationem ad te ipsum et ad omne privatum` | 1 | dittographie ; ws |
| `l3.c54.v1` | III, 54, 1 | `saluntur` | `falluntur` | 1 | mot mal lu ; ws |
| `l3.c54.v2` | III, 54, 2 | `decinat` | `declinat` | 1 | lettre omise ; ws, 1840, 1888 |
| `l3.c54.v3` | III, 54, 3 | `subesee` | `subesse` | 1 | « s » lu « e » ; ws |
| `l3.c54.v5` | III, 54, 5 | `honoem` | `honorem` | 1 | lettre omise ; ws, 1840, 1888 |
| `l3.c54.v6` | III, 54, 6 | `Grata autem` | `Gratia autem` | 1 | lettre omise |
| `l3.c54.v8` | III, 54, 8 | `Natural` | `Natura` | 1 | lettre en trop |
| `l3.c54.v12` | III, 54, 12 | `visiblia` | `visibilia` | 1 | lettre omise ; ws, 1840, 1888 |
| `l3.c54.v13` | III, 54, 13 | `otest` | `potest` | 1 | lettre omise ; ws, 1840, 1888 |
| `l3.c54.v13` | III, 54, 13 | `speart` | `sperat` | 1 | lettres interverties ; ws, 1840, 1888 |
| `l3.c54.v13` | III, 54, 13 | `affecutionem` | `assecutionem` | 1 | s long lu f ; ws, 1840, 1888 |
| `l3.c54.v17` | III, 54, 17 | `corruptonis` | `corruptionis` | 1 | lettre omise ; ws, 1840, 1867 |
| `l3.c54.v18` | III, 54, 18 | `formatur` | `reformatur` | 1 | mot mal lu ; ws |
| `l3.c55.v1` | III, 55, 1 | `mhi` | `mihi` | 1 | lettre omise ; ws |
| `l3.c55.v2` | III, 55, 2 | `pœ` | `pœna` | 1 | mot mal lu ; ws, 1840, 1888 |
| `l3.c55.v2` | III, 55, 2 | `affectionem suarum` | `affectionum suarum` | 1 | « u » lu « e » |
| `l3.c55.v3` | III, 55, 3 | `Hin` | `Hinc` | 1 | lettre omise ; ws, 1840, 1867, 1888 |
| `l3.c55.v4` | III, 55, 4 | `igneium` | `ingenium` | 1 | mot mal lu ; ws, 1840 |
| `l3.c55.v4` | III, 55, 4 | `habenter` | `habentur` | 1 | « u » lu « e » ; ws, 1840, 1888 |
| `l3.c55.v4` | III, 55, 4 | `naturaæ` | `naturæ` | 2 | lettre en trop (2 fois) |
| `l3.c55.v5` | III, 55, 5 | `desiert` | `desiderat` | 1 | mot mal lu ; ws, 1840, 1888 |
| `l3.c55.v5` | III, 55, 5 | `axilium` | `auxilium` | 1 | lettre omise ; ws, 1840, 1888 |
| `l3.c55.v6` | III, 55, 6 | `præbenitat` | `præveniat` | 1 | mot mal lu ; ws |
| `l3.c56.v1` | III, 56, 1 | `maseris` | `manseris` | 1 | lettre omise ; ws |
| `l3.c56.v1` | III, 56, 1 | `vitram` | `vitam` | 1 | lettre en trop ; ws, 1840 |
| `l3.c56.v2` | III, 56, 2 | `crecem` | `crucem` | 1 | « u » lu « e » ; ws, 1840, 1888 |
| `l3.c56.v3` | III, 56, 3 | `discipuus` | `discipulus` | 1 | lettre omise ; ws, 1840 |
| `l3.c56.v5` | III, 56, 5 | `perfeveremus` | `perseveremus` | 1 | s long lu f ; ws, 1840, 1888 |
| `l3.c57.v1` | III, 57, 1 | `ultmum` | `ultimum` | 1 | lettre omise ; ws |
| `l3.c57.v2` | III, 57, 2 | `diue` | `diu` | 1 | lettre en trop ; ws, 1840, 1888 |
| `l3.c57.v3` | III, 57, 3 | `provoco` | `proveho` | 1 | mot mal lu ; 1840, 1888 |
| `l3.c58.v1` | III, 58, 1 | `intellectua` | `intellectu` | 1 | lettre en trop ; ws |
| `l3.c58.v2` | III, 58, 2 | `præfferre` | `præferre` | 1 | lettre en trop ; ws, 1840, 1888 |
| `l3.c58.v2` | III, 58, 2 | `consisit` | `consistit` | 1 | lettre omise ; ws |
| `l3.c58.v3` | III, 58, 3 | `miscericordiam` | `misericordiam` | 1 | lettre en trop |
| `l3.c58.v5` | III, 58, 5 | `differere` | `disserere` | 1 | s long lu f (2 fois) ; 1840, 1888 |
| `l3.c58.v6` | III, 58, 6 | `cognitant` | `cogitant` | 1 | lettre en trop (douteux) |
| `l3.c58.v7` | III, 58, 7 | `humliaret` | `humiliaret` | 1 | lettre omise ; ws |
| `l3.c58.v7` | III, 58, 7 | `implicare` | `implorare` | 1 | mot mal lu ; 1840, 1888 |
| `l3.c59.v1` | III, 59, 1 | `peregrainari` | `peregrinari` | 1 | lettre en trop ; ws, 1840, 1867 |
| `l3.c59.v1` | III, 59, 1 | `considere` | `confidere` | 1 | f lu s long |
| `l3.c59.v3` | III, 59, 3 | `infirumum` | `infirmum` | 1 | lettre en trop |
| `l4.exhortation.p1` | IV, exhortation, préambule | `meam sanguinem` | `meum sanguinem` | 1 | « u » lu « a » ; 1840, 1888 |
| `l4.exhortation.p1` | IV, exhortation, préambule | `locutus sum nobis` | `locutus sum vobis` | 1 | « v » lu « n » ; 1840, 1888 |
| `l4.c1.v1` | IV, 1, 1 | `grantater` | `gratanter` | 1 | mot mal lu ; ws |
| `l4.c1.v1` | IV, 1, 1 | `dulcendinis` | `dulcedinis` | 1 | lettre en trop ; ws, 1840, 1867, 1888 |
| `l4.c1.v1` | IV, 1, 1 | `suspicio` | `suscipio` | 1 | lettres interverties ; 1840, 1888 |
| `l4.c1.v1` | IV, 1, 1 | `inferantur` | `inserantur` | 1 | s long lu f ; 1888 |
| `l4.c1.v2` | IV, 1, 2 | `onerasti` | `onerati` | 1 | lettre en trop |
| `l4.c1.v4` | IV, 1, 4 | `fabricatorent` | `fabricatorem` | 1 | mot mal lu ; ws, 1840, 1888 |
| `l4.c1.v4` | IV, 1, 4 | `tuam` | `tam` | 1 | lettre en trop ; ws, 1840, 1888 |
| `l4.c1.v4` | IV, 1, 4 | `templukm` | `templum` | 1 | lettre en trop ; ws, 1888 |
| `l4.c1.v4` | IV, 1, 4 | `fæderis` | `fœderis` | 1 | « œ » lu « æ » |
| `l4.c1.v5` | IV, 1, 5 | `abo` | `ab` | 1 | lettre en trop ; ws, 1840, 1888 |
| `l4.c1.v7` | IV, 1, 7 | `quandoilli` | `quando illi` | 1 | mots soudés ; ws, 1888 |
| `l4.c1.v8` | IV, 1, 8 | `institui` | `instituit` | 1 | lettre omise ; ws, 1888 |
| `l4.c1.v8` | IV, 1, 8 | `veribus` | `viribus` | 1 | « i » lu « e » |
| `l4.c1.v9` | IV, 1, 9 | `since` | `sine` | 1 | lettre en trop ; ws, 1840, 1867, 1888 |
| `l4.c1.v9` | IV, 1, 9 | `quotescumque` | `quotiescumque` | 1 | lettre omise |
| `l4.c1.v12` | IV, 1, 12 | `Sancrotum` | `Sanctorum` | 1 | lettres interverties ; ws |
| `l4.c1.v13` | IV, 1, 13 | `divinia` | `divina` | 1 | lettre en trop |
| `l4.c1.v14` | IV, 1, 14 | `esti` | `estis` | 1 | lettre omise ; ws, 1840, 1867, 1888 |
| `l4.c2.v1` | IV, 2, 1 | `disolatus` | `desolatus` | 1 | « e » lu « i » ; ws, 1840, 1888 |
| `l4.c2.v1` | IV, 2, 1 | `coni` | `boni` | 1 | « b » lu « c » ; ws, 1888 |
| `l4.c2.v3` | IV, 2, 3 | `vonvivium` | `convivium` | 1 | « c » lu « v » ; ws |
| `l4.c2.v3` | IV, 2, 3 | `daer` | `dare` | 1 | lettres interverties ; ws, 1840, 1888 |
| `l4.c2.v4` | IV, 2, 4 | `sauve` | `suave` | 1 | lettres interverties ; ws |
| `l4.c2.v4` | IV, 2, 4 | `factum es quod` | `factum est quod` | 1 | lettre omise ; 1840, 1888 |
| `l4.c3.v1` | IV, 3, 1 | `test` | `est` | 1 | lettre en trop ; ws, 1840, 1867, 1888 |
| `l4.c3.v1` | IV, 3, 1 | `exmunere` | `ex munere` | 1 | mots soudés |
| `l4.c3.v2` | IV, 3, 2 | `name` | `nam` | 1 | lettre en trop ; ws, 1840, 1888 |
| `l4.c3.v2` | IV, 3, 2 | `et in remedium salutis meæ recipere, et in remedium salutis meæ recipere` | `et in remedium salutis meæ recipere` | 1 | dittographie ; ws |
| `l4.c3.v2` | IV, 3, 2 | `hæes` | `hæres` | 1 | lettre omise ; ws |
| `l4.c3.v2` | IV, 3, 2 | `dificiant` | `deficiant` | 1 | « e » lu « i » |
| `l4.c3.v3` | IV, 3, 3 | `adlescentia` | `adolescentia` | 1 | lettre omise ; ws, 1840, 1888 |
| `l4.c3.v4` | IV, 3, 4 | `susceptone` | `susceptione` | 1 | lettre omise ; ws, 1888 |
| `l4.c4.v1` | IV, 4, 1 | `plenaire` | `plenarie` | 1 | lettres interverties ; ws, 1840 |
| `l4.c4.v1` | IV, 4, 1 | `sauvitatem` | `suavitatem` | 1 | lettres interverties |
| `l4.c4.v2` | IV, 4, 2 | `firm` | `firma` | 1 | lettre omise ; ws |
| `l4.c4.v3` | IV, 4, 3 | `tribulatones` | `tribulationes` | 1 | lettre omise ; ws |
| `l4.c4.v3` | IV, 4, 3 | `illustra` | `illustras` | 1 | lettre omise ; ws |
| `l4.c4.v3` | IV, 4, 3 | `serventes` | `ferventes` | 1 | f lu s long ; ws |
| `l4.c4.v4` | IV, 4, 4 | `cælesti` | `cælestis` | 1 | lettre omise ; ws, 1888 |
| `l4.c4.v4` | IV, 4, 4 | `præparae` | `præparare` | 1 | lettre omise ; ws |
| `l4.c4.v4` | IV, 4, 4 | `esti` | `estis` | 1 | lettre omise ; ws, 1840, 1867, 1888 |
| `l4.c4.v5` | IV, 4, 5 | `devotinis` | `devotionis` | 1 | lettre omise ; ws |
| `l4.c5.v1` | IV, 5, 1 | `myterium` | `mysterium` | 1 | lettre omise ; ws, 1840, 1867, 1888 |
| `l4.c5.v2` | IV, 5, 2 | `sacrificum` | `sacrificium` | 1 | lettre omise |
| `l4.c6.v1` | IV, 6, 1 | `ingesserio` | `ingessero` | 1 | lettre en trop ; ws, 1840, 1888 |
| `l4.c7.v1` | IV, 7, 1 | `e` | `et` | 1 | lettre omise ; ws |
| `l4.c7.v1` | IV, 7, 1 | `remodeat` | `remordeat` | 1 | lettre omise ; ws |
| `l4.c7.v1` | IV, 7, 1 | `nuda et clarifica` | `munda et clarifica` | 1 | lettre omise ; 1840, 1888 |
| `l4.c7.v1` | IV, 7, 1 | `patiturum` | `patitur` | 1 | lettres en trop |
| `l4.c7.v2` | IV, 7, 2 | `pervolvendis` | `persolvendis` | 1 | « s » lu « v » ; ws, 1840, 1867, 1888 |
| `l4.c7.v2` | IV, 7, 2 | `vene` | `bene` | 1 | « b » lu « v » ; ws |
| `l4.c7.v3` | IV, 7, 3 | `holocastum peretuum` | `holocaustum perpetuum` | 1 | mot mal lu ; ws, 1888 |
| `l4.c7.v3` | IV, 7, 3 | `commitendo` | `committendo` | 1 | lettre omise |
| `l4.c7.v4` | IV, 7, 4 | `satisfacio` | `satisfactio` | 1 | lettre omise |
| `l4.c8.v1` | IV, 8, 1 | `uram` | `puram` | 1 | lettre omise ; ws, 1840, 1888 |
| `l4.c8.v1` | IV, 8, 1 | `offere` | `offerre` | 1 | lettre omise |
| `l4.c9.v1` | IV, 9, 1 | `spontatneam` | `spontaneam` | 1 | lettre en trop ; ws, 1888 |
| `l4.c9.v2` | IV, 9, 2 | `ut te pariter` | `ut tu pariter` | 1 | mot mal lu ; ws |
| `l4.c9.v3` | IV, 9, 3 | `bontiatem` | `bonitatem` | 1 | lettres interverties ; ws, 1888 |
| `l4.c9.v5` | IV, 9, 5 | `parentem` | `parentum` | 1 | « u » lu « e » ; ws, 1840, 1888 |
| `l4.c9.v5` | IV, 9, 5 | `foroum` | `sororum` | 1 | mot mal lu ; ws, 1888 |
| `l4.c9.v5` | IV, 9, 5 | `suique` | `suisque` | 1 | lettre omise ; ws |
| `l4.c10` | IV, 10, titre | `reliquenda` | `relinquenda` | 1 | lettre omise |
| `l4.c10.v1` | IV, 10, 1 | `misericordiaæ` | `misericordiæ` | 1 | lettre en trop |
| `l4.c10.v2` | IV, 10, 2 | `omnittenda` | `omittenda` | 1 | lettre en trop ; ws, 1840, 1888 |
| `l4.c10.v3` | IV, 10, 3 | `conteri` | `confiteri` | 1 | mot mal lu ; ws, 1840, 1888 |
| `l4.c10.v4` | IV, 10, 4 | `Communinem` | `Communionem` | 1 | lettre omise ; ws, 1888 |
| `l4.c10.v4` | IV, 10, 4 | `fositan` | `forsitan` | 1 | lettre omise ; ws |
| `l4.c10.v4` | IV, 10, 4 | `impediti` | `impediri` | 1 | « r » lu « t » ; ws, 1840, 1888 |
| `l4.c10.v4` | IV, 10, 4 | `induceret` | `inducere` | 1 | lettre en trop ; ws, 1840 |
| `l4.c10.v5` | IV, 10, 5 | `illle` | `ille` | 1 | lettre en trop ; ws, 1840 |
| `l4.c10.v6` | IV, 10, 6 | `certes` | `certis` | 1 | « i » lu « e » ; ws, 1840, 1888 |
| `l4.c10.v6` | IV, 10, 6 | `Redemntoris` | `Redemtoris` | 1 | lettre en trop ; ws, 1840 |
| `l4.c10.v6` | IV, 10, 6 | `affectuoso` | `affectuosa` | 1 | « a » lu « o » ; ws, 1840, 1888 |
| `l4.c10.v7` | IV, 10, 7 | `devotini` | `devotioni` | 1 | lettre omise ; ws, 1840, 1888 |
| `l4.c11.v1` | IV, 11, 1 | `desiderabilies` | `desiderabilis` | 1 | lettre en trop ; ws |
| `l4.c11.v3` | IV, 11, 3 | `sit` | `fit` | 1 | f lu s long ; ws, 1840, 1888 |
| `l4.c11.v3` | IV, 11, 3 | `Haeo` | `Habeo` | 1 | lettre omise ; ws |
| `l4.c11.v4` | IV, 11, 4 | `lux est animæ,` | `lux est animæ meæ,` | 1 | mot omis ; ws |
| `l4.c11.v4` | IV, 11, 4 | `miserabilis vitia` | `miserabilis vita` | 1 | lettre en trop ; 1840, 1888 |
| `l4.c11.v5` | IV, 11, 5 | `cœ nam` | `cœnam` | 1 | espace parasite ; ws, 1840 |
| `l4.c11.v5` | IV, 11, 5 | `læ tificans` | `lætificans` | 1 | espace parasite ; ws, 1840, 1888 |
| `l4.c11.v6` | IV, 11, 6 | `Q` | `O` | 1 | « O » lu « Q » ; ws |
| `l4.c11.v6` | IV, 11, 6 | `mangum` | `magnum` | 1 | lettres interverties ; ws |
| `l4.c11.v6` | IV, 11, 6 | `cæ teris` | `cæteris` | 1 | espace parasite ; ws |
| `l4.c11.v6` | IV, 11, 6 | `sæ pe` | `sæpe` | 1 | espace parasite ; ws, 1840 |
| `l4.c11.v7` | IV, 11, 7 | `cæ lum` | `cælum` | 1 | espace parasite ; ws, 1888 |
| `l4.c11.v7` | IV, 11, 7 | `cæ li` | `cæli` | 1 | espace parasite ; ws, 1888 |
| `l4.c12.v1` | IV, 12, 1 | `cognioscitur` | `cognoscitur` | 1 | lettre en trop ; ws, 1888 |
| `l4.c12.v2` | IV, 12, 2 | `consuetundine` | `consuetudine` | 1 | lettre en trop ; ws, 1840, 1888 |
| `l4.c12.v2` | IV, 12, 2 | `retribuendem` | `retribuendum` | 1 | « u » lu « e » |
| `l4.c12.v3` | IV, 12, 3 | `qui` | `quia` | 1 | lettre omise ; ws, 1840, 1888 |
| `l4.c13.v2` | IV, 13, 2 | `appropinquante` | `appropinquantes` | 1 | lettre omise ; ws, 1840 |
| `l4.c13.v3` | IV, 13, 3 | `exultabant` | `exultabunt` | 1 | « u » lu « a » ; 1840, 1888 |
| `l4.c14.v1` | IV, 14, 1 | `<i>Quam magna` | `O, <i>quam magna` | 1 | lettrine « O » perdue ; ws |
| `l4.c14.v1` | IV, 14, 1 | `accendentium` | `accedentium` | 1 | lettre en trop |
| `l4.c14.v3` | IV, 14, 3 | `inflamati` | `inflammati` | 1 | lettre omise |
| `l4.c15` | IV, 15, titre | `abnegatine` | `abnegatione` | 1 | lettre omise |
| `l4.c15.v2` | IV, 15, 2 | `tolitur` | `tollitur` | 1 | lettre omise |
| `l4.c15.v3` | IV, 15, 3 | `enit` | `venit` | 1 | lettre omise ; ws, 1840, 1888 |
| `l4.c16.v1` | IV, 16, 1 | `Dulcissime atque` | `O, dulcissime atque` | 1 | lettrine « O » perdue ; ws |
| `l4.c16.v3` | IV, 16, 3 | `accends` | `accendas` | 1 | lettre omise ; ws, 1840, 1867, 1888 |
| `l4.c17.v2` | IV, 17, 2 | `suscepti` | `suscepit` | 1 | lettres interverties ; ws |
| `l4.c17.v2` | IV, 17, 2 | `servore` | `fervore` | 1 | f lu s long |
| `l4.c17.v3` | IV, 17, 3 | `cælica` | `cælicas` | 1 | lettre omise ; ws, 1888 |
| `l4.c17.v4` | IV, 17, 4 | `quætibi` | `quæ tibi` | 1 | mots soudés ; ws, 1840, 1888 |
| `l4.c18.v1` | IV, 18, 1 | `studentis` | `studens` | 1 | mot mal lu ; 1840, 1888 |
| `l4.c18.v3` | IV, 18, 3 | `ultum` | `multum` | 1 | lettre omise ; ws |
<!-- corrections:fin -->
