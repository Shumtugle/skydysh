package com.shumtugle.skydysh;

import java.util.Calendar;

/**
 * Between rounds. The street stays where it is and goes still; from above and below the
 * dispatcher's panel closes in over it, until the street is left only on the old set in the
 * middle of it. A cream panel inked in navy: at the top the clock and the date (the real ones,
 * the player's own) in orange segments; the set, a navy box with a teal rim, the street on its
 * glass; under it the level in one big orange number; a key that goes on, a key for the hammer
 * given; under those a key back with the number before, and a speaker grille.
 *
 * No hand is drawn: the hammer is the hand. It comes up from the lower right, drawn flat like
 * the panel, and knocks the keys one after another, each going in with a click and lighting up:
 * the hammer it gives, then the one that goes on. The set flicks over to the next channel, the
 * number turns to the next level, and the panel draws back off the street, where the next
 * building is already coming down.
 *
 * Drawn on a layer finer than the frame: the still parts once, the moving parts (digits, keys,
 * the glass, the hammer) over them every frame.
 */
final class Console {
    // the keys
    static final int NEXT = 0, HAMMER = 1;

    /** Layer pixels to a frame pixel. */
    static final int Q = 2;

    // where things are on the panel, frame pixels (the panel is 274 x 640)
    private static final int PW = 274, PH = 640;
    private static final float[] TUBE = {23, 96, 252, 269};             // the glass: x0, y0, x1, y1
    private static final float SPLIT = 182f;                            // above it comes down, below it comes up
    private static final float[][] KEY = {{16, 401, 169, 473}, {183, 401, 259, 473}};
    // where on each key the hammer comes down: toward the right of the wide one, clear of the number
    private static final float[] STRIKE = {158, 221};
    private static final float[] BACK = {16, 504, 148, 576}, GRILLE = {162, 504, 259, 576};
    private static final float[] CLOCK = {61, 40, 215, 76}, NUMBER = {79, 303, 197, 384};

    private static final int NAVY = 0xFF232A4A, CREAM = 0xFFE9DDC3, CREAM_DOT = 0xFFD9CBAD, TEAL = 0xFF3C8C96,
            ORANGE = 0xFFE8622E, ORANGE_DARK = 0xFFB0431C, KEY_FACE = 0xFFD8CDB6,
            KEY_LIP = 0xFFB7AA8E, KEY_SIGN = 0xFF8E8A86, YELLOW = 0xFFF4C23C, YELLOW_LIP = 0xFFD39A22,
            GRILLE_FACE = 0xFFCFBA92, DARK = 0xFF1B2038;

    // the timeline, seconds: the hammer coming up, each knock, the channel flicking over
    static final float RISE = 0.3f, KNOCK = 0.42f, FLICK = 0.18f;
    private static final float MOVE = 0.18f, DOWN = 0.08f, STAY = 0.06f;

    private int[] art;
    private final int[] press = new int[4];
    private int presses;
    private int giftKind = -1, handKind;
    private int shown, then;                          // the level on the counter now, and after the flick
    private final boolean[] lit = new boolean[2];
    private final float[] hitAt = new float[2];
    private float t = -1f;                            // seconds into the knocking, below 0 not yet
    private int heard;                                // knocks already sounded
    private final java.util.Random rnd = new java.util.Random(11);

    /** Kept for the shell's call: the panel is drawn, not read. */
    void read(byte[] b) {
    }

    /**
     * A new visit: the keys to knock, in order (the last is NEXT); the hammer in hand, and the
     * one given, if any (its kind lights the hammer key); the level shown now and after the
     * flick, as the counter counts them, from one.
     */
    void begin(int[] keys, int n, int handKind, int gift, int levelNow, int levelThen) {
        presses = Math.min(n, press.length);
        System.arraycopy(keys, 0, press, 0, presses);
        giftKind = gift;
        this.handKind = handKind;
        shown = levelNow;
        then = levelThen;
        java.util.Arrays.fill(lit, false);
        java.util.Arrays.fill(hitAt, -9f);
        t = 0f;
        heard = 0;
    }

    /** How long the knocking and the flick take, seconds. */
    float length() {
        return RISE + presses * KNOCK + FLICK;
    }

