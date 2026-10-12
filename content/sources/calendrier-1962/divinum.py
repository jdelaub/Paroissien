"""Lecture des données de Divinum Officium (MIT) pour la version « Rubrics 1960 - 1960 ».

Fonctions pures, bibliothèque standard seule. Les fichiers lus sont des données non fiables :
on ne les exécute jamais, on les lit comme du texte.

- `kalendarium(www)` : compose le Kalendarium de 1960 en suivant la chaîne des bases de
  `Tabulae/data.txt` (1960 → 1955 → 1954 → 1939 → 1906 → 1888 → 1570), comme
  `DivinumOfficium::Directorium::get_from_directorium`.
- `tempora_substitutions(www)` : substitutions de fichiers du temporal pour 1960
  (`Tabulae/Tempora/Generale.txt`, lignes dont la liste de versions contient « 1960 »).
- `Proper(www, key)` : sections d'un propre de messe (`missa/Latin/<key>.txt`), conditions
  `(sed rubrica …)` évaluées pour 1960 comme `SetupString.pl` (`vero`, `process_conditional_lines`).
- `dclass(rank)` : rang numérique de Divinum Officium → classe de 1960 (table `[Festa] (rubrica 196)`
  de `horas/Latin/Psalterium/Comment.txt` : 1 → IV, 2 à 4 → III, 5 → II, 6 et 7 → I ; 4.9, rang
  des Quatre-Temps, → II).

`vero` et `process_conditional_lines` acceptent un `context` facultatif (`{"tempore": "Quadragesimæ",
"missa": 1, …}`) qui donne la valeur des sujets dépendant du jour ; sans lui, ces conditions restent fausses
et sont consignées dans `UNEVALUATED` (comportement du calendrier). Le missel (content/sources/missel) s'en sert.
"""

from __future__ import annotations

import re
from functools import lru_cache
from pathlib import Path

VERSION = "Rubrics 1960 - 1960"  # chaîne testée par les conditions « rubrica … » de Divinum Officium
TRANSFER = "1960"                # colonne « transfer » de data.txt pour cette version

# ── Kalendarium ──────────────────────────────────────────────────────────────────────────────


def _data_versions(www: Path) -> dict[str, tuple[str, str]]:
    """version → (fichier de Kalendarium, version de base), première section de data.txt."""
    versions: dict[str, tuple[str, str]] = {}
    lines = (www / "Tabulae" / "data.txt").read_text(encoding="utf-8").splitlines()
    for line in lines[1:]:
        if line.startswith("#"):
            break  # la suite concerne les anciens noms de la messe
        fields = line.split(",")
        name, kal = fields[0], fields[1]
        base = fields[4] if len(fields) > 4 else ""
        versions[name] = (kal, base)
    return versions


def kalendar_chain(www: Path, version: str = VERSION) -> list[str]:
    versions = _data_versions(www)
    chain = []
    while version:
        kal, base = versions[version]
        chain.append(kal)
        version = base
    return chain


def kalendarium(www: Path, version: str = VERSION) -> dict[str, dict]:
    """MM-DD → {"files": [clé, …] (vide si XXXXX), "layer": fichier de Kalendarium qui la fournit,
    "titles": libellés informatifs du Kalendarium}."""
    days: dict[str, dict] = {}
    for kal in kalendar_chain(www, version):
        for line in (www / "Tabulae" / "Kalendaria" / f"{kal}.txt").read_text(encoding="utf-8").splitlines():
            if "=" not in line or line.startswith("#") or line.startswith("*"):
                continue
            fields = line.split("=")
            day = fields[0].strip()
            if day in days:
                continue  # la couche la plus récente l'emporte
            files = fields[1].strip()
            days[day] = {
                "files": [] if files == "XXXXX" else files.split("~"),
                "layer": kal,
                "titles": [t for t in fields[2::2] if t and t != "XXXXX"],
            }
    return days


