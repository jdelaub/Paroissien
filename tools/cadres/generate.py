"""
Cadres ornementaux d'après un paroissien chromolithographié de la fin du XIXᵉ siècle. Source unique : chaque cadre est
décrit une fois dans tools/cadres/cadres/<nom>.py (fonction `spec()`, primitives de commun.py), puis exporté en
  - Kotlin : données du composable OrnamentalFrame (core:designsystem, paquet frame), un fichier par cadre, plus
             le registre Cadres.kt,
  - SVG    : un fichier par module, ouvrable dans Inkscape (tools/cadres/svg/<nom>/),
  - HTML   : aperçu autonome du même moteur en JavaScript (tools/cadres/apercu.html).

    python3 -I tools/cadres/generate.py          # régénère les trois sorties
    python3 -I tools/cadres/generate.py --check  # échoue si une sortie commitée n'est plus à jour
"""
import glob
import importlib
import json
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(os.path.dirname(HERE))
sys.path.insert(0, HERE)  # python3 -I n'ajoute pas le dossier du script

FRAME_DIR = os.path.join(ROOT, "android", "core", "designsystem", "src", "main", "kotlin",
                         "app", "paroissien", "core", "designsystem", "frame")


def frames():
    """Les cadres, dans l'ordre du paroissien d'origine (attribut ORDRE de chaque module)."""
    out = []
    for f in sorted(glob.glob(os.path.join(HERE, "cadres", "*.py"))):
        name = os.path.splitext(os.path.basename(f))[0]
        if name.startswith("_"):
            continue
        mod = importlib.import_module("cadres." + name)
        out.append((getattr(mod, "ORDRE", 999), name, mod))
    out.sort(key=lambda x: (x[0], x[1]))
    return [(name, mod.spec()) for _, name, mod in out]


def load(name):
    return importlib.import_module("cadres." + name).spec()


# --------------------------------------------------------------------------- #
# SVG des modules
# --------------------------------------------------------------------------- #
def svg_ops(ops, inks):
    out = []
    for o in ops:
        col = inks.get(o["ink"], "#FBF8F1")
        if "stroke" in o:
            cap = ' stroke-linecap="round" stroke-linejoin="round"' if o.get("round") else ""
            out.append(f'<path d="{o["d"]}" fill="none" stroke="{col}" stroke-width="{o["stroke"]}"{cap}/>')
        else:
            out.append(f'<path d="{o["d"]}" fill="{col}"/>')
    return "\n    ".join(out)


def ground_of(spec, inks):
    return inks[spec["ground"]] if spec["ground"] else "#FBF8F1"


def svg_strip(spec, mod, inks, count=6, scale=2.0):
    P = mod["period"]
    w = P * count
    body = [f'<rect width="{w:.2f}" height="100" fill="{ground_of(spec, inks)}"/>']
    for pass_ in ("base", "over"):
        if not mod[pass_]:
            continue
        for i in range(-1, count + 2):
            body.append(f'<g transform="translate({(i + mod["phase"]) * P:.2f} 0)">\n    {svg_ops(mod[pass_], inks)}\n  </g>')
    return (f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {w:.2f} 100" '
            f'width="{w * scale:.0f}" height="{100 * scale:.0f}">\n'
            f'  <defs><clipPath id="c"><rect width="{w:.2f}" height="100"/></clipPath></defs>\n'
            f'  <g clip-path="url(#c)">\n  ' + "\n  ".join(body) + "\n  </g>\n</svg>\n")


def svg_box(spec, mod, inks, scale=3.0, x0=0.0, y0=0.0, w=None, h=None, bg=None):
    w = mod["w"] if w is None else w
    h = mod["h"] if h is None else h
    bg = ground_of(spec, inks) if bg is None else bg
    return (f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="{x0:.2f} {y0:.2f} {w:.2f} {h:.2f}" '
            f'width="{w * scale:.0f}" height="{h * scale:.0f}">\n'
            f'  <rect x="{x0:.2f}" y="{y0:.2f}" width="{w:.2f}" height="{h:.2f}" fill="{bg}"/>\n  {svg_ops(mod["ops"], inks)}\n</svg>\n')


def svgs(name, spec):
    inks = dict(spec["inks"], paper="#FBF8F1")
    d = os.path.join(HERE, "svg", name)
    out = {}
    seen = set()
    for k, mod in spec["bands"].items():
        if id(mod) in seen:
            continue
        seen.add(id(mod))
        out[os.path.join(d, f"frise_{k}.svg")] = svg_strip(spec, mod, inks)
    for k, mod in spec["centers"].items():
        if mod:
            w = mod["width"]
            out[os.path.join(d, f"centre_{k}.svg")] = svg_box(spec, {"ops": mod["ops"]}, inks, x0=-w / 2, w=w, h=100)
    seen = set()
    for k, mod in spec["corners"].items():
        if mod and id(mod) not in seen:
            seen.add(id(mod))
            out[os.path.join(d, f"coin_{k}.svg")] = svg_box(spec, mod, inks)
    return out


