"""
Cadre « Prières du matin » (p.5) : fond d'or ; en haut et sur les côtés, un treillis de rubans blancs en losanges
garnis de cœurs bleus, avec des paires de feuilles vertes dans les triangles du bord et des perles rouges aux
nœuds ; au pied, plus haut, des médaillons à fleuron bleu reliés par des nœuds en sablier ; quadrilobes verts aux
coins hauts, palmette verte aux coins du pied ; filets bleu et blanc croisés aux coins, volutes bleues au dehors.
"""
import math

from commun import (Path, affine, arc, boite, cadre, circle, diamond, ellipse, fill, frise, leaf, mirror_x, outlined,
                    palmette, polygon, smooth, spiral, stroke, strand, teardrop)

ORDRE = 5

INKS = {
    "gold": "#CDAE68",       # or du fond
    "blue": "#5F82B8",       # bleu des cœurs et fleurons
    "blueDark": "#2E4A7C",   # bleu foncé : contours et filets
    "white": "#F5F0E3",      # rubans et rehauts
    "green": "#56692C",      # feuilles, vert sombre
    "greenLight": "#A7B55A", # feuilles, vert clair
    "red": "#D2563A",        # perles et pétales
    "outline": "#2E4A7C",    # contour des formes (bleu foncé, comme sur la photo)
}

PALETTES = {
    "GREEN":  {"blue": "#5C8C64", "blueDark": "#284A33", "outline": "#284A33"},
    "VIOLET": {"blue": "#8466AE", "blueDark": "#45305F", "outline": "#45305F"},
    "RED":    {"blue": "#C0503F", "blueDark": "#6E2620", "outline": "#6E2620", "red": "#E7A04E"},
    "ROSE":   {"blue": "#D18EA4", "blueDark": "#7E4558", "outline": "#7E4558"},
    "WHITE":  {"blue": "#AFC2D6", "blueDark": "#8C7A4E", "outline": "#8C7A4E", "green": "#8C9A5A"},
    "BLACK":  {"blue": "#6C6A66", "blueDark": "#2A2826", "outline": "#2A2826", "gold": "#A9A397", "red": "#8C8379",
               "green": "#55534E", "greenLight": "#8E8B84"},
}

STRAND_W = 6.0    # ruban blanc
STRAND_O = 1.6    # son contour bleu


def heart(L, W):
    """Cœur pointant vers +x, base (les deux lobes) côté -x, centre de la boîte en (0, 0)."""
    h = W / 2
    return (Path().M((L / 2, 0))
            .C((L * 0.18, h * 0.55), (-L * 0.30, h * 1.15), (-L * 0.48, h * 0.55))
            .C((-L * 0.62, h * 0.12), (-L * 0.46, -h * 0.02), (-L * 0.30, 0))
            .C((-L * 0.46, h * 0.02), (-L * 0.62, -h * 0.12), (-L * 0.48, -h * 0.55))
            .C((-L * 0.30, -h * 1.15), (L * 0.18, -h * 0.55), (L / 2, 0)).Z())


def blue_heart(cx, cy, L, W, rot=0.0):
    """Cœur bleu à contour foncé, rehaussé d'une virgule blanche."""
    t = affine(cx, cy, rot=rot)
    ops = outlined("blue", heart(L, W).transformed(t), outline=1.4)
    ops.append(stroke("white", smooth([(-L * 0.28, -W * 0.18), (-L * 0.05, -W * 0.05), (L * 0.18, W * 0.02)]).transformed(t), 2.2, round=True))
    ops.append(stroke("white", smooth([(-L * 0.30, W * 0.22), (-L * 0.12, W * 0.12)]).transformed(t), 1.6, round=True))
    return ops


def green_leaf(x, y, rot, L=24, W=11, flip=False):
    """Feuille verte : vert sombre, nervure claire. `flip` la courbe de l'autre côté."""
    b = -0.12 if flip else 0.12
    t = affine(x, y, rot=rot)
    ops = outlined("green", leaf(L, W, bend=b).transformed(t), outline=1.0)
    ops += [fill("greenLight", leaf(L * 0.72, W * 0.42, bend=b).transformed(affine(x, y, rot=rot)).transformed(lambda p: p))]
    return ops


def pearl(x, y, r=3.4):
    return outlined("red", circle(x, y, r), outline=0.9)


