package com.shumtugle.skydysh;

/**
 * The hot brums: in the morning, five knots of them stand about the town, three in each, burnt
 * dark, their plates and their eyes glowing, ready to go off. Each knot waits at its place till it
 * sees her; then it comes after her, all three, and keeps coming: along the way she ran, turning
 * where she turned. Any that comes up to her goes off: she is to keep going. Left too far behind
 * (in a pickup, say), they lose her and go back to their place. With all five knots after her,
 * the way goes out of the town: down to the beach, along the spit to the island, round its sand
 * to the pier. A story: played once.
 *
 * The navigator shows the way to each knot in turn, the first not yet after her, along the
 * streets; a knot counts once it has seen her and comes after her, not when she is at its place.
 */
final class Herd {
    // the steps: gathering the knots, leading them out to the island, done
    static final int GATHER = 0, ISLE = 1, DONE = 2;
    static final int KNOTS = 5, EACH = 3, ALL = KNOTS * EACH;
    // how near she is seen from; how near one comes before it goes off; how far behind it loses her
    static final float SEE = 18f, CATCH = 1.5f, LOSE = 150f;
    // how fast they come: near her, well slower than her run (she gets away if she keeps going,
    // and a turn at a crossing costs her little); well behind, faster (they come up); in between,
    // eased: so they hang about thirty meters behind her, and come up only if she stands
    static final float NEAR_SPEED = 8.5f, FAR_SPEED = 12.5f, NEAR_LAG = 30f, FAR_LAG = 80f;
    // her way kept as points so far apart; so many of them at most
    static final float TRAIL_STEP = 1.2f;
    static final int TRAIL = 3000;
    // how near the pier's foot, and how far out past the beach every one of them must be
    static final float PIER_NEAR = 12f, OUT_PAST = 40f;
    // on a try again, how far back along her way the knots after her are put
    static final float BACK_BY = 36f;
    // once seen, so long they stand turning to her before they come: she may have run right
    // through them
    static final float WAKE = 1.6f;

    /** Whether mission n is this: the twelfth kind. */
    static boolean its(int n) {
        return World.kindOf(n) == 12;
    }

    // where each knot waits: at a crossing by a place of the town, on the pavements of one of its
    // corners (which, across and along: -1 or 1), in the order the way goes to them, round the town
    // so that each comes up ahead of her, never back through those after her; the corner the
    // outer one of the turn she makes there, so that cutting the turn she does not run into them
    static final float[][] PLACES = {
        {(City.OLD_I0 + 1) * City.CELL, (City.SHORE + 2) * City.CELL, -1f, 1f},
        {City.CONCERT_I * City.CELL, (City.CONCERT_J + 1) * City.CELL, -1f, 1f},
        {(City.WORKSHOP_I + 1) * City.CELL, City.WORKSHOP_J * City.CELL, -1f, 1f},
        {City.ARCHIVE_I * City.CELL, City.ARCHIVE_J * City.CELL, 1f, 1f},
        {City.TOWER_I * City.CELL, City.TOWER_J * City.CELL, 1f, 1f},
    };
    static final String[] WORDS = {"nav.oldtown", "nav.concert", "nav.workshop", "nav.archive", "herd.park"};
    static final int[] MARKS = {Marks.PIN, Marks.CONCERT, Marks.WORKSHOP, Marks.ARCHIVE, Marks.PIN};
    // each of a knot on its corner, off the crossing's middle: so far across and along (times the
    // corner's side): the corner itself, and a little way along either street, all on the pavements
    private static final float[][] SPOTS = {{7.6f, 7.6f}, {7.6f, 3.4f}, {3.4f, 7.6f}};

    // a brum's state: at its place, after her, gone off
    static final int WAITS = 0, COMES = 1, GONE = 2;