    /** Moves on; returns the key knocked just now, or -1. */
    int update(float dt) {
        if (t < 0f) return -1;
        t += dt;
        if (heard < presses && t >= RISE + heard * KNOCK + MOVE + DOWN) {
            int b = press[heard];
            lit[b] = true;
            hitAt[b] = t;
            heard++;
            return b;
        }
        return -1;
    }

    /** Whether the set has flicked over to the next channel. */
    boolean flicked() {
        return t >= RISE + presses * KNOCK;
    }

    void stop() {
        t = -1f;
    }

    // ---- the still panel, drawn once ----

    private void build() {
        Ink g = new Ink(PW * Q, PH * Q);
        // the panel, cream with a fine screen of dots, inked round with rounded corners
        g.box(0, 0, PW * Q, PH * Q, 0, DARK, DARK, 1f, 1f);
        g.box(5 * Q, 5 * Q, (PW - 5) * Q, (PH - 5) * Q, 18 * Q, NAVY, NAVY, 1f, 1f);
        g.box(6.5f * Q, 6.5f * Q, (PW - 6.5f) * Q, (PH - 6.5f) * Q, 16.5f * Q, CREAM, CREAM, 1f, 1f);
        for (int y = 9; y < PH - 9; y += 4)
            for (int x = 9 + (y / 4 % 2) * 2; x < PW - 9; x += 4) g.disc(x * Q, y * Q, 0.55f * Q, CREAM_DOT, 1f, 0.8f);
        // the clock's window
        box(g, CLOCK, 9, NAVY);
        // the set: a teal rim, the navy box, the glass left clear
        float x0 = TUBE[0] - 11, y0 = TUBE[1] - 10, x1 = TUBE[2] + 11, y1 = TUBE[3] + 22;
        g.box((x0 - 1.6f) * Q, (y0 - 1.6f) * Q, (x1 + 1.6f) * Q, (y1 + 1.6f) * Q, 22 * Q, TEAL, TEAL, 1f, 1f);
        g.box(x0 * Q, y0 * Q, x1 * Q, y1 * Q, 20 * Q, NAVY, NAVY, 1f, 1f);
        g.disc((x0 + 13) * Q, (y1 - 10) * Q, 2.6f * Q, 0xFF7FE0A0, 1f, 1f);
        g.line((x1 - 52) * Q, (y1 - 12) * Q, (x1 - 16) * Q, (y1 - 12) * Q, 1.2f * Q, 0xFF4E5A80, 1f);
        g.line((x1 - 52) * Q, (y1 - 8) * Q, (x1 - 16) * Q, (y1 - 8) * Q, 1.2f * Q, 0xFF4E5A80, 1f);
        // the level's window
        box(g, NUMBER, 13, NAVY);
        // the speaker: a tan face inked round, holes in rows
        float[] s = GRILLE;
        g.box((s[0] - 1.5f) * Q, (s[1] - 1.5f) * Q, (s[2] + 1.5f) * Q, (s[3] + 1.5f) * Q, 11.5f * Q, NAVY, NAVY, 1f, 1f);
        g.box(s[0] * Q, s[1] * Q, s[2] * Q, s[3] * Q, 10 * Q, GRILLE_FACE, GRILLE_FACE, 1f, 1f);
        for (int r = 0; r < 5; r++)
            for (int c = 0; c < 7; c++) {
                float cx = s[0] + 13 + c * ((s[2] - s[0] - 26) / 6f), cy = s[1] + 12 + r * ((s[3] - s[1] - 24) / 4f);
                g.disc(cx * Q, cy * Q, 3f * Q, NAVY, 1f, 1f);
                g.disc((cx + 0.6f) * Q, (cy + 0.8f) * Q, 1.6f * Q, 0xFF3A4060, 1f, 1f);
            }
        // the key back, never lit: a bar and a point to the left
        keyFace(g, BACK, false, false);
        float kx = BACK[0] + 26, ky = (BACK[1] + BACK[3]) * 0.5f - 2;
        g.box((kx - 9) * Q, (ky - 10) * Q, (kx - 5.5f) * Q, (ky + 10) * Q, 1.5f * Q, KEY_SIGN, KEY_SIGN, 1f, 1f);
        g.shape((kx + 10) * Q, (ky - 10) * Q, (kx - 4) * Q, ky * Q, (kx + 10) * Q, (ky + 10) * Q);
        g.polyFill(KEY_SIGN, KEY_SIGN, 1f, 1f);
        // the glass left clear
        clearRound(g, TUBE, 15);
        art = g.a;
    }

