"""
Cadre « Saint Matthieu, évangéliste » : gravure en taille-douce de la fin du XIXᵉ siècle, frontispice de l'Évangile
selon saint Matthieu, à l'encre brune sur papier nu. En haut, un rinceau d'acanthe à volutes et fleurettes, la
colombe du Saint-Esprit ailes déployées au milieu ; aux coins hauts, une croix dont la hampe descend le long des
côtés, enlacée d'une tige à feuilles ; au milieu de chaque côté, un ange debout, de profil, les mains jointes, tourné
vers le texte ; au pied, des rinceaux plus amples, à tige creuse et grappes de baies, et deux têtes de chérubins
ailées au milieu. Il ouvre chacun des quatre Évangiles.

Une seule encre, `line` : les pleins et déliés du burin sont des tailles remplies (`taille`), plus épaisses au
milieu qu'aux bouts. L'encre `paper` (fond de page) masque ce qui passe derrière les figures et creuse les tiges.
"""
import math
import re

from commun import Path, arc, boite, cadre, circle, fill, frise, mirror_x, piece, polygon, smooth, stroke

ORDRE = 903

INKS = {
    "line": "#5A4636",   # brun sépia de la taille-douce
}

# Les couleurs liturgiques teintent le trait. Luminance voisine du brun, pour rester lisible sur les fonds sombres :
# le trait n'est pas atténué (dimmed=[]), il n'éblouit pas.
PALETTES = {
    "GREEN":  {"line": "#3F5F43"},
    "VIOLET": {"line": "#5E4478"},
    "RED":    {"line": "#87352F"},
    "ROSE":   {"line": "#9A5468"},
    "WHITE":  {"line": "#86703F"},
    "BLACK":  {"line": "#46423E"},
}

INK = "line"
PAPER = "paper"

# Mise en page (dp) ; le pied est 1,6 fois plus haut que les côtés, comme sur la gravure.
SIDE, BOTTOM = 22, 35
K_PIED = BOTTOM / SIDE


# --------------------------------------------------------------------------- #
# Outils de tracé : tailles de burin, volutes, feuilles, fleurettes
# --------------------------------------------------------------------------- #
def _bez(a, b, c, d, t):
    s = 1 - t
    return (s * s * s * a[0] + 3 * s * s * t * b[0] + 3 * s * t * t * c[0] + t * t * t * d[0],
            s * s * s * a[1] + 3 * s * s * t * b[1] + 3 * s * t * t * c[1] + t * t * t * d[1])


def jalons(path, k=6):
    """Points le long d'un chemin (M, L, C), k par cubique, sans doublons."""
    pts, cur = [], None
    for c in path.cmds:
        if c[0] in "ML":
            cur = c[1]
            pts.append(cur)
        elif c[0] == "C":
            for i in range(1, k + 1):
                pts.append(_bez(cur, c[1], c[2], c[3], i / k))
            cur = c[3]
    out = [pts[0]]
    for p in pts[1:]:
        if math.hypot(p[0] - out[-1][0], p[1] - out[-1][1]) > 1e-6:
            out.append(p)
    return out


def ruban(pts, largeurs):
    """Polygone de part et d'autre d'une ligne de points, de largeur variable."""
    n = len(pts)
    gauche, droite = [], []
    for i, p in enumerate(pts):
        a, b = pts[max(0, i - 1)], pts[min(n - 1, i + 1)]
        dx, dy = b[0] - a[0], b[1] - a[1]
        d = math.hypot(dx, dy) or 1.0
        nx, ny = -dy / d * largeurs[i] / 2, dx / d * largeurs[i] / 2
        gauche.append((p[0] + nx, p[1] + ny))
        droite.append((p[0] - nx, p[1] - ny))
    return polygon(gauche + droite[::-1])


def taille(points, w, w0=None, w1=None, k=4, ink=INK):
    """Taille de burin : trait rempli, plein (w) au milieu, délié (w0, w1) aux deux bouts."""
    path = points if isinstance(points, Path) else smooth(points)
    pts = jalons(path, k)
    w0 = 0.35 * w if w0 is None else w0
    w1 = 0.35 * w if w1 is None else w1
    s = [0.0]
    for a, b in zip(pts, pts[1:]):
        s.append(s[-1] + math.hypot(b[0] - a[0], b[1] - a[1]))
    tot = s[-1] or 1.0
    larg = []
    for si in s:
        t = si / tot
        larg.append(w0 + (w1 - w0) * t + (w - (w0 + w1) / 2) * 4 * t * (1 - t))
    return fill(ink, ruban(pts, larg))


