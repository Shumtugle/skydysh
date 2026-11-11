package com.shumtugle.skydysh;

/**
 * What a pickup carries in its bed, and the one thing of it in her hands. A thing belongs to its
 * pickup, not to her: she takes it out, does the work with it and puts it back; not put back (she
 * takes the wheel of another, a job is over, she takes up a tool of her own), it goes back of
 * itself. What a bed holds is the job's to say: a job's pickups hold what the job wants, for the
 * job only; between jobs, the ladder.
 *
 * The ladder: carried on her shoulder; a tap before a wall sets it up against it, from her feet to
 * the roof's edge, and she climbs it as any; put back, it is taken down.
 * The rope: carried coiled in her hand; a tap by the pickup to be towed ties it, from the pickup it
 * came out of to that one (see Tow).
 * The charge: on a call out, for the heap in the pass; carried in her hand, set against the heap
 * (see Rescue), it does not come back: the bed has another.
 */
final class Kit {
    static final int NONE = 0, LADDER = 1, ROPE = 2, CHARGE = 3;
    /** The longest the ladder reaches up a wall; the nearest and farthest the wall may be before her. */
    static final float LADDER_LONG = 26f, WALL_FROM = 0.3f, WALL_TO = 2.2f;
    /** How near the back of a pickup's bed she stands for its ring of things. */
    static final float BED_NEAR = 3.4f;

    /** What is in her hands, and the pickup it came out of. */
    int held = NONE;
    Pickup from;
    /** The ladder set against a wall (and the pickup it belongs to): its foot on the wall, bottom, top, the wall's way out. */
    boolean set;
    Pickup setFrom;
    float lx, lz, bottom, top, nx, nz;
    /** The rope tied between two pickups, for the tow (put back once the tow is over). */
    boolean tied;
    Pickup tiedFrom;

    /** Whether anything of a bed is out of it: in her hands, set up, or tied. */
    boolean out() {
        return held != NONE || set || tied;
    }

    /** Whether thing k of pickup p is out of its bed now. */
    boolean outOf(Pickup p, int k) {
        return held == k && from == p || k == LADDER && set && setFrom == p || k == ROPE && tied && tiedFrom == p;
    }

    /** Thing k out of pickup p's bed into her hands (anything else she held back first). */
    void take(int k, Pickup p) {
        held = k;
        from = p;
    }

    /** What she holds back in its bed, and the ladder taken down; the rope, while it tows, left on. */
    void putBackHands(City city) {
        held = NONE;
        from = null;
        if (set) city.unsetLadder();
        set = false;
        setFrom = null;
    }

    /** Everything out back in its bed: what she holds, the ladder taken down, the rope untied. */
    void putBack(City city) {
        held = NONE;
        from = null;
        if (set) city.unsetLadder();
        set = false;
        setFrom = null;
        tied = false;
        tiedFrom = null;
    }

    /** Whether a wall with a roof in the ladder's reach stands right before her (within a step). */
    boolean wallBefore(Runner r, City city) {
        float fx = (float) Math.sin(r.heading), fz = (float) Math.cos(r.heading);
        float ax = Math.abs(fx) > Math.abs(fz) ? Math.signum(fx) : 0f, az = ax == 0f ? Math.signum(fz) : 0f;
        for (float d = WALL_FROM; d <= WALL_TO; d += 0.1f) {
            float g = city.ground(r.x + ax * d, r.z + az * d, r.y + 200f, 0f);
            if (g >= r.y + 2f) return g - r.y <= LADDER_LONG;
        }
        return false;
    }

