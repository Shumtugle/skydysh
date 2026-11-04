package com.shumtugle.skydysh;

/**
 * The town's helicopter, made for brums: a cabin wide enough for two of them side by side, open
 * at the sides between its pillars so who sits in it is seen, a glass nose, a long boom and a fin,
 * two skids, four blades. It stands, or it flies where it is sent: the blades run up, it rises to
 * cruising height, turns, goes, slows over where it is to come down, and comes straight down.
 * Come down hard, a skid can give way under it: then it stands leaning, its blades run down.
 */
final class Copter {
    // its sizes: the cabin's half width, its floor and roof over the skids' bottom, its half
    // length; the blades' reach
    static final float HALF_W = 1.4f, FLOOR = 0.75f, ROOF = 3.05f, HALF_L = 1.9f, BLADE = 5.8f;
    // how it flies: its height over the town (over every roof), top speed, how fast it gathers
    // way and loses it, how fast it climbs and comes down, how fast it turns
    static final float CRUISE = 42f, VMAX = 26f, ACCEL = 4.5f, CLIMB = 6f, SINK = 4.2f, TURN = 0.9f;
    /** The blades running up to speed, or down from it, seconds. */
    static final float SPIN_UP = 2.2f, SPIN_DOWN = 5f;

    float x, y, z, heading, pitch, roll;
    /** The blades: their turn, and how fast, 0 (still) to 1. */
    float rotor, spin;
    /** A skid given way: it stands leaning on the other side. */
    boolean broken;
    private float speed, breakT = -1f;

    // where it is sent: (tx, tz), the ground there; whether it is on its way, down there
    private float tx, tz, groundTo;
    private boolean going, down = true;
    private int phase;
    static final int STAND = 0, SPIN = 1, RISE = 2, CROSS = 3, DESCEND = 4, LANDED = 5;

