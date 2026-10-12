"""
Cadre « Pour la communion » (p.119) : fond d'or olive ; tout autour, une guirlande continue d'œillets roses (cinq
pétales dentés, gorge cramoisie, reflets clairs, cœur nacré), vus de face ou de profil dans leur calice vert, avec des
boutons, de petites feuilles et des vrilles sur une tige vert sauge ; un œillet de face à chaque coin ; de chaque côté de
la bande, un filet vert entre deux filets bruns. Le pied est une fois et demie plus haut et sa guirlande plus touffue.
"""
from commun import Path, affine, boite, cadre, circle, filet, fill, frise, leaf, outlined, rect, smooth, spiral, stroke

ORDRE = 119

INKS = {
    "gold": "#C0A85E",       # or olive du fond
    "green": "#93A56F",      # vert sauge : filets, tiges, calices, feuilles
    "greenDark": "#55623C",  # contour des tiges et des feuilles
    "pink": "#E99FAA",       # pétales
    "pinkDark": "#B2475F",   # gorge et revers cramoisis
    "pinkLight": "#F8DFD8",  # reflets des pétales, cœur des fleurs
    "brown": "#7A4E42",      # filets bruns de part et d'autre du filet vert
    "outline": "#7D3343",    # contour des pétales
}

PALETTES = {
    "GREEN":  {"gold": "#9FAF6C", "green": "#55764A", "greenDark": "#2F4528"},
    "VIOLET": {"pink": "#B596C9", "pinkDark": "#6E4790", "pinkLight": "#EDE3F2", "outline": "#4A2E5E"},
    "RED":    {"pink": "#D9585E", "pinkDark": "#8E1F2E", "pinkLight": "#F4C4B8", "outline": "#5E1A22"},
    "ROSE":   {"pink": "#F0AFC0", "pinkDark": "#C76A88", "pinkLight": "#FCE9EC"},
    "WHITE":  {"pink": "#F5EFE3", "pinkDark": "#CDB88C", "pinkLight": "#FFFFFF", "outline": "#8C7A4E"},
    "BLACK":  {"gold": "#A9A397", "pink": "#8E8A86", "pinkDark": "#4A4643", "green": "#77756F"},
}

K = 1.5          # le pied est 1,5 fois plus haut que les côtés (≈ 270 px contre 175 sur la photo)

# Filets (unités des côtés, 100 = épaisseur de la bande) : (encre, profondeur du milieu, largeur).
FILETS = [
    ("brown", 0.9, 1.8), ("green", 4.6, 5.6), ("brown", 8.5, 2.2),        # bord extérieur
    ("brown", 88.7, 2.2), ("green", 92.6, 5.6), ("brown", 97.7, 4.6),     # bord intérieur
]

STEM = 4.2        # tige principale
STEM_O = 1.3      # son contour
PED = 3.0         # pédoncules et vrilles
PET_O = 1.4       # contour des pétales


# --------------------------------------------------------------------------- #
# Formes
# --------------------------------------------------------------------------- #
def petale(L, W):
    """Pétale d'œillet vers +x, base en (0, 0) : onglet étroit au cœur, limbe en éventail, bout denté."""
    pts = [(0, 0), (0.25 * L, -0.16 * W), (0.55 * L, -0.38 * W), (0.80 * L, -0.50 * W),
           (0.97 * L, -0.34 * W), (0.92 * L, -0.17 * W), (1.02 * L, -0.03 * W), (0.93 * L, 0.12 * W),
           (0.99 * L, 0.29 * W), (0.82 * L, 0.46 * W), (0.55 * L, 0.34 * W), (0.25 * L, 0.15 * W)]
    return smooth(pts, closed=True)


