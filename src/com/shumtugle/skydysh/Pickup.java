package com.shumtugle.skydysh;

/**
 * A duty pickup: the dispatch's own, open-topped (she is too tall for a roof), cream over teal
 * with a coral stripe, the plate's sign on its doors and an amber lamp behind the cab; a bed
 * behind for a load. Parked, it stands at a curb; got into, she drives it.
 *
 * It drives as a car in a town game should: easier than going on foot, and quick. The throttle
 * held, it goes, the longer the faster; the wheel turns it where its nose points, as sharply as the
 * wheel says, less and less the faster it goes, and it never slides; hard over, it slows by itself
 * to make the corner. The brake stops it, and held standing, after a breath it backs; neither,
 * it rolls on, slowing. It turns only as it goes, as a car does: no swinging round on the spot. Run
 * into a wall, it runs along it, losing only what went into it.
 *
 * Its own frame: x to its right, y up, its nose along +z; the road at y = 0.
 *
 * A boat is the island's yacht, got into and driven as a pickup is, the same wheel in the hand;
 * but it goes on the water (see sail), slower to answer, and slides a little through a turn.
 */
final class Pickup {
    static final float VMAX = 36f;                     // top speed, meters a second
    static final float ACCEL = 9f, BRAKE = 22f, HARD_BRAKE = 30f, COAST = 3f;
    static final float LET_GO = 4f;                    // the throttle let go: it rolls on, slowing so
    static final float BACK_AFTER = 0.2f;              // stopped with the brake held: so long, and it backs
    static final float BACK_TOP = 8f;                  // backing, no faster than this
    static final float CREEP = 1.5f;
    // the wheel full over, the speed it is held to is so much less than the top; how hard it slows for it
    static final float TURN_SLOWS = 0.55f, EASE = 15f;                   // the driver noses along at least this fast
    // the tightest turn, its radius; how fast it may turn at a middling speed and at the top
    static final float TIGHTEST = 4.5f, YAW_MID = 2.4f, YAW_TOP = 0.9f;
    static final float STEER_RATE = 9f;                // how fast the wheel follows the hand, and comes back straight
    static final float HALF_LONG = 2.5f, HALF_WIDE = 1f, TOP = 1.7f, ENDS = 1.3f, END_R = 1.05f;
    static final int CREAM = 0xF1E8D4, TEAL = 0x2F8A86, CORAL = 0xE0644A, AMBER = 0xF6B54A;

    float x, z, heading, speed, steer;
    /** The speed the one driving wants, if it is not the pickup's own care that picks it (-1). */
    float cap = -1f;
    float y;                                           // the ground under it
    boolean driven;                                    // she is in it
    /** Kept out wherever it is left, however far she goes (a job's own). */
    boolean kept;
    /** The crates in its bed, none to CRATES; and how long since the last was put on or taken off. */
    int load;
    float loadedAgo = 10f;
    static final int CRATES = 4;
    int gear = 1;
    private float still;
    /** The cell whose place at the curb it stands for: where it was first parked, or one it took over (see Duty). */
    long home;

    /** The island's yacht, not a pickup: on the water, not the road. */
    final boolean boat;
    // a boat: the way it goes (the bow swings first, the way follows), and the swell under it
    private float course, swell;
    /** A boat: run aground or into the pier hard, this frame. */
    boolean bumped;

    Pickup(float x, float z, float heading, long home) {
        this(x, z, heading, home, false);
    }

    Pickup(float x, float z, float heading, long home, boolean boat) {
        this.x = x;
        this.z = z;
        this.heading = heading;
        this.home = home;
        this.boat = boat;
        course = heading;
        if (boat) y = City.WATER;
    }

    /** Half its length, half its width; how far along its two round ends are, and how round. */
    float halfLong() {
        return boat ? Island.HULL_HALF : HALF_LONG;
    }

    float halfWide() {
        return boat ? Island.BEAM : HALF_WIDE;
    }

    float ends() {
        return boat ? B_ENDS : ENDS;
    }

    float endR() {
        return boat ? B_END_R : END_R;
    }

