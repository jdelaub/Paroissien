"""
Cadre « Messe de mariage » (p.167) : fond vert sombre ; tout autour, des rinceaux d'acanthe aux feuilles pastel (rose,
bleu clair, vert clair) sur de fines tiges d'or, qui naissent au milieu de la bande haute (deux boucles d'or nouées sous
un fleuron rose posé sur la baguette) et au milieu du pied (fleur de lys rose entre deux feuilles vertes) ; baguettes
d'or croisées en treillis aux quatre coins, qui débordent et finissent en boutons ; cases d'angle à quatre pétales
gris bleu autour d'une fleurette verte, et au pied, plus hautes, un losangé gris bleu à fleurs de lys d'or.
"""
import math

from commun import (Path, affine, arc, boite, cadre, circle, ellipse, fill, fleur_de_lis, frise, outlined, piece, polygon,
                    quatrefoil, rect, smooth, stroke, strand, teardrop)

ORDRE = 167

INKS = {
    "ground": "#475A4C",     # vert sombre du fond (mesuré #485648)
    "gold": "#C9AB70",       # or des baguettes et des tiges (mesuré #BBA77C à #D4B985, remonté)
    "goldLight": "#ECDDB0",  # reflet au milieu des baguettes
    "pink": "#EFB6A0",       # feuilles roses (mesuré #F0B8A3)
    "pinkDark": "#C27A68",   # nervures des feuilles roses
    "blue": "#97AFBA",       # feuilles bleu clair (mesuré #708384, gris bleu terne en photo)
    "blueDark": "#5B7888",   # nervures des feuilles bleues
    "green": "#9DB485",      # feuilles vert clair (mesuré #96AC82)
    "greenDark": "#5F7B4C",  # nervures des feuilles vertes
    "slate": "#7F8795",      # pétales gris bleu des cases d'angle (mesuré #76766B à #656E6B)
    "slateDark": "#5A6170",  # losangé des cases du pied
    "outline": "#272C24",    # contour vert noir
}

# Le fond prend la couleur liturgique ; l'or, le rose et le gris bleu gardent l'identité du livre.
PALETTES = {
    "GREEN":  {"ground": "#3F5A45"},
    "VIOLET": {"ground": "#4C3B60", "green": "#B0B98F"},
    "RED":    {"ground": "#6E2B2A", "pink": "#F2C7A6", "pinkDark": "#B8754F"},
    "ROSE":   {"ground": "#94586A", "pink": "#F6D9CC", "pinkDark": "#B77A72"},
    "WHITE":  {"ground": "#E8DEC6", "slate": "#A9B1BC", "outline": "#5C5442"},
    "BLACK":  {"ground": "#2B2A27", "pink": "#BDB2A8", "blue": "#98A2A8", "green": "#A3A797"},
}

# Mise en page (dp) : pied 1,7 fois plus haut que les côtés, comme sur la page.
LAYOUT = {"margin": 14, "top": 18, "side": 18, "bottom": 30, "textPadding": 12}
PIED = 100.0 * LAYOUT["bottom"] / LAYOUT["side"]   # hauteur de la case du pied, en unités des côtés

# Baguettes (unités : 100 = épaisseur des côtés)
BAR_OUT = 15.0     # contour
BAR = 11.5         # or
BAR_LIGHT = 3.0    # reflet
OVERHANG = 13.0    # débord des baguettes aux coins
KNOB = 5.0         # longueur du bouton qui les termine

TIGE = 3.6         # tige d'or des rinceaux
TIGE_OUT = 1.1     # son contour
FEUILLE_OUT = 1.5  # contour des feuilles

# Teinte de nervure de chaque encre de feuille
NERVURE = {"pink": "pinkDark", "blue": "blueDark", "green": "greenDark"}


