# Sources des données du calendrier

Ce fichier donne, pour `content/data/calendar/` et les oracles de `content/golden/calendar/`, la provenance, la licence et la version figée de chaque source. Le format est décrit dans `content/schema/calendar-v1.md`. On n'a repris aucun texte protégé : seulement des faits (dates, rangs, couleurs) et des titres courts.

## 1969 : calendrier romain général et propre de France

### Sources

| Source | Licence | Version figée | Ce qu'on en tire |
|---|---|---|---|
| **romcal**, [github.com/romcal/romcal](https://github.com/romcal/romcal), branche `dev` | MIT, « Copyright (c) 2018 Julian Pereira » (The Romcal Team) | 3.0.0-dev.140, commit `3cde69c21fcf3176b8728cf0a54d0ca8f3d97166` du 2026-10-09 | Définitions des calendriers `general_roman`, `europe` et `france`, identifiants, rangs, couleurs, titres `fr` et `la`, oracle `1969-fr-romcal.jsonl` |
| **LitCal**, [github.com/Liturgical-Calendar/LiturgicalCalendarAPI](https://github.com/Liturgical-Calendar/LiturgicalCalendarAPI), branche `development` | Apache-2.0, J. Romano D'Orazio | commit `d8efa8cf48e4f17412a2eab81ae89dad100cccb9` du 2026-10-10 | `validFrom`, contrôle des rangs et des dates, titres français mis en regard dans `content/sources/calendrier-1969/titres-litcal.tsv` |

L'application doit reproduire la notice MIT de romcal dans ses mentions légales, ainsi que la mention Apache-2.0 de LitCal.

Les scripts se trouvent dans `content/sources/calendrier-1969/` ; les commandes sont dans son `README.md`. Les sorties brutes de romcal vont dans `content/.cache/raw/calendrier-1969/`, qui n'est pas versionné.

### Ce que contiennent les fichiers

**`1969/general.json`** compte 219 entrées : les 221 définitions du calendrier `general_roman` de romcal, moins deux fêtes mobiles.
- **Date** : on reprend `dateDef: {month, date}`. Neuf fêtes sont placées par romcal au moyen d'une fonction (`dateFn`) ; on garde leur date nominale, vérifiée sur la sortie de romcal :
  - Présentation, 02-02 ;
  - Annonciation, 03-25 ;
  - Nativité de saint Jean-Baptiste, 06-24 ;
  - saints Pierre et Paul, 06-29 ;
  - Transfiguration, 08-06 ;
  - Assomption, 08-15 ;
  - Croix glorieuse, 09-14 ;
  - Toussaint, 11-01 ;
  - Immaculée Conception, 12-08.

  Les reports que romcal code en dur (Annonciation, saint Joseph, Immaculée Conception) sont décrits dans `1969/TEMPORAL.md`, § « Transferts ».
- **Rang** : le rang de romcal. Les Défunts (`commemoration_of_all_the_faithful_departed`) sont une `SOLEMNITY` chez romcal, au degré 3 des NU, avec les couleurs `VIOLET` puis `BLACK`.
- **Couleurs** : celles de la définition romcal. romcal colore les martyrs en `RED` d'après son martyrologe, et écrit `PURPLE` là où le schéma dit `VIOLET`.
- **`lord: true`** : les fêtes au degré 5 de romcal (`GENERAL_LORD_FEAST_5`). Il y en a quatre : Présentation, Transfiguration, Croix glorieuse et Dédicace du Latran. Les solennités du Seigneur restent sans ce drapeau, puisque leur degré est de toute façon supérieur.
- **Titres** : `fr` vient de la locale `fr` de romcal, dont on retire les dates de mort « († …) » et à laquelle on applique les corrections listées plus bas. `la` vient de la locale `la`, quand elle existe. Quand romcal n'a pas de latin, son bundle renvoie l'anglais : dans ce cas, on omet `la`.
- **`saints`** : omis. Il n'existe pas encore de registre de `SaintId`. Les identifiants du martyrologe de romcal sont disponibles dans `romcal-defs.json` (champ `martyrology`).

**`1969/fr.json`** compte 21 entrées. Il reprend les couches `europe` et `france` de romcal (chez romcal, le calendrier France a pour parent la région Europe), et il porte le précepte.
- **Nouvelles entrées du propre de France** (10) :
  - sainte Geneviève (01-03) ;
  - saint Remi (01-15) ;
  - sainte Bernadette (02-18) ;
  - sainte Louise de Marillac (05-09) ;
  - saint Yves (05-19) ;
  - sainte Jeanne d'Arc (05-30, `MEMORIAL`, `patron: SECONDARY`) ;
  - saints Pothin, Blandine et leurs compagnons (06-02) ;
  - sainte Clotilde (06-04) ;
  - saint Césaire d'Arles (08-26) ;
  - Notre-Dame de La Salette (09-19).
- **Patrons de l'Europe** (5), en `FEAST` avec `patron: PRINCIPAL` (degré 8c de romcal, « patron principal d'une région ») :
  - saints Cyrille et Méthode ;
  - sainte Catherine de Sienne ;
  - saint Benoît ;
  - sainte Brigitte ;
  - sainte Thérèse-Bénédicte de la Croix.
- **Modifications d'entrées générales** :
  - saint Joseph de Calasanz passe au 08-26 ;
  - saint Louis devient `MEMORIAL` ;
  - sainte Thérèse de l'Enfant-Jésus reçoit `patron: SECONDARY` et le titre de patronne de la France ;
  - l'Assomption reçoit `patron: PRINCIPAL`, `obligation` et le titre de patronne principale. Sa précédence reste celle d'une solennité générale ; romcal la met même au degré 2.
- **Précepte** : Toussaint et Noël (voir plus bas).
- Les entrées qui remplacent une entrée générale sont complètes : date, rang, couleurs, titre et `validFrom` sont recopiés. Elles donnent donc le même résultat qu'on les lise comme un remplacement ou comme une surcharge champ par champ. Quand romcal n'a pas de latin pour le titre propre à la France, on garde le latin de l'entrée générale (cas de l'Assomption).
- **Non repris** :
  - `dedication_of_consecrated_churches`, la Dédicace des églises consacrées dont on ignore la date. romcal en fait une solennité facultative (`isOptional`) le 25 octobre, avec le dernier dimanche d'octobre comme autre date possible, et elle n'est célébrée que dans les églises concernées. Le schéma n'a pas de champ pour une solennité facultative. Dans l'oracle, elle figure parmi les `optional` du 25 octobre.
  - `our_lady_of_lourdes` et `john_mary_vianney_priest` : romcal France n'y change que le commun, rien qui soit visible dans le schéma.

