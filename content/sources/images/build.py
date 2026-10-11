#!/usr/bin/env python3
"""Recadre et encode les images pieuses de `selection.json`, puis écrit `catalog.json`.

    python3 -I build.py [--raw DIR] [--out DIR] [--sheet FICHIER]

Lit les fichiers bruts et `fetch-log.json` produits par `fetch.py`. Pour chaque carte, découpe
au rapport 0,58 le plus grand rectangle possible dans `bounds` (zone utile de l'image source,
hors cadre, mire ou marge ; toute l'image par défaut), le réduit du facteur `scale` (1 par
défaut), le centre sur `focal` puis le ramène dans `bounds`. Encode ensuite la vue pleine
(côté long 1 440 px) et la vignette (360 px) en WebP q75. Les coordonnées de `focal` et `bounds`
sont des fractions de la largeur et de la hauteur de l'image source.

Demande Pillow avec WebP. Les images sont converties en sRGB et ne gardent aucune métadonnée.
"""
import argparse
import hashlib
import io
import json
import os
import re
import sys

from PIL import Image, ImageCms, ImageDraw, ImageFont, features

HERE = os.path.dirname(os.path.abspath(__file__))
CONTENT = os.path.dirname(os.path.dirname(HERE))
DEFAULT_RAW = os.path.join(CONTENT, ".cache", "raw", "images")
DEFAULT_OUT = os.path.join(CONTENT, "data", "images")

RATIO = 0.58
FULL_H, THUMB_H = 1440, 360
FULL = (round(FULL_H * RATIO), FULL_H)
THUMB = (round(THUMB_H * RATIO), THUMB_H)
QUALITY = 75
# Budget d'une vue pleine (rapport 09, §3.2.3 : 150 à 200 Ko ; 170 pour tenir l'APK public sous 30 Mo). Les gravures au trait se compressent mal : on baisse
# la qualité par paliers de 5, jusqu'à 50 au plus bas, plutôt que de dépasser.
MAX_FULL_BYTES = 170 * 1024
MIN_QUALITY = 50
MIN_FULL_H = 1080
SEASONS = {"ADVENT", "CHRISTMAS", "LENT", "PASSIONTIDE", "EASTER", "PENTECOST", "ORDINARY"}
LICENCE_LABEL = {"CC0": "CC0", "domaine-public": "domaine public"}
SLUG = re.compile(r"[a-z0-9_]+")
SRGB = ImageCms.ImageCmsProfile(ImageCms.createProfile("sRGB"))


def load_srgb(path):
    im = Image.open(path)
    im.load()
    icc = im.info.get("icc_profile")
    if icc:
        src = ImageCms.ImageCmsProfile(io.BytesIO(icc))
        if "srgb" not in ImageCms.getProfileDescription(src).lower():
            im = ImageCms.profileToProfile(im, src, SRGB, outputMode="RGB")
    return im.convert("RGB")


def crop_box(card, w, h):
    x0, y0, x1, y1 = card.get("bounds", [0, 0, 1, 1])
    x0, x1, y0, y1 = x0 * w, x1 * w, y0 * h, y1 * h
    bw, bh = x1 - x0, y1 - y0
    if bw / bh > RATIO:
        ch = bh
        cw = ch * RATIO
    else:
        cw = bw
        ch = cw / RATIO
    s = card.get("scale", 1.0)
    cw, ch = cw * s, ch * s
    fx, fy = card["focal"]
    x = min(max(fx * w - cw / 2, x0), x1 - cw)
    y = min(max(fy * h - ch / 2, y0), y1 - ch)
    return round(x), round(y), round(cw), round(ch)


def encode(im, box, size, budget=None):
    """WebP de la taille demandée ; au-delà du budget, qualité réduite, puis définition (jamais sous 1 080 px)."""
    w, h = size
    while True:
        out = im.resize((w, h), Image.LANCZOS, box=box, reducing_gap=3.0)
        quality = QUALITY
        while True:
            buf = io.BytesIO()
            out.save(buf, "WEBP", quality=quality, method=6)
            if budget is None or len(buf.getvalue()) <= budget or quality <= MIN_QUALITY:
                break
            quality -= 5
        if budget is None or len(buf.getvalue()) <= budget or h <= MIN_FULL_H:
            return buf.getvalue(), (w, h)
        h = max(MIN_FULL_H, round(h * 0.9))
        w = round(h * RATIO)


def check(card):
    cid = card["id"]
    assert SLUG.fullmatch(cid), cid
    assert set(card["seasons"]) <= SEASONS, (cid, card["seasons"])
    assert all(SLUG.fullmatch(f) for f in card["feasts"]), (cid, card["feasts"])
    assert all(SLUG.fullmatch(t) for t in card["themes"]), (cid, card["themes"])
    assert all(0 <= v <= 1 for v in card["focal"] + card.get("bounds", [])), cid
    assert 0 < card.get("scale", 1.0) <= 1, cid


