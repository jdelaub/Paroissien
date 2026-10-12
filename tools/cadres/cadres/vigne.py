"""
Cadre « Vigne aux oiseaux », d'après un paroissien chromolithographié de la fin du XIXᵉ siècle (cadre sans texte) :
bordure gothique à baguette sur papier nu. Une baguette fine (filet vert rehaussé de blanc, doublé d'une bande d'or
où alternent panneaux roses et verts, avec des pattes d'or festonnées) court sur les côtés et au pied ; le haut est
ouvert. Au-dehors, une vigne : vrilles brunes, feuilles d'olive à cinq lobes, baies rouges, quelques feuilles
vertes. En haut, la baguette se fourche en volutes d'où part la vigne, et deux oiseaux se font face au milieu ; au
pied, un oiseau bleu à ventre orangé lève la tête vers la baguette ; aux coins du pied, une patte d'or porte un
rinceau rose en huit, deux fleurettes bleues et une acanthe rose ; au milieu des côtés, la baguette fait un nœud.

Unités de dessin : 1/100 de la bande latérale (0,22 dp), pour toutes les bandes ; chaque module est ensuite mis à
l'échelle de sa bande (k = S / épaisseur), pour que feuilles, vrilles et baguette gardent la même taille à l'écran.
"""
import math

from commun import Path, affine, arc, boite, cadre, circle, frise, leaf, piece, polygon, rect, smooth

ORDRE = 901

# Épaisseurs des bandes (dp) et passage des unités de dessin aux unités de bande
S, T, B = 22.0, 30.0, 34.0
KT, KB = S / T, S / B
HT, HB = 100 / KT, 100 / KB          # profondeur des bandes haute et du pied, en unités de dessin

INKS = {
    "gold": "#B99E62",       # or de la baguette et des pattes (remonté : il sort terne en photo)
    "green": "#7D8B5E",      # filet vert de la baguette
    "greenDark": "#55664A",  # panneaux verts, contour des feuilles vertes
    "pink": "#D88870",       # panneaux et rinceaux roses
    "pinkLight": "#F1C6B2",  # rehaut des rinceaux
    "leaf": "#9A8C5C",       # feuilles de vigne, olive doré
    "leafDark": "#625840",   # contour des feuilles
    "stem": "#7A7160",       # vrilles, brun gris
    "berry": "#C75A3B",      # baies
    "leafGreen": "#7F9467",  # petites feuilles vertes
    "blue": "#47648F",       # oiseaux et fleurettes
    "blueDark": "#2C3A55",   # queues, ailes sombres
    "red": "#D0704B",        # poitrail orangé
    "white": "#F6F1E4",      # rehauts
    "outline": "#463C2E",    # contour brun foncé
}

# Couleurs liturgiques : la baguette (filet et panneaux) prend la couleur du temps ; vigne, or et oiseaux restent.
PALETTES = {
    "GREEN":  {"pink": "#9DB97F", "pinkLight": "#DDE9CB", "green": "#4E7A43"},
    "VIOLET": {"pink": "#A58BC4", "pinkLight": "#DDD0EA", "green": "#5F4685", "greenDark": "#45305F"},
    "RED":    {"pink": "#C8483A", "pinkLight": "#F0B7A6", "green": "#8E2D29", "greenDark": "#6A2220"},
    "ROSE":   {"pink": "#E29DB4", "pinkLight": "#F6D9E2", "green": "#B0607C", "greenDark": "#874660"},
    "WHITE":  {"pink": "#E9DDB8", "pinkLight": "#FBF6E8", "green": "#C2A766", "greenDark": "#9C8550"},
    "BLACK":  {"pink": "#8F8981", "pinkLight": "#C9C4BC", "green": "#4A4744", "berry": "#6E6862"},
}

# Baguette : distances au bord intérieur (côté texte), en unités de dessin
BAR_IN, BAR_MID, BAR_OUT = 3.0, 13.0, 24.0   # filet vert de 3 à 13, bande d'or de 13 à 24
BAR_LINE = 0.9                                # contour de la baguette

STEM_W = 2.6      # tige principale
TWIG_W = 1.9      # rameaux et vrilles
LEAF_O = 1.0      # contour des feuilles


# --------------------------------------------------------------------------- #
# Dessin en unités de dessin, émis à l'échelle de la bande
# --------------------------------------------------------------------------- #
class Dessin:
    """Tracés en unités de dessin ; `ops(k)` les émet mis à l'échelle k, épaisseurs de trait comprises."""

    def __init__(self):
        self.items = []

    def fill(self, ink, path):
        self.items.append((ink, path, None, False))

    def stroke(self, ink, path, w, round=True):
        self.items.append((ink, path, w, round))

    def outlined(self, ink, path, o=LEAF_O, oink="outline"):
        self.stroke(oink, path, 2 * o)
        self.fill(ink, path)

    def extend(self, other):
        self.items += other.items
        return self

    def ops(self, k=1.0):
        out = []
        f = affine(sx=k)
        for ink, p, w, r in self.items:
            q = p.transformed(f)
            if w is None:
                out.append({"ink": ink, "d": q.normalized().d()})
            else:
                op = {"ink": ink, "d": q.d(), "stroke": round(w * k, 2)}
                if r:
                    op["round"] = True
                out.append(op)
        return out


