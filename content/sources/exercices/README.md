# Exercices spirituels de saint Ignace — `exercices.fr`

Traduction du P. Pierre Jennesseaux sj (édition de 1913), numérisée par le Frère Jérôme (Namur, 2005) et publiée par livres-mystiques.com. Le choix de cette source et le plan canonique viennent du rapport `docs/planning/02-exercices-ignace.md`.

## Commandes

```bash
python3 -I content/sources/exercices/fetch.py [--witness]   # téléchargements (une seule fois) dans content/.cache/raw/exercices/
python3 -I content/sources/exercices/normalize.py           # → content/data/exercices.fr/
python3 -I content/pqt.py validate exercices.fr
python3 -I content/pqt.py build --out <dossier> exercices-fr
python3 -I content/pqt.py registry update exercices.fr
```

`normalize.py` s'arrête si les 370 numéros ne sont pas tous présents une fois, dans l'ordre, si une unité ne couvre pas sa plage, ou si une correction ci-dessous ne s'applique plus. Si l'archive du Crampon est présente (`content/.cache/raw/bible/crampon-*.tar.gz`, lue en mémoire, sans extraction), il vérifie aussi que chaque renvoi biblique existe dans sa versification.

## Provenance (téléchargements du 2026-10-11)

| Fichier | URL | sha256 | Rôle |
|---|---|---|---|
| `exercices.html` (213 476 o) | http://livres-mystiques.com/partieTEXTES/Exercices_Ignace/exercices.html | `b5032642d39903e89461c2464205fa16f04fcf6694612b81a26451ed57c2d489` | **source ingérée** |
| `raggionline-exercices.pdf` (331 207 o) | http://www.raggionline.com/saggi/scritti/fr/exercices.pdf | `b1d6a4f3c13428db32643dc2cec6de9b55bf550725ad503d320d5295f39616ad` | témoin : impression de la même page (2013) |
| `jennesseaux-1913_djvu.txt` (875 457 o) | https://archive.org/download/exercicesspirit00igna/exercicesspirit00igna_djvu.txt | `551cd1227ff5c0ad84802c0e0e3dea44d3a39e0aab5b6dd8ab8bf95498b658d9` | témoin : OCR de l'édition Gigord de 1913, pour trancher les références douteuses |

**Droits.** Traducteur mort en 1884 : texte du domaine public. La page déclare « Édition libre de tout droit » (bloc de titre `#t1`, mention reprise telle quelle dans `license.attribution`), alors que le pied de page du site porte « © Livres-Mystiques 1997-2020 ». Crédit : « Trad. P. Pierre Jennesseaux sj (éd. 1913), numérisation Frère Jérôme, Namur 2005 — livres-mystiques.com ». Le rapport 02 recommande un message de courtoisie à l'éditeur du site (décision D2).

**Témoin PDF.** Le texte normalisé, comparé à `pdftotext -layout` du PDF, partage 98,4 % de ses 6-grammes. Les écarts relevés tiennent aux titres, en capitales dans le PDF, et aux césures de fin de ligne. Aucune divergence de texte n'a été trouvée.

## Découpage

Dix unités, dans l'ordre de la source. Chaque division porte `range` (premier et dernier `ex.<n>`), et `week` pour les quatre semaines.

