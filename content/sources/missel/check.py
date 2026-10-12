#!/usr/bin/env python3
"""Contrôle du missel de 1962 (publication `missel.1962`) : complétude des formulaires, clés, marques de DO, droits.

    python3 -I content/sources/missel/check.py [--private DIR] [--divinum DIR] [-v]

- chaque formulaire ordinaire a Introït, Collecte, Épître ou Leçon, Évangile, Offertoire, Secrète, Communion et
  Postcommunion (exceptions nommées dans EXCEPTIONS) ; chaque commémoraison, Collecte, Secrète et Postcommunion ;
- chaque formulaire ordinaire porte les attributs lus par l'app : `gloria` et `credo` (booléens) sur `<formulaire>.gloria`
  et `.credo`, `preface` (et `communicantes`, `hancIgitur` s'il y a lieu) sur `.preface.1`, chacun désignant une
  section de l'unité `prefaces` ;
- chaque clé de cles.txt a son unité (`missalUnit()` : minuscules, « / » → « - »), et inversement ;
- l'oracle Missalemeum 2020-2045 est couvert (clés ramenées comme dans GoldenTest.kt) ;
- aucune marque de script de DO dans le texte (`wait10`, `N.b`, `N.p`, `$Per`, `&Gloria`, `@Commune`, `!x!`…) ;
- paquet public : aucun texte français tiré des fichiers français de DO (traduction du Barroux) ; seuls les titres et
  rubriques de Paroissien y sont en français ;
- paquet privé (--private) : mêmes identifiants que le public, typographie française (’, espaces fines).
Code de sortie : 0 si tout va bien, 1 sinon.
"""

from __future__ import annotations

import argparse
import json
import re
import sys
import unicodedata
from collections import Counter
from pathlib import Path

HERE = Path(__file__).resolve().parent
CONTENT = HERE.parents[1]
sys.path.insert(0, str(CONTENT / "pipeline"))
sys.path.insert(0, str(HERE))

from paroissien_content import canon, inlines  # noqa: E402

import keys as K  # noqa: E402

PUB = "missel.1962"
REQUIRED = {"introit", ("epitre", "lecon"), "collecte", "evangile", "offertoire", "secrete", "communion", "postcommunion"}
# Formulaires sans la structure ordinaire : la cérémonie est donnée en entier (« Full text » de DO).
EXCEPTIONS = {
    "tempora-quad6-5": "Vendredi saint : action liturgique (lectures, Passion, oraisons solennelles, adoration de la Croix, communion), sans messe",
    "tempora-quad6-6": "Vigile pascale : cérémonie complète, la messe de la Vigile est dans le texte de la cérémonie",
}
DO_MARKS = re.compile(r"\bwait\d+\b|\bN\.[bp]\b|\$[A-Z][a-z]|&[A-Z][a-z]|@[A-Z][a-z]+/|!x!|^\s*[!_~]|\[[A-Z][a-z]+\]")


def fold(s: str) -> str:
    s = unicodedata.normalize("NFD", s)
    s = "".join(c for c in s if unicodedata.category(c) != "Mn")
    s = s.replace("æ", "ae").replace("œ", "oe").replace("’", "'").replace(" ", " ")
    return re.sub(r"[^a-z0-9]+", " ", s.lower()).strip()


COMMEMORATION_PIECES = ["collecte", "secrete", "postcommunion"]


def commemorations(pub: canon.Publication) -> list[str]:
    return [u.id for u in pub.units() if u.kind == "commemoration"]


def masses(pub: canon.Publication) -> dict[str, list[str]]:
    """Formulaire (préfixe d'identifiant du titre de niveau 1) → pièces (identifiants des titres de niveau 2)."""
    out: dict[str, list[str]] = {}
    for unit in pub.units():
        if unit.id == "prefaces" or unit.kind == "commemoration":
            continue
        blocks = pub.blocks[unit.id]
        prefixes = [b["id"][: -len(".titre")] for b in blocks if b["type"] == "heading" and b.get("level") == 1]
        multi = any(p != unit.id for p in prefixes)
        for p in prefixes:
            if multi and p == unit.id:
                continue  # titre du jour au-dessus des messes multiples
            out[p] = [b["id"][len(p) + 1:] for b in blocks if b["type"] == "heading" and b.get("level") == 2
                      and b["id"].startswith(p + ".") and b["id"].count(".") == p.count(".") + 1]
    return out


