package com.shumtugle.skydysh;

/**
 * A place indoors, standing in for the town while she is in it: made by Rooms, kept here with
 * what moves in it. Its frame is the house it is in: across the front, and in from it.
 *
 * The hall of a house: glass doors at the front, on the street, and at the back, on the yard,
 * that slide open as she comes to them; past either she is out. A lift in one side wall: come
 * near it and its button lights by itself, the cabin is called and its doors open; walk in and
 * she turns to the doors, the top floor's button lights, the doors close, the floors count up
 * in a row of lamps over them, and she comes out on the roof. Come down from the roof, and she
 * is in the cabin as it goes down, the doors opening onto the hall.
 *
 * While she rides, the eye is still, across the hall from the lift; walking, it is behind her.
 */
final class Indoors implements Ground {
    // the floor, a step over the pavement; the hall's ceiling and the cabin's over it; the walls' thickness
    static final float FLOOR = 0.3f, HALL_HIGH = 4f, CAB_HIGH = 3f, WALL = 0.4f;
    // the glass doors, front and back: half their width, their height; the lift's doors the same
    static final float DOOR_HALF = 1f, DOOR_HIGH = 2.8f, LIFT_HALF = 0.95f, LIFT_HIGH = 2.75f;
    // the cabin: how deep past the lift's doors, half its width
    static final float CAB_DEEP = 2.5f, CAB_HALF = 1.35f;
    // the glass doors open when she is so near; the lift is called when she is so near it
    static final float DOORS_NEAR = 2.4f, CALL_NEAR = 3.6f;
    // a floor's worth of the ride, seconds; the lift's doors, open a second (1 over this)
    static final float A_FLOOR = 0.42f, LIFT_DOORS = 0.6f;

    /** What happened, for the world: nothing; up on the roof; out at the front; out at the back. */
    static final int NONE = 0, UP = 1, OUT_FRONT = 2, OUT_BACK = 3;
    // the lift: shut; called; open; she in it; going up; coming down; open again, she getting out
    static final int SHUT = 0, CALLED = 1, OPEN = 2, ABOARD = 3, RISING = 4, SINKING = 5, LEAVING = 6;

    // the frame: the middle of the front, at the street (fx, fz); across the front (ax, az); in from it (bx, bz)
    private final float fx, fz, ax, az, bx, bz;
    /** The hall: half its width, its depth from the front to the back. */
    final float half, deep;
    /** The lift: on this side across (1 or -1), so far in from the front; floors over the hall. */
    final int side;
    final float liftV;
    final int floors;
    // what it is made of: the frame of the lift's doors, its lamps, the cabin's walls
    int frameColor = 0xB9A27A, cabColor = 0x7A5236, lampLit = 0xFFB347;
    float frameMat = DrawList.GOLD;

    private final DrawList still = new DrawList();
    // what is in her way: x0, z0, x1, z1, top, in the town's terms; the doors' last
    private float[] solid = new float[5 * 64];
    private int solidN, fixedN;
    private boolean built;

    // how far open the front and back doors and the lift's are, 0 to 1
    float front, back, doors;
    int lift = SHUT;
    private float clock, liftAt, away;
    private float level;                 // the cabin, in floors over the hall
    private boolean pressed;

    /** The hall of the house whose front's middle is (cx, cz) facing (nx, nz), so wide and deep, the lift so. */
    Indoors(float cx, float cz, float nx, float nz, float half, float deep, int side, float liftV, int floors) {
        fx = cx;
        fz = cz;
        bx = -nx;
        bz = -nz;
        ax = nz;
        az = -nx;
        this.half = half;
        this.deep = deep;
        this.side = side;
        this.liftV = liftV;
        this.floors = floors;
    }

    // ---- the frame ----

    float wx(float u, float v) {
        return fx + ax * u + bx * v;
    }

    float wz(float u, float v) {
        return fz + az * u + bz * v;
    }

    /** Across, for a point in the town's terms. */
    float u(float x, float z) {
        return (x - fx) * ax + (z - fz) * az;
    }

    /** In from the front, for a point in the town's terms. */
    float v(float x, float z) {
        return (x - fx) * bx + (z - fz) * bz;
    }

