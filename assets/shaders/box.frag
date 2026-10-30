#version 300 es
// The faces of the boxes, painted flat: each material keeps its own simple pattern, and the
// light comes in two tones only, lit or in shade, with the sun's shadows cut clean; far things
// go into the haze. The alpha carries what the face belongs to (its colour, its material, which
// way it faces), so the frame can draw an ink line wherever one thing meets another.
// A material may carry a number of its own in its fraction (a house's seed, a flag).
precision highp float;
precision highp int;
precision highp sampler2DShadow;

in vec3 vWorld;
in vec3 vLocal;
in vec3 vNormal;
in vec3 vColor;
in vec4 vLightPos;
flat in vec3 vHalf;
flat in vec3 vFace;
flat in float vMaterial;

uniform sampler2DShadow uShadow;
uniform float uShadowTexel;
uniform vec3 uSun;
uniform vec3 uSunColor;
uniform vec3 uAmbSky;
uniform vec3 uAmbGround;
uniform vec3 uEye;
uniform vec3 uFog;
uniform float uFogStart;
uniform float uFogScale;
uniform float uTime;          // seconds, for what moves on its own
uniform float uNight;         // how far into the night: windows lit
uniform float uClear;
uniform vec4 uFire;           // something burning: where, and how bright its light on what is near (0, none)         // the see-through pass: only clear plastic is drawn, over what is there

out vec4 outColor;

const float PLAIN = 0.0, GRAIN = 1.0, STONE = 2.0, ASPHALT = 3.0, WOOD = 4.0, PAVING = 5.0, PAINT = 6.0, GOLD = 7.0;
const float FACADE = 8.0, FROND = 9.0, TRUNK = 10.0, ROAD = 11.0, GLASS = 12.0, GLOW = 13.0, SEA = 14.0, RAIL = 15.0;
const float PROMENADE = 16.0, AWNING = 17.0, SQUARE = 18.0, LANE = 19.0;
const float POLISHED = 20.0, RUBBER = 21.0, MATTE = 26.0, CLEAR = 27.0;
const float ROD = 28.0, HEAD = 29.0, FIRE = 30.0;

const vec3 INK = vec3(0.137, 0.165, 0.290);

float hash(vec3 p) {
    p = fract(p * vec3(0.1031, 0.1030, 0.0973));
    p += dot(p, p.yxz + 33.33);
    return fract((p.x + p.y) * p.z);
}

// the integer hash the world uses too, so both agree on which window is shut
int nz(int a, int b, int seed) {
    uint h = uint(a) * 374761393u + uint(b) * 668265263u + uint(seed) * 1442695041u;
    h = (h ^ (h >> 13u)) * 1274126177u;
    return int((h ^ (h >> 16u)) & 255u);
}

vec3 rgb(int c) {
    return vec3(float((c >> 16) & 255), float((c >> 8) & 255), float(c & 255)) / 255.0;
}

// what a face belongs to, as a number for the alpha: never 0, which is the sky
float ident(vec3 col, float mat, vec3 face) {
    // (the colour comes in interpolated, so a hair's difference across a face is rounded away first)
    vec3 c = floor(col * 255.0 + 0.5);
    float h = hash(c + vec3(mat * 7.31, mat * 3.17, mat * 1.93) + floor(face + 0.5) * 41.7);
    return (1.0 + floor(h * 254.0)) / 255.0;
}

// how much of the sun reaches a point, seen from the sun as lp
float shadowAt(vec4 lp) {
    vec3 p = lp.xyz * 0.5 + 0.5;
    if (p.x < 0.0 || p.x > 1.0 || p.y < 0.0 || p.y > 1.0 || p.z > 1.0) return 1.0;
    p.z -= 0.0012;
    float s = 0.0;
    s += texture(uShadow, vec3(p.xy + vec2(-0.5, -0.5) * uShadowTexel, p.z));
    s += texture(uShadow, vec3(p.xy + vec2(0.5, -0.5) * uShadowTexel, p.z));
    s += texture(uShadow, vec3(p.xy + vec2(-0.5, 0.5) * uShadowTexel, p.z));
    s += texture(uShadow, vec3(p.xy + vec2(0.5, 0.5) * uShadowTexel, p.z));
    return s * 0.25;
}

