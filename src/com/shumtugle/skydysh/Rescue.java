package com.shumtugle.skydysh;

/**
 * The call out: the road to the village over the hills is blocked in the pass, the slopes come
 * down across it. She takes a duty pickup out of the town by the road north, clears the heap as
 * she likes (see Rockfall), the mallet, her own weight, the pickup, till a way through is open;
 * then on to the village square, at the wheel or on foot. The clock runs from the start to the
 * square; there is no losing it, only a time.
 */
final class Rescue implements City.Extra {
    // the steps: a pickup to be found, the pass to be cleared, the village to be got to, done
    static final int PICKUP = 0, PASS = 1, VILLAGE = 2, DONE = 3;

    /** Whether mission n is a call out: the fourth, and every fifth after it. */
    static boolean its(int n) {
        return World.kindOf(n) == 4;
    }

    int step = PICKUP;
    float clock, doneAt = -1f;
    /** The heap in the pass. */
    final Rockfall fall;
    /** When the way through opened (the clock's), or below zero. */
    float openedAt = -1f;

    Rescue(long seed) {
        fall = new Rockfall(seed);
    }

    boolean over() {
        return step == DONE;
    }

    /** Whether the road through the pass is clear (the stand tells it). */
    boolean cleared() {
        return fall.open();
    }

    /** A step on: the duty pickups about, the one she drives (or none), where she is. */
    void update(float dt, Duty duty, Pickup driving, Runner rn, City city, World.Host host) {
        fall.update(dt, driving, rn, host);
        if (step == DONE) return;
        clock += dt;
        float ax = driving != null ? driving.x : rn.x, az = driving != null ? driving.z : rn.z;
        if (step == PICKUP && (driving != null || az > City.PASS_Z - 120f)) step = PASS;
        // a way through the heap as wide as the pickup, she at it, or the pickup got through: the road is open
        boolean through = driving != null && driving.z > City.PASS_Z + Rockfall.DEPTH && driving.z < City.PASS_Z + 60f;
        if (step == PASS && (Math.abs(az - City.PASS_Z) < 40f && fall.open() || through)) {
            step = VILLAGE;
            openedAt = clock;
            host.sound(Synth.CHIME, 0.5f, 1.3f);
            host.sound(Synth.CHIME, 0.4f, 1.7f);
            host.buzz(80);
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
                    if (p.boat) continue;
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
