package com.shumtugle.skydysh;

/**
 * The player's hammer: a round wooden handle with a wrapped grip and a rounded end; a navy
 * collar where it goes into the head; the head a short barrel across it, capped in navy at both
 * ends, painted with a band of scallops; at one end of the barrel a rounded bulb, and from the
 * bulb a pale ribbon hanging back along the handle. Its own frame: the handle along x (the head
 * at +x), the barrel along z, the bulb at +z. A pose is twelve numbers: the middle, then the
 * turn row by row. The heavy hammer is the same in dark stone, the golden one in gold.
 */
final class Hammer {
    // what each part is drawn as
    static final int ROD_X = 1, ROD_Z = 2, BALL = 3, HEAD_Z = 4, STRIP = 5;
    // the parts: middle, half sizes along the hammer's own x, y, z (meters), shape, a turn about y (radians)
    static final float[][] PARTS = {
            {-0.12f, 0f, 0f, 0.31f, 0.026f, 0.026f, ROD_X, 0f},          // handle
            {-0.31f, 0f, 0f, 0.085f, 0.033f, 0.033f, ROD_X, 0f},         // grip
            {-0.44f, 0f, 0f, 0.05f, 0.043f, 0.043f, BALL, 0f},           // the rounded end
            {0.165f, 0f, 0f, 0.03f, 0.04f, 0.04f, ROD_X, 0f},            // collar
            {0.28f, 0f, 0f, 0.088f, 0.088f, 0.18f, HEAD_Z, 0f},          // head
            {0.28f, 0f, 0.185f, 0.096f, 0.096f, 0.016f, ROD_Z, 0f},      // caps
            {0.28f, 0f, -0.185f, 0.096f, 0.096f, 0.016f, ROD_Z, 0f},
            {0.35f, 0f, 0.22f, 0.09f, 0.08f, 0.06f, BALL, 0f},           // bulb
            {0.27f, 0f, 0.3f, 0.07f, 0.005f, 0.022f, STRIP, 0.35f},      // ribbon
            {0.15f, 0f, 0.325f, 0.065f, 0.005f, 0.02f, STRIP, -0.2f},
            {0.05f, 0f, 0.3f, 0.05f, 0.005f, 0.018f, STRIP, 0.45f},
    };
    // the index of the head among the parts
    static final int HEAD = 4;
    // handle, grip, end, collar, head, caps, bulb, ribbon
    private static final int[] COL = {0xD9A35E, 0xEDD39C, 0xC8803F, 0x26304F, 0xD4462E, 0x26304F, 0x26304F, 0xC93F2C,
            0xEEDAB0, 0xEEDAB0, 0xEEDAB0};
    private static final int[] HEAVY_COL = {0x7A5432, 0xA3A3AA, 0x5A3A22, 0x26304F, 0x50566A, 0x26304F, 0x26304F, 0x434858,
            0xA59F92, 0xA59F92, 0xA59F92};
    private static final int[] GOLD_COL = {0x8A4A26, 0xF4C24A, 0x7A3E22, 0xC88A2E, 0xF2B63A, 0xC88A2E, 0xC88A2E, 0xF0B53C,
            0xF6D06A, 0xF6D06A, 0xF6D06A};
    static final int PLAIN = 0, HEAVY_KIND = 1, GOLDEN = 2;

    private static final float[] T = new float[9], M = new float[9], OVAL = new float[32];

    private Hammer() {
    }

    /** The plain hammer or the heavy one, so big. */
    static void draw(DrawList d, float[] pose, boolean heavy, float scale) {
        draw(d, pose, heavy ? HEAVY_KIND : PLAIN, scale);
    }

