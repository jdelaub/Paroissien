# Cadres ornementaux

Cadres de page d'après un paroissien chromolithographié de la fin du XIXᵉ siècle (domaine public) : un cadre par section du livre (prières du matin, du soir, avant et pendant la messe, confession, communion, vêpres, complies, mariage…), plus quelques cadres sans texte, le frontispice gravé d'un évangéliste et la bordure dorée de la doublure. Le premier relevé est celui de la p.103 « Pour la confession » : baguettes roses croisées terminées par des bourgeons d'or, arcature entrelacée en haut et sur les côtés, chaîne d'anneaux au pied, rosaces aux coins.

## Principe

Un cadre n'est pas une image. Il se compose de **modules vectoriels répétés** :

| Module | Rôle |
|---|---|
| Frises | Bande haute, bandes latérales (la gauche en miroir de la droite) et bande du pied, souvent plus haute (convention des manuscrits) ; chacune répétée |
| Pièces centrales | Médaillon ou losange au milieu d'une bande, qui devient alors deux demi-bandes symétriques |
| Coins | Dessinés pour le coin haut gauche, en miroir pour les autres ; le coin du pied peut être plus haut |
| Complément de coin | Comble les coins allongés du pied |
| Filets et baguettes | Filets tout autour à une profondeur donnée ; baguettes croisées aux coins, avec leurs terminaisons |
| Pièces de bout | Posées une fois à chaque bout d'une bande (socle et couronnement d'un candélabre) |
| Ornements extérieurs | Hors du cadre, aux coins et au milieu des côtés |

**Dispositions** (moteur v3). Chaque bande peut manquer : un cadre est plein, latéral (bande extérieure seule), en tête, en équerre (Γ : tête et côté extérieur ; L : côté extérieur et pied)… Le côté extérieur suit le folio : à droite sur un recto, à gauche sur un verso. Une bande sans voisine court jusqu'au bord du cadre, les coins n'existent que là où deux bandes se rencontrent, les filets suivent les bandes présentes, et le texte reprend la place d'une bande absente. Les filets se placent en proportion de chaque bande ou à distance fixe du bord ; pièces centrales et coins peuvent déborder vers le texte et passer par-dessus filets et baguettes ; une pièce « droite » tourne avec la bande sans passer en miroir, pour qu'une inscription reste lisible.

**Gardes à motif.** Une tuile répétée en réseau carré ou en quinconce (`commun.motif`, fichiers `gardes/<nom>.py`), peinte une fois dans une petite image puis répétée par un shader (`PatternPaper`).

Pour chaque bande de longueur `L` :

1. On pose `n = arrondi(L / pas nominal)` modules.
2. On ajuste le pas à `L / n` pour que la bande tombe juste entre les coins, comme `border-image-repeat: round` en CSS. La déformation reste sous 10 % dès 5 modules.

Un NinePatch ne convient pas : il étire le motif au lieu de le répéter.

Les éléments d'une frise se chevauchent. Toutes les instances dessinent leur passe `base`, puis toutes leur passe `over`. L'élément dessiné en dernier passe dessus au croisement, ce qui alterne dessus et dessous dans l'entrelacs.

Chaque tracé référence une **encre** (`GROUND`, `ACCENT`, `GOLD`…) et non une couleur. `FrameStyle.inks` résout les couleurs à l'exécution en deux étapes :

- **Couleur liturgique.** Si elle est fournie, elle remplace une partie des encres ; sinon, le cadre garde les couleurs du livre.
- **Thème sombre.** Les encres sont rapprochées du fond de page selon `FrameStyle.ornamentDim` : 0,28 en sombre, 0,6 en nuit.

## Fichiers