    private static void box(Ink g, float[] r, float round, int c) {
        g.box(r[0] * Q, r[1] * Q, r[2] * Q, r[3] * Q, round * Q, c, c, 1f, 1f);
    }

    /** A rounded opening cut clean through. */
    private static void clearRound(Ink g, float[] r, float round) {
        for (int y = Math.round(r[1] * Q); y < Math.round(r[3] * Q); y++)
            for (int x = Math.round(r[0] * Q); x < Math.round(r[2] * Q); x++) {
                float px = (x + 0.5f) / Q, py = (y + 0.5f) / Q;
                float qx = Math.max(Math.max(r[0] + round - px, px - (r[2] - round)), 0f);
                float qy = Math.max(Math.max(r[1] + round - py, py - (r[3] - round)), 0f);
                float d = (float) Math.hypot(qx, qy) - round;
                if (d > 0.5f) continue;
                if (d > -0.5f) {
                    // the rim: half way
                    int c = g.a[y * g.w + x], k = Math.round((d + 0.5f) * 256f);
                    g.a[y * g.w + x] = ((((c >>> 24) * k) >> 8) << 24) | ((((c >> 16) & 0xFF) * k >> 8) << 16)
                            | ((((c >> 8) & 0xFF) * k >> 8) << 8) | ((c & 0xFF) * k >> 8);
                } else g.a[y * g.w + x] = 0;
            }
    }

    /** A key's face: its lip below, its top; lit yellow, or pale; pressed in. */
    private static void keyFace(Ink g, float[] r, boolean on, boolean in) {
        float dy = in ? 3f : 0f;
        int face = on ? YELLOW : KEY_FACE, lip = on ? YELLOW_LIP : KEY_LIP;
        if (on) g.box((r[0] - 3) * Q, (r[1] - 3 + dy) * Q, (r[2] + 3) * Q, (r[3] + 4) * Q, 14 * Q, 0xFFF0A060, 0xFFF0A060, 3f * Q, 0.55f);
        g.box(r[0] * Q, (r[1] + 4) * Q, r[2] * Q, (r[3] + 4) * Q, 11 * Q, lip, lip, 1f, 1f);
        g.box(r[0] * Q, (r[1] + dy) * Q, r[2] * Q, (r[3] - 2 + dy) * Q, 11 * Q, face, face, 1f, 1f);
    }

    // ---- drawing, on the fine layer ----

    /**
     * The panel over the frame, as far as it has closed in (0: not at all, 1: only the glass
     * left), and the hammer, as far as it is up (0 to 1); on the fine layer, which is cleared for it.
     */
    void draw(Ink g, int fh, float cover, float handUp) {
        if (cover <= 0f) return;
        if (art == null) build();
        g.clear();
        float oy = (fh - PH) / 2f;                      // the panel centred on the frame
        float split = SPLIT + oy;
        float e = ease(cover);
        float top = -(1f - e) * split, bot = (1f - e) * (fh - split);
        int topQ = Math.round(top * Q), botQ = Math.round(bot * Q), oyQ = Math.round(oy * Q), splitQ = Math.round(split * Q);
        int[] a = g.a;
        int gw = g.w, aw = PW * Q, ah = PH * Q;
        for (int y = 0; y < g.h; y++) {
            boolean upper = y < splitQ + topQ, lower = y >= splitQ + botQ;
            if (!upper && !lower) continue;             // the gap between, where the street still shows whole
            int sy = (upper ? y - topQ : y - botQ) - oyQ, row = y * gw;
            if (sy < 0 || sy >= ah) java.util.Arrays.fill(a, row, row + gw, DARK);
            else System.arraycopy(art, sy * aw, a, row, Math.min(aw, gw));
        }
        g.used = true;
        tube(g, oyQ, e);
        float dTop = top + oy, dBot = bot + oy;         // where the moving halves are, in frame pixels
        clock(g, dTop);
        keys(g, dBot);
        counter(g, dBot);
        if (handUp > 0f) hammer(g, fh, cover, handUp);
    }

    /** Where the lower half of the panel is, in frame pixels, as far as it has closed in. */
    private static float lowerAt(int fh, float cover) {
        float oy = (fh - PH) / 2f;
        return (1f - ease(cover)) * (fh - SPLIT - oy) + oy;
    }

