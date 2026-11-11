package com.shumtugle.skydysh;

/**
 * The delivery: a load waits at the factory on the shore road, west of home, and is wanted at the
 * store, east of it. She finds a duty pickup and drives it to the factory; stood in the bay before
 * its gate, the crates are put on its bed one by one; then to the store, where they are taken off
 * the same way. Got out of the pickup with the load on it, it waits where it was left, and the way
 * is to it. The clock runs until the last crate is off: the job is done there, and home is after.
 * The dispatcher speaks on the way: in at the wheel, near the factory's gate, near the store's,
 * and once it is done (the World hears each as the step's result).
 */
final class Delivery {
    // the steps: a pickup to be found, the factory, the store, done
    static final int PICKUP = 0, FACTORY = 1, STORE = 2, DONE = 4;
    // what a step brought: nothing, in at the wheel, near the factory's gate, near the store's, the load off
    static final int NONE = 0, IN = 1, AT_FACTORY = 2, AT_STORE = 3, DELIVERED = 4;
    /** How near a gate the pickup she drives is when the dispatcher speaks of its bay, meters. */
    static final float NEAR_GATE = 45f;
    // how long the pickup must stand in a bay before the first crate, and between crates
    static final float SETTLE = 0.6f, EACH = 0.45f;

    /** Whether mission n is a delivery: the third, and every fifth after it. */
    static boolean its(int n) {
        return World.kindOf(n) == 3;
    }

    int step = PICKUP;
    float clock, doneAt = -1f;
    /** The pickup with the load on it, once there is one. */
    Pickup carrying;
    private float stood;
    private boolean factorySaid, storeSaid;

    boolean over() {
        return step == DONE;
    }

    /** A step on: the duty pickups about, the one she drives (or none), where she is; what it brought. */
    int update(float dt, Duty duty, Pickup driving, Runner r, World.Host host) {
        if (step == DONE) return NONE;
        clock += dt;
        for (Pickup p : duty.all) p.loadedAgo += dt;
        if (step == PICKUP && driving != null) {
            step = FACTORY;
            return IN;
        }
        // at the wheel near a gate, the first time: its bay to be stood in
        if (driving != null && (step == FACTORY && !factorySaid || step == STORE && !storeSaid && driving == carrying)) {
            boolean factory = step == FACTORY;
            float dx = driving.x - City.gateX(factory), dz = driving.z - (City.SHORE * City.CELL + City.BAY_Z);
            if (dx * dx + dz * dz < NEAR_GATE * NEAR_GATE) {
                if (factory) factorySaid = true;
                else storeSaid = true;
                return factory ? AT_FACTORY : AT_STORE;
            }
        }
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
                    // the last crate off: the job done
                    carrying = null;
                    step = DONE;
                    doneAt = clock;
                    stood = 0f;
                    host.sound(Synth.CHIME, 0.5f, 1.3f);
                    return DELIVERED;
                }
            }
        }
        return NONE;
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
                    if (p.boat) continue;
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
