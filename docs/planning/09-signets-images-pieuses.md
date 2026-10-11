# 09 — Signets de tissu, images pieuses et annotations

> Agent 09 · rapport de planification · 11 octobre 2026
> Périmètre : rubans, images pieuses (catalogue et photos personnelles), surlignages, notes, intentions, modèle de données des annotations, écran « Mes signets ».
> Hors périmètre, avec renvois : calcul des couleurs liturgiques (05), moteur de lecture et localisateurs (06), tourne-page (07), navigation générale (08), architecture (10), pipeline de contenu (11), droit (12), textes des prières (14).

**Légende.** **[V]** : vérifié sur la source citée, ou mesuré par moi le 11/10/2026. **[S]** : supposé, déduit ou issu de la mémoire ; à confirmer. **[P]** : proposition de conception, sans valeur de fait.

> **Note de méthode.** Le quota de recherches web, partagé entre les agents, a été épuisé en cours de mission. Les vérifications restantes ont été faites par consultation directe des pages et des API des musées. Quelques points restent donc marqués [S] et sont repris en §5.

---

## 1. Résumé

- **Rubans attachés au livre, images pieuses libres.** Chaque livre a ses propres rubans « cousus dans la reliure » : 6 dans le Missel, 2 à 4 ailleurs. Une image pieuse est un objet libre, qu'on fait passer d'un livre à l'autre comme dans la réalité [P]. Les missels réels ont 5 ou 6 rubans : Baronius 1962 en a 6, Angelus 1962 en a 5 et le nouveau Missel romain français de 2021 en a 5 [V].
- **Un 7ᵉ ruban, « messe du jour », dans le Missel.** Il se place tout seul sur le propre du jour et prend la couleur liturgique du jour, fournie par l'agent 05. On le reconnaît à un ferret doré [P]. Dans les missels réels, la couleur d'un ruban ne sert qu'à le distinguer : c'est l'usager qui lui donne un rôle. Le guide Lasance de 1946 propose ainsi or = Ordinaire, vert = dimanches, etc. [V].
- **Un rendu procédural et sobre.** Une petite tuile en niveaux de gris (gros-grain ou satin) est teintée à la couleur du ruban. L'extrémité est coupée en V. La queue dépasse sous la page, dans une « tranche de queue » réservée **au-dessus** de la zone de gestes système, que l'application ne peut pas neutraliser [V]. Les queues des rubans placés avant la page courante sortent à gauche, celles des rubans placés après sortent à droite [P].
- **Quatre gestes.** Un *tap* sur la queue ouvre le livre au ruban. Glisser la queue vers la page place le ruban ici, et tirer le ruban hors de la page le range. Après chaque saut, un « doigt glissé » permet de revenir en un geste, indispensable pour les allers-retours entre l'Ordinaire et le Propre [P].
- **Des ancres sans numéro de page.** Une ancre combine `blockId` stable, décalage en caractères, citation (`exact` + 32 caractères avant et après, comme chez Hypothesis) et progression de secours. Le ré-ancrage procède en cascade, et une annotation devenue orpheline n'est **jamais** supprimée en silence [V pour les techniques, P pour l'assemblage].
- **Un catalogue CC0 d'une soixantaine d'images.** Il puise d'abord dans les institutions qui publient en CC0 (Art Institute of Chicago, Met, NGA, Rijksmuseum, Paris Musées). J'ai vérifié une quarantaine d'œuvres par API (§3.2.4). Wikimedia Commons ne sert qu'en appoint, car le statut des photographies d'œuvres 2D (« PD-Art ») est jugé « *Inconclusive* » pour la France [V]. **Gallica et le MuCEM sont exclus du MVP** : réutilisation commerciale payante pour l'un, aucune licence ouverte pour l'autre [V].
- **Le poids des images, mesuré.** En WebP à 1 440 px de côté long (q75), une image pèse environ 150 à 200 Ko, plus une vignette de 360 px d'environ 15 à 25 Ko. Il faut donc compter environ 5 Mo pour 30 images embarquées et environ 16 Mo pour un paquet de 100 [V, mesure sur 3 échantillons]. L'AVIF n'est décodé nativement qu'à partir d'Android 12 [V] : on ne l'envisagera que si `minSdk` ≥ 31.
- **Photo personnelle sans permission.** On passe par le sélecteur de photos du système ou par l'intent de l'appareil photo, sans déclarer `CAMERA`, sous peine de `SecurityException` [V]. Viennent ensuite un recadrage en 4 coins (`Matrix.setPolyToPoly`), la capture du recto et du verso, puis un ré-encodage en WebP qui **supprime les métadonnées EXIF**, dont la position GPS [P].
- **Local d'abord.** Les données vivent dans Room. Un fichier `.paroissien` (ZIP) permet l'export et l'import par le sélecteur de fichiers du système. L'Auto Backup est chiffré de bout en bout, et `disableIfNoEncryptionCapabilities="true"` empêche toute sauvegarde dans le nuage non chiffrée [V]. Le **quota de 25 Mo** [V] oblige à plafonner les photos. Les intentions, données les plus sensibles, peuvent être exclues de la sauvegarde.
- **« Mes signets » montre des objets.** On y voit la tranche de chaque livre avec ses rubans pendants, une boîte à images en éventail, un carnet de fiches, et un mémento des vivants et des défunts (maquettes en §3.6).

---

## 2. Constats détaillés

### 2.1 Les rubans dans les missels réels

| Ouvrage | Nombre de rubans | Statut |
|---|---|---|
| *Daily Missal 1962*, Baronius Press | 6, de couleurs différentes | [V] Baronius, Latin Mass Society, Clear Creek |
| *Roman Catholic Daily Missal 1962*, Angelus Press | 5, « liturgically-colored non-fraying » | [V] fiches de revendeurs (Cenacle Press) |
| Missel romain, nouvelle traduction française (2021) | 5, « de différentes couleurs » | [V] note de l'évêché d'Edmundston (PDF) |
| Missel quotidien FE, Éd. Sainte-Madeleine (2013) | 6 sur un exemplaire d'occasion | [S] fiche de libraire |

- **[V]** Les fidèles ne s'accordent sur aucun schéma d'attribution. Sur le blog de Fr. Z (« QUAERITUR: Bought a Baronius Missal… how to set up the ribbons »), chacun a le sien. Le schéma le plus « canonique » cité vient du guide Lasance de 1946 :
  - or : Ordinaire ;
  - vert : dimanches après la Pentecôte et après l'Épiphanie ;
  - blanc : fêtes du Seigneur, de la Vierge et des saints non martyrs ;
  - rouge : Pentecôte, Passion et martyrs ;
  - violet : Avent, Carême et vigiles ;
  - noir : défunts.
- **[V]** D'autres réservent des rubans à la préparation à la messe, aux préfaces, au Commun, à des litanies… Un lecteur note que les rubans **s'usent** à force d'être posés au même endroit, et qu'il marque donc les sections fixes autrement.
- **Conclusion [P].** La couleur d'un ruban est d'abord un **identifiant**. Le rôle d'un ruban (Ordinaire, Propre, Préfaces…) relève de l'usager. L'application peut proposer des préréglages, sans les imposer.
- **Le précédent numérique [V, rapport 13].** L'application Missale Romanum présente ses onglets comme « l'équivalent des rubans d'un missel » qui mémorisent leur position. **Aucune liseuse ne propose de ruban visible sur la tranche** [S, rapport 13]. Le Kindle Colorsoft offre 8 couleurs de signets depuis 2026, et ses lecteurs demandent à pouvoir les nommer [S, rapport 13].

### 2.2 Les couleurs liturgiques et la palette des rubans

- **[V]** La PGMR (n. 346, version anglaise adaptée aux États-Unis, publiée sur vatican.va) prévoit les couleurs suivantes :
  - blanc : temps de Pâques et de Noël, fêtes du Seigneur hors Passion, Vierge, anges, saints non martyrs ;
  - rouge : Rameaux, Vendredi saint, Pentecôte, Passion, apôtres, martyrs ;
  - vert : temps ordinaire ;
  - violet : Avent et Carême ;
  - rose : « *where it is the practice* », à *Gaudete* et *Laetare* ;
  - ornements « plus précieux » aux jours solennels, « *even if not of the color of the day* » ; d'où l'**or**, explicitement permis aux États-Unis ;
  - noir : à confirmer pour l'édition typique (Masses pour les défunts, là où c'est l'usage) [S]. Il est d'usage constant dans le rite de 1962 (Vendredi saint, défunts) [S, agent 05].
- **[V]** Le **bleu** n'est pas une couleur du rite romain. Il ne relève que d'un **privilège** (décret de la Congrégation des Rites du 12/02/1864) accordé à l'Espagne et à d'anciens territoires espagnols, pour l'Immaculée Conception. Sa portée actuelle est discutée (Wikipédia, EWTN, Fr. Z).
- **Conséquence [P].** Le « bleu marial » peut exister comme ruban **de dévotion** au choix de l'usager. Le ruban automatique ne le prend jamais, sauf si l'agent 05 gère un calendrier propre à l'Espagne.
- **Le calendrier renvoie parfois plusieurs couleurs** [S, agent 05]. Exemples : « violet ou rose », « blanc ou or », « violet ou noir » pour les défunts en forme ordinaire. Le ruban automatique a besoin d'une couleur **principale** et de couleurs **admises**.

### 2.3 Les contraintes Android pour les gestes en bas d'écran

