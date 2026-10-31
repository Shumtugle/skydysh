#version 300 es
// A person: a mesh in the skeleton's rest pose, every corner moved by the bones it hangs on
// (each bone a turn and a shift from rest to the pose now, weighted), then sized, turned about
// the upright and stood in the town. Built with SHADOW defined, only the place seen from the sun.
layout(location = 0) in vec3 aPos;
layout(location = 1) in vec3 aNormal;
layout(location = 2) in vec2 aUV;
layout(location = 3) in vec4 aBones;
layout(location = 4) in vec4 aWeights;

uniform mat4 uViewProj;
uniform mat4 uLight;
uniform vec4 uBoneQ[23];
uniform vec3 uBoneT[23];
uniform vec4 uPlace;          // where it stands (x, y, z) and its turn about the upright
uniform float uSize;

out vec3 vWorld;
out vec3 vNormal;
out vec2 vUV;
out vec4 vLightPos;

vec3 turned(vec4 q, vec3 v) {
    return v + 2.0 * cross(q.xyz, cross(q.xyz, v) + q.w * v);
}

void main() {
    vec3 p = vec3(0.0), n = vec3(0.0);
    for (int k = 0; k < 4; k++) {
        int b = int(aBones[k] + 0.5);
        float w = aWeights[k];
        p += w * (turned(uBoneQ[b], aPos) + uBoneT[b]);
        n += w * turned(uBoneQ[b], aNormal);
    }
    p *= uSize;
    float c = cos(uPlace.w), s = sin(uPlace.w);
    vec3 world = vec3(uPlace.x + c * p.x + s * p.z, uPlace.y + p.y, uPlace.z - s * p.x + c * p.z);
    vec3 normal = normalize(vec3(c * n.x + s * n.z, n.y, -s * n.x + c * n.z));
#ifdef SHADOW
    gl_Position = uLight * vec4(world, 1.0);
#else
    vWorld = world;
    vNormal = normal;
    vUV = aUV;
    vLightPos = uLight * vec4(world + normal * 0.04, 1.0);
    gl_Position = uViewProj * vec4(world, 1.0);
#endif
}
