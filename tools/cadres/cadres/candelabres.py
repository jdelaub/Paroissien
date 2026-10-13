"""
Cadre « Candélabres » : gravure sur bois de bout à l'encre noire, imprimée avec le texte, dans l'Ordinaire de la messe
(p.96-97) et la Table des matières du même paroissien. Un candélabre de la Renaissance occupe toute la hauteur du côté
extérieur ; au pied, un panneau de rinceaux d'acanthe à clochettes entoure une corbeille à anses gardée par deux
griffons. Chacun a son cadre de filets ; un petit jour les sépare.

Photos (lot 2), prises de côté : `table-candelabres-et-rinceaux.jpg` et `pages-candelabres-ordinaire-messe.jpg`,
toutes deux à tourner de 270° (sens horaire) : le haut de la page est à droite de la photo. Les trois pages montrent
le même bois, jamais retourné : colonne à gauche sur la table (verso, la couture à droite) et sur la p.96, à droite
sur la p.97 ; panneau au pied, de la colonne au bord du texte. Disposition « equerre_pied » (côté extérieur et pied,
en L), et non en Γ : la rangée de fleurons au-dessus du titre de la table est typographique, pas du cadre.

Relevé (table, 1638 × 4000 px une fois tournée ; la colonne fait 100 unités) : colonne 175 px de large sur 2205 de
haut (12,6 largeurs), panneau 182 px de haut sur 975 de long (5,4 hauteurs) : côté et pied ont la même épaisseur ;
jour entre colonne et panneau ≈ 8 unités. La colonne, de haut en bas : chapiteau en corbeille à volutes et
pendeloques, urne couverte aux draperies, trompe (0-240) ; lyre de deux rinceaux d'acanthe (240-360) ; chérubin sur
une coupe, guirlandes de perles, hostie IHS posée dans un calice à godrons, tulipes (360-750) ; gerbe d'acanthe à
rubans, coupe à godrons, dauphins affrontés, nœud à facettes (750-1110) ; grand vase sur sa plinthe (1110-1260).
Une seule encre : les 5 % les plus sombres mesurent #6F7373 après correction du blanc (traits fins, flous) ; c'est
l'encre du texte, d'où l'encre de thème « text » (sépia foncé en clair, claire en sombre). Pas de fond, pas de
palettes liturgiques.

Composition. Sur un téléphone la colonne est deux fois plus longue (≈ 24 largeurs) : un candélabre unique ne
s'étire pas, il se compose de pièces fixes et d'un étage répété dans le même sens (`symetrique=False`) :
  - bout haut : le chapiteau et l'urne ;
  - pièce centrale « droite », aux 45 % de la hauteur comme sur la gravure : chérubin, hostie IHS (lisible sur un
    verso), calice et tulipes, puis la gerbe à rubans sur son petit vase ;
  - bout bas et coin du pied, tirés du même dessin découpé en deux : trompe, dauphins, nœud, gerbe, vase, plinthe ;
  - entre eux, l'étage (215 unités) : trompe et plateau, lyre d'acanthe, nœud dans une coupe à godrons, bourgeons ;
    il s'en loge 2 à 5 de chaque côté de la pièce centrale selon la hauteur de la page.
Au pied, la corbeille et les griffons font la pièce centrale ; le rinceau (145 unités, une volute par période) s'y
répète en miroir, 2 ou 3 fois par côté sur un téléphone. Ses bouts, dessinés par-dessus les filets, ouvrent le jour
et ferment le panneau. Les longs filets sont ceux du moteur (deux L, à 1,4 unité des bords).

Simplifications : contours nets et peu de tailles ; l'ombre des pièces tournées devient quelques hachures sur les deux
bords (le moteur retourne la colonne sur un verso) ; l'acanthe, une bande festonnée hachurée d'un côté ; griffons et
dauphins stylisés ; les cordons que le chérubin tient dans sa bouche, deux traits.
"""
import math

from commun import Path, affine, arc, boite, bouts, cadre, circle, ellipse, filet, fill, frise, piece, polygon, \
    rect, smooth, stroke

ORDRE = 96

INK = "text"      # encre du texte du thème : le cadre est imprimé avec le texte
S = 30.0          # côté et pied (dp)
AXE = 50.0        # axe du candélabre (profondeur)
RB = 1.4          # filets : distance du bord (unités)
RW = 2.0          # filets : épaisseur
TR = 1.6          # contour visible des formes
FIN = 1.1         # traits de détail
TIGE = 3.6        # demi-largeur de la tige du candélabre


