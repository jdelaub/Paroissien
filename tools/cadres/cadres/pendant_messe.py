"""
Cadre « Prières pendant la messe » (p.57) : baguettes d'or ; en haut, des palmettes bleues en cœur, bordées de blanc,
sur fond bleu nuit, avec des écoinçons vert olive et des perles rouges ; sur les côtés, des feuilles de vigne rondes,
vert et jaune, qui alternent avec des éventails bleu clair ; au milieu du haut, un nœud d'or sur un losange rouge ;
au milieu des côtés, une fenêtre claire où un losange d'or entrelacé enserre une croix bleue ; carreaux roses aux
coins ; au pied, presque deux fois plus haut, un panneau unique d'entrelacs d'or (amandes bleu nuit semées de rose, cœur
bleu clair au centre, champs verts) prolongé par une chaîne de lentilles ; trèfles d'or aux coins hauts et socle
débordant au pied.
"""
import math

from commun import (Path, affine, boite, cadre, circle, diamond, fill, frise, leaf, mirror_x, outlined, palmette, piece,
                    polygon, rect, smooth, spiral, stroke, strand, teardrop, trefoil)

ORDRE = 57

INKS = {
    "navy": "#27304A",       # bleu nuit du fond des frises
    "gold": "#D7B565",       # or des baguettes et des entrelacs
    "goldDark": "#8C6434",   # liseré brun de l'or
    "white": "#F3EEE2",      # bords des palmettes, semis
    "lightBlue": "#A8BBCC",  # bleu clair des palmettes et des éventails
    "blue": "#3B5088",       # bleu foncé : nervures, croix
    "olive": "#A3A060",      # vert olive des feuilles et des champs du pied
    "yellow": "#E3D182",     # rehauts jaunes
    "greenDark": "#3E4A33",  # nervures vert sombre
    "pink": "#D3948A",       # carreaux des coins
    "red": "#AD4B3E",        # perles, losanges, filets des carreaux
}

PALETTES = {
    "GREEN":  {"navy": "#203A2B", "blue": "#2E5B40", "lightBlue": "#AFC8A8"},
    "VIOLET": {"navy": "#33264B", "blue": "#5A4386", "lightBlue": "#BAADCD"},
    "RED":    {"navy": "#561C1C", "blue": "#8E2F2A", "lightBlue": "#E1B9A8"},
    "ROSE":   {"navy": "#743A4F", "blue": "#A65A74", "lightBlue": "#EDC9D1"},
    "WHITE":  {"navy": "#DCC898", "blue": "#A07E3E", "lightBlue": "#FFFCF4", "white": "#FFFFFF"},
    "BLACK":  {"navy": "#24221F", "blue": "#55524D", "lightBlue": "#B9B5AD", "pink": "#ABA49A"},
}

BAR = 10.5        # baguette d'or (unités : 100 = épaisseur de la bande haute)
BAR_EDGE = 1.75   # son liseré brun, de chaque côté
STRAP = 6.0       # entrelacs d'or
STRAP_O = 1.2     # son liseré


def autour(cx, cy, s):
    """Homothétie de rapport s autour de (cx, cy)."""
    return affine(cx * (1 - s), cy * (1 - s), sx=s)


def semis(inside, x0, y0, x1, y1, pas, r, graine=1):
    """Semis de points réguliers, légèrement décalés (suite pseudo-aléatoire fixe), dans la zone `inside(x, y)`."""
    s = graine * 7919
    def rnd():
        nonlocal s
        s = (s * 1103515245 + 12345) & 0x7FFFFFFF
        return s / 0x7FFFFFFF - 0.5
    p = Path()
    row = 0
    y = y0
    while y <= y1:
        x = x0 + (pas / 2 if row % 2 else 0)
        while x <= x1:
            px, py = x + 0.5 * pas * rnd(), y + 0.5 * pas * rnd()
            if inside(px, py):
                p.extend(circle(px, py, r * (0.8 + 0.4 * (rnd() + 0.5))))
            x += pas
        y += pas * 0.87
        row += 1
    return p


def gold_strand(path, width=STRAP):
    return strand(path, width=width, ink="gold", outline=STRAP_O, outline_ink="goldDark")


