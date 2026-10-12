"""
Cadre « Rinceaux et candélabres », d'après un cadre sans texte d'un paroissien chromolithographié de la fin du
XIXᵉ siècle : un portique de la Renaissance italienne sur fond bleu. En haut, un rinceau d'acanthe verte semé
d'églantines rouges, avec un bouquet de lys blancs au milieu ; sur les côtés, des candélabres d'or (vase à anses et
bouquet d'acanthe, dauphins, gemme rouge et perles) ; au pied, un socle d'or : plinthe, panneau bleu à rinceaux d'or,
cartouche à gemme entourée de perles, corniche à godrons. Autour du texte, une moulure d'or à filets et petit rinceau,
couronnée en haut d'une corniche à denticules et oves.

Relevé sur la photo (côté ≈ 315 px) : haut ≈ 1,4 côté, pied ≈ 1,75 côté ; la moulure fait le tiers du côté.
Le candélabre unique de chaque côté de l'original devient une frise de vases fleuris superposés, et son pied le coin
du pied. L'original n'est pas tout à fait symétrique ; le moteur reflète les côtés, on garde le même dessin des deux.
"""
import math

from commun import (Path, affine, boite, cadre, circle, diamond, ellipse, fill, frise, leaf, piece, rect, smooth,
                    stroke, teardrop)

ORDRE = 900

INKS = {
    "ground": "#5B7BA5",     # bleu du fond
    "green": "#6C9256",      # acanthe
    "greenDark": "#3F6038",  # contour des feuilles
    "greenLight": "#AFC98C", # nervures
    "red": "#D56F5C",        # églantines, gemmes, perles rouges
    "redDark": "#93402F",    # contour des fleurs et des gemmes
    "white": "#F6F0DF",      # lys et perles
    "gold": "#C6A86C",       # or
    "goldLight": "#E2CC92",  # or éclairé : listels, rehauts
    "goldDark": "#9A8158",   # or dans l'ombre : retraits des moulures
    "copper": "#B38157",     # filet cuivré au ras du texte
    "outline": "#4E3F2B",    # contour brun des ors
}

PALETTES = {
    "GREEN":  {"ground": "#3F6A52", "green": "#9DBB68", "greenDark": "#4E6E33", "greenLight": "#D3E2A8"},
    "VIOLET": {"ground": "#6A5893", "greenLight": "#C3D3A2"},
    "RED":    {"ground": "#9B3A33", "red": "#EAB75E", "redDark": "#8A5A20"},
    "ROSE":   {"ground": "#C08397", "red": "#A63650", "redDark": "#6A1F30"},
    "WHITE":  {"ground": "#ECE3CD", "white": "#FFFDF7", "green": "#7FA266"},
    "BLACK":  {"ground": "#2B2927", "gold": "#A8A296", "goldLight": "#CFCBC1", "goldDark": "#7A766E",
               "copper": "#8F8A80", "red": "#8C8379", "redDark": "#55504A", "green": "#6F6D68",
               "greenDark": "#45433F", "greenLight": "#9C9993"},
}

# --------------------------------------------------------------------------- #
# Proportions. Les bandes sont découpées en zones d'épaisseur fixe à l'écran (dp), pour que la moulure du texte et
# le filet extérieur gardent la même épaisseur sur les quatre côtés. Tout se dessine en unités de côté (100 = S) ;
# les bandes haute et du pied sont ensuite réduites de S/T et S/B.
# --------------------------------------------------------------------------- #
S, T, B = 24.0, 30.0, 42.0  # côtés, haut, pied (dp)
FILET = 1.5                 # filet d'or extérieur
CADRE = 7.0                 # moulure d'or autour du texte
BLEU = 17.0                 # côtés : filet + champ bleu du candélabre
RINCEAU = 16.0              # haut : filet + champ bleu du rinceau ; puis la corniche, puis la moulure
SOCLE = 5.5                 # pied : filet + plinthe basse ; puis le panneau bleu
CORNICHE_PIED = 4.5         # pied : corniche du socle, sous la moulure
TRAIT = 0.3                 # filets sombres


def su(dp):
    """dp → unités de côté."""
    return dp / S * 100.0


