package com.shumtugle.skydysh;

import java.util.Random;

/**
 * The yachts going up: a golden mallet thrown from among those after her flies end over end
 * over her and comes down among the yachts (the farther, the longer and higher its way); a ball of fire there, white at heart, swelling and going
 * dark; the yachts' planks, rails and cabin roofs flung up and out, turning as they go, falling
 * back into the water and sinking, or onto the sand and lying there. The yachts themselves left
 * wrecked (see Island).
 */
final class Wreck {
    // the mallet: how long it flies, how much higher than both ends its way goes at the top; how
    // fast it turns end over end
    static final float FLY_PER = 1f / 32f, FLY_MIN = 1.4f, FLY_MAX = 2.6f, APEX_PER = 0.22f, SPIN = 13f;
    // the fire: how long it swells and dies, how big it gets
    static final float BALL = 1.1f, BALL_R = 5.5f;
    // the pieces: how many, how fast out, how fast up, how hard they fall
    static final int PIECES = 56;
    static final float OUT = 9f, UP = 9f, FALL = 18f;

    private final float[] from = new float[3], to = new float[3];
    private float clock = -1f, hit = -1f;
    private final float[] axis = new float[3], base = new float[9], turn = new float[9];

    // the pieces: where, how fast, their turn's axis, how far turned, how fast, half sizes, colour, landed
    private final float[] px = new float[PIECES], py = new float[PIECES], pz = new float[PIECES];
    private final float[] vx = new float[PIECES], vy = new float[PIECES], vz = new float[PIECES];
    private final float[] ax = new float[PIECES * 3], ang = new float[PIECES], spin = new float[PIECES];
    private final float[] hx = new float[PIECES], hy = new float[PIECES], hz = new float[PIECES];
    private final int[] col = new int[PIECES];
    private final boolean[] down = new boolean[PIECES];
    private final float[] unit = {1f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f};
    private final float[] spun = new float[9];

    /** Thrown from (x, y, z) at the yachts' middle (tx, ty, tz). */
    // this throw: how long it flies, how much higher than both ends its way goes at the top
    private float fly = 1.5f, apex = 11f;

    void thrown(float x, float y, float z, float tx, float ty, float tz) {
        from[0] = x;
        from[1] = y;
        from[2] = z;
        to[0] = tx;
        to[1] = ty;
        to[2] = tz;
        clock = 0f;
        hit = -1f;
        // the farther, the longer and higher
        float far = (float) Math.sqrt((tx - x) * (tx - x) + (tz - z) * (tz - z));
        fly = Math.max(FLY_MIN, Math.min(FLY_MAX, far * FLY_PER));
        apex = Math.max(8f, far * APEX_PER);
    }

    /** A step on; true at the moment it comes down among the yachts. */
    boolean update(float dt, City city) {
        if (clock < 0f) return false;
        clock += dt;
        boolean now = false;
        if (hit < 0f && clock >= fly) {
            hit = clock;
            burst();
            now = true;
        }
        if (hit >= 0f) {
            for (int k = 0; k < PIECES; k++) {
                if (down[k]) {
                    // in the water, going down slowly
                    if (py[k] < City.WATER) py[k] -= dt * 0.6f;
                    continue;
                }
                vy[k] -= FALL * dt;
                px[k] += vx[k] * dt;
                py[k] += vy[k] * dt;
                pz[k] += vz[k] * dt;
                ang[k] += spin[k] * dt;
                float g = Island.onPier(px[k], pz[k]) ? Island.DECK : Math.max(City.WATER - 0.2f, city.ground(px[k], pz[k]));
                if (py[k] < g + hy[k]) {
                    py[k] = g + hy[k];
                    down[k] = true;
                    if (g <= City.WATER - 0.1f) py[k] = City.WATER - hy[k] * 0.3f;
                }
            }
        }
        return now;
    }

    /** Whether it has come down. */
    boolean hit() {
        return hit >= 0f;
    }

    /** Since it came down, seconds; less than nothing before. */
    float since() {
        return hit < 0f ? -1f : clock - hit;
    }

    private void burst() {
        Random rnd = new Random(0x8A7CL);
        int[] colours = {0xF2F0EA, 0xB07A4A, 0xE0644A, 0x2E9C9A, 0x22406E, 0xD9DCDF, 0x3A3532};
        for (int k = 0; k < PIECES; k++) {
            float a = rnd.nextFloat() * 6.2832f, out = OUT * (0.35f + rnd.nextFloat());
            px[k] = to[0] + (rnd.nextFloat() - 0.5f) * 8f;
            py[k] = to[1] + rnd.nextFloat() * 1.5f;
            pz[k] = to[2] + (rnd.nextFloat() - 0.5f) * 8f;
            vx[k] = (float) Math.sin(a) * out;
            vz[k] = (float) Math.cos(a) * out;
            vy[k] = UP * (0.5f + rnd.nextFloat());
            float bx = rnd.nextFloat() - 0.5f, by = rnd.nextFloat() - 0.5f, bz = rnd.nextFloat() - 0.5f;
            float bl = Math.max(1e-3f, (float) Math.sqrt(bx * bx + by * by + bz * bz));
            ax[k * 3] = bx / bl;
            ax[k * 3 + 1] = by / bl;
            ax[k * 3 + 2] = bz / bl;
            ang[k] = rnd.nextFloat() * 6.2832f;
            spin[k] = (rnd.nextFloat() - 0.5f) * 14f;
            // planks long and thin, some square bits
            boolean plank = rnd.nextFloat() < 0.6f;
            hx[k] = plank ? 0.12f + rnd.nextFloat() * 0.1f : 0.2f + rnd.nextFloat() * 0.25f;
            hy[k] = plank ? 0.05f : 0.08f + rnd.nextFloat() * 0.1f;
            hz[k] = plank ? 0.5f + rnd.nextFloat() * 0.9f : 0.2f + rnd.nextFloat() * 0.3f;
            col[k] = colours[rnd.nextInt(colours.length)];
            down[k] = false;
        }
    }