    /** A heading (0 along +z) along (du, dv) in the frame. */
    float heading(float du, float dv) {
        return (float) Math.atan2(ax * du + bx * dv, az * du + bz * dv);
    }

    // ---- building it (see Rooms) ----

    /** The part that never moves, built once. */
    DrawList still() {
        return still;
    }

    /** A box in the frame: its middle across, up and in, its half sizes so, into d. */
    void box(DrawList d, float u, float y, float v, float hu, float hy, float hv, int color, float material) {
        if (Math.abs(ax) > 0.5f) d.box(wx(u, v), y, wz(u, v), hu, hy, hv, null, color, material);
        else d.box(wx(u, v), y, wz(u, v), hv, hy, hu, null, color, material);
    }

    /** A ball in the frame. */
    void ball(DrawList d, float u, float y, float v, float r, int color, int kind) {
        d.ball(wx(u, v), y, wz(u, v), r, null, color, kind);
    }

    /** Something in her way, from (u0, v0) to (u1, v1) in the frame, its top so high. */
    void solid(float u0, float v0, float u1, float v1, float top) {
        if ((solidN + 1) * 5 > solid.length) {
            float[] more = new float[solid.length * 2];
            System.arraycopy(solid, 0, more, 0, solid.length);
            solid = more;
        }
        float x0 = wx(u0, v0), z0 = wz(u0, v0), x1 = wx(u1, v1), z1 = wz(u1, v1);
        int o = solidN++ * 5;
        solid[o] = Math.min(x0, x1);
        solid[o + 1] = Math.min(z0, z1);
        solid[o + 2] = Math.max(x0, x1);
        solid[o + 3] = Math.max(z0, z1);
        solid[o + 4] = top;
        if (!built) fixedN = solidN;
    }

    /** Built: what is in the way from now on is the doors' too, as they stand. */
    void done() {
        built = true;
        fixedN = solidN;
    }

    // ---- where she can go ----

    public float ground(float x, float z, float feet, float reach) {
        float g = FLOOR;
        for (int k = 0; k < solidN; k++) {
            int o = k * 5;
            float top = solid[o + 4];
            if (top <= g || top > feet + reach) continue;
            if (x >= solid[o] && x <= solid[o + 2] && z >= solid[o + 1] && z <= solid[o + 3]) g = top;
        }
        return g;
    }

    public boolean push(float[] p, float r, float feet) {
        boolean hit = false;
        for (int k = 0; k < solidN; k++) {
            int o = k * 5;
            if (solid[o + 4] <= feet + City.LOW) continue;
            float x0 = solid[o], z0 = solid[o + 1], x1 = solid[o + 2], z1 = solid[o + 3];
            float nx = Math.max(x0, Math.min(p[0], x1)), nz = Math.max(z0, Math.min(p[1], z1));
            float dx = p[0] - nx, dz = p[1] - nz, dd = dx * dx + dz * dz;
            if (dd >= r * r) continue;
            hit = true;
            if (dd > 1e-8f) {
                float d = (float) Math.sqrt(dd);
                p[0] = nx + dx / d * r;
                p[1] = nz + dz / d * r;
            } else {
                // inside it: out by the nearest side
                float l = p[0] - x0, rr = x1 - p[0], t = p[1] - z0, b = z1 - p[1];
                float m = Math.min(Math.min(l, rr), Math.min(t, b));
                if (m == l) p[0] = x0 - r;
                else if (m == rr) p[0] = x1 + r;
                else if (m == t) p[1] = z0 - r;
                else p[1] = z1 + r;
            }
        }
        return hit;
    }

    public float ceiling(float x, float z) {
        return FLOOR + (inCabin(u(x, z)) > 0f ? CAB_HIGH : HALL_HIGH);
    }

