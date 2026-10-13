"""
Cadre « Prières avant la messe » (p.45) : camaïeu d'or, de blanc d'argent et de bistre sur le papier. Chaque bande est
bordée de filets bruns doublés d'un liseré blanc. En haut, des postes : un ruban blanc cerné d'or qui s'enroule en
volutes, avec des feuilles hachurées sombres, et un losange blanc cerné d'or au milieu. Sur les côtés, une colonne de
carreaux alternés, croix sombre sur carré d'or puis treillis en losanges, avec une rosace d'or au milieu. Au pied,
plus haut, des volutes affrontées en mandorles de part et d'autre d'un nœud tressé dans un médaillon d'argent. Aux
coins hauts, deux têtes de chien sur des cornes d'or entrelacées ; aux coins du pied, une tresse sur fond d'argent.
"""
import math

from commun import (Path, affine, arc, boite, cadre, circle, diamond, fill, frise, leaf, outlined, piece, polygon, rect,
                    smooth, stroke, strand, teardrop)

ORDRE = 45

INKS = {
    "ground": "#F1E7D1",   # fond crème des bandes (le papier, à peine plus soutenu)
    "white": "#FDFAF1",    # blanc d'argent : rubans, liserés, losange
    "gold": "#C09D5E",     # or (ocre, remonté)
    "silver": "#CEC5BA",   # argent gris des médaillons et des ombres du treillis
    "olive": "#8C7D63",    # argent sombre (gris olive) des rinceaux du pied
    "bistre": "#5E5551",   # gris bistre des feuilles hachurées et des croix
    "outline": "#7A5848",  # brun des filets et des contours
}

# Le camaïeu reste ; la couleur liturgique teinte les accents sombres, l'argent et le fond.
PALETTES = {
    "GREEN":  {"bistre": "#3D5A43", "silver": "#BAC6B0", "ground": "#ECEBD6"},
    "VIOLET": {"bistre": "#57406E", "silver": "#C6BACF", "ground": "#EFE7E6"},
    "RED":    {"bistre": "#8A2F28", "silver": "#D9B7AC", "ground": "#F3E5D3"},
    "ROSE":   {"bistre": "#A0566D", "silver": "#E2C4C9", "ground": "#F5E7E1"},
    "WHITE":  {"bistre": "#9C8458", "silver": "#E3DACA", "ground": "#F8F2E3"},
    "BLACK":  {"bistre": "#2C2927", "silver": "#A9A49E", "gold": "#A29D93", "olive": "#6E6A64", "outline": "#3A3532"},
}

# Épaisseurs des bandes (dp) : le pied fait 1,7 fois les autres, comme sur la page.
TOP = 18
FOOT = 31
RF = TOP / FOOT             # unités de la bande haute -> unités du pied
H_FOOT = 100.0 * FOOT / TOP  # hauteur de la case de coin du pied, en unités de la bande haute

# Groupe de filets (unités de la bande haute) : brun, liseré blanc, brun.
G = 12.0
GL = 2.6


# --------------------------------------------------------------------------- #
# Outils
# --------------------------------------------------------------------------- #
def filets(ops, u0, u1, v0, k=1.0):
    """Groupe de filets le long d'une bande, de v0 vers +v. `k` : échelle (RF pour le pied).
    Le liseré déborde le brun d'une unité : sinon, au raccord de deux éléments, le bord du brun de l'un
    transparaît sous le bord du liseré de l'autre (fine couture sombre)."""
    g, l = G * k, GL * k
    ops.append(fill("outline", rect(u0, v0, u1 - u0, g)))
    ops.append(fill("white", rect(u0 - 1, v0 + l, u1 - u0 + 2, g - 2 * l)))


def chain(*paths):
    """Met bout à bout des chemins ouverts (le M des suivants devient un L)."""
    p = Path()
    for i, q in enumerate(paths):
        for c in q.cmds:
            p.cmds.append(("L", c[1]) if (c[0] == "M" and i) else c)
    return p


def volute(cx, cy, r0, r1, turns, a0, sens=1, expo=1.0):
    """Points d'une spirale de r0 à r1 ; sens 1 = horaire à l'écran, -1 = antihoraire. `expo` < 1 resserre vite."""
    n = max(4, int(turns * 16))
    pts = []
    for i in range(n + 1):
        t = i / n
        a = math.radians(a0 + sens * 360 * turns * t)
        r = r0 + (r1 - r0) * t ** expo
        pts.append((cx + r * math.cos(a), cy + r * math.sin(a)))
    return pts