    // the glass, worked out once: how dark each pixel of it is, and whether pale
    private int[] glassFull;
    private byte[] glassA;
    private boolean[] glassW;
    private int gx0, gy0, gw, gh;

    /** The glass: fine scan lines, darker toward its corners, a pale sheen at the top left; a moment of noise as it flicks. */
    private void tube(Ink g, int oyQ, float e) {
        int y0 = Math.max(0, Math.round(TUBE[1] * Q) + oyQ), y1 = Math.min(g.h, Math.round(TUBE[3] * Q) + oyQ);
        int x0 = Math.max(0, Math.round(TUBE[0] * Q)), x1 = Math.min(g.w, Math.round(TUBE[2] * Q));
        if (glassA == null || gx0 != x0 || gy0 != y0 || gw != x1 - x0 || gh != y1 - y0) {
            gx0 = x0;
            gy0 = y0;
            gw = Math.max(1, x1 - x0);
            gh = Math.max(1, y1 - y0);
            glassA = new byte[gw * gh];
            glassW = new boolean[gw * gh];
            glassFull = new int[gw * gh];
            float cx = (TUBE[0] + TUBE[2]) * 0.5f * Q, cy = (TUBE[1] + TUBE[3]) * 0.5f * Q + oyQ;
            float hx = (TUBE[2] - TUBE[0]) * 0.5f * Q, hy = (TUBE[3] - TUBE[1]) * 0.5f * Q;
            for (int y = y0; y < y1; y++) {
                float v = (y - cy) / hy;
                for (int x = x0; x < x1; x++) {
                    float u = (x - cx) / hx;
                    float dark = 0.06f + 0.22f * (float) Math.pow(u * u * 0.6f + v * v * 0.8f, 1.6);
                    if ((y / Q) % 2 == 1) dark += 0.08f;
                    float sheen = Math.max(0f, 1f - (float) Math.hypot(u + 0.7f, v + 0.75f) / 0.9f);
                    sheen = sheen * sheen * 0.1f;
                    int k = (y - y0) * gw + (x - x0);
                    int ai = Math.round(Math.min(1f, dark + sheen) * 255f);
                    glassA[k] = (byte) ai;
                    glassW[k] = sheen > dark;
                    int w = glassW[k] ? ai : 0;
                    glassFull[k] = (ai << 24) | (w << 16) | (w << 8) | w;
                }
            }
        }
        boolean noise = t >= 0f && flicked() && t < RISE + presses * KNOCK + FLICK;
        // the glass only once the panel is all but closed: before that the street shows plain
        int ek = Math.round(Math.max(0f, Math.min(1f, (e - 0.85f) / 0.15f)) * 256f);
        int[] a = g.a;
        for (int y = y0; y < y1; y++) {
            int row = y * g.w, grow = (y - y0) * gw - x0;
            for (int x = x0; x < x1; x++) {
                int k = row + x, d = a[k], da = d >>> 24;
                if (da == 255) continue;
                int glass;
                if (da == 0 && ek == 256 && !noise) {
                    a[k] = glassFull[grow + x];
                    continue;
                }
                if (noise) {
                    int c = (60 + rnd.nextInt(170)) * 0xE6 / 255;
                    glass = 0xE6000000 | (c << 16) | (c << 8) | c;
                } else {
                    int ai = ((glassA[grow + x] & 0xFF) * ek) >> 8, w = glassW[grow + x] ? ai : 0;
                    glass = (ai << 24) | (w << 16) | (w << 8) | w;
                }
                int keep = 255 - da;
                int ra = da + ((glass >>> 24) * keep + 127) / 255;
                int rr = ((d >> 16) & 0xFF) + (((glass >> 16) & 0xFF) * keep + 127) / 255;
                int rg = ((d >> 8) & 0xFF) + (((glass >> 8) & 0xFF) * keep + 127) / 255;
                int rb = (d & 0xFF) + ((glass & 0xFF) * keep + 127) / 255;
                a[k] = (Math.min(255, ra) << 24) | (Math.min(255, rr) << 16) | (Math.min(255, rg) << 8) | Math.min(255, rb);
            }
        }
    }

    // ---- what is drawn once and laid down again: digits and keys ----

    private final java.util.HashMap<Integer, Ink> sprites = new java.util.HashMap<Integer, Ink>();
    private static final float PAD = 4f;
    // the kinds of digit: the clock's, the date's, the level's
    private static final float[] DW = {17, 9, 30}, DH = {26, 14, 54}, DT = {3.6f, 2f, 7f};

