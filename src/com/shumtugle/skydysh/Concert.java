package com.shumtugle.skydysh;

import java.util.Random;

/**
 * A call out to the concert hall, north of the old town, and a paid one: the hall is full, the
 * pianist at the grand piano on the stage, and the lights have not come on; one lamp on a wall
 * burns, on a line of its own, and the rest is dark. She goes in by the middle door, through the
 * dark to the door to the wings beside the stage, and there is the panel: its wiring come apart
 * (see Panel). Mended and its main switch thrown, the lights come on, and the concert begins:
 * pieces played one after another, each new, written there and then from the seed bank (see
 * Composer). The job is done when the lights come on; the music goes on while she stays.
 */
final class Concert {
    static final int REWARD = 40000;
    // the steps: to the hall, in it in the dark, the lights on
    static final int GO = 0, INSIDE = 1, DONE = 2;
    /** The panel's grid: so many pieces a side. */
    static final int GRID = 5;

    // the hall's room: half its width, its depth, its height; the rows and the seats, the stage, the piano
    static final float HALF = 10f, DEEP = 26f, HIGH = 8f;
    static final int ROWS = 8, SEATS = 7;
    static final float ROW0 = 6f, ROW_STEP = 1.45f, SEAT0 = 2.3f, SEAT_STEP = 0.95f;
    static final float STAGE = 19.5f, STAGE_HIGH = 1f, PIANO_U = -1.5f, PIANO_V = 22.8f;
    // the door to the wings, in the right wall before the stage; the one lamp burning, on the same wall
    static final float WINGS_V = 18f, WINGS_HALF = 0.55f, WINGS_HIGH = 2.2f;
    static final float SCONCE_V = 12f, SCONCE_Y = 2.8f;
    /** The lamps hung from the ceiling: how many, the first one's depth, between them. */
    static final int LAMPS = 3;
    static final float LAMP0 = ROW0 + 2f, LAMP_STEP = 4.5f;
    // how near the door she goes in; how near the door to the wings she opens the panel
    static final float DOOR_NEAR = 2.6f, WINGS_NEAR = 1.7f;
    // between pieces, seconds; the first after the lights come on
    static final float GAP = 5f, FIRST = 3f;
    /** How many of the seats are taken. */
    static final float FULL = 0.72f;

    static float rowV(int r) {
        return ROW0 + r * ROW_STEP;
    }

    static float seatU(int k) {
        return SEAT0 + k * SEAT_STEP;
    }

    /** Which way across is her right, facing the stage. */
    static final float RIGHT = -1f;

    /** Across, the door to the wings (against the right wall). */
    static float wingsU() {
        return RIGHT * (HALF - 0.75f);
    }

    /** Whether mission n is the concert hall: the seventh kind. */
    static boolean its(int n) {
        return World.kindOf(n) == 7;
    }

    int step = GO;
    float clock, doneAt = -1f;
    /** The panel, once she has got to it; open over the frame while she works at it. */
    Panel panel;
    boolean panelOpen;
    // she has stepped away from the door to the wings since the panel was last shut (so it does not open again at once)
    private boolean wingsArmed = true;
    private final long panelSeed;

    // the listeners: a seat each (row, side, place), who sits there (a townsperson's look)
    private final int count = ROWS * 2 * SEATS;
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
        this.host = host;
        this.panelSeed = panelSeed;
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
        return step == DONE;
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
                panelOpen = false;
                step = DONE;
                doneAt = clock;
                nextIn = FIRST;
            }
            return false;
        }
        if (step == GO && in) step = INSIDE;
        if (!in) {
            // out of the hall: the music stops
            if (playing()) host.music(null);
            pieceLeft = -1f;
            if (lit()) nextIn = FIRST;
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
        // the lights on: the concert
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
        int side = (i / SEATS) % 2 == 0 ? -1 : 1, k = i % SEATS;
        return side * seatU(k);
    }

    /** Where the door to the wings is, a little over it, into out (x, y, z), in the town's terms. */
    void wings(Indoors room, float[] out) {
        out[0] = room.wx(wingsU(), WINGS_V);
        out[1] = Indoors.FLOOR + WINGS_HIGH + 1.2f;
        out[2] = room.wz(wingsU(), WINGS_V);
    }

    /** Stopped: the music too (the mission over, or given up). */
    void stop() {
        if (playing()) host.music(null);
        pieceLeft = -1f;
        panelOpen = false;
    }

    // ---- drawing ----

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
            if (!taken[i]) continue;
            float u = seatUOf(i), v = rowV(i / (2 * SEATS));
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
