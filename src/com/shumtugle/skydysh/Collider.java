package com.shumtugle.skydysh;

/**
 * The collider hall of LAB Inc., deep in the mountain under its deck: where it is, so that she can
 * be sent there. A hall of grey-teal panels under strip lights; on its floor the collider's ring,
 * steel and blue magnets, light running round in its pipe; at its near point the detector, a drum
 * on a plinth; along the far wall, up on a platform, the night shift of brums on their chargers,
 * fifteen, a green lamp over each; a door in the right-hand wall onto the platform; in the
 * left-hand wall, near the front, the lift down from the lobby.
 *
 * What the dispatcher's set shows of the night it went wrong, as time goes from its start: the
 * light running round, faster; a flash at the detector, and a black hole the size of a cherry
 * in it, a disc of light turning round it; from its pole a thin violet jet, swept along the row
 * of brums, each one it crosses glowing like coal, its lamp going red; then they turn, one after
 * another, and go out by the door.
 *
 * And the hall as she finds it after (see Cherry): the chargers empty, their lamps red; six of
 * the ring's magnets knocked off their stands, hanging askew, blinking red; the hole turning in
 * its trap, its jet going down and out to the floor and round; set again, each lit green; the
 * ring taking the hole, the hole drawn in to a bead. Once the job is done the hole stays so, calm,
 * and the shift is back on its chargers.
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
    /** The lift from the lobby: in the left-hand wall, so far in from the front. */
    static final float LIFT_V = 5f;
    // a magnet knocked off: hanging so much lower, leaning out so far
    static final float KNOCK_DROP = 0.7f, KNOCK_LEAN = 0.5f;
    // the bead the hole is drawn in to, as large as it was
    static final float BEAD = 0.3f;

    /** Where the set's room stands to show the hall: x, y, z, the heading it faces. */
    static void view(float[] out) {
        out[0] = HX;
        out[1] = FLOOR + 4f;
        out[2] = HZ - HALF_D + 4f;
        out[3] = 0f;
    }

    /** Where piece k of the ring stands: x. */
    static float ringX(int k) {
        return HX + (float) Math.sin(k * 2f * Math.PI / RING_PIECES) * RING_R;
    }

    /** Where piece k of the ring stands: z. */
    static float ringZ(int k) {
        return RING_Z + (float) Math.cos(k * 2f * Math.PI / RING_PIECES) * RING_R;
    }

    /** The hall, and the night in it at so far into it (less than nothing: before, all calm). */
    static void draw(DrawList d, float time, float night) {
        float f = FLOOR;
        shell(d, f, false);
        detector(d, f);
        float run = night < 0f ? time * 0.6f : time * (0.6f + Math.min(3f, Math.max(0f, night)) * 1.2f);
        ring(d, f, time, run, null, false);
        // the black hole and what comes of it
        // (held over the detector in its trap, at the eye's height)
        float hy = f + HOLE_Y, hz = DET_Z;
        if (night >= FLASH) {
            float since = night - FLASH;
            if (since < 0.5f) flash(d, hy, since);
            hole(d, hy, time * 3f, 1f);
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
            float x = rowX(k);
            float hitAt = JET_FROM + (JET_TO - JET_FROM) * k / (Herd.ALL - 1f);
            boolean hot = night >= hitAt;
            charger(d, x, f + ROW_UP, hot ? 0xFF3A2A : 0x46D27E);
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

    /**
     * The hall as it is now, its floor at f: with the job on (c), the ring's knocked magnets, the
     * hole and its jet, the chargers empty; with none (c null), as the job left it: the ring whole,
     * the bead calm in its trap, the shift on its chargers if back. The shell and the detector too,
     * if walls (the set; a room has them built once).
     */
    static void live(DrawList d, float f, float time, Cherry c, boolean walls, boolean back) {
        if (walls) {
            shell(d, f, false);
            detector(d, f);
        }
        float hy = f + HOLE_Y;
        int step = c == null ? Cherry.DONE : c.step;
        float t = c == null ? 0f : c.sealT;
        // the light in the pipe: racing while the ring is out of true, faster still as it takes
        // the hole, then steady
        float run;
        if (step < Cherry.SEAL) run = time * 2.4f + 0.6f * (float) Math.sin(time * 1.7f);
        else if (step == Cherry.SEAL) run = time * (2.4f + 3f * Math.min(1f, t / Cherry.DRAWN));
        else run = time * 0.9f;
        ring(d, f, time, run, c, step >= Cherry.DONE);
        // the hole: turning; as the ring takes it, swelling, then drawn in to a bead, a flash
        float s = 1f, spin = time * 3f;
        if (step == Cherry.SEAL) {
            if (t < Cherry.SWELL) s = 1f + 0.35f * t / Cherry.SWELL;
            else if (t < Cherry.DRAWN) {
                float a = (t - Cherry.SWELL) / (Cherry.DRAWN - Cherry.SWELL);
                s = 1.35f + (BEAD - 1.35f) * a * a;
            } else s = BEAD;
            if (t >= Cherry.DRAWN && t < Cherry.DRAWN + 0.5f) flash(d, hy, t - Cherry.DRAWN);
        } else if (step > Cherry.SEAL) {
            s = BEAD;
            spin = time * 0.8f;
        }
        hole(d, hy, spin, s);
        // the jet: from the pole down and out to the floor, going round
        if (c != null && c.jetOn() > 0f && c.reach > 0.5f) {
            float on = c.jetOn();
            float ex = HX + (float) Math.sin(c.jet) * c.reach, ez = DET_Z + (float) Math.cos(c.jet) * c.reach;
            float flick = (0.12f + 0.05f * (float) Math.sin(time * 40f)) * on;
            beam(d, HX, hy, DET_Z, ex, f + 0.03f, ez, flick, 0xB86BFF);
            beam(d, HX, hy, DET_Z, ex, f + 0.03f, ez, flick * 0.4f, 0xF1E0FF);
            // where it meets the floor, a pool of its light
            float pool = (0.7f + 0.15f * (float) Math.sin(time * 23f)) * on;
            d.box(ex, f + 0.02f, ez, pool, 0.02f, pool, null, 0xC48BFF, DrawList.GLOW);
        }
        // the chargers: empty and red while the job is on, going green one by one as the ring
        // takes the hole; after, green, the shift on them if it is back
        for (int k = 0; k < Herd.ALL; k++) {
            float x = rowX(k);
            boolean green = step > Cherry.SEAL || step == Cherry.SEAL && t >= Cherry.LAMPS + k * 0.1f;
            charger(d, x, f + ROW_UP, green ? 0x46D27E : (time * 1.5f + k * 0.21f) % 1f < 0.75f ? 0xFF3A2A : 0x7A1C14);
            if (back && step > Cherry.SEAL)
                Frida.draw(d, x, f + ROW_UP, ROW_Z, 0f, Chase.SIZE, "idle", time + k, false);
        }
    }

    /** Charger k's place along the row: x. */
    private static float rowX(int k) {
        return HX - ROW_HALF + 2f * ROW_HALF * k / (Herd.ALL - 1f);
    }

    /** A charger on the platform (its top at g): two posts, a bar over, the lamp so lit. */
    private static void charger(DrawList d, float x, float g, int lamp) {
        for (int e = -1; e <= 1; e += 2)
            d.box(x + e * 0.8f, g + 1.6f, ROW_Z + 0.6f, 0.08f, 1.6f, 0.08f, null, 0x55585C, DrawList.POLISHED);
        d.box(x, g + 3.2f, ROW_Z + 0.6f, 0.9f, 0.08f, 0.1f, null, 0x55585C, DrawList.POLISHED);
        d.box(x, g + 3.45f, ROW_Z + 0.6f, 0.12f, 0.12f, 0.06f, null, lamp, DrawList.GLOW);
    }

    /**
     * The shell, its floor at f: floor, ceiling, four walls (the left-hand one with the lift's
     * doorway in it, if lift), strips of light, lamps along the walls, the platform along the far
     * wall and the door onto it.
     */
    static void shell(DrawList d, float f, boolean lift) {
        float top = f + HIGH;
        int panel = 0x86A9AC, panelDark = 0x5E7F82;
        d.box(HX, f - 0.2f, HZ, HALF_W, 0.2f, HALF_D, null, 0x4E5A5E, DrawList.POLISHED);
        d.box(HX, top + 0.2f, HZ, HALF_W, 0.2f, HALF_D, null, 0x1E2628, DrawList.PAINT);
        for (int e = -1; e <= 1; e += 2) {
            float wx = HX + e * (HALF_W + 0.2f);
            if (lift && e < 0) {
                // the left-hand wall in three: before the lift, after it, over its doors
                float z0 = HZ - HALF_D, za = z0 + LIFT_V - Indoors.LIFT_HALF, zb = z0 + LIFT_V + Indoors.LIFT_HALF, z1 = HZ + HALF_D;
                float lh = Indoors.LIFT_HIGH;
                d.box(wx, f + HIGH * 0.5f, (z0 + za) * 0.5f, 0.2f, HIGH * 0.5f, (za - z0) * 0.5f, null, panel, DrawList.PAINT);
                d.box(wx, f + HIGH * 0.5f, (zb + z1) * 0.5f, 0.2f, HIGH * 0.5f, (z1 - zb) * 0.5f, null, panel, DrawList.PAINT);
                d.box(wx, f + (lh + HIGH) * 0.5f, z0 + LIFT_V, 0.2f, (HIGH - lh) * 0.5f, Indoors.LIFT_HALF, null, panel, DrawList.PAINT);
            } else d.box(wx, f + HIGH * 0.5f, HZ, 0.2f, HIGH * 0.5f, HALF_D, null, panel, DrawList.PAINT);
            d.box(HX, f + HIGH * 0.5f, HZ + e * (HALF_D + 0.2f), HALF_W, HIGH * 0.5f, 0.2f, null, panel, DrawList.PAINT);
            for (float s = -HALF_D + 2f; s < HALF_D; s += 4f) {
                // the dark panels low on the walls, none over the lift's doorway
                if (lift && e < 0 && Math.abs(HZ + s - (HZ - HALF_D + LIFT_V)) < Indoors.LIFT_HALF + 1.9f) continue;
                d.box(HX + e * (HALF_W - 0.02f), f + 1.2f, HZ + s, 0.02f, 1.2f, 1.8f, null, panelDark, DrawList.PAINT);
            }
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
    }

    /** The detector, a drum on a plinth, its glass window toward the eye; the trap over it, four posts, a ring of coils. */
    static void detector(DrawList d, float f) {
        int magnet = 0x3E7FD8;
        d.box(HX, f + 0.4f, DET_Z, 2.4f, 0.4f, 2.4f, null, 0x55585C, DrawList.POLISHED);
        for (int k = 0; k < 4; k++)
            d.box(HX, f + RING_Y, DET_Z, 1.9f * 0.4142f, 1.2f, 1.9f, yaw(k * 0.7854f), 0x8A949C, DrawList.POLISHED);
        d.box(HX, f + RING_Y, DET_Z - 1.92f, 0.7f, 0.6f, 0.04f, null, 0x24343C, DrawList.GLASS);
        for (int k = 0; k < 4; k++) {
            float a = k * 1.5708f + 0.7854f;
            d.box(HX + (float) Math.sin(a) * 1.5f, f + (RING_Y + 1.2f + HOLE_Y + 1.4f) * 0.5f, DET_Z + (float) Math.cos(a) * 1.5f, 0.08f,
                    (HOLE_Y + 1.4f - RING_Y - 1.2f) * 0.5f, 0.08f, null, 0x55585C, DrawList.POLISHED);
        }
        for (int k = 0; k < 8; k++) {
            float a = k * 0.7854f;
            d.box(HX + (float) Math.sin(a) * 1.5f, f + HOLE_Y + 1.4f, DET_Z + (float) Math.cos(a) * 1.5f, 0.3f, 0.12f, 0.3f, yaw(a), magnet, DrawList.PAINT);
        }
    }

    /**
     * The ring: steel pieces, every other a blue magnet, on stands; light running round in its
     * pipe (so far round by now). With the job on, its knocked magnets hanging askew off their
     * stands, blinking red, or set again and lit green; whole, the six lit green.
     */
    private static void ring(DrawList d, float f, float time, float run, Cherry c, boolean whole) {
        int steel = 0xB2BAC0, magnet = 0x3E7FD8;
        boolean blink = (time * 2.2f) % 1f < 0.55f;
        for (int k = 0; k < RING_PIECES; k++) {
            float a = k * 2f * (float) Math.PI / RING_PIECES, x = HX + (float) Math.sin(a) * RING_R, z = RING_Z + (float) Math.cos(a) * RING_R;
            boolean mag = k % 2 == 0;
            int knocked = knocked(k);
            boolean off = c != null && knocked >= 0 && !c.set[knocked];
            boolean lit = knocked >= 0 && (whole || c != null && c.set[knocked]);
            float y = f + RING_Y;
            if (off) {
                // hanging off its stand, leaning out of the ring, darker
                y -= KNOCK_DROP;
                d.box(x, y, z, 0.75f, 0.75f, (float) Math.PI * RING_R / RING_PIECES + 0.05f, lean(a, KNOCK_LEAN), 0x2E5A96, DrawList.PAINT);
                d.box(x + (float) Math.sin(a) * 0.35f, y + 0.95f, z + (float) Math.cos(a) * 0.35f, 0.14f, 0.1f, 0.14f, null,
                        blink ? 0xFF3A2A : 0x5A1A16, blink ? DrawList.GLOW : DrawList.PAINT);
            } else {
                float[] t = yaw(a + (float) Math.PI * 0.5f);
                d.box(x, y, z, mag ? 0.75f : 0.45f, mag ? 0.75f : 0.45f, (float) Math.PI * RING_R / RING_PIECES + 0.05f, t, mag ? magnet : steel,
                        mag ? DrawList.PAINT : DrawList.POLISHED);
                if (lit) d.box(x, y + 0.85f, z, 0.14f, 0.1f, 0.14f, null, 0x46D27E, DrawList.GLOW);
            }
            d.box(x, f + (RING_Y - 0.75f) * 0.5f, z, 0.25f, (RING_Y - 0.75f) * 0.5f, 0.25f, null, 0x55585C, DrawList.POLISHED);
        }
        // light running round in the pipe
        for (int q = 0; q < 3; q++) {
            float a = run + q * 2.0944f, x = HX + (float) Math.sin(a) * RING_R, z = RING_Z + (float) Math.cos(a) * RING_R;
            d.box(x, f + RING_Y, z, 0.5f, 0.5f, 0.5f, null, 0x9FE8FF, DrawList.GLOW);
        }
    }

    /** Which of the knocked magnets piece k is (its number of Cherry.KNOCKED), or -1. */
    private static int knocked(int k) {
        for (int q = 0; q < Cherry.KNOCKED.length; q++) if (Cherry.KNOCKED[q] == k) return q;
        return -1;
    }

    /** The white of the hole's birth (or its drawing in), so long since it began. */
    private static void flash(DrawList d, float hy, float since) {
        float s = 1f + since * 10f;
        d.box(HX, hy, DET_Z, s, s, s, null, 0xF4E6FF, DrawList.GLOW);
    }

    /** The black hole in its trap at hy, so large: the disc of light round it, tilted toward the eye, turned so far; its dark. */
    private static void hole(DrawList d, float hy, float spin, float s) {
        float hz = DET_Z;
        for (int k = 0; k < 16; k++) {
            float a = k * 0.3927f + spin, r = 0.9f * s;
            d.box(HX + (float) Math.sin(a) * r, hy + (float) Math.cos(a) * r * 0.35f, hz + (float) Math.cos(a) * r * 0.3f, 0.16f * s, 0.06f * s, 0.16f * s, null,
                    k % 3 == 0 ? 0xFFB060 : 0xC48BFF, DrawList.GLOW);
        }
        d.box(HX, hy, hz, 0.35f * s, 0.35f * s, 0.35f * s, yaw(spin / 3f), 0x050308, DrawList.PLAIN);
        d.box(HX, hy, hz, 0.3f * s, 0.3f * s, 0.3f * s, yaw(spin / 3f + 0.6f), 0x050308, DrawList.PLAIN);
    }

    /**
     * What is in her way in the hall, built into its room: the walls (the lift's doorway left
     * open), the ring's pieces, the detector, the platform along the far wall. The room's frame
     * runs across the hall from its right-hand wall to its left, and in from its front.
     */
    static void solids(Indoors in) {
        float z0 = HZ - HALF_D, w = Indoors.WALL, half = HALF_W, deep = 2f * HALF_D, tall = Indoors.FLOOR + HIGH + 1f;
        float lift0 = LIFT_V - Indoors.LIFT_HALF, lift1 = LIFT_V + Indoors.LIFT_HALF;
        in.solid(-half - w, -w, half + w, 0f, tall);
        in.solid(-half - w, deep, half + w, deep + w, tall);
        in.solid(-half - w, 0f, -half, deep, tall);
        in.solid(half, 0f, half + w, lift0, tall);
        in.solid(half, lift1, half + w, deep, tall);
        // the ring: each piece a square round its middle, as high as a magnet's top
        float top = Indoors.FLOOR + RING_Y + 0.75f;
        for (int k = 0; k < RING_PIECES; k++) {
            float u = HX - ringX(k), v = ringZ(k) - z0;
            in.solid(u - 0.8f, v - 0.8f, u + 0.8f, v + 0.8f, top);
        }
        float dv = DET_Z - z0;
        in.solid(-2.4f, dv - 2.4f, 2.4f, dv + 2.4f, Indoors.FLOOR + RING_Y + 1.2f);
        in.solid(-half, ROW_Z - 1.6f - z0, half, deep, Indoors.FLOOR + ROW_UP);
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

    /**
     * A magnet's turn at a out round the ring (its length along the ring), leaning out of it by
     * so much: turned about the ring's line, its top toward the outside.
     */
    private static float[] lean(float a, float by) {
        // along the ring; out of it; up
        float tx = (float) Math.cos(a), tz = -(float) Math.sin(a);
        float ox = (float) Math.sin(a), oz = (float) Math.cos(a);
        float c = (float) Math.cos(by), s = (float) Math.sin(by);
        // its own across (out, tipped down), up (tipped out), along
        float xx = ox * c, xy = -s, xz = oz * c;
        float yx = ox * s, yy = c, yz = oz * s;
        return new float[] {xx, yx, tx, xy, yy, 0f, xz, yz, tz};
    }
}