    /**
     * The ladder in her hands set up against the wall before her, if there is one near enough with
     * a roof within its reach: true if it is.
     */
    boolean setLadder(Runner r, City city) {
        if (held != LADDER) return false;
        float fx = (float) Math.sin(r.heading), fz = (float) Math.cos(r.heading);
        // the wall square to where she faces, as the town's walls are
        float ax = Math.abs(fx) > Math.abs(fz) ? Math.signum(fx) : 0f, az = ax == 0f ? Math.signum(fz) : 0f;
        float feet = r.y;
        for (float d = WALL_FROM; d <= WALL_TO; d += 0.1f) {
            float px = r.x + ax * d, pz = r.z + az * d, g = city.ground(px, pz, feet + 200f, 0f);
            if (g < feet + 2f) continue;
            if (g - feet > LADDER_LONG) return false;
            lx = px - ax * 0.12f;
            lz = pz - az * 0.12f;
            bottom = feet;
            top = g;
            nx = -ax;
            nz = -az;
            set = true;
            setFrom = from;
            held = NONE;
            from = null;
            city.setLadder(lx, lz, bottom, top, nx, nz);
            return true;
        }
        return false;
    }

    /** The things of a bed, as the job has them: into out; how many. */
    static int bed(boolean towing, boolean blasting, int[] out) {
        int n = 0;
        if (blasting) out[n++] = CHARGE;
        if (towing) out[n++] = ROPE;
        out[n++] = LADDER;
        return n;
    }

    /** The sign of each thing in a ring. */
    static int sign(int k) {
        return k == ROPE ? Badges.ROPE : k == CHARGE ? Badges.CHARGE : Badges.LADDER;
    }

    // ---- drawn ----
    static final int RAIL = 0xB9C0C6, RUNG = 0x9AA2A8, HEMP = 0xC9A86A, HEMP_DARK = 0x9A7E48;

    private final float[] hand = new float[3];

    /** What she holds, in her hands; the ladder where it is set up. */
    void draw(DrawList d, Runner r) {
        if (set) ladder(d, lx + nx * 0.18f, lz + nz * 0.18f, bottom, top + 0.9f, -nz, nx);
        if (held == LADDER) {
            // on her right shoulder, along the way she faces, its front a little up
            r.bone("hand_r", hand);
            float fx = (float) Math.sin(r.heading), fz = (float) Math.cos(r.heading), sx = fz, sz = -fx;
            float cx = hand[0] + sx * 0.1f, cy = hand[1] + 0.55f, cz = hand[2] + sz * 0.1f;
            for (float t = -2.2f; t <= 2.2f; t += 0.25f) {
                float y = cy + t * 0.18f;
                for (int e = -1; e <= 1; e += 2)
                    d.box(cx + fx * t + sx * e * 0.28f, y, cz + fz * t + sz * e * 0.28f, 0.05f, 0.05f, 0.05f, null, RAIL, DrawList.POLISHED);
            }
            for (float t = -2.0f; t <= 2.0f; t += 0.5f)
                d.box(cx + fx * t, cy + t * 0.18f, cz + fz * t, Math.abs(sx) * 0.28f + 0.03f, 0.025f, Math.abs(sz) * 0.28f + 0.03f, null, RUNG,
                        DrawList.POLISHED);
        } else if (held == ROPE) {
            r.bone("hand_l", hand);
            coil(d, hand[0], hand[1] - 0.25f, hand[2], r.heading);
        } else if (held == CHARGE) {
            r.bone("hand_l", hand);
            charge(d, hand[0], hand[1] - 0.2f, hand[2], r.heading, -1f);
        }
    }

    /** A ladder up from (x, bottom, z) to top, its rungs across along (sx, sz). */
    static void ladder(DrawList d, float x, float z, float bottom, float top, float sx, float sz) {
        float h = (top - bottom) * 0.5f, w = 0.36f;
        for (int e = -1; e <= 1; e += 2)
            d.box(x + sx * e * w, bottom + h, z + sz * e * w, 0.05f, h, 0.05f, null, RAIL, DrawList.POLISHED);
        for (float y = bottom + 0.35f; y < top - 0.1f; y += 0.42f)
            d.box(x, y, z, Math.abs(sx) * w + 0.03f, 0.03f, Math.abs(sz) * w + 0.03f, null, RUNG, DrawList.POLISHED);
    }

    // the charge: red sticks, a black band round them, the fuse
    static final int STICK = 0xC8322A, BAND = 0x1B1B20, FUSE = 0xD9C9A6;

