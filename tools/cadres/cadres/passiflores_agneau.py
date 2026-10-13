"""
Bandeau « Passiflores et Agneau », d'après un paroissien imprimé à la fin du XIXᵉ siècle (p.109, « Carême », et
p.221 : le même bloc revient de page en page ; photos bandeau-passiflores-lyre-a et -b, prises de côté, rotation 270°,
même dessin sur les deux). Une similigravure brun sépia sur papier nu, en bandeau le long du côté extérieur de la page
seulement : une liane de passiflore (feuilles palmées à cinq lobes en étoile, fleurs vues de face ou de trois quarts
avec leur couronne de filaments, vrilles) monte du pied d'un seul élan ; au tiers de sa hauteur, une pièce la coupe :
une croix pattée chargée d'un anneau où l'Agneau couché, tourné vers la gauche, repose sur une barre, sur fond
sombre (ce n'est pas une lyre, malgré le nom des photos). Au pied, la liane part de grandes vrilles enroulées.

Relevé (photo a, 1638 × 4000 px orientée) : bandeau de ≈ 350 px de large pour une ligne de texte de 88 px, à 170 px
du texte ; une paire de fleurs tous les ≈ 550 px (1,6 largeur), séparées par des grappes de feuilles ; pièce
centrale ≈ 330 × 320 px, aussi large que le bandeau, au tiers de la hauteur. Palette, après correction du blanc :
trait #69513F au plus sombre (flou compris : l'encre pure est plus sombre), demi-teinte de la trame #C8B5A1.

Redessin et simplifications :
  - les traits et contours sont dans l'encre du texte (`text`), pour rester lisibles en thème sombre ; deux encres
    propres : `trait` (les aplats sépia pleins) et `teinte` (la trame claire, en aplat, seule atténuée en sombre) ;
    `paper` évide la couronne des fleurs et l'anneau ;
  - le bandeau fait 30 dp (le haut de la plage d'une bande latérale) au lieu du quart de page de l'original ;
  - la liane est une frise régulière, une paire de fleurs et quatre ou cinq feuilles par période de 200 unités, au
    lieu d'une composition libre ; elle traverse la pièce sans repartir en miroir, et ses feuilles passent derrière
    les bras de la croix ;
  - deux bouts distincts : la pointe de la liane en haut (bouton, vrille ; le haut de l'original est caché par
    l'image), le départ en vrilles au pied ;
  - la pièce centrale est « droite » : sur un verso, elle est déplacée sans miroir. Un bloc d'imprimerie ne se
    retourne pas, et l'Agneau de l'iconographie regarde vers la gauche : c'est le même emblème sur les deux pages.

Repère des fonctions de dessin : la bande comme une bande haute (u le long, v de 0 = bord extérieur à 100 = texte).
La pièce centrale se dessine dans le repère de la page de droite (X vers l'extérieur, Y vers le bas), puis `PAGE` la
porte dans celui de la bande.
"""
import math

from commun import Path, affine, boite, bouts, cadre, circle, ellipse, frise, leaf, piece, polygon, rect, smooth

ORDRE = 109

INKS = {
    "trait": "#563E2F",   # brun sépia des aplats pleins
    "teinte": "#CDB9A2",  # demi-teinte de la trame
}

TRAIT, TEINTE, PAPIER = "trait", "teinte", "paper"
LIGNE = "text"    # traits et contours : l'encre du texte du thème, foncée en clair, claire en sombre
FONCE = TRAIT     # aplats sombres : feuilles sombres, fond de l'anneau, cœur des fleurs

W_TIGE = 2.8      # tige de la liane
W_PETIOLE = 1.6   # queues des feuilles et des fleurs
W_VRILLE = 1.3    # vrilles
CONTOUR = 1.2     # contour des feuilles et des pétales (de chaque côté)
W_NERVURE = 0.9   # nervures

P = 200.0         # période de la frise (unités) : une paire de fleurs et quatre feuilles


def PAGE(p):
    """Repère de la page (X vers l'extérieur depuis le milieu de la bande, Y vers le bas) -> repère de la bande."""
    return (p[1], 50.0 - p[0])


# --------------------------------------------------------------------------- #
# Tracés : (encre, chemin, épaisseur ou None pour un aplat), émis par `ops` dans le repère voulu
# --------------------------------------------------------------------------- #
def fill(ink, path):
    return (ink, path, None)


def stroke(ink, path, w):
    return (ink, path, w)


def contour(ink, path, o=CONTOUR):
    """Forme remplie avec contour sépia : le trait large dessous, l'aplat dessus."""
    return [stroke(LIGNE, path, 2 * o), fill(ink, path)]


