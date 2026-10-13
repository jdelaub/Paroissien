"""
Cadre « Enluminure aux fraisiers », dessiné pour l'atelier (13/10/2026) dans le genre des bordures des livres d'heures
du XVᵉ siècle que l'utilisateur a donnés en exemple : sur le vélin nu, une tige à l'encre qui ondule, des acanthes
bleues et ocre qui s'enroulent, des fraises rouges à graines d'or et à collerette verte, des églantines blanches, des
besants d'or d'où partent de fines vrilles ; une rosace rouge aux angles ; au pied, un oiseau posé. Dessin original :
aucun motif n'est recopié des images (photos de banque d'images, enluminure signée), seul le genre est repris.

Les acanthes prennent la couleur du temps liturgique ; les besants et le filet d'or, le reflet du métal choisi.
"""
import math

from commun import (Path, affine, boite, cadre, circle, ellipse, filet, fill, frise, leaf, outlined, piece, smooth,
                    spiral, stroke, teardrop)

ORDRE = 913

INKS = {
    "trait": "#3A3128",       # tige et vrilles à l'encre
    "outline": "#2E2620",     # cerne des formes
    "gold": "#C9A24A",        # besants, cœur des fleurs, filet
    "blue": "#3D5A99",        # acanthes bleues
    "blueLight": "#9DB3DE",   # rehauts des acanthes
    "ochre": "#E2BE62",       # acanthes ocre
    "ochreDark": "#B0782C",   # ombres des acanthes ocre
    "red": "#C2392B",         # fraises, rosaces
    "seed": "#F4D98A",        # graines des fraises
    "green": "#5E8A4A",       # feuilles, collerettes
    "greenDark": "#3E6334",   # nervures
    "white": "#FBF7EE",       # églantines
    "grey": "#A49D90",        # cerne des pétales blancs
    "bird": "#D9B25A",        # poitrail de l'oiseau
    "birdHead": "#9BB0C4",    # tête et dos
    "birdWing": "#857560",    # aile et queue
}

PALETTES = {
    "GREEN":  {"blue": "#4E7F4A", "blueLight": "#A9CC9B"},
    "VIOLET": {"blue": "#6B4C9A", "blueLight": "#C1ADDF"},
    "RED":    {"blue": "#A8322A", "blueLight": "#EBA79C"},
    "ROSE":   {"blue": "#C9708F", "blueLight": "#F0C3D2"},
    "WHITE":  {"blue": "#B99645", "blueLight": "#EEDDAA"},
    "BLACK":  {"blue": "#4A4744", "blueLight": "#A9A49D"},
}

P = 150.0       # pas de la frise : une églantine, une acanthe bleue, une fraise, une acanthe ocre
TRAIT = 1.5
O = 1.0         # cerne


def cerne(ink, path, o=O):
    return outlined(ink, path, outline=o)


# --------------------------------------------------------------------------- #
# Éléments
# --------------------------------------------------------------------------- #
def tige():
    pts = [(-37.5, 70), (0, 50), (37.5, 30), (75, 52), (112.5, 70), (150, 50), (187.5, 30)]
    return smooth(pts)


def acanthe(tx, ty, rot, s, ink, light, flip=False):
    """Feuille d'acanthe qui s'enroule : trois lobes sur le bord extérieur, la pointe recourbée en crosse."""
    f = (lambda p: (p[0], -p[1])) if flip else (lambda p: p)
    pts = [(0, 0), (8, -6), (16, -14), (20, -9), (26, -19), (32, -14), (40, -22), (44, -14), (48, -10),
           (50, -2), (46, 5), (40, 4), (43, -2), (38, -4), (30, 2), (20, 6), (10, 6), (2, 4)]
    shape = smooth([f(p) for p in pts], closed=True)
    place = affine(tx=tx, ty=ty, rot=rot, sx=s)
    ops = cerne(ink, shape.transformed(place))
    rib = smooth([f(p) for p in [(2, 2), (16, -2), (30, -6), (42, -6), (46, -2)]])
    ops.append(stroke(light, rib.transformed(place), 1.6 * s, round=True))
    for a, b in (((16, -6), (20, -11)), ((28, -9), (32, -15)), ((38, -9), (42, -17))):
        ops.append(stroke(light, Path().M(f(a)).L(f(b)).transformed(place), 1.0 * s, round=True))
    return ops


def fraise(cx, cy, s=1.0):
    """Fraise pendante : baie rouge à graines d'or, collerette verte de cinq sépales, courte queue."""
    place = affine(tx=cx, ty=cy, sx=s)
    berry = smooth([(0, 0), (9, 3), (11, 13), (6, 24), (0, 29), (-6, 24), (-11, 13), (-9, 3)], closed=True)
    ops = cerne("red", berry.transformed(place))
    for x, y in ((-4, 8), (3, 7), (-6, 15), (1, 14), (6, 16), (-2, 21), (3, 22)):
        ops.append(fill("seed", circle(x, y, 1.1).transformed(place)))
    for k in range(5):
        ops += cerne("green", teardrop(10, 4).transformed(affine(rot=200 + k * 35)).transformed(affine(ty=1)).transformed(place), 0.7)
    ops.append(stroke("trait", Path().M((0, -1)).L((0, -9)).transformed(place), TRAIT * s, round=True))
    return ops


def eglantine(cx, cy, s=1.0):
    """Églantine blanche à cinq pétales, cœur d'or."""
    place = affine(tx=cx, ty=cy, sx=s)
    ops = []
    for k in range(5):
        a = -90 + k * 72
        petal = circle(7.5 * math.cos(math.radians(a)), 7.5 * math.sin(math.radians(a)), 6.2)
        ops += cerne("white", petal.transformed(place), 0.8)
    ops.append(fill("gold", circle(0, 0, 3.6).transformed(place)))
    ops.append(stroke("outline", circle(0, 0, 3.6).transformed(place), 0.6))
    return ops