// the two tones of the light: in shade, a cool share of the colour; lit, a warm one
vec3 toon(vec3 base, vec3 n, float light) {
    float t = smoothstep(0.16, 0.26, light);
    vec3 shade = 0.33 + uAmbSky * 1.05;
    vec3 lit = 0.5 + uSunColor * 0.32;
    lit = max(lit, shade);
    vec3 k = mix(shade, lit, t);
    // tops a little lighter than the sides, under the open sky
    if (n.y > 0.6) k *= 1.04;
    return base * k;
}

vec3 haze(vec3 c, vec3 world) {
    float dist = length(world - uEye);
    float h = 1.0 - exp(-max(dist - uFogStart, 0.0) / uFogScale);
    return mix(c, uFog, h);
}

// a frond: leaflets slanting from a spine toward the tip, gaps between them (as in shadow.frag)
bool frondHole(vec3 l, vec3 hf) {
    float across = (l.z / hf.z + 1.0) * 0.5;
    if (across < 0.1) return false;
    if (across > 0.96) return true;
    return across > 0.5 && fract((l.x - (l.z + hf.z) * 0.8) / 0.16) > 0.62 - (across - 0.5) * 0.5;
}

// a railing: a rail along the top and the bottom, bars between (as in shadow.frag)
bool railHole(vec3 l, vec3 hf) {
    float v = l.y + hf.y;
    if (v < 0.05 || v > 2.0 * hf.y - 0.06) return false;
    float a = hf.x > hf.z ? l.x : l.z;
    return fract(a / 0.13) > 0.3;
}

/**
 * A street front in a seaside quarter at (u along the wall, v up), meters: shops at street
 * level, above them tall windows framed in ink with shutters beside them, floor after floor, a
 * cornice line at every floor. glass is set where the pane is (2 when lit at night).
 */
vec3 facade(float u, float v, vec3 base, int seed, out float glass) {
    glass = 0.0;
    const float fh = 3.3, bw = 2.9;
    int storey = int(floor(v / fh));
    float fv = v - float(storey) * fh;
    int bay = int(floor(u / bw));
    float fu = u - float(bay) * bw;
    int h = nz(bay, storey, seed);
    vec3 wall = base;
    if (storey > 0 && fv < 0.12) return INK;                            // cornice line
    if (storey > 0 && fv < 0.26) return base * 1.07;
    if (storey == 0) {
        if (fu > 0.3 && fu < 2.6 && fv < 2.75) {
            if (fu < 0.4 || fu > 2.5 || fv > 2.65 || fv < 0.1) return INK;
            glass = 1.0;
            if (uNight > 0.0 && float(nz(bay, 77, seed)) < 255.0 * 0.7 * uNight) glass = 2.0;
            // a striped blind half down over some of the shop windows
            if (nz(bay, 99, seed) > 150 && fv > 1.7) return mod(floor(fu * 3.0), 2.0) == 0.0 ? rgb(0xD9503A) : rgb(0xF3E9D6);
            return rgb(0x2C3A55);
        }
        return wall * 0.92;
    }
    bool closed = (h & 7) == 0;
    vec3 shutter = (h & 16) != 0 ? rgb(0x4E8C86) : rgb(0x5B86A0);
    if (fv > 0.36 && fv < 2.7) {
        if (fu > 0.95 && fu < 1.95) {
            if (fu < 1.03 || fu > 1.87 || fv < 0.44 || fv > 2.62) return INK;     // the frame, inked
            if (closed) return shutter;
            glass = 1.0;
            if (uNight > 0.0 && float(nz(bay, storey + 50, seed)) < 255.0 * 0.38 * uNight) glass = 2.0;
            // the pane: the sky in it, a pale streak across its top corner
            float s = fu - 1.03 + (2.62 - fv) * 0.6;
            if (s > 0.12 && s < 0.26) return rgb(0x9ED0D4);
            return rgb(0x3F7E8C);
        }
        if ((fu > 0.62 && fu < 0.95) || (fu > 1.95 && fu < 2.28)) {
            if (closed) return wall;
            if (fu < 0.67 || fu > 2.23) return INK;
            return shutter;                                                    // open shutters
        }
    }
    return wall;
}

