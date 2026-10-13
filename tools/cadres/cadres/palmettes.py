"""
Cadre « Palmettes et lotus », dessiné pour l'atelier (13/10/2026) d'après les exemples de reliures de l'utilisateur :
la frise néo-grecque des fers de reliure, palmettes tournées vers le dehors et fleurons de lotus tournés vers le
texte, liés par des tiges en S terminées en volutes ; deux filets de chaque côté ; une rosace dans un carré à chaque
angle. Dessin original, d'après le genre (anthémion) et non d'après une image.

Or plein cerné d'un or plus sombre, sur le papier nu ; l'or prend le reflet du métal choisi dans l'atelier.
"""
import math

from commun import affine, boite, cadre, circle, ellipse, filet, fill, frise, outlined, rect, smooth, spiral, stroke, teardrop

ORDRE = 911

INKS = {
    "gold": "#C9A85A",       # or des palmettes et des filets
    "goldDark": "#8A6A2C",   # cerne des formes, tiges
}

P = 100.0
BASE = 76.0                  # cœur de la palmette, posé sur les filets intérieurs
TOP = 17.0                   # attache du lotus, sous les filets extérieurs
O = 1.2                      # cerne


def cerne(path):
    return outlined("gold", path, outline=O, outline_ink="goldDark")


def palmette_ops(u):
    """Palmette à sept lobes en éventail, tournée vers le dehors, sur son cœur et deux volutes."""
    ops = []
    for side in (-1, 1):
        vol = spiral(u + side * 15, BASE + 4, 1.5, 6.5, 1.1, a0=(0 if side > 0 else 180))
        ops.append(stroke("goldDark", vol, 3.6, round=True))
        ops.append(stroke("gold", vol, 1.8, round=True))
    for k, ang in enumerate(range(-66, 67, 22)):
        a = math.radians(ang)
        length = [26, 34, 40, 44, 40, 34, 26][k]
        rot = math.degrees(math.atan2(-math.cos(a), math.sin(a)))
        start = (u + math.sin(a) * 4, BASE - math.cos(a) * 4)
        ops += cerne(teardrop(length, 0.34 * length).transformed(affine(tx=start[0], ty=start[1], rot=rot)))
    ops += cerne(ellipse(u, BASE + 1, 9, 6))
    return ops


def lotus_ops(u):
    """Fleuron de lotus pendant vers le texte : un pétale au milieu, deux qui s'écartent, une bague à l'attache."""
    ops = []
    for side in (-1, 1):
        ops += cerne(teardrop(28, 11).transformed(affine(tx=u + side * 2, ty=TOP + 3, rot=90 - side * 42)))
    ops += cerne(teardrop(40, 14).transformed(affine(tx=u, ty=TOP + 2, rot=90)))
    ops += cerne(ellipse(u, TOP + 2, 7, 4))
    return ops


def frise_palmettes():
    return frise(P, palmette_ops(0) + lotus_ops(P / 2))


def coin():
    ops = [fill("paper", rect(4, 4, 92, 92)), stroke("goldDark", rect(12, 12, 76, 76), 3.6), stroke("gold", rect(12, 12, 76, 76), 2.0)]
    for k in range(8):
        ops += cerne(teardrop(28, 11).transformed(affine(tx=50, ty=50, rot=45 * k + 22.5)))
    ops += cerne(circle(50, 50, 8))
    ops.append(fill("goldDark", circle(50, 50, 3)))
    return boite(ops, dessus=True)


def spec():
    return cadre(
        name="palmettes",
        title="Palmettes et lotus",
        source="atelier : frise d'anthémion des fers de reliure, dessin original",
        inks=INKS,
        palettes={},
        layout={"margin": 10, "top": 19, "side": 19, "bottom": 19, "textPadding": 10},
        ground=None,
        top=frise_palmettes(),
        corners={"top": coin()},
        rules=[filet("gold", 4, 4.0, over=False), filet("goldDark", 10, 1.4, over=False),
               filet("goldDark", 90, 1.4, over=False), filet("gold", 96, 4.0, over=False)],
    )
