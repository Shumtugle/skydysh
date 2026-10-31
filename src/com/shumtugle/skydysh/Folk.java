package com.shumtugle.skydysh;

import java.util.Random;

/**
 * Townspeople, each one made from a seed and a part to play: of natural build, in boxes as
 * everything in the game is, riding on the same skeleton as Freedie and moving with the same
 * motions, so they walk, stand, fold their arms or dance as she does. A face of a few small
 * blocks (eyes, brows, nose, mouth, ears), hair, clothes of cloth that falls in folds, and what
 * goes with the part: a waiter's tray, a postman's bag, an old man's paper and stick.
 *
 * The figure stands at the origin facing -z, as the skeleton does; size 1 is a person of about
 * 1.76 m.
 */
final class Folk {
    /** The parts a person may play. */
    static final int WAITER = 0, POSTMAN = 1, GUARD = 2, OLD_MAN = 3, ICE_CREAM = 4, FISHER = 5, TOURIST = 6,
            LADY = 7, PARTS = 8;

    private static final int[] SKIN = {0xF0CDAE, 0xE9BC98, 0xD69F7A, 0xB98058, 0x8C5A3A, 0x6A4028};
    private static final int[] HAIR = {0x2E2118, 0x5A3C24, 0xC9A25E, 0x161311, 0x8A4A26, 0xB8B4AC};

    private static final int MAX = 120;
    private final int[] bone = new int[MAX], col = new int[MAX], mat = new int[MAX];
    private final float[] lp = new float[MAX * 3], lq = new float[MAX * 4], half = new float[MAX * 3];
    private int n;
    /** The part played, and how tall, as a share of the skeleton. */
    final int part;
    final float size;

    private static Moves moves;
    private static float[] pq, pp;
    private int at;                       // the bone the next blocks ride on

    /** A person, from a seed, playing a part. */
    Folk(long seed, int part) {
        Random r = new Random(seed * 0x9E3779B97F4A7C15L + part);
        this.part = part;
        moves = Frida.moves();
        if (moves != null && (pq == null || pq.length != moves.bone.length * 4)) {
            pq = new float[moves.bone.length * 4];
            pp = new float[moves.bone.length * 3];
        }
        int skin = SKIN[r.nextInt(SKIN.length)], hair = HAIR[r.nextInt(HAIR.length)];
        boolean woman = part == ICE_CREAM || part == LADY || part != OLD_MAN && part != GUARD && part != FISHER && r.nextInt(3) == 0;
        if (part == OLD_MAN) hair = 0xC8C4BC;
        float build = 0.9f + r.nextFloat() * 0.25f;          // slim to stout
        size = (woman ? 0.93f : 1f) * (0.96f + r.nextFloat() * 0.08f);
        if (moves == null) return;
        dress(r, skin, hair, woman, build);
    }

    // ---- laying blocks out round the rest pose (the skeleton's own units) ----

    private void on(String name) {
        at = moves.bone(name);
    }

    private void box(float x, float y, float z, float hx, float hy, float hz, float az, float ax, int c, int m) {
        if (at < 0 || n >= MAX) return;
        float cz = (float) Math.cos(az * 0.5f), sz = (float) Math.sin(az * 0.5f);
        float cx = (float) Math.cos(ax * 0.5f), sx = (float) Math.sin(ax * 0.5f);
        float[] own = {sx * cz, -sx * sz, cx * sz, cx * cz};
        float[] inv = {-moves.restQ[at * 4], -moves.restQ[at * 4 + 1], -moves.restQ[at * 4 + 2], moves.restQ[at * 4 + 3]};
        float[] d = {x - moves.restP[at * 3], y - moves.restP[at * 3 + 1], z - moves.restP[at * 3 + 2]};
        float[] l = Frida.rotate(inv, d), q = Frida.mul(inv, own);
        bone[n] = at;
        System.arraycopy(l, 0, lp, n * 3, 3);
        System.arraycopy(q, 0, lq, n * 4, 4);
        half[n * 3] = hx;
        half[n * 3 + 1] = hy;
        half[n * 3 + 2] = hz;
        col[n] = c;
        mat[n] = m;
        n++;
    }

