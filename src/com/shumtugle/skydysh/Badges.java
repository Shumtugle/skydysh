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
    static final int LIFT = 0, DOWN = 1, UP = 2, LEFT = 3, RIGHT = 4, MALLET = 5, WHEEL = 6, LIST = 7,
            THROW = 8, PICK_UP = 9, MAP = 10, HELP = 11, PAUSE = 12, PLAY = 13, HOME = 14, LADDER = 15, EXIT = 16, ENTER = 17;
    // the stick: its round pane with nothing on it, and the knob in it, a white disc
    static final int BLANK = 18, KNOB = 19;
    // the ways to play, one finger or two thumbs; the pedals at the wheel, the throttle and the brake
    static final int ONE_FINGER = 20, TWO_THUMBS = 21, GAS = 22, BRAKE = 23;
    // the wheel at the wheel: a steering wheel of the same glass, no pane round it, a mark at its top
    static final int STEER = 24;
    // once more: an arrow round nearly a whole turn, back to where it began
    static final int AGAIN = 25;
    // the compass's needle, on its own (no pane: it turns over a blank one), coral to the north
    static final int NEEDLE = 26;
    // a screwdriver: a fat handle low on the left, the shaft up to the right, its blade at the end
    static final int SCREWDRIVER = 27;
    // a coil of rope, its end hanging; what a pickup's bed holds: a box of things, as a toolbox
    static final int ROPE = 28, BED = 29;
    // the game kept: a disc in its square case, the corner cut, the shutter and the label
    static final int SAVE = 30;
    // a torch: its body low on the left, the wider head up to the right, three rays out of it
    static final int TORCH = 31;
    // a charge: three sticks bound with a band, the fuse curling up out of them, a spark at its end
    static final int CHARGE = 32;
    // a helicopter from the side: the body, its window cut out, the tail boom and its fin, the
    // blade over it on its mast, the skids under it
    static final int COPTER = 33;
    static final int COUNT = 34, CELLS = 6, SIZE = 160;
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
        if (k == STEER) return steer(u, v, px);
        if (k == NEEDLE) return needle(u, v, px);
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

    /**
     * The steering wheel: its rim, three spokes (left, right, down) and the hub, all of the dark
     * glass with the lit edge and the glow the buttons have; a white mark at the top of the rim, so
     * that it is seen to turn.
     */
    /** The needle: a slim diamond up and down, its north half coral, its south half white, a pin in its middle; a glow round it. */
    private static int needle(float u, float v, float px) {
        float north = poly(u, v, 0f, 0.6f, -0.15f, 0f, 0.15f, 0f), south = poly(u, v, 0f, -0.6f, 0.15f, 0f, -0.15f, 0f);
        float shape = Math.min(north, south);
        if (shape > -0.02f) over(v > 0f ? 0xFF6F61 : WHITE, 0.4f * (float) Math.exp(-Math.max(0f, shape) / 0.05f));
        over(0xFF6F61, cover(north, px));
        over(WHITE, cover(south, px));
        over(GLASS_TOP, cover(circle(u, v, 0f, 0f, 0.07f), px));
        int a = Math.round(Math.min(1f, ca) * 255f);
        return a << 24 | Math.round(Math.min(1f, cr) * 255f) << 16 | Math.round(Math.min(1f, cg) * 255f) << 8
                | Math.round(Math.min(1f, cb) * 255f);
    }

    private static int steer(float u, float v, float px) {
        float r = (float) Math.sqrt(u * u + v * v);
        float t = r > 1e-4f ? ((u - v) / r * 0.7071f + 1f) * 0.5f : 0.5f;
        int rim = mix(TURQUOISE, PINK, t * 1.15f - 0.08f);
        // thin lines: the wheel is drawn big, so its rim and spokes are half what a sign's would be
        float ring = Math.abs(r - 0.8f) - 0.055f;
        float spokes = min(seg(u, v, 0f, 0f, -0.78f, 0.02f, 0.038f), seg(u, v, 0f, 0f, 0.78f, 0.02f, 0.038f), seg(u, v, 0f, 0f, 0f, -0.78f, 0.042f));
        float shape = min(ring, spokes, circle(u, v, 0f, 0f, 0.11f));
        // the glow round it
        if (shape > -0.01f) over(rim, 0.45f * (float) Math.exp(-Math.max(0f, shape) / 0.035f));
        float inside = cover(shape, px);
        if (inside > 0f) {
            over(mix(GLASS_TOP, GLASS_LOW, (0.8f - v) / 1.6f), 0.72f * inside);
            over(rim, 0.45f * inside * (float) Math.exp(-Math.max(0f, -shape) / 0.025f));
            // a sheen along the top of the rim
            if (v > 0.3f && ring < 0f) over(0xFFFFFF, 0.12f * inside * (v - 0.3f));
        }
        // the lit edge
        over(mix(rim, 0xFFFFFF, 0.3f), cover(Math.abs(shape) - 0.012f, px));
        // the mark at the top
        over(WHITE, cover(rbox(u, v, 0f, 0.8f, 0.035f, 0.07f, 0.015f), px));
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
            case ROPE: {
                // three turns of a coil, one in another, and its end hanging down on the right
                float d0 = (float) Math.sqrt(x * x + (y - 0.08f) * (y - 0.08f));
                float coil = Math.min(Math.abs(d0 - 0.56f), Math.min(Math.abs(d0 - 0.38f), Math.abs(d0 - 0.2f))) - 0.07f;
                float end = box(x, y, 0.56f, -0.55f, 0.07f, 0.3f);
                return Math.min(coil, end);
            }
            case SAVE: {
                float body = rbox(x, y, 0f, 0f, 0.78f, 0.8f, 0.1f);
                body = Math.max(body, -poly(x, y, 0.42f, 0.96f, 0.96f, 0.96f, 0.96f, 0.42f));
                float shutter = box(x, y, -0.06f, 0.54f, 0.36f, 0.22f);
                float slot = box(x, y, 0.12f, 0.54f, 0.09f, 0.15f);
                float label = box(x, y, 0f, -0.36f, 0.54f, 0.34f);
                return Math.min(Math.max(body, -Math.min(shutter, label)), slot);
            }
            case BED: {
                // a box of things, as a toolbox is: the box, its lid's seam across, the latch over the
                // seam, the handle on top
                float body = rbox(x, y, 0f, -0.2f, 0.8f, 0.46f, 0.08f);
                body = Math.max(body, -box(x, y, 0f, 0.06f, 0.82f, 0.035f));
                float latch = rbox(x, y, 0f, 0.04f, 0.12f, 0.13f, 0.03f);
                float handle = Math.max(rbox(x, y, 0f, 0.42f, 0.34f, 0.2f, 0.08f), -rbox(x, y, 0f, 0.4f, 0.2f, 0.09f, 0.04f));
                return min(body, latch, handle);
            }
            case COPTER: {
                float body = rbox(x, y, -0.18f, -0.06f, 0.4f, 0.27f, 0.2f);
                body = Math.max(body, -rbox(x, y, -0.36f, 0.02f, 0.13f, 0.1f, 0.04f));
                float tail = Math.min(seg(x, y, 0.15f, 0.02f, 0.8f, 0.1f, 0.07f), seg(x, y, 0.78f, 0.04f, 0.84f, 0.34f, 0.06f));
                float mast = seg(x, y, -0.18f, 0.2f, -0.18f, 0.42f, 0.05f);
                float blade = rbox(x, y, -0.18f, 0.46f, 0.72f, 0.045f, 0.03f);
                float skids = min(rbox(x, y, -0.18f, -0.56f, 0.5f, 0.045f, 0.03f), seg(x, y, -0.4f, -0.3f, -0.42f, -0.54f, 0.04f),
                        seg(x, y, 0.04f, -0.3f, 0.06f, -0.54f, 0.04f));
                return min(min(body, tail, mast), blade, skids);
            }
            case CHARGE: {
                // three sticks side by side, a band across them cut out; the fuse up to the right; its spark
                float sticks = min(rbox(x, y, -0.3f, -0.12f, 0.12f, 0.5f, 0.06f), rbox(x, y, 0f, -0.12f, 0.12f, 0.5f, 0.06f),
                        rbox(x, y, 0.3f, -0.12f, 0.12f, 0.5f, 0.06f));
                sticks = Math.max(sticks, -box(x, y, 0f, 0.05f, 0.5f, 0.04f));
                float fuse = Math.min(seg(x, y, 0f, 0.38f, 0.18f, 0.6f, 0.04f), seg(x, y, 0.18f, 0.6f, 0.42f, 0.66f, 0.04f));
                float spark = circle(x, y, 0.55f, 0.7f, 0.12f);
                return min(sticks, fuse, spark);
            }
            case TORCH: {
                // along the diagonal: the body, ridged for the grip; the head wider; the light out of its glass
                float c = 0.7071f, lx = (x + y) * c, ly = (y - x) * c;
                float body = rbox(lx, ly, -0.34f, 0f, 0.34f, 0.15f, 0.06f);
                float ridges = Math.min(box(lx, ly, -0.42f, 0.15f, 0.2f, 0.035f), box(lx, ly, -0.42f, -0.15f, 0.2f, 0.035f));
                body = Math.max(body, -ridges);
                float head = rbox(lx, ly, 0.12f, 0f, 0.14f, 0.26f, 0.04f);
                // the rays, laid along the diagonal (lx, ly) and turned back into the cell
                float rays = min(ray(x, y, 0.36f, 0f, 0.76f, 0f), ray(x, y, 0.34f, 0.2f, 0.64f, 0.44f), ray(x, y, 0.34f, -0.2f, 0.64f, -0.44f));
                return min(body, head, rays);
            }
            case SCREWDRIVER: {
                // the handle, rounded, ridged; the shaft; the flat blade at its tip
                float c = 0.7071f, lx = (x + y) * c, ly = (y - x) * c;
                float handle = rbox(lx, ly, -0.38f, 0f, 0.34f, 0.17f, 0.12f);
                float grooves = Math.min(box(lx, ly, -0.38f, 0.07f, 0.26f, 0.025f), box(lx, ly, -0.38f, -0.07f, 0.26f, 0.025f));
                handle = Math.max(handle, -grooves);
                float shaft = box(lx, ly, 0.27f, 0f, 0.33f, 0.045f);
                float blade = box(lx, ly, 0.66f, 0f, 0.07f, 0.09f);
                return min(handle, shaft, blade);
            }
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
            case LIST: {
                // a list: three rows, a dot and a line each
                float d = 1e9f;
                for (int r = -1; r <= 1; r++) {
                    float cy = -r * 0.52f;
                    d = min(d, circle(x, y, -0.62f, cy, 0.13f), rbox(x, y, 0.2f, cy, 0.56f, 0.1f, 0.08f));
                }
                return d;
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
            case AGAIN: {
                // a ring open at the top right, its end an arrowhead pointing on round
                float rr = 0.56f, d = Math.abs((float) Math.sqrt(x * x + y * y) - rr) - 0.11f;
                float a = (float) Math.atan2(y, x);
                if (a > 0.35f && a < 1.25f) d = 1e9f;
                float hx = rr * (float) Math.cos(0.35f), hy = rr * (float) Math.sin(0.35f);
                float head = poly(x, y, hx - 0.3f, hy - 0.02f, hx + 0.3f, hy - 0.02f, hx, hy + 0.36f);
                return Math.min(d, head);
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
    /** A torch's ray from (la, wa) to (lb, wb) along and across the cell's diagonal. */
    private static float ray(float x, float y, float la, float wa, float lb, float wb) {
        float c = 0.7071f;
        return seg(x, y, c * (la - wa), c * (la + wa), c * (lb - wb), c * (lb + wb), 0.05f);
    }

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