def ops(traces, f=None):
    """Opérations de dessin du moteur, après la transformation `f` des chemins (traits ronds)."""
    out = []
    for ink, path, w in traces:
        q = path.transformed(f) if f else path
        if w is None:
            out.append({"ink": ink, "d": q.normalized().d()})
        else:
            out.append({"ink": ink, "d": q.d(), "stroke": round(w, 2), "round": True})
    return out


def petale(L, W):
    """Pétale oblong à bout arrondi, pointant vers +x, base en (0, 0)."""
    return (Path().M((0, 0))
            .C((0.1 * L, -0.55 * W), (0.55 * L, -0.62 * W), (0.84 * L, -0.46 * W))
            .C((1.03 * L, -0.3 * W), (1.03 * L, 0.3 * W), (0.84 * L, 0.46 * W))
            .C((0.55 * L, 0.62 * W), (0.1 * L, 0.55 * W), (0, 0)).Z())


def feuille(cx, cy, L, rot, sombre=False):
    """Feuille de passiflore : cinq lobes lancéolés en étoile autour de (cx, cy), le premier vers `rot`."""
    lobes, nervures = Path(), Path()
    for da, k in ((0, 1.0), (70, 0.92), (142, 0.8), (218, 0.8), (290, 0.92)):
        a = rot + da
        lobes.extend(leaf(L * k, 0.3 * L * k).transformed(affine(cx, cy, rot=a)))
        c, s = math.cos(math.radians(a)), math.sin(math.radians(a))
        nervures.M((cx, cy)).L((cx + 0.78 * L * k * c, cy + 0.78 * L * k * s))
    t = contour(FONCE if sombre else TEINTE, lobes)
    t.append(stroke(TEINTE if sombre else LIGNE, nervures, W_NERVURE))
    return t


def passiflore(cx, cy, r, rot=0.0, aplati=1.0):
    """Fleur de passiflore vue de face : dix pétales, couronne de filaments, cœur. `aplati` < 1 la voit de biais."""
    f = lambda p: affine(cx, cy, rot=rot)((p[0], p[1] * aplati))
    petales = Path()
    for i in range(10):
        p = petale(0.78 * r, (0.42 if i % 2 else 0.36) * r).transformed(affine(0.22 * r))
        petales.extend(p.transformed(affine(rot=36 * i)))
    petales = petales.transformed(f)
    t = contour(TEINTE, petales)
    t.append(fill(PAPIER, ellipse(0, 0, 0.66 * r, 0.66 * r).transformed(f)))
    filaments = Path()
    for i in range(22):
        a = math.radians(360 * i / 22)
        c, sn = math.cos(a), math.sin(a)
        filaments.M((0.32 * r * c, 0.32 * r * sn)).L((0.66 * r * c, 0.66 * r * sn))
    t.append(stroke(LIGNE, filaments.transformed(f), 1.0))
    t += contour(TEINTE, circle(0, 0, 0.27 * r).transformed(f), o=0.9)
    t.append(fill(FONCE, circle(0, 0, 0.1 * r).transformed(f)))
    return t


def vrille(x, y, ang, r, tours=1.4, sens=1):
    """Vrille qui part de (x, y) dans la direction `ang` et s'enroule (sens 1 : horaire) en se resserrant."""
    a = math.radians(ang)
    cx, cy = x - sens * r * math.sin(a), y + sens * r * math.cos(a)
    phi0 = ang - 90 * sens
    n = int(12 * tours) + 2
    pts = []
    for i in range(n + 1):
        t = i / n
        ph = math.radians(phi0 + sens * 360 * tours * t)
        rr = r * (1 - 0.7 * t)
        pts.append((cx + rr * math.cos(ph), cy + rr * math.sin(ph)))
    return smooth(pts)