    private void box(float x, float y, float z, float hx, float hy, float hz, int c, int m) {
        box(x, y, z, hx, hy, hz, 0f, 0f, c, m);
    }

    /** Clothes, body, face and what the part carries. */
    private void dress(Random r, int skin, int hair, boolean woman, float build) {
        int cloth = DrawList.CLOTH, plain = DrawList.PLAIN;
        // what the part wears: top, sleeves to the wrist or not, below, shoes, and a hat
        int top, below, shoes = 0x2E2724, hat = -1;
        boolean longSleeves = true, skirt = woman && r.nextBoolean(), shorts = false;
        switch (part) {
            case WAITER: top = 0xF4F1EA; below = 0x1E1D22; skirt = false; break;
            case POSTMAN: top = 0x6F8AA8; below = 0x34465E; hat = 0x34465E; skirt = false; break;
            case GUARD: top = 0x2A3550; below = 0x2A3550; hat = 0xF2F0EA; skirt = false; break;
            case OLD_MAN: top = 0xC9B48E; below = 0x7A6A55; hat = 0x6A5A48; break;
            case ICE_CREAM: top = 0xF6E3EA; below = 0xE07A9A; skirt = true; longSleeves = false; break;
            case FISHER: top = 0xE8C23A; below = 0x2E3A40; shoes = 0x2A3A30; hat = 0xE8C23A; skirt = false; break;
            case TOURIST: top = new int[]{0x5FB8C8, 0xF08A5A, 0xA8D86A}[r.nextInt(3)]; below = 0xD8C8A0; hat = 0xE3C784;
                longSleeves = false; shorts = true; skirt = false; break;
            default: top = new int[]{0xD9735A, 0x6F92C6, 0xE8C25A, 0x5E9E92, 0xD58FAE}[r.nextInt(5)];
                below = new int[]{0x3F5679, 0xF2EEE4, 0x2F2F36}[r.nextInt(3)]; longSleeves = r.nextBoolean(); break;
        }
        float w = build;
        for (int s = -1; s <= 1; s += 2) {
            String side = s < 0 ? "_l" : "_r";
            float lx = s * 0.089f;
            // shoes, and the legs: trousers, bare under shorts or a skirt
            on("foot" + side);
            box(lx, 0.045f, -0.035f, 0.05f, 0.045f, 0.115f, part == FISHER ? 0x2A3A30 : shoes, plain);
            on("calf" + side);
            boolean bare = shorts || skirt;
            box(lx, 0.31f, 0f, 0.052f * w, 0.215f, 0.058f * w, part == FISHER ? 0x2A3A30 : bare ? skin : below, bare ? plain : cloth);
            if (!bare && part != FISHER) box(lx, 0.12f, 0f, 0.056f * w, 0.03f, 0.062f * w, Street.scale(below, 0.85f), cloth);
            on("thigh" + side);
            box(lx, 0.73f, 0f, 0.068f * w, 0.2f, 0.074f * w, skirt ? skin : below, skirt ? plain : cloth);
            if (shorts) box(lx, 0.83f, 0f, 0.072f * w, 0.1f, 0.078f * w, below, cloth);
        }
        // hips; a skirt over them; a belt
        on("pelvis");
        box(0f, 0.93f, 0.005f, 0.16f * w, 0.075f, 0.1f * w, skirt ? below : below, cloth);
        if (skirt) {
            box(0f, 0.78f, 0.005f, 0.2f * w, 0.17f, 0.14f * w, below, cloth);
            box(0f, 0.63f, 0.005f, 0.21f * w, 0.02f, 0.15f * w, Street.scale(below, 0.85f), cloth);
        }
        if (!skirt) box(0f, 0.995f, 0.005f, 0.165f * w, 0.014f, 0.105f * w, 0x3A2A20, plain);
        // the body: waist, middle, chest
        on("spine_01");
        box(0f, 1.08f, 0.01f, 0.15f * w, 0.07f, 0.095f * w, top, cloth);
        on("spine_02");
        box(0f, 1.2f, 0.005f, 0.165f * w, 0.065f, 0.1f * w, top, cloth);
        on("spine_03");
        box(0f, 1.34f, 0.005f, 0.185f * w, 0.09f, 0.105f * w, top, cloth);
        if (woman) box(0f, 1.33f, -0.1f * w, 0.13f, 0.045f, 0.03f, top, cloth);
        // the collar
        box(0f, 1.44f, 0.005f, 0.075f, 0.018f, 0.06f, Street.scale(top, 0.92f), cloth);
        // the part's own over the body
        if (part == WAITER) {
            // a black waistcoat, a bow tie; a long white apron
            box(0f, 1.3f, 0.005f, 0.19f * w, 0.13f, 0.11f * w, 0x1E1D22, cloth);
            box(0f, 1.43f, -0.075f, 0.035f, 0.014f, 0.01f, 0x1E1D22, plain);
            on("pelvis");
            box(0f, 0.75f, -0.11f * w, 0.17f * w, 0.24f, 0.012f, 0xF4F1EA, cloth);
        } else if (part == POSTMAN) {
            // the strap across, the bag on the hip
            on("spine_03");
            box(0f, 1.27f, -0.11f * w, 0.012f, 0.22f, 0.006f, 0.75f, 0f, 0x5A3A22, plain);
            on("pelvis");
            box(0.2f * w, 0.9f, -0.02f, 0.05f, 0.11f, 0.13f, 0x7A5030, DrawList.GRAIN);
            box(0.2f * w, 0.97f, -0.02f, 0.055f, 0.04f, 0.135f, 0x6A4228, DrawList.GRAIN);
        } else if (part == GUARD) {
            // a white belt and strap, buttons of brass
            on("pelvis");
            box(0f, 0.995f, 0.005f, 0.168f * w, 0.02f, 0.108f * w, 0xF2F0EA, plain);
            on("spine_03");
            for (int b = 0; b < 3; b++) box(0f, 1.38f - b * 0.07f, -0.108f * w, 0.012f, 0.012f, 0.004f, 0xD8B45A, DrawList.GOLD);
        } else if (part == OLD_MAN) {
            // the jacket open over a shirt, its lapels
            on("spine_03");
            box(0f, 1.34f, -0.107f * w, 0.05f, 0.085f, 0.004f, 0xF0EBDF, cloth);
            box(0f, 1.36f, -0.111f * w, 0.012f, 0.06f, 0.004f, 0x8A2E2A, plain);
        } else if (part == ICE_CREAM) {
            // stripes across the top, an apron, a scarf on the head below
            on("spine_03");
            for (int b = 0; b < 3; b++) box(0f, 1.29f + b * 0.05f, 0.005f, 0.188f * w, 0.012f, 0.107f * w, 0xE07A9A, plain);
            on("pelvis");
            box(0f, 0.86f, -0.11f * w, 0.14f, 0.13f, 0.01f, 0xF8F4EC, cloth);
        } else if (part == FISHER) {
            // an oilskin smock down over the hips
            on("pelvis");
            box(0f, 0.86f, 0.005f, 0.19f * w, 0.1f, 0.12f * w, top, cloth);
        } else if (part == TOURIST) {
            // a camera on its strap
            on("spine_03");
            box(0f, 1.27f, -0.12f * w, 0.05f, 0.035f, 0.025f, 0x222226, plain);
            box(0f, 1.27f, -0.15f * w, 0.018f, 0.018f, 0.012f, 0x3A3A44, DrawList.GLASS);
        }
        // the arms: sleeves, bare forearms when short, the hands
        for (int s = -1; s <= 1; s += 2) {
            String side = s < 0 ? "_l" : "_r";
            on("clavicle" + side);
            box(s * 0.16f * w, 1.42f, 0.03f, 0.07f, 0.055f, 0.075f, top, cloth);
            on("upperarm" + side);
            box(s * 0.33f, 1.441f, 0.065f, 0.13f, 0.052f * w, 0.054f * w, longSleeves ? top : top, cloth);
            if (!longSleeves) box(s * 0.25f, 1.441f, 0.065f, 0.05f, 0.056f * w, 0.058f * w, Street.scale(top, 0.9f), cloth);
            on("lowerarm" + side);
            box(s * 0.6f, 1.441f, 0.068f, 0.13f, 0.043f, 0.043f, longSleeves ? top : skin, longSleeves ? cloth : plain);
            if (longSleeves) box(s * 0.715f, 1.441f, 0.068f, 0.018f, 0.046f, 0.046f, Street.scale(top, 0.88f), cloth);
            on("hand" + side);
            int glove = part == GUARD ? 0xF2F0EA : skin;
            box(s * 0.79f, 1.441f, 0.065f, 0.055f, 0.03f, 0.042f, glove, plain);
            box(s * 0.765f, 1.441f, 0.02f, 0.025f, 0.02f, 0.016f, glove, plain);
        }
        // the neck and the head: a face of a few blocks, hair, ears
        on("neck_01");
        box(0f, 1.49f, 0.01f, 0.042f, 0.045f, 0.045f, skin, plain);
        on("Head");
        float hy = 1.6f;
        box(0f, hy, -0.008f, 0.078f, 0.1f, 0.092f, skin, plain);
        box(0f, hy - 0.075f, -0.03f, 0.06f, 0.03f, 0.07f, skin, plain);
        for (int s = -1; s <= 1; s += 2) {
            box(s * 0.033f, hy + 0.015f, -0.1f, 0.019f, 0.01f, 0.003f, 0xF4F0E6, plain);
            box(s * 0.031f, hy + 0.015f, -0.103f, 0.009f, 0.009f, 0.002f, 0x2A2018, plain);
            box(s * 0.034f, hy + 0.042f, -0.101f, 0.024f, 0.006f, 0.004f, s * -0.08f, 0f, Street.scale(hair, 0.9f), plain);
            box(s * 0.081f, hy, 0f, 0.006f, 0.026f, 0.018f, Street.scale(skin, 0.92f), plain);
        }
        box(0f, hy - 0.012f, -0.104f, 0.011f, 0.026f, 0.012f, Street.scale(skin, 0.94f), plain);
        box(0f, hy - 0.055f, -0.1f, 0.025f, 0.006f, 0.004f, woman ? 0xB04048 : 0x8A4A40, plain);
        if (part == GUARD || part == OLD_MAN && r.nextBoolean()) box(0f, hy - 0.038f, -0.102f, 0.032f, 0.008f, 0.005f, hair, plain);
        // hair: a cap of it, and down the back; long for some women
        boolean bald = part == OLD_MAN && r.nextBoolean();
        if (!bald) box(0f, hy + 0.08f, 0.005f, 0.083f, 0.035f, 0.098f, hair, cloth);
        box(0f, hy + 0.01f, 0.07f, 0.081f, 0.08f, 0.03f, hair, cloth);
        if (woman && part != ICE_CREAM) box(0f, hy - 0.08f, 0.075f, 0.07f, 0.12f, 0.025f, hair, cloth);
        if (woman) for (int s = -1; s <= 1; s += 2) box(s * 0.078f, hy + 0.01f, 0.005f, 0.012f, 0.07f, 0.08f, hair, cloth);
        // a hat for the part
        if (part == ICE_CREAM) box(0f, hy + 0.085f, 0.0f, 0.088f, 0.04f, 0.1f, 0xE07A9A, cloth);
        else if (part == TOURIST) {
            box(0f, hy + 0.105f, 0f, 0.17f, 0.008f, 0.17f, hat, DrawList.GRAIN);
            box(0f, hy + 0.14f, 0f, 0.085f, 0.035f, 0.095f, hat, DrawList.GRAIN);
            box(0f, hy + 0.115f, 0f, 0.087f, 0.01f, 0.097f, 0x8A2E2A, plain);
        } else if (part == GUARD || part == POSTMAN) {
            box(0f, hy + 0.12f, 0f, 0.09f, 0.04f, 0.1f, hat, cloth);
            box(0f, hy + 0.085f, -0.11f, 0.07f, 0.006f, 0.04f, 0x1E1D22, plain);
            if (part == GUARD) box(0f, hy + 0.12f, -0.1f, 0.015f, 0.015f, 0.004f, 0xD8B45A, DrawList.GOLD);
        } else if (part == OLD_MAN) {
            box(0f, hy + 0.105f, -0.015f, 0.092f, 0.03f, 0.11f, hat, cloth);
            box(0f, hy + 0.09f, -0.12f, 0.07f, 0.006f, 0.03f, hat, cloth);
        } else if (part == FISHER) {
            box(0f, hy + 0.11f, 0.01f, 0.1f, 0.035f, 0.11f, hat, cloth);
            box(0f, hy + 0.08f, 0.08f, 0.11f, 0.008f, 0.08f, 0.0f, 0.25f, hat, cloth);
        }
    }

