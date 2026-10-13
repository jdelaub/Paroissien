"""
Bandeau « Lis, Vox Dei » : bande latérale seule, au bord extérieur de la page (paroissien de la fin du XIXᵉ siècle,
p.33, photo `lot2/bandeau-lis-vox-dei.jpg`, prise de côté : rotation 270 pour remettre la page droite). Gravure en
similigravure, une seule encre sépia sur papier nu. Un lis monte du pied au haut de la bande : au pied, une touffe
d'acanthe en deux rangs autour d'une tige cannelée ; puis des tiges à boutons, un lis ouvert, de longues feuilles ; au
tiers haut, un cartouche « VOX DEI » (traverse à volutes et rosette, tablette au pied en pointe, triangle renversé dont
les ailes débordent, triangle sombre à la pointe) ; au-dessus, la gerbe de feuilles s'effile jusqu'à la pointe.

Relevé (photo redressée, 1638 × 4000 px) : bande de 400 px de large (100 unités, 4 px par unité), longue de 2930 px
(7,3 largeurs) ; cartouche et triangle ≈ 400 px, centré à 38 % de la longueur depuis le haut ; touffe d'acanthe ≈ 670 px ;
pointe ≈ 400 px. Palette (papier mesuré #C4BFBC, corrigé vers #F1E8D6) : aplat d'encre #473327 à #5D4538, trame claire
#C5B4A5 à #D8CABB, trame moyenne ≈ #A08F81. Retenu : `tint` #CDBBA6 pour le lavis (seule encre atténuée en thème
sombre), `line` #5E4636 pour les ombres (flanc des boutons et du lis, triangle du cartouche), et l'encre « text » du
thème pour les contours et les lettres : sépia foncé en clair, clair en sombre, la gravure reste lisible partout.

Simplifications, pour une bande de 25 dp :
  - une seule tige droite, au lieu de trois tiges mêlées ; la frise répète une période de 180 unités (45 dp) : feuilles
    longues alternées, une plus courte qui croise, un bouton sur son pédoncule ; elle pousse vers le haut d'un seul élan
    (`symetrique=False`). Tout tient dans une période, pour que rien ne soit tranché aux bouts de la frise ; d'où des
    feuilles qui ne se croisent pas d'une période à l'autre comme en haut de la gravure, et des boutons au-dessus du
    cartouche aussi (la gravure n'y a que des feuilles) ;
  - un seul lis ouvert, au départ de la tige (pièce de bout du pied) ; acanthes en lames dentées, sans le modelé de la
    trame ; les tiges qui passent en V derrière le cartouche sont omises ;
  - l'inscription, sur une ligne dans la gravure, passe sur deux (« VOX » sur « DEI ») en capitales romaines dessinées
    (pleins et déliés, empattements, chasse réduite à 0,9) : 28 unités, soit 7 dp de haut. Pièce `droit` : elle se lit
    de même sur le recto et le verso ; elle déborde de 16 unités de chaque côté (ailes du triangle, volutes).
"""
import math

from commun import (Path, affine, arc, bouts, boite, cadre, circle, fill, frise, leaf, piece, polygon, rect, smooth,
                    spiral, stroke)

ORDRE = 33

INKS = {
    "line": "#5E4636",   # sépia de la gravure : ombres et accents
    "tint": "#CDBBA6",   # lavis sépia (la trame claire)
}
TRAIT = "text"           # contours et lettres : l'encre du texte du thème, lisible en sombre

SIDE = 25.0
OL = 1.8                 # contour des feuilles et boutons


# --------------------------------------------------------------------------- #
# Outils
# --------------------------------------------------------------------------- #
def _bez(a, b, c, d, t):
    s = 1 - t
    return (s ** 3 * a[0] + 3 * s * s * t * b[0] + 3 * s * t * t * c[0] + t ** 3 * d[0],
            s ** 3 * a[1] + 3 * s * s * t * b[1] + 3 * s * t * t * c[1] + t ** 3 * d[1])


def jalons(path, k=10):
    """Points le long d'un chemin (M, L, C), k par cubique."""
    pts, cur = [], None
    for c in path.cmds:
        if c[0] in "ML":
            cur = c[1]
            pts.append(cur)
        elif c[0] == "C":
            for i in range(1, k + 1):
                pts.append(_bez(cur, c[1], c[2], c[3], i / k))
            cur = c[3]
    return pts


