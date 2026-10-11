# 05 — Moteur de calendrier liturgique hors ligne

> Agent 05. Rapport de recherche et de planification, sans code applicatif. Rédigé le 2026-10-11.
>
> **Légende.** **[V]** : vérifié pendant cette recherche (lecture du code cloné, d'un texte officiel ou d'une réponse d'API, avec la source citée). **[S]** : supposé, déduit ou tiré de mes connaissances, à confirmer. **[?]** : inconnu.
>
> Remarque de méthode : le quota de recherche web partagé entre agents s'est épuisé en cours de route. Les constats reposent donc surtout sur la lecture directe du code des bibliothèques (`git clone --depth 1`), sur des textes officiels récupérés à des URL connues et sur des appels ponctuels à l'API AELF. Quelques points de droit français restent marqués [S] ou [?] faute d'avoir pu chercher la source primaire.

---

## 1. Résumé

- **Aucune bibliothèque existante ne couvre nos deux besoins à la fois**, à savoir le calendrier de 1962 et celui de 1969 avec le propre de France, pour Android. romcal (TypeScript, MIT) est la meilleure source pour 1969 et la France, avec 26 calendriers diocésains, mais il est en bêta et son paquet 1962 est vide. Missalemeum (Python, MIT) et Divinum Officium (Perl, MIT) font référence pour 1962, mais aucun des deux ne connaît le propre de France. **[V]**
- **Recommandation : un moteur natif en Kotlin pur** (module JVM sans dépendance Android) piloté par des **données versionnées** livrées dans le paquet de contenu. On écarte QuickJS/romcal, qui ne couvrirait que 1969, en bêta, et avec des préceptes faux pour la France. On écarte aussi le tout précalculé, qui ne résout ni le 1962 français ni les options utilisateur.
- **Les autres moteurs servent d'oracles et non de dépendances.** La chaîne de contenu (agent 11) génère avec eux des jeux de tests « golden » : romcal, LitCal, calendarium-romanum et AELF pour 1969 ; Missalemeum, Divinum Officium et le fork romcal1962 pour 1962. On examine leurs désaccords à la main et on les consigne.
- **Idée clé pour les tests** : la structure d'une année civile ne dépend que de la date de Pâques (35 valeurs possibles) et du caractère bissextile de l'année, soit **70 types d'année**. Divinum Officium indexe d'ailleurs ses tables de transfert ainsi. Balayer ces 70 types couvre exhaustivement les combinaisons de dates. **[V pour Divinum Officium, S pour l'exhaustivité]**
- **Pour la France, le droit en vigueur s'impose aux deux rites** : quatre fêtes d'obligation en plus des dimanches (Noël, Ascension, Assomption, Toussaint) ; Épiphanie et Saint-Sacrement reportés au dimanche, Ascension maintenue le jeudi ; jeûne le Mercredi des Cendres et le Vendredi saint, pénitence chaque vendredi. romcal marque à tort le 8 décembre, le 29 juin, etc. comme jours d'obligation pour la France. **[V]**
- **Couleurs** : blanc, rouge, vert, violet, rose et noir, plus l'or toléré, qui correspondent exactement aux six signets de tissu de la vision. Une journée peut avoir plusieurs couleurs : pour les défunts, violet ou noir en 1969 ; le Vendredi saint de 1962, noir puis violet à la communion. **[V]**
- **API proposée** : `LiturgicalCalendar.day(date)` renvoie un `LiturgicalDay` qui contient la célébration principale, les commémoraisons, le rang, la couleur, le temps, les cycles, le précepte, la pénitence, un `MassRef` (agent 03), un `ReadingsRef` (agent 04) et des `SaintId` (agent 01). Les identifiants sont des slugs stables, repris de romcal pour 1969 et alignés autant que possible entre les deux rites.
- **Interface** : vue mensuelle façon « Ordo », avec un ruban de couleur par jour ; page « fête du jour » ; bascule 1962/1969, avec en paysage une double page qui montre les deux rites en regard.
- **Principaux risques** : la casuistique des rubriques (commémoraisons de 1962, décisions ponctuelles du Dicastère, comme la Saint-Jean-Baptiste avancée au 23 juin 2022), l'absence d'un Ordo 1962 français lisible par machine, la licence des données AELF et du Martyrologe, et l'arrivée de nouveaux décrets (Newman, inscrit en 2025) qui impose de mettre les données à jour.

---

## 2. Constats détaillés

### 2.1 Exigences fonctionnelles, rite par rite

#### 2.1.1 Commun aux deux rites

| Élément | Règle | Statut |
|---|---|---|
| Comput pascal | Grégorien (algorithme de Meeus, Jones et Butcher). Table officielle de 1985 à 2050 dans *Notitiae* 210/1984, reprise par calendarium-romanum (`data/easter_dates.txt`). | [V] |
| Jour liturgique | De minuit à minuit. Les dimanches et solennités commencent la veille au soir (normes universelles, NU 3). | [V] |
| Couleurs | Rubriques de 1960 (n. 117-132) : cinq couleurs (blanc, rouge, vert, violet, noir), plus le rose à *Gaudete* et *Laetare*. En 1969 : le même ensemble, l'or étant toléré et le noir facultatif pour les défunts. | [V] pour 1960 ; [S] pour le détail de la PGMR |
| Fêtes d'obligation (France) | Dimanches, Ascension, Assomption, Toussaint, Noël. Le CIC 1983, c. 1246 §2, permet à la conférence des évêques d'en supprimer ou d'en reporter. | [V] (c. 1246 sur vatican.va, liste française d'après Wikipédia) |
| Report au dimanche | Épiphanie : dimanche entre le 2 et le 8 janvier. Saint-Sacrement : dimanche après la Trinité. Ascension : maintenue le jeudi en France. | [V] (NU 7 ; config `France` de romcal ; AELF place l'Épiphanie le 3 janvier 2027) |
| Pénitence (droit en vigueur) | Vendredis et Carême. Abstinence le vendredi sauf solennité. Jeûne et abstinence le Mercredi des Cendres et le Vendredi saint (c. 1250-1253). En France, la CEF permet de remplacer l'abstinence de viande par d'autres actes (alcool, tabac, prière, partage). | [V] pour le CIC ; [V] pour le glossaire CEF ; [?] pour le texte exact du décret complémentaire de la CEF |

**Le précepte et la pénitence relèvent du droit en vigueur, pas du missel choisi.** Un fidèle qui suit le rite de 1962 en France est tenu aux quatre fêtes d'obligation et au c. 1251 actuel. La discipline de 1917 (Quatre-Temps, vigiles jeûnées, etc.) ne peut s'afficher que comme une **surcouche traditionnelle facultative**. C'est une décision à prendre (§4). **[S, raisonnement canonique à valider par l'agent 12 ou l'utilisateur]**

#### 2.1.2 Rubriques de 1962 (*Rubricae generales* de 1960)

Source lue : traduction anglaise des rubriques générales de 1960, hébergée dans Divinum Officium (`web/www/horas/Help/Rubrics/General Rubrics.html`, sous CC BY-SA 2.5 d'après la page). **[V]**

- **Classes I à IV** ; table de préséance unique (n. 91) : par exemple 1° Noël, Pâques, Pentecôte ; 2° le Triduum ; 3° Épiphanie, Ascension, Trinité, Fête-Dieu, Sacré-Cœur, Christ-Roi ; 4° Immaculée Conception et Assomption…
- **Dimanches** : de 1re classe pour l'Avent, le Carême, le temps de la Passion, Pâques, *in Albis* et la Pentecôte ; tous les autres sont de 2e classe. L'Immaculée Conception l'emporte sur un dimanche de l'Avent (n. 15).
- **Féries** : 1re classe pour les Cendres et la Semaine sainte ; 2e classe du 17 au 23 décembre et pour les Quatre-Temps de l'Avent, du Carême et de septembre ; 3e classe pour le Carême et l'Avent jusqu'au 16 décembre ; 4e classe pour les autres, jamais commémorées (n. 23-26).
- **Vigiles** : de 1re classe pour Noël, qui remplace le 4e dimanche de l'Avent, et pour la Pentecôte. De 2e classe pour l'Ascension, l'Assomption, saint Jean-Baptiste et saints Pierre et Paul. De 3e classe pour saint Laurent. Une vigile de 2e ou 3e classe est omise un dimanche, une fête de 1re classe, ou quand sa fête est transférée (n. 28-33).
- **Octaves** : Noël (2e classe, le jour octave étant de 1re classe), Pâques et Pentecôte (1re classe), à l'exclusion de toutes les autres (n. 64-68).
- **Transferts** : seules les fêtes de 1re classe se transfèrent, au premier jour libre qui n'est ni de 1re ni de 2e classe. L'Annonciation reportée après Pâques va au lundi après *in Albis* ; les Défunts tombant un dimanche passent au lundi (n. 95-97). Les autres fêtes empêchées sont commémorées ou omises.
- **Commémoraisons** : n. 106-114. Le fork romcal1962 les plafonne à 3 aux messes chantées et à 1 aux messes basses.
- **Quatre-Temps de septembre** : mercredi, vendredi et samedi après le 3e dimanche de septembre (*Rubricarum instructum*, 1960). Missalemeum code exactement cette règle (`calc_ember_wednesday_september`). **[V]**
- **Couleurs** (n. 119-132) : la vigile de la Pentecôte est rouge ; le Vendredi saint est noir jusqu'à la communion, violet ensuite ; les Quatre-Temps de septembre et les vigiles de 2e et 3e classe hors temps pascal sont violets. **[V]**
- **Pièges connus**, tous gérés par Missalemeum, dont je n'ai pas exécuté les tests mais lu le code :
  - les dimanches après l'Épiphanie omis sont repris avant le dernier dimanche après la Pentecôte ;
  - saint Matthias passe du 24 au 25 février les années bissextiles ;
  - la vigile de Noël un dimanche ;
  - les trois messes de Noël et des Défunts ;
  - la Sainte Vierge le samedi ;
  - Notre-Dame des Sept Douleurs le vendredi de la Passion ;
  - le Christ-Roi au dernier dimanche d'octobre.

  **[V, lecture de `rules.py` et `factory.py`]**
- **Usages français relevés dans un Ordo traditionnel 2026** (source non officielle, dépôt `Francefidele/ordo-liturgique`, sans licence, consultable pour vérification uniquement) :
  - **solennités extérieures dominicales** : Épiphanie, sainte Jeanne d'Arc le 2e dimanche de mai, Saint-Sacrement, saints Pierre et Paul, sainte Thérèse le dernier dimanche de septembre, Rosaire le 1er dimanche d'octobre ;
  - **propres français** : sainte Thérèse le 3 octobre en 2e classe (patronne secondaire), sainte Geneviève le 3 janvier et saint Rémi le 1er octobre en 3e classe ;
  - **mentions** de jeûne, d'abstinence et d'obligation dans une colonne de notes.

  **[V pour le contenu de cette source, S pour sa représentativité]**

#### 2.1.3 Calendrier romain général d'après 1969

Normes universelles sur l'année liturgique (NU) et table des jours liturgiques, lues sur catholicculture.org. **[V]**

- **Table de préséance en 13 degrés** (le 4 et le 8 ont des sous-cas). On y trouve dans l'ordre : le Triduum ; Noël, l'Épiphanie, l'Ascension, la Pentecôte, les dimanches de l'Avent, du Carême et de Pâques, les Cendres, la Semaine sainte et l'octave de Pâques ; les solennités du calendrier général et les Défunts ; les solennités propres… jusqu'aux féries ordinaires (13). romcal reprend ces codes : `OptionalMemorial_12`, `ProperMemorial_SecondPatron_11a`, etc. **[V]**
- **Transferts** (NU 5 et 60) : une solennité qui tombe un dimanche de l'Avent, du Carême ou de Pâques passe au lundi. Une solennité empêchée passe au jour le plus proche hors degrés 1 à 8. L'Annonciation tombant en Semaine sainte passe au lundi après le 2e dimanche de Pâques. Les autres célébrations empêchées sont omises. **[V]**
- **Décisions ponctuelles du Dicastère**, confirmées par AELF : en 2022, la Nativité de saint Jean-Baptiste est avancée au 23 juin pour laisser le 24 au Sacré-Cœur ; en 2024, l'Immaculée Conception passe au lundi 9 décembre. **[V, API AELF]**
- **Le calendrier évolue dans le temps.** Marie, Mère de l'Église est inscrite depuis 2018 ; saint John Henry Newman est inscrit comme mémoire facultative au 9 octobre par décret du 9 novembre 2025 (Léon XIV). **[V, décret sur vatican.va]** LitCal modélise ces dates de validité (`since_year`, 222 occurrences) ; romcal ne le fait pas, Newman y apparaît pour toutes les années. **[V]**
- **Cycles**. Soit L l'année civile où **se termine** l'année liturgique, qui commence au 1er dimanche de l'Avent de L−1.
  - Dimanches : L mod 3 = 1 donne l'année A, 2 l'année B, 0 l'année C. AELF le confirme : A pour le 11 octobre 2026, B pour le 3 janvier 2027, C pour le 8 décembre 2024. **[V]**
  - Féries du temps ordinaire : année I si L est impair, II s'il est pair. **[S, règle standard que les tests golden confirmeront]**
  - Semaine du psautier : ((semaine − 1) mod 4) + 1 à l'intérieur de chaque temps. AELF donne la semaine IV au 28e dimanche du TO et la semaine II au 2e dimanche de l'Avent. **[V]**
- **Couleur des Défunts en France** : AELF renvoie `violet` pour le 2 novembre 2026. Le modèle doit donc admettre plusieurs couleurs possibles pour un même jour. **[V]**

### 2.2 Bibliothèques existantes

Code cloné et lu le 2026-10-11 dans le scratchpad ; métadonnées obtenues par `gh api`.

| | **romcal** | **LitCal** (J. Romano D'Orazio) | **Divinum Officium** | **Missalemeum** | **calendarium-romanum** | **romcal1962** (fork d'A. Bocken) |
|---|---|---|---|---|---|---|
| Langage | TypeScript | PHP (API serveur) | Perl, avec des données en texte INI | Python (FastAPI) | Ruby | TypeScript |
| Licence | MIT | Apache-2.0 | MIT (© 2026) | MIT | MIT ou LGPL-3.0 au choix | MIT |
| Activité | Push le 2026-10-11 ; ≥ 100 commits en 2026, dont beaucoup de Dependabot ; 139★ ; **v3.0.0-dev.140, en bêta** | Push le 2026-10-10 ; 108★ | Push le 2026-10-10 ; 470★ ; 277 forks | Push le 2026-10-07 ; 121★ | Push le 2026-02-08 ; 63★ | Créé en 12/2025, dernier push le 2026-04-17 ; 0★ ; un seul auteur |
| Rite 1962 | **Non** (`rites/roman1962/src/.gitkeep` vide) | Non (1969 et ambrosien) | **Oui** : versions 1570, 1888, 1906, Divino Afflatu, 1955, 1960, monastique (dont « 1963 – Barroux »), dominicain 1962 | **Oui** : 1960, messe seulement | Non | **Oui** : messe 1962 importée de Divinum Officium |
| Rite 1969 | **Oui** | **Oui**, avec exactitude historique | Non (« New Mass » partiel) | Non | **Oui** | Oui (reprend romcal) |
| France | **Oui** : propre national (Missel romain 2021) + **20 diocèses, 6 archidiocèses et le sanctuaire de Lourdes** ; config Épiphanie et Fête-Dieu au dimanche, Ascension le jeudi | **Non** (pays : CA, HR, IT, NL, US) | Non (pas de calendrier gallican ; textes de messe en `Francais`, 960 fichiers) | Non (calendriers anglais et polonais) | Non (universel en français, plus les diocèses tchèques) | Non (Suisse, Coire) |
| Précepte (France) | **Faux** : drapeaux du calendrier général non surchargés pour la France | n.d. | n.d. | n.d. | n.d. | n.d. |
| Couleurs | Champ structuré `colors[]` | Oui | **Déduites par regex du titre** ; la fonction renvoie « black » pour le blanc | Codées dans l'identifiant (`sancti:11-19:4:w`) | Oui | Oui |
| Cycles A/B/C et I/II | Oui (`sundayCycle`, `weekdayCycle`, `psalterWeek`) | Oui | n.a. | n.a. | Oui | n.a. |
| Références des lectures | Non | **Partielles** : jeu « lectionary » complet en anglais pour le dimanche année A, quasi vide en français | Dans les propres de messe (`[Lectio]`, `!Rom 13:11-14`) | Via les propres de Divinum Officium | Non | Via les propres |
| Tests | Jest : snapshots, `dates.test` (109 cas), tests par pays dont la France | PHPUnit, PHPStan niveau 10, **tests déclaratifs en JSON** (« exactCorrespondenceSince ») | Tests unitaires `t/` (dont `LiturgicalColor.t`) et `regress/`, qui compare les sorties entre commits | pytest : fixture des dates mobiles **2004-2050**, environ 166 cas paramétrés, calendrier complet 2023 | rspec et **regression dumps** par année | Jest, environ 11 suites |
| Volume utile | Cœur d'environ 3 900 lignes, 37 700 lignes au total avec les données ; France : 4 500 lignes | Environ 90 000 lignes, surtout l'infrastructure de l'API | `horascommon.pl` : 2 344 lignes, dont `occurrence` sur environ 820 lignes, mêlé à l'Office | **Environ 1 000 lignes de logique**, plus 1 244 lignes de constantes | Environ 3 650 lignes | n.m. |
| Portabilité vers Kotlin | Bonne : modèle propre, données déclaratives | Faible : serveur PHP | Faible : état global, couplage à l'Office | **Très bonne** : règles = fonctions ordonnées | Bonne | Moyenne |

Sources : voir §6. Sauf mention contraire, toutes les cases sont **[V]**.

**Bibliothèques Java/Kotlin repérées** (recherche sur GitHub, métadonnées seulement, code non audité) :

| Projet | Licence | Activité | Remarque | Utilisable ? |
|---|---|---|---|---|
| `gold-cal/liturgical-calendar` | GPL-3.0 | 0★, push 04/2026 | Application Android avec calendrier traditionnel | **Non**, sauf si l'application passe sous GPL (agent 12) |
| `vandenesse/calendrier-liturgique` | Aucune | 4★, dernier push 2020 | Calcul des fêtes et export iCal | **Non** : pas de licence, projet ancien |
| `martinjuskelis/divinum-officium-android` | Aucune | 0★, 03/2026 | Portage Android de Divinum Officium | **Non** : pas de licence |
| `misiek198910/LiturgicalCalendar-Kotlin` | Aucune | 0★ | Calendrier liturgique en Kotlin | **Non** : pas de licence |
| `paulbroyles/python-romcal` | Aucune | 0★ | Portage de romcal en Python | **Non** : pas de licence |
| `scriptura/liturgical-calendar` | Aucune | 1★ | Moteur Rust qui **précalcule 1969-2399** dans un binaire (157 746 entrées × 8 octets) | **Non**, mais intéressant comme modèle d'architecture |

**Conclusion : il n'existe aucune bibliothèque JVM réutilisable, mature et sous licence permissive.** **[V dans la limite de la recherche GitHub effectuée]**

**Moteurs JavaScript embarquables** (pour l'option QuickJS) :

| Projet | Licence | Activité |
|---|---|---|
| `dokar3/quickjs-kt` | Apache-2.0 | 154★, actif |
| `HarlonWang/quickjs-wrapper` | Apache-2.0 | 274★, dernier push 07/2025 |
| `cashapp/zipline` | Apache-2.0 | Conçu pour Kotlin/JS, pas pour des paquets npm arbitraires |
| Mozilla Rhino | MPL-2.0 [S] | JVM pur, ES6 partiel |

Le surcoût natif serait d'environ 1 Mo par ABI. **[S]**

### 2.3 Qualité des oracles disponibles pour les tests

| Oracle | Couverture | Accès | Limites |
|---|---|---|---|
| **AELF** (`api.aelf.org/v1/informations/{date}/france`) | 1969, **zone France** (Jeanne d'Arc, saint Rémi…) ; donne couleur, année A/B/C, semaine, degré | Disponible environ de **2018 à mi-2027** (404 pour 2015, 2027-09 et 2028) | Libellés parfois faux : saint Joseph est étiqueté « Solennité de la Vierge Marie » ; mémoires rangées dans `fete` avec `jour_liturgique_nom` à « de la férie ». **Conditions d'utilisation de l'API introuvables [?]** |
| romcal France | 1969, pays et diocèses, perpétuel | npm, à exécuter dans la chaîne de contenu | Pas de validité par année ; précepte faux pour la France ; bêta |
| LitCal | 1969 général, historiquement exact | API publique et code | Pas de France |
| calendarium-romanum | 1969 universel | Gem Ruby, regression dumps | Pas de France |
| Missalemeum | 1962 général, 2004-2050 testé | API publique et code | Pas de France ; couplé au polonais |
| Divinum Officium | 1962 général et autres versions | Site et code Perl | Couleurs heuristiques ; pas de France |
| romcal1962 | 1962 général | Code | Jeune, un seul auteur |
| Ordo papier ou PDF (FSSP, FSSPX « La Porte Latine », Le Barroux, CEF) | 1962 France, 1969 France | Manuel | Pas lisible par machine ; variations entre communautés [S] |

### 2.4 Stratégie d'implémentation : comparaison

| Critère | A. romcal via QuickJS | B. Tables précalculées par la chaîne | **C. Moteur Kotlin natif + données** |
|---|---|---|---|
| Couverture 1969 France | Oui, mais précepte à corriger | Oui (si générées avec romcal) | Oui (données reprises de romcal) |
| Couverture 1962 France | **Non** (rien en amont) | **Non** : aucun moteur ne sait la générer | Oui, à écrire nous-mêmes |
| Options utilisateur (diocèse, solennités extérieures, pénitence, messe anticipée) | Partielles | Explosion combinatoire | Natives |
| Taille dans l'APK | ~1-2 Mo natifs par ABI + bundle JS [S] | ~1-5 Mo pour 100 ans × 2 rites [S] (scriptura : 1,26 Mo pour 431 ans) | ~100-300 Ko de données + code [S] |
| Mises à jour (nouveaux décrets) | Mise à jour de l'APK ou du bundle | Régénération du paquet | Mise à jour du **paquet de données** seulement |
| Testabilité sur la VM | Difficile (JNI) | Triviale | Tests JVM purs, idéalement en CI |
| Dette | Deux langages, romcal en bêta | Dépend de Node, Python et Perl dans la chaîne | Code à maintenir, environ 3-5 k lignes [S] |

**Le point décisif : le 1962 français n'existe nulle part.** Il faut donc écrire un moteur, et autant que ce soit le nôtre, pour les deux rites, avec les autres moteurs pour le vérifier.

### 2.5 La propriété des 70 types d'année

Pâques tombe toujours un dimanche, entre le 22 mars et le 25 avril (35 dates possibles). Le jour de la semaine de chaque date de l'année civile ne dépend donc que de la date de Pâques et du caractère bissextile de l'année : il y a **70 types d'année**. Divinum Officium exploite déjà cette propriété : `Tabulae/Transfer/322.txt` à `425.txt` sont indexés par date de Pâques, `a.txt` à `g.txt` par lettre dominicale. **[V]**

Deux dimensions s'y ajoutent, indépendantes de la structure des dates : les cycles A/B/C et I/II (6 combinaisons) et la version du droit (`since_year`).

→ Une suite de tests qui balaie les 70 types pour chaque rite et chaque calendrier particulier couvre toutes les configurations de dates. **[S, raisonnement]**

### 2.6 Points d'interface avec les autres agents

| Agent | Ce que le calendrier fournit | Ce que le calendrier attend |
|---|---|---|
| 01 Clairval | `SaintId` stables pour le saint principal, les commémoraisons et les saints du jour | Une table lettre → `SaintId`. **Correspondance par saint, pas par date** : saint Thomas d'Aquin tombe le 7 mars en 1962 et le 28 janvier en 1969. [V pour les dates de romcal et de Divinum Officium, S pour l'exemple précis] |
| 03 Missel | `MassRef` : propre, commun, préface, Gloria et Credo [à arbitrer], **liste ordonnée des commémoraisons** (oraisons à ajouter en 1962) | Les identifiants des propres du missel, à aligner sur nos `CelebrationId` |
| 04 Bible / Évangile du jour | `ReadingsRef` : clé de lectionnaire et cycle (1969), renvoi au propre (1962) | Une table clé → références, fournie par la chaîne de contenu |
| 06 Moteur de lecture | En-tête « jour liturgique » des pages ; couleur pour les filets et les lettrines | — |
| 07 Tourne-page, 09 Signets | `LiturgicalColor.bookmark` (or → blanc) ; « signet du jour » | Une palette de rubans |
| 08 UX | Données des vues mois et jour, bascule de rite | Maquettes |
| 10 Architecture | Module `:core:liturgy` en Kotlin pur, sans Android ; un `Flow<LiturgicalDay>` « aujourd'hui » qui se met à jour à minuit | Injection de dépendances, chargement du paquet de données, CI pour les tests JVM |
| 11 Chaîne de contenu | Schéma des données du calendrier, format des fichiers golden | Génération des données (romcal → JSON, Divinum Officium et romcal1962 → JSON, saisie manuelle du 1962 français), golden, table du lectionnaire |
| 12 Juridique | Liste des sources réutilisées et de leurs licences | Avis sur la licence MIT et l'attribution, l'API AELF, le Martyrologe, la GPL |
| 13 Benchmark | — | Fonctions calendaires des applications concurrentes |
| 14 Prières et extensions | Semaine du psautier, temps liturgique (Office à venir) | Besoins de la concurrence des vêpres, hors MVP |

---

## 3. Recommandations

### 3.1 MVP

1. **Créer un module `:core:liturgy` en Kotlin pur** (JVM, sans Android). Choix entre `java.time` et `kotlinx-datetime` laissé à l'agent 10 ; le second faciliterait un éventuel passage multiplateforme. Pipeline interne :
   - `Computus` ;
   - `TemporalBuilder1969` et `TemporalBuilder1962` ;
   - `SanctoralResolver`, en couches général → national → diocésain, avec dates de validité ;
   - `PrecedenceResolver1969` (table des NU, NU 5 et 60, notifications) et `OccurrenceResolver1962` (rubriques n. 91-114, vigiles, octaves, samedi marial, solennités extérieures) ;
   - `ColorResolver`, `CycleResolver`, `ObligationResolver` et `PenanceResolver` ;
   - `YearOverrides` pour les décisions ponctuelles du Dicastère.

   On calcule une année entière à la fois, parce que les transferts doivent regarder en avant, et on garde quelques années en mémoire.
2. **Données déclaratives dans le paquet de contenu** :
   - fichiers prévus : `calendar/1969/general.json`, `calendar/1969/fr.json`, `calendar/1962/general.json`, `calendar/1962/fr.json`, plus les noms en français et en latin ;
   - règles de date sous forme de petit DSL : `"10-09"`, `{easter:+49}`, `{sundayBetween:["01-02","01-08"]}`, `{lastSundayOf:10}`, `{weekdayAfter:{WED, nthSundayOf:[3,9]}}` ;
   - champs `validFrom` et `validUntil`, obligation par région, couleurs, `saints[]`, `massProperId`, `lectionaryKey` ;
   - versionnage (`dataVersion`, dernier décret pris en compte) affiché dans « À propos ».
3. **Sources des données**, toutes sous licence MIT, avec attribution :
   - 1969 : conversion des définitions romcal (général et France), avec correction locale du précepte et ajout de `validFrom` d'après LitCal (Apache-2.0) ;
   - 1962 : Divinum Officium (`Kalendaria/1960.txt` et propres) ou les slugs de romcal1962 ;
   - **1962 français** : saisie manuelle d'une vingtaine d'entrées et des solennités extérieures, à partir d'un Ordo de référence (décision §4).
4. **Périmètre du MVP** :
   - 1969 avec le calendrier général et la France (sans diocèses) ;
   - 1962 avec le calendrier général et le propre de France, solennités extérieures en option ;
   - couleurs, cycles, précepte et pénitence selon le droit en vigueur ;
   - commémoraisons de 1962 ordonnées ;
   - identifiants pour le missel, les lectures et les saints.
5. **Tests**, à lancer **en CI** et pas sur la VM :
   - **G0** : comput, comparé à la table de *Notitiae* 1985-2050 et à un second algorithme sur 1583-4099 ;
   - **G1** : invariants vérifiés sur des années aléatoires de 1970 à 2400 (une seule célébration principale, aucun dimanche privilégié déplacé, couleurs dans l'ensemble autorisé, 12 jours de Quatre-Temps par an en 1962…) ;
   - **G2** : balayage des **70 types d'année** pour chaque rite ;
   - **G3** : fichiers golden JSONL produits par la chaîne de contenu et comparés champ par champ (identifiant, rang, couleurs, commémoraisons, cycle, précepte, jeûne, abstinence) :
     - 1969 France : romcal (2026-2060) et AELF (2018-2027, si l'autorisation est obtenue) ;
     - 1969 général : LitCal et calendarium-romanum (1970-2100) ;
     - 1962 : Missalemeum (2004-2050), romcal1962 et Divinum Officium ;
   - **G4** : cas de rubriques écrits à la main, chacun avec sa source :
     - 23 juin 2022, 9 décembre 2024 ;
     - Annonciation en Semaine sainte ;
     - saint Matthias les années bissextiles ;
     - Défunts un dimanche (1962) ;
     - vigile de Noël un dimanche ;
     - dimanches après l'Épiphanie repris ;
     - Épiphanie le 7 ou le 8 janvier → Baptême le lundi [S] ;
     - Newman présent à partir de 2026 seulement.
   - **`known-divergences.md`** : chaque désaccord entre oracles y est justifié, jamais accepté en silence.
6. **API publique**, détaillée au §3.4.
7. **Interface MVP**, détaillée au §3.5 : vue mensuelle, page du jour et bascule de rite.

### 3.2 V1

- Les 26 diocèses de France de romcal, au choix dans les réglages, et des propres diocésains de 1962 à la demande.
- La surcouche « discipline traditionnelle » de jeûne et d'abstinence pour 1962 (CIC 1917, can. 1252 [S]), clairement étiquetée comme facultative.
- Les Rogations et les Quatre-Temps dans le calendrier de 1969, quand un diocèse les fixe (NU 45-47 [S]).
- Une table de lectionnaire 1969 complète, alimentée par la chaîne de contenu (avec l'agent 04), et l'« Évangile du jour » dans les deux rites.
- Un catalogue des saints : métadonnées « martyrology » de romcal (MIT), plus les entrées propres à Clairval.
- Un widget « fête du jour » sur l'écran d'accueil, et des rappels locaux des fêtes d'obligation (sans réseau).
- Une option « messe anticipée » : à partir d'une heure réglable le samedi soir, afficher le dimanche.
- Un export iCal local, si l'agent 08 le juge utile.

### 3.3 Plus tard

- L'Office : premières vêpres, concurrence (rubriques de 1960, n. 103-105) et psautier (avec l'agent 14).
- D'autres calendriers : monastique 1963 du Barroux, déjà présent dans Divinum Officium ; dominicain ; ambrosien (LitCal) ; d'autres pays et d'autres langues.
- Les rubriques antérieures à 1962 (1954 et la Semaine sainte d'avant 1955).
- Le Martyrologe du jour, selon ce que permettent les droits (agent 12).
- Une ouverture multiplateforme (iOS) du module, si `kotlinx-datetime` a été retenu.

### 3.4 API Kotlin proposée

Ce sont des signatures de conception, pas du code applicatif.

```kotlin
// ── Module :core:liturgy (Kotlin pur) ────────────────────────────────

enum class Rite { ROMAN_1962, ROMAN_1969 }

/** Calendrier particulier : "fr", "fr/paris", "fr/lyon"… (hiérarchie général → national → diocésain). */
@JvmInline value class CalendarId(val value: String)
@JvmInline value class CelebrationId(val value: String)   // slug stable : "john_henry_newman_priest"
@JvmInline value class SaintId(val value: String)

data class CalendarConfig(
    val rite: Rite,
    val calendar: CalendarId = CalendarId("fr"),
    val externalSolemnities: Boolean = true,              // 1962 : solennités extérieures dominicales (France)
    val penanceDisplay: PenanceDisplay = PenanceDisplay.LAW_IN_FORCE,
    val anticipateSundayFrom: LocalTime? = null,           // V1 : messe anticipée du samedi soir
)

enum class PenanceDisplay { LAW_IN_FORCE, LAW_IN_FORCE_PLUS_TRADITIONAL }

interface LiturgicalCalendar {
    val config: CalendarConfig
    val dataVersion: DataVersion                            // version du paquet + dernier décret intégré
    fun day(date: LocalDate): LiturgicalDay
    fun range(from: LocalDate, toInclusive: LocalDate): List<LiturgicalDay>
    fun month(month: YearMonth): List<LiturgicalDay>
    fun occurrences(id: CelebrationId, year: Int): List<LocalDate>   // « Quand tombe la Saint-Joseph en 2030 ? »
}

interface LiturgicalCalendars {                            // point d'entrée injecté (agent 10)
    fun calendar(config: CalendarConfig): LiturgicalCalendar
    fun day(date: LocalDate, rite: Rite): LiturgicalDay     // raccourci demandé : LiturgicalDay(date, rite)
    fun today(rite: Rite): Flow<LiturgicalDay>              // se met à jour à minuit, heure locale
}

data class LiturgicalDay(
    val date: LocalDate,
    val rite: Rite,
    val calendar: CalendarId,
    val season: Season,
    val week: Int?,                                         // « 28e semaine du TO », « 3e dimanche après la Pentecôte »
    val principal: Celebration,
    val commemorations: List<Celebration>,                  // 1962 : dans l'ordre des oraisons
    val optionalCelebrations: List<Celebration>,            // 1969 : mémoires facultatives au choix du célébrant
    val colors: LiturgicalColors,
    val cycles: Cycles?,                                    // 1969 uniquement
    val obligation: Obligation?,                            // null s'il n'y a pas de précepte
    val penance: Penance,
    val flags: Set<DayFlag>,
)

data class Celebration(
    val id: CelebrationId,
    val title: LocalizedText,                               // fr, la
    val kind: CelebrationKind,
    val rank: Rank,
    val precedence: Int,                                    // degré dans la table du rite (1..13 ou 1..n pour 1962)
    val source: CelebrationSource,                          // TEMPORAL, GENERAL, NATIONAL, DIOCESAN, OVERRIDE
    val colors: LiturgicalColors,
    val transferredFrom: LocalDate? = null,
    val saints: List<SaintId> = emptyList(),
    val mass: MassRef,
    val readings: ReadingsRef,
)

sealed interface Rank {
    enum class R1962 : Rank { CLASS_I, CLASS_II, CLASS_III, CLASS_IV, COMMEMORATION }
    enum class R1969 : Rank { SOLEMNITY, FEAST, MEMORIAL, OPTIONAL_MEMORIAL, SUNDAY, WEEKDAY }
}

enum class CelebrationKind { SUNDAY, FERIA, FEAST, VIGIL, OCTAVE_DAY, EMBER_DAY, ALL_SOULS, EXTERNAL_SOLEMNITY }

enum class Season {
    ADVENT, CHRISTMAS, LENT, PASCHAL_TRIDUUM, EASTER, ORDINARY_TIME,          // 1969
    TIME_AFTER_EPIPHANY, SEPTUAGESIMA, PASSIONTIDE, TIME_AFTER_PENTECOST,     // 1962, en plus d'ADVENT, CHRISTMAS, LENT et EASTER
}

enum class LiturgicalColor { WHITE, RED, GREEN, VIOLET, ROSE, BLACK, GOLD }
data class LiturgicalColors(val primary: LiturgicalColor, val alternatives: List<LiturgicalColor> = emptyList())
enum class BookmarkColor { GREEN, RED, VIOLET, WHITE, ROSE, BLACK }        // les six signets de la vision
val LiturgicalColor.bookmark: BookmarkColor get() = TODO("GOLD → WHITE, sinon identité")

enum class SundayCycle { A, B, C }
enum class WeekdayCycle { I, II }
data class Cycles(val sunday: SundayCycle, val weekday: WeekdayCycle, val psalterWeek: Int)

data class Obligation(val basis: String /* "CIC 1246 + CEF" */)
data class Penance(
    val fast: Boolean,
    val abstinence: Boolean,
    val substitutable: Boolean,                             // France : abstinence remplaçable (décision CEF)
    val traditional: TraditionalPenance? = null,            // surcouche 1962 facultative (V1)
)

enum class DayFlag { EMBER_DAY, ROGATION, VIGIL, OCTAVE, TRANSFERRED, PRIVILEGED, HOLY_WEEK, GAUDETE_LAETARE }

// ── Interfaces avec les autres modules ───────────────────────────────

/** Pour le missel (agent 03). Les identifiants de propres sont fixés en commun. */
data class MassRef(
    val properId: String,                                   // ex. "1962:sancti/10-09" ou "1969:ot28_sunday"
    val commonId: String? = null,
    val prefaceId: String? = null,
    val gloria: Boolean? = null,                            // null = au missel de décider (à arbitrer avec l'agent 03)
    val credo: Boolean? = null,
)

/** Pour la Bible et l'Évangile du jour (agent 04). La résolution clé → références vient du paquet de contenu. */
data class ReadingsRef(
    val lectionaryKey: String?,                             // 1969 : ex. "TO28-DIM", résolue avec Cycles
    val alternativeKeys: List<String> = emptyList(),        // mémoire à lectures propres, messe du jour…
    val fromProper: Boolean = false,                        // 1962 : épître et évangile pris dans MassRef.properId
)

interface LectionaryRepository { fun resolve(ref: ReadingsRef, cycles: Cycles?): Readings? }     // agents 04 et 11
interface SaintCatalog {                                                                     // agents 01 et 11
    fun saint(id: SaintId): Saint?
    fun saintsOf(day: LiturgicalDay): List<Saint>           // célébrés, commémorés, puis « du martyrologe »
}
```

Principes :
- les identifiants sont **les mêmes d'un rite à l'autre** chaque fois que la fête est la même (romcal1962 en réutilise déjà dix) ;
- le noyau reste **sans état et pur** : même entrée, même sortie ;
- les **libellés sont séparés des identifiants**, pour que les tests golden ne comparent que des identifiants.

### 3.5 Interface (avec l'agent 08)

**Vue mensuelle « Ordo »**
- Une grille de 7 colonnes. Chaque case porte en haut un **petit ruban de tissu** à la couleur du jour, en rappel des signets, et une rayure discrète quand plusieurs couleurs sont possibles.
- Dans la case, la date et le titre en petites capitales tronqué. Le rang se lit à la typographie :
  - 1969 : solennité en rouge et en capitales ; fête en gras ; mémoire en romain ; mémoire facultative en italique ;
  - 1962 : chiffres I à IV en exposant.
- Des marques sobres dans le style d'un Ordo imprimé : ✠ pour l'obligation [S, marque à choisir], **J** pour le jeûne, **A** pour l'abstinence [S, usage des Ordos français à vérifier].
- En paysage, la double page montre le mois à gauche et la page du jour sélectionné à droite.

**Page « fête du jour »**
- Un bandeau à la couleur liturgique.
- Le titre avec une lettrine ; le rang en toutes lettres (« Mémoire facultative », « Fête de IIIe classe ») ; le temps et la semaine ; les cycles (« Année A, semaine II du psautier »).
- Les commémoraisons et les mémoires facultatives possibles.
- Le précepte et la pénitence, avec un lien vers l'explication.
- Des actions : *Ouvrir la messe* (03), *Lectures et Évangile* (04), *Lettre de Clairval* (01, si une correspondance existe), *Tirer le signet du jour* (09).

**Bascule de rite**
- Un sélecteur segmenté « 1962 | 1969 » dans l'en-tête des vues calendrier et jour, avec un rite par défaut dans les réglages.
- Un appui long ou le paysage ouvre une **double page comparée** : 1962 à gauche, 1969 à droite. Elle montre pourquoi le même jour diffère, par exemple le Christ-Roi fin octobre en 1962 et à la fin de l'année liturgique en 1969.

**Accessibilité** : la couleur n'est jamais le seul signal. Le nom de la couleur s'affiche en texte, des motifs servent pour le daltonisme et les contrastes sont adaptés aux thèmes clair, sombre et sépia.

---

## 4. Décisions à soumettre à l'utilisateur

1. **Stratégie du moteur.** Je recommande le Kotlin natif avec des données dans le paquet de contenu, contre romcal sous QuickJS et contre des tables précalculées. *Recommandation : Kotlin natif, les autres moteurs servant d'oracles de test.*
2. **Rite par défaut et affichage.** Faut-il calculer et montrer les deux rites en permanence, ou un seul avec une bascule ? *Recommandation : un rite par défaut, la bascule, et la double page comparée en paysage.*
3. **Ordo de référence pour le 1962 français** : FSSP (district de France), FSSPX (La Porte Latine), un monastère (Le Barroux ou Fontgombault) ou un ordo diocésain. Leurs usages peuvent différer, par exemple sur les solennités extérieures. **Sans ce choix, on ne peut pas trancher les cas litigieux.**
4. **Solennités extérieures en 1962 (France)** : activées par défaut ou non ?
5. **Jeûne et abstinence.** Trois options : le droit en vigueur seul (CIC 1983 et CEF), le droit en vigueur plus la discipline traditionnelle de 1962 marquée comme facultative, ou la discipline traditionnelle seule pour le rite de 1962. *Recommandation : droit en vigueur par défaut, surcouche traditionnelle en option.*
6. **Diocèses dans le MVP** : France seulement, ou aussi les 26 diocèses de romcal ? *Recommandation : France seulement, diocèses en V1.*
7. **Plage d'années garantie** : par exemple 1970-2100 garantie par les tests, au-delà calculé sans garantie. Faut-il pouvoir remonter dans le passé pour dater les lettres de Clairval anciennes ?
8. **Version de 1962** : 1962 « pur », ou avec les ajouts permis depuis, comme les saints canonisés depuis et les nouvelles préfaces. Je pense aux décrets *Cum sanctissima* et *Quo magis* de 2020 [S, non vérifiés ici]. À trancher aussi avec l'agent 03.
9. **Utiliser l'API AELF comme oracle de test**, en lui demandant l'autorisation (agent 12).
10. **Réutiliser les données de romcal et de Divinum Officium** (MIT) avec attribution dans « À propos », et **exclure tout code GPL**, sauf si l'application devient elle-même GPL (agent 12).
11. **Messe anticipée du samedi soir** : faut-il afficher le dimanche à partir d'une heure donnée, et laquelle ?
12. **Langue des intitulés** : français seul, ou français avec le latin en regard ? Le latin est disponible dans romcal (`la`) et dans Divinum Officium.

---

## 5. Risques et inconnues

| # | Risque ou inconnue | Gravité | Atténuation |
|---|---|---|---|
| R1 | **Casuistique des rubriques** : commémoraisons de 1962, conflits de solennités, mémoires obligatoires en Carême | Élevée | Tests G1 à G4, plusieurs oracles, journal des divergences, règles écrites comme une liste ordonnée de fonctions (modèle de Missalemeum) |
| R2 | **Décisions ponctuelles du Dicastère** (2022, 2024…) que les règles générales ne prévoient pas | Moyenne | Fichier `YearOverrides` dans le paquet de données ; veille sur les décrets |
| R3 | **Aucun Ordo 1962 français lisible par machine** ; différences entre communautés | Élevée | Choisir un Ordo de référence (§4.3), saisir à la main, faire relire l'année par un connaisseur |
| R4 | **Données périmées hors ligne** : un utilisateur sans mise à jour du paquet ne verra pas un nouveau saint (Newman, 2025) | Moyenne | Afficher `dataVersion` ; mise à jour du paquet découplée de celle de l'APK (agents 10 et 11) |
| R5 | **Licences** : conditions de l'API AELF [?] ; textes et traductions de Divinum Officium (licence MIT du code, mais statut des sources [?]) ; Martyrologe romain de 2004 protégé [S] ; projets sans licence (Francefidele, scriptura, vandenesse) inutilisables | Moyenne | Avis de l'agent 12 ; ne livrer que des faits (dates, rangs) ou des données sous licence permissive |
| R6 | **romcal en bêta** (3.0.0-dev.140) : changements cassants, précepte faux pour la France, pas de validité par année | Faible, car romcal ne sert que d'oracle et de source | Figer la version dans la chaîne ; surcharges locales |
| R7 | **Oracles imparfaits** : libellés AELF incohérents, couleurs « black » de Divinum Officium pour le blanc, horizon AELF limité (2018-2027) | Moyenne | Comparer les identifiants et les champs normalisés, jamais les libellés |
| R8 | **Droit de la pénitence en France** : texte exact du décret complémentaire de la CEF sur le c. 1253 [?] ; abstinence le vendredi dans l'octave de Pâques [?] ; Quatre-Temps et Rogations en France après 1969 [?] | Moyenne | Sourcer auprès de la CEF ; afficher une note explicative plutôt qu'une règle trop affirmative |
| R9 | **Charge sur la VM** (2 cœurs) : les tests JVM sont légers, mais Gradle reste lourd | Faible | Lancer les tests en CI (GitHub Actions) ; localement, un seul module à la fois |
| R10 | **Effort sous-estimé** pour le moteur de 1962 [S : environ 2 à 4 semaines-homme assistées, contre 1,5 à 3 pour 1969] | Moyenne | Livrer 1969 en premier si besoin, puis 1962 ; s'appuyer sur la structure de Missalemeum, environ 1 000 lignes Python |
| R11 | **Frontière du jour** : minuit local, fuseaux horaires des voyageurs, messe anticipée | Faible | Date locale de l'appareil ; option « messe anticipée » en V1 |
| R12 | **Modèle « plusieurs couleurs possibles »** mal compris par l'interface des signets | Faible | `LiturgicalColors(primary, alternatives)`, et le signet du jour suit `primary` |

---

## 6. Sources

**Bibliothèques et code** (clonés ou lus le 2026-10-11)
- romcal : https://github.com/romcal/romcal (branche `dev`) : `rites/roman1969/src/calendars/countries/france/index.ts`, `.../general-roman/index.ts` (Newman, l. 1327), `rites/roman1962/` (vide), `docs/CHANGELOG.md`.
- LitCal : https://github.com/Liturgical-Calendar/LiturgicalCalendarAPI (branche `development`) : `jsondata/sourcedata/rite/roman/{calendars,lectionary,decrees}`, `jsondata/tests/roman/*.json`. Site : https://litcal.johnromanodorazio.com/
- Divinum Officium : https://github.com/DivinumOfficium/divinum-officium (branche `master`) : `web/www/Tabulae/{data.txt,Kalendaria,Transfer}`, `web/cgi-bin/horas/horascommon.pl`, `t/DivinumOfficium/LiturgicalColor.t`, `regress/README.md`, `web/www/horas/Help/Rubrics/General Rubrics.html` (rubriques générales de 1960, traduction anglaise de Seasoltz 1966, CC BY-SA 2.5 selon la page).
- Missalemeum : https://github.com/mmolenda/Missalemeum (branche `main`) : `backend/api/kalendar/{rules,factory,models}.py`, `backend/api/constants/common.py`, `backend/tests/test_calendar.py`, `backend/tests/fixtures/tempora.json`.
- calendarium-romanum : https://github.com/igneus/calendarium-romanum : `data/`, `liturgical_law/`, `spec/regression_dumps/`, `data/easter_dates.txt` (*Notitiae* 210/1984).
- romcal1962 : https://github.com/AlexBocken/romcal1962 : `docs/1962/README.md`, `docs/1962/02-findings-divinum-officium.md`, `rites/roman1962/README.md`.
- Autres projets repérés, non audités : https://github.com/gold-cal/liturgical-calendar, https://github.com/scriptura/liturgical-calendar, https://github.com/vandenesse/calendrier-liturgique, https://github.com/martinjuskelis/divinum-officium-android, https://github.com/ecclesia-dev/liturgical-calendar, https://github.com/Francefidele/ordo-liturgique
- Moteurs JavaScript : https://github.com/dokar3/quickjs-kt, https://github.com/HarlonWang/quickjs-wrapper, https://github.com/cashapp/zipline, https://github.com/mozilla/rhino

**Textes normatifs et officiels**
- Normes universelles sur l'année liturgique et le calendrier, avec la table des jours liturgiques : https://www.catholicculture.org/culture/library/view.cfm?id=10842
- Code de droit canonique (1983), c. 1244-1253 : https://www.vatican.va/archive/cod-iuris-canonici/eng/documents/cic_lib4-cann1244-1253_en.html
- Décret d'inscription de saint John Henry Newman au calendrier romain général (9 novembre 2025) : https://www.vatican.va/content/romancuria/en/dicasteri/dicastero-culto-divino-e-disciplina-sacramenti/documenti/20251109-decreto-iscrizione-newman.html
- CEF, glossaire : https://eglise.catholique.fr/glossaire/abstinence/ et https://eglise.catholique.fr/glossaire/jeune/
- Wikipédia, « Fête d'obligation » (liste française ; source secondaire) : https://fr.wikipedia.org/wiki/F%C3%AAte_d%27obligation

**Données d'oracle**
- API AELF, informations liturgiques (zone `france`) : `https://api.aelf.org/v1/informations/{AAAA-MM-JJ}/france`. Dates interrogées : 2022-06-23/24, 2024-12-08/09, 2026-02-20, 2026-04-03, 2026-05-25, 2026-05-30, 2026-10-11, 2026-11-02, 2026-12-08, 2027-01-03, 2027-01-15, 2027-03-19/20. Horizon sondé : de 2018 à mi-2027.
