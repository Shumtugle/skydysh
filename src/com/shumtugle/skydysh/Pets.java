package com.shumtugle.skydysh;

import java.util.Random;

/**
 * Dogs and cats about the pavements. A dog is walked on a lead at someone's side, or goes about
 * on its own, trotting round a block nearer the curb than the people do; a cat goes round close
 * to the house fronts, slowly, sitting down now and then to look about. Someone running at
 * them, a cat bolts on ahead and a dog trots off quicker a while. Like the people, they come and
 * go out of sight round the runner.
 *
 * Each is made of round parts, its legs swinging in pairs as it goes, its tail up or out.
 */
final class Pets {
    static final int DOG = 0, CAT = 1;
    static final int MOST = 16;
    // the rings round a block they keep to: so far in from each street's middle
    static final float DOG_RING = 5.5f, CAT_RING = 6.75f;
    static final float BORN_NEAR = 26f, BORN_FAR = 100f, GONE = 130f, SHY = 4.5f;
    private static final int[] DOGS = {0x8A5A34, 0xD9B482, 0x2B2622, 0xF1E9DA, 0x6B4A30, 0xB9A58C, 0x3E3A38};
    private static final int[] CATS = {0x3A3432, 0xE89A4E, 0xD8D2C8, 0x8A817A, 0xC98F5A, 0x24201E};

    private final Random rnd = new Random(0xD06CA7L);
    private final int[] kind = new int[MOST], coat = new int[MOST], patch = new int[MOST], owner = new int[MOST];
    private final int[] bi = new int[MOST], bj = new int[MOST];
    private final float[] u = new float[MOST], dir = new float[MOST], pace = new float[MOST], size = new float[MOST],
            ph = new float[MOST], sit = new float[MOST], fright = new float[MOST], walked = new float[MOST];
    final float[] x = new float[MOST], z = new float[MOST], yaw = new float[MOST];
    private final boolean[] live = new boolean[MOST];
    private boolean started;

    private static float side(float ring) {
        return City.CELL - 2f * ring;
    }

    void update(float dt, Runner r, Walkers people) {
        for (int i = 0; i < MOST; i++) {
            float rx = x[i] - r.x, rz = z[i] - r.z;
            if (!live[i] || rx * rx + rz * rz > GONE * GONE || owner[i] >= 0 && !people.walking(owner[i])) {
                spawn(i, r, people, !started);
                continue;
            }
            float near = (float) Math.sqrt(rx * rx + rz * rz);
            if (near < SHY && r.speed > 2f) {
                fright[i] = kind[i] == CAT ? 2.2f : 1.4f;
                sit[i] = 0f;
            }
            fright[i] = Math.max(0f, fright[i] - dt);
            float px = x[i], pz = z[i];
            if (owner[i] >= 0) {
                // on the lead: at the walker's side, toward the road, a little ahead
                int o = owner[i];
                float fx = (float) Math.sin(people.yaw[o]), fz = (float) Math.cos(people.yaw[o]);
                float sx = people.outX[o], sz = people.outZ[o];
                x[i] = people.x[o] + sx * 0.75f + fx * 0.55f;
                z[i] = people.z[o] + sz * 0.75f + fz * 0.55f;
                yaw[i] = people.yaw[o];
            } else {
                float ring = kind[i] == CAT ? CAT_RING : DOG_RING;
                float len = side(ring) * 4f, v = pace[i];
                if (kind[i] == CAT) {
                    // a cat sits a while now and then, unless frightened
                    if (sit[i] > 0f) {
                        sit[i] -= dt;
                        v = 0f;
                    } else if (fright[i] <= 0f && rnd.nextInt(600) == 0) sit[i] = 2f + rnd.nextFloat() * 5f;
                }
                if (fright[i] > 0f) v = kind[i] == CAT ? 4.5f : 3.4f;
                u[i] = (u[i] + dir[i] * v * dt + len) % len;
                place(i, ring);
            }
            float dx = x[i] - px, dz = z[i] - pz;
            float moved = (float) Math.sqrt(dx * dx + dz * dz);
            walked[i] = moved / Math.max(dt, 1e-4f);
            ph[i] += moved / (0.42f * size[i]);
        }
        started = true;
    }