def fleur_face(cx, cy, R, rot=0.0, squash=1.0, s=1.0):
    """Œillet vu de face : cinq pétales roses dentés, gorge cramoisie, reflets clairs, cœur nacré.
    `squash` aplatit la fleur (vue un peu de biais), `s` met contours et détails à l'échelle."""
    place = affine(cx, cy, rot=rot, sx=1.0, sy=squash)
    petals, throats, lights = Path(), Path(), Path()
    W = 0.62 * R
    for k in range(5):
        a = 72 * k
        L = R * (1.0, 0.93, 1.04, 0.96, 0.99)[k]
        f = lambda p, a=a: place(affine(rot=a)(p))
        petals.extend(petale(L, W).transformed(f))
        throats.extend(petale(0.46 * L, 0.42 * W).transformed(f))
        lights.extend(leaf(0.40 * L, 0.42 * W).transformed(affine(0.50 * L, -0.12 * W, rot=6)).transformed(f))
    ops = outlined("pink", petals, outline=PET_O * s, outline_ink="outline")
    ops.append(fill("pinkDark", throats))
    ops.append(fill("pinkLight", lights))
    ops += outlined("pinkLight", circle(cx, cy, 0.17 * R), outline=1.2 * s, outline_ink="greenDark")
    return ops


def fleur_profil(x, y, ang, L=50.0, s=1.0):
    """Œillet vu de profil, pointe vers `ang` depuis (x, y) : calice vert en tube, deux pétales cramoisis au
    revers, trois pétales roses qui s'évasent en éventail."""
    t = affine(x, y, rot=ang, sx=L / 50.0)
    root = (15.0, 0.0)
    back = Path()
    for a in (-24, 24):
        back.extend(petale(27, 15).transformed(affine(*root, rot=a)))
    ops = outlined("pinkDark", back.transformed(t), outline=PET_O * s, outline_ink="outline")
    lights = Path()
    for a, l, w in ((-50, 26, 15), (50, 26, 15), (0, 32, 17)):
        ops += outlined("pink", petale(l, w).transformed(affine(*root, rot=a)).transformed(t), outline=PET_O * s,
                        outline_ink="outline")
        lights.extend(leaf(0.40 * l, 0.42 * w).transformed(affine(0.50 * l, -0.12 * w, rot=4))
                      .transformed(affine(*root, rot=a)))
    ops.append(fill("pinkLight", lights.transformed(t)))
    calyx = smooth([(0, 0), (4, -3.6), (11, -5.2), (17, -5.0), (19.5, 0), (17, 5.0), (11, 5.2), (4, 3.6)], closed=True)
    for sg in (-1, 1):
        calyx.extend(Path().M((13, sg * 3.2)).L((24, sg * 7.5)).L((17.5, sg * 6.0)).Z())
    ops += outlined("green", calyx.transformed(t), outline=1.1 * s, outline_ink="greenDark")
    return ops


def bouton(x, y, ang, L=26.0, s=1.0):
    """Bouton d'œillet pointant vers `ang` : calice vert, pointe rose."""
    t = affine(x, y, rot=ang, sx=L / 26.0)
    bud = smooth([(7, 0), (11, -4.6), (19, -4.2), (26, 0), (19, 4.2), (11, 4.6)], closed=True)
    ops = outlined("pink", bud.transformed(t), outline=PET_O * s, outline_ink="outline")
    ops.append(fill("pinkDark", smooth([(9, 0), (14, -2.0), (22, -0.6), (14, 1.2)], closed=True).transformed(t)))
    calyx = smooth([(0, 0), (3, -3.4), (9, -5.0), (12, 0), (9, 5.0), (3, 3.4)], closed=True)
    ops += outlined("green", calyx.transformed(t), outline=1.1 * s, outline_ink="greenDark")
    return ops


def feuille(x, y, ang, L=17.0, W=7.0, bend=0.1, s=1.0):
    return outlined("green", leaf(L, W, bend=bend).transformed(affine(x, y, rot=ang)), outline=1.0 * s,
                    outline_ink="greenDark")


def tige_periodique(points, P, i0, i1):
    """Courbe lisse (Catmull-Rom) par `points` (u croissants dans [0, P)), répétés tous les P : la tige d'une
    période se raccorde exactement à celle des voisines. Tracée du point i0 au point i1 (indices étendus)."""
    n = len(points)

    def pt(i):
        k, r = divmod(i, n)
        return (points[r][0] + k * P, points[r][1])
    p = Path().M(pt(i0))
    for i in range(i0, i1):
        p0, p1, p2, p3 = pt(i - 1), pt(i), pt(i + 1), pt(i + 2)
        p.C((p1[0] + (p2[0] - p0[0]) / 6, p1[1] + (p2[1] - p0[1]) / 6),
            (p2[0] - (p3[0] - p1[0]) / 6, p2[1] - (p3[1] - p1[1]) / 6), p2)
    return p