/**
 * A stretch of asphalt with its paint, in the stretch's own frame: across it (0 the middle,
 * half its width at the curbs) and along it; the middle dashes, the lines along the edges.
 */
vec3 road(float across, float along, float hw, vec3 base) {
    float ax = abs(across);
    vec3 c = base;
    if (ax < 0.09 && mod(along, 6.0) < 2.6) c = rgb(0xF0E6D2);
    if (ax > hw - 0.3 && ax < hw - 0.17) c = rgb(0xE9DCC2);
    else if (ax >= hw - 0.17) c = base * 0.9;
    return c;
}

/** The warm light thrown by what burns on what is near it, falling off within some nine meters. */
vec3 fireLight(vec3 p, vec3 n) {
    if (uFire.w <= 0.0) return vec3(0.0);
    vec3 d = uFire.xyz - p;
    float r = length(d);
    float fall = max(0.0, 1.0 - r / 9.0);
    float face = 0.25 + 0.75 * max(dot(n, d / max(r, 1e-3)), 0.0);
    return vec3(1.0, 0.42, 0.1) * uFire.w * fall * fall * face;
}

/** Heat as colour: black-red crust, red, orange, gold, white. */
vec3 heat(float h) {
    h = clamp(h, 0.0, 1.0);
    vec3 c0 = vec3(0.24, 0.02, 0.01), c1 = vec3(0.82, 0.08, 0.02), c2 = vec3(1.0, 0.36, 0.03),
         c3 = vec3(1.0, 0.66, 0.06), c4 = vec3(1.0, 0.9, 0.5);
    if (h < 0.3) return mix(c0, c1, h / 0.3);
    if (h < 0.55) return mix(c1, c2, (h - 0.3) / 0.25);
    if (h < 0.8) return mix(c2, c3, (h - 0.55) / 0.25);
    return mix(c3, c4, (h - 0.8) / 0.2);
}

#ifdef SPHERE
flat in vec3 vCenter;
flat in vec3 vTurn0, vTurn1, vTurn2;          // the ball's turn, column after column
uniform mat4 uViewProj;
uniform mat4 uLight;

// a water bottle's outline, in its own frame squeezed to -1..1 every way
float bottleR(float y) {
    if (y < -0.92) return 0.9 + (y + 1.0) * 1.25;
    if (y < 0.42) return 1.0;
    if (y < 0.74) return mix(1.0, 0.38, (y - 0.42) / 0.32);
    return 0.38;
}

bool inBottle(vec3 p) {
    return p.y > -1.0 && p.y < 0.86 && length(p.xz) < bottleR(p.y);
}

