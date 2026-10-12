# Atelier : cadres, couvertures, gardes et rubans

Proposition du 12/10/2026, à la suite des scans du paroissien chromolithographié (premier lot, 14 photos) et d'un second lot de 16 photos : couvertures, gardes, bandeaux latéraux, compositions partielles, cadre simple. Deux buts : enrichir la bibliothèque d'ornements, et permettre à l'utilisateur de composer lui-même l'habit de chaque livre.

## 1. Ce que les scans apportent

| Famille | Scans | Modèle |
|---|---|---|
| Cadres pleins | matin, soir, avant et pendant la messe, confession, communion, vêpres, complies, mariage, renaissance, vigne, quadrilobes, frontispice de l'évangéliste | Cadre à quatre bandes (fait) |
| Bandeaux latéraux | vigne et calice, palmes, passiflores et lyre (2), figuier « Jacob Juda David », lis « Vox Dei » | Une bande du côté extérieur, médaillon au milieu |
| Compositions partielles | candélabres en tête et rinceaux au côté (table des matières, ordinaire), candélabre au côté et rinceaux au pied (litanies) | Deux bandes en équerre (Γ ou L) |
| Cadre simple | double filet rouge à anneaux d'angle (p.81) | Filets et ornements d'angle seuls |
| Gardes | asanoha or et bleu, symboles chrétiens rouges ; soie moirée (faite) | Motif répété sur toute la page |
| Couvertures | toile noire gaufrée à rameau et monogramme ; veau brun nu ; veau à filets et carrés dorés ; toile noire à croix dorée, lis et bande gaufrée au dos | Matière + décor en modules |
| Tranches | rouge orangé | Une finition de plus |
| Vignettes | scènes gravées en tête de page | Images tramées : hors du moteur vectoriel (voir §5) |

## 2. Moteur, troisième version

Les relevés du premier lot ont buté sur les mêmes limites ; le second lot les rend indispensables.

- **Disposition.** Chaque bande peut manquer : haut, côté extérieur, pied, côté intérieur. Le côté extérieur suit le folio, comme dans un livre : à droite sur un recto (folio impair), à gauche sur un verso. Les dispositions courantes : pleine, latérale, en tête, en équerre (Γ, L), filets seuls.
- **Pièces de bout.** Un ornement posé une fois à chaque bout d'une bande (socle d'un candélabre, départ d'une vigne), en plus des coins.
- **Filets à distance fixe** du bord extérieur ou du texte, en unités, et non plus en fraction de la bande : une moulure garde la même épaisseur sur une bande du pied plus haute.
- **Débords.** Pièces centrales et coins peuvent déborder vers le texte, et se dessiner par-dessus les baguettes (médaillons qui interrompent les filets).
- **Motifs répétés** (gardes) : une tuile et un réseau (carré, quinconce, losange, hexagonal), dessinés une fois en mémoire puis répétés par un shader d'image.
- **Couvertures en modules** : une matière (toile grenée, veau patiné, maroquin, papier), un décor de bord (une frise, comme un cadre), un motif central, des ornements de tête et de pied, une bande au dos, des coins arrondis ; trois fers : doré (or en dégradé), à froid (gaufrage : ombre et lumière décalées), imprimé.

## 3. L'atelier

Un écran par livre, ouvert depuis la bibliothèque (appui long sur la couverture) ou depuis Aa : « Habiller ce livre ».

- **Couverture** : matière et couleur, décor (modèles de la bibliothèque), motif central, titre doré ou non.
- **Gardes** : moiré, asanoha, symboles, ou papier uni ; couleur.
- **Tranche** : dorée, rouge, rouge orangé, naturelle.
- **Pages** : un cadre par section (la règle du sommaire), un cadre unique, ou aucun ; disposition (pleine, latérale, en tête, en équerre) ; couleurs du livre ou couleur liturgique du jour.
- **Rubans** : nombre (1 à 6), couleurs, tissu (mat ou satin), extrémité (biais, queue d'aronde, frange, ferret), croix brodée ou non.
- **Composer un cadre** : choisir dans la bibliothèque la frise de chaque bande, les coins, le médaillon et la palette ; le résultat s'enregistre comme « mon cadre » et se réutilise. On assemble des modules existants, on ne dessine pas à main levée.
- **Aperçu vivant** : le livre fermé, puis ouvert sur la garde et une page encadrée, rubans posés.

Stockage : `user.db`, une table `book_look` (publication → description JSON de l'habit) et une table `custom_frame` (composition JSON qui ne référence que des modules de la bibliothèque par leur nom). L'export JSON des signets les reprend.

## 4. Étapes

1. Fin du premier lot : génération, captures, intégration au lecteur, livraison.
2. Moteur v3 : disposition, pièces de bout, filets fixes, débords ; motifs répétés.
3. Second lot, relevé en parallèle : bandeaux latéraux, compositions partielles, cadre simple, gardes, couvertures.
4. Couvertures en modules.
5. Atelier : habit par livre, puis composition de cadres.

## 5. Questions ouvertes

- **Vignettes gravées** : ce sont des images tramées (scènes de l'Évangile) que le moteur vectoriel ne sait pas redessiner. Elles pourraient entrer comme images, tirées de vos propres scans ; à vous de dire si elles ont leur place.
- **Portée de l'atelier** : un habit par livre (proposé), ou des thèmes globaux appliqués à toute la bibliothèque, ou les deux.
