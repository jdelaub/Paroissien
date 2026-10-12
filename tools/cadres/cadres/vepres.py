"""
Cadre « Vêpres du dimanche » (p.129) : fond brun chocolat bordé de filets d'argent, liseré d'or tout autour ;
entrelacs de rubans bleu clair. En haut, deux rubans torsadés qui s'ouvrent en œils (pupille blanche) et en
grandes cellules garnies d'acanthes d'or ; sur les côtés, des rubans qui se croisent en X et enferment des
candélabres d'acanthe à boutons blancs et perles corail ; au pied, plus haut, des rinceaux d'or autour d'un fleuron
d'argent. Losanges corail à rosette blanche aux coins et au milieu du haut, médaillon d'argent et de corail à oreilles au
milieu des côtés, nœud de deux boucles autour d'une amande corail au milieu du pied ; les nœuds bouclent par-dessus les
filets.

Tous les croisements des frises sont aux jonctions (phase 0) : la demi-croix au bout de chaque course part d'un
sommet de losange (coin) ou d'une pointe de la pièce centrale, sous le filet d'argent qui masque le raccord.
"""
import math

from commun import (Path, affine, arc, boite, cadre, circle, diamond, ellipse, filet, fill, fleur_de_lis, frise,
                    leaf, outlined, palmette, piece, smooth, spiral, stroke, strand, teardrop)

ORDRE = 129

INKS = {
    "ground": "#5B4637",   # brun chocolat du fond des frises
    "strand": "#86A6B0",   # bleu clair des entrelacs
    "gold": "#D2B377",     # or des rinceaux et du liseré extérieur
    "coral": "#DE8466",    # corail : perles, losanges, amande du pied
    "white": "#F3ECDD",    # blanc : rosettes, boutons, pupilles
    "silver": "#CFCBC0",   # argent : filets, médaillon, fleurons du pied
    "outline": "#2E231C",  # contour brun noir
}

# Le fond et une encre d'accent suivent la couleur liturgique ; l'or, l'argent et le corail restent ceux du livre.
PALETTES = {
    "GREEN":  {"ground": "#2F4B37", "strand": "#9CBDB2"},
    "VIOLET": {"ground": "#46305A", "strand": "#A9A8CF"},
    "RED":    {"ground": "#6E2420", "coral": "#E8A55C", "strand": "#B9C6CA"},
    "ROSE":   {"ground": "#94506A", "coral": "#F3C4A8", "strand": "#C3D5DC"},
    "WHITE":  {"ground": "#EFE6D2", "white": "#FFFFFF", "silver": "#BDB8AA"},
    "BLACK":  {"ground": "#2A2826", "strand": "#9A9893", "coral": "#8C8379", "gold": "#A9A397"},
}

# Épaisseurs (dp) : le pied est plus haut que le haut et les côtés.
T, S, B = 20.0, 20.0, 34.0
FK = T / B          # unités du haut -> unités du pied (même épaisseur physique des rubans)

SW = 8.4            # largeur du ruban bleu
SO = 1.5            # son contour
BAR_OUT, BAR_SILVER = 10.5, 7.5   # filets d'argent (baguettes) : contour, argent


def ruban(path, w=SW, o=SO):
    return strand(path, width=w, ink="strand", outline=o)


def croisement(points_long, points_court, w=SW, o=SO):
    """Tronçon de dessus à un croisement : contour plus court que le ruban, pour ne pas laisser de liseré."""
    return [stroke("outline", smooth(points_court), w + 2 * o), stroke("strand", smooth(points_long), w)]


def perle(x, y, r=4.6, o=1.2, ink="coral"):
    return outlined(ink, circle(x, y, r), outline=o)


def acanthe(L, W, curl=0.25, lobes=2):
    """Feuille d'acanthe vers +x, base en (0, 0) : une lame qui se recourbe (vers -y si curl > 0, vers +y sinon)
    et des lobes arrondis à pointe du côté convexe, tournés vers la pointe."""
    sg = 1 if curl >= 0 else -1
    b = -curl * L
    p = Path()
    p.extend(leaf(L, W * 0.62, bend=-curl))
    for i in range(lobes):
        t = 0.16 + 0.3 * i
        x, y = t * L, b * (1 - (1 - t) ** 3)
        rot = math.degrees(math.atan2(3 * b * (1 - t) ** 2, L)) + sg * 34
        p.extend(teardrop(L * (0.52 - 0.12 * i), W * (0.66 - 0.14 * i)).transformed(affine(x, y + sg * 0.12 * W, rot=rot)))
    return p


