package com.shumtugle.skydysh;

import java.util.Calendar;

/**
 * The dispatcher's room is her head. She is a golem too, of brick and stone; where the forged
 * golems have a plate on the brow, she has this: a room inside her head, walled in courses of
 * her own brick and stone, a ceiling of planks as her cap is, a floor of slab. In its front wall
 * the screen: her eyes. Through it the street is seen as she sees it, standing still while she
 * is in here; her eyes, two lamps over the screen, lit. Under the screen a shelf of planks with a
 * row of keys, a red one, and a clock with the real time and the date.
 *
 * Coming in, the walls close round the street from every side until it is left only on the
 * screen; going out, they draw back past the edges of the frame and the eye is behind her again.
 * On the screen, over what she sees, the dispatch is typed out: before a mission what happened and
 * what is to be done, after it what was done and in what time. At a touch (when all is typed;
 * the first touch types the rest at once) her mallet comes up from below on the right, the same
 * mallet and as big, and knocks the red key in; the walls draw back and the street goes on.
 *
 * The room stands in the world where her head is, turned as she is, so the screen looks out the
 * way she looks.
 */
final class Dispatch {
    static final float TYPE = 38f;                      // letters a second
    static final float CLOSE = 0.55f, OPEN = 0.55f;     // the walls closing round the street; drawing back
    static final float KNOCK = 0.34f, KNOCKED = 0.16f;  // the mallet up and onto the key; resting on it
    static final float WIDE = 4.5f;                     // how far out the walls start, and end, times their size
    static final float HEAD = 2.2f;                     // the room's middle above her feet

    // the screen in the front wall, the eye before it (the room's own frame: x right, y up, z ahead)
    private static final float SX = 0.5f, SY0 = -0.1f, SY1 = 0.62f, FRONT = 0.65f;
    private static final float EYE_Y = 0.2f, EYE_Z = -1.3f;
    private static final float KEY_X = 0.25f, KEY_Y = -0.47f, KEY_Z = 0.42f;
    private static final float CLOCK_X = -0.35f, CLOCK_Y = -0.4f, CLOCK_Z = 0.4f;

    private final World.Host host;
    private String[] lines, next;
    private int letters;
    private float t, typed, knockAt = -1f, openAt = -1f;
    private boolean pressed, done;
    private int ticked;
    private final float[] at = new float[3];

    // where the room is: her head, and the way she faces
    private float ox, oy, oz, cs, sn, size = 1f;

    /** The room in her head, its screen showing these lines (the last is the call to go on, blinking once all is typed). */
    Dispatch(World.Host host, String[] lines) {
        this.host = host;
        show(lines);
    }

    private void show(String[] lines) {
        this.lines = lines;
        int n = 0;
        for (String l : lines) n += l.length();
        letters = n;
        typed = 0f;
        ticked = 0;
    }

    /** After these lines, at the key, the next ones on the screen before the walls draw back. */
    Dispatch then(String[] more) {
        next = more;
        return this;
    }

    /** What the screen says before a mission. */
    static String[] briefing(int round) {
        if (Folder.its(round)) return Lang.lines("brief.folder", round, null);
        return Lang.lines(round <= 1 ? "brief.first" : "brief.more", round, null);
    }

    /** Before a mission again: the folder, after the rivals got there first; a hunt, after the time ran out. */
    static String[] again(int round) {
        return Lang.lines(Folder.its(round) ? "brief.again" : "brief.late", round, null);
    }

    /** The time ran out with some of the five still loose: how many were caught. */
    static String[] late(int round, int caught) {
        String[] lines = Lang.lines("report.late", round, null);
        for (int k = 0; k < lines.length; k++) lines[k] = lines[k].replace("{caught}", String.valueOf(caught));
        return lines;
    }

    /** The folder brought in: the reward, what has been won in all, the time. */
    static String[] paid(int round, String time, int money) {
        return fill(Lang.lines("report.folder", round, time), money);
    }

    /** The rivals got there first. */
    static String[] lost(int round, String time) {
        return Lang.lines("report.lost", round, time);
    }

    private static String[] fill(String[] lines, int money) {
        for (int k = 0; k < lines.length; k++)
            lines[k] = lines[k].replace("{reward}", World.money(Folder.REWARD)).replace("{sum}", World.money(money));
        return lines;
    }

    /** What the screen says when a mission is done. */
    static String[] report(int round, String time) {
        return Lang.lines("report", round, time);
    }

