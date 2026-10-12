"""
Rendu d'un cadre en PNG, par le moteur JavaScript de l'aperçu dans Chrome sans fenêtre : la boucle de travail pour
redessiner un cadre d'après sa photo.

    python3 -I tools/cadres/rendu.py matin                         # page de 360 × 760 dp, thème clair
    python3 -I tools/cadres/rendu.py matin --photo ~/…/p005-matin.jpg   # la photo à droite, même hauteur
    python3 -I tools/cadres/rendu.py matin --modules               # planche des modules, en grand
    python3 -I tools/cadres/rendu.py matin --theme SOMBRE --couleur VIOLET --taille 412x892 --echelle 2
    python3 -I tools/cadres/rendu.py litanies --verso              # page paire : le côté extérieur passe à gauche
    python3 -I tools/cadres/rendu.py asanoha                       # une garde (tools/cadres/gardes/) : le motif en pleine page

La sortie va dans $CADRES_SORTIE (par défaut <tmp>/cadres/<nom>[-modules].png) ; le chemin est affiché.
"""
import argparse
import json
import os
import shutil
import subprocess
import sys
import tempfile

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)

import generate  # noqa: E402

PAGE = """<!doctype html><html><head><meta charset="utf-8"><style>
html,body{margin:0;padding:0;background:#fff}canvas{display:block}</style></head><body>
<canvas id="c"></canvas>
<script>%(js)s</script>
<script>
const S = %(spec)s, P = %(params)s, MF = window.MisselFrame;
const c = document.getElementById('c'), ctx = c.getContext('2d');
const inks = MF.resolveInks(S, P.couleur, P.theme);
if (S.kind === 'motif') {
  c.width = P.w * P.k; c.height = P.h * P.k;
  ctx.scale(P.k, P.k);
  MF.drawPattern(ctx, S, inks, P.w, P.h);
} else if (!P.modules) {
  c.width = P.w * P.k; c.height = P.h * P.k;
  ctx.scale(P.k, P.k);
  ctx.fillStyle = inks.paper; ctx.fillRect(0, 0, P.w, P.h);
  MF.paperGrain(ctx, P.w, P.h, inks.dark);
  const g = MF.geometry(S, P.w, P.h, 22, !P.verso);
  MF.drawFrame(ctx, S, inks, g);
  // titre courant et texte factice, pour juger des proportions
  ctx.fillStyle = inks.text;
  ctx.font = '10.5px Georgia, serif';
  ctx.textAlign = 'center';
  ctx.fillText(S.title.toUpperCase() + '.', P.w / 2, S.layout.margin + 13);
  if (P.texte) {
    ctx.globalAlpha = 0.28;
    const lh = 17;
    for (let y = g.text.y + 4; y + 6 < g.text.y + g.text.h; y += lh) {
      const w = g.text.w * (0.86 + 0.14 * Math.abs(Math.sin(y * 0.37)));
      ctx.fillRect(g.text.x, y, w, 6);
    }
    ctx.globalAlpha = 1;
  }
} else {
  // planche : chaque module en grand, sur le fond de bande
  const ground = S.ground ? inks[S.ground] : inks.paper;
  const items = [];
  const keys = new Set();
  const add = (t, m, kind) => { if (!m) return; const k = JSON.stringify(m); if (keys.has(k)) return; keys.add(k); items.push([t, m, kind]); };
  add('frise haute', S.bands.top, 'band'); add('frise des côtés', S.bands.side, 'band'); add('frise du pied', S.bands.bottom, 'band');
  add('centre haut', S.centers.top, 'center'); add('centre des côtés', S.centers.side, 'center'); add('centre du pied', S.centers.bottom, 'center');
  add('coin', S.corners.top, 'box'); add('coin du pied', S.corners.bottom, 'box'); add('complément de coin', S.corners.fill, 'box');
  add('ornement de coin', S.outer.corner, 'outer'); add('ornement de coin du pied', S.outer.cornerBottom, 'outer');
  add('ornement du milieu', S.outer.mid, 'outer'); add('ornement du milieu du pied', S.outer.midBottom, 'outer');
  add('ornement du milieu des côtés', S.outer.midSide, 'outer');
  if (S.bars && S.bars.terminal) add('terminaison', S.bars.terminal, 'term');
  for (const k of ['top', 'side', 'bottom']) {
    const e = (S.ends || {})[k] || {};
    add('bout de début (' + k + ')', e.start, 'box'); add('bout de fin (' + k + ')', e.end, 'box');
  }
  const k = P.k * 2.5, pad = 16, W = 1100;
  const sizes = items.map(([t, m, kind]) => {
    if (kind === 'band') { const o = m.outset || 0, i = m.inset || 0; const n = Math.max(2, Math.floor((W - 2 * pad) / k / m.period)); return [n * m.period, 100 + o + i, n]; }
    if (kind === 'center') return [m.width, 100 + (m.outset || 0)];
    if (kind === 'box') return [m.w, m.h];
    return [140, 140];
  });
  const H = sizes.reduce((a, s) => a + s[1] * k + 34, pad);
  c.width = W; c.height = H;
  ctx.fillStyle = '#ECE8DF'; ctx.fillRect(0, 0, W, H);
  let y = pad;
  items.forEach(([t, m, kind], i) => {
    const [w, h, n] = sizes[i];
    ctx.fillStyle = '#333'; ctx.font = '14px sans-serif'; ctx.textAlign = 'left';
    ctx.fillText(t + ' (' + ((m.base || []).length + (m.over || []).length + (m.ops || []).length) + ' tracés)', pad, y + 14);
    y += 20;
    ctx.save();
    ctx.translate(pad, y); ctx.scale(k, k);
    ctx.fillStyle = inks.paper; ctx.fillRect(0, 0, w, h);
    if (kind === 'band') {
      ctx.translate(0, m.outset || 0);
      ctx.fillStyle = ground; ctx.fillRect(0, 0, w, 100);
      ctx.beginPath(); ctx.rect(0, -(m.outset || 0), w, h); ctx.clip();
      for (const pass of ['base', 'over']) for (let j = -1; j <= n + 1; j++) { ctx.save(); ctx.translate((j + m.phase) * m.period, 0); MF.drawOps(ctx, m[pass], inks); ctx.restore(); }
    } else if (kind === 'center') {
      ctx.fillStyle = ground; ctx.fillRect(0, m.outset || 0, w, 100);
      ctx.translate(w / 2, m.outset || 0); MF.drawOps(ctx, m.ops, inks);
    } else if (kind === 'box') {
      ctx.fillStyle = ground; ctx.fillRect(0, 0, w, h); MF.drawOps(ctx, m.ops, inks);
    } else {
      ctx.translate(kind === 'term' ? 10 : 105, kind === 'term' ? 70 : 105); MF.drawOps(ctx, m.ops, inks);
    }
    ctx.restore();
    y += h * k + 14;
  });
}
</script></body></html>"""