- **[V]** `View.setSystemGestureExclusionRects` : « *the system will put a limit of 200dp on the vertical extent of the exclusions* ». Cette exclusion concerne les gestes latéraux (retour). Le geste « accueil » en bas d'écran fait partie des *mandatory system gestures*, que l'application ne peut pas neutraliser [S, d'après l'API `WindowInsets.Type.mandatorySystemGestures`].
- **Conséquence [P].** Les queues des rubans se posent dans une bande de 32 à 40 dp située **au-dessus** de `WindowInsets.systemGestures` / `navigationBars`. Glisser vers le haut depuis le bord inférieur de l'écran est exclu. Une queue se tire depuis la bande elle-même, à au moins 16 dp du bord.
- **[S, agent 07]** Les bords gauche et droit servent à tourner les pages. Un *tap* sur la tranche reste compatible avec ces gestes, mais un glisser horizontal ne l'est pas. D'où le choix de faire dépasser les images pieuses par la **tête** (le bord supérieur) de la page (§3.2).

### 2.4 Les images pieuses comme objets physiques

- **[V]** Les notices du MuCEM donnent des formats typiques de 11 à 13,9 cm de haut pour 6 à 8,5 cm de large :
  - Cœur Sacré de Jésus, Paris 1924 : 11 × 6 cm ;
  - Sainte Louise de Marillac : 13 × 7 cm ;
  - Saint Martin à Ligugé : 13,9 × 8,5 cm.
- **[V]** Côté anglo-saxon, les cartes font « *almost always smaller than 3 x 5 inches* » (Burns Library, Boston College). Les fiches de revendeurs indiquent couramment 2,5 × 4,25 à 4,5 pouces.
- **Conséquence [P].** On retient un **rapport largeur/hauteur de 0,58** (≈ 7 × 12 cm) comme gabarit de carte, avec une variante « canivet » plus carrée (rapport de 0,75 à 0,8), car beaucoup de canivets du XVIIIᵉ siècle sont plus larges.
- **[S]** Les images du XIXᵉ siècle ont souvent, au verso, une prière et une mention d'**indulgence** chiffrée en jours. Ces mentions sont caduques depuis l'*Enchiridion indulgentiarum* de 1968 (voir le rapport 14). Il faut les afficher comme historiques sur une photo personnelle et ne pas les reproduire dans les versos du catalogue.
- **[V]** Commons a une catégorie « *In memoriam cards* » : les images mortuaires sont une variante d'usage. C'est un cas fort pour la photo personnelle : l'image du grand-père défunt glissée au *Memento des morts*.

### 2.5 Sources d'images : licences et API

| Source | Licence des images du domaine public | Accès | Statut | Utilité pour Paroissien |
|---|---|---|---|---|
| **Art Institute of Chicago** | CC0 pour les données (vu dans `info.license_text` de la réponse API) ; images PD réputées CC0 | API `api.artic.edu/api/v1`, filtre `query[term][is_public_domain]=true`, IIIF `…/iiif/2/{image_id}/full/843,/0/default.jpg` (1 686 px pour le PD), 60 req/min, en-tête `AIC-User-Agent` souhaité | [V] API et données ; [S] licence des images (page *image-licensing* en 403) | **Excellente** : peintures et gravures de dévotion, pertinence des résultats |
| **Metropolitan Museum** | CC0 (« *Creative Commons Zero* »), champ `isPublicDomain` | API sans clé, `/v1/objects/{id}`, `primaryImage` / `primaryImageSmall` ; **`/search` v1 retirée le 1ᵉʳ/10/2026**, remplacée par `/v1.1/search` (paginée) ; limite de 80 req/s. J'ai reçu un 403 temporaire après une rafale. | [V] | Très bonne : canivet au Sacré-Cœur, gravures, primitifs |
| **National Gallery of Art** | CC0 pour les images PD (« *free of charge for any use, commercial or non-commercial* »). Les données CSV sont en CC0, les fichiers images n'en font pas partie. | Pas d'API REST : CSV sur GitHub (`opendata`), IIIF ; 53 000 images versées sur Commons | [V] politique et README ; [S] détail IIIF | Bonne, hors ligne via le CSV traité par le pipeline |
| **Rijksmuseum** | CC0 (« *waives these rights by using the Creative Commons Zero* ») ; crédit souhaité | Nouvelle Search API `data.rijksmuseum.nl/search/collection`, « *No API key is needed* », Linked Art, IIIF, OAI-PMH ; ancienne API dépréciée | [V] | Moyenne : 16 résultats « Heilig Hart » et 9 « bidprentje » avec image [V] |
| **Paris Musées** | CC0 pour les œuvres 2D du domaine public (programme *Open Content* lancé le 8/1/2020 ; 253 106 notices filtrées « images libres de droit ») ; charte de bonnes pratiques demandant la citation | Portail et API « pour usage professionnel », sans documentation trouvée | [V] programme ; [S] API | À explorer (imagerie religieuse de Carnavalet) ; la recherche du portail n'est pas pertinente (« canivet » → 413 254 résultats sans rapport) |
| **Wikimedia Commons** | Variable selon les fichiers (`extmetadata.LicenseShortName`). Pour les photos fidèles d'œuvres 2D (PD-Art) : US OK, UK OK (*THJ v Sheridan* 2023), Allemagne « *OK in most cases* », **France « *Inconclusive* »** | API MediaWiki `prop=imageinfo&iiprop=url|extmetadata&iiurlwidth=…`, User-Agent descriptif exigé | [V] | En appoint, au cas par cas ; garder la provenance d'origine |
| **Gallica (BnF)** | Réutilisation **non commerciale** libre avec mention de la source ; réutilisation **commerciale payante** sous licence, au sens large (« revente… fourniture de services ») | Page des conditions bloquée par Cloudflare (403) ; informations tirées de sources secondaires concordantes (LinuxFr, ActuaLitté, question écrite AN n° 23878) | [V] sources secondaires ; [S] version 2026 | **À exclure du MVP**, avis de 12 nécessaire |
| **MuCEM** | Aucune licence ouverte sur les notices ; mentions légales : usage « *personnel et privé* », « *Pour d'autres utilisations, veuillez nous consulter* » | Notices en ligne | [V] | Référence documentaire seulement (formats, iconographie) |

**Le corpus « holy cards » de Commons, échantillonné par API le 11/10/2026 [V].**

| Catégorie | Fichiers | Licences | Résolution |
|---|---|---|---|
| `Category:Holy cards` | 15 | — | — |
| `Holy cards depicting saints` | 89 (50 échantillonnés) | PD 40, CC BY-SA 4.0 7, CC0 2 | Souvent 300 à 750 px de côté long |
| `Holy cards depicting Jesus` | 46 | PD 37, CC BY-SA 9 | Souvent 300 à 750 px de côté long |
| `Canivet` | 39 | PD 24, CC BY-SA 4.0 8, CC BY 4.0 5, CC0 1 | Souvent 300 à 750 px de côté long |
| `Holy cards from France` | 5 | — | — |

- **Les pépites existent.** Exemple : « *Sacré Coeur de Jésus – image pieuse Bouasse Lebel – fin XIXème siècle* », 3031 × 5336 px, PD.
- **[S]** Beaucoup de *santini* italiens des années 1930 et 1940 sont marqués « PD » sans auteur identifié. Il faudra les vérifier un par un : illustrateur connu ou non, mort depuis plus de 70 ans ou non.

### 2.6 Le poids des images, mesuré sur des échantillons CC0

J'ai mesuré trois échantillons :

- Memling, *Vierge à l'Enfant*, AIC 16295 : une peinture ;
- Dürer, *Homme de douleurs assis*, AIC 79896 : une gravure, donc riche en hautes fréquences ;
- canivet au Sacré-Cœur, Met 775447, dans la seule taille « web-large » de 500 × 624 px.

Méthode : redimensionnement Lanczos avec Pillow 12.3, WebP en `method=6`. Mon Pillow n'encode pas l'AVIF, je n'ai donc pas pu le mesurer. Poids en Ko [V] :

| Côté long | WebP q80 | WebP q70 | JPEG q85 |
|---|---|---|---|
| 1 600 px | 211 – 245 | 145 – 183 | 365 – 369 |
| 1 200 px | 125 – 158 | 85 – 124 | 213 – 225 |
| 800 px | 57 – 80 | 39 – 65 | 97 – 110 |
| 320 px | 12 – 28 (canivet : 28) | 9 – 23 | 19 – 33 |

- Les gravures pèsent environ 20 % de plus que les peintures. Le WebP fait gagner environ 40 % par rapport au JPEG q85.
- **[S]** L'AVIF ferait gagner 20 à 30 % de plus à qualité perçue égale : c'est l'ordre de grandeur courant, non mesuré ici.
- **[V]** L'AVIF est décodé nativement à partir d'**Android 12** (page « Android 12 features »). Le WebP avec perte est pris en charge partout ; le WebP sans perte et avec transparence, depuis l'API 18 [S].

### 2.7 La photo personnelle d'une image pieuse

- **[V] Sélecteur de photos.** Il fonctionne nativement à partir d'Android 11 (API 30) avec les mises à jour système. Un rétroportage via Google Play services couvre les API 19 à 29.
  - Contrat `PickVisualMedia` (androidx.activity ≥ 1.7.0), sans permission de stockage dans les exemples.
  - Repli automatique sur `ACTION_OPEN_DOCUMENT`.
  - L'accès est temporaire, sauf `takePersistableUriPermission`, plafonné à 5 000 autorisations.
  - **[P]** On copie donc l'image dans le stockage privé ; on ne garde pas l'URI.