def tiges(main, branches, s=1.0, main_full=None):
    """Tige principale et pédoncules / vrilles : tous les contours, puis tous les verts (raccords propres).
    `main_full`, plus long que `main`, ne porte que le vert : il recouvre le raccord avec la période voisine."""
    br = Path()
    for b in branches:
        br.extend(b)
    ops = [stroke("greenDark", main, (STEM + 2 * STEM_O) * s), stroke("greenDark", br, (PED + 2 * STEM_O) * s, round=True),
           stroke("green", main_full or main, STEM * s), stroke("green", br, PED * s, round=True)]
    return ops


# --------------------------------------------------------------------------- #
# Frise haute et latérale
# --------------------------------------------------------------------------- #
P_SIDE = 330.0
V0 = 50.0         # hauteur de la tige aux raccords (et aux coins), à pente nulle
STEM_SIDE = [(0, V0), (22, V0), (64, 54), (110, 46), (150, 56), (186, 58), (222, 52), (262, 52), (290, 48), (308, V0)]


def module_guirlande():
    """Une période : œillet de profil levé vers le bord, œillet de face, bouton, œillet de face de biais, œillet
    de profil penché vers le texte. Aux raccords la tige passe seule : rien n'y serait coupé au bord d'un coin,
    et l'œillet de face du coin n'a pas de jumeau à côté de lui."""
    P = P_SIDE
    n = len(STEM_SIDE)
    main = tige_periodique(STEM_SIDE, P, 0, n)
    full = tige_periodique(STEM_SIDE, P, -1, n + 1)
    branches = [
        smooth([(150, 56), (155, 63), (160, 68)]),                    # vers le bouton
        smooth([(262, 52), (266, 55), (270, 57)]),                    # vers l'œillet penché
        smooth([(160, 56), (164, 44), (168, 34)]), spiral(170, 27, 1.0, 7.0, 1.2, a0=170),   # vrille
    ]
    base = tiges(main, branches, main_full=full)
    over = []
    over += feuille(66, 56, 60, bend=0.12)
    over += feuille(186, 58, 120, L=15, W=6.5, bend=-0.1)
    over += feuille(298, 49, -120, L=14, W=6.0, bend=0.1)
    over += fleur_profil(16, 50, -24, L=58)
    over += fleur_face(110, 46, 40, rot=8)
    over += bouton(160, 68, 36, L=24)
    over += fleur_face(222, 52, 37, rot=40, squash=0.86)
    over += fleur_profil(270, 57, 14, L=54)
    return frise(P, base, over, phase=0.0)


# --------------------------------------------------------------------------- #
# Frise du pied (unités du pied : 100 = sa hauteur ; × 1/K pour garder la taille des fleurs)
# --------------------------------------------------------------------------- #
P_FOOT = 230.0
V0F = 41.3        # = 62 / K : hauteur de la tige du pied au raccord avec le coin
STEM_FOOT = [(0, V0F), (22, 41), (66, 38), (106, 48), (156, 60), (190, 52), (208, 41)]


def filets_du_pied(P):
    """Le pied repeint son fond et ses filets : les filets du cadre (`rules`) sont à une profondeur relative,
    trop loin du bord dans une bande plus haute ; ici ils restent à la même distance du bord que sur les côtés."""
    ops = [fill("gold", rect(0, 0, P + 1, 100))]
    for ink, at, w in FILETS:
        v = at / K if at < 50 else 100 - (100 - at) / K
        ops.append(stroke(ink, Path().M((-1, v)).L((P + 1, v)), w / K))
    return ops