def besant(cx, cy, curl=1, r=3.6):
    """Besant d'or cerné, avec sa vrille à l'encre."""
    ops = [stroke("outline", circle(cx, cy, r), 1.0), fill("gold", circle(cx, cy, r))]
    v = spiral(cx + curl * (r + 4), cy - 2, 0.8, 4.2, 1.0, a0=(180 if curl > 0 else 0))
    ops.append(stroke("trait", Path().M((cx + curl * r, cy)).L((cx + curl * (r + 4), cy - 1)), 0.9, round=True))
    ops.append(stroke("trait", v, 0.9, round=True))
    return ops


def feuille_verte(cx, cy, rot, s=1.0):
    place = affine(tx=cx, ty=cy, rot=rot, sx=s)
    ops = cerne("green", leaf(16, 7, 0.1).transformed(place), 0.7)
    ops.append(stroke("greenDark", Path().M((1, 0)).L((13, 0.6)).transformed(place), 0.7 * s, round=True))
    return ops


# --------------------------------------------------------------------------- #
# Modules
# --------------------------------------------------------------------------- #
def frise_enluminee():
    base = [stroke("trait", tige(), TRAIT, round=True)]
    # vrilles fines qui partent de la tige
    for (u, v, a0) in ((18, 40, 0), (58, 42, 180), (95, 62, 0), (132, 60, 180)):
        base.append(stroke("trait", spiral(u, v, 0.8, 5.5, 1.2, a0=a0), 0.9, round=True))
    base += feuille_verte(20, 38, -30)
    base += feuille_verte(96, 64, 150)
    base += acanthe(37.5, 30, -20, 0.9, "blue", "blueLight")
    base += acanthe(104, 69, 18, 0.78, "ochre", "ochreDark", flip=True)
    base += fraise(75, 61, 0.9)
    base += eglantine(0, 50, 0.95)
    for u, v, c in ((26, 18, 1), (56, 78, -1), (90, 30, 1), (128, 86, -1), (140, 22, 1)):
        base += besant(u, v, c)
    return frise(P, base, outset=4, inset=2)


def coin():
    """Rosace rouge à cœur d'or au croisement des tiges, quatre acanthes bleues dans les diagonales, besants."""
    ops = []
    for k in range(4):
        a = 45 + 90 * k
        ops += acanthe(50 + 6 * math.cos(math.radians(a)), 50 + 6 * math.sin(math.radians(a)), a, 0.62, "blue", "blueLight")
    for k in range(6):
        a = k * 60
        ops += cerne("red", circle(50 + 9 * math.cos(math.radians(a)), 50 + 9 * math.sin(math.radians(a)), 6.5), 0.8)
    ops.append(fill("gold", circle(50, 50, 6)))
    ops.append(stroke("outline", circle(50, 50, 6), 0.7))
    for x, y, c in ((16, 16, 1), (84, 14, -1), (14, 84, 1)):
        ops += besant(x, y, c)
    return boite(ops, outset=3)


def oiseau(dx=0.0, dy=46.0):
    """Un oiseau posé sur la tige, tourné vers la gauche : poitrail ocre, tête et dos gris bleu, aile brune."""
    at = affine(tx=dx, ty=dy)
    ops = []

    def shape(ink, path, o=O):
        ops.extend(cerne(ink, path.transformed(at), o))

    shape("bird", smooth([(-22, 2), (-10, -10), (8, -12), (24, -6), (30, 2), (20, 10), (0, 12), (-16, 9)], closed=True))
    shape("birdHead", smooth([(-14, -9), (2, -14), (20, -9), (28, -2), (14, -4), (0, -6)], closed=True), 0.8)
    shape("birdWing", smooth([(-6, -6), (10, -9), (26, -3), (36, 6), (18, 4), (2, 2)], closed=True), 0.8)
    shape("birdWing", Path().M((28, 0)).L((46, 6)).L((44, 11)).L((26, 6)).Z(), 0.8)
    shape("birdHead", circle(-24, -6, 8.5), 0.8)
    ops.append(fill("outline", circle(-27, -8, 1.4).transformed(at)))
    shape("ochreDark", Path().M((-31, -7)).L((-39, -4)).L((-31, -3)).Z(), 0.6)
    for x in (-6, 6):
        ops.append(stroke("trait", Path().M((x, 11)).L((x - 2, 20)).L((x + 3, 20)).transformed(at), 1.1, round=True))
    for k in range(5):
        ops.append(stroke("birdWing", Path().M((4 + k * 5, -6 + k)).L((12 + k * 5, -3 + k)).transformed(at), 0.6))
    return ops


def pied():
    return piece(110, oiseau(), outset=6, droit=True)


def spec():
    f = frise_enluminee()
    return cadre(
        name="enluminure",
        title="Enluminure aux fraisiers",
        source="atelier : bordure de livre d'heures (acanthes, fraises, églantines, besants), dessin original",
        inks=INKS,
        palettes=PALETTES,
        dimmed=[k for k in INKS if k not in ("outline", "trait")],
        layout={"margin": 8, "top": 32, "side": 30, "bottom": 36, "textPadding": 6},
        ground=None,
        top=f,
        corners={"top": coin()},
        centers={"bottom": pied()},
        rules=[filet("blue", 95, 2.2), filet("gold", 98.5, 2.4)],
    )
