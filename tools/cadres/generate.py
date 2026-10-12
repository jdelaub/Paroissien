"""
Cadre ornemental « Pour la confession », d'après un paroissien chromolithographié de la fin
du XIXᵉ siècle (p.103). Source unique : la géométrie est décrite ici une fois, puis exportée en
  - Kotlin : données du composable OrnamentalFrame (core:designsystem, paquet frame),
  - SVG    : un fichier par module, ouvrable dans Inkscape (tools/cadres/svg),
  - HTML   : aperçu autonome du même moteur en JavaScript (tools/cadres/apercu.html).

    python3 -I tools/cadres/generate.py          # régénère les trois sorties
    python3 -I tools/cadres/generate.py --check  # échoue si une sortie commitée n'est plus à jour

Conventions
  - Un module de frise est décrit dans un repère local : u le long de la bande,
    v en profondeur, de 0 (bord extérieur) à 100 (bord intérieur, côté texte).
    100 unités = épaisseur de la bande, quelle que soit sa taille à l'écran.
  - Chaque opération de dessin = (encre, chemin SVG, épaisseur de trait ou remplissage).
    L'encre est un nom (ground, accent, gold...) : la couleur est résolue à l'exécution,
    d'où les palettes liturgiques et l'atténuation dans les thèmes sombres.
  - Les frises ont deux passes : `base` puis `over`. Toutes les instances dessinent
    leur passe `base`, puis toutes leur passe `over` (les dessus d'entrelacs).
"""
import json
import math
import os
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
HERE = os.path.join(ROOT, "tools", "cadres")
KOTLIN_OUT = os.path.join(
    ROOT, "android", "core", "designsystem", "src", "main", "kotlin",
    "app", "paroissien", "core", "designsystem", "frame", "ConfessionFrame.kt")

# --------------------------------------------------------------------------- #
# Encres du cadre (mesurées sur la photo, corrigées par rapport au blanc du papier).
# Le papier, l'encre du texte et la rubrique viennent des thèmes (PaperColors.kt).
# --------------------------------------------------------------------------- #
INKS = {
    "ground": "#2E4865",   # bleu nuit du fond des frises
    "accent": "#D8745A",   # saumon des lunettes
    "leaf": "#8EABBA",     # bleu clair des palmettes
    "gold": "#B89C5A",     # or (ocre métallique)
    "strand": "#D3CEC1",   # argent des entrelacs
    "bar": "#D4958A",      # baguettes roses
    "barLight": "#F3DDD3", # filet clair au centre des baguettes
    "outline": "#33261F",  # contour brun foncé
}

# Encres remplacées selon la couleur liturgique (core:liturgy LiturgicalColor) ; sans couleur
# liturgique, le cadre garde les couleurs du livre.
PALETTES = {
    "GREEN":  {"ground": "#2F5A44", "leaf": "#A8C29A"},
    "VIOLET": {"ground": "#4A3163", "leaf": "#B6A3CF"},
    "RED":    {"ground": "#7A2626", "accent": "#E0A25A", "leaf": "#D9B88F"},
    "ROSE":   {"ground": "#9C4F67", "accent": "#F0C3A6", "leaf": "#F2D8DA"},
    "WHITE":  {"ground": "#EFE3C6", "strand": "#B89C5A"},
    "BLACK":  {"ground": "#2A2826", "accent": "#8C8379", "leaf": "#A3A09A", "gold": "#9E9A90"},
    "GOLD":   {"ground": "#5A4720", "strand": "#E2CF9C", "leaf": "#C9B27A"},
}

# Dans un thème sombre, ces encres sont rapprochées du fond de page (FrameStyle.ornamentDim)
# pour que le cadre n'éblouisse pas ; le contour reste tel quel.
DIMMED_INKS = ["ground", "accent", "leaf", "gold", "strand", "bar", "barLight"]

# Pour l'aperçu web seulement : fond, encre et rubrique recopiés de PaperColors.kt,
# atténuation recopiée de FrameStyle.ornamentDim.
PAPER_THEMES = {
    "CLAIR":  {"background": "#FBF8F1", "ink": "#1F1B16", "rubric": "#A31F1A", "dark": False, "ornamentDim": 0.0},
    "SEPIA":  {"background": "#F2E8D3", "ink": "#2E2418", "rubric": "#962216", "dark": False, "ornamentDim": 0.0},
    "SOMBRE": {"background": "#17140F", "ink": "#E6DED0", "rubric": "#E88A78", "dark": True, "ornamentDim": 0.28},
    "NUIT":   {"background": "#000000", "ink": "#9A7F5C", "rubric": "#C2604F", "dark": True, "ornamentDim": 0.6},
}

