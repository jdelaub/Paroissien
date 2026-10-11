# 11 — Chaîne de contenu et modèle canonique unifié

> Agent 11, transversal CONTENU. Rapport de planification rédigé le 11 octobre 2026.
> Périmètre : le modèle sémantique commun à toutes les sources, le format physique des paquets, la chaîne de production `content/`, l'indexation de la recherche. Le détail de chaque source revient aux agents 01 à 04 et 14, le moteur de rendu à l'agent 06, les signets à l'agent 09 et l'architecture Android à l'agent 10.
>
> **Légende de fiabilité** : **[V]** vérifié dans une source citée en §6 ; **[M]** mesuré localement dans la VM (Python 3.12.3, SQLite 3.45.1, scripts jetables dans le scratchpad, rien d'installé) ; **[S]** supposé ou de mémoire, à confirmer.
>
> Le quota de recherche web partagé entre les agents s'est épuisé en cours de route. Quelques points sont donc restés au stade [S], et §5 les signale.

---

## 1. Résumé

- **Un seul modèle pour tout.** Une *Publication* contient un arbre de *Divisions* de profondeur libre (partie, livre, chapitre, jour, messe…), puis une liste ordonnée de *Blocs*. Chaque bloc porte un texte par langue, ce qui fait de la « paire bilingue » un bloc à deux langues et non un type à part. La lettrine devient un attribut du paragraphe. Le texte enrichi suit une petite grammaire JSON d'« inlines » (italique, rouge, petites capitales, ℣/℟ sémantiques, appels de note, renvois, changements de langue).
- **Des identifiants stables par construction, jamais déduits à l'exécution.** On utilise une clé naturelle quand le texte a un système de référence (`jhn.3.16`, `ex.230`, `sancti.12-08.introitus`). Sinon, l'identifiant opaque est frappé une fois puis conservé dans le JSON canonique commité. Un réalignement flou à chaque réingestion le reporte d'une version à l'autre, une table `id_alias` sert de redirection ou de pierre tombale, et la CI échoue dès qu'un identifiant publié disparaît sans alias.
- **Ancres compatibles W3C et Readium.** Une ancre se compose de `(publication, bloc, langue, début, fin en points de code, citation exacte avec contexte, révision du bloc)`. Elle survit aux corrections de texte grâce au repli sur la citation, comme le fait Hypothesis.
- **Format physique : une base SQLite par paquet**, ouverte en lecture seule. Elle contient des blocs JSON compacts et un index FTS5 précalculé à contenu externe. Ce format reste neutre vis-à-vis du rendu : le rendu natif Compose consomme le JSON directement, et un rendu WebView se contente d'une sérialisation XHTML triviale. EPUB n'est retenu que comme **sortie secondaire**, si l'agent 06 choisit Readium. Le ZIP de JSON sert de format *source* dans le dépôt, pas de format d'exécution.
- **Mesures [M]** sur 2,1 Mo de texte français : un paquet complet pèse environ 3 fois le texte brut et environ 0,84 fois une fois compressé en gzip. L'index FTS5 `detail=full` ajoute environ 36 % au fichier et environ 48 % au téléchargement. Sa reconstruction prend environ 0,1 s dans la VM, si bien qu'on peut aussi le reconstruire sur l'appareil. Deux constructions successives donnent un **fichier bit à bit identique**.
- **FTS5 n'est pas garanti dans le SQLite du système Android.** Il l'est en revanche dans `androidx.sqlite:sqlite-bundled` (SQLite 3.53.4, `SQLITE_ENABLE_FTS5` [V]), et Room 3 ajoute `@Fts5`. Je recommande à l'agent 10 le `BundledSQLiteDriver` pour les paquets, avec la même version de SQLite épinglée côté chaîne via APSW 3.53.4.x [V].
- **La recherche s'appuie sur un pliage fait à la construction**, identique en Python et en Kotlin et testé par des vecteurs communs : NFKC, minuscules, suppression des diacritiques, œ→oe, æ→ae, j→i, v→u. Il est vérifié [M] que `unicode61 remove_diacritics 2` **ne plie pas** œ/æ ni les ligatures de PDF. Pas de racinisation au MVP : la recherche par préfixe et par expression suffit.
- **La chaîne de production est écrite en Python avec uv**, pas en script Kotlin. Elle est légère pour une VM à 2 cœurs et dispose du meilleur écosystème pour le scraping et le PDF. Le contrat avec l'application passe par des JSON Schema générés et un « paquet golden » lu par les tests Android. Les étapes sont `fetch → .cache/raw (jamais commité) → normalize → data/ (JSON canonique commité) → validate → package → publish`.
- **Publication** : les paquets `.sqlite.gz` vont dans les GitHub Releases (moins de 2 Gio par fichier, sans limite de bande passante annoncée [V]) et le `catalog.json` dans GitHub Pages ou une ressource « latest ». La CI lance un cron mensuel pour Clairval, qui **ouvre une PR à relire** au lieu de publier automatiquement. Au MVP, on remplace le paquet entier ; les paquets « supplément » viennent en V1.
- **Décisions clés à trancher** (§4) : commit du texte des sources non libres dans un dépôt public ou non, hébergement GitHub ou indépendant, index précalculé ou reconstruit à l'installation, FTS5 avec pilote embarqué ou FTS4 du système, normalisation typographique.

---

## 2. Constats détaillés

### 2.1 Ce que le modèle doit couvrir : inventaire par module

| Module | Structure naturelle | Système de référence natif | Particularités qui contraignent le modèle |
|---|---|---|---|
| Lettres de Clairval | Lettre → (intertitres) → paragraphes, notes, citations | Date ou numéro de lettre ; **aucun** numéro de paragraphe [S] | Corpus qui grandit chaque mois. HTML susceptible d'être reformaté, d'où un risque de « churn » des identifiants. Plusieurs langues non alignées paragraphe par paragraphe [S]. |
| Exercices de saint Ignace | Annotations → Semaines → jours, exercices → préambules, points, colloque | **Numéros marginaux traditionnels 1–370** [S] (l'édition Ganss confirme une numérotation traditionnelle de sections [V], mais pas son étendue exacte) | Mode « retraite guidée » : un parcours qui pointe vers des plages de blocs. |
| Missel (1962, ordinaire, autres) | Ordinaire / Temporal / Sanctoral / Commun → messe → pièces (introït, collecte…) | Chemins de Divinum Officium du type `Sancti/12-08` plus sections `[Introitus]` [S, à confirmer par l'agent 03] | Latin et français en regard, rubriques rouges, rôles ℣/℟/S/P, **assemblage à l'exécution** (ordinaire + propre du jour), couleur liturgique. |
| Bible | Testament → livre → chapitre → verset | Codes de livres USFM (`GEN`, `TOB`, `SIR`, `1MA`… deutérocanoniques inclus [V]) | Versification différente selon la Vulgate et la numérotation hébraïque, surtout dans les Psaumes. Notes de bas de page abondantes (Crampon). Renvois. |
| Imitation de Jésus-Christ | Livre → chapitre → versets | Livre.chapitre(.verset) selon l'édition [S] | Texte court et très structuré. |
| Prières classiques | Recueil → section → prière → strophes ou répons | Noms usuels (*Pater*, *Ave*, *Credo*…) | Bilingue, ℣/℟, signes de croix ✠. |
| Extensions (bréviaire, rosaire, chemin de croix, catéchisme…) | Variable | Variable (numéros du catéchisme, stations…) | Psalmodie (médiante `*`, flexe `†`), hymnes en vers, emplacements à remplir. |

**Conséquences pour le modèle.**
1. La hiérarchie « Partie → Chapitre → Bloc » doit avoir une **profondeur variable**. On l'obtient avec un arbre de divisions typées par un `kind`.
2. Plusieurs corpus ont des **clés naturelles** qu'il faut exploiter comme identifiants.
3. Le **multilinguisme aligné** (missel, prières) et le **multilinguisme non aligné** (Clairval) coexistent.
4. Le missel et le bréviaire imposent un mécanisme d'**emplacement** (bloc `slot`) résolu par le moteur liturgique, et non une copie statique.

### 2.2 Modèles existants examinés

| Modèle | Idée retenue | Pourquoi on ne l'adopte pas tel quel |
|---|---|---|
| **Portable Text** (Sanity, MIT [V]) | Des blocs JSON typés, des *spans* avec des *marks* et des définitions de marques. Le modèle prouve qu'un texte riche se sérialise proprement en JSON et se rend en natif ou en HTML. | Il est verbeux (`_type`, `_key` sur chaque span) et n'a pas de notion de langue par bloc ni de rôle liturgique. On en garde l'esprit avec une grammaire plus compacte. |
| **EPUB 3.3 / XHTML** (Recommandation W3C [V]) | Des `id` sur les éléments ; les CFI avec « assertions d'identifiant » pour la robustesse [V]. | Le contenu est en XHTML (ou SVG) [V] : un rendu natif Compose devrait réimplémenter un sous-ensemble HTML et CSS. La sémantique liturgique se réduit à des conventions de classes CSS. |
| **Readium Web Publication Manifest** | Un manifeste JSON simple (`metadata`, `readingOrder`, `resources`, `toc`) [V]. Le **modèle Locator** (`href`, `locations.fragments/progression/position`, `text.before/highlight/after`) [V] inspire notre ancre. | Il reste centré sur des ressources HTML, et son support n'est que « partiellement implémenté » dans le kit Kotlin [V]. |
| **TEI, OSIS, USX** | Ce sont les références savantes et bibliques. On leur emprunte les codes de livres et la notion de versification. | Trop lourds pour servir de format d'exécution. On les traite comme des formats d'**entrée** possibles (agent 04). |
| **Fichiers texte de Divinum Officium** | Des sections nommées qui fournissent des clés naturelles pour le missel et l'office. | Il s'agit d'un format de travail spécifique, que l'agent 03 convertira. |

### 2.3 Identifiants stables : ce que font les autres

- **EPUB CFI 1.1** (spécification recommandée par l'IDPF, 2017 [V]) adresse une position par un chemin d'éléments. Les assertions `[id]` « ajoutent de la robustesse » : si le chemin casse, le lecteur recherche l'identifiant [V]. Il en découle que **de bons `id` d'éléments suffisent**, et que le chemin n'est qu'un secours.
- **Le modèle d'annotation du W3C** (Recommandation de 2017 [V]) propose deux sélecteurs. Le `TextQuoteSelector` stocke `exact`, `prefix` et `suffix`. Le `TextPositionSelector` stocke `start` et `end` **en points de code Unicode, pas en unités de code**, sur un texte normalisé ; le W3C le juge « fragile » face aux modifications [V]. Le chaînage `refinedBy` correspond exactement à notre cas, un fragment (le bloc) affiné par une citation (le passage) [V].
- **Hypothesis** stocke trois sélecteurs et se replie dans l'ordre suivant : plage DOM, puis position, puis recherche floue guidée par le contexte de 32 caractères, puis recherche floue de la citation seule (diff-match-patch, Bitap) [V].
- **Readium Locator** combine `fragments` (l'identifiant du bloc), `progression` et `text.before/highlight/after` [V]. Notre ancre se convertit en Locator sans perte, ce qui garde la porte ouverte à Readium.

**Conclusion.** La stabilité repose sur trois étages :
1. un **identifiant de bloc stable**, garanti par la chaîne ;
2. des **décalages en points de code** à l'intérieur du bloc, valides tant que la `rev` du bloc n'a pas changé ;
3. une **citation et son contexte** pour réancrer de façon floue quand le bloc a été modifié.

Ces trois étages se complètent : aucun ne suffit seul.

### 2.4 Formats physiques comparés

Notation : ●●● très bon, ●● correct, ● faible.

| Critère | **SQLite + blocs JSON + FTS5** | SQLite + fragments XHTML + FTS5 | EPUB 3 | Readium WebPub (`.webpub`) | ZIP de JSON (NDJSON) |
|---|---|---|---|---|---|
| Rendu **natif Compose** | ●●● (JSON → `AnnotatedString` directement) | ● (il faut un analyseur HTML) | ● (HTML et CSS à réinterpréter) | ● | ●●● |
| Rendu **WebView** | ●● (sérialiseur JSON → XHTML d'environ 200 lignes [S]) | ●●● | ●●● (navigateur Readium mature : recherche, décorations, synthèse vocale [V]) | ●● (support Readium partiel [V]) | ●● |
| **Recherche** | ●●● (FTS5 précalculé ; requêtes en moins d'1 ms [M]) | ●●● | ● (parcours des ressources ; pas d'index standard) | ● | ● (indexation obligatoire sur l'appareil après import, mais bon marché [M]) |
| **Taille** (téléchargement, avec index) | ●● (≈ 0,84 × texte en gzip [M]) | ●● | ●●● (≈ 0,3 à 0,4 × texte en deflate, sans index [S]) | ●●● | ●●● (≈ 0,3 × [S]) |
| **Mise à jour incrémentale** | ●● (remplacement du paquet, ou paquets « supplément » ; l'extension *session* de SQLite ne prend pas en charge les tables virtuelles [V]) | ●● | ● (fichier entier) | ● | ●●● (petit ZIP delta) |
| **Accès aléatoire / mémoire** | ●●● (lecture page par page, pas de chargement global [V]) | ●●● | ●● (décompression par ressource) | ●● | ● (il faut importer avant de lire) |
| **Extensions tierces** | ●● (format ouvert, mais l'outillage est à fournir) | ●● | ●●● (Sigil, Calibre, pandoc…) | ●● | ●●● (JSON validé par un schéma) |
| **Intégrité et identification** | ●●● (un seul fichier, `application_id`, `quick_check` [V]) | ●●● | ●● | ●● | ●● |
| **Sémantique liturgique** (rôles, bilingue, emplacements) | ●●● (types et attributs explicites) | ●● (classes CSS) | ● (conventions) | ● | ●●● |

**Lecture du tableau.**
- Le **ZIP de JSON** est le meilleur format d'*échange et de source* : il se lit dans un diff et se valide par un schéma. **SQLite** est le meilleur format d'*exécution*. Toute la question est de savoir où se fait la conversion : en CI (recommandé) ou sur l'appareil (possible en V1 pour les extensions tierces).
- Choisir **XHTML plutôt que JSON** dans SQLite n'aurait de sens que si l'agent 06 retenait la WebView de façon définitive. Le JSON garde les deux voies ouvertes à coût quasi nul.
- **EPUB** n'apporte un gain que si l'on adopte le navigateur Readium. Dans ce cas, on ajoute une étape `package-epub` qui produit l'EPUB à partir du même JSON canonique, avec `id` XHTML = identifiant de bloc. Ce n'est pas un changement de modèle.

**Conditions à transmettre à l'agent 06 (rendu)**

| Si l'agent 06 choisit… | Ce que le contenu fournit | Ce qu'il reste à faire côté application |
|---|---|---|
| **Rendu natif Compose** (pagination maison) | Blocs d'une unité de lecture triés par `seq`, inlines JSON, langue par bloc et par span (pour la césure et la synthèse vocale), `char_start` pour la progression | Inlines → `AnnotatedString`, pagination, lettrine et ℣/℟ dessinés selon la police |
| **WebView maison** | Idem | Sérialiseur Kotlin bloc → XHTML (`id`=bloc, `class`=type ou rôle, `lang`), feuille CSS, pont JavaScript pour les sélections |
| **Navigateur Readium** (EPUB) | Étape supplémentaire `package-epub` dans la chaîne (ou génération dans l'application) | Conversion `Anchor` ↔ `Locator` (`fragments: ["#<blockId>"]` et `text`). La capacité de Readium à ouvrir une publication construite en mémoire depuis SQLite reste à vérifier [S]. |

### 2.5 Mesures locales [M]

Les mesures ont été faites avec des scripts jetables dans le scratchpad, Python 3.12.3 et SQLite 3.45.1 du système, sans rien installer. Le corpus de substitution est constitué des pages de manuel françaises de la VM : 961 paragraphes, 2,17 Mo. C'est un texte technique aux paragraphes longs : **les ordres de grandeur sont fiables, les chiffres exacts devront être remesurés sur un vrai corpus** (Clairval, Crampon).

**Tokenisation `unicode61`** (requêtes et vocabulaire via `fts5vocab`) :

| Entrée | `remove_diacritics 0` | `remove_diacritics 1` et `2` | Remarque |
|---|---|---|---|
| `Cœur coeur ŒUVRE` | `cœur`, `coeur`, `œuvre` | `cœur`, `coeur`, `œuvre` | **œ n'est pas plié** : cœur ≠ coeur |
| `Ægypti aegypti` | `ægypti`, `aegypti` | idem | **æ n'est pas plié** |
| `l’Église l'Église aujourd’hui` | `l`, `église`, `aujourd`, `hui` | `l`, `eglise`, `aujourd`, `hui` | Les deux apostrophes séparent les mots : l'élision est bien traitée |
| `℣. Dóminus vobíscum. ℟.` | `dóminus`, `vobíscum` | `dominus`, `vobiscum` | ℣/℟ (catégorie So) sont ignorés, ce qui convient |
| `Iesus Jesus vivere uiuere` | 4 jetons distincts | 4 jetons distincts | Pas de pliage i/j ni u/v |
| `ﬁdèle` (ligature de PDF) | `ﬁdèle` | `ﬁdele` | **La ligature ﬁ n'est pas pliée** |

Le tokeniseur `trigram remove_diacritics 1` trouve bien « therese » dans « Thérèse » sous SQLite 3.45.

**Taille de l'index FTS5** (table à contenu externe, après `optimize` et `VACUUM`) :

| Variante | Fichier | Surcoût par rapport au texte seul | Compressé (deflate 9) |
|---|---|---|---|
| Texte seul, sans FTS | 2,43 Mo | — | 0,62 Mo |
| `detail=full` (expressions et NEAR) | 3,30 Mo | **+36 %** | 1,16 Mo |
| `detail=column` | 2,92 Mo | +20 % | 0,80 Mo |
| `detail=none` | 2,68 Mo | +10 % | 0,77 Mo |
| `detail=full` + `prefix='2 3'` | 4,29 Mo | +77 % | — |

La documentation de SQLite donne un ordre de grandeur cohérent : 743 Mio en `full`, 340 en `column` et 134 en `none` pour 1 636 Mio de courriels [V].

**Paquet réaliste** (JSON d'affichage, copie pliée pour la recherche et FTS) : 6,44 Mo, soit **2,97 × le texte**, et 1,83 Mo en gzip, soit **0,84 × le texte**. Sans index FTS, on descend à 1,24 Mo en gzip (0,57 ×) : **reconstruire l'index sur l'appareil réduit le téléchargement d'environ 32 %**.

**Temps** : `rebuild` de l'index sur 2,1 Mo en 0,09 s, `optimize` en 0,03 s, quatre requêtes (préfixe, NEAR, expression, AND) en 0,8 ms au total. Un téléphone milieu de gamme serait plusieurs fois plus lent [S], mais resterait sous la seconde.

**Déterminisme** : deux constructions successives du même paquet donnent le **même SHA-256**. L'en-tête SQLite contient pourtant un compteur de modifications (décalage 24) et le `SQLITE_VERSION_NUMBER` de la dernière bibliothèque qui a écrit le fichier (décalage 96) [V]. Le hash de fichier ne vaut donc qu'**à version de SQLite épinglée**. D'où un second hash, « logique », calculé sur le JSON canonique.

### 2.6 FTS5 sur Android

- **Le SQLite du système n'offre pas de garantie.** Aucune source officielle n'affirme que le SQLite du framework est compilé avec FTS5. Les sources communautaires évoquent une disponibilité dépendante du fabricant, et Room ne gérait historiquement que FTS3 et FTS4 [V : CommonsWare, forum B4X].
- **Le pilote embarqué AndroidX inclut FTS5** : `sqlite-bundled` compile SQLite **3.53.4** avec `SQLITE_ENABLE_FTS5`, `FTS4`, `JSON1`, `RTREE`, `MATH_FUNCTIONS`… [V, build.gradle à HEAD]. Le pilote accepte des drapeaux d'ouverture, dont la lecture seule, depuis la version 2.5.0-alpha04 [V].
- **Room 3.0** (stable 3.0.3 au 9 septembre 2026) ajoute `@Fts5` à partir de 3.0.0-alpha02, avec les tokeniseurs ASCII et trigram et l'option `detail` [V]. Il **exige** un `SQLiteDriver` [V], et la documentation de `@Fts5` précise que la disponibilité dépend du pilote, en citant `BundledSQLiteDriver` [V via la recherche]. Nos paquets n'ont toutefois **pas besoin de Room** : ce sont des bases externes en lecture seule, interrogées en SQL brut par le même pilote.
- **Compatibilité de format** : FTS5 existe depuis 3.9.0. Le format ne change (v5) que si l'option `secure-delete` a servi à des suppressions [V], ce qui n'arrive jamais dans un paquet construit d'un bloc. Règle : **la version de SQLite de la chaîne doit être inférieure ou égale à celle de l'application**. APSW suit les versions de SQLite (APSW 3.53.4.0 ↔ SQLite 3.53.4) [V], ce qui permet un alignement exact.
- **Solution de repli** si l'agent 10 refuse le pilote embarqué : FTS4, présent dans le SQLite du système, avec le même tokeniseur `unicode61` sur la même colonne pliée. Comme le pliage est fait à la construction, la seule perte concerne `bm25()`, que FTS4 n'a pas. Il faudrait calculer un score via `matchinfo` [S].

### 2.7 Tokenisation du français et du latin

- **Accents** : `remove_diacritics 2` les traite tous [V][M]. On les retire aussi dans le pliage, par sécurité et pour que le pliage Kotlin des requêtes soit autonome.
- **Ligatures et digrammes** : œ/æ (cœur, sœur, œuvre ; *cælum*, *Ægyptus*), ǽ du latin liturgique (se décompose en æ + accent [M]), ligatures typographiques issues des PDF (ﬁ, ﬂ). **Aucune n'est pliée par FTS5 [M]**, d'où un pliage explicite.
- **Graphies latines** : i/j (*Iesus*/*Jesus*, *eius*/*ejus*) et u/v (*vivere*/*uiuere*) varient selon les éditions. On les plie (j→i, v→u). Appliqué aussi au français, ce pliage ne crée pratiquement pas de collision, puisqu'il s'applique de façon symétrique à l'index et à la requête [S, à valider sur un vrai corpus]. Ce choix figure en décision D5.
- **Élisions et traits d'union** : l'apostrophe droite comme la typographique sépare les mots [M]. « Jésus-Christ » donne deux jetons, et la requête par expression `"jesus christ"` le retrouve.
- **Espaces insécables** : la typographie française place une espace fine insécable (U+202F) devant `; : ! ?` et à l'intérieur des guillemets. **NFKC la transforme en espace simple [M]**. On applique donc NFKC **uniquement à la copie de recherche**, jamais au texte affiché, qui reste en NFC.
- **Racinisation** : FTS5 ne fournit que Porter, pour l'anglais [V]. Snowball a un algorithme pour le français et une implémentation de l'algorithme latin de Schinke [V]. Pour l'utiliser, il faudrait le même raciniseur en Kotlin et une colonne racinisée supplémentaire. Ce n'est **pas nécessaire au MVP** : la recherche par préfixe automatique (`glori*` trouve *gloria*, *gloriae*, *gloriam*) couvre l'essentiel.
- **Mots vides** : aucun, car les expressions comme « Notre Père » doivent fonctionner.
- **Surlignage** : `highlight()` et `snippet()` de FTS5 travailleraient sur le texte plié. L'application surligne plutôt **dans le texte original**, en le retokenisant en Kotlin avec la même fonction de pliage, jeton par jeton.

### 2.8 Mise à jour incrémentale (nouvelle lettre chaque mois)

| Option | Principe | Pour | Contre |
|---|---|---|---|
| A. **Remplacement complet du paquet** | Le paquet `clairval-fr` est reconstruit chaque mois et l'application télécharge la nouvelle version | Extrêmement simple et robuste ; les identifiants stables rendent l'opération sans danger pour les signets | Retéléchargement de quelques Mo par mois [S : quelques centaines de lettres × environ 30 Ko ≈ 10 à 15 Mo de texte, soit 8 à 13 Mo en gzip avec index — à mesurer par l'agent 01] |
| B. **Paquets « supplément »** | Un mini-paquet `clairval-fr+2026-11` qui `extends` la base, puis une consolidation annuelle qui `supersedes` les suppléments | Téléchargement d'environ 100 Ko par mois ; même format ; aucune logique de fusion | La recherche porte sur plusieurs bases (au plus une douzaine par an ; `ATTACH` limité à 10 par défaut [V], donc plutôt des connexions séparées) |
| C. Delta binaire (bsdiff, `zstd --patch-from`) | Correctif entre deux fichiers SQLite | Petit en théorie | `optimize` réécrit l'arbre B de l'index, donc les deltas sont gros et fragiles [S] ; nécessite une bibliothèque native |
| D. Changesets de l'extension *session* | Changeset SQL appliqué sur l'appareil | Natif SQLite | **Les tables virtuelles ne sont pas prises en charge** [V], il faut reconstruire le FTS ; l'extension n'est pas activée par défaut [V] |
| E. Import de JSON dans une base locale unique | L'application ingère les publications dans sa propre base | Delta trivial, index global unique | Logique d'import et migrations de schéma sur l'appareil ; le paquet n'est plus un fichier immuable vérifiable |

**Choix** : A au MVP, avec dès le départ les champs `extends`/`supersedes` dans le manifeste, puis B en V1. L'option E reste une piste pour l'import d'extensions tierces (plus tard). C et D sont écartées.

### 2.9 Hébergement des mises à jour

- **GitHub Releases** : chaque fichier doit faire moins de 2 Gio ; 1 000 ressources au plus par release ; « pas de limite sur la taille totale d'une release ni sur la bande passante » [V]. L'URL stable `/releases/latest/download/<nom>` existe [V].
- **GitHub Pages** : site de 1 Go au plus, bande passante limitée à 100 Go par mois (limite souple), réponses 429 en cas d'abus [V]. Cela convient au `catalog.json`, mais **pas** au stockage des paquets.
- **Vie privée** : chaque téléchargement révèle l'adresse IP à GitHub. Aucun identifiant n'est envoyé, et les requêtes ne partent qu'à l'initiative de l'utilisateur, ou en vérification périodique sur Wi-Fi si l'utilisateur l'active. Le catalogue accepte **plusieurs URL** par paquet, ce qui permet d'ajouter un miroir indépendant (Codeberg, serveur personnel) sans changer l'application.
- **Paquets embarqués dans l'APK** : les paquets « cœur » (prières, ordinaire de la messe) pèsent moins de 1 Mo et doivent être disponibles hors ligne dès le premier lancement. Ils sont copiés depuis `assets/` vers le stockage de l'application au premier lancement, car SQLite ne peut pas ouvrir un fichier compressé dans l'APK [S].

### 2.10 Langage de la chaîne : Python (uv) ou script Kotlin

| Critère | Python + uv | Script Kotlin ou module JVM |
|---|---|---|
| Coût sur la VM (2 cœurs, 10 Go) | Faible (pas de JVM ni de Gradle) | JVM, Gradle et compilation de scripts : lourd et lent à démarrer |
| Scraping HTML | httpx, lxml, selectolax | jsoup (bon) |
| Extraction de PDF | pypdf, pdfminer.six (licences permissives [S]) ; **PyMuPDF est en AGPL, à éviter [S]** | PDFBox (bon) |
| SQLite épinglé avec FTS5 | **APSW** embarque sa propre copie de SQLite avec toutes les extensions sauf ICU, à la version de SQLite [V] | sqlite-jdbc (embarque SQLite [S]) |
| Partage du modèle avec l'application | Non, d'où le contrat JSON Schema et le paquet golden | Oui (module `model` partagé) |
| Reproductibilité | `uv.lock`, `uv sync --locked` qui échoue si le verrou n'est pas à jour [V] | Gradle lockfiles |

**Choix : Python avec uv.** Le seul vrai avantage de Kotlin, le partage des data classes, se compense par un **contrat de test** : l'application lit le paquet golden et les vecteurs de pliage produits par la chaîne.

---

## 3. Recommandations

### 3.1 Par phase

**MVP**
1. Adopter le **modèle canonique** de §3.2, au format 1.0, et le figer par JSON Schema.
2. Construire la chaîne Python `content/` (§3.6) : `fetch`, `normalize`, `validate`, `package`, `publish`, plus `diff`.
3. Utiliser des **paquets SQLite** (§3.3) avec un FTS5 précalculé sur une colonne pliée, `detail=full`, sans index de préfixe. Ils sont ouverts **en lecture seule** par le `BundledSQLiteDriver`, à condition que l'agent 10 valide ce choix.
4. Appliquer les **règles d'identifiants** (§3.2.4) avec la porte de CI « aucun identifiant perdu sans alias » et un rapport de churn dans chaque PR de contenu.
5. Fournir le **contrat d'ancre** (§3.2.5) à l'agent 09 : bloc, décalages en points de code, citation avec contexte et `rev`.
6. Écrire la **fonction de pliage** (§3.7) en Python et en Kotlin, avec `content/spec/folding-vectors.json` partagé.
7. Héberger les paquets sur **GitHub Releases** et `catalog.json` dans une ressource « latest » ou sur Pages, avec un SHA-256 obligatoire. Mettre à jour Clairval par **remplacement complet**.
8. Embarquer dans l'APK les **paquets cœur** (prières, ordinaire 1962) ; les autres se téléchargent à la demande.
9. Mettre en place les **tests golden** : instantanés par adaptateur, paquet `mini` couvrant tous les types de blocs, vecteurs de pliage et de `plainText`, test de stabilité des identifiants.

**V1**
1. Paquets **supplément** pour Clairval, puis consolidation annuelle.
2. **Signature Ed25519** du catalogue, avec la clé publique embarquée dans l'application.
3. Clairval en **plusieurs langues** (une publication par langue, reliées par `translationOf` quand c'est possible).
4. Blocs `slot` pleinement exploités par le missel et le bréviaire (agents 03 et 05) ; publication `itinerary` pour la retraite guidée (agent 02).
5. Option **« index reconstruit à l'installation »**, si les tailles réelles le justifient (environ −32 % de téléchargement [M]).
6. Mesures réelles de taille et de temps sur téléphone ; ajout de `prefix='3'` seulement si les recherches par préfixe sont lentes.
7. Outil `paroissien-pack` documenté pour les auteurs d'extensions tierces (espace de noms `ext.<auteur>.<nom>`).

**Plus tard**
1. Export **EPUB 3** à partir du JSON canonique, pour le partage ou si l'agent 06 adopte Readium.
2. **Import sur l'appareil** de paquets JSON tiers (validation, puis construction locale de la base et du FTS).
3. Racinisation française (Snowball) et latine (Schinke) dans une colonne dédiée ; synonymes (« St » / « Saint »).
4. Repères de pagination de l'édition imprimée (`{"pg":"123"}`) pour citer une page du livre papier.
5. Table de rétroliens (`link`) pour les renvois inverses ; catalogue OPDS 2.0.

### 3.2 Modèle canonique, format 1.0

#### 3.2.1 Hiérarchie

```
Paquet (unité de distribution : un fichier)
 └─ Publication (unité éditoriale : une lettre, un livre, la Bible, le Missel 1962)
     ├─ Division* (arbre de profondeur libre, kind ∈ {part, book, chapter, section, day, week,
     │             mass, office, letter, prayer, station, …}) — sert à la table des matières et à la navigation
     │    └─ is_unit = vrai : « unité de lecture », le flux paginé que le moteur de rendu charge d'un coup
     └─ Bloc* (liste plate ordonnée par seq ; chaque bloc appartient à exactement une unité de lecture)
          └─ textes : { langue → inlines }   (1 langue, ou plusieurs pour le texte en regard)
```

**Règles.**
- **Les ancres visent toujours une publication et un bloc, jamais un paquet.** On peut ainsi fusionner ou scinder des paquets sans casser les signets.
- **Les divisions sont invisibles au rendu.** Le moteur de rendu ne fait qu'afficher des blocs dans l'ordre ; les titres affichés sont des blocs `heading`. Une division porte un `toc_title` court pour la table des matières et une plage `seq_first..seq_last`.
- **`seq` n'est pas stable** d'une version à l'autre : seul `id` l'est. `seq` et `char_start` servent à la pagination, à la progression et aux requêtes de plage.
- **Clairval** : une lettre forme une publication (`clairval.fr.2024-03`), regroupée par une `series`. Un paquet contient toutes les lettres d'une langue.

#### 3.2.2 Types de blocs (vocabulaire fermé, versionné)

| Type | Correspond à la demande | Attributs | Remarques |
|---|---|---|---|
| `heading` | titre | `level` (1–6), `label?` (« Chapitre III ») | Indexé dans la colonne `head` (poids bm25 plus élevé) |
| `para` | paragraphe, **lettrine** | `dropCap: bool`, `style` ∈ {normal, intro, epigraph, signature, centered} | La lettrine est un **attribut**. En faire un bloc à part couperait le mot et casserait la recherche. |
| `verse` | verset | `n` (« 16 »), `p: bool` (début de paragraphe), `ref?` (`JHN.3.16`) | Rendu en continu par paragraphes (numéros en exposant), mais un verset reste un bloc ancrable |
| `rubric` | rubrique (rouge) | — | Exclue de la synthèse vocale par défaut ; on peut aussi la masquer |
| `versicle` | ℣/℟ | `role` ∈ {V, R, S (célébrant), P (peuple), M (ministre), ALL} | **Le rôle est sémantique** : le glyphe ℣ (U+2123), ℟ (U+211F) ou « V/ » est choisi par le moteur de rendu selon la police [M] |
| `quote` | citation | `source?` (renvoi), `attribution?` | Bloc de citation |
| `stanza` | (hymnes, psaumes) | `kind` ∈ {hymn, psalm, poem} | Lignes séparées par `{"br":1}` ; médiante et flexe via `sym` |
| `note` | note | `placement` ∈ {foot, end, margin}, `label` | Appelée par l'inline `{"note": "<id>"}` |
| `image` | image | `href` (ressource), `alt{}`, `credit?`, `license?` | Légende dans `t` ; image pieuse possible |
| `separator` | séparateur | `kind` ∈ {rule, asterism, fleuron, space} | Pas de texte |
| `item` | (listes, points d'oraison) | `marker?` (« 1er point »), `level` | — |
| `slot` | (emplacement liturgique) | `slot` (ex. `proprium.introitus`), `fallback?` | Résolu à l'exécution par le moteur liturgique (agents 03 et 05) |
| *(inconnu)* | — | — | Une application ancienne affiche le texte comme un `para` : compatibilité ascendante au sein d'un même format majeur |

**Paire bilingue** : *n'importe quel* bloc porte `t: { "la": […], "fr": […] }`. La disposition (latin seul, français seul, en regard, alterné) est un **réglage de lecture**, pas une propriété du contenu. Une publication déclare `aligned: true` quand ses langues sont alignées bloc par bloc.

**Attributs d'extension** : toute clé `x-…` est conservée par la chaîne et ignorée par le moteur de rendu. Cela permet d'expérimenter sans changer de format.

#### 3.2.3 Grammaire des inlines (JSON compact)

```
inline :=
    "texte"                                    texte simple (NFC)
  | {"m": ["i"|"b"|"sc"|"red"|"sup"|"sub"|"sp"], "c": [inline…]}   marques imbriquables
  | {"lang": "la", "c": [inline…]}             changement de langue (césure, synthèse vocale)
  | {"note": "<blockId>", "l": "3"}            appel de note
  | {"link": "<ref>", "c": [inline…]}          renvoi : "bible:JHN.3.16-18", "pub:imitation.fr#1.3.2", "https://…"
  | {"sym": "V"|"R"|"cross"|"star"|"dagger"}   symbole liturgique sémantique (℣ ℟ ✠ * †)
  | {"num": "16"}                              numéro en ligne (verset ou paragraphe inclus dans un paragraphe)
  | {"br": 1}                                  saut de ligne (vers, psaumes)
```

**Texte brut canonique** (`plainText`, qui sert aux décalages, à la synthèse vocale et aux extraits) : concaténation des chaînes. `sym` compte comme son caractère canonique (1 point de code), `br` comme `\n` ; `note` et `num` ne comptent pas. Les deux implémentations, Python et Kotlin, sont testées sur les mêmes vecteurs.

Les renvois bibliques sont **abstraits** (`bible:JHN.3.16`) : l'application les résout vers la traduction choisie par l'utilisateur, grâce à la table de versification de l'agent 04.

#### 3.2.4 Identifiants : règles

- **Publication** : `^[a-z0-9]+(\.[a-z0-9-]+)+$`, sans version. Exemples : `clairval.fr.2024-03`, `bible.crampon`, `vulgata.clementina`, `missale.1962`, `imitation.fr.<traducteur>`, `ignace.exercices.fr`, `preces.classicae`.
- **Bloc et division** : `^[a-z0-9][a-z0-9._-]{0,63}$`, unique dans la publication.
  - **Clé naturelle** si possible : `jhn.3.16` (code USFM en minuscules), `ex.230` (numéro marginal), `sancti.12-08.introitus.1`, `ordo.confiteor.2`, `pater`.
  - **Sinon, identifiant opaque frappé** : `p` + 6 caractères base32 de Crockford aléatoires (`p7k2m9q`), créé **une seule fois** à la première ingestion puis conservé dans le JSON canonique commité. On ne le calcule jamais à partir de la position ou du hash à l'exécution.
- **Immuabilité** : un identifiant publié n'est **jamais réutilisé** pour un autre contenu. S'il disparaît, il passe dans `id_alias` avec soit le bloc successeur (fusion, déplacement), soit `null` (pierre tombale, suppression réelle).
- **`rev`** : entier propre à chaque bloc, incrémenté quand son texte canonique change (le hash change).
- **Réalignement à la réingestion** (`ids.py`) :
  1. diff de Myers sur la suite des hash de blocs (ancienne puis nouvelle version) ;
  2. dans chaque bloc de lignes modifiées, appariement flou par similarité normalisée ≥ 0,8 (rapidfuzz), en respectant l'ordre ;
  3. en cas de **scission**, l'ancien identifiant reste sur le fragment le plus similaire et le reste reçoit un nouvel identifiant ;
  4. en cas de **fusion**, l'identifiant du bloc le plus similaire est conservé et les autres reçoivent un alias vers lui ;
  5. production d'un **rapport de churn** (identifiants conservés, modifiés, aliasés, perdus), avec un seuil bloquant en CI (par exemple plus de 2 % d'identifiants aliasés dans une publication = relecture humaine obligatoire).
- **Variante sans texte commité** (si le juridique interdit de commiter le texte, décision D1) : on commite seulement un **registre** `ids/<pub>.jsonl` qui contient l'identifiant, le hash et une **signature MinHash** de 64 entiers [S]. Cela suffit à l'appariement flou sans exposer le texte.

#### 3.2.5 Contrat d'ancre (à valider par l'agent 09)

```json
{
  "pub": "clairval.fr.2024-03",
  "block": "p7k2m9q",
  "lang": "fr",
  "start": 120, "end": 168,
  "quote": { "exact": "…48 caractères sélectionnés…", "prefix": "…32 caractères…", "suffix": "…32 caractères…" },
  "blockRev": 1,
  "pubRevision": 3
}
```

- `start` et `end` sont exprimés en **points de code** du `plainText` en NFC, comme dans le W3C [V]. En Kotlin, on utilise `offsetByCodePoints`.
- Les décalages ne sont valides que si `blockRev` est égal à la `rev` actuelle. Sinon, on réancre de façon floue sur `quote` dans le bloc, puis dans l'unité de lecture. Si le bloc a disparu, on passe par `id_alias`.
- Un **signet** de page n'utilise que `pub`, `block` et éventuellement `start`. Une **image pieuse** glissée entre deux pages est une ancre de bloc.
- La conversion vers un **Readium Locator** est directe : `href` = unité, `fragments: ["#block"]`, `text.highlight/before/after` = `quote`.

#### 3.2.6 Métadonnées de publication

| Champ | Exemple | Obligatoire |
|---|---|---|
| `id`, `revision`, `kind` | `missale.1962`, `4`, `missal` | oui |
| `title{}`, `subtitle{}` | `{"la":"Missale Romanum","fr":"Missel romain"}` | `title` |
| `languages`, `primaryLanguage`, `aligned` | `["la","fr"]`, `la`, `true` | oui |
| `contributors[]` | `{name, role: author|translator|editor|compiler}` | non |
| `issued` | `1962` / `2024-03` | non |
| `series` | `{id: "clairval.fr", index: 2024.03}` | non |
| `source` | `{name, url, edition, retrieved, rawSha256}` | oui |
| `license` | `{spdx: "LicenseRef-Clairval-Permission", url, attribution, permissionRef}` | oui (identifiant SPDX, ou `LicenseRef-` avec lettres, chiffres, `.` et `-` [V]) |
| `approbation` | `{imprimatur: "…", date, diocese, nihilObstat}` | non (propre au contenu catholique ; à afficher) |
| `rite`, `versification`, `liturgicalCalendar` | `1962`, `vulg`, `…` | selon `kind` |
| `contentHash` | `sha256:…` du JSONL canonique | calculé |

#### 3.2.7 Exemple de JSON canonique (source dans le dépôt)

`content/data/preces.classicae/publication.json` contient les métadonnées et l'arbre des divisions. Les blocs sont rangés **une ligne par bloc** dans `blocks/<division>.jsonl`, ce qui donne des diffs Git lisibles. Les textes ci-dessous sont des formules liturgiques latines du domaine public, accompagnées d'une traduction écrite pour l'exemple.

```jsonl
{"id":"ordo.introibo.r1","type":"rubric","t":{"fr":["Le prêtre, au bas de l’autel, fait le signe de la croix."]}}
{"id":"ordo.introibo.1","type":"versicle","role":"S","t":{"la":[{"sym":"V"}," Introíbo ad altáre Dei."],"fr":[{"sym":"V"}," Je m’approcherai de l’autel de Dieu."]}}
{"id":"ordo.introibo.2","type":"versicle","role":"M","t":{"la":[{"sym":"R"}," Ad Deum qui lætíficat iuventútem meam."],"fr":[{"sym":"R"}," Du Dieu qui réjouit ma jeunesse."]}}
{"id":"p7k2m9q","type":"para","dropCap":true,"t":{"fr":["Bien chers amis, ",{"m":["i"],"c":["exemple de texte"]}," avec un appel de note.",{"note":"n1","l":"1"}]}}
{"id":"jhn.1.1","type":"verse","n":"1","p":true,"t":{"la":["In princípio erat Verbum…"]}}
```

### 3.3 Format du paquet : DDL SQL (format 1.0)

```sql
-- Paquet Paroissien — format 1.0
-- Ouvert en LECTURE SEULE sur l'appareil (mode=ro ; immutable=1 seulement pour les paquets
-- qui ne seront jamais réécrits, cf. avertissement SQLite sur immutable).
PRAGMA application_id = 1347572558;  -- 0x5052534E « PRSN » : identifie le fichier comme paquet
PRAGMA user_version   = 100;         -- format = majeur*100 + mineur
PRAGMA page_size      = 4096;
PRAGMA encoding       = 'UTF-8';
PRAGMA journal_mode   = DELETE;      -- pas de WAL dans un fichier distribué

-- Manifeste embarqué (copie de celui du catalogue) et informations de construction
CREATE TABLE pack_info (
  key    TEXT PRIMARY KEY,     -- id, version, format, built_at, content_hash, pipeline_commit, sqlite_version, manifest
  value  TEXT NOT NULL
) WITHOUT ROWID;

CREATE TABLE publication (
  rowid         INTEGER PRIMARY KEY,
  id            TEXT    NOT NULL UNIQUE,     -- 'clairval.fr.2024-03'
  revision      INTEGER NOT NULL,
  kind          TEXT    NOT NULL,            -- letter|book|bible|missal|prayers|exercises|office|itinerary|…
  primary_lang  TEXT    NOT NULL,            -- BCP 47
  languages     TEXT    NOT NULL,            -- JSON ["la","fr"]
  aligned       INTEGER NOT NULL DEFAULT 0,
  title         TEXT    NOT NULL,            -- JSON {"fr":"…"}
  sort_key      TEXT,                        -- tri en bibliothèque (date, numéro)
  meta          TEXT    NOT NULL,            -- JSON : contributors, source, license, approbation, series…
  content_hash  TEXT    NOT NULL,            -- sha256 du JSONL canonique (indépendant de SQLite)
  block_count   INTEGER NOT NULL,
  char_count    INTEGER NOT NULL             -- pour la progression globale
);

CREATE TABLE division (
  rowid         INTEGER PRIMARY KEY,
  pub_rowid     INTEGER NOT NULL REFERENCES publication(rowid),
  id            TEXT    NOT NULL,            -- stable
  parent_rowid  INTEGER REFERENCES division(rowid),
  ord           INTEGER NOT NULL,            -- rang parmi les frères
  depth         INTEGER NOT NULL,
  kind          TEXT    NOT NULL,
  number        TEXT,                        -- '3', 'III', 'JHN'
  toc_title     TEXT,                        -- JSON {"fr":"…"} : titre court de la table des matières
  is_unit       INTEGER NOT NULL DEFAULT 0,  -- 1 = unité de lecture (flux paginé)
  seq_first     INTEGER NOT NULL,
  seq_last      INTEGER NOT NULL,
  attrs         TEXT,                        -- JSON : couleur liturgique, rang, date, durée (retraite)…
  UNIQUE (pub_rowid, id)
);
CREATE INDEX division_parent ON division(pub_rowid, parent_rowid, ord);

CREATE TABLE block (
  rowid         INTEGER PRIMARY KEY,         -- interne, NON stable
  pub_rowid     INTEGER NOT NULL REFERENCES publication(rowid),
  id            TEXT    NOT NULL,            -- STABLE entre versions
  unit_rowid    INTEGER NOT NULL REFERENCES division(rowid),
  seq           INTEGER NOT NULL,            -- ordre du document (NON stable)
  type          TEXT    NOT NULL,            -- heading|para|verse|rubric|versicle|quote|stanza|note|image|separator|item|slot
  attrs         TEXT,                        -- JSON propre au type (dropCap, role, n, level, slot…)
  rev           INTEGER NOT NULL,
  hash          TEXT    NOT NULL,            -- 16 hex : détection rapide d'un changement
  char_start    INTEGER NOT NULL,            -- position cumulée (progression)
  char_len      INTEGER NOT NULL,
  UNIQUE (pub_rowid, id),
  UNIQUE (pub_rowid, seq)
);
CREATE INDEX block_unit ON block(unit_rowid, seq);

CREATE TABLE block_text (
  block_rowid   INTEGER NOT NULL REFERENCES block(rowid),
  lang          TEXT    NOT NULL,
  inlines       TEXT    NOT NULL,            -- JSON compact, grammaire §3.2.3 (NFC)
  PRIMARY KEY (block_rowid, lang)
) WITHOUT ROWID;

CREATE TABLE id_alias (
  pub_rowid     INTEGER NOT NULL REFERENCES publication(rowid),
  old_id        TEXT    NOT NULL,
  new_id        TEXT,                        -- NULL = pierre tombale
  since_rev     INTEGER NOT NULL,            -- révision de publication où l'alias est apparu
  PRIMARY KEY (pub_rowid, old_id)
) WITHOUT ROWID;

-- Ressources (images). Table AVEC rowid : WITHOUT ROWID est déconseillé pour les grosses lignes.
-- En deçà d'environ 100 Ko, lire depuis la base est plus rapide que depuis des fichiers séparés [V].
CREATE TABLE resource (
  rowid         INTEGER PRIMARY KEY,
  href          TEXT    NOT NULL UNIQUE,     -- 'img/sacre-coeur.webp'
  media_type    TEXT    NOT NULL,
  sha256        TEXT    NOT NULL,
  meta          TEXT,                        -- JSON : width, height, credit, license
  data          BLOB    NOT NULL
);

-- Recherche : documents pliés (contenu externe de l'index FTS5)
CREATE TABLE search_doc (
  rowid         INTEGER PRIMARY KEY,
  block_rowid   INTEGER NOT NULL REFERENCES block(rowid),
  lang          TEXT    NOT NULL,
  head          TEXT,                        -- texte plié (blocs heading)
  body          TEXT                         -- texte plié (autres blocs)
);
CREATE VIRTUAL TABLE search_fts USING fts5(
  head, body, lang UNINDEXED,
  content       = 'search_doc',
  content_rowid = 'rowid',
  tokenize      = 'unicode61 remove_diacritics 2',
  detail        = full
);
-- Construction : INSERT INTO search_fts(search_fts) VALUES('rebuild');
--                INSERT INTO search_fts(search_fts) VALUES('optimize');  puis VACUUM.
-- Variante V1 « index reconstruit à l'installation » : on livre search_doc sans exécuter 'rebuild'.

-- Requête type (la chaîne de requête est pliée en Kotlin avec la même fonction) :
-- SELECT p.id AS pub, b.id AS block, d.lang, bm25(search_fts, 4.0, 1.0) AS score
--   FROM search_fts
--   JOIN search_doc  d ON d.rowid = search_fts.rowid
--   JOIN block       b ON b.rowid = d.block_rowid
--   JOIN publication p ON p.rowid = b.pub_rowid
--  WHERE search_fts MATCH :q
--  ORDER BY score LIMIT 50;
```

**À l'ouverture sur l'appareil** (paquet tiers ou téléchargé) :
1. vérifier le SHA-256 avant l'ouverture ;
2. vérifier `application_id` et `user_version` ;
3. exécuter `PRAGMA quick_check` ;
4. passer `PRAGMA trusted_schema=OFF` et, si possible, désactiver déclencheurs et vues, conformément aux recommandations de SQLite pour les fichiers non fiables [V].

### 3.4 Data classes Kotlin (esquisse)

```kotlin
// module :content-model — kotlinx.serialization ; le décodage des inlines compacts passe par un sérialiseur dédié
typealias Lang = String                                   // BCP 47

@JvmInline @Serializable value class PubId(val value: String)
@JvmInline @Serializable value class BlockId(val value: String)

sealed interface Inline {
    data class Text(val text: String) : Inline
    data class Marked(val marks: Set<Mark>, val children: List<Inline>) : Inline
    data class LangSpan(val lang: Lang, val children: List<Inline>) : Inline
    data class NoteRef(val note: BlockId, val label: String?) : Inline
    data class Link(val target: String, val children: List<Inline>) : Inline   // "bible:JHN.3.16", "pub:…#…", "https://…"
    data class Sym(val symbol: LiturgicalSymbol) : Inline
    data class Num(val value: String) : Inline
    data object Break : Inline
}
enum class Mark { ITALIC, BOLD, SMALL_CAPS, RED, SUPERSCRIPT, SUBSCRIPT, SPACED }
enum class LiturgicalSymbol(val canonical: String) {
    VERSICLE("℣"), RESPONSE("℟"), CROSS("✠"), STAR("*"), DAGGER("†")
}

enum class Role { V, R, S, P, M, ALL }
enum class NotePlacement { FOOT, END, MARGIN }
enum class SeparatorKind { RULE, ASTERISM, FLEURON, SPACE }
enum class ParaStyle { NORMAL, INTRO, EPIGRAPH, SIGNATURE, CENTERED }

sealed interface Block {
    val id: BlockId
    val rev: Int
    val t: Map<Lang, List<Inline>>        // une langue, ou plusieurs (texte en regard)

    data class Heading(override val id: BlockId, override val rev: Int, override val t: Map<Lang, List<Inline>>,
                       val level: Int, val label: String? = null) : Block
    data class Paragraph(override val id: BlockId, override val rev: Int, override val t: Map<Lang, List<Inline>>,
                         val dropCap: Boolean = false, val style: ParaStyle = ParaStyle.NORMAL) : Block
    data class Verse(override val id: BlockId, override val rev: Int, override val t: Map<Lang, List<Inline>>,
                     val n: String, val startsParagraph: Boolean, val ref: String? = null) : Block
    data class Rubric(override val id: BlockId, override val rev: Int, override val t: Map<Lang, List<Inline>>) : Block
    data class Versicle(override val id: BlockId, override val rev: Int, override val t: Map<Lang, List<Inline>>,
                        val role: Role) : Block
    data class Quote(override val id: BlockId, override val rev: Int, override val t: Map<Lang, List<Inline>>,
                     val source: String? = null) : Block
    data class Stanza(override val id: BlockId, override val rev: Int, override val t: Map<Lang, List<Inline>>,
                      val kind: String = "poem") : Block
    data class Note(override val id: BlockId, override val rev: Int, override val t: Map<Lang, List<Inline>>,
                    val placement: NotePlacement, val label: String?) : Block
    data class Image(override val id: BlockId, override val rev: Int, override val t: Map<Lang, List<Inline>>, // légende
                     val href: String, val alt: Map<Lang, String>, val credit: String? = null) : Block
    data class Separator(override val id: BlockId, override val rev: Int, val kind: SeparatorKind) : Block {
        override val t: Map<Lang, List<Inline>> get() = emptyMap()
    }
    data class Item(override val id: BlockId, override val rev: Int, override val t: Map<Lang, List<Inline>>,
                    val marker: String?, val level: Int = 1) : Block
    data class Slot(override val id: BlockId, override val rev: Int, override val t: Map<Lang, List<Inline>>,
                    val slot: String, val fallback: BlockId? = null) : Block
    /** Type inconnu d'une version mineure plus récente : rendu comme un paragraphe. */
    data class Unknown(override val id: BlockId, override val rev: Int, override val t: Map<Lang, List<Inline>>,
                       val type: String, val attrs: JsonObject) : Block
}

@Serializable data class TextQuote(val exact: String, val prefix: String, val suffix: String)
@Serializable data class Anchor(
    val pub: String, val block: String, val lang: String? = null,
    val start: Int? = null, val end: Int? = null,          // points de code du plainText (NFC)
    val quote: TextQuote? = null,
    val blockRev: Int, val pubRevision: Int,
)

@Serializable data class PackManifest(
    val format: String,                    // "1.0"
    val id: String, val version: Long,     // version : AAAAMMJJnn, monotone
    val title: Map<Lang, String>,
    val kind: String,                      // core | collection | bible | missal | extension…
    val languages: List<Lang>,
    val license: License, val source: Source,
    val publications: Int,
    val size: Sizes, val sha256: Hashes, val contentHash: String,
    val urls: List<String>,
    val extends: String? = null,           // V1 : paquet supplément
    val supersedes: List<String> = emptyList(),
    val requires: List<String> = emptyList(),
    val minAppFormat: String = "1.0",
    val builtAt: String,                   // ISO 8601, issu de SOURCE_DATE_EPOCH
)
@Serializable data class License(val spdx: String, val url: String? = null, val attribution: String? = null)
@Serializable data class Source(val name: String, val url: String? = null, val retrieved: String? = null)
@Serializable data class Sizes(val download: Long, val installed: Long)
@Serializable data class Hashes(val download: String, val installed: String)
```

### 3.5 Manifeste de paquet et catalogue (JSON)

**Manifeste d'un paquet**, embarqué dans `pack_info` et repris dans le catalogue :

```json
{
  "format": "1.0",
  "id": "clairval-fr",
  "version": 2026101100,
  "title": { "fr": "Lettres spirituelles de l’Abbaye Saint-Joseph de Clairval" },
  "kind": "collection",
  "languages": ["fr"],
  "license": {
    "spdx": "LicenseRef-Clairval-Permission",
    "url": "https://www.clairval.com/",
    "attribution": "© Abbaye Saint-Joseph de Clairval, Flavigny-sur-Ozerain"
  },
  "source": { "name": "Abbaye Saint-Joseph de Clairval", "url": "https://www.clairval.com/", "retrieved": "2026-10-05" },
  "publications": 412,
  "size":   { "download": 9123456, "installed": 27459584 },
  "sha256": { "download": "3f1c…", "installed": "a8fc…" },
  "contentHash": "sha256:7d2e…",
  "urls": [
    "https://github.com/<propriétaire>/paroissien-content/releases/download/packs-2026.10.11/clairval-fr-2026101100.sqlite.gz"
  ],
  "extends": null,
  "supersedes": [],
  "requires": [],
  "minAppFormat": "1.0",
  "builtAt": "2026-10-11T00:00:00Z"
}
```

Les valeurs numériques sont fictives. Le nombre de lettres et les tailles seront fournis par l'agent 01. La licence dépend de l'agent 12.

**Catalogue** (`catalog.json`) :

```json
{
  "catalogFormat": 1,
  "generated": "2026-10-11T00:00:00Z",
  "packs": [ { "…": "un manifeste par paquet, dernière version seulement" } ],
  "signature": { "alg": "ed25519", "keyId": "paroissien-2026", "sig": "…" }
}
```

Le champ `signature` arrive en V1. Au MVP, l'intégrité repose sur HTTPS et le SHA-256 de chaque paquet.

**Transport** : `.sqlite.gz`, décompressé par `java.util.zip.GZIPInputStream` sans dépendance. Le gzip est produit avec `mtime=0` et sans nom de fichier, pour rester reproductible.

### 3.6 Chaîne de production

#### Arborescence de `content/`

```
content/
├── pyproject.toml              # projet uv « paroissien-content » (CLI : pqt)
├── uv.lock                     # verrou commité ; CI : uv sync --locked
├── .python-version             # Python épinglé (ex. 3.13)
├── src/paroissien_content/
│   ├── cli.py                  # pqt fetch|normalize|validate|package|publish|diff|golden
│   ├── model.py                # pydantic : Publication, Division, Block, Inline → JSON Schema générés
│   ├── inlines.py              # plainText(), hash canonique, validation de la grammaire
│   ├── ids.py                  # frappe, réalignement (Myers + rapidfuzz), alias, rapport de churn
│   ├── fold.py                 # pliage de recherche (miroir exact de Kotlin)
│   ├── typo.py                 # NFC, apostrophes, espaces fines, ligatures de PDF, césures de fin de ligne
│   ├── package/
│   │   ├── sqlite.py           # écriture déterministe (APSW, SQLite épinglé)
│   │   └── epub.py             # (plus tard, ou si l'agent 06 choisit Readium)
│   ├── publish.py              # Releases + catalog.json (idempotent : rien si le sha256 est inchangé)
│   └── sources/                # UN adaptateur par source, écrit avec l'agent concerné
│       ├── clairval/           # agent 01 : fetch.py, parse.py
│       ├── ignace/             # agent 02
│       ├── divinum_officium/   # agent 03
│       ├── bible/ , imitation/ # agent 04
│       └── prieres/            # agent 14 (saisie manuelle en YAML ou JSON)
├── sources/                    # déclarations : URL, licence, paramètres, politesse (délai, user-agent)
│   └── clairval-fr.yaml …
├── packs/                      # composition : quelles publications vont dans quel paquet
│   └── clairval-fr.yaml …
├── data/                       # JSON CANONIQUE COMMITÉ (source de vérité) — sous réserve de D1
│   └── <publication-id>/
│       ├── publication.json    # métadonnées et arbre des divisions
│       ├── blocks/<division>.jsonl   # un bloc par ligne
│       └── assets/             # images (WebP), avec leurs métadonnées de licence
├── ids/                        # (variante D1 « texte non commité ») registres id/hash/MinHash
├── schema/                     # JSON Schema générés depuis model.py et commités (la CI vérifie qu'ils sont à jour)
│   ├── publication.schema.json
│   ├── block.schema.json
│   ├── pack-manifest.schema.json
│   └── catalog.schema.json
├── spec/
│   ├── pack.sql                # DDL de référence (§3.3)
│   ├── folding-vectors.json    # entrée → sortie, partagé avec les tests Kotlin
│   ├── plaintext-vectors.json  # inlines → plainText, partagé avec Kotlin
│   └── golden/
│       ├── mini/               # publication couvrant TOUS les types de blocs et d'inlines
│       └── mini.sqlite.sha256  # hash attendu du paquet construit (déterminisme)
├── tests/                      # pytest + syrupy (instantanés)
│   ├── fixtures/               # extraits bruts minimaux, ou fixtures synthétiques si la licence l'impose
│   └── __snapshots__/
├── .cache/                     # raw/ (téléchargements), http/ (ETag) — JAMAIS commité
└── dist/                       # paquets construits — jamais commité
```

Le dépôt comprend aussi `.github/workflows/content.yml`, et `.gitignore` doit contenir `content/.cache/` et `content/dist/`.

#### Étapes

| Étape | Entrée → sortie | Points clés |
|---|---|---|
| `fetch` | Web, PDF → `.cache/raw/<source>/…` et `fetch-log.jsonl` | Cache HTTP (ETag, Last-Modified), délai entre requêtes, user-agent identifié, respect de robots.txt. Le journal (URL, date, statut, sha256) est commité ; le brut ne l'est jamais. |
| `normalize` | brut → `data/<pub>/…` | Analyseur propre à chaque adaptateur, puis normalisation commune (`typo.py`) et attribution des identifiants (`ids.py`, réalignement sur la version commitée). La `revision` et les `rev` augmentent si le hash change. |
| `validate` | `data/` → rapport | JSON Schema, puis vérifications sémantiques : identifiants uniques, **aucun identifiant publié perdu sans alias**, notes et renvois résolus, ressources présentes, langues déclarées, licence présente, pas de bloc vide, détection de mojibake (`Ã©`), caractères autorisés. |
| `package` | `data/` + `packs/*.yaml` → `dist/*.sqlite(.gz)` et manifeste | Insertions triées, pragmas fixes, `rebuild` puis `optimize` de l'index, `VACUUM`, `SOURCE_DATE_EPOCH`, sha256 du fichier et `contentHash`. |
| `publish` | `dist/` → Release et `catalog.json` | Idempotent ; tag `packs-AAAA.MM.JJ` ; met à jour le catalogue (et le signe en V1). |
| `diff` | deux versions → rapport lisible | Blocs ajoutés, modifiés, aliasés et supprimés, churn, tailles. Le rapport est posté en commentaire de PR. |

#### Reproductibilité

- Python épinglé (`.python-version`), dépendances verrouillées (`uv.lock`, `--locked` en CI [V]) et **APSW épinglé à la version de SQLite de `sqlite-bundled`** (aujourd'hui 3.53.4 [V]).
- JSON canonique écrit avec des clés triées, `ensure_ascii=False`, en NFC et avec un `\n` final ; écriture SQLite dans un ordre déterministe ; gzip avec `mtime=0`.
- Les paquets sont **construits uniquement à partir de `data/` commité**, jamais depuis le brut. N'importe qui peut donc reconstruire le paquet à l'octet près depuis un commit, sous réserve de D1.
- Deux hash : le `sha256` du fichier (intégrité du téléchargement) et le `contentHash` du JSONL (identité logique, indépendante de la version de SQLite).

#### Tests « golden »

1. **Adaptateurs** : fixture brute minimale → JSON canonique attendu (instantané syrupy). Toute modification du parseur montre son diff.
2. **Paquet `mini`** : couvre tous les types de blocs et d'inlines, le bilingue, les notes, un alias et une ressource. On vérifie le sha256 attendu, ce qui prouve le déterminisme. Il est **consommé par les tests unitaires Android** (contrat entre la chaîne et l'application).
3. **Vecteurs de pliage et de `plainText`** : le même fichier JSON est testé par pytest et par JUnit.
4. **Stabilité des identifiants** : relancer `normalize` sur le brut de la dernière release doit donner **zéro churn**.
5. **Recherche** : une dizaine de requêtes types (« coeur », « Iesus », « eglise », `"notre pere"`, `glori*`) avec les identifiants de blocs attendus.

#### CI (GitHub Actions)

- **Sur PR modifiant `content/**`** : `uv sync --locked`, `pytest`, `pqt validate`, `pqt package` (à blanc), puis commentaire `pqt diff`. Ces tâches tournent en Python seul et restent légères.
- **Cron mensuel** (Clairval) : `fetch`, `normalize`, puis **ouverture automatique d'une PR**. Une relecture humaine précède toujours la publication d'un contenu religieux.
- **Sur tag `packs-*`** : `package`, `publish`. Aucun build Android n'est nécessaire.

### 3.7 Spécification du pliage de recherche

Fonction `fold(s)`, identique en Python (`fold.py`) et en Kotlin, appliquée au **texte indexé** comme à la **requête** :

1. **NFKC** : ﬁ→fi, espaces insécables→espace, exposants→chiffres.
2. **Minuscules simples** : `str.lower()` en Python, `lowercase(Locale.ROOT)` en Kotlin, et **pas** `casefold`, pour éviter ß→ss et les divergences.
3. **NFD**, puis suppression des marques combinantes (catégorie Mn).
4. **Table explicite** : œ→oe, æ→ae (ǽ devient æ à l'étape 3), ĳ→ij [S], et toute autre lettre propre aux sources découverte par les agents de source.
5. **Pliage latin** : j→i, v→u (décision D5 : universel, ou seulement sur les textes `la`).
6. Le reste, ponctuation comprise, est conservé : le tokeniseur `unicode61` découpe ensuite.

Pour éviter les divergences de version Unicode entre Python (15.0 ici [M]) et l'ICU d'Android, la CI vérifie que **tous les caractères présents dans `data/`** figurent dans les vecteurs de test : un caractère nouveau fait échouer la CI tant qu'il n'a pas de vecteur. Le surlignage se fait dans l'application, sur le texte original, par retokenisation et pliage jeton par jeton.

Exemples de vecteurs : `Cœur`→`coeur` ; `l’Église`→`l’eglise` ; `Dóminus`→`dominus` ; `Jésus`→`iesus` ; `prǽdica`→`praedica` ; `ﬁdèle`→`fidele` ; `vivere`→`uiuere`.

### 3.8 Interfaces avec les autres agents

| Agent | Ce que le modèle lui fournit | Ce que j'attends de lui |
|---|---|---|
| 01 Clairval | Schéma `clairval.<lang>.<AAAA-MM>`, identifiants frappés et réalignés, `series` | Volume réel (nombre de lettres, Ko par lettre, langues), stabilité du HTML, images, faisabilité depuis les runners de la CI |
| 02 Ignace | Clés `ex.<n>`, `kind=day/week`, publication `itinerary` (retraite guidée) faite de `slot` vers des blocs | Confirmation de la numérotation 1–370 dans l'édition source ; nettoyage du PDF (césures, ligatures) |
| 03 Missel | `versicle.role`, `rubric`, `slot`, texte en regard, `attrs.color` | Clés naturelles depuis Divinum Officium ; liste des emplacements de l'ordinaire |
| 04 Bible / Imitation | Clés `book.ch.v` (USFM), `verse.p`, notes, renvois abstraits `bible:` | Versification (`meta.versification`) et table de correspondance Vulgate / hébreu |
| 05 Calendrier | `attrs` des divisions (couleur, rang) ; résolution des `slot` | Vocabulaire commun des identifiants de fêtes (identique aux clés du propre) |
| 06 Rendu | §2.4 (conditions), grammaire des inlines, `plainText`, `seq`/`char_start` | Choix natif / WebView / Readium ; besoins éventuels d'attributs de mise en page |
| 07 Tourne-page | Images en `resource`, paquet `images` dédié possible | Formats et poids des images pieuses |
| 08 UX | Bibliothèque = publications ; recherche multi-paquets ; gestion des téléchargements | Parcours de téléchargement et de mise à jour ; filtres de recherche |
| 09 Signets | `Anchor`, `rev`, `id_alias`, conversion vers Locator | Validation du contrat d'ancre |
| 10 Architecture | Fichiers en lecture seule, `application_id`, multi-paquets | Choix du pilote (embarqué ou système), emplacement de stockage, `minSdk` |
| 12 Juridique | Champs `license`, `attribution`, `permissionRef`, `approbation` | D1 (commit du texte), licences par source, mention d'attribution obligatoire |
| 14 Prières & extensions | Même chaîne, saisie YAML ou JSON, espace de noms `ext.` | Liste des recueils et des sources |

---

## 4. Décisions à soumettre à l'utilisateur

| # | Question | Options | Recommandation |
|---|---|---|---|
| **D1** | Commiter le **texte normalisé** de sources non libres (Clairval, traductions récentes) dans le dépôt ? | a) dépôt public avec texte ; b) **dépôt de contenu privé séparé** (`paroissien-content`), l'application restant publique ; c) aucun texte commité, seulement le registre id/hash/MinHash | **b** si la licence le permet, **c** sinon. À trancher avec l'agent 12. |
| **D2** | Hébergement des paquets | a) **GitHub Releases et Pages** ; b) serveur ou miroir indépendant (Codeberg, hébergeur européen) ; c) les deux | **a** au MVP, avec des URL multiples dans le catalogue pour ajouter **b** plus tard |
| **D3** | Index de recherche | a) **précalculé dans le paquet** ; b) reconstruit à l'installation (environ −32 % de téléchargement [M], mais fichier modifié sur l'appareil) | **a** au MVP ; **b** en option V1 selon les tailles réelles |
| **D4** | Moteur SQLite des paquets (avec l'agent 10) | a) **`BundledSQLiteDriver`, FTS5** ; b) SQLite du système, FTS4 | **a** (FTS5 garanti, version maîtrisée), au prix d'une bibliothèque native [S : de l'ordre du Mo par ABI] |
| **D5** | Pliage i/j et u/v | a) **universel** (une seule fonction) ; b) seulement pour les textes `la` | **a**, sauf si un test sur corpus réel montre des collisions françaises |
| **D6** | Normalisation typographique | a) **normaliser** (apostrophe ’, espaces fines U+202F devant ; : ! ? et dans « ») ; b) conserver la source telle quelle | **a**, pour un rendu homogène de « livre » ; le texte de recherche est plié de toute façon |
| **D7** | Mise à jour mensuelle de Clairval | a) **remplacement complet** ; b) suppléments mensuels (V1) | **a** au MVP, puis **b** |
| **D8** | Paquets embarqués dans l'APK | a) **prières et ordinaire** (moins de 1 Mo) ; b) a + Imitation et Exercices ; c) rien | **a** ou **b** selon le poids d'APK acceptable |
| **D9** | Langage de la chaîne | a) **Python + uv** ; b) Kotlin (module partagé) | **a** |
| **D10** | Relecture humaine avant toute publication de contenu | a) **toujours, via une PR** ; b) publication automatique de Clairval | **a** |

---

## 5. Risques et inconnues

| Risque ou inconnue | Gravité | Atténuation |
|---|---|---|
| **Churn d'identifiants** si Clairval reformate son HTML ou si une source corrige massivement ses textes : signets et annotations orphelins | Élevée | Réalignement flou, `id_alias`, repli sur la citation côté application, porte de CI sur le churn, relecture humaine |
| **Divergence du pliage** entre Python et Kotlin (versions Unicode et ICU, minuscules selon la locale) : recherches qui échouent sans bruit | Moyenne | Table explicite, `Locale.ROOT`, vecteurs partagés, CI qui exige un vecteur pour chaque caractère du corpus |
| **FTS5 indisponible** si l'agent 10 refuse le pilote embarqué | Moyenne | Repli FTS4 sur la même colonne pliée, sans `bm25` |
| **Version de SQLite de la chaîne plus récente que celle de l'application** : format illisible, hash de fichier différent | Moyenne | APSW épinglé sur la version de `sqlite-bundled` ; règle « chaîne ≤ application » ; `contentHash` logique |
| **Licences** : redistribuer ou commiter des textes protégés (Clairval, traductions françaises récentes de la Bible ou du Missel) | Élevée | D1 ; champ `license` obligatoire, validé en CI ; agent 12 |
| **Paquets tiers malveillants** (fichier SQLite non fiable) | Moyenne (V1 et plus tard) | sha256, `application_id`, `quick_check`, `trusted_schema=OFF`, désactivation des vues et déclencheurs [V] ; liste d'origines approuvées |
| **Assemblage liturgique** (ordinaire + propre, conditions selon le temps) plus riche que de simples `slot` | Moyenne | Garder le modèle statique simple et confier la logique au moteur liturgique (agents 03 et 05) ; `x-` pour expérimenter |
| **Versification biblique** (Psaumes Vulgate ou hébreu, Daniel, Esther grecs) qui casse les renvois `bible:` | Moyenne | `meta.versification` et table de correspondance (agent 04) |
| **Volume réel inconnu** (Clairval, missel complet en latin et français) ; mesures faites sur un corpus de substitution | Faible à moyenne | Remesurer dès les premiers adaptateurs ; `detail` et l'index reconstruit restent des variables d'ajustement |
| **Scraping depuis la CI** (IP de datacenter bloquées, politesse) | Moyenne | Lancer `fetch` depuis la machine de l'utilisateur si besoin ; cache ETag ; un fetch par mois seulement |
| **Disparition d'une source** (site fermé ou restructuré) | Moyenne | Archive brute privée (hors dépôt public) et `fetch-log` commité avec les sha256 |
| **Dépendance à GitHub et vie privée** (IP visible) | Faible | URL miroirs dans le catalogue, téléchargements à l'initiative de l'utilisateur |
| **Non vérifié dans cette session** : table des versions de SQLite par niveau d'API Android ; étendue exacte 1–370 des Exercices ; capacité de Readium à ouvrir une publication en mémoire ; licences d'APSW, pypdf et syrupy ; poids exact de `sqlite-bundled` par ABI ; collisions du pliage u/v en français | — | À confirmer par les agents 02, 06 et 10, ou lors d'une passe de recherche ultérieure (quota de recherche web épuisé) |

---

## 6. Sources

**SQLite**
- FTS5 (tokeniseurs `unicode61` et `trigram`, `detail`, contenu externe, `rebuild`/`optimize`, `bm25`, Porter, format) : https://www.sqlite.org/fts5.html
- SQLite comme format de fichier applicatif (`application_id`) : https://www.sqlite.org/appfileformat.html
- Format de fichier (en-tête : compteur de modifications, `SQLITE_VERSION_NUMBER`, `application_id`) : https://www.sqlite.org/fileformat.html
- BLOB internes ou externes (seuil d'environ 100 Ko) : https://www.sqlite.org/intern-v-extern-blob.html
- Limites (`SQLITE_MAX_ATTACHED` : 10 par défaut, 125 au plus) : https://www.sqlite.org/limits.html
- Paramètres URI (`immutable`, `mode=ro`) : https://www.sqlite.org/uri.html
- Extension *session* (tables virtuelles non prises en charge) : https://www.sqlite.org/sessionintro.html
- Sécurité, fichiers non fiables : https://www.sqlite.org/security.html

**Android**
- Options de compilation de `sqlite-bundled` (SQLite 3.53.4, FTS5) : https://android.googlesource.com/platform/frameworks/support/+/HEAD/sqlite/sqlite-bundled/build.gradle
- Notes de version de Room 3 (`@Fts5`, `SQLiteDriver` obligatoire, 3.0.3) : https://developer.android.com/jetpack/androidx/releases/room3
- Référence de `@Fts5` (disponibilité selon le pilote) : https://developer.android.com/reference/kotlin/androidx/room3/Fts5 (consultée via un résumé de recherche)
- Pilote embarqué : https://developer.android.com/reference/androidx/sqlite/driver/bundled/BundledSQLiteDriver et https://developer.android.com/jetpack/androidx/releases/sqlite
- FTS5 non garanti dans le SQLite du framework : https://commonsware.com/Room/pages/chap-roomfts-001 et https://www.b4x.com/android/forum/threads/sqlite3-fts5.110369/

**Formats de publication et ancres**
- Readium Web Publication Manifest : https://readium.org/webpub-manifest/ ; empaquetage : https://readium.org/webpub-manifest/packaging.html
- Readium Locator : https://readium.org/architecture/models/locators/
- Kit Readium pour Kotlin (formats, navigateurs, BSD-3) : https://github.com/readium/kotlin-toolkit
- EPUB 3.3 (W3C) : https://www.w3.org/TR/epub-33/
- EPUB CFI 1.1 : https://idpf.org/epub/linking/cfi/
- W3C Web Annotation Data Model (sélecteurs) : https://www.w3.org/TR/annotation-model/
- Ancrage flou de Hypothesis : https://web.hypothes.is/blog/fuzzy-anchoring/
- Portable Text : https://github.com/portabletext/portabletext

**Référentiels**
- Codes de livres USFM : https://ubsicap.github.io/usfm/identification/books.html
- SPDX, identifiants `LicenseRef-` : https://spdx.github.io/spdx-spec/v2.3.1/other-licensing-information-detected/
- Snowball (français ; Schinke pour le latin) : https://snowballstem.org/algorithms/
- Numérotation des Exercices, édition Ganss (confirmation partielle) : https://jesuitsources.bc.edu/the-spiritual-exercises-of-saint-ignatius-a-translation-and-commentary-paperback
- Divinum Officium (arborescence `web/www/missa/<langue>`) : https://github.com/DivinumOfficium/divinum-officium

**Hébergement et outillage**
- Limites des GitHub Releases : https://docs.github.com/en/repositories/releasing-projects-on-github/about-releases
- Lien vers la dernière release : https://docs.github.com/en/repositories/releasing-projects-on-github/linking-to-releases
- Limites de GitHub Pages : https://docs.github.com/en/pages/getting-started-with-github-pages/github-pages-limits
- uv, verrou et `--locked` : https://docs.astral.sh/uv/concepts/projects/sync/
- APSW (SQLite embarqué, versions alignées) : https://rogerbinns.github.io/apsw/ et https://rogerbinns.github.io/apsw/install.html
- python-build-standalone, FTS5 dans les options : https://github.com/astral-sh/python-build-standalone/pull/800

**Mesures locales [M]**
- Scripts jetables (`tok.py`, `size.py`, `pack.py`) exécutés dans le scratchpad de session, non conservés dans le dépôt. Environnement : Python 3.12.3, SQLite 3.45.1, Unicode 15.0. Corpus : pages de manuel françaises de la VM (2,17 Mo).