# --------------------------------------------------------------------------- #
# Feuille d'acanthe
# --------------------------------------------------------------------------- #
def croc(l, w, bend):
    """Lobe d'acanthe en croc : base large de `w` en x = 0, pointe en (l, bend·l), recourbée vers -y si bend < 0.
    Renvoie (silhouette, ligne médiane pour la nervure)."""
    b = bend * l
    up = [(0, -w / 2), (0.45 * l, -0.5 * w + 0.35 * b), (0.78 * l, 0.8 * b), (l, b)]
    lo = [(l, b), (0.72 * l, b + 0.45 * w), (0.38 * l, 0.62 * w), (0, w / 2)]
    bez = lambda P, t: tuple((1 - t) ** 3 * P[0][i] + 3 * (1 - t) ** 2 * t * P[1][i] + 3 * (1 - t) * t * t * P[2][i]
                             + t ** 3 * P[3][i] for i in range(2))
    mid = [tuple((bez(up, t)[i] + bez(lo, 1 - t)[i]) / 2 for i in range(2)) for t in (0.12, 0.4, 0.68)]
    return Path().M(up[0]).C(*up[1:]).C(*lo[1:]).Z(), smooth(mid)


def acanthe(L, courbe, lobes, W=0.2, base=0.24, volute=False):
    """Feuille d'acanthe pointant vers +x, base en (0, 0) : une nervure large qui s'enroule de `courbe` degrés vers
    -y, de plus en plus vers la pointe, garnie de lobes en croc (t le long de la nervure, longueur relative à L,
    angle par rapport à la nervure, positif côté +y) qui se recourbent vers la pointe. `volute` : la pointe s'enroule
    en crosse et finit arrondie. Renvoie (silhouette, nervure)."""
    phi = math.radians(courbe)
    heading = lambda t: -phi * (0.3 * t + 0.7 * t * t)
    N = 40
    spine = [((0.0, 0.0), 0.0)]
    x = y = 0.0
    for i in range(N):
        h = heading((i + 0.5) / N)
        x += L / N * math.cos(h)
        y += L / N * math.sin(h)
        spine.append(((x, y), heading((i + 1) / N)))
    at = lambda t: spine[min(N, max(0, round(t * N)))]

    def width(t):
        w = (1 - t) ** 0.8 if not volute else max(1 - t, 0.0) * 0.55 + 0.45 * (1 - t ** 4)
        return W * L / 2 * w * min(1.0, 0.5 + 3 * t)

    left, right = [], []
    for i in range(0, N + 1, 4):
        t = i / N
        (x, y), h = at(t)
        w = width(t)
        nx, ny = -math.sin(h), math.cos(h)
        left.append((x + nx * w, y + ny * w))
        right.append((x - nx * w, y - ny * w))
    if volute:
        (x, y), h = at(1.0)
        sil = smooth(left + [(x + math.cos(h) * width(0.97), y + math.sin(h) * width(0.97))] + right[::-1][:-1], closed=True)
    else:
        sil = smooth(left + right[::-1][1:], closed=True)
    veins = []
    for t, k, ang in lobes:
        (x, y), h = at(t)
        bend = -0.5 if ang > 0 else 0.4
        place = affine(x, y, rot=math.degrees(h) + ang)
        l = k * L
        lobe, mid = croc(l, base * L * (1 - 0.6 * t), bend)
        sil.extend(lobe.transformed(place))
        veins.append(mid.transformed(place))
    vein = smooth([at(t)[0] for t in (0.03, 0.3, 0.55, 0.8)])
    for v in veins:
        vein.extend(v)
    return sil, vein


# Trois formes, comme sur la page : la rose en main de doigts pointus, la verte en plume large, la bleue enroulée.
FORMES = {
    "pink":  dict(courbe=30, W=0.18, base=0.18, lobes=[(0.08, 0.52, 60), (0.14, 0.52, -76), (0.36, 0.46, 56),
                                                      (0.42, 0.42, -72), (0.64, 0.32, 50), (0.70, 0.26, -66)]),
    "green": dict(courbe=50, W=0.26, base=0.30, lobes=[(0.12, 0.50, 64), (0.20, 0.46, -80), (0.44, 0.40, 60),
                                                      (0.52, 0.32, -74)]),
    "blue":  dict(courbe=240, W=0.26, base=0.22, volute=True,
                  lobes=[(0.06, 0.40, 72), (0.12, 0.30, -84), (0.24, 0.36, 66), (0.42, 0.30, 60), (0.60, 0.22, 56)]),
}


