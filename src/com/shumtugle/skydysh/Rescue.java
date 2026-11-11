package com.shumtugle.skydysh;

/**
 * The call out: the road to the village over the hills is blocked in the pass, the slopes come
 * down across it, wedged fast. She takes a duty pickup out of the town by the road north; at the
 * heap the pickup stops, and nothing she does by hand stirs it (see Rockfall). A charge out of the
 * pickup's bed, carried to the heap and set against it, burns its fuse down and blows the heap
 * apart: the way through is open, and the job is done there. The clock runs from the start to the
 * open road; there is no losing it, only a time. What the blast left lies in the pass a while yet.
 */
final class Rescue implements City.Extra {
    // the steps: a pickup to be found, the pass to be opened, done
    static final int PICKUP = 0, PASS = 1, DONE = 3;
    // what a step brought: nothing, come up to the heap the first time, the charge gone off, the way open
    static final int NONE = 0, AT_HEAP = 1, BLAST = 2, OPEN = 3;
    // how near the heap she is when it is first seen close; the fuse, seconds; a tick of it so often
    static final float HEAP_NEAR = 34f, FUSE = 4f, TICK = 0.5f;

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
    /** The heap seen close (the dispatcher has said what it takes). */
    boolean heapSaid;
    /** A charge set against it, burning down: where it stands, which way, how much of the fuse is left. */
    boolean chargeSet;
    float cx, cy, cz, ca, fuse;
    /** How long since the charge went off (below zero: not yet); its light on the pass lasts so long. */
    float blastT = -1f;
    static final float FLASH = 0.7f;

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

    /** Whether the heap stands wedged fast still, no charge yet set at it. */
    boolean wanting() {
        return step == PASS && fall.sealed && !chargeSet;
    }

    /** A step on: the duty pickups about, the one she drives (or none), where she is; what it brought. */
    int update(float dt, Duty duty, Pickup driving, Runner rn, City city, World.Host host) {
        fall.update(dt, driving, rn, host);
        if (blastT >= 0f) blastT += dt;
        if (step == DONE) return NONE;
        clock += dt;
        float ax = driving != null ? driving.x : rn.x, az = driving != null ? driving.z : rn.z;
        if (step == PICKUP && (driving != null || az > City.PASS_Z - 120f)) step = PASS;
        if (step == PASS && fall.sealed && !heapSaid) {
            float dx = ax - fall.px, dz = az - fall.pz;
            if (dx * dx + dz * dz < HEAP_NEAR * HEAP_NEAR) {
                heapSaid = true;
                return AT_HEAP;
            }
        }
        if (chargeSet) {
            float was = fuse;
            fuse -= dt;
            if ((int) (was / TICK) != (int) (Math.max(0f, fuse) / TICK)) host.sound(Synth.TOCK, 0.35f, 1.6f + 0.3f * (1f - fuse / FUSE));
            if (fuse <= 0f) {
                chargeSet = false;
                fall.blast(cx, cy, cz);
                blastT = 0f;
                host.sound(Synth.THUNDER, 1f, 0.7f);
                host.sound(Synth.CRASH, 0.8f, 0.6f);
                host.sound(Synth.STONE, 0.7f, 0.5f);
                return BLAST;
            }
        }
        // the heap blown apart, a way through as wide as the pickup, she near it: the road is open
        boolean through = driving != null && driving.z > City.PASS_Z + Rockfall.DEPTH && driving.z < City.PASS_Z + 60f;
        if (step == PASS && !fall.sealed && (Math.abs(az - City.PASS_Z) < 60f && fall.open() || through)) {
            step = DONE;
            openedAt = clock;
            doneAt = clock;
            host.sound(Synth.CHIME, 0.5f, 1.3f);
            host.sound(Synth.CHIME, 0.4f, 1.7f);
            host.buzz(80);
            return OPEN;
        }
        return NONE;
    }

    /** A charge set at (x, y, z), facing a: true if it stands against the heap and none burns already. */
    boolean setCharge(float x, float y, float z, float a) {
        if (!wanting() || !fall.atFace(x, z)) return false;
        chargeSet = true;
        fuse = FUSE;
        cx = x;
        cy = y;
        cz = z;
        ca = a;
        return true;
    }

    /** Wedged fast, the heap is a wall to what drives at it. */
    public boolean wall() {
        return fall.sealed;
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
        if (chargeSet) Kit.charge(d, cx, cy + 0.28f, cz, ca, fuse / FUSE);
        // the blast's light on the rocks of the pass, there and gone
        if (blastT >= 0f && blastT < FLASH) {
            float k = 1f - blastT / FLASH;
            d.fire[0] = cx;
            d.fire[1] = cy + 2f;
            d.fire[2] = cz;
            d.fire[3] = 3.2f * k * k;
        }
    }

    /** Where a charge goes, from (x, z): the heap's face on the side she is, on the road before her. */
    void face(float x, float z, float[] out) {
        float side = z < fall.pz ? -1f : 1f, fz = fall.pz + side * Rockfall.WALL_DEEP;
        out[0] = City.routeX(fz);
        out[1] = 2.2f;
        out[2] = fz;
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
            default:
                return false;
        }
    }

    /** The word for where the way goes now, as Lang keeps it. */
    String goalWord() {
        return step == PICKUP ? "goal.pickup" : "goal.pass";
    }
}
