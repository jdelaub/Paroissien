"""
Primitives communes aux cadres : chemins, formes, opérations de dessin et constructeurs de modules.
Chaque cadre (tools/cadres/cadres/<nom>.py) les importe et décrit sa spécification avec `cadre(...)`.

Repères
  - Frise : u le long de la bande, v en profondeur, de 0 (bord extérieur) à 100 (côté texte).
    100 unités = épaisseur de la bande, quelle que soit sa taille à l'écran.
  - Coin : boîte w × h (100 × 100 par défaut) dessinée pour le coin haut gauche : x de 0 (bord
    extérieur gauche) à w (côté texte), y de 0 (bord extérieur haut) à h (côté texte). Les trois autres
    coins sont des miroirs.
  - Pièce centrale : u de -largeur/2 à +largeur/2 autour du milieu de la bande, v comme la frise.
  - Ornement extérieur de coin : unités u (100 = épaisseur de la bande la plus fine), origine au coin
    extérieur haut gauche du cadre, vers l'extérieur en x et y négatifs.
  - Ornement extérieur du milieu : origine au milieu du bord extérieur haut, l'extérieur vers -y.
  - Terminaison : pointe vers +x depuis le bout de la baguette.
"""
import math

# Épaisseurs usuelles (unités de module)
STRAND = 9.0      # largeur d'un ruban d'entrelacs
OUTLINE = 2.2     # contour de chaque côté d'un ruban
LEAF_OUT = 1.6    # contour des feuilles et fleurons

LITURGICAL = ["GREEN", "VIOLET", "RED", "ROSE", "WHITE", "BLACK", "GOLD"]

# Recopiés de PaperColors.kt et de FrameStyle.ornamentDim, pour l'aperçu web seulement.
PAPER_THEMES = {
    "CLAIR":  {"background": "#FBF8F1", "ink": "#1F1B16", "rubric": "#A31F1A", "dark": False, "ornamentDim": 0.0},
    "SEPIA":  {"background": "#F2E8D3", "ink": "#2E2418", "rubric": "#962216", "dark": False, "ornamentDim": 0.0},
    "SOMBRE": {"background": "#17140F", "ink": "#E6DED0", "rubric": "#E88A78", "dark": True, "ornamentDim": 0.28},
    "NUIT":   {"background": "#000000", "ink": "#9A7F5C", "rubric": "#C2604F", "dark": True, "ornamentDim": 0.6},
}


# --------------------------------------------------------------------------- #
# Chemins
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
    """Point local -> échelle, rotation (degrés), puis translation."""
    sy = sx if sy is None else sy
    ca, sa = math.cos(math.radians(rot)), math.sin(math.radians(rot))
    def f(p):
        x, y = p[0] * sx, p[1] * sy
        return (tx + x * ca - y * sa, ty + x * sa + y * ca)
    return f


def mirror_x(p):
    return (-p[0], p[1])


def mirror_y(p):
    return (p[0], -p[1])


# --------------------------------------------------------------------------- #
# Formes
# --------------------------------------------------------------------------- #
def arc(cx, cy, r, a0, a1, start=True, path=None, ry=None):
    """Arc de cercle ou d'ellipse (degrés, repère écran y vers le bas) en cubiques <= 90°."""
    path = path or Path()
    ry = r if ry is None else ry
    n = max(1, int(math.ceil(abs(a1 - a0) / 90.0 - 1e-9)))
    step = (a1 - a0) / n
    pt = lambda a: (cx + r * math.cos(math.radians(a)), cy + ry * math.sin(math.radians(a)))
    if start:
        path.M(pt(a0))
    for i in range(n):
        b0 = a0 + i * step
        b1 = b0 + step
        k = 4.0 / 3.0 * math.tan(math.radians(b1 - b0) / 4.0)
        p0, p3 = pt(b0), pt(b1)
        t0 = (-math.sin(math.radians(b0)) * r, math.cos(math.radians(b0)) * ry)
        t1 = (-math.sin(math.radians(b1)) * r, math.cos(math.radians(b1)) * ry)
        p1 = (p0[0] + k * t0[0], p0[1] + k * t0[1])
        p2 = (p3[0] - k * t1[0], p3[1] - k * t1[1])
        path.C(p1, p2, p3)
    return path


