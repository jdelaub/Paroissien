# Ordinaire de la messe (1962) : provenance

Publication `ordinaire.1962`, paquet `ordinaire` (`licence-libre:MIT`, version 2026101201). C'est l'Ordinaire de la messe basse selon le missel de 1962, en latin et en français, aligné bloc à bloc.

| | |
|---|---|
| Unités | 6 : `avant-messe`, `messe-des-catechumenes`, `offertoire`, `canon`, `communion`, `fin-de-messe` |
| Sections | 37 (titres `heading` : 6 de niveau 1, 31 de niveau 2) |
| Blocs | 295 : 37 `heading`, 94 `versicle`, 86 `para`, 78 `rubric` |
| Collation | latin : 2 985 mots, tous dans Divinum Officium ; français : 4 192 mots, 94,7 % confirmés par l'OCR (voir « Relecture ») |

## Droits

- **Latin** : Divinum Officium, sous licence MIT depuis le 28/08/2026 (`LICENSE` du dépôt, « Copyright (c) 2026 Divinum Officium »). La notice MIT complète est recopiée dans `license.notice` de `publication.json` et voyage donc avec le paquet.
- **Français** : paroissiens anonymes de 1869, 1890, 1913 et 1914, du domaine public (les mêmes éditions que les prières classiques).
- **Rubriques et ajustements à 1962** : rédigés par Paroissien.

D'où `license.spdx` = `MIT AND LicenseRef-PublicDomain` et, pour le paquet, `rights` = `licence-libre:MIT`, qui est un statut public (`package.is_public`, `tools/check-pack-rights.py`). Aucun texte du Barroux ni de l'AELF n'entre ici.

## Chaîne

```bash
python3 -I content/sources/ordinaire/fetch.py                        # Ordo de DO et OCR des paroissiens (cache)
python3 -I content/sources/ordinaire/fetch.py page ID N… [--scale 2]  # fac-similés, pour la relecture
python3 -I content/sources/prieres/ocr.py find ID 'regex'             # recherche dans l'OCR (outil des prières)
python3 -I content/sources/ordinaire/normalize.py -v                  # texte/*.txt → content/data/ordinaire.1962
python3 -I content/sources/ordinaire/check.py --tsv content/sources/ordinaire/collation.tsv
python3 -I content/pqt.py validate ordinaire.1962
```

- **Latin** : `fetch.py` télécharge cinq fichiers de Divinum Officium (`LICENSE`, `web/www/missa/Latin/Ordo/{Ordo,Prayers,Prefationes,Communio}.txt`) au commit `b9c100d79ae481c67b2801fdd3622b00dd86bd73`, celui du calendrier de 1962. Ils sont pris sur raw.githubusercontent.com, un fichier par requête, et rangés dans `content/.cache/raw/ordinaire/divinum-officium/`. `fetch-log.json` consigne l'URL, la date, la taille et le sha256 de chaque fichier ; les 16 premiers caractères du sha256 sont aussi dans `x-divinum-officium` de `publication.json`.
- **Français** : `fetch.py` reprend la fonction de téléchargement de `content/sources/prieres/fetch.py` et partage son cache `content/.cache/raw/prieres/<identifiant>/`. Même User-Agent `Paroissien-pipeline/0.1`, une seconde de pause entre deux requêtes, rien n'est retéléchargé, et aucun fichier téléchargé n'est exécuté.
- **Transcriptions** (`texte/<unité>.txt`) : même format que les prières, avec trois ajouts.
  - `== clé 2` ouvre une section de niveau 2.
  - `src: do | la | Fichier.txt | lignes` cite Divinum Officium ; le lien `x-source` pointe vers ces lignes, au commit figé.
  - `wit:` donne un témoin, que lit `check.py` mais que `x-source` ne cite pas.
- **`normalize.py`** applique la typographie française au français seulement. Le latin garde celle de Divinum Officium : accents, æ, œ et ǽ, sans espace avant « : » et « ; ». Les identifiants sont `<clé>` pour la section et `<clé>.<n>` pour ses blocs. Le registre `content/registry/ordinaire.1962.ids` est créé (301 identifiants).
- **`check.py`** collationne le latin avec les lignes de DO citées, et le français avec l'OCR des pages citées (`src` et `wit`). Les rubriques ne sont pas collationnées.

## Sources

