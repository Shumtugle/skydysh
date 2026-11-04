package com.shumtugle.skydysh;

/**
 * The rail at the wheel: the street leads her, as on a string. In the town, going on with the
 * wheel let be (or nearly), a point a little ahead in her lane, on the right of the street's
 * middle, is aimed at, so the pickup keeps to the lane by itself, through the crossings and on.
 * The wheel turned near a crossing lays the rail round into the street that way, and the pickup
 * takes the corner by itself, slowing for it. The wheel turned hard between crossings lays it
 * round the other way: she turns about in the street. A car ahead in the lane, going slower:
 * the rail goes out into the other lane past it, if that is clear far enough, and back after;
 * if not, she slows behind it. The wheel turned hard anywhere else, or off the streets (a square,
 * a yard, the country), the rail is let go and the hand has the wheel alone.
 */
final class Rail {
    // the lane's middle off the street's; how far ahead it is aimed at, and more the faster;
    // how hard the wheel follows the aim
    static final float LANE = 2.2f, LOOK = 5f, LOOK_PER = 0.5f, GAIN = 2.4f;
    // how near a crossing a turn of the wheel lays the rail into the street that way; how far the
    // wheel must be over for it, for a turn about between crossings, and for the hand to have it alone
    static final float TURN_NEAR = 14f, TURN_PER = 1.3f, TURN_WHEEL = 0.3f, ABOUT_WHEEL = 0.92f, OWN_WHEEL = 0.7f;
    // how fast round a corner, and turning about
    static final float CORNER = 8.5f, ABOUT = 5.5f;
    // a car this near ahead in the lane is in the way; the other lane must be clear so far to go past
    static final float AHEAD = 22f, PAST = 30f;

    private static final int ALONG = 0, CORNERING = 1, ABOUT_TURN = 2;
    private int plan = ALONG;
    // the way the street runs (a unit step along x or z), the way into the new street, the
    // crossing turned at (or the point turned about at)
    private float ax, az, nx, nz, px, pz;
    /** Which lane: LANE on the right, -LANE out past a car. */
    private float lane = LANE;
    private float outFor;
    /** Whether the rail held the wheel last frame. */
    boolean on;
    /** The speed the rail asks for now (-1: none); how hard it brakes for it (0: not at all). */
    float cap = -1f, brake;
    private final float[] probe = new float[2];

    /** Let go: nothing planned, the right lane. */
    void reset() {
        plan = ALONG;
        lane = LANE;
        on = false;
        cap = -1f;
    }

    /** The wheel for the pickup now: hand is where the finger turns it (-1 to 1); off the rail, hand itself. */
    float wheel(Pickup p, float hand, boolean going, float dt, Cars cars) {
        cap = -1f;
        brake = 0f;
        on = false;
        float sp = p.speed;
        boolean town = p.z > City.SHORE * City.CELL - City.ROAD && p.z < City.EDGE_J * City.CELL + City.ROAD;
        if (!town || !going || sp < 1.2f) {
            if (sp < 0.5f) reset();
            return hand;
        }
        if (plan == ALONG) {
            // the way the street runs: the heading's nearest of the four, if it is near it
            float q = (float) Math.PI * 0.5f;
            int k = Math.round(p.heading / q);
            float off = wrap(p.heading - k * q);
            if (Math.abs(off) > 0.75f) return hand;
            ax = Math.round((float) Math.sin(k * q));
            az = Math.round((float) Math.cos(k * q));
            if (!onStreet(p)) return hand;
            float left = toCrossing(p);
            float h = Math.abs(hand);
            float near = TURN_NEAR + TURN_PER * Math.abs(sp);
            if (h > TURN_WHEEL && left < near && left > 1.5f) {
                // into the street that way at the crossing ahead, if there is one
                float rx = az, rz = -ax, s = Math.signum(hand);
                float tx = rx * s, tz = rz * s;
                int cj = Math.round(crossingZ(p) / City.CELL);
                if (City.road(cj, Math.round(tz))) {
                    nx = tx;
                    nz = tz;
                    px = crossingX(p);
                    pz = crossingZ(p);
                    plan = CORNERING;
                    lane = LANE;
                }
            } else if (h > ABOUT_WHEEL && left >= near && sp < 14f) {
                // turned about here: the rail round to the other side of the street, back the way she came
                nx = -ax;
                nz = -az;
                px = p.x;
                pz = p.z;
                plan = ABOUT_TURN;
                lane = LANE;
            } else if (h > OWN_WHEEL) {
                // the hand has it alone
                return hand;
            }
        }
        float look = LOOK + LOOK_PER * Math.abs(sp), tx, tz;
        if (plan == ALONG) {
            traffic(p, cars, dt);
            float rx = az, rz = -ax;
            float lineX = Math.abs(az) > 0.5f ? Math.round(p.x / City.CELL) * City.CELL : p.x;
            float lineZ = Math.abs(ax) > 0.5f ? Math.round(p.z / City.CELL) * City.CELL : p.z;
            tx = lineX + ax * look + rx * lane;
            tz = lineZ + az * look + rz * lane;
        } else {
            // round into the new way: aimed at its lane past the corner (or past where she turned about)
            float rx = nz, rz = -nx;
            float bx = plan == CORNERING ? px : (Math.abs(nz) > 0.5f ? Math.round(px / City.CELL) * City.CELL : px);
            float bz = plan == CORNERING ? pz : (Math.abs(nx) > 0.5f ? Math.round(pz / City.CELL) * City.CELL : pz);
            tx = bx + nx * look + rx * LANE;
            tz = bz + nz * look + rz * LANE;
            cap = plan == CORNERING ? CORNER : ABOUT;
            // too fast for it: the brakes, till it is slow enough
            if (sp > cap + 1.5f) brake = Math.min(1f, (sp - cap) / 8f);
            float want = (float) Math.atan2(nx, nz);
            if (Math.abs(wrap(want - p.heading)) < 0.2f) {
                // round: on along the new street
                plan = ALONG;
                ax = nx;
                az = nz;
            }
        }
        float aim = (float) Math.atan2(tx - p.x, tz - p.z);
        float w = Math.max(-1f, Math.min(1f, wrap(aim - p.heading) * GAIN));
        on = true;
        // a little of the hand still heard along the street, none in a turn
        return plan == ALONG ? Math.max(-1f, Math.min(1f, w + hand * 0.5f)) : w;
    }

