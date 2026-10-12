"""
Cadre sans texte « quadrilobes et crêtes » : sur le papier nu, une baguette rouge à filets d'or et bande noire
semée de perles bleues ; au dehors, une crête de crosses rouges à bouton d'or alternant avec des crêtes dentelées,
les crosses tournées vers un fleuron bleu au milieu de chaque côté ; aux coins, une rosace rouge à quatre anneaux
bleus barrés d'or, centrée sur le croisement des baguettes, avec une croix d'or au cœur.

Mise en page : la bande compte la crête (v 0 à 25), la baguette (v 25 à 54,6) et, côté texte, du papier nu où
déborde la rosace (le moteur ne laisse déborder un coin que vers l'extérieur).
"""
import math

from commun import (Path, affine, boite, cadre, circle, filet, fill, frise, outlined, piece, polygon, rect, smooth,
                    stroke)

ORDRE = 902

INKS = {
    "red": "#CF6B5C",      # rouge corail : baguette, crête, corps de la rosace
    "gold": "#CDB06E",     # or : filets de la baguette, boutons, croix
    "blue": "#687C96",     # bleu ardoise : perles, anneaux, fleurons
    "navy": "#2C2F3A",     # noir bleuté : bande de la baguette, cœur de la rosace
    "outline": "#33292A",  # contour brun noir
}

PALETTES = {
    "GREEN":  {"red": "#5F8A58", "blue": "#5E7C8C"},
    "VIOLET": {"red": "#8A63A6", "blue": "#7183A8"},
    "RED":    {"red": "#C4473A", "gold": "#D5B25E"},
    "ROSE":   {"red": "#DA8FA3", "blue": "#7C8FA8"},
    "WHITE":  {"red": "#E4D5B3", "gold": "#C2A055", "blue": "#93A8C2"},
    "BLACK":  {"red": "#77716B", "gold": "#AAA498", "blue": "#8F8C86", "navy": "#26241F"},
}

# Profondeurs (unités de bande, 0 = bord extérieur de la crête)
BAR0 = 25.0                     # bord extérieur de la baguette
LAYERS = [("outline", 1.6), ("red", 5.0), ("outline", 1.2), ("gold", 2.0), ("navy", 10.0),
          ("gold", 2.0), ("outline", 1.2), ("red", 5.0), ("outline", 1.6)]
BAR_W = sum(w for _, w in LAYERS)   # 29,6
AXIS = BAR0 + BAR_W / 2             # axe de la baguette (39,8), centre de la rosace
RED_IN = (BAR0 + 1.6, 5.0)          # bande rouge extérieure : où naissent les tiges

P = 58.0          # pas de la crête : une crosse et une crête dentelée
DOTS = 9          # perles par pas
DOT_R = 2.25
O = 1.3           # contour des formes


def tapered(points, w0, w1):
    """Tige effilée le long d'une ligne lisse : largeur w0 au départ, w1 à l'arrivée (0 = pointe)."""
    n = len(points)
    left, right = [], []
    for i, p in enumerate(points):
        a, b = points[max(0, i - 1)], points[min(n - 1, i + 1)]
        tx, ty = b[0] - a[0], b[1] - a[1]
        d = math.hypot(tx, ty) or 1.0
        nx, ny = -ty / d, tx / d
        w = (w0 + (w1 - w0) * i / (n - 1)) / 2
        left.append((p[0] + nx * w, p[1] + ny * w))
        right.append((p[0] - nx * w, p[1] - ny * w))
    path = smooth(left)
    back = smooth(right[::-1])
    path.L(back.cmds[0][1])
    path.cmds.extend(back.cmds[1:])
    return path.Z()


def cerne(ink, path, o=O):
    """Forme remplie, cernée du contour brun noir."""
    return outlined(ink, path, outline=o)


def cerne_rond(ink, path, o=O):
    """Comme cerne, mais à joints ronds : sur une pointe effilée, un joint en onglet déborderait loin."""
    return [stroke("outline", path, 2 * o, round=True), fill(ink, path)]


