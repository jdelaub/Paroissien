"""
Cadre « Complies du dimanche » (p.151, « Du dimanche ») : fond mauve semé de baies prune ; rubans d'argent
entrelacés, en câble à œillets en haut, en tresse à croisillons sur les côtés, en chaîne de grandes boucles au
pied, plus haut ; acanthes et rinceaux d'or, trèfles verts entre les brins. Losange noué aux coins et au milieu du
haut, médaillon vert et prune au milieu des côtés, nœud en vesica au milieu du pied ; filets verts croisés aux
coins, bord d'or au dehors, petites boucles d'argent accrochées aux filets.

Repère : v = 0 et v = 100 sont les axes des filets verts (baguettes du moteur), le fond mauve est entre les deux.
Le bord d'or est un filet centré sur le bord extérieur ; sa moitié intérieure est recouverte de fond par chaque
module (`fond_bord`), pour que l'or ne reste visible qu'au dehors, de la même largeur partout.
"""
import math

from commun import (Path, affine, boite, cadre, circle, diamond, ellipse, fill, filet, fleur_de_lis, frise, leaf,
                    outlined, palmette, piece, polygon, rect, smooth, spiral, stroke, strand, teardrop, trefoil)

ORDRE = 151

INKS = {
    "ground": "#866D80",   # mauve (gris violacé) du fond des frises
    "plum": "#5E3150",     # prune : baies, cadre des losanges, anneau du médaillon
    "strand": "#F5F1EA",   # argent des entrelacs et du lys
    "gold": "#E0C374",     # or : acanthes, rinceaux, bord extérieur
    "green": "#7FA073",    # vert des filets, trèfles et médaillon
    "outline": "#2E2229",  # contour brun prune
}

PALETTES = {
    "GREEN":  {"ground": "#7F9A79", "plum": "#2F4A2C", "green": "#4A6C43"},
    "VIOLET": {"ground": "#8B72A3", "plum": "#4C2F68"},
    "RED":    {"ground": "#A65A55", "plum": "#682322"},
    "ROSE":   {"ground": "#C9919F", "plum": "#8A4560"},
    "WHITE":  {"ground": "#E4D8C0", "plum": "#A88A4E", "green": "#8FAA80"},
    "BLACK":  {"ground": "#5A5556", "plum": "#2E2B2C", "green": "#77786F", "gold": "#B1AA9C"},
}

# Épaisseurs (dp) : haut et côtés, pied (× 1,65 comme sur la photo)
T, B = 19, 31
KP = T / B                  # unités du haut -> unités du pied
FOOT_H = 100 * B / T        # profondeur du pied en unités u (≈ 163)

BAR_O, BAR_G = 12.5, 9.5    # filets verts : contour, vert
GOLD = 15.0                 # largeur visible du bord d'or, au dehors du filet extérieur
GOLD_RULE = 2 * (BAR_O / 2 + GOLD)
PATCH = GOLD_RULE / 2 + 1   # profondeur (unités u) à recouvrir de fond dans chaque module

SW, SO = 8.5, 1.7           # ruban d'argent, son contour
M = 50.0                    # axe des bandes
E = 12.0                    # demi-longueur des croisements rectilignes (pente 1)
D = 4.0                     # recouvrement des rubans entre deux éléments voisins


# --------------------------------------------------------------------------- #
# Courbes : segments cubiques (p0, c1, c2, p1), pour pouvoir en découper des tronçons
# --------------------------------------------------------------------------- #
def herm(p0, t0, p1, t1, k=0.38):
    """Cubique de p0 à p1, tangentes t0 au départ et t1 à l'arrivée."""
    d = math.dist(p0, p1) * k
    n0, n1 = math.hypot(*t0), math.hypot(*t1)
    return (p0, (p0[0] + t0[0] / n0 * d, p0[1] + t0[1] / n0 * d),
            (p1[0] - t1[0] / n1 * d, p1[1] - t1[1] / n1 * d), p1)


def line(p0, p1):
    return (p0, (p0[0] + (p1[0] - p0[0]) / 3, p0[1] + (p1[1] - p0[1]) / 3),
            (p0[0] + 2 * (p1[0] - p0[0]) / 3, p0[1] + 2 * (p1[1] - p0[1]) / 3), p1)