def plume(x, y, rot, L=15.0, n=4, h=8.0, w=2.0, slant=0.5):
    """Feuille hachurée : n traits sombres parallèles le long d'un axe pointé vers `rot`, de plus en plus courts."""
    t = affine(x, y, rot=rot)
    p = Path()
    for i in range(n):
        s = i / max(1, n - 1)
        xi = L * (0.1 + 0.85 * s)
        hi = h * (1 - 0.55 * s)
        p.M((xi - slant * hi / 2, -hi / 2)).L((xi + slant * hi / 2, hi / 2))
    return [stroke("bistre", p.transformed(t), w)]


def clip_seg(p, q, x0, y0, x1, y1):
    """Segment pq coupé au rectangle (Liang-Barsky), ou None."""
    t0, t1 = 0.0, 1.0
    dx, dy = q[0] - p[0], q[1] - p[1]
    for pk, qk in ((-dx, p[0] - x0), (dx, x1 - p[0]), (-dy, p[1] - y0), (dy, y1 - p[1])):
        if pk == 0:
            if qk < 0:
                return None
        else:
            r = qk / pk
            if pk < 0:
                t0 = max(t0, r)
            else:
                t1 = min(t1, r)
    if t0 >= t1:
        return None
    return (p[0] + t0 * dx, p[1] + t0 * dy), (p[0] + t1 * dx, p[1] + t1 * dy)


def clip_poly(pts, x0, y0, x1, y1):
    """Polygone coupé au rectangle (Sutherland-Hodgman)."""
    def cut(pts, inside, inter):
        out = []
        for i, p in enumerate(pts):
            q = pts[i - 1]
            if inside(p):
                if not inside(q):
                    out.append(inter(q, p))
                out.append(p)
            elif inside(q):
                out.append(inter(q, p))
        return out
    ix = lambda x: (lambda q, p: (x, q[1] + (p[1] - q[1]) * (x - q[0]) / (p[0] - q[0])))
    iy = lambda y: (lambda q, p: (q[0] + (p[0] - q[0]) * (y - q[1]) / (p[1] - q[1]), y))
    for inside, inter in ((lambda p: p[0] >= x0, ix(x0)), (lambda p: p[0] <= x1, ix(x1)),
                          (lambda p: p[1] >= y0, iy(y0)), (lambda p: p[1] <= y1, iy(y1))):
        if not pts:
            break
        pts = cut(pts, inside, inter)
    return pts


