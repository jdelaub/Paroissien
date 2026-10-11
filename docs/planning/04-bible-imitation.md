# 04 — Bible, Évangiles et Imitation de Jésus-Christ

> Rapport de l'agent 04, rédigé le 11 octobre 2026 en phase de recherche, sans code applicatif.
> Convention : **[V]** signale un fait vérifié pendant cette recherche (fichier téléchargé et inspecté, page consultée ou mesure faite). **[S]** signale une supposition, un souvenir ou une information de seconde main restée non vérifiée.
> Limite : le budget de recherche web partagé entre les agents a été épuisé en cours de travail. Quelques points sont donc restés à l'état **[S]** : traduction de Marillac, éditions de Gonnelieu sur Gallica, conditions exactes de la Nova Vulgata et FTS5 sous Android. Ils sont repris dans la section 5.

---

## 1. Résumé

- **Bible française principale : Crampon 1923.** La meilleure source est le dépôt **GitLab CrossWire `crampon`**, en USFM et OSIS. Il dérive de Wikisource et le texte est du domaine public. Le dépôt contient les 73 livres, 35 599 versets dont aucun n'est vide, **8 789 notes** (1,6 Mo) et 2 445 intertitres. **[V]**
- **Le module SWORD compilé `FreCrampon` (v3.4) ne doit pas être utilisé tel quel.** Une fois décodé, il présente **962 versets vides et 60 « versets géants »** : des chapitres entiers ont été fusionnés dans le dernier verset du chapitre précédent, surtout dans Job, Proverbes, Siracide, Sagesse et Baruch 6. Le défaut vient de la conversion et non de la source USFM, qui est saine. **[V]**
- **Latin : Vulgate clémentine** (Clementine Text Project, module CrossWire `VulgClementine`, domaine public, versification Vulgate). La **Nova Vulgata** reste sous © Libreria Editrice Vaticana et demanderait une autorisation. **[V/S]**
- **Autres Bibles françaises.** Le **Glaire 1905** (révisé par Vigouroux) est complet et relu sur Wikisource. Il suit la numérotation de la Vulgate et contient une *Table des Épîtres et Évangiles* du rite romain. Le **Sacy** est sur Wikisource, dans l'édition d'une société biblique protestante de 1855 dont les deutérocanoniques ont été ajoutés depuis une autre édition (1841). Le **Crampon 1904** est incomplet sur Wikisource, avec environ 6 % de pages corrigées. Le **Fillion** n'a aucune source numérique fiable : la copie Zefania est défectueuse et les PDF sont mal océrisés. **[V]**
- **Anglais : Douay-Rheims.** Trois sources propres existent : eBible `engDRA` (1899, USFM), CrossWire `DRC` et Gutenberg n° 8300. Cette dernière inclut les **annotations de Challoner**. **[V]**
- **Imitation : Lamennais** dans le Gutenberg n° 57824, une édition de 1859 relue par Distributed Proofreaders, avec une **réflexion à la fin de chaque chapitre**. Ce choix est recommandé pour le MVP, avec le **latin** de The Latin Library. Les chapitres (25, 12, 59 et 18) s'alignent sur le français et **82 % des chapitres** ont le même découpage en paragraphes. **[V]**
- **Le module Évangiles n'est pas un texte à part.** On le conçoit comme une **vue filtrée** sur la Bible choisie, à laquelle s'ajoutent un **« Évangile du jour »** fourni par l'interface lectionnaire (agents 03 et 05) et, en V1, des **parallèles synoptiques**.
- **Modèle de données.** On retient des clés de verset entières et stables (`livre·chapitre·verset·partie`), une **versification déclarée par édition** et des tables de correspondance entre la Vulgate et l'hébreu. Un **analyseur de références** couvre les styles AELF (« Lc 15, 1-3.11-32 »), les styles anciens et latins (« Joan., VIII, 12 », « Eccli. 44, 17 ») et la double numérotation des psaumes.
- **Recherche : SQLite FTS5**, à condition d'ajouter une **colonne normalisée**. Le tokenizer `unicode61` ne replie ni « œ » ni « æ » ni « j/i » : « coeur » ne trouve pas « cœur » et « caelum » ne trouve pas « cælum ». C'est mesuré. **[V]**
- **Tailles mesurées.** Crampon avec notes et index FTS : environ **11 Mo** en SQLite, **3,9 à 5 Mo** une fois compressé. Vulgate : 8 Mo, 3 à 4 Mo compressée. Imitation de Lamennais : environ 0,6 Mo. Un paquet MVP complet (Crampon, Vulgate, Imitation latin et français) pèserait **environ 20 Mo installé et 8 Mo à télécharger**.

---

## 2. Constats détaillés

### 2.1 Bibles inspectées concrètement

Neuf sources ont été téléchargées et analysées avec des scripts d'inspection en lecture seule (`python3 -I`), dans le dossier `scratchpad/04/` :

| Source | Format | Inspection |
|---|---|---|
| CrossWire `FreCrampon` 3.4 (2026-08-27) | SWORD zText, OSIS | Décodage de l'index des versets avec la versification `Catholic2`, puis statistiques |
| GitLab CrossWire `crampon` (2026-08-26) | USFM (73 fichiers) | Statistiques et marqueurs |
| CrossWire `VulgClementine` 2.0.1 | SWORD zText, OSIS | Décodage avec la versification `Vulg` |
| CrossWire `DRC` 2.0 | SWORD zText | Décodage avec la versification `Vulg` |
| eBible `francl` (néo-Crampon Libre) | USFM | Statistiques et comparaison avec Crampon 1923 |
| eBible `latVUC` | USFM | Statistiques |
| eBible `engDRA` | USFM | Statistiques |
| Zefania `FRE_FILL` (Fillion, 2010) | Zefania XML | Analyse XML |
| jesusmarie.free.fr, Fillion « Ruth » | PDF scanné | `pdftotext` |

