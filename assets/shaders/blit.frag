#version 300 es
// The printed scene onto the screen: scaled up from the size it was drawn at, the flat layer on
// top, then the fine layer at the top of the frame (the crest, or the room over all of it), then
// the aim, drawn smooth at the screen's own size from what the world says of it: the ring on the
// building and its wind-up, the finger's own circle, the dotted flight. The grain of the paper
// over all of it.
precision highp float;
precision highp int;

in vec2 vNdc;

uniform sampler2D uPrint;   // the scene as printed, at the size it was drawn
uniform sampler2D uFlat;
uniform sampler2D uHud;     // the fine layer over the top of the frame, premultiplied
uniform float uHudRows;     // how many frame rows it covers from the top; none, 0
uniform vec2 uFrameSize;    // the frame, in frame pixels
uniform vec2 uScreen;       // the screen, in its own pixels
uniform float uScale;       // screen pixels to a frame pixel
// the aim: the ring's middle (frame pixels), the wind-up 0..1, shown (1) or not; the finger:
// where it is, shown or not; the dots of the flight, so many, each x, y and kind (0 the aim's,
// 1 the throw's trail), and the box they all lie in (frame pixels), so the rest of the screen
// skips them
uniform vec4 uRing;
uniform vec3 uFinger;
uniform vec3 uTrail[32];
uniform int uTrailN;
uniform vec4 uTrailBox;
uniform float uTime;

out vec4 outColor;

const vec3 INK = vec3(0.137, 0.165, 0.290);

float hash(vec2 p) {
    vec3 q = fract(vec3(p.xyx) * vec3(0.1031, 0.1030, 0.0973));
    q += dot(q, q.yzx + 33.33);
    return fract((q.x + q.y) * q.z);
}

// a soft disc or ring, in screen pixels: 1 inside, 0 outside, a pixel of smoothing
float disc(vec2 p, vec2 c, float r) {
    return clamp(r - length(p - c) + 0.5, 0.0, 1.0);
}

float ring(vec2 p, vec2 c, float r, float w) {
    float d = abs(length(p - c) - r);
    return clamp(w * 0.5 - d + 0.5, 0.0, 1.0);
}

void main() {
    vec2 uv = vNdc * 0.5 + 0.5;
    vec3 c = texture(uPrint, uv).rgb;
    vec4 flat2 = texture(uFlat, vec2(uv.x, 1.0 - uv.y));
    c = mix(c, flat2.rgb, flat2.a);
    if (uHudRows > 0.0) {
        float gy = (1.0 - uv.y) * uFrameSize.y;
        if (gy < uHudRows) {
            vec4 hd = texture(uHud, vec2(uv.x, gy / uHudRows));
            c = c * (1.0 - hd.a) + hd.rgb;
        }
    }

    // the aim, in screen pixels from the top left
    vec2 p = vec2(gl_FragCoord.x, uScreen.y - gl_FragCoord.y);
    float s = uScale;
    vec2 pf = p / s;
    bool nearTrail = uTrailN > 0 && pf.x > uTrailBox.x - 4.0 && pf.y > uTrailBox.y - 4.0
            && pf.x < uTrailBox.z + 4.0 && pf.y < uTrailBox.w + 4.0;
    for (int i = 0; i < 32; i++) {
        if (!nearTrail) break;
        if (i >= uTrailN) break;
        vec3 t = uTrail[i];
        vec2 q = t.xy * s;
        if (t.z > 0.5) {
            // the throw's trail: orange dots, inked round
            float o = disc(p, q, 3.0 * s);
            float in1 = disc(p, q, 2.3 * s);
            c = mix(c, INK, o * 0.8);
            c = mix(c, vec3(0.93, 0.45, 0.2), in1);
        } else {
            float o = disc(p, q, 1.25 * s);
            c = mix(c, vec3(1.0, 0.97, 0.9), o * 0.9);
        }
    }
    if (uFinger.z > 0.5 && length(pf - uFinger.xy) < 20.0) {
        vec2 f = uFinger.xy * s;
        c = mix(c, vec3(1.0), disc(p, f, 16.0 * s) * 0.14);
        c = mix(c, INK, ring(p, f, 16.0 * s, 2.4 * s) * 0.35);
        c = mix(c, vec3(1.0, 0.98, 0.94), ring(p, f, 16.0 * s, 1.2 * s) * 0.9);
    }
    if (uRing.w > 0.5 && length(pf - uRing.xy) < 26.0) {
        vec2 r0 = uRing.xy * s;
        float power = uRing.z;
        c = mix(c, vec3(1.0, 0.98, 0.94), disc(p, r0, 21.0 * s) * 0.16);
        // the wind-up: an orange band from the top, clockwise; full, it flickers
        vec2 d = p - r0;
        float a = atan(d.x, -d.y);
        if (a < 0.0) a += 6.2831853;
        float fill = power >= 1.0 ? 1.0 : power;
        float band = ring(p, r0, 22.0 * s, 4.6 * s) * step(a, fill * 6.2831853);
        vec3 orange = power >= 1.0 && fract(uTime * 4.0) < 0.5 ? vec3(1.0, 0.72, 0.36) : vec3(0.91, 0.42, 0.18);
        c = mix(c, orange, band);
        // the ring itself: white, a thin shade of ink under it
        c = mix(c, INK, ring(p, r0 + vec2(0.0, 0.6 * s), 14.0 * s, 3.4 * s) * 0.3);
        c = mix(c, vec3(1.0, 0.98, 0.94), ring(p, r0, 14.0 * s, 2.6 * s));
        c = mix(c, vec3(1.0, 0.98, 0.94), disc(p, r0, 1.3 * s));
    }

    // the paper: a fine grain over everything, a little warm
    float n = hash(floor(gl_FragCoord.xy * 0.75)) - 0.5;
    c += n * 0.045;
    c = mix(c, c * vec3(1.02, 1.0, 0.96), 0.5);
    outColor = vec4(clamp(c, 0.0, 1.0), 1.0);
}