def volute(c, r0, r1, tours, a0, sens=1, pas=12, ouverture=0.85):
    """Points d'une volute de centre c : du rayon r0 (angle a0, degrés) au rayon r1, en `tours` tours ;
    sens = 1 horaire à l'écran (y vers le bas), -1 antihoraire. Plus `ouverture` est grande, plus le premier tour
    reste large avant de se resserrer."""
    n = max(4, int(tours * pas))
    out = []
    for i in range(n + 1):
        t = i / n
        a = math.radians(a0 + sens * 360 * tours * t)
        r = r0 * (r1 / r0) ** (t ** ouverture)
        out.append((c[0] + r * math.cos(a), c[1] + r * math.sin(a)))
    return out


def volute_depuis(b, d, r0, r1, tours, sens=1, pas=12, ouverture=0.85):
    """Volute qui part du point b, tangente à la direction d, et s'enroule (sens horaire si sens = 1).
    Rend (points, centre)."""
    n = math.hypot(*d)
    dx, dy = d[0] / n, d[1] / n
    a0 = math.atan2(-dx, dy) if sens > 0 else math.atan2(dx, -dy)
    c = (b[0] - r0 * math.cos(a0), b[1] - r0 * math.sin(a0))
    return volute(c, r0, r1, tours, math.degrees(a0), sens, pas, ouverture), c


def enroulement(points, w, w0=None, w1=0.7, oeil=None, centre=None, echo=0.0):
    """Tige qui finit en volute : la taille, un bouton à l'œil, et si `echo` un second trait fin qui double le
    premier tour à l'intérieur, comme sur la gravure."""
    ops = [taille(points, w, w0, w1, k=4)]
    e = points[-1]
    ops.append(fill(INK, circle(e[0], e[1], oeil if oeil is not None else 0.75 * w)))
    if echo and centre:
        n = len(points)
        dedans = []
        for p in points[1:int(n * 0.55)]:
            dx, dy = centre[0] - p[0], centre[1] - p[1]
            d = math.hypot(dx, dy) or 1
            dedans.append((p[0] + dx / d * echo, p[1] + dy / d * echo))
        if len(dedans) > 2:
            ops.append(taille(dedans, 1.5, 0.4, 0.4, k=4))
    return ops


def _relire(d):
    """Relit un chemin produit par Path.d() (M, L, C, Z)."""
    toks = re.findall(r"[MLCZ]|-?\d+(?:\.\d+)?", d)
    p, i = Path(), 0
    while i < len(toks):
        t = toks[i]
        if t == "Z":
            p.Z(); i += 1
            continue
        k = 6 if t == "C" else 2
        a = [float(x) for x in toks[i + 1:i + 1 + k]]
        pts = [(a[j], a[j + 1]) for j in range(0, k, 2)]
        getattr(p, t)(*pts)
        i += 1 + k
    return p


def transformer(ops, f):
    """Les mêmes tracés, chaque point passé par f (miroir, glissement, échange des axes)."""
    out = []
    for o in ops:
        q = dict(o)
        p = _relire(o["d"]).transformed(f)
        q["d"] = p.d() if "stroke" in o else p.normalized().d()
        out.append(q)
    return out