Wikisource (wikitexte brut et état de relecture via l'API ProofreadPage), Gutenberg n° 57824 et n° 8300, The Latin Library et archive.org (Gonnelieu, 1828) ont aussi été inspectés.

### 2.2 Bibles catholiques françaises

#### Crampon 1923 (Desclée, un volume) — **recommandé**

- **Statut juridique.** Augustin Crampon est mort en 1894. L'édition de 1923 est une révision posthume. Les réviseurs seraient des Pères jésuites **[S]**. Si cette révision n'est pas une œuvre collective, il faudrait vérifier la date de décès des réviseurs pour le droit français. Wikisource FR l'héberge et CrossWire la publie sous `DistributionLicense=Public Domain` **[V]**. Wikisource FR exige en principe le domaine public en France et aux États-Unis **[S]**. Le point est **à confirmer par l'agent 12**.
- **Wikisource** ([Bible_Crampon_1923](https://fr.wikisource.org/wiki/Bible_Crampon_1923)) **[V]**
  - Le texte est transcrit depuis le fac-similé `La Sainte Bible, trad Crampon, édition 1923.djvu` : **1 815 pages, dont 1 766 corrigées, 48 validées et 1 sans texte**. Il est donc entièrement relu une fois.
  - Le balisage est très structuré : `{{verset|chap|vers}}` en ligne, notes en `<ref>`, poésie en `<poem>`, et un en-tête de psaume du type « Ps. xviii (Vulg. xvii) ». Crampon numérote donc **selon l'hébreu, avec le numéro de la Vulgate entre parenthèses**, et le titre du psaume compte comme verset 1.
  - Les 150 psaumes sont répartis sur cinq sous-pages, « Psaumes 1 » à « Psaumes 5 ».
- **GitLab CrossWire `crampon`** ([dépôt](https://gitlab.com/crosswire-bible-society/crampon), README « New Crampon sources files ») **[V]**
  - Le dépôt contient un USFM par livre (archive de 2,7 Mo, 7,8 Mo décompressés) et `frecrampon.osis.xml` (13,5 Mo).
  - Mesures : **73 livres, 35 599 versets, 0 vide, 4,96 Mo de texte brut et 8 789 notes de bas de page (1,62 Mo)**.
  - Marqueurs : 2 000 `\s2`, 214 `\s1` et 231 `\ms1` pour les intertitres et sommaires analytiques ; 34 014 `\q1` pour les lignes poétiques ; 9 048 `\add` pour les mots en italique ajoutés par le traducteur ; 1 334 `\cp` ; 122 `\vp` pour les numéros de verset imprimés.
  - L'ordre des livres suit `Catholic2` : Tobie, Judith, Esther et 1-2 Maccabées après Néhémie. Esther compte 16 chapitres, avec les additions grecques en 10,4–16,24 comme dans la Vulgate.
  - Petit défaut relevé : un appel de note « [109] » resté dans un intertitre du psaume 18.
- **Le module SWORD compilé `FreCrampon` 3.4** ([conf](https://www.crosswire.org/ftpmirror/pub/sword/packages/rawzip/FreCrampon.zip), 4,2 Mo) **[V]**
  - Configuration : `Versification=Catholic2`, filtres OSISFootnotes et OSISHeadings. L'historique indique « Addition of notes, crossreferences, titles. All moved to wikisource » (v3.1, 2025) et l'OCR est attribuée à Jean-Marie Weber.
  - Décodage : sur **35 610 emplacements, 962 sont vides**. On compte 140 séquences de vides, dont 85 d'au moins trois versets, et **60 versets de plus de 900 caractères**. Exemples : Job 12,25 contient tout Job 13, Baruch 5,9 contient Baruch 6 et Actes 14,6 contient les versets 7 et 8 avec leurs numéros dans le texte.
  - Ces zones correspondent aux passages en `<poem>`, en `\cp` ou en colonnes. Il s'agit d'un **bug de conversion vers OSIS** : l'USFM du même dépôt est complet.
  - Le module apporte en revanche 4 468 balises `<reference osisRef>` dans les notes. Certaines sont incohérentes, par exemple un renvoi affiché « Is. iv, 5 » pointe vers `Isa.51.5`.
  - La recommandation est de signaler le problème en amont à CrossWire et de **ne pas utiliser le binaire SWORD**.
- **Notes de Crampon.** L'édition de 1923 ne contient qu'une *sélection* de notes, soit environ 8 800 notes et 1,6 Mo. Les notes complètes figurent dans **la grande édition en 7 volumes de 1894-1904**, avec la Vulgate latine en regard. Elle est sur archive.org (`lasaintebibletra01cram` à `07cram`) avec une OCR ABBYY de 2,9 Mo par volume, sans structure **[V]**. Les exploiter supposerait une campagne de structuration lourde.
- **Le nom divin « Yahweh » apparaît 7 757 fois** dans le Crampon 1923 **[V]**. L'édition de 1904 aurait employé « Jéhovah » **[S]**. Une lettre de la Congrégation pour le culte divin (2008) demande de ne pas employer ce nom dans la liturgie **[S, souvenir à vérifier]**. Ce point donne lieu à la décision D2.

#### Néo-Crampon Libre (Fraternité de Tibériade, 2022) — option moderne

- Licence **CC BY-SA 4.0**, © 2022 Fraternité de Tibériade. Sources : [eBible `francl`](https://ebible.org/find/details.php?id=francl) et [GitLab](https://gitlab.com/crosswire-bible-society/neo-crampon-libre) (README en version 0.4). **[V]**
- Le texte compte 35 530 versets sans aucun vide, 5,11 Mo, et 2 530 notes (0,48 Mo). Il contient des numéros Strong posés automatiquement et souvent faux : la préposition « Au » est par exemple étiquetée G1722 et le déterminant « le » G2532. **[V]**
- **La modernisation est légère.** Selon la comparaison mot à mot avec le Crampon 1923, 64 % des versets des Psaumes et 93 % de ceux de l'Épître aux Romains sont identiques, avec une similarité moyenne de 0,97 à 0,99. Le texte garde « Yahweh ». **[V]**
- La licence share-alike imposerait de redistribuer sous la même licence le paquet de contenu dérivé, voir l'agent 12.

#### Glaire 1905 (texte de J.-B. Glaire, introductions et notes de F. Vigouroux, Roger et Chernoviz)

- Domaine public : Glaire est mort en 1879 et Vigouroux en 1915. Le texte est traduit **sur la Vulgate**, ce qui donne la même numérotation et le même canon que le missel de 1962. **[V]** pour l'édition, **[S]** pour les dates de décès.
- Sur Wikisource ([page](https://fr.wikisource.org/wiki/La_sainte_Bible_selon_la_Vulgate_(J.-B._Glaire))), on compte **3 104 pages, dont 2 550 corrigées, 525 validées, 19 non créées et 10 sans texte**. Les 73 livres sont présents avec introductions, notes de Glaire et appendices de Vigouroux. **[V]**
- Le balisage est moins structuré que celui du Crampon : numéros de verset en texte (`39.{{lié}}`), notes en `<ref>` préfixées « Jean 1,39 : » et ancres de chapitre `CHAPITRE n.`. L'analyse reste fiable. **[V]**
- **Atout pour les agents 03 et 05** : la sous-page *Table des Épîtres et Évangiles qui se lisent dans l'Église pendant toute l'année (rite romain)* donne, dimanche par dimanche et fête par fête, les références tridentines. On y trouve par exemple « Ep. Rom., 15, v. 4-13 » et « Ev. Matth., 11, V. 2-10 ». **[V]** Les changements de 1955 et 1960 sont à reporter pour obtenir l'Ordo de 1962.
- Zefania propose une « Bible Vigouroux 1902 » d'1,8 Mo, sans doute la Polyglotte, dont le français reprend Glaire **[S]**. Elle n'a pas été inspectée.

#### Lemaistre de Sacy

- Wikisource [Bible_Sacy](https://fr.wikisource.org/wiki/Bible_Sacy) donne le texte de l'édition de la **Société biblique britannique et étrangère, Bruxelles 1855**, c'est-à-dire la traduction catholique de Sacy publiée par une société biblique protestante, sans les deutérocanoniques. Les livres deutérocanoniques ont été **ajoutés depuis l'édition Furne de 1841**. Le fac-similé de 1855 compte **1 041 pages, dont 936 corrigées et 94 validées**. Les numéros de verset y sont écrits en texte (« 11 Jésus prit… »). **[V]**
- Il s'agit d'une traduction du XVIIᵉ siècle, au français classique, numérotée selon la Vulgate. Il faut vérifier que l'édition de 1855 n'a pas retouché le texte **[S]**.
- Zefania propose aussi « La Bible de Saci 1759 » (1,6 Mo), non inspectée. **[V]** pour la présence.

#### Fillion (*La Sainte Bible commentée*, 1888-1904)

- Domaine public, Fillion étant mort en 1927. **[V]** d'après la notice Zefania.
- **Aucune source numérique propre n'a été trouvée.** **[V]**
  - **Zefania `SF_2010-05-15_FRE_FILL`** contient 35 787 versets (4,98 Mo, sans notes) mais seulement **72 livres**. Le livre numéro 30, étiqueté « Amos », contient en réalité **Isaïe** (66 chapitres) et **Amos manque**. Les psaumes semblent **renumérotés selon l'hébreu**, alors que Fillion suit la Vulgate, des astérisques orphelins renvoient à des notes absentes et aucune licence n'est indiquée. La source déclarée, `magnificat.ca/textes/bible/…`, renvoie une erreur 404.
  - **jesusmarie.free.fr** propose des PDF scannés en deux colonnes latin et français avec commentaire, et une OCR inutilisable : ses archives `.zip` renvoient une page HTML de 2 Ko.
- Le Fillion est donc **à écarter** tant qu'une transcription sérieuse n'existe pas.

#### Crampon 1904 (Desclée, un volume)

- Wikisource [Bible_Crampon_1904](https://fr.wikisource.org/wiki/Bible_Crampon_1904) : **1 578 pages, dont 894 non créées, 594 non corrigées et 89 corrigées**. Seuls Gn, Rt, Jr, Ez, Ml et Mt ont une page. Le texte est donc **inexploitable aujourd'hui**. **[V]**
- La différence avec 1923, qui serait le passage de « Jéhovah » à « Yahweh » et une révision du Nouveau Testament, n'a pas été vérifiée **[S]**.

### 2.3 Latin

- **Vulgate clémentine, module CrossWire `VulgClementine` 2.0.1** **[V]**
  - Texte établi d'après Colunga et Turrado (BAC, Madrid 1946) par le **Clementine Text Project** de M. Tweedale, avec des lectures douteuses vérifiées sur Vercellone (1861) et Hetzenauer (1914) **[S, source secondaire]**.
  - `DistributionLicense=Public Domain`, versification `Vulg`, 78 emplacements de livres dont 73 remplis. L'appendice clémentin (Prière de Manassé, 3-4 Esdras) est absent.
  - Mesures : **35 809 versets, 4,06 Mo** de texte brut, 225 titres, sans notes.
  - Graphie à ligatures et « j » : « cælum », « ejus ». Le texte est décodé proprement.
- **eBible `latVUC`** contient le même texte avec **la Glossa ordinaria (Migne 1880) en notes**, soit 13 775 notes et 5,2 Mo. Le fichier a deux défauts : des crochets parasites « [ … ] » autour d'unités poétiques, et un en-tête de Psautier « IUXTA HEBRAICUM » trompeur alors que le texte est bien le **Psautier gallican**, avec « Dominus regit me » au psaume 22. Il est **utilisable seulement pour la Glossa**, plus tard. **[V]**
- **Le psautier gallican** est celui de la Clémentine et du missel de 1962. Les dérivés courants que sont **getbible.net** (`vulgate` et `douayrheims`, JSON) et **scrollmapper/bible_databases** (`VulgClementine.json`, `FreCrampon.json`, `DRC.json`) **redistribuent les modules CrossWire**. Le `FreCrampon.json` de scrollmapper a toutes les chances d'hériter des défauts de segmentation **[S]**. **[V]** pour la présence de ces fichiers.
- **Nova Vulgata.** vatican.va la publie avec une page HTML par livre et des numéros de verset en ligne suivis de `<br />`, sans mention de licence sur la page **[V]**. Une édition de la Deutsche Bibelgesellschaft cite « Nova Vulgata, Editio typica altera © 1986 Libreria Editrice Vaticana » **[S, via résultat de recherche]**. Logos la vend sous licence LEV **[S]**. Une **autorisation écrite de la LEV** est donc nécessaire. Elle ne servirait de toute façon que pour la forme ordinaire en latin.

### 2.4 Anglais : Douay-Rheims

| Source | Édition | Contenu | Versets | Texte brut | Notes |
|---|---|---|---|---|---|
| eBible [`engDRA`](https://ebible.org/find/details.php?id=engDRA) | Édition américaine de 1899, domaine public | USFM, 73 livres | 35 811 | 4,61 Mo | aucune |
| CrossWire `DRC` 2.0 | Challoner, source sacredbible.org, `Vulg` | SWORD | 35 805 | 4,61 Mo | aucune |
| Gutenberg [n° 8300](https://www.gutenberg.org/ebooks/8300) | Challoner 1749-1752 | Texte brut (5,65 Mo), versets en « 3:16. » | 35 770 lignes c:v | — | **oui**, en paragraphes non balisés après le verset |

Toutes ces données sont **[V]**. Dans le Gutenberg n° 8300, les annotations de Challoner sont repérables par heuristique : ce sont les paragraphes qui ne commencent pas par « c:v. ».

### 2.5 Évangiles

- **Volume mesuré** sur le Crampon 1923 : les quatre Évangiles font **0,47 Mo de texte et 0,13 Mo de notes**, et le Nouveau Testament 1,05 Mo plus 0,36 Mo de notes. Un « livre des Évangiles » est donc **un filtre** (Mt, Mc, Lc, Jn), pas un texte à part. **[V]**
- **Évangile du jour.** Il dépend des données de lectionnaire.
  - **Forme extraordinaire (1962)** : cycle annuel. Les références viennent des données de missel de l'agent 03 ou de la table du Glaire, à corriger pour 1962. Le latin est celui de la Clémentine, ce qui correspond exactement au missel.
  - **Forme ordinaire** : cycles dominicaux A/B/C et cycles feriaux I/II. Les **références** sont des faits, mais les **textes AELF sont protégés**. Le site [aelf.org/bible](https://www.aelf.org/bible) n'affiche aucune licence ; on y relève les abréviations Gn…Ap, les psaumes « 9A/9B » et « 113A/113B » et le code « XXX » pour la *Lettre de Jérémie* **[V]**. On pourrait afficher la **référence de l'AELF avec le texte de Crampon**, à condition de traiter la versification (section 2.7).
- **Concordance et parallèles.** Trois sources du domaine public existent sur Wikisource, toutes **[V]** pour leur présence et non examinées en détail :
  - la *Concorde des Évangiles* du Dictionnaire de la Bible de Vigouroux, tome II, sous forme de tables ;
  - l'*Histoire de Notre-Seigneur Jésus-Christ*, récit harmonisé en appendice du Glaire ;
  - les renvois propres au Crampon, ceux du module SWORD ou des notes `\ft` de l'USFM.

  Les [renvois d'OpenBible](https://www.openbible.info/labs/cross-references/) (environ 340 000, **CC BY**, archive de 2 Mo) existent aussi, mais avec un canon et une versification non précisés, probablement protestants **[S]**.

### 2.6 Imitation de Jésus-Christ

**Structure commune** : 4 livres de **25, 12, 59 et 18 chapitres**. Le livre IV est précédé d'une exhortation ou d'un préambule. Chaque chapitre est découpé en **paragraphes numérotés** (1., 2., …) et non en versets, et ce découpage varie légèrement d'une édition à l'autre. **[V]** pour le latin de The Latin Library et le Lamennais du Gutenberg.

| Version | Source | Qualité et statut | Taille | Remarques |
|---|---|---|---|---|
| **Latin** | [The Latin Library](https://www.thelatinlibrary.com/kempis.html), 4 pages HTML ; copie identique sur [la.wikisource](https://la.wikisource.org/wiki/De_imitatione_Christi) | Propre, avec des en-têtes « Cap. I. » et des paragraphes numérotés. **L'édition source n'est pas indiquée.** | environ 0,29 Mo | Domaine public. Il faudrait identifier l'édition, par exemple en recoupant avec Hirsche 1874 sur archive.org **[S]**. |
| **Lamennais (1824)**, 43ᵉ édition, Bray 1859 | [Gutenberg n° 57824](https://www.gutenberg.org/ebooks/57824) | **Excellente** : produite par Distributed Proofreaders d'après Google Books. Chapitres 25/12/59/18, une **« RÉFLEXION. » après chaque chapitre** (115 au total), 721 notes de références bibliques. | 0,56 Mo de corps dont 0,19 Mo de réflexions ; 0,68 Mo pour le fichier entier | **Le fichier contient aussi des prières pendant la messe tirées de Fénelon et les Vêpres du dimanche**, ce qui intéresse les agents 14 et 03. Les références sont au format « Joan., VIII, 12 » ou « Ps. XXXIII, 9 », numérotées selon la Vulgate. |
| **Lamennais**, édition Pagès, Bonne Presse 1890 | [Wikisource](https://fr.wikisource.org/wiki/L%E2%80%99Imitation_de_J%C3%A9sus-Christ_(Lamennais)) | **360 pages, dont 285 corrigées, 61 validées et 14 sans texte**. Une sous-page par chapitre, avec réflexion. | — | Utile pour contrôler le Gutenberg. Le texte a été « établi par M. Pagès » et des retouches éditoriales sont possibles **[S]**. |
| **Corneille** (paraphrase en vers, 1656), texte de Marty-Laveaux, Hachette 1862 | [Wikisource](https://fr.wikisource.org/wiki/Imitation_de_J%C3%A9sus-Christ) | **738 pages, dont 683 non corrigées**, 16 corrigées, 6 validées, 16 non créées et 17 sans texte. C'est surtout de l'OCR brute. | non mesuré | Grand intérêt littéraire, mais une relecture est indispensable. La structure en strophes demande un rendu de type poème. |
| **Gonnelieu** (traduction 1712, attribuée en réalité à J. Cusson **[S]**), avec une pratique et une prière par chapitre | archive.org : [`limitationdejes00gonngoog`](https://archive.org/details/limitationdejes00gonngoog) (Bélin-le-Prieur, 1828), McGill 1843, CIHM 1908 | **OCR ABBYY bruitée** : coquilles comme « yanité » ou « sétosualité », césures en fin de ligne. On relève 103 « PRATIQUE » et 30 « PRIÈRE » détectés. | 0,84 Mo d'OCR | Demande une campagne de correction. Gallica est **bloqué par un challenge Cloudflare** pour tout accès scripté. |
| **Marillac** (1621) | — | **Aucune source numérique trouvée** : rien sur archive.org, rien sur Wikisource **[V]**. Gallica n'a pas pu être consulté. | — | À rechercher manuellement sur Gallica **[S]**. |
| Brignon (1718) | Wikisource, titre « De l'Imitation de Jésus-Christ (Brignon) » | Statut non mesuré | — | Alternative possible. |

**Alignement latin et français (Lamennais)** **[V]** : le nombre de paragraphes numérotés est identique dans **93 chapitres sur 114, soit 82 %**. Par livre, cela donne 20 sur 25, 11 sur 12, 45 sur 59 et 17 sur 18, une fois le préambule latin du livre IV mis à part. L'affichage latin et français **en regard au niveau du paragraphe** est donc faisable, avec une correction manuelle d'une vingtaine de chapitres.

### 2.7 Versification et références

**Systèmes de versification en présence** **[V]**, d'après les fichiers SWORD `canon_catholic2.h` et `canon_vulg.h` :

- **Crampon 1923** suit `Catholic2`. Psaumes, Joël (4 chapitres) et Malachie (3 chapitres, 3,19-24) sont numérotés **selon l'hébreu**. Esther a 16 chapitres et Daniel 3,24-90 et 13-14 sont présents.
- **Clémentine, Glaire, Sacy et Douay-Rheims** suivent la `Vulg`. Psaumes, Malachie 4,1-6, Joël et le Siracide (1 592 versets contre 1 418 dans le Crampon, qui suit le grec) sont numérotés **selon la Vulgate**.
- La **forme ordinaire (AELF ou néo-Vulgate)** numérote selon l'hébreu, avec des psaumes scindés (9A/9B, 113A/113B), une *Lettre de Jérémie* séparée et les additions d'Esther repérées par des **versets à lettres**. La lettre de Esther 4 va jusqu'à des doubles lettres, comme « 17aa » **[S, d'après l'usage du lectionnaire]**.
- Le fichier SWORD contient un tableau de correspondance `mappings_vulg[]`, mais sous licence GPL-2 : à faire valider par l'agent 12 avant toute réutilisation.

**Styles de références relevés dans les sources** (tous **[V]**, sauf mention contraire) :

| Style | Exemples réels | Source |
|---|---|---|
| Liturgie moderne | « Jn 3, 16 », « Mt 5, 1-12a », « Lc 15, 1-3.11-32 », « Is 52, 13 – 53, 12 », « Ps 22 (21), 2-3 » | AELF (forme et abréviations **[V]** sur aelf.org ; exemples **[S]**) |
| Glaire 1905, table liturgique | « Epître, Rom. chap. 13, depuis le verset 11 jusqu'au verset 14 », « Ev. Matth., 11, V. 2-10 » | Wikisource, Glaire |
| Notes du Crampon | « Ps. li, 12 ; lxxxix, 13, 48 », « II Sam. xxii, 2-51 » (chiffres romains en minuscules) | USFM du Crampon |
| Notes de Lamennais | « Joan., VIII, 12 », « Eccl., I, 2 », « Ps. XXXIII, 9 » (numérotation Vulgate) | Gutenberg n° 57824 |
| Challoner | « Matt. 28. 19. » | Gutenberg n° 8300 |
| Notes de Gonnelieu | « (Ec. 1.8.) » | archive.org, OCR |
| Missel latin de 1962 | « Matth. 5, 1-12 », « Eccli. 44, 17 », « IV Reg. », « I Par. », « Apoc. » | **[S]**, à confirmer avec l'agent 03 |
| Anglais | « John 3:16 » | — |

**Pièges connus** **[V/S]** :

- **« Eccl. »** désigne l'Ecclésiaste (Qo), alors que **« Eccli. »** désigne l'Ecclésiastique (Si).
- **« I Reg. »** dans la Vulgate correspond à 1 Samuel, alors que **« 1 R »** dans l'AELF correspond à 1 Rois. De même, « III Rois » chez Glaire équivaut à « 1 R » dans l'AELF.
- « I Esdr. » correspond à Esd et « II Esdr. » à Ne. « Par. » désigne les Chroniques.
- « Jud. » désigne Jude, mais « Judic. » désigne les Juges. « Jn » se confond avec « 1 Jn » si l'espace est absent.
- La notation « Ps 22 (21) » met l'hébreu en premier dans l'AELF. Les anciens missels n'indiquent que le numéro de la Vulgate.

### 2.8 Recherche plein texte (mesures)

Le test a été fait avec SQLite 3.45.1 (Python, FTS5 activé) sur les trois Bibles décodées, avec une table `verse` et un index FTS5 sur contenu externe utilisant `unicode61 remove_diacritics 2`. Résultats **[V]** :

- « Eglise » trouve « Église » (69 versets) : les accents sont bien repliés.
- **« coeur » ne trouve rien, contre 913 versets pour « cœur ».** **« caelum » et « coelum » ne trouvent rien, contre 175 versets pour « cælum ».** **« eius » ne trouve rien, contre 4 363 versets pour « ejus ».** L'expression « vitam aeternam » ne trouve rien, contre 37 résultats pour « vitam æternam ».
- **Conséquence** : il faut indexer une colonne `text_search` normalisée et appliquer **la même normalisation à la requête**. Elle remplace œ par oe et æ par ae. En latin seulement, elle replie aussi j vers i, et éventuellement v vers u pour les éditions qui distinguent ces lettres. La recherche par préfixe (« charit* », 66 résultats) et la recherche d'expression fonctionnent.

### 2.9 Tailles

| Texte | Versets ou unités | Texte brut | Notes | SQLite sans FTS | SQLite avec FTS5 | Compressé zlib / xz | Statut |
|---|---|---|---|---|---|---|---|
| Crampon 1923 (avec notes) | 35 599 | 4,96 Mo | 1,62 Mo (8 789) | 7,6 Mo | **11,1 Mo** | 5,0 / 3,9 Mo | **[V]** mesuré sur le module décodé |
| Néo-Crampon Libre | 35 530 | 5,11 Mo | 0,48 Mo | — | environ 9 Mo | — | **[V]** pour le texte, **[S]** pour la base |
| Vulgate clémentine | 35 809 | 4,06 Mo | — | 4,9 Mo | **8,0 Mo** | 3,9 / 3,0 Mo | **[V]** |
| Douay-Rheims (DRC) | 35 805 | 4,61 Mo | (Challoner : environ 0,8 Mo **[S]**) | 5,5 Mo | **8,8 Mo** | 4,0 / 3,1 Mo | **[V]** |
| Glaire 1905 | environ 35 800 | environ 5 Mo | environ 2 à 3 Mo | — | environ 12 Mo | environ 5 Mo | **[S]**, estimation |
| Sacy | environ 35 800 | environ 5 Mo | peu | — | environ 9 Mo | environ 4 Mo | **[S]** |
| Fillion (Zefania) | 35 787 | 4,98 Mo | — | — | — | — | **[V]**, mais inutilisable |
| Les 4 Évangiles (Crampon) | 3 779 **[S]** | 0,47 Mo | 0,13 Mo | — | environ 1 Mo | — | **[V]** pour les octets |
| Imitation, latin | 114 chapitres | 0,29 Mo | — | — | environ 0,5 Mo | — | **[V]** pour le texte |
| Imitation, Lamennais avec réflexions | 114 chapitres et 115 réflexions | 0,56 Mo | 721 renvois | — | environ 1 Mo | — | **[V]** pour le texte |
| Imitation, Gonnelieu (OCR) | — | 0,84 Mo | — | — | — | — | **[V]**, brut |

**Paquet MVP** (Crampon avec notes, Clémentine, Imitation latin et Lamennais) : **environ 21 Mo installé et 8 à 10 Mo à télécharger** **[V/S]**. La taille dépend de l'indexation ou non des notes et de la décision de livrer les index tout faits ou de les construire sur l'appareil (agent 10).

---

## 3. Recommandations

### MVP

1. **Bible française : Crampon 1923, à partir de l'USFM du dépôt GitLab `crosswire-bible-society/crampon`.** L'import doit figer un commit et garder Wikisource comme source de référence en amont. Il conserve les notes `\f`, les intertitres `\s1`, `\s2` et `\ms1`, la poésie `\q1`, les italiques `\add` et le numéro de psaume selon la Vulgate qui figure dans l'en-tête.
2. **Latin : Vulgate clémentine** depuis le module CrossWire `VulgClementine`, ou mieux depuis sa source au [GitLab `vulgclementine`](https://gitlab.com/crosswire-bible-society/vulgclementine). C'est le texte des lectures du missel de 1962, et il permet l'affichage **en regard** latin et français par verset.
3. **Couche de versification minimale** : correspondance entre la Vulgate et `Catholic2` pour les **psaumes** (9-10, 114-116 et 147, avec le décalage des titres), **Malachie** 3,19-24 et 4,1-6, **Joël** 2,28-32 et 3,1-5, **Daniel** 3, **Esther** (additions) et le **Siracide**. On commence par une table construite à la main et testée, centrée sur les péricopes liturgiques.
4. **Analyseur de références** : une grammaire unique (section 2.7), alimentée par un **dictionnaire d'abréviations** pour trois styles (AELF, ancien français et latin, anglais), et un **corpus de tests** tiré des exemples réels ci-dessus. Il produit des plages normalisées dans une versification source déclarée, puis les projette sur l'édition affichée. Esquisse de grammaire :
   - une référence se compose d'un **livre**, d'un **chapitre** (en chiffres arabes ou romains, majuscules ou minuscules) avec une éventuelle **numérotation alternative entre parenthèses**, puis d'une **liste de versets** ;
   - la liste de versets est une suite de **plages séparées par « . »** ;
   - une plage va d'un verset à un autre verset, à un autre chapitre et verset, ou à « fin » ; on accepte aussi « depuis le verset 11 jusqu'au verset 14 » ;
   - un verset est un nombre suivi éventuellement d'une ou deux lettres (« 12a », « 17aa ») ;
   - **« ; »** sépare les chapitres et **« , »** ou **« : »** sépare chapitre et verset ;
   - en dehors de toute référence, le **« . »** final est ignoré.
5. **Module Évangiles = vue** filtrée sur Mt, Mc, Lc et Jn de la Bible active, avec son propre signet de tissu. L'**Évangile du jour** prend une référence fournie par l'API lectionnaire des agents 03 et 05 et la résout dans l'édition choisie. On affiche le verset entier en cas de demi-verset (« 12a »), la partie hors lecture étant estompée, voir D7.
6. **Imitation : le Lamennais du Gutenberg n° 57824** comme texte français par défaut, avec ses réflexions, plus le **latin de The Latin Library**. On aligne les paragraphes automatiquement, ce qui couvre 82 % des chapitres, et on corrige le reste à la main. Les identifiants prennent la forme `imit:L.C.P`, par exemple `imit:1.1.3`. Il faut retirer l'en-tête et la marque « Project Gutenberg », comme l'exige leur licence pour une redistribution sans la marque (agent 12).
7. **Recherche** : FTS5 sur une colonne **normalisée** (repli de œ, æ et des accents, plus j vers i et v vers u pour le latin), une recherche distincte dans les notes, l'extrait surligné (`snippet()`) et un **champ unique** qui détecte une référence (« Jn 3,16 ») avant de lancer une recherche plein texte.
8. **Contrôles automatiques à l'import**, pour l'agent 11 :
   - nombre de versets par chapitre conforme à la versification déclarée ;
   - **aucun verset vide** non attendu ;
   - **alerte au-delà de 900 caractères** ;
   - présence de chiffres isolés au milieu du texte, signe de numéros de verset collés ;
   - comparaison avec une autre édition pour repérer les décalages, en utilisant le néo-Crampon comme **outil de contrôle seulement**, sans rien copier.

### V1

- **Douay-Rheims** si l'anglais entre dans le périmètre. La source serait l'USFM `engDRA` ou le Gutenberg n° 8300, ce dernier pour les annotations de Challoner.
- **Glaire 1905**, depuis Wikisource, comme **seconde Bible française selon la Vulgate**. Sa numérotation est identique à celle du missel de 1962, il contient des introductions et des notes, et sa **Table des Épîtres et Évangiles** doit être transmise à l'agent 03.
- **Parallèles synoptiques** : une table de péricopes dérivée de la *Concorde des Évangiles* de Vigouroux (Wikisource, domaine public), plus les renvois du Crampon. Les renvois d'OpenBible (CC BY) seraient un complément, après projection de leur versification.
- **Option « nom divin »** : « Yahweh » ou « le Seigneur », par substitution grammaticale contrôlée (« de Yahweh » devient « du Seigneur », « à Yahweh » devient « au Seigneur »), si la décision D2 la retient.
- **Lamennais de 1890 sur Wikisource** comme second témoin, pour corriger les éventuelles coquilles du Gutenberg.

### Plus tard

- **Notes complètes du Crampon** de 1894-1904, à partir de l'OCR d'archive.org : gros travail de structuration.
- **Corneille en vers**, après relecture sur Wikisource, possiblement en contribuant directement à Wikisource.
- **Gonnelieu**, avec ses pratiques et prières, à partir de l'OCR de 1828, et **Marillac** une fois une source identifiée.
- **Sacy** (Wikisource 1855 complété par Furne 1841) et **Fillion**, si une transcription fiable apparaît.
- **Glossa ordinaria** (latin, eBible `latVUC`) comme commentaire. Commentaires patristiques en français : les dépôts CrossWire `freaug` (saint Augustin, 1864-1872) et `frechry` (saint Jean Chrysostome) existent **[V]** pour leur présence.
- **Nova Vulgata**, seulement si la LEV donne son accord et si la forme ordinaire en latin est demandée.
- **Néo-Crampon Libre** comme option « français modernisé », en isolant la licence CC BY-SA dans un paquet de contenu séparé.

### Modèle de données proposé (description, pas de code)

| Table | Champs principaux | Rôle |
|---|---|---|
| `edition` | `id` (`crampon1923`, `vulgclem`, `glaire1905`…), titre, langue, `versification_id`, licence, URL et version de la source, empreinte de l'import | Une ligne par texte |
| `book` | `edition_id`, `book_id` (code OSIS, par exemple `John`), ordre, titres long et court, abréviation préférée, testament, deutérocanonique | Ordre et noms propres à l'édition |
| `verse` | `edition_id`, **`vkey` entier**, chapitre, verset, partie (lettre), `label` affiché (« 17a », numéro de la Vulgate entre parenthèses), `text_markup` (balisage léger : italique, poésie, appels de note), `text_plain`, `text_search` (normalisé) | Unité stable |
| `heading` | `edition_id`, `before_vkey`, niveau, texte | Intertitres et sommaires |
| `note` | `edition_id`, `vkey`, rang, ancre (mot repère ou décalage), type (note ou renvoi), corps | Notes de Crampon, de Glaire ou de Challoner |
| `xref` | `edition_id`, `from_vkey`, cible de début et de fin (avec versification) | Renvois |
| `versification_map` | système source, `vkey` source, système cible, `vkey` cible de début et de fin | Projection entre Vulgate, hébreu et lettres |
| `book_alias` | alias (« Joan. », « Jn », « Io », « John », « S. Jean »), `book_id`, style (`aelf`, `vulg_fr`, `latin`, `en`), versification par défaut du style | Analyseur de références |
| `imit_unit` | `edition_id`, livre, chapitre, paragraphe, type (texte, réflexion, pratique ou prière), texte | Imitation, avec une table d'alignement entre latin et français |

Format de **`vkey`** : `livre(2) · chapitre(3) · verset(3) · partie(2)` dans un entier de 64 bits. Par exemple Jean 3,16 donne `43 003 016 00`. La partie vaut 00 pour un verset entier, puis 01 pour « a », jusqu'à 27 pour « aa », et ainsi de suite. **Les signets et les surlignages** (agent 09) conservent `(edition_id, vkey, décalage)` **et** une référence canonique dans une versification pivot, pour survivre à un changement d'édition.

### Points d'interface avec les autres sujets

| Agent | Échange |
|---|---|
| **03 Missel** | Format des références des lectures (forme extraordinaire : Vulgate ; forme ordinaire : AELF et néo-Vulgate), texte latin des lectures de la forme extraordinaire (Clémentine), table du Glaire, besoin d'un service `resolve(reference, versification source, édition)`, prières de messe de Fénelon dans le Lamennais |
| **05 Calendrier** | Identifiant du jour liturgique avec rite, cycle A/B/C ou I/II et couleur, d'où la référence de l'Évangile du jour (via le lectionnaire porté par 03 ou 05). La couleur du jour pilote le signet du module Évangiles. |
| **06 Moteur de lecture** | Rendu de la poésie (`\q1`), des intertitres, des notes (appel cliquable puis note en bas de page ou en panneau), des numéros de verset et de psaume (« 18 (17) »), de l'italique des mots ajoutés, et colonnes latin et français alignées par verset ou par paragraphe |
| **07 Tourne-page** | Pagination avec un chapitre ou un psaume par début de page éventuellement, et double page en paysage avec le latin à gauche et le français à droite |
| **08 UX** | Navigation livre, chapitre puis verset, champ de référence et de recherche unique, choix de l'édition, bascule de la numérotation des psaumes |
| **09 Signets** | Ancrage `(edition_id, vkey, décalage)` avec une référence canonique de secours |
| **10 Architecture** | SQLite avec FTS5 (pilote embarqué si le SQLite du framework ne fournit pas FTS5 **[S]**), paquets de contenu téléchargeables, index construits pendant le build |
| **11 Pipeline** | Importeurs USFM, OSIS, Gutenberg et wikitexte ProofreadPage, contrôles de la section 3, respect des limites de débit de Wikimedia (erreur 429 obtenue après quelques appels sans `User-Agent` documenté), pas de récupération scriptée sur Gallica (challenge Cloudflare) |
| **12 Juridique** | Statut de la révision de 1923 du Crampon, licence CC BY-SA du néo-Crampon et de Wikisource, licence et marque Gutenberg, Nova Vulgata (LEV), textes AELF, tables SWORD sous GPL-2 |
| **14 Prières** | Prières pendant la messe (Fénelon) et Vêpres du dimanche dans le Lamennais de 1859 ; prières de Gonnelieu (plus tard) |

---

## 4. Décisions à soumettre à l'utilisateur

1. **D1. Bible française par défaut**
   - (a) **Crampon 1923** : traduit sur l'hébreu et le grec, avec des notes et une source propre. C'est la recommandation.
   - (b) Glaire 1905 : traduit sur la Vulgate, ce qui le cale sur le missel de 1962.
   - (c) Néo-Crampon : français modernisé, sous CC BY-SA.
   - On peut aussi livrer (a) au MVP et ajouter (b) en V1.
2. **D2. Nom divin** : garder « Yahweh » tel qu'imprimé (7 757 occurrences) ou proposer « le Seigneur » par défaut ou en option.
3. **D3. Numérotation des psaumes** : la numérotation hébraïque avec la Vulgate entre parenthèses (style Crampon et AELF), ou une numérotation qui **suit le rite actif**, celle de la Vulgate en forme extraordinaire.
4. **D4. Anglais (Douay-Rheims)** : à inclure dès la V1 ou à laisser hors périmètre, selon la décision générale sur le multilingue.
5. **D5. Notes de Crampon** : visibles par défaut (environ 8 800 notes, risque de surcharge) ou masquées et accessibles en touchant l'appel de note.
6. **D6. Imitation** : Lamennais avec réflexions par défaut, ou texte seul avec réflexions repliables. Il faut aussi décider de l'intérêt du Corneille en vers, qui demande une relecture.
7. **D7. Demi-versets** (« 12a ») dans l'Évangile du jour : afficher le verset entier en estompant la partie hors lecture, ou découper à la main les versets concernés.
8. **D8. Nova Vulgata** : faire ou non une demande d'autorisation à la LEV. Elle n'est utile que pour la forme ordinaire en latin.
9. **D9. Évangiles** : livre distinct dans la bibliothèque (avec son signet), simple filtre de la Bible, ou les deux.

---

## 5. Risques et inconnues

| Risque | Gravité | Atténuation |
|---|---|---|
| **Défauts de segmentation des binaires dérivés** : SWORD `FreCrampon` (962 vides), Zefania Fillion (Isaïe étiqueté « Amos », Amos absent), JSON scrollmapper probablement hérités | Élevée | N'importer que depuis les sources USFM et wikitexte, et appliquer les contrôles automatiques de la section 3 |
| **Statut juridique de la révision de 1923 du Crampon en France** (réviseurs morts après 1955 ?) | Moyenne | Analyse de l'agent 12. Repli possible sur Glaire 1905, dont le statut est sûr. |
| **Versification** : pas de table de correspondance libre vérifiée (SWORD est sous GPL). Additions d'Esther avec versets à lettres, Siracide, psaumes scindés de l'AELF. | Élevée pour l'Évangile du jour de la forme ordinaire | Table construite à la main et centrée sur les péricopes du lectionnaire, avec tests |
| **Données du lectionnaire de la forme ordinaire** : textes AELF protégés ; source libre des *références* non identifiée **[S]** | Moyenne | Point à porter par les agents 03 et 05. Ne stocker que les références et résoudre le texte avec le Crampon. |
| **Normalisation de la recherche** (œ, æ, j/i) mal gérée, ce qui casse la recherche | Moyenne | Colonne normalisée, testée (section 2.8) |
| **FTS5 sous Android** : disponibilité dans le SQLite du framework non vérifiée **[S]** | Moyenne | Agent 10, par exemple avec un pilote SQLite embarqué |
| **Licences de Wikisource et de Gutenberg** : CC BY-SA sur les contributions, marque Gutenberg | Faible à moyenne | Attribution dans l'écran « Sources », retrait de la marque Gutenberg (agent 12) |
| **Sources non traçables** : le dossier Zefania FRE contient aussi des Bibles **protégées** (Bible de Jérusalem, Bible des Peuples, Pirot-Clamer) | Moyenne | N'utiliser que des sources dont la lignée est documentée |
| **Accès aux sources** : limite de débit de Wikimedia (erreur 429 obtenue), Gallica derrière Cloudflare, archives de jesusmarie bloquées | Faible à moyenne | Dumps Wikisource ou cadence lente avec `User-Agent` documenté ; téléchargements Gallica manuels ; aucun contournement de protection anti-robots |
| **Points non vérifiés** (budget de recherche épuisé) : source numérique de Marillac, éditions de Gonnelieu sur Gallica, édition du latin de The Latin Library, lettre de 2008 sur le nom divin, auteurs de la révision de 1923, données d'OpenBible (canon et versification) | — | À reprendre dans une passe de recherche complémentaire |
| **Qualité de l'OCR** pour Gonnelieu, Corneille et les notes du Crampon de 1894-1904 | Élevée pour ces textes | Repoussés à « Plus tard » ; contribuer plutôt à Wikisource |

---

## 6. Sources

Bibles :
- CrossWire, liste des modules : https://www.crosswire.org/sword/modules/ModDisp.jsp?modType=Bibles
- Modules SWORD : https://www.crosswire.org/ftpmirror/pub/sword/packages/rawzip/FreCrampon.zip ; …/VulgClementine.zip ; …/DRC.zip
- Versifications SWORD : https://www.crosswire.org/svn/sword/trunk/include/canon_catholic2.h ; https://www.crosswire.org/svn/sword/trunk/include/canon_vulg.h
- GitLab CrossWire : https://gitlab.com/crosswire-bible-society/crampon ; https://gitlab.com/crosswire-bible-society/neo-crampon-libre ; https://gitlab.com/crosswire-bible-society/vulgclementine ; liste des projets : https://gitlab.com/api/v4/groups/crosswire-bible-society/projects
- eBible : https://ebible.org/find/details.php?id=francl ; https://ebible.org/find/details.php?id=latVUC ; https://ebible.org/find/details.php?id=engDRA ; téléchargements USFM : https://eBible.org/Scriptures/{francl,latVUC,engDRA}_usfm.zip
- Wikisource :
  - https://fr.wikisource.org/wiki/Bible_Crampon_1923
  - https://fr.wikisource.org/wiki/Livre:La_Sainte_Bible,_trad_Crampon,_%C3%A9dition_1923.djvu
  - https://fr.wikisource.org/wiki/Bible_Crampon_1904
  - https://fr.wikisource.org/wiki/La_sainte_Bible_selon_la_Vulgate_(J.-B._Glaire) (avec la sous-page « Table des Épitres et Évangiles »)
  - https://fr.wikisource.org/wiki/Bible_Sacy
  - « Dictionnaire de la Bible - Vigouroux/Évangiles (Concorde des) »
- Zefania : https://sourceforge.net/projects/zefania-sharp/files/Bibles/FRE/ (Fillion : `SF_2010-05-15_FRE_FILL_…zip`)
- Fillion chez jesusmarie : http://jesusmarie.free.fr/bible_fillion.html ; http://jesusmarie.free.fr/bible_fillion_ruth.pdf
- Crampon 1894-1904 en 7 volumes : https://archive.org/details/lasaintebibletra07cram (et `…01cram` à `…05cram`)
- Douay-Rheims : https://www.gutenberg.org/ebooks/8300
- getbible : https://api.getbible.net/v2/translations.json
- scrollmapper : https://github.com/scrollmapper/bible_databases
- Renvois OpenBible : https://www.openbible.info/labs/cross-references/
- Nova Vulgata : https://www.vatican.va/archive/bible/nova_vulgata/documents/nova-vulgata_index_lt.html ; mention « © 1986 LEV » via un résultat de recherche : https://shop.die-bibel.de/media/77/93/5a/1722039590/9783438053015_Leseprobe_01.pdf.pdf
- Clementine Text Project (archive) : https://web-archive.nli.org.il/National_Library/20160526010038mp_/http://vulsearch.sourceforge.net ; https://sourceforge.net/projects/vulsearch/ ; https://biblegateway.com/versions/Biblia-Sacra-Vulgata-VULGATE
- AELF, abréviations : https://www.aelf.org/bible

Imitation :
- Lamennais 1859 : https://www.gutenberg.org/ebooks/57824
- Lamennais 1890 : https://fr.wikisource.org/wiki/L%E2%80%99Imitation_de_J%C3%A9sus-Christ_(Lamennais)
- Corneille : https://fr.wikisource.org/wiki/Imitation_de_J%C3%A9sus-Christ ; liste des éditions : https://fr.wikisource.org/wiki/L%E2%80%99Imitation_de_J%C3%A9sus-Christ
- Latin : https://www.thelatinlibrary.com/kempis.html ; https://la.wikisource.org/wiki/De_imitatione_Christi
- Gonnelieu 1828 : https://archive.org/details/limitationdejes00gonngoog

Outillage et contraintes :
- Limites de débit Wikimedia : https://www.mediawiki.org/wiki/Wikimedia_APIs/Rate_limits

Fichiers de travail (non livrés, dans le scratchpad de la session `scratchpad/04/`) : décodeur zText `scripts/ztext.py`, statistiques `scripts/stats.py` et `scripts/usfm_stats.py`, comparaison Crampon et néo-Crampon `scripts/cmp_crampon.py`, alignement de l'Imitation `scripts/imit_align.py`, test de taille SQLite et FTS5 `scripts/sizetest.py`.
