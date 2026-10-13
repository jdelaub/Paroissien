# Temporal de 1962 : clés de propre et règles

Ce document sert à écrire `TemporalBuilder1962` et `OccurrenceResolver1962` dans `core:liturgy`. Le sanctoral à date fixe est dans `general.json` ; ici figurent les jours que le code calcule.

Sources :
- Divinum Officium (DO, MIT), commit `b9c100d79ae481c67b2801fdd3622b00dd86bd73` : clés, titres latins, tables de transfert ;
- Missalemeum (MIT), commit `88c491d17d7e7edabaa43d2cf02463413336fb75` : classes, couleurs et règles de `backend/api/kalendar/` ;
- *Rubricae generales* de 1960, citées « n. » d'après la traduction anglaise hébergée par DO (`horas/Help/Rubrics/General Rubrics.html`) ;
- le Calendarium du Bréviaire de 1961 (`horas/Help/Rubrics/1960calendarium.pdf` dans DO).

## Conventions

- **Clé** : propre de messe de DO pour la version « Rubrics 1960 », soit le fichier `web/www/missa/Latin/<clé>.txt`. Les communs (`Commune/…`) sont lus dans `horas/Latin/`, comme le fait DO. Les substitutions de `Tabulae/Tempora/Generale.txt` pour 1960 sont déjà appliquées : `Tempora/Pent02-0` devient `Tempora/Pent02-0r`, `Tempora/Pasc2-3` devient `Tempora/Pasc2-3Feria`, etc.
- **Notation** : `{04..23}` énumère des nombres en gardant la largeur du premier, `{a,b}` des variantes. Le suffixe `-D` d'une clé est le jour de la semaine : 0 = dimanche, 1 = lundi… 6 = samedi.
- **Classe** : `I` à `IV`, d'après les n. 10-12 (dimanches), 23-26 (féries), 29-32 (vigiles) et 66-67 (octaves).
- **Couleurs** : n. 117-132. La première couleur est la principale.
- **Titres** : le latin est celui de DO, retouché quand il était abrégé ; le français suit l'usage des missels.
- Une ligne par jour quand le jour est particulier ; une ligne par motif pour les semaines ordinaires.

## 1. Repères de date

| Repère | Règle | Source |
|---|---|---|
| Pâques | Comput grégorien. Missalemeum utilise `dateutil.easter`, DO l'algorithme de `Date::Easter` (`DivinumOfficium/Date.pm`). | — |
| Septuagésime | Pâques − 63 jours | — |
| Cendres | Pâques − 46 jours | — |
| Ascension | Pâques + 39 jours | — |
| Pentecôte | Pâques + 49 jours | — |
| Trinité | Pâques + 56 jours (1er dimanche après la Pentecôte) | n. 17 c |
| Fête-Dieu | Pâques + 60 jours (jeudi) | — |
| Sacré-Cœur | Pâques + 68 jours (vendredi) | — |
| 1er dimanche de l'Avent | Dimanche du 30 novembre ou le plus proche, soit du 27 novembre au 3 décembre | n. 20 |
| 24e et dernier dimanche après la Pentecôte | 1er dimanche de l'Avent − 7 jours ; toujours en dernier | n. 18 |
| Saint Nom de Jésus | Dimanche du 2 au 5 janvier, sinon le 2 janvier | n. 17 a |
| Sainte Famille | 1er dimanche après l'Épiphanie (7-13 janvier) | n. 17 b |
| Christ-Roi | Dernier dimanche d'octobre (25-31) | n. 17 d |
| Dimanche dans l'octave de Noël | Dimanche du 26 au 31 décembre | n. 69 |
| Quatre-Temps | Mercredi, vendredi et samedi : après le 3e dimanche de l'Avent ; après le 1er dimanche de Carême ; dans l'octave de la Pentecôte ; après le 3e dimanche de septembre (dimanche du 15 au 21) | *Rubricarum instructum*, 1960 ; Missalemeum `calc_ember_wednesday_september` |
| Temps de Noël | 25 décembre au 13 janvier ; « per annum » à partir du 14 janvier | n. 72, 77 |

## 2. Avent

| Clé | Jour | Classe | Couleurs | Titre fr | Titre la |
|---|---|---|---|---|---|
| `Tempora/Adv{1..4}-0` | Dimanches de l'Avent | I | VIOLET | 1er… 4e dimanche de l'Avent | Dominica I… IV Adventus |
| `Tempora/Adv3-0` | 3e dimanche | I | ROSE, VIOLET | 3e dimanche de l'Avent (Gaudete) | Dominica III Adventus |
| `Tempora/Adv{1..3}-{1..6}`, `Tempora/Adv4-{1..5}` | Féries jusqu'au 23 décembre | III jusqu'au 16 décembre, II du 17 au 23 | VIOLET | Lundi… samedi de la Nᵉ semaine de l'Avent | Feria II… Sabbato infra Hebdomadam N Adventus |
| `Tempora/Adv3-{3,5,6}` | Quatre-Temps de l'Avent | II | VIOLET | Mercredi, vendredi, samedi des Quatre-Temps de l'Avent | Feria IV, Feria VI, Sabbato Quattuor Temporum Adventus |