    /** Where the mallet is in its flight, into out (x, y, z); false once it has come down or before it is thrown. */
    boolean flying(float[] out) {
        if (clock < 0f || hit >= 0f) return false;
        float s = Math.min(1f, clock / fly);
        out[0] = from[0] + (to[0] - from[0]) * s;
        out[1] = from[1] + (to[1] - from[1]) * s + 4f * apex * s * (1f - s);
        out[2] = from[2] + (to[2] - from[2]) * s;
        return true;
    }

    private final float[] at = new float[3];
    // the billows of the fire: off its heart (x, y, z) and how big, all as shares of how big it is
    private static final int BILLOWS = 9;
    private static final float[] BILLOW = {
            0f, 0.1f, 0f, 0.5f,
            0.45f, 0f, 0.15f, 0.34f,
            -0.4f, 0.05f, -0.2f, 0.36f,
            0.15f, 0.05f, -0.45f, 0.3f,
            -0.2f, 0.1f, 0.42f, 0.32f,
            0.1f, 0.55f, 0.1f, 0.38f,
            -0.3f, 0.45f, 0.25f, 0.26f,
            0.35f, 0.4f, -0.25f, 0.24f,
            0f, 0.9f, 0f, 0.22f,
    };

    /** The mallet flying, the fire, the pieces. */
    void draw(DrawList d) {
        if (clock < 0f) return;
        if (flying(at)) {
            // the handle along its way, turning end over end about the level line across it
            float hx0 = to[0] - from[0], hz0 = to[2] - from[2], l = Math.max(1e-3f, (float) Math.sqrt(hx0 * hx0 + hz0 * hz0));
            float ux = hx0 / l, uz = hz0 / l, bx = -uz, bz = ux;
            base[0] = ux; base[1] = 0f; base[2] = bx;
            base[3] = 0f; base[4] = 1f; base[5] = 0f;
            base[6] = uz; base[7] = 0f; base[8] = bz;
            axis[0] = bx;
            axis[1] = 0f;
            axis[2] = bz;
            Chase.spun(axis, 0, -clock * SPIN, base, 0, turn);
            Mallet.draw(d, turn, at[0], at[1], at[2], Toss.SCALE * 1.4f, Toss.MIDDLE, true, 0, Mallet.ALL);
            d.fire[0] = at[0];
            d.fire[1] = at[1];
            d.fire[2] = at[2];
            d.fire[3] = 1.2f;
            return;
        }
        float t = clock - hit;
        if (t < BALL) {
            // the fire: a cluster of billows, white and gold at heart, orange out, swelling and
            // rising, going dark to soot as it dies
            float a = t / BALL, r = BALL_R * (0.35f + 0.65f * (float) Math.sqrt(a));
            for (int b = 0; b < BILLOWS; b++) {
                float ox = BILLOW[b * 4] * r, oy = BILLOW[b * 4 + 1] * r + a * 2.5f, oz = BILLOW[b * 4 + 2] * r, rr = BILLOW[b * 4 + 3] * r * (1f - 0.35f * a);
                boolean heart = BILLOW[b * 4 + 3] > 0.42f;
                int c = a > 0.7f ? (heart ? 0x8A3A1A : 0x3A302C) : a > 0.45f ? (heart ? 0xFF8A2A : 0xC4501E) : heart ? 0xFFE7A0 : 0xFFA040;
                d.box(to[0] + ox, to[1] + rr * 0.6f + oy, to[2] + oz, rr, rr * 0.85f, rr, null, c, DrawList.material(DrawList.FIRE, heart ? 250 : 225));
            }
            d.fire[0] = to[0];
            d.fire[1] = to[1] + 3f;
            d.fire[2] = to[2];
            d.fire[3] = 6f * (1f - a);
        }
        for (int k = 0; k < PIECES; k++) {
            if (down[k] && py[k] < City.WATER - 1.5f) continue;
            Chase.spun(ax, k * 3, ang[k], unit, 0, spun);
            d.box(px[k], py[k], pz[k], hx[k], hy[k], hz[k], spun, t < 2f && k % 3 == 0 ? 0x2A2624 : col[k],
                    col[k] == 0xB07A4A ? DrawList.WOOD : DrawList.PAINT);
        }
    }
}