- **[V] Appareil photo.** Avec `ACTION_IMAGE_CAPTURE`, « *if you app targets M and above and declares as using the CAMERA permission which is not granted, then attempting to use this action will result in a SecurityException* ».
  - **[P]** Il ne faut donc **pas** déclarer `CAMERA` et passer par `TakePicture` avec un `FileProvider`.
- **[V] ML Kit Document Scanner.**
  - Fonctions : détection des bords, recadrage, rotation et filtres.
  - Permission : « *No camera permission is required* ».
  - Traitement : « *operates on-device* ».
  - Dépendance : Google Play services, qui fournit l'interface et les modèles. C'est **incompatible avec F-Droid et les appareils dégooglisés** [S].
- **[S] Alternative sans dépendance.** On peut recadrer en 4 coins à la main avec `android.graphics.Matrix.setPolyToPoly`, qui accepte jusqu'à 4 points et donc une transformation perspective. Il n'y a pas besoin d'OpenCV.

### 2.8 L'ancrage des annotations : état de l'art

- **[V] W3C Web Annotation Data Model (Recommandation du 23/02/2017).**
  - Sélecteurs : `TextQuoteSelector` (`exact`, `prefix`, `suffix`), `TextPositionSelector` (`start` inclus, `end` exclu), `FragmentSelector`, `CssSelector`, `RangeSelector`, etc.
  - Plusieurs sélecteurs sont autorisés : « *Multiple Selectors SHOULD select the same content* ».
  - Motivations : `bookmarking`, `highlighting`, `commenting`, `tagging`, `describing`.
- **[V] Readium Locator.** Il se compose de `href`, `type`, `locations` (`fragments`, `progression`, `position`, `totalProgression`) et `text` (`before`, `highlight`, `after`). Le rapport 06 en est le cadre naturel.
- **[V] Hypothesis, « Fuzzy Anchoring ».** Hypothesis stocke trois sélecteurs : Range, Position et Quote, avec 32 caractères de contexte. Le ré-ancrage procède en cascade :
  1. le Range (XPath) ;
  2. la Position globale, vérifiée par la citation ;
  3. une recherche floue guidée par le contexte (le préfixe près du début attendu, le suffixe près de la fin) ;
  4. une recherche floue de la citation seule.

  La recherche floue repose sur diff-match-patch (Bitap et Myers).
- **[V, rapport 13] Retours d'utilisateurs des liseuses.**
  - Play Livres et Librera : « signets qui renvoient à la mauvaise page ». Librera distingue les signets « fixes » des signets « flottants » ancrés au texte.
  - Kindle : un carnet de notes qui met des minutes à s'ouvrir sur une Bible très annotée.
  - Leçon : **ancrer au texte** et **indexer, paginer** la liste des annotations.
- **[V, rapport 14]** L'agent 14 prévoit une « ancre stable par paragraphe » et une « table de renommage d'ancres » dans le manifeste de chaque paquet. Mon modèle s'appuie dessus.

### 2.9 La sauvegarde Android

- **[V] Quota et stockage.**
  - « *up to 25 MB of backup data per app user* ».
  - Au-delà : `onQuotaExceeded()`, sans sauvegarde dans le nuage.
  - Les données sont stockées dans un dossier privé de Google Drive, hors du quota de l'utilisateur. Seule la dernière sauvegarde est conservée.
- **[V] Chiffrement et déclenchement.**
  - « *end-to-end encrypted on devices running Android 9 or higher using the device's PIN, pattern, or password* ».
  - La sauvegarde part au plus une fois toutes les 24 h, appareil inactif, en Wi-Fi.
  - L'application est arrêtée pendant la sauvegarde.
- **[V] Contenu par défaut.** Sont inclus les SharedPreferences, `filesDir`, les bases de données et `getExternalFilesDir`. Sont exclus `cacheDir`, `codeCacheDir` et `noBackupFilesDir`.
- **[V] Règles d'extraction (Android 12+).**
  - `dataExtractionRules` distingue `<cloud-backup>`, `<device-transfer>` et, depuis Android 16 QPR2, `<cross-platform-transfer platform="ios">`.
  - `disableIfNoEncryptionCapabilities="true"` empêche l'envoi vers le nuage sans chiffrement possible, sans bloquer le transfert d'appareil à appareil.
  - Pas de joker dans les chemins ; `exclude` l'emporte sur `include`.
  - Il faut garder `fullBackupContent` pour Android 11 et antérieurs.
- **[V] Personnalisation.**
  - Un `BackupAgent` permet de surcharger `onFullBackup()`, avec `android:fullBackupOnly="true"`.
  - Ses callbacks `onRestoreFinished()` et `onQuotaExceeded()` s'exécutent en mode restreint : sans fournisseurs de contenu et sans la sous-classe d'`Application`.
- **[S]** Seedvault, l'outil de sauvegarde de GrapheneOS, LineageOS et CalyxOS, s'appuie sur le même cadre de sauvegarde. Les règles déclarées valent donc aussi hors de Google.

---

## 3. Recommandations

### 3.1 Les rubans

#### 3.1.1 Nombre, couleurs et appartenance [P]

| Livre | Rubans par défaut | Maximum |
|---|---|---|
| Missel (chaque rite est un livre distinct) | 6 manuels + 1 « messe du jour » automatique | 8 |
| Bible | 4 | 8 |
| Imitation, Exercices, recueil de prières | 2 | 6 |
| Lettres de Clairval (une collection) | 1 par collection, plus le marque-page de lecture automatique (06) | 4 |

- **Couleurs du jeu de base** : vert, rouge, violet, blanc (ivoire à liseré), rose et noir, comme dans le brief.
- **Couleurs en option** : or et « bleu marial », non liturgique et présenté comme tel.
- **Une couleur par ruban dans un même livre**, pour garder la mémoire musculaire.

**Rubans propres à chaque livre ou communs à tous ?**

| | Propres à chaque livre (recommandé) | Communs à tous |
|---|---|---|
| Fidélité au papier | Totale : le ruban est collé à la tranchefile | Faible : un ruban « voyage » entre les livres |
| Clarté | « Le vert du Missel » est sans ambiguïté | « Le vert » renvoie à un livre ou à un autre |
| Auto-ruban | Un par Missel ou par rite, chacun avec sa couleur (le même jour peut être vert en FO et violet en FE) | Conflit entre les rites |
| Vue d'ensemble | Assurée par l'écran « Mes signets » | Native |

**Préréglage proposé pour le Missel 1962, modifiable et inspiré de Lasance** [P] :

- auto : propre du jour ;
- vert : Ordinaire ;
- rouge : préfaces ;
- violet : préparation et action de grâces ;
- blanc : sanctoral ou Commun ;
- noir : messe des défunts ;
- rose : libre.

#### 3.1.2 Le ruban automatique « messe du jour » [P]

- **Position.** Le ruban se pose sur l'ancre du propre de la célébration du jour, dans le rite du livre :
  - l'agent 05 fournit `(date, rite) → celebrationId, couleurs[principale, admises]` ;
  - les agents 03 et 11 fournissent `celebrationId → blockId` du début du propre.
- **Couleur.** Il prend la couleur principale. Si plusieurs couleurs sont admises, par exemple rose ou violet, un appui long permet de choisir.
- **Moment du déplacement.** Il se déplace à la première ouverture du jour, sans tâche de fond (sobriété). **Option** : « préparer le dimanche dès le samedi à 16 h », pour la messe anticipée ou pour la préparation à la maison.
- **Signe distinctif.** Un **ferret doré** au bout de la queue (ou un petit fil d'or) dit qu'il bouge seul. C'est le seul écart assumé au réalisme.
- **Débrayage.** Si on le déplace à la main, il se « fige » jusqu'au lendemain, ou jusqu'à « Revenir à la messe du jour ».
- **[S] Plus tard.** Un second ruban automatique « préface du jour », si l'agent 03 expose la préface propre.

#### 3.1.3 Le rendu [P]

- **Sur la page.** Le ruban part de la tête du pli, côté reliure, longe la gouttière (10 à 12 dp de large, sans masquer le texte : la marge intérieure doit lui faire place, à voir avec 06 et 07), puis sort par la tranche de queue.
- **Le pli.** Un court segment plus sombre de 4 à 6 dp, avec une ombre portée, montre le ruban qui **se replie sur le bord de la page**.
- **La queue.** Elle pend dans une « tranche de queue » de 32 à 40 dp, dessinée comme le bord du bloc de pages (filets fins, tranche dorée pour le Missel) et posée au-dessus des encarts système.
- **La largeur.** 14 à 18 dp, soit l'équivalent d'un ruban de 6 à 7 mm.
- **L'extrémité.** Coupe en **V (queue d'aronde)** par défaut. Autres coupes au choix : biais, droite légèrement effilochée (quelques fils dessinés en traits fins).
- **La texture.** Une seule tuile de 32 × 32 px en niveaux de gris, soit deux variantes (« gros-grain » à côtes horizontales serrées, « satin » à reflet longitudinal), appliquée en `ImageShader` avec `TileMode.Repeat` et teintée par la couleur (`BlendMode.Multiply` / `Modulate`). Une seule ressource sert toutes les couleurs, sans contrainte d'API.
  - Un reflet satiné en dégradé linéaire s'y ajoute.
  - Un `RuntimeShader` AGSL, plus riche, reste en option pour l'API 33 et plus [V AGSL ≥ Android 13].
