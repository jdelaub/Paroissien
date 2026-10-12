"""
Cadre « Pour la confession », d'après un paroissien chromolithographié de la fin du XIXᵉ siècle (p.97 et p.103) :
baguettes roses croisées terminées par des bourgeons d'or, arcature entrelacée en haut et sur les côtés, chaîne
d'anneaux au pied, rosaces aux coins. Voir METHODE.md pour la façon dont il a été relevé.
"""
import math

from commun import (Path, affine, arc, boite, cadre, circle, fill, fleur_de_lis, frise, outlined, palmette, stroke,
                    strand, teardrop, trefoil, OUTLINE, STRAND)

INKS = {
    "ground": "#2E4865",   # bleu nuit du fond des frises
    "accent": "#D8745A",   # saumon des lunettes
    "leaf": "#8EABBA",     # bleu clair des palmettes
    "gold": "#B89C5A",     # or (ocre métallique)
    "strand": "#D3CEC1",   # argent des entrelacs
    "bar": "#D4958A",      # baguettes roses
    "barLight": "#F3DDD3", # filet clair au centre des baguettes
    "outline": "#33261F",  # contour brun foncé
}

# Encres remplacées selon la couleur liturgique (core:liturgy LiturgicalColor) ; sans couleur
# liturgique, le cadre garde les couleurs du livre.
PALETTES = {
    "GREEN":  {"ground": "#2F5A44", "leaf": "#A8C29A"},
    "VIOLET": {"ground": "#4A3163", "leaf": "#B6A3CF"},
    "RED":    {"ground": "#7A2626", "accent": "#E0A25A", "leaf": "#D9B88F"},
    "ROSE":   {"ground": "#9C4F67", "accent": "#F0C3A6", "leaf": "#F2D8DA"},
    "WHITE":  {"ground": "#EFE3C6", "strand": "#B89C5A"},
    "BLACK":  {"ground": "#2A2826", "accent": "#8C8379", "leaf": "#A3A09A", "gold": "#9E9A90"},
    "GOLD":   {"ground": "#5A4720", "strand": "#E2CF9C", "leaf": "#C9B27A"},
}

# Dans un thème sombre, ces encres sont rapprochées du fond de page (FrameStyle.ornamentDim)
# pour que le cadre n'éblouisse pas ; le contour reste tel quel.
DIMMED_INKS = ["ground", "accent", "leaf", "gold", "strand", "bar", "barLight"]

# --------------------------------------------------------------------------- #
# Module 1 — « arcature » : arcades romanes entrelacées (bandes haute et latérales)
# --------------------------------------------------------------------------- #
def module_arcade():
    R = 78.0          # rayon des arcs = pas nominal
    P = R
    vb = 92.0         # ligne de naissance des arcs (près du bord intérieur)
    # croisement entre l'arc 0 (centre u=0) et l'arc +1 (centre u=P)
    vc = vb - math.sqrt(R * R - (P / 2) ** 2)
    ang = math.degrees(math.atan2(vc - vb, P / 2))           # angle du croisement vu de l'arc 0 (≈ -60°)
    ang1 = math.degrees(math.atan2(vc - vb, -P / 2))         # vu de l'arc +1 (≈ -120°)

    base, over = [], []

    # lunette (intérieur commun des arcs 0 et +1) : saumon
    lens = Path().M((0, vb))
    arc(P, vb, R, 180, ang1 + 360, start=False, path=lens)
    arc(0, vb, R, ang + 360, 360, start=False, path=lens)
    lens.L((P, 100)).L((0, 100)).Z()
    base.append(fill("accent", lens))

    # fleur de lys dans la lunette, pointe vers l'extérieur
    base += outlined("gold", fleur_de_lis(46).transformed(affine(P / 2, vb - 2)))

    # palmette bleue sous la clé de l'arc 0, pointant vers l'extérieur
    base += outlined("leaf", palmette(30).transformed(affine(0, 58, rot=-90)))
    # petit bourgeon d'or dans l'écoinçon extérieur entre deux arcs
    base += outlined("gold", trefoil(13).transformed(affine(P / 2, 2, rot=180)))

    # l'arc lui-même, prolongé jusqu'au bord intérieur
    a = Path().M((-R, 104)).L((-R, vb))
    arc(0, vb, R, 180, 360, start=False, path=a)
    a.L((R, 104))
    base += strand(a)

    # pas de passe « over » : l'arc k+1, dessiné après l'arc k, passe dessus à leur
    # croisement et dessous au suivant -> l'alternance de l'entrelacs est automatique.
    return frise(P, base, over, phase=0.0)


