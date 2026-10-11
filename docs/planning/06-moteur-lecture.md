# 06 — Moteur de lecture

> Agent 06 · rapport de planification · 11 octobre 2026
> Périmètre : choix du moteur de rendu et de pagination, format interne consommé par ce moteur, localisateurs, recherche, synthèse vocale (TTS), accessibilité et performance.
> Hors périmètre, avec renvois : effet de page et polices (07), gestes et écrans (08), signets et images pieuses (09), modules et build (10), format canonique et pipeline (11), licences (12).

---

## 1. Résumé

- **Recommandation : un moteur natif Jetpack Compose** (nom de travail « Codex ») qui affiche des **documents structurés en blocs** (JSON versionné dans les paquets SQLite). Aucune WebView pour le contenu maison. Chaque page devient un composable disponible immédiatement, en direct ou en bitmap. C'est ce qu'il faut pour un vrai effet de page qui tourne (agent 07).
- **Une page qui tourne au-dessus d'une WebView reste faisable, mais elle coûte cher.** Readest (foliate-js) l'a livrée entre juillet et septembre 2026 : capture `PixelCopy` de la page sortante, navigation instantanée en dessous, maillage WebGL par-dessus. Il a fallu une douzaine de PR, un JPEG plafonné à 2× (le PNG pleine densité prenait environ 1,5 s par page), des tours de retour dégradés, et un scintillement de graisse de police reste ouvert en octobre 2026.
- **Readium Kotlin Toolkit 3.4.0** (11 septembre 2026, BSD-3) est mûr pour l'EPUB : décorations, TTS, locators, PDF via PDFium. Ses navigateurs Compose restent expérimentaux, et il n'a ni effet de page ni API de capture de page. Il est à garder pour un éventuel **import d'EPUB externes (« Plus tard »)**, pas comme moteur principal.
- **foliate-js** (MIT) n'a aucune release et son API est déclarée instable. **readium-js** est archivé depuis 2022. Les deux sont écartés.
- **La typographie native est suffisante, à condition d'écrire quatre briques.** Le texte Compose 1.12 repose sur `StaticLayout` : coupure de ligne « HighQuality », césure `FULL_FAST` (API 33+) et justification inter-mots (API 26+). Android embarque la **césure française et latine** (`hyph-fr`, `hyph-la` ; le latin liturgique n'est pas compilé). Restent à construire : le paginateur, la lettrine, les colonnes latin/français en regard et la sélection de texte.
- **Localisateurs :** `identifiant de bloc stable + décalage + citation (préfixe/exact/suffixe) + révision du contenu`. Ils résistent aux changements de police et se ré-ancrent après mise à jour. Ils s'exportent vers le Locator Readium et vers les **EPUB Annotations 1.0** du W3C (Working Draft du 20 septembre 2026).
- **Recherche :** FTS5 via le SQLite embarqué d'androidx (`sqlite-bundled` 2.7.1, compilé avec `SQLITE_ENABLE_FTS5`) ou `@Fts5` de Room 3.0 (stable depuis juillet 2026). Tokeniseur `unicode61 remove_diacritics 2`, racinisation française faite en Kotlin, index construit d'avance par le pipeline (11).
- **TTS :** `TextToSpeech` d'Android avec une session Media3, en itérant sur les blocs avec la langue de chaque bloc. Les rubriques peuvent être sautées. Pour le latin, il faut choisir (décision D6).
- **PDF :** `PdfRenderer` de la plateforme ou `androidx.pdf` 1.0.0-beta01 (26 août 2026), seulement pour une vue fac-similé. Les PDF sources, comme Ignace, doivent être **convertis en blocs** par le pipeline.
- **Avant de s'engager**, faire un spike de 2 à 3 semaines (pagination, lettrine, colonnes en regard, effet de page, TalkBack) sur un téléphone physique, avec des builds en CI, puis décider au vu des résultats (porte de décision).

---

## 2. Constats détaillés

### 2.1 Grille d'exigences

| # | Exigence | Pourquoi c'est structurant |
|---|---|---|
| E1 | Modes paginé **et** défilant | Paginé pour l'effet « livre », défilant pour TalkBack et les longs textes |
| E2 | Pagination stable et déterministe pour un réglage donné | Signets, folios et retour à la bonne page |
| E3 | Double page en paysage | Le « livre ouvert » du brief |
| E4 | **Page courante et page suivante disponibles en couche Compose ou en bitmap** | C'est la condition de l'effet de page (07) |
| E5 | Polices embarquées, césure fr/la, justification | Le rendu « papier » |
| E6 | Lettrines, rubriques rouges, petites capitales, ℣ ℟ ✠ | Identité du livre de prière |
| E7 | Latin et français en regard | Le missel (03) |
| E8 | Localisateurs robustes (police, mise à jour du contenu) | Signets de tissu, images pieuses (09) et notes |
| E9 | Recherche plein texte insensible aux accents | Navigation |
| E10 | TTS et TalkBack | Accessibilité, prière à l'écoute |
| E11 | Fluidité sur un appareil de milieu de gamme | 60 à 90 images/s pendant le tour de page |

### 2.2 Option (a) : Readium Kotlin Toolkit

**État en octobre 2026**

- Versions : **3.4.0 (2026-09-11)**, 3.3.0 (2026-06-02), 3.2.0 (2026-05-13), 3.1.2 (2025-09-29). Licence BSD-3-Clause, activité régulière.
- Prérequis de la 3.4.0 : minSdk **24**, compileSdk **37**, Kotlin **2.4.20** et Gradle 9.7.0 (seulement en sous-module). C'est compatible avec la pile du projet (Kotlin 2.4.x, AGP 9.4.x).
- Formats : EPUB 2 et 3 complets. Le **Readium Web Publication Manifest (RWPM)** n'est que partiellement pris en charge. Le PDF passe par l'adaptateur PDFium (`com.github.marain87:PdfiumAndroid` 1.9.8) ou par PSPDFKit, qui est commercial.
- Navigateurs :
  - Le **legacy** `EpubNavigatorFragment` est un Fragment avec un ViewPager, une WebView par ressource et une pagination en colonnes CSS (ReadiumCSS). La 3.2.0 lui a ajouté des tours de page animés, mais uniquement en glissement (`animated = true`).
  - Les **nouveaux navigateurs Compose** `ReflowableWebRendition` et `FixedWebRendition` sont marqués `@ExperimentalReadiumApi` et « not battle tested ». Leur matrice de parité indique : pas d'animation de tour de page programmatique et pas de clavier. Ils restent des WebView dans un pager Compose.
