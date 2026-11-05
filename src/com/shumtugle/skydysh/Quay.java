package com.shumtugle.skydysh;

import java.util.Random;

/**
 * The containers in the port, and the crane over them: a sorting puzzle. The field has bays in
 * rows of three, a stack of up to four containers on each; there are as many colours as full
 * stacks, and two bays more. The crane takes the top container off a stack and puts it on an
 * empty bay or on a stack whose top is of its colour, not over four high. The field is done when
 * every stack on it is four of one colour.
 *
 * A field is dealt by taking a sorted one apart with moves that can each be made back: a
 * container is lifted off a stack only where what is under it is of its colour (or nothing), so
 * the crane could have put it there; so a field always has a way back to order.
 *
 * The crane is a gantry on two rails along the field: its beam across, a trolley along the beam
 * with the cabin under it and the hoist, the spreader on four ropes. Touched, a stack: the crane
 * goes over it and takes its top up; touched again, another: it carries it there and lets it
 * down. Up first, then across, then down, as a crane does.
 */
final class Quay {
    // the field's middle, in the port's cell; its bays: so far apart across and along, four to a row
    static final float CX = City.PORT_I * City.CELL + City.CELL * 0.5f, CZ = City.PORT_J * City.CELL + City.CELL * 0.5f;
    static final float COL = 5.2f, ROW = 7.7f;
    static final int ACROSS = 3, SLOTS = 12, HIGH = 4;
    // a container: half its width, half its length, its height
    static final float HALF_WIDE = 1.22f, HALF_LONG = 3.03f, HIGH_EACH = 2.6f;
    // the fence round the field (half its sizes); the crane's rails (half the length), half its span
    static final float FENCE_X = 11.5f, FENCE_Z = 16.5f, RAILS_HALF = 18.5f, GANTRY_HALF = 9f;
    // the beam's underside, the height the crane carries a container at (its bottom)
    static final float BEAM = 16.6f, CARRY = HIGH * HIGH_EACH + 1.2f;
    // the tower up to it at the fence's south-west corner, its top
    static final float TOWER_X = CX - FENCE_X - 2.5f, TOWER_Z = CZ - FENCE_Z - 2.5f, TOWER_TOP = 16f;
    // where the crane stands when no one works it: at the rails' north end, the far side from the shore road
    static final float PARK_Z = CZ + RAILS_HALF - 2.5f;
    // how fast the hook goes up and down, across
    static final float LIFTS = 24f, CROSSES = 32f, EASE = 14f;
    // the fewest colours, the most
    static final int FEWEST = 6, MOST = 9;
    /** Paid once, the first field done. */
    static final int REWARD = 10000;
    static final int[] COLORS = {0x2F5FB8, 0x8A3CC0, 0xE8B830, 0xDCD2B8, 0x4E9A34, 0x1FA3A0, 0xC23A30, 0xE07A26, 0xE0609A};

    // what a touch did
    static final int NONE = 0, PICK = 1, PUT = 2, BACK = 3, NO = 4;

    /** How many bays the field has now, how many colours. */
    int bays, colors;
    final int[][] stack = new int[SLOTS][HIGH];
    final int[] high = new int[SLOTS];
    /** How many moves made on this field. */
    int moves;
    boolean solved;
    // the crane: the trolley across, the gantry along, the spreader's bottom (that of what it carries)
    float hx = CX, hz = PARK_Z, hy = CARRY;
    // what hangs from it (-1: nothing), from which bay; whether it has been taken up yet; where it goes (-1: nowhere yet)
    int held = -1, from = -1, to = -1;
    private boolean attached;
    /** A touch made while the crane was still on its way: done when it is free. */
    private int waiting = -1;
    // the moves made, to go back over: from, to
    private final int[] undoFrom = new int[512], undoTo = new int[512];
    private int undoN;
    private boolean undoing;
    private float sway, swayV;
    private final Random rnd;

    Quay(long seed) {
        rnd = new Random(seed);
    }