### Corrections locales

- **Précepte.** Les drapeaux `isHolyDayOfObligation` de romcal sont faux pour la France (rapport 05, §2.2) et ne sont pas repris. Au sanctoral, romcal marque saint Joseph, saints Pierre et Paul, l'Assomption, la Toussaint et l'Immaculée Conception ; au temporal, il marque notamment Sainte Marie Mère de Dieu, l'Épiphanie, la Trinité et le Saint-Sacrement. Seules ont `obligation: true` dans `fr.json` les fêtes suivantes :
  - **Noël** (`nativity_of_the_lord`). Cette fête appartient au temporal de romcal (voir `1969/TEMPORAL.md`). Son entrée dans `fr.json` ne sert qu'à porter le précepte : le moteur doit l'appliquer à la célébration du temporal de même identifiant, et non créer une seconde célébration le 25 décembre ;
  - **l'Assomption** ;
  - **la Toussaint**.

  L'**Ascension**, mobile, est d'obligation en France ; le code s'en charge.
- **Titres français** (`convert.py`, `TITLE_FR_FIXES`). On ne corrige que les erreurs objectives :
  - « Michel, Gabriel **and** Raphaël » devient « et » ;
  - « **m**artyre de Saint Jean-Baptiste » prend une capitale initiale ;
  - « Guadalup**é** » devient « Guadalupe » ;
  - « d'**E**cosse » devient « d'Écosse » ;
  - « **É**poux de la vierge Marie » devient « époux » ;
  - « Edith Stein, … **martyr** » devient « martyre » ;
  - « vierge Marie » devient « Vierge Marie » partout (6 titres).

  On laisse en l'état le style de romcal, souvent plus descriptif que celui des calendriers liturgiques usuels. Exemples :
  - « Saint Antoine le Grand, ermite en Égypte » ;
  - « Saint Isidore de Séville, docteur de l'Église, évêque et Confesseur » ;
  - « Saint Alexis Falconieri et les fondateurs des Servites » ;
  - « Saint Joseph, Artisan » ;
  - des majuscules à « Saint » au milieu d'un titre, ou à des noms d'ordres (« Jésuite », « Carmélite »).

  Les 135 titres qui diffèrent de ceux de LitCal sont listés dans `content/sources/calendrier-1969/titres-litcal.tsv`, pour relecture. Le français de LitCal a aussi ses défauts (« Martin 1er », « Achillé »).
