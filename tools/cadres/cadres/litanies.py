"""
Cadre « Litanies de la Sainte Vierge », d'après la p.29 d'un paroissien de la fin du XIXᵉ siècle (Prière du soir,
photo litanies-candelabre-et-pied) : gravure sur bois à l'encre noire, sur papier nu. Deux panneaux cernés d'un
filet, sans fond : un candélabre le long du côté extérieur, toute la hauteur de la page, et une bande de rinceaux au
pied du texte, qui s'arrête juste avant le candélabre. Disposition en équerre (L) : côté extérieur et pied.

Le candélabre de l'original, de haut en bas : corbeille d'acanthe à volutes et pendeloques de perles ; urne à deux
draperies ; cône renversé entre deux crosses d'acanthe ; tête de chérubin ailée sur une coupe, d'où pendent des
chapelets ; hostie « IHS » posée dans un calice à godrons ; bourgeons et trèfles ; bouquet d'acanthe à pompons dans
un petit vase ; tazza ; deux dragons sur de longs cous ; grand vase à godrons, piédouche, socle à degrés. Au pied :
une corbeille à lambrequins coiffée de deux crosses, deux chimères adossées (tête de griffon, corps d'acanthe) et
un rinceau d'acanthe à fleurettes de chaque côté.

Relevé (photo de 1638 × 4000 px, prise droite) : panneau du candélabre ≈ 200 px de large et 2 400 px de haut
(12 largeurs) ; panneau du pied ≈ 210 px de haut (1,05 largeur) et 1 045 px de long, à 12 px du candélabre, les
deux panneaux alignés en bas ; filets ≈ 4 px. Encre : gris-noir neutre (#555655 dans les traits les plus noirs
après correction du blanc, le trait fin étant flou en photo) : la même encre que le texte.

Modules (unité : 1/100 de la largeur du candélabre, 28 dp ; le pied a la même épaisseur, donc la même unité) :
  - côté : bout de début = corbeille et urne ; frise = un étage de deux éléments qui montent, bouquet dans son
    vase puis cône entre deux crosses, répété dans le même sens des deux côtés de la pièce centrale
    (`symetrique=False`) ; pièce centrale = chérubin, chapelets, hostie et calice, « droite » pour que IHS se lise
    sur le verso ; bout de fin = tazza, dragons et col du grand vase ; coin du pied = panse du vase, piédouche et
    socle, dans le prolongement du bout de fin (un seul dessin, coupé à la limite). Le candélabre est symétrique :
    le reflet du verso ne se voit pas.
  - pied : rinceau ondé d'acanthe (une volute en haut et une en bas par période), corbeille et chimères en pièce
    centrale, et à chaque bout un filet qui ferme le panneau et un bouton de tulipe. Le pied n'est pas retourné
    entre recto et verso ; ses deux bouts sont donc le même dessin (le moteur reflète le début).
  - coin : les deux panneaux restent séparés comme sur la page, le filet du pied s'arrête à 6,5 unités (1,8 dp)
    de celui du candélabre et leurs pieds sont alignés. Les tiges qui passent d'un module à l'autre (rinceau, bouts
    et chimères du pied) sont deux traits sur fond papier, pas des rubans : pas de fil gris aux raccords.

Simplifications : traits d'épaisseur constante, hachures seulement sur les volumes et les crinières ; un étage
répété au lieu de la suite d'éléments tous différents de l'original (le téléphone est deux fois plus haut que la
page, en largeurs de candélabre) ; chimères, dragons et chérubin réduits à leur silhouette ; les chapelets pendent
le long des bords au lieu de s'enrouler autour de l'hostie.
Tracé à l'encre du texte du thème (« text ») et évidé au papier : la gravure reste lisible en sombre et en nuit.
Pas d'encre propre, donc pas de palette liturgique.
"""
import math

from commun import Path, boite, bouts, cadre, circle, ellipse, fill, fleur_de_lis, frise, leaf, piece, rect, smooth, \
    stroke, teardrop

ORDRE = 29

ENCRE = "text"           # l'encre du texte : la gravure était tirée avec elle
PAPIER = "paper"

S = 28.0                 # largeur du candélabre et hauteur du pied (dp)
VC = 50.0                # axe du candélabre (profondeur v)
R = 47.0                 # filets du panneau du candélabre : X = ±R de part et d'autre de l'axe
R_PIED = (3.0, 97.0)     # filets du panneau du pied (y depuis le haut du panneau)
FERME = 3.5              # filet qui ferme le panneau du pied, depuis le bout de la bande

FILET = 2.6              # filets des panneaux
TRAIT = 2.2              # contours
FIN = 1.5                # détails
HACH = 1.0               # hachures