def feuille(ink, L, t):
    """Opérations d'une feuille d'acanthe de couleur `ink`, longueur L, placée par la transformation t."""
    f = FORMES[ink]
    sil, vein = acanthe(L, f["courbe"], f["lobes"], f["W"], f["base"], f.get("volute", False))
    ops = outlined(ink, sil.transformed(t), outline=FEUILLE_OUT)
    ops.append(stroke(NERVURE[ink], vein.transformed(t), 1.6, round=True))
    return ops


def tige(path):
    return strand(path, width=TIGE, ink="gold", outline=TIGE_OUT)


def troncon(path, i, t0, t1):
    """Portion [t0, t1] du i-ème segment cubique d'un chemin (pour redessiner un passage dessus)."""
    segs = [c for c in path.cmds if c[0] != "M"]
    pts = [path.cmds[0][1]] + [c[-1] for c in path.cmds[1:]]
    p0, (_, p1, p2, p3) = pts[i], segs[i]
    lerp = lambda a, b, t: (a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t)

    def split(a, b, c, d, t):   # de Casteljau : (moitié avant, moitié après)
        ab, bc, cd = lerp(a, b, t), lerp(b, c, t), lerp(c, d, t)
        abc, bcd = lerp(ab, bc, t), lerp(bc, cd, t)
        m = lerp(abc, bcd, t)
        return (a, ab, abc, m), (m, bcd, cd, d)

    _, after = split(p0, p1, p2, p3, t0)
    seg, _ = split(*after, (t1 - t0) / (1 - t0))
    return Path().M(seg[0]).C(seg[1], seg[2], seg[3])


def miroir_v(p):
    return (p[0], 100.0 - p[1])


# --------------------------------------------------------------------------- #
# Frise : rinceau ondulant, trois ondes par période, six feuilles (chaque couleur une fois de chaque côté)
# --------------------------------------------------------------------------- #
def rinceau(lam, amp, L, root=0.45, angle=210.0, premiere=1.0, inverse=False):
    """Tige v = 50 + amp·sin(2πu/lam) ; une feuille par demi-onde, alternativement de chaque côté de la tige, qui
    pousse vers -u (vers le haut sur les côtés, vers les coins en haut et au pied) en travers de la bande.
    La bande s'arrête toujours à une jonction de période (coin, bord d'une pièce centrale) : là, la tige passe seule
    à mi-hauteur. Les feuilles débordent sur leurs voisines, sauf la première, raccourcie de `premiere` pour ne pas
    franchir la jonction (sinon sa pointe resterait coupée au bord de la pièce centrale).
    `inverse` retourne le module en profondeur (la tige descend vers l'extérieur aux jonctions)."""
    P = 3 * lam
    flip = miroir_v if inverse else (lambda p: p)
    stem_v = lambda u: 50 + amp * math.sin(2 * math.pi * u / lam)
    pts = [(u, stem_v(u)) for u in [i * lam / 16 for i in range(-2, 3 * 16 + 3)]]
    base = tige(smooth(pts).transformed(flip))
    over = []
    couleurs = ["green", "blue", "pink", "green", "blue", "pink"]
    for k in range(6):
        u = root * lam + k * lam / 2
        if k % 2 == 0:   # au-dessus d'un creux : la feuille monte de la tige vers le bord extérieur
            t = affine(u, stem_v(u), rot=angle)
        else:            # même feuille, en miroir de l'autre côté de la tige
            t0 = affine(u, 100 - stem_v(u), rot=angle)
            t = lambda p, t0=t0: miroir_v(t0(p))
        over += feuille(couleurs[k], L * (premiere if k == 0 else 1.0), lambda p, t=t: flip(t(p)))
    return frise(P, base, over, phase=0.0)


# --------------------------------------------------------------------------- #
# Pièces centrales
# --------------------------------------------------------------------------- #
ZOOM_TOP = 1.22                 # la pièce est dessinée sur une demi-largeur de 85, puis agrandie autour de v = 50
CW_TOP = 170.0 * ZOOM_TOP