def feuille(path, t, ink="gold", o=1.2):
    """Feuille placée par la transformation t, contour brun."""
    return outlined(ink, path.transformed(t), outline=o)


def rosette(cx, cy, r, ink="white"):
    """Rosette à quatre pétales arrondis, cœur sombre."""
    p = Path()
    for k in range(4):
        a = math.radians(90 * k)
        p.extend(circle(cx + 0.48 * r * math.cos(a), cy + 0.48 * r * math.sin(a), 0.52 * r))
    ops = outlined(ink, p, outline=1.1)
    ops.append(fill("outline", circle(cx, cy, 0.16 * r)))
    return ops


# --------------------------------------------------------------------------- #
# Frise haute : torsade à œils et grandes cellules d'acanthe
# --------------------------------------------------------------------------- #
E_TOP, C_TOP = 62.0, 230.0       # longueur d'un œil, d'une cellule
A_EYE, A_CELL = 14.0, 30.0        # demi-ouverture de l'œil et de la cellule
# un pas : œil, cellule, œil, cellule, œil (le signe alterne d'un tronçon à l'autre)
SEGS = [(E_TOP, A_EYE), (C_TOP, -A_CELL), (E_TOP, A_EYE), (C_TOP, -A_CELL), (E_TOP, A_EYE)]
P_TOP = sum(L for L, _ in SEGS)


def _segments():
    """(début, longueur, amplitude) de chaque tronçon du pas."""
    out, a = [], 0.0
    for L, A in SEGS:
        out.append((a, L, A))
        a += L
    return out


def _f_top(u):
    for a, L, A in _segments():
        if u <= a + L:
            return A * math.sin(math.pi * (u - a) / L)
    return 0.0


def h_top(u):
    """Écart du ruban 1 à l'axe ; le pas compte un nombre impair de croisements, les deux rubans s'échangent donc
    d'un pas à l'autre (h(u + P) = -h(u))."""
    k = math.floor(u / P_TOP)
    return _f_top(u - k * P_TOP) * (-1) ** k


def _pts(fn, u0, u1, step=6.5):
    n = max(2, int(math.ceil((u1 - u0) / step)))
    return [(u0 + (u1 - u0) * i / n, fn(u0 + (u1 - u0) * i / n)) for i in range(n + 1)]


def perles_jonction(u):
    """Perles corail contre les deux filets ; la pièce centrale complète celles que coupe le bout de la course."""
    return perle(u, 13, 5.2) + perle(u, 87, 5.2)


def acanthe_couchee(u0, su, ops):
    """Au-dessus et au-dessous d'un œil : acanthe d'or couchée le long de la bande, née en u0, pointe vers su et
    recourbée vers le filet ; baie blanche au bout."""
    for sv in (-1, 1):
        t = affine(u0, 50 + sv * 30, rot=0 if su > 0 else 180)
        ops += feuille(acanthe(58, 19, curl=-sv * su * 0.26), t)
        tip = t((62, sv * su * 0.26 * 62))
        ops += perle(tip[0], tip[1] - sv * 2, 4.0, ink="white")


def cellule(a, L, ops):
    """Grande cellule : éventail d'acanthes né de chaque croisement, fleur bleue au centre entre des baies
    blanches, perles corail vers les filets."""
    C = a + L / 2
    for su in (1, -1):
        x0 = C - su * (L / 2 - 8)
        rot = 0 if su > 0 else 180
        for sv in (-1, 1):
            ops += feuille(acanthe(66, 21, curl=-sv * su * 0.2, lobes=3),
                           affine(x0 + su * 2, 50 + sv * 1.5, rot=rot + sv * su * 15))
        ops += feuille(leaf(54, 17), affine(x0, 50, rot=rot))
        for sv in (-1, 1):
            ops += perle(C - su * 50, 50 + sv * 38, 4.4)
            ops += perle(C - su * 30, 50 + sv * 12, 3.6, ink="white")
    petals = Path()
    for k in range(4):
        Lp, Wp = (23, 15) if k % 2 == 0 else (17, 12)
        petals.extend(teardrop(Lp, Wp).transformed(affine(C, 50, rot=90 * k)))
    ops += outlined("strand", petals, outline=1.3)
    ops += perle(C, 50, 4.6)