def _normale(s, i):
    a, b = s[max(0, i - 1)], s[min(len(s) - 1, i + 1)]
    return math.atan2(b[1] - a[1], b[0] - a[0]) + math.pi / 2


def effile(pts, w0, w1):
    """Ruban effilé le long d'une courbe lisse : largeur w0 au départ, w1 à l'arrivée."""
    s = jalons(smooth(pts))
    n = len(s)
    left, right = [], []
    for i, (x, y) in enumerate(s):
        ang = _normale(s, i)
        w = (w0 + (w1 - w0) * i / (n - 1)) / 2
        left.append((x + w * math.cos(ang), y + w * math.sin(ang)))
        right.append((x - w * math.cos(ang), y - w * math.sin(ang)))
    return polygon(left + right[::-1])


def lame(pts, W, dents=0, prof=0.0, cote=1, base=0.18):
    """Feuille le long d'une courbe lisse : W au plus large (au tiers), pointue au bout, `base` (fraction de W) au
    départ. `dents` lobes pointus tournés vers la pointe, creusés de `prof` (fraction de la largeur), sur le bord
    `cote` : +1 à gauche du sens de tracé (côté concave d'une courbe qui tourne en sens horaire), -1 à droite, 0 les
    deux."""
    s = jalons(smooth(pts), 14)
    n = len(s)
    left, right = [], []
    for i, (x, y) in enumerate(s):
        t = i / (n - 1)
        ang = _normale(s, i)
        if t <= 0.36:
            w = W / 2 * (base + (1 - base) * math.sin(math.pi / 2 * t / 0.36))
        else:
            w = W / 2 * (1 - ((t - 0.36) / 0.64) ** 1.3)
        wl = wr = w
        if dents and 0.08 < t < 0.92:
            f = (t * dents) % 1.0
            cut = 1 - prof * ((1 - f / 0.85) ** 1.3 if f < 0.85 else (f - 0.85) / 0.15)
            if cote >= 0:
                wl = w * cut
            if cote <= 0:
                wr = w * cut
        left.append((x + wl * math.cos(ang), y + wl * math.sin(ang)))
        right.append((x - wr * math.cos(ang), y - wr * math.sin(ang)))
    return polygon(left + right[::-1])


def nervure(pts, t0=0.08, t1=0.82):
    s = jalons(smooth(pts), 14)
    n = len(s)
    return smooth(s[int(t0 * (n - 1)):int(t1 * (n - 1)) + 1][::3] + [s[int(t1 * (n - 1))]])


def inverse(path):
    """Un sous-chemin fermé parcouru en sens inverse : contre-forme d'une lettre (trou en nonzero comme en evenodd)."""
    start = path.cmds[0][1]
    segs = [c for c in path.cmds[1:] if c[0] != "Z"]
    if segs[-1][-1] != start:
        segs.append(("L", start))
    ends = [start] + [c[-1] for c in segs]
    q = Path().M(ends[-1])
    for i in range(len(segs) - 1, -1, -1):
        c = segs[i]
        if c[0] == "L":
            q.L(ends[i])
        else:
            q.C(c[2], c[1], ends[i])
    return q.Z()


class Dessin:
    """Tracés passés par une transformation `t` (repère de dessin -> repère du module)."""

    def __init__(self, t=None):
        self.ops, self.t = [], t

    def _p(self, p):
        return p.transformed(self.t) if self.t else p

    def fill(self, ink, p):
        self.ops.append(fill(ink, self._p(p)))

    def raw(self, ink, p):
        """Remplissage sans réorientation des sous-chemins (lettres à contre-forme)."""
        self.ops.append({"ink": ink, "d": self._p(p).d()})

    def stroke(self, ink, p, w, round=True):
        self.ops.append(stroke(ink, self._p(p), w, round))

    def outlined(self, ink, p, ol=OL):
        q = self._p(p)
        self.ops += [stroke(TRAIT, q, 2 * ol), fill(ink, q)]

    def tige(self, pts, w, ol=1.5):
        p = smooth(pts)
        self.stroke(TRAIT, p, w + 2 * ol)
        self.stroke("tint", p, w)

    def feuille(self, pts, W, **kw):
        self.outlined("tint", lame(pts, W, **kw))
        self.stroke(TRAIT, nervure(pts), 1.2)