    /** A digit of a kind, its top left at (x, y) in frame pixels; below 0, all dark. */
    private void digitAt(Ink g, int kind, int n, float x, float y) {
        int key = 1000 + kind * 16 + n + 1;
        Ink sp = sprites.get(key);
        if (sp == null) {
            sp = new Ink(Math.round((DW[kind] + 2 * PAD) * Q), Math.round((DH[kind] + 2 * PAD) * Q));
            if (kind == 2) {
                // the level's: orange, inked darker round
                seven(sp, PAD, PAD, DW[kind], DH[kind], DT[kind] + 2.4f, n, ORANGE_DARK, 0);
                seven(sp, PAD, PAD, DW[kind], DH[kind], DT[kind], n, ORANGE, 0);
            } else seven(sp, PAD, PAD, DW[kind], DH[kind], DT[kind], n, ORANGE, 0);
            sprites.put(key, sp);
        }
        g.blit(sp, (x - PAD) * Q, (y - PAD) * Q, 1f);
    }

    /** The clock and the date: the player's own, in orange segments. */
    private void clock(Ink g, float dy) {
        Calendar c = Calendar.getInstance();
        int hh = c.get(Calendar.HOUR_OF_DAY), mm = c.get(Calendar.MINUTE);
        int dd = c.get(Calendar.DAY_OF_MONTH), mo = c.get(Calendar.MONTH) + 1;
        float y = CLOCK[1] + 5 + dy, x = CLOCK[0] + 10;
        digitAt(g, 0, hh / 10, x, y);
        digitAt(g, 0, hh % 10, x + 21, y);
        // the colon, blinking with the seconds
        if (c.get(Calendar.SECOND) % 2 == 0)
            for (float cy : new float[]{y + 8, y + 18}) g.disc((x + 42.5f) * Q, cy * Q, 1.8f * Q, ORANGE, 1f, 1f);
        digitAt(g, 0, mm / 10, x + 48, y);
        digitAt(g, 0, mm % 10, x + 69, y);
        float dyy = y + 10, dx = x + 100;
        digitAt(g, 1, dd / 10, dx, dyy);
        digitAt(g, 1, dd % 10, dx + 11, dyy);
        g.disc((dx + 22.5f) * Q, (dyy + 13) * Q, 1.2f * Q, ORANGE, 1f, 1f);
        digitAt(g, 1, mo / 10, dx + 25, dyy);
        digitAt(g, 1, mo % 10, dx + 36, dyy);
    }

    /**
     * A digit of seven segments, each a long six-sided bar with pointed ends, a hair apart.
     * In frame pixels; n below 0 lights none; off segments in their own colour, or none (0).
     */
    private static void seven(Ink g, float x, float y, float w, float h, float t, int n, int on, int off) {
        String lit = n >= 0 && n <= 9 ? SEG[n] : "";
        float gap = t * 0.16f, hf = t * 0.5f, m = y + h * 0.5f;
        for (int k = 0; k < 7; k++) {
            char ch = "agdfbec".charAt(k);
            boolean l = lit.indexOf(ch) >= 0;
            if (!l && off == 0) continue;
            float ax, ay, bx, by;
            switch (ch) {
                case 'a': ax = x + hf; ay = y + hf; bx = x + w - hf; by = ay; break;
                case 'g': ax = x + hf; ay = m; bx = x + w - hf; by = m; break;
                case 'd': ax = x + hf; ay = y + h - hf; bx = x + w - hf; by = ay; break;
                case 'f': ax = x + hf; ay = y + hf; bx = ax; by = m; break;
                case 'b': ax = x + w - hf; ay = y + hf; bx = ax; by = m; break;
                case 'e': ax = x + hf; ay = m; bx = ax; by = y + h - hf; break;
                default: ax = x + w - hf; ay = m; bx = ax; by = y + h - hf; break;
            }
            bar7(g, ax, ay, bx, by, hf, gap);
            g.polyFill(l ? on : off, l ? on : off, 1f, 1f);
        }
    }

