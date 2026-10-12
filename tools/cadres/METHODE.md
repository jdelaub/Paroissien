# Méthode : d'une photo de paroissien à un cadre Compose

Ce document décrit comment le cadre de la p.103 a été obtenu, pour que les cadres suivants n'aient pas à refaire ce travail. Il complète le [README](README.md), qui décrit le moteur et ses fichiers.

## 1. Principe retenu

- **Redessiner, pas décalquer.** Une vectorisation automatique (potrace, vtracer, Inkscape) d'une photo donne des milliers de nœuds et des lignes qui tremblent, surtout pour les entrelacs. On relève plutôt la structure du cadre, puis on redessine chaque module en géométrie propre dans `generate.py`.
- **Un cadre est un système de modules**, jamais une image entière :
  - les **baguettes** : rectangle extérieur et baguettes intérieures croisées, terminées par des bourgeons ;
  - les **frises** : un élément qui se répète ;
  - les **coins** : un motif dans un carré ;
  - les **terminaisons** au bout des baguettes.
- **Une encre par pierre lithographique.** La chromolithographie imprime une pierre par couleur. Chaque tracé référence donc une encre nommée (`ground`, `accent`, `gold`…), jamais une couleur. C'est ce qui permet les palettes liturgiques et l'atténuation en thème sombre.
- **Lisible à 20 dp.** Sur téléphone, une bande latérale fait environ 20 dp, soit 60 px à 3x. Le motif doit rester lisible à cette taille : peu de détails par élément, des contours nets.

## 2. Matériel source

- **Photos actuelles.** Les photos de travail sont dans le zip d'import (`sources/photos/`), hors du dépôt : il est public et ce sont des photos personnelles. Le catalogue du §7 donne la rotation de chacune.
- **Ce que permet une photo de téléphone.** Elle suffit à relever la structure et la palette, à condition de corriger le blanc (§4). Elle ne suffit pas à décalquer : perspective, courbure près de la reliure, reflets sur l'or.
- **Pour serrer le dessin plus tard.** Il faut un scan à plat de 600 à 1200 dpi, sans forcer la reliure, ou un appareil sur pied avec une lumière diffuse.

## 3. Relever la structure (`analyse/decoupe.py`)

```bash
python3 -I tools/cadres/analyse/decoupe.py p103-pour-la-confession.jpg --sortie /tmp/cadre --rotation 0 --grille 100 \
    --zone pied 300 2470 900 2740 --zone coin 120 860 700 1300 --zoom 2
```

- **`-oriente.png`.** La photo, orientation EXIF appliquée puis rotation. Toutes les coordonnées se rapportent à cette image.
- **`-grille.png`.** Une vue réduite avec un quadrillage coté, pour relever les zones.
- **Un zoom par `--zone`**, pour lire le dessin d'un module.

Ce qu'on relève, avec les valeurs de la p.103 (photo de 1638 × 4000 px) :

| Mesure | p.103 | Usage |
|---|---|---|
| Épaisseur des bandes haute et latérales | ≈ 120 px | Base de l'unité de module (100 unités) |
| Épaisseur de la bande du pied | ≈ 230 px (× 1,9) | Pied de 34 dp pour des côtés de 20 dp |
| Période de la frise | ≈ 230 à 245 px | Pas nominal relatif à l'épaisseur |
| Baguettes | Rose, contour brun, filet clair au centre ; les intérieures croisent les extérieures et dépassent | `bar`, `barOutline`, `barLight`, `barOverhang` |
| Terminaisons | Anneau puis bourgeon trilobé d'or, 3 par coin (2 dans l'axe, 1 en diagonale) | Module `terminal` |
| Coins | Carrés en haut ; rectangles plus hauts au pied | Rosace, plus une fleur de lys pour l'excédent |

Orientation : la plupart des cadres ont une **bande du pied plus large**, la marge de pied des manuscrits. Il faut la garder : c'est un marqueur fort du style.

## 4. Mesurer la palette (`analyse/palette.py`)

```bash
python3 -I tools/cadres/analyse/palette.py p103-pour-la-confession.jpg --papier 500 2760 800 2790 \
    --cadre 300 2490 900 2720 --k 8 --apercu /tmp/cadre/quantif.png
```

1. **Correction du blanc.** On prend une zone de papier nu près du cadre, et chaque canal est multiplié pour que ce papier devienne `#F1E8D6`. Sur la p.103, le papier mesurait `#8A8074` : la photo était sous-exposée d'environ ×1,8. Sans cette correction, tous les groupes sortent gris.
2. **Regroupement.** Les k-moyennes groupent les pixels du cadre ; 8 groupes couvrent encres, contour et papier. L'image `--apercu` vérifie qu'à chaque encre correspond un groupe.
3. **Choix final.** Les encres de `INKS` se choisissent à l'œil à partir des groupes. L'éclairage est rarement uniforme (sur la p.103, le côté droit était plus clair), et l'or et l'argent ressortent ternes en photo : on les remonte un peu.

Palette retenue pour la p.103 :

