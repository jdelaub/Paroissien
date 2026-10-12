// Moteur de cadre : même algorithme que OrnamentalFrame.kt (core:designsystem, paquet frame).
// Entrée : la spécification produite par tools/cadres/generate.py.
(function () {
  const cache = new Map();
  const path = d => {
    let p = cache.get(d);
    if (!p) { p = new Path2D(d); cache.set(d, p); }
    return p;
  };

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
    for (const o of ops) {
      const p = path(o.d);
      if (o.stroke != null) {
        ctx.lineWidth = o.stroke;
        ctx.strokeStyle = inks[o.ink];
        ctx.stroke(p);
      } else {
        ctx.fillStyle = inks[o.ink];
        ctx.fill(p);
      }
    }
  }

  // Géométrie du cadre dans un écran W × H (dp). `top` réserve la place du titre courant.
  function geometry(spec, W, H, top) {
    const L = spec.layout;
    const ox = L.margin, oy = L.margin + (top || 0);
    const ow = W - 2 * L.margin, oh = H - oy - L.margin;
    const t = L.top, s = L.side, b = L.bottom;
    const u = Math.min(t, s) / 100;                    // unité des baguettes
    const barHalf = (L.bar / 2 + L.barOutline) * u;
    const pad = L.textPadding;
    return {
      ox, oy, ow, oh, t, s, b, u,
      text: {
        x: ox + s + barHalf + pad, y: oy + t + barHalf + pad,
        w: ow - 2 * (s + barHalf + pad), h: oh - t - b - 2 * (barHalf + pad),
      },
    };
  }

  // Une bande : `n` éléments répétés, pas ajusté pour tomber juste (≈ border-image: round).
  function drawBand(ctx, mod, inks, band) {
    const k = band.depth / 100;
    const n = Math.max(1, Math.round(band.len / (mod.period * k)));
    const step = band.len / n;
    const ka = step / mod.period;
    ctx.save();
    ctx.beginPath();
    ctx.rect(band.clip[0], band.clip[1], band.clip[2], band.clip[3]);
    ctx.clip();
    for (const pass of ['base', 'over']) {
      for (let i = -1; i <= n + 1; i++) {
        const u0 = (i + mod.phase) * step;
        ctx.save();
        ctx.transform(band.ax * ka, band.ay * ka, band.dx * k, band.dy * k,
                      band.x0 + band.ax * u0, band.y0 + band.ay * u0);
        drawOps(ctx, mod[pass], inks);
        ctx.restore();
      }
    }
    ctx.restore();
    return n;
  }

  function drawBox(ctx, ops, inks, x, y, scale, flipY) {
    ctx.save();
    ctx.transform(scale, 0, 0, flipY ? -scale : scale, x, y);
    drawOps(ctx, ops, inks);
    ctx.restore();
  }

  // Coin : rosace dans le carré côté texte ; si le coin est allongé (pied), une fleur
  // de lys comble le reste, pointe vers l'extérieur.
  function drawCorner(ctx, spec, inks, x, y, w, h, outerAtBottom) {
    const m = spec.modules;
    const q = Math.min(w, h);
    ctx.save();
    ctx.beginPath(); ctx.rect(x, y, w, h); ctx.clip();
    const rosY = outerAtBottom ? y : y + h - q;
    drawBox(ctx, m.corner.ops, inks, x + (w - q) / 2, rosY, q / 100, false);
    const rest = h - q;
    if (rest > 0.25 * w) {
      const f = Math.min(w, rest) / 100;
      const fx = x + w / 2 - 50 * f;
      if (outerAtBottom) drawBox(ctx, m.cornerFill.ops, inks, fx, y + h, f, true);
      else drawBox(ctx, m.cornerFill.ops, inks, fx, y, f, false);
    }
    ctx.restore();
  }

  function drawFrame(ctx, spec, inks, g) {
    const { ox, oy, ow, oh, t, s, b, u } = g;
    const L = spec.layout, m = spec.modules;

    // 1. fond des bandes (anneau entre rectangle extérieur et intérieur)
    ctx.fillStyle = inks.ground;
    ctx.beginPath();
    ctx.rect(ox, oy, ow, oh);
    ctx.rect(ox + s, oy + t, ow - 2 * s, oh - t - b);
    ctx.fill('evenodd');

    // 2. frises
    const vLen = oh - t - b, hLen = ow - 2 * s;
    const counts = {
      top: drawBand(ctx, m.edge, inks, { x0: ox + s, y0: oy, ax: 1, ay: 0, dx: 0, dy: 1, len: hLen, depth: t, clip: [ox + s, oy, hLen, t] }),
      left: drawBand(ctx, m.edge, inks, { x0: ox, y0: oy + t, ax: 0, ay: 1, dx: 1, dy: 0, len: vLen, depth: s, clip: [ox, oy + t, s, vLen] }),
      right: drawBand(ctx, m.edge, inks, { x0: ox + ow, y0: oy + t, ax: 0, ay: 1, dx: -1, dy: 0, len: vLen, depth: s, clip: [ox + ow - s, oy + t, s, vLen] }),
      bottom: drawBand(ctx, m.bottomEdge, inks, { x0: ox + s, y0: oy + oh, ax: 1, ay: 0, dx: 0, dy: -1, len: hLen, depth: b, clip: [ox + s, oy + oh - b, hLen, b] }),
    };

    // 3. coins
    drawCorner(ctx, spec, inks, ox, oy, s, t, false);
    drawCorner(ctx, spec, inks, ox + ow - s, oy, s, t, false);
    drawCorner(ctx, spec, inks, ox, oy + oh - b, s, b, true);
    drawCorner(ctx, spec, inks, ox + ow - s, oy + oh - b, s, b, true);

    // 4. baguettes : rectangle extérieur + baguettes intérieures croisées qui débordent
    const ext = L.barOverhang * u;
    const y1 = oy + t, y2 = oy + oh - b, x1 = ox + s, x2 = ox + ow - s;
    const bars = new Path2D();
    bars.rect(ox, oy, ow, oh);
    for (const y of [y1, y2]) { bars.moveTo(ox - ext, y); bars.lineTo(ox + ow + ext, y); }
    for (const x of [x1, x2]) { bars.moveTo(x, oy - ext); bars.lineTo(x, oy + oh + ext); }
    ctx.lineJoin = 'miter';
    ctx.lineCap = 'butt';
    for (const [ink, w] of [['outline', L.bar + 2 * L.barOutline], ['bar', L.bar], ['barLight', L.barLight]]) {
      ctx.lineWidth = w * u;
      ctx.strokeStyle = inks[ink];
      ctx.stroke(bars);
    }

    // 5. terminaisons (anneau + bourgeon d'or)
    const term = (x, y, deg, push) => {
      const a = deg * Math.PI / 180, c = Math.cos(a), sn = Math.sin(a);
      ctx.save();
      ctx.transform(c * u, sn * u, -sn * u, c * u, x + c * push * u, y + sn * push * u);
      drawOps(ctx, m.terminal.ops, inks);
      ctx.restore();
    };
    for (const y of [y1, y2]) { term(ox - ext, y, 180, 0); term(ox + ow + ext, y, 0, 0); }
    for (const x of [x1, x2]) { term(x, oy - ext, 270, 0); term(x, oy + oh + ext, 90, 0); }
    term(ox, oy, 225, 4); term(ox + ow, oy, 315, 4);
    term(ox + ow, oy + oh, 45, 4); term(ox, oy + oh, 135, 4);
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
