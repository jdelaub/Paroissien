# 03 — Le missel dans les différents rites

> Agent 03 · rapport de planification · 11 octobre 2026
> Périmètre : inventaire des rites, sources et licences, modèle de données du missel, interface attendue du calendrier (agent 05), rendu bilingue, trajectoire MVP.
> Conventions : **[V]** = vérifié dans cette session (fichier inspecté, page lue, requête faite) ; **[S]** = supposé, connaissance antérieure ou déduction, à confirmer.
> Le quota de recherche web partagé entre agents s'est épuisé en cours de mission. Quelques points restent donc à vérifier ; ils sont listés en §5.

---

## 1. Résumé

- **Le MVP doit porter sur le Missale Romanum 1962 (rubriques de 1960), en latin et en français.** C'est le seul rite pour lequel il existe un corpus complet, structuré, maintenu et sous licence libre : Divinum Officium (licence MIT, dernier commit le 10 octobre 2026) **[V]**. Le latin est complet, et le français couvre environ 99 % des sections du Propre du temps et au moins 88 % de celles du Propre des saints **[V]**.
- **Point juridique bloquant n° 1.** Malgré la licence MIT, la page de crédits de Divinum Officium déclare que les traductions françaises du Propre sont « l'œuvre et la propriété » de l'abbaye Sainte-Madeleine du Barroux **[V]**. Il faut donc obtenir une autorisation écrite du Barroux, ou bien publier le Propre en latin seul et ne traduire que ce qui provient de sources du domaine public.
- **Point juridique bloquant n° 2 : la forme ordinaire en français dépend entièrement de l'AELF.** Les CGU d'aelf.org limitent l'usage au cadre privé et exigent une autorisation « préalable et expresse » pour toute reproduction **[V]**. L'API `api.aelf.org` ne fournit que les lectures et la couleur, ni les oraisons ni l'Ordinaire **[V]**. La forme ordinaire ne peut donc entrer qu'en V1, et seulement après un accord avec l'AELF. L'ICEL ne concerne que l'anglais **[V]**.
- **Les missels anciens ne sont pas un raccourci.** Dom Gaspar Lefebvre est mort en 1966 : son missel reste protégé dans l'UE jusqu'au 31 décembre 2036 **[V pour la date de décès]**. Le missel du P. Feder (Mame, années 1950) est probablement protégé **[S]**. Le « Barbier » n'a pas pu être identifié. Les véritables sources du domaine public sont les *paroissiens romains* de 1866 à 1919 présents sur archive.org **[V]** et *L'Année liturgique* de Dom Guéranger **[S]**.
- **introibo.fr ne fonctionne plus.** Les captures de la Wayback Machine renvoient des 404 depuis mars 2025, et la racine du site n'affiche aujourd'hui qu'un index vide **[V]**. On ne peut pas en faire une source.
- **Modèle de données proposé.** Les « formulaires » de messe (Propre du temps, Propre des saints, Communs, messes votives) sont composés de « pièces » typées (Introït, Collecte, etc.), elles-mêmes faites de « blocs » bilingues alignés (rubrique, texte, ℣/℟, ✠, référence biblique). Un gabarit d'Ordinaire porte des emplacements pour ces pièces. Les identifiants stables reprennent les clés de fichiers de Divinum Officium (`R1962:Sancti/08-15`), comme le fait Missale Meum **[V]**.
- **Partage des rôles avec l'agent 05.** Le calendrier décide **ce qu'on célèbre** : célébration principale, commémoraisons, alternatives, couleurs, cycle des lectures. Le missel décide **quels textes on lit** : héritage des Communs, Gloria, Credo, préface, assemblage des oraisons. Le contrat d'interface est détaillé en §2.6.
- **Rendu.** En paysage, latin à gauche et français à droite, en double page, avec une pagination synchronisée par paires de blocs. En portrait, les langues alternent, ou une seule langue s'affiche avec un toucher qui révèle la traduction. Rubriques en rouge, ℣ ℟ ✠ en rouge, et deux modes au choix : « messe intégrée » ou « livre » (Propre et Ordinaire séparés, rubans).
- **Trajectoire des rites.** D'abord le 1962 général, puis le calendrier propre de la France, les messes votives, la Semaine sainte et le chant grégorien en option (V1). La forme ordinaire suit sous licence AELF (V1 ou plus tard). Viennent ensuite le dominicain, le lyonnais et l'ambrosien. Le byzantin, Divine Worship, le mozarabe, le cartusien et le prémontré restent hors feuille de route proche (§3).

---

## 2. Constats détaillés

### 2.1 Inventaire des rites et des formes : faisabilité

