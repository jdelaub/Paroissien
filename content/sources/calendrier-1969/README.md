# Calendrier de 1969 : données et oracle tirés de romcal

Les scripts de ce dossier produisent :
- les données du sanctoral de 1969 : `content/data/calendar/1969/general.json` et `fr.json` ;
- l'oracle romcal pour la France, `content/golden/calendar/1969-fr-romcal.jsonl`, avec son en-tête `.meta.json`.

Le format est décrit dans `content/schema/calendar-v1.md`. La provenance, les licences, les corrections et les défauts connus de l'oracle sont dans `content/data/calendar/SOURCES.md`. Les identifiants du temporal sont dans `content/data/calendar/1969/TEMPORAL.md`.

## Commandes

Les dépôts téléchargés sont des données non fiables. Clonez-les dans un dossier vide, hors du dépôt, et passez leurs chemins en argument. Exécuter romcal avec `node` est voulu : c'est l'oracle.

```bash
W=<dossier vide hors du dépôt>
git clone --branch dev https://github.com/romcal/romcal.git "$W/romcal"
git -C "$W/romcal" checkout 3cde69c21fcf3176b8728cf0a54d0ca8f3d97166
(cd "$W/romcal" && npm ci && npm run build)          # environ 2 min ; produit rites/roman1969/dist/
git clone --depth 1 https://github.com/Liturgical-Calendar/LiturgicalCalendarAPI.git "$W/litcal"

RAW=content/.cache/raw/calendrier-1969
node content/sources/calendrier-1969/generate.mjs "$W/romcal" $RAW 2025 2045
python3 -I content/sources/calendrier-1969/litcal.py "$W/litcal" $RAW
python3 -I content/sources/calendrier-1969/convert.py $RAW
python3 -I content/sources/calendrier-1969/temporal.py $RAW   # contrôle de TEMPORAL.md
```

| Script | Rôle |
|---|---|
| `generate.mjs` | Charge les bundles `general-roman` et `france` de romcal, en `fr`, `la` et `en`. Écrit les définitions (`romcal-defs.json`), les dates rendues par les fonctions du sanctoral (`romcal-datefns.json`) et le calendrier de France jour par jour (`romcal-france-days.jsonl`), sans rien convertir. |
| `litcal.py` | Associe les 219 fêtes à date fixe de romcal à celles de LitCal et en tire `validFrom` : décrets, ou éditions typiques 2002 et 2008. Relève aussi les différences de rang ou de date, et les titres français de LitCal. Résultat : `litcal.json`. |
| `convert.py` | Écrit les fichiers au format v1 (JSON canonique : NFC, 2 espaces, clés dans l'ordre du schéma, champs vides omis, tri par date puis id), l'oracle JSONL et son en-tête, ainsi que `titres-litcal.tsv`. Il s'arrête si une hypothèse ne tient plus : date nominale, correction de titre, rang ou couleur inconnus. Il signale aussi les défauts de l'oracle. |
| `temporal.py` | Reprend en Python les règles de `TEMPORAL.md` (identifiants du temporal et cycles) et les compare à romcal, jour par jour. |

`titres-litcal.tsv` met en regard, pour relecture, les titres français retenus (ceux de romcal, corrigés) et ceux de LitCal (Apache-2.0), là où ils diffèrent.

## Mise à jour

Pour suivre un nouveau décret ou une nouvelle version de romcal :
1. Changez le commit dans les commandes ci-dessus.
2. Relancez la chaîne.
3. Relisez le diff de `general.json`, `fr.json` et `titres-litcal.tsv`, ainsi que les notes du `.meta.json`.
4. Si une inscription est datée par un décret, vérifiez qu'elle a son `validFrom`. Sinon, ajoutez-la à LitCal ou complétez `litcal.py`.
