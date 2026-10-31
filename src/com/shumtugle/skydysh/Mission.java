package com.shumtugle.skydysh;

import java.util.Random;

/**
 * A hunt: five golems that should not be are loose in the town, each waiting somewhere on a
 * pavement of its own, far apart, round where the runner starts. Come near one and it runs;
 * caught, it is done with for good. The clock runs until the last of them is caught; then the
 * dispatcher's room has the say (see Dispatch), and the next five are let loose from there.
 */
final class Mission {
    static final int FIVE = 5;
    static final float NEAREST = 150f, STEP = 55f, SPREAD = 50f;

    final Chase[] golems = new Chase[FIVE];
    float clock, doneAt = -1f;
    int round;
    private final Random rnd;

    Mission(long seed) {
        rnd = new Random(seed);
    }

    /** Five let loose round (x, z): one a way round the compass each, further and further off. */
    void start(float x, float z) {
        round++;
        clock = 0f;
        doneAt = -1f;
        float turn = rnd.nextFloat() * 6.2832f;
        for (int k = 0; k < FIVE; k++) {
            float a = turn + (k + 0.3f + rnd.nextFloat() * 0.4f) * 6.2832f / FIVE;
            float d = NEAREST + k * STEP + rnd.nextFloat() * SPREAD;
            int ci = Math.round((x + (float) Math.sin(a) * d) / City.CELL);
            int cj = Math.round((z + (float) Math.cos(a) * d) / City.CELL);
            golems[k] = new Chase(rnd.nextLong(), ci, cj, rnd.nextInt(4));
        }
    }

    void update(float dt, Runner r, City city, Cars cars) {
        if (doneAt < 0f) clock += dt;
        for (Chase g : golems) g.update(dt, r, city, cars);
        if (doneAt < 0f && caught() == FIVE) doneAt = clock;
    }

    /** How many are caught; none before the first hunt (a game may begin with the folder). */
    int caught() {
        int n = 0;
        for (Chase g : golems) if (g != null && g.done()) n++;
        return n;
    }

    boolean over() {
        return doneAt >= 0f;
    }

    /** The nearest of them still loose; the last caught when none is. */
    Chase target(Chase last) {
        Chase best = null;
        for (Chase g : golems) if (g != null && !g.done() && (best == null || g.apart < best.apart)) best = g;
        return best != null ? best : last != null ? last : golems[0];
    }
}