| Rite / forme | Usage actuel | Sources textuelles exploitables | Droits | Calendrier | Faisabilité |
|---|---|---|---|---|---|
| **Missale Romanum 1962** (forme extraordinaire, rubriques de 1960) | Communautés traditionnelles du monde entier ; nombreux fidèles francophones **[S]** | Divinum Officium : latin complet, français à environ 90-99 % **[V]** | Latin : textes anciens, risque faible **[S]**. Français : propriété du Barroux **[V]** | Calculable hors ligne ; tables de Divinum Officium et règles de Missale Meum disponibles **[V]** | **Élevée** pour le latin ; français conditionné (§2.3) |
| **Missel de Paul VI** (forme ordinaire, 3ᵉ édition typique 2002/2008, traduction française 2021) | Rite majoritaire. Traduction française en vigueur depuis le 28 novembre 2021 (au Canada depuis le 6 mars 2022) **[V]** | Oraisons, Ordinaire et lectionnaire français : AELF/Mame. Latin : Libreria Editrice Vaticana **[S]** | © AELF : autorisation préalable obligatoire **[V]**. Latin : © LEV **[S]** | Nombreux choix et calendriers propres ; l'API AELF donne la couleur et la semaine **[V]** | **Faible sans licence**, moyenne avec |
| **Dominicain** (Ordo Praedicatorum) | Usage occasionnel chez les dominicains **[S]** | Divinum Officium : calendrier `OP1962` et `OrdoOP` (Ordinaire, rubriques en anglais) ; pas de Propre OP de la messe repéré **[V]** | Missale OP de 1933 probablement du domaine public **[S]** | Calendrier propre à l'ordre | Faible à moyenne |
| **Ambrosien** (Milan) | Environ 5 millions de fidèles ; missel de 1954 encore célébré à Milan ; missel réformé de 1976, révisé en 2024 **[V]** | Divinum Officium : `OrdoA` seulement **[V]** | Éditions de 1976 et 2024 protégées **[S]** ; 1954 incertain | **Différent** : Avent de 6 semaines **[V]** ; moteur de calendrier distinct | Faible |
| **Lyonnais** | Presque éteint ; célébration hebdomadaire à Saint-Georges (Lyon) ; dernier missel en 1956 **[V]** | Aucune donnée structurée ; éditions anciennes à numériser **[S]** | Édition de 1956 : ayant droit incertain (diocèse de Lyon ?) **[S]** | Proche du romain, avec un sanctoral propre | Faible : effort de numérisation lourd, mais public francophone |
| **Cartusien** | Seul rite d'ordre encore employé pour la messe, révisé en 1981 **[V]** | Livres internes à l'ordre **[S]** | Non publics **[S]** | Propre | Très faible |
| **Prémontré** | Largement abandonné après Vatican II **[S]** | Missels anciens à numériser **[S]** | Éditions anciennes probablement du domaine public **[S]** | Propre | Très faible |
| **Mozarabe** (hispano-mozarabe) | Quotidien à la chapelle mozarabe de Tolède ; quelques usages à Salamanque et Madrid **[V]** | `OrdoM` dans Divinum Officium **[V]** ; *Missale Mixtum* de 1500 et Lorenzana (1804) anciens **[V]** | *Missale Hispano-Mozarabicum* (1988-1995) protégé **[S]** | Très différent | Très faible (curiosité) |
| **Divine Worship: The Missal** (Ordinariats anglicans) | En usage depuis le 29 novembre 2015 ; anglais seulement ; éd. CTS 2015, révisée en 2020 **[V]** | Aucune source libre | Titulaire des droits non confirmé (CTS ou Saint-Siège ?), avec des parties sous ICEL **[S]** | Calendrier de l'Ordinariat | Hors périmètre tant que l'app n'est pas en anglais |
| **Byzantin** (Églises orientales catholiques) | Melkites, Ukrainiens, Roumains… paroisses en France **[S]** | Ordinaire (Chrysostome, Basile) traduisible ; *Liturgicon* d'Edelby (1960) sur archive.org **[V]** mais protégé **[S]** | Traductions françaises modernes protégées **[S]** | **Très complexe** : Octoèque, Ménées, Triode, Pentecostaire, Typikon, julien ou grégorien selon l'Église | Ordinaire seul : moyenne avec licence ; Propre : très faible |

> Contexte non juridique : *Traditionis custodes* (2021) encadre la **célébration** selon le missel de 1962, pas sa **lecture** privée. Une application de lecture n'est pas concernée **[S]**. Le positionnement éditorial relève de l'utilisateur (§4).

### 2.2 Divinum Officium (DO) : inspection du dépôt

Clone superficiel du 11 octobre 2026, commit `57bb299` du 10 octobre 2026. Le dépôt complet pèse 464 Mo pour 40 302 fichiers **[V]**.

**Licence [V].** Le fichier `LICENSE` est la licence MIT (« Copyright (c) 2026 Divinum Officium »), qui couvre le « Software ». La page `web/www/horas/Help/credits.html` ajoute que le code et les données sont sous MIT et qu'à la connaissance de l'équipe tous les textes liturgiques sont du domaine public. **Contradiction relevée** : la même page affirme que les traductions françaises du Propre sont l'œuvre et la propriété de l'abbaye du Barroux. Elle indique aussi que la traduction française de l'Ordinaire provient de Sancta Missa (`sanctamissa.org/fr`, en 404 aujourd'hui **[V]**). Le Nouveau Testament vient de Crampon, avec le tutoiement remplacé par le vouvoiement, et l'Ancien Testament de Fillion **[V]**.

**Arborescence utile [V] :**

| Chemin | Contenu |
|---|---|
| `web/www/missa/{Latin,Francais,…}/Ordo/` | Ordinaire (`Ordo.txt`), préfaces (`Prefationes.txt`), conclusions et prières (`Prayers.txt` : Pater, Confiteor, *Per Dominum*…), prières après la messe (`Post.txt`). Ordinaires d'autres rites en latin : `OrdoA` (ambrosien), `OrdoM` (mozarabe), `OrdoS` (Sarum), `OrdoOP` (dominicain), `Ordo67` (1965-1967), `OrdoN` (nouvel Ordo) |
| `web/www/missa/*/Tempora/` | Propre du temps : `Adv1-0` (Avent, semaine 1, dimanche), `Quadp…` (Septuagésime), `Quad6-5r` (Vendredi saint réformé), `Pasc…`, `Pent01-0`… ; suffixes `r`, `t`, `o`… selon les versions rubricales **[S sur le sens exact des suffixes]** |
| `web/www/missa/*/Sancti/` | Propre des saints `MM-JJ`, suffixes `cc` (commémoraison), `pl` (Pologne)… |
| `web/www/horas/{Latin,Francais}/Commune/` (+ `Votiva/`) | **Communs et messes votives, partagés avec l'Office** (sections `[Introitus]`, `[Graduale]`… dans les mêmes fichiers) |
| `web/www/Tabulae/Kalendaria/*.txt`, `Transfer/`, `Stransfer/`, `data.txt` | Calendriers par version (`1570`, `1906`, `1954`, `1955`, `1960`, `OP1962`, monastiques…) ; tables de translations ; liste des versions rubricales et de leur héritage |
| `web/www/horas/Latin-gabc/` (26 Mo) | Partitions grégoriennes gabc **de l'Office** (pas de la messe) |

