package com.shumtugle.skydysh;

import java.util.ArrayList;
import java.util.Random;

/**
 * Where brums went off in the town: the ground torn round the spot, a black hole of a crater,
 * cracks running out of it, black and lit violet from within as the jet's light was; where a crack
 * runs into a house it goes on up its wall. They stay: kept between games, so many at most (the
 * oldest goes when a new one comes past that).
 */
final class Scars {
    static final int MOST = 40;
    static final String SAVE = "scars", SAVE_AT = "scar";
    // how far off the eye they are drawn
    static final float SEEN = 150f;
    // the cracks: how many out of a spot, how long, in steps of so long
    static final int RAYS = 8;
    static final float RAY_FROM = 6f, RAY_TO = 13f, STEP_FROM = 1.1f, STEP_TO = 2f;
    // a crack up a wall: how high
    static final float UP_FROM = 3f, UP_TO = 9f;
    static final int DARK = 0x0E0B14, VIOLET = 0xA25BFF, DEEP = 0x5A2AA8, RUBBLE = 0x3A3540;

    private final float[] x = new float[MOST], z = new float[MOST];
    private final int[] packed = new int[MOST];
    private final ArrayList<Piece>[] built;
    private int n;

    @SuppressWarnings("unchecked")
    Scars() {
        built = new ArrayList[MOST];
    }

    /** The kept spots, from the save. */
    void load(World.Host host) {
        n = 0;
        int m = Math.min(MOST, Math.max(0, host.loadInt(SAVE)));
        for (int k = 0; k < m; k++) put(host.loadInt(SAVE_AT + k));
    }

    /** A new spot at (sx, sz), kept; the oldest dropped if there are too many. */
    void add(World.Host host, float sx, float sz) {
        if (Island.off(sx, sz)) return;
        // one close by already: the same spot, torn again
        for (int k = 0; k < n; k++) if ((x[k] - sx) * (x[k] - sx) + (z[k] - sz) * (z[k] - sz) < 9f) return;
        int p = ((Math.round(sx) + 32768) & 0xFFFF) << 16 | (Math.round(sz) + 32768) & 0xFFFF;
        if (n == MOST) {
            System.arraycopy(x, 1, x, 0, MOST - 1);
            System.arraycopy(z, 1, z, 0, MOST - 1);
            System.arraycopy(packed, 1, packed, 0, MOST - 1);
            System.arraycopy(built, 1, built, 0, MOST - 1);
            n--;
        }
        put(p);
        host.saveInt(SAVE, n);
        for (int k = 0; k < n; k++) host.saveInt(SAVE_AT + k, packed[k]);
    }

    private void put(int p) {
        packed[n] = p;
        x[n] = ((p >>> 16) & 0xFFFF) - 32768;
        z[n] = (p & 0xFFFF) - 32768;
        built[n] = null;
        n++;
    }

    int count() {
        return n;
    }

    void draw(DrawList d, City city, float ex, float ez) {
        for (int k = 0; k < n; k++) {
            if ((x[k] - ex) * (x[k] - ex) + (z[k] - ez) * (z[k] - ez) > SEEN * SEEN) continue;
            if (built[k] == null) built[k] = build(city, x[k], z[k], packed[k]);
            ArrayList<Piece> b = built[k];
            for (int q = 0; q < b.size(); q++) {
                Piece p = b.get(q);
                d.box(p.x, p.y, p.z, p.hx, p.hy, p.hz, p.t, p.color, p.material);
            }
        }
    }

    /** A box of one: where, its half sizes, how turned, its color and stuff. */
    private static final class Piece {
        float x, y, z, hx, hy, hz;
        float[] t;
        int color, material;
    }

    // ---- the making of one ----

    private final float[] wall = new float[5];