- Préférences : `fontFamily` (avec déclarations de polices embarquées), `fontSize`, `hyphens`, `columnCount` (auto, 1 ou 2, d'où la double page), `lineHeight`, `textAlign`, `paragraphIndent`, `ligatures`, thèmes, `scroll`.
- **Décorations :** surlignage, soulignement et gabarits HTML personnalisés. Le placement « derrière le texte » est expérimental depuis la 3.2.0. Le ticket #352 (« Epub decorators are not accessible ») est ouvert depuis mai 2023.
- **Locator :** `href`, `mediaType`, `locations` (`fragments`, `progression`, `position`, `totalProgression`, `otherLocations`, qui porte cssSelector, partialCfi et domRange) et `text` (`before`, `highlight`, `after`). C'est un bon modèle, que nous reprenons en partie (§3.4).
- **TTS :** `TtsNavigator` et `AndroidTtsEngine` exposés en `MediaNavigator` (Media3). Ils itèrent via le *Content Service* sans afficher le navigateur et prennent en charge les préférences de langue, de hauteur et de vitesse.
- La **recherche** existe pour l'EPUB reflowable. C'est une itération en mémoire, pas un index FTS persistant.

**Confrontation aux exigences**

| Exig. | Évaluation |
|---|---|
| E1–E3 | Bon : paginé, défilant et `columnCount = 2`. |
| **E4** | **Faible.** Aucune API ne donne la page suivante. Il faudrait la technique de Readest (§2.4) : capturer la fenêtre, appeler `goForward(animated=false)` sous un calque, puis animer la capture. Les tours arrière et la double page imposent un travail de niveau fork. |
| E5–E6 | Très bon grâce au CSS : `hyphens:auto`, `initial-letter` pour les lettrines (Chromium 110 et plus), petites capitales **synthétisées** même sans `smcp`, couleurs, `@font-face`. La césure dépend des dictionnaires de la WebView ; pour le latin, à vérifier. |
| E7 | Moyen. Tableau ou grille CSS dans des colonnes de pagination : la fragmentation fonctionne dans Chromium récent, mais on contrôle mal les coupures alignées d'une ligne à l'autre. |
| E8 | Bon pour de l'EPUB statique (CFI et texte). Plus fragile pour le missel assemblé dynamiquement, où le DOM change selon le jour. |
| E9 | Partiel. Il faudrait de toute façon un index FTS5 à nous. |
| E10 | TTS bon. TalkBack moyen en mode paginé (contenu hors écran dans les colonnes, décorations inaccessibles). |
| E11 | Correct, mais chaque WebView coûte plusieurs dizaines de Mo et un temps de démarrage. Il y a deux mondes à déboguer (Kotlin et JS/CSS). |

**Conclusion (a) :** c'est excellent comme lecteur d'EPUB **arbitraire**. Pour notre contenu maison, il se heurte à l'exigence centrale (E4) et ajoute la WebView sans en tirer de bénéfice décisif. Le missel dynamique demanderait de générer du XHTML à la volée, puis de le paginer dans un moteur qu'on ne maîtrise pas.

### 2.3 Option (b) : foliate-js ou readium-js dans une WebView

- **foliate-js** (johnfactotum, licence MIT) : dernier commit le 2026-05-01 (`78914ae`), **aucune release**. Le README est explicite : « This library itself is, however, *not* stable. Expect it to break and the API to change at any time. »
  - Modules : `paginator.js` (colonnes CSS dans une iframe, attributs `flow` paginé ou défilé et `max-column-count` pour la double page), `epubcfi.js`, `overlayer.js` (surlignages SVG), `search.js`, `tts.js` (qui **génère du SSML avec marques** : il faut un moteur hôte, donc un pont vers `TextToSpeech` sur Android), `progress.js`, `footnotes.js`.
  - Sécurité : une CSP est obligatoire (les contenus partagent la même origine via `blob:`).
  - Son principal utilisateur, **Readest** (Tauri, licence **AGPL-3.0**, v0.12.12 du 2026-10-04), maintient un **fork**.
- **readium-js** : dépôt **archivé** (dernier push le 2022-04-11). Son successeur est `readium/ts-toolkit` (navigator 2.12.1 du 2026-10-09, BSD-3), orienté web (Thorium Web). Sur Android, Readium Kotlin encapsule déjà la même approche ReadiumCSS + JS.
- **Conclusion (b) :** on cumule les inconvénients de (a) (WebView, effet de page indirect) sans les garanties d'une bibliothèque Kotlin versionnée. **Option écartée.** Readest reste une **excellente source d'enseignements** sur l'effet de page en WebView (§2.4), mais il ne faut **pas réutiliser son code** : il est sous AGPL (voir agent 12).

### 2.4 Effet de page et WebView : faisabilité et coût (cas Readest, 2026)

Readest a livré « Page Curl » dans la PR #4940 (2026-07-05), puis l'a corrigé dans les PR #5217, #5291, #6107 et #6246 (juillet à septembre 2026). Voici sa technique, telle que décrite dans les PR :

1. **Capturer la page sortante.** Sur Android, c'est un `PixelCopy` de la surface de la fenêtre. La capture est encodée en **JPEG plafonné à 2× les pixels CSS**, parce qu'un PNG pleine densité prenait **environ 1,5 s par tour** sur un écran 3×.
2. **Naviguer instantanément** dans la WebView, sous le calque.
3. **Animer la capture** avec un maillage WebGL enroulé autour d'un cylindre et suivre le doigt. La page « révélée » est la WebView elle-même, en direct.

Les difficultés qu'ils documentent :
- **Tour arrière dégradé** : « only the outgoing page can be snapshotted (a second capture would include the overlay itself) ».
- **Verso de la feuille en double page** : il faut une chorégraphie. On fige la zone derrière un cache natif, on découpe le calque, on capture, puis on restaure (#6107).
- **Latence au moment du geste** : il a fallu préparer à l'avance des « surfaces spéculatives » (capture, décodage, texture), les invalider à chaque changement de thème, d'annotation ou de barre d'outils, et tamponner les échantillons de toucher pendant la capture (#5291).
- **Différence de rendu entre le bitmap et le texte vivant** : le ticket #6670 (2026-10-06, ouvert) signale un texte « plus fin et flou » pendant l'animation, puis un saut à la fin.
- La variante View Transitions demande Chromium ou WebView 140 et plus. Elle a été désactivée sur iOS 18 à cause de plantages.

**Coût mémoire** : une capture ARGB de 1080×2400 pèse environ 10 Mo. Une double page avec verso en demande deux ou trois.

**Pour Readium Kotlin**, le principe serait le même : un calque Compose au-dessus du navigateur, `PixelCopy`, puis `go(…, animated=false)`. Aucune API publique ne le facilite, et les tours arrière ou la double page demandent d'accéder aux internes du pager.

**Verdict E4 :** une WebView peut **simuler** un effet de page avec un bitmap de la seule page sortante. Le résultat est fragile et cher à maintenir, et il n'atteint jamais la qualité d'un rendu où les **trois** pages (recto, verso, page révélée) sont disponibles nettes et à la cadence d'affichage.

### 2.5 Option (c) : rendu natif en Compose

**État de la pile.** La BOM 2026.09.00 correspond à Compose UI et Foundation **1.12.1** (9 septembre 2026) ; la 1.13.0-beta01 date du 7 octobre 2026. Le code source d'androidx (`AndroidParagraph.android.kt`) montre comment le texte Compose s'appuie sur `StaticLayout` et `BoringLayout` sous Android :
- `TextAlign.Justify` donne `JUSTIFICATION_MODE_INTER_WORD`, qui n'est effectif qu'à partir de l'**API 26**.
- `Hyphens.Auto` donne `HYPHENATION_FREQUENCY_FULL_FAST` à partir de l'API 33, et `FULL` en dessous. Android 13 annonce une césure jusqu'à 200 % plus rapide.
- `LineBreak.Paragraph` (stratégie `HighQuality`) donne `BREAK_STRATEGY_HIGH_QUALITY`. `Balanced` existe aussi.
- La césure suit la **locale du texte**. `SpanStyle(localeList = …)` permet une césure **par segment**, latin ou français, dans un même paragraphe. Ce comportement est à valider dans le spike.
- **Dictionnaires de la plateforme** (AOSP `external/hyphenation-patterns`, `android_text_Hyphenator.cpp`) :
  - `addHyphenator("fr", 2, 3)` et `addHyphenator("la", 2, 2)`.
  - `hyph-la` correspond aux motifs « modern and medieval Latin » de C. Beccari, v3.201 (2016), sous licence MIT.
  - Les motifs **liturgiques** `hyph-la-x-liturgic` sont présents dans le dépôt mais **non compilés** : un commentaire indique qu'il n'existe pas d'étiquette BCP 47 standard.

**Outils disponibles.**
- `TextMeasurer`, `Paragraph` et `MultiParagraph` produisent un `TextLayoutResult`. Celui-ci donne `getLineTop/Bottom/Start/End`, `getLineForVerticalPosition`, `getOffsetForPosition`, `getPathForRange` (pour les surlignages) et `getBoundingBox`, soit tout ce qu'il faut pour paginer, tester les touchers et décorer.
- `DrawScope.drawText(textLayoutResult)` dessine une mise en page déjà mesurée. On découpe une **tranche de lignes** avec `clipRect` et une translation.
- Le `TextMeasurer` a un cache LRU (8 entrées par défaut). Il est annoté `@Immutable`, mais **sa sûreté entre threads n'est pas documentée**. Pour paginer en arrière-plan, il faut une instance par thread ou appeler directement `Paragraph(...)`. À valider.
- `GraphicsLayer` (Compose 1.7 et plus) offre `record {}` et `toImageBitmap()`, ce qui transforme n'importe quelle page en bitmap pour l'agent 07. Sur l'API 33 et plus, un `RuntimeShader` (AGSL) branché dans `graphicsLayer { renderEffect }` peut même déformer la page **sans bitmap intermédiaire**.
- Bibliothèques existantes :
  - `oleksandrbalan/pagecurl` (Apache-2.0, **v1.5.1 du 2024-02-09**, non maintenue depuis) **compose simultanément** les pages n-1, n et n+1. Elle dessine le verso en rappelant `drawContent()` en miroir, et prouve qu'en natif les trois pages s'obtiennent gratuitement.
  - `oleksandrbalan/textflow` (Apache-2.0, v1.2.1 du 2024-07-20) fait des lettrines en deux blocs. Elle mesure `topBlock` avec une largeur réduite, puis `bottomBlock` avec `text.subSequence(...)`. Défaut : la dernière ligne du bloc du haut n'est **pas justifiée**, car Android traite toute fin de chaîne comme une dernière ligne de paragraphe.

**Ce qu'il faut écrire soi-même** :

| Brique | Difficulté | Remarque |
|---|---|---|
| Paginateur (veuves et orphelines, titres attachés à la suite, images, double page) | Moyenne | Algorithme glouton sur les lignes mesurées (§3.5) |
| Lettrine | Moyenne | Mesurer le **paragraphe entier** à largeur réduite et ne garder que N lignes, qui restent justifiées. Mesurer le reste à pleine largeur à partir de `getLineEnd(N-1)`. Repli possible : `StaticLayout` et `LeadingMarginSpan2` dessinés dans un `Canvas` natif. |
| Colonnes latin/français en regard | Moyenne | Mise en page par rangées alignées (§3.6) |
| Sélection de texte et menu d'annotation | Moyenne à élevée | Texte dessiné à la main : poignées et hit-test à coder. Le compromis MVP est la sélection par mot ou par verset. |
| Sémantique d'accessibilité | Faible | `Modifier.semantics` par paragraphe, construit à partir des blocs |
| Petites capitales | Faible | `fontFeatureSettings = "smcp, c2sc"`. Android **ne synthétise pas** les petites capitales : la police (07) doit les fournir. |
| Système de styles (une « cascade » minimale) | Faible | Thème, rôle du bloc, puis marques en ligne, convertis en `AnnotatedString` |

**Confrontation aux exigences** : E1–E4 très bon, car c'est nous qui contrôlons tout. E5 bon. E6 bon une fois les briques écrites. E7 très bon, avec un contrôle total des coupures alignées. E8 très bon (identifiants de blocs). E9 indépendant du moteur. E10 bon, à condition de soigner la sémantique. E11 très bon : pas de WebView, texte déjà mesuré, aucune mise en page pendant l'animation.

**Coût :** il faut investir dans le moteur, environ **6 à 10 semaines-personne** pour un moteur solide, spike compris. Cette estimation est indicative, à affiner après le spike. En contrepartie, il n'y a aucune dépendance fragile.

### 2.6 Option (d) : hybrides

| Variante | Description | Avis |
|---|---|---|
| d1 | Natif pour le contenu maison, **Readium pour les EPUB importés** par l'utilisateur | **Recommandée, pour « Plus tard ».** On isole la WebView dans un module optionnel. L'effet de page y est limité (glissement, ou capture façon Readest). |
| d2 | Natif, plus une WebView pour les blocs « exotiques » (tableaux, notation) | À éviter. On **pré-rend** ces blocs dans le pipeline, en SVG converti en `ImageVector` ou en image. |
| d3 | Mesure dans la WebView, rendu natif | Non : c'est le pire des deux mondes. |
| d4 | Natif, plus **PDF en bitmaps** (fac-similé) | **Recommandée en V1.** Les pages PDF sont des images et s'intègrent naturellement à l'effet de page. |

### 2.7 Option (e) : rendu PDF

- **`android.graphics.pdf.PdfRenderer`** (plateforme) produit des bitmaps de pages. Les API de texte, de recherche et de sélection sont arrivées avec Android 15 et l'extension SDK 13 (`PdfRendererPreV` pour les versions antérieures concernées). Les niveaux exacts sont à vérifier lors de l'implémentation.
- **`androidx.pdf`** : **1.0.0-beta01 du 2026-08-26** (pas encore stable). Elle comprend `PdfViewerFragment`, un `PdfViewer` Compose (`@ExperimentalPdfApi`, `pagesPerRow` pour deux pages côte à côte), la recherche, la sélection, l'annotation via `androidx.ink` et l'OCR. Les fonctions de lecture et de rendu ont été rétroportées à **minSdk 28** dans l'alpha18.
- **PDFium** via `PdfiumAndroid` (fork marain87, utilisé par Readium 3.4.0) : licence permissive, rendu natif. **MuPDF est sous AGPL** et donc à éviter (agent 12).
- **Usage recommandé :** un PDF n'est **pas** reflowable et se lit mal en portrait sur un téléphone, où le zoom entre en conflit avec l'effet de page. Pour les sources PDF (Exercices de saint Ignace, ragionline.com), le pipeline (02 et 11) doit **extraire le texte et produire des blocs**. On garde la vue PDF pour « voir l'original » en V1, avec un rendu bitmap par page directement compatible avec l'effet de page de l'agent 07.

### 2.8 Typographie fine : constats exigence par exigence

| Besoin | Natif (c) | WebView (a/b) | Notes et interfaces |
|---|---|---|---|
| Polices embarquées | `FontFamily(Font(R.font…))` | `@font-face` | Choix des polices : agent 07 |
| Césure française | `hyph-fr` (plateforme) | `hyphens:auto` (Chromium) | Le pipeline doit insérer les **espaces fines insécables** (U+202F) devant ; : ! ? et à l'intérieur des guillemets. La police doit avoir ce glyphe (07, 11). |
| Césure latine | `hyph-la`, moderne et médiéval | Incertaine | Le latin **accentué** (Kýrie, eléison) avec les motifs Beccari est à tester. L'option liturgique passe par des tirets conditionnels (U+00AD) insérés par le pipeline avec `hyph-la-x-liturgic`, à retirer pour la copie et la recherche. |
| Justification | Inter-mots (API 26+) | `text-align: justify` | Pas de limite d'espacement maximal sous Android : risque de **lézardes en colonnes étroites**, d'où D3. |
| Lettrines | À coder (§2.5) | `initial-letter` (Chromium 110+) | Lettrines ornées vectorielles : agent 07 |
| Rubriques rouges | `SpanStyle(color)` avec un jeton de thème | CSS | Il faut **un rouge par thème** (plus clair en sombre) et un **second indice que la couleur**, comme l'italique ou un corps plus petit (WCAG 1.4.1). |
| Petites capitales | `smcp` et `c2sc`, **dans la police** | Synthétisées si absentes | Contrainte de police pour 07 |
| ℣ ℟ ✠ † * | Glyphes Unicode (U+2123, U+211F, U+2720) | Idem | **Couverture des glyphes à vérifier** dans la police retenue (07). Sinon, le repli système donne un style incohérent. Solutions : une petite police « symboles liturgiques » dédiée, ou la forme « V/. R/. ». La sémantique TalkBack doit dire « Verset » et « Répons ». |
| Colonnes en regard | Rangées alignées (§3.6) | Tableau ou grille CSS | Agent 03 |

### 2.9 Localisateurs, signets et annotations

- **Les CFI EPUB** sont précis mais **dépendent de la structure du DOM** : toute révision du XHTML les casse. **Les positions Readium** (environ une position par tranche fixe d'octets de ressource) ne dépendent pas de la police, mais dérivent si le texte change. **La progression** (fraction) est trop grossière pour une note.
- **Le standard de fait pour la robustesse** est la combinaison des sélecteurs du *W3C Web Annotation Data Model* : `FragmentSelector` (identifiant), `TextQuoteSelector` (préfixe, texte exact, suffixe) et `TextPositionSelector` (décalages). Le W3C la reprend dans **EPUB Annotations 1.0**, en Working Draft depuis février 2026 (dernier instantané repéré : WD du 2026-09-20). C'est un profil du Web Annotation Data Model qui couvre l'export et l'import.
- **Conclusion :** avec des identifiants de blocs **stables et sémantiques**, fournis par le pipeline (11), on obtient le meilleur des trois. L'identifiant est insensible à la police, le décalage est précis, et la citation permet de ré-ancrer. Détail au §3.4.

### 2.10 Recherche plein texte

- Le SQLite **du framework Android ne garantit pas FTS5**. En revanche, `androidx.sqlite:sqlite-bundled` (2.7.1 du 2026-09-09) est compilé avec `-DSQLITE_ENABLE_FTS5` (vérifié dans `sqlite/sqlite-bundled/build.gradle` d'androidx-main). **Room 3.0** (stable le 2026-07-01, 3.0.3 le 2026-09-09) ajoute `@Fts5`, avec un tokeniseur trigramme et une option `detail`, mais il exige un `SQLiteDriver`. C'est l'affaire de l'agent 10.
- **Tokeniseur `unicode61 remove_diacritics 2`** : il retire correctement les diacritiques de tous les caractères latins. Il y a donc insensibilité aux accents (é/e, Kýrie/kyrie). Il faut **pré-normaliser** œ→oe et æ→ae, unifier les apostrophes (’ et '), appliquer NFC et retirer ℣ ℟ et U+00AD.
- **Racinisation française :** FTS5 n'en fournit pas. Un tokeniseur personnalisé exige l'API C (`fts5_tokenizer_v2`), donc du code NDK. **Alternative sans NDK** : une colonne `stems` calculée par le pipeline avec Snowball French, et la **même** racinisation appliquée à la requête en Kotlin (implémentation Java de Snowball, BSD). Il faut figer la version de l'algorithme des deux côtés.
- `highlight()` et `snippet()` produisent les extraits. Les résultats renvoient à un `blockId` et à un décalage, puis le moteur ouvre la page qui contient le bloc et décore l'occurrence.
- Un **index par paquet** est construit d'avance. La recherche globale passe par `ATTACH` (10 bases attachées par défaut) ou par une itération sur les paquets. Les notes de l'utilisateur ont leur propre table FTS sur l'appareil.

### 2.11 Synthèse vocale (TTS)

- `TextToSpeech` d'Android. `UtteranceProgressListener.onRangeStart` (API 26) donne la **position du mot prononcé**, ce qui permet de surligner le mot en cours. Pour la lecture en arrière-plan, il faut une `MediaSession` Media3 (notification, écran verrouillé). Readium a la même architecture, qu'on peut s'inspirer sans l'adopter.
- Sur une application ciblant Android 11 et plus, il faut déclarer `<queries><intent><action android:name="android.intent.action.TTS_SERVICE"/></intent></queries>` (le guide Readium le rappelle).
- Les **énoncés** sont découpés en phrases (`BreakIterator` selon la locale du bloc), avec un **changement de voix par bloc ou par segment** grâce à l'attribut `lang`.
- **Spécificités du livre de prière :**
  - Option « ne pas lire les rubriques » : ce sont des instructions.
  - ℣ et ℟ sont lus « Verset » et « Répons », ou omis.
  - Les numéros de versets sont facultatifs.
  - Pendant l'écoute, la page tourne automatiquement, avec une animation douce ou sans animation.
- **Latin :** les voix Google n'ont pas de latin. Trois options : une voix **italienne**, proche de la prononciation ecclésiastique ; **eSpeak NG** s'il est installé (il a une voix latine ; licence GPL, mais c'est une application séparée et non intégrée) ; ou sauter le latin. C'est la décision D6.

### 2.12 Accessibilité (TalkBack)

- **Natif :** chaque page expose une sémantique par paragraphe, dans l'ordre de lecture. Il faut aussi des `heading()` pour les titres, des actions personnalisées « Page suivante » et « Page précédente » (plus les actions de défilement), et l'annonce « Page 12 sur 40 » en région active. Le texte sémantique est **construit à partir des blocs**, ce qui permet de prononcer ℣ « Verset » sans altérer l'affichage. C'est un avantage net sur la WebView.
- Si TalkBack est actif, il faut **proposer automatiquement le mode défilant**. Le mode paginé ne doit jamais bloquer la lecture continue.
- Il faut respecter « Supprimer les animations » (échelle d'animation à 0, ce qui désactive l'effet de page) et l'agrandissement **non linéaire** des polices jusqu'à 200 % (Android 14 et plus), qui oblige à re-paginer.
- **WebView :** l'arbre d'accessibilité de Chromium est riche, mais les colonnes hors écran du mode paginé et les décorations (#352 chez Readium) posent problème.

### 2.13 Performance (appareil de milieu de gamme)

- **Natif :** toute la mesure se fait **hors du fil principal**, avant l'affichage. Pendant l'effet de page, on ne fait que dessiner des `TextLayoutResult` déjà calculés ou des couches Compose, donc sans aucune mise en page. La césure `FULL_FAST` (API 33+) réduit le coût de la mesure.
- **WebView :** démarrage du moteur, plusieurs dizaines de Mo par instance, pagination en JS, et le coût de capture vu chez Readest.
- **Objectifs proposés**, à mesurer dans le spike sur un téléphone physique et non sur un émulateur, conformément aux limites de la VM :
  - première page d'une section affichée en moins de 150 ms après le toucher ;
  - re-pagination de la section courante après un changement de police en moins de 300 ms, le reste en arrière-plan ;
  - tour de page sans image perdue à 60 ou 90 Hz ;
  - mémoire du moteur sous 60 Mo hors images.
- **Mémoire :** on garde un LRU des mises en page autour de la position (environ ±3 pages) et on persiste uniquement les **sauts de page**. Les bitmaps ne sont créés que si l'agent 07 en a besoin, soit trois au plus d'environ 10 Mo.

### 2.14 Matrice de synthèse

| Exigence | (a) Readium | (b) foliate-js | (c) Natif Compose | (e) PDF |
|---|---|---|---|---|
| E1 paginé et défilant | Bon | Bon | Bon (à coder) | Bon |
| E2 pagination déterministe | Moyen (moteur web) | Moyen | **Très bon** | Fixe |
| E3 double page | Bon | Bon | Bon | Bon |
| **E4 effet de page** | **Faible** (capture) | **Faible** (capture) | **Très bon** | **Très bon** (bitmaps) |
| E5 césure et justification | Très bon | Très bon | Bon | Sans objet |
| E6 lettrines et petites capitales | Très bon | Très bon | Moyen (à coder, dépend de la police) | Sans objet |
| E7 latin/français en regard | Moyen | Moyen | **Très bon** | Fixe |
| E8 localisateurs | Bon | Bon | **Très bon** | Moyen (page et rectangle) |
| E9 recherche | Partiel | Partiel | FTS5 (indépendant) | Texte PDF (Android 15+) |
| E10 TTS et TalkBack | Bon et moyen | Pont nécessaire | Bon et bon | Faible |
| E11 performance | Moyen | Moyen | **Très bon** | Bon |
| Maturité et risque | Mûr, mais Compose expérimental | Instable | Code à écrire | Mûr (beta pour androidx.pdf) |

---

## 3. Recommandations

### 3.1 Choix d'architecture

**Moteur natif Compose, alimenté par des documents en blocs**, avec deux satellites :
- un **visionneur PDF fac-similé** (bitmaps, V1) ;
- un **module optionnel Readium** pour importer des EPUB externes (« Plus tard », seulement si la décision D4 le veut).

**Pourquoi ce choix sert l'exigence centrale (une expérience proche du papier, avec des pages qui tournent) :**
1. **Les trois pages d'un tour** (recto courant, verso de la feuille, page révélée) sont des composables disponibles **de façon synchrone**, nets et sans capture. L'agent 07 peut les dessiner en direct (pli 2D ou shader AGSL) ou les figer en `ImageBitmap` pour un maillage 3D, au choix.
2. **La pagination est à nous.** Elle est déterministe, met en cache les sauts de page, gère les veuves et orphelines, les titres attachés, les lettrines, les rangées latin/français et le début de section sur une page de droite (recto) : autant de « gestes typographiques » qui font le papier.
3. **Le contenu est assemblé dynamiquement** (messe du jour, office, rosaire) par simple concaténation de blocs. Il n'y a pas de XHTML à régénérer.
4. **Les identifiants de blocs** donnent des signets, des images pieuses et des notes qui survivent aux changements de police et aux mises à jour.
5. **Une seule technologie** (Kotlin et Compose), adaptée à une VM modeste et à un projet porté par une seule personne.

### 3.2 Format de contenu interne (contrat de rendu proposé à l'agent 11)

**Recommandation : un JSON structuré de blocs, consommé par le moteur natif.** Le format **canonique** de rédaction appartient à l'agent 11 : ce peut être un XHTML ou un XML sémantique (TEI-lite), compilé vers ces blocs. Ce qui est **livré** dans les paquets, ce sont les blocs. L'EPUB 3 n'est qu'un **format d'export** (Plus tard).

Exigences minimales du contrat :

1. **Hiérarchie :** paquet → document (une lettre, un chapitre, une partie de l'ordinaire) → section, d'environ 50 000 caractères **au plus**, car c'est l'unité de pagination → blocs.
2. **Chaque bloc** porte un `id` **stable et sémantique**, unique dans le paquet (par exemple `ordo.gloria.p2` ou `bible.crampon.jn.3.16`), une `lang` (`fr` ou `la`), un `rev` (empreinte du texte) et un `t` (type).
3. **Le texte en ligne** est une **chaîne brute plus une liste de marques** `[début, fin, type, attributs]`, à l'image d'`AnnotatedString`. La même chaîne sert à l'affichage, à la FTS, au TTS et aux ancres. Les décalages sont exprimés en **unités UTF-16** (chaînes Kotlin), après normalisation **NFC**.
4. **Types de blocs v1 :** `heading` (niveau), `paragraph` (attributs `dropCap`, `indent`, `align`), `rubric`, `versicle` (rôle V ou R), `verse` (numéro), `psalmVerse` (médiante `*`, flexe `†`), `prayer`, `quote`, `list`, `image` (asset, `alt`, légende), `separator` (fleuron), `note` (renvoi), `parallel` (rangées `{la: blocs, fr: blocs}`) et `insertPoint` (point d'insertion autorisé pour les images pieuses).
5. **Marques en ligne :** `em`, `strong`, `sc` (petites capitales), `red`, `lang`, `sup`, `noteRef`, `link` (vers un `id`) et `sym` (℣ ℟ ✠ † *, avec une étiquette parlée).
6. **Indications de mise en page** (facultatives) : `keepWithNext`, `keepTogether`, `breakBefore` (`page` ou `recto`).
7. Une **table de redirection d'identifiants** (`ancien → nouveau`) à chaque version du paquet, pour ré-ancrer les annotations.
8. Le **stockage** se fait dans le SQLite du paquet : une ligne par section avec le JSON compressé, plus les tables FTS5 (§2.10) et les ressources. La sérialisation passe par kotlinx.serialization, avec un schéma versionné (`schemaVersion`).

Exemple, à titre purement illustratif :

```json
{"id":"ordo.kyrie.v1","t":"versicle","lang":"la","role":"V","rev":"a91c",
 "text":"Kýrie, eléison.","marks":[]}
{"id":"ordo.gloria.r1","t":"rubric","lang":"fr","rev":"0f2e",
 "text":"Le prêtre, au milieu de l'autel, entonne le Glória.","marks":[[44,50,"lang",{"v":"la"}]]}
```

### 3.3 Schéma d'architecture

```
  +---------------------- Paquets de contenu (agent 11) ----------------------+
  | pack.sqlite : documents > sections (JSON de blocs) | assets | FTS5 | redir |
  +-------------+-----------------------------------------------+-------------+
                | lecture d'une section                         | MATCH
                v                                               v
  Calendrier -->+------------------------+            +-------------------+
  (agent 05)    | Assembleur de documents|            | Recherche (FTS5,  |
  messe du jour | ordinaire + propre ... |            | normalisation fr) |
                +-----------+------------+            +---------+---------+
                            | BlockDocument (id stables, lang)  | Position
  Préférences ->+-----------v------------+                      |
  (taille,      | Styliste : thème,      |<-- polices (07)      |
  thème,        | couleur liturgique(05),|                      |
  interligne)   | règles -> AnnotatedStr.|                      |
                +-----------+------------+                      |
                            | StyledBlock                       |
                +-----------v------------+    LRU   +---------------------+
                | Mesureur (TextMeasurer |--------->| BlockLayout (lignes,|
                | / Paragraph, hors UI)  |          | métriques, lettrine)|
                +-----------+------------+          +---------------------+
                            |
                +-----------v------------+  clé de pagination -> cache SQLite
                | Paginateur : veuves,   |  (sauts de page = bloc + décalage)
                | orphelines, rangées    |
                | la/fr, recto, doubles  |
                +-----+-------------+----+
     PageSequence     |             |  mode défilant (LazyColumn de blocs)
   (+ intercalaires 09)             v
                +-----v------+  +--------------+
                | PageSurface|  | ScrollReader |
                | composable |  +--------------+
                | + snapshot |
                +-----+------+
                      | pages n-1, n, n+1 (+ verso)
                +-----v-----------------------------+
                | Tourne-page (07) <- gestes (08)   |
                | rubans et signets (09)            |
                +-----------------------------------+

  Transverses : Ancres et Positions --> Décorations (surlignages, notes,
                résultats de recherche, mot lu par le TTS)
                TTS (TextToSpeech + Media3) <-- itérateur de blocs --> tourne la page
                Accessibilité : sémantique construite à partir des blocs
                PDF fac-similé : PdfRenderer -> bitmaps -> PageSurface (V1)
                Import EPUB externe : Readium (module isolé, Plus tard)
```

Les modules proposés, à arbitrer par l'agent 10, figurent ci-dessous. Le modèle reste en Kotlin pur pour qu'on puisse le tester en JVM sans émulateur.

```
:core:model        Block, Inline, Position, Anchor (Kotlin pur, testable en JVM)
:content:packs     lecture SQLite, assemblage, redirections        (10/11)
:reader:layout     styliste, mesureur, paginateur, cache (Android text)
:reader:ui         PageSurface, Spread, ScrollReader, sélection, décorations
:reader:turn       effet de page (agent 07), consomme PageSurface
:reader:search     FTS5, normalisation, racinisation
:reader:tts        TextToSpeech, Media3, itérateur d'énoncés
:reader:pdf        fac-similé (V1)
:reader:epub       Readium (Plus tard, optionnel)
```

### 3.4 Interface avec l'agent 07 (tourne-page)

Le contrat proposé est donné en pseudo-code, pour la planification uniquement :

```
PageSource
  pageCount : StateFlow<Int?>     // null tant que la pagination de fond n'est pas finie
  pageAt(i) : PageHandle          // synchrone pour i dans la fenêtre préparée (±2)
  indexOf(Position) : Int
PageHandle
  index, side (RECTO | VERSO), spreadIndex
  @Composable Content(modifier)   // rendu vivant, texte net
  suspend snapshot(scale) : ImageBitmap   // via GraphicsLayer, pour un maillage 3D
Leaf (double page) = { front: PageHandle, back: PageHandle }   // verso de la feuille
```

- **En portrait :** face = page n, page révélée = n+1. Le **verso** affiche n en miroir et estompé (transparence du papier) ou un papier uni. C'est le choix de l'agent 07.
- **En paysage :** la feuille qui tourne porte la page de droite n à l'avant et la page de gauche n+1 au dos. La page révélée est la page de droite n+2. Comme le paginateur produit des **pages indépendantes**, ce cas ne pose aucune difficulté.
- **Les images pieuses (09)** sont des **intercalaires** insérés dans la `PageSequence` à une `Position` donnée. Elles ne perturbent pas la pagination du texte.
- **Aucune mise en page pendant le geste** : le moteur garantit que n-1, n et n+1 sont mesurées d'avance.

### 3.5 Pagination et localisateurs

**Algorithme du paginateur** (glouton, par section) :
1. Chaque bloc donne des « lignes » (hauteur, ligne de base) et des contraintes : non sécable, `keepWithNext`, minimum de 2 lignes en orpheline et 2 en veuve.
2. On remplit la page. Si un bloc déborde, on cherche la dernière ligne L telle que L+1 ≥ orphelines et le reste ≥ veuves ; sinon le bloc passe à la page suivante. Un titre suivi de moins de 2 lignes est reporté.
3. Une image plus grande que la page est réduite ; sinon elle est reportée à la page suivante (pas de flottants en MVP).
4. Une page est une liste de **tranches** `(blockId, ligneDébut, ligneFin, y)`, plus `startPos` et `endPos`.
5. La double page découpe deux pages de demi-largeur, avec option « une section commence sur une page de droite ».

**Clé de pagination :** une empreinte de `(rev du contenu, version du moteur, police, taille, interligne, marges, taille de page en px, césure, justification, échelle de police système)`. Avec la même clé, on obtient les mêmes pages, et le cache SQLite des sauts de page rend l'ouverture instantanée et les folios stables d'une session à l'autre. Quand la clé change, on re-pagine la section courante d'abord, **en gardant l'ancre de lecture** (premier caractère visible), puis le reste en arrière-plan.

**Pagination « stable selon la taille de police » : ce qui est possible et ce qui ne l'est pas.** Dans un texte reflowable, les numéros de page **changent forcément** avec la taille de police. On garantit en revanche :
- que la position de lecture est préservée au caractère près ;
- que la pagination est identique pour des réglages identiques ;
- que les folios sont affichés **par section** (« p. 3 / 11 »), avec une barre de progression globale calculée sur les caractères.

L'autre voie consiste à afficher des « pages de référence » fixes. C'est la décision D2.

**Position et ancre** (modèle commun à 09, 08 et 14) :

```
Position = { pack, packRev, doc, block, offset }            // signets, dernière lecture, rubans
Anchor   = { start: Position, end: Position,
             quote: { prefix(32), exact, suffix(32) }, blockRev }  // notes, surlignages
```

**Ré-ancrage après une mise à jour du paquet :**
1. Si `rev` est inchangé, on ancre directement.
2. Si le bloc a été redirigé, on suit l'identifiant.
3. Si le texte du bloc a changé, on cherche `exact` dans le bloc près de l'ancien décalage, puis on fait une recherche floue de type Bitap (diff-match-patch).
4. Sinon, on cherche dans le document, puis dans le paquet.
5. À défaut, l'annotation devient **orpheline** et apparaît dans une liste « à replacer ». On ne la perd jamais.

**Export :** vers le Web Annotation, profil EPUB Annotations (`FragmentSelector` = identifiant de bloc, `TextQuoteSelector`, `TextPositionSelector`), et vers le Locator Readium (`href` = document, `fragments` = [identifiant de bloc], `text.before/highlight/after`).

### 3.6 Colonnes latin/français en regard (interface avec l'agent 03)

Un seul algorithme couvre trois présentations :
- L'unité est la **rangée** : une paire de segments alignés (verset, phrase ou paragraphe), fournie par le contenu (03 et 11).
- **Côte à côte :** chaque cellule est mesurée à `(L - gouttière) / 2` avec **le même interligne** des deux côtés. La rangée a la hauteur maximale des deux cellules. Une rangée se coupe en bas de page à la même ligne k dans les deux colonnes. Une rubrique peut s'étendre sur toute la largeur.
- **Alterné :** le latin, puis le français en italique ou en corps réduit. C'est une simple suite de blocs.
- **En vis-à-vis :** en paysage, le latin occupe la page de gauche et le français la page de droite, paginés **en parallèle** par groupes de rangées. Le côté le plus court est complété par du blanc, comme dans les missels qui présentent les deux textes sur des pages en regard.
- **Avertissement :** en portrait sur un téléphone, une colonne fait environ 160 dp, soit 22 à 25 caractères par ligne. C'est trop étroit pour justifier. Il vaut mieux du **fer à gauche avec césure**, ou le mode alterné par défaut (décision D3).

### 3.7 Recommandations classées

**MVP**
1. Spike de **porte de décision** (2 à 3 semaines, builds en CI, téléphone physique). Quatre critères :
   - (i) paginer le livre I de l'Imitation et une messe de l'ordinaire selon les objectifs de §2.13 ;
   - (ii) lettrine justifiée sur 2 à 3 lignes ;
   - (iii) rangées latin/français coupées proprement ;
   - (iv) intégration avec le prototype de l'agent 07 : pages n-1, n, n+1 dessinées en direct et en `ImageBitmap`, et TalkBack qui lit une page.
2. Contrat de blocs v1 signé avec l'agent 11 (§3.2) : identifiants stables, `lang`, `rev`, marques en UTF-16.
3. Moteur natif :
   - blocs courants : titres, paragraphes, rubriques rouges, ℣ et ℟, versets, images, fleurons ;
   - justification et césure fr/la ; petites capitales issues de la police ; lettrine simple ;
   - mode paginé (portrait et double page en paysage) et mode défilant ;
   - thèmes clair, sombre et sépia ; taille de police, interligne et marges.
4. `PageSource` et `PageHandle` pour l'agent 07, plus les intercalaires pour l'agent 09.
5. `Position` (dernière lecture, signets et rubans) et cache de pagination.
6. Recherche FTS5 insensible aux accents, avec un index construit d'avance pour chaque paquet. Un résultat ouvre le bloc et surligne l'occurrence.
7. Accessibilité de base : sémantique par page, actions de page, proposition du mode défilant sous TalkBack, respect de « Supprimer les animations ».
8. TTS de base en français, phrase par phrase, avec la phrase en cours surlignée.
9. minSdk **26 au minimum**, à cause de la justification et de `onRangeStart`. À arbitrer avec l'agent 10 ; 28 si l'on veut androidx.pdf.

**V1**
1. Latin et français en regard : côte à côte, alterné et en vis-à-vis.
2. Sélection de texte avec poignées, surlignages et notes, ré-ancrage, export JSON (Web Annotation et Locator Readium).
3. TTS avancé : mot en cours (`onRangeStart`), Media3 en arrière-plan, stratégie pour le latin (D6), rubriques sautées.
4. Racinisation française dans la recherche, recherche dans le document courant, extraits (`snippet()`).
5. Lettrines ornées (vectorielles, agent 07), titres courants et folios, début de section sur une page de droite.
6. Fac-similé PDF avec `PdfRenderer` ou androidx.pdf, l'effet de page s'appliquant aux bitmaps.
7. Macrobenchmark, JankStats et Baseline Profiles en CI (agent 10).

**Plus tard**
1. Import d'EPUB externes via Readium : module isolé, navigateur Compose une fois stabilisé, effet de page limité au glissement ou à une capture.
2. Export EPUB 3 du contenu (à partir du format canonique de l'agent 11).
3. Césure du latin liturgique (tirets conditionnels insérés par le pipeline).
4. Notation grégorienne : GABC converti en SVG par le pipeline, puis en `ImageVector` (agents 03 et 14).
5. Recherche par trigrammes (sous-chaînes) et racinisation latine.
6. Raffinements typographiques que seul le natif permet : alignement optique des marges (protrusion), équilibrage vertical des pages, ponctuation suspendue.

### 3.8 Points d'interface avec les autres agents

| Agent | Point d'interface |
|---|---|
| 01 Clairval | Conversion du HTML des lettres en blocs (via 11). Identifiants stables : `clairval.AAAA-MM.pN`. |
| 02 Ignace | Extraction du PDF vers des blocs. Option fac-similé (V1). Mode « retraite guidée » = séquence de documents. |
| 03 Missel | Rangées latin/français, rubriques, ℣ et ℟, assemblage ordinaire + propre, D3. |
| 04 Bible et Imitation | Identifiants de versets (de type OSIS), sections par chapitre, références « Jn 3,16 » vers une `Position`. |
| 05 Calendrier | Couleur liturgique comme jeton de thème, assemblage du jour. |
| 07 Tourne-page et papier | `PageSource` et `PageHandle` (§3.4). Polices : glyphes ℣ ℟ ✠, `smcp`, U+202F. Lettrines ornées. |
| 08 UX | Arbitrage des gestes (toucher en marge, glisser depuis le bord, appui long pour sélectionner, tirer vers le bas pour le ruban). Pincer = taille de police ? D2, D3. |
| 09 Signets et images | Modèle `Position`, intercalaires dans la `PageSequence`, ruban affiché sur la page qui contient l'ancre. |
| 10 Architecture | Modules (§3.3), pilote SQLite embarqué ou Room 3 (`@Fts5`), minSdk, CI, benchmarks. |
| 11 Pipeline | Contrat de blocs (§3.2), index FTS5, colonne `stems`, redirections d'identifiants, espaces fines. |
| 12 Juridique | Readium BSD-3 ; pagecurl et TextFlow Apache-2.0 ; motifs de césure de la plateforme. **Ne pas réutiliser le code de Readest (AGPL), ni MuPDF (AGPL).** |
| 13 Benchmark | Comparer avec l'effet de page d'Apple Books et de Readest, et avec la typographie de KOReader. |
| 14 Prières | Mêmes blocs, rangées latin/français, séquences (rosaire, chemin de croix). |

---

## 4. Décisions à soumettre à l'utilisateur

| # | Question | Options | Recommandation |
|---|---|---|---|
| D1 | **Moteur principal** | (A) Natif Compose avec des blocs · (B) Readium/WebView avec EPUB · (C) foliate-js | **A**, après le spike de validation. B reste le plan de repli si le spike échoue. |
| D2 | **Sens des numéros de page** | (A) Folios recalculés par section, position de lecture exacte · (B) « Pages de référence » fixes, indépendantes de la police · (C) Pas de numéros, seulement la progression | **A**, avec une barre de progression globale |
| D3 | **Latin/français en portrait** | Côte à côte (étroit) · Alterné · Bascule de langue | **Alterné** en portrait. **Côte à côte** ou **en vis-à-vis** en paysage, au choix de l'utilisateur. |
| D4 | **Importer des EPUB ou PDF personnels ?** | Oui (module Readium, Plus tard) · Non | Non en MVP. À décider pour V2. |
| D5 | **Sources PDF** (Ignace) | Conversion en texte · Fac-similé seul · Les deux | **Les deux** : le texte d'abord, l'original en V1 |
| D6 | **Voix pour le latin** | Voix italienne · eSpeak NG si installé · Ignorer le latin | Laisser le choix, avec la voix italienne par défaut |
| D7 | **minSdk** | 26 · 28 · plus élevé | **26** (justification, TTS). 28 si androidx.pdf est adopté. |
| D8 | **Style des lettrines** | Lettre simple de la police · Lettrines ornées (images) · Les deux selon le module | Simple en MVP, ornées en V1 (07) |
| D9 | **Césure du latin** | Motifs de la plateforme (modernes) · Motifs liturgiques via le pipeline | Plateforme en MVP. Liturgique si le rendu déçoit. |

---

## 5. Risques et inconnues

| Risque ou inconnue | Gravité | Atténuation |
|---|---|---|
| **Effort du moteur natif** sous-estimé (sélection, cas limites, images, notes) | Élevée | Spike avec porte de décision. Périmètre de blocs restreint, puisque le contenu est le nôtre. Sélection simplifiée en MVP. |
| Sûreté de `TextMeasurer` entre threads non documentée | Moyenne | Une instance par thread ou `Paragraph()` direct, test de charge dans le spike |
| Césure par segment (`localeList`) et latin accentué (`hyph-la`) non vérifiés | Moyenne | Test dans le spike. Repli : tirets conditionnels insérés par le pipeline. |
| Lézardes de justification en colonnes étroites | Moyenne | Fer à gauche en colonnes étroites, césure, D3 |
| Glyphes ℣ ℟ ✠ absents des polices retenues | Moyenne | Vérification de couverture (07). Petite police de symboles ou repli « V/. R/. ». |
| Accessibilité d'un texte dessiné à la main | Moyenne | Sémantique construite à partir des blocs, tests TalkBack dès le spike |
| Mémoire sur les gros documents (Bible) | Moyenne | Sections par chapitre, LRU des mises en page, persistance des seuls sauts de page |
| Les mises à jour d'Android (police système, motifs de césure) changent la pagination | Faible | Polices embarquées, empreinte du build dans la clé de pagination |
| **Bibliothèques Compose tierces non maintenues** (pagecurl depuis 02/2024, TextFlow depuis 07/2024) | Faible | S'en inspirer sans en dépendre (les licences Apache permettent de les forker) |
| Navigateurs Compose de Readium expérimentaux | Faible (Plus tard) | Rester sur le Fragment legacy pour l'import, ou attendre la stabilisation |
| Identifiants de blocs instables côté pipeline | **Élevée** | Faire de la stabilité des identifiants une règle de l'agent 11, avec table de redirection et tests de non-régression |
| VM sans GPU : impossible de mesurer les performances localement | Moyenne | Builds en CI (GitHub Actions) et mesures sur un téléphone physique de milieu de gamme |

**Inconnues non vérifiées dans cette passe.** Le quota de recherche web partagé entre les agents a été épuisé en cours de mission. Les points suivants sont donc **à vérifier** :
- le niveau exact de la fragmentation de tables et de grilles dans Chromium (LayoutNG) en mode multicolonne ;
- la source des dictionnaires de césure de la WebView Android, en particulier pour le latin ;
- les niveaux d'API exacts des nouvelles fonctions texte de `PdfRenderer` (Android 15 et extension SDK 13) ;
- la couverture ℣ ℟ des polices candidates.

---

## 6. Sources

Les sources ont été consultées le 11 octobre 2026. Les versions et les dates sont celles des pages ou des dépôts.

**Readium**
- Readium Kotlin Toolkit, releases : https://github.com/readium/kotlin-toolkit/releases. 3.4.0 (2026-09-11), 3.3.0 (2026-06-02), 3.2.0 (2026-05-13), 3.1.2 (2025-09-29). Licence BSD-3-Clause.
- CHANGELOG et README (prérequis de la 3.4.0 : minSdk 24, compileSdk 37, Kotlin 2.4.20 ; tableau des fonctionnalités) : https://github.com/readium/kotlin-toolkit/blob/develop/CHANGELOG.md et https://github.com/readium/kotlin-toolkit. Lus dans un clone de la branche `develop`.
- Guide des navigateurs Compose (« still experimental and have not been battle tested ») : https://github.com/readium/kotlin-toolkit/blob/develop/docs/guides/navigator/web-navigators.md
- Parité des fonctionnalités entre le legacy et Compose : https://github.com/readium/kotlin-toolkit/blob/develop/docs/guides/navigator/epub-feature-parity.md
- Guide TTS : https://github.com/readium/kotlin-toolkit/blob/develop/docs/guides/tts.md
- Modèle `Locator` : `readium/shared/.../publication/Locator.kt` (dépôt ci-dessus). Spécification : https://readium.org/architecture/models/locators/
- Note de version 3.2.0 (tours de page animés) : https://blog.readium.org/release-note-kotlin-toolkit-version-3-2-0/
- Ticket #352, « Epub decorators are not accessible » (ouvert le 2023-05-08) : https://github.com/readium/kotlin-toolkit/issues/352
- Adaptateur PDFium : `com.github.marain87:PdfiumAndroid` 1.9.8 (`gradle/libs.versions.toml` de Readium).
- readium/ts-toolkit, navigator 2.12.1 (2026-10-09) : https://github.com/readium/ts-toolkit
- readium/readium-js, archivé (dernier push le 2022-04-11) : https://github.com/readium/readium-js

**foliate-js et Readest**
- foliate-js (MIT, sans release, dernier commit `78914ae` du 2026-05-01) : https://github.com/johnfactotum/foliate-js
- Readest v0.12.12 (2026-10-04, AGPL-3.0) : https://github.com/readest/readest
- Readest PR #4940, « slide and page curl turn animations » (2026-07-05) : https://github.com/readest/readest/pull/4940
- Readest PR #5217, alignement sur le doigt (2026-07-19) : https://github.com/readest/readest/pull/5217
- Readest PR #5291, réactivité des gestes et surfaces spéculatives (2026-07-23) : https://github.com/readest/readest/pull/5291
- Readest PR #6107, pliure à la reliure en double page (2026-09-07) : https://github.com/readest/readest/pull/6107
- Readest PR #6246, effet de page sur PDF et mise en page fixe (2026-09-17) : https://github.com/readest/readest/pull/6246
- Readest ticket #6670, scintillement de graisse pendant l'animation (2026-10-06, ouvert) : https://github.com/readest/readest/issues/6670

**Jetpack Compose et texte Android**
- Correspondance de la BOM Compose (2026.09.00 = UI 1.12.1) : https://developer.android.com/develop/ui/compose/bom/bom-mapping
- Notes de version de Compose UI (1.12.1 du 2026-09-09 ; 1.13.0-beta01 du 2026-10-07) : https://developer.android.com/jetpack/androidx/releases/compose-ui
- « Style paragraph » (LineBreak, Hyphens ; page mise à jour le 2026-10-01) : https://developer.android.com/develop/ui/compose/text/style-paragraph
- Correspondance Hyphens et Justify vers les constantes de `Layout` : https://github.com/androidx/androidx/blob/androidx-main/compose/ui/ui-text/src/androidMain/kotlin/androidx/compose/ui/text/AndroidParagraph.android.kt
- `TextMeasurer` (cache de 8 par défaut) : https://github.com/androidx/androidx/blob/androidx-main/compose/ui/ui-text/src/commonMain/kotlin/androidx/compose/ui/text/TextMeasurer.kt
- GraphicsLayer, `record` et `toImageBitmap` (Compose 1.7 et plus) : https://developer.android.com/develop/ui/compose/graphics/draw/modifiers
- Android 13, césure jusqu'à 200 % plus rapide : https://developer.android.com/about/versions/13/features
- Motifs de césure AOSP (dont `TeX/la/Android.bp`, qui ne compile pas le latin liturgique ; `hyph-la` v3.201, MIT) : https://android.googlesource.com/platform/external/hyphenation-patterns/
- Enregistrement des césures fr (2,3) et la (2,2) : https://android.googlesource.com/platform/frameworks/base/+/refs/heads/main/core/jni/android_text_Hyphenator.cpp
- `WebView.enableSlowWholeDocumentDraw()` (référence, coût du dessin complet) : https://developer.android.com/reference/android/webkit/WebView
- oleksandrbalan/pagecurl v1.5.1 (2024-02-09, Apache-2.0) : https://github.com/oleksandrbalan/pagecurl
- oleksandrbalan/textflow v1.2.1 (2024-07-20, Apache-2.0) : https://github.com/oleksandrbalan/textflow

**Recherche et stockage**
- androidx.sqlite (2.7.1 du 2026-09-09 ; 2.8.0-alpha01) : https://developer.android.com/jetpack/androidx/releases/sqlite
- Options de compilation de `sqlite-bundled` (`-DSQLITE_ENABLE_FTS5`) : https://github.com/androidx/androidx/blob/androidx-main/sqlite/sqlite-bundled/build.gradle
- Room 3 (3.0.0 du 2026-07-01, 3.0.3 du 2026-09-09, `@Fts5` depuis l'alpha02) : https://developer.android.com/jetpack/androidx/releases/room3
- SQLite FTS5 (unicode61 `remove_diacritics`, trigram, `fts5_tokenizer_v2`, `highlight` et `snippet`) : https://sqlite.org/fts5.html

**PDF**
- androidx.pdf 1.0.0-beta01 (2026-08-26) : https://developer.android.com/jetpack/androidx/releases/pdf
- `PdfRenderer` (référence) : https://developer.android.com/reference/android/graphics/pdf/PdfRenderer

**Annotations**
- W3C EPUB Annotations 1.0, Working Draft du 2026-09-20 : https://www.w3.org/TR/2026/WD-epub-anno-10-20260920/
- Annonce de la FPWD (février 2026) : https://www.w3.org/news/2026/first-public-working-draft-and-supporting-notes-epub-annotations-1-0

**Références d'expérience**
- Apple Books, réglage « Curl » contre « Slide » (iOS 16.4) : https://ios.gadgethacks.com/how-to/get-page-turning-curl-animation-back-apple-books-for-iphone-and-ipad-0385329