class G:
    """Tracés dans un repère local ; `t` passe au repère du module. `fenetre` = (y0, y1) : seuls les tracés qui
    touchent cette tranche du repère local sont gardés (une pièce découpée en deux modules n'emporte que sa part)."""

    def __init__(self, t=None, fenetre=None):
        self.ops, self.t, self.fenetre = [], t, fenetre

    def _p(self, path):
        return path.transformed(self.t) if self.t else path

    def _garde(self, path, w=0.0):
        if not self.fenetre:
            return True
        ys = [p[1] for c in path.cmds for p in c[1:]]
        return ys and max(ys) + w >= self.fenetre[0] and min(ys) - w <= self.fenetre[1]

    def contour(self, path, w=TR):
        """Forme évidée (papier) cernée d'un trait."""
        if self._garde(path, w):
            p = self._p(path)
            self.ops += [stroke(INK, p, 2 * w), fill("paper", p)]

    def trait(self, path, w=FIN, round=True):
        if self._garde(path, w):
            self.ops.append(stroke(INK, self._p(path), w, round))

    def plein(self, path):
        if self._garde(path):
            self.ops.append(fill(INK, self._p(path)))

    def ruban(self, path, w, bord=TR):
        """Tige gravée : deux traits parallèles (un trait large, évidé)."""
        if self._garde(path, w):
            p = self._p(path)
            self.ops += [stroke(INK, p, w + 2 * bord, True), stroke("paper", p, w, True)]


def miroir(s):
    """Identité (s > 0) ou miroir gauche-droite (s < 0) d'un point."""
    return (lambda p: p) if s > 0 else (lambda p: (-p[0], p[1]))


# --------------------------------------------------------------------------- #
# Formes
# --------------------------------------------------------------------------- #
def galbe(profile):
    """Silhouette symétrique d'après son profil droit [(demi-largeur, y)], de haut en bas."""
    right = smooth(profile)
    left = smooth([(-x, y) for x, y in reversed(profile)])
    p = Path()
    p.cmds = right.cmds + [("L", left.cmds[0][1])] + left.cmds[1:] + [("Z",)]
    return p


def profil(profile, y):
    """Demi-largeur d'un profil à la hauteur y (interpolation linéaire)."""
    for (x0, y0), (x1, y1) in zip(profile, profile[1:]):
        if y0 <= y <= y1:
            t = (y - y0) / (y1 - y0) if y1 != y0 else 0
            return x0 + (x1 - x0) * t
    return profile[-1][0] if y > profile[-1][1] else profile[0][0]


def vase(g, prof, ombre=True, pas=2.9):
    """Corps tourné : silhouette, et hachures d'ombre sur les deux bords (le dessin est retourné sur un verso)."""
    g.contour(galbe(prof))
    if ombre:
        y0, y1 = prof[0][1], prof[-1][1]
        y = y0 + pas * 0.7
        h = Path()
        while y < y1 - pas * 0.4:
            hw = profil(prof, y)
            if hw > 6:
                for s in (-1, 1):
                    h.M((s * 0.55 * hw, y)).L((s * 0.92 * hw, y))
            y += pas
        if h.cmds:
            g.trait(h, 0.8, round=False)


def godrons(g, prof, y0, y1, n=7):
    """Godrons : côtes qui suivent la panse de y0 à y1."""
    cotes = Path()
    for i in range(n):
        c = -0.8 + 1.6 * i / (n - 1)
        ys = [y0 + (y1 - y0) * t for t in (0, 0.35, 0.7, 1)]
        cotes.extend(smooth([(c * profil(prof, y), y) for y in ys]))
    g.trait(cotes, FIN)


def tige(g, y0, y1, hw=TIGE):
    """Fût du candélabre, de y0 à y1."""
    g.contour(rect(-hw, y0, 2 * hw, y1 - y0))


def bague(g, y, hw, h=3.2):
    g.contour(ellipse(0, y, hw, h))


def perle(g, x, y, r=3.0):
    g.contour(circle(x, y, r), 1.1)


def volute_pts(c, r0, r1, tours, a0, sens=1, n_tour=14):
    """Points d'une spirale de centre c, du rayon r0 (angle a0, degrés) au rayon r1 ; sens = 1 horaire à l'écran."""
    n = max(4, int(tours * n_tour))
    out = []
    for i in range(n + 1):
        t = i / n
        a = math.radians(a0 + sens * 360 * tours * t)
        r = r0 + (r1 - r0) * t
        out.append((c[0] + r * math.cos(a), c[1] + r * math.sin(a)))
    return out


def _bez(a, b, c, d, t):
    s = 1 - t
    return (s ** 3 * a[0] + 3 * s * s * t * b[0] + 3 * s * t * t * c[0] + t ** 3 * d[0],
            s ** 3 * a[1] + 3 * s * s * t * b[1] + 3 * s * t * t * c[1] + t ** 3 * d[1])


