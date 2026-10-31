package com.shumtugle.skydysh;

import java.util.Calendar;

/**
 * The dispatcher's room, between missions: a room high up over the town, the sea in its window
 * at evening; on a wooden desk an old television, a panel of keys before it, a clock with the
 * real time, a lamp, papers and a mug. On the glass of the set the dispatch is typed out, line by
 * line: before a mission what happened and what is to be done, after it what was done and in
 * what time. A touch (when it is all typed; the first touch types the rest at once) and the eye
 * goes into the glass, and the mission begins in the street. Coming back, the eye comes out of the
 * glass into the room.
 */
final class Dispatch {
    static final float TYPE = 38f;                      // letters a second
    static final float DIVE = 0.75f, EMERGE = 0.7f;

    // the glass of the set, and where the eye sits before it
    private static final float GX = -0.04f, GY = 1.07f, GZ = 0.06f, GHX = 0.24f, GHY = 0.185f;
    private static final float EYE_BACK = 1.22f;
    private static final float LAMP_X = 0.62f, LAMP_Y = 1.24f, LAMP_Z = 0.12f;
    private static final float CX = -0.17f, CY = 1.385f, CZ = 0.2f;     // the clock's face

    private final World.Host host;
    private final DrawList room = new DrawList();
    private final String[] lines;
    private final int letters;
    private float t, typed, diveAt = -1f;
    private boolean done;
    private int ticked;
    private final float[] at = new float[3];

    /**
     * The room, its set showing these lines (the last is the call to go on, and blinks once all
     * is typed); coming in out of the glass if emerging.
     */
    Dispatch(World.Host host, String[] lines, boolean emerging) {
        this.host = host;
        this.lines = lines;
        int n = 0;
        for (String l : lines) n += l.length();
        letters = n;
        build(room);
        t = emerging ? 0f : EMERGE;
    }

    /** What the set says before a mission. */
    static String[] briefing(int round) {
        if (round <= 1)
            return new String[]{"DISPATCH - MISSION 1", "", "AT SUNSET FIVE GOLEMS", "BROKE OUT OF THE FOUNDRY.",
                    "THEIR PLATES ARE FORGED.", "FORGED PLATES BURN:", "THE GOLEMS BURN WITH THEM", "AND SO WILL THE TOWN.", "",
                    "THEY HIDE ABOUT THE STREETS.", "FIND THEM. RUN THEM DOWN.", "KNOCK EVERY PLATE OFF.", "",
                    "TOUCH TO GO"};
        return new String[]{"DISPATCH - MISSION " + round, "", "FIVE MORE HAVE BROKEN OUT", "WITH FORGED PLATES.", "",
                "FIND THEM. RUN THEM DOWN.", "KNOCK EVERY PLATE OFF.", "", "TOUCH TO GO"};
    }

    /** What the set says when a mission is done. */
    static String[] report(int round, String time) {
        return new String[]{"DISPATCH - MISSION " + round, "", "ALL FIVE PLATES ARE IN.", "THE TOWN IS SAFE",
                "FOR TONIGHT.", "", "TIME  " + time, "", "MORE ARE LOOSE.", "", "TOUCH TO GO ON"};
    }

    /** A touch: the rest typed at once, or, all typed, into the glass. */
    void touch() {
        if (t < EMERGE || diveAt >= 0f) return;
        if (typed < letters) {
            typed = letters;
            return;
        }
        diveAt = t;
        host.sound(Synth.WHOOSH, 0.5f, 1.4f);
    }

    /** Whether the eye has gone into the glass: the mission begins. */
    boolean done() {
        return done;
    }

    void update(float dt) {
        t += dt;
        if (t >= EMERGE && diveAt < 0f) {
            typed = Math.min(letters, typed + dt * TYPE);
            // the set ticks as it types
            int k = (int) typed / 3;
            if (k > ticked && typed < letters) {
                ticked = k;
                host.sound(Synth.TOCK, 0.12f, 1.9f);
            }
        }
        if (diveAt >= 0f && t - diveAt >= DIVE) done = true;
    }

    // ---- drawing ----

