package com.shumtugle.skydysh;

import java.util.Random;

/**
 * A call out to the concert hall, north of the old town, and a paid one: the hall is full, the
 * pianist at the grand piano on the stage, and the lights have not come on; one lamp on a wall
 * burns, on a line of its own, and the rest is dark. She goes in by the middle door, through the
 * dark to the door to the wings beside the stage, and there is the panel: its wiring come apart
 * (see Panel). Mended and its main switch thrown, the lights come on; her own chair waits in the
 * back row, at its left end by the wall, among the people, a chair her size. With the light the
 * concert begins, whether she sits or not: a piece written there and then from the seed bank (see
 * Composer). Down in her chair a few seconds of it, and the dark comes down (or gone out of the
 * hall): she wakes at home on her sofa, in the morning. That is the job done.
 *
 * Done once, the hall is hers to come back to: open, lit, full, her chair kept; the music plays,
 * piece after piece, while she is in it.
 */
final class Concert {
    static final int REWARD = 40000;
    // the steps: to the hall, in it in the dark, the lights on, in her chair listening, the job done
    static final int GO = 0, INSIDE = 1, LIT = 2, SEATED = 3, DONE = 4;
    /** The panel's grid: so many pieces a side. */
    static final int GRID = 5;

    // the hall's room: half its width, its depth, its height; the rows and the seats, the stage, the piano
    static final float HALF = 10f, DEEP = 26f, HIGH = 8f;
    static final int ROWS = 8;
    static final float ROW0 = 6f, ROW_STEP = 1.45f, SEAT_STEP = 0.95f;
    // the seats across a row, in three blocks: eight in the middle, four by each wall, an aisle
    // between each; the blocks' edges (across, from and to) and the aisles' middles
    static final float[] SEAT_U = seats();
    static final int SEATS = SEAT_U.length;
    static final float MID_HALF = 3.8f, SIDE_FROM = 5.45f, SIDE_TO = 9.2f, AISLE_U = (MID_HALF + SIDE_FROM) * 0.5f;

    private static float[] seats() {
        float[] u = new float[16];
        int n = 0;
        for (int k = 0; k < 8; k++) u[n++] = (k - 3.5f) * SEAT_STEP;
        for (int e = -1; e <= 1; e += 2)
            for (int k = 0; k < 4; k++) u[n++] = e * (5.9f + k * SEAT_STEP);
        return u;
    }
    static final float STAGE = 19.5f, STAGE_HIGH = 1f, PIANO_U = -1.5f, PIANO_V = 22.8f;
    // the door to the wings, in the right wall before the stage; the one lamp burning, on the same wall
    static final float WINGS_V = 18f, WINGS_HALF = 0.55f, WINGS_HIGH = 2.2f;
    static final float SCONCE_V = 12f, SCONCE_Y = 2.8f;
    /** The lamps hung from the ceiling: how many, the first one's depth, between them. */
    static final int LAMPS = 3;
    static final float LAMP0 = ROW0 + 2f, LAMP_STEP = 4.5f;
    // how near the door she goes in; how near the door to the wings she opens the panel
    static final float DOOR_NEAR = 2.6f, WINGS_NEAR = 1.7f;
    // between pieces, seconds; the first after she sits
    static final float GAP = 5f, FIRST = 0.6f;
    // her chair: in the back row, at its left end by the wall (left: across from RIGHT), among the people, facing the
    // stage (in the place of the row's last two seats there); across, along; its half width and
    // depth, its seat's height; how near she comes to sit in it
    static final float CHAIR_U = 8.25f, CHAIR_V = ROW0, CHAIR_HALF = 1.25f, CHAIR_DEEP = 0.8f, CHAIR_SEAT = 0.6f, CHAIR_NEAR = 1.9f;