Notes :
- Le 24 décembre est la vigile de Noël (§ 3), jamais une férie de l'Avent. La 4e semaine s'arrête au 23 ; DO n'a pas de clé `Adv4-6`.
- L'Immaculée Conception l'emporte sur le dimanche de l'Avent et le commémore (n. 15).
- La classe II des féries du 17 au 23 décembre est la `TEMPORA_RANK_MAP` de Missalemeum (n. 24 a).

## 3. Temps de Noël (24 décembre – 13 janvier)

| Clé | Jour | Classe | Couleurs | Titre fr | Titre la |
|---|---|---|---|---|---|
| `Sancti/12-24` | 24 décembre | I (vigile de Iʳᵉ classe) | VIOLET | Vigile de Noël | In Vigilia Nativitatis Domini |
| `Sancti/12-25` | 25 décembre ; trois messes `Sancti/12-25m{1..3}` | I | WHITE | Nativité de Notre-Seigneur Jésus-Christ (messes de la nuit, de l'aurore, du jour) | In Nativitate Domini |
| `Tempora/Nat1-0` | Dimanche du 26 au 31 décembre | II | WHITE | Dimanche dans l'octave de Noël | Dominica infra Octavam Nativitatis |
| `Tempora/Nat{29..31}` | 29-31 décembre (sauf dimanche) | II | WHITE | 5e, 6e, 7e jour dans l'octave de Noël | Die V, VI, VII infra Octavam Nativitatis |
| `Sancti/01-01` | 1er janvier | I | WHITE | Octave de la Nativité de Notre-Seigneur | In Octava Nativitatis Domini |
| `Tempora/Nat2-0` | Dimanche du 2 au 5 janvier, sinon le 2 | II | WHITE | Très Saint Nom de Jésus | Sanctissimi Nominis Jesu |
| `Tempora/Nat{02..05}` | 2-5 janvier (féries) | IV | WHITE | Férie du temps de Noël (2… 5 janvier) | Die Secunda… Quinta Januarii |
| `Sancti/01-06` | 6 janvier | I | WHITE | Épiphanie de Notre-Seigneur | In Epiphania Domini |
| `Sancti/01-{07..12}` | 7-12 janvier, jusqu'à la Sainte Famille | IV | WHITE | Férie du temps de l'Épiphanie (7… 12 janvier) | Die Septima… Duodecima Januarii |
| `Tempora/Epi1-0` | 1er dimanche après l'Épiphanie | II, fête du Seigneur | WHITE | La Sainte Famille, Jésus, Marie, Joseph | Sanctæ Familiæ Jesu Mariæ Joseph |
| `Tempora/Epi1-{1..6}` | Féries de la semaine de la Sainte Famille | IV | WHITE jusqu'au 13 janvier, GREEN ensuite | Lundi… samedi de la 1re semaine après l'Épiphanie | Feria II… Sabbato infra Hebdomadam I post Epiphaniam |
| `Sancti/01-13` | 13 janvier | II, fête du Seigneur | WHITE | Commémoraison du Baptême de Notre-Seigneur Jésus-Christ | In Commemoratione Baptismatis Domini Nostri Jesu Christi |

Notes :
- **Vigile de Noël un dimanche** : elle remplace le 4e dimanche de l'Avent, sans commémoraison (n. 30 a ; Missalemeum `rule_nativity_vigil`). DO emploie alors `Sancti/12-24s`.
- **Noël** : la clé du jour est `Sancti/12-25`. Ce fichier de DO renvoie aux trois messes (« celebranda aut missa prima aut missa secunda aut missa tertia ») ; la normalisation de l'oracle ramène `sancti:12-25m1…m3` à cette clé.
- **Octave de Noël** (n. 67-69) :
  - les 26, 27 et 28 décembre sont les fêtes de IIᵉ classe de `general.json`, avec la commémoraison de l'octave (`commemoration_of_the_octave_of_christmas`) ;
  - les 29 et 31, le jour est dans l'octave et commémore saint Thomas Becket ou saint Silvestre (`general.json`) ;
  - le dimanche du 26 au 31 l'emporte sur la fête du jour, qui est commémorée, sauf une fête de Iʳᵉ classe (n. 69). DO emploie `Tempora/Nat1-0a` si ce dimanche tombe le 29 ; c'est la même messe.
- **Couleur des féries `Epi1-*`** : Missalemeum les met toujours en blanc. D'après les n. 72, 77 et 127 a, elles sont vertes à partir du 14 janvier (écart connu).
- **13 janvier un dimanche** : la Sainte Famille l'emporte et le Baptême est omis (n. 95, al. 2 ; Missalemeum `rule_lord_feast1`).
- **Samedis** : le 2-5 et le 7-12 janvier, un samedi de férie de IVᵉ classe devient le samedi de la Sainte Vierge (`Commune/C10b`, § 10).

## 4. Temps après l'Épiphanie

| Clé | Jour | Classe | Couleurs | Titre fr | Titre la |
|---|---|---|---|---|---|
| `Tempora/Epi{2..6}-0` | Dimanches jusqu'à la Septuagésime | II | GREEN | 2e… 6e dimanche après l'Épiphanie | Dominica II… VI post Epiphaniam |
| `Tempora/Epi{2..6}-{1..6}` | Féries | IV | GREEN | Lundi… samedi de la Nᵉ semaine après l'Épiphanie | Feria II… Sabbato infra Hebdomadam N post Epiphaniam |

Les semaines empêchées par la Septuagésime sont reprises en novembre (§ 9).

## 5. Septuagésime

| Clé | Jour | Classe | Couleurs | Titre fr | Titre la |
|---|---|---|---|---|---|
| `Tempora/Quadp1-0` | Pâques − 63 | II | VIOLET | Dimanche de la Septuagésime | Dominica in Septuagesima |
| `Tempora/Quadp2-0` | Pâques − 56 | II | VIOLET | Dimanche de la Sexagésime | Dominica in Sexagesima |
| `Tempora/Quadp3-0` | Pâques − 49 | II | VIOLET | Dimanche de la Quinquagésime | Dominica in Quinquagesima |
| `Tempora/Quadp{1,2}-{1..6}`, `Tempora/Quadp3-{1,2}` | Féries jusqu'au mardi de la Quinquagésime | IV | VIOLET | Lundi… samedi après la Septuagésime, la Sexagésime ; lundi et mardi après la Quinquagésime | Feria II… infra Hebdomadam Septuagesimæ, Sexagesimæ, Quinquagesimæ |

## 6. Carême et temps de la Passion

| Clé | Jour | Classe | Couleurs | Titre fr | Titre la |
|---|---|---|---|---|---|
| `Tempora/Quadp3-3` | Pâques − 46 | I | VIOLET | Mercredi des Cendres | Feria IV Cinerum |
| `Tempora/Quadp3-{4..6}` | Jeudi, vendredi, samedi après les Cendres | III | VIOLET | Jeudi, vendredi, samedi après les Cendres | Feria V, Feria VI, Sabbato post Cineres |
| `Tempora/Quad{1..3}-0` | 1er au 3e dimanche de Carême | I | VIOLET | 1er… 3e dimanche de Carême | Dominica I… III in Quadragesima |
| `Tempora/Quad4-0` | 4e dimanche de Carême | I | ROSE, VIOLET | 4e dimanche de Carême (Lætare) | Dominica IV in Quadragesima |
| `Tempora/Quad{1..4}-{1..6}` | Féries de Carême | III | VIOLET | Lundi… samedi de la Nᵉ semaine de Carême | Feria II… Sabbato infra Hebdomadam N in Quadragesima |
| `Tempora/Quad1-{3,5,6}` | Quatre-Temps de Carême | II | VIOLET | Mercredi, vendredi, samedi des Quatre-Temps de Carême | Feria IV, Feria VI, Sabbato Quattuor Temporum Quadragesimæ |
| `Tempora/Quad5-0` | 1er dimanche de la Passion | I | VIOLET | 1er dimanche de la Passion | Dominica I Passionis |
| `Tempora/Quad5-{1..4,6}`, `Tempora/Quad5-5Feria` | Féries de la semaine de la Passion | III | VIOLET | Lundi… samedi de la semaine de la Passion | Feria II… Sabbato infra Hebdomadam Passionis |
| `Tempora/Quad6-0r` | Dimanche des Rameaux | I | VIOLET (messe), RED (bénédiction et procession) | 2e dimanche de la Passion ou des Rameaux | Dominica II Passionis seu in Palmis |
| `Tempora/Quad6-{1..3}` | Lundi, mardi, mercredi saints | I | VIOLET | Lundi saint, mardi saint, mercredi saint | Feria II, III, IV Hebdomadæ Sanctæ |
| `Tempora/Quad6-4r` | Jeudi saint | I | WHITE | Jeudi saint, messe de la Cène du Seigneur | Feria V in Cena Domini |
| `Tempora/Quad6-5r` | Vendredi saint | I | BLACK, VIOLET (communion) | Vendredi saint, Passion et mort du Seigneur | Feria VI in Passione et Morte Domini |
| `Tempora/Quad6-6r` | Samedi saint et Vigile pascale | I | VIOLET, WHITE (messe de la Vigile) | Samedi saint | Sabbato Sancto |

Notes :
- **Quad5-5Feria** : la clé de 1960 remplace l'ancienne fête des Sept Douleurs (`Tempora/Quad5-5`). Le Calendarium prescrit une commémoraison des Sept Douleurs ; DO la met dans le propre (sections « Commemoratio »). Missalemeum la modélise à part, sous la clé `tempora:Quad5-5Feriac` (`rule_seven_sorrows_on_friday_after_passion_sunday`).
- **Couleurs des Rameaux** : Missalemeum donne `rv`, soit RED puis VIOLET ; l'oracle garde cet ordre. Le n. 128 b fixe le violet pour la messe et le rouge pour la seule procession (n. 126) : écart d'ordre connu.
- **Rang** : les féries de Carême (III) l'emportent sur les fêtes de IIIᵉ classe, qui sont commémorées (n. 25 a). Le Mercredi des Cendres et la Semaine sainte excluent toute fête et toute commémoraison non privilégiée (n. 23).

## 7. Temps pascal

| Clé | Jour | Classe | Couleurs | Titre fr | Titre la |
|---|---|---|---|---|---|
| `Tempora/Pasc0-0` | Pâques | I | WHITE | Dimanche de la Résurrection | Dominica Resurrectionis |
| `Tempora/Pasc0-{1..6}` | Octave de Pâques | I | WHITE | Lundi… samedi de Pâques (samedi *in albis*) | Die II… VI infra octavam Paschæ, Sabbato in Albis |
| `Tempora/Pasc1-0` | Pâques + 7 | I | WHITE | Dimanche *in albis*, octave de Pâques | Dominica in Albis in Octava Paschæ |
| `Tempora/Pasc{2,4,5}-0`, `Tempora/Pasc3-0r` | 2e au 5e dimanche après Pâques | II | WHITE | 2e… 5e dimanche après Pâques | Dominica II… V post Pascha |
| `Tempora/Pasc1-{1..6}`, `Tempora/Pasc2-{1,2}`, `Tempora/Pasc2-{3..6}Feria`, `Tempora/Pasc3-{1..3}Feria`, `Tempora/Pasc3-{4..6}`, `Tempora/Pasc4-{1..6}` | Féries | IV | WHITE | Lundi… samedi de la Nᵉ semaine après l'octave de Pâques | Feria II… Sabbato infra Hebdomadam N post Octavam Paschæ |
| `Tempora/Pasc5-1r` | Lundi des Rogations | IV | VIOLET (messe des Rogations) | Lundi des Rogations | Feria II in Rogationibus |
| `Tempora/Pasc5-2` | Mardi des Rogations | IV | WHITE | Mardi des Rogations | Feria III in Rogationibus |
| `Tempora/Pasc5-3` | Mercredi des Rogations | II (vigile de IIᵉ classe) | WHITE | Vigile de l'Ascension | In Vigilia Ascensionis |
| `Tempora/Pasc5-4` | Pâques + 39 | I | WHITE | Ascension de Notre-Seigneur | In Ascensione Domini |
| `Tempora/Pasc5-{5,6}`, `Tempora/Pasc6-{1..3}`, `Tempora/Pasc6-4r`, `Tempora/Pasc6-5` | Féries après l'Ascension | IV | WHITE | Vendredi, samedi après l'Ascension ; lundi… vendredi de la semaine après l'Ascension | Feria VI post Ascensionem… |
| `Tempora/Pasc6-0` | Dimanche après l'Ascension | II | WHITE | Dimanche après l'Ascension | Dominica post Ascensionem |
| `Tempora/Pasc6-6r` | Samedi avant la Pentecôte | I (vigile de Iʳᵉ classe) | RED | Vigile de la Pentecôte | Sabbato in Vigilia Pentecostes |

Notes :
- **Rogations** (n. 87-90) : messe des Rogations, violette (n. 130 a), après la procession des lundi, mardi et mercredi. Missalemeum ne colore en violet que le lundi.
- **Litanies majeures** (n. 80) : le 25 avril, commémoraison privilégiée (n. 108 f, `greater_litanies` dans `general.json`, clé `Sancti/04-25c`). Si Pâques ou le lundi de Pâques tombe ce jour-là, elles sont transférées au mardi suivant ; c'est au code de le faire.

## 8. Octave de la Pentecôte

| Clé | Jour | Classe | Couleurs | Titre fr | Titre la |
|---|---|---|---|---|---|
| `Tempora/Pasc7-0` | Pâques + 49 | I | RED | Dimanche de la Pentecôte | Dominica Pentecostes |
| `Tempora/Pasc7-{1,2,4}` | Lundi, mardi, jeudi | I | RED | Lundi, mardi, jeudi de Pentecôte | Die II, III, V infra octavam Pentecostes |
| `Tempora/Pasc7-{3,5,6}` | Quatre-Temps de Pentecôte | I | RED | Mercredi, vendredi, samedi des Quatre-Temps de Pentecôte | Feria IV, Feria VI, Sabbato Quattuor Temporum Pentecostes |

## 9. Temps après la Pentecôte

| Clé | Jour | Classe | Couleurs | Titre fr | Titre la |
|---|---|---|---|---|---|
| `Tempora/Pent01-0r` | Pâques + 56 | I | WHITE | Très Sainte Trinité | Dominica Sanctissimæ Trinitatis |
| `Tempora/Pent01-{1..3,5,6}` | Féries après la Trinité | IV | GREEN | Lundi… samedi de la 1re semaine après la Pentecôte | Feria II… infra Hebdomadam I post Octavam Pentecostes |
| `Tempora/Pent01-4` | Pâques + 60 | I | WHITE | Fête du Très Saint Sacrement (Fête-Dieu) | Festum Sanctissimi Corporis Christi |
| `Tempora/Pent{02,03}-0r`, `Tempora/Pent{04..23}-0` | 2e au 23e dimanche après la Pentecôte | II | GREEN | 2e… 23e dimanche après la Pentecôte | Dominica II… XXIII post Pentecosten |
| `Tempora/Pent02-5` | Pâques + 68 | I | WHITE | Sacré-Cœur de Jésus | Sacratissimi Cordis Domini Nostri Jesu Christi |
| `Tempora/Pent02-{1..4}`, `Tempora/Pent02-6Feria`, `Tempora/Pent03-{1..5}Feria`, `Tempora/Pent03-6`, `Tempora/Pent{04..24}-{1..6}` | Féries | IV | GREEN | Lundi… samedi de la Nᵉ semaine après la Pentecôte | Feria II… Sabbato infra Hebdomadam N post Octavam Pentecostes |
| `Tempora/PentEpi{3..6}-0` | Dimanches après l'Épiphanie repris entre le 23e et le 24e dimanche | II | GREEN | 3e… 6e dimanche après l'Épiphanie (repris) | Dominica III… VI quæ superfuit post Epiphaniam |
| `Tempora/PentEpi{3..6}-{1..6}` | Leurs féries | IV | GREEN | Lundi… samedi de la Nᵉ semaine après l'Épiphanie (reprise) | Feria II… infra Hebdomadam N quæ superfuit post Epiphaniam |
| `Tempora/093-{3,5,6}` | Mercredi, vendredi, samedi après le 3e dimanche de septembre | II | VIOLET | Mercredi, vendredi, samedi des Quatre-Temps de septembre | Feria IV, Feria VI, Sabbato Quattuor Temporum Septembris |
| `Sancti/10-DU` | Dernier dimanche d'octobre | I | WHITE | Notre-Seigneur Jésus-Christ Roi | In festo Domini Nostri Jesu Christi Regis |
| `Tempora/Pent24-0` | 1er dimanche de l'Avent − 7 | II | GREEN | 24e et dernier dimanche après la Pentecôte | Dominica XXIV et ultima post Pentecosten |

Notes :
- **Dimanches repris** (n. 18) : le 24e dimanche est toujours le dernier. Entre le 23e et le 24e, on reprend, dans l'ordre, les dimanches empêchés après l'Épiphanie, au plus le 3e, le 4e, le 5e et le 6e ; DO numérote ces semaines par leur distance à l'Avent (`getweek`, `Date.pm`). S'il n'y a que 23 dimanches, le 24e remplace le 23e. Missalemeum garde les identifiants `tempora:Epi3-0`… et emprunte les chants au 23e dimanche ; l'oracle les normalise en `Tempora/PentEpi…`.
- **Christ-Roi** : DO place `10-DUr` le dernier dimanche d'octobre (`Tabulae/Transfer/*.txt`). Ce fichier se réduit à `@Sancti/10-DU` ; la clé retenue est `Sancti/10-DU`, comme chez Missalemeum.
- **Féries de la Trinité et de la Sainte Famille** : la messe de la semaine est celle du dimanche « ordinaire » (`Tempora/Pent01-0a`, `Tempora/Epi1-0a`), que Missalemeum utilise pour les féries sans propre. Le calendrier n'en a pas besoin ; le missel, si.

## 10. Samedi de la Sainte Vierge

Le samedi dont l'office est une férie de IVᵉ classe (n. 78 ; table n. 91, 27°), avec commémoraison éventuelle d'un saint du jour : classe IV, couleur WHITE.

| Clé | Période | Titre fr | Titre la |
|---|---|---|---|
| `Commune/C10a` | Avent (en pratique jamais : les féries de l'Avent sont de IIIᵉ classe ou plus) | Sainte Marie au samedi (Rorate) | Sanctæ Mariæ Sabbato |
| `Commune/C10b` | De Noël au 1er février | Sainte Marie au samedi (Vultum tuum) | Sanctæ Mariæ Sabbato |
| `Commune/C10c` | Du 2 février au mercredi saint (en pratique jusqu'à la Septuagésime) | Sainte Marie au samedi (Salve, sancta Parens) | Sanctæ Mariæ Sabbato |
| `Commune/C10Pasc` | Temps pascal | Sainte Marie au samedi (temps pascal) | Sanctæ Mariæ Sabbato |
| `Commune/C10` | De la Trinité à l'Avent | Sainte Marie au samedi (Salve, sancta Parens) | Sanctæ Mariæ Sabbato |

Missalemeum nomme la dernière `commune:C10t` ; l'oracle la normalise en `Commune/C10`.

## 11. Fêtes mobiles et cas laissés au code

La liste complète figure dans `content/data/calendar/SOURCES.md`, § « Fêtes mobiles laissées au code (1962) ». Elle couvre tout ce qui précède, ainsi que :
- **transferts** des fêtes de Iʳᵉ classe (n. 95-99) ;
- **saint Matthias** le 25 février et **saint Gabriel de la Vierge des Douleurs** le 28 février les années bissextiles (note du Calendarium) ;
- **Défunts** reportés au 3 novembre quand le 2 est un dimanche (n. 96 b). DO emploie alors `Sancti/11-03sec`, et l'oracle la clé `Sancti/11-02`.

DO précalcule ces cas dans `Tabulae/Transfer/{a..g}.txt` et `Tabulae/Transfer/{322..426}.txt` (lettre et date de Pâques), lignes marquées `1960`. C'est un second oracle pour les transferts. Exemples :
- `03-20=03-19` : saint Joseph un dimanche de Carême ;
- `04-01=03-25` : Annonciation transférée après *in Albis* ;
- `06-25=06-24` : Nativité de saint Jean-Baptiste empêchée par le Sacré-Cœur ;
- `08-14=X-X` : vigile de l'Assomption un dimanche ;
- `12-24=12-24s` : vigile de Noël un dimanche ;
- `02-02=02-02-quadp` : Purification après la Septuagésime, variante de propre ;
- `07-16=07-16sab` : Notre-Dame du Mont-Carmel un samedi, office seulement.

## 12. Règles de Missalemeum (`backend/api/kalendar/rules.py`), dans l'ordre d'application

Missalemeum construit l'année en deux temps (`factory.py`).
1. **Temporal** :
   - les blocs de l'Épiphanie (depuis la Sainte Famille), de la Septuagésime au 23e dimanche après la Pentecôte, de la 24e semaine (Avent − 7) et de l'Avent (arrêté au 23 décembre) ;
   - les dimanches après l'Épiphanie repris, insérés à rebours depuis le samedi avant le 24e dimanche, sans écraser ;
   - les Quatre-Temps de septembre, le Saint Nom, le Christ-Roi, le dimanche dans l'octave de Noël et les féries des 29-31 décembre (`Nat1-1`, sans écraser).
2. **Sanctoral** : chaque jour reçoit les identifiants `sancti:MM-JJ…` de la liste SANCTI, triés par rang.

Ensuite, pour chaque jour, la **première règle** qui renvoie un résultat fixe la célébration, les commémoraisons et les reports. Les observances reportées s'ajoutent à celles du jour cible avant ses règles. Un jour sans célébration devient `:feria:4`, avec la couleur du temporal.

| # | Règle | Effet | Rubriques de 1960 |
|---|---|---|---|
| 1 | `rule_nativity_vigil` | 24 décembre un dimanche : la vigile seule, sans le 4e dimanche de l'Avent | n. 30 a |
| 2 | `rule_nativity_has_multiple_masses` | 25 décembre : les trois messes (`12-25m1`, `m2`, `m3`) sont la célébration | Rubriques du Missel (trois messes) ; table n. 91, 1° |
| 3 | `rule_all_souls` | 2 novembre : les trois messes des Défunts ; un dimanche, le dimanche est célébré et les Défunts vont au 3 | n. 16 b, 91 (8°), 96 b |
| 4 | `rule_st_matthias` | Années bissextiles : saint Matthias passe du 24 au 25 février | Calendarium, note de février |
| 5 | `rule_feb27` | Années bissextiles : saint Gabriel de la Vierge des Douleurs passe du 27 au 28 février | Calendarium, note de février |
| 6 | `rule_st_lawrence_vigil` | 9 août : en semaine, vigile avec commémoraison de saint Romain ; le dimanche, la vigile est omise (règles suivantes) ; **le samedi, messe de la Sainte Vierge au samedi** avec commémoraison de saint Romain | n. 32-33 ; le cas du samedi n'a pas d'article (n. 78 réserve le samedi de la Vierge aux féries de IVᵉ classe) : écart probable |
| 7 | `rule_seven_sorrows_on_friday_after_passion_sunday` | Vendredi de la Passion : la férie, avec commémoraison des Sept Douleurs | Calendarium (« Feria VI post dominicam I Passionis ») |
| 8 | `rule_same_class_feasts_take_over_advent_feria_and_ember_days` | Férie de l'Avent ou Quatre-Temps (hors dimanche) : une fête de même classe est célébrée et la férie commémorée ; une fête de classe inférieure est commémorée ; sans fête, la férie | n. 24-25, 108 d-e ; table n. 91 (16°, 18°, 24°, 25°) |
| 9 | `rule_lent_commemoration` | Carême : férie de même classe que le saint → férie, saint commémoré ; saint de classe supérieure → saint, puis les autres saints et la férie commémorés ; deux Iʳᵉ classe → règle 10 | n. 25 a, 108 e, 113 ; table n. 91 (22°) |
| 10 | `rule_shift_conflicting_1st_class_feasts` | Immaculée Conception un dimanche de l'Avent : la fête, avec commémoraison du dimanche. Plusieurs Iʳᵉ classe : la plus haute dans TABLE_OF_PRECEDENCE est célébrée, la suivante reportée au premier jour sans Iʳᵉ ni IIᵉ classe | n. 15, 95-97 |
| 11 | `rule_lord_feast1` | 13 janvier un dimanche : la Sainte Famille, le Baptême est omis | n. 95 al. 2 (deux fêtes de la même Personne), 112 a |
| 12 | `rule_lord_feast2` | Fête du Seigneur de Iʳᵉ ou IIᵉ classe (6 et 13 janvier, 2 février, 6 août) un dimanche de IIᵉ classe : la fête, sans commémoraison du dimanche | n. 16 a |
| 13 | `rule_first_class_feast_with_sunday_commemoration` | Saint Michel (29 septembre), saints Pierre et Paul (29 juin) et saint Adalbert (23 avril, propre polonais) un dimanche de IIᵉ classe : la fête, avec commémoraison du dimanche | n. 108 a, 111 a |
| 14 | `rule_first_class_feast_no_commemoration` | Toute autre fête de Iʳᵉ classe : célébrée seule, sans commémoraison | n. 111 a, en partie : la commémoraison privilégiée du dimanche (n. 108 a) n'est faite que pour les fêtes de la règle 13 ; Missalemeum le signale en TODO. Écart probable, par exemple pour l'Assomption un dimanche |
| 15 | `second_class_feast_no_sunday_commemoration` | Exaltation de la Croix (14 septembre) et Dédicace du Latran (9 novembre) un dimanche : la fête, sans commémoraison du dimanche | n. 16 a (traitées comme fêtes du Seigneur ; `lord: true` dans `general.json`) |
| 16 | `rule_fixed_vigils_suppressed_on_sunday` | Vigiles de saint Jean-Baptiste, de saints Pierre et Paul, de saint Laurent et de l'Assomption un dimanche : le dimanche seul | n. 33 |
| 17 | `rule_2nd_class_sunday` | Dimanche de IIᵉ classe : le dimanche ; une fête de IIᵉ classe est commémorée, les autres omises | n. 16, 111 b |
| 18 | `rule_1st_class_feria` | Cendres, Semaine sainte et dimanche de Pâques : le temporal seul | n. 23 ; table n. 91 (1°, 2°, 7°) |
| 19 | `rule_bmv_office_on_saturday` | Samedi dont toutes les observances sont de IVᵉ classe (ou aucune) : Sainte Marie au samedi (C10a, C10b, C10c, C10Pasc ou C10t selon la période), avec au plus une commémoraison de saint | n. 78-79 ; table n. 91 (27°) |
| 20 | `rule_3rd_class_local_saint_celebrated_3rd_class_general_saint_commemorated` | Calendriers propres (suffixes `pl`, `en`) : la fête locale de IIIᵉ classe l'emporte sur la fête universelle de IIIᵉ classe, commémorée. Sans effet sur le calendrier général | table n. 91 (23° avant 24°) |
| 21 | `rule_4th_class_feria_are_removed_from_celebration` | Férie de IVᵉ classe : retirée ; les saints de rang 4 (commémoraisons) sont commémorés ; sinon une autre observance éventuelle est célébrée | n. 26 (féries de IVᵉ classe jamais commémorées) |
| 22 | `rule_4th_class_commemorations_are_only_commemorated` | Commémoraison de saint (rang 4) : jamais célébrée, seulement commémorée | n. 5, 108-109 (simple commémoraison) |
| 23 | `rule_general` | Cas restants : tri par TABLE_OF_PRECEDENCE, puis classe ; la première célébrée, la deuxième commémorée, sauf si la première est un dimanche ou si la deuxième est le temporal du jour | table n. 91 ; n. 93, 113-114 |

Remarques pour le moteur Kotlin :
- **Table de préséance** : la `TABLE_OF_PRECEDENCE` de Missalemeum (`constants/common.py`) est une version abrégée de la table n. 91. Elle ne distingue pas fêtes universelles et propres, ni fixes et mobiles, que la table ordonne pourtant.
- **Nombre de commémoraisons** (n. 111) : Missalemeum n'en retient en général qu'une. Les rubriques en admettent une aux jours de IIᵉ classe et deux aux jours de IIIᵉ et IVᵉ classe ; il faut aussi l'exclusion du n. 112 et l'ordre du n. 113.
- **Commémoraisons intrinsèques** :
  - DO les place dans le propre, `general.json` les porte sans clé : saint Pierre et saint Paul (n. 110), octave de Noël ;
  - Missalemeum les modélise à part, avec des identifiants absents de DO : `sancti:01-25c`, `sancti:02-22c`, `sancti:12-26c`, `sancti:12-27c`, `sancti:12-28c` et `tempora:Quad5-5Feriac`. Il n'a pas de saint Pierre le 30 juin.
- **Couleurs des fêtes de IIIᵉ classe** : Missalemeum se trompe sur une quinzaine de fêtes ; il prend souvent la couleur du saint commémoré. La liste est dans le README de `content/sources/calendrier-1962/`.

## 13. Identifiants proposés

Le moteur choisit ses identifiants. Cette proposition reprend ceux de `1969/TEMPORAL.md` quand la célébration est la même. `1962/fr.json` emploie déjà `nativity_of_the_lord` pour porter le précepte de Noël.

| Jours | Identifiant | Rapport avec 1969 |
|---|---|---|
| Dimanches et féries de l'Avent | `advent_{w}_sunday`, `advent_{w}_{jour}` (w = 1…4, jusqu'au 23 décembre) | même motif ; 1969 passe à `advent_december_{n}` le 17 |
| Vigile de Noël | `vigil_of_the_nativity_of_the_lord` | nouveau |
| Noël | `nativity_of_the_lord` | identique |
| Dimanche dans l'octave | `sunday_within_the_octave_of_christmas` | nouveau (1969 : Sainte Famille ce dimanche) |
| 29-31 décembre | `christmas_octave_day_{n}` (n = 5…7) | même motif |
| 1er janvier | `octave_of_the_nativity_of_the_lord` | nouveau (`mary_mother_of_god` est une autre célébration) |
| Saint Nom de Jésus | `most_holy_name_of_jesus` | identique (romcal, 3 janvier) |
| 2-5 janvier | `christmas_time_january_{n}` | même motif |
| Épiphanie | `epiphany_of_the_lord` | identique |
| 7-12 janvier | `epiphany_time_january_{n}` | nouveau |
| Sainte Famille | `holy_family_of_jesus_mary_and_joseph` | identique |
| Baptême (13 janvier) | `baptism_of_the_lord` | identique |
| Après l'Épiphanie, y compris les semaines reprises en novembre | `epiphany_{w}_sunday`, `epiphany_{w}_{jour}` | nouveau |
| Septuagésime, Sexagésime, Quinquagésime | `septuagesima_sunday`, `septuagesima_{jour}`, `sexagesima_…`, `quinquagesima_…` | nouveau |
| Cendres et jours suivants | `ash_wednesday`, `{jour}_after_ash_wednesday` | identique |
| Carême | `lent_{w}_sunday`, `lent_{w}_{jour}` (w = 1…4) | même motif |
| Temps de la Passion | `first_sunday_of_the_passion`, `passion_week_{jour}` | nouveau (1969 : `lent_5_…`) |
| Rameaux, Semaine sainte | `palm_sunday_of_the_passion_of_the_lord`, `holy_monday`, `holy_tuesday`, `holy_wednesday`, `thursday_of_the_lords_supper`, `friday_of_the_passion_of_the_lord`, `holy_saturday` | identiques |
| Pâques et son octave | `easter_sunday`, `easter_{jour}` | identiques |
| Dimanche *in albis* | `low_sunday` | nouveau (1969 : `divine_mercy_sunday`, même jour) |
| Après Pâques (numérotation du Missel de 1962) | `after_easter_{w}_sunday` (w = 2…5), `after_easter_{w}_{jour}` (w = 1…4) | nouveau |
| Rogations, vigile, Ascension | `rogation_monday`, `rogation_tuesday`, `vigil_of_the_ascension_of_the_lord`, `ascension_of_the_lord` | Ascension identique |
| Après l'Ascension | `after_ascension_sunday`, `after_ascension_{jour}` | nouveau |
| Pentecôte | `vigil_of_pentecost`, `pentecost_sunday`, `pentecost_{jour}` | `pentecost_sunday` identique |
| Trinité, Fête-Dieu, Sacré-Cœur | `most_holy_trinity`, `most_holy_body_and_blood_of_christ`, `most_sacred_heart_of_jesus` | identiques |
| Après la Pentecôte | `after_pentecost_{w}_sunday` (w = 2…24), `after_pentecost_{w}_{jour}` (w = 1…24) | nouveau |
| Quatre-Temps de septembre | `september_ember_wednesday`, `september_ember_friday`, `september_ember_saturday` | nouveau |
| Christ-Roi | `our_lord_jesus_christ_king_of_the_universe` | même célébration, autre date |
| Samedi de la Sainte Vierge | `saturday_of_our_lady` | nouveau |