    /**
     * A charge at (x, y, z), upright, turned the way a: three red sticks bound with a black band, a
     * pale fuse out of its top; lit, (0 to 1 the time left) a spark at the fuse's end.
     */
    static void charge(DrawList d, float x, float y, float z, float a, float lit) {
        float sx = (float) Math.cos(a), sz = -(float) Math.sin(a), fx = (float) Math.sin(a), fz = (float) Math.cos(a);
        for (int k = -1; k <= 1; k++)
            d.box(x + sx * k * 0.11f, y, z + sz * k * 0.11f, 0.055f, 0.28f, 0.055f, null, STICK, DrawList.PAINT);
        d.box(x, y + 0.1f, z, Math.abs(sx) * 0.18f + Math.abs(fx) * 0.07f, 0.04f, Math.abs(sz) * 0.18f + Math.abs(fz) * 0.07f, null, BAND, DrawList.PLAIN);
        float fl = lit >= 0f ? 0.08f + 0.22f * lit : 0.3f;
        d.box(x, y + 0.28f + fl * 0.5f, z, 0.015f, fl * 0.5f, 0.015f, null, FUSE, DrawList.PLAIN);
        if (lit >= 0f) d.box(x, y + 0.28f + fl + 0.03f, z, 0.05f, 0.05f, 0.05f, null, 0xFFD45A, DrawList.GLOW);
    }

    /** A coil of rope hanging upright at (x, y, z), facing the way a. */
    static void coil(DrawList d, float x, float y, float z, float a) {
        float sx = (float) Math.cos(a), sz = -(float) Math.sin(a);
        for (int k = 0; k < 12; k++) {
            float u = k * (float) Math.PI / 6f, rr = 0.26f;
            float px = x + sx * (float) Math.cos(u) * rr, py = y + (float) Math.sin(u) * rr, pz = z + sz * (float) Math.cos(u) * rr;
            d.box(px, py, pz, 0.06f, 0.06f, 0.06f, null, k % 2 == 0 ? HEMP : HEMP_DARK, DrawList.PAINT);
        }
    }

    /** The things in a pickup's bed: the ladder along it, the rope coiled at its back; what is out of it not there. */
    void inBed(DrawList d, Pickup p, boolean towing, boolean blasting) {
        float fx = (float) Math.sin(p.heading), fz = (float) Math.cos(p.heading), sx = fz, sz = -fx;
        float bx = p.x - fx * 1.55f, by = p.y + 0.95f, bz = p.z - fz * 1.55f;
        if (!outOf(p, LADDER)) {
            for (float t = -1.3f; t <= 1.31f; t += 0.26f)
                for (int e = -1; e <= 1; e += 2)
                    d.box(bx + fx * t + sx * (e * 0.28f + 0.25f), by, bz + fz * t + sz * (e * 0.28f + 0.25f), 0.045f, 0.045f, 0.045f, null, RAIL,
                            DrawList.POLISHED);
        }
        if (blasting && !outOf(p, CHARGE)) {
            // lying in the bed by its front corner
            float cx = bx + sx * 0.45f + fx * 0.6f, cz = bz + sz * 0.45f + fz * 0.6f;
            charge(d, cx, by + 0.05f, cz, p.heading + 0.6f, -1f);
        }
        if (towing && !outOf(p, ROPE)) {
            // a ring lying flat at the bed's back corner
            float cx = bx - sx * 0.45f - fx * 0.8f, cz = bz - sz * 0.45f - fz * 0.8f;
            for (int k = 0; k < 10; k++) {
                float u = k * (float) Math.PI / 5f, cu = (float) Math.cos(u) * 0.3f, su = (float) Math.sin(u) * 0.3f;
                d.box(cx + fx * cu + sx * su, by - 0.1f, cz + fz * cu + sz * su, 0.07f, 0.06f, 0.07f, null, k % 2 == 0 ? HEMP : HEMP_DARK,
                        DrawList.PAINT);
            }
        }
    }
}
