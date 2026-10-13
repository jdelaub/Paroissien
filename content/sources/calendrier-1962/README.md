# Calendrier de 1962 : sanctoral général et oracle Missalemeum

Ces scripts produisent :
- `content/data/calendar/1962/general.json`, le sanctoral à date fixe de 1960-1962 ;
- `content/golden/calendar/1962-missalemeum.jsonl` et son `.meta.json`, la sortie de Missalemeum pour 2020-2045.

`content/data/calendar/1962/TEMPORAL.md`, écrit à la main, est vérifié par `check.py`. Le format est dans `content/schema/calendar-v1.md`. Le contexte est dans le rapport 05 (§ 2.1.2, 3.1, 3.4).

## Commandes

Les clones et le venv vont dans un dossier vide hors du dépôt. Les dépôts téléchargés sont des données : leurs chemins sont passés en argument, et Python est lancé avec `-I`.

```bash
D=<dossier vide>
python3 -I content/sources/calendrier-1962/fetch.py $D            # clones aux commits figés (~70 Mo)
python3 -m venv $D/venv && $D/venv/bin/pip install python-dateutil==2.9.0.post0 PyYAML==6.0.3
python3 -I content/sources/calendrier-1962/general.py --divinum $D/divinum-officium --missalemeum $D/Missalemeum
$D/venv/bin/python -I content/sources/calendrier-1962/oracle.py --missalemeum $D/Missalemeum \
    --divinum $D/divinum-officium --from 2020 --to 2045 --out content/golden/calendar/1962-missalemeum.jsonl
python3 -I content/sources/calendrier-1962/check.py --divinum $D/divinum-officium   # doit afficher « 0 erreur(s) »
python3 -I content/sources/calendrier-1962/general.py --divinum $D/divinum-officium --skeleton   # gabarit de sanctoral.tsv
```

| Fichier | Rôle |
|---|---|
| `fetch.py` | Clone DO (extraction partielle) et Missalemeum, avec le User-Agent du projet |
| `divinum.py` | Lecture de DO : Kalendarium composé, conditions « rubrica 196 » (port de `SetupString.pl`), rang → classe |
| `general.py` | Construit `general.json` ; avec `--missalemeum`, affiche les écarts avec la liste SANCTI de Missalemeum |
| `sanctoral.tsv` | Saisie : identifiant et titre français de chacune des 324 clés |
| `oracle.py` | Exécute `api.kalendar` de Missalemeum, le seul code tiers lancé, et normalise ses identifiants |
| `check.py` | Forme canonique, existence des clés dans DO, couverture de l'oracle |

Missalemeum demande Python ≥ 3.13, mais son module `kalendar` tourne avec 3.12, qui est la version de la VM. Le venv ne contient que `python-dateutil` et `PyYAML`.

## Sources figées

