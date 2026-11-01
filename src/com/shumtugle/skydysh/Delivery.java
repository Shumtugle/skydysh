package com.shumtugle.skydysh;

/**
 * The delivery: a load waits at the factory on the shore road, west of home, and is wanted at the
 * store, east of it. She finds a duty pickup and drives it to the factory; stood in the bay before
 * its gate, the crates are put on its bed one by one; then to the store, where they are taken off
 * the same way; then home. Got out of the pickup with the load on it, it waits where it was left,
 * and the way is to it. The clock runs until she is home.
 */
final class Delivery {
    static final int REWARD = 50000;
    // the steps: a pickup to be found, the factory, the store, home, done
    static final int PICKUP = 0, FACTORY = 1, STORE = 2, HOME = 3, DONE = 4;
    // how long the pickup must stand in a bay before the first crate, and between crates
    static final float SETTLE = 0.6f, EACH = 0.45f;
    // how near home's door is home: on foot or at the wheel
    static final float HOME_NEAR = 7f;

    /** Whether mission n is a delivery: the third, and every third after it. */
    static boolean its(int n) {
        return n % 3 == 0;
    }

    int step = PICKUP;
    float clock, doneAt = -1f;
    /** The pickup with the load on it, once there is one. */
    Pickup carrying;
    private float stood;

    boolean over() {
        return step == DONE;
    }

    /** A step on: the duty pickups about, the one she drives (or none), where she is. */
    void update(float dt, Duty duty, Pickup driving, Runner r, World.Host host) {
        if (step == DONE) return;
        clock += dt;
        for (Pickup p : duty.all) p.loadedAgo += dt;
        if (step == PICKUP && driving != null) step = FACTORY;
        if (step == FACTORY || step == STORE) {
            boolean factory = step == FACTORY;
            Pickup p = factory ? driving : carrying;
            boolean in = p != null && p == driving && Math.abs(p.speed) < 0.6f && City.inBay(factory, p.x, p.z);
            stood = in ? stood + dt : 0f;
            if (in && stood > SETTLE) {
                // a crate on, or off, every so often
                if (p.loadedAgo > EACH) {
                    p.load += factory ? 1 : -1;
                    p.loadedAgo = 0f;
                    host.sound(Synth.WOOD, 0.8f, factory ? 0.9f : 1.1f);
                    host.buzz(25);
                }
                if (factory && p.load >= Pickup.CRATES) {
                    carrying = p;
                    step = STORE;
                    stood = 0f;
                    host.sound(Synth.CHIME, 0.5f, 1.3f);
                } else if (!factory && p.load <= 0) {
                    carrying = null;
                    step = HOME;
                    stood = 0f;
                    host.sound(Synth.CHIME, 0.5f, 1.3f);
                }
            }
        }
        if (step == HOME) {
            float x = driving != null ? driving.x : r.x, z = driving != null ? driving.z : r.z;
            float dx = x - City.HOME_DOOR_X, dz = z - (City.HOME_DOOR_Z - 4f);
            if (dx * dx + dz * dz < HOME_NEAR * HOME_NEAR) {
                step = DONE;
                doneAt = clock;
            }
        }
    }

    /**
     * Where the way goes now, into out (x, y, z); false if there is nowhere to point (no duty pickup
     * about yet). A pickup to be found: the nearest one. The factory or the store: its bay; but the
     * pickup with the load, when she has got out of it.
     */
    boolean goal(Duty duty, Pickup driving, Runner r, float[] out) {
        switch (step) {
            case PICKUP: {
                Pickup best = null;
                float bd = 1e18f;
                for (Pickup p : duty.all) {
                    float dd = (p.x - r.x) * (p.x - r.x) + (p.z - r.z) * (p.z - r.z);
                    if (dd < bd) {
                        bd = dd;
                        best = p;
                    }
                }
                if (best == null) return false;
                set(out, best.x, best.y + 2.2f, best.z);
                return true;
            }
            case FACTORY:
            case STORE:
                if (step == STORE && carrying != null && carrying != driving) {
                    set(out, carrying.x, carrying.y + 2.2f, carrying.z);
                    return true;
                }
                set(out, City.gateX(step == FACTORY), 1.2f, City.SHORE * City.CELL + City.BAY_Z);
                return true;
            case HOME:
                set(out, City.HOME_DOOR_X, City.WALK_TOP + 3.5f, City.HOME_DOOR_Z);
                return true;
            default:
                return false;
        }
    }

    private static void set(float[] out, float x, float y, float z) {
        out[0] = x;
        out[1] = y;
        out[2] = z;
    }

    /** The word for where the way goes now, as Lang keeps it. */
    String goalWord() {
        switch (step) {
            case PICKUP: return "goal.pickup";
            case FACTORY: return "goal.factory";
            case STORE: return "goal.store";
            default: return "goal.home";
        }
    }
}