    int step = GATHER;
    float clock, doneAt = -1f;
    final float[] x = new float[ALL], y = new float[ALL], z = new float[ALL], heading = new float[ALL];
    final float[] speed = new float[ALL], stride = new float[ALL];
    final int[] state = new int[ALL];
    // when each began to come after her, by the clock
    private final float[] since = new float[ALL];
    // the point of her way each is going to, counted from the first ever kept
    private final long[] at = new long[ALL];
    /** The knots after her, a bit each; those after her when the last one came. */
    int after, kept;
    // her way: points in a ring, how many kept in all
    private final float[] tx = new float[TRAIL], ty = new float[TRAIL], tz = new float[TRAIL];
    private long trail;
    // where she was so far back along her way when each knot came after her: a try again starts there
    private final float[] backX = new float[KNOTS], backY = new float[KNOTS], backZ = new float[KNOTS];
    private final float[] fromX = new float[KNOTS], fromZ = new float[KNOTS];
    /** For the desktop stand: the one that went off last, how far behind her along her way it was. */
    int blastBy = -1;
    String blastAt;
    float blastLag, lagNow;
    /** The knot that came after her last (-1: none yet). */
    int last = -1;

    Herd(City city) {
        for (int k = 0; k < ALL; k++) home(k, city);
    }

    private void home(int k, City city) {
        float[] p = PLACES[k / EACH], s = SPOTS[k % EACH];
        x[k] = p[0] + s[0] * p[2];
        z[k] = p[1] + s[1] * p[3];
        y[k] = city.ground(x[k], z[k]);
        heading[k] = (k * 2.1f) % 6.2832f;
        speed[k] = 0f;
        state[k] = WAITS;
    }

    boolean over() {
        return step == DONE;
    }

    /** How many are after her now. */
    int coming() {
        int n = 0;
        for (int k = 0; k < ALL; k++) if (state[k] == COMES) n++;
        return n;
    }

    /** How many knots are after her. */
    int knots() {
        return Integer.bitCount(after);
    }

    // what a step brought: nothing, a knot came after her, one lost her, all after her, one went
    // off by her, done
    static final int NONE = 0, CAME = 1, LOST = 2, ALL_CAME = 3, BLAST = 4, WON = 5;

    /** A step on: where she is, and whether she is out of doors. What it brought. */
    int update(float dt, float hx, float hy, float hz, boolean out, City city) {
        if (step == DONE) return NONE;
        clock += dt;
        keep(hx, hy, hz);
        int got = NONE;
        for (int k = 0; k < ALL; k++) {
            if (state[k] == WAITS) {
                // looking about where it stands
                heading[k] += dt * 0.4f * (float) Math.sin(clock * 0.7f + k);
                if (!out) continue;
                float dx = hx - x[k], dz = hz - z[k];
                if (dx * dx + dz * dz < SEE * SEE && Math.abs(hy - y[k]) < 8f) {
                    int g = k / EACH;
                    if ((after & (1 << g)) != 0) {
                        // one of a knot after her, that had lost her, sees her again
                        state[k] = COMES;
                        at[k] = Math.max(0L, trail - 1);
                        since[k] = clock;
                    } else {
                        came(g);
                        got = after == (1 << KNOTS) - 1 ? ALL_CAME : CAME;
                    }
                }
            } else if (state[k] == COMES) {
                if (follow(k, dt, hx, hy, hz, out, city)) {
                    blastBy = k;
                    blastLag = lagNow;
                    blastAt = String.format(java.util.Locale.ROOT, "brum %.1f %.1f her %.1f %.1f, at %d of %d, %.1fs after it came", x[k], z[k], hx, hz, at[k], trail, clock - since[k]);
                    return blast();
                }
            }
        }
        // one knot all lost: back at its place
        for (int g = 0; g < KNOTS; g++) {
            if ((after & (1 << g)) == 0) continue;
            boolean any = false;
            for (int e = 0; e < EACH; e++) any |= state[g * EACH + e] == COMES;
            if (!any) {
                after &= ~(1 << g);
                for (int e = 0; e < EACH; e++) home(g * EACH + e, city);
                if (got == NONE) got = LOST;
            }
        }
        apart(dt);
        step = after == (1 << KNOTS) - 1 ? ISLE : GATHER;
        if (step == ISLE && atPier(hx, hz)) {
            step = DONE;
            doneAt = clock;
            return WON;
        }
        return got;
    }