# Épaisseurs (unités de module : 100 = épaisseur de bande)
STRAND = 9.0      # largeur d'un ruban d'entrelacs
OUTLINE = 2.2     # contour de chaque côté
LEAF_OUT = 1.6    # contour des feuilles et fleurons


# --------------------------------------------------------------------------- #
# Géométrie : chemins = listes de commandes, transformées puis sérialisées
# --------------------------------------------------------------------------- #
class Path:
    def __init__(self):
        self.cmds = []

    def M(self, p):
        self.cmds.append(("M", p)); return self

    def L(self, p):
        self.cmds.append(("L", p)); return self

    def C(self, a, b, c):
        self.cmds.append(("C", a, b, c)); return self

    def Z(self):
        self.cmds.append(("Z",)); return self

    def extend(self, other):
        self.cmds.extend(other.cmds); return self

    def transformed(self, f):
        q = Path()
        for c in self.cmds:
            q.cmds.append((c[0],) + tuple(f(p) for p in c[1:]))
        return q

    def normalized(self):
        """Oriente chaque sous-chemin fermé dans le même sens : avec la règle de
        remplissage nonzero, les recouvrements s'additionnent au lieu de s'annuler
        (sinon une forme en miroir perce un trou dans sa voisine)."""
        subs, cur = [], []
        for c in self.cmds:
            if c[0] == "M" and cur:
                subs.append(cur); cur = []
            cur.append(c)
        if cur:
            subs.append(cur)
        q = Path()
        for s in subs:
            if s[-1][0] != "Z":
                q.cmds.extend(s); continue
            pts = [p for c in s for p in c[1:]]
            area = sum(a[0] * b[1] - b[0] * a[1] for a, b in zip(pts, pts[1:] + pts[:1]))
            if area >= 0:
                q.cmds.extend(s); continue
            start = s[0][1]
            segs = [c for c in s[1:] if c[0] != "Z"]
            if segs[-1][-1] != start:
                segs.append(("L", start))
            ends = [start] + [c[-1] for c in segs]
            q.M(ends[-1])
            for i in range(len(segs) - 1, -1, -1):
                c = segs[i]
                if c[0] == "L":
                    q.L(ends[i])
                else:
                    q.C(c[2], c[1], ends[i])
            q.Z()
        return q

    def d(self):
        out = []
        fmt = lambda p: f"{p[0]:.2f} {p[1]:.2f}".replace(".00", "")
        for c in self.cmds:
            if c[0] == "Z":
                out.append("Z")
            else:
                out.append(c[0] + " ".join(fmt(p) for p in c[1:]))
        return "".join(out)


def affine(tx=0.0, ty=0.0, rot=0.0, sx=1.0, sy=None):
    """Point local -> rotation (degrés) + échelle + translation."""
    sy = sx if sy is None else sy
    ca, sa = math.cos(math.radians(rot)), math.sin(math.radians(rot))
    def f(p):
        x, y = p[0] * sx, p[1] * sy
        return (tx + x * ca - y * sa, ty + x * sa + y * ca)
    return f


def mirror_x(p):
    return (-p[0], p[1])


def arc(cx, cy, r, a0, a1, start=True, path=None):
    """Arc de cercle (degrés, repère écran y vers le bas) en cubiques <= 90°."""
    path = path or Path()
    n = max(1, int(math.ceil(abs(a1 - a0) / 90.0 - 1e-9)))
    step = (a1 - a0) / n
    pt = lambda a: (cx + r * math.cos(math.radians(a)), cy + r * math.sin(math.radians(a)))
    if start:
        path.M(pt(a0))
    for i in range(n):
        b0 = a0 + i * step
        b1 = b0 + step
        k = 4.0 / 3.0 * math.tan(math.radians(b1 - b0) / 4.0)
        p0, p3 = pt(b0), pt(b1)
        t0 = (-math.sin(math.radians(b0)), math.cos(math.radians(b0)))
        t1 = (-math.sin(math.radians(b1)), math.cos(math.radians(b1)))
        p1 = (p0[0] + k * r * t0[0], p0[1] + k * r * t0[1])
        p2 = (p3[0] - k * r * t1[0], p3[1] - k * r * t1[1])
        path.C(p1, p2, p3)
    return path