def acanthe(lg, larg):
    """Feuille d'acanthe vers +x, base en (0, 0) : bord supérieur en deux lobes pointus dirigés vers l'avant, pointe
    relevée, bord inférieur lisse. Rend (contour, nervure, ombre du bord inférieur)."""
    L, W = lg, larg
    p = (Path().M((0, 0))
         .C((0.05 * L, -0.35 * W), (0.14 * L, -0.62 * W), (0.32 * L, -0.80 * W))
         .C((0.27 * L, -0.60 * W), (0.29 * L, -0.44 * W), (0.36 * L, -0.34 * W))
         .C((0.44 * L, -0.60 * W), (0.54 * L, -0.76 * W), (0.68 * L, -0.82 * W))
         .C((0.63 * L, -0.60 * W), (0.65 * L, -0.44 * W), (0.70 * L, -0.34 * W))
         .C((0.80 * L, -0.50 * W), (0.92 * L, -0.62 * W), (L, -0.70 * W))
         .C((0.92 * L, -0.24 * W), (0.62 * L, 0.30 * W), (0.30 * L, 0.27 * W))
         .C((0.14 * L, 0.25 * W), (0.04 * L, 0.14 * W), (0, 0)).Z())
    nervure = [(0.03 * L, 0.0), (0.36 * L, -0.02 * W), (0.68 * L, -0.16 * W), (0.94 * L, -0.56 * W)]
    ombre = [(0.10 * L, 0.12 * W), (0.32 * L, 0.18 * W), (0.58 * L, 0.10 * W), (0.84 * L, -0.20 * W)]
    return p, nervure, ombre


def feuille_acanthe(x, y, rot, lg, larg, flip=False, w=1.5):
    """Feuille d'acanthe posée en (x, y), tournée de rot degrés ; `flip` met les lobes de l'autre côté."""
    ca, sa = math.cos(math.radians(rot)), math.sin(math.radians(rot))
    sg = -1 if flip else 1
    f = lambda p: (x + p[0] * ca - sg * p[1] * sa, y + p[0] * sa + sg * p[1] * ca)
    contour, nervure, ombre = acanthe(lg, larg)
    c = contour.transformed(f)
    return [fill(PAPER, c), stroke(INK, c, w, round=True), taille([f(p) for p in nervure], 2.0, 0.6, 0.4),
            taille([f(p) for p in ombre], 0.22 * larg, 0.5, 0.4)]


def fleurette(cx, cy, r, w=1.4):
    """Fleurette à cinq pétales ronds, cœur plein."""
    petales = Path()
    for k in range(5):
        a = math.radians(-90 + 72 * k)
        petales.extend(circle(cx + 0.55 * r * math.cos(a), cy + 0.55 * r * math.sin(a), 0.45 * r))
    return [stroke(INK, petales, 2 * w), fill(PAPER, petales), fill(INK, circle(cx, cy, 0.3 * r))]


def grappe(cx, cy, r, rot=90.0):
    """Grappe de six baies en triangle, pointe vers rot (degrés) ; chaque baie a son ombre."""
    ca, sa = math.cos(math.radians(rot)), math.sin(math.radians(rot))
    pos = [(-1.7, -1.05), (-1.7, 1.05), (-1.85, 0.0), (0.0, -0.55), (0.0, 0.55), (1.6, 0.0)]
    ops = []
    for (a, b) in pos:
        x = cx + r * (a * ca - b * sa)
        y = cy + r * (a * sa + b * ca)
        ops += [fill(PAPER, circle(x, y, r)), stroke(INK, circle(x, y, r), 1.4),
                fill(INK, circle(x + 0.3 * r, y + 0.3 * r, 0.36 * r))]
    return ops


def frange(a, b, n):
    return [a + (b - a) * i / n for i in range(n + 1)]


def tige_ondulee(v, w, u0, u1, n=36, ink=INK):
    """Tige maîtresse : la courbe v(u), d'épaisseur w(u), de u0 à u1."""
    pts = [(u, v(u)) for u in frange(u0, u1, n)]
    return fill(ink, ruban(pts, [w(u) for u, _ in pts]))


def deriv(v, u, h=0.5):
    return (2 * h, v(u + h) - v(u - h))


# --------------------------------------------------------------------------- #
# Frise haute : rinceau d'acanthe, volute sous chaque crête, fleurettes
# --------------------------------------------------------------------------- #
P_HAUT = 240.0
A_HAUT = 30.0


def v_haut(u):
    return 50.0 - A_HAUT * math.sin(2 * math.pi * u / P_HAUT)


def w_haut(u):
    return 3.4 - 1.0 * math.cos(4 * math.pi * u / P_HAUT)


