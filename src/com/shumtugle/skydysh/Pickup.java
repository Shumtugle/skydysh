package com.shumtugle.skydysh;

/**
 * A duty pickup: the dispatch's own, open-topped (she is too tall for a roof), cream over teal
 * with a coral stripe, the plate's sign on its doors and an amber lamp behind the cab; a bed
 * behind for a load. Parked, it stands at a curb; got into, she drives it.
 *
 * It drives as the old racing game's car did: the finger on the glass is the throttle and the
 * driver drives, choosing the speed by itself (slower into a hard turn, behind a car ahead,
 * before something in the way); lifted, it rolls on and slows; drawn down, it brakes, and held
 * down standing still, it backs. Sideways from where the finger came down, the wheel turns as far
 * as the finger went. The wheel turns the car the less the faster it goes, and the tyres hold so
 * much and no more: past that it runs wide. The engine pulls hard from a standstill and less as
 * it nears its top, with a short pause at each change of gear.
 *
 * Its own frame: x to its right, y up, its nose along +z; the road at y = 0.
 */
final class Pickup {
    static final float VMAX = 20f;                     // top speed, meters a second
    static final float ACCEL = 0.32f * VMAX, BRAKE = 0.6f * VMAX, HARD_BRAKE = 0.9f * VMAX, COAST = 0.12f * VMAX;
    static final float BACK_TOP = 4f;                  // backing, no faster than this
    static final float CREEP = 1.5f;                   // the driver noses along at least this fast
    static final float WHEELBASE = 3.1f, LOCK = 0.6f, GRIP = 8.5f;
    static final float HALF_LONG = 2.5f, HALF_WIDE = 1f, TOP = 1.7f, ENDS = 1.3f, END_R = 1.05f;
    static final int CREAM = 0xF1E8D4, TEAL = 0x2F8A86, CORAL = 0xE0644A, AMBER = 0xF6B54A;

    float x, z, heading, speed, steer;
    float y;                                           // the ground under it
    boolean driven;                                    // she is in it
    int gear = 1;
    private float shift, still;
    /** Where it was first parked: the cell it belongs to (see Duty). */
    final long home;

    Pickup(float x, float z, float heading, long home) {
        this.x = x;
        this.z = z;
        this.heading = heading;
        this.home = home;
    }

    /**
     * A frame of driving: the finger down or not, the wheel as the finger has it (-1 to 1), the
     * brake (0 to 1); the town, the traffic and the people in its way.
     */
    void drive(float dt, boolean throttle, float wheel, float brake, City city, Cars cars, Walkers walkers) {
        steer += (wheel - steer) * Math.min(1f, dt * 14f);
        float fx = (float) Math.sin(heading), fz = (float) Math.cos(heading);
        if (brake > 0.05f) {
            if (speed > 0.3f) {
                speed = Math.max(0f, speed - (brake > 0.6f ? HARD_BRAKE : BRAKE) * brake * dt);
                still = 0f;
            } else {
                // held down, standing: after a moment it backs
                still += dt;
                if (still > 0.4f) speed = Math.max(-BACK_TOP, speed - ACCEL * 0.5f * dt);
            }
        } else if (!throttle) {
            still = 0f;
            if (speed > 0f) speed = Math.max(0f, speed - COAST * dt);
            else speed = Math.min(0f, speed + COAST * 2f * dt);
        } else {
            still = 0f;
            if (speed < 0f) speed = Math.min(0f, speed + HARD_BRAKE * dt);
            else accelerate(dt, target(fx, fz, city, cars));
        }
        // the wheel: less lock the faster it goes; the tyres hold so much, past that it runs wide
        float lock = LOCK / (1f + Math.abs(speed) / 9f);
        // nosed up against something with the finger down and the wheel over, it still edges round
        float turning = throttle && Math.abs(speed) < 1.2f && Math.abs(steer) > 0.2f ? 1.2f : speed;
        float yaw = turning * (float) Math.tan(lock * steer) / WHEELBASE;
        float most = GRIP / Math.max(2f, Math.abs(speed));
        yaw = Math.max(-most, Math.min(most, yaw));
        heading += yaw * dt;
        fx = (float) Math.sin(heading);
        fz = (float) Math.cos(heading);
        x += fx * speed * dt;
        z += fz * speed * dt;
        // what it runs into: two round ends along it, against the town, the traffic, the people
        float mx = 0f, mz = 0f;
        boolean hit = false;
        for (int e = -1; e <= 1; e += 2) {
            float[] p = {x + fx * e * ENDS, z + fz * e * ENDS};
            float ox = p[0], oz = p[1];
            boolean h = city.push(p, END_R, y);
            h |= cars.push(p, END_R, 0f, this);
            h |= walkers.push(p, END_R);
            if (h) {
                mx += p[0] - ox;
                mz += p[1] - oz;
                hit = true;
            }
        }
        if (hit) {
            x += mx * 0.5f;
            z += mz * 0.5f;
            float m = (float) Math.sqrt(mx * mx + mz * mz);
            if (m > 1e-4f) {
                float into = -(fx * mx + fz * mz) / m * Math.signum(speed);
                if (into > 0.2f) speed *= 1f - 0.85f * into;
            }
        }
        y += (city.ground(x, z) - y) * Math.min(1f, dt * 12f);
    }