    /** A new field of so many colours, the crane over it, nothing in its hands. */
    void deal(int colours) {
        colors = Math.max(FEWEST, Math.min(MOST, colours));
        bays = colors + 2;
        for (int tries = 0; ; tries++) {
            for (int b = 0; b < bays; b++) {
                high[b] = b < colors ? HIGH : 0;
                for (int k = 0; k < HIGH; k++) stack[b][k] = b;
            }
            // taken apart by moves that could each be made back
            for (int step = 0; step < 300 * colors; step++) {
                int a = rnd.nextInt(bays), b = rnd.nextInt(bays);
                if (a == b || high[a] == 0 || high[b] >= HIGH) continue;
                int c = stack[a][high[a] - 1];
                if (high[a] > 1 && stack[a][high[a] - 2] != c) continue;
                if (high[b] > 0 && stack[b][high[b] - 1] == c) continue;
                high[a]--;
                stack[b][high[b]++] = c;
            }
            // the colours put in an order of their own, the bays shuffled
            int[] tint = new int[colors];
            for (int k = 0; k < colors; k++) tint[k] = k;
            for (int k = colors - 1; k > 0; k--) {
                int j = rnd.nextInt(k + 1), t = tint[k];
                tint[k] = tint[j];
                tint[j] = t;
            }
            for (int b = 0; b < bays; b++)
                for (int k = 0; k < high[b]; k++) stack[b][k] = tint[stack[b][k]];
            for (int b = bays - 1; b > 0; b--) {
                int j = rnd.nextInt(b + 1);
                int[] s = stack[b];
                stack[b] = stack[j];
                stack[j] = s;
                int t = high[b];
                high[b] = high[j];
                high[j] = t;
            }
            // well mixed: hardly a stack already in order
            int ordered = 0;
            for (int b = 0; b < bays; b++) if (high[b] == HIGH && uniform(b)) ordered++;
            if (ordered <= 1 || tries > 40) break;
        }
        moves = 0;
        undoN = 0;
        solved = false;
        held = from = to = -1;
        waiting = -1;
        attached = false;
        undoing = false;
    }

    private boolean uniform(int b) {
        for (int k = 1; k < high[b]; k++) if (stack[b][k] != stack[b][0]) return false;
        return true;
    }

    /** Whether every stack on the field is four of one colour. */
    boolean done() {
        for (int b = 0; b < bays; b++) {
            if (high[b] == 0) continue;
            if (high[b] != HIGH || !uniform(b)) return false;
        }
        return held < 0;
    }

    /** Whether what hangs from the crane may go on bay b. */
    boolean fits(int b) {
        if (held < 0 || high[b] >= HIGH) return false;
        return high[b] == 0 || stack[b][high[b] - 1] == held;
    }

    /** Whether the crane is on its way somewhere, not waiting. */
    boolean busy() {
        return held >= 0 && (!attached || to >= 0) || held < 0 && to >= 0;
    }

    /** Bay b touched: what that did (NONE to NO). */
    int touch(int b) {
        if (solved || b < 0 || b >= bays) return NONE;
        if (busy()) {
            waiting = b;
            return NONE;
        }
        if (held < 0) {
            if (high[b] == 0) return NO;
            held = stack[b][--high[b]];
            from = b;
            attached = false;
            to = -1;
            return PICK;
        }
        if (b == from) {
            to = b;
            return BACK;
        }
        if (!fits(b)) {
            swayV += 2.5f;
            return NO;
        }
        to = b;
        if (undoN < undoFrom.length) {
            undoFrom[undoN] = from;
            undoTo[undoN] = b;
            undoN++;
        }
        moves++;
        return PUT;
    }

    /** Let go of at once (she leaves the cabin): what hangs back on its stack, the crane free where it is. */
    void putBack() {
        if (held >= 0) {
            stack[from][high[from]++] = held;
            if (to >= 0 && to != from && undoN > 0 && !undoing) {
                undoN--;
                moves = Math.max(0, moves - 1);
            }
            held = -1;
        }
        to = from = -1;
        waiting = -1;
        attached = false;
        undoing = false;
    }

    /** For the desktop stand: the field put in order but for one container, on an empty bay of its own. */
    void standNearly() {
        putBack();
        for (int b = 0; b < bays; b++) high[b] = 0;
        for (int b = 0; b < colors; b++) {
            for (int k = 0; k < HIGH; k++) stack[b][k] = b;
            high[b] = HIGH;
        }
        // the top of the last colour's stack moved onto the first empty bay
        high[colors - 1]--;
        stack[colors][0] = colors - 1;
        high[colors] = 1;
        solved = false;
        undoN = 0;
    }

    /** Whether a move can be taken back now. */
    boolean canUndo() {
        return !solved && !busy() && (held >= 0 || undoN > 0);
    }

