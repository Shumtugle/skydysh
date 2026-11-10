package com.shumtugle.skydysh;

/**
 * The black cherry: the black hole left over from the night at LAB Inc., still held in its trap in
 * the collider hall under the mountain, but turning free, for the ring that keeps it calm is out
 * of true: six of its magnets knocked off their stands. From its pole the jet goes down and out
 * across the hall, round and round like a lighthouse's beam; under the trap it is high over her
 * head, out by the walls it runs along the floor and is to be leapt. It burns a brum's firmware,
 * hers too: a touch, and she heats; three close together, and she is too hot, and it is tried
 * again from the lift's doors (the magnets set stay set).
 *
 * She goes up to the laboratory, in, down in the lift by its counter; in the hall each magnet
 * knocked off blinks red: a blow of the mallet at it and it is back on its stand, lit green, and
 * the jet goes round slower. The last set, the ring takes the hole: the jet dies away, the hole
 * swells and is drawn in to a bead, a flash; the chargers' lamps go green. Then the island can be
 * opened: the bridge down, the yachts whole again, the brums on it brought here to be mended.
 * A story: played once.
 */
final class Cherry {
    // the steps: up to the laboratory and down to the hall; at the magnets; the ring taking the hole; done
    static final int UP = 0, MEND = 1, SEAL = 2, DONE = 3;
    /** The ring's pieces whose magnets are knocked off (of Collider.RING_PIECES; every other piece is a magnet). */
    static final int[] KNOCKED = {2, 6, 10, 22, 26, 30};
    // a blow reaches a magnet so near her (she turns to it as she swings)
    static final float REACH = 3.1f;
    // the jet: turning so fast at first, slower by so much a magnet set; out along the floor so far,
    // breathing in and out by so much, so fast; too near its line, so near
    static final float TURN_FROM = 0.62f, TURN_LESS = 0.055f, REACH_MID = 15f, REACH_BREATH = 2.5f, BREATH = 0.9f, NEAR = 0.75f;
    // the heat: so much a touch, cooling so much a second, a touch counted again after so long;
    // put back at the lift, so long before the jet can touch her
    static final float BURN = 0.4f, COOL = 0.1f, TOUCH_GAP = 0.6f, GRACE = 2.5f;
    // the ring taking the hole: the jet gone by, the hole swollen by, drawn in by (the flash), the
    // chargers' lamps green one by one from, done
    static final float JET_DIES = 1.6f, SWELL = 2.6f, DRAWN = 3.4f, LAMPS = 3.7f, SEAL_TIME = 5.6f;

    /** Whether mission n is the black cherry: the thirteenth kind. */
    static boolean its(int n) {
        return World.kindOf(n) == 13;
    }

    int step = UP;
    float clock, doneAt = -1f, sealT;
    /** How hot she is, 0 to 1 (1: too hot). */
    float heat;
    private float grace, sinceTouch = 9f;
    /** Which of the knocked magnets have been set again. */
    final boolean[] set = new boolean[KNOCKED.length];
    /** The jet: the heading it goes out along, how far along the floor it reaches. */
    float jet, reach = REACH_MID;

    boolean over() {
        return step == DONE;
    }

    /** How many magnets are set again. */
    int setCount() {
        int n = 0;
        for (boolean s : set) if (s) n++;
        return n;
    }

    /** How strong the jet is now, 0 to 1 (going out as the ring takes the hole). */
    float jetOn() {
        if (step < SEAL) return 1f;
        if (step > SEAL) return 0f;
        return Math.max(0f, 1f - sealT / JET_DIES);
    }

    /** The jet as the set shows it before the job begins: going round by the clock. */
    void idle(float time) {
        if (clock > 0f) return;
        jet = time * TURN_FROM;
        reach = REACH_MID + REACH_BREATH * (float) Math.sin(time * BREATH);
    }

    // what happened in a step: nothing; first in the hall; touched by the jet; too hot; the ring
    // taking the hole, its flash; done
    static final int NONE = 0, HALL = 1, TOUCH = 2, HOT = 3, FLASH = 4, WON = 5;

