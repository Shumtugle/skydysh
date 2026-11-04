package com.shumtugle.skydysh;

/**
 * The town's helicopter, made for brums: a broad cabin with a rounded nose, two engine housings
 * on its roof, a tapering boom, a fin and a tail rotor, a wide tailplane, two skids on struts,
 * four blades. Painted from five flat views, as the cars are (the seventh skin of the cars'
 * atlas, see DrawList.skin): cream above, teal below, a coral stripe, the town's shield on the
 * boom.
 *
 * It stands, or it flies where it is sent: the blades run up, it rises over the roofs, turns to
 * its way, and goes in a long arc over the town, higher at the arc's middle, banked into it; it
 * slows over where it is to come down, and comes straight down. Come down onto a weak skid, the
 * skid gives way: it drops onto that side with a jolt, rocks, and stands leaning. Mended, it can
 * be flown by hand, on two sticks (see pilot).
 */
final class Copter {
    /** Its painted skin among the cars' (see DrawList.skin). */
    static final int SKIN = 6;
    // its sizes, in its own frame (x right, y up from the skids' bottom, z ahead from the mast):
    // the cabin's half width, its floor and roof, the blades' reach
    static final float HALF_W = 1.21f, FLOOR = 0.58f, ROOF = 3.08f, BLADE = 5f;
    // how it flies: its height over the town (over every roof), how much higher at the arc's
    // middle, how far the arc bows out to the side for its length; top speed, how fast it gathers
    // way and loses it, how fast it climbs and comes down, how fast it turns
    static final float CRUISE = 42f, ARC_UP = 14f, ARC_BOW = 0.32f, VMAX = 26f, ACCEL = 4.5f, CLIMB = 6f, SINK = 4.2f, TURN = 0.9f;
    /** The blades running up to speed, or down from it, seconds. */
    static final float SPIN_UP = 2.2f, SPIN_DOWN = 5f;

    float x, y, z, heading, pitch, roll;
    /** The blades: their turn, and how fast, 0 (still) to 1. */
    float rotor, spin;
    /** A skid given way: it stands leaning on the other side. */
    boolean broken;
    private float speed, breakT = -1f, hop;

    // the way it is sent: from (sx, sz) to (tx, tz) by (cx, cz), the ground at the end; so far along it (0 to 1)
    private float sx, sz, cx, cz, tx, tz, groundTo, along, length;
    private boolean breakOnLanding;
    private int phase;
    static final int STAND = 0, SPIN = 1, RISE = 2, CROSS = 3, DESCEND = 4, LANDED = 5;
    /** The way it is going now, as STAND to HAND. */
    int phase() {
        return phase;
    }