    /** Where one going round its block stands and which way it faces, from how far round it is. */
    private void place(int i, float ring) {
        float s = side(ring), w = u[i];
        int edge = (int) (w / s);
        float k = w - edge * s;
        float x0 = bi[i] * City.CELL + ring, z0 = bj[i] * City.CELL + ring;
        float px, pz, ax, az;
        if (edge == 0) { px = x0 + k; pz = z0; ax = 1f; az = 0f; }
        else if (edge == 1) { px = x0 + s; pz = z0 + k; ax = 0f; az = 1f; }
        else if (edge == 2) { px = x0 + s - k; pz = z0 + s; ax = -1f; az = 0f; }
        else { px = x0; pz = z0 + s - k; ax = 0f; az = -1f; }
        x[i] = px;
        z[i] = pz;
        yaw[i] = (float) Math.atan2(ax * dir[i], az * dir[i]);
    }

    /** One put round the runner, out of her sight (or anywhere, the first time): a dog on a lead, a stray, a cat. */
    private void spawn(int i, Runner r, Walkers people, boolean anywhere) {
        live[i] = false;
        int roll = rnd.nextInt(10);
        kind[i] = roll < 6 ? DOG : CAT;
        owner[i] = -1;
        if (kind[i] == DOG && roll < 4) {
            // walked by someone not walking one yet
            int o = people.free(rnd, this);
            if (o >= 0) {
                float dx = people.x[o] - r.x, dz = people.z[o] - r.z;
                if (anywhere || dx * dx + dz * dz > BORN_NEAR * BORN_NEAR) {
                    owner[i] = o;
                    looks(i);
                    x[i] = people.x[o];
                    z[i] = people.z[o];
                    live[i] = true;
                    return;
                }
            }
        }
        float ring = kind[i] == CAT ? CAT_RING : DOG_RING;
        for (int tries = 0; tries < 20; tries++) {
            bi[i] = City.index(r.x) + rnd.nextInt(5) - 2;
            bj[i] = City.index(r.z) + rnd.nextInt(5) - 2;
            u[i] = rnd.nextFloat() * side(ring) * 4f;
            dir[i] = rnd.nextBoolean() ? 1f : -1f;
            place(i, ring);
            float dx = x[i] - r.x, dz = z[i] - r.z, dd = (float) Math.sqrt(dx * dx + dz * dz);
            if (dd < (anywhere ? 8f : BORN_NEAR) || dd > BORN_FAR) continue;
            float fx = (float) Math.sin(r.eyeYaw), fz = (float) Math.cos(r.eyeYaw);
            if (!anywhere && (dx * fx + dz * fz) / dd > 0.7f && dd < 60f) continue;
            looks(i);
            pace[i] = kind[i] == CAT ? 0.7f + rnd.nextFloat() * 0.4f : 1.6f + rnd.nextFloat() * 0.8f;
            live[i] = true;
            return;
        }
    }

    private void looks(int i) {
        boolean dog = kind[i] == DOG;
        coat[i] = dog ? DOGS[rnd.nextInt(DOGS.length)] : CATS[rnd.nextInt(CATS.length)];
        patch[i] = rnd.nextInt(3) == 0 ? (dog ? 0xF1E9DA : 0xF4EFE6) : coat[i];
        size[i] = dog ? 0.65f + rnd.nextFloat() * 0.6f : 0.9f + rnd.nextFloat() * 0.2f;
        ph[i] = rnd.nextFloat() * 10f;
        sit[i] = 0f;
        fright[i] = 0f;
    }

    /** The one nearest (px, pz) of a kind (-1, any), as its place here; -1 if none is about. */
    int nearest(float px, float pz, int of) {
        int best = -1;
        float bd = 1e18f;
        for (int i = 0; i < MOST; i++) {
            if (!live[i] || of >= 0 && kind[i] != of) continue;
            float dx = x[i] - px, dz = z[i] - pz, dd = dx * dx + dz * dz;
            if (dd < bd) {
                bd = dd;
                best = i;
            }
        }
        return best;
    }

    boolean alive(int i) {
        return live[i];
    }

    int kindOf(int i) {
        return kind[i];
    }

    /** Whether one is on a lead. */
    boolean led(int i) {
        return owner[i] >= 0;
    }

    /** Whether person o is walking one of them on a lead. */
    boolean leads(int o) {
        for (int i = 0; i < MOST; i++) if (live[i] && owner[i] == o) return true;
        return false;
    }