def check_pieces(pub: canon.Publication, errors: list[str]) -> dict:
    stats = Counter()
    for prefix, pieces in masses(pub).items():
        stats["formulaires"] += 1
        unit = prefix.split(".")[0]
        if unit in EXCEPTIONS:
            stats["exceptions"] += 1
            continue
        missing = []
        for need in REQUIRED:
            options = need if isinstance(need, tuple) else (need,)
            if not any(o in pieces for o in options):
                missing.append("/".join(options))
        if missing:
            errors.append(f"{prefix} : pièces manquantes {missing}")
        for p in pieces:
            stats[re.sub(r"-\d+$", "", p)] += 1
    return dict(stats)


def check_commemorations(pub: canon.Publication, errors: list[str]) -> int:
    ids = {b["id"] for b in pub.all_blocks()}
    units = commemorations(pub)
    for u in units:
        for piece in COMMEMORATION_PIECES:
            if f"{u}.{piece}" not in ids or f"{u}.{piece}.1" not in ids:
                errors.append(f"{u} : commémoraison sans {piece}")
    return len(units)


def check_attributes(pub: canon.Publication, errors: list[str]) -> Counter:
    """Attributs que lit l'app (gloria, credo, preface, communicantes, hancIgitur), clés de l'unité `prefaces`."""
    blocks = {b["id"]: b for b in pub.all_blocks()}
    sections = {b["id"][len("prefaces."):] for b in pub.blocks.get("prefaces", []) if b["type"] == "heading" and b.get("level") in (2, 3)}
    stats = Counter()
    for prefix in masses(pub):
        if prefix.split(".")[0] in EXCEPTIONS:
            continue
        g, c, p = blocks.get(f"{prefix}.gloria"), blocks.get(f"{prefix}.credo"), blocks.get(f"{prefix}.preface.1")
        if not g or not isinstance(g.get("gloria"), bool):
            errors.append(f"{prefix} : attribut gloria absent")
        if not c or not isinstance(c.get("credo"), bool):
            errors.append(f"{prefix} : attribut credo absent")
        if not p or not p.get("preface"):
            errors.append(f"{prefix} : attribut preface absent")
            continue
        for attr in ("preface", "communicantes", "hancIgitur"):
            if attr in p:
                stats[f"{attr}={p[attr]}"] += 1
                if p[attr] not in sections:
                    errors.append(f"{prefix} : {attr} {p[attr]!r} absent de l'unité prefaces")
        stats["gloria" if g and g.get("gloria") else "sans gloria"] += 1
        stats["credo" if c and c.get("credo") else "sans credo"] += 1
    return stats


def check_keys(pub: canon.Publication, errors: list[str]) -> int:
    keys = [k.strip() for k in (HERE / "cles.txt").read_text(encoding="utf-8").splitlines() if k.strip()]
    if keys != sorted(set(keys)):
        errors.append("cles.txt : doublons ou ordre non trié")
    units = {u.id for u in pub.units()}
    for k in keys:
        if k.lower().replace("/", "-") not in units:
            errors.append(f"cles.txt : {k} sans unité {k.lower().replace('/', '-')}")
    needed = (K.all_keys() | K.commemoration_keys()) - {"Tempora/Nat26", "Tempora/Nat27", "Tempora/Nat28"}
    for k in sorted(needed - set(keys)):
        errors.append(f"clé du calendrier sans formulaire : {k}")
    for k in K.missing_from_oracle(set(keys)):
        errors.append(f"clé de l'oracle non couverte : {k}")
    return len(keys)


def check_marks(pub: canon.Publication, errors: list[str]) -> None:
    for b in pub.all_blocks():
        for lang, value in b.get("t", {}).items():
            text = inlines.plain_text(value)
            for line in text.split("\n"):
                if DO_MARKS.search(line):
                    errors.append(f"{b['id']} ({lang}) : marque de DO {DO_MARKS.search(line).group(0)!r} dans {line[:60]!r}")