def trace(segs):
    p = Path().M(segs[0][0])
    for s in segs:
        p.C(s[1], s[2], s[3])
    return p


def split(seg, t):
    lerp = lambda a, b: (a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t)
    p0, p1, p2, p3 = seg
    a, b, c = lerp(p0, p1), lerp(p1, p2), lerp(p2, p3)
    d, e = lerp(a, b), lerp(b, c)
    f = lerp(d, e)
    return (p0, a, d, f), (f, e, c, p3)


def bez(seg, t):
    return split(seg, t)[0][3]


def sub_x(seg, x0, x1):
    """Tronçon d'un segment entre les abscisses x0 et x1 (x monotone sur le segment)."""
    def t_at(x):
        lo, hi = 0.0, 1.0
        up = seg[3][0] > seg[0][0]
        for _ in range(40):
            mid = (lo + hi) / 2
            if (bez(seg, mid)[0] < x) == up:
                lo = mid
            else:
                hi = mid
        return (lo + hi) / 2
    ta, tb = sorted((t_at(x0), t_at(x1)))
    right = split(seg, ta)[1]
    return split(right, (tb - ta) / (1 - ta))[0]


def flip_v(segs, h=100.0):
    f = lambda p: (p[0], h - p[1])
    return [tuple(f(p) for p in s) for s in segs]


def flip_u(segs):
    f = lambda p: (-p[0], p[1])
    return [tuple(f(p) for p in s) for s in segs]


# --------------------------------------------------------------------------- #
# Motifs
# --------------------------------------------------------------------------- #
def trim_start(segs, d):
    """Les segments, raccourcis de `d` unités au départ."""
    seg = segs[0]
    lo, hi = 0.0, 1.0
    for _ in range(40):
        mid = (lo + hi) / 2
        if math.dist(bez(seg, mid), seg[0]) < d:
            lo = mid
        else:
            hi = mid
    return [split(seg, lo)[1]] + list(segs[1:])


def brin(segs, s=1.0, trim=0.0):
    """Ruban d'argent. `trim` raccourcit le contour au départ : quand le ruban commence sur un autre identique
    (raccord entre deux éléments), le bout du contour reste caché sous l'argent, sans liseré en travers."""
    outline = trace(trim_start(segs, trim * s)) if trim else trace(segs)
    return [stroke("outline", outline, (SW + 2 * SO) * s), stroke("strand", trace(segs), SW * s)]


def troncon(seg_long, seg_court, s=1.0):
    """Tronçon de dessus : le contour, plus court que le ruban, ne laisse pas de liseré en travers."""
    return [stroke("outline", trace([seg_court]), (SW + 2 * SO) * s), stroke("strand", trace([seg_long]), SW * s)]


def fond_bord(x, w, depth=PATCH):
    """Fond mauve sur la moitié intérieure du filet d'or (v de 0 à `depth`)."""
    return fill("ground", rect(x, -2, w, depth + 2))


def acanthe(L, W):
    """Feuille d'acanthe vers +x, base en (0, 0) : trois lobes de chaque côté, balayés vers la pointe."""
    up = [(0.08, 0.26), (0.26, 0.50), (0.32, 0.20), (0.52, 0.44), (0.58, 0.15), (0.76, 0.30), (0.82, 0.07)]
    pts = [(0, 0)] + [(L * a, -W * b) for a, b in up] + [(L, 0)] + [(L * a, W * b) for a, b in reversed(up)]
    return smooth(pts, closed=True, tension=0.8)


def gold_leaf(x, y, rot, L, W, out=1.3):
    """Acanthe d'or, avec sa nervure."""
    t = affine(x, y, rot=rot)
    vein = smooth([(0.06 * L, 0), (0.45 * L, 0.02 * W), (0.80 * L, 0)]).transformed(t)
    return outlined("gold", acanthe(L, W).transformed(t), outline=out) + [stroke("outline", vein, 1.1 * out, round=True)]


