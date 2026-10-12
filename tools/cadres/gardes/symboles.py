"""
Garde « Symboles chrétiens » : papier de contre-plat d'un paroissien de la fin du XIXᵉ siècle (photo
garde-rouge-symboles-chretiens). Un réseau de losanges à filets fins bistre, un anneau à chaque nœud ; dans chaque
losange, un symbole rouge. Les symboles vont par rangées : toute une rangée porte le même, et la rangée suivante est
décalée d'un demi-losange. De haut en bas, huit rangées qui se répètent :
rameau d'olivier, chrisme, croix, alpha, palme, chrisme, croix, oméga.

Relevé (photo de 1638 × 4000 px, garde de 1380 px de large) :
  - losanges de 263 px de large et 280 px de haut, soit des carrés posés sur la pointe (la photo est allongée de
    7 % en hauteur, comme celle de la garde asanoha) ; 5,25 losanges dans la largeur ;
  - filets de 5 px (1,9 unité pour 100 unités de losange), anneaux de 42 px de diamètre, vides : les filets
    s'arrêtent à l'anneau ;
  - symboles de 190 à 205 px (73 à 78 unités), traits épais de 20 à 27 px (8 à 10 unités) ; autour de chacun,
    quelques traits fins rouges (2 unités) qui en répètent le contour : équerres et chevrons autour de la croix,
    filets le long des jambes de l'alpha, petit triangle sous lui, chevron à droite du chrisme ;
  - le chrisme a la boucle du rhô à gauche de la haste, enroulée en volute, et seule la moitié basse de la
    seconde branche du khi ; l'alpha est un triangle (Δ) à jambe droite plus grasse ; l'oméga a les pieds en épines ;
  - palette (papier ramené à #F1E8D6) : rouge #AF676C en médiane (bords compris), plus soutenu au cœur des
    symboles ; filets #A98C7A, mais ils paraissent plus clairs qu'ils ne sont (traits fins flous) ; papier crème.
    Retenu : crème #EDE2CB (un peu plus soutenu que le papier des pages), bistre #8D735C, rouge garance #A9474D.

Simplifications : les symboles sont symétrisés (la gravure est irrégulière), les traits fins réduits à ceux qui
reviennent à chaque symbole, les nervures des feuilles d'olivier seules gardées. Tuile : une colonne de huit
losanges (100 × 400 unités), réseau carré ; 68 dp de large, pour 5,3 losanges sur un écran de 360 dp comme sur la
garde du livre. Le papier imprimé garde sa couleur : en thème sombre, les trois encres sont rapprochées du fond.
"""
import math

from commun import Path, affine, arc, circle, fill, leaf, mirror_x, motif, polygon, rect, smooth, stroke

W, H = 100.0, 400.0
FILET = 1.9        # filets du réseau et anneaux
ANNEAU = 7.0       # rayon de l'anneau (milieu du trait)
FIN = 2.0          # traits fins rouges autour des symboles

INKS = {
    "creme": "#EDE2CB",   # papier de la garde
    "bistre": "#8D735C",  # filets et anneaux
    "rouge": "#A9474D",   # symboles
}


# --------------------------------------------------------------------------- #
# Réseau
# --------------------------------------------------------------------------- #
def reseau():
    """Filets à ±45° passant par les nœuds (0, 25 + 100 m) et (50, 75 + 100 m), puis les anneaux qui les coupent.
    Les filets sont coupés au ras de la tuile : le moteur dessine chaque tuile voisine en entier après la
    précédente, et un filet qui déborderait passerait sur les anneaux et les symboles de la voisine."""
    ops = []
    for m in range(-1, 5):
        c = 25 + 100 * m
        for sens in (1, -1):    # y = sens·x + c
            xs = sorted(((0 - c) / sens, (H - c) / sens))
            x0, x1 = max(0.0, xs[0]), min(W, xs[1])
            if x1 > x0:
                ops.append(stroke("bistre", Path().M((x0, sens * x0 + c)).L((x1, sens * x1 + c)), FILET))
    for m in range(4):
        for cx, cy in ((0, 25 + 100 * m), (50, 75 + 100 * m)):
            ops.append(fill("creme", circle(cx, cy, ANNEAU)))
            ops.append(stroke("bistre", circle(cx, cy, ANNEAU), FILET))
    return ops