# --------------------------------------------------------------------------- #
# Kotlin
# --------------------------------------------------------------------------- #
def pascal(name):
    return "".join(p[:1].upper() + p[1:] for p in name.replace("-", "_").split("_"))


def camel(name):
    p = pascal(name)
    return p[:1].lower() + p[1:]


def kt_string(s):
    return '"' + s.replace("\\", "\\\\").replace('"', '\\"').replace("$", "\\$") + '"'


def kt_float(x):
    return f"{float(x)}f"


def kt_ops(ops, indent):
    """Les tracés en texte compact (une ligne par tracé : encre, trait, r si bouts ronds, chemin), découpé en
    morceaux de moins de 20 000 caractères : une chaîne constante ne doit pas dépasser 64 Ko dans le .class."""
    if not ops:
        return "emptyList()"
    lines = [f'{o["ink"]} {o.get("stroke", 0)} {"r" if o.get("round") else "-"} {o["d"]}' for o in ops]
    chunks, cur = [], ""
    for ln in lines:
        if cur and len(cur) + len(ln) > 20000:
            chunks.append(cur); cur = ""
        cur += ln + "\n"
    chunks.append(cur)
    body = ",\n".join(f'{indent}    """\n{c}"""' for c in chunks)
    return f"ops(\n{body},\n{indent})"


def kotlin(name, spec):
    P = pascal(name)
    c = camel(name)
    L = spec["layout"]
    decls = []
    ids = {}

    def ref(mod, kind, label):
        if mod is None:
            return "null"
        if id(mod) in ids:
            return ids[id(mod)]
        v = f"{c}{label}"
        ind = "    "
        if kind == "edge":
            decls.append(f"""private val {v} = EdgeModule(
    period = {kt_float(mod["period"])},
    phase = {kt_float(mod["phase"])},
    outset = {kt_float(mod["outset"])},
    inset = {kt_float(mod["inset"])},
    base = {kt_ops(mod["base"], ind)},
    over = {kt_ops(mod["over"], ind)},
)""")
        elif kind == "center":
            decls.append(f"""private val {v} = CenterModule(
    width = {kt_float(mod["width"])},
    outset = {kt_float(mod.get("outset", 0))},
    ops = {kt_ops(mod["ops"], ind)},
)""")
        else:
            decls.append(f"""private val {v} = BoxModule(
    width = {kt_float(mod.get("w", 100))},
    height = {kt_float(mod.get("h", 100))},
    outset = {kt_float(mod.get("outset", 0))},
    ops = {kt_ops(mod["ops"], ind)},
)""")
        ids[id(mod)] = v
        return v

    B, C, K, O = spec["bands"], spec["centers"], spec["corners"], spec["outer"]
    fields = [
        ("top", ref(B["top"], "edge", "Top")),
        ("side", ref(B["side"], "edge", "Side")),
        ("bottom", ref(B["bottom"], "edge", "Bottom")),
        ("topCenter", ref(C["top"], "center", "TopCenter")),
        ("sideCenter", ref(C["side"], "center", "SideCenter")),
        ("bottomCenter", ref(C["bottom"], "center", "BottomCenter")),
        ("corner", ref(K["top"], "box", "Corner")),
        ("bottomCorner", ref(K["bottom"], "box", "BottomCorner")),
        ("cornerFill", ref(K["fill"], "box", "CornerFill")),
        ("outerCorner", ref(O["corner"], "box", "OuterCorner")),
        ("outerBottomCorner", ref(O["cornerBottom"], "box", "OuterBottomCorner")),
        ("outerMid", ref(O["mid"], "box", "OuterMid")),
        ("outerBottomMid", ref(O["midBottom"], "box", "OuterBottomMid")),
        ("outerSideMid", ref(O["midSide"], "box", "OuterSideMid")),
    ]
    bars = spec["bars"]
    if bars:
        layers = ", ".join(f'{kt_string(i)} to {kt_float(w)}' for i, w in bars["layers"])
        term = ref(bars.get("terminal"), "box", "Terminal")
        fields.append(("bars", f"Bars(layers = listOf({layers}), overhang = {kt_float(bars['overhang'])}, "
                               f"terminal = {term}, cornerTerminals = {str(bool(bars.get('cornerTerminals'))).lower()})"))
    else:
        fields.append(("bars", "null"))
    rules = ", ".join(f"Rule({kt_string(r['ink'])}, at = {kt_float(r['at'])}, width = {kt_float(r['width'])}, "
                      f"over = {str(bool(r.get('over', True))).lower()})" for r in spec["rules"])
    fields.append(("rules", f"listOf({rules})" if rules else "emptyList()"))
    fields.append(("ground", kt_string(spec["ground"]) if spec["ground"] else "null"))
    for k, f in (("margin", "margin"), ("top", "topDepth"), ("side", "sideDepth"), ("bottom", "bottomDepth"), ("textPadding", "textPadding")):
        fields.append((f, f"{L[k]}.dp"))

    inks = "\n".join(f"        {kt_string(k)} to {kt_color(v)}," for k, v in spec["inks"].items())
    palettes = "\n".join(
        f"        LiturgicalColor.{n} to mapOf(" + ", ".join(f"{kt_string(k)} to {kt_color(v)}" for k, v in over.items()) + "),"
        for n, over in spec["palettes"].items())
    dimmed = ", ".join(kt_string(k) for k in spec["dimmedInks"])
    body = "\n".join(f"    {k} = {v}," for k, v in fields)
    nl = "\n\n"
    return f"""// Généré par tools/cadres/generate.py : ne pas modifier à la main.
// Source : {spec["source"]}.
package app.paroissien.core.designsystem.frame

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.paroissien.core.liturgy.LiturgicalColor

{nl.join(decls)}

/** Cadre « {spec["title"]} ». */
val {P}Frame = FrameSpec(
    name = {kt_string(name)},
    title = {kt_string(spec["title"])},
{body}
)

/** Encres du cadre : couleurs du livre, remplacements par couleur liturgique, encres atténuées en thème sombre. */
val {P}Style = FrameStyle(
    base = mapOf(
{inks}
    ),
    palettes = {"mapOf(" + chr(10) + palettes + chr(10) + "    )" if palettes else "emptyMap()"},
    dimmed = setOf({dimmed}),
)
"""