    public void keepEye(float[] p) {
        float u = u(p[0], p[1]), v = v(p[0], p[1]);
        if (inCabin(u) > -0.2f && Math.abs(v - liftV) < CAB_HALF) {
            u = side * Math.min(side * u, half + WALL + CAB_DEEP - 0.3f);
        } else {
            u = Math.max(-half + 0.3f, Math.min(half - 0.3f, u));
        }
        v = Math.max(WALL + 0.3f, Math.min(deep - WALL - 0.3f, v));
        p[0] = wx(u, v);
        p[1] = wz(u, v);
    }

    /** How far past the lift's doors into the cabin a point across at u is; negative, in the hall. */
    private float inCabin(float u) {
        return side * u - (half + WALL);
    }

    // ---- coming in ----

    /** She comes in at the front door (or the back), a few steps in, facing into the hall. */
    void enter(Runner r, boolean atBack) {
        if (atBack) {
            back = 1f;
            r.put(wx(0f, deep - WALL - 2.6f), FLOOR, wz(0f, deep - WALL - 2.6f), heading(0f, -1f));
        } else {
            front = 1f;
            r.put(wx(0f, WALL + 2.6f), FLOOR, wz(0f, WALL + 2.6f), heading(0f, 1f));
        }
        r.room = this;
        doorsInTheWay();
    }

    /** She comes down from the roof: in the cabin, facing its doors, on its way down. */
    void comeDown(Runner r) {
        aboard(r);
        r.room = this;
        lift = SINKING;
        liftAt = clock;
        level = floors;
        pressed = true;
        cameDown = true;
        doorsInTheWay();
    }

    /** Her in the middle of the cabin, facing its doors, still. */
    private void aboard(Runner r) {
        float cu = side * (half + WALL + CAB_DEEP * 0.5f);
        r.put(wx(cu, liftV), FLOOR, wz(cu, liftV), heading(-side, 0f));
        r.held = true;
    }

    // ---- what goes on ----

    /** A step of the place: the doors and the lift. What happened, for the world (see NONE). */
    int update(float dt, Runner r, World.Host host) {
        clock += dt;
        float u = u(r.x, r.z), v = v(r.x, r.z);
        // the glass doors: open while she is near them; through one, she is out
        front = toward(front, near(u, v, 0f, WALL) < DOORS_NEAR, dt * 2.5f);
        back = toward(back, near(u, v, 0f, deep - WALL) < DOORS_NEAR, dt * 2.5f);
        int out = NONE;
        if (front > 0.85f && v < WALL * 0.3f) out = OUT_FRONT;
        if (back > 0.85f && v > deep - WALL * 0.3f) out = OUT_BACK;
        float t = clock - liftAt;
        float callU = side * (half - 0.9f);
        switch (lift) {
            case SHUT:
                if (near(u, v, callU, liftV) < CALL_NEAR) {
                    // her near: the button lights by itself
                    lift = CALLED;
                    liftAt = clock;
                    host.sound(Synth.DTMF + 1, 0.35f, 1f);
                }
                break;
            case CALLED:
                if (t > 0.5f) {
                    lift = OPEN;
                    liftAt = clock;
                    host.sound(Synth.DOOR, 0.3f, 1.4f);
                }
                break;
            case OPEN:
                if (inCabin(u) > 0.35f && !r.inAir()) {
                    // in: she turns to the doors and stands
                    aboard(r);
                    lift = ABOARD;
                    liftAt = clock;
                    pressed = false;
                    break;
                }
                away = near(u, v, callU, liftV) > CALL_NEAR * 1.6f ? away + dt : 0f;
                if (away > 2f) lift = SHUT;
                break;
            case ABOARD:
                if (!pressed && t > 0.35f) {
                    // the top floor's button, lit by itself
                    pressed = true;
                    host.sound(Synth.DTMF + Math.min(9, floors), 0.35f, 1f);
                }
                if (t > 1.1f) {
                    lift = RISING;
                    liftAt = clock;
                    host.sound(Synth.HUM, 0.22f, 1f);
                }
                break;
            case RISING:
                level = Math.min(floors, t / A_FLOOR);
                if (t > floors * A_FLOOR + 0.25f) {
                    host.sound(Synth.CHIME, 0.5f, 1f);
                    lift = SHUT;
                    return UP;
                }
                break;
            case SINKING:
                level = Math.max(0f, floors - Math.max(0f, t - 0.3f) / A_FLOOR);
                if (t > 0.3f + floors * A_FLOOR + 0.2f) {
                    level = 0f;
                    lift = LEAVING;
                    liftAt = clock;
                    host.sound(Synth.CHIME, 0.5f, 1f);
                    host.sound(Synth.DOOR, 0.3f, 1.4f);
                }
                break;
            case LEAVING:
                if (doors > 0.8f) r.held = false;
                if (inCabin(u) < -1.4f) {
                    // out into the hall: the eye behind her again
                    lift = OPEN;
                    liftAt = clock;
                    r.recenter();
                }
                break;
        }
        boolean open = lift == OPEN || lift == LEAVING || lift == ABOARD && t < 0.45f;
        doors = toward(doors, open, dt / LIFT_DOORS);
        doorsInTheWay();
        return out;
    }

