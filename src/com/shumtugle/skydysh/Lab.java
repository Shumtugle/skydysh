package com.shumtugle.skydysh;

/**
 * The navigator: the laboratory up on the mountain fits pickups with a navigator it is trying out,
 * a map in the corner that shows the way. She drives up, leaves the pickup on the deck at the
 * road's top end, goes in at the laboratory's door; at the counter in its lobby she is given a box
 * with the navigator in it, carries it out to her pickup and unpacks it there: the navigator goes
 * in by the wheel and comes on, and is hers from then on. Then the trial: five minutes at the
 * wheel along the navigator's line, place after place across the town and out of it, each told as
 * the last is reached; only the time spent going the line's way counts, nearer the place along it.
 * Other brums are trying theirs out round the town at the same time, each on a round of its own.
 * A story: played once.
 */
final class Lab {
    // the steps: up to the laboratory and in, to the counter, the box handed over, carried out to
    // the pickup, unpacked there, the trial, done
    static final int GO = 0, DESK = 1, HAND = 2, CARRY = 3, UNPACK = 4, TRIAL = 5, DONE = 6;
    // the handing over: when the box leaves the hands that hold it out, when it is in hers, done
    static final float GIVE_FROM = 0.7f, GIVE_TO = 1.9f, HAND_TIME = 2.6f, DESK_NEAR = 1.3f;
    // the unpacking: the box set down by then, open by then, the navigator lifted out and in by
    // the wheel by then (it comes on), done; how near the pickup she is to be
    static final float SET_END = 0.8f, OPEN_END = 1.6f, IN_END = 3.2f, UNPACK_TIME = 4.2f, CAR_NEAR = 3.6f;
    // the trial counted; how near a place
    static final float TRIAL_TIME = 300f, PLACE_NEAR = 13f;
    // the way counts only while she comes nearer along it, so much a second at least, and for so
    // long after she last did (a stop at a crossing does not stop it at once)
    static final float GAIN = 0.8f, HOLD = 1.2f;

    /** Whether mission n is the navigator: the eleventh kind. */
    static boolean its(int n) {
        return World.kindOf(n) == 11;
    }

    int step = GO;
    float clock, doneAt = -1f, handT, unpackT;
    /** When the trial began, by the clock. */
    float trialFrom = -1f;
    /** The trial's time counted so far. */
    float counted;
    /** The place now, of PLACES; how many reached. */
    int place, reached;
    private float best = Float.MAX_VALUE, held;

    boolean over() {
        return step == DONE;
    }

    /** Whether the navigator is hers by now (in by the wheel). */
    boolean fitted() {
        return step >= TRIAL || step == UNPACK && unpackT >= IN_END;
    }

    /** Whether the time counts now. */
    boolean counting() {
        return step == TRIAL && held > 0f;
    }

    /** Whether the box is being handed over: she stands, the eye on the two of them. */
    boolean handing() {
        return step == HAND;
    }

    /** How far the box has gone from the counter's hands to hers, 0 to 1; -1 when it is none of theirs. */
    float giving() {
        if (step < HAND) return 0f;
        if (step > HAND) return 1f;
        return Math.max(0f, Math.min(1f, (handT - GIVE_FROM) / (GIVE_TO - GIVE_FROM)));
    }

    /** Whether she has the box in her hand, on foot. */
    boolean carrying() {
        return step == HAND && handT >= GIVE_TO || step == CARRY || step == UNPACK && unpackT < SET_END * 0.5f;
    }

    /** Whether she is unpacking it at the pickup: she stands, the eye on her and the pickup. */
    boolean unpacking() {
        return step == UNPACK;
    }

    // what happened in a step: nothing, the navigator came on, a place reached, done; the box
    // begun to be handed over, in her hands; the unpacking begun
    static final int NONE = 0, ON = 1, REACHED = 2, WON = 3, HANDING = 4, GIVEN = 5, OPENING = 6;

