package com.shumtugle.skydysh;

/**
 * The call out: the road to the village over the hills is blocked in the pass, rocks and fallen
 * trunks across it. She takes a duty pickup out of the town by the road north, clears the pass
 * (the pickup shoves what it runs into off the road, the mallet knocks it aside on foot) and gets
 * the pickup through it, and drives on to the village square. The clock runs from the start to the square; there is no
 * losing it, only a time. The rocks and trunks slide and turn as they are struck and come to
 * rest where they come; she and the pickup cannot go through them.
 */
final class Rescue implements City.Extra {
    static final int REWARD = 60000;
    // the steps: a pickup to be found, the pass to be cleared, the village to be got to, done
    static final int PICKUP = 0, PASS = 1, VILLAGE = 2, DONE = 3;

    /** Whether mission n is a call out: the fourth, and every fourth after it. */
    static boolean its(int n) {
        return n % 4 == 0;
    }

    int step = PICKUP;
    float clock, doneAt = -1f;

    // what blocks the pass: where each lies (x, z), how it is turned, its half sizes, how it moves,
    // its colour and stuff (rock or wood), how far round it reaches for what runs into it
    static final int MOST = 10;
    final float[] x = new float[MOST], z = new float[MOST], yaw = new float[MOST], hx = new float[MOST], hy = new float[MOST],
            hz = new float[MOST], vx = new float[MOST], vz = new float[MOST], spin = new float[MOST], r = new float[MOST];
    private final int[] color = new int[MOST];
    private final boolean[] wood = new boolean[MOST];
    private int n;
    // shoved by the pickup only going faster than this; how fast what is struck slides off, at most
    static final float SHOVE_FROM = 2.5f, KNOCK = 9f, DRAG = 2.6f;
    // past the pass by so much, at the wheel, the way is through
    static final float PAST = 8f;
    // a trunk is round bodies along it, so far apart from its middle, so big
    static final float TRUNK_STEP = 1.3f, TRUNK_R = 0.6f;
    private boolean pushing;

    Rescue() {
        float px = City.routeX(City.PASS_Z), pz = City.PASS_Z;
        java.util.Random rnd = new java.util.Random(0xFA11L);
        // three trunks lying across the pass, rocks in the gaps and against them, wall to wall
        for (int k = 0; k < MOST; k++) {
            boolean trunk = k < 3;
            wood[k] = trunk;
            if (trunk) {
                x[k] = px + (k - 1) * 4.6f;
                z[k] = pz + (k == 1 ? 0.8f : -0.6f);
                yaw[k] = 1.5708f + (k - 1) * 0.18f;
                hx[k] = 0.35f;
                hy[k] = 0.35f;
                hz[k] = 1.95f;
                color[k] = 0x5E4A38;
                r[k] = TRUNK_R;
            } else {
                int q = k - 3;
                x[k] = px + (q - 3) * 2.3f + (rnd.nextFloat() - 0.5f) * 0.6f;
                z[k] = pz + (q % 2 == 0 ? -2.4f : 2.6f) + (rnd.nextFloat() - 0.5f) * 0.8f;
                yaw[k] = rnd.nextFloat() * 3f;
                float s = 0.8f + rnd.nextFloat() * 0.5f;
                hx[k] = s;
                hy[k] = s * 0.7f;
                hz[k] = s * 0.85f;
                color[k] = Street.mix(0x8E8476, 0xA79C8B, rnd.nextFloat());
                r[k] = Math.max(hx[k], hz[k]);
            }
        }
        n = MOST;
    }

    /** How many round bodies piece k is (a trunk three, a rock one); the c-th one's middle into at. */
    private int bodies(int k) {
        return wood[k] ? 3 : 1;
    }

    private final float[] at = new float[2];

    private void body(int k, int c, float[] out) {
        if (!wood[k]) {
            out[0] = x[k];
            out[1] = z[k];
            return;
        }
        float off = (c - 1) * TRUNK_STEP;
        out[0] = x[k] + (float) Math.sin(yaw[k]) * off;
        out[1] = z[k] + (float) Math.cos(yaw[k]) * off;
    }

    boolean over() {
        return step == DONE;
    }