def polyline(path, n=16):
    """Points d'un chemin (premier sous-chemin), les cubiques découpées en n segments."""
    pts, cur = [], None
    for c in path.cmds:
        if c[0] in ("M", "L"):
            cur = c[1]; pts.append(cur)
        elif c[0] == "C":
            p0, (p1, p2, p3) = cur, c[1:]
            for i in range(1, n + 1):
                t = i / n; s = 1 - t
                pts.append((s ** 3 * p0[0] + 3 * s * s * t * p1[0] + 3 * s * t * t * p2[0] + t ** 3 * p3[0],
                            s ** 3 * p0[1] + 3 * s * s * t * p1[1] + 3 * s * t * t * p2[1] + t ** 3 * p3[1]))
            cur = p3
    return pts


def dedans(path, marge=0.0):
    """Prédicat « (x, y) est dans la forme fermée, à plus de `marge` de son bord »."""
    pts = polyline(path)
    def inside(x, y):
        c = False
        for (x0, y0), (x1, y1) in zip(pts, pts[1:] + pts[:1]):
            if (y0 > y) != (y1 > y) and x < x0 + (y - y0) * (x1 - x0) / (y1 - y0):
                c = not c
        return c and all((x - px) ** 2 + (y - py) ** 2 >= marge * marge for px, py in pts)
    return inside


# --------------------------------------------------------------------------- #
# Frise haute : palmettes en cœur sur fond bleu nuit
# --------------------------------------------------------------------------- #
def coeur():
    """Contour d'une palmette en cœur, pointe vers l'extérieur (v = 8), deux volutes au pied (v ≈ 92)."""
    return (Path().M((0, 8))
            .C((10, 16), (44, 32), (45, 58))
            .C((46, 80), (38, 93), (25, 93))
            .C((15, 93), (9, 90), (0, 90))
            .C((-9, 90), (-15, 93), (-25, 93))
            .C((-38, 93), (-46, 80), (-45, 58))
            .C((-44, 32), (-10, 16), (0, 8)).Z())


def palmette_coeur():
    """Palmette en cœur : bord blanc, filet bleu, fond bleu clair, anthémion bleu et deux volutes."""
    ops = []
    h = coeur()
    ops.append(fill("white", h))
    ops.append(fill("blue", h.transformed(autour(0, 56, 0.88))))
    ops.append(fill("lightBlue", h.transformed(autour(0, 56, 0.82))))
    # anthémion : lance centrale et feuilles qui rayonnent du pied vers les lobes
    fronds = Path()
    fronds.extend(leaf(74, 14).transformed(affine(0, 86, rot=-90)))
    for sg in (-1, 1):
        fronds.extend(leaf(46, 8, bend=-0.10 * sg).transformed(affine(sg * 2, 82, rot=-90 + sg * 30)))
        fronds.extend(leaf(30, 7, bend=-0.12 * sg).transformed(affine(sg * 3, 80, rot=-90 + sg * 58)))
    ops.append(fill("blue", fronds))
    # volutes : œil bleu, enroulement blanc
    curls = Path()
    eyes = Path()
    for sg in (-1, 1):
        curls.extend(spiral(sg * 21, 80, 2.0, 9.5, 1.0, a0=90))
        eyes.extend(circle(sg * 21, 80, 3.6))
    ops.append(stroke("white", curls, 3.0, round=True))
    ops.append(fill("blue", eyes))
    return ops


def module_palmettes():
    P = 104.0
    c = P / 2
    # le fond des bandes est vert olive (celui du pied) : la frise haute pose son propre bleu nuit, borné à sa
    # case ; écoinçons et palmettes passent en « over », par-dessus les cases voisines
    base = [fill("navy", rect(-c - 1, 0, P + 2, 100))]
    # écoinçon vert olive entre deux palmettes, feuille jaune pointe en bas
    over = [fill("olive", polygon([(c - 46, 0), (c + 46, 0), (c + 8, 40), (c, 54), (c - 8, 40)]))]
    over += outlined("yellow", palmette(30).transformed(affine(c, 3, rot=90)), outline=1.0, outline_ink="greenDark")
    over.append(stroke("greenDark", Path().M((c, 6)).L((c, 28)), 1.8, round=True))
    over += palmette_coeur()
    over += outlined("red", circle(c, 90, 3.4), outline=0.8, outline_ink="navy")
    return frise(P, base, over, phase=0.5)


