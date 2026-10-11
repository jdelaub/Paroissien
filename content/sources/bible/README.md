# Bibles : Crampon 1923 et Vulgate clémentine

Produit `content/data/bible.crampon/` et `content/data/bible.vulgata/` (paquets `bible-crampon` et `bible-vulgata`). Sources choisies d'après le rapport 04 (§2.2, §2.3 et §3).

```bash
python3 -I content/sources/bible/fetch.py      # archives GitLab à un commit figé → content/.cache/raw/bible/
python3 -I content/sources/bible/crampon.py    # USFM → data/bible.crampon (≈ 9 s)
python3 -I content/sources/bible/vulgata.py    # OSIS → data/bible.vulgata (≈ 4 s)
python3 -I content/sources/bible/check.py      # comptes, versets vides ou géants, écarts avec SWORD
python3 -I content/pqt.py validate bible.crampon bible.vulgata
```

Bibliothèque standard seulement. Les archives sont lues sans extraction, livre par livre, et rien de téléchargé n'est exécuté (les scripts `txt2Usfm.sh` et `nom.sh` du dépôt Vulgate sont ignorés).

## Provenance

| | Crampon 1923 | Vulgate clémentine |
|---|---|---|
| Dépôt | https://gitlab.com/crosswire-bible-society/crampon | https://gitlab.com/crosswire-bible-society/vulgclementine |
| Commit | `8dbc427f853798132620ccff958fdb25459d2c0a` (2026-08-26, « Correction d'une grosse erreur de scraper, les titres avaient disparus ») | `be6ca9c13addb83ea6faa056e78971fd408e8bb4` (2022-09-19) |
| Fichier | archive `…/projects/14219928/repository/archive.tar.gz?sha=<commit>`, 73 fichiers `usfm/*.usfm` | archive `…/projects/39534063/repository/archive.tar.gz?sha=<commit>`, `vulgclementine.osis.xml` |
| SHA-256 de l'archive | `0743c8e8db570b55dae4d36b94abb7281b38a31fc42c26e666c9c1caa7961675` (5 803 314 o) | `425abbeb781d61a87ebfba7a33bab85c4ce97d5fee7cb5edaa90c303057e56b3` (1 950 912 o) |
| Module SWORD | FreCrampon 3.4.0, `Versification=Catholic2`, `DistributionLicense=Public Domain` | VulgClementine 2.1.0, `Versification=Vulg`, `DistributionLicense=Public Domain` |
| Texte | Transcription relue de fr.wikisource « Bible Crampon 1923 » (fac-similé BnF bpt6k318720w), OCR de J.-M. Weber | Clementine Text Project (M. Tweedale), d'après Colunga et Turrado (BAC, Madrid, 1946) |
| Récupéré le | 2026-10-11 | 2026-10-11 |

Témoins de contrôle, jamais recopiés dans les données : `canon_catholic2.h` (`ed472d41…`) et `canon_vulg.h` (`8a3f6d05…`) du dépôt SVN SWORD (GPL-2), lus comme texte par `check.py`. Les titres français des livres viennent de la table des matières de l'édition (fr.wikisource, `Page:La Sainte Bible, trad Crampon, édition 1923.djvu/8` et `/1758`). Les titres latins sont des formes traditionnelles ajoutées par nous, car l'OSIS n'en a pas (`books.py`).

Le module SWORD compilé `FreCrampon` n'est **pas** utilisé : il contient 962 versets vides et 60 versets géants (rapport 04).

## Modèle produit

- Une unité par livre, clé USFM en minuscules (`gen`, `1sa`, `jhn`), sous les divisions `at` et `nt`. L'ordre est celui de chaque édition : Catholic2 pour le Crampon (1-2 Machabées après Esther), Vulgate pour la Clémentine (1-2 Machabées après Malachie).
- `<livre>.titre` : titre du livre, `heading` de niveau 1. La clé `<livre>` est déjà celle de l'unité, et divisions et blocs partagent un même espace de noms.
- `<livre>.<ch>` : `heading` de niveau 2. Le libellé est « Chapitre n », ou « Psaume n » dans les Psaumes (en latin « Caput n », « Psalmus n »).
- `<livre>.<ch>.h<k>` : intertitres de l'édition, de niveau 3. L'attribut d'extension `x-usfm` (`ms1`, `s1`, `s2`, `qa`, `is1`, `is3` ou `p`) ou `x-osis` (`acrostic`, `speaker`, `p`) garde le marqueur source, ce qui permettra de hiérarchiser plus tard.
- `<livre>.<ch>.<v>` : `verse`, `n` = « v », texte précédé de `{"num":"v"}`. `p: true` quand le verset ouvre un paragraphe (`\p`), une strophe (`\b`, ou deux `\q1` vides de suite) ou suit un intertitre. Le premier verset de chaque livre porte `dropCap: true`.
- **Poésie** (`\q1`, `<l>`) : chaque vers commence par `{"br":1}`. Quand un verset commence un nouveau vers, le saut est placé à la fin du verset précédent, qui se termine donc par `{"br":1}`. Un `\p` au milieu d'un verset devient lui aussi `{"br":1}`.
- `<livre>.<ch>.<v>b` : suite d'un verset coupé par un intertitre. Le Crampon place des sommaires au milieu de versets (« Chap. xxiii, 19b »). La suite est un `verse` avec `n` = « 19b », `p: true` et sans `num` : 19 blocs dans le Crampon, aucun dans la Vulgate.
- Notes (Crampon) : `<livre>.<ch>.<v>.n<k>` (`k` = rang dans le verset), `placement: foot`, regroupées à la fin du chapitre. L'étiquette est numérotée dans le chapitre. Une note d'intertitre a pour clé `<livre>.<ch>.h<k>.n<j>`. `\fq` et `\+qt` sont rendus en italique.
- Renvois des notes : `\+xt Ps. li, 12|PSA 51:12\+xt*` devient `{"link":"bible:PSA.51.12"}`, mais seulement si le chapitre affiché (chiffres romains) concorde avec la cible et que la cible existe dans l'édition. Sinon le texte reste et le lien est retiré : 149 cibles incohérentes (« Is. iv, 5 » → ISA 51:5) et 62 cibles absentes. 4 120 liens sont gardés. La versification des cibles est celle du Crampon.
- `\add` (mots ajoutés par le traducteur) et `\qs` (Séla) sont rendus en italique.
- Vulgate : le Prologue du Siracide et celui des Lamentations deviennent `<livre>.prologus` (intertitre) et `<livre>.prologus.p1` (`para`). Les locuteurs du Cantique (`<speaker>`) et les lettres acrostiches deviennent des intertitres.
- Typographie : NFC, apostrophe ’, espace fine insécable avant ; : ! ? et dans les guillemets, aucune espace avant , et . Les U+00A0 et U+202F de la source sont reposés uniformément, et deux U+FEFF sont retirés. Le latin garde sa graphie (æ, œ, ë, j, sans accents) ; on y rend seulement insécable l'espace que la Clémentine imprime avant : ; ? !.
- Ligatures restituées dans le Crampon : « oeil » → « œil », « Coelé-Syrie » et « Coelésyrie » → « Cœlé… », « Phoebé » → « Phœbé ». Les mots latins des notes (« gloriae ») ne sont pas touchés.
- Nom divin : « Yahweh » tel qu'imprimé (PLAN §5.2).

## Défauts de la source USFM et corrections (Crampon)

Toutes les retouches sont listées dans `PATCHES` (`crampon.py`), avec leur motif. Chacune doit trouver son texte exactement une fois, sinon la conversion s'arrête.

- **Versets dédoublés ou coupés**, repérés par `\vp`, puis renumérotés d'après le numéro imprimé (110 versets) :
  - Gn 30, 23-24 répétés en 25-26 ; numéros imprimés « 30 » et « 40 » corrigés en 31 et 41 ;
  - 1 S 2, 1 coupé en deux ;
  - Esd 2, 32 et 2, 35 coupés ; Esd 9, 11 coupé ;
  - Ne 2, 17 tronqué puis répété ;
  - Ps 60, 6 : « — Séla » détaché en verset ;
  - Ez 42 : verset fantôme fait de fragments de 16 et 17 ;
  - 1 M 16, 24 et Ba 2, 19 : `\vp` erroné ou vide, ignoré.
  Bilan : 35 599 `\v` dans la source, **35 590 versets** produits.
- **Poèmes recollés** : Nb 21, 17-18, Nb 21, 27-30 et 1 R 12, 16. La source les écrit en `\qa` avec les numéros des versets suivants, puis les répète sans espaces (« Hésebon,une flamme »). Ils sont reconstruits ; la coupe des vers suit les jonctions sans espace. La fin de Nb 21, 30 (« avec le feu jusqu'à Médaba ») est reprise du `\qa`.
- **Ps 120, 1** : la note sur « Cantique des montées » était fondue dans le texte du verset. Elle redevient une note, ce qui donne 8 790 notes contre 8 789 balisées dans la source.
- **Sommaires écrits en `\p`** (« 2. Chap. xiv. … », « Ps. xlvi (Vulg. xlv) : … », lignes tout en italique suivies de renvois) : 21 cas, rendus en intertitres (`x-usfm: "p"`). Avant, certains se retrouvaient collés au verset précédent.
- **Appels de note de Wikisource** restés dans les intertitres (« [109] », « ([60]) ») : 75 retirés. Leurs notes n'existent pas dans la source.
- **Restes de wikitexte** (`[[…|…]]`, `{sc|…}}`, `</poem>`, `</ref>`, accolades) : 14 retouches. Il faut y ajouter 4 lignes « Psaumes Livres », en-têtes de colonnes de la mise en page, et un « 23 » parasite en Ex 23, 14.
- **Espaces perdues autour des italiques** : 4 « \add …,\add*mot », 1 Ch 7, 2 et Ez 43, 7.
- **Prologue du Siracide** : seul le titre « PROLOGUE » est présent, sans le texte. Le titre est retiré ; le prologue du traducteur manque donc au Crampon.

Défauts connus, **non corrigés** :

- **Ordinaux perdus dans les notes**, environ 170 cas : « Le  jour du mois d'Elui,  mois de l'année », « la  partie du Ps. cviii ». L'extraction a supprimé les « 18e », « 2e »… Il faudrait les reprendre sur Wikisource.
- **Coquilles** de la transcription laissées telles quelles : « chap, xxi », « l'arnon », « Levons-nous, et bâtissons ! « » en Ne 2, 20.
- **Renvois** : la cible verset n'est pas vérifiée quand le chapitre concorde.

`check.py` compare aussi les comptes à SWORD Catholic2. 74 chapitres diffèrent, surtout Tobie, Judith et le Siracide, que Crampon traduit sur le grec, ainsi que des chapitres où le Crampon suit la Vulgate (1 M 1, Dn 13-14, Mc 4 et 8-9, Ac 19). La revue n'y a pas trouvé de verset fusionné ni de numéro collé dans le texte. Contrôles : 0 verset vide, 0 verset de plus de 900 caractères (le plus long, 2 R 6, 32, en fait 466) et une numérotation continue dans chaque chapitre.

## Vulgate : contrôles

35 809 versets (rapport 04 : 35 809), sans aucun défaut à corriger. Un seul cas particulier : « Alleluja. » placé avant Ps 118, 1 hors de tout verset devient un intertitre. Le contrôle donne 0 verset vide et 0 verset géant (le plus long, Est 8, 9, fait 425 caractères). Huit chapitres ont un verset de moins que le maximum SWORD Vulg (Ps 15, 42, 125, 135 ; Is 45 ; Jn 11 ; 2 Co 1 ; 3 Jn), ce qui correspond à la numérotation de cette édition. Psaumes numérotés selon la Vulgate.

## Droits

- **Crampon 1923** : œuvre collective publiée en 1923 sans nom de réviseur. Domaine public probable en France depuis 1994 (CPI L113-2, L123-3) et domaine public aux États-Unis. Risque faible selon la revue 15 ; le repli est le Glaire 1905. Voir `license.status` dans `publication.json`.
- **Vulgate clémentine** : domaine public. Le Clementine Text Project a versé sa transcription au domaine public.
- **Point douteux** : l'édition Colunga-Turrado de 1946 apporte la division en paragraphes et quelques titres (prologues). Le risque d'un droit d'éditeur sur cette présentation paraît très faible.
- **Attribution** : CrossWire Bible Society, ainsi que Wikisource pour le Crampon (`license.attribution`).

## Mesures (2026-10-11, VM 2 cœurs)

| | Crampon | Vulgate |
|---|---|---|
| Blocs | 48 407 | 37 370 |
| Données JSONL | 12 Mo | 7 Mo |
| Paquet SQLite (avec FTS5) | 29,1 Mo | 17,6 Mo |
| gzip -6 / xz -6 | 11,4 / 8,1 Mo | 7,2 / 4,7 Mo |

Le build des deux paquets a pris 12,3 s, avec un pic de 140 Mo de mémoire. Les paquets dépassent nettement l'estimation du rapport 04 (11 et 8 Mo) : le format v1 stocke les inlines en JSON plus `search_doc` et l'index FTS5.
