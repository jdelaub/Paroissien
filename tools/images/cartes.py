"""
Images pieuses photographiées (recto puis verso, carte après carte, à la même échelle) : d'une série de photos à une
bibliothèque propre pour l'app.

    python3 -I tools/images/cartes.py detecter <dossier-photos> --sortie <travail>
        Range les photos par nom (horodatage), les groupe par deux (recto, verso), trouve la carte sur le fond sombre,
        la redresse et écrit <travail>/brut/NN-recto.jpg, NN-verso.jpg, releve.json (taille en pixels, quadrilatère) et
        planche-*.jpg pour vérifier.

    python3 -I tools/images/cartes.py produire <dossier-photos> --cartes <cartes.json> --sortie <bibliotheque>
        Applique la description de chaque carte (cartes.json : rotation de chaque face, titre, catégorie, prière du
        verso…), détoure la carte (coins arrondis et bords dentelés gardés, le fond devient transparent) et écrit la
        bibliothèque au format de l'app : catalog.json, <id>.webp, <id>-verso.webp, thumbs/<id>.webp.

Les photos sont personnelles : la bibliothèque va dans le dépôt privé (paroissien-content/images-pieuses/), jamais
dans ce dépôt. Seul PIL est requis.
"""
import argparse
import json
import math
import os
import sys

from PIL import Image, ImageChops, ImageDraw, ImageFilter, ImageOps

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(os.path.dirname(HERE))

K = 4                 # réduction pour la détection
FULL_SIDE = 1440      # côté long d'une face dans l'app (PiousImages.MAX_SIDE)
THUMB_HEIGHT = 360    # vignette de la boîte à images


# --------------------------------------------------------------------------- #
# Détection de la carte sur le fond sombre
# --------------------------------------------------------------------------- #
def card_mask(im, bright=105, sat=70, dim=45):
    """Pixels de carte : clairs, ou colorés et pas trop sombres. Le fond (drap noir) est sombre et terne."""
    h, s, v = im.convert("HSV").split()
    light = v.point(lambda x: 255 if x > bright else 0)
    colored = ImageChops.multiply(s.point(lambda x: 255 if x > sat else 0), v.point(lambda x: 255 if x > dim else 0))
    return ImageChops.lighter(light, colored)


def components(mask):
    """Composantes connexes (4-voisins) d'un masque binaire : liste de listes d'indices, la plus grande d'abord."""
    w, h = mask.size
    data = bytearray(1 if p else 0 for p in mask.getdata())
    seen = bytearray(w * h)
    out = []
    for start in range(w * h):
        if not data[start] or seen[start]:
            continue
        stack, comp = [start], []
        seen[start] = 1
        while stack:
            i = stack.pop()
            comp.append(i)
            x, y = i % w, i // w
            for j in ((i - 1) if x > 0 else -1, (i + 1) if x < w - 1 else -1, (i - w) if y > 0 else -1, (i + w) if y < h - 1 else -1):
                if j >= 0 and data[j] and not seen[j]:
                    seen[j] = 1
                    stack.append(j)
        out.append(comp)
    out.sort(key=len, reverse=True)
    return out


def hull(points):
    """Enveloppe convexe (chaîne monotone)."""
    pts = sorted(set(points))
    if len(pts) < 3:
        return pts
    cross = lambda o, a, b: (a[0] - o[0]) * (b[1] - o[1]) - (a[1] - o[1]) * (b[0] - o[0])
    lower, upper = [], []
    for p in pts:
        while len(lower) >= 2 and cross(lower[-2], lower[-1], p) <= 0:
            lower.pop()
        lower.append(p)
    for p in reversed(pts):
        while len(upper) >= 2 and cross(upper[-2], upper[-1], p) <= 0:
            upper.pop()
        upper.append(p)
    return lower[:-1] + upper[:-1]