def module_rinceau_haut():
    """Une demi-période (volute sous la crête, feuille d'acanthe sur la crête, fleurette au creux), puis la même
    glissée d'une demi-période et retournée : les volutes alternent dessus et dessous."""
    P = P_HAUT
    demi = []
    b = 84.0
    pts, c = volute_depuis((b, v_haut(b)), deriv(v_haut, b), 30, 4.5, 1.6, sens=1, ouverture=1.3)
    demi += enroulement(pts, 4.0, w_haut(b), 0.8, centre=c, echo=6.5)
    demi += feuille_acanthe(16, v_haut(16) - 1, -30, 46, 20)
    demi += fleurette(106, 82, 11)   # assez loin de u = P/2 : sa sœur glissée ne tombe pas sur la coupure
    base = [tige_ondulee(v_haut, w_haut, -12, P + 12)]
    base += demi
    base += transformer(demi, lambda p: (p[0] + P / 2, 100 - p[1]))
    return frise(P, base, [], phase=0.0)


# --------------------------------------------------------------------------- #
# Frise des côtés : hampe de la croix, qu'une tige enlace en ondes serrées ; à chaque crête la tige jette une
# volute au-dehors, et une feuille d'acanthe lui répond de l'autre côté de la hampe
# --------------------------------------------------------------------------- #
P_COTE = 260.0
A_COTE = 22.0
HAMPE = 54.0
W_HAMPE = 4.0


def v_cote(u):
    return HAMPE + A_COTE * math.sin(2 * math.pi * u / P_COTE)


def w_cote(u):
    return 3.6 - 0.9 * math.cos(4 * math.pi * u / P_COTE)


def barre(u0, u1, v, w, ink=INK):
    return fill(ink, polygon([(u0, v - w / 2), (u1, v - w / 2), (u1, v + w / 2), (u0, v + w / 2)]))


def module_hampe():
    P = P_COTE
    demi = []
    # feuille d'acanthe côté bord, née au croisement
    demi += feuille_acanthe(4, HAMPE - 3, -15, 84, 28)
    # volute côté texte, née de la crête
    b = 0.27 * P
    pts, c = volute_depuis((b, v_cote(b)), deriv(v_cote, b), 15, 2.5, 1.45, sens=1, ouverture=1.5)
    volute_ops = enroulement(pts, 3.8, w_cote(b), 0.7)
    base = [barre(-10, P + 10, HAMPE, W_HAMPE)]
    base += demi + transformer(demi, lambda p: (p[0] + P / 2, 2 * HAMPE - p[1]))
    # la tige passe sur la hampe au croisement u = 0 (un liseré de papier coupe la hampe)
    base.append(tige_ondulee(v_cote, lambda u: w_cote(u) + 3.4, -14, 14, n=8, ink=PAPER))
    base.append(tige_ondulee(v_cote, w_cote, -12, P + 12))
    base += volute_ops + transformer(volute_ops, lambda p: (p[0] + P / 2, 2 * HAMPE - p[1]))
    # au croisement u = P/2, une fleurette sur la hampe
    over = fleurette(P / 2, HAMPE, 11)
    return frise(P, base, over, phase=0.0, inset=8, outset=4)


# --------------------------------------------------------------------------- #
# Frise du pied : tige creuse en ondes amples, volutes doublées, grappes
# --------------------------------------------------------------------------- #
P_PIED = 230.0
A_PIED = 28.0


def v_pied(u):
    return 50.0 + A_PIED * math.sin(2 * math.pi * u / P_PIED)


def module_rinceau_pied():
    P = P_PIED
    demi = []
    b = 76.0
    # la volute naît au bord de la tige creuse, pas dans son canal de papier
    d = deriv(v_pied, b)
    _, c = volute_depuis((b, v_pied(b)), d, 29, 5, 1.6, sens=-1)
    e = math.hypot(c[0] - b, c[1] - v_pied(b))
    b2 = (b + (c[0] - b) / e * 4.4, v_pied(b) + (c[1] - v_pied(b)) / e * 4.4)
    pts, c = volute_depuis(b2, d, 29 - 4.4, 5, 1.6, sens=-1, ouverture=1.3)
    demi += enroulement(pts, 4.6, 3.6, 0.9, centre=c, echo=7.5)
    demi += feuille_acanthe(20, v_pied(20) + 2, 34, 50, 22, flip=True)
    base = [tige_ondulee(v_pied, lambda u: 10.0, -12, P + 12),
            tige_ondulee(v_pied, lambda u: 4.6, -14, P + 14, ink=PAPER)]
    base += demi
    base += transformer(demi, lambda p: (p[0] + P / 2, 100 - p[1]))
    # une fleurette près du croisement, une seule fois par période (sa sœur glissée tomberait sur la coupure)
    base += fleurette(112, 20, 10)
    base.append(taille([(106, v_pied(106) + 2), (110, 66), (116, 72)], 2.2, 2.0, 0.8))
    base += grappe(124, 80, 4.6, rot=40)
    return frise(P, base, [], phase=0.0)


