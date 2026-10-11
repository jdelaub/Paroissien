# 08 — UX, architecture de l'information et design system

> Agent 08 · rapport de planification · rédigé le 11 octobre 2026.
> Périmètre : navigation, écrans, langage visuel (couleurs, thèmes, typographie, mouvement), mode méditation « Ne pas déranger », mises en page adaptatives, accessibilité, maquettes.
> Hors périmètre, renvoyés aux agents concernés : moteur de pagination (06), animation de page et texture papier (07), mécanique des signets et images pieuses (09), architecture technique (10), contenus (01 à 05, 11, 14), juridique (12).

---

## 1. Résumé

- **Navigation à quatre entrées** : *Aujourd'hui* (écran de démarrage piloté par le calendrier), *Bibliothèque*, *Signets* et *Recherche*. Les *Réglages* passent par l'icône ⚙ de la barre supérieure. Le lecteur occupe tout l'écran, sans barre de navigation, et son « chrome » n'apparaît qu'au tap.
- **Material 3 sert d'ossature, avec un habillage sur mesure.** Les composants M3 apportent l'accessibilité, les feuilles, les dialogues et les scaffolds adaptatifs. Le reste est propre au projet : palette « papier et encre » sans couleur dynamique, typographie à empattements, formes peu arrondies et mouvements sobres. On n'adopte pas le style « Expressive », jugé trop rebondissant pour la prière. Le BOM 2026.09.00 fournit Material3 **1.4.0**, et les API Expressive et `MotionScheme` publiques n'arrivent qu'avec la **1.5.0**, encore en bêta depuis le 7 octobre 2026.
- **Jetons de couleur** : il y a quatre thèmes, *Clair*, *Sépia*, *Sombre* et *Nuit* (ambre sur noir pour lire au lit). Tous les textes atteignent un contraste d'au moins 4,5:1, calculé. Chaque couleur liturgique se décline en six jetons : « tissu » clair et sombre, accent de texte clair et sombre, conteneur et texte sur conteneur. La liste suit la PGMR n° 346 : blanc, rouge, vert, violet, noir et rose, avec l'or en option.
- **Deux constats d'accessibilité changent la conception.**
  - Le rouge des rubriques ne contraste qu'à environ 2:1 avec le texte noir : seule la teinte les distingue. Il faut donc un second indice, l'italique.
  - Pour un daltonien deutéranope, les rubans **vert et rouge deviennent identiques** (contraste simulé de 1,0:1). Chaque ruban a donc besoin d'une forme d'extrémité ou d'une initiale.
- **Mode méditation** : depuis Android 15, une application ciblant l'API 35 ou plus **ne peut plus modifier le NPD global**. Elle doit fournir une `AutomaticZenRule`, que le système fusionne avec les autres règles. Notre « mode » *Temps de prière* est créé une fois, puis activé et désactivé avec `setAutomaticZenRuleState`. Le système **restaure de lui-même l'état précédent**, et on peut aussi passer en niveaux de gris avec `ZenDeviceEffects` à partir d'Android 15. Le refus de l'autorisation n'empêche jamais la séance.
- **Minuteur et cloche** : en MVP, l'écran reste allumé et très sombre (`Modifier.keepScreenOn()`, Compose UI 1.9 et plus) et la séance se déroule au premier plan. Pour faire sonner la cloche écran éteint, Android 17 (« background audio hardening ») exige un service de premier plan `mediaPlayback` **démarré pendant que l'app est visible**. Sinon le son échoue **en silence**.
- **Mise en page adaptative** : la classe de taille de fenêtre (`currentWindowAdaptiveInfoV2()`, material3-adaptive 1.3.0) et des critères de lisibilité (au moins 38 caractères par ligne et 12 lignes par page) décident entre page simple et double page. En posture « livre », le pli d'un pliable sert de gouttière. Depuis Android 16, et sans dérogation possible en Android 17, les grands écrans ignorent le verrouillage d'orientation.
- **Position de lecture** : elle est ancrée sur une **position dans le contenu**, pas sur un numéro de page. Cette ancre ne change pas lors d'une rotation et n'est mise à jour que lorsque l'utilisateur tourne la page, ce qui évite toute dérive lors des rotations successives.
- **Typographie** : par défaut, **Literata** (OFL, axes optique 7–72 et graisse 200–900, conçue pour la lecture sur écran). L'option « classique » utilise **EB Garamond**, la seule des polices testées qui contient ℣ et ℟, avec les **lettrines EB Garamond Initials** (OFL). Aucune des polices testées ne contient la croix ✠ : il faut prévoir une police de secours.
- **Huit maquettes ASCII** sont fournies (§ 2.13). Dix décisions sont soumises à l'utilisateur (§ 4) : les plus structurantes portent sur la messe du jour « assemblée » ou « livre », la mise en page bilingue du missel et la teinte liturgique de l'interface.

---

## 2. Constats détaillés

### 2.1 État des bibliothèques et des API en octobre 2026