def contact_sheet(cards, out_dir, path):
    cols = 6
    tw, th = THUMB
    pad, label = 10, 18
    rows = (len(cards) + cols - 1) // cols
    sheet = Image.new("RGB", (cols * (tw + pad) + pad, rows * (th + label + pad) + pad), (246, 241, 230))
    draw = ImageDraw.Draw(sheet)
    try:
        font = ImageFont.truetype("DejaVuSans.ttf", 11)
    except OSError:
        font = ImageFont.load_default(11)
    for k, c in enumerate(cards):
        x = pad + (k % cols) * (tw + pad)
        y = pad + (k // cols) * (th + label + pad)
        sheet.paste(Image.open(os.path.join(out_dir, "thumbs", c["id"] + ".webp")).convert("RGB"), (x, y))
        text = c["id"]
        while draw.textlength(text, font=font) > tw and len(text) > 4:
            text = text[:-2] + "…"
        draw.text((x, y + th + 3), text, fill=(60, 40, 20), font=font)
    sheet.save(path, "WEBP", quality=80, method=6)


def main():
    ap = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    ap.add_argument("--raw", default=DEFAULT_RAW)
    ap.add_argument("--out", default=DEFAULT_OUT)
    ap.add_argument("--selection", default=os.path.join(HERE, "selection.json"))
    ap.add_argument("--sheet", default=os.path.join(HERE, "contact-sheet.webp"))
    args = ap.parse_args()
    if not features.check("webp"):
        raise SystemExit("Pillow sans WebP")

    sel = json.load(open(args.selection, encoding="utf-8"))
    assert sel["ratio"] == RATIO
    cards = sorted(sel["cards"], key=lambda c: c["id"])
    ids = [c["id"] for c in cards]
    assert len(set(ids)) == len(ids), "identifiants en double"
    log = json.load(open(os.path.join(args.raw, "fetch-log.json"), encoding="utf-8"))
    os.makedirs(os.path.join(args.out, "thumbs"), exist_ok=True)

    catalog, total = [], 0
    for card in cards:
        check(card)
        cid = card["id"]
        rec = log[cid]
        raw = os.path.join(args.raw, cid + ".jpg")
        with open(raw, "rb") as f:
            if hashlib.sha256(f.read()).hexdigest() != rec["sha256"]:
                raise SystemExit(f"{cid} : le fichier brut ne correspond pas à fetch-log.json")
        im = load_srgb(raw)
        w, h = im.size
        x, y, cw, ch = crop_box(card, w, h)
        box = (x, y, x + cw, y + ch)
        if ch < FULL_H:
            print(f"  attention : {cid} agrandi ×{FULL_H / ch:.2f} (rectangle source de {ch} px de haut)")
        (full, full_size), (thumb, _) = encode(im, box, FULL, MAX_FULL_BYTES), encode(im, box, THUMB)
        blobs = {"full": full, "thumb": thumb}
        paths = {"full": os.path.join(args.out, cid + ".webp"),
                 "thumb": os.path.join(args.out, "thumbs", cid + ".webp")}
        for k in blobs:
            with open(paths[k], "wb") as f:
                f.write(blobs[k])
        total += sum(len(b) for b in blobs.values())
        who = ", ".join(p for p in (card["artist"], card["work"], card["date"]) if p)
        catalog.append({
            "id": cid,
            "title": {"fr": card["title"]},
            "work": card["work"],
            "originalTitle": rec["originalTitle"],
            "artist": card["artist"],
            "date": card["date"],
            "institution": rec["institution"],
            "objectId": str(card["objectId"]),
            "sourceUrl": rec["sourceUrl"],
            "imageUrl": rec["imageUrl"],
            "licence": rec["licence"],
            "licenceEvidence": rec["licenceEvidence"],
            "licenceChecked": rec["licenceChecked"],
            "credit": f"{who} · {rec['institution']} · {LICENCE_LABEL[rec['licence']]}",
            "focal": {"x": card["focal"][0], "y": card["focal"][1]},
            "crop": {"x": x, "y": y, "w": cw, "h": ch, "sourceWidth": w, "sourceHeight": h},
            "width": full_size[0],
            "height": full_size[1],
            "thumbWidth": THUMB[0],
            "thumbHeight": THUMB[1],
            "bytes": {k: len(b) for k, b in blobs.items()},
            "sha256": {k: hashlib.sha256(b).hexdigest() for k, b in blobs.items()},
            "themes": card["themes"],
            "seasons": card["seasons"],
            "feasts": card["feasts"],
            "prayer": card["prayer"],
        })
        print(f"{cid:32} {len(blobs['full']) // 1024:4} Ko + {len(blobs['thumb']) // 1024:3} Ko  crop {cw}×{ch}")

    # Retire les fichiers d'une carte sortie de la sélection.
    for sub in ("", "thumbs"):
        d = os.path.join(args.out, sub)
        for name in os.listdir(d):
            if name.endswith(".webp") and name[:-5] not in ids:
                os.remove(os.path.join(d, name))

    with open(os.path.join(args.out, "catalog.json"), "w", encoding="utf-8") as f:
        json.dump({"format": 1, "ratio": RATIO, "cards": catalog}, f, ensure_ascii=False, indent=1)
        f.write("\n")
    print(f"{len(catalog)} cartes, {total / 1024 / 1024:.2f} Mo d'images")
    if args.sheet:
        contact_sheet(catalog, args.out, args.sheet)
    return 0


if __name__ == "__main__":
    sys.exit(main())