def centre_haut(lam, amp):
    """Deux boucles d'or nouées au milieu de la bande haute, une feuille bleue enroulée de chaque côté.
    La tige du rinceau arrive au bord de la pièce à mi-hauteur, en descendant vers le côté texte."""
    ops = []
    h = 85.0
    slope = amp * 2 * math.pi / lam
    z = lambda p: (p[0] * ZOOM_TOP, 50 + (p[1] - 50) * ZOOM_TOP)
    for sgn in (-1, 1):
        m = (lambda p: z((-p[0], p[1]))) if sgn > 0 else z
        stem = smooth([(-h - 6, 50 - 6 * slope), (-h, 50), (-64, 70), (-40, 84), (-14, 84), (4, 72), (11, 52), (7, 30),
                       (-6, 17), (-22, 18), (-33, 28), (-34, 42), (-26, 50), (-18, 46)])
        ops += strand(stem.transformed(m), width=5.0, ink="gold", outline=1.3)
        ops += feuille("blue", 78, lambda p, m=m: m(affine(-28, 42, rot=184)(p)))
        if sgn < 0:
            boucle = stem.transformed(z)
    # entrelacs : en haut, la boucle gauche repasse sur la droite (contour plus court que l'or, sans liseré)
    ops.append(stroke("outline", troncon(boucle, 7, 0.15, 0.85), 5.0 + 2 * 1.3))
    ops.append(stroke("gold", troncon(boucle, 7, 0.05, 0.95), 5.0))
    return piece(CW_TOP, ops)


CW_FOOT = 120.0


def centre_pied(lam, amp):
    """Fleur de lys rose dressée vers le texte entre deux feuilles vertes, sur un nœud d'or ; les tiges du rinceau
    arrivent en descendant vers le bord extérieur et se rejoignent sous le nœud."""
    ops = []
    h = CW_FOOT / 2
    slope = amp * 2 * math.pi / lam
    for sgn in (-1, 1):
        m = (lambda p: (-p[0], p[1])) if sgn > 0 else (lambda p: p)
        stem = smooth([(-h - 6, 50 + 6 * slope), (-h, 50), (-44, 34), (-24, 24), (-8, 27), (0, 36)])
        ops += tige(stem.transformed(m))
        ops += feuille("green", 72, lambda p, m=m: m(affine(-5, 40, rot=180 - 30)((p[0], -p[1]))))
    ops += outlined("pink", fleur_de_lis(62).transformed(affine(0, 33, rot=180)), outline=FEUILLE_OUT)
    ops.append(stroke("pinkDark", smooth([(0, 46), (0, 64), (0, 80)]), 1.6, round=True))
    ops += outlined("gold", ellipse(0, 35, 10, 5.5), outline=1.2)
    return piece(CW_FOOT, ops)


# --------------------------------------------------------------------------- #
# Cases d'angle
# --------------------------------------------------------------------------- #
def fleurette(cx, cy, r):
    ops = outlined("green", quatrefoil(cx, cy, r), outline=1.3)
    ops.append(fill("greenDark", circle(cx, cy, 0.22 * r)))
    return ops


def coin_haut():
    """Quatre pétales gris bleu en sautoir, sur un losange d'or, fleurette verte au centre."""
    c = 50.0
    ops = [stroke("gold", polygon([(c, 6), (94, c), (c, 94), (6, c)]), 2.6)]
    petals = Path()
    for k in range(4):
        a = 45 + 90 * k
        d = (c + 26 * math.cos(math.radians(a)), c + 26 * math.sin(math.radians(a)))
        petals.extend(ellipse(0, 0, 25, 17).transformed(affine(*d, rot=a)))
    ops += outlined("slate", petals, outline=1.5)
    for k in range(4):
        a = 45 + 90 * k
        ops.append(stroke("slateDark", smooth([(16, 0), (30, 0), (42, 0)]).transformed(affine(c, c, rot=a)), 1.8, round=True))
    ops += fleurette(c, c, 17)
    return boite(ops)