# --------------------------------------------------------------------------- #
# Dessin dans un repère local, émis dans le repère du module
# --------------------------------------------------------------------------- #
class Dessin:
    """Tracés en unités du module, à travers une transformation de points `t` (sans changement d'échelle)."""

    def __init__(self, t):
        self.ops, self.t = [], t

    def _p(self, path):
        return path.transformed(self.t)

    def trait(self, path, w=TRAIT, rond=True):
        self.ops.append(stroke(ENCRE, self._p(path), w, rond))

    def plein(self, path):
        self.ops.append(fill(ENCRE, self._p(path)))

    def masque(self, path):
        self.ops.append(fill(PAPIER, self._p(path)))

    def forme(self, path, w=TRAIT):
        """Forme évidée cernée d'un trait : elle cache ce qui est dessous."""
        self.masque(path)
        self.trait(path, w)

    def silhouette(self, path, w=FIN):
        """Comme `forme`, mais seul le contour de l'union des sous-chemins reste visible."""
        self.trait(path, 2 * w)
        self.masque(path)

    def ruban(self, path, w, bord=FIN):
        """Tige creuse : deux traits parallèles, papier entre les deux."""
        self.ops.append(stroke(ENCRE, self._p(path), w + 2 * bord, True))
        self.ops.append(stroke(PAPIER, self._p(path), w, True))

    def tige_creuse(self, path, w, bord=FIN):
        """Comme `ruban`, mais en deux traits parallèles et un fond papier : pour une tige qui passe d'un module à
        l'autre. Un ruban coupé par la découpe d'un module y laisse un fil gris (le trait large, puis le papier,
        tous deux à demi couverts au bord de la découpe) ; deux traits identiques de part et d'autre se fondent."""
        pts = jalons(path)
        g, d = [], []
        for i, p in enumerate(pts):
            a, b = pts[max(0, i - 1)], pts[min(len(pts) - 1, i + 1)]
            n = math.hypot(b[0] - a[0], b[1] - a[1]) or 1.0
            nx, ny = -(b[1] - a[1]) / n * w / 2, (b[0] - a[0]) / n * w / 2
            g.append((p[0] + nx, p[1] + ny))
            d.append((p[0] - nx, p[1] - ny))
        fond = Path().M(g[0])
        for q in g[1:] + d[::-1]:
            fond.L(q)
        self.masque(fond.Z())
        for bordure in (g, d):
            self.trait(smooth(bordure[::2] + [bordure[-1]]), bord, rond=False)


def jalons(path, k=8):
    """Points le long d'un chemin (M, L, C), k par cubique."""
    pts, cur = [], None
    for c in path.cmds:
        if c[0] in "ML":
            cur = c[1]
            pts.append(cur)
        elif c[0] == "C":
            a, b, e = c[1], c[2], c[3]
            for i in range(1, k + 1):
                t = i / k
                m = 1 - t
                pts.append((m ** 3 * cur[0] + 3 * m * m * t * a[0] + 3 * m * t * t * b[0] + t ** 3 * e[0],
                            m ** 3 * cur[1] + 3 * m * m * t * a[1] + 3 * m * t * t * b[1] + t ** 3 * e[1]))
            cur = e
    return pts


def cote(du=0.0):
    """Candélabre : X en travers depuis l'axe (positif vers le texte), Y vers le bas de la page → repère de bande."""
    return lambda p: (p[1] + du, VC + p[0])


def socle_fin(h_coin):
    """Bas du candélabre : X depuis l'axe, Z hauteur au-dessus du bas de la page → bout de fin de la bande (dessiné
    comme au début de la bande ; le moteur le retourne)."""
    return lambda p: (p[1] - h_coin, VC + p[0])


def socle_coin():
    """Bas du candélabre → case du coin (dessinée pour le coin haut gauche : x depuis le bord, y depuis le bord)."""
    return lambda p: (VC + p[0], p[1])


def pied(du=0.0):
    """Pied : x le long de la bande, y vers le bas depuis le haut du panneau (côté texte) → repère de bande."""
    return lambda p: (p[0] + du, 100.0 - p[1])


# --------------------------------------------------------------------------- #
# Formes
# --------------------------------------------------------------------------- #
def galbe(profil):
    """Silhouette symétrique (vase, balustre) d'après son profil droit [(demi-largeur, y)], d'un bout à l'autre."""
    droite = smooth(profil)
    gauche = smooth([(-x, y) for x, y in reversed(profil)])
    p = Path()
    p.cmds = droite.cmds + [("L", gauche.cmds[0][1])] + gauche.cmds[1:] + [("Z",)]
    return p


def demi(profil, y):
    """Demi-largeur du profil à la hauteur y (interpolation linéaire)."""
    pts = sorted(profil, key=lambda q: q[1])
    if y <= pts[0][1]:
        return pts[0][0]
    for (x0, y0), (x1, y1) in zip(pts, pts[1:]):
        if y0 <= y <= y1:
            return x0 + (x1 - x0) * (y - y0) / ((y1 - y0) or 1)
    return pts[-1][0]


def hachures(g, profil, y0, y1, pas, a=0.3, b=0.9, w=HACH):
    """Hachures horizontales du côté de l'ombre (X > 0), de a à b de la demi-largeur."""
    p = Path()
    y = y0
    while y <= y1 + 1e-6:
        hw = demi(profil, y)
        p.M((a * hw, y)).L((b * hw, y))
        y += pas
    g.trait(p, w, rond=False)


def cannelures(g, profil, y0, y1, fracs, w=HACH):
    """Cannelures ou godrons : traits qui suivent la silhouette, à une fraction donnée de la demi-largeur."""
    p = Path()
    for f in fracs:
        pts = [(f * demi(profil, y0 + (y1 - y0) * i / 4), y0 + (y1 - y0) * i / 4) for i in range(5)]
        p.extend(smooth(pts))
    g.trait(p, w)


def spirale(cx, cy, R0, r1, a0, tours, sens=-1, avant=()):
    """Volute : points `avant`, puis spirale autour de (cx, cy) du rayon R0 au rayon r1, depuis l'angle a0 (degrés,
    y vers le bas), dans le sens des angles décroissants si sens = -1."""
    n = max(6, int(tours * 14))
    pts = list(avant)
    for i in range(n + 1):
        t = i / n
        a = math.radians(a0 + sens * 360 * tours * t)
        r = R0 + (r1 - R0) * t
        pts.append((cx + r * math.cos(a), cy + r * math.sin(a)))
    return smooth(pts)


def _rot(q, tx, ty, deg):
    a = math.radians(deg)
    return (tx + q[0] * math.cos(a) - q[1] * math.sin(a), ty + q[0] * math.sin(a) + q[1] * math.cos(a))


def place(path, x, y, deg=0.0, s=1.0, sy=None):
    """Chemin dessiné vers +x depuis l'origine → mis à l'échelle, tourné de `deg` et posé en (x, y)."""
    sy = s if sy is None else sy
    return path.transformed(lambda q: _rot((q[0] * s, q[1] * sy), x, y, deg))