# --------------------------------------------------------------------------- #
# Éléments végétaux
# --------------------------------------------------------------------------- #
def bouton(L, W):
    """Bouton de lis vers +x : base étroite, ventre aux deux cinquièmes, pointe."""
    return (Path().M((0, 0))
            .C((0.04 * L, -0.62 * W), (0.5 * L, -0.66 * W), (L, 0))
            .C((0.5 * L, 0.66 * W), (0.04 * L, 0.62 * W), (0, 0)).Z())


def bourgeon(d, x, y, rot, L, W, ombre=1):
    """Bouton de lis : la forme, une suture de pétales, une ombre au flanc `ombre` (±1), deux bractées."""
    t = affine(x, y, rot=rot)
    d.outlined("tint", bouton(L, W).transformed(t))
    o = ombre
    d.fill("line", (Path().M((0.12 * L, o * 0.2 * W)).C((0.35 * L, o * 0.38 * W), (0.7 * L, o * 0.3 * W), (0.97 * L, o * 0.02 * W))
                    .C((0.7 * L, o * 0.18 * W), (0.4 * L, o * 0.22 * W), (0.12 * L, o * 0.2 * W)).Z()).transformed(t))
    d.stroke(TRAIT, smooth([(0.2 * L, -o * 0.1 * W), (0.6 * L, -o * 0.1 * W), (0.95 * L, 0)]).transformed(t), 1.1)
    for sg in (-1, 1):
        d.outlined("tint", leaf(0.34 * L, 0.2 * W, bend=sg * 0.1).transformed(affine(x, y, rot=rot + sg * 30)), 1.2)


def lis_ouvert(d, x, y, rot, L, W):
    """Lis ouvert vu de profil, vers +x : trompe évasée, trois pétales à la gueule (le pétale de face dépasse), une
    ombre au flanc, les nervures des pétales, deux bractées."""
    t = affine(x, y, rot=rot)
    fleur = (Path().M((0, -0.08 * W)).C((0.45 * L, -0.1 * W), (0.72 * L, -0.28 * W), (L, -0.52 * W))
             .L((0.84 * L, -0.2 * W)).L((1.06 * L, 0)).L((0.84 * L, 0.2 * W)).L((L, 0.52 * W))
             .C((0.72 * L, 0.28 * W), (0.45 * L, 0.1 * W), (0, 0.08 * W)).Z())
    d.outlined("tint", fleur.transformed(t))
    d.fill("line", (Path().M((0.1 * L, 0.07 * W)).C((0.45 * L, 0.12 * W), (0.75 * L, 0.3 * W), (0.97 * L, 0.48 * W))
                    .C((0.7 * L, 0.2 * W), (0.45 * L, 0.06 * W), (0.1 * L, 0.07 * W)).Z()).transformed(t))
    for sg in (-1, 1):
        d.stroke(TRAIT, smooth([(0.1 * L, sg * 0.04 * W), (0.5 * L, sg * 0.08 * W), (0.82 * L, sg * 0.19 * W)])
                 .transformed(t), 1.1)
    for sg in (-1, 1):
        d.outlined("tint", leaf(0.26 * L, 0.16 * W, bend=sg * 0.1).transformed(affine(x, y, rot=rot + sg * 30)), 1.2)


# --------------------------------------------------------------------------- #
# Frise : la tige de lis, qui pousse vers le haut (-u) d'un bout à l'autre de la bande
# --------------------------------------------------------------------------- #
P = 180.0