    /** Knot g has seen her: all three after her, from where she is now. */
    private void came(int g) {
        if ((after & (1 << g)) != 0) return;
        after |= 1 << g;
        for (int e = 0; e < EACH; e++) {
            int k = g * EACH + e;
            state[k] = COMES;
            at[k] = Math.max(0L, trail - 1);
            since[k] = clock;
        }
        // for a try again: where she was a little way back along her way, by what she ran
        long first = Math.max(0L, trail - TRAIL + 1), back = trail - 1;
        float run = 0f;
        while (back > first && run < BACK_BY) {
            int a = (int) (back % TRAIL), b = (int) ((back - 1) % TRAIL);
            run += (float) Math.sqrt((tx[a] - tx[b]) * (tx[a] - tx[b]) + (tz[a] - tz[b]) * (tz[a] - tz[b]));
            back--;
        }
        int r = (int) (back % TRAIL);
        backX[g] = tx[r];
        backY[g] = ty[r];
        backZ[g] = tz[r];
        int n = (int) ((trail - 1) % TRAIL);
        fromX[g] = tx[n];
        fromZ[g] = tz[n];
        last = g;
        kept = after;
    }

    /** Her way kept: a point each time she is so far on from the last. */
    private void keep(float hx, float hy, float hz) {
        if (trail > 0) {
            int n = (int) ((trail - 1) % TRAIL);
            float dx = hx - tx[n], dz = hz - tz[n], dy = hy - ty[n];
            if (dx * dx + dz * dz + dy * dy < TRAIL_STEP * TRAIL_STEP) return;
        }
        int n = (int) (trail % TRAIL);
        tx[n] = hx;
        ty[n] = hy;
        tz[n] = hz;
        trail++;
    }

    /** Brum k after her along her way; true if it has come up to her. */
    private boolean follow(int k, float dt, float hx, float hy, float hz, boolean out, City city) {
        // too far back: her way there is no longer kept
        if (at[k] < trail - TRAIL + 1) at[k] = trail - TRAIL + 1;
        // the next point of her way it is not yet at
        while (at[k] < trail - 1) {
            int n = (int) (at[k] % TRAIL);
            float dx = tx[n] - x[k], dz = tz[n] - z[k];
            if (dx * dx + dz * dz > 1.4f * 1.4f) break;
            at[k]++;
        }
        float gx, gy, gz;
        if (at[k] >= trail - 1) {
            gx = hx;
            gy = hy;
            gz = hz;
        } else {
            int n = (int) (at[k] % TRAIL);
            gx = tx[n];
            gy = ty[n];
            gz = tz[n];
        }
        float dx = gx - x[k], dz = gz - z[k], d = (float) Math.sqrt(dx * dx + dz * dz);
        // how far behind her along her way
        float lag = d + (trail - 1 - at[k]) * TRAIL_STEP;
        lagNow = lag;
        if (lag > LOSE) {
            float ox = hx - x[k], oz = hz - z[k];
            if (ox * ox + oz * oz > 60f * 60f) {
                // lost her: back at its place
                home(k, city);
                return false;
            }
        }
        // just woken: turning to her where it stands
        if (clock - since[k] < WAKE) {
            float want = (float) Math.atan2(hx - x[k], hz - z[k]), turn = want - heading[k];
            while (turn > Math.PI) turn -= 6.2832f;
            while (turn < -Math.PI) turn += 6.2832f;
            heading[k] += turn * Math.min(1f, dt * 5f);
            speed[k] = 0f;
            return false;
        }
        // indoors she is out of reach: they wait at her door
        if (!out && at[k] >= trail - 1) {
            speed[k] = Math.max(0f, speed[k] - 20f * dt);
            return false;
        }
        float t = Math.max(0f, Math.min(1f, (lag - NEAR_LAG) / (FAR_LAG - NEAR_LAG)));
        float want = NEAR_SPEED + (FAR_SPEED - NEAR_SPEED) * t;
        speed[k] += Math.max(-24f * dt, Math.min(10f * dt, want - speed[k]));
        if (d > 1e-3f) {
            float s = Math.min(d, speed[k] * dt);
            x[k] += dx / d * s;
            z[k] += dz / d * s;
            float want2 = (float) Math.atan2(dx, dz), turn = want2 - heading[k];
            while (turn > Math.PI) turn -= 6.2832f;
            while (turn < -Math.PI) turn += 6.2832f;
            heading[k] += turn * Math.min(1f, dt * 10f);
        }
        y[k] += (gy - y[k]) * Math.min(1f, dt * 8f);
        stride[k] += dt * Math.max(0.3f, speed[k]) / (speed[k] > 6f ? 7f : 3.3f);
        float hx2 = hx - x[k], hz2 = hz - z[k];
        return out && hx2 * hx2 + hz2 * hz2 < CATCH * CATCH && Math.abs(hy - y[k]) < 2.5f;
    }

