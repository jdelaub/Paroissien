"""
Bandeau latéral « Rameaux d'olivier à la colombe » (dit « palmes »), d'après un paroissien illustré de la fin du
XIXᵉ siècle (photo pages-bandeaux-lateraux-vigne-palmes-vignettes, p.50 « Prières ») : gravure à l'encre brune sur
papier nu, une seule bande au côté extérieur de la page (à gauche sur ce verso). Un rameau d'olivier, tige près du
bord, feuilles lancéolées et petites grappes de boutons, monte le long du texte ; vers le milieu, un médaillon ovale
à bord sombre porte la colombe du Saint-Esprit, de face, ailes abaissées. En haut, le rameau s'épanouit sous le coin
de la vignette gravée de la page (une scène, qui entrera plus tard comme image) ; au pied, deux tiges croisées.

Relevé (photo de 4000 × 1638, à tourner de 180°) : bande de ≈ 190 px de large pour une colonne de texte de ≈ 610 px ;
tige à ≈ 30 % de la bande depuis le bord extérieur ; feuilles de ≈ 50 × 10 px ; un départ de rameau tous les
≈ 120 px ; médaillon de 110 × 145 px, centré à 55 % de la largeur de la bande et à ≈ 40 % de sa longueur depuis le
haut (au milieu de la justification, vignette comprise).

Palette : une encre propre, `lavis`, pour le ton moyen que font les hachures des feuilles (#A1907E à #BBAA99 après
correction du blanc), atténuée en thème sombre. Le trait (mesuré #725E4C, éclairci par le flou) est tracé à l'encre
du texte du thème (`text`) : brun-noir en clair, clair en sombre, où un trait brun disparaîtrait. Pas de palette
liturgique, faute d'encre de fond. Le fond du médaillon, en lavis, détache la colombe évidée (encre `paper`).

Simplifications :
  - la bande fait 30 dp, au lieu du quart de la colonne de texte ;
  - le dessin libre de la gravure devient une frise de 255 unités : trois rameaux de port différent vers le texte, un
    rameau court au dehors, des feuilles isolées ; toutes les feuilles restent dans la période (rien n'est coupé aux
    raccords) ;
  - le rameau pousse d'un seul élan vers le haut (frise non symétrique), derrière le médaillon posé à 42 % de la
    bande ; il finit en bouquet de feuilles en haut et part de deux tiges coupées croisées au pied.
"""
import math

from commun import Path, affine, boite, bouts, cadre, circle, ellipse, fill, frise, leaf, piece, smooth, stroke

ORDRE = 50

INKS = {
    "lavis": "#AE9B83",   # ton moyen des feuilles hachurées, fond du médaillon
}
TRAIT = "text"            # le trait suit l'encre du texte du thème : brun-noir en clair, clair en sombre

S = 30.0          # épaisseur de la bande (dp)
STEM_V = 28.0     # tige principale, depuis le bord extérieur
STEM_W = 4.4      # tige principale
TWIG_W = 2.6      # rameaux
LEAF_O = 1.0      # contour des feuilles (de chaque côté)
RIB_W = 0.8       # nervure
FL = 1.12         # échelle des feuilles


# --------------------------------------------------------------------------- #
# Tiges, feuilles, boutons
# --------------------------------------------------------------------------- #
def courbe(pts, period=None):
    """Courbe lisse (Catmull-Rom) par des points ; avec `period`, prolongée d'un segment de chaque côté pour que deux
    périodes voisines se raccordent sans couture."""
    pts = list(pts)
    if period:
        q = [(x - period, y) for x, y in pts[-2:-1]] + pts + [(x + period, y) for x, y in pts[1:2]]
    else:
        q = [pts[0]] + pts + [pts[-1]]
    p = Path().M(q[1])
    for i in range(1, len(q) - 2):
        p0, p1, p2, p3 = q[i - 1], q[i], q[i + 1], q[i + 2]
        p.C((p1[0] + (p2[0] - p0[0]) / 6, p1[1] + (p2[1] - p0[1]) / 6),
            (p2[0] - (p3[0] - p1[0]) / 6, p2[1] - (p3[1] - p1[1]) / 6), p2)
    return p


