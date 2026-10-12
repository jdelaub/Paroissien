# Missel de 1962 : Propre de la messe (provenance)

Publication `missel.1962`, paquet `missel` (`licence-libre:MIT`, version 2026101201). C'est le Propre de la messe selon les rubriques de 1960 (missel de 1962), tiré de Divinum Officium : le latin seul, avec des titres et des rubriques en français rédigés par Paroissien. Le dépôt privé publie la même publication avec la traduction française de Divinum Officium, sous le même `packageId` et une version supérieure (2026101202) : l'app garde la plus haute version, la saveur personnelle montre donc le missel bilingue.

| | |
|---|---|
| Unités | 712 : Propre du temps 406 (Avent 27, Temps de Noël 27, après l'Épiphanie 35, Septuagésime 17, Carême 32, Passion et Semaine sainte 14, Temps pascal 56, après la Pentecôte 198), Propre des saints 270 (par mois), Communs des saints 33, Messes des défunts 2, Préfaces 1 |
| Formulaires | 716 : trois messes à Noël et le 2 novembre, deux le Jeudi saint ; dont 2 cérémonies données en entier (Vendredi saint, Vigile pascale) |
| Blocs | 31 490 : 8 065 `heading`, 9 475 `rubric`, 11 023 `para`, 2 710 `versicle`, 217 `stanza` |
| Clés | 682 clés de Divinum Officium couvertes (`cles.txt`) |
| Sources | 497 fichiers de Divinum Officium (latin, `LICENSE`, `Tabulae/Tempora/Generale.txt`), sha256 dans `sources.sha256` |
| Paquet | 18,0 Mo (18 436 096 octets) |

## Droits

- **Latin, rubriques latines, références** : Divinum Officium, licence MIT depuis le 28/08/2026 (`LICENSE` du dépôt, « Copyright (c) 2026 Divinum Officium »). La notice complète est recopiée dans `license.notice` de `publication.json`.
- **Français du paquet public** : titres des jours (repris du calendrier de l'app : `general.json`, `fr.json`, `Calendar1962.kt`), intitulés des pièces, rubriques de rang, de couleur, de Gloria, de Credo et de préface, stations, titres des lectures (« Lecture de l'Épître de saint Paul, Apôtre, aux Romains ») : rédigés par Paroissien.
- **Aucun texte des fichiers français de DO** (traduction du Propre par l'abbaye du Barroux, usage personnel seulement) n'entre ici. `build.py` les lit pour écrire le dépôt privé et pour découper les sections aux mêmes endroits dans les deux paquets (mêmes identifiants) ; `check.py` vérifie qu'aucun texte français du paquet public ne figure dans ces fichiers.

D'où `license.spdx` = `MIT` et `rights` = `licence-libre:MIT` (`tools/check-pack-rights.py` : conforme).

## Chaîne

```bash
python3 -I content/sources/missel/fetch.py        # extraction partielle de DO (≈ 20 Mo) dans content/.cache/raw/missel/
python3 -I content/sources/missel/keys.py         # clés du calendrier de l'app, couverture de l'oracle Missalemeum
python3 -I content/sources/missel/build.py -v     # → content/data/missel.1962, cles.txt, sources.sha256 ;
                                                  #   et ../paroissien-content/data/missel.1962 si le dépôt privé est là
python3 -I content/sources/missel/check.py --private ../paroissien-content/data
python3 -I content/pqt.py validate missel.1962
python3 -I content/pqt.py registry update missel.1962
```

| Fichier | Rôle |
|---|---|
| `fetch.py` | Clone superficiel et partiel (`--depth 1`, `--filter=blob:none`, sparse-checkout : `missa/Latin`, `missa/Francais`, `horas/{Latin,Francais}/Commune`, `Tabulae`, `LICENSE`), User-Agent `Paroissien-pipeline/0.1` |
| `do.py` | Lecture de DO : sections, conditions de 1960, inclusions `@` (fichier entier, section, substitutions `s/…/…/` et sélections de lignes), emplacement des Communs dans `horas/` |
| `keys.py` | Clés dont le calendrier a besoin (port de `Calendar1962.base()`, sanctoral, samedis de la Sainte Vierge) ; contrôle par l'oracle |
| `build.py` | Formulaires, règles de 1960, alignement latin-français, écriture des deux arbres |
| `check.py` | Complétude des formulaires, clés, marques de DO, absence du français de DO dans le paquet public |
| `cles.txt` | Clés couvertes, une par ligne, triées ; un test Kotlin s'en sert pour vérifier que chaque jour de 2020 à 2045 trouve sa messe |
| `sources.sha256` | sha256 des fichiers de DO lus pour le paquet public (son empreinte est dans `x-divinum-officium` de `publication.json`) |

`do.py` réutilise `content/sources/calendrier-1962/divinum.py` (port de `SetupString.pl`). Celui-ci a reçu un paramètre `context` facultatif (`vero`, `process_conditional_lines`) qui donne la valeur des sujets dépendant du jour (`tempore`, `missa`, `feria`, `commune`) ; sans lui, rien ne change pour le calendrier. Les fichiers de DO ne sont jamais exécutés et Python est lancé avec `-I`.

## Source figée

Divinum Officium, commit `b9c100d79ae481c67b2801fdd3622b00dd86bd73` (11/10/2026), le même que le calendrier de 1962 et l'Ordinaire : les clés de `general.json` et les textes viennent ainsi d'un même état du dépôt. Version rubricale : « Rubrics 1960 - 1960 ». La messe n° 1 (`missa longior`) est retenue, ce qui donne aux samedis des Quatre-Temps toutes leurs leçons.

## Clés et unités

- **Identifiant d'unité** = clé de DO en minuscules, « / » remplacé par « - », exactement comme `missalUnit()` de `TodayScreen.kt` : `Tempora/Adv1-0` → `tempora-adv1-0`, `Sancti/10-DU` → `sancti-10-du`, `Commune/C10b` → `commune-c10b`.
- **Clés énumérées** (`keys.py`) :
  - le temporal, par un port de `Calendar1962.base()` parcouru sur 1900-2299 (toutes les dates de Pâques) : 413 clés, dont les samedis de la Sainte Vierge (`Commune/C10`, `C10b`, `C10c`, `C10Pasc`) ;
  - le sanctoral : toutes les entrées de `general.json` et `fr.json` (`proper`, sinon `Sancti/<date>`), commémoraisons comprises : 271 clés ;
  - le Baptême du Seigneur (`Sancti/01-13`) et le Christ-Roi (`Sancti/10-DU`) ;
  - contrôle : chaque clé de l'oracle Missalemeum 2020-2045, ramenée comme dans `GoldenTest.kt`, est couverte.
- **Substitutions de 1960** (`Tabulae/Tempora/Generale.txt`) : l'app donne la clé du jour (`Tempora/Quad6-5`), DO range le formulaire de 1960 dans un autre fichier (`Quad6-5r.txt`). L'unité porte la clé de l'app et le texte du fichier substitué : `Quad6-0r`, `Quad6-4r`, `Quad6-5r`, `Quad6-6r`, `Quad5-5Feria`, `Pasc2-3Feria` à `Pasc3-3Feria`, `Pasc3-0r`, `Pasc5-1r`, `Pasc6-4r`, `Pasc6-6r`, `Pent01-0r`, `Pent02-0r`, `Pent02-6Feria`, `Pent03-0r`, `Pent03-1Feria` à `Pent03-5Feria`. Il n'y a donc pas d'unité `tempora-quad6-5r` : le Vendredi saint est `tempora-quad6-5`.
- **Clés sans formulaire propre dans DO, ou ambiguës** :
  - `Sancti/01-03` (sainte Geneviève, calendrier de France) : le fichier 01-03 de DO est l'ancienne octave de saint Jean, supprimée en 1955. Le formulaire est pris au commun des vierges (`C6a`, *Dilexisti*), avec le nom *Genovéfæ* dans la collecte. Le propre de France n'est pas dans DO : à relire.
  - `Sancti/05-30` : la même clé désigne saint Félix Ier (commémoraison du calendrier général) et sainte Jeanne d'Arc (France, IIᵉ classe, et sa solennité extérieure). DO n'a pas la messe de Jeanne d'Arc ; l'unité donne celle de saint Félix et une rubrique le signale. Il faudrait une clé distincte dans `fr.json` (par exemple `Sancti/05-30fr`) et une source pour la messe propre.
  - `Tempora/Nat26`, `Nat27`, `Nat28` : produites par `base()`, jamais célébrées (saint Étienne, saint Jean et les saints Innocents, de IIᵉ classe, l'emportent toujours) ; pas d'unité.
  - `Tempora/PentEpi3-0` n'existe jamais : ce dimanche est toujours le Christ-Roi.
  - `Commune/C10a` (samedi de la Sainte Vierge en Avent) : jamais choisi par l'app (les féries de l'Avent sont de IIIᵉ classe), donné dans les Communs.
  - Les clés de commémoraison seule (`Tempora/Quad5-5Feriac`, `Sancti/01-14cc`…) n'ont pas d'unité : leurs oraisons sont dans le formulaire du jour.

## Divisions

Propre du temps (huit temps), Propre des saints (douze mois, de janvier à décembre), Communs des saints, Messes des défunts, Préfaces. L'ordre est liturgique : semaines puis jours ; le temps de Noël va de la Vigile à la commémoraison du Baptême ; les Quatre-Temps de septembre suivent le 17ᵉ dimanche après la Pentecôte ; les dimanches repris de l'Épiphanie (`PentEpi`) se placent entre le 23ᵉ et le dernier dimanche. Le Christ-Roi termine octobre. Les messes du 2 novembre sont dans les Messes des défunts, avec la messe quotidienne.

- **Communs** : vigile d'un Apôtre, martyrs (*Statuit*, *Sacerdotes Dei*, *In virtute*, *Lætabitur*, *Si diligis*, *Protexisti*, *Intret*, *Sapientiam*, *Salus autem*, *Sancti tui*), confesseurs pontifes, souverains pontifes, docteurs, confesseurs non pontifes, abbés, vierges, saintes femmes, Dédicace, fêtes de la Sainte Vierge, samedis de la Sainte Vierge (cinq formulaires). DO n'a pas de messe dans les communs des Apôtres et des Évangélistes (`C1`, `C1a` : office seulement) : ils manquent ; les Apôtres ont leur messe au Propre des saints.
- **Messes des défunts** : les trois messes du 2 novembre (`Sancti/11-02m1` à `m3`) et la messe quotidienne (`Commune/C9`), avec ses oraisons au choix. Les messes *In die obitus* et *In anniversario* du missel de 1962 ne sont pas dans DO à ce commit : elles manquent.
- **Préfaces** : les 15 préfaces du missel, puis celles « propres à certains lieux » données par DO (Avent, Saint-Sacrement, tous les Saints, Dédicace, saint Jean-Baptiste) et celle de la messe chrismale ; les *Communicantes* et le *Hanc igitur* propres (versions de 1962, avec saint Joseph). Messes votives : non faites.

## Un formulaire

Titre (niveau 1, français et latin), rubrique de rang et de couleur (« Iʳᵉ classe — ornements violets. Station à Sainte-Marie-Majeure. »), puis chaque pièce en titre de niveau 2 : Introït, Collecte, Épître ou Leçon, Graduel (ou Alléluia, ou Trait), Séquence, Évangile, Offertoire, Secrète, Préface (renvoi), Communion, Postcommunion, Oraison sur le peuple. Les rubriques « On dit le Gloria in excelsis » et « On dit le Credo » suivent l'Introït et l'Évangile. Les cérémonies (rameaux, cierges, cendres, saintes huiles) précèdent la messe ; le Jeudi saint a le lavement des pieds et le dépouillement des autels.

Identifiants : `<unité>.titre`, `<unité>.rang`, `<unité>.<pièce>` (titre), `<unité>.<pièce>.<n>` (lignes) ; `.m1`… pour les messes multiples ; `.lecon-1`, `.graduel-1`, `.oraison-1` pour les leçons des Quatre-Temps ; `.c1` pour une commémoraison ; `.tp`, `.sept`, `.hors` pour les variantes de temps ; `.a1` pour les oraisons au choix des défunts. Tous ont moins de 64 caractères.

## Règles de 1960

Port des fonctions de `web/cgi-bin/missa/propers.pl` :
- **Gloria** : `gloriflag` (dimanches, saints, temps pascal ; pas en Avent, Septuagésime, Carême, aux féries *per annum*, aux défunts ; `[Rule]` l'emporte). Féries après la Sainte Famille : Gloria jusqu'au 13 janvier, puis non (rubrique).
- **Credo** : `Credo` (dimanches, rang de IIᵉ classe et plus, octaves ; `[Rule]`, `CredoDA` pour 1960), puis le n. 471 des rubriques de 1960 : pas de Credo aux fêtes de IIIᵉ classe, aux commémoraisons et aux féries de IIIᵉ et IVᵉ classe. Cela retire le Credo que DO donne encore à Notre-Dame de Lourdes, au Saint Nom de Marie, à Notre-Dame de la Merci et au samedi de la 2ᵉ semaine après Pâques (`Pasc2-6Feria`). Saint Barnabé, apôtre, garde le sien.
- **Préface** : `prefatio` (`Prefatio1960=`, `Prefatio=` avec le mot propre, « Et te in Assumptióne » ; sinon temps, Sainte Vierge, souverains pontifes, défunts, dimanche, commune). Pour une fête à date fixe sans préface propre, la rubrique donne la préface de chaque temps où la date peut tomber (« Préface commune ; en Carême : préface du Carême »).
- **Graduel** : `getitem` (`GradualeP` au temps pascal, `Tractus` après la Septuagésime, `GradualeF` aux féries qui reprennent la messe du dimanche). Un graduel de Carême à plusieurs versets sans alléluia est intitulé Trait.
- **Fêtes à date fixe et Communs** : le formulaire principal est celui du temps où la date tombe le plus souvent (1900-2299) ; les autres temps possibles (au moins 3 % des années, hors Semaine sainte et octave de Pâques) donnent des variantes sous la pièce, si le texte change : « Au temps pascal : », « Après la Septuagésime : », « Hors du temps pascal : ». Les alléluias entre parenthèses de DO sont gardés au temps pascal, ôtés sinon. Les Communs sont donnés hors du temps pascal avec la variante de Septuagésime ; *Protexisti* et *Sancti tui* sont les communs des martyrs au temps pascal.
- **Commun et nom** : une pièce absente du formulaire vient du commun (`ex`/`vide` du `[Rank]`, `p` au temps pascal) ; `N.` est remplacé par le nom de `[Name]` (oraisons seulement, comme `oratio`). Trois oraisons de commémoraison gardent `N.` faute de `[Name]` dans DO : `Sancti/08-18r` (secrète), `Sancti/10-07t` (collecte, postcommunion).
- **Féries** : « Oratio Dominica » et `vide` : la messe du dimanche précédent, en entier (l'écran Aujourd'hui ouvre l'unité du jour). Les 259 féries de IVᵉ classe en sont des copies, soit un tiers du paquet.
- **Commémoraisons** : celles du calendrier de l'app qui ont leur clé (`Sancti/01-14cc`…) et celles écrites dans le formulaire (`[Commemoratio Oratio]`, filtrées comme `commemoratio` pour 1960 : octave de Noël, Sept Douleurs, sainte Anastasie) suivent la collecte, la secrète et la postcommunion. *Sub unica conclusione* (conversion de saint Paul) : la première conclusion tombe.
- **Quatre-Temps** : leçons, graduels et oraisons `LectioL1`… dans l'ordre de `LectionesTemporum`, avec *Flectámus génua* hors du temps pascal.
- **Semaine sainte réformée** : `[Rule] Full text` ; Vendredi saint et Vigile pascale donnés en entier (rubriques latines numérotées comprises), découpés aux titres `!!`. Jeudi saint : messe chrismale et messe vespérale.
- **Stations** : table de `build.py` (Avent, Noël, Septuagésime, Carême, Semaine sainte, octaves de Pâques et de la Pentecôte, Ascension, Quatre-Temps), rédigée par Paroissien d'après le Missel romain (« Statio ad… ») ; DO ne les donne pas.

## Corrections et normalisations

- Conclusions (`$Per Dominum`…) développées d'après `Ordo/Prayers.txt`, avec « ℟ Amen » ; `&Gloria` devient le *Glória Patri* (omis au temps de la Passion) ; `$Deo gratias`, `$Gloria tibi`, `$Laus tibi` sont omis (réponses de l'Ordinaire).
- Graphies fautives de DO ramenées : `$per Dominum`, `$Per Dominium`, `$Per Dominum ejusdem`, `$Per e`, `$Deo Gratias`, `&Glória`.
- `+` et `++` → ✠ ; `N.b`, `N.p` → `N.` ; entités HTML (`&Alpha;`, `&Omega;`, `&Psi;`) → lettres ; séparateurs `_ \`` ignorés.
- Alignement : un « Allelúja, allelúja. » isolé rejoint le verset qui précède ; « Orémus. » isolé rejoint le texte qui suit ; « ~ » devant un verset ℣ ne colle plus les deux lignes ; « Oratio super populum » (doublon du titre) est retiré, et l'oraison n'y est plus marquée ℣.
- Le latin garde la typographie de DO (accents, æ, œ, ǽ, pas d'espace avant « : » et « ; »). Le français de Paroissien suit la typographie française (’, espaces fines).

## Relire

- Les stations, et les préfaces dites « propres à certains lieux ».
- Sainte Geneviève (`sancti-01-03`) et la note de `sancti-05-30`.
- Les règles de DO gardées sans contrôle extérieur : pas de Gloria aux mercredi et samedi des Quatre-Temps de la Pentecôte (`Pasc7-3`, `Pasc7-6`), Gloria au lundi des Rogations, Credo à saint Laurent et à saint Barnabé, préface de l'Avent aux féries et aux saints de l'Avent (concession de France ; l'édition typique a la préface commune).
- Le seuil des variantes de temps (3 %).