# --------------------------------------------------------------------------- #
# Tiges : Catmull-Rom échantillonnable, périodique pour les frises
# --------------------------------------------------------------------------- #
def _bez(s, t):
    (x0, y0), (x1, y1), (x2, y2), (x3, y3) = s
    m = 1 - t
    p = (m ** 3 * x0 + 3 * m * m * t * x1 + 3 * m * t * t * x2 + t ** 3 * x3,
         m ** 3 * y0 + 3 * m * m * t * y1 + 3 * m * t * t * y2 + t ** 3 * y3)
    d = (3 * m * m * (x1 - x0) + 6 * m * t * (x2 - x1) + 3 * t * t * (x3 - x2),
         3 * m * m * (y1 - y0) + 6 * m * t * (y2 - y1) + 3 * t * t * (y3 - y2))
    return p, math.degrees(math.atan2(d[1], d[0]))


class Tige:
    """Courbe lisse par des points. Avec `period`, les points couvrent une période et la courbe se prolonge d'un
    segment de chaque côté : deux instances voisines se recouvrent exactement, sans couture."""

    def __init__(self, pts, period=None):
        pts = list(pts)
        if period:
            q = [(x - period, y) for x, y in pts[-2:]] + pts + [(x + period, y) for x, y in pts[:3]]
        else:
            q = [pts[0]] + pts + [pts[-1]]
        self.segs = []
        for i in range(1, len(q) - 2):
            p0, p1, p2, p3 = q[i - 1], q[i], q[i + 1], q[i + 2]
            c1 = (p1[0] + (p2[0] - p0[0]) / 6, p1[1] + (p2[1] - p0[1]) / 6)
            c2 = (p2[0] - (p3[0] - p1[0]) / 6, p2[1] - (p3[1] - p1[1]) / 6)
            self.segs.append((p1, c1, c2, p2))
        self.samples = [_bez(s, j / 24) for s in self.segs for j in range(25)]

    def path(self):
        p = Path().M(self.segs[0][0])
        for s in self.segs:
            p.C(s[1], s[2], s[3])
        return p

    def at(self, u):
        """Point de la tige d'abscisse la plus proche de u, et angle de la tangente."""
        return min(self.samples, key=lambda s: abs(s[0][0] - u))


def pousse(x, y, ang, L, bend=0.0, n=4):
    """Rameau partant de (x, y) dans la direction ang (degrés), long de L, qui tourne de `bend` degrés."""
    pts = [(x, y)]
    for i in range(n):
        a = math.radians(ang + bend * (i + 0.5) / n)
        x += L / n * math.cos(a)
        y += L / n * math.sin(a)
        pts.append((x, y))
    return pts, ang + bend


def vrille(x, y, ang, r, turns=1.25, sense=1):
    """Vrille en spirale qui part de (x, y) dans la direction ang ; sense = 1 : s'enroule dans le sens horaire."""
    a = math.radians(ang)
    cx, cy = x - sense * r * math.sin(a), y + sense * r * math.cos(a)
    phi0 = ang - 90 * sense
    pts = []
    n = int(10 * turns) + 2
    for i in range(n + 1):
        t = i / n
        ph = math.radians(phi0 + sense * 360 * turns * t)
        rr = r * (1 - 0.72 * t)
        pts.append((cx + rr * math.cos(ph), cy + rr * math.sin(ph)))
    return smooth(pts)


def lobe(L, W):
    """Lobe oblong à bout arrondi, pointant vers +x, base en (0, 0)."""
    return (Path().M((0, 0))
            .C((0.12 * L, -0.5 * W), (0.5 * L, -0.64 * W), (0.8 * L, -0.46 * W))
            .C((1.0 * L, -0.32 * W), (1.03 * L, 0.18 * W), (0.86 * L, 0.38 * W))
            .C((0.55 * L, 0.62 * W), (0.12 * L, 0.5 * W), (0, 0)).Z())


def feuille_vigne(L):
    """Feuille de vigne stylisée à cinq lobes digités, pointant vers +x, base en (0, 0)."""
    p = Path()
    for ang, kl, kw in ((0, 1.0, 0.29), (-38, 0.86, 0.28), (38, 0.86, 0.28), (-80, 0.58, 0.25), (80, 0.58, 0.25)):
        p.extend(lobe(L * kl, L * kw).transformed(affine(0.08 * L, 0, rot=ang)))
    return p