# --------------------------------------------------------------------------- #
# Frise des côtés : feuilles de vigne et éventails bleu clair, en alternance
# --------------------------------------------------------------------------- #
def feuille_vigne(cx, cy, flip):
    """Feuille ronde vert olive, festonnée et bordée de jaune : deux volutes au pied (v bas, ou v haut si `flip`),
    nervure vers l'autre bord."""
    sg = -1 if flip else 1           # sens de la pointe en v
    lobes = Path()
    lobes.extend(circle(cx, cy + 3 * sg, 29))
    for a in range(-10, 200, 30):
        ra = math.radians(a)
        lobes.extend(circle(cx + 28 * math.cos(ra), cy + sg * (3 + 27 * math.sin(ra)), 9.5))
    for side in (-1, 1):
        lobes.extend(circle(cx + side * 17, cy - sg * 17, 15))
    ops = outlined("olive", lobes, outline=2.4, outline_ink="yellow")
    veins = Path().M((cx, cy - sg * 6)).L((cx, cy + sg * 36))
    for t, dx in ((2.0, 21), (16.0, 15)):
        for side in (-1, 1):
            veins.M((cx, cy + sg * t)).L((cx + side * dx, cy + sg * (t + 13)))
    ops.append(stroke("greenDark", veins, 2.4, round=True))
    curls = Path()
    for side in (-1, 1):
        curls.extend(spiral(cx + side * 17, cy - sg * 17, 1.5, 11, 1.2, a0=(0 if side > 0 else 180)))
    ops.append(stroke("greenDark", curls, 2.8, round=True))
    return ops


def module_vigne():
    P = 250.0          # deux feuilles et deux éventails : les feuilles se retournent d'une fois sur l'autre
    R = 44.0
    # fond bleu clair borné à la période : sinon chaque élément recouvrirait le précédent
    base = [fill("lightBlue", rect(-1, 0, P + 2, 100))]
    # éventails : nervures bleues qui partent du milieu d'un bord et s'ouvrent vers les deux feuilles voisines
    ribs = Path()
    for u0, root in ((0, 6), (P / 2, 94)):
        sv = 1 if root < 50 else -1
        for side in (-1, 1):
            for du, dv in ((12, 90), (30, 86), (50, 66)):
                ribs.extend(smooth([(u0, root), (u0 + side * du * 0.3, root + sv * dv * 0.55),
                                    (u0 + side * du, root + sv * dv)]))
    base.append(stroke("blue", ribs, 3.4, round=True))
    base.append(fill("white", semis(lambda x, y: 0 < x < P and 10 < y < 90, 0, 12, P, 90, 17, 1.5, graine=3)))
    # médaillons bleu nuit et feuilles, par-dessus les éventails des deux éléments voisins
    over = []
    discs = Path()
    for c in (P / 4, 3 * P / 4):
        discs.extend(circle(c, 50, R))
    over.append(fill("navy", discs))
    over += feuille_vigne(P / 4, 50, flip=False)
    over += feuille_vigne(3 * P / 4, 50, flip=True)
    for c, v in ((P / 4, 9), (3 * P / 4, 91)):
        over += outlined("red", circle(c + 30, v + (6 if v < 50 else -6), 3.4), outline=0.8, outline_ink="navy")
    return frise(P, base, over, phase=0.0)


# --------------------------------------------------------------------------- #
# Milieu du haut : losange rouge (pièce) et nœud d'or (ornement par-dessus les baguettes)
# --------------------------------------------------------------------------- #
NOEUD = 47.0     # largeur de la case du nœud, au milieu de la bande haute


def piece_case_noeud():
    """Case vide qui interrompt la frise haute : le nœud, dessiné par-dessus les baguettes, la remplit."""
    return piece(NOEUD, [])


