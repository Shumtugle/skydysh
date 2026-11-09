package com.shumtugle.skydysh;

/**
 * Life at the laboratory on the mountain: on the deck the brums' own pickups in their spaces, two
 * brums talking by one, two by the door; one coming out of her pickup and going in at the door,
 * another coming out of the door and going to hers and getting in, over and over. In the lobby its
 * brums: one behind the counter (who holds out the box with the navigator in it, when there is
 * one for her), some standing about in twos and threes, some on the benches, some crossing the
 * hall. And the box itself, and the navigator in it.
 *
 * The brums are slate grey at work, violet and black among them; all as she is made (see Frida).
 */
final class LabTop {
    private LabTop() {
    }

    private static Pickup[] parked;
    private static final float[] at = new float[3], seat = new float[3];
    // the way from the deck to the door: up the middle of the flat, the steps, to the door
    private static final float[][] TO_DOOR = {
        {Mountain.MX - 1.5f, Mountain.MZ - 23f},
        {Mountain.MX - 1f, Mountain.MZ - Mountain.PODIUM - Mountain.STAIRS * Mountain.STAIR_DEEP - 0.5f},
        {Mountain.MX - 0.8f, Mountain.MZ - Mountain.PODIUM + 0.5f},
        {Mountain.MX - 0.6f, Mountain.DOOR_Z - 0.6f},
    };
    static final float WALK = 1.4f, CYCLE = 34f, IN_CAR = 5f;

    /** How high one stands on her way to the door: the deck and the flat, the steps, the podium. */
    private static float ground(float z) {
        float foot = Mountain.MZ - Mountain.PODIUM - Mountain.STAIRS * Mountain.STAIR_DEEP, top = Mountain.MZ - Mountain.PODIUM;
        if (z <= foot) return Mountain.TOP;
        if (z >= top) return Mountain.TOP + Mountain.PODIUM_HIGH;
        return Mountain.TOP + Mountain.PODIUM_HIGH * (z - foot) / (top - foot);
    }

    private static void parked() {
        if (parked != null) return;
        parked = new Pickup[Mountain.TAKEN.length];
        for (int k = 0; k < parked.length; k++) {
            int s = Mountain.TAKEN[k];
            parked[k] = new Pickup(Mountain.spaceX(s), Mountain.spaceZ(s), Mountain.spaceHeading(s), 0x1AB0L + k * 37L);
            parked[k].y = Mountain.TOP;
        }
    }

    /** A point so far along the way from pickup p's door to the laboratory's door, into at (x, y, z); its heading into at too. */
    private static float wayAt(Pickup p, float s, float[] out) {
        p.door(0.4f, seat);
        float px = seat[0], pz = seat[1];
        for (int k = 0; k < TO_DOOR.length; k++) {
            float qx = TO_DOOR[k][0], qz = TO_DOOR[k][1], l = (float) Math.hypot(qx - px, qz - pz);
            if (s <= l || k == TO_DOOR.length - 1) {
                float a = l < 1e-3f ? 1f : Math.min(1f, s / l);
                out[0] = px + (qx - px) * a;
                out[2] = pz + (qz - pz) * a;
                out[1] = ground(out[2]);
                return (float) Math.atan2(qx - px, qz - pz);
            }
            s -= l;
            px = qx;
            pz = qz;
        }
        return 0f;
    }

    private static float wayLong(Pickup p) {
        p.door(0.4f, seat);
        float px = seat[0], pz = seat[1], l = 0f;
        for (float[] q : TO_DOOR) {
            l += (float) Math.hypot(q[0] - px, q[1] - pz);
            px = q[0];
            pz = q[1];
        }
        return l;
    }

    /** The deck near the eye: the pickups, the brums by them, the two going in and out. */
    static void deck(DrawList d, float time, float ex, float ez) {
        float dx = Mountain.MX - ex, dz = Mountain.MZ - ez;
        if (dx * dx + dz * dz > 260f * 260f) return;
        parked();
        for (Pickup p : parked) p.draw(d, time);
        // two talking by the second pickup, turned to each other
        Pickup t = parked[1];
        t.door(1.4f, at);
        float tx = at[0], tz = at[1];
        Frida.slate = true;
        Frida.draw(d, tx, Mountain.TOP, tz - 0.9f, 0f + (float) Math.PI, Chase.SIZE, "idle", time);
        Frida.draw(d, tx, Mountain.TOP, tz + 0.9f, (float) Math.PI + (float) Math.PI, Chase.SIZE, "idle", time + 1.7f);
        Frida.slate = false;
        // two by the door, at either side of it, waiting
        Frida.violet = true;
        Frida.draw(d, Mountain.MX - 3f, Mountain.TOP + Mountain.PODIUM_HIGH, Mountain.DOOR_Z - 1.6f, 0.6f, Chase.SIZE, "idle", time + 0.6f);
        Frida.violet = false;
        Frida.black = true;
        Frida.draw(d, Mountain.MX + 3f, Mountain.TOP + Mountain.PODIUM_HIGH, Mountain.DOOR_Z - 1.4f, -0.5f, Chase.SIZE, "arms_folded", time);
        Frida.black = false;
        // one out of her pickup and in at the door; another out of the door and into hers
        commuter(d, parked[2], time, false);
        commuter(d, parked[4], time + CYCLE * 0.5f, true);
    }