def pousse(x, y, ang, L, bend=0.0, n=6):
    """Points d'un rameau partant de (x, y) dans la direction ang (degrés), long de L, qui tourne de `bend` degrés ;
    rend aussi l'angle de la tangente en chaque point."""
    pts, angs = [(x, y)], [ang]
    for i in range(n):
        a = ang + bend * (i + 0.5) / n
        x += L / n * math.cos(math.radians(a))
        y += L / n * math.sin(math.radians(a))
        pts.append((x, y))
        angs.append(ang + bend * (i + 1) / n)
    return pts, angs


class Olivier:
    """Couches d'un rameau d'olivier : les tiges d'abord, puis les feuilles, puis les boutons."""

    def __init__(self):
        self.tiges, self.feuilles, self.boutons = [], [], []

    def tige(self, path, w=TWIG_W):
        self.tiges.append(stroke(TRAIT, path, w, round=True))

    def feuille(self, x, y, ang, L, W=None, bend=0.0):
        """Feuille d'olivier, lame étroite pointue aux deux bouts, base en (x, y), vers ang."""
        L *= FL
        W = 0.28 * L if W is None else W
        t = affine(x, y, rot=ang)
        p = leaf(L, W, bend).transformed(t)
        rib = smooth([(0.1 * L, 0), (0.5 * L, bend * L * 0.6), (0.82 * L, bend * L * 0.9)]).transformed(t)
        self.feuilles += [stroke(TRAIT, p, 2 * LEAF_O), fill("lavis", p), stroke(TRAIT, rib, RIB_W)]

    def grappe(self, x, y, ang, n=3, r=2.6):
        """Petite grappe de boutons (fleurs d'olivier) au bout d'un pédoncule, vers ang."""
        a = math.radians(ang)
        ca, sa = math.cos(a), math.sin(a)
        ex, ey = x + 5 * ca, y + 5 * sa
        self.tiges.append(stroke(TRAIT, Path().M((x, y)).L((ex, ey)), 1.2, round=True))
        offs = [(1.0, 0.0), (2.2, -1.1), (2.2, 1.1), (3.4, 0.0)][:n]
        for f, s in offs:
            bx = ex + f * r * ca - s * r * sa
            by = ey + f * r * sa + s * r * ca
            c = circle(bx, by, r)
            self.boutons += [stroke(TRAIT, c, 1.6), fill("lavis", c)]

    def rameau(self, x, y, ang, L, bend=0.0, feuilles=(), bout=26, grappe=None):
        """Rameau : tige, feuilles posées le long (t de 0 à 1, écart angulaire, taille), feuille au bout (taille, 0 :
        aucune) ; `grappe` : (t, écart, nombre) de boutons."""
        pts, angs = pousse(x, y, ang, L, bend)
        self.tige(smooth(pts))
        n = len(pts) - 1
        for t, da, size in feuilles:
            i = max(0, min(n, round(t * n)))
            self.feuille(pts[i][0], pts[i][1], angs[i] + da, size)
        if bout:
            self.feuille(pts[-1][0], pts[-1][1], angs[-1], bout)
        if grappe:
            t, da, k = grappe
            i = max(0, min(n, round(t * n)))
            self.grappe(pts[i][0], pts[i][1], angs[i] + da, k)
        return pts[-1], angs[-1]

    def ops(self):
        return self.tiges + self.feuilles + self.boutons


# --------------------------------------------------------------------------- #
# Frise : le rameau pousse vers u = 0 (le bout de la bande). Toutes les feuilles restent dans la période : rien n'est
# coupé aux raccords, ni contre le médaillon ou les bouts.
# --------------------------------------------------------------------------- #
P = 255.0