    /** The engine: hard from a standstill, less near the top, a short pause at each change of gear. */
    private void accelerate(float dt, float target) {
        float sp = speed / VMAX;
        int g = 1 + Math.min(4, (int) (sp * 5f));
        if (g > gear) shift = 0.2f;
        gear = g;
        if (speed < target) {
            if (shift > 0f) {
                shift -= dt;
                return;
            }
            float acc = Math.max(ACCEL * 0.05f, ACCEL * (1.12f - sp));
            speed = Math.min(target, speed + acc * dt);
        } else {
            float dec = speed - target > 1.5f ? BRAKE : COAST * 2f;
            speed = Math.max(target, speed - dec * dt);
        }
    }

    /**
     * The speed the driver picks: the top, less into a hard turn; no faster than it can stop
     * short of a car ahead in its way, or of something standing in its way.
     */
    private float target(float fx, float fz, City city, Cars cars) {
        float t = VMAX * (1f - 0.55f * Math.abs(steer));
        for (int c = 0; c < Cars.MOST; c++) {
            if (!cars.live(c)) continue;
            float ox = cars.x[c] - x, oz = cars.z[c] - z;
            float along = ox * fx + oz * fz, across = Math.abs(ox * fz - oz * fx);
            if (along < 0f || along > 30f || across > 1.8f) continue;
            float gap = Math.max(0f, along - 6f);
            t = Math.min(t, Math.max(0f, cars.speed[c]) + gap * 0.9f);
        }
        // something standing ahead: looked for a few steps on, along the way it goes
        float[] p = new float[2];
        for (float d = 4f; d <= 16f; d += 3f) {
            p[0] = x + fx * d;
            p[1] = z + fz * d;
            if (city.push(p, 0.9f, y)) {
                t = Math.min(t, (float) Math.sqrt(2f * BRAKE * Math.max(0f, d - 3.5f)));
                break;
            }
        }
        // never quite nothing: nosing along, the wheel can still take it round what is in the way
        return Math.max(t, CREEP);
    }

    /** Pushes a round body of radius rr at (p[0], p[1]) out of it; true if it touched it. */
    boolean push(float[] p, float rr) {
        float fx = (float) Math.sin(heading), fz = (float) Math.cos(heading);
        boolean hit = false;
        for (int e = -1; e <= 1; e += 2) {
            float cx = x + fx * e * ENDS, cz = z + fz * e * ENDS;
            float dx = p[0] - cx, dz = p[1] - cz, dd = dx * dx + dz * dz, m = rr + END_R;
            if (dd >= m * m || dd < 1e-8f) continue;
            float l = (float) Math.sqrt(dd);
            p[0] = cx + dx / l * m;
            p[1] = cz + dz / l * m;
            hit = true;
        }
        return hit;
    }

    // where the driver sits, in its frame: across, over the road, along
    static final float SEAT_X = -0.32f, SEAT_Y = 0.98f, SEAT_Z = 0.2f;

    /** Where the seat is in the world, into out (x, y, z). */
    void seat(float[] out) {
        float c = (float) Math.cos(heading), s = (float) Math.sin(heading);
        out[0] = x + c * SEAT_X + s * SEAT_Z;
        out[1] = y + SEAT_Y;
        out[2] = z - s * SEAT_X + c * SEAT_Z;
    }

    /** Where she gets in and out: beside the driver's door, so far off it (x, z into out). */
    void door(float off, float[] out) {
        float c = (float) Math.cos(heading), s = (float) Math.sin(heading);
        float ax = -(HALF_WIDE + off), az = SEAT_Z;
        out[0] = x + c * ax + s * az;
        out[1] = z - s * ax + c * az;
    }

    // ---- drawing ----

    private static float px, py, pz;
    private static final float[] frame = new float[9], t = new float[9];

