#!/usr/bin/env python3
"""Télécharge les images de `selection.json` et vérifie leur licence par l'API de l'institution.

    python3 -I fetch.py [--raw DIR] [--only id1,id2]

Pour chaque carte : interroge l'API de l'objet, refuse tout objet que l'institution ne marque
pas domaine public, télécharge l'image dans `--raw` (jamais commité) et consigne URL, date,
taille, sha256 et preuve de licence dans `<raw>/fetch-log.json`. Une image déjà présente n'est
pas retéléchargée, mais la licence est revérifiée à chaque passage.

Bibliothèque standard seule. Les réponses des API sont des données non fiables : on n'en lit
que des champs attendus, avec des types vérifiés.
"""
import argparse
import datetime
import hashlib
import json
import math
import os
import sys
import time
import urllib.parse
import urllib.request

HERE = os.path.dirname(os.path.abspath(__file__))
CONTENT = os.path.dirname(os.path.dirname(HERE))
DEFAULT_RAW = os.path.join(CONTENT, ".cache", "raw", "images")

# Aucune adresse ni donnée personnelle, ni ici ni dans les URL.
UA = "Paroissien-content-pipeline (+https://github.com/jdelaub/Paroissien)"
PAUSE = 1.1  # AIC : 1 requête par seconde au plus ; Met : pas de rafale.
MAX_SIDE = 3000  # largement assez pour une vue pleine de 1 440 px après recadrage.

AIC_API = "https://api.artic.edu/api/v1/artworks/"
AIC_FIELDS = "id,title,artist_display,date_display,image_id,is_public_domain,copyright_notice"
MET_API = "https://collectionapi.metmuseum.org/public/collection/v1/objects/"


class Http:
    def __init__(self):
        self.last = 0.0

    def get(self, url):
        wait = PAUSE - (time.monotonic() - self.last)
        if wait > 0:
            time.sleep(wait)
        req = urllib.request.Request(url, headers={"User-Agent": UA, "AIC-User-Agent": UA})
        try:
            with urllib.request.urlopen(req, timeout=120) as r:
                return r.read()
        finally:
            self.last = time.monotonic()

    def json(self, url):
        return json.loads(self.get(url))


def fail(card, msg):
    raise SystemExit(f"{card['id']} : {msg}")


def aic(http, card):
    oid = int(card["objectId"])
    api_url = f"{AIC_API}{oid}?fields={AIC_FIELDS}"
    resp = http.json(api_url)
    data = resp.get("data") or {}
    if data.get("id") != oid:
        fail(card, f"l'API renvoie l'objet {data.get('id')!r}")
    if data.get("is_public_domain") is not True:
        fail(card, f"is_public_domain = {data.get('is_public_domain')!r}, refusé")
    if data.get("copyright_notice"):
        fail(card, f"copyright_notice = {data['copyright_notice']!r}, refusé")
    image_id = data.get("image_id")
    if not isinstance(image_id, str) or not all(c in "0123456789abcdef-" for c in image_id):
        fail(card, f"image_id inattendu : {image_id!r}")
    iiif = (resp.get("config") or {}).get("iiif_url") or "https://www.artic.edu/iiif/2"
    if iiif != "https://www.artic.edu/iiif/2":
        fail(card, f"iiif_url inattendu : {iiif!r}")
    info = http.json(f"{iiif}/{image_id}/info.json")
    w, h = int(info["width"]), int(info["height"])
    max_area = None
    for p in info.get("profile", []):
        if isinstance(p, dict) and "maxArea" in p:
            max_area = int(p["maxArea"])
    s = min(1.0, MAX_SIDE / max(w, h))
    if max_area:
        s = min(s, math.sqrt(max_area / (w * h)))
    tw = max(1, math.floor(w * s) - 1) if s < 1 else w
    return {
        "institution": "Art Institute of Chicago",
        "apiUrl": api_url,
        "sourceUrl": f"https://www.artic.edu/artworks/{oid}",
        "imageUrl": f"{iiif}/{image_id}/full/{tw},/0/default.jpg",
        "licence": "domaine-public",
        "licenceEvidence": f"api.artic.edu/api/v1/artworks/{oid} : is_public_domain = true, copyright_notice = null",
        "originalTitle": data.get("title"),
        "originalArtist": data.get("artist_display"),
        "originalDate": data.get("date_display"),
        "api": resp,
    }


