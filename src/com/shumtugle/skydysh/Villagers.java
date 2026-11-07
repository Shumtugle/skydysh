package com.shumtugle.skydysh;

import java.util.Random;

/**
 * The village's own people, about its square: some walk to and fro across it and along the road
 * in, some stand in twos talking, one draws water at the well, a few sit on the benches by the
 * houses. They are not manikins: one she comes up to stops and turns to look at her, and goes on
 * once she has gone by; those talking turn their heads to her as she passes.
 */
final class Villagers {
    static final int MANY = 18;
    // what one does: to and fro between two places; standing (talking, at the well); sitting on a bench
    static final int STROLL = 0, STAND = 1, SIT = 2;
    // how fast they walk; how near she is when they stop for her, and when they go on again
    static final float PACE = 1.15f, NOTICE = 5.5f, LET_GO = 8f;
    // a bench's seat over the ground; the hips over the feet on the skeleton
    static final float SEAT = 0.5f, HIPS = 0.93f;
    // seen only so near
    static final float NEAR = 230f;

    private final int[] mode = new int[MANY], model = new int[MANY];
    private final Stroll.Way[] way = new Stroll.Way[MANY];
    private final float[] x = new float[MANY], z = new float[MANY], yaw = new float[MANY], face = new float[MANY];
    private final float[] ax = new float[MANY], az = new float[MANY], bx = new float[MANY], bz = new float[MANY];
    private final float[] walked = new float[MANY], walking = new float[MANY], rest = new float[MANY];
    private final boolean[] toB = new boolean[MANY], stopped = new boolean[MANY];
    // the benches: where (x, z), the way one sitting on it faces
    private final float[] bench = new float[3 * 4];
    private int benches;
    private boolean dressed;

    Villagers() {
        Random rnd = new Random(0x7111A6EL);
        float sx = City.villageX(), sz = City.VILLAGE_Z, q = City.SQUARE_HALF;
        // four benches, at the square's corners, facing its middle
        for (int k = 0; k < 4; k++) {
            float cx = sx + ((k & 1) == 0 ? -1f : 1f) * (q - 2.2f), cz = sz + ((k & 2) == 0 ? -1f : 1f) * (q - 2.2f);
            bench[k * 3] = cx;
            bench[k * 3 + 1] = cz;
            bench[k * 3 + 2] = (float) Math.atan2(sx - cx, sz - cz);
        }
        benches = 4;
        int n = 0;
        // to and fro across the square, and along the road in from the south
        for (int k = 0; k < 7; k++, n++) {
            mode[n] = STROLL;
            float a = rnd.nextFloat() * 6.283f, r = q * (0.5f + rnd.nextFloat() * 0.35f);
            ax[n] = sx + (float) Math.sin(a) * r;
            az[n] = sz + (float) Math.cos(a) * r;
            bx[n] = sx - (float) Math.sin(a + 0.6f) * r;
            bz[n] = sz - (float) Math.cos(a + 0.6f) * r;
            if (k >= 5) {
                // along the road in
                float z0 = sz - q - 6f - rnd.nextFloat() * 10f;
                ax[n] = City.routeX(z0) + (k == 5 ? -3.2f : 3.2f);
                az[n] = z0;
                bx[n] = City.routeX(z0 - 34f) + (k == 5 ? -3.2f : 3.2f);
                bz[n] = z0 - 34f;
            }
            float u = rnd.nextFloat();
            x[n] = ax[n] + (bx[n] - ax[n]) * u;
            z[n] = az[n] + (bz[n] - az[n]) * u;
            toB[n] = rnd.nextBoolean();
        }
        // in twos, talking, here and there on the square
        for (int k = 0; k < 3; k++) {
            float a = rnd.nextFloat() * 6.283f, r = q * (0.3f + rnd.nextFloat() * 0.4f);
            float cx = sx + (float) Math.sin(a) * r, cz = sz + (float) Math.cos(a) * r, t = rnd.nextFloat() * 6.283f;
            for (int e = -1; e <= 1; e += 2, n++) {
                mode[n] = STAND;
                x[n] = cx + (float) Math.sin(t) * 0.55f * e;
                z[n] = cz + (float) Math.cos(t) * 0.55f * e;
                yaw[n] = (float) Math.atan2(cx - x[n], cz - z[n]);
            }
        }
        // at the well
        mode[n] = STAND;
        x[n] = sx + 1.6f;
        z[n] = sz - 0.4f;
        yaw[n] = (float) Math.atan2(sx - x[n], sz - z[n]);
        n++;
        // on the benches
        for (int k = 0; k < 4 && n < MANY; k++, n++) {
            mode[n] = SIT;
            x[n] = bench[k * 3];
            z[n] = bench[k * 3 + 1];
            yaw[n] = bench[k * 3 + 2];
        }
        for (int i = 0; i < MANY; i++) {
            face[i] = yaw[i];
            rest[i] = rnd.nextFloat() * 3f;
            model[i] = rnd.nextInt(1 << 16);
        }
    }