def module_guirlande_pied():
    """Une période du pied (v = 0 au bord du bas, 100 côté texte) : œillet de profil tourné vers le texte, œillet
    de face plus bas, œillet de profil tourné vers le bord, œillet de face plus haut, bouton."""
    P = P_FOOT
    s = 1 / K
    n = len(STEM_FOOT)
    main = tige_periodique(STEM_FOOT, P, 0, n)
    full = tige_periodique(STEM_FOOT, P, -1, n + 1)
    branches = [
        smooth([(106, 48), (108, 42), (110, 36)]),                    # vers l'œillet tourné vers le bord
        smooth([(190, 52), (194, 44), (198, 38)]),                    # vers le bouton
        smooth([(96, 46), (98, 58), (98, 68)]), spiral(94, 73, 1.0, 6.0, 1.2, a0=0),         # vrilles
        smooth([(196, 50), (204, 60), (208, 70)]), spiral(203, 75, 1.0, 5.5, 1.1, a0=-30),
    ]
    base = filets_du_pied(P) + tiges(main, branches, s=s, main_full=full)
    over = []
    over += feuille(130, 56, -60, L=13, W=5.5, s=s)
    over += feuille(36, 39, 110, L=12, W=5.0, s=s)
    over += fleur_profil(12, 41, 36, L=40, s=s)
    over += fleur_face(66, 38, 27, rot=12, s=s)
    over += fleur_profil(110, 36, -38, L=38, s=s)
    over += fleur_face(156, 60, 26, rot=44, squash=0.88, s=s)
    over += bouton(198, 38, -50, L=20, s=s)
    return frise(P, base, over, phase=0.0)


# --------------------------------------------------------------------------- #
# Coins : un œillet de face où la guirlande tourne
# --------------------------------------------------------------------------- #
def module_coin():
    """Coin haut, 100 × 100 : les tiges des deux bandes (hauteur V0, pente nulle) se rejoignent sous l'œillet."""
    c = 50.0
    br = Path().M((V0, 102)).C((V0, 80), (c, 70), (c, c)).M((102, V0)).C((80, V0), (70, c), (c, c))
    ops = [stroke("greenDark", br, STEM + 2 * STEM_O), stroke("green", br, STEM)]
    ops += feuille(V0, 86, 35, L=14, W=6.0)
    ops += feuille(86, V0, 55, L=14, W=6.0, bend=-0.1)
    # pétale vers l'angle extérieur, les autres symétriques par rapport à la diagonale
    ops += fleur_face(c, c, 38, rot=9)
    return boite(ops)


def module_coin_pied():
    """Coin du pied, 100 × 150 : repeint fond et filets (voir filets_du_pied), puis l'œillet et un bouton."""
    W, H = 100.0, 150.0
    ops = [fill("gold", rect(0, 0, W, H))]
    for ink, at, w in FILETS:
        if at < 50:
            d = at
            ops.append(stroke(ink, Path().M((d, H + 1)).L((d, d)).L((W + 1, d)), w))
        else:
            d = 100 - at
            ops.append(stroke(ink, Path().M((W - d, H + 1)).L((W - d, H - d)).L((W + 1, H - d)), w))
    cy = 62.0
    br = Path().M((V0, H + 2)).C((V0, 120), (48, 96), (50, cy))
    br.extend(Path().M((W + 2, V0F * K)).C((85, cy), (70, cy), (50, cy)))
    br.extend(smooth([(V0, 112), (55, 110), (60, 112)]))
    ops += [stroke("greenDark", br, STEM + 2 * STEM_O), stroke("green", br, STEM)]
    ops += feuille(49, 120, -150, L=15, W=6.5)
    ops += bouton(60, 112, 40, L=26)
    ops += fleur_face(50, cy, 39, rot=9)
    return boite(ops, W, H)


def spec():
    return cadre(
        name="communion",
        title="Pour la communion",
        source="paroissien chromolithographié, fin du XIXᵉ siècle, p.119 « Pour la communion »",
        inks=INKS,
        palettes=PALETTES,
        layout={"margin": 12, "top": 18, "side": 18, "bottom": 27, "textPadding": 12},
        ground="gold",
        top=module_guirlande(),
        bottom=module_guirlande_pied(),
        corners={"top": module_coin(), "bottom": module_coin_pied()},
        rules=[filet(ink, at, w, over=False) for ink, at, w in FILETS],
    )
