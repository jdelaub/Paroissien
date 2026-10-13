"""
Bandeau « Vigne et calice » : similigravure à l'encre brune sur papier nu, en marge extérieure d'une page de la
Communion (paroissien de la fin du XIXᵉ siècle, p.35, photo bandeau-vigne-ble-calice, prise de côté : rotation de
270°). Une seule bande, du côté de la tranche : deux sarments torsadés se croisent à intervalles réguliers ; chaque
boucle tient une feuille de vigne et une grappe, et quatre feuilles entourent chaque croisement. Au milieu, une
mandorle à double filet porte le calice et l'hostie ; une gerbe de blé passe derrière elle, épis en haut, tiges en
bas. Au pied, les deux sarments partent d'un cœur garni de deux crosses ; en haut, la vigne se perd sous la gravure
de la page : on la termine par deux volutes et une feuille.

Relevé (photo redressée, 1638 × 4000 px) : bande de 305 px de large (= 100 unités, 3,05 px par unité) ; croisements
tous les 330 à 415 px (période retenue : 120 unités) ; feuilles de 90 à 100 px (30 unités) ; mandorle de 505 × 200 px
(166 × 64 unités), hostie de 30 unités, coupe de 58 ; cœur du pied de 320 px (105 unités).

Palette (blanc corrigé sur le papier voisin, ×1,27) : trait sépia #574032 au plus sombre de la photo floue (le texte,
lui, est gris noir #65605D) ; demi-teinte des feuilles #A08A76 → `teinte` #A8927C ; mandorle plus claire (#C6B29E),
ramenée à la même teinte. Une encre propre, la trame ; le trait prend l'encre du texte du thème (« text ») : un peu plus
noir que le sépia d'origine en clair, mais lisible en sombre et en nuit, où un trait sépia se perdait dans le fond. Le
calice passe en trait plein pour rester lisible à 28 dp. Le papier (`paper`) évide l'hostie, l'intérieur des sarments
et le filet clair de la mandorle. Pas de palette liturgique : pas d'encre de fond.

Simplifications : sarments en sinusoïde aplatie (torsade à deux brins, le même brin dessus à chaque croisement),
feuilles à cinq lobes sans dentelure, grappes de onze grains, pas de vrilles (trop fines à cette taille), trois épis.
La frise ne repart pas en miroir après le calice (`symetrique=False`) : les grappes pendent sur toute la hauteur, comme
sur la page. Le calice, symétrique, n'est pas « droit » : sur un verso, la pièce se retourne avec la frise, sans quoi
les croisements de ses deux bouts changeraient de brin dessus en passant de la frise à la pièce.
"""
import math

from commun import Path, affine, arc, boite, bouts, cadre, circle, fill, frise, outlined, piece, polygon, smooth, stroke

ORDRE = 35

INKS = {
    "teinte": "#A8927C",  # demi-teinte de la trame : feuilles, grains, mandorle
}
TRAIT = "text"            # le trait suit l'encre du texte : sépia foncé en clair, clair en sombre

TW = 4.2       # largeur claire d'un sarment
TO = 1.8       # contour de chaque côté d'un sarment
LO = 1.5       # contour des feuilles
VEIN = 1.2     # nervures
P = 120.0      # période : d'un croisement au suivant
AMP = 32.0     # écart des sarments à l'axe, au milieu d'une boucle
FL = 28.0      # longueur des feuilles


# --------------------------------------------------------------------------- #
# Formes
# --------------------------------------------------------------------------- #
def contour(points, bulges):
    """Contour fermé par des points ; entre un point et le suivant, une courbe bombée vers l'extérieur de `bulge`
    (fraction de la longueur du côté)."""
    n = len(points)
    area = sum(points[i][0] * points[(i + 1) % n][1] - points[(i + 1) % n][0] * points[i][1] for i in range(n))
    sg = 1 if area > 0 else -1
    p = Path().M(points[0])
    for i in range(n):
        (ax, ay), (bx, by) = points[i], points[(i + 1) % n]
        dx, dy = bx - ax, by - ay
        nx, ny = sg * dy, -sg * dx
        k = bulges[i] * 1.33
        p.C((ax + dx / 3 + k * nx, ay + dy / 3 + k * ny), (ax + 2 * dx / 3 + k * nx, ay + 2 * dy / 3 + k * ny), (bx, by))
    return p.Z()