def acanthe(L, W, lobes=2):
    """Feuille d'acanthe vers +x, base en (0, 0) : une lame et des lobes inclinés vers la pointe."""
    p = Path().extend(leaf(L, 0.55 * W))
    for i in range(lobes):
        t = 0.18 + 0.58 * i / max(1, lobes)
        k = 1 - 0.35 * i / max(1, lobes)
        for sg in (-1, 1):
            p.extend(place(leaf(0.46 * L * k, 0.38 * W * k, bend=-0.15 * sg), t * L, 0, sg * 38))
    return p


def feuille(g, x, y, deg, L, W, lobes=2):
    """Acanthe gravée : silhouette et nervure."""
    g.silhouette(place(acanthe(L, W, lobes), x, y, deg))
    g.trait(place(Path().M((0.08 * L, 0)).L((0.8 * L, 0)), x, y, deg), HACH)


def perle(g, x, y, r):
    g.forme(circle(x, y, r), FIN)


def bourgeon(g, x, y, r, deg=-90.0):
    """Bouton rond dans son calice, pointé vers `deg`."""
    a = math.radians(deg)
    g.forme(circle(x + 0.35 * r * math.cos(a), y + 0.35 * r * math.sin(a), r), FIN)
    cal = Path()
    for sg in (-1, 1):
        cal.extend(place(teardrop(1.25 * r, 0.75 * r), x, y, deg + sg * 38))
    g.forme(cal, FIN)


def trefle(g, x, y, r, deg=-90.0):
    """Trèfle : trois lobes ronds."""
    p = Path()
    for k in (-1, 0, 1):
        a = math.radians(deg + 72 * k)
        p.extend(circle(x + 0.95 * r * math.cos(a), y + 0.95 * r * math.sin(a), 0.78 * r))
    g.silhouette(p)


def tulipe(g, x, y, L, deg=-90.0):
    """Bouton de tulipe : trois pétales pointés vers `deg`."""
    p = Path()
    for d, k in ((0, 1.0), (-32, 0.85), (32, 0.85)):
        p.extend(place(teardrop(L * k, 0.55 * L), x, y, deg + d))
    g.silhouette(p)


def pompon(g, x, y, L):
    """Pompon pendu au bout d'un cordon : un gland qui s'évase vers le bas."""
    g.forme(Path().M((x - 1.5, y)).L((x + 1.5, y)).L((x + 3.5, y + L)).L((x - 3.5, y + L)).Z(), FIN)
    g.trait(Path().M((x - 1.2, y + 0.45 * L)).L((x - 2, y + L)).M((x + 1.2, y + 0.45 * L)).L((x + 2, y + L)), HACH)


# --------------------------------------------------------------------------- #
# Côté : couronnement (bout de début) — corbeille d'acanthe, pendeloques, urne à draperies
# --------------------------------------------------------------------------- #
W_HAUT = 236.0


def filets_cote(g, y0, y1):
    g.trait(Path().M((-R, y0)).L((-R, y1)), FILET, rond=False)
    g.trait(Path().M((R, y0)).L((R, y1)), FILET, rond=False)


def tige(g, y0, y1, hw=2.4):
    """La tige qui relie les étages : deux traits."""
    g.trait(Path().M((-hw, y0)).L((-hw, y1)), FIN, rond=False)
    g.trait(Path().M((hw, y0)).L((hw, y1)), FIN, rond=False)


def bague(g, y, rx=8.0, ry=2.8):
    g.forme(ellipse(0, y, rx, ry), FIN)


def couronnement():
    g = Dessin(cote())
    g.trait(Path().M((-R, W_HAUT + 2)).L((-R, 3)).L((R, 3)).L((R, W_HAUT + 2)), FILET, rond=False)
    tige(g, 226, W_HAUT + 2)
    # pendeloques : cordon et deux perles sous chaque volute
    for s in (-1, 1):
        g.trait(Path().M((s * 40.5, 32)).L((s * 40.5, 84)), FIN)
        perle(g, s * 40.5, 60, 3.0)
        perle(g, s * 40.5, 86, 4.0)
    # corbeille : cloche évasée, cannelée en bas
    cloche = [(36, 14), (34, 32), (30, 52), (27, 72), (25, 89)]
    g.forme(galbe(cloche))
    cannelures(g, cloche, 70, 88, [-0.78, -0.52, -0.26, 0, 0.26, 0.52, 0.78])
    g.forme(rect(-27, 88, 54, 7))
    hachures(g, [(27, 88), (27, 95)], 91.5, 91.5, 3, 0.0, 1.0)
    # acanthes : une droite au milieu, deux qui montent vers les volutes en se recourbant
    for s in (-1, 1):
        feuille(g, s * 8, 70, -90 + s * 40, 44, 26, lobes=2)
    feuille(g, 0, 72, -90, 56, 28, lobes=3)
    # volutes aux angles : la lèvre de la corbeille s'enroule vers le dehors et le bas
    for s in (-1, 1):
        lev = spirale(s * 32, 25, 11.5, 2.5, -90, 1.2, sens=s, avant=[(s * 2, 14), (s * 18, 13.5)])
        g.ruban(lev, 4.2)
        g.plein(circle(s * 32.8, 25.5, 2.3))
    # col sous la corbeille
    col = [(15, 95), (10, 100), (6, 105), (5, 110)]
    g.forme(galbe(col))
    hachures(g, col, 98, 107, 2.6, 0.2, 0.95)
    # urne : bouton, panse ovoïde à moulures, épaules à anneaux d'où pendent deux draperies
    for s in (-1, 1):
        drap = smooth([(s * 25, 122), (s * 33, 128), (s * 39, 150), (s * 42, 174), (s * 37, 181), (s * 41, 192),
                       (s * 33, 188), (s * 28, 178), (s * 26, 156), (s * 22, 132)], closed=True)
        g.forme(drap, FIN)
        g.trait(smooth([(s * 31, 132), (s * 35, 156), (s * 36, 178)]), HACH)
        g.trait(smooth([(s * 27, 140), (s * 30, 160), (s * 31, 176)]), HACH)
        g.trait(Path().M((s * 16, 120)).L((s * 24, 118)), FIN)
        perle(g, s * 27.5, 118.5, 4.3)
    urne = [(5, 111), (10, 114), (18, 120), (22, 132), (21, 144), (16, 154), (9, 161), (6, 164)]
    g.forme(galbe(urne))
    g.trait(Path().M((-20, 127)).L((20, 127)), FIN, rond=False)
    g.trait(Path().M((-21.5, 140)).L((21.5, 140)), FIN, rond=False)
    hachures(g, urne, 130, 159, 2.8, 0.35, 0.92)
    # nœud et cône renversé jusqu'au disque
    g.forme(ellipse(0, 168, 9.5, 3.4), FIN)
    cone = [(4, 171), (5, 186), (9, 202), (16, 214), (20, 219)]
    g.forme(galbe(cone))
    hachures(g, cone, 176, 216, 2.8, 0.3, 0.9)
    g.forme(ellipse(0, 221, 24, 4.2))
    bague(g, 229, 7, 2.6)
    return boite(g.ops, w=W_HAUT)