| Unité | Plage | Justification |
|---|---|---|
| `annotations` | [1]–[20] | Partie `#c1` de la source |
| `fondement` | [21]–[23] | Titre des Exercices [21], « Supposition préalable » [22], « Principe et fondement » [23]. Le titre « Première semaine » de la source tombe entre [22] et [23] : il reste à sa place, dans cette unité |
| `examen` | [24]–[44] | Examen particulier, examen général, confession générale. Dans la source, ces sections sont des h4 de la première semaine. On les sépare quand même : ce sont les textes les plus consultés hors retraite. `week: 1` garde le rattachement |
| `semaine-1` | [45]–[90] | Les cinq exercices et les additions |
| `semaine-2` | [91]–[189] | Du Roi temporel à l'élection |
| `semaine-3` | [190]–[217] | Passion et règles de tempérance |
| `semaine-4` | [218]–[237] | Apparition à Notre-Dame et contemplation pour obtenir l'amour |
| `manieres-de-prier` | [238]–[260] | **Unité ajoutée au plan demandé.** La source en fait une partie à part (`#c3` « Oraisons », titre ajouté en 2005), avec sa propre entrée de sommaire. L'annotation [4] la rattache à la 4e semaine, mais une unité propre sert aussi la 1re manière de prier de l'annotation [18] |
| `mysteres` | [261]–[312] | Partie `#c4` |
| `regles` | [313]–[370] | Partie `#c5` : discernement (deux séries), aumônes, scrupules, « De la soumission à l'Église » (règles pour sentir avec l'Église) |

## Modélisation

- **Paragraphes canoniques.** Chaque paragraphe canonique est un bloc `ex.<n>`, de type `para`, ou `item` quand il s'ouvre sur « Premier point. »… Il porte l'attribut `n` et commence par l'inline `{"num":"<n>"}`. Les alinéas suivants (`<br>`, `<p>` sans numéro, texte libre entre deux titres) deviennent `ex.<n>.2`, `ex.<n>.3`… : un texte appartient au dernier numéro qui le précède. On compte 699 blocs : 155 titres, 398 `para` et 146 `item`.
- **Numéro porté par un sous-titre** (h5/h6 : [21], [44], [45], [101], [120], [158], [161], [313], [337], [345], [352]). Le sous-titre devient le bloc `ex.<n>`, `style: "intro"`.
- **Numéro porté par un titre** (h3/h4 : [262]–[312] et [328]). Le titre reste un `heading` sans numéro, et le numéro passe au bloc suivant : la ligne de références d'un mystère (`intro`), le sous-titre du [328], ou le premier alinéa quand le mystère n'a pas de références ([275], [299], [310]).
- **Titres.** Les titres sont des blocs `heading` `ex.h.<mot-clé>`, avec h2 → niveau 1 et h6 → niveau 5. Les clés viennent de tables explicites dans `normalize.py`, par exemple `ex.h.s2.jour-1.contemplation-2`, `ex.h.examen-general.pensee` ou `ex.h.m.annonciation`. Un sous-titre h6 non numéroté devient `ex.h.<titre parent>.st`. **Exception :** les sous-titres des jours 7 à 12 de la 2e semaine suivent le [161] et en sont le texte canonique ; ils deviennent donc `ex.161.2`…`ex.161.7`.
- **Graphique du [31].** Il devient `ex.31.2` : un `para` dont les lignes de la source sont séparées par `{"br":1}`, avec l'attribut d'extension `"x-chart": "examen-particulier"` pour un rendu natif futur.
- **Appels de note.** Les 39 appels renvoient à la liste « Modifications du texte original » (2005). Ils sont retirés du texte. La liste est conservée dans `publication.json` → `x-modifications2005`, avec le numéro, l'astérisque « première occurrence seulement », le libellé et le bloc où figurait l'appel.
- **Renvois bibliques.** Les 104 `<cite>` donnent 109 liens `bible:` : cinq `<cite>` contiennent plusieurs plages. Formes produites :
  - `BOOK.C.V` et `BOOK.C.V1-V2` ;
  - pour les chapitres entiers (« Matthieu 5-7 », « 14-17 » au [290]), la plage interchapitres `BOOK.C1.V1-C2.V2` (`MAT.5.1-7.29`, `JHN.14.1-17.26`) : **extension à confirmer** avec le résolveur de l'agent 04.

  La numérotation est celle de l'édition. Par exemple, « Marc 9, 1-8 » pour la Transfiguration suit la Vulgate. Les 109 liens existent dans le Crampon.

## Normalisation typographique (comptes de la dernière exécution)

