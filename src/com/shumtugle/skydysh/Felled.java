package com.shumtugle.skydysh;

/**
 * Street lamps felled by the pickup she drives: each falls the way it was struck, about its foot,
 * quicker and quicker, and lies across the road, its lamp gone out. They lie there till she is far
 * off; then the town puts them up again.
 */
final class Felled {
    static final int MOST = 24;
    static final float FALL = 0.75f, GONE = 220f;
    private final float[] x = new float[MOST], z = new float[MOST], dx = new float[MOST], dz = new float[MOST], t = new float[MOST];
    private final boolean[] live = new boolean[MOST];
    private final float[] rot = new float[9];

    /** A lamp felled at (lx, lz), struck going along (fx, fz). */
    void fell(float lx, float lz, float fx, float fz, City city) {
        int k = -1;
        float oldest = -1f;
        for (int i = 0; i < MOST; i++) {
            if (!live[i]) {
                k = i;
                break;
            }
            if (t[i] > oldest) {
                oldest = t[i];
                k = i;
            }
        }
        if (live[k]) city.raise(x[k], z[k]);
        float l = (float) Math.sqrt(fx * fx + fz * fz) + 1e-4f;
        x[k] = lx;
        z[k] = lz;
        dx[k] = fx / l;
        dz[k] = fz / l;
        t[k] = 0f;
        live[k] = true;
    }

    /** Time going on; those far behind her put up again. */
    void update(float dt, float rx, float rz, City city) {
        for (int i = 0; i < MOST; i++) {
            if (!live[i]) continue;
            t[i] += dt;
            float ox = x[i] - rx, oz = z[i] - rz;
            if (ox * ox + oz * oz > GONE * GONE) {
                live[i] = false;
                city.raise(x[i], z[i]);
            }
        }
    }

    void draw(DrawList d, float ex, float ez, float fx, float fz) {
        for (int i = 0; i < MOST; i++) {
            if (!live[i]) continue;
            float ox = x[i] - ex, oz = z[i] - ez;
            if (ox * fx + oz * fz < -8f || ox * ox + oz * oz > 160f * 160f) continue;
            // the tilt: falling quicker and quicker, a little bounce as it lands
            float u = Math.min(1f, t[i] / FALL);
            float tilt = 1.5708f * u * u;
            if (t[i] > FALL) tilt = 1.5708f - 0.06f * (float) Math.exp(-(t[i] - FALL) * 9f) * (float) Math.abs(Math.sin((t[i] - FALL) * 20f));
            tilt = Math.min(tilt, 1.62f);
            // its own frame: y up the post, falling over toward (dx, dz); x across it
            float s = (float) Math.sin(tilt), c = (float) Math.cos(tilt);
            float uy0 = dx[i] * s, uy1 = c, uy2 = dz[i] * s;
            float ax0 = dz[i], ax1 = 0f, ax2 = -dx[i];
            float az0 = ax1 * uy2 - ax2 * uy1, az1 = ax2 * uy0 - ax0 * uy2, az2 = ax0 * uy1 - ax1 * uy0;
            rot[0] = ax0; rot[1] = uy0; rot[2] = az0;
            rot[3] = ax1; rot[4] = uy1; rot[5] = az1;
            rot[6] = ax2; rot[7] = uy2; rot[8] = az2;
            int iron = 0x2B2F3A;
            float bx = x[i], by = City.WALK_TOP, bz = z[i];
            part(d, bx, by, bz, uy0, uy1, uy2, 2.2f, 0.07f, 2.2f, iron);
            part(d, bx, by, bz, uy0, uy1, uy2, 0.25f, 0.14f, 0.25f, iron);
            part(d, bx, by, bz, uy0, uy1, uy2, 4.4f, 0.135f, 0.185f, 0xB8AE96);
            part(d, bx, by, bz, uy0, uy1, uy2, 4.62f, 0.2f, 0.04f, iron);
        }
    }

    /** A piece of the post so far up it, so wide and so tall (half sizes). */
    private void part(DrawList d, float bx, float by, float bz, float ux, float uy, float uz, float up, float half, float tall, int color) {
        d.box(bx + ux * up, Math.max(by + 0.05f, by + uy * up), bz + uz * up, half, tall, half, rot, color, DrawList.PLAIN);
    }
}