    Copter(float x, float y, float z, float heading) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.heading = heading;
    }

    /** Sent to (x, z), to come down on ground at gy there, a skid giving way as it touches if it is to. */
    void send(float x, float z, float gy, boolean breaks) {
        tx = x;
        tz = z;
        groundTo = gy;
        going = true;
        down = false;
        phase = SPIN;
        breakOnLanding = breaks;
    }

    private boolean breakOnLanding;

    /** Whether it has come down where it was sent. */
    boolean landed() {
        return phase == LANDED;
    }

    /** Whether it is in the air, or its blades running to lift. */
    boolean flying() {
        return phase >= SPIN && phase <= DESCEND;
    }

    /** A step on; true at the moment a skid gives way. */
    boolean update(float dt) {
        boolean cracked = false;
        rotor += dt * 26f * spin;
        switch (phase) {
            case STAND:
            case LANDED:
                spin = Math.max(0f, spin - dt / SPIN_DOWN);
                break;
            case SPIN:
                spin = Math.min(1f, spin + dt / SPIN_UP);
                if (spin >= 1f) phase = RISE;
                break;
            case RISE: {
                float want = CRUISE;
                y = Math.min(want, y + CLIMB * dt);
                // turned toward where it goes while it climbs
                turnTo(dt);
                if (y >= want - 0.5f && Math.abs(off()) < 0.15f) phase = CROSS;
                break;
            }
            case CROSS: {
                float dx = tx - x, dz = tz - z, far = (float) Math.sqrt(dx * dx + dz * dz);
                turnTo(dt);
                // gathering way, and losing it in time to stand over the place
                float can = (float) Math.sqrt(2f * ACCEL * 0.8f * Math.max(0f, far - 1f));
                float want = Math.min(VMAX, can);
                speed += Math.max(-ACCEL * dt, Math.min(ACCEL * dt, want - speed));
                float step = Math.min(far, speed * dt);
                if (far > 1e-3f) {
                    x += dx / far * step;
                    z += dz / far * step;
                }
                // the nose down as it gathers way, up as it loses it
                float lean = speed / VMAX * 0.16f - Math.max(0f, speed - want) * 0.02f;
                pitch += (lean - pitch) * Math.min(1f, dt * 2f);
                y += (CRUISE - y) * Math.min(1f, dt * 0.8f);
                if (far < 2.5f && speed < 2f) {
                    // over the place: the last of the way taken up as it starts down
                    speed = 0f;
                    phase = DESCEND;
                }
                break;
            }
            case DESCEND: {
                pitch += (0f - pitch) * Math.min(1f, dt * 2f);
                x += (tx - x) * Math.min(1f, dt * 1.5f);
                z += (tz - z) * Math.min(1f, dt * 1.5f);
                float left = y - groundTo;
                // slower the last few metres
                float v = left > 8f ? SINK : Math.max(1.1f, SINK * left / 8f);
                y -= v * dt;
                if (y <= groundTo) {
                    y = groundTo;
                    phase = LANDED;
                    going = false;
                    down = true;
                    if (breakOnLanding) {
                        broken = true;
                        breakT = 0f;
                        cracked = true;
                    }
                }
                break;
            }
            default:
                break;
        }
        if (broken && breakT >= 0f) {
            // the skid gone on the right: down onto that side
            breakT += dt;
            float u = Math.min(1f, breakT / 0.35f);
            roll = LEAN * (1f - (1f - u) * (1f - u));
        }
        return cracked;
    }

    /** How far it leans when a skid has given way. */
    static final float LEAN = 0.2f;

    /** The turn left to face where it goes. */
    private float off() {
        float want = (float) Math.atan2(tx - x, tz - z);
        float d = want - heading;
        while (d > Math.PI) d -= 2f * (float) Math.PI;
        while (d < -Math.PI) d += 2f * (float) Math.PI;
        return d;
    }

    private void turnTo(float dt) {
        float dx = tx - x, dz = tz - z;
        if (dx * dx + dz * dz < 4f) return;
        float d = off(), step = TURN * dt;
        heading += Math.max(-step, Math.min(step, d));
        // banked into the turn
        float bank = Math.max(-0.25f, Math.min(0.25f, d * 0.4f));
        if (!broken) roll += (bank - roll) * Math.min(1f, dt * 2f);
    }

    /** The helicopter at rest where it stands now, its blades still, whole again (mended). */
    void mend() {
        broken = false;
        roll = 0f;
        breakT = -1f;
    }

    // ---- its frame: right, up, ahead, as the columns of a turn ----
    private final float[] frame = new float[9], part = new float[9], spinT = new float[9];

    private void frame() {
        float ch = (float) Math.cos(heading), sh = (float) Math.sin(heading);
        float cp = (float) Math.cos(pitch), sp = (float) Math.sin(pitch);
        float cr = (float) Math.cos(roll), sr = (float) Math.sin(roll);
        // ahead, the nose down by pitch
        float fx = sh * cp, fy = -sp, fz = ch * cp;
        // up before the roll, tipped forward with the nose
        float ux = sh * sp, uy = cp, uz = ch * sp;
        // right, level
        float rx = ch, ry = 0f, rz = -sh;
        // the roll about ahead: right goes down on the right
        float r2x = rx * cr - ux * sr, r2y = ry * cr - uy * sr, r2z = rz * cr - uz * sr;
        float u2x = ux * cr + rx * sr, u2y = uy * cr + ry * sr, u2z = uz * cr + rz * sr;
        frame[0] = r2x; frame[1] = u2x; frame[2] = fx;
        frame[3] = r2y; frame[4] = u2y; frame[5] = fy;
        frame[6] = r2z; frame[7] = u2z; frame[8] = fz;
    }

    /** A point of its own frame (right, up, ahead) into the town's, into out. */
    void point(float lx, float ly, float lz, float[] out) {
        frame();
        float py = y + (broken ? -HALF_W * (float) Math.sin(roll) * 0.5f : 0f);
        out[0] = x + frame[0] * lx + frame[1] * ly + frame[2] * lz;
        out[1] = py + frame[3] * lx + frame[4] * ly + frame[5] * lz;
        out[2] = z + frame[6] * lx + frame[7] * ly + frame[8] * lz;
    }

    /** The seat of the pilot (left) or the one beside (right): its top's middle, into out. */
    void seat(boolean pilot, float[] out) {
        point(pilot ? -0.68f : 0.68f, FLOOR + 0.55f, 0.5f, out);
    }

    /** Where one gets in and out: beside the right-hand door, a step off it (x, z into out). */
    void door(float[] out) {
        point(HALF_W + 1.2f, 0f, 0.4f, at);
        out[0] = at[0];
        out[1] = at[2];
    }

    private final float[] at = new float[3];

    private void box(DrawList d, float lx, float ly, float lz, float hx, float hy, float hz, int color, int material) {
        point(lx, ly, lz, at);
        d.box(at[0], at[1], at[2], hx, hy, hz, frame, color, material);
    }

    /** A box turned about its own upright by a first (the blades), then with the frame. */
    private void spun(DrawList d, float lx, float ly, float lz, float hx, float hy, float hz, float a, int color) {
        point(lx, ly, lz, at);
        float c = (float) Math.cos(a), s = (float) Math.sin(a);
        part[0] = c; part[1] = 0f; part[2] = s;
        part[3] = 0f; part[4] = 1f; part[5] = 0f;
        part[6] = -s; part[7] = 0f; part[8] = c;
        mul(frame, part, spinT);
        d.box(at[0], at[1], at[2], hx, hy, hz, spinT, color, DrawList.PLAIN);
    }

    private static void mul(float[] a, float[] b, float[] out) {
        for (int r = 0; r < 3; r++)
            for (int c = 0; c < 3; c++)
                out[r * 3 + c] = a[r * 3] * b[c] + a[r * 3 + 1] * b[3 + c] + a[r * 3 + 2] * b[6 + c];
    }

    static final int BODY = 0x26446A, STRIPE = 0xF2EAD6, DARK = 0x1B2333, GLASS = 0x9FC4D8, FIN = 0xD8562E;

    /** The helicopter: the cabin's floor and its low sides, the pillars and roof, the glass nose, the boom, the fin, the skids, the blades. */
    void draw(DrawList d) {
        frame();
        float f = FLOOR, top = ROOF, w = HALF_W, l = HALF_L;
        // the floor and the low sides, the stripe along them
        box(d, 0f, f - 0.12f, 0f, w, 0.18f, l + 0.2f, BODY, DrawList.PAINT);
        for (int e = -1; e <= 1; e += 2) {
            box(d, e * (w - 0.06f), f + 0.4f, 0f, 0.07f, 0.38f, l, BODY, DrawList.PAINT);
            box(d, e * (w - 0.02f), f + 0.62f, 0f, 0.05f, 0.06f, l, STRIPE, DrawList.PAINT);
            // the pillars at the front and back corners, and between
            for (int k = -1; k <= 1; k++)
                box(d, e * (w - 0.08f), (f + 0.8f + top) * 0.5f, k * (l - 0.1f), 0.07f, (top - f - 0.8f) * 0.5f, 0.07f, BODY, DrawList.PAINT);
        }
        // the back wall, the roof, the engine over it
        box(d, 0f, (f + top) * 0.5f, -l, w, (top - f) * 0.5f, 0.08f, BODY, DrawList.PAINT);
        box(d, 0f, top + 0.08f, -0.1f, w, 0.1f, l + 0.1f, BODY, DrawList.PAINT);
        box(d, 0f, top + 0.4f, -0.6f, 0.7f, 0.25f, 1.1f, DARK, DrawList.PAINT);
        // the nose: a low glass front, the panel before the seats
        box(d, 0f, f + 0.55f, l + 0.35f, w - 0.15f, 0.5f, 0.35f, GLASS, DrawList.GLASS);
        box(d, 0f, f + 1.15f, l + 0.1f, w - 0.2f, 0.12f, 0.2f, DARK, DrawList.PLAIN);
        box(d, 0f, top - 0.2f, l + 0.05f, w - 0.1f, 0.12f, 0.12f, BODY, DrawList.PAINT);
        // the seats
        for (int e = -1; e <= 1; e += 2) {
            box(d, e * 0.68f, f + 0.3f, 0.4f, 0.5f, 0.25f, 0.45f, 0x3A3A40, DrawList.PLAIN);
            box(d, e * 0.68f, f + 1.1f, -0.15f, 0.5f, 0.6f, 0.1f, 0x3A3A40, DrawList.PLAIN);
        }
        // the boom, the fin, the little blades at its end
        box(d, 0f, top - 0.4f, -l - 2.6f, 0.26f, 0.26f, 2.6f, BODY, DrawList.PAINT);
        box(d, 0f, top + 0.35f, -l - 5f, 0.08f, 0.85f, 0.45f, FIN, DrawList.PAINT);
        box(d, 0f, top - 0.4f, -l - 4.7f, 1f, 0.05f, 0.3f, BODY, DrawList.PAINT);
        spun(d, 0.2f, top + 0.35f, -l - 5f, 0.02f, 0.04f, 0.7f, 0f, DARK);
        // the skids, and the struts down to them; the right one gone under it if broken
        for (int e = -1; e <= 1; e += 2) {
            boolean gone = broken && e > 0;
            float sy = gone ? 0.35f : 0.06f;
            box(d, e * (w - 0.1f), sy, 0.2f, 0.07f, 0.07f, l + 0.7f, DARK, DrawList.PLAIN);
            for (int k = -1; k <= 1; k += 2) {
                if (gone && k > 0) continue;
                box(d, e * (w - 0.15f), (sy + f - 0.2f) * 0.5f, 0.2f + k * 1.1f, 0.05f, (f - 0.2f - sy) * 0.5f, 0.05f, DARK, DrawList.PLAIN);
            }
        }
        // the mast and the blades: four (two across), going round; a shadow of them when they run fast
        box(d, 0f, top + 0.75f, -0.1f, 0.13f, 0.2f, 0.13f, DARK, DrawList.PLAIN);
        for (int b = 0; b < 2; b++)
            spun(d, 0f, top + 0.97f, -0.1f, 0.15f, 0.025f, BLADE, rotor + b * (float) (Math.PI / 2), DARK);
        if (spin > 0.6f) {
            for (int b = 0; b < 2; b++)
                spun(d, 0f, top + 0.96f, -0.1f, 0.15f, 0.02f, BLADE, rotor + 0.45f + b * (float) (Math.PI / 2), 0x3A4252);
        }
    }
}
