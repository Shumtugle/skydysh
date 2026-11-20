package com.shumtugle.skydysh;

/**
 * At the wheel, seen from her own eyes: the eye where her head is in the seat, looking out over
 * the frame of the screen and the bonnet down the street. She is not drawn; only what she would
 * see of herself is: her two fists on the wheel, turning it as she does, and her forearms going
 * down out of the frame. The wheel is round here, and turns.
 *
 * Its own frame is the pickup's: x to its right, y up, its nose along +z; the road at y = 0.
 */
final class Cockpit {
    /** This build drives from her eyes. */
    static final boolean ON = true;

    // her eyes in the seat: across, over the road, along (her head as she sits, a little before it)
    static final float EYE_X = Pickup.SEAT_X, EYE_Y = 2.1f, EYE_Z = 0.3f;
    // looking so far down from level; and wider than the eye behind her: the lens, frame widths
    static final float PITCH = -0.12f, LENS = 1.12f;
    // the head leads into a turn so far for the wheel full over; how fast it follows
    static final float LEAD = 0.14f, LEAD_RATE = 5f;
    // a little give in the neck: braking dips it, pulling away lifts it, so much at most
    static final float NOD = 0.035f, NOD_RATE = 4f;

    // the wheel: its middle, its rim's radius, how far it leans back (radians), how far the rim
    // turns for the wheel full over
    static final float WHEEL_X = Pickup.SEAT_X, WHEEL_Y = 1.3f, WHEEL_Z = 0.72f, RIM = 0.24f, LEANS = 0.45f, TURNS = 2.2f;
    static final int SEGMENTS = 16;
    // her fists on the rim, so far round from its top either way; where her elbows are
    static final float GRIP = 1.05f, ELBOW_OUT = 0.36f, ELBOW_Y = 1.52f, ELBOW_Z = 0.38f;

    private float lead, nod, lastSpeed;
    private boolean placed;

    /** The eye's world place and turn, into out (x, y, z, yaw, pitch). */
    final float[] eye = new float[5];

    /** A frame at the wheel: where her eyes are now, eased in the turn and the pull. */
    void follow(float dt, Pickup p) {
        if (!placed) {
            lead = 0f;
            nod = 0f;
            lastSpeed = p.speed;
            placed = true;
        }
        lead += (p.steer * LEAD - lead) * Math.min(1f, dt * LEAD_RATE);
        float pull = dt > 0f ? (p.speed - lastSpeed) / dt : 0f;
        lastSpeed = p.speed;
        float want = Math.max(-1f, Math.min(1f, pull / Pickup.HARD_BRAKE)) * NOD;
        nod += (want - nod) * Math.min(1f, dt * NOD_RATE);
        place(p);
    }

    /** Off the wheel: next time it starts level. */
    void reset() {
        placed = false;
    }

    void place(Pickup p) {
        float c = (float) Math.cos(p.heading), s = (float) Math.sin(p.heading);
        eye[0] = p.x + c * EYE_X + s * EYE_Z;
        eye[1] = p.y + EYE_Y;
        eye[2] = p.z - s * EYE_X + c * EYE_Z;
        eye[3] = p.heading + lead;
        eye[4] = PITCH + nod;
    }

    // ---- drawing: the wheel and her fists, in the pickup's frame ----

    private static final float[] frame = new float[9], rot = new float[9];
    private static float px, py, pz;

