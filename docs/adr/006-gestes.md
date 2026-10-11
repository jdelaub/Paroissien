# ADR 006 — Gestes du lecteur

## Statut
Accepté, 2026-10-11. La variante avec exclusion est tranchée au spike 1b.

## Contexte
Le tourne-page, les rubans, les images pieuses, l'appui long et les gestes du système se disputent les mêmes bords de l'écran. Le rapport 07 excluait le geste retour aux coins ; le rapport 08 recommandait de ne pas l'exclure. Sur Samsung s'ajoute la poignée des Edge panels, qu'une application ne peut pas neutraliser. Le Flip6 se lit aussi en double page, en paysage ou en posture livre.

## Décision
- **Geste retour : aucune exclusion par défaut.** Une bande de 32 dp le long des bords gauche et droit appartient au système : l'application n'y consomme aucun appui. Un glisser de page ou une saisie de coin ne démarre qu'**à plus de 32 dp du bord**.
- **Variante testée au spike 1b** : `Modifier.systemGestureExclusion` sur quatre rectangles de 32 × 96 dp aux coins (192 dp par bord, sous la limite de 200 dp). Elle n'est retenue que si le comportement par défaut échoue au critère (0 retour involontaire sur 50 saisies de coin) et qu'elle le réussit. Le test couvre tous les modes de navigation d'One UI.
- **Edge panels** : au spike 1b, compter les ouvertures involontaires sur 50 saisies de coin du côté de la poignée. En cas de conflit, rien n'est contourné dans le code : l'aide propose de déplacer ou de masquer la poignée.
- **Zones de tap** : 25 % à gauche = page précédente ; 50 % au centre = afficher ou masquer le chrome ; 25 % à droite = page suivante. Options : zones inversées, « tap partout = page suivante ». En double page, les zones se calculent sur toute la largeur de la double page.
- **Glisser** : départ après le *touch slop*, mouvement horizontal ; le bord libre de la feuille suit le doigt ; une saisie dans la moitié haute soulève le coin haut, dans la moitié basse le coin bas. **Validation** si la progression atteint 35 % ou si la vitesse dépasse 600 dp/s dans le sens du tour ; sinon retour élastique. Un second doigt ou une interruption du système annule le tour.
- **Retour haptique** : trois signaux (soulèvement, seuil franchi dans un sens ou dans l'autre, atterrissage), émis par la View pour respecter le réglage du système ; les tours déclenchés par programme sont muets ; un interrupteur les coupe. **Aucun son** au MVP.
- **Priorités**, décidées à l'appui initial, de la plus forte à la plus faible :
  1. gestes système obligatoires (bas de l'écran) et bandes de bord de 32 dp ;
  2. chrome visible ;
  3. queues de rubans (bande de 32 à 40 dp au-dessus des encarts système du bas, saisissable à 16 dp au moins du bord) et images pieuses (qui dépassent par la tête de la page) ;
  4. tour de page (tap ou glisser) ;
  5. appui long sur le texte (menu : « Poser un ruban ici » ; sélection, notes et coin corné en V1).

  Pendant un tour, les niveaux 3 et 5 sont inactifs. Le coin corné ne se posera pas par un tap dans le coin : il entrerait en conflit avec la zone « page suivante » et avec la saisie du coin.
- **Touches de volume** : option désactivée par défaut (volume bas = page suivante, volume haut = page précédente), sans effet pendant la séance de méditation. Clavier : flèches, Page précédente et Page suivante, espace.
- **Animations réduites** (`ANIMATOR_DURATION_SCALE` à 0, observé en continu) : les gestes restent ; le tour devient une coupe nette ou un fondu de 150 ms.
- **Accessibilité** : un nœud par paragraphe ; actions « Page suivante » et « Page précédente » en français ; sous TalkBack, le mode défilant est proposé. Pas de pincement sur le texte au MVP.

## Conséquences
- (+) L'utilisateur peut toujours sortir par le geste retour, sans dépendre d'une API plafonnée.
- (+) La même mécanique sert en portrait, en paysage et en posture livre.
- (−) Le coin se saisit un peu à l'intérieur de la page, ce qui est moins naturel qu'au bord du papier.
- (−) La poignée des Edge panels peut rester gênante sans parade logicielle.

## Points ouverts
- Résultats de la variante d'exclusion et du test des Edge panels (spike 1b), à consigner ici.
- Réglage fin des seuils (35 %, 600 dp/s, 32 dp) au ressenti sur le Flip6.

## Références
PLAN §5.1 (5), §5.3 (Gestes) ; 15 M12, m1, §5.2 ; 07 §2.4, §2.5, §2.7 ; 08 §2.4, §2.12 ; 09 §2.3, §3.1.4 ; 13 §2.5.
