# Atelier : cadres, couvertures, gardes et rubans

Proposition du 12/10/2026, à la suite des scans du paroissien chromolithographié (premier lot, 14 photos) et d'un second lot de 16 photos : couvertures, gardes, bandeaux latéraux, compositions partielles, cadre simple. Deux buts : enrichir la bibliothèque d'ornements, et permettre à l'utilisateur de composer lui-même l'habit de chaque livre.

**Décisions du 12/10/2026** (réponses de l'utilisateur) :

- les vignettes gravées entrent, comme images tirées de ses scans (§6) ;
- un habit par livre, pas de thème global ;
- distinguer les livres nobles, qui méritent un habillage travaillé, des livres simples (exercices, lettres de Clairval) (§4) ;
- prévoir l'import et l'export de livres (PDF, txt, epub) et d'images pieuses recto verso (§7), et une sauvegarde complète de l'application (§8) ;
- moteur v3 lancé (§2).

## 1. Ce que les scans apportent

| Famille | Scans | Modèle |
|---|---|---|
| Cadres pleins | matin, soir, avant et pendant la messe, confession, communion, vêpres, complies, mariage, renaissance, vigne, quadrilobes, frontispice de l'évangéliste | Cadre à quatre bandes (fait) |
| Bandeaux latéraux | vigne et calice, palmes, passiflores et lyre (2), figuier « Jacob Juda David », lis « Vox Dei » | Une bande du côté extérieur, médaillon au milieu |
| Compositions partielles | candélabres en tête et rinceaux au côté (table des matières, ordinaire), candélabre au côté et rinceaux au pied (litanies) | Deux bandes en équerre (Γ ou L) |
| Cadre simple | filet rouge à anneaux d'angle (p.81) | Filet et coins seuls (fait : `filets_rouges`) |
| Gardes | asanoha or et bleu, symboles chrétiens rouges ; soie moirée (faite) | Motif répété sur toute la page |
| Couvertures | toile noire gaufrée à rameau et monogramme ; veau brun nu ; veau à filets et carrés dorés ; toile noire à croix dorée, lis et bande gaufrée au dos | Matière + décor en modules |
| Tranches | rouge orangé | Une finition de plus |
| Vignettes | scènes gravées en tête ou au pied de page | Images tirées des scans (§6) |

## 2. Moteur, troisième version (fait le 12/10/2026)

Les relevés du premier lot ont buté sur les mêmes limites ; le second lot les rendait indispensables. Le Python (`commun.py`), l'aperçu JavaScript et le Kotlin suivent le même algorithme ; les 14 premiers cadres sortent à l'identique.

- **Disposition.** Chaque bande peut manquer : haut, côté extérieur, pied, côté du pli. Le côté extérieur suit le folio, comme dans un livre : à droite sur un recto, à gauche sur un verso (et à droite sur une page seule, dont le pli est à gauche). Dispositions nommées : pleine, latérale, tête, pied, tête et pied, équerre de tête (Γ), équerre de pied (L), ouverte. Une bande sans voisine court jusqu'au bord du cadre ; les coins n'existent que là où deux bandes se rencontrent ; les filets suivent les bandes présentes ; le texte reprend la place d'une bande absente.
- **Pièces de bout.** Un ornement posé une fois à chaque bout d'une bande (socle et couronnement d'un candélabre, départ d'une vigne), en plus des coins.
- **Filets à distance fixe** du bord extérieur ou du texte, en unités, et non plus en fraction de la bande.
- **Débords.** Pièces centrales et pièces de bout peuvent déborder vers le texte ; pièces centrales et coins peuvent se dessiner par-dessus les filets et les baguettes. Une pièce « droite » tourne avec la bande sans jamais passer en miroir : une inscription reste lisible sur un verso.
- **Gardes à motif** : une tuile et un réseau (carré ou quinconce, qui donne aussi losanges et nids d'abeilles), peints une fois dans une petite image puis répétés par un shader (`PatternPaper`). Les gardes vivent dans `tools/cadres/gardes/` et le registre `EndpaperPatterns`.
- **Ajouts demandés par le relevé du second lot (v3.1)** : pièce centrale placée à une fraction de la bande (au tiers, comme sur les gravures) ; frise qui continue dans le même sens après la pièce (une tige qui pousse d'un seul élan) ; frise qui mord sur ses voisines ; pièce « droite » identique sur un recto et un verso, déplacée et jamais retournée (une figure reste debout, une inscription lisible) ; encres du thème `text` et `rubric`, pour qu'une gravure tracée à l'encre du texte reste lisible en sombre.

Souhaits notés pendant le relevé, pour plus tard :

- plusieurs pièces sur une même bande, ou deux frises qui alternent (A B A B), pour un candélabre qui varie de bas en haut ;
- un mode « espacé » qui garde le pas de la frise et comble le reste de tige nue, au lieu d'étirer les motifs (jusqu'à ±20 % sur une colonne courte) ;
- une frise qui passe devant la pièce centrale (feuille qui croise un cartouche), et une frise différente avant et après la pièce ;
- des bouts « libre » et « contre le coin » distincts sur une équerre, au lieu de début et fin ;
- une encre qui change selon le thème (sépia en clair, encre du texte en sombre) et une demi-teinte (opacité) pour le modelé des gravures ;
- un raccord sans liseré pour les tiges évidées qui passent d'un module à l'autre ;
- pour les gardes : dessin par passes, origine centrée sur la page, fond plus sombre en thème sombre ;
- un bandeau latéral qui part sous la vignette de tête au lieu de longer toute la page.

Reste pour plus tard : **couvertures en modules** (§5).

## 3. Second lot (fait le 12/10/2026)

Relevé en parallèle d'après les photos de `cadres-photos/lot2/`, un paroissien gravé en sépia : cinq bandeaux latéraux (vigne et calice, rameaux d'olivier à la colombe, passiflores et Agneau, figuier « Jacob, Juda, David », lis « Vox Dei »), deux équerres de pied (candélabres de l'ordinaire et de la table des matières, litanies), le filet rouge, et deux gardes (asanoha, symboles chrétiens). Les « palmes » sont un rameau d'olivier, la « lyre » une croix à l'Agneau, et la table et l'ordinaire partagent le même bois. Catalogue : `tools/cadres/METHODE.md` §7.

La règle du sommaire les emploie : par le sens (litanies, table des matières et ordinaire, carême et passion, Saint-Esprit et Pentecôte, Saint-Sacrement, Avent et Noël, Évangile), puis dans la boucle des cadres courants pour les bandeaux.

## 4. Rangs d'habillage

Le rang se déduit de la nature du livre (`BookDresses`), pas du réglage « Livres » qui rend les reliures uniformes :

| Rang | Livres | Pages | Garde |
|---|---|---|---|
| Noble | livres de prières reliés de cuir (importance ≥ 2 : prières, Imitation) | un cadre ornemental par section, choisi par le sens puis par le sommaire | soie moirée dans sa roulette dorée |
| Bible | les Bibles | réglées d'un filet rouge (`filets_rouges`), comme les Bibles de luxe | soie moirée |
| Simple | livres reliés de toile (exercices) | nues | papier imprimé d'un motif (asanoha, symboles) |
| Sans habit | lettres et brochures sous couverture de papier | nues | aucune |

Pistes pour les livres nobles, à trancher à l'usage :

- **Page d'ouverture et page courante.** Dans le paroissien, les pages courantes portent des compositions plus légères (bandeau latéral, équerre) que les pages d'ouverture. Le lecteur pourrait poser le cadre plein sur la première page d'une section et un bandeau de la même famille sur les suivantes.
- **Vignettes** en tête des sections (§6).

L'atelier (§9) laisse changer le rang d'un livre, par exemple habiller noblement un livre importé.

## 5. Couvertures en modules

Une matière (toile grenée, veau patiné, maroquin, papier), un décor de bord (une frise, comme un cadre), un motif central, des ornements de tête et de pied, une bande au dos, des coins arrondis ; trois fers : doré (or en dégradé), à froid (gaufrage : ombre et lumière décalées), imprimé. Les quatre couvertures du second lot en sont les premiers modèles.

## 6. Vignettes gravées

Les scènes gravées (Évangile, saints) sont des images tramées que le moteur vectoriel ne sait pas redessiner : elles entrent comme images, tirées des scans de l'utilisateur.

- **Préparation** (outil `tools/cadres/vignettes.py`, fait) : redressement, recadrage, correction du blanc, passage en niveaux de gris, papier rendu transparent (l'encre seule, posée sur le papier du thème en mode « multiplier », comme une impression), WebP, environ 1 000 px de large.
- **Où elles vivent.** Ce sont des photos personnelles : jamais dans le dépôt public. Elles vont dans le dépôt privé `paroissien-content` et entrent dans l'APK de la saveur `personnel` avec les paquets privés ; la saveur publique n'en a pas.
- **Placement** (fait). En tête de la première page d'une section des livres nobles, sur la largeur du texte (au plus 40 % de la page), gardée avec le titre qui la suit ; une vignette par section, associée par le sens comme les cadres, sinon en boucle. L'app la teint de l'encre du thème : sépia sur papier clair, claire en sombre. Réglage « Vignettes ».
- **Premières vignettes** : le Baptême du Christ et Jésus donnant la communion (pp.50-51 du paroissien gravé), tirées d'une photo prise en biais. Pour les suivantes, des photos à plat, bien éclairées, une gravure par photo, donneront un meilleur grain.
- **Légende et source** gardées avec l'image : page du paroissien d'origine, sujet.

## 7. Import et export

### Livres

- **Import.** Un livre importé devient un paquet comme les autres : le même schéma SQLite que les paquets embarqués, écrit sur l'appareil dans `files/livres/`. Le lecteur, le sommaire, la recherche plein texte, les rubans et l'habit marchent donc sans changement.
  - **txt** : paragraphes séparés par des lignes vides ; titres repérés (lignes courtes en capitales, « Chapitre… ») pour le sommaire ; encodage détecté (UTF-8, sinon Windows-1252).
  - **epub** : le manifeste et l'ordre de lecture (`spine`) donnent les unités ; le XHTML donne les blocs (titres, paragraphes, italiques, gras) ; titre, auteur et langue viennent des métadonnées. Lecture avec le parseur XML d'Android, sans bibliothèque de plus.
  - **PDF** : deux cas. Un PDF né numérique : extraction du texte (bibliothèque PdfBox-Android, Apache-2.0) puis même traitement que le txt. Un PDF scanné, sans texte : livre en fac-similé, une image par page (`PdfRenderer` d'Android) dans le même livre à pages ; pas de recherche, mais rubans, signets et habit.
  - Le livre importé reçoit le rang simple ; l'atelier peut l'anoblir.
  - **Fait le 12/10/2026 (0.6.4) pour le txt et l'epub** : tuile « Importer un livre » sur le rayon ; `BookImport` lit le fichier (texte : UTF-8 sinon Windows-1252, en-tête Gutenberg écarté, chapitres repérés ; epub : `container.xml`, OPF, `spine`, XHTML avec titres, paragraphes, italiques, gras, exposants, entités HTML courantes ; images ignorées), `PackWriter` (core:content, même schéma `pack-v1.sql` que la chaîne Python) l'écrit dans `files/mes-livres/perso-<titre>-<6>.sqlite`. Statut `import-personnel` ; « Retirer ce livre » dans « Habiller ce livre » ; la sauvegarde complète l'emporte (`mes-livres/`). Reste le PDF.
- **Export.** Un livre vers **epub** (titres, paragraphes, rubriques en rouge) ou **txt** ; vers **PDF**, en imprimant les pages telles que l'app les met en page, cadres et vignettes compris (`PdfDocument` d'Android) : de quoi imprimer son propre paroissien.
- **Contenus protégés.** Ils font partie de la sauvegarde complète (§8), décision du 12/10/2026 ; l'export d'un livre seul (epub, txt, PDF) suivra la même règle.

### Images pieuses recto verso

- **La collection de l'utilisateur** (fait le 12/10/2026) : 55 cartes photographiées recto puis verso, à la même échelle. `tools/images/cartes.py detecter` range les photos par horodatage, les groupe par deux, trouve la carte sur le drap sombre et la redresse ; chaque carte est ensuite décrite à l'œil (orientation de chaque face, titre, catégories, sujets, temps liturgiques, transcription fidèle du texte imprimé et des dédicaces manuscrites, personnes, dates, lieux, éditeur) dans `paroissien-content/images-pieuses/cartes.json` ; `cartes.py produire` détoure (coins arrondis gardés, le fond devient transparent), écrit recto, verso et vignette en WebP et le catalogue. Photos et cartes de famille restent dans le dépôt privé ; la saveur personnelle les embarque, la sauvegarde complète les emporte.
- **Catégories** : prédéfinies par occasion (baptême, première communion, communion solennelle, confirmation, ordination, vie religieuse, mariage, mementos, prières, pèlerinages, fêtes, œuvres d'art, saints), plus celles que crée l'utilisateur ; une image peut en avoir plusieurs, l'utilisateur en ajoute ou en retire ; la boîte à images se filtre par catégorie. Les souvenirs de famille ne sont jamais proposés en image du jour.
- **Cartes réelles** : montrées dans leurs proportions, coins arrondis et ombre légère ; sous la carte, ce qu'elle dit (date, personnes, lieu, dédicace, texte, éditeur).
- **Taille réelle** : les photos étant à la même échelle, une seule mesure (le grand côté d'une carte, en millimètres) donnera la taille de toutes (`px_par_mm` dans `cartes.json`).
- **Import** (fait le 12/10/2026, 0.6.4) : « Importer une image… » dans la boîte prend un fichier `.paroissien-image`, ou une ou deux images ordinaires (recto, puis verso). Le zip est lu comme une donnée : seuls les noms attendus, 20 Mo au plus chacun, faces décodées puis réencodées. La description reçue (date, personnes, dédicace, catégories) est gardée avec l'image (`personal_card.meta`, base en version 7).
- **Partage** (fait, 0.6.4) : « Partager l'image » envoie les deux faces en JPEG sur fond blanc à n'importe quelle app ; « Pour Paroissien » envoie le `.paroissien-image` (zip : `image.json`, recto, verso, vignette). Les catégories créées par l'utilisateur ne voyagent pas.

## 8. Sauvegarde complète (faite le 12/10/2026)

Décision de l'utilisateur : la sauvegarde emporte **tout**, textes privés compris, pour pouvoir livrer plus tard une app vide, remplie en important ce fichier, en attendant les autorisations. Les textes protégés restent dans l'APK personnel tant qu'il sert aux essais ; la saveur `vide` (aucun livre, `app.paroissien.vide`, « Paroissien vide » sur le téléphone) permet d'essayer dès maintenant l'app livrée vide.

Un seul fichier `paroissien-sauvegarde-<date>.zip`, écrit et relu par le sélecteur de documents d'Android (aucun réseau), depuis Réglages, groupe « Sauvegarde » ; la bibliothèque vide y renvoie :

| Contenu | Source |
|---|---|
| `manifeste.json` | version du format, version de l'app, date, nombre d'éléments |
| `reglages.json` | toutes les préférences (DataStore), clé par clé |
| `donnees.json` | l'export actuel (positions, rubans, images posées, images personnelles), puis les habits des livres et les cadres composés |
| `images/` | recto, verso et vignette des images pieuses personnelles |
| `livres/<dossier>/` | chaque catalogue signé, sa signature et ses paquets : livres de l'APK (publics et privés) et livres importés |
| `vignettes/` | les vignettes gravées et leur catalogue (saveur personnelle), restaurées dans `files/vignettes/` si l'app n'en a pas |

- **Restauration** au choix : fusion (la donnée la plus récente l'emporte et rien n'est effacé, comme l'import actuel des signets) ou remplacement complet, après confirmation. Un fichier d'une version plus récente de l'app est refusé avec un message clair.
- **Livres** : à la restauration, chaque catalogue est revérifié (signature, SHA-256, intégrité SQLite) avant installation dans `files/livres/` ; un livre déjà présent dans la même version est sauté ; un livre présent à la fois dans l'APK et dans une sauvegarde ne compte qu'une fois, dans sa version la plus récente.
- **Sécurité** : seuls les noms attendus sont dépliés (pas de chemin absolu ni de « .. »), 2 Gio au plus.
- **Non sauvegardé** : les caches, la règle « Ne pas déranger » propre à l'appareil.
- **Sauvegarde Android automatique** (cloud) : reste désactivée (PLAN, ADR 008) ; le fichier de sauvegarde ne quitte l'appareil que si l'utilisateur le place ailleurs.

## 9. L'atelier

Un écran par livre, ouvert depuis la bibliothèque (appui long sur la couverture) ou depuis Aa : « Habiller ce livre ». Un habit par livre (décision du 12/10/2026).

**Fait le 12/10/2026 (0.6.1)** : les cadres de chaque livre, en cinq modes (selon le sommaire, un seul cadre, un cadre par chapitre, au hasard dans une sélection avec « tirer à nouveau », aucun), et « Composer un cadre » : frises du haut, des côtés et du pied, coins et médaillon pris dans tous les cadres de la bibliothèque (chacun montré sur un petit cadre), disposition, épaisseurs, fond, filets, aperçu vivant. Les encres de chaque module gardent leur cadre d'origine (« matin.gold ») ; les cadres composés se choisissent comme les autres. Tables `book_look` et `custom_frame` (base en version 6), dans la sauvegarde.

**Reliure (0.6.4)** : en tête de « Habiller ce livre », la couleur de la peau ou de la toile, la matière (maroquin, veau, toile, papier), le décor (doré, à froid, imprimé), le motif du plat, la tranche (dorée, rouge, naturelle), la garde (celle du livre, soie moirée, chaque papier à motif, aucune) et les rubans (ceux qu'on coud, parmi les six couleurs), sur un aperçu de la couverture et de la garde. Chaque champ non retouché garde la valeur du livre (`BindingChoice` dans `book_look`) ; « Reliure d'origine » efface tout. La bibliothèque et le lecteur montrent la reliure retouchée.

**Ajouts du 12/10/2026 (0.6.3)** :

- **Atelier des cadres** sans passer par un livre (réglages, et une tuile sur le rayon) : ses cadres composés, ses motifs.
- **Composer au hasard** : un cadre principal donne le fond, les coins et le haut, un second les côtés ou le pied ; médaillon une fois sur deux ; disposition le plus souvent pleine ; on retouche ensuite.
- **Couleurs** : chaque cadre a ses variantes liturgiques (vert, violet, rouge, rose, blanc, noir, or) ; un cadre composé choisit la sienne ; un livre choisit les couleurs du livre, celles du jour (couleur liturgique) ou une couleur fixe.
- **Dessiner un motif** : on trace du doigt dans une cellule de frise ; chaque trait est simplifié (Douglas-Peucker) puis lissé en courbe (Catmull-Rom), reflété selon la symétrie (gauche-droite, haut-bas), tracé à bouts ronds en trois épaisseurs ; les couleurs se prennent dans l'éventail des encres des cadres (toutes, avec leurs variantes, rangées par teinte). Le motif devient une frise qu'on pose dans un cadre composé.
- **Reliure** : la couverture, la doublure et le second plat pivotent d'un bloc autour du dos, sans se plier.

**Le 13/10/2026 (0.7.0)** :

- **Navigation** : la bibliothèque ne montre que des livres ; cinq onglets (Aujourd'hui, Bibliothèque, Images, Atelier, Signets) ; la recherche devient une loupe et l'import un « + » en tête de la bibliothèque ; le livre de démonstration passe dans l'atelier (« Essais »).
- **Éditeur de couvertures** : un modèle de reliure composé à vue (matière, couleur libre, fers, motif, filets, fleurons, nerfs, tranche), enregistré sous un nom (`custom_frame`, préfixe `couverture-`) et appliqué à un ou plusieurs livres ; « Habiller ce livre » propose les modèles de l'atelier. Cinq matières nouvelles, chacune une texture procédurale qui se répète sans couture : chagrin (galets), vélin (fibres, marbrures douces), velours (poil couché par plages), toile de lin (fils inégaux), papier marbré (volutes, demi-reliure de basane au dos et aux coins).
- **Éditeur de rubans** : les six rubans des livres, retouchés un à un (nom, couleur liturgique ou libre, tissu mat, satin, moiré ou velours, extrémité en biais, droite, en queue d'aronde ou à ferret, croix brodée ou non) ; gardés dans les réglages (`rubans`), donc dans la sauvegarde ; les livres gardent leurs emplacements.
- **Langue par livre** : un livre latin-français a sa propre langue (Aa, « Ce livre ») en plus du réglage général (`BookDressChoice.language`).

**Le 13/10/2026 (0.7.2)**, d'après une vingtaine d'images d'inspiration données par l'utilisateur (plats dorés, roulettes, écoinçons, cartouches, dos à caissons, bordures enluminées). Ces images (banques d'images, enluminure signée) ne sont pas recopiées : seuls les genres sont repris, en dessins originaux.

- **Métaux** (`Metal` : or, argent, bronze, cuivre) : les fers, le titre et le motif du plat ; la tranche dorée ; les ors des cadres de pages (« Ors des cadres » dans l'habit du livre : à plat, ou en reflet d'un métal sur les encres `gold`, `goldLight`, `goldDark`).
- **Plat** : dix motifs (croix, croix tréflée, IHS, chrisme, fleur de lys, lis, rose, Sacré-Cœur, étoile, calice et hostie) ; encadrement du motif (médaillon, losange, cartouche aux angles rentrants, gloire) ; roulette entre les filets (fleurs de lys, palmettes et lotus, rinceaux, dentelle, perles), carrés aux angles de la roulette ; fers d'angle (quadrilobe, fleur de lys, écoinçon de rinceaux, carré, rosace).
- **Dos** : nerfs (0 à 6) et dos à caissons (filets dorés et petit fer dans chaque caisson).
- **Tranche** : dorée (dans un des quatre métaux), rouge sous or, rouge, rouge orangé, bleue, verte, jaune, jaspée, marbrée (caillouté), naturelle ; ciselure d'une tranche dorée (pointillés, losanges, rinceaux) ; un aperçu de la tranche vue de face. La couverture et la page ouverte dessinent la même tranche (`drawPageEdge`).
- **Quatre cadres de pages** (§1) : roulette de fleurs de lys, palmettes et lotus, écoinçons et filets, enluminure aux fraisiers (acanthes bleues et ocre aux couleurs du temps, fraises, églantines, besants à vrilles, oiseau au pied).
- L'éditeur de couvertures et « Habiller ce livre » partagent les mêmes choix (`BindingFields`).

- **Atelier des motifs** (même jour, 0.7.3) : quatre outils. *Trait* à main levée. *Formes* : ligne, losange, rectangle, cercle, ovale, triangle, étoile, arc, spirale, vague ; on les trace d'un coin à l'autre, pleines ou au trait. *Motifs* : une réserve de silhouettes simples à poser d'un toucher (fleurs : fleur de lys, lis, rose, marguerite, tulipe, églantine, bouton, clochette, trèfle, fleuron ; fruits : fraise, cerises, raisin, pomme, poire, grenade, gland, olivier ; feuilles : feuille, vigne, acanthe, palmette, épi, rameau ; symboles : croix, croix pattée, cœur, étoiles, couronne, ancre, poisson, goutte, calice ; volutes : volute, vrille, crosse, rinceau en S). *Choisir* : un doigt déplace l'élément, deux doigts l'agrandissent et le tournent ; curseurs de taille, d'étirement, de rotation et d'épaisseur ; retourner, dupliquer, devant, derrière, supprimer, recolorer. Épaisseur du trait au curseur ; or, argent, bronze et cuivre parmi les couleurs (traits et fond), dessinés en reflet ; « Annuler » revient en arrière d'une retouche ; plein écran (barres du système masquées, réglages à droite en paysage). Les motifs déjà dessinés se relisent tels quels (`MotifStroke` garde ses champs et en gagne).

Idées notées sur les mêmes images, pour plus tard : tranchefiles de couleur ; bandeau latéral sur le plat (frise verticale côté dos) ; pièce de titre et cartouche doré pour le titre ; cadre de page aux angles rentrants (arcs aux coins) ; gaufrage du cuir (décor en relief sans or) et mosaïque de cuirs de couleur ; bandeau latéral enluminé (un fraisier seul, sur le côté extérieur) et lettrines enluminées en tête de section ; d'autres oiseaux et fleurs (ancolie, pensée, chardon) pour varier l'enluminure.

- **Rang** : noble, Bible, simple, sans habit (§4) ; il donne les valeurs de départ des choix suivants.
- **Couverture** : matière et couleur, décor (modèles de la bibliothèque), motif central, titre doré ou non.
- **Gardes** : moiré, asanoha, symboles, ou papier uni ; couleur.
- **Tranche** : dorée, rouge, rouge orangé, naturelle.
- **Pages** : un cadre par section (la règle du sommaire), un cadre unique, ou aucun ; disposition (pleine, latérale, en tête, en équerre) ; couleurs du livre ou couleur liturgique du jour ; vignettes ou non.
- **Rubans** : nombre (1 à 6), couleurs, tissu (mat ou satin), extrémité (biais, queue d'aronde, frange, ferret), croix brodée ou non.
- **Composer un cadre** : choisir dans la bibliothèque la frise de chaque bande, les coins, le médaillon et la palette ; le résultat s'enregistre comme « mon cadre » et se réutilise. On assemble des modules existants, on ne dessine pas à main levée.
- **Aperçu vivant** : le livre fermé, puis ouvert sur la garde et une page encadrée, rubans posés.

Stockage : `user.db`, une table `book_look` (publication → description JSON de l'habit) et une table `custom_frame` (composition JSON qui ne référence que des modules de la bibliothèque par leur nom). La sauvegarde (§8) les reprend.

## 10. Étapes

1. Premier lot : génération, captures, intégration au lecteur, livraison (0.5.7, fait).
2. Moteur v3, rangs d'habillage, cadre simple (fait le 12/10/2026).
3. Second lot : bandeaux latéraux, équerres, gardes ; règle de choix étendue aux nouveaux cadres (fait).
4. Vignettes gravées : outil de préparation, dépôt privé, placement (fait ; d'autres vignettes à photographier).
5. Sauvegarde complète et saveur vide (fait) ; collection d'images pieuses de l'utilisateur, catégories, cartes aux coins arrondis (fait) ; import et partage d'une image à la fois (fait).
6. Couvertures en modules.
7. Atelier : habit par livre, composition de cadres, reliure (fait) ; métaux, motifs, roulettes, fers d'angle, dos à caissons, tranches travaillées et quatre cadres nouveaux (fait, 0.7.2) ; reste le titre doré ou non.
8. Import de livres (txt, epub : fait ; PDF à venir), puis export (epub, txt, PDF).

## 11. Questions ouvertes

- **Page d'ouverture et page courante** (§4) : cadre plein puis bandeau léger dans une même section, ou un seul cadre par section comme aujourd'hui ?