/** Clear plastic: only the bottle's near skin, pale blue, whiter at the rims, laid over what is behind. */
void clearShell() {
    vec3 hs = vHalf;
    mat3 vTurn = mat3(vTurn0, vTurn1, vTurn2);
    mat3 back = transpose(vTurn);
    vec3 ro = back * (uEye - vCenter) / hs;
    vec3 rd = normalize(back * (vWorld - uEye) / hs);
    vec3 inv = 1.0 / rd;
    vec3 ta = (-1.0 - ro) * inv, tb = (1.0 - ro) * inv;
    vec3 tn = min(ta, tb), tf = max(ta, tb);
    float t0 = max(max(tn.x, tn.y), max(tn.z, 0.0)), t1 = min(min(tf.x, tf.y), tf.z);
    if (t1 <= t0) discard;
    float dt = (t1 - t0) / 40.0, t = t0;
    bool hit = false;
    for (int i = 0; i < 40; i++) {
        if (inBottle(ro + rd * t)) { hit = true; break; }
        t += dt;
    }
    if (!hit) discard;
    float lo = t - dt, hi = t;
    for (int i = 0; i < 6; i++) {
        float m = (lo + hi) * 0.5;
        if (inBottle(ro + rd * m)) hi = m; else lo = m;
    }
    vec3 l = ro + rd * hi;
    vec3 nl;
    if (l.y < -0.985) nl = vec3(0.0, -1.0, 0.0);
    else {
        float slope = (l.y > 0.42 && l.y < 0.74) ? 0.62 / 0.32 : 0.0;
        vec2 r = normalize(l.xz);
        nl = vec3(r.x, slope * bottleR(l.y), r.y);
    }
    vec3 world = vCenter + vTurn * (l * hs);
    vec4 clip = uViewProj * vec4(world, 1.0);
    gl_FragDepth = clip.z / clip.w * 0.5 + 0.5;
    vec3 n = normalize(vTurn * (nl / hs));
    vec3 v = normalize(uEye - world);
    float rim = 1.0 - max(dot(n, v), 0.0);
    vec3 c = rim > 0.72 ? vec3(0.93, 0.97, 1.0) : rgb(0xA8D6F2);
    float a = rim > 0.72 ? 0.85 : 0.3;
    // a hard white streak down the lit side
    float sp = dot(n, normalize(uSun + v));
    if (sp > 0.93) { c = vec3(1.0); a = 0.95; }
    c = haze(c, world);
    outColor = vec4(c * a, a);
}

/** The point where the eye's ray meets a rod along the ball's own x, in its squeezed frame; false if it misses. */
bool rod(vec3 ro, vec3 rd, out vec3 l, out vec3 nl) {
    // the side: y*y + z*z = 1
    float a = dot(rd.yz, rd.yz), b = dot(ro.yz, rd.yz), c = dot(ro.yz, ro.yz) - 1.0;
    float t = 1e9;
    if (a > 1e-6) {
        float h = b * b - a * c;
        if (h >= 0.0) {
            float s = (-b - sqrt(h)) / a;
            if (s > 0.0 && abs(ro.x + rd.x * s) <= 1.0) {
                t = s;
                l = ro + rd * s;
                nl = vec3(0.0, l.y, l.z);
            }
        }
    }
    // the ends
    for (int i = 0; i < 2; i++) {
        float e = i == 0 ? -1.0 : 1.0;
        if (abs(rd.x) < 1e-6) continue;
        float s = (e - ro.x) / rd.x;
        vec3 p = ro + rd * s;
        if (s > 0.0 && s < t && dot(p.yz, p.yz) <= 1.0) {
            t = s;
            l = p;
            nl = vec3(e, 0.0, 0.0);
        }
    }
    return t < 1e8;
}