| Opération | Nombre |
|---|---|
| Apostrophes droites → ’ | 1 701 |
| `<q>` (paroles de l'Évangile, cf. [261] « entre guillemets ») → « … » avec espaces fines | 112 |
| Espaces fines insécables avant `; : ! ?` | 565 |
| Espaces retirées avant `.` ou `,` (laissées par les appels de note) | 4 |
| Ligatures « oe » → œ (cœur, œuvre, vœu, sœur, œil, bœuf) | 31 |
| Capitales accentuées : « A » → « À » (8), « Eve » → « Ève » (2) | 10 |
| « ... » → « … » | 1 |
| ` - ` entre les références des mystères → ` – ` | 56 |

L'orthographe n'est pas modernisée. Le texte est en NFC.

## Corrections (texte de la source → texte publié)

| Bloc | Source | Publié | Fondement |
|---|---|---|---|
| `ex.298` | Jean 29, 30-42 | Jean 19, 30-42 | **Signalée par le rapport 02.** Chapitre impossible. L'édition de 1913 porte « S. Joan. xix. 30-42 » ; le rapport proposait 38-42, mais la leçon de 1913 est retenue |
| `ex.279` | Luc 7, 22-25 | Luc 8, 22-25 | **Signalée par le rapport 02.** Erreur évidente : la tempête apaisée est Lc 8, 22-25 (parallèles Mt 8, 23-27 et Mc 4, 35-40). L'erreur figure **déjà dans l'édition de 1913** (« S. Luc. vii. 22-25 ») |
| `ex.280` | Matthieu 14, 22-23 | Matthieu 14, 22-33 | 1913 : « xiv. 22-33 ». Les points 2 et 3 couvrent les v. 24-33 |
| `ex.302` | Luc 24, 12-34 | Luc 24, 12 ; 24, 34 | 1913 : « xxiv. 12, 34 », deux versets (Pierre au tombeau, « il est apparu à Simon »). Une plage engloberait Emmaüs, qui est cité au [303]. La forme suit la convention de 2005 (« Jean 20, 1 ; 20, 11-18 ») |
| `ex.281` | Matthieu 10, 1-42: 11, 1 | Matthieu 10, 1-42 ; 11, 1 | Deux-points pour point-virgule (1913 : « x. 1-42 ; xi. 1 ») |
| `ex.258` | prier.L'oraison | prier. L'oraison | Espace manquante |
| `ex.289.3` | `Second point.` en romain | en italique | Balisage aligné sur les 145 autres libellés de points |

## Points douteux

- **Références modifiées en 2005 et conservées.** L'appel 33 l'annonce : « Référence biblique corrigée », avec astérisque. Plusieurs plages diffèrent de 1913 sans être fausses. Par exemple, [272] porte Lc 2, 41-50 contre 41-51 en 1913, [277] Jn 2, 13-22 contre 13-23, [312] Ac 1, 1-12 contre 1-11. D'autres, aux [291], [294], [295] et [303], s'écartent de leçons OCR peu lisibles. On garde le texte de 2005.
- **Luc 8 au [279].** C'est une correction de l'appareil du traducteur lui-même. Elle est réversible en retirant la ligne de `CITE_FIXES`.
- **Placement des numéros.** Pour [1], [24], [27], [32], [43], [55], [62], [64], [65], [73], [91], [110], [118], [121], [170], [184], [200], [218] et [230], l'édition place un sous-titre non numéroté avant le numéro (« Le deuxième exercice est… »). Dans le texte espagnol, ce sous-titre appartient au paragraphe numéroté. Ici il reste `ex.h.….st`, hors de la plage `ex.<n>`. Une jointure FR/ES/LA sur `ex.<n>` ne le ramènera donc pas.
- **Retouches de 2005.** La liste ne donne que 39 types de retouches, et seulement à leur première occurrence. Pour revenir au texte de 1913, il faudrait un diff avec l'OCR (rapport 02, §3.3).
- **Graphique du [31].** Le soulignement typographique ne s'aligne qu'en police à chasse fixe.
