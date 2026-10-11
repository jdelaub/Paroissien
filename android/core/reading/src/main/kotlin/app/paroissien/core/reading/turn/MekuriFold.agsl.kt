// Adapté de Mekuri (https://github.com/AniCanon/mekuri, commit b438b03), MIT © 2026 AniCanon : voir THIRD_PARTY_NOTICES.md.
package app.paroissien.core.reading.turn

/**
 * The fold shader, a term-for-term transliteration of `MekuriFold.metal`.
 *
 * Frozen text. A new term lands in `MekuriFold.metal` first and is copied here
 * term for term; nothing is reassociated, and no branch shares a converted value
 * with another. Every departure from the Metal source is listed below and the list
 * must stay complete:
 *
 * 1. `layer.sample(p)` is `layer.eval(p)` — forced, AGSL's sampler API.
 * 2. The whole-mode local `flat` is `sheet` — forced, `flat` is reserved in
 *    SkSL (`error: 108: expected ';', but found 'flat'`).
 * 3. `M_PI_F` is `mekuriPi` — the substitution is forced, AGSL declares no such
 *    constant; the named-constant form rather than an inline literal is chosen.
 *    3.14159265 rounds to the same float as `M_PI_F`.
 * 4. The `h` suffix on half literals (`0.0h`, `1.0h`) is dropped — forced, AGSL
 *    has no such suffix.
 * 5. The declaration frame — `main` for the `[[stitchable]]` entry point, a
 *    uniform block for its arguments, `const` for `constant`, no `static` — is
 *    forced by the language.
 */
internal const val MEKURI_FOLD_AGSL: String = """
uniform shader layer;
uniform float2 size;
uniform float progress;
uniform float heldRadius;
uniform float radiusSlope;
uniform float shear;
uniform float bow;
uniform float backFaceDim;
uniform float shadowWidth;
uniform float shadowOpacity;
uniform float face;

const float mekuriPi = 3.14159265;

// Unit light direction in the x/z plane, z toward the viewer.
const float2 mekuriLight = float2(0.45, 0.893);

const float mekuriFaceFront = 1.0;
const float mekuriFaceBack = 2.0;
const float mekuriFaceShadow = 3.0;
const half4 mekuriClear = half4(0.0);

// Diffuse shade of a back-face pixel whose surface normal is (nx, nz),
// between `dim` when turned fully from the light and 1 when facing it.
half mekuriBackShade(float nx, float nz, float dim) {
    float facing = saturate(nx * mekuriLight.x + nz * mekuriLight.y);
    return half(mix(dim, 1.0, facing * facing * facing));
}

// Shade of a front-face pixel `u` radii along the roll, from 1 where the
// sheet leaves the page to `dim` at the crest.
half mekuriFrontShade(float u, float dim) {
    return half(mix(1.0, dim, 1.0 - sqrt(saturate(1.0 - u * u))));
}

// Shadow the lifted sheet casts on the page beneath, `d` past the crease.
// Starts at `extent`, how far the sheet reaches past the crease, darkest
// where the roll is tightest, and fades over half that reach.
half4 mekuriContactShadow(float d, float extent, float radius, float heldRadius, float opacity) {
    float reach = 1.0 - saturate((d - extent) / (0.5 * max(extent, 1.0)));
    float alpha = opacity * (heldRadius / radius) * reach * reach;
    return half4(0.0, 0.0, 0.0, half(alpha));
}

// Shadow the sheet's flat-lying edge casts on the page beneath, `d` past
// that edge (negative short of it). Same ramp as the whole-face dimming of
// the flat front.
half4 mekuriCreaseShadow(float d, float width, float opacity) {
    half reach = half(saturate(1.0 + d / width));
    return half4(0.0, 0.0, 0.0, half(opacity) * reach * reach);
}

half4 main(float2 position) {
    float held = 1.0 - position.y / size.y;
    float lead = bow * size.x * progress * (1.0 - progress);
    float axis = size.x * (1.0 - progress) + shear * (position.y - size.y * 0.5) - lead * held * held;
    float flap = size.x - axis;
    float d = position.x - axis;
    float radius = heldRadius + radiusSlope * (size.y - position.y);
    float halfTurn = mekuriPi * radius;
    float lip = halfTurn - flap;
    float extent = flap < 0.5 * halfTurn ? radius * sin(max(flap, 0.0) / radius) : radius;

    if (d > radius) {
        if (face == mekuriFaceFront || face == mekuriFaceBack) {
            return mekuriClear;
        }
        return mekuriContactShadow(d, extent, radius, heldRadius, shadowOpacity);
    }

    if (d >= 0.0) {
        float front = radius * asin(clamp(d / radius, -1.0, 1.0));
        float back = halfTurn - front;
        if (back <= flap) {
            if (face == mekuriFaceFront || face == mekuriFaceShadow) {
                return mekuriClear;
            }
            half4 color = layer.eval(float2(axis + back, position.y));
            float u = d / radius;
            half shade = mekuriBackShade(u, sqrt(saturate(1.0 - u * u)), backFaceDim);
            return half4(color.rgb * shade, color.a);
        }
        if (face == mekuriFaceBack) {
            return mekuriClear;
        }
        if (front <= flap) {
            if (face == mekuriFaceShadow) {
                return mekuriClear;
            }
            half4 color = layer.eval(float2(axis + front, position.y));
            half shade = mekuriFrontShade(d / radius, backFaceDim);
            return half4(color.rgb * shade, color.a);
        }
        if (face == mekuriFaceFront) {
            return mekuriClear;
        }
        return mekuriContactShadow(d, extent, radius, heldRadius, shadowOpacity);
    }

    float behind = halfTurn - d;
    if (behind <= flap) {
        if (face == mekuriFaceFront || face == mekuriFaceShadow) {
            return mekuriClear;
        }
        half4 color = layer.eval(float2(axis + behind, position.y));
        return half4(color.rgb * mekuriBackShade(0.0, 1.0, backFaceDim), color.a);
    }

    if (face == mekuriFaceBack) {
        return mekuriClear;
    }
    if (face == mekuriFaceShadow) {
        return mekuriCreaseShadow(d - lip, shadowWidth, shadowOpacity);
    }
    if (face == mekuriFaceFront) {
        return layer.eval(position);
    }

    half4 sheet = layer.eval(position);
    float reach = saturate(1.0 + (d - lip) / shadowWidth);
    float shade = shadowOpacity * reach * reach;
    return half4(sheet.rgb * (1.0 - half(shade)), sheet.a);
}
"""
