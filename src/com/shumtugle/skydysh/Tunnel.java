package com.shumtugle.skydysh;

/**
 * The tunnel through the island: in at the foot of the rock by the pier's foot, straight through
 * under the lighthouse, rising evenly all the way, out at the foot of the rock on the far side
 * onto the middle ledge. Where it is in the world it is: the island stands over it, those after
 * her follow her through it. In it she walks on its floor, between its walls, under its roof,
 * and the eye stays in it with her.
 *
 * Its frame: the middle of its mouth by the pier (ex, ez), the floor there (y0); the way in
 * (ux, uz) and to the right (vx, vz); how long, the floor at its far mouth (y1).
 */
final class Tunnel implements Ground {
    // how wide, half; how high, floor to roof; how far out of its mouths the eye may be
    static final float HALF = 2.4f, HIGH = 5f, EYE_OUT = 9f;
    // the rock face it goes in at (the profile's ring), the one it comes out at on the far side
    static final int IN_FACE = 5, OUT_FACE = 9;
    // its heading: the middle of the slopes' sector nearest the pier, so its mouths come where one
    // of the rock's pieces is left out for each
    static final int SECTOR = 11;

    final float ex, ez, ux, uz, vx, vz, len, y0, y1;

    Tunnel() {
        float a = Island.sectorA(SECTOR), b = a + (float) Math.PI;
        float sa = (float) Math.sin(a), ca = (float) Math.cos(a);
        float rin = Island.ring(IN_FACE - 1) * Island.wob(a), rout = Island.ring(OUT_FACE - 1) * Island.wob(b);
        ex = Island.IX + sa * rin;
        ez = Island.IZ + ca * rin;
        float xx = Island.IX + (float) Math.sin(b) * rout, xz = Island.IZ + (float) Math.cos(b) * rout;
        float dx = xx - ex, dz = xz - ez;
        len = (float) Math.sqrt(dx * dx + dz * dz);
        ux = dx / len;
        uz = dz / len;
        vx = uz;
        vz = -ux;
        y0 = Island.height(ex, ez);
        y1 = Island.height(xx, xz);
    }

    /** How far in from the pier's mouth (x, z) is, along it. */
    float along(float x, float z) {
        return (x - ex) * ux + (z - ez) * uz;
    }

    /** How far off its middle (x, z) is, to the right. */
    float across(float x, float z) {
        return (x - ex) * vx + (z - ez) * vz;
    }

    /** The floor so far in (before the mouth and past the far one, as at the mouth). */
    float floor(float s) {
        return y0 + (y1 - y0) * Math.max(0f, Math.min(1f, s / len));
    }

    /** Whether one at (x, y, z) is in it: between its mouths and its walls, down at its floor. */
    boolean holds(float x, float y, float z) {
        float s = along(x, z);
        if (s < 0f || s > len) return false;
        if (Math.abs(across(x, z)) > HALF) return false;
        return y < floor(s) + 3f;
    }

    /** A point so far in, so far to the right, into out (x, z). */
    void at(float s, float c, float[] out) {
        out[0] = ex + ux * s + vx * c;
        out[1] = ez + uz * s + vz * c;
    }

    @Override
    public float ground(float x, float z, float feet, float reach) {
        return floor(along(x, z));
    }

    @Override
    public boolean push(float[] p, float r, float feet) {
        float c = across(p[0], p[1]), lim = HALF - r;
        if (Math.abs(c) <= lim) return false;
        float m = (c > 0f ? lim : -lim) - c;
        p[0] += vx * m;
        p[1] += vz * m;
        return true;
    }

    @Override
    public float ceiling(float x, float z, float feet) {
        return floor(along(x, z)) + HIGH;
    }

    @Override
    public void keepEye(float[] p) {
        push(p, 0.3f, 0f);
        float s = along(p[0], p[1]), m = 0f;
        if (s < -EYE_OUT) m = -EYE_OUT - s;
        else if (s > len + EYE_OUT) m = len + EYE_OUT - s;
        p[0] += ux * m;
        p[1] += uz * m;
    }
}