HT, HB = su(T), su(B)       # profondeur des bandes haute et du pied, en unités de côté
Y_FIL = su(FILET)
X_BLEU = su(BLEU)           # côtés : fin du champ bleu, début de la moulure
Y_RINCEAU = su(RINCEAU)     # haut : fin du champ bleu, début de la corniche
Y_CORN = HT - su(CADRE)     # haut : fin de la corniche, début de la moulure
Y_SOCLE = su(SOCLE)         # pied : haut de la plinthe basse
Y_PANNEAU = HB - su(CADRE + CORNICHE_PIED)   # pied : haut du panneau bleu
Y_CADRE_PIED = HB - su(CADRE)                # pied : début de la moulure
VC = (Y_FIL + X_BLEU) / 2   # côtés : axe du candélabre

# couches de la moulure, depuis le bord du texte (dp) : (début, fin, encre) ; un petit rinceau court sur la 5ᵉ
COUCHES = [(0.0, 0.9, "copper"), (0.9, 2.1, "gold"), (2.1, 3.2, "goldLight"), (3.2, 3.8, "gold"),
           (3.8, 6.4, "gold"), (6.4, CADRE, "goldDark")]
TRAITS = [0.9, 2.1, 3.2, 3.8, 6.4, CADRE]
PETIT_RINCEAU = (3.8, 6.4)

# corniche du haut, depuis son bord supérieur (dp) : listel, denticules, oves et dards, denticules, ombre
CORNICHE = [(0.0, 0.6, "goldLight"), (0.6, 2.3, "gold"), (2.3, 2.7, "goldLight"), (2.7, 5.1, "gold"),
            (5.1, 5.5, "goldLight"), (5.5, 6.7, "gold"), (6.7, 7.0, "goldDark")]
DENTICULE = su(1.75)        # pas des denticules
OVE = su(4.9)               # pas des oves

OL = 1.3                    # contour des ors
OLF = 1.1                   # contour des feuilles et des fleurs


class Dessin:
    """Opérations dessinées à travers une transformation : on dessine en unités de côté dans un repère local, `t`
    passe au repère du module et `z` est son échelle (épaisseur des traits)."""

    def __init__(self, t=None, z=1.0):
        self.ops, self.t, self.z = [], t, z

    def _p(self, path):
        return path.transformed(self.t) if self.t else path

    def fill(self, ink, path):
        self.ops.append(fill(ink, self._p(path)))

    def stroke(self, ink, path, w, round=False):
        self.ops.append(stroke(ink, self._p(path), w * self.z, round))

    def outlined(self, ink, path, ol=OL, oink="outline"):
        p = self._p(path)
        self.ops += [stroke(oink, p, 2 * ol * self.z), fill(ink, p)]


def echelle(z):
    return Dessin(lambda p: (p[0] * z, p[1] * z), z)


# --------------------------------------------------------------------------- #
# Formes
# --------------------------------------------------------------------------- #
def galbe(profile):
    """Silhouette symétrique (vase, balustre) d'après son profil droit [(demi-largeur, y)], d'un bout à l'autre."""
    right = smooth(profile)
    left = smooth([(-x, y) for x, y in reversed(profile)])
    p = Path()
    p.cmds = right.cmds + [("L", left.cmds[0][1])] + left.cmds[1:] + [("Z",)]
    return p


def acanthe(L, W, lobes=3, bend=0.0, lobe=None, lobe_w=None):
    """Feuille d'acanthe pointant vers +x, base en (0, 0) : une lame et, de chaque côté, des lobes inclinés vers la
    pointe, de plus en plus petits. `bend` courbe la nervure (fraction de L, vers +y) ; `lobe` et `lobe_w` donnent
    la taille du premier lobe (par défaut proportionnelle à la feuille)."""
    lobe = 0.42 * L if lobe is None else lobe
    lobe_w = 0.36 * W if lobe_w is None else lobe_w
    p = Path()
    p.extend(leaf(L, 0.5 * W, bend=bend))
    b = bend * L
    for i in range(lobes):
        t = 0.12 + 0.62 * i / lobes
        # point et tangente de la nervure (la cubique de leaf)
        x = 3 * t * (1 - t) ** 2 * 0.3 * L + 3 * t * t * (1 - t) * 0.75 * L + t ** 3 * L
        y = b * (1 - (1 - t) ** 3)
        dx = 3 * (1 - t) ** 2 * 0.3 * L + 6 * (1 - t) * t * 0.45 * L + 3 * t * t * 0.25 * L
        dy = 3 * b * (1 - t) ** 2
        a = math.degrees(math.atan2(dy, dx))
        k = 1 - 0.5 * i / lobes
        for sg in (-1, 1):
            p.extend(leaf(lobe * k, lobe_w * k, bend=-0.12 * sg).transformed(affine(x, y, rot=a + sg * 40)))
    return p


