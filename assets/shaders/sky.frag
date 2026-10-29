#version 300 es
// The evening sky behind everything, painted flat: teal overhead going down through a pale
// green to peach at the horizon, warmest where the street runs out into it; long thin bars of
// cloud laid along the horizon, salmon, each with a teal line under it; and far off, now and
// then, birds or a plane's white trail, laid out by the world as strokes. At night the stars
// and the moon. The alpha is 0: the sky is nothing the ink outlines.
precision highp float;

in vec2 vNdc;

uniform vec3 uRight;
uniform vec3 uUp;
uniform vec3 uAhead;
uniform vec2 uFrame;      // the frame in pixels
uniform float uFocal;
uniform float uScale;     // drawn pixels to a frame pixel
uniform vec3 uTop;
uniform vec3 uHorizon;
uniform vec3 uGlow;
uniform vec3 uSun;
uniform float uTime;
uniform float uNight;    // how far into the night: the stars out, the moon in place of the sun
// strokes: ends in frame pixels, then widths and covers at the ends (a negative first width: dark)
uniform vec4 uFly[64];
uniform int uFlyN;

out vec4 outColor;

float h(vec2 p) { return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453); }

const float PI = 3.14159265;

void main() {
    vec3 d = normalize(uAhead * uFocal + uRight * (vNdc.x * uFrame.x * 0.5) + uUp * (vNdc.y * uFrame.y * 0.5));
    float el = max(d.y, 0.0);
    float az = atan(d.x, d.z);                                   // 0 down the street
    float toStreet = max(cos(az), 0.0);
    float k = pow(min(1.0, el * 2.1), 0.62);
    vec3 mid = mix(uHorizon, uTop, 0.45) * vec3(0.98, 1.04, 1.0);
    vec3 c = k < 0.5 ? mix(mix(uHorizon, uGlow, toStreet * toStreet * 0.85), mid, k * 2.0) : mix(mid, uTop, (k - 0.5) * 2.0);
    // the glow where the street runs out: a soft warm dome over its end
    float dome = exp(-(az * az) * 6.0 - el * 14.0) * toStreet;
    c = mix(c, mix(uGlow, vec3(1.0, 0.93, 0.8), 0.35), dome * 0.55 * (1.0 - uNight));

    // bars of cloud: rows at their heights, each a long bar with rounded ends, a teal line under
    float drift = uTime * 0.004;
    vec3 salmon = mix(vec3(0.925, 0.56, 0.47), uGlow, 0.25), teal = mix(vec3(0.43, 0.63, 0.67), uTop, 0.2);
    float light = clamp(length(uHorizon) / 1.49, 0.2, 1.0);
    for (int i = 0; i < 9; i++) {
        float fi = float(i);
        float row = 0.035 + fi * 0.034 + h(vec2(fi, 3.1)) * 0.02;
        float th = 0.0065 + h(vec2(fi, 7.7)) * 0.006;
        float len = 0.14 + h(vec2(fi, 1.3)) * 0.32;
        float at = (h(vec2(fi, 5.9)) - 0.5) * 2.4 + drift * (1.0 + fi * 0.2);
        float da = mod(az - at + PI, 2.0 * PI) - PI;
        float de = el - row;
        // the bar: a stretched capsule
        float q = length(vec2(max(abs(da) - len, 0.0) * 0.55, de));
        if (q < th) c = salmon * light;
        // the line under it, a little shorter, set off to one side
        float da2 = da - len * 0.18;
        float q2 = length(vec2(max(abs(da2) - len * 0.7, 0.0) * 0.55, de + th * 2.1));
        if (q2 < th * 0.45) c = teal * light;
    }

    if (uNight > 0.0) {
        float dark = smoothstep(0.3, 0.75, uNight);
        float s = max(dot(d, uSun), 0.0);
        if (s > 0.99985) c = mix(c, vec3(0.95, 0.94, 0.86), dark);
        vec3 g3 = floor(d * 160.0);
        float st = h(g3.xy + g3.z * vec2(0.37, 0.71));
        if (st > 0.993 && d.y > 0.06) c = mix(c, vec3(0.96, 0.94, 0.86), dark * smoothstep(0.06, 0.25, d.y));
    }
    // what flies: each stroke covers the pixels near its line
    vec2 p = vec2(gl_FragCoord.x, uFrame.y - gl_FragCoord.y) / uScale;
    for (int i = 0; i < 32; i++) {
        if (i >= uFlyN) break;
        vec4 e = uFly[2 * i], m = uFly[2 * i + 1];
        vec2 a = e.xy, ab = e.zw - e.xy;
        float t = clamp(dot(p - a, ab) / max(dot(ab, ab), 1e-4), 0.0, 1.0);
        float dist = length(p - a - ab * t);
        float w = mix(abs(m.x), m.y, t);
        float cover = clamp((w * 0.5 + 0.35 - dist) * 2.5 * uScale * 0.5, 0.0, 1.0) * mix(m.z, m.w, t);
        vec3 ink = m.x < 0.0 ? vec3(0.137, 0.165, 0.29) : vec3(1.0, 0.98, 0.94);
        c = mix(c, ink, cover);
    }
    outColor = vec4(c, 0.0);
}
