# ADR 003 — Paquet de contenu v1

## Statut
Proposé, 2026-10-11. Validé par le paquet golden en phase 2.

## Contexte
Les rapports proposaient trois formats (SQLite et Ed25519 en 11, ZIP `.paq` et `index.json` en 10, ZIP `.ppk` en 14) avec des noms de champs de version différents. Il faut un format unique, lu par un seul lecteur, qui accueille aussi images, dévotions et données du calendrier, et que le SQLite embarqué par `androidx.sqlite:sqlite-bundled` 2.7.1 (**3.50.1**, FTS5, sans ICU) sache ouvrir.

## Décision
- **Un paquet = un fichier SQLite** `<packageId>.sqlite`, sans conteneur ZIP. La DDL de référence est `content/schema/pack-v1.sql` ; en voici le résumé (colonnes `NOT NULL` sauf `parent`, `toc_title`, `attrs`, `new_id`, `head`, `body`) :

```sql
PRAGMA application_id = 1347572558;  -- 0x5052534E « PRSN »
PRAGMA user_version = 100;           -- formatMajor*100 + formatMinor ; page 4096, UTF-8, journal DELETE
CREATE TABLE pack_info  (key TEXT PRIMARY KEY, value TEXT) WITHOUT ROWID;
CREATE TABLE publication(rowid INTEGER PRIMARY KEY, id TEXT UNIQUE, revision INT, kind TEXT, primary_lang TEXT,
                         languages TEXT, aligned INT, title TEXT, meta TEXT, content_hash TEXT, char_count INT);
CREATE TABLE division   (rowid INTEGER PRIMARY KEY, pub INT, id TEXT, parent INT, ord INT, kind TEXT, toc_title TEXT,
                         is_unit INT, seq_first INT, seq_last INT, attrs TEXT, UNIQUE (pub, id));
CREATE TABLE block      (rowid INTEGER PRIMARY KEY, pub INT, id TEXT, unit INT, seq INT, type TEXT, attrs TEXT,
                         char_start INT, char_len INT, UNIQUE (pub, id), UNIQUE (pub, seq));
CREATE TABLE block_text (block INT, lang TEXT, inlines TEXT, rev TEXT, PRIMARY KEY (block, lang)) WITHOUT ROWID;
CREATE TABLE id_alias   (pub INT, old_id TEXT, new_id TEXT, since_revision INT, PRIMARY KEY (pub, old_id)) WITHOUT ROWID;
CREATE TABLE asset      (rowid INTEGER PRIMARY KEY, href TEXT UNIQUE, media_type TEXT, sha256 TEXT, meta TEXT, data BLOB);
CREATE TABLE devotion   (id TEXT PRIMARY KEY, engine TEXT, def TEXT) WITHOUT ROWID;
CREATE TABLE calendar   (id TEXT PRIMARY KEY, rite TEXT, region TEXT, data_version TEXT, def TEXT) WITHOUT ROWID;
CREATE TABLE search_doc (rowid INTEGER PRIMARY KEY, block INT, lang TEXT, head TEXT, body TEXT);
CREATE VIRTUAL TABLE search_fts USING fts5(head, body, lang UNINDEXED, content='search_doc', content_rowid='rowid',
                         tokenize='unicode61 remove_diacritics 2', detail=full);
```

- Colonnes JSON (NFC, clés triées) : `languages`, `title`, `meta` (contributeurs, source, licence SPDX, approbation), `attrs`, `inlines` (grammaire du rapport 11 §3.2.3), `asset.meta` (dimensions, crédit, licence), `devotion.def` et `calendar.def`, validés par `content/schema/devotion.schema.json` et `calendar.schema.json`. `rev` : ADR 004. `search_doc` contient le texte plié (ADR 004). `asset` : WebP seulement en 1.0. Toutes les tables existent dans chaque paquet, vides au besoin ; ni vue ni déclencheur. Un seul `rights` par paquet.
- **Champs de version**, dans `pack_info` et dans le manifeste :

  | Champ | Règle |
  |---|---|
  | `formatMajor`, `formatMinor` | 1 et 0. Majeur = rupture ; mineur = ajout qu'une app plus ancienne peut ignorer (type de bloc, attribut, table) |
  | `PRAGMA user_version`, `application_id` | `formatMajor*100 + formatMinor` ; `0x5052534E`. Les deux doivent concorder avec `pack_info` |
  | `packageId` | `^[a-z0-9]+(-[a-z0-9]+)*$` (`imitation-fr`, `missel-1962`), stable pour toujours |
  | `packageVersion` | entier `AAAAMMJJNN`, strictement croissant |
  | `minAppFormat` | `"majeur.mineur"` minimal que l'app doit savoir lire pour afficher le paquet correctement |
  | `rights` | statut de droits (ADR 001) |

  S'y ajoutent `builtAt`, `contentHash` (SHA-256 du JSONL canonique), `pipelineCommit`, `sqliteVersion`.