    /**
     * One of them, a cycle of CYCLE seconds: at the wheel of pickup p, then out, along the way to
     * the door, gone in; or (back) the other way round: out of the door, along to p, in.
     */
    private static void commuter(DrawList d, Pickup p, float time, boolean back) {
        float c = time % CYCLE, l = wayLong(p), walk = l / WALK;
        Frida.slate = true;
        if (!back && c < IN_CAR || back && c >= CYCLE - IN_CAR) {
            p.seat(seat);
            Frida.draw(d, seat[0], seat[1] - Runner.SEAT_DROP, seat[2], p.heading + (float) Math.PI, Chase.SIZE, Frida.SIT, 0f);
        } else {
            float s = back ? (c - (CYCLE - IN_CAR - walk)) * WALK : (c - IN_CAR) * WALK;
            if (s >= 0f && s <= l) {
                float h = back ? wayAt(p, l - s, at) + (float) Math.PI : wayAt(p, s, at);
                Frida.draw(d, at[0], at[1], at[2], h + (float) Math.PI, Chase.SIZE, "walk", s / 3.3f * 1.3333f);
            }
        }
        Frida.slate = false;
    }

    // ---- the lobby's brums: where they stand (across, along), the way they face, how ----
    private static final float[][] STAND = {
        {-3.5f, 5f, 0.4f}, {-2.6f, 5.8f, -2.6f},
        {4.5f, 9.5f, 1.9f}, {3.6f, 10.4f, -1.2f}, {5.3f, 10.6f, -2.2f},
        {-4.8f, 12f, 1.1f},
        {8.5f, 17.5f, 3.1f}, {9.3f, 18.6f, -2.4f},
    };
    static final float LOBBY_SIT = Rooms.LOBBY_HALF - 0.75f;
    private static final float[][] SITS = {{-LOBBY_SIT, 3.4f}, {-LOBBY_SIT, 5f}, {LOBBY_SIT, 8.6f}, {LOBBY_SIT, 9.4f}};
    // two crossing the hall, back and forth: from (across, along) to (across, along), how fast
    private static final float[][] CROSS = {{-8.5f, 2.5f, 8.5f, 12f, 1.3f}, {7f, 3f, -5f, 18.5f, 1.1f}};

    /**
     * The lobby's brums. The one at the counter holds the box out before her while it is still
     * hers to give (holds); where her hands are into hands.
     */
    static void lobby(DrawList d, Indoors in, float time, boolean holds, float[] hands) {
        float f = Indoors.FLOOR;
        for (int k = 0; k < STAND.length; k++) {
            float[] s = STAND[k];
            boolean violet = k % 3 == 1, black = k == 5;
            Frida.violet = violet;
            Frida.black = black;
            Frida.slate = !violet && !black;
            Frida.draw(d, in.wx(s[0], s[1]), f, in.wz(s[0], s[1]), in.heading((float) Math.sin(s[2]), (float) Math.cos(s[2])) + (float) Math.PI,
                    Chase.SIZE, k % 4 == 3 ? "arms_folded" : "idle", time + k * 0.9f);
        }
        Frida.violet = Frida.black = false;
        Frida.slate = true;
        for (int k = 0; k < SITS.length; k++) {
            float[] s = SITS[k];
            float face = -Math.signum(s[0]);
            Frida.draw(d, in.wx(s[0], s[1]), f + 0.5f - Runner.SEAT_DROP, in.wz(s[0], s[1]), in.heading(face, 0f) + (float) Math.PI, Chase.SIZE, Frida.SIT, 0f);
        }
        for (int k = 0; k < CROSS.length; k++) {
            float[] c = CROSS[k];
            float l = (float) Math.hypot(c[2] - c[0], c[3] - c[1]), go = l / c[4], cyc = (time * 1f + k * 7f) % (2f * go + 4f);
            float a;
            boolean back;
            if (cyc < go) {
                a = cyc / go;
                back = false;
            } else if (cyc < go + 2f) {
                a = 1f;
                back = true;
            } else if (cyc < 2f * go + 2f) {
                a = 1f - (cyc - go - 2f) / go;
                back = true;
            } else {
                a = 0f;
                back = false;
            }
            float u = c[0] + (c[2] - c[0]) * a, v = c[1] + (c[3] - c[1]) * a;
            float du = (c[2] - c[0]) * (back ? -1f : 1f), dv = (c[3] - c[1]) * (back ? -1f : 1f);
            boolean still = cyc >= go && cyc < go + 2f || cyc >= 2f * go + 2f;
            Frida.draw(d, in.wx(u, v), f, in.wz(u, v), in.heading(du, dv) + (float) Math.PI, Chase.SIZE, still ? "idle" : "walk",
                    still ? time : a * l / 3.3f * 1.3333f);
        }
        // the one behind the counter, facing the hall: holding the box out while it is for her
        float cu = 0f, cv = Rooms.COUNTER_V + 1.1f, yaw = in.heading(0f, -1f) + (float) Math.PI;
        float wx = in.wx(cu, cv), wz = in.wz(cu, cv);
        Frida.draw(d, wx, f, wz, yaw, Chase.SIZE, holds ? Frida.HOLD : "idle", time);
        Frida.slate = false;
        Frida.hands(wx, f, wz, yaw, Chase.SIZE, hands);
    }