def circle(cx, cy, r):
    return arc(cx, cy, r, 0, 360).Z()


def teardrop(L, W):
    """Goutte / pétale pointant vers +x, base en (0,0)."""
    return (Path().M((0, 0))
            .C((0.22 * L, -0.62 * W), (0.72 * L, -0.52 * W), (L, 0))
            .C((0.72 * L, 0.52 * W), (0.22 * L, 0.62 * W), (0, 0)).Z())


def palmette(L):
    """Palmette à trois lobes pointant vers +x."""
    p = Path()
    p.extend(teardrop(L, 0.42 * L))
    for ang, k in ((-40, 0.74), (40, 0.74)):
        p.extend(teardrop(L * k, 0.40 * L).transformed(affine(rot=ang)))
    p.extend(circle(0.04 * L, 0, 0.12 * L))
    return p


def trefoil(size):
    """Bourgeon trilobé pointant vers -y (haut), base en (0,0), hauteur ≈ size."""
    s = size / 30.0
    p = Path()
    p.extend(circle(0, -21 * s, 7.5 * s))
    p.extend(circle(-8.5 * s, -11 * s, 6.5 * s))
    p.extend(circle(8.5 * s, -11 * s, 6.5 * s))
    p.extend(circle(0, -12 * s, 5.5 * s))
    p.extend(Path().M((-4.5 * s, -8 * s)).L((4.5 * s, -8 * s)).L((0, 2 * s)).Z())
    return p


def fleur_de_lis(height):
    """Fleur de lys pointant vers -y, base en (0,0), hauteur = height."""
    s = height / 100.0
    right = (Path().M((5, -36))
             .C((10, -54), (25, -64), (31, -53))
             .C((35, -46), (33, -38), (26, -36))
             .C((28, -42), (25, -47), (20, -45))
             .C((14, -42), (10, -36), (9, -30)).Z())
    p = Path()
    p.extend(Path().M((0, -100))
             .C((11, -86), (16, -66), (12, -50))
             .C((10, -42), (7, -37), (5, -34))
             .L((-5, -34))
             .C((-7, -37), (-10, -42), (-12, -50))
             .C((-16, -66), (-11, -86), (0, -100)).Z())
    p.extend(right)
    p.extend(right.transformed(mirror_x))
    p.extend(Path().M((-15, -35)).L((15, -35)).L((15, -26)).L((-15, -26)).Z())
    p.extend(Path().M((-7, -26)).C((-8, -16), (-4, -7), (0, 0)).C((4, -7), (8, -16), (7, -26)).Z())
    return p.transformed(affine(sx=s))


# --------------------------------------------------------------------------- #
# Opérations de dessin
# --------------------------------------------------------------------------- #
def fill(ink, path):
    return {"ink": ink, "d": path.normalized().d()}


def stroke(ink, path, width):
    return {"ink": ink, "d": path.d(), "stroke": round(width, 2)}


def outlined(ink, path, outline=LEAF_OUT):
    """Forme remplie avec contour : le trait large dessous, le remplissage dessus
    → seul le contour extérieur de l'union reste visible."""
    return [stroke("outline", path, 2 * outline), fill(ink, path)]


def strand(path, width=STRAND):
    return [stroke("outline", path, width + 2 * OUTLINE), stroke("strand", path, width)]