def module_torsade():
    P = P_TOP
    base, over = [], []
    segs = _segments()
    eyes = [s for s in segs if s[2] > 0]
    # perles corail contre les filets : à la croix de jonction et au-dessus du ou des œils du milieu
    for u in [0.0] + [a + L / 2 for a, L, _ in eyes[1:-1]]:
        base += perles_jonction(u)
    for a, L, A in eyes:
        if a == 0:
            acanthe_couchee(5, 1, base)
        elif a + L >= P:
            acanthe_couchee(P - 5, -1, base)
        else:
            acanthe_couchee(a + L / 2 + 4, 1, base)
            acanthe_couchee(a + L / 2 - 4, -1, base)
    for a, L, A in segs:
        if A < 0:
            cellule(a, L, base)
    # les deux rubans
    for s in (1, -1):
        base += ruban(smooth(_pts(lambda u: 50 + s * h_top(u), 0, P)))
    # œils : pupille blanche
    for a, L, _ in eyes:
        base += outlined("white", ellipse(a + L / 2, 50, 5.5, 2.6), outline=1.1)
    # croisements : le ruban qui descend (vers le texte) passe dessus
    for u0 in [a for a, _, _ in segs]:
        s = 1 if h_top(u0 + 0.5) - h_top(u0 - 0.5) > 0 else -1
        fn = lambda u, s=s: 50 + s * h_top(u)
        over += croisement(_pts(fn, u0 - 12, u0 + 12, 3), _pts(fn, u0 - 9, u0 + 9, 3))
    return frise(P, base, over, phase=0.0)


# --------------------------------------------------------------------------- #
# Frise des côtés : rubans croisés en X, cellules à candélabre d'acanthe
# --------------------------------------------------------------------------- #
P_SIDE, D_SIDE, V_SIDE = 210.0, 27.0, 23.0


def candelabre(P, base, over):
    """Cellule des côtés : candélabre à deux étages, chacun un bouton blanc (vers -u) entre deux feuilles d'or qui
    s'écartent puis s'enroulent par-dessus les rubans, une perle corail dans l'enroulement."""
    C = P / 2
    stem = Path().M((C - 40, 50)).L((P - 20, 50))
    base.append(stroke("outline", stem, 6.0))
    base.append(stroke("gold", stem, 3.2))
    for x, Lb in ((C - 22, 34), (C + 48, 28)):
        for sv in (-1, 1):
            over += feuille(acanthe(42, 17, curl=sv * 0.3, lobes=3), affine(x, 50 + sv * 3, rot=180 - sv * 24))
            over += perle(x - 40, 50 + sv * 20, 4.4)
        base += outlined("white", palmette(Lb).transformed(affine(x + 2, 50, rot=180)), outline=1.2)
    for sv in (-1, 1):
        base += feuille(acanthe(24, 10, curl=-sv * 0.2), affine(P - 22, 50, rot=180 - sv * 20))


def autour_croix(u):
    """Autour d'une croix des côtés : perles corail vers les filets, pétales blancs dans les angles."""
    ops = []
    for sv in (-1, 1):
        ops += perle(u, 50 + sv * 36, 4.6)
        for su in (-1, 1):
            ops += outlined("white", teardrop(13, 7).transformed(affine(u + su * 9, 50 + sv * 22, rot=sv * 90 - su * sv * 35)),
                            outline=1.0)
    return ops


def module_croisillons():
    P, d, v = P_SIDE, D_SIDE, V_SIDE
    base, over = [], []
    base += autour_croix(0)
    candelabre(P, base, over)
    for vv in (v, 100 - v):
        base += ruban(Path().M((0, 50)).L((d, vv)).L((P - d, vv)).L((P, 50)))
    # croix : le brin qui descend vers le texte passe dessus
    m = (50 - v) / d
    over += [stroke("outline", Path().M((-8, 50 - 8 * m)).L((8, 50 + 8 * m)), SW + 2 * SO),
             stroke("strand", Path().M((-11, 50 - 11 * m)).L((11, 50 + 11 * m)), SW)]
    return frise(P, base, over, phase=0.0)