def tempora_substitutions(www: Path, transfer: str = TRANSFER) -> dict[str, str]:
    """« Tempora/Pent02-0 » → « Tempora/Pent02-0r » pour 1960."""
    subs: dict[str, str] = {}
    for line in (www / "Tabulae" / "Tempora" / "Generale.txt").read_text(encoding="utf-8").splitlines():
        if not line.strip() or "=" not in line:
            continue
        rule, _, versions = line.partition(";;")
        if versions and not re.search(transfer, versions):
            continue
        src, dst = rule.split("=", 1)
        if src.startswith("Tempora/"):
            subs[src.strip()] = dst.strip()
    return subs


# ── Conditions (port de SetupString.pl) ─────────────────────────────────────────────────────

_STOPWORD_WEIGHTS = {"sed": 1, "vero": 1, "atque": 2, "attamen": 3, "si": 0, "deinde": 1}
_BACKSCOPED = {"sed", "vero", "atque", "attamen"}
_STOPWORDS = "|".join(_STOPWORD_WEIGHTS)
_SCOPE = r"""
    (?:\bloco\s+(?:hu[ij]us\s+versus|horum\s+versuum)\b)?
    \s*
    (?:\b(?:
        (?:dicitur|dicuntur)(?:\s+semper)?
      | (?:hic\s+versus\s+)?omittitur
      | (?:hoc\s+versus\s+)?omittitur
      | (?:hæc\s+versus\s+)?omittuntur
      | (?:hi\s+versus\s+)?omittuntur
      | (?:haec\s+versus\s+)?omittuntur
    )\b)?
"""
_CONDITIONAL = rf"\(\s*((?:{_STOPWORDS})\b)*(.*?)({_SCOPE})?\s*\)"
_CONDITIONAL_LINE = re.compile(rf"^\s*{_CONDITIONAL}\s*(.*)$", re.I | re.X)
_SECTION = re.compile(rf"^\s*\[([\w #,:-]+)\](?:\s*{_CONDITIONAL})?", re.I | re.X)

SCOPE_NULL, SCOPE_LINE, SCOPE_CHUNK, SCOPE_NEST = 0, 1, 2, 3

# Sujets et prédicats de `vero` évaluables hors contexte d'un jour donné. Un sujet inconnu
# (tempore, feria, mense…) dépend du jour : on le signale au lieu de deviner.
_SUBJECTS = {"rubricis": VERSION, "rubrica": VERSION, "communi": VERSION, "ad": "missam"}
_KNOWN_DAY_SUBJECTS = {"tempore", "missa", "die", "feria", "commune", "votiva", "officio", "mense", "dioecesis", "tonus", "toni"}
_PREDICATES = {
    "paschali": lambda s: re.search("Paschæ|Ascensionis|Octava Pentecostes", s, re.I) is not None,
    "post septuagesimam": lambda s: re.search("Septua|Quadra|Passio", s, re.I) is not None,
    "prima": lambda s: s == "1",
    "secunda": lambda s: s == "2",
    "tertia": lambda s: s == "3",
    "longior": lambda s: s == "1",
    "brevior": lambda s: s == "2",
    "feriali": lambda s: re.search("feria|vigilia", s, re.I) is not None,
    "tridentina": lambda s: re.search("Trident", s) is not None,
    "monastica": lambda s: re.search("Monastic", s) is not None,
    "innovata": lambda s: re.search("2020 USA|NewCal", s, re.I) is not None,
    "innovatis": lambda s: re.search("2020 USA|NewCal", s, re.I) is not None,
    "summorum pontificum": lambda s: re.search(r"194[2-9]]|195[45]|196", s) is not None,
}

UNEVALUATED: set[str] = set()  # conditions dépendant du jour rencontrées (pour le rapport)


def vero(condition: str, context: dict | None = None) -> bool:
    condition = condition.strip()
    if not condition:
        return True
    for alternative in re.split(r"\baut\b", condition):
        negation = False
        ok = True
        for part in re.split(r"\b(et|nisi)\b", alternative):
            if part == "nisi":
                negation = True
                continue
            if part == "et":
                continue
            part = re.sub(r"\s+", " ", part.strip())
            if not part:
                continue  # « nisi rubrica 196 » : le champ vide avant « nisi » ne compte pas
            subject, _, predicate = part.partition(" ")
            if not predicate:
                subject, predicate = "", subject
            if subject and subject.lower() not in _SUBJECTS and subject.lower() not in _KNOWN_DAY_SUBJECTS:
                subject, predicate = "", f"{subject} {predicate}"
            subject = (subject or "tempore").lower()
            if context is not None and subject in context:
                value = str(context[subject])
            elif subject in _SUBJECTS:
                value = _SUBJECTS[subject]
            else:
                UNEVALUATED.add(condition)
                ok = False
                break
            test = _PREDICATES.get(predicate.lower())
            result = test(value) if test else re.search(predicate, value, re.I) is not None
            if not (result ^ negation):
                ok = False
                break
        if ok:
            return True
    return False