    void draw(DrawList d, Px f, int h) {
        // the eye before the set, a little above it, looking down at it; out of the glass at first,
        // into it at the end
        float back = EYE_BACK;
        if (t < EMERGE) {
            float u = t / EMERGE;
            u = 1f - (1f - u) * (1f - u);
            back = 0.04f + (EYE_BACK - 0.04f) * u;
        }
        if (diveAt >= 0f) {
            float u = Math.min(1f, (t - diveAt) / DIVE);
            back = EYE_BACK + (0.03f - EYE_BACK) * u * u;
        }
        float k = back / EYE_BACK;
        float ex = GX, ey = GY + 0.07f * k, ez = GZ - back;
        float pitch = (float) Math.atan2(GY - 0.04f * k - ey, back);
        Daylight.light(d, 1f);
        d.fogStart = 40f;
        d.fogScale = 400f;
        // the low sun outside the window, coming in over the desk
        double el = Math.toRadians(22), tu = Math.toRadians(18);
        d.sun((float) (Math.cos(el) * Math.sin(tu)), (float) Math.sin(el), (float) (Math.cos(el) * Math.cos(tu)),
                0f, 1f, 0.5f, 7f, 5f, 12f);
        d.camera(ex, ey, ez, 0f, pitch, World.W * 1.6f, 0.02f, 500f);
        d.append(room);
        // the lamp's warm light on the desk
        d.fire[0] = LAMP_X;
        d.fire[1] = LAMP_Y - 0.05f;
        d.fire[2] = LAMP_Z;
        d.fire[3] = 0.55f;
        screen(d, f);
        clock(d, f);
        float white = 0f;
        if (t < EMERGE) white = Math.max(0f, 1f - t / (EMERGE * 0.45f));
        if (diveAt >= 0f) white = Math.max(white, (t - diveAt - DIVE * 0.6f) / (DIVE * 0.4f));
        if (white > 0f) Opening.wash(f, Math.min(1f, white));
    }

    /** The glass of the set: dark phosphor, scan lines, the dispatch typed on it. */
    private void screen(DrawList d, Px f) {
        float x0 = 1e9f, y0 = 1e9f, x1 = -1e9f, y1 = -1e9f;
        for (int c = 0; c < 4; c++) {
            float px = GX + ((c & 1) == 0 ? -GHX : GHX), py = GY + ((c & 2) == 0 ? -GHY : GHY);
            if (!d.project(px, py, GZ - 0.012f, at)) return;
            x0 = Math.min(x0, at[0]);
            x1 = Math.max(x1, at[0]);
            y0 = Math.min(y0, at[1]);
            y1 = Math.max(y1, at[1]);
        }
        int ix0 = Math.round(x0), iy0 = Math.round(y0), ix1 = Math.round(x1), iy1 = Math.round(y1);
        float cx = (x0 + x1) * 0.5f, cy = (y0 + y1) * 0.5f, rx = (x1 - x0) * 0.5f, ry = (y1 - y0) * 0.5f;
        for (int y = Math.max(0, iy0); y < Math.min(f.h, iy1); y++)
            for (int x = Math.max(0, ix0); x < Math.min(f.w, ix1); x++) {
                // the tube's corners rounded off, darker toward them
                float qx = (x + 0.5f - cx) / rx, qy = (y + 0.5f - cy) / ry;
                float r = qx * qx * qx * qx + qy * qy * qy * qy;
                if (r > 1f) continue;
                float dim = 1f - 0.45f * r;
                if ((y & 1) == 1) dim *= 0.82f;
                int g = Math.round(42 * dim), b = Math.round(46 * dim), red = Math.round(12 * dim);
                f.a[y * f.w + x] = 0xFF000000 | (red << 16) | (g << 8) | b;
            }
        // the lines, as far as typed; the last one blinks once all is typed
        int scale = Math.max(1, (int) ((x1 - x0) / 185f));
        int lineH = 9 * scale, top = iy0 + Math.max(4, Math.round((iy1 - iy0 - lines.length * lineH) * 0.5f));
        int left = ix0 + 6 * scale;
        int shown = (int) typed;
        for (int i = 0; i < lines.length; i++) {
            String s = lines[i];
            if (shown <= 0) break;
            String part = s.length() <= shown ? s : s.substring(0, shown);
            shown -= s.length();
            boolean last = i == lines.length - 1;
            if (last && typed >= letters && (int) (t * 2.2f) % 2 == 1) continue;
            int col = last ? 0xFFF6D36A : 0xFFA6F2DA;
            int y = top + i * lineH;
            if (y + 7 * scale > iy1 - 2) break;
            PixelFont.draw(f, part, left + 1, y + 1, scale, 0x55102A28);
            PixelFont.draw(f, part, left, y, scale, col);
            // the cursor
            if (shown < 0 && typed < letters)
                f.rect(left + PixelFont.width(part, scale) + scale, y, left + PixelFont.width(part, scale) + 5 * scale, y + 7 * scale, col);
        }
    }