    /** A touch: the rest typed at once, or, all typed, the mallet onto the key. */
    void touch() {
        if (t < CLOSE || knockAt >= 0f) return;
        if (typed < letters) {
            typed = letters;
            return;
        }
        knockAt = t;
    }

    /** Whether the walls have drawn back: the street goes on. */
    boolean done() {
        return done;
    }

    void update(float dt) {
        t += dt;
        if (t >= CLOSE && knockAt < 0f) {
            typed = Math.min(letters, typed + dt * TYPE);
            int k = (int) typed / 3;
            if (k > ticked && typed < letters) {
                ticked = k;
                host.sound(Synth.TOCK, 0.12f, 1.9f);
            }
        }
        if (knockAt >= 0f && !pressed && t - knockAt >= KNOCK) {
            pressed = true;
            host.sound(Synth.WOOD, 0.9f, 0.75f);
            host.sound(Synth.TOCK, 0.5f, 1.3f);
        }
        if (pressed && openAt < 0f && t - knockAt >= KNOCK + KNOCKED && next != null) {
            // the screen wiped, the next dispatch typed on it
            show(next);
            next = null;
            knockAt = -1f;
            pressed = false;
            host.sound(Synth.TV_OFF, 0.3f, 1.6f);
        } else if (pressed && openAt < 0f && t - knockAt >= KNOCK + KNOCKED) {
            openAt = t;
            host.sound(Synth.WHOOSH, 0.45f, 1.2f);
        }
        if (openAt >= 0f && t - openAt >= OPEN) done = true;
    }

    private static float ease(float u) {
        u = Math.max(0f, Math.min(1f, u));
        return u * u * (3f - 2f * u);
    }

    /**
     * Where the eye is, into eye (x, y, z, yaw, pitch): in the room before the screen, or on its way
     * between there and behind her, where the street is seen from.
     */
    void eye(Runner r, float[] eye) {
        place(r);
        float k = 1f;
        if (t < CLOSE) k = ease(t / CLOSE);
        if (openAt >= 0f) k = 1f - ease((t - openAt) / OPEN);
        // the room's eye
        float ix = ox + sn * EYE_Z, iy = oy + EYE_Y, iz = oz + cs * EYE_Z;
        float ipitch = -0.04f;
        eye[0] = r.eyeX + (ix - r.eyeX) * k;
        eye[1] = r.eyeY + (iy - r.eyeY) * k;
        eye[2] = r.eyeZ + (iz - r.eyeZ) * k;
        float dy = r.heading - r.eyeYaw;
        while (dy > Math.PI) dy -= 2f * (float) Math.PI;
        while (dy < -Math.PI) dy += 2f * (float) Math.PI;
        eye[3] = r.eyeYaw + dy * k;
        eye[4] = r.eyePitch + (ipitch - r.eyePitch) * k;
        // the walls far out while the eye is out, round it when it is in
        size = 1f + (WIDE - 1f) * (1f - k);
    }

    /** Whether she is to be drawn: not while the eye is in her head. */
    boolean showsHer() {
        return size > 3.9f;
    }

    private void place(Runner r) {
        ox = r.x;
        oy = r.y + HEAD;
        oz = r.z;
        cs = (float) Math.cos(r.heading);
        sn = (float) Math.sin(r.heading);
    }

    // ---- drawing ----

    private final float[] rot = new float[9];

    /** A box of the room, in its own frame, scaled about its middle as far as the walls are out. */
    private void box(DrawList d, float x, float y, float z, float hx, float hy, float hz, int color, int material) {
        float s = size;
        rot[0] = cs; rot[1] = 0f; rot[2] = sn;
        rot[3] = 0f; rot[4] = 1f; rot[5] = 0f;
        rot[6] = -sn; rot[7] = 0f; rot[8] = cs;
        d.box(ox + (cs * x + sn * z) * s, oy + y * s, oz + (-sn * x + cs * z) * s, hx * s, hy * s, hz * s, rot, color, material);
    }

    /** The room round the eye, and over the frame what the screen says. */
    void draw(DrawList d, Px f, int h) {
        walls(d);
        if (knockAt >= 0f && openAt < 0f) mallet(d);
        // a warm light inside
        d.fire[0] = ox + sn * -0.6f;
        d.fire[1] = oy + 0.9f * size;
        d.fire[2] = oz + cs * -0.6f;
        d.fire[3] = 0.55f;
        if (size < 1.05f) {
            screen(d, f);
            clock(d, f);
        }
    }

    private static final int BRICK = Frida.BRICK, BRICK_DARK = Frida.BRICK_DARK, STONE = Frida.STONE,
            STONE_DARK = Frida.STONE_DARK, PLANK = Frida.CAP, SLAB = Frida.SLAB, IRON = Frida.IRON;