def kt_color(hexstr):
    return "Color(0xFF" + hexstr.lstrip("#").upper() + ")"


def registry(all_frames):
    items = "\n".join(f"    OrnamentalFrameDef({kt_string(n)}, {kt_string(s['title'])}, {pascal(n)}Frame, {pascal(n)}Style),"
                      for n, s in all_frames)
    return f"""// Généré par tools/cadres/generate.py : ne pas modifier à la main.
package app.paroissien.core.designsystem.frame

/** Tous les cadres, dans l'ordre du paroissien d'origine. */
val OrnamentalFrames: List<OrnamentalFrameDef> = listOf(
{items}
)
"""


# --------------------------------------------------------------------------- #
# Aperçu HTML
# --------------------------------------------------------------------------- #
def preview_html(all_frames):
    """Aperçu autonome, à ouvrir dans un navigateur : gabarit + moteur JS + spécifications en ligne."""
    tpl = open(os.path.join(HERE, "apercu", "template.html"), encoding="utf-8").read()
    js = open(os.path.join(HERE, "apercu", "frame.js"), encoding="utf-8").read()
    data = json.dumps([s for _, s in all_frames], ensure_ascii=False, separators=(",", ":"))
    page = tpl.replace("/*@SPECS@*/null", data).replace("/*@FRAMEJS@*/", js)
    head, body = page.split("<main", 1)
    return ('<!doctype html>\n<html lang="fr">\n<head>\n<meta charset="utf-8">\n'
            '<meta name="viewport" content="width=device-width, initial-scale=1">\n'
            f"<!-- Généré par tools/cadres/generate.py : ne pas modifier à la main. -->\n{head}</head>\n"
            f"<body>\n<main{body}</body>\n</html>\n")


def outputs():
    all_frames = frames()
    out = {}
    for name, spec in all_frames:
        out[os.path.join(FRAME_DIR, f"{pascal(name)}Frame.kt")] = kotlin(name, spec)
        out.update(svgs(name, spec))
    out[os.path.join(FRAME_DIR, "Cadres.kt")] = registry(all_frames)
    out[os.path.join(HERE, "apercu.html")] = preview_html(all_frames)
    return out


def main():
    check = "--check" in sys.argv[1:]
    stale = []
    for path, content in outputs().items():
        rel = os.path.relpath(path, ROOT)
        if check:
            try:
                current = open(path, encoding="utf-8").read()
            except FileNotFoundError:
                current = None
            if current != content:
                stale.append(rel)
            continue
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, "w", encoding="utf-8") as f:
            f.write(content)
        print("écrit", rel)
    if stale:
        print("Sorties à régénérer (python3 -I tools/cadres/generate.py) :", *stale, sep="\n  ")
        sys.exit(1)


if __name__ == "__main__":
    main()
