package com.shumtugle.skydysh;

/**
 * The buttons and signs laid over the frame: round panes of dark glass, half see-through, a rim
 * lit turquoise at the top left going to pink at the bottom right, a soft glow round it, and a
 * sign in white on the glass. They are drawn here once, from shapes worked out in code, into a
 * sheet of cells (CELLS by CELLS, SIZE pixels each, premultiplied); the renderer lays any cell of
 * it over the frame, at any size, as smooth as the screen is fine (see DrawList.badge).
 *
 * A sign is made of plain shapes, each given by how far a point is from its edge (less than
 * nothing inside): boxes, discs, rods, polygons, joined, cut out of each other, thickened.
 */
final class Badges {
    static final int LIFT = 0, DOWN = 1, UP = 2, LEFT = 3, RIGHT = 4, MALLET = 5, WHEEL = 6, JETPACK = 7,
            THROW = 8, PICK_UP = 9, MAP = 10, HELP = 11, PAUSE = 12, PLAY = 13, HOME = 14, LADDER = 15, EXIT = 16, ENTER = 17;
    // the stick: its round pane with nothing on it, and the knob in it, a white disc
    static final int BLANK = 18, KNOB = 19;
    // the ways to play, one finger or two thumbs; the pedals at the wheel, the throttle and the brake
    static final int ONE_FINGER = 20, TWO_THUMBS = 21, GAS = 22, BRAKE = 23;
    static final int COUNT = 24, CELLS = 5, SIZE = 160;
    // the pane's edge, and how much of it the sign takes up, as parts of half a cell
    private static final float PANE = 0.8f, SIGN = 0.45f;

    private static int[] sheet;

    private Badges() {
    }

    /** The sheet, ARGB premultiplied, row after row from the top; drawn the first time it is asked for. */
    static synchronized int[] sheet() {
        if (sheet == null) {
            int side = CELLS * SIZE;
            int[] s = new int[side * side];
            for (int k = 0; k < COUNT; k++) cell(s, side, k);
            sheet = s;
        }
        return sheet;
    }

    private static void cell(int[] s, int side, int k) {
        int ox = (k % CELLS) * SIZE, oy = (k / CELLS) * SIZE;
        float px = 2f / SIZE;                         // one pixel, in the cell's own measure
        for (int y = 0; y < SIZE; y++)
            for (int x = 0; x < SIZE; x++) {
                float u = (x + 0.5f) * px - 1f, v = 1f - (y + 0.5f) * px;
                s[(oy + y) * side + ox + x] = pixel(k, u, v, px);
            }
    }

    // the colour being laid down, premultiplied, 0 to 1
    private static float cr, cg, cb, ca;

    private static void over(int rgb, float a) {
        if (a <= 0f) return;
        a = Math.min(1f, a);
        float r = ((rgb >> 16) & 0xFF) / 255f, g = ((rgb >> 8) & 0xFF) / 255f, b = (rgb & 0xFF) / 255f;
        cr = cr * (1f - a) + r * a;
        cg = cg * (1f - a) + g * a;
        cb = cb * (1f - a) + b * a;
        ca = ca * (1f - a) + a;
    }

    private static float cover(float d, float px) {
        return Math.max(0f, Math.min(1f, 0.5f - d / px));
    }

    private static int mix(int a, int b, float t) {
        return Street.mix(a, b, Math.max(0f, Math.min(1f, t)));
    }

    static final int TURQUOISE = 0x4FE8EE, PINK = 0xFF52C4, GLASS_TOP = 0x0F3440, GLASS_LOW = 0x22163A, WHITE = 0xF4FAFF;

