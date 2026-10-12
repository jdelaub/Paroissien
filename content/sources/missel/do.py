"""Lecture des fichiers de Divinum Officium (MIT) pour le Propre de la messe : sections, conditions, inclusions.

Bibliothèque standard seule. Les fichiers lus sont des données non fiables : on ne les exécute jamais.

Port de `setupstring_parse_file`, `setupstring` et `get_loadtime_inclusion` (web/cgi-bin/DivinumOfficium/
SetupString.pl) restreint à ce dont la messe a besoin :
- conditions évaluées pour « Rubrics 1960 - 1960 » par `divinum.vero` et `divinum.process_conditional_lines`
  (content/sources/calendrier-1962/divinum.py), avec un contexte du jour (`tempore`, `missa`, `feria`…) ;
- inclusion du fichier entier (`@Tempora/Pasc5-4` en tête), puis des sections (`@Sancti/07-22`,
  `@:Evangelium1`, `@Commune/C11::s/^.*?\\s(\\!)//s`), avec les substitutions `s/…/…/` et les sélections de
  lignes `3-5` ou `!2` de `do_inclusion_substitutions` ;
- emplacement des fichiers comme `setupstring_layers` : les Communs (`C1`… `C12`) sont dans horas/, les autres
  fichiers dans missa/, puis horas/ s'ils manquent.
Chaque langue est lue dans son propre arbre (missa/Latin, missa/Francais) : le français absent reste absent, sans
repli sur le latin, pour que le rapport des lacunes soit exact.

`overlay` : dossier de propres transcrits par Paroissien au format de DO (content/sources/missel/propres/<langue>/),
pour les fêtes de France absentes de DO. Une clé présente dans l'overlay (en latin) masque DO dans toutes les langues :
`Sancti/01-03` de DO est l'ancienne octave de saint Jean, pas sainte Geneviève.
"""

from __future__ import annotations

import importlib.util
import re
import sys
from dataclasses import dataclass
from pathlib import Path

HERE = Path(__file__).resolve().parent


def _load_divinum():
    spec = importlib.util.spec_from_file_location("divinum", HERE.parent / "calendrier-1962" / "divinum.py")
    module = importlib.util.module_from_spec(spec)
    sys.modules.setdefault("divinum", module)
    spec.loader.exec_module(module)
    return module


divinum = _load_divinum()

SECTION = re.compile(r"^\s*\[([\w #,:-]+)\](?:\s*(\(.*\)))?", re.U)
INCLUSION = re.compile(r"^\s*@([^\n:]+)?(?::([^\n:]+?))?[^\S\n\r]*(?::(.*))?$")
SUBST = re.compile(r"(?:s/(?P<s>[^/]*)/(?P<r>[^/]*)/(?P<f>[gism]*))|(?:(?P<n>!?)(?P<b>\d+)(-(?P<e>\d+))?)")
CONDITION = re.compile(r"^\(\s*(.*?)\s*\)$")


def _perl_replacement(r: str) -> str:
    """Remplacement Perl (`$1`, `\\n`, `\\$`) → gabarit de `re.sub`."""
    out, i = [], 0
    while i < len(r):
        c = r[i]
        if c == "\\" and i + 1 < len(r):
            nxt = r[i + 1]
            out.append("\n" if nxt == "n" else ("\\\\" if nxt == "\\" else nxt))
            i += 2
            continue
        if c == "$" and i + 1 < len(r) and r[i + 1].isdigit():
            j = i + 1
            while j < len(r) and r[j].isdigit():
                j += 1
            out.append(f"\\g<{r[i + 1:j]}>")
            i = j
            continue
        out.append("\\\\" if c == "\\" else c)
        i += 1
    return "".join(out)


def substitute(text: str, subs: str) -> str:
    """`do_inclusion_substitutions` : `s/motif/remplacement/drapeaux` et sélections de lignes, dans l'ordre."""
    for m in SUBST.finditer(subs or ""):
        if m.group("b"):
            start = int(m.group("b")) - 1
            count = int(m.group("e")) - start if m.group("e") else 1
            lines = text.split("\n")
            picked = lines[start:start + count]
            rest = lines[:start] + lines[start + count:]
            text = "\n".join(rest if m.group("n") else picked) + "\n"
        else:
            flags = 0
            f = m.group("f")
            flags |= re.I if "i" in f else 0
            flags |= re.S if "s" in f else 0
            flags |= re.M if "m" in f else 0
            pattern = m.group("s").replace("\\Z", "\\Z")
            text = re.sub(pattern, _perl_replacement(m.group("r")), text, count=0 if "g" in f else 1, flags=flags)
    return text


