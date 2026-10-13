"""
Cadre « Prières du soir » (p.25) : fond lie-de-vin entre des filets d'argent qui encadrent bandes et coins.
En haut, deux rubans d'or se croisent en X et s'ouvrent en navettes garnies de volutes en cœur, bleues et vertes en
alternance, séparées par une mandorle ; sur les côtés, une chaîne de cœurs d'or à fleur de lys, doublés de bleu ou
de vert par paires, volutes aux nœuds et perles d'argent entre les lobes ; au pied, plus haut, un losange à double
fleuron entre deux navettes vertes, sous des feuilles d'acanthe. Un médaillon à anneau d'or au milieu de chaque
bande : quadrilobe d'argent à cœur rouge en haut, croix fleuronnée sur les côtés, quatre cœurs d'argent au pied.
Coins : nœud d'argent en losange sous un double pignon en haut, treillis d'argent au pied, flanqué au dehors d'une
volute d'ardoise en forme de dragon.
"""
import math

from commun import (Path, affine, boite, cadre, circle, diamond, filet, fill, fleur_de_lis, frise, leaf, mirror_y,
                    outlined, piece, polygon, rect, smooth, stroke, strand, teardrop, trefoil)

ORDRE = 25

INKS = {
    "ground": "#6B302B",   # lie-de-vin du fond des bandes
    "gold": "#CFAC60",     # or des rinceaux et des anneaux
    "green": "#4B7444",    # vert des navettes et des cœurs
    "blue": "#A3C2C6",     # bleu pâle des cellules et des médaillons
    "silver": "#C9C9C2",   # argent des filets, des nœuds et des quadrilobes
    "red": "#C2453B",      # rouge des lobes du médaillon haut, liseré des dragons
    "slate": "#6E6A69",    # ardoise des dragons du pied
    "outline": "#2C1A15",  # contour brun très foncé
}

# Encres remplacées selon la couleur liturgique : le fond et l'accent vert ; l'or et l'argent restent.
PALETTES = {
    "GREEN":  {"ground": "#2E4A34", "green": "#7BA25E"},
    "VIOLET": {"ground": "#47305F", "green": "#8E74B2"},
    "RED":    {"ground": "#8A1F1C", "green": "#D9894A"},
    "ROSE":   {"ground": "#9A4E64", "green": "#E3A9B8", "red": "#B84A6A"},
    "WHITE":  {"ground": "#EFE5CC", "gold": "#B8923E", "green": "#8FA56A"},
    "BLACK":  {"ground": "#2A2826", "green": "#6B6863", "blue": "#A3A19B", "red": "#8C8379", "gold": "#A9A397"},
}

STRAP = 7.0       # ruban d'or
STRAP_O = 1.4     # son contour
DOT_O = 1.0       # contour des perles et petits fleurons

# Pied 1,7 fois plus haut que le haut et les côtés, comme dans l'original (285 px contre 168).
LAYOUT = {"margin": 14, "top": 18, "side": 18, "bottom": 30, "textPadding": 14}
FOOT_H = 100.0 * LAYOUT["bottom"] / min(LAYOUT["top"], LAYOUT["side"])   # hauteur de la case du coin du pied
RULE = 8.0        # filet d'argent, à cheval sur les bords extérieur et intérieur des bandes
RULE_O = 1.5      # son contour


# --------------------------------------------------------------------------- #
# Outils
# --------------------------------------------------------------------------- #
def catmull(points, samples=0):
    """Courbe lisse (Catmull-Rom) par les points, sans le premier ni le dernier, qui ne servent qu'aux tangentes :
    deux éléments voisins tracent ainsi exactement le même raccord. Avec `samples`, renvoie des points (pour un
    remplissage) au lieu d'un chemin."""
    k = 1 / 6
    p = Path().M(points[1])
    pts = [points[1]]
    for i in range(1, len(points) - 2):
        p0, p1, p2, p3 = points[i - 1:i + 3]
        c1 = (p1[0] + (p2[0] - p0[0]) * k, p1[1] + (p2[1] - p0[1]) * k)
        c2 = (p2[0] - (p3[0] - p1[0]) * k, p2[1] - (p3[1] - p1[1]) * k)
        p.C(c1, c2, p2)
        for j in range(1, samples + 1):
            t = j / samples
            mt = 1 - t
            pts.append(tuple(mt ** 3 * p1[d] + 3 * mt * mt * t * c1[d] + 3 * mt * t * t * c2[d] + t ** 3 * p2[d]
                             for d in (0, 1)))
    return pts if samples else p