    /**
     * A step on: where she is (or the pickup she drives), at the wheel or not; how far she has still
     * to go along the navigator's way (-1: no way); the room she is in (null out of doors); where
     * the pickup she is to carry the box to is. What it brought.
     */
    int update(float dt, float x, float z, boolean wheel, float left, Indoors in, float carX, float carZ) {
        if (step == DONE) return NONE;
        clock += dt;
        switch (step) {
            case GO:
                if (in != null && in.lobby) step = DESK;
                return NONE;
            case DESK: {
                if (in == null || !in.lobby) return NONE;
                float du = in.u(x, z) - in.deskU, dv = in.v(x, z) - in.deskV;
                if (du * du + dv * dv < DESK_NEAR * DESK_NEAR) {
                    step = HAND;
                    handT = 0f;
                    return HANDING;
                }
                return NONE;
            }
            case HAND: {
                float was = handT;
                handT += dt;
                if (handT >= HAND_TIME) step = CARRY;
                return was < GIVE_TO && handT >= GIVE_TO ? GIVEN : NONE;
            }
            case CARRY: {
                float dx = x - carX, dz = z - carZ;
                if (in == null && !wheel && dx * dx + dz * dz < CAR_NEAR * CAR_NEAR) {
                    step = UNPACK;
                    unpackT = 0f;
                    return OPENING;
                }
                return NONE;
            }
            case UNPACK: {
                float was = unpackT;
                unpackT += dt;
                if (unpackT >= UNPACK_TIME) {
                    step = TRIAL;
                    trialFrom = clock;
                    place = 0;
                    best = Float.MAX_VALUE;
                }
                return was < IN_END && unpackT >= IN_END ? ON : NONE;
            }
            case TRIAL: {
                float[] p = PLACES[place];
                float dx = x - p[0], dz = z - p[1];
                if (dx * dx + dz * dz < PLACE_NEAR * PLACE_NEAR) {
                    place = (place + 1) % PLACES.length;
                    reached++;
                    best = Float.MAX_VALUE;
                    return REACHED;
                }
                // nearer along the line, at the wheel: it counts, and goes on counting a moment
                if (wheel && left >= 0f && left < best - GAIN * dt) {
                    best = left;
                    held = HOLD;
                } else {
                    held = Math.max(0f, held - dt);
                    if (left >= 0f && left > best + 40f) best = left;
                }
                if (held > 0f) counted += dt;
                if (counted >= TRIAL_TIME) {
                    counted = TRIAL_TIME;
                    step = DONE;
                    doneAt = clock;
                    return WON;
                }
                return NONE;
            }
            default:
                return NONE;
        }
    }

    // the places of the trial, in turn, round again if the time is not up: where, what is there
    // (its word, as Lang keeps it), its mark
    static final float[][] PLACES = {
        {City.QUARRY_X + City.QUARRY_HX + 6f, City.QUARRY_Z},
        {City.clinicDoorX(), City.CLINIC_J * City.CELL + 3f},
        {City.concertDoorX(), City.CONCERT_J * City.CELL + 3f},
        {-2f * City.CELL, 2f * City.CELL},
        {City.archiveDoorX(), City.ARCHIVE_J * City.CELL + 3f},
        {City.TOWNHALL_I * City.CELL + City.CELL * 0.5f, City.TOWNHALL_J * City.CELL - 2f},
        {-6f, City.workshopZ()},
        {City.gateX(true), City.SHORE * City.CELL + 4f},
    };
    static final String[] WORDS = {"nav.quarry", "nav.clinic", "nav.concert", "nav.oldtown", "nav.archive", "nav.townhall", "nav.workshop", "nav.factory"};
    static final int[] MARKS = {Marks.QUARRY, Marks.CLINIC, Marks.CONCERT, Marks.PIN, Marks.ARCHIVE, Marks.TOWNHALL, Marks.WORKSHOP, Marks.FACTORY};

    /**
     * Where the way goes now, into out (x, y, z); false if nowhere (the handing over, the
     * unpacking): up the mountain, at the wheel the deck's free space, on foot its door; in the
     * lobby the counter (the room's own place for it: in); the pickup the box goes to; the places.
     */
    boolean goal(boolean wheel, Indoors in, float carX, float carZ, float[] out) {
        if (step == GO) {
            int k = Mountain.freeSpace();
            out[0] = wheel ? Mountain.spaceX(k) : Mountain.MX;
            out[1] = Mountain.TOP + 4f;
            out[2] = wheel ? Mountain.spaceZ(k) : Mountain.DOOR_Z - 1f;
            return true;
        }
        if (step == DESK) {
            if (in == null || !in.lobby) return false;
            out[0] = in.wx(in.deskU, in.deskV + 1f);
            out[1] = Indoors.FLOOR + 2.4f;
            out[2] = in.wz(in.deskU, in.deskV + 1f);
            return true;
        }
        if (step == CARRY) {
            if (in != null && in.lobby) {
                // out of the lobby first, by its door
                out[0] = in.wx(0f, -1f);
                out[1] = Indoors.FLOOR + 2.4f;
                out[2] = in.wz(0f, -1f);
                return true;
            }
            out[0] = carX;
            out[1] = Mountain.TOP + 3f;
            out[2] = carZ;
            return true;
        }
        if (step != TRIAL) return false;
        float[] p = PLACES[place];
        out[0] = p[0];
        out[1] = 4f;
        out[2] = p[1];
        return true;
    }

    String goalWord(boolean wheel) {
        switch (step) {
            case GO:
                return wheel ? "lab.parking" : "goal.lab";
            case DESK:
                return "lab.counter";
            case CARRY:
                return "lab.car";
            case TRIAL:
                return WORDS[place];
            default:
                return null;
        }
    }