    /** The plain hammer, the heavy one or the golden one, so big. */
    static void draw(DrawList d, float[] pose, int kind, float scale) {
        int[] col = colours(kind);
        T[0] = pose[3]; T[1] = pose[4]; T[2] = pose[5];
        T[3] = pose[6]; T[4] = pose[7]; T[5] = pose[8];
        T[6] = pose[9]; T[7] = pose[10]; T[8] = pose[11];
        for (int i = 0; i < PARTS.length; i++) {
            float[] p = PARTS[i];
            float ox = p[0] * scale, oy = p[1] * scale, oz = p[2] * scale;
            float cx = pose[0] + T[0] * ox + T[1] * oy + T[2] * oz;
            float cy = pose[1] + T[3] * ox + T[4] * oy + T[5] * oz;
            float cz = pose[2] + T[6] * ox + T[7] * oy + T[8] * oz;
            float hx = p[3] * scale, hy = p[4] * scale, hz = p[5] * scale;
            int shape = (int) p[6];
            switch (shape) {
                case ROD_X:
                    d.ellipsoid(cx, cy, cz, hx, hy, hz, T, col[i], DrawList.ROD);
                    break;
                case BALL:
                    d.ellipsoid(cx, cy, cz, hx, hy, hz, T, col[i], DrawList.RUBBER);
                    break;
                case ROD_Z:
                case HEAD_Z:
                    // the rod's own axis laid along the hammer's z
                    M[0] = T[2]; M[1] = T[1]; M[2] = -T[0];
                    M[3] = T[5]; M[4] = T[4]; M[5] = -T[3];
                    M[6] = T[8]; M[7] = T[7]; M[8] = -T[6];
                    d.ellipsoid(cx, cy, cz, hz, hy, hx, M, col[i], shape == HEAD_Z && kind == PLAIN ? DrawList.HEAD : DrawList.ROD);
                    break;
                default: {
                    // a strip of cloth, turned about the hammer's y
                    float c = (float) Math.cos(p[7]), s = (float) Math.sin(p[7]);
                    M[0] = T[0] * c - T[2] * s; M[1] = T[1]; M[2] = T[0] * s + T[2] * c;
                    M[3] = T[3] * c - T[5] * s; M[4] = T[4]; M[5] = T[3] * s + T[5] * c;
                    M[6] = T[6] * c - T[8] * s; M[7] = T[7]; M[8] = T[6] * s + T[8] * c;
                    d.box(cx, cy, cz, hx, hy, hz, M, col[i], DrawList.PLAIN);
                }
            }
        }
    }

    /** The colours of a kind's parts, as 0xRRGGBB, in the order of the parts. */
    static int[] colours(int kind) {
        return kind == HEAVY_KIND ? HEAVY_COL : kind == GOLDEN ? GOLD_COL : COL;
    }

    /** The colour of a kind's head, the one the eye takes it by. */
    static int head(int kind) {
        return colours(kind)[HEAD];
    }

    /**
     * A hammer as a sign, drawn flat on a fine layer, in profile: the handle with its rounded
     * end, the barrel across it with its caps, the bulb, the ribbon hanging from it; the head up
     * and to the right, so long overall; in its own colours (one below 0; -2 with an ink edge
     * round it), or all in one colour given (a sign on a key, a shadow).
     */
    static void icon(Ink g, float cx, float cy, float length, int kind, int one, float cover) {
        iconAt(g, cx, cy, length, kind, one, cover, 58f);
    }

    /** The same, its handle turned so many degrees up from pointing right. */
    static void iconAt(Ink g, float cx, float cy, float length, int kind, int one, float cover, float angle) {
        float s = length / 0.98f;
        float a = (float) Math.toRadians(angle);
        float dx = (float) Math.cos(a), dy = -(float) Math.sin(a);
        flat(g, cx, cy, s, dx, dy, kind, one, cover);
    }

    /** The same, placed by the far end of its striking cap (the one away from the bulb). */
    static void strikeAt(Ink g, float sx, float sy, float length, int kind, int one, float cover, float angle) {
        float s = length / 0.98f;
        float a = (float) Math.toRadians(angle);
        float dx = (float) Math.cos(a), dy = -(float) Math.sin(a), zx = -dy, zy = dx;
        float hx = 0.28f, hz = -0.2f;
        flat(g, sx - (hx * dx + hz * zx) * s, sy - (hx * dy + hz * zy) * s, s, dx, dy, kind, one, cover);
    }

    private static final int[] FLAT_ORDER = {0, 1, 3, 8, 9, 10, 4, 5, 6, 7, 2};

