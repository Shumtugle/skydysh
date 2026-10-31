package com.shumtugle.skydysh;

/**
 * The town's animals, in boxes as everything is: a gull, a pigeon, a cat, a dog. Each is drawn
 * standing at (x, y, z) facing along its heading (0 along +z), its own small motion at time t:
 * a gull's head turning, a pigeon's nod, a cat's tail, a dog's.
 */
final class Beasts {
    private Beasts() {
    }

    static final int GULL = 0, PIGEON = 1, CAT = 2, DOG = 3;

    // the one being drawn: where, turned how
    private static float ox, oy, oz, c, s;
    private static final float[] rot = new float[9];

    static void draw(DrawList d, int kind, float x, float y, float z, float heading, float t) {
        ox = x;
        oy = y;
        oz = z;
        c = (float) Math.cos(heading);
        s = (float) Math.sin(heading);
        switch (kind) {
            case GULL: gull(d, t); break;
            case PIGEON: pigeon(d, t); break;
            case CAT: cat(d, t); break;
            default: dog(d, t); break;
        }
    }

    /** A box at (lx, ly, lz) in the beast's own terms (x right, y up, z ahead), tipped nose-down by pitch. */
    private static void box(DrawList d, float lx, float ly, float lz, float hx, float hy, float hz, float pitch, int color, int material) {
        float cp = (float) Math.cos(pitch), sp = (float) Math.sin(pitch);
        // columns of the turn: its own x, y, z in the world
        rot[0] = c; rot[3] = 0f; rot[6] = -s;
        rot[1] = s * sp; rot[4] = cp; rot[7] = c * sp;
        rot[2] = s * cp; rot[5] = -sp; rot[8] = c * cp;
        d.box(ox + c * lx + s * lz, oy + ly, oz - s * lx + c * lz, hx, hy, hz, rot, color, material);
    }

    private static void box(DrawList d, float lx, float ly, float lz, float hx, float hy, float hz, int color, int material) {
        box(d, lx, ly, lz, hx, hy, hz, 0f, color, material);
    }

    private static void gull(DrawList d, float t) {
        int white = 0xF4F2EE, grey = 0x9AA4AC, black = 0x1E1E22, yellow = 0xF0C43A, pink = 0xE8A090;
        int plain = DrawList.PLAIN;
        for (int e = -1; e <= 1; e += 2) {
            box(d, e * 0.04f, 0.07f, 0.0f, 0.008f, 0.07f, 0.008f, pink, plain);
            box(d, e * 0.04f, 0.005f, 0.03f, 0.022f, 0.005f, 0.03f, pink, plain);
        }
        box(d, 0f, 0.21f, 0f, 0.085f, 0.075f, 0.19f, 0.08f, white, plain);
        // the wings folded along the back, grey, their tips black past the tail
        for (int e = -1; e <= 1; e += 2) {
            box(d, e * 0.075f, 0.25f, -0.03f, 0.02f, 0.05f, 0.2f, 0.12f, grey, plain);
            box(d, e * 0.06f, 0.24f, -0.25f, 0.016f, 0.025f, 0.08f, 0.15f, black, plain);
        }
        box(d, 0f, 0.22f, -0.21f, 0.05f, 0.025f, 0.06f, 0.15f, white, plain);
        // the head, turning now and then
        float turn = (float) Math.sin(t * 0.9f) > 0.6f ? 0.5f : 0f;
        float hx = (float) Math.sin(turn) * 0.04f;
        box(d, hx, 0.33f, 0.17f, 0.055f, 0.055f, 0.065f, white, plain);
        box(d, hx, 0.32f, 0.26f, 0.014f, 0.015f, 0.045f, yellow, plain);
        box(d, hx, 0.31f, 0.285f, 0.012f, 0.008f, 0.012f, 0xD84030, plain);
        for (int e = -1; e <= 1; e += 2) box(d, hx + e * 0.056f, 0.345f, 0.19f, 0.003f, 0.009f, 0.009f, black, plain);
    }

    private static void pigeon(DrawList d, float t) {
        int grey = 0x8E939C, dark = 0x5E636C, neck = 0x4E7A6A, sheen = 0x7A5A8A, red = 0xC0504A;
        int plain = DrawList.PLAIN;
        for (int e = -1; e <= 1; e += 2) box(d, e * 0.03f, 0.035f, 0.01f, 0.007f, 0.035f, 0.02f, red, plain);
        box(d, 0f, 0.12f, 0f, 0.065f, 0.06f, 0.12f, 0.15f, grey, plain);
        for (int e = -1; e <= 1; e += 2) {
            box(d, e * 0.058f, 0.14f, -0.02f, 0.012f, 0.04f, 0.11f, 0.2f, dark, plain);
            box(d, e * 0.06f, 0.13f, -0.04f, 0.01f, 0.008f, 0.05f, 0.2f, 0x2E3036, plain);
        }
        box(d, 0f, 0.12f, -0.15f, 0.035f, 0.012f, 0.06f, 0.3f, dark, plain);
        // the nod: the neck and head go forward and back as it steps
        float nod = 0.03f * (float) Math.sin(t * 9f);
        box(d, 0f, 0.18f, 0.09f + nod * 0.5f, 0.04f, 0.045f, 0.04f, neck, plain);
        box(d, 0f, 0.16f, 0.1f + nod * 0.5f, 0.042f, 0.02f, 0.035f, sheen, plain);
        box(d, 0f, 0.235f, 0.11f + nod, 0.032f, 0.032f, 0.04f, grey, plain);
        box(d, 0f, 0.225f, 0.16f + nod, 0.009f, 0.009f, 0.02f, 0x3A3036, plain);
        for (int e = -1; e <= 1; e += 2) box(d, e * 0.033f, 0.245f, 0.12f + nod, 0.003f, 0.007f, 0.007f, 0xE88A30, plain);
    }

