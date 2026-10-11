# 12 — Juridique : droits, licences, approbation ecclésiastique et distribution

> **Avertissement.** Ce document est une analyse de risque argumentée, rédigée par un agent de planification qui n'est pas avocat. Ce n'est pas un avis juridique. Avant une diffusion publique (scénario C), une relecture par un juriste en propriété intellectuelle est recommandée.
>
> **Date de vérification :** 11 octobre 2026. Le droit applicable retenu est le droit français et celui de l'UE, puisque l'utilisateur est en France.
>
> **Légende des niveaux de preuve**
> - **[V]** : vérifié en ligne ce jour sur la source citée (texte de loi, page officielle, CGU, dépôt).
> - **[C]** : connu, mais non revérifié en ligne. Le budget de recherche web partagé entre les agents a été épuisé en cours de mission ; ces points sont à revérifier avant toute décision engageante.
> - **[S]** : supposition ou raisonnement de l'agent, à confirmer.

---

## 1. Résumé

- **Les lettres de Clairval sont protégées et leur republication exige une autorisation.** Chaque lettre en ligne se termine par une mention indiquant que, pour la publier dans une revue, un journal ou sur un site, « une autorisation est nécessaire » ; la demande se fait par la page contact [V]. Le site n'a pas de CGU [V]. Les lettres en ligne remontent à 1996 [V] : elles resteront protégées au moins jusqu'en 2067. **Il faut demander une autorisation écrite** ; un projet de courriel figure en annexe A.
- **« Récupérer tout le site » ne s'appuie sur aucune exception pour une diffusion.** La copie privée (art. L122-5 2° CPI) couvre l'usage personnel des lettres, mais elle exclut les copies de bases de données électroniques [V]. Le droit *sui generis* ne prévoit pas d'exception d'extraction privée pour une base électronique (L342-3 2°) [V]. La fouille de textes et de données (L122-5-3) ne vise que l'*analyse*, et les copies doivent être détruites ensuite [V]. Elle ne couvre ni la lecture ni la redistribution.
- **Divinum Officium est sous licence MIT depuis le 28 août 2026** [V]. Le latin est utilisable. En revanche, les crédits du projet indiquent que **les traductions françaises du propre sont l'œuvre et la propriété de l'abbaye Sainte-Madeleine du Barroux** [V]. La licence MIT ne peut pas les « libérer » : ces traductions ne sont pas utilisables dans une version publique sans l'accord du Barroux.
- **Les textes AELF (forme ordinaire, Bible liturgique, Liturgie des Heures) sont réservés à un usage personnel.** Les CGU interdisent toute rediffusion sans autorisation préalable et expresse [V]. L'API publique ne publie aucune condition [V]. L'application officielle « AELF – Lectures du jour » fonctionne **avec l'autorisation de l'AELF** [V]. L'ICEL (textes anglais) exige une permission et des redevances en cas de vente [V].
- **Pour un auteur français, la règle pratique est la suivante : au 1er janvier 2026, une œuvre est dans le domaine public si son auteur est mort en 1955 ou avant.** Une œuvre collective ou anonyme l'est si elle a été publiée en 1955 ou avant (L123-1, L123-3) [V]. Les prorogations de guerre sont absorbées par la durée de 70 ans (Cass. 1re civ., 27 févr. 2007), sauf pour les auteurs « morts pour la France » et, peut-être, pour la musique [V, source secondaire]. **Dom Lefebvre est mort en 1966 : son missel est protégé jusqu'au 31 décembre 2036** [V]. Sont en revanche dans le domaine public : Crampon, Fillion, Lamennais, Corneille, Vautier (Exercices, 1673) ainsi que les illustrations de René De Cramer (mort en 1951) [V/S].
- **Images.** L'article 14 de la directive (UE) 2019/790 exclut tout droit sur la reproduction fidèle d'une œuvre d'art visuel du domaine public [V]. La France ne l'a pas transposé explicitement, car elle estime que son droit le prévoit déjà [V, source secondaire]. Les photos d'œuvres en 3D peuvent toutefois rester protégées. Il faut tracer la provenance de chaque image. **Polices OFL :** on peut les embarquer librement, à condition d'inclure la notice et la licence [V].
- **Approbation ecclésiastique.** Elle n'a aucun effet civil. Canoniquement, le canon 824 §2 étend les règles sur les livres à tout écrit destiné à la publication [V]. Le canon 826 §3 soumet les **livres de prière** à l'autorisation de l'Ordinaire du lieu. Le canon 826 §2 exige une attestation de concordance pour les rééditions de livres liturgiques [V]. Rien de cela ne s'applique à l'usage personnel (scénario A). Pour une diffusion publique (C), il faut soit demander la permission, soit afficher un avertissement clair.
- **Distribution.** Google Play exige une politique de confidentialité **même si l'application ne collecte rien** et un formulaire « Sécurité des données » [V]. Un compte personnel récent doit faire un test fermé de 12 testeurs pendant 14 jours [V]. **Android impose la vérification des développeurs**, mondialement en 2027. Les installations par adb en sont exemptées, et un compte « distribution limitée » gratuit couvre jusqu'à 20 appareils [V]. Cela intéresse directement le scénario B.
- **RGPD.** Un journal spirituel relève de l'article 9 (convictions religieuses). **Stockage local uniquement, exclusion des sauvegardes cloud par défaut, chiffrement au repos, aucune télémétrie** : à ces conditions, le développeur ne traite aucune donnée [S]. Le simple fait de télécharger des contenus catholiques depuis un serveur du développeur peut déjà révéler une conviction religieuse [C].
- **Licence du code (recommandation) : GPL-3.0-or-later pour l'application, Apache-2.0 pour les bibliothèques réutilisables** (calendrier liturgique), compatible avec la MIT de Divinum Officium [V]. Les contenus restent **séparés** du code, dans des paquets dont chacun porte sa propre licence et sa fiche de provenance.

---

## 2. Constats détaillés

### 2.1 Cadre juridique de base (France et UE)

**Durées de protection** (Code de la propriété intellectuelle, version consolidée au 5 août 2026 [V]) :

| Situation | Règle | Article |
|---|---|---|
| Auteur identifié | Année du décès, puis 70 ans | L123-1 [V] |
| Œuvre de collaboration | 70 ans après la mort du dernier collaborateur vivant | L123-2 [V] |
| Œuvre anonyme, pseudonyme ou **collective** | 70 ans à compter du 1er janvier suivant la publication. Si les auteurs se font connaître, on revient à L123-1 ou L123-2 | L123-3 [V] |
| Œuvre posthume publiée après l'expiration | 25 ans à compter de la publication | L123-4 [V] |
| Œuvre collective : définition et titulaire | Créée à l'initiative d'une personne qui l'édite et la divulgue sous son nom, les contributions s'y fondant. Elle appartient à cette personne | L113-2 al. 3, L113-5 [V] |
| Traduction | Œuvre dérivée, protégée pour elle-même : un texte du domaine public peut avoir une traduction protégée | L112-3 [C] |

**Règle pratique au 1er janvier 2026 [S, déduite de L123-1 et L123-3]** : un auteur mort en **1955 ou avant** est dans le domaine public. Une œuvre collective ou anonyme publiée en **1955 ou avant** l'est aussi, sauf si l'auteur est identifié, auquel cas on applique la date de son décès.