    private static int pixel(int k, float u, float v, float px) {
        cr = cg = cb = ca = 0f;
        float r = (float) Math.sqrt(u * u + v * v);
        // the rim's colour round it: turquoise at the top left, pink at the bottom right
        float t = r > 1e-4f ? ((u - v) / r * 0.7071f + 1f) * 0.5f : 0.5f;
        int rim = mix(TURQUOISE, PINK, t * 1.15f - 0.08f);
        // a glow outside the rim
        if (r > PANE - 0.02f) over(rim, 0.55f * (float) Math.exp(-Math.max(0f, r - PANE) / 0.065f));
        // the glass: dark, darker and bluer below, half see-through; the rim's light inside its edge
        float inside = cover(r - PANE, px);
        if (inside > 0f) {
            over(mix(GLASS_TOP, GLASS_LOW, (0.8f - v) / 1.6f), 0.6f * inside);
            over(rim, 0.38f * inside * (float) Math.exp(-(PANE - r) / 0.09f));
            // a sheen across the top
            float gx = u / 0.52f, gy = (v - 0.44f) / 0.26f, gl = gx * gx + gy * gy;
            if (gl < 1f) over(0xFFFFFF, 0.1f * (1f - gl) * inside);
        }
        // the rim itself
        over(mix(rim, 0xFFFFFF, 0.3f), cover(Math.abs(r - PANE) - 0.028f, px));
        // the sign: a glow round it, then the white of it
        if (r < PANE) {
            float d = sign(k, u / SIGN, v / SIGN) * SIGN;
            if (d < 0.2f) over(mix(TURQUOISE, 0xFFFFFF, 0.4f), 0.32f * (float) Math.exp(-Math.max(0f, d) / 0.04f) * inside);
            over(WHITE, cover(d, px) * inside);
        }
        int a = Math.round(Math.min(1f, ca) * 255f);
        return a << 24 | Math.round(Math.min(1f, cr) * 255f) << 16 | Math.round(Math.min(1f, cg) * 255f) << 8
                | Math.round(Math.min(1f, cb) * 255f);
    }

    // ---- the signs, in their own measure: -1 to 1 each way, y up ----