# --------------------------------------------------------------------------- #
# Frise haute et latérale : treillis en losanges
# --------------------------------------------------------------------------- #
def module_treillis():
    P = 128.0
    top, bot, mid = 13.0, 87.0, 50.0
    base, over = [], []

    # feuilles vertes dans les triangles du bord, au-dessus et au-dessous du nœud (u = 0)
    for sgn in (-1, 1):
        y = mid + sgn * 11
        for side in (-1, 1):
            # depuis la pointe du triangle, en éventail vers le bord
            ang = (180 + 38 if side < 0 else -38) if sgn < 0 else (180 - 38 if side < 0 else 38)
            base += green_leaf(side * 2, y, rot=ang, L=23, W=11, flip=side > 0)
        base += pearl(0, mid + sgn * 30, 2.6)

    # cœur bleu au milieu du losange, pointe vers +u
    base += blue_heart(P / 2, mid, 40, 30)

    # rubans : deux « V » par losange, prolongés au-delà des nœuds pour se raccorder aux voisins
    e = 10.0
    def v_shape(y_tip):
        dx, dy = P / 2, y_tip - mid
        n = math.hypot(dx, dy)
        ex, ey = dx / n * e, dy / n * e
        return Path().M((-ex, mid - ey)).L((P / 2, y_tip)).L((P + ex, mid - ey))
    for y_tip in (top, bot):
        base += strand(v_shape(y_tip), width=STRAND_W, ink="white", outline=STRAND_O, outline_ink="blueDark")

    # perles rouges : aux nœuds et aux pointes des losanges
    over += pearl(0, mid, 4.2)
    over += pearl(P / 2, top, 3.2)
    over += pearl(P / 2, bot, 3.2)
    return frise(P, base, over, phase=0.0)


# --------------------------------------------------------------------------- #
# Frise du pied : médaillons à fleuron, nœuds en sablier
# --------------------------------------------------------------------------- #
def fleuron_bleu(cx, cy, s=1.0):
    """Fleuron bleu à trois lobes (deux volutes en bas, une palmette en haut), pétales rouges."""
    ops = []
    for side in (-1, 1):
        lobe = spiral(cx + side * 11 * s, cy + 9 * s, 2.5 * s, 11 * s, 0.9, a0=(200 if side < 0 else -20))
        ops += outlined("blue", circle(cx + side * 11 * s, cy + 9 * s, 10.5 * s), outline=1.3)
        ops.append(stroke("white", lobe, 1.6 * s, round=True))
    ops += outlined("blue", palmette(24 * s).transformed(affine(cx, cy - 2 * s, rot=-90)), outline=1.3)
    for side in (-1, 1):
        ops += outlined("red", teardrop(12 * s, 6 * s).transformed(affine(cx + side * 4 * s, cy + 2 * s, rot=-90 + side * 55)), outline=0.9)
    ops += pearl(cx, cy + 15 * s, 3.0 * s)
    return ops


def module_medaillons():
    P = 176.0
    mid = 50.0
    R = 36.0
    base, over = [], []
    # entre deux médaillons (u = P/2) : ovale bleu, feuilles vertes au-dessus et au-dessous
    c = P / 2
    for sgn in (-1, 1):
        for side in (-1, 1):
            base += green_leaf(c + side * 5, mid + sgn * 25, rot=(180 if side < 0 else 0) + sgn * side * -22, L=30, W=13, flip=side > 0)
    base += outlined("blue", ellipse(c, mid, 9, 15), outline=1.3)
    base += [stroke("white", smooth([(c, mid - 9), (c - 3, mid), (c, mid + 9)]), 1.8, round=True)]
    # sablier : deux triangles de ruban, pointes vers le milieu
    for side in (-1, 1):
        x0 = c + side * (P / 2 - R - 3)
        tri = polygon([(x0, mid - 24), (c + side * 15, mid), (x0, mid + 24)])
        base += strand(tri, width=4.6, ink="white", outline=1.4, outline_ink="blueDark")
    # le médaillon : ruban blanc, fleuron bleu
    base += fleuron_bleu(0, mid - 6, 1.05)
    base += strand(circle(0, mid, R), width=4.6, ink="white", outline=1.4, outline_ink="blueDark")
    over += pearl(0, mid - R, 3.4)
    over += pearl(0, mid + R, 3.4)
    for side in (-1, 1):
        over += pearl(c + side * 15, mid, 3.4)
        over += pearl(side * R * 0.75, mid - R * 0.66, 2.8)
        over += pearl(side * R * 0.75, mid + R * 0.66, 2.8)
    return frise(P, base, over, phase=0.0)