    // ---- drawing ----

    private float px, py, pz, c, sn, s;
    private final float[] rot = new float[9], a = new float[3], b = new float[3];

    void draw(DrawList d, float ex, float ez, float fx, float fz, Walkers people) {
        for (int i = 0; i < MOST; i++) {
            if (!live[i]) continue;
            float dx = x[i] - ex, dz = z[i] - ez;
            if (dx * fx + dz * fz < -2f || dx * dx + dz * dz > 90f * 90f) continue;
            float go = Math.min(1f, walked[i] / 1.2f);
            if (kind[i] == DOG) dog(d, i, go);
            else cat(d, i, go);
            if (owner[i] >= 0) lead(d, i, people);
        }
    }

    private void begin(int i) {
        px = x[i];
        py = City.WALK_TOP;
        pz = z[i];
        c = (float) Math.cos(yaw[i]);
        sn = (float) Math.sin(yaw[i]);
        s = size[i];
    }

    /** A dog: a deep body, a head with a muzzle and ears, four legs trotting, the tail up. */
    private void dog(DrawList d, int i, float go) {
        begin(i);
        int k = coat[i];
        float sw = (float) Math.sin(ph[i] * 6.283f) * go, bob = Math.abs(sw) * 0.015f;
        float hip = 0.42f + bob;
        ball(d, 0f, hip, 0f, 0.13f, 0.13f, 0.3f, k);
        ball(d, 0f, hip - 0.04f, 0.08f, 0.11f, 0.1f, 0.16f, patch[i]);
        // the head, up and forward, the muzzle on it, the ears
        float hy = hip + 0.17f, hz = 0.36f;
        ball(d, 0f, hy, hz, 0.1f, 0.1f, 0.11f, k);
        ball(d, 0f, hy - 0.035f, hz + 0.11f, 0.055f, 0.05f, 0.08f, patch[i]);
        ball(d, 0f, hy - 0.01f, hz + 0.19f, 0.025f, 0.022f, 0.02f, 0x1A1614);
        for (int e = -1; e <= 1; e += 2) ball(d, e * 0.075f, hy + 0.04f, hz - 0.03f, 0.025f, 0.07f, 0.045f, Street.scale(k, 0.8f));
        // legs: the near pairs swinging against each other
        for (int e = -1; e <= 1; e += 2)
            for (int end = -1; end <= 1; end += 2) {
                float swing = sw * e * end * 0.13f, lx = e * 0.08f, lz = end * 0.21f;
                limb(d, lx, hip - 0.05f, lz, lx, 0.03f, lz + swing, 0.04f, k);
            }
        limb(d, 0f, hip + 0.04f, -0.28f, 0f, hip + 0.22f, -0.42f + sw * 0.03f, 0.03f, k);
    }

    /** A cat: a long low body, a round head with pointed ears, a tail raised in a curve; or sitting. */
    private void cat(DrawList d, int i, float go) {
        begin(i);
        int k = coat[i];
        boolean sitting = go < 0.05f && sit[i] > 0f;
        if (sitting) {
            ball(d, 0f, 0.13f, -0.04f, 0.09f, 0.12f, 0.11f, k);
            ball(d, 0f, 0.2f, 0.05f, 0.07f, 0.1f, 0.06f, patch[i]);
            ball(d, 0f, 0.33f, 0.07f, 0.075f, 0.07f, 0.07f, k);
            for (int e = -1; e <= 1; e += 2) ball(d, e * 0.045f, 0.4f, 0.07f, 0.018f, 0.035f, 0.015f, k);
            limb(d, 0.04f, 0.06f, -0.14f, 0.12f, 0.02f, 0.06f, 0.022f, k);
            return;
        }
        float sw = (float) Math.sin(ph[i] * 6.283f) * go;
        float hip = 0.2f;
        ball(d, 0f, hip, 0f, 0.075f, 0.075f, 0.2f, k);
        ball(d, 0f, hip - 0.025f, 0.05f, 0.06f, 0.05f, 0.1f, patch[i]);
        float hy = hip + 0.08f, hz = 0.24f;
        ball(d, 0f, hy, hz, 0.068f, 0.062f, 0.064f, k);
        for (int e = -1; e <= 1; e += 2) ball(d, e * 0.04f, hy + 0.065f, hz - 0.005f, 0.017f, 0.033f, 0.014f, k);
        for (int e = -1; e <= 1; e += 2)
            for (int end = -1; end <= 1; end += 2) {
                float swing = sw * e * end * 0.08f, lx = e * 0.045f, lz = end * 0.13f;
                limb(d, lx, hip - 0.03f, lz, lx, 0.02f, lz + swing, 0.022f, k);
            }
        limb(d, 0f, hip + 0.02f, -0.18f, 0f, hip + 0.12f, -0.3f, 0.018f, k);
        limb(d, 0f, hip + 0.12f, -0.3f, 0f, hip + 0.24f, -0.27f + sw * 0.02f, 0.018f, k);
    }