def coin_pied():
    """Case du pied, plus haute : losangé gris bleu, fleurette verte au centre entre deux fleurs de lys d'or."""
    H = PIED
    c, m = 50.0, H / 2
    ops = [fill("slate", rect(0, 0, 100, H))]
    lat = Path()
    for k in range(-3, 4):   # deux familles de diagonales y = y0 ± 0,72 x, coupées à la case
        for s in (-0.72, 0.72):
            y0 = m + k * 52 - s * 50
            xs = sorted(min(100.0, max(0.0, (yy - y0) / s)) for yy in (0.0, H))
            if xs[1] - xs[0] > 1:
                lat.M((xs[0], y0 + s * xs[0])).L((xs[1], y0 + s * xs[1]))
    ops.append(stroke("slateDark", lat, 2.0))
    for sgn in (-1, 1):   # pointes vers l'extérieur de la case, de part et d'autre de la fleurette
        ops += outlined("gold", fleur_de_lis(54).transformed(affine(c, m + sgn * 10, rot=0 if sgn < 0 else 180)), outline=1.4)
    ops += fleurette(c, m, 18)
    return boite(ops, 100, H)


# --------------------------------------------------------------------------- #
# Baguettes : bouton au bout, et prolongement des baguettes extérieures aux coins
# --------------------------------------------------------------------------- #
def bouton(t=lambda p: p):
    """Bout arrondi d'une baguette, vers +x depuis son extrémité."""
    ops = []
    for ink, w in (("outline", BAR_OUT), ("gold", BAR), ("goldLight", BAR_LIGHT)):
        # le contour ne passe pas derrière x = 0 (il mordrait sur l'or de la baguette) ; l'or et le reflet y
        # reviennent d'un rien, pour couvrir le joint
        x0 = 0.0 if ink == "outline" else -0.5
        if ink == "goldLight":
            p = rect(x0, -w / 2, KNOB - x0, w)
        else:
            p = Path().M((x0, -w / 2)).L((KNOB, -w / 2))
            arc(KNOB, 0, w / 2, -90, 90, start=False, path=p)
            p.L((x0, w / 2)).Z()
        ops.append(fill(ink, p.transformed(t)))
    return ops


def ornement_coin():
    """Les baguettes extérieures débordent elles aussi du coin (le moteur ne fait déborder que les intérieures) :
    on redessine la croisée de l'angle extérieur, puis les deux bouts et leurs boutons."""
    e = OVERHANG
    ops = []
    for ink, w in (("outline", BAR_OUT), ("gold", BAR), ("goldLight", BAR_LIGHT)):
        p = Path().M((18, 0)).L((-e, 0)).M((0, 18)).L((0, -e))
        ops.append(stroke(ink, p, w))
    for rot, (x, y) in ((180, (-e, 0)), (270, (0, -e))):
        ops += bouton(affine(x, y, rot=rot))
    return boite(ops)


def fleuron_haut():
    """Fleuron rose posé sur la baguette au milieu du haut, pointe vers l'extérieur (origine au bord extérieur)."""
    p = teardrop(52, 22).transformed(affine(0, 30, rot=-90))
    for sgn in (-1, 1):
        p.extend(teardrop(26, 13).transformed(affine(sgn * 3, 24, rot=-90 + sgn * 58)))
    ops = outlined("pink", p, outline=FEUILLE_OUT)
    ops.append(stroke("pinkDark", smooth([(0, 22), (0, 6), (0, -10)]), 1.6, round=True))
    return boite(ops)


def spec():
    lam, amp = 260.0, 24.0
    lam_f, amp_f = 160.0, 26.0
    return cadre(
        name="mariage",
        title="Messe de mariage",
        source="paroissien chromolithographié, fin du XIXᵉ siècle, p.167 « Messe de mariage »",
        inks=INKS,
        palettes=PALETTES,
        layout=LAYOUT,
        top=rinceau(lam, amp, 135, root=0.45, angle=215, premiere=0.86),
        bottom=rinceau(lam_f, amp_f, 100, root=0.44, angle=215, premiere=0.70, inverse=True),
        centers={"top": centre_haut(lam, amp), "bottom": centre_pied(lam_f, amp_f)},
        corners={"top": coin_haut(), "bottom": coin_pied()},
        bars={"layers": [["outline", BAR_OUT], ["gold", BAR], ["goldLight", BAR_LIGHT]], "overhang": OVERHANG,
              "terminal": boite(bouton()), "cornerTerminals": False},
        outer={"corner": ornement_coin(), "mid": fleuron_haut(), "midBottom": boite([]), "midSide": boite([])},
    )