# --------------------------------------------------------------------------- #
# Centres : colombe (haut), ange en prière (côtés), chérubins (pied)
# --------------------------------------------------------------------------- #
def symetrique(ops):
    """Les tracés et leur miroir (u -> -u)."""
    return list(ops) + transformer(ops, mirror_x)


def module_colombe():
    """Colombe du Saint-Esprit, de face, ailes déployées le long de la bande, tête tournée vers la droite."""
    W = 300.0
    moitie = []
    # aile droite : fond de papier, pennes rayonnantes, rangée de couvertures en festons, bord d'attaque plein
    aile = smooth([(8, 40), (22, 18), (52, 8), (96, 4), (148, 2), (132, 20), (102, 36), (68, 52), (36, 62), (14, 58)],
                  closed=True)
    moitie.append(fill(PAPER, aile))
    for tx, ty in [(146, 7), (132, 18), (116, 28), (100, 37), (84, 45), (66, 53), (48, 59), (32, 61)]:
        moitie.append(taille([(20, 36), ((20 + tx) / 2, (36 + ty) / 2 - 2), (tx, ty)], 2.8, 1.6, 0.6))
    for i in range(6):
        x = 28 + 11 * i
        moitie.append(stroke(INK, arc(x, 16 - 1.5 * i, 5.5, 10, 170), 1.7, round=True))
    moitie.append(taille([(8, 40), (22, 17), (52, 7), (96, 3), (150, 1)], 4.8, 2.4, 0.8))
    moitie.append(taille([(14, 58), (42, 62), (78, 50), (114, 32), (148, 6)], 2.2, 1.6, 0.6))
    # la tige de la frise finit en volute sous la pointe de l'aile
    pts, c = volute_depuis((140, 46), (-1, -0.35), 9, 2.5, 1.2, sens=-1)
    moitie += enroulement([(152, 50)] + pts, 3.0, w_haut(0), 0.6)
    ops = symetrique(moitie)
    # queue en éventail, sombre comme sur la gravure, plumes réservées en papier
    queue = smooth([(-8, 62), (-20, 90), (-7, 97), (0, 92), (7, 97), (20, 90), (8, 62)], closed=True)
    ops += [fill(INK, queue)]
    for x in (-10, 0, 10):
        ops.append(taille([(x * 0.3, 70), (x, 92)], 1.6, 0.6, 1.0, ink=PAPER))
    corps = smooth([(0, 22), (14, 33), (15, 50), (8, 67), (0, 72), (-8, 67), (-15, 50), (-14, 33)], closed=True)
    ops += [fill(PAPER, corps), stroke(INK, corps, 2.0, round=True)]
    ops.append(taille([(-11, 38), (-10, 54), (-3, 67)], 3.4, 0.8, 0.6))
    tete = circle(0, 20, 9.5)
    ops += [fill(PAPER, tete), stroke(INK, tete, 2.0)]
    ops.append(fill(INK, polygon([(8, 16.5), (17, 22), (8, 25.5)])))
    ops.append(fill(INK, circle(4, 18.5, 1.9)))
    return piece(W, ops, outset=4)