def jalons(path, k=8):
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


def bande(pts, w0, w1):
    """Bande de largeur décroissante (w0 → w1) le long d'une ligne de points. Rend le polygone, et pour chaque point
    (abscisse relative, point, tangente, normale, demi-largeur)."""
    s = [0.0]
    for a, b in zip(pts, pts[1:]):
        s.append(s[-1] + math.hypot(b[0] - a[0], b[1] - a[1]))
    tot = s[-1] or 1.0
    gauche, droite, info = [], [], []
    n = len(pts)
    for i, p in enumerate(pts):
        a, b = pts[max(0, i - 1)], pts[min(n - 1, i + 1)]
        dx, dy = b[0] - a[0], b[1] - a[1]
        d = math.hypot(dx, dy) or 1.0
        tx, ty = dx / d, dy / d
        nx, ny = -ty, tx
        t = s[i] / tot
        hw = (w1 + (w0 - w1) * (1 - t)) / 2
        gauche.append((p[0] + nx * hw, p[1] + ny * hw))
        droite.append((p[0] - nx * hw, p[1] - ny * hw))
        info.append((t, p, (tx, ty), (nx, ny), hw))
    return polygon(gauche + droite[::-1]), info


def tige_acanthe(g, centre, w0, w1, lobes=(), cote=1, taille=11.0, oeil=True, pas=3.0):
    """Tige d'acanthe qui s'amincit (w0 → w1) et finit en volute ; du côté `cote` (1 : à gauche du sens de tracé
    dans le repère local, -1 : à droite), un bord festonné de lobes ronds, chaque creux marqué d'une incision.
    Silhouette cernée, nervure fine, et l'autre moitié ombrée de tailles en travers (tous les `pas`)."""
    pts = jalons(centre, 5)
    corps, info = bande(pts, w0, w1)
    sil = Path().extend(corps)
    creux = Path()
    for tl in lobes:
        i = min(range(len(info)), key=lambda j: abs(info[j][0] - tl))
        t, p, (tx, ty), (nx, ny), hw = info[i]
        r = 0.5 * taille * (1 - 0.45 * tl)
        c = (p[0] + cote * nx * (hw + 0.1 * r), p[1] + cote * ny * (hw + 0.1 * r))
        sil.extend(circle(c[0], c[1], r))
        # incision dans le creux qui suit le lobe
        e = (c[0] + tx * r * 0.95, c[1] + ty * r * 0.95)
        creux.M(e).L((e[0] - cote * nx * r * 0.9, e[1] - cote * ny * r * 0.9))
    g.contour(sil)
    if creux.cmds:
        g.trait(creux, 0.9)
    g.trait(smooth(pts[: int(len(pts) * 0.8)]), 0.9)
    if pas:
        ombre, s, prochain = Path(), 0.0, 0.0
        for j, (t, p, _, (nx, ny), hw) in enumerate(info):
            if j:
                s += math.hypot(p[0] - pts[j - 1][0], p[1] - pts[j - 1][1])
            if s >= prochain and hw > 2.4:
                prochain = s + pas
                ombre.M((p[0] - cote * nx * hw * 0.95, p[1] - cote * ny * hw * 0.95))
                ombre.L((p[0] - cote * nx * hw * 0.25, p[1] - cote * ny * hw * 0.25))
        if ombre.cmds:
            g.trait(ombre, 0.75, round=False)
    if oeil:
        e = pts[-1]
        g.plein(circle(e[0], e[1], max(1.6, 0.6 * w1)))


def feuille(L, W):
    """Feuille d'acanthe vers +x, base en (0, 0), deux lobes de chaque côté."""
    return (Path().M((0, 0))
            .C((0.1 * L, -0.5 * W), (0.25 * L, -0.6 * W), (0.36 * L, -0.42 * W))
            .C((0.42 * L, -0.62 * W), (0.6 * L, -0.6 * W), (0.66 * L, -0.34 * W))
            .C((0.75 * L, -0.42 * W), (0.9 * L, -0.3 * W), (L, 0))
            .C((0.9 * L, 0.3 * W), (0.75 * L, 0.42 * W), (0.66 * L, 0.34 * W))
            .C((0.6 * L, 0.6 * W), (0.42 * L, 0.62 * W), (0.36 * L, 0.42 * W))
            .C((0.25 * L, 0.6 * W), (0.1 * L, 0.5 * W), (0, 0)).Z())


def acanthe(g, x, y, rot, L, W):
    """Feuille d'acanthe cernée, avec sa nervure, posée en (x, y) et tournée de `rot` degrés."""
    t = affine(x, y, rot=rot)
    g.contour(feuille(L, W).transformed(t))
    g.trait(smooth([(0.08 * L, 0), (0.5 * L, 0), (0.88 * L, 0)]).transformed(t), 0.9)