# --------------------------------------------------------------------------- #
# Côté : frise — un étage qui monte : bouquet d'acanthe dans un petit vase, puis cône renversé entre deux crosses
# --------------------------------------------------------------------------- #
Y_CONE = 112.0            # début du second élément
P_ETAGE = 236.0


def bouquet(g):
    """Coupelle, bourgeons, bouquet d'acanthe à trois branches et pompons, dans un petit vase (y de 0 à Y_CONE)."""
    bague(g, 6)
    tige(g, 0, 14)
    # bourgeons au-dessus, de part et d'autre de la coupelle
    for s in (-1, 1):
        g.trait(smooth([(s * 9, 20), (s * 22, 20), (s * 30, 14)]), FIN)
        bourgeon(g, s * 32, 11, 4.4, -90 + s * 25)
    coupelle = [(13, 12), (12, 16), (8, 21), (4, 24)]
    g.forme(galbe(coupelle))
    cannelures(g, coupelle, 13, 23, [-0.6, -0.2, 0.2, 0.6])
    tige(g, 24, 40)
    # branches latérales : elles montent, s'enroulent vers le dehors ; un pompon pend de chaque volute
    for s in (-1, 1):
        g.trait(smooth([(s * 38, 54), (s * 40, 64), (s * 37, 72), (s * 40, 80)]), FIN)
        pompon(g, s * 40, 80, 9)
        br = spirale(s * 31, 50, 9.5, 1.8, -66 if s > 0 else 246, 1.0, sens=s,
                     avant=[(s * 4, 92), (s * 9, 72), (s * 17, 52), (s * 27, 40), (s * 35, 41)])
        g.ruban(br, 4.0)
        feuille(g, s * 10, 76, -90 + s * 38, 22, 13, lobes=1)
    # branche du milieu : une acanthe droite
    feuille(g, 0, 90, -90, 52, 20, lobes=3)
    # petit vase et trèfles à son pied
    for s in (-1, 1):
        g.trait(smooth([(s * 8, 104), (s * 20, 104), (s * 28, 98)]), FIN)
        trefle(g, s * 31, 96, 4.0, -90 + s * 50)
    vase = [(11, 88), (9, 93), (8, 99), (10, 104), (7, 107)]
    g.forme(galbe(vase))
    g.forme(ellipse(0, 88, 11.5, 2.8), FIN)
    hachures(g, vase, 92, 104, 2.6, 0.3, 0.92)
    tige(g, 107, Y_CONE + 2)


def cone_crosses(g, y0):
    """Plateau, cône renversé entre deux crosses d'acanthe enroulées vers la tige, feuilles pendantes et fleur de lys
    (y de y0 à P_ETAGE)."""
    def at(x, y):
        return (x, y0 + y)

    for s in (-1, 1):
        # feuille qui pend de la volute, terminée par un bouton
        feuille(g, s * 22, y0 + 60, 90 - s * 18, 34, 18, lobes=2)
        tulipe(g, s * 33, y0 + 98, 10, 90 - s * 25)
        # crosse : du plateau vers le dehors, le long du bord, puis enroulée vers la tige ; lobes sur le dos
        for y, a in ((14, -60), (30, -35), (46, -10)):
            g.silhouette(place(leaf(12, 8), *at(s * 37, y), a if s > 0 else 180 - a))
        cr = spirale(s * 23, y0 + 50, 13, 2.2, -15 if s > 0 else 195, 1.15, sens=s,
                     avant=[at(s * 10, 8), at(s * 26, 4), at(s * 38, 12), at(s * 42, 28), at(s * 38, 45)])
        g.ruban(cr, 4.8)
    g.forme(ellipse(0, y0 + 7, 15, 3.6))
    cone = [(9, 10), (7, 30), (4.5, 62), (2.8, 92), (2.4, 112)]
    cone = [(x, y0 + y) for x, y in cone]
    g.forme(galbe(cone))
    hachures(g, cone, y0 + 14, y0 + 100, 3.0, 0.25, 0.9)
    g.forme(place(fleur_de_lis(18), 0, y0 + 78), FIN)
    tige(g, y0 + 110, P_ETAGE + 1)


def etage():
    g = Dessin(cote())
    filets_cote(g, -2, P_ETAGE + 2)
    bouquet(g)
    cone_crosses(g, Y_CONE)
    return frise(P_ETAGE, g.ops, symetrique=False)


# --------------------------------------------------------------------------- #
# Côté : pièce centrale — chérubin, chapelets, hostie IHS dans le calice (pièce droite : IHS se lit sur le verso)
# --------------------------------------------------------------------------- #
W_CENTRE = 330.0