    /** How far (x, y) is from the edge of sign k: less than nothing inside it. */
    static float sign(int k, float x, float y) {
        switch (k) {
            case LIFT: {
                float frame = Math.abs(rbox(x, y, 0f, -0.06f, 0.74f, 0.82f, 0.14f)) - 0.085f;
                float lamp = rbox(x, y, 0f, 0.86f, 0.24f, 0.08f, 0.05f);
                float up = poly(x, y, 0f, 0.6f, 0.34f, 0.08f, -0.34f, 0.08f);
                float down = poly(x, y, 0f, -0.72f, -0.34f, -0.2f, 0.34f, -0.2f);
                return min(frame, lamp, up, down);
            }
            case DOWN:
                return arrow(x, y);
            case UP:
                return arrow(x, -y);
            case LEFT:
                return arrow(y, x);
            case RIGHT:
                return arrow(y, -x);
            case MALLET: {
                // the head across the end of a short handle, two bands round it; a cap on the handle's end
                float c = 0.7071f, hx = x - 0.24f, hy = y - 0.24f;
                float lx = (hx + hy) * c, ly = (hy - hx) * c;
                float head = rbox(lx, ly, 0f, 0f, 0.3f, 0.62f, 0.07f);
                float bands = Math.min(box(lx, ly, 0f, 0.3f, 0.4f, 0.035f), box(lx, ly, 0f, -0.3f, 0.4f, 0.035f));
                head = Math.max(head, -bands);
                float handle = seg(x, y, -0.62f, -0.62f, 0.05f, 0.05f, 0.11f);
                float cap = circle(x, y, -0.7f, -0.7f, 0.17f);
                return min(head, handle, cap);
            }
            case WHEEL: {
                float rim = Math.abs(circle(x, y, 0f, 0f, 0.8f)) - 0.12f;
                float hub = Math.max(circle(x, y, 0f, 0f, 0.22f), -circle(x, y, 0f, 0f, 0.08f));
                float spokes = min(seg(x, y, 0f, 0f, -0.72f, 0.12f, 0.1f), seg(x, y, 0f, 0f, 0.72f, 0.12f, 0.1f),
                        seg(x, y, 0f, 0f, 0f, -0.74f, 0.1f));
                return Math.min(rim, Math.max(Math.min(hub, spokes), -circle(x, y, 0f, 0f, 0.08f)));
            }
            case JETPACK: {
                // two tanks side by side, a band across them, a nozzle and a flame under each
                float d = 1e9f;
                for (int e = -1; e <= 1; e += 2) {
                    float cx = e * 0.42f;
                    d = min(d, rbox(x, y, cx, 0.2f, 0.22f, 0.54f, 0.2f), box(x, y, cx, -0.42f, 0.12f, 0.08f),
                            poly(x, y, cx - 0.13f, -0.58f, cx + 0.13f, -0.58f, cx, -0.98f));
                }
                return Math.min(d, box(x, y, 0f, 0.36f, 0.3f, 0.07f));
            }
            case THROW: {
                // a ball thrown along a dashed curve, the way it goes at the end
                float cx = 0f, cy = -0.62f, rr = 0.86f;
                float dx = x - cx, dy = y - cy, a = (float) Math.atan2(dy, dx);
                float arc = Math.abs((float) Math.sqrt(dx * dx + dy * dy) - rr) - 0.07f;
                float from = 0.75f, to = 2.75f;
                if (a < from || a > to) arc = 1e9f;
                else if (((a - from) / 0.32f) % 1f > 0.62f) arc = Math.max(arc, 0.05f);
                float ta = 0.62f, ex = cx + (float) Math.cos(ta) * rr, ey = cy + (float) Math.sin(ta) * rr;
                float tx = (float) Math.sin(ta), ty = -(float) Math.cos(ta), nx = (float) Math.cos(ta), ny = (float) Math.sin(ta);
                float head = poly(x, y, ex + tx * 0.28f, ey + ty * 0.28f, ex - tx * 0.06f + nx * 0.2f, ey - ty * 0.06f + ny * 0.2f,
                        ex - tx * 0.06f - nx * 0.2f, ey - ty * 0.06f - ny * 0.2f);
                float ba = 2.95f, ball = circle(x, y, cx + (float) Math.cos(ba) * rr, cy + (float) Math.sin(ba) * rr, 0.22f);
                return min(arc, head, ball);
            }
            case PICK_UP: {
                float front = box(x, y, -0.12f, -0.5f, 0.4f, 0.38f);
                float top = poly(x, y, -0.52f, -0.06f, 0.28f, -0.06f, 0.58f, 0.16f, -0.22f, 0.16f);
                float right = poly(x, y, 0.34f, -0.12f, 0.34f, -0.88f, 0.58f, -0.66f, 0.58f, 0.1f);
                float shaft = box(x, y, 0.03f, 0.46f, 0.08f, 0.17f);
                float head = poly(x, y, -0.24f, 0.6f, 0.03f, 0.96f, 0.3f, 0.6f);
                return min(front, top, right, shaft, head);
            }
            case MAP: {
                float map = min(poly(x, y, -0.86f, -0.66f, -0.3f, -0.86f, -0.3f, 0.34f, -0.86f, 0.54f),
                        poly(x, y, -0.24f, -0.86f, 0.24f, -0.66f, 0.24f, 0.54f, -0.24f, 0.34f),
                        poly(x, y, 0.3f, -0.66f, 0.86f, -0.86f, 0.86f, 0.34f, 0.3f, 0.54f));
                float pin = Math.min(circle(x, y, 0.1f, 0.5f, 0.3f), poly(x, y, -0.16f, 0.36f, 0.1f, -0.12f, 0.36f, 0.36f));
                map = Math.max(map, -(pin - 0.08f));
                return Math.min(map, Math.max(pin, -circle(x, y, 0.1f, 0.52f, 0.11f)));
            }
            case HELP: {
                // a hook over and round to the right, down into a stem; a dot under it
                float d = 1e9f, cx = 0f, cy = 0.36f, rr = 0.36f;
                float pxp = cx + (float) Math.cos(2.9f) * rr, pyp = cy + (float) Math.sin(2.9f) * rr;
                for (int i = 1; i <= 14; i++) {
                    float a = 2.9f - i * (2.9f + 1.5708f) / 14f;
                    float qx = cx + (float) Math.cos(a) * rr, qy = cy + (float) Math.sin(a) * rr;
                    d = Math.min(d, seg(x, y, pxp, pyp, qx, qy, 0.14f));
                    pxp = qx;
                    pyp = qy;
                }
                d = Math.min(d, seg(x, y, pxp, pyp, 0f, -0.2f, 0.14f));
                return Math.min(d, circle(x, y, 0f, -0.66f, 0.16f));
            }
            case BLANK:
                return 1e9f;
            case ONE_FINGER: {
                // a fingertip coming down, and the ring of its touch on the glass under it
                float finger = rbox(x, y, 0f, 0.28f, 0.2f, 0.52f, 0.19f);
                float ring = Math.abs(circle(x, y, 0f, -0.56f, 0.26f)) - 0.06f;
                return Math.min(finger, Math.max(ring, -box(x, y, 0f, -0.3f, 0.3f, 0.16f)));
            }
            case TWO_THUMBS: {
                float a = rbox(x, y, -0.4f, 0.2f, 0.19f, 0.44f, 0.18f), b = rbox(x, y, 0.4f, 0.2f, 0.19f, 0.44f, 0.18f);
                float ra = Math.abs(circle(x, y, -0.4f, -0.56f, 0.22f)) - 0.055f, rb = Math.abs(circle(x, y, 0.4f, -0.56f, 0.22f)) - 0.055f;
                float cut = Math.min(box(x, y, -0.4f, -0.34f, 0.26f, 0.12f), box(x, y, 0.4f, -0.34f, 0.26f, 0.12f));
                return min(a, b, Math.max(Math.min(ra, rb), -cut));
            }
            case GAS: {
                // the throttle: a tall narrow pedal, grooved across
                float pedal = rbox(x, y, 0f, 0f, 0.3f, 0.78f, 0.12f);
                for (int g = -2; g <= 2; g++) pedal = Math.max(pedal, -box(x, y, 0f, g * 0.28f, 0.2f, 0.045f));
                return pedal;
            }
            case BRAKE: {
                // the brake: a wide pedal, grooved across
                float pedal = rbox(x, y, 0f, 0f, 0.7f, 0.48f, 0.14f);
                for (int g = -1; g <= 1; g++) pedal = Math.max(pedal, -box(x, y, 0f, g * 0.26f, 0.56f, 0.05f));
                return pedal;
            }
            case KNOB:
                return circle(x, y, 0f, 0f, 0.62f);
            case PAUSE:
                return Math.min(rbox(x, y, -0.32f, 0f, 0.17f, 0.7f, 0.06f), rbox(x, y, 0.32f, 0f, 0.17f, 0.7f, 0.06f));
            case PLAY:
                return poly(x, y, -0.42f, 0.7f, -0.42f, -0.7f, 0.74f, 0f) - 0.05f;
            case HOME: {
                float body = box(x, y, 0f, -0.42f, 0.56f, 0.46f);
                float roof = poly(x, y, -0.86f, 0.0f, 0f, 0.82f, 0.86f, 0.0f);
                float chimney = box(x, y, 0.46f, 0.5f, 0.11f, 0.26f);
                float door = box(x, y, 0f, -0.62f, 0.17f, 0.28f);
                return Math.max(min(body, roof, chimney), -door);
            }
            case ENTER: {
                // in: a doorway, open on its left, an arrow going in through it
                float frame = Math.abs(rbox(x, y, 0.32f, 0f, 0.44f, 0.82f, 0.06f)) - 0.08f;
                frame = Math.max(frame, -box(x, y, -0.12f, 0f, 0.2f, 0.3f));
                float shaft = box(x, y, -0.36f, 0f, 0.42f, 0.11f);
                float head = poly(x, y, 0.06f, 0.36f, 0.5f, 0f, 0.06f, -0.36f);
                return min(frame, shaft, head);
            }
            case EXIT: {
                // out: a doorway, open on its right, an arrow going out through it
                float frame = Math.abs(rbox(x, y, -0.32f, 0f, 0.44f, 0.82f, 0.06f)) - 0.08f;
                frame = Math.max(frame, -box(x, y, 0.12f, 0f, 0.2f, 0.3f));
                float shaft = box(x, y, 0.2f, 0f, 0.42f, 0.11f);
                float head = poly(x, y, 0.5f, 0.36f, 0.94f, 0f, 0.5f, -0.36f);
                return min(frame, shaft, head);
            }
            default: {
                // a ladder: two rails, four rungs
                float d = Math.min(seg(x, y, -0.4f, -0.9f, -0.4f, 0.9f, 0.09f), seg(x, y, 0.4f, -0.9f, 0.4f, 0.9f, 0.09f));
                for (int i = 0; i < 4; i++) d = Math.min(d, box(x, y, 0f, -0.6f + i * 0.4f, 0.4f, 0.06f));
                return d;
            }
        }
    }