    void draw(DrawList d, float time) {
        float cy = (float) Math.cos(heading), sy = (float) Math.sin(heading);
        px = x;
        py = y;
        pz = z;
        frame[0] = cy; frame[1] = 0f; frame[2] = sy;
        frame[3] = 0f; frame[4] = 1f; frame[5] = 0f;
        frame[6] = -sy; frame[7] = 0f; frame[8] = cy;
        float plain = DrawList.PLAIN, glass = DrawList.GLASS;
        // the body: teal below a coral stripe, cream above; the bonnet
        part(d, 0f, 0.6f, 0f, 0.98f, 0.28f, 2.5f, 0f, TEAL, plain);
        part(d, 0f, 0.36f, 0f, 0.92f, 0.05f, 1.9f, 0f, 0x2A2829, plain);
        part(d, 0f, 0.9f, 0f, 0.99f, 0.03f, 2.5f, 0f, CORAL, plain);
        part(d, 0f, 1.0f, 1.75f, 0.94f, 0.08f, 0.75f, 0f, CREAM, plain);
        part(d, 0f, 0.92f, 2.48f, 0.7f, 0.06f, 0.04f, 0f, 0x2A2A2C, plain);
        // the cab, open to the sky: its sides and back, the dashboard, the screen in its frame
        for (int e = -1; e <= 1; e += 2) {
            part(d, e * 0.95f, 1.12f, 0.45f, 0.04f, 0.2f, 0.55f, 0f, CREAM, plain);
            // the plate's sign on the door
            part(d, e * 0.995f, 0.66f, 0.42f, 0.01f, 0.13f, 0.2f, 0f, Frida.SHEM, DrawList.GLOW);
            part(d, e * 0.88f, 1.5f, 1.04f, 0.04f, 0.32f, 0.04f, -0.35f, 0x3A3B40, plain);
        }
        part(d, 0f, 1.24f, -0.12f, 0.95f, 0.34f, 0.05f, 0f, CREAM, plain);
        part(d, 0f, 1.08f, 0.98f, 0.9f, 0.1f, 0.1f, 0f, 0x3A3B40, plain);
        part(d, 0f, 1.5f, 1.05f, 0.84f, 0.3f, 0.015f, -0.35f, 0x33414E, glass);
        // the seat, and the wheel before the driver's place
        part(d, 0f, 0.96f, 0.22f, 0.8f, 0.07f, 0.3f, 0f, 0x4A3A32, plain);
        part(d, 0f, 1.2f, -0.04f, 0.8f, 0.24f, 0.05f, 0f, 0x4A3A32, plain);
        part(d, SEAT_X, 1.28f, 0.74f, 0.2f, 0.2f, 0.025f, 0.45f, 0x1C1B1D, plain);
        // the amber lamp on the back of the cab, turning
        float beam = 0.6f + 0.4f * (float) Math.sin(time * 9f);
        part(d, 0f, 1.66f, -0.12f, 0.16f, 0.07f, 0.08f, 0f, Street.mix(0xB86A20, AMBER, beam), DrawList.GLOW);
        // the bed: its floor, sides, the tailgate
        part(d, 0f, 0.92f, -1.42f, 0.93f, 0.04f, 1.05f, 0f, 0x6B6258, DrawList.WOOD);
        for (int e = -1; e <= 1; e += 2) part(d, e * 0.95f, 1.1f, -1.42f, 0.04f, 0.2f, 1.05f, 0f, CREAM, plain);
        part(d, 0f, 1.1f, -2.47f, 0.95f, 0.2f, 0.04f, 0f, CREAM, plain);
        // wheels, lamps, bumpers
        for (int e = -1; e <= 1; e += 2) {
            for (int end = -1; end <= 1; end += 2) {
                Cars.wheel(d, px, py, pz, frame, e * 0.9f, 0.37f, end * 1.62f, 0.14f, 0.37f);
            }
            part(d, e * 0.66f, 0.8f, 2.505f, 0.17f, 0.08f, 0.01f, 0f, 0xFFF0C2, DrawList.GLOW);
            part(d, e * 0.72f, 0.82f, -2.515f, 0.14f, 0.07f, 0.01f, 0f, 0xD43A2E, plain);
        }
        part(d, 0f, 0.44f, 2.55f, 1f, 0.08f, 0.06f, 0f, 0xD2CEC6, plain);
        part(d, 0f, 0.44f, -2.55f, 1f, 0.08f, 0.06f, 0f, 0xD2CEC6, plain);
    }

    /** A box of it in its own frame: tilted about its own x first, then turned with it. */
    private static void part(DrawList d, float lx, float ly, float lz, float hx, float hy, float hz, float tilt,
                             int color, float material) {
        float ct = (float) Math.cos(tilt), st = (float) Math.sin(tilt);
        float[] f = frame;
        for (int r = 0; r < 3; r++) {
            t[r * 3] = f[r * 3];
            t[r * 3 + 1] = f[r * 3 + 1] * ct + f[r * 3 + 2] * st;
            t[r * 3 + 2] = -f[r * 3 + 1] * st + f[r * 3 + 2] * ct;
        }
        d.box(px + f[0] * lx + f[1] * ly + f[2] * lz, py + f[3] * lx + f[4] * ly + f[5] * lz,
                pz + f[6] * lx + f[7] * ly + f[8] * lz, hx, hy, hz, t, color, material);
        if (Cars.skinned(tilt, material)) d.skin(Cars.PICKUP_SKIN, lx, ly, lz, color);
    }
}