def bar_rules():
    """La baguette : filets concentriques, du plus large au plus étroit, sous tout le reste."""
    return [filet(LAYERS[i][0], AXIS, round(BAR_W - 2 * sum(w for _, w in LAYERS[:i]), 2), over=False)
            for i in range(len(LAYERS) // 2 + 1)]


def dots(us):
    p = Path()
    for u in us:
        p.extend(circle(u, AXIS, DOT_R))
    return [fill("blue", p)]


# --------------------------------------------------------------------------- #
# Crête : crosse à bouton d'or (u = 0, tournée vers +u) et crête dentelée
# --------------------------------------------------------------------------- #
def crosse():
    """Crosse : tige rouge un peu cambrée, née de la baguette, coiffée d'un gros bouton d'or en virgule
    qui s'enroule vers +u ; une épine effilée part du sommet vers -u."""
    ops = []
    stem = tapered([(-0.4, 29), (-1.8, 21), (-3.2, 14), (-2.9, 8), (-1.0, 4.2), (2.2, 2.2)], 5.2, 2.6)
    # pied évasé : congé à droite (large) et à gauche (petit), fondu dans la bande rouge
    foot = (Path().M((-6.5, 29)).L((-6.5, BAR0)).C((-4.6, BAR0), (-4.0, 23.5), (-4.0, 21.5))
            .L((0.6, 20)).C((1.2, 23), (3.2, BAR0), (7.5, BAR0)).L((7.5, 29)).Z())
    spur = tapered([(-1.0, 4.8), (-3.6, 1.8), (-6.6, -0.2), (-10.0, -1.6)], 3.2, 0.0)
    body = Path().extend(stem).extend(foot)
    ops.append(stroke("outline", body, 2 * O))
    ops.append(stroke("outline", spur, 2 * O, round=True))
    ops.append(fill("red", body))
    ops.append(fill("red", spur))
    bud = (Path().M((0.6, 3.4))
           .C((4.2, 0.4), (11.4, 1.8), (12.8, 8.6))
           .C((13.8, 13.6), (10.6, 17.6), (7.4, 17.4))
           .C((4.4, 17.2), (2.6, 14.8), (2.8, 12.0))
           .C((3.0, 9.0), (2.6, 5.8), (0.6, 3.4)).Z())
    ops += cerne("gold", bud)
    return ops


def crete(cx):
    """Crête dentelée rouge, posée au-dessus de la baguette (un filet de papier entre les deux)."""
    pts = [(-15.5, 22), (-12.2, 19.2), (-10.8, 14.2), (-8.4, 15.4), (-5.6, 11.8), (-3.0, 13.0), (0, 10.6),
           (3.0, 13.0), (5.6, 11.8), (8.4, 15.4), (10.8, 14.2), (12.2, 19.2), (15.5, 22)]
    return cerne("red", polygon([(cx + u, v) for u, v in pts]))


def module_crete():
    phase = 0.17   # la première crosse juste après la rosace, son épine entière
    base = crosse() + crete(0.55 * P)
    # perles calées sur le pas absolu, à mi-intervalle : aucune n'est coupée au bout d'une demi-bande
    base += dots([((i + 0.5) / DOTS - phase) * P for i in range(DOTS)])
    # la bande rouge de la baguette repasse sur le pied des tiges : elles en sortent sans couture
    over = [fill("red", rect(-2, RED_IN[0], P + 4, RED_IN[1]))]
    return frise(P, base, over, phase=phase, outset=2.5)


# --------------------------------------------------------------------------- #
# Fleuron du milieu des côtés (pièce centrale)
# --------------------------------------------------------------------------- #
CW = 6 * P / DOTS   # largeur de la pièce : six perles, raccord juste avec les demi-bandes


def fleuron_bleu():
    """Fleur bleue à trois lobes, le lobe du milieu en ogive, pointant vers -v."""
    p = Path()
    for s in (-1, 1):
        p.extend(circle(s * 8.2, -3.2, 5.6))
    p.extend(Path().M((-6.2, 0.5)).C((-6.4, -6.5), (-3.2, -11.5), (0, -15.2))
             .C((3.2, -11.5), (6.4, -6.5), (6.2, 0.5)).Z())
    p.extend(Path().M((-11, 0)).C((-6, 3.8), (6, 3.8), (11, 0)).L((8, -3)).L((-8, -3)).Z())
    return p


def module_fleuron():
    ops = []
    # cœur d'or en pointe vers la baguette
    ops += cerne("gold", Path().M((0, 19.5)).C((-3.5, 13), (-6, 6), (-5.5, 1)).L((5.5, 1))
                        .C((6, 6), (3.5, 13), (0, 19.5)).Z())
    ops += cerne("blue", fleuron_bleu())
    ops += cerne("gold", Path().extend(circle(-12.0, 8, 4.4)).extend(circle(12.0, 8, 4.4)))
    # deux tiges rouges en Y, nées d'un même pied, effilées vers l'extérieur
    horns = Path()
    for s in (-1, 1):
        horns.extend(tapered([(s * 2.6, 29), (s * 2.6, 20), (s * 3.6, 12), (s * 7.0, 3), (s * 12.0, -4.5),
                              (s * 17.0, -10.5)], 4.4, 0.0))
    horns.extend(Path().M((-8.5, 29)).L((-8.5, BAR0)).C((-5, BAR0), (-4.6, 22.5), (-4.6, 19)).L((4.6, 19))
                 .C((4.6, 22.5), (5, BAR0), (8.5, BAR0)).L((8.5, 29)).Z())
    ops += cerne_rond("red", horns)
    ops.append(fill("red", rect(-CW / 2, RED_IN[0], CW, RED_IN[1])))
    ops += dots([(i + 0.5) * P / DOTS for i in range(-3, 3)])
    return piece(CW, ops, outset=17)


# --------------------------------------------------------------------------- #
# Rosace de coin : quadrilobe à anneaux, centrée sur le croisement des baguettes
# --------------------------------------------------------------------------- #
D_RING = 29.0     # distance du centre aux anneaux
R_RING = 16.8     # rayon de l'anneau (axe du trait)
W_RING = 4.6
R_LOBE = 25.5     # lobe rouge autour de chaque anneau


def module_rosace():
    c = AXIS
    ops = []
    rot = lambda a: affine(c, c, rot=a)

    body = Path()
    for k in range(4):
        a = math.radians(90 * k)
        body.extend(circle(c + D_RING * math.cos(a), c + D_RING * math.sin(a), R_LOBE))
    # oreilles dentelées aux diagonales
    ear = polygon([(18, -11), (33, -10.5), (37.5, -14), (39, -7.5), (49, 0), (39, 7.5), (37.5, 14), (33, 10.5),
                   (18, 11)])
    for k in range(4):
        body.extend(ear.transformed(rot(45 + 90 * k)))
    # pointes sur les axes, vers l'extérieur seulement (les autres sont sous les baguettes)
    spike = polygon([(46, -7.5), (52, -6.5), (53.5, -9.5), (55.5, -4), (61, 0), (55.5, 4), (53.5, 9.5),
                     (52, 6.5), (46, 7.5)])
    for a in (180, 270):
        body.extend(spike.transformed(rot(a)))
    ops += outlined("red", body, outline=1.6)

    # feuilles d'or aux diagonales, cœur noir
    leaf = polygon([(20, 0), (25, -6.5), (31, -7.5), (34.5, -10.5), (35, -4.5), (41.5, 0), (35, 4.5), (34.5, 10.5),
                    (31, 7.5), (25, 6.5)])
    leaves = Path()
    for k in range(4):
        leaves.extend(leaf.transformed(rot(45 + 90 * k)))
    ops += cerne("gold", leaves)
    ops.append(fill("navy", circle(c, c, 20)))

    # croix d'or : ses bras traversent les anneaux jusqu'à leur bord extérieur
    reach = D_RING + R_RING
    cross = Path().extend(rect(c - reach, c - 2.2, 2 * reach, 4.4)).extend(rect(c - 2.2, c - reach, 4.4, 2 * reach))
    ops += outlined("gold", cross, outline=1.0)
    small = Path()
    for k in range(4):
        a = math.radians(45 + 90 * k)
        small.extend(circle(c + 11.5 * math.cos(a), c + 11.5 * math.sin(a), 1.9))
    ops.append(fill("blue", small))

    # anneaux bleus, barrés de deux traits de part et d'autre de la croix
    rings, chords = Path(), Path()
    off = 5.2
    half = math.sqrt(R_RING ** 2 - off ** 2)
    for k in range(4):
        a = 90 * k
        t = affine(c, c, rot=a)
        rings.extend(circle(D_RING, 0, R_RING).transformed(t))
        for s in (-1, 1):
            chords.extend(Path().M((D_RING - half, s * off)).L((D_RING + half, s * off)).transformed(t))
    ops.append(stroke("outline", rings, W_RING + 2 * O))
    ops.append(stroke("outline", chords, 3.4 + 2 * O))
    ops.append(stroke("blue", rings, W_RING))
    ops.append(stroke("blue", chords, 3.4))
    return boite(ops, outset=61 - c + 2)   # les pointes extérieures (r = 61) dépassent la crête


def spec():
    crest = module_crete()
    center = module_fleuron()
    return cadre(
        name="gothique",
        title="Quadrilobes et crêtes",
        source="paroissien chromolithographié, fin du XIXᵉ siècle, cadre sans texte « quadrilobes et crêtes »",
        inks=INKS,
        palettes=PALETTES,
        layout={"margin": 11, "top": 33, "side": 33, "bottom": 33, "textPadding": 2},
        ground=None,
        top=crest,
        centers={"top": center, "side": center, "bottom": center},
        corners={"top": module_rosace()},
        rules=bar_rules(),
    )
