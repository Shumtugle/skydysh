package com.shumtugle.skydysh;

import java.util.Random;

/**
 * The box of a set of lights opened up: a board of sockets, and on it three pairs of terminals, red,
 * amber and green. Each pair is to be joined by a wire of its colour, drawn with a finger from one
 * terminal through the sockets to the other; no two wires through one socket, and every socket used.
 * Joined, a wire lights its lamp at the top of the box; all three, and the lights work again.
 *
 * Drawing: down on a terminal starts its wire afresh from there; down on a wire goes on from that
 * socket. Moved into the next socket, the wire goes on into it; back into its own, it is taken back
 * there; into another's, that one is cut where it was gone into. At its other terminal it is done.
 *
 * Each board is new: one path is laid through every socket of the board (a snake, then bent at
 * random many times, keeping it one path through all), and cut into three: the ends of each piece
 * are a pair. So there is always a way, and the board does not come twice.
 */
final class Wires {
    static final int N = 5, COLOURS = 3;
    static final int[] LIVE = {0xFFE8463A, 0xFFF2B43A, 0xFF3FD27A}, DIM = {0xFF6A2A26, 0xFF6A5426, 0xFF24603C};
    static final int STEEL = Panel.STEEL, STEEL_DARK = Panel.STEEL_DARK, STEEL_LIGHT = Panel.STEEL_LIGHT, BACK = Panel.BACK,
            INK = Panel.INK, PAPER = Panel.PAPER;
    /** After the last wire is joined, the box is shut so long after. */
    static final float SHUT_AFTER = 1.3f;

    // each colour's two terminals (socket numbers); each colour's wire as it is drawn, socket by socket
    private final int[] endA = new int[COLOURS], endB = new int[COLOURS];
    private final int[][] wire = new int[COLOURS][N * N];
    private final int[] len = new int[COLOURS];
    private final boolean[] joined = new boolean[COLOURS];
    // the colour drawn now, by the finger down; -1 if none
    private int drawing = -1;
    private float t, doneAt = -1f;
    /** Left unmended, by the way back. */
    boolean left;

    Wires(long seed) {
        Random rnd = new Random(seed);
        int[] path = new int[N * N];
        // a snake through every socket, row by row
        for (int r = 0, k = 0; r < N; r++)
            for (int c = 0; c < N; c++) path[k++] = r * N + (r % 2 == 0 ? c : N - 1 - c);
        // bent many times: from one end a step to a neighbour along the path, and the part after it turned back
        for (int it = 0; it < 600; it++) backbite(path, rnd);
        // cut in three, no piece shorter than five
        int total = N * N, a = 5 + rnd.nextInt(total - 15 + 1), b = a + 5 + rnd.nextInt(total - a - 10 + 1);
        int[] cut = {0, a, b, total};
        for (int k = 0; k < COLOURS; k++) {
            endA[k] = path[cut[k]];
            endB[k] = path[cut[k + 1] - 1];
        }
    }

    /** One bend of the path through every socket: one end goes over to a neighbour of it, and the loop so made is undone. */
    private static void backbite(int[] path, Random rnd) {
        int n = path.length;
        if (rnd.nextBoolean()) reverse(path, 0, n - 1);
        int end = path[n - 1], r = end / N, c = end % N;
        int[][] steps = {{0, 1}, {0, -1}, {1, 0}, {-1, 0}};
        int[] s = steps[rnd.nextInt(4)];
        int nr = r + s[0], nc = c + s[1];
        if (nr < 0 || nc < 0 || nr >= N || nc >= N) return;
        int to = nr * N + nc, at = -1;
        for (int k = 0; k < n; k++) if (path[k] == to) at = k;
        if (at < 0 || at >= n - 2) return;
        // the end joined to the neighbour: the part after the neighbour turned back
        reverse(path, at + 1, n - 1);
    }

    private static void reverse(int[] a, int i, int j) {
        while (i < j) {
            int x = a[i];
            a[i++] = a[j];
            a[j--] = x;
        }
    }

    void update(float dt) {
        t += dt;
    }

    /** Whether every wire is joined and every socket used. */
    boolean whole() {
        int used = 0;
        for (int k = 0; k < COLOURS; k++) {
            if (!joined[k]) return false;
            used += len[k];
        }
        return used == N * N;
    }

    /** Whether the lights have come on long enough for the box to be shut. */
    boolean shut() {
        return doneAt >= 0f && t - doneAt > SHUT_AFTER;
    }

    // ---- what is where ----