- **Couleurs** : `PURPLE` devient `VIOLET`, sans autre changement.

### Dates de validité (`validFrom`)

romcal ne date pas les inscriptions. Les années viennent de LitCal. `litcal.py` associe les 219 fêtes à date fixe des deux moteurs, par la date puis par le titre anglais, et exige qu'elles se correspondent toutes.

- **Par décret** (10), avec l'année du `since_year` de LitCal ; la date du décret est aussi citée en commentaire dans le source de romcal :
  - saints Jean XXIII et Jean-Paul II : 2014 ;
  - saint Paul VI et Notre-Dame de Lorette : 2019 ;
  - sainte Faustine : 2020 ;
  - saint Grégoire de Narek, saint Jean d'Avila et sainte Hildegarde : 2021 ;
  - sainte Teresa de Calcutta : 2025 ;
  - **saint John Henry Newman : 2026** (décret du 9 novembre 2025).
- **Par édition typique du Missel** (22) : 2002 pour les 19 ajouts de la 3ᵉ édition, 2008 pour saint Pio, Notre-Dame de Guadalupe et saint Juan Diego. C'est l'année de la première édition typique qui les porte ; le décret d'inscription peut être plus ancien. On reprend ces années pour rester cohérent avec l'oracle LitCal.
- **Non modélisé**, parce que le schéma ne peut pas changer le rang, la date ou le titre d'une entrée selon l'année :
  - sainte Marie-Madeleine, mémoire jusqu'en 2015 puis fête (décret de 2016) ;
  - sainte Jeanne-Françoise de Chantal, au 12 décembre avant 2002 ;
  - sainte Marthe, seule avant 2021, puis Marthe, Marie et Lazare ;
  - le titre de docteur de sainte Thérèse (1997) et de saint Irénée (2022).

  Rien de tout cela ne touche les années postérieures à 2022.

### Différences entre romcal et LitCal

- **Rangs et dates (état actuel)** : aucune différence sur les 219 fêtes à date fixe. On tient compte du déplacement de sainte Jeanne-Françoise de Chantal, codé en dur dans LitCal, et de la fête de sainte Marie-Madeleine.
- **Cœur immaculé de Marie et mémoire obligatoire le même jour.** LitCal rend les deux facultatives. romcal sort les deux comme obligatoires, le Cœur immaculé en premier.
- **Années d'inscription.** LitCal applique les dates d'inscription ; romcal donne Newman dès 2025, et Marie Mère de l'Église avant 2018.

### Fêtes mobiles laissées au code

Elles ne sont dans aucun fichier : `core:liturgy` les place. Les identifiants, les règles de date, les rangs et les couleurs sont dans `1969/TEMPORAL.md`.
- **Du sanctoral général de romcal** :
  - `mary_mother_of_the_church` : lundi de Pentecôte, mémoire, **inscrite en 2018** (décret du 11 février 2018) ;
  - `immaculate_heart_of_mary` : samedi qui suit le Sacré-Cœur, mémoire.
