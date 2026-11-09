package com.shumtugle.skydysh;

/**
 * The collider hall of LAB Inc., deep in the mountain under its deck: where it is, so that she can
 * be sent there. A hall of grey-teal panels under strip lights; on its floor the collider's ring,
 * steel and blue magnets, light running round in its pipe; at its near point the detector, a drum
 * on a plinth; along the far wall, up on a platform, the night shift of brums on their chargers,
 * fifteen, a green lamp over each; a door in the right-hand wall onto the platform.
 *
 * What the dispatcher's set shows of the night it went wrong, as time goes from its start: the
 * light running round, faster; a flash at the detector, and a black hole the size of a cherry
 * in it, a disc of light turning round it; from its pole a thin violet jet, swept along the row
 * of brums, each one it crosses glowing like coal, its lamp going red; then they turn, one after
 * another, and go out by the door.
 */
final class Collider {
    // the hall: its middle (x, z), its floor; half as wide, half as deep, as high
    static final float HX = Mountain.MX, HZ = Mountain.MZ + 4f, FLOOR = Mountain.TOP - 46f;
    static final float HALF_W = 18f, HALF_D = 22f, HIGH = 14f;
    // the ring: its middle (along), how far out, how high; how many pieces round
    static final float RING_Z = HZ + 4f, RING_R = 8f, RING_Y = 2.6f;
    static final int RING_PIECES = 32;
    // the detector on the ring's near side; the chargers along the far wall up on a platform so
    // high, so far in from the wall, so far either side; the door in the right-hand wall onto it
    static final float DET_Z = RING_Z - RING_R, ROW_Z = HZ + HALF_D - 2.2f, ROW_HALF = 14f, ROW_UP = 1.6f, DOOR_Z = HZ + HALF_D - 3.5f;
    // the night: the flash, the jet from and to, the turning to go, the last gone (seconds)
    // the black hole held so high over the floor
    static final float HOLE_Y = 6f;
    static final float FLASH = 2.4f, JET_FROM = 3.4f, JET_TO = 6.6f, TURN = 7.4f, GONE = 13f;

    /** Where the set's room stands to show the hall: x, y, z, the heading it faces. */
    static void view(float[] out) {
        out[0] = HX;
        out[1] = FLOOR + 4f;
        out[2] = HZ - HALF_D + 4f;
        out[3] = 0f;
    }