def berry(x, y, r=5.5, out=1.2):
    return outlined("plum", circle(x, y, r), outline=out)


def quatrefoil_x(cx, cy, r, sy=1.0):
    """Quatre lobes vers les sommets (haut, bas, gauche, droite), étirable en hauteur."""
    p = Path()
    for k in range(4):
        p.extend(teardrop(r, 0.75 * r).transformed(affine(rot=90 * k)).transformed(affine(cx, cy, sx=1, sy=sy)))
    return p


def boucle(px, py, nx, ny, hook=1):
    """Petite boucle d'argent accrochée à un filet : anneau posé sur le filet (normale extérieure n), puis un bout
    de filet redessiné par-dessus d'un côté, pour qu'il passe dessus d'un côté et dessous de l'autre."""
    r, w, o = 7.5, 6.5, 1.3
    cx, cy = px + 5 * nx, py + 5 * ny
    tx, ty = -ny * hook, nx * hook          # le long du filet
    ops = strand(circle(cx, cy, r), width=w, ink="strand", outline=o, outline_ink="outline")
    seg = lambda a, b: Path().M((px + tx * a, py + ty * a)).L((px + tx * b, py + ty * b))
    ops.append(stroke("outline", seg(3.0, 11.5), BAR_O))
    ops.append(stroke("green", seg(1.5, 13.0), BAR_G))
    return ops


# --------------------------------------------------------------------------- #
# Frise haute : câble à œillets (croisement, ouverture avec lys et acanthe, croisement, œillet)
# --------------------------------------------------------------------------- #
TOP_P, TOP_LO = 240.0, 150.0


def cable_strands():
    """Brin A en deux tronçons qui se recouvrent sur une courbe (loin de tout croisement, donc sans couture) :
    a1 passe le croisement u = 0 en montant, a2 celui de LO en descendant."""
    P, LO = TOP_P, TOP_LO
    H, EYE = 22.0, 23.0
    me = (LO + P) / 2
    a1 = [line((-E - D, M + E + D), (E, M - E)),
          herm((E, M - E), (1, -1), (46, H), (1, 0)),
          line((46, H), (LO - 46, H)),
          herm((LO - 46, H), (1, 0), (LO - E, M - E), (1, 1))]
    a2 = [a1[-1],
          line((LO - E, M - E), (LO + E, M + E)),
          herm((LO + E, M + E), (1, 1), (me, M + EYE), (1, 0)),
          herm((me, M + EYE), (1, 0), (P - E - D, M + E + D), (1, -1)),
          line((P - E - D, M + E + D), (P - E + D, M + E - D))]
    return a1, a2


def module_cable():
    P, LO = TOP_P, TOP_LO
    me = (LO + P) / 2
    base = [fond_bord(-0.5, P + 1)]
    # œillet : olive prune ; au-dessus et au-dessous, une pastille verte
    base += outlined("plum", ellipse(me, M, 18, 8.5), outline=1.2)
    for v in (15, 85):
        base += outlined("green", circle(me, v, 5.0), outline=1.2)
    # baies prune au-dessus et au-dessous du croisement u = 0, bourgeons d'or autour de celui de u = LO
    base += berry(0, 20) + berry(0, 80)
    base += outlined("gold", trefoil(17).transformed(affine(LO, 36)), outline=1.3)
    base += outlined("gold", trefoil(17).transformed(affine(LO, 64, rot=180)), outline=1.3)
    # ouverture : lys d'argent issu du croisement de gauche, acanthe d'or issue de celui de droite
    base += berry(64, 33, 4.5) + berry(64, 67, 4.5)
    base += outlined("strand", fleur_de_lis(58).transformed(affine(8, M, rot=90)), outline=1.4)
    base += gold_leaf(LO - 12, M, 180, 74, 44)
    # rubans : A monte en u = 0 (dessous), descend en LO (dessus) ; B en miroir
    a1, a2 = cable_strands()
    b = flip_v(a1) + flip_v(a2[1:])
    base += brin(a1, trim=2) + brin(b, trim=2) + brin(a2, trim=2)
    return frise(P, base, phase=0.0)