    /** Draws the person at (x, y, z), turned about the upright by yaw (0: facing -z), in a motion at a time. */
    void draw(DrawList d, float x, float y, float z, float yaw, String motion, float t) {
        if (moves == null) return;
        int c = moves.motion(motion);
        if (c < 0) c = 0;
        moves.pose(c, t, pq, pp);
        float cy = (float) Math.cos(yaw), sy = (float) Math.sin(yaw);
        float[] bq = new float[4], q0 = new float[4], p0 = new float[3];
        for (int i = 0; i < n; i++) {
            int b = bone[i];
            System.arraycopy(pq, b * 4, bq, 0, 4);
            System.arraycopy(lq, i * 4, q0, 0, 4);
            System.arraycopy(lp, i * 3, p0, 0, 3);
            float[] off = Frida.rotate(bq, p0);
            float px = (pp[b * 3] + off[0]) * size, py = (pp[b * 3 + 1] + off[1]) * size, pz = (pp[b * 3 + 2] + off[2]) * size;
            float[] q = Frida.mul(bq, q0);
            float qx = q[0], qy = q[1], qz = q[2], qw = q[3];
            float r00 = 1 - 2 * (qy * qy + qz * qz), r01 = 2 * (qx * qy - qz * qw), r02 = 2 * (qx * qz + qy * qw);
            float r10 = 2 * (qx * qy + qz * qw), r11 = 1 - 2 * (qx * qx + qz * qz), r12 = 2 * (qy * qz - qx * qw);
            float r20 = 2 * (qx * qz - qy * qw), r21 = 2 * (qy * qz + qx * qw), r22 = 1 - 2 * (qx * qx + qy * qy);
            rot[0] = cy * r00 + sy * r20; rot[1] = cy * r01 + sy * r21; rot[2] = cy * r02 + sy * r22;
            rot[3] = r10; rot[4] = r11; rot[5] = r12;
            rot[6] = -sy * r00 + cy * r20; rot[7] = -sy * r01 + cy * r21; rot[8] = -sy * r02 + cy * r22;
            d.box(x + cy * px + sy * pz, y + py, z - sy * px + cy * pz, half[i * 3] * size, half[i * 3 + 1] * size,
                    half[i * 3 + 2] * size, rot, col[i], DrawList.material(mat[i], i * 37));
        }
        props(d, x, y, z, cy, sy);
    }