    private ArrayList<Piece> build(City city, float cx, float cz, int seed) {
        ArrayList<Piece> out = new ArrayList<>();
        Random r = new Random(seed * 2654435761L);
        float cy = city.ground(cx, cz);
        // the crater: black plates over each other, turned every way, a violet glow at the bottom
        for (int k = 0; k < 4; k++) {
            float a = r.nextFloat() * 3.1416f, s = 1.1f + r.nextFloat() * 0.8f;
            flat(out, cx + (r.nextFloat() - 0.5f) * 0.8f, cy + 0.03f + k * 0.004f, cz + (r.nextFloat() - 0.5f) * 0.8f, s, s * (0.6f + r.nextFloat() * 0.4f), a, DARK, DrawList.PAINT);
        }
        flat(out, cx, cy + 0.05f, cz, 0.35f, 0.3f, r.nextFloat() * 3.1416f, VIOLET, DrawList.GLOW);
        // rubble thrown round it
        for (int k = 0; k < 9; k++) {
            float a = r.nextFloat() * 6.2832f, f = 1.8f + r.nextFloat() * 3.5f, s = 0.08f + r.nextFloat() * 0.16f;
            float px = cx + (float) Math.sin(a) * f, pz = cz + (float) Math.cos(a) * f;
            if (city.wallAt(px, pz, cy, wall)) continue;
            float[] t = yaw(r.nextFloat() * 6.2832f);
            add(out, px, city.ground(px, pz) + s, pz, s * 1.3f, s, s, t, RUBBLE, DrawList.STONE);
        }
        // the cracks out of it
        float a0 = r.nextFloat() * 6.2832f;
        for (int k = 0; k < RAYS; k++) {
            float a = a0 + k * 6.2832f / RAYS + (r.nextFloat() - 0.5f) * 0.5f;
            float far = RAY_FROM + r.nextFloat() * (RAY_TO - RAY_FROM);
            crack(out, city, r, cx + (float) Math.sin(a) * 1.2f, cy, cz + (float) Math.cos(a) * 1.2f, a, far, 0.26f, true);
        }
        return out;
    }

    /** A crack over the ground from (px, pz) heading a so far, narrowing; branching once maybe; up a wall it meets. */
    private void crack(ArrayList<Piece> out, City city, Random r, float px, float feet, float pz, float a, float far, float wide, boolean branch) {
        float py = city.ground(px, pz);
        float went = 0f;
        boolean forked = !branch;
        while (went < far) {
            float step = STEP_FROM + r.nextFloat() * (STEP_TO - STEP_FROM);
            a += (r.nextFloat() - 0.5f) * 0.9f;
            float qx = px + (float) Math.sin(a) * step, qz = pz + (float) Math.cos(a) * step;
            // the sea: no further
            if (qz < City.SHORE * City.CELL + City.WATERLINE) return;
            if (city.wallAt(qx, qz, feet, wall)) {
                up(out, r, px, py, pz, qx, qz, wide);
                return;
            }
            float qy = city.ground(qx, qz);
            if (Math.abs(qy - py) > 0.6f) return;
            float w = wide * (1f - went / far * 0.75f);
            seg(out, px, py + 0.035f, pz, qx, qy + 0.035f, qz, 0f, 1f, 0f, w, DARK, DrawList.PAINT);
            seg(out, px, py + 0.05f, pz, qx, qy + 0.05f, qz, 0f, 1f, 0f, w * 0.35f, went < far * 0.5f ? VIOLET : DEEP, DrawList.GLOW);
            px = qx;
            py = qy;
            pz = qz;
            went += step;
            if (!forked && went > far * 0.35f && r.nextFloat() < 0.5f) {
                forked = true;
                crack(out, city, r, px, feet, pz, a + (r.nextBoolean() ? 0.7f : -0.7f), (far - went) * 0.7f, w * 0.7f, false);
            }
        }
    }