    // in the chair on the job: so long the music is heard; the last of it the dark coming down
    static final float LISTEN = 9f, DUSK = 2.5f;
    // the eye on her in her chair: across, along, how high; what it looks at, across and along
    static final float SHOT_U = 0f, SHOT_V = 0.2f, SHOT_Y = 2.9f, SHOT_TU = 0.8f, SHOT_TV = -2.5f;
    /** How many of the seats are taken. */
    static final float FULL = 0.72f;

    static float rowV(int r) {
        return ROW0 + r * ROW_STEP;
    }

    static float seatU(int k) {
        return SEAT_U[k];
    }

    /** Which way across is her right, facing the stage. */
    static final float RIGHT = -1f;

    /** Whether seat k of row r is where her chair stands, not a seat. */
    static boolean underChair(int r, int k) {
        return r == 0 && Math.abs(SEAT_U[k] - CHAIR_U) < CHAIR_HALF + 0.2f;
    }

    /** Across, the door to the wings (against the right wall). */
    static float wingsU() {
        return RIGHT * (HALF - 0.75f);
    }

    /** Whether mission n is the concert hall: the seventh kind. */
    static boolean its(int n) {
        return World.kindOf(n) == 7;
    }

    int step = GO;
    /** The clock now; when the lights came on; when she sat down (the job's time). */
    float clock, litAt = -1f, doneAt = -1f;
    /** The hall come back to, the job done before: lit from the first, no panel, no end. */
    final boolean open;
    /** Whether she sits in her chair; whether she may sit again (she has stepped away from it); how long she has sat. */
    boolean seated;
    private boolean chairArmed = true;
    private float listened;
    /** The panel, once she has got to it; open over the frame while she works at it. */
    Panel panel;
    boolean panelOpen;
    // she has stepped away from the door to the wings since the panel was last shut (so it does not open again at once)
    private boolean wingsArmed = true;
    private final long panelSeed;

    // the listeners: a seat each (row, side, place), who sits there (a townsperson's look)
    private final int count = ROWS * SEATS;
    private final boolean[] taken = new boolean[count];
    private final int[] look = new int[count];
    private final float[] lean = new float[count];
    private final Random rnd;

    // the music: the generator (made the first time), the piece now, when it ends, when the next begins
    private Composer composer;
    private Piece piece;
    private float pieceLeft = -1f, nextIn = -1f;
    private int bpm = 72;
    private final World.Host host;

    /** The hall's people from seed; the panel's wiring from a seed of its own (a new one each time). */
    Concert(World.Host host, long seed, long panelSeed) {
        this(host, seed, panelSeed, false);
    }

    /** The hall come back to, the job done: lit, full, her chair waiting. */
    static Concert open(World.Host host, long seed) {
        Concert c = new Concert(host, seed, 0L, true);
        c.step = LIT;
        c.litAt = 0f;
        return c;
    }

