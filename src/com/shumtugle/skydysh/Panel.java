package com.shumtugle.skydysh;

import java.util.Random;

/**
 * An electrical panel opened up: a grid of wiring pieces in a steel box, each a straight, a corner,
 * a branch of three or an end; the feed fixed in the middle. A touch turns a piece a quarter round.
 * Power runs from the feed along every pair of pieces whose wires meet; what it reaches glows. The
 * panel is mended when it reaches every piece (then, with as many joins as there are pieces less one,
 * the wiring is one tree: no loop, no loose end). Three of the ends are lamps, lit as the power
 * comes to them; the lamps alone are not the panel mended, and while they all burn and some piece
 * is still dead, the dead pieces pulse, so it is plain what is left. Mended, the main switch below
 * the grid can be thrown, and the lights come on.
 *
 * Each panel is new: the wiring is grown at random as a tree over the whole grid out from the feed
 * (now from one piece already wired, now from another, so that it branches), no piece with four
 * wires; then every piece is turned at random, and turned again if that happened to leave it whole.
 * A grid of four by four has about a hundred thousand such trees, five by five over half a billion,
 * and each tree as many ways of being turned: the same panel does not come twice.
 */
final class Panel {
    // a piece's wires, a bit each: up, right, down, left
    static final int UP = 1, RIGHT = 2, DOWN = 4, LEFT = 8;
    static final int LAMPS = 3;

    final int n;
    /** Each piece's wires as they are turned now, row after row from the top. */
    final int[] wires;
    private final int feed;
    /** The wiring as it was grown, before it was turned (for the desktop stand). */
    private final int[] grown;
    private final boolean[] lamp, powered;
    /** How many of the lamps the power reaches now, and how many there are. */
    private int lampsLit, lamps;
    /** The panel mended: the power reaches every piece. */
    boolean whole;
    /** The main switch thrown, after it was mended. */
    boolean on;
    /** Let go of before it was done (by the sign of the way back). */
    boolean left;
    private float t, onAt = -1f;
    private int turns;

    Panel(int n, long seed) {
        this.n = n;
        Random rnd = new Random(seed);
        wires = new int[n * n];
        lamp = new boolean[n * n];
        powered = new boolean[n * n];
        feed = (n / 2) * n + n / 2;
        grow(rnd);
        // three of the ends are lamps
        int[] ends = new int[n * n];
        int m = 0;
        for (int i = 0; i < n * n; i++) if (i != feed && Integer.bitCount(wires[i]) == 1) ends[m++] = i;
        for (int k = 0; k < LAMPS && m > 0; k++) {
            int j = rnd.nextInt(m);
            lamp[ends[j]] = true;
            ends[j] = ends[--m];
            lamps++;
        }
        grown = wires.clone();
        // turned at random, the feed too; never left whole
        do {
            for (int i = 0; i < n * n; i++) {
                int r = rnd.nextInt(4);
                for (int k = 0; k < r; k++) wires[i] = turned(wires[i]);
            }
            power();
        } while (whole);
    }

    /** The wiring grown as a tree over the grid from the feed: each step a wire from a piece in it to a neighbour not yet. */
    private void grow(Random rnd) {
        boolean[] in = new boolean[n * n];
        int[] edge = new int[n * n * 4];
        in[feed] = true;
        int count = 1;
        while (count < n * n) {
            // every way out of the tree to a piece not in it yet, from a piece with fewer than three wires
            int m = 0;
            for (int i = 0; i < n * n; i++) {
                if (!in[i] || Integer.bitCount(wires[i]) >= 3) continue;
                for (int dir = 0; dir < 4; dir++) {
                    int j = step(i, dir);
                    if (j >= 0 && !in[j]) edge[m++] = i * 4 + dir;
                }
            }
            if (m == 0) {
                // boxed in by the three-wire rule: start again (rare on grids this small)
                java.util.Arrays.fill(wires, 0);
                java.util.Arrays.fill(in, false);
                in[feed] = true;
                count = 1;
                continue;
            }
            int e = edge[rnd.nextInt(m)], i = e / 4, dir = e % 4, j = step(i, dir);
            wires[i] |= 1 << dir;
            wires[j] |= 1 << ((dir + 2) % 4);
            in[j] = true;
            count++;
        }
    }

    /** The piece next to i the way dir goes (0 up, 1 right, 2 down, 3 left), or -1 past the edge. */
    private int step(int i, int dir) {
        int r = i / n, c = i % n;
        if (dir == 0) r--;
        else if (dir == 1) c++;
        else if (dir == 2) r++;
        else c--;
        return r < 0 || c < 0 || r >= n || c >= n ? -1 : r * n + c;
    }

    /** A piece's wires turned a quarter round, the way a clock goes. */
    static int turned(int w) {
        return ((w << 1) | (w >> 3)) & 15;
    }