def ornement_noeud():
    """Nœud d'or : deux brins sinueux croisés trois fois, encadrés de deux brins en parenthèse, boucles aux bouts ;
    dessous, le fond bleu nuit de la case (il couvre aussi la jointure avec la frise) et un losange rouge."""
    ops = [fill("navy", rect(-NOEUD / 2 - 0.5, 6.5, NOEUD + 1, 87)), fill("red", diamond(0, 50, 24, 86))]
    paths = []
    s1 = smooth([(-7, -9)] + [(8 * math.sin(math.pi * (y - 6) / 41), y) for y in range(2, 92, 6)] + [(7, 103)])
    paths += [s1, s1.transformed(mirror_x)]
    br = smooth([(-6, -10), (-16, 8), (-21, 47), (-16, 86), (-6, 104)])
    paths += [br, br.transformed(mirror_x)]
    for y, a0 in ((-15, 0), (109, 0)):
        for sg in (-1, 1):
            paths.append(circle(sg * 9, y, 5.2))
    ops += [stroke("goldDark", p, 5.6 + 2 * STRAP_O) for p in paths]
    ops += [stroke("gold", p, 5.6) for p in paths]
    return boite(ops)


# --------------------------------------------------------------------------- #
# Milieu des côtés : fenêtre claire, losange d'or entrelacé et croix bleue
# --------------------------------------------------------------------------- #
def piece_fenetre():
    W = 270.0
    e = W / 2 - 7          # baguettes transversales, entièrement dans la pièce
    t = 88.0               # bord des carreaux vers la fenêtre
    ops = []
    ops.append(fill("paper", rect(-t, 0, 2 * t, 100)))
    # carreaux verts aux deux bouts, trèfle d'or pointé vers la fenêtre
    for sg in (-1, 1):
        x0 = min(sg * t, sg * e)
        ops.append(fill("olive", rect(x0, 0, abs(e - t), 100)))
        ops.append(fill("yellow", polygon([(sg * e, 6), (sg * e - sg * 14, 6), (sg * e, 22)])))
        ops.append(fill("yellow", polygon([(sg * e, 94), (sg * e - sg * 14, 94), (sg * e, 78)])))
        ops += outlined("gold", trefoil(20).transformed(affine(sg * (t + e + 20) / 2, 50, rot=-sg * 90)), outline=1.0,
                        outline_ink="goldDark")
    # triangles roses et bandes bleu clair dans les angles de la fenêtre
    tri = Path()
    strips = Path()
    L, H = 80.0, 44.0
    n = (H / math.hypot(L, H), L / math.hypot(L, H))
    for su in (-1, 1):
        for sv in (-1, 1):
            tri.extend(polygon([(su * (t - 2), 50 + sv * 44), (su * (t - 20), 50 + sv * 44), (su * (t - 2), 50 + sv * 26)]))
            d = 10.0
            a = (su * L + su * n[0] * d, 50 + sv * n[1] * d)
            b = (su * n[0] * d, 50 + sv * H + sv * n[1] * d)
            strips.M(a).L(b)
    ops.append(fill("red", tri))
    ops.append(stroke("lightBlue", strips, 5.0))
    # baguettes transversales
    bars = Path()
    for x in (-e, -t, t, e):
        bars.M((x, 0)).L((x, 100))
    ops.append(stroke("goldDark", bars, BAR + 2 * BAR_EDGE))
    ops.append(stroke("gold", bars, BAR))
    # losange rouge, croix bleue cernée de blanc
    ops.append(fill("red", diamond(0, 50, 2 * 60, 2 * 34)))
    cross = Path()
    for rot, l, w in ((0, 46, 22), (180, 46, 22), (90, 30, 20), (-90, 30, 20)):
        cross.extend(teardrop(l, w).transformed(affine(0, 50, rot=rot)))
    cross.extend(circle(0, 50, 12))
    ops += outlined("blue", cross, outline=2.0, outline_ink="white")
    notches = Path()
    for k in range(4):
        a = math.radians(45 + 90 * k)
        notches.extend(circle(16 * math.cos(a), 50 + 11 * math.sin(a), 3.0))
    ops.append(fill("red", notches))
    ops.append(fill("white", circle(0, 50, 2.8)))
    # losange d'or et boucle extérieure
    loz = polygon([(-82, 50), (0, 4), (82, 50), (0, 96)])
    ops += gold_strand(loz, 7.0)
    ops += gold_strand(circle(0, -11, 8.5), 5.5)
    return piece(W, ops, outset=20)


