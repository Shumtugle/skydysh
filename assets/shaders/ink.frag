#version 300 es
// The drawn scene as a printed picture, at the size it was drawn: the hour's grade, a screen of
// halftone dots in the shadows, an ink line wherever one thing meets another (a step in depth,
// or a face of another thing). The screen pass scales it up and lays the rest over it.
precision highp float;
precision highp int;

in vec2 vNdc;

uniform sampler2D uScene;   // the scene; alpha says what each pixel belongs to
uniform sampler2D uDepth;   // and its depth
uniform vec3 uGrade;        // the hour's grade: contrast, saturation, warmth
uniform float uScale;       // drawn pixels to a frame pixel
uniform float uNight;
// a glow round something burning: its middle and reach across (frame pixels; it reaches half as
// far again up and down), how strong (0, none), and how far off it is (meters): only what is as
// far or farther takes it, so nothing nearer is lit and no wall shows it through
uniform vec4 uHalo;
uniform float uHaloDepth;

out vec4 outColor;

const vec3 INK = vec3(0.137, 0.165, 0.290);

float near = 0.1, far = 500.0;

float lin(float d) {
    float z = d * 2.0 - 1.0;
    return 2.0 * near * far / (far + near - z * (far - near));
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

void main() {
    ivec2 px = ivec2(gl_FragCoord.xy);
    vec4 sc = texelFetch(uScene, px, 0);
    vec3 scene = sc.rgb;
    // the hour of the day: colours deeper and contrast harder toward sunset, warm or cool
    if (uGrade != vec3(1.0, 1.0, 0.0)) {
        float l0 = dot(scene, vec3(0.299, 0.587, 0.114));
        scene = mix(vec3(l0), scene, uGrade.y);
        scene = clamp((scene - 0.45) * uGrade.x + 0.45, 0.0, 1.0);
        scene *= vec3(1.0 + 0.05 * uGrade.z, 1.0 + 0.01 * uGrade.z, 1.0 - 0.07 * uGrade.z);
    }
    // the glow round what burns: a warm light laid over, as a print in fluorescent orange
    float glow = 0.0;
    if (uHalo.w > 0.0) {
        vec2 fp = vec2(gl_FragCoord.x, float(textureSize(uScene, 0).y) - gl_FragCoord.y) / uScale;
        vec2 q = (fp - uHalo.xy) / vec2(uHalo.z, uHalo.z * 1.5);
        float r2 = dot(q, q);
        if (r2 < 4.0 && lin(texelFetch(uDepth, px, 0).r) > uHaloDepth - 1.5) {
            glow = exp(-r2 * 1.3) * smoothstep(0.08, 0.5, r2) * uHalo.w;
            scene = 1.0 - (1.0 - scene) * (1.0 - vec3(1.0, 0.5, 0.14) * glow * 0.6);
        }
    }
    // the halftone: dots on a screen turned at 45 degrees, larger the darker the colour
    float cell = max(2.5, uScale * 1.7);
    vec2 g = mat2(0.7071, -0.7071, 0.7071, 0.7071) * gl_FragCoord.xy / cell;
    float dd = length(fract(g) - 0.5);
    float L = dot(scene, vec3(0.299, 0.587, 0.114));
    float amount = clamp((0.78 - L) * 0.85, 0.06, 0.55);
    float rr = sqrt(amount) * 0.42;
    float dotk = clamp((rr - dd) * cell + 0.5, 0.0, 1.0);
    // (in the glow the dots are printed hot, not dark)
    vec3 dotInk = mix(scene * vec3(0.8, 0.82, 0.9), vec3(1.0, 0.56, 0.16), min(1.0, glow * 1.6));
    scene = mix(scene, dotInk, dotk * (0.55 + 0.3 * min(1.0, glow * 1.6)));
    // the ink
    int reach = max(1, int(floor(uScale * 0.75 + 0.5)));
    float ink = edge(px, reach);
    scene = mix(scene, mix(INK, INK * 0.6, uNight), ink * 0.95);
    outColor = vec4(scene, 1.0);
}