def _parse_conditional(stopwords: str, condition: str, scope: str, context: dict | None = None):
    words = stopwords.lower().split()
    strength = sum(_STOPWORD_WEIGHTS.get(w, 0) for w in words)
    result = vero(condition, context)
    implicit_backscope = any(w in _BACKSCOPED for w in words)
    if re.search("versuum|omittuntur", scope, re.I):
        backscope = SCOPE_NEST
    elif re.search("versus|omittitur", scope, re.I):
        backscope = SCOPE_CHUNK
    elif not re.search("semper", scope, re.I) and implicit_backscope:
        backscope = SCOPE_LINE
    else:
        backscope = SCOPE_NULL
    if re.search("omittitur|omittuntur", scope, re.I):
        forwardscope = SCOPE_NULL
    elif re.search("dicuntur", scope, re.I):
        forwardscope = SCOPE_CHUNK if backscope == SCOPE_CHUNK else SCOPE_NEST
    else:
        forwardscope = SCOPE_CHUNK if backscope in (SCOPE_CHUNK, SCOPE_NEST) else SCOPE_LINE
    return strength, result, backscope, forwardscope


def process_conditional_lines(lines: list[str], context: dict | None = None) -> list[str]:
    """Port ligne à ligne de `process_conditional_lines` (SetupString.pl)."""
    AFFIRMATIVE, NOT_YET, DUMMY = 1, 0, 2
    blank = re.compile(r"^\s*_?\s*$")
    output: list[str] = []
    stack = [[AFFIRMATIVE, SCOPE_NEST]]
    offsets = [-1]
    for line in lines:
        m = _CONDITIONAL_LINE.match(line)
        if m:
            strength, result, backscope, forwardscope = _parse_conditional(m.group(1) or "", m.group(2) or "", m.group(3) or "", context)
            line = m.group(4)
            last = len(offsets) - 1
            if stack[-1][0] == AFFIRMATIVE or strength >= last:
                if strength >= last:
                    stack = []
                elif strength >= last - (len(stack) - 1):
                    del stack[last - strength:]
                if result:
                    fence = offsets[strength] if last >= strength else -1
                    if backscope == SCOPE_LINE:
                        if len(output) - 1 > fence:
                            output.pop()
                    elif backscope == SCOPE_CHUNK:
                        while len(output) - 1 > fence and not blank.match(output[-1]):
                            output.pop()
                        while len(output) - 1 > fence and blank.match(output[-1]):
                            output.pop()
                    elif backscope == SCOPE_NEST:
                        del output[fence + 1:]
                if forwardscope == SCOPE_NULL:
                    forwardscope = SCOPE_NEST
                    result = True
                if result:
                    while len(offsets) <= strength:
                        offsets.append(-1)
                    for i in range(strength + 1):
                        offsets[i] = len(output) - 1
                while strength < len(offsets) - 1 - (len(stack) - 1) - 1:
                    stack.append([DUMMY, forwardscope])
                stack.append([AFFIRMATIVE if result else NOT_YET, forwardscope])
            if not line:
                continue
        line = re.sub(r"^~", "", line)
        if stack[-1][0] == AFFIRMATIVE:
            output.append(line)
        while stack[-1][1] == SCOPE_LINE or (stack[-1][1] == SCOPE_CHUNK and blank.match(line)):
            while True:
                stack.pop()
                if not (stack and stack[-1][0] == DUMMY):
                    break
            if not stack:
                stack.append([AFFIRMATIVE, SCOPE_NEST])
    return output


# ── Propres ─────────────────────────────────────────────────────────────────────────────────