# --------------------------------------------------------------------------- #
# Pied : panneau d'entrelacs (pièce centrale) et chaîne de lentilles (frise)
# Repère du pied : v = 0 au bord extérieur (en bas), v = 100 côté texte (en haut).
# --------------------------------------------------------------------------- #
LENS_P = 110.0   # pas de la chaîne
LENS_H = 38.0    # demi-hauteur d'une lentille
LENS_A = 0.16    # poignée des cubiques, en fraction du pas (croisement raide)
FOOT_STRAP = 7.5 # entrelacs du pied, plus larges : l'or y domine


def lentille(u0, u1, h):
    """Brins haut et bas d'une lentille entre deux croisements (u0, 50) et (u1, 50)."""
    a = LENS_A * (u1 - u0) if (u1 - u0) < 2 * LENS_P else LENS_A * LENS_P
    k = 4 * h / 3
    up = Path().M((u0, 50)).C((u0 + a, 50 + k), (u1 - a, 50 + k), (u1, 50))
    lo = Path().M((u0, 50)).C((u0 + a, 50 - k), (u1 - a, 50 - k), (u1, 50))
    zone = (Path().M((u0, 50)).C((u0 + a, 50 + k), (u1 - a, 50 + k), (u1, 50))
            .C((u1 - a, 50 - k), (u0 + a, 50 - k), (u0, 50)).Z())
    return up, lo, zone


def fleuron_or(cx, cy, s=1.0):
    """Petit fleuron d'or : quatre pétales en croix autour d'un bouton."""
    p = Path()
    for rot, l in ((90, 15), (-90, 15), (0, 10), (180, 10)):
        p.extend(teardrop(l * s, 8 * s).transformed(affine(cx, cy, rot=rot)))
    p.extend(circle(cx, cy, 4 * s))
    return outlined("gold", p, outline=1.0, outline_ink="goldDark")


def module_chaine():
    P = LENS_P
    # fonds bornés à la période ; les brins, à cheval sur deux éléments, passent en « over »
    base = [fill("olive", rect(-1, 0, P + 2, 100))]
    base.append(fill("yellow", semis(lambda x, y: 0 < x < P, 0, 8, P, 92, 11, 1.6, graine=5)))
    up, lo, zone = lentille(0, P, LENS_H)
    base.append(fill("lightBlue", zone))
    base.append(fill("white", semis(dedans(zone, 6), 0, 14, P, 86, 9, 1.5, graine=6)))
    over = fleuron_or(P / 2, 50, 1.1)
    # vrilles d'or dans les champs verts, de part et d'autre des croisements
    curls = Path()
    for u, a0 in ((15, 180), (P - 15, 0)):
        for v in (85, 15):
            curls.extend(spiral(u, v, 1.2, 6.5, 1.0, a0=a0 if v > 50 else -a0))
    over += gold_strand(curls, 3.2)
    for p in (up, lo):
        over += gold_strand(p, FOOT_STRAP)
    return frise(P, base, over, phase=0.0)