    /** The round wheel turned as the pickup's is, her fists on it and her forearms. */
    static void draw(DrawList d, Pickup p) {
        float cy = (float) Math.cos(p.heading), sy = (float) Math.sin(p.heading);
        px = p.x;
        py = p.y;
        pz = p.z;
        frame[0] = cy; frame[1] = 0f; frame[2] = sy;
        frame[3] = 0f; frame[4] = 1f; frame[5] = 0f;
        frame[6] = -sy; frame[7] = 0f; frame[8] = cy;
        // the wheel's own axes: across, up its face (leaning back toward the nose), out of its face toward her
        float ux = 1f, uy = 0f, uz = 0f;
        float vx = 0f, vy = (float) Math.cos(LEANS), vz = (float) Math.sin(LEANS);
        float nx = 0f, ny = (float) Math.sin(LEANS), nz = -(float) Math.cos(LEANS);
        float turn = p.steer * TURNS;
        // the rim
        float half = RIM * (float) Math.sin(Math.PI / SEGMENTS) * 1.08f;
        for (int k = 0; k < SEGMENTS; k++) {
            float a = turn + k * 2f * (float) Math.PI / SEGMENTS;
            float sa = (float) Math.sin(a), ca = (float) Math.cos(a);
            float rx = sa * ux + ca * vx, ry = sa * uy + ca * vy, rz = sa * uz + ca * vz;
            float tx = ca * ux - sa * vx, ty = ca * uy - sa * vy, tz = ca * uz - sa * vz;
            part(d, WHEEL_X + rx * RIM, WHEEL_Y + ry * RIM, WHEEL_Z + rz * RIM, tx, ty, tz, rx, ry, rz, nx, ny, nz,
                    half, 0.022f, 0.026f, 0x1C1B1D, DrawList.RUBBER);
        }
        // three spokes and the boss
        for (int k = 0; k < 3; k++) {
            float a = turn + (float) Math.PI * (k == 0 ? 1f : k == 1 ? 0.5f : -0.5f);
            float sa = (float) Math.sin(a), ca = (float) Math.cos(a);
            float rx = sa * ux + ca * vx, ry = sa * uy + ca * vy, rz = sa * uz + ca * vz;
            float tx = ca * ux - sa * vx, ty = ca * uy - sa * vy, tz = ca * uz - sa * vz;
            float m = RIM * 0.5f;
            part(d, WHEEL_X + rx * m, WHEEL_Y + ry * m, WHEEL_Z + rz * m, tx, ty, tz, rx, ry, rz, nx, ny, nz,
                    0.018f, m, 0.012f, 0x2E2D30, DrawList.PLAIN);
        }
        part(d, WHEEL_X - nx * 0.01f, WHEEL_Y - ny * 0.01f, WHEEL_Z - nz * 0.01f, ux, uy, uz, vx, vy, vz, nx, ny, nz,
                0.055f, 0.055f, 0.02f, Pickup.CORAL, DrawList.PLAIN);
        // the column down into the dashboard
        part(d, WHEEL_X + nx * -0.12f - vx * 0.05f, WHEEL_Y + ny * -0.12f - vy * 0.05f, WHEEL_Z + nz * -0.12f - vz * 0.05f,
                ux, uy, uz, vx, vy, vz, nx, ny, nz, 0.03f, 0.03f, 0.12f, 0x2E2D30, DrawList.PLAIN);
        // her fists on the rim and her forearms down to her elbows
        for (int e = -1; e <= 1; e += 2) {
            float a = turn + e * GRIP;
            float sa = (float) Math.sin(a), ca = (float) Math.cos(a);
            float rx = sa * ux + ca * vx, ry = sa * uy + ca * vy, rz = sa * uz + ca * vz;
            float tx = ca * ux - sa * vx, ty = ca * uy - sa * vy, tz = ca * uz - sa * vz;
            float fx = WHEEL_X + rx * RIM, fy = WHEEL_Y + ry * RIM, fz = WHEEL_Z + rz * RIM;
            float ex = WHEEL_X + e * ELBOW_OUT, ey = ELBOW_Y, ez = ELBOW_Z;
            // the forearm: brick, from a little behind the fist to the elbow
            float ax = ex - fx, ay = ey - fy, az = ez - fz, len = (float) Math.sqrt(ax * ax + ay * ay + az * az);
            ax /= len;
            ay /= len;
            az /= len;
            // a side for it, square to it and to up
            float sx = -az, sz = ax, sl = (float) Math.sqrt(sx * sx + sz * sz);
            sx /= sl;
            sz /= sl;
            // and the third, square to both (a x s, s level)
            float qx = ay * sz, qy = az * sx - ax * sz, qz = -ay * sx;
            float from = 0.1f, mid = from + (len - from) * 0.5f, armHalf = (len - from) * 0.5f;
            float mx = fx + ax * mid, my = fy + ay * mid, mz = fz + az * mid;
            part(d, mx, my, mz, sx, 0f, sz, qx, qy, qz, ax, ay, az, 0.085f, 0.08f, armHalf, Frida.BRICK, DrawList.GRAIN);
            // a darker course across it, as her bricks are laid
            part(d, mx + ax * armHalf * 0.3f, my + ay * armHalf * 0.3f, mz + az * armHalf * 0.3f, sx, 0f, sz, qx, qy, qz, ax, ay, az,
                    0.088f, 0.083f, 0.03f, Frida.BRICK_DARK, DrawList.GRAIN);
            // the iron cuff at the wrist
            part(d, fx + ax * 0.12f, fy + ay * 0.12f, fz + az * 0.12f, sx, 0f, sz, qx, qy, qz, ax, ay, az,
                    0.092f, 0.087f, 0.018f, 0x3A3B40, DrawList.PLAIN);
            // the fist, stone, round the rim, the thumb over it
            part(d, fx + nx * 0.02f, fy + ny * 0.02f, fz + nz * 0.02f, tx, ty, tz, rx, ry, rz, nx, ny, nz,
                    0.06f, 0.07f, 0.06f, Frida.STONE_DARK, DrawList.STONE);
            part(d, fx + nx * 0.07f + rx * 0.03f, fy + ny * 0.07f + ry * 0.03f, fz + nz * 0.07f + rz * 0.03f, tx, ty, tz, rx, ry, rz, nx, ny, nz,
                    0.05f, 0.025f, 0.02f, Frida.STONE, DrawList.STONE);
        }
    }

    /** A block at a place of the pickup's frame, its own three axes given in that frame. */
    private static void part(DrawList d, float lx, float ly, float lz,
                             float ax, float ay, float az, float bx, float by, float bz, float cx, float cy, float cz,
                             float hx, float hy, float hz, int color, float material) {
        float[] f = frame;
        // columns: each axis carried into the world by the pickup's turn
        for (int r = 0; r < 3; r++) {
            rot[r * 3] = f[r * 3] * ax + f[r * 3 + 1] * ay + f[r * 3 + 2] * az;
            rot[r * 3 + 1] = f[r * 3] * bx + f[r * 3 + 1] * by + f[r * 3 + 2] * bz;
            rot[r * 3 + 2] = f[r * 3] * cx + f[r * 3 + 1] * cy + f[r * 3 + 2] * cz;
        }
        d.box(px + f[0] * lx + f[1] * ly + f[2] * lz, py + f[3] * lx + f[4] * ly + f[5] * lz,
                pz + f[6] * lx + f[7] * ly + f[8] * lz, hx, hy, hz, rot, color, material);
    }
}