**Volumes [V].** `missa/Latin` : 1 066 fichiers, 5,2 Mo ; `missa/Francais` : 960 fichiers, 5,2 Mo. Communs latins (horas) : 63 fichiers ; Communs français : 44.

**Couverture du français (mesurée par script, lecture seule) [V] :**

| | Latin | Fichiers avec textes propres | Sections de messe présentes en français |
|---|---|---|---|
| Tempora | 526 fichiers | 164 | 1 549 / 1 569 (**98,7 %**) |
| Sancti (y compris propres locaux) | 524 fichiers | 342 | 1 793 / 2 027 (**88,5 %**, borne basse) |
| Sancti du calendrier général (`MM-JJ.txt`) | 238 formulaires | | 196 entièrement traduits ; la plupart des 42 autres héritent leurs sections manquantes d'un Commun (`@Commune/C3a`…), ce qui ne constitue pas une vraie lacune |

**Alignement latin/français [V].** Sur les 2 850 sections présentes dans les deux langues, **93,7 %** ont exactement le même nombre de lignes. L'alignement ligne à ligne est donc exploitable directement. Les 6,3 % restants (surtout `Super populum`, `Graduale` et le `Prelude` du Vendredi saint) devront être revus à la main.

**Le format des données est un mini-langage à base de lignes [V] :**

| Marque | Sens | Rendu prévu |
|---|---|---|
| `[Introitus]`, `[Oratio]`, `[Lectio]`, `[Graduale]`, `[GradualeP]`, `[Tractus]`, `[Sequentia]`, `[Evangelium]`, `[Offertorium]`, `[Secreta]`, `[Communio]`, `[Postcommunio]`, `[Super populum]`, `[LectioL1..L5]`… | Sections, c'est-à-dire les pièces du Propre | Type de pièce |
| `[X] (sed rubrica 1960)`, `(rubrica 196 aut rubrica 1955)`, `(tempore paschali)`… | Variantes conditionnelles selon la version rubricale ou le temps | À résoudre **à la compilation** pour 1960 ; garder les variantes saisonnières |
| `[Rank]` `titre;;classe;;poids;;ex C11` | Rang, poids numérique, Commun de référence | Métadonnées |
| `[Rule]` `Gloria` / `no Gloria` / `Credo` / `Prefatio=Maria=Assumptióne;` / `Full text` | Règles du formulaire | Moteur de résolution |
| `!texte` | Rubrique ou référence (`!Ps 24:1-3`) | Rouge italique ; lien vers la Bible (agent 04) |
| `!*D`, `!*S`, `!*R`, `!*SnD`, `!*RnD` | Blocs conditionnels : défunts, messe solennelle, messe lue… | Options d'affichage |
| `v.` / `V.` `R.` / `S.` `M.` `D.` `P.` | Texte ; ℣ et ℟ ; locuteurs (prêtre, servant, diacre) | ℣ ℟ en rouge, ou étiquettes de locuteur |
| `+` dans le texte ; `++` à l'Évangile | Signe de croix ; triple signe | ✠ rouge |
| `&Gloria`, `&DominusVobiscum`, `&introitus` | Macros (doxologie, dialogue, emplacement d'une pièce) | Expansion ou emplacement |
| `$Per Dominum`, `$Qui vivis` | Conclusion d'oraison à développer | Expansion depuis `Prayers.txt` |
| `@Commune/C3a-1[:Section]` | Inclusion ou héritage | Résolution d'héritage |
| `# Titre`, `!! Sous-titre`, `_`, `~` | Titres de l'Ordinaire, séparateur de strophe, continuation de ligne | Structure |

**Rubriques de 1962.** `data.txt` définit « Rubrics 1960 - 1960 » comme une version héritée de « Reduced - 1955 », elle-même tirée de « Divino Afflatu ». Il existe aussi une variante « Rubrics 1960 - 2020 USA » (calendrier `NC`) **[V]**. La Semaine sainte réformée de 1955 est fournie en textes complets (`[Rule] Full text`, par exemple `Quad6-5r.txt`, 44 ko) **[V]**. Les Quatre-Temps ont plusieurs leçons (`LectioL1`…`L5`, 5 à 13 occurrences par type) **[V]**. Les crédits mentionnent l'aide de prêtres de la FSSPX et de la FFI pour la fidélité à l'édition typique **[V]**.

**Qualité du français [V sur échantillon].** C'est une langue classique au vouvoiement, cohérente avec les missels des années 1950. On relève de rares coquilles (« mileu » dans la postcommunion du 1er dimanche de l'Avent). Les intitulés restent parfois en latin. 81 fichiers contiennent quelques mots latins, surtout dans les incipit (« Sequéntia… »).

**Réutilisation existante [V].** Missale Meum (`github.com/mmolenda/Missalemeum`, MIT, © 2022 Marcin Molenda) est une application 1962 en Python/FastAPI et Next.js qui consomme Divinum Officium en sous-module git. Elle gère l'anglais et le polonais. Le calendrier 1962 y tient en environ 300 lignes de règles (`kalendar/rules.py`). Ses identifiants de propre ont la forme `sancti:08-15:1:w` (type, clé DO, classe, couleur). Son API est « free to use » (README). Elle constitue **la référence d'architecture et un oracle de tests** pour les agents 05 et 11.

### 2.3 Traductions françaises du missel de 1962 : options

