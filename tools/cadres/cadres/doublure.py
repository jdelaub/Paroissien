"""
Cadre « Doublure de soie » : le contre-plat d'une reliure de paroissien de la fin du XIXᵉ siècle. Une soie bleue moirée
est bordée d'une roulette dorée poussée sur le cuir sombre de la chasse. De l'extérieur vers la soie : une frise
d'anthémions (palmette dans une lyre de deux volutes, alternant avec un bouton pointu coiffé d'une perle), deux filets
d'or, une bande d'or percée de denticules et bordée de petites dents. Aux coins, un éventail en diagonale coiffé
d'une perle.

Seule la bordure est modelée : l'application dessine la soie moirée à part, dans le rectangle intérieur.
Relevé sur la photo (1638 × 4000 px, bande haute, 154 px d'épaisseur = 100 unités) :
  - v 0 à 59 : frise d'anthémions, période 112 px ≈ 72 unités (une palmette, un bouton) ;
  - v 59 à 64 et 67 à 71 : deux filets d'or, séparés par un trait de cuir ; v 71 à 75 : gorge de cuir ;
  - v 75 à 97 : bande d'or percée de denticules de cuir (7,5 × 11 unités, quatre par période) ;
  - v 97 à 100 : petites dents d'or contre la soie.
Soie mesurée (blanc corrigé sur le clavier de la photo) : fond #45447F, ombre de la moire #3A396E, reflet #5A59A0.
"""
from commun import Path, affine, boite, cadre, circle, filet, fill, frise, mirror_x, rect, stroke, teardrop

ORDRE = 0

INKS = {
    "leather": "#2C2925",    # cuir de la chasse, brun presque noir
    "gold": "#C8A257",       # or de la roulette
    "goldLight": "#EED492",  # rehaut, là où le fer a bruni la dorure
}

# Pas de palette liturgique : c'est la reliure du livre, la même quel que soit le jour.
PALETTES = {}

P = 72.0          # période de la frise : une palmette (u = 0) et un bouton (u = P/2)
DENT = P / 4      # pas des denticules
LYRE_W = 5.4      # épaisseur des volutes de la lyre

# Bandes d'or (v de début, v de fin), tracées en filets tout autour du cadre pour des angles nets,
# et rehauts clairs (v, épaisseur) au milieu des deux filets et du listel au-dessus des denticules.
GOLD_BANDS = [(59, 64), (67, 71), (75, 97)]
HIGHLIGHTS = [(61.5, 1.6), (69, 1.2), (79, 2.0)]


def lobe(L, W):
    """Feuille de palmette, élargie vers la pointe, pointant vers +x depuis (0, 0)."""
    return (Path().M((0, 0))
            .C((0.25 * L, -0.12 * W), (0.45 * L, -0.55 * W), (0.78 * L, -0.5 * W))
            .C((0.95 * L, -0.45 * W), (L, -0.18 * W), (L, 0))
            .C((L, 0.18 * W), (0.95 * L, 0.45 * W), (0.78 * L, 0.5 * W))
            .C((0.45 * L, 0.55 * W), (0.25 * L, 0.12 * W), (0, 0)).Z())


def leaf_bud(L, W, bend):
    """Feuille de base du bouton, couchée vers +x, la pointe infléchie vers +y."""
    b = bend * L
    return (Path().M((0, 0))
            .C((0.3 * L, -0.6 * W), (0.7 * L, -0.4 * W + b), (L, b))
            .C((0.7 * L, 0.3 * W + b), (0.3 * L, 0.5 * W), (0, 0)).Z())


def swap(p):
    """Échange u et v : passe du repère de la bande haute à celui de la bande gauche, dans le coin."""
    return (p[1], p[0])


# --------------------------------------------------------------------------- #
# Éléments de la frise (u le long de la bande, v de 0 dehors à 100 côté soie).
# Chaque élément prend une transformation `t` qui le place.
# --------------------------------------------------------------------------- #
def palmette(t):
    """Palmette dans sa lyre, axe en u = 0 : éventail de cinq lobes, deux volutes en U recourbées vers l'extérieur."""
    ops = []
    for a, L, W in ((40, 28, 13), (20, 35, 13.5), (0, 39, 14)):
        for sg in ((1,) if a == 0 else (-1, 1)):
            ops.append(fill("gold", lobe(L, W).transformed(affine(0, 44, rot=-90 + sg * a)).transformed(t)))
    ops.append(fill("goldLight", lobe(27, 4.8).transformed(affine(0, 42, rot=-90)).transformed(t)))
    ops.append(fill("gold", circle(0, 45, 4.6).transformed(t)))
    arm = (Path().M((1, 50.5))
           .C((12.5, 50.5), (19.5, 44), (20, 33))
           .C((20.5, 24), (21.5, 17), (24.5, 14))
           .C((27.5, 11), (30, 14), (27.5, 17.5)))
    for f in ((lambda p: p), mirror_x):
        ops.append(stroke("gold", arm.transformed(f).transformed(t), LYRE_W, round=True))
    return ops


