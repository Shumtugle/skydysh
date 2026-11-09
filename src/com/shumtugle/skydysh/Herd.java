package com.shumtugle.skydysh;

/**
 * The hot brums: in the morning, five knots of them stand about the town, three in each, burnt
 * dark, their plates and their eyes glowing, ready to go off. Each knot waits at its place till it
 * sees her; then it comes after her, all three, and keeps coming: along the way she ran, turning
 * where she turned. Any that comes up to her goes off: she is to keep going. Left too far behind
 * (in a pickup, say), they lose her and go back to their place. With all five knots after her,
 * the way goes out of the town: down to the beach, along the spit to the island, round its sand
 * to the pier. There the dispatcher sends her out along it to a yacht; one of those after her,
 * on the shore, throws a golden mallet over her head at the yachts, and they go up: burning, sunk,
 * and those after her flung down on the sand. No way off by sea: up to the lighthouse, they after
 * her again; one that comes up to her now goes off and it is tried again from the pier. A story:
 * played once.
 *
 * The navigator shows the way to each knot in turn, the first not yet after her, along the
 * streets; a knot counts once it has seen her and comes after her, not when she is at its place.
 */
final class Herd {
    // the steps: gathering the knots, leading them out to the island, done; out along the pier to
    // a yacht, the yachts going up, up to the lighthouse
    static final int GATHER = 0, ISLE = 1, DONE = 2, BOAT = 3, SCENE = 4, LIGHT = 5;
    static final int KNOTS = 5, EACH = 3, ALL = KNOTS * EACH;
    // how near she is seen from; how near one comes before it goes off; how far behind it loses her
    static final float SEE = 18f, CATCH = 1.5f, LOSE = 150f;
    // how fast they come: near her, well slower than her run (she gets away if she keeps going,
    // and a turn at a crossing costs her little); well behind, faster (they come up); in between,
    // eased: so they hang about thirty meters behind her, and come up only if she stands
    static final float NEAR_SPEED = 8.5f, FAR_SPEED = 13f, NEAR_LAG = 22f, FAR_LAG = 50f;
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
    // the yachts: so far out along the pier she is when it starts; when the one throwing begins to
    // throw; how long after the blast it is watched; how fast, and how far up, those after her are
    // flung by it (the nearer the harder); how long they lie once it is over
    static final float BOAT_AT = 4f, THROW_AT = 0.7f, AFTER = 3.2f, KNOCK = 11f, KNOCK_NEAR = 45f, KNOCK_UP = 7.5f, STUN = 2.6f;
    // the golden mallet a little bigger than hers, to be seen from afar
    static final float GOLDEN = Toss.SCALE * 1.4f;

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
    // which points of her way were indoors
    private final boolean[] tin = new boolean[TRAIL];
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