| Source | Rôle | Pages imprimées (images archive.org `/page/nN`) |
|---|---|---|
| Divinum Officium, `Ordo.txt`, `Prayers.txt`, `Prefationes.txt`, `Communio.txt` | **tout le latin** | lignes citées dans chaque `src: do` |
| [`cihm_990278`](https://archive.org/details/cihm_990278) *Nouveau paroissien romain*, Saint-Hyacinthe, É. Solis, 1913 | **français de référence** : « L'Ordinaire de la sainte Messe » et « Prières après chaque messe basse » | 84-128 (n87-n90, n94-n134 ; n91-n93 sont des mires de microfiche) ; 80-83 (n83-n86) |
| [`cihm_80314`](https://archive.org/details/cihm_80314) *Paroissien de poche*, Québec, I.-P. Déry, 1914 | témoin : même traduction, légèrement retouchée ; sert à collationner et à compléter les conclusions abrégées par Solis | 36-84 (n35-n83) |
| [`paroissienromai00churgoog`](https://archive.org/details/paroissienromai00churgoog) *Paroissien romain*, Tours, Mame, 1869 | préface de la Sainte Trinité, absente de Solis | 128-129 (n138-n139) |
| [`paroissienromain00tour`](https://archive.org/details/paroissienromain00tour) *Paroissien romain*, Tours, Mame, 1890 | Ave Maria des prières après la messe basse ; c'est le texte des prières classiques (`ave`) | 20-21 (n33-n34) |

Correspondance entre index hOCR et image : `LEAF_OFFSET` de `content/sources/prieres/ocr.py` (3 pour les deux paroissiens de Québec, 0 pour Mame 1869). Chez Solis, l'image vaut la page imprimée + 3 jusqu'à la page 87, puis + 6. Chez Déry, elle vaut la page − 1.

**Pourquoi Solis et non Mame.** L'« Ordinaire de la sainte Messe » de Mame 1869 (p. 94-118) et celui de Mame 1890 (p. 84 et suiv.) portent la mention « pour servir surtout aux Messes chantées ». Ils ne donnent en regard que les parties dites à haute voix : psaume 42, Confiteor, Gloria, Credo, préface, Pater, Agnus Dei. L'Offertoire et le Canon y sont remplacés par des prières de dévotion (« Recevez, Père adorable, les commencements de notre sacrifice… »). ≈ C'était l'usage des paroissiens français tant que l'Index interdisait de traduire l'Ordinaire, levée en 1897 (à vérifier). Solis 1913 et Déry 1914, postérieurs, traduisent tout l'Ordinaire, Canon compris. Ce sont les deux paroissiens du domaine public cités au README des prières qui le font : la recherche de *Te igitur*, *Suscipe, sancte Pater*, *Unde et memores* et de leurs traductions dans les neuf éditions ne trouve que ces deux-là (et Barbou vers 1895, dont l'OCR est inutilisable).

## Ajustements à 1962

Solis 1913 imprime l'Ordinaire de la messe basse d'avant les réformes de 1955-1962, illustré par le propre du premier dimanche de l'Avent. Les ajustements suivants le ramènent au missel de 1962. Ils sont repris dans les rubriques (`rubric`) ou dans le texte, et chacun reçoit une justification.

| Bloc | Ajustement | Justification |
|---|---|---|
| `introit` … `postcommunion` | Les pièces propres du 1ᵉʳ dimanche de l'Avent (Introït, Collecte, Épître, Graduel, Évangile, Offertoire, Secrète, Communion, Postcommunion) sont retirées. Une rubrique « voir au propre » les remplace. | Elles varient chaque jour et appartiennent au Propre. |
| `evangile.4` | « Suite du saint Évangile selon saint Luc » devient « selon saint N. », et *Sequéntia sancti Evangélii secúndum N.* (DO, Ordo.txt l. 114). | Formule générale de l'Ordinaire. |
| `judica.1` | Le psaume 42 est omis aux messes des défunts et au temps de la Passion. | Rubrique du livre (p. 84). DO la conditionne de même (Ordo.txt l. 8 : « a Dominica Passionis usque ad Sabbatum sanctum exclusive »). |
| `gloria.1` | « On omet le Gloria aux messes avec ornements noirs ou violets » (note p. 92) devient « quand la messe le demande ; jamais aux messes des défunts ». | En 1962, les féries *per annum* (vert) sont aussi sans Gloria. La règle du livre ne vaut donc plus ; on renvoie au propre. |
| `credo.1` | « Le Credo, qui se dit le dimanche et à certaines fêtes » devient « les dimanches, aux fêtes de 1ʳᵉ classe et aux autres messes où le propre l'indique ». | Classes de 1960. |
| `lavabo.9` | Le Gloria Patri du Lavabo est omis aux messes du temps, du premier dimanche de la Passion au Jeudi saint, en plus des messes des défunts (note du livre). | Ritus servandus VII ; même règle pour l'introït (Code des rubriques, n. 428). |
| `preface`, `preface-commune`, `preface-trinite` | Seules la préface commune (Solis) et celle de la Sainte Trinité (Mame 1869, rubrique « tous les dimanches qui n'en ont pas de propre ») sont données. Une rubrique renvoie au propre pour les autres. | Demande éditoriale. En 1962, la préface de la Trinité est celle des dimanches sans préface propre. |
| `te-igitur.7` | *Communicantes* : « sed et beáti Joseph, ejúsdem Vírginis Sponsi » (DO, Prefationes.txt `[C-common1962]`). En français : « et aussi du bienheureux Joseph, Époux de la même Vierge », traduction de Paroissien. | Décret de la S. Congrégation des Rites du 13 novembre 1962, en vigueur le 8 décembre 1962. Les missels de 1962 imprimés ensuite le portent. |
| `consecration.3` | La rubrique de l'élévation garde l'invocation « Mon Seigneur et mon Dieu ! » et perd l'indulgence. | Indulgences « en jours et quarantaines » abolies en 1967 (rapport 14). |
| `communion-des-fideles` | La communion des fidèles est ajoutée (absente du livre) **sans Confiteor ni absolution** : *Ecce Agnus Dei*, *Domine, non sum dignus*, *Corpus Domini… animam tuam*. Le latin vient de DO, Communio.txt l. 8-16 ; les l. 1-7, Confiteor, Misereatur et Indulgentiam, ne sont pas reprises. | Code des rubriques de 1960, n. 503. Le fichier de DO garde ce Confiteor et laisse son script le retirer pour 1960. |
| `ite-missa-est.5`, `benediction.1` | Benedicamus Domino : « Si le Gloria n'a pas été dit… » (livre) devient « seulement quand une procession suit la messe, ainsi le Jeudi saint ». Pas de bénédiction après lui. On ajoute l'alléluia de l'octave de Pâques. | Code des rubriques de 1960, n. 507-508. |
| `dernier-evangile.1` | Le dernier Évangile est le prologue de saint Jean, sauf aux Rameaux sans procession (propre). Il est omis quand une procession suit la messe, à la 3ᵉ messe de Noël, aux Rameaux après la procession, à la Vigile pascale et à la messe des défunts suivie de l'absoute. | Code des rubriques de 1960, n. 509-510. |
| `prieres-apres-la-messe` | Les prières léonines après la messe basse sont gardées : elles sont en vigueur en 1962 et ne sont supprimées qu'en 1964-1965. L'oraison porte saint Joseph et l'invocation au Sacré-Cœur (Pie X, 1904). Le livre ne nomme pas les trois Ave : une rubrique les donne, avec l'Ave en regard (DO l. 395-396 ; Mame 1890). | Ordo.txt l. 393-415 (`!*R`, messe basse). |

Parties de l'Ordo de DO **non reprises**, absentes du paroissien :

- la messe solennelle : bénédiction et prières de l'encensement à l'Introït et à l'Offertoire (*Per intercessionem beati Michaelis*, *Incensum istud*, *Dirigatur*, *Accendat*), *Munda cor* dit par le diacre, baiser de paix (*Pax tecum*) ;
- l'*Asperges* et le *Vidi aquam*, qui précèdent la grand-messe du dimanche ;
- les insertions propres du *Communicantes* et du *Hanc igitur* (Noël, Pâques, Pentecôte…), qui sont au propre.

## Corrections et normalisations

**Latin (DO).**
- Les rubriques incidentes entre parenthèses sont retirées, par exemple *(Percutit sibi pectus ter, dicens:)* ou *(Signat ter super Hostiam…)*. Les principales sont reprises en rubriques françaises avant le texte.
- Les marques de script de DO sont retirées : `wait10`, `wait5`, `N.p` et `N.b` (deviennent « N. »), *(vel meis)* dans le *Suscipiat*.
- Les « + » et « ++ » de DO, qui marquent les signes de croix, deviennent ✠.
- Les paroles de la consécration restent en capitales, comme dans DO.
- Le *Lavabo*, le dernier Évangile et le *Te igitur* sont coupés en plusieurs blocs aux mêmes endroits que le français, pour l'alignement.

**Français (Solis 1913).** Coquilles du livre corrigées :

| Bloc | Livre | Corrigé | Page |
|---|---|---|---|
| `evangile.9` | « Que par la saint Évangile » | « le saint » | 98 |
| `credo.6` | « le troisième iour » | « jour » | 100 |
| `munda-cor.2` | « mis-séricordieuse » (coupure) | « miséricordieuse » | 96 |
| `sanctus.1` | « Saint, Saint Saint, » | virgule restituée | 109 |
| `te-igitur.2` | « Nous vous supplions. Père très clément » | virgule | 110 |
| `te-igitur.3` | « pour notre sainte Église », « pour notre serviteur notre Pape » | « votre » (*Ecclesia tua*, *famulo tuo*), comme Déry p. 64 | 110 |
| `te-igitur.9` | « enrôles » | « enrôlés » | 112 |
| `consecration.1` | « Prenez et mangez en tous » | « mangez-en » (Déry p. 67) | 113 |
| `consecration.4` | « e même » (lettrine tombée) | « De même » | 113 |
| `offrande.8` | « Nous vous présentons devant vous » (de même chez Déry) | « Nous nous présentons » (*suscipiamur*) | 103-104 |
| `pater.4`, `pater.5` | la colonne française donne « Mais délivrez-nous du mal » au prêtre et « Ainsi soit-il » au servant | rôles du latin (℟ puis ℣) | 118 |

**Abréviations développées et conclusions complétées d'après Déry 1914 :**
- « par le Christ N. S. » devient « par le Christ notre Seigneur » (`preface-commune.2`) ;
- « Par J.-C. N. S. » devient « Par Jésus-Christ notre Seigneur. Ainsi soit-il. » (`placeat.2`, Déry p. 81) ;
- « Vous qui vivez… » devient « Vous qui vivez et régnez dans les siècles des siècles. Ainsi soit-il. » (`ablutions.4`, Déry p. 79) ;
- « Commencement du S. Évangile » devient « du saint Évangile » ;
- « Père, Fils et S.-Esprit » devient « Saint-Esprit » ;
- « Jésus-Christ… » devient « Jésus-Christ. » à la fin du *Quam oblationem*.

**Retraits, avec renvoi au README** :
- le Confiteor du prêtre, que le livre abrège en « Je confesse, etc. ». On donne celui des fidèles, avec « mes frères » au lieu de « mon Père », comme le latin du prêtre (*et vobis, fratres*) (`confiteor.2`) ;
- les incipits « Aufer. — », « Oremus. — », « Suscipe. — » qui ouvrent les prières françaises ;
- les astérisques qui balisent la partie commune de la préface ;
- les mentions entre parenthèses dans le texte : « (le prêtre baise l'autel) », « (On nomme ici les défunts…) », « (Ici l'on s'agenouille.) ». Elles deviennent des rubriques ;
- les indulgences, consignées dans `removed:` :
  - « (Indulgence de 7 ans et de 7 quarantaines) » à l'élévation ;
  - « (300 jour d'indulgence.) » sur les prières après la messe basse ;
  - « (Indulgence de 7 ans et de 7 quarantaines pour cette invocation. PIE X, 7 juin 1904). » sur le Sacré-Cœur.

**Normalisations** :
- les italiques d'insistance du livre (« ôtez de nous *nos iniquités* ») sont imprimées en romain ;
- lettrines et petites capitales (« QUI A PRIS CHAIR… ») sont en casse normale ;
- les croix † du livre (*Pax Domini*) deviennent ✠ ;
- « A eux », « O Dieu », « Saint Pierre et Paul » restent tels qu'imprimés.

**Rubriques.** Elles sont courtes et en français seulement. Elles reprennent celles de Solis quand elles conviennent (« Le Prêtre profondément incliné, récite le Confiteor »), sinon elles sont rédigées d'après l'Ordo de DO.

## Points vérifiés

Les neuf blocs d'abord marqués `x-review` ont été repris le 12/10/2026. Les règles de 1960 sont confirmées par le Code des rubriques (traduction anglaise de Divinum Officium, `web/www/horas/Help/Rubrics/Missal1960 rubrics.html`, n. 424-428, 503, 507-510). Deux rubriques ont été corrigées : le Benedicamus Domino (n. 507 : seulement si une procession suit) et le dernier Évangile (n. 509 : propre aux Rameaux sans procession). Les traductions de Paroissien (saint Joseph au *Communicantes*, *Ecce Agnus Dei*, *Corpus Domini… animam tuam*) et les deux corrections du livre (`confiteor.2`, `offrande.8`) sont gardées. Chaque justification reste en commentaire `# vérifié :` dans `texte/`.

## Relecture

1. Pour chaque section, ouvrir `texte/<unité>.txt`, puis la page citée par `src` (`https://archive.org/details/<id>/page/n<image>`), ou en local avec `fetch.py page <id> <image> --scale 2`. Tout le français a été relu sur les fac-similés de Solis (n83-n134) et de Mame 1869 (n138-n139).
2. `collation.tsv` liste, bloc par bloc, les mots que la source ne confirme pas (forme pliée).
   - Latin : aucun, tout le texte est dans les lignes citées de DO.
   - Français : 222 mots sur 4 192. Ce sont des fautes d'OCR (colonnes latin et français entremêlées, lettrines, mots coupés), les corrections et compléments ci-dessus, ou les blocs composés par Paroissien. Un mot qui n'entre dans aucune de ces catégories est une erreur de transcription.
3. Après correction, lancer dans l'ordre :
   - `normalize.py` ;
   - `check.py --tsv …` ;
   - `pqt.py validate ordinaire.1962` ;
   - `pqt.py registry update ordinaire.1962` si des blocs ont été ajoutés.

   Un identifiant publié ne disparaît jamais : un bloc retiré passe par `content/data/ordinaire.1962/aliases.json`.