    /** Her head from inside: courses of brick and stone, the cap of planks, the slab floor, the screen, the shelf. */
    private void walls(DrawList d) {
        float back = -1.7f, top = 1.3f, bottom = -1.05f, side = 1.2f, course = 0.22f;
        // the front wall, round the screen
        int row = 0;
        for (float y = bottom; y < top; y += course, row++) {
            float y1 = Math.min(top, y + course), cy = (y + y1) * 0.5f, hy = (y1 - y) * 0.5f;
            int c = (row & 1) == 0 ? BRICK : BRICK_DARK;
            boolean beside = y1 > SY0 && y < SY1;
            if (beside) {
                for (int s = -1; s <= 1; s += 2) {
                    float x0 = SX + 0.06f, x1 = side;
                    box(d, s * (x0 + x1) * 0.5f, cy, FRONT + 0.08f, (x1 - x0) * 0.5f, hy, 0.08f, (row % 3 == 1) ? STONE : c, DrawList.STONE);
                }
            } else {
                box(d, 0f, cy, FRONT + 0.08f, side, hy, 0.08f, c, DrawList.STONE);
            }
            // the side walls, in the same courses, a stone block now and then
            for (int s = -1; s <= 1; s += 2) {
                box(d, s * side, cy, (back + FRONT) * 0.5f, 0.08f, hy, (FRONT - back) * 0.5f, c, DrawList.STONE);
                if ((row + (s > 0 ? 1 : 0)) % 3 == 0)
                    box(d, s * (side - 0.09f), cy, -0.4f + 0.3f * s, 0.02f, hy * 0.92f, 0.32f, (row & 2) == 0 ? STONE : STONE_DARK, DrawList.STONE);
            }
        }
        // between the screen's sides and the bricks, above and below it
        box(d, 0f, (SY1 + 0.06f + Math.min(top, SY1 + 0.3f)) * 0.5f, FRONT + 0.08f, SX + 0.06f, (0.3f - 0.06f) * 0.5f, 0.08f, IRON, DrawList.PLAIN);
        // the screen's frame, dark, and her eyes over it, lit
        box(d, 0f, SY1 + 0.03f, FRONT, SX + 0.06f, 0.03f, 0.03f, 0x26241F, DrawList.PLAIN);
        box(d, 0f, SY0 - 0.03f, FRONT, SX + 0.06f, 0.03f, 0.03f, 0x26241F, DrawList.PLAIN);
        for (int s = -1; s <= 1; s += 2) {
            box(d, s * (SX + 0.03f), (SY0 + SY1) * 0.5f, FRONT, 0.03f, (SY1 - SY0) * 0.5f + 0.06f, 0.03f, 0x26241F, DrawList.PLAIN);
            box(d, s * 0.36f, SY1 + 0.19f, FRONT - 0.01f, 0.07f, 0.035f, 0.01f, Frida.EYES, DrawList.GLOW);
        }
        // the cap: planks across overhead, a slab over them
        for (int k = 0; k < 7; k++) {
            float x = -side + (k + 0.5f) * (2f * side / 7f);
            box(d, x, top + 0.05f, (back + FRONT) * 0.5f, side / 7f - 0.01f, 0.05f, (FRONT - back) * 0.5f + 0.1f,
                    (k & 1) == 0 ? PLANK : 0x8F5220, DrawList.WOOD);
        }
        box(d, 0f, bottom - 0.05f, (back + FRONT) * 0.5f, side, 0.05f, (FRONT - back) * 0.5f, SLAB, DrawList.STONE);
        box(d, 0f, top * 0.5f + bottom * 0.5f, back - 0.08f, side, (top - bottom) * 0.5f, 0.08f, BRICK_DARK, DrawList.STONE);
        // the shelf under the screen: planks, keys, the red key, the clock
        box(d, 0f, KEY_Y - 0.04f, 0.4f, 0.75f, 0.03f, 0.24f, 0x8A6650, DrawList.WOOD);
        for (int k = 0; k < 4; k++)
            box(d, 0.13f - k * 0.09f, KEY_Y, KEY_Z, 0.033f, 0.015f, 0.05f, 0xEDE6D6, DrawList.PLAIN);
        if (pressed) box(d, KEY_X, KEY_Y - 0.008f, KEY_Z, 0.07f, 0.012f, 0.08f, 0xFF5A3C, DrawList.GLOW);
        else box(d, KEY_X, KEY_Y + 0.006f, KEY_Z, 0.07f, 0.018f, 0.08f, 0xC5372B, DrawList.PLAIN);
        box(d, CLOCK_X, CLOCK_Y, CLOCK_Z + 0.06f, 0.14f, 0.06f, 0.06f, 0x2C2A28, DrawList.PLAIN);
        box(d, CLOCK_X, CLOCK_Y, CLOCK_Z - 0.002f, 0.11f, 0.04f, 0.004f, 0x120C0A, DrawList.GLASS);
    }