    private static void cat(DrawList d, float t) {
        int fur = 0xD98A3A, stripe = 0xA85E24, white = 0xF2E8DA, green = 0x9AD06A, pink = 0xE09A9A;
        int plain = DrawList.PLAIN, cloth = DrawList.CLOTH;
        // sitting: haunches, the body up from them, the chest white, the forelegs straight
        box(d, 0f, 0.09f, -0.05f, 0.1f, 0.09f, 0.12f, fur, cloth);
        box(d, 0f, 0.2f, 0.02f, 0.075f, 0.12f, 0.08f, -0.35f, fur, cloth);
        for (int k = 0; k < 3; k++) box(d, 0f, 0.16f + k * 0.06f, -0.03f + k * 0.03f, 0.078f, 0.008f, 0.06f, -0.35f, stripe, plain);
        box(d, 0f, 0.21f, 0.085f, 0.05f, 0.08f, 0.02f, -0.3f, white, plain);
        for (int e = -1; e <= 1; e += 2) {
            box(d, e * 0.035f, 0.08f, 0.09f, 0.02f, 0.08f, 0.022f, fur, plain);
            box(d, e * 0.035f, 0.012f, 0.11f, 0.022f, 0.012f, 0.03f, white, plain);
        }
        // the head, the ears, the eyes, the nose, the whiskers
        box(d, 0f, 0.36f, 0.08f, 0.07f, 0.06f, 0.06f, fur, plain);
        box(d, 0f, 0.34f, 0.135f, 0.035f, 0.025f, 0.015f, white, plain);
        for (int e = -1; e <= 1; e += 2) {
            box(d, e * 0.045f, 0.43f, 0.07f, 0.02f, 0.025f, 0.008f, fur, plain);
            box(d, e * 0.045f, 0.425f, 0.078f, 0.012f, 0.016f, 0.003f, pink, plain);
            box(d, e * 0.028f, 0.37f, 0.141f, 0.014f, 0.01f, 0.003f, green, plain);
            box(d, e * 0.028f, 0.37f, 0.144f, 0.004f, 0.009f, 0.002f, 0x141210, plain);
            box(d, e * 0.06f, 0.345f, 0.14f, 0.035f, 0.002f, 0.002f, 0xF4F0E8, plain);
        }
        box(d, 0f, 0.352f, 0.152f, 0.008f, 0.006f, 0.004f, pink, plain);
        // the tail round the paws, its tip lifting
        float lift = 0.03f + 0.03f * (float) Math.sin(t * 2f);
        box(d, 0.09f, 0.025f, 0.0f, 0.025f, 0.02f, 0.11f, fur, plain);
        box(d, 0.07f, 0.025f + lift * 0.5f, 0.12f, 0.022f, 0.02f, 0.05f, stripe, plain);
        box(d, 0.05f, 0.03f + lift, 0.17f, 0.02f, 0.02f, 0.03f, fur, plain);
    }

    private static void dog(DrawList d, float t) {
        int fur = 0x8A5A34, dark = 0x5A3A22, light = 0xD8B88A, black = 0x161412;
        int plain = DrawList.PLAIN, cloth = DrawList.CLOTH;
        for (int fx = -1; fx <= 1; fx += 2)
            for (int e = -1; e <= 1; e += 2) {
                box(d, e * 0.075f, 0.16f, fx * 0.19f, 0.032f, 0.16f, 0.035f, fur, plain);
                box(d, e * 0.075f, 0.015f, fx * 0.19f + 0.02f, 0.035f, 0.015f, 0.045f, light, plain);
            }
        box(d, 0f, 0.4f, 0f, 0.11f, 0.1f, 0.26f, fur, cloth);
        box(d, 0f, 0.33f, 0.05f, 0.08f, 0.04f, 0.18f, light, plain);
        // the collar, the head, the muzzle, the ears hanging
        box(d, 0f, 0.5f, 0.22f, 0.085f, 0.06f, 0.025f, 0.6f, 0xC0383A, plain);
        box(d, 0f, 0.58f, 0.3f, 0.08f, 0.075f, 0.09f, fur, plain);
        box(d, 0f, 0.54f, 0.41f, 0.045f, 0.04f, 0.06f, light, plain);
        box(d, 0f, 0.565f, 0.47f, 0.02f, 0.016f, 0.008f, black, plain);
        box(d, 0f, 0.515f, 0.43f, 0.03f, 0.005f, 0.035f, 0x6A2A2A, plain);
        for (int e = -1; e <= 1; e += 2) {
            box(d, e * 0.088f, 0.56f, 0.28f, 0.015f, 0.07f, 0.04f, dark, plain);
            box(d, e * 0.04f, 0.61f, 0.388f, 0.012f, 0.012f, 0.003f, black, plain);
        }
        // the tail up and wagging
        float wag = 0.06f * (float) Math.sin(t * 12f);
        box(d, wag * 0.5f, 0.5f, -0.28f, 0.02f, 0.02f, 0.05f, -0.7f, fur, plain);
        box(d, wag, 0.58f, -0.31f, 0.018f, 0.018f, 0.05f, -1.1f, fur, plain);
    }
}