class Vigne(Dessin):
    """Vigne : on pose d'abord les tiges, puis les feuilles, puis les baies, chacune dans sa couche."""

    def __init__(self):
        super().__init__()
        self.stems, self.leaves, self.greens, self.berries = Dessin(), Dessin(), Dessin(), Dessin()

    def tige(self, path, w=TWIG_W):
        self.stems.stroke("stem", path, w)

    def feuille(self, x, y, ang, L):
        self.leaves.outlined("leaf", feuille_vigne(L).transformed(affine(x, y, rot=ang)), oink="leafDark")

    def verte(self, x, y, ang, L=15):
        self.greens.outlined("leafGreen", leaf(L, 0.55 * L, bend=0.06).transformed(affine(x, y, rot=ang)),
                             o=0.9, oink="greenDark")

    def baie(self, x, y, r=4.4):
        self.berries.outlined("berry", circle(x, y, r), o=0.8)

    def rameau(self, x, y, ang, L, bend=0.0, fin="feuille", size=28, twist=0.0, mid=()):
        """Rameau terminé par une feuille, une feuille verte, une vrille (size = rayon, signe = sens) ou une
        grappe de baies (size = rayon) ; `mid` : feuilles posées le long du rameau, (point 1 à 3, angle, taille)."""
        pts, a_end = pousse(x, y, ang, L, bend)
        ex, ey = pts[-1]
        self.tige(smooth(pts))
        if fin == "feuille":
            self.feuille(ex, ey, a_end + twist, size)
        elif fin == "verte":
            self.verte(ex, ey, a_end + twist, size)
        elif fin == "vrille":
            self.tige(vrille(ex, ey, a_end, abs(size), sense=1 if size > 0 else -1), TWIG_W * 0.85)
        elif fin == "baies":
            a = math.radians(a_end)
            r = size
            self.baie(ex + 0.9 * r * math.cos(a), ey + 0.9 * r * math.sin(a), r)
            for da in (-70, 70):
                b = math.radians(a_end + da)
                self.tige(Path().M((ex, ey)).L((ex + 2.2 * r * math.cos(b), ey + 2.2 * r * math.sin(b))), TWIG_W * 0.8)
                self.baie(ex + 2.6 * r * math.cos(b), ey + 2.6 * r * math.sin(b), r * 0.9)
        for i, a, s in mid:
            if s > 0:
                self.feuille(pts[i][0], pts[i][1], a, s)
            else:
                self.verte(pts[i][0], pts[i][1], a, -s)
        return pts, a_end

    def garnir(self, tige, table):
        """Pose les rameaux d'une table (u, angle, longueur, courbure, fin, taille, torsion, feuilles du milieu)
        sur la tige."""
        for u, ang, L, bend, fin, size, twist, mid in table:
            (x, y), _ = tige.at(u)
            self.rameau(x, y, ang, L, bend, fin, size, twist, mid)

    def done(self):
        self.items = []
        for d in (self.stems, self.leaves, self.greens, self.berries):
            self.items += d.items
        return self


# --------------------------------------------------------------------------- #
# Baguette (en unités de dessin, bord intérieur à y = depth)
# --------------------------------------------------------------------------- #
def baguette(d, x0, x1, depth, panels=()):
    """Tronçon horizontal de baguette de x0 à x1 ; `panels` : (x0, x1, encre) incrustés dans la bande d'or."""
    yi, ym, yo = depth - BAR_IN, depth - BAR_MID, depth - BAR_OUT
    d.fill("outline", rect(x0, yo - BAR_LINE, x1 - x0, yi - yo + 2 * BAR_LINE))
    d.fill("gold", rect(x0, yo, x1 - x0, ym - yo - BAR_LINE / 2))
    d.fill("green", rect(x0, ym + BAR_LINE / 2, x1 - x0, yi - ym - BAR_LINE / 2))
    d.fill("white", rect(x0, (ym + yi) / 2 - 0.6, x1 - x0, 1.2))
    for a, b, ink in panels:
        r = rect(a, yo + 1.6, b - a, ym - yo - 3.6)
        d.stroke("outline", r, 1.4, round=False)
        d.fill(ink, r)
        if ink == "greenDark":
            # rinceau clair dans le panneau vert
            n = max(1, round((b - a) / 14))
            w = (b - a) / n
            pts = [(a + i * w / 2, (yo + ym) / 2 + (2.0 if i % 2 else -2.0)) for i in range(2 * n + 1)]
            d.stroke("green", smooth(pts), 1.2)
        else:
            d.stroke("pinkLight", Path().M((a + 3, (yo + ym) / 2 - 0.8)).L((b - 3, (yo + ym) / 2 - 0.8)), 1.1)


def festons(tips):
    """Contour fermé passant par des pointes (x, y, creux) : entre une pointe et la suivante, une échancrure ronde
    creusée de `creux` vers l'intérieur (0 : bord droit)."""
    cx = sum(t[0] for t in tips) / len(tips)
    cy = sum(t[1] for t in tips) / len(tips)
    p = Path().M(tips[0][:2])
    for i, (ax, ay, dd) in enumerate(tips):
        bx, by = tips[(i + 1) % len(tips)][:2]
        dx, dy = bx - ax, by - ay
        L = math.hypot(dx, dy) or 1.0
        nx, ny = -dy / L, dx / L
        if (cx - (ax + bx) / 2) * nx + (cy - (ay + by) / 2) * ny < 0:
            nx, ny = -nx, -ny
        k = 1.33 * dd
        p.C((ax + 0.15 * dx + k * nx, ay + 0.15 * dy + k * ny), (ax + 0.85 * dx + k * nx, ay + 0.85 * dy + k * ny), (bx, by))
    return p.Z()