def min_rect(hull_pts):
    """Rectangle d'aire minimale contenant l'enveloppe (calibres tournants) : 4 coins, sens horaire."""
    best = None
    n = len(hull_pts)
    for i in range(n):
        (x0, y0), (x1, y1) = hull_pts[i], hull_pts[(i + 1) % n]
        a = math.atan2(y1 - y0, x1 - x0)
        c, s = math.cos(a), math.sin(a)
        us = [x * c + y * s for x, y in hull_pts]
        vs = [-x * s + y * c for x, y in hull_pts]
        area = (max(us) - min(us)) * (max(vs) - min(vs))
        if best is None or area < best[0]:
            best = (area, a, min(us), max(us), min(vs), max(vs))
    _, a, u0, u1, v0, v1 = best
    c, s = math.cos(a), math.sin(a)
    back = lambda u, v: (u * c - v * s, u * s + v * c)
    return [back(u0, v0), back(u1, v0), back(u1, v1), back(u0, v1)]


def order(corners):
    """Coins dans l'ordre haut gauche, haut droit, bas droit, bas gauche de l'image (avant toute rotation)."""
    cx = sum(p[0] for p in corners) / 4
    cy = sum(p[1] for p in corners) / 4
    by_angle = sorted(corners, key=lambda p: math.atan2(p[1] - cy, p[0] - cx))
    # le premier coin est celui en haut à gauche (somme x + y minimale)
    start = min(range(4), key=lambda i: by_angle[i][0] + by_angle[i][1])
    return by_angle[start:] + by_angle[:start]