    /** An arrow pointing down: a shaft and a broad head. */
    private static float arrow(float x, float y) {
        return Math.min(box(x, y, 0f, 0.32f, 0.2f, 0.52f), poly(x, y, -0.64f, -0.08f, 0f, -0.86f, 0.64f, -0.08f));
    }

    private static float min(float a, float b, float c) {
        return Math.min(a, Math.min(b, c));
    }

    private static float min(float a, float b, float c, float d) {
        return Math.min(Math.min(a, b), Math.min(c, d));
    }

    private static float min(float a, float b, float c, float d, float e) {
        return Math.min(min(a, b, c, d), e);
    }

    private static float box(float x, float y, float cx, float cy, float hx, float hy) {
        float dx = Math.abs(x - cx) - hx, dy = Math.abs(y - cy) - hy;
        float ox = Math.max(dx, 0f), oy = Math.max(dy, 0f);
        return (float) Math.sqrt(ox * ox + oy * oy) + Math.min(Math.max(dx, dy), 0f);
    }

    private static float rbox(float x, float y, float cx, float cy, float hx, float hy, float r) {
        return box(x, y, cx, cy, hx - r, hy - r) - r;
    }

    private static float circle(float x, float y, float cx, float cy, float r) {
        float dx = x - cx, dy = y - cy;
        return (float) Math.sqrt(dx * dx + dy * dy) - r;
    }