def trefle(g, x, y, r=2.6, rot=-90):
    """Bourgeon trilobé (trois boules), pointe vers `rot`."""
    a = math.radians(rot)
    for da in (-55, 0, 55):
        b = a + math.radians(da)
        d = r * (1.5 if da == 0 else 1.25)
        perle(g, x + d * math.cos(b), y + d * math.sin(b), r)


def tulipe(g, x, y, h=10.0, rot=-90):
    """Clochette (tulipe) ouverte vers `rot`, base en (x, y)."""
    w = 0.55 * h
    p = (Path().M((0, -w)).C((0.35 * h, -w), (0.8 * h, -0.9 * w), (h, -w)).L((0.8 * h, -0.2 * w))
         .L((h, 0.1 * w)).L((0.8 * h, 0.35 * w)).L((h, w)).C((0.8 * h, 0.9 * w), (0.35 * h, w), (0, w)).Z())
    g.contour(p.transformed(affine(x, y, rot=rot)), 1.1)


# --------------------------------------------------------------------------- #
# Candélabre : repère « debout » (x depuis l'axe, y vers le bas)
# --------------------------------------------------------------------------- #
def debout():
    """Repère d'une pièce de la colonne : u = y (vers le bas), v = AXE + x (x > 0 vers le texte sur la bande de droite)."""
    return G(lambda p: (p[1], AXE + p[0]))


W_TOP = 205.0


def couronnement():
    """Haut de la colonne : chapiteau en corbeille à volutes et pendeloques, urne couverte aux draperies."""
    g = debout()
    tige(g, 168, W_TOP + 2)
    bague(g, 186, 9)
    for s in (-1, 1):
        g.trait(Path().M((s * 41, 30)).L((s * 41, 72)), 0.9)
        perle(g, s * 41, 52, 3.2)
        perle(g, s * 41, 74, 3.2)
    body = [(36, 18), (33, 40), (28, 62), (25, 80), (25, 90)]
    g.contour(galbe(body))
    cannelures = Path()
    for x in (-19, -13, -7, 7, 13, 19):
        cannelures.M((x, 56)).L((x * 0.95, 89))
    g.trait(cannelures, FIN)
    for s in (-1, 1):
        f = miroir(s)
        c = smooth([f(p) for p in [(4, 14), (20, 9)] + volute_pts((32, 24), 14, 3, 1.15, -100, sens=1)])
        g.ruban(c, 4.0)
    acanthe(g, 0, 8, 90, 64, 28)
    g.contour(rect(-28, 88, 56, 9))
    g.contour(rect(-21, 97, 42, 6))
    # urne couverte, anneaux et draperies
    g.contour(rect(-7, 103, 14, 5))
    for s in (-1, 1):
        d = [(22, 120), (31, 125), (36, 140), (38, 160), (39, 178), (35, 188), (32, 181), (29, 190), (25, 182),
             (26, 165), (24, 145)]
        g.contour(smooth([(s * x, y) for x, y in d], closed=True))
        g.trait(smooth([(s * 29, 132), (s * 32, 160), (s * 32, 180)]), 0.9)
    vase(g, [(5, 107), (12, 112), (18, 118), (22, 125), (24, 136), (23, 147), (18, 158), (10, 166), (6, 170)])
    g.trait(Path().M((-22, 126)).L((22, 126)), FIN)
    for s in (-1, 1):
        g.trait(circle(s * 23, 121, 4.2), 2.4)
    g.trait(Path().M((-AXE, RB)).L((AXE, RB)), RW, round=False)
    return boite(g.ops, w=W_TOP)


P_ETAGE = 215.0


def etage():
    """Un étage du fût, de haut en bas : trompe et plateau, lyre de deux rinceaux d'acanthe enroulés vers l'axe,
    nœud à facettes posé dans une coupe à godrons, tige et bourgeons."""
    P = P_ETAGE
    g = debout()
    tige(g, -2, P + 2)
    # trompe et plateau
    vase(g, [(4, 0), (5, 12), (9, 24), (16, 32), (21, 36)])
    g.contour(rect(-25, 35, 50, 6))
    # lyre
    for s in (-1, 1):
        f = miroir(s)
        c = [(7, 43), (24, 46), (36, 56), (41, 72), (41, 88)] + volute_pts((27, 100), 14, 3, 1.0, 0, sens=1)[1:]
        tige_acanthe(g, smooth([f(p) for p in c]), 9, 2.5, lobes=(0.18, 0.36, 0.54), cote=-s, taille=12)
    bague(g, 62, 6, 2.4)
    trefle(g, 0, 74, 3.0, rot=-90)
    # nœud et coupe
    g.contour(polygon([(-17, 126), (-11, 117), (11, 117), (17, 126), (11, 133), (-11, 133)]))
    for x in (-11, 11):
        g.trait(Path().M((x, 117)).L((x, 133)), FIN)
    coupe = [(33, 137), (31, 144), (25, 152), (15, 158), (7, 161)]
    g.contour(galbe(coupe))
    godrons(g, coupe, 144, 160, 7)
    g.contour(rect(-35, 133, 70, 5))
    # tige, bague et bourgeons
    bague(g, 172, 8)
    for s in (-1, 1):
        g.trait(smooth([(s * 4, 182), (s * 18, 190), (s * 30, 186), (s * 34, 176)]), FIN)
        trefle(g, s * 35, 172, 2.6, rot=-90)
    bague(g, 200, 11, 3)
    return frise(P, g.ops, symetrique=False)


