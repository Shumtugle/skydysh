package com.shumtugle.skydysh;

/**
 * The rail at the wheel: the street leads her, as on a string, but only while the hand lets it.
 * In the town, going on with the wheel let be, a point a little ahead in her lane, on the right of
 * the street's middle, is aimed at, so the pickup keeps to the lane by itself, through the
 * crossings and on; a car ahead in the lane, going slower, is passed in the other lane if that is
 * clear far enough, and if not she slows behind it. The moment the hand turns the wheel, the rail
 * gives way to it, all of it a little further over: the hand is never overruled. What the rail
 * still does for the hand: turned hard near a crossing, into a street that way, it brakes for the
 * corner and holds her to a speed the corner can be made at, till the wheel is let back.
 */
final class Rail {
    // the lane's middle off the street's; how far ahead it is aimed at, and more the faster;
    // how hard the wheel follows the aim
    static final float LANE = 2.2f, LOOK = 5f, LOOK_PER = 0.5f, GAIN = 2.4f;
    // the hand turned so far has the wheel alone; less, it shares it with the rail, as much as it is turned
    static final float HAND_ALL = 0.25f;
    // how near a crossing a hard turn of the wheel is taken for a corner (and more the faster); how hard
    static final float TURN_NEAR = 14f, TURN_PER = 1.3f, TURN_WHEEL = 0.5f;
    // how fast round a corner; how long at most it is held to that
    static final float CORNER = 10f, CORNER_FOR = 3f;
    // a car this near ahead in the lane is in the way; the other lane must be clear so far to go past
    static final float AHEAD = 22f, PAST = 30f;

    // the way the street runs (a unit step along x or z)
    private float ax, az;
    /** Which lane: LANE on the right, -LANE out past a car. */
    private float lane = LANE;
    private float outFor;
    /** How long she has been held to the corner's speed (-1: not). */
    private float cornerFor = -1f;
    /** Whether the rail held the wheel last frame. */
    boolean on;
    /** The speed the rail asks for now (-1: none); how hard it brakes for it (0: not at all). */
    float cap = -1f, brake;
    private final float[] probe = new float[2];

    /** Let go: nothing planned, the right lane. */
    void reset() {
        lane = LANE;
        on = false;
        cap = -1f;
        cornerFor = -1f;
    }

    /** The wheel for the pickup now: hand is where the finger turns it (-1 to 1); off the rail, hand itself. */
    float wheel(Pickup p, float hand, boolean going, float dt, Cars cars) {
        cap = -1f;
        brake = 0f;
        on = false;
        float sp = p.speed, h = Math.abs(hand);
        boolean town = p.z > City.SHORE * City.CELL - City.ROAD && p.z < City.EDGE_J * City.CELL + City.ROAD;
        if (!town || !going || sp < 1.2f) {
            if (sp < 0.5f) reset();
            return hand;
        }
        // round a corner: held to its speed while the wheel stays over, whichever way she now faces
        if (cornerFor >= 0f) {
            cornerFor += dt;
            if (h < TURN_WHEEL * 0.6f || cornerFor > CORNER_FOR) cornerFor = -1f;
            else {
                slowFor(sp);
                return hand;
            }
        }
        // the way the street runs: the heading's nearest of the four, if it is near it
        float q = (float) Math.PI * 0.5f;
        int k = Math.round(p.heading / q);
        float off = wrap(p.heading - k * q);
        if (Math.abs(off) > 0.75f) return hand;
        ax = Math.round((float) Math.sin(k * q));
        az = Math.round((float) Math.cos(k * q));
        if (!onStreet(p)) return hand;
        if (h > TURN_WHEEL && toCrossing(p) < TURN_NEAR + TURN_PER * sp) {
            // a hard turn near a crossing, into a street that way, if there is one: slowed for the corner
            float tz = -ax * Math.signum(hand);
            int cj = Math.round(crossingZ(p) / City.CELL);
            if (City.road(cj, Math.round(tz))) {
                cornerFor = 0f;
                lane = LANE;
                slowFor(sp);
                return hand;
            }
        }
        if (h >= HAND_ALL) {
            // the hand has it alone; back in the right lane once it lets go
            lane = LANE;
            return hand;
        }
        traffic(p, cars, dt);
        float look = LOOK + LOOK_PER * Math.abs(sp);
        float rx = az, rz = -ax;
        float lineX = Math.abs(az) > 0.5f ? Math.round(p.x / City.CELL) * City.CELL : p.x;
        float lineZ = Math.abs(ax) > 0.5f ? Math.round(p.z / City.CELL) * City.CELL : p.z;
        float tx = lineX + ax * look + rx * lane, tz = lineZ + az * look + rz * lane;
        float aim = (float) Math.atan2(tx - p.x, tz - p.z);
        float w = Math.max(-1f, Math.min(1f, wrap(aim - p.heading) * GAIN));
        on = true;
        // the hand a little over: it and the rail, shared as far as it is turned
        float s = h / HAND_ALL;
        return Math.max(-1f, Math.min(1f, w * (1f - s) + hand * s));
    }

    /** Held to the corner's speed: the brakes, while it is faster than that. */
    private void slowFor(float sp) {
        cap = CORNER;
        if (sp > CORNER + 1.5f) brake = Math.min(1f, (sp - CORNER) / 8f);
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
