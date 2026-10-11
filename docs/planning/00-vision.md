# Paroissien — Vision produit (brief commun aux agents de planification)

> Nom de travail : **Paroissien**, en référence au « paroissien romain », le livre de prière du fidèle qui réunissait missel, prières, dévotions, images pieuses et signets. Le nom peut changer.

## Intention

Une application Android (APK) catholique romaine qui reproduit l'expérience d'un **livre de prière papier** : on tourne les pages, on glisse des images pieuses entre les feuillets, on laisse des signets de tissu de couleur. Sous le capot, elle repose sur les techniques modernes des liseuses : mise en page reflowable, annotations, recherche plein texte, synthèse vocale et paquets de contenu hors ligne.

## Modules de contenu

| Module | Source envisagée | Remarques |
|---|---|---|
| Lettres spirituelles mensuelles | clairval.com (Abbaye Saint-Joseph de Clairval, **Flavigny-sur-Ozerain**) | Archiver toute la collection et lire les lettres dans un lecteur dédié. Le format HTML est réputé stable. Les « lettres mensuelles de Flavigny » sont *a priori* ces mêmes lettres, à vérifier. |
| Exercices spirituels de saint Ignace | ragionline.com (PDF) | Prévoir aussi un mode « retraite guidée ». |
| Missel | Plusieurs rites : forme extraordinaire 1962, forme ordinaire, et d'autres à évaluer | Ordinaire et propre, latin et français en regard, rubriques en rouge. |
| Bible et Évangiles | Traductions catholiques du domaine public | |
| Imitation de Jésus-Christ | Traduction du domaine public | |
| Prières classiques | Latin et français | |
| Calendrier liturgique | Calculé hors ligne | Fêtes, rangs et **couleurs liturgiques**, qui pilotent aussi les couleurs des signets. |
| Extensions | À définir | Bréviaire, rosaire, chemin de croix, catéchisme, vies de saints, examen de conscience… |

## Principes d'expérience

- **Mobile d'abord.** Le portrait et le paysage sont gérés nativement : une page en portrait, une double page en paysage, comme un livre ouvert.
- **Une interface épurée, moderne et sans distraction**, très facile à naviguer.
- **Thèmes clair, sombre et sépia/papier**, plus un **mode méditation « Ne pas déranger »** : DND système, écran maintenu allumé, minuteur et cloche discrète.
- **Une expérience proche du papier.** La page qui tourne fait partie de l'expérience, avec une typographie soignée : lettrines, rubriques rouges, justification et césure.
- **Signets de tissu** de couleurs vert, rouge, violet, blanc, rose et noir, à tirer pour rouvrir le livre à la bonne page.
- **Images pieuses** utilisées comme marque-pages et glissées entre les pages.
- **Hors ligne d'abord.** Pas de pistage et respect de la vie privée.

## Contraintes techniques connues

- La stack de l'utilisateur, issue d'un projet existant, est Kotlin 2.4.x, Jetpack Compose (BOM 2026.09), AGP 9.4.x et le SDK Android déjà installé.
- La machine de développement est une VM de **2 cœurs et 10 Go de RAM, sans GPU**. On ne lance jamais Gradle et un émulateur en même temps, on préfère un téléphone physique et on envisage de déporter les builds en CI (GitHub Actions).
- La langue principale est le français. Le multilingue reste à évaluer, car les lettres de Clairval existent en plusieurs langues.

## Phase actuelle : planification

Chaque agent produit un rapport en français dans `docs/planning/NN-sujet.md`. La synthèse finale sera rédigée dans `docs/planning/PLAN.md`.