W_CENTRE = 480.0


def ihs(g, cx, cy):
    """Monogramme IHS en capitales, dessiné pour la bande de droite (le x du repère « debout » y va vers la gauche)."""
    m = lambda pts: smooth([(-(cx + x), cy + y) for x, y in pts])
    ln = lambda a, b, w: g.trait(Path().M((-(cx + a[0]), cy + a[1])).L((-(cx + b[0]), cy + b[1])), w, round=False)
    for x in (-12, -3, 6):
        ln((x, -8), (x, 8), 2.6)
        ln((x - 2.6, -8), (x + 2.6, -8), 1.1)
        ln((x - 2.6, 8), (x + 2.6, 8), 1.1)
    ln((-3, 0), (6, 0), 1.3)
    g.trait(m([(16.5, -5.5), (14, -8), (11, -7.6), (9.6, -4.5), (12, -1), (15.4, 2.5), (14.4, 7), (11, 8),
               (8.6, 6)]), 2.2)


def cherubin(g, y):
    """Tête d'ange ailée, centrée en (0, y) : ailes déployées vers le haut, bord festonné de plumes."""
    for s in (-1, 1):
        f = lambda p: (s * p[0], y + p[1])
        aile = smooth([f(p) for p in [(6, -2), (10, -18), (18, -30), (30, -36), (40, -32), (44, -20), (42, -6),
                                      (34, 4), (20, 8), (10, 6)]], closed=True)
        for p in ((22, -35), (32, -35), (40, -29), (44, -19), (42, -8), (35, 2), (25, 7)):
            aile.extend(circle(*f(p), 5.2))
        g.contour(aile)
        for p in ((26, -28), (36, -24), (38, -12), (32, -2)):
            g.trait(smooth([f((10, -2)), f(((10 + p[0]) / 2, (p[1] - 2) / 2 - 2)), f(p)]), 0.9)
    g.contour(circle(0, y, 15))
    g.trait(arc(0, y, 15, 200, 340), 2.2)
    for s in (-1, 1):
        g.plein(circle(s * 5.5, y - 1, 1.3))
    g.trait(Path().M((0, y + 1)).L((0, y + 4)), 0.9)
    g.plein(ellipse(0, y + 8, 2.2, 1.4))


