#version 300 es
// A person's skin and clothes: the colour from the shared picture, lit as the boxes are (the open
// sky from above, the warm ground from below, the low sun where it reaches, its shadows soft), in
// the same haze and dark. The alpha carries which person it is, so each is inked round its edge.
precision highp float;
precision highp sampler2DShadow;

in vec3 vWorld;
in vec3 vNormal;
in vec2 vUV;
in vec4 vLightPos;

uniform sampler2DShadow uShadow;
uniform sampler2D uPicture;
uniform float uShadowTexel;
uniform vec3 uSun;
uniform vec3 uSunColor;
uniform vec3 uAmbSky;
uniform vec3 uAmbGround;
uniform vec3 uEye;
uniform vec3 uFog;
uniform float uFogStart;
uniform float uFogScale;
uniform vec4 uSpot;
uniform float uDark;
uniform float uIdent;         // which person, as the alpha (never 0, which is the sky)

out vec4 outColor;

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

void main() {
    vec3 base = texture(uPicture, vUV).rgb;
    vec3 n = normalize(vNormal);
    float sh = shadowAt(vLightPos);
    float light = max(dot(n, uSun), 0.0) * sh;
    float up = n.y * 0.5 + 0.5;
    vec3 amb = mix(vec3(0.46, 0.38, 0.34) + uAmbGround * 0.25, vec3(0.38, 0.45, 0.52) + uAmbSky * 0.33, up);
    vec3 sun = (vec3(0.5, 0.41, 0.29) + uSunColor * 0.36) * light;
    vec3 c = base * (amb + sun);
    float dist = length(vWorld - uEye);
    c = mix(c, uFog, 1.0 - exp(-max(dist - uFogStart, 0.0) / uFogScale));
    if (uDark > 0.0) {
        float r = length(vWorld - uSpot.xyz) / max(uSpot.w, 1e-3);
        c *= mix(1.0, (1.0 - smoothstep(0.55, 1.0, r)) * 1.15, uDark);
    }
    outColor = vec4(c, uIdent);
}