def cherubin(g, cy):
    """Tête de chérubin entre deux ailes relevées."""
    for s in (-1, 1):
        aile = smooth([(s * 8, cy - 5), (s * 18, cy - 18), (s * 30, cy - 28), (s * 42, cy - 33), (s * 44, cy - 26),
                       (s * 42, cy - 21), (s * 45, cy - 15), (s * 41, cy - 10), (s * 43, cy - 3), (s * 38, cy + 1),
                       (s * 38, cy + 8), (s * 32, cy + 10), (s * 29, cy + 16), (s * 22, cy + 14), (s * 11, cy + 9)],
                      closed=True)
        g.forme(aile, FIN)
        plumes = Path()
        for a, b in (((s * 14, cy - 8), (s * 41, cy - 27)), ((s * 15, cy - 3), (s * 41, cy - 17)),
                     ((s * 14, cy + 2), (s * 39, cy - 6)), ((s * 13, cy + 6), (s * 34, cy + 5)),
                     ((s * 12, cy + 9), (s * 26, cy + 12))):
            plumes.extend(smooth([a, ((a[0] + b[0]) / 2, (a[1] + b[1]) / 2 - 2.5), b]))
        g.trait(plumes, HACH)
    g.forme(circle(0, cy, 14.5))
    # cheveux bouclés
    for x, y in ((-8, cy - 10), (0, cy - 12.5), (8, cy - 10)):
        g.trait(spirale(x, y, 3.2, 0.8, 90, 0.9, sens=-1), FIN)
    # yeux, nez, bouche, menton
    for s in (-1, 1):
        g.trait(Path().M((s * 7.5, cy - 0.5)).C((s * 6, cy - 2.4), (s * 3.5, cy - 2.4), (s * 2.5, cy - 0.5)), FIN)
    g.trait(Path().M((0, cy + 0.5)).L((-1, cy + 4.5)).L((1, cy + 4.8)), HACH)
    g.plein(circle(0, cy + 8, 1.5))
    g.trait(Path().M((-11, cy + 7)).C((-8, cy + 13.5), (8, cy + 13.5), (11, cy + 7)), HACH)


def ihs(g, cy, h=16.0):
    """Monogramme IHS en capitales, lisible sur la page (x de la page = -X)."""
    k = h / 16.0
    p = Path()
    for x in (-11.5, -4, 4):       # fûts de I et de H, x de la page
        p.M((-x * k, cy - 8 * k)).L((-x * k, cy + 8 * k))
    p.M((4 * k, cy)).L((-4 * k, cy))        # barre de H
    g.trait(p, 2.6 * k, rond=False)
    ser = Path()
    for x in (-11.5, -4, 4):
        for y in (-8, 8):
            ser.M((-(x - 2) * k, cy + y * k)).L((-(x + 2) * k, cy + y * k))
    g.trait(ser, 1.1 * k, rond=False)
    s = smooth([(15, -5.5), (12, -8), (8.6, -6.2), (9.2, -2.2), (13.6, 1.5), (14.6, 5.6), (11.4, 8), (7.8, 6)])
    g.trait(s.transformed(lambda q: (-q[0] * k, cy + q[1] * k)), 2.4 * k)


def centre_cote():
    W = W_CENTRE
    g = Dessin(cote(-W / 2))
    filets_cote(g, -1, W + 1)
    tige(g, -1, 24)
    bague(g, 7)
    # chapelets : de la bouche du chérubin jusqu'aux ailes, puis en festons jusqu'aux flancs de l'hostie
    for s in (-1, 1):
        g.trait(smooth([(s * 3, 58), (s * 18, 72), (s * 33, 86)]), HACH)
        pts = smooth([(s * 40, 52), (s * 42, 80), (s * 40, 110), (s * 34, 136), (s * 27, 152)])
        g.trait(pts, HACH)
        for x, y in ((41.5, 64), (42, 74), (42, 84), (41.5, 94), (40.6, 104), (39, 114), (37, 123), (34.5, 132),
                     (31.5, 141)):
            perle(g, s * x, y, 2.8)
    cherubin(g, 46)
    # coupe sous le chérubin
    coupe = [(25, 72), (22, 84), (16, 94), (9, 100), (5, 104)]
    g.forme(galbe(coupe))
    g.forme(ellipse(0, 72, 25, 3.5), FIN)
    cannelures(g, coupe, 80, 101, [-0.7, -0.35, 0, 0.35, 0.7])
    g.forme(rect(-4.5, 103, 9, 13), FIN)
    g.forme(ellipse(0, 116, 10, 2.8), FIN)
    tige(g, 118, 142)
    # calice : lèvre du fond, hostie, coupe à godrons, nœud, pied
    g.forme(ellipse(0, 197, 34, 4.5), FIN)
    g.forme(circle(0, 169, 27))
    g.trait(circle(0, 169, 23.5), HACH)
    ihs(g, 169)
    calice = [(34, 197), (33, 209), (29, 224), (21, 237), (10, 245), (5, 248)]
    g.forme(galbe(calice))
    g.trait(Path().M((-34, 197)).C((-28, 201.5), (28, 201.5), (34, 197)), FIN)
    g.trait(Path().M((-31.5, 218)).C((-20, 221), (20, 221), (31.5, 218)), FIN)
    cannelures(g, calice, 221, 244, [-0.78, -0.52, -0.26, 0, 0.26, 0.52, 0.78])
    hachures(g, calice, 204, 216, 2.8, 0.5, 0.93)
    # bourgeons et trèfles de part et d'autre du pied
    for s in (-1, 1):
        g.trait(smooth([(s * 8, 286), (s * 22, 278), (s * 30, 264), (s * 30, 258)]), FIN)
        bourgeon(g, s * 30, 255, 4.8, -90)
        g.trait(smooth([(s * 14, 290), (s * 30, 290), (s * 35, 283)]), FIN)
        trefle(g, s * 37, 279, 4.6, -90 + s * 40)
    noeud = [(5, 248), (4, 256), (11, 262), (11, 266), (4, 272), (4.5, 282), (9, 288)]
    g.forme(galbe(noeud))
    hachures(g, noeud, 251, 285, 2.6, 0.2, 0.95)
    g.forme(ellipse(0, 292, 38, 6.5))
    g.trait(Path().M((-37, 293)).C((-20, 297.5), (20, 297.5), (37, 293)), HACH)
    tige(g, 298, W + 1)
    return piece(W, g.ops, droit=True)