def patte(d, x, depth, reach=16.0, half=16.0):
    """Patte d'or festonnée, collée au bord extérieur de la baguette : trois épines, deux échancrures rondes."""
    yo = depth - BAR_OUT + 1.5
    top = yo - reach
    p = festons([(x - half - 7, yo, 0), (x - half, top + 3, 4.5), (x + 1, top - 3, 4.5), (x + half, top + 3, 0),
                 (x + half + 7, yo, 0)])
    d.outlined("gold", p, o=BAR_LINE)


# --------------------------------------------------------------------------- #
# Frises : une période de vigne par bande. Tables : (u, angle, longueur, courbure, fin, taille, torsion, milieu)
# --------------------------------------------------------------------------- #
P = 240.0     # période de la vigne, en unités de dessin (53 dp), la même pour les trois bandes

# côtés : la vigne au-dehors (y < 74), la baguette côté texte ; période double (106 dp), pour que la vigne ne
# se répète pas trop visiblement le long des grands côtés
P_SIDE = 2 * P
STEM_SIDE = [(0, 50), (55, 26), (125, 58), (190, 30), (250, 54), (320, 24), (390, 58), (440, 26)]
TABLE_SIDE = [
    (20, 66, 10, 12, "feuille", 24, -8, ()),
    (64, -118, 24, -20, "feuille", 44, 4, ()),
    (72, -36, 12, 0, "baies", 4.4, 0, ()),
    (100, -64, 34, 44, "vrille", 8, 0, ((2, -150, 30),)),
    (128, 84, 9, 0, "verte", 16, 0, ()),
    (165, -100, 30, -30, "feuille", 38, 0, ((2, -20, -16),)),
    (198, -58, 18, 20, "feuille", 30, -6, ()),
    (224, 70, 12, 16, "baies", 4.2, 0, ()),
    (262, -128, 40, -30, "vrille", -7, 0, ((1, -60, 28), (3, -170, 34))),
    (300, 64, 10, 10, "feuille", 26, -8, ()),
    (328, -84, 14, 12, "feuille", 44, 0, ()),
    (362, -40, 14, 0, "baies", 4.6, 0, ()),
    (394, 96, 8, 0, "verte", 15, 0, ()),
    (420, -112, 30, -16, "feuille", 36, 0, ()),
    (450, -80, 14, 10, "verte", 16, 0, ()),
]
PADS_SIDE = (125, 390)       # pattes d'or, sous les creux de la tige
PANELS_SIDE = ((14, 106, "pink"), (144, 226, "greenDark"), (254, 372, "pink"), (408, 466, "greenDark"))

# haut : guirlande qui remonte aux jonctions (y = 70) et s'abaisse au milieu de la période, feuilles dressées
STEM_TOP = [(0, 70), (60, 94), (120, 108), (180, 94)]
TABLE_TOP = [
    (46, -96, 38, -10, "feuille", 42, 0, ((2, -32, 30),)),
    (74, -66, 46, 40, "vrille", 9, 0, ((2, -150, 32), (3, -30, -16))),
    (96, 82, 6, 8, "feuille", 24, 0, ()),
    (122, -94, 56, -28, "feuille", 44, 0, ((2, 0, 30),)),
    (146, 104, 8, 0, "verte", 16, 0, ()),
    (168, -58, 30, 18, "feuille", 38, 0, ()),
    (196, -122, 28, -16, "baies", 4.6, 0, ()),
    (200, -100, 18, 8, "feuille", 32, 0, ()),
]

# pied : vigne pendante sous la baguette (y < 126), feuilles tournées vers le bas de la page (y décroissant)
STEM_BOT = [(0, 96), (60, 70), (120, 104), (180, 70)]
TABLE_BOT = [
    (52, -104, 30, -14, "feuille", 42, 0, ()),
    (64, -58, 40, 36, "vrille", 8, 0, ((2, -140, 30),)),
    (92, 72, 8, 0, "verte", 16, 0, ()),
    (124, -90, 44, -30, "feuille", 44, 0, ((2, -18, 30),)),
    (150, 100, 6, 0, "feuille", 22, 0, ()),
    (186, -68, 18, 16, "baies", 4.6, 0, ()),
    (204, -122, 34, -20, "feuille", 38, 0, ()),
]


def vigne(stem, table, period=P):
    tige = Tige(stem, period)
    v = Vigne()
    v.tige(tige.path(), STEM_W)
    v.garnir(tige, table)
    return tige, v.done()


def module_cote():
    tige, v = vigne(STEM_SIDE, TABLE_SIDE, P_SIDE)
    bar = Dessin()
    baguette(bar, -3, P_SIDE + 3, 100, panels=PANELS_SIDE)
    for u in PADS_SIDE:
        # vrille qui sort de la patte d'or et rejoint la tige
        (x, y), _ = tige.at(u)
        v.stems.stroke("stem", smooth([(u + 2, 77), (u, 70), (x, y)]), TWIG_W)
        patte(bar, u, 100, reach=13, half=11)
    return frise(P_SIDE, v.done().ops(), bar.ops(), phase=0.0, outset=38)


def module_haut():
    _, v = vigne(STEM_TOP, TABLE_TOP)
    return frise(P * KT, v.ops(KT), [], phase=0.0, outset=30 * KT)