    /** A car in the way ahead: out past it if the other lane is clear, back in once by it; slower behind it if not. */
    private void traffic(Pickup p, Cars cars, float dt) {
        float blocked = firstCar(cars, LANE, 4f, AHEAD);
        if (lane == LANE) {
            if (blocked < AHEAD) {
                if (toCrossing(p) > blocked + 8f && firstCar(cars, -LANE, -4f, PAST) >= PAST) {
                    lane = -LANE;
                    outFor = 0f;
                } else cap = Math.max(2f, blocked * 0.4f);
            }
        } else {
            outFor += dt;
            // back in once the right lane is clear beside and a little ahead; at once, if one comes the other way
            if (outFor > 1.5f && firstCar(cars, LANE, -6f, 10f) >= 10f) lane = LANE;
            if (firstCar(cars, -LANE, 4f, 16f) < 16f) lane = LANE;
        }
    }

    /** How far ahead along the way (from `from` to `to` metres) the first car in the lane at offset off is; `to` if none. */
    private float firstCar(Cars cars, float off, float from, float to) {
        float rx = az, rz = -ax;
        // from where she is along the street
        for (float s = from; s < to; s += 2f) {
            probe[0] = baseX + ax * s + rx * off;
            probe[1] = baseZ + az * s + rz * off;
            float ox = probe[0], oz = probe[1];
            if (cars.push(probe, 1.2f) || Math.abs(probe[0] - ox) + Math.abs(probe[1] - oz) > 1e-3f) return s;
        }
        return to;
    }

    private float baseX, baseZ;

    /** Whether she is on a street running the way she goes, not out across a square or a yard. */
    private boolean onStreet(Pickup p) {
        baseX = Math.abs(az) > 0.5f ? Math.round(p.x / City.CELL) * City.CELL : p.x;
        baseZ = Math.abs(ax) > 0.5f ? Math.round(p.z / City.CELL) * City.CELL : p.z;
        // the street's middle and her along it, for the traffic's look ahead
        if (Math.abs(az) > 0.5f) {
            baseZ = p.z;
            return Math.abs(p.x - baseX) < City.ROAD + 1f;
        }
        baseX = p.x;
        return Math.abs(p.z - baseZ) < City.ROAD + 1f;
    }

    /** How far ahead the next crossing is. */
    private float toCrossing(Pickup p) {
        return Math.abs(az) > 0.5f ? Math.abs(crossingZ(p) - p.z) : Math.abs(crossingX(p) - p.x);
    }

    private float crossingX(Pickup p) {
        if (Math.abs(ax) < 0.5f) return Math.round(p.x / City.CELL) * City.CELL;
        float c = ax > 0f ? (float) Math.ceil(p.x / City.CELL) : (float) Math.floor(p.x / City.CELL);
        return c * City.CELL;
    }

    private float crossingZ(Pickup p) {
        if (Math.abs(az) < 0.5f) return Math.round(p.z / City.CELL) * City.CELL;
        float c = az > 0f ? (float) Math.ceil(p.z / City.CELL) : (float) Math.floor(p.z / City.CELL);
        return c * City.CELL;
    }

    private static float wrap(float a) {
        while (a > Math.PI) a -= 2f * (float) Math.PI;
        while (a < -Math.PI) a += 2f * (float) Math.PI;
        return a;
    }
}
