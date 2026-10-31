package com.shumtugle.skydysh;

/**
 * A stonemason's maul: a long ash handle swelling a little toward the butt, a navy band where it
 * goes into the head; the head a block of dark steel across the handle's end, its two striking
 * faces worn bright, a wedge driven into its top. Its own frame: the handle along x, the grip at
 * the origin, the head out at +x; the head's length along z.
 */
final class Maul {
    // the parts: middle along x, y, z; half sizes; color (meters, at size 1)
    private static final float[][] PARTS = {
            {-0.05f, 0f, 0f, 0.05f, 0.026f, 0.026f},       // butt
            {0.2f, 0f, 0f, 0.22f, 0.019f, 0.019f},         // handle near the grip
            {0.55f, 0f, 0f, 0.15f, 0.022f, 0.022f},        // handle toward the head
            {0.69f, 0f, 0f, 0.018f, 0.03f, 0.03f},         // band
            {0.76f, 0f, 0f, 0.055f, 0.055f, 0.13f},        // head
            {0.76f, 0f, 0.138f, 0.047f, 0.047f, 0.009f},   // faces
            {0.76f, 0f, -0.138f, 0.047f, 0.047f, 0.009f},
            {0.818f, 0f, 0f, 0.006f, 0.014f, 0.022f},      // wedge
    };
    private static final int[] COL = {0x8E5E36, 0xC99B5E, 0xD5AA6C, 0x26304F, 0x4C5466, 0xC8CED6, 0xC8CED6, 0x2A2E38};
    private static final float[] T = new float[9];

    private Maul() {
    }

    /**
     * Draws it turned by rot (rows; columns are its own x, y, z in the world), size times, the
     * point at pivot along its handle put at (x, y, z).
     */
    static void draw(DrawList d, float[] rot, float x, float y, float z, float size, float pivot) {
        System.arraycopy(rot, 0, T, 0, 9);
        for (int i = 0; i < PARTS.length; i++) {
            float[] p = PARTS[i];
            float ox = (p[0] - pivot) * size, oy = p[1] * size, oz = p[2] * size;
            float cx = x + T[0] * ox + T[1] * oy + T[2] * oz;
            float cy = y + T[3] * ox + T[4] * oy + T[5] * oz;
            float cz = z + T[6] * ox + T[7] * oy + T[8] * oz;
            int material = i <= 2 ? DrawList.WOOD : DrawList.PLAIN;
            d.box(cx, cy, cz, p[3] * size, p[4] * size, p[5] * size, T, COL[i], material);
        }
    }
}