    /** Which colour's terminal socket i is, or -1. */
    private int terminal(int i) {
        for (int k = 0; k < COLOURS; k++) if (endA[k] == i || endB[k] == i) return k;
        return -1;
    }

    /** Which colour's wire goes through socket i, and where along it (into at[0], at[1]); false if none. */
    private boolean onWire(int i, int[] at) {
        for (int k = 0; k < COLOURS; k++)
            for (int q = 0; q < len[k]; q++)
                if (wire[k][q] == i) {
                    at[0] = k;
                    at[1] = q;
                    return true;
                }
        return false;
    }

    private final int[] found = new int[2];

    // ---- the touch ----
    private int gx, gy, cell, backX, backY;

    /** A finger down, moved or lifted at (x, y) over the frame: World.DOWN, MOVE or UP. */
    void touch(int type, float x, float y, World.Host host) {
        if (doneAt >= 0f) return;
        if (type == World.DOWN && Math.abs(x - backX) < 18 && Math.abs(y - backY) < 18) {
            left = true;
            return;
        }
        if (type == World.UP) {
            drawing = -1;
            return;
        }
        int c = (int) Math.floor((x - gx) / (float) cell), r = (int) Math.floor((y - gy) / (float) cell);
        if (c < 0 || r < 0 || c >= N || r >= N) return;
        int i = r * N + c;
        if (type == World.DOWN) {
            int term = terminal(i);
            if (term >= 0) {
                // from a terminal: its wire afresh from there
                len[term] = 1;
                wire[term][0] = i;
                joined[term] = false;
                drawing = term;
                host.sound(Synth.TOCK, 0.35f, 1.2f);
            } else if (onWire(i, found)) {
                // from a wire: on from that socket
                len[found[0]] = found[1] + 1;
                joined[found[0]] = false;
                drawing = found[0];
            }
            return;
        }
        if (drawing < 0 || joined[drawing]) return;
        int k = drawing, head = wire[k][len[k] - 1];
        if (i == head) return;
        // only into the next socket over, up, down or across
        int hr = head / N, hc = head % N;
        if (Math.abs(hr - r) + Math.abs(hc - c) != 1) return;
        // back into its own: taken back there
        for (int q = 0; q < len[k] - 1; q++)
            if (wire[k][q] == i) {
                len[k] = q + 1;
                return;
            }
        int term = terminal(i);
        if (term >= 0 && term != k) return;
        // into another's wire: that one cut where it was gone into
        if (onWire(i, found) && found[0] != k) {
            len[found[0]] = found[1];
            joined[found[0]] = false;
        }
        wire[k][len[k]++] = i;
        host.sound(Synth.TOCK, 0.2f, 1.5f + 0.05f * len[k]);
        if (term == k) {
            // at its other terminal: joined, its lamp lit
            joined[k] = true;
            drawing = -1;
            host.sound(Synth.CHIME, 0.35f, 1.1f + 0.2f * k);
            host.buzz(20);
            if (whole()) {
                doneAt = t;
                host.sound(Synth.HUM, 0.4f, 1f);
                host.buzz(80);
            }
        }
    }

    /** For the desktop stand: every wire laid as the board was made, and done. */
    void standSolve() {
        // the way the board was made is not kept: lay each colour by searching for its wire
        boolean[] used = new boolean[N * N];
        if (lay(0, used)) {
            for (int k = 0; k < COLOURS; k++) joined[k] = true;
            doneAt = t;
        }
    }

    /** Each colour's wire from k on, through the sockets not used yet, every socket used at the end: true if found. */
    private boolean lay(int k, boolean[] used) {
        if (k == COLOURS) {
            for (boolean u : used) if (!u) return false;
            return true;
        }
        len[k] = 1;
        wire[k][0] = endA[k];
        used[endA[k]] = true;
        if (walk(k, used)) return true;
        used[endA[k]] = false;
        return false;
    }

    private boolean walk(int k, boolean[] used) {
        int head = wire[k][len[k] - 1];
        if (head == endB[k]) return lay(k + 1, used);
        int r = head / N, c = head % N;
        int[][] steps = {{0, 1}, {1, 0}, {0, -1}, {-1, 0}};
        for (int[] s : steps) {
            int nr = r + s[0], nc = c + s[1];
            if (nr < 0 || nc < 0 || nr >= N || nc >= N) continue;
            int i = nr * N + nc;
            if (used[i]) continue;
            int term = terminal(i);
            if (term >= 0 && term != k) continue;
            used[i] = true;
            wire[k][len[k]++] = i;
            if (walk(k, used)) return true;
            len[k]--;
            used[i] = false;
        }
        return false;
    }