def gold_strand(path, width=STRAP):
    """Ruban d'or : contour foncé, puis l'or."""
    return strand(path, width=width, ink="gold", outline=STRAP_O)


def pearl(x, y, r=3.4, ink="silver"):
    return outlined(ink, circle(x, y, r), outline=DOT_O)


def volute_points(L, W, turns=0.85):
    """Lobe supérieur d'un cœur en volutes : de la pointe (0, 0) vers +x, puis la spirale qui s'enroule dans le lobe."""
    cx, cy, r1 = 0.66 * L, -0.24 * W, 0.30 * min(L, W * 1.2)
    pts = [(0, 0), (0.22 * L, -0.30 * W)]
    n = 9
    for i in range(n + 1):
        t = i / n
        a = math.radians(190 + 360 * turns * t)
        r = r1 * (1 - 0.68 * t)
        pts.append((cx + r * math.cos(a), cy + r * math.sin(a)))
    return pts


def coeur_volutes(x, y, rot, L, W, width=4.0, outline=1.2):
    """Cœur de deux volutes d'or, pointe en (x, y), lobes vers `rot` (degrés)."""
    t = affine(x, y, rot=rot)
    ops = []
    up = smooth(volute_points(L, W))
    for path in (up, up.transformed(mirror_y)):
        ops += strand(path.transformed(t), width=width, ink="gold", outline=outline)
    return ops


def heart(L, W, cusp=0.17):
    """Cœur couché : dos des lobes en x = 0, pointe en (L, 0), largeur W ; chemin fermé partant de la pointe."""
    h = W / 2
    up = (Path().M((L, 0))
          .C((0.70 * L, -0.34 * h), (0.52 * L, -h), (0.30 * L, -h))
          .C((0.10 * L, -h), (0, -0.80 * h), (0, -0.50 * h))
          .C((0, -0.22 * h), (cusp * 0.55 * L, -0.08 * h), (cusp * L, 0)))
    down = up.transformed(mirror_y)
    p = Path()
    p.cmds = list(up.cmds)
    # retour par la moitié basse, parcourue à l'envers
    segs = down.cmds[1:]
    ends = [down.cmds[0][1]] + [c[-1] for c in segs]
    for i in range(len(segs) - 1, -1, -1):
        p.C(segs[i][2], segs[i][1], ends[i])
    return p.Z()


def bourgeon(x, y, rot, L=11.0):
    """Petit éventail de trois gouttes d'or, pointé vers `rot` (degrés)."""
    p = Path()
    for a, k in ((-34, 0.72), (34, 0.72), (0, 1.0)):
        p.extend(teardrop(L * k, 0.55 * L * k).transformed(affine(rot=a)))
    return outlined("gold", p.transformed(affine(x, y, rot=rot)), outline=DOT_O)


# --------------------------------------------------------------------------- #
# Frise haute : deux rubans d'or croisés en X, navettes et mandorles
# --------------------------------------------------------------------------- #
P_TOP = 180.0
# profondeur du ruban A en fonction de u / P ; le ruban B est son miroir (v -> 100 - v)
ONDE = [(-0.87, 66), (-0.70, 81), (-0.50, 84), (-0.30, 81), (-0.13, 66), (0.0, 50), (0.13, 34), (0.30, 19),
        (0.50, 16), (0.70, 19), (0.87, 34)]


def onde(x0, x1, flip=False):
    """Points du ruban de x0 à x1 (fractions de période), avec un point de plus de chaque côté pour les tangentes."""
    xs = [x for x, _ in ONDE]
    i0, i1 = xs.index(x0) - 1, xs.index(x1) + 1
    return [(x * P_TOP, 100 - v if flip else v) for x, v in ONDE[i0:i1 + 1]]