def panneau_pied():
    """Pièce centrale du pied, symétrique : cœur bleu clair au centre, amandes bleu nuit à dragons d'or, reliées à la
    chaîne de lentilles par un croisement à chaque bout."""
    W = 560.0
    E = W / 2
    ops = [fill("olive", rect(-E, 0, W, 100))]
    ops.append(fill("yellow", semis(lambda x, y: True, -E, 8, E, 92, 11, 1.6, graine=7)))

    # amande gauche : du croisement (-E, 50) à la pointe intérieure (-58, 32) ; la droite en miroir
    k = LENS_A * LENS_P
    up = (Path().M((-E, 50)).C((-E + k, 96), (-205, 95), (-150, 92))
          .C((-104, 89), (-66, 74), (-50, 28)))
    lo = (Path().M((-E, 50)).C((-E + k, 4), (-205, 6), (-150, 8))
          .C((-104, 10), (-70, 10), (-50, 28)))
    zone = (Path().M((-E, 50)).C((-E + k, 96), (-205, 95), (-150, 92))
            .C((-104, 89), (-66, 74), (-50, 28))
            .C((-70, 10), (-104, 10), (-150, 8))
            .C((-205, 6), (-E + k, 4), (-E, 50)).Z())
    mx = lambda p: p.transformed(mirror_x)
    ops.append(fill("navy", zone))
    ops.append(fill("navy", mx(zone)))
    dots = semis(dedans(zone, 6), -E, 8, 0, 92, 10, 1.8, graine=9)
    ops.append(fill("pink", Path().extend(dots).extend(mx(dots))))

    # dragons : rinceau d'or en S au travers de chaque amande, nimbé de blanc, flammes et volutes
    body = smooth([(-246, 62), (-222, 78), (-196, 76), (-170, 58), (-140, 36), (-108, 22), (-80, 26)])
    head = spiral(-236, 64, 1.5, 10, 1.0, a0=-60)
    tail = spiral(-86, 38, 1.5, 9, 1.0, a0=120)
    flames = Path()
    for (x, y, r, l) in ((-200, 74, 75, 22), (-178, 64, -105, 20), (-150, 44, 80, 22), (-124, 29, -100, 19),
                         (-214, 76, -120, 16), (-100, 22, 70, 16), (-160, 52, -115, 14)):
        flames.extend(teardrop(l, 9).transformed(affine(x, y, rot=r)))
    for t in (lambda p: p, mx):
        ops.append(stroke("white", t(body), 10.5 + 2 * STRAP_O + 3.5))
        ops += outlined("gold", t(flames), outline=STRAP_O, outline_ink="goldDark")
        ops += gold_strand(t(body), 10.5)
        ops += gold_strand(t(head), 4.5)
        ops += gold_strand(t(tail), 4.5)

    # cœur central bleu clair, lobes vers le texte, fleuron d'or tombant
    heart = (Path().M((0, 8))
             .C((18, 22), (56, 42), (54, 70))
             .C((52, 92), (24, 98), (12, 88))
             .C((6, 83), (2, 78), (0, 74))
             .C((-2, 78), (-6, 83), (-12, 88))
             .C((-24, 98), (-52, 92), (-54, 70))
             .C((-56, 42), (-18, 22), (0, 8)).Z())
    ops.append(fill("lightBlue", heart))
    ops.append(fill("white", semis(lambda x, y: abs(x) > 9 and dedans(heart, 6)(x, y), -56, 12, 56, 94, 9, 1.5,
                                   graine=11)))
    ops += gold_strand(Path().M((0, 66)).L((0, 18)), 6.0)
    ops += outlined("gold", trefoil(34).transformed(affine(0, 74)), outline=1.0, outline_ink="goldDark")
    ops += gold_strand(heart, FOOT_STRAP)
    for p in (up, lo):
        ops += gold_strand(p, FOOT_STRAP)
        ops += gold_strand(mx(p), FOOT_STRAP)
    for sg in (-1, 1):
        ops += outlined("gold", circle(sg * 50, 28, 6.5), outline=STRAP_O, outline_ink="goldDark")
    # vrilles d'or des champs verts, comme dans la chaîne
    curls = Path()
    for u, a0 in ((-E + 15, 0), (-46, 180)):
        for v in (85, 15):
            if u > -60 and v < 50:
                continue
            curls.extend(spiral(u, v, 1.2, 6.5, 1.0, a0=a0 if v > 50 else -a0))
    ops += gold_strand(Path().extend(curls).extend(mx(curls)), 3.2)
    return piece(W, ops)


# --------------------------------------------------------------------------- #
# Coins
# --------------------------------------------------------------------------- #
def lierre(L, W):
    """Feuille de lierre en cœur, pointe vers +x, lobes du côté -x ; centre de la boîte en (0, 0)."""
    h = W / 2
    return (Path().M((L / 2, 0))
            .C((L * 0.25, h * 0.45), (-L * 0.25, h * 1.15), (-L * 0.46, h * 0.62))
            .C((-L * 0.62, h * 0.20), (-L * 0.50, -h * 0.02), (-L * 0.30, 0))
            .C((-L * 0.50, h * 0.02), (-L * 0.62, -h * 0.20), (-L * 0.46, -h * 0.62))
            .C((-L * 0.25, -h * 1.15), (L * 0.25, -h * 0.45), (L / 2, 0)).Z())