# --------------------------------------------------------------------------- #
# Frise des côtés : tresse à croisillons (rubans le long des filets, croisés en X), acanthes en candélabre
# --------------------------------------------------------------------------- #
SIDE_P = 180.0


def module_tresse():
    P = SIDE_P
    H = 20.0
    base = [fond_bord(-0.5, P + 1)]
    base += berry(0, 21, 6) + berry(0, 79, 6)
    # candélabre : deux acanthes d'or issues du croisement, trèfle vert dans l'axe, baies
    for sgn in (-1, 1):
        base += gold_leaf(14, M + sgn * 3, sgn * 13, 74, 28)
        base += berry(P - 42, M + sgn * 19, 4.5)
    base += outlined("green", trefoil(30).transformed(affine(P - 66, M, rot=90)), outline=1.3)
    a = [line((-E - D, M + E + D), (24, M - 24)),
         herm((24, M - 24), (1, -1), (38, H), (1, 0)),
         line((38, H), (P - 38, H)),
         herm((P - 38, H), (1, 0), (P - 24, M - 24), (1, 1)),
         line((P - 24, M - 24), (P - E + D, M - E + D))]
    base += brin(a, trim=2) + brin(flip_v(a), trim=2)   # B, descendant en u = 0, passe dessus
    return frise(P, base, phase=0.0)


# --------------------------------------------------------------------------- #
# Frise du pied (unités du pied : 100 = 31 dp) : chaîne de grandes boucles, rinceaux d'or, trèfle vert
# --------------------------------------------------------------------------- #
FOOT_P = 150.0


def ecoincons_pied(u0=0.0, t=None, sc=1.0):
    """Écoinçons du croisement du pied (en u0) : acanthe d'or vers chaque filet, baies. Les pièces voisines (coin,
    nœud central) les redessinent à leur bord, pour qu'ils ne soient pas coupés en deux par la découpe ; `t` et
    `sc` les passent dans un autre repère."""
    t = t or (lambda p: p)
    ops = []
    for sgn in (-1, 1):
        ops += outlined("gold", acanthe(27, 22).transformed(affine(u0, M + sgn * 33, rot=-90 * sgn)).transformed(t),
                        outline=1.0 * sc)
        for side in (-1, 1):
            ops += outlined("plum", circle(u0 + side * 16, M + sgn * 37, 3.6).transformed(t), outline=0.9 * sc)
    return ops


def module_boucles():
    P = FOOT_P
    s = KP
    e = E * s * 1.2
    d = D * s
    H = 18.0
    base = [fond_bord(-0.5, P + 1, PATCH * s)]
    base += ecoincons_pied()
    # dans la boucle : rinceaux d'or en volute, trèfle vert vers +u, feuille verte vers -u
    for sgn in (-1, 1):
        stem = smooth([(20, M + sgn * 4), (42, M + sgn * 15), (70, M + sgn * 21), (96, M + sgn * 18), (108, M + sgn * 10)])
        curl = spiral(104, M + sgn * 14, 1.0, 6.0, 1.1, a0=(90 if sgn < 0 else -90))
        base += [stroke("outline", stem, 6.4), stroke("outline", curl, 6.4, round=True)]
        base += [stroke("gold", stem, 4.0), stroke("gold", curl, 4.0, round=True)]
        base += gold_leaf(44, M + sgn * 15, -sgn * 42, 26, 16, out=0.9)
    base += outlined("green", trefoil(34).transformed(affine(70, M, rot=90)), outline=1.0)
    base += outlined("green", leaf(22, 9).transformed(affine(34, M, rot=180)), outline=0.9)
    a = [line((-e - d, M + e + d), (e, M - e)),
         herm((e, M - e), (1, -1), (P / 2, H), (1, 0)),
         herm((P / 2, H), (1, 0), (P - e - d, M - e - d), (1, 1)),
         line((P - e - d, M - e - d), (P - e + d, M - e + d))]
    base += brin(a, s, trim=2) + brin(flip_v(a), s, trim=2)
    return frise(P, base, phase=0.0)


