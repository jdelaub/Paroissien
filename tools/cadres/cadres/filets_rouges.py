"""
Cadre simple : un filet rouge autour de la page, interrompu à chaque angle par un anneau entre quatre points
(paroissien chromolithographié, p.81, « Élévation »). Le cadre des livres sobres : exercices, Bible réglée.

Bandes minces et vides : seul le filet (à distance fixe du bord) et les coins dessinent. Le coin passe « dessus » :
un disque couleur papier coupe le filet avant l'anneau, comme sur la page où le trait s'arrête sur un point.
"""
from commun import circle, fill, stroke, boite, frise, filet, cadre

ORDRE = 81

C = 50.0       # l'angle du filet dans la case du coin (unités)
RING = 22.0    # rayon de l'anneau
DOT = 6.0      # rayon des points


def coin():
    ops = [fill("paper", circle(C, C, 38))]                          # coupe le filet autour de l'angle
    ops.append(stroke("rule", circle(C, C, RING), 7))
    for dx, dy in ((1, 0), (0, 1), (-1, 0), (0, -1)):
        d = RING + 15
        ops.append(fill("rule", circle(C + dx * d, C + dy * d, DOT)))
    return boite(ops, dessus=True)


def spec():
    vide = frise(100, [])
    return cadre(
        name="filets_rouges",
        title="Filets rouges",
        source="paroissien chromolithographié, p.81 (Élévation), photo cadre-simple-filets-rouges",
        inks={"rule": "#C4473B"},
        palettes={},
        layout={"margin": 10, "top": 12, "side": 12, "bottom": 12, "textPadding": 12},
        ground=None,
        top=vide,
        corners={"top": coin()},
        rules=[filet("rule", C, 8.5, depuis="exterieur")],
    )