    private static void flat(Ink g, float ox, float oy, float s, float dx, float dy, int kind, int one, float cover) {
        int[] col = colours(kind);
        float zx = -dy, zy = dx;                                       // across the handle, along the barrel
        boolean inked = one == -2;
        for (int pass = inked ? 0 : 1; pass < 2; pass++) {
            // the ink edge first, every part grown by it, then the parts themselves
            float grow = pass == 0 ? Math.max(2f, s * 0.022f) : 0f;
            for (int i : FLAT_ORDER) {
                float[] p = PARTS[i];
                float mx = ox + (p[0] * dx + p[2] * zx) * s, my = oy + (p[0] * dy + p[2] * zy) * s;
                float hx = p[3] * s + grow, hz = p[5] * s + grow;
                int shape = (int) p[6];
                if (shape == STRIP) {
                    float c = (float) Math.cos(p[7]), sn = (float) Math.sin(p[7]);
                    float ax = dx * c + zx * sn, ay = dy * c + zy * sn, bx = -dx * sn + zx * c, by = -dy * sn + zy * c;
                    g.shape(mx - ax * hx - bx * hz, my - ay * hx - by * hz, mx + ax * hx - bx * hz, my + ay * hx - by * hz,
                            mx + ax * hx + bx * hz, my + ay * hx + by * hz, mx - ax * hx + bx * hz, my - ay * hx + by * hz);
                } else if (shape == BALL) {
                    // an oval, as a shape of many sides, its long way along the handle
                    for (int k = 0; k < 16; k++) {
                        double an = k * Math.PI / 8;
                        float u = hx * (float) Math.cos(an), v = hz * (float) Math.sin(an);
                        OVAL[k * 2] = mx + dx * u + zx * v;
                        OVAL[k * 2 + 1] = my + dy * u + zy * v;
                    }
                    g.shape(OVAL);
                } else {
                    g.shape(mx - dx * hx - zx * hz, my - dy * hx - zy * hz, mx + dx * hx - zx * hz, my + dy * hx - zy * hz,
                            mx + dx * hx + zx * hz, my + dy * hx + zy * hz, mx - dx * hx + zx * hz, my - dy * hx + zy * hz);
                }
                if (pass == 0) {
                    g.polyFill(0xFF232A4A, 0xFF232A4A, 1f, cover);
                    continue;
                }
                int c = 0xFF000000 | (one >= 0 ? one : col[i]);
                g.polyFill(c, c, 1f, cover);
                // the barrel's paint: a pale streak along it, the dark band of scallops nearer the handle
                if (one < 0 && i == HEAD) {
                    float w = hx * 0.16f;
                    g.shape(mx + dx * (hx * 0.5f) - zx * hz * 0.92f, my + dy * (hx * 0.5f) - zy * hz * 0.92f,
                            mx + dx * (hx * 0.5f + w) - zx * hz * 0.92f, my + dy * (hx * 0.5f + w) - zy * hz * 0.92f,
                            mx + dx * (hx * 0.5f + w) + zx * hz * 0.92f, my + dy * (hx * 0.5f + w) + zy * hz * 0.92f,
                            mx + dx * (hx * 0.5f) + zx * hz * 0.92f, my + dy * (hx * 0.5f) + zy * hz * 0.92f);
                    g.polyFill(0xFFF3E3C4, 0xFFF3E3C4, 1f, cover * 0.85f);
                    if (kind == PLAIN) {
                        for (int k = 0; k < 5; k++) {
                            float zc = -hz + hz * 2f * (k + 0.5f) / 5f;
                            float r = hz / 5f;
                            for (int q = 0; q < 16; q++) {
                                double an = q * Math.PI / 8;
                                OVAL[q * 2] = mx - dx * (hx * 0.45f) + dx * r * 0.9f * (float) Math.cos(an) + zx * (zc + r * (float) Math.sin(an));
                                OVAL[q * 2 + 1] = my - dy * (hx * 0.45f) + dy * r * 0.9f * (float) Math.cos(an) + zy * (zc + r * (float) Math.sin(an));
                            }
                            g.shape(OVAL);
                            g.polyFill(0xFF26304F, 0xFF26304F, 1f, cover);
                        }
                        g.shape(mx - dx * hx - zx * hz, my - dy * hx - zy * hz, mx - dx * (hx * 0.45f) - zx * hz, my - dy * (hx * 0.45f) - zy * hz,
                                mx - dx * (hx * 0.45f) + zx * hz, my - dy * (hx * 0.45f) + zy * hz, mx - dx * hx + zx * hz, my - dy * hx + zy * hz);
                        g.polyFill(0xFF26304F, 0xFF26304F, 1f, cover);
                    }
                }
            }
        }
    }


    /**
     * Held near the eye: at a point given in the eye's own space (right, up, ahead), its
     * handle along one such direction, the barrel across another.
     */
    static void held(DrawList d, float vx, float vy, float vz, float ux, float uy, float uz, float wx, float wy, float wz,
                     float[] pose) {
        float[] r = d.right, up = d.up, a = d.ahead;
        for (int k = 0; k < 3; k++) pose[k] = d.eye[k] + r[k] * vx + up[k] * vy + a[k] * vz;
        float ax = r[0] * ux + up[0] * uy + a[0] * uz, ay = r[1] * ux + up[1] * uy + a[1] * uz, az = r[2] * ux + up[2] * uy + a[2] * uz;
        float l = (float) Math.sqrt(ax * ax + ay * ay + az * az);
        ax /= l;
        ay /= l;
        az /= l;
        float zx = r[0] * wx + up[0] * wy + a[0] * wz, zy = r[1] * wx + up[1] * wy + a[1] * wz, zz = r[2] * wx + up[2] * wy + a[2] * wz;
        float dd = zx * ax + zy * ay + zz * az;
        zx -= dd * ax;
        zy -= dd * ay;
        zz -= dd * az;
        l = (float) Math.sqrt(zx * zx + zy * zy + zz * zz);
        zx /= l;
        zy /= l;
        zz /= l;
        pose[3] = ax;
        pose[6] = ay;
        pose[9] = az;
        pose[4] = zy * az - zz * ay;
        pose[7] = zz * ax - zx * az;
        pose[10] = zx * ay - zy * ax;
        pose[5] = zx;
        pose[8] = zy;
        pose[11] = zz;
    }
}