    /** The lead from the walker's hand down to the dog's collar. */
    private void lead(DrawList d, int i, Walkers people) {
        int o = owner[i];
        float fx = (float) Math.sin(people.yaw[o]), fz = (float) Math.cos(people.yaw[o]);
        float sx = people.outX[o], sz = people.outZ[o];
        float hx = people.x[o] + sx * 0.25f + fx * 0.1f, hz = people.z[o] + sz * 0.25f + fz * 0.1f, hy = City.WALK_TOP + 0.85f;
        float cx = x[i] + (float) Math.sin(yaw[i]) * 0.3f * size[i], cz = z[i] + (float) Math.cos(yaw[i]) * 0.3f * size[i];
        float cy = City.WALK_TOP + 0.55f * size[i];
        float ux = cx - hx, uy = cy - hy, uz = cz - hz, l = (float) Math.sqrt(ux * ux + uy * uy + uz * uz);
        if (l < 1e-3f) return;
        segment(d, hx, hy, hz, cx, cy, cz, 0.012f, 0xB0352C);
    }

    private void ball(DrawList d, float lx, float ly, float lz, float hx, float hy, float hz, int rgb) {
        rot[0] = c; rot[1] = 0f; rot[2] = sn;
        rot[3] = 0f; rot[4] = 1f; rot[5] = 0f;
        rot[6] = -sn; rot[7] = 0f; rot[8] = c;
        lx *= s;
        ly *= s;
        lz *= s;
        d.ellipsoid(px + c * lx + sn * lz, py + ly, pz - sn * lx + c * lz, hx * s, hy * s, hz * s, rot, rgb, DrawList.MATTE);
    }

    private void limb(DrawList d, float x0, float y0, float z0, float x1, float y1, float z1, float r, int rgb) {
        world(x0, y0, z0, a);
        world(x1, y1, z1, b);
        segment(d, a[0], a[1], a[2], b[0], b[1], b[2], r * s, rgb);
    }

    /** A round rod from one point to another in the world, so thick. */
    private void segment(DrawList d, float x0, float y0, float z0, float x1, float y1, float z1, float rr, int rgb) {
        float ux = x1 - x0, uy = y1 - y0, uz = z1 - z0;
        float len = (float) Math.sqrt(ux * ux + uy * uy + uz * uz);
        if (len < 1e-4f) return;
        ux /= len;
        uy /= len;
        uz /= len;
        // a way square to it: the level one across, unless it is upright
        float rx = -uz, ry = 0f, rz = ux;
        float rl = (float) Math.sqrt(rx * rx + rz * rz);
        if (rl < 1e-3f) {
            rx = 1f;
            rz = 0f;
            rl = 1f;
        }
        rx /= rl;
        rz /= rl;
        float fx = ry * uz - rz * uy, fy = rz * ux - rx * uz, fz = rx * uy - ry * ux;
        rot[0] = rx; rot[1] = ux; rot[2] = fx;
        rot[3] = ry; rot[4] = uy; rot[5] = fy;
        rot[6] = rz; rot[7] = uz; rot[8] = fz;
        d.ellipsoid((x0 + x1) * 0.5f, (y0 + y1) * 0.5f, (z0 + z1) * 0.5f, rr, len * 0.5f + rr * 0.6f, rr, rot, rgb, DrawList.MATTE);
    }

    private void world(float lx, float ly, float lz, float[] out) {
        lx *= s;
        ly *= s;
        lz *= s;
        out[0] = px + c * lx + sn * lz;
        out[1] = py + ly;
        out[2] = pz - sn * lx + c * lz;
    }
}