**Prorogations de guerre.** L123-8 couvre la Première Guerre mondiale (environ 6 ans et 152 jours pour les œuvres publiées avant la fin de 1919), L123-9 la Seconde (8 ans et 120 jours pour les œuvres publiées avant 1948) et L123-10 ajoute 30 ans pour les auteurs « morts pour la France » [V]. Selon deux arrêts de la Cour de cassation (1re civ., 27 févr. 2007, n° 280 « Monet » et n° 281 « Boldini »), la durée de 70 ans **absorbe** ces prorogations. Elles ne jouent donc plus que si l'ancien régime (50 ans plus les prorogations, plus 30 ans le cas échéant) donnait une durée plus longue, ce qui vise surtout les auteurs morts pour la France. La question reste ouverte pour les œuvres musicales, déjà protégées 70 ans depuis la loi de 1985 [V, via Wikipédia, à confirmer sur Légifrance].
→ Pour nos sources (prêtres et religieux morts en temps de paix), les prorogations ne changent presque rien. Il faut seulement vérifier la mention « mort pour la France » pour les auteurs tués en 1914-1918 ou en 1939-1945.

**Exceptions utiles (L122-5) [V]**
- 1° Les représentations privées et gratuites dans le cercle de famille. Il s'agit de représentations, pas de copies remises à des tiers.
- 2° La copie privée : elle doit provenir d'une **source licite** et être strictement réservée à l'usage privé du copiste. Les **copies de bases de données électroniques en sont exclues**.
- 3° a) Les courtes citations justifiées, avec le nom de l'auteur et la source.
- 10° et L122-5-3 : la fouille de textes et de données (voir 2.4).

**Bases de données (droit *sui generis*) [V]**
- Le producteur est protégé s'il a consenti un investissement substantiel dans la constitution, la vérification ou la présentation du contenu (L341-1).
- Il peut interdire l'extraction ou la réutilisation d'une partie substantielle (L342-1), ainsi que l'extraction répétée et systématique de parties non substantielles (L342-2).
- Exceptions (L342-3) : 1° l'extraction non substantielle par un utilisateur légitime ; 2° l'extraction privée substantielle, **seulement pour une base non électronique** ; 6° la fouille de textes et de données.
- La protection dure 15 ans et se renouvelle à chaque nouvel investissement substantiel (L342-5).
- Selon la CJUE, l'investissement consacré à **créer** les données (rédiger les lettres, par exemple) ne compte pas (arrêt *British Horseracing Board*, C-203/02, 2004) [C].

**Article 14 de la directive (UE) 2019/790 [V]** : la reproduction d'une œuvre d'art visuel tombée dans le domaine public ne peut faire l'objet d'aucun droit d'auteur ni droit voisin, sauf si elle est elle-même originale. Le considérant 53 précise que les musées restent libres de vendre des cartes postales. **La France ne l'a pas transposé explicitement**, au motif que son droit ne protège déjà pas les reproductions non originales (Europeana Copyright Community [V, source secondaire]). Le CPI consolidé ne contient en effet aucune disposition de ce type [V, recherche dans le texte].

### 2.2 Statut de chaque source

#### 2.2.1 Lettres spirituelles de Clairval

- **Éditeur et mentions légales [V].** Le site est édité par la Congrégation des Bénédictins de Saint-Joseph de Clairval, à Flavigny-sur-Ozerain ; le directeur de la publication est le Père Abbé. Le pied de page porte « © 2024 Abbaye Saint-Joseph de Clairval ». Les mentions légales ne contiennent **aucune clause** de propriété intellectuelle ou de réutilisation, et le site n'a pas de CGU.
- **Mention de reproduction [V]**, présente à la fin de chaque lettre (vérifiée sur la lettre du 16 avril 2021, « Saint Joseph Vaz ») : publier la Lettre spirituelle dans une revue, un journal, etc., ou sur un site internet suppose une autorisation, à demander par la page contact.
- **Volume et formats [V].** Les archives en ligne couvrent 1996 à 2025, réparties sur six pages de cinq ans. Chaque lettre a sa page HTML et un PDF (par exemple `/documents/FR-2021-04-16.pdf`). La lettre paraît chaque mois en français, anglais, allemand, espagnol, italien, néerlandais, portugais et catalan.
- **robots.txt [V].** Le fichier interdit `/wp-content/themes/twentyseventeen-child/assets/lettres` et fixe un `crawl-delay: 10`. Il n'y a ni `tdmrep.json` ni balise de réserve TDM ; la balise robots vaut `index, follow`.
- **Contacts [V]** : le formulaire https://www.clairval.com/contact/ ; les adresses `abbaye@clairval.com` et `site-internet@clairval.com`, présentes dans le code CSS du site ; l'adresse postale 6 Grande Rue, 21150 Flavigny-sur-Ozerain ; le téléphone +33 3 80 96 22 31.
- **Qualification [S].** Les lettres sont des œuvres originales, publiées sous le nom de l'abbaye. Il s'agit vraisemblablement d'une **œuvre collective** appartenant à l'abbaye (L113-5), protégée 70 ans à compter de la publication de chaque lettre, donc au moins jusqu'en 2067. Si on les qualifiait d'œuvres individuelles d'un abbé ou d'un moine, la durée serait encore plus longue. Dans tous les cas, **c'est l'abbaye qu'il faut solliciter.**
- **Contenus tiers [S].** Les lettres reproduisent des photos, dont les droits appartiennent peut-être à des tiers, et des citations pontificales (© Libreria Editrice Vaticana), qui relèvent du droit de citation de l'abbaye. Il faut poser la question des images dans la demande.

#### 2.2.2 Exercices spirituels : « ragionline.com »

- **Domaine introuvable [V].** `ragionline.com` ne résout pas (NXDOMAIN), pas plus que `.it`, `.fr` ou `.org`, et aucune recherche ne le trouve. La source exacte est donc inconnue : **l'agent 02 doit l'identifier.** Le fait qu'un PDF soit en ligne ne vaut jamais licence.
- **Ce qui est libre.** Le texte original d'Ignace (autographe espagnol, Vulgate latine de 1548) est dans le domaine public [C]. Wikisource propose la **traduction Vautier (1673)**, du domaine public [V]. La traduction du P. Pierre Jennesseaux s.j. (1854) est très probablement libre aussi [S : dates de décès à confirmer].
- **Ce qui est protégé [C].** Les traductions modernes : Courel (Christus, Desclée de Brouwer, années 1960), Jean-Claude Guy (Seuil, 1982), Gueydan et collaborateurs (Christus, 1985).
- **Mode « retraite guidée ».** Les textes que l'utilisateur rédigera lui-même seront sa propriété, et il pourra les placer sous la licence de son choix.

#### 2.2.3 Divinum Officium (dépôt GitHub)

- **Licence [V].** Le fichier `LICENSE` est sous MIT, « Copyright (c) 2026 Divinum Officium », ajouté par le commit 2e2ddf97 du **28 août 2026**. La page de crédits précise que le code et les données sont sous MIT, que les textes liturgiques sont, *à leur connaissance*, du domaine public, et qu'aucune permission n'est nécessaire.
- **Provenance réelle, selon les crédits [V]**
  - **Latin** : le *Breviarium Romanum* de 1888 à 1962 (Pustet, Benziger, Desclée et Mame 1962…) et la Vulgate. La Vulgate accentuée a été fournie par Frère Romain-Marie, **de Clairval**. Ces sources sont du domaine public, et leur reprise sous MIT ne pose aucun problème.
  - **Français, ordinaire de la messe** : il vient de *Sancta Missa* et d'*introibo.fr*, dont la licence est inconnue. Le site introibo.fr ne répond pas [V].
  - **Français, propre** : **« œuvre et propriété de l'Abbaye Ste-Madeleine du Barroux »**.
  - **Français, Bible** : le Nouveau Testament de Crampon, modifié par le passage au vouvoiement, et l'Ancien Testament de Fillion, tous deux du domaine public.
  - **Français, bréviaire** : le *Bréviaire romain* latin-français édité par Labergerie en 1956. Le traducteur est inconnu ; l'œuvre est probablement protégée [S].
  - **Commentaires** : Guéranger est du domaine public. Schuster (mort en 1954) et Parsch (mort en 1954) le sont aussi pour l'original, mais leurs **traducteurs français** sont inconnus [S].
  - **Anglais** : la source de la traduction *Sancta Missa* n'est pas documentée, ce que le projet reconnaît lui-même. Le comparatif des missels inclut la traduction anglaise de 2010 (USCCB et ICEL), qui est protégée.