    private final float[] head = new float[3], turn = new float[9];

    /**
     * Her mallet, the same and as big as in the street: up from below the frame on the right, and
     * down onto the red key, where it rests a moment.
     */
    private void mallet(DrawList d) {
        float u = Math.min(1f, (t - knockAt) / KNOCK);
        // in the room's frame: from below, up beside the shelf, down onto the key
        float[] from = {0.85f, -1.4f, 0.1f}, top = {0.42f, -0.3f, 0.32f}, key = {KEY_X, KEY_Y + 0.17f, KEY_Z};
        float lx, ly, lz;
        if (u < 0.6f) {
            float v = u / 0.6f;
            v = 1f - (1f - v) * (1f - v);
            lx = from[0] + (top[0] - from[0]) * v;
            ly = from[1] + (top[1] - from[1]) * v;
            lz = from[2] + (top[2] - from[2]) * v;
        } else {
            float v = (u - 0.6f) / 0.4f;
            v = v * v;
            lx = top[0] + (key[0] - top[0]) * v;
            ly = top[1] + (key[1] - top[1]) * v;
            lz = top[2] + (key[2] - top[2]) * v;
        }
        // the handle back toward her right shoulder, off the frame below and to the right
        float hx = lx - 1.1f, hy = ly + 1.2f, hz = lz + 0.5f;
        float l = (float) Math.sqrt(hx * hx + hy * hy + hz * hz);
        hx /= l;
        hy /= l;
        hz /= l;
        // into the world
        float wx = cs * hx + sn * hz, wz = -sn * hx + cs * hz;
        head[0] = ox + cs * lx + sn * lz;
        head[1] = oy + ly;
        head[2] = oz - sn * lx + cs * lz;
        float bx = -wz, bz = wx, bl = Math.max(1e-3f, (float) Math.sqrt(bx * bx + bz * bz));
        bx /= bl;
        bz /= bl;
        float ux = -bz * hy, uy = bz * wx - bx * wz, uz = bx * hy;
        turn[0] = wx; turn[1] = ux; turn[2] = bx;
        turn[3] = hy; turn[4] = uy; turn[5] = 0f;
        turn[6] = wz; turn[7] = uz; turn[8] = bz;
        Mallet.draw(d, turn, head[0], head[1], head[2], Toss.SCALE, Mallet.HEAD_MIDDLE);
    }

    /** A point of the room's frame onto the frame of the picture. */
    private boolean onFrame(DrawList d, float x, float y, float z) {
        return d.project(ox + cs * x + sn * z, oy + y, oz - sn * x + cs * z, at);
    }