def module_pied():
    _, v = vigne(STEM_BOT, TABLE_BOT)
    v.stems.stroke("stem", smooth([(120, HB - BAR_OUT + 2), (118, 112), (120, 104)]), TWIG_W)
    bar = Dessin()
    baguette(bar, -3, P + 3, HB, panels=((14, 100, "pink"), (150, 226, "greenDark")))
    patte(bar, 120, HB, reach=13, half=11)
    return frise(P * KB, v.done().ops(KB), bar.ops(KB), phase=0.0, outset=36 * KB)


# --------------------------------------------------------------------------- #
# Oiseaux
# --------------------------------------------------------------------------- #
def oiseau(d, x, y, s=1.0, flip=False, rot=0.0, look=0.0, vflip=False, body="red", wing="blue", head="blue",
           tail="blueDark", breast=None):
    """Oiseau perché, de profil, pattes en (x, y), tourné vers +x (vers -x si flip), haut de page vers -y (vers +y
    si vflip, pour la bande du pied). `rot` penche le corps, `look` tourne la tête autour du cou (degrés)."""
    place = affine(0, 0, rot=rot, sx=s)
    neck = (4.0, -38.0)
    ca, sa = math.cos(math.radians(look)), math.sin(math.radians(look))

    def f(p):
        if flip:
            p = (-p[0], p[1])
        p = place(p)
        if vflip:
            p = (p[0], -p[1])
        return (x + p[0], y + p[1])

    def fh(p):
        dx, dy = p[0] - neck[0], p[1] - neck[1]
        return f((neck[0] + dx * ca - dy * sa, neck[1] + dx * sa + dy * ca))

    tail_p = smooth([(-4, -16), (-14, -8), (-24, 8), (-31, 24), (-25, 23), (-16, 10), (-4, -4)], closed=True)
    body_p = smooth([(13, -34), (15, -24), (11, -13), (3, -6), (-7, -5), (-15, -11), (-17, -22), (-12, -33),
                     (-2, -41), (8, -41)], closed=True)
    breast_p = smooth([(13, -33), (15, -24), (11, -13), (3, -6), (-3, -8), (1, -19), (7, -30)], closed=True)
    wing_p = smooth([(6, -35), (-4, -38), (-14, -30), (-21, -16), (-27, 0), (-16, -6), (-4, -16), (4, -26)],
                    closed=True)
    for leg in ((3, -7, 2, 0), (-3, -7, -5, 0)):
        d.stroke("stem", Path().M((leg[0], leg[1])).L((leg[2], leg[3])).transformed(f), 1.8)
    d.outlined(tail, tail_p.transformed(f), o=1.1)
    d.outlined(body, body_p.transformed(f), o=1.1)
    if breast:
        d.fill(breast, breast_p.transformed(f))
    d.outlined(wing, wing_p.transformed(f), o=1.0)
    d.outlined("outline", polygon([(14.5, -47.8), (23.5, -45.2), (15.2, -42.2)]).transformed(fh), o=0.4)
    d.outlined(head, circle(8, -44, 8.5).transformed(fh), o=1.1)
    d.fill("white", circle(10.2, -45.6, 2.3).transformed(fh))
    d.fill("outline", circle(10.7, -45.6, 1.2).transformed(fh))


# --------------------------------------------------------------------------- #
# Pièces centrales
# --------------------------------------------------------------------------- #
CW_TOP = 290.0


def centre_haut():
    """Deux oiseaux affrontés sur la vigne, une gerbe qui monte entre eux."""
    w = CW_TOP
    y0 = STEM_TOP[0][1]
    tige = Tige([(-w / 2 - 40, y0 + 12), (-w / 2, y0), (-74, 98), (0, 88), (74, 98), (w / 2, y0), (w / 2 + 40, y0 + 12)])
    v = Vigne()
    v.tige(tige.path(), STEM_W)
    gerbe = Tige([(0, 88), (-5, 64), (5, 40), (0, 18)])
    v.tige(gerbe.path(), TWIG_W)
    v.feuille(0, 18, -90, 38)
    for (yy, ang, L, fin, size, mid) in ((72, -150, 12, "feuille", 32, ()), (52, -30, 12, "feuille", 32, ()),
                                         (34, -150, 8, "baies", 4.4, ()), (84, -24, 8, "verte", 16, ())):
        x = min(gerbe.samples, key=lambda s: abs(s[0][1] - yy))[0][0]
        v.rameau(x, yy, ang, L, 0, fin, size, 0, mid)
    for sg in (-1, 1):
        # feuilles qui relient aux frises, et une qui retombe sous chaque oiseau
        x, y = tige.at(sg * 126)[0]
        v.rameau(x, y, -90 - sg * 34, 26, sg * 16, "feuille", 40)
        x, y = tige.at(sg * 34)[0]
        v.rameau(x, y, 90 + sg * 36, 10, 0, "feuille", 24)
        x, y = tige.at(sg * 104)[0]
        v.rameau(x, y, 90 - sg * 30, 8, 0, "verte", 16)
    d = Dessin().extend(v.done())
    for sg in (-1, 1):
        x, y = tige.at(sg * 70)[0]
        oiseau(d, x, y - 1, s=1.55, flip=sg > 0, rot=-sg * 3, wing="blue" if sg < 0 else "blueDark")
    return piece(w * KT, d.ops(KT), outset=26 * KT)