def feuille(g, x, y, rot, L, W, lobes=3, bend=0.0, lobe=None, lobe_w=None):
    """Acanthe verte à contour sombre et nervure claire."""
    t = affine(x, y, rot=rot)
    g.outlined("green", acanthe(L, W, lobes, bend, lobe, lobe_w).transformed(t), OLF, "greenDark")
    rib = smooth([(0.06 * L, 0), (0.5 * L, bend * L * 0.85), (0.88 * L, bend * L)])
    g.stroke("greenLight", rib.transformed(t), 1.6, round=True)


def eglantine(g, cx, cy, r, rot=-90):
    """Églantine rouge à cinq pétales, cœur d'or."""
    p = Path()
    for k in range(5):
        p.extend(teardrop(r, 0.95 * r).transformed(affine(cx, cy, rot=rot + 72 * k)))
    g.outlined("red", p, 0.9, "redDark")
    g.fill("goldLight", circle(cx, cy, 0.26 * r))


def lys(g, cx, cy, r, rot=-90):
    """Lys blanc vu de face : six pétales pointus, cœur d'or."""
    p = Path()
    for k in range(6):
        p.extend(leaf(r * (1.0 if k % 2 == 0 else 0.86), 0.52 * r).transformed(affine(cx, cy, rot=rot + 60 * k)))
    g.outlined("white", p, 0.8)
    g.fill("gold", circle(cx, cy, 0.2 * r))


def bouton(g, x, y, L, rot=-90):
    """Bouton rouge dans son calice vert, pointe vers `rot`."""
    g.outlined("red", teardrop(L, 0.75 * L).transformed(affine(x, y, rot=rot)), 0.8, "redDark")
    cal = Path()
    for s in (-1, 1):
        cal.extend(leaf(0.55 * L, 0.3 * L).transformed(affine(x, y, rot=rot + s * 28)))
    g.outlined("green", cal, 0.7, "greenDark")


def perle(g, x, y, r=3.6, ink="white"):
    g.outlined(ink, circle(x, y, r), 0.9)


def tige(g, points, w, ink="green", oink="greenDark", ol=OLF):
    p = smooth(points)
    g.stroke(oink, p, w + 2 * ol, round=True)
    g.stroke(ink, p, w, round=True)


def ruban_or(g, path, w):
    g.stroke("outline", path, w + 2 * OL, round=True)
    g.stroke("gold", path, w, round=True)


def gemme(g, x, y, w, h):
    """Chaton d'or en losange, gemme rouge et son reflet."""
    g.outlined("gold", diamond(x, y, w, h))
    g.outlined("red", diamond(x, y, 0.52 * w, 0.54 * h), 0.8, "redDark")
    g.fill("white", circle(x - 0.07 * w, y - 0.08 * h, 0.07 * w))


def volute(cx, cy, R, r0, a_start, turns, cw=True, lead=None):
    """Volute : spirale de rayon R → r0 autour de (cx, cy), partant de l'angle a_start (degrés), dans le sens des
    angles décroissants si `cw` ; `lead` : point d'attache qui la précède."""
    n = max(6, int(turns * 14))
    pts = [lead] if lead else []
    for i in range(n + 1):
        t = i / n
        a = math.radians(a_start + (-1 if cw else 1) * 360 * turns * t)
        r = R + (r0 - R) * t
        pts.append((cx + r * math.cos(a), cy + r * math.sin(a)))
    return smooth(pts)


# --------------------------------------------------------------------------- #
# Bandes droites, dessinées le long de x, profondeur y de 0 (bord extérieur) à H (côté texte)
# --------------------------------------------------------------------------- #
def h_strip(g, x0, x1, y0, y1, ink):
    g.fill(ink, rect(x0, min(y0, y1), x1 - x0, abs(y1 - y0)))


def h_line(g, x0, x1, y, w=su(TRAIT), ink="outline"):
    g.stroke(ink, Path().M((x0, y)).L((x1, y)), w)


def rangee(x0, x1, pas):
    """Positions d'une rangée régulière (denticules, oves) : un nombre entier d'éléments de x0 à x1."""
    n = max(1, round((x1 - x0) / pas))
    step = (x1 - x0) / n
    return [x0 + (i + 0.5) * step for i in range(n)], step


