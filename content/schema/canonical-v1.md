# Format canonique v1 — guide de production

Ce guide résume ce qu'il faut savoir pour produire une publication. Le détail est dans les ADR 003 et 004 et dans le rapport 11 (§3.2). Le code de référence est `content/pipeline/paroissien_content/` (`canon.py`, `inlines.py`, `package.py`).

## Disposition

```
content/sources/<corpus>/          scripts fetch + normalize (stdlib, python3 -I), README de provenance
content/.cache/raw/<corpus>/       téléchargements bruts (jamais commités)
content/data/<publication>/publication.json
content/data/<publication>/blocks/<unité>.jsonl     un bloc par ligne, dans l'ordre de lecture
content/data/<publication>/aliases.json             (facultatif) {"ancien-id": "nouvel-id" | null}
content/packages/<paquet>.json                      définition du paquet
content/registry/<publication>.ids                  identifiants publiés (pqt registry update)
```

Les contenus protégés (Clairval, Barroux, AELF) suivent la même disposition dans le dépôt **privé** `paroissien-content`, jamais ici.

## publication.json

```json
{
  "id": "imitation.fr", "revision": 1, "kind": "book",
  "title": {"fr": "Imitation de Jésus-Christ"}, "subtitle": {"fr": "Traduction de F. de Lamennais"},
  "languages": ["fr"], "primaryLanguage": "fr", "aligned": false,
  "contributors": [{"name": "Félicité de Lamennais", "role": "translator"}],
  "issued": "1824",
  "source": {"name": "Projet Gutenberg n° 57824", "url": "…", "edition": "…", "retrieved": "2026-10-11", "rawSha256": "…"},
  "license": {"spdx": "LicenseRef-PublicDomain", "attribution": "…"},
  "divisions": [
    {"id": "l1", "kind": "book", "toc": "Livre premier", "unit": true}
  ]
}
```

Les **divisions** descendent jusqu'aux unités de lecture (`"unit": true`), c'est-à-dire le flux paginé chargé d'un coup : un livre de l'Imitation, un livre biblique, une lettre, une section de prières. Une unité ne contient pas d'autre unité. Sous l'unité, la structure s'exprime par des blocs `heading` (`level` 1 pour un chapitre, 2 pour une section).

## Blocs (une ligne JSON par bloc)

| type | attributs | usage |
|---|---|---|
| `heading` | `level` (1–6), `label?` | titres de chapitre ou de section |
| `para` | `dropCap?`, `style?` ∈ normal, intro, epigraph, signature, centered, small | paragraphe |
| `verse` | `n` (« 16 »), `p?` (ouvre un paragraphe), `ref?` | verset biblique ou numéroté |
| `rubric` | — | rubrique, en rouge |
| `versicle` | `role` ∈ V, R, S, P, M, ALL | ℣/℟ ; le texte commence par `{"sym":"V"}` ou `{"sym":"R"}` |
| `quote` | `attribution?` | citation |
| `stanza` | `kind` ∈ hymn, psalm, poem | vers séparés par `{"br":1}` |
| `note` | `placement` ∈ foot, end, margin ; `label` | note appelée par `{"note":"<id>"}` |
| `separator` | `kind` ∈ rule, asterism, fleuron, space | pas de texte |
| `item` | `marker?`, `level?` | points, listes |
| `image` | `href` (fichier de `assets/`), `alt` {langue: description}, `credit?`, `kind?` ∈ ink (défaut), color | gravure ou dessin ; légende facultative dans `t`. `ink` : un masque d'encre (WebP, l'encre dans l'alpha) que le lecteur teint de l'encre du thème. `color` : un dessin en couleurs sur fond blanc, que le lecteur multiplie avec le papier (sur papier sombre, posé sur une planche claire). Des images qui se suivent restent ensemble sur la page quand elles y tiennent. Le paquet les range dans `asset` sous `<publication>/<nom>` |
| `slot` | `slot` (ex. `proprium.introitus`) | emplacement d'une pièce variable (« texte de la messe du jour ») ; `t` porte l'intitulé affiché, l'app pourra le remplir depuis le missel |