# --------------------------------------------------------------------------- #
# Coins
# --------------------------------------------------------------------------- #
def module_quadrilobe():
    c = 50.0
    ops = []
    lobes = Path()
    for k in range(4):
        a = math.radians(90 * k)
        lobes.extend(circle(c + 17 * math.cos(a), c + 17 * math.sin(a), 15))
    ops += outlined("green", lobes, outline=1.6)
    inner = Path()
    for k in range(4):
        a = math.radians(90 * k)
        inner.extend(circle(c + 17 * math.cos(a), c + 17 * math.sin(a), 8))
    ops.append(fill("greenLight", inner))
    ops += outlined("red", diamond(c, c, 22), outline=1.2)
    ops.append(fill("white", circle(c, c, 3.2)))
    for k in range(4):
        a = 45 + 90 * k
        ops += outlined("red", teardrop(10, 5).transformed(affine(c + 31 * math.cos(math.radians(a)), c + 31 * math.sin(math.radians(a)), rot=a)), outline=0.8)
    return boite(ops)


def module_palmette_pied():
    """Coin du pied, plus haut que large (100 × 160) : palmette verte dressée vers l'extérieur."""
    W, H = 100.0, 160.0
    cx = 50.0
    ops = []
    for side in (-1, 1):
        for i, y in enumerate((60, 100)):
            ops += green_leaf(cx + side * 4, y, rot=-90 + side * (40 + 12 * i), L=40, W=16, flip=side > 0)
    ops += outlined("blue", palmette(34).transformed(affine(cx, 52, rot=-90)), outline=1.3)
    ops.append(stroke("white", smooth([(cx, 44), (cx - 2, 30), (cx, 22)]), 2.0, round=True))
    for y in (78, 118):
        ops += pearl(cx, y, 4.0)
    for side in (-1, 1):
        ops += pearl(cx + side * 30, 136, 3.0)
    return boite(ops, W, H)


# --------------------------------------------------------------------------- #
# Ornements extérieurs (unités : 100 = épaisseur de la bande haute)
# --------------------------------------------------------------------------- #
def ornement_haut():
    """Petite volute bleue et blanche dans le prolongement de la diagonale, vers l'extérieur."""
    ops = []
    stem = smooth([(-2, -2), (-12, -12), (-20, -16), (-26, -13)])
    ops += strand(stem, width=3.6, ink="white", outline=1.3, outline_ink="blueDark")
    ops.append(stroke("blueDark", spiral(-24, -9, 1.0, 5.5, 1.1, a0=-60), 2.4, round=True))
    ops += outlined("blue", teardrop(12, 7).transformed(affine(-10, -12, rot=-120)), outline=1.0)
    ops += outlined("blue", teardrop(12, 7).transformed(affine(-12, -10, rot=-150)), outline=1.0)
    return boite(ops)


def ornement_pied():
    """Feuille d'acanthe bleue qui s'enroule vers l'extérieur, au bas du cadre."""
    ops = []
    blade = (Path().M((0, -2))
             .C((-12, -10), (-24, -26), (-26, -44))
             .C((-20, -34), (-12, -26), (-4, -24))
             .C((-14, -20), (-20, -14), (-28, -12))
             .C((-36, -10), (-44, -4), (-48, 0)).Z())
    ops += outlined("blue", blade, outline=1.3)
    ops.append(stroke("white", smooth([(-6, -6), (-16, -14), (-22, -30)]), 1.6, round=True))
    ops.append(stroke("blueDark", spiral(-47, -6, 1.2, 6, 1.0, a0=90), 2.2, round=True))
    return boite(ops)


def spec():
    return cadre(
        name="matin",
        title="Prières du matin",
        source="paroissien chromolithographié, fin du XIXᵉ siècle, p.5 « Prières du matin »",
        inks=INKS,
        palettes=PALETTES,
        layout={"margin": 14, "top": 18, "side": 18, "bottom": 30, "textPadding": 13},
        ground="gold",
        top=module_treillis(),
        bottom=module_medaillons(),
        corners={"top": module_quadrilobe(), "bottom": module_palmette_pied()},
        bars={"layers": [["blueDark", 5.0], ["white", 1.8]], "overhang": 0, "terminal": None, "cornerTerminals": False},
        outer={"corner": ornement_haut(), "cornerBottom": ornement_pied()},
    )