def petit_rinceau(g, x0, x1, y0, y1):
    """Petit rinceau courant de la moulure : une onde et un point dans chaque creux, un nombre entier d'ondes."""
    n = max(1, round((x1 - x0) / su(3.2)))
    lam = (x1 - x0) / n
    yc, a = (y0 + y1) / 2, 0.32 * abs(y1 - y0)
    pts = [(x0 + lam * i / 8, yc + a * math.sin(2 * math.pi * i / 8)) for i in range(8 * n + 1)]
    g.stroke("outline", smooth(pts), su(0.22))
    dots = Path()
    r = 0.12 * abs(y1 - y0)
    for i in range(n):
        dots.extend(circle(x0 + lam * (i + 0.25), yc - 0.7 * a, r))
        dots.extend(circle(x0 + lam * (i + 0.75), yc + 0.7 * a, r))
    g.fill("outline", dots)


def moulure(g, x0, x1, H):
    """Moulure du texte, de x0 à x1, contre le bord y = H : filets d'or, petit rinceau, filet cuivré."""
    for d0, d1, ink in COUCHES:
        h_strip(g, x0, x1, H - su(d1), H - su(d0), ink)
    r0, r1 = PETIT_RINCEAU
    petit_rinceau(g, x0, x1, H - su(r1 - 0.3), H - su(r0 + 0.3))
    for d in TRAITS:
        h_line(g, x0, x1, H - su(d))


def filet_ext(g, x0, x1):
    h_strip(g, x0, x1, 0, Y_FIL, "gold")
    h_strip(g, x0, x1, 0, su(0.45), "goldLight")
    h_line(g, x0, x1, Y_FIL)


def corniche(g, x0, x1, y0):
    """Corniche du haut à partir de y0 : listel, denticules, oves et dards, denticules."""
    for d0, d1, ink in CORNICHE:
        h_strip(g, x0, x1, y0 + su(d0), y0 + su(d1), ink)
    dents = Path()
    xs, _ = rangee(x0, x1, DENTICULE)
    for x in xs:
        dents.extend(rect(x - su(0.28), y0 + su(0.95), su(0.56), su(1.15)))
        dents.extend(rect(x - su(0.24), y0 + su(5.8), su(0.48), su(0.8)))
    g.fill("goldDark", dents)
    xs, step = rangee(x0, x1, OVE)
    oves, dards = Path(), Path()
    for x in xs:
        oves.extend(ellipse(x, y0 + su(4.1), su(0.8), su(0.8)))
        dards.M((x + step / 2, y0 + su(3.0))).L((x + step / 2, y0 + su(4.9)))
        dards.extend(Path().M((x - su(1.5), y0 + su(2.9))).C((x - su(1.5), y0 + su(5.2)), (x + su(1.5), y0 + su(5.2)),
                                                            (x + su(1.5), y0 + su(2.9))))
    g.stroke("goldDark", dards, su(0.22))
    g.fill("outline", oves)
    for d in (0.6, 2.3, 2.7, 5.1, 5.5, 6.7, 7.0):
        h_line(g, x0, x1, y0 + su(d), su(0.2))


def plinthe_basse(g, x0, x1):
    """Plinthe du socle, au ras du filet extérieur : ressaut sombre, puis une rangée de rais-de-cœur."""
    y0, y1 = Y_FIL, Y_SOCLE
    h_strip(g, x0, x1, y0, y1, "gold")
    h_strip(g, x0, x1, y0, y0 + su(0.9), "goldDark")
    xs, step = rangee(x0, x1, su(2.4))
    rais = Path()
    for x in xs:
        rais.M((x - 0.36 * step, y1 - su(0.6))).C((x - 0.36 * step, y0 + su(1.2)), (x + 0.36 * step, y0 + su(1.2)),
                                                  (x + 0.36 * step, y1 - su(0.6)))
    g.stroke("outline", rais, su(0.24))
    for y in (y0, y0 + su(0.9), y1):
        h_line(g, x0, x1, y)


def corniche_pied(g, x0, x1):
    """Corniche du socle sous la moulure : ressaut sombre, godrons pendants, listel clair."""
    y0, y1 = Y_PANNEAU, Y_CADRE_PIED
    h_strip(g, x0, x1, y0, y1, "gold")
    h_strip(g, x0, x1, y0, y0 + su(0.8), "goldDark")
    h_strip(g, x0, x1, y1 - su(0.9), y1, "goldLight")
    xs, step = rangee(x0, x1, su(2.6))
    godrons = Path()
    top = y1 - su(0.9)
    for x in xs:
        godrons.M((x - step / 2, top)).C((x - step / 2, y0 + su(1.0)), (x + step / 2, y0 + su(1.0)), (x + step / 2, top))
    g.stroke("outline", godrons, su(0.24))
    for y in (y0, y0 + su(0.8), top, y1):
        h_line(g, x0, x1, y)