Chaque bloc : `{"id": …, "type": …, <attributs>, "t": {"<langue>": [inlines]}}`. Une langue doit figurer dans `languages`.

## Inlines

`"texte"` · `{"m":["i"|"b"|"sc"|"red"|"sup"|"sub"|"sp"],"c":[…]}` · `{"lang":"la","c":[…]}` · `{"note":"<blocId>","l":"1"}` · `{"link":"bible:JHN.3.16","c":[…]}` · `{"sym":"V"|"R"|"cross"|"star"|"dagger"}` · `{"num":"16"}` · `{"br":1}`

## Règles du texte (le validateur les vérifie)

- **NFC.** Jamais de U+00AD, U+200B, U+2060 ou U+FEFF, ni aucun caractère de contrôle. La césure se calcule à l'affichage.
- **Typographie française.** Apostrophe ’ ; espace fine insécable U+202F avant ; : ! ? et à l'intérieur de « ». Pas d'espace avant . ou ,. Restituer œ et æ quand la source les a écrits « oe » ou « ae » dans un mot qui les prend (cœur, œuvre). Pas de modernisation de l'orthographe.
- **Latin.** On garde les accents de la source, les æ et œ, et j/v tels qu'imprimés.
- **Fidélité.** Le texte de la source, sans réécriture. Une correction (coquille d'OCR, erreur évidente) est consignée dans le README du corpus. Les appels de note de la source deviennent des `note`, ou sont retirés et consignés.
- **Mentions périmées.** Les indulgences « de N jours » sont retirées (système aboli, rapport 14).

## Identifiants (stables pour toujours)

- Publication : `^[a-z0-9]+(\.[a-z0-9-]+)+$` (`imitation.fr`, `bible.crampon`). Divisions et blocs : `^[a-z0-9][a-z0-9._-]{0,63}$`, uniques dans la publication, sur un même espace de noms.
- **Clé naturelle quand elle existe**, sinon une clé de position stable dans la source :
  - Bible : livre en code USFM minuscule (`gen`, `1sa`, `jhn`) ; verset `jhn.3.16` dans la versification **de l'édition** ; titre de chapitre `jhn.3` ; note `jhn.3.16.n1`.
  - Imitation : unités `l1`…`l4` ; chapitre `l1.c3` ; paragraphe `l1.c3.p2` (ordre dans le chapitre ; `verse` en attribut s'il ouvre un verset numéroté) ; réflexion `l1.c3.r1`.
  - Exercices : paragraphe canonique `ex.230` (numéros [1] à [370]) ; sous-paragraphes `ex.230.2` ; titres `ex.h.<mot-clé>`.
  - Prières : `<prière>.<n>` (`pater.1`, `litanies-lorette.12`), titres `<prière>`.
  - Clairval : une publication par lettre, `clairval.fr.AAAA-MM-JJ`, blocs `p1`, `p2`…
- Un identifiant publié ne disparaît jamais : `aliases.json` le redirige, ou le marque `null`.

## Paquet (packages/<paquet>.json)

```json
{"packageId": "imitation-fr", "packageVersion": 2026101101, "rights": "domaine-public",
 "publications": ["imitation.fr"], "title": {"fr": "Imitation de Jésus-Christ"}, "minAppFormat": "1.0"}
```

`rights` : `domaine-public`, `licence-libre:<SPDX>`, `autorisation-écrite:<réf>` ou `usage-privé-seulement`. Les deux derniers n'existent que dans le dépôt privé.

## Commandes

```bash
python3 -I content/pqt.py validate imitation.fr
python3 -I content/pqt.py build --out /tmp/packs imitation-fr
python3 -I content/pqt.py registry update imitation.fr
python3 -I content/run_tests.py
```

Le dépôt privé passe `--data`, `--packages` et `--registry` vers ses propres dossiers.
