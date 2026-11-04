package com.shumtugle.skydysh;

import java.util.Random;

/**
 * The concert: a paid job at the concert hall, north of the old town. She goes there and in by its
 * middle door; the hall is full, the pianist at the grand piano on the stage, and pieces are
 * played one after another, each new, written there and then from the seed bank (see Composer).
 * She keeps order in the hall: now and then someone gets up and talks during the music, and she
 * goes up the aisle to them and they sit down again. Two pieces heard through, and the job is
 * done; the concert goes on while she stays. Out of the hall before that, the pieces heard are
 * not counted, and they begin again when she is back in.
 */
final class Concert {
    static final int REWARD = 40000;
    // the steps: to the hall, in it listening, done
    static final int GO = 0, INSIDE = 1, DONE = 2;
    /** Pieces to be heard through. */
    static final int PIECES = 2;

    // the hall's room: half its width, its depth, its height; the rows and the seats, the stage, the piano
    static final float HALF = 10f, DEEP = 26f, HIGH = 8f;
    static final int ROWS = 8, SEATS = 7;
    static final float ROW0 = 6f, ROW_STEP = 1.45f, SEAT0 = 2.3f, SEAT_STEP = 0.95f;
    static final float STAGE = 19.5f, STAGE_HIGH = 1f, PIANO_U = -1.5f, PIANO_V = 22.8f;
    // how near the door she goes in; how near someone standing she must come for them to sit
    static final float DOOR_NEAR = 2.6f, SETTLE = 4.5f;
    // between pieces, seconds; the first after she comes in; how often someone gets up, at most
    static final float GAP = 5f, FIRST = 3f, RESTLESS_MIN = 14f, RESTLESS_MAX = 26f;
    /** How many of the seats are taken. */
    static final float FULL = 0.72f;

    static float rowV(int r) {
        return ROW0 + r * ROW_STEP;
    }

    static float seatU(int k) {
        return SEAT0 + k * SEAT_STEP;
    }

    /** Whether mission n is the concert: the seventh kind. */
    static boolean its(int n) {
        return World.kindOf(n) == 7;
    }

    int step = GO;
    float clock, doneAt = -1f;
    /** Pieces heard through, while she is in; those who got up and talked, and those she sat down again. */
    int heard, noisy, calmed;

    // the listeners: a seat each (row, side, place), who sits there (a townsperson's look), whether standing
    private final int count = ROWS * 2 * SEATS;
    private final boolean[] taken = new boolean[count], standing = new boolean[count];
    private final int[] look = new int[count];
    private final float[] lean = new float[count];
    private final Stroll.Way[] ways = new Stroll.Way[count];
    private final Random rnd;

    // the music: the generator (made the first time), the piece now, when it ends, when the next begins
    private Composer composer;
    private Piece piece;
    private float pieceLeft = -1f, nextIn = -1f, restless;
    private int bpm = 72;
    private final World.Host host;

    Concert(World.Host host, long seed) {
        this.host = host;
        rnd = new Random(seed);
        for (int i = 0; i < count; i++) {
            taken[i] = rnd.nextFloat() < FULL;
            look[i] = rnd.nextInt(1 << 16);
            lean[i] = (rnd.nextFloat() - 0.5f) * 0.3f;
        }
    }

    boolean over() {
        return step == DONE;
    }

    /** Whether the music plays now. */
    boolean playing() {
        return pieceLeft > 0f;
    }

    /**
     * A step on. In: whether she is in the hall's room now; she (in the room's frame: across, in
     * from the front), so the restless can be come up to.
     */
    void update(float dt, boolean in, Indoors room, Runner r) {
        clock += dt;
        if (step == GO && in) step = INSIDE;
        if (!in) {
            // out of the hall: the music stops, what was heard is not counted
            if (playing()) host.music(null);
            pieceLeft = -1f;
            nextIn = -1f;
            if (step == INSIDE) heard = 0;
            for (int i = 0; i < count; i++) standing[i] = false;
            return;
        }
        if (nextIn < 0f && pieceLeft < 0f) nextIn = FIRST;
        if (nextIn >= 0f) {
            nextIn -= dt;
            if (nextIn < 0f) begin();
        }
        if (pieceLeft > 0f) {
            pieceLeft -= dt;
            restless -= dt;
            if (restless < 0f) getUp();
            if (pieceLeft <= 0f) {
                // the piece ends: it is heard through; a gap, and the next
                pieceLeft = -1f;
                heard++;
                nextIn = GAP;
                host.sound(Synth.CHIME, 0.25f, 0.8f);
                for (int i = 0; i < count; i++) standing[i] = false;
                if (step == INSIDE && heard >= PIECES) {
                    step = DONE;
                    doneAt = clock;
                }
            }
        }
        // someone standing, she come up to them: they sit down again
        float u = room.u(r.x, r.z), v = room.v(r.x, r.z);
        for (int i = 0; i < count; i++) {
            if (!standing[i]) continue;
            float du = u - seatUOf(i), dv = v - rowV(i / (2 * SEATS));
            if (du * du + dv * dv < SETTLE * SETTLE) {
                standing[i] = false;
                calmed++;
                host.buzz(30);
            }
        }
    }