    /** Where bone b is now in the world, posed as draw() left it, into out. */
    private void where(String name, float x, float y, float z, float cy, float sy, float[] out) {
        int b = moves.bone(name);
        float px = pp[b * 3] * size, py = pp[b * 3 + 1] * size, pz = pp[b * 3 + 2] * size;
        out[0] = x + cy * px + sy * pz;
        out[1] = y + py;
        out[2] = z - sy * px + cy * pz;
    }

    private final float[] hr = new float[3], hl = new float[3];

    /** What the part carries, in the world where the hands are now: a stick, a rod, a cone, a tray. */
    private void props(DrawList d, float x, float y, float z, float cy, float sy) {
        where("hand_r", x, y, z, cy, sy, hr);
        where("hand_l", x, y, z, cy, sy, hl);
        // ahead of the person (it faces -z at no turn)
        float fx = -sy, fz = -cy;
        if (part == OLD_MAN) {
            // a walking stick from the fist to the ground, a little ahead
            stick(d, hr[0] + fx * 0.06f, hr[1], hr[2] + fz * 0.06f, hr[0] + fx * 0.14f, y, hr[2] + fz * 0.14f, 0.014f, 0x4A3220, DrawList.WOOD);
            // a folded paper in the other hand
            d.box(hl[0] + fx * 0.05f, hl[1] - 0.02f, hl[2] + fz * 0.05f, 0.03f, 0.13f, 0.06f, turn(cy, sy), 0xEDE6D4, DrawList.PLAIN);
        } else if (part == FISHER) {
            // a rod up and ahead out of the fist
            stick(d, hr[0], hr[1], hr[2], hr[0] + fx * 1.5f, hr[1] + 1.5f, hr[2] + fz * 1.5f, 0.011f, 0x5A3A22, DrawList.WOOD);
            d.box(hr[0] + fx * 0.12f, hr[1] + 0.1f, hr[2] + fz * 0.12f, 0.035f, 0.035f, 0.02f, turn(cy, sy), 0x9A9EA2, DrawList.GOLD);
        } else if (part == ICE_CREAM) {
            // a cone held up, a scoop on it
            d.box(hr[0] + fx * 0.05f, hr[1] + 0.08f, hr[2] + fz * 0.05f, 0.022f, 0.06f, 0.022f, turn(cy, sy), 0xD8A260, DrawList.GRAIN);
            d.box(hr[0] + fx * 0.05f, hr[1] + 0.16f, hr[2] + fz * 0.05f, 0.04f, 0.035f, 0.04f, turn(cy, sy), 0xF4D8E0, DrawList.PLAIN);
        } else if (part == WAITER) {
            // a round tray on the palm, a glass and a cup on it
            float tx = hl[0] + fx * 0.1f, ty = hl[1] + 0.04f, tz = hl[2] + fz * 0.1f;
            d.box(tx, ty, tz, 0.17f, 0.008f, 0.17f, turn(cy, sy), 0xC8CCD0, DrawList.GOLD);
            d.box(tx - 0.05f, ty + 0.05f, tz, 0.022f, 0.045f, 0.022f, turn(cy, sy), 0xB8D8DC, DrawList.GLASS);
            d.box(tx + 0.06f, ty + 0.03f, tz + 0.04f, 0.032f, 0.025f, 0.032f, turn(cy, sy), 0xF4F1EA, DrawList.PLAIN);
        }
    }