def module_tige():
    """Une période de P : deux demi-périodes en alternance. Chacune porte, d'un côté, une longue feuille effilée qui
    s'écarte puis file le long du bord, doublée d'une plus courte qui la croise ; de l'autre, un bouton sur son
    pédoncule devant une feuille étroite. Dessinée pour pousser vers +u puis retournée (u -> P - u). Tout tient dans
    [0, P] : une instance ne déborde pas sur sa voisine, la découpe aux bouts de la frise ne tranche rien."""
    haut = lambda p: (P - p[0], p[1])
    base, over, stalk = Dessin(haut), Dessin(haut), Dessin(haut)
    h = P / 2
    for u0, s in ((0.0, -1), (h, 1)):
        b = -s
        base.feuille([(u0 + 8, 50), (u0 + 22, 50 + b * 28), (u0 + 44, 50 + b * 40), (u0 + 70, 50 + b * 45)], 12)
        base.feuille([(u0 + 2, 50), (u0 + 16, 50 + s * 22), (u0 + 40, 50 + s * 37), (u0 + 66, 50 + s * 44),
                      (u0 + 88, 50 + s * 46)], 17)
        base.feuille([(u0 + 34, 50), (u0 + 48, 50 + s * 15), (u0 + 68, 50 + s * 24), (u0 + 88, 50 + s * 27)], 11)
        over.tige([(u0 + 12, 50), (u0 + 24, 50 + b * 14), (u0 + 32, 50 + b * 21)], 2.6, 1.2)
        over.feuille([(u0 + 14, 50), (u0 + 28, 50 + b * 6), (u0 + 42, 50 + b * 7)], 7)
        bourgeon(over, u0 + 31, 50 + b * 21, b * 13, 34, 19, ombre=-b)
    stalk.tige([(-4, 50), (P + 4, 50)], 3.6)
    return frise(P, base.ops, stalk.ops + over.ops, outset=4, inset=4, symetrique=False, deborde=2)


# --------------------------------------------------------------------------- #
# Lettres : capitales romaines en chemins (hauteur 100, ligne de base y = 0, chasse en x)
# --------------------------------------------------------------------------- #
def _r(x0, y0, x1, y1):
    return rect(x0, y0, x1 - x0, y1 - y0)


def lettre_V():
    p = Path()
    p.extend(polygon([(4, -100), (21, -100), (46, 0), (38, 0)]))
    p.extend(polygon([(70, -100), (78, -100), (46, 0), (41, 0)]))
    p.extend(_r(-1, -100, 26, -96)).extend(_r(65, -100, 83, -96))
    return p, 82, False


def lettre_O():
    outer = arc(46, -50, 46, 0, 360, ry=51).Z()
    inner = inverse(arc(46, -50, 30, 0, 360, ry=43).Z())
    return outer.extend(inner), 92, True


def lettre_X():
    p = Path()
    p.extend(polygon([(2, -100), (19, -100), (78, 0), (61, 0)]))
    p.extend(polygon([(66, -100), (74, -100), (15, 0), (7, 0)]))
    p.extend(_r(-3, -100, 24, -96)).extend(_r(61, -100, 79, -96))
    p.extend(_r(2, -4, 22, 0)).extend(_r(56, -4, 83, 0))
    return p, 80, False


def lettre_D():
    outer = (Path().M((-2, -100)).L((40, -100)).C((70, -100), (86, -80), (86, -50))
             .C((86, -20), (70, 0), (40, 0)).L((-2, 0)).L((-2, -4)).L((4, -4)).L((4, -96)).L((-2, -96)).Z())
    inner = inverse(Path().M((19, -93)).L((40, -93)).C((60, -93), (70, -75), (70, -50))
                    .C((70, -25), (60, -7), (40, -7)).L((19, -7)).Z())
    return outer.extend(inner), 86, True


def lettre_E():
    p = Path()
    p.extend(_r(4, -100, 19, 0))
    p.extend(_r(-2, -100, 56, -93)).extend(_r(51, -100, 56, -86))
    p.extend(_r(19, -54, 46, -47)).extend(_r(43, -60, 47, -41))
    p.extend(_r(-2, -7, 60, 0)).extend(_r(55, -17, 60, 0))
    return p, 60, False


def lettre_I():
    p = Path()
    p.extend(_r(6, -100, 21, 0)).extend(_r(0, -100, 27, -96)).extend(_r(0, -4, 27, 0))
    return p, 27, False


LETTRES = {"V": lettre_V, "O": lettre_O, "X": lettre_X, "D": lettre_D, "E": lettre_E, "I": lettre_I}
APPROCHE = 10      # entre deux lettres (unités de lettre)
ETROIT = 0.9       # capitales un peu étroites, pour tenir dans la largeur de la bande


def chasse(mot):
    return sum(LETTRES[c]()[1] for c in mot) + APPROCHE * (len(mot) - 1)


