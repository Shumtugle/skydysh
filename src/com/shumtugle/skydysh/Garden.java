package com.shumtugle.skydysh;

import java.util.Random;

/**
 * The town's green things and the small furniture of its streets and squares, built of boxes:
 * broad-leaved trees, bushes, clipped hedges, flower beds and pots, ivy up a wall, benches, a
 * low iron fence, lamp posts of the old kind, and a fountain of two basins with its water
 * thrown up. Each is put down where it is told; what cannot be walked through is the caller's
 * to keep (see City).
 *
 * Space as in DrawList: x to the right, y up.
 */
final class Garden {
    static final int[] LEAF = {0x3E6A2C, 0x4B7A31, 0x587F35, 0x3A5F2A, 0x62893A};
    static final int HEDGE = 0x35592A, EARTH = 0x6E5440, IRON = 0x2B2F3A, TERRACOTTA = 0xB8643E, STONE = 0xE2D4BA;
    static final int BARK = 0x6B5A48, SLAT = 0xB07A48, WATER = 0x2D6990, SPRAY = 0xD8F0F2;

    private static final float[] T = new float[9];
    // the frame of the thing being put down: where, and turned how about the upright
    private static float ox, oy, oz, oc, os;

    private Garden() {
    }

    /** A turn about the upright by a, into T (rows; columns its own x, y, z). */
    private static float[] yaw(float a) {
        float c = (float) Math.cos(a), s = (float) Math.sin(a);
        T[0] = c; T[1] = 0f; T[2] = s;
        T[3] = 0f; T[4] = 1f; T[5] = 0f;
        T[6] = -s; T[7] = 0f; T[8] = c;
        return T;
    }

    /** A turn about the upright by a, then tipped about the level by b and c, into T: a mass of leaves set askew. */
    private static float[] askew(float a, float b, float c) {
        float ca = (float) Math.cos(a), sa = (float) Math.sin(a), cb = (float) Math.cos(b), sb = (float) Math.sin(b);
        float cc = (float) Math.cos(c), sc = (float) Math.sin(c);
        // Ry(a) Rx(b) Rz(c), as rows
        float m00 = cc, m01 = -sc, m02 = 0f;
        float m10 = cb * sc, m11 = cb * cc, m12 = -sb;
        float m20 = sb * sc, m21 = sb * cc, m22 = cb;
        T[0] = ca * m00 + sa * m20; T[1] = ca * m01 + sa * m21; T[2] = ca * m02 + sa * m22;
        T[3] = m10; T[4] = m11; T[5] = m12;
        T[6] = -sa * m00 + ca * m20; T[7] = -sa * m01 + ca * m21; T[8] = -sa * m02 + ca * m22;
        return T;
    }

    private static float[] askew(Random rnd) {
        return askew(rnd.nextFloat() * 6.283f, (rnd.nextFloat() - 0.5f) * 1.1f, (rnd.nextFloat() - 0.5f) * 1.1f);
    }

    private static void frame(float x, float y, float z, float a) {
        ox = x;
        oy = y;
        oz = z;
        oc = (float) Math.cos(a);
        os = (float) Math.sin(a);
    }

    /** A box in the frame set by frame(): its middle along the frame's own x, y, z. */
    private static void part(DrawList d, float lx, float ly, float lz, float hx, float hy, float hz, int color, float material) {
        T[0] = oc; T[1] = 0f; T[2] = os;
        T[3] = 0f; T[4] = 1f; T[5] = 0f;
        T[6] = -os; T[7] = 0f; T[8] = oc;
        d.box(ox + oc * lx + os * lz, oy + ly, oz - os * lx + oc * lz, hx, hy, hz, T, color, material);
    }

    private static float leaves(int seed) {
        return DrawList.material(DrawList.LEAVES, seed);
    }

    /** A colour of flowers, as a seed for LEAVES: never none. */
    static int bloom(Random rnd) {
        return 1 + rnd.nextInt(240);
    }