    // ---- the box over the frame ----

    /** The open box over the whole frame: its steel, the three lamps at its top, the board with its wires. */
    void draw(Px f, String label) {
        int w = f.w, h = f.h;
        f.rect(0, 0, w, h, BACK);
        int margin = 14, side = w - 2 * margin;
        int boxY = Math.max(110, (h - side) / 2 + 30);
        f.rect(margin - 6, boxY - 84, w - margin + 6, boxY + side + 20, STEEL_DARK);
        f.rect(margin - 3, boxY - 81, w - margin + 3, boxY + side + 17, STEEL);
        for (int k = 0; k < 4; k++) {
            int rx = k % 2 == 0 ? margin + 2 : w - margin - 2, ry = k < 2 ? boxY - 76 : boxY + side + 12;
            f.disc(rx, ry, 2.2f, STEEL_LIGHT);
        }
        f.rect(w / 2 - 50, boxY - 74, w / 2 + 50, boxY - 60, PAPER);
        PixelFont.draw(f, label, w / 2 - PixelFont.width(label, 1) / 2, boxY - 71, 1, INK);
        // the three lamps, red, amber, green, in a black head; lit as each wire is joined, all of them blinking once done
        int hx0 = w / 2 - 54, hy = boxY - 34;
        f.rect(hx0 - 6, hy - 16, hx0 + 114, hy + 16, INK);
        boolean blink = doneAt >= 0f && ((int) ((t - doneAt) * 6f)) % 2 == 0;
        for (int k = 0; k < COLOURS; k++) {
            float lx = hx0 + 18 + k * 36;
            boolean on = joined[k] && !blink;
            if (on) f.disc(lx, hy, 15f, Px.mix(LIVE[k], BACK, 0.6f));
            f.disc(lx, hy, 11f, 0xFF0C0E10);
            f.disc(lx, hy, 9.5f, on ? LIVE[k] : DIM[k]);
        }
        // the board
        cell = side / N;
        gx = (w - cell * N) / 2;
        gy = boxY;
        f.rect(gx - 2, gy - 2, gx + cell * N + 2, gy + cell * N + 2, INK);
        for (int r = 0; r < N; r++)
            for (int c = 0; c < N; c++) {
                int x0 = gx + c * cell, y0 = gy + r * cell;
                f.rect(x0 + 1, y0 + 1, x0 + cell - 1, y0 + cell - 1, (r + c) % 2 == 0 ? 0xFF2E3439 : 0xFF2A3035);
                f.disc(x0 + cell / 2, y0 + cell / 2, 2f, 0xFF4A5258);
            }
        int thick = Math.max(4, cell / 6);
        for (int k = 0; k < COLOURS; k++) {
            int col = joined[k] ? LIVE[k] : Px.mix(LIVE[k], 0xFF2E3439, 0.25f);
            for (int q = 0; q < len[k]; q++) {
                int i = wire[k][q], cx = gx + (i % N) * cell + cell / 2, cy = gy + (i / N) * cell + cell / 2;
                f.disc(cx, cy, thick, col);
                if (q > 0) {
                    int j = wire[k][q - 1], px = gx + (j % N) * cell + cell / 2, py = gy + (j / N) * cell + cell / 2;
                    f.rect(Math.min(cx, px) - thick, Math.min(cy, py) - thick, Math.max(cx, px) + thick, Math.max(cy, py) + thick, col);
                }
            }
        }
        // the terminals: a ring of the colour, a dark middle
        for (int k = 0; k < COLOURS; k++)
            for (int e = 0; e < 2; e++) {
                int i = e == 0 ? endA[k] : endB[k], cx = gx + (i % N) * cell + cell / 2, cy = gy + (i / N) * cell + cell / 2;
                f.disc(cx, cy, cell * 0.3f, INK);
                f.disc(cx, cy, cell * 0.26f, LIVE[k]);
                f.disc(cx, cy, cell * 0.1f, joined[k] ? 0xFFFFF2C2 : INK);
            }
        // the way back, high on the left
        backX = margin + 10;
        backY = Math.max(10, boxY - 104);
        f.disc(backX, backY, 11f, STEEL_DARK);
        f.disc(backX, backY, 9.5f, STEEL_LIGHT);
        f.rect(backX - 5, backY - 1, backX + 5, backY + 1, INK);
        for (int s = 0; s < 4; s++) {
            f.rect(backX - 5 + s, backY - 1 - s, backX - 3 + s, backY - s, INK);
            f.rect(backX - 5 + s, backY + s, backX - 3 + s, backY + 1 + s, INK);
        }
    }
}