def module_ange():
    """Ange debout, de profil, mains jointes, tourné vers le texte. Dessiné en (x, y) : x de 0 (bord extérieur)
    à 100 (côté texte), y de -H/2 (tête) à +H/2 (pieds) ; puis (u, v) = (y, x)."""
    H = 640.0
    ops = []

    # aile, derrière : festons au bord, pennes rayonnantes
    bord = [(34, -196), (20, -246), (2, -280), (-16, -298), (-36, -292), (-48, -268), (-54, -232), (-52, -196),
            (-46, -160), (-36, -126), (-22, -96), (-6, -72), (12, -56)]
    aile = smooth(bord + [(24, -80), (30, -120), (36, -160)], closed=True)
    ops.append(fill(PAPER, aile))
    pts = jalons(smooth(bord[2:]), 8)
    s = [0.0]
    for a, b in zip(pts, pts[1:]):
        s.append(s[-1] + math.hypot(b[0] - a[0], b[1] - a[1]))
    N = 13
    reg = []
    for i in range(N + 1):
        t = s[-1] * i / N
        j = min(max(k for k in range(len(s)) if s[k] <= t + 1e-9), len(pts) - 2)
        f = (t - s[j]) / ((s[j + 1] - s[j]) or 1)
        reg.append((pts[j][0] + f * (pts[j + 1][0] - pts[j][0]), pts[j][1] + f * (pts[j + 1][1] - pts[j][1])))
    racine = (30, -170)
    for a, b in zip(reg, reg[1:]):
        m = ((a[0] + b[0]) / 2, (a[1] + b[1]) / 2)
        dx, dy = m[0] - racine[0], m[1] - racine[1]
        d = math.hypot(dx, dy)
        ops.append(taille([a, (m[0] + dx / d * 5, m[1] + dy / d * 5), b], 2.0, 1.0, 1.0, k=4))
    for a in reg[1:-1]:
        ops.append(taille([(racine[0] + (a[0] - racine[0]) * 0.3, racine[1] + (a[1] - racine[1]) * 0.3), a], 1.8, 0.4, 1.1))
    ops.append(taille([(34, -196), (20, -246), (2, -280), (-16, -298)], 3.0, 1.6, 1.2))

    # robe : contour, plis profonds du côté de l'ombre
    robe = smooth([(48, -206), (32, -192), (24, -150), (16, -80), (10, 0), (2, 90), (-12, 170), (-30, 226), (-48, 262),
                   (-58, 284), (-36, 292), (-8, 288), (22, 294), (56, 292), (82, 296), (98, 288), (96, 250), (94, 160),
                   (93, 60), (92, -30), (90, -90), (84, -140), (76, -186), (64, -204)], closed=True)
    ops += [fill(PAPER, robe), stroke(INK, robe, 2.0, round=True)]
    plis = [
        ([(22, -70), (15, 60), (2, 170), (-22, 240), (-46, 278)], 5.0),
        ([(30, -20), (26, 90), (16, 190), (-2, 262)], 2.2),
        ([(44, -40), (42, 80), (36, 180), (26, 270)], 2.4),
        ([(60, -30), (61, 90), (59, 190), (54, 278)], 2.0),
        ([(78, -50), (82, 70), (84, 190), (86, 278)], 1.8),
        ([(-34, 262), (-16, 274), (6, 278)], 1.8),
        ([(58, 282), (72, 272), (88, 278)], 1.6),
    ]
    for pts_, w in plis:
        ops.append(taille(pts_, w, 0.6, 0.9))

    # bras replié dans une manche ample, mains jointes vers le menton
    manche = smooth([(52, -192), (46, -150), (50, -114), (64, -98), (84, -104), (96, -128), (95, -146), (84, -146),
                     (70, -138), (62, -160), (64, -190)], closed=True)
    ops += [fill(PAPER, manche), stroke(INK, manche, 1.8, round=True)]
    ops.append(taille([(52, -150), (56, -120), (70, -104)], 2.6, 0.6, 0.6))
    ops.append(taille([(60, -176), (60, -150), (68, -132)], 1.6, 0.5, 0.5))
    mains = smooth([(86, -146), (88, -168), (94, -188), (99, -170), (99, -148)], closed=True)
    ops += [fill(PAPER, mains), stroke(INK, mains, 1.5, round=True)]

    # tête de profil, regard levé ; chevelure jusqu'à la nuque
    visage = smooth([(56, -276), (72, -270), (80, -258), (88, -248), (81, -242), (83, -236), (79, -231), (80, -224),
                     (70, -217), (60, -213), (44, -218), (32, -232), (30, -254), (38, -270)], closed=True)
    ops += [fill(PAPER, visage), stroke(INK, visage, 1.7, round=True)]
    ops += [taille([(62, -214), (62, -198)], 1.6, 1.2, 1.2), taille([(72, -218), (76, -202)], 1.6, 1.2, 1.2)]
    cheveux = smooth([(74, -270), (60, -283), (42, -279), (27, -265), (21, -240), (21, -218), (27, -200), (40, -204),
                      (46, -222), (54, -238), (64, -254), (72, -264)], closed=True)
    ops += [fill(PAPER, cheveux), stroke(INK, cheveux, 1.6, round=True)]
    for pts_ in ([(66, -272), (48, -268), (34, -250), (30, -224)], [(58, -260), (44, -248), (37, -226), (33, -206)],
                 [(70, -264), (56, -252), (48, -232), (42, -212)]):
        ops.append(taille(pts_, 2.2, 0.5, 0.6))
    ops.append(taille([(72, -252), (78, -253)], 1.8, 1.2, 1.0))

    # culot sous les pieds, bouton au-dessus de la tête : la hampe s'y arrête
    ops.append(taille([(-30, 302), (10, 306), (56, 304), (100, 300)], 3.4, 1.2, 1.6))
    pts, c = volute_depuis((-30, 302), (-1, -0.4), 8, 2, 1.0, sens=1)
    ops += enroulement(pts, 2.6, 2.6, 0.6)
    ops.append(barre(304, 322, HAMPE, W_HAMPE))
    ops.append(barre(-322, -306, HAMPE, W_HAMPE))
    ops.append(fill(INK, circle(HAMPE, -304, 4.0)))
    return piece(H, transformer(ops, lambda p: (p[1], p[0])), outset=60)