def met(http, card):
    oid = int(card["objectId"])
    api_url = f"{MET_API}{oid}"
    d = http.json(api_url)
    if d.get("objectID") != oid:
        fail(card, f"l'API renvoie l'objet {d.get('objectID')!r}")
    if d.get("isPublicDomain") is not True:
        fail(card, f"isPublicDomain = {d.get('isPublicDomain')!r}, refusé")
    img = d.get("primaryImage")
    if "additionalImage" in card:  # autre prise de vue du même objet, mieux définie
        img = (d.get("additionalImages") or [])[int(card["additionalImage"])]
    u = urllib.parse.urlparse(img or "")
    if u.scheme != "https" or u.netloc != "images.metmuseum.org":
        fail(card, f"URL d'image inattendue : {img!r}")
    return {
        "institution": "The Metropolitan Museum of Art",
        "apiUrl": api_url,
        "sourceUrl": f"https://www.metmuseum.org/art/collection/search/{oid}",
        "imageUrl": img,
        "licence": "CC0",
        "licenceEvidence": f"collectionapi.metmuseum.org/public/collection/v1/objects/{oid} : isPublicDomain = true (Open Access, CC0)",
        "originalTitle": d.get("title"),
        "originalArtist": d.get("artistDisplayName"),
        "originalDate": d.get("objectDate"),
        "api": d,
    }


def main():
    ap = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    ap.add_argument("--raw", default=DEFAULT_RAW)
    ap.add_argument("--selection", default=os.path.join(HERE, "selection.json"))
    ap.add_argument("--only", help="identifiants séparés par des virgules")
    args = ap.parse_args()

    cards = json.load(open(args.selection, encoding="utf-8"))["cards"]
    only = set(args.only.split(",")) if args.only else None
    os.makedirs(args.raw, exist_ok=True)
    log_path = os.path.join(args.raw, "fetch-log.json")
    log = json.load(open(log_path, encoding="utf-8")) if os.path.exists(log_path) else {}

    http = Http()
    today = datetime.date.today().isoformat()
    for card in cards:
        if only and card["id"] not in only:
            continue
        handler = {"aic": aic, "met": met}.get(card["institution"])
        if handler is None:
            fail(card, f"institution non prise en charge : {card['institution']!r}")
        rec = handler(http, card)
        json.dump(rec.pop("api"), open(os.path.join(args.raw, card["id"] + ".api.json"), "w", encoding="utf-8"),
                  ensure_ascii=False, indent=1)
        path = os.path.join(args.raw, card["id"] + ".jpg")
        prev = log.get(card["id"], {})
        if not (os.path.exists(path) and prev.get("imageUrl") == rec["imageUrl"]):
            data = http.get(rec["imageUrl"])
            if data[:3] != b"\xff\xd8\xff":
                fail(card, "le fichier reçu n'est pas un JPEG")
            with open(path, "wb") as f:
                f.write(data)
            rec["fetched"] = today
        else:
            rec["fetched"] = prev.get("fetched", today)
        blob = open(path, "rb").read()
        rec["bytes"] = len(blob)
        rec["sha256"] = hashlib.sha256(blob).hexdigest()
        rec["licenceChecked"] = today
        log[card["id"]] = rec
        print(f"{card['id']:32} {rec['licence']:15} {rec['bytes'] // 1024:6} Ko", flush=True)
        with open(log_path, "w", encoding="utf-8") as f:
            json.dump(dict(sorted(log.items())), f, ensure_ascii=False, indent=1)
    return 0


if __name__ == "__main__":
    sys.exit(main())