    private static float near(float u, float v, float pu, float pv) {
        return (float) Math.sqrt((u - pu) * (u - pu) + (v - pv) * (v - pv));
    }

    private static float toward(float now, boolean open, float step) {
        return Math.max(0f, Math.min(1f, now + (open ? step : -step)));
    }

    /** The doors that are not open far enough to go through, in her way. */
    private void doorsInTheWay() {
        solidN = fixedN;
        float top = FLOOR + HALL_HIGH;
        if (front < 0.85f) solid(-DOOR_HALF, 0f, DOOR_HALF, WALL, top);
        if (back < 0.85f) solid(-DOOR_HALF, deep - WALL, DOOR_HALF, deep, top);
        float wu = side * half, wu1 = side * (half + WALL);
        if (doors < 0.85f) solid(Math.min(wu, wu1), liftV - LIFT_HALF, Math.max(wu, wu1), liftV + LIFT_HALF, top);
    }

    /** Whether the eye is held still across the hall, and if so where and how it looks: x, y, z, yaw, pitch. */
    boolean shot(float[] eye) {
        if (lift != ABOARD && lift != RISING && lift != SINKING && lift != LEAVING) return false;
        float eu = -side * (half - 1.1f), ev = liftV + 1.3f, ey = FLOOR + 2.7f;
        float tu = side * (half + WALL + CAB_DEEP * 0.45f), tv = liftV, ty = FLOOR + 1.75f;
        eye[0] = wx(eu, ev);
        eye[1] = ey;
        eye[2] = wz(eu, ev);
        float dx = wx(tu, tv) - eye[0], dz = wz(tu, tv) - eye[2];
        eye[3] = (float) Math.atan2(dx, dz);
        eye[4] = (float) Math.atan2(ty - ey, Math.sqrt(dx * dx + dz * dz));
        return true;
    }

    /** For a player that knows the way (the desktop stand): where to go now, x and z. */
    void goal(Runner r, float[] out) {
        float u = u(r.x, r.z), v = v(r.x, r.z), gu, gv;
        if (cameDown && (lift == LEAVING || lift == OPEN) && inCabin(u) > -0.8f) {
            // out of the cabin first, straight out of its doors
            gu = side * (half - 1.5f);
            gv = liftV;
        } else if (cameDown && (lift == LEAVING || lift == OPEN)) {
            gu = 0f;
            gv = -1f;
        } else if (Math.abs(v - liftV) > 0.45f && inCabin(u) < -0.6f) {
            // before the lift's doors first, then straight in
            gu = side * (half - 1.3f);
            gv = liftV;
        } else {
            gu = side * (half + WALL + CAB_DEEP * 0.5f);
            gv = liftV;
        }
        out[0] = wx(gu, gv);
        out[1] = wz(gu, gv);
    }

    private boolean cameDown;