    /** So near its sides or ends she stands to get in: from a pier, a boat is a step further off. */
    float reach(float near) {
        return boat ? near + 0.9f : near;
    }

    // ---- a boat ----
    // the top, how hard the screw drives it and backs it, how the water holds it back; the rudder:
    // the tightest turn's radius, the fastest it swings, how fast it follows the hand; how fast the
    // way follows the bow; how deep its keel goes; its ends
    static final float SEA_TOP = 15f, SEA_ACCEL = 3.4f, SEA_BACK = 4.5f, SEA_BRAKE = 5f, SEA_DRAG = 0.35f;
    static final float SEA_TIGHT = 8f, SEA_YAW = 0.75f, SEA_STEER_RATE = 3.5f, SLIP = 1.8f, DRAFT = 0.9f;
    static final float B_ENDS = 3.3f, B_END_R = 1.5f, SLOW_TURN = 0.3f, SLOW_TURN_UNDER = 2f;
    // where she sits at the helm, behind the cabin: across, over the water, along
    static final float HELM_Y = 1.25f, HELM_Z = -3.2f;

    /** A frame of a boat on the water: the throttle, the wheel, the brake (astern, held). */
    private void sail(float dt, boolean throttle, float wheel, float brake, City city) {
        bumped = false;
        steer += (wheel - steer) * Math.min(1f, dt * SEA_STEER_RATE);
        swell += dt;
        float want = cap >= 0f ? Math.min(cap, SEA_TOP) : SEA_TOP;
        if (brake > 0.05f) {
            if (speed > 0.3f) {
                speed = Math.max(0f, speed - SEA_BRAKE * brake * dt);
                still = 0f;
            } else {
                // the screw astern: held, it backs
                still += dt;
                if (still > BACK_AFTER) speed = Math.max(-SEA_BACK * Math.max(0.35f, brake), speed - SEA_ACCEL * 0.6f * dt);
            }
        } else if (throttle) {
            still = 0f;
            if (speed < 0f) speed = Math.min(0f, speed + SEA_BRAKE * dt);
            else if (speed < want) speed = Math.min(want, speed + SEA_ACCEL * (1.1f - 0.6f * speed / SEA_TOP) * dt);
            else speed = Math.max(want, speed - SEA_DRAG * 4f * dt);
        } else {
            // let go: the water takes the way off it, slowly
            still = 0f;
            float off = (SEA_DRAG + 0.02f * speed * speed) * dt;
            speed -= Math.signum(speed) * Math.min(Math.abs(speed), off);
        }
        gear = 1 + Math.min(4, (int) (Math.abs(speed) / SEA_TOP * 5f));
        // the rudder bites as it goes: tight when slow, wider fast; never round on the spot
        float yaw = steer * Math.min(Math.abs(speed) / SEA_TIGHT, SEA_YAW) * Math.signum(speed);
        // but the screw turning, slow, the wash over the rudder swings the stern a little: off a bank
        if (throttle && Math.abs(speed) < SLOW_TURN_UNDER) yaw += steer * SLOW_TURN * (1f - Math.abs(speed) / SLOW_TURN_UNDER);
        heading += yaw * dt;
        if (aground(x, z, heading, city) > aground(x, z, heading - yaw * dt, city)) heading -= yaw * dt;
        // the way follows the bow, late: it slides a little through a turn
        float e = heading - course;
        while (e > Math.PI) e -= 2f * (float) Math.PI;
        while (e < -Math.PI) e += 2f * (float) Math.PI;
        course += e * Math.min(1f, dt * SLIP);
        float cx = (float) Math.sin(course), cz = (float) Math.cos(course);
        float nx = x + cx * speed * dt, nz = z + cz * speed * dt;
        // not aground, nor out past the open sea: along the shore if it can, else it stops, a jolt
        float now = aground(x, z, heading, city);
        if (aground(nx, nz, heading, city) <= now) {
            x = nx;
            z = nz;
        } else if (aground(nx, z, heading, city) <= now) {
            x = nx;
            speed *= 1f - Math.min(1f, dt * 2f);
        } else if (aground(x, nz, heading, city) <= now) {
            z = nz;
            speed *= 1f - Math.min(1f, dt * 2f);
        } else {
            bumped = Math.abs(speed) > 2f;
            speed *= -0.2f;
        }
        // the pier and the yachts moored: its round ends off them, as a pickup's off a wall
        float fx = (float) Math.sin(heading), fz = (float) Math.cos(heading), mx = 0f, mz = 0f;
        boolean hit = false;
        for (int s = -1; s <= 1; s += 2) {
            float[] p = {x + fx * s * B_ENDS, z + fz * s * B_ENDS};
            float ox = p[0], oz = p[1];
            if (city.push(p, B_END_R, City.WATER - 0.3f)) {
                mx += p[0] - ox;
                mz += p[1] - oz;
                hit = true;
            }
        }
        if (hit) {
            float m = (float) Math.sqrt(mx * mx + mz * mz);
            if (aground(x + mx * 0.5f, z + mz * 0.5f, heading, city) <= aground(x, z, heading, city)) {
                x += mx * 0.5f;
                z += mz * 0.5f;
            }
            if (m > 1e-4f) {
                float into = -(cx * mx + cz * mz) / m * Math.signum(speed);
                if (into > 0.05f) {
                    if (into > 0.5f && Math.abs(speed) > 2f) bumped = true;
                    speed *= 1f - into * 0.8f;
                }
            }
        }
        y = City.WATER + 0.09f * (float) Math.sin(swell * 1.1f);
    }