CW_SIDE = 84.0


def centre_cote():
    """Nœud de la baguette : le filet vert fait une boucle sur une patte d'or festonnée, d'où sort la vigne."""
    w = CW_SIDE
    y0 = STEM_SIDE[0][1]
    tige = Tige([(-w / 2 - 40, 24), (-w / 2, y0), (0, 60), (w / 2, y0), (w / 2 + 40, 24)])
    v = Vigne()
    v.tige(tige.path(), STEM_W)
    v.rameau(-6, 58, -96, 20, -8, "feuille", 36)
    v.rameau(10, 58, -58, 20, 30, "vrille", -7)
    v.baie(-24, 66, 4.4)
    v.baie(22, 64, 4.0)
    d = Dessin().extend(v.done())
    baguette(d, -w / 2 - 2, w / 2 + 2, 100)
    patte(d, 0, 100, reach=24, half=21)
    # boucle du nœud : elle passe sur la bande d'or et sous le filet vert
    yc = 100 - (BAR_MID + BAR_OUT) / 2
    ring = circle(0, yc, 16)
    d.stroke("outline", ring, 8.2)
    d.stroke("green", ring, 6.0)
    d.stroke("white", arc(0, yc, 16, 196, 256), 1.3)
    yi, ym = 100 - BAR_IN, 100 - BAR_MID
    d.fill("outline", rect(-w / 2, ym - BAR_LINE / 2, w, yi - ym + BAR_LINE))
    d.fill("green", rect(-w / 2, ym + BAR_LINE / 2, w, yi - ym - BAR_LINE / 2))
    d.fill("white", rect(-w / 2, (ym + yi) / 2 - 0.6, w, 1.2))
    return piece(w, d.ops(), outset=38)


CW_BOT = 250.0


def centre_pied():
    """Oiseau bleu à ventre orangé sous la baguette, la tête levée ; panneau rose au milieu de la baguette."""
    w = CW_BOT
    y0 = STEM_BOT[0][1]
    tige = Tige([(-w / 2 - 40, 70), (-w / 2, y0), (-64, 66), (6, 34), (70, 64), (w / 2, y0), (w / 2 + 40, 70)])
    v = Vigne()
    v.tige(tige.path(), STEM_W)
    # la vrille qui descend de la baguette et porte l'oiseau
    v.tige(smooth([(-36, HB - BAR_OUT + 2), (-44, 116), (-30, 96), (-30, 70), (-12, 44)]), STEM_W)
    for (u, ang, L, bend, fin, size, mid) in ((-86, -84, 28, -10, "feuille", 40, ()),
                                              (-64, -150, 26, 10, "feuille", 36, ()),
                                              (-40, -70, 14, 0, "baies", 4.4, ()),
                                              (56, -100, 32, 20, "feuille", 42, ((2, -170, 28),)),
                                              (100, -60, 24, 20, "vrille", 8, ()),
                                              (30, 80, 10, 0, "verte", 16, ()),
                                              (-94, 70, 10, 0, "verte", 16, ()),
                                              (84, 104, 8, 0, "feuille", 24, ())):
        x, y = tige.at(u)[0]
        v.rameau(x, y, ang, L, bend, fin, size, 0, mid)
    # grappe qui pend plus bas au milieu, comme la chute de feuillage sous l'oiseau
    (x, y), _ = tige.at(42)
    chute = Tige([(x, y), (48, 24), (39, 6), (42, -6)])
    v.tige(chute.path(), TWIG_W)
    v.feuille(42, -6, -86, 34)
    v.rameau(48, 24, -8, 10, 0, "feuille", 28)
    v.rameau(39, 8, -168, 10, 0, "feuille", 26)
    v.rameau(44, 15, -40, 6, 0, "baies", 4.0)
    d = Dessin().extend(v.done())
    x, y = tige.at(4)[0]
    oiseau(d, x, y, s=1.3, rot=18, look=-48, vflip=True, body="blue", wing="blue", tail="blueDark", breast="red")
    baguette(d, -w / 2 - 2, w / 2 + 2, HB, panels=((-w / 2 + 16, w / 2 - 16, "pink"),))
    return piece(w * KB, d.ops(KB), outset=48 * KB)


# --------------------------------------------------------------------------- #
# Coins
# --------------------------------------------------------------------------- #
def tapered(pts, w0, w1):
    """Ruban effilé le long d'une courbe lisse : largeur w0 au départ, w1 à l'arrivée."""
    t = Tige(pts)
    samples = [_bez(s, j / 8) for s in t.segs for j in range(9)]
    n = len(samples)
    left, right = [], []
    for i, ((x, y), a) in enumerate(samples):
        w = (w0 + (w1 - w0) * i / (n - 1)) / 2
        r = math.radians(a + 90)
        left.append((x + w * math.cos(r), y + w * math.sin(r)))
        right.append((x - w * math.cos(r), y - w * math.sin(r)))
    return polygon(left + right[::-1])