- **Conséquence.** Un fichier MIT ne peut pas concéder des droits que le projet ne détient pas. **On peut utiliser librement le latin, le code et les règles de calcul (sous MIT, avec la notice). Pour le français, il faut l'accord du Barroux ou une autre traduction.** Si l'on reprend des données de Divinum Officium, il faut conserver le texte de la licence MIT dans l'écran « Licences ».

#### 2.2.4 AELF (textes liturgiques officiels francophones) et ICEL

- **CGU de l'AELF [V]** (https://www.aelf.org/page/conditions-generales-dutilisation)
  - Elles autorisent la reproduction pour stockage et affichage sur un **écran monoposte**, plus une copie de sauvegarde ou une impression, dans un cadre **strictement personnel, privé et non collectif**.
  - Elles **interdisent** toute mise en réseau, rediffusion ou commercialisation auprès de tiers.
  - Toute autre reproduction exige une autorisation **préalable et expresse** de l'AELF.
  - La mention de crédit est « Textes liturgiques © AELF, Paris ». Certains textes relèvent des Éditions du Cerf.
  - Les services de l'AELF ne remplacent pas les livres liturgiques officiels.
- **API [V].** https://api.aelf.org expose un Swagger public (messes, lectures, offices, informations) **sans conditions propres**. Ce sont donc les CGU qui s'appliquent.
- **Pratique des applications existantes [V]**
  - L'application « AELF – Lectures du jour » (HackMyChurch) a un code sous licence MIT. Les textes et le logo y sont reproduits avec l'autorisation de l'AELF. Elle est présente sur F-Droid avec l'antifonction **NonFreeNet** (« Depends on api.aelf.org »).
  - *Prions en Église* (Bayard) signale la traduction liturgique © AELF.
  - Pour *Magnificat*, je n'ai vu qu'un « © Magnificat » sur le site. Il s'agit probablement d'une licence AELF [S].
  - Le site d'iMissal n'a pas répondu, je n'ai donc rien pu vérifier [S].
- **ICEL (anglais) [V]**
  - Toutes ses traductions sont protégées, et toute reproduction suppose sa permission.
  - Toute publication destinée à la vente, y compris sur support électronique, donne lieu à une redevance.
  - La permission dépend de l'autorisation de la conférence épiscopale du territoire.
  - La mention obligatoire est « The English translation of The Roman Missal © 2010, ICEL ».
  - Contact : permission@eliturgy.org (https://icelweb.org/PubPolicy.PDF, https://www.icelweb.org/copyright.htm).
- **Droit de l'Église sur les textes [V].**
  - *Liturgiam authenticam* (2001), n° 111 : le Saint-Siège revendique le « copyright » des éditions typiques, y compris celles d'avant Vatican II, par l'APSA ou la Libreria Editrice Vaticana.
  - N° 114 à 117 : les conférences épiscopales détiennent les droits sur les traductions.
  - Ces numéros visent les éditions **destinées à l'usage liturgique** (livres d'autel). Un missel de fidèle numérique n'en est pas une [S]. Le latin de 1962 est pour l'essentiel ancien. Le risque civil est donc faible, mais non nul pour les éléments récents, comme les rubriques de 1960.

#### 2.2.5 Missels bilingues anciens

| Ouvrage | Auteur ou traducteur | Statut en France [niveau] |
|---|---|---|
| *Missel quotidien et vespéral* (1920, puis plus de 80 éditions jusqu'à 1962) | Dom Gaspar Lefebvre, **mort le 16 avril 1966** [V] | **Protégé jusqu'au 31/12/2036** si on l'attribue à Lefebvre seul [S]. Si on le qualifiait d'œuvre collective de l'Apostolat liturgique, chaque édition serait protégée 70 ans après sa publication : l'édition de 1962 jusqu'en 2032 [S]. Les éditions antérieures à 1955 sont aussi protégées, puisque l'auteur est nommé [S]. |
| Traduction dans le Lefebvre | Fillion (mort en 1927) [V] | Wikipédia attribue la traduction à Fillion, sans préciser si cela vaut pour tout le missel ou seulement pour l'Écriture [V]. La partie due à Fillion est dans le domaine public. Les oraisons, notes et introductions sont vraisemblablement de Lefebvre et de ses confrères, donc protégées [S]. |
| Illustrations du Lefebvre | René De Cramer (mort en 1951) [V] | Domaine public depuis le 1/1/2022 [S] |
| *Missel quotidien des fidèles* (Feder, Mame) | P. Joseph Feder s.j. | **Dates non vérifiées** : à considérer comme protégé tant que la notice BnF n'a pas été consultée [S] |
| *L'Année liturgique* | Dom Guéranger (mort en 1875), puis Dom Fromage | Domaine public [C]. C'est une bonne source de traductions françaises des propres antérieurs à 1900. |
| Paroissiens romains du XIXe et du début du XXe siècle (Mame, Desclée…) | Souvent anonymes ou collectifs | Domaine public s'ils ont été publiés en 1955 ou avant et que l'auteur n'est pas identifié [S] |
| Propres de 1962 (Barroux, via Divinum Officium) | Abbaye du Barroux | Protégés [V] |

**Éditions modernes de textes anciens [S].** Le droit français ne connaît pas de droit sur la mise en page typographique. Une réimpression récente d'un texte du domaine public ne crée donc pas de droit nouveau, sauf pour les notes, introductions et révisions originales qu'elle ajoute.

**Lacunes 1955-1962 [S].** La Semaine sainte de 1955 et les rubriques de 1960 n'existent pas dans les éditions tombées dans le domaine public. Il faudra soit obtenir une licence, soit **traduire soi-même** : l'utilisateur sera alors titulaire des droits et pourra placer ses traductions sous CC BY-SA 4.0.

#### 2.2.6 Bibles et Imitation

| Texte | Statut |
|---|---|
| Crampon 1894-1904 (Augustin Crampon, mort en 1894 [V]) | Domaine public |
| Crampon révisée de 1923 : révisée par des Pères jésuites avec des professeurs de Saint-Sulpice, sans noms, Desclée et Société Saint-Jean-l'Évangéliste [V] | Œuvre collective publiée en 1923, **dans le domaine public depuis 1994** [S] |
| Fillion (mort en 1927), Glaire, Lemaistre de Sacy, Vigouroux | Domaine public [V/C] |
| Bible de Jérusalem (Cerf), TOB, Osty (mort en 1981), Bible liturgique AELF (2013) | **Protégées** [C] |
| *Imitation* : Lamennais (mort en 1854), Brignon (1718), Corneille en vers (1862 sur Wikisource) [V] | Domaine public. Il faut éviter les préfaces et notes modernes. |

**Wikisource [V].** Les pages sont publiées sous CC BY-SA. Le texte du domaine public reste libre, mais si l'on reprend les corrections et la mise en forme propres à Wikisource, mieux vaut créditer Wikisource et traiter le paquet comme CC BY-SA [S].

#### 2.2.7 Polices (SIL OFL 1.1) [V]

- On peut embarquer une police OFL dans une application, y compris propriétaire ou payante. Il faut inclure **la notice de copyright, la notice de licence et le texte de la licence**, et la FAQ OFL recommande d'en faire mention dans l'écran « À propos ».
- On ne peut pas vendre la police seule.
- Le sous-ensemble de glyphes compte comme une modification : si la police a des noms réservés (RFN), il faut la renommer.
- **Pour la vie privée, il vaut mieux embarquer les polices que les télécharger à l'exécution depuis Google Fonts [S].**

#### 2.2.8 Images (images pieuses, illustrations, enluminures)

- **Reproductions fidèles d'œuvres en 2D du domaine public.** Elles ne sont pas protégées (article 14 de la directive et position française) [V/S]. C'est la doctrine « PD-Art » de Wikimedia Commons [C].
- **Photos d'œuvres en 3D** (statues, reliefs, objets). Elles peuvent être des œuvres originales du photographe. Il faut alors respecter leur licence (souvent CC BY-SA, avec attribution et partage à l'identique) [S].
- **Images pieuses anciennes.** Il faut regarder l'artiste et l'éditeur. Une carte anonyme d'éditeur publiée en 1955 ou avant est dans le domaine public (L123-3), de même qu'une carte d'un artiste mort en 1955 ou avant [S]. Les scans personnels de cartes du domaine public ne posent pas de difficulté.
- **CC0 [C].** Plusieurs musées publient leurs images d'œuvres du domaine public en CC0 : le Rijksmuseum, le Metropolitan Museum, la National Gallery of Art, l'Art Institute of Chicago, Paris Musées.
- **Points à surveiller [S, non vérifiés faute de budget de recherche]**
  - Les conditions de Gallica (BnF) ont longtemps rendu payante la réutilisation commerciale des documents du domaine public numérisés. La page est restée inaccessible (erreur 403).
  - Le droit italien du patrimoine culturel (Codice dei beni culturali, art. 107-108) prétend soumettre l'image des biens culturels publics à autorisation et redevance, même pour des œuvres du domaine public.
  - Le Code du patrimoine (L621-42) encadre l'image des domaines nationaux.
