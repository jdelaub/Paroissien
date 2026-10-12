"""
Prépare une photo de page pour l'analyse d'un cadre : orientation, quadrillage repéré, zooms.

  - L'orientation EXIF est appliquée, puis une rotation éventuelle (photos prises de côté).
    Toutes les coordonnées données aux autres scripts se rapportent à cette image orientée.
  - Le quadrillage (une ligne tous les --grille pixels, cotée) sert à relever les zones :
    papier nu, coins, bandes, et la période d'une frise (distance entre deux motifs identiques).
  - Chaque --zone produit un zoom net (LANCZOS) pour lire le dessin d'un module.

    python3 -I tools/cadres/analyse/decoupe.py photo.jpg --sortie /tmp/p103 --grille 100 \
        --zone coin-haut-gauche 120 860 700 1300 --zone pied 300 2470 900 2740 --zoom 2

Dépendances : Pillow et NumPy (pip install pillow numpy).
"""
import argparse
import os

from PIL import Image, ImageDraw, ImageFont, ImageOps


def oriented(path, rotation):
    """Image en RVB, orientation EXIF appliquée puis rotation (degrés, sens horaire)."""
    image = ImageOps.exif_transpose(Image.open(path)).convert("RGB")
    if rotation:
        image = image.rotate(-rotation, expand=True)
    return image


def with_grid(image, step, width=1600):
    """Vue réduite avec un quadrillage coté en pixels de l'image orientée."""
    scale = min(1.0, width / image.width)
    view = image.resize((round(image.width * scale), round(image.height * scale)), Image.LANCZOS)
    draw = ImageDraw.Draw(view)
    try:
        font = ImageFont.load_default(size=16)  # Pillow 10.1 et plus
    except TypeError:
        font = ImageFont.load_default()
    for x in range(0, image.width, step):
        draw.line([(x * scale, 0), (x * scale, view.height)], fill=(255, 0, 160), width=1)
        draw.text((x * scale + 3, 3), str(x), fill=(255, 0, 160), font=font)
    for y in range(0, image.height, step):
        draw.line([(0, y * scale), (view.width, y * scale)], fill=(0, 140, 255), width=1)
        draw.text((3, y * scale + 3), str(y), fill=(0, 140, 255), font=font)
    return view


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("photo")
    parser.add_argument("--sortie", required=True, help="dossier de sortie")
    parser.add_argument("--rotation", type=int, default=0, choices=[0, 90, 180, 270], help="sens horaire, après l'EXIF")
    parser.add_argument("--grille", type=int, default=100, help="pas du quadrillage en pixels (0 : aucun)")
    parser.add_argument("--zone", nargs=5, action="append", default=[], metavar=("NOM", "X0", "Y0", "X1", "Y1"))
    parser.add_argument("--zoom", type=float, default=2.0)
    args = parser.parse_args()

    os.makedirs(args.sortie, exist_ok=True)
    base = os.path.splitext(os.path.basename(args.photo))[0]
    image = oriented(args.photo, args.rotation)
    full = os.path.join(args.sortie, f"{base}-oriente.png")
    image.save(full)
    print(f"{full}  {image.width} × {image.height}")
    if args.grille:
        grid = os.path.join(args.sortie, f"{base}-grille.png")
        with_grid(image, args.grille).save(grid)
        print(grid)
    for name, *box in args.zone:
        x0, y0, x1, y1 = map(int, box)
        crop = image.crop((x0, y0, x1, y1))
        crop = crop.resize((round(crop.width * args.zoom), round(crop.height * args.zoom)), Image.LANCZOS)
        out = os.path.join(args.sortie, f"{base}-{name}.png")
        crop.save(out)
        print(out)


if __name__ == "__main__":
    main()