void sphere(float mat) {
    if ((mat == CLEAR) != (uClear > 0.5)) discard;
    if (mat == CLEAR) {
        clearShell();
        return;
    }
    vec3 hs = vHalf;
    mat3 vTurn = mat3(vTurn0, vTurn1, vTurn2);
    mat3 back = transpose(vTurn);
    vec3 ro = back * (uEye - vCenter) / hs;
    vec3 rd = normalize(back * (vWorld - uEye) / hs);
    vec3 l, nl;
    vec3 base = vColor;
    if (mat == ROD || mat == HEAD) {
        if (!rod(ro, rd, l, nl)) discard;
        if (mat == HEAD && nl.x == 0.0) {
            // the head's own paint: a band of scallops round its lower half, in the dark of the caps
            // (the band runs along the barrel; the scallops rise out of the dark half into the paint)
            // (its local z runs back toward the handle: the dark half is the one nearer the hand)
            float wave = 0.3 * abs(sin(l.x * 3.14159 * 2.5));
            if (l.z > 0.25 - wave) base = rgb(0x26304F);
        }
    } else {
        float b = dot(ro, rd);
        float h = b * b - dot(ro, ro) + 1.0;
        if (h < 0.0) discard;
        l = ro + rd * (-b - sqrt(h));
        nl = l;
    }
    vec3 world = vCenter + vTurn * (l * hs);
    vec4 clip = uViewProj * vec4(world, 1.0);
    gl_FragDepth = clip.z / clip.w * 0.5 + 0.5;
    vec3 n = normalize(vTurn * (nl / hs));
    vec3 v = normalize(uEye - world);
    bool polished = mat == POLISHED;
    bool hole = false;
    if (polished) {
        const vec3 H0 = vec3(0.0, 0.96, 0.28), H1 = vec3(-0.24, 0.95, -0.2), H2 = vec3(0.24, 0.95, -0.2);
        hole = max(max(dot(l, H0), dot(l, H1)), dot(l, H2)) > 0.988;
        if (hole) base = INK;
    }
    float sh = shadowAt(uLight * vec4(world + n * 0.04, 1.0));
    vec3 c = toon(base, n, max(dot(n, uSun), 0.0) * sh);
    c += base * fireLight(world, n);
    // a flat spot of shine, cut clean, where the sun glances off it
    if (!hole && mat != MATTE) {
        float sp = dot(n, normalize(uSun + v));
        float cut = polished ? 0.975 : mat == RUBBER ? 0.95 : 0.985;
        if (sp > cut && sh > 0.5) c = mix(c, vec3(1.0, 0.98, 0.92), 0.85);
    }
    c = haze(c, world);
    // a ball is one thing whichever way it faces; a rod's ends are faces of their own
    outColor = vec4(c, ident(vColor, mat, mat == ROD || mat == HEAD ? abs(nl.x) * vec3(1.0, 0.0, 0.0) : vec3(0.0)));
}
#endif