| Source | Statut | Verdict |
|---|---|---|
| **Divinum Officium, Propre français** | Propriété du Barroux selon les crédits de DO **[V]** | Excellente qualité et couverture. **Autorisation écrite requise** |
| **Divinum Officium, Ordinaire français** | Repris de `sanctamissa.org/fr` (404 aujourd'hui) **[V]** ; droits inconnus | À sécuriser : remplaçable par l'Ordinaire d'un paroissien du domaine public |
| **Dom Gaspar Lefebvre**, *Missel quotidien et vespéral* (Saint-André, 1920-1960) | Auteur mort le 16 avril 1966 **[V]** → protégé dans l'UE jusqu'au 31 décembre 2036 (calcul « vie + 70 ans ») ; édition de 1928 à la BnF **[V]** ; aucune numérisation française trouvée sur archive.org **[V]** (seulement 1962 en espagnol et le *St Andrew* anglais) | **Exclu** jusqu'en 2037, sauf licence |
| **P. José Feder SJ**, *Missel quotidien des fidèles* (Mame, 1953-1961) | Date de décès non trouvée **[V : recherche infructueuse]** ; probablement protégé **[S]** | Exclu en l'état |
| **« Barbier »** | Aucun missel bilingue de ce nom identifié **[V : recherche infructueuse]** | À préciser par l'utilisateur |
| **Paroissiens romains** (1866-1913) et *Missel de Jeanne d'Arc — paroissien romain* (1919) | Sur archive.org, certains avec la marque « Public Domain Mark 1.0 » **[V]** ; auteurs anonymes ou éditeurs **[S]** | **Source sûre** pour l'Ordinaire et les dimanches et fêtes ; calendrier antérieur à 1962, donc à compléter |
| **Dom Guéranger**, *L'Année liturgique* | Guéranger est mort en 1875 et sa continuation date d'avant 1920 **[S]** ; domaine public | Traduction intégrale des Propres, mais calendrier d'avant 1914 ; travail de réalignement et d'OCR |
| **introibo.fr** | Hors service : 404 dans les captures depuis le 12 mars 2025 ; index vide (`spip_loader.php` daté du 29 septembre 2026) le 11 octobre 2026 **[V]** | **Inutilisable** ; ne pas en dépendre |
| **Gallica** | Accès scripté bloqué par Cloudflare le 11 octobre 2026 **[V]** ; fonds non vérifiés | Vérification manuelle à faire (agent 11) |

### 2.4 AELF, ICEL et forme ordinaire

**API AELF [V]** (spécification Swagger lue sur `https://api.aelf.org/`, une requête de test) :
- Points d'accès `GET /v1/{informations|messes|lectures|laudes|tierce|sexte|none|vepres|complies}/{AAAA-MM-JJ}/{zone}`, avec `zone ∈ {afrique, belgique, canada, france, luxembourg, romain, suisse, monaco}`.
- `messes` renvoie `informations` (couleur, année A/B/C, temps liturgique, semaine, nom du jour, fête, degré) et `messes[].lectures[]` (type `lecture_1`/`psaume`/`lecture_2`/`evangile`, référence, intro, HTML). **Il ne renvoie ni oraisons, ni préface, ni Ordinaire.**
- En-têtes : `cache-control: max-age=86400`, CORS ouvert, derrière Cloudflare. **Aucune condition propre à l'API n'est publiée.**

**CGU d'aelf.org [V].** Les textes sont réservés à un usage privé. Toute reproduction exige une demande « préalable et expresse » à l'AELF. Commercialisation et rediffusion sont interdites. La page « Questions courantes » renvoie au service juridique de l'AELF, même pour un usage non commercial. Il existe un précédent : l'application libre « AELF » sur F-Droid (`co.epitre.aelf_lectures`) affiche les textes avec l'autorisation de l'AELF **[V par extrait de recherche]**.

**Missel romain 2021 [V].** C'est la traduction de la 3ᵉ édition typique, en vigueur depuis le 28 novembre 2021, éditée par Mame en lien avec l'AELF. Un document de la CECC indique que les textes français du Missel sont protégés par le droit d'auteur de l'AELF.

**ICEL [V].** L'ICEL est la commission pour l'**anglais**. Ses textes sont « All rights reserved » et soumis à redevance, y compris pour les médias électroniques à usage liturgique. Elle ne concerne Paroissien que si l'app est un jour traduite en anglais (avec aussi *Divine Worship*).

**Latin de la forme ordinaire.** *Missale Romanum* 2002/2008 et Nova Vulgata : © Libreria Editrice Vaticana **[S]**. Divinum Officium publie pourtant un `OrdoN` latin **[V]**, mais il faut faire valider ce point par l'agent 12.

**Conséquence.** Une forme ordinaire en français hors ligne suppose une licence AELF, et peut-être LEV et Mame. À défaut, une forme ordinaire « allégée » est possible : calendrier, couleur, références des lectures et textes bibliques d'une traduction du domaine public (Crampon), présentés comme « traduction non liturgique ». C'est à valider par l'agent 12, notamment pour la liste des péricopes.

### 2.5 GregoBase et la notation grégorienne (option)

**[V]** GregoBase (`gregobase.selapa.net`) ne publie pas de licence formelle. Son administrateur a écrit en 2023 que les chants sont, à quelques exceptions près, du domaine public. Les contributions nouvelles sont acceptées sous domaine public ou CC0, et les livres de Solesmes sont proposés en très basse résolution faute d'autorisation. Le format gabc est téléchargeable.

**[S]** Les signes rythmiques de Solesmes et les éditions récentes (*Graduale Novum*) peuvent être protégés ; la provenance doit être vérifiée chant par chant. Il n'existe pas de moteur de rendu gabc natif sous Android. Deux voies sont possibles, à arbitrer par les agents 06 et 07 :
- **SVG pré-rendus en CI** (Gregorio/LuaTeX) : rendu fidèle, mais sans reflow ;
- **rendu dynamique** (moteur JS de type exsurge dans une WebView) : reflow, mais dépendance web.

Divinum Officium ne contient de gabc que pour l'Office **[V]**.

### 2.6 Interface attendue du calendrier (agent 05)

**Principe.** Le calendrier décide *ce qu'on célèbre* ; le missel décide *quels textes on lit*. Le calendrier ne manipule que des **clés de célébration** et n'a jamais besoin des textes.

**`JourLiturgique`** (une instance par date, rite et calendrier) :

| Champ | Contenu | Exemple / remarques |
|---|---|---|
| `date` | ISO 8601 | `2026-08-15` |
| `calendrier` | identifiant | `R1962-general`, `R1962-france`, `OF-france`, `OP1962`… |
| `forme` | `EF1962` / `OF2002` | Détermine la grammaire des rangs |
| `temps`, `semaine`, `jourSemaine` | Énumération du temps ; numéro de semaine ; 0 = dimanche | EF : Septuagésime et Passion distincts ; OF : temps ordinaire |
| `principale` | `Celebration` | |
| `commemorations` | liste ordonnée de `Celebration` | **Déjà filtrée** selon les rubriques de 1960 (nombre maximal, exclusions) |
| `alternatives` | liste de `Celebration` | OF : mémoires facultatives ; EF : férie de classe IV permettant une messe votive ou de Requiem ; messe de la férie au lieu d'une commémoraison majeure |
| `messesMultiples` | liste de `{cle, libellé}` | Noël (nuit, aurore, jour), 2 novembre (3 messes), vigile + jour, Vigile pascale |
| `couleurs` | liste ordonnée | Pilote les signets (agent 09) ; OF : couleur d'une mémoire facultative en alternative |
| `drapeaux` | ensemble | `dimanche`, `quatreTemps`, `vigile`, `octave`, `feriePrivilegiee`, `obligation` (selon la région), `votiveAutorisee`, `requiemAutorise` |
| `cycleLectures` | OF : `{dimanche: A/B/C, semaine: I/II}` ; EF : null | |

**`Celebration`** :

| Champ | Contenu |
|---|---|
| `cle` | **Clé de formulaire stable**, reprise des chemins DO pour le 1962 : `R1962:Tempora/Adv1-0`, `R1962:Sancti/08-15`, `R1962:Commune/C11`, `R1962:Votiva/…` ; schéma analogue pour l'OF : `OF:Sancti/08-15`, `OF:TO/28-0` |
| `titre` | `{la, fr}` |
| `rang` | EF : classe I-IV plus poids DO (`6.5`, `7`) ; OF : `SOLENNITE`, `FETE`, `MEMOIRE`, `MEMOIRE_FAC` |
| `couleur` | Énumération : blanc, rouge, vert, violet, rose, noir (et or ?) |
| `categorieSaint` | Utile à l'OF pour proposer le Commun (martyr, pasteur, vierge…) |

**Ce que le missel calcule lui-même** à partir du formulaire et des drapeaux du jour : l'héritage des Communs (`ex C11`, `@Commune/…`), le Gloria et le Credo (règle `[Rule]`, plus dimanche et classe), la préface (règle, sinon préface du temps, sinon Trinité le dimanche ou préface commune), les *Communicantes* et *Hanc igitur* propres, l'ordre et les conclusions des oraisons commémorées, et le choix entre Graduel/Alléluia, Graduel pascal et Trait selon le temps.

**Ressources pour l'agent 05 [V] :**
- tables `Tabulae/Kalendaria/1960.txt` (ex. `08-08=08-09~08-08=…`, avec `~` pour la commémoraison), plus `Transfer/` et `Stransfer/` (MIT) ;
- règles 1962 de Missale Meum (MIT) ;
- oracles de test : sortie de Divinum Officium, API Missale Meum, et pour l'OF le point d'accès `informations` de l'AELF (couleur, semaine, année). Pour ce dernier, demander l'accord de l'AELF, même pour des tests **[S]**.

### 2.7 Modèle de données du missel (pack de contenu)

Le stockage (SQLite, FTS) relève des agents 10 et 11. Voici le modèle logique proposé.

| Entité | Rôle | Clés et attributs principaux |
|---|---|---|
| **Rite** | `R1962`, `OF2002-fr`, `OP1962`… | version rubricale de compilation (`Rubrics 1960 - 1960`), langues, licence |
| **GabaritOrdinaire** | L'*Ordo Missae* sous forme de suite de nœuds | `BlocFixe`, `Emplacement(typePiece)` (équivalent de `&introitus`), `Conditionnel(condition, nœuds)` avec les conditions `solennelle`, `lue`, `defunts`, `gloria`, `credo`, `aspersion`, `temps=…` |
| **Formulaire** | Une messe du Propre du temps, du Propre des saints, d'un Commun, votive ou de défunts | `cle`, `titre{la,fr}`, `rang`, `couleur`, `regles{gloria, credo, preface, communicantes, hancIgitur, texteComplet}`, `heriteDe` (Commun), `pieces[]` |
| **Piece** | Introït, Collecte, Leçon (n), Graduel, Alléluia, Trait, Séquence, Évangile, Offertoire, Secrète, Préface, Communion, Postcommunion, *Super populum*… | `type`, `index` (Quatre-Temps), `variante` (`paschal`, `septuagesime`, `avent`), `blocs[]` |
| **Bloc** | L'unité minimale **alignée entre les langues** | `id` stable, `nature` (rubrique, texte, verset ℣, répons ℟, locuteur, titre, référence biblique, macro, chant), `la`, `fr` (au choix, chacun avec ses spans : rouge, ✠, `*` médiante, `†` flexe), `refBible` (lien vers l'agent 04), `refChant` (gabc, option) |
| **Préface**, **Conclusion**, **Macro** | Catalogues partagés | `Communis`, `Trinitate`, `Nat`, `Pasch`… ; `Per Dominum`… ; `Gloria Patri`, `Dominus vobiscum` |
| **GroupeChoix** | Choix proposés au fidèle | `id`, `options[]`, `defaut`, `persistance` (globale ou par jour). Peu utilisé en EF (messes multiples, votives) ; omniprésent en OF (lecture longue ou brève, prières eucharistiques, préparation pénitentielle, Communs) |
| **ManifesteLicence** | Une entrée par source | `source`, `licence`, `autorisation` (référence de courrier), `attribution` affichée dans les crédits (agent 12) |