# --------------------------------------------------------------------------- #
# Module 1 — « arcature » : arcades romanes entrelacées (bandes haute et latérales)
# --------------------------------------------------------------------------- #
def module_arcade():
    R = 78.0          # rayon des arcs = pas nominal
    P = R
    vb = 92.0         # ligne de naissance des arcs (près du bord intérieur)
    # croisement entre l'arc 0 (centre u=0) et l'arc +1 (centre u=P)
    vc = vb - math.sqrt(R * R - (P / 2) ** 2)
    ang = math.degrees(math.atan2(vc - vb, P / 2))           # angle du croisement vu de l'arc 0 (≈ -60°)
    ang1 = math.degrees(math.atan2(vc - vb, -P / 2))         # vu de l'arc +1 (≈ -120°)

    base, over = [], []

    # lunette (intérieur commun des arcs 0 et +1) : saumon
    lens = Path().M((0, vb))
    arc(P, vb, R, 180, ang1 + 360, start=False, path=lens)
    arc(0, vb, R, ang + 360, 360, start=False, path=lens)
    lens.L((P, 100)).L((0, 100)).Z()
    base.append(fill("accent", lens))

    # fleur de lys dans la lunette, pointe vers l'extérieur
    base += outlined("gold", fleur_de_lis(46).transformed(affine(P / 2, vb - 2)))

    # palmette bleue sous la clé de l'arc 0, pointant vers l'extérieur
    base += outlined("leaf", palmette(30).transformed(affine(0, 58, rot=-90)))
    # petit bourgeon d'or dans l'écoinçon extérieur entre deux arcs
    base += outlined("gold", trefoil(13).transformed(affine(P / 2, 2, rot=180)))

    # l'arc lui-même, prolongé jusqu'au bord intérieur
    a = Path().M((-R, 104)).L((-R, vb))
    arc(0, vb, R, 180, 360, start=False, path=a)
    a.L((R, 104))
    base += strand(a)

    # pas de passe « over » : l'arc k+1, dessiné après l'arc k, passe dessus à leur
    # croisement et dessous au suivant -> l'alternance de l'entrelacs est automatique.
    return {"period": P, "phase": 0.0, "base": base, "over": over}


# --------------------------------------------------------------------------- #
# Module 2 — « chaîne » : anneaux entrelacés (bande du pied, plus haute)
# --------------------------------------------------------------------------- #
def module_chain():
    r = 36.0
    P = 61.0
    cy = 50.0
    h = math.sqrt(r * r - (P / 2) ** 2)
    top, bot = (P / 2, cy - h), (P / 2, cy + h)
    a_top0 = math.degrees(math.atan2(top[1] - cy, top[0]))          # vu de l'anneau 0
    a_bot0 = math.degrees(math.atan2(bot[1] - cy, bot[0]))

    base, over = [], []
    base.append(fill("accent", circle(0, cy, r)))

    # palmettes bleues horizontales, fleur de lys en haut, goutte d'or en bas
    base += outlined("leaf", palmette(19).transformed(affine(3, cy + 1)))
    base += outlined("leaf", palmette(19).transformed(affine(-3, cy + 1, rot=180)))
    base += outlined("gold", fleur_de_lis(25).transformed(affine(0, cy - 4)))
    base += outlined("gold", teardrop(17, 9).transformed(affine(0, cy + 6, rot=90)))

    # bourgeons d'or dans les écoinçons haut et bas entre deux anneaux
    base += outlined("gold", trefoil(12).transformed(affine(P / 2, 1.5, rot=180)))
    base += outlined("gold", trefoil(12).transformed(affine(P / 2, 98.5)))

    base += strand(circle(0, cy, r))

    # en bas, l'anneau +1 (dessiné après) passe dessus naturellement ; en haut, on
    # redessine un tronçon de l'anneau 0. Le contour est plus court que le ruban :
    # ses extrémités restent cachées sous l'argent, sans liseré visible.
    over.append(stroke("outline", arc(0, cy, r, a_top0 - 16, a_top0 + 16), STRAND + 2 * OUTLINE))
    over.append(stroke("strand", arc(0, cy, r, a_top0 - 20, a_top0 + 20), STRAND))

    return {"period": P, "phase": 0.5, "base": base, "over": over}


# --------------------------------------------------------------------------- #
# Module 3 — rosace de coin (carré 100 × 100)
# --------------------------------------------------------------------------- #
def module_rosette():
    c = 50.0
    ops = []
    # feuilles bleues dans les angles, pointant vers le centre
    for k in range(4):
        a = 45 + 90 * k
        ca, sa = math.cos(math.radians(a)), math.sin(math.radians(a))
        ops += outlined("leaf", palmette(15).transformed(affine(c + 61 * ca, c + 61 * sa, rot=a + 180)))
    ops.append(fill("ground", circle(c, c, 36)))
    # quatre pétales saumon en croix, quatre feuilles d'or en diagonale
    petals = Path()
    for k in range(4):
        petals.extend(teardrop(31, 25).transformed(affine(c, c, rot=90 * k)))
    ops += outlined("accent", petals)
    golds = Path()
    for k in range(4):
        golds.extend(teardrop(27, 11).transformed(affine(c, c, rot=45 + 90 * k)))
    ops += outlined("gold", golds)
    ops += outlined("gold", circle(c, c, 6.5))
    ops += strand(circle(c, c, 38))
    return {"size": 100, "ops": ops}