def coin_lierre():
    """Carreau rose du coin haut : feuille de lierre pointée vers l'angle extérieur, volutes blanches."""
    ops = [fill("olive", rect(0, 0, 100, 100))]
    t = affine(50, 50, rot=225)
    f = lierre(104, 96)
    ops.append(fill("pink", f.transformed(t)))
    ops.append(stroke("white", f.transformed(affine(50, 50, rot=225, sx=0.78)), 2.4))
    ops.append(stroke("red", Path().M((-30, 0)).L((40, 0)).transformed(t), 2.0, round=True))
    curls = Path()
    for sg in (-1, 1):
        curls.extend(spiral(-28, sg * 22, 2, 11, 1.0, a0=(90 if sg > 0 else -90)).transformed(t))
    ops.append(stroke("white", curls, 3.0, round=True))
    ops += outlined("red", trefoil(16).transformed(affine(84, 84, rot=135)), outline=0.8, outline_ink="pink")
    return boite(ops)


def coin_pied(h):
    """Carreau rose du pied, plus haut : arabesque symétrique autour d'un bouton d'or."""
    W = 100.0
    cy = h / 2
    ops = [fill("pink", rect(0, 0, W, h))]
    ops.append(stroke("red", rect(13, 13, W - 26, h - 26), 2.6))
    ops.append(stroke("red", Path().M((50, 18)).L((50, h - 18)), 2.6))
    # palmettes roses cernées de blanc, pointées vers les deux bouts ; feuilles blanches dans les angles
    palms = Path()
    for sv in (-1, 1):
        palms.extend(palmette(30).transformed(affine(50, cy + sv * 46, rot=sv * 90)))
    ops += outlined("pink", palms, outline=1.8, outline_ink="white")
    leaves = Path()
    for x, y, r in ((21, 21, 45), (79, 21, 135), (21, h - 21, -45), (79, h - 21, -135)):
        leaves.extend(teardrop(17, 9).transformed(affine(x, y, rot=r)))
    ops.append(fill("white", leaves))
    # quatre volutes blanches autour du bouton d'or
    curls = Path()
    for su in (-1, 1):
        for sv in (-1, 1):
            curls.extend(spiral(50 + su * 18, cy + sv * 20, 1.5, 12, 1.1, a0=(90 if sv < 0 else -90)))
    ops.append(stroke("white", curls, 3.6, round=True))
    ops += outlined("gold", circle(50, cy, 6), outline=1.2, outline_ink="red")
    return boite(ops, W, h)


# --------------------------------------------------------------------------- #
# Ornements extérieurs (unités : 100 = épaisseur de la bande haute)
# --------------------------------------------------------------------------- #
def ornement_trefle():
    """Trèfle d'or à l'angle haut, dans le prolongement de la diagonale."""
    p = Path()
    for x, y in ((-17, -17), (3, -15), (-15, 3)):
        p.extend(circle(x, y, 7.5))
    p.extend(circle(-6, -6, 8.5))
    return boite(outlined("gold", p, outline=1.2, outline_ink="goldDark"))


def ornement_socle():
    """Socle du pied : la baguette d'or déborde vers l'extérieur et se termine en pointe."""
    p = (Path().M((2, -7)).L((-48, -7))
         .C((-54, -7), (-60, -6), (-62, -4))
         .C((-50, 2), (-40, 10), (-30, 18))
         .C((-25, 21), (-21, 16), (-17, 12))
         .C((-13, 16), (-9, 26), (-5, 32))
         .L((2, 32)).Z())
    return boite(outlined("gold", p, outline=1.2, outline_ink="goldDark"))


def spec():
    s, b = 18, 32
    return cadre(
        name="pendant_messe",
        title="Prières pendant la messe",
        source="paroissien chromolithographié, fin du XIXᵉ siècle, p.57 « Prières pendant la messe »",
        inks=INKS,
        palettes=PALETTES,
        layout={"margin": 14, "top": s, "side": s, "bottom": b, "textPadding": 12},
        ground="olive",
        top=module_palmettes(),
        side=module_vigne(),
        bottom=module_chaine(),
        centers={"top": piece_case_noeud(), "side": piece_fenetre(), "bottom": panneau_pied()},
        corners={"top": coin_lierre(), "bottom": coin_pied(100.0 * b / s)},
        bars={"layers": [["goldDark", BAR + 2 * BAR_EDGE], ["gold", BAR]], "overhang": 0, "terminal": None,
              "cornerTerminals": False},
        outer={"corner": ornement_trefle(), "cornerBottom": ornement_socle(), "mid": ornement_noeud(),
               "midBottom": boite([]), "midSide": boite([])},
    )