# --------------------------------------------------------------------------- #
# Frise du pied (unités du pied) : rinceaux d'or et fleuron d'argent entre deux rubans
# --------------------------------------------------------------------------- #
P_FOOT, D_FOOT, V_FOOT = 200.0, 18.0, 32.0
SWF, SOF = SW * FK, SO * FK


def module_rinceaux():
    P, d, v = P_FOOT, D_FOOT, V_FOOT
    C = P / 2
    base, over = [], []
    # rinceaux d'or entre les rubans et chaque filet : tige en S, grande volute, acanthes affrontées
    for sv in (-1, 1):
        e = lambda k: 50 + sv * (50 - k)      # k : distance au filet
        stem = smooth([(8, e(25)), (32, e(17)), (C - 20, e(22)), (C + 20, e(17)), (C + 46, e(9)), (C + 66, e(14))])
        base.append(stroke("outline", stem, 5.0, round=True))
        base.append(stroke("gold", stem, 2.8, round=True))
        vol = spiral(C + 62, e(17), 1.5, 8.5, 1.1, a0=(-90 if sv < 0 else 90))
        base.append(stroke("outline", vol, 5.0, round=True))
        base.append(stroke("gold", vol, 2.8, round=True))
        base += feuille(acanthe(56, 19, curl=sv * 0.2, lobes=3), affine(C - 66, e(16), rot=sv * 3), o=1.0)
        base += feuille(acanthe(40, 15, curl=-sv * 0.25), affine(C + 34, e(13), rot=180 + sv * 4), o=1.0)
        base += perle(C - 4, e(9), 3.6, o=0.8)
        base += perle(C + 40, e(24), 3.6, o=0.8)
        # feuille bleue contre le filet, près de la croix
        base += outlined("strand", leaf(20, 8).transformed(affine(P - 8, e(10), rot=180 + sv * 8)), outline=0.8)
    for vv in (v, 100 - v):
        base += ruban(Path().M((0, 50)).L((d, vv)).L((P - d, vv)).L((P, 50)), SWF, SOF)
    # fleuron d'argent vers +u, feuille d'argent vers -u, perle au milieu
    for sv in (-1, 1):
        base += feuille(acanthe(30, 11, curl=sv * 0.3), affine(C + 2, 50 + sv * 2, rot=sv * 26), o=0.9)
        base += perle(C - 40, 50 + sv * 9, 2.8, o=0.8)
    base += outlined("silver", palmette(34).transformed(affine(C + 6, 50, sy=0.8)), outline=0.9)
    base += outlined("silver", leaf(34, 13).transformed(affine(C - 10, 50, rot=180)), outline=0.9)
    base += perle(C - 2, 50, 3.0, o=0.8, ink="silver")
    m = (50 - v) / d
    over += [stroke("outline", Path().M((-6, 50 - 6 * m)).L((6, 50 + 6 * m)), SWF + 2 * SOF),
             stroke("strand", Path().M((-8, 50 - 8 * m)).L((8, 50 + 8 * m)), SWF)]
    return frise(P, base, over, phase=0.0)


# --------------------------------------------------------------------------- #
# Losange à rosette (coins du haut, milieu du haut)
# --------------------------------------------------------------------------- #
def losange(cx, cy, rx, ry, w=SW, o=SO, ext=6.0):
    """Losange de rubans aux sommets (cx ± rx, cy) et (cx, cy ± ry), côtés prolongés au-delà des sommets ;
    losange corail, rosette blanche."""
    ops = []
    ops += outlined("coral", diamond(cx, cy, 2 * rx * 0.70, 2 * ry * 0.70), outline=1.3)
    ops.append(fill("ground", diamond(cx, cy, 2 * rx * 0.50, 2 * ry * 0.50)))
    ops += rosette(cx, cy, min(rx, ry) * 0.42)
    top, right, bot, left = (cx, cy - ry), (cx + rx, cy), (cx, cy + ry), (cx - rx, cy)
    for a, b in ((left, top), (top, right), (right, bot), (bot, left)):
        dx, dy = b[0] - a[0], b[1] - a[1]
        n = math.hypot(dx, dy)
        ex, ey = dx / n * ext, dy / n * ext
        ops += ruban(Path().M((a[0] - ex, a[1] - ey)).L((b[0] + ex, b[1] + ey)), w, o)
    return ops