def module_fleur():
    """Fleur de lys d'or pour combler les coins rectangulaires (boîte 100 × 100, pointe en haut)."""
    ops = outlined("gold", fleur_de_lis(80).transformed(affine(50, 90)))
    ops = outlined("leaf", palmette(24).transformed(affine(46, 84, rot=150))) + \
          outlined("leaf", palmette(24).transformed(affine(54, 84, rot=30))) + ops
    return {"size": 100, "ops": ops}


# --------------------------------------------------------------------------- #
# Module 4 — terminaison de baguette (anneau rose + bourgeon d'or), pointe vers +x
# --------------------------------------------------------------------------- #
def module_terminal():
    ops = []
    ring = circle(7, 0, 6)
    ops.append(stroke("outline", ring, 4 + 2 * 1.8))
    ops.append(stroke("bar", ring, 4))
    ops += outlined("gold", trefoil(30).transformed(affine(13, 0, rot=90)))
    return {"length": 46, "ops": ops}


# --------------------------------------------------------------------------- #
# Mise en page par défaut (dp) — reprise telle quelle par l'aperçu et par Compose
# --------------------------------------------------------------------------- #
LAYOUT = {
    "margin": 13,        # espace extérieur pour les terminaisons
    "top": 20,           # épaisseur bande haute
    "side": 20,          # épaisseur bandes latérales
    "bottom": 34,        # bande du pied, plus haute (convention des manuscrits)
    "bar": 7,            # largeur de baguette (unités de module)
    "barOutline": 1.8,
    "barLight": 1.6,
    "barOverhang": 14,   # dépassement des baguettes intérieures avant la terminaison (unités)
    "textPadding": 14,
}


def frame_spec():
    return {
        "name": "confession",
        "source": "paroissien chromolithographié, fin du XIXᵉ siècle, p.103 « Pour la confession »",
        "inks": INKS,
        "palettes": PALETTES,
        "dimmedInks": DIMMED_INKS,
        "paperThemes": PAPER_THEMES,
        "layout": LAYOUT,
        "modules": {
            "edge": module_arcade(),
            "bottomEdge": module_chain(),
            "corner": module_rosette(),
            "cornerFill": module_fleur(),
            "terminal": module_terminal(),
        },
    }


# --------------------------------------------------------------------------- #
# Exports
# --------------------------------------------------------------------------- #
def svg_ops(ops, inks):
    out = []
    for o in ops:
        col = inks[o["ink"]]
        if "stroke" in o:
            out.append(f'<path d="{o["d"]}" fill="none" stroke="{col}" stroke-width="{o["stroke"]}"/>')
        else:
            out.append(f'<path d="{o["d"]}" fill="{col}"/>')
    return "\n    ".join(out)


def svg_strip(mod, inks, count=6, scale=2.0):
    P = mod["period"]
    w = P * count
    body = [f'<rect width="{w}" height="100" fill="{inks["ground"]}"/>']
    for pass_ in ("base", "over"):
        for i in range(-1, count + 2):
            body.append(f'<g transform="translate({(i + mod["phase"]) * P:.2f} 0)">\n    {svg_ops(mod[pass_], inks)}\n  </g>')
    return (f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {w:.2f} 100" '
            f'width="{w * scale:.0f}" height="{100 * scale:.0f}">\n'
            f'  <defs><clipPath id="c"><rect width="{w:.2f}" height="100"/></clipPath></defs>\n'
            f'  <g clip-path="url(#c)">\n  ' + "\n  ".join(body) + "\n  </g>\n</svg>\n")


def svg_box(mod, inks, scale=3.0):
    s = mod.get("size", 100)
    return (f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {s} {s}" width="{s * scale:.0f}" height="{s * scale:.0f}">\n'
            f'  <rect width="{s}" height="{s}" fill="{inks["ground"]}"/>\n  {svg_ops(mod["ops"], inks)}\n</svg>\n')


def constant(name):
    """barLight -> BAR_LIGHT (constantes d'énumération du projet)."""
    out = ""
    for c in name:
        out += ("_" + c) if c.isupper() else c.upper()
    return out


def kt_string(s):
    return '"' + s.replace("\\", "\\\\").replace('"', '\\"').replace("$", "\\$") + '"'


