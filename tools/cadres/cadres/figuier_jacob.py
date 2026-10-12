"""
Bandeau « Figuier, Jacob, Juda, David », d'après un paroissien illustré de la fin du XIXᵉ siècle (p.39, Messe ;
photo bandeau-figuier-jacob-juda-david, prise de côté : rotation de 270° en sens horaire pour remettre la page
droite). Similigravure brune sur papier nu, sur le seul côté extérieur de la page : une branche de figuier monte
du pied de la page à sa tête, chargée de feuilles palmées et de figues ; vers le milieu, un cartouche aux côtés
échancrés porte sur trois lignes « JACOB / JUDA / DAVID », trois ancêtres du Christ dans la généalogie de saint
Matthieu (Mt 1, 2-6).

Relevé (photo redressée de 1638 × 4000 px) :
  - bande de ≈ 300 px de large sur toute la hauteur de la page (≈ 3270 px, onze largeurs) ; marges extérieure et
    vers le texte à peu près égales à la bande ;
  - une feuille tous les ≈ 85 px, alternativement de chaque côté d'une tige qui ondule ; feuilles de 120 à 150 px ;
  - cartouche de ≈ 295 × 300 px, aussi large que la bande, échancré d'un demi-cercle au milieu des côtés gauche et
    droit, centré à 55 % de la bande ; capitales bâton de ≈ 42 px (14 % de la bande), en travers de la bande.
Palette (blanc corrigé, papier mesuré #C3BBB7) : trait #8A7866 et teintes #AE9E8C à #DFD3C1, la trame de la
similigravure fondue par le flou de la photo. Deux encres : le trait sépia, plus foncé que la mesure pour compenser
le flou, et une teinte claire pour les feuilles, la tige et le fond du cartouche. Pas de palette liturgique (pas
d'encre de fond).

Simplifications :
  - la trame et ses ombres deviennent un aplat de teinte cerné de trait, avec trois nervures par feuille ;
  - le feuillage, très dense et enchevêtré sur la page, devient une tige à feuilles alternées : par pas de 120
    unités (31 dp), une grande feuille de chaque côté, une jeune feuille et deux figues ; les feuilles débordent
    sur les pièces voisines (`deborde`) ;
  - la branche pousse d'un seul élan à travers le cartouche (`symetrique=False`) : pièce du haut, une pousse ;
    pièce du pied, la branche coupée net, d'où part une jeune pousse ;
  - le cartouche est centré sur la bande (`a` laissé à 0,5) ; les feuilles voisines passent derrière lui, alors
    que sur la page un bouquet de feuilles chevauche son bord haut ; les petites étoiles de ses coins sont omises ;
  - l'inscription est en capitales bâton tracées au trait (chemins, pas de police), de 22 unités de haut au lieu
    de 14 : ≈ 6 dp dans une bande de 26 dp. Le cartouche est dessiné tel qu'il paraît sur la bande droite d'un
    recto ; `droit` le pose tel quel sur un verso, où l'inscription reste droite et lisible.
"""
import math

from commun import (Path, affine, arc, boite, bouts, cadre, circle, ellipse, fill, frise, outlined, piece, smooth,
                    stroke, strand)

ORDRE = 39

SIDE = 26  # largeur de la bande (dp)

INKS = {
    "trait": "#5C4A3A",   # sépia de la similigravure, contours, nervures et lettres
    "teinte": "#D5C8B3",  # teinte claire : feuilles, tiges, fond du cartouche
}

TRAIT, TEINTE = "trait", "teinte"
CONTOUR = 1.9         # contour des feuilles (de chaque côté du bord)
NERVURE = 1.5

P = 120.0             # pas de la frise (unités)
V_TIGE = 50.0         # profondeur de la tige aux raccords (bouts de pas, cartouche, pièces de bout)
DEBORDE = 16.0        # débord des feuilles d'un pas sur ses voisins (pièces de bout, cartouche)


# --------------------------------------------------------------------------- #
# Figuier : feuille, figue, tige
# --------------------------------------------------------------------------- #
def _polaire(a, r):
    return (r * math.cos(math.radians(a)), r * math.sin(math.radians(a)))