    /** A broad-leaved tree standing at (x, y, z), so tall: a trunk forking under a crown of a few masses of leaves. */
    static void tree(DrawList d, Random rnd, float x, float y, float z, float height) {
        float trunk = height * 0.42f;
        d.box(x, y + trunk * 0.5f, z, 0.17f, trunk * 0.5f, 0.17f, yaw(rnd.nextFloat()), BARK,
                DrawList.material(DrawList.TRUNK, rnd.nextInt(200)));
        // two limbs up into the crown
        for (int k = 0; k < 2; k++) {
            float a = rnd.nextFloat() * 6.283f;
            float lx = (float) Math.cos(a) * 0.35f, lz = (float) Math.sin(a) * 0.35f;
            float[] t = yaw(a);
            // leaning out: the limb's own y tipped toward its own x
            float tip = 0.45f, ct = (float) Math.cos(tip), st = (float) Math.sin(tip);
            float r0 = t[0], r3 = t[3], r6 = t[6];
            t[0] = r0 * ct + t[1] * st; t[1] = -r0 * st + t[1] * ct;
            t[3] = r3 * ct + t[4] * st; t[4] = -r3 * st + t[4] * ct;
            t[6] = r6 * ct + t[7] * st; t[7] = -r6 * st + t[7] * ct;
            d.box(x + lx, y + trunk + 0.55f, z + lz, 0.1f, 0.65f, 0.1f, t, BARK, DrawList.material(DrawList.TRUNK, 40 + k * 70));
        }
        float cy = y + height * 0.66f, r = height * 0.27f;
        int g = LEAF[rnd.nextInt(LEAF.length)];
        // the crown: a heart of leaves, and masses askew round it, so that no edge of it runs straight
        d.box(x, cy, z, r * 0.78f, r * 0.62f, r * 0.78f, askew(rnd), g, leaves(0));
        int n = 7 + rnd.nextInt(3);
        for (int k = 0; k < n; k++) {
            float a = k * 6.283f / n + rnd.nextFloat() * 0.5f, up = (rnd.nextFloat() - 0.35f) * 1.2f;
            float out = r * (0.62f + rnd.nextFloat() * 0.2f) * (float) Math.cos(up * 0.6f);
            float m = r * (0.42f + rnd.nextFloat() * 0.18f);
            int c = Street.mix(g, LEAF[rnd.nextInt(LEAF.length)], 0.5f);
            c = Street.scale(c, up > 0.2f ? 1.08f : up < -0.2f ? 0.9f : 1f);
            d.box(x + (float) Math.cos(a) * out, cy + up * r * 0.55f, z + (float) Math.sin(a) * out, m, m * 0.8f, m,
                    askew(rnd), c, leaves(0));
        }
        d.box(x, cy + r * 0.6f, z, r * 0.5f, r * 0.4f, r * 0.5f, askew(rnd), Street.scale(g, 1.1f), leaves(0));
    }

    /** A bush: two or three masses of leaves, so big, now and then in flower. */
    static void bush(DrawList d, Random rnd, float x, float y, float z, float r) {
        int g = LEAF[rnd.nextInt(LEAF.length)];
        int flowers = rnd.nextInt(3) == 0 ? bloom(rnd) : 0;
        int n = 2 + rnd.nextInt(2);
        for (int k = 0; k < n; k++) {
            float a = rnd.nextFloat() * 6.283f, s = r * (0.65f + rnd.nextFloat() * 0.35f);
            d.box(x + (float) Math.cos(a) * r * 0.35f, y + s * 0.7f, z + (float) Math.sin(a) * r * 0.35f, s, s * 0.7f, s,
                    askew(rnd.nextFloat() * 6.283f, (rnd.nextFloat() - 0.5f) * 0.5f, (rnd.nextFloat() - 0.5f) * 0.5f),
                    Street.mix(g, HEDGE, 0.3f), leaves(flowers));
        }
    }

    /** A clipped hedge from (x0, z0) to (x1, z1), so high: one dense block, level on top. */
    static void hedge(DrawList d, float x0, float z0, float x1, float z1, float y, float high) {
        d.box((x0 + x1) * 0.5f, y + high * 0.5f, (z0 + z1) * 0.5f, Math.abs(x1 - x0) * 0.5f, high * 0.5f, Math.abs(z1 - z0) * 0.5f,
                null, HEDGE, leaves(0));
    }

    /** A bed of flowers in a low stone kerb, from (x0, z0) to (x1, z1). */
    static void bed(DrawList d, Random rnd, float x0, float z0, float x1, float z1, float y) {
        float mx = (x0 + x1) * 0.5f, mz = (z0 + z1) * 0.5f, hx = Math.abs(x1 - x0) * 0.5f, hz = Math.abs(z1 - z0) * 0.5f;
        d.box(mx, y + 0.12f, mz, hx, 0.12f, hz, null, STONE, DrawList.STONE);
        d.box(mx, y + 0.2f, mz, hx - 0.12f, 0.1f, hz - 0.12f, null, EARTH, DrawList.GRAIN);
        d.box(mx, y + 0.38f, mz, hx - 0.18f, 0.16f, hz - 0.18f, null, LEAF[rnd.nextInt(LEAF.length)], leaves(bloom(rnd)));
    }