void main() {
    float mat = floor(vMaterial + 0.001);
#ifdef SPHERE
    sphere(mat);
    return;
#endif
    int seed = int(floor(fract(vMaterial + 0.001) * 256.0));
    if (mat == FROND && frondHole(vLocal, vHalf)) discard;
    if (mat == RAIL && railHole(vLocal, vHalf)) discard;

    // the face's own two directions, in meters, and how far each runs from the middle
    vec2 uv, ext;
    if (abs(vFace.x) > 0.5) { uv = vLocal.zy; ext = vHalf.zy; }
    else if (abs(vFace.y) > 0.5) { uv = vLocal.xz; ext = vHalf.xz; }
    else { uv = vLocal.xy; ext = vHalf.xy; }
    vec3 base = vColor;
    float k = 1.0;
    float glass = 0.0, leaf = 0.0, gloss = 0.0;
    vec3 emit = vec3(0.0);
    vec3 v = normalize(uEye - vWorld);
    vec3 face = vFace;

    if (mat == GLOW) {
        // lit from inside: no sun, no shade, only the haze
        outColor = vec4(haze(min(base * 1.25, vec3(1.0)), vWorld), ident(vColor, mat, face));
        return;
    }
    if (mat == FIRE) {
        // burning through: each block hot at its heart, crusted dark at its edges, the heat
        // running up it in waves; the seed is how hot the block is
        float h = float(seed) / 250.0;
        vec2 e = abs(uv) / max(ext, vec2(1e-3));
        float rim = max(e.x, e.y);
        float core = 1.0 - smoothstep(0.35, 0.92, rim);
        float wave = sin(vWorld.y * 6.0 - uTime * 9.0 + sin(vWorld.x * 2.3 + vWorld.z * 1.9) * 2.0);
        float tip = vFace.y > 0.5 ? 0.12 : 0.0;
        float t = h - 0.42 + core * 0.48 + wave * 0.08 + tip;
        outColor = vec4(haze(heat(t), vWorld), ident(vColor, mat, face));
        return;
    }
    if (mat == ROAD) {
        // the long way of the stretch is the way it runs; dashes counted from the world, so stretches meet in step
        base = vHalf.x > vHalf.z ? road(vLocal.z, vWorld.x, vHalf.z, vColor) : road(vLocal.x, vWorld.z, vHalf.x, vColor);
    } else if (mat == WOOD) {
        // planks along the longer way, every other a shade apart
        float across = ext.x > ext.y ? uv.y : uv.x;
        k = mod(floor(across / 0.09), 2.0) == 0.0 ? 1.0 : 0.94;
    } else if (mat == PAVING) {
        vec2 f = fract(vWorld.xz / 0.9);
        k = (f.x < 0.04 || f.y < 0.04) ? 0.88 : 1.0;
    } else if (mat == PROMENADE) {
        vec2 f = fract(vWorld.xz / 1.4);
        k = (f.x < 0.03 || f.y < 0.03) ? 0.9 : 1.0;
    } else if (mat == SQUARE) {
        float rr = length(vec2(vLocal.x, vLocal.z * 1.18));
        k = mod(rr, 1.2) < 0.06 ? 0.86 : 1.0;
    } else if (mat == LANE) {
        bool alongX = vHalf.x > vHalf.z;
        float across = alongX ? vLocal.z : vLocal.x, along = alongX ? vWorld.x : vWorld.z;
        if (abs(across) < 0.08 && mod(along, 6.0) < 2.6) base = rgb(0xF0E6D2);
    } else if (mat == FACADE) {
        if (abs(vFace.y) < 0.5) {
            base = facade(uv.x + ext.x, uv.y + ext.y, vColor, seed, glass);
            if (glass > 1.5) emit = rgb(0xFFC56A) * uNight;
        } else {
            base *= 0.78;                                 // the roof
        }
    } else if (mat == AWNING) {
        if (seed == 1 && mod(floor(vLocal.x * 4.0), 2.0) == 0.0) base = rgb(0xF4EEE2);
    } else if (mat == TRUNK) {
        float r = fract((vLocal.y + float(seed) * 0.07) / 0.3);
        k = r < 0.2 ? 0.82 : 1.0;
    } else if (mat == FROND) {
        leaf = 1.0;
    } else if (mat == GLASS) {
        glass = 1.0;
    } else if (mat == SEA) {
        // the sea: deep teal near, paler far, a few bright dashes toward the sun
        float far = clamp(1.0 - v.y * 9.0, 0.0, 1.0);
        base = mix(rgb(0x2F7E8E), rgb(0x8FC2C0), smoothstep(0.55, 1.0, far));
        float n = hash(vec3(floor(vWorld.x * 0.8), floor(vWorld.z * 0.35), 9.0));
        if (n > 0.9) base = mix(base, rgb(0xF4E6C8), 0.7);
    } else if (mat == GOLD) {
        gloss = 1.0;
    }
    vec3 albedo = base * k;
    vec3 n = normalize(vNormal);
    float sh = shadowAt(vLightPos);
    float ndl = dot(n, uSun);
    // leaves let the light through: lit from either side
    float light = leaf > 0.0 ? (0.4 + 0.6 * abs(ndl)) * sh : max(ndl, 0.0) * sh;
    vec3 c = toon(albedo, n, light);
    c += albedo * fireLight(vWorld, n);
    if (glass > 0.0 && glass < 1.5 && mat == GLASS) {
        // glass: the sky's teal in it, a pale streak across
        float s = uv.x + uv.y * 0.7;
        c = mix(rgb(0x3F7E8C), rgb(0x9ED0D4), step(0.7, fract(s * 1.3)));
    }
    if (gloss > 0.0) {
        // gold: a band of pale light across each face
        float s = (uv.x / max(ext.x, 1e-3) + uv.y / max(ext.y, 1e-3)) * 0.5;
        if (abs(s - 0.25) < 0.12) c = mix(c, vec3(1.0, 0.96, 0.78), 0.6);
    }
    c += emit;
    c = haze(c, vWorld);
    // a few things are drawn as part of what they stand on, without a line between
    if (seed == 252) face = vec3(0.0);
    outColor = vec4(c, ident(vColor, mat, face));
}
