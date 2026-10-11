-- Paquet de contenu Paroissien, format 1.0 (ADR 003). Toutes les tables existent dans chaque paquet, vides au besoin.
PRAGMA application_id = 1347572558;  -- 0x5052534E « PRSN »
PRAGMA user_version = 100;           -- formatMajor*100 + formatMinor
CREATE TABLE pack_info  (key TEXT PRIMARY KEY, value TEXT NOT NULL) WITHOUT ROWID;
CREATE TABLE publication(rowid INTEGER PRIMARY KEY, id TEXT NOT NULL UNIQUE, revision INT NOT NULL, kind TEXT NOT NULL,
                         primary_lang TEXT NOT NULL, languages TEXT NOT NULL, aligned INT NOT NULL, title TEXT NOT NULL,
                         meta TEXT NOT NULL, content_hash TEXT NOT NULL, char_count INT NOT NULL);
CREATE TABLE division   (rowid INTEGER PRIMARY KEY, pub INT NOT NULL, id TEXT NOT NULL, parent INT, ord INT NOT NULL,
                         kind TEXT NOT NULL, toc_title TEXT, is_unit INT NOT NULL, seq_first INT NOT NULL, seq_last INT NOT NULL,
                         attrs TEXT, UNIQUE (pub, id));
CREATE TABLE block      (rowid INTEGER PRIMARY KEY, pub INT NOT NULL, id TEXT NOT NULL, unit INT NOT NULL, seq INT NOT NULL,
                         type TEXT NOT NULL, attrs TEXT, char_start INT NOT NULL, char_len INT NOT NULL,
                         UNIQUE (pub, id), UNIQUE (pub, seq));
CREATE TABLE block_text (block INT NOT NULL, lang TEXT NOT NULL, inlines TEXT NOT NULL, rev TEXT NOT NULL,
                         PRIMARY KEY (block, lang)) WITHOUT ROWID;
CREATE TABLE id_alias   (pub INT NOT NULL, old_id TEXT NOT NULL, new_id TEXT, since_revision INT NOT NULL,
                         PRIMARY KEY (pub, old_id)) WITHOUT ROWID;
CREATE TABLE asset      (rowid INTEGER PRIMARY KEY, href TEXT NOT NULL UNIQUE, media_type TEXT NOT NULL, sha256 TEXT NOT NULL,
                         meta TEXT NOT NULL, data BLOB NOT NULL);
CREATE TABLE devotion   (id TEXT PRIMARY KEY, engine TEXT NOT NULL, def TEXT NOT NULL) WITHOUT ROWID;
CREATE TABLE calendar   (id TEXT PRIMARY KEY, rite TEXT NOT NULL, region TEXT NOT NULL, data_version TEXT NOT NULL,
                         def TEXT NOT NULL) WITHOUT ROWID;
CREATE TABLE search_doc (rowid INTEGER PRIMARY KEY, block INT NOT NULL, lang TEXT NOT NULL, head TEXT NOT NULL, body TEXT NOT NULL);
CREATE VIRTUAL TABLE search_fts USING fts5(head, body, lang UNINDEXED, content='search_doc', content_rowid='rowid',
                         tokenize='unicode61 remove_diacritics 2', detail=full);