    /**
     * After the dispatcher's screen: she stood still while it was up, so they stand a moment
     * again too, turning to her, as when they first saw her.
     */
    void hold() {
        for (int k = 0; k < ALL; k++) if (state[k] == COMES) since[k] = clock;
    }

    /** Those after her kept from running through one another. */
    private void apart(float dt) {
        for (int a = 0; a < ALL; a++) {
            if (state[a] != COMES) continue;
            for (int b = a + 1; b < ALL; b++) {
                if (state[b] != COMES) continue;
                float dx = x[b] - x[a], dz = z[b] - z[a], d2 = dx * dx + dz * dz;
                if (d2 > 1.7f * 1.7f || d2 < 1e-6f) continue;
                float d = (float) Math.sqrt(d2), push = (1.7f - d) * 0.5f * Math.min(1f, dt * 8f);
                x[a] -= dx / d * push;
                z[a] -= dz / d * push;
                x[b] += dx / d * push;
                z[b] += dz / d * push;
            }
        }
    }

    /** One has come up to her and gone off; the rest stand still till the try again. */
    private int blast() {
        for (int k = 0; k < ALL; k++) if (state[k] == COMES) speed[k] = 0f;
        return BLAST;
    }

    /** Whether she is at the pier's foot with every one of them out past the beach behind her. */
    private boolean atPier(float hx, float hz) {
        Island.pierFoot(foot);
        float dx = hx - foot[0], dz = hz - foot[1];
        if (dx * dx + dz * dz > PIER_NEAR * PIER_NEAR) return false;
        for (int k = 0; k < ALL; k++) if (state[k] == COMES && z[k] > Island.SPIT_FROM - OUT_PAST) return false;
        return true;
    }

    private final float[] foot = new float[2];

    /**
     * A try again after one went off: from where she was a little before the last knot came after
     * her, the knots that were after her then a way behind her, coming; the rest at their places.
     * Where she is to be put, and the way she is to face, into out (x, y, z, heading).
     */
    void again(City city, float[] out) {
        int g = last;
        trail = 0;
        for (int k = 0; k < ALL; k++) home(k, city);
        after = 0;
        if (g < 0) return;
        float sx = backX[g], sy = backY[g], sz = backZ[g];
        float hx = fromX[g], hz = fromZ[g];
        // she where the knot saw her; behind her, back along her way, those that were after her
        keep(sx, sy, sz);
        keep(hx, city.ground(hx, hz), hz);
        int n = 0;
        for (int q = 0; q < KNOTS; q++) {
            if ((kept & (1 << q)) == 0) continue;
            after |= 1 << q;
            for (int e = 0; e < EACH; e++) {
                int k = q * EACH + e;
                state[k] = COMES;
                float a = n * 2.4f;
                x[k] = sx + (float) Math.sin(a) * (1.2f + n * 0.25f);
                z[k] = sz + (float) Math.cos(a) * (1.2f + n * 0.25f);
                y[k] = sy;
                speed[k] = 0f;
                at[k] = 0;
                n++;
            }
        }
        out[0] = hx;
        out[1] = city.ground(hx, hz);
        out[2] = hz;
        out[3] = (float) Math.atan2(hx - sx, hz - sz);
        step = GATHER;
    }

