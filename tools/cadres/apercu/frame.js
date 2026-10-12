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
    const inks = Object.assign({}, spec.inks, (liturgical && (spec.palettes || {})[liturgical]) || {});
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

  // Géométrie du cadre dans un écran W × H (dp). `header` réserve la place du titre courant. `outerRight` : le côté
  // extérieur de la page est à droite (recto, par défaut) ou à gauche (verso) ; il porte la bande « outer ».
  function geometry(spec, W, H, header, outerRight) {
    const L = spec.layout;
    const P = spec.presence || { top: true, outer: true, bottom: true, inner: true };
    const right = outerRight !== false;
    const has = { top: P.top, bottom: P.bottom, left: right ? P.inner : P.outer, right: right ? P.outer : P.inner };
    const ox = L.margin, oy = L.margin + (header || 0);
    const ow = W - 2 * L.margin, oh = H - oy - L.margin;
    const t = L.top, s = L.side, b = L.bottom;
    const u = Math.min(t, s) / 100;
    const barHalf = spec.bars ? spec.bars.layers[0][1] / 2 * u : 0;
    const pad = L.textPadding;
    const eL = has.left ? s + barHalf : 0, eR = has.right ? s + barHalf : 0;
    const eT = has.top ? t + barHalf : 0, eB = has.bottom ? b + barHalf : 0;
    return {
      ox, oy, ow, oh, t, s, b, u, has,
      text: { x: ox + eL + pad, y: oy + eT + pad, w: ow - eL - eR - 2 * pad, h: oh - eT - eB - 2 * pad },
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
    // un demi-pixel de recouvrement avec la pièce voisine : deux découpes lissées bord à bord laissent un liseré ;
    // une frise qui « déborde » (spill) mord sur ses voisines, et ne dessine alors que ses propres éléments
    const spill = (mod.spill || 0) * k;
    const e = SEAM + spill;
    clipRect(ctx,
      band.x0 + band.ax * (from - e) - band.dx * o, band.y0 + band.ay * (from - e) - band.dy * o,
      band.x0 + band.ax * (from + len + e) + band.dx * (band.depth + i), band.y0 + band.ay * (from + len + e) + band.dy * (band.depth + i));
    for (const pass of ['base', 'over']) {
      for (let j = spill ? 0 : -1; j <= (spill ? n - 1 : n + 1); j++) {
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

  // Pièce posée sur la bande entre u0 et u0 + w (largeur à l'écran), au milieu ou à un bout. `flip` la retourne le
  // long de la bande (pièce de fin). `upright` : la pièce garde sur toutes les bandes l'aspect qu'elle a en haut (bandes
  // haute et du pied) ou à droite (bandes latérales) : sur la bande gauche ou du pied, elle est déplacée, jamais
  // retournée ni renversée ; son v = 0 passe alors du côté du texte (figure debout, inscription).
  function drawPiece(ctx, ops, inks, band, uAt, w, outset, inset, flip, upright) {
    const k = band.depth / 100;
    let x0 = band.x0, y0 = band.y0, dx = band.dx, dy = band.dy;
    if (upright && band.ax * band.dy - band.ay * band.dx < 0) {
      x0 += dx * band.depth; y0 += dy * band.depth; dx = -dx; dy = -dy;
      const t = outset; outset = inset; inset = t;
    }
    let ax = band.ax, ay = band.ay;
    if (flip) { ax = -ax; ay = -ay; }
    const lo = Math.min(uAt.from, uAt.to), hi = Math.max(uAt.from, uAt.to);
    ctx.save();
    clipRect(ctx,
      x0 + band.ax * (lo - SEAM) - dx * outset * k, y0 + band.ay * (lo - SEAM) - dy * outset * k,
      x0 + band.ax * (hi + SEAM) + dx * (band.depth + inset * k), y0 + band.ay * (hi + SEAM) + dy * (band.depth + inset * k));
    ctx.transform(ax * k, ay * k, dx * k, dy * k, x0 + band.ax * uAt.origin, y0 + band.ay * uAt.origin);
    drawOps(ctx, ops, inks);
    ctx.restore();
  }

  // Une bande entière : pièces de bout, frise, et pièce centrale entre deux demi-frises symétriques. Les pièces
  // « dessus » sont renvoyées pour être dessinées après les filets et les baguettes.
  function drawBand(ctx, mod, center, ends, inks, band, later) {
    const k = band.depth / 100;
    const st = ends && ends.start, en = ends && ends.end;
    const a = st ? st.w * k : 0, z = en ? en.w * k : 0;
    if (st) {
      const f = () => drawPiece(ctx, st.ops, inks, band, { from: 0, to: a, origin: 0 }, a, st.outset || 0, st.inset || 0, false, false);
      st.above ? later.push(f) : f();
    }
    if (en) {
      const f = () => drawPiece(ctx, en.ops, inks, band, { from: band.len - z, to: band.len, origin: band.len }, z, en.outset || 0, en.inset || 0, true, false);
      en.above ? later.push(f) : f();
    }
    if (!center) return drawRun(ctx, mod, inks, band, a, band.len - a - z, false);
    const cw = center.width * k;
    // au milieu, ou à la fraction `at` de la bande ; la frise repart en miroir après la pièce, ou dans le même sens
    const mid = band.len * (center.at == null ? 0.5 : center.at);
    const n = drawRun(ctx, mod, inks, band, a, mid - cw / 2 - a, false);
    const m = drawRun(ctx, mod, inks, band, mid + cw / 2, band.len - z - mid - cw / 2, mod.mirror !== false);
    const f = () => drawPiece(ctx, center.ops, inks, band, { from: mid - cw / 2, to: mid + cw / 2, origin: mid }, cw,
      center.outset || 0, center.inset || 0, false, !!center.upright);
    center.above ? later.push(f) : f();
    return n + m;
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

  // Filets : le long des bandes présentes seulement ; un filet ouvert court jusqu'au bord du cadre.
  function drawRules(ctx, spec, inks, g, over) {
    const { ox, oy, ow, oh, t, s, b, u, has } = g;
    const full = has.top && has.left && has.right && has.bottom;
    for (const r of spec.rules) {
      if (!!r.over !== over) continue;
      const off = (present, depth) => {
        if (!present) return 0;
        if (r.from === 'out') return r.at * u;
        if (r.from === 'in') return depth - r.at * u;
        return r.at / 100 * depth;
      };
      const T = off(has.top, t), Lf = off(has.left, s), R = off(has.right, s), B = off(has.bottom, b);
      ctx.lineWidth = r.width * u;
      ctx.lineJoin = 'miter';
      ctx.lineCap = 'butt';
      ctx.strokeStyle = inks[r.ink];
      if (full) { ctx.strokeRect(ox + Lf, oy + T, ow - Lf - R, oh - T - B); continue; }
      // côtés dans l'ordre haut, droite, pied, gauche ; chaque suite de côtés présents fait une ligne brisée
      const pts = [[ox + Lf, oy + T], [ox + ow - R, oy + T], [ox + ow - R, oy + oh - B], [ox + Lf, oy + oh - B]];
      const sides = [has.top, has.right, has.bottom, has.left];
      const first = sides.indexOf(false);
      ctx.beginPath();
      let open = false;
      for (let i = 1; i <= 4; i++) {
        const k = (first + i) % 4;
        if (!sides[k]) { open = false; continue; }
        if (!open) { ctx.moveTo(...pts[k]); open = true; }
        ctx.lineTo(...pts[(k + 1) % 4]);
      }
      ctx.stroke();
    }
  }

  function drawFrame(ctx, spec, inks, g) {
    const { ox, oy, ow, oh, t, s, b, u, has } = g;
    const B = spec.bands, C = spec.centers, K = spec.corners;
    const E = spec.ends || {};
    const later = [];

    // 1. fond des bandes présentes
    if (spec.ground) {
      ctx.fillStyle = inks[spec.ground];
      ctx.beginPath();
      if (has.top) ctx.rect(ox, oy, ow, t);
      if (has.bottom) ctx.rect(ox, oy + oh - b, ow, b);
      if (has.left) ctx.rect(ox, oy, s, oh);
      if (has.right) ctx.rect(ox + ow - s, oy, s, oh);
      ctx.fill('nonzero');
    }

    drawRules(ctx, spec, inks, g, false);

    // 2. frises : haut, gauche, droite, pied ; une bande sans voisine court jusqu'au bord du cadre
    const x0 = has.left ? ox + s : ox, x1 = has.right ? ox + ow - s : ox + ow;
    const y0 = has.top ? oy + t : oy, y1 = has.bottom ? oy + oh - b : oy + oh;
    const counts = { top: 0, left: 0, right: 0, bottom: 0 };
    if (has.top) counts.top = drawBand(ctx, B.top, C.top, E.top, inks, { x0, y0: oy, ax: 1, ay: 0, dx: 0, dy: 1, len: x1 - x0, depth: t }, later);
    if (has.left) counts.left = drawBand(ctx, B.side, C.side, E.side, inks, { x0: ox, y0, ax: 0, ay: 1, dx: 1, dy: 0, len: y1 - y0, depth: s }, later);
    if (has.right) counts.right = drawBand(ctx, B.side, C.side, E.side, inks, { x0: ox + ow, y0, ax: 0, ay: 1, dx: -1, dy: 0, len: y1 - y0, depth: s }, later);
    if (has.bottom) counts.bottom = drawBand(ctx, B.bottom, C.bottom, E.bottom, inks, { x0, y0: oy + oh, ax: 1, ay: 0, dx: 0, dy: -1, len: x1 - x0, depth: b }, later);

    // 3. coins, là où deux bandes se rencontrent
    const corner = (mod, ok, cx, cy, w, h, fx, fy) => {
      if (!ok || !mod) return;
      const f = () => drawCorner(ctx, spec, mod, inks, cx, cy, w, h, fx, fy);
      mod.above ? later.push(f) : f();
    };
    corner(K.top, has.top && has.left, ox, oy, s, t, 1, 1);
    corner(K.top, has.top && has.right, ox + ow - s, oy, s, t, -1, 1);
    corner(K.bottom, has.bottom && has.left, ox, oy + oh - b, s, b, 1, -1);
    corner(K.bottom, has.bottom && has.right, ox + ow - s, oy + oh - b, s, b, -1, -1);

    drawRules(ctx, spec, inks, g, true);

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

    // 5. pièces « dessus » : elles interrompent filets et baguettes
    for (const f of later) f();

    // 6. ornements extérieurs : aux coins (miroirs), au milieu de chaque côté présent
    const O = spec.outer;
    const place = (mod, ok, a, bb, c, d, e, f) => {
      if (!mod || !ok) return;
      ctx.save();
      ctx.transform(a * u, bb * u, c * u, d * u, e, f);
      drawOps(ctx, mod.ops, inks);
      ctx.restore();
    };
    place(O.corner, has.top && has.left, 1, 0, 0, 1, ox, oy);
    place(O.corner, has.top && has.right, -1, 0, 0, 1, ox + ow, oy);
    place(O.cornerBottom, has.bottom && has.left, 1, 0, 0, -1, ox, oy + oh);
    place(O.cornerBottom, has.bottom && has.right, -1, 0, 0, -1, ox + ow, oy + oh);
    place(O.mid, has.top, 1, 0, 0, 1, ox + ow / 2, oy);
    place(O.midBottom, has.bottom, 1, 0, 0, -1, ox + ow / 2, oy + oh);
    // au milieu de la bande latérale (et de sa pièce centrale), pas du cadre : le pied est souvent plus haut
    const my = (y0 + y1) / 2;
    place(O.midSide, has.left, 0, -1, 1, 0, ox, my);
    place(O.midSide, has.right, 0, 1, -1, 0, ox + ow, my);
    return counts;
  }

  // Garde : la tuile (et ses voisines, pour les raccords) peinte une fois dans un petit canevas, puis répétée.
  function drawPattern(ctx, pat, inks, W, H) {
    const dpr = ctx.getTransform().a || 1;
    const rows = pat.lattice === 'quinconce' ? 2 : 1;
    const k = pat.width / pat.w;
    const tile = document.createElement('canvas');
    tile.width = Math.max(1, Math.round(pat.w * k * dpr));
    tile.height = Math.max(1, Math.round(pat.h * rows * k * dpr));
    const t = tile.getContext('2d');
    t.scale(tile.width / pat.w, tile.height / (pat.h * rows));
    if (pat.ground) { t.fillStyle = inks[pat.ground]; t.fillRect(0, 0, pat.w, pat.h * rows); }
    for (let r = -1; r <= rows; r++) {
      for (let c = -1; c <= 1; c++) {
        t.save();
        t.translate(c * pat.w + (rows === 2 && (r & 1) ? pat.w / 2 : 0), r * pat.h);
        drawOps(t, pat.ops, inks);
        t.restore();
      }
    }
    const p = ctx.createPattern(tile, 'repeat');
    p.setTransform(new DOMMatrix().scale(1 / dpr));
    ctx.save();
    ctx.fillStyle = p;
    ctx.fillRect(0, 0, W, H);
    ctx.restore();
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

  window.MisselFrame = { resolveInks, geometry, drawFrame, drawOps, drawPattern, paperGrain };
})();