    /** Where the crack from (px, pz) toward (qx, qz) meets the wall: up its face, zigzag. */
    private void up(ArrayList<Piece> out, Random r, float px, float py, float pz, float qx, float qz, float wide) {
        // the face it came at: whichever side of the box (px, pz) is out past
        float nx = 0f, nz = 0f, fx = px, fz = pz;
        float out0 = wall[0] - px, out1 = px - wall[2], out2 = wall[1] - pz, out3 = pz - wall[3];
        float most = Math.max(Math.max(out0, out1), Math.max(out2, out3));
        if (most == out0) { nx = -1f; fx = wall[0]; fz = clamp(pz + (qz - pz) * 0.5f, wall[1], wall[3]); }
        else if (most == out1) { nx = 1f; fx = wall[2]; fz = clamp(pz + (qz - pz) * 0.5f, wall[1], wall[3]); }
        else if (most == out2) { nz = -1f; fz = wall[1]; fx = clamp(px + (qx - px) * 0.5f, wall[0], wall[2]); }
        else { nz = 1f; fz = wall[3]; fx = clamp(px + (qx - px) * 0.5f, wall[0], wall[2]); }
        fx += nx * 0.04f;
        fz += nz * 0.04f;
        // along the face, sideways: the one across its normal
        float sx = -nz, sz = nx;
        float high = Math.min(wall[4] - py - 0.5f, UP_FROM + r.nextFloat() * (UP_TO - UP_FROM));
        float y = py, h = 0f;
        float w = wide;
        while (h < high) {
            float step = 0.8f + r.nextFloat() * 1.1f, side = (r.nextFloat() - 0.5f) * 1.2f;
            float gx = fx + sx * side, gz = fz + sz * side, gy = y + step;
            seg(out, fx, y, fz, gx, gy, gz, nx, 0f, nz, w, DARK, DrawList.PAINT);
            seg(out, fx + nx * 0.02f, y, fz + nz * 0.02f, gx + nx * 0.02f, gy, gz + nz * 0.02f, nx, 0f, nz, w * 0.35f, VIOLET, DrawList.GLOW);
            fx = gx;
            fz = gz;
            y = gy;
            h += step;
            w *= 0.85f;
        }
    }

    private static float clamp(float v, float lo, float hi) {
        return v < lo ? lo : v > hi ? hi : v;
    }

    /** A flat plate on the ground: half sizes across and along, turned a. */
    private static void flat(ArrayList<Piece> out, float x, float y, float z, float across, float along, float a, int color, int material) {
        add(out, x, y, z, across, 0.015f, along, yaw(a), color, material);
    }

    /** A strip from a to b, so wide, lying on the face whose normal is n. */
    private static void seg(ArrayList<Piece> out, float ax, float ay, float az, float bx, float by, float bz, float nx, float ny, float nz, float wide, int color, int material) {
        float zx = bx - ax, zy = by - ay, zz = bz - az, len = (float) Math.sqrt(zx * zx + zy * zy + zz * zz);
        if (len < 1e-3f) return;
        zx /= len;
        zy /= len;
        zz /= len;
        // the normal made square to the strip
        float dn = nx * zx + ny * zy + nz * zz;
        float yx = nx - dn * zx, yy = ny - dn * zy, yz = nz - dn * zz, yl = (float) Math.sqrt(yx * yx + yy * yy + yz * yz);
        if (yl < 1e-3f) return;
        yx /= yl;
        yy /= yl;
        yz /= yl;
        float xx = yy * zz - yz * zy, xy = yz * zx - yx * zz, xz = yx * zy - yy * zx;
        float[] t = {xx, yx, zx, xy, yy, zy, xz, yz, zz};
        add(out, (ax + bx) * 0.5f, (ay + by) * 0.5f, (az + bz) * 0.5f, wide * 0.5f, 0.015f, len * 0.5f + wide * 0.3f, t, color, material);
    }

    private static void add(ArrayList<Piece> out, float x, float y, float z, float hx, float hy, float hz, float[] t, int color, int material) {
        Piece p = new Piece();
        p.x = x;
        p.y = y;
        p.z = z;
        p.hx = hx;
        p.hy = hy;
        p.hz = hz;
        p.t = t;
        p.color = color;
        p.material = material;
        out.add(p);
    }

    private static float[] yaw(float a) {
        float c = (float) Math.cos(a), s = (float) Math.sin(a);
        return new float[] {c, 0f, s, 0f, 1f, 0f, -s, 0f, c};
    }
}