    /** The hall, and the night in it at so far into it (less than nothing: before, all calm). */
    static void draw(DrawList d, float time, float night) {
        float f = FLOOR, top = f + HIGH;
        int panel = 0x86A9AC, panelDark = 0x5E7F82, steel = 0xB2BAC0, magnet = 0x3E7FD8;
        // the shell: floor, ceiling, four walls, strips of light under the ceiling
        d.box(HX, f - 0.2f, HZ, HALF_W, 0.2f, HALF_D, null, 0x4E5A5E, DrawList.POLISHED);
        d.box(HX, top + 0.2f, HZ, HALF_W, 0.2f, HALF_D, null, 0x1E2628, DrawList.PAINT);
        for (int e = -1; e <= 1; e += 2) {
            d.box(HX + e * (HALF_W + 0.2f), f + HIGH * 0.5f, HZ, 0.2f, HIGH * 0.5f, HALF_D, null, panel, DrawList.PAINT);
            d.box(HX, f + HIGH * 0.5f, HZ + e * (HALF_D + 0.2f), HALF_W, HIGH * 0.5f, 0.2f, null, panel, DrawList.PAINT);
            for (float s = -HALF_D + 2f; s < HALF_D; s += 4f)
                d.box(HX + e * (HALF_W - 0.02f), f + 1.2f, HZ + s, 0.02f, 1.2f, 1.8f, null, panelDark, DrawList.PAINT);
        }
        for (int k = -2; k <= 2; k++) d.box(HX + k * 6f, top - 0.05f, HZ, 0.4f, 0.05f, HALF_D - 2f, null, 0xE8F4FF, DrawList.GLOW);
        // lamps along the walls, low, so the hall reads through the set's glass
        for (int e = -1; e <= 1; e += 2)
            for (float s0 = -HALF_D + 3f; s0 < HALF_D; s0 += 6f)
                d.box(HX + e * (HALF_W - 0.05f), f + 4.2f, HZ + s0, 0.05f, 0.15f, 1.2f, null, 0xCFF4FF, DrawList.GLOW);
        // the platform along the far wall, its edge lit; the door in the right-hand wall onto it
        d.box(HX, f + ROW_UP * 0.5f, ROW_Z + 0.4f, HALF_W, ROW_UP * 0.5f, HALF_D + HZ - ROW_Z - 0.2f, null, 0x2F393C, DrawList.POLISHED);
        d.box(HX, f + ROW_UP + 0.01f, ROW_Z - 1.75f, HALF_W, 0.02f, 0.06f, null, 0xFFB347, DrawList.GLOW);
        d.box(HX + HALF_W - 0.05f, f + ROW_UP + 1.6f, DOOR_Z, 0.05f, 1.6f, 1.2f, null, 0x111517, DrawList.PAINT);
        d.box(HX + HALF_W - 0.06f, f + ROW_UP + 3.35f, DOOR_Z, 0.05f, 0.08f, 0.5f, null, 0x7FF6E8, DrawList.GLOW);
        // the ring: steel pieces, every other a blue magnet, on stands
        float run = night < 0f ? time * 0.6f : time * (0.6f + Math.min(3f, Math.max(0f, night)) * 1.2f);
        for (int k = 0; k < RING_PIECES; k++) {
            float a = k * 2f * (float) Math.PI / RING_PIECES, x = HX + (float) Math.sin(a) * RING_R, z = RING_Z + (float) Math.cos(a) * RING_R;
            float[] t = yaw(a + (float) Math.PI * 0.5f);
            boolean mag = k % 2 == 0;
            d.box(x, f + RING_Y, z, mag ? 0.75f : 0.45f, mag ? 0.75f : 0.45f, (float) Math.PI * RING_R / RING_PIECES + 0.05f, t, mag ? magnet : steel,
                    mag ? DrawList.PAINT : DrawList.POLISHED);
            d.box(x, f + (RING_Y - 0.75f) * 0.5f, z, 0.25f, (RING_Y - 0.75f) * 0.5f, 0.25f, null, 0x55585C, DrawList.POLISHED);
        }
        // light running round in the pipe
        for (int q = 0; q < 3; q++) {
            float a = run + q * 2.0944f, x = HX + (float) Math.sin(a) * RING_R, z = RING_Z + (float) Math.cos(a) * RING_R;
            d.box(x, f + RING_Y, z, 0.5f, 0.5f, 0.5f, null, 0x9FE8FF, DrawList.GLOW);
        }
        // the detector: a drum on a plinth, its glass window toward the eye
        d.box(HX, f + 0.4f, DET_Z, 2.4f, 0.4f, 2.4f, null, 0x55585C, DrawList.POLISHED);
        for (int k = 0; k < 4; k++)
            d.box(HX, f + RING_Y, DET_Z, 1.9f * 0.4142f, 1.2f, 1.9f, yaw(k * 0.7854f), 0x8A949C, DrawList.POLISHED);
        d.box(HX, f + RING_Y, DET_Z - 1.92f, 0.7f, 0.6f, 0.04f, null, 0x24343C, DrawList.GLASS);
        // the trap over it: four posts, a ring of coils at the top
        for (int k = 0; k < 4; k++) {
            float a = k * 1.5708f + 0.7854f;
            d.box(HX + (float) Math.sin(a) * 1.5f, f + (RING_Y + 1.2f + HOLE_Y + 1.4f) * 0.5f, DET_Z + (float) Math.cos(a) * 1.5f, 0.08f,
                    (HOLE_Y + 1.4f - RING_Y - 1.2f) * 0.5f, 0.08f, null, 0x55585C, DrawList.POLISHED);
        }
        for (int k = 0; k < 8; k++) {
            float a = k * 0.7854f;
            d.box(HX + (float) Math.sin(a) * 1.5f, f + HOLE_Y + 1.4f, DET_Z + (float) Math.cos(a) * 1.5f, 0.3f, 0.12f, 0.3f, yaw(a), magnet, DrawList.PAINT);
        }
        // the black hole and what comes of it
        // (held over the detector in its trap, at the eye's height)
        float hy = f + HOLE_Y, hz = DET_Z;
        if (night >= FLASH) {
            float since = night - FLASH;
            if (since < 0.5f) {
                float s = 1f + since * 10f;
                d.box(HX, hy, hz, s, s, s, null, 0xF4E6FF, DrawList.GLOW);
            }
            // the disc of light round it, turning, tilted toward the eye
            for (int k = 0; k < 16; k++) {
                float a = k * 0.3927f + time * 3f, r = 0.9f;
                d.box(HX + (float) Math.sin(a) * r, hy + (float) Math.cos(a) * r * 0.35f, hz + (float) Math.cos(a) * r * 0.3f, 0.16f, 0.06f, 0.16f, null,
                        k % 3 == 0 ? 0xFFB060 : 0xC48BFF, DrawList.GLOW);
            }
            d.box(HX, hy, hz, 0.35f, 0.35f, 0.35f, yaw(time), 0x050308, DrawList.PLAIN);
            d.box(HX, hy, hz, 0.3f, 0.3f, 0.3f, yaw(time + 0.6f), 0x050308, DrawList.PLAIN);
            // the jet: from the pole up and over, swept along the row from left to right
            if (night >= JET_FROM && night < JET_TO + 0.4f) {
                float u = Math.min(1f, (night - JET_FROM) / (JET_TO - JET_FROM));
                float tx = HX - ROW_HALF + 2f * ROW_HALF * u, ty = f + ROW_UP + 1.6f, tz = ROW_Z;
                float flick = 0.08f + 0.04f * (float) Math.sin(time * 40f);
                beam(d, HX, hy, hz, tx, ty, tz, flick, 0xB86BFF);
                beam(d, HX, hy, hz, tx, ty, tz, flick * 0.4f, 0xF1E0FF);
            }
        }
        // the row of chargers and the night shift on them
        float wasHot = Frida.hot ? 1f : 0f;
        for (int k = 0; k < Herd.ALL; k++) {
            float x = HX - ROW_HALF + 2f * ROW_HALF * k / (Herd.ALL - 1f);
            float hitAt = JET_FROM + (JET_TO - JET_FROM) * k / (Herd.ALL - 1f);
            boolean hot = night >= hitAt;
            // the charger: two posts, a bar over, the lamp
            float g = f + ROW_UP;
            for (int e = -1; e <= 1; e += 2)
                d.box(x + e * 0.8f, g + 1.6f, ROW_Z + 0.6f, 0.08f, 1.6f, 0.08f, null, 0x55585C, DrawList.POLISHED);
            d.box(x, g + 3.2f, ROW_Z + 0.6f, 0.9f, 0.08f, 0.1f, null, 0x55585C, DrawList.POLISHED);
            d.box(x, g + 3.45f, ROW_Z + 0.6f, 0.12f, 0.12f, 0.06f, null, hot ? 0xFF3A2A : 0x46D27E, DrawList.GLOW);
            // the brum: standing on it, facing the hall; once hot, trembling; then turning and going
            // out along the row to the door
            float bx = x, bz = ROW_Z, h = (float) Math.PI;
            String motion = "idle";
            float mt = time + k;
            float goAt = TURN + k * 0.35f;
            if (night >= goAt) {
                float walk = (night - goAt) * 3.2f;
                float toDoor = HX + HALF_W - 1.5f - x;
                if (walk < toDoor) {
                    bx = x + walk;
                    h = (float) Math.PI * 0.5f;
                } else {
                    float down = walk - toDoor;
                    bx = HX + HALF_W - 1.5f + down;
                    if (bx > HX + HALF_W + 0.5f) continue;
                }
                motion = "walk";
                mt = walk / 3.2f * 1.3333f;
            } else if (hot) bx += 0.03f * (float) Math.sin(time * 30f + k);
            Frida.hot = hot;
            Frida.draw(d, bx, f + ROW_UP, bz, h + (float) Math.PI, Chase.SIZE, motion, mt, false);
        }
        Frida.hot = wasHot > 0f;
    }

    /** A beam of light from (ax, ay, az) to (bx, by, bz), so thick (half). */
    private static void beam(DrawList d, float ax, float ay, float az, float bx, float by, float bz, float half, int color) {
        float len = (float) Math.sqrt((bx - ax) * (bx - ax) + (by - ay) * (by - ay) + (bz - az) * (bz - az));
        float zx = (bx - ax) / len, zy = (by - ay) / len, zz = (bz - az) / len;
        float xx = zz, xz = -zx, xl = (float) Math.sqrt(xx * xx + xz * xz);
        xx /= xl;
        xz /= xl;
        float yx = -xz * zy, yy = xz * zx - xx * zz, yz = xx * zy;
        float[] t = {xx, yx, zx, 0f, yy, zy, xz, yz, zz};
        d.box((ax + bx) * 0.5f, (ay + by) * 0.5f, (az + bz) * 0.5f, half, half, len * 0.5f, t, color, DrawList.GLOW);
    }

    private static float[] yaw(float a) {
        float c = (float) Math.cos(a), s = (float) Math.sin(a);
        return new float[] {c, 0f, s, 0f, 1f, 0f, -s, 0f, c};
    }
}