    /** The last move taken back (or what hangs put back where it came from); false if none. */
    boolean undo() {
        if (!canUndo()) return false;
        if (held >= 0) {
            to = from;
            return true;
        }
        undoN--;
        int f = undoFrom[undoN], t = undoTo[undoN];
        held = stack[t][--high[t]];
        from = t;
        attached = false;
        to = f;
        undoing = true;
        moves = Math.max(0, moves - 1);
        return true;
    }

    /** Where bay b stands: x, z into out. */
    void bay(int b, float[] out) {
        int row = b / ACROSS, inRow = Math.min(ACROSS, bays - row * ACROSS), col = b % ACROSS;
        int rows = (bays + ACROSS - 1) / ACROSS;
        out[0] = CX + (col - (inRow - 1) * 0.5f) * COL;
        // the first row the farthest from the shore road, the last the nearest
        out[1] = CZ + ((rows - 1) * 0.5f - row) * ROW;
    }

    private final float[] at = new float[2];

    /** A frame on: the crane on its way; what it brought: 1 a container let down, 2 taken up, 0 nothing. */
    int update(float dt) {
        sway += swayV * dt;
        swayV += (-sway * 40f - swayV * 3.5f) * dt;
        int got = 0;
        float tx, ty, tz;
        if (held >= 0 && !attached) {
            // down to what it is to take, where it lies on its stack
            bay(from, at);
            tx = at[0];
            tz = at[1];
            ty = City.WALK_TOP + high[from] * HIGH_EACH;
            if (move(dt, tx, ty, tz)) {
                attached = true;
                got = 2;
            }
        } else if (to >= 0) {
            bay(to, at);
            tx = at[0];
            tz = at[1];
            ty = held >= 0 ? City.WALK_TOP + high[to] * HIGH_EACH : CARRY;
            if (move(dt, tx, ty, tz)) {
                if (held >= 0) {
                    stack[to][high[to]++] = held;
                    held = -1;
                    got = 1;
                    swayV -= 1.2f;
                    if (done()) solved = true;
                }
                // up again, clear of the stack, and free
                to = -1;
                from = -1;
                undoing = false;
            }
        } else {
            // holding it up over where it came from, or free: up to carrying height
            if (held >= 0) {
                bay(from, at);
                move(dt, at[0], CARRY, at[1]);
            } else move(dt, hx, CARRY, hz);
            if (waiting >= 0 && !busy()) {
                int w = waiting;
                waiting = -1;
                lastWaited = touch(w);
            }
        }
        return got;
    }

    /** What the touch made while the crane was busy did, once it was done (NONE if none). */
    int lastWaited = NONE;

    /** The hook a step toward (x, y, z): up to carrying height first, then across, then down; true when there. */
    private boolean move(float dt, float x, float y, float z) {
        float dx = x - hx, dz = z - hz, d = (float) Math.sqrt(dx * dx + dz * dz);
        if (d > 0.04f) {
            if (hy < CARRY - 0.05f) {
                hy = Math.min(CARRY, hy + LIFTS * dt);
                return false;
            }
            float go = Math.min(d, Math.max(5f, Math.min(CROSSES, d * EASE)) * dt);
            hx += dx / d * go;
            hz += dz / d * go;
            return false;
        }
        hx = x;
        hz = z;
        float dy = y - hy;
        if (Math.abs(dy) > 0.02f) {
            float go = Math.min(Math.abs(dy), Math.max(5f, Math.min(LIFTS, Math.abs(dy) * EASE)) * dt);
            hy += Math.signum(dy) * go;
            return false;
        }
        hy = y;
        return true;
    }

    // where the cabin hangs off the trolley
    static final float CAB_X = 3.1f, CAB_Z = -2.75f;

    /** Where Frida sits in the cabin: x, y (the seat), z, into out; she faces down the field toward the shore. */
    void seat(float[] out) {
        out[0] = hx + CAB_X;
        out[1] = BEAM - 3.2f;
        out[2] = hz + CAB_Z + 0.2f;
    }

    // ---- drawing ----

    private static final float[] ALONG_X = {0f, 0f, 1f, 0f, 1f, 0f, -1f, 0f, 0f};