def filet_vert(d, pts, w0, w1):
    """Filet vert effilé, avec son contour."""
    d.fill("outline", tapered(pts, w0 + 2 * BAR_LINE, w1 + 2 * BAR_LINE))
    d.fill("green", tapered(pts, w0, w1))


LINE_W = BAR_MID - BAR_IN - BAR_LINE      # largeur du filet vert
XG = 100 - BAR_OUT                        # bord extérieur de la baguette des côtés
XV = 100 - (BAR_IN + BAR_MID) / 2         # milieu du filet vert


def coin_haut():
    """Haut des côtés : la baguette se fourche ; le bras intérieur devient la vigne du haut, l'extérieur s'enroule
    au-dehors ; l'or remplit la fourche, festonné, avec une longue épine. Boîte 100 × HT."""
    h = HT
    y0 = STEM_TOP[0][1]
    d = Dessin()
    v = Vigne()
    # la vigne du côté monte et s'échappe par l'angle
    side = Tige([(STEM_SIDE[0][1], h + 20), (STEM_SIDE[0][1], h), (42, 104), (26, 66), (8, 34), (-10, 12)])
    v.tige(side.path(), STEM_W)
    v.tige(vrille(-10, 12, -130, 8), TWIG_W)
    for (yy, ang, L, bend, fin, size, mid) in ((114, 170, 14, 10, "feuille", 36, ()),
                                              (80, -150, 18, -10, "feuille", 42, ()),
                                              (60, -30, 12, 0, "baies", 4.4, ()),
                                              (34, -170, 16, 10, "feuille", 34, ()),
                                              (22, -70, 18, -10, "feuille", 36, ()),
                                              (96, 10, 10, 0, "verte", 16, ())):
        x = min(side.samples, key=lambda s: abs(s[0][1] - yy))[0][0]
        v.rameau(x, yy, ang, L, bend, fin, size, 0, mid)
    d.extend(v.done())
    # or de la fourche : longue épine au-dehors, festons au sommet
    gold = festons([(XG, h + 2, 0), (XG, 100, 0), (40, 22, 5), (62, 40, 4), (80, 32, 4), (98, 44, 0),
                    (101, y0 - 6, 0), (101, h + 2, 0)])
    d.outlined("gold", gold, o=BAR_LINE)
    # filet vert du côté, du bas de la boîte à la fourche (l'or de la fourche couvre la baguette)
    d.fill("outline", rect(100 - BAR_MID - BAR_LINE / 2, 100, LINE_W + 2 * BAR_LINE, h - 98))
    d.fill("green", rect(100 - BAR_MID + BAR_LINE / 2, 100, LINE_W, h - 98))
    d.fill("white", rect(XV - 0.6, 104, 1.2, h - 102))
    # bras extérieur, qui s'enroule au-dehors, et bras intérieur, qui se courbe vers le milieu et s'effile jusqu'à
    # la tige du haut ; les deux contours d'abord, pour qu'aucun ne raye l'autre à la fourche
    arms = [([(XV, 103), (XV, 96), (XV - 1, 82), (XV - 8, 66), (XV - 20, 56), (XV - 32, 54)], 3.6),
            ([(XV, 103), (XV, 96), (XV, 84), (XV + 2, y0 + 7), (97, y0 + 1), (101, y0), (104, y0)], STEM_W)]
    curl = vrille(XV - 32, 54, 180, 6)
    for pts, w1 in arms:
        d.fill("outline", tapered(pts, LINE_W + 2 * BAR_LINE, w1 + 2 * BAR_LINE))
    d.stroke("outline", curl, 3.6 + 2 * BAR_LINE)
    for pts, w1 in arms:
        d.fill("green", tapered(pts, LINE_W, w1))
    d.stroke("green", curl, 3.6)
    return boite(d.ops(), 100, h, outset=44)


def acanthe(L, W):
    """Feuille d'acanthe : lame pointant vers +x, base en (0, 0), trois lobes festonnés du côté -y, pointe
    recourbée."""
    return festons([(0, -0.45 * W, 0), (0.3 * L, -0.85 * W, 0.16 * W), (0.58 * L, -0.8 * W, 0.16 * W),
                    (0.84 * L, -0.62 * W, 0.12 * W), (1.0 * L, -0.12 * W, 0), (0.82 * L, 0.28 * W, 0),
                    (0.45 * L, 0.5 * W, 0), (0, 0.45 * W, 0)])


def fleurette(d, x, y, r):
    """Fleurette bleue en croix, rehaussée de blanc."""
    p = Path()
    for k in range(4):
        p.extend(lobe(r, 0.7 * r).transformed(affine(x, y, rot=45 + 90 * k)))
    d.outlined("blue", p, o=0.9, oink="blueDark")
    for k in range(4):
        a = math.radians(45 + 90 * k)
        d.stroke("white", Path().M((x + 0.3 * r * math.cos(a), y + 0.3 * r * math.sin(a)))
                 .L((x + 0.7 * r * math.cos(a), y + 0.7 * r * math.sin(a))), 1.1)