class Tige:
    """Courbe lisse par des points (u, v). Avec `period`, la courbe déborde d'un segment de chaque côté de la
    période : deux instances voisines se recouvrent exactement."""

    def __init__(self, pts, period=None):
        pts = list(pts)
        if period:
            q = [(u - period, v) for u, v in pts[-2:]] + pts + [(u + period, v) for u, v in pts[:3]]
        else:
            q = [pts[0]] + pts + [pts[-1]]
        self.segs = []
        for i in range(1, len(q) - 2):
            p0, p1, p2, p3 = q[i - 1], q[i], q[i + 1], q[i + 2]
            c1 = (p1[0] + (p2[0] - p0[0]) / 6, p1[1] + (p2[1] - p0[1]) / 6)
            c2 = (p2[0] - (p3[0] - p1[0]) / 6, p2[1] - (p3[1] - p1[1]) / 6)
            self.segs.append((p1, c1, c2, p2))

    def path(self):
        p = Path().M(self.segs[0][0])
        for s in self.segs:
            p.C(s[1], s[2], s[3])
        return p

    def at(self, u):
        """Point de la courbe d'abscisse la plus proche de u."""
        best = None
        for s in self.segs:
            for j in range(25):
                t = j / 24
                m = 1 - t
                p = tuple(m ** 3 * s[0][k] + 3 * m * m * t * s[1][k] + 3 * m * t * t * s[2][k] + t ** 3 * s[3][k]
                          for k in (0, 1))
                if best is None or abs(p[0] - u) < abs(best[0] - u):
                    best = p
        return best


def queue(tige, u, x, y):
    """Queue droite et légèrement courbe, de la tige (abscisse u) jusqu'au point (x, y)."""
    a = tige.at(u)
    m = ((a[0] + x) / 2 + 0.12 * (y - a[1]), (a[1] + y) / 2 - 0.12 * (x - a[0]))
    return stroke(LIGNE, smooth([a, m, (x, y)]), W_PETIOLE)


# --------------------------------------------------------------------------- #
# Frise : la liane
# --------------------------------------------------------------------------- #
TIGE = [(0, 50), (50, 58), (100, 42), (150, 58)]

# La liane monte du pied : les queues partent de la tige sous chaque feuille ou fleur (u croît vers le bas). Les
# éléments peuvent chevaucher les périodes voisines, et mordre de DEBORDE unités sur la pièce centrale et les bouts
# (au-delà, ils seraient coupés net).
DEBORDE = 12.0
# (u, v, longueur des lobes, orientation, sombre)
FEUILLES = [
    (6, 16, 18, 20, False),
    (112, 20, 24, 100, False),
    (130, 76, 29, -15, True),
    (158, 26, 26, 30, False),
    (188, 80, 23, 75, False),
]
# (u, v, rayon, orientation, aplati) : l'une de face, l'autre de trois quarts
FLEURS = [
    (40, 30, 27, 10, 1.0),
    (74, 70, 25, -25, 0.74),
]
# vrilles : (u sur la tige, direction, rayon, sens)
VRILLES = [(96, -120, 8, 1), (176, 100, 7, -1)]


def frise_liane():
    tige = Tige(TIGE, period=P)
    traces = [stroke(LIGNE, tige.path(), W_TIGE)]
    for u, ang, r, sens in VRILLES:
        x, y = tige.at(u)
        a = math.radians(ang)
        x1, y1 = x + 10 * math.cos(a), y + 10 * math.sin(a)
        traces.append(stroke(LIGNE, Path().M((x, y)).L((x1, y1)).extend(vrille(x1, y1, ang, r, sens=sens)),
                             W_VRILLE))
    for u, v, L, rot, sombre in FEUILLES:
        traces.append(queue(tige, u + 10, u, v))
    for u, v, L, rot, sombre in FEUILLES:
        traces += feuille(u, v, L, rot, sombre)
    for u, v, r, rot, ap in FLEURS:
        traces.append(queue(tige, u + 16, u, v))
        traces += passiflore(u, v, r, rot, ap)
    return frise(P, ops(traces), symetrique=False, deborde=DEBORDE)


# --------------------------------------------------------------------------- #
# Bouts : boîtes dessinées comme au début de la bande (u = 0 au bout, la frise arrive en u = w, v = 50) ; le moteur
# retourne celle du pied
# --------------------------------------------------------------------------- #
W_POINTE, W_PIED = 100.0, 120.0


def bouton(x, y, ang, L):
    """Bouton de passiflore : un ovale clair entre deux sépales, pointant vers `ang`."""
    t = contour(TEINTE, leaf(L, 0.62 * L).transformed(affine(x, y, rot=ang)))
    for da in (-28, 28):
        t += contour(TEINTE, leaf(0.7 * L, 0.26 * L).transformed(affine(x, y, rot=ang + da)), o=1.0)
    return t