@dataclass
class Resolved:
    """Une section résolue : ses lignes et la chaîne des (fichier, section) d'où elle vient, du haut vers le bas."""
    lines: list[str]
    chain: list[tuple[str, str]]


class Library:
    """Arbre de DO pour une langue (`Latin`, `Francais`)."""

    def __init__(self, www: Path, lang: str, overlay: Path | None = None):
        self.www, self.lang, self.overlay = www, lang, overlay
        self.overlay_keys = ({str(f.relative_to(overlay / "Latin").with_suffix("")) for f in (overlay / "Latin").rglob("*.txt")}
                             if overlay and (overlay / "Latin").is_dir() else set())
        self._parsed: dict[tuple, dict | None] = {}
        self._loaded: dict[tuple, tuple[dict, dict] | None] = {}
        self.missing_refs: set[str] = set()

    # ── Fichiers ──────────────────────────────────────────────────────────────────────────────

    def path(self, key: str) -> Path | None:
        if key in self.overlay_keys:
            own = self.overlay / self.lang / f"{key}.txt"
            return own if own.exists() else None
        missa = self.www / "missa" / self.lang / f"{key}.txt"
        horas = self.www / "horas" / self.lang / f"{key}.txt"
        if re.search(r"C\d", key) and horas.exists():
            return horas
        if missa.exists():
            return missa
        if horas.exists():
            return horas
        return None

    def exists(self, key: str) -> bool:
        return self.path(key) is not None

    def relpath(self, key: str) -> str | None:
        """Chemin dans le dépôt de DO ; pour un propre de Paroissien, chemin dans content/ (« content/sources/… »)."""
        p = self.path(key)
        if not p:
            return None
        if key in self.overlay_keys:
            return "content/" + str(p.relative_to(self.overlay.parents[2]))
        return str(p.relative_to(self.www.parent.parent))

    def _parse(self, key: str, ctx: tuple) -> dict | None:
        cache_key = (key, ctx)
        if cache_key in self._parsed:
            return self._parsed[cache_key]
        p = self.path(key)
        if p is None:
            self._parsed[cache_key] = None
            return None
        context = dict(ctx)
        sections: dict[str, list[str]] = {"__preamble": []}
        current, use = "__preamble", True
        for line in p.read_text(encoding="utf-8").splitlines():
            if line.startswith("["):
                m = SECTION.match(line)
                if m:
                    cond = m.group(2)
                    if not cond or divinum.vero(CONDITION.match(cond).group(1), context):
                        current, use = m.group(1), True
                        sections[current] = []
                    else:
                        use = False
                    continue
            if not use:
                continue
            if current != "__preamble" and line.lstrip().startswith("@"):
                im = INCLUSION.match(line)
                if im:
                    f, s, subs = im.groups()
                    line = "@" + (f or key) + ":" + (s or current) + (":" + subs if subs else "")
            sections[current].append(line)
        out = {k: divinum.process_conditional_lines(v, context) for k, v in sections.items()}
        self._parsed[cache_key] = out
        return out

    def load(self, key: str, ctx: tuple = ()) -> tuple[dict, dict] | None:
        """Sections après inclusion du fichier entier, et pour chacune le fichier qui l'a fournie."""
        cache_key = (key, ctx)
        if cache_key in self._loaded:
            return self._loaded[cache_key]
        self._loaded[cache_key] = None  # garde contre les cycles
        own = self._parse(key, ctx)
        if own is None:
            return None
        merged = {k: v for k, v in own.items() if k != "__preamble"}
        origin = {k: key for k in merged}
        for line in own.get("__preamble", []):
            m = re.match(r"^\s*@([^:\s]+)\s*$", line)
            if m:
                base = self.load(m.group(1), ctx)
                if base:
                    for k, v in base[0].items():
                        if k not in merged:
                            merged[k] = v
                            origin[k] = base[1][k]
        result = (merged, origin)
        self._loaded[cache_key] = result
        return result

    # ── Sections ──────────────────────────────────────────────────────────────────────────────

    def has(self, key: str, name: str, ctx: tuple = ()) -> bool:
        loaded = self.load(key, ctx)
        return bool(loaded and name in loaded[0] and any(l.strip() for l in loaded[0][name]))

    def raw(self, key: str, name: str, ctx: tuple = ()) -> list[str] | None:
        loaded = self.load(key, ctx)
        if not loaded or name not in loaded[0]:
            return None
        return loaded[0][name]

    def resolve(self, key: str, name: str, ctx: tuple = ()) -> Resolved | None:
        """Section `name` du fichier `key`, inclusions résolues ; None si elle manque ou est vide."""
        loaded = self.load(key, ctx)
        if not loaded or name not in loaded[0]:
            return None
        lines = loaded[0][name]
        if not any(l.strip() for l in lines):
            return None
        chain = [(key, name)]
        if loaded[1][name] != key:
            chain.append((loaded[1][name], name))
        # Inclusions pures (« @Commune/C4b ») suivies jusqu'au texte : la chaîne sert au français et à x-source.
        content = [l for l in lines if l.strip()]
        for _ in range(8):
            if not (len(content) == 1 and content[0].lstrip().startswith("@")):
                break
            m = INCLUSION.match(content[0])
            if not m or m.group(3):
                break
            chain.append((m.group(1), m.group(2)))
            target = self.load(m.group(1), ctx)
            if not target or m.group(2) not in target[0]:
                break
            content = [l for l in target[0][m.group(2)] if l.strip()]
        out = self._expand(lines, ctx, 0, chain)
        if out is None:
            return None
        return Resolved(out, chain)

    def _expand(self, lines: list[str], ctx: tuple, depth: int, chain: list) -> list[str] | None:
        if depth > 7:
            return None
        out: list[str] = []
        for line in lines:
            if line.lstrip().startswith("@"):
                m = INCLUSION.match(line)
                if not m:
                    out.append(line)
                    continue
                f, s, subs = m.groups()
                target = self.load(f, ctx)
                if not target or s not in target[0]:
                    self.missing_refs.add(f"{self.lang}:{f}:{s}")
                    return None
                text = "\n".join(target[0][s])
                text = re.sub(r"\n+$", "\n", text + "\n")
                if subs:
                    text = substitute(text, subs)
                sub_lines = text.rstrip("\n").split("\n")
                expanded = self._expand(sub_lines, ctx, depth + 1, chain)
                if expanded is None:
                    return None
                out.extend(expanded)
            else:
                out.append(line)
        return out

    # ── Commodités ────────────────────────────────────────────────────────────────────────────

    def rank_fields(self, key: str, ctx: tuple = ()) -> list[str]:
        lines = [l for l in (self.raw(key, "Rank", ctx) or []) if l.strip()]
        return lines[-1].split(";;") if lines else []

    def rule(self, key: str, ctx: tuple = ()) -> list[str]:
        return [l.strip() for l in (self.raw(key, "Rule", ctx) or []) if l.strip()]

    def officium(self, key: str, ctx: tuple = ()) -> str:
        r = self.resolve(key, "Officium", ctx)
        lines = [l.strip() for l in (r.lines if r else []) if l.strip()]
        if lines:
            return lines[0]
        fields = self.rank_fields(key, ctx)
        return fields[0].strip() if fields else ""


def commune_of(lib: Library, key: str, ctx: tuple, paschal: bool) -> tuple[str, str] | None:
    """`extract_common` : (type « ex »/« vide », fichier) d'après le 4ᵉ champ de [Rank]."""
    fields = lib.rank_fields(key, ctx)
    field = fields[3].strip() if len(fields) > 3 else ""
    m = re.match(r"^(ex|vide)\s*(?!Sancti)((?:[a-z\s]*/)?C[0-9]+[a-z]*-*[123]*)", field, re.I)
    if m:
        name = m.group(2)
        if paschal and re.search(r"C\d", name) and lib.exists(f"Commune/{name}p"):
            name += "p"
        return m.group(1), f"Commune/{name}"
    m = re.match(r"^(ex|vide)\s*Sancti/(.*)\s*$", field, re.I)
    if m:
        return m.group(1), f"Sancti/{m.group(2).strip()}"
    m = re.match(r"^(ex|vide)\s*(.*)\s*$", field, re.I)
    if m and m.group(2).strip():
        name = re.sub(r"^Tempora/", "", m.group(2).strip())
        return m.group(1), (name if re.search("Sancti|Commune", name) else f"Tempora/{name}")
    return None