def centre():
    """Pièce du milieu : chérubin sur une coupe, guirlandes de perles, hostie IHS sur le calice, tulipes ; sous le
    pied, une gerbe d'acanthe à rubans qui sort d'un petit vase."""
    g = debout()
    H = W_CENTRE / 2
    tige(g, -H - 2, -205)
    bague(g, -228, 9)
    tige(g, 96, H + 2)
    # coupe du chérubin
    tige(g, -136, -112, 4)
    bague(g, -124, 8, 3)
    g.contour(ellipse(0, -111, 13, 3))
    vase(g, [(28, -164), (26, -152), (20, -142), (12, -136), (6, -133)])
    g.contour(rect(-31, -170, 62, 6))
    # guirlandes de perles et cordons tenus par le chérubin
    for s in (-1, 1):
        g.trait(Path().M((s * 2, -180)).L((s * 33, -166)), 0.8)
        pts = jalons(smooth([(s * 34, -166), (s * 43, -140), (s * 40, -114), (s * 28, -98)]))
        step = max(1, len(pts) // 8)
        for p in pts[::step]:
            perle(g, p[0], p[1], 2.8)
    cherubin(g, -190)
    # hostie IHS
    g.contour(circle(0, -72, 27))
    g.trait(circle(0, -72, 22.5), 0.9)
    ihs(g, 0, -72)
    # calice
    bowl = [(35, -38), (34, -26), (31, -14), (27, -2), (19, 8), (10, 14), (5, 16)]
    g.contour(galbe(bowl))
    feston = Path()
    for i in range(8):
        arc(-28 + 8 * i, -15, 4, 180, 360, path=feston)
    g.trait(feston, FIN)
    godrons(g, bowl, -12, 14, 7)
    g.contour(rect(-37, -44, 74, 6))
    tige(g, 16, 78, 4)
    bague(g, 28, 7, 2.5)
    g.contour(ellipse(0, 44, 10, 5))
    bague(g, 60, 7, 2.5)
    for s in (-1, 1):
        g.trait(smooth([(s * 6, 80), (s * 20, 72), (s * 28, 58), (s * 29, 48)]), FIN)
        tulipe(g, s * 29, 50, 11)
        trefle(g, s * 18, 68, 2.6, rot=-90 + s * 40)
    g.contour(galbe([(6, 74), (14, 79), (26, 84), (33, 89)]))
    g.contour(rect(-34, 89, 68, 6))
    # gerbe d'acanthe : bouton au milieu, plumets retombants et rubans, sur un petit vase
    for s in (-1, 1):
        f = miroir(s)
        g.trait(smooth([f(p) for p in [(43, 150), (40, 160), (45, 170), (40, 180), (44, 190)]]), 1.6)
        c = [(4, 182), (12, 158), (24, 136), (36, 126), (45, 130), (47, 140)] + volute_pts((41, 142), 6, 2, 0.7, 0,
                                                                                        sens=1)[1:]
        tige_acanthe(g, smooth([f(p) for p in c]), 8, 2.4, lobes=(0.3, 0.5, 0.68), cote=-s, taille=10)
        c = [(3, 180), (8, 150), (14, 128), (20, 114)]
        tige_acanthe(g, smooth([f(p) for p in c]), 6, 2.2, lobes=(), oeil=False)
    g.contour(Path().M((0, 100)).C((10, 112), (10, 126), (0, 132)).C((-10, 126), (-10, 112), (0, 100)).Z())
    g.trait(Path().M((0, 106)).L((0, 128)), 0.9)
    g.contour(rect(-13, 176, 26, 5))
    vase(g, [(9, 181), (14, 190), (13, 198), (7, 204), (4, 207)])
    bague(g, 218, 8)
    return piece(W_CENTRE, g.ops, droit=True, a=0.45)


# --------------------------------------------------------------------------- #
# Socle : vase sur sa plinthe, dans le coin du pied et le bout bas de la bande latérale. Repère « debout », y = 0 au
# bord bas du cadre, y négatif vers le haut.
# --------------------------------------------------------------------------- #
W_SOCLE = 200.0


def dauphin(g, f):
    """Dauphin de droite, stylisé en rinceau : la tête contre la trompe, au-dessus du nœud, le museau vers le bas ; le
    corps monte vers le dehors et la queue s'enroule."""
    c = [(10, -250), (20, -258), (30, -270), (36, -284)] + volute_pts((42, -284), 6, 2, 0.8, 180, sens=1)[1:]
    tige_acanthe(g, smooth([f(p) for p in c]), 11, 2.4, lobes=(0.4, 0.62), cote=-1 if f((1, 0))[0] > 0 else 1,
                 taille=8)
    g.contour(smooth([f(p) for p in [(6, -246), (12, -256), (20, -256), (22, -248), (16, -242), (10, -238)]],
                     closed=True))
    g.trait(smooth([f(p) for p in [(13, -246), (18, -250)]]), 0.9)


def socle(g):
    """Le pied de la colonne, de haut en bas : trompe, dauphins, nœud, cône, gerbe d'acanthe, vase à godrons, plinthe."""
    tige(g, -100 - W_SOCLE - 2, -150)
    # trompe évasée vers le bas, dauphins, nœud, cône qui descend dans le vase
    vase(g, [(4, -298), (6, -280), (9, -262), (13, -248)])
    for s in (-1, 1):
        dauphin(g, miroir(s))
    vase(g, [(13, -226), (11, -205), (8, -180), (5, -152)])
    g.contour(polygon([(-17, -232), (-11, -241), (11, -241), (17, -232), (11, -225), (-11, -225)]))
    for x in (-11, 11):
        g.trait(Path().M((x, -241)).L((x, -225)), FIN)
    # bouquet d'acanthe qui sort du col
    for s in (-1, 1):
        f = miroir(s)
        c = [(8, -150), (18, -170), (30, -190), (40, -198), (46, -192)] + volute_pts((42, -184), 7, 2, 0.8, -60, sens=1)
        tige_acanthe(g, smooth([f(p) for p in c]), 8, 2.5, lobes=(0.3, 0.55), cote=-s, taille=10)
    g.contour(rect(-24, -152, 48, 6))
    neck = [(19, -146), (18, -134), (20, -122), (26, -108), (34, -94)]
    g.contour(galbe(neck))
    acanthe(g, 0, -96, -90, 44, 22)
    g.contour(rect(-45, -94, 90, 7))
    bowl = [(44, -87), (42, -76), (36, -66), (26, -58), (14, -53), (8, -51)]
    g.contour(galbe(bowl))
    godrons(g, bowl, -86, -53, 9)
    tige(g, -52, -30, 6)
    g.contour(ellipse(0, -42, 11, 5))
    g.contour(ellipse(0, -30, 16, 3))
    g.contour(galbe([(22, -28), (25, -22), (31, -18), (38, -15)]))
    g.contour(rect(-40, -15, 80, 12))
    # le montant intérieur de la colonne descend jusqu'au bas, entre la colonne et le panneau
    g.trait(Path().M((AXE - RB, 1)).L((AXE - RB, -100 + RB - RW / 2)), RW, round=False)


def bout_socle():
    """Haut du socle, au bout bas de la bande latérale (retourné par le moteur : u = 0 en bas, vers le haut)."""
    g = G(lambda p: (-p[1] - 100, AXE + p[0]), fenetre=(-100 - W_SOCLE - 2, -98))
    socle(g)
    return boite(g.ops, w=W_SOCLE)


def coin_pied():
    """Bas du socle, dans le coin du pied (x : profondeur depuis le bord extérieur, y : hauteur depuis le bas)."""
    g = G(lambda p: (AXE + p[0], -p[1]), fenetre=(-102, 2))
    socle(g)
    return boite(g.ops)


# --------------------------------------------------------------------------- #
# Pied : panneau de rinceaux, corbeille entre deux griffons. Repère de l'écran (x le long, y vers le bas, de 0 en
# haut du panneau à 100 au bord bas du cadre).
# --------------------------------------------------------------------------- #
def ecran():
    """Repère du panneau du pied tel qu'on le voit : x le long, y vers le bas (v = 100 - y)."""
    return G(lambda p: (p[0], 100 - p[1]))


P_RINCEAU = 145.0
Y_TIGE = 82.0


def rinceau():
    """Une période du rinceau, qui pousse du milieu (x = P) vers le bout (x = 0) : tige basse, grande volute
    d'acanthe qui monte et s'enroule vers le milieu, feuille engainante au départ, vrille à clochette."""
    P = P_RINCEAU
    g = ecran()
    # vrille à clochette et bourgeon, au-dessus de la feuille
    g.trait(smooth([(P - 4, Y_TIGE - 3), (P - 12, 62), (P - 16, 44), (P - 15, 30)]), FIN)
    tulipe(g, P - 15, 32, 10, rot=-95)
    g.trait(smooth([(P - 13, 56), (P - 6, 46), (P - 5, 38)]), FIN)
    trefle(g, P - 5, 35, 2.3)
    g.ruban(smooth([(P + 6, Y_TIGE), (0.5 * P, Y_TIGE + 2), (-6, Y_TIGE)]), 5.0)
    c = [(P - 30, Y_TIGE - 2), (P - 60, 78), (30, 68), (18, 48), (20, 26), (32, 13), (48, 10)]
    c += volute_pts((52, 42), 27, 5, 1.3, -95, sens=1)[1:]
    tige_acanthe(g, smooth(c), 16, 3, lobes=(0.3, 0.37, 0.44, 0.51, 0.58, 0.65), cote=-1, taille=12)
    acanthe(g, P - 18, Y_TIGE - 1, 194, 44, 16)
    return frise(P, g.ops, deborde=8)


W_CORBEILLE = 390.0


def griffon(g, f):
    """Griffon de droite, tête vers le dehors, gueule ouverte ; le cou à crinière s'enroule en volute d'acanthe, la
    queue file en feuille le long du bas. f : miroir pour celui de gauche."""
    m = lambda pts: [f(p) for p in pts]
    droite = f((1, 0))[0] > 0
    tail = feuille(84, 24).transformed(affine(108, 86, rot=-2))
    g.contour(tail.transformed(f))
    g.trait(smooth(m([(114, 86), (160, 84), (188, 83)])), 0.9)
    body = [(130, 30), (122, 44), (110, 56), (100, 66)] + volute_pts((112, 70), 12, 3, 1.1, 200, sens=-1)[1:]
    tige_acanthe(g, smooth(m(body)), 16, 3, lobes=(0.08, 0.2, 0.32, 0.44), cote=1 if droite else -1, taille=11)
    head = [(118, 22), (122, 11), (132, 7), (142, 9), (152, 11), (161, 12), (162, 17), (153, 19), (146, 22),
            (156, 26), (150, 31), (138, 33), (128, 38), (120, 32)]
    g.contour(smooth(m(head), closed=True))
    g.contour(polygon(m([(121, 12), (113, 0), (130, 8)])), 1.2)
    g.plein(circle(*f((141, 13)), 1.8))
    g.trait(smooth(m([(134, 18), (138, 22), (144, 22)])), 0.9)
    g.trait(smooth(m([(146, 23), (160, 23), (170, 18), (171, 10), (165, 9)])), FIN)
    criniere = Path()
    for k in range(4):
        criniere.extend(smooth(m([(124 - 2 * k, 18 + 5 * k), (130 - 2 * k, 22 + 5 * k)])))
    g.trait(criniere, 0.8)


def corbeille():
    """Pièce du milieu du pied : corbeille à anses en volute, sous une crête de deux petites volutes, entre deux
    griffons ; un rang de perles pend dessous."""
    g = ecran()
    H = W_CORBEILLE / 2
    for s in (-1, 1):
        g.ruban(smooth([(s * (H + 6), Y_TIGE), (s * (H - 30), Y_TIGE + 2), (s * (H - 60), 85)]), 5.0)
    for s in (-1, 1):
        griffon(g, miroir(s))
    # anses en volute
    for s in (-1, 1):
        f = miroir(s)
        pts = [(56, 27), (72, 30)] + volute_pts((70, 52), 20, 4, 1.05, -85, sens=1)
        tige_acanthe(g, smooth([f(p) for p in pts]), 8, 2.5, lobes=(0.2, 0.4), cote=-s, taille=8)
        g.trait(smooth([f(p) for p in [(50, 72), (34, 84), (16, 89), (0, 90)]]), 0.8)
    # corbeille
    corps = [(-60, 25), (60, 25), (50, 58), (30, 72), (-30, 72), (-50, 58)]
    g.contour(polygon(corps))
    tresse, dents = Path(), Path()
    for y in (46, 52, 58, 64, 70):
        hw = 60 - (y - 25) * 0.3 if y < 58 else 50 - (y - 58) * 1.4
        tresse.M((-hw + 2, y)).L((hw - 2, y))
    for i in range(-4, 5):
        x = 13 * i
        dents.extend(polygon([(x - 4.5, 28), (x + 4.5, 28), (x, 44)]))
    g.trait(tresse, 0.9, round=False)
    g.plein(dents)
    g.contour(rect(-66, 17, 132, 8))
    g.trait(Path().M((-64, 21)).L((64, 21)), 0.8, round=False)
    for s in (-1, 1):
        f = miroir(s)
        g.ruban(smooth([f(p) for p in [(4, 16)] + volute_pts((15, 9), 7, 2, 0.9, 180, sens=1)]), 2.8)
    g.contour(Path().M((0, 3)).C((5, 8), (5, 14), (0, 17)).C((-5, 14), (-5, 8), (0, 3)).Z(), 1.2)
    for x, y in ((-20, 87), (0, 90), (20, 87)):
        perle(g, x, y, 3.4)
    return piece(W_CORBEILLE, g.ops)


W_FIN = 34.0
GAP = 7.0


def fin_panneau():
    """Bout du panneau, dessiné par-dessus les filets : un jour de papier les coupe (entre la colonne et le panneau,
    deux cadres distincts sur la gravure ; au bout libre, le reste du filet), puis le filet de fermeture ; la tige
    finit en volute."""
    g = ecran()
    g.ops.append(fill("paper", rect(0, -2, GAP, 104).transformed(g.t)))
    g.trait(Path().M((GAP + RB, RB - RW / 2)).L((GAP + RB, 100 - RB + RW / 2)), RW, round=False)
    g.ruban(smooth([(W_FIN + 6, Y_TIGE), (GAP + 18, Y_TIGE - 1)] + volute_pts((GAP + 18, Y_TIGE - 12), 11, 3, 0.9,
                                                                             90, sens=1)), 3.4)
    return boite(g.ops, w=W_FIN, dessus=True)


def spec():
    return cadre(
        name="candelabres",
        title="Ordinaire de la messe",
        source="paroissien, p.96-97 (Ordinaire de la messe) et table des matières",
        inks={},
        palettes={},
        layout={"margin": 12, "top": S, "side": S, "bottom": S, "textPadding": 9},
        ground=None,
        disposition="equerre_pied",
        top=frise(100, []),
        side=etage(),
        bottom=rinceau(),
        centers={"side": centre(), "bottom": corbeille()},
        corners={"bottom": coin_pied()},
        rules=[filet(INK, RB, RW, depuis="exterieur"), filet(INK, RB, RW, depuis="interieur")],
        ends={"side": bouts(couronnement(), bout_socle()), "bottom": bouts(fin_panneau())},
    )
