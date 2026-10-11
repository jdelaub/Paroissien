# 02 — Exercices spirituels de saint Ignace

> Rapport de planification de l'agent 02, daté du 11 octobre 2026.
> **Légende.** [V] signifie vérifié pendant cette session : fichier téléchargé et analysé, page lue ou métadonnée consultée. [S] signifie supposé, de mémoire ou à confirmer.
> **Limites.** Le quota de recherches web partagé entre les agents a été épuisé en cours de mission. Les dates des traducteurs ont donc été vérifiées sur Wikidata (avec les identifiants BnF), et non par une recherche web. Gallica renvoie un défi Cloudflare aux requêtes scriptées. Je ne l'ai pas contourné et je me suis appuyé sur archive.org et Wikisource.

---

## 1. Résumé

- **ragionline.com n'existe pas** : le nom ne se résout pas en DNS et le Wayback Machine n'en a gardé qu'un `robots.txt` de 2008 [V]. Le site visé est presque sûrement **raggionline.com** (avec deux « g »), « Raggi – spiritualità ignaziana », le site italien du P. Paolo Monaco sj. Une de ses pages propose les Exercices en PDF dans 14 langues [V].
- **Le PDF français de raggionline n'est qu'une impression**, faite en 2013 avec PDFCreator, de la page HTML de **livres-mystiques.com** [V]. On y trouve la traduction du **P. Pierre Jennesseaux sj (1804-1884)** dans son édition de 1913, numérisée puis **modernisée** par un « Frère Jérôme » (Namur, 2005), avec la mention « Édition libre de tout droit » [V]. raggionline n'indique aucune licence [V].
- **Le PDF s'extrait bien** : il a une vraie couche texte, sans OCR. On retrouve 370 numéros canoniques sur 370, les en-têtes se repèrent facilement et les appels de note sont collés aux mots. Attention : la césure automatique de `pdftotext` soude des composés comme « JésusChrist » [V]. **La source HTML est encore meilleure** : chaque paragraphe porte `id="pN"`, les titres vont de h2 à h6 et 104 références bibliques sont balisées en `<cite>` [V]. **Il faut ingérer le HTML et garder le PDF comme témoin.**
- **Les alternatives libres de droits sont bien identifiées [V].** Jennesseaux 1913 existe en original sur archive.org (OCR avec les notes de Roothaan). Debuchy 1910, traduit sur l'autographe, est plus littéral ; Debuchy (1862-1923) est dans le domaine public. Vatier 1673 est sur Wikisource, mais non relu. En latin, on a la *versio litteralis* de Roothaan (Marietti 1928, numéros marginaux) et la Vulgate, dont le texte de 1548 est inexploitable en OCR. L'espagnol autographe numéroté se trouve sur raggionline (370 sur 370, transcription d'origine inconnue).
- **La numérotation [1]–[370] est confirmée** dans les sources française, espagnole et italienne [V]. L'édition Marietti de 1928 indique employer des « numéros marginaux » à la place des pages [V]. **Le paragraphe numéroté `ex.<n>` devient la clé commune** à toutes les langues et éditions. Il sert aux renvois, aux signets, au parallèle et aux parcours de retraite.
- **MVP proposé** : lecture intégrale numérotée en français, **examen** (général en cinq points [43], particulier avec compteur [24]–[31]) et **gabarit de méditation** (oraison préparatoire, préludes, points, colloque) avec minuteur, cloche et mode « Ne pas déranger ».
- **V1** : moteur de retraite (annotation 19 dans la vie courante, retraite de 30 jours selon l'annotation 20, parcours léger selon l'annotation 18), **journal spirituel chiffré**, aide-mémoire du discernement, outil d'élection et texte en regard espagnol ou latin. **Plus tard** : retraite de 8 jours (contenu à composer et à faire relire), audio, autres langues.
- **Trois décisions à trancher** : quel texte français (version modernisée de 2005, 1913 rétabli ou Debuchy), le journal dans le MVP (seulement s'il est chiffré), et la posture face à l'accompagnateur (l'application soutient, elle ne remplace pas).
- **Principaux risques** : le statut des retouches de 2005 et celui de la transcription espagnole, la tentation de rédiger soi-même du contenu spirituel non relu, et la très grande sensibilité du journal, qui peut contenir des péchés et des élections de vie.

---

## 2. Constats détaillés

### 2.1 Enquête sur « ragionline.com »

| Vérification | Résultat |
|---|---|
| DNS de `ragionline.com`, `www.ragionline.com` et des variantes .org, .fr, .it, .net, .eu, .ch, .be, .ca, .info | Aucune résolution [V] |
| Wayback CDX `ragionline.com` | Une seule capture : `robots.txt` du 26/05/2008. Aucune page de contenu [V] |
| Recherche web « ragionline » | Rien de pertinent (homonymes) [V] |
| Variante `raggionline.com` (deux g) | Le site répond. Titre : « Raggi - spiritualità ignaziana in comunione - by Paolo Monaco sj » [V] |
| `raggionline.com/robots.txt` | 404, donc aucune restriction [V] |

**Conclusion.** Il s'agit très probablement d'une faute de frappe pour raggionline.com. *Raggi* signifie « rayons » en italien. C'est un site personnel tenu par un jésuite italien, hébergé sous IIS et construit avec des pages exportées de Word [V]. La page utile est **« Scritti di sant'Ignazio di Loyola in varie lingue »** :
`http://www.raggionline.com/saggi/scritti/scritti_di_ignazio_di_loyola_in_varie_lingue.htm` [V]. Elle propose les Exercices en `it, ch, cl, en, es, de, fr, ne, pl, pt, ru, si, ua, vn` [V], ainsi que l'Autobiographie, le Journal spirituel, les Constitutions et des Lettres. Une page de liens (`scritti_link.htm`) cite pour le français **livres-mystiques.com** comme source [V].

**Les PDF examinés** (requêtes HEAD, puis téléchargement de cinq fichiers) :

| Fichier | Taille, date serveur | Édition identifiée | Couche texte | Numéros [1]–[370] |
|---|---|---|---|---|
| `fr/exercices.pdf` | 331 Ko, 2013 | Jennesseaux (éd. 1913), numérisation de Frère Jérôme 2005 via livres-mystiques. 71 p. A4, polices Times non incorporées | Oui, native (PDFCreator + Ghostscript) | 370/370, mais [1] est collé à l'appel de note « 11 » [V] |
| `es/ejercicios.pdf` | 315 Ko, 2013 | « Texto autógrafo », orthographe du XVIe siècle (« exercicios », « annotación »). Aucune source indiquée | Oui, native | 370/370 au format `[n]` [V] |
| `en/exercises.pdf` | 210 Ko, 2024 | Elder Mullan sj, New York, Kenedy 1914, PDF d'ixtmedia.com | Oui | Aucune : l'édition de 1914 précède la numérotation [V] |
| `it/esercizi.pdf` | 606 Ko, 2016 | Traduction moderne du **Centro Ignaziano di Spiritualità (Italie)**, avec sous-versets numérotés | Oui | 370 + sous-numéros [V]. **Protégée** |
| `esercizi_cl.pdf` | 545 Ko, 2018 | **Cinghalais**, et non latin : police legacy *FMAbhaya*. « cl » vient sans doute de *Ceylon* [V] | Oui, mais illisible sans transcodage | Hors sujet |

**Licence.** Les pages de raggionline ne portent aucune mention de droit ou de licence [V]. Le site rediffuse des textes venus d'ailleurs (livres-mystiques, ixtmedia, CIS). **raggionline ne peut donc pas servir de source canonique** : il faut remonter à l'origine de chaque texte.

### 2.2 Test d'extraction du PDF français (pdftotext 24.02)

Les commandes testées sont `pdftotext` en mode brut et en mode `-layout`. L'analyse a été faite avec des scripts jetables, sous `python3 -I`.

- **Numéros de paragraphes.** 391 lignes commencent par un nombre. La plus longue suite croissante en retient **369**, de 2 à 370, sans trou. Il ne manque que [1], fusionné en « 11 » avec l'appel de note n° 1 (« 11 Première annotation… »). Les faux positifs sont les 39 lignes de la liste finale « Modifications du texte original » [V].
- **En-têtes et pieds de page.** Chaque page se termine par « - N - », et un caractère `\f` marque chaque changement de page. Il n'y a pas d'en-tête courant [V]. Les grandes sections commencent sur une nouvelle page, avec un titre en capitales sur sa propre ligne (« PREMIÈRE SEMAINE », « DISCERNEMENT DES ESPRITS »…) [V].
- **Appels de note.** 27 chiffres sont collés au mot qui précède (« pas2 », « difficile4 », « ORAISONS28 »), et un autre est isolé (« addition. 9 »). Ils renvoient à la liste des modifications de 2005 [V]. Il faut les retirer avant l'indexation.
- **Césures.** Le PDF est justifié et coupé : le mode `-layout` montre **179 fins de ligne avec trait d'union**. En mode brut, `pdftotext` recolle automatiquement les mots, mais il **soude aussi les vrais composés** : 7 « JésusChrist » et 1 « NotreDame » [V]. Il faut donc partir du mode `-layout` et appliquer sa propre décésure, avec une liste des composés (Jésus-Christ, Notre-Seigneur, c'est-à-dire, -même, peut-être…).
- **Typographie.** Le PDF n'utilise pas la ligature œ (« oeuvres », « voeu », « coeur »). Le [31] contient le tableau de l'examen particulier, dessiné en soulignés [V].
- **Verdict.** L'extraction est d'excellente qualité pour un PDF. Mais le PDF dérive du HTML, qui se parse mieux encore.

### 2.3 La source HTML : livres-mystiques.com

L'URL est `http://livres-mystiques.com/partieTEXTES/Exercices_Ignace/exercices.html`. Le `robots.txt` n'interdit que `/utils` [V]. La page fait 213 Ko en UTF-8.

- **Structure** [V] : 657 blocs, dont 436 `<p>`. **368 paragraphes portent `id="pN"`** et les deux derniers, [21] et [45], ont leur numéro en tête d'un titre h6. Avec le numéro de tête des titres, la page **couvre les 370 numéros**. On compte 68 `<p>` sans numéro (suites de paragraphe, lignes vides, tableau du [31]). Les titres vont de h2 (parties) à h6 (sous-titres), en passant par h3 (semaines), h4 (exercices et jours) et h5 (contemplations et références). Le balisage est sémantique : `<b>N</b>` pour le numéro, `<i>` pour les intitulés-rubriques (« *Premier point.* », « *Colloque.* »), `<cite>` pour **104 références bibliques** et `<q>` pour les paroles de l'Évangile. On trouve aussi 36 appels de note (`<a href="#nN"><sup>`).
- **Typographie à normaliser** [V] : 1 675 apostrophes droites, 30 mots avec « oe » au lieu de « œ », aucun guillemet français, et 554 signes `;:!?` collés au mot qui précède.
- **Erreurs relevées dans les références** [V en lecture ; correction proposée S] : au [298], « Jean 29, 30-42 » devrait sans doute être Jean 19, 38-42 ; au [279], « Luc 7, 22-25 » devrait être Luc 8, 22-25. **Il faut valider toutes les références** contre la versification du module Bible (agent 04).
- **Retouches de 2005** [V] : la liste finale annonce 39 types de modifications, « indiquées ici qu'à leur première occurrence ». On y trouve la numérotation ajoutée, l'orthographe modernisée (« très-difficile »), « Oraison dominicale » remplacé par « Notre Père », la traduction latine retirée, les références de page retirées, des références bibliques corrigées, et des lexèmes rajeunis (« soufflet », « faix », « pasteurs »). **La liste ne suffit donc pas pour reconstituer le texte de 1913.** Seul un diff avec l'original OCRisé le permet (§3.3).
- Le pied de page du site indique « © Livres-Mystiques 1997-2020 », tandis que la page elle-même déclare l'édition « libre de tout droit » [V]. La même numérisation est déposée sur archive.org (`ignace_exercices`) avec la même mention [V].

### 2.4 Alternatives libres de droits : comparaison

| # | Édition | Langue, base | Statut (droit français, à confirmer par l'agent 12) | Accès et format | N° [1]–[370] | Extraction | Fidélité et lisibilité |
|---|---|---|---|---|---|---|---|
| A | **Jennesseaux, numérisation de 2005** (livres-mystiques) | FR, traduite de l'espagnol (éd. Roothaan, Rome 1837) | Traducteur mort en 1884, domaine public [V : Wikidata Q16062832]. Retouches de 2005 déclarées « libres de tout droit » [V] | HTML structuré | **Oui, 370** | ★★★★★ | Bonne, langue du XIXe siècle légèrement rajeunie. Retouches non tracées une à une |
| B | Même texte en PDF (raggionline) | idem | idem, sans licence propre | PDF avec couche texte | Oui, 370 | ★★★★ | idem |
| C | **Jennesseaux 1913, 17e éd.**, Paris, J. de Gigord, « annotés par le R. P. Roothaan » | FR + notes de Roothaan (1785-1853) + méditations ajoutées + 2 lettres + opuscule de Roothaan | Domaine public. La mention « droits réservés » de 1913 est caduque [V : page de titre OCR] | archive.org `exercicesspirit00igna` : 462 images, PDF de 19 Mo, OCR djvu et hOCR | Non (numérotation ajoutée en 2005) | ★★ (notes de bas de page longues, titres courants, OCR moyen : « JENXESSEAUX », césures) | **Texte original**, avec un commentaire de Roothaan qui pourrait devenir une couche de notes |
| D | **Debuchy 1910**, Paris, Lethielleux, « traduits sur l'Autographe espagnol » | FR, d'après le fac-similé de 1908 | Debuchy 1862-1923, domaine public depuis 1994 [V : Wikidata Q16787386] | archive.org `exercicesspiritu00igna` : 242 images, OCR djvu | Non | ★★★ (texte sans gros apparat, 226 p.) | **Plus littérale** (« louer, révérer et servir » contre « louer, honorer et servir » chez Jennesseaux) [V] |
| E | Vatier, Anvers 1673 | FR, XVIIe siècle, probablement d'après le latin [S] | Domaine public | Wikisource `Livre:…traduction Vatier, 1673.pdf`, avancement **C** (non relu) ; Gallica `bpt6k5679765x` [V] | Non | ★ | Langue ancienne, intérêt patrimonial seulement |
| F | Abbé Clément (XVIIIe) ; Avignon 1836 | FR, traduites de la Vulgate de Frusius | Domaine public | Non localisées | Non | — | Jugées infidèles par Jennesseaux dans sa préface [V] |
| G | Meschler et Godard, 1913 (Mission Saint-Joseph) | Latin + commentaire français [S] | Domaine public [S] | archive.org `lesexercicesspir00igna`, 962 images | ? | Non examinée | — |
| H | **Autographe espagnol** (raggionline) | ES, XVIe siècle | Œuvre dans le domaine public. **Provenance de la transcription inconnue** (risque d'un droit d'éditeur, LPI espagnole art. 129 [S]) | PDF avec couche texte | **Oui, 370** | ★★★★★ | Texte de référence |
| I | Fac-similé de l'autographe (Rome, Danesi 1908) | ES manuscrit, « Spanish Autograph (1544) » selon le catalogue | Domaine public | archive.org `ejerciciosespiri00igna`, 150 images | — | Images seulement | Sert de vérité terrain pour l'espagnol |
| J | **Marietti 1928** : texte espagnol + *versio litteralis* de Roothaan « ex editione quarta romana anni 1852 » | ES + LA | Domaine public : textes anciens, édition de 1928 [S] | archive.org `exercitiaspiritu0000arpj`, OCR Tesseract 374 Ko | **Oui, marginaux** : 332 distincts détectés par OCR, dont 273 présents en ES et en LA [V] | ★★★ (pages en regard ES puis LA, séparables) | Meilleure source **latine numérotée** |
| K | Roehampton 1881, Vulgate + version littérale + notes de Roothaan | LA sur deux colonnes | Domaine public | archive.org `exercitiaspiritu00igna_0`, OCR 797 Ko | Non | ★★ (colonnes, italiques, ligatures æ/œ mal lues) | Source possible de la **Vulgate** |
| L | Pustet 1920, *versio litteralis* annotée | LA | Domaine public | archive.org `exspir00igna` | ? | ★★ | Doublon de J |
| M | Vulgate *editio princeps*, Rome 1548 | LA | Domaine public | archive.org `bub_gb_eyZ68wmgWoIC` | Non | ✗ (OCR illisible : s long, abréviations) [V] | Valeur historique seulement |
| N | **MHSI 1919**, *Exercitia spiritualia S. Ignatii et eorum Directoria* (Madrid) | Synopse en 4 colonnes (autographe, *versio prima*, Vulgate, Roothaan) + apparat | Domaine public probable [S] | archive.org `monumentaignatia01igna`, 1 300 images | ? | ✗ en automatique (colonnes et apparat mêlés) [V] | **Référence critique** pour vérifier un point douteux |
| O | Mullan 1914 | EN | Domaine public aux États-Unis (1914) [S pour l'UE] | PDF raggionline, archive.org | Non | ★★★★ | Option anglaise future |
| P | CIS (Italie) | IT moderne | **Protégée** | PDF | Oui + sous-versets | — | **À exclure** |
| Q | jesuites.com | — | — | Page de présentation **sans le texte** [V]. Renvoie aux retraites en ligne de **Prie en chemin** et aux 5 centres jésuites de la province | — | — | Utile pour le benchmark et le ton |
| R | Lecture audio Jennesseaux 1913 (archive.org `exercices_spirituels_ignace_de_loyola_livre_audio_gratuit`) | FR, audio | **CC BY-ND 4.0** [V] | MP3 découpés par section | — | — | Distribuable **sans modification** : un découpage ou un mixage fin poserait problème [S] |

Les traductions françaises modernes (Courel, Gueydan, etc.) sont protégées [S]. On ne peut les utiliser qu'avec une licence d'éditeur.

### 2.5 Structure du texte et numérotation canonique

**Les témoins.** L'*Autographe* espagnol est une copie d'un scribe, datée de 1544 dans le catalogue, avec plus d'une trentaine de corrections de la main d'Ignace [V : préface de Debuchy, notice archive.org]. Viennent ensuite la *versio prima* latine, puis la **Vulgate** de 1548, due au P. André des Freux (Frusius), qui est le texte approuvé par le bref *Pastoralis officii* de Paul III [V pour le bref (Marietti) et Frusius (préface de Jennesseaux) ; date du 31 juillet 1548 S]. Enfin, la **versio litteralis** de Roothaan date de 1835 [V : préface de Debuchy]. La **numérotation marginale [1]–[370]** est employée par l'édition Marietti de 1928, qui la substitue aux pages [V]. On l'attribue couramment à cette édition, mais son origine exacte (1919 ou 1928) reste [S]. Toutes les éditions modernes l'utilisent, et c'est la façon usuelle de citer : « ES 234 » en français, « SpEx 234 » en anglais.

**Plan canonique.** Ce plan a été vérifié paragraphe par paragraphe dans l'extraction française [V]. Il peut servir directement de **table de validation** au pipeline.

| Plage | Contenu |
|---|---|
| [1]–[20] | **Annotations** (20). Les plus utiles au produit : [4] durée d'environ 30 jours, semaines souples ; [12]–[13] une heure par exercice, à prolonger en désolation ; [18] adaptation, version légère ; **[19] vie courante, 1 h 30 par jour** ; [20] retraite fermée complète |
| [21] | Titre : « Exercices spirituels pour se vaincre soi-même… » |
| [22] | Présupposé (« Supposition préalable ») |
| **Première semaine** | |
| [23] | Principe et fondement |
| [24]–[31] | Examen particulier : trois temps [24]–[26], quatre additions [27]–[30], **tableau des lignes** [31] |
| [32]–[42] | Examen général : pensées [33]–[37], paroles [38]–[41], actions [42] |
| [43] | **Manière de faire l'examen général, en cinq points** |
| [44] | Confession générale et communion |
| [45]–[54] | 1er exercice, les trois péchés : **oraison préparatoire [46], composition de lieu [47], demande [48]**, note [49], points [50]–[52], **colloque [53]–[54]** |
| [55]–[61] | 2e exercice, ses propres péchés |
| [62]–[63] | 3e exercice, répétition et **triple colloque** [63] |
| [64] | 4e exercice, résumé |
| [65]–[71] | 5e exercice, l'enfer (application des sens) |
| [72] | Horaire des cinq exercices : minuit, lever, messe, vêpres, avant souper |
| [73]–[90] | **Dix additions** ([73]–[81], la 10e, sur la pénitence, en [82]–[86]) et remarques [87]–[90] |
| **Deuxième semaine** | |
| [91]–[100] | Appel du roi temporel (offrande [98]) et remarques |
| [101]–[109] | 1re contemplation, l'Incarnation (3 préludes [102]–[104], points [106]–[108], colloque [109]) |
| [110]–[117] | Nativité |
| [118]–[120] | Répétitions |
| [121]–[126] | **Application des sens** |
| [127]–[131] | Remarques (dont [127] : ne lire que le mystère du jour ; [128] : horaire) |
| [132]–[134] | 2e et 3e jours |
| [135] | Préambule pour considérer les états de vie |
| [136]–[148] | **Deux étendards**, triple colloque [147] |
| [149]–[157] | **Trois classes d'hommes** |
| [158]–[164] | Jours 5 à 12 et remarques |
| [165]–[168] | **Trois degrés d'humilité** |
| [169]–[189] | **Élection** : matières [170]–[174], trois temps [175]–[177], première manière en 6 points [178]–[183], seconde manière en 4 règles [184]–[188], réforme de vie [189] |
| **Troisième semaine** | |
| [190]–[209] | Contemplations de la Passion, jours 1 à 7, remarques |
| [210]–[217] | Règles de tempérance |
| **Quatrième semaine** | |
| [218]–[229] | Apparition à Notre-Dame, remarques ([229] : additions modifiées, lumière et joie) |
| [230]–[237] | **Contemplation pour obtenir l'amour** (offrande « Prenez, Seigneur » en [234]) |
| [238]–[260] | **Trois manières de prier** : commandements, péchés capitaux, puissances et sens [238]–[248] ; sens de chaque mot [249]–[257] ; au rythme de la respiration [258]–[260] |
| [261]–[312] | **Mystères de la vie du Christ** : note [261], puis 51 mystères ([262]–[312]), chacun avec références évangéliques et trois points |
| [313]–[327] | **Règles du discernement des esprits, 1re semaine** : titre et 14 règles |
| [328]–[336] | Règles de la 2e semaine : titre et 8 règles |
| [337]–[344] | Règles pour distribuer les aumônes |
| [345]–[351] | Notes sur les scrupules |
| [352]–[370] | **Règles pour sentir avec l'Église** : titre et 18 règles. Jennesseaux les intitule « De la soumission à l'Église » [V] |

**Motifs internes utiles au produit.**
1. **Gabarit de méditation** [45]–[54] : oraison préparatoire, préludes (histoire, composition de lieu, demande), points (trois puissances, ou « voir, entendre, regarder » en 2e semaine), colloque, puis Pater.
2. **Répétitions** [62], [118] : on revient « aux endroits » qui ont donné consolation ou désolation, ce qui crée un lien naturel avec le journal.
3. **Règles numérotées** : ordinal + texte, aux plages [170]–[174], [184]–[188], [210]–[217], [313]–[370].
4. **Mystères** : un titre, des références bibliques et trois points.
5. **Bloc spécial du [31]**, le graphique de l'examen particulier.
6. **Titres d'édition et titres canoniques diffèrent.** Par exemple, « De la soumission à l'Église » chez Jennesseaux correspond à « sentir avec l'Église » dans la tradition. La table des matières doit pouvoir afficher un titre canonique neutre et multilingue, distinct du titre de l'édition.

### 2.6 Points d'interface avec les autres sujets

| Agent | Interface |
|---|---|
| 04 Bible | 104 références en `<cite>` dans les mystères, à convertir en références USFM (`luk.1.26-38`). Validation de la versification et lien pop-up vers une Bible du domaine public. Deux erreurs déjà repérées (§2.3) |
| 05 Calendrier | Fête de saint Ignace le 31 juillet. Suggestion de calendrier de retraite (Carême, Avent) [S]. **Collision de vocabulaire** : la « semaine » ignatienne n'est pas une semaine liturgique, à nommer clairement dans l'interface |
| 06 Moteur de lecture | Numéros `ex.<n>` affichés **en marge**, comme chez Marietti : un détail « papier » fidèle. Intitulés en rubrique (« Premier point. », « Colloque. »). Bloc spécial du [31]. Synthèse vocale des préludes. Plein texte avec pliage œ/oe |
| 07 Tourne-page | Rien de particulier, sinon que le mode retraite n'affiche qu'une plage (voir [11] et [127]) |
| 08 UX | Mode méditation (Ne pas déranger, écran allumé, minuteur, cloche). Révélation progressive en retraite. Thème clair ou sombre suggéré selon la semaine ([79] et [229]) |
| 09 Signets | Signets sur `ex.<n>` (« ES 234 »). Contrat d'ancre de l'agent 11, inchangé |
| 10 Architecture | `feature:retreat`, `user.db` (progression, examen, journal). **Chiffrement du journal**, exclusion des sauvegardes, `AutomaticZenRule`, alarmes à fenêtre pour les rappels d'examen |
| 11 Pipeline | Publications `ignace.exercices.fr`, `.es`, `.la`. Clés `ex.<n>`. Publication `itinerary` faite de `slot`. **Confirmation : numérotation 1–370 [V]**, contrairement au [S] du rapport 11 |
| 12 Juridique | Retouches de 2005 sur livres-mystiques, transcription espagnole, édition Marietti 1928, audio CC BY-ND, exclusion du texte du CIS |
| 13 Benchmark | Rejoint le constat de l'agent 13 : aucune application française des Exercices, retraite guidée **en texte** sans équivalent |
| 14 Prières | Anima Christi (en tête de Debuchy et du CIS), Prenez, Seigneur [234], Pater, Ave, Credo et Salve, appelés par les colloques et par les 2e et 3e manières de prier. **Référencer ces prières par identifiant** (`pater`) au lieu de les recopier. L'examen de conscience des extensions recoupe [32]–[43] |

---

## 3. Recommandations

### 3.1 Choix du texte

1. **Texte français du MVP : Jennesseaux, édition livres-mystiques en HTML (source A).** Il est complet, numéroté et balisé, et son ingestion se fait en un jour [S]. On le crédite ainsi : « trad. P. Pierre Jennesseaux sj (éd. 1913), numérisation Frère Jérôme, Namur 2005 ». On conserve la liste des 39 modifications dans les métadonnées. Un message de courtoisie à `contacts@livres-mystiques.com` est recommandé (décision D2).
2. **Le PDF raggionline et l'original de 1913** servent de **témoins** dans la validation, par diff (§3.3). On n'ingère pas raggionline comme source.
3. **En V1 ou plus tard, on pourra établir une édition « Jennesseaux 1913 rétablie »** par diff avec l'OCR de 1913, avec en option les **notes de Roothaan** comme couche de commentaire. On pourra aussi proposer **Debuchy 1910** comme seconde traduction, plus littérale. Ce choix revient à la décision D1.
4. **Espagnol** : partir de la transcription raggionline (370 numéros) **pour prototyper seulement**. Avant publication, il faut soit retrouver sa provenance, soit la recontrôler avec Marietti 1928 et le fac-similé de 1908.
5. **Latin** : *versio litteralis* d'après Marietti 1928 (numéros présents). Vulgate d'après Roehampton 1881, plus tard.

### 3.2 Modèle de données

Le modèle reprend le format canonique de l'agent 11 : publications, blocs, `rev`, ancres W3C et `slot`. Il y ajoute ce qui est propre aux Exercices.

**a) Une œuvre, plusieurs éditions alignées sur la même clé.**

```
Œuvre « ignace.exercices »  (citation : « ES n », 370 unités canoniques)
 ├─ ignace.exercices.fr            (Jennesseaux/LM2005)    blocs ex.1 … ex.370
 ├─ ignace.exercices.fr-debuchy    (plus tard)             blocs ex.1 … ex.370
 ├─ ignace.exercices.es            (autographe)            blocs ex.1 … ex.370
 └─ ignace.exercices.la            (versio litteralis)     blocs ex.1 … ex.370
```

- **Clé de bloc `ex.<n>`** (clé naturelle déjà prévue par l'agent 11). Si un numéro contient plusieurs blocs (liste, tableau, titre intégré), on les note `ex.<n>.<k>` : `ex.31.2` désigne le tableau. Les titres d'édition sans numéro reçoivent un identifiant opaque frappé une fois pour toutes (règle de l'agent 11).
- **Le parallèle FR/ES/LA s'obtient par jointure sur `ex.<n>`.** Aucune table d'alignement n'est nécessaire au niveau du paragraphe.
- **Sous-ancres.** Certaines éditions (CIS) numérotent des sous-versets. Nous ne le faisons pas. On ancre à l'intérieur d'un paragraphe par décalage en points de code et par `TextQuote`, selon le contrat de l'agent 11.

**b) Le plan canonique est une donnée de l'œuvre, partagée par toutes les éditions.**

```jsonc
// divisions de l'œuvre (indépendantes de la langue de l'édition)
{ "id": "w1.ex1", "kind": "exercise", "week": 1,
  "title": { "fr": "Premier exercice : les trois péchés", "la": "Exercitium primum", "es": "Primer ejercicio" },
  "range": ["ex.45", "ex.54"], "parent": "w1" }
{ "id": "rules.disc1", "kind": "rules", "range": ["ex.313", "ex.327"],
  "title": { "fr": "Règles du discernement des esprits (1re semaine)" } }
```

On utilise une énumération de `kind` : `annotations | title | presupposition | week | day | exercise | contemplation | examen | additions | notes | rules | mysteries | mystery | methods_of_prayer`.

**c) Les rôles sémantiques des blocs** (`attrs.role`) pilotent la méditation guidée :
`preparatory_prayer` ([46]), `prelude.history`, `prelude.composition`, `prelude.petition`, `point`, `colloquy`, `note`, `rule` (avec `ordinal`), `addition` (avec `ordinal`), `examen.step` (avec `ordinal`, voir [43]), `mystery.ref` (référence biblique). Le pipeline les pose de façon semi-automatique, en repérant les intitulés en `<i>` (« Premier prélude. », « Colloque. »), puis une relecture humaine les confirme.

**d) Les parcours sont une publication `itinerary`** faite de `slot` qui pointent vers des plages de blocs, comme le propose l'agent 11.

```jsonc
{ "pub": "ignace.itineraire.annot19", "kind": "itinerary",
  "days": [
    { "id": "d001", "week": 1, "label": "Principe et fondement",
      "sessions": [
        { "type": "meditation", "minutes": 60, "template": "meditation.v1",
          "refs": ["ex.23"], "petitionRef": "ex.48", "after": ["ex.77"] },
        { "type": "examen.particulier", "refs": ["ex.24", "ex.31"] },
        { "type": "examen.general", "refs": ["ex.43"] } ],
      "advance": "user_confirms" } ] }   // [4] : on avance quand le fruit est recueilli, pas à date fixe
```

**e) Données utilisateur** (`user.db`, en accord avec les agents 09 et 10).