    /** For the desktop stand: knot g after her, its three at (bx, by, bz), her way from there to (hx, hy, hz). */
    void standAfter(int g, float bx, float by, float bz, float hx, float hy, float hz) {
        if (after == 0) {
            keep(bx, by, bz);
            keep(hx, hy, hz);
        }
        after |= 1 << g;
        kept = after;
        last = g;
        for (int e = 0; e < EACH; e++) {
            int k = g * EACH + e;
            state[k] = COMES;
            x[k] = bx + (e - 1) * 1.8f;
            y[k] = by;
            z[k] = bz - g * 2f;
            at[k] = trail - 2;
        }
        backX[g] = bx;
        backY[g] = by;
        backZ[g] = bz;
        fromX[g] = hx;
        fromZ[g] = hz;
    }

    /** Where the way goes now, into out (x, y, z); false if nowhere. */
    boolean goal(float[] out) {
        if (step == DONE) return false;
        if (step == ISLE) {
            Island.pierFoot(foot);
            out[0] = foot[0];
            out[1] = 3f;
            out[2] = foot[1];
            return true;
        }
        int g = next();
        out[0] = PLACES[g][0];
        out[1] = 4f;
        out[2] = PLACES[g][1];
        return true;
    }

    /** The first knot, in order, not yet after her. */
    int next() {
        for (int g = 0; g < KNOTS; g++) if ((after & (1 << g)) == 0) return g;
        return KNOTS - 1;
    }

    String goalWord() {
        return step == ISLE ? "herd.pier" : WORDS[next()];
    }

    int mark() {
        return step == ISLE ? Marks.PIN : MARKS[next()];
    }

    /** Those after her, as dots for the navigator: so many put into out (x, z each). */
    int dots(float[] out, int from) {
        int n = from;
        for (int k = 0; k < ALL && n * 2 + 1 < out.length; k++) {
            if (state[k] != COMES) continue;
            out[n * 2] = x[k];
            out[n * 2 + 1] = z[k];
            n++;
        }
        return n - from;
    }

    /** The nearest of them to (px, pz), how far; MAX_VALUE with none about. */
    float nearest(float px, float pz) {
        float best = Float.MAX_VALUE;
        for (int k = 0; k < ALL; k++) {
            float dx = x[k] - px, dz = z[k] - pz, d = dx * dx + dz * dz;
            if (d < best) best = d;
        }
        return best == Float.MAX_VALUE ? best : (float) Math.sqrt(best);
    }

    /** Them near the eye: each as a brum burnt dark, its plate and eyes glowing; at their place, still. */
    void draw(DrawList d, float time, float ex, float ez) {
        if (step == DONE) return;
        Frida.hot = true;
        for (int k = 0; k < ALL; k++) {
            float dx = x[k] - ex, dz = z[k] - ez;
            if (dx * dx + dz * dz > 120f * 120f) continue;
            String motion = speed[k] > 6f ? "sprint" : speed[k] > 2.4f ? "jog" : speed[k] > 0.4f ? "walk" : "idle";
            float t = speed[k] > 0.4f ? stride[k] * (speed[k] > 6f ? 0.6667f : speed[k] > 2.4f ? 0.9333f : 1.3333f) : time + k;
            Frida.draw(d, x[k], y[k], z[k], heading[k] + (float) Math.PI, Chase.SIZE, motion, t, false);
        }
        Frida.hot = false;
    }
}