def french_corpus(divinum: Path) -> str:
    www = divinum / "web" / "www"
    files = list((www / "missa" / "Francais").rglob("*.txt")) + list((www / "horas" / "Francais" / "Commune").glob("*.txt"))
    return "\n".join(fold(f.read_text(encoding="utf-8")) for f in files)


def check_public_french(pub: canon.Publication, divinum: Path | None, errors: list[str]) -> int:
    """Aucun texte du Propre français de DO dans le paquet public."""
    if divinum is None or not divinum.exists():
        print("(DO absent : contrôle du français public sauté)")
        return 0
    corpus = french_corpus(divinum)
    seen: dict[str, bool] = {}
    checked = 0
    for b in pub.all_blocks():
        fr = b.get("t", {}).get("fr")
        if not fr or b["type"] == "heading" or b.get("style") == "intro":
            continue  # titres : calendrier, pièces, titres des lectures (formules traduites par Paroissien)
        text = fold(inlines.plain_text(fr))
        if len(text) < 40 or text in seen:
            continue
        checked += 1
        seen[text] = text in corpus
        if seen[text]:
            errors.append(f"{b['id']} : texte français présent dans les fichiers français de DO : {text[:60]!r}")
    return checked


def check_private(pub: canon.Publication, priv: canon.Publication, errors: list[str]) -> dict:
    stats = Counter()
    if canon.node_ids(pub) - canon.node_ids(priv):
        missing = sorted(canon.node_ids(pub) - canon.node_ids(priv))
        errors.append(f"privé : {len(missing)} identifiants du public absents, p. ex. {missing[:5]}")
    for b in priv.all_blocks():
        t = b.get("t", {})
        if "fr" in t:
            stats["blocs avec français"] += 1
            text = inlines.plain_text(t["fr"])
            if "'" in text:
                errors.append(f"{b['id']} : apostrophe droite dans le français")
            if re.search(r"[^\s ][;!?]", text) and not re.search(r"\d[;:]|[A-Z][a-z]* \d", text):
                stats["ponctuation sans espace fine"] += 1
        if "la" in t:
            stats["blocs avec latin"] += 1
    return dict(stats)


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--data", type=Path, default=CONTENT / "data")
    parser.add_argument("--private", type=Path, help="dossier data du dépôt privé")
    parser.add_argument("--divinum", type=Path, default=CONTENT / ".cache" / "raw" / "missel" / "divinum-officium")
    parser.add_argument("-v", action="store_true")
    args = parser.parse_args(argv)
    errors: list[str] = []
    pub = canon.load(args.data / PUB)
    errors += canon.validate(pub)
    stats = check_pieces(pub, errors)
    ncomm = check_commemorations(pub, errors)
    attrs = check_attributes(pub, errors)
    nkeys = check_keys(pub, errors)
    check_marks(pub, errors)
    checked = check_public_french(pub, args.divinum, errors)
    types = Counter(b["type"] for b in pub.all_blocks())
    print(f"{PUB} (public) : {len(pub.units())} unités, {stats.get('formulaires')} formulaires "
          f"({stats.get('exceptions', 0)} cérémonies complètes), {sum(types.values())} blocs {dict(types)}, {nkeys} clés")
    print("pièces : " + ", ".join(f"{k} {v}" for k, v in sorted(stats.items()) if k not in ("formulaires", "exceptions")))
    print(f"commémoraisons (oraisons seules) : {ncomm}")
    print("attributs : " + ", ".join(f"{k} {v}" for k, v in sorted(attrs.items())))
    print(f"français public : {checked} textes comparés aux fichiers français de DO")
    if args.private:
        priv = canon.load(args.private / PUB)
        errors += [f"privé : {e}" for e in canon.validate(priv)]
        check_marks(priv, errors)
        pstats = check_private(pub, priv, errors)
        print(f"{PUB} (privé) : {len(priv.all_blocks())} blocs ; {pstats}")
    for e in errors[:80]:
        print("  " + e)
    print(f"{len(errors)} erreur(s)")
    return 1 if errors else 0


if __name__ == "__main__":
    sys.exit(main())