# --------------------------------------------------------------------------- #
# Côtés : candélabre de vases fleuris (u vers le bas de la page, v vers le texte)
# --------------------------------------------------------------------------- #
P_COTE = 255.0


def debout():
    """Dessin « debout » (x vers le texte depuis l'axe du candélabre, y vers le bas) → repère de la bande latérale."""
    return Dessin(lambda p: (p[1], VC + p[0]))


def etage(g):
    """Un étage du candélabre, de y = -P_COTE (haut) à y = 0 (bas) : bouquet d'acanthe avec un lys et une
    églantine, vase à anses, collier, dauphins affrontés, gemme entre deux fils de perles, tige et perle."""
    y0 = -129.0
    for x, rot, L, W, bend in ((-3, -90 - 50, 46, 24, 0.06), (3, -90 + 50, 46, 24, -0.06),
                               (-2, -90 - 20, 88, 32, 0.05), (2, -90 + 20, 88, 32, -0.05), (0, -90, 125, 34, 0.0)):
        feuille(g, x, y0, rot, L, W, bend=bend)
    tige(g, [(-2, -160), (-9, -184), (-16, -202)], 2.4)
    tige(g, [(2, -150), (10, -168), (16, -180)], 2.4)
    lys(g, -16, -206, 13)
    eglantine(g, 16, -183, 11)
    # vase : pied en y = -74, lèvre en y = -132
    for s in (-1, 1):
        ruban_or(g, smooth([(s * 7, -121), (s * 17, -130), (s * 22, -124), (s * 20, -114), (s * 15, -110)]), 3.0)
    g.outlined("gold", galbe([(9, -74), (8.5, -77), (4, -80), (7, -84), (15, -93), (17, -101), (14, -111), (7.5, -119),
                              (5.5, -123), (11, -128), (11, -132)]))
    g.stroke("goldDark", smooth([(7, -90), (10.5, -100), (8, -109)]), 2.6, round=True)
    g.stroke("goldLight", smooth([(-8, -91), (-11.5, -100), (-9, -108)]), 2.0, round=True)
    h_line(g, -14, 14, -111.5, 1.2)
    g.outlined("gold", ellipse(0, -71, 10, 3.6))
    # dauphins affrontés, la queue enroulée vers le dehors
    for s in (-1, 1):
        ruban_or(g, smooth([(s * 3, -46), (s * 13, -50), (s * 18, -58), (s * 19, -66), (s * 24, -70), (s * 27, -65)]), 4.6)
    perle(g, 0, -56, 3.8)
    # gemme et perles ; de chaque perle pend un fil de perles rouges et blanches coupé d'un losange d'or
    for s in (-1, 1):
        for i, ink in enumerate(("red", "white", "gold", "red")):
            y = -22 + 7.2 * i
            if ink == "gold":
                g.outlined("gold", diamond(s * 19, y, 6, 7.5), 0.9)
            else:
                perle(g, s * 19, y, 3.2, ink)
        perle(g, s * 19, -30, 4.0)
    gemme(g, 0, -30, 28, 34)
    # tige de raccord vers l'étage du dessous
    g.outlined("gold", rect(-2.6, -14, 5.2, 15))
    perle(g, 0, -6, 3.8)


def module_cote():
    P = P_COTE
    g = debout()
    etage(g)
    o = Dessin()
    filet_ext(o, -P - 1, 1)
    moulure(o, -P - 1, 1, 100.0)
    return frise(P, g.ops, o.ops, phase=0.0)


# --------------------------------------------------------------------------- #
# Haut : rinceau d'acanthe vers le milieu, églantines ; corniche ; moulure (y vers le texte)
# --------------------------------------------------------------------------- #
P_HAUT = 175.0
Y_TIGE = 40.0               # tige du rinceau


def bandes_haut(g, x0, x1):
    filet_ext(g, x0, x1)
    corniche(g, x0, x1, Y_RINCEAU)
    moulure(g, x0, x1, HT)


def module_haut():
    z = S / T
    P = P_HAUT
    g = echelle(z)
    tige(g, [(-P / 2 - 2, Y_TIGE), (P / 2 + 2, Y_TIGE)], 4.0)
    # une petite feuille retombante sous la tige, entre deux grandes, rend la guirlande continue
    feuille(g, 52, Y_TIGE + 2, 22, 52, 26, lobes=2, bend=0.12)
    feuille(g, -92, Y_TIGE + 3, -4, 185, 40, lobes=5, bend=0.04, lobe=48, lobe_w=27)
    tige(g, [(30, Y_TIGE - 4), (42, 26), (55, 19)], 2.4)
    bouton(g, 55, 18, 17, rot=-75)
    eglantine(g, -14, 21, 15)
    o = echelle(z)
    bandes_haut(o, -P / 2 - 1, P / 2 + 1)
    return frise(P * z, g.ops, o.ops, phase=0.5)