# --------------------------------------------------------------------------- #
# Module 2 — « chaîne » : anneaux entrelacés (bande du pied, plus haute)
# --------------------------------------------------------------------------- #
def module_chain():
    r = 36.0
    P = 61.0
    cy = 50.0
    h = math.sqrt(r * r - (P / 2) ** 2)
    top, bot = (P / 2, cy - h), (P / 2, cy + h)
    a_top0 = math.degrees(math.atan2(top[1] - cy, top[0]))          # vu de l'anneau 0
    a_bot0 = math.degrees(math.atan2(bot[1] - cy, bot[0]))

    base, over = [], []
    base.append(fill("accent", circle(0, cy, r)))

    # palmettes bleues horizontales, fleur de lys en haut, goutte d'or en bas
    base += outlined("leaf", palmette(19).transformed(affine(3, cy + 1)))
    base += outlined("leaf", palmette(19).transformed(affine(-3, cy + 1, rot=180)))
    base += outlined("gold", fleur_de_lis(25).transformed(affine(0, cy - 4)))
    base += outlined("gold", teardrop(17, 9).transformed(affine(0, cy + 6, rot=90)))

    # bourgeons d'or dans les écoinçons haut et bas entre deux anneaux
    base += outlined("gold", trefoil(12).transformed(affine(P / 2, 1.5, rot=180)))
    base += outlined("gold", trefoil(12).transformed(affine(P / 2, 98.5)))

    base += strand(circle(0, cy, r))

    # en bas, l'anneau +1 (dessiné après) passe dessus naturellement ; en haut, on
    # redessine un tronçon de l'anneau 0. Le contour est plus court que le ruban :
    # ses extrémités restent cachées sous l'argent, sans liseré visible.
    over.append(stroke("outline", arc(0, cy, r, a_top0 - 16, a_top0 + 16), STRAND + 2 * OUTLINE))
    over.append(stroke("strand", arc(0, cy, r, a_top0 - 20, a_top0 + 20), STRAND))

    return frise(P, base, over, phase=0.5)


# --------------------------------------------------------------------------- #
# Module 3 — rosace de coin (carré 100 × 100)
# --------------------------------------------------------------------------- #
def module_rosette():
    c = 50.0
    ops = []
    # feuilles bleues dans les angles, pointant vers le centre
    for k in range(4):
        a = 45 + 90 * k
        ca, sa = math.cos(math.radians(a)), math.sin(math.radians(a))
        ops += outlined("leaf", palmette(15).transformed(affine(c + 61 * ca, c + 61 * sa, rot=a + 180)))
    ops.append(fill("ground", circle(c, c, 36)))
    # quatre pétales saumon en croix, quatre feuilles d'or en diagonale
    petals = Path()
    for k in range(4):
        petals.extend(teardrop(31, 25).transformed(affine(c, c, rot=90 * k)))
    ops += outlined("accent", petals)
    golds = Path()
    for k in range(4):
        golds.extend(teardrop(27, 11).transformed(affine(c, c, rot=45 + 90 * k)))
    ops += outlined("gold", golds)
    ops += outlined("gold", circle(c, c, 6.5))
    ops += strand(circle(c, c, 38))
    return boite(ops)


def module_fleur():
    """Fleur de lys d'or pour combler les coins rectangulaires (boîte 100 × 100, pointe en haut)."""
    ops = outlined("gold", fleur_de_lis(80).transformed(affine(50, 90)))
    ops = outlined("leaf", palmette(24).transformed(affine(46, 84, rot=150))) + \
          outlined("leaf", palmette(24).transformed(affine(54, 84, rot=30))) + ops
    return boite(ops)


# --------------------------------------------------------------------------- #
# Module 4 — terminaison de baguette (anneau rose + bourgeon d'or), pointe vers +x
# --------------------------------------------------------------------------- #
def module_terminal():
    ops = []
    ring = circle(7, 0, 6)
    ops.append(stroke("outline", ring, 4 + 2 * 1.8))
    ops.append(stroke("bar", ring, 4))
    ops += outlined("gold", trefoil(30).transformed(affine(13, 0, rot=90)))
    return boite(ops)



# Baguettes (unités u : 100 = épaisseur de la bande la plus fine)
BAR = 7
BAR_OUTLINE = 1.8
BAR_LIGHT = 1.6


def spec():
    return cadre(
        name="confession",
        title="Pour la confession",
        source="paroissien chromolithographié, fin du XIXᵉ siècle, p.103 « Pour la confession »",
        inks=INKS,
        palettes=PALETTES,
        dimmed=DIMMED_INKS,
        layout={"margin": 13, "top": 20, "side": 20, "bottom": 34, "textPadding": 14},
        top=module_arcade(),
        bottom=module_chain(),
        corners={"top": module_rosette(), "fill": module_fleur()},
        bars={
            "layers": [["outline", BAR + 2 * BAR_OUTLINE], ["bar", BAR], ["barLight", BAR_LIGHT]],
            "overhang": 14,
            "terminal": module_terminal(),
            "cornerTerminals": True,
        },
    )
