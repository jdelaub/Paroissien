"""
Garde « Asanoha » : papier de contre-plat d'un paroissien de la fin du XIXᵉ siècle (photo garde-asanoha-or-et-bleu),
le motif japonisant de la feuille de chanvre. Un réseau de triangles équilatéraux dont chaque triangle est partagé en
trois par des traits menés de son centre à ses sommets : à chaque nœud se croisent douze traits (étoile à six
branches), au centre de chaque triangle trois (le « Y »). Traits bleu ardoise sur fond d'or ; l'or ne paraît plus
que dans les petits triangles obtus (30-30-120) laissés entre les traits.

Relevé (photo de 1638 × 4000 px, garde de 1310 px de large) :
  - nœuds en rangées horizontales, au pas de 198 px, une rangée sur deux décalée d'un demi-pas : 6,6 pas dans la
    largeur de la garde ; rangées tous les 184 px, soit 0,93 du pas au lieu de 0,866 (√3/2) : la photo est
    allongée de 7 % en hauteur (même allongement sur la garde aux symboles, prise de la même façon) ; le réseau est
    donc équilatéral ;
  - épaisseur perpendiculaire des traits (pas = 100 unités) : 11 pour les rayons verticaux, 10 pour les arêtes à
    60°, 8,5 pour les rayons à 30°, 6 pour les arêtes horizontales, soit 6 + 5·|sin θ| : l'impression a grossi les
    traits proches de la verticale (photo nette dans les deux sens, et les traits de l'autre garde n'ont pas cet
    écart). On garde cet écart : c'est lui qui fait lire les losanges d'or partagés d'un trait fin, et le bleu
    couvre la moitié de la surface comme sur la photo ;
  - palette (médianes, de la zone la mieux éclairée à la moins éclairée) : or #D8C69C à #B1A082, bleu #525D68 à
    #4E5965. Retenu : or #C3AB74, remonté (il sort terne et gris en photo), bleu ardoise #43536A, un peu saturé.

Dessin : plutôt que des traits (douze bouts carrés qui se croisent au nœud), le fond est bleu et chaque petit
triangle d'or est le triangle du réseau rentré de la demi-épaisseur de chacun de ses trois traits. Les angles de 30°
reculent d'autant vers l'extérieur : le gros point bleu au centre des étoiles vient de là, comme sur la photo.
Tuile : un nœud et ses deux triangles (pas de 100 unités, hauteur de rangée 50·√3), en quinconce. Largeur 54 dp :
6,7 pas sur un écran de 360 dp, autant que sur la garde du livre.
"""
import math

from commun import fill, motif, polygon

W = 100.0                     # pas du réseau (côté des triangles)
H = W * math.sqrt(3) / 2      # hauteur d'une rangée

INKS = {
    "bleu": "#43536A",  # bleu ardoise des traits (le fond de la tuile)
    "or": "#C3AB74",    # or des triangles
}


def epaisseur(a, b):
    """Épaisseur du trait de a à b selon son orientation (relevé : 6 à l'horizontale, 11 à la verticale)."""
    dx, dy = b[0] - a[0], b[1] - a[1]
    return 6.0 + 5.0 * abs(dy) / math.hypot(dx, dy)


def rentre(pts):
    """Triangle rentré de la demi-épaisseur de chacun de ses côtés (intersection des côtés décalés)."""
    aire = sum(p[0] * q[1] - q[0] * p[1] for p, q in zip(pts, pts[1:] + pts[:1]))
    sens = 1 if aire > 0 else -1
    lignes = []
    for p, q in zip(pts, pts[1:] + pts[:1]):
        dx, dy = q[0] - p[0], q[1] - p[1]
        n = math.hypot(dx, dy)
        nx, ny = -dy / n * sens, dx / n * sens          # normale vers l'intérieur
        lignes.append((nx, ny, nx * p[0] + ny * p[1] + epaisseur(p, q) / 2))
    out = []
    for (a1, b1, c1), (a2, b2, c2) in zip([lignes[-1]] + lignes[:-1], lignes):
        det = a1 * b2 - a2 * b1
        out.append(((c1 * b2 - c2 * b1) / det, (a1 * c2 - a2 * c1) / det))
    return out


def triangles():
    """Les six petits triangles d'une tuile : nœud en (0, 0), triangle pointe en bas à sa droite, pointe en haut
    sous lui ; chacun partagé en trois autour de son centre."""
    o, b, c, d = (0.0, 0.0), (W, 0.0), (W / 2, H), (-W / 2, H)
    out = []
    for tri in ((o, b, c), (o, c, d)):
        g = (sum(p[0] for p in tri) / 3, sum(p[1] for p in tri) / 3)
        for p, q in zip(tri, tri[1:] + tri[:1]):
            out.append([p, q, g])
    return out


def spec():
    ops = [fill("or", polygon(rentre(t))) for t in triangles()]
    return motif(
        name="asanoha",
        title="Asanoha",
        source="garde d'un paroissien de la fin du XIXᵉ siècle, photo garde-asanoha-or-et-bleu",
        inks=INKS,
        w=W,
        h=round(H, 3),
        ops=ops,
        largeur=54,
        reseau="quinconce",
        ground="bleu",
    )