    int mark() {
        return step == DESK ? Marks.DESK : step == CARRY ? Marks.TRUCK : step == TRIAL ? MARKS[place] : Marks.LAB;
    }

    // ---- the other brums trying theirs out: each round a loop of the town's streets ----
    // each round: its west and south crossings, so many blocks across and up; how fast
    private static final int[][] ROUNDS = {{-3, 1, 3, 3}, {1, 0, 2, 3}, {-6, 2, 2, 4}, {-1, 5, 3, 4}};
    static final float TESTER_SPEED = 11f, LANE = 3f, CORNER = 7f;
    private static float[][] loops;
    private final Pickup[] testers = new Pickup[ROUNDS.length];
    private final float[] seat = new float[3];

    /** The loops as points round them (x, z each), in the right-hand lane, corners rounded. */
    private static void loops() {
        if (loops != null) return;
        loops = new float[ROUNDS.length][];
        for (int k = 0; k < ROUNDS.length; k++) {
            int[] r = ROUNDS[k];
            float x0 = r[0] * City.CELL + LANE, z0 = r[1] * City.CELL + LANE;
            float x1 = (r[0] + r[2]) * City.CELL - LANE, z1 = (r[1] + r[3]) * City.CELL - LANE;
            // round the blocks with the hands of a clock seen from above (east along the north
            // street, south down the east...): so the right-hand lane is the inner one
            float[] qx = {x0, x1, x1, x0}, qz = {z1, z1, z0, z0};
            java.util.ArrayList<Float> pts = new java.util.ArrayList<Float>();
            for (int c = 0; c < 4; c++) {
                int pc = (c + 3) % 4, nc = (c + 1) % 4;
                float px = qx[pc] - qx[c], pz = qz[pc] - qz[c], pl = (float) Math.hypot(px, pz);
                float nx = qx[nc] - qx[c], nz = qz[nc] - qz[c], nl = (float) Math.hypot(nx, nz);
                float ax = qx[c] + px / pl * CORNER, az = qz[c] + pz / pl * CORNER, bx = qx[c] + nx / nl * CORNER, bz = qz[c] + nz / nl * CORNER;
                // the corner rounded: a curve from a to b pulled toward it
                for (int q = 0; q <= 4; q++) {
                    float t = q / 4f, u = 1f - t;
                    pts.add(u * u * ax + 2f * u * t * qx[c] + t * t * bx);
                    pts.add(u * u * az + 2f * u * t * qz[c] + t * t * bz);
                }
            }
            float[] l = new float[pts.size()];
            for (int q = 0; q < l.length; q++) l[q] = pts.get(q);
            loops[k] = l;
        }
    }

    /** Tester k at time: where (x, z), its heading, into out. */
    static void tester(int k, float time, float[] out) {
        loops();
        float[] l = loops[k];
        int n = l.length / 2;
        float total = 0f;
        for (int q = 0; q < n; q++) total += dist(l, q, (q + 1) % n);
        float s = (time * TESTER_SPEED + k * 137f) % total;
        for (int q = 0; q < n; q++) {
            float d = dist(l, q, (q + 1) % n);
            if (s <= d || q == n - 1) {
                int b = (q + 1) % n;
                float a = d < 1e-3f ? 0f : Math.min(1f, s / d);
                out[0] = l[q * 2] + (l[b * 2] - l[q * 2]) * a;
                out[1] = l[q * 2 + 1] + (l[b * 2 + 1] - l[q * 2 + 1]) * a;
                out[2] = (float) Math.atan2(l[b * 2] - l[q * 2], l[b * 2 + 1] - l[q * 2 + 1]);
                return;
            }
            s -= d;
        }
    }

    private static float dist(float[] l, int a, int b) {
        return (float) Math.hypot(l[b * 2] - l[a * 2], l[b * 2 + 1] - l[a * 2 + 1]);
    }

    static int testers() {
        return ROUNDS.length;
    }

    private final float[] at = new float[3];

    /** The testers near the eye: each a pickup going round, a brum at its wheel, slate grey. */
    void draw(DrawList d, float time, float ex, float ez) {
        if (step != TRIAL) return;
        for (int k = 0; k < ROUNDS.length; k++) {
            tester(k, time, at);
            float dx = at[0] - ex, dz = at[1] - ez;
            if (dx * dx + dz * dz > 170f * 170f) continue;
            if (testers[k] == null) testers[k] = new Pickup(at[0], at[1], at[2], 0x7E57L + k);
            Pickup p = testers[k];
            p.x = at[0];
            p.z = at[1];
            p.heading = at[2];
            p.y = 0f;
            p.draw(d, time);
            p.seat(seat);
            Frida.slate = true;
            Frida.draw(d, seat[0], seat[1] - Runner.SEAT_DROP, seat[2], p.heading + (float) Math.PI, Chase.SIZE, Frida.SIT, 0f);
            Frida.slate = false;
        }
    }
}
