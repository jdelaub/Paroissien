# ADR 004 — Texte canonique, identifiants et ancres

## Statut
Proposé, 2026-10-11. Validé par les vecteurs partagés et le paquet golden en phase 2.

## Contexte
Rubans, images pieuses, notes, recherche et synthèse vocale pointent dans le texte. La pagination change avec la police et l'orientation, et les textes seront corrigés : une ancre ne peut reposer ni sur un numéro de page, ni sur un texte qui varie avec la césure. Le rapport 07 proposait d'insérer des U+00AD dans le texte stocké ; « Dó­mi­nus » deviendrait alors trois jetons pour FTS5, et chaque révision des motifs changerait la `rev` de tous les blocs latins.

## Décision
- **Texte canonique** d'un bloc dans une langue = `plainText` de ses inlines (rapport 11 §3.2.3) : concaténation des chaînes ; un `sym` compte pour son caractère (℣ U+2123, ℟ U+211F, ✠ U+2720, `*`, †), `br` pour `\n`, `note` et `num` pour rien. Il est en **NFC** et ne contient **ni U+00AD, ni U+200B, ni U+2060, ni U+FEFF**, ni aucun caractère de contrôle autre que `\n` : `validate` échoue sinon. La normalisation typographique de la chaîne (’, U+202F) en fait partie.
- Ce texte sert à la `rev`, aux décalages, à l'index FTS (après pliage), à la synthèse vocale et au presse-papiers. Python et Kotlin le calculent à l'identique, testés sur `content/schema/plaintext-vectors.json`.
- **Césure** : jamais dans le texte canonique. Latin : motifs `la-x-liturgic` (MIT, version amont du 04/02/2019), algorithme de Liang en Kotlin, appliqué **à l'exécution** sur la seule chaîne affichée ; si le spike 1a le trouve trop lent, les points de césure sont stockés à part (décalages par bloc et par langue, hors `rev`). Français : motifs du système (`Hyphens.Auto`, locale `fr`).
- **Identifiants**, stables par construction :
  - publication `^[a-z0-9]+(\.[a-z0-9-]+)+$` (`bible.crampon`) ; bloc et division `^[a-z0-9][a-z0-9._-]{0,63}$`, uniques dans la publication ;
  - clé naturelle quand elle existe : `jhn.3.16` dans la versification **propre à chaque édition**, `ex.230`, `sancti.08-15.introitus` ; sinon `p` suivi de 6 caractères base32 de Crockford, tirés une seule fois puis commités ;
  - un identifiant publié n'est jamais réutilisé. S'il disparaît, il passe dans `id_alias` (successeur, ou `null` pour une pierre tombale) ; la CI bloque toute disparition sans alias.
- **`rev`** = les 16 premiers caractères hexadécimaux du SHA-256 du texte canonique en UTF-8, **par bloc et par langue** (`block_text.rev`). La `revision` d'une publication augmente dès qu'une `rev` change.
- **Ancre**, jamais un numéro de page ni un paquet :
  `Anchor = { pub, block, lang, start, end?, rev, quote: { exact, prefix, suffix }, progression }`
  - `start` et `end` en **points de code** du texte canonique (`offsetByCodePoints` en Kotlin) ; sans `end`, l'ancre est ponctuelle (ruban, position de lecture, image pieuse) ;
  - `prefix` et `suffix` : 32 points de code ; pour une ancre ponctuelle, `exact` = les 32 points de code qui suivent ;
  - `progression` : fraction des caractères de la publication.
- **Ré-ancrage**, en tâche de fond au premier démarrage qui suit un changement de `packageVersion`, jamais à l'affichage :
  1. bloc présent, `rev` identique → décalages conservés (OK) ;
  2. bloc présent, `rev` différente → `exact` cherché près de l'ancien décalage avec `prefix` et `suffix`, puis recherche floue (Bitap, diff-match-patch) → OK ou APPROX ;
  3. bloc absent → `id_alias`, suivi en chaîne, puis étape 2 ; pierre tombale → étape 4 ;
  4. recherche floue de la citation dans l'unité de lecture, puis dans la publication, bornée à `progression` ± 5 % ;
  5. sinon ORPHELIN : rattachée à `progression`, rangée dans « Annotations à replacer », **jamais supprimée**.

  Les seuils sont calés sur les ancres « avant et après correction » du paquet golden. Chaque passage est journalisé localement. Le ruban automatique n'a pas d'ancre persistée.
- **Pliage de recherche** `fold(s)`, identique en Python (`fold.py`) et en Kotlin, appliqué au texte indexé (`search_doc`) comme à la requête :
  0. retirer U+00AD, U+200B et U+2060 ;
  1. NFKC ;
  2. minuscules simples : `str.lower()` en Python, `lowercase(Locale.ROOT)` en Kotlin, jamais `casefold` ;
  3. NFD, puis suppression des marques combinantes (catégorie Mn) ;
  4. œ→oe, æ→ae, ĳ→ij ;
  5. j→i, v→u, **dans toutes les langues** : la langue d'une requête est inconnue ;
  6. le reste, ponctuation comprise, est conservé ; `unicode61` découpe ensuite.

  Vecteur partagé : **`content/schema/fold-vectors.json`**, lu par pytest et par JUnit. Il contient au moins `Dó­mi­nus` (avec U+00AD) → `dominus`, `Cœur` → `coeur`, `l’Église` → `l’eglise`, `Jésus` → `iesus`, `prǽdica` → `praedica`, `ﬁdèle` → `fidele`, `vivere` → `uiuere`. La CI échoue si un caractère présent dans les données n'apparaît dans aucun vecteur.
- **Requête** : construite à partir des jetons pliés, chacun entre guillemets, `*` sur le dernier ; la saisie brute ne va jamais dans `MATCH`. Le surlignage se fait sur le texte original, par retokenisation et pliage jeton par jeton.

## Conséquences
- (+) Changer les motifs de césure ne touche ni les ancres, ni l'index, ni les `rev`.
- (+) Les annotations survivent aux corrections ; aucune ne disparaît sans être montrée.
- (−) Le moteur tient une correspondance entre la chaîne affichée (césures, lettrine) et le texte canonique.
- (−) Le pliage j/v crée quelques faux positifs en français, à surveiller dans les requêtes types.
- (−) Deux implémentations (Python et Kotlin) que seule la CI garde alignées.

## Points ouverts
- Nom divin du Crampon (« Yahweh » tel qu'imprimé, ou variante « le Seigneur » avec ses propres ancres) : avant la phase 2.
- Coût de Liang à l'exécution et comportement de minikin avec les locales par segment : spike 1a.

## Références
11 §3.2.1 à §3.2.5, §3.7 ; 06 §3.5 ; 09 §2.8, §3.5.2 ; 07 §2.10 (Césure) ; 15 M3, m4, §5.4, §5.8 ; PLAN §1, §2 (principes 2 et 3).
