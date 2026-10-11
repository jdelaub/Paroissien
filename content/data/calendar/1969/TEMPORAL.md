# Temporal de 1969 : identifiants et règles de romcal (France)

Le temporal n'est pas dans les fichiers de données : `core:liturgy` le calcule. Ce tableau donne les identifiants de romcal et la façon dont romcal les place, **configuré pour la France** : Épiphanie et Saint-Sacrement au dimanche, Ascension le jeudi.

- **Source** : romcal 3.0.0-dev.140, commit `3cde69c21fcf3176b8728cf0a54d0ca8f3d97166` (branche `dev`, MIT) : `rites/roman1969/src/proper-of-time/proper-of-time.ts` et `src/utils/dates.ts`.
- **Vérification** : `content/sources/calendrier-1969/temporal.py` reprend ces règles en Python et les compare à la sortie de romcal pour la France, du 1er janvier 2025 au 31 décembre 2045 (7 670 jours). On n'a relevé aucun écart, ni sur l'identifiant du temporal, ni sur les cycles.

**Notations**
- E : dimanche de Pâques.
- P : Pentecôte, E + 49.
- A1 : 1er dimanche de l'Avent, c'est-à-dire le dimanche du 27 novembre au 3 décembre.
- `{jour}` : `sunday`, `monday`… `saturday`, en anglais et en minuscules.
- `{w}` : numéro de la semaine.
- **Degré** : rang dans la table de préséance des NU, d'après le code `precedence` de romcal (1 = Triduum… 13 = férie).
- Couleurs : celles du schéma. romcal écrit `PURPLE` pour `VIOLET`.
- Titres : locale `fr` de romcal, tels quels.

## Avent

| Identifiant | Règle | Rang | Degré | Couleurs | Titre fr (romcal) |
|---|---|---|---|---|---|
| `advent_{w}_sunday` (w = 1…4) | A1 + 7(w − 1) | SUNDAY | 2 | VIOLET ; w = 3 : ROSE, VIOLET | « premier dimanche de l’Avent » |
| `advent_{w}_{jour}` (w = 1…3, du lundi au samedi) | A1 + 7(w − 1) + n, **jusqu'au 16 décembre** seulement | WEEKDAY | 13 | VIOLET | « lundi de la première semaine de l’Avent » |
| `advent_december_{n}` (n = 17…24) | n décembre, sauf un dimanche | WEEKDAY | 9 | VIOLET | « 17 décembre » |

## Temps de Noël

| Identifiant | Règle | Rang | Degré | Couleurs | Titre fr (romcal) |
|---|---|---|---|---|---|
| `nativity_of_the_lord` | 25 décembre | SOLEMNITY | 2 | WHITE | « Nativité du Seigneur » |
| `holy_family_of_jesus_mary_and_joseph` | dimanche du 26 au 31 décembre ; **30 décembre** si Noël est un dimanche | FEAST | 5 | WHITE | « La Sainte Famille » |
| `christmas_octave_day_{n}` (n = 2…7) | (24 + n) décembre, sauf le jour de la Sainte Famille | WEEKDAY | 9 | WHITE | « 2ᵉ jour dans l’Octave de la Nativité » |
| `mary_mother_of_god` | 1er janvier | SOLEMNITY | 3 | WHITE | « Sainte Marie, Mère de Dieu » |
| `christmas_time_january_{n}` (n = 2…8) | du 2 janvier à la veille de l'Épiphanie (en France, n ≤ 7) | WEEKDAY | 13 | WHITE | « 2 janvier » |
| `epiphany_of_the_lord` | **dimanche du 2 au 8 janvier** | SOLEMNITY | 2 | WHITE | « Épiphanie du Seigneur » |
| `{jour}_after_epiphany` (du lundi au samedi) | du lendemain de l'Épiphanie à la veille du Baptême | WEEKDAY | 13 | WHITE | « lundi après l’Épiphanie » |
| `baptism_of_the_lord` | dimanche qui suit l'Épiphanie ; **lundi** si l'Épiphanie tombe le 7 ou le 8 janvier | FEAST | 5 | WHITE | « Baptême du Seigneur » |
| `second_sunday_after_christmas` | jamais en France : il n'existe que si l'Épiphanie est fixée au 6 janvier | SUNDAY | 6 | WHITE | « Deuxième dimanche après la Nativité » |

## Temps ordinaire