def module_cote():
    o = Olivier()
    o.tige(courbe([(0, STEM_V), (52, STEM_V + 5), (112, STEM_V - 4), (172, STEM_V + 4), (222, STEM_V - 3), (255, STEM_V)],
                  period=P), STEM_W)
    # trois rameaux vers le texte, de port différent : long et couché, court et dressé, moyen
    o.rameau(246, STEM_V, 118, 64, bend=26, bout=36, grappe=(0.12, 80, 4),
             feuilles=((0.2, 54, 34), (0.38, -46, 32), (0.55, 52, 32), (0.72, -42, 30), (0.88, 46, 28)))
    o.rameau(164, STEM_V + 4, 100, 50, bend=44, bout=32,
             feuilles=((0.3, -48, 30), (0.55, 50, 30), (0.8, -40, 26)))
    o.rameau(90, STEM_V + 2, 122, 54, bend=22, bout=32, grappe=(0.2, 76, 3),
             feuilles=((0.3, 52, 30), (0.5, -44, 30), (0.75, 48, 28)))
    # un rameau court au dehors
    o.rameau(132, STEM_V - 3, 206, 22, bend=-10, bout=30, feuilles=((0.7, 44, 26),))
    # feuilles isolées : au dehors, puis sur la tige vers le haut
    for u, a, L in ((228, 216, 34), (186, 200, 30), (66, 218, 34), (38, 202, 30)):
        o.feuille(u, STEM_V - 1, a, L, bend=-0.04)
    for u, a, L in ((206, 170, 32), (112, 168, 30), (40, 164, 30)):
        o.feuille(u, STEM_V + 1, a, L, bend=0.04)
    o.grappe(150, STEM_V - 2, 240, 3)
    o.grappe(48, STEM_V - 4, 232, 3)
    return frise(P, o.ops(), [], phase=0.0, outset=12, inset=4, symetrique=False)


# --------------------------------------------------------------------------- #
# Bouts. En haut, le rameau finit en bouquet de feuilles (boîte posée au bout, u de 0 à W_BOUT). Au pied, deux tiges
# coupées se croisent ; la boîte est dessinée comme au début de la bande et retournée par le moteur : u = 0 y est le
# bas de la page, et le haut de la page est vers +u (les feuilles y pointent).
# --------------------------------------------------------------------------- #
W_BOUT = 110.0
W_PIED = 130.0


def bout_haut():
    o = Olivier()
    o.tige(courbe([(W_BOUT + 4, STEM_V), (84, STEM_V + 2), (52, STEM_V + 6)]), STEM_W)
    o.feuille(52, STEM_V + 6, 184, 42)
    o.feuille(54, STEM_V + 4, 214, 38, bend=-0.06)
    o.feuille(54, STEM_V + 8, 156, 38, bend=0.06)
    o.rameau(100, STEM_V + 1, 120, 56, bend=30, feuilles=((0.35, -46, 32), (0.65, 50, 30)), bout=34,
             grappe=(0.2, 80, 4))
    o.feuille(86, STEM_V - 1, 204, 36, bend=-0.04)
    o.grappe(66, STEM_V - 1, 236, 3)
    return boite(o.ops(), w=W_BOUT, h=100, outset=6, inset=4)


def bout_pied():
    o = Olivier()
    # la tige principale descend et se recourbe vers le texte ; une seconde tige la croise et remonte côté texte
    o.tige(courbe([(W_PIED + 4, STEM_V), (96, STEM_V + 1), (58, STEM_V + 8), (30, STEM_V + 24), (10, STEM_V + 38)]),
           STEM_W)
    o.tige(courbe([(8, 18), (26, 30), (48, 48), (72, 62), (92, 70)]), STEM_W * 0.8)
    o.feuille(92, 70, 8, 31)
    o.feuille(80, 66, -26, 30, bend=-0.05)
    o.feuille(84, 68, 44, 28, bend=0.05)
    o.feuille(60, 56, 64, 28)
    o.grappe(66, 60, -40, 3)
    # feuilles de la tige principale, vers le haut
    o.feuille(92, STEM_V, -22, 32, bend=-0.04)
    o.feuille(70, STEM_V + 4, -32, 30, bend=-0.04)
    o.feuille(44, STEM_V + 14, -40, 26)
    o.grappe(90, STEM_V - 1, -60, 3)
    # sections des tiges coupées
    for x, y, r in ((10, STEM_V + 38, 2.6), (8, 18, 2.1)):
        o.boutons += [stroke(TRAIT, circle(x, y, r), 1.2), fill("lavis", circle(x, y, r))]
    return boite(o.ops(), w=W_PIED, h=100, outset=6, inset=4)


# --------------------------------------------------------------------------- #
# Pièce centrale : médaillon ovale, colombe de face, ailes abaissées
# --------------------------------------------------------------------------- #
MV, MRV, MRU = 52.0, 40.0, 52.0     # centre (profondeur), demi-axes en profondeur et le long de la bande
W_CENTRE = 150.0
A_CENTRE = 0.42                     # comme sur la gravure : vers 40 % de la bande depuis le haut