    /** A segment from (ax, ay) to (bx, by), level or upright, so thick, pointed at its ends and drawn in a little. */
    private static void bar7(Ink g, float ax, float ay, float bx, float by, float hf, float gap) {
        if (ay == by) {
            float x0 = ax + gap, x1 = bx - gap;
            g.shape(x0 * Q, ay * Q, (x0 + hf) * Q, (ay - hf) * Q, (x1 - hf) * Q, (ay - hf) * Q, x1 * Q, ay * Q,
                    (x1 - hf) * Q, (ay + hf) * Q, (x0 + hf) * Q, (ay + hf) * Q);
        } else {
            float y0 = ay + gap, y1 = by - gap;
            g.shape(ax * Q, y0 * Q, (ax + hf) * Q, (y0 + hf) * Q, (ax + hf) * Q, (y1 - hf) * Q, ax * Q, y1 * Q,
                    (ax - hf) * Q, (y1 - hf) * Q, (ax - hf) * Q, (y0 + hf) * Q);
        }
    }

    /** The keys: pale until knocked, then lit yellow; the one just knocked in, with strokes of the blow round it. */
    private void keys(Ink g, float dy) {
        for (int i = 0; i < 2; i++) {
            float[] r = KEY[i];
            float since = t - hitAt[i];
            boolean in = since >= 0f && since < 0.15f;
            int gift = i == HAMMER && giftKind >= 0 ? giftKind : -1;
            int key = 2000 + i * 64 + (lit[i] ? 32 : 0) + (in ? 16 : 0) + gift + 1;
            Ink sp = sprites.get(key);
            if (sp == null) {
                float w = r[2] - r[0], h = r[3] - r[1];
                sp = new Ink(Math.round((w + 12) * Q), Math.round((h + 14) * Q));
                float[] at = {6, 6, 6 + w, 6 + h};
                keyFace(sp, at, lit[i], in);
                float cx = 6 + w * 0.5f, cy = 6 + h * 0.5f + (in ? 3f : 0f) - 2f;
                int c = lit[i] ? 0xFF7A4A1E : KEY_SIGN;
                if (i == NEXT) {
                    sp.shape((cx - 8) * Q, (cy - 12) * Q, (cx + 12) * Q, cy * Q, (cx - 8) * Q, (cy + 12) * Q);
                    sp.polyFill(c, c, 1f, 1f);
                } else {
                    Hammer.iconAt(sp, cx * Q, (cy - 4) * Q, 40f * Q, Hammer.HEAVY_KIND, c & 0xFFFFFF, 1f, 75f);
                    // the dots: how much it gives, the colour of the hammer given
                    int dc = gift >= 0 && lit[i] ? 0xFF000000 | Hammer.head(gift) : lit[i] ? ORANGE : 0xFFB3A78E;
                    if (gift == Hammer.PLAIN || gift == Hammer.HEAVY_KIND) dc = lit[i] ? ORANGE : dc;
                    for (int k = -1; k <= 1; k++) sp.disc((cx + k * 8f) * Q, (cy + 21f) * Q, 2.6f * Q, dc, 1f, 1f);
                }
                sprites.put(key, sp);
            }
            g.blit(sp, (r[0] - 6) * Q, (r[1] - 6 + dy) * Q, 1f);
            // strokes of the blow, flying off the key's upper corner
            if (since >= 0f && since < 0.4f) {
                float u = since / 0.4f, cover = 1f - u;
                float cx = r[0] - 2, cy = r[1] + dy - 2;
                for (int k = 0; k < 3; k++) {
                    double an = Math.toRadians(200 + k * 35);
                    float r0 = 5f + u * 6f, r1 = r0 + 7f;
                    g.line((cx + (float) Math.cos(an) * r0) * Q, (cy - (float) Math.sin(an) * r0) * Q,
                            (cx + (float) Math.cos(an) * r1) * Q, (cy - (float) Math.sin(an) * r1) * Q, 2.4f * Q, ORANGE, cover);
                }
            }
        }
    }

    /** The level, big; and on the key back the one before it. */
    private void counter(Ink g, float dy) {
        int n = flicked() ? then : shown;
        String s = String.valueOf(n);
        float w = DW[2] + 8f, x = (NUMBER[0] + NUMBER[2]) * 0.5f - (s.length() * w - 8f) * 0.5f;
        float y = (NUMBER[1] + NUMBER[3]) * 0.5f - DH[2] * 0.5f + dy;
        for (int i = 0; i < s.length(); i++) digitAt(g, 2, s.charAt(i) - '0', x + i * w, y);
        int back = n - 1;
        if (back >= 1) {
            String b = String.valueOf(back);
            float th = 22f;
            g.digits(b, (BACK[0] + 52) * Q, ((BACK[1] + BACK[3]) * 0.5f - th * 0.5f - 2 + dy) * Q, th * Q, KEY_SIGN, KEY_SIGN, 0.5f * Q, 1f);
        }
    }