| Identifiant | Règle | Rang | Degré | Couleurs | Titre fr (romcal) |
|---|---|---|---|---|---|
| `ordinary_time_{w}_{jour}` | **Avant le Carême**, du lendemain du Baptême à la veille des Cendres : w = ⌊(d − S)/7⌋ + 1, où S est le dimanche du Baptême, ou le dimanche de l'Épiphanie quand le Baptême tombe un lundi. **Après la Pentecôte**, de P + 1 à la veille de A1 : w = 34 − (CR − D)/7, où CR = A1 − 7 et D est le dimanche qui précède d ou d lui-même. | WEEKDAY ; SUNDAY le dimanche | 13 ; 6 le dimanche | GREEN | « lundi de la première semaine du Temps Ordinaire », « deuxième dimanche du Temps Ordinaire » |
| `sunday_of_the_word_of_god` | 3e dimanche du temps ordinaire ; remplace `ordinary_time_3_sunday`, qui n'existe pas | SUNDAY | 6 | GREEN | « Troisième dimanche du Temps Ordinaire, ou Dimanche de la Parole de Dieu » |
| `most_holy_trinity` | P + 7 | SOLEMNITY | 3 | WHITE | « La Sainte Trinité » |
| `most_holy_body_and_blood_of_christ` | **P + 14**, le dimanche | SOLEMNITY | 3 | WHITE | « Le Saint-Sacrement du Corps et du Sang du Christ » |
| `most_sacred_heart_of_jesus` | P + 19, un vendredi | SOLEMNITY | 3 | WHITE | « Sacré-Cœur de Jésus » |
| `our_lord_jesus_christ_king_of_the_universe` | A1 − 7, le 34e dimanche ; remplace `ordinary_time_34_sunday`, qui n'existe pas | SOLEMNITY | 3 | WHITE | « Trente-quatrième et dernier dimanche - Le Christ, Roi de l’Univers » |

Quelques identifiants n'existent jamais :
- `ordinary_time_1_sunday`, car c'est le Baptême ;
- `ordinary_time_3_sunday` et `ordinary_time_34_sunday` (voir plus haut).

Les jours de la Trinité, du Saint-Sacrement et du Sacré-Cœur, l'identifiant `ordinary_time_…` de ce jour-là n'existe pas.

## Carême et Semaine sainte

| Identifiant | Règle | Rang | Degré | Couleurs | Titre fr (romcal) |
|---|---|---|---|---|---|
| `ash_wednesday` | E − 46 | WEEKDAY | 2 | VIOLET | « Mercredi des Cendres » |
| `{jour}_after_ash_wednesday` (jeudi, vendredi, samedi) | E − 45 à E − 43 | WEEKDAY | 9 | VIOLET | « jeudi après les Cendres » |
| `lent_{w}_sunday` (w = 1…5) | E − 42 + 7(w − 1) | SUNDAY | 2 | VIOLET ; w = 4 : ROSE, VIOLET | « premier dimanche de Carême » |
| `lent_{w}_{jour}` (w = 1…5, du lundi au samedi) | E − 42 + 7(w − 1) + n | WEEKDAY | 9 | VIOLET | « lundi de la première semaine de Carême » |
| `palm_sunday_of_the_passion_of_the_lord` | E − 7 | SUNDAY | 2 | RED | « Dimanche des Rameaux et de la Passion » |
| `holy_monday`, `holy_tuesday`, `holy_wednesday`, `holy_thursday` | E − 6 à E − 3 | WEEKDAY | 9 chez romcal (2 selon les NU) | VIOLET | « lundi Saint » |

## Triduum pascal

| Identifiant | Règle | Rang | Degré | Couleurs | Titre fr (romcal) |
|---|---|---|---|---|---|
| `thursday_of_the_lords_supper` | E − 3 ; romcal le sort **en second**, après `holy_thursday`, et tous deux sont non facultatifs | WEEKDAY | 1 | WHITE | « Mémoire de la Cène du Seigneur » |
| `friday_of_the_passion_of_the_lord` | E − 2 | WEEKDAY | 1 | RED | « Vendredi Saint » |
| `holy_saturday` | E − 1 | WEEKDAY | 1 | **aucune** (`colors: []`) | « Samedi Saint » |
| `easter_sunday` | E | SOLEMNITY | 1 | WHITE | « Dimanche de Pâques - La résurrection du Seigneur » |

## Temps pascal

| Identifiant | Règle | Rang | Degré | Couleurs | Titre fr (romcal) |
|---|---|---|---|---|---|
| `easter_{jour}` (du lundi au samedi) | E + 1 à E + 6 | **SOLEMNITY** | 2 | WHITE | « lundi dans l’Octave de Pâques » |
| `divine_mercy_sunday` | E + 7 | SUNDAY | 2 | WHITE | « Deuxième dimanche de Pâques ou de la Divine Miséricorde » |
| `easter_time_{w}_{jour}` (w = 2…7) | E + 7(w − 1) + n, de E + 8 à E + 48, sauf `easter_time_6_thursday`, qui est toujours l'Ascension en France | WEEKDAY ; SUNDAY le dimanche | 13 ; 2 le dimanche | WHITE | « lundi de la deuxième semaine de Pâques », « troisième dimanche de Pâques » |
| `ascension_of_the_lord` | **E + 39**, le jeudi | SOLEMNITY | 2 | WHITE | « Ascension du Seigneur » |
| `pentecost_sunday` | P = E + 49 | SOLEMNITY | 2 | RED | « Pentecôte » |