# --------------------------------------------------------------------------- #
# Pièces centrales
# --------------------------------------------------------------------------- #
def losange(cx, cy, hx, hy, out=1.3):
    """Cœur des losanges noués : cadre prune, quadrilobe vert, point d'or."""
    ops = outlined("plum", diamond(cx, cy, 2 * hx * 0.72, 2 * hy * 0.72), outline=out)
    ops += outlined("green", quatrefoil_x(cx, cy, hx * 0.42, sy=hy / hx), outline=out)
    ops += outlined("gold", circle(cx, cy, 4.2), outline=1.0)
    return ops


def centre_haut():
    """Losange noué au milieu du haut ; ses sommets gauche et droit prolongent les croisements du câble."""
    W = 100.0
    ops = [fond_bord(-W / 2, W)]
    for sgn in (-1, 1):
        ops += berry(sgn * W / 2, 20) + berry(sgn * W / 2, 80)     # moitiés des baies du câble
    ops += losange(0, M, 50, 50)
    L, R, T, Bt = (-50, M), (50, M), (0, 0), (0, 100)
    # A (montant) passe dessous aux sommets gauche et droit, B (descendant) dessus
    for seg in ([line(L, T)], [line(R, T)], [line(L, Bt)], [line(R, Bt)]):
        ops += brin(seg)
    return piece(W, ops)


def centre_cote():
    """Médaillon vert à rosace, palmettes vertes de part et d'autre, rubans le long des filets ; ses oreilles,
    posées sur les filets, sont un ornement extérieur (`oreilles`)."""
    W = 150.0
    ops = [fond_bord(-W / 2, W)]
    for sgn in (-1, 1):
        ops += berry(sgn * W / 2, 21, 6) + berry(sgn * W / 2, 79, 6)     # moitiés des baies de la tresse
    for sgn in (-1, 1):
        x0 = sgn * W / 2
        up = [line((x0, M), (x0 - sgn * 30, M - 30)), line((x0 - sgn * 30, M - 30), (x0 - sgn * 60, M - 30))]
        ops += brin(up) + brin(flip_v(up))
        ops += outlined("green", palmette(30).transformed(affine(sgn * 36, M, rot=0 if sgn > 0 else 180)), outline=1.3)
    ops += outlined("green", circle(0, M, 40), outline=1.5)
    ring = circle(0, M, 26)
    ops += [stroke("outline", ring, 12), stroke("plum", ring, 9)]
    for k in range(4):   # rosace : quatre crans sombres sur le disque vert
        ops.append(fill("outline", polygon([(0, -20), (-3, -13), (3, -13)]).transformed(affine(0, M, rot=90 * k))))
    ops += outlined("gold", circle(0, M, 5), outline=1.2)
    return piece(W, ops)


def centre_pied():
    """Nœud du pied (unités du pied) : deux boucles pointues entrelacées, une vesica au milieu, olive prune."""
    W = 140.0
    s = KP
    ops = [fond_bord(-W / 2, W, PATCH * s)]
    E0, T0, R0, B0 = (70, M), (30, 17), (-10, M), (30, 83)
    top = [herm(E0, (-1, -1), T0, (-1, 0)), herm(T0, (-1, 0), R0, (0, 1))]
    bot = [herm(R0, (0, 1), B0, (1, 0)), herm(B0, (1, 0), E0, (1, -1))]
    for sgn in (-1, 1):
        ops += ecoincons_pied(sgn * W / 2)
        ops += outlined("green", trefoil(30).transformed(affine(sgn * 34, M, rot=90 * sgn)), outline=1.0)
    ops += outlined("plum", ellipse(0, M, 4.5, 15), outline=1.0)
    # boucle de gauche, puis de droite : la droite passe dessus aux deux croisements de la vesica,
    # puis un tronçon de la gauche repasse dessus en haut
    for segs in (flip_u(top), flip_u(bot), top, bot):
        ops += brin(segs, s)
    lt = flip_u(top)[1]
    ops += troncon(sub_x(lt, -9, 9), sub_x(lt, -6, 6), s)
    return piece(W, ops)