    // ---- the hammer ----

    /**
     * The hand's hammer, drawn flat on the panel: up from the lower right, over the key it is
     * going for, down onto it, up again. Its striking end comes down on the key's middle.
     */
    private void hammer(Ink g, int fh, float cover, float handUp) {
        if (t < 0f || presses == 0) return;
        float dy = lowerAt(fh, cover);
        float restX = 330f, restY = fh + 90f;
        float x, y;
        float k = t - RISE;
        int i = k < 0f ? 0 : Math.min(presses - 1, (int) (k / KNOCK));
        float in = k < 0f ? 0f : k - i * KNOCK;
        int b = press[i];
        float ax = STRIKE[b], ay = (KEY[b][1] + KEY[b][3]) * 0.5f + dy;
        float hx = ax + 6f, hy = ay - 34f;
        float tilt;
        if (k < 0f) {
            float e = ease(t / RISE);
            x = restX + (hx - restX) * e;
            y = restY + (hy - restY) * e;
            tilt = 0f;
        } else if (in < MOVE) {
            float px = i == 0 ? hx : STRIKE[press[i - 1]] + 6f;
            float e = ease(in / MOVE);
            x = px + (hx - px) * e;
            y = hy;
            tilt = 0f;
        } else if (in < MOVE + DOWN) {
            float e = (in - MOVE) / DOWN;
            x = hx + (ax - hx) * e * e;
            y = hy + (ay - hy) * e * e;
            tilt = 12f * e * e;
        } else if (in < MOVE + DOWN + STAY) {
            x = ax;
            y = ay;
            tilt = 12f;
        } else {
            float e = ease((in - MOVE - DOWN - STAY) / (KNOCK - MOVE - DOWN - STAY));
            x = ax + (hx - ax) * e;
            y = ay + (hy - ay) * e;
            tilt = 12f * (1f - e);
        }
        float e = ease(handUp);
        x = restX + (x - restX) * e;
        y = restY + (y - restY) * e;
        // (x, y) is where the striking end comes down: the head is up and to the right of it
        float angle = 158f - tilt, len = 205f;
        if (hammerKind != handKind || hammerInk == null) {
            // drawn once, lying flat, with its shade; turned into place every frame after
            hammerKind = handKind;
            hammerInk = flatHammer(len * Q, handKind, -2, hammerAt);
            hammerShade = flatHammer(len * Q, handKind, NAVY & 0xFFFFFF, shadeAt);
        }
        g.blitTurned(hammerShade, shadeAt[0], shadeAt[1], (x + 3f) * Q, (y + 6f) * Q, angle, 0.25f);
        g.blitTurned(hammerInk, hammerAt[0], hammerAt[1], x * Q, y * Q, angle, 1f);
    }

    private Ink hammerInk, hammerShade;
    private int hammerKind = -99;
    private final float[] hammerAt = new float[2], shadeAt = new float[2];

    /**
     * The hammer drawn lying flat (handle to the right) and cut out; {@code strike} gets where on
     * the cut the far end of its striking cap is, the point it is placed by.
     */
    private static Ink flatHammer(float length, int kind, int one, float[] strike) {
        int side = Math.round(length * 2.4f);
        Ink big = new Ink(side, side);
        float cx = side * 0.5f, cy = side * 0.5f;
        Hammer.strikeAt(big, cx, cy, length, kind, one, 1f, 0f);
        int[] at = new int[2];
        Ink cut = big.crop(at);
        strike[0] = cx - at[0];
        strike[1] = cy - at[1];
        return cut;
    }

    // which segments light for each digit: a top, b upper right, c lower right, d bottom, e lower left, f upper left, g middle
    private static final String[] SEG = {"abcdef", "bc", "abged", "abgcd", "fgbc", "afgcd", "afgedc", "abc", "abcdefg", "abcfgd"};

    private static float ease(float x) {
        x = Math.max(0f, Math.min(1f, x));
        return x * x * (3f - 2f * x);
    }
}