## Mémoires mobiles du calendrier général

Chez romcal, ces deux mémoires appartiennent au calendrier `general_roman`, mais avec `properCycle: PROPER_OF_TIME`. Elles sont absentes de `general.json` (voir `../SOURCES.md`, § « Fêtes mobiles laissées au code »).

| Identifiant | Règle | Rang | Degré | Couleurs | Titre fr (romcal) |
|---|---|---|---|---|---|
| `mary_mother_of_the_church` | P + 1, le lundi ; inscrite en 2018, mais romcal la donne pour toutes les années | MEMORIAL | 10 | WHITE | « Bienheureuse Vierge Marie, Mère de l’Église » |
| `immaculate_heart_of_mary` | E + 69 = P + 20, le samedi qui suit le Sacré-Cœur. Avec `allowSimilarRankItems`, une mémoire obligatoire du même jour est **aussi** sortie, en second : saint Irénée, saint Antoine de Padoue, saint Boniface ou saint Louis de Gonzague selon les années. | MEMORIAL | 10 | WHITE | « Cœur immaculé de Marie » |

## Cycles

L'année liturgique L est l'année civile, plus 1 à partir de A1. Une célébration du sanctoral prend les cycles du jour du temporal sur lequel elle tombe.

- **`sundayCycle`** : A si L mod 3 = 1, B si L mod 3 = 2, C si L mod 3 = 0.
- **`weekdayCycle`** : I si L est impair, II si L est pair.
- **`psalterWeek`** : ((w − 1) mod 4) + 1. Pour l'Avent, le Carême, le temps pascal et le temps ordinaire, w est le numéro de semaine des tableaux. Pour le temps pascal, w = ⌊(d − E)/7⌋ + 1, et donc 4 à la Pentecôte. Il y a quatre exceptions :
  - du mercredi des Cendres au samedi suivant : semaine **4** ;
  - Semaine sainte, des Rameaux à `holy_thursday` : w = 6, soit la semaine **2**. Le Vendredi saint, le Samedi saint et le dimanche de Pâques sont en semaine **1**. `thursday_of_the_lords_supper` serait en semaine 1, mais l'oracle garde `holy_thursday`, en semaine 2 ;
  - temps de Noël, du 25 décembre au Baptême inclus. On pose n = rang du jour depuis le 25 décembre (1 le 25), s = jour de la semaine de ce 25 décembre (0 = dimanche) et w = ⌈(n + s)/7⌉. Si s ≠ 0, la semaine est ((w + 2) mod 4) + 1, car le psautier continue celui de l'Avent (PGLH 133). Si s = 0, elle vaut ((w − 1) mod 4) + 1 ;
  - temps ordinaire après la Pentecôte : w est compté à rebours depuis le Christ-Roi (voir plus haut). Le Christ-Roi est donc toujours en semaine 2.

## Transferts du sanctoral codés en dur dans romcal

Les fêtes suivantes sont à date fixe dans `general.json`, mais romcal les déplace lui-même, par une fonction de date ou par des `dateExceptions`. Un moteur qui veut retrouver l'oracle doit produire le même résultat :

- **Annonciation** (`annunciation_of_the_lord`, 25 mars) : si elle tombe entre le dimanche des Rameaux et le 2e dimanche de Pâques inclus, elle passe au lundi suivant ce dimanche, E + 8. C'est le cas en 2027, 2029, 2032, 2035, 2040 et 2043. Un dimanche ordinaire de Carême, c'est la règle générale de romcal qui s'applique (aucun cas de 2025 à 2045).
- **Saint Joseph** (`joseph_spouse_of_mary`, 19 mars) : un dimanche, il passe au lundi (2028, 2034, 2045). Entre les Rameaux et le 2e dimanche de Pâques, il devrait être avancé au **samedi qui précède les Rameaux**. Mais romcal n'applique pas cette règle et **omet saint Joseph en 2035**. C'est un défaut de l'oracle (voir `../SOURCES.md`).
- **Immaculée Conception** (`immaculate_conception_of_the_blessed_virgin_mary`, 8 décembre) : un dimanche, elle passe au lundi 9 décembre (2030, 2041).

Les autres conflits entre le sanctoral et les fêtes mobiles sont réglés par la préséance générale de romcal, avec des défauts documentés dans `../SOURCES.md` :
- la Nativité de saint Jean-Baptiste est supprimée au lieu d'être transférée quand le Sacré-Cœur tombe le 24 juin ;
- le Cœur immaculé de Marie évince une fête ou une solennité.