def kt_ops(ops, indent):
    lines = []
    for o in ops:
        st = f", stroke = {o['stroke']}f" if "stroke" in o else ""
        lines.append(f'{indent}Op(FrameInk.{constant(o["ink"])}, {kt_string(o["d"])}{st}),')
    return "\n".join(lines)


def kt_color(hexstr):
    return "Color(0xFF" + hexstr.lstrip("#").upper() + ")"


def kt_edge(mod):
    return f"""EdgeModule(
        period = {mod["period"]}f,
        phase = {mod["phase"]}f,
        base = listOf(
{kt_ops(mod["base"], "            ")}
        ),
        over = listOf(
{kt_ops(mod["over"], "            ")}
        ),
    )"""


def kt_box(mod):
    return f"""BoxModule(
        listOf(
{kt_ops(mod["ops"], "            ")}
        ),
    )"""


def kotlin(spec):
    m = spec["modules"]
    L = spec["layout"]
    inks = "\n".join(f"        FrameInk.{constant(k)} to {kt_color(v)}," for k, v in spec["inks"].items())
    palettes = "\n".join(
        f"        LiturgicalColor.{name} to mapOf("
        + ", ".join(f"FrameInk.{constant(k)} to {kt_color(v)}" for k, v in over.items()) + "),"
        for name, over in spec["palettes"].items())
    dimmed = ", ".join(f"FrameInk.{constant(k)}" for k in spec["dimmedInks"])
    return f"""// Généré par tools/cadres/generate.py : ne pas modifier à la main.
// Source : {spec["source"]}.
package app.paroissien.core.designsystem.frame

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.paroissien.core.liturgy.LiturgicalColor

/** Cadre « Pour la confession » : arcature entrelacée, chaîne d'anneaux au pied, rosaces aux coins. */
val ConfessionFrame = FrameSpec(
    edge = {kt_edge(m["edge"])},
    bottomEdge = {kt_edge(m["bottomEdge"])},
    corner = {kt_box(m["corner"])},
    cornerFill = {kt_box(m["cornerFill"])},
    terminal = {kt_box(m["terminal"])},
    margin = {L["margin"]}.dp,
    top = {L["top"]}.dp,
    side = {L["side"]}.dp,
    bottom = {L["bottom"]}.dp,
    bar = {L["bar"]}f,
    barOutline = {L["barOutline"]}f,
    barLight = {L["barLight"]}f,
    barOverhang = {L["barOverhang"]}f,
    textPadding = {L["textPadding"]}.dp,
)

/** Encres du cadre : couleurs du livre, remplacements par couleur liturgique, encres atténuées en thème sombre. */
val ConfessionStyle = FrameStyle(
    base = mapOf(
{inks}
    ),
    palettes = mapOf(
{palettes}
    ),
    dimmed = setOf({dimmed}),
)
"""


def preview_html(spec):
    """Aperçu autonome, à ouvrir dans un navigateur : gabarit + moteur JS + spécification en ligne."""
    tpl = open(os.path.join(HERE, "apercu", "template.html"), encoding="utf-8").read()
    js = open(os.path.join(HERE, "apercu", "frame.js"), encoding="utf-8").read()
    data = json.dumps(spec, ensure_ascii=False, separators=(",", ":"))
    page = tpl.replace("/*@SPEC@*/null", data).replace("/*@FRAMEJS@*/", js)
    head, body = page.split("<main", 1)
    return ('<!doctype html>\n<html lang="fr">\n<head>\n<meta charset="utf-8">\n'
            '<meta name="viewport" content="width=device-width, initial-scale=1">\n'
            f"<!-- Généré par tools/cadres/generate.py : ne pas modifier à la main. -->\n{head}</head>\n"
            f"<body>\n<main{body}</body>\n</html>\n")


def outputs():
    spec = frame_spec()
    inks = spec["inks"]
    m = spec["modules"]
    svg = os.path.join(HERE, "svg")
    return {
        KOTLIN_OUT: kotlin(spec),
        os.path.join(svg, "frise_arcature.svg"): svg_strip(m["edge"], inks),
        os.path.join(svg, "frise_chaine_pied.svg"): svg_strip(m["bottomEdge"], inks),
        os.path.join(svg, "coin_rosace.svg"): svg_box(m["corner"], inks),
        os.path.join(svg, "coin_fleur.svg"): svg_box(m["cornerFill"], inks),
        os.path.join(HERE, "apercu.html"): preview_html(spec),
    }


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