def module_coin():
    """Coin du haut, 100 × 100 : losange de rubans dont les sommets intérieurs rejoignent les croix des frises."""
    ops = []
    for x, y in ((15, 15), (85, 15), (15, 85), (85, 85)):
        ops += perle(x, y, 5.0)
    ops += losange(50, 50, 50, 50)
    return boite(ops)


def module_coin_pied():
    """Coin du pied, 100 × 167 : losange étiré dont les sommets intérieurs rejoignent les croix des frises."""
    H = 100.0 / FK
    ops = []
    for x, y in ((16, 20), (84, 20), (16, H - 20), (84, H - 20)):
        ops += perle(x, y, 5.0)
    ops += losange(50, H / 2, 50, H / 2)
    return boite(ops, 100, H)


def centre_haut():
    ops = perles_jonction(-50) + perles_jonction(50)
    return piece(100, ops + losange(0, 50, 50, 50))


# --------------------------------------------------------------------------- #
# Médaillon du milieu des côtés
# --------------------------------------------------------------------------- #
W_MED = 236.0


def centre_cote():
    W = W_MED
    ops = []
    h = W / 2
    ops += autour_croix(-h) + autour_croix(h)
    # rubans le long des bords, derrière le médaillon ; celui qui gagne le côté texte passe dessus aux pointes
    for vv in (V_SIDE, 100 - V_SIDE):
        ops += ruban(Path().M((-h, 50)).L((-h + D_SIDE, vv)).L((h - D_SIDE, vv)).L((h, 50)))
    # fleurs de lys d'argent, pointe vers l'extérieur
    for sgn in (-1, 1):
        ops += outlined("silver", fleur_de_lis(46).transformed(affine(sgn * 38, 50, rot=sgn * 90)), outline=1.2)
    # médaillon : anneau d'argent, anneau corail, disque d'argent à croisette
    ops += outlined("silver", circle(0, 50, 42), outline=1.4)
    ops.append(fill("outline", circle(0, 50, 33.5)))
    ops.append(fill("coral", circle(0, 50, 32.5)))
    ops.append(fill("outline", circle(0, 50, 25)))
    ops.append(fill("silver", circle(0, 50, 24)))
    for k in range(4):
        a = math.radians(45 + 90 * k)
        ops.append(fill("outline", circle(14 * math.cos(a), 50 + 14 * math.sin(a), 2.2)))
    ops.append(stroke("outline", circle(0, 50, 5.5), 1.6))
    return piece(W, ops)


# --------------------------------------------------------------------------- #
# Nœud du milieu du pied (unités du pied)
# --------------------------------------------------------------------------- #
W_KNOT = 170.0