| Entité | Champs principaux | Sensibilité |
|---|---|---|
| `Bookmark`, `Highlight` | ancre (contrat de l'agent 11), couleur | faible |
| `RetreatProgress` | itinéraire, jour courant, sessions faites, date de début, rythme | moyenne |
| `ExamenParticularLog` | date, période (midi ou soir), sujet visé, compte | **élevée** |
| `ExamenGeneralLog` | date, durée, notes facultatives | **élevée** |
| `PrayerSessionLog` | date, référence, durée réelle, prolongée oui ou non ([13]) | moyenne |
| `JournalEntry` | id, date, jour de retraite, refs `ex.<n>`, grâce demandée, mouvement (consolation, désolation, neutre ; [316]–[317]), texte libre, notes pour l'accompagnateur ([17]), drapeau « à reprendre en répétition » ([62]) | **très élevée → chiffrée** |
| `ElectionWorksheet` | question, avantages et inconvénients des deux options ([181]), étranger, lit de mort, jugement ([185]–[187]), décision, confirmation ([183]) | **très élevée → chiffrée** |

### 3.3 Pipeline : du PDF au texte structuré, puis au format canonique

Ce pipeline s'inscrit dans celui de l'agent 11 (Python, sous `content/ignace/`). Il n'y a rien d'applicatif ici : c'est une description d'étapes.

0. **Acquisition, une seule fois.** On télécharge, on calcule l'empreinte sha256 et on renseigne le manifeste : URL, date, licence déclarée, éditeur, traducteur. Chaque fichier va dans un dossier propre, et on traite les données comme non fiables (`python3 -I`).
1. **Lecture propre à chaque source.**
   - *HTML LM* : DOM, puis suite de blocs `{h2…h6, p}` (déjà prototypé : 370 numéros sur 370) [V].
   - *PDF* : `pdftotext -layout`, et **pas le mode brut**, qui soude les composés. On peut aussi utiliser `-bbox-layout` pour récupérer la taille des polices et distinguer titres et notes.
   - *Scans archive.org* : **hOCR** plutôt que djvu.txt, pour avoir les coordonnées, les colonnes et la taille de police. Cela permet de séparer le corps du texte des notes de Roothaan (en petit corps) et des titres courants.
2. **Nettoyage.** Retirer `\f`, « - N - » et les titres courants (« ANNOTATIONS. 3 »). Retirer les appels de note : chiffres collés de 1 à 39 dans l'ordre, en épargnant les vraies références (« Jean 2, 1-11 »). Décésurer avec un lexique de composés. Joindre les lignes.
3. **Segmentation en unités canoniques.**
   - Sources numérotées : ancres `^(\d{1,3}) ` ou `\[(\d{1,3})\]`, puis **plus longue sous-suite croissante** pour écarter les faux positifs, et lecture des numéros placés en tête de titre ([21], [45]).
   - Sources non numérotées (1913, Debuchy, Vulgate) : **alignement sur un pivot numéroté**. Pour la même langue, on fait un diff au niveau des phrases entre l'OCR de 1913 et LM2005, qui reporte les numéros et révèle les retouches de 2005. Entre langues, Debuchy contre l'espagnol, on s'appuie sur des indices ordinaux (« Première règle », « Second point », « Colloque ») et sur un **fichier d'ancres manuelles** pour les cas ambigus.
4. **Structure.** Rattacher les titres d'édition au plan canonique (table du §2.5) et générer les divisions de l'œuvre.
5. **Enrichissement.** Intitulés en rubrique, rôles (§3.2 c), références bibliques converties en USFM, renvois internes (« règles de la seconde semaine » → `rules.disc2`), bloc `chart` pour le [31].
6. **Normalisation typographique du français.** Apostrophe typographique, œ restauré par dictionnaire (« oeuvre » devient « œuvre », mais « coexister » reste intact), espaces fines insécables avant `;:!?`, guillemets « », majuscules accentuées (« A cette fin » devient « À cette fin »). Ces règles sont partagées avec l'agent 06 pour la recherche (pliage œ/oe).
7. **Validation, bloquante en CI.** Contrôler la présence des 370 unités dans l'ordre, la cohérence des plages avec le plan canonique, et l'absence de chiffre parasite. Les références bibliques doivent être valides dans la versification de l'agent 04. Pour le diff contre le témoin (PDF raggionline), un seuil de divergence de 1 % est un point de départ [S]. Le rapport de churn des identifiants vient de l'agent 11. Enfin, une **relecture humaine** d'un échantillon d'environ 5 % se fait contre les images des scans.
8. **Émission.** Le JSON canonique est commité et sert de vérité. Il est compilé en paquet SQLite + FTS5 (agent 11), d'une taille de moins de 1 Mo par langue d'après l'agent 10.

**Effort estimé [S]** : source A, 1 à 2 jours avec la validation ; espagnol, 1 jour ; latin Marietti, 2 à 3 jours (séparation ES/LA des pages en regard et 38 numéros à retrouver) ; 1913 rétablie avec les notes de Roothaan, 4 à 6 jours, relecture comprise ; Debuchy aligné, 4 à 6 jours.

### 3.4 Produit : fonctions et lot proposé

| Fonction | Description | Fondement dans le texte | Lot |
|---|---|---|---|
| **Lecture intégrale** | Livre complet, numéros en marge, table des matières canonique, renvois cliquables (« voir ES 313 »), signets « ES n », recherche plein texte, glossaire court (consolation, désolation, composition de lieu, colloque, application des sens, répétition, additions) | tout | **MVP** |
| **Examen général** | Parcours guidé en **5 étapes** : action de grâces, demande de lumière, revue (pensées, paroles, actions), pardon, résolution et Pater. Durée de 10 à 15 min, rappel du soir facultatif (alarme à fenêtre, agent 10), notes facultatives **sans historique par défaut** | [43], [32]–[42] | **MVP** |
| **Examen particulier** | Un sujet choisi, une résolution le matin, deux comptages (midi et soir), et le **graphique des lignes** du [31] qui compare les jours et les semaines ([29]–[30]). Geste discret : « la main sur la poitrine » ([27]) correspond à un tap rapide | [24]–[31] | **MVP** |
| **Méditation structurée** | Lancée depuis n'importe quel exercice ou mystère. Étapes : se tenir un instant avant ([75]), oraison préparatoire [46], préludes, points, colloque (prière liée au module 14), puis **revue de 15 min** ([77]) qui ouvre le journal en V1. **Minuteur de 60 min par défaut** ([12]), bouton « prolonger un peu » ([13]), cloche, écran maintenu allumé, Ne pas déranger via `AutomaticZenRule` (agents 08 et 10). Lecture vocale des préludes en option | [45]–[54], [101]–[109], [121]–[126] | **MVP** (gabarit et minuteur) |
| **Retraite selon l'annotation 18** (légère) | Examens, 1re manière de prier sur les commandements [238]–[248], confession hebdomadaire suggérée. Quelques semaines | [18] | **V1** |
| **Retraite dans la vie courante (annotation 19)** | Environ **1 h 30 par jour** ([19]) en une ou deux sessions, dans l'ordre des Exercices, sur plusieurs mois [S pour la durée usuelle]. Rythme réglable, **progression confirmée par l'utilisateur** et non par le calendrier ([4]). **Révélation progressive** : on ne montre que le jour en cours ([11], [127]) | [4], [11], [19], [127] | **V1** |
| **Retraite de 30 jours (annotation 20)** | Programme tiré **uniquement du texte**. 1re semaine : exercices 1 à 5 et horaire [72]. 2e semaine : jours 1 à 12 et élection à partir du 5e jour. 3e semaine : jours 1 à 7. 4e semaine : souple. Mode « retraite fermée » (DND renforcé, horaires [72] et [128]) | [20], [72], [101]–[164], [190]–[209], [218]–[237] | **V1** |
| **Journal spirituel privé** | Entrées liées à `ex.<n>`, marquage du mouvement, « à reprendre en répétition » qui pré-remplit la répétition suivante ([62], [118]), notes pour l'accompagnateur ([17]). **Chiffré, local, sans sauvegarde cloud par défaut**, verrou biométrique, export chiffré choisi par l'utilisateur, effacement sûr | [6], [17], [62], [77], [316]–[317] | **V1**, ou MVP seulement s'il est chiffré (D3) |
| **Aide-mémoire du discernement** | Si l'entrée du journal porte la mention « désolation », on rappelle [318]–[322] (« ne pas changer » en désolation). Les règles de 2e semaine ne s'affichent pas avant la 2e semaine ([9]–[10]) | [313]–[336], [8]–[10] | **V1** |
| **Outil d'élection** | Feuille guidée : six points, avantages et inconvénients, l'étranger, le lit de mort, le jugement, puis offrande et confirmation. **Avec un avertissement : l'élection se fait accompagnée** ([15]) | [169]–[189] | **V1** tardive |
| **Texte en regard ES/LA** | Jointure sur `ex.<n>` | — | **V1** |
| **Notes de Roothaan en commentaire** | Couche de notes optionnelle | — | Plus tard |
| **Retraite de 8 jours** | **Absente du texte d'Ignace** : c'est une tradition [S]. Le choix des méditations doit être **composé et relu par un prêtre**, ou repris d'un manuel du domaine public | — | Plus tard |
| **Audio** | Synthèse vocale (agent 06), ou lecture CC BY-ND diffusée telle quelle | — | Plus tard |
| **Thème selon la semaine** | Suggestion facultative : sombre en 1re et 3e semaines ([79]), lumineux en 4e ([229]) | [79], [229] | Plus tard (détail qui fait plaisir) |
| **Autres langues** (EN Mullan, etc.) | — | — | Plus tard |

**Garde-fous pastoraux.** Ce sont des recommandations fortes, fondées sur le texte lui-même.
- Afficher dès l'entrée en retraite que les Exercices se **donnent par un accompagnateur** ([2], [6]–[10], [15], [17], [22]). jesuites.com le rappelle aussi, avec ses 5 centres et Prie en chemin [V]. L'application soutient la retraite, elle ne joue pas le rôle de l'accompagnateur.
- **Pas de série de jours, pas de score, pas de badge** (même ligne que l'agent 13). Le seul « comptage » est celui de l'examen particulier, voulu par Ignace lui-même.
- Ne jamais **rédiger** de méditation au nom de la tradition sans relecture. Les parcours V1 se bornent à **ordonner des passages du texte** : c'est notre travail, et il ne crée pas de doctrine.

---

## 4. Décisions à soumettre à l'utilisateur

| # | Décision | Options | Recommandation |
|---|---|---|---|
| D1 | **Quel texte français ?** | (a) Jennesseaux modernisé en 2005 (LM), (b) Jennesseaux 1913 rétabli d'après l'OCR, (c) Debuchy 1910, plus littéral, (d) plusieurs au choix | **(a) pour le MVP**, puis (b) ou (c) en V1 selon la préférence entre lisibilité et fidélité |
| D2 | **Remercier ou demander à livres-mystiques ?** | Reprise silencieuse avec crédit, ou message de courtoisie | Message de courtoisie et crédit visible ; avis de l'agent 12 |
| D3 | **Journal dans le MVP ?** | Pas de journal au MVP, ou journal chiffré dès le MVP | **Pas de journal non chiffré, jamais.** S'il est dans le MVP, il doit être chiffré dès la première version |
| D4 | **Posture face à l'accompagnateur** | Application autonome, ou application de soutien avec export pour l'accompagnateur | Soutien, avec export **choisi et chiffré** |
| D5 | **Ampleur de la retraite guidée en V1** | Annotation 19 seule, ou 19 + 30 jours + 18 | 19 et 18 d'abord (vie courante), 30 jours ensuite |
| D6 | **Langues de référence** | FR seul, FR + ES, FR + LA, ou les trois | FR au MVP, ES et LA en V1 (la jointure ne coûte presque rien une fois les textes établis) |
| D7 | **Retraite de 8 jours** | L'abandonner, la composer avec un prêtre, ou reprendre un manuel du domaine public | Plus tard, avec un relecteur ecclésiastique identifié |
| D8 | **Licence d'une traduction moderne** (Courel, Gueydan…) | Ne pas en avoir, ou négocier | Non pour l'instant, à reconsidérer si les utilisateurs le demandent |

---

## 5. Risques et inconnues

| Risque ou inconnue | Gravité | Mitigation |
|---|---|---|
| **Statut des retouches de 2005** : 39 types de modifications, une originalité minime mais non nulle. Le site affiche « © 1997-2020 » à côté de la mention « libre de tout droit » | Moyenne | La mention explicite « libre de tout droit » joue en notre faveur. Crédit et message de courtoisie. Plan B : texte de 1913 rétabli (domaine public certain) |
| **Transcription espagnole de raggionline** d'origine inconnue (droit espagnol sur les éditions [S]) | Moyenne | Ne pas la publier telle quelle. La recontrôler et la rétablir avec Marietti 1928 et le fac-similé de 1908 |
| **Erreurs de références bibliques** dans LM2005 (au moins 2 repérées) | Moyenne | Validation automatique par l'agent 04 et relecture |
| **Données du journal et de l'examen** : péchés, élection de vie. Données d'une sensibilité extrême | **Élevée** | Chiffrement au repos, exclusion de la sauvegarde automatique et du transfert d'appareil, verrou, aucune télémétrie, export uniquement à l'initiative de l'utilisateur. Choix de bibliothèque à confirmer par l'agent 10 (SQLCipher ou Tink avec clé Keystore [S]) |
| **Dérive vers un « coach spirituel »** : contenu rédigé, conseils automatiques, élection guidée par l'application | Élevée sur le plan pastoral | Parcours = ordre de passages du texte. Avertissements fondés sur [15] et [17]. Aucune génération de contenu |
| **Ne pas déranger** : depuis Android 15, une `AutomaticZenRule` est nécessaire, avec l'accès « Ne pas déranger » accordé par l'utilisateur (agent 10) | Faible | Repli : silence de l'application et écran allumé seulement |
| **Origine de la numérotation** : édition de 1919 ou de 1928 ? | Faible (son usage est universel) | À confirmer dans l'OCR de MHSI 1919 si l'on veut l'écrire dans les crédits |
| **Qualité OCR** des sources 1913, Debuchy et latines | Moyenne (V1) | hOCR, alignement et relecture. Budget de 4 à 6 jours par édition |
| **Audio CC BY-ND** : l'intégrer au lecteur ou le découper peut constituer une « adaptation » | Faible | Le diffuser tel quel, ou y renoncer |
| **Non vérifiés** : les dates de Mullan, le contenu de Meschler et Godard 1913, la date exacte du bref, la durée usuelle d'une retraite selon l'annotation 19, les fonds de Gallica (bloqué) | Faible | Reprendre la recherche quand le quota sera disponible |

---

## 6. Sources

**ragionline / raggionline**
- Wayback CDX (capture unique de `robots.txt`, 2008) : https://web.archive.org/cdx/search/cdx?url=ragionline.com&matchType=domain
- Accueil de raggionline : http://www.raggionline.com/
- Écrits d'Ignace en plusieurs langues : http://www.raggionline.com/saggi/scritti/scritti_di_ignazio_di_loyola_in_varie_lingue.htm
- PDF : http://www.raggionline.com/saggi/scritti/fr/exercices.pdf, …/es/ejercicios.pdf, …/en/exercises.pdf, …/it/esercizi.pdf, …/esercizi_cl.pdf
- Page de liens et sources : http://www.raggionline.com/saggi/scritti/scritti_link.htm

**Textes français**
- livres-mystiques (HTML) : http://livres-mystiques.com/partieTEXTES/Exercices_Ignace/exercices.html (robots : http://livres-mystiques.com/robots.txt)
- Jennesseaux 1913 (Gigord, 17e éd.) : https://archive.org/details/exercicesspirit00igna
- Debuchy 1910 (Lethielleux) : https://archive.org/details/exercicesspiritu00igna
- Numérisation Frère Jérôme : https://archive.org/details/ignace_exercices
- Meschler et Godard 1913 : https://archive.org/details/lesexercicesspir00igna
- Vatier 1673 (Wikisource) : https://fr.wikisource.org/wiki/Livre:Exercices_spirituels_d%27Ignace_de_Loyola,_traduction_Vatier,_1673.pdf et la page auteur https://fr.wikisource.org/wiki/Auteur:Ignace_de_Loyola
- Audio, CC BY-ND 4.0 : https://archive.org/details/exercices_spirituels_ignace_de_loyola_livre_audio_gratuit
- Wikidata, Pierre Jennesseaux (1804-1884) : https://www.wikidata.org/wiki/Q16062832 ; Paul Debuchy (1862-1923) : https://www.wikidata.org/wiki/Q16787386

**Latin et espagnol**
- Vulgate 1548 : https://archive.org/details/bub_gb_eyZ68wmgWoIC
- Roehampton 1881 (Vulgate et version littérale) : https://archive.org/details/exercitiaspiritu00igna_0
- Pustet 1920, *versio litteralis* : https://archive.org/details/exspir00igna
- Marietti 1928 (ES et LA, numéros marginaux) : https://archive.org/details/exercitiaspiritu0000arpj
- MHSI 1919, *Exercitia spiritualia et Directoria* : https://archive.org/details/monumentaignatia01igna
- Fac-similé de l'autographe, 1908 : https://archive.org/details/ejerciciosespiri00igna

**Contexte**
- jesuites.com, Exercices spirituels : https://www.jesuites.com/exercices-spirituels/
- Prie en chemin, retraites : https://prieenchemin.org/les-retraites/

**Rapports frères consultés** : `11-pipeline-contenu.md` (identifiants `ex.<n>`, ancres, `itinerary`), `10-architecture-android.md` (`AutomaticZenRule`, alarmes, `feature:retreat`) et `13-benchmark.md` (absence d'application française des Exercices).
