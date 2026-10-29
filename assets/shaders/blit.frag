#version 300 es
// The drawn scene onto the screen, as a printed picture: an ink line wherever one thing meets
// another (a step in depth, or a face of another thing), a screen of halftone dots in the
// shadows, the grain of the paper over all of it. The flat layer goes on top, then the fine
// layer at the top of the frame (the crest, or the room over all of it), then the aim, drawn
// smooth from what the world says of it: the ring on the building and its wind-up, the
// finger's own circle, the dotted flight.
precision highp float;
precision highp int;

in vec2 vNdc;

uniform sampler2D uScene;   // the scene, drawn at the screen's own size; alpha says what each pixel belongs to
uniform sampler2D uDepth;   // and its depth
uniform sampler2D uFlat;
uniform vec3 uGrade;        // the hour's grade: contrast, saturation, warmth
uniform sampler2D uHud;     // the fine layer over the top of the frame, premultiplied
uniform float uHudRows;     // how many frame rows it covers from the top; none, 0
uniform vec2 uFrameSize;    // the frame, in frame pixels
uniform float uScale;       // screen pixels to a frame pixel
uniform float uNight;
// the aim: the ring's middle (frame pixels), the wind-up 0..1, shown (1) or not; the finger:
// where it is, shown or not; the dots of the flight, so many, each x, y and kind (0 the aim's, 1 the throw's trail)
uniform vec4 uRing;
uniform vec3 uFinger;
uniform vec3 uTrail[32];
uniform int uTrailN;
uniform float uTime;

out vec4 outColor;

const vec3 INK = vec3(0.137, 0.165, 0.290);

float near = 0.1, far = 500.0;

float lin(float d) {
    float z = d * 2.0 - 1.0;
    return 2.0 * near * far / (far + near - z * (far - near));
}

float hash(vec2 p) {
    vec3 q = fract(vec3(p.xyx) * vec3(0.1031, 0.1030, 0.0973));
    q += dot(q, q.yzx + 33.33);
    return fract((q.x + q.y) * q.z);
}

// a line of ink on this pixel: where a neighbour lies well behind it, or belongs to another
// thing (one side of the meeting only, so the line is as wide as the reach)
float edge(ivec2 p, int r) {
    ivec2 size = textureSize(uScene, 0);
    float dc = lin(texelFetch(uDepth, p, 0).r);
    float ic = texelFetch(uScene, p, 0).a;
    if (ic == 0.0) return 0.0;
    float hits = 0.0;
    for (int i = 0; i < 8; i++) {
        vec2 dir = i < 4 ? vec2(i == 0 ? 1.0 : i == 1 ? -1.0 : 0.0, i == 2 ? 1.0 : i == 3 ? -1.0 : 0.0)
                         : vec2(i == 4 || i == 6 ? 0.7071 : -0.7071, i < 6 ? 0.7071 : -0.7071);
        ivec2 q = clamp(p + ivec2(round(dir * float(r))), ivec2(0), size - 1);
        float dn = lin(texelFetch(uDepth, q, 0).r);
        float in2 = texelFetch(uScene, q, 0).a;
        bool behind = dn - dc > 0.03 * dc + 0.04;
        bool other = in2 != ic && in2 > ic && abs(dn - dc) < 0.03 * dc + 0.3;
        if (behind || other) hits += 1.0;
    }
    float e = smoothstep(0.5, 2.5, hits);
    // far off the lines thin away into the haze
    return e * (1.0 - 0.65 * smoothstep(35.0, 170.0, dc));
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
    ivec2 px = ivec2(gl_FragCoord.xy);
    vec2 uv = vNdc * 0.5 + 0.5;
    vec4 sc = texelFetch(uScene, px, 0);
    vec3 scene = sc.rgb;
    // the hour of the day: colours deeper and contrast harder toward sunset, warm or cool
    if (uGrade != vec3(1.0, 1.0, 0.0)) {
        float l0 = dot(scene, vec3(0.299, 0.587, 0.114));
        scene = mix(vec3(l0), scene, uGrade.y);
        scene = clamp((scene - 0.45) * uGrade.x + 0.45, 0.0, 1.0);
        scene *= vec3(1.0 + 0.05 * uGrade.z, 1.0 + 0.01 * uGrade.z, 1.0 - 0.07 * uGrade.z);
    }
    // the halftone: dots on a screen turned at 45 degrees, larger the darker the colour
    float cell = max(4.0, uScale * 1.7);
    vec2 g = mat2(0.7071, -0.7071, 0.7071, 0.7071) * gl_FragCoord.xy / cell;
    float dd = length(fract(g) - 0.5);
    float L = dot(scene, vec3(0.299, 0.587, 0.114));
    float amount = clamp((0.78 - L) * 0.85, 0.06, 0.55);
    float rr = sqrt(amount) * 0.42;
    float dotk = clamp((rr - dd) * cell + 0.5, 0.0, 1.0);
    scene = mix(scene, scene * vec3(0.8, 0.82, 0.9), dotk * 0.55);
    // the ink
    int reach = max(1, int(floor(uScale * 0.75 + 0.5)));
    float ink = edge(px, reach);
    scene = mix(scene, mix(INK, INK * 0.6, uNight), ink * 0.95);

    vec4 flat2 = texture(uFlat, vec2(uv.x, 1.0 - uv.y));
    vec3 c = mix(scene, flat2.rgb, flat2.a);
    if (uHudRows > 0.0) {
        float gy = (1.0 - uv.y) * uFrameSize.y;
        if (gy < uHudRows) {
            vec4 hd = texture(uHud, vec2(uv.x, gy / uHudRows));
            c = c * (1.0 - hd.a) + hd.rgb;
        }
    }

    // the aim, in screen pixels from the top left
    vec2 p = vec2(gl_FragCoord.x, float(textureSize(uScene, 0).y) - gl_FragCoord.y);
    float s = uScale;
    for (int i = 0; i < 32; i++) {
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
    if (uFinger.z > 0.5) {
        vec2 f = uFinger.xy * s;
        c = mix(c, vec3(1.0), disc(p, f, 16.0 * s) * 0.14);
        c = mix(c, INK, ring(p, f, 16.0 * s, 2.4 * s) * 0.35);
        c = mix(c, vec3(1.0, 0.98, 0.94), ring(p, f, 16.0 * s, 1.2 * s) * 0.9);
    }
    if (uRing.w > 0.5) {
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