    /**
     * How far aground a boat headed so with its middle at (x, z) would be: how much too shallow
     * under its ends and its sides, added (past the open sea counts as a meter each); 0 afloat.
     * Run onto a bank, it may go along it or off it, never further on.
     */
    private static float aground(float x, float z, float heading, City city) {
        float fx = (float) Math.sin(heading), fz = (float) Math.cos(heading), sum = 0f;
        for (int k = 0; k < 4; k++) {
            float along = k == 0 ? Island.HULL_HALF : k == 1 ? -Island.HULL_HALF : 0f;
            float across = k == 2 ? Island.BEAM : k == 3 ? -Island.BEAM : 0f;
            float px = x + fx * along + fz * across, pz = z + fz * along - fx * across;
            if (!City.reachable(px, pz)) sum += 1f;
            sum += Math.max(0f, city.ground(px, pz) - (City.WATER - DRAFT));
        }
        return sum;
    }

    private static boolean afloat(float x, float z, float heading, City city) {
        return aground(x, z, heading, city) <= 0f;
    }

    /** Whether a boat floats at (x, z), headed so: for the stand. */
    static boolean floats(float x, float z, float heading, City city) {
        return afloat(x, z, heading, city);
    }

    /**
     * A frame of driving: the finger down or not, the wheel as the finger has it (-1 to 1), the
     * brake (0 to 1), the throttle held flat by hand (the driver's own care set aside); the town,
     * the traffic and the people in its way.
     */
    void drive(float dt, boolean throttle, float wheel, float brake, boolean flat, City city, Cars cars, Walkers walkers) {
        if (boat) {
            sail(dt, throttle, wheel, brake, city);
            return;
        }
        steer += (wheel - steer) * Math.min(1f, dt * STEER_RATE);
        float fx = (float) Math.sin(heading), fz = (float) Math.cos(heading);
        if (brake > 0.05f) {
            if (speed > 0.3f) {
                speed = Math.max(0f, speed - (brake > 0.6f ? HARD_BRAKE : BRAKE) * brake * dt);
                still = 0f;
            } else {
                // held, standing: it backs
                still += dt;
                // as fast back as the brake is held hard, never slower than a crawl
                if (still > BACK_AFTER) speed = Math.max(-BACK_TOP * Math.max(0.35f, brake), speed - ACCEL * 0.5f * dt);
            }
        } else if (!throttle) {
            still = 0f;
            if (speed > 0f) speed = Math.max(0f, speed - (driven ? LET_GO : COAST) * dt);
            else speed = Math.min(0f, speed + COAST * 2f * dt);
        } else {
            still = 0f;
            if (speed < 0f) speed = Math.min(0f, speed + HARD_BRAKE * dt);
            else accelerate(dt, flat ? VMAX : cap >= 0f ? Math.min(cap, target(fx, fz, city, cars)) : target(fx, fz, city, cars));
        }
        // the wheel: it turns the car as sharply as it says, as tight as the tightest turn when slow,
        // less and less toward the top; only as it goes, backing the other way round. It goes where
        // its nose points: no sliding
        float v = speed;
        float cap = YAW_MID + (YAW_TOP - YAW_MID) * Math.min(1f, Math.abs(speed) / VMAX);
        float yaw = steer * Math.min(Math.abs(v) / TIGHTEST, cap) * Math.signum(v);
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
                // only what went into it is lost: along a wall it runs on, its nose turned along it
                float into = -(fx * mx + fz * mz) / m * Math.signum(speed);
                if (into > 0.05f) {
                    speed *= 1f - into;
                    float ax = -mz / m, az = mx / m;
                    if (ax * fx + az * fz < 0f) {
                        ax = -ax;
                        az = -az;
                    }
                    float to = (float) Math.atan2(ax, az), d = to - heading;
                    while (d > Math.PI) d -= 2f * (float) Math.PI;
                    while (d < -Math.PI) d += 2f * (float) Math.PI;
                    if (Math.abs(d) < 1.2f) heading += d * Math.min(1f, dt * 8f) * into;
                }
            }
        }
        // not up the mountain's slope, off its road: back, and on along the slope's foot with what
        // went along it, its nose turned that way, as along a wall
        // nor up the ridge's: back, and half as fast
        if (City.ridgeHolds(x - fx * speed * dt, z - fz * speed * dt, x, z)) {
            x -= fx * speed * dt;
            z -= fz * speed * dt;
            speed *= 0.5f;
        }
        if (Mountain.holds(x - fx * speed * dt, z - fz * speed * dt, x, z)) {
            x -= fx * speed * dt;
            z -= fz * speed * dt;
            float rx = x - Mountain.MX, rz = z - Mountain.MZ, rl = (float) Math.sqrt(rx * rx + rz * rz);
            if (rl > 1e-3f) {
                float ax = rz / rl, az = -rx / rl, along = ax * fx + az * fz;
                if (along < 0f) {
                    ax = -ax;
                    az = -az;
                    along = -along;
                }
                speed *= along;
                float to = (float) Math.atan2(ax, az), d = to - heading;
                while (d > Math.PI) d -= 2f * (float) Math.PI;
                while (d < -Math.PI) d += 2f * (float) Math.PI;
                heading += d * Math.min(1f, dt * 8f);
            } else speed = 0f;
        }
        // not into the sea: it stops at the water's edge
        if (City.overWater(x, z)) {
            x -= fx * speed * dt;
            z -= fz * speed * dt;
            speed = 0f;
        }
        y += (city.ground(x, z) - y) * Math.min(1f, dt * 12f);
    }

    /** The engine: hard all the way up, a little less near the top. */
    private void accelerate(float dt, float target) {
        float sp = speed / VMAX;
        gear = 1 + Math.min(4, (int) (sp * 5f));
        if (speed < target) {
            float acc = ACCEL * (1.15f - 0.55f * sp);
            speed = Math.min(target, speed + acc * dt);
        } else {
            // slowing for what the one driving wants, as a driver slows, not as one stamps on the brake
            float dec = speed - target > 1.5f ? EASE : COAST * 2f;
            speed = Math.max(target, speed - dec * dt);
        }
    }

    /**
     * The speed the driver picks: the top, a little less into a hard turn; held back by something
     * standing in its way (a wall, not a car: a car is rammed).
     */
    private float target(float fx, float fz, City city, Cars cars) {
        float t = VMAX * (1f - TURN_SLOWS * Math.abs(steer));
        // a car ahead does not hold it back: it is rammed out of the way (see Cars.push)
        // something standing ahead: looked for a few steps on, along the way it goes (the town's
        // own walls, not what lies loose in the way: that is shoved off)
        float[] p = new float[2];
        City.Extra loose = city.extra;
        city.extra = null;
        for (float d = 4f; d <= 16f; d += 3f) {
            p[0] = x + fx * d;
            p[1] = z + fz * d;
            if (city.push(p, 0.9f, y)) {
                t = Math.min(t, (float) Math.sqrt(2f * BRAKE * Math.max(0f, d - 3.5f)));
                break;
            }
        }
        city.extra = loose;
        // never quite nothing: nosing along, the wheel can still take it round what is in the way
        return Math.max(t, CREEP);
    }

    /** Pushes a round body of radius rr at (p[0], p[1]) out of it; true if it touched it. */
    boolean push(float[] p, float rr) {
        float fx = (float) Math.sin(heading), fz = (float) Math.cos(heading), ends = ends(), endR = endR();
        boolean hit = false;
        for (int e = -1; e <= 1; e += 2) {
            float cx = x + fx * e * ends, cz = z + fz * e * ends;
            float dx = p[0] - cx, dz = p[1] - cz, dd = dx * dx + dz * dz, m = rr + endR;
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
        float sx = boat ? 0f : SEAT_X, sz = boat ? HELM_Z : SEAT_Z;
        out[0] = x + c * sx + s * sz;
        out[1] = y + (boat ? HELM_Y : SEAT_Y);
        out[2] = z - s * sx + c * sz;
    }

    /** How far (x, z) is from the pickup's body: past its sides, or its ends; 0 within it. */
    float apart(float px, float pz) {
        float c = (float) Math.cos(heading), s = (float) Math.sin(heading), dx = px - x, dz = pz - z;
        float across = Math.max(0f, Math.abs(dx * c - dz * s) - halfWide()), along = Math.max(0f, Math.abs(dx * s + dz * c) - halfLong());
        return (float) Math.sqrt(across * across + along * along);
    }

    /**
     * The nearest place beside it from (px, pz), on any side or at any corner, so far off its body
     * (x, z into out): she gets in wherever she comes up to it, the nose or the tail as well.
     */
    void beside(float px, float pz, float off, float[] out) {
        float c = (float) Math.cos(heading), s = (float) Math.sin(heading), dx = px - x, dz = pz - z;
        final float HALF_WIDE = halfWide(), HALF_LONG = halfLong();
        float across = dx * c - dz * s, along = dx * s + dz * c;
        float a = Math.max(-HALF_WIDE, Math.min(HALF_WIDE, across)), b = Math.max(-HALF_LONG, Math.min(HALF_LONG, along));
        if (a == across && b == along) {
            // within its body: out by its nearest side
            if (HALF_WIDE - Math.abs(across) < HALF_LONG - Math.abs(along)) a = Math.signum(across == 0f ? -1f : across) * HALF_WIDE;
            else b = Math.signum(along == 0f ? 1f : along) * HALF_LONG;
        }
        float ox = across - a, oz = along - b, m = (float) Math.sqrt(ox * ox + oz * oz);
        if (m < 1e-3f) {
            ox = Math.abs(a) >= HALF_WIDE ? Math.signum(a) : 0f;
            oz = ox == 0f ? Math.signum(b) : 0f;
            m = 1f;
        }
        float la = a + ox / m * off, lb = b + oz / m * off;
        out[0] = x + c * la + s * lb;
        out[1] = z - s * la + c * lb;
    }

    /** Where she gets in and out: beside the driver's door, so far off it (x, z into out). */
    void door(float off, float[] out) {
        float c = (float) Math.cos(heading), s = (float) Math.sin(heading);
        float ax = -(halfWide() + off), az = boat ? HELM_Z : SEAT_Z;
        out[0] = x + c * ax + s * az;
        out[1] = z - s * ax + c * az;
    }

    // ---- drawing ----

    private static float px, py, pz;
    private static final float[] frame = new float[9], t = new float[9];

    void draw(DrawList d, float time) {
        if (boat) {
            sea(d, time);
            return;
        }
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
        // the load: crates of planks, two by two, the last one put on dropping into its place
        for (int k = 0; k < load; k++) {
            float lx = (k % 2 == 0 ? -0.45f : 0.45f), lz = k < 2 ? -0.88f : -1.92f;
            float drop = k == load - 1 ? Math.max(0f, 0.5f - loadedAgo * 3f) : 0f;
            crate(d, lx, 1.36f + drop, lz, k);
        }
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

    /** A crate of pale planks, banded, in its own frame (no skin on it: it is not the pickup's). */
    // the wake: so many dashes of foam behind along each arm of it, so far apart, the arms
    // spreading so, from this fast
    static final int WAKE = 8;
    static final float WAKE_STEP = 1.7f, WAKE_SPREAD = 0.4f, WAKE_FROM = 1.2f;
    static final int FOAM = 0xD6EAEC;

    /** A boat: the yacht, and its wake on the water as it goes; the foam at its bow. */
    private void sea(DrawList d, float time) {
        Island.yacht(d, x, z, heading, y - City.WATER, Island.freeStripe());
        float v = Math.abs(speed);
        if (v < WAKE_FROM) return;
        float k = Math.min(1f, (v - WAKE_FROM) / 8f);
        float bx = (float) Math.sin(course), bz = (float) Math.cos(course), sgn = Math.signum(speed);
        float fx = (float) Math.sin(heading), fz = (float) Math.cos(heading);
        float[] t = {bz, 0f, bx, 0f, 1f, 0f, -bx, 0f, bz};
        float sx = x - fx * sgn * (Island.HULL_HALF - 0.4f), sz = z - fz * sgn * (Island.HULL_HALF - 0.4f);
        // the dashes drift back as it goes, so the wake runs
        float run = (time * v * 0.5f) % WAKE_STEP;
        for (int i = 0; i < WAKE; i++) {
            float back = i * WAKE_STEP + run, wide = Island.BEAM * 0.8f + back * WAKE_SPREAD;
            float fade = k * (1f - back / (WAKE * WAKE_STEP));
            if (fade <= 0.02f) continue;
            for (int e = -1; e <= 1; e += 2) {
                float jit = 0.25f * (float) Math.sin(i * 2.3f + e);
                float px = sx - bx * sgn * back + bz * e * (wide + jit), pz = sz - bz * sgn * back - bx * e * (wide + jit);
                d.box(px, City.WATER + 0.03f, pz, 0.04f + 0.08f * fade, 0.012f, 0.25f + 0.2f * fade, t, FOAM, DrawList.MATTE);
            }
            // the churned water straight behind it, narrowing out
            if (i < WAKE / 2) {
                float px = sx - bx * sgn * back, pz = sz - bz * sgn * back;
                d.box(px, City.WATER + 0.025f, pz, 0.14f * fade + 0.04f, 0.01f, 0.22f, t, FOAM, DrawList.MATTE);
            }
        }
        if (speed > 4f) {
            // a curl of white at the bow
            for (int e = -1; e <= 1; e += 2) {
                float px = x + fx * (Island.HULL_HALF - 1.4f) + fz * e * (Island.BEAM + 0.1f);
                float pz = z + fz * (Island.HULL_HALF - 1.4f) - fx * e * (Island.BEAM + 0.1f);
                d.box(px, City.WATER + 0.05f, pz, 0.1f, 0.05f, 0.9f * k, t, FOAM, DrawList.MATTE);
            }
        }
    }

    private static void crate(DrawList d, float lx, float ly, float lz, int k) {
        float[] f = frame;
        float wx = px + f[0] * lx + f[1] * ly + f[2] * lz, wy = py + ly, wz = pz + f[6] * lx + f[7] * ly + f[8] * lz;
        int wood = k % 2 == 0 ? 0xC79B5E : 0xB88A52;
        d.box(wx, wy, wz, 0.4f, 0.4f, 0.48f, f, wood, DrawList.material(DrawList.WOOD, 40 + k));
        for (int e = -1; e <= 1; e += 2) {
            float bx = lz + e * 0.3f;
            d.box(px + f[0] * lx + f[2] * bx, wy, pz + f[6] * lx + f[8] * bx, 0.41f, 0.41f, 0.05f, f, 0x6E5434, DrawList.WOOD);
        }
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