def mandorle(cx, top, bot, w):
    return (Path().M((cx, top))
            .C((cx + w, top + 0.25 * (bot - top)), (cx + w, bot - 0.25 * (bot - top)), (cx, bot))
            .C((cx - w, bot - 0.25 * (bot - top)), (cx - w, top + 0.25 * (bot - top)), (cx, top)).Z())


def module_entrelacs():
    P = P_TOP
    base, over = [], []
    # navettes : bleue à droite du X, verte à gauche (la couleur alterne d'une navette à l'autre)
    for x0, x1, ink in ((0.0, 0.50, "blue"), (-0.50, 0.0, "green")):
        a = catmull(onde(x0, x1), samples=6)
        b = catmull(onde(x0, x1, flip=True), samples=6)
        base.append(fill(ink, polygon(a + b[::-1])))
    # mandorles (fond nu) au milieu des navettes
    for cx in (-P / 2, P / 2):
        base.append(fill("ground", mandorle(cx, 16, 84, 13)))
    # dans chaque navette : volutes en cœur, pointe vers le X, et une feuille d'or de part et d'autre
    for sg in (1, -1):
        base += coeur_volutes(sg * 0.10 * P, 50, 0 if sg > 0 else 180, 50, 44, width=5.0)
        for sv in (-1, 1):
            base += outlined("gold", leaf(26, 10, bend=0.10 * sv).transformed(
                affine(sg * 0.24 * P, 50 + sv * 6, rot=(-28 * sv if sg > 0 else 180 + 28 * sv))), outline=1.2)
    # fleurs de lys d'or au-dessus et au-dessous du X, vers les bords
    for sv in (-1, 1):
        base += outlined("gold", fleur_de_lis(30).transformed(affine(0, 50 + sv * 9, rot=0 if sv < 0 else 180)),
                         outline=1.2)
    # rubans et mandorles d'or
    for cx in (-P / 2, P / 2):
        base += gold_strand(mandorle(cx, 16, 84, 13), width=5.0)
    for flip in (False, True):
        base += gold_strand(catmull(onde(-0.70, 0.70, flip)))
    # bourgeons aux pointes des mandorles, vers les bords
    for cx in (-P / 2, P / 2):
        over += bourgeon(cx, 17, -90, L=9.5)
        over += bourgeon(cx, 83, 90, L=9.5)
    return frise(P, base, over, phase=0.0)


# --------------------------------------------------------------------------- #
# Frise des côtés : chaîne de cœurs à fleur de lys
# --------------------------------------------------------------------------- #
P_SIDE = 360.0     # deux paires de cœurs : bleue puis verte
HEART_L = 90.0
HEART_W = 78.0


def coeur_fleuri(x, rot, lining):
    """Cœur doublé de couleur, fleur de lys d'or vers la pointe ; dos des lobes en (x, 50), orienté par rot.
    Renvoie l'intérieur et le chemin du ruban, dessiné à part pour que les rubans voisins se raccordent."""
    t = affine(x, 50, rot=rot)
    h = heart(HEART_L, HEART_W)
    ops = [fill(lining, h.transformed(t))]
    inner = heart(HEART_L * 0.80, HEART_W * 0.74).transformed(affine(0.12 * HEART_L, 0))
    ops.append(fill("ground", inner.transformed(t)))
    ops += outlined("gold", fleur_de_lis(64).transformed(affine(0.18 * HEART_L, 0, rot=90)).transformed(t),
                    outline=1.2)
    return ops, h.transformed(t)