- **Les thèmes.**
  - Sombre : couleurs désaturées de 10 à 15 % ; le noir reçoit un reflet satiné, sinon il disparaît.
  - Clair et sépia : le blanc devient ivoire, avec un liseré et une ombre, sinon il disparaît sur le papier.
- **Sens de lecture des queues.** Les rubans placés **avant** la page courante pendent du côté gauche, ceux placés **après** du côté droit, comme dans un livre ouvert dont les deux blocs de pages laissent pendre leurs rubans. Les queues gardent un ordre fixe dans chaque moitié. En paysage (double page), on applique la même logique sous chaque bloc.
- **L'animation.** Pas d'animation continue. Seuls un léger balancement amorti (`spring`) au tournement de page et un glissement à la pose sont animés. Ils se désactivent avec « Réduire les animations ».

#### 3.1.4 Les interactions [P]

| Geste | Effet | Retour |
|---|---|---|
| *Tap* sur une queue | Ouvre le livre à la page du ruban, avec un feuilletage rapide (agent 07) | Petit bruit de papier facultatif |
| Puce « ↩ Revenir p. 34 » après un saut | Le « doigt glissé » : retour à la position précédente. On peut enchaîner Ordinaire ↔ Propre | Visible pendant 10 s ou jusqu'au prochain saut |
| Glisser une queue vers le haut, dans la page | « Glisser le ruban ici » : il se pose au premier bloc visible, ou au bloc sous le doigt | Retour haptique au moment où il « accroche » |
| Tirer le ruban de la gouttière vers le bas, hors de la page | Le retire ; il revient au repos en fin de livre | Annulation possible 5 s (snackbar) |
| Appui long sur une queue ou un ruban | Menu : renommer le rôle, changer la coupe, « suivre la messe du jour » (auto seulement), retirer | — |
| Barre d'outils de lecture → « Rubans » | Pour chaque ruban : « placer ici » ou « ouvrir » | Alternative sans geste |

- **Le signet rapide** (rapport 13, §8). Le *tap* dans le coin supérieur droit, standard des liseuses, pose un **coin corné**, distinct des rubans. Ce signet ponctuel, illimité, entre dans le carnet. Les rubans, eux, sont en nombre fini et gardent une **position**.
- **Accessibilité.**
  - Chaque queue est un bouton TalkBack. Exemple : « Ruban vert, Ordinaire de la messe, page de l'Offertoire. Toucher deux fois pour ouvrir. »
  - Des actions personnalisées « Placer ici » et « Retirer » complètent le bouton.
  - La couleur n'est **jamais** le seul repère : il y a aussi un libellé et un ordre fixe.
  - Les cas violet/bleu et rouge/vert, difficiles pour les daltoniens, sont testés en simulation.

### 3.2 Les images pieuses

#### 3.2.1 L'objet [P]

- **La composition.** Une carte se compose de :
  - un **cadre procédural** : bord, filet doré, dentelle vectorielle pour les « canivets », papier légèrement teinté ;
  - une **image** recadrée au rapport 0,58 ;
  - un titre ;
  - un **verso** : texte composé en vrai texte (rubriques rouges, lettrine), qu'on peut lire à voix haute (TTS), rechercher et agrandir. Le verso est soit une prière du rapport 14 par référence `prayer:…`, soit, pour une carte personnelle, la photo du verso.