    /** A new piece, written now, sent to be played. */
    private void begin() {
        if (composer == null) {
            byte[] b = host.asset("seeds.bin");
            if (b == null) return;
            try {
                composer = new Composer(Seeds.read(new java.io.ByteArrayInputStream(b)), rnd);
            } catch (java.io.IOException e) {
                return;
            }
        }
        piece = composer.compose();
        bpm = 66 + rnd.nextInt(5) * 6;
        pieceLeft = Piece.millis(piece.end, bpm) / 1000f + 1.5f;
        restless = RESTLESS_MIN * 0.6f + rnd.nextFloat() * (RESTLESS_MAX - RESTLESS_MIN) * 0.5f;
        host.music(piece.midi(bpm, 0));
    }

    /** Someone gets up in the middle of the music, and talks. */
    private void getUp() {
        restless = RESTLESS_MIN + rnd.nextFloat() * (RESTLESS_MAX - RESTLESS_MIN);
        for (int tries = 0; tries < 30; tries++) {
            int i = rnd.nextInt(count);
            if (taken[i] && !standing[i]) {
                standing[i] = true;
                noisy++;
                return;
            }
        }
    }

    /** Across, the seat of listener i. */
    private static float seatUOf(int i) {
        int side = (i / SEATS) % 2 == 0 ? -1 : 1, k = i % SEATS;
        return side * seatU(k);
    }

    /** Whether anyone stands now; the first who does into out (x, y, z), in the town's terms. */
    boolean someoneUp(Indoors room, float[] out) {
        for (int i = 0; i < count; i++) {
            if (!standing[i]) continue;
            float u = seatUOf(i), v = rowV(i / (2 * SEATS));
            out[0] = room.wx(u, v);
            out[1] = Indoors.FLOOR + 2.6f;
            out[2] = room.wz(u, v);
            return true;
        }
        return false;
    }

    /** Stopped: the music too (the mission over, or given up). */
    void stop() {
        if (playing()) host.music(null);
        pieceLeft = -1f;
    }

    // ---- drawing ----

    /** The listeners in their seats, those up standing; the pianist at the piano, swaying as the music goes. */
    void draw(DrawList d, Indoors room) {
        if (FolkPack.town == null || !Frida.ready()) return;
        float f = Indoors.FLOOR, yaw = (float) Math.atan2(room.wx(0f, 1f) - room.wx(0f, 0f), room.wz(0f, 1f) - room.wz(0f, 0f));
        // facing the stage: in, away from the doors
        for (int i = 0; i < count; i++) {
            if (!taken[i]) continue;
            float u = seatUOf(i), v = rowV(i / (2 * SEATS));
            float x = room.wx(u, v), z = room.wz(u, v);
            int m = look[i] % FolkPack.town.count;
            if (standing[i]) {
                if (ways[i] == null) ways[i] = Stroll.way(look[i], FolkPack.town.woman(m));
                FolkPack.walker(d, m, x, f, z, yaw + (float) Math.PI * 0.75f + lean[i], 1f, ways[i], 0f, 0f);
                continue;
            }
            float y = f + 0.5f - Walkers.HIPS * FolkPack.town.size[m];
            FolkPack.sitter(d, m, x, y, z, yaw + (float) Math.PI, 1f, lean[i], 0f);
        }
        // the pianist on the bench, before the keys
        float sway = playing() ? 0.12f * (float) Math.sin(clock * 2.1f) : 0f;
        float u = PIANO_U, v = PIANO_V - 1.85f;
        int m = 7 % FolkPack.town.count;
        float y = f + STAGE_HIGH + 0.48f - Walkers.HIPS * FolkPack.town.size[m];
        FolkPack.sitter(d, m, room.wx(u, v), y, room.wz(u, v), yaw + (float) Math.PI, 1f, sway, 0f);
    }
}