| Encre | Valeur | Rôle |
|---|---|---|
| `ground` | `#2E4865` | Fond bleu nuit des frises |
| `accent` | `#D8745A` | Saumon des lunettes |
| `leaf` | `#8EABBA` | Bleu clair des palmettes |
| `gold` | `#B89C5A` | Or (ocre métallique) |
| `strand` | `#D3CEC1` | Argent des entrelacs |
| `bar` | `#D4958A` | Baguettes roses |
| `barLight` | `#F3DDD3` | Filet clair au centre des baguettes |
| `outline` | `#33261F` | Contour brun foncé |

Les palettes liturgiques (`PALETTES`) ne remplacent que 2 ou 3 encres, en général `ground` et `leaf`. Le saumon, l'or et les baguettes gardent l'identité du livre.

## 5. Redessiner les modules (`generate.py`)

### Repère et unités

- **Repère d'une frise.** u va le long de la bande, v en profondeur, de 0 (bord extérieur) à 100 (côté texte). 100 unités valent l'épaisseur de la bande, quelle que soit sa taille à l'écran.
- **Repère des coins.** Une boîte de 100 × 100.
- **Repère des terminaisons.** Elles pointent vers +x depuis l'origine, qui est le bout de la baguette.
- **Écran.** Le moteur (Kotlin et JS) applique la transformation affine propre à chaque bande, pour la direction et le sens de l'extérieur. Les bandes gauche et du pied sont donc des miroirs : l'entrelacs y change de sens, ce qui ne se voit pas.

### Primitives disponibles (`commun.py`)

| Fonction | Forme |
|---|---|
| `arc(cx, cy, r, a0, a1)` | Arc de cercle en cubiques de 90° au plus (angles en degrés, y vers le bas) |
| `circle` | Cercle complet |
| `teardrop(L, W)` | Goutte ou pétale vers +x |
| `palmette(L)` | Trois lobes vers +x |
| `trefoil(size)` | Bourgeon trilobé vers le haut |
| `fleur_de_lis(h)` | Fleur de lys vers le haut |
| `ellipse`, `rect`, `polygon`, `diamond`, `quatrefoil` | Formes simples |
| `smooth(points)`, `spiral(cx, cy, r0, r1, tours)` | Courbe lisse passant par des points ; volute |
| `leaf(L, W, bend)` | Feuille lancéolée vers +x |
| `affine(tx, ty, rot, sx, sy)`, `mirror_x`, `mirror_y` | Placement : translation, rotation, échelle, miroir |

### Conventions de dessin

- **Formes avec contour : `outlined(ink, path)`.** On trace d'abord un trait brun du double de l'épaisseur du contour, puis on remplit par-dessus. Seul le contour extérieur de l'union reste visible, sans calcul d'union.
- **Rubans d'entrelacs : `strand(path)`.** Un trait brun large, puis un trait argent plus étroit.
- **Sens des sous-chemins.** `fill()` oriente tous les sous-chemins dans le même sens, avec la règle de remplissage nonzero. Une forme en miroir (`mirror_x`) inverse le sens de tracé ; sans cette normalisation, elle perce un trou là où elle recouvre sa voisine.
- **Passes `base` et `over`.** Toutes les instances d'une frise dessinent `base`, puis toutes dessinent `over`.
  - Dans l'arcature, l'arc k+1 est dessiné après l'arc k : il passe dessus au croisement et dessous au suivant. L'alternance est automatique, donc pas de passe `over`.
  - Dans la chaîne d'anneaux, l'anneau +1 passe naturellement dessus en bas. En haut, la passe `over` redessine un tronçon de l'anneau 0.