# --------------------------------------------------------------------------- #
# Coins
# --------------------------------------------------------------------------- #
def coin_haut():
    """Case carrée : losange noué dont les sommets touchent les filets."""
    ops = [fill("ground", rect(-2, -2, 104, PATCH + 2)), fill("ground", rect(-2, -2, PATCH + 2, 104))]
    for x, y, a in ((24, 24, -45), (76, 24, 45), (24, 76, -135), (76, 76, 135)):
        ops += outlined("plum", trefoil(15).transformed(affine(x, y, rot=a)), outline=1.1)
    ops += losange(50, 50, 50, 50)
    T_, R_, Bv, L_ = (50, 0), (100, 50), (50, 100), (0, 50)
    for a, b in ((R_, Bv), (T_, R_), (L_, Bv), (T_, L_)):
        ops += brin([line(a, b)])
    return boite(ops)


def coin_pied():
    """Case du pied, plus haute (100 × 163) : losange étiré, volutes d'or dans les écoinçons."""
    H = FOOT_H
    ops = [fill("ground", rect(-2, -2, 104, PATCH + 2)), fill("ground", rect(-2, -2, PATCH + 2, H + 4))]
    # volutes d'or dans les quatre écoinçons, enroulées vers l'angle de la case
    for x, y, sx, sy in ((19, 21, 1, 1), (81, 21, -1, 1), (19, H - 21, 1, -1), (81, H - 21, -1, -1)):
        c = spiral(0, 0, 2.0, 11.5, 1.15, a0=45).transformed(lambda p, x=x, y=y, sx=sx, sy=sy: (x + sx * p[0], y + sy * p[1]))
        ops += [stroke("outline", c, 7.4, round=True), stroke("gold", c, 4.6, round=True)]
    ops += ecoincons_pied(t=lambda p: (100 + p[0] / KP, p[1] / KP), sc=1 / KP)
    ops += losange(50, H / 2, 50, H / 2)
    O, R_, I, L_ = (50, 0), (100, H / 2), (50, H), (0, H / 2)
    for a, b in ((I, R_), (O, R_), (I, L_), (O, L_)):
        ops += brin([line(a, b)])
    return boite(ops, 100, H)


# --------------------------------------------------------------------------- #
# Ornements extérieurs : boucles d'argent sur les filets (unités u, 100 = épaisseur du haut)
# --------------------------------------------------------------------------- #
def boucles_coin(h):
    return boite(boucle(50, 0, 0, -1) + boucle(0, h / 2, -1, 0))


def boucles_milieu(h):
    return boite(boucle(0, 0, 0, -1) + boucle(0, h, 0, 1, hook=-1))


def oreilles():
    """Oreilles vertes du médaillon, à cheval sur les filets extérieur (y = 0) et intérieur (y = 100) : un lobe
    vert cerné, une volute sombre dedans."""
    ops = []
    for y, n in ((0, -1), (100, 1)):
        ops += outlined("green", circle(0, y + 5 * n, 9.5), outline=1.6)
        ops.append(stroke("outline", spiral(0, y + 5 * n, 1.0, 5.4, 0.85, a0=90 - 90 * n), 1.8, round=True))
    return boite(ops)


def spec():
    return cadre(
        name="complies",
        title="Complies du dimanche",
        source="paroissien chromolithographié, fin du XIXᵉ siècle, p.151 « Du dimanche » (complies)",
        inks=INKS,
        palettes=PALETTES,
        layout={"margin": 14, "top": T, "side": T, "bottom": B, "textPadding": 12},
        top=module_cable(),
        side=module_tresse(),
        bottom=module_boucles(),
        centers={"top": centre_haut(), "side": centre_cote(), "bottom": centre_pied()},
        corners={"top": coin_haut(), "bottom": coin_pied()},
        rules=[filet("gold", 0, GOLD_RULE, over=False)],
        bars={"layers": [["outline", BAR_O], ["green", BAR_G]], "overhang": 0, "terminal": None,
              "cornerTerminals": False},
        outer={"corner": boucles_coin(100), "cornerBottom": boucles_coin(FOOT_H),
               "mid": boucles_milieu(100), "midBottom": boucles_milieu(FOOT_H),
               "midSide": oreilles()},
    )