def feuille_vigne(L):
    """Feuille de vigne à cinq lobes pointus, pointant vers +x, attache en (0, 0)."""
    half = [(0.0, -0.26), (0.26, -0.52), (0.42, -0.30), (0.72, -0.44), (0.58, -0.14)]
    pts = [(0.10, 0.0)] + half + [(1.0, 0.0)] + [(x, -y) for x, y in reversed(half)]
    bulges = [0.22, 0.30, 0.10, 0.16, 0.08, 0.12, 0.12, 0.08, 0.16, 0.10, 0.30, 0.22]
    return contour([(x * L, y * L) for x, y in pts], bulges)


def feuille(ops, x, y, ang, L=FL, attache=None):
    """Feuille posée en (x, y), pointant vers `ang` (degrés) ; `attache` : point du sarment d'où part le pétiole
    (il part du bord du sarment, pour ne pas en rayer le creux)."""
    if attache:
        dx, dy = x - attache[0], y - attache[1]
        d = math.hypot(dx, dy) or 1.0
        e = TW / 2 + TO - 0.6
        if d > e:
            ops.append(stroke(TRAIT, Path().M((attache[0] + dx / d * e, attache[1] + dy / d * e)).L((x, y)), 1.8))
    f = affine(x, y, rot=ang)
    ops += outlined("teinte", feuille_vigne(L).transformed(f), outline=LO, outline_ink=TRAIT)
    for tip in ((0.84, 0), (0.62, -0.34), (0.62, 0.34)):
        ops.append(stroke(TRAIT, Path().M(f((0.12 * L, 0))).L(f((tip[0] * L, tip[1] * L))), VEIN, round=True))


def grappe(ops, x, y, ang, r=3.9):
    """Grappe pendante depuis (x, y) vers `ang` : rangs de 3, 2, 3, 2 et 1 grains, en quinconce."""
    f = affine(x, y, rot=ang)
    ops.append(stroke(TRAIT, Path().M(f((-2, 0))).L(f((r, 0))), 1.8, round=True))
    rows = [(-1.0, 0.0, 1.0), (-0.5, 0.5), (-1.0, 0.0, 1.0), (-0.5, 0.5), (0.0,)]
    for i, row in enumerate(rows):
        for j in row:
            c = circle(*f((r * (1.6 + 1.55 * i), j * 1.9 * r)), r)
            ops.append(stroke(TRAIT, c, 2.4))
            ops.append(fill("teinte", c))


def sarment(ops, path, w=TW, o=TO):
    """Sarment creux : contour brun, intérieur évidé (papier)."""
    ops.append(stroke(TRAIT, path, w + 2 * o, round=True))
    ops.append(stroke("paper", path, w, round=True))


def courbe(fn, u0, u1, step=4.0):
    n = max(2, int(math.ceil(abs(u1 - u0) / step)))
    return [(u0 + (u1 - u0) * i / n, fn(u0 + (u1 - u0) * i / n)) for i in range(n + 1)]


def onde(u):
    """Écart du sarment à l'axe le long d'une période (torsade) : sinusoïde aplatie au ventre."""
    t = u / P
    return AMP * (math.sin(math.pi * t) + 0.1 * math.sin(3 * math.pi * t)) / 0.9


def croisement(ops, u0, sg=1):
    """Brin du dessus au croisement en u0 : celui qui passe de v > 50 à v < 50 quand u croît (sg = 1). Le contour
    est plus court que le ruban, pour ne pas laisser de liseré au travers."""
    a = smooth(courbe(lambda u: 50 - sg * onde(u - u0), u0 - 10, u0 + 10, 2))
    b = smooth(courbe(lambda u: 50 - sg * onde(u - u0), u0 - 13, u0 + 13, 2))
    ops.append(stroke(TRAIT, a, TW + 2 * TO))
    ops.append(stroke("paper", b, TW))


def feuilles_croisement(ops, u0, avant=True, apres=True, L=24.0):
    """Les feuilles autour d'un croisement en u0, au-dehors des sarments : deux après (vers +u), deux avant."""
    for side in (-1, 1):
        for sens, ok in ((1, apres), (-1, avant)):
            if not ok:
                continue
            u = u0 + sens * 19
            v = 50 + side * onde(19)
            ang = side * (56 if sens > 0 else 124)
            feuille(ops, u + sens * 3, v + side * 6, ang, L, attache=(u, v))


# --------------------------------------------------------------------------- #
# Frise : une boucle d'un croisement au suivant
# --------------------------------------------------------------------------- #
def module_cote():
    base = []
    for side in (-1, 1):
        sarment(base, smooth(courbe(lambda u: 50 + side * onde(u), 0, P)))
    over = []
    croisement(over, 0)
    grappe(over, 52, 50, 0)
    feuille(over, 22, 50, 0, 30, attache=(10, 50))
    feuilles_croisement(over, 0, avant=False)
    feuilles_croisement(over, P, apres=False)
    return frise(P, base, over, outset=3, inset=3, symetrique=False)