| Source | Licence | Commit | Usage |
|---|---|---|---|
| [Divinum Officium](https://github.com/DivinumOfficium/divinum-officium) | MIT | `b9c100d79ae481c67b2801fdd3622b00dd86bd73` (2026-10-11) | Kalendarium 1960, clés, titres latins, rangs, tables de transfert, rubriques, Calendarium de 1961 |
| [Missalemeum](https://github.com/mmolenda/Missalemeum) | MIT | `88c491d17d7e7edabaa43d2cf02463413336fb75` (2026-10-07) | Oracle ; contrôle des classes, couleurs et commémoraisons |
| [romcal](https://github.com/romcal/romcal), branche `dev` | MIT | `3cde69c21fcf3176b8728cf0a54d0ca8f3d97166` | Slugs du calendrier général de 1969 et du martyrologe (`catalog/martyrology.ts`), consultés à la main pour `sanctoral.tsv` |

## Construction de `general.json`

- **Jours** : Kalendarium `Tabulae/Kalendaria/1960.txt`, complété par ses bases (1955 → 1954 → 1939 → 1906 → 1888 → 1570) comme `Directorium::get_from_directorium`. Le premier fichier du jour est célébré, les suivants sont commémorés. `XXXXX` signifie que le jour est vide.
- **Classe** : `[Rank]` de `horas/Latin/Sancti/<clé>.txt`, conditions évaluées pour « Rubrics 1960 », puis la table `[Festa] (rubrica 196)` : 1 → commémoraison, 2 à 4 → III, 5 → II, 6 et 7 → I. On lit l'office et non la messe : c'est lui que lit la préséance de DO, et le `[Rank]` de la messe n'a pas toujours la variante de 1960. Sans elle, Notre-Dame du Mont-Carmel, Notre-Dame de la Merci et l'Octave de Noël sortiraient avec une fausse classe.
- **Cas imposés** :
  - les vigiles (n. 30-32) ;
  - saint Thomas Becket et saint Silvestre, commémorés dans l'octave de Noël (n. 68).
- **Couleurs** : par le commun de DO (`C1`, `C2`, `C3` rouge ; `C4`, `C5` blanc ; `C6` rouge, `C6a` blanc ; `C7` rouge, `C7a` blanc ; `C8` blanc ; `C9` noir ; `C10`, `C11` blanc). Douze exceptions sont nommées dans `COLOR_OVERRIDES` (n. 117-132), et les vigiles sont violettes.
- **`lord: true`** :
  - Présentation, Précieux Sang, Transfiguration, Exaltation de la Croix, Dédicace du Latran (n. 16 a) ;
  - les deux dernières suivent Missalemeum (`second_class_feast_no_sunday_commemoration`) et le choix de romcal pour 1969.
- **Laissés au temporal** (`TEMPORAL_KEYS`) : 24 et 25 décembre, 1er, 6, 7-12 et 13 janvier, Saint Nom. Les saints commémorés ces jours-là, Télesphore et Hygin, restent dans `general.json`.
- **Commémoraisons intrinsèques** (`INTRINSIC`) : elles figurent au Calendarium de 1961, mais DO les met dans le propre du jour.
  - Saint Pierre le 25 janvier et le 30 juin, saint Paul le 22 février (n. 110) ;
  - l'octave de Noël les 26, 27 et 28 décembre (n. 68) ;
  - les Litanies majeures le 25 avril, clé `Sancti/04-25c` (n. 80, 108 f).

  Sans clé de DO, `proper` est omis : les oraisons sont dans le propre principal. L'identifiant se répète d'une date à l'autre (`peter_apostle`, `commemoration_of_the_octave_of_christmas`), puisque c'est la même commémoraison.
- **Titres latins** : `[Officium]` de l'office. Treize coquilles évidentes sont corrigées dans `LA_FIXES`, par exemple « Urbanis » et « Archangelis ».
- **Vérification** : les 269 entrées et leurs 62 commémoraisons ont été relues contre le Calendarium du Bréviaire de 1961 (`horas/Help/Rubrics/1960calendarium.pdf` dans DO, OCR) : classes, présence et ordre des commémoraisons. Aucun écart.

## Oracle

Une ligne par jour : `{"date","proper","rank","colors","commemorations"}`. Missalemeum est appelé en latin (`MissalFactory().create(année, "la")`). La célébration principale est `celebration[0]`. Les commémoraisons sont `get_commemorations()`, normalisées et dédoublonnées.

**Correspondance des identifiants** (`oracle.normalize`) :
- **Règle générale** :
  - `sancti:10-11:2:w` → `Sancti/10-11` ;
  - `tempora:Pent19-0:2:g` → `Tempora/Pent19-0` ;
  - `commune:C10b:4:w` → `Commune/C10b`.

  On ne garde que le nom. La classe et la couleur deviennent `rank` (1 → `I`…) et `colors`, lettre par lettre : `w` WHITE, `r` RED, `g` GREEN, `v` VIOLET, `p` ROSE, `b` BLACK ; `rv` donne `["RED","VIOLET"]`.
- **Substitutions de DO pour 1960** (`Tabulae/Tempora/Generale.txt`) : `Tempora/Pasc5-1` → `Pasc5-1r`, `Tempora/Pasc6-6` → `Pasc6-6r`. Missalemeum applique déjà les autres (`Pent02-0r`, `Quad6-0r`, `Pasc2-3Feria`…).
- **Noms qui désignent un autre fichier de DO** (`RENAMES`) :

| Missalemeum | Clé | Pourquoi |
|---|---|---|
| `sancti:01-05` | `Sancti/01-05cc` | S. Télesphore |
| `sancti:05-31c` | `Sancti/05-31o` | Ste Pétronille |
| `sancti:07-18cc` | `Sancti/07-18o` | Ste Symphorose |
| `sancti:08-08` | `Sancti/08-09` | S. Jean-Marie Vianney (le Kalendarium de 1960 met le fichier 08-09 au 8 août) |
| `sancti:08-08c` | `Sancti/08-08` | Ss. Cyriaque et compagnons |
| `sancti:10-08c` | `Sancti/10-07cc` | Ss. Serge et compagnons |
| `sancti:11-02m1`, `m2`, `m3` | `Sancti/11-02` | Défunts : le jour, qui renvoie aux trois messes |
| `sancti:12-25m1`, `m2`, `m3` | `Sancti/12-25` | Noël, idem |
| `sancti:12-28`, `12-29`, `12-31` | `Sancti/12-28r`, `12-29r`, `12-31r` | Kalendarium de 1960 |
| `commune:C10t` | `Commune/C10` | Sainte Vierge au samedi après la Trinité |

- **Normalisations selon la date** :
  - `tempora:Nat1-1` → `Tempora/Nat29`, `Nat30` ou `Nat31` ;
  - `tempora:Epi3-0`… `Epi6-6` en octobre et novembre → `Tempora/PentEpi3-0`…, les dimanches repris (n. 18, `getweek` de DO) ;
  - `:feria:` sans temporal → `Tempora/Nat02`… `Nat05` (2-5 janvier) ou `Sancti/01-07`… `01-12` (7-12 janvier).
- **Clés propres à Missalemeum, gardées telles quelles** : ce sont les commémoraisons intrinsèques.

| Missalemeum | Notre modèle |
|---|---|
| `Sancti/01-25c`, `Sancti/02-22c` | `peter_apostle`, `paul_apostle` sans clé |
| `Sancti/12-26c`, `12-27c`, `12-28c` | `commemoration_of_the_octave_of_christmas` sans clé |
| `Tempora/Quad5-5Feriac` | commémoraison des Sept Douleurs le vendredi de la Passion (TEMPORAL.md § 6) |

## Écarts entre Missalemeum et les sources de référence

DO et le Calendarium de 1961 concordent sur tous les points ci-dessous ; Missalemeum s'en écarte. Ces écarts restent dans l'oracle.
- **Classes** : saint Ubald (05-16) et saint Didace (11-13) sont des commémoraisons chez Missalemeum, de IIIᵉ classe au Calendarium et dans DO.
- **Commémoraisons** : Missalemeum n'a pas sainte Barbe (12-04) ni saint Pierre le 30 juin (n. 110).
- **Couleurs des fêtes** : 13 fêtes, 292 jours de l'oracle. Missalemeum prend souvent la couleur du saint commémoré.
  - Rouge au lieu de blanc :
    - 06-12 S. Jean de Saint-Facond ;
    - 06-18 S. Éphrem ;
    - 06-19 Ste Julienne Falconieri ;
    - 07-12 S. Jean Gualbert ;
    - 07-18 S. Camille ;
    - 07-20 S. Jérôme Émilien ;
    - 07-29 Ste Marthe ;
    - 08-02 S. Alphonse ;
    - 08-28 S. Augustin ;
    - 08-30 Ste Rose de Lima.
  - Blanc au lieu de rouge : 07-23 S. Apollinaire, 11-14 S. Josaphat.
  - 08-09 : vigile de saint Laurent rouge au lieu de violet (n. 128 d).
  - Sur des commémoraisons, sans effet sur l'oracle : 01-18, 06-15, 08-18.
- **Couleurs du temporal** :
  - féries `Epi1-*` blanches après le 13 janvier (n. 127 a : vert) ;
  - Rameaux `RED, VIOLET` (violet pour la messe).
- **Règles** (TEMPORAL.md § 12) :
  - saint Laurent : vigile remplacée par la Sainte Vierge le samedi ;
  - pas de commémoraison du dimanche pour la plupart des fêtes de Iʳᵉ classe ;
  - au plus une commémoraison par jour.

## Titres français incertains

Les choix suivants sont à relire contre l'Ordo de référence :
- les noms rares :
  - « Saint Gabriel de la Vierge des Douleurs » (ou « de l'Addolorata ») ;
  - « Saint Jean de Saint-Facond » (Jean de Sahagun) ;
  - « Saints Processe et Martinien » ;
  - « Saints Tryphon, Respice et Nymphe » ;
  - « Saint Mennas » ;
  - « Saints Marius, Marthe, Audifax et Abachum » ;
  - « Saints Alexandre Ier, Événce et Théodule » ;
  - « Saint Luce Ier » ;
- « Dédicace de Sainte-Marie-aux-Neiges » ;
- « Notre-Dame du Rosaire » (Calendarium : « B. Mariæ Virg. a Rosario ») ;
- « La bienheureuse Vierge Marie, Reine » ;
- « Saint Joseph artisan, époux de la bienheureuse Vierge Marie, confesseur » ;
- « Purification de la bienheureuse Vierge Marie », « Annonciation de la bienheureuse Vierge Marie » : les missels français écrivent aussi « de la Sainte Vierge » ;
- « Saint Pierre de Vérone, martyr » (latin « S. Petri Martyris ») ;
- « Saint Saturnin, martyr », dont l'identité varie selon les sources (Rome ou Toulouse) ;
- « Litanies majeures ».
