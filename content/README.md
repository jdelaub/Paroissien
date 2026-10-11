# content/ — chaîne de contenu

Produit, hors appareil, les paquets lus par l'application : texte canonique (NFC, sans U+00AD), identifiants stables, index FTS5 plié, paquets SQLite signés. Voir `docs/planning/11-pipeline-contenu.md` et la revue 15.

## Étapes

```
fetch → .cache/raw → normalize → data/ → validate → package → publish
```

| Étape | Rôle |
|---|---|
| `fetch` | Télécharge les sources dans `.cache/raw/` (jamais commité) et journalise URL, date, sha256. |
| `normalize` | Brut → JSON canonique dans `data/` (commité) ; attribue les identifiants. |
| `validate` | Schémas, identifiants uniques, aucun identifiant perdu sans alias, droits déclarés. |
| `package` | `data/` → paquet SQLite déterministe (APSW 3.50.1.0, SQLite ≤ celui de l'app) et manifeste. |
| `publish` | Release et `catalog.json`. |

Seul le pliage de recherche existe aujourd'hui : `pipeline/paroissien_content/fold.py`, avec son contrat partagé avec Kotlin, `schema/fold-vectors.json`.

## Commandes

- Aujourd'hui (bibliothèque standard seule) : `python3 -I run_tests.py` depuis `content/` (`-v` pour le détail).
- À terme : `uv sync --locked`, `uv run pytest`, puis `uv run pqt fetch|normalize|validate|package|publish`.

## Calendrier et images pieuses

- `data/calendar/` : sanctoral de 1962 et de 1969 (général et France), au format `schema/calendar-v1.md`, lu par `core:liturgy`. Provenance dans `data/calendar/SOURCES.md` ; scripts rejouables dans `sources/calendrier-*`.
- `golden/calendar/` : oracles des tests G3 (romcal pour 1969, Missalemeum pour 1962) et `known-divergences.md`, où chaque écart est justifié.
- `data/images/` (absent pour l'instant) : la bibliothèque d'images pieuses, choisie par l'utilisateur ; un `catalog.json` et ses fichiers y seraient copiés dans les assets de l'APK. Chacun peut aussi numériser ses propres images dans l'app.

## Règles

- Rien de `.cache/raw/` n'est commité ; les paquets se construisent uniquement depuis `data/`.
- Les contenus protégés (Clairval, français du Barroux, AELF) ne vivent que dans le dépôt privé `paroissien-content`, jamais ici.
- Chaque paquet porte un statut `rights` ; la CI publique (`tools/check-pack-rights.py`) refuse tout paquet embarqué qui n'est pas `domaine-public` ou `licence-libre:…`.
- Toute modification de `fold.py` passe par `schema/fold-vectors.json`, testé en Python et en Kotlin.