- **Fêtes du Seigneur et solennités du temporal** :
  - `holy_family_of_jesus_mary_and_joseph` ;
  - `epiphany_of_the_lord` (en France, le dimanche du 2 au 8 janvier) ;
  - `baptism_of_the_lord` ;
  - `ascension_of_the_lord` (en France, le jeudi, d'obligation) ;
  - `most_holy_trinity` ;
  - `most_holy_body_and_blood_of_christ` (en France, le dimanche) ;
  - `most_sacred_heart_of_jesus` ;
  - `our_lord_jesus_christ_king_of_the_universe`.
- **Non reprise** : `dedication_of_consecrated_churches`, le 25 octobre ou le dernier dimanche d'octobre, solennité facultative (voir plus haut).

### Oracle `content/golden/calendar/1969-fr-romcal.jsonl`

L'oracle donne la sortie de romcal pour la France, en années civiles de 2025 à 2045 : 7 670 lignes, avec la configuration `scope: gregorian`, l'Épiphanie et le Saint-Sacrement au dimanche, l'Ascension le jeudi. L'en-tête `1969-fr-romcal.meta.json` donne le commit, les commandes, la date, l'empreinte sha256 et les notes. Correspondance des champs :
- `id` et `rank` : la première célébration de romcal ;
- `colors` : celles de romcal, avec `PURPLE` écrit `VIOLET` ;
- `optional` : tout ce que romcal marque `isOptional` ;
- les cycles : ceux de la célébration principale.

**Défauts de romcal dans cette plage**, repérés par `convert.py` et à reporter dans `known-divergences.md` quand le moteur tournera :
- **Saint Joseph absent en 2035.** Le 19 mars tombe le lundi saint. romcal code lui-même le report au samedi qui précède les Rameaux (`dateExceptions`), soit le 17 mars, mais ne l'applique pas.
- **Nativité de saint Jean-Baptiste absente en 2033 et 2044.** Le Sacré-Cœur tombe le 24 juin. romcal supprime la solennité au lieu de la transférer. En 2022, le Dicastère l'avait avancée au 23 juin (rapport 05, §2.1.3).
- **Le Cœur immaculé de Marie, simple mémoire, évince une fête ou une solennité** :
  - saint Jean-Baptiste le 24 juin 2028 ;
  - saints Pierre et Paul le 29 juin 2030 et en 2041 ;
  - saint Thomas le 3 juillet 2038.
- **Jours à deux célébrations non facultatives** : on ne garde que la première.
  - Jeudi saint : `holy_thursday` puis `thursday_of_the_lords_supper` ;
  - Cœur immaculé de Marie et une mémoire obligatoire, 7 jours.
- **À vérifier** : le 26 mai 2042, romcal garde saint Philippe Néri et omet Marie Mère de l'Église.
- **Hors défaut, attendu** :
  - Newman figure en 2025 ;
  - les drapeaux de précepte de romcal ne sont pas repris ;
  - la Dédicace des églises consacrées figure parmi les `optional` du 25 octobre (18 années sur 21).

## 1962 : calendrier général (rubriques de 1960)

### Sources

| Source | Licence | Version figée | Ce qu'on en tire |
|---|---|---|---|
| **Divinum Officium**, [github.com/DivinumOfficium/divinum-officium](https://github.com/DivinumOfficium/divinum-officium) | MIT, « Copyright (c) 2026 Divinum Officium » | commit `b9c100d79ae481c67b2801fdd3622b00dd86bd73` du 2026-10-11 | Kalendarium de 1960 et ses bases (`Tabulae/Kalendaria`), rangs et titres latins (`horas/Latin/Sancti`), clés `proper` (`missa/Latin`), substitutions et transferts de 1960 (`Tabulae/Tempora`, `Tabulae/Transfer`), traduction anglaise des *Rubricae generales* (page sous CC BY-SA 2.5, citée par numéro seulement), Calendarium du Bréviaire de 1961 (`horas/Help/Rubrics/1960calendarium.pdf`) qui a servi au contrôle |
| **Missalemeum**, [github.com/mmolenda/Missalemeum](https://github.com/mmolenda/Missalemeum) | MIT, « Copyright (c) 2022 Marcin Molenda » | commit `88c491d17d7e7edabaa43d2cf02463413336fb75` du 2026-10-07 | Oracle `1962-missalemeum.jsonl` ; contrôle des classes, couleurs et commémoraisons ; règles résumées dans `1962/TEMPORAL.md` § 12 |
| **romcal**, branche `dev` | MIT | commit `3cde69c21fcf3176b8728cf0a54d0ca8f3d97166` | Slugs : ceux du calendrier général de 1969 quand la célébration est la même, sinon le style de romcal et de son martyrologe |

L'application doit reproduire les notices MIT de Divinum Officium et de Missalemeum dans ses mentions légales. Aucun texte de messe n'est repris : seulement des faits (dates, classes, couleurs, clés) et des titres courts.

Les scripts se trouvent dans `content/sources/calendrier-1962/`. Les commandes, les choix détaillés et la correspondance des identifiants de l'oracle sont dans son `README.md`.

### Ce que contiennent les fichiers

**`1962/general.json`** compte 269 entrées et 62 commémoraisons attachées.
- **Classes** : 11 de Iʳᵉ classe, 28 de IIᵉ, 180 de IIIᵉ ; 46 jours qui ne sont qu'une commémoraison ; 3 vigiles de IIᵉ classe et 1 de IIIᵉ.
- **Champs** :
  - `proper` est la clé de propre de messe de DO pour 1960, par exemple `Sancti/12-28r` ou `Sancti/08-09` (saint Jean-Marie Vianney au 8 août) ;
  - `lord: true` pour cinq fêtes : Présentation, Précieux Sang, Transfiguration, Exaltation de la Croix et Dédicace du Latran.
- **Contrôle** : les classes et les commémoraisons ont été relues contre le Calendarium du Bréviaire de 1961, sans écart.
- **Écarts avec Missalemeum** : saint Ubald et saint Didace (IIIᵉ classe), sainte Barbe, et 13 couleurs. Ils sont listés dans le README.
- **Commémoraisons intrinsèques** :
  - saint Pierre et saint Paul (n. 110), octave de Noël : elles figurent au Calendarium, mais DO les met dans le propre du jour. Elles n'ont donc pas de `proper` ;
  - Litanies majeures le 25 avril : `Sancti/04-25c`.
- **Titres** :
  - `la` vient de DO, avec 13 coquilles corrigées ;
  - `fr` est saisi dans `content/sources/calendrier-1962/sanctoral.tsv` d'après l'usage des missels français. Une quinzaine de titres sont à relire (README).
- **`saints`** : omis.

**`1962/TEMPORAL.md`** est écrit à la main. Il donne les clés de propre du temporal, la règle de date, la classe, la couleur et les titres, le résumé ordonné des règles de Missalemeum, et une proposition d'identifiants alignés sur 1969. `check.py` vérifie que chaque clé citée existe dans DO.

### Fêtes mobiles laissées au code (1962)

Elles ne sont pas dans `general.json`. `1962/TEMPORAL.md` donne leurs clés et leurs règles.
- **Cycle de Noël**, rangé au *Proprium de Tempore* du Missel de 1962, même pour les jours à date fixe :
  - vigile de Noël (24 décembre, qui remplace le 4e dimanche de l'Avent) ;
  - Noël et ses trois messes ;
  - dimanche dans l'octave ;
  - 29-31 décembre ;
  - Octave de la Nativité (1er janvier) ;
  - féries du 2 au 5 janvier ;
  - Épiphanie ;
  - féries du 7 au 12 janvier ;
  - Baptême (13 janvier).
- **Fêtes mobiles du Seigneur** :
  - Saint Nom de Jésus (dimanche du 2 au 5 janvier, sinon le 2) ;
  - Sainte Famille (1er dimanche après l'Épiphanie) ;
  - Trinité, Fête-Dieu, Sacré-Cœur ;
  - Christ-Roi (dernier dimanche d'octobre, clé `Sancti/10-DU`).
- **Temporal mobile** :
  - Septuagésime, Cendres, Carême, temps de la Passion, Semaine sainte ;
  - Pâques et son octave, Rogations, vigile et fête de l'Ascension ;
  - vigile et octave de la Pentecôte ;
  - Quatre-Temps de l'Avent, du Carême, de la Pentecôte et de septembre ;
  - dimanches après l'Épiphanie repris en novembre.
- **Commémoraison des Sept Douleurs** le vendredi de la Passion (Calendarium).
- **Sainte Marie au samedi** (n. 78).
- **Déplacements calculés par le code** :
  - saint Matthias (24 → 25 février) et saint Gabriel de la Vierge des Douleurs (27 → 28 février) les années bissextiles ;
  - Défunts un dimanche, reportés au 3 novembre (n. 96 b) ;
  - Annonciation transférée après *in Albis* (n. 96 a) ;
  - transferts des fêtes de Iʳᵉ classe (n. 95-99) ;
  - Litanies majeures si Pâques ou le lundi de Pâques tombe le 25 avril (n. 80).

### Oracle `content/golden/calendar/1962-missalemeum.jsonl`

Il donne la sortie de Missalemeum en latin pour les années civiles 2020 à 2045 : 9 497 lignes, avec l'en-tête `1962-missalemeum.meta.json`. Les identifiants de Missalemeum sont ramenés aux clés de DO (`sancti:10-11:2:w` → `Sancti/10-11`, `tempora:Pent19-0:2:g` → `Tempora/Pent19-0`, substitutions de 1960, dimanches repris en `Tempora/PentEpi…`). Six clés propres à Missalemeum, des commémoraisons intrinsèques, sont gardées telles quelles. La table de correspondance est dans `content/sources/calendrier-1962/README.md`, § « Oracle ».

**Défauts de l'oracle**, à reporter dans `known-divergences.md` quand le moteur tournera :
- couleurs fausses pour 13 fêtes (292 jours), en général la couleur du saint commémoré ;
- féries de la semaine de la Sainte Famille en blanc après le 13 janvier ;
- vigile de saint Laurent rouge, et remplacée par la Sainte Vierge le samedi ;
- saint Ubald et saint Didace réduits à une commémoraison ;
- sainte Barbe et saint Pierre (30 juin) absents ;
- pas de commémoraison du dimanche aux fêtes de Iʳᵉ classe, sauf saint Michel et saints Pierre et Paul ;
- une seule commémoraison par jour.

## 1962, propre de France

`1962/fr.json` est **saisi à la main**. La recherche complète se trouve dans `content/sources/calendrier-1962-fr/RECHERCHE.md` : tableau sourcé, désaccords, règles des solennités extérieures, points à trancher.

### Sources

| Source | Statut | Version figée | Ce qu'on en tire |
|---|---|---|---|
| **Abbaye Sainte-Madeleine du Barroux**, *Ordo* monastique en latin, https://www.barroux.org/ordo-liturgique/ | **Ordo de référence** (décision de l'utilisateur), ramené au calendrier romain. Protégé : on n'en tire que des faits | `ordo-2026.pdf`, sha256 `a04e7515…0313a40` ; `ordo-2025.pdf`, sha256 `06d65998…bf4408080` | Fêtes marquées « in Prop. » (*Proprium provinciæ Gallicæ*), patronnes de la France, messes « pro fidelibus » (solennités extérieures) |
| **Ordo de la FSSPX**, source `gallia`, API `https://ordo.fsspx.org/api/v1/calendar/{a}/{m}` (via laportelatine.org/ordo) | Recoupement, faits seulement | Interrogée le 2026-10-11 (2025 à 2027, et des mois de 2028 à 2057) | Classes, solennités, précepte, cas limites des solennités empêchées |
| **introibo.fr**, propres diocésains et propre de France | Recoupement ; site hors service, lu dans la Wayback Machine | Captures de 2021 | Mention « propre de France » et classe de chaque messe |
| **Francefidele/ordo-liturgique** | **Sans licence : consultation seulement, rien n'est repris** | Commit `b44a05cf5ac0788cbe5e5f608d49d5d08fae0595` | Recoupement pour 2026 |
| **Divinum Officium** | MIT | Commit `b9c100d79ae481c67b2801fdd3622b00dd86bd73` | Titres latins et clés `proper` des entrées qui remplacent une entrée générale ; rubriques de 1960 (n. 42, 341, 356-361). Aucun calendrier gallican |

### Ce que contient `1962/fr.json` (8 entrées)

- **Nouvelles fêtes** :
  - sainte Geneviève (01-03, III) ;
  - sainte Jeanne d'Arc (05-30, II, `patron: SECONDARY`, *Rubricae generales* n. 42 b).

  Aucune des deux n'a de propre de messe romain dans Divinum Officium : `proper` est donc omis.
- **Le 30 mai** : l'entrée générale `felix_i_pope`, une simple commémoraison, est retirée (`remove`) et rattachée comme commémoraison à sainte Jeanne d'Arc. Le fichier garde ainsi une seule entrée par jour, comme `1962/general.json`.
- **Entrées générales remplacées** (complètes : rang, couleurs, titre, `proper`) :
  - saint Remi (10-01), qui passe de commémoraison à IIIᵉ classe ;
  - sainte Thérèse de l'Enfant-Jésus (10-03), qui passe de la IIIᵉ à la IIᵉ classe, avec `patron: SECONDARY` ;
  - l'Assomption (08-15), qui reçoit `patron: PRINCIPAL`, `obligation` et le titre de patronne principale ;
  - la Toussaint (11-01), qui reçoit `obligation`.
- **Noël** (12-25, `obligation`) appartient au temporal de 1962. Comme en 1969, son entrée ne fait que porter le précepte : le moteur l'applique à la célébration du temporal de même identifiant. L'identifiant `nativity_of_the_lord` est à vérifier quand le temporal de 1962 sera écrit.
- **Précepte** : celui du droit en vigueur (Noël, Ascension, Assomption, Toussaint), que les Ordos de 1962 consultés appliquent aussi. L'Ascension, mobile, est laissée au code.
- **Non repris**, faute d'accord entre les sources (voir `RECHERCHE.md`, § 5) :
  - sainte Clotilde, sainte Jeanne de Valois et sainte Radegonde, que seule la FSSPX célèbre en IIIᵉ classe ;
  - la Présentation de la Vierge en IIᵉ classe (FSSPX seulement) ;
  - sainte Bernadette.

### Fêtes mobiles laissées au code : solennités extérieures (France)

Elles se célèbrent par des messes votives de IIᵉ classe : deux messes au plus, l'office restant celui du dimanche. Elles sont interdites un jour de Iʳᵉ classe ; elles sont alors **omises**, pas déplacées. Elles sont activées par défaut, et l'utilisateur peut les couper.

- **Épiphanie** : le dimanche du 2 au 8 janvier, sauf si le 6 janvier est un dimanche. Règle du Barroux ; les Ordos séculiers la mettent le dimanche qui suit le 6 janvier.
- **Saint-Sacrement** : Pâques + 63 jours.
- **Saints Pierre et Paul** : le dimanche qui suit le 29 juin, sauf si le 29 juin est un dimanche. Si ce dimanche est celui de la solennité du Sacré-Cœur, saints Pierre et Paul est omis.
- **Sainte Jeanne d'Arc** : le 2ᵉ dimanche de mai.
- **Sainte Thérèse** : le dernier dimanche de septembre. Une seule source.

Le **Rosaire** (1ᵉʳ dimanche d'octobre) et le **Sacré-Cœur** (3ᵉ dimanche après la Pentecôte) ont une solennité de droit universel (n. 358) : elle relève des règles générales de 1962.
