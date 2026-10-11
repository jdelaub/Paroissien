# ADR 007 — Polices

## Statut
Proposé, 2026-10-11. La police et le corps par défaut sont confirmés à la porte A.

## Contexte
Les livres liturgiques demandent ℣ ℟ ✠ et les voyelles accentuées du latin (á, ǽ) ; le français demande l'espace fine insécable U+202F, que la chaîne place dans le texte canonique (ADR 004). Aucune police de Google Fonts n'a tout : EB Garamond a ℣ et ℟ mais ni ✠, ni ✝, ni U+202F ; Literata n'a ni ℣, ni ℟, ni ✠, ni U+202F ; Junicode 2 a tout (cmap vérifiées, 15 §3). L'app est hors ligne : les polices sont embarquées.

## Décision
- **Lecture : EB Garamond** (variable, romain et italique, OFL) par défaut pour tous les livres. **Literata** (OFL) en option de lecture. L'interface utilise la police du système.
- **Secours : un sous-ensemble de Junicode 2** (OFL), produit par `pyftsubset` dans la chaîne, commité avec sa commande : ✠ ✝ ⁂ ℣ ℟ ℞ et U+202F (℣ et ℟ servent à Literata). Quelques Ko.
- **Mécanisme** : `Typeface.CustomFallbackBuilder` (API 29) chaîne police de lecture → sous-ensemble Junicode → système ; la `Typeface` obtenue passe à Compose par `FontFamily(Typeface)`. On ne modifie pas la `cmap` d'EB Garamond, et l'on ne remplace pas U+202F par U+2060 U+2009 U+2060 (U+2060 est interdit dans le texte canonique).
- **Caractères garantis** (aucun carré vide toléré) :

  | Groupe | Caractères |
  |---|---|
  | Base | A-Z, a-z, 0-9, ponctuation ASCII |
  | Français | à â ä ç é è ê ë î ï ô ö ù û ü ÿ œ æ et leurs capitales ; « » ’ ‘ “ ” – — … |
  | Latin liturgique | á é í ó ú ý ǽ Á É Í Ó Ú Ý Ǽ ; æ œ Æ Œ ; ë |
  | Espaces | U+0020, U+00A0, U+2009, U+202F |
  | Signes | ℣ ℟ ℞ ✠ ✝ † ‡ * ⁂ ❦ ❧ ☙ ¶ § |

  Un test de la chaîne (fontTools) vérifie que chaque caractère garanti est couvert par les deux chaînes de lecture (EB Garamond puis Junicode ; Literata puis Junicode), et que **tout caractère présent dans un paquet** figure dans cette liste. Sinon, la CI échoue.
- **Fonctions OpenType** : `smcp` (suite de la lettrine, noms de fêtes, intitulés), `c2sc` (sigles), `onum` (chiffres du texte courant), `liga`. Ni `hist` ni `dlig`.
- **Corps** : EB Garamond à 20 sp par défaut (son œil est petit), interligne de 1,45 ; Literata à 18 sp ; réglable de 14 à 32 sp, multiplié par l'échelle du système (jusqu'à 200 %).
- **Rubriques** : rouge **et** italique, à 0,85 em ; ℣ ℟ ✠ en rouge.
- **Lettrines** :
  - MVP : initiale EB Garamond rouge sur 2 ou 3 lignes ; le haut s'aligne sur la hauteur de capitale de la ligne 1, le bas sur la ligne de base de la ligne N ; l'accent est retiré à l'affichage seulement (*Ádjutórium* → A) ; la suite du mot passe en petites capitales ;
  - V1 : lettrines ornées EB Garamond Initials (couches F1 et F2, Duffner, OFL).
- **Embarquement** : fichiers dans `core:designsystem` (`res/font`) ; EB Garamond et Literata sous-ensemblés (latin et ponctuation, fonctions OpenType conservées) ; pas de *Downloadable Fonts* ; chaque police et le texte de l'OFL figurent dans l'écran « Licences ». Budget de toutes les polices : 3 Mo au plus.
- **V1** : Atkinson Hyperlegible Next en option « grande lisibilité », avec la même chaîne de secours.

## Conséquences
- (+) Un seul petit fichier de secours sert toutes les polices de lecture, sans retoucher les polices amont.
- (+) La couverture des glyphes est vérifiée en CI, au lieu d'être découverte sur l'appareil.
- (−) Les signes de secours (✠, U+202F) n'ont pas exactement le dessin d'EB Garamond.
- (−) Le texte passe par une `Typeface` Android plutôt que par les familles de polices de Compose : graisse, italique et variations sont à valider.

## Points ouverts
- Spike 1a : vérifier que Compose respecte la chaîne `CustomFallbackBuilder` pour la mesure, la césure, la justification, la graisse et l'italique. Repli : le moteur pose `SpanStyle(fontFamily = junicode)` sur les caractères garantis absents de la police de lecture.
- Version de Junicode à épingler (v2.226 du 20/06/2026) ; la couverture est à revérifier sur le fichier téléchargé.
- Police et corps par défaut : porte A.

## Références
PLAN §1 (Glyphes), §5.2 (porte A), §5.3 (Rubriques) ; 15 m2, §3 (glyphes) ; 07 §2.10 ; 08 §2.8 ; 12 §2.2.7, §3 (5).