    /** Over what she sees: the lines of a tube, a dark veil where the dispatch is typed, the dispatch. */
    private void screen(DrawList d, Px f) {
        float x0 = 1e9f, y0 = 1e9f, x1 = -1e9f, y1 = -1e9f;
        for (int c = 0; c < 4; c++) {
            if (!onFrame(d, (c & 1) == 0 ? -SX : SX, (c & 2) == 0 ? SY0 : SY1, FRONT)) return;
            x0 = Math.min(x0, at[0]);
            x1 = Math.max(x1, at[0]);
            y0 = Math.min(y0, at[1]);
            y1 = Math.max(y1, at[1]);
        }
        int ix0 = Math.max(0, Math.round(x0)), iy0 = Math.max(0, Math.round(y0));
        int ix1 = Math.min(f.w, Math.round(x1)), iy1 = Math.min(f.h, Math.round(y1));
        float typedShare = Math.min(1f, (t - CLOSE) / 0.25f);
        // a dark veil over the street, so the words read; the tube draws its rows over it
        int a = Math.round(185 * Math.max(0f, typedShare));
        for (int y = iy0; y < iy1; y++)
            for (int x = ix0; x < ix1; x++) f.a[y * f.w + x] = (a << 24) | 0x0E0A07;
        d.tube[0] = ix0;
        d.tube[1] = iy0;
        d.tube[2] = ix1;
        d.tube[3] = iy1;
        if (t < CLOSE) return;
        if (font == null) font = TubeFont.read(host.asset("tube.bin"));
        if (font == null) {
            pixelLines(f, ix0, iy0, iy1);
            return;
        }
        // a line a row of letters; an empty line half that
        int lineH = font.rows - 1, half = lineH / 2, total = 0, empty = 0;
        for (String l : lines) {
            total += l.isEmpty() ? half : lineH;
            if (l.isEmpty()) empty++;
        }
        // a long text closes up to fit the glass, the call to go on with it: the empty lines first,
        // then the rows a little
        int room = iy1 - iy0 - 6;
        while (total > room && half > 2) {
            half--;
            total -= empty;
        }
        while (total > room && lineH > font.rows - 3) {
            lineH--;
            total -= lines.length - empty;
        }
        int top = iy0 + Math.max(3, Math.round((iy1 - iy0 - total) * 0.5f)), left = ix0 + 7;
        // the beam: a band a little brighter running down the glass now and then
        float sweep = (t * 0.55f) % 2.2f, beamY = iy0 + sweep * (iy1 - iy0);
        int shown = (int) typed, y = top;
        for (int i = 0; i < lines.length; i++) {
            String s = lines[i];
            if (shown <= 0) break;
            String part = s.length() <= shown ? s : s.substring(0, shown);
            shown -= s.length();
            int lh = s.isEmpty() ? half : lineH;
            boolean last = i == lines.length - 1;
            if (y + lh > iy1 - 2) break;
            if (!(last && typed >= letters && (int) (t * 2.2f) % 2 == 1)) {
                int col = last ? 0xFFF2C46A : 0xFFE9DDB0;
                // each line its own: drifting a little brighter and weaker, and now and then
                // dimmed a moment, as an uneven beam leaves it
                float own = 0.84f + 0.1f * (float) Math.sin(t * (1.3f + i * 0.37f) + i * 2.1f)
                        + 0.06f * (float) Math.sin(t * (4.1f + i * 0.9f) + i);
                int tick = (int) (t * 7f);
                if (((tick * 7919 + i * 104729) ^ (tick >>> 3) * 31) % 41 == 0) own *= 0.62f;
                for (int r = 0; r < font.rows; r++) {
                    float off = (y + r - beamY) / 7f;
                    light[r] = own * (1f + 0.28f * (float) Math.exp(-off * off));
                }
                font.draw(f, part, left, y, col, light);
                if (shown < 0 && typed < letters) {
                    int cx = left + font.width(part) + 1;
                    f.rect(cx, y + 2, cx + 3, y + font.base, col);
                }
            }
            y += lh;
        }
    }

    private static TubeFont font;
    private final float[] light = new float[32];

    /** The dispatch in the small capitals of the frame, when the tube's own font is not to be had. */
    private void pixelLines(Px f, int left, int iy0, int iy1) {
        int lineH = 9, top = iy0 + Math.max(4, Math.round((iy1 - iy0 - lines.length * lineH) * 0.5f));
        int shown = (int) typed;
        for (int i = 0; i < lines.length && shown > 0; i++) {
            String s = lines[i].toUpperCase();
            String part = s.length() <= shown ? s : s.substring(0, shown);
            shown -= s.length();
            int y = top + i * lineH;
            if (y + 7 > iy1 - 2) break;
            PixelFont.draw(f, part, left + 7, y, 1, 0xFFE9DDB0);
        }
    }

    /** The clock on the shelf: the real time and the date, in orange. */
    private void clock(DrawList d, Px f) {
        if (!onFrame(d, CLOCK_X, CLOCK_Y, CLOCK_Z - 0.01f)) return;
        Calendar c = Calendar.getInstance();
        int hh = c.get(Calendar.HOUR_OF_DAY), mm = c.get(Calendar.MINUTE);
        String s = (hh < 10 ? "0" : "") + hh + (c.get(Calendar.SECOND) % 2 == 0 ? ":" : " ") + (mm < 10 ? "0" : "") + mm;
        int dd = c.get(Calendar.DAY_OF_MONTH), mo = c.get(Calendar.MONTH) + 1;
        String date = (dd < 10 ? "0" : "") + dd + "." + (mo < 10 ? "0" : "") + mo;
        int w = PixelFont.width(s, 1), wd = PixelFont.width(date, 1);
        PixelFont.draw(f, s, Math.round(at[0] - w * 0.5f), Math.round(at[1] - 8f), 1, 0xFFFF8A3C);
        PixelFont.draw(f, date, Math.round(at[0] - wd * 0.5f), Math.round(at[1] + 1f), 1, 0xFFC9622A);
    }
}