def circle(cx, cy, r):
    return arc(cx, cy, r, 0, 360).Z()


def ellipse(cx, cy, rx, ry):
    return arc(cx, cy, rx, 0, 360, ry=ry).Z()


def rect(x, y, w, h):
    return Path().M((x, y)).L((x + w, y)).L((x + w, y + h)).L((x, y + h)).Z()


def polygon(points, closed=True):
    p = Path().M(points[0])
    for q in points[1:]:
        p.L(q)
    return p.Z() if closed else p


def diamond(cx, cy, w, h=None):
    h = w if h is None else h
    return polygon([(cx, cy - h / 2), (cx + w / 2, cy), (cx, cy + h / 2), (cx - w / 2, cy)])


def smooth(points, closed=False, tension=1.0):
    """Courbe lisse (Catmull-Rom convertie en cubiques) passant par tous les points."""
    pts = list(points)
    n = len(pts)
    p = Path().M(pts[0])
    if n < 3:
        for q in pts[1:]:
            p.L(q)
        return p.Z() if closed else p
    get = (lambda i: pts[i % n]) if closed else (lambda i: pts[max(0, min(n - 1, i))])
    last = n if closed else n - 1
    k = tension / 6.0
    for i in range(last):
        p0, p1, p2, p3 = get(i - 1), get(i), get(i + 1), get(i + 2)
        c1 = (p1[0] + (p2[0] - p0[0]) * k, p1[1] + (p2[1] - p0[1]) * k)
        c2 = (p2[0] - (p3[0] - p1[0]) * k, p2[1] - (p3[1] - p1[1]) * k)
        p.C(c1, c2, p2)
    return p.Z() if closed else p


def spiral(cx, cy, r0, r1, turns, a0=0.0, steps_per_turn=12):
    """Volute : spirale d'Archimède de r0 à r1, en `turns` tours (sens horaire à l'écran), départ à a0°."""
    n = max(2, int(turns * steps_per_turn))
    pts = []
    for i in range(n + 1):
        t = i / n
        a = math.radians(a0 + 360 * turns * t)
        r = r0 + (r1 - r0) * t
        pts.append((cx + r * math.cos(a), cy + r * math.sin(a)))
    return smooth(pts)


def teardrop(L, W):
    """Goutte / pétale pointant vers +x, base en (0,0)."""
    return (Path().M((0, 0))
            .C((0.22 * L, -0.62 * W), (0.72 * L, -0.52 * W), (L, 0))
            .C((0.72 * L, 0.52 * W), (0.22 * L, 0.62 * W), (0, 0)).Z())


def leaf(L, W, bend=0.0):
    """Feuille lancéolée pointant vers +x, base en (0,0) ; `bend` courbe la nervure (fraction de L)."""
    b = bend * L
    return (Path().M((0, 0))
            .C((0.3 * L, -0.55 * W + b), (0.75 * L, -0.45 * W + b), (L, b))
            .C((0.75 * L, 0.45 * W + b), (0.3 * L, 0.55 * W + b), (0, 0)).Z())


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


def quatrefoil(cx, cy, r):
    """Quadrilobe : quatre cercles autour du centre, rayon d'ensemble r."""
    p = Path()
    for k in range(4):
        a = math.radians(90 * k)
        p.extend(circle(cx + 0.45 * r * math.cos(a), cy + 0.45 * r * math.sin(a), 0.55 * r))
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
# Opérations de dessin : une encre nommée, un chemin, un trait ou un remplissage
# --------------------------------------------------------------------------- #
def fill(ink, path):
    return {"ink": ink, "d": path.normalized().d()}


def stroke(ink, path, width, round=False):
    op = {"ink": ink, "d": path.d(), "stroke": round_(width)}
    if round:
        op["round"] = True
    return op


def round_(x):
    return round(x, 2)