    // the yachts: how long since it started, which one throws (-1: none), whether the mallet has left its hand
    float sceneT;
    int thrower = -1;
    private boolean thrown;
    final Wreck wreck = new Wreck();
    // flung by the blast: how fast, whether still in the air; how long each lies yet
    private final float[] vx = new float[ALL], vy = new float[ALL], vz = new float[ALL];
    private final boolean[] airborne = new boolean[ALL];
    final float[] down = new float[ALL];
    // for a try again after the yachts: where she was, where each was, which were after her
    private float herX, herY, herZ;
    private final float[] keptX = new float[ALL], keptY = new float[ALL], keptZ = new float[ALL];
    private final boolean[] keptOn = new boolean[ALL];

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
        // out on the island those that lost her still count: they wait for her there
        for (int k = 0; k < ALL; k++) if (state[k] == COMES || step >= BOAT && (after & (1 << k / EACH)) != 0) n++;
        return n;
    }

    /** How many knots are after her. */
    int knots() {
        return Integer.bitCount(after);
    }

    // what a step brought: nothing, a knot came after her, one lost her, all after her, one went
    // off by her, done; at the pier, out along it (the yachts begin), the blast, the yachts over
    static final int NONE = 0, CAME = 1, LOST = 2, ALL_CAME = 3, BLAST = 4, WON = 5, PIER = 6, ON = 7, BOOM = 8, WRECKED = 9;

    /** A step on: where she is, and whether she is out of doors. What it brought. */
    int update(float dt, float hx, float hy, float hz, boolean out, City city) {
        if (step == DONE) return NONE;
        clock += dt;
        keep(hx, hy, hz, out);
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
                // flung by the blast, in the air: flown in the yachts' step; the one throwing, throwing
                if (airborne[k] || step == SCENE && k == thrower && !thrown) continue;
                if (down[k] > 0f) {
                    // flung down by the blast: lying a while, then up and after her from her way nearest
                    speed[k] = 0f;
                    down[k] -= dt;
                    if (down[k] <= 0f) {
                        since[k] = clock;
                        at[k] = nearestKept(x[k], z[k]);
                    }
                    continue;
                }
                if (follow(k, dt, hx, hy, hz, out, city) && step != SCENE) {
                    blastBy = k;
                    blastLag = lagNow;
                    blastAt = String.format(java.util.Locale.ROOT, "brum %.1f %.1f her %.1f %.1f, at %d of %d, %.1fs after it came", x[k], z[k], hx, hz, at[k], trail, clock - since[k]);
                    return blast();
                }
            }
        }
        // one knot all lost: back at its place (out on the island, each waits where it lost her)
        for (int g = 0; g < KNOTS && step < BOAT; g++) {
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
        if (step <= ISLE) step = after == (1 << KNOTS) - 1 ? ISLE : GATHER;
        if (step == ISLE && atPier(hx, hz)) {
            step = BOAT;
            return PIER;
        }
        if (step == BOAT && Island.alongPier(hx, hz) > BOAT_AT && Math.abs(Island.acrossPier(hx, hz)) < Island.PIER_HALF + 0.5f
                && hy > Island.DECK - 0.8f) {
            begin(hx, hz, city);
            return ON;
        }
        if (step == SCENE) {
            int s = scene(dt, hx, hy, hz, city);
            if (s != NONE) return s;
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
        keep(hx, hy, hz, true);
    }

    private void keep(float hx, float hy, float hz, boolean out) {
        if (trail > 0) {
            int n = (int) ((trail - 1) % TRAIL);
            float dx = hx - tx[n], dz = hz - tz[n], dy = hy - ty[n];
            if (dx * dx + dz * dz + dy * dy < TRAIL_STEP * TRAIL_STEP && tin[n] == !out) return;
        }
        int n = (int) (trail % TRAIL);
        tx[n] = hx;
        ty[n] = hy;
        tz[n] = hz;
        tin[n] = !out;
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
        boolean foot;
        if (at[k] >= trail - 1) {
            gx = hx;
            gy = hy;
            gz = hz;
            foot = out && onFoot(hx, hy, hz, city);
        } else {
            int n = (int) (at[k] % TRAIL);
            gx = tx[n];
            gy = ty[n];
            gz = tz[n];
            foot = !tin[n] && onFoot(gx, gy, gz, city);
        }
        float dx = gx - x[k], dz = gz - z[k], d = (float) Math.sqrt(dx * dx + dz * dz);
        // how far behind her along her way
        float lag = d + (trail - 1 - at[k]) * TRAIL_STEP;
        lagNow = lag;
        if (lag > LOSE) {
            float ox = hx - x[k], oz = hz - z[k];
            if (ox * ox + oz * oz > 60f * 60f) {
                // lost her: back at its place; out on the island, waiting where it is till it sees her again
                if (step >= BOAT) {
                    state[k] = WAITS;
                    speed[k] = 0f;
                } else home(k, city);
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
        // where she went by water, up a ladder, over the roofs, through the air or indoors they do
        // not go: they wait at its edge, or go on to where her way is on foot again if that is near
        if (!foot) {
            long j = at[k] + 1;
            boolean found = false;
            for (; j < trail; j++) {
                int n = (int) (j % TRAIL);
                if (tin[n] || !onFoot(tx[n], ty[n], tz[n], city)) continue;
                float qx = tx[n] - x[k], qz = tz[n] - z[k];
                found = qx * qx + qz * qz < 20f * 20f;
                break;
            }
            if (!found) {
                speed[k] = Math.max(0f, speed[k] - 20f * dt);
                return false;
            }
            at[k] = j;
            int n = (int) (j % TRAIL);
            gx = tx[n];
            gy = ty[n];
            gz = tz[n];
            dx = gx - x[k];
            dz = gz - z[k];
            d = (float) Math.sqrt(dx * dx + dz * dz);
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
     * Whether a point of her way (or she) is somewhere one can go on foot: not swimming, not more
     * than a man's height over the street or the ground (a car's roof is, a house's is not; a step
     * of the promenade is, a ladder is not).
     */
    private static boolean onFoot(float x, float y, float z, City city) {
        if (y < City.WATER + 0.25f) return false;
        float g = Island.onPier(x, z) ? Island.DECK : city.ground(x, z);
        return y - g < 2.6f;
    }

    /** The point of her way nearest (x, z), among the last kept: the newest of those as near. */
    private long nearestKept(float px, float pz) {
        long first = Math.max(0L, trail - 600), best = Math.max(0L, trail - 1);
        float bd = Float.MAX_VALUE;
        for (long q = trail - 1; q >= first; q--) {
            int n = (int) (q % TRAIL);
            float dx = tx[n] - px, dz = tz[n] - pz, d = dx * dx + dz * dz;
            if (d < bd - 1f) {
                bd = d;
                best = q;
            }
        }
        return best;
    }

    // ---- the yachts ----
    private final float[] tmp = new float[3];

    /**
     * Out along the pier: nobody stops. The one in the thick of the crowd after her is to throw:
     * it stands, turns to the yachts and throws, while she runs on and the rest come on.
     */
    private void begin(float hx, float hz, City city) {
        step = SCENE;
        sceneT = 0f;
        thrown = false;
        thrower = -1;
        float cx = 0f, cz = 0f;
        int n = 0;
        for (int k = 0; k < ALL; k++) {
            if (state[k] != COMES || down[k] > 0f) continue;
            cx += x[k];
            cz += z[k];
            n++;
        }
        if (n == 0) return;
        cx /= n;
        cz /= n;
        float best = Float.MAX_VALUE;
        for (int k = 0; k < ALL; k++) {
            if (state[k] != COMES || down[k] > 0f) continue;
            float dx = x[k] - cx, dz = z[k] - cz, d = dx * dx + dz * dz;
            if (d < best) {
                best = d;
                thrower = k;
            }
        }
    }

    /** The point of her way so far back along it, by what she ran. */
    private long behind(float far) {
        long first = Math.max(0L, trail - TRAIL + 1), back = Math.max(0L, trail - 1);
        float run = 0f;
        while (back > first && run < far) {
            int a = (int) (back % TRAIL), b = (int) ((back - 1) % TRAIL);
            run += (float) Math.sqrt((tx[a] - tx[b]) * (tx[a] - tx[b]) + (tz[a] - tz[b]) * (tz[a] - tz[b]));
            back--;
        }
        return back;
    }

    /** Where the mallet is thrown at: among the yachts, over the pier (x, y, z), into out. */
    static void target(float[] out) {
        Island.onPier(Island.YACHTS_MIDDLE, 0f, out);
        out[2] = out[1];
        out[1] = Island.DECK + 0.8f;
    }

    private final float[] aim = new float[3];

    /**
     * The yachts, all on the move: the one throwing turns to them, swings and lets go; the mallet
     * flies out of the crowd over her; the blast flings those after her back from it, down where
     * they land. A while after, it is on: up to the lighthouse.
     */
    private int scene(float dt, float hx, float hy, float hz, City city) {
        sceneT += dt;
        target(aim);
        int k0 = thrower;
        if (k0 >= 0 && !thrown) {
            speed[k0] = Math.max(0f, speed[k0] - 30f * dt);
            float want = (float) Math.atan2(aim[0] - x[k0], aim[2] - z[k0]), turn = want - heading[k0];
            while (turn > Math.PI) turn -= 6.2832f;
            while (turn < -Math.PI) turn += 6.2832f;
            heading[k0] += turn * Math.min(1f, dt * 8f);
            if (sceneT >= THROW_AT + Toss.RELEASE) {
                thrown = true;
                Frida.place("hand_r", x[k0], y[k0], z[k0], heading[k0] + (float) Math.PI, Chase.SIZE, "throw", Toss.RELEASE, tmp);
                if (tmp[1] < y[k0] + 0.5f) {
                    tmp[0] = x[k0];
                    tmp[1] = y[k0] + 2.4f;
                    tmp[2] = z[k0];
                }
                wreck.thrown(tmp[0], tmp[1], tmp[2], aim[0], aim[1], aim[2]);
            }
        } else if (k0 < 0 && !thrown) {
            // none to throw (none after her): it goes off anyway
            thrown = true;
            wreck.thrown(hx, hy + 3f, hz, aim[0], aim[1], aim[2]);
        }
        boolean boom = wreck.update(dt, city);
        if (boom) fling(aim[0], aim[2]);
        for (int k = 0; k < ALL; k++) {
            if (!airborne[k]) continue;
            vy[k] -= 20f * dt;
            x[k] += vx[k] * dt;
            y[k] += vy[k] * dt;
            z[k] += vz[k] * dt;
            heading[k] += dt * 3f;
            speed[k] = 0f;
            float g = Island.onPier(x[k], z[k]) ? Island.DECK : Math.max(City.WATER - 1f, city.ground(x[k], z[k]));
            if (vy[k] < 0f && y[k] <= g) {
                y[k] = g;
                airborne[k] = false;
                down[k] = 99f;
            }
        }
        if (wreck.hit() && wreck.since() >= AFTER) {
            step = LIGHT;
            trail = 0;
            keep(hx, hy, hz);
            // a try again: from out on the pier, facing the shore
            Island.onPier(BOAT_AT + 2f, 0f, tmp);
            herX = tmp[0];
            herZ = tmp[1];
            herY = Island.DECK;
            for (int k = 0; k < ALL; k++) {
                airborne[k] = false;
                keptOn[k] = state[k] == COMES;
                keptX[k] = x[k];
                keptY[k] = y[k];
                keptZ[k] = z[k];
                if (state[k] == COMES && down[k] > 0f) down[k] = STUN;
                if (state[k] == COMES) at[k] = 0;
            }
            thrower = -1;
            return WRECKED;
        }
        return boom ? BOOM : NONE;
    }

    /** The blast at (bx, bz): every one after her flung back from it, the nearer the harder, up and over. */
    private void fling(float bx, float bz) {
        for (int k = 0; k < ALL; k++) {
            if (state[k] != COMES) continue;
            float dx = x[k] - bx, dz = z[k] - bz, d = Math.max(1f, (float) Math.sqrt(dx * dx + dz * dz));
            float push = KNOCK * Math.min(1.3f, KNOCK_NEAR / d);
            vx[k] = dx / d * push;
            vz[k] = dz / d * push;
            vy[k] = KNOCK_UP * (0.8f + 0.4f * ((k * 37) % 10) / 10f);
            airborne[k] = true;
        }
    }

    /** Whether the yachts have gone up. */
    boolean wrecked() {
        return step == LIGHT || step == SCENE && wreck.hit();
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
        if (step == LIGHT) {
            // after the yachts: from the pier where she stood, those after her down where the blast threw them
            trail = 0;
            keep(herX, herY, herZ);
            for (int k = 0; k < ALL; k++) {
                if (!keptOn[k]) continue;
                state[k] = COMES;
                x[k] = keptX[k];
                y[k] = keptY[k];
                z[k] = keptZ[k];
                speed[k] = 0f;
                at[k] = 0;
                down[k] = STUN;
            }
            Island.pierWay(tmp);
            out[0] = herX;
            out[1] = herY;
            out[2] = herZ;
            out[3] = (float) Math.atan2(-tmp[0], -tmp[1]);
            return;
        }
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
        if (step == BOAT || step == SCENE) {
            Island.onPier(Island.YACHTS_MIDDLE - 6f, 0f, out);
            out[2] = out[1];
            out[1] = Island.DECK + 1f;
            return true;
        }
        if (step == LIGHT) {
            Island.door(out);
            out[1] += 2f;
            return true;
        }
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
        return step == BOAT || step == SCENE ? "herd.boat" : step == LIGHT ? "herd.light" : step == ISLE ? "herd.pier" : WORDS[next()];
    }

    int mark() {
        return step >= ISLE ? Marks.PIN : MARKS[next()];
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

    /**
     * Them near the eye: each as a brum burnt dark, its plate and eyes glowing; at their place,
     * still; flung through the air, lying where they came down; the one throwing, the golden
     * mallet in its hand till it lets go. The mallet flying, the blast, the pieces.
     */
    void draw(DrawList d, float time, float ex, float ez) {
        if (step == DONE) return;
        wreck.draw(d);
        Frida.hot = true;
        for (int k = 0; k < ALL; k++) {
            float dx = x[k] - ex, dz = z[k] - ez;
            if (dx * dx + dz * dz > 120f * 120f) continue;
            String motion = speed[k] > 6f ? "sprint" : speed[k] > 2.4f ? "jog" : speed[k] > 0.4f ? "walk" : "idle";
            float t = speed[k] > 0.4f ? stride[k] * (speed[k] > 6f ? 0.6667f : speed[k] > 2.4f ? 0.9333f : 1.3333f) : time + k;
            if (airborne[k]) {
                motion = "jump_air";
                t = time;
            } else if (down[k] > 0f) {
                motion = "slide";
                t = 0.35f;
            } else if (k == thrower && step == SCENE) {
                motion = sceneT < THROW_AT ? "idle" : "throw";
                t = sceneT < THROW_AT ? time + k : Math.min(sceneT - THROW_AT, 0.75f);
            }
            float yaw = heading[k] + (float) Math.PI;
            Frida.draw(d, x[k], y[k], z[k], yaw, Chase.SIZE, motion, t, false);
            if (k == thrower && step == SCENE && !thrown) inHand(d, k, yaw, motion, t);
        }
        Frida.hot = false;
    }

    private final float[] hand = new float[3], elbow = new float[3], grip = new float[9];

    /** The golden mallet in the hand of the one about to throw it, the handle on from its forearm. */
    private void inHand(DrawList d, int k, float yaw, String motion, float t) {
        Frida.place("hand_r", x[k], y[k], z[k], yaw, Chase.SIZE, motion, t, hand);
        Frida.place("lowerarm_r", x[k], y[k], z[k], yaw, Chase.SIZE, motion, t, elbow);
        float hx = hand[0] - elbow[0], hy = hand[1] - elbow[1], hz = hand[2] - elbow[2];
        float l = Math.max(1e-3f, (float) Math.sqrt(hx * hx + hy * hy + hz * hz));
        hx /= l;
        hy /= l;
        hz /= l;
        float bx = -hz, bz = hx, bl = Math.max(1e-3f, (float) Math.sqrt(bx * bx + bz * bz));
        bx /= bl;
        bz /= bl;
        float ux = -bz * hy, uy = bz * hx - bx * hz, uz = bx * hy;
        grip[0] = hx; grip[1] = ux; grip[2] = bx;
        grip[3] = hy; grip[4] = uy; grip[5] = 0f;
        grip[6] = hz; grip[7] = uz; grip[8] = bz;
        Mallet.draw(d, grip, hand[0], hand[1], hand[2], GOLDEN, 0f, true, 0, Mallet.ALL);
    }
}
