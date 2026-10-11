# 07 — La page qui tourne et le rendu « papier »

> Agent 07 · rapport de planification · 11 octobre 2026
> **Périmètre** : animation de tourne-page, double page en paysage, aspect du papier (texture, thèmes, couleurs), typographie vue sous l'angle du rendu (polices, glyphes, lettrines, rubriques, césure).
> **Hors périmètre** : pagination et mise en page du texte (06), navigation et chrome (08), rubans et images pieuses (09), architecture des modules (10), préparation du contenu (11), licences (12).
> Les dépôts cités ont été clonés en lecture seule (`git clone --depth 1`) dans le scratchpad et leurs métadonnées vérifiées le 11 octobre 2026 avec l'API GitHub. Les couvertures de glyphes de la §2.10 viennent d'une lecture directe des tables `cmap`, `GSUB` et `GPOS` des fichiers TTF de google/fonts (commit `bd8f81d` du 9 octobre 2026).
> En cours de mission, le quota de recherche web partagé entre les agents a été épuisé. Les points que je n'ai pas pu revérifier sont marqués **(à vérifier)**.

---

## 1. Résumé

- **Technique recommandée** : un pli **cylindrique en AGSL** (`RuntimeShader` appliqué en `RenderEffect` sur le calque Compose de chaque face), réservé à **Android 13 / API 33 et plus**. Il prend trois passes par tour (recto, verso, ombre) et ne coûte rien au repos. La géométrie est une fonction pure en Kotlin, testable en JVM.
- **Référence de code** : **Mekuri** (AniCanon, licence MIT, v0.3.0 du 14 septembre 2026) fait exactement cela en Compose : page simple, double page avec une vraie feuille recto-verso, gestes, retour haptique et « réduire les animations ». Le projet a **un mois et zéro étoile**. Ses performances Android n'ont été **mesurées que sur émulateur**, et il impose `minSdk 33` et GitHub Packages avec jeton. Je recommande de **reprendre et adapter le code** (avec sa notice MIT) plutôt que de l'ajouter comme dépendance.
- **Couverture** : l'API 33+ ne représente que **≈ 69 % des appareils** (StatCounter, avril 2026). Il faut donc un **repli soigné** : une « feuille qui glisse », avec son ombre, sur API 26 à 32 pour le MVP, puis éventuellement en V1 un pli plat façon *pagecurl* (Apache-2.0, sans maintenance depuis février 2024) ou un maillage `drawBitmapMesh`.
- **Gestes** : zones de tap 25 / 50 / 25 %, glisser dont le coin suit le doigt, validation à 35 % du parcours ou au-delà de ~600 dp/s, retour élastique si l'on annule, trois signaux haptiques (soulèvement, seuil, atterrissage). Il faut **exclure le geste « retour » du système** aux coins (point oublié par Mekuri) et proposer des actions TalkBack en français. Le son de papier est optionnel et désactivé par défaut.
- **Budget** : 8,3 ms par image à 120 Hz. Viser moins de 2 ms sur le fil UI (aucune recomposition de page pendant le tour) et moins de 5 ms GPU sur un milieu de gamme. Pendant un tour, prévoir environ 31 Mo de calques hors écran, libérés à l'atterrissage. Mesurer sur **téléphone physique** (Macrobenchmark ou `gfxinfo`), jamais sur la VM.
- **Papier** : un **bruit procédural** (fBm + fibres) de ±1,5 % de luminance au plus, généré une fois en tuile de 512 px (tous niveaux d'API) ou calculé en AGSL. Quatre palettes : Clair ivoire, Sépia, Sombre et Nuit. **Tous les contrastes calculés dépassent l'AA**, même sur papier texturé : 15,3:1 pour l'encre et 6,7:1 pour le rouge en thème clair.
- **Polices** : **EB Garamond** (OFL) comme texte courant. Elle a ℣ ℟, les petites capitales, les chiffres elzéviriens et ❦ ❧ ☙. Il lui manque **✠, ✝, ⁂ et l'espace fine insécable U+202F**, indispensable en français. Je propose de les prendre dans **Junicode 2** (OFL, v2.226), qui a tout, via des segments de police ou un `CustomFallbackBuilder`.
- **Césure (constat vérifié dans le code AOSP)** : Android hyphène le français (`fr`, 2/3) et le latin (`la`, 2/2). Mais les motifs latins embarqués (Beccari 2016) **ignorent les voyelles accentuées**, et minikin renonce alors à couper le mot. Les mots liturgiques accentués (*Dóminus*, *sǽcula*) ne sont donc **jamais coupés**. Les motifs « latin liturgique » sont présents dans les sources d'AOSP mais **non compilés**. Solution : **insérer des tirets conditionnels (U+00AD) à la préparation du contenu** (11), avec les motifs `la-x-liturgic` (licence MIT).
- **Double page** : une feuille aussi large que la double page, qui pivote autour de la reliure. Ombre de reliure en exponentielle (−12 % au maximum). La **tranche s'épaissit selon la progression** (0 à 6 dp de chaque côté), avec en option une tranche rouge ou dorée, comme sur les missels. Les rubans et images (09) dépassent de la tranche.
- **Prototype en 3 étapes** sur téléphone physique, avec APK construits en CI : ① une feuille qui tourne en portrait, ② le papier et l'encre (thèmes, polices, césure en A/B), ③ le livre ouvert (paysage, reliure, tranche).

---

## 2. Constats détaillés

### 2.1 Panorama des techniques de tourne-page (octobre 2026)

| Technique | API min. | Réalisme | Coût d'intégration Compose | État (vérifié le 11/10/2026) | Verdict |
|---|---|---|---|---|---|
| **AGSL `RuntimeShader` + `RenderEffect`** sur le calque vivant de chaque face (approche Mekuri) | 33 | ★★★ cylindre, verso, ombres par pixel | Faible : un `Modifier.graphicsLayer`, sans capture d'image ni GL | Mekuri v0.3.0, MIT, créé le 9 septembre 2026, 0 étoile | **Recommandé (≥ 33)** |
| AGSL `RuntimeShader` comme `ShaderBrush` sur des **instantanés** (`GraphicsLayer.toImageBitmap()`) | 33 | ★★★ | Moyen : il faut capturer la page avant le tour, environ 10 Mo par page en 1080×2400 | — | Variante si les calques vivants posent problème |
| OpenGL ES (harism/android-pagecurl) | 9 (ES 1.0) | ★★★ cylindre, recto-verso par texture | Élevé : `GLSurfaceView`/`TextureView`, textures à charger, Views et non Compose | Apache-2.0, 1 674 étoiles, dernier push en 2017 (HEAD de 2012) | Exemple historique seulement |
| OpenGL ES 2.0 (eschao/android-PageFlip) | ~14 | ★★★ page simple **et double page** | Élevé (même raison) ; distribué par JitPack 1.0.2 | Apache-2.0, 1 746 étoiles, dernier commit le 11 avril 2024 | Utile pour lire comment il gère la double page |
| **Pli plat** Canvas (`clipPath` + contenu en miroir + ombres), oleksandrbalan/**pagecurl** | 21 | ★★ pas de cylindre, mais un verso « par transparence » réglable (`backPageContentAlpha`) | Faible : Compose natif | Apache-2.0, v1.5.1, 457 étoiles, 8 issues ouvertes, **dernier commit le 9 février 2024** (Compose BOM 2024.02), page simple seulement | Candidat de **repli V1** pour l'API 26 à 32 (à adapter) |
| `Canvas.drawBitmapMesh` avec un tableau de couleurs | 18 en accélération matérielle | ★★★ vrai cylindre par maillage | Moyen : instantané en bitmap, maillage de recto et de verso | C'était le premier design de Mekuri, abandonné quand le projet est passé en minSdk 33 | Repli V1 alternatif |
| `Canvas.drawVertices` / `Vertices` de Compose | 29 en accélération matérielle | ★★★ | Moyen | — | Pas d'avantage sur `drawBitmapMesh` |
| Rotation 3D rigide (`graphicsLayer { rotationY; cameraDistance }`) autour de la reliure | 21 | ★ la feuille reste rigide | Très faible | — | Repli acceptable **en double page** sous l'API 33 |
| **Glissement de feuille** (translation et ombre portée) / **fondu** | 21 | — | Très faible | — | **Repli MVP** et option « réduire les animations » |

Notes :
- *pagecurl-cmp* (Zeyad-37, fork Compose Multiplatform de pagecurl, créé en août 2026, une étoile) n'apporte rien pour un projet Android seul.
- Mekuri documente ce qu'il a appris en mesurant (`docs/porting.md`). Plusieurs leçons sont directement réutilisables :
  1. les faces doivent être **figées pendant le tour** (pas de chargement ni d'animation à l'intérieur) ;
  2. une feuille **non soulevée reste opaque**, sinon elle prend l'air d'un papier calque ;
  3. en double page, **la feuille couvre toute la largeur de la double page** et pivote autour de la reliure ;
  4. ne **jamais reconstruire la face verso à chaque image** ;
  5. les demi-pages se placent par transformation (`graphicsLayer.translationX`), jamais par une taille plus grande que les contraintes ;
  6. la lecture du réglage « réduire les animations » se fait **une seule fois par Context** (à améliorer, voir §2.7).
- Mekuri ne règle pas l'**exclusion du geste « retour » du système** et ses libellés d'accessibilité (« Next page ») sont en anglais, codés en dur.

### 2.2 Physique de l'enroulement

#### Modèle du cylindre

La feuille est un plan d'épaisseur nulle qui s'enroule sur un cylindre de rayon *R*. L'axe du cylindre est **quasi parallèle à la reliure**. Soit *u* la distance, mesurée sur la feuille dépliée, entre un point et l'axe, comptée positivement vers le bord libre.

```
             vue en coupe (axe ⟂ à l'écran)
     z ↑            ___
       |          /     \   ← nappe haute : on voit le VERSO
       |  2R ....|.......\..........  partie retournée, posée à plat (u > πR)
       |         |        )
       |  0 _____|______ /        ← nappe basse : RECTO, assombri vers la crête
       +---------+-------+------→ x (écran)
              axe       axe+R
```

- **Projection directe** (feuille vers écran) :
  - si *u ≤ 0*, la feuille est à plat : *x = u* ;
  - si *0 < u < πR*, la feuille est sur le rouleau : *x = R·sin(u/R)* et *z = R·(1 − cos(u/R))* ;
  - si *u ≥ πR*, la feuille est retournée et à plat : *x = −(u − πR)* et *z = 2R*.
- **Projection inverse** (pour chaque pixel d'écran, c'est ce que fait le shader). Soit *d* la distance horizontale du pixel à l'axe :
  - si *d > R* : aucune feuille ; on voit la page du dessous et l'ombre de contact ;
  - si *0 ≤ d ≤ R* : deux candidats, *s₁ = R·asin(d/R)* (recto, nappe basse) et *s₂ = πR − s₁* (verso, nappe haute). **Le verso l'emporte** s'il existe (*s₂ ≤ longueur soulevée*), sinon c'est le recto si *s₁ ≤ longueur soulevée* ;
  - si *d < 0* : si *πR − d ≤ longueur soulevée*, c'est la partie retournée (verso) ; sinon le recto plat est échantillonné en *p*.
- **Coin d'abord** : on incline l'axe par cisaillement, `axis(y) = axis0 + shear·(y − h/2)`. Avec `shear > 0`, le coin haut part en premier ; avec `shear < 0`, c'est le coin bas. Le signe vient de la moitié de page où le doigt a saisi. Mekuri utilise `cornerShear = 0,10`, plus un « arc » du pli (`creaseBow = 0,35·W·p(1−p)`) et un rayon qui s'ouvre vers le coin (`R = 0,25·W` au bout tenu, +0,25 par largeur). Ce sont de bonnes valeurs de départ.
- **Pli libre ou axe quasi vertical** : le modèle iBooks place le pli sur la médiatrice entre le coin et le doigt, avec n'importe quel angle. Il est spectaculaire mais instable dans la main : la feuille semble se déchirer quand le doigt descend. Pour un livre de prière, je recommande l'**axe quasi parallèle à la reliure avec un cisaillement borné** (environ ±15°). La lecture est plus calme et le suivi au doigt plus fiable.
- **Atterrissage** : sur les derniers 30 % du tour, le rouleau et le cisaillement s'aplatissent, pour que le pli rejoigne la reliure (Mekuri : `landingFraction = 0,3`). Sans cela, un rouleau resterait « debout » sur la reliure à la fin du tour.

#### Lumière et ombres

1. **Recto sur le rouleau** : on l'assombrit vers la crête, `mix(1, dim, 1 − √(1 − (d/R)²))`, avec `dim ≈ 0,86`.
2. **Verso** : éclairage diffus selon la normale *(n_x, n_z)* face à une lumière fixe, par exemple (0,45 ; 0,89) ; l'assombrissement minimal vaut 0,86.
3. **Ombre de contact** sur la page révélée, juste après le rouleau : alpha maximal ≈ 0,35, qui décroît sur une demi-portée.
4. **Ombre du bord retourné** posé à plat sur le recto : une rampe d'environ 0,10·W. En double page, elle est presque toujours cachée.
5. **Thème sombre** : une ombre noire se voit à peine sur du papier à 1 % de luminance. Il faut ajouter un **reflet de crête** (+6 à 10 % de luminance sur la nappe haute) et un verso légèrement plus clair que le recto.

#### « Verso visible par transparence »

- **Page simple (portrait)** : il n'y a pas de vraie page au dos, puisque la page suivante apparaît dessous. On fait comme iBooks et *pagecurl* : le dos montre **le recto en miroir, délavé vers la couleur du papier**, `mix(paper, rectoMiroir, 0,15…0,25)`. C'est l'effet du papier bible.
- **Double page (paysage)** : le dos est la **vraie page suivante de gauche**. On peut y ajouter une transparence très légère du recto en miroir, en multiplication à 3–6 %, mais **uniquement sur la feuille qui tourne**, pas au repos (voir les décisions en §4).

#### Shader AGSL (pseudo-code commenté, inspiré de Mekuri, MIT)

```glsl
// Pli cylindrique : une passe par face (pass 0 = recto, 1 = verso, 2 = ombre).
// Coordonnées en px, origine en haut à gauche (convention Canvas d'AGSL).
uniform shader face;          // contenu VIVANT de la face ; le verso est déjà mis en miroir horizontal
uniform float2 size;          // taille du calque : une page, ou toute la double page en paysage
uniform float  axis0;         // abscisse de l'axe à mi-hauteur
uniform float  shear;         // décalage de l'axe par px vertical (> 0 : le coin haut part en premier)
uniform float  radius;        // rayon du cylindre (px)
uniform float  pass;          // 0, 1 ou 2
uniform float  backDim;       // ≈ 0.86
uniform float  showThrough;   // 1 = vrai verso ; 0.15–0.25 = recto en miroir délavé (page simple)
layout(color) uniform half4 paper;

const float PI = 3.14159265;

half4 back(float2 q, float nz) {
    half4 c = face.eval(q);
    half3 rgb = mix(paper.rgb, c.rgb, half(showThrough));      // « par transparence »
    return half4(rgb * half(mix(backDim, 1.0, nz)), 1.0);
}

half4 main(float2 p) {
    float axis = axis0 + shear * (p.y - 0.5 * size.y);
    float flap = size.x - axis;              // longueur de feuille déjà soulevée
    float d    = p.x - axis;                 // distance au pli (> 0 vers le bord libre)
    float halfTurn = PI * radius;

    if (d > radius) {                        // au-delà du rouleau : page du dessous et ombre de contact
        if (pass < 1.5) return half4(0.0);
        float reach = 1.0 - saturate((d - radius) / (0.5 * radius));
        return half4(0.0, 0.0, 0.0, half(0.35 * reach * reach));
    }
    if (d >= 0.0) {                          // sous le rouleau : deux nappes possibles
        float sFront = radius * asin(d / radius);
        float sBack  = halfTurn - sFront;
        float nz     = sqrt(saturate(1.0 - (d / radius) * (d / radius)));
        if (sBack <= flap)                   // la nappe haute existe : on voit le verso
            return pass == 1.0 ? back(float2(axis + sBack, p.y), nz) : half4(0.0);
        if (sFront <= flap && pass == 0.0) { // recto qui s'assombrit vers la crête
            half4 c = face.eval(float2(axis + sFront, p.y));
            return half4(c.rgb * half(mix(backDim, 1.0, nz)), c.a);
        }
        return half4(0.0);
    }
    float sFlat = halfTurn - d;              // partie retournée, posée à plat au-dessus
    if (sFlat <= flap)
        return pass == 1.0 ? back(float2(axis + sFlat, p.y), 0.9) : half4(0.0);
    return pass == 0.0 ? face.eval(p) : half4(0.0);   // zone non soulevée : recto intact et opaque
}
```

Remarques d'implémentation :
- Côté Kotlin, il faut `graphicsLayer { renderEffect = RenderEffect.createRuntimeShaderEffect(shader, "face").asComposeRenderEffect(); compositingStrategy = Offscreen; clip = true }`. Les uniformes se mettent à jour **dans le bloc du calque** et la progression y est lue, jamais pendant la composition. On garde un seul `RuntimeShader` par passe.
- AGSL reprend les fonctionnalités de GLSL ES 1.0. Les boucles doivent donc avoir des bornes constantes et il n'y a pas de `#define` (on utilise `const`). Les uniformes de couleur se déclarent `layout(color)` pour la gestion des couleurs. Le shader **se compile à l'exécution** : une erreur de syntaxe ne se voit que sur l'appareil, d'où un test instrumenté de capture d'écran (Mekuri en a un).
- La distance *d* est mesurée horizontalement et non perpendiculairement à l'axe incliné. C'est une approximation acceptable tant que le cisaillement reste borné.

### 2.3 Architecture de rendu : calques vivants ou instantanés

| | **Calques vivants + RenderEffect** (Mekuri) | **Instantanés + ShaderBrush** |
|---|---|---|
| Latence du premier pli | Nulle, rien à capturer | Une image de plus, sauf si les voisines sont capturées d'avance |
| Mémoire | Calques hors écran pendant le tour seulement (≈ 10 Mo chacun en 1080×2400) | Environ 10 Mo par page capturée, gardée tant qu'elle est voisine |
| Netteté au repos | Identique au rendu normal, puisque l'effet est retiré au repos | Identique si la capture est en 1:1 |
| Une seule passe pour tout (verso, transparence, texture) | Non : trois passes (recto, verso, ombre) | Oui : un shader avec trois enfants (recto, verso, dessous) |
| Contraintes | Faces figées pendant le tour ; le calque de la feuille doit couvrir la double page | Gérer la fraîcheur de la capture (thème ou police changés…) |

**Recommandation** : les **calques vivants**, approche éprouvée par Mekuri. Les instantanés restent un plan B si un pilote GPU pose problème. Dans les deux cas, **au repos, aucun effet n'est appliqué**.

Ordre des couches, de bas en haut :
1. cadre du livre : fond, tranche, queues de rubans (09), bords d'images pieuses (09) ;
2. page de gauche posée ;
3. page de droite posée ou page révélée ;
4. passe d'ombre (sous la feuille) ;
5. feuille : recto puis verso ;
6. chrome (08).

### 2.4 Gestes

| Geste | Comportement proposé |
|---|---|
| **Tap** | Le quart gauche tourne vers l'arrière, le quart droit vers l'avant, la moitié centrale affiche ou masque le chrome (08). Option « tap partout = page suivante » pour les gauchers ou les lecteurs à une main. Le tour animé dure environ 450 ms. |
| **Glisser** | Il démarre après le *touch slop* de l'appareil (`ViewConfiguration`), si le mouvement est horizontal. Le **bord libre suit le doigt** : Mekuri inverse la géométrie par bissection (`MekuriEdgeTrack`), ce qui fait qu'une petite traction soulève déjà une large arche, comme le papier. Une saisie dans la moitié haute soulève le coin haut, dans la moitié basse le coin bas. |
| **Tirer depuis le coin** | Une saisie à moins d'environ 15 % de la hauteur d'un coin augmente le cisaillement, pour que le coin s'enroule en premier. |
| **Relâcher** | Le tour est **validé** si la progression atteint 35 % du parcours possible, ou si la vitesse dans le sens du tour dépasse ≈ 600 dp/s (un lancer à contresens ne valide jamais). Sinon, **retour élastique**. L'animation de fin dure 0,42 s, avec l'easing (0,35 ; 0,1 ; 0,75 ; 0,85) de Mekuri, ou un ressort critique qui part de la vitesse du doigt, limité à environ 450 ms. |
| **Annulation** | On revient en arrière au-delà du seuil (avec un signal haptique au franchissement), on pose un second doigt, ou le système interrompt (appel, perte de focus) : retour élastique. Un nouveau glisser **reprend** un tour en cours de fin à partir de la progression affichée. |
| **Tour arrière** | C'est la feuille précédente qui se déroule. On réutilise le même shader avec `1 − progression` et les faces échangées. |
| **Geste retour système** | En navigation gestuelle, les bords d'écran déclenchent « Retour » (et l'aperçu du retour prédictif). Il faut réserver les **coins** avec `Modifier.systemGestureExclusion` (API 29+, hauteur limitée à environ 200 dp par bord **(à vérifier)**) et rendre la saisie possible **à l'intérieur** de la page, sans dépendre du bord. **Point à trancher avec 08.** |
| **Autres conflits** | Appui long : sélection et annotation, côté 06 ou 08, désactivées pendant un tour. Traction d'un ruban et déplacement d'une image pieuse (09) : leurs zones de saisie sont prioritaires sur le glisser. Pas de zoom par pincement sur du texte reflowable (06). |
| **Clavier, touches de volume** | Flèches, Page préc./suiv. et espace. Touches de volume en option (V1), désactivées en mode méditation. |
| **TalkBack** | Le livre est un seul élément qui expose « Page 12 sur 240 » et deux actions personnalisées, **« Page suivante » et « Page précédente », en français**, plus le défilement d'accessibilité. Le changement de page est annoncé. |

Pseudo-code :

```kotlin
Modifier.pointerInput(book) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        val drag = awaitHorizontalTouchSlopOrCancellation(down.id) { c, _ -> c.consume() }
        if (drag == null) { onTap(zoneOf(down.position)); return@awaitEachGesture }
        val dir = if (drag.position.x < down.position.x) Forward else Backward
        if (!state.canTurn(dir)) return@awaitEachGesture
        val turn = state.beginTurn(dir, grabY = down.position.y)   // fige les faces, choisit le coin
        haptic(Lift)
        val vt = VelocityTracker(); var armed = false
        horizontalDrag(drag.id) { c ->
            vt.addPosition(c.uptimeMillis, c.position)
            turn.progress = edgeTrack.progressFor(c.position)       // le bord libre est sous le doigt
            (turn.progress >= SNAP).let { if (it != armed) { armed = it; haptic(Threshold) } }
            c.consume()
        }
        val v = vt.calculateVelocity().x * dir.sign                 // px/s → dp/s dans le sens du tour
        val commit = turn.progress >= SNAP || v.toDp() >= FLING
        scope.launch { turn.settle(commit, v); if (commit) haptic(Land) }
    }
}
```

### 2.5 Retour haptique et son

- **Haptique** (via la View, pour respecter le réglage système de retour tactile) :

  | Signal | API 34+ | API 30–33 | Plus ancien |
  |---|---|---|---|
  | Soulèvement | `GESTURE_START` | `GESTURE_START` | rien |
  | Seuil franchi, dans un sens ou dans l'autre | `GESTURE_THRESHOLD_ACTIVATE` (ou `SEGMENT_TICK`) | `CLOCK_TICK` | `CLOCK_TICK` |
  | Atterrissage | `GESTURE_END` ou `CONFIRM` léger | `GESTURE_END` | `VIRTUAL_KEY` |

  Compose expose les mêmes types dans `HapticFeedbackType` : `GestureThresholdActivate`, `SegmentTick`, `GestureEnd`, `Confirm`… (référence composables.com mise à jour le 7 octobre 2026 ; versions d'apparition **à vérifier**). Les tours lancés par programme (synthèse vocale, navigation) restent **muets**. Les vibreurs bas de gamme sont souvent bruyants : il faut un interrupteur. Plus tard : une « friction de papier » faite de `PRIMITIVE_LOW_TICK` espacés pendant le glisser (`VibrationEffect.Composition`).
- **Son de papier** (optionnel, **désactivé par défaut**) :
  - `SoundPool`, attributs `USAGE_ASSISTANCE_SONIFICATION` ;
  - 2 ou 3 variantes Opus de 100 à 300 ms (≈ 10–20 Ko chacune), hauteur aléatoire ±3 % et volume entre 0,3 et 0,5 ;
  - déclenché au soulèvement ou à l'atterrissage, jamais à chaque image ;
  - coupé si la sonnerie est en mode silencieux ou vibreur, si « Ne pas déranger » est actif ou en **mode méditation**.

  Source : un enregistrement maison, ou du CC0 vérifié par 12.

### 2.6 Budget de performance, minSdk et replis

**Distribution** (apilevels.com, mis à jour le 28 mai 2026, données StatCounter d'avril 2026, en cumulé) : API 36+ 22,3 % · 35+ 41,0 % · 34+ 54,5 % · **33+ 68,9 %** · 31+ 78,8 % · 30+ 86,9 % · 29+ 91,1 % · 26+ 96,1 % · 23+ 98,0 %. Google Play exige `targetSdk 36` depuis le 31 août 2026.

**Budget par image pendant un tour**

| Poste | 60 Hz (16,7 ms) | 120 Hz (8,3 ms) |
|---|---|---|
| Fil UI : geste, uniformes, lecture de l'état, **zéro recomposition de page** | ≤ 3 ms | ≤ 2 ms |
| RenderThread et GPU : 2 faces hors écran + 3 passes de shader | ≤ 8 ms | ≤ 5 ms |
| Mémoire supplémentaire (calques de 1080×2400×4 o ≈ 10,4 Mo × 3) | ≈ 31 Mo, libérés à l'atterrissage | idem |
| Latence entre le *touch slop* et la première image pliée | ≤ 1 image | ≤ 1 image |
| Objectif Macrobenchmark `FrameTimingMetric` | P95 < 14 ms | P95 < 8,3 ms, moins de 1 % d'images en retard |

Ordre de grandeur : 3 passes sur 2,6 Mpx font 7,8 Mpx ombrés par image, soit ≈ 0,9 Gpx/s à 120 Hz. C'est faisable sur un GPU de milieu de gamme 2024–2026, mais ce n'est pas négligeable. Pour optimiser, on **limite les passes verso et ombre au rectangle englobant de la partie soulevée** (rabat plus *R*), puis on mesure avant de complexifier. Mekuri annonce environ 1 ms sur le fil UI et +1,5 à 3 ms sur le RenderThread, **sur émulateur seulement**.

- **Pré-requis de 06** : les pages i ± 1 (i ± 2 en double page) sont déjà composées et mises en page avant le début d'un tour. Pendant un tour, aucune mesure de texte n'est permise.
- **Taux de rafraîchissement** : `Modifier.preferredFrameRate(FrameRateCategory.High)` (Compose 1.9 ; la doc emploie aussi `requestedFrameRate`, nom exact **à vérifier**) pendant le tour, sur les appareils à taux adaptatif (Android 15 QPR1+). Sinon, le *touch boost* du système suffit.
- **Mesure** : `adb shell dumpsys gfxinfo <pkg> framestats`, Perfetto, JankStats en surimpression dans le prototype, Macrobenchmark sur **le téléphone physique**. Ne jamais lancer d'émulateur sur la VM (2 cœurs, 10 Go).

**Replis selon le niveau d'API (MVP)**

| API | Portrait | Paysage (double page) |
|---|---|---|
| 33+ | Pli AGSL « Papier » | Feuille AGSL recto-verso |
| 26–32 | **Feuille qui glisse** : translation, ombre en dégradé et dessous fixe | Rotation 3D rigide autour de la reliure (`rotationY`), ou glissement |
| Toutes | Fondu (150 ms) ou coupe nette | idem |

Pour 07, un `minSdk` de 26 convient (repli assuré). C'est à 10 de décider, sachant que Compose en 2026 pourrait exiger au moins 23 **(à vérifier)**.

### 2.7 « Réduire les animations »

- Android n'expose pas directement le réglage d'accessibilité « Supprimer les animations ». Il fixe `Settings.Global.ANIMATOR_DURATION_SCALE` à 0, ce qui sert d'indicateur. Les animations Compose suivent ce facteur par `MotionDurationScale`.
- **À faire** :
  - lire la valeur **et l'observer** (`ContentObserver` sur `Settings.Global.getUriFor(ANIMATOR_DURATION_SCALE)`, relu à `onResume`). Mekuri ne la lit qu'une fois par Context ;
  - réglage dans l'application : **Tourne-page : Papier / Glissement / Fondu / Aucun**, plus « Suivre le réglage du système » ;
  - quand la réduction est active, le glisser reste possible (seuil, puis changement de page) mais **sans pli** : coupe nette ou fondu de 120 à 150 ms (le fondu ne gêne pas le système vestibulaire) ;
  - en option, économiseur de batterie (`PowerManager.isPowerSaveMode`) : passer en Glissement ;
  - liseuses à encre électronique (Android Boox…) : mode « Aucun » automatique, plus tard.

### 2.8 Double page en paysage

- **Quand afficher une double page** : par défaut, seulement si chaque page fait **au moins ≈ 300 dp de large** et si la double page occupe au moins 60 % de la hauteur (Mekuri : 270 pt et 0,6). Sur un téléphone en paysage (≈ 360–410 dp de haut), une page n'affiche qu'**environ 12 à 14 lignes** à 18 sp. C'est lisible mais court, d'où une **décision à prendre** (§4). Sur tablette et pliable déplié, c'est le mode naturel.
- **La feuille** : elle prend toute la largeur de la double page. Son recto est la page de droite courante, placée dans la moitié droite ; son verso est la page de gauche suivante, mise en miroir dans sa moitié. Le pli balaie du bord extérieur jusqu'à la reliure, au centre. La page de droite suivante est révélée dessous et reste **vivante**.
- **Appariement** : la couverture est seule à droite, puis les pages vont par paires. Les pages blanches de calage (un chapitre qui commence sur une page de droite, comme dans un vrai missel) relèvent de 06 et sont à trancher (§4).
- **Ombre de la reliure** : elle s'applique dans le shader papier ou en dégradé, en multiplication au repos :
  ```
  g = |x − xReliure| / largeurGouttière      (largeurGouttière ≈ 4 % de la largeur d'une page)
  teinte = 1 − 0.12·exp(−3g) + 0.012·exp(−8(g − 1.3)²)   // creux puis léger reflet
  ```
  La **marge intérieure doit être au moins égale à la largeur de la gouttière**, pour que le texte ne tombe jamais dans le creux (contrainte pour 06). On ne déforme pas le texte près de la reliure : c'est illisible et coûteux.
- **Épaisseur de la tranche** :
  - en paysage, des bandes aux bords extérieurs : à gauche `t = Tmax·progression`, à droite `t = Tmax·(1 − progression)`, avec `Tmax ≈ 6 dp` pour un gros livre et 3 dp pour un petit. En portrait, une bande à droite seulement (`Tmax ≈ 3 dp`) ;
  - le dessin est procédural : des filets de 0,75 dp alternant le ton du papier et un ton 6 à 8 % plus sombre, avec un léger dégradé extérieur ;
  - option **tranche rouge ou dorée**, clin d'œil aux missels (or : dégradé chaud et reflet fin, très beau en thème sombre) ;
  - la progression vient de 06 (fraction des caractères, voir §2.11). Les **rubans** et **images pieuses** (09) dépassent de la tranche à leur position relative : la tranche sert aussi d'index visuel. Plus tard : feuilleter en faisant défiler le doigt sur la tranche (`SEGMENT_FREQUENT_TICK` à chaque page), onglets façon répertoire.
- **Pliables** : en posture livre, la reliure se place sur la charnière (`FoldingFeature` de WindowManager), à voir avec 08 et 10.

### 2.9 Le papier : texture, thèmes, couleurs et contraste

**Texture procédurale** : bruit de valeur fBm sur 4 octaves, plus des « fibres » étirées horizontalement. Amplitude : ±1,5 % (clair), ±1,0 % (sépia), ±0,6 % (sombre, ou désactivée). La texture est **statique et liée à la feuille**, si bien qu'elle s'enroule avec la page, sans coût supplémentaire puisqu'elle fait partie du contenu de la face. La version AGSL pour l'API 33+, en option :

```glsl
uniform float2 origin;  uniform float grain;  uniform float amp;    // grain ≈ 1,2 mm en px
layout(color) uniform half4 paper;
float hash12(float2 p) {                       // « hash without sine » : stable en mediump
    float3 p3 = fract(float3(p.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}
float vnoise(float2 p) {
    float2 i = floor(p);
    float2 f = fract(p);
    float2 u = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash12(i), hash12(i + float2(1, 0)), u.x),
               mix(hash12(i + float2(0, 1)), hash12(i + float2(1, 1)), u.x), u.y);
}
half4 main(float2 xy) {
    float2 q = (xy - origin) / grain;
    float n = 0.0; float a = 0.5;
    for (int k = 0; k < 4; k++) { n += a * vnoise(q); q = q * 2.07 + 17.0; a *= 0.5; }
    float fibres = vnoise(float2((xy.x - origin.x) / (grain * 8.0), (xy.y - origin.y) / (grain * 0.4)));
    float v = (n - 0.47) + 0.5 * (fibres - 0.5);
    return half4(paper.rgb * half(1.0 + amp * v), paper.a);
}
```

**Pour toutes les API (recommandé au MVP)** : on génère **une seule fois** la même fonction en Kotlin, dans une tuile périodique de 512×512 (le hachage se calcule modulo la période, pour que les raccords ne se voient pas). On la teinte selon le thème en arrière-plan (environ 20 à 50 ms), puis on la peint avec un `BitmapShader(REPEAT)`. L'APK ne contient aucune image et le rendu est identique d'un appareil à l'autre. On ajoute un vignettage de 2 à 3 % vers les bords de la page.

**Palettes proposées** (rapports de contraste WCAG calculés ; « texturé » signifie le papier assombri de 3 %, ou éclairci de 6 % en sombre) :

| Thème | Papier | Encre | Rubrique (rouge) | Gris secondaire | Encre / papier | Rouge / papier (texturé) |
|---|---|---|---|---|---|---|
| **Clair (ivoire)** | `#FBF8F1` | `#1E1A16` | `#A3201A` | `#5E554B` | 16,3:1 | 7,1:1 (6,7:1) |
| **Sépia** | `#F2E8D5` | `#33291F` | `#93261A` | `#5F5141` | 11,7:1 | 6,8:1 (6,4:1) |
| **Sombre** | `#1C1A17` | `#E6DFD2` | `#E8897A` | `#A89F92` | 13,1:1 | 6,9:1 (6,7:1) |
| **Nuit (OLED)** | `#000000` | `#CFC8BB` | `#D9786A` | `#8E877C` | 12,6:1 | 6,9:1 |

- Tous les couples dépassent **AA (4,5:1)**, et l'encre dépasse AAA (7:1). Le rouge reste au-dessus de 6:1, ce qui laisse de la marge aux rubriques en italique et en petit corps.
- À éviter : `#C0392B` sur sépia ne donne que 4,47:1.
- En thème sombre, l'encre n'est pas du blanc pur, pour éviter le halo, et le rouge vire au corail désaturé.
- Le rouge ne doit **pas être le seul porteur de sens** (WCAG 1.4.1) : les rubriques sont aussi en italique ou en corps plus petit.
- Ces jetons (`paper`, `ink`, `rubric`, `muted`, `gutterShade`, `edgeTone`, `textureAmp`) sont proposés à 08, qui les intègre au thème global. Les couleurs liturgiques des rubans relèvent de 09 et 05.

### 2.10 Typographie

#### Couverture des glyphes (analyse directe des TTF, google/fonts au 9 octobre 2026 ; Junicode d'après ses sources Glyphs)

| Police (OFL) | ℣ | ℟ | ✠ | ✝ | æ œ ǽ Ǽ | á é í ó ú ý ë | ❦ ❧ ☙ | ⁂ | U+202F | `smcp` | `c2sc` | `onum` | Poids |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| **EB Garamond** (variable 400–800) | ✔ | ✔ | — | — | ✔ | ✔ | ✔ ✔ ✔ | — | **—** | ✔ | ✔ | ✔ | 831 Ko |
| **Junicode 2** v2.226 (variable) | ✔ (+ pt. cap.) | ✔ (+ pt. cap.) | ✔ | ✔ | ✔ | ✔ | ✔ ✔ ✔ | ✔ | ✔ | ✔ | ✔ | ✔ | zip de 59 Mo (à sous-ensembler) |
| Cardo | ✔ | ✔ | — | ✔ | ✔ | ✔ | ✔ ✔ — | ✔ | ✔ | ✔ | ✔ | — | 391 Ko |
| Noto Serif | ✔ | ✔ | — | — | ✔ | ✔ | — | ✔ | ✔ | ✔ | ✔ | ✔ | 1,8 Mo |
| Gentium Book Plus (**RFN « Gentium », « SIL »**) | ✔ | ✔ | — | — | ✔ | ✔ | — | — | ✔ | ✔ | ✔ | ✔ | 799 Ko |
| Cormorant / Cormorant Garamond | — | — | — | — | ✔ | ✔ | ✔ — — | ✔ | ✔ | ✔ | ✔ | ✔ | 559 Ko / 1,1 Mo |
| Alegreya (+ Alegreya SC) | — | — | — | — | ✔ | ✔ | — | — | — | ✔ | ✔ | ✔ | 415 Ko |
| Crimson Pro | — | — | — | — | ✔ | ✔ | — ✔ ✔ | — | — | — | — | — | 240 Ko |
| Literata (conçue pour la lecture sur écran) | — | — | — | — | ✔ | ✔ | — | ✔ | — | ✔ | ✔ | ✔ | 932 Ko |
| Libertinus Serif (version Google Fonts) | — | — | — | ✔ | ✔ | ✔ | — ✔ ✔ | ✔ | ✔ | **pas de table GSUB** | — | — | 589 Ko |
| Vollkorn | — | — | — | — | ✔ | ✔ | — | — | ✔ | ✔ | ✔ | ✔ | 564 Ko |

Constats :
- **Aucune police de Google Fonts n'a ✠ (U+2720)**. Seul Junicode 2 réunit ✠ ✝ ℣ ℟ ⁂ et U+202F, avec en plus des ℣ et ℟ en petites capitales.
- **EB Garamond n'a pas U+202F**, l'espace fine insécable de la typographie française, avant `; : ! ?` et à l'intérieur des « ». Elle a en revanche U+2009 (espace fine) et U+2060 (gluon). Trois parades possibles :
  - a) remplacer U+202F par la séquence U+2060 U+2009 U+2060 lors de la préparation (11) ;
  - b) ajouter U+202F à la table `cmap` d'EB Garamond (glyphe = espace fine), ce qui est permis par l'OFL, la version Google Fonts n'ayant pas de nom réservé ;
  - c) une police de repli.

  Je préfère **b) dans le pipeline (11)**, avec a) en solution de secours.
- La version **Google Fonts de Libertinus n'a pas de table GSUB** : ni ligatures ni petites capitales. Elle est écartée.
- **Cormorant** est une police de titrage, trop fine pour le texte courant sur un téléphone. **Alegreya** et **Literata** sont très lisibles à l'écran mais n'ont pas ℣ ℟. Elles restent possibles comme « police alternative » si l'on prévoit un repli pour ces glyphes.

#### Recommandation

- **Texte courant : EB Garamond**. C'est le Garamond, la voix même du livre français de la Renaissance. Son œil (hauteur d'x) est petit : il faut un **corps par défaut plus grand** (≈ 19–21 sp, interligne de 1,4 à 1,5) et une justesse **à tester aux échelles de police 130 % et 200 %** (mise à l'échelle non linéaire d'Android 14+).
- **Symboles et repli : un sous-ensemble de Junicode 2** (✠ ✝ ⁂ U+202F, et éventuellement ℣ ℟ en petites capitales), de l'ordre de quelques dizaines de Ko. Deux mécanismes possibles :
  - (1) **segments `SpanStyle(fontFamily = junicodeSymbols)`** posés par 11 ou 06 sur ces caractères. Cela fonctionne sur toutes les API, et c'est ma recommandation ;
  - (2) `Typeface.CustomFallbackBuilder` (API 29+), chaîne EB Garamond → Junicode → système.
- **Petites capitales** : `fontFeatureSettings = "smcp"` (et `"c2sc"` pour les sigles). On les emploie pour les mots qui suivent une lettrine, les noms de fêtes et les intitulés comme « ORAISON ».
- **Chiffres elzéviriens** (`onum`) dans le texte courant, numéros de versets en exposant ou en rouge (06).
- **Fleurons** : ❦ ❧ ☙ d'EB Garamond, ⁂ de Junicode. Les ornements de section plus riches sont des `ImageVector` (SVG), à colorer selon le thème.
- **Lettrines** (spécification pour 06, qui les met en page) :
  - MVP : initiale EB Garamond **rouge sur 2 ou 3 lignes**. Le haut de l'initiale s'aligne sur la hauteur de capitale de la ligne 1, le bas sur la ligne de base de la ligne N, et le reste du premier mot passe en petites capitales. On **retire l'accent de la lettrine** (*Ádjutórium* → A), comme dans l'usage typographique. La mise en œuvre passe par `LeadingMarginSpan2` ou une mise en page à N lignes en retrait (06).
  - V1 : **lettrines ornées** avec `EBGaramond-Initials.otf` (G. Duffner, OFL, paquet CTAN ebgaramond du 23 avril 2024), dessinées d'après **une Bible française du XVIᵉ siècle**. Les variantes bicolores InitialsF1 (ornement) et F2 (lettre) permettent une lettre rouge sur un ornement noir ou or.
- **Rubriques** : couleur `rubric`, italique, corps de 0,85 à 0,9 em. ℣ et ℟ en rouge.
- **Taille de l'APK** : EB Garamond romain et italique (≈ 1,7 Mo) et le sous-ensemble Junicode (moins de 100 Ko). Les polices sont embarquées (pas de *Downloadable Fonts*, qui irait contre le principe hors ligne et le respect de la vie privée), avec le texte OFL dans « Licences » (12).

#### Césure (vérifiée dans le code source d'AOSP)

- **Ce qu'Android embarque** (`frameworks/base/core/jni/android_text_Hyphenator.cpp`, branche main) :
  - `addHyphenator("fr", 2, 3)` : français, avec au moins 2 lettres avant la coupure et 3 après ;
  - `addHyphenator("la", 2, 2)` : latin.
- **Quels motifs latins** (`platform/external/hyphenation-patterns`, `TeX/la/`) :
  - Android compile `hyph-la.pat.txt`, « latin moderne et médiéval » de **Claudio Beccari, v3.201 du 28 août 2016** (MIT) ;
  - les motifs **`hyph-la-x-liturgic`** (latin liturgique, v1.040 du 19 mai 2016, même auteur) sont **présents dans le dépôt mais non compilés**. Le commentaire dans `Android.bp` dit : *« we have data for Liturgical Latin, but there is no standard BCP 47 tag for it, so we don't build it »*.
- **Le piège des accents** :
  - l'alphabet de `hyph-la` (`hyph-la.chr.txt`) ne contient **que les lettres sans accent, plus æ et œ** ;
  - minikin (`Hyphenator.cpp`, `alphabetLookup`) renvoie `DONT_BREAK` pour **tout le mot** dès qu'un caractère sort de l'alphabet, sans normalisation NFC (`// TODO: try NFC normalization`) ;
  - conséquence : *Dóminus*, *glória*, *sǽcula*, *Spíritu* ne sont **jamais coupés**, alors que les textes du missel de 1962 accentuent systématiquement les proparoxytons. L'alphabet liturgique, lui, contient `á é í ó ú ý ë ǽ` et l'accent aigu combinant U+0301.
- **Tirets conditionnels** :
  - dans minikin, un mot qui contient un U+00AD ne passe pas par les motifs : il ne se coupe **qu'aux tirets conditionnels** (`hyphenateWithNoPatterns`) ;
  - ils ne sont pris en compte que si la césure est activée : avec `HyphenationFrequency::None`, aucune coupure n'a lieu ;
  - en texte justifié, la pénalité de césure est divisée par 4 (`OptimalLineBreaker.cpp`) : la césure y est plus agressive.
- **Côté Compose** (`AndroidParagraph.android.kt`, branche androidx-main) :
  - `Hyphens.Auto` donne `HYPHENATION_FREQUENCY_FULL` jusqu'à l'API 32 et `FULL_FAST` à partir de l'API 33 ;
  - `TextAlign.Justify` donne `JUSTIFICATION_MODE_INTER_WORD` ;
  - `LineBreak.Paragraph` active la stratégie haute qualité ;
  - la langue passe par `SpanStyle(localeList = LocaleList("la"))` ou `"fr"`, segment par segment.
- **Recommandation** :
  1. **Français** : les motifs système (`Hyphens.Auto` et la locale `fr`).
  2. **Latin** : **insertion de U+00AD au moment de la préparation (11)** avec les motifs `la-x-liturgic` (algorithme de Liang, environ 100 lignes, ou une bibliothèque qui gère cette variante, **à évaluer par 11**). Le comportement est alors identique sur toutes les versions d'Android, et l'on garde `Hyphens.Auto`.
  3. **Retirer les U+00AD** pour la recherche plein texte, la synthèse vocale et le presse-papiers (06, 11 et 14).
  4. Limites connues : Android ne sait pas limiter le nombre de lignes consécutives coupées (la règle française demande au plus trois), et les colonnes latin et français étroites en portrait créent des « rivières » quand elles sont justifiées (décision en §4).
  5. Texte en NFC obligatoire (11).

### 2.11 Points d'interface

**Avec 06 (moteur de lecture), le contrat principal**

```kotlin
/** Fourni par 06. */
interface PagedBook {
    val pageCount: StateFlow<PageCount>                 // Exact(n) ou Estimated(n) pendant la pagination
    fun progressAt(page: Int): Float                    // 0..1 (offset de caractères) → épaisseur de la tranche
    fun spreadOf(page: Int, mode: SpreadMode): Spread   // appariement recto-verso, pages blanches comprises
    suspend fun prepare(pages: IntRange)                // mise en page anticipée des voisines, hors fil UI
    @Composable fun Page(index: Int, mode: PageMode)    // ENCRE seulement, fond transparent
}
enum class PageMode { Settled, Turning }               // en Turning : rendu figé, sans animation ni chargement

/** Fourni par 07. */
@Composable fun PaperBook(
    book: PagedBook, state: PaperBookState,             // currentPage, animateToPage, snapToPage
    turnStyle: TurnStyle,                               // Papier | Glissement | Fondu | Aucun | Auto
    spread: SpreadPolicy, theme: PaperTheme,
    haptics: Boolean, sound: Boolean,
    edgeDecorations: @Composable BookEdgeScope.() -> Unit = {},   // emplacement pour 09 (rubans, images)
    onCenterTap: () -> Unit,                            // pour 08
)
```

- **07 dessine le papier** (fond, texture, gouttière, tranche) et **06 dessine l'encre** par-dessus. Le pli s'applique ensuite à l'ensemble.
- Ce dont 07 a besoin de 06 :
  - des pages **composées d'avance** ;
  - une page **figée en mode `Turning`** ;
  - la **largeur de page en px** et les **marges intérieures**, au moins égales à la gouttière ;
  - la **progression** de lecture ;
  - les **événements** `onPageSettled` (relayés à 08 et 09).
- La sélection et les annotations ne sont actives que sur une page posée.
- Le corps par défaut, l'interligne, les règles de veuves et orphelines et la mise en page des lettrines relèvent de 06. 07 fournit la spécification visuelle (§2.10).

**Avec les autres agents**

- **08 (UX)** :
  - zone centrale et chrome, réglages Tourne-page, Thème et Police ;
  - exclusion du geste retour aux coins et interaction avec le retour prédictif ;
  - seuil d'affichage de la double page ;
  - jetons de couleur.
- **09 (signets et images)** :
  - emplacement `edgeDecorations` (queues de rubans, bords d'images dans la tranche) ;
  - priorité de leurs gestes sur le glisser ;
  - règle de pile : un objet « posé sur la page N » fait partie du contenu de la face et s'enroule avec elle ; sinon il appartient au cadre.
- **10 (architecture)** :
  - modules `:core:paper` (thèmes, texture, polices) et `:feature:pageturn` (gestes, géométrie, rendu) ;
  - minSdk ;
  - tests instrumentés de capture d'écran sur l'appareil ;
  - construction des APK de prototype en CI.
- **11 (pipeline)** :
  - texte en NFC ;
  - U+00AD pour le latin ;
  - correctif U+202F d'EB Garamond ;
  - sous-ensemble de Junicode ;
  - segments de police pour ✠ ✝ ⁂.
- **12 (juridique)** :
  - OFL pour EB Garamond, Junicode et éventuellement Cardo ;
  - noms réservés de Gentium ou Lora si l'on en modifie une ;
  - MIT pour Mekuri ;
  - Apache-2.0 (NOTICE) pour pagecurl ou PageFlip s'ils sont repris ;
  - son de papier ;
  - motifs de césure (MIT).
- **05 (calendrier) et 14 (prières)** : aucune dépendance directe. Le tour automatique lancé par la synthèse vocale (14) doit être lent (≈ 700 ms) ou en fondu, et silencieux.

---

## 3. Recommandations

### MVP

1. **Pli « Papier » en AGSL (API 33+)** sur les calques vivants, avec trois passes (recto, verso, ombre) et un modèle de cylindre à axe quasi vertical et cisaillement borné. **Code repris et adapté de Mekuri** (MIT, avec la notice), en gardant ses constantes comme valeurs de départ et sa séparation entre une **géométrie Kotlin pure, testée en JVM**, et un rendu propre à la plateforme.
2. **Repli « Feuille qui glisse »** (translation et ombre, sur toutes les API) sous l'API 33. Pour la double page sous l'API 33 : rotation 3D rigide. Options **Papier / Glissement / Fondu / Aucun**, plus « Suivre le système », avec observation d'`ANIMATOR_DURATION_SCALE`.
3. **Gestes** comme au §2.4 : zones de tap 25 / 50 / 25 %, bord libre qui suit le doigt, seuil de 35 % ou 600 dp/s, retour élastique, reprise d'un tour en cours, exclusion du geste système aux coins, actions TalkBack en français, clavier.
4. **Haptique** sur trois signaux, désactivable. Les tours programmés restent muets.
5. **Papier** : tuile de bruit procédural (sans aucune image), thèmes Clair, Sépia, Sombre et Nuit avec les jetons et contrastes du §2.9, et un **test unitaire qui vérifie tous les ratios** à partir des jetons.
6. **Double page** dès que les dimensions le permettent : feuille recto-verso, ombre de reliure, **tranche simple** liée à la progression.
7. **Typographie** :
   - EB Garamond et un sous-ensemble Junicode (✠ ✝ ⁂ U+202F) ;
   - petites capitales, chiffres elzéviriens, rubriques rouges, lettrines simples rouges ;
   - `Hyphens.Auto` et justification ;
   - latin pré-coupé par U+00AD (motifs liturgiques, fournis par 11).
8. **Budget de performance** inscrit dans les critères d'acceptation et mesuré sur le téléphone physique. Calques hors écran seulement pendant un tour.

### V1

- **Pli de repli pour l'API 26 à 32** : pli plat dérivé de *pagecurl* (Apache-2.0) ou cylindre `drawBitmapMesh`, **si** la part d'utilisateurs concernés le justifie. Il faudra des statistiques locales et anonymes, ou des retours d'utilisateurs, ce qui reste à concilier avec le principe « pas de pistage ».
- **Son de papier** optionnel (2 ou 3 variantes, désactivé par défaut, coupé en mode silencieux, en « Ne pas déranger » et en méditation).
- **Lettrines ornées bicolores** (EBGaramond-Initials F1 et F2).
- **Tranche rouge ou dorée**, rubans et images visibles dans la tranche (avec 09), **feuilleter par la tranche**.
- `preferredFrameRate(High)` pendant un tour, et passes verso et ombre limitées au rectangle englobant de la partie soulevée.
- Choix entre 2 ou 3 polices : EB Garamond, plus une police « grande lisibilité » (Alegreya ou Literata) avec repli Junicode pour ℣ ℟ ✠.
- Touches de volume pour tourner les pages, et tour lent ou fondu pour la synthèse vocale (14).

### Plus tard

- Transparence « papier bible » **au repos** (verso en miroir à 2–4 %), en option et désactivée par défaut à cause de la lisibilité.
- Onglets (index à encoches) sur la tranche ; « friction » haptique pendant le glisser.
- Mode encre électronique ; alignement de la reliure sur la charnière des pliables (si 08 et 10 ne le font pas dès la V1).
- Césure latine à l'exécution (Liang en Kotlin) pour les textes ajoutés par l'utilisateur.
- Version instantanés + `ShaderBrush` en une passe (verso, transparence et texture), si les mesures montrent que les trois passes coûtent trop.

### Plan de prototype en 3 étapes (testable sur téléphone physique)

> Le prototype est une application de démonstration isolée (`proto-tourne-page`), séparée de l'application. L'APK de débogage est construit par **GitHub Actions** puis installé avec `adb install` en Wi-Fi sur le téléphone. **Aucun émulateur ni Gradle en parallèle sur la VM.**

| Étape | Contenu | Critères de réussite | Durée estimée |
|---|---|---|---|
| **① Une feuille qui tourne** (portrait) | 30 pages factices (EB Garamond, rubriques rouges, une lettrine) ; pli AGSL (3 passes) ; zones de tap, glisser, lancer, annulation, reprise ; haptique ; bascule Papier / Glissement / Fondu / Aucun ; « forcer le repli » pour simuler l'API < 33 ; « réduire les animations » ; surimpression JankStats ; trois réglages de rayon et de durée à comparer | P95 du temps d'image < 8,3 ms à 120 Hz (`gfxinfo framestats`) ; **aucune recomposition de page par image** (Layout Inspector) ; aucun saut d'un pixel entre le repos et le début du pli ; ressenti validé par l'utilisateur sur ses réglages préférés | 4 à 6 jours |
| **② Le papier et l'encre** | Thèmes Clair, Sépia, Sombre et Nuit ; texture en tuile CPU **et** en AGSL (A/B) ; sélecteur de police (EB Garamond, Junicode, Cardo, Alegreya) ; ℣ ℟ ✠ et U+202F ; petites capitales ; lettrine simple ou ornée ; **césure latine en A/B/C** : Android `la`, tirets conditionnels liturgiques, aucune ; sur l'Ordinaire de la messe en colonnes latin et français, justifié ou en drapeau | Test unitaire des contrastes au vert ; aucun carré vide ni police de repli disparate ; 50 mots latins accentués de contrôle coupés correctement ; lisibilité jugée à 100, 130 et 200 % | 3 à 5 jours |
| **③ Le livre ouvert** (paysage) | Double page, feuille recto-verso pleine largeur, ombre de reliure, tranche (progression factice) et emplacements factices de rubans et d'images (pour 09) ; exclusion du geste système aux coins ; son optionnel ; rotation portrait ↔ paysage | P95 < 8,3 ms à 120 Hz sur le téléphone de test (et < 16,6 ms sur un appareil API 33 d'entrée de gamme si possible) ; la feuille ne traverse pas la reliure ; la position est conservée à la rotation ; aucun déclenchement involontaire du retour système | 4 à 6 jours |

---

## 4. Décisions à soumettre à l'utilisateur

1. **Repli sous l'API 33 (≈ 31 % des appareils)** :
   - (a) glissement de feuille seulement (MVP, recommandé pour commencer) ;
   - (b) pli plat *pagecurl* adapté, 2 à 3 jours de plus ;
   - (c) cylindre `drawBitmapMesh`, 4 à 6 jours de plus.
2. **Mekuri** :
   - (a) reprendre et adapter son code (recommandé) ;
   - (b) l'utiliser comme dépendance (minSdk 33 imposé, jeton GitHub Packages, projet d'un mois) ;
   - (c) écrire notre propre code en s'en inspirant seulement.
3. **Animation par défaut** : « Papier » (identité du produit, recommandé) ou « Glissement », la courbure devenant alors une option.
4. **Police du texte courant** : EB Garamond (recommandé), ou Junicode 2 (plus médiévale et plus complète), Cardo, Alegreya ou Literata (plus lisibles à l'écran) ? Et l'utilisateur peut-il en choisir parmi 2 ou 3 ?
5. **Césure du latin** : tirets conditionnels liturgiques à la préparation (recommandé), motifs Android `la` (accents non gérés), ou pas de césure du tout pour le latin.
6. **Justification des colonnes bilingues étroites** en portrait : justifié avec césure (aspect papier) ou en drapeau (moins de rivières) ?
7. **Double page sur téléphone en paysage** : toujours, ou seulement si chaque page fait au moins ≈ 300 dp (recommandé) ?
8. **Pages blanches de calage** en double page (chapitre qui commence à droite, comme dans un vrai livre) : oui ou non ? C'est à décider avec 06.
9. **Son de papier** : le prévoir (désactivé par défaut) ? Source : enregistrement maison ou CC0 ?
10. **Transparence « papier bible » au repos** : jamais (recommandé), en option, ou seulement pendant le tour ?
11. **Tranche** : visible aussi en portrait (fine bande à droite) ? Tranche rouge ou dorée en option ?
12. **Zones de tap** : 25 / 50 / 25 % (recommandé), ou « tap partout = suivant » par défaut ?

---

## 5. Risques et inconnues

| Risque | Impact | Atténuation |
|---|---|---|
| **Performances AGSL non mesurées sur du vrai matériel** : Mekuri n'a mesuré que sur émulateur, et le coût d'une passe `RenderEffect` hors écran sur des GPU Mali ou Adreno d'entrée de gamme reste inconnu | Saccades, chauffe | L'étape ① sert précisément à mesurer ; passes limitées au rectangle soulevé ; repli automatique en Glissement si le temps d'image dépasse le budget (détection par JankStats) |
| **Bogues de pilotes** avec `RuntimeShader` sur certains appareils API 33 (inconnus) | Artefacts ou plantage | Liste de blocage par modèle ; capture d'exception à la compilation du shader avec repli ; test instrumenté de capture d'écran |
| **31 % d'appareils sous l'API 33**, probablement plus parmi un public âgé ou équipé de téléphones anciens | Expérience « papier » diminuée | Repli soigné dès le MVP ; option V1 d'un pli de repli |
| **Mekuri** : projet jeune (zéro étoile, un mois), API susceptible de changer, libellés en anglais, pas d'exclusion du geste système | Dette technique | Reprise du code avec ses tests de parité ; nous maîtrisons le code |
| **Conflits de gestes** : retour système, retour prédictif, sélection, rubans et images (09) | Retours en arrière involontaires, frustration | Exclusion des coins (limitée à environ 200 dp par bord **(à vérifier)**) ; saisie possible à l'intérieur de la page ; priorités claires avec 08 et 09 |
| **Netteté et transition** repos ↔ pli (saut d'un pixel, clignotement) ; faces non figées (images qui se chargent) | Effet « bon marché » | Contrat `PageMode.Turning` avec 06 ; tests de capture d'écran |
| **Polices** : glyphes manquants dans EB Garamond (✠ ✝ ⁂ U+202F), repli disparate, petit œil peu lisible pour les seniors | Lisibilité, aspect | Sous-ensemble Junicode et correctif U+202F ; corps par défaut plus grand ; tests à 200 % |
| **Césure** : tirets conditionnels gênants pour la recherche, la synthèse vocale et le copier-coller ; pas de limite de lignes consécutives coupées ; date d'arrivée du `la` dans Android **(à vérifier)** | Bogues de recherche ou de lecture, typographie imparfaite | Normalisation à l'indexation et à la lecture (06, 11, 14) ; tests sur l'appareil au minSdk |
| **Thème sombre** : ombres invisibles et texture qui « salit » | Pli peu lisible | Reflet de crête, texture à 0,6 % ou désactivée |
| **Double page sur petit téléphone** : 12 à 14 lignes par page | Lecture hachée | Seuil de 300 dp, réglage dans l'application |
| **Retour haptique** de mauvaise qualité sur les vibreurs bas de gamme | Gêne | Signaux légers, interrupteur, respect du réglage système |
| **Recherche web interrompue** (quota partagé atteint pendant la mission) | Quelques détails non revérifiés | Points marqués **(à vérifier)** : minSdk de Compose, limite d'exclusion de gestes, versions d'apparition des types haptiques Compose, nom exact de `preferredFrameRate` |
| **VM limitée** (2 cœurs, 10 Go, sans GPU) | Impossible d'évaluer le rendu localement | CI pour les APK, téléphone physique pour tous les tests de rendu et de performance |

---

## 6. Sources

Toutes consultées ou clonées le **11 octobre 2026**, sauf mention contraire.

**Tourne-page**
- AniCanon, *Mekuri* : https://github.com/AniCanon/mekuri. v0.3.0, commit `b438b03` du 14 septembre 2026, MIT, créé le 9 septembre 2026, 0 étoile. Docs lues : `docs/the-fold.md`, `docs/architecture.md`, `docs/porting.md`, `docs/compose.md`, `docs/accessibility.md` ; sources lues : `MekuriFold.agsl.kt`, `MekuriFoldEffect.kt`, `MekuriHaptic.kt`, `MekuriReducedMotion.kt`, `MekuriTurnDecision.kt`.
- oleksandrbalan, *pagecurl* : https://github.com/oleksandrbalan/pagecurl. v1.5.1, dernier commit du 9 février 2024, Apache-2.0, 457 étoiles, 8 issues ouvertes, minSdk 21, Compose BOM 2024.02.00. Source lue : `CurlDraw.kt`, `PageCurlConfig.kt`.
- Zeyad-37, *pagecurl-cmp* : https://github.com/Zeyad-37/pagecurl-cmp. Créé le 13 août 2026.
- harism, *android-pagecurl* : https://github.com/harism/android-pagecurl. Apache-2.0, HEAD `7a2c8f1` du 6 juin 2012, dernier push le 4 novembre 2017, 1 674 étoiles.
- eschao, *android-PageFlip* : https://github.com/eschao/android-PageFlip. Apache-2.0, OpenGL ES 2.0, commit `6c361e0` du 11 avril 2024, 1 746 étoiles.

**Plateforme Android et Compose**
- Android Developers, *AGSL vs GLSL* : https://developer.android.com/develop/ui/views/graphics/agsl/agsl-vs-glsl
- Android Developers, *Hardware acceleration* (support de `drawBitmapMesh` en API 18, `drawVertices` en API 29, `setShadowLayer` en API 28) : https://developer.android.com/develop/ui/views/graphics/hardware-accel
- Android Developers, *Adaptive refresh rate* (Compose 1.9, `preferredFrameRate`, Android 15 QPR1) : https://developer.android.com/develop/ui/views/animations/adaptive-refresh-rate
- Android Developers, *Gesture navigation* (`setSystemGestureExclusionRects`, Android 10) : https://developer.android.com/develop/ui/views/touch-and-input/gestures/gesturenav
- Références d'API (non relues pendant la mission) :
  - `RuntimeShader` (API 33) : https://developer.android.com/reference/android/graphics/RuntimeShader
  - `HapticFeedbackConstants` : https://developer.android.com/reference/android/view/HapticFeedbackConstants
  - `Typeface.CustomFallbackBuilder` (API 29) : https://developer.android.com/reference/android/graphics/Typeface.CustomFallbackBuilder
- composables.com, *HapticFeedbackType* (contenu mis à jour le 7 octobre 2026) : https://composables.com/docs/androidx.compose.ui/ui/classes/HapticFeedbackType
- *Inclusive Android Apps #8 — The problem of animations* (`ANIMATOR_DURATION_SCALE` comme indicateur) : https://buttondown.com/inclusive-android-apps/archive/inclusive-android-apps-8-the-problem-of-animations/
- apilevels.com (mis à jour le 28 mai 2026, données StatCounter d'avril 2026 ; `targetSdk 36` exigé depuis le 31 août 2026) : https://apilevels.com/
- AndroidX, `AndroidParagraph.android.kt` (branche androidx-main : correspondance de `Hyphens.Auto` vers FULL ou FULL_FAST, `Justify` vers INTER_WORD) : https://github.com/androidx/androidx/blob/androidx-main/compose/ui/ui-text/src/androidMain/kotlin/androidx/compose/ui/text/AndroidParagraph.android.kt

**Césure**
- AOSP `platform/external/hyphenation-patterns`, HEAD `9ee8012` du 8 janvier 2025 : https://android.googlesource.com/platform/external/hyphenation-patterns. Fichiers lus :
  - `TeX/la/Android.bp` (commentaire sur le latin liturgique non compilé) ;
  - `hyph-la.lic.txt` (Beccari v3.201, 28 août 2016, MIT) ;
  - `hyph-la-x-liturgic.lic.txt` (v1.040, 19 mai 2016) ;
  - fichiers `.chr.txt` du latin, du latin liturgique et du français.
- AOSP `platform/frameworks/minikin`, HEAD `05a978c` du 8 mars 2025 : https://android.googlesource.com/platform/frameworks/minikin. Fichiers lus :
  - `libs/minikin/Hyphenator.cpp` (`alphabetLookup`, `hyphenateWithNoPatterns`, TODO NFC) ;
  - `OptimalLineBreaker.cpp` (pénalités, fréquence None).
- AOSP `frameworks/base/core/jni/android_text_Hyphenator.cpp` (branche main : `addHyphenator("la", 2, 2)`, `("fr", 2, 3)`) : https://android.googlesource.com/platform/frameworks/base/+/refs/heads/main/core/jni/android_text_Hyphenator.cpp
- Projet TeX hyphenation (hyph-utf8) : https://github.com/hyphenation/tex-hyphen

**Polices**
- google/fonts, commit `bd8f81d` du 9 octobre 2026 : https://github.com/google/fonts. Familles analysées dans `ofl/` : ebgaramond, cormorant, cormorantgaramond, crimsonpro, alegreya, alegreyasc, literata, cardo, vollkorn, gentiumbookplus, sourceserif4, libertinusserif, libertinusserifdisplay, notoserif, oldstandardtt, newsreader, spectral, imfellenglish, imfelldoublepica, lora. Analyse par un script local (tables `cmap`, `GSUB`, `GPOS`, `maxp`) ; noms réservés lus dans chaque `OFL.txt`.
- Junicode 2, P. S. Baker : https://github.com/psb1558/Junicode-font. Version v2.226 du 20 juin 2026, commit `6978714` du 3 août 2026, OFL. Glyphes vérifiés dans `sources/Junicode.glyphspackage` : `malteseCross` U+2720, `cross` U+271D, `versicle` U+2123, `response` U+211F, `uni2042`, `floralHeart`, `narrownbspace`, `aeacute`, variantes en petites capitales.
- G. Duffner, *EB-Garamond* (README : polices Initials d'après une Bible française du XVIᵉ siècle, F1 = ornement, F2 = lettre) : https://github.com/georgd/EB-Garamond. Commit `d04d0f9` du 18 avril 2023, OFL-1.1.
- CTAN, paquet *ebgaramond*, version du 23 avril 2024 (contient `EBGaramond-Initials.otf`) : https://ctan.org/pkg/ebgaramond et https://mirrors.ctan.org/fonts/ebgaramond/opentype/

**Accessibilité**
- W3C, *WCAG 2.2*, critères 1.4.3 (contraste minimum) et 1.4.1 (utilisation de la couleur) : https://www.w3.org/TR/WCAG22/. Ratios du §2.9 calculés avec la formule de luminance relative du W3C.