# --------------------------------------------------------------------------- #
# Pièce centrale : mandorle, calice et hostie, gerbe de blé
# --------------------------------------------------------------------------- #
CW = 360.0
UC = 4.0              # centre de la mandorle
MA, MB = 83.0, 30.0   # demi-longueur et demi-largeur de la mandorle


def vesica(uc, a, b):
    """Mandorle de demi-longueur a (le long de u) et demi-largeur b, centrée en (uc, 50)."""
    R = (a * a + b * b) / (2 * b)
    t = math.degrees(math.asin((R - b) / R))
    p = arc(uc, 50 - b + R, R, -180 + t, -t)
    arc(uc, 50 + b - R, R, t, 180 - t, start=False, path=p)
    return p.Z()


def mandorle(ops):
    """Double filet : trait, filet clair, trait, puis la demi-teinte."""
    tip = 1.565   # 1 / sinus du demi-angle de la pointe
    for d, ink in ((0, TRAIT), (2.0, "paper"), (4.6, TRAIT), (6.0, "teinte")):
        ops.append(fill(ink, vesica(UC, MA - tip * d, MB - d)))


def calice(ops):
    """Calice en trait plein, coupe évasée, nœud, pied en cône ; l'hostie au-dessus. Le haut vers -u."""
    u = UC
    rim, depth = u - 18, 21
    ops.append(fill("paper", circle(u - 40, 50, 14)))
    ops.append(stroke(TRAIT, circle(u - 40, 50, 14), 1.4))
    cup = (Path().M((rim, 24)).L((rim, 76))
           .C((rim + 12, 74), (rim + depth, 62), (rim + depth, 50))
           .C((rim + depth, 38), (rim + 12, 26), (rim, 24)).Z())
    ops.append(fill(TRAIT, cup))
    # reflet de la coupe
    ops.append(fill("teinte", Path().M((rim + 2.2, 30)).C((rim + 5, 40), (rim + 5, 60), (rim + 2.2, 70))
                    .C((rim + 9, 62), (rim + 9, 38), (rim + 2.2, 30)).Z()))
    ops.append(fill(TRAIT, polygon([(rim + depth - 2, 46.5), (u + 34, 47.5), (u + 34, 52.5), (rim + depth - 2, 53.5)])))
    ops.append(fill(TRAIT, circle(rim + depth + 4, 50, 4.2)))
    ops.append(fill(TRAIT, Path().M((u + 30, 47)).C((u + 38, 44), (u + 41, 36), (u + 44, 31)).L((u + 47, 31))
                    .L((u + 47, 69)).L((u + 44, 69)).C((u + 41, 64), (u + 38, 56), (u + 30, 53)).Z()))


def epi(ops, x, y, ang, L=44):
    """Épi de blé de (x, y) (base des grains) vers `ang` : grains par paires, barbes."""
    f = affine(x, y, rot=ang)

    def grain(k):
        return (Path().M((0, 0)).C((0.3 * k, -0.22 * k), (0.8 * k, -0.2 * k), (k, 0))
                .C((0.8 * k, 0.2 * k), (0.3 * k, 0.22 * k), (0, 0)).Z())

    n = 5
    g = 0.27 * L
    for i in range(n):
        s = 0.05 * L + i * 0.14 * L
        k = g * (1 - 0.06 * i)
        for side in (-1, 1):
            t = affine(s, side * 0.5, rot=side * 24)
            ops += outlined("teinte", grain(k).transformed(t).transformed(f), outline=1.1, outline_ink=TRAIT)
            tip = t((k, 0))
            ops.append(stroke(TRAIT, Path().M(f(tip)).L(f((tip[0] + 9, tip[1] + side * 3.5))), 1.1, round=True))
    top = 0.05 * L + n * 0.14 * L - 2
    ops += outlined("teinte", grain(g * 0.85).transformed(affine(top, 0)).transformed(f), outline=1.1, outline_ink=TRAIT)
    ops.append(stroke(TRAIT, Path().M(f((top + g * 0.8, 0))).L(f((L + 8, 0))), 1.1, round=True))