def tresse(x0, y0, x1, y1, m, n, width, outline=1.4, rho=0.9, k=12):
    """Tresse fermée dans un rectangle, m cases de large et n de haut (m impair, n pair, premiers entre eux) :
    une trajectoire de billard aux rebonds arrondis, dessus-dessous alternés à chaque croisement."""
    cx, cy, ax, ay = (x0 + x1) / 2, (y0 + y1) / 2, (x1 - x0) / 2, (y1 - y0) / 2
    g = lambda th: math.asin(rho * math.sin(th)) / math.asin(rho)
    T = 2 * m * n // math.gcd(m, n)
    N = T * k
    pts = [(cx + ax * g(math.pi * i / k / m), cy + ay * g(math.pi * i / k / n)) for i in range(N)]
    # croisements : deux passages éloignés le long du chemin, au même endroit
    cell = min(ax / m, ay / n)
    hits = []
    for i in range(N):
        for j in range(i + k, N):
            if min(j - i, N - (j - i)) < k:
                continue
            d = math.dist(pts[i], pts[j])
            if d < 0.12 * cell:
                hits.append((d, i, j))
    hits.sort()
    near = lambda a, b: min((a - b) % N, (b - a) % N) <= k // 3
    cross = []
    for d, i, j in hits:
        if not any(near(i, a) and near(j, b) for a, b in cross):
            cross.append((i, j))
    # en suivant le chemin, les croisements passent alternativement dessus et dessous
    events = sorted([(i, c) for c, (i, j) in enumerate(cross)] + [(j, c) for c, (i, j) in enumerate(cross)])
    over = {c: t for idx, (t, c) in enumerate(events) if idx % 2 == 0}
    assert len(over) == len(cross), "tresse non alternée"
    ops = strand(smooth(pts, closed=True, tension=1.0), width=width, ink="white", outline=outline)
    for c, t in over.items():
        seg = lambda h: smooth([pts[(t + s) % N] for s in range(-h, h + 1)])
        ops.append(stroke("outline", seg(k // 3), width + 2 * outline))
        ops.append(stroke("white", seg(k // 3 + 2), width))
    return ops


# --------------------------------------------------------------------------- #
# Frise haute : postes (ruban blanc cerné d'or, en volutes)
# --------------------------------------------------------------------------- #
def module_postes():
    P, c, R = 88.0, 50.0, 24.0
    turns, r_eye, expo = 1.55, 4.5, 1.5
    W, O = 7.0, 2.0
    base, over = [], []
    filets(base, -P / 2 - 1, P / 2 + 1, 0)
    filets(base, -P / 2 - 1, P / 2 + 1, 100 - G)

    # la volute naît en bas (où arrive le ruban précédent) et s'enroule à rebours ;
    # le ruban suivant part de son sommet et descend vers le bas de la volute voisine
    pts = volute(0, c, R, r_eye, turns, 90, sens=-1, expo=expo)
    r_top = R + (r_eye - R) * (0.5 / turns) ** expo
    curl = smooth(pts)
    conn = Path().M((0, c - r_top)).C((0.42 * P, c - r_top), (0.58 * P, c + R), (P, c + R))
    eye = pts[-1]
    for p in (curl, conn):
        base.append(stroke("gold", p, W + 2 * O, round=True))
    base.append(fill("gold", circle(eye[0], eye[1], 3.8 + O)))
    for p in (curl, conn):
        over.append(stroke("white", p, W, round=True))
    over.append(fill("white", circle(eye[0], eye[1], 3.8)))

    # feuilles hachurées : deux au-dessus du ruban (contre la volute suivante), deux dessous
    # (symétriques par rapport au milieu du ruban)
    for (x, y, rot) in ((48, 18, 180), (60, 31, -120)):
        base += plume(x, y, rot, L=16, n=5, h=9, w=2.3)
        base += plume(P - x, 100 - y, rot + 180, L=16, n=5, h=9, w=2.3)
    return frise(P, base, over, phase=0.5)


def piece_losange():
    """Losange du milieu de la bande haute : brun, liseré blanc, filet d'or, cœur blanc. Il déborde un peu en haut."""
    Wd = 116.0
    ops = []
    filets(ops, -Wd / 2, Wd / 2, 0)
    filets(ops, -Wd / 2, Wd / 2, 100 - G)
    cy, a, b = 46.0, Wd / 2, 54.0     # la pointe touche le bord de la pièce, où arrive le ruban coupé
    dia = lambda inset: diamond(0, cy, 2 * (a - inset * math.sqrt(2)), 2 * (b - inset * math.sqrt(2)))
    for ink, ins in (("outline", 0), ("white", 1.8), ("outline", 7.0), ("gold", 8.6), ("outline", 12.6),
                     ("white", 14.0)):
        ops.append(fill(ink, dia(ins)))
    return piece(Wd, ops, outset=10)


# --------------------------------------------------------------------------- #
# Frise des côtés : carreaux alternés, rosace au milieu
# --------------------------------------------------------------------------- #
def module_carreaux():
    A, Bh = 70.0, 76.0          # carreau à croix, carreau en treillis
    P = A + Bh
    c = 50.0
    v0, v1 = G, 100 - G
    base, over = [], []
    u0, u1 = -A / 2, -A / 2 + P
    base.append(fill("white", rect(u0 - 0.5, v0, P + 1, v1 - v0)))

    # carreau en treillis (centre u = cb) : losanges alternés blanc / argent
    cb = A / 2 + Bh / 2
    d = 13.0
    x0, x1 = cb - Bh / 2, cb + Bh / 2
    shade = Path()
    for a in range(-4, 5):
        for b in range(-4, 5):
            if (a + b) % 2 == 0:
                continue
            px, py = cb + (a + b) * d, c + (a - b) * d
            q = clip_poly([(px, py - d), (px + d, py), (px, py + d), (px - d, py)], x0, v0, x1, v1)
            if len(q) >= 3:
                shade.extend(polygon(q))
    base.append(fill("silver", shade))
    lattice = Path()
    for kk in (-5, -3, -1, 1, 3, 5):
        for sg in (1, -1):
            s = clip_seg((cb + kk * d - 60, c - sg * 60), (cb + kk * d + 60, c + sg * 60), x0, v0, x1, v1)
            if s:
                lattice.M(s[0]).L(s[1])
    base.append(stroke("gold", lattice, 2.0))
    base.append(stroke("bistre", circle(cb, c, 3.6), 2.0))
    darts = Path()
    for k in range(4):
        ang = 90 * k
        ca, sa = math.cos(math.radians(ang)), math.sin(math.radians(ang))
        darts.extend(leaf(8, 3.6).transformed(affine(cb + 30 * ca, c + 30 * sa, rot=ang + 180)))
    base.append(fill("bistre", darts))

    # carreau à croix (centre u = 0) : carré d'or, losange inscrit, diagonales
    s = 26.0
    gl = Path()
    gl.extend(rect(-s, c - s, 2 * s, 2 * s))
    gl.extend(diamond(0, c, 2 * s))
    gl.M((-A / 2, c - A / 2)).L((A / 2, c + A / 2))
    gl.M((-A / 2, c + A / 2)).L((A / 2, c - A / 2))
    base.append(stroke("gold", gl, 2.0))
    # séparations d'or entre carreaux
    sep = Path()
    for x in (-A / 2, A / 2):
        sep.M((x, v0)).L((x, v1))
    base.append(stroke("gold", sep, 2.6))
    # croix sombre à quatre pétales effilés
    petals = Path()
    for k in range(4):
        petals.extend(leaf(29, 5.5).transformed(affine(0, c, rot=90 * k)))
    base.append(fill("bistre", petals))
    base.append(fill("bistre", circle(0, c, 3.4)))

    filets(base, u0 - 1, u1 + 1, 0)
    filets(base, u0 - 1, u1 + 1, 100 - G)
    return frise(P, base, over, phase=(A / 2) / P)


def piece_rosace():
    """Carreau du milieu des côtés : rosace d'or à quatre pétales, rinceaux aux angles, doubles filets d'or."""
    Wd = 118.0
    c = 50.0
    v0, v1 = G, 100 - G
    ops = []
    ops.append(fill("white", rect(-Wd / 2, v0, Wd, v1 - v0)))
    lines = Path()
    for x in (Wd / 2 - 1.5, Wd / 2 - 7.5):
        for sg in (-1, 1):
            lines.M((sg * x, v0)).L((sg * x, v1))
    ops.append(stroke("gold", lines, 2.2))
    # rinceaux d'angle : une feuille vers chaque angle, deux folioles
    sprigs = Path()
    for sx in (-1, 1):
        for sy in (-1, 1):
            ang = math.degrees(math.atan2(sy, sx))
            sprigs.extend(teardrop(14, 8).transformed(affine(sx * 24, c + sy * 21, rot=ang)))
            sprigs.extend(teardrop(9, 5).transformed(affine(sx * 30, c + sy * 26, rot=ang - 60)))
            sprigs.extend(teardrop(9, 5).transformed(affine(sx * 30, c + sy * 26, rot=ang + 60)))
    ops += outlined("gold", sprigs, outline=1.0)
    # rosace : quatre pétales d'or, ombre sombre d'un côté, cœur blanc cerné
    petals = Path()
    for k in range(4):
        petals.extend(teardrop(30, 24).transformed(affine(0, c, rot=90 * k)))
    ops += outlined("gold", petals, outline=1.3)
    shadow = Path()
    for k in range(4):
        shadow.extend(Path().M((7, 4.5)).C((13, 10), (22, 9), (28, 2)).transformed(affine(0, c, rot=90 * k)))
    ops.append(stroke("bistre", shadow, 2.4, round=True))
    ops += outlined("white", circle(0, c, 7.5), outline=1.3)
    ops.append(stroke("bistre", circle(0, c, 4.2), 2.0))
    filets(ops, -Wd / 2, Wd / 2, 0)
    filets(ops, -Wd / 2, Wd / 2, 100 - G)
    return piece(Wd, ops)


# --------------------------------------------------------------------------- #
# Frise du pied : volutes affrontées en mandorles ; nœud tressé au milieu
# --------------------------------------------------------------------------- #
def module_rinceau():
    P = 52.0
    c = 50.0
    cu, r0 = 31.0, 15.0          # volute haute : centre, rayon de départ
    lw = 2.6
    g = G * RF
    base, over = [], []
    filets(base, -P / 2 - 1, P / 2 + 1, 0, RF)
    filets(base, -P / 2 - 1, P / 2 + 1, 100 - g, RF)
    top = cu - r0
    # arc de la mandorle : du croisement gauche, par-dessus la volute haute, au croisement droit
    arch = (Path().M((-P / 2, c)).C((-P / 2 + 6, c - 20), (-14, top), (0, top))
            .C((14, top), (P / 2 - 6, c - 20), (P / 2, c)))
    pts = volute(0, cu, r0, 2.5, 1.6, -90, sens=1, expo=1.2)
    lines = Path().extend(arch).extend(smooth(pts))
    flip = lambda p: (p[0], 100 - p[1])
    base.append(stroke("olive", lines, lw, round=True))
    base.append(stroke("olive", lines.transformed(flip), lw, round=True))
    eye = pts[-1]
    base.append(fill("silver", circle(eye[0], eye[1], 3.0)))
    base.append(fill("silver", circle(eye[0], 100 - eye[1], 3.0)))
    # feuilles hachurées dans les creux au-dessus et au-dessous des croisements ; feuilles pleines entre les volutes
    base += plume(P / 2, g + 2, 90, L=24, n=5, h=12, w=2.2)
    base += plume(P / 2, 100 - g - 2, -90, L=24, n=5, h=12, w=2.2)
    pockets = Path()
    for sx in (1, -1):
        pockets.extend(leaf(11, 5.5).transformed(affine(sx * 10, c, rot=0 if sx > 0 else 180)))
    base.append(fill("bistre", pockets))
    return frise(P, base, over, phase=0.5)


def piece_noeud():
    """Médaillon d'argent bordé de deux arcs de filets, nœud tressé blanc cerné de brun."""
    Wd = 84.0
    g, l = G * RF, GL * RF
    q = 86.1                       # rayon extérieur des arcs : ils passent par (±Wd/2, 50)
    X = q - Wd / 2                 # centres en (±X, 50)
    a = math.degrees(math.asin(50 / q))
    ops = []
    ca = q * math.cos(math.radians(a))
    barrel = arc(X, 50, q, 180 + a, 180 - a)
    barrel.L((-X + ca, 100))
    arc(-X, 50, q, a, -a, start=False, path=barrel)
    barrel.Z()
    ops.append(fill("silver", barrel))
    browns, whites = Path(), Path()
    browns.extend(rect(-Wd / 2, 0, Wd, g)).extend(rect(-Wd / 2, 100 - g, Wd, g))
    whites.extend(rect(-Wd / 2, l, Wd, g - 2 * l)).extend(rect(-Wd / 2, 100 - g + l, Wd, g - 2 * l))
    ops.append(fill("outline", browns))
    for sx in (1, -1):
        ops.append(stroke("outline", arc(sx * X, 50, q - g / 2, (180 if sx > 0 else 0) - a - 6,
                                         (180 if sx > 0 else 0) + a + 6), g))
    ops.append(fill("white", whites))
    for sx in (1, -1):
        ops.append(stroke("white", arc(sx * X, 50, q - g / 2, (180 if sx > 0 else 0) - a - 6,
                                       (180 if sx > 0 else 0) + a + 6), g - 2 * l))
    # les liserés des arcs s'arrêtent aux filets bruns du bord
    ops.append(fill("outline", Path().extend(rect(-Wd / 2, 0, Wd, l)).extend(rect(-Wd / 2, 100 - l, Wd, l))))
    ops += tresse(-23, 15, 23, 85, 3, 4, width=4.6, outline=1.3)
    return piece(Wd, ops)


# --------------------------------------------------------------------------- #
# Coins
# --------------------------------------------------------------------------- #
def croisillons(ops, w, h, top=True, bottom=True, left=True, right=True):
    """Filets du pourtour d'une case de coin, croisés aux angles : les bruns d'abord, puis les liserés blancs.
    Côté extérieur (x = 0, y = 0), les liserés s'arrêtent au filet brun du bord ; côté intérieur, ils continuent
    dans la bande voisine."""
    browns, whites = Path(), Path()
    if top:
        browns.extend(rect(0, 0, w, G)); whites.extend(rect(GL, GL, w - GL, G - 2 * GL))
    if bottom:
        browns.extend(rect(0, h - G, w, G)); whites.extend(rect(GL, h - G + GL, w - GL, G - 2 * GL))
    if left:
        browns.extend(rect(0, 0, G, h)); whites.extend(rect(GL, GL, G - 2 * GL, h - GL))
    if right:
        browns.extend(rect(w - G, 0, G, h)); whites.extend(rect(w - G + GL, GL, G - 2 * GL, h - GL))
    ops.append(fill("outline", browns))
    ops.append(fill("white", whites))


def corne(t=lambda p: p):
    """Corne d'or (coin haut gauche) : embouchure en haut à gauche, sous la tête ; elle plonge, croise sa
    jumelle au milieu et sa queue file de l'autre côté pour s'enrouler. Renvoie (corps, queue).
    `t` : miroir pour la corne de droite."""
    outer = smooth([(4, 17), (6, 34), (16, 50), (35, 63), (57, 71), (73, 74)])
    inner = smooth([(73, 74), (57, 67), (40, 58), (28, 46), (22.5, 31), (21.5, 17)])
    body = outlined("gold", chain(outer, inner).Z().transformed(t), outline=1.5)
    stripe = chain(smooth([(10, 19), (12, 34), (20, 48), (40, 62)]), smooth([(40, 62), (25, 46), (17, 33), (16, 19)]))
    body.append(fill("white", stripe.Z().transformed(t)))
    tail = smooth([(62, 72), (72, 74), (82, 75), (89, 69), (91, 60), (86, 55), (81, 59)])
    return body, strand(tail.transformed(t), width=4.0, ink="gold", outline=1.4)


def module_coin_haut():
    """Nœud de deux cornes d'or croisées et d'une mandorle, sur le papier nu comme dans le livre ; les têtes sont
    en ornement extérieur. Les queues passent sous les cornes, la mandorle par-dessus tout."""
    ops = [fill("paper", rect(0, 0, 100, 100 - G))]
    croisillons(ops, 100, 100, top=False, left=False, right=False)
    ops.append(fill("white", smooth([(22, 17), (50, 10), (78, 17), (72, 40), (50, 62), (28, 40)], closed=True)))
    left = smooth([(50, 84), (39, 64), (36, 36), (50, 6)])
    right = smooth([(50, 6), (64, 36), (61, 64), (50, 84)])
    (b1, t1), (b2, t2) = corne(), corne(lambda p: (100 - p[0], p[1]))
    ops += t1 + t2 + b2 + b1
    ops += strand(chain(left, right), width=4.0, ink="gold", outline=1.4)
    return boite(ops)


def module_coin_pied():
    """Case du pied, plus haute que large : tresse blanche sur fond d'argent, filets croisés tout autour."""
    W, H = 100.0, H_FOOT
    ops = [fill("silver", rect(G, G, W - 2 * G, H - 2 * G))]
    ops += tresse(24, 22, 76, H - 22, 3, 4, width=4.6, outline=1.3)
    croisillons(ops, W, H)
    return boite(ops, W, H)


def tetes():
    """Deux têtes de chien dos à dos, au-dessus du coin haut (unités : 100 = épaisseur de la bande haute)."""
    def tete(t):
        # profil tourné vers l'extérieur (-x) : gueule ouverte, oreille pointue rabattue vers l'arrière
        pts = [(-18, -2), (-7, -7), (3, -11), (12, -13), (16, -14), (26, -22), (25, -10), (29, -3), (27, 10), (24, 15),
               (10, 15), (1, 10), (-13, 5), (-4, 1.5)]
        ops = outlined("silver", smooth(pts, closed=True, tension=0.6).transformed(t), outline=1.6)
        ops.append(fill("white", smooth([(-16, -2), (-6, -6), (4, -9), (12, -10), (8, -4), (-2, -1), (-10, -0.5)],
                                        closed=True).transformed(t)))
        ops.append(fill("outline", circle(6, -6, 2.1).transformed(t)))
        ops.append(stroke("outline", Path().M((17, -11)).L((23, -17)).transformed(t), 1.4, round=True))
        return ops
    return boite(tete(lambda p: p) + tete(lambda p: (100 - p[0], p[1])))


def spec():
    return cadre(
        name="avant_messe",
        title="Prières avant la messe",
        source="paroissien chromolithographié, fin du XIXᵉ siècle, p.45 « Prières avant la messe »",
        inks=INKS,
        palettes=PALETTES,
        layout={"margin": 14, "top": TOP, "side": TOP, "bottom": FOOT, "textPadding": 12},
        ground="ground",
        top=module_postes(),
        side=module_carreaux(),
        bottom=module_rinceau(),
        centers={"top": piece_losange(), "side": piece_rosace(), "bottom": piece_noeud()},
        corners={"top": module_coin_haut(), "bottom": module_coin_pied()},
        outer={"corner": tetes(), "cornerBottom": boite([])},
    )
