"""
Cadre « Roulette de fleurs de lys », dessiné pour l'atelier (13/10/2026) d'après les reliures que l'utilisateur a
données en exemple : une roulette dorée de fleurs de lys entre deux filets, comme on la pousse au bord d'un plat de
maroquin, et un carré plein à chaque angle, un losange réservé au milieu. Dessin original : aucune image n'est
recopiée, seul le genre (roulette de reliure) est repris.

Les lys pointent vers le dehors de la page ; un point d'or les sépare. La bande est mince : c'est un cadre sobre,
pour les livres qu'on veut seulement border d'or. Son or prend le reflet du métal choisi dans l'atelier.
"""
from commun import boite, cadre, circle, diamond, filet, fill, fleur_de_lis, frise, polygon, rect

ORDRE = 910

INKS = {
    "gold": "#C2A055",       # or de la roulette, des filets et des carrés
}

P = 64.0          # pas de la roulette : un lys et un point
LYS_H = 72.0      # hauteur d'un lys (unités de bande)
BASE_V = 86.0     # pied du lys, côté texte ; la pointe va vers le dehors


def roulette():
    ops = [fill("gold", fleur_de_lis(LYS_H).transformed(lambda p: (p[0], p[1] + BASE_V)))]
    ops.append(fill("gold", circle(P / 2, 50, 4.2)))
    return frise(P, ops)


def coin():
    """Carré plein sur l'angle des filets, un losange réservé et un point d'or au milieu."""
    square = rect(10, 10, 80, 80)
    ops = [fill("gold", square), fill("paper", diamond(50, 50, 44)), fill("gold", circle(50, 50, 7))]
    return boite(ops, dessus=True)


def spec():
    return cadre(
        name="fleurs_de_lys",
        title="Roulette de fleurs de lys",
        source="atelier : roulette de reliure (fleurs de lys, carrés d'angle), dessin original",
        inks=INKS,
        palettes={},
        layout={"margin": 10, "top": 18, "side": 18, "bottom": 18, "textPadding": 10},
        ground=None,
        top=roulette(),
        corners={"top": coin()},
        rules=[filet("gold", 6, 4.5), filet("gold", 96, 3.0)],
    )