    /** The counter's side of the hall, the way the one behind it faces. */
    static float clerkYaw(Indoors in) {
        return in.heading(0f, -1f) + (float) Math.PI;
    }

    // ---- the box and the navigator ----
    static final float BOX_X = 0.34f, BOX_Y = 0.24f, BOX_Z = 0.28f;

    /**
     * The box with the navigator in it: brown card, a turquoise band round it with the name's
     * colour, its lid on, or open so far (0 to 1: its two flaps up and out).
     */
    static void box(DrawList d, float x, float y, float z, float yaw, float open) {
        float[] t = turn(yaw);
        d.box(x, y, z, BOX_X, BOX_Y, BOX_Z, t, 0xB98A55, DrawList.GRAIN);
        d.box(x, y, z, BOX_X + 0.005f, 0.05f, BOX_Z + 0.005f, t, 0x2E9C9A, DrawList.PAINT);
        float c = (float) Math.cos(yaw), s = (float) Math.sin(yaw);
        if (open <= 0f) {
            d.box(x, y + BOX_Y + 0.01f, z, BOX_X, 0.01f, BOX_Z, t, 0xA97A48, DrawList.GRAIN);
            return;
        }
        // the flaps hinged at the long edges, turned up and out
        float a = open * 2.2f;
        for (int e = -1; e <= 1; e += 2) {
            float cu = (float) Math.cos(a), su = (float) Math.sin(a);
            float mx = e * BOX_X + e * cu * BOX_X * 0.5f, my = BOX_Y + su * BOX_X * 0.5f;
            d.box(x + c * mx, y + my, z - s * mx, BOX_X * 0.5f, 0.01f, BOX_Z, flap(yaw, e * a), 0xA97A48, DrawList.GRAIN);
        }
    }

    /** The navigator: a dark slab, its screen lit, the map on it in lines. */
    static void device(DrawList d, float x, float y, float z, float yaw) {
        float[] t = turn(yaw);
        float c = (float) Math.cos(yaw), s = (float) Math.sin(yaw);
        d.box(x, y, z, 0.17f, 0.11f, 0.025f, t, 0x23282E, DrawList.PLAIN);
        d.box(x - s * 0.027f, y, z - c * 0.027f, 0.14f, 0.085f, 0.004f, t, 0x1E4E66, DrawList.GLOW);
        d.box(x - s * 0.031f, y + 0.01f, z - c * 0.031f, 0.1f, 0.006f, 0.002f, t, 0x7FF6E8, DrawList.GLOW);
        d.box(x - s * 0.031f, y - 0.02f, z - c * 0.031f, 0.006f, 0.05f, 0.002f, t, 0x7FF6E8, DrawList.GLOW);
    }

    private static float[] turn(float yaw) {
        float c = (float) Math.cos(yaw), s = (float) Math.sin(yaw);
        return new float[]{c, 0f, s, 0f, 1f, 0f, -s, 0f, c};
    }

    /** A turn about the up axis by yaw, after one about the box's own z by roll. */
    private static float[] flap(float yaw, float roll) {
        float c = (float) Math.cos(yaw), s = (float) Math.sin(yaw), cr = (float) Math.cos(roll), sr = (float) Math.sin(roll);
        // rows of R_yaw * R_roll(z)
        return new float[]{c * cr, -c * sr, s, sr, cr, 0f, -s * cr, s * sr, c};
    }
}