    /** A pot of terracotta with a plant in it, in flower, at (x, y, z), so wide. */
    static void pot(DrawList d, Random rnd, float x, float y, float z, float r) {
        float[] t = yaw(rnd.nextFloat() * 1.6f);
        d.box(x, y + r * 0.8f, z, r * 0.8f, r * 0.8f, r * 0.8f, t, TERRACOTTA, DrawList.GRAIN);
        d.box(x, y + r * 1.6f, z, r, r * 0.12f, r, t, Street.scale(TERRACOTTA, 1.08f), DrawList.GRAIN);
        float leaf = r * (0.9f + rnd.nextFloat() * 0.4f);
        d.box(x, y + r * 1.7f + leaf * 0.55f, z, leaf, leaf * 0.6f, leaf, yaw(rnd.nextFloat() * 1.6f),
                LEAF[rnd.nextInt(LEAF.length)], leaves(rnd.nextInt(4) == 0 ? 0 : bloom(rnd)));
    }

    /**
     * Ivy up a wall: a sheet of leaves on the wall at (x, z) whose face looks along (nx, nz), so
     * wide, from y0 up to about y1, its top ragged with tongues climbing higher than the rest.
     */
    static void ivy(DrawList d, Random rnd, float x, float z, float nx, float nz, float half, float y0, float y1) {
        float ax = -nz, az = nx, off = 0.07f;
        float h = (y1 - y0) * 0.75f;
        boolean alongX = Math.abs(az) < 0.5f;
        int g = Street.mix(LEAF[rnd.nextInt(LEAF.length)], HEDGE, 0.4f);
        sheet(d, x + nx * off, y0 + h * 0.5f, z + nz * off, half, h * 0.5f, alongX, g);
        int n = 3 + rnd.nextInt(3);
        for (int k = 0; k < n; k++) {
            float u = (k + 0.5f) / n * 2f - 1f + (rnd.nextFloat() - 0.5f) * 0.3f;
            float w = half / n * (0.6f + rnd.nextFloat() * 0.5f);
            float up = (y1 - y0) * (0.08f + rnd.nextFloat() * 0.3f);
            float tx = x + ax * u * half + nx * (off + 0.02f), tz = z + az * u * half + nz * (off + 0.02f);
            sheet(d, tx, y0 + h + up * 0.5f - 0.1f, tz, w, up * 0.5f + 0.1f, alongX, Street.scale(g, 0.95f + rnd.nextFloat() * 0.15f));
        }
    }

    private static void sheet(DrawList d, float x, float y, float z, float half, float hy, boolean alongX, int color) {
        d.box(x, y, z, alongX ? half : 0.07f, hy, alongX ? 0.07f : half, null, color, leaves(0));
    }

    /** A bench at (x, y, z) for sitting facing along yaw: slats of wood on iron legs, a back of slats. */
    static void bench(DrawList d, float x, float y, float z, float yaw) {
        frame(x, y, z, yaw);
        float wood = DrawList.WOOD;
        part(d, 0f, 0.44f, 0.02f, 0.9f, 0.035f, 0.21f, SLAT, wood);
        part(d, 0f, 0.62f, -0.21f, 0.9f, 0.07f, 0.025f, SLAT, wood);
        part(d, 0f, 0.8f, -0.23f, 0.9f, 0.06f, 0.025f, SLAT, wood);
        for (int e = -1; e <= 1; e += 2) {
            part(d, e * 0.78f, 0.22f, 0.02f, 0.035f, 0.22f, 0.2f, IRON, DrawList.PLAIN);
            part(d, e * 0.78f, 0.62f, -0.2f, 0.035f, 0.24f, 0.03f, IRON, DrawList.PLAIN);
            part(d, e * 0.78f, 0.55f, 0.06f, 0.035f, 0.035f, 0.18f, IRON, DrawList.PLAIN);
        }
    }

