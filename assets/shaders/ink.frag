#version 300 es
// The drawn scene, finished, at the size it was drawn: the hour's grade, a soft bloom where it is
// brightest, the sea haze a little warmer over the far half, and the glow round what burns laid
// on as light. The screen pass scales it up and lays the rest over it.
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

float near = 0.1, far = 500.0;

float lin(float d) {
    float z = d * 2.0 - 1.0;
    return 2.0 * near * far / (far + near - z * (far - near));
}

// how far a colour is over the bright end, the part that blooms
vec3 over(vec3 c) {
    return max(c - 0.78, 0.0);
}

void main() {
    ivec2 px = ivec2(gl_FragCoord.xy);
    ivec2 size = textureSize(uScene, 0);
    vec3 scene = texelFetch(uScene, px, 0).rgb;
    // a soft bloom: the bright part of the pixels round about, spread
    vec3 bloom = vec3(0.0);
    float r = max(2.0, uScale * 3.0);
    for (int i = 0; i < 8; i++) {
        float a = float(i) * 0.785398 + 0.39;
        ivec2 q = clamp(px + ivec2(round(vec2(cos(a), sin(a)) * r)), ivec2(0), size - 1);
        ivec2 q2 = clamp(px + ivec2(round(vec2(cos(a + 0.39), sin(a + 0.39)) * r * 2.2)), ivec2(0), size - 1);
        bloom += over(texelFetch(uScene, q, 0).rgb) + over(texelFetch(uScene, q2, 0).rgb) * 0.6;
    }
    scene += bloom * 0.09 * vec3(1.0, 0.92, 0.82);
    // the hour of the day: colours richer and warmer toward sunset
    if (uGrade != vec3(1.0, 1.0, 0.0)) {
        float l0 = dot(scene, vec3(0.299, 0.587, 0.114));
        scene = mix(vec3(l0), scene, uGrade.y * 1.08);
        scene = clamp((scene - 0.45) * mix(1.0, uGrade.x, 0.6) + 0.45, 0.0, 1.0);
        scene *= vec3(1.0 + 0.05 * uGrade.z, 1.0 + 0.01 * uGrade.z, 1.0 - 0.06 * uGrade.z);
    }
    // the shadows a touch cooler, the lights a touch warmer
    float L = dot(scene, vec3(0.299, 0.587, 0.114));
    scene *= mix(vec3(0.94, 0.98, 1.06), vec3(1.04, 1.0, 0.95), smoothstep(0.25, 0.75, L));
    // the glow round what burns: warm light laid over, softly
    if (uHalo.w > 0.0) {
        vec2 fp = vec2(gl_FragCoord.x, float(size.y) - gl_FragCoord.y) / uScale;
        vec2 q = (fp - uHalo.xy) / vec2(uHalo.z, uHalo.z * 1.5);
        float r2 = dot(q, q);
        if (r2 < 4.0 && lin(texelFetch(uDepth, px, 0).r) > uHaloDepth - 1.5) {
            float glow = exp(-r2 * 1.3) * uHalo.w;
            scene = 1.0 - (1.0 - scene) * (1.0 - vec3(1.0, 0.5, 0.16) * glow * 0.55);
        }
    }
    // the corners a little darker
    vec2 uv = gl_FragCoord.xy / vec2(size);
    float vig = 1.0 - 0.18 * dot(uv - 0.5, uv - 0.5) * 2.0;
    outColor = vec4(clamp(scene * vig, 0.0, 1.0), 1.0);
}