    /**
     * A step on: whether she is in the hall (out of the lift's cabin), where she is (x, z, her feet
     * over the hall's floor).
     */
    int update(float dt, boolean inHall, float x, float feet, float z) {
        if (step == DONE) return NONE;
        clock += dt;
        float turn = TURN_FROM - TURN_LESS * setCount();
        jet += turn * dt * jetOn();
        reach = (REACH_MID + REACH_BREATH * (float) Math.sin(clock * BREATH)) * jetOn();
        if (step == SEAL) {
            float was = sealT;
            sealT += dt;
            if (was < DRAWN && sealT >= DRAWN) return FLASH;
            if (sealT >= SEAL_TIME) {
                step = DONE;
                doneAt = clock;
                return WON;
            }
            return NONE;
        }
        if (step == UP) {
            if (!inHall) return NONE;
            step = MEND;
            grace = GRACE;
            return HALL;
        }
        // at the magnets: the jet's touch, the heat
        grace = Math.max(0f, grace - dt);
        sinceTouch += dt;
        boolean under = inHall && grace <= 0f && under(x, feet, z);
        if (under && sinceTouch > TOUCH_GAP) {
            sinceTouch = 0f;
            heat = Math.min(1f, heat + BURN);
            if (heat >= 0.999f) return HOT;
            return TOUCH;
        }
        if (!under) heat = Math.max(0f, heat - COOL * dt);
        return NONE;
    }

    /** Too hot: put back at the lift, cool, a moment before the jet can touch her again. */
    void again() {
        heat = 0f;
        grace = GRACE;
        sinceTouch = 9f;
    }

    /** Whether the jet goes through her at (x, z), her feet so high over the floor (she stands about so tall). */
    boolean under(float x, float feet, float z) {
        if (jetOn() <= 0f) return false;
        float dx = x - Collider.HX, dz = z - Collider.DET_Z;
        float sx = (float) Math.sin(jet), sz = (float) Math.cos(jet);
        float along = dx * sx + dz * sz, across = Math.abs(dx * sz - dz * sx);
        if (along < 1f || along > reach || across > NEAR) return false;
        float y = Collider.HOLE_Y * (1f - along / reach);
        return y > feet - 0.15f && y < feet + 2.5f;
    }

    /** The knocked magnet still off that a blow from (x, z) reaches (its number of KNOCKED), the nearest; -1 if none. */
    int within(float x, float z) {
        if (step != MEND) return -1;
        int k = nearestOff(x, z);
        if (k < 0) return -1;
        float dx = Collider.ringX(KNOCKED[k]) - x, dz = Collider.ringZ(KNOCKED[k]) - z;
        return dx * dx + dz * dz <= REACH * REACH ? k : -1;
    }

    /** Knocked magnet k struck: set again; the last set, the ring begins to take the hole. */
    void setOne(int k) {
        if (step != MEND || set[k]) return;
        set[k] = true;
        if (setCount() == KNOCKED.length) {
            step = SEAL;
            sealT = 0f;
            heat = 0f;
        }
    }

    /** The knocked magnet still off nearest (x, z), its number of KNOCKED; -1 if none. */
    int nearestOff(float x, float z) {
        int best = -1;
        float bd = Float.MAX_VALUE;
        for (int k = 0; k < KNOCKED.length; k++) {
            if (set[k]) continue;
            float dx = Collider.ringX(KNOCKED[k]) - x, dz = Collider.ringZ(KNOCKED[k]) - z, dd = dx * dx + dz * dz;
            if (dd < bd) {
                bd = dd;
                best = k;
            }
        }
        return best;
    }

    /** For the desktop stand: the first so many magnets set at once. */
    void standSet(int n) {
        for (int k = 0; k < KNOCKED.length && k < n; k++) set[k] = true;
        if (setCount() == KNOCKED.length && step == MEND) {
            step = SEAL;
            sealT = 0f;
            heat = 0f;
        }
    }
}