    Copter(float x, float y, float z, float heading) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.heading = heading;
    }

    /** Sent to (x, z), to come down on ground at gy there, a skid giving way as it touches if it is to; bowing to the right if bow &gt; 0. */
    void send(float x, float z, float gy, boolean breaks, float bow) {
        tx = x;
        tz = z;
        groundTo = gy;
        sx = this.x;
        sz = this.z;
        float dx = tx - sx, dz = tz - sz, d = (float) Math.sqrt(dx * dx + dz * dz);
        // the arc's middle pulled out to one side of the straight way
        cx = (sx + tx) * 0.5f + dz / Math.max(1f, d) * d * ARC_BOW * bow;
        cz = (sz + tz) * 0.5f - dx / Math.max(1f, d) * d * ARC_BOW * bow;
        length = 0f;
        float px = sx, pz = sz;
        for (int k = 1; k <= 32; k++) {
            float u = k / 32f;
            float qx = curve(sx, cx, tx, u), qz = curve(sz, cz, tz, u);
            length += (float) Math.hypot(qx - px, qz - pz);
            px = qx;
            pz = qz;
        }
        along = 0f;
        phase = SPIN;
        breakOnLanding = breaks;
    }

    private static float curve(float a, float b, float c, float u) {
        float v = 1f - u;
        return v * v * a + 2f * v * u * b + u * u * c;
    }

    private static float slope(float a, float b, float c, float u) {
        return 2f * (1f - u) * (b - a) + 2f * u * (c - b);
    }

    /** Whether it has come down where it was sent. */
    boolean landed() {
        return phase == LANDED;
    }

    /** Whether it is in the air, or its blades running to lift. */
    boolean flying() {
        return phase >= SPIN && phase <= DESCEND;
    }

    /** The way its arc goes at u, as a heading. */
    private float way(float u) {
        return (float) Math.atan2(slope(sx, cx, tx, u), slope(sz, cz, tz, u));
    }

    /** A step on; true at the moment a skid gives way. */
    boolean update(float dt) {
        boolean cracked = false;
        rotor += dt * 26f * spin;
        switch (phase) {
            case STAND:
            case LANDED:
                spin = Math.max(0f, spin - dt / SPIN_DOWN);
                pitch += (0f - pitch) * Math.min(1f, dt * 3f);
                if (!broken) roll += (0f - roll) * Math.min(1f, dt * 3f);
                if (settle >= 0f) {
                    // mended: righted with a little hop
                    settle += dt;
                    hop = 0.22f * (float) Math.exp(-settle * 4f) * Math.abs((float) Math.sin(settle * 8f));
                    if (settle > 2f) {
                        settle = -1f;
                        hop = 0f;
                    }
                }
                break;
            case SPIN:
                spin = Math.min(1f, spin + dt / SPIN_UP);
                if (spin >= 1f) phase = RISE;
                break;
            case RISE: {
                // straight up over the roofs, turning to the arc's first way as it goes
                y = Math.min(CRUISE, y + CLIMB * dt);
                float d = wrap(way(0f) - heading), step = TURN * dt;
                heading += Math.max(-step, Math.min(step, d));
                if (y >= CRUISE - 0.5f && Math.abs(d) < 0.1f) phase = CROSS;
                break;
            }
            case CROSS: {
                // along the arc: gathering way, and losing it in time to stand over the place
                float left = (1f - along) * length;
                float want = Math.min(VMAX, (float) Math.sqrt(2f * ACCEL * 0.8f * Math.max(0f, left - 1f)));
                speed += Math.max(-ACCEL * dt, Math.min(ACCEL * dt, want - speed));
                float sl = (float) Math.hypot(slope(sx, cx, tx, along), slope(sz, cz, tz, along));
                along = Math.min(1f, along + speed * dt / Math.max(1f, sl));
                x = curve(sx, cx, tx, along);
                z = curve(sz, cz, tz, along);
                float was = heading, h = way(along);
                heading = was + wrap(h - was) * Math.min(1f, dt * 4f);
                // banked into the arc, the nose down as it gathers way and up as it loses it
                float bank = Math.max(-0.3f, Math.min(0.3f, wrap(heading - was) / Math.max(dt, 1e-3f) * 0.35f));
                roll += (bank - roll) * Math.min(1f, dt * 2f);
                float lean = speed / VMAX * 0.16f - Math.max(0f, speed - want) * 0.03f;
                pitch += (lean - pitch) * Math.min(1f, dt * 2f);
                // higher over the arc's middle
                y = CRUISE + ARC_UP * (float) Math.sin(Math.PI * along);
                if (along >= 0.999f || left < 2f && speed < 2f) {
                    speed = 0f;
                    phase = DESCEND;
                }
                break;
            }
            case DESCEND: {
                pitch += (0f - pitch) * Math.min(1f, dt * 2f);
                roll += (0f - roll) * Math.min(1f, dt * 2f);
                x += (tx - x) * Math.min(1f, dt * 1.5f);
                z += (tz - z) * Math.min(1f, dt * 1.5f);
                float h = y - groundTo;
                // slower the last few metres
                float v = h > 8f ? SINK : Math.max(1.1f, SINK * h / 8f);
                y -= v * dt;
                if (y <= groundTo) {
                    y = groundTo;
                    phase = LANDED;
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
            // the skid gives way: down onto the right with a jolt, a hop, a rock or two, and still
            breakT += dt;
            float u = breakT;
            roll = LEAN * (1f - (float) Math.exp(-u * 9f)) * (1f + 0.3f * (float) Math.exp(-u * 3.5f) * (float) Math.sin(u * 13f));
            hop = 0.28f * (float) Math.exp(-u * 5f) * Math.abs((float) Math.sin(u * 9f));
        }
        return cracked;
    }

    private static float wrap(float a) {
        while (a > Math.PI) a -= 2f * (float) Math.PI;
        while (a < -Math.PI) a += 2f * (float) Math.PI;
        return a;
    }

    /** How far it leans when a skid has given way. */
    static final float LEAN = 0.2f;

    /** The helicopter whole again (mended), at rest where it stands: it rights itself with a little hop. */
    void mend() {
        broken = false;
        breakT = -1f;
        // the body eased level, the left skid's foot let go of as it comes (see update and point)
        settle = 0f;
    }

    /** Put down whole where it was left (a game kept): standing, its blades still. */
    void standAt(float x, float y, float z, float heading, boolean broken) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.heading = heading;
        this.broken = broken;
        roll = broken ? LEAN : 0f;
        pitch = 0f;
        hop = 0f;
        breakT = -1f;
        spin = 0f;
        phase = STAND;
        vx = vz = vy = turnRate = 0f;
        grounded = true;
    }

    // ---- flown by hand: two sticks, the left for height and turning, the right for the way ----
    // top speed ahead, backing, aside; how fast it gathers and loses way; climbing, sinking, how
    // fast either is taken up; turning; the highest it goes
    static final float HAND_AHEAD = 30f, HAND_BACK = 10f, HAND_ASIDE = 12f, HAND_ACCEL = 7f,
            HAND_CLIMB = 8f, HAND_SINK = 6f, HAND_LIFT = 10f, HAND_TURN = 1.3f, HAND_TOP = 140f;
    /** At the controls, flown by hand. */
    static final int HAND = 6;
    /** How it moves when flown by hand (the town's x and z, up), and turns. */
    float vx, vz, vy, turnRate;
    /** Down on something it can stand on (not the sea). */
    boolean grounded = true;
    private float settle = -1f;

    /** Taken in hand: the blades run up, and it answers the sticks. */
    void takeControls() {
        phase = HAND;
        speed = 0f;
        vx = vz = vy = turnRate = 0f;
    }

    /** Let go of, standing: its blades run down. */
    void leave() {
        phase = STAND;
        vx = vz = vy = turnRate = 0f;
    }

    boolean inHand() {
        return phase == HAND;
    }

    /**
     * A frame at the controls: lift (up the left stick, -1 to 1), turn (aside on it), ahead (up the
     * right stick) and aside (aside on it). It climbs and sinks as the left says and holds its height
     * let be; it turns as fast as the left is over; it goes ahead, back and aside as the right says,
     * gathering and losing way as a helicopter does. It stands on whatever is under it, the street or
     * a roof, and is stopped by what stands higher than it: it cannot fly into a house. Over the sea
     * it will not come down onto the water.
     */
    void pilot(float dt, float lift, float turn, float ahead, float aside, City city) {
        spin = Math.min(1f, spin + dt / SPIN_UP);
        rotor += dt * 26f * spin;
        // the blades not up to speed yet: it stays down
        if (spin < 1f) lift = Math.min(lift, 0f);
        float want = lift > 0f ? lift * HAND_CLIMB : lift * HAND_SINK;
        vy += Math.max(-HAND_LIFT * dt, Math.min(HAND_LIFT * dt, want - vy));
        boolean down = grounded && vy <= 0.05f;
        float fx = (float) Math.sin(heading), fz = (float) Math.cos(heading), rx = fz, rz = -fx;
        float tvx = 0f, tvz = 0f;
        if (!down) {
            float a = ahead >= 0f ? ahead * HAND_AHEAD : ahead * HAND_BACK, s = aside * HAND_ASIDE;
            tvx = fx * a + rx * s;
            tvz = fz * a + rz * s;
            turnRate += (turn * HAND_TURN - turnRate) * Math.min(1f, dt * 5f);
        } else turnRate = 0f;
        float ax = tvx - vx, az = tvz - vz, am = (float) Math.sqrt(ax * ax + az * az), step = (down ? 30f : HAND_ACCEL) * dt;
        if (am > step) {
            ax *= step / am;
            az *= step / am;
        }
        vx += ax;
        vz += az;
        heading += turnRate * dt;
        // along the town's x, then its z: stopped by what stands higher than it is
        float nx = x + vx * dt, nz = z + vz * dt;
        if (blocked(nx, z, city)) vx *= -0.2f;
        else x = nx;
        if (blocked(x, nz, city)) vz *= -0.2f;
        else z = nz;
        y = Math.min(HAND_TOP, y + vy * dt);
        float floor = floor(city);
        boolean sea = City.overWater(x, z);
        if (y <= floor) {
            y = floor;
            if (vy < 0f) vy = 0f;
            // down only with both skids on it: a nose or a tail over a roof's edge holds it up, not down
            grounded = !sea && level(city, floor);
        } else if (y > floor + 0.05f) grounded = false;
        if (grounded) {
            vx = vz = 0f;
        }
        // its lean: the nose down as it goes ahead, the side down as it goes aside and turns
        float goAhead = vx * fx + vz * fz, goAside = vx * rx + vz * rz;
        float p = grounded ? 0f : goAhead / HAND_AHEAD * 0.2f + (tvx * fx + tvz * fz - goAhead) * 0.012f;
        float r = grounded ? 0f : Math.max(-0.35f, Math.min(0.35f, goAside / HAND_ASIDE * 0.16f + turnRate * 0.18f));
        pitch += (p - pitch) * Math.min(1f, dt * 3f);
        roll += (r - roll) * Math.min(1f, dt * 3f);
    }

    // the points of its body tried against the town: its middle, its sides, its nose, its tail
    private static final float[] FOOT = {0f, 0f, -1.7f, 0f, 1.7f, 0f, 0f, 3.9f, 0f, -4.9f};

    /** The highest thing under it, of the town's, that it could stand on (no higher than a step over its skids). */
    private float floor(City city) {
        float fx = (float) Math.sin(heading), fz = (float) Math.cos(heading), g = -1e9f;
        for (int k = 0; k < FOOT.length; k += 2) {
            float px = x + fz * FOOT[k] + fx * FOOT[k + 1], pz = z - fx * FOOT[k] + fz * FOOT[k + 1];
            g = Math.max(g, City.overWater(px, pz) ? City.WATER + 0.6f : city.ground(px, pz, y, 0.6f));
        }
        return g;
    }

    /** Whether its middle and both its sides stand on ground at floor (or nearly). */
    private boolean level(City city, float floor) {
        float fx = (float) Math.sin(heading), fz = (float) Math.cos(heading);
        for (int k = 0; k < 6; k += 2) {
            float px = x + fz * FOOT[k] + fx * FOOT[k + 1], pz = z - fx * FOOT[k] + fz * FOOT[k + 1];
            if (City.overWater(px, pz) || city.ground(px, pz, y, 0.6f) < floor - 0.4f) return false;
        }
        return true;
    }

    /** Whether at (nx, nz) something of the town stands higher than its skids (and a step). */
    private boolean blocked(float nx, float nz, City city) {
        float fx = (float) Math.sin(heading), fz = (float) Math.cos(heading);
        for (int k = 0; k < FOOT.length; k += 2) {
            float px = nx + fz * FOOT[k] + fx * FOOT[k + 1], pz = nz - fx * FOOT[k] + fz * FOOT[k + 1];
            if (City.overWater(px, pz)) continue;
            if (city.ground(px, pz, 1e4f, 0f) > y + 0.6f) return true;
        }
        return false;
    }

    // ---- its frame: right, up, ahead, as the columns of a turn ----
    private final float[] frame = new float[9], part = new float[9], spinT = new float[9], at = new float[3];
    // the skids' half span: the left one stays on the ground when it leans
    static final float SKID_X = 1.72f;

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
        // the roll about ahead: the right side goes down
        float r2x = rx * cr - ux * sr, r2y = ry * cr - uy * sr, r2z = rz * cr - uz * sr;
        float u2x = ux * cr + rx * sr, u2y = uy * cr + ry * sr, u2z = uz * cr + rz * sr;
        frame[0] = r2x; frame[1] = u2x; frame[2] = fx;
        frame[3] = r2y; frame[4] = u2y; frame[5] = fy;
        frame[6] = r2z; frame[7] = u2z; frame[8] = fz;
    }

    /** A point of its own frame (right, up, ahead) into the town's, into out. */
    void point(float lx, float ly, float lz, float[] out) {
        frame();
        // leaning on a broken skid, the other skid stays on the ground
        float py = y + hop - (broken || settle >= 0f ? SKID_X * (float) Math.sin(roll) : 0f);
        out[0] = x + frame[0] * lx + frame[1] * ly + frame[2] * lz;
        out[1] = py + frame[3] * lx + frame[4] * ly + frame[5] * lz;
        out[2] = z + frame[6] * lx + frame[7] * ly + frame[8] * lz;
    }

    /** The seat of the pilot (left) or the one beside (right): its top's middle, into out. */
    void seat(boolean pilot, float[] out) {
        point(pilot ? -0.6f : 0.6f, FLOOR + 0.55f, 1.5f, out);
    }

    /** Where one gets in and out: beside the right-hand door, a step off it (x, z into out). */
    void door(float[] out) {
        point(HALF_W + 1.6f, 0f, 0.6f, at);
        out[0] = at[0];
        out[1] = at[2];
    }

    /** A box of its body, painted from the views. */
    private void skinned(DrawList d, float lx, float ly, float lz, float hx, float hy, float hz, int tone) {
        point(lx, ly, lz, at);
        d.box(at[0], at[1], at[2], hx, hy, hz, frame, tone, DrawList.PLAIN);
        d.skin(SKIN, lx, ly, lz, tone);
    }

    private void plain(DrawList d, float lx, float ly, float lz, float hx, float hy, float hz, int color, int material) {
        point(lx, ly, lz, at);
        d.box(at[0], at[1], at[2], hx, hy, hz, frame, color, material);
    }

    /** A box turned about the frame's upright (a about y) or its right (about x) by a first, then with the frame. */
    private void spun(DrawList d, float lx, float ly, float lz, float hx, float hy, float hz, float a, boolean aboutX, int color, int material) {
        point(lx, ly, lz, at);
        float c = (float) Math.cos(a), s = (float) Math.sin(a);
        if (aboutX) {
            part[0] = 1f; part[1] = 0f; part[2] = 0f;
            part[3] = 0f; part[4] = c; part[5] = -s;
            part[6] = 0f; part[7] = s; part[8] = c;
        } else {
            part[0] = c; part[1] = 0f; part[2] = s;
            part[3] = 0f; part[4] = 1f; part[5] = 0f;
            part[6] = -s; part[7] = 0f; part[8] = c;
        }
        mul(frame, part, spinT);
        d.box(at[0], at[1], at[2], hx, hy, hz, spinT, color, material);
    }

    private static void mul(float[] a, float[] b, float[] out) {
        for (int r = 0; r < 3; r++)
            for (int c = 0; c < 3; c++)
                out[r * 3 + c] = a[r * 3] * b[c] + a[r * 3 + 1] * b[3 + c] + a[r * 3 + 2] * b[6 + c];
    }

    static final int CREAM = 0xE6DCC6, TEAL = 0x2E6A64, DARK = 0x3A3E44, BLADES = 0x4A4F55, TIPS = 0xE8B828;

    void draw(DrawList d) {
        frame();
        // the body: the cabin, the nose, the housings on the roof, the back narrowing to the boom
        skinned(d, 0f, 1.83f, 0.575f, HALF_W, 1.25f, 2.825f, CREAM);
        skinned(d, 0f, 1.71f, 3.82f, 1f, 0.99f, 0.42f, CREAM);
        skinned(d, 0f, 3.36f, 0.25f, 0.85f, 0.28f, 1.2f, CREAM);
        skinned(d, 0f, 2.225f, -2.675f, 0.75f, 0.825f, 0.425f, TEAL);
        // the boom, the fin, the tailplane
        skinned(d, 0f, 2.5f, -3.55f, 0.35f, 0.35f, 0.45f, TEAL);
        skinned(d, 0f, 3.22f, -4.47f, 0.1f, 1.12f, 0.47f, TEAL);
        skinned(d, 0f, 2.5f, -4.47f, 1.5f, 0.06f, 0.28f, TEAL);
        // the beacon on the roof
        plain(d, 0f, 3.72f, -0.8f, 0.1f, 0.08f, 0.1f, 0xFF9A2A, DrawList.GLOW);
        // the skids, the struts down to them; the right ones pushed up under it if it gave way
        for (int e = -1; e <= 1; e += 2) {
            boolean gone = broken && e > 0;
            float sy = gone ? 0.74f : 0.12f;
            skinned(d, e * SKID_X, sy, 0.565f, 0.1f, 0.12f, 2.7f, DARK);
            for (int k = 0; k < 2; k++) {
                float sz = k == 0 ? -1.1f : 2.2f;
                if (gone) skinned(d, e * 1.45f, 0.72f, sz, 0.28f, 0.08f, 0.12f, DARK);
                else skinned(d, e * 1.45f, 0.5f, sz, 0.28f, 0.26f, 0.12f, DARK);
            }
        }
        // the mast, the hub
        plain(d, 0f, 3.95f, 0f, 0.14f, 0.32f, 0.14f, DARK, DrawList.PLAIN);
        plain(d, 0f, 4.32f, 0f, 0.32f, 0.1f, 0.32f, DARK, DrawList.PLAIN);
        plain(d, 0f, 4.48f, 0f, 0.22f, 0.07f, 0.22f, 0x2A2E34, DrawList.PLAIN);
        // the blades: four (two across), going round, a yellow band at each tip
        for (int b = 0; b < 2; b++) {
            float a = rotor + b * (float) (Math.PI / 2);
            spun(d, 0f, 4.34f, 0f, 0.17f, 0.03f, BLADE, a, false, BLADES, DrawList.PLAIN);
            float s = (float) Math.sin(a), c = (float) Math.cos(a);
            for (int e = -1; e <= 1; e += 2)
                spun(d, e * s * (BLADE - 0.35f), 4.345f, e * c * (BLADE - 0.35f), 0.175f, 0.032f, 0.22f, a, false, TIPS, DrawList.PAINT);
        }
        // the tail rotor, on the right of the fin, turning about the right
        float tr = rotor * 1.7f;
        for (int b = 0; b < 2; b++)
            spun(d, 0.2f, 3.29f, -5f, 0.02f, 0.08f, 0.82f, tr + b * (float) (Math.PI / 2), true, BLADES, DrawList.PLAIN);
        plain(d, 0.16f, 3.29f, -5f, 0.06f, 0.1f, 0.1f, DARK, DrawList.PLAIN);
    }
}
