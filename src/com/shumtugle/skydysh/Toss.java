package com.shumtugle.skydysh;

/**
 * Her mallet (see Mallet): carried in her right hand, hanging from the fist and trailing behind
 * her, its head low. At a tap she uses it: a golem within her reach she strikes where she stands;
 * one further off, ahead of her and not too far, she throws at: she stops, turns after it as it
 * runs on, and the mallet flies after its head end over end, knocks the plate off its brow, and
 * comes back to her hand. With nothing to throw at, she throws all the same, straight ahead, and
 * it comes back empty.
 */
final class Toss {
    static final float LOCK = 0.8f, RELEASE = 0.33f;       // how long she stands to throw; when it leaves her hand
    static final float STRIKE_LOCK = 0.6f;                 // how long she stands to strike
    // within so far she strikes; ahead within so far she throws; ahead: the cosine off the eye's way
    static final float STRIKE = 3.6f, RANGE = 20f, AHEAD = 0.6f, MISS = 14f;
    static final float OUT_SPEED = 24f, BACK_TIME = 0.45f, SCALE = 1.6f, MIDDLE = 0.5f;
    static final int HELD = 0, OUT = 1, BACK = 2, WINDUP = 3;

    int state = HELD;
    int thrown;
    private float clock, at;
    private Chase aim;
    private boolean near, hit;
    private final float[] from = new float[3], pos = new float[3], to = new float[3];
    private float flight, spin;
    private final float[] hand = new float[3], elbow = new float[3];
    private final float[] axis = new float[3], base = new float[9], turn = new float[9];

    /** The golem a tap now would be for: within reach, or ahead of the eye and not too far; or null. */
    static Chase pick(Runner r, Chase[] golems) {
        float ax = (float) Math.sin(r.eyeYaw), az = (float) Math.cos(r.eyeYaw);
        Chase best = null;
        float bd = 1e9f;
        for (Chase g : golems) {
            if (g.done()) continue;
            float dx = g.x - r.x, dy = g.y - r.y, dz = g.z - r.z;
            float flat = (float) Math.sqrt(dx * dx + dz * dz), d = (float) Math.sqrt(flat * flat + dy * dy);
            boolean ahead = flat > 1e-3f && (dx * ax + dz * az) / flat > AHEAD;
            if (d < STRIKE || (ahead && d < RANGE)) {
                if (d < bd) {
                    bd = d;
                    best = g;
                }
            }
        }
        return best;
    }

    void update(float dt, Runner r, Chase[] golems, City city, boolean tap) {
        clock += dt;
        if (state == HELD) {
            if (!tap || r.inAir() || r.hidden) return;
            aim = pick(r, golems);
            hit = false;
            if (aim != null) {
                float dx = aim.x - r.x, dy = aim.y - r.y, dz = aim.z - r.z;
                near = dx * dx + dy * dy + dz * dz < STRIKE * STRIKE;
                r.lock(near ? STRIKE_LOCK : LOCK, aim.x, aim.z);
            } else {
                // nothing to throw at: straight on along the eye's way
                near = false;
                float ax = (float) Math.sin(r.eyeYaw), az = (float) Math.cos(r.eyeYaw);
                to[0] = r.x + ax * MISS;
                to[1] = r.y + 1.6f;
                to[2] = r.z + az * MISS;
                r.lock(LOCK, to[0], to[2]);
            }
            state = WINDUP;
            return;
        }
        if (state == WINDUP) {
            // it runs on: she turns after it as she swings
            if (aim != null) r.face(aim.x, aim.z);
            if (near) {
                // within reach: the blow lands at the height of the swing, the mallet kept in hand
                if (!hit && r.throwing() >= RELEASE) {
                    hit = true;
                    aim.head(to);
                    grip(r);
                    aim.take(r, city, to[0] - hand[0], to[2] - hand[2]);
                }
                if (r.throwing() < 0f) state = HELD;
                return;
            }
            if (r.throwing() < RELEASE) return;
            // out of her hand at the height of the swing
            grip(r);
            System.arraycopy(hand, 0, from, 0, 3);
            if (aim != null) aim.head(to);
            float dx = to[0] - from[0], dy = to[1] - from[1], dz = to[2] - from[2];
            flight = Math.max(0.15f, (float) Math.sqrt(dx * dx + dy * dy + dz * dz) / OUT_SPEED);
            at = clock;
            spin = 0f;
            thrown++;
            state = OUT;
            return;
        }
        spin += dt * 17f;
        float u = (clock - at) / (state == OUT ? flight : BACK_TIME);
        if (state == OUT) {
            // the head it was thrown at
            if (aim != null && !aim.done()) aim.head(to);
            flown(Math.min(1f, u), 0.08f);
            if (u >= 1f) {
                if (aim != null) aim.take(r, city, to[0] - from[0], to[2] - from[2]);
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
        boolean inHand = state == HELD || state == WINDUP;
        if (inHand && state == WINDUP) {
            // swung: the handle runs on from her forearm
            grip(r);
            hx = hand[0] - elbow[0];
            hy = hand[1] - elbow[1];
            hz = hand[2] - elbow[2];
        } else if (inHand) {
            // hanging from the fist, trailing behind her, swung a little with her forearm
            grip(r);
            float fx = hand[0] - elbow[0], fy = hand[1] - elbow[1], fz = hand[2] - elbow[2];
            float fl = Math.max(1e-3f, (float) Math.sqrt(fx * fx + fy * fy + fz * fz));
            hx = -0.45f * (float) Math.sin(r.heading) + 0.35f * fx / fl;
            hy = -1f + 0.35f * fy / fl;
            hz = -0.45f * (float) Math.cos(r.heading) + 0.35f * fz / fl;
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
        if (inHand) {
            System.arraycopy(base, 0, turn, 0, 9);
            cx = hand[0];
            cy = hand[1];
            cz = hand[2];
        } else {
            axis[0] = bx;
            axis[1] = 0f;
            axis[2] = bz;
            Chase.spun(axis, 0, spin, base, 0, turn);
            cx = pos[0];
            cy = pos[1];
            cz = pos[2];
        }
        // held, by the grip; flying, turning about its middle
        Mallet.draw(d, turn, cx, cy, cz, SCALE, inHand ? 0f : MIDDLE);
    }
}
