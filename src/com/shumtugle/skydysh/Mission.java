package com.shumtugle.skydysh;

import java.util.Random;

/**
 * A hunt: five brums that should not be are loose in the town, each waiting somewhere on a
 * pavement of its own, far apart, round where the runner starts. Come near one and it runs;
 * caught, it is done with for good. The clock runs until the last of them is caught, five minutes
 * at most: then those still loose are away and the hunt is lost. Either way the dispatcher's room
 * has the say (see Dispatch); won, the next five are let loose from there, lost, five again.
 */
final class Mission {
    static final int FIVE = 5;
    static final float NEAREST = 150f, STEP = 55f, SPREAD = 50f;
    /** How long a hunt has, in seconds. */
    static final float LIMIT = 300f;

    final Chase[] brums = new Chase[FIVE];
    float clock, doneAt = -1f;
    int round;
    /** Whether the time ran out with some still loose. */
    boolean late;
    private final Random rnd;

    Mission(long seed) {
        rnd = new Random(seed);
    }

    /** Five let loose round (x, z): one a way round the compass each, further and further off. */
    void start(float x, float z) {
        round++;
        clock = 0f;
        doneAt = -1f;
        late = false;
        float turn = rnd.nextFloat() * 6.2832f;
        for (int k = 0; k < FIVE; k++) {
            float a = turn + (k + 0.3f + rnd.nextFloat() * 0.4f) * 6.2832f / FIVE;
            float d = NEAREST + k * STEP + rnd.nextFloat() * SPREAD;
            int ci = Math.round((x + (float) Math.sin(a) * d) / City.CELL);
            int cj = Math.round((z + (float) Math.cos(a) * d) / City.CELL);
            // none out at sea: the other way, inland
            if (cj < City.SHORE) cj = 2 * City.SHORE - cj;
            brums[k] = new Chase(rnd.nextLong(), ci, cj, City.way(cj, rnd.nextInt(4)));
        }
    }

    void update(float dt, Runner r, City city, Cars cars) {
        if (doneAt < 0f) clock += dt;
        for (Chase g : brums) g.update(dt, r, city, cars);
        if (doneAt < 0f && caught() == FIVE) doneAt = clock;
        if (doneAt < 0f && clock >= LIMIT) {
            doneAt = LIMIT;
            late = true;
        }
    }

    /** The time a hunt still has, in seconds. */
    float left() {
        return Math.max(0f, LIMIT - (doneAt >= 0f ? doneAt : clock));
    }

    /** How many are caught; none before the first hunt (a game may begin with the folder). */
    int caught() {
        int n = 0;
        for (Chase g : brums) if (g != null && g.done()) n++;
        return n;
    }

    boolean over() {
        return doneAt >= 0f;
    }

    /** The nearest of them still loose; the last caught when none is. */
    Chase target(Chase last) {
        Chase best = null;
        for (Chase g : brums) if (g != null && !g.done() && (best == null || g.apart < best.apart)) best = g;
        return best != null ? best : last != null ? last : brums[0];
    }
}