**Pipeline de construction (agent 11) :**
1. Épingler un commit de Divinum Officium.
2. Analyser le format à lignes et produire un arbre de syntaxe.
3. Résoudre les `@` et **aplatir les conditions rubricales pour la version de 1960** ; garder les variantes saisonnières comme données.
4. Aligner latin et français par section puis par ligne, et signaler les 6,3 % de sections non alignées.
5. Produire un JSON normalisé, puis SQLite.
6. Valider : chaque jour de 2025 à 2045 doit se résoudre en une messe latine complète, avec un rapport des trous en français.
7. Construire en CI uniquement (VM à 2 cœurs).

Taille estimée du pack 1962 latin-français : quelques Mo compressés, à partir de 10,4 Mo de texte brut **[S]**.

### 2.8 Résolution de la « messe du jour »

1. **Entrée** : un `JourLiturgique` (agent 05) et les `OptionsMesse`, à savoir le rite, la forme (lue, chantée, solennelle), l'affichage des rubriques, le mode de langue, l'alternative choisie (votive, Requiem, mémoire facultative), la messe choisie parmi les `messesMultiples`, et les choix de l'OF.
2. **Choix du formulaire** : `principale.cle`, ou l'alternative autorisée par les drapeaux.
3. **Résolution d'héritage** : chaque section absente du formulaire est prise dans son Commun, puis dans la macro concernée. Les sections absentes en français retombent sur la section héritée en français, sinon sur le latin seul (marqué comme tel).
4. **Règles du jour** : Gloria, Credo, préface, *Communicantes*/*Hanc igitur*, variante saisonnière du graduel, nombre de leçons (Quatre-Temps), Ite ou Benedicamus, fin de messe des défunts. Les jours en `texteComplet` (rites de la Semaine sainte) court-circuitent le gabarit.
5. **Commémoraisons** : on ajoute Collecte, Secrète et Postcommunion de chaque commémoraison, dans l'ordre fourni, avec leur intitulé (« Pro S. N. ») et la règle des conclusions **[S : règle exacte à coder d'après les rubriques de 1960]**.
6. **Assemblage** : on parcourt le gabarit de l'Ordinaire, on remplit les emplacements, on filtre les conditionnels, puis on développe les macros et conclusions.
7. **Sortie** : un `MesseResolue`, c'est-à-dire une suite de blocs bilingues avec des ancres stables (`ordo/canon`, `propre/introit`…) pour les signets, les rubans, la recherche et la reprise de lecture. Elle est mise en cache par (date, options).

### 2.9 Rendu (interface avec les agents 06, 07 et 08)

- **Paysage, langues en regard** : page gauche en latin, page droite en français, **pagination synchronisée par paires de blocs**. Chaque paire occupe la hauteur de la plus longue des deux versions ; on accepte un peu de blanc sous la plus courte. Sur tablette en portrait, on peut proposer deux colonnes sur une même page.
- **Portrait** : trois modes. (a) Alternance : paragraphe latin puis paragraphe français, la seconde langue dans un style distinct (italique, ou corps légèrement réduit). (b) Une seule langue. (c) Une seule langue, et un toucher sur un paragraphe révèle sa traduction.
- **Rubriques** : rouges, en italique et en corps réduit, avec une teinte adaptée à chaque thème (clair, sépia, sombre ; contraste ≥ 4,5:1). Un réglage permet de les masquer. Les intitulés de pièces sont en petites capitales rouges.
- **Signes** : ℣ (U+2123), ℟ (U+211F) et ✠ (U+2720) en rouge ; `*` et `†` des psaumes en rouge. Le `+` de Divinum Officium devient ✠ et le `++` de l'Évangile un triple ✠. Il faut des polices couvrant ℣, ℟, ✠, æ, œ et ǽ, ou une police de secours (agent 07). L'accessibilité doit lire « Verset » et « Répons ».
- **Latin** : les accents toniques de Divinum Officium (« Dómine ») sont gardés par défaut, comme dans les missels papier, avec un réglage pour les masquer. La recherche normalise les accents, `æ` en `ae`, `j` en `i` et `v` en `u`. La césure latine n'est probablement pas native sous Android (motifs TeX `hyph-la` à embarquer) **[S]**.
- **Lettrines** à l'Introït et au *Te igitur* ; Canon en corps plus grand, comme dans les missels d'autel (option).
- **Deux modes de lecture** (§4) : « messe intégrée », où le Propre est inséré dans l'Ordinaire (c'est le modèle de Divinum Officium et de Missale Meum), et « livre », avec Propre du jour, Ordinaire, Préfaces et Canon séparés et **rubans posés automatiquement** (agent 09), comme dans un missel papier.
- **Messe lue, chantée ou solennelle** : bascule qui filtre `!*S`, `!*R` et `!*D`. En messe lue, les dialogues `S.`/`M.` s'affichent en ℣/℟ ou avec les étiquettes « Le prêtre » et « Le servant » (option).

---

## 3. Recommandations

### MVP : Missale Romanum 1962, latin-français

**Pourquoi ce rite.** C'est le seul corpus complet, libre et structuré. Il correspond au public naturel d'un « paroissien », et la forme ordinaire en français est bloquée par les droits de l'AELF.

1. **Écrire immédiatement à l'abbaye du Barroux** (via l'agent 12 et l'utilisateur) pour demander l'autorisation de reprendre sa traduction du Propre telle qu'elle figure dans Divinum Officium, avec la mention d'attribution qu'elle souhaite.
2. **Contenu** : Ordinaire latin-français avec rubriques (messe lue d'abord) ; Préfaces ; Aspersion (*Asperges*, *Vidi aquam*) ; prières de Léon XIII ; Propre du temps complet ; Propre des saints du calendrier général de 1962 ; Communs nécessaires ; messe des défunts.
3. **Plan B, si le Barroux refuse ou tarde** : Propre en **latin seul**, sauf pour les dimanches et fêtes de 1ʳᵉ classe, traduits d'après un paroissien du domaine public (archive.org, 1866-1919). L'Ordinaire français doit dans tous les cas être **ré-sourcé** sur un paroissien du domaine public, puisque Sancta Missa est en 404 et que ses droits sont inconnus.
4. **Pipeline** : conversion de Divinum Officium vers un pack normalisé en CI, aplati pour les rubriques de 1960 ; tests de non-régression sur 20 ans de calendrier, comparés à Divinum Officium et Missale Meum.
5. **Interface avec l'agent 05** : adopter le contrat de §2.6, en particulier les **clés de formulaire DO** comme identifiants partagés.
6. **Rendu** : langues en regard en paysage ; alternance ou toucher pour traduire en portrait ; rubriques rouges ; ℣ ℟ ✠ ; mode « messe intégrée » par défaut.
7. **Hors MVP** : chant, votives (sauf défunts), Semaine sainte d'avant 1955, forme ordinaire et autres rites.

### V1

- **1962 complet** : calendrier propre de la France, puis éventuellement des propres diocésains ; messes votives (`Commune/Votiva`) ; messes multiples ; Semaine sainte de 1955 complète, avec ses rites (rameaux, *Mandatum*, Action liturgique, Vigile).
- **Mode « livre »** avec rubans automatiques sur le Propre du jour, l'Ordinaire, la Préface et le Canon (agent 09).
- **Option 1962 « décrets 2020 »** (saints canonisés après 1960, préfaces ajoutées) **[S : contenu à vérifier dans le calendrier `NC` de Divinum Officium]**.
- **Chant grégorien (option)** : Introït, Graduel, Offertoire et Communion depuis GregoBase, avec manifeste de provenance chant par chant et rendu à arbitrer (§2.5).
- **Forme ordinaire, à condition d'un accord AELF** (et LEV pour le latin) : Ordinaire, oraisons du Missel 2021, lectionnaire, calendrier propre de la France, groupes de choix. **Sans accord**, une forme ordinaire allégée : calendrier, couleur, références des lectures et texte Crampon marqué « non liturgique », sous réserve de validation par l'agent 12.

### Plus tard

- **Dominicain 1962** : `OrdoOP` et calendrier `OP1962` de Divinum Officium, plus numérisation d'un *Missale OP* ancien du domaine public.
- **Lyonnais 1956** : intéressant pour un public francophone, mais tout est à numériser et l'ayant droit est à identifier.
- **Ambrosien 1954** : moteur de calendrier distinct (agent 05).
- **Mode « comparer les ordinaires »** (Sarum, mozarabe, ambrosien, dominicain, 1570, 1967, nouvel Ordo), à partir des fichiers `Ordo*` de Divinum Officium, latin seul. Fonction éducative peu coûteuse, avec une vérification de provenance par fichier.
- **Byzantin** : Ordinaire de la liturgie de saint Jean Chrysostome seulement, avec licence d'une traduction française d'Église catholique orientale. Le Propre (Octoèque, Ménées) est hors de portée sans partenaire.
- **Divine Worship** : seulement si l'application passe à l'anglais (licences CTS et ICEL).
- **Non recommandés** : mozarabe, cartusien et prémontré (pas de source exploitable ou pas de public).

---

## 4. Décisions à soumettre à l'utilisateur

1. **Ordre des rites** : confirmer « 1962 d'abord, forme ordinaire ensuite » (recommandé), ou l'inverse.
2. **Barroux** : accepter que le projet sollicite l'abbaye pour sa traduction du Propre ? En cas de refus, accepter le plan B (latin seul, plus dimanches et fêtes traduits d'après un paroissien du domaine public) ?
3. **AELF** : faut-il demander une licence pour la forme ordinaire (et LEV pour le latin) ? Le modèle économique de l'app (gratuite, libre, commerciale) conditionne la réponse. Faut-il viser la forme ordinaire allégée en attendant ?
4. **Mode de lecture par défaut** : « messe intégrée » (recommandé) ou « livre » avec rubans ?
5. **Versions rubricales** : 1960 seulement, ou aussi la Semaine sainte d'avant 1955, Divino Afflatu, les décrets de 2020 ?
6. **Calendriers propres** : la France seulement, ou des diocèses ? Lesquels ?
7. **Chant grégorien** : à inclure en V1, ou reporter ?
8. **Missel « Barbier »** : de quel ouvrage s'agit-il (titre, éditeur, date) ?
9. **Rites secondaires à prioriser** après le 1962 et la forme ordinaire : lyonnais (public francophone), dominicain ou byzantin ?
10. **Conventions d'affichage** : accents toniques latins visibles par défaut ? Dialogues de messe lue en ℣/℟ ou « Le prêtre / Le servant » ?
11. **Positionnement éditorial** : présenter les deux formes à égalité ? La question est sensible depuis *Traditionis custodes*.

---

## 5. Risques et inconnues

| Risque / inconnue | Gravité | Atténuation |
|---|---|---|
| Traduction française du Propre de Divinum Officium protégée par le Barroux **[V]**, malgré la licence MIT | **Élevée** | Autorisation écrite ; sinon plan B ; manifeste de licence par texte |
| Provenance et droits de l'Ordinaire français de Divinum Officium (Sancta Missa, en 404) | Moyenne | Ré-sourcer sur un paroissien du domaine public |
| Forme ordinaire française entièrement sous droits AELF et Mame ; latin sous LEV **[S]** ; aucune condition propre à l'API **[V]** | **Élevée** pour la forme ordinaire | Négociation, forme ordinaire allégée, ou report |
| Exactitude rubricale (occurrence, concurrence, commémoraisons, conclusions) jugée par un public exigeant | Élevée | Tests de comparaison avec Divinum Officium et Missale Meum sur 20 ans ; signalement d'erreurs dans l'app |
| Complexité du format à lignes de Divinum Officium (conditions multi-versions, `@`, macros) ; dépôt très actif (commit la veille) | Moyenne | Commit épinglé ; pipeline déterministe ; diff de contenu à chaque montée de version |
| 6,3 % de sections non alignées entre latin et français **[V]** ; coquilles ponctuelles | Faible | Revue manuelle outillée ; correctifs remontés à Divinum Officium |
| introibo.fr hors service **[V]** ; Gallica inaccessible par script **[V]** | Faible | Ne pas en dépendre ; vérifications Gallica manuelles |
| Lefebvre protégé jusqu'à fin 2036 **[V/S]** ; dates de Feder inconnues ; « Barbier » non identifié | Faible | Sources du domaine public alternatives (§2.3) |
| Glyphes ℣ ℟ ✠ ǽ manquants selon la police ; césure latine non native **[S]** | Moyenne | Choix de police (agent 07) ; motifs de césure embarqués |
| Pagination synchronisée de deux langues : coût de calcul sur un appareil modeste | Moyenne | Pagination par paires, mise en cache par (date, options, taille) (agent 06) |
| Byzantin et ambrosien : calendriers différents, sources protégées | Moyenne (hors MVP) | Reporter ; partenariats |
| Recherche web interrompue (quota partagé épuisé) : Feder, ayant droit de *Divine Worship*, nouvelle adresse d'introibo, conditions LEV, fonds Gallica, sens exact des suffixes de fichiers DO **non vérifiés** | Faible | Reprendre ces vérifications dans une session ultérieure |

**Points d'interface avec les autres agents :**
- **05 Calendrier** : contrat de §2.6, clés de formulaire DO, tables `Kalendaria`, oracles de test.
- **04 Bible** : les références `!Ps 24:1-3` deviennent des liens. Les lectures de 1962 sont incluses dans les Propres (Crampon et Fillion côté Divinum Officium). Le texte biblique d'une forme ordinaire allégée viendrait de l'agent 04.
- **06 Moteur de lecture** : blocs alignés, pagination par paires, conditionnels, recherche bilingue normalisée, synthèse vocale (latin mal pris en charge **[S]**).
- **07 Tourne-page & papier** : polices et glyphes, rouge par thème, lettrines, césure latine.
- **08 UX** : entrée « Messe du jour », choix du rite, modes de langue, messe lue ou solennelle, choix et alternatives.
- **09 Signets** : couleur du jour fournie par l'agent 05 ; rubans automatiques du mode « livre ».
- **10 Architecture** : pack par rite, modules `missel` et `modèle liturgique` séparés du calendrier.
- **11 Pipeline** : conversion de Divinum Officium en CI, alignement, validation, manifeste de licence.
- **12 Juridique** : Barroux, AELF, LEV, Sancta Missa, Lefebvre, GregoBase, CTS/ICEL.
- **13 Benchmark** : Divinum Officium, Missale Meum, AELF.
- **14 Prières & extensions** : bréviaire, à partir de la même base Divinum Officium (`horas`) ; prières avant et après la messe (`Ordo/Prayers.txt`, `Post.txt`).

---

## 6. Sources

Consultées le 11 octobre 2026.

**Divinum Officium et dérivés**
- Dépôt : https://github.com/DivinumOfficium/divinum-officium (clone superficiel, commit `57bb299`, 10/10/2026) : `LICENSE`, `README.md`, `docs/how-the-calendar-works.md`, `web/www/missa/**`, `web/www/horas/*/Commune/**`, `web/www/Tabulae/**`, `web/cgi-bin/missa/{ordo.pl,Cmissa.pl}` (lus, non exécutés)
- Crédits : https://divinumofficium.com/www/horas/Help/credits.html (copie lue dans le dépôt : `web/www/horas/Help/credits.html`)
- Missale Meum : https://github.com/mmolenda/Missalemeum (README, LICENSE MIT, `backend/api/{kalendar,propers,constants}` lus, non exécutés)

**AELF, forme ordinaire, ICEL**
- API : https://api.aelf.org/ (spécification Swagger) ; requête unique https://api.aelf.org/v1/messes/2026-10-11/france
- CGU : https://www.aelf.org/page/conditions-generales-dutilisation
- Questions courantes : https://www.aelf.org/page/questions-courantes
- Mentions légales : https://www.aelf.org/page/mentions-legales
- Application AELF sur F-Droid : https://f-droid.org/packages/co.epitre.aelf_lectures (extrait de recherche)
- Droits AELF (CECC) : https://www.dioceserimouski.com/covid-19/cecc_info_diffusion_celebrations.pdf (extrait de recherche)
- Missel romain 2021 : https://www.diocese-edmundston.ca/fr/docs1/missel-livre.pdf ; https://diocesedesherbrooke.org/wp-content/uploads/2024/07/Missel-Romain.pdf ; https://www.laprocure.com/missel-romain/9782718910215.html ; https://librairiescdd.be/images/PLAQUETTE_MISSEL_ROMAIN.pdf (extraits de recherche)
- ICEL : https://www.icelweb.org/copyright.htm

**Missels anciens et sources du domaine public**
- Dom G. Lefebvre (1880-1966) : https://liturgyinstitute.org/dom-gaspar-lefebvre/ ; BnF, édition de 1928 : https://catalogue.bnf.fr/ark:/12148/cb31978736c
- P. J. Feder SJ : https://www.livre-rare-book.com/book/5472496/R200022575 ; https://catalogue.bnf.fr/ark:/12148/cb126445835 (extraits de recherche)
- API de recherche archive.org : https://archive.org/advancedsearch.php (requêtes « missel », « paroissien romain », « Lefebvre ») ; notices : https://archive.org/details/missel_de_jeanne-darc ; https://archive.org/details/paroissienromain00cath ; https://archive.org/details/paroissienromai00romagoog ; https://archive.org/details/saintandrewdaily0000domg ; https://archive.org/details/misal-diario-y-vesperal-1962 ; https://archive.org/details/Liturgicon
- introibo.fr : https://www.introibo.fr/ (index vide le 11/10/2026) ; historique : https://web.archive.org/cdx/search/cdx?url=introibo.fr/
- Sancta Missa (FR) : http://sanctamissa.org/fr/ (404)
- Abbaye du Barroux : https://www.barroux.org/
- Gallica : https://gallica.bnf.fr (accès scripté bloqué par Cloudflare)

**Chant**
- GregoBase : https://gregobase.selapa.net/?page_id=18

**Rites**
- Divine Worship : https://en.wikipedia.org/wiki/Divine_Worship:_The_Missal
- Rite lyonnais : https://fr.wikipedia.org/wiki/Rite_lyonnais
- Rite ambrosien : https://en.wikipedia.org/wiki/Ambrosian_Rite
- Rite mozarabe : https://en.wikipedia.org/wiki/Mozarabic_Rite
- Rites latins, dont le cartusien : https://en.wikipedia.org/wiki/Carthusian_Rite