# --------------------------------------------------------------------------- #
# Bas du candélabre : bout de fin (tazza, dragons, col du vase) et coin (panse, piédouche, socle)
# Repère : X depuis l'axe, Z hauteur au-dessus du bas de la page (le haut vers +Z).
# --------------------------------------------------------------------------- #
H_COIN = 100.0
W_BAS = 252.0                 # bout de fin : de Z = H_COIN à Z = H_COIN + W_BAS


def bas_candelabre(g, zmin, zmax):
    """Tout le bas du candélabre ; seules les parties qui touchent [zmin, zmax] sont tracées."""
    def entre(a, b):
        return b >= zmin and a <= zmax

    if zmin <= 3:
        g.trait(Path().M((-R, zmax + 2)).L((-R, 3)).L((R, 3)).L((R, zmax + 2)), FILET, rond=False)
    else:
        filets_cote(g, zmin - 1, zmax + 2)
    # socle à degrés
    if entre(3, 26):
        g.forme(rect(-43, 3, 86, 7))
        g.forme(Path().M((-43, 10)).L((43, 10)).L((26, 20)).L((-26, 20)).Z())
        hachures(g, [(43, 10), (26, 20)], 12, 18.5, 2.2, 0.2, 0.95)
        g.forme(rect(-24, 20, 48, 4.5), FIN)
    # piédouche
    pied_vase = [(20, 24), (12, 29), (8.5, 35), (12, 42), (9, 50), (7, 55), (11, 59)]
    if entre(24, 60):
        g.forme(galbe(pied_vase))
        hachures(g, pied_vase, 29, 56, 2.4, 0.2, 0.9)
    # panse du vase : godrons en bas, lèvre
    panse = [(11, 59), (24, 64), (35, 72), (40, 82), (39, 92)]
    if entre(58, 98):
        g.forme(galbe(panse))
        cannelures(g, [(14, 61), (28, 66), (36, 74), (39, 82)], 62, 83,
                   [-0.85, -0.62, -0.4, -0.2, 0, 0.2, 0.4, 0.62, 0.85])
        g.trait(Path().M((-39.5, 84)).L((39.5, 84)), FIN, rond=False)
        hachures(g, panse, 86, 91, 2.5, 0.4, 0.95)
    # dragons : cou en S qui sort des acanthes, tête tournée vers l'axe, gueule ouverte
    if entre(120, 280):
        for s in (-1, 1):
            cou = smooth([(s * 10, 128), (s * 26, 150), (s * 38, 176), (s * 41, 202), (s * 37, 226), (s * 29, 242)])
            g.ruban(cou, 9.0)
            # écailles en travers du cou
            ecailles = Path()
            for x, z in ((24, 147), (30, 158), (35, 170), (38.5, 183), (40.2, 196), (40, 209), (37.5, 222)):
                ecailles.M((s * (x - 4.5), z + 2)).L((s * (x + 0.5), z - 2))
            g.trait(ecailles, HACH)
            # tête penchée vers le petit vase : oreille, crâne, museau à gueule ouverte, langue fourchue
            def t(q, s=s):
                return _tete(q, s)
            g.trait(Path().M(t((16, 250.5))).L(t((5, 250))).L(t((2, 252))).M(t((5, 250))).L(t((2, 248))), HACH)
            g.forme(Path().M(t((24, 259))).L(t((31, 271))).L(t((29, 257))).Z(), FIN)
            tete = smooth([t(q) for q in ((28, 243), (31, 251), (27, 259), (20, 262), (12, 260), (4, 258),
                                          (1.5, 255.5), (12, 254), (17, 251), (5, 247), (6, 244.5), (19, 241.5))],
                          closed=True)
            g.forme(tete, FIN)
            g.plein(circle(*t((20, 256.5)), 1.7))
    # col du vase et acanthes qui s'en échappent en se recourbant vers le dehors
    col = [(28, 94), (22, 104), (16, 116), (14, 126), (16, 134)]
    if entre(92, 200):
        g.forme(ellipse(0, 94, 41, 4.0))
        for s in (-1, 1):
            for z, deg, L in ((110, 20, 42), (124, 52, 48)):
                feuille(g, s * 12, z, deg if s > 0 else 180 - deg, L, 24, lobes=2)
        g.forme(galbe(col))
        hachures(g, col, 98, 130, 2.8, 0.3, 0.92)
        g.forme(ellipse(0, 136, 17, 3.6), FIN)
        tige(g, 138, 192)
    # cône, petit vase à créneaux entre les têtes, cône jusqu'à la tazza
    if entre(185, 300):
        cone = [(2.5, 190), (6, 200), (10, 210), (12, 214)]
        g.forme(galbe(cone))
        hachures(g, cone, 194, 212, 2.6, 0.2, 0.95)
        vase = [(12, 214), (14, 222), (16, 232)]
        g.forme(galbe(vase))
        g.trait(Path().M((-13, 223)).C((-9, 218), (-5, 218), (-4.3, 223)).C((-2, 218), (2, 218), (4.3, 223))
                .C((5, 218), (9, 218), (13, 223)), FIN)
        g.forme(rect(-17, 232, 34, 4), FIN)
        tige(g, 236, 250, 2.6)
        balustre = [(2.6, 250), (4.5, 266), (8, 282), (11, 290)]
        g.forme(galbe(balustre))
        hachures(g, balustre, 254, 288, 2.6, 0.2, 0.92)
    # tazza cannelée et bourgeons
    if entre(285, H_COIN + W_BAS):
        for s in (-1, 1):
            g.trait(smooth([(s * 6, 262), (s * 22, 274), (s * 33, 284), (s * 37, 296)]), FIN)
            trefle(g, s * 37, 299, 4.4, 90 + s * 20)
            g.trait(smooth([(s * 18, 322), (s * 30, 326), (s * 35, 334)]), FIN)
            bourgeon(g, s * 35, 337, 4.0, 90)
        g.forme(ellipse(0, 292, 11, 3), FIN)
        tazza = [(8, 295), (22, 300), (32, 308), (33, 314)]
        g.forme(galbe(tazza))
        cannelures(g, tazza, 297, 312, [-0.75, -0.5, -0.25, 0, 0.25, 0.5, 0.75])
        g.forme(ellipse(0, 315, 33, 3.4), FIN)
        g.forme(galbe([(9, 316), (7, 324), (9, 330), (5, 336)]), FIN)
        tige(g, 335, H_COIN + W_BAS + 2)