def module_coeurs():
    P = P_SIDE
    base, over = [], []
    straps = []
    for k, lining in enumerate(("blue", "green")):
        u0 = k * P / 2
        for x, rot in ((u0, 0), (u0 + 2 * HEART_L, 180)):
            ops, h = coeur_fleuri(x, rot, lining)
            base += ops
            straps.append(h)
    # tous les contours d'abord (passe de base), l'or ensuite (passe de dessus) : pas de liseré aux raccords
    for h in straps:
        base.append(stroke("outline", h, STRAP + 2 * STRAP_O))
    for h in straps:
        over.append(stroke("gold", h, STRAP))
    # nœuds (pointe contre pointe) : volutes en cœur de part et d'autre, losange d'argent
    for k in range(2):
        un = k * P / 2 + HEART_L
        for sv in (-1, 1):
            over += coeur_volutes(un, 50 + sv * 7, 90 * sv, 33, 34, width=4.0)
        over += outlined("silver", diamond(un, 50, 8, 8), outline=DOT_O)
    # jonctions (lobe contre lobe) : perles d'argent
    for k in range(3):
        for v in (12, 50, 88):
            over += pearl(k * P / 2, v, 3.4)
    return frise(P, base, over, phase=0.0)


# --------------------------------------------------------------------------- #
# Frise du pied : losange à double fleuron entre deux navettes, feuilles d'acanthe
# --------------------------------------------------------------------------- #
P_FOOT = 140.0
# ruban A du pied (moitié droite) : sommet du losange en u = 0, croisement en u = 34, creux de la navette en u = 70,
# symétrique autour de u = 70 ; le premier point ne sert qu'à la tangente au sommet. Le ruban B est son miroir en v.
PIED = [(-8, -1), (0, 9), (10, 18), (22, 33), (34, 50), (48, 63), (70, 71), (92, 63), (106, 50), (118, 33)]


def pied(sx=1, flip=False, start=0, end=len(PIED)):
    return [(sx * u, 100 - v if flip else v) for u, v in PIED[start:end]]


def acanthe(sx, sy):
    """Feuille d'acanthe couchée le long du bord, volute au-dessus de la navette, pointe vers le sommet du
    losange ; sx, sy : ±1 pour les quatre quarts de l'élément."""
    t = lambda p: (sx * p[0], 50 + sy * (p[1] - 50))
    blade = (Path().M((62, 25))
             .C((50, 25), (30, 20), (11, 9))
             .C((26, 6), (40, 6), (50, 9))
             .C((46, 12), (50, 14), (57, 12))
             .C((63, 10), (68, 13), (70, 18)).Z())
    ops = outlined("gold", blade.transformed(t), outline=1.3)
    ops.append(stroke("outline", smooth([(54, 20), (40, 16), (24, 12)]).transformed(t), 1.2, round=True))
    curl = smooth([(60, 25), (69, 23), (71, 15), (64, 11), (59, 15), (62, 19)])
    ops += strand(curl.transformed(t), width=4.4, ink="gold", outline=1.2)
    return ops


def module_losanges():
    P = P_FOOT
    base, over = [], []
    # cellules bleues autour du centre du losange, demi-navettes vertes (jusqu'à la limite de l'élément : la
    # moitié voisine est remplie par le voisin, qui ne recouvre donc pas les volutes de celui-ci)
    base.append(fill("blue", diamond(0, 50, 58, 46)))
    for sx in (-1, 1):
        a = catmull(pied(sx, False, 3, 8), samples=6)
        b = [(u, 100 - v) for u, v in a]
        base.append(fill("green", polygon(a + b[::-1])))
    # double fleuron dans le losange, petit losange de fond au centre
    for sv in (-1, 1):
        base += outlined("gold", fleur_de_lis(44).transformed(affine(0, 50 + sv * 3, rot=0 if sv < 0 else 180)),
                         outline=1.2)
    base += outlined("ground", diamond(0, 50, 10, 16), outline=1.0)
    # volutes en cœur dans chaque demi-navette, pointe vers le losange
    for sx in (-1, 1):
        base += coeur_volutes(sx * 40, 50, 0 if sx > 0 else 180, 30, 30, width=4.4)
    # feuilles d'acanthe
    for sx in (-1, 1):
        for sy in (-1, 1):
            base += acanthe(sx, sy)
    # rubans : du sommet du losange jusqu'au-delà du creux de la navette, recouvrement identique chez le voisin
    for flip in (False, True):
        for sx in (-1, 1):
            base += gold_strand(catmull(pied(sx, flip)), width=6.0)
    return frise(P, base, over, phase=0.5)


