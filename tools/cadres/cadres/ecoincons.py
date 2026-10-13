"""
Cadre « Écoinçons et filets », dessiné pour l'atelier (13/10/2026) d'après les plats dorés que l'utilisateur a
donnés en exemple : un double filet d'or autour de la page, et dans chaque angle un écoinçon de rinceaux, deux
tiges qui partent d'une fleur de lys couchée dans la diagonale et courent le long des côtés en volutes, feuilles et
besants ; au milieu de chaque côté, un petit fleuron pendant. Dessin original, dans le genre des fers d'angle.

L'écoinçon tient en deux modules : le coin (la fleur de lys et le départ des tiges) et une pièce de bout à chaque
bout de bande (la tige qui continue le long du côté) ; le moteur la retourne au bout opposé.
"""
from commun import (Path, affine, boite, bouts, cadre, circle, fill, filet, fleur_de_lis, frise, leaf, piece, smooth,
                    spiral, stroke, trefoil)

ORDRE = 912

INKS = {
    "gold": "#C49A4A",       # or des filets et des rinceaux
}

ARM_V = 34.0     # profondeur où la tige quitte le coin et longe la bande
STEM = 3.4       # épaisseur des tiges
END_W = 170.0    # longueur de la pièce de bout


def coin():
    ops = []
    # fleur de lys dans la diagonale, la pointe vers l'angle extérieur
    ops.append(fill("gold", fleur_de_lis(44).transformed(affine(tx=64, ty=64, rot=-45))))
    for swap in (False, True):
        def pt(x, y, swap=swap):
            return (y, x) if swap else (x, y)
        stem = smooth([pt(54, 44), pt(66, 33), pt(84, ARM_V - 2), pt(100, ARM_V)])
        ops.append(stroke("gold", stem, STEM, round=True))
        ops.append(fill("gold", leaf(16, 7, 0.15).transformed(lambda p, swap=swap: pt(*affine(tx=70, ty=36, rot=-160)(p)))))
    ops.append(fill("gold", circle(24, 24, 3.6)))
    return boite(ops)


def bout():
    """La tige qui continue le long de la bande depuis l'angle : ondulation, feuilles, bouton, volute, besants."""
    ops = []
    stem = smooth([(0, ARM_V), (35, ARM_V - 4), (70, ARM_V + 10), (110, ARM_V + 2), (140, ARM_V + 12)])
    ops.append(stroke("gold", stem, STEM, round=True))
    ops.append(stroke("gold", spiral(146, ARM_V + 22, 2, 10, 1.15, a0=-90), STEM * 0.85, round=True))
    ops.append(fill("gold", leaf(22, 9, 0.2).transformed(affine(tx=48, ty=ARM_V + 2, rot=55))))
    ops.append(fill("gold", leaf(18, 8, -0.2).transformed(affine(tx=92, ty=ARM_V + 6, rot=-40))))
    ops.append(fill("gold", trefoil(20).transformed(affine(tx=118, ty=ARM_V + 6, rot=180))))
    for u, v in ((24, ARM_V + 14), (78, ARM_V - 6), (128, ARM_V - 4), (60, ARM_V + 24)):
        ops.append(fill("gold", circle(u, v, 3.2)))
    return boite(ops, w=END_W)


def milieu():
    """Petit fleuron pendant au milieu de chaque côté, sous le filet intérieur."""
    ops = [fill("gold", trefoil(24).transformed(affine(tx=0, ty=20, rot=180)))]
    for side in (-1, 1):
        ops.append(stroke("gold", spiral(side * 10, 26, 1.5, 6.5, 1.0, a0=(0 if side > 0 else 180)), 2.4, round=True))
    ops.append(fill("gold", circle(0, 52, 3.4)))
    return piece(40, ops)


def spec():
    vide = frise(100, [])
    ends = bouts(bout())
    return cadre(
        name="ecoincons",
        title="Écoinçons et filets",
        source="atelier : fers d'angle de reliure (écoinçons de rinceaux), dessin original",
        inks=INKS,
        palettes={},
        layout={"margin": 10, "top": 30, "side": 30, "bottom": 30, "textPadding": 2},
        ground=None,
        top=vide,
        corners={"top": coin()},
        centers={"top": milieu(), "side": milieu(), "bottom": milieu()},
        ends={"top": ends, "side": ends, "bottom": ends},
        rules=[filet("gold", 7, 4.5), filet("gold", 16, 1.5)],
    )
