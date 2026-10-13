"""
Vignettes gravées : d'une photo de page à l'image que l'app pose en tête de section.

Une gravure tramée ne se redessine pas en vecteurs : on la redresse, on corrige l'éclairage, puis on n'en garde que
l'encre, en masque alpha (le papier devient transparent). L'app teint ce masque de l'encre du thème, comme une
impression : sépia sur papier clair, encre claire sur fond sombre.

    python3 -I tools/cadres/vignettes.py <photo> --rotation 180 \
        --coins 1050,40 2860,60 2850,500 1040,480 --nom bapteme --titre "Baptême du Christ" \
        --page 50 --sens bapteme baptism --apercu

  --coins : les quatre angles de la gravure dans la photo orientée (EXIF, puis rotation horaire comme decoupe.py),
            dans l'ordre haut gauche, haut droit, bas droit, bas gauche ; `analyse/decoupe.py --grille` aide à les lire.
  --sens  : mots du titre d'une section qui appellent cette vignette (sans accents), comme pour les cadres.

Les vignettes viennent de photos personnelles : elles vont dans le dépôt privé (par défaut
../paroissien-content/vignettes/, avec son catalog.json), jamais dans ce dépôt public. tools/deliver-apk.sh les
copie dans les assets de la saveur personnelle.
"""
import argparse
import json
import os
import sys
import tempfile

from PIL import Image, ImageFilter, ImageOps

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(os.path.dirname(HERE))
DEFAULT_OUT = os.path.join(os.path.dirname(ROOT), "paroissien-content", "vignettes")


def oriented(path, rotation):
    image = ImageOps.exif_transpose(Image.open(os.path.expanduser(path))).convert("RGB")
    return image.rotate(-rotation, expand=True) if rotation else image


def straighten(image, corners):
    """Redresse le quadrilatère (haut gauche, haut droit, bas droit, bas gauche) en rectangle."""
    (x0, y0), (x1, y1), (x2, y2), (x3, y3) = corners
    dist = lambda a, b, c, d: ((a - c) ** 2 + (b - d) ** 2) ** 0.5
    w = round((dist(x0, y0, x1, y1) + dist(x3, y3, x2, y2)) / 2)
    h = round((dist(x0, y0, x3, y3) + dist(x1, y1, x2, y2)) / 2)
    # QUAD : haut gauche, bas gauche, bas droit, haut droit
    return image.transform((w, h), Image.Transform.QUAD, (x0, y0, x3, y3, x2, y2, x1, y1), Image.Resampling.BICUBIC)


def percentile(gray, q):
    hist = gray.histogram()
    target = q * sum(hist)
    acc = 0
    for v, n in enumerate(hist):
        acc += n
        if acc >= target:
            return v
    return 255


def ink_mask(image, gamma=0.85):
    """Encre seule, en alpha : l'éclairage inégal de la photo est retiré par un fond estimé (le papier, clair,
    l'emporte dans un filtre max), puis les niveaux sont étirés entre l'encre la plus noire et le papier."""
    gray = image.convert("L")
    k = max(3, (min(gray.size) // 40) | 1)
    paper = gray.filter(ImageFilter.MaxFilter(k)).filter(ImageFilter.GaussianBlur(k * 1.5))
    flat = Image.new("L", gray.size)
    flat.putdata([min(255, round(g * 235 / max(p, 1))) for g, p in zip(gray.getdata(), paper.getdata())])
    white, black = percentile(flat, 0.90), percentile(flat, 0.01)
    span = max(1, white - black)
    lut = [round(255 * min(1.0, max(0.0, (white - v) / span)) ** gamma) for v in range(256)]
    return flat.point(lut)


def main():
    a = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    a.add_argument("photo")
    a.add_argument("--coins", nargs=4, required=True, metavar="X,Y")
    a.add_argument("--rotation", type=int, default=0, choices=[0, 90, 180, 270])
    a.add_argument("--nom", required=True, help="identifiant (lettres, chiffres, tirets)")
    a.add_argument("--titre", required=True)
    a.add_argument("--page", type=int)
    a.add_argument("--sens", nargs="*", default=[])
    a.add_argument("--largeur", type=int, default=1000, help="largeur de sortie en pixels")
    a.add_argument("--gamma", type=float, default=0.85, help="< 1 renforce les gris de la trame")
    a.add_argument("--sortie", default=DEFAULT_OUT)
    a.add_argument("--apercu", action="store_true",
                   help="écrit aussi <nom>-apercu.png (encre sur papier crème) dans $CADRES_SORTIE ou <tmp>/cadres")
    o = a.parse_args()
    if os.path.commonpath([os.path.abspath(o.sortie), ROOT]) == ROOT:
        sys.exit("Les vignettes viennent de photos personnelles : pas dans le dépôt public.")
    corners = [tuple(float(v) for v in c.split(",")) for c in o.coins]

    piece = straighten(oriented(o.photo, o.rotation), corners)
    alpha = ink_mask(piece, o.gamma)
    if alpha.width > o.largeur:
        alpha = alpha.resize((o.largeur, round(alpha.height * o.largeur / alpha.width)), Image.Resampling.LANCZOS)
    os.makedirs(o.sortie, exist_ok=True)
    out = Image.new("RGBA", alpha.size, (0, 0, 0, 0))
    out.putalpha(alpha)
    name = f"{o.nom}.webp"
    out.save(os.path.join(o.sortie, name), "WEBP", quality=88, method=6)
    if o.apercu:
        paper = Image.new("RGB", alpha.size, (241, 232, 214))
        paper.paste(Image.new("RGB", alpha.size, (58, 40, 28)), mask=alpha)
        preview_dir = os.environ.get("CADRES_SORTIE") or os.path.join(tempfile.gettempdir(), "cadres")
        os.makedirs(preview_dir, exist_ok=True)
        paper.save(os.path.join(preview_dir, f"{o.nom}-apercu.png"))
        print(os.path.join(preview_dir, f"{o.nom}-apercu.png"))

    catalog_path = os.path.join(o.sortie, "catalog.json")
    catalog = {"format": 1, "vignettes": []}
    if os.path.exists(catalog_path):
        with open(catalog_path, encoding="utf-8") as f:
            catalog = json.load(f)
    entry = {"id": o.nom, "title": o.titre, "file": name, "width": alpha.width, "height": alpha.height,
             "senses": o.sens, "page": o.page, "source": os.path.basename(o.photo)}
    catalog["vignettes"] = [v for v in catalog["vignettes"] if v["id"] != o.nom] + [entry]
    with open(catalog_path, "w", encoding="utf-8") as f:
        json.dump(catalog, f, ensure_ascii=False, indent=2)
        f.write("\n")
    print(os.path.join(o.sortie, name), f"{alpha.width} × {alpha.height}")


if __name__ == "__main__":
    main()
