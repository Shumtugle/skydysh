package com.shumtugle.skydysh;

/**
 * The port, the first time: to the container yard by the store, up the tower at its fence to the
 * crane, the field put in order. A game: after it, the containers are played from home's list
 * with no way to go, or at the crane whenever she climbs it.
 */
final class Port {
    // the steps: to the crane, at it, done
    static final int GO = 0, PLAY = 1, DONE = 2;

    /** Whether mission n is the port: the tenth kind. */
    static boolean its(int n) {
        return World.kindOf(n) == 10;
    }

    int step;
    float clock, doneAt = -1f;

    boolean over() {
        return step == DONE;
    }

    /** A step on: whether she is at the crane, whether its field is done. */
    void update(float dt, boolean atCrane, boolean solved) {
        if (step == DONE) return;
        clock += dt;
        if (step == GO && atCrane) step = PLAY;
        if (step == PLAY && solved) {
            step = DONE;
            doneAt = clock;
        }
    }

    /** Where the way goes now: the foot of the tower's ladder; false at the crane. */
    boolean goal(float[] out) {
        if (step != GO) return false;
        out[0] = Quay.TOWER_X;
        out[1] = 3f;
        out[2] = Quay.TOWER_Z - 2.2f;
        return true;
    }
}