def centre_pied():
    """Deux boucles en goutte, pointe à la croix du bord, têtes rondes qui se chevauchent en amande au milieu."""
    W = W_KNOT
    r, cx = 42.0, 26.0
    ops = []
    # tiges de l'amande vers les boucles des filets (sous les boucles)
    for vv0, vv1 in ((-4, 22), (104, 78)):
        ops += ruban(Path().M((0, vv0)).L((0, vv1)), SWF, SOF)
    # amande corail au milieu
    ops += outlined("coral", ellipse(0, 50, 9, 24), outline=1.0)
    ops.append(fill("outline", ellipse(0, 50, 3.5, 13)))
    # fleurons d'argent dans les boucles, pointe vers l'extérieur
    for sgn in (-1, 1):
        ops += outlined("silver", palmette(30).transformed(affine(sgn * 38, 50, rot=0 if sgn > 0 else 180, sy=0.9)),
                        outline=0.9)
    for sgn in (-1, 1):
        for vv in (8, 92):
            ops += perle(sgn * 62, vv, 3.0, o=0.8)
    # deux boucles : pointe au bord, tête ronde de centre (-sgn * cx)
    paths = []
    for sgn in (-1, 1):
        tip = (sgn * W / 2, 50)
        c = (sgn * cx, 50)
        dist = W / 2 - cx
        a = math.degrees(math.asin(r / dist))
        L = math.sqrt(dist * dist - r * r)
        # points de tangence
        t1 = (tip[0] - sgn * L * math.cos(math.radians(a)), 50 - L * math.sin(math.radians(a)))
        t2 = (tip[0] - sgn * L * math.cos(math.radians(a)), 50 + L * math.sin(math.radians(a)))
        p = Path().M((tip[0] + sgn * 4, 50 + 4)).L(t1)
        ang1 = math.degrees(math.atan2(t1[1] - 50, t1[0] - c[0]))
        ang2 = math.degrees(math.atan2(t2[1] - 50, t2[0] - c[0]))
        if sgn > 0:
            arc(c[0], 50, r, ang1, ang2 - 360 if ang2 > ang1 else ang2, start=False, path=p)
        else:
            arc(c[0], 50, r, ang1, ang2 + 360 if ang2 < ang1 else ang2, start=False, path=p)
        p.L((tip[0] + sgn * 4, 50 - 4))
        paths.append((p, tip, t2))
    for p, _, _ in paths:
        ops += ruban(p, SWF, SOF)
    # la boucle gauche repasse dessus au croisement bas ; aux pointes, le brin vers le texte passe dessus
    y0 = 50 + math.sqrt(r * r - cx * cx)
    a0 = math.degrees(math.atan2(y0 - 50, cx))
    ops += [stroke("outline", arc(-cx, 50, r, a0 - 9, a0 + 9), SWF + 2 * SOF),
            stroke("strand", arc(-cx, 50, r, a0 - 12, a0 + 12), SWF)]
    for _, tip, t2 in paths:
        dx, dy = t2[0] - tip[0], t2[1] - tip[1]
        n = math.hypot(dx, dy)
        ex, ey = dx / n, dy / n
        ops += [stroke("outline", Path().M((tip[0] - 6 * ex, tip[1] - 6 * ey)).L((tip[0] + 6 * ex, tip[1] + 6 * ey)), SWF + 2 * SOF),
                stroke("strand", Path().M((tip[0] - 8 * ex, tip[1] - 8 * ey)).L((tip[0] + 8 * ex, tip[1] + 8 * ey)), SWF)]
    return piece(W, ops)


# --------------------------------------------------------------------------- #
# Boucles par-dessus les filets (ornements extérieurs, unités du haut)
# --------------------------------------------------------------------------- #
def anneau(x, y, r=8.5):
    return ruban(circle(x, y, r))


def ornement_coin():
    return boite(anneau(50, -8) + anneau(-8, 50))


def ornement_coin_pied():
    H = 100.0 / FK
    return boite(anneau(50, -8) + anneau(-8, H / 2))


def ornement_milieu():
    return boite(anneau(0, -8) + anneau(0, 108))


def ornement_medaillon():
    """Oreilles d'argent du médaillon, à cheval sur les deux filets (milieu des côtés)."""
    ops = []
    for y in (0, 100):
        ops += outlined("silver", circle(0, y, 10), outline=1.4)
        ops.append(stroke("outline", circle(0, y, 4.5), 1.4))
    return boite(ops)


def ornement_milieu_pied():
    H = 100.0 / FK
    return boite(anneau(0, -8) + anneau(0, H + 8))


def spec():
    return cadre(
        name="vepres",
        title="Vêpres du dimanche",
        source="paroissien chromolithographié, fin du XIXᵉ siècle, p.129 « Vêpres du dimanche »",
        inks=INKS,
        palettes=PALETTES,
        layout={"margin": 14, "top": T, "side": S, "bottom": B, "textPadding": 13},
        ground="ground",
        top=module_torsade(),
        side=module_croisillons(),
        bottom=module_rinceaux(),
        centers={"top": centre_haut(), "side": centre_cote(), "bottom": centre_pied()},
        corners={"top": module_coin(), "bottom": module_coin_pied()},
        rules=[filet("gold", -8, 26, over=False)],
        bars={"layers": [["outline", BAR_OUT], ["silver", BAR_SILVER]], "overhang": 0, "terminal": None,
              "cornerTerminals": False},
        outer={"corner": ornement_coin(), "cornerBottom": ornement_coin_pied(), "mid": ornement_milieu(),
               "midBottom": ornement_milieu_pied(), "midSide": ornement_medaillon()},
    )