def colombe(f):
    """Colombe du Saint-Esprit de face, ailes déployées et abaissées. Dessinée dans un repère local (x : envergure,
    y : vers le bas, tête en haut, centre du médaillon en (0, 0)) puis placée par f."""
    body = smooth([(0, -20), (6, -14), (7, -2), (5, 8), (0, 12), (-5, 8), (-7, -2), (-6, -14)], closed=True)
    wing = smooth([(4, -14), (13, -19), (22, -18), (26, -12), (25, -2), (21, 8), (15, 15), (10, 17), (8, 8), (6, -4)],
                  closed=True)
    tail = smooth([(-4, 8), (4, 8), (8, 26), (0, 29), (-8, 26)], closed=True)
    head = circle(0, -21, 5.5)
    shapes = [s.transformed(f) for s in (tail, wing, wing.transformed(lambda p: (-p[0], p[1])), body, head)]
    ops = [stroke(TRAIT, s, 2.4) for s in shapes] + [fill("paper", s) for s in shapes]
    # plumes : rémiges des ailes et de la queue
    for sg in (1, -1):
        for a, b in (((10, -12), (14, 14)), ((15, -14), (19, 9)), ((20, -14), (23, 2))):
            m = (sg * ((a[0] + b[0]) / 2 + 1.5), (a[1] + b[1]) / 2)
            ops.append(stroke(TRAIT, smooth([(sg * a[0], a[1]), m, (sg * b[0], b[1])]).transformed(f), 0.9, round=True))
    for x in (-3.2, 0, 3.2):
        ops.append(stroke(TRAIT, Path().M((x * 0.6, 12)).L((x * 1.4, 26)).transformed(f), 0.9, round=True))
    # bec et yeux
    ops.append(fill(TRAIT, Path().M((-1.6, -18)).L((1.6, -18)).L((0, -14.5)).Z().transformed(f)))
    for x in (-2.2, 2.2):
        ops.append(fill(TRAIT, circle(x, -22, 0.9).transformed(f)))
    return ops


def centre_cote():
    o = Olivier()
    h = W_CENTRE / 2
    # la tige passe derrière le médaillon et continue vers le haut (u < 0)
    o.tige(courbe([(-h - 4, STEM_V), (-40, STEM_V - 2), (40, STEM_V - 2), (h + 4, STEM_V)]), STEM_W)
    # au-dessus : feuilles qui sortent de derrière le médaillon
    o.feuille(-40, STEM_V - 2, 204, 31)
    o.feuille(-44, MV + 2, 176, 26)
    o.feuille(-40, MV + 22, 152, 28)
    # au-dessous : feuilles dressées dont la pointe passe derrière le médaillon
    o.feuille(66, STEM_V - 1, 204, 30, bend=-0.04)
    o.feuille(70, STEM_V + 1, 150, 30, bend=0.04)
    o.grappe(56, STEM_V - 2, 234, 3)
    ops = o.ops()
    # médaillon : bord sombre, fond lavis, colombe évidée
    ops.append(fill(TRAIT, ellipse(0, MV, MRU, MRV)))
    ops.append(fill("lavis", ellipse(0, MV, MRU - 4.6, MRV - 4.6)))
    ops.append(stroke(TRAIT, ellipse(0, MV, MRU - 8.0, MRV - 8.0), 0.8))
    # la colombe, tête vers le haut de la page (-u) : y local le long de la bande, x local en profondeur
    k = 1.2
    ops += colombe(lambda p: (k * p[1], MV + k * p[0]))
    return piece(W_CENTRE, ops, outset=6, inset=4, a=A_CENTRE)


# --------------------------------------------------------------------------- #
LAYOUT = {"margin": 12, "top": S, "side": S, "bottom": S, "textPadding": 10}


def spec():
    cote = module_cote()
    return cadre(
        name="palmes",
        title="Rameaux d'olivier",
        source="paroissien illustré, fin du XIXᵉ siècle, p.50 « Prières », bandeau latéral à la colombe",
        inks=INKS,
        palettes={},
        dimmed=["lavis"],
        layout=LAYOUT,
        ground=None,
        disposition="laterale",
        top=cote,
        side=cote,
        centers={"side": centre_cote()},
        ends={"side": bouts(bout_haut(), bout_pied())},
    )