    /** Whether the road through the pass is clear: nothing left lying across it (the stand tells it). */
    boolean cleared() {
        for (int k = 0; k < n; k++) for (int b = 0; b < bodies(k); b++) {
            body(k, b, at);
            if (Math.abs(at[1] - City.PASS_Z) > 14f) continue;
            if (Math.abs(at[0] - City.routeX(at[1])) < City.ROUTE_HALF + r[k] * 0.4f) return false;
        }
        return true;
    }

    /** A step on: the duty pickups about, the one she drives (or none), where she is. */
    void update(float dt, Duty duty, Pickup driving, Runner rn, City city, World.Host host) {
        slide(dt, city);
        if (step == DONE) return;
        clock += dt;
        float ax = driving != null ? driving.x : rn.x, az = driving != null ? driving.z : rn.z;
        if (step == PICKUP && (driving != null || az > City.PASS_Z - 120f)) step = PASS;
        // through the pass at the wheel: the way is open (shoved through, or round what is left)
        if (step == PASS && driving != null && driving.z > City.PASS_Z + PAST) {
            step = VILLAGE;
            host.sound(Synth.CHIME, 0.5f, 1.3f);
            host.buzz(60);
        }
        if (step == VILLAGE && Math.abs(ax - City.villageX()) < City.SQUARE_HALF && Math.abs(az - City.VILLAGE_Z) < City.SQUARE_HALF) {
            step = DONE;
            doneAt = clock;
        }
    }

    /** What was struck sliding and turning to rest, against the town's walls (the hills, the pass's rock). */
    private void slide(float dt, City city) {
        float slow = (float) Math.exp(-DRAG * dt);
        float[] p = new float[2];
        for (int k = 0; k < n; k++) {
            if (vx[k] * vx[k] + vz[k] * vz[k] < 1e-4f && Math.abs(spin[k]) < 1e-3f) continue;
            x[k] += vx[k] * dt;
            z[k] += vz[k] * dt;
            yaw[k] += spin[k] * dt;
            vx[k] *= slow;
            vz[k] *= slow;
            spin[k] *= slow;
            p[0] = x[k];
            p[1] = z[k];
            pushing = true;
            if (city.push(p, r[k] * 0.8f, 0.2f)) {
                x[k] = p[0];
                z[k] = p[1];
                vx[k] *= 0.3f;
                vz[k] *= 0.3f;
            }
            pushing = false;
            // and off one another
            for (int o = 0; o < n; o++) {
                if (o == k) continue;
                float dx = x[k] - x[o], dz = z[k] - z[o], m = (r[k] + r[o]) * 0.7f, dd = dx * dx + dz * dz;
                if (dd >= m * m || dd < 1e-6f) continue;
                float l = (float) Math.sqrt(dd), push = (m - l) * 0.5f;
                x[k] += dx / l * push;
                z[k] += dz / l * push;
                x[o] -= dx / l * push;
                z[o] -= dz / l * push;
                vx[o] += vx[k] * 0.3f;
                vz[o] += vz[k] * 0.3f;
            }
        }
    }

    /**
     * The pickup she drives, about to move on: whatever it is going into fast enough is shoved off
     * ahead of it and aside, out of its way at once (it is not stopped).
     */
    void ram(Pickup pk, float dt) {
        if (Math.abs(pk.speed) < SHOVE_FROM) return;
        float sg = Math.signum(pk.speed), fx = (float) Math.sin(pk.heading) * sg, fz = (float) Math.cos(pk.heading) * sg;
        float v = Math.abs(pk.speed);
        for (int e = 0; e < 2; e++) {
            float reach = e == 0 ? Pickup.ENDS + v * dt : 0f;
            float cx = pk.x + fx * reach, cz = pk.z + fz * reach;
            for (int k = 0; k < n; k++) for (int b = 0; b < bodies(k); b++) {
                body(k, b, at);
                float dx = at[0] - cx, dz = at[1] - cz, m = r[k] + Pickup.END_R + 0.2f, dd = dx * dx + dz * dz;
                if (dd >= m * m) continue;
                float l = (float) Math.sqrt(dd) + 1e-4f;
                // out of the way now, and off: along the pickup's way, and aside from where it was struck
                x[k] += dx / l * (m - l);
                z[k] += dz / l * (m - l);
                // thrown off to the side it lies on (either, if dead ahead), a little on along the way
                float push = Math.max(v * 0.9f, 4f), across = dx * fz - dz * fx;
                float side = Math.abs(across) > 0.2f ? Math.signum(across) : (k % 2 == 0 ? 1f : -1f);
                vx[k] = fx * push * 0.45f + fz * side * push * 0.8f;
                vz[k] = fz * push * 0.45f - fx * side * push * 0.8f;
                spin[k] = (dx * fz - dz * fx) / l * push * 0.3f;
                hit++;
            }
        }
    }