def module_cherubins():
    """Deux têtes de chérubins, ailes en cœur sous le menton. Dessinées en (x, y), y vers le bas de 0 (côté texte)
    à 100 (bord extérieur) ; puis (u, v) = (x, 100 - y)."""
    W = 260.0
    ops = []
    for sg in (-1, 1):
        cx, cy, ech = sg * 57, 38, 1.15
        f = lambda p, cx=cx, sg=sg: (cx + sg * ech * p[0], cy + ech * p[1])
        for side in (-1, 1):
            aile = smooth([(0, 54), (side * 16, 40), (side * 36, 16), (side * 46, -10), (side * 34, -22),
                           (side * 18, -6), (side * 6, 20)], closed=True).transformed(f)
            ops += [fill(PAPER, aile), stroke(INK, aile, 1.8, round=True)]
            # pennes qui rayonnent de la pointe, festons des couvertures le long du bord haut
            for k in range(4):
                ops.append(taille([f((side * (8 + 9 * k), -4 + 7 * k)), f((side * (14 + 7 * k), 18 + 5 * k)),
                                   f((side * 4, 48))], 2.0, 0.5, 0.4))
            for (x, y) in ((22, -6), (31, -12), (40, -10)):
                ops.append(stroke(INK, arc(side * x, y + 2, 4.5, 20, 160).transformed(f), 1.5, round=True))
        tete = circle(0, 0, 20).transformed(f)
        ops += [fill(PAPER, tete), stroke(INK, tete, 1.8)]
        # chevelure bouclée, sombre comme sur la gravure, chaque boucle reprise d'un trait de papier
        for (x, y, r) in ((-12, -14, 6), (0, -19, 6.5), (12, -14, 6), (-17, -3, 4.5)):
            m = circle(x, y, r).transformed(f)
            ops += [stroke(PAPER, m, 1.6), fill(INK, m)]
            ops.append(taille([f((x - r * 0.5, y + r * 0.1)), f((x, y + r * 0.4)), f((x + r * 0.4, y - r * 0.1))],
                              1.4, 0.5, 0.5, ink=PAPER))
        for ex in (-7, 7):
            ops.append(taille([f((ex - 3, 0)), f((ex, 1.5)), f((ex + 3, 0))], 1.8, 0.8, 0.8))
        ops.append(fill(INK, circle(*f((0, 11)), 1.7)))
        ops.append(taille([f((-1, 4)), f((1.5, 7))], 1.4, 1.0, 1.0))
    return piece(W, transformer(ops, lambda p: (p[0], 100 - p[1])))