    /** Where the power reaches, out from the feed along wires that meet; whole, if everywhere. */
    private void power() {
        java.util.Arrays.fill(powered, false);
        int[] queue = new int[n * n];
        int head = 0, tail = 0, reached = 1;
        queue[tail++] = feed;
        powered[feed] = true;
        while (head < tail) {
            int i = queue[head++];
            for (int dir = 0; dir < 4; dir++) {
                if ((wires[i] & (1 << dir)) == 0) continue;
                int j = step(i, dir);
                if (j < 0 || powered[j] || (wires[j] & (1 << ((dir + 2) % 4))) == 0) continue;
                powered[j] = true;
                queue[tail++] = j;
                reached++;
            }
        }
        whole = reached == n * n;
        lampsLit = 0;
        for (int i = 0; i < n * n; i++) if (lamp[i] && powered[i]) lampsLit++;
    }

    /** Every lamp burning while the panel is not yet mended: the dead pieces are to be shown. */
    boolean lampsOnly() {
        return !whole && lamps > 0 && lampsLit == lamps;
    }

    /** For the desktop stand: the first so many pieces (row after row) turned back as they were grown. */
    void standMend(int pieces) {
        for (int i = 0; i < Math.min(pieces, n * n); i++) wires[i] = grown[i];
        power();
    }

    /** For the desktop stand: where the main switch is over the frame (x, y into out), once drawn. */
    void standLever(float[] out) {
        out[0] = leverX + leverW * 0.5f;
        out[1] = leverY + leverH * 0.5f;
    }

    void update(float dt) {
        t += dt;
    }

    /** Whether the lights have been on long enough for the panel to be shut. */
    boolean shut() {
        return on && t - onAt > 1.1f;
    }

    // ---- the box over the frame: where it is, laid out last ----
    private int gx, gy, cell, leverX, leverY, leverW, leverH, backX, backY;

    /** A touch on the panel: a piece turned, or the main switch thrown; true if something was. */
    boolean touch(float x, float y, World.Host host) {
        if (on) return false;
        if (Math.abs(x - backX) < 18 && Math.abs(y - backY) < 18) {
            left = true;
            return true;
        }
        if (whole && x >= leverX - 6 && x <= leverX + leverW + 6 && y >= leverY - 6 && y <= leverY + leverH + 6) {
            on = true;
            onAt = t;
            host.sound(Synth.LATCH, 0.9f, 0.8f);
            host.sound(Synth.HUM, 0.4f, 1f);
            host.buzz(80);
            return true;
        }
        int c = (int) Math.floor((x - gx) / (float) cell), r = (int) Math.floor((y - gy) / (float) cell);
        if (c < 0 || r < 0 || c >= n || r >= n) return false;
        int i = r * n + c;
        wires[i] = turned(wires[i]);
        turns++;
        boolean was = whole;
        power();
        host.sound(Synth.TOCK, 0.4f, whole ? 1.5f : 1.1f);
        host.buzz(12);
        if (whole && !was) host.sound(Synth.CHIME, 0.4f, 1.4f);
        return true;
    }

    static final int STEEL = 0xFF5E676E, STEEL_DARK = 0xFF3C4349, STEEL_LIGHT = 0xFF8A949B, BACK = 0xFF23282C;
    static final int COPPER = 0xFF8A5A36, COPPER_HOT = 0xFFD9935A, LIVE = 0xFFFFC94A, LIVE_CORE = 0xFFFFF2C2, LAMP_OFF = 0xFF4A4A40;
    static final int RED = 0xFFC8402E, GREEN = 0xFF46D27E, INK = 0xFF15191C, PAPER = 0xFFE8E1CC;