def centre_cote():
    w = CW
    u0, u1 = -w / 2, w / 2
    ops = []

    # sarments : torsade près des croisements, puis le long des bords autour de la mandorle
    def ecart(u):
        a = min(u - u0, u1 - u)
        if a < P / 2:
            return onde(a)
        return AMP + 3 * math.sin(math.pi / 2 * (a - P / 2) / (w / 2 - P / 2))

    # gerbe derrière la mandorle : épis en haut, pieds en éventail en bas
    for a, b in (((UC - 92, 50), (UC + 40, 50)), ((UC - 78, 39), (UC + 20, 61)), ((UC - 78, 61), (UC + 20, 39))):
        ops.append(stroke(TRAIT, Path().M(a).L(b), 2.2))
    for dv in (-20, -9, 4, 15, 24):
        ops.append(stroke(TRAIT, Path().M((UC + 60, 50 + dv * 0.25)).L((UC + 142, 50 + dv)), 2.0, round=True))
    epi(ops, UC - 92, 50, -180, 50)
    epi(ops, UC - 78, 39, -160, 44)
    epi(ops, UC - 78, 61, 160, 44)

    for side in (-1, 1):
        sarment(ops, smooth(courbe(lambda u: 50 + side * ecart(u), u0, u1)))
    croisement(ops, u0)
    croisement(ops, u1)      # la frise continue dans le même sens après la pièce : même brin dessus qu'en u0
    mandorle(ops)
    calice(ops)
    feuilles_croisement(ops, u0, avant=False)
    feuilles_croisement(ops, u1, apres=False)
    # feuilles aux épaules et au pied de la mandorle, sur les sarments
    for side in (-1, 1):
        for u, ang in ((-64, 166), (76, 14)):
            v = 50 + side * ecart(u)
            feuille(ops, u, v + side * 2, side * ang, 24)
    return piece(w, ops, outset=3, inset=3)


# --------------------------------------------------------------------------- #
# Bouts, dessinés au début de la bande (le croisement en u = w) : volutes en haut, cœur au pied
# --------------------------------------------------------------------------- #
def volute(cu, cv, r0, r1, a0, turns, sg=1):
    """Points d'une spirale de centre (cu, cv), du rayon r0 à r1, depuis l'angle a0 (degrés) ; sg : sens."""
    n = int(16 * turns)
    return [(cu + (r0 + (r1 - r0) * i / n) * math.cos(math.radians(a0 + sg * 360 * turns * i / n)),
             cv + (r0 + (r1 - r0) * i / n) * math.sin(math.radians(a0 + sg * 360 * turns * i / n))) for i in range(1, n + 1)]


def bout_haut():
    """Fin de la vigne : les sarments s'ouvrent et s'enroulent en volutes vers l'axe ; une feuille au sommet."""
    w = 100.0
    ops = []
    for side in (-1, 1):
        pts = courbe(lambda u: 50 + side * onde(w - u), w, w - P / 2, 6)
        pts += [(w - P / 2 - 10, 50 + side * AMP)]
        pts += volute(22, 50 + side * (AMP - 11), 11, 3.5, 90 * side, 1.1, sg=side)
        sarment(ops, smooth(pts))
    croisement(ops, w)
    feuilles_croisement(ops, w, apres=False)
    feuille(ops, 50, 50, 180, 25, attache=(w - 12, 50))
    return boite(ops, w=w, h=100, outset=3, inset=3)


def bout_pied():
    """Départ de la vigne : les sarments naissent d'un cœur, deux crosses au fond ; une feuille pend dans le cœur."""
    w = 106.0
    ops = []
    half = courbe(lambda u: 50 + onde(w - u), w, w - P / 2, 6)
    half += [(34, 82), (22, 80.5), (12, 75), (6, 66), (3.5, 58)]
    coeur = [(u, 100 - v) for u, v in half] + [(3, 50)] + half[::-1]
    sarment(ops, smooth(coeur))
    croisement(ops, w, sg=-1)   # bout retourné par le moteur, au bout d'une frise qui n'est pas en miroir
    feuilles_croisement(ops, w, apres=False)
    for side in (-1, 1):
        pts = [(9, 50), (14, 50 + side * 4), (21, 50 + side * 9)]
        pts += volute(20, 50 + side * 17, 8, 2.5, 0, 0.9, sg=side)
        sarment(ops, smooth(pts), w=2.4, o=1.4)
    feuille(ops, 82, 50, 180, 34, attache=(w - 10, 50))
    return boite(ops, w=w, h=100, outset=3, inset=3)


# --------------------------------------------------------------------------- #
LAYOUT = {"margin": 16, "top": 28, "side": 28, "bottom": 28, "textPadding": 10}


def spec():
    return cadre(
        name="vigne_calice",
        title="Vigne et calice",
        source="paroissien, fin du XIXᵉ siècle, p.35 (Communion), photo bandeau-vigne-ble-calice",
        inks=INKS,
        palettes={},
        layout=LAYOUT,
        ground=None,
        disposition="laterale",
        top=module_cote(),
        centers={"side": centre_cote()},
        ends={"side": bouts(bout_haut(), bout_pied())},
    )