- **Images des lettres de Clairval.** Leur statut est inconnu : il faut poser la question à l'abbaye.

#### 2.2.9 Chant grégorien

- Les mélodies médiévales sont dans le domaine public. L'**édition Vaticane** (1905-1912) l'est aussi [C].
- Les **signes rythmiques solesmiens** (épisèmes, ictus) des éditions Desclée et Solesmes peuvent relever d'un droit d'éditeur. Les éditions publiées en 1955 ou avant sont probablement dans le domaine public en tant qu'œuvres collectives [S]. Le *Liber Usualis* de 1961 serait protégé jusqu'en 2031 environ s'il est qualifié d'œuvre collective [S]. Les éditions récentes sont protégées : *Graduale Triplex* (1979), *Antiphonale Monasticum* (2005 et suivants), *Graduale Novum* (2011) [C].
- **GregoBase [V]** ne publie pas de licence, et ses partitions viennent en grande partie du *Caecilia Project*. Il faut demander ou créditer, et préférer transcrire soi-même à partir des éditions du domaine public. Divinum Officium tire ses partitions de l'*Antiphonale* (Desclée, 1949) et du *Liber Antiphonarius* (Solesmes, 1934, 1960 et 1891) [V].
- **Enregistrements audio.** Les droits voisins des interprètes et des producteurs durent 70 ans (L211-4) [C]. Il faut des enregistrements propres, des enregistrements sous licence CC0 ou CC BY, ou une restitution synthétique à partir du gabc.

### 2.3 Les trois scénarios de distribution

| Source | A — Usage strictement personnel (APK installé par adb) | B — Diffusion à des proches | C — Diffusion publique (Play, F-Droid, site) |
|---|---|---|---|
| Lettres de Clairval | Copie privée des lettres admise (L122-5 2°). L'aspiration de toute l'archive est en **zone grise**, car la base électronique est exclue de l'exception, mais le risque pratique est quasi nul. Il faut respecter le robots.txt. | Un APK contenant les lettres sort de la copie privée. La variante « chaque appareil télécharge pour son propriétaire » se défend [S], mais **l'autorisation reste préférable**. | **Autorisation écrite indispensable** : mention de l'abbaye, politique de propriété intellectuelle de Google Play, risque de parasitisme. |
| Divinum Officium, latin et code | Libre | Libre, avec la notice MIT | Libre, avec la notice MIT |
| Divinum Officium, propre français | Usage privé admis | Zone grise : propriété du Barroux | Accord du Barroux ou autre traduction |
| AELF | Admis par les CGU (usage personnel, écran monoposte) | Embarqué : non. Appel à l'API depuis chaque appareil : défendable [S]. | **Autorisation AELF**, comme pour l'application AELF |
| Missels protégés (Lefebvre…) | Numériser son propre exemplaire est une copie privée | Non | Licence des ayants droit, ou attendre 2037 |
| Bibles et Imitation du domaine public, Vautier | Libre | Libre | Libre. Créditer la source (Wikisource, Gallica…). |
| Bible de Jérusalem, TOB, Bible AELF | Usage privé | Non | Licence |
| Polices OFL | Libre | Inclure la licence | Inclure la licence |
| Images CC0 ou du domaine public | Libre | Libre | Libre, avec une fiche de provenance par image |
| Chant (édition Vaticane, transcriptions propres) | Libre | Libre | Libre. Éditions récentes et enregistrements : licence. |

**Conséquences transversales**

- **Droit canonique**
  - A : aucune « publication », donc rien à demander.
  - B : un partage privé n'est pas vraiment une publication au sens du canon 824 §2 [S].
  - C : les canons 826 §3, 826 §2 et 825 entrent en jeu (voir 2.5).
- **Distribution**
  - A : l'installation par adb n'est pas concernée par la vérification des développeurs [V].
  - B : à partir du déploiement mondial de 2027, il faut un compte « distribution limitée » (gratuit, 20 appareils au plus, sans pièce d'identité) ou un compte complet (25 USD, avec pièce d'identité). À défaut, chaque proche doit activer le « parcours avancé » : mode développeur, 24 heures d'attente, puis confirmation biométrique [V].
  - C : il faut une fiche Play avec un test fermé, ou passer par F-Droid (voir 2.6).
- **RGPD**
  - A et B : rien à faire si aucune donnée ne quitte l'appareil, car l'utilisateur est couvert par l'exemption domestique (art. 2(2)(c)) [C].
  - C : une politique de confidentialité est obligatoire sur Play [V]. Le RGPD s'applique pleinement au développeur dès qu'il exploite un serveur ou une télémétrie.

### 2.4 « Récupérer tout le site » de Clairval

1. **Droit d'auteur sur chaque lettre.** Il est certain (2.2.1) et constitue l'obstacle principal.
2. **Droit *sui generis* sur l'archive.** Il est incertain [S]. L'abbaye investit surtout dans la *création* des lettres, ce qui ne compte pas selon la jurisprudence *BHB* [C]. Son investissement dans la *présentation* (site WordPress, classement, multilinguisme) pourrait toutefois suffire. Si ce droit existe, l'extraction de tout ou partie substantielle de l'archive est interdite sans accord (L342-1). La seule exception d'extraction privée vise les bases **non électroniques** (L342-3 2°) [V].
3. **Fouille de textes et de données (L122-5-3 III) [V].** Toute personne peut faire des copies en vue d'une analyse automatisée destinée à dégager des constantes, des tendances ou des corrélations, sauf opposition « par des procédés lisibles par machine », et **les copies doivent être détruites à l'issue de la fouille**. Clairval n'exprime pas d'opposition lisible par machine [V]. Cette exception permettrait par exemple d'étudier la structure HTML pour concevoir l'analyseur. **Elle ne permet ni de constituer une bibliothèque de lecture, ni de la conserver, ni de la redistribuer.**
4. **CGU et robots.txt.** Le site n'a pas de CGU contractuelles [V]. La mention d'autorisation constitue cependant une réserve claire pour toute publication. Respecter `crawl-delay: 10` et la zone interdite relève de la bonne foi et réduit le risque de grief, ou de qualification d'atteinte aux systèmes si l'on surchargeait le serveur [S].
5. **Parasitisme (art. 1240 C. civ.) [S].** Une application publique qui détournerait l'audience de l'abbaye, ses abonnements gratuits ou ses dons pourrait se le voir reprocher. La parade : des liens visibles vers l'abonnement, les dons et la page d'origine.
6. **Conclusion.** Pour A, archiver pour soi est défendable. Pour B et C, **il faut demander une autorisation écrite**, en précisant : le texte intégral sans modification, les langues, la synthèse vocale, le partage de courts extraits, les images, la méthode de récupération (téléchargement depuis le site ou archive fournie), la mention de crédit et la possibilité de retrait. Voir l'annexe A.

