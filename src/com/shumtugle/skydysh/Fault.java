package com.shumtugle.skydysh;

/**
 * A light gone wrong: at one crossing with lights all of them blink amber, the cars find their own
 * way through it and the people cross as at a zebra with none. At its foot, on the post on the
 * right of the cars coming north, a grey box: on foot at it, she opens it, and inside is a panel to
 * be mended, as the concert hall's was (a smaller one); mended and switched on, the lights go round
 * again as they should.
 *
 * As a job, the fourteenth kind: the brum towed out of the quarry, his pickup in the workshop now,
 * asks her to take the job he was on his way to: a light in the town gone wrong. A story: played
 * once. Then, about the town between jobs, now and then a light goes wrong of itself, somewhere
 * near her but not under her eyes; found and mended, it brings likes each time.
 */
final class Fault {
    /** Whether mission n is the light gone wrong: the fourteenth kind. */
    static boolean its(int n) {
        return World.kindOf(n) == 14;
    }

    // the steps: to the light and its box; the panel open; done
    static final int GO = 0, OPEN = 1, DONE = 2;
    /** How near the box she has to come, on foot; how long she stands at it before it opens. */
    static final float NEAR = 1.9f, STAND = 0.35f;
    /** The panel inside the box: so many pieces each way. */
    static final int GRID = 4;
    // where to look for a light to go wrong: no nearer her than this, no farther
    static final float FROM = 110f, TO = 340f;

    int step = GO;
    float clock, doneAt = -1f;
    /** The crossing whose lights have gone wrong. */
    final int ci, cj;
    /** The panel inside the box, while it is open. */
    Panel panel;
    private float stood;
    private final long seed;

    Fault(int ci, int cj, long seed) {
        this.ci = ci;
        this.cj = cj;
        this.seed = seed;
        Crossings.broken(ci, cj);
    }

    boolean over() {
        return step == DONE;
    }

    /** Where the box stands: on the post on the right of the cars coming north, toward the street. */
    float boxX() {
        return Crossings.boxX(ci);
    }

    float boxZ() {
        return Crossings.boxZ(cj);
    }

    /**
     * A step on: her, on foot or not. True at the moment the box is opened (the panel over
     * everything from then on, till it is mended and switched on, or left).
     */
    boolean update(float dt, Runner r, boolean onFoot) {
        if (step == DONE) return false;
        clock += dt;
        if (step == GO) {
            float dx = r.x - boxX(), dz = r.z - boxZ();
            if (onFoot && !r.inAir() && dx * dx + dz * dz < NEAR * NEAR) {
                stood += dt;
                if (stood >= STAND) {
                    step = OPEN;
                    panel = new Panel(GRID, seed + (long) (clock * 1000f));
                    return true;
                }
            } else stood = 0f;
        } else if (step == OPEN) {
            panel.update(dt);
            if (panel.left) {
                // shut again unmended: still wrong, the box to be opened again
                panel = null;
                step = GO;
                stood = -1.5f;
            } else if (panel.shut()) {
                panel = null;
                step = DONE;
                doneAt = clock;
                Crossings.mended();
            }
        }
        return false;
    }

    /** Whether the panel is open over everything now. */
    boolean open() {
        return step == OPEN && panel != null;
    }

    /** Where the way goes now (x, y, z): the box. */
    void goal(float[] out) {
        out[0] = boxX();
        out[1] = City.WALK_TOP + 2.6f;
        out[2] = boxZ();
    }

    /**
     * A crossing with lights near (x, z) to go wrong, no nearer than FROM and no farther than TO,
     * the box on its south side (the cars come north into it): into out (i, j); false if none.
     */
    static boolean pick(float x, float z, long seed, int[] out) {
        int ci = Math.round(x / City.CELL), cj = Math.round(z / City.CELL);
        int found = 0, reach = (int) (TO / City.CELL) + 1;
        long h = seed;
        for (int j = cj - reach; j <= cj + reach; j++)
            for (int i = ci - reach; i <= ci + reach; i++) {
                if (j <= City.SHORE || !Crossings.lit(i, j)) continue;
                float dx = i * City.CELL - x, dz = j * City.CELL - z, d = (float) Math.sqrt(dx * dx + dz * dz);
                if (d < FROM || d > TO) continue;
                // one of those found, as the seed falls
                found++;
                h = h * 6364136223846793005L + 1442695040888963407L;
                if (((h >>> 33) % found) == 0) {
                    out[0] = i;
                    out[1] = j;
                }
            }
        return found > 0;
    }
}