# --------------------------------------------------------------------------- #
# Médaillons (pièces centrales) : l'anneau touche les deux bords de la bande et recouvre les filets, comme dans
# l'original où les médaillons interrompent l'encadrement d'argent.
# --------------------------------------------------------------------------- #
RING_W = 8.0


def anneau(r_out, w=RING_W):
    """Anneau d'or de rayon extérieur r_out (contour compris), centré en (0, 50)."""
    return gold_strand(circle(0, 50, r_out - STRAP_O - w / 2), width=w)


def medaillon_haut():
    """Quadrilobe d'argent à cœur rouge, rayons d'or en diagonale, dans un anneau d'or."""
    c, k = 50.0, 1.15
    ops = [fill("blue", circle(0, c, 42))]
    for i in range(4):
        ops += outlined("gold", teardrop(34 * k, 9 * k).transformed(affine(0, c, rot=45 + 90 * i)), outline=1.0)
    lobes = Path()
    reds = Path()
    for i in range(4):
        a = math.radians(90 * i)
        lobes.extend(circle(19 * k * math.cos(a), c + 19 * k * math.sin(a), 13 * k))
        reds.extend(circle(22 * k * math.cos(a), c + 22 * k * math.sin(a), 6.5 * k))
    ops.append(stroke("outline", lobes, 2 * 3.4 + 2 * 1.2))
    ops.append(stroke("gold", lobes, 2 * 3.4))
    ops.append(fill("silver", lobes))
    ops.append(fill("red", reds))
    ops += pearl(0, c, 5.0, ink="gold")
    ops += anneau(50)
    return piece(100, ops)


def medaillon_cote():
    """Croix fleuronnée d'argent sur fond bleu, perles d'or dans les cantons, dans un anneau d'or."""
    c, k = 50.0, 1.15
    ops = [fill("blue", circle(0, c, 42))]
    cross = Path()
    for i in range(4):
        t = affine(0, c, rot=90 * i)
        cross.extend(polygon([(0, -6 * k), (20 * k, -7 * k), (20 * k, 7 * k), (0, 6 * k)]).transformed(t))
        cross.extend(trefoil(20 * k).transformed(affine(19 * k, 0, rot=90)).transformed(t))
    ops += outlined("silver", cross, outline=1.3)
    for i in range(4):
        a = math.radians(45 + 90 * i)
        ops += pearl(13 * k * math.cos(a), c + 13 * k * math.sin(a), 3.6 * k, ink="gold")
    ops += anneau(50)
    return piece(100, ops)


def medaillon_pied():
    """Quatre cœurs d'argent, pointes au centre, croisillon d'argent, dans un grand anneau d'or."""
    c, k = 50.0, 1.08
    ops = [fill("blue", circle(0, c, 42))]
    xs = Path()
    for i in range(4):
        a = math.radians(45 + 90 * i)
        xs.extend(Path().M((0, c)).L((42 * math.cos(a), c + 42 * math.sin(a))))
    ops += strand(xs, width=4.0, ink="silver", outline=1.1)
    for i in range(4):
        t = affine(0, c, rot=90 * i + 180)
        ops += outlined("silver", heart(34 * k, 30 * k).transformed(affine(-38 * k, 0)).transformed(t), outline=1.2)
        for sy in (-1, 1):
            vol = smooth(volute_points(14 * k, 12 * k))
            if sy > 0:
                vol = vol.transformed(mirror_y)
            vol = vol.transformed(affine(-34 * k, 4 * k * sy)).transformed(t)
            ops.append(stroke("outline", vol, 1.2, round=True))
    ops += pearl(0, c, 5.0, ink="gold")
    ops += anneau(48, w=7.0)
    return piece(98, ops)