    /** A rod of radius r from (ax, ay) to (bx, by), round at its ends. */
    private static float seg(float x, float y, float ax, float ay, float bx, float by, float r) {
        float px = x - ax, py = y - ay, dx = bx - ax, dy = by - ay;
        float h = Math.max(0f, Math.min(1f, (px * dx + py * dy) / (dx * dx + dy * dy)));
        float ex = px - dx * h, ey = py - dy * h;
        return (float) Math.sqrt(ex * ex + ey * ey) - r;
    }

    /** A polygon by its corners (x, y, x, y, ...), any way round. */
    private static float poly(float x, float y, float... p) {
        int n = p.length / 2;
        float d = Float.MAX_VALUE, s = 1f;
        for (int i = 0, j = n - 1; i < n; j = i, i++) {
            float vix = p[i * 2], viy = p[i * 2 + 1], vjx = p[j * 2], vjy = p[j * 2 + 1];
            float ex = vjx - vix, ey = vjy - viy, wx = x - vix, wy = y - viy;
            float h = Math.max(0f, Math.min(1f, (wx * ex + wy * ey) / (ex * ex + ey * ey)));
            float bx = wx - ex * h, by = wy - ey * h;
            d = Math.min(d, bx * bx + by * by);
            boolean c1 = y >= viy, c2 = y < vjy, c3 = ex * wy > ey * wx;
            if ((c1 && c2 && c3) || (!c1 && !c2 && !c3)) s = -s;
        }
        return s * (float) Math.sqrt(d);
    }
}