    /**
     * A container at (x, bottom y, z), its length along z (or x): a box of its colour, ribs down
     * its sides a shade darker, its door end with two bars and the handles, the corner posts.
     */
    static void box(DrawList d, DrawList small, float x, float y, float z, boolean alongX, int color) {
        float[] t = alongX ? ALONG_X : null;
        float my = y + HIGH_EACH * 0.5f;
        int dark = Street.mix(color, 0x1A1A1A, 0.28f), light = Street.mix(color, 0xFFFFFF, 0.22f);
        d.box(x, my, z, HALF_WIDE, HIGH_EACH * 0.5f - 0.02f, HALF_LONG, t, color, DrawList.PAINT);
        // ribs down both long sides
        for (int k = -4; k <= 4; k++) {
            float lz = k * 0.66f;
            for (int e = -1; e <= 1; e += 2) {
                float lx = e * (HALF_WIDE + 0.015f);
                small.box(alongX ? x + lz : x + lx, my, alongX ? z - lx : z + lz, alongX ? 0.09f : 0.02f, HIGH_EACH * 0.5f - 0.12f,
                        alongX ? 0.02f : 0.09f, null, dark, DrawList.PAINT);
            }
        }
        // the frame: the top rails and corner posts
        for (int e = -1; e <= 1; e += 2) {
            float lz = e * (HALF_LONG - 0.06f);
            small.box(alongX ? x + lz : x, my, alongX ? z : z + lz, alongX ? 0.07f : HALF_WIDE + 0.02f, HIGH_EACH * 0.5f, alongX ? HALF_WIDE + 0.02f : 0.07f,
                    null, dark, DrawList.PAINT);
        }
        // the door end (toward the shore): two bars and the handles, light
        float dz = -(HALF_LONG + 0.02f);
        for (int e = -1; e <= 1; e += 2) {
            float lx = e * 0.4f;
            small.box(alongX ? x + dz : x + lx, my, alongX ? z - lx : z + dz, alongX ? 0.02f : 0.04f, HIGH_EACH * 0.5f - 0.15f, alongX ? 0.04f : 0.02f,
                    null, light, DrawList.POLISHED);
        }
    }

    private static final int YELLOW = 0xE8B830, STEEL = 0x5A5E62, ROPE = 0x2A2A2A, PAINT_LINE = 0xE0B02A;

    /** The field: the bays painted on the concrete, the stacks, the crane with what it carries; she in its cabin is drawn by her own. */
    void draw(DrawList d, boolean worked) {
        DrawList s = d;
        // the bays: a yellow frame each, a stripe at the end toward the shore
        for (int b = 0; b < bays; b++) {
            bay(b, at);
            float x = at[0], z = at[1], w = HALF_WIDE + 0.45f, l = HALF_LONG + 0.5f, y = City.WALK_TOP + 0.02f;
            for (int e = -1; e <= 1; e += 2) {
                s.box(x + e * w, y, z, 0.08f, 0.01f, l, null, PAINT_LINE, DrawList.PAINT);
                s.box(x, y, z + e * l, w, 0.01f, 0.08f, null, PAINT_LINE, DrawList.PAINT);
            }
            for (float u = -w + 0.3f; u < w; u += 0.5f) s.box(x + u, y, z - l + 0.35f, 0.12f, 0.01f, 0.2f, null, 0x2A2A2A, DrawList.PAINT);
            for (int k = 0; k < high[b]; k++) box(d, d, x, City.WALK_TOP + k * HIGH_EACH, z, false, COLORS[stack[b][k]]);
        }
        // what hangs, or still lies where the crane is to take it from
        if (held >= 0) {
            if (attached) box(d, d, hx + sway * 0.15f, hy, hz, false, COLORS[held]);
            else {
                bay(from, at);
                box(d, d, at[0], City.WALK_TOP + high[from] * HIGH_EACH, at[1], false, COLORS[held]);
            }
        }
        drawCrane(d, worked);
    }