### 2.5 Approbation ecclésiastique

**Textes [V]** (CIC, vatican.va ; *Magnum principium*, 2017)

- **Canon 824 §1** : l'Ordinaire compétent est celui de l'auteur ou celui du lieu d'édition.
- **Canon 824 §2** : les dispositions sur les livres s'appliquent **à tout écrit destiné à la publication**, sauf s'il apparaît qu'il en va autrement. Une application diffusée publiquement entre donc vraisemblablement dans le champ [S].
- **Canon 825** : l'édition de la Bible et de ses traductions requiert l'approbation du Saint-Siège ou de la conférence épiscopale, avec des notes explicatives.
- **Canon 826 §2** : pour rééditer des livres liturgiques, leurs traductions ou des parties de ceux-ci, l'Ordinaire du lieu de publication doit attester leur **concordance** avec l'édition approuvée.
- **Canon 826 §3** : les **livres de prière** pour l'usage public ou privé des fidèles ne s'éditent pas **sans l'autorisation de l'Ordinaire du lieu**.
- **Canon 827** : les catéchismes requièrent une approbation (§1). Un écrit religieux ne peut être exposé, vendu ou donné **dans les églises** sans permission (§4), ce qui compte si l'application est promue en paroisse.
- **Canon 829** : l'approbation vaut pour le texte original, pas pour les nouvelles éditions. L'imprimatur d'un missel de 1950 ne couvre donc pas une réédition numérique.
- **Canon 838** (modifié par *Magnum principium*) : le Saint-Siège édite les livres liturgiques, et les conférences épiscopales préparent, approuvent et publient les traductions après confirmation du Saint-Siège.

**Cas de la forme extraordinaire [V].** Les *Responsa ad dubia* de la Congrégation pour le Culte divin du 18 décembre 2021 (n° 211218c) prévoient que les lectures se lisent dans la traduction biblique approuvée par la conférence épiscopale. Ils précisent aussi qu'**on ne peut pas publier de lectionnaires en langue vernaculaire reproduisant le cycle de lectures de l'ancien rite.** Un missel de fidèle n'est pas un lectionnaire liturgique [S], mais obtenir un imprimatur pour un contenu de 1962 risque d'être délicat dans ce contexte. La politique actuelle du Saint-Siège sur ce point n'a pas pu être revérifiée.

**Effet civil.** Aucun : l'imprimatur n'est pas une condition légale de publication en France. C'est une obligation de discipline ecclésiale pour un fidèle qui publie, et un gage de crédibilité.

**Pratique observée**

- **[V]** L'application AELF tient les droits de l'AELF, et non un imprimatur.
- **[V]** Divinum Officium se présente comme une œuvre personnelle qui ne représente aucune autorité officielle.
- **[C]** L'application « Confession: A Roman Catholic App » a obtenu un imprimatur en 2011 (diocèse de Fort Wayne–South Bend).
- **[S]** La plupart des applications de dévotion n'en ont pas.

**Conclusion [S]**

- A et B : rien à faire.
- C, version minimale : un avertissement clair, par exemple « Application privée de dévotion ; n'est pas un livre liturgique officiel ; sans imprimatur ». On peut aussi indiquer pour chaque texte l'imprimatur de l'édition source, en précisant qu'il ne couvre pas l'application.
- C, version complète : demander l'**autorisation de l'Ordinaire** (canon 826 §3) à la chancellerie du diocèse de l'utilisateur, au moins pour le volet « prières », et demander l'attestation de concordance (canon 826 §2) pour le volet missel.

### 2.6 Distribution

**Google Play [V]**

- **Politique de confidentialité obligatoire pour toutes les applications, même celles qui n'accèdent à aucune donnée.** Elle doit être accessible par un lien dans la Console et dans l'application, sur une URL publique qui ne soit pas un PDF. Elle doit mentionner le développeur, un contact, les données traitées, la sécurité, la conservation et la suppression.
- Le formulaire **« Sécurité des données »** est obligatoire. Les données traitées uniquement sur l'appareil ne sont pas « collectées » et n'ont pas à être déclarées. La catégorie « Political or religious beliefs » existe : elle serait à cocher si une donnée quittait l'appareil.
- **Propriété intellectuelle.** La politique interdit les applications contrefaisantes. Elle cite parmi les exemples la reproduction ou la traduction intégrale de livres qui ne sont pas dans le domaine public. Il faut **garder les autorisations écrites sous la main**, car Google peut en demander la preuve.
- **Comptes personnels créés après le 13 novembre 2023** : test fermé d'**au moins 12 testeurs pendant 14 jours consécutifs** avant la production. Les frais d'inscription sont de 25 USD.
- **[C]** Aucune règle n'est propre aux applications religieuses. Les règles générales s'appliquent : discours haineux, usurpation d'identité (ne pas utiliser le blason ou le nom de Clairval comme s'ils approuvaient l'application), propriété intellectuelle, données.
- **[S]** Le statut de « professionnel » (*trader*) au sens du DSA doit être déclaré dans la Console. Une application gratuite et non monétisée relève normalement du statut « non professionnel ». Un « professionnel » voit ses coordonnées publiées.