# --------------------------------------------------------------------------- #
# Coins
# --------------------------------------------------------------------------- #
def module_noeud():
    """Coin haut : nœud d'argent en losange, cellules vertes, triangles de fond aux angles."""
    c = 50.0
    R = 44.0
    q = R / 2
    offs = (-15.0, 0.0, 15.0)
    ops = [fill("green", diamond(c, c, 2 * R - 4))]
    lines = Path()
    for d in offs:
        # parallèles aux bords du losange (directions (1, 1) et (1, -1)), décalées de d
        lines.extend(Path().M((c - q + d / 2 - 2, c - q - d / 2 - 2)).L((c + q + d / 2 + 2, c + q - d / 2 + 2)))
        lines.extend(Path().M((c + q + d / 2 + 2, c - q + d / 2 - 2)).L((c - q + d / 2 - 2, c + q + d / 2 + 2)))
    ops += strand(diamond(c, c, 2 * R - 8), width=5.4, ink="silver", outline=1.2)
    ops += strand(lines, width=5.0, ink="silver", outline=1.2)
    # dessus-dessous : un croisement sur deux, un tronçon de la diagonale « \ » repasse dessus
    for i, d1 in enumerate(offs):
        for j, d2 in enumerate(offs):
            if (i + j) % 2:
                continue
            x, y = c + (d1 + d2) / 2, c + (d2 - d1) / 2
            ops.append(stroke("outline", Path().M((x - 3, y - 3)).L((x + 3, y + 3)), 5.0 + 2.4))
            ops.append(stroke("silver", Path().M((x - 4.5, y - 4.5)).L((x + 4.5, y + 4.5)), 5.0))
    # boucles aux quatre pointes
    for k in range(4):
        a = math.radians(90 * k - 90)
        loop = circle(c + (R - 6.5) * math.cos(a), c + (R - 6.5) * math.sin(a), 4.0)
        ops += strand(loop, width=3.2, ink="silver", outline=1.1)
    return boite(ops)


def lattice(x0, y0, x1, y1, step):
    """Segments diagonaux d'un treillis (deux familles, à 45°) découpés au rectangle donné."""
    segs = []
    for sgn in (1, -1):
        c = -(y1 - y0)
        while c < (x1 - x0) + (y1 - y0):
            # droite x = x0 + c + sgn * (y - y0), ou son miroir
            xa, xb = x0 + c, x0 + c + (y1 - y0)
            if sgn < 0:
                xa, xb = x1 - c, x1 - c - (y1 - y0)
            ya, yb = y0, y1
            if xa > xb:
                xa, xb, ya, yb = xb, xa, yb, ya
            if xb > x0 and xa < x1:
                k = (yb - ya) / (xb - xa)
                if xa < x0:
                    ya, xa = ya + (x0 - xa) * k, x0
                if xb > x1:
                    yb, xb = yb - (xb - x1) * k, x1
                if math.hypot(xb - xa, yb - ya) > 4:
                    segs.append(((xa, ya), (xb, yb)))
            c += step
    return segs


def module_treillis():
    """Coin du pied (100 × FOOT_H) : treillis d'argent, cellules bleues aux bouts, vertes au milieu."""
    W, H = 100.0, FOOT_H
    ops = [fill("blue", rect(12, 10, 76, H - 20)), fill("green", rect(12, 50, 76, H - 100))]
    p = Path()
    for a, b in lattice(12, 10, 88, H - 10, 26):
        p.M(a).L(b)
    ops += strand(p, width=5.0, ink="silver", outline=1.2)
    ops += strand(rect(12, 10, 76, H - 20), width=4.0, ink="silver", outline=1.2)
    return boite(ops, W, H)


# --------------------------------------------------------------------------- #
# Filets d'argent et ornements extérieurs (unités : 100 = épaisseur de la bande haute)
# --------------------------------------------------------------------------- #

