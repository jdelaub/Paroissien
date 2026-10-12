# Cadres ornementaux

Cadres de page d'après les paroissiens chromolithographiés de la fin du XIXᵉ siècle (domaine public). Le premier est celui de la p.103 « Pour la confession » : baguettes roses croisées terminées par des bourgeons d'or, arcature entrelacée en haut et sur les côtés, chaîne d'anneaux au pied, rosaces aux coins.

## Principe

Un cadre n'est pas une image. Il se compose de **modules vectoriels répétés** :

| Module | Rôle |
|---|---|
| Frise | Bandes haute et latérales, répétée |
| Frise du pied | Bande du pied, plus haute (convention des manuscrits) |
| Coin | Rosace |
| Complément de coin | Fleur de lys qui comble les coins allongés du pied |
| Terminaison | Bout des baguettes croisées |

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
| `generate.py` | Source unique : géométrie, encres, palettes, mise en page |
| `svg/*.svg` | Un fichier par module, ouvrable dans Inkscape (généré) |
| `apercu.html` | Aperçu autonome du même moteur en JavaScript : thèmes, couleurs liturgiques, tailles d'écran (généré) |
| `apercu/` | Gabarit et moteur JavaScript de l'aperçu |
| `android/core/designsystem/.../frame/ConfessionFrame.kt` | Données du cadre pour Compose (généré) |
| `android/core/designsystem/.../frame/OrnamentalFrame.kt` | Composable et moteur de dessin |

```bash
python3 -I tools/cadres/generate.py          # régénère le Kotlin, les SVG et l'aperçu
python3 -I tools/cadres/generate.py --check  # échoue si une sortie commitée n'est plus à jour
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

`FramedPageSample` en donne un exemple complet. `OrnamentalFrameScreenshotTest` le capture en clair, en sombre (violet) et en nuit.

Le cadre est dessiné dans sa propre couche (`graphicsLayer` + `drawWithCache`). Le texte qui défile ne le fait donc pas redessiner. Les chemins sont analysés une seule fois par cadre.

## Limites connues

- **Interprétation géométrique.** Les frises ne sont pas un décalque : la bande du pied de l'original a des boucles nouées plus riches. Un scan à plat (600 à 1200 dpi) permettrait de serrer le dessin.
- **Or mat.** L'or est un aplat : pas encore de reflet métallique.
- **Épaisseurs fixes.** Les épaisseurs sont en dp (20 et 34) ; sur tablette, il faudra les indexer sur la largeur.
- **Lettrine.** Elle reste dans la ligne ; la lettrine sur plusieurs lignes relève du moteur de lecture.
- **Contraste élevé.** La variante n'est pas traitée. Le cadre devrait sans doute s'y réduire à ses baguettes.
