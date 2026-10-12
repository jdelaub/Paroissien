// Moteur de cadre : même algorithme que OrnamentalFrame.kt (core:designsystem, paquet frame).
// Entrée : une spécification produite par tools/cadres/generate.py (voir commun.cadre).
(function () {
  const cache = new Map();
  const path = d => {
    let p = cache.get(d);
    if (!p) { p = new Path2D(d); cache.set(d, p); }
    return p;
  };

  const SEAM = 0.5; // recouvrement des découpes voisines (dp)
  const hex = c => [1, 3, 5].map(i => parseInt(c.slice(i, i + 2), 16));
  const mix = (a, b, t) => '#' + hex(a).map((v, i) => Math.round(v + (hex(b)[i] - v) * t).toString(16).padStart(2, '0')).join('');

  // Même règle que FrameStyle.inks : palette liturgique, puis atténuation vers le fond en thème sombre.
  function resolveInks(spec, liturgical, themeName) {
    const theme = spec.paperThemes[themeName];
    const inks = Object.assign({}, spec.inks, (liturgical && spec.palettes[liturgical]) || {});
    if (theme.ornamentDim > 0) for (const k of spec.dimmedInks) inks[k] = mix(inks[k], theme.background, theme.ornamentDim);
    return Object.assign(inks, { paper: theme.background, text: theme.ink, rubric: theme.rubric, dark: theme.dark });
  }

  function drawOps(ctx, ops, inks) {
    ctx.miterLimit = 4; // comme Compose (le canvas vaut 10 par défaut)
    for (const o of ops) {
      const p = path(o.d);
      if (o.stroke != null) {
        ctx.lineWidth = o.stroke;
        ctx.lineCap = o.round ? 'round' : 'butt';
        ctx.lineJoin = o.round ? 'round' : 'miter';
        ctx.strokeStyle = inks[o.ink];
        ctx.stroke(p);
      } else {
        ctx.fillStyle = inks[o.ink];
        ctx.fill(p);
      }
    }
  }

  // Géométrie du cadre dans un écran W × H (dp). `header` réserve la place du titre courant.
  function geometry(spec, W, H, header) {
    const L = spec.layout;
    const ox = L.margin, oy = L.margin + (header || 0);
    const ow = W - 2 * L.margin, oh = H - oy - L.margin;
    const t = L.top, s = L.side, b = L.bottom;
    const u = Math.min(t, s) / 100;
    const barHalf = spec.bars ? spec.bars.layers[0][1] / 2 * u : 0;
    const pad = L.textPadding;
    return {
      ox, oy, ow, oh, t, s, b, u,
      text: {
        x: ox + s + barHalf + pad, y: oy + t + barHalf + pad,
        w: ow - 2 * (s + barHalf + pad), h: oh - t - b - 2 * (barHalf + pad),
      },
    };
  }

  function clipRect(ctx, x0, y0, x1, y1) {
    ctx.beginPath();
    ctx.rect(Math.min(x0, x1), Math.min(y0, y1), Math.abs(x1 - x0), Math.abs(y1 - y0));
    ctx.clip();
  }

  // Une portion de bande, de u = from à u = from + len : n éléments, n = arrondi(len / pas nominal), puis le pas
  // est ajusté pour tomber juste (≈ border-image: round). `back` la parcourt depuis sa fin, en miroir.
  function drawRun(ctx, mod, inks, band, from, len, back) {
    if (len <= 0) return 0;
    const k = band.depth / 100;
    const n = Math.max(1, Math.round(len / (mod.period * k)));
    const step = len / n;
    const ka = step / mod.period;
    const sg = back ? -1 : 1;
    const start = back ? from + len : from;
    const X = band.x0 + band.ax * start, Y = band.y0 + band.ay * start;
    ctx.save();
    const o = mod.outset * k, i = mod.inset * k;
    // un demi-pixel de recouvrement avec la pièce voisine : deux découpes lissées bord à bord laissent un liseré
    const e = SEAM;
    clipRect(ctx,
      band.x0 + band.ax * (from - e) - band.dx * o, band.y0 + band.ay * (from - e) - band.dy * o,
      band.x0 + band.ax * (from + len + e) + band.dx * (band.depth + i), band.y0 + band.ay * (from + len + e) + band.dy * (band.depth + i));
    for (const pass of ['base', 'over']) {
      for (let j = -1; j <= n + 1; j++) {
        const u0 = (j + mod.phase) * step;
        ctx.save();
        ctx.transform(sg * band.ax * ka, sg * band.ay * ka, band.dx * k, band.dy * k, X + sg * band.ax * u0, Y + sg * band.ay * u0);
        drawOps(ctx, mod[pass], inks);
        ctx.restore();
      }
    }
    ctx.restore();
    return n;
  }

  // Une bande entière ; avec une pièce centrale, deux demi-bandes symétriques de part et d'autre.
  function drawBand(ctx, mod, center, inks, band) {
    const k = band.depth / 100;
    if (!center) return drawRun(ctx, mod, inks, band, 0, band.len, false);
    const cw = center.width * k;
    const half = (band.len - cw) / 2;
    const n = drawRun(ctx, mod, inks, band, 0, half, false);
    drawRun(ctx, mod, inks, band, band.len - half, half, true);
    const mid = band.len / 2;
    const o = center.outset * k;
    ctx.save();
    clipRect(ctx,
      band.x0 + band.ax * (mid - cw / 2 - SEAM) - band.dx * o, band.y0 + band.ay * (mid - cw / 2 - SEAM) - band.dy * o,
      band.x0 + band.ax * (mid + cw / 2 + SEAM) + band.dx * band.depth, band.y0 + band.ay * (mid + cw / 2 + SEAM) + band.dy * band.depth);
    ctx.transform(band.ax * k, band.ay * k, band.dx * k, band.dy * k, band.x0 + band.ax * mid, band.y0 + band.ay * mid);
    drawOps(ctx, center.ops, inks);
    ctx.restore();
    return 2 * n;
  }

  // Coin : le module, dessiné pour le coin haut gauche, est posé contre l'angle intérieur de la case et mis à
  // l'échelle pour tenir dans sa largeur ; les autres coins sont des miroirs. Si la case est plus haute (pied),
  // `fill` comble le reste, pointe vers l'extérieur.
  function drawCorner(ctx, spec, mod, inks, cx, cy, w, h, fx, fy) {
    if (!mod) return;
    const sc = Math.min(w / mod.w, h / mod.h);
    const o = (mod.outset || 0) * sc;
    // angle intérieur de la case
    const ix = fx > 0 ? cx + w : cx, iy = fy > 0 ? cy + h : cy;
    ctx.save();
    clipRect(ctx, fx > 0 ? cx - o : cx + w + o, fy > 0 ? cy - o : cy + h + o, ix, iy);
    ctx.save();
    ctx.transform(fx * sc, 0, 0, fy * sc, ix - fx * mod.w * sc, iy - fy * mod.h * sc);
    drawOps(ctx, mod.ops, inks);
    ctx.restore();
    const rest = h - mod.h * sc;
    const fill = spec.corners.fill;
    if (fill && rest > 0.25 * w) {
      const f = Math.min(w, rest) / 100;
      const x = cx + w / 2 - 50 * f;
      const outerY = fy > 0 ? cy : cy + h;
      ctx.save();
      ctx.transform(f, 0, 0, fy * f, x, outerY);
      drawOps(ctx, fill.ops, inks);
      ctx.restore();
    }
    ctx.restore();
  }

  function drawFrame(ctx, spec, inks, g) {
    const { ox, oy, ow, oh, t, s, b, u } = g;
    const B = spec.bands, C = spec.centers, K = spec.corners;

    // 1. fond des bandes (anneau entre rectangle extérieur et intérieur)
    if (spec.ground) {
      ctx.fillStyle = inks[spec.ground];
      ctx.beginPath();
      ctx.rect(ox, oy, ow, oh);
      ctx.rect(ox + s, oy + t, ow - 2 * s, oh - t - b);
      ctx.fill('evenodd');
    }

    const rules = over => {
      for (const r of spec.rules) {
        if (!!r.over !== over) continue;
        const f = r.at / 100;
        ctx.lineWidth = r.width * u;
        ctx.lineJoin = 'miter';
        ctx.strokeStyle = inks[r.ink];
        ctx.strokeRect(ox + f * s, oy + f * t, ow - 2 * f * s, oh - f * t - f * b);
      }
    };
    rules(false);

    // 2. frises : haut, gauche, droite, pied
    const vLen = oh - t - b, hLen = ow - 2 * s;
    const counts = {
      top: drawBand(ctx, B.top, C.top, inks, { x0: ox + s, y0: oy, ax: 1, ay: 0, dx: 0, dy: 1, len: hLen, depth: t }),
      left: drawBand(ctx, B.side, C.side, inks, { x0: ox, y0: oy + t, ax: 0, ay: 1, dx: 1, dy: 0, len: vLen, depth: s }),
      right: drawBand(ctx, B.side, C.side, inks, { x0: ox + ow, y0: oy + t, ax: 0, ay: 1, dx: -1, dy: 0, len: vLen, depth: s }),
      bottom: drawBand(ctx, B.bottom, C.bottom, inks, { x0: ox + s, y0: oy + oh, ax: 1, ay: 0, dx: 0, dy: -1, len: hLen, depth: b }),
    };

    // 3. coins
    drawCorner(ctx, spec, K.top, inks, ox, oy, s, t, 1, 1);
    drawCorner(ctx, spec, K.top, inks, ox + ow - s, oy, s, t, -1, 1);
    drawCorner(ctx, spec, K.bottom, inks, ox, oy + oh - b, s, b, 1, -1);
    drawCorner(ctx, spec, K.bottom, inks, ox + ow - s, oy + oh - b, s, b, -1, -1);

    rules(true);

    // 4. baguettes : rectangle extérieur + baguettes intérieures croisées qui débordent, puis terminaisons
    const bars = spec.bars;
    if (bars) {
      const ext = bars.overhang * u;
      const y1 = oy + t, y2 = oy + oh - b, x1 = ox + s, x2 = ox + ow - s;
      const p = new Path2D();
      p.rect(ox, oy, ow, oh);
      for (const y of [y1, y2]) { p.moveTo(ox - ext, y); p.lineTo(ox + ow + ext, y); }
      for (const x of [x1, x2]) { p.moveTo(x, oy - ext); p.lineTo(x, oy + oh + ext); }
      ctx.lineJoin = 'miter';
      ctx.lineCap = 'butt';
      for (const [ink, w] of bars.layers) {
        ctx.lineWidth = w * u;
        ctx.strokeStyle = inks[ink];
        ctx.stroke(p);
      }
      if (bars.terminal) {
        const term = (x, y, deg, push) => {
          const a = deg * Math.PI / 180, c = Math.cos(a), sn = Math.sin(a);
          ctx.save();
          ctx.transform(c * u, sn * u, -sn * u, c * u, x + c * push * u, y + sn * push * u);
          drawOps(ctx, bars.terminal.ops, inks);
          ctx.restore();
        };
        for (const y of [y1, y2]) { term(ox - ext, y, 180, 0); term(ox + ow + ext, y, 0, 0); }
        for (const x of [x1, x2]) { term(x, oy - ext, 270, 0); term(x, oy + oh + ext, 90, 0); }
        if (bars.cornerTerminals) {
          term(ox, oy, 225, 4); term(ox + ow, oy, 315, 4);
          term(ox + ow, oy + oh, 45, 4); term(ox, oy + oh, 135, 4);
        }
      }
    }

    // 5. ornements extérieurs : aux quatre coins (miroirs), au milieu de chaque côté
    const O = spec.outer;
    const place = (mod, a, bb, c, d, e, f) => {
      if (!mod) return;
      ctx.save();
      ctx.transform(a * u, bb * u, c * u, d * u, e, f);
      drawOps(ctx, mod.ops, inks);
      ctx.restore();
    };
    place(O.corner, 1, 0, 0, 1, ox, oy);
    place(O.corner, -1, 0, 0, 1, ox + ow, oy);
    place(O.cornerBottom, 1, 0, 0, -1, ox, oy + oh);
    place(O.cornerBottom, -1, 0, 0, -1, ox + ow, oy + oh);
    place(O.mid, 1, 0, 0, 1, ox + ow / 2, oy);
    place(O.midBottom, 1, 0, 0, -1, ox + ow / 2, oy + oh);
    // au milieu de la bande latérale (et de sa pièce centrale), pas du cadre : le pied est souvent plus haut
    const my = oy + t + (oh - t - b) / 2;
    place(O.midSide, 0, -1, 1, 0, ox, my);
    place(O.midSide, 0, 1, -1, 0, ox + ow, my);
    return counts;
  }

  // Grain de papier discret, déterministe (même rendu à chaque fois)
  function paperGrain(ctx, W, H, dark) {
    let seed = 1234567;
    const rnd = () => ((seed = (seed * 1103515245 + 12345) & 0x7fffffff) / 0x7fffffff);
    const n = Math.round(W * H / 18);
    ctx.save();
    for (let i = 0; i < n; i++) {
      const v = rnd();
      ctx.fillStyle = (v > 0.5) === !dark ? 'rgba(90,70,40,0.05)' : 'rgba(255,250,240,0.035)';
      ctx.fillRect(rnd() * W, rnd() * H, 1 + rnd(), 1 + rnd());
    }
    ctx.restore();
  }

  window.MisselFrame = { resolveInks, geometry, drawFrame, drawOps, paperGrain };
})();