def separateurs(h):
    """Filets d'argent qui séparent la case de coin (100 × h) des bandes, posés au-dessus de tout. Les filets du
    cadre (`rules`) sont dessinés sous les frises, pour que les médaillons les recouvrent ; ils ne passent pas
    entre coins et bandes, d'où ces deux traits. Le contour s'arrête à l'argent des filets qu'il rejoint : les
    croisements restent nets, comme avec des baguettes."""
    a, w = RULE / 2, RULE + 2 * RULE_O
    return [stroke("outline", Path().M((100, a)).L((100, h - a)), w),
            stroke("outline", Path().M((a, h)).L((100 - a, h)), w),
            stroke("silver", Path().M((100, 0)).L((100, h)), RULE),
            stroke("silver", Path().M((0, h)).L((100, h)), RULE)]


def pignons():
    """Double pignon d'argent au-dessus des coins hauts, fond lie-de-vin, liseré rouge ; séparateurs de la case."""
    pts = [(-5.5, 4), (-5.5, -4), (21, -26), (40, -10), (50, -19), (60, -10), (79, -26), (105.5, -4), (105.5, 4)]
    edge = polygon(pts[1:-1], closed=False)
    ops = separateurs(100.0)
    ops += [stroke("red", edge, 13.0), fill("ground", polygon(pts)), stroke("outline", edge, RULE + 2 * RULE_O),
            stroke("silver", edge, RULE)]
    for x in (21, 79):
        ops += strand(Path().M((x - 10, -1)).L((x, -11)).L((x + 10, -1)), width=3.2, ink="silver", outline=1.0)
    return boite(ops)


def spiral_path(cx, cy, r, turns=0.92):
    pts = []
    for i in range(13):
        t = i / 12
        a = math.radians(-90 + 360 * turns * t)
        rr = r * (1 - 0.75 * t)
        pts.append((cx + rr * math.cos(a), cy + rr * math.sin(a)))
    return smooth(pts)


def dragon():
    """Volute d'ardoise en forme de dragon, au dehors des coins du pied, liserée de rouge : une spirale au coin,
    un corps en S le long du bord avec deux boutons, une vrille rouge qui remonte. Tous les liserés d'abord, puis
    toutes les pièces d'ardoise : la silhouette reste d'un seul tenant."""
    o = 1.6
    body = smooth([(-10, 8), (-15, 28), (-24, 44), (-17, 62), (-11, 82)])
    discs = [circle(-19, 4, 16), circle(-32, 44, 8.5), circle(-23, 82, 7.5), circle(-11, 92, 6.5),
             circle(-4, -13, 6.5)]
    web = polygon([(-5.5, -8), (-5.5, 40), (-12, 40), (-30, 2)])
    ops = separateurs(FOOT_H)
    ops.append(stroke("red", smooth([(-11, 92), (-18, 104), (-11, 116), (-17, 130)]), 2.8, round=True))
    ops.append(stroke("red", body, 13 + 2 * o))
    for d in discs:
        ops.append(stroke("red", d, 2 * o))
    ops.append(stroke("slate", body, 13))
    for d in discs:
        ops.append(fill("slate", d))
    ops.append(fill("slate", web))
    ops.append(stroke("outline", spiral_path(-19, 4, 11), 1.8, round=True))
    ops.append(stroke("outline", spiral_path(-32, 44, 5), 1.3, round=True))
    ops.append(stroke("outline", smooth([(-13, 26), (-19, 44), (-13, 62)]), 1.4, round=True))
    return boite(ops)


def spec():
    return cadre(
        name="soir",
        title="Prières du soir",
        source="paroissien chromolithographié, fin du XIXᵉ siècle, p.25 « Prières du soir »",
        inks=INKS,
        palettes=PALETTES,
        layout=LAYOUT,
        ground="ground",
        top=module_entrelacs(),
        side=module_coeurs(),
        bottom=module_losanges(),
        centers={"top": medaillon_haut(), "side": medaillon_cote(), "bottom": medaillon_pied()},
        corners={"top": module_noeud(), "bottom": module_treillis()},
        rules=[filet("outline", 0, RULE + 2 * RULE_O, over=False), filet("silver", 0, RULE, over=False),
               filet("outline", 100, RULE + 2 * RULE_O, over=False), filet("silver", 100, RULE, over=False)],
        outer={"corner": pignons(), "cornerBottom": dragon()},
    )