| Fichier | Rôle |
|---|---|
| `METHODE.md` | Comment passer d'une photo de paroissien à un cadre : relevé, palette, redessin, pièges, catalogue des autres cadres |
| `analyse/decoupe.py`, `analyse/palette.py` | Outils de relevé : orientation et quadrillage des photos, mesure des encres |
| `cadres/<nom>.py` | Un cadre : géométrie, encres, palettes, mise en page (fonction `spec()`, attribut `ORDRE`) |
| `gardes/<nom>.py` | Une garde à motif (fonction `spec()` qui renvoie `motif(...)`) |
| `commun.py` | Primitives partagées : chemins, formes, opérations de dessin, constructeurs de modules (`frise`, `boite`, `piece`, `bouts`, `filet`, `cadre`, `motif`) |
| `generate.py` | Trouve les cadres et écrit leurs sorties |
| `rendu.py` | Rendu PNG d'un cadre (page recto ou verso, ou planche des modules) ou d'une garde, à côté de sa photo : la boucle de travail |
| `vignettes.py` | Vignettes gravées : redresse une gravure photographiée et n'en garde que l'encre, en masque alpha, dans le dépôt privé (jamais ici : ce sont des photos personnelles) |
| `svg/<nom>/*.svg` | Un fichier par module, ouvrable dans Inkscape (généré) |
| `apercu.html` | Aperçu autonome du même moteur en JavaScript : cadres, thèmes, couleurs liturgiques, tailles d'écran (généré) |
| `apercu/` | Gabarit et moteur JavaScript de l'aperçu |
| `android/core/designsystem/.../frame/<Nom>Frame.kt`, `Cadres.kt` | Données de chaque cadre pour Compose, et leur registre (générés) |
| `android/core/designsystem/.../frame/<Nom>Pattern.kt`, `Gardes.kt` | Données de chaque garde, et leur registre (générés) |
| `android/core/designsystem/.../frame/OrnamentalFrame.kt`, `EndpaperPattern.kt` | Composables et moteurs de dessin |

```bash
python3 -I tools/cadres/generate.py          # régénère le Kotlin, les SVG et l'aperçu
python3 -I tools/cadres/generate.py --check  # échoue si une sortie commitée n'est plus à jour
python3 -I tools/cadres/rendu.py matin --photo …/p005-matin.jpg   # rendu PNG à côté de la photo
```

Après une modification du dessin, réenregistrer les captures :

```bash
cd android && JAVA_HOME=~/Android/jdk-21 ./gradlew :core:designsystem:recordRoborazziDebug
```

## Utilisation

```kotlin
OrnamentalFrame(ConfessionFrame, ConfessionStyle, liturgicalColor = LiturgicalColor.VIOLET, headerHeight = 22.dp,
    header = { /* titre courant, folio */ }) { padding ->
    Column(Modifier.padding(padding).fadingEdges().verticalScroll(rememberScrollState())) { /* texte */ }
}
```

`OrnamentalFrames` (généré) liste tous les cadres, `EndpaperPatterns` toutes les gardes. `FramedPageSample` en donne un exemple complet ; `outerOnRight = false` le montre en verso. `OrnamentalFrameScreenshotTest` capture le cadre de la confession en clair, en sombre (violet) et en nuit ; `OrnamentalFramesGalleryTest`, tous les cadres en clair (les cadres partiels aussi en verso), puis toutes les gardes.

Le cadre est dessiné dans sa propre couche (`graphicsLayer` + `drawWithCache`). Le texte qui défile ne le fait donc pas redessiner. Les chemins sont analysés une seule fois par cadre.

## Limites connues

- **Interprétation géométrique.** Les frises ne sont pas un décalque : la bande du pied de l'original a des boucles nouées plus riches. Un scan à plat (600 à 1200 dpi) permettrait de serrer le dessin.
- **Or mat.** L'or est un aplat : pas encore de reflet métallique.
- **Épaisseurs fixes.** Les épaisseurs sont en dp (20 et 34) ; sur tablette, il faudra les indexer sur la largeur.
- **Lettrine.** Elle reste dans la ligne ; la lettrine sur plusieurs lignes relève du moteur de lecture.
- **Contraste élevé.** La variante n'est pas traitée. Le cadre devrait sans doute s'y réduire à ses baguettes.