def coin_pied():
    """Coin du pied : la baguette du côté descend au-delà du pied et finit dans une patte d'or festonnée portant
    un rinceau rose en huit et deux fleurettes bleues. Boîte 100 × HB, dessinée en haut à gauche (miroir vertical) ;
    on la décrit dans le repère de la page, Y vers le bas depuis le bord intérieur de la bande du pied."""
    h = HB

    def pg(x, Y, *rest):
        return (x, h - Y) + rest

    d = Dessin()
    v = Vigne()
    # vignes du côté et du pied : elles passent sous la patte et ressortent dans l'angle
    yb = h - STEM_BOT[0][1]
    side = Tige([pg(STEM_SIDE[0][1], -20), pg(STEM_SIDE[0][1], 0), pg(40, 26), pg(26, 70), pg(16, 112), pg(2, 146),
                 pg(-14, 162)])
    bot = Tige([pg(120, yb), pg(100, yb), pg(76, 120), pg(52, 142), pg(26, 150)])
    v.tige(side.path(), STEM_W)
    v.tige(bot.path(), STEM_W)
    v.tige(vrille(*pg(-14, 162), 140, 7), TWIG_W)
    for (x, Y, ang, L, bend, fin, size) in ((30, 52, 196, 14, -10, "feuille", 38), (20, 96, 168, 16, 10, "feuille", 42),
                                           (8, 130, 120, 12, -10, "feuille", 36), (40, 146, 90, 12, 10, "feuille", 36),
                                           (38, 20, 220, 10, 0, "baies", 4.4), (66, 130, 70, 8, 0, "verte", 16)):
        X, Yy = pg(x, Y)
        v.rameau(X, Yy, -ang, L, -bend, fin, size)   # angles donnés dans le repère de la page
    d.extend(v.done())
    # baguette du pied, venant de la droite
    yo, ym, yi = BAR_IN, BAR_MID, BAR_OUT
    d.fill("outline", polygon([pg(80, yo - BAR_LINE), pg(102, yo - BAR_LINE), pg(102, yi + BAR_LINE), pg(80, yi + BAR_LINE)]))
    d.fill("green", polygon([pg(80, yo), pg(102, yo), pg(102, ym - BAR_LINE / 2), pg(80, ym - BAR_LINE / 2)]))
    d.fill("white", polygon([pg(80, (yo + ym) / 2 - 0.6), pg(102, (yo + ym) / 2 - 0.6), pg(102, (yo + ym) / 2 + 0.6),
                             pg(80, (yo + ym) / 2 + 0.6)]))
    d.fill("gold", polygon([pg(80, ym + BAR_LINE / 2), pg(102, ym + BAR_LINE / 2), pg(102, yi), pg(80, yi)]))
    # patte d'or festonnée autour du rinceau
    pad = festons([pg(XG, -3, 0), pg(56, 2, 4), pg(42, 24, 4), pg(40, 50, 4), pg(36, 76, 4), pg(40, 102, 4),
                   pg(52, 122, 4), pg(74, 126, 4), pg(90, 134, 0), pg(98, 112, 0), pg(98, -3, 0)])
    d.outlined("gold", pad, o=BAR_LINE)
    # filet vert du côté qui descend dans la patte et s'enroule au-dehors
    filet_vert(d, [pg(XV, -4), pg(XV, 40), pg(XV - 1, 96), pg(XV - 6, 114), pg(XV - 16, 120)], LINE_W, 3.6)
    d.stroke("outline", vrille(*pg(XV - 16, 120), 180, 5), 3.6 + 2 * BAR_LINE)
    d.stroke("green", vrille(*pg(XV - 16, 120), 180, 5), 3.6)
    # acanthe rose au bout du rinceau, recourbée vers le dehors
    t = affine(*pg(56, 92), rot=-118)
    d.outlined("pink", acanthe(46, 20).transformed(t), o=BAR_LINE)
    d.stroke("pinkLight", smooth([(4, 0), (22, -2), (38, -5)]).transformed(t), 1.3)
    # rinceau rose en huit, fleurettes bleues dans les boucles
    for Yc in (36, 78):
        X, Yy = pg(62, Yc)
        ring = circle(X, Yy, 15)
        d.stroke("outline", ring, 7.8)
        d.stroke("pink", ring, 5.6)
        d.stroke("pinkLight", arc(X, Yy, 15, 130, 210), 1.3)
        fleurette(d, X, Yy, 9.5)
    return boite(d.ops(), 100, h, outset=44)


# --------------------------------------------------------------------------- #
LAYOUT = {"margin": 16, "top": T, "side": S, "bottom": B, "textPadding": 10}


def spec():
    return cadre(
        name="vigne",
        title="Vigne aux oiseaux",
        source="paroissien chromolithographié, fin du XIXᵉ siècle, cadre sans texte « vigne aux oiseaux »",
        inks=INKS,
        palettes=PALETTES,
        layout=LAYOUT,
        ground=None,
        top=module_haut(),
        side=module_cote(),
        bottom=module_pied(),
        centers={"top": centre_haut(), "side": centre_cote(), "bottom": centre_pied()},
        corners={"top": coin_haut(), "bottom": coin_pied()},
    )