    /** How many times something has been struck (for the world to hear it). */
    int hit;

    /** A blow of the mallet, on foot, at (bx, bz), facing (fx, fz): what lies within reach before her is knocked off. */
    void strike(float bx, float bz, float fx, float fz) {
        for (int k = 0; k < n; k++) {
            float dx = x[k] - bx, dz = z[k] - bz, dd = (float) Math.sqrt(dx * dx + dz * dz);
            if (dd > r[k] + 2.2f || dx * fx + dz * fz < -0.3f) continue;
            float l = dd + 1e-4f;
            vx[k] = fx * KNOCK * 0.6f + dx / l * KNOCK * 0.6f;
            vz[k] = fz * KNOCK * 0.6f + dz / l * KNOCK * 0.6f;
            spin[k] = (dx * fz - dz * fx) / l * 2.5f;
            hit++;
        }
    }

    /** What lies in the pass, in her way and the pickup's (see City.Extra). */
    public boolean push(float[] p, float rr, float feet) {
        if (pushing) return false;
        boolean any = false;
        for (int k = 0; k < n; k++) {
            if (feet > hy[k] * 2f) continue;
            for (int b = 0; b < bodies(k); b++) {
                body(k, b, at);
                float dx = p[0] - at[0], dz = p[1] - at[1], m = rr + r[k] * 0.85f, dd = dx * dx + dz * dz;
                if (dd >= m * m || dd < 1e-8f) continue;
                float l = (float) Math.sqrt(dd);
                p[0] = at[0] + dx / l * m;
                p[1] = at[1] + dz / l * m;
                any = true;
            }
        }
        return any;
    }

    void draw(DrawList d, float ex, float ez, float fx, float fz) {
        for (int k = 0; k < n; k++) {
            float ox = x[k] - ex, oz = z[k] - ez;
            if (ox * fx + oz * fz < -6f || ox * ox + oz * oz > 200f * 200f) continue;
            float c = (float) Math.cos(yaw[k]), s = (float) Math.sin(yaw[k]);
            float[] f = {c, 0f, s, 0f, 1f, 0f, -s, 0f, c};
            if (wood[k]) {
                d.box(x[k], hy[k], z[k], hx[k], hy[k], hz[k], f, color[k], DrawList.material(DrawList.TRUNK, 60 + k * 30));
                // its broken end, paler
                d.box(x[k] + s * hz[k], hy[k], z[k] + c * hz[k], hx[k] * 0.9f, hy[k] * 0.9f, 0.04f, f, 0xC9A57A, DrawList.WOOD);
            } else {
                d.box(x[k], hy[k], z[k], hx[k], hy[k], hz[k], f, color[k], DrawList.STONE);
            }
        }
    }

    /** Where the way goes now, into out (x, y, z); false if there is nowhere to point. */
    boolean goal(Duty duty, Pickup driving, Runner rn, float[] out) {
        switch (step) {
            case PICKUP: {
                Pickup best = null;
                float bd = 1e18f;
                for (Pickup p : duty.all) {
                    float dd = (p.x - rn.x) * (p.x - rn.x) + (p.z - rn.z) * (p.z - rn.z);
                    if (dd < bd) {
                        bd = dd;
                        best = p;
                    }
                }
                if (best == null) return false;
                out[0] = best.x;
                out[1] = best.y + 2.2f;
                out[2] = best.z;
                return true;
            }
            case PASS:
                out[0] = City.routeX(City.PASS_Z);
                out[1] = 2.5f;
                out[2] = City.PASS_Z;
                return true;
            case VILLAGE:
                out[0] = City.villageX();
                out[1] = 2.5f;
                out[2] = City.VILLAGE_Z;
                return true;
            default:
                return false;
        }
    }

    /** The word for where the way goes now, as Lang keeps it. */
    String goalWord() {
        return step == PICKUP ? "goal.pickup" : step == PASS ? "goal.pass" : "goal.village";
    }
}