- **Tronçons de dessus.** Leur contour est plus court que leur ruban (±16° contre ±20°). Sinon, le bout du contour laisse un liseré sombre au travers du ruban.
- **Phase.** C'est la position de l'élément dans sa période : 0 pour un élément centré sur les jonctions (l'arcature), 0,5 pour un élément décalé d'un demi-pas (les anneaux).
- **Débord.** Les éléments débordent sur leurs voisins. Le moteur dessine de i = -1 à n+1 et coupe à la bande ; il n'y a pas de découpage par tuile, donc pas de couture visible.

### Règles de lisibilité

- **Lentilles.** Une lentille plus étroite que deux rubans disparaît. C'est arrivé dans la chaîne : la lentille de 11 unités était entièrement couverte par les rubans de 13,4.
- **Recouvrement des anneaux.** Garder 2r − P ≥ 18 unités si l'on veut voir l'espace entre deux anneaux.
- **Proportions.** Un motif fait au moins 12 à 15 unités : bourgeon d'or, goutte, palmette. Contour : 1,6 unité pour les feuilles, 2,2 pour les rubans.

## 6. Exporter et vérifier

```bash
python3 -I tools/cadres/generate.py           # écrit ConfessionFrame.kt, svg/*.svg, apercu.html
python3 -I tools/cadres/generate.py --check   # échoue si une sortie commitée n'est plus à jour
```

1. **Le rendu.** `python3 -I tools/cadres/rendu.py <nom> --photo <photo>` dessine une page de 360 × 760 dp à côté de la photo, `--modules` la planche des modules ; `--theme` et `--couleur` changent de thème et de palette. C'est la boucle de travail la plus rapide. L'aperçu `apercu.html`, dans un navigateur, montre tous les cadres de façon interactive.
2. **Le rendu Android.** Sur la VM, un seul build à la fois, et pas pendant qu'une autre session compile :

   ```bash
   cd android && JAVA_HOME=~/Android/jdk-21 ./gradlew :core:designsystem:recordRoborazziDebug
   ```

   Puis regarder les images de `core/designsystem/src/test/screenshots/` avant de les committer.

## 7. Catalogue des cadres relevés

Les photos sont hors du dépôt (photos personnelles), dans `cadres-photos/` à côté du dépôt, redressées. Chaque cadre a son fichier `cadres/<nom>.py`, dont la docstring décrit le relevé et les simplifications.

| Cadre | Photo | Style |
|---|---|---|
| `doublure` | `contreplat-doublure.jpg` | Roulette dorée sur cuir, autour de la soie moirée (dessinée par l'app) |
| `matin` | `p005-matin.jpg` | Treillis à cœurs bleus sur or ; médaillons au pied |
| `soir` | `p025-soir.jpg` | Lunettes d'or sur fond sang-de-bœuf ; médaillons au milieu des bandes ; dragons aux coins du pied |
| `avant_messe` | `p045-avant-la-messe.jpg` | Camaïeu or et argent : rinceau, carreaux, nattes ; têtes de chien aux coins hauts |
| `pendant_messe` | `p057-pendant-la-messe.jpg` | Palmettes bleues, feuilles de vigne ; grand panneau du pied à rinceaux |
| `confession` | `p097-confession.jpg` (et p.103) | Arcature entrelacée, chaîne d'anneaux, baguettes roses croisées |
| `communion` | `p119-communion.jpg` | Guirlande d'œillets roses sur or olive |
| `vepres` | `p129-vepres.jpg` | Entrelacs bleu clair et acanthes d'or sur brun ; médaillons latéraux |
| `complies` | `p151-dimanche.jpg` | Câble d'argent et acanthes d'or sur mauve ; médaillons latéraux |
| `mariage` | `p167-mariage.jpg` | Rinceaux d'acanthe pastel sur vert ; baguettes d'or croisées |
| `renaissance` | `renaissance-vases.jpg` | Candélabres, guirlande, moulure d'architecture sur bleu |
| `vigne` | `vigne-oiseaux.jpg` | Vigne et oiseaux sur papier nu, cadre ouvert en haut |
| `gothique` | `gothique-quadrilobes.jpg` | Crêtes rouges et or, quadrilobes saillants aux coins |
| `evangeliste` | `saint-matthieu-frontispice.jpg` | Gravure : colombe, anges, chérubins, rinceaux ; ouverture de chaque évangile |

Le second lot (`cadres-photos/lot2/`) attend le moteur v3 : bandeaux latéraux, compositions partielles, cadre simple, gardes et couvertures (voir `docs/planning/ATELIER.md`).

## 8. Ajouter un cadre : liste de contrôle

1. Choisir la photo et sa rotation (§7). Avec `decoupe.py`, relever les épaisseurs, la période et les zones de coin.
2. Avec `palette.py`, mesurer les encres ; noter les valeurs retenues dans ce document.
3. Créer `cadres/<nom>.py` : ses fonctions de module, `ORDRE` (la page du paroissien) et `spec()`, qui renvoie `commun.cadre(...)`. `generate.py` le trouve seul et écrit `<Nom>Frame.kt`, les SVG et l'aperçu.
4. Itérer avec `rendu.py` jusqu'à un rendu lisible à 360 dp.
5. Lancer `generate.py`, réenregistrer les captures (la galerie `OrnamentalFramesGalleryTest` prend tous les cadres du registre), les regarder, puis vérifier `generate.py --check`.

## 9. Pièges déjà rencontrés

- **Sortie périmée.** Une modification de `generate.py` doit être suivie de sa relance. Sans le champ `phase`, toutes les instances se dessinaient au même endroit : le canvas ignore une transformation NaN. `--check` évite ce cas.
- **Miroir et trou.** Voir « Sens des sous-chemins » au §5.
- **Liserés de raccord.** Voir « Tronçons de dessus » au §5.
- **`TextIndent` en `em`.** Il plante le rendu Compose sur desktop (Skia) ; il faut des `sp`. Android l'accepte, mais `sp` marche partout.
- **Mode nuit.** Un cadre aux couleurs du livre éblouit sur fond noir. D'où `FrameStyle.ornamentDim` : les encres sont rapprochées du fond.
- **Maven Central en 429.** Dans un conteneur cloud, Maven Central renvoie 429 à Gradle. Un script d'initialisation local, dans `~/.gradle/init.d/`, place le miroir `https://maven-central.storage-download.googleapis.com/maven2/` en tête des dépôts, sans rien modifier dans le projet.