    /** The clock on the desk: the real time, in orange. */
    private void clock(DrawList d, Px f) {
        if (!d.project(CX, CY, CZ - 0.005f, at)) return;
        Calendar c = Calendar.getInstance();
        int hh = c.get(Calendar.HOUR_OF_DAY), mm = c.get(Calendar.MINUTE);
        String s = (hh < 10 ? "0" : "") + hh + (c.get(Calendar.SECOND) % 2 == 0 ? ":" : " ") + (mm < 10 ? "0" : "") + mm;
        int w = PixelFont.width(s, 1);
        PixelFont.draw(f, s, Math.round(at[0] - w * 0.5f), Math.round(at[1] - 3.5f), 1, 0xFFFF8A3C);
    }

    // ---- the room, once ----

    /** A turn about the upright, into a fresh array of rows. */
    private static float[] yaw(float a) {
        float c = (float) Math.cos(a), s = (float) Math.sin(a);
        return new float[]{c, 0f, s, 0f, 1f, 0f, -s, 0f, c};
    }

    private static void build(DrawList d) {
        int plaster = 0xE6D7BC, frame = 0xF1EBDD;
        // the floor, boards; the walls, plaster; the window in the back wall over the desk
        d.box(0f, -0.05f, 0f, 3f, 0.05f, 3f, null, 0x9A6B44, DrawList.WOOD);
        d.box(-2.95f, 0f, 0.9f, 2.05f, 1.5f, 0.06f, null, plaster, DrawList.PLAIN);
        d.box(2.95f, 0f, 0.9f, 2.05f, 1.5f, 0.06f, null, plaster, DrawList.PLAIN);
        d.box(0f, 0.72f, 0.9f, 0.9f, 0.72f, 0.06f, null, plaster, DrawList.PLAIN);
        d.box(0f, 2.95f, 0.9f, 0.9f, 0.35f, 0.06f, null, plaster, DrawList.PLAIN);
        d.box(-2.2f, 1.5f, -1f, 0.06f, 1.5f, 2f, null, plaster, DrawList.PLAIN);
        d.box(2.2f, 1.5f, -1f, 0.06f, 1.5f, 2f, null, plaster, DrawList.PLAIN);
        // the window: a frame, the bars across, a sill
        d.box(0f, 1.45f, 0.86f, 0.95f, 0.04f, 0.1f, null, frame, DrawList.PLAIN);
        d.box(0f, 2.6f, 0.9f, 0.95f, 0.04f, 0.08f, null, frame, DrawList.PLAIN);
        d.box(-0.92f, 2.02f, 0.9f, 0.04f, 0.6f, 0.08f, null, frame, DrawList.PLAIN);
        d.box(0.92f, 2.02f, 0.9f, 0.04f, 0.6f, 0.08f, null, frame, DrawList.PLAIN);
        d.box(0f, 2.02f, 0.92f, 0.025f, 0.58f, 0.03f, null, frame, DrawList.PLAIN);
        d.box(0f, 2.1f, 0.92f, 0.9f, 0.02f, 0.03f, null, frame, DrawList.PLAIN);
        // outside, far below: the sea out to the haze, the promenade's edge, a few roofs
        d.box(0f, -14f, 500f, 900f, 0.25f, 480f, null, 0x2D6990, DrawList.SEA);
        d.box(0f, -13.2f, 22f, 60f, 0.8f, 4f, null, 0xDCCDB2, DrawList.PROMENADE);
        for (int i = 0; i < 7; i++) {
            float x = -30f + i * 10f + (i % 3) * 2f, hgt = 3f + (i * 37 % 5);
            d.box(x, -14f + hgt, 10f + (i % 2) * 4f, 4f, hgt, 3f, null, Street.WALLS[i % Street.WALLS.length],
                    DrawList.material(DrawList.FACADE, 200 + i));
            d.box(x, -14f + 2f * hgt + 0.3f, 10f + (i % 2) * 4f, 3.8f, 0.3f, 2.8f, null, 0xC4673E, DrawList.GRAIN);
        }
        // the desk
        d.box(0f, 0.755f, 0.05f, 1.15f, 0.025f, 0.6f, null, 0x8A5A36, DrawList.WOOD);
        d.box(0f, 0.38f, -0.5f, 1.12f, 0.35f, 0.02f, null, 0x7A4E2E, DrawList.WOOD);
        for (int s = -1; s <= 1; s += 2) d.box(s * 1.08f, 0.38f, 0.05f, 0.03f, 0.37f, 0.55f, null, 0x7A4E2E, DrawList.WOOD);
        // the set: a cabinet of grey plastic, a dark bezel, the glass, two knobs and a grille beside it
        d.box(0f, 1.05f, 0.36f, 0.34f, 0.27f, 0.29f, null, 0xBDB5A6, DrawList.PLAIN);
        d.box(0f, 1.05f, 0.08f, 0.335f, 0.265f, 0.012f, null, 0x34322F, DrawList.PLAIN);
        d.box(GX, GY, GZ, GHX + 0.01f, GHY + 0.01f, 0.01f, null, 0x0C2A2E, DrawList.GLASS);
        d.box(0.275f, 1.2f, 0.06f, 0.022f, 0.022f, 0.02f, null, 0xC9C2B2, DrawList.PLAIN);
        d.box(0.275f, 1.12f, 0.06f, 0.022f, 0.022f, 0.02f, null, 0xC9C2B2, DrawList.PLAIN);
        for (int k = 0; k < 5; k++) d.box(0.275f, 0.98f - k * 0.025f, 0.065f, 0.035f, 0.004f, 0.005f, null, 0x1C1B1A, DrawList.PLAIN);
        d.box(0f, 0.79f, 0.36f, 0.3f, 0.012f, 0.25f, null, 0x2A2826, DrawList.PLAIN);
        // the panel of keys before it: a row of pale keys, a red one, a yellow one
        d.box(0.02f, 0.8f, -0.24f, 0.36f, 0.025f, 0.12f, null, 0xCFC6B4, DrawList.PLAIN);
        for (int k = 0; k < 6; k++)
            d.box(-0.24f + k * 0.075f, 0.835f, -0.27f, 0.028f, 0.012f, 0.04f, null, 0xEDE6D6, DrawList.PLAIN);
        d.box(0.25f, 0.84f, -0.24f, 0.06f, 0.016f, 0.07f, null, 0xC5372B, DrawList.PLAIN);
        d.box(-0.24f, 0.835f, -0.17f, 0.1f, 0.012f, 0.025f, null, 0xF0C03A, DrawList.PLAIN);
        // the clock on top of the set: a dark case, its face
        d.box(CX, CY, CZ + 0.08f, 0.13f, 0.06f, 0.08f, null, 0x2C2A28, DrawList.PLAIN);
        d.box(CX, CY, CZ, 0.1f, 0.035f, 0.004f, null, 0x120C0A, DrawList.GLASS);
        // the lamp: a heavy foot, an arm, a green shade, the bulb
        d.box(LAMP_X, 0.79f, LAMP_Z + 0.1f, 0.09f, 0.015f, 0.09f, null, 0x2F3A33, DrawList.PLAIN);
        d.box(LAMP_X, 1.03f, LAMP_Z + 0.1f, 0.012f, 0.24f, 0.012f, null, 0x8E8A80, DrawList.PLAIN);
        d.box(LAMP_X, LAMP_Y + 0.04f, LAMP_Z, 0.13f, 0.05f, 0.1f, null, 0x2E5E44, DrawList.PLAIN);
        d.box(LAMP_X, LAMP_Y - 0.02f, LAMP_Z, 0.04f, 0.02f, 0.04f, null, 0xFFF1C8, DrawList.GLOW);
        // papers, a pencil, a mug, before the keys
        d.box(-0.2f, 0.785f, -0.46f, 0.15f, 0.003f, 0.1f, yaw(0.25f), 0xF4EFE4, DrawList.PLAIN);
        d.box(-0.14f, 0.789f, -0.47f, 0.15f, 0.003f, 0.1f, yaw(-0.12f), 0xEDE6D4, DrawList.PLAIN);
        d.box(-0.05f, 0.795f, -0.5f, 0.08f, 0.005f, 0.005f, yaw(0.6f), 0xD9A43A, DrawList.PLAIN);
        d.box(0.27f, 0.84f, -0.46f, 0.045f, 0.06f, 0.045f, null, 0xF1EEE6, DrawList.PLAIN);
        d.box(0.27f, 0.9f, -0.46f, 0.038f, 0.002f, 0.038f, null, 0x3A2416, DrawList.PLAIN);
        d.box(0.32f, 0.85f, -0.46f, 0.012f, 0.03f, 0.01f, null, 0xF1EEE6, DrawList.PLAIN);
    }
}
