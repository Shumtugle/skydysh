package com.shumtugle.skydysh;

/**
 * The call out: the road to the village over the hills is blocked in the pass. She takes a duty
 * pickup out of the town by the road north; short of the pass the slopes come down across the
 * road before her eyes, and she breaks the heap with the mallet (see Rockfall), as many heaps as
 * she likes; then gets the pickup through and drives on to the village square. The clock runs
 * from the start to the square, standing while she plays on past the first heap; there is no
 * losing it, only a time.
 */
final class Rescue implements City.Extra {
    static final int REWARD = 60000;
    // the steps: a pickup to be found, the pass to be cleared, the village to be got to, done
    static final int PICKUP = 0, PASS = 1, VILLAGE = 2, DONE = 3;

    /** Whether mission n is a call out: the fourth, and every fifth after it. */
    static boolean its(int n) {
        return World.kindOf(n) == 4;
    }

    int step = PICKUP;
    float clock, doneAt = -1f;
    // past the pass by so much, at the wheel, the way is through
    static final float PAST = 8f;
    /** The heap in the pass, and the game of it. */
    final Rockfall fall = new Rockfall();
    /** What the last step brought from the heap (see Rockfall). */
    int fell;

    boolean over() {
        return step == DONE;
    }

    /** Whether the road through the pass is clear (the stand tells it). */
    boolean cleared() {
        return fall.cleared();
    }

    /** A step on: the duty pickups about, the one she drives (or none), where she is. */
    void update(float dt, Duty duty, Pickup driving, Runner rn, City city, World.Host host) {
        fell = step == PASS || step == VILLAGE ? fall.update(dt, driving, rn, host) : Rockfall.NONE;
        if (step == DONE) return;
        if (!fall.extra()) clock += dt;
        float ax = driving != null ? driving.x : rn.x, az = driving != null ? driving.z : rn.z;
        if (step == PICKUP && (driving != null || az > City.PASS_Z - 120f)) step = PASS;
        // through the pass at the wheel, the heap broken: the way is open
        if (step == PASS && driving != null && fall.phase == Rockfall.LEFT && driving.z > City.PASS_Z + PAST) {
            step = VILLAGE;
            host.sound(Synth.CHIME, 0.5f, 1.3f);
            host.buzz(60);
        }
        if (step == VILLAGE && Math.abs(ax - City.villageX()) < City.SQUARE_HALF && Math.abs(az - City.VILLAGE_Z) < City.SQUARE_HALF) {
            step = DONE;
            doneAt = clock;
        }
    }

    /** The pickup she drives, about to move on: what it is going into is shoved off. */
    void ram(Pickup pk, float dt) {
        fall.ram(pk, dt);
    }

    /** How many times something has been struck (for the world to hear it). */
    int hit() {
        return fall.hit;
    }

    /** A blow of the mallet, on foot, at (bx, bz), facing (fx, fz). */
    void strike(float bx, float bz, float fx, float fz) {
        fall.strike(bx, bz, fx, fz);
    }

    /** What lies in the pass, in her way and the pickup's (see City.Extra). */
    public boolean push(float[] p, float rr, float feet) {
        return fall.push(p, rr, feet);
    }

    void draw(DrawList d, Runner r, float ex, float ez) {
        fall.draw(d, r, ex, ez);
    }

    /** Where the way goes now, into out (x, y, z); false if there is nowhere to point. */
    boolean goal(Duty duty, Pickup driving, Runner rn, float[] out) {
        switch (step) {
            case PICKUP: {
                Pickup best = null;
                float bd = 1e18f;
                for (Pickup p : duty.all) {
                    float dd = (p.x - rn.x) * (p.x - rn.x) + (p.z - rn.z) * (p.z - rn.z);
                    if (dd < bd) {
                        bd = dd;
                        best = p;
                    }
                }
                if (best == null) return false;
                out[0] = best.x;
                out[1] = best.y + 2.2f;
                out[2] = best.z;
                return true;
            }
            case PASS:
                out[0] = City.routeX(City.PASS_Z);
                out[1] = 2.5f;
                out[2] = City.PASS_Z;
                return true;
            case VILLAGE:
                out[0] = City.villageX();
                out[1] = 2.5f;
                out[2] = City.VILLAGE_Z;
                return true;
            default:
                return false;
        }
    }

    /** The word for where the way goes now, as Lang keeps it. */
    String goalWord() {
        return step == PICKUP ? "goal.pickup" : step == PASS ? "goal.pass" : "goal.village";
    }
}