# --------------------------------------------------------------------------- #
# Coins
# --------------------------------------------------------------------------- #
def module_croix():
    """Coin haut : la croix, dont la hampe descend le long du côté ; la tige du rinceau tourne le coin."""
    ops = []
    # tige : de la frise haute (100, 50) jusqu'à la hampe, puis le long du côté
    ops.append(taille([(104, v_haut(-4)), (100, 50), (88, 64), (72, 76), (60, 86), (HAMPE, 100), (HAMPE + 1, 106)],
                      3.6, w_haut(0), w_cote(0)))
    pts, c = volute_depuis((82, 70), (-1, 0.5), 11, 2.5, 1.2, sens=-1)
    ops += enroulement([(86, 67)] + pts, 2.4, 2.4, 0.6)
    ops += fleurette(84, 88, 9)
    # la croix, bouts boutonnés
    ops += transformer([barre(-22, 106, HAMPE, W_HAMPE + 0.4)], lambda p: (p[1], p[0]))
    ops.append(barre(16, 96, 32, W_HAMPE + 0.4))
    for (x, y) in ((HAMPE, -22), (16, 32), (96, 32)):
        ops.append(fill(INK, circle(x, y, 4.0)))
    ops.append(fill(INK, circle(HAMPE, 32, 6.0)))
    return boite(ops, 100, 100, outset=28)


def module_coin_pied():
    """Coin du pied (100 × 159) : la hampe finit sur un bouton, la tige creuse du pied s'enroule en grande volute
    dans l'angle. Dessiné en repère naturel (y vers le bas, 0 au bord haut de la bande du pied, H au bord extérieur),
    puis retourné."""
    W, H = 100.0, 100.0 * K_PIED
    yn = lambda v: H - K_PIED * v
    nat = transformer([barre(-4, 40, HAMPE, W_HAMPE)], lambda p: (p[1], p[0]))
    # la tige enlacée finit en petite volute contre la hampe
    pts, c = volute_depuis((HAMPE, 0), (-0.45, 1), 9, 2, 1.2, sens=1)
    nat += enroulement([(HAMPE + 1, -4)] + pts, 3.0, w_cote(0), 0.6)
    # tige creuse venue du pied, qui s'enroule en grande volute dans l'angle
    b = (58, yn(v_pied(0)) + 8)
    creux = smooth([(106, yn(v_pied(-6))), (100, yn(v_pied(0))), (80, b[1] - 3), b])
    pts, c = volute_depuis(b, (-1, 0.12), 31, 4.5, 1.75, sens=-1, pas=14, ouverture=1.5)
    nat += enroulement(pts, 4.8, 9.0, 0.9, centre=c, echo=8.0)
    nat.append(taille(creux, 10.0, 10.0, 9.0))
    nat.append(taille(smooth([(108, yn(v_pied(-8))), (100, yn(v_pied(0))), (80, b[1] - 3), b, (b[0] - 4, b[1] + 0.5)]),
                      4.6, 4.6, 0.8, ink=PAPER))
    nat += feuille_acanthe(80, b[1] - 6, -142, 50, 22, flip=True)
    nat.append(fill(INK, circle(HAMPE, 44, 4.6)))
    return boite(transformer(nat, lambda p: (p[0], H - p[1])), W, H)


def spec():
    return cadre(
        name="evangeliste",
        title="Saint Matthieu, évangéliste",
        source="gravure, fin du XIXᵉ siècle, frontispice « Saint Matthieu, évangéliste »",
        inks=INKS,
        palettes=PALETTES,
        dimmed=[],
        layout={"margin": 15, "top": SIDE, "side": SIDE, "bottom": BOTTOM, "textPadding": 12},
        ground=None,
        top=module_rinceau_haut(),
        side=module_hampe(),
        bottom=module_rinceau_pied(),
        centers={"top": module_colombe(), "side": module_ange(), "bottom": module_cherubins()},
        corners={"top": module_croix(), "bottom": module_coin_pied()},
    )