def centre_haut():
    """Bouquet de lys au milieu du haut, entre les deux moitiés du rinceau."""
    z = S / T
    W = 160.0
    g = echelle(z)
    tige(g, [(-W / 2 - 2, Y_TIGE), (W / 2 + 2, Y_TIGE)], 4.0)
    for s in (-1, 1):
        feuille(g, s * 10, 50, 90 - s * 72, 48, 22, lobes=2)
        tige(g, [(s * 44, Y_TIGE), (s * 56, 26), (s * 62, 19)], 2.2)
        bouton(g, s * 62, 18, 14, rot=-90 + s * 30)
        lys(g, s * 36, 30, 17, rot=-90 + s * 10)
    lys(g, 0, 36, 24)
    bandes_haut(g, -W / 2 - 1, W / 2 + 1)
    return piece(W * z, g.ops)


# --------------------------------------------------------------------------- #
# Pied : plinthe, panneau bleu à rinceau d'or, corniche à godrons, moulure (y vers le texte)
# --------------------------------------------------------------------------- #
P_PIED = 200.0
Y_MID = (Y_SOCLE + Y_PANNEAU) / 2
AMP = 24.0


def onde_pied(x):
    return Y_MID + AMP * math.sin(2 * math.pi * x / P_PIED)


def bandes_pied(g, x0, x1):
    filet_ext(g, x0, x1)
    plinthe_basse(g, x0, x1)
    corniche_pied(g, x0, x1)
    moulure(g, x0, x1, HB)


def feuille_or(g, x, y, rot, L, W, lobes=2, bend=0.0):
    """Acanthe d'or du socle, nervure dans l'ombre."""
    t = affine(x, y, rot=rot)
    g.outlined("gold", acanthe(L, W, lobes, bend).transformed(t))
    g.stroke("goldDark", smooth([(0.1 * L, 0), (0.5 * L, bend * L * 0.85), (0.85 * L, bend * L)]).transformed(t), 2.0,
             round=True)


def rinceau_pied(g):
    """Une période du rinceau d'or : tige ondulée, une volute dans chaque creux (églantine en haut, feuille verte en
    bas), une gaine d'acanthe d'or à chaque départ de volute."""
    P = P_PIED
    xs = [P * (i / 12 - 0.5) for i in range(13)]
    pts = [(xs[0] - 4, onde_pied(xs[0] - 4))] + [(x, onde_pied(x)) for x in xs] + [(xs[-1] + 4, onde_pied(xs[-1] + 4))]
    xa, xb = -P / 4 - 38, P / 4 + 38
    up = volute(-P / 4 + 8, Y_MID + 18, 24, 5, 180, 1.1, cw=True, lead=(xa, onde_pied(xa)))
    dn = volute(P / 4 - 8, Y_MID - 18, 24, 5, 0, 1.1, cw=True, lead=(xb, onde_pied(xb)))
    for p in (up, dn):
        ruban_or(g, p, 6.0)
    ruban_or(g, smooth(pts), 8.0)
    eglantine(g, -P / 4 + 8, Y_MID + 18, 12)
    feuille(g, P / 4 - 8, Y_MID - 14, -90 + 25, 30, 20, lobes=2)
    # gaines d'acanthe d'or aux départs des volutes, couchées le long de la tige
    feuille_or(g, xa - 8, onde_pied(xa - 8), 60, 46, 30, bend=-0.08)
    feuille_or(g, xb + 8, onde_pied(xb + 8), -120, 46, 30, bend=-0.08)


def module_pied():
    z = S / B
    P = P_PIED
    g = echelle(z)
    rinceau_pied(g)
    o = echelle(z)
    bandes_pied(o, -P / 2 - 1, P / 2 + 1)
    return frise(P * z, g.ops, o.ops, phase=0.0)