def mot(d, texte, x_centre, y_base, cap):
    """Un mot centré sur x_centre, ligne de base y_base, capitales de hauteur `cap` (repère de dessin)."""
    s = cap / 100.0
    x = -chasse(texte) / 2
    for c in texte:
        p, w, creuse = LETTRES[c]()
        q = p.transformed(affine(x_centre + x * s * ETROIT, y_base, sx=s * ETROIT, sy=s))
        (d.raw if creuse else d.fill)(TRAIT, q)
        x += w + APPROCHE


# --------------------------------------------------------------------------- #
# Cartouche
# --------------------------------------------------------------------------- #
WC = 212.0     # longueur de la pièce le long de la bande
CAP = 28.0     # hauteur des capitales (7 dp pour une bande de 25 dp)


def rosette(d, cx, cy, r):
    p = Path()
    for k in range(6):
        p.extend(bouton(r, 0.62 * r).transformed(affine(cx, cy, rot=30 + 60 * k)))
    d.outlined("tint", p, 1.2)
    d.fill(TRAIT, circle(cx, cy, 0.3 * r))


def cartouche():
    """Cartouche à l'inscription, dessiné comme il se voit sur la bande de droite (recto), dans un repère d'écran :
    x vers la droite (-v), y vers le bas (+u), origine au centre de la pièce. La pièce « droite » garde cet aspect
    sur le verso. De haut en bas : rosette sur une traverse à volutes pendantes, tablette au pied en pointe, champ clair à « VOX / DEI », triangle renversé dont les ailes débordent de la bande."""
    d = Dessin(lambda p: (p[1], 50 - p[0]))
    h = WC / 2
    # tige qui traverse : elle relie la frise du dessus et celle du dessous
    d.tige([(0, -h - 4), (0, -60)], 3.6)
    d.tige([(0, 92), (0, h + 4)], 3.6)
    # triangle renversé, derrière la tablette
    d.outlined("tint", polygon([(-63, 34), (63, 34), (0, 100)]), 2.0)
    d.fill("line", polygon([(0, 93), (-14, 72), (14, 72)]))
    # tablette : flancs droits, pied en pointe
    T = 50
    body = (Path().M((-T, -47)).L((T, -47)).L((T, 42)).C((T, 58), (22, 64), (0, 76)).C((-22, 64), (-T, 58), (-T, 42))
            .Z())
    d.outlined("tint", body, 2.0)
    d.stroke(TRAIT, Path().M((-26, 58)).C((-12, 62), (-6, 66), (0, 70)).C((6, 66), (12, 62), (26, 58)), 1.4)
    # traverse à volutes pendantes, rosette au milieu
    for sg in (-1, 1):
        v = spiral(0, 0, 1.5, 9.0, 1.2, a0=-90).transformed(lambda p, sg=sg: (sg * (57 + p[0]), -42 + p[1]))
        d.stroke(TRAIT, v, 4.6)
        d.stroke("tint", v, 1.4)
    d.outlined("tint", _bar(-60, -56, 60, -47), 1.6)
    rosette(d, 0, -55, 12)
    # champ et inscription
    field = (Path().M((-33, -38)).L((33, -38)).C((40, -38), (44, -34), (44, -27))
             .L((44, 36)).C((44, 43), (40, 47), (33, 47)).L((-33, 47)).C((-40, 47), (-44, 43), (-44, 36))
             .L((-44, -27)).C((-44, -34), (-40, -38), (-33, -38)).Z())
    d.stroke(TRAIT, field, 2.4)
    d.fill("paper", field)
    mot(d, "VOX", 0, -3, CAP)
    mot(d, "DEI", 0, 38, CAP)
    return piece(WC, d.ops, outset=16, inset=16, droit=True, a=0.38)


def _bar(x0, y0, x1, y1):
    r = (y1 - y0) / 2
    return (Path().M((x0 + r, y0)).L((x1 - r, y0)).C((x1, y0), (x1, y1), (x1 - r, y1)).L((x0 + r, y1))
            .C((x0, y1), (x0, y0), (x0 + r, y0)).Z())


# --------------------------------------------------------------------------- #
# Bouts : pointe de feuilles en haut, touffe d'acanthe au pied
# --------------------------------------------------------------------------- #
W_POINTE = 220.0