# --------------------------------------------------------------------------- #
# Aides
# --------------------------------------------------------------------------- #
def lame(base, tip, w, bend=0.0):
    """Feuille de `base` à `tip`, large de `w`."""
    dx, dy = tip[0] - base[0], tip[1] - base[1]
    return leaf(math.hypot(dx, dy), w, bend).transformed(affine(base[0], base[1], math.degrees(math.atan2(dy, dx))))


def F(ink, path):
    return ("f", ink, path)


def S(ink, path, w, rond=False):
    return ("s", ink, path, w, rond)


def trait(points, w=FIN, ink="rouge"):
    return S(ink, polygon(points, closed=False), w)


def place(op, cx, cy):
    """Opération de dessin, le symbole centré en (cx, cy)."""
    path = op[2].transformed(affine(cx, cy))
    return fill(op[1], path) if op[0] == "f" else stroke(op[1], path, op[3], round=op[4])


def miroir(points):
    return [(-x, y) for x, y in points]


def rentre(pts, d):
    """Triangle rentré de d[i] sur le côté pts[i] → pts[i+1] (intersection des côtés décalés)."""
    aire = sum(p[0] * q[1] - q[0] * p[1] for p, q in zip(pts, pts[1:] + pts[:1]))
    s = 1 if aire > 0 else -1
    lignes = []
    for (p, q), e in zip(zip(pts, pts[1:] + pts[:1]), d):
        dx, dy = q[0] - p[0], q[1] - p[1]
        n = math.hypot(dx, dy)
        nx, ny = -dy / n * s, dx / n * s
        lignes.append((nx, ny, nx * p[0] + ny * p[1] + e))
    out = []
    for (a1, b1, c1), (a2, b2, c2) in zip([lignes[-1]] + lignes[:-1], lignes):
        det = a1 * b2 - a2 * b1
        out.append(((c1 * b2 - c2 * b1) / det, (a1 * c2 - a2 * c1) / det))
    return out


# --------------------------------------------------------------------------- #
# Symboles, centrés sur le losange (0, 0), y vers le bas
# --------------------------------------------------------------------------- #
def olivier():
    """Rameau d'olivier : tige, bourgeon de trois feuilles, une paire de feuilles larges, deux paires d'olives."""
    p = Path()
    for s in (-1, 1):
        p.extend(lame((s * 2.5, -5), (s * 17, -30), 12, bend=0.04 * s))
        p.extend(lame((s * 1.5, 15), (s * 33, -5), 14, bend=-0.05 * s))
        p.extend(circle(s * 7.5, 3, 3.6))
        p.extend(circle(s * 8.5, 26, 3.6))
    p.extend(lame((0, -11), (0, -38), 10))
    ops = [S("rouge", Path().M((0, -14)).L((0, 37)), 3.0), F("rouge", p)]
    # nervures (papier), sur les grandes feuilles seulement
    for s in (-1, 1):
        ops.append(S("creme", Path().M((s * 6.5, -12)).L((s * 13.5, -23.5)), 1.2, rond=True))
        ops.append(S("creme", Path().M((s * 8, 11)).L((s * 27, -2)), 1.2, rond=True))
    return ops


def chrisme():
    """Chrisme : haste du rhô, boucle à gauche enroulée en volute, khi dont la branche haute gauche se perd dans
    la boucle ; chevron fin à droite, trait fin à gauche."""
    return [
        S("rouge", Path().M((0, -33)).L((0, 37)), 8),
        S("rouge", Path().M((21, -16)).L((-21, 23)), 7.5),
        S("rouge", Path().M((-2, 4)).L((20, 23)), 7.5),
        S("rouge", smooth([(-1, -31), (-10, -33), (-20, -28), (-24, -18), (-20, -8), (-10, -3), (-1, -2)]), 6,
          rond=True),
        S("rouge", smooth([(-16, -21), (-17, -14), (-10, -10)]), 5, rond=True),
        trait([(26, -8), (17, 0.5), (25, 9)]),
        trait([(-33, -2), (-22, 14)]),
    ]