def _tete(q, s):
    """Tête de dragon dessinée de profil vers l'axe (côté droit), puis penchée vers le bas autour du cou."""
    x, z = 1.15 * (q[0] - 28), 1.15 * (q[1] - 243)
    a = math.radians(40)
    x, z = x * math.cos(a) - z * math.sin(a), x * math.sin(a) + z * math.cos(a)
    return (s * (28 + x), 243 + z)


def fin_cote():
    g = Dessin(socle_fin(H_COIN))
    bas_candelabre(g, H_COIN, H_COIN + W_BAS)
    return boite(g.ops, w=W_BAS)


def coin_pied():
    g = Dessin(socle_coin())
    bas_candelabre(g, 0, H_COIN)
    return boite(g.ops, w=100, h=H_COIN)


# --------------------------------------------------------------------------- #
# Pied : rinceau ondé d'acanthe, corbeille et chimères, bouts fermés par un filet (y vers le bas)
# --------------------------------------------------------------------------- #
P_PIED = 190.0
A_ONDE = 20.0
W_BOUT = 44.0
W_CORB = 380.0
PENTE = 2 * math.pi * A_ONDE / P_PIED      # pente de la tige aux raccords (descend vers la droite)


def onde(x):
    return 50 + A_ONDE * math.sin(2 * math.pi * x / P_PIED)


def filets_pied(g, x0, x1):
    for y in R_PIED:
        g.trait(Path().M((x0, y)).L((x1, y)), FILET, rond=False)


def vrille(g, x0, y0, cx, cy, r, a0, sens):
    """Vrille fine qui part de la tige et s'enroule, une baie au bout."""
    g.trait(spirale(cx, cy, r, 1.2, a0, 1.1, sens=sens, avant=[(x0, y0)]), FIN)


def rinceau():
    """Une période : la tige ondée, une grande volute au-dessus du creux et une sous la crête, chacune partie de la
    tige à travers une gaine d'acanthe et enroulée autour d'une fleurette ; des vrilles dans les vides."""
    P = P_PIED
    g = Dessin(pied())
    filets_pied(g, -2, P + 2)
    # vrilles : sous le creux, au-dessus de la crête
    vrille(g, 44, onde(44), 66, 86, 6, 180, -1)
    vrille(g, 128, onde(128), 116, 15, 6, 0, -1)
    perle(g, 66, 86, 2.0)
    perle(g, 116, 15, 2.0)
    for (cx, cy, a0, sens, lead, fleur) in (
            (50, 34, -45, -1, [(92, onde(92)), (84, 32), (73, 17)], -1),
            (145, 66, 45, 1, [(183, onde(183)), (177, 68), (167, 83)], 1)):
        v = spirale(cx, cy, 23, 5.5, a0, 1.05, sens=sens, avant=lead)
        g.ruban(v, 5.0)
        if fleur < 0:
            g.silhouette(place(acanthe(22, 12, lobes=1), cx - 14, cy - 17, 205))
            trefle(g, cx + 2, cy + 1, 5.0, 60)
        else:
            g.silhouette(place(acanthe(22, 12, lobes=1), cx + 14, cy + 17, 25))
            tulipe(g, cx - 1, cy - 1, 11, -120)
    # la tige ondée
    xs = [P * i / 16 for i in range(-1, 18)]
    g.tige_creuse(smooth([(x, onde(x)) for x in xs]), 5.6)
    # gaines d'acanthe aux départs des volutes, couchées sur la tige vers l'arrière (rien d'autre que la tige ne
    # passe les bouts de la période : la frise est coupée net contre les bouts et la pièce centrale)
    feuille(g, 92, onde(92), 145, 34, 20, lobes=2)
    feuille(g, 184, onde(184), 215, 34, 20, lobes=2)
    return frise(P, g.ops)


def bout_pied():
    """Bout du panneau du pied : filet de fermeture, départ de la tige vers un bouton de tulipe, vrille et feuille."""
    W = W_BOUT
    g = Dessin(pied())
    g.trait(Path().M((W + 2, R_PIED[0])).L((FERME, R_PIED[0])).L((FERME, R_PIED[1])).L((W + 2, R_PIED[1])),
            FILET, rond=False)
    vrille(g, W - 4, 53, W - 28, 76, 6, -90, 1)
    perle(g, W - 28, 76, 2.0)
    feuille(g, W - 8, 50 - 8 * PENTE, 110, 30, 16, lobes=1)
    t = smooth([(W + 4, 50 + 4 * PENTE), (W, 50), (W - 12, 50 - 12 * PENTE), (W - 24, 30), (W - 27, 22)])
    g.tige_creuse(t, 5.6)
    tulipe(g, W - 27, 21, 13, -100)
    return boite(g.ops, w=W)