def detect(im, bright=105, dim=45):
    """Quadrilatère de la carte dans l'image pleine résolution, ou None. Une carte sombre (photographie en noir et
    blanc) demande des seuils plus bas (« seuil_recto » : [65, 28] dans cartes.json)."""
    small = im.resize((im.width // K, im.height // K), Image.Resampling.BILINEAR)
    mask = card_mask(small, bright=bright, dim=dim).filter(ImageFilter.MaxFilter(5)).filter(ImageFilter.MinFilter(5))
    mask = mask.filter(ImageFilter.MinFilter(3)).filter(ImageFilter.MaxFilter(3))
    comps = components(mask)
    if not comps or len(comps[0]) < 0.01 * small.width * small.height:
        return None
    w = small.width
    inside = set(comps[0])
    edge = [(i % w, i // w) for i in comps[0] if (i - 1) not in inside or (i + 1) not in inside or (i - w) not in inside or (i + w) not in inside]
    rect = min_rect(hull(edge))
    return order([(x * K + K / 2, y * K + K / 2) for x, y in rect])


def straighten(im, quad, margin=4):
    """Redresse le quadrilatère en rectangle, avec une petite marge pour ne rien couper du bord."""
    (x0, y0), (x1, y1), (x2, y2), (x3, y3) = quad
    cx, cy = (x0 + x1 + x2 + x3) / 4, (y0 + y1 + y2 + y3) / 4
    grow = lambda x, y: (x + (x - cx) / math.hypot(x - cx, y - cy) * margin, y + (y - cy) / math.hypot(x - cx, y - cy) * margin)
    (x0, y0), (x1, y1), (x2, y2), (x3, y3) = (grow(*p) for p in quad)
    w = round((math.dist((x0, y0), (x1, y1)) + math.dist((x3, y3), (x2, y2))) / 2)
    h = round((math.dist((x0, y0), (x3, y3)) + math.dist((x1, y1), (x2, y2))) / 2)
    return im.transform((w, h), Image.Transform.QUAD, (x0, y0, x3, y3, x2, y2, x1, y1), Image.Resampling.BICUBIC)


def silhouette(card, corner=0.09, band=4):
    """Alpha de la carte redressée : le fond relié au bord devient transparent. Il n'est cherché que dans les coins
    (carrés de [corner] × le petit côté : coins arrondis) et sur une bande de [band] pixels le long des bords : une image
    sombre imprimée jusqu'au bord de la carte n'est pas rongée."""
    k = 2
    small = card.resize((max(1, card.width // k), max(1, card.height // k)), Image.Resampling.BILINEAR)
    background = ImageChops.invert(card_mask(small, bright=95, sat=60, dim=40))
    w, h = small.size
    r = corner * min(w, h)
    def allowed(i):
        x, y = i % w, i // w
        dx, dy = min(x, w - 1 - x), min(y, h - 1 - y)
        return min(dx, dy) <= band or (dx <= r and dy <= r)
    bg = bytearray(1 if p and allowed(i) else 0 for i, p in enumerate(background.getdata()))
    out = bytearray(w * h)
    stack = [i for i in range(w * h) if (i % w in (0, w - 1) or i // w in (0, h - 1)) and bg[i]]
    for i in stack:
        out[i] = 1
    while stack:
        i = stack.pop()
        x, y = i % w, i // w
        for j in ((i - 1) if x > 0 else -1, (i + 1) if x < w - 1 else -1, (i - w) if y > 0 else -1, (i + w) if y < h - 1 else -1):
            if j >= 0 and bg[j] and not out[j]:
                out[j] = 1
                stack.append(j)
    alpha = Image.new("L", (w, h))
    alpha.putdata([0 if o else 255 for o in out])
    # un pixel rogné : pas de liseré du drap noir ; puis un bord adouci
    alpha = alpha.filter(ImageFilter.MinFilter(3)).resize(card.size, Image.Resampling.BILINEAR).filter(ImageFilter.GaussianBlur(0.8))
    return alpha


# --------------------------------------------------------------------------- #
# Commandes
# --------------------------------------------------------------------------- #
def photos(folder):
    fs = sorted(f for f in os.listdir(folder) if f.lower().endswith((".jpg", ".jpeg", ".png")))
    if len(fs) % 2:
        print(f"Attention : {len(fs)} photos, nombre impair ; la dernière reste seule.", file=sys.stderr)
    return [(fs[i], fs[i + 1] if i + 1 < len(fs) else None) for i in range(0, len(fs), 2)]


def load(folder, name):
    return ImageOps.exif_transpose(Image.open(os.path.join(folder, name))).convert("RGB")


def cmd_detect(o):
    brut = os.path.join(o.sortie, "brut")
    os.makedirs(brut, exist_ok=True)
    report = []
    for n, (recto, verso) in enumerate(photos(o.photos), start=1):
        entry = {"n": n}
        for face, name in (("recto", recto), ("verso", verso)):
            if not name:
                continue
            im = load(o.photos, name)
            quad = detect(im)
            if quad is None:
                entry[face] = {"photo": name, "erreur": "carte introuvable"}
                print(f"{n:02d} {face} : carte introuvable ({name})", file=sys.stderr)
                continue
            card = straighten(im, quad)
            card.save(os.path.join(brut, f"{n:02d}-{face}.jpg"), quality=90)
            entry[face] = {"photo": name, "quad": [[round(x, 1), round(y, 1)] for x, y in quad], "px": [card.width, card.height]}
        report.append(entry)
        print(f"{n:02d} {entry.get('recto', {}).get('px')} {entry.get('verso', {}).get('px')}")
    with open(os.path.join(o.sortie, "releve.json"), "w", encoding="utf-8") as f:
        json.dump(report, f, ensure_ascii=False, indent=1)
    contact_sheets(brut, report, o.sortie)


def contact_sheets(brut, report, out, per=12, th=260):
    for s in range(0, len(report), per):
        chunk = report[s:s + per]
        cols = 4
        cell = (2 * th + 30, th + 26)
        sheet = Image.new("RGB", (cols * cell[0], ((len(chunk) + cols - 1) // cols) * cell[1]), "white")
        draw = ImageDraw.Draw(sheet)
        for k, e in enumerate(chunk):
            x, y = (k % cols) * cell[0], (k // cols) * cell[1]
            for j, face in enumerate(("recto", "verso")):
                p = os.path.join(brut, f"{e['n']:02d}-{face}.jpg")
                if os.path.exists(p):
                    im = Image.open(p)
                    im.thumbnail((th, th))
                    sheet.paste(im, (x + j * (th + 6), y))
            draw.text((x, y + th + 6), f"{e['n']:02d}", fill="black")
        sheet.save(os.path.join(out, f"planche-{s // per + 1}.jpg"), quality=85)


def rotate(im, quarter):
    """Rotation horaire en quarts de tour (0, 90, 180, 270)."""
    return im.rotate(-quarter, expand=True) if quarter else im


def fit(im, side):
    if max(im.size) <= side:
        return im
    k = side / max(im.size)
    return im.resize((max(1, round(im.width * k)), max(1, round(im.height * k))), Image.Resampling.LANCZOS)


def cmd_produce(o):
    if os.path.commonpath([os.path.abspath(o.sortie), ROOT]) == ROOT:
        sys.exit("Les images viennent de photos personnelles : pas dans le dépôt public.")
    with open(o.cartes, encoding="utf-8") as f:
        cards = json.load(f)
    os.makedirs(os.path.join(o.sortie, "thumbs"), exist_ok=True)
    pairs = photos(o.photos)
    catalog = []
    for c in cards["cartes"]:
        if c.get("ignorer"):
            continue
        recto_name, verso_name = pairs[c["n"] - 1]
        faces = {}
        for face, name in (("recto", recto_name), ("verso", verso_name)):
            if not name or (face == "verso" and not c.get("verso", True)):
                continue
            im = load(o.photos, name)
            quad = c.get(f"quad_{face}") or detect(im, *c.get(f"seuil_{face}", (105, 45)))
            card = rotate(straighten(im, quad, margin=0 if c.get(f"detourer_{face}", True) is False else 4), c.get(f"rotation_{face}", 0))
            # Une carte sombre jusqu'au bord se confondrait avec le drap : on garde alors son rectangle.
            card.putalpha(silhouette(card) if c.get(f"detourer_{face}", True) else Image.new("L", card.size, 255))
            faces[face] = card
        front = faces["recto"]
        full = fit(front, FULL_SIDE)
        full.save(os.path.join(o.sortie, f"{c['id']}.webp"), "WEBP", quality=86, method=6)
        thumb = full.resize((max(1, round(full.width * THUMB_HEIGHT / full.height)), THUMB_HEIGHT), Image.Resampling.LANCZOS)
        thumb.save(os.path.join(o.sortie, "thumbs", f"{c['id']}.webp"), "WEBP", quality=82, method=6)
        if "verso" in faces:
            fit(faces["verso"], FULL_SIDE).save(os.path.join(o.sortie, f"{c['id']}-verso.webp"), "WEBP", quality=86, method=6)
        # Taille réelle : toutes les photos sont à la même échelle (cards["px_par_mm"], relevée une fois).
        ppmm = cards.get("px_par_mm")
        size = [round(front.width / ppmm), round(front.height / ppmm)] if ppmm else None
        entry = {k: v for k, v in c.items() if not k.startswith(("rotation_", "quad_", "seuil_", "detourer_")) and k not in ("n", "ignorer", "verso")}
        entry.update({"personal": False, "hasBack": "verso" in faces, "photographed": True})
        if size:
            entry["sizeMm"] = size
        catalog.append(entry)
        print(c["id"], front.size, size)
    with open(os.path.join(o.sortie, "catalog.json"), "w", encoding="utf-8") as f:
        json.dump({"format": 2, "categories": cards.get("categories", []), "cards": catalog}, f, ensure_ascii=False, indent=2)
        f.write("\n")


def main():
    a = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    sub = a.add_subparsers(dest="cmd", required=True)
    d = sub.add_parser("detecter")
    d.add_argument("photos")
    d.add_argument("--sortie", required=True)
    p = sub.add_parser("produire")
    p.add_argument("photos")
    p.add_argument("--cartes", required=True)
    p.add_argument("--sortie", required=True)
    o = a.parse_args()
    {"detecter": cmd_detect, "produire": cmd_produce}[o.cmd](o)


if __name__ == "__main__":
    main()