def centre_pied():
    """Cartouche du socle : arc de deux grandes volutes d'or qui se rejoignent en haut et s'enroulent vers le dehors
    en bas ; dedans, une gemme rouge dans une couronne de perles, entre deux feuilles vertes."""
    z = S / B
    W = 250.0
    g = echelle(z)
    yb = Y_SOCLE + 24
    for s in (-1, 1):
        # la tige du rinceau finit en volute contre l'arc
        ruban_or(g, volute(s * 104, Y_MID + 12, 17, 4, 180 if s < 0 else 0, 1.1, cw=(s < 0),
                           lead=(s * (W / 2 + 3), Y_MID - 4)), 5.5)
        bras = smooth([(s * 4, Y_PANNEAU - 9), (s * 28, Y_PANNEAU - 9), (s * 48, Y_MID + 26), (s * 54, Y_MID),
                       (s * 50, yb + 4), (s * 54, yb - 8)])
        bout = volute(s * 68, yb, 14, 3.5, 180 if s > 0 else 0, 1.05, cw=(s < 0))
        ruban_or(g, bras, 10.0)
        ruban_or(g, bout, 7.0)
        feuille_or(g, s * 60, Y_MID + 14, -20 if s > 0 else 200, 40, 26, bend=-0.06 * s)
        feuille(g, s * 26, Y_MID - 16, 90 - s * 45, 30, 18, lobes=2)
    feuille_or(g, 0, Y_SOCLE + 30, 90, 30, 24)
    g.outlined("gold", teardrop(24, 22).transformed(affine(0, Y_PANNEAU - 6, rot=-90)))
    g.outlined("gold", rect(-14, Y_PANNEAU - 11, 28, 6))
    for k in range(8):
        a = math.radians(22.5 + 45 * k)
        perle(g, 25 * math.cos(a), Y_MID + 4 + 29 * math.sin(a), 4.4)
    gemme(g, 0, Y_MID + 4, 34, 42)
    bandes_pied(g, -W / 2 - 1, W / 2 + 1)
    return piece(W * z, g.ops)


# --------------------------------------------------------------------------- #
# Coins
# --------------------------------------------------------------------------- #
def moulure_coin(g, W, H):
    """Moulure du texte tournant à l'angle (W, H) de la case : couches en L, filets à onglet."""
    for d0, d1, ink in COUCHES:
        a, b = su(d0), su(d1)
        g.fill(ink, Path().M((W - b, H + 1)).L((W - b, H - b)).L((W + 1, H - b)).L((W + 1, H - a)).L((W - a, H - a))
               .L((W - a, H + 1)).Z())
    r0, r1 = PETIT_RINCEAU
    m = su((r0 + r1) / 2)
    perle(g, W - m, H - m, su(0.75), "goldLight")
    for d in TRAITS:
        a = su(d)
        g.stroke("outline", Path().M((W - a, H + 1)).L((W - a, H - a)).L((W + 1, H - a)), su(TRAIT))
    g.stroke("outline", Path().M((W - su(CADRE), H - su(CADRE))).L((W, H)), su(0.2))


def filet_coin(g, W, H):
    g.fill("gold", Path().M((0, 0)).L((W + 1, 0)).L((W + 1, Y_FIL)).L((Y_FIL, Y_FIL)).L((Y_FIL, H + 1)).L((0, H + 1)).Z())
    g.fill("goldLight", Path().M((0, 0)).L((W + 1, 0)).L((W + 1, su(0.45))).L((su(0.45), su(0.45))).L((su(0.45), H + 1))
           .L((0, H + 1)).Z())
    g.stroke("outline", Path().M((W + 1, Y_FIL)).L((Y_FIL, Y_FIL)).L((Y_FIL, H + 1)), su(TRAIT))


def module_coin_haut():
    """Coin haut gauche (100 × HT) : la tige du candélabre monte et tourne vers le milieu en rinceau ; lys blanc dans
    l'angle ; bout de la corniche en saillie sur le bleu ; moulure à onglet."""
    W, H = 100.0, HT
    g = Dessin()
    tige(g, [(W + 2, Y_TIGE), (72, Y_TIGE), (52, 46), (40, 62), (37, 92), (VC, H + 2)], 4.0)
    feuille(g, 37, 76, -98, 56, 28, bend=0.05)
    feuille(g, 44, 50, -12, 56, 34, bend=0.05)
    eglantine(g, 19, 104, 11)
    lys(g, 21, 21, 15)
    bouton(g, 62, 17, 13, rot=-80)
    # corniche en saillie de 1,4 dp sur la moulure
    x0 = X_BLEU - su(1.4)
    corniche(g, x0, W + 1, Y_RINCEAU)
    g.stroke("outline", Path().M((x0, Y_RINCEAU)).L((x0, Y_CORN)), su(TRAIT))
    moulure_coin(g, W, H)
    filet_coin(g, W, H)
    return boite(g.ops, W, H)