def chimere(g, s):
    """Chimère du pied, côté s (-1 à gauche) : queue d'acanthe le long du bas, corps en volute, poitrail en S à
    crinière, tête de griffon tournée vers le dehors, bec ouvert tenant une vrille."""
    E = W_CORB / 2
    # vrille tenue dans le bec, enroulée vers le dehors, une baie au bout
    g.trait(spirale(s * 148, 22, 8, 1.5, 180, 1.1, sens=s, avant=[(s * 128, 27), (s * 140, 31)]), FIN)
    g.trait(Path().M((s * 140, 31)).C((s * 146, 38), (s * 152, 42), (s * 160, 40)), HACH)
    perle(g, s * 161, 40, 2.4)
    # la tige du rinceau arrive au bord de la pièce et devient la queue
    q = smooth([(s * (E + 4), 50 - 4 * PENTE), (s * E, 50), (s * (E - 18), 64), (s * (E - 40), 78),
                (s * (E - 64), 86), (s * 116, 86)])
    g.tige_creuse(q, 5.6)
    feuille(g, s * (E - 52), 84, 0 if s > 0 else 180, 46, 22, lobes=2)
    # corps : grosse volute d'acanthe
    corps = spirale(s * 104, 72, 13, 3.5, 90, 1.15, sens=-s, avant=[(s * 122, 86), (s * 112, 85)])
    g.ruban(corps, 6.4)
    g.silhouette(place(acanthe(24, 14, lobes=1), s * 92, 80, 200 if s > 0 else -20))
    # poitrail en S, crinière hachurée
    cou = smooth([(s * 96, 72), (s * 90, 58), (s * 92, 44), (s * 100, 32)])
    g.ruban(cou, 11.0)
    crin = Path()
    for x, y in ((84, 62), (83, 55), (84, 48), (86, 41), (89, 35)):
        crin.M((s * x, y)).L((s * (x + 6), y - 3))
    g.trait(crin, HACH)
    # tête de griffon : crâne rond, bec crochu entrouvert, œil sous l'arcade, aigrette
    g.forme(Path().M((s * 95, 15)).L((s * 87, 4)).L((s * 101, 11)).Z(), FIN)
    tete = smooth([(s * 92, 31), (s * 91, 19), (s * 99, 10), (s * 110, 9), (s * 120, 12), (s * 127, 20),
                   (s * 125, 25), (s * 120, 20), (s * 111, 22), (s * 120, 26), (s * 116, 30), (s * 105, 33)],
                  closed=True)
    g.forme(tete, FIN)
    g.plein(circle(s * 108, 16.5, 1.8))
    g.trait(Path().M((s * 103, 13.5)).C((s * 106, 12), (s * 110, 12), (s * 112, 14)), HACH)


def corbeille(g):
    """Corbeille à lambrequins entre deux crosses d'acanthe, coiffée de deux crosses adossées ; trois perles dessous."""
    for s in (-1, 1):
        cr = spirale(s * 66, 76, 9, 2.2, 0 if s > 0 else 180, 1.1, sens=s,
                     avant=[(s * 62, 30), (s * 74, 40), (s * 79, 58)])
        g.silhouette(place(acanthe(20, 12, lobes=1), s * 76, 46, s * 70 if s > 0 else 180 - 70))
        g.ruban(cr, 5.0)
    # cordon et perles sous la corbeille
    g.trait(smooth([(-60, 84), (-30, 92), (0, 94), (30, 92), (60, 84)]), HACH)
    for x in (-14, 0, 14):
        perle(g, x, 93.5 - abs(x) * 0.08, 2.6)
    corps = Path().M((-56, 34)).L((56, 34)).C((50, 52), (44, 66), (42, 76)).C((36, 84), (24, 86), (14, 80)) \
        .C((8, 88), (-8, 88), (-14, 80)).C((-24, 86), (-36, 84), (-42, 76)).C((-44, 66), (-50, 52), (-56, 34)).Z()
    g.forme(corps)
    h = Path()
    y = 38.0
    while y < 80:
        k = (y - 34) / 42
        x = 54 - 13 * k
        h.M((-x + 2, y)).L((x - 2, y))
        y += 3.0
    g.trait(h, HACH, rond=False)
    # lambrequin : quatre pointes sous la corniche
    lam = Path()
    for i in range(4):
        x0 = -33 + 22 * i
        lam.extend(Path().M((x0 - 9, 36)).L((x0 + 9, 36)).L((x0, 52)).Z())
    g.forme(lam, FIN)
    g.forme(rect(-62, 26, 124, 9))
    g.trait(Path().M((-62, 30.5)).L((62, 30.5)), HACH, rond=False)
    # cimier : deux crosses adossées et un nœud
    for s in (-1, 1):
        g.ruban(spirale(s * 20, 14, 7, 1.8, 90, 1.0, sens=-s, avant=[(s * 2, 25), (s * 12, 22)]), 3.6)
    perle(g, 0, 20, 3.8)


def centre_pied():
    W = W_CORB
    g = Dessin(pied())
    filets_pied(g, -W / 2 - 1, W / 2 + 1)
    for s in (-1, 1):
        chimere(g, s)
    corbeille(g)
    return piece(W, g.ops)


def spec():
    return cadre(
        name="litanies",
        title="Litanies de la Sainte Vierge",
        source="paroissien de la fin du XIXᵉ siècle, p.29 (Prière du soir), photo litanies-candelabre-et-pied",
        inks={},
        palettes={},
        layout={"margin": 12, "top": S, "side": S, "bottom": S, "textPadding": 10},
        ground=None,
        disposition="equerre_pied",
        top=frise(100, []),
        side=etage(),
        bottom=rinceau(),
        centers={"side": centre_cote(), "bottom": centre_pied()},
        corners={"bottom": coin_pied()},
        ends={"side": bouts(couronnement(), fin_cote()), "bottom": bouts(bout_pied())},
    )