    /** The light indoors: lamps overhead, the light pooled under them, no haze. */
    void light(DrawList d) {
        Daylight.light(d, 1f);
        float cu = 0f, cv = deep * 0.5f;
        // the light from overhead, its depth map starting under the ceiling so the ceiling casts nothing
        d.sun(0.18f * ax + 0.12f * bx, 1f, 0.18f * az + 0.12f * bz, wx(cu, cv), FLOOR + 1.6f, wz(cu, cv), 30f, 30f, 4.6f);
        d.sunColor[0] = 0.95f;
        d.sunColor[1] = 0.86f;
        d.sunColor[2] = 0.7f;
        d.fogStart = 400f;
        d.fogScale = 400f;
        d.spot[0] = wx(cu, cv);
        d.spot[1] = FLOOR + 3f;
        d.spot[2] = wz(cu, cv);
        d.spot[3] = deep * 0.85f;
        d.spot[4] = 0.32f;
    }

    void draw(DrawList d) {
        d.append(still);
        float top = FLOOR;
        // the glass doors: two leaves each, sliding apart
        for (int k = 0; k < 2; k++) {
            float open = k == 0 ? front : back, v = k == 0 ? WALL * 0.5f : deep - WALL * 0.5f;
            float slide = open * DOOR_HALF * 0.92f;
            for (int s = -1; s <= 1; s += 2) {
                float cu = s * (DOOR_HALF * 0.5f + slide);
                box(d, cu, top + DOOR_HIGH * 0.5f, v, DOOR_HALF * 0.5f, DOOR_HIGH * 0.5f, 0.03f, 0x3E5560, DrawList.GLASS);
                box(d, cu, top + DOOR_HIGH * 0.5f, v, DOOR_HALF * 0.5f + 0.02f, 0.03f, 0.05f, frameColor, frameMat);
                box(d, cu - s * (DOOR_HALF * 0.5f - 0.03f), top + DOOR_HIGH * 0.5f, v, 0.03f, DOOR_HIGH * 0.5f, 0.05f, frameColor, frameMat);
            }
        }
        // the lift's doors: two leaves sliding apart into the wall
        float du = side * (half + WALL * 0.5f), slide = doors * LIFT_HALF * 0.95f;
        for (int s = -1; s <= 1; s += 2)
            box(d, du, top + LIFT_HIGH * 0.5f, liftV + s * (LIFT_HALF * 0.5f + slide), 0.04f, LIFT_HIGH * 0.5f, LIFT_HALF * 0.5f,
                    0x8C8F8A, DrawList.PLAIN);
        // the button by the doors: lit once she is near; over the doors, a lamp a floor, the cabin's lit
        float wall = side * (half - 0.03f);
        boolean lit = lift != SHUT;
        box(d, wall, top + 1.3f, liftV + LIFT_HALF + 0.42f, 0.03f, 0.05f, 0.05f, lit ? 0x7FF0B8 : 0x2E3A36, lit ? DrawList.GLOW : DrawList.PLAIN);
        for (int f = 0; f <= floors; f++) {
            float lv = liftV + (f - floors * 0.5f) * 0.2f;
            boolean on = Math.round(level) == f;
            box(d, wall, top + LIFT_HIGH + 0.42f, lv, 0.03f, 0.055f, 0.055f, on ? lampLit : 0x4A3A2A, on ? DrawList.GLOW : DrawList.PLAIN);
        }
        // in the cabin: the buttons, a column of them by the doors, the one she is going to lit
        float pu = side * (half + WALL + 0.35f), pv = liftV + CAB_HALF - 0.04f;
        for (int f = 0; f <= floors; f++) {
            boolean on = pressed && (lift == SINKING ? f == 0 : lift == LEAVING ? false : f == floors);
            box(d, pu, top + 1.05f + f * 0.11f, pv, 0.045f, 0.04f, 0.02f, on ? lampLit : 0xC8C2B0, on ? DrawList.GLOW : DrawList.PLAIN);
        }
        // the lamps over the doors inside the cabin, the same
        float iu = side * (half + WALL + 0.03f);
        for (int f = 0; f <= floors; f++) {
            float lv = liftV + (f - floors * 0.5f) * 0.16f;
            boolean on = Math.round(level) == f && lift != OPEN && lift != SHUT && lift != CALLED;
            box(d, iu, top + LIFT_HIGH + 0.12f, lv, 0.02f, 0.04f, 0.04f, on ? lampLit : 0x3A2E24, on ? DrawList.GLOW : DrawList.PLAIN);
        }
    }
}