# Lobes de la feuille : (direction en degrés, longueur, demi-largeur), en fractions de L ; le lobe central porte
# deux petites dents.
LOBES = ((-112, 0.52, 0.17), (-52, 0.86, 0.23), (0, 1.0, 0.25), (52, 0.86, 0.23), (112, 0.52, 0.17))
SINUS = (0.44, 0.50, 0.50, 0.44)   # profondeur des sinus entre lobes voisins


def feuille_figuier(L):
    """Feuille de figuier, base en (0,0), pointe vers +x ; L = de la base à la pointe. Renvoie (contour, nervures)."""
    pts = [_polaire(180, 0.10 * L)]                                   # échancrure du pétiole
    for i, (a, long, large) in enumerate(LOBES):
        if i:
            pts.append(_polaire((a + LOBES[i - 1][0]) / 2, SINUS[i - 1] * L))
        r, w = long * L, large * L
        local = [(0.5 * r, -0.8 * w), (0.82 * r, -w), (r, -0.25 * w), (r, 0.25 * w), (0.82 * r, w), (0.5 * r, 0.8 * w)]
        if a == 0:                                                     # dents du lobe central
            local[1:1] = [(0.62 * r, -0.7 * w)]
            local[-1:-1] = [(0.62 * r, 0.7 * w)]
        pts += [affine(rot=a)(p) for p in local]
    contour = smooth(pts, closed=True)
    nervures = Path()
    for a, long, _ in LOBES[1:4]:
        nervures.M((0.04 * L, 0)).L(_polaire(a, long * L * 0.7))
    return contour, nervures


def feuille(base, angle, L, queue=7.0):
    """Feuille posée : pétiole depuis `base` (sur la tige), dans la direction `angle` (degrés, y vers le bas)."""
    forme, nervures = feuille_figuier(L)
    a = math.radians(angle)
    pied = (base[0] + queue * math.cos(a), base[1] + queue * math.sin(a))
    f = affine(pied[0], pied[1], angle)
    ops = [stroke(TRAIT, Path().M(base).L(pied), 2.6, round=True)]
    ops += outlined(TEINTE, forme.transformed(f), outline=CONTOUR, outline_ink=TRAIT)
    ops.append(stroke(TRAIT, nervures.transformed(f), NERVURE, round=True))
    return ops


def figue(attache, angle, L=15.0):
    """Figue en poire pendue à `attache`, l'œil vers `angle`."""
    W = 0.78 * L
    pts = [(0, 0), (0.28 * L, -0.2 * W), (0.6 * L, -0.5 * W), (0.92 * L, -0.34 * W), (L, 0),
           (0.92 * L, 0.34 * W), (0.6 * L, 0.5 * W), (0.28 * L, 0.2 * W)]
    f = affine(attache[0], attache[1], angle)
    corps = smooth(pts, closed=True).transformed(f)
    a = math.radians(angle)
    oeil = (attache[0] + 0.9 * L * math.cos(a), attache[1] + 0.9 * L * math.sin(a))
    return outlined(TEINTE, corps, outline=CONTOUR, outline_ink=TRAIT) + [fill(TRAIT, circle(oeil[0], oeil[1], 1.6))]


def tige(path, largeur=5.6):
    """Branche : un tube clair cerné de trait."""
    return strand(path, width=largeur, ink=TEINTE, outline=1.6, outline_ink=TRAIT)


def v_tige(u):
    """Profondeur de la tige de la frise : une ondulation par pas, V_TIGE aux raccords."""
    return V_TIGE - 4 * math.sin(2 * math.pi * u / P)


def sur_tige(u):
    return (u, v_tige(u))


def frise_figuier():
    # Tige prolongée d'un demi-pas de chaque côté : les tangentes se raccordent d'un pas à l'autre.
    base = tige(smooth([sur_tige(k * P / 8) for k in range(-4, 13)]))
    # La branche pousse vers u = 0, le haut de la bande : feuilles alternées, pointe vers le haut ; figues pendantes.
    over = []
    over += feuille(sur_tige(92), -138, 24, queue=4)          # jeune feuille, côté extérieur
    over += feuille(sur_tige(30), -108, 46)                   # feuille côté extérieur
    over += feuille(sur_tige(90), 110, 44)                    # feuille côté texte
    over += figue(sur_tige(26), 40, 20)                       # figues pendues sous les feuilles
    over += figue(sur_tige(98), -34, 18)
    return frise(P, base, over, inset=8, outset=8, symetrique=False, deborde=DEBORDE)