@lru_cache(maxsize=None)
def _parse_file(path: Path) -> dict[str, list[str]] | None:
    if not path.exists():
        return None
    sections: dict[str, list[str]] = {"__preamble": []}
    key, use = "__preamble", True
    for line in path.read_text(encoding="utf-8").splitlines():
        if line.startswith("["):
            m = _SECTION.match(line)
            if m:
                condition = m.group(3)
                if not condition or vero(condition):
                    key, use = m.group(1), True
                    sections[key] = []
                else:
                    use = False
                continue
        if use:
            sections[key].append(line)
    return {k: process_conditional_lines(v) for k, v in sections.items()}


class Proper:
    """Un fichier de Divinum Officium, clé « Sancti/03-07 », « Tempora/Pent19-0 », « Commune/C10a »…,
    lu dans `missa/` (propres de messe) ou `horas/` (office, dont le [Rank] porte les conditions
    « (rubrica 196) » que la messe n'a pas toujours)."""

    def __init__(self, www: Path, key: str, tree: str = "missa", lang: str = "Latin"):
        self.www, self.key, self.tree, self.lang = www, key, tree, lang
        self.path = www / tree / lang / f"{key}.txt"
        self.sections = self._load(key, set())

    @property
    def exists(self) -> bool:
        return self.sections is not None

    def _load(self, key: str, seen: set[str]) -> dict[str, list[str]] | None:
        if key in seen:
            return None
        seen = seen | {key}
        own = _parse_file(self.www / self.tree / self.lang / f"{key}.txt")
        if own is None:
            return None
        merged = dict(own)
        # Inclusion du fichier entier : « @Sancti/12-28 » en tête de fichier.
        for line in own.get("__preamble", []):
            m = re.match(r"^\s*@([^:\s]+)\s*$", line)
            if m:
                base = self._load(m.group(1), seen) or {}
                for k, v in base.items():
                    merged.setdefault(k, v)
        # Sections réduites à une référence : « @Sancti/12-29 » ou « @Sancti/12-29:Oratio ».
        for name, lines in list(merged.items()):
            content = [l for l in lines if l.strip()]
            if len(content) == 1 and content[0].startswith("@"):
                ref = content[0][1:].strip()
                target, _, section = ref.partition(":")
                if ":" in section:  # substitutions éventuelles ignorées
                    section = section.split(":", 1)[0]
                other = self._load(target or key, seen) if target != key else merged
                if other and (section or name) in other:
                    merged[name] = other[section or name]
        return merged

    def section(self, name: str) -> list[str]:
        if not self.sections:
            return []
        return [l for l in self.sections.get(name, []) if l.strip()]

    @property
    def rank_fields(self) -> list[str]:
        lines = self.section("Rank")
        return lines[-1].split(";;") if lines else []

    @property
    def rank(self) -> float | None:
        fields = self.rank_fields
        try:
            return float(fields[2])
        except (IndexError, ValueError):
            return None

    @property
    def rank_name(self) -> str:
        fields = self.rank_fields
        return fields[1].strip() if len(fields) > 1 else ""

    @property
    def commune(self) -> str:
        """« vide C5a » / « ex C11 » / « ex Sancti/01-06 » du champ 4 du rang, ou de [Rule]."""
        fields = self.rank_fields
        if len(fields) > 3 and fields[3].strip():
            return fields[3].strip()
        for line in self.section("Rule"):
            m = re.match(r"^\s*((?:vide|ex)\s+\S+?);?\s*$", line)
            if m:
                return m.group(1)
        return ""

    @property
    def title(self) -> str:
        """Titre latin : [Officium], sinon premier champ de [Rank]."""
        officium = self.section("Officium")
        if officium:
            return officium[0].strip()
        fields = self.rank_fields
        return fields[0].strip() if fields else ""


def dclass(rank: float | None) -> str | None:
    """Rang numérique → classe de 1960 (`[Festa] (rubrica 196)` : index = partie entière)."""
    if rank is None:
        return None
    if rank == 4.9:
        return "II"  # Quatre-Temps : `$rank == 4.9 ? 5` dans rankname() (horascommon.pl)
    i = int(rank)
    return {0: None, 1: "IV", 2: "III", 3: "III", 4: "III", 5: "II", 6: "I", 7: "I"}.get(i)