def croix():
    """Croix latine ; dans chaque angle, une équerre fine qui en répète le contour, et sous les bras un chevron."""
    ops = [F("rouge", rect(-5, -34, 10, 69)), F("rouge", rect(-24, -22, 48, 9.5))]
    for pts in ([(-33, -7), (-9.5, -7), (-9.5, 25)],      # équerre sous le bras
                [(-34, 6), (-15.5, 24), (-15.5, 2)],       # chevron
                [(-8.5, -31), (-8.5, -25), (-14, -25)]):   # petite équerre au-dessus du bras
        ops.append(trait(pts))
        ops.append(trait(miroir(pts)))
    return ops


def alpha():
    """Alpha en triangle (Δ), jambe droite plus grasse, haut dans son losange ; un filet fin le long de chaque
    jambe et un petit triangle fin dessous."""
    out = [(0, -37), (27, 10), (-27, 10)]
    ops = [F("rouge", polygon(out)), F("creme", polygon(rentre(out, (9, 5, 7))))]
    ops.append(trait([(-15, -22), (-29, 3)]))
    ops.append(trait([(15, -22), (29, 3)]))
    ops.append(S("rouge", polygon([(-13, 17), (13, 17), (0, 27)]), FIN))
    return ops


def palme():
    """Palme : six paires de folioles dressées en éventail sur une tige, une foliole au sommet."""
    p = Path()
    for base, tip, w in ((35, (33, -5), 5.5), (27, (27, -15), 5.5), (19, (21, -24), 5), (10, (15, -31), 5),
                         (1, (9.5, -36), 4.5), (-8, (4.5, -40), 4)):
        for s in (-1, 1):
            p.extend(lame((0, base), (s * tip[0], tip[1]), w, bend=-0.04 * s))
    p.extend(lame((0, -13), (0, -42), 4.5))
    return [S("rouge", Path().M((0, 39)).L((0, -14)), 3.0), F("rouge", p)]


def omega():
    """Oméga : panse ronde ouverte en bas, jambes serrées, pieds en épines vers l'extérieur, séparés de la panse
    par une encoche ; deux petits crochets fins dessous. Dessiné par moitié, la gauche en miroir."""
    cy, rx, ry, hx, hy = -2.0, 30.0, 23.0, 14.5, 13.0
    m = arc(0, cy, rx, 270, 395, ry=ry)                                    # panse, du haut au bas droit
    m.C((21, 15.5), (14, 17.5), (10, 17.5))                                # encoche au-dessus du pied
    m.C((17, 17.8), (25, 17.2), (31, 16))                                  # dessus du pied, jusqu'à la pointe
    m.C((24, 20.5), (13, 24.5), (3.2, 25.5))                               # dessous du pied
    m.L((3.2, 18)).C((3.2, 14), (4.5, 12), (hx * math.cos(math.radians(70)), cy + hy * math.sin(math.radians(70))))
    arc(0, cy, hx, 70, -90, start=False, path=m, ry=hy)                    # bord de l'œil, jusqu'en haut
    m.Z()
    p = Path().extend(m).extend(m.transformed(mirror_x))
    ops = [F("rouge", p)]
    for s in (-1, 1):
        ops.append(trait([(s * 11, 31), (s * 4.5, 31.5), (s * 6, 36.5)]))
    return ops


RANGEES = [olivier, chrisme, croix, alpha, palme, chrisme, croix, omega]


def spec():
    ops = reseau()
    for j, sym in enumerate(RANGEES):
        cx, cy = (50.0 if j % 2 == 0 else 0.0), 25.0 + 50.0 * j
        ops += [place(o, cx, cy) for o in sym()]
    return motif(
        name="symboles",
        title="Symboles chrétiens",
        source="garde d'un paroissien de la fin du XIXᵉ siècle, photo garde-rouge-symboles-chretiens",
        inks=INKS,
        w=W,
        h=H,
        ops=ops,
        largeur=68,
        reseau="carre",
        ground="creme",
    )