    private Concert(World.Host host, long seed, long panelSeed, boolean open) {
        this.host = host;
        this.panelSeed = panelSeed;
        this.open = open;
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

    /** Whether the hall's lights are on. */
    boolean lit() {
        return step >= LIT;
    }

    /** How long she has sat in her chair. */
    float listened() {
        return listened;
    }

    /** How far the dark has come down over the end of the job, 0 to 1. */
    float dusk() {
        if (step == DONE) return 1f;
        if (step != SEATED) return 0f;
        return Math.max(0f, Math.min(1f, (listened - (LISTEN - DUSK)) / DUSK));
    }

    /** Whether she has come up to her chair, the lights on, and may sit in it. */
    boolean atChair(Indoors room, Runner r) {
        if (!lit() || seated || step == DONE) return false;
        float du = room.u(r.x, r.z) - CHAIR_U, dv = room.v(r.x, r.z) - CHAIR_V;
        boolean near = du * du + dv * dv < CHAIR_NEAR * CHAIR_NEAR;
        if (!near) chairArmed = true;
        return near && chairArmed && !r.inAir();
    }

    /** Down in her chair (the music plays on, as it did). */
    void sit() {
        seated = true;
        chairArmed = false;
        listened = 0f;
        if (!open && step == LIT) {
            step = SEATED;
            doneAt = clock;
        }
    }

    /** Up from her chair: the music plays on. */
    void rise() {
        seated = false;
    }

    private void quiet() {
        if (playing()) host.music(null);
        pieceLeft = -1f;
        nextIn = -1f;
    }

    /** Where she sits, the seat's middle (x, y, z into out), in the town's terms. */
    void chair(Indoors room, float[] out) {
        out[0] = room.wx(CHAIR_U, CHAIR_V);
        out[1] = Indoors.FLOOR + CHAIR_SEAT;
        out[2] = room.wz(CHAIR_U, CHAIR_V);
    }

    /** Where she stands up to: behind the chair, on the way across the hall, facing it (x, z into out). */
    void offChair(Indoors room, float[] out) {
        out[0] = room.wx(CHAIR_U, CHAIR_V - CHAIR_DEEP - 0.65f);
        out[1] = room.wz(CHAIR_U, CHAIR_V - CHAIR_DEEP - 0.65f);
    }

    /**
     * The eye while she sits: hers, from her chair at the back, over the heads of the rows before
     * her to the stage and the pianist at the piano (x, y, z, yaw, pitch into eye); she is not seen.
     */
    void shot(Indoors room, float[] eye) {
        float eu = CHAIR_U + SHOT_U, ev = CHAIR_V + SHOT_V, ey = Indoors.FLOOR + SHOT_Y;
        float tu = PIANO_U + SHOT_TU, tv = PIANO_V + SHOT_TV, ty = STAGE_HIGH + 1.3f;
        eye[0] = room.wx(eu, ev);
        eye[1] = ey;
        eye[2] = room.wz(eu, ev);
        float dx = room.wx(tu, tv) - eye[0], dz = room.wz(tu, tv) - eye[2];
        eye[3] = (float) Math.atan2(dx, dz);
        eye[4] = (float) Math.atan2(ty - ey, Math.sqrt(dx * dx + dz * dz));
    }

    /** Whether the music plays now. */
    boolean playing() {
        return pieceLeft > 0f;
    }

    /**
     * A step on. In: whether she is in the hall's room now; she, so the door to the wings can be
     * come up to. True if the panel has just been opened.
     */
    boolean update(float dt, boolean in, Indoors room, Runner r) {
        clock += dt;
        if (panelOpen) {
            panel.update(dt);
            if (panel.left) {
                panelOpen = false;
                panel.left = false;
                wingsArmed = false;
            } else if (panel.shut()) {
                // the lights on: her chair to go to
                panelOpen = false;
                step = LIT;
                litAt = clock;
            }
            return false;
        }
        if (step == GO && in) step = INSIDE;
        if (!in) {
            // out of the hall: the music stops; on the job, the lights on, gone out: the evening over
            seated = false;
            quiet();
            if (!open && step == LIT) {
                step = DONE;
                doneAt = clock;
            }
            return false;
        }
        if (step == INSIDE) {
            float du = room.u(r.x, r.z) - wingsU(), dv = room.v(r.x, r.z) - WINGS_V;
            boolean near = du * du + dv * dv < WINGS_NEAR * WINGS_NEAR;
            if (!near) wingsArmed = true;
            else if (wingsArmed) {
                if (panel == null) panel = new Panel(GRID, panelSeed);
                panelOpen = true;
                wingsArmed = false;
                return true;
            }
            return false;
        }
        if (!lit()) return false;
        // the lights on: the concert, whether she sits or not, the people do; in her chair on the
        // job, a few seconds of it and the dark
        if (nextIn < 0f && pieceLeft <= 0f) nextIn = FIRST;
        if (seated) {
            listened += dt;
            if (!open && step == SEATED && listened >= LISTEN) {
                step = DONE;
                quiet();
                return false;
            }
        }
        if (nextIn >= 0f) {
            nextIn -= dt;
            if (nextIn < 0f) begin();
        }
        if (pieceLeft > 0f) {
            pieceLeft -= dt;
            if (pieceLeft <= 0f) {
                pieceLeft = -1f;
                nextIn = GAP;
            }
        }
        return false;
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
        host.music(piece.midi(bpm, 0));
    }

    /** Across, the seat of listener i. */
    private static float seatUOf(int i) {
        return SEAT_U[i % SEATS];
    }

    /** Where the door to the wings is, a little over it, into out (x, y, z), in the town's terms. */
    void wings(Indoors room, float[] out) {
        out[0] = room.wx(wingsU(), WINGS_V);
        out[1] = Indoors.FLOOR + WINGS_HIGH + 1.2f;
        out[2] = room.wz(wingsU(), WINGS_V);
    }

    /** Stopped: the music too (the mission over, or given up). */
    void stop() {
        quiet();
        seated = false;
        panelOpen = false;
    }

    // ---- drawing ----

    /** Her chair: a frame of dark wood, a red cushion, a high red back, arms with gilt tops, gilt feet. */
    static void drawChair(DrawList d, Indoors room) {
        float f = Indoors.FLOOR, v = CHAIR_V, hw = CHAIR_HALF, hd = CHAIR_DEEP, seat = CHAIR_SEAT, cu = CHAIR_U;
        int wood = 0x3A2A24, red = 0xA8323A, gilt = 0xC9A15A;
        room.box(d, cu, f + seat * 0.5f - 0.05f, v, hw - 0.18f, seat * 0.5f - 0.05f, hd - 0.05f, wood, DrawList.WOOD);
        room.box(d, cu, f + seat - 0.05f, v + 0.05f, hw - 0.22f, 0.08f, hd - 0.12f, red, DrawList.GRAIN);
        // the back, on the side of the doors, high as she sits
        room.box(d, cu, f + seat + 1.0f, v - hd + 0.1f, hw - 0.12f, 1.05f, 0.1f, wood, DrawList.WOOD);
        room.box(d, cu, f + seat + 1.0f, v - hd + 0.22f, hw - 0.3f, 0.9f, 0.03f, red, DrawList.GRAIN);
        room.box(d, cu, f + seat + 2.08f, v - hd + 0.1f, hw - 0.05f, 0.06f, 0.13f, gilt, DrawList.GOLD);
        for (int e = -1; e <= 1; e += 2) {
            float u = cu + e * (hw - 0.1f);
            room.box(d, u, f + seat + 0.2f, v, 0.1f, seat * 0.5f + 0.2f, hd, wood, DrawList.WOOD);
            room.box(d, u, f + seat + 0.72f, v, 0.13f, 0.04f, hd + 0.02f, gilt, DrawList.GOLD);
            for (int g = -1; g <= 1; g += 2)
                room.box(d, u, f + 0.06f, v + g * (hd - 0.1f), 0.12f, 0.06f, 0.12f, gilt, DrawList.GOLD);
        }
    }

    /** The lamps alight, once on; the listeners in their seats; the pianist at the piano, swaying as the music goes. */
    void draw(DrawList d, Indoors room) {
        float f = Indoors.FLOOR;
        if (lit()) {
            float top = f + HIGH;
            for (int k = 0; k < LAMPS; k++)
                room.box(d, 0f, top - 2.15f, LAMP0 + k * LAMP_STEP, 0.6f, 0.1f, 0.6f, 0xFFE9BE, DrawList.GLOW);
        }
        if (FolkPack.town == null || !Frida.ready()) return;
        float yaw = (float) Math.atan2(room.wx(0f, 1f) - room.wx(0f, 0f), room.wz(0f, 1f) - room.wz(0f, 0f));
        // facing the stage: in, away from the doors
        for (int i = 0; i < count; i++) {
            if (!taken[i] || underChair(i / SEATS, i % SEATS)) continue;
            float u = seatUOf(i), v = rowV(i / SEATS);
            float x = room.wx(u, v), z = room.wz(u, v);
            int m = look[i] % FolkPack.town.count;
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