def bouton(t, leaves=(-1, 1)):
    """Bouton pointu coiffé d'une perle, entre deux lyres ; feuilles couchées à sa base, deux points d'or."""
    ops = [fill("gold", circle(0, 8, 5.6).transformed(t)),
           fill("goldLight", circle(-1.4, 6.6, 2.1).transformed(t)),
           fill("gold", rect(-1.8, 11, 3.6, 8).transformed(t)),
           fill("gold", teardrop(33, 17.5).transformed(affine(0, 50, rot=-90)).transformed(t))]
    for sg in (-1, 1):
        f = (lambda p: p) if sg > 0 else mirror_x
        if sg in leaves:
            ops.append(fill("gold", leaf_bud(25, 9, 0.08).transformed(affine(1.5, 55)).transformed(f).transformed(t)))
        ops.append(fill("gold", circle(sg * 10, 27, 2.8).transformed(t)))
    return ops


def bandes(u0, u1):
    """Filets et bande d'or entre u0 et u1 : redoublent les filets du cadre (or sur or), pour la planche des modules."""
    return [fill("gold", rect(u0, a, u1 - u0, b - a)) for a, b in GOLD_BANDS]


def rehauts(u0, u1):
    """Rehauts des filets entre u0 et u1 (redoublent aussi les filets du cadre)."""
    return [fill("goldLight", rect(u0, v - w / 2, u1 - u0, w)) for v, w in HIGHLIGHTS]


def denticules():
    """Trous de cuir dans la bande d'or, puis dents d'or contre la soie, sur une période."""
    ops = [fill("leather", rect(DENT / 2 + k * DENT - 3.75, 84, 7.5, 11)) for k in range(4)]
    ops += [fill("gold", circle(k * DENT, 97, 3)) for k in range(4)]
    return ops


def module_roulette():
    base = bandes(-1, P + 1) + palmette(affine(0, 0)) + bouton(affine(P / 2, 0))
    return frise(P, base, rehauts(-1, P + 1) + denticules(), phase=0.0)


# --------------------------------------------------------------------------- #
# Coin (boîte 100 × 100, coin haut gauche, symétrique par rapport à la diagonale).
# La frise haute commence en x = 100 par une demi-palmette : le coin dessine l'autre moitié, puis le bouton
# qui suit (x = 64) ; à la place de la palmette suivante, un fer d'angle : un éventail tourné vers le dehors,
# coiffé d'une perle. Même chose, transposée, pour la frise gauche.
# --------------------------------------------------------------------------- #
def eventail_angle():
    """Éventail de cinq lobes en diagonale, pointe vers le coin extérieur, cœur en (40, 40)."""
    ops = []
    for a, L, W in ((54, 30, 13), (27, 38, 14), (0, 42, 15)):
        for sg in ((1,) if a == 0 else (-1, 1)):
            ops.append(fill("gold", lobe(L, W).transformed(affine(40, 40, rot=225 + sg * a))))
    ops.append(fill("goldLight", lobe(29, 4.8).transformed(affine(42, 42, rot=225))))
    ops.append(stroke("gold", Path().M((47, 33)).C((51, 41), (41, 51), (33, 47)), LYRE_W, round=True))
    ops.append(fill("gold", circle(41, 41, 5)))
    ops.append(fill("gold", circle(7, 7, 4.4)))
    ops.append(fill("goldLight", circle(6, 6, 1.7)))
    return ops


def module_coin():
    ops = []
    # angle de la bande des denticules : or plein jusqu'à la soie, un trou carré
    ops.append(fill("gold", rect(75, 75, 25, 25)))
    ops.append(fill("leather", rect(84, 84, 11, 11)))
    for f in ((lambda p: p), swap):
        ops += palmette(lambda p, f=f: f(affine(100, 0)(p)))
        # la feuille du bouton tournée vers le coin croiserait celle de l'autre bande : on l'omet
        ops += bouton(lambda p, f=f: f(affine(100 - P / 2, 0)(p)), leaves=(1,))
    ops += eventail_angle()
    return boite(ops)


def spec():
    return cadre(
        name="doublure",
        title="Doublure de soie",
        source="reliure de paroissien, fin du XIXᵉ siècle, doublure de soie moirée bordée d'une roulette dorée",
        inks=INKS,
        palettes=PALETTES,
        layout={"margin": 6, "top": 14, "side": 14, "bottom": 14, "textPadding": 8},
        ground="leather",
        top=module_roulette(),
        corners={"top": module_coin()},
        # l'or sous les frises (les denticules le percent), les rehauts par-dessus tout
        rules=[filet("gold", (a + b) / 2, b - a, over=False) for a, b in GOLD_BANDS]
              + [filet("goldLight", v, w) for v, w in HIGHLIGHTS],
    )