- **Le crédit.** Un pied de verso discret : « Hans Memling, *Vierge à l'Enfant*, v. 1485 · Art Institute of Chicago · CC0 ». Le CC0 ne l'exige pas, mais le Rijksmuseum, la NGA et Paris Musées le demandent.
- **Dans le livre.** La carte dépasse de 8 à 12 dp par la **tête** de la page, légèrement inclinée (± 2°, angle fixe dérivé de l'identifiant de la carte). Comme pour les rubans, les cartes placées avant la page sont à gauche et celles placées après sont à droite. Au-delà de 4 cartes visibles, l'« épaisseur » est suggérée sans tout dessiner.
- **La sémantique.** Une carte placée « entre deux pages » est ancrée à un point du texte. Elle s'affiche **juste avant** la page qui contient ce point et reste donc collée au texte quand la police change.

#### 3.2.2 Les interactions [P]

| Geste | Effet |
|---|---|
| *Tap* sur le bord qui dépasse | La carte sort du livre (transition d'élément partagé), se centre et s'agrandit |
| *Tap* sur la carte ouverte, ou bouton « Retourner » | Retournement 3D (`graphicsLayer { rotationY; cameraDistance }`) vers le verso |
| Verso → « Prier » | Ouvre la prière en mode méditation ou TTS (agents 08 et 14) |
| « Remettre » / « Glisser ailleurs » / « Ranger dans la boîte » | La replace, la déplace ou la retire du livre |
| Depuis la **boîte à images** : « Glisser dans… » | Liste des livres, puis page courante ou « à la messe du jour » |
| Lecture → menu « Images » | Insère une image de la boîte à la page courante, sans geste |

#### 3.2.3 Le budget de poids [P, sur la base des mesures du §2.6]

| Variante | Côté long | Format | Poids unitaire |
|---|---|---|---|
| Vue plein écran | 1 440 px (≈ 830 px de large au rapport 0,58 ; correct jusqu'à 1 080 px de large) | WebP q75 | ≈ 150 – 200 Ko |
| Vignette (tranche, boîte) | 360 px | WebP q75 | ≈ 15 – 25 Ko |

| Lot | Contenu | Total estimé | Emplacement |
|---|---|---|---|
| **MVP** | 30 cartes | ≈ 5 – 6,5 Mo | `assets/` de l'APK |
| **V1** | + 30 à 70 cartes, plus les 14 stations du chemin de croix et les 20 mystères du rosaire (rapport 14) | ≈ 6 – 16 Mo | Paquet `images` signé (format de 14 et 11), installé à la demande |
| **AVIF** | Si `minSdk` ≥ 31 (décision de 10) | −20 à −30 % [S] | Paquets uniquement |

- **Aucun appel réseau à l'exécution** vers les musées : vie privée, et l'application est hors ligne.
- **Le pipeline (agent 11) se charge de tout** :
  - téléchargement, recadrage d'après un point focal choisi à la main, encodage ;
  - écriture des métadonnées et des sommes de contrôle ;
  - respect des limites de débit (AIC : 1 req/s ; Met : rafales modérées ; Commons : User-Agent descriptif sans donnée personnelle).

#### 3.2.4 La curation initiale proposée (61 entrées)

Notes de lecture :

- **[V]** signifie que l'objet existe, que l'institution le marque domaine public ou CC0 et qu'il a une image (vérification par API le 11/10/2026).
- **[S]** désigne un candidat à vérifier, dont l'identifiant est donné de mémoire ou non vérifié.
- Les thèmes suivent le brief. Les fêtes servent à proposer « l'image du jour » plus tard.

**Sacré-Cœur et Eucharistie (8)**

| # | Œuvre | Source, identifiant | Statut |
|---|---|---|---|
| 1 | Canivet au Sacré-Cœur, anonyme flamand, XVIIIᵉ | Met 775447 | [V] |
| 2 | *La Vierge du Sacré-Cœur*, Géricault, v. 1820 (lithographie) | Met 358722 | [V] |
| 3 | *Le Sacré-Cœur sur un linge tenu par un ange*, Nuremberg, v. 1480-1490 | Met 337750 | [V] |
| 4 | *Quatre saints adorant le Christ crucifié sur le Sacré-Cœur*, Cranach l'Ancien, 1505 | Met 625309 | [V] |
| 5 | *Sacré-Cœur de Jésus*, image pieuse Bouasse-Lebel, fin XIXᵉ (3031 × 5336) | Commons, marqué PD | [V] licence déclarée, provenance à documenter |
| 6 | *Sacré-Cœur*, Pompeo Batoni, 1767 (Il Gesù, Rome) | Commons, PD-Art | [S] PD-Art incertain en France |
| 7 | Images de communion (cartes françaises du XIXᵉ) | Commons, catégorie « Holy cards depicting the Eucharist » (26 fichiers) | [S] tri à faire |
| 8 | Ostensoir ou Agnus Dei gravé (XVIIᵉ – XVIIIᵉ) | AIC ou Met, gravures | [S] |

**Vierge Marie (17)**

| # | Œuvre | Source, identifiant | Statut |
|---|---|---|---|
| 9 | *Vierge à l'Enfant*, Hans Memling, v. 1485 | AIC 16295 | [V] |
| 10 | *Vierge à l'Enfant avec un ange*, Botticelli, 1475-1485 | AIC 80530 | [V] |
| 11 | *Vierge à l'Enfant avec deux anges*, Botticelli, 1485-1495 | AIC 80526 | [V] |
| 12 | *Vierge à l'Enfant couronnée par des anges*, Colyn de Coter, v. 1490 | AIC 16257 | [V] |
| 13 | Icône crétoise, *Vierge à l'Enfant*, v. 1500 | AIC 82511 | [V] |
| 14 | *Vierge à l'Enfant*, Jan Gossart, v. 1520 | AIC 111456 | [V] |
| 15 | *L'Assomption de la Vierge*, El Greco, 1577-1579 | AIC 87479 | [V] |
| 16 | *L'Immaculée Conception*, Guido Reni, 1627 | Met 437423 | [V] |
| 17 | *L'Immaculée Conception*, Andrea Schiavone, v. 1539-1543 (gravure) | Met 365100 | [V] |
| 18 | *Vierge de l'Immaculée Conception*, école de Séville, XVIIᵉ | Met 336465 | [V] |
| 19 | *L'Annonciation*, Sassetta, 1437-1444 | Met 458966 | [V] |
| 20 | *L'Annonciation*, Altdorfer, 1513 (gravure) | Met 429031 | [V] |
| 21 | *L'Annonciation*, Jan van Eyck, v. 1434-1436 | NGA | [S] |
| 22 | *La Madone d'Albe*, Raphaël | NGA | [S] |
| 23 | Notre-Dame du Perpétuel Secours (icône) | Commons | [S] |
| 24 | Notre-Dame de Lourdes ou Médaille miraculeuse, imagerie XIXᵉ | Commons, Paris Musées | [S] |
| 25 | *La Vierge en prière*, Sassoferrato | Commons, PD-Art | [S] PD-Art incertain en France |

**Passion et Sainte Face (14)**

| # | Œuvre | Source, identifiant | Statut |
|---|---|---|---|
| 26 | *La Crucifixion*, Zurbarán, 1627 | AIC 80084 | [V] |
| 27 | *La Crucifixion*, Carlo Crivelli, v. 1487 | AIC 111622 | [V] |
| 28 | *La Crucifixion*, Cranach l'Ancien, 1538 | AIC 59856 | [V] |
| 29 | *Homme de douleurs assis*, Dürer, 1515 | AIC 79896 | [V] |
| 30 | *Homme de douleurs*, gravure sur bois anonyme, 1465-1470 | AIC 105719 | [V] |
| 31 | *Le Suaire de sainte Véronique (Sainte Face)*, Claude Mellan, 1649, gravée d'un seul trait | AIC 47146 | [V] |
| 32 | *Sainte Véronique*, Martin Schongauer | AIC 3943 | [V] |
| 33 | *La Sainte Face*, Gerard David, v. 1485-1490 | Met 459253 | [V] |
| 34 | *Tête du Christ (Ecce Homo)*, Petrus Christus, v. 1445 | Met 435897 | [V] |
| 35 | *Ecce Homo*, Agostino Carracci, 1587 | Met 341241 | [V] |
| 36 | *Ecce Homo*, Wenceslaus Hollar, 1644-1652 | Met 360827 | [V] |
| 37 | *Pietà*, Hendrick Goltzius, 1596 | AIC 77854 | [V] |
| 38 | *Pietà*, d'après Cosimo Tura, 1475-1500 | AIC 16250 | [V] |
| 39 | Suite de la *Passion gravée* de Dürer, pour les stations du chemin de croix (rapport 14) | AIC (frontispices 68058, 19534 [V]) ; suite complète [S] | [V] partiel |

**Saints patrons (18)**

| # | Œuvre | Source, identifiant | Statut |
|---|---|---|---|
| 40 | *Saint Joseph et l'Enfant endormi*, Murillo (dessin) | AIC 148802 | [V] |
| 41 | *La Mort de saint Joseph* (patron de la bonne mort), Ricciolini, 1739 | Met 340557 | [V] |
| 42 | *Saint Michel*, Maître de Belmonte, 1450-1500 | Met 471730 | [V] |
| 43 | *Saint Michel*, Pietro di Giovanni d'Ambrogio, v. 1435 | Met 459257 | [V] |
| 44 | *Saint Michel*, Agostino Veneziano, v. 1514-1516 (gravure) | Met 342836 | [V] |
| 45 | *Saint François*, Rubens, v. 1615 | AIC 100342 | [V] |
| 46 | *Saint François agenouillé en méditation*, El Greco, v. 1595-1600 | AIC 21907 | [V] |
| 47 | *L'Extase de saint François*, Baglione, 1601 | AIC 160030 | [V] |
| 48 | *Saint Antoine de Padoue*, Maso di Banco, v. 1340 | Met 436982 | [V] |
| 49 | *La Vision de saint Antoine de Padoue*, Mengs | Met 437068 | [V] |
| 50 | *Jeanne d'Arc*, Jules Bastien-Lepage, 1879 | Met 435621 | [V] |
| 51 | *Saint Martin et le mendiant*, El Greco | NGA | [S] |
| 52 | *Saint Georges et le dragon*, Rogier van der Weyden | NGA | [S] |
| 53 | Saint Jean-Marie Vianney, portrait ou photographie (mort en 1859) | Commons | [S] |
| 54 | Sainte Bernadette, photographies des années 1860 | Commons | [S] |
| 55 | Saints Pierre et Paul, gravures | AIC ou Met | [S] |
| 56 | Saint Jean-Baptiste | AIC ou Met | [S] |
| 57 | Sainte Geneviève ou saint Louis, imagerie XIXᵉ | Paris Musées | [S] |

**Anges et images françaises du XIXᵉ (4)**

| # | Œuvre | Source, identifiant | Statut |
|---|---|---|---|
| 58 | Ange gardien, imagerie | Commons, « Holy cards depicting angels » (22 fichiers) | [S] |
| 59 | Canivets XVIIIᵉ – XIXᵉ (2 ou 3 retenus) | Commons, catégorie « Canivet » (24 PD sur 39, souvent en basse définition) | [S] |
| 60 | *Bidprentjes* et Sacré-Cœur néerlandais | Rijksmuseum (« Heilig Hart » : 16 résultats ; « bidprentje » : 9) | [S] tri à faire |
| 61 | Images de mission, de communion, souvenirs | Commons, « Holy cards from France » (5 fichiers, dont 2 en CC0) | [S] |

**Exclusions et précautions.**

- **[S, 12]** Photographies de sainte Thérèse de Lisieux. La plupart sont de sa sœur Céline (sœur Geneviève), morte en **1959**. Si ces photos sont des œuvres originales, elles restent protégées en France **jusqu'au 1ᵉʳ janvier 2030**. Il faut les exclure tant que l'agent 12 ne s'est pas prononcé.
- **Gallica, MuCEM et RMN-Grand Palais (Louvre)** sont exclus du MVP.
- **Les santini du XXᵉ siècle** à auteur identifiable, dont l'illustrateur est mort après 1955, sont exclus.

**Format des métadonnées d'une carte, pour 11 et 14 [P].** Ce n'est pas du code, seulement le schéma du contrat :

```json
{
  "id": "aic-16295",
  "titre": "Vierge à l'Enfant",
  "auteur": "Hans Memling", "date": "v. 1485",
  "institution": "Art Institute of Chicago",
  "url": "https://www.artic.edu/artworks/16295",
  "licence": "CC0-1.0", "credit": "Art Institute of Chicago",
  "themes": ["vierge"], "saints": [], "fetes": ["05:mary-*"],
  "gabarit": "carte", "recadrage": {"cx": 0.50, "cy": 0.42, "ratio": 0.58},
  "verso": {"priere": "prayer:souvenez-vous", "variante": "fr-trad"},
  "fichiers": {"plein": "images/aic-16295.webp", "vignette": "images/aic-16295_t.webp"},
  "sha256": {"plein": "…", "vignette": "…"}
}
```

### 3.3 La photo personnelle [P]

1. **Ajouter une image** : appareil photo ou galerie, avec un recto **et** un verso facultatif.
2. **Recadrer.**
   - **MVP** : 4 poignées d'angle et correction de perspective via `Matrix.setPolyToPoly`, puis rotation par pas de 90°.
   - **V1** : détection automatique des bords, en option via ML Kit Document Scanner **si** la variante Play est retenue (décision de 10) ; jamais obligatoire.
3. **Encoder** en WebP q75 à 1 440 px de côté long, plus une vignette de 360 px. Les **EXIF ne sont pas conservés**, la position GPS en particulier. On stocke dans `filesDir/cartes/{uuid}-recto.webp`, sans copie dans la galerie.
4. **Décrire** : titre (« Ma première communion »), date, personne, et lien facultatif vers une intention ou vers un défunt du mémento.
5. **Ranger** dans la boîte, ou glisser directement dans un livre.

- **[P] Budget sauvegarde.** Une carte personnelle recto et verso pèse environ 0,3 à 0,4 Mo [S, une photo est plus bruitée qu'un scan de musée]. Avec 20 Mo réservés aux photos, cela fait environ 50 à 60 cartes dans l'Auto Backup. Au-delà, un bandeau propose d'« Exporter vers un fichier », et l'application peut réduire l'échelle des cartes les plus anciennes si l'utilisateur l'accepte.
- **[S] Plus tard.** OCR local du verso (ML Kit Text Recognition), pour rendre la prière cherchable et lisible à voix haute.

### 3.4 Surlignages, notes et intentions [P]

- **Surlignages.**
  - **Cinq teintes** sobres sans sens liturgique : paille, sauge, bleu ciel, rose poudré, et un **trait rouge** souligné « au crayon de rubrique ».
  - Rendu de « lavis » en `Multiply` sur les thèmes clair et sépia. En thème sombre, on bascule vers un soulignement épais ou un fond translucide clair, car `Multiply` y disparaît.
  - Les « décorations » sont gérées avec 06.
- **Notes.**
  - Une note s'ancre à un point ou à une plage.
  - Elle est signalée par une **petite marque de crayon dans la marge extérieure**, sans bulle ; un *tap* ouvre une feuille basse.
  - Le texte est brut, sans mise en forme lourde.
  - Les notes entrent dans la recherche plein texte (06/08).
- **Intentions de prière.** C'est un carnet séparé, présenté comme un **mémento** à deux pages :
  - « Vivants » et « Défunts », comme les *Memento* du Canon ;
  - pour chaque intention : texte, personne, date de début, statut (en cours, exaucée avec action de grâces, archivée) ;
  - liens facultatifs vers une image pieuse et vers une neuvaine (rapport 14) ;
  - **pour un défunt : la date de décès**, qui permettra plus tard de rappeler son anniversaire ;
  - au *Memento* du Missel, un discret lien « Mes intentions » ouvre le carnet. C'est **un renvoi, pas une annotation du texte**.
- **Sobriété.** Aucun rappel par défaut. Les notifications locales sont activées en option, par neuvaine. Il n'y a pas de compteur de séries, ni de gamification.

### 3.5 Le modèle de données

#### 3.5.1 Les entités, à stocker dans Room ou dans l'équivalent choisi par 10 [P]

| Entité | Champs principaux |
|---|---|
| `Ancre` (embarquée) | `livreId`, `versionContenu`, `blockId`, `debut`, `fin?`, `citation{exact, avant, apres}`, `progression`, `titreSection`, `etat` (OK, APPROX ou ORPHELIN) |
| `Ruban` | `id`, `livreId`, `emplacement` (0 à 7), `couleur`, `coupe`, `role` (libellé), `mode` (MANUEL ou AUTO_MESSE_DU_JOUR, ou AUTO_PREFACE plus tard), `ancre?` (null = au repos), `figeJusquA?`, `majLe` |
| `Carte` | `id`, `origine` (CATALOGUE ou PERSONNELLE), `catalogueId?`, `recto?`, `verso?`, `versoPriereRef?`, `titre`, `date?`, `personne?`, `creeLe`, `majLe` |
| `PlacementCarte` | `carteId`, `ancre` (null = dans la boîte) ; **une carte se trouve à un seul endroit à la fois**, comme dans la réalité |
| `CoinCorne` | `id`, `ancre` (point), `creeLe` |
| `Surlignage` | `id`, `ancre` (plage), `teinte`, `noteId?`, `majLe` |
| `Note` | `id`, `ancre` (point ou plage), `texte`, `majLe` |
| `Intention` | `id`, `type` (VIVANT, DEFUNT ou AUTRE), `texte`, `personne?`, `debut`, `fin?`, `dateDeces?`, `statut`, `exauceeLe?`, `carteId?`, `neuvaineRef?`, `majLe` |
| Champs communs | Identifiant **UUID** stable (l'import peut ainsi fusionner), `majLe`, `supprimeLe?` (corbeille de 30 jours, pierre tombale pour la fusion) |

#### 3.5.2 L'ancre et le ré-ancrage [P, coordination avec 06 et 11]

- **On ne stocke jamais de numéro de page.** La pagination dépend de la police, de l'orientation et du thème. On stocke un **point dans le texte**, et la page affichée est celle qui contient ce point.
- **Le `blockId` est fourni par 11 et 06.**
  - Il est **sémantique** quand c'est possible : `bible:Jn.3.16`, `missel-1962:temporal/adv-3/introit`, `prayer:ave#p2`, `clairval:2019-03#p12`, `imitation:1.1#p3`.
  - Sinon, il est attribué une fois pour toutes par le pipeline et **conservé** d'une version à l'autre.
  - Le manifeste du paquet porte une **table de renommage**, déjà prévue par le rapport 14 : `ancien → nouveau`, avec fusion et scission.
- **Le décalage** se compte en points de code sur le texte **normalisé** du bloc : NFC, espaces fusionnées, sans traits d'union conditionnels, sans balisage de lettrine, sans numéros de verset injectés. L'agent 06 doit exposer cette normalisation unique.
- **La citation** reprend `exact`, plus 32 caractères avant et après (repris de Hypothesis et du `TextQuoteSelector`).
- **La cascade de ré-ancrage** s'exécute à l'installation d'une nouvelle version d'un paquet, en tâche de fond, et non à l'affichage :
  1. si le `blockId` existe et que l'empreinte du texte du bloc est inchangée → décalages conservés ;
  2. si le `blockId` existe mais que le texte a changé → recherche floue dans le bloc guidée par le contexte (Bitap ou diff-match-patch) → `APPROX` si la distance dépasse le seuil ;
  3. si le `blockId` a disparu → table de renommage, puis étape 2 ;
  4. sinon → recherche floue de la citation dans le livre, bornée par la `progression` ± 5 % ;
  5. en dernier recours → `ORPHELIN`, rattaché à la `progression`. Une annotation orpheline est **visible** dans « Mes signets » (« à replacer »), **jamais supprimée**.
- **Le ruban automatique** n'a pas d'ancre persistée. Il est recalculé chaque jour, ce qui lui épargne le ré-ancrage.

#### 3.5.3 Export et import [P]

- **Fichier `Paroissien-AAAA-MM-JJ.paroissien`** : un ZIP passé par le sélecteur de fichiers du système (`CreateDocument` / `OpenDocument`), sans permission. Il contient :
  - `manifest.json` : `formatVersion`, version de l'application, date, comptes et sommes SHA-256 ;
  - `annotations.json` : rubans, coins cornés, surlignages, notes et placements de cartes. Les ancres y sont aussi exprimées en sélecteurs **de style W3C** (`TextQuoteSelector` + `TextPositionSelector` relatif au bloc), pour rester lisibles et réutilisables ;
  - `cartes/` : métadonnées et WebP des **cartes personnelles**. Les cartes du catalogue n'y figurent que par leur `catalogueId` ;
  - `intentions.json` : **seulement si l'utilisateur coche « inclure mes intentions »**, décoché par défaut.
- **[V1] Chiffrement facultatif** de l'export par mot de passe : AES-GCM, clé dérivée par PBKDF2-HMAC-SHA256 avec un grand nombre d'itérations. C'est disponible en standard dans le JCA d'Android [S], à valider par 10.
- **Import** au choix :
  - « Fusionner » : par UUID, la dernière `majLe` l'emporte, et les pierres tombales sont respectées ;
  - « Remplacer tout » : précédé d'une sauvegarde automatique locale.
- **Garde-fous à l'import** : validation du schéma, protection contre le *zip-slip*, limites de taille, décodage sûr des images.

#### 3.5.4 Auto Backup [P, faits en §2.9]

- `allowBackup="true"`, et `dataExtractionRules` sur Android 12 et plus :
  - `<cloud-backup disableIfNoEncryptionCapabilities="true">` : inclure la base des annotations, les préférences et `filesDir/cartes/` ; exclure les paquets de contenu (réinstallables), les index plein texte, les caches d'images et les vignettes régénérables ;
  - `<device-transfer>` : tout inclure, sauf les caches.
- Un fichier `fullBackupContent` équivalent couvre Android 11 et antérieurs.
- **Intentions** : base **séparée** (`intentions.db`). En V1, l'interrupteur « Exclure mes intentions de la sauvegarde Android » passe par un `BackupAgent` dont `onFullBackup()` saute ce fichier selon la préférence. Les règles XML ne peuvent pas dépendre d'un réglage de l'utilisateur.
- **Quota** : `onQuotaExceeded()` lève un indicateur, et l'application affiche ensuite « Votre sauvegarde Android est pleine (photos) : exportez un fichier ».
- **Texte d'aide honnête** : « Paroissien n'a pas de serveur. Si la sauvegarde Android est activée sur votre téléphone, vos signets y sont copiés, chiffrés avec le code de votre écran de verrouillage. »

### 3.6 L'écran « Mes signets » (maquettes ASCII) [P]

**A. La lecture en portrait : un ruban dans le pli, des queues sous la page, une carte qui dépasse.**

```
                ┌───────┐
                │ ✠ ˙˙˙ │  ← image pieuse glissée avant cette page (tap = l'ouvrir)
 ┌──────────────┴───────┴─────────────────────┐
 │▐█  IIIᵉ DIMANCHE DE L'AVENT                │
 │▐█                                          │
 │▐█  INTROÏT · Ph 4, 4-6                     │
 │▐█  ╔═╗                                     │
 │▐█  ║G║audéte in Dómino semper :            │
 │▐█  ╚═╝ íterum dico, gaudéte.               │
 │▐█  Réjouissez-vous sans cesse dans le      │
 │▐█  Seigneur ; je le répète, réjouissez-    │
 │▐█  vous.                                   │
 │▐█   ↑                                      │
 │▐█   ruban rose « messe du jour », couché   │
 │▐█   dans le pli, côté reliure              │
 └▐█▄─────────────────────────────────────────┘  ← le ruban se replie sur la tranche
  ▐█  │▌ │▌         ┆          │▌ │▌ │▌
  ╲╱  ╲╱ ╲╱         ┆          ╲╱ ╲╱ ╲╱          ← queues coupées en V
  rose● vert rouge  ┆      violet blanc noir
  (● = ferret doré du ruban auto ; avant | après cette page)
 ─────────── zone de gestes système (intouchable) ───────────
```

**B. « Mes signets », onglet Rubans : la tranche de chaque livre et ses rubans pendants.**

```
┌──────────────────────────────────────────────┐
│ ‹  Mes signets                         ⌕   ⋮ │
│  [Rubans]   Images    Carnet    Intentions   │
├──────────────────────────────────────────────┤
│                                              │
│  ┌────────────────────────────────────────┐  │
│  │      MISSEL ROMAIN · 1962    ✠         │  │ ← tête du livre vue de dos
│  └──┬────┬────┬────┬────┬────┬────┬───────┘  │
│     █    █    █    █    █    █    █          │
│     █    █    █    █    █    █    █          │
│    ╲╱   ╲╱   ╲╱    █   ╲╱   ╲╱   ╲╱          │
│    vert rouge viol.╲╱  rose noir  ●jour      │
│                  blanc                       │
│  ╭───────────────────────────────────────╮   │
│  │ ▌ Ruban du jour · rose                │   │ ← étiquette du ruban touché
│  │   IIIᵉ dim. de l'Avent — Introït      │   │
│  │   « Gaudéte in Dómino semper… »       │   │
│  │                     [ Ouvrir › ]      │   │
│  ╰───────────────────────────────────────╯   │
│                                              │
│  ┌──────────────┐  ┌──────────────┐          │
│  │ BIBLE        │  │ IMITATION    │   ›      │ ← autres livres (carrousel)
│  └─┬──┬──┬──┬───┘  └─┬──┬─────────┘          │
│   ╲╱ ╲╱ ╲╱ ╲╱       ╲╱ ╲╱                    │
│                                              │
│  ⚠ 1 ruban à replacer (texte mis à jour)     │ ← orphelins, jamais supprimés
└──────────────────────────────────────────────┘
```

**C. Onglet Images : la boîte à images pieuses.**

```
┌──────────────────────────────────────────────┐
│ ‹  Mes signets                         ⌕   ⋮ │
│   Rubans   [Images]   Carnet    Intentions   │
├──────────────────────────────────────────────┤
│  DANS MES LIVRES                             │
│   ┌────┐ ┌────┐  ┌────┐                      │
│   │ ♥  │ │ ✠  │  │ ☧  │   ← cartes debout,   │
│   │    │ │    │  │    │     légèrement       │
│   └────┘ └────┘  └────┘     inclinées        │
│   Missel  Missel  Imita-                     │
│   Avent   Canon   tion I,3                   │
│                                              │
│  DANS LA BOÎTE                               │
│       ┌────┐                                 │
│    ┌──┤    ├──┐   ┌────┐  ┌────┐             │
│    │  │ ✿  │  │   │ ⚘  │  │ ✚  │  + Ajouter  │
│    │  └────┘  │   │    │  │    │   ma photo  │
│    └──────────┘   └────┘  └────┘             │
│   (éventail : touchez pour sortir une carte)│
│                                              │
│  CATALOGUE  Sacré-Cœur · Vierge · Passion ·  │
│             Saints · Anges                 › │
└──────────────────────────────────────────────┘
```

**D. Une carte ouverte, recto puis verso.**

```
   ┌───────────────────┐         ┌───────────────────┐
   │ ┌───────────────┐ │         │   SOUVENEZ-VOUS   │
   │ │               │ │  tap    │                   │
   │ │   (image du   │ │ ─────►  │ Souvenez-vous, ô  │
   │ │  Sacré-Cœur)  │ │ retour- │ très miséricor-   │
   │ │               │ │  ner    │ dieuse Vierge     │
   │ └───────────────┘ │         │ Marie…            │
   │  CŒUR SACRÉ DE    │         │                   │
   │      JÉSUS        │         │ ── Met 775447 ·   │
   └───────────────────┘         │    CC0            │
   [Remettre] [Prier] [⋯]        └───────────────────┘
```

**E. Les onglets Carnet et Intentions.**

- **Carnet** : des fiches bristol empilées, avec une bordure de la teinte du surlignage, l'extrait, le livre et la date. Le filtrage se fait par livre, teinte ou type (coin corné, surlignage, note). La liste est paginée et indexée, pour éviter la lenteur constatée chez Kindle.
- **Intentions** : une double page de garde « *Memento, Domine…* », avec « Vivants » à gauche et « Défunts » à droite. Les noms y apparaissent comme écrits à la plume, avec la date ; *tap* = fiche. On peut verrouiller le carnet (V1, voir §4).

### 3.7 Priorisation

| | MVP | V1 | Plus tard |
|---|---|---|---|
| **Rubans** | 6 rubans par Missel et 2 à 4 par livre ; texture en tuile, coupe en V ; *tap*, glisser ici, retirer ; doigt glissé ; menu « Rubans » accessible ; signet rapide (coin corné) | Ruban auto « messe du jour » (dès que 05 et 03 sont prêts) ; coupes au choix ; couleurs or et bleu ; préréglages de rôles | Ruban auto « préface du jour » ; texture AGSL (API 33 et plus) ; son de papier |
| **Images pieuses** | 30 cartes CC0 embarquées ; glisser, ouvrir, retourner ; boîte ; verso en vrai texte (prières de 14) ; crédits | Paquet de 60 à 100 cartes, plus les stations et les mystères ; photo personnelle recto et verso avec recadrage en 4 coins | Détection auto des bords (ML Kit, si Play) ; OCR du verso ; « image du jour » selon le saint ; AVIF |
| **Annotations** | Surlignages en 5 teintes, notes, carnet | Intentions et mémento (vivants et défunts) ; lien depuis le *Memento* du Missel ; exclusion des intentions de la sauvegarde | Anniversaires de décès ; rappels de neuvaine (opt-in) |
| **Données** | Ancres de bloc, citation et progression ; cascade de ré-ancrage ; Room ; Auto Backup chiffré avec règles d'extraction | Export et import `.paroissien` (fusion ou remplacement) ; chiffrement facultatif de l'export ; corbeille de 30 jours | Format d'échange W3C JSON-LD complet |
| **Écran « Mes signets »** | Onglets Rubans et Images en vue « objets » | Carnet et Intentions ; orphelins « à replacer » | Verrouillage biométrique du carnet |

> Si 05 n'est pas prêt pour le MVP, le ruban auto glisse en V1. Je recommande toutefois de l'en faire sortir le plus tôt possible : c'est la démonstration la plus parlante du concept.

### 3.8 Points d'interface avec les autres agents

| Agent | Ce dont j'ai besoin, ou ce que je fournis |
|---|---|
| **03 Missel** | `celebrationId → blockId` du début du propre ; ancre de l'Ordinaire, des préfaces, du *Memento* des vivants et des défunts ; éventuellement « préface du jour » |
| **05 Calendrier** | `(date, rite) → couleur principale + couleurs admises (rose/violet, blanc/or, violet/noir)` ; heure de bascule vers le dimanche (option du samedi) ; identifiants de saints, pour « l'image du jour » |
| **06 Moteur de lecture** | Normalisation unique du texte d'un bloc (pour les décalages) ; résolution `ancre → page` après pagination ; API de décorations (surlignages, marques de marge, ruban dans la gouttière) ; marge intérieure réservée au ruban ; historique de navigation (« doigt glissé ») |
| **07 Tourne-page** | Le ruban et les cartes suivent la feuille qu'on tourne ; bande de la tranche de queue au-dessus des encarts ; feuilletage rapide après un *tap* sur un ruban ; pas de conflit avec les zones de tap de la tête et de la queue |
| **08 UX** | Intégration de l'écran « Mes signets » dans la navigation ; découverte du geste « tirer » (le rapport 13 signale ce risque) ; tests de compréhension |
| **10 Architecture** | Room et migrations ; `minSdk` (AVIF, photo picker) ; variante Play ou F-Droid (ML Kit) ; `BackupAgent` ; FileProvider pour l'appareil photo |
| **11 Pipeline** | `blockId` stables et table de renommage ; paquet `images` (recadrage, WebP, vignettes, sommes, crédits) ; respect des limites de débit des API |
| **12 Juridique** | PD-Art en France ; Gallica ; photos de sainte Thérèse ; santini du XXᵉ siècle ; mentions de crédit ; statut des intentions (données religieuses, RGPD art. 9) si un jour elles quittent l'appareil |
| **14 Prières** | Versos des cartes (`prayer:…`, en variantes du domaine public) ; listes des images pour les 14 stations et les 20 mystères ; liens entre intentions et neuvaines |
| **13 Benchmark** | Le signet rapide (coin corné) et les rubans sont séparés, comme le rapport 13 le recommande |

---

## 4. Décisions à soumettre à l'utilisateur

1. **Rubans propres à chaque livre** (recommandé) **ou communs à tous ?**
2. **Le ruban « messe du jour » : un 7ᵉ ruban à ferret doré, ou l'un des 6 rubans désigné « liturgique » ?** Je recommande le 7ᵉ : le ruban de l'usager n'est jamais déplacé par surprise.
3. **Le ruban auto bascule-t-il vers le dimanche dès le samedi après-midi ?** C'est utile pour la messe anticipée ou pour la préparation ; je propose de le permettre en option, désactivée par défaut.
4. **Le bleu marial et l'or** : les proposer comme couleurs de ruban de dévotion, en dehors du jeu liturgique ?
5. **Le préréglage de rôles du Missel** : celui du §3.1.1, celui de Lasance (or = Ordinaire), ou aucun ?
6. **Par où les images pieuses dépassent-elles** : par la tête de la page (recommandé, pas de conflit avec le tourne-page) ou par la gouttière latérale (plus « livre », mais en concurrence avec les gestes de 07) ?
7. **La taille du catalogue** : 30 cartes dans l'APK, soit environ 5 Mo, avec le reste en paquet à la demande. Faut-il plutôt tout embarquer (environ 16 Mo) ?
8. **Commons et le PD-Art.** Faut-il s'en tenir aux institutions CC0 (prudent), ou accepter les reproductions PD-Art de Commons après l'avis de 12 ? Avec Commons, le catalogue gagne Batoni, Sassoferrato, l'imagerie de Lourdes, etc.
9. **ML Kit Document Scanner** (de meilleure qualité, mais avec Google Play services) **ou un recadrage manuel seul** (libre, compatible F-Droid) ?
10. **Les intentions dans l'Auto Backup.** Les inclure par défaut (chiffrées de bout en bout) avec un interrupteur pour les exclure, ou les exclure par défaut ?
11. **Les photos personnelles au-delà du quota de 25 Mo** : prévenir et proposer l'export (recommandé), ou réduire automatiquement la résolution ?
12. **Le verrouillage du carnet d'intentions** (biométrie ou code de l'appareil) : est-ce souhaité dès la V1 ?

---

## 5. Risques et inconnues

| Risque ou inconnue | Gravité | Atténuation |
|---|---|---|
| **Geste « tirer le ruban » mal compris** (rapport 13) | Élevée | *Tap* = ouvrir, qui est le geste principal ; menu « Rubans » sans geste ; tests d'utilisateurs sur papier ou maquette dès la V0 |
| **Conflit avec les gestes système et le tourne-page** | Élevée | Bande de queue au-dessus des encarts ; cartes en tête ; validation avec 07 sur un téléphone physique |
| **Encombrement visuel** sur un téléphone de 360 dp (7 queues, cartes, ruban dans la gouttière) | Moyenne | Queues de 14 dp ; épaisseur suggérée au-delà de 4 cartes ; option « rubans discrets » |
| **Dérive des ancres** lors des mises à jour (Clairval, corrections du Missel) | Élevée | `blockId` sémantiques, table de renommage, cascade floue, orphelins visibles ; **tests de non-régression** sur deux versions d'un même texte (pipeline 11) |
| **Statut juridique des images** : PD-Art en France, santini du XXᵉ siècle, Thérèse, Gallica | Moyenne | Catalogue MVP 100 % CC0 institutionnel ; registre de provenance par carte ; avis de 12 |
| **Licence des images de l'AIC** : seule la CC0 des **données** a été vue, la page *image-licensing* a renvoyé un 403 | Faible | Confirmer avant publication (12) ; à défaut, se replier sur Met, NGA ou Rijksmuseum pour les mêmes thèmes |
| **API instables** : `/search` v1 du Met retirée le 1/10/2026, ancienne API du Rijksmuseum dépréciée, résolveur Rijksmuseum modifié en juin 2026 | Faible | Récupération une fois, au moment de la construction, par le pipeline ; pas d'appel à l'exécution |
| **Quota de 25 Mo** dépassé par les photos | Moyenne | Budget par carte, alerte et export ; base et images séparées |
| **Données sensibles** (intentions, notes de confession, noms de défunts) dans un export partagé par erreur | Moyenne | Intentions exclues de l'export par défaut ; chiffrement facultatif ; texte d'avertissement |
| **Lenteur avec beaucoup d'annotations** (cas Kindle) | Moyenne | Index par `livreId` et `blockId` ; carnet paginé ; ré-ancrage en tâche de fond, à l'installation d'un paquet |
| **Rendu des teintes** de ruban et de surlignage en thème sombre et pour les daltoniens | Faible | Palette par thème ; libellés ; tests de simulation |
| **AVIF** : poids réel non mesuré ici | Faible | Rester en WebP tant que `minSdk` < 31 |
| **Coût de la curation à la main** : point focal, choix du verso, crédits pour 60 à 100 cartes | Moyenne | Commencer avec 30 cartes ; gabarit JSON ; vérification par lot |
| **Ma recherche est incomplète** : quota de recherche web épuisé, Gallica et la page images de l'AIC inaccessibles, NGA et Rijksmuseum non vérifiés pièce par pièce | — | Points marqués [S] ; à reprendre dans une passe de vérification |

---

## 6. Sources

**Rubans et missels**
- Baronius Press, *Daily Missal 1962* : https://www.baronius.com/daily-missal-1962.html
- Latin Mass Society, Baronius Daily Missal : https://lms.org.uk/taxonomy/term/531
- Clear Creek Monks, 1962 hand missal : https://clearcreekmonks.org/gift-shop/1962-hand-missal/
- Cenacle Press, Roman Missal 1962 (Angelus Press, 5 rubans) : https://cenaclepress.com/products/roman-missal-1962
- Fr. Z, « QUAERITUR: Bought a Baronius Missal… how to set up the ribbons » : https://wdtprs.com/?p=4801
- Évêché d'Edmundston, note sur le nouveau missel (« Les rubans, au nombre de 5 ») : https://www.diocese-edmundston.ca/fr/docs1/missel-livre.pdf
- Rapport 13 (benchmark) : `docs/planning/13-benchmark.md` ; rapport 14 (prières) : `docs/planning/14-prieres-devotions-extensions.md`

**Couleurs liturgiques**
- PGMR, version anglaise (adaptation des États-Unis), n. 345-346 : https://www.vatican.va/roman_curia/congregations/ccdds/documents/rc_con_ccdds_doc_20030317_ordinamento-messale_en.html
- Wikipédia, *Liturgical colours* : https://en.wikipedia.org/wiki/Liturgical_colours
- EWTN, *Blue Liturgical Vestments* : https://www.ewtn.com/catholicism/library/blue-liturgical-vestments-4526
- Fr. Z, *Blue vestments* : https://wdtprs.com/2015/11/blue-vestments-wherein-fr-z-rants/

**Images pieuses : formats**
- MuCEM, notice « image pieuse, Cœur Sacré de Jésus », 1924, 11 × 6 cm : https://mucem.org/notice/c/4113620
- MuCEM, autres notices : https://mucem.org/notice/c/4113943 · https://mucem.org/notice/c/4113938 · https://mucem.org/notice/c/4094867
- MuCEM, mentions légales : https://www.mucem.org/mentions-legales
- Boston College, Burns Library, *Pray for Us* : https://library.bc.edu/burns-exhibits/pray-for-us/

**Sources d'images, licences et API**
- Met, API : https://metmuseum.github.io/ ; objets cités : https://www.metmuseum.org/art/collection/search/775447 (et identifiants du §3.2.4)
- Met, *Open Access* : https://www.metmuseum.org/hubs/open-access (HTTP 429 lors de la consultation ; licence CC0 vérifiée via la documentation de l'API)
- Art Institute of Chicago, API : https://api.artic.edu/docs/ ; réponses `api.artic.edu/api/v1/artworks/{id}` (champ `info.license_text`) ; page *image licensing* : https://www.artic.edu/image-licensing (403)
- National Gallery of Art, open data : https://github.com/NationalGalleryOfArt/opendata ; politique : https://nga.gov/imagepolicy ; Wikimedia : https://nga.gov/open-access-images/wikimedia-commons-wikidata.html
- Rijksmuseum, data services : https://data.rijksmuseum.nl/docs/ · Search API : https://data.rijksmuseum.nl/docs/search · politique de données : https://www.rijksmuseum.nl/en/data/policy
- Paris Musées, *Open Content* : https://parismusees.paris.fr/en/actualite/open-content-150000-works-from-the-museum-collections-of-the-city-of-paris-freely · Creative Commons : https://creativecommons.org/2020/01/10/paris-musees-releases-100000-works-into-the-public-domain/ · portail : https://www.parismuseescollections.paris.fr/
- Wikimedia Commons, PD-Art : https://commons.wikimedia.org/wiki/Commons:Reuse_of_PD-Art_photographs · API imageinfo : https://www.mediawiki.org/wiki/API:Imageinfo · catégories : https://commons.wikimedia.org/wiki/Category:Holy_cards · https://commons.wikimedia.org/wiki/Category:Canivet
- Gallica, conditions d'utilisation : https://gallica.bnf.fr/edit/und/conditions-dutilisation-des-contenus-de-gallica (403 Cloudflare) ; sources secondaires : https://linuxfr.org/users/pamputt/journaux/conditions-d-utilisation-des-contenus-de-gallica · https://actualitte.com/article/57529/archives/la-bnf-en-flagrant-delit-de-copyfraud-une-deputee-alerte-aurelie-filippetti · https://questions.assemblee-nationale.fr/dyn/14/questions/QANR5L14QE23878.pdf

**Android**
- Auto Backup : https://developer.android.com/identity/data/autobackup
- Sélecteur de photos : https://developer.android.com/training/data-storage/shared/photo-picker
- `MediaStore.ACTION_IMAGE_CAPTURE` (note sur `CAMERA`) : https://developer.android.com/reference/android/provider/MediaStore
- Android 12, prise en charge de l'AVIF : https://developer.android.com/about/versions/12/features
- AGSL (Android 13 et plus) : https://developer.android.com/develop/ui/views/graphics/agsl
- `View.setSystemGestureExclusionRects` (limite de 200 dp) : https://developer.android.com/reference/android/view/View
- Navigation par gestes : https://developer.android.com/develop/ui/views/touch-and-input/gestures/gesturenav
- ML Kit Document Scanner : https://developers.google.com/ml-kit/vision/doc-scanner

**Ancrage des annotations**
- W3C Web Annotation Data Model : https://www.w3.org/TR/annotation-model/
- Readium, *Locators* : https://readium.org/architecture/models/locators/
- Hypothesis, *Fuzzy Anchoring* : https://web.hypothes.is/blog/fuzzy-anchoring/

**Mesures**
- Échantillons (AIC 16295, AIC 79896, Met 775447) et script de mesure dans le dossier de travail de l'agent 09 (hors dépôt). Le résultat est reproduit en §2.6.