def pointe():
    """Haut de la bande : la tige s'achève en gerbe de longues feuilles dressées, de plus en plus courtes, et une
    feuille terminale. Dessinée au début de la bande (u = 0 au bout) ; la tige arrive de u = W_POINTE."""
    d = Dessin()
    W = W_POINTE
    d.fill(TRAIT, effile([(W + 4, 50), (120, 50), (36, 50)], 6.6, 3.4))
    d.fill("tint", effile([(W + 4, 50), (120, 50), (38, 50)], 3.6, 1.2))
    for u0, s, L, wd in ((206, -1, 86, 20), (176, 1, 80, 19), (144, -1, 72, 17), (114, 1, 64, 15), (86, -1, 54, 13),
                         (62, 1, 44, 11)):
        dv = min(42, 0.55 * L)
        d.feuille([(u0, 50), (u0 - 0.22 * L, 50 + s * 0.6 * dv), (u0 - 0.55 * L, 50 + s * 0.92 * dv),
                   (u0 - L, 50 + s * dv)], wd)
    d.feuille([(44, 50), (24, 49), (2, 50)], 10)
    return boite(d.ops, w=W, outset=4, inset=4)


def miroir_v(p):
    return (p[0], 100 - p[1])


W_RACINE = 330.0


def racine():
    """Pied de la bande : touffe d'acanthe d'où part la tige. Dessinée au début de la bande (u = 0 au bout), la tige
    vers +u ; le moteur la retourne au pied. Deux rangs de grandes acanthes qui s'écartent jusqu'au bord et
    retombent, de petites feuilles au bout ; au-dessus, un lis ouvert d'un côté, un bouton de l'autre, et les
    premières feuilles de la tige."""
    d = Dessin()
    W = W_RACINE
    # tige cannelée, effilée
    d.fill(TRAIT, effile([(12, 50), (150, 50), (W + 4, 50)], 22, 7.8))
    d.fill("tint", effile([(15, 50), (150, 50), (W + 4, 50)], 17.5, 3.6))
    for dv in (-3.6, 3.6):
        d.stroke(TRAIT, smooth([(24, 50 + dv), (130, 50 + dv * 0.75), (215, 50 + dv * 0.45)]), 1.0)
    # lis ouvert, bouton, premières feuilles
    d.feuille([(226, 50), (244, 24), (270, 10), (300, 5), (328, 4)], 16)
    d.feuille([(230, 50), (250, 76), (280, 90), (326, 95)], 16)
    d.tige([(236, 50), (254, 40), (266, 33)], 2.6, 1.2)
    bourgeon(d, 265, 33, -14, 34, 19, ombre=1)
    d.tige([(226, 50), (242, 58), (254, 63)], 2.8, 1.2)
    lis_ouvert(d, 253, 63, 10, 50, 36)
    for pts, larg, n in (
        ([(40, 54), (33, 64), (22, 74), (10, 80), (2, 80)], 18, 2),                                    # bout
        ([(125, 54), (121, 68), (110, 80), (94, 88), (76, 91), (60, 88)], 40, 4),                      # grand rang
        ([(210, 54), (207, 66), (198, 78), (184, 85), (168, 87), (155, 83)], 34, 3),                   # second rang
    ):
        for f, cote in ((lambda p: p, -1), (miroir_v, 1)):
            q = [f(p) for p in pts]
            d.outlined("tint", lame(q, larg, dents=n, prof=0.5, cote=cote, base=0.4))
            d.stroke(TRAIT, nervure(q, 0.1, 0.7), 1.2)
    d.outlined("tint", leaf(34, 13).transformed(affine(42, 50, rot=180)))
    for u in (125, 210):
        d.outlined("tint", (Path().M((u - 6, 50 - 11)).C((u + 2, 50 - 12), (u + 2, 50 + 12), (u - 6, 50 + 11))
                            .C((u - 1, 50 + 4), (u - 1, 50 - 4), (u - 6, 50 - 11)).Z()), 1.3)
    return boite(d.ops, w=W, outset=4, inset=4)


def spec():
    return cadre(
        name="lis_vox_dei",
        title="Lis, Vox Dei",
        source="paroissien, p.33, bandeau latéral « Vox Dei », photo bandeau-lis-vox-dei",
        inks=INKS,
        palettes={},
        dimmed=["tint"],
        layout={"margin": 12, "top": SIDE, "side": SIDE, "bottom": SIDE, "textPadding": 10},
        ground=None,
        disposition="laterale",
        top=module_tige(),
        centers={"side": cartouche()},
        ends={"side": bouts(pointe(), racine())},
    )