    /** A low iron fence from (x0, z0) to (x1, z1), straight along x or z: bars, a rail on top, a post now and then. */
    static void fence(DrawList d, float x0, float z0, float x1, float z1, float y) {
        boolean alongX = Math.abs(x1 - x0) > Math.abs(z1 - z0);
        float mx = (x0 + x1) * 0.5f, mz = (z0 + z1) * 0.5f, half = (alongX ? Math.abs(x1 - x0) : Math.abs(z1 - z0)) * 0.5f;
        d.box(mx, y + 0.42f, mz, alongX ? half : 0.02f, 0.38f, alongX ? 0.02f : half, null, IRON, DrawList.RAIL);
        d.box(mx, y + 0.82f, mz, alongX ? half : 0.035f, 0.025f, alongX ? 0.035f : half, null, IRON, DrawList.PLAIN);
        for (float t = -half; t <= half + 0.01f; t += 2.5f) {
            float px = alongX ? mx + t : mx, pz = alongX ? mz : mz + t;
            d.box(px, y + 0.45f, pz, 0.05f, 0.45f, 0.05f, null, IRON, DrawList.PLAIN);
            d.box(px, y + 0.93f, pz, 0.07f, 0.04f, 0.07f, null, IRON, DrawList.PLAIN);
        }
    }

    /** A lamp post of the old kind at (x, y, z): a fluted iron post, two arms, two lanterns lit. */
    static void lamp(DrawList d, float x, float y, float z, float yaw) {
        frame(x, y, z, yaw);
        part(d, 0f, 0.3f, 0f, 0.2f, 0.3f, 0.2f, IRON, DrawList.PLAIN);
        part(d, 0f, 2.2f, 0f, 0.08f, 1.9f, 0.08f, IRON, DrawList.PLAIN);
        part(d, 0f, 3.85f, 0f, 0.7f, 0.04f, 0.04f, IRON, DrawList.PLAIN);
        for (int e = -1; e <= 1; e += 2) {
            part(d, e * 0.7f, 3.62f, 0f, 0.13f, 0.18f, 0.13f, 0xF3E2B4, DrawList.GLOW);
            part(d, e * 0.7f, 3.84f, 0f, 0.17f, 0.04f, 0.17f, IRON, DrawList.PLAIN);
        }
        part(d, 0f, 4.12f, 0f, 0.05f, 0.18f, 0.05f, IRON, DrawList.PLAIN);
    }

    /**
     * A fountain at (x, y, z) of radius r: a wide eight-sided basin of pale stone with water in
     * it, a pillar up its middle carrying a smaller bowl, and the water thrown up out of the bowl
     * and falling back into the basin in a ring. How high its rim is.
     */
    static float fountain(DrawList d, float x, float y, float z, float r) {
        float rim = 0.55f;
        for (int k = 0; k < 2; k++) {
            float[] t = yaw(k * (float) (Math.PI / 4));
            d.box(x, y + rim * 0.5f, z, r, rim * 0.5f, r * 0.414f + 0.25f, t, STONE, DrawList.STONE);
            d.box(x, y + rim * 0.5f, z, r * 0.414f + 0.25f, rim * 0.5f, r, t, STONE, DrawList.STONE);
            d.box(x, y + rim - 0.05f, z, r - 0.3f, 0.06f, (r - 0.3f) * 0.414f + 0.15f, t, WATER, DrawList.SEA);
            d.box(x, y + rim - 0.05f, z, (r - 0.3f) * 0.414f + 0.15f, 0.06f, r - 0.3f, t, WATER, DrawList.SEA);
        }
        float bowl = y + 1.9f;
        d.box(x, y + 1.2f, z, 0.32f, 0.75f, 0.32f, yaw(0.4f), STONE, DrawList.STONE);
        for (int k = 0; k < 2; k++) {
            float[] t = yaw(0.4f + k * (float) (Math.PI / 4));
            d.box(x, bowl, z, r * 0.36f, 0.16f, r * 0.15f, t, STONE, DrawList.STONE);
            d.box(x, bowl, z, r * 0.15f, 0.16f, r * 0.36f, t, STONE, DrawList.STONE);
        }
        d.box(x, bowl + 0.17f, z, r * 0.3f, 0.02f, r * 0.3f, yaw(0.4f), WATER, DrawList.SEA);
        // the water: a jet up from the bowl, and a ring of it falling into the basin
        d.box(x, bowl + 0.9f, z, 0.08f, 0.75f, 0.08f, null, SPRAY, DrawList.GLOW);
        d.box(x, bowl + 1.68f, z, 0.2f, 0.08f, 0.2f, yaw(0.8f), SPRAY, DrawList.GLOW);
        for (int k = 0; k < 8; k++) {
            float a = k * (float) (Math.PI / 4) + 0.4f, ca = (float) Math.cos(a), sa = (float) Math.sin(a);
            float out = r * 0.4f;
            d.box(x + ca * out, bowl - 0.5f, z + sa * out, 0.03f, 0.55f, 0.03f, null, SPRAY, DrawList.GLOW);
        }
        return y + rim;
    }
}