    private final float[] turned = new float[9];

    private float[] turn(float cy, float sy) {
        turned[0] = cy; turned[1] = 0f; turned[2] = sy;
        turned[3] = 0f; turned[4] = 1f; turned[5] = 0f;
        turned[6] = -sy; turned[7] = 0f; turned[8] = cy;
        return turned;
    }

    /** A thin rod from (ax, ay, az) to (bx, by, bz). */
    static void stick(DrawList d, float ax, float ay, float az, float bx, float by, float bz, float r, int color, int material) {
        float ux = bx - ax, uy = by - ay, uz = bz - az, len = (float) Math.sqrt(ux * ux + uy * uy + uz * uz);
        if (len < 1e-4f) return;
        ux /= len;
        uy /= len;
        uz /= len;
        // two ways square to it
        float px = Math.abs(uy) < 0.9f ? 0f : 1f, py = Math.abs(uy) < 0.9f ? 1f : 0f, pz = 0f;
        float vx = uy * pz - uz * py, vy = uz * px - ux * pz, vz = ux * py - uy * px;
        float vl = (float) Math.sqrt(vx * vx + vy * vy + vz * vz);
        vx /= vl;
        vy /= vl;
        vz /= vl;
        float wx = uy * vz - uz * vy, wy = uz * vx - ux * vz, wz = ux * vy - uy * vx;
        float[] m = {vx, ux, wx, vy, uy, wy, vz, uz, wz};
        d.box((ax + bx) * 0.5f, (ay + by) * 0.5f, (az + bz) * 0.5f, r, len * 0.5f, r, m, color, material);
    }

    private final float[] rot = new float[9];
}