def bout_pointe():
    """Haut de la bande : la liane s'amincit, un bouton, deux feuilles, et finit en vrille."""
    t = []
    tige = smooth([(W_POINTE + 8, 50), (W_POINTE, 50.3), (74, 47), (50, 56), (30, 54)])
    t.append(stroke(LIGNE, tige, W_TIGE * 0.85))
    t.append(stroke(LIGNE, vrille(30, 54, 200, 11, tours=1.5, sens=1), W_VRILLE))
    t.append(stroke(LIGNE, smooth([(58, 53), (44, 70), (30, 80)]), W_PETIOLE))
    t += bouton(30, 80, 210, 16)
    t.append(queue_droite((84, 48), (78, 24)))
    t += feuille(78, 24, 17, -60)
    t.append(queue_droite((66, 49), (66, 80)))
    t += feuille(66, 82, 15, 40, sombre=True)
    return boite(ops(t), w=W_POINTE)


def bout_pied():
    """Pied de la bande : la liane part de grandes vrilles enroulées, parmi de petites feuilles."""
    t = []
    tige = smooth([(W_PIED + 8, 50), (W_PIED, 50.3), (92, 50), (66, 60), (44, 68)])
    t.append(stroke(LIGNE, tige, W_TIGE))
    t.append(stroke(LIGNE, vrille(44, 68, 195, 15, tours=1.6, sens=-1), W_TIGE * 0.75))
    t.append(stroke(LIGNE, smooth([(74, 57), (56, 42), (40, 30)]).extend(vrille(40, 30, 205, 11, sens=1)), W_VRILLE))
    t.append(stroke(LIGNE, smooth([(100, 49), (88, 30), (76, 18)]).extend(vrille(76, 18, 235, 6, sens=-1)), W_VRILLE))
    t += feuille(102, 22, 17, 40)
    t += feuille(100, 80, 19, -20, sombre=True)
    t += feuille(72, 88, 13, 60)
    t += feuille(18, 26, 12, 15)
    return boite(ops(t), w=W_PIED)


def queue_droite(a, b):
    return stroke(LIGNE, Path().M(a).L(b), W_PETIOLE)


# --------------------------------------------------------------------------- #
# Pièce centrale : l'Agneau dans l'anneau, sur la croix pattée
# --------------------------------------------------------------------------- #
CW = 120.0   # longueur de la pièce le long de la bande


def agneau():
    corps = Path()
    corps.extend(ellipse(5, -2, 17, 8))
    corps.extend(circle(-13, -9, 7))
    corps.extend(ellipse(-19, -6.5, 5, 4))
    corps.extend(leaf(9, 4.5).transformed(affine(-10, -11, rot=25)))
    corps.extend(ellipse(-7, 4, 6, 2.5))
    t = contour(TEINTE, corps, o=1.0)
    laine = Path()
    for x in (-2, 6, 14):
        laine.extend(Path().M((x - 3, -4)).C((x - 2, -8), (x + 2, -8), (x + 3, -4)))
    t.append(stroke(LIGNE, laine, 0.9))
    t.append(fill(FONCE, circle(-15, -10.5, 1.3)))
    return t


def centre():
    t = []
    liane = Path().M((0, -70)).C((-6, -30), (6, 30), (0, 70))
    t.append(stroke(LIGNE, liane, W_TIGE))
    for (x, y, rot) in ((-32, -50, 80), (32, 50, 260), (34, -48, 120), (-34, 48, -60)):
        t += feuille(x, y, 16, rot)
    croix = polygon([(-28, -57), (28, -57), (12, -12), (48, -36), (48, 36), (12, 12), (28, 57), (-28, 57),
                     (-12, 12), (-48, 36), (-48, -36), (-12, -12)])
    t += contour(TEINTE, croix, o=1.4)
    t.append(stroke(LIGNE, polygon([(-21, -51), (21, -51), (0, -14)]), 0.8))
    t.append(stroke(LIGNE, polygon([(-21, 51), (21, 51), (0, 14)]), 0.8))
    t += contour(PAPIER, circle(0, 0, 34), o=1.4)
    t.append(fill(FONCE, circle(0, 0, 27)))
    t += contour(TEINTE, rect(-46, 6, 92, 9), o=1.0)
    t += agneau()
    return piece(CW, ops(t, PAGE), droit=True, a=1 / 3)


def spec():
    liane = frise_liane()
    return cadre(
        name="passiflores_agneau",
        title="Passiflores et Agneau",
        source="paroissien de la fin du XIXᵉ siècle, p.109 et p.221, photos bandeau-passiflores-lyre-a et -b",
        inks=INKS,
        palettes={},
        layout={"margin": 14, "top": 30, "side": 30, "bottom": 30, "textPadding": 12},
        ground=None,
        dimmed=["teinte"],
        disposition="laterale",
        top=liane,
        side=liane,
        centers={"side": centre()},
        ends={"side": bouts(bout_pointe(), bout_pied())},
    )
