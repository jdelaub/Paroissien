# ADR 005 — Moteur de lecture et porte A

## Statut
Accepté, 2026-10-11. Les résultats de la porte A seront consignés à la suite de cet ADR.

## Contexte
L'expérience de livre (page qui tourne, typographie de missel, double page) est la raison d'être du projet. Le rapport 06 recommande un moteur natif Compose, plus coûteux à écrire que Readium et non éprouvé. Les seuils proposés divergeaient (P95 < 8,3 ms à 120 Hz en 07, P90 < 16 ms à 60 Hz en 10, sans règle de repli). Il faut des critères chiffrés, mesurés sur le Flip6 à 120 Hz, et une règle de décision fixée d'avance.

## Décision
- **Moteur natif « Codex »** dans `core:reading` :
  - documents en blocs (ADR 003) → paginateur glouton hors du fil principal (veuves et orphelines de 2 lignes, titre lié à la suite) → pages Compose ;
  - pages n-1, n et n+1 (± 2 en double page) prêtes avant tout geste ; aucune mesure de texte pendant un tour ;
  - encre dessinée sur fond transparent ; le tourne-page dessine papier, reliure et tranche, et fige la page pendant le tour ;
  - pli cylindrique AGSL à partir de l'API 33 (code de Mekuri repris sous MIT avec sa notice, libellés d'accessibilité traduits) ; glissement de l'API 29 à 32 ;
  - clé de pagination = empreinte de (`rev`, version du moteur, police, corps, interligne, marges, taille de page, césure, justification, échelle système) ; sauts de page en cache ; folios par section ; progression globale en caractères.
- **Spike** en deux temps, écrit directement dans `core:reading`, avec un écran de démonstration en debug (pas de prototype jetable) :
  - **1a « feuille et encre »** (2 à 3 semaines) : pli AGSL en portrait, papier, thèmes, polices, césure fr et la. Mini-porte : critères « Fluidité » et « Typographie » ci-dessous ;
  - **1b « livre »** (2 à 3 semaines) : paginateur, lettrine, rubriques, double page en paysage et en posture livre, TalkBack, variante de gestes (ADR 006).
- **Conditions de mesure** : Flip6, écran principal à 120 Hz, APK `domainePublicBenchmark` construit par la CI (R8 actif, non débogable ; ADR 002), économie d'énergie coupée.
- **Critères de la porte A** :

  | Domaine | Critère | Mesure |
  |---|---|---|
  | Fluidité du pli | P95 de la durée d'image ≤ 8,3 ms ; moins de 1 % d'images manquées sur 100 tours (50 lents, 50 lancés), en portrait et en double page | `dumpsys gfxinfo app.paroissien.bench framestats`, JankStats |
  | Repos et mémoire | 0 recomposition de page pendant un tour ; pic < 40 Mo pendant le tour ; PSS après 200 tours ≤ PSS après 10 tours + 5 Mo | compteur interne journalisé ; `dumpsys meminfo` |
  | Pagination | un chapitre de l'Imitation < 50 ms (médiane) ; le livre I < 1,5 s hors fil principal ; réouverture avec cache < 200 ms jusqu'à la première page | `Trace.beginSection`, Perfetto |
  | Ancre | premier bloc visible et décalage identiques après 10 rotations, 3 changements de corps et 5 passages entre posture livre et écran à plat | test instrumenté ou journal |
  | Typographie | 0 glyphe manquant sur les caractères garantis (ADR 007) ; 50 mots latins de contrôle coupés comme attendu ; lettrine alignée sur 2 à 3 lignes à 100, 130 et 200 % | captures, liste de contrôle |
  | Double page | parité respectée ; la feuille ne traverse pas la reliure ; en posture livre, la gouttière épouse la charnière ; position conservée entre portrait et paysage | captures, vidéo |
  | TalkBack | ordre de lecture correct ; actions « Page suivante » et « Page précédente » en français ; mode défilant proposé | test manuel scénarisé |
  | Gestes | 0 retour système involontaire sur 50 saisies de coin ; zones de tap 25/50/25 | test manuel compté |

- **Règle de décision** :
  - pli en échec → moteur natif avec tourne-page par **glissement** ;
  - pagination ou typographie en échec **après une semaine de corrections** → **Readium** (Kotlin Toolkit) : la chaîne produit alors de l'EPUB à partir du JSON canonique, et les ancres se convertissent en Locator Readium ;
  - TalkBack ou gestes en échec → correction avant B1, sans changer de moteur ;
  - sinon → natif confirmé.
- **Se décident aussi à la porte A** : la police et le corps par défaut (ADR 007), le seuil du mode Auto de la double page sur le Flip6, la variante de gestes (ADR 006).
- Avant B, mêmes critères sur le second appareil, le seuil devenant la période de son écran (16,6 ms à 60 Hz). Macrobenchmark arrive en V1.

## Conséquences
- (+) Le choix coûteux entre natif et Readium se fait sur des chiffres, au bout de 6 semaines au plus.
- (+) Le code du spike sert directement s'il passe la porte.
- (−) À 120 Hz, il reste 8,3 ms par image pour trois passes de shader sur 2,85 Mpx. Trois calques de 2640 × 1080 × 4 octets occupent déjà environ 34 Mo sur les 40 permis.
- (−) Un repli sur Readium coûterait le pli et une partie du travail de 1b.

## Points ouverts
- Nom exact de l'API Compose qui demande la fréquence haute pendant un tour (`preferredFrameRate` ou `requestedFrameRate`).
- Mesures sur le second appareil, avant B.

## Références
PLAN §2 (principe 4), §4 (phases 1a, 1b, porte A), §4.2 ; 15 M1, M2, §5.2 ; 06 §2.13, §3.1, §3.4, §3.5 ; 07 §2.6, §2.8, plan de prototype ; 11 §3.6 (epub).