**Vérification des développeurs Android [V]** (https://developer.android.com/developer-verification)

- Le 30 septembre 2026, elle s'applique dans quatre pays (Brésil, Indonésie, Singapour, Thaïlande), pour les installations depuis les boutiques participantes. Elle s'étend au **monde entier en 2027** pour toutes les applications sur les appareils certifiés.
- L'installation par **adb** reste libre, sans délai.
- Le **compte « distribution limitée »** est gratuit, sans pièce d'identité, et couvre jusqu'à 20 appareils. Le compte complet coûte 25 USD. Le « parcours avancé » permet à l'utilisateur d'autoriser une fois pour toutes les applications non vérifiées.
- **[S, inconnu]** L'effet sur F-Droid en 2027 n'est pas documenté.

**F-Droid [V]**

- Le code doit être entièrement libre (FLOSS).
- Les bibliothèques propriétaires de pistage ou de publicité sont interdites.
- Les ressources non fonctionnelles doivent avoir une licence valide ou être dans le domaine public.
- Les antifonctions concernées sont **NonFreeAssets** (ressources non libres embarquées), **NonFreeNet** (dépendance à un service non libre, comme l'API AELF) et **Tracking** (rapports d'erreur ou vérification de mises à jour sans consentement).

**Site web (scénario C) [C].** La LCEN (art. 6-III) impose des mentions légales. Un particulier peut se contenter de communiquer son identité à l'hébergeur.

### 2.7 RGPD et journal spirituel

- **Données sensibles.** Les notes de méditation, les intentions de prière, les examens de conscience et même l'historique de lecture révèlent des **convictions religieuses** (art. 9 §1 RGPD) [C].
- **Qui est responsable du traitement ? [S]** Si tout reste sur l'appareil et que le développeur n'y a jamais accès, c'est l'utilisateur qui traite ses propres données dans un cadre personnel (art. 2(2)(c)). Le considérant 18 rappelle que le RGPD s'applique aux responsables qui fournissent les moyens d'un traitement domestique **lorsqu'ils traitent eux-mêmes des données**. L'objectif est donc qu'aucune donnée ne parvienne jamais au développeur. La CNIL a publié des recommandations sur les applications mobiles (délibération 2024-061, version modifiée le 8 avril 2025) qui répartissent les rôles entre éditeur, développeur, SDK et magasin [V].
- **Trafic réseau [C].** La CJUE a jugé que les données issues de la consultation de sites ou d'applications en lien avec une catégorie sensible peuvent relever de l'article 9 (Meta c/ Bundeskartellamt, C-252/21, 4 juillet 2023, point non revérifié en ligne). Si l'application télécharge des paquets depuis un serveur du développeur, les journaux d'adresses IP de ce serveur révèlent une pratique religieuse. Les appels à l'API AELF ou à clairval.com transmettent aussi l'adresse IP à des tiers, ce que doit dire la politique de confidentialité.
- **Points de fuite à fermer [S/C]**
  1. La **sauvegarde automatique Android** vers le compte Google de l'utilisateur : il faut exclure le journal par défaut et proposer à la place un export chiffré, explicite.
  2. Les **rapports de plantage** : pas de SDK tiers. Si l'on en veut, utiliser un rapport par courriel ou par fichier, sur option et désactivé par défaut, sans jamais inclure le texte du journal.
  3. Les **notifications** : pas de contenu du journal sur l'écran verrouillé.
  4. Les **captures d'écran et l'écran des applications récentes** : protection optionnelle de l'écran du journal.
  5. Le **clavier** : désactiver l'apprentissage du clavier sur le champ du journal, si la plateforme le permet.
- **Chiffrement [C].** Android chiffre par fichier (FBE) par défaut sur les appareils récents. Pour le journal, il faut ajouter un chiffrement applicatif dont la clé est stockée dans **Android Keystore** (base chiffrée ou fichiers chiffrés, au choix de l'agent 10) et, en option, un verrou biométrique ou un code. Jetpack Security Crypto semble déprécié [S, à vérifier par l'agent 10].
- **ePrivacy (art. 82 de la loi Informatique et Libertés) [C].** Stocker localement ce qui est strictement nécessaire au service demandé ne requiert pas de consentement.

### 2.8 Licence du code et compatibilité

**Compatibilités vérifiées [V]** (liste de la FSF) : Apache-2.0 est compatible avec la GPL v3, mais pas avec la v2. L'OFL est une licence libre pour polices. CC0 est compatible avec la GPL. **CC BY-SA 4.0 est compatible avec la GPL v3 dans un seul sens.**

| Option | Avantages | Inconvénients |
|---|---|---|
| **GPL-3.0-or-later** (application) | Les améliorations restent libres, l'application est bien acceptée par F-Droid, cohérente avec l'éthique « bien commun » et compatible avec AndroidX et Kotlin (Apache-2.0) | Rebute les intégrateurs propriétaires. Relicencier plus tard exige l'accord des contributeurs (prévoir une décision CLA ou DCO). |
| **Apache-2.0** | Licence permissive, avec une clause de brevets, recommandée par la FSF pour les gros programmes permissifs | Autorise des dérivés fermés |
| **MIT** | Celle de l'application AELF et de Divinum Officium | Pas de clause de brevets, permissive |

**Point clé : les contenus sont des œuvres séparées.** Un APK sous GPL qui contient des textes sous licence propre constitue un simple « agrégat » (section 5 de la GPL v3) : la GPL ne s'étend pas à ces textes, et ces textes ne la contaminent pas [C]. En revanche, **la licence du code ne dit rien des contenus** : chaque paquet a besoin de sa propre licence et de sa fiche de provenance.

---

## 3. Recommandations

### MVP — scénario A (usage personnel, installation par adb)

1. **Tenir un registre de provenance dès le premier paquet** (pour les agents 11 et 03). Chaque paquet porte une fiche avec les champs suivants :
   - la source et l'URL ;
   - l'édition et l'année ;
   - l'auteur et le traducteur, avec leurs dates de décès ;
   - la qualification : individuelle, collaboration ou collective ;
   - le statut, parmi `domaine-public`, `licence-libre:<SPDX>`, `autorisation-écrite:<réf>` et `usage-privé-seulement` ;
   - les scénarios autorisés (A, B, C) ;
   - la date de vérification.

   Le pipeline doit **refuser d'inclure dans un build B ou C** tout paquet marqué `usage-privé-seulement`.
2. **Séparer le code des contenus.** Le dépôt de code ne contient que du code et des données libres. Les contenus non libres (Clairval, AELF, propre français de Divinum Officium, missels protégés) restent hors du dépôt public et hors de tout APK partagé.
3. **Clairval.**
   - L'archivage personnel est acceptable.
   - Le récupérateur doit respecter le robots.txt (zone interdite, `crawl-delay: 10`), s'identifier par son agent utilisateur, faire une passe initiale lente puis des mises à jour incrémentales.
   - Il faut **envoyer dès maintenant la demande d'autorisation** de l'annexe A, en tenant compte du délai de réponse.
4. **Construire le missel et la Bible sur des sources du domaine public dès le MVP** pour éviter de tout refaire plus tard :
   - le latin de Divinum Officium (MIT) ;
   - Fillion ou Crampon pour l'Écriture ;
   - Guéranger et les paroissiens publiés en 1955 ou avant pour les propres ;
   - des traductions propres pour les lacunes, sous CC BY-SA 4.0 ;
   - Lamennais pour l'Imitation ;
   - Vautier, ou Jennesseaux une fois vérifié, pour les Exercices.

   Le propre français de Divinum Officium et les textes AELF ne servent qu'à l'usage privé.
5. **Polices OFL embarquées** (aucun appel à Google Fonts) et un écran « Licences » qui liste chaque police avec son copyright et le texte OFL.
6. **Images** : uniquement du domaine public ou du CC0, chacune avec sa fiche de provenance. Ne pas reprendre les images des lettres de Clairval sans réponse de l'abbaye.
7. **Vie privée dès la conception** (pour l'agent 10) :
   - le réseau n'est utilisé qu'à l'initiative explicite de l'utilisateur ;
   - le journal reste local, exclu des sauvegardes, chiffré avec une clé dans le Keystore ;
   - aucun SDK d'analyse ni de rapport de plantage.
8. **Choisir la licence du code avant toute contribution extérieure.** Recommandation : GPL-3.0-or-later pour l'application, Apache-2.0 pour les bibliothèques réutilisables (moteur de calendrier, analyseurs). Ajouter des en-têtes SPDX et, éventuellement, la convention REUSE.

### V1 — scénario B (proches), préparation de C

1. **Obtenir les autorisations écrites** :
   - de Clairval (annexe A) ;
   - de l'AELF, si l'on retient la forme ordinaire ou les prières en traduction liturgique (Notre Père, Credo…), en s'inspirant de l'application AELF ;
   - du Barroux, si l'on veut son propre français.
2. **En attendant les réponses, ne rien embarquer de protégé** dans l'APK partagé. Proposer une importation à l'initiative de l'utilisateur : chaque appareil télécharge pour son propriétaire, ou l'utilisateur importe ses propres fichiers.
3. **Préparer la vérification Android de 2027.** Ouvrir un compte « distribution limitée » (20 appareils, gratuit) ou un compte complet, et documenter pour les proches la marche à suivre.
4. **Écrans « À propos » complets** :
   - les sources, les crédits et les licences ;
   - un avertissement : « dévotion privée ; ne remplace pas les livres liturgiques officiels ; sans imprimatur » ;
   - une politique de confidentialité courte, même hors de Google Play.
5. **Prise de contact canonique informelle** avec la chancellerie du diocèse de l'utilisateur, pour savoir s'il faut une autorisation au titre du canon 826 §3 avant toute publication.

### Plus tard — scénario C (public)

1. **Google Play** :
   - un compte (25 USD) ;
   - le test fermé de 12 testeurs pendant 14 jours ;
   - le formulaire « Sécurité des données » (« aucune donnée collectée », si c'est vrai) ;
   - l'URL de la politique de confidentialité ;
   - les preuves d'autorisation prêtes ;
   - le statut DSA de non-professionnel si l'application reste gratuite.
2. **F-Droid** : code libre, sans dépendance propriétaire. Les paquets non libres sont proposés en téléchargement séparé, et on accepte les antifonctions NonFreeNet et NonFreeAssets là où elles s'appliquent.
3. **Imprimatur ou permission** au titre du canon 826 §3, plus l'attestation du canon 826 §2 pour le volet liturgique, si l'utilisateur le souhaite. À défaut, maintenir l'avertissement.
4. **Vérifier que le nom « Paroissien » est disponible** à l'INPI et à l'EUIPO, et sur Google Play [S].
5. **Veille réglementaire** :
   - le règlement sur la cyberrésilience (CRA), applicable le 11 décembre 2027, mais seulement en cas d'activité commerciale ;
   - l'Acte européen d'accessibilité, en cas de monétisation ;
   - la LCEN pour le site.
6. **Calendrier du domaine public** : le Lefebvre au 1er janvier 2037 (ou 2033 pour l'édition de 1962, si on la qualifie d'œuvre collective). Prévoir une mise à jour des paquets à ces dates.
7. **Avant le lancement public, faire relire le dispositif par un avocat** en propriété intellectuelle et données personnelles.

---

## 4. Décisions à soumettre à l'utilisateur

1. **Quel horizon de diffusion ?** A seulement, A puis B, ou jusqu'à C ? Toute la gestion des contenus en dépend.
2. **Envoi de la demande à Clairval** : qui signe, par quel canal (`abbaye@clairval.com`, le formulaire ou un courrier postal), pour quel périmètre (français seulement ou toutes les langues ; scénarios B et C) ?
3. **Forme ordinaire et textes AELF** : les inclure, ce qui suppose une autorisation de l'AELF pour B et C, ou s'en tenir à la forme extraordinaire et aux traductions du domaine public ?
4. **Traduction française du missel de 1962** :
   - (a) demander l'autorisation du Barroux ;
   - (b) reconstruire le missel à partir du domaine public (Fillion, Guéranger, anciens paroissiens) et de traductions propres ;
   - (c) obtenir une licence pour le Lefebvre.
5. **Licence du code** : GPL-3.0-or-later (recommandé), Apache-2.0 ou MIT ? Accepter des contributions avec un DCO ou un CLA ?
6. **Approbation ecclésiastique** : solliciter l'Ordinaire avant C, ou se contenter d'un avertissement ? Auprès de quel diocèse ?
7. **Journal** : autoriser la sauvegarde cloud (non par défaut, recommandé), proposer un verrou de l'application, choisir le format d'export chiffré.
8. **Réseau** : l'application peut-elle télécharger d'elle-même (Clairval, API AELF), ou seulement à la demande ? Hébergement éventuel de paquets, avec l'exposition au RGPD qui en découle.
9. **Images** : accepter le CC BY-SA, avec attribution et partage à l'identique, ou se limiter au domaine public et au CC0 ?
10. **Identité publique du développeur** sur Google Play et sur le site : nom, et adresse si statut professionnel.

---

## 5. Risques et inconnues

| Risque | Gravité | Probabilité | Commentaire |
|---|---|---|---|
| Clairval refuse ou ne répond pas | Haute pour C | Moyenne | Le module central de la vision serait limité à l'usage personnel. Prévoir un mode « importation à l'initiative de l'utilisateur ». |
| Images des lettres de Clairval détenues par des tiers | Moyenne | Moyenne | Afficher le texte seul tant que la question n'est pas réglée. |
| Provenance contradictoire de Divinum Officium (MIT ou propriété du Barroux) | Haute pour C | Certaine | Seul le latin est sûr. |
| Qualification incertaine des missels (individuel, collaboration ou collectif) et dates de Feder non vérifiées | Moyenne | Moyenne | C'est le juge qui qualifie. Adopter l'hypothèse prudente : la plus longue durée. |
| Source « ragionline » introuvable | Faible | Certaine | L'agent 02 doit l'identifier. Des solutions de repli existent (Vautier, Jennesseaux). |
| Absence de conditions propres à l'API AELF ; dépendance à un service non libre | Moyenne | Moyenne | Fonctionnement en ligne requis, antifonction NonFreeNet, refus possible. |
| Contexte canonique de la forme extraordinaire (*Traditionis custodes*, *Responsa* de 2021) ; imprimatur difficile à obtenir | Moyenne (réputation et conscience) | Moyenne | La politique actuelle n'a pas été revérifiée. |
| Revendications de l'APSA et de la LEV sur les éditions typiques (LA n° 111) | Faible | Faible | Elles visent les éditions destinées à l'usage liturgique. |
| Calendrier exact de la vérification Android en 2027 ; effet sur F-Droid | Moyenne pour B et C | Certaine en 2027 | adb reste libre ; compte « distribution limitée ». |
| Droit italien du patrimoine culturel ; conditions Gallica pour la réutilisation commerciale | Faible à moyenne | Faible pour une application gratuite | Non vérifié [S]. |
| RGPD en cas de serveur ou de télémétrie (article 9) | Haute | Évitable | Architecture sans serveur ni télémétrie recommandée. |
| Fuite du journal par la sauvegarde cloud, les rapports de plantage ou les notifications | Haute | Moyenne si on ne la traite pas | Voir 2.7. |
| **Limite de cette étude** | — | — | Le budget de recherche web commun a été épuisé en cours de mission. Les points [C] et [S] sont à revérifier, notamment : la CJUE C-252/21, Feder, Jennesseaux, Gallica, l'Italie, le DSA, la dépréciation de Jetpack Security et l'imprimatur de l'application « Confession ». |

---

## 6. Sources

**Droit français et de l'UE**
- CPI consolidé (édition codes.droit.org du 20/09/2026, dernière modification le 05/08/2026, à partir des API Légifrance) : https://codes.droit.org/PDF/Code%20de%20la%20propri%C3%A9t%C3%A9%20intellectuelle.pdf. Articles consultés : L112-3, L113-2, L113-5, L122-5, L122-5-3, L123-1 à L123-4, L123-8 à L123-10, L341-1, L342-1 à L342-3, L342-5 [V]
- Légifrance, CPI : https://www.legifrance.gouv.fr/codes/texte_lc/LEGITEXT000006069414
- Directive (UE) 2019/790, articles 4 et 14 et considérants 18 et 53 : https://eur-lex.europa.eu/legal-content/FR/TXT/?uri=CELEX:32019L0790 [V]
- Europeana Copyright Community, page sur la transposition française de l'article 14 : https://europeana.atlassian.net/wiki/spaces/EF/pages/2406056122/France [V]
- Prorogations de guerre et Cass. 1re civ., 27 févr. 2007 (n° 280 et 281) : https://fr.wikipedia.org/wiki/Prorogations_de_guerre [V, source secondaire]
- RGPD (règlement 2016/679 : art. 2, 9, 25 et 32, considérant 18) : https://eur-lex.europa.eu/eli/reg/2016/679/oj [C]
- CJUE, C-252/21, Meta Platforms c/ Bundeskartellamt, 4 juillet 2023 : https://curia.europa.eu/juris/liste.jsf?num=C-252/21 [C]
- CNIL, recommandations sur les applications mobiles : https://www.cnil.fr/fr/applications-mobiles-la-cnil-publie-ses-recommandations-pour-mieux-proteger-la-vie-privee [V]

**Sources de contenu**
- Clairval :
  - accueil : https://www.clairval.com/
  - mentions légales : https://www.clairval.com/mentions-legales
  - présentation de la Lettre : https://www.clairval.com/la-lettre-spirituelle/presentation-de-la-lettre/
  - archives : https://www.clairval.com/lettres-en-ligne-6/
  - lettre consultée : https://www.clairval.com/saint-joseph-vaz/
  - robots.txt : https://www.clairval.com/robots.txt
  - contact : https://www.clairval.com/contact/

  [V]
- Divinum Officium :
  - dépôt : https://github.com/DivinumOfficium/divinum-officium
  - licence : https://github.com/DivinumOfficium/divinum-officium/blob/master/LICENSE
  - crédits : https://github.com/DivinumOfficium/divinum-officium/blob/master/web/www/horas/Help/credits.html
  - sources du missel : https://github.com/DivinumOfficium/divinum-officium/blob/master/web/www/missa/source.txt

  [V]
- AELF :
  - CGU : https://www.aelf.org/page/conditions-generales-dutilisation
  - mentions légales : https://www.aelf.org/page/mentions-legales
  - API : https://api.aelf.org/

  [V]
- Application AELF :
  - code : https://github.com/HackMyChurch/aelf-dailyreadings
  - fiche F-Droid : https://f-droid.org/fr/packages/co.epitre.aelf_lectures/

  [V]
- Prions en Église, mentions légales : https://www.prionseneglise.fr/mentions-legales [V]
- ICEL : https://icelweb.org/PubPolicy.PDF et https://www.icelweb.org/copyright.htm [V, via résumé de recherche]
- Wikisource :
  - Bible Crampon 1923 : https://fr.wikisource.org/wiki/Bible_Crampon_1923
  - Imitation : https://fr.wikisource.org/wiki/L%E2%80%99Imitation_de_J%C3%A9sus-Christ
  - Ignace de Loyola : https://fr.wikisource.org/wiki/Auteur:Ignace_de_Loyola

  [V]
- Wikipédia :
  - Gaspar Lefebvre : https://fr.wikipedia.org/wiki/Gaspar_Lefebvre
  - Augustin Crampon : https://fr.wikipedia.org/wiki/Augustin_Crampon
  - Louis-Claude Fillion : https://fr.wikipedia.org/wiki/Louis-Claude_Fillion

  [V]
- GregoBase : https://gregobase.selapa.net/ [V]

**Droit de l'Église**
- CIC, canons 822 à 832 (FR) : https://www.vatican.va/archive/cod-iuris-canonici/fra/documents/cic_libro3_cann822-832_fr.html [V]
- CIC, canons 834 à 839 (FR, version antérieure à 2017) : https://www.vatican.va/archive/cod-iuris-canonici/fra/documents/cic_libro4_cann834-839_fr.html [V]
- *Magnum principium* (2017), nouveau canon 838 : https://www.vatican.va/content/francesco/en/motu_proprio/documents/papa-francesco-motu-proprio_20170903_magnum-principium.html [V]
- *Liturgiam authenticam*, n° 111 à 117 : https://www.vatican.va/roman_curia/congregations/ccdds/documents/rc_con_ccdds_doc_20010507_liturgiam-authenticam_fr.html [V]
- *Responsa ad dubia* du 18/12/2021 : https://press.vatican.va/content/salastampa/en/bollettino/pubblico/2021/12/18/211218c.html [V]

**Distribution et licences**
- Google Play :
  - politique sur les données utilisateur : https://support.google.com/googleplay/android-developer/answer/10144311
  - section « Sécurité des données » : https://support.google.com/googleplay/android-developer/answer/10787469
  - propriété intellectuelle : https://support.google.com/googleplay/android-developer/answer/9888072
  - exigences de test : https://support.google.com/googleplay/android-developer/answer/14151465

  [V]
- Vérification des développeurs Android : https://developer.android.com/developer-verification et https://developer.android.com/developer-verification/guides/faq [V]
- F-Droid : https://f-droid.org/docs/Inclusion_Policy/ et https://f-droid.org/docs/Anti-Features/ [V]
- FAQ de l'OFL : https://openfontlicense.org/ofl-faq/ [V]
- Liste des licences de la FSF : https://www.gnu.org/licenses/license-list.html [V]
- REUSE : https://reuse.software/ [C]

---

## Annexe A — Projet de courriel à l'abbaye Saint-Joseph de Clairval (NE PAS ENVOYER sans validation)

**À :** abbaye@clairval.com (ou le formulaire https://www.clairval.com/contact/, objet « autre »)
**Objet :** Demande d'autorisation — reproduction de la Lettre spirituelle dans une application de prière gratuite

> Mon Révérend Père,
>
> Lecteur fidèle de la Lettre spirituelle de l'abbaye, je me permets de vous écrire au sujet d'un projet personnel pour lequel votre autorisation m'est nécessaire.
>
> Catholique et développeur, je prépare bénévolement une application Android de prière, provisoirement nommée « Paroissien », sur le modèle de l'ancien paroissien romain. Elle réunira le missel, des prières, l'Imitation de Jésus-Christ et des lectures spirituelles, avec une mise en page proche du livre imprimé. Elle est gratuite et sans publicité. Elle ne collecte aucune donnée et fonctionne entièrement hors ligne.
>
> J'aimerais y proposer la lecture des Lettres spirituelles publiées sur clairval.com. Votre site indique qu'une autorisation est nécessaire pour publier la Lettre, et je viens donc vous la demander, dans les conditions suivantes que je vous soumets.
>
> 1. **Intégrité.** Le texte serait reproduit intégralement, sans coupure ni modification. Seule la présentation serait adaptée à l'écran (typographie, pagination, lettrines).
> 2. **Mention de l'abbaye.** Chaque Lettre porterait la mention « © Abbaye Saint-Joseph de Clairval », sa date et un lien vers sa page d'origine. L'application renverrait aussi visiblement vers l'abonnement gratuit, vers la page de dons et vers la demande de messes.
> 3. **Gratuité.** Aucune vente, aucune publicité, aucune exploitation commerciale.
> 4. **Discrétion.** Je n'utiliserais ni le nom ni le blason de l'abbaye pour laisser croire que vous approuvez l'application, sauf si vous me l'accordez expressément.
> 5. **Retrait.** Je retirerais une Lettre, ou l'ensemble, sur simple demande de votre part.
> 6. **Respect de votre site.** L'application téléchargerait les Lettres lentement, en respectant vos consignes techniques. Si vous le préférez, je pourrais aussi utiliser une archive que vous me fourniriez.
>
> Pour être tout à fait clair, la diffusion se ferait en trois étapes, et je vous serais reconnaissant de me dire lesquelles vous acceptez :
> (a) sur mon propre téléphone ;
> (b) auprès de quelques proches ;
> (c) plus tard, et seulement si vous en êtes d'accord, en diffusion publique gratuite (Google Play, F-Droid).
>
> J'aurais par ailleurs quelques questions.
> - Puis-je reproduire les illustrations et photographies des Lettres, ou faut-il m'en tenir au texte ?
> - Puis-je proposer la lecture à voix haute par la synthèse vocale du téléphone et le partage de courts extraits, toujours avec la mention de l'abbaye ?
> - L'autorisation peut-elle valoir pour les autres langues de la Lettre ?
> - Quelle formule de crédit souhaitez-vous voir figurer ?
>
> Je serais heureux de vous présenter l'application avant toute diffusion, et je me conformerai bien sûr à vos conditions. Une réponse par courriel me suffira.
>
> En vous remerciant de l'attention que vous voudrez bien porter à cette demande, et en me recommandant à vos prières, je vous prie d'agréer, mon Révérend Père, l'expression de mon profond respect.
>
> [Prénom Nom]
> [Adresse postale]
> [Courriel] — [Téléphone, facultatif]

*Remarques :*
- La formule d'appel peut devenir « Révérendissime Père Abbé » si le courriel s'adresse à l'abbé.
- Conserver la réponse (courriel avec ses en-têtes complets, ou courrier) dans le registre de provenance, avec la référence `autorisation-écrite:clairval-AAAA-MM-JJ`.
- Des demandes du même type sont à préparer pour l'AELF (formulaire de contact du site) et pour l'abbaye Sainte-Madeleine du Barroux, si les décisions 3 et 4 le requièrent.