    /** Dressed once the town's people are there to dress them from. */
    private void dress() {
        if (dressed || FolkPack.town == null) return;
        dressed = true;
        Random rnd = new Random(0xD8E55L);
        for (int i = 0; i < MANY; i++) {
            model[i] = model[i] % FolkPack.town.count;
            way[i] = Stroll.way(rnd.nextLong(), FolkPack.town.woman(model[i]));
        }
    }

    /** A step on: the walkers along their ways, all of them minding her (at rx, rz). */
    void update(float dt, float rx, float rz) {
        float sx = City.villageX(), sz = City.VILLAGE_Z;
        if ((rx - sx) * (rx - sx) + (rz - sz) * (rz - sz) > NEAR * NEAR) return;
        dress();
        for (int i = 0; i < MANY; i++) {
            float dx = rx - x[i], dz = rz - z[i], dd = dx * dx + dz * dz;
            float toHer = (float) Math.atan2(dx, dz);
            if (mode[i] == STROLL) {
                // she comes up: stop and look; she goes on: so do they
                if (dd < NOTICE * NOTICE) stopped[i] = true;
                else if (dd > LET_GO * LET_GO) stopped[i] = false;
                if (stopped[i]) {
                    walking[i] = Math.max(0f, walking[i] - dt * 3f);
                    yaw[i] = turn(yaw[i], toHer, dt * 3f);
                    continue;
                }
                if (rest[i] > 0f) {
                    rest[i] -= dt;
                    walking[i] = Math.max(0f, walking[i] - dt * 3f);
                    continue;
                }
                float tx = toB[i] ? bx[i] : ax[i], tz = toB[i] ? bz[i] : az[i];
                float ex = tx - x[i], ez = tz - z[i], l = (float) Math.sqrt(ex * ex + ez * ez);
                if (l < 0.4f) {
                    toB[i] = !toB[i];
                    rest[i] = 1f + (i * 0.77f) % 2.5f;
                    continue;
                }
                walking[i] = Math.min(1f, walking[i] + dt * 3f);
                float step = PACE * walking[i] * dt;
                x[i] += ex / l * step;
                z[i] += ez / l * step;
                walked[i] += step;
                yaw[i] = turn(yaw[i], (float) Math.atan2(ex, ez), dt * 5f);
            } else {
                // standing or sitting: the head (the whole of one standing) turns to her as she passes
                float want = dd < NOTICE * NOTICE * 1.4f ? toHer : yaw[i];
                face[i] = turn(face[i], mode[i] == SIT ? clampTo(yaw[i], want, 0.9f) : want, dt * 2.5f);
            }
        }
    }

    private static float turn(float from, float to, float most) {
        float d = (float) Math.atan2(Math.sin(to - from), Math.cos(to - from));
        return from + Math.max(-most, Math.min(most, d));
    }

    private static float clampTo(float about, float to, float most) {
        float d = (float) Math.atan2(Math.sin(to - about), Math.cos(to - about));
        return about + Math.max(-most, Math.min(most, d));
    }

    void draw(DrawList d, City city, float ex, float ez) {
        float sx = City.villageX(), sz = City.VILLAGE_Z;
        if ((ex - sx) * (ex - sx) + (ez - sz) * (ez - sz) > NEAR * NEAR) return;
        // the benches: a seat of boards on two legs of stone
        for (int k = 0; k < benches; k++) {
            float cx = bench[k * 3], cz = bench[k * 3 + 1], a = bench[k * 3 + 2];
            float c = (float) Math.cos(a), s = (float) Math.sin(a);
            float[] f = {c, 0f, s, 0f, 1f, 0f, -s, 0f, c};
            float gy = city.ground(cx, cz);
            d.box(cx, gy + SEAT - 0.04f, cz, 0.9f, 0.04f, 0.22f, f, 0x9A6E46, DrawList.WOOD);
            // the back, behind the one sitting
            d.box(cx - s * 0.24f, gy + SEAT + 0.3f, cz - c * 0.24f, 0.9f, 0.18f, 0.03f, f, 0x8E6440, DrawList.WOOD);
            for (int e = -1; e <= 1; e += 2)
                d.box(cx + c * e * 0.75f, gy + (SEAT - 0.08f) * 0.5f, cz - s * e * 0.75f, 0.08f, (SEAT - 0.08f) * 0.5f, 0.2f, f, 0xA89C88, DrawList.STONE);
        }
        if (!dressed || !Frida.ready()) return;
        for (int i = 0; i < MANY; i++) {
            int m = model[i];
            float gy = city.ground(x[i], z[i]);
            // the skeleton faces -z at no turn: turned half round, they face the way they look
            if (mode[i] == SIT) {
                float y = gy + SEAT - HIPS * FolkPack.town.size[m];
                FolkPack.sitter(d, m, x[i], y, z[i], yaw[i] + (float) Math.PI, 1f, 0.15f, face[i] - yaw[i]);
            } else if (mode[i] == STAND) {
                FolkPack.walker(d, m, x[i], gy, z[i], face[i] + (float) Math.PI, 1f, way[i], 0f, 0f);
            } else {
                FolkPack.walker(d, m, x[i], gy, z[i], yaw[i] + (float) Math.PI, 1f, way[i], walked[i], walking[i]);
            }
        }
    }
}