- **Manifeste et catalogue.** Chaque paquet a son `<packageId>.manifest.json` (champs ci-dessus, `title`, `languages`, `license`, `size`, `sha256` du fichier). `catalog.json` (`catalogFormat: 1`, `generated`, `packs[]`) les réunit ; c'est le seul nom de catalogue. Dans l'APK, `assets/packs/` contient `catalog.json`, `catalog.json.sig` et, par paquet, le `.sqlite` et son `.manifest.json`. En V1, le transport est `.sqlite.gz` (gzip `mtime=0`).
- **Signature ECDSA P-256** (`SHA256withECDSA`, DER, avec `keyId`), détachée, sur les octets exacts de `catalog.json` (Ed25519 n'existe qu'à partir de l'API 33). Le même code vérifie le catalogue embarqué et, en V1, le catalogue téléchargé ou importé. Clé de test commitée et non secrète (`content/schema/keys/test-p256.pem`) pour debug, benchmark et CI ; clé de production hors dépôt (`~/.android-keys/`), utilisée en local seulement. Une release n'accepte que les deux clés publiques de production qu'elle embarque (actuelle et suivante).
- **Installation** : au premier lancement ou après une mise à jour de l'APK, copie vers `noBackupFilesDir/packs/<packageId>/<packageVersion>.sqlite`, puis vérification du SHA-256 contre le catalogue signé, de `application_id`, de `user_version` et de `PRAGMA quick_check`, une seule fois. Ouverture ensuite en lecture seule avec `BundledSQLiteDriver`, `trusted_schema=OFF`.
- **SQLite 3.50.1** : la chaîne épingle APSW 3.50.1.x. Un test JVM ouvre chaque paquet avec `sqlite-bundled-jvm` 2.7.1 et vérifie `sqlite_version()` et une requête FTS5. Pour monter de version : androidx.sqlite (2.8.x) d'abord, la chaîne ensuite.
- **La CI échoue** si un identifiant publié disparaît sans `id_alias`, si le format baisse, si `packageVersion` ne croît pas, si le SQLite de la chaîne dépasse celui de l'app, si le paquet golden (`content/golden/`) n'est pas identique bit à bit à son empreinte, ou si l'APK dépasse 30 Mo.
- **Erreurs** : jamais de plantage ; le livre est grisé avec un message en français.

  | Situation | Comportement |
  |---|---|
  | Signature du catalogue invalide | Catalogue refusé ; paquets installés inchangés |
  | SHA-256, `application_id`, `user_version` ou `quick_check` en échec | Quarantaine (`packs/quarantine/`) ; paquet embarqué : une nouvelle copie depuis l'APK, puis quarantaine |
  | `formatMajor` inconnu, ou `minAppFormat` supérieur au format de l'app | Refus : « Ce livre demande une version plus récente de Paroissien » |
  | `formatMinor` supérieur, majeur connu | Ouvert ; bloc inconnu affiché comme `para` ; attribut ou table inconnus ignorés |
  | JSON invalide (bloc, dévotion, calendrier) ; date hors calendrier | Texte brut ; dévotion masquée ; « férie » avec indicateur. Alerte en build debug |

## Conséquences
- (+) Un seul format et un seul lecteur de la phase 2 à la V1 : le téléchargement n'ajoutera que le transport.
- (+) Index FTS5 précalculé : aucune construction sur l'appareil.
- (−) Stockage double pour les paquets embarqués (asset dans l'APK, copie extraite).
- (−) Une clé de contenu à garder en plus de la clé de l'APK.
- (−) Le format reste lié à la plus ancienne version de SQLite en service.

## Points ouverts
- Index de préfixes FTS5 (`prefix='2 3'`) : à mesurer en phase 3 (ajout compatible, format 1.1).
- Schémas de `calendar.def` (phase 4) et de `devotion.def` (V1).
- Ouverture en `immutable=1` (chaque version a son fichier) avec `BundledSQLiteDriver` : à vérifier ; sinon lecture seule simple.

## Références
11 §3.2 à §3.6 ; 10 §2.5, §2.7, §3.2 ; 14 §2.4 ; 05 §3.1 ; 15 M4, M13, m12, §5.4, §5.5, §5.7 ; PLAN §2 (principe 7), §5.3 (Paquets).
