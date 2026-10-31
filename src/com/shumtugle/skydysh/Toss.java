package com.shumtugle.skydysh;

/**
 * Her hammer: carried in her right hand, the handle running on from the forearm, the head past
 * the fist. When a golem still loose is near enough, before her and not behind a wall, the
 * hammer flies at it by itself, end over end, finding the head wherever it has got to; it
 * knocks the plate off its brow, and comes back to her hand.
 */
final class Toss {
    static final float RANGE = 10f, CONE = 1.05f;          // how near, how far to either side of her way
    static final float OUT_SPEED = 26f, BACK_TIME = 0.55f, SCALE = 1.7f;
    static final int HELD = 0, OUT = 1, BACK = 2;

    int state = HELD;
    int thrown;
    private float clock, at;
    private Chase aim;
    private final float[] from = new float[3], pos = new float[3], to = new float[3];
    private float flight, spin;
    private final float[] pose = new float[12], hand = new float[3], elbow = new float[3];
    private final float[] axis = new float[3], base = new float[9], turn = new float[9];

    void update(float dt, Runner r, Mission m, City city) {
        clock += dt;
        if (state == HELD) {
            Chase g = pick(r, m, city);
            if (g == null) return;
            aim = g;
            grip(r);
            from[0] = hand[0];
            from[1] = hand[1];
            from[2] = hand[2];
            g.head(to);
            float dx = to[0] - from[0], dy = to[1] - from[1], dz = to[2] - from[2];
            flight = Math.max(0.3f, (float) Math.sqrt(dx * dx + dy * dy + dz * dz) / OUT_SPEED);
            at = clock;
            spin = 0f;
            thrown++;
            state = OUT;
            return;
        }
        spin += dt * 17f;
        float u = (clock - at) / (state == OUT ? flight : BACK_TIME);
        if (state == OUT) {
            // the head it was thrown at, wherever it has run to
            if (!aim.done()) aim.head(to);
            flown(Math.min(1f, u), 0.12f);
            if (u >= 1f) {
                aim.take(r, city, to[0] - from[0], to[2] - from[2]);
                System.arraycopy(pos, 0, from, 0, 3);
                at = clock;
                state = BACK;
            }
        } else {
            grip(r);
            System.arraycopy(hand, 0, to, 0, 3);
            flown(Math.min(1f, u), 0.06f);
            if (u >= 1f) state = HELD;
        }
    }

    /** The nearest golem still loose within reach, before her and in sight; none if no such. */
    private Chase pick(Runner r, Mission m, City city) {
        Chase best = null;
        float fx = (float) Math.sin(r.heading), fz = (float) Math.cos(r.heading);
        for (Chase g : m.golems) {
            if (g.done() || g.apart > RANGE) continue;
            float dx = g.x - r.x, dz = g.z - r.z, l = Math.max(0.1f, g.apart);
            if ((dx * fx + dz * fz) / l < (float) Math.cos(CONE)) continue;
            if (!clear(city, r.x, r.z, g.x, g.z)) continue;
            if (best == null || g.apart < best.apart) best = g;
        }
        return best;
    }

    /**
     * Whether no house stands between two points on the ground: every point along the way on a
     * street or a pavement, not within a block. Posts and palms do not stop it.
     */
    private static boolean clear(City city, float ax, float az, float bx, float bz) {
        for (int k = 1; k < 10; k++) {
            float u = k / 10f, px = ax + (bx - ax) * u, pz = az + (bz - az) * u;
            float ox = Math.abs(px - Math.round(px / City.CELL) * City.CELL);
            float oz = Math.abs(pz - Math.round(pz / City.CELL) * City.CELL);
            if (Math.min(ox, oz) > City.FRONT) return false;
        }
        return true;
    }

    /** Along the way from where it set off to where it is going, a little arc over it. */
    private void flown(float u, float arc) {
        float dx = to[0] - from[0], dy = to[1] - from[1], dz = to[2] - from[2];
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        for (int k = 0; k < 3; k++) pos[k] = from[k] + (to[k] - from[k]) * u;
        pos[1] += (float) Math.sin(Math.PI * u) * len * arc;
    }

    /** Her fist and the way her forearm points, as she is now. */
    private void grip(Runner r) {
        r.bone("hand_r", hand);
        r.bone("lowerarm_r", elbow);
    }

    void draw(DrawList d, Runner r) {
        float hx, hy, hz;
        if (state == HELD) {
            grip(r);
            hx = hand[0] - elbow[0];
            hy = hand[1] - elbow[1];
            hz = hand[2] - elbow[2];
        } else {
            // flying: the handle along the way it goes, turning end over end
            hx = to[0] - from[0];
            hy = 0f;
            hz = to[2] - from[2];
        }
        float l = Math.max(1e-3f, (float) Math.sqrt(hx * hx + hy * hy + hz * hz));
        hx /= l;
        hy /= l;
        hz /= l;
        // the barrel across: level, square to the handle
        float bx = -hz, bz = hx, bl = Math.max(1e-3f, (float) Math.sqrt(bx * bx + bz * bz));
        bx /= bl;
        bz /= bl;
        // the third way: barrel by handle
        float ux = 0f * hz - bz * hy, uy = bz * hx - bx * hz, uz = bx * hy - 0f * hx;
        base[0] = hx; base[1] = ux; base[2] = bx;
        base[3] = hy; base[4] = uy; base[5] = 0f;
        base[6] = hz; base[7] = uz; base[8] = bz;
        float cx, cy, cz;
        if (state == HELD) {
            System.arraycopy(base, 0, turn, 0, 9);
            // the grip in the fist
            float g = 0.31f * SCALE;
            cx = hand[0] + hx * g;
            cy = hand[1] + hy * g;
            cz = hand[2] + hz * g;
        } else {
            axis[0] = bx;
            axis[1] = 0f;
            axis[2] = bz;
            Chase.spun(axis, 0, spin, base, 0, turn);
            cx = pos[0];
            cy = pos[1];
            cz = pos[2];
        }
        pose[0] = cx;
        pose[1] = cy;
        pose[2] = cz;
        System.arraycopy(turn, 0, pose, 3, 9);
        Hammer.draw(d, pose, Hammer.PLAIN, SCALE);
    }
}