def pousse():
    """Pièce du haut : la tige s'achève en deux jeunes feuilles et une figue, pointe vers le bout de la bande."""
    w = 72.0
    # la tige suit d'abord celle de la frise (mêmes jalons), qui déborde ici de DEBORDE unités, puis s'achève
    ops = tige(smooth([(w - u, v_tige(-u)) for u in (-P / 8, 0, P / 8, P / 4)] + [(34, 53)]))
    ops += figue((60, v_tige(60 - w)), 34, 15)
    ops += feuille((52, v_tige(52 - w)), -128, 28)
    ops += feuille((36, 53), 180, 34, queue=3)
    return boite(ops, w=w, h=100, outset=2, inset=2)


def souche():
    """Pièce du pied, retournée par le moteur (u = 0 au bout de la bande) : la branche coupée net, dont part une
    pousse ; les feuilles pointent vers +u, c'est-à-dire vers le haut de la page."""
    w = 84.0
    # retournée : u local = w - (distance au-delà de la frise) ; la tige suit d'abord celle de la frise
    ops = tige(smooth([(w + u, v_tige(-u)) for u in (P / 8, 0, -P / 8, -P / 4, -3 * P / 8, -P / 2)] + [(12, 51.5)]))
    ops += outlined(TEINTE, ellipse(12, 51, 2.6, 4.8), outline=1.6, outline_ink=TRAIT)      # coupe de la branche
    ops.append(stroke(TRAIT, Path().M((22, 47.5)).C((23, 50), (23, 53), (22, 56)), 1.2))  # nœud de l'écorce
    ops += feuille((44, v_tige(w - 44)), 58, 28)
    ops += feuille((30, v_tige(w - 30)), -60, 24)
    return boite(ops, w=w, h=100, outset=2, inset=2)


# --------------------------------------------------------------------------- #
# Inscription : capitales bâton tracées au trait (hauteur 100, ligne de base y = 0)
# --------------------------------------------------------------------------- #
def _glyphe_J():
    return Path().M((50, -100)).L((50, -30)).C((50, -8), (38, 0), (25, 0)).C((12, 0), (2, -8), (0, -24)), 50


def _glyphe_A():
    p = Path().M((0, 0)).L((35, -100)).L((70, 0))
    p.M((14, -36)).L((56, -36))
    return p, 70


def _glyphe_C():
    return arc(36, -50, 36, 42, 318, ry=50), 64


def _glyphe_O():
    return arc(37, -50, 37, 0, 360, ry=50).Z(), 74


def _glyphe_B():
    p = Path().M((0, 0)).L((0, -100)).L((28, -100)).C((46, -100), (53, -89), (53, -77)).C((53, -63), (43, -54), (27, -54))
    p.L((0, -54))
    p.M((27, -54)).C((49, -54), (58, -42), (58, -28)).C((58, -10), (46, 0), (28, 0)).L((0, 0))
    return p, 58


def _glyphe_U():
    return Path().M((0, -100)).L((0, -34)).C((0, -12), (14, 0), (32, 0)).C((50, 0), (64, -12), (64, -34)).L((64, -100)), 64


def _glyphe_D():
    return (Path().M((0, 0)).L((0, -100)).L((22, -100)).C((52, -100), (66, -78), (66, -50))
            .C((66, -22), (52, 0), (22, 0)).Z()), 66


def _glyphe_V():
    return Path().M((0, -100)).L((35, 0)).L((70, -100)), 70


def _glyphe_I():
    return Path().M((0, -100)).L((0, 0)), 0


GLYPHES = {"J": _glyphe_J, "A": _glyphe_A, "C": _glyphe_C, "O": _glyphe_O, "B": _glyphe_B, "U": _glyphe_U,
           "D": _glyphe_D, "V": _glyphe_V, "I": _glyphe_I}
INTERLETTRE = 20                      # entre les boîtes de deux lettres (pour 100 de haut, trait compris)
CRENAGE = {"AV": -14, "VA": -14}      # paires obliques rapprochées