def chrome():
    for exe in ("google-chrome", "chromium", "chromium-browser"):
        p = shutil.which(exe)
        if p:
            return p
    sys.exit("Chrome introuvable : il faut google-chrome ou chromium.")


def render(name, w=360, h=760, k=2, theme="CLAIR", couleur="", modules=False, texte=True, photo=None, out=None,
           verso=False, photo_rotation=0):
    spec = generate.load(name)
    js = open(os.path.join(HERE, "apercu", "frame.js"), encoding="utf-8").read()
    params = {"w": w, "h": h, "k": k, "theme": theme, "couleur": couleur, "modules": modules and spec.get("kind") != "motif",
              "texte": texte, "verso": verso}
    html = PAGE % {"js": js, "spec": json.dumps(spec, ensure_ascii=False), "params": json.dumps(params)}
    outdir = os.environ.get("CADRES_SORTIE") or os.path.join(tempfile.gettempdir(), "cadres")
    os.makedirs(outdir, exist_ok=True)
    out = out or os.path.join(outdir, f"{name}{'-modules' if modules else ''}{'-verso' if verso else ''}.png")
    work = tempfile.mkdtemp(prefix="cadre-")
    try:
        page = os.path.join(work, "page.html")
        with open(page, "w", encoding="utf-8") as f:
            f.write(html)
        if params["modules"]:
            size = (1100, 8000)
        else:
            size = (round(w * k), round(h * k))
        subprocess.run([chrome(), "--headless=new", "--disable-gpu", "--no-sandbox", "--hide-scrollbars",
                        f"--user-data-dir={os.path.join(work, 'profil')}", f"--window-size={size[0]},{size[1]}",
                        f"--screenshot={out}", "file://" + page],
                       check=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, timeout=120)
    finally:
        shutil.rmtree(work, ignore_errors=True)
    modules = params["modules"]
    if modules or photo:
        from PIL import Image, ImageOps
        im = Image.open(out).convert("RGB")
        if modules:
            # recadre la planche sur son contenu (le fond de page Chrome est blanc)
            px = im.load()
            bottom = im.height
            while bottom > 1 and px[5, bottom - 1] == (255, 255, 255):
                bottom -= 1
            im = im.crop((0, 0, im.width, bottom))
        if photo:
            ph = ImageOps.exif_transpose(Image.open(os.path.expanduser(photo))).convert("RGB")
            if photo_rotation:
                ph = ph.rotate(-photo_rotation, expand=True)  # sens horaire, comme decoupe.py
            ph = ph.resize((round(ph.width * im.height / ph.height), im.height))
            both = Image.new("RGB", (im.width + ph.width + 12, im.height), "white")
            both.paste(im, (0, 0))
            both.paste(ph, (im.width + 12, 0))
            im = both
        im.save(out)
    return out


def main():
    a = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    a.add_argument("nom")
    a.add_argument("--taille", default="360x760", help="largeur x hauteur en dp")
    a.add_argument("--echelle", type=float, default=2, help="pixels par dp")
    a.add_argument("--theme", default="CLAIR", choices=["CLAIR", "SEPIA", "SOMBRE", "NUIT"])
    a.add_argument("--couleur", default="", help="couleur liturgique (GREEN, VIOLET…), vide pour celles du livre")
    a.add_argument("--modules", action="store_true", help="planche des modules au lieu de la page")
    a.add_argument("--sans-texte", action="store_true", help="pas de texte factice")
    a.add_argument("--photo", help="photo à placer à droite, à la même hauteur")
    a.add_argument("--verso", action="store_true", help="page paire : le côté extérieur à gauche")
    a.add_argument("--rotation-photo", type=int, default=0, choices=[0, 90, 180, 270], help="sens horaire, après l'EXIF")
    a.add_argument("--sortie", help="fichier PNG de sortie")
    o = a.parse_args()
    w, h = (int(v) for v in o.taille.lower().split("x"))
    print(render(o.nom, w, h, o.echelle, o.theme, o.couleur, o.modules, not o.sans_texte, o.photo, o.sortie, o.verso, o.rotation_photo))


if __name__ == "__main__":
    main()