    /** The gantry: two legs on the rails, the beam across, the trolley, the cabin, the ropes, the spreader. */
    private void drawCrane(DrawList d, boolean worked) {
        float gz = hz, beamY = BEAM + 0.6f;
        for (int e = -1; e <= 1; e += 2) {
            float lx = CX + e * GANTRY_HALF;
            // a leg: two posts on a bogie, braced
            for (int f = -1; f <= 1; f += 2) {
                float lz = gz + f * 2.6f;
                d.box(lx, (BEAM + 0.5f) * 0.5f + 0.4f, lz, 0.35f, (BEAM + 0.5f) * 0.5f - 0.4f, 0.35f, null, YELLOW, DrawList.PAINT);
            }
            d.box(lx, 0.55f, gz, 0.6f, 0.42f, 3.6f, null, YELLOW, DrawList.PAINT);
            for (int w = -2; w <= 2; w++) d.box(lx, 0.32f, gz + w * 1.5f, 0.66f, 0.28f, 0.28f, null, 0x3A3A3A, DrawList.TYRE);
            d.box(lx, BEAM * 0.55f, gz, 0.12f, 0.12f, 2.6f, null, YELLOW, DrawList.PAINT);
            // the warning stripes low on the legs
            for (int k = 0; k < 3; k++) d.box(lx, 1.4f + k * 0.5f, gz - 2.6f - 0.36f, 0.36f, 0.12f, 0.01f, null, k % 2 == 0 ? 0x1A1A1A : YELLOW, DrawList.PAINT);
        }
        // the beam across, two girders, a walk between
        for (int f = -1; f <= 1; f += 2) d.box(CX, beamY, gz + f * 1.1f, GANTRY_HALF + 0.4f, 0.45f, 0.22f, null, YELLOW, DrawList.PAINT);
        // the trolley on it, the hoist's drum
        d.box(hx, beamY + 0.2f, gz, 1.6f, 0.75f, 1.9f, null, YELLOW, DrawList.PAINT);
        d.box(hx, beamY + 0.25f, gz, 0.5f, 0.5f, 1.2f, null, STEEL, DrawList.POLISHED);
        // the cabin hung off the trolley's east side: glass on its sides in a yellow frame, open
        // over her head (a cage roof), so she shows in it from above; a lamp at its corner
        float cx = hx + CAB_X, cy = BEAM - 2.2f, cz = gz + CAB_Z, ch = 1.35f;
        d.box(cx, cy - ch - 0.05f, cz, ch, 0.1f, ch, null, YELLOW, DrawList.PAINT);
        for (int a = -1; a <= 1; a += 2)
            for (int b = -1; b <= 1; b += 2) d.box(cx + a * (ch - 0.05f), cy, cz + b * (ch - 0.05f), 0.06f, ch, 0.06f, null, YELLOW, DrawList.PAINT);
        for (int a = -1; a <= 1; a += 2) {
            d.box(cx, cy + ch, cz + a * (ch - 0.05f), ch, 0.06f, 0.06f, null, YELLOW, DrawList.PAINT);
            d.box(cx + a * (ch - 0.05f), cy + ch, cz, 0.06f, 0.06f, ch, null, YELLOW, DrawList.PAINT);
        }
        d.box(cx, cy, cz - ch + 0.02f, ch - 0.06f, ch - 0.06f, 0.02f, null, 0x8FB8C8, DrawList.CLEAR);
        d.box(cx + ch - 0.02f, cy, cz, 0.02f, ch - 0.06f, ch - 0.06f, null, 0x8FB8C8, DrawList.CLEAR);
        d.box(cx + ch - 0.1f, cy + ch + 0.15f, cz - ch + 0.1f, 0.12f, 0.12f, 0.12f, null, worked ? 0xFFE08A : 0xB0A070, worked ? DrawList.GLOW : DrawList.PLAIN);
        d.box(cx - ch - 0.3f, cy + ch - 0.2f, cz, 0.3f, 0.12f, 0.5f, null, YELLOW, DrawList.PAINT);
        // the ropes down to the spreader, the spreader over what it carries
        float sx = hx + sway * 0.15f, top = hy + HIGH_EACH + 0.3f, ropeTop = beamY - 0.5f;
        for (int a = -1; a <= 1; a += 2)
            for (int b = -1; b <= 1; b += 2) {
                float rx = sx + a * 0.6f, rz = gz + b * 1.6f;
                d.box((hx + a * 0.4f + rx) * 0.5f, (ropeTop + top) * 0.5f, (gz + b * 0.5f + rz) * 0.5f, 0.03f, (ropeTop - top) * 0.5f, 0.03f, null, ROPE, DrawList.PLAIN);
            }
        // the spreader: a frame round the container's top, its ends striped, open in the middle
        for (int a = -1; a <= 1; a += 2) {
            d.box(sx + a * (HALF_WIDE - 0.05f), top - 0.12f, gz, 0.12f, 0.14f, HALF_LONG + 0.08f, null, YELLOW, DrawList.PAINT);
            d.box(sx, top - 0.12f, gz + a * (HALF_LONG - 0.05f), HALF_WIDE + 0.08f, 0.14f, 0.16f, null, 0x1A1A1A, DrawList.PAINT);
        }
        d.box(sx, top - 0.05f, gz, 0.14f, 0.1f, HALF_LONG - 0.1f, null, YELLOW, DrawList.PAINT);
    }
}