| Élément | Version et état au 11/10/2026 | Conséquence pour le projet |
|---|---|---|
| Compose BOM **2026.09.00** | Compose UI et Foundation **1.12.1**, Material3 **1.4.0** | C'est la base de l'utilisateur. Toutes les API citées ici existent dans ces versions, sauf indication contraire. |
| Material3 **1.5.0-beta01** (7 octobre 2026) | Le `MotionScheme` est sorti de l'expérimental en 1.5.0-alpha15. Le thème Expressive et `expressiveLightColorScheme` ont été promus en alpha18. FloatingToolbar, boutons et menus Expressive ont été stabilisés entre alpha19 et alpha23. | Dans la 1.4.0, les API Expressive ont été **retirées** (1.4.0-beta01). Il faudra donc migrer vers la 1.5.0 stable pour personnaliser le `MotionScheme`. Nos jetons de mouvement doivent rester indépendants de M3. |
| material3-adaptive **1.3.0** (12 août 2026) | `currentWindowAdaptiveInfo()` est dépréciée au profit de `currentWindowAdaptiveInfoV2()`. Le paramètre `supportLargeAndXLargeWidth` ajoute les classes Large et XL. Le module `adaptive-navigation3` est nouveau. Les API de posture ont été successivement rendues expérimentales puis internes (`calculatePosture()` est cachée). | Il faut utiliser la V2. Pour les plis, on lit `FoldingFeature` via WindowManager plutôt que des API de posture instables. |
| androidx.window **1.5.1** (stable) | Ajoute les points de rupture Large (1200 dp) et Extra-large (1600 dp). | — |
| Navigation 3 | 1.0 stable depuis novembre 2025, ligne 1.1.x en 2026 | C'est le choix de l'agent 10, mais il conditionne le retour arrière et les scènes adaptatives. |
| `Modifier.keepScreenOn()` | Dans `androidx.compose.ui:ui` depuis la **1.9.0** | Remplace `FLAG_KEEP_SCREEN_ON`. L'effet est limité à la durée de vie du composable. |
| Android 16 (API 36) | **Edge-to-edge obligatoire** (la dérogation n'existe plus). **Retour prédictif** activé par défaut : `onBackPressed` n'est plus appelé. Sur un écran d'au moins 600 dp (plus petite largeur), l'orientation, le redimensionnement et le ratio imposés sont **ignorés**, avec une dérogation temporaire. | Le lecteur doit gérer les encarts système et les découpes d'écran, prévoir le retour prédictif et accepter toutes les tailles de fenêtre. |
| Android 17 (API 37) | La dérogation d'orientation disparaît pour les apps ciblant l'API 37 (échéance Play : août 2027). **Durcissement de l'audio en arrière-plan.** `AutomaticZenRule.TYPE_TRANSIT` est ajouté. | Il faut prévoir la cloche de méditation en arrière-plan (§ 2.10) et ne verrouiller la rotation que sur téléphone. |

### 2.2 Architecture de l'information

**Principe.** Le *Paroissien* est d'abord un livre qu'on ouvre. L'application doit donc mener au texte en un ou deux gestes. On distingue deux modes :

- le mode **« bibliothèque »** sert à choisir quoi lire, avec une navigation visible ;
- le mode **« lecture »** affiche le livre ouvert et rien d'autre.

**Carte de navigation recommandée :**

```
Paroissien
├── ☼ Aujourd'hui  (destination de départ)
│     ├── Messe du jour ─────────────┐
│     ├── Évangile du jour ──────────┤
│     ├── Lettre du mois (Clairval) ─┤──► Lecteur, à la bonne position
│     ├── Méditation du jour ────────┤
│     ├── Reprendre la lecture ──────┘
│     ├── ‹ jour précédent | jour suivant ›   (préparer dimanche)
│     ├── Temps de prière ─────────────► Séance de méditation
│     └── Calendrier du mois (V1)
├── ▤ Bibliothèque
│     ├── Continuer (dernier livre ouvert)
│     ├── Livres par rayon ──► Sommaire ──► Lecteur
│     └── Contenus à télécharger (paquets hors ligne, agent 11)
├── ⚑ Signets
│     ├── Rubans │ Images pieuses │ Notes (V1) ──► Lecteur
├── ⌕ Recherche
│     ├── Plein texte (portée : tout, Bible, Missel, Lettres, Prières)
│     └── Références (« Jn 3,16 », « Ps 22 ») ──► Lecteur
└── ⚙ Réglages  (icône de la barre supérieure, pas un onglet)

Lecteur (plein écran, sans barre de navigation)
  calques au tap : chrome haut et bas · Sommaire · Aller à · Rubans · Aa (texte) · Thème · Méditer
```

**Pourquoi quatre entrées et pas cinq :**

- les Réglages sont rarement ouverts : un onglet serait gaspillé ;
- la Méditation est une action contextuelle, lancée depuis Aujourd'hui, depuis le lecteur ou depuis la retraite guidée de l'agent 02, et non un lieu ;
- la Recherche mérite un onglet, car la recherche plein texte et les références bibliques sont un vrai point fort d'une liseuse, que le papier n'offre pas.

L'alternative à trois onglets (sans Recherche, avec une loupe dans la barre supérieure) reste possible : voir la décision D1.

**Comportement du retour (retour prédictif d'Android 16) :**

- Dans le lecteur, le retour ferme d'abord le calque ouvert (feuille, sommaire), puis quitte le lecteur vers l'écran d'origine. La position est sauvegardée en permanence.
- Le retour ne revient jamais page par page : c'est contre-intuitif sur Android.
- Après un saut (lien, sommaire, « aller à », ruban), une puce **« ↩ Revenir à p. 112 »** apparaît dans le chrome. Dans un missel, on fait sans cesse des allers-retours entre l'ordinaire et le propre, et c'est le pendant numérique du doigt glissé entre les pages.

**Premier lancement**, en trois écrans au plus, sans compte ni permission :

1. choix de la ou des formes liturgiques (ordinaire, extraordinaire ou les deux) et du calendrier propre (France par défaut) ;
2. thème et taille du texte, avec un aperçu en direct sur un vrai paragraphe ;
3. paquets de contenus à télécharger maintenant ou plus tard.

La permission NPD n'est **jamais** demandée à l'installation : on la demande au premier usage, en contexte.

### 2.3 L'écran « Aujourd'hui »

Il est entièrement piloté par le calendrier calculé hors ligne de l'agent 05 et fonctionne **même sans aucun paquet de contenu** : il affiche alors la fête, la couleur et des liens « Télécharger le propre ».

**Données attendues de l'agent 05** (contrat d'interface proposé) :

| Champ | Exemple (dimanche 11 octobre 2026) |
|---|---|
| date civile | dimanche 11 octobre 2026 |
| forme | `ORDINAIRE` ou `EXTRAORDINAIRE` |
| célébration principale | « 28ᵉ dimanche du temps ordinaire » ; en forme extraordinaire, « XXᵉ dimanche après la Pentecôte » |
| rang | dimanche, ou dimanche de IIᵉ classe |
| couleur liturgique | `VERT` (énumération : BLANC, ROUGE, VERT, VIOLET, ROSE, NOIR, et en option OR et BLEU) |
| couleurs alternatives | par exemple une mémoire facultative en blanc |
| temps liturgique | temps ordinaire, ou temps après la Pentecôte |
| commémoraisons (forme extraordinaire) | Maternité de la B. V. Marie |
| cycle | année A, semaine paire ou impaire |
| références des lectures | Is 25,6-10a · Ps 22 · Ph 4,12-14.19-20 · Mt 22,1-14 |
| drapeau « solennité » | permet un accent or facultatif (PGMR 346 : « vêtements festifs ») |

**Composition**, de haut en bas :

1. **Bandeau du jour** : date civile, nom liturgique, rang et pastille de couleur avec son **nom écrit** (« Vert »). Le bandeau prend la teinte `lit.<couleur>.container`. Les flèches ‹ › font changer de jour, ce qui sert surtout à préparer la messe du dimanche. Si les deux formes sont activées, un bouton segmenté *Ordinaire | Extraordinaire* bascule la fête et la couleur.
2. **Reprendre** : dernier livre ouvert, avec section et page.
3. **Messe du jour** : références, puis ouverture du missel au propre du jour (décision D7).
4. **Évangile du jour** : l'incipit sur deux lignes, puis le texte.
5. **Lettre du mois** (Clairval, agent 01).
6. **Méditation** : chapitre de l'*Imitation* ou exercice d'Ignace selon la retraite en cours (agents 02 et 04), avec le bouton **Temps de prière** qui ouvre la séance de méditation.

Il n'y a ni statistiques, ni séries de jours, ni badges : aucune ludification. Cela rejoint le principe « sans distraction » et la vie privée (décision D9).

### 2.4 Le lecteur : chrome, gestes et mode immersif

**Mode immersif.** Le lecteur masque les barres système avec `WindowInsetsControllerCompat.hide(WindowInsetsCompat.Type.systemBars())` et le comportement `BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE`. Les barres système réapparaissent **en même temps** que le chrome de l'application, pour un état visuel unique. Le texte respecte toujours les encarts `displayCutout` : jamais de texte sous une encoche.

**Gestes** (tous configurables, avec un équivalent bouton) :

| Geste | Action | Remarque |
|---|---|---|
| Tap sur le tiers droit ou gauche | Page suivante ou précédente | Zones inversibles pour les gauchers |
| Tap au centre | Afficher ou masquer le chrome | — |
| Glissement horizontal démarré **hors des bords** | Tourner la page (animation de l'agent 07) | Voir le conflit avec le geste retour ci-dessous |
| Appui long | Sélection, surlignage, note, ou « poser un ruban ici » | Agents 06 et 09 |
| Touches de volume | Page suivante ou précédente | Option désactivée par défaut |
| Pincement | Taille du texte | V1, à tester (risque de déclenchement accidentel) |

**Conflit avec le geste retour.** En navigation par gestes, un glissement depuis le bord gauche ou droit déclenche le retour. `View.setSystemGestureExclusionRects()` (et `Modifier.systemGestureExclusion` en Compose) peut réserver une zone, mais **le système plafonne l'exclusion à 200 dp de hauteur par bord**, sauf quand la barre de navigation est masquée en mode « collant », ce qui est précisément notre cas en mode immersif. Nous recommandons malgré tout de **ne pas exclure** le geste retour : l'utilisateur doit toujours pouvoir sortir. Les pages se tournent par tap ou par un glissement commencé à plus d'environ 32 dp du bord.

**Le chrome au tap** contient (maquette § 2.13-E) :

- **en haut** : ←, titre courant (livre et section), recherche dans le livre, menu ⋮ (lecture à voix haute, partage d'une citation, infos du livre) ;
- **en bas** :
  - la puce « ↩ Revenir », si elle s'applique ;
  - le curseur **Aller à**, avec folio et section ; un tap sur le numéro ouvre la saisie d'une page ou d'une référence ;
  - une barre d'outils flottante : **Sommaire · Rubans · Aa · Thème · Méditer**. La `FloatingToolbar` de M3 1.5 convient ; en 1.4, on l'écrit à la main.

**Pas de masquage automatique temporisé.** Le chrome disparaît quand on tourne la page ou qu'on tape au centre. Un délai imposé contredirait le critère WCAG 2.2.1 (« Réglage du délai »). Si un délai devenait nécessaire, il passerait par `AccessibilityManager.getRecommendedTimeoutMillis(ms, FLAG_CONTENT_CONTROLS)` (API 29).

**Verrouiller la rotation** : c'est un interrupteur rapide du chrome, très utile pour lire allongé. Il n'agit que sur téléphone, puisque les écrans d'au moins 600 dp l'ignorent en API 36 et 37. On le masque donc quand la plus petite largeur atteint 600 dp.

**Rubans dans le lecteur.** Les queues de rubans dépassent discrètement du bas de la page. On tire un ruban pour ouvrir sa page : la mécanique et le rendu relèvent de l'agent 09, ce rapport en fixe les jetons et les contraintes d'accessibilité.

### 2.5 Langage visuel : Material 3 ou design sur mesure ?

| Critère | M3 « standard » habillé | M3 Expressive | Design entièrement sur mesure |
|---|---|---|---|
| Accessibilité (sémantique, focus, TalkBack, 200 % de police) | Fournie et testée | Fournie | Tout est à refaire, avec un fort risque de régression |
| Scaffolds adaptatifs (barre du bas, rail, liste-détail) | `NavigationSuiteScaffold`, `ListDetailPaneScaffold` | Idem | À écrire |
| Ton visuel | Neutre, et entièrement retouchable par les jetons | Formes qui se transforment et ressorts marqués, jugés « joueurs » | Libre |
| Disponibilité dans le BOM 2026.09 | 1.4.0 stable | API retirées de la 1.4 ; 1.5 en bêta | — |
| Coût pour un développeur seul sur une VM à 2 cœurs | Faible | Moyen (version bêta) | Élevé |

**Recommandation : M3 sert d'ossature, avec un habillage « papier et encre ».**

- **Couleurs** : un `ColorScheme` construit à la main à partir de nos jetons (§ 2.6). **Pas de couleur dynamique (Material You) par défaut** : elle effacerait l'identité liturgique.
- **Typographie** : une `Typography` M3 remappée, avec des titres à empattements et de petits libellés en police système (§ 2.8).
- **Formes** : rayons de 4 dp (puces, champs), 8 dp (cartes, boutons) et 16 dp (haut des feuilles). On remplace les « pilules » M3 par défaut, pour évoquer l'imprimé.
- **Élévation** : pas d'ombres portées. On utilise des filets (`line.hairline`) et des surfaces tonales. Seule la page porte une ombre de tranche (agent 07).
- **Icônes** : Material Symbols *Outlined* (Apache 2.0), graisse 300. On dessine quelques icônes propres : ruban, calice, croix, livre ouvert, cloche. **Aucun émoji.**
- **Rouge réservé** : le rouge appartient aux rubriques et à la liturgie. Les erreurs d'interface utilisent un rouge brun distinct (`#8C1D18`), toujours accompagné d'une icône et d'un texte.
- **Composants Expressive** à reprendre une fois la 1.5 stable : FloatingToolbar, groupes de boutons. **Le `MotionScheme` reste « standard »** : pas de rebond.

### 2.6 Jetons de couleur

Les contrastes ont été calculés selon WCAG 2.x (luminance relative). Les valeurs entre parenthèses indiquent le contraste avec `paper.background`.

**Jetons sémantiques de base :**

| Jeton | Clair | Sépia | Sombre | Nuit |
|---|---|---|---|---|
| `paper.background` | `#FBF8F1` | `#F2E8D3` | `#17140F` | `#000000` |
| `paper.surface` (cartes, feuilles) | `#FFFDF8` | `#F8F0DE` | `#211D17` | `#0D0A07` |
| `ink.primary` (corps) | `#1F1B16` (16,1) | `#2E2418` (12,5) | `#E6DED0` (13,8) | `#9A7F5C` (5,6) |
| `ink.secondary` | `#5E554A` (6,9) | `#5A4B38` (6,9) | `#B3A897` (7,8) | `#8A7252` (4,6) |
| `ink.tertiary` (folios, en-têtes courants) | `#7A7064` (4,6) | `#6E5E48` (5,1) | `#9A8F7F` (5,8) | = secondary |
| `ink.rubric` (rubriques, ℣ ℟, numéros de versets) | `#A31F1A` (7,2) | `#962216` (6,8) | `#E88A78` (7,3) | `#C2604F` (5,1) |
| `line.hairline` (décoratif) | `#DDD4C4` | `#D4C4A3` | `#3A332A` | `#2A2118` |
| `ui.primary` (boutons pleins, curseurs) | `#5A4630` (8,4) | `#57412A` (7,9) | `#D9C3A0` (10,7) | `#9A7F5C` (5,6) |
| `ui.error` | `#8C1D18` (8,6) | idem | `#FFB4A6` | `#C2604F` |
| `highlight.default` (surlignage, agent 09) | `#F3E3A6`, texte à 13,3:1 | idem | `#4A3F1C`, texte à 7,8:1 | `#2A2010` |
| `chrome.scrim` (voile sous le chrome) | noir à 8 % | noir à 8 % | noir à 30 % | noir à 40 % |

**Variante « contraste élevé »** : elle s'active sur l'option de l'app, ou quand `UiModeManager.getContrast()` (API 34) dépasse 0,5. L'encre passe en `#000000` sur `#FFFFFF`, ou `#FFFFFF` sur `#000000`. La rubrique devient `#8A0F0B` (9,8:1) en clair et `#FFB4A6` (12,3:1) en sombre. Les filets se renforcent et la texture papier est coupée. `AccessibilityManager.isHighContrastTextEnabled()` est disponible à partir de l'API 36 pour notre rendu personnalisé.

**Constat 1 : les rubriques ne se distinguent que par la teinte.** Le rouge et le noir du corps ne contrastent qu'à 1,8 à 2,3:1. C'est conforme à la tradition typographique des missels, mais pas au critère WCAG 1.4.1 (« Utilisation de la couleur ») pour qui ne perçoit pas le rouge. **Recommandation** : composer les rubriques en rouge **et en italique**, à 0,85 fois la taille du corps, comme dans de nombreux missels imprimés. Le réglage « Rubriques en italique aussi » est activé par défaut. Les symboles ℣ et ℟ restent rouges, mais sont des glyphes distincts.

**Couleurs liturgiques.** La PGMR n° 346 fixe le blanc, le rouge, le vert et le violet, « le noir aux messes des défunts, là où c'est la coutume », le rose à *Gaudete* et *Laetare*, et autorise aux jours les plus solennels des vêtements « festifs » même d'une autre couleur. La forme extraordinaire emploie les mêmes couleurs, le noir y étant d'usage courant. Le bleu relève d'un privilège local : désactivé par défaut, il reste à valider avec l'agent 05.

| Couleur | Tissu, thèmes clairs (ruban, pastille) | Tissu, thèmes sombres | Accent de texte clair | Accent de texte sombre | Conteneur clair / texte | Conteneur sombre / texte |
|---|---|---|---|---|---|---|
| Vert | `#2E6B3A` | `#4E9A5E` (5,3) | `#2E6B3A` (6,0) | `#7FC08B` (8,6) | `#DDEBDB` / `#163B1F` (10,1) | `#1F3524` / `#BFE3C4` (9,4) |
| Rouge | `#A3171C` | `#C8423F` (3,8) | `#A3171C` (7,3) | `#F07D74` (6,9) | `#F6DAD6` / `#5E0D0F` (10,3) | `#45191A` / `#FFC9C2` (10,2) |
| Violet | `#5B2A6E` | `#8E5BA6` (3,7) | `#5B2A6E` (9,9) | `#C49AD6` (7,8) | `#E9DDEE` / `#371443` (11,9) | `#33213B` / `#E6CFEF` (10,2) |
| Rose | `#C9708F` | `#D886A3` (6,9) | `#A2405F` (5,8) | `#F0A6BF` (9,5) | `#F7DDE6` / `#5C1F33` (9,7) | `#45242F` / `#FFD3E1` (10,1) |
| Blanc | `#F4EEDC` + **liseré** `#B8912F` | `#EDE6D3` | `#7A5E14` (5,8, encre or) | `#DDBB66` (10,0) | `#F3ECD8` / `#4A3B12` (9,2) | `#38321F` / `#F2E6C2` (10,3) |
| Noir | `#1C1C1C` | `#0A0A0A` + **liseré** `#5A5650` | `#2B2B2B` (13,4) | `#BDB6AA` (9,1) | `#E1DDD6` / `#1C1C1C` (12,6) | `#2A2826` / `#E3DED6` (11,0) |
| Or (facultatif, solennités) | `#B8912F` | `#DDBB66` | `#7A5E14` | `#DDBB66` | — | — |

Règles d'usage :

- un élément non textuel doit contraster d'au moins 3:1 avec son fond (WCAG 1.4.11). Le blanc sur papier clair et le noir sur fond sombre n'y parviennent pas : d'où les **liserés** obligatoires ;
- le nom de la couleur est **toujours écrit** à côté de la pastille ;
- en thème *Nuit*, la luminance des couleurs liturgiques est atténuée d'environ 40 %, tout en gardant au moins 3:1.

**Constat 2 : les rubans vert et rouge se confondent pour un daltonien.** La simulation de Machado et coll. (2009, sévérité 1,0) le montre :

- en **deutéranopie**, le vert `#2E6B3A` devient `#645D3D` et le rouge `#A3171C` devient `#685D15` : le contraste entre eux tombe à **1,0:1**, ils sont indiscernables ;
- en protanopie, il reste de 1,7:1.

Or le vert et le rouge sont les rubans les plus utilisés. **Exigence pour l'agent 09** : chaque ruban porte un second code. On propose une forme d'extrémité différente et une initiale brodée :

| Ruban | Extrémité | Initiale |
|---|---|---|
| Vert | queue d'aronde | V |
| Rouge | coupe droite | R |
| Violet | pointe | Vi |
| Blanc | double encoche | B |
| Rose | arrondie | Ro |
| Noir | biseau | N |

**Teinte liturgique de l'interface** (décision D3). Nous recommandons un accent **discret** qui suit la couleur du jour : bandeau d'Aujourd'hui, indicateur de l'onglet actif, curseur de progression. La base papier et encre reste neutre. Une option permet de le désactiver.

### 2.7 Thèmes

| Thème | Intention | Particularités |
|---|---|---|
| **Clair** | Papier blanc cassé et chaud ; lecture de jour | La texture papier est très légère (agent 07). |
| **Sépia** | Papier ancien ; lecture prolongée | Contraste un peu plus doux (12,5:1), tout en dépassant largement le niveau AAA. |
| **Sombre** | Suit le mode sombre du système | Fond brun très foncé plutôt que noir pur, pour éviter le « smearing » des écrans OLED en défilement. Le texte est crème, jamais blanc pur, pour limiter le halo. Pas de texture. |
| **Nuit** | Lecture au lit, dans le noir | Encre **ambre sur noir pur**, de luminance faible (L ≈ 0,15 à 0,28) mais à au moins 4,5:1. Il n'y a que deux niveaux d'encre, une rubrique rouge orangé et aucune texture. Un curseur de **luminosité propre à l'app** agit sur `WindowManager.LayoutParams.screenBrightness` (0 à 1). Pour descendre sous le minimum matériel, on ajoute un voile noir facultatif. La fonction système « Luminosité réduite » peut être suggérée dans l'aide. |

- **Auto** est le défaut : *Clair* ou *Sombre* selon le système.
- Le passage automatique en *Nuit* se fait **sur une plage horaire** choisie par l'utilisateur (par exemple 21 h 30 à 6 h 30), jamais au coucher du soleil, ce qui exigerait la localisation, contraire à la vie privée.
- Les barres système et l'écran de démarrage suivent le thème actif.
- Une variante « Nuit rouge » (texte rouge sur noir, pour préserver la vision nocturne) est envisageable plus tard (décision D5).

### 2.8 Typographie

**Polices (toutes sous licence OFL, vérifiées dans le dépôt google/fonts et chez l'auteur) :**

| Rôle | Police | Pourquoi | Points d'attention |
|---|---|---|---|
| Lecture, par défaut | **Literata** (TypeTogether, commandée pour Google Play Books) | Variable sur deux axes, optique de 7 à 72 et graisse de 200 à 900 ; petites capitales, chiffres elzéviriens | Fichier de 955 Ko pour le romain seul ; **pas de ℣ ℟ ✠** |
| Lecture « classique » | **EB Garamond** (Octavio Pardo, d'après Georg Duffner) | Ton « missel » ; petites capitales, ligatures historiques (`hist`, `dlig`, `swsh`) ; **contient ℣ (U+2123), ℟ (U+211F) et ℞** | 851 Ko pour le romain ; pas de ✠ |
| Lettrines | **EB Garamond Initials**, avec les couches F1 et F2 (Duffner) | Initiales d'après une Bible française imprimée au XVIᵉ siècle, sous OFL ; en deux couches, elles permettent une lettre rouge sur un ornement | Dépôt `georgd/EB-Garamond`, à empaqueter |
| Lecture « grande lisibilité » | **Atkinson Hyperlegible Next** (2025) | Pour la basse vision | Pas de ℣ ℟ ✠ ; pas de petites capitales |
| Interface | Police système (Roboto Flex ou celle du constructeur) | Taille de l'APK nulle, familière, et adaptée à la mise à l'échelle non linéaire | — |

Alternatives évaluées : Source Serif 4, la seule à contenir l'espace fine insécable U+202F, et Crimson Pro.

**Constat de couverture :**

- aucune des cinq polices testées ne contient **✠ (U+2720)** ni **✝** ;
- seul EB Garamond contient ℣ et ℟ ;
- l'espace fine insécable (U+202F), indispensable devant ; : ! ? en français, manque partout sauf dans Source Serif 4.

**Recommandation** : une chaîne de secours (`FontFamily` avec repli) vers une **petite police maison « Liturgica »** de quelques glyphes (℣ ℟ ✠ ☩ ✝), dessinée ou dérivée d'EB Garamond sous OFL. Le pipeline de contenu (agent 11) normalise les espaces. Les polices doivent être **sous-ensemblées** (Latin étendu et symboles) : comptez environ 4 Mo au total avant sous-ensemble, romain et italique compris.

**Échelle typographique de l'interface** (sp, rôles M3 remappés) :

| Rôle | Taille / interligne | Police | Usage |
|---|---|---|---|
| `headlineMedium` | 28 / 36 | Literata, opsz 28, graisse 450 | Nom de la fête (Aujourd'hui) |
| `headlineSmall` | 24 / 32 | Literata, opsz 24 | Titres d'écran |
| `titleLarge` | 22 / 28 | Literata | Titres de livres |
| `titleMedium` | 16 / 24 | Système, graisse 500 | Titres de cartes |
| `bodyLarge` | 16 / 24 | Système | Texte d'interface |
| `bodyMedium` | 14 / 20 | Système | Descriptions |
| `labelLarge` | 14 / 20 | Système, graisse 500 | Boutons |
| `labelMedium` | 12 / 16 | Système, graisse 500, petites capitales simulées (+0,5 d'interlettrage) | Surtitres (« MESSE DU JOUR »), onglets |

Aucun texte n'est en dessous de 12 sp. Tout est exprimé en `sp` : la mise à l'échelle non linéaire d'Android 14, jusqu'à 200 %, s'applique alors d'elle-même.

**Échelle du lecteur**, relative à la taille de base T (18 sp par défaut, réglable de 14 à 32 sp, puis multipliée par l'échelle système) :

| Élément | Taille | Style |
|---|---|---|
| Corps | T, interligne 1,45 T | Justifié, césure FR et LA (agent 06), alinéa de 1 em sans blanc entre paragraphes |
| Rubrique | 0,85 T | Rouge et italique |
| ℣ / ℟, numéros de versets | 0,7 T | Rouge, en exposant pour les versets |
| Titre de chapitre | 1,5 T | Petites capitales, interlettrage +5 %, opsz « display » |
| Sous-titre | 1,15 T | Italique |
| Lettrine | 3 lignes | EB Garamond Initials, lettre en `ink.rubric` ou `lit.accent` |
| En-tête courant et folio | 0,7 T | Petites capitales, `ink.tertiary` |
| Mesure (longueur de ligne) | 45 à 75 caractères, 60 à 66 visés | Les marges latérales s'adaptent ; mesure plafonnée sur tablette |

### 2.9 Mouvement

| Jeton | Valeur | Usage |
|---|---|---|
| `motion.instant` | 0 ms | Toute animation quand « Réduire les animations » est actif |
| `motion.fade.short` | 100 ms, standard | Apparition du chrome (fondu et translation de 8 dp) |
| `motion.chrome.enter` / `exit` | 200 ms en décélération accentuée / 150 ms en accélération accentuée | Barres du lecteur |
| `motion.page.slide` | 300 ms, standard (0,2 ; 0 ; 0 ; 1) | Tourne-page « glissement » |
| `motion.page.curl` | Suit le doigt, puis ressort **sans rebond** (amortissement 1,0, raideur moyenne) | Tourne-page « courbure » (agent 07) |
| `motion.page.fade` | 150 ms | Alternative douce |
| `motion.sheet` | Valeurs par défaut de M3 | Feuilles, dialogues |
| `motion.ribbon` | Ressort à amortissement 0,8 | Le ruban qu'on tire (agent 09) |

**Réduire les animations :**

- **Réglage dans l'app** : *Suivre le système* (défaut), *Toujours réduire* ou *Jamais*.
- **Détection système** : `ValueAnimator.areAnimatorsEnabled()` (API 26) ou `Settings.Global.ANIMATOR_DURATION_SCALE == 0`. C'est ce que règle l'option d'accessibilité Android « Supprimer les animations ».
- **Compose** respecte déjà ce réglage par l'intermédiaire de `MotionDurationScale`, que les mainteneurs indiquent comme le mécanisme utilisé pour couper les animations. Les animations M3 sautent donc à leur état final. Notre tourne-page, piloté par le geste, doit en revanche lire le réglage lui-même.
- **Effet en mode réduit** :
  - le tourne-page passe à « Aucune », ou à un fondu de 150 ms si l'utilisateur le préfère ;
  - pas de parallaxe ni de physique de ruban ;
  - l'anneau de méditation est mis à jour chaque minute, sans animation continue ;
  - le retour haptique reste disponible.
- C'est le critère WCAG 2.3.3, niveau AAA, visé ici.

### 2.10 Mode méditation « Ne pas déranger »

#### 2.10.1 Ce que dit la plateforme en octobre 2026

- **Android 15 et plus (cible API 35 ou plus)** : les apps « ne peuvent plus changer l'état global ou la politique du NPD ». Elles doivent contribuer une `AutomaticZenRule`, et le système fusionne les règles actives selon le principe « la plus restrictive l'emporte ». Les appels `setInterruptionFilter()` et `setNotificationPolicy()` créent ou mettent à jour une **règle implicite** au nom de l'application. Conséquence : `setInterruptionFilter(INTERRUPTION_FILTER_ALL)` ne peut plus éteindre un NPD activé par l'utilisateur ou par une autre application. Le comportement est donc plus sûr pour nous.
- **Autorisation** : toutes ces API exigent l'**accès « Ne pas déranger »**. On déclare la permission `ACCESS_NOTIFICATION_POLICY`, mais l'utilisateur doit l'accorder **à la main** dans les réglages, via `Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS`. La documentation prévient que « dans certains cas, l'activité peut ne pas exister » : il faut se protéger. L'état se lit avec `isNotificationPolicyAccessGranted()`, et `ACTION_NOTIFICATION_POLICY_ACCESS_GRANTED_CHANGED` signale tout changement. Sans cette autorisation, `addAutomaticZenRule` lève une `SecurityException`.
- **Modes d'Android 15** : `AutomaticZenRule.Builder` (API 35) accepte un **type**. `TYPE_BEDTIME` est réservé à l'app Bien-être numérique et `TYPE_MANAGED` au propriétaire de l'appareil. `TYPE_THEATER` correspond à un lieu « où le silence est demandé ». On peut aussi fournir une icône (`setIconResId`), une description de déclenchement, une activité de configuration et `setManualInvocationAllowed(true)`, qui permet à l'utilisateur d'activer le mode lui-même. Le mode apparaît alors dans *Réglages › Modes*, où l'utilisateur peut le personnaliser. `areAutomaticZenRulesUserManaged()` indique dans ce cas qu'il faut renvoyer vers `Settings.ACTION_AUTOMATIC_ZEN_RULE_SETTINGS` avec `EXTRA_AUTOMATIC_ZEN_RULE_ID`.
- **Effets sur l'appareil** (`ZenDeviceEffects`, API 35) : niveaux de gris (`setShouldDisplayGrayscale`), fond d'écran assombri, écran toujours allumé coupé et thème sombre forcé, **uniquement pendant que la règle est active**.
- **Avant Android 15** : le constructeur `AutomaticZenRule(name, owner, configurationActivity, conditionId, ZenPolicy, filter, enabled)` existe depuis l'API 29, et `setAutomaticZenRuleState()` aussi. **Avec un minSdk d'au moins 29, un seul mécanisme couvre donc toutes les versions.** En dessous de 29, il faudrait l'ancienne méthode (`getCurrentInterruptionFilter` à mémoriser, puis restaurer), avec un risque de rester bloqué en NPD.
- **Rétablissement** : avec une règle, **rien n'est à restaurer à la main**. Désactiver notre règle (`Condition.STATE_FALSE`) laisse intact un NPD manuel ou un autre mode actif. Si l'utilisateur a « mis en veille » notre règle depuis les réglages rapides, le système ignore nos `STATE_TRUE` jusqu'au cycle suivant (diffusion `AUTOMATIC_RULE_STATUS_DEACTIVATED`).

#### 2.10.2 Conception du mode « Temps de prière »

Ce qui suit est une spécification, pas du code.

1. **Création, une seule fois**, après l'obtention de l'autorisation :
   - nom : « Temps de prière » ;
   - type : `TYPE_THEATER`, ou `TYPE_OTHER` (décision mineure : *theater* exprime bien « silence demandé ») ;
   - filtre : `INTERRUPTION_FILTER_PRIORITY` ;
   - `ZenPolicy` : `disallowAllSounds()`, puis `allowAlarms(true)`, `allowMedia(true)` (indispensable pour que **notre cloche** sonne), `allowCalls(PEOPLE_TYPE_STARRED)` et `allowRepeatCallers(true)`. Ce dernier réglage par défaut est un choix de sécurité, à valider en décision D8 ;
   - effets : écran toujours allumé coupé, et niveaux de gris si l'option est cochée ;
   - icône : une bougie ou une croix ;
   - activité de configuration : l'écran « Méditation » des Réglages ;
   - `setManualInvocationAllowed(true)` ;
   - l'identifiant de la règle est conservé dans DataStore. À chaque démarrage, `getAutomaticZenRules()` vérifie qu'elle existe encore, puisque l'utilisateur peut la supprimer.
2. **Début de séance** : `setAutomaticZenRuleState(id, Condition(uri, "Temps de prière", STATE_TRUE, SOURCE_USER_ACTION))`. Le paramètre `source` exige l'API 35 ; avant, on utilise le constructeur à trois arguments.
3. **Fin de séance**, qu'elle soit normale, interrompue ou abandonnée : même appel avec `STATE_FALSE`.
4. **Filets de sécurité** contre un NPD resté actif :
   - service de premier plan pendant toute la séance (voir 2.10.3) ;
   - au démarrage de l'app, si aucune séance n'est en cours et que `getAutomaticZenRuleState(id) == STATE_TRUE` (API 35), on désactive ;
   - le mode est visible et éteignable dans les réglages rapides d'Android 15.
5. **Refus ou absence de l'autorisation** : la séance a lieu quand même. Un bandeau propose « Activer “Ne pas déranger” vous-même » avec un lien vers les réglages. Le refus n'est jamais bloquant et n'est pas redemandé à chaque fois (une fois par mois au plus).

#### 2.10.3 Écran, minuteur et cloche

| Fonction | API ou technique | Autorisation |
|---|---|---|
| Écran allumé | `Modifier.keepScreenOn()` sur l'écran de séance (Compose UI 1.9 et plus) | Aucune |
| Luminosité | `WindowManager.LayoutParams.screenBrightness` (0 à 1) pour notre fenêtre seulement, réinitialisée en quittant l'écran ; voile noir facultatif. Android 17 QPR (« version 37.2 ») ajoute `screenBrightnessUnit` (pourcentage) | Aucune |
| Niveaux de gris | Système entier : `ZenDeviceEffects` (Android 15 et plus, règle active). Avant Android 15, et en complément, filtre de saturation nulle sur **notre** contenu (`ColorMatrix`) | Accès NPD pour l'option système |
| Minuteur au premier plan | Horloge `SystemClock.elapsedRealtime()`, l'état survivant à la rotation | Aucune |
| Cloches | Son court avec `AudioAttributes` `USAGE_MEDIA` et `CONTENT_TYPE_SONIFICATION`. Notre `ZenPolicy` laisse passer les médias. Bouton « Tester la cloche » et vibration de repli si le volume média est nul | Aucune |
| Cloche **écran éteint** | **Android 17** : jouer du son en arrière-plan exige un service de premier plan qui ne soit pas `SHORT_SERVICE`, avec la capacité « pendant l'utilisation » s'il est **démarré depuis l'app visible**. Sinon, **échec silencieux**. Il faut donc démarrer un FGS `mediaPlayback` au tap sur « Commencer ». Pour l'échéance : soit un wake-lock partiel borné à la durée de la séance, soit `AlarmManager.setExact(…, OnAlarmListener, …)`, qui n'exige pas `SCHEDULE_EXACT_ALARM` selon la documentation mais dépend du processus vivant et du Doze, à tester. `SCHEDULE_EXACT_ALARM` n'est plus pré-accordée depuis l'API 33, et `USE_EXACT_ALARM` est réservée aux apps de réveil ou d'agenda | `FOREGROUND_SERVICE_MEDIA_PLAYBACK` ; notification du FGS (masquée dans le volet si `POST_NOTIFICATIONS` est refusée) |

**Recommandation** : en MVP, la séance se déroule à l'écran (allumé, très sombre). En V1, on ajoute l'option « Continuer écran éteint » (FGS `mediaPlayback`, conçu avec l'agent 10).

#### 2.10.4 Parcours utilisateur

```
[Aujourd'hui « Temps de prière »] ou [Lecteur › Méditer] ou [Retraite (02) › Exercice]
        │
        ▼
Feuille « Temps de prière »  (durée, cloches, NPD, écran, niveaux de gris, texte)
        │ Commencer
        ▼
NPD coché ? ── non ───────────────────────────────────────────┐
        │ oui                                                 │
Accès accordé ? ── oui ──► activer la règle ───────────────────┤
        │ non                                                  │
Écran d'explication (« pourquoi, quoi, réversible »)         │
  [Ouvrir les réglages Android]  [Continuer sans]              │
        │ retour à l'app (onResume) : accès vérifié            │
        ├── accordé ► créer la règle ► activer ────────────────┤
        └── refusé ► séance sans NPD (bandeau d'aide discret) ──┤
                                                               ▼
Séance : cloche de début ► écran noir, texte du passage, temps restant masquable,
         luminosité réduite, écran allumé, chrome au tap (Pause · +5 min · Terminer)
         Geste retour ► « Terminer le temps de prière ? »
        │ fin du minuteur, ou Terminer
        ▼
Cloche de fin ► désactiver la règle (STATE_FALSE) ► rétablir la luminosité
        ▼
« Fin du temps de prière » : [Revenir au texte] [Noter une intention (V1)]  — sans statistiques
```

Avec **TalkBack**, les commandes restent visibles en permanence, et le temps restant est annoncé seulement sur demande, sans région « live » bavarde.

### 2.11 Mises en page adaptatives

**Points de rupture officiels** : largeur *Compact* (moins de 600 dp), *Medium* (600 à 840), *Expanded* (840 à 1200), *Large* (1200 à 1600) et *Extra-large* (1600 et plus) ; hauteur *Compact* (moins de 480), *Medium* (480 à 900) et *Expanded* (900 et plus). La V2 s'obtient avec `currentWindowAdaptiveInfoV2()`, plus `supportLargeAndXLargeWidth = true`.

| Fenêtre typique | Navigation de l'app | Lecteur |
|---|---|---|
| Téléphone en portrait (largeur Compact) | Barre du bas, 4 entrées | 1 page |
| Téléphone en paysage (largeur Medium ou Expanded, **hauteur Compact**) | **Rail** : une barre du bas mangerait la hauteur | Double page **si les critères ci-dessous sont remplis**, sinon une colonne centrée à mesure plafonnée |
| Petite tablette en portrait, pliable ouvert (Medium) | Rail | 1 page, marges généreuses |
| Tablette en paysage (Expanded) | Rail ; Bibliothèque et Signets en **liste-détail** | Double page |
| Large et XL (tablette 13 pouces, bureau, fenêtre libre) | Rail étendu | Double page, taille de page plafonnée et centrée |
| Pliable en **posture livre** (`FoldingFeature` `HALF_OPENED`, pli vertical, `isSeparating`) | — | Double page dont la gouttière **épouse le pli** (`bounds` du pli) |
| Pliable en posture table (pli horizontal) | — | Plus tard : texte en haut, commandes en bas (missel posé sur le banc, méditation) |

**Critères de la double page** : la décision se fonde sur la géométrie, pas sur le nom de la classe de taille. On calcule :

- largeur de page = (largeur de fenêtre − marges − gouttière) ÷ 2 ;
- caractères par ligne ≈ largeur de page ÷ (0,5 × taille du corps).

La double page s'applique si **les quatre conditions** sont réunies :

1. ratio largeur sur hauteur d'au moins 1,2 ;
2. au moins 38 caractères par ligne ;
3. au moins 12 lignes par page ;
4. réglage « Double page en paysage » sur *Auto* ou *Toujours*.

Exemple sur un téléphone de 915 × 412 dp à 18 sp : environ 47 caractères et 13 lignes par page, d'où la double page. À 200 % de taille, la condition n'est plus remplie et l'on revient à une seule page. Les seuils sont à calibrer sur un appareil réel.

**Conservation exacte de la position lors d'une rotation ou d'un redimensionnement** (spécification, en interface avec les agents 06 et 10) :

1. La position est une **ancre de contenu**, par exemple `(livreId, blocId, décalage en caractères)`, qui désigne le premier caractère de la page affichée. En double page, c'est celui de la page de gauche.
2. Lors d'une repagination (rotation, police, taille, fenêtre), on **pagine d'abord autour de l'ancre**, puis le reste en tâche de fond. On affiche ensuite la page, ou la double page, qui **contient** l'ancre.
3. **L'ancre n'est pas recalculée** depuis la nouvelle pagination : elle ne change que si l'utilisateur tourne la page, saute ou fait défiler. Sans cette règle, chaque rotation décale l'ancre au début de la nouvelle page, et une suite de rotations finit par faire avancer de plusieurs paragraphes : c'est la « dérive ».
4. Passer de la double page à la simple montre la page de gauche, comme un livre qu'on referme à moitié. On évite les heuristiques du type « deviner la page lue » : elles sont imprévisibles.
5. **Côté Android** :
   - l'ancre est enregistrée dans `rememberSaveable` ou `SavedStateHandle` pour survivre à la mort du processus, et persistée en base pour « Reprendre » ;
   - pour éviter de recréer l'activité, l'agent 10 décidera s'il faut déclarer `android:configChanges` (orientation, taille d'écran, disposition). Le blog Android 17 recommande, pour les apps Compose, de recomposer plutôt que de recréer ;
   - pendant la repagination, l'ancien rendu reste affiché, ou un fondu de 100 ms au plus est appliqué. Jamais de page blanche.

### 2.12 Accessibilité

| Domaine | Exigences |
|---|---|
| **Taille de police** | Interface entièrement en `sp`, testée à 200 % (mise à l'échelle non linéaire d'Android 14). Taille du lecteur = taille de base T de l'app × échelle système. Sur la barre du bas, les libellés passent sur une ligne, puis l'icône seule avec sa description, et jamais tronqués au milieu d'un mot. |
| **TalkBack dans le lecteur** | C'est le **risque principal**. Si l'agent 06 dessine le texte sur un `Canvas`, l'arbre d'accessibilité est vide. **Exigence** : exposer chaque paragraphe comme un nœud sémantique de texte, dans l'ordre de lecture, en-têtes marqués avec `heading()`. On ajoute des `customActions` « Page suivante », « Page précédente », « Sommaire », « Poser un ruban » et les actions de défilement, pour que les gestes à deux doigts tournent la page. Quand `isTouchExplorationEnabled()` est vrai, on **propose** le mode « défilement continu », plus naturel avec un lecteur d'écran. |
| **Latin** | Les passages latins sont marqués avec une `LocaleList("la")` (ou l'italien en repli) dans `SpanStyle`, pour que la synthèse vocale change de prononciation. La prise en charge du latin par les moteurs TTS est à vérifier (agent 06). Les rubriques sont annoncées. Les symboles ℣ et ℟ portent la description « Verset » et « Répons ». |
| **Contraste** | Texte à au moins 4,5:1 et éléments non textuels à au moins 3:1 dans les quatre thèmes (§ 2.6). Variante contraste élevé (`UiModeManager.getContrast()`). Les rubriques sont en italique en plus du rouge (WCAG 1.4.1). |
| **Couleur seule** | Le nom de la couleur liturgique est écrit, les rubans ont une forme et une initiale, le surlignage a un soulignement facultatif. |
| **Animation** | Alternatives au tourne-page : glissement, fondu ou aucune ; mode réduit (§ 2.9). |
| **Cibles tactiles** | Au moins 48 × 48 dp. Les zones de tap des pages sont doublées par des boutons dans le chrome et par les touches de volume. |
| **Délais** | Pas de masquage temporisé du chrome du lecteur. En méditation, les commandes restent visibles sous TalkBack. |
| **Écran Aujourd'hui** | Bandeau annoncé ainsi : « Dimanche 11 octobre, 28ᵉ dimanche du temps ordinaire, couleur liturgique : vert ». Les cartes sont des boutons dont le libellé est complet. |

### 2.13 Maquettes ASCII des écrans clés

Les contenus sont donnés à titre d'exemple pour le 11 octobre 2026. Les références et formulations sont à valider par les agents de contenu.

**A. Aujourd'hui** (téléphone, portrait)

```
┌──────────────────────────────────────────┐
│ ‹  Dimanche 11 octobre 2026          › ⚙ │
│▐ 28ᵉ dimanche du temps ordinaire         │  bandeau teinté
│▐ ● Vert · Année A                        │  lit.vert.container
│▐ [ Ordinaire │ Extraordinaire ]          │  (si les deux formes)
├──────────────────────────────────────────┤
│  REPRENDRE                               │
│  Imitation de Jésus-Christ               │
│  Livre I, ch. III · p. 14             ›  │
├──────────────────────────────────────────┤
│  MESSE DU JOUR                           │
│  Is 25,6-10a · Ps 22 · Ph 4,12-20        │
│  Ouvrir le missel                     ›  │
│ ──────────────────────────────────────── │
│  ÉVANGILE · Mt 22,1-14                   │
│  « Le royaume des cieux est semblable    │
│  à un roi qui fit les noces de son… » ›  │
│ ──────────────────────────────────────── │
│  LETTRE DU MOIS · Clairval               │
│  Octobre 2026 — (titre de la lettre)  ›  │
│ ──────────────────────────────────────── │
│  MÉDITATION                              │
│  Imitation, livre I, ch. XI           ›  │
│  [ ◷  Temps de prière · 15 min ]         │
├──────────────────────────────────────────┤
│ ☼ Aujourd'hui  ▤ Biblioth.  ⚑ Signets  ⌕ │
└──────────────────────────────────────────┘
En forme extraordinaire, le bandeau devient :
« XXᵉ dimanche après la Pentecôte · ● Vert · comm. Maternité de la B. V. M. »
```

**B. Bibliothèque**

```
┌──────────────────────────────────────────┐
│ Bibliothèque                       ⌕  ⚙  │
├──────────────────────────────────────────┤
│ CONTINUER                                │
│ █ Missel romain 1962                     │  █ = dos du livre (couleur de reliure)
│ █ Propre du temps · p. 412               │
│ █ ━━━━━━━━━━━━━━━━━━━━░░░░░░░░  38 %    │
│                                          │
│ LITURGIE                                 │
│ █ Missel romain 1962              ›      │
│ █ Missel romain (forme ordinaire) ›      │
│ █ Prières classiques (lat.-fr.)   ›      │
│ ÉCRITURE ET SPIRITUALITÉ                 │
│ █ La Sainte Bible                 ›      │
│ █ Imitation de Jésus-Christ       ›      │
│ █ Exercices spirituels            ›      │
│ █ Lettres de Clairval (1975-2026) ›      │
│ À TÉLÉCHARGER                            │
│   Vies de saints · 12 Mo          [ ↓ ]  │
├──────────────────────────────────────────┤
│ ☼ Aujourd'hui  ▤ Biblioth.  ⚑ Signets  ⌕ │
└──────────────────────────────────────────┘
Le format liste, plutôt que la grille de couvertures, résiste mieux à une police à 200 %.
Sur tablette (Expanded) : liste à gauche, sommaire du livre à droite.
```

**C. Lecteur en portrait** (immersif, aucun chrome)

```
┌──────────────────────────────────────────┐
│ IMITATION DE J.-C.         LIVRE I · III │  en-tête courant (ink.tertiary)
│                                          │
│              CHAPITRE III                │
│       De la doctrine de la vérité        │
│                                          │
│ ╔═══╗ eureux celui que la vérité instruit│  lettrine sur 3 lignes
│ ║ H ║ elle-même, non par des figures et  │  (EB Garamond Initials)
│ ╚═══╝ des paroles qui passent, mais en   │
│ se montrant telle qu'elle est ! Notre    │
│ raison et nos sens voient peu, et nous   │
│ trompent souvent.                        │
│    À quoi servent ces grandes disputes   │
│ sur des choses cachées et obscures, dont │
│ on ne nous fera point un crime, au jour  │
│ du jugement, d'avoir été ignorants ?     │
│    …                                     │
│                                          │
│                    14              ▌ ▌   │  folio · queues de rubans
└──────────────────────────────────────────┘
 zones de tap :  [‹ précédente | chrome | suivante ›]
```

**D. Lecteur en paysage, double page** (missel bilingue « en regard », décision D6)

```
┌────────────────────────────────────────────────────────────────────────────────┐
│ ORDINARIUM MISSÆ                        12 ┃ 13            ORDINAIRE DE LA MESSE │
│                                            ┃                                     │
│ Sacerdos, stans ante infimum gradum altá-  ┃ Le prêtre, debout au bas de l'autel,│
│ ris, signat se signo crucis, dicens :  (r) ┃ fait le signe de la croix et dit :(r)│
│                                            ┃                                     │
│ In nómine Patris, ✠ et Fílii, et Spíritus  ┃ Au nom du Père, ✠ et du Fils, et du │
│ Sancti. Amen.                              ┃ Saint-Esprit. Ainsi soit-il.        │
│ ℣. Introíbo ad altáre Dei.                 ┃ ℣. Je monterai à l'autel de Dieu.   │
│ ℟. Ad Deum, qui lætíficat juventútem meam. ┃ ℟. Du Dieu qui réjouit ma jeunesse. │
│                                            ┃                                     │
│                                            ┃                         ▌ ▌ ▌       │
└────────────────────────────────────────────────────────────────────────────────┘
(r) = rubrique, en rouge et en italique. ┃ = gouttière, ou pli d'un pliable en posture livre.
Les blocs latins et français sont alignés au même niveau sur les deux pages (contrainte pour 06 et 03).
```

**E. Menu du lecteur, au tap**

```
┌──────────────────────────────────────────┐
│ ←  Imitation · Livre I, ch. III    ⌕  ⋮  │  barre haute + barres système
│░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░│
│░  (la page reste visible, voile 8 %)   ░░│
│░                                       ░░│
│░                                       ░░│
│░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░│
│        ┌──────────────────────────┐      │
│        │ ↩ Revenir à la page 112  │      │  après un saut seulement
│        └──────────────────────────┘      │
│  p. 14 / 286 · Livre I, ch. III          │
│  ●━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━  │  curseur « Aller à »
│ ┌──────────────────────────────────────┐ │
│ │   ≡        ⚑       Aa      ◐      ◷  │ │  barre d'outils flottante
│ │ Sommaire Rubans  Texte  Thème Méditer│ │
│ └──────────────────────────────────────┘ │
└──────────────────────────────────────────┘
Feuille « Aa » : police · taille (A− ●── A+) · interligne · marges · justification ·
césure · lettrines · thème (4 pastilles) · luminosité · animation de page · verrouiller la rotation
Menu ⋮ : lire à voix haute · rechercher dans le livre · citer · informations
```

**F. Séance de méditation**

```
Préparation (feuille)                        En séance
┌──────────────────────────────────────┐     ┌──────────────────────────────────────┐
│ Temps de prière                      │     │                                      │
│ Durée   (5)(10)[15](20)(30)(…)       │     │                                      │
│ Cloche  Début ☑  Fin ☑  Interv. ☐    │     │   « Car il y a beaucoup d'appelés,   │
│         Cloche douce ▾   ▶ Tester    │     │          mais peu d'élus. »          │
│ Ne pas déranger               [■ ]   │     │               Mt 22,14               │
│   Alarmes et contacts favoris        │     │                                      │
│   autorisés · Modifier ›             │     │                  ◯                   │
│ Écran   allumé, très sombre   [■ ]   │     │                11:42                 │  masquable
│ Niveaux de gris (Android 15+) [  ]   │     │                                      │
│ Texte   Mt 22,1-14 (passage ouvert)  │     │                                      │
│                                      │     │  Toucher pour les commandes          │  disparaît
│          [   Commencer   ]           │     │                                      │  après 3 s
└──────────────────────────────────────┘     └──────────────────────────────────────┘
                                             fond #000 · encre ambre · luminosité de
Commandes au tap :                           fenêtre 10 % · NPD actif · écran allumé
[ ❚❚ Pause ]  [ +5 min ]  [ Terminer ]
```

**G. Mes signets**

```
┌──────────────────────────────────────────┐
│ Signets                            ⌕  ⋮  │
│ [ Rubans ]   Images pieuses    Notes     │
├──────────────────────────────────────────┤
│ ⋎V Vert · Missel romain 1962             │  ⋎ = queue d'aronde
│    XXᵉ dim. après la Pentecôte · p. 412  │
│    « Omnia, quæ fecísti nobis… »      ›  │
│ ──────────────────────────────────────── │
│ ▬R Rouge · Ordinaire de la messe         │  ▬ = coupe droite
│    Canon · p. 34                      ›  │
│ ──────────────────────────────────────── │
│ ▼Vi Violet · Exercices spirituels        │  ▼ = pointe
│    1ʳᵉ semaine, 3ᵉ exercice · p. 58   ›  │
│ ──────────────────────────────────────── │
│ ⩛B Blanc · libre                         │
│    Poser ce ruban dans un livre…      +  │
│ ──────────────────────────────────────── │
│ ◖Ro Rose · libre                      +  │
│ ◣N Noir · Office des défunts · p. 610 ›  │
├──────────────────────────────────────────┤
│ ☼ Aujourd'hui  ▤ Biblioth.  ⚑ Signets  ⌕ │
└──────────────────────────────────────────┘
Onglet « Images pieuses » : grille de vignettes, chacune avec le livre et la page où elle est glissée.
Le nombre de rubans (six fixes, ou un par livre) relève de l'agent 09.
```

**H. Réglages**

```
┌──────────────────────────────────────────┐
│ ←  Réglages                              │
├──────────────────────────────────────────┤
│ APPARENCE                                │
│ Thème   (Auto)(Clair)(Sépia)(Sombre)(Nuit)│
│ Nuit automatique     21:30 → 06:30    ›  │
│ Teinte liturgique de l'interface  [■ ]   │
│ Contraste élevé            Système ▾     │
│ LECTURE                                  │
│ Police                     Literata   ›  │
│ Taille du texte    A ─────●──── A   18   │
│ Interligne, marges, justification     ›  │
│ Lettrines · Césure                [■ ]   │
│ Rubriques en italique aussi       [■ ]   │
│ Tourner la page            Courbure ▾    │
│ Double page en paysage     Auto ▾        │
│ Touches de volume pour tourner    [  ]   │
│ LITURGIE                                 │
│ Formes        Ordinaire + Extraord. ›    │
│ Calendrier propre          France ▾      │
│ MÉDITATION                               │
│ Durée, cloches, Ne pas déranger       ›  │
│ ACCESSIBILITÉ                            │
│ Réduire les animations     Système ▾     │
│ Défilement continu (lecteur d'écran)  ›  │
│ CONTENUS · CONFIDENTIALITÉ · À PROPOS ›  │
└──────────────────────────────────────────┘
```

### 2.14 Points d'interface avec les autres agents

| Agent | Ce que l'UX attend | Ce que l'UX fournit |
|---|---|---|
| 01 Clairval | Métadonnées des lettres (mois, titre, langue) ; « lettre du mois » | Carte d'Aujourd'hui, rayon de la Bibliothèque |
| 02 Ignace | Structure de la retraite (jours, exercices, durées) | Mode méditation réutilisable (minuteur, cloches, NPD) ; point d'entrée « Retraite » |
| 03 Missel | Choix entre messe **assemblée** ou **livre** (D7) ; mise en page bilingue (D6) ; balisage des rubriques | Jetons de rubrique, ℣ et ℟, échelle du lecteur |
| 04 Bible / Imitation | Analyse des références (« Jn 3,16 ») pour « Aller à » et Recherche ; découpage en chapitres pour la méditation du jour | Champs de saisie de référence, cartes |
| 05 Calendrier | Contrat de l'écran Aujourd'hui (§ 2.3), énumération des couleurs, alternatives, drapeau « solennité » | Jetons `lit.*` |
| 06 Moteur de lecture | Pagination « ancre d'abord », **nœuds d'accessibilité par paragraphe**, repli de police (℣ ℟ ✠), césure FR et LA, `LocaleList` latine, critères de la double page | Échelle typographique, mesure, règle de l'ancre |
| 07 Tourne-page et papier | Respect de `motion.*` et du mode réduit ; textures coupées en Sombre, Nuit et contraste élevé | Jetons de mouvement, couleurs de tranche |
| 09 Signets et images pieuses | Formes d'extrémité et initiales des rubans (daltonisme) ; liserés blanc et noir | Couleurs de ruban par thème, maquette G |
| 10 Architecture | Navigation 3 et scènes adaptatives ; `configChanges` ; DataStore des réglages ; FGS `mediaPlayback` ; **minSdk d'au moins 29** pour unifier le NPD | Spécification du mode NPD et du parcours de méditation |
| 11 Pipeline de contenu | Normalisation typographique (U+202F), sous-ensembles de polices, tailles des paquets | Liste des glyphes requis |
| 12 Juridique | Licences OFL (Literata, EB Garamond et Initials, Atkinson) ; licence du **son de cloche** ; Material Symbols (Apache 2.0) | Inventaire des ressources |
| 13 Benchmark | Conventions de lecteurs comparables (« revenir à », zones de tap, thèmes) | — |
| 14 Prières et extensions | Points d'entrée futurs (rosaire, chemin de croix) dans la Bibliothèque ; rappels éventuels (Angélus) | Règle « aucune notification par défaut » |

---

## 3. Recommandations

### MVP

1. **Navigation** : `NavigationSuiteScaffold` avec quatre entrées (barre du bas sur téléphone en portrait, rail ailleurs). Réglages par ⚙. Lecteur en plein écran immersif. Retour prédictif. Puce « ↩ Revenir ».
2. **Écran Aujourd'hui** : contrat de données avec l'agent 05, bandeau teinté, flèches de jour, bascule de forme, cinq cartes et bouton *Temps de prière*.
3. **Bibliothèque en liste** avec « Continuer ». **Signets** : onglet Rubans. **Recherche** plein texte et par référence (avec les agents 04 et 06).
4. **Design system v1** :
   - M3 1.4.0 habillé : `ColorScheme` sur mesure, sans couleur dynamique ; `Typography` remappée ; formes de 4, 8 et 16 dp ; aucune ombre ;
   - jetons `paper`, `ink`, `lit`, `motion` **définis hors de M3**, pour survivre à la migration vers la 1.5 ;
   - thèmes **Clair, Sépia, Sombre et Nuit**, plus *Auto*.
5. **Typographie** : Literata (lecture), police système (interface), EB Garamond Initials (lettrines), police de secours « Liturgica » (℣ ℟ ✠). Rubriques en rouge **et en italique**.
6. **Lecteur** : zones de tap, glissement, chrome au tap sans délai, feuille Aa, curseur « Aller à », verrouillage de rotation sur téléphone, **ancre de position** stable à la rotation.
7. **Adaptatif** : simple ou double page selon la géométrie (§ 2.11), gouttière sur le pli en posture livre, rail en hauteur Compact.
8. **Mode méditation au premier plan** : `keepScreenOn`, luminosité de fenêtre, minuteur, cloches de début et de fin en `USAGE_MEDIA`, **règle NPD « Temps de prière »** (création unique, activation et désactivation, filets de sécurité), parcours d'autorisation non bloquant.
9. **Accessibilité** : 200 % de police, TalkBack (nœuds par paragraphe et actions personnalisées), contrastes vérifiés, mode « Réduire les animations » qui suit le système, rubans avec forme et initiale.
10. **Premier lancement** en trois écrans, sans permission.

### V1

1. Mode méditation **écran éteint** : FGS `mediaPlayback` démarré au tap, avec échéance fiable (à valider sur appareil, Doze compris).
2. Niveaux de gris : système via `ZenDeviceEffects` (Android 15 et plus) et filtre dans l'app.
3. **Mode « À la messe »** : la même règle NPD sans minuteur, écran allumé dans le missel, luminosité réduite.
4. Calendrier du mois (pastilles de couleur, fêtes), Notes et surlignages (agent 09), onglet Images pieuses.
5. Variante **contraste élevé** automatique (`getContrast()`), et police Atkinson Hyperlegible Next en option.
6. Migration vers **Material3 1.5 stable** : FloatingToolbar du lecteur, `MotionScheme` standard personnalisé.
7. Liste-détail sur tablette (Bibliothèque, Signets), rail étendu en Large et XL.
8. Lecture à voix haute dans le chrome (avec l'agent 06), marquage du latin.
9. Défilement continu proposé aux utilisateurs de TalkBack.

### Plus tard

1. Posture table sur pliable (texte en haut, commandes en bas).
2. Thème « Nuit rouge » ; thèmes saisonniers facultatifs.
3. Widget « Aujourd'hui » (Glance) ; raccourcis d'app (« Messe du jour », « Temps de prière »).
4. Pincement pour la taille du texte ; retour haptique au tourne-page.
5. Rappels facultatifs (Angélus, examen du soir), désactivés par défaut et soumis à `POST_NOTIFICATIONS`.
6. Multilingue de l'interface (les lettres de Clairval existent en plusieurs langues).

---

## 4. Décisions à soumettre à l'utilisateur

| # | Question | Options | Recommandation |
|---|---|---|---|
| **D1** | Combien d'entrées de navigation ? | (a) 4 : Aujourd'hui, Bibliothèque, Signets, Recherche · (b) 3, avec la Recherche en icône · (c) 5, avec *Prières* | **(a)** |
| **D2** | Socle visuel ? | (a) M3 habillé, puis 1.5 quand elle sera stable · (b) M3 Expressive dès maintenant (bêta) · (c) entièrement sur mesure | **(a)** |
| **D3** | La couleur liturgique du jour teinte-t-elle l'interface ? | (a) accent discret (bandeau, indicateurs) · (b) aucune teinte · (c) thème complet du jour | **(a)**, désactivable |
| **D4** | Police de lecture par défaut ? | (a) Literata (écran, sobre) · (b) EB Garamond (« missel ») | **(a)** pour le confort, (b) en option ; lettrines EB Garamond dans les deux cas |
| **D5** | Style du thème *Nuit* ? | (a) ambre sur noir · (b) rouge sur noir · (c) simple sombre atténué | **(a)**, (b) plus tard |
| **D6** | Missel bilingue : mise en page ? | (a) paysage : latin à gauche, français à droite ; portrait : blocs alternés · (b) deux colonnes sur chaque page · (c) une langue à la fois, avec bascule | **(a)**, à confirmer avec les agents 03 et 06 |
| **D7** | « Messe du jour » : assemblée ou livre ? | (a) séquence linéaire assemblée (ordinaire et propre), plus fluide · (b) « mode livre » avec rubans entre ordinaire et propre, plus proche du papier · (c) les deux, (a) par défaut | **(c)** |
| **D8** | Que laisse passer le NPD de méditation ? | (a) alarmes, contacts favoris et appels répétés · (b) alarmes seulement · (c) rien | **(a)** (sécurité) ; modifiable dans les Modes d'Android |
| **D9** | Statistiques et séries de prière ? | (a) aucune · (b) historique privé facultatif | **(a)** |
| **D10** | minSdk (avec l'agent 10) | 29 ou plus, ce qui unifie le NPD par les règles, ou moins avec l'ancien filtre | **29 ou plus** |

---

## 5. Risques et inconnues

| Risque | Gravité | Atténuation |
|---|---|---|
| **TalkBack et texte paginé dessiné à la main** : arbre d'accessibilité vide, lecture continue impossible | Élevée | Exigence formelle envers l'agent 06 (nœuds par paragraphe, actions personnalisées) ; défilement continu ; tests TalkBack dès le premier prototype |
| **Cloche muette** écran éteint (Android 17, échec silencieux) | Élevée pour la V1 | FGS `mediaPlayback` démarré au tap ; test avec `adb shell cmd audio set-enable-hardening throw` ; MVP au premier plan |
| **NPD resté actif** après un plantage | Moyenne | Règle plutôt que filtre global, FGS, nettoyage au démarrage, mode visible dans les réglages rapides |
| Variations des constructeurs (One UI, HyperOS) pour les Modes et l'écran d'autorisation (« l'activité peut ne pas exister ») | Moyenne | Garde `ActivityNotFoundException`, texte d'aide générique, tests sur téléphone physique |
| `areAutomaticZenRulesUserManaged()` vrai : `updateAutomaticZenRule` peut échouer | Faible | Ne pas réécrire la règle ; renvoyer vers `ACTION_AUTOMATIC_ZEN_RULE_SETTINGS` |
| Material3 **1.5** encore en bêta ; churn des API adaptatives (V2, postures cachées) | Moyenne | Jetons hors de M3 ; `FoldingFeature` direct ; migration planifiée |
| **Dérive ou latence de position** lors d'une rotation sur un téléphone d'entrée de gamme | Moyenne | Pagination « ancre d'abord », ancre immuable, affichage maintenu pendant le calcul |
| Daltonisme : rubans rouge et vert indiscernables ; rubriques reconnues à la seule teinte | Moyenne | Formes et initiales des rubans, rubriques en italique |
| Glyphes manquants (✠ dans toutes les polices, ℣ et ℟ hors EB Garamond, U+202F) | Moyenne | Police « Liturgica », normalisation par le pipeline |
| Poids des polices (environ 4 Mo bruts) | Faible | Sous-ensembles, police système pour l'interface |
| Conflit entre le glissement de page et le geste retour | Faible | Pas d'exclusion de geste ; zones de tap ; glissement démarré hors des bords |
| Double page sur téléphone en paysage, trop courte (environ 13 lignes) | Faible | Critères géométriques, réglage *Auto, Toujours, Jamais* |
| Prise en charge du latin par les moteurs de synthèse vocale | Inconnue | Repli sur l'italien ; à tester (agent 06) |
| Couleurs propres à la forme extraordinaire, bleu marial et or | Inconnue | À trancher par l'agent 05 ; énumération extensible |

**Limite de l'enquête.** Le quota de recherches web de la session a été atteint en fin de travail. Les lignes directrices M3 Expressive publiées sur m3.material.io n'ont donc pas pu être relues directement : l'état des API Expressive repose sur les notes de version officielles de Compose Material3. Les autres affirmations sur les API ont été vérifiées dans les pages de référence developer.android.com, mises à jour entre août et octobre 2026.

---

## 6. Sources

**Android : Ne pas déranger, Modes et autorisations**
- Changements de comportement d'Android 15 (NPD) : https://developer.android.com/about/versions/15/behavior-changes-15
- `AutomaticZenRule` (types, dont `TYPE_TRANSIT` en API 37) : https://developer.android.com/reference/android/app/AutomaticZenRule
- `AutomaticZenRule.Builder` : https://developer.android.com/reference/android/app/AutomaticZenRule.Builder
- `ZenDeviceEffects.Builder` : https://developer.android.com/reference/android/service/notification/ZenDeviceEffects.Builder
- `ZenPolicy.Builder` : https://developer.android.com/reference/android/service/notification/ZenPolicy.Builder
- `Condition` (`SOURCE_USER_ACTION`, API 35) : https://developer.android.com/reference/android/service/notification/Condition
- `NotificationManager` (`setInterruptionFilter`, `setAutomaticZenRuleState`, `getAutomaticZenRuleState`, `areAutomaticZenRulesUserManaged`) : https://developer.android.com/reference/android/app/NotificationManager
- `Settings` (`ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS`, `ACTION_AUTOMATIC_ZEN_RULE_SETTINGS`) : https://developer.android.com/reference/android/provider/Settings
- Commit AOSP « Respect user customization for implicit Zen rules » : https://android.googlesource.com/platform/frameworks/base/+/4f4ad6bf07b2

**Android 16 et 17, arrière-plan, audio**
- Changements de comportement d'Android 16 : https://developer.android.com/about/versions/16/behavior-changes-16
- Changements de comportement d'Android 17 : https://developer.android.com/about/versions/17/behavior-changes-17
- Durcissement de l'audio en arrière-plan (Android 17) : https://developer.android.com/about/versions/17/changes/bg-audio
- Redimensionnement et orientation en Android 17 : https://developer.android.com/blog/posts/prepare-your-app-for-the-resizability-and-orientation-changes-in-android-17
- Types de services de premier plan : https://developer.android.com/develop/background-work/services/fgs/service-types
- Alarmes exactes (`SCHEDULE_EXACT_ALARM`, `USE_EXACT_ALARM`, `OnAlarmListener`) : https://developer.android.com/develop/background-work/services/alarms/schedule

**Compose, Material 3 et adaptatif**
- Correspondance des BOM (2026.09.00 donne UI 1.12.1 et M3 1.4.0) : https://developer.android.com/develop/ui/compose/bom/bom-mapping
- Notes de version Compose Material3 (1.4.0, 1.5.0-alpha15, alpha18, beta01) : https://developer.android.com/jetpack/androidx/releases/compose-material3
- Notes de version Material3 Adaptive (1.3.0, V2) : https://developer.android.com/jetpack/androidx/releases/compose-material3-adaptive
- Paquet `androidx.compose.material3.adaptive` (`currentWindowAdaptiveInfoV2`, `Posture`) : https://developer.android.com/reference/kotlin/androidx/compose/material3/adaptive/package-summary
- WindowManager 1.5.1 : https://developer.android.com/jetpack/androidx/releases/window
- Classes de taille de fenêtre : https://developer.android.com/develop/ui/compose/layouts/adaptive/use-window-size-classes
- Applications compatibles avec les pliables (`FoldingFeature`) : https://developer.android.com/develop/ui/compose/layouts/adaptive/foldables/make-your-app-fold-aware
- `Modifier.keepScreenOn` (Compose UI 1.9.0) : https://developer.android.com/reference/kotlin/androidx/compose/ui/keepScreenOn.modifier
- `MotionDurationScale` : https://developer.android.com/reference/kotlin/androidx/compose/ui/MotionDurationScale
- Discussion des mainteneurs Compose sur la coupure des animations (source secondaire) : https://slack-chats.kotlinlang.org/t/2913570/is-there-a-way-to-turn-off-animations-for-a-compose-hierarch
- Sémantique et accessibilité en Compose : https://developer.android.com/develop/ui/compose/accessibility/semantics
- Navigation 3 stable : https://developer.android.com/blog/posts/jetpack-navigation-3-is-stable

**Fenêtre, gestes, affichage, accessibilité**
- Mode immersif : https://developer.android.com/develop/ui/views/layout/immersive
- Navigation par gestes et conflits avec le retour : https://developer.android.com/develop/ui/views/touch-and-input/gestures/gesturenav
- `View.setSystemGestureExclusionRects` (limite de 200 dp) : https://developer.android.com/reference/android/view/View
- `WindowManager.LayoutParams.screenBrightness` et `screenBrightnessUnit` : https://developer.android.com/reference/android/view/WindowManager.LayoutParams
- `UiModeManager.getContrast()` (API 34) : https://developer.android.com/reference/android/app/UiModeManager
- `AccessibilityManager` (`isHighContrastTextEnabled` en API 36, `getRecommendedTimeoutMillis`) : https://developer.android.com/reference/android/view/accessibility/AccessibilityManager
- Mise à l'échelle non linéaire des polices jusqu'à 200 % (Android 14) : https://developer.android.com/about/versions/14/features
- WCAG 2.2 (critères 1.4.1, 1.4.3, 1.4.11, 2.2.1, 2.3.3) : https://www.w3.org/TR/WCAG22/
- Machado, Oliveira et Fernandes (2009), simulation des déficiences de la vision des couleurs : https://www.inf.ufrgs.br/~oliveira/pubs_files/CVD_Simulation/CVD_Simulation.html

**Liturgie**
- *Présentation générale du Missel romain*, n° 345 à 347 (couleurs liturgiques), texte français du Saint-Siège : https://www.vatican.va/roman_curia/congregations/ccdds/documents/rc_con_ccdds_doc_20030317_ordinamento-messale_fr.html

**Polices** (licence et axes vérifiés dans les fichiers `METADATA.pb` et `OFL.txt`, couverture des glyphes vérifiée avec fontTools)
- Literata : https://github.com/google/fonts/tree/main/ofl/literata ; https://www.type-together.com/literata-3
- EB Garamond (Google Fonts) : https://github.com/google/fonts/tree/main/ofl/ebgaramond
- EB Garamond de Georg Duffner, Initials (OFL) : https://github.com/georgd/EB-Garamond
- Source Serif 4 : https://github.com/google/fonts/tree/main/ofl/sourceserif4
- Crimson Pro : https://github.com/google/fonts/tree/main/ofl/crimsonpro
- Atkinson Hyperlegible Next : https://github.com/google/fonts/tree/main/ofl/atkinsonhyperlegiblenext