def ligne(mot, base, h):
    """Un mot centré sur x = 0, ligne de base y = `base`, capitales de hauteur h, dans le repère de la page."""
    s = h / 100.0
    pos, x = [], 0.0
    for i, c in enumerate(mot):
        if i:
            x += INTERLETTRE + CRENAGE.get(mot[i - 1] + c, 0)
        g, w = GLYPHES[c]()
        pos.append((g, x))
        x += w
    p = Path()
    for g, gx in pos:
        p.extend(g.transformed(affine((gx - x / 2) * s, base, sx=s)))
    return p


def largeur_mot(mot, h):
    return (sum(GLYPHES[c]()[1] for c in mot) + INTERLETTRE * (len(mot) - 1)
            + sum(CRENAGE.get(a + b, 0) for a, b in zip(mot, mot[1:]))) * h / 100.0


# --------------------------------------------------------------------------- #
# Cartouche, dessiné dans le repère de la page (x vers la droite, y vers le bas, origine au centre) tel qu'il paraît
# sur la bande droite d'un recto, puis porté dans le repère de la bande (u vers le bas, v de la droite vers la
# gauche). Avec `droit`, le moteur le pose tel quel sur le verso : l'inscription se lit toujours droite.
# --------------------------------------------------------------------------- #
H_LETTRE = 22.0
INTERLIGNE = 8.5      # blanc entre deux lignes
EPAISSEUR = 3.2       # trait des lettres
MOTS = ("JACOB", "JUDA", "DAVID")
DEMI_X = 53.0         # demi-largeur du cartouche, en travers de la bande (v de -3 à 103)
DEMI_Y = 53.0         # demi-hauteur, le long de la bande
ECHANCRURE = 11.0     # rayon des échancrures des côtés
FILET = 4.0           # retrait du filet intérieur


def vers_bande(p):
    return (p[1], V_TIGE - p[0])


def contour_cartouche(d=0.0):
    """Rectangle échancré d'un demi-cercle au milieu de chaque côté ; `d` le rentre (filet intérieur)."""
    x, y, r = DEMI_X - d, DEMI_Y - d, ECHANCRURE + d
    p = Path().M((-x, -y)).L((x, -y)).L((x, -r))
    arc(x, 0, r, -90, -270, start=False, path=p)        # échancrure de droite, creusée vers l'intérieur
    p.L((x, y)).L((-x, y)).L((-x, r))
    arc(-x, 0, r, 90, -90, start=False, path=p)         # échancrure de gauche
    return p.Z()


def cartouche():
    ops = outlined(TEINTE, contour_cartouche().transformed(vers_bande), outline=2.2, outline_ink=TRAIT)
    ops.append(stroke(TRAIT, contour_cartouche(FILET).transformed(vers_bande), 0.9))
    bloc = 3 * H_LETTRE + 2 * INTERLIGNE
    lettres = Path()
    for i, mot in enumerate(MOTS):
        lettres.extend(ligne(mot, -bloc / 2 + H_LETTRE + i * (H_LETTRE + INTERLIGNE), H_LETTRE))
    ops.append(stroke(TRAIT, lettres.transformed(vers_bande), EPAISSEUR, round=True))
    marge = DEMI_X - V_TIGE + 2
    return piece(2 * DEMI_Y + 2, ops, outset=marge, inset=marge, droit=True)


def spec():
    assert largeur_mot("JACOB", H_LETTRE) + EPAISSEUR < 2 * (DEMI_X - FILET - 2), "inscription trop large"
    assert largeur_mot("JUDA", H_LETTRE) + EPAISSEUR < 2 * (DEMI_X - ECHANCRURE - FILET - 2), "JUDA touche les échancrures"
    f = frise_figuier()
    return cadre(
        name="figuier_jacob",
        title="Figuier, Jacob, Juda, David",
        source="paroissien illustré, p.39 (Messe), photo bandeau-figuier-jacob-juda-david",
        inks=INKS,
        palettes={},
        layout={"margin": 12, "top": SIDE, "side": SIDE, "bottom": SIDE, "textPadding": 12},
        ground=None,
        disposition="laterale",
        top=f,
        side=f,
        centers={"side": cartouche()},
        ends={"side": bouts(pousse(), souche())},
    )