def candelabre_pied(g):
    """Pied du candélabre (repère « debout », y < 0 vers le haut, base en y = -Y_SOCLE) : base à volutes, urne
    d'acanthe, coupe cannelée, balustre et fils de perles, lyre, tige qui rejoint l'étage du dessus."""
    yb = -Y_SOCLE
    g.outlined("gold", galbe([(22, yb), (21, yb - 3), (14, yb - 7), (11, yb - 10), (13, yb - 13), (9, yb - 17)]))
    for s in (-1, 1):
        g.stroke("outline", volute(s * 18, yb - 7, 4.5, 1, 90, 0.9, cw=(s > 0)), 1.3, round=True)
    g.outlined("gold", ellipse(0, yb - 18, 11, 3.5))
    g.outlined("gold", galbe([(8, yb - 20), (10, yb - 28), (14, yb - 39), (16.5, yb - 48), (18.5, yb - 52), (17.5, yb - 55)]))
    for s in (-1, 1):
        ruban_or(g, smooth([(s * 12, yb - 51), (s * 21, yb - 58), (s * 25, yb - 52), (s * 22, yb - 47)]), 3.4)
    g.stroke("goldDark", smooth([(8, yb - 29), (11, yb - 40), (12, yb - 49)]), 2.2, round=True)
    g.outlined("gold", ellipse(0, yb - 58, 13, 3.5))
    g.outlined("gold", galbe([(5.5, yb - 61), (9, yb - 68), (13, yb - 77), (15, yb - 84), (15, yb - 87)]))
    flutes = Path()
    for x in (-7, -2.3, 2.3, 7):
        flutes.M((x * 0.6, yb - 63)).L((x, yb - 85))
    g.stroke("outline", flutes, 1.0)
    # balustre et fils de perles
    g.outlined("gold", rect(-5, yb - 140, 10, 54))
    g.outlined("gold", ellipse(0, yb - 101, 7.5, 2.8))
    g.outlined("gold", ellipse(0, yb - 123, 7.5, 2.8))
    perle(g, 0, yb - 112, 3.4)
    for s in (-1, 1):
        x = s * 23
        for i, ink in enumerate(("red", "red", "gold", "white", "white", "gold", "red")):
            y = yb - 136 + 7.5 * i
            if ink == "gold":
                g.outlined("gold", diamond(x, y, 6, 7.5), 0.9)
            else:
                perle(g, x, y, 3.3, ink)
        ruban_or(g, smooth([(s * 2, yb - 140), (s * 13, yb - 143), (s * 21, yb - 151), (s * 18, yb - 158),
                            (s * 12, yb - 156)]), 3.8)
    g.outlined("gold", rect(-2.6, -HB - 2, 5.2, HB + 2 + yb - 138))


def module_coin_pied():
    """Coin du pied (100 × HB, y vers le texte) : pied du candélabre sur la plinthe, pilastre du socle, bout de la
    corniche du socle, moulure à onglet."""
    W, H = 100.0, HB
    g = Dessin(lambda p: (VC + p[0], -p[1]))
    candelabre_pied(g)
    o = Dessin()
    # pilastre du socle : bloc d'or et panneau en creux
    o.outlined("gold", rect(X_BLEU, Y_SOCLE, W + 2 - X_BLEU, Y_PANNEAU - Y_SOCLE))
    o.stroke("goldDark", rect(X_BLEU + 5, Y_SOCLE + 6, W - X_BLEU - 9, Y_PANNEAU - Y_SOCLE - 12), 1.6)
    o.outlined("gold", diamond((X_BLEU + W) / 2, Y_MID, 9, 14), 0.9)
    plinthe_basse(o, 0, W + 1)
    x0 = X_BLEU - su(1.0)
    corniche_pied(o, x0, W + 1)
    o.stroke("outline", Path().M((x0, Y_PANNEAU)).L((x0, Y_CADRE_PIED)), su(TRAIT))
    moulure_coin(o, W, H)
    filet_coin(o, W, H)
    return boite(g.ops + o.ops, W, H)


def spec():
    return cadre(
        name="renaissance",
        title="Rinceaux et candélabres",
        source="paroissien chromolithographié, fin du XIXᵉ siècle, cadre sans texte « rinceaux et candélabres »",
        inks=INKS,
        palettes=PALETTES,
        layout={"margin": 12, "top": T, "side": S, "bottom": B, "textPadding": 9},
        top=module_haut(),
        side=module_cote(),
        bottom=module_pied(),
        centers={"top": centre_haut(), "bottom": centre_pied()},
        corners={"top": module_coin_haut(), "bottom": module_coin_pied()},
    )