    /** The open box over the whole frame: its steel, the grid of pieces, the lamps, the switch, the plate with its name. */
    void draw(Px f, String label) {
        int w = f.w, h = f.h;
        f.rect(0, 0, w, h, BACK);
        int margin = 14, side = w - 2 * margin;
        int boxY = Math.max(64, (h - side - 138) / 2 + 34);
        // the steel door's inside: a frame, rivets, a label plate
        f.rect(margin - 6, boxY - 34, w - margin + 6, boxY + side + 104, STEEL_DARK);
        f.rect(margin - 3, boxY - 31, w - margin + 3, boxY + side + 101, STEEL);
        for (int k = 0; k < 4; k++) {
            int rx = k % 2 == 0 ? margin + 2 : w - margin - 2, ry = k < 2 ? boxY - 26 : boxY + side + 96;
            f.disc(rx, ry, 2.2f, STEEL_LIGHT);
        }
        f.rect(w / 2 - 50, boxY - 24, w / 2 + 50, boxY - 10, PAPER);
        PixelFont.draw(f, label, w / 2 - PixelFont.width(label, 1) / 2, boxY - 21, 1, INK);
        // the grid
        cell = side / n;
        gx = (w - cell * n) / 2;
        gy = boxY;
        f.rect(gx - 2, gy - 2, gx + cell * n + 2, gy + cell * n + 2, INK);
        int wire = Math.max(3, cell / 9);
        // the lamps all lit but the panel not mended: the dead copper pulses
        int dead = lampsOnly() ? Px.mix(COPPER, COPPER_HOT, 0.5f + 0.5f * (float) Math.sin(t * 6f)) : COPPER;
        for (int r = 0; r < n; r++)
            for (int c = 0; c < n; c++) {
                int i = r * n + c, x0 = gx + c * cell, y0 = gy + r * cell, cx = x0 + cell / 2, cy = y0 + cell / 2;
                f.rect(x0 + 1, y0 + 1, x0 + cell - 1, y0 + cell - 1, (r + c) % 2 == 0 ? 0xFF2E3439 : 0xFF2A3035);
                int col = powered[i] ? LIVE : dead;
                int k = wires[i];
                if ((k & UP) != 0) f.rect(cx - wire, y0 + 1, cx + wire, cy, col);
                if ((k & DOWN) != 0) f.rect(cx - wire, cy, cx + wire, y0 + cell - 1, col);
                if ((k & LEFT) != 0) f.rect(x0 + 1, cy - wire, cx, cy + wire, col);
                if ((k & RIGHT) != 0) f.rect(cx, cy - wire, x0 + cell - 1, cy + wire, col);
                if (powered[i]) {
                    // a bright core along the live wires
                    int core = Math.max(1, wire / 2);
                    if ((k & UP) != 0) f.rect(cx - core, y0 + 1, cx + core, cy, LIVE_CORE);
                    if ((k & DOWN) != 0) f.rect(cx - core, cy, cx + core, y0 + cell - 1, LIVE_CORE);
                    if ((k & LEFT) != 0) f.rect(x0 + 1, cy - core, cx, cy + core, LIVE_CORE);
                    if ((k & RIGHT) != 0) f.rect(cx, cy - core, x0 + cell - 1, cy + core, LIVE_CORE);
                }
                if (i == feed) {
                    // the feed: a block with a bolt on it
                    int b = cell / 4;
                    f.rect(cx - b, cy - b, cx + b, cy + b, INK);
                    f.rect(cx - b + 2, cy - b + 2, cx + b - 2, cy + b - 2, LIVE);
                    for (int s = 0; s < b; s++) f.rect(cx + b / 3 - s / 2, cy - b + 3 + s, cx + b / 3 - s / 2 + 2, cy - b + 4 + s, INK);
                } else if (lamp[i]) {
                    // a lamp at the end: dark, or lit with a glow round it
                    float rr = cell * 0.2f;
                    if (powered[i]) f.disc(cx, cy, rr + 4f, 0xFF6A5A2A);
                    f.disc(cx, cy, rr + 1.5f, INK);
                    f.disc(cx, cy, rr, powered[i] ? LIVE_CORE : LAMP_OFF);
                } else if (Integer.bitCount(k) == 1) {
                    f.disc(cx, cy, wire + 2.5f, INK);
                    f.disc(cx, cy, wire + 1.2f, col);
                } else {
                    f.disc(cx, cy, wire + 0.8f, col);
                }
            }
        // the main switch: a lever in a slot, red and down; green to be thrown once mended; up when thrown
        leverW = 46;
        leverH = 54;
        leverX = w / 2 - leverW / 2;
        leverY = gy + cell * n + 22;
        f.rect(leverX - 3, leverY - 3, leverX + leverW + 3, leverY + leverH + 3, STEEL_DARK);
        f.rect(leverX, leverY, leverX + leverW, leverY + leverH, INK);
        int handle = on ? leverY + 4 : leverY + leverH - 18;
        int hc = on ? GREEN : whole ? (((int) (t * 3f)) % 2 == 0 ? GREEN : 0xFF2E8A54) : RED;
        f.rect(leverX + leverW / 2 - 3, Math.min(handle, leverY + leverH / 2), leverX + leverW / 2 + 3, Math.max(handle, leverY + leverH / 2), STEEL_LIGHT);
        f.rect(leverX + 6, handle, leverX + leverW - 6, handle + 14, hc);
        // the three lamps' state beside the switch
        int lit = lampsLit;
        for (int k = 0; k < LAMPS; k++) {
            float lx = leverX + leverW + 22 + k * 16, ly = leverY + leverH / 2f;
            f.disc(lx, ly, 6f, INK);
            f.disc(lx, ly, 4.5f, k < lit ? LIVE_CORE : LAMP_OFF);
        }
        // the way back, high on the left; what to do, in a line under the box
        backX = margin + 10;
        backY = boxY - 52;
        if (backY < 10) backY = 10;
        f.disc(backX, backY, 11f, STEEL_DARK);
        f.disc(backX, backY, 9.5f, STEEL_LIGHT);
        f.rect(backX - 5, backY - 1, backX + 5, backY + 1, INK);
        for (int s = 0; s < 4; s++) {
            f.rect(backX - 5 + s, backY - 1 - s, backX - 3 + s, backY - s, INK);
            f.rect(backX - 5 + s, backY + s, backX - 3 + s, backY + 1 + s, INK);
        }
    }
}