def outlined(ink, path, outline=LEAF_OUT, outline_ink="outline"):
    """Forme remplie avec contour : le trait large dessous, le remplissage dessus
    → seul le contour extérieur de l'union reste visible."""
    return [stroke(outline_ink, path, 2 * outline), fill(ink, path)]


def strand(path, width=STRAND, ink="strand", outline=OUTLINE, outline_ink="outline"):
    """Ruban d'entrelacs : un trait de contour large, puis le ruban plus étroit."""
    return [stroke(outline_ink, path, width + 2 * outline), stroke(ink, path, width)]


# --------------------------------------------------------------------------- #
# Modules et cadre
# --------------------------------------------------------------------------- #
def frise(period, base, over=None, phase=0.0, outset=0.0, inset=0.0):
    """Élément répété le long d'une bande. `outset` / `inset` : débord permis au-delà du bord
    extérieur / intérieur, en unités de profondeur (la découpe s'élargit d'autant)."""
    return {"period": period, "phase": phase, "base": base, "over": over or [], "outset": outset, "inset": inset}


def boite(ops, w=100.0, h=100.0, outset=0.0):
    """Motif dans une boîte w × h (coin), ou ornement en unités (terminaison, ornement extérieur)."""
    return {"ops": ops, "w": w, "h": h, "outset": outset}


def piece(width, ops, outset=0.0):
    """Pièce centrale d'une bande (médaillon, losange), large de `width` unités de profondeur."""
    return {"width": width, "ops": ops, "outset": outset}


def filet(ink, at, width, over=True):
    """Filet tout autour du cadre, à la profondeur `at` (0 = bord extérieur, 100 = bord intérieur)."""
    return {"ink": ink, "at": at, "width": width, "over": over}


def cadre(name, title, source, inks, layout, top, side=None, bottom=None, palettes=None, dimmed=None, ground="ground",
          centers=None, corners=None, rules=None, bars=None, outer=None):
    """Spécification complète d'un cadre, avec ses valeurs par défaut explicites (le Kotlin et le JS n'en ont pas)."""
    centers = centers or {}
    corners = corners or {}
    outer = outer or {}
    for k in ("margin", "top", "side", "bottom", "textPadding"):
        assert k in layout, f"layout.{k} manquant"
    names = set(inks) | {"paper"}

    def check_ops(ops, where):
        for o in ops:
            assert o["ink"] in names, f"{name} : encre inconnue « {o['ink']} » dans {where}"

    spec = {
        "name": name,
        "title": title,
        "source": source,
        "inks": inks,
        "palettes": palettes or {},
        "dimmedInks": dimmed if dimmed is not None else [k for k in inks if k != "outline"],
        "paperThemes": PAPER_THEMES,
        "layout": layout,
        "ground": ground,
        "bands": {"top": top, "side": side or top, "bottom": bottom or top},
        "centers": {"top": centers.get("top"), "side": centers.get("side"), "bottom": centers.get("bottom")},
        "corners": {"top": corners.get("top"), "bottom": corners.get("bottom") or corners.get("top"), "fill": corners.get("fill")},
        "rules": rules or [],
        "bars": bars,
        "outer": {
            "corner": outer.get("corner"),
            "cornerBottom": outer.get("cornerBottom") or outer.get("corner"),
            "mid": outer.get("mid"),
            "midBottom": outer.get("midBottom") or outer.get("mid"),
            "midSide": outer.get("midSide") or outer.get("mid"),
        },
    }
    for k, b in spec["bands"].items():
        check_ops(b["base"] + b["over"], f"bande {k}")
    for k, c in list(spec["centers"].items()) + list(spec["corners"].items()) + list(spec["outer"].items()):
        if c:
            check_ops(c["ops"], k)
    if bars and bars.get("terminal"):
        check_ops(bars["terminal"]["ops"], "terminaison")
    for r in spec["rules"]:
        assert r["ink"] in names, f"{name} : encre inconnue « {r['ink']} » dans un filet"
    for pal in spec["palettes"].values():
        for k in pal:
            assert k in inks, f"{name} : palette sur une encre inconnue « {k} »"
    if ground:
        assert ground in names
    return spec
