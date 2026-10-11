# Images pieuses : lot MVP

Trente cartes embarquées dans l’APK, choisies d’après la curation du rapport 09 (§3.2.4). Sortie : `content/data/images/` (vue pleine, vignettes, `catalog.json`).

## Commandes

Depuis ce dossier, avec un venv qui contient Pillow compilé avec WebP :

```
python3 -I fetch.py              # API + téléchargement → content/.cache/raw/images/ (non commité)
<venv>/bin/python -I build.py    # recadrage + WebP → content/data/images/, contact-sheet.webp
```

`--raw DIR` change le dossier des fichiers bruts, `--only id1,id2` limite `fetch.py` à quelques cartes. `fetch.py` revérifie la licence à chaque passage et ne retélécharge que les images absentes ou dont l’URL a changé ; `build.py` refuse un fichier brut dont le sha256 ne correspond plus à `fetch-log.json`.

## Fichiers

| Fichier | Rôle |
|---|---|
| `selection.json` | Les 30 cartes : objet source, textes français, thèmes, temps, fêtes (identifiants romcal), prière suggérée, recadrage |
| `fetch.py` | Interroge l’API de l’objet, refuse tout objet non marqué domaine public, télécharge l’image, journalise URL, date, taille, sha256 et preuve de licence |
| `build.py` | Recadre au rapport 0,58, encode en WebP q75 (1 440 px et 360 px de haut), en baissant la qualité, puis la définition (1 080 px au plus bas), d'une vue pleine qui dépasserait 170 Ko ; écrit `catalog.json` et la planche contact |
| `contact-sheet.webp` | Toutes les vignettes avec leur identifiant, pour validation |

## Recadrage

Dans `selection.json`, `focal` est le point à centrer, en fractions de la largeur et de la hauteur de l’image source. `bounds` (facultatif) délimite la zone utile : hors cadre doré, mire de couleur, inscription, marge du support. `scale` (facultatif, 1 par défaut) resserre le cadre. Le rectangle effectivement découpé, en pixels de l’image source, est recopié dans `catalog.json` (`crop`).

## Licences

- **Metropolitan Museum** : `isPublicDomain = true`, images Open Access sous CC0. `licence: "CC0"`.
- **Art Institute of Chicago** : `is_public_domain = true` et `copyright_notice = null`. L’API ne place sous CC0 que ses données, pas les images ; on inscrit donc `licence: "domaine-public"` (rapport 09, §2.5).
- Toutes les œuvres sont en 2D (peinture, gravure, dessin, enluminure, canivet) : leurs reproductions fidèles ne créent pas de droit nouveau (rapport 12, §2.2.8).
- Aucune image de Wikimedia Commons, de Gallica, du MuCEM ni de RMN.
