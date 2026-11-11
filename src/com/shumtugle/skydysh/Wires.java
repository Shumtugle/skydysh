package com.shumtugle.skydysh;

import java.util.Random;

/**
 * The box of a set of lights opened up: a board of sockets, and on it three pairs of terminals, red,
 * amber and green. Each pair is to be joined by a wire of its colour, drawn with a finger from one
 * terminal through the sockets to the other; no two wires through one socket (the sockets need not all be used).
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
    static final float SHUT_AFTER = 2.2f;

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
        // every colour joined to its pair: that is all it asks, the sockets left empty or not
        for (int k = 0; k < COLOURS; k++)
            if (!joined[k]) return false;
        return true;
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

    /** For the desktop stand: the first k colours' wires laid as the board was made, the rest only at their terminal. */
    void standLay(int k) {
        boolean[] used = new boolean[N * N];
        if (!lay(0, used)) return;
        for (int c = 0; c < COLOURS; c++) {
            joined[c] = c < k;
            if (c >= k) len[c] = 1;
        }
        if (k >= COLOURS) doneAt = t;
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
    // the box's own look: a steel frame gone rusty at its edges, screwed at its corners; a brass plate
    // with its name; a recess with three lenses in bronze rings; the board of dark tiles; wires as
    // rounded tubes; the terminals, a ring of the colour round a brass knob
    static final int FRAME = 0xFF34363A, FRAME_EDGE = 0xFF24262A, FRAME_LIGHT = 0xFF4A4D52, RUST = 0xFF8A4E2C,
            BRASS = 0xFFD9C79C, BRASS_DARK = 0xFF8E7A50, BRONZE = 0xFF5A4A36, BRONZE_LIGHT = 0xFF8A7454, TILE = 0xFF202226,
            TILE_LIGHT = 0xFF2C2F33, TILE_DARK = 0xFF15171A, KNOB = 0xFFC9A55A, KNOB_LIGHT = 0xFFF2DDA0, CREAM = 0xFFF0E6CC,
            WORKS = 0xFF46D27E;
    // the lenses: unlit, dark glass with a breath of its colour; lit, the colour bright, a pale heart
    static final int[] LENS_OFF = {0xFF2A1513, 0xFF2A2212, 0xFF102419};
    /** Once done, the lamps go round as a set of lights does: so long each. */
    static final float CYCLE = 0.28f;

    /** The open box over the whole frame: its steel, the three lamps at its top, the board with its wires. */
    void draw(Px f, String label, String works) {
        int w = f.w, h = f.h;
        f.rect(0, 0, w, h, BACK);
        int margin = 14, side = w - 2 * margin;
        int boxY = Math.max(110, (h - side) / 2 + 30);
        int x0 = margin - 8, x1 = w - margin + 8, y0 = boxY - 88, y1 = boxY + side + 22;
        // the frame: its edge, its face, a lighter bevel along the top and left, rust worn in at the edges
        f.rect(x0, y0, x1, y1, FRAME_EDGE);
        f.rect(x0 + 2, y0 + 2, x1 - 2, y1 - 2, FRAME);
        f.rect(x0 + 2, y0 + 2, x1 - 2, y0 + 3, FRAME_LIGHT);
        f.rect(x0 + 2, y0 + 2, x0 + 3, y1 - 2, FRAME_LIGHT);
        for (int k = 0; k < 22; k++) {
            int a = (k * 37 + 11) % 97, e = k % 4;
            int rx = e == 0 ? x0 + 2 + a % 3 : e == 1 ? x1 - 4 - a % 3 : x0 + 6 + (a * 7) % (x1 - x0 - 12);
            int ry = e == 2 ? y0 + 2 + a % 3 : e == 3 ? y1 - 4 - a % 3 : y0 + 6 + (a * 11) % (y1 - y0 - 12);
            f.rect(rx, ry, rx + 2 + a % 3, ry + 1 + a % 2, RUST);
        }
        // the screws at its corners: a head with a cross cut in it
        for (int k = 0; k < 4; k++) {
            int sx = k % 2 == 0 ? x0 + 10 : x1 - 10, sy = k < 2 ? y0 + 10 : y1 - 10;
            f.disc(sx, sy, 4.2f, FRAME_EDGE);
            f.disc(sx, sy, 3.4f, BRONZE_LIGHT);
            f.rect(sx - 2, sy, sx + 3, sy + 1, FRAME_EDGE);
            f.rect(sx, sy - 2, sx + 1, sy + 3, FRAME_EDGE);
        }
        // the plate: brass, two rivets, the name; once all is joined, what it says now
        boolean done = doneAt >= 0f;
        String plate = done ? works : label;
        int pw = Math.max(110, PixelFont.width(plate, 2) + 30), py0 = boxY - 80, py1 = boxY - 62;
        f.rect(w / 2 - pw / 2 - 1, py0 - 1, w / 2 + pw / 2 + 1, py1 + 1, BRASS_DARK);
        f.rect(w / 2 - pw / 2, py0, w / 2 + pw / 2, py1, done ? WORKS : BRASS);
        f.disc(w / 2 - pw / 2 + 6, (py0 + py1) / 2, 1.8f, BRASS_DARK);
        f.disc(w / 2 + pw / 2 - 6, (py0 + py1) / 2, 1.8f, BRASS_DARK);
        PixelFont.draw(f, plate, w / 2 - PixelFont.width(plate, 2) / 2, py0 + 2, 2, INK);
        // the lamps in their recess: each lit as its wire is joined; once done, round as a set of lights goes
        int hx0 = w / 2 - 54, hy = boxY - 32;
        f.rect(hx0 - 10, hy - 20, hx0 + 118, hy + 20, FRAME_EDGE);
        f.rect(hx0 - 8, hy - 18, hx0 + 116, hy + 18, 0xFF1A1B1E);
        int lit = done ? ((int) ((t - doneAt) / CYCLE)) % COLOURS : -1;
        for (int k = 0; k < COLOURS; k++) {
            float lx = hx0 + 18 + k * 36;
            boolean on = done ? k == lit || t - doneAt < CYCLE * 0.5f : joined[k];
            if (on) {
                f.disc(lx, hy, 18f, Px.mix(LIVE[k], 0xFF1A1B1E, 0.72f));
                f.disc(lx, hy, 16f, Px.mix(LIVE[k], 0xFF1A1B1E, 0.5f));
            }
            f.disc(lx, hy, 14f, BRONZE);
            f.disc(lx, hy, 12.5f, BRONZE_LIGHT);
            for (int q = 0; q < 4; q++) {
                double a = Math.PI / 4 + q * Math.PI / 2;
                f.disc(lx + (float) Math.cos(a) * 13.2f, hy + (float) Math.sin(a) * 13.2f, 1.2f, FRAME_EDGE);
            }
            f.disc(lx, hy, 11f, 0xFF0C0E10);
            f.disc(lx, hy, 10f, on ? LIVE[k] : LENS_OFF[k]);
            if (on) {
                f.disc(lx, hy, 5.5f, Px.mix(LIVE[k], 0xFFFFFFFF, 0.45f));
                f.disc(lx - 3, hy - 4, 2f, 0xFFFFFFFF);
            } else f.disc(lx - 3, hy - 4, 1.5f, Px.mix(LENS_OFF[k], 0xFFFFFFFF, 0.15f));
        }
        // the board: an inset frame, tiles with a lighter top edge and a darker foot, a dot in each
        cell = side / N;
        gx = (w - cell * N) / 2;
        gy = boxY;
        f.rect(gx - 4, gy - 4, gx + cell * N + 4, gy + cell * N + 4, FRAME_EDGE);
        f.rect(gx - 2, gy - 2, gx + cell * N + 2, gy + cell * N + 2, TILE_DARK);
        for (int r = 0; r < N; r++)
            for (int c = 0; c < N; c++) {
                int tx = gx + c * cell, ty = gy + r * cell;
                f.rect(tx + 1, ty + 1, tx + cell - 1, ty + cell - 1, TILE);
                f.rect(tx + 1, ty + 1, tx + cell - 1, ty + 2, TILE_LIGHT);
                f.rect(tx + 1, ty + cell - 2, tx + cell - 1, ty + cell - 1, TILE_DARK);
                f.disc(tx + cell / 2, ty + cell / 2, 1.8f, 0xFF3A3E44);
            }
        // the wires: a dark edge, the colour, a light stripe along it; dimmer till joined
        int thick = Math.max(4, cell / 6);
        for (int pass = 0; pass < 3; pass++)
            for (int k = 0; k < COLOURS; k++) {
                int base = joined[k] ? LIVE[k] : Px.mix(LIVE[k], TILE, 0.4f);
                int col = pass == 0 ? Px.mix(base, 0xFF000000, 0.55f) : pass == 1 ? base : Px.mix(base, 0xFFFFFFFF, 0.35f);
                int tk = pass == 0 ? thick + 1 : pass == 1 ? thick : Math.max(1, thick / 3);
                int off = pass == 2 ? -thick / 2 : 0;
                for (int q = 0; q < len[k]; q++) {
                    int i = wire[k][q], cx = gx + (i % N) * cell + cell / 2, cy = gy + (i / N) * cell + cell / 2;
                    f.disc(cx + off, cy + off, tk, col);
                    if (q > 0) {
                        int j = wire[k][q - 1], px = gx + (j % N) * cell + cell / 2, py = gy + (j / N) * cell + cell / 2;
                        f.rect(Math.min(cx, px) - tk + off, Math.min(cy, py) - tk + off, Math.max(cx, px) + tk + off, Math.max(cy, py) + tk + off, col);
                    }
                }
            }
        // the terminals: a dark ring, a ring of the colour, a brass knob with a light on it
        for (int k = 0; k < COLOURS; k++)
            for (int e = 0; e < 2; e++) {
                int i = e == 0 ? endA[k] : endB[k], cx = gx + (i % N) * cell + cell / 2, cy = gy + (i / N) * cell + cell / 2;
                f.disc(cx, cy, cell * 0.32f, FRAME_EDGE);
                f.disc(cx, cy, cell * 0.28f, LIVE[k]);
                f.disc(cx, cy, cell * 0.17f, BRASS_DARK);
                f.disc(cx, cy, cell * 0.15f, KNOB);
                f.disc(cx - cell * 0.04f, cy - cell * 0.05f, cell * 0.06f, KNOB_LIGHT);
            }
        // the way back, high on the left: a dark button in a bronze ring, a cream arrow
        backX = margin + 10;
        backY = Math.max(12, boxY - 108);
        f.disc(backX, backY, 12f, BRONZE);
        f.disc(backX, backY, 10f, 0xFF1E2024);
        f.rect(backX - 5, backY - 1, backX + 6, backY + 1, CREAM);
        for (int q = 0; q < 4; q++) {
            f.rect(backX - 5 + q, backY - 1 - q, backX - 3 + q, backY - q, CREAM);
            f.rect(backX - 5 + q, backY + q, backX - 3 + q, backY + 1 + q, CREAM);
        }
    }
}
