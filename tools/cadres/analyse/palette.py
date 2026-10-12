"""
Mesure les encres d'un cadre chromolithographié sur une photo.

Une chromolithographie imprime une pierre par encre : un cadre compte 4 à 9 couleurs en aplats.
Le script :
  1. corrige la photo sur le blanc du papier : chaque canal est multiplié pour qu'une zone de papier
     nu, prise près du cadre, devienne --papier-cible. Les photos de livre sont souvent sous-exposées
     et teintées par l'éclairage : sans cette correction, les mesures sont grises et sombres ;
  2. regroupe les couleurs de la zone du cadre par k-moyennes ;
  3. affiche les groupes du plus fréquent au plus rare et écrit une image quantifiée pour vérifier
     à l'œil qu'à chaque encre correspond un groupe.

Le résultat est un point de départ. Les encres finales (INKS dans generate.py) se choisissent à l'œil :
l'éclairage est rarement uniforme, et l'or ou l'argent ressortent mal en photo.

    python3 -I tools/cadres/analyse/palette.py photo.jpg --papier 500 2760 800 2790 \
        --cadre 300 2490 900 2720 --k 8 --apercu /tmp/p103/quantif.png

Les coordonnées se rapportent à l'image orientée (voir decoupe.py, même option --rotation).
Dépendances : Pillow et NumPy (pip install pillow numpy).
"""
import argparse
import os
import sys

import numpy as np
from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))  # python3 -I n'ajoute pas le dossier du script
from decoupe import oriented  # noqa: E402


def kmeans(pixels, k, seed=0, iterations=80, sample=60000):
    rng = np.random.default_rng(seed)
    if len(pixels) > sample:
        pixels = pixels[rng.choice(len(pixels), sample, replace=False)]
    centers = pixels[rng.choice(len(pixels), k, replace=False)]
    for _ in range(iterations):
        labels = ((pixels[:, None, :] - centers[None]) ** 2).sum(-1).argmin(1)
        moved = np.array([pixels[labels == j].mean(0) if (labels == j).any() else centers[j] for j in range(k)])
        if np.allclose(moved, centers):
            break
        centers = moved
    return centers


def hexa(rgb):
    return "#%02X%02X%02X" % tuple(int(round(v)) for v in rgb)


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("photo")
    parser.add_argument("--papier", nargs=4, type=int, required=True, metavar=("X0", "Y0", "X1", "Y1"),
                        help="zone de papier nu, sans encre, près du cadre")
    parser.add_argument("--cadre", nargs=4, type=int, required=True, metavar=("X0", "Y0", "X1", "Y1"),
                        help="zone du cadre à mesurer (une bande bien éclairée)")
    parser.add_argument("--papier-cible", default="#F1E8D6", help="teinte du papier après correction")
    parser.add_argument("--k", type=int, default=8, help="nombre de groupes (encres + contour + papier)")
    parser.add_argument("--rotation", type=int, default=0, choices=[0, 90, 180, 270])
    parser.add_argument("--apercu", help="écrit l'image quantifiée (agrandie ×2)")
    args = parser.parse_args()

    image = np.asarray(oriented(args.photo, args.rotation)).astype(float)
    x0, y0, x1, y1 = args.papier
    paper = np.median(image[y0:y1, x0:x1].reshape(-1, 3), 0)
    target = np.array([int(args.papier_cible[i:i + 2], 16) for i in (1, 3, 5)], float)
    gain = target / np.maximum(paper, 1)
    print(f"papier mesuré {hexa(paper)}, gains R {gain[0]:.2f}  V {gain[1]:.2f}  B {gain[2]:.2f}")

    x0, y0, x1, y1 = args.cadre
    zone = np.clip(image[y0:y1, x0:x1] * gain, 0, 255)
    pixels = zone.reshape(-1, 3)
    centers = kmeans(pixels, args.k)
    labels = ((pixels[:, None, :] - centers[None]) ** 2).sum(-1).argmin(1)
    counts = np.bincount(labels, minlength=args.k)
    print("groupes, du plus fréquent au plus rare :")
    for j in np.argsort(-counts):
        print(f"  {hexa(centers[j])}  {100 * counts[j] / counts.sum():5.1f} %")

    if args.apercu:
        quantized = centers[labels].reshape(zone.shape).astype(np.uint8)
        out = Image.fromarray(quantized)
        out.resize((out.width * 2, out.height * 2), Image.NEAREST).save(args.apercu)
        print("aperçu :", args.apercu)


if __name__ == "__main__":
    main()
